package control;

import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.VersionMessage;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import data.PeerRecord;
import data.SanatizedRecord;
import data.PeerAddressTimePair;
import listeners.ConnTestSlave;
import listeners.DeadPeerListener;
import listeners.VersionTestSlave;

//TODO IMO since we're governed by the all might time stamp, get rid of the idea of solicited vs unsolicited
public class ConnectionTester implements Runnable {

	private Manager myParent;
	private Executor connTestPool;
	private Executor versionTestPool;
	private ScheduledExecutorService versionTimeoutPool;

	private BlockingQueue<ConnectionEvent> eventQueue;

	private Set<PeerAddress> pendingTests;
	private int availTests;

	private PriorityBlockingQueue<PeerAddressTimePair> harvestedPeers;
	private PriorityBlockingQueue<PeerAddressTimePair> unsolicitedPeers;
	private int availNew;
	private PriorityBlockingQueue<PeerAddressTimePair> disconnectedPeers;
	private PriorityBlockingQueue<PeerAddressTimePair> retryPeers;

	private static final int MAX_PEERS_TO_TEST = 3000;
	private static final long RETEST_TRY_SEC = 1800;
	private static final long NO_VERSION_RETEST_TRY_SEC = 900;
	private static final long RECONNECT_TRY_SEC = 300;

	private static final long VERSION_TIMEOUT_SEC = 10;

	private enum ConnectionEvent {
		AVAILNEW, AVAILOLD, AVAILTEST, TIMER;
	}

