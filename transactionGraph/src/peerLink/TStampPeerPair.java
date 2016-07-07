package peerLink;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;

public class TStampPeerPair implements Comparable<TStampPeerPair>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long tstamp;
	private String peer;

	public TStampPeerPair(String ts, String p) {
		tstamp = Long.parseLong(ts);
		peer = p;
	}

	@Override
	/*
	 * Compares two string time stamps by converting them to longs and using
	 * comparison operators arg: takes in TStampPeerPair object
	 * 
	 */
	public int compareTo(TStampPeerPair rhsPair) {
		if (this.tstamp < rhsPair.tstamp) {
			return -1;
		} else if (this.tstamp == rhsPair.tstamp) {
			return 0;
		}
		return 1; // ts1Long > ts2Long
	}

	public long getTimeStamp() {
		return this.tstamp;
	}

	public String getPeer() {
		return this.peer;
	}

	public static void main(String[] args) {
		TStampPeerPair tspp = new TStampPeerPair("12345", "12345");
		TStampPeerPair tspp2 = new TStampPeerPair("12346", "12346");
		TStampPeerPair tspp3 = new TStampPeerPair("12344", "12346");
		LinkedList<TStampPeerPair> tsppList = new LinkedList<TStampPeerPair>();
		tsppList.add(tspp3);
		tsppList.add(tspp2);
		tsppList.add(tspp);
		for (TStampPeerPair tsp : tsppList) {
			String ts = Long.toString(tsp.getTimeStamp());
			System.out.println(ts);
		}
		Collections.sort(tsppList);
		for (TStampPeerPair tsp : tsppList) {
			String ts = Long.toString(tsp.getTimeStamp());
			System.out.println(ts);
		}

	}
}
