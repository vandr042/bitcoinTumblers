package peerLink;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapBuilder {
	public static void buildMapFromLogs(String folder,HashMap<String, HashSet<String>> pMap, HashMap<String, List<TStampPeerPair>> txMap) throws IOException{
		int mb = 1024*1024;
		File logDir = new File(folder);
		File[] contents = logDir.listFiles();
		Runtime runtime = Runtime.getRuntime();
		for (File f : contents) {
			buildMap(f, pMap, txMap);
			System.out.println("Free memory: " + runtime.freeMemory()/mb);
			System.out.println("Total memory: " + runtime.totalMemory()/mb);
			System.out.println("Max memory: " + runtime.maxMemory()/mb);
		}
		serializeMaps(pMap, txMap, "pMap.ser", "txMap.ser");
	}
	
	
	private static void serializeMaps(HashMap<String, HashSet<String>> pMap, HashMap<String, List<TStampPeerPair>> txMap, String peerFile, String txFile){
		try{
			FileOutputStream f1 = new FileOutputStream(peerFile);
			FileOutputStream f2 = new FileOutputStream(txFile);
			ObjectOutputStream peerOut = new ObjectOutputStream(f1);
			ObjectOutputStream txOut = new ObjectOutputStream(f2);
			
			peerOut.writeObject(pMap);
			txOut.writeObject(txMap);
			
			peerOut.close();
			txOut.close();
			f1.close();
			f2.close();
			
			System.out.println("Serialized data was saved.");
		}catch(IOException i){
			i.printStackTrace();
		}
	}
	
	private static void buildMap(File f, HashMap<String, HashSet<String>> pMap, HashMap<String, List<TStampPeerPair>> txMap) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(f));

		String regex = "(\\w*),(?:(\\d*),)*\\[((?:\\d+\\.*){4})+\\]:\\d*(?:,\\[((?:\\d+\\.*){4})+\\]:\\d*)*,(\\d*)";
		Pattern p = Pattern.compile(regex);

		/* pattern match each line to build peer map */
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
			if (msgType.compareTo("conn") == 0 && !pMap.containsKey(addr)) {
				peerConns = new HashSet<String>();
				pMap.put(addr, peerConns);
			} else if (msgType.compareTo("remoteconn") == 0) {
				peerConns = pMap.get(toAddr);
				peerConns.add(addr);
			} else {
				List<TStampPeerPair> tsppList = txMap.get(txID);
				if (tsppList != null) {
					TStampPeerPair tspp = new TStampPeerPair(timeStamp, addr);
					tsppList.add(tspp);
				} else {
					tsppList = new ArrayList<TStampPeerPair>();
					TStampPeerPair tspp = new TStampPeerPair(timeStamp, addr);
					tsppList.add(tspp);
					txMap.put(txID, tsppList);
				}
			}
			line = reader.readLine();
		}
		reader.close();
	}
	
	public static void buildMapFromTruth(String filename, HashMap<String, String> truthTxToPeer) throws IOException {
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
	/**
	 * @throws IOException **********************************************************/
	public static void main(String[] args) throws IOException {
		HashMap<String, HashSet<String>> peerMap = new HashMap<String, HashSet<String>>();
		HashMap<String, List<TStampPeerPair>> txMap = new HashMap<String, List<TStampPeerPair>>();
		buildMapFromLogs("../miscScripts/Logs", peerMap, txMap);

	}

}
