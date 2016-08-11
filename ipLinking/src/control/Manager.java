package control;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.io.*;

import org.bitcoinj.core.AddressMessage;
import org.bitcoinj.core.AddressUser;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.InventoryItem;
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
import logging.*;
import planetlab.MoveFile;

public class Manager implements Runnable, AddressUser {

	@SuppressWarnings("unused")
	private Context bcjContext;
	private NetworkParameters params;
	private boolean isVantangePoint;
	private String plManHost;
	private String myHostName;

	private NioClientManager[] nioManagers;

	private HashMap<SanatizedRecord, PeerRecord> records;
	private HashMap<SanatizedRecord, Peer> peerObjs;

	private HashSet<String> interestingIPSet;

	private ConnectionTester connTester;
	private Thread connTesterThread;
	private AddressHarvest addrHarvester;
	private Thread addrHarvestThread;

	private int myLogLevel;
	private ThreadedWriter runLog;
	private ThreadedWriter exceptionLog;

	public static Random insecureRandom = new Random();

	public static final File LOG_DIR = new File("logs/");
	private static final String RECOVER_DIR = "recovery/";
	private static final File EX_DIR = new File("errors/");
	private static final String INTERESTED_IP_FILE_PATH = "intIP.txt";

	private static final DateFormat LONG_DF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	private static final long STATUSREPORT_INTERVAL_SEC = 120;

	private static final int NIO_CLIENT_MGER_COUNT = 2;

	public static final int EMERGENCY_LOG_LEVEL = 1;
	public static final int CRIT_LOG_LEVEL = 2;
	public static final int DEBUG_LOG_LEVEL = 3;
	public static final int IGNORE_LOG_LEVEL = Integer.MAX_VALUE;

	private static final String PL_MAN_USER = "pendgaft";
	private static final String PL_MAN_ID = "~/.ssh/id_rsa";

	/*
	 * Int window is 1 hr 50 minutes (tighten?)
	 */
	private static final long INT_WINDOW_SEC = 60 * 60 * 4;
	private static final int UNSOL_SIZE = 10;

	private static final boolean BULKY_STATUS = false;
	private static final boolean HUMAN_READABLE_DATE = false;
	private static final boolean SUPPRESS_EXCESS_STATE = true;

	public Manager(Set<String> recoverySet, String plManager) throws IOException {
		this(recoverySet, false, plManager);
	}