	public ConnectionTester(Manager parent) {
		this.myParent = parent;

		this.eventQueue = new LinkedBlockingQueue<ConnectionEvent>();

		this.harvestedPeers = new PriorityBlockingQueue<PeerAddressTimePair>();
		this.unsolicitedPeers = new PriorityBlockingQueue<PeerAddressTimePair>();
		this.disconnectedPeers = new PriorityBlockingQueue<PeerAddressTimePair>();
		this.retryPeers = new PriorityBlockingQueue<PeerAddressTimePair>();

		this.availTests = ConnectionTester.MAX_PEERS_TO_TEST;
		this.availNew = 0;

		// TODO sanity check numbers?
		this.connTestPool = new ThreadPoolExecutor(2, 4, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		this.versionTestPool = new ThreadPoolExecutor(2, 4, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		this.versionTimeoutPool = new ScheduledThreadPoolExecutor(1);
	}

	// TODO work with sanatized record more intelligently
	public void giveNewNode(SanatizedRecord addr) {
		/*
		 * TODO check that these queues remain "sane" in size
		 */
		PeerAddressTimePair tmpPair = new PeerAddressTimePair(addr.getPeerAddressObject(), addr.getTS() * -1);
		this.harvestedPeers.offer(tmpPair);

		this.eventQueue.add(ConnectionEvent.AVAILNEW);
	}

	public void giveReconnectTarget(PeerAddress addr) {
		this.disconnectedPeers.offer(
				new PeerAddressTimePair(addr, System.currentTimeMillis() + ConnectionTester.RECONNECT_TRY_SEC * 1000));
		this.eventQueue.add(ConnectionEvent.AVAILOLD);
	}
	
	public void givePriorityConnectTarget(PeerAddress add){
		//FIXME implement me!!!
	}

	public void run() {

		while (true) {
			try {
				/*
				 * Compute the next timer expiration, if there is a pending
				 * timer expiration then wait for it. IF there isn't a few
				 * things that might have happened. Option 1, we have nothing in
				 * the timer queues, in that case we just wait for an event.
				 * Otherwise something has already expired, but we couldn't
				 * clear it, which means we need a test slot, so wait for an
				 * event (only way to get test slots).
				 */
				long nextWait = this.computeNextWaitTime();
				ConnectionEvent nextEvent = null;
				if (nextWait <= 0) {
					nextEvent = this.eventQueue.take();
				} else {
					nextEvent = this.eventQueue.poll(nextWait, TimeUnit.MILLISECONDS);
					if (nextEvent == null) {
						nextEvent = ConnectionEvent.TIMER;
					}
				}

				/*
				 * If the event results in a counter getting updated do so
				 */
				if (nextEvent.equals(ConnectionEvent.AVAILNEW)) {
					this.availNew++;
				} else if (nextEvent.equals(ConnectionEvent.AVAILTEST)) {
					this.availTests++;
				}

				/*
				 * check if we have avail reconnect attempts and open test
				 * slots, spin them up so long as we have both of these
				 * resources
				 */
				this.drainQueue(this.disconnectedPeers);

				/*
				 * Check if we have avail "untested" nodes
				 */
				while (this.availTests > 0 && this.availNew > 0) {
					/*
					 * Get an available new test, update counter
					 */
					PeerAddressTimePair testPull = this.unsolicitedPeers.poll();
					if (testPull == null) {
						testPull = this.harvestedPeers.poll();
					}
					if (testPull == null) {
						throw new RuntimeException("There should have been an object in those queues.");
					}
					this.availNew--;

					/*
					 * Try and start the test
					 */
					this.startTest(testPull.getAddress());
				}

				/*
				 * check if we have avail test slots and if we have a valid
				 * retry node
				 */
				this.drainQueue(this.retryPeers);

			} catch (Exception e) {
				this.myParent.logException(e);
			}
		}

	}

	/*
	 * Drains one of the timer based queues
	 */
	private void drainQueue(PriorityBlockingQueue<PeerAddressTimePair> queueToDrain) {
		while (this.availTests > 0 && !queueToDrain.isEmpty()) {
			long ts = queueToDrain.peek().getTime();
			if (ts <= System.currentTimeMillis()) {
				PeerAddress tAddr = queueToDrain.poll().getAddress();
				this.startTest(tAddr);
			} else {
				break;
			}
		}
	}

	private long computeNextWaitTime() {
		long shortestTime = Long.MAX_VALUE;
		if (!this.retryPeers.isEmpty()) {
			if (this.retryPeers.peek().getTime() < shortestTime) {
				shortestTime = this.retryPeers.peek().getTime();
			}
		}
		if (!this.disconnectedPeers.isEmpty()) {
			if (this.disconnectedPeers.peek().getTime() < shortestTime) {
				shortestTime = this.disconnectedPeers.peek().getTime();
			}
		}

		if (shortestTime == Long.MAX_VALUE) {
			return 0;
		}

		return shortestTime - System.currentTimeMillis();
	}

	private boolean startTest(PeerAddress toTest) {
		PeerRecord tRecord = null;
		while (tRecord == null) {
			tRecord = this.myParent.getRecord(toTest);
		}

		// TODO we "lose" the once connected status if we retry the connection
		// and the second connection attempt fails, but we don't from the record...

		if (tRecord.getTimeConnFailed() != -1 && (System.currentTimeMillis()
				- tRecord.getTimeConnFailed()) < (ConnectionTester.RETEST_TRY_SEC * 1000)) {
			return false;
		}

		if (tRecord.attemptConnectionStart()) {
			this.availTests--;

			/*
			 * Actually spin the test up
			 */
			Peer peerObj = new Peer(this.myParent.getParams(), new VersionMessage(this.myParent.getParams(), 0), toTest,
					null, false);
			peerObj.registerAddressConsumer(this.myParent);
			ConnTestSlave testSlave = new ConnTestSlave(peerObj, this);
			ListenableFuture<SocketAddress> connFuture = this.myParent.getRandomNIOClient()
					.openConnection(toTest.getSocketAddress(), peerObj);
			Futures.addCallback(connFuture, testSlave, this.connTestPool);
			return true;

		} else {
			return false;
		}

	}

	public void logSummary() {
		synchronized (this.pendingTests) {
			this.myParent.logEvent("conn tests pending: " + this.pendingTests.size(), Manager.DEBUG_LOG_LEVEL);
		}
	}

	public void reportTCPFailure(Peer failedPeer, String reason) {

		/*
		 * Record the failure time and remove pending test
		 */
		this.myParent.getRecord(failedPeer.getAddress()).signalConnectionFailed();
		this.retryPeers.add(new PeerAddressTimePair(failedPeer.getAddress(),
				System.currentTimeMillis() + ConnectionTester.RETEST_TRY_SEC * 1000));
		this.myParent.logEvent("tcpfailed " + failedPeer.getAddress().toString() + " - " + reason,
				Manager.DEBUG_LOG_LEVEL);

		/*
		 * We have a new retest option AND an open test slot, pair of events
		 * gogo
		 */
		this.eventQueue.add(ConnectionEvent.AVAILOLD);
		this.eventQueue.add(ConnectionEvent.AVAILTEST);
	}

	public void reportTCPSuccess(Peer connPeer) {
		this.myParent.logEvent("tcpstart " + connPeer.getAddress().toString(), Manager.DEBUG_LOG_LEVEL);
		VersionTestSlave nextSlave = new VersionTestSlave(this, connPeer);
		ListenableFuture<Peer> verHandshakeFuture = connPeer.getVersionHandshakeFuture();
		ListenableFuture<Peer> timeOutFuture = Futures.withTimeout(verHandshakeFuture,
				ConnectionTester.VERSION_TIMEOUT_SEC, TimeUnit.SECONDS, this.versionTimeoutPool);
		Futures.addCallback(timeOutFuture, nextSlave, this.versionTestPool);
	}

	public void reportNoVersionPeer(Peer failedPeer, String reason) {
		/*
		 * Force the connection closed
		 */
		failedPeer.close();

		/*
		 * Record the failure time and remove pending test
		 */
		this.myParent.getRecord(failedPeer.getAddress()).signalConnectionFailed();
		this.retryPeers.add(new PeerAddressTimePair(failedPeer.getAddress(),
				System.currentTimeMillis() + ConnectionTester.NO_VERSION_RETEST_TRY_SEC * 1000));
		this.myParent.logEvent("versionfailed " + failedPeer.getAddress().toString() + " - " + reason,
				Manager.DEBUG_LOG_LEVEL);

		/*
		 * We have a new retest option AND an open test slot, pair of events
		 * gogo
		 */
		this.eventQueue.add(ConnectionEvent.AVAILOLD);
		this.eventQueue.add(ConnectionEvent.AVAILTEST);
	}

	public void reportWorkingPeer(Peer workingPeer) {

		this.myParent.getRecord(workingPeer.getAddress()).signalConnected();

		// TODO should this have a thread pool/executor?
		workingPeer.addConnectionEventListener(new DeadPeerListener(this.myParent));

		this.myParent.resolvedStartedPeer(workingPeer);

		this.myParent.logEvent("conn," + workingPeer.getAddress().toString(), Manager.CRIT_LOG_LEVEL);
		this.eventQueue.add(ConnectionEvent.AVAILTEST);
	}
}
