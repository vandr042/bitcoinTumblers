package bitblender;

import bitcoinLink.AddressFinder;
import bitcoinLink.FinderResult;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.store.BlockStoreException;

public class GetPool {

	private AddressFinder addrFinder;
	private HashSet<String> depKeys;
	private HashSet<String> poolKeys;

	private HashMap<String, GetPool.ValidateResult> poolKeyTestResult;
	private HashMap<String, Double> incomingValue;
	private HashMap<String, Date> dateFilled;

	private BufferedWriter poolOutput;
	private BufferedWriter depOutput;
	private BufferedWriter rejectOutput;

	private boolean ran;

	private static NetworkParameters PARAMS = MainNetParams.get();
	private static final String KNOWN_DEP_KEY = "1Dv7uNrFYP8JfWo1b7oo14Xz6LjHrycYCj";

	private static final int MIN_TX_PK_ID = 3;

	private enum ValidateResult {
		VALID, TOOSMALL, MULTIPLETIMES, TOOMANYDOUBLE, TOOLARGEOUTPUT, TOOLARGEINPUT, LACKSODDTX;
	}

	public GetPool() throws IOException, InterruptedException, ExecutionException {
		this.addrFinder = new AddressFinder(GetPool.PARAMS);
		this.depKeys = new HashSet<String>();
		this.poolKeys = new HashSet<String>();
		this.poolKeyTestResult = new HashMap<String, GetPool.ValidateResult>();
		this.incomingValue = new HashMap<String, Double>();
		this.dateFilled = new HashMap<String, Date>();
		this.poolOutput = new BufferedWriter(new FileWriter("pkeys.txt"));
		this.depOutput = new BufferedWriter(new FileWriter("dkeys.txt"));
		this.rejectOutput = new BufferedWriter(new FileWriter("reject.log"));
		this.ran = false;
	}

