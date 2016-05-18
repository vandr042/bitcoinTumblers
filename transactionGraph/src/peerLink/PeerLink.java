package peerLink;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.*;

public class PeerLink {

	/**
	 * (Peer, peers connected to) pairs
	 */
	private HashMap<String, HashSet<String>> pMap;

	/**
	 * Mapping from txID to (peer,timestamp peer says it saw tx) pairs
	 */
	private HashMap<String, LinkedList<TStampPeerPair>> txMap;

	public PeerLink(String dataFile) throws IOException {
		pMap = new HashMap<String, HashSet<String>>();
		txMap = new HashMap<String, LinkedList<TStampPeerPair>>();
		buildMapsFromFile(dataFile);
	}

	/*
	 * sim calls findSender on each transaction in txMap and returns a map from
	 * txID's to the set of possible senders
	 */
	public HashMap<String, HashSet<String>> sim(int searchDepth) throws IOException {
		System.out.println("Starting simulation with search depth: " + searchDepth);
		HashMap<String, HashSet<String>> txToPeersMap = new HashMap<String, HashSet<String>>();
		Set<String> txIDs = txMap.keySet();
		String[] txArr = new String[txIDs.size()];
		txIDs.toArray(txArr);
		for (String tx : txArr) {
			// bw.write("tx: " + tx + "\n");
			txToPeersMap.put(tx, this.findSender(tx, searchDepth));
		}
		// bw.close();
		return txToPeersMap;
	}
	
	public HashMap<String, Set<String>> simVoting(int tStampDepth){
		System.out.println("Starting simulation with tStamp depth: " + tStampDepth);
		HashMap<String, Set<String>> txToPeersMap = new HashMap<String, Set<String>>();
		Set<String> txIDs = txMap.keySet();
		String[] txArr = new String[txIDs.size()];
		txIDs.toArray(txArr);
		for (String tx : txArr) {
			//bw.write("tx: " + tx + "\n");
			txToPeersMap.put(tx, this.findSenderByVoting(tx, tStampDepth).keySet());
		}
		//bw.close();
		return txToPeersMap;
	}
	
	public HashMap<String,HashSet<String>> findSenderByVoting(String tx, int tStampDepth){
		LinkedList<TStampPeerPair> tsppList = txMap.get(tx);
		Collections.sort(tsppList);
		HashMap<String,HashSet<String>> mcpSeenByMap = computeMostCommonPeers(tsppList,tStampDepth);
		return mcpSeenByMap;
	}
	/*
	 * findSender starts at the earliest known timestamp and iterates to a
	 * specified search depth, intersecting the peerConn sets
	 */
	private HashSet<String> findSender(String tx, int searchDepth) throws IOException {
		LinkedList<TStampPeerPair> tsppList = txMap.get(tx);
		Collections.sort(tsppList);
		TStampPeerPair tspp = tsppList.get(0);
		String peer = tspp.getPeer();
		HashSet<String> peerConns = pMap.get(peer);
		HashSet<String> intersectConns = (HashSet<String>) peerConns.clone();
		
		// HashSet<String> trailIConns = (HashSet<String>)
		// intersectConns.clone();
	
		for (int i = 1; i < searchDepth; i++) {
			TStampPeerPair tmpTSPP = tsppList.get(i);
			String tmpPeer = tmpTSPP.getPeer();
			HashSet<String> tmpPeerConns = pMap.get(tmpPeer);
			intersectConns.retainAll(tmpPeerConns);
			// if (intersectConns.size() > 1) {
			// trailIConns = (HashSet<String>) intersectConns.clone();
			// } else {
			// break;
			// }
		}
		// bw.write("intersected set: " + intersectConns.toString() + "\n");
		// if (intersectConns.size() == 0) { // step back
		// return trailIConns;
		// } else {
		// return intersectConns;
		// }
		return intersectConns;
	}
	
	/*private HashSet<String> intersectPeerConns(String peer, LinkedList<TStampPeerPair> tsppList){
		HashSet<String> tmpPeerConns = pMap.get(peer);
	}*/
	
	
	
