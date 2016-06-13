package blockStore;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;

public class MaxBlockStore {

	private File baseDir;

	private HashMap<Sha256Hash, Block> loadedBlocks;
	private int loadedFile;

	private HashMap<Sha256Hash, Integer> manifest;
	private boolean manifestChanged;
	private int headFile;

	private WalletAppKit kit;

	private static final String DEFAULT_BASE_DIR = "./blockStore";
	private static final String MANIFEST_FILE = "manifest.txt";

	private static final int MAX_LOADED_SIZE = 1000;

	private static final boolean DEBUG = false;

	public MaxBlockStore() throws IOException {
		this(MaxBlockStore.DEFAULT_BASE_DIR);
	}

	@SuppressWarnings("unchecked")
	public MaxBlockStore(String baseDirPath) throws IOException {
		this.baseDir = new File(baseDirPath);
		this.manifestChanged = false;

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

		/*
		 * Attempt to load a manifest file
		 */
		File manifestFile = new File(this.baseDir, MaxBlockStore.MANIFEST_FILE);
		if (manifestFile.exists()) {
			ObjectInputStream oIn = new ObjectInputStream(new FileInputStream(manifestFile));
			try {
				this.headFile = oIn.readInt();
				this.manifest = (HashMap<Sha256Hash, Integer>) oIn.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			oIn.close();
			this.loadDataFile(this.headFile);
		} else {
			this.loadedBlocks = new HashMap<Sha256Hash, Block>();
			this.manifest = new HashMap<Sha256Hash, Integer>();
			this.headFile = 0;
			this.loadedFile = 0;
		}
	}

	public Block getBlock(Sha256Hash blockHash) throws InterruptedException, ExecutionException {
		Integer fileWanted = this.manifest.get(blockHash);
		if (fileWanted != null) {
			if (this.loadedFile != fileWanted.intValue()) {
				this.loadDataFile(fileWanted.intValue());
			}
			if (!this.loadedBlocks.containsKey(blockHash)) {
				System.err.println("CORRUPTION IN BLOCK STORAGE, REPAIRING AT COST OF FRAGMENTATION");
				return this.getBlockFromNet(blockHash);
			}
			return this.loadedBlocks.get(blockHash);
		} else {
			return this.getBlockFromNet(blockHash);
		}
	}

	public Sha256Hash getHeadOfChain() {
		return this.kit.chain().getChainHead().getHeader().getHash();
	}

	public void done() {
		this.reSaveManifest();
		this.kit.stopAsync();
		this.kit.awaitTerminated();
	}

	private Block getBlockFromNet(Sha256Hash blockHash) throws InterruptedException, ExecutionException {
		if (MaxBlockStore.DEBUG) {
			System.out.println("****FETCHING BLOCK FROM NET" + blockHash.toString());
		}
		Block fetchedBlock = this.kit.peerGroup().getDownloadPeer().getBlock(blockHash).get();
		/*
		 * If we're not on the head block file load in the head
		 */
		if (this.headFile != this.loadedFile) {
			this.loadDataFile(this.headFile);
		}

		/*
		 * If the head is too large save it to disk and create a new head
		 */
		if (this.loadedBlocks.size() >= MaxBlockStore.MAX_LOADED_SIZE) {
			if (MaxBlockStore.DEBUG) {
				System.out.println("****ROTOATING HEAD FROM " + this.headFile);
			}
			this.reSaveManifest();
			this.encourageMemRelease();
			this.loadedBlocks = new HashMap<Sha256Hash, Block>();
			this.headFile++;
			this.loadedFile = this.headFile;
		}

		this.loadedBlocks.put(blockHash, fetchedBlock);
		this.manifest.put(blockHash, this.loadedFile);
		this.manifestChanged = true;

		return fetchedBlock;
	}

	@SuppressWarnings("unchecked")
	private void loadDataFile(int index) {
		if (MaxBlockStore.DEBUG) {
			System.out.println("*****LOADING FILE " + index);
		}

		if (index < 0 || index > this.headFile) {
			throw new RuntimeException("Can't load index outside of manifest bounds");
		}

		/*
		 * Check if we've got the head file loaded, save it if we need before
		 * swapping
		 */
		if (this.loadedFile == this.headFile) {
			this.reSaveManifest();
		}
		this.encourageMemRelease();

		try {
			ObjectInputStream inStream = new ObjectInputStream(
					new FileInputStream(new File(this.baseDir, Integer.toString(index))));
			int count = inStream.readInt();
			for (int i = 0; i < count; i++) {
				Sha256Hash tHash = (Sha256Hash) inStream.readObject();
				Block tBlock = new Block(MainNetParams.get(), (byte[]) inStream.readObject());
				this.loadedBlocks.put(tHash, tBlock);
			}
			this.loadedFile = index;
			inStream.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private void encourageMemRelease() {
		/*
		 * If we have data loaded do everything we can to force a GC
		 */
		if (this.loadedBlocks != null) {
			this.loadedBlocks.clear();
			this.loadedBlocks = null;
			System.gc();
		}
		this.loadedBlocks = new HashMap<Sha256Hash, Block>();
	}

	private void reSaveManifest() {
		if (!this.manifestChanged) {
			return;
		}

		if (MaxBlockStore.DEBUG) {
			System.out.println("*****SAVING CHANGED HEAD FILE");
		}

		try {
			/*
			 * Write the mainfest file
			 */
			ObjectOutputStream outStream = new ObjectOutputStream(
					new FileOutputStream(new File(this.baseDir, MaxBlockStore.MANIFEST_FILE)));
			outStream.writeInt(this.headFile);
			outStream.writeObject(this.manifest);
			outStream.close();

			/*
			 * Write the current loaded map
			 */
			outStream = new ObjectOutputStream(
					new FileOutputStream(new File(this.baseDir, Integer.toString(this.headFile))));
			outStream.writeInt(this.loadedBlocks.size());
			for (Sha256Hash tHash : this.loadedBlocks.keySet()) {
				outStream.writeObject(tHash);
				outStream.writeObject(this.loadedBlocks.get(tHash).bitcoinSerialize());
			}
			outStream.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		this.manifestChanged = false;
	}

	public void shard(File destDir) throws IOException {
		for (int counter = 0; counter <= this.headFile; counter++) {
			this.loadDataFile(counter);
			for (Sha256Hash tHash : this.loadedBlocks.keySet()) {
				ObjectOutputStream outStr = new ObjectOutputStream(
						new FileOutputStream(new File(destDir, tHash.toString())));
				outStr.writeObject(this.loadedBlocks.get(tHash).bitcoinSerialize());
				outStr.close();
			}
		}
	}

	public static void main(String[] args) throws Exception {
		MaxBlockStore self = new MaxBlockStore();
		self.shard(new File("/export/scratch2/public/shardBS"));
	}

}
