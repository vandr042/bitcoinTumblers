package bitcoinLink;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.regex.*;


public class PeerLink {
	private HashMap<String, HashSet<String>> pMap; //(Peer, peers connected to) pairs
	private HashMap<String, LinkedList<TStampPeerPair>> txMap; //Mapping from txID to (peer,timestamp peer says it saw tx) pairs 
	File f = new File("simtest.log");
	BufferedWriter bw = new BufferedWriter(new FileWriter(f));
	public PeerLink(String dataFile) throws IOException{
		pMap = new HashMap<String, HashSet<String>>();
		txMap = new HashMap<String, LinkedList<TStampPeerPair>>();
		buildMapsFromFile(dataFile);
	}
	
	/* sim calls findSender on each transaction in txMap and returns a map from txID's to the set of possible senders */
	public HashMap<String, HashSet<String>> sim(int searchDepth) throws IOException{
		System.out.println("Starting simulation with search depth: " + searchDepth);
		HashMap<String, HashSet<String>> txToPeersMap = new HashMap<String, HashSet<String>>();
		Set<String> txIDs = txMap.keySet();
		String[] txArr = new String[txIDs.size()];
		txIDs.toArray(txArr);
		for (String tx:txArr){
			//bw.write("tx: " + tx + "\n");
			txToPeersMap.put(tx, this.findSender(tx,searchDepth));
		}
		//bw.close();
		return txToPeersMap;
	}

	private HashSet<String> findSender(String tx, int searchDepth) throws IOException{
		LinkedList<TStampPeerPair> tsppList = txMap.get(tx);
		Collections.sort(tsppList);
		TStampPeerPair tspp = tsppList.get(0);
		String peer = tspp.getPeer();
		HashSet<String> peerConns = pMap.get(peer);
		HashSet<String> intersectConns = (HashSet<String>) peerConns.clone();
		HashSet<String> trailIConns = (HashSet<String>) intersectConns.clone();
		for (int i = 1; i < searchDepth; i++){
			TStampPeerPair tmpTSPP = tsppList.get(i);
			String tmpPeer = tmpTSPP.getPeer();
			HashSet<String> tmpPeerConns = pMap.get(tmpPeer);
			intersectConns.removeAll(tmpPeerConns);
			if (intersectConns.size() > 1){
				trailIConns = (HashSet<String>) intersectConns.clone();
			}else{
				break;
			}
		}
		//bw.write("intersected set: " + intersectConns.toString() + "\n");
		if (intersectConns.size() == 0){
			return trailIConns;
		}else{
			return intersectConns;
		}
	}
	
	private void buildMapsFromFile(String filename) throws IOException{
		File f = new File(filename);
		BufferedReader reader = new BufferedReader(new FileReader(f));
		
		String regex = "(\\w*),(?:(\\d*),)*\\[((?:\\d+\\.*){4})+\\]:\\d*(?:,\\[((?:\\d+\\.*){4})+\\]:\\d*)*,(\\d*)";
		Pattern p = Pattern.compile(regex);
	
		/* pattern match each line to build peer map */
		System.out.println("Building maps from file...");
		String line = reader.readLine();
		while (line != null){
			Matcher m = p.matcher(line);
			m.matches();
			String msgType = m.group(1); //conn, remoteconn, or tx
			String txID = m.group(2); //Can be null
			String addr = m.group(3); //This is the address that initiates a connection in remote connection messages
			String toAddr = m.group(4); //Peer that above addr connected to.  Can be null
			String timeStamp = m.group(5);
			/*System.out.println("mtype: " + m.group(1));
			System.out.println("txID: " + m.group(2));
			System.out.println("addr: " + addr);
			System.out.println("toAddr: " + toAddr);
			System.out.println("time stamp: " + timeStamp);*/
			
			/* populate maps based on message type */
			HashSet<String> peerConns;
			if (msgType.compareTo("conn") == 0){
				peerConns = new HashSet<String>();
				peerConns.add(addr);
				pMap.put(addr, peerConns);
			}else if(msgType.compareTo("remoteconn") == 0){
				peerConns = pMap.get(toAddr);
				peerConns.add(addr);
			}else{
				LinkedList<TStampPeerPair> tsppList = txMap.get(txID);
				if (tsppList != null){
					TStampPeerPair tspp = new TStampPeerPair(timeStamp,addr);
					tsppList.add(tspp);
				}else{
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
	
	public static void main(String[] args) throws IOException {
		PeerLink pl = new PeerLink("/home/connor/workspace/bitcoinTumblers/miscScripts/peer-finder-synth-out.log");
		pl.sim(2);
		//System.out.println(pl.pMap.keySet().toString());
		//System.out.println(pl.txMap.keySet().toString());
		//System.out.println(pl.pMap.values().toString());

	}

}
