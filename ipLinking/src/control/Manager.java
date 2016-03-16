package control;

import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

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

import data.PeerRecord;
import logging.ThreadedWriter;

//TODO general issue, we never actually restart the address harvester after the first harvest...
public class Manager implements Runnable, AddressUser {

	private NetworkParameters params;
	private Context bcjContext;

	private NioClientManager[] nioManagers;

	private ConcurrentHashMap<String, PeerRecord> records;
	private ConcurrentHashMap<String, Peer> peerObjs;

	private ConnectionTester connTester;
	private AddressHarvest addrHarvester;

	private ThreadedWriter runLog;
	private ThreadedWriter exceptionLog;

	public static Random insecureRandom = new Random();

	private static final DateFormat LONG_DF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	private static final long STATUSREPORT_INTERVAL_SEC = 120;

	private static final int NIO_CLIENT_MGER_COUNT = 4;

	public Manager() throws IOException {
		/*
		 * Build the params and context objects which are needed by other data
		 * structures.
		 */
		this.params = MainNetParams.get();
		this.bcjContext = new Context(params);
		Peer.lobotomizeMe();

		/*
		 * Internal data structures
		 */
		this.records = new ConcurrentHashMap<String, PeerRecord>();
		this.peerObjs = new ConcurrentHashMap<String, Peer>();

		/*
		 * Logging
		 */
		String logName = Manager.getTimestamp();
		this.runLog = new ThreadedWriter(logName, true);
		this.exceptionLog = new ThreadedWriter(logName + "-err", true);
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
		this.logEvent("NIO start");
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
		this.logEvent("NIO start done");

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
		this.logEvent("DNS bootstrap start");
		PeerAddress[] dnsPeers = this.buildDNSBootstrap();
		this.logEvent("DNS boostrap done");
		if (dnsPeers == null) {
			throw new RuntimeException("Failure during DNS peer fetch.");
		}
		for (PeerAddress tPeer : dnsPeers) {
			this.possiblyLearnPeer(tPeer, null, false, 0);
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

	public void logEvent(String eventMsg) {
		this.runLog.writeOrDie(Manager.getTimestamp() + "," + eventMsg + "\n");
	}

	public void logException(String errMsg) {
		this.exceptionLog.writeOrDie(Manager.getTimestamp() + "," + errMsg + "\n");
	}

	public boolean possiblyLearnPeer(PeerAddress learnedPeer, PeerAddress learnedFrom, boolean unsolicitied, long ts) {
		boolean returnFlag = false;

		/*
		 * XXX super tiny race condition could cause node to not get credited
		 * with going into unsolicitied queue, but still ends up in harvested to
		 * queue so not that big of a deal IMO
		 */
		String learnedAddrStr = learnedPeer.toString();
		synchronized (this.records) {
			if (!this.records.containsKey(learnedAddrStr)) {
				PeerRecord newRecord = new PeerRecord(learnedPeer, this);
				this.records.put(learnedAddrStr, newRecord);
				returnFlag = true;
				this.connTester.giveNewNode(learnedPeer, ts, unsolicitied);
			}
		}
		if (learnedFrom != null) {
			this.records.get(learnedAddrStr).addNodeWhoKnowsMe(learnedFrom, ts);
		}

		return returnFlag;
	}

	public void resolvedStartedPeer(Peer thePeer) {
		this.addrHarvester.giveNewHarvestTarget(thePeer, true);
		this.peerObjs.put(thePeer.getAddress().toString(), thePeer);
	}

	public void cleanupDeadPeer(Peer thePeer) {
		this.addrHarvester.poisonPeer(thePeer.getAddress());
		this.peerObjs.remove(thePeer.getAddress().toString());
		// TODO ensure this gets called once per peer
		// TODO give the PeerAddress back tot he conn tester for work
	}
	
	public void getBurstResults(PeerAddress fromPeer, Set<PeerAddress> harvestedAddrs){
		for(PeerAddress tLearned: harvestedAddrs){
			this.handleAddressNotificiation(tLearned, this.peerObjs.get(fromPeer.toString()));
		}
	}
	
	@Override
	public void getAddresses(AddressMessage arg0, Peer arg1) {
		this.logEvent("Unsolicted  push of " + arg0.getAddresses().size() + " from " + arg1.getAddress());
		List<PeerAddress> harvestedAddrs = arg0.getAddresses();
		/*
		 * This math should be correct, clockSkew is myNow - theirNow (i.e. the
		 * number of seconds ahead I am)
		 */
		for (PeerAddress tAddr : harvestedAddrs) {
			this.handleAddressNotificiation(tAddr, arg1);
		}
	}
	
	private void handleAddressNotificiation(PeerAddress incAddr, Peer learnedFrom){
		String tAddrStr = incAddr.toString();
		long logonGuess = learnedFrom.convertTheirTimeToLocal(incAddr.getTime());
		if (!this.records.containsKey(tAddrStr)) {
			this.possiblyLearnPeer(incAddr, learnedFrom.getAddress(), true, logonGuess);
		} else {
			this.records.get(tAddrStr).addNodeWhoKnowsMe(learnedFrom.getAddress(), logonGuess);
		}
	}

	public PeerRecord getRecord(PeerAddress addr) {
		return this.records.get(addr.toString());
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
				HashMap<String, Long> freshMap = tRecord.getCopyOfNodesWhoKnow();
				outBuffer.write("***," + tRecord.getMyAddr().toString() + "\n");
				for (String tKnowingPeer : freshMap.keySet()) {
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
			for (String tAddr : this.peerObjs.keySet()) {
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

			this.logEvent("total known nodes " + this.records.size());
			this.logEvent("active connections " + sum + "(" + Arrays.toString(counts) + ")");
			/*
			 * Force the cleaning up of useless arrays every little bit
			 */
			System.gc();
		}

	}

	public static String getTimestamp() {
		Date curDate = new Date();
		return Manager.LONG_DF.format(curDate);
	}

	public static void main(String[] args) throws IOException {
		Manager self = new Manager();
		Thread selfThread = new Thread(self);
		selfThread.start();

		// TODO set *this (main)* thread to daemon
		Scanner inScanner = new Scanner(System.in);
		while (true) {
			String cmd = inScanner.next();

			if (cmd.equalsIgnoreCase("knowledge")) {
				System.out.println("Enter file name");
				String fileName = inScanner.next();
				self.dumpKnowledge(fileName);
			} else if (cmd.equalsIgnoreCase("sucesses")) {
				System.out.println("Enter file name");
				String fileName = inScanner.next();
				self.dumpRespondingNodes(fileName);
			} else if (cmd.equalsIgnoreCase("clock")) {
				System.out.println("Enter file name");
				String fileName = inScanner.next();
				self.dumpTimeSkew(fileName);
			} else if (cmd.equalsIgnoreCase("exit")) {
				// TODO make this actually kill the program?
				break;
			} else {
				System.out.println("Bad command.");
			}
			// TODO report when dump done
		}
		inScanner.close();
	}
}
