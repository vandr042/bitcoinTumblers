package bitcoinLink;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.regex.*;


public class PeerLink {
	HashMap<String, HashSet<String>> pMap; //(Peer, peers connected to) pairs
	HashMap<String, HashMap<String,ArrayList<String>>> txMap; //Mapping from txID to (peer,timestamp peer says it saw tx) pairs 
	public PeerLink(String dataFile) throws IOException{
		pMap = new HashMap<String, HashSet<String>>();
		txMap = new HashMap<String, HashMap<String,ArrayList<String>>>();
		buildMapsFromFile(dataFile);
		
	}
	
	//public HashMap<String, LinkedList<String>> sim(){return null;}
	
	private String findSender(String tx){
		HashMap<String,ArrayList<String>> pTStampMap = txMap.get(tx);
		String[] tstamps = (String[]) pTStampMap.keySet().toArray();
		tstamps.
		return null;
	}
	
	private String recIntersection(int index){
	}
	
	private static String[] sortTimestamps(String[] tstamps, int start, int end){
		if (start == end)
			return tstamps;
		int k;
		k = (tstamps.length/2)
	}
	
	private HashSet<String> Intersect(HashSet<String> set1, HashSet<String> set2){
		HashSet<String> clone = (HashSet<String>) set1.clone();
		set1.removeAll(set2);
		
	}
	
	private void buildMapsFromFile(String filename) throws IOException{
		File f = new File(filename);
		BufferedReader reader = new BufferedReader(new FileReader(f));
		
		String regex = "(\\w*),(?:(\\d*),)*\\[((?:\\d+\\.*){4})+\\]:\\d*(?:,\\[((?:\\d+\\.*){4})+\\]:\\d*)*,(\\d*)";
		Pattern p = Pattern.compile(regex);
	
		/* pattern match each line to build peer map */
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
				HashMap<String,ArrayList<String>> pTStampMap = txMap.get(txID);
				if (pTStampMap != null){
					ArrayList<String> peerList = pTStampMap.get(timeStamp);
					peerList.add(addr);
				}else{
					pTStampMap = new HashMap<String,ArrayList<String>>();
					ArrayList<String> peerList = new ArrayList<String>();
					peerList.add(addr);
					pTStampMap.put(timeStamp,peerList);
					txMap.put(txID, pTStampMap);
				}
			}
			line = reader.readLine();
		}
		reader.close();
	}
	
	public static void main(String[] args) throws IOException {
		PeerLink pl = new PeerLink("/home/connor/workspace/bitcoinTumblers/miscScripts/peer-finder-synth-out.log");
		//System.out.println(pl.pMap.keySet().toString());
		//System.out.println(pl.txMap.keySet().toString());
		//System.out.println(pl.pMap.values().toString());

	}

}
