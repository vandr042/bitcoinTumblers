package data;

import java.util.HashMap;

import org.bitcoinj.core.PeerAddress;

import control.Manager;

public class PeerRecord {

	private PeerAddress myAddr;
	private long timeConnected;
	private long timeDisconnected;
	private long timeConnFailed;
	private long lastUptime;
	private HashMap<String, Long> peersWhoKnowMe;

	private Manager myParent;

	public PeerRecord(PeerAddress addr, Manager parent) {
		this.myAddr = addr;
		this.timeConnected = -1;
		this.timeDisconnected = -1;
		this.timeConnFailed = -1;
		this.lastUptime = 0;
		this.peersWhoKnowMe = new HashMap<String, Long>();
		this.myParent = parent;
	}

	public boolean equals(Object rhs) {
		if (!(rhs instanceof PeerRecord)) {
			return false;
		}

		return this.myAddr.toString().equals(((PeerRecord) rhs).myAddr.toString());
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

	public boolean addNodeWhoKnowsMe(PeerAddress nodeWhoKnows, Long ts) {
		boolean retFlag = false;
		String addrWhoKnowsStr = nodeWhoKnows.toString();
		synchronized (this.peersWhoKnowMe) {
			retFlag = this.peersWhoKnowMe.containsKey(addrWhoKnowsStr);
			if (retFlag && this.peersWhoKnowMe.get(addrWhoKnowsStr) != ts) {
				this.myParent.logEvent("TS update for " + this.myAddr.toString() + " from " + addrWhoKnowsStr
						+ "(" + this.peersWhoKnowMe.get(addrWhoKnowsStr) + "," + ts + ")");
			}
			this.peersWhoKnowMe.put(addrWhoKnowsStr, ts);
		}
		return retFlag;
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

	public HashMap<String, Long> getCopyOfNodesWhoKnow() {
		HashMap<String, Long> cloneMap = new HashMap<String, Long>();

		synchronized (this.peersWhoKnowMe) {
			for (String tAddr : this.peersWhoKnowMe.keySet()) {
				cloneMap.put(tAddr, this.peersWhoKnowMe.get(tAddr));
			}
		}

		return cloneMap;
	}
}
