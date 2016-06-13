package control;

import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
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
import listeners.ConnTestSlave;
import listeners.DeadPeerListener;
import listeners.VersionTestSlave;

public class ConnectionTester implements Runnable {

	private Manager myParent;
	private Executor connTestPool;
	private Executor versionTestPool;
	private ScheduledExecutorService versionTimeoutPool;

	private BlockingQueue<ConnectionEvent> eventQueue;

	private Set<SanatizedRecord> pendingTests;
	private int availTests;

	private PriorityQueue<SanatizedRecord> scheduledTestTimes;
	private HashSet<SanatizedRecord> scheduledPeers;

	private Queue<SanatizedRecord> priorityTests;
	private HashSet<SanatizedRecord> prioritySet;

	private static final int MAX_PEERS_TO_TEST = 3000;
	private static final long RETEST_TRY_SEC = 1800;
	private static final long RECONNECT_TRY_SEC = 300;

	private static final long VERSION_TIMEOUT_SEC = 10;

	public static final long INTERESTING_WINDOW_SEC = 24 * 3600;

	private enum ConnectionEvent {
		AVAILTEST, TIMER;
	}

	public ConnectionTester(Manager parent) {
		this.myParent = parent;

		this.eventQueue = new LinkedBlockingQueue<ConnectionEvent>();

		this.scheduledTestTimes = new PriorityQueue<SanatizedRecord>();
		this.scheduledPeers = new HashSet<SanatizedRecord>();

		this.priorityTests = new LinkedList<SanatizedRecord>();
		this.prioritySet = new HashSet<SanatizedRecord>();

		this.pendingTests = new HashSet<SanatizedRecord>();
		this.availTests = ConnectionTester.MAX_PEERS_TO_TEST;

		// TODO sanity check numbers?
		this.connTestPool = new ThreadPoolExecutor(2, 4, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		this.versionTestPool = new ThreadPoolExecutor(2, 4, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		this.versionTimeoutPool = new ScheduledThreadPoolExecutor(1);
	}

	// XXX do we want to worry about nodes in the "future" or clock skew?
	public boolean shouldIntroduce(long netTimestamp) {
		long now = System.currentTimeMillis() / 1000;
		return now - netTimestamp > ConnectionTester.INTERESTING_WINDOW_SEC;
	}

	/*
	 * Should only be called the very first time we learn about a node
	 */
	public void giveNewNode(SanatizedRecord addr) {
		SanatizedRecord clonedRecord = addr.clone();
		clonedRecord.updateTS(System.currentTimeMillis());
		synchronized (this) {
			if (this.scheduledPeers.contains(clonedRecord)) {
				/*
				 * XXX there are a few race conditions where I think this could
				 * happen
				 */
				//this.myParent
				//		.logException(new IllegalStateException("someone called giveNewNode twice " + addr.toString()));
			} else {
				this.scheduledTestTimes.add(clonedRecord);
				this.scheduledPeers.add(clonedRecord);
				this.eventQueue.add(ConnectionEvent.TIMER);
			}
		}
	}

	public void giveReconnectTarget(SanatizedRecord addr) {
		/*
		 * XXX we can run into a scenario where we connect off of a priority
		 * test, then D/C and don't add the reconnect targe because of the
		 * normal "we've never connected to you" test that is pending, this is
		 * fine, because in the worst case we're delayed by one "long" reconnect
		 * interval, but would be nice if we could update it without having to
		 * rebuild our whole heap, but this might be rare enough where we just
		 * rebuild the priority heap if we're really concerned
		 */
		SanatizedRecord cloneRecord = addr.clone();
		cloneRecord.updateTS(System.currentTimeMillis() + ConnectionTester.RECONNECT_TRY_SEC * 1000);
		synchronized (this) {
			if (!this.scheduledPeers.contains(cloneRecord)) {
				this.scheduledTestTimes.add(cloneRecord);
				this.scheduledPeers.add(cloneRecord);
				this.eventQueue.add(ConnectionEvent.TIMER);
			}
		}
	}

	public void givePriorityConnectTarget(SanatizedRecord addr) {
		synchronized (this) {
			if (!this.prioritySet.contains(addr) && !this.pendingTests.contains(addr)) {
				this.priorityTests.add(addr);
				this.prioritySet.add(addr);
				this.eventQueue.add(ConnectionEvent.TIMER);
			}
		}
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
				ConnectionEvent nextEvent = this.eventQueue.poll(nextWait, TimeUnit.MILLISECONDS);
				if (nextEvent == null) {
					nextEvent = ConnectionEvent.TIMER;
				}

				/*
				 * If the event results in a counter getting updated do so
				 */
				if (nextEvent.equals(ConnectionEvent.AVAILTEST)) {
					this.availTests++;
				}

				synchronized (this) {
					/*
					 * Check if we have avail "untested" nodes
					 */
					while (this.availTests > 0) {
						SanatizedRecord toTest = null;

						if (!this.priorityTests.isEmpty()) {
							toTest = this.priorityTests.poll();
							this.prioritySet.remove(toTest);
						} else if (!this.scheduledTestTimes.isEmpty()) {
							if (this.scheduledTestTimes.peek().getTS() <= System.currentTimeMillis()) {
								toTest = this.scheduledTestTimes.poll();
								this.scheduledPeers.remove(toTest);
							}
						}

						if (toTest == null) {
							/*
							 * We failed to find a suitable test, exit the loop
							 * even though we have tests
							 */
							break;
						} else {
							this.startTest(toTest);
						}
					}
				}
			} catch (Throwable e) {
				if (e instanceof Error) {
					this.myParent.logException(new RuntimeException("WTFHOLYSHIT"));
				}
				this.myParent.logException(e);
			}
		}
	}