	public Manager(Set<String> recoverySet, boolean vantagePoint, String plManager) throws IOException {
		/*
		 * Build the params and context objects which are needed by other data
		 * structures.
		 */
		BriefLogFormatter.init();
		this.params = MainNetParams.get();
		this.bcjContext = new Context(params);
		Peer.lobotomizeMe();
		this.isVantangePoint = vantagePoint;
		this.plManHost = plManager;
		this.myHostName = InetAddress.getLocalHost().getHostName();

		/*
		 * Internal data structures
		 */
		this.records = new HashMap<SanatizedRecord, PeerRecord>();
		this.peerObjs = new HashMap<SanatizedRecord, Peer>();
		this.interestingIPSet = new HashSet<String>();

		/*
		 * Logging
		 */
		if (!Manager.LOG_DIR.exists()) {
			Manager.LOG_DIR.mkdirs();
		}
		if (!Manager.EX_DIR.exists()) {
			Manager.EX_DIR.mkdirs();
		}
		String logName = Manager.getTimestamp();
		this.runLog = new RotatingLogger(Manager.LOG_DIR, true);
		this.exceptionLog = new ThreadedWriter(new File(Manager.EX_DIR, logName + "-err"), true);
		this.myLogLevel = Manager.CRIT_LOG_LEVEL;
		Thread loggingThread = new Thread(this.runLog, "general-logging");
		Thread exceptionThread = new Thread(this.exceptionLog, "exception-logging");
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
		 * Start ze address harvester assuming we're not just a vantage point
		 */
		if (!this.isVantangePoint) {
			this.addrHarvester = new AddressHarvest(this);
			this.addrHarvestThread = new Thread(this.addrHarvester, "Address Harvest Master");
			this.addrHarvestThread.start();
		}

		/*
		 * Start ze connection tester, WE MUST BE 100% READY FOR LIVE NODES AT
		 * THIS POINT
		 */
		this.connTester = new ConnectionTester(this);
		this.connTesterThread = new Thread(this.connTester);
		this.connTesterThread.setName("Connection Tester Master");
		this.connTesterThread.start();

		/*
		 * Learn peers from the DNS Discovery system of bitcoin
		 */
		List<PeerAddress> startingList = null;
		if (recoverySet == null) {

			/*
			 * We should never ever be in a state where a vantage point is not
			 * given a list of peers...
			 */
			if (this.isVantangePoint) {
				RuntimeException tExc = new RuntimeException("no peer list given to a vantage point");
				this.logException(tExc);
				throw tExc;
			}

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
					startingList.add(new PeerAddress(InetAddress.getByName(tokens[0]), Integer.parseInt(tokens[1])));
				} catch (Exception e) {
					this.logException(e);
				}
			}
		}

		/*
		 * Actually boot off of our starting list
		 */
		for (PeerAddress tPeer : startingList) {
			this.handleAddressNotificiation(new SanatizedRecord(tPeer), null);
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

	public void logException(Throwable errMsg) {
		this.exceptionLog.writeOrDie(
				Manager.getTimestamp() + "," + errMsg.getMessage() + "," + errMsg.getStackTrace()[0].toString() + "\n");
	}

	public void resolvedStartedPeer(Peer thePeer) {
		SanatizedRecord myRecord = new SanatizedRecord(thePeer.getAddress());
		synchronized (this.peerObjs) {
			this.peerObjs.put(myRecord, thePeer);
		}

		/*
		 * If we're not a vantage point then add this node into the address
		 * harvester
		 */
		if (!this.isVantangePoint) {
			this.addrHarvester.giveNewHarvestTarget(myRecord, true);
		}
	}

	public void cleanupDeadPeer(Peer thePeer) {
		SanatizedRecord tRec = new SanatizedRecord(thePeer.getAddress());
		boolean actuallyRemoved = false;
		synchronized (this.peerObjs) {
			actuallyRemoved = this.peerObjs.remove(tRec) != null;
		}
		if (actuallyRemoved) {
			if (!this.isVantangePoint) {
				this.addrHarvester.poisonPeer(tRec);
			}
			this.connTester.giveReconnectTarget(tRec);
		}
	}

	@Override
	public void getInventory(List<InventoryItem> arg0, Peer arg1) {
		long now = System.currentTimeMillis();
		this.logEvent("INVRCV," + arg0.size() + ",from:" + arg1.getAddress().toString(), Manager.DEBUG_LOG_LEVEL);
		for (InventoryItem tItem : arg0) {
			this.logEvent("TX," + tItem.hash.toString() + ",from," + arg1.getAddress().toString() + "," + now,
					Manager.CRIT_LOG_LEVEL);
		}
	}

	@Override
	public void getAddresses(AddressMessage arg0, Peer arg1) {

		/*
		 * Vantage points take zero actions and simply ignore all ADDR messages
		 */
		if (this.isVantangePoint) {
			return;
		}

		this.logEvent("ADDRRCV," + arg0.getAddresses().size() + ",from:" + arg1.getAddress().toString(),
				Manager.DEBUG_LOG_LEVEL);
		List<PeerAddress> harvestedAddrs = arg0.getAddresses();
		boolean actuallyUnsolAddr = harvestedAddrs.size() < Manager.UNSOL_SIZE;
		SanatizedRecord fromPeer = new SanatizedRecord(arg1.getAddress());
		/*
		 * This math should be correct, clockSkew is myNow - theirNow (i.e. the
		 * number of seconds ahead I am)
		 */
		for (PeerAddress tAddr : harvestedAddrs) {
			SanatizedRecord incPeer = new SanatizedRecord(tAddr);
			this.handleAddressNotificiation(incPeer, fromPeer);
			if (actuallyUnsolAddr) {
				this.logEvent(
						"ANNOUNCED," + incPeer.toString() + ",from," + fromPeer.toString() + "," + incPeer.getTS(),
						Manager.CRIT_LOG_LEVEL);
				this.connTester.givePriorityConnectTarget(incPeer);
			} else {
				this.solTest(incPeer, fromPeer, harvestedAddrs.size());
			}
		}
	}

	private void handleAddressNotificiation(SanatizedRecord incAddr, SanatizedRecord learnedFrom) {
		PeerRecord theRecord = null;

		synchronized (this.records) {
			if (!this.records.containsKey(incAddr)) {
				theRecord = new PeerRecord(incAddr, this);
				this.records.put(incAddr, theRecord);
			} else {
				theRecord = this.records.get(incAddr);
			}
		}

		/*
		 * If this carries a time stamp, do some updating
		 */
		boolean introduce = false;
		if (learnedFrom != null) {
			if (!Manager.SUPPRESS_EXCESS_STATE) {
				theRecord.addNodeWhoKnowsMe(learnedFrom, incAddr.getTS());
			}
			introduce = theRecord.shouldIntroduce(incAddr.getTS());
		} else {
			introduce = true;
			theRecord.setAsIntroduced();
		}

		if (introduce) {
			this.connTester.giveNewNode(incAddr);
		}
	}

	private void solTest(SanatizedRecord incRecord, SanatizedRecord remotePeer, int messageSize) {
		long myNowSec = System.currentTimeMillis() / 1000;

		/*
		 * Check if the TS is within the window (currently 4 hours)
		 */
		if (myNowSec - incRecord.getTS() < Manager.INT_WINDOW_SEC) {
			this.logEvent("CONNPOINT," + remotePeer.toString() + "," + incRecord.toString() + "," + incRecord.getTS()
					+ "," + messageSize, Manager.CRIT_LOG_LEVEL);
		}

		synchronized (this.interestingIPSet) {
			if (this.interestingIPSet.contains(incRecord.toString())) {
				this.logEvent("INTIP," + remotePeer.toString() + "," + incRecord.toString() + "," + incRecord.getTS(),
						Manager.CRIT_LOG_LEVEL);
			}
		}

	}

	public PeerRecord getRecord(SanatizedRecord tRec) {
		PeerRecord retValue = null;
		synchronized (this.records) {
			retValue = this.records.get(tRec);
		}
		return retValue;
	}

	public PeerRecord getRecord(PeerAddress addr) {
		SanatizedRecord tmpRec = new SanatizedRecord(addr);
		return this.getRecord(tmpRec);
	}

	@SuppressWarnings("unchecked")
	public HashMap<SanatizedRecord, PeerRecord> getCopyOfRecords() {
		HashMap<SanatizedRecord, PeerRecord> copyOfState = null;
		synchronized (this.records) {
			copyOfState = (HashMap<SanatizedRecord, PeerRecord>) this.records.clone();
		}
		return copyOfState;
	}

	public Peer getPeerObject(SanatizedRecord targetRecord) {
		Peer retPeer = null;
		synchronized (this.peerObjs) {
			retPeer = this.peerObjs.get(targetRecord);
		}
		return retPeer;
	}

	@SuppressWarnings("unchecked")
	public HashMap<SanatizedRecord, Peer> getCopyOfPeerMap() {
		HashMap<SanatizedRecord, Peer> retMap = null;
		synchronized (this.peerObjs) {
			retMap = (HashMap<SanatizedRecord, Peer>) this.peerObjs.clone();
		}
		return retMap;
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
		HashMap<SanatizedRecord, PeerRecord> copyOfState = this.getCopyOfRecords();
		try {
			BufferedWriter outBuffer = new BufferedWriter(new FileWriter(file));
			for (PeerRecord tRecord : copyOfState.values()) {
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
		HashMap<SanatizedRecord, PeerRecord> copyOfState = this.getCopyOfRecords();
		try {
			BufferedWriter outBuffer = new BufferedWriter(new FileWriter(file));
			for (PeerRecord tRecord : copyOfState.values()) {
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
			HashMap<SanatizedRecord, Peer> copyOfPeerMap = this.getCopyOfPeerMap();
			for (SanatizedRecord tAddr : copyOfPeerMap.keySet()) {
				Peer tPeer = copyOfPeerMap.get(tAddr);
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
		/*
		 * Vantage points are not allowed to update their recovery file
		 */
		if (this.isVantangePoint) {
			return;
		}

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
		File currentRecFile = new File(baseDir, file + "-recovery-" + this.myHostName);
		try {
			BufferedWriter outBuff = new BufferedWriter(new FileWriter(currentRecFile));
			HashMap<SanatizedRecord, Peer> copyOfPeerMap = this.getCopyOfPeerMap();
			for (SanatizedRecord tRec : copyOfPeerMap.keySet()) {
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

		/*
		 * If we have a Planetlab Manager then phone home, giving the recovery
		 * set
		 */
		//TODO clean up this directory issue
		if (this.havePlManager()) {
			MoveFile fileMover = MoveFile.pushLocalFile(Manager.PL_MAN_USER, Manager.PL_MAN_ID, this.plManHost,
					currentRecFile.getAbsolutePath(), "/home/pendgaft/btc/recovery");
			try {
				fileMover.blockingExecute(10000);
			} catch (InterruptedException e) {
				this.logException(e);
			}
		}
	}

	private void bluePill() {
		try {
			this.exceptionLog.close();
			Thread.sleep(5000);
		} catch (Throwable e) {

		}
		System.exit(0);
	}

	private boolean havePlManager() {
		return this.plManHost != null;
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

			synchronized (this.records) {
				this.logEvent("total known nodes " + this.records.size(), Manager.CRIT_LOG_LEVEL);
			}
			this.logEvent("active connections " + sum + "(" + Arrays.toString(counts) + ")", Manager.CRIT_LOG_LEVEL);
			this.connTester.logSummary();

			if (!this.connTesterThread.isAlive()) {
				this.logException(new RuntimeException("CONN TESTER THREAD DIED!"));
				this.bluePill();
			}
			if (!this.addrHarvestThread.isAlive()) {
				this.logException(new RuntimeException("HARVEST THREAD DIED!"));
				this.bluePill();
			}

			File intFile = new File(Manager.INTERESTED_IP_FILE_PATH);
			if (intFile.exists()) {
				try {
					BufferedReader inBuff = new BufferedReader(new FileReader(intFile));
					String line = null;
					synchronized (this.interestingIPSet) {
						this.interestingIPSet.clear();
						while ((line = inBuff.readLine()) != null) {
							line = line.trim();
							if (line.length() > 0) {
								this.interestingIPSet.add(line);
							}
						}
					}
					inBuff.close();
				} catch (IOException e) {
					this.logException(e);
				}
			}

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
		argParse.addArgument("--vantagepoint").help("Turns on vantage point behavior, disabling peer searches")
				.required(false).action(Arguments.storeTrue());
		argParse.addArgument("--plMan").help("Informs the node what host is running a planet lab manager")
				.required(false).type(String.class);
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
		boolean amIVantage = ns.getBoolean("vantagepoint");
		String plMan = ns.getString("plMan");

		if (ns.getBoolean("recovery")) {
			Set<String> recoveryPeerSet = Manager.buildRecoverySet();
			self = new Manager(recoveryPeerSet, amIVantage, plMan);
		} else {
			self = new Manager(null, amIVantage, plMan);
		}

		Thread selfThread = new Thread(self, "Status Reporting Thread");
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
