package peerLink;

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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PeerLinkTester {
	private HashMap<String, String> truthTxToPeer = new HashMap<String, String>();
	private int depthStep;
	private PeerLink peerLink;
	private int maxDepth;
	private int tStampDepth;

	public PeerLinkTester(int stepSize, PeerLink pl, int depth, String truthFile, int tsDepth) throws IOException {
		depthStep = stepSize;
		tStampDepth = tsDepth;
		peerLink = pl;
		maxDepth = depth;
		buildMapFromTruth(truthFile);
	}

	/*
	 * testForBestDepth runs PeerLink.sim for a specified number of different
	 * depths and outputs the accuracies to file
	 */
	public void testForBestDepth(String outFile) throws IOException {
		System.out.println("********************************** Testing for Depth ************************************");
		File f = new File(outFile);
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		for (int searchDepth = depthStep; searchDepth < maxDepth; searchDepth += depthStep) {
			double avgSetSize, accuracy, totalSets, numPeers, nonZeroCount;
			numPeers = 0;
			nonZeroCount = 0;
			HashMap<String, HashSet<String>> txToPeers = peerLink.sim(searchDepth);
			Collection<HashSet<String>> peers = txToPeers.values();
			for (HashSet<String> pset : peers) {
				numPeers += pset.size();
				if (pset.size() > 0) {
					nonZeroCount += 1.0;
				}
			}
			totalSets = peers.size();
			avgSetSize = numPeers / totalSets;
			accuracy = this.checkAccuracy(txToPeers);
			bw.write("Depth: " + searchDepth + " Accuracy: " + accuracy + " avgSetSize: " + avgSetSize
					+ " non zero avg set size: " + (numPeers / nonZeroCount) + "\n");
		}
		bw.close();
	}
	
	public void testBestDepthWithVoting(String outFile) throws IOException {
		System.out.println("********************************** Testing for Depth ************************************");
		File f = new File(outFile);
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		for (int searchDepth = depthStep; searchDepth <= tStampDepth; searchDepth += depthStep) {
			double avgSetSize, accuracy, totalSets, numPeers, nonZeroCount;
			numPeers = 0;
			nonZeroCount = 0;
			HashMap<String, Set<String>> txToPeers = peerLink.simVoting(searchDepth);
			Collection<Set<String>> peers = txToPeers.values();
			for (Set<String> pset : peers) {
				numPeers += pset.size();
				if (pset.size() > 0) {
					nonZeroCount += 1.0;
				}
			}
			totalSets = peers.size();
			avgSetSize = numPeers / totalSets;
			accuracy = this.checkVotingAccuracy(txToPeers);
			bw.write("Depth: " + searchDepth + " Accuracy: " + accuracy + " avgSetSize: " + avgSetSize
					+ " non zero avg set size: " + (numPeers / nonZeroCount) + "\n");
		}
		bw.close();
	}
	
	private float checkVotingAccuracy(HashMap<String, Set<String>> txPeerMap){
		float accuracy, totalTx, correct;
		correct = 0;
		String[] txArr = new String[txPeerMap.keySet().size()];
		txPeerMap.keySet().toArray(txArr);
		totalTx = txArr.length;
		for (String tx : txArr) {
			String peer = truthTxToPeer.get(tx);
			Set<String> possiblePeers = txPeerMap.get(tx);
			if (possiblePeers.contains(peer) == true) {
				correct += 1;
			}
		}
		accuracy = correct / totalTx;
		return accuracy;
	}

	/* private helper */
	private float checkAccuracy(HashMap<String, HashSet<String>> txPeerMap) {
		float accuracy, totalTx, correct;
		correct = 0;
		String[] txArr = new String[txPeerMap.keySet().size()];
		txPeerMap.keySet().toArray(txArr);
		totalTx = txArr.length;
		for (String tx : txArr) {
			String peer = truthTxToPeer.get(tx);
			HashSet<String> possiblePeers = txPeerMap.get(tx);
			if (possiblePeers.contains(peer) == true) {
				correct += 1;
			}
		}
		accuracy = correct / totalTx;
		return accuracy;
	}

	/* called in constructor to build truth map */
	private void buildMapFromTruth(String filename) throws IOException {
		File f = new File(filename);
		BufferedReader reader = new BufferedReader(new FileReader(f));

		String regex = "\\[((?:\\d+\\.*){4})+\\]:\\d*,(\\d*),(\\d*)";
		Pattern p = Pattern.compile(regex);

		/* pattern match each line to build tx to peer map */
		System.out.println("Building maps from file...");
		String line = reader.readLine();
		while (line != null) {
			Matcher m = p.matcher(line);
			m.matches();
			String addr = m.group(1);
			String txID = m.group(2);
			String timeStamp = m.group(3);

			/*
			 * System.out.println("txID: " + txID); System.out.println("addr: "
			 * + addr); System.out.println("time stamp: " + timeStamp);
			 */

			/* populate maps based on message type */
			truthTxToPeer.put(txID, addr);
			line = reader.readLine();
		}
		reader.close();
		// System.out.println("Finished building maps...");
	}

	public HashMap<String, String> getTruthMap() {
		return this.truthTxToPeer;
	}

	private static void gogo(String fileBase) throws IOException {
		PeerLink pl = new PeerLink("../miscScripts/" + fileBase + "-out.log");
		PeerLinkTester test = new PeerLinkTester(1, pl, 10, "../miscScripts/" + fileBase + "-groundTruth.log",10);
		test.testForBestDepth(fileBase.split("-")[0] + ".txt");
		test.testBestDepthWithVoting(fileBase.split("-")[0] + "Voting" + ".txt");
	}

	public static void main(String[] args) throws IOException {
	//	gogo("peer-finder-synth");
	//	gogo("allKnown-peer-finder-synth");
	//	gogo("perfect-peer-finder-synth");
	//	gogo("zeroRandom-peer-finder-synth");
		gogo("synth1000-8-8-300-300-20-100-peer-finder-synth");
	}
}
