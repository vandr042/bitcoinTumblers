package data;

import java.util.Set;
import java.util.HashSet;

import org.bitcoinj.core.PeerAddress;

public class PeerRecord {

	private PeerAddress myAddr;
	private long timeConnected;
	private long timeDisconnected;
	private long timeConnFailed;
	private Set<PeerAddress> peersWhoKnowMe;

	public PeerRecord(PeerAddress addr) {
		this.myAddr = addr;
		this.timeConnected = -1;
		this.timeDisconnected = -1;
		this.timeConnFailed = -1;
		this.peersWhoKnowMe = new HashSet<PeerAddress>();
	}

	public boolean equals(Object rhs) {
		if (!(rhs instanceof PeerRecord)) {
			return false;
		}

		return this.myAddr.equals(((PeerRecord) rhs).myAddr);
	}

	public void signalConnected() {
		synchronized (this) {
			if (this.timeConnected != -1) {
				throw new RuntimeException("Already connected to that address!");
			}

			this.timeConnected = System.currentTimeMillis();
			this.timeDisconnected = -1;
			this.timeConnFailed = -1;
		}
	}

	public void signalDisconnected() {
		synchronized (this) {
			if (this.timeConnected == -1) {
				throw new RuntimeException("Not connected to that address!");
			}

			this.timeConnected = -1;
			this.timeDisconnected = System.currentTimeMillis();
		}
	}

	public void signalConnectionFailed() {
		synchronized (this) {
			if (this.timeConnected != -1) {
				throw new RuntimeException("Already connected to that address!");
			}
			if (this.timeConnFailed != -2) {
				throw new RuntimeException("Not set to testing state!");
			}

			this.timeConnFailed = System.currentTimeMillis();
		}
	}

	public boolean attemptConnectionStart() {
		boolean retFlag;
		synchronized (this) {
			if (this.timeConnected == -1 && this.timeConnFailed != -2) {
				this.timeConnFailed = -2;
				retFlag = true;
			} else {
				retFlag = false;
			}
		}
		return retFlag;
	}

	public void addNodeWhoKnowsMe(PeerAddress nodeWhoKnows) {
		synchronized (this.peersWhoKnowMe) {
			this.peersWhoKnowMe.add(nodeWhoKnows);
		}
	}

	public PeerAddress getMyAddr() {
		return myAddr;
	}

	public long getTimeConnected() {
		return timeConnected;
	}

	public long getTimeDisconnected() {
		return timeDisconnected;
	}

	public long getTimeConnFailed() {
		return timeConnFailed;
	}
}
