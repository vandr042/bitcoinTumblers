package control;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;

import data.PeerTimePair;
import listeners.BurstableHarvester;

public class AddressHarvest implements Runnable {

	private Manager myParent;
	private Random rng;

	private PriorityBlockingQueue<PeerTimePair> toTestQueue;

	private Set<String> blackListSet;
	private Set<String> runningSet;

	private ThreadPoolExecutor burstThreadPool;

	private static final long MIN_HARVEST_INTERVAL_SEC = 15;

	private static final boolean LOG_ADDR_TIMING = true;

	public AddressHarvest(Manager parent) {
		this.myParent = parent;
		this.rng = new Random();

		this.toTestQueue = new PriorityBlockingQueue<PeerTimePair>();
		this.blackListSet = new HashSet<String>();
		this.runningSet = new HashSet<String>();

		this.burstThreadPool = new ThreadPoolExecutor(1, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	}

	public void giveNewHarvestTarget(Peer newPeer, boolean jitter) {
		long ts = System.currentTimeMillis();
		if (jitter) {
			ts += this.rng.nextDouble() * AddressHarvest.MIN_HARVEST_INTERVAL_SEC * 1000;
			this.myParent.logEvent("New harvest target " + newPeer.getAddress());
		}
		PeerTimePair tmpPair = new PeerTimePair(newPeer, ts);
		this.toTestQueue.put(tmpPair);
	}

	public void poisonPeer(PeerAddress addr) {
		synchronized (this) {
			this.blackListSet.add(addr.toString());
		}
	}

	public void startNewBurstHarvest(Peer targetPeer) {
		boolean okToStart = false;
		synchronized (this) {
			if (!this.runningSet.contains(targetPeer.getAddress().toString())) {
				this.runningSet.add(targetPeer.getAddress().toString());
				okToStart = true;
			}
		}

		// TODO do we want a thread pool of some type for this work?
		if (okToStart) {
			this.myParent.logEvent("Addr burst harvest started for " + targetPeer.getAddress());
			BurstableHarvester burstChild = new BurstableHarvester(targetPeer, this);
			Thread burstThread = new Thread(burstChild);
			burstThread.start();
		}
	}

	public void reportFinishedBurst(BurstableHarvester harv) {

		/*
		 * Step one, we're not running, so remove it from the running set
		 */
		synchronized (this) {
			this.runningSet.remove(harv.getTarget().getAddress().toString());
		}

		/*
		 * Step two, point all future incoming ADDR messages to the Manager and
		 * then push the harvest to the parent
		 */
		harv.getTarget().registerAddressConsumer(this.myParent);
		this.myParent.getBurstResults(harv.getTarget().getAddress(), harv.getResponses());

		/*
		 * Log how long this took us
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
			this.myParent.logEvent(logStrBuild.toString());
		}
	}

	@Override
	public void run() {
		while (true) {

			try {
				/*
				 * Get the next peer to query, skipping any that are black
				 * listed (crashed)
				 */
				PeerTimePair tmpPair = null;
				while (tmpPair == null) {
					tmpPair = this.toTestQueue.take();
					synchronized (this.blackListSet) {
						if (this.blackListSet.remove(tmpPair.getPeer().getAddress().toString())) {
							tmpPair = null;
						}
					}
				}

				/*
				 * This is the next peer to update, if we can't do it now we
				 * know there is no node in the queue we can, block until this
				 * one is good to go
				 */
				long timeSinceLastHarvest = System.currentTimeMillis() - tmpPair.getTime();
				if (timeSinceLastHarvest < AddressHarvest.MIN_HARVEST_INTERVAL_SEC * 1000) {
					Thread.sleep(AddressHarvest.MIN_HARVEST_INTERVAL_SEC * 1000 - timeSinceLastHarvest);
				}

				
				this.startNewBurstHarvest(tmpPair.getPeer());
			} catch (Exception e) {
				this.myParent.logException(e.getLocalizedMessage());
			}
		}
	}

}
