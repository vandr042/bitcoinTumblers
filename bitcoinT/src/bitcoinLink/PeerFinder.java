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

	private HashMap<PeerAddress, PeerHelper> activePeers;
	private BlockingQueue<PeerAddress> toTestQueue;
	

	private PrintStream peerHarvestLog;
	private PrintStream hmStats = new PrintStream("hmStats.txt");
	public static final String PEER_HARVEST_LOG_FILE = "harvest.log";
	public static final File WALLET_FILE = new File("foo");
	public static final String WALLET_PFX = "test";

	private static final Long UPDATE_ACTIVE_NODES_INTERVAL = (long) 60000;
	private static final Long TRY_TO_CONNECT_WINDOW_SEC = (long) 10000;

	private static final Long EXPERIMENT_TIME_SEC = (long) 1800;

	/* constructor */
	public PeerFinder() throws FileNotFoundException {
		/*
		 * Build our internal data structures
		 */
		this.activePeers = new HashMap<PeerAddress, PeerHelper>();
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
		Thread.sleep(10000);
		List<Peer> bootStrapPeers = this.kit.peerGroup().getConnectedPeers();
		startThreadPool(); //spin up thread pool 

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

	/* create thread pool to take from testQueue */
	private void startThreadPool(){
		toTestQueue = new LinkedBlockingQueue();
		int i = 0;
		
		while (i < 28){
			TestConnThread newTest = new TestConnThread(toTestQueue, kit.peerGroup(), activePeers, peerHarvestLog);
			Thread cThread = new Thread(newTest);
			cThread.setDaemon(true);
			cThread.start();
			i++;
		}
	}

	public void run() {
		
		long startTimeSec = System.currentTimeMillis() / 1000;
		while ((startTimeSec - System.currentTimeMillis()/ 1000)  < PeerFinder.EXPERIMENT_TIME_SEC) {
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

			Set<PeerAddress> activePeersWeKnow = new HashSet<PeerAddress>();
			Set<PeerAddress> deadPeers = new HashSet<PeerAddress>();
			synchronized (this) {
				for (PeerAddress tConnectedAddr : this.activePeers.keySet()) {
					activePeersWeKnow.addAll(this.activePeers.get(tConnectedAddr).getNodesActiveWithin(PeerFinder.TRY_TO_CONNECT_WINDOW_SEC));
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
			
			hmStats.println(startTimeSec - System.currentTimeMillis()/1000 + " connected to: " + this.activePeers.size());
			System.out.println("connected to: " + this.activePeers.size());
			System.out.println("active nodes we're not connected to: " + activePeersWeKnow.size());

			/* add active peers to que so test threads can start working */
			for (PeerAddress aPeerAddr : activePeersWeKnow){
				if (aPeerAddr == null) {
					System.out.println("No Peers Found");
				}else{
					System.out.println("Adding peer to que: " + aPeerAddr);
					toTestQueue.offer(aPeerAddr);
				}
			}
		}
		System.out.println(activePeers);
	}
	
	public PeerAddress getAddressToTest() throws InterruptedException{
		return this.toTestQueue.take();
	}

	public static void main(String[] args) throws Exception {
		PeerFinder finder = new PeerFinder();
		finder.bootstrap();

		Thread tthread = new Thread(finder);
		tthread.setName("New connection trying thread");
		tthread.start();
	}

}