	/**
	 * computeMostCommonPeers computes the peer who the greatest number of peers said they heard about the transaction from in the list given, up to a certain timestamp.
	 * \param peers is a sorted LinkedList of TStampPeerPairs
	 * \param tStampDepth is a maximum time stamp for which to compute the most common peers
	 * returns a linked list of the peers containing unique most common peers (different peers seen same # times) in their conn set
	 */
	private HashMap<String,HashSet<String>> computeMostCommonPeers(LinkedList<TStampPeerPair>  peers, int tStampDepth){
		int tStampsDeep;
		List<PeerCountPair> pcpList;			// List private peers and the number of times they are seen			
		HashMap<String, Integer> peerSeenCount; 	// Map of peers to times seen in order to implement counting
		LinkedList<String> mostCommonPeers;			// This is a list of the most common peers
		HashMap<String, HashSet<String>> mcpSeenBy; // map from the most common peers to a set of the peers who said they saw them
		
		
		tStampsDeep = 0;
		peerSeenCount = new HashMap<String, Integer>();
		pcpList = new LinkedList<PeerCountPair>();
		
		/**************************************************************
		 * Populate peerSeenCount with peers and the number of times 
		 * they are seen in peerConn sets. 
		 **************************************************************/
		long currTstamp,lastTstamp;
		TStampPeerPair ttspp = peers.get(0);	
		String tpeer = ttspp.getPeer();
		lastTstamp = ttspp.getTimeStamp();
		currTstamp = ttspp.getTimeStamp();
		int i = 1;
		while(tStampsDeep < tStampDepth && i < peers.size()){
			lastTstamp = currTstamp;
			HashSet<String> peerConns = this.pMap.get(tpeer);
			for(String p:peerConns){
				if (peerSeenCount.get(p) == null)
					peerSeenCount.put(p, 1);
				else{
					int count = peerSeenCount.get(p);
					peerSeenCount.put(p, count + 1);
				}
			}
			ttspp = peers.get(i);
			tpeer = ttspp.getPeer();
			currTstamp = ttspp.getTimeStamp();
			if (currTstamp > lastTstamp)
				tStampsDeep++;
			i++;
		}//end while
		/*************************************************************/
		
		
		/**************************************************************
		 ************  Extract the most common peers ******************
		 **************************************************************/
		Set<String> possiblePeers = peerSeenCount.keySet();
		for (String p:possiblePeers){
			PeerCountPair peerCountPair;
			
			peerCountPair = new PeerCountPair(p,peerSeenCount.get(p));
			pcpList.add(peerCountPair);
		}
		Collections.sort(pcpList);
		int highCount, currCount, j;
		j= 1;
		highCount = currCount = pcpList.get(0).getCount();
		while (currCount == highCount && j < pcpList.size()){
			currCount = pcpList.get(j).getCount();
			j++;
		}
		pcpList = pcpList.subList(0, j);
		mostCommonPeers = new LinkedList<String>();
		for (PeerCountPair pcp:pcpList){
			String peer = pcp.getPeer();
			mostCommonPeers.add(peer);
		}
		/*************************************************************/
		
		
		
		/***********************************************************
		 * For each most common peer, find the public peers who are 
		 * connected to the common peer 
		 ************************************************************/
		mcpSeenBy = new HashMap<String,HashSet<String>>();
		for (String mcp: mostCommonPeers){
			HashSet<String> seenBy = new HashSet<String>();	//set of peers a given peer was seen by
			for (int k = 0; k < peers.size(); k++){
				TStampPeerPair  tspp = peers.get(k);
				String peer = tspp.getPeer();
				HashSet<String> peerConns = pMap.get(peer);
				if (peerConns.contains(mcp)){
					seenBy.add(peer);
				}
			}
			mcpSeenBy.put(mcp, seenBy);
		}
		/************************************************************/
		
		return mcpSeenBy;
	}
	/*********************************************************************/
	

	
	/********************************************************************
	 * buildMapsFromFile reads in from input file to build pMap and txMap
	 * \param filename - name of the file to read
	 ********************************************************************/
	private void buildMapsFromFile(String filename) throws IOException {
		File f = new File(filename);
		BufferedReader reader = new BufferedReader(new FileReader(f));

		String regex = "(\\w*),(?:(\\d*),)*\\[((?:\\d+\\.*){4})+\\]:\\d*(?:,\\[((?:\\d+\\.*){4})+\\]:\\d*)*,(\\d*)";
		Pattern p = Pattern.compile(regex);

		/* pattern match each line to build peer map */
		System.out.println("Building maps from file...");
		String line = reader.readLine();
		while (line != null) {
			Matcher m = p.matcher(line);
			m.matches();
			String msgType = m.group(1); // conn, remoteconn, or tx
			String txID = m.group(2); // Can be null
			String addr = m.group(3); // This is the address that initiates a
										// connection in remote connection
										// messages
			String toAddr = m.group(4); // Peer that above addr connected to.
										// Can be null
			String timeStamp = m.group(5);
			/*
			 * System.out.println("mtype: " + m.group(1)); System.out.println(
			 * "txID: " + m.group(2)); System.out.println("addr: " + addr);
			 * System.out.println("toAddr: " + toAddr); System.out.println(
			 * "time stamp: " + timeStamp);
			 */

			/* populate maps based on message type */
			HashSet<String> peerConns;
			if (msgType.compareTo("conn") == 0) {
				peerConns = new HashSet<String>();
				peerConns.add(addr);
				pMap.put(addr, peerConns);
			} else if (msgType.compareTo("remoteconn") == 0) {
				peerConns = pMap.get(toAddr);
				peerConns.add(addr);
			} else {
				LinkedList<TStampPeerPair> tsppList = txMap.get(txID);
				if (tsppList != null) {
					TStampPeerPair tspp = new TStampPeerPair(timeStamp, addr);
					tsppList.add(tspp);
				} else {
					tsppList = new LinkedList<TStampPeerPair>();
					TStampPeerPair tspp = new TStampPeerPair(timeStamp, addr);
					tsppList.add(tspp);
					txMap.put(txID, tsppList);
				}
			}
			line = reader.readLine();
		}
		reader.close();
		System.out.println("Finished building maps...");
	}
	/************************************************************/

	public HashMap<String, HashSet<String>> getPmap() {
		return pMap;
	}

	public HashMap<String, LinkedList<TStampPeerPair>> getTxMap() {
		return txMap;
	}

	public static void main(String[] args) throws IOException {
		PeerLink pl = new PeerLink("/home/connor/workspace/bitcoinTumblers/miscScripts/peer-finder-synth-out.log");
		pl.sim(2);
		// System.out.println(pl.pMap.keySet().toString());
		// System.out.println(pl.txMap.keySet().toString());
		// System.out.println(pl.pMap.values().toString());

	}

}
