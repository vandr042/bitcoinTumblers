package control;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.bitcoinj.core.Peer;

import data.SanatizedRecord;
import data.WaitMap;

public class AddressHarvest implements Runnable {

	private Manager myParent;
	private Random rng;

	private WaitMap<SanatizedRecord> nextHarvestMap;

	private Semaphore waitSem;

	private static final long NORMAL_RESTART_INTERVAL_SEC = 30;

	public AddressHarvest(Manager parent) {
		this.myParent = parent;
		this.rng = new Random();

		this.nextHarvestMap = new WaitMap<SanatizedRecord>();
		this.waitSem = new Semaphore(0);
	}

	public void giveNewHarvestTarget(SanatizedRecord newPeer, boolean jitter) {
		long ts = System.currentTimeMillis();
		if (jitter) {
			ts += this.rng.nextDouble() * AddressHarvest.NORMAL_RESTART_INTERVAL_SEC * 1000;
			this.myParent.logEvent("HARVEST-NEW," + newPeer.toString() + "," + ts, Manager.DEBUG_LOG_LEVEL);
		}

		synchronized (this) {
			if (this.nextHarvestMap.updateObject(newPeer, ts)) {
				this.waitSem.release();
			}
		}
	}

	public void poisonPeer(SanatizedRecord addr) {
		synchronized (this) {
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
	 * MUST BE CALLED IN A SYNCH BLOCK
	 */
	private void startNewHarvest(SanatizedRecord theRecord) {
		/*
		 * Slightly concerned with us getting a null peer back magically, so
		 * test and log if it happens to the exception log
		 */
		Peer targetPeer = this.myParent.getPeerObject(theRecord);
		if (targetPeer != null) {
			this.myParent.logEvent("HARVEST-START," + theRecord.toString(), Manager.DEBUG_LOG_LEVEL);
			targetPeer.getAddr();
			this.nextHarvestMap.updateObject(theRecord,
					System.currentTimeMillis() + AddressHarvest.NORMAL_RESTART_INTERVAL_SEC * 1000);
		} else {
			this.myParent.logException(new NullPointerException("Tried to start harvest, but got null peer."));
		}
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
							this.startNewHarvest(toStart);
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
