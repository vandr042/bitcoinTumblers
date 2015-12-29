package bitblender;

import java.util.*;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.Context;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;

import bitcoinLink.BCJTranslator;
import blockStore.SimpleBlockStore;

public class PoolHuntWorker implements Runnable {

	private List<Sha256Hash> myBlocks;
	private Context context;
	private SimpleBlockStore bStore;

	private HashMap<String, Date> dateFilled;
	private HashMap<String, Integer> timesSeen;
	private HashMap<String, Boolean> oddTxSeen;
	private Set<String> rejected;

	public PoolHuntWorker(List<Sha256Hash> blocks, Context bcjContext, SimpleBlockStore bstore) {
		this.myBlocks = blocks;
		this.context = bcjContext;
		this.bStore = bstore;

		this.dateFilled = new HashMap<String, Date>();
		this.timesSeen = new HashMap<String, Integer>();
		this.oddTxSeen = new HashMap<String, Boolean>();
		this.rejected = new HashSet<String>();
	}

	@Override
	public void run() {
		Context.propagate(this.context);

		for (Sha256Hash tBlockHash : this.myBlocks) {
			Block currBlock = this.bStore.getBlock(tBlockHash);
			Date blockDate = currBlock.getTime();
			for (Transaction tTx : currBlock.getTransactions()) {
				List<String> outputKeys = BCJTranslator.getOutputKeys(tTx.getOutputs());
				if (tTx.getInputs().size() > 1) {
					this.rejectAll(outputKeys);
					continue;
				}
				if (outputKeys.size() > 2) {
					this.rejectAll(outputKeys);
					continue;
				}

				for (String tKey : outputKeys) {
					//FIXME odd tx check
					if (!this.timesSeen.containsKey(tKey)) {
						this.timesSeen.put(tKey, 1);
						this.dateFilled.put(tKey, blockDate);
					} else {
						if (this.dateFilled.get(tKey).compareTo(blockDate) != 0) {
							this.reject(tKey);
						} else {
							this.timesSeen.put(tKey, this.timesSeen.get(tKey) + 1);
						}
					}
				}
			}
		}
	}
	
	public Set<String> getRejected(){
		return this.rejected;
	}
	
	public HashMap<String, Date> getDateSeen(){
		return this.dateFilled;
	}
	
	public HashMap<String, Integer> getTimesSeen(){
		return this.timesSeen;
	}
	
	public HashMap<String, Boolean> getOddTxSeen(){
		return this.oddTxSeen;
	}

	private void reject(String rejectedKey) {
		this.rejected.add(rejectedKey);
		this.dateFilled.remove(rejectedKey);
		this.timesSeen.remove(rejectedKey);
	}

	private void rejectAll(List<String> rejectList) {
		rejected.addAll(rejectList);
		for (String tKey : rejectList) {
			this.dateFilled.remove(tKey);
			this.timesSeen.remove(tKey);
		}
	}

}
