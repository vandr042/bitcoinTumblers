package bitcoinLink;

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
	
	public TestConnThread(BlockingQueue<PeerAddress> toTestQueue, PeerGroup peerGroup){
		this.que = toTestQueue;
		this.pg = peerGroup;
	}
		
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	@Override
	public void run() {
		while (true){
			try {
				PeerAddress addr = que.take();
				Peer peer = pg.connectTo(addr.getSocketAddress());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
