package bitblender;

import java.io.*;
import java.net.URL;
import java.util.*;

import org.bitcoinj.params.MainNetParams;

import bitcoinLink.FinderResult;
import bitcoinLink.AddressFinder;

public class DepositDetails {

	private Set<FinderResult> intoPoolTx;
	private Set<FinderResult> outPoolTx;
	private Set<String> poolKeys;

	DepositDetails(String poolKeyFile) throws IOException {
		this.poolKeys = new HashSet<String>();
		BufferedReader inFP = new BufferedReader(new FileReader(poolKeyFile));
		while (inFP.ready()) {
			String line = inFP.readLine().trim();
			if (line.length() > 0) {
				this.poolKeys.add(line);
			}
		}
		inFP.close();

		System.out.println("setting up adder finder");
		AddressFinder addrFinder = new AddressFinder(MainNetParams.get());
		System.out.println("finding into pool");
		this.intoPoolTx = addrFinder.getKeysPayingInto(poolKeys);
		System.out.println("finding out pool");
		this.outPoolTx = addrFinder.getKeysPaidBy(this.poolKeys);
		addrFinder.done();
	}

	public void dateParse() throws IOException {
		HashMap<String, Date> dateFilled = new HashMap<String, Date>();
		HashMap<String, Date> firstDateSpent = new HashMap<String, Date>();
		HashMap<String, Date> lastDateSpent = new HashMap<String, Date>();

		for (FinderResult tTx : this.intoPoolTx) {
			/*
			 * Ignore the strange tx
			 */
			if (tTx.getOuputs().size() > 1) {
				continue;
			}

			for (String tPK : tTx.getOuputs()) {
				dateFilled.put(tPK, tTx.getTimestamp());
			}
		}

		for (FinderResult tTx : this.outPoolTx) {
			for (String tPK : tTx.getInputs()) {
				if (this.poolKeys.contains(tPK)) {
					if ((!firstDateSpent.containsKey(tPK)) || (firstDateSpent.get(tPK).after(tTx.getTimestamp()))) {
						firstDateSpent.put(tPK, tTx.getTimestamp());
					}
					if ((!lastDateSpent.containsKey(tPK)) || (lastDateSpent.get(tPK).before(tTx.getTimestamp()))) {
						lastDateSpent.put(tPK, tTx.getTimestamp());
					}
				}
			}
		}

		List<Date> fillDates = new ArrayList<Date>(dateFilled.size());
		for (String tKey : dateFilled.keySet()) {
			fillDates.add(dateFilled.get(tKey));
		}
		Collections.sort(fillDates);
		List<Double> deltas = new ArrayList<Double>(fillDates.size() - 1);
		for (int pos = 0; pos < fillDates.size() - 1; pos++) {
			Date lhs = fillDates.get(pos);
			Date rhs = fillDates.get(pos + 1);
			double deltaMins = ((double) (rhs.getTime() - lhs.getTime())) / 60000.0;
			deltas.add(deltaMins);
		}
		BufferedWriter outFP = new BufferedWriter(new FileWriter("poolFillDeltaMin.csv"));
		for (double tDelta : deltas) {
			outFP.write("" + tDelta + "\n");
		}
		outFP.close();

		List<Double> firstToLastRemoval = new ArrayList<Double>(firstDateSpent.size());
		for (String tKey : firstDateSpent.keySet()) {
			double deltaMins = ((double) (lastDateSpent.get(tKey).getTime() - firstDateSpent.get(tKey).getTime()))
					/ 60000.0;
			firstToLastRemoval.add(deltaMins);
		}
		outFP = new BufferedWriter(new FileWriter("poolEmptyDelta.csv"));
		for (double tDelta : firstToLastRemoval) {
			outFP.write("" + tDelta + "\n");
		}
		outFP.close();
	}

	public static void main(String[] args) throws IOException{
		DepositDetails self = new DepositDetails(args[0]);
		self.dateParse();
	}

}
