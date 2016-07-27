package peerLink;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class PLStats {

	private HashMap<String, String> truthTxToPeer = new HashMap<String, String>();
	
	public PLStats(String filename) throws IOException{
		MapBuilder.buildMapFromTruth(filename, truthTxToPeer);
	}

	public int checkFirstPeer(String tx, HashSet<String> conns){
		if (conns.contains(truthTxToPeer.get(tx))){
			return 1;
		}else{ 
			return 0;
		}
	}	

	public float checkLinks(HashMap<String, String> txLinks){
		float totalTx, correct;
		totalTx = correct = 0;
		
		Set<String> keys = txLinks.keySet();
		for (String k : keys){
			totalTx++;
			String peer = txLinks.get(k);
			if (peer.compareTo(truthTxToPeer.get(k)) == 0){
				correct++;
			}
		}
		return (correct/totalTx);
	}
}
