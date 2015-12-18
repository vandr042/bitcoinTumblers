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
	private LinkedList<Integer> loadedFiles;

	private HashMap<Sha256Hash, Integer> manifest;
	private boolean manifestChanged;
	private int headFile;

	private WalletAppKit kit;

	private static final String DEFAULT_BASE_DIR = "./blockStore";
	private static final String MANIFEST_FILE = "manifest.txt";

	private static final int MAX_LOADED_SIZE = 10;
	// TODO revert to minerva number
	// private static final double MEM_HEAD_ROOM = 5000000;
	private static final double MEM_HEAD_ROOM = 500000;

	private static final boolean DEBUG = true;

	public MaxBlockStore() throws IOException {
		this(MaxBlockStore.DEFAULT_BASE_DIR);
	}

	@SuppressWarnings("unchecked")
	public MaxBlockStore(String baseDirPath) throws IOException {
		this.baseDir = new File(baseDirPath);
		this.loadedBlocks = new HashMap<Sha256Hash, Block>();
		this.loadedFiles = new LinkedList<Integer>();
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
			this.manifest = new HashMap<Sha256Hash, Integer>();
			this.headFile = 0;
			this.loadedFiles.addLast(0);
		}
	}

	public Block getBlock(Sha256Hash blockHash) throws InterruptedException, ExecutionException {
		Integer fileWanted = this.manifest.get(blockHash);
		/*
		 * Check to see if we've seen the block before, if so then figure out if
		 * it's in memory safely
		 */
		if (fileWanted != null) {
			/*
			 * If the file we want to reference isn't loaded, then load it
			 */
			if (!this.loadedFiles.contains(fileWanted)) {
				this.loadDataFile(fileWanted.intValue());
			}
			/*
			 * If it is suppose to be loaded and isn't then I'm not sure what's
			 * up, but reload the stupid thing
			 */
			if (!this.loadedBlocks.containsKey(blockHash)) {
				System.err.println("CORRUPTION IN BLOCK STORAGE, REPAIRING AT COST OF FRAGMENTATION");
				return this.getBlockFromNet(blockHash);
			}
			/*
			 * It's loaded, it exists, update the most recently used file and
			 * return the stupid block
			 */
			this.updateMostRecentlyUsed(fileWanted);
			return this.loadedBlocks.get(blockHash);
		} else {
			/*
			 * Else we've never seen it before, get the thing from the net
			 */
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
		// TODO handle failure here gracefully?
		Block fetchedBlock = this.kit.peerGroup().getDownloadPeer().getBlock(blockHash).get();

		/*
		 * If the head is too large save it to disk and create a new head
		 */
		if (this.getSizeOfCurrentHead() >= MaxBlockStore.MAX_LOADED_SIZE) {
			if (MaxBlockStore.DEBUG) {
				System.out.println("****ROTOATING HEAD FROM " + this.headFile);
			}
			this.reSaveManifest();
			this.headFile++;
			this.memCheck();
		}

		/*
		 * Store the block, update the manifest, and update our cache usage
		 */
		this.loadedBlocks.put(blockHash, fetchedBlock);
		this.manifest.put(blockHash, this.headFile);
		this.manifestChanged = true;
		this.updateMostRecentlyUsed(this.headFile);

		return fetchedBlock;
	}

	@SuppressWarnings("unchecked")
	//TODO make parallel?
	private void loadDataFile(int index) {
		if (MaxBlockStore.DEBUG) {
			System.out.println("*****LOADING FILE " + index);
		}

		if (index < 0 || index > this.headFile) {
			throw new RuntimeException("Can't load index outside of manifest bounds");
		}
		if (this.loadedFiles.contains(index)) {
			throw new RuntimeException("Asked to load a file that is already loaded...");
		}

		/*
		 * Ensure we have memory to handle loaded in a new file
		 */
		this.memCheck();

		try {
			ObjectInputStream inStream = new ObjectInputStream(
					new FileInputStream(new File(this.baseDir, Integer.toString(index))));
			int count = inStream.readInt();
			for (int i = 0; i < count; i++) {
				Sha256Hash tHash = (Sha256Hash) inStream.readObject();
				Block tBlock = new Block(MainNetParams.get(), (byte[]) inStream.readObject());
				this.loadedBlocks.put(tHash, tBlock);
			}
			this.updateMostRecentlyUsed(index);
			inStream.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private void updateMostRecentlyUsed(int file) {
		if(this.loadedFiles.size() == 0){
			this.loadedFiles.addLast(this.headFile);
			return;
		}
		
		if (this.loadedFiles.getLast() == file) {
			return;
		}
		this.loadedFiles.removeFirstOccurrence(file);
		this.loadedFiles.addLast(file);
	}

	private int getSizeOfCurrentHead() {
		int result = 0;
		for (Sha256Hash tHash : this.manifest.keySet()) {
			if (this.manifest.get(tHash) == this.headFile) {
				result++;
			}
		}
		return result;
	}

	private void memCheck() {
		/*
		 * Nothing to unload if there is nothing loaded
		 */
		if(this.loadedFiles.size() == 0){
			return;
		}
		
		Runtime rt = Runtime.getRuntime();
		double totMem = rt.totalMemory();
		double guessAtMemPerFile = totMem / this.loadedFiles.size();
		double freeMem = rt.freeMemory();

		/*
		 * If the amount of mem we would have remaining is less than the
		 * headroom we need to unload something
		 */
		if (freeMem - guessAtMemPerFile < MaxBlockStore.MEM_HEAD_ROOM) {
			if (MaxBlockStore.DEBUG) {
				System.out.println("Unloading file: " + totMem + " " + guessAtMemPerFile + " " + freeMem);
			}

			/*
			 * Check if the least recently used file is the head file, is so
			 * unload the second file, otherwise unload the first file
			 */
			if (this.loadedFiles.getFirst() == this.headFile) {
				if (this.loadedFiles.size() == 1) {
					throw new RuntimeException("Why are we out of memory with only the head file?");
				}
				this.unloadFile(this.loadedFiles.get(1));
			} else {
				this.unloadFile(this.loadedFiles.getFirst());
			}
		} else {
			if (MaxBlockStore.DEBUG) {
				System.out.println("elected to not unload: " + totMem + " " + guessAtMemPerFile + " " + freeMem);
			}
		}
	}

	private void unloadFile(int index) {
		if (MaxBlockStore.DEBUG) {
			System.out.println("*****LOADING FILE " + index);
		}

		/*
		 * Yell loudly if we're trying to unload a file that isn't loaded or
		 * trying to unload the head file
		 */
		if (!this.loadedFiles.contains(index)) {
			throw new RuntimeException("That file is not currently loaded!");
		}
		if (this.headFile == index) {
			throw new RuntimeException("Not allowed to unload the head file!");
		}

		/*
		 * Iterate across the mainifest hunting for every block to unload,
		 * unload them. Could make this faster if we kept a file to hashes
		 * mapping as well
		 */
		for (Sha256Hash tHash : this.manifest.keySet()) {
			if (this.manifest.get(tHash) == index) {
				this.loadedBlocks.remove(tHash);
			}
		}
		this.loadedFiles.removeFirstOccurrence(index);
		/*
		 * Encourage GC
		 */
		System.gc();
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
			// FIXME write the blocks that map to the head
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

	public static void main(String[] args) throws Exception {
		MaxBlockStore self = new MaxBlockStore();

		Sha256Hash headHash = self.getHeadOfChain();
		Sha256Hash nextHash = headHash;
		for (int counter = 0; counter < 100; counter++) {
			Block tBlock = self.getBlock(nextHash);
			System.out.println("" + tBlock.getTransactions().size() + " tx in block " + tBlock.getHashAsString());
			nextHash = tBlock.getPrevBlockHash();
		}
		self.done();
	}

}
