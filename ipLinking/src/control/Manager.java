package control;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.io.*;

import org.bitcoinj.core.AddressMessage;
import org.bitcoinj.core.AddressUser;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.net.NioClientManager;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.net.discovery.PeerDiscoveryException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.utils.BriefLogFormatter;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import data.PeerRecord;
import data.SanatizedRecord;
import logging.ThreadedWriter;

public class Manager implements Runnable, AddressUser {

	private NetworkParameters params;
	private Context bcjContext;

	private NioClientManager[] nioManagers;

	private ConcurrentHashMap<SanatizedRecord, PeerRecord> records;
	private ConcurrentHashMap<SanatizedRecord, Peer> peerObjs;

	private ConnectionTester connTester;
	private AddressHarvest addrHarvester;

	private int myLogLevel;
	private ThreadedWriter runLog;
	private ThreadedWriter exceptionLog;

	public static Random insecureRandom = new Random();

	private static final String RECOVER_DIR = "recovery/";

	private static final DateFormat LONG_DF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	private static final long STATUSREPORT_INTERVAL_SEC = 120;

	private static final int NIO_CLIENT_MGER_COUNT = 2;

	public static final int EMERGENCY_LOG_LEVEL = 1;
	public static final int CRIT_LOG_LEVEL = 2;
	public static final int DEBUG_LOG_LEVEL = 3;

	private static final long INT_WINDOW_SEC = 4500;

	private static final boolean BULKY_STATUS = false;
	private static final boolean HUMAN_READABLE_DATE = false;

	public Manager(Set<String> recoverySet) throws IOException {
		/*
		 * Build the params and context objects which are needed by other data
		 * structures.
		 */
        BriefLogFormatter.init();
		this.params = MainNetParams.get();
		this.bcjContext = new Context(params);
		Peer.lobotomizeMe();

		/*
		 * Internal data structures
		 */
		this.records = new ConcurrentHashMap<SanatizedRecord, PeerRecord>();
		this.peerObjs = new ConcurrentHashMap<SanatizedRecord, Peer>();

		/*
		 * Logging
		 */
		String logName = Manager.getTimestamp();
		this.runLog = new ThreadedWriter(logName, true);
		this.exceptionLog = new ThreadedWriter(logName + "-err", true);
		this.myLogLevel = Manager.CRIT_LOG_LEVEL;
		Thread loggingThread = new Thread(this.runLog);
		Thread exceptionThread = new Thread(this.exceptionLog);
		loggingThread.setName("Logging thread.");
		exceptionThread.setName("Exception logging thread");
		loggingThread.setDaemon(true);
		exceptionThread.setDaemon(true);
		loggingThread.start();
		exceptionThread.start();

		/*
		 * Spin up NIO client
		 */
		this.logEvent("NIO start", Manager.EMERGENCY_LOG_LEVEL);
		this.nioManagers = new NioClientManager[Manager.NIO_CLIENT_MGER_COUNT];
		for (int counter = 0; counter < this.nioManagers.length; counter++) {
			this.nioManagers[counter] = new NioClientManager(Thread.NORM_PRIORITY);
		}
		for (NioClientManager tManager : this.nioManagers) {
			tManager.startAsync();
		}
		for (NioClientManager tManager : this.nioManagers) {
			tManager.awaitRunning();
		}
		this.logEvent("NIO start done", Manager.EMERGENCY_LOG_LEVEL);

		/*
		 * Start ze address harvester
		 */
		this.addrHarvester = new AddressHarvest(this);
		Thread harvestThread = new Thread(this.addrHarvester);
		harvestThread.setDaemon(true);
		harvestThread.start();

		/*
		 * Start ze connection tester, WE MUST BE 100% READY FOR LIVE NODES AT
		 * THIS POINT
		 */
		this.connTester = new ConnectionTester(this);
		Thread connTestThread = new Thread(this.connTester);
		connTestThread.setName("Connection tester master");
		connTestThread.start();

		/*
		 * Learn peers from the DNS Discovery system of bitcoin
		 */
		List<PeerAddress> startingList = null;
		if (recoverySet == null) {
			this.logEvent("DNS bootstrap start", Manager.EMERGENCY_LOG_LEVEL);
			PeerAddress[] dnsPeers = this.buildDNSBootstrap();
			this.logEvent("DNS boostrap done", Manager.EMERGENCY_LOG_LEVEL);
			if (dnsPeers == null) {
				throw new RuntimeException("Failure during DNS peer fetch.");
			}
			startingList = Arrays.asList(dnsPeers);
		} else {
			startingList = new LinkedList<PeerAddress>();
			for (String tStr : recoverySet) {
				String[] tokens = tStr.split(":");
				try {
					startingList.add(new PeerAddress(InetAddress.getByName(tokens[0].split("/")[1]),
							Integer.parseInt(tokens[1])));
				} catch (Exception e) {
					this.logException(e);
				}
			}
		}

		/*
		 * Actually boot off of our starting list
		 */
		for (PeerAddress tPeer : startingList) {
			this.possiblyLearnPeer(new SanatizedRecord(tPeer), null);
		}
	}

