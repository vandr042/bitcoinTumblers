package bitcoinLink;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PeerLinkTester{
	/* depth of ~275 to get to 1 intersection */
	private HashMap<String, String> truthTxToPeer = new HashMap<String,String>(); 
	private int depthStep;
	private PeerLink peerLink;
	private int maxDepth;
	public PeerLinkTester(int stepSize, PeerLink pl, int depth, String truthFile) throws IOException{
		depthStep = stepSize;
		peerLink = pl;
		maxDepth = depth;
		buildMapFromTruth(truthFile);
	}
	
	public void testForBestDepth() throws IOException{
		System.out.println("********************************** Testing for Depth ************************************");
		File f = new File("peerLinkSizeTest.log");
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		for (int searchDepth = depthStep; searchDepth < maxDepth; searchDepth += depthStep){
			float accuracy;
			HashMap <String, HashSet<String>> txToPeers = peerLink.sim(searchDepth);
			accuracy = this.checkAccuracy(txToPeers);
			bw.write("Depth: " + searchDepth + " Accuracy: " + accuracy + "\n");
		}
		bw.close();
	}
	
	private float checkAccuracy(HashMap<String,HashSet<String>> txPeerMap){
		float accuracy, totalTx, correct;
		correct = 0;
		String[] txArr = new String[txPeerMap.keySet().size()];
		txPeerMap.keySet().toArray(txArr);
		totalTx = txArr.length;
		for (String tx:txArr){
			String peer = truthTxToPeer.get(tx);
			HashSet<String> possiblePeers = txPeerMap.get(tx);
			if (possiblePeers.contains(peer) == true){
				correct += 1;
			}
		}
		accuracy = correct/totalTx;
		return accuracy;
	}
	
	private void buildMapFromTruth(String filename) throws IOException{
		File f = new File(filename);
		BufferedReader reader = new BufferedReader(new FileReader(f));
		
		String regex = "\\[((?:\\d+\\.*){4})+\\]:\\d*,(\\d*),(\\d*)";
		Pattern p = Pattern.compile(regex);
	
		/* pattern match each line to build tx to peer map */
		System.out.println("Building maps from file...");
		String line = reader.readLine();
		while (line != null){
			Matcher m = p.matcher(line);
			m.matches();
			String addr = m.group(1);
			String txID = m.group(2);
			String timeStamp = m.group(3);
			
			/*System.out.println("txID: " + txID);
			System.out.println("addr: " + addr);
			System.out.println("time stamp: " + timeStamp);*/
			
			/* populate maps based on message type */
			truthTxToPeer.put(txID, addr);
			line = reader.readLine();
		}
		reader.close();
		//System.out.println("Finished building maps...");
	}
	
	public static void main(String[] args) throws IOException{
		PeerLink pl = new PeerLink("/home/connor/workspace/bitcoinTumblers/miscScripts/peer-finder-synth-out.log");
		PeerLinkTester test = new PeerLinkTester(1,pl,10, "/home/connor/workspace/bitcoinTumblers/miscScripts/peer-finder-synth-groundTruth.log");
		test.testForBestDepth();
		
	}
}
