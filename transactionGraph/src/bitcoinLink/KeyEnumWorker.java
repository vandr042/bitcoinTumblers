package bitcoinLink;

import java.util.*;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.params.MainNetParams;

import blockStore.SimpleBlockStore;

public class KeyEnumWorker implements Runnable {

	private List<Sha256Hash> workBlocks;
	private boolean recordInputs;

	private SimpleBlockStore bStore;
	private Context myContex;
	private NetworkParameters params;

	private Set<String> results;
	private Date earliestTxSeen;

	public KeyEnumWorker(List<Sha256Hash> myBlocks, boolean wantInputs, SimpleBlockStore blockStore, Context bcjContext) {
		this.workBlocks = myBlocks;
		this.recordInputs = wantInputs;
		this.bStore = blockStore;
		this.params = MainNetParams.get();

		this.results = new HashSet<String>();
		this.earliestTxSeen = null;
	}

	public Set<String> getResults() {
		return this.results;
	}

	public Date getEarliestBlock() {
		return this.earliestTxSeen;
	}

	@Override
	public void run() {
		Context.propagate(this.myContex);

		for (Sha256Hash tHash : this.workBlocks) {
			Block currentBlock = this.bStore.getBlock(tHash);
			List<Transaction> tx_list = currentBlock.getTransactions();

			Date txTS = currentBlock.getTime();
			if (this.earliestTxSeen == null) {
				this.earliestTxSeen = txTS;
			} else {
				if (this.earliestTxSeen.compareTo(txTS) > 0) {
					this.earliestTxSeen = txTS;
				}
			}

			for (Transaction tx : tx_list) {
				if (this.recordInputs) {
					for (TransactionInput tx_input : tx.getInputs()) {
						try {
							// TODO what should we ACTUALLY do here?
							Address in_Addr = tx_input.getFromAddress();
							this.results.add(in_Addr.toString());
						} catch (ScriptException e) {
							break;
						}
					}
				} else {
					for (TransactionOutput tx_o : tx.getOutputs()) {
						Address o_addr = null;
						try {
							o_addr = tx_o.getAddressFromP2PKHScript(params);
						} catch (ScriptException e) {
							break;
						}
						if (o_addr == null) {
							try {
								o_addr = tx_o.getAddressFromP2SH(params);
							} catch (ScriptException e) {
								break;
							}
						}
						if (o_addr != null) {
							this.results.add(o_addr.toString());
						}
					}
				}

			}

		}
	}
}
