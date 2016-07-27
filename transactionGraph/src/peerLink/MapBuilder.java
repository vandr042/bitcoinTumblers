package peerLink;

import java.util.Collections;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
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
	public static void buildMapFromLogs(String folder, String filebase, HashMap<String, HashSet<String>> pMap, HashMap<String, Pair<Integer, List<TStampPeerPair>>> txMap) throws IOException{
		int mb = 1024*1024;
		File logDir = new File(folder);
		File[] contents = logDir.listFiles();
		Runtime runtime = Runtime.getRuntime();
		for (File f : contents) {
//			System.out.println(f.getName());
			buildMap(f, pMap, txMap);
//			System.out.println("Free memory: " + runtime.freeMemory()/mb);
//			System.out.println("Total memory: " + runtime.totalMemory()/mb);
//			System.out.println("Max memory: " + runtime.maxMemory()/mb);
		}
		serializeMaps(pMap, txMap, filebase + "pMap.ser", filebase + "txMap.ser");
	}
	
	
	private static void serializeMaps(HashMap<String, HashSet<String>> pMap, HashMap<String, Pair<Integer, List<TStampPeerPair>>> txMap, String peerFile, String txFile){
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
	
	private static void buildMap(File f, HashMap<String, HashSet<String>> pMap, HashMap<String, Pair<Integer, List<TStampPeerPair>>> txMap) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(f));

		String regex = "(\\w*),(?:(\\d*),)*\\[((?:\\d+\\.*){4})+\\]:\\d*(?:,\\[((?:\\d+\\.*){4})+\\]:\\d*)*,(\\d*),*(\\d*)";
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
			String cluster = m.group(6);
			/*
			 * System.out.println("mtype: " + m.group(1)); System.out.println(
			 * "txID: " + m.group(2)); System.out.println("addr: " + addr);
			 * System.out.println("toAddr: " + toAddr); System.out.println(
			 * "time stamp: " + timeStamp);
			 */

			/* populate maps based on message type */
			HashSet<String> peerConns;
			if (msgType.compareTo("conn") == 0) {
				if (!pMap.containsKey(addr)){
					peerConns = new HashSet<String>();
					pMap.put(addr, peerConns);
				}
			} else if (msgType.compareTo("remoteconn") == 0) {
				peerConns = pMap.get(toAddr);
				peerConns.add(addr);
			} else {
				Pair<Integer, List<TStampPeerPair>> clusterTSPPList = txMap.get(txID);
				if (clusterTSPPList != null) {
					
//						System.out.println(txID + " " + addr + " " + timeStamp);
					TStampPeerPair tspp = new TStampPeerPair(timeStamp, addr);
					clusterTSPPList.getY().add(tspp);
				
				} else {
					clusterTSPPList = new Pair<Integer, List<TStampPeerPair>>(Integer.parseInt(cluster), new ArrayList<TStampPeerPair>());
					TStampPeerPair tspp = new TStampPeerPair(timeStamp, addr);
					clusterTSPPList.getY().add(tspp);
					txMap.put(txID, clusterTSPPList);
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
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		HashMap<String, HashSet<String>> peerMap = new HashMap<String, HashSet<String>>();
		HashMap<String, Pair<Integer, List<TStampPeerPair>>> txMap = new HashMap<String, Pair<Integer, List<TStampPeerPair>>>();
		buildMapFromLogs(args[0], args[1], peerMap, txMap);
//		File f = new File("../../miscScripts/LinkLogs/test100-txLinkSynth-out0.log");
//		buildMap(f, peerMap, txMap);
//		serializeMaps(peerMap,txMap, "testpMap.ser", "testtxMap.ser");
//		HashMap<String, HashSet<String>> pMap  = null;
//		HashMap<String, List<TStampPeerPair>> tMap = null;
//		FileInputStream f1 = new FileInputStream("testpMap.ser");
//		FileInputStream f2 = new FileInputStream("testtxMap.ser");
//		ObjectInputStream oi1 = new ObjectInputStream(f1);
//		ObjectInputStream oi2 = new ObjectInputStream(f2);
//		pMap = (HashMap<String, HashSet<String>>) oi1.readObject();
//		tMap = (HashMap<String, List<TStampPeerPair>>) oi2.readObject();
//		ArrayList<TStampPeerPair> tsppList = (ArrayList<TStampPeerPair>) txMap.get("4085841858");
//		ArrayList<TStampPeerPair> tsppList2 = (ArrayList<TStampPeerPair>) tMap.get("4085841858");
//		Collections.sort(tsppList);
//		Collections.sort(tsppList2);
//		for (int i = 0; i < 15; i++){
//			System.out.println(tsppList.get(i).getPeer() + " " + tsppList2.get(i).getPeer());
//		}
	}
	
	public static List<String> getTransactionsFromTruth(String filename) throws IOException {
		File f = new File(filename);
		BufferedReader reader = new BufferedReader(new FileReader(f));
		ArrayList<String> txList = new ArrayList<String>();
		String regex = "\\[((?:\\d+\\.*){4})+\\]:\\d*,(\\d*),(\\d*)";
		Pattern p = Pattern.compile(regex);

		/* pattern match each line to build tx to peer map */
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
			txList.add(txID);
			line = reader.readLine();
		}
		reader.close();
		// System.out.println("Finished building maps...");
		return txList;	
	}

}
