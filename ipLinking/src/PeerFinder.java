
import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.store.*;
import org.bitcoinj.utils.BriefLogFormatter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.*;

public class PeerFinder implements Runnable {

	private WalletAppKit kit;

	private HashMap<PeerAddress, PeerHelper> activePeers;

	private BlockingQueue<PeerAddress> toTestQueue;
	private HashSet<PeerAddress> inTestQueue;
	private HashMap<PeerAddress, Long> failedConnectionAttempts;

	private PrintStream peerHarvestLog;
	private PrintStream peerConnectionLog;
	private PrintStream hmStats = new PrintStream("hmStats.txt");

	public static final String PEER_HARVEST_LOG_FILE = "harvest.log";
	public static final String PEER_CONNECTION_LOG_FILE = "connection.log";
	public static final File WALLET_FILE = new File("foo");
	public static final String WALLET_PFX = "test";

	private static final Long UPDATE_ACTIVE_NODES_INTERVAL = (long) 60000;
	private static final Long TRY_TO_CONNECT_WINDOW_SEC = (long) 86400;
	private static final Long EXPERIMENT_TIME_SEC = (long) 14400;

	private static final int NUMBER_OF_TEST_CONN_THREADS = 30;

	/* constructor */
	public PeerFinder() throws FileNotFoundException {
		/*
		 * Build our internal data structures
		 */
		this.activePeers = new HashMap<PeerAddress, PeerHelper>();

		/*
		 * Build bitcoinj required objects
		 */
		NetworkParameters params = MainNetParams.get();
		this.kit = new WalletAppKit(params, PeerFinder.WALLET_FILE, PeerFinder.WALLET_PFX);

		/*
		 * Build logging tools
		 */
		this.peerHarvestLog = new PrintStream(PeerFinder.PEER_HARVEST_LOG_FILE);
		this.peerConnectionLog = new PrintStream(PeerFinder.PEER_CONNECTION_LOG_FILE);
	}

	/*
	 * initializes peergroup and starts main thread that will start the helper
	 * threads
	 */
	public void bootstrap() throws InterruptedException {
		/*
		 * Start up our connection to the bit coin network
		 */
		this.kit.startAsync();
		this.kit.awaitRunning();
		System.out.println("Making sure we're up and running actually");
		// TODO feels like we should need this...
		Thread.sleep(30000);
		List<Peer> bootStrapPeers = this.kit.peerGroup().getConnectedPeers();

		/*
		 * Start the connection testers
		 */
		this.startTestConnectorPool();

		/*
		 * Spin up address harvesters for our initial peers
		 */
		for (Peer tPeer : bootStrapPeers) {
			this.spinUpPeerHarvester(tPeer);
		}
	}

	/*
	 * Spins up a peer harvesting thread for a given peer if we don't already
	 * have one
	 */
	public void spinUpPeerHarvester(Peer peerToStart) {

		/*
		 * Create needed data structures and update the master's state, let's
		 * synchronize on this just to be safe
		 */
		PeerAddress peersAddr = peerToStart.getAddress();
		PeerHelper newHelper = null;
		synchronized (this) {
			/*
			 * Check to make sure this peer does not already have a peer helper
			 * object spun up, if so just silently exit
			 */
			if (this.activePeers.containsKey(peersAddr)) {
				return;
			}

			newHelper = new PeerHelper(peerToStart, peerHarvestLog);
			this.activePeers.put(peersAddr, newHelper);
		}

		/*
		 * Create the peer helper object, wrapping thread, and start it
		 */
		Thread pthread = new Thread(newHelper);
		pthread.setName(peersAddr.toString() + " address harvesting thread");
		pthread.setDaemon(true);
		pthread.start();
	}

