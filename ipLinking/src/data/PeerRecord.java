package data;

import java.util.HashMap;

import org.bitcoinj.core.PeerAddress;

public class PeerRecord {

	private PeerAddress myAddr;
	private long timeConnected;
	private long timeDisconnected;
	private long timeConnFailed;
	private long lastUptime;
	private HashMap<PeerAddress, Long> peersWhoKnowMe;

	public PeerRecord(PeerAddress addr) {
		this.myAddr = addr;
		this.timeConnected = -1;
		this.timeDisconnected = -1;
		this.timeConnFailed = -1;
		this.lastUptime = 0;
		this.peersWhoKnowMe = new HashMap<PeerAddress, Long>();
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

			this.timeDisconnected = System.currentTimeMillis();
			this.lastUptime = this.timeDisconnected - this.timeConnected;
			this.timeConnected = -1;
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

	public void addNodeWhoKnowsMe(PeerAddress nodeWhoKnows, Long ts) {
		synchronized (this.peersWhoKnowMe) {
			this.peersWhoKnowMe.put(nodeWhoKnows, ts);
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

	public long getLastUptime() {
		if (this.timeConnected != -1) {
			return System.currentTimeMillis() - this.timeConnected;
		} else {
			return this.lastUptime;
		}
	}

	public long getTimeConnFailed() {
		return timeConnFailed;
	}

	public HashMap<PeerAddress, Long> getCopyOfNodesWhoKnow() {
		HashMap<PeerAddress, Long> cloneMap = new HashMap<PeerAddress, Long>();

		synchronized (this.peersWhoKnowMe) {
			for (PeerAddress tAddr : this.peersWhoKnowMe.keySet()) {
				cloneMap.put(tAddr, this.peersWhoKnowMe.get(tAddr));
			}
		}

		return cloneMap;
	}
}
