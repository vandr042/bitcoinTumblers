package listeners;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bitcoinj.core.AddressMessage;
import org.bitcoinj.core.AddressUser;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;

import control.AddressHarvest;

public class BurstableHarvester implements Runnable, AddressUser {

	private Peer myTarget;
	private AddressHarvest myParent;

	private boolean pending;

	private int roundGoal;
	private int roundDone;

	private Set<PeerAddress> responseSet;
	private List<Long> delays;

	private long lastStartTime;

	private static final int MAX_ROUNDS = 20;
	private static final long DEAD_PEER_TIMEOUT_SEC = 300;

	public BurstableHarvester(Peer target, AddressHarvest parent) {
		this.myTarget = target;
		this.myParent = parent;
		this.pending = true;

		this.roundGoal = BurstableHarvester.MAX_ROUNDS;
		this.roundDone = 0;

		this.lastStartTime = 0;

		this.responseSet = new HashSet<PeerAddress>();
		this.delays = new LinkedList<Long>();
	}

	@Override
	public void run() {
		try {
			if (!this.reachedGoal()) {
				/*
				 * Log delays since we might be interested in them
				 */
				long nextRoundTime = System.currentTimeMillis() / 1000;
				if (this.lastStartTime != 0) {
					this.delays.add(nextRoundTime - this.lastStartTime);
				}
				this.lastStartTime = nextRoundTime;

			} else {
				// TODO clean up the fact that we're done
			}

			while (!this.reachedGoal()) {
				this.roundDone++;
				long startTime = System.currentTimeMillis() / 1000;
				AddressMessage addrMessage = this.myTarget.getAddr().get(BurstableHarvester.DEAD_PEER_TIMEOUT_SEC,
						TimeUnit.SECONDS);
				long endTime = System.currentTimeMillis() / 1000;
				this.delays.add(endTime - startTime);
				this.responseSet.addAll(addrMessage.getAddresses());
			}
		} catch (TimeoutException | ExecutionException | InterruptedException e) {
			// TODO should prob note this
		}
	}

	private boolean reachedGoal() {
		return this.roundDone >= this.roundGoal;
	}

	@Override
	public void getAddresses(AddressMessage m, Peer myPeer) {
		synchronized (this) {
			this.responseSet.addAll(m.getAddresses());
			this.myParent.restartBurst(this);
		}
	}

}