	private long computeNextWaitTime() {
		long shortestTime = Long.MAX_VALUE;

		synchronized (this) {
			if (!this.scheduledTestTimes.isEmpty()) {
				shortestTime = this.scheduledTestTimes.peek().getTS();
			}
		}

		long wait = shortestTime - System.currentTimeMillis() + 500;
		return Math.max(wait, 500);
	}

	/*
	 * THIS MUST BE CALLED FROM INSIDE A SYNCHRNOZIED(THIS) BLOCK
	 */
	private boolean startTest(SanatizedRecord toTest) {
		if (this.pendingTests.contains(toTest)) {
			return false;
		}

		PeerRecord tRecord = this.myParent.getRecord(toTest);

		/*
		 * XXX LOG IF WE'RE TRYING ADDERS TOO FAST?
		 */

		if (tRecord.attemptConnectionStart()) {
			this.pendingTests.add(toTest);
			this.availTests--;

			/*
			 * Actually spin the test up
			 */
			PeerAddress addrObj = toTest.getPeerAddressObject();
			Peer peerObj = new Peer(this.myParent.getParams(), new VersionMessage(this.myParent.getParams(), 0),
					addrObj, null, false);
			peerObj.registerAddressConsumer(this.myParent);
			ConnTestSlave testSlave = new ConnTestSlave(peerObj, this);
			ListenableFuture<SocketAddress> connFuture = this.myParent.getRandomNIOClient()
					.openConnection(addrObj.getSocketAddress(), peerObj);
			Futures.addCallback(connFuture, testSlave, this.connTestPool);
			return true;

		} else {
			return false;
		}

	}

	public void logSummary() {
		synchronized (this) {
			this.myParent.logEvent("CONNSTATUS," + this.pendingTests.size() + "," + this.priorityTests.size() + ","
					+ this.scheduledPeers.size(), Manager.CRIT_LOG_LEVEL);
		}
	}

	private void handleFailedTest(SanatizedRecord failed, String reason, String message) {
		/*
		 * Record the failure time and remove pending test
		 */
		PeerRecord theRec = this.myParent.getRecord(failed);
		synchronized (this) {
			theRec.signalConnectionFailed();
			this.pendingTests.remove(failed);
			if (!this.scheduledPeers.contains(failed)) {
				/*
				 * Figure out what our delay should be
				 */
				long delay = -1;
				if (theRec.isOrHasEverConnected()) {
					delay = ConnectionTester.RECONNECT_TRY_SEC;
				} else {
					delay = ConnectionTester.RETEST_TRY_SEC;
				}
				failed.updateTS(System.currentTimeMillis() + delay * 1000);

				/*
				 * Actually reschedule the next test
				 */
				this.scheduledPeers.add(failed);
				this.scheduledTestTimes.add(failed);
			}
		}
		this.myParent.logEvent(message + "," + failed.toString() + "," + reason, Manager.DEBUG_LOG_LEVEL);
	}

	public void reportTCPFailure(Peer failedPeer, String reason) {
		/*
		 * Report the open test slot
		 */
		this.eventQueue.add(ConnectionEvent.AVAILTEST);

		try {
			SanatizedRecord failed = new SanatizedRecord(failedPeer.getAddress());
			this.handleFailedTest(failed, reason, "tcpfailed");
		} catch (Throwable e) {
			this.myParent.logException(e);
		}
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
		 * Report the open test slot
		 */
		this.eventQueue.add(ConnectionEvent.AVAILTEST);

		try {
			/*
			 * Force the connection closed
			 */
			failedPeer.close();

			/*
			 * Record the failure time and remove pending test
			 */
			SanatizedRecord failed = new SanatizedRecord(failedPeer.getAddress());
			this.handleFailedTest(failed, reason, "versionfailed");
		} catch (Throwable e) {
			this.myParent.logException(e);
		}
	}

	public void reportWorkingPeer(Peer workingPeer) {
		this.eventQueue.add(ConnectionEvent.AVAILTEST);

		try {
			SanatizedRecord tRec = new SanatizedRecord(workingPeer.getAddress());
			this.myParent.getRecord(tRec).signalConnected();
			synchronized (this) {
				this.pendingTests.remove(tRec);
			}

			this.myParent.resolvedStartedPeer(workingPeer);
			//TODO thread pool?
			workingPeer.addConnectionEventListener(new DeadPeerListener(this.myParent));
			this.myParent.logEvent("conn," + tRec.toString(), Manager.CRIT_LOG_LEVEL);
		} catch (Throwable e) {
			this.myParent.logException(e);
		}
	}
}
