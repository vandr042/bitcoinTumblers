package analysis;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import control.Manager;

public class SpyLogCleaner implements Runnable {

	private File logDirectory;
	private File parsedDirectory;

	private HashMap<String, Integer> txCountMap;
	private HashMap<String, HashMap<String, Long>> connTimeMap;
	private HashSet<String> ignoreSet;

	private static final long CHECK_INTERVAL_MS = 60 * 1000;
	private static final int MAX_TX_ITEMS = 300;

	public static final File OUT_DIR = new File("parsed/");

	public SpyLogCleaner() throws FileNotFoundException {
		this(Manager.LOG_DIR, SpyLogCleaner.OUT_DIR);
	}

	public SpyLogCleaner(File logDir, File parseDir) throws FileNotFoundException {
		this.logDirectory = logDir;
		if (!this.logDirectory.exists()) {
			throw new FileNotFoundException("Log dir does not exist");
		}

		this.parsedDirectory = parseDir;
		if (!this.parsedDirectory.exists()) {
			this.parsedDirectory.mkdirs();
		}

		this.txCountMap = new HashMap<String, Integer>();
		this.connTimeMap = new HashMap<String, HashMap<String, Long>>();
		this.ignoreSet = new HashSet<String>();
	}

	private void handleFile(File specLogFile) {
		try {
			BufferedReader rawFile = new BufferedReader(new FileReader(specLogFile));
			GZIPOutputStream outStream = new GZIPOutputStream(
					new FileOutputStream(new File(this.parsedDirectory, specLogFile.getName() + ".gz")));

			String readStr = null;
			while ((readStr = rawFile.readLine()) != null) {
				String[] tokens = readStr.split(",");
				String prunedStr = null;
				if (tokens.length < 2) {
					prunedStr = readStr;
				} else if (tokens[1].equals("TX")) {
					String txID = tokens[2];
					if (this.ignoreSet.contains(txID)) {
						prunedStr = null;
					} else {
						prunedStr = readStr;
						if (!this.txCountMap.containsKey(txID)) {
							this.txCountMap.put(txID, 0);
						}
						int newTxCount = this.txCountMap.get(txID) + 1;
						if (newTxCount >= SpyLogCleaner.MAX_TX_ITEMS) {
							this.txCountMap.remove(txID);
							this.ignoreSet.add(txID);
						} else {
							this.txCountMap.put(txID, newTxCount);
						}
					}
				} else if (tokens[1].equals("CONNPOINT")) {
					String peerWeConnnectedTo = tokens[2];
					String privatePeer = tokens[3];
					Long ts = Long.parseLong(tokens[4]);
					prunedStr = readStr;
					
					if(!this.connTimeMap.containsKey(peerWeConnnectedTo)){
						this.connTimeMap.put(peerWeConnnectedTo, new HashMap<String, Long>());
					}
					if(this.connTimeMap.get(peerWeConnnectedTo).containsKey(privatePeer)){
						if(this.connTimeMap.get(peerWeConnnectedTo).get(privatePeer).equals(ts)){
							prunedStr = null;
						}
					}
					this.connTimeMap.get(peerWeConnnectedTo).put(privatePeer, ts);
				} else {
					prunedStr = readStr;
				}

				if (prunedStr != null) {
					prunedStr = prunedStr + "\n";
					outStream.write(prunedStr.getBytes());
				}
			}

			rawFile.close();
			outStream.close();
			specLogFile.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		try {
			while (true) {
				/*
				 * Wait before looking at the log dir
				 */
				Thread.sleep(SpyLogCleaner.CHECK_INTERVAL_MS);

				String[] childFileNames = this.logDirectory.list();
				if (childFileNames.length > 1) {
					/*
					 * We have more files than just the one we're working on in
					 * the directory, let's do some cleaning!
					 */
					Arrays.sort(childFileNames);
					for (int pos = 0; pos < childFileNames.length - 1; pos++) {
						this.handleFile(new File(this.logDirectory, childFileNames[pos]));
					}
				}
			}
		} catch (InterruptedException e) {
			System.out.println("Spy Log Cleaner exiting.");
		}
	}

	public static void main(String[] args) throws Exception {
		SpyLogCleaner self = new SpyLogCleaner();
		Thread selfThread = new Thread(self);
		selfThread.start();
	}

}
