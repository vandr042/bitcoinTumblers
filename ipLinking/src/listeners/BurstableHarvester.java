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

	private Peer myTarget;
	private AddressHarvest myParent;

	private Semaphore advance;

	private int roundGoal;
	private int roundDone;

	private HashSet<SanatizedRecord> responses;
	private List<Long> delays;
	private List<Integer> newRecords;

	private static final int MAX_ROUNDS = 60;

	public BurstableHarvester(Peer target, AddressHarvest parent) {
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
		this.myTarget.registerAddressConsumer(this);
		// TODO there is an issue here where if the peer dies during this, the
		// thread gets stuck running forever, fix that with a timeout?
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
			// TODO should prob note this
		}

		// ET Phone Home...
		this.myParent.reportFinishedBurst(this);
	}

	private boolean reachedGoal() {
		return this.roundDone >= this.roundGoal;
	}

	@Override
	public void getAddresses(AddressMessage m, Peer myPeer) {
		int tempNew = 0;
		synchronized (this) {
			for(PeerAddress tAddr: m.getAddresses()){
				//TODO how do we actually want to handle updated time stamps?
				SanatizedRecord tRec = new SanatizedRecord(tAddr);
				if(this.responses.add(tRec)){
					tempNew++;
				}
			}
			this.newRecords.add(tempNew);
			this.advance.release();
		}
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
	
	public List<Integer> getNewRecordsPerRound(){
		return this.newRecords;
	}
	
	public Long getTotalTime(){
		long sum = 0;
		for(Long tDelay: this.delays){
			sum += tDelay;
		}
		return sum;
	}

}