	private PeerAddress[] buildDNSBootstrap() {
		/*
		 * Build a DNS discovery engine and harvest a set of socket addresses
		 */
		DnsDiscovery dnsDisc = new DnsDiscovery(params);
		InetSocketAddress[] dnsSeeds;
		try {
			dnsSeeds = dnsDisc.getPeers(0, 30, TimeUnit.SECONDS);
		} catch (PeerDiscoveryException e) {
			e.printStackTrace();
			return null;
		}

		PeerAddress[] initPeerAddresses = new PeerAddress[dnsSeeds.length];
		for (int pos = 0; pos < dnsSeeds.length; pos++) {
			initPeerAddresses[pos] = new PeerAddress(this.params, dnsSeeds[pos]);
		}

		this.runLog.writeOrDie("DNS discovery found " + initPeerAddresses.length + " hosts");
		return initPeerAddresses;
	}

	public void logEvent(String eventMsg, int logLevel) {
		if (logLevel <= this.myLogLevel) {
			this.runLog.writeOrDie(Manager.getTimestamp() + "," + eventMsg + "\n");
		}
	}

	public void logException(Exception errMsg) {
		this.exceptionLog.writeOrDie(
				Manager.getTimestamp() + "," + errMsg.getMessage() + "," + errMsg.getStackTrace()[0].toString() + "\n");
	}

	public boolean possiblyLearnPeer(SanatizedRecord learnedPeer, SanatizedRecord learnedFrom) {
		boolean returnFlag = false;

		/*
		 * XXX super tiny race condition could cause node to not get credited
		 * with going into unsolicitied queue, but still ends up in harvested to
		 * queue so not that big of a deal IMO
		 */
		synchronized (this.records) {
			if (!this.records.containsKey(learnedPeer)) {
				PeerRecord newRecord = new PeerRecord(learnedPeer, this);
				this.records.put(learnedPeer, newRecord);
				returnFlag = true;
				this.connTester.giveNewNode(learnedPeer);
			}
		}
		if (learnedFrom != null) {
			this.records.get(learnedPeer).addNodeWhoKnowsMe(learnedFrom, learnedPeer.getTS());
		}

		return returnFlag;
	}

	public void resolvedStartedPeer(Peer thePeer) {
		this.addrHarvester.giveNewHarvestTarget(thePeer, true);
		this.peerObjs.put(new SanatizedRecord(thePeer.getAddress()), thePeer);
	}

	public void cleanupDeadPeer(Peer thePeer) {
		SanatizedRecord tRec = new SanatizedRecord(thePeer.getAddress());
		if (this.peerObjs.remove(tRec) != null) {
			this.addrHarvester.poisonPeer(thePeer.getAddress());
			this.connTester.giveReconnectTarget(thePeer.getAddress());
		}
	}

