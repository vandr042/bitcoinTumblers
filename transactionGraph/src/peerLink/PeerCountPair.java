package peerLink;


/*
 * This class is an ordered pair of 
 */
public class PeerCountPair implements Comparable<PeerCountPair>{
	
	private String peer;
	private int seenCount;
	
	public PeerCountPair(String aPeer, int timesSeen){
		peer = aPeer;
		seenCount = timesSeen;
	}
	
	public void seen(){
		seenCount+=1;
	}
	
	public int getCount(){
		return seenCount;
	}
	
	public String getPeer(){
		return peer;
	}

	@Override
	public int compareTo(PeerCountPair pcp) {
		int otherCount = pcp.getCount();
		if (seenCount > otherCount)
			return 1;
		else if (seenCount < otherCount)
			return -1;
		return 0;
	}

}