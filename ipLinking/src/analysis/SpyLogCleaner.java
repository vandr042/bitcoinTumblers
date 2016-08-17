package analysis;

import java.io.*;
import java.net.InetAddress;
import java.util.*;
import java.util.zip.*;

import control.Manager;
import data.TimeRotatingHashMap;
import planetlab.MoveFile;

public class SpyLogCleaner implements Runnable {

	private File logDirectory;
	private File parsedDirectory;

	private TimeRotatingHashMap<String, Integer> txCountMap;
	private TimeRotatingHashMap<String, TimeRotatingHashMap<String, Long>> connTimeMap;

	private static final long CHECK_INTERVAL_MS = 60 * 1000;
	private static final int MAX_TX_ITEMS = 30;

	private String myHostName = "";
	private boolean plMon;

	public static final File OUT_DIR = new File("parsed/");

	public static final String MANAGER_USER = "pendgaft";
	public static final String MANAGER_END_HOST = "taranis.eecs.utk.edu";
	public static final String MANAGER_ID_FILE = "~/.ssh/id_rsa";
	public static final String REMOTE_DIR = "/home/pendgaft/btc/logs/";

	public SpyLogCleaner(boolean plMonInLoop) throws FileNotFoundException {
		this(Manager.LOG_DIR, SpyLogCleaner.OUT_DIR, plMonInLoop);
	}

	public SpyLogCleaner(File logDir, File parseDir, boolean plMonInLoop) throws FileNotFoundException {
		this.logDirectory = logDir;
		if (!this.logDirectory.exists()) {
			throw new FileNotFoundException("Log dir does not exist");
		}

		this.parsedDirectory = parseDir;
		if (!this.parsedDirectory.exists()) {
			this.parsedDirectory.mkdirs();
		}

		this.txCountMap = new TimeRotatingHashMap<String, Integer>(20 * 60 * 1000);
		this.connTimeMap = new TimeRotatingHashMap<String, TimeRotatingHashMap<String, Long>>(6 * 60 * 60 * 1000);

		try {
			this.myHostName = InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.plMon = plMonInLoop;
	}

	private void handleFile(File specLogFile) {
		try {
			BufferedReader rawFile = new BufferedReader(new FileReader(specLogFile));
			File endFile = new File(this.parsedDirectory, specLogFile.getName() + "-" + this.myHostName + ".gz");
			GZIPOutputStream outStream = new GZIPOutputStream(new FileOutputStream(endFile));

			String readStr = null;
			while ((readStr = rawFile.readLine()) != null) {
				String[] tokens = readStr.split(",");
				String prunedStr = null;
				if (tokens.length < 2) {
					prunedStr = readStr;
				} else if (tokens[1].equals("TX")) {
					String txID = tokens[2];
					int count = 0;
					if (this.txCountMap.containsKey(txID)) {
						count = this.txCountMap.get(txID);
					}
					if (count >= SpyLogCleaner.MAX_TX_ITEMS) {
						prunedStr = null;
					} else {
						prunedStr = readStr;
						count++;
						this.txCountMap.put(txID, count);
					}
				} else if (tokens[1].equals("CONNPOINT")) {
					String peerWeConnnectedTo = tokens[2];
					String privatePeer = tokens[3];
					Long ts = Long.parseLong(tokens[4]);
					prunedStr = readStr;

					if (!this.connTimeMap.containsKey(peerWeConnnectedTo)) {
						this.connTimeMap.put(peerWeConnnectedTo,
								new TimeRotatingHashMap<String, Long>(2 * 60 * 60 * 1000));
					}
					if (this.connTimeMap.get(peerWeConnnectedTo).containsKey(privatePeer)) {
						if (this.connTimeMap.get(peerWeConnnectedTo).get(privatePeer).equals(ts)) {
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

			/*
			 * Move file back to controller and clean local disk space
			 */
			if (this.plMon) {
				MoveFile fileMover = MoveFile.pushLocalFile(SpyLogCleaner.MANAGER_USER, SpyLogCleaner.MANAGER_ID_FILE,
						SpyLogCleaner.MANAGER_END_HOST, endFile.getAbsolutePath(), SpyLogCleaner.REMOTE_DIR);
				fileMover.blockingExecute(60 * 1000);
				endFile.delete();
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void run() {

		while (true) {
			try {
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
			} catch (Throwable e) {

				try {
					BufferedWriter errBuff = new BufferedWriter(
							new FileWriter("errors/" + (System.currentTimeMillis() / 1000) + "-cleanerError"));
					errBuff.write(e.getMessage());
					errBuff.close();
				} catch (IOException e2) {
					e.printStackTrace();
				}

				System.out.println("Spy Log Cleaner exiting.");
			}
		}
	}

	public static void main(String[] args) throws Exception {
		boolean plFlag = false;
		for (String arg : args) {
			if (arg.equalsIgnoreCase("--pl")) {
				plFlag = true;
				break;
			}
		}
		SpyLogCleaner self = new SpyLogCleaner(plFlag);
		Thread selfThread = new Thread(self);
		selfThread.start();
	}

}
