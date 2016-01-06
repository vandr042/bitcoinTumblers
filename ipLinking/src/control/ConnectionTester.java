package control;

import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.VersionMessage;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import data.PeerPool;
import data.PeerTimePair;
import listeners.ConnTestSlave;
import listeners.DeadPeerListener;

public class ConnectionTester implements Runnable {

	private Manager myParent;
	private VersionMessage version;
	private Executor connTestPool;

	private Set<PeerAddress> pendingTests;
	private Semaphore pendingWall;

	private static final int MAX_PEERS_TO_TEST = 2000;
	private static final int LOWER_BOUND_PEERS_TO_TEST = 800;
	private static final long SLEEP_TIME_SEC = 60;
	private static final long TOO_SOON_SEC = 1800;

	public ConnectionTester(Manager parent) {
		this.myParent = parent;
		this.version = new VersionMessage(this.myParent.getParams(), 0);

		this.pendingTests = new HashSet<PeerAddress>();
		this.pendingWall = new Semaphore(0);

		/*
		 * We need very few threads as the action of recording success/failure
		 * blocks REALLY heavily and is basically single threaded (but super
		 * cheap), therefore don't muddy the waters with a ton of threads
		 */
		this.connTestPool = new ThreadPoolExecutor(1, 2, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}

	public void run() {
		PeerPool peers = this.myParent.getPeerPool();
		Set<PeerAddress> testPeers = null;
		int wallCount = 0;

		while (true) {

			synchronized (this) {
				List<PeerTimePair> testBase = peers.getConnectionCandidates();
				testPeers = this.filterPeerTest(testBase);
				this.pendingTests.addAll(testPeers);
				this.myParent.logEvent(testPeers.size() + " peers to attempt to connect to");

				for (PeerAddress testPeer : testPeers) {
					Peer peerObj = new Peer(this.myParent.getParams(), this.version, testPeer, null, false);
					ConnTestSlave testSlave = new ConnTestSlave(peerObj, this);
					ListenableFuture<SocketAddress> connFuture = this.myParent.getNIOClient()
							.openConnection(testPeer.getSocketAddress(), peerObj);
					Futures.addCallback(connFuture, testSlave, this.connTestPool);
				}

				this.pendingWall.drainPermits();
				wallCount = this.pendingTests.size() - ConnectionTester.LOWER_BOUND_PEERS_TO_TEST;
			}

			if (testPeers.size() == 0 || wallCount < 0) {
				/*
				 * We had no new things to test, give it a rest for a bit...
				 */
				try {
					this.myParent.logEvent("Connection tester exhuasted, going to sleep.");
					Thread.sleep(SLEEP_TIME_SEC * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
			} else {
				/*
				 * Wait for results until we're at our "refill" point
				 */
				try {
					this.myParent.logEvent("Connection tester blocking till open capacity.");
					this.pendingWall.acquire(wallCount);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void logSummary() {
		synchronized (this.pendingTests) {
			this.myParent.logEvent("conn tests pending: " + this.pendingTests.size());
		}
	}

	/*
	 * NONE OF THIS SHOULD BE THREAD SAFE, we're already locking in the
	 * place that calls this
	 */
	private Set<PeerAddress> filterPeerTest(List<PeerTimePair> testBase) {
		
		/*
		 * Remove all peers we're actively testing
		 */
		int searchPos = 0;
		while (searchPos < testBase.size()) {
			if (this.pendingTests.contains(testBase.get(searchPos).getAddress())) {
				testBase.remove(searchPos);
			} else {
				searchPos++;
			}
		}

		/*
		 * Ensure we're not hurling too many tests at once at us and that we're
		 * not trying too soon
		 */
		Set<PeerAddress> testAddrs = new HashSet<PeerAddress>();
		long currTime = System.currentTimeMillis();
		for (int pos = 0; pos < testBase.size(); pos++) {
			/*
			 * Too soon check
			 */
			if (currTime - testBase.get(pos).getTime() < ConnectionTester.TOO_SOON_SEC * 1000) {
				break;
			}

			/*
			 * size of testing load check
			 */
			if (testAddrs.size() + this.pendingTests.size() >= ConnectionTester.MAX_PEERS_TO_TEST) {
				break;
			}

			testAddrs.add(testBase.get(pos).getAddress());
		}

		return testAddrs;
	}

	public void reportFailedPeer(Peer failedPeer, String reason) {

		/*
		 * Record the failure time and remove pending test
		 */
		synchronized (this) {
			this.myParent.getPeerPool().signalFailedConnect(failedPeer.getAddress());
			this.pendingTests.remove(failedPeer.getAddress());
			this.pendingWall.release();
		}

		/*
		 * Log failure
		 */
		this.myParent.logEvent("failed " + failedPeer.getAddress().toString() + " - " + reason);
	}

	public void reportWorkingPeer(Peer workingPeer) {

		/*
		 * Update our state and peer pool
		 */
		synchronized (this) {
			this.myParent.getPeerPool().signalConnected(workingPeer.getAddress(), workingPeer);
			this.pendingTests.remove(workingPeer.getAddress());
			this.pendingWall.release();
		}

		//TODO should this have a thread pool/executor?
		workingPeer.addConnectionEventListener(new DeadPeerListener(this.myParent));
		/*
		 * Log
		 */
		this.myParent.logEvent("worked " + workingPeer.getAddress().toString());
	}
}
