package smallTest;

import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.store.*;
import org.bitcoinj.utils.BriefLogFormatter;

import java.io.File;
import java.util.*;

public class Test {

	public static void main(String args[]) throws Exception {

		NetworkParameters params = MainNetParams.get();

		String[] dasSeeds = params.getDnsSeeds();
		for (String tSeed : dasSeeds) {
			System.out.println("seed: " + tSeed);
		}

		WalletAppKit kit = new WalletAppKit(params, new File("foo"), "test");
		kit.startAsync();
		kit.awaitRunning();

		PeerGroup pGroup = kit.peerGroup();
		System.out.println("connected to " + pGroup.numConnectedPeers());

		Peer dlPeer = pGroup.getDownloadPeer();
		System.out.println("BC height: " + dlPeer.getBestHeight());

		BlockChain bc = kit.chain();
		StoredBlock curBlock = bc.getChainHead();
		Wallet wallet = new Wallet(params);
		for (int counter = 10; counter < 11; counter++) {
			System.out.println(curBlock.getHeader().getTime());
			System.out.println(curBlock.getHeader().getHash());
			Block tBlock = dlPeer.getBlock(curBlock.getHeader().getHash()).get();
			//System.out.println(tBlock);
			List<Transaction> tx = tBlock.getTransactions();
			System.out.println("has " + tx.size());
			for (Transaction ttx : tx) {
			
				List<TransactionInput> ttxi = ttx.getInputs();
				if (ttxi.size() > 1) {
					System.out.println("*************");
					List<TransactionOutput> ttxo = ttx.getOutputs();
					for (TransactionOutput ttxto : ttxo) {
						System.out.println("out: " + ttxto.getAddressFromP2PKHScript(params));
					}
					for (TransactionInput ttxti : ttxi) {
						System.out.println("in: " + ttxti.getFromAddress());
					}
				}
			}
			curBlock = curBlock.getPrev(kit.store());
		}

	}
}
