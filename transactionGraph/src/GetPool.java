import java.io.*;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.store.BlockStoreException;

public class GetPool {

	private AddressFinder addrFinder;
	private HashSet<Address> depKeys;
	private HashSet<Address> poolKeys;

	private BufferedWriter poolOutput;
	private BufferedWriter depOutput;

	private boolean ran;

	private static NetworkParameters PARAMS = MainNetParams.get();
	private static final String KNOWN_DEP_KEY = "1Dv7uNrFYP8JfWo1b7oo14Xz6LjHrycYCj";

	public GetPool() throws IOException, InterruptedException, ExecutionException {
		this.addrFinder = new AddressFinder(GetPool.PARAMS);
		this.depKeys = new HashSet<Address>();
		this.poolKeys = new HashSet<Address>();
		this.poolOutput = new BufferedWriter(new FileWriter("pkeys.txt"));
		this.depOutput = new BufferedWriter(new FileWriter("dkeys.txt"));
		this.ran = false;
	}

	/*
	 * takes in a pool key and builds sets of pool keys and deposit keys by
	 * calling getOutputs and getInputs until no new pool keys or deposit keys
	 * are found.
	 */
	public int buildPool(Address seedDepositKey) throws InterruptedException, ExecutionException, BlockStoreException {
		if (this.ran) {
			throw new IllegalStateException("Can't run buildPool twice!");
		}

		Set<Address> newDKeys = new HashSet<Address>();
		Set<Address> newPKeys = null;
		newDKeys.add(seedDepositKey);
		this.depKeys.add(seedDepositKey);
		try {
			this.depOutput.write(seedDepositKey.toString());
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("ABORTING SINCE FILE I/O FAILED");
			newDKeys.clear();
		}

		int rounds = 0;
		long startTime = System.currentTimeMillis();
		System.out.println("*****\nGet pool starting!!!!\n*****");
		while (newDKeys.size() != 0) {
			long lapTime = System.currentTimeMillis();
			rounds++;

			/*
			 * Get all pool keys paid by this new push of deposit keys, remove
			 * all the pool keys we know about yielding the set of newly learned
			 * pool keys, update our fully known set of pool keys after
			 */
			newPKeys = this.addrFinder.getKeysPaidBy(newDKeys);
			newPKeys.removeAll(this.poolKeys);
			this.poolKeys.addAll(newPKeys);

			/*
			 * Same game in the opposite direction getting our new set of
			 * deposit keys
			 */
			newDKeys = this.addrFinder.getKeysPayingInto(newPKeys);
			newDKeys.removeAll(this.depKeys);
			this.depKeys.addAll(newDKeys);

			/*
			 * Dump our newly found keys to the correct files
			 */
			try {
				this.dumpSetToFile(newPKeys, this.poolOutput);
				this.dumpSetToFile(newDKeys, this.depOutput);
				this.poolOutput.flush();
				this.depOutput.flush();
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("ABORTING SINCE FILE I/O FAILED");
				break;
			}

			/*
			 * Output some stats to console
			 */
			System.out
					.println("Round " + rounds + " took " + (System.currentTimeMillis() - lapTime) / 1000 + " seconds");
			System.out.println("New pool keys " + newPKeys.size());
			System.out.println("New deposit keys " + newDKeys.size());
		}

		/*
		 * Clean up our I/O state
		 */
		this.addrFinder.done(); //terminates MaxBlockStore
		try {
			this.poolOutput.close();
			this.depOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("FAILED TO CLOSE CLEANLY, SOME DATA MAY BE LOST!");
		}

		System.out.println("*****\nGet pool completed in: " + (System.currentTimeMillis() - startTime) / 1000
				+ " seconds\nTook " + rounds + " rounds\n*****");
		System.out.println("Total pool keys: " + this.poolKeys.size());
		System.out.println("Total deposit keys: " + this.depKeys.size());
		this.ran = true;
		return rounds;
	}

	private void dumpSetToFile(Set<Address> addrSet, BufferedWriter outFP) throws IOException {
		for (Address tAddr : addrSet) {
			outFP.write(tAddr.toString() + "\n");
		}
	}

	public HashSet<Address> getDepositKeys() {
		return depKeys;
	}

	public HashSet<Address> getPoolKeys() {
		return poolKeys;
	}

	public static void main(String[] args)
			throws AddressFormatException, InterruptedException, ExecutionException, BlockStoreException, IOException {
		GetPool poolBuilder = new GetPool();
		Address addr = new Address(GetPool.PARAMS, GetPool.KNOWN_DEP_KEY);
		poolBuilder.buildPool(addr);
	}
}
