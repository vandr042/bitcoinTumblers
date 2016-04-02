package control;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.bitcoinj.core.Peer;

import data.SanatizedRecord;
import data.WaitMap;
import listeners.BurstableHarvester;

public class AddressHarvest implements Runnable {

	private Manager myParent;
	private Random rng;

	private WaitMap<SanatizedRecord> nextHarvestMap;
	private HashMap<SanatizedRecord, Thread> harvesterThreads;

	private Semaphore waitSem;

	private static final long NORMAL_RESTART_INTERVAL_SEC = 1800;

	private static final boolean LOG_ADDR_TIMING = false;
	private static final boolean LOG_UNSEEN_STATS = true;

	public AddressHarvest(Manager parent) {
		this.myParent = parent;
		this.rng = new Random();

		this.nextHarvestMap = new WaitMap<SanatizedRecord>();
		this.harvesterThreads = new HashMap<SanatizedRecord, Thread>();

		this.waitSem = new Semaphore(0);
	}

	public void giveNewHarvestTarget(SanatizedRecord newPeer, boolean jitter) {
		long ts = System.currentTimeMillis();
		if (jitter) {
			ts += this.rng.nextDouble() * AddressHarvest.NORMAL_RESTART_INTERVAL_SEC * 1000;
			this.myParent.logEvent("HARVEST-NEW," + newPeer.toString() + "," + ts, Manager.DEBUG_LOG_LEVEL);
		}

		synchronized (this) {
			/*
			 * I'm not 100% sure that this scenario happening is an incorrectly
			 * handled thing. If the Peer D/Cs, then reconnects before the
			 * harvest thread notices the interrupt flag then this could happen,
			 * for now we're going to log this as an illegal state, but allow
			 * the addition because of the above scenario, but I'm guessing this
			 * is an exception in the error log that can be ignored
			 */
			if (this.harvesterThreads.containsKey(newPeer)) {
				this.myParent.logException(new IllegalStateException(
						"Tried to add newly connected peer to harvester, but we're harvesting, can probablly be ignored."));
			}

			if (this.nextHarvestMap.updateObject(newPeer, ts)) {
				this.waitSem.release();
			}
		}
	}

	public void poisonPeer(SanatizedRecord addr) {
		synchronized (this) {
			/*
			 * Wake up and kill any currently running harvester for that host
			 */
			if (this.harvesterThreads.containsKey(addr)) {
				this.harvesterThreads.get(addr).interrupt();
			}

			/*
			 * Purge all references to that host's wait time as we're not
			 * starting a harvester
			 */
			if (this.nextHarvestMap.deleteWait(addr)) {
				this.waitSem.release();
			}
		}
	}

	/*
	 * MUST BE CALLED IN A SYNCHRONIZED BLOCK!!!!
	 */
	private void startNewBurstHarvest(SanatizedRecord theRecord) {

		/*
		 * Step one, sanity check that we're not already running
		 */
		if (!this.harvesterThreads.containsKey(theRecord)) {

			/*
			 * Slightly concerned with us getting a null peer back magically, so
			 * test and log if it happens to the exception log
			 */
			Peer targetPeer = this.myParent.getPeerObject(theRecord);
			if (targetPeer != null) {
				this.myParent.logEvent("HARVEST-START," + theRecord.toString(), Manager.DEBUG_LOG_LEVEL);

				/*
				 * Create the harvester thread and remember it in case we have
				 * to interrupt it
				 */
				BurstableHarvester burstChild = new BurstableHarvester(theRecord, targetPeer, this);
				Thread burstThread = new Thread(burstChild, "Burst Harvest - " + theRecord.toString());
				this.harvesterThreads.put(theRecord, burstThread);

				/*
				 * Lastly start the harvester
				 */
				burstThread.start();
			} else {
				this.myParent.logException(new NullPointerException("Tried to start harvest, but got null peer."));
			}
		}

	}

	public void reportFinishedBurst(BurstableHarvester harv, boolean restart) {

		/*
		 * Step one, we're not running, so remove it from the running set
		 */
		synchronized (this) {
			this.harvesterThreads.remove(harv.getMyRecord());
		}

		/*
		 * Step two, point all future incoming ADDR messages to the Manager and
		 * then push the harvest to the parent
		 */
		harv.getTarget().registerAddressConsumer(this.myParent);
		this.myParent.getBurstResults(new SanatizedRecord(harv.getTarget().getAddress()), harv.getResponses());

		/*
		 * Log do end of harvest logging
		 */
		if (AddressHarvest.LOG_ADDR_TIMING) {
			List<Long> timeDeltas = harv.getInterMsgIntervals();
			StringBuilder logStrBuild = new StringBuilder();
			logStrBuild.append("addr timing ");
			logStrBuild.append(harv.getTarget().getAddress().toString());
			for (int counter = 0; counter < timeDeltas.size(); counter++) {
				logStrBuild.append(",");
				logStrBuild.append(timeDeltas.get(counter).toString());
			}
			this.myParent.logEvent(logStrBuild.toString(), Manager.DEBUG_LOG_LEVEL);
		}
		if (AddressHarvest.LOG_UNSEEN_STATS) {
			List<Integer> newRecs = harv.getNewRecordsPerRound();
			StringBuilder unseenStrBuild = new StringBuilder();
			unseenStrBuild.append("unseen count ");
			unseenStrBuild.append(harv.getTarget().getAddress().toString());
			for (int counter = 0; counter < newRecs.size(); counter++) {
				unseenStrBuild.append(",");
				unseenStrBuild.append(newRecs.get(counter));
			}
			this.myParent.logEvent(unseenStrBuild.toString(), Manager.DEBUG_LOG_LEVEL);
		}
		this.myParent.logEvent("HARVEST-FINISH," + harv.getTarget().getAddress().toString() + "," + harv.getTotalTime(),
				Manager.DEBUG_LOG_LEVEL);

		/*
		 * Put the node's next harvest time into the queue if the harvester
		 * finished without error
		 */
		if (restart) {
			long nextHarvestTime = System.currentTimeMillis() + AddressHarvest.NORMAL_RESTART_INTERVAL_SEC * 1000;
			synchronized (this) {
				if (this.nextHarvestMap.updateObject(harv.getMyRecord(), nextHarvestTime)) {
					this.waitSem.release();
				}
			}
		}
	}

	public void noteKilledHarvester(SanatizedRecord tRec) {
		this.myParent.logEvent("HARVEST-KILLED," + tRec, Manager.DEBUG_LOG_LEVEL);
	}

	@Override
	public void run() {
		long waitTime = Long.MAX_VALUE;

		while (true) {

			try {
				/*
				 * Sleep until the sleep time finishes, or it needs to be
				 * updated, which we're told of via the semaphore
				 */
				this.waitSem.tryAcquire(waitTime, TimeUnit.MILLISECONDS);

				/*
				 * Walk through nodes in the "queue" (harvest map) finding those
				 * who are ready to start, and start them
				 */
				SanatizedRecord toStart = null;
				do {
					toStart = null;
					synchronized (this) {
						if (this.nextHarvestMap.getNextExpire() < System.currentTimeMillis()) {
							toStart = this.nextHarvestMap.popNext();
						}
						if (toStart != null) {
							this.startNewBurstHarvest(toStart);
						}
					}
				} while (toStart != null);

				/*
				 * Update how long we're suppose to sleep for
				 */
				long newTime = this.nextHarvestMap.getNextExpire();
				waitTime = newTime - System.currentTimeMillis() + 500;
				waitTime = Math.max(waitTime, 500);
			} catch (Exception e) {
				this.myParent.logException(e);
			}
		}
	}

}
