package peerLink;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapBuilder {
	public static void buildMapFromLogs(String folder,HashMap<String, HashSet<String>> pMap, HashMap<String, List<TStampPeerPair>> txMap){
		File logDir = new File(folder);
		File[] contents = logDir.listFiles();
		for (File f : contents) {
			System.out.println(f.getAbsolutePath());
		}
	}
	private static void buildMap(String filename, HashMap<String, HashSet<String>> pMap, HashMap<String, List<TStampPeerPair>> txMap) throws IOException {
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
		System.out.println("Finished building maps...");
	}
	/************************************************************/
	public static void main(String[] args) {
		buildMapFromLogs("../../../miscScripts/Logs", null, null);

	}

}
