package bitcoinLink;

import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.store.*;
import org.bitcoinj.utils.BriefLogFormatter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class PeerFinder {

	private static NetworkParameters params = MainNetParams.get();
	
	private static HashMap<String,String> findPeers(){
		HashMap <String, String> peers = new HashMap();
		WalletAppKit kit = new WalletAppKit(params, new File("foo"), "test");
		kit.startAsync();
		kit.awaitRunning();
		PeerGroup pGroup = kit.peerGroup();
		List<Peer> pl = pGroup.getConnectedPeers();
		
		
		return peers;
	}
	
	public static void main(String[] args) {
		WalletAppKit kit = new WalletAppKit(params, new File("foo"), "test");
		kit.startAsync();
		kit.awaitRunning();
		PeerGroup pGroup = kit.peerGroup();
		List<Peer> pl = pGroup.getConnectedPeers();
		pl.get(0).getAddr().addListener(arg0, arg1);
		

	}


}
