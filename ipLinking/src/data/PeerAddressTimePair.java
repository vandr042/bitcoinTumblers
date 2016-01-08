package data;

import org.bitcoinj.core.PeerAddress;

public class PeerAddressTimePair implements Comparable<PeerAddressTimePair> {

	private PeerAddress myAddr;
	private long myTime;

	public PeerAddressTimePair(PeerAddress addr, long time) {
		this.myAddr = addr;
		this.myTime = time;
	}
	
	public PeerAddress getAddress(){
		return this.myAddr;
	}
	
	public long getTime(){
		return this.myTime;
	}

	public int hashCode(){
		return this.myAddr.hashCode();
	}
	
	public boolean equals(Object rhs) {
		if (!(rhs instanceof PeerAddressTimePair)) {
			return false;
		}

		PeerAddressTimePair rhsObj = (PeerAddressTimePair) rhs;
		return this.myAddr.equals(rhsObj.myAddr);
	}

	public int compareTo(PeerAddressTimePair rhs) {
		if (this.myTime < rhs.myTime) {
			return -1;
		} else if (this.myTime > rhs.myTime) {
			return 1;
		} else {
			return 0;
		}
	}

}
