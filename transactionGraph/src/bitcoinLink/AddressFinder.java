package bitcoinLink;

import org.bitcoinj.core.*;

import blockStore.SimpleBlockStore;

import java.io.*;
import java.util.*;

public class AddressFinder {

	private List<Sha256Hash> fullHashList;
	private SimpleBlockStore bstore;

	private static final int SEARCH_DEPTH = 10000;
	private static final int NTHREADS = 10;

	public AddressFinder(NetworkParameters parameters) throws IOException {
		this.bstore = new SimpleBlockStore("/export/scratch2/public/shardBS");
		this.fullHashList = this.bstore.getHashChain(AddressFinder.SEARCH_DEPTH);
	}

	public Set<FinderResult> getKeysPaidBy(Set<String> inputKeys) {
		return this.getKeysTouching(inputKeys, true);
	}

	public Set<FinderResult> getKeysPayingInto(Set<String> outputKeys) {
		return this.getKeysTouching(outputKeys, false);
	}

	private Set<FinderResult> getKeysTouching(Set<String> targetKeys, boolean targetIsInput) {
		Set<FinderResult> keysTouching = new HashSet<FinderResult>();

		List<List<Sha256Hash>> workLists = new ArrayList<List<Sha256Hash>>(AddressFinder.NTHREADS);
		for (int counter = 0; counter < AddressFinder.NTHREADS; counter++) {
			workLists.add(new LinkedList<Sha256Hash>());
		}
		int pos = 0;
		for (Sha256Hash tHash : this.fullHashList) {
			workLists.get(pos % workLists.size()).add(tHash);
			pos++;
		}

		AddressFinderWorker[] slaves = new AddressFinderWorker[AddressFinder.NTHREADS];
		for (int counter = 0; counter < AddressFinder.NTHREADS; counter++) {
			slaves[counter] = new AddressFinderWorker(targetKeys, targetIsInput, workLists.get(counter), this.bstore,
					Context.get());
		}
		Thread[] threads = new Thread[AddressFinder.NTHREADS];
		for (int counter = 0; counter < AddressFinder.NTHREADS; counter++) {
			threads[counter] = new Thread(slaves[counter]);
		}

		/*
		 * Start threads and wait for them to finish
		 */
		long startTime = System.currentTimeMillis();
		for (Thread tThread : threads) {
			tThread.start();
		}
		for (Thread tThread : threads) {
			try {
				tThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		long stopTime = System.currentTimeMillis();

		int totalInputException = 0;
		int totalOutputException = 0;
		int totalTx = 0;
		Date earliestTX = null;
		for (AddressFinderWorker tSlave : slaves) {
			totalInputException += tSlave.getInputExceptions();
			totalOutputException += tSlave.getOutputExceptions();
			totalTx += tSlave.getTotalTx();
			keysTouching.addAll(tSlave.getResults());
			
			if(earliestTX == null){
				earliestTX = tSlave.getEarliestTxSeen();
			}else{
				if(earliestTX.compareTo(tSlave.getEarliestTxSeen()) > 0){
					earliestTX = tSlave.getEarliestTxSeen();
				}
			}
		}

		System.out.println("total input exceptions " + totalInputException);
		System.out.println("total output exceptions " + totalOutputException);
		System.out.println("total tx " + totalTx);
		System.out.println("earliest tx seen from: " + earliestTX.toString());
		System.out.println("Time taken " + (stopTime - startTime) / 1000 + " seconds");

		return keysTouching;
	}

	/*
	 * addrAsOutput takes in an address and returns a list of transaction in
	 * which that address appears as an output.
	 */
	// public LinkedList<Transaction> addrAsOutput(LinkedList<Address> addr)
	// throws InterruptedException, ExecutionException, BlockStoreException {
	//
	// LinkedList<Transaction> tx_appears = new LinkedList<Transaction>();
	// int currentSearchDepth = 0;
	// int stepSize = (int) (Math.floor(AddressFinder.SEARCH_DEPTH) / 10);
	// int currStep = 1;
	//
	// /* stats */
	// double exceptionCount = 0.0;
	// double totalTx = 0.0;
	//
	// while (currentSearchDepth <= AddressFinder.SEARCH_DEPTH && stored_block
	// != null) {
	//
	// if (currentSearchDepth >= stepSize * currStep) {
	// System.out.println("" + currStep * 10 + "% done");
	// currStep++;
	// }
	// List<Transaction> tx_list = stored_block.getTransactions();
	// totalTx += tx_list.size();
	//
	// for (Transaction tx : tx_list) {
	// List<TransactionOutput> tx_olist = tx.getOutputs();
	// for (TransactionOutput tx_o : tx_olist) {
	// Address o_addr = tx_o.getAddressFromP2PKHScript(params);
	// boolean added = false;
	//
	// /*
	// * modified to check if any address from input list appears
	// * in transaction
	// */
	// for (Address address : addr) {
	// if (o_addr != null && o_addr.toString().equals(address.toString())) {
	// tx_appears.add(tx);
	// added = true;
	// break;
	// } else {
	// o_addr = tx_o.getAddressFromP2SH(params);
	// if (o_addr != null && o_addr.toString().equals(address.toString())) {
	// tx_appears.add(tx);
	// added = true;
	// break;
	// }
	// }
	// }
	// if (added == true)
	// break;
	// }
	// }
	// stored_block = bstore.getBlock(stored_block.getPrevBlockHash());
	// currentSearchDepth += 1;
	// }
	// return tx_appears;
	// }

	/*
	 * addrAsInput takes in an address and returns the transactions in which
	 * that address appears as an input.
	 */
	// public LinkedList<Transaction> addrAsInput(LinkedList<Address> addr)
	// throws InterruptedException, ExecutionException, BlockStoreException {
	//
	// LinkedList<Transaction> tx_appears = new LinkedList<Transaction>();
	// int currentSearchDepth = 0;
	// int stepSize = (int) (Math.floor(AddressFinder.SEARCH_DEPTH) / 10);
	// int currStep = 1;
	//
	// /* stats */
	// double exceptionCount = 0.0;
	// double totalTx = 0.0;
	//
	// while (currentSearchDepth <= AddressFinder.SEARCH_DEPTH && stored_block
	// != null) {
	// List<Transaction> tx_list = stored_block.getTransactions();
	// totalTx += tx_list.size();
	//
	// if (currentSearchDepth >= stepSize * currStep) {
	// System.out.println("" + currStep * 10 + "% done");
	// currStep++;
	// }
	// for (Transaction tx : tx_list) {
	// List<TransactionInput> tx_ilist = tx.getInputs();
	//
	// /* iterate over input and look for address */
	// for (TransactionInput tx_input : tx_ilist) {
	// boolean added = false;
	// try {
	// Address in_Addr = tx_input.getFromAddress();
	//
	// /*
	// * modified this to loop through address list and add
	// * transactions in which any of the addresses appear
	// */
	// for (Address address : addr) {
	// if (in_Addr.toString().equals(address.toString())) { //if it appears then
	// add to list
	// tx_appears.add(tx);
	// added = true;
	// break;
	// }
	// }
	//
	// } catch (ScriptException e) {
	// exceptionCount++; //num exceptions
	// break;
	// }
	// if (added == true)
	// break;
	// }
	// }
	//
	// stored_block = bstore.getBlock(stored_block.getPrevBlockHash());
	// currentSearchDepth += 1;
	// }
	// return tx_appears;
	// }

	public void done() {
		this.bstore.done();
	}

}
