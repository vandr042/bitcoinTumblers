package control;

import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.io.IOException;

import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.net.NioClientManager;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.net.discovery.PeerDiscoveryException;
import org.bitcoinj.params.MainNetParams;

import data.PeerPool;
import logging.ThreadedWriter;

public class Manager implements Runnable {

	private NetworkParameters params;
	private Context bcjContext;
	private NioClientManager nioClient;

	private PeerPool myPeers;

	private ThreadedWriter runLog;
	
	private static final DateFormat LONG_DF = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	private static final long STATUSREPORT_INTERVAL_SEC = 120;

	public Manager() throws IOException {
		/*
		 * Build the params and context objects which are needed by other data
		 * structures.
		 */
		this.params = MainNetParams.get();
		this.bcjContext = new Context(params);
		Peer.lobotomizeMe();

		/*
		 * Internal data structures and logging
		 */
		this.myPeers = new PeerPool();
		this.runLog = new ThreadedWriter(Manager.getTimestamp(), true);
		Thread loggingThread = new Thread(this.runLog);
		loggingThread.setName("Logging thread.");
		loggingThread.setDaemon(true);
		loggingThread.start();

		/*
		 * Spin up NIO client
		 */
		this.logEvent("NIO start");
		this.nioClient = new NioClientManager();
		this.nioClient.startAsync();
		this.nioClient.awaitRunning();
		this.logEvent("NIO start done");
		
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
			this.myPeers.learnedPeer(tPeer, null);
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
	
	public void logEvent(String eventMsg){
		this.runLog.writeOrDie(Manager.getTimestamp() + "," + eventMsg + "\n");
	}
	
	public PeerPool getPeerPool(){
		return this.myPeers;
	}
	
	public NioClientManager getNIOClient(){
		return this.nioClient;
	}
	
	public NetworkParameters getParams(){
		return this.params;
	}

	@Override
	public void run() {
		ConnectionTester connTester = new ConnectionTester(this);
		Thread connTestThread = new Thread(connTester);
		connTestThread.start();
		AddressHarvest addrHarvest = new AddressHarvest(this);
		Thread harvestThread = new Thread(addrHarvest);
		harvestThread.start();
		
		while(true){
			try {
				Thread.sleep(Manager.STATUSREPORT_INTERVAL_SEC * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			this.logEvent("total known nodes " + this.myPeers.getKnownCount());
			this.logEvent("active connections " + this.nioClient.getConnectedClientCount());
		}

	}
	
	public static String getTimestamp(){
		Date curDate = new Date();
		return Manager.LONG_DF.format(curDate);
	}

	public static void main(String[] args) throws IOException {
		Manager self = new Manager();
		Thread selfThread = new Thread(self);
		selfThread.start();

		// TODO simple UI to query Manager
	}

}
