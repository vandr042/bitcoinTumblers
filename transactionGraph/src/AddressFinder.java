
import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.store.*;
import org.bitcoinj.utils.BriefLogFormatter;

import blockStore.MaxBlockStore;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.lang.Math;

public class AddressFinder {

	/**
	 * @param args
	 * @throws BlockStoreException
	 * @throws FileNotFoundException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private NetworkParameters params;
	private static final int SEARCH_DEPTH = 60000;
	private MaxBlockStore bstore;

	public AddressFinder(NetworkParameters parameters) throws IOException {
		bstore = new MaxBlockStore("/export/scratch2/public/blockStore");
		params = parameters;
	}

	public Set<String> getKeysPaidBy(Set<String> inputKeys) {
		return this.getKeysTouching(inputKeys, true);
	}

	public Set<String> getKeysPayingInto(Set<String> outputKeys) {
		return this.getKeysTouching(outputKeys, false);
	}

	//TODO this NEEDS to be done in parallel
	private Set<String> getKeysTouching(Set<String> targetKeys, boolean targetIsInput) {
		Set<String> keysTouching = new HashSet<String>();
		int currentSearchDepth = 0;
		int stepSize = (int) (Math.floor(AddressFinder.SEARCH_DEPTH) / 10);
		int currStep = 1;

		/* stats */
		double exceptionCount = 0.0;
		double totalTx = 0.0;

		try {
			Block currentBlock = this.bstore.getBlock(this.bstore.getHeadOfChain());
			while (currentSearchDepth <= AddressFinder.SEARCH_DEPTH && currentBlock != null) {

				if (currentSearchDepth >= stepSize * currStep) {
					System.out.println("" + currStep * 10 + "% done");
					currStep++;
				}

				List<Transaction> tx_list = currentBlock.getTransactions();
				totalTx += tx_list.size();

				for (Transaction tx : tx_list) {
					boolean found = false;
					if (targetIsInput) {
						List<TransactionInput> tx_ilist = tx.getInputs();
						for (TransactionInput tx_input : tx_ilist) {
							try {
								//TODO what should we ACTUALLY do here?
								String in_Addr = tx_input.getFromAddress().toString();
								found = targetKeys.contains(in_Addr); 
							} catch (ScriptException e) {
								exceptionCount++;
								break;
							}

							/*
							 * If we see a target key in the inputs stop
							 * iterating, we're already going to add all of the
							 * outputs
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
								exceptionCount++;
								break;
							}
							if (o_addr == null) {
								try {
									o_addr = tx_o.getAddressFromP2SH(params);
								} catch (ScriptException e) {
									exceptionCount++;
									break;
								}
							}
							if (o_addr == null) {
								exceptionCount++;
								continue;
							}

							found = targetKeys.contains(o_addr.toString());

							/*
							 * If we found a target key in outputs stop
							 * iterating across outputs, we're already adding
							 * the inputs
							 */
							if (found) {
								break;
							}
						}
					}

					if (found) {
						if (targetIsInput) {
							/*
							 * Iterate across outputs harvesting the keys since
							 * at least one input key matched a target
							 */
							for (TransactionOutput tx_o : tx.getOutputs()) {
								Address o_addr = null;
								try {
									o_addr = tx_o.getAddressFromP2PKHScript(params);
								} catch (ScriptException e) {
									exceptionCount++;
									break;
								}
								if (o_addr == null) {
									try {
										o_addr = tx_o.getAddressFromP2SH(params);
									} catch (ScriptException e) {
										exceptionCount++;
										break;
									}
								}
								if (o_addr != null) {
									keysTouching.add(o_addr.toString());
								}
							}
						} else {
							/*
							 * Iterate across inputs adding them since at leaste
							 * on output was in the target set
							 */
							for (TransactionInput tx_input : tx.getInputs()) {
								try {
									//TODO what should we ACTUALLY do here?
									Address in_Addr = tx_input.getFromAddress();
									keysTouching.add(in_Addr.toString());
								} catch (ScriptException e) {
									exceptionCount++;
									break;
								}
							}
						}
					}
				}
				currentBlock = bstore.getBlock(currentBlock.getPrevBlockHash());
				currentSearchDepth += 1;
			}
		} catch (ExecutionException | InterruptedException e2) {
			e2.printStackTrace();
			//TODO signal up the chain better/cleaner?
			System.exit(-1);
		}

