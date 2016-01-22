package data;

import org.bitcoinj.core.Peer;

public class PeerTimePair implements Comparable<PeerTimePair> {

	private Peer myPeer;
	private long myTime;

	public PeerTimePair(Peer peer, long time) {
		this.myPeer = peer;
		this.myTime = time;
	}
	
	public Peer getPeer(){
		return this.myPeer;
	}
	
	public long getTime(){
		return this.myTime;
	}

	public int hashCode(){
		return this.myPeer.getAddress().toString().hashCode();
	}
	
	public boolean equals(Object rhs) {
		if (!(rhs instanceof PeerTimePair)) {
			return false;
		}

		PeerTimePair rhsObj = (PeerTimePair) rhs;
		return this.myPeer.getAddress().toString().equals(rhsObj.myPeer.getAddress().toString());
	}

	public int compareTo(PeerTimePair rhs) {
		if (this.myTime < rhs.myTime) {
			return -1;
		} else if (this.myTime > rhs.myTime) {
			return 1;
		} else {
			return 0;
		}
	}

}