	public void getBurstResults(SanatizedRecord fromPeer, HashSet<SanatizedRecord> responses) {
		for (SanatizedRecord tLearned : responses) {
			this.handleAddressNotificiation(tLearned, fromPeer);
			if ((System.currentTimeMillis() / 1000) - tLearned.getTS() < Manager.INT_WINDOW_SEC) {
				this.logEvent("Poss Connect Point," + fromPeer.toString() + "," + tLearned.toString() + ","
						+ tLearned.getTS(), Manager.CRIT_LOG_LEVEL);
			}
		}
	}

	@Override
	public void getAddresses(AddressMessage arg0, Peer arg1) {
		this.logEvent("Unsolicted  push of " + arg0.getAddresses().size() + " from " + arg1.getAddress(),
				Manager.DEBUG_LOG_LEVEL);
		List<PeerAddress> harvestedAddrs = arg0.getAddresses();
		boolean actuallyUnsolAddr = harvestedAddrs.size() < 3;
		SanatizedRecord fromPeer = new SanatizedRecord(arg1.getAddress());
		/*
		 * This math should be correct, clockSkew is myNow - theirNow (i.e. the
		 * number of seconds ahead I am)
		 */
		for (PeerAddress tAddr : harvestedAddrs) {
			SanatizedRecord incPeer = new SanatizedRecord(tAddr);
			if (actuallyUnsolAddr) {
				this.logEvent(
						"ANNOUNCED," + incPeer.toString() + ",from," + fromPeer.toString() + "," + incPeer.getTS(),
						Manager.CRIT_LOG_LEVEL);
				this.connTester.givePriorityConnectTarget(incPeer);
			}
			this.handleAddressNotificiation(incPeer, fromPeer);
		}
	}

	private void handleAddressNotificiation(SanatizedRecord incAddr, SanatizedRecord learnedFrom) {
		if (!this.records.containsKey(incAddr)) {
			this.possiblyLearnPeer(incAddr, learnedFrom);
		} else {
			this.records.get(incAddr).addNodeWhoKnowsMe(learnedFrom, incAddr.getTS());
		}
	}

	public PeerRecord getRecord(PeerAddress addr) {
		SanatizedRecord tmpRec = new SanatizedRecord(addr);
		return this.records.get(tmpRec);
	}

	public NioClientManager getRandomNIOClient() {
		int slot = Manager.insecureRandom.nextInt(Manager.NIO_CLIENT_MGER_COUNT);
		return this.nioManagers[slot];
	}

	private int[] getConnectionCoutns() {
		int[] counts = new int[this.nioManagers.length];
		for (int counter = 0; counter < this.nioManagers.length; counter++) {
			counts[counter] = this.nioManagers[counter].getConnectedClientCount();
		}
		return counts;
	}

	/*
	 * public NioClientManager getNIOClient() { return this.firstNIO; }
	 */

	public NetworkParameters getParams() {
		return this.params;
	}

