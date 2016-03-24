package bitcoinLink;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class PeerLinkTester{
	private int depthStep;
	private PeerLink peerLink;
	private int maxDepth;
	public PeerLinkTester(int stepSize, PeerLink pl, int depth){
		depthStep = stepSize;
		peerLink = pl;
		maxDepth = depth;
	}
	
	public void testForBestDepth() throws IOException{
		File f = new File("peerLinkSizeTest.log");
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		for (int searchDepth = depthStep; searchDepth < maxDepth; searchDepth += depthStep){
			int averagePeersPerSet;
			int numPeers = 0;
			int numSets = 0;
			HashMap <String, HashSet<String>> txToPeers = peerLink.sim(searchDepth);
			LinkedList<HashSet<String>> peerSetList = (LinkedList<HashSet<String>>) txToPeers.values();
			for (HashSet<String> pset:peerSetList){
				numPeers += pset.size();
				numSets += 1;
			}
			averagePeersPerSet = numPeers/numSets;
			bw.write("Depth: " + searchDepth + "AveragePeersPerSet: " + averagePeersPerSet + "\n");
		}
	}
	
	public void test(){
		
	}
	
	public static void main(String[] args) throws IOException{
		PeerLink pl = new PeerLink("/home/connor/workspace/bitcoinTumblers/miscScripts/peer-finder-synth-out.log");
		PeerLinkTester test = new PeerLinkTester(2,pl,10);
		test.testForBestDepth();
	}
}
