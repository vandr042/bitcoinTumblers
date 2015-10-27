package bitcoinLink;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.VersionMessage;
import org.bitcoinj.kits.WalletAppKit;

public class TestConnThread implements Runnable {
	
	private BlockingQueue<PeerAddress> que;
	private PeerGroup pg;
	private HashMap<PeerAddress, PeerHelper> peerMap;
	private PrintStream harvestLog;
	
	public TestConnThread(BlockingQueue<PeerAddress> toTestQueue, PeerGroup peerGroup, HashMap<PeerAddress, PeerHelper> hm, PrintStream log){
		this.que = toTestQueue;
		this.pg = peerGroup;
		this.peerMap = hm;
		harvestLog = log;
	}
		
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	/* potential problem with race conditions on the hashmap i.e. PeerFinder searches map to
	 * see if an address is already in it, doesn't find it so adds it to queue - during this time
	 * Test thread was working on that peer so we get duplicate threads. Depending on how the peer group handles requests to connect
	 * to already connected peers this may not be possible.*/
	@Override
	public void run() {
		while (true){
			try {
				PeerAddress addr = que.take();
				Peer peer = pg.connectTo(addr.getSocketAddress());
				if (peer != null){
					PeerHelper newHelper = new PeerHelper(peer,harvestLog);
					synchronized(peerMap){
						peerMap.put(addr, newHelper);
					}
					Thread tThread = new Thread(newHelper);
					tThread.setName(addr.toString() + " address harvesting thread");
					tThread.setDaemon(true);
					tThread.start();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
