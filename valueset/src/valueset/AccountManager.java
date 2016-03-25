package valueset;

import java.io.*;
import java.util.*;

public class AccountManager {

	private ArrayList<Transaction> deposits = null;
	private ArrayList<Transaction> withdrawls = null;

	public AccountManager() {
		this.deposits = new ArrayList<Transaction>();
		this.withdrawls = new ArrayList<Transaction>();
		this.importData("../miscScripts/balance-synth.log");
		Collections.sort(this.deposits);
		Collections.sort(this.withdrawls);
	}

	public List<Integer> runExperiment(boolean reportKeys) {
		BufferedWriter fos = null;
		List<Integer> anonSetSizes = new ArrayList<Integer>(this.withdrawls.size());

		try {
			fos = new BufferedWriter(new FileWriter("anonimity_set.txt"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		int count = 0;
		int current = 10;
		for (Transaction currentWithdrawl : this.withdrawls) {
			try {
				fos.write(currentWithdrawl + "\n");
				Set<String> anonSet = getAnonimitySet(currentWithdrawl);
				anonSetSizes.add(anonSet.size());
				fos.write("anon set size: " + anonSet.size() + "\n");
				if (reportKeys) {
					for (String j : anonSet) {
						fos.write("\t" + j + "\n");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if(count == this.withdrawls.size() * current / 100){
				System.out.println(current + "% done");
				current += 10;
			}
			count++;
		}

		try {
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return anonSetSizes;
	}

	private void importData(String dataFileName) {
		File dataFile = new File(dataFileName);
		try {
			Scanner inputStream = new Scanner(dataFile);
			while (inputStream.hasNext()) {
				String data = inputStream.next();
				String[] values = data.split(",");
				if (values[0].toLowerCase().startsWith("dep"))
					this.deposits.add(
							new Transaction(true, values[1], Long.parseLong(values[3]), Double.parseDouble(values[2])));
				else if (values[0].toLowerCase().startsWith("with"))
					this.withdrawls.add(new Transaction(false, values[1], Long.parseLong(values[3]),
							Double.parseDouble(values[2])));
			}
			inputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private Set<String> getAnonimitySet(Transaction withdrawl) {
		Set<String> anonimitySet = new HashSet<String>();
		HashMap<String, Double> balances = new HashMap<String, Double>();

		/*
		 * Build the total deposits for all keys at the point in time when the
		 * withdrawl happens
		 */
		for (Transaction tDeposit : this.deposits) {
			/*
			 * Check to see if we're later than the withdrawl, if so we can stop
			 * accumulating deposit
			 */
			if (tDeposit.getTimeStamp() > withdrawl.getTimeStamp()) {
				break;
			}

			/*
			 * Updating the total amount the key has deposited
			 */
			if (!balances.containsKey(tDeposit.getKeyResponsible())) {
				balances.put(tDeposit.getKeyResponsible(), 0.0);
			}
			balances.put(tDeposit.getKeyResponsible(),
					balances.get(tDeposit.getKeyResponsible()) + tDeposit.getValue());
		}

		/*
		 * Find all keys that had sufficient value to ask for the withdrawal
		 */
		for (String tempDepositKey : balances.keySet()) {
			if (balances.get(tempDepositKey) >= withdrawl.getValue()) {
				anonimitySet.add(tempDepositKey);
			}
		}

		return anonimitySet;
	}

}