	public void dumpRespondingNodes(String file) {
		try {
			BufferedWriter outBuffer = new BufferedWriter(new FileWriter(file));
			for (PeerRecord tRecord : this.records.values()) {
				long tempUp = tRecord.getLastUptime();
				if (tempUp != 0) {
					outBuffer.write(tRecord.getMyAddr().toString() + "," + tempUp + ","
							+ Boolean.toString(tRecord.getTimeConnected() != -1) + "\n");
				}
			}
			outBuffer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void dumpKnowledge(String file) {
		try {
			BufferedWriter outBuffer = new BufferedWriter(new FileWriter(file));
			for (PeerRecord tRecord : this.records.values()) {
				HashMap<SanatizedRecord, Long> freshMap = tRecord.getCopyOfNodesWhoKnow();
				outBuffer.write("***," + tRecord.getMyAddr().toString() + "\n");
				for (SanatizedRecord tKnowingPeer : freshMap.keySet()) {
					outBuffer.write(tKnowingPeer + "," + freshMap.get(tKnowingPeer) + "\n");
				}
			}
			outBuffer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void dumpTimeSkew(String file) {
		try {
			BufferedWriter outBuffer = new BufferedWriter(new FileWriter(file));
			for (SanatizedRecord tAddr : this.peerObjs.keySet()) {
				Peer tPeer = this.peerObjs.get(tAddr);
				if (tPeer != null) {
					outBuffer.write(tAddr + "," + tPeer.getClockSkewGuess() + "\n");
				}
			}
			outBuffer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void makeRecoveryFile(String file) {
		File baseDir = new File(Manager.RECOVER_DIR);

		/*
		 * Sanity check that the enviornment has the recovery directory
		 */
		if (!baseDir.exists()) {
			baseDir.mkdirs();
		}

		/*
		 * Actually dump our current connection state
		 */
		File currentRecFile = new File(baseDir, file + "-recovery");
		try {
			BufferedWriter outBuff = new BufferedWriter(new FileWriter(currentRecFile));
			for (SanatizedRecord tRec : this.peerObjs.keySet()) {
				outBuff.write(tRec.toString() + "\n");
			}
			outBuff.close();
		} catch (IOException e) {
			this.logException(e);
		}

		/*
		 * Delete all others
		 */
		for (File tFile : baseDir.listFiles()) {
			if (!tFile.equals(currentRecFile)) {
				tFile.delete();
			}
		}
	}

	@Override
	public void run() {

		while (true) {
			try {
				Thread.sleep(Manager.STATUSREPORT_INTERVAL_SEC * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			int[] counts = this.getConnectionCoutns();
			int sum = 0;
			for (int tCount : counts) {
				sum += tCount;
			}

			this.logEvent("total known nodes " + this.records.size(), Manager.CRIT_LOG_LEVEL);
			this.logEvent("active connections " + sum + "(" + Arrays.toString(counts) + ")", Manager.CRIT_LOG_LEVEL);
			/*
			 * Force the cleaning up of useless arrays every little bit
			 */
			System.gc();
		}

	}

	public static String getTimestamp() {
		if (Manager.HUMAN_READABLE_DATE) {
			Date curDate = new Date();
			return Manager.LONG_DF.format(curDate);
		} else {
			return Long.toString(System.currentTimeMillis());
		}
	}

	public static Set<String> buildRecoverySet() throws IOException {
		File baseDir = new File(Manager.RECOVER_DIR);
		Set<String> recoverySet = new HashSet<String>();
		for (File tFile : baseDir.listFiles()) {
			if (!tFile.getName().contains("-recovery")) {
				continue;
			}
			BufferedReader inBuffer = new BufferedReader(new FileReader(tFile));
			while (inBuffer.ready()) {
				String pollStr = inBuffer.readLine().trim();
				if (pollStr.contains(":")) {
					recoverySet.add(pollStr);
				}
			}
			inBuffer.close();
		}
		return recoverySet;
	}

	public static void main(String[] args) throws Exception {
		ArgumentParser argParse = ArgumentParsers.newArgumentParser("Manager");
		argParse.addArgument("--recovery").help("Triggers recover mode start").required(false)
				.action(Arguments.storeTrue());
		/*
		 * Actually parse
		 */
		Namespace ns = null;
		try {
			ns = argParse.parseArgs(args);
		} catch (ArgumentParserException e1) {
			argParse.handleError(e1);
			System.exit(-1);
		}

		Manager self = null;
		if (ns.getBoolean("recovery")) {
			Set<String> recoveryPeerSet = Manager.buildRecoverySet();
			self = new Manager(recoveryPeerSet);
		} else {
			self = new Manager(null);
		}

		Thread selfThread = new Thread(self);
		selfThread.start();

		long nextLargeLog = System.currentTimeMillis() + 1800 * 1000;
		while (true) {
			Thread.sleep(300 * 1000);
			String tempTS = Long.toString((System.currentTimeMillis() / 1000));

			self.makeRecoveryFile(tempTS);

			if (Manager.BULKY_STATUS && System.currentTimeMillis() > nextLargeLog) {
				nextLargeLog = System.currentTimeMillis() + 1800 * 1000;
				self.dumpTimeSkew("skew-" + tempTS);
				self.dumpKnowledge("know-" + tempTS);
			}

		}
	}
}
