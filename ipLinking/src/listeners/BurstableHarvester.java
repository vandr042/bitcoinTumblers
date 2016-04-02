package listeners;

import java.util.*;
import java.util.concurrent.Semaphore;

import org.bitcoinj.core.AddressMessage;
import org.bitcoinj.core.AddressUser;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;

import control.AddressHarvest;
import data.SanatizedRecord;

public class BurstableHarvester implements Runnable, AddressUser {

	private SanatizedRecord myRecord;
	private Peer myTarget;
	private AddressHarvest myParent;

	private Semaphore advance;

	private int roundGoal;
	private int roundDone;

	private HashSet<SanatizedRecord> responses;
	private List<Long> delays;
	private List<Integer> newRecords;

	private static final int MAX_ROUNDS = 60;

	public BurstableHarvester(SanatizedRecord myRec, Peer target, AddressHarvest parent) {
		this.myRecord = myRec;
		this.myTarget = target;
		this.myParent = parent;

		this.advance = new Semaphore(0);

		this.roundGoal = BurstableHarvester.MAX_ROUNDS;
		this.roundDone = 0;

		this.responses = new HashSet<SanatizedRecord>();
		this.delays = new ArrayList<Long>();
		this.newRecords = new ArrayList<Integer>();
	}

	@Override
	public void run() {
		boolean noErrors = true;
		this.myTarget.registerAddressConsumer(this);

		try {
			while (!this.reachedGoal()) {
				this.roundDone++;
				long startTime = System.currentTimeMillis();
				this.myTarget.getAddr();
				this.advance.acquire();
				long endTime = System.currentTimeMillis();
				this.delays.add(endTime - startTime);
			}
		} catch (InterruptedException e) {
			/*
			 * Log the fact that this harvester was killed and then die silently
			 */
			this.myParent.noteKilledHarvester(this.myRecord);
			noErrors = false;
		}

		/*
		 * Report results if no errors happened
		 */
		this.myParent.reportFinishedBurst(this, noErrors);
	}

	private boolean reachedGoal() {
		return this.roundDone >= this.roundGoal;
	}

	@Override
	public void getAddresses(AddressMessage m, Peer myPeer) {
		int tempNew = 0;
		synchronized (this) {
			for (PeerAddress tAddr : m.getAddresses()) {
				// TODO how do we actually want to handle updated time stamps?
				SanatizedRecord tRec = new SanatizedRecord(tAddr);
				if (this.responses.add(tRec)) {
					tempNew++;
				}
			}
			this.newRecords.add(tempNew);
			this.advance.release();
		}
	}

	public SanatizedRecord getMyRecord() {
		return this.myRecord;
	}

	public Peer getTarget() {
		return this.myTarget;
	}

	public HashSet<SanatizedRecord> getResponses() {
		return this.responses;
	}

	public List<Long> getInterMsgIntervals() {
		return this.delays;
	}

	public List<Integer> getNewRecordsPerRound() {
		return this.newRecords;
	}

	public Long getTotalTime() {
		long sum = 0;
		for (Long tDelay : this.delays) {
			sum += tDelay;
		}
		return sum;
	}

}
