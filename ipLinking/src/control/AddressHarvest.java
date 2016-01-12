package control;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;

import data.PeerTimePair;

public class AddressHarvest implements Runnable {

	private Manager myParent;
	private Random rng;

	private PriorityBlockingQueue<PeerTimePair> toTestQueue;
	private Set<PeerAddress> blackListSet;

	private static final long MIN_HARVEST_INTERVAL_SEC = 30;

	public AddressHarvest(Manager parent) {
		this.myParent = parent;
		this.rng = new Random();

		this.toTestQueue = new PriorityBlockingQueue<PeerTimePair>();
		this.blackListSet = new HashSet<PeerAddress>();
	}

	public void giveNewHarvestTarget(Peer newPeer, boolean jitter) {
		long ts = System.currentTimeMillis();
		if(jitter){
			ts += this.rng.nextDouble() * AddressHarvest.MIN_HARVEST_INTERVAL_SEC * 1000;
			this.myParent.logEvent("New harvest target " + newPeer.getAddress());
		}
		PeerTimePair tmpPair = new PeerTimePair(newPeer, ts);
		this.toTestQueue.put(tmpPair);
	}
	
	public void poisonPeer(PeerAddress addr){
		synchronized(this.blackListSet){
			this.blackListSet.add(addr);
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
						if (this.blackListSet.remove(tmpPair.getPeer().getAddress())) {
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

				/*
				 * Actually request addrs, SUPER IMPORTANT TO NOTE THIS WILL
				 * ALWAYS RETURN NULL
				 */
				this.myParent.logEvent("Addr harvest request for " + tmpPair.getPeer().getAddress());
				tmpPair.getPeer().getAddr();
			} catch (InterruptedException e) {
				this.myParent.logException(e.getLocalizedMessage());
			}
		}
	}

}
