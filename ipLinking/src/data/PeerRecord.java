package data;

import java.util.HashMap;

import control.Manager;

public class PeerRecord {

	private SanatizedRecord myAddr;
	private long timeConnected;
	private long timeDisconnected;
	private long timeConnFailed;
	private long lastUptime;
	private HashMap<SanatizedRecord, Long> peersWhoKnowMe;
	private volatile boolean introducedToConnTester;

	private Manager myParent;

	public PeerRecord(SanatizedRecord addr, Manager parent) {
		this.myAddr = addr;
		this.timeConnected = -1;
		this.timeDisconnected = -1;
		this.timeConnFailed = -1;
		this.lastUptime = 0;
		this.peersWhoKnowMe = new HashMap<SanatizedRecord, Long>();
		this.introducedToConnTester = false;
		this.myParent = parent;
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

	public void addNodeWhoKnowsMe(SanatizedRecord nodeWhoKnows, Long ts) {
		synchronized (this.peersWhoKnowMe) {
			if (this.peersWhoKnowMe.containsKey(nodeWhoKnows) && (!this.peersWhoKnowMe.get(nodeWhoKnows).equals(ts))) {
				this.myParent.logEvent("TS update for " + this.myAddr.toString() + " from " + nodeWhoKnows.toString()
						+ "(" + this.peersWhoKnowMe.get(nodeWhoKnows) + "," + ts + ")", Manager.CRIT_LOG_LEVEL);
			}
			this.peersWhoKnowMe.put(nodeWhoKnows, ts);
		}
	}
	
	public void flagAsPassedToConnTester(){
		this.introducedToConnTester = true;
	}
	
	public boolean connTesterKnows(){
		return this.introducedToConnTester;
	}

	public SanatizedRecord getMyAddr() {
		return myAddr;
	}

	public long getTimeConnected() {
		return timeConnected;
	}

	public long getTimeDisconnected() {
		return timeDisconnected;
	}

	public boolean isOrHasEverConnected() {
		return this.timeDisconnected != -1 || this.timeConnected != -1;
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

	public HashMap<SanatizedRecord, Long> getCopyOfNodesWhoKnow() {
		HashMap<SanatizedRecord, Long> cloneMap = new HashMap<SanatizedRecord, Long>();

		synchronized (this.peersWhoKnowMe) {
			for (SanatizedRecord tAddr : this.peersWhoKnowMe.keySet()) {
				cloneMap.put(tAddr, this.peersWhoKnowMe.get(tAddr));
			}
		}

		return cloneMap;
	}
}
