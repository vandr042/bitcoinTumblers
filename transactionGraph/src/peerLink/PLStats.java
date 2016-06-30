package peerLink;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class PLStats {

	private HashMap<String, String> truthTxToPeer = new HashMap<String, String>();
	
	public PLStats(String filename) throws IOException{
		MapBuilder.buildMapFromTruth(filename, truthTxToPeer);
	}
	public float checkFirstPeerDirConn(HashMap<String, List<PeerCountPair>> txLinks){
		float totalTx, firstCorrect;
		totalTx = firstCorrect = 0;
		
		Set<String> keys = txLinks.keySet();
		for (String k : keys){
			totalTx++;
			if (txLinks.get(k).get(0).getPeer().equals(truthTxToPeer.get(k))){
				firstCorrect++;
			}
		}
		return (firstCorrect/totalTx);
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
