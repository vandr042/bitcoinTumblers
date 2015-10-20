package bitcoinLink;

import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.store.*;
import org.bitcoinj.utils.BriefLogFormatter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class PeerFinder implements Runnable{
	
	static NetworkParameters params = MainNetParams.get();
	static HashMap<Peer, HashMap <InetAddress, Long>> peers = new HashMap<Peer, HashMap<InetAddress, Long>>();
	static WalletAppKit kit = new WalletAppKit(params, new File("foo"), "test");
	static PeerGroup pGroup;
	static List<Peer> pl;
	File file1 = new File("connections.txt");
	static PrintStream writer;
	static Thread tthread;
	
	/* constructor */
	public PeerFinder(){
	}

	/* initializes peergroup and starts main thread that will start the helper threads */
	public void findPeers() throws FileNotFoundException, InterruptedException{
		writer = new PrintStream("connections.txt");
		kit.startAsync();
		kit.awaitRunning();
		pGroup = kit.peerGroup();
		pl = pGroup.getConnectedPeers();
		
	}
	
	/* this is wherwriter.println("Peer" + inetAddr + ": " + addr.getTime());e the helper threads get created */
	public void run() {
		
		for (Peer peer: pl){
			try {
				HashMap<InetAddress, Long> tempMap = new HashMap<InetAddress, Long>();
				peers.put(peer, tempMap);
				PeerHelper newHelper = new PeerHelper(peer, tempMap, writer);
				Thread pthread = new Thread(newHelper);
				pthread.setDaemon(true);
				pthread.start();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
		}
		try {
			Thread.sleep(180000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException, InterruptedException {
		PeerFinder finder = new PeerFinder();
		finder.findPeers();
		tthread = new Thread(finder);
		tthread.setDaemon(false);
		tthread.start();
	}

}
