package peerLink;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class PLVoter {

	private HashMap<String, HashSet<String>> pMap; 
	private HashMap<String, Pair<Integer, List<TStampPeerPair>>> txMap;
	private HashMap<String, List<String>> txLinks; //txID to peers
	private HashMap<Integer, Pair<List<String>, List<String>>> clusterPMap;  //cluster to <List<String>, List<peer>> -- first list is peers we think pushed in tx
	private PLStats pls;
	private float fpCorr;
	private float totalTx;	
	private float tpAcc;

	public PLVoter(String filebase, String groundTruth) throws IOException, ClassNotFoundException{
		pMap = null;
		txMap = null;
		txLinks = new HashMap<String, List<String>>();
		FileInputStream f1 = new FileInputStream(filebase + "pMap.ser");
		FileInputStream f2 = new FileInputStream(filebase + "txMap.ser");
		ObjectInputStream oi1 = new ObjectInputStream(f1);
		ObjectInputStream oi2 = new ObjectInputStream(f2);
		pMap = (HashMap<String, HashSet<String>>) oi1.readObject();
		txMap = (HashMap<String, Pair<Integer, List<TStampPeerPair>>>) oi2.readObject();
		clusterPMap = new HashMap<Integer, Pair<List<String>, List<String>>>();
		pls = new PLStats(groundTruth);
		fpCorr = 0;
		totalTx = 0;
		tpAcc = 0;
	}

	public void checkLinks(){
		tpAcc = pls.checkLinks(txLinks);
	}
	/* call after linking and check links*/	
	public float getTruePeerAcc(){
		return tpAcc;
	}

	/* call after linking */
	public float getFPCorr(){
		return (fpCorr);
	}	

	/* call after linking  and check links*/
	public float getTotalTx(){
		return totalTx;
	}
	
	/* call after linking */	
	public float getAS(){
		return pls.getAvgSuspectSize();
	}
	
	public void link(int n) throws IOException {
		Set<String> txSet = txMap.keySet();
		totalTx += (float) txSet.size();

		for (String tx : txSet){
			doTx(tx, n);
		}
		resolveClusters();
		int clusterNum;
		for (String tx: txSet){
			clusterNum = txMap.get(tx).getX();		
			txLinks.put(tx, clusterPMap.get(clusterNum).getX());	
		}
	}
	
	private void doTx(String tx, int n) throws IOException {
		Pair<Integer, List<TStampPeerPair>> cluster_TSPPList = txMap.get(tx);
		List<TStampPeerPair> sortedTSPP = sortTSPPList(cluster_TSPPList.getY());
		HashSet<String> firstPeerConns = getFirstPeerConns(sortedTSPP);
		fpCorr += pls.checkFirstPeerConns(tx, firstPeerConns);
/* ******	Dump first peer conns into clusters ************* */
//		List<String> topPeers = (ArrayList<String>) getTopPeers(sortedTSPP, n);
//		List<PeerCountPair> pcpList= (ArrayList<PeerCountPair>) voteAndCheck(topPeers, firstPeerConns);
//		txLinks.put(tx, pcpList);
		int clusterNum = cluster_TSPPList.getX();		
		Pair<List<String>, List<String>> cluster = clusterPMap.get(clusterNum);
		List<String> possiblePeers; //list of peers who could have pushed in transaction

		if (cluster != null){
			possiblePeers = cluster.getY();			
			addPossiblePeers(firstPeerConns, possiblePeers);
		}else{
			cluster = new Pair<List<String>, List<String>>();
			possiblePeers = new ArrayList<String>();
			addPossiblePeers(firstPeerConns, possiblePeers);
			cluster.setY(possiblePeers);	
			clusterPMap.put(clusterNum,cluster);
		}

/* ****************************************************************************/
	}

	/** resolveClusters finds the peer responsible for each cluster and sets the pair */
	private void resolveClusters(){
		Set<Integer> clusterSet = clusterPMap.keySet();
		Pair<List<String>, List<String>> cluster;
		for (Integer clusterNum : clusterSet){
			cluster = clusterPMap.get(clusterNum);	
			voteCluster(cluster);		
		}	
	}

	
	/** adds first peer conns who may have performed a cluster of transactions to the clusters pair in the hash map */
	private void addPossiblePeers(HashSet<String> firstPeerConns, List<String> possiblePeers){
		for (String p : firstPeerConns){
			possiblePeers.add(p);
		}
	}

	private void voteCluster(Pair<List<String>, List<String>> cluster){
		HashMap<String, Integer> countMap = new HashMap<String, Integer>();
		List<String> suspectPeers = new ArrayList<String>();
		Iterator<String> li = cluster.getY().iterator();
		String peer;
		int count;
		while (li.hasNext()){
			peer = li.next();			
			if (!countMap.containsKey(peer)){
				countMap.put(peer, 1);
			}else{
				count = countMap.get(peer);
				count++;
				countMap.put(peer, count);
			}
		}
		Set<String> keys = countMap.keySet();
		int size;
		Collection<Integer> values = (Collection<Integer>) countMap.values();
		size = values.size();
		Integer[] vals = values.toArray( new Integer[size]); 		
		values.toArray(vals);
		Arrays.sort(vals);
		for (String k : keys){
			count = countMap.get(k);
			if (count == vals[size-1]){
				suspectPeers.add(k);
			}
		}
		cluster.setX(suspectPeers);		
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
		int size;
		Collection<Integer> values = (Collection<Integer>) countMap.values();
		size = values.size();
		Integer[] vals = values.toArray( new Integer[size]); 		
		values.toArray(vals);
		Arrays.sort(vals);
		for (String k : keys){
			count = countMap.get(k);
			/* if peer seen > 1 times and is in the first peers 
			 * connection set add it to the pcp list 
			 */
			if (firstPeerConns.contains(k) && count == vals[size-1]){
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

	
	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		PLVoter plv = new PLVoter(args[0], args[1]);
		plv.link(10);
		
	}

}