	/**
	 * Method to start up the test connection threads
	 */
	private void startTestConnectorPool() {
		/*
		 * Sanity check that we have not already started up the test connector
		 * pool
		 */
		if (this.toTestQueue != null) {
			throw new RuntimeException("Start Test Connector should only be done once!");
		}

		/*
		 * Build the initial data structures
		 */
		this.toTestQueue = new LinkedBlockingQueue<PeerAddress>();
		this.inTestQueue = new HashSet<PeerAddress>();
		this.failedConnectionAttempts = new HashMap<PeerAddress, Long>();

		/*
		 * Spin up the tester threads
		 */
		for (int i = 0; i < PeerFinder.NUMBER_OF_TEST_CONN_THREADS; i++) {
			TestConnThread newTest = new TestConnThread(this, kit.peerGroup(), this.peerConnectionLog);
			Thread cThread = new Thread(newTest);
			cThread.setName("Test connection thread number " + i);
			cThread.setDaemon(true);
			cThread.start();
		}
	}

	public void run() {

		long startTimeSec = System.currentTimeMillis() / 1000;
		while ((startTimeSec - System.currentTimeMillis() / 1000) < PeerFinder.EXPERIMENT_TIME_SEC) {
			/*
			 * Wait the UPDATE_ACTIVE_NODES_INTERVAL milliseconds
			 */
			try {
				Thread.sleep(PeerFinder.UPDATE_ACTIVE_NODES_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			/*
			 * Get all addresses we know about
			 */

			Set<PeerAddress> peersToTryAndConnectTo = new HashSet<PeerAddress>();
			Set<PeerAddress> deadPeers = new HashSet<PeerAddress>();
			synchronized (this) {
				for (PeerAddress tConnectedAddr : this.activePeers.keySet()) {
					// TODO scale up the "try" window
					peersToTryAndConnectTo.addAll(this.activePeers.get(tConnectedAddr)
							.getNodesActiveWithin(PeerFinder.TRY_TO_CONNECT_WINDOW_SEC));
					if (!this.activePeers.get(tConnectedAddr).isAlive()) {
						deadPeers.add(tConnectedAddr);
					}
				}

				/*
				 * Purge dead peers, also remove from active peers the nodes
				 * we're already connected to
				 */
				for (PeerAddress tDead : deadPeers) {
					this.activePeers.remove(tDead);
				}

				peersToTryAndConnectTo.removeAll(this.activePeers.keySet());
				synchronized (this.inTestQueue) {
					/*
					 * Remove everyone we've already got a pending test also
					 * remove everyone we tried to connect to and fail
					 */
					peersToTryAndConnectTo.removeAll(this.inTestQueue);
					peersToTryAndConnectTo.removeAll(this.failedConnectionAttempts.keySet());

					/*
					 * Note that we're adding all of the addresses to the test
					 * queue, and actually do so
					 */
					this.inTestQueue.addAll(peersToTryAndConnectTo);
					for (PeerAddress tAddress : peersToTryAndConnectTo) {
						this.toTestQueue.offer(tAddress);
					}
					this.hmStats.println("Added " + peersToTryAndConnectTo.size() + " to test.");
					this.hmStats.println("current active/pending test count: " + this.inTestQueue.size());
					this.hmStats.println("current failed test count: " + this.failedConnectionAttempts.size());
				}

				// TODO add in the peers with observed logons to the to test
				// queue no matter their "failed" state

				// TODO age things out of the "we tried to connected and failed
				// set?
			}

			/*
			 * A little bit of reporting
			 */

			hmStats.println((System.currentTimeMillis() / 1000 - startTimeSec) + " - connected to: "
					+ this.activePeers.size() + " pg thinks " + this.kit.peerGroup().getConnectedPeers().size());
		}
	}

	public PeerAddress getAddressToTest() throws InterruptedException {
		return this.toTestQueue.take();
	}

	public void reportConnectionFailure(PeerAddress testedAddress) {
		synchronized (this.inTestQueue) {
			this.inTestQueue.remove(testedAddress);
			this.failedConnectionAttempts.put(testedAddress, System.currentTimeMillis());
		}
	}

	public void reportConnectionSuccess(PeerAddress testedAddress, Peer resultantPeer) {
		this.spinUpPeerHarvester(resultantPeer);
		synchronized (this.inTestQueue) {
			this.inTestQueue.remove(testedAddress);
		}
	}

	public static void main(String[] args) throws Exception {
		PeerFinder finder = new PeerFinder();
		finder.bootstrap();

		Thread tthread = new Thread(finder);
		tthread.setName("New connection trying thread");
		tthread.start();
	}

}
