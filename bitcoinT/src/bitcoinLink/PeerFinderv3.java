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

public class PeerFinderv3 implements Runnable {

	NetworkParameters params;
	private PeerGroup pg;

	private HashMap<PeerAddress, PeerHelper> activePeers;
	private BlockingQueue<PeerAddress> toTestQueue;
	

	private PrintStream peerHarvestLog;

	public static final String PEER_HARVEST_LOG_FILE = "harvest.log";
	public static final File WALLET_FILE = new File("foo");
	public static final String WALLET_PFX = "test";

	private static final Long UPDATE_ACTIVE_NODES_INTERVAL = (long) 60000;
	private static final Long TRY_TO_CONNECT_WINDOW_SEC = (long) 1800;

	private static final Long EXPERIMENT_TIME_SEC = (long) 3600;

	/* constructor */
	public PeerFinderv3() throws FileNotFoundException {
		/*
		 * Build our internal data structures
		 */
		this.activePeers = new HashMap<PeerAddress, PeerHelper>();
		this.toTestQueue = new LinkedBlockingQueue<PeerAddress>();

		/*
		 * Build bitcoinj required objects
		 */
		params = MainNetParams.get();
		pg = new PeerGroup(params);

		/*
		 * Build logging tools
		 */
		this.peerHarvestLog = new PrintStream(PeerFinderv3.PEER_HARVEST_LOG_FILE);
		
		//startThreadPool();
	}

	/*
	 * initializes peergroup and starts main thread that will start the helper
	 * threads
	 */
	public void bootstrap() {
		/*
		 * Start up our connection to the bit coin network
		 */
		pg.start();
		List<Peer> bootStrapPeers = pg.getConnectedPeers();

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
	
	private void startThreadPool(){
		toTestQueue = new LinkedBlockingQueue();
		int i = 0;
		
		/* create thread pool to take from testQueue */
		
		while (i < 28){
			TestConnThread newTest = new TestConnThread(toTestQueue, pg);
			Thread cThread = new Thread(newTest);
			cThread.setDaemon(true);
			cThread.start();
		}
	}

	public void run() {
		
		long startTimeSec = System.currentTimeMillis() / 1000;
		while ((System.currentTimeMillis() / 1000) - startTimeSec < PeerFinderv3.EXPERIMENT_TIME_SEC) {
			/*
			 * Wait the UPDATE_ACTIVE_NODES_INTERVAL milliseconds
			 */
			try {
				Thread.sleep(PeerFinderv3.UPDATE_ACTIVE_NODES_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			/*
			 * Get all addresses we know about
			 */

			Set<PeerAddress> activePeersWeKnow = new HashSet<PeerAddress>();
			Set<PeerAddress> deadPeers = new HashSet<PeerAddress>();
			synchronized (this) {
				for (PeerAddress tConnectedAddr : this.activePeers.keySet()) {
					activePeersWeKnow.addAll(this.activePeers.get(tConnectedAddr).getNodesActiveWithin( //TODO change this to PeerAddresses
							PeerFinderv3.TRY_TO_CONNECT_WINDOW_SEC));
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
				activePeersWeKnow.removeAll(this.activePeers.keySet());
			}

			/*
			 * A little bit of reporting
			 */
			System.out.println("connected to: " + pg.getConnectedPeers());
			System.out.println("active nodes we're not connected to: " + activePeersWeKnow.size());

			/*for (PeerAddress aPeerAddr : activePeersWeKnow){
				toTestQueue.offer(aPeerAddr);
			}*/
		}
		pg.stop();
	}
	
	public PeerAddress getAddressToTest() throws InterruptedException{
		return this.toTestQueue.take();
	}
	
	//TODO way to hand back working connections

	public static void main(String[] args) throws Exception {
		PeerFinderv3 finder = new PeerFinderv3();
		finder.bootstrap();

		Thread tthread = new Thread(finder);
		tthread.setName("New connection trying thread");
		tthread.start();
	}

}
