package bitcoinLink;

import java.util.*;
import java.util.concurrent.ExecutionException;

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

public class AddressFinderWorker implements Runnable {

	private SimpleBlockStore bStore;
	private NetworkParameters params;
	private Context myCont;
	private Set<String> targetKeys;
	private boolean targetIsInput;
	private List<Sha256Hash> myHashes;

	private Set<FinderResult> results;
	private int inputExceptions;
	private int outputExceptions;
	private int totalTx;
	private Date earliestTxSeen;

	public AddressFinderWorker(Set<String> goalKeys, boolean targetsAreInputs, List<Sha256Hash> blockHashes,
			SimpleBlockStore blocks, Context bcjContext) {
		this.bStore = blocks;
		this.myCont = bcjContext;
		this.params = MainNetParams.get();

		this.targetKeys = goalKeys;
		this.targetIsInput = targetsAreInputs;
		this.myHashes = blockHashes;

		this.results = new HashSet<FinderResult>();
		this.inputExceptions = 0;
		this.outputExceptions = 0;
		this.totalTx = 0;
		this.earliestTxSeen = null;
	}

	public Set<FinderResult> getResults() {
		return this.results;
	}

	public int getInputExceptions() {
		return this.inputExceptions;
	}

	public int getOutputExceptions() {
		return this.outputExceptions;
	}

	public int getTotalTx() {
		return this.totalTx;
	}
	
	public Date getEarliestTxSeen(){
		return this.earliestTxSeen;
	}

	@Override
	public void run() {

		Context.propagate(this.myCont);

		for (Sha256Hash tHash : this.myHashes) {
			Block currentBlock = this.bStore.getBlock(tHash);
			List<Transaction> tx_list = currentBlock.getTransactions();
			this.totalTx += tx_list.size();

			Date txTS = currentBlock.getTime();
			for (Transaction tx : tx_list) {
				if(this.earliestTxSeen == null){
					this.earliestTxSeen = txTS;
				}else{
					if(this.earliestTxSeen.compareTo(txTS) > 0){
						this.earliestTxSeen = txTS;
					}
				}
				
				boolean found = false;
				if (targetIsInput) {
					List<TransactionInput> tx_ilist = tx.getInputs();
					for (TransactionInput tx_input : tx_ilist) {
						try {
							// TODO what should we ACTUALLY do here?
							String in_Addr = tx_input.getFromAddress().toString();
							found = targetKeys.contains(in_Addr);
						} catch (ScriptException e) {
							this.inputExceptions++;
							break;
						}

						/*
						 * If we see a target key in the inputs stop iterating,
						 * we're already going to add all of the outputs
						 */
						if (found) {
							break;
						}
					}

				} else {
					List<TransactionOutput> tx_olist = tx.getOutputs();
					for (TransactionOutput tx_o : tx_olist) {
						Address o_addr = null;
						try {
							o_addr = tx_o.getAddressFromP2PKHScript(params);
						} catch (ScriptException e) {
							this.outputExceptions++;
							break;
						}
						if (o_addr == null) {
							try {
								o_addr = tx_o.getAddressFromP2SH(params);
							} catch (ScriptException e) {
								this.outputExceptions++;
								break;
							}
						}
						if (o_addr == null) {
							this.outputExceptions++;
							continue;
						}

						found = targetKeys.contains(o_addr.toString());

						/*
						 * If we found a target key in outputs stop iterating
						 * across outputs, we're already adding the inputs
						 */
						if (found) {
							break;
						}
					}
				}

				if (found) {
					FinderResult tResult = new FinderResult(txTS);

					/*
					 * Iterate across outputs harvesting the keys since at least
					 * one input key matched a target
					 */
					for (TransactionOutput tx_o : tx.getOutputs()) {
						Address o_addr = null;
						try {
							o_addr = tx_o.getAddressFromP2PKHScript(params);
						} catch (ScriptException e) {
							this.outputExceptions++;
							break;
						}
						if (o_addr == null) {
							try {
								o_addr = tx_o.getAddressFromP2SH(params);
							} catch (ScriptException e) {
								this.outputExceptions++;
								break;
							}
						}
						if (o_addr != null) {
							tResult.addOutput(o_addr.toString(), tx_o.getValue());
						}
					}

					/*
					 * Iterate across inputs adding them since at leaste on
					 * output was in the target set
					 */
					for (TransactionInput tx_input : tx.getInputs()) {
						try {
							// TODO what should we ACTUALLY do here?
							Address in_Addr = tx_input.getFromAddress();
							tResult.addInput(in_Addr.toString());
						} catch (ScriptException e) {
							this.inputExceptions++;
							break;
						}
					}
					this.results.add(tResult);
				}

			}
		}
	}

}
