package networkTesters;

import java.net.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.VersionMessage;
import org.bitcoinj.net.NioClientManager;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.utils.BriefLogFormatter;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.*;
import java.util.*;

public class NetworkConnector {

	private String myIPStr = null;
	private NetworkParameters params;
	private PeerAddress[] dnsSeeds;
	private NioClientManager nio;

	private int testPos;
	private List<PeerAddress> workingList;
	private List<Long> workTime;

	private Semaphore doneFlag;
	
	public static void main(String[] args) throws Exception {
		NetworkConnector self = new NetworkConnector();
		self.runConnect();
		self.waitDone();
		/*
		 * Too lazy to actually build a listener to make sure our announce goes out
		 */
		Thread.sleep(30 * 1000);
	}

	public NetworkConnector() throws Exception {
		this.testPos = 0;
		this.workingList = new ArrayList<PeerAddress>(7);
		this.workTime = new ArrayList<Long>(7);
		this.doneFlag = new Semaphore(0);

		/*
		 * Step 1, figure out what the public IP address is
		 */
		URL whoAmIurl = new URL("https://api.ipify.org");
		InputStream is = whoAmIurl.openStream();
		System.out.println("connected");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String myIPStr = "";
		String line = null;
		while ((line = br.readLine()) != null) {
			myIPStr += line;
		}
		this.myIPStr = myIPStr.trim();

		/*
		 * Basic setup stuff here...
		 */
		BriefLogFormatter.init();
		this.params = MainNetParams.get();
		new Context(params);

		/*
		 * Build a DNS discovery engine and harvest a set of socket addresses
		 */
		DnsDiscovery dnsDisc = new DnsDiscovery(params);
		InetSocketAddress[] dnsSeeds;
		dnsSeeds = dnsDisc.getPeers(30, TimeUnit.SECONDS);
		this.dnsSeeds = new PeerAddress[dnsSeeds.length];
		for (int pos = 0; pos < dnsSeeds.length; pos++) {
			this.dnsSeeds[pos] = new PeerAddress(dnsSeeds[pos]);
		}

		/*
		 * Stat up the NIO client manager
		 */
		this.nio = new NioClientManager();
		nio.startAsync();
		nio.awaitRunning();
	}
	
	public void waitDone() throws InterruptedException{
		this.doneFlag.acquire();
	}

	public void runConnect() {
		if (this.testPos >= this.dnsSeeds.length) {
			System.out.println("TOO MANY FAILURES!");
			System.exit(-1);
		}

		Peer peerObj = new Peer(params, new VersionMessage(params, 0), this.dnsSeeds[this.testPos], null, false);
		ListenableFuture<SocketAddress> connection = nio.openConnection(this.dnsSeeds[this.testPos].getSocketAddress(),
				peerObj);
		this.testPos++;
		ConnTestSlave tSlave = new ConnTestSlave(peerObj, this);
		Futures.addCallback(connection, tSlave);
	}

	public void reportFail() {
		this.runConnect();
	}

	public void reportWorking(Peer upPeer) {
		this.workingList.add(upPeer.getAddress());
		this.workTime.add(System.currentTimeMillis());
		if (this.workingList.size() == 7) {
			this.dumpToFile();
		} else {
			this.runConnect();
		}
	}

	public void dumpToFile() {
		try {
			BufferedWriter resultFile = new BufferedWriter(new FileWriter("result.txt"));
			resultFile.write("My IP: " + this.myIPStr + "\n");
			for (int counter = 0; counter < 7; counter++) {
				resultFile.write("Peer " + counter + "," + this.workingList.get(counter).toString() + ","
						+ this.workTime.get(counter) + "\n");
			}
			resultFile.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		this.doneFlag.release();
	}

}
