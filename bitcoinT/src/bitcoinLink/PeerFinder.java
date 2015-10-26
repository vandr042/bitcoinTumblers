package bitcoinLink;

import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.store.*;
import org.bitcoinj.utils.BriefLogFormatter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;

public class PeerFinder implements Runnable {

	private WalletAppKit kit;

	private HashMap<InetAddress, PeerHelper> activePeers;
	private BlockingQueue<PeerAddress> toTestQueue;
	

	private PrintStream peerHarvestLog;

	public static final String PEER_HARVEST_LOG_FILE = "harvest.log";
	public static final File WALLET_FILE = new File("foo");
	public static final String WALLET_PFX = "test";

	private static final Long UPDATE_ACTIVE_NODES_INTERVAL = (long) 60000;
	private static final Long TRY_TO_CONNECT_WINDOW_SEC = (long) 1800;

	private static final Long EXPERIMENT_TIME_SEC = (long) 3600;

	/* constructor */
	public PeerFinder() throws FileNotFoundException {
		/*
		 * Build our internal data structures
		 */
		this.activePeers = new HashMap<InetAddress, PeerHelper>();
		this.toTestQueue = new LinkedBlockingQueue<PeerAddress>();

		/*
		 * Build bitcoinj required objects
		 */
		NetworkParameters params = MainNetParams.get();
		this.kit = new WalletAppKit(params, PeerFinder.WALLET_FILE, PeerFinder.WALLET_PFX);

		/*
		 * Build logging tools
		 */
		this.peerHarvestLog = new PrintStream(PeerFinder.PEER_HARVEST_LOG_FILE);
		
		//TODO build and start TestConnection Threads here
	}

	/*
	 * initializes peergroup and starts main thread that will start the helper
	 * threads
	 */
	public void bootstrap() {
		/*
		 * Start up our connection to the bit coin network
		 */
		this.kit.startAsync();
		this.kit.awaitRunning();
		List<Peer> bootStrapPeers = this.kit.peerGroup().getConnectedPeers();

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
	private void spinUpPeerHarvester(Peer peerToStart) {

		/*
		 * Create needed data structures and update the master's state, let's
		 * synchronize on this just to be safe
		 */
		InetAddress peersIP = peerToStart.getAddress().getAddr();
		PeerHelper newHelper = null;
		synchronized (this) {
			/*
			 * Check to make sure this peer does not already have a peer helper
			 * object spun up, if so just silently exit
			 */
			if (this.activePeers.containsKey(peersIP)) {
				return;
			}

			newHelper = new PeerHelper(peerToStart, peerHarvestLog);
			this.activePeers.put(peersIP, newHelper);
		}

		/*
		 * Create the peer helper object, wrapping thread, and start it
		 */
		Thread pthread = new Thread(newHelper);
		pthread.setName(peersIP.toString() + " address harvesting thread");
		pthread.setDaemon(true);
		pthread.start();
	}

	public void run() {

		long startTimeSec = System.currentTimeMillis() / 1000;
		while ((System.currentTimeMillis() / 1000) - startTimeSec < PeerFinder.EXPERIMENT_TIME_SEC) {
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

			Set<InetAddress> activePeersWeKnow = new HashSet<InetAddress>();
			Set<InetAddress> deadPeers = new HashSet<InetAddress>();
			synchronized (this) {
				for (InetAddress tConnectedAddr : this.activePeers.keySet()) {
					activePeersWeKnow.addAll(this.activePeers.get(tConnectedAddr).getNodesActiveWithin(
							PeerFinder.TRY_TO_CONNECT_WINDOW_SEC));
					if (!this.activePeers.get(tConnectedAddr).isAlive()) {
						deadPeers.add(tConnectedAddr);
					}
				}

				/*
				 * Purge dead peers, also remove from active peers the nodes
				 * we're already connected to
				 */
				for (InetAddress tDead : deadPeers) {
					this.activePeers.remove(tDead);
				}
				activePeersWeKnow.removeAll(this.activePeers.keySet());
			}

			/*
			 * A little bit of reporting
			 */
			System.out.println("connected to: " + this.activePeers.size());
			System.out.println("active nodes we're not connected to: " + activePeersWeKnow.size());

			//TODO we want to try and connect to addresses in activePeersWeKnow here
		}

	}
	
	public PeerAddress getAddressToTest() throws InterruptedException{
		return this.toTestQueue.take();
	}
	
	//TODO way to hand back working connections

	public static void main(String[] args) throws Exception {
		PeerFinder finder = new PeerFinder();
		finder.bootstrap();

		Thread tthread = new Thread(finder);
		tthread.setName("New connection trying thread");
		tthread.start();
	}

}