	public void blindScan() {
		Set<String> allKeysPaid = this.addrFinder.getAllOutputKeys();

		int splitSize = 100;
		List<Set<String>> rounds = new ArrayList<Set<String>>(splitSize);
		for (int counter = 0; counter < splitSize; counter++) {
			rounds.add(new HashSet<String>());
		}
		int pos = 0;
		for (String tKey : allKeysPaid) {
			rounds.get(pos).add(tKey);
			pos = (pos + 1) % splitSize;
		}

		Set<String> foundKeys = new HashSet<String>();
		long startTime = System.currentTimeMillis();
		for (Set<String> tKeySet : rounds) {
			Set<FinderResult> relevantTx = this.addrFinder.getKeysPayingInto(tKeySet);

			for (String tKey : tKeySet) {
				if (this.validateSinglePushPoolKey(relevantTx, tKey) == GetPool.ValidateResult.VALID) {
					foundKeys.add(tKey);
				}
			}
		}
		long stopTime = System.currentTimeMillis();
		
		System.out.println("Blind scan took " + (stopTime - startTime)/60000 + " minutes.");
		System.out.println("Found " + foundKeys.size());

		try {
			BufferedWriter outFP = new BufferedWriter(new FileWriter("blindPookKeys.txt"));
			for(String tWorking : foundKeys){
				outFP.write(tWorking + "\n");
			}
			outFP.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * takes in a pool key and builds sets of pool keys and deposit keys by
	 * calling getOutputs and getInputs until no new pool keys or deposit keys
	 * are found.
	 */
	public int buildPool(String seedDepositKey) throws InterruptedException, ExecutionException, BlockStoreException {
		if (this.ran) {
			throw new IllegalStateException("Can't run buildPool twice!");
		}

		/*
		 * Prep the new deposit keys set
		 */
		Set<String> newDKeys = new HashSet<String>();
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
		double strangeMoney = 0.0;
		while (newDKeys.size() != 0) {
			long lapTime = System.currentTimeMillis();
			rounds++;

			/*
			 * Map to store the number of times we reject a possible pool key
			 */
			HashMap<GetPool.ValidateResult, Integer> rejectReason = new HashMap<GetPool.ValidateResult, Integer>();
			for (GetPool.ValidateResult tReason : GetPool.ValidateResult.values()) {
				rejectReason.put(tReason, 0);
			}
			Set<String> candidatePoolKeys = new HashSet<String>();

			/*
			 * Get all pool keys paid by this new push of deposit keys, remove
			 * all the pool keys we have tested in the past since there is no
			 * need to test them again
			 */
			Set<FinderResult> tempPKResult = this.addrFinder.getKeysPaidBy(newDKeys);
			for (FinderResult tResult : tempPKResult) {
				if (tResult.getInputs().size() == 1 && tResult.getOuputs().size() == 1) {
					candidatePoolKeys.addAll(tResult.getOuputs());
				}
			}
			candidatePoolKeys.removeAll(this.poolKeyTestResult.keySet());

			/*
			 * TODO we might want to do some more detailed sanity checking of
			 * our dep keys
			 */

			/*
			 * Get all txs that pay into the possible pool keys
			 */
			Set<FinderResult> tempDKResult = this.addrFinder.getKeysPayingInto(candidatePoolKeys);

			/*
			 * Filter out based on our current criteria
			 */
			Set<String> workingPKs = new HashSet<String>();
			for (String testedKey : candidatePoolKeys) {
				GetPool.ValidateResult myResult = this.validateSinglePushPoolKey(tempDKResult, testedKey);
				this.poolKeyTestResult.put(testedKey, myResult);
				if (myResult.equals(GetPool.ValidateResult.VALID)) {
					this.incomingValue.put(testedKey, 0.0);
					workingPKs.add(testedKey);
				} else {
					rejectReason.put(myResult, rejectReason.get(myResult) + 1);
				}
			}
			String failureSummary = this.getFailureString(rejectReason);

			/*
			 * Extra our new deposit keys, drop out any we already know about
			 */
			newDKeys.clear();
			for (FinderResult tResult : tempDKResult) {
				if (tResult.cotainsAnyAsOutput(workingPKs)) {
					/*
					 * Extract all of the deposit keys that paid in
					 */
					newDKeys.addAll(tResult.getInputs());

					/*
					 * Harvest values
					 */
					for (String tOutputKey : tResult.getOuputs()) {
						if (workingPKs.contains(tOutputKey)) {
							this.incomingValue.put(tOutputKey,
									this.incomingValue.get(tOutputKey) + tResult.getPayment(tOutputKey));
						} else {
							strangeMoney += tResult.getPayment(tOutputKey);
						}
					}
				}
			}
			newDKeys.removeAll(this.depKeys);

			/*
			 * Dump our newly found keys to the correct files
			 */
			try {
				// TODO also record date filled
				this.dumpSetToFile(workingPKs, this.poolOutput);

				// TODO maybe report a date here as well?
				this.dumpSetToFile(newDKeys, this.depOutput);
				this.rejectOutput.write("" + rounds + "," + failureSummary);
				this.poolOutput.flush();
				this.depOutput.flush();
				this.rejectOutput.flush();
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("ABORTING SINCE FILE I/O FAILED");
				break;
			}

			/*
			 * Update base sets
			 */
			this.poolKeys.addAll(workingPKs);
			this.depKeys.addAll(newDKeys);

			/*
			 * Output some stats to console
			 */
			System.out
					.println("Round " + rounds + " took " + (System.currentTimeMillis() - lapTime) / 1000 + " seconds");
			System.out.println(failureSummary);
			System.out.println("New pool keys " + workingPKs.size());
			System.out.println("New deposit keys " + newDKeys.size());
		}

		/*
		 * Clean up our I/O state
		 */
		this.addrFinder.done(); // terminates MaxBlockStore
		try {
			this.poolOutput.close();
			this.depOutput.close();
			this.rejectOutput.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("FAILED TO CLOSE CLEANLY, SOME DATA MAY BE LOST!");
		}

		/*
		 * Print hunt summary
		 */
		System.out.println("*****\nGet pool completed in: " + (System.currentTimeMillis() - startTime) / 1000
				+ " seconds\nTook " + rounds + " rounds\n*****");
		System.out.println("Total pool keys: " + this.poolKeys.size());
		System.out.println("Total deposit keys: " + this.depKeys.size());

		/*
		 * Print date summary
		 */
		List<Date> allDates = new ArrayList<Date>(this.dateFilled.size());
		for (String tPoolKey : this.dateFilled.keySet()) {
			allDates.add(this.dateFilled.get(tPoolKey));
		}
		Collections.sort(allDates);
		System.out.println("Earliest date pool key seen: " + allDates.get(0).toString());
		System.out.println("Most recent date pool key: " + allDates.get(allDates.size() - 1).toString());

		/*
		 * Print value summary
		 */
		double goodMoney = 0.0;
		double largestPK = 0.0;
		for (String tPoolKey : this.incomingValue.keySet()) {
			goodMoney += this.incomingValue.get(tPoolKey);
			if (this.incomingValue.get(tPoolKey) > largestPK) {
				largestPK = this.incomingValue.get(tPoolKey);
			}
		}
		System.out.println("Good money: " + goodMoney);
		System.out.println("Strange money: " + strangeMoney);
		System.out.println("Largest pool key: " + largestPK);

		this.ran = true;
		return rounds;
	}

	private String getFailureString(HashMap<GetPool.ValidateResult, Integer> failures) {
		StringBuilder strBuild = new StringBuilder();
		for (GetPool.ValidateResult tReason : failures.keySet()) {
			strBuild.append(tReason.toString());
			strBuild.append(":");
			strBuild.append(failures.get(tReason).toString());
			strBuild.append(",");
		}

		String outStr = strBuild.toString();
		return outStr.substring(0, outStr.length() - 1);
	}

	private GetPool.ValidateResult validateSinglePushPoolKey(Set<FinderResult> parsedTxs, String possPK) {

		/*
		 * Walk the list of finder results, filter down to those which have the
		 * possible key as an output
		 */
		Set<FinderResult> matchingTxs = new HashSet<FinderResult>();
		for (FinderResult tResult : parsedTxs) {
			if (tResult.containsOutput(possPK)) {
				matchingTxs.add(tResult);
			}
		}

		/*
		 * Check if we have enough txs to confirm
		 */
		if (matchingTxs.size() < GetPool.MIN_TX_PK_ID) {
			return GetPool.ValidateResult.TOOSMALL;
		}

		/*
		 * Next filter point, we expect the pool keys to ONLY have one push into
		 * them (so all txs that feed the pool key should have the same time
		 * stamp since they should be in the same block)
		 */
		Date myDate = null;
		for (FinderResult tResult : matchingTxs) {
			if (myDate == null) {
				myDate = tResult.getTimestamp();
			} else {
				if (tResult.getTimestamp().compareTo(myDate) != 0) {
					return GetPool.ValidateResult.MULTIPLETIMES;
				}
			}
		}

		/*
		 * Lastly we expect the transactions to pull from a single deposit key
		 * and outside of the one "odd" transaction it should just put into the
		 * pool key
		 */
		boolean seenDouble = false;
		for (FinderResult tResult : matchingTxs) {
			if (tResult.getOuputs().size() == 2) {
				if (seenDouble) {
					return GetPool.ValidateResult.TOOMANYDOUBLE;
				} else {
					seenDouble = true;
				}
			} else if (tResult.getOuputs().size() > 2) {
				return GetPool.ValidateResult.TOOLARGEOUTPUT;
			}

			if (tResult.getInputs().size() > 1) {
				return GetPool.ValidateResult.TOOLARGEINPUT;
			}
		}

		/*
		 * For now we reject if we don't see the odd double tx, might want to
		 * consider dropping this in the future
		 */
		if (!seenDouble) {
			return GetPool.ValidateResult.LACKSODDTX;
		}

		/*
		 * WE ARE VALID, going to do some fetching of info while it's easy and
		 * then return that the key is a valid pool key
		 */
		this.dateFilled.put(possPK, myDate);
		return GetPool.ValidateResult.VALID;
	}

	private void dumpSetToFile(Set<String> addrSet, BufferedWriter outFP) throws IOException {
		for (String tAddr : addrSet) {
			outFP.write(tAddr + "\n");
		}
	}

	public HashSet<String> getDepositKeys() {
		return depKeys;
	}

	public HashSet<String> getPoolKeys() {
		return poolKeys;
	}

	public static void main(String[] args)
			throws AddressFormatException, InterruptedException, ExecutionException, BlockStoreException, IOException {
		GetPool poolBuilder = new GetPool();
		//poolBuilder.buildPool(GetPool.KNOWN_DEP_KEY);
		poolBuilder.blindScan();
	}
}
