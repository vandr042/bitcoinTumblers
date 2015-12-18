package blockStore;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.store.SPVBlockStore;

public class SimpleBlockStore {

	private File baseDir;
	private WalletAppKit kit;

	private static final String DEFAULT_BASE_DIR = "./blockStore";
	private static final boolean DEBUG = true;

	public SimpleBlockStore() {
		this(SimpleBlockStore.DEFAULT_BASE_DIR);
	}

	public SimpleBlockStore(String pathToBaseDir) {
		this.baseDir = new File(pathToBaseDir);

		/*
		 * Check if the base directory exists, if it doesn't create it
		 */
		if (!this.baseDir.exists()) {
			this.baseDir.mkdirs();
		}

		/*
		 * Boot up the Wallet App Kit
		 */
		this.kit = new WalletAppKit(MainNetParams.get(), this.baseDir, "bcj");
		this.kit.startAsync();
		this.kit.awaitRunning();
	}

	public List<Sha256Hash> getHashChain(int depth) {
		Sha256Hash tempHash = this.kit.chain().getChainHead().getHeader().getHash();
		Block currBlock = this.getBlock(tempHash);
		List<Sha256Hash> retList = new ArrayList<Sha256Hash>(depth);
		for (int counter = 0; counter < depth; counter++) {
			retList.add(currBlock.getHash());
			currBlock = this.getBlock(currBlock.getPrevBlockHash());
		}
		return retList;
	}

	public Block getBlock(Sha256Hash blockHash) {
		File testFile = new File(this.baseDir, blockHash.toString());

		Block fetchedBlock = null;
		if (testFile.exists()) {
			try {
				ObjectInputStream inStr = new ObjectInputStream(new FileInputStream(testFile));
				byte[] loadedBytes = (byte[]) inStr.readObject();
				inStr.close();
				fetchedBlock = new Block(MainNetParams.get(), loadedBytes);
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		} else {
			if (SimpleBlockStore.DEBUG) {
				System.out.println("****FETCHING BLOCK FROM NET" + blockHash.toString());
			}
			// TODO handle failure here gracefully?
			try {
				fetchedBlock = this.kit.peerGroup().getDownloadPeer().getBlock(blockHash).get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			try {
				ObjectOutputStream outStr = new ObjectOutputStream(new FileOutputStream(testFile));
				outStr.writeObject(fetchedBlock.bitcoinSerialize());
				outStr.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

		return fetchedBlock;
	}

	public void done() {
		this.kit.stopAsync();
		this.kit.awaitTerminated();
	}

	public static void main(String args[]) {
		System.out.println(SPVBlockStore.DEFAULT_NUM_HEADERS);
		SimpleBlockStore test = new SimpleBlockStore();
		List<Sha256Hash> testList = test.getHashChain(10000);
		System.out.println(testList.size());
		test.done();
	}
}
