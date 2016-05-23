package peerLink;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PLCombiner {
	/* mode: 1 for voting 0 for intersection */
	private String[] scriptArr;
	private int mode;
	private int depth;
	private HashMap<String, HashMap<String, PeerCountPair>> txToPeers;
	BufferedWriter bw;
	public PLCombiner(String[] scripts, int simMode, int searchDepth, String outfile) throws IOException{
		scriptArr = scripts;
		mode = simMode;
		depth = searchDepth;
		txToPeers = new HashMap<String, HashMap<String, PeerCountPair>>();
		File f = new File(outfile);
		bw = new BufferedWriter(new FileWriter(f));
	}
	
	public void combine() throws IOException{
		if (mode == 0){
	
		}else if(mode == 1){
			combineVoting();
		}
	}
	
	private void combineVoting() throws IOException{
		for (int i = 0; i < scriptArr.length-2; i++){
			HashMap<String, PeerCountPair> peerToPCP;
			String txID;
			File f = new File(scriptArr[i]);
			BufferedReader reader = new BufferedReader(new FileReader(f));

			/* pattern match each line to build tx to peer map */
			System.out.println("Building maps from file...");
		
			String line = reader.readLine();
			while (line != null) {
		
				String[] addrs = line.split(",");
				txID = addrs[0];
				
				/* If tx isn't in map, put it in the map */
				peerToPCP = txToPeers.get(txID);
				if (peerToPCP == null){
					peerToPCP = new HashMap<String, PeerCountPair>();
					txToPeers.put(txID, peerToPCP);
				}
				
				PeerCountPair pcp;
				
				for (int j = 1; j < addrs.length; j++){
					pcp  = peerToPCP.get(addrs[j]);
					if (pcp != null){
						pcp.seen();
					}else{
						pcp = new PeerCountPair(addrs[j], 1);
						peerToPCP.put(addrs[j], pcp);
					}
				}
				line = reader.readLine();
			}
			reader.close();
		}
		
		/* Find the most common peers */ 
		Set<String> transactions = txToPeers.keySet();
		int highCount, currCount, j;
		j= 1;
		for (String tx: transactions){
			List<String> mostCommonPeers;
			HashMap<String, PeerCountPair> pcpMap = txToPeers.get(tx);
			List<PeerCountPair> counts = (ArrayList<PeerCountPair>)pcpMap.values();
			Collections.sort(counts);

			
			highCount = currCount = counts.get(0).getCount();
			while (currCount == highCount && j < counts.size()){
				currCount = counts.get(j).getCount();
				j++;
			}
			counts = counts.subList(0, j);
			mostCommonPeers = new ArrayList<String>();
			for (PeerCountPair pcp:counts){
				String peer = pcp.getPeer();
				mostCommonPeers.add(peer);
			}
			this.writeResults(tx, mostCommonPeers);
		}
		
	}

	/* TODO implement intersection method */
	private void combineIntersection(){
		
	}
	
	private void writeResults(String txID, List<String> mostCommonPeers) throws IOException{
		bw.write(txID);
		for (String p: mostCommonPeers){
			bw.write(", " + p);
		}
		bw.write("\n");
	}
	/* 
	 * Run the combiner from the command line by supplying 
	 * any number of script arguments followed by the mode
	 * and the search depth, in that order.
	 */
	public static void main(String args[]) throws NumberFormatException, IOException{
		PLCombiner plc = new PLCombiner(args, Integer.parseInt(args[args.length-2]), Integer.parseInt(args[args.length-1]), "outfile.txt");
		
	}
}