		/*
		 * Report the fraction of txs we could not parse input/output for
		 */
		System.out.println(
				"Exception txs: " + exceptionCount + " out of " + totalTx + "(" + (exceptionCount / totalTx) + ")");

		return keysTouching;
	}

	/*
	 * addrAsOutput takes in an address and returns a list of transaction in
	 * which that address appears as an output.
	 */
	//	public LinkedList<Transaction> addrAsOutput(LinkedList<Address> addr)
	//			throws InterruptedException, ExecutionException, BlockStoreException {
	//
	//		LinkedList<Transaction> tx_appears = new LinkedList<Transaction>();
	//		int currentSearchDepth = 0;
	//		int stepSize = (int) (Math.floor(AddressFinder.SEARCH_DEPTH) / 10);
	//		int currStep = 1;
	//
	//		/* stats */
	//		double exceptionCount = 0.0;
	//		double totalTx = 0.0;
	//
	//		while (currentSearchDepth <= AddressFinder.SEARCH_DEPTH && stored_block != null) {
	//
	//			if (currentSearchDepth >= stepSize * currStep) {
	//				System.out.println("" + currStep * 10 + "% done");
	//				currStep++;
	//			}
	//			List<Transaction> tx_list = stored_block.getTransactions();
	//			totalTx += tx_list.size();
	//
	//			for (Transaction tx : tx_list) {
	//				List<TransactionOutput> tx_olist = tx.getOutputs();
	//				for (TransactionOutput tx_o : tx_olist) {
	//					Address o_addr = tx_o.getAddressFromP2PKHScript(params);
	//					boolean added = false;
	//
	//					/*
	//					 * modified to check if any address from input list appears
	//					 * in transaction
	//					 */
	//					for (Address address : addr) {
	//						if (o_addr != null && o_addr.toString().equals(address.toString())) {
	//							tx_appears.add(tx);
	//							added = true;
	//							break;
	//						} else {
	//							o_addr = tx_o.getAddressFromP2SH(params);
	//							if (o_addr != null && o_addr.toString().equals(address.toString())) {
	//								tx_appears.add(tx);
	//								added = true;
	//								break;
	//							}
	//						}
	//					}
	//					if (added == true)
	//						break;
	//				}
	//			}
	//			stored_block = bstore.getBlock(stored_block.getPrevBlockHash());
	//			currentSearchDepth += 1;
	//		}
	//		return tx_appears;
	//	}

	/*
	 * addrAsInput takes in an address and returns the transactions in which
	 * that address appears as an input.
	 */
//	public LinkedList<Transaction> addrAsInput(LinkedList<Address> addr)
//			throws InterruptedException, ExecutionException, BlockStoreException {
//
//		LinkedList<Transaction> tx_appears = new LinkedList<Transaction>();
//		int currentSearchDepth = 0;
//		int stepSize = (int) (Math.floor(AddressFinder.SEARCH_DEPTH) / 10);
//		int currStep = 1;
//
//		/* stats */
//		double exceptionCount = 0.0;
//		double totalTx = 0.0;
//
//		while (currentSearchDepth <= AddressFinder.SEARCH_DEPTH && stored_block != null) {
//			List<Transaction> tx_list = stored_block.getTransactions();
//			totalTx += tx_list.size();
//
//			if (currentSearchDepth >= stepSize * currStep) {
//				System.out.println("" + currStep * 10 + "% done");
//				currStep++;
//			}
//			for (Transaction tx : tx_list) {
//				List<TransactionInput> tx_ilist = tx.getInputs();
//
//				/* iterate over input and look for address */
//				for (TransactionInput tx_input : tx_ilist) {
//					boolean added = false;
//					try {
//						Address in_Addr = tx_input.getFromAddress();
//
//						/*
//						 * modified this to loop through address list and add
//						 * transactions in which any of the addresses appear
//						 */
//						for (Address address : addr) {
//							if (in_Addr.toString().equals(address.toString())) { //if it appears then add to list
//								tx_appears.add(tx);
//								added = true;
//								break;
//							}
//						}
//
//					} catch (ScriptException e) {
//						exceptionCount++; //num exceptions
//						break;
//					}
//					if (added == true)
//						break;
//				}
//			}
//
//			stored_block = bstore.getBlock(stored_block.getPrevBlockHash());
//			currentSearchDepth += 1;
//		}
//		return tx_appears;
//	}

	public void done() {
		this.bstore.done();
	}


}
