package bitcoinLink;

public class TStampPeerPair implements Comparable{
	private String tstamp;
	private String peer;
	public TStampPeerPair(String ts, String p){
		tstamp = ts;
		peer = p;
	}
	
	@Override
	/* Compares two string time stamps by converting them to longs and using comparison operators 
	 * arg: takes in TStampPeerPair object
	 * 
	 */
	public int compareTo(Object arg0) {
		TStampPeerPair tspp = (TStampPeerPair) arg0;
		String ts1 = this.tstamp;
		String ts2 = tspp.tstamp;
		Long ts1Long = Long.valueOf(ts1).longValue();
		Long ts2Long = Long.valueOf(ts2).longValue();
		if (ts1Long < ts2Long){
			return -1;
		}else if (ts1Long == ts2Long){
			return 0;
		}
		return 1; // ts1Long > ts2Long 
	}
	
	public String getTimeStamp(){
		return this.tstamp;
	}
	
	public String getPeer(){
		return this.peer;
	}
}
