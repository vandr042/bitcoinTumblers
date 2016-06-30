package peerLink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class PLVoter {

	private HashMap<String, HashSet<String>> pMap; 
	private HashMap<String, List<TStampPeerPair>> txMap;
	private HashMap<String, List<PeerCountPair>> txLinks;
	
	public PLVoter(String dirName) throws IOException{
		pMap = new HashMap<String, HashSet<String>>();
		txMap = new HashMap<String, List<TStampPeerPair>>();
		txLinks = new HashMap<String, List<PeerCountPair>>();
		MapBuilder.buildMapFromLogs(dirName, pMap, txMap);
	}
	
	public void link(int n){
		Set<String> txSet = txMap.keySet();
		for (String tx : txSet){
			doTx(tx, n);
		}
		Set<String> keys = txLinks.keySet();
		for (String k : keys){
			System.out.println("tx: " + k + " peer: " + txLinks.get(k).get(0).getCount());
		}
	}
	
	private void doTx(String tx, int n){
		List<TStampPeerPair> sortedTSPP = sortTSPPList(txMap.get(tx));
		HashSet<String> firstPeerConns = getFirstPeerConns(sortedTSPP);
		//for (String p : firstPeerConns){
		//	System.out.println(p);
		//}
		// System.out.println(firstPeerConns.size());
		List<String> topPeers = (ArrayList<String>) getTopPeers(sortedTSPP, n);
		List<PeerCountPair> pcpList= (ArrayList<PeerCountPair>) voteAndCheck(topPeers, firstPeerConns);
		txLinks.put(tx, pcpList);
	}
	
	/** returns a sorted list of the peers with > 1 votes contained in the first peer conn set */
	private List<PeerCountPair> voteAndCheck(List<String> topPeers, HashSet<String> firstPeerConns){
		HashMap<String, Integer> countMap = new HashMap<String, Integer>();
		List<PeerCountPair> pcpList = new ArrayList<PeerCountPair>();
		Iterator<String> li = topPeers.iterator();
		String peer;
		while (li.hasNext()){
			peer = li.next();
			HashSet<String> peerConns = pMap.get(peer);
			for (String p : peerConns){
				if (!countMap.containsKey(p)){
					countMap.put(p, 1);
				}else{
					int count = countMap.get(p);
					count++;
					countMap.put(p, count);
				}
			}
		}
		Set<String> keys = countMap.keySet();
		int count;
		
		for (String k : keys){
			count = countMap.get(k);
			/* if peer seen > 1 times and is in the first peers 
			 * connection set add it to the pcp list 
			 */
			if (firstPeerConns.contains(k)){
				PeerCountPair pcp = new PeerCountPair(k,count);
				pcpList.add(pcp);
			}
		}
		Collections.sort(pcpList);
		return pcpList;
		
	}

	/** takes in a sorted tspp list and returns a list of the top n unique peers */
	private List<String> getTopPeers(List<TStampPeerPair> tsppList, int n){
		int i;
		List<String> topPeers;
		
		i = 0;
		topPeers = new ArrayList<String>(10);
		
		Iterator<TStampPeerPair> iter = tsppList.iterator();
		String currPeer = iter.next().getPeer();
		String lastPeer = currPeer;
		topPeers.add(currPeer);
		i++;
		
		/* loop over tsppList and get top 10 unique peers */
		while (i < n){
			if (!currPeer.equals(lastPeer)){
				topPeers.add(currPeer);
				lastPeer = currPeer;
				i++;
			}
			
			currPeer = iter.next().getPeer();
		}
		return topPeers;
	}
	
	private HashSet<String> getFirstPeerConns(List<TStampPeerPair> tsppList){
		HashSet<String> fpConns = pMap.get(tsppList.get(0).getPeer());
		return fpConns;
	}
	
	private List<TStampPeerPair> sortTSPPList(List<TStampPeerPair> tsppList){
		Collections.sort(tsppList);
		return tsppList;
	}
	
	public static void main(String[] args) throws IOException {
		PLVoter plv = new PLVoter("../miscScripts/Logs");
		plv.link(10);
		PLStats pls = new PLStats("../miscScripts/zeroRandom-peer-finder-synth-groundTruth.log");
		System.out.println(pls.checkFirstPeerDirConn(plv.txLinks));
		
	}

}
