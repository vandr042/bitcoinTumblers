package control;

import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.VersionMessage;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import data.PeerRecord;
import data.PeerAddressTimePair;
import listeners.ConnTestSlave;
import listeners.DeadPeerListener;

public class ConnectionTester implements Runnable {

	private Manager myParent;
	private VersionMessage version;
	private Executor connTestPool;

	private Set<PeerAddress> pendingTests;
	private Semaphore pendingWall;
	private int step;

	private PriorityBlockingQueue<PeerAddressTimePair> harvestedPeers;
	private PriorityBlockingQueue<PeerAddressTimePair> unsolicitedPeers;
	private PriorityBlockingQueue<PeerAddressTimePair> disconnectedPeers;

	private static final int MAX_PEERS_TO_TEST = 2000;
	private static final long TOO_SOON_SEC = 1800;

	public ConnectionTester(Manager parent) {
		this.myParent = parent;
		this.version = new VersionMessage(this.myParent.getParams(), 0);

		this.harvestedPeers = new PriorityBlockingQueue<PeerAddressTimePair>();
		this.unsolicitedPeers = new PriorityBlockingQueue<PeerAddressTimePair>();
		this.disconnectedPeers = new PriorityBlockingQueue<PeerAddressTimePair>();

		this.pendingWall = new Semaphore(ConnectionTester.MAX_PEERS_TO_TEST);
		this.step = 0;

		//TODO numbers?
		this.connTestPool = new ThreadPoolExecutor(2, 4, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}

	public void giveNewNode(PeerAddress addr, long ts, boolean unsolicited) {
		/*
		 * TODO check that these queues remain "sane" in size
		 */
		PeerAddressTimePair tmpPair = null;
		if (unsolicited) {
			/*
			 * We prioritize the most recently learned unsolicted addrs, so
			 * invert the TS
			 */
			tmpPair = new PeerAddressTimePair(addr, ts * -1);
			this.unsolicitedPeers.offer(tmpPair);
		} else {
			tmpPair = new PeerAddressTimePair(addr, ts);
			this.harvestedPeers.offer(tmpPair);
		}
	}

	public void run() {

		while (true) {
			try {
				this.pendingWall.acquire();

				/*
				 * Get the next peer to test
				 */
				PeerAddress toTest = null;
				while (toTest == null) {
					// TODO integrate reconnection
					// TODO check we're not trying a node too soon again
					// TODO how do we refill this once we test all the nodes?

					PeerAddressTimePair testPull = this.unsolicitedPeers.poll();
					if (testPull == null) {
						testPull = this.harvestedPeers.take();
					}

					/*
					 * Get the record object and attempt to spin up a test,
					 * prevents us from spinning up two tests at the same time
					 * (attemptConnectionStart returns false)
					 */
					PeerRecord tRecord = this.myParent.getRecord(testPull.getAddress());
					if (tRecord.attemptConnectionStart()) {
						toTest = tRecord.getMyAddr();
					}
				}

				/*
				 * Actually spin the test up
				 */
				Peer peerObj = new Peer(this.myParent.getParams(), this.version, toTest, null, false);
				peerObj.registerAddressConsumer(this.myParent);
				ConnTestSlave testSlave = new ConnTestSlave(peerObj, this);
				ListenableFuture<SocketAddress> connFuture = this.myParent.getNIOClient()
						.openConnection(toTest.getSocketAddress(), peerObj);
				Futures.addCallback(connFuture, testSlave, this.connTestPool);

			} catch (InterruptedException e) {
				this.myParent.logException(e.getLocalizedMessage());
			}
		}
	}

	public void logSummary() {
		synchronized (this.pendingTests) {
			this.myParent.logEvent("conn tests pending: " + this.pendingTests.size());
		}
	}

	public void reportFailedPeer(Peer failedPeer, String reason) {

		/*
		 * Record the failure time and remove pending test
		 */
		this.myParent.getRecord(failedPeer.getAddress()).signalConnectionFailed();
		this.pendingWall.release();
		this.myParent.logEvent("failed " + failedPeer.getAddress().toString() + " - " + reason);
	}

	public void reportWorkingPeer(Peer workingPeer) {

		this.myParent.getRecord(workingPeer.getAddress()).signalConnected();
		this.pendingWall.release();

		// TODO should this have a thread pool/executor?
		workingPeer.addConnectionEventListener(new DeadPeerListener(this.myParent));
		
		this.myParent.resolvedStartedPeer(workingPeer);

		this.myParent.logEvent("worked " + workingPeer.getAddress().toString());
	}
}
