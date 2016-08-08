package peerLink;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

public class PLStats {

	private float avgSuspectSize;
	private float totalTx, correct;

	private HashMap<String, String> truthTxToPeer = new HashMap<String, String>();
	
	public PLStats(String filename) throws IOException{
		MapBuilder.buildMapFromTruth(filename, truthTxToPeer);
	}

	public int checkFirstPeerConns(String tx, HashSet<String> conns){
		if (conns.contains(truthTxToPeer.get(tx))){
			return 1;
		}else{ 
			return 0;
		}
	}	

	public float checkLinks(HashMap<String, List<String>> txLinks){
		totalTx = correct = avgSuspectSize = 0;
		
		Set<String> keys = txLinks.keySet();
		System.out.println("Key size: " + keys.size());
		List<String> suspectPeers;
		String peer;
	
		for (String k : keys){
			totalTx++;
			suspectPeers = txLinks.get(k);
			avgSuspectSize += suspectPeers.size();
			Iterator<String> li = suspectPeers.iterator();
			while (li.hasNext()){
				peer = li.next();			
				if (peer.compareTo(truthTxToPeer.get(k)) == 0){
					correct++;
					break;
				}
			}
		}
		System.out.println("totalTx: " + totalTx);
	
		avgSuspectSize = avgSuspectSize/totalTx;
		return (correct/totalTx);
	}

	public float getAvgSuspectSize(){
		return avgSuspectSize;	
	}

}
