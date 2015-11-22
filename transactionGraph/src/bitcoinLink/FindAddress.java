package bitcoinLink;

import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.store.*;
import org.bitcoinj.utils.BriefLogFormatter;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class FindAddress {

	/**
	 * @param args
	 * @throws BlockStoreException
	 * @throws FileNotFoundException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private static NetworkParameters params = MainNetParams.get();
	private static final int SEARCH_DEPTH = 100;

	private static LinkedList<Transaction> getTransactionsTouching(Address addr) throws BlockStoreException,
			FileNotFoundException, InterruptedException, ExecutionException {

		WalletAppKit kit = new WalletAppKit(params, new File("foo"), "test");
		kit.startAsync();
		kit.awaitRunning();
		PeerGroup pGroup = kit.peerGroup();
		Peer dlPeer = pGroup.getDownloadPeer();
		BlockChain chain = kit.chain();
		StoredBlock stored_block = chain.getChainHead();

		/* STATS */
		double exceptionCount = 0.0;
		double totalTx = 0.0;
		int addrAsInput = 0;
		int addrAsOutput = 0;

		LinkedList<Transaction> tx_appears = new LinkedList<>();

		/*
		 * Search depth variables
		 */
		int currentSearchDepth = 0;
		int stepSize = Math.floorDiv(FindAddress.SEARCH_DEPTH, 10);
		int currStep = 1;

		while (currentSearchDepth <= FindAddress.SEARCH_DEPTH && stored_block != null) {

			/*
			 * Progress reporting
			 */
			if (currentSearchDepth >= stepSize * currStep) {
				System.out.println("" + currStep * 10 + "% done");
				currStep++;
			}

			Block tBlock = dlPeer.getBlock(stored_block.getHeader().getHash()).get();
			List<Transaction> tx_list = tBlock.getTransactions();
			totalTx += tx_list.size();
			for (Transaction tx : tx_list) {
				List<TransactionInput> tx_ilist = tx.getInputs();
				List<TransactionOutput> tx_olist = tx.getOutputs();

				boolean addrFound = false;

				/* iterate over input and look for address */
				for (TransactionInput tx_input : tx_ilist) {
					try {
						Address in_Addr = tx_input.getFromAddress();
						if (in_Addr.toString().equals(addr.toString())) { //if it appears then add to list
							tx_appears.add(tx);
							addrFound = true;
							addrAsInput++;
							break;
						}

					} catch (ScriptException e) {
						exceptionCount++; //num exceptions
						addrFound = true;
						break;
					}

				}//end i_list loop

				/* if address wasn't an input check if an output */
				if (addrFound == false) {
					for (TransactionOutput tx_o : tx_olist) {
						Address o_addr = tx_o.getAddressFromP2PKHScript(params);
						/*
						 * if it appears then add to list
						 */
						if (o_addr != null && o_addr.toString().equals(addr.toString())) {
							tx_appears.add(tx);
							addrAsOutput++;
							addrFound = true;
							break;
						} else {
							o_addr = tx_o.getAddressFromP2SH(params);
							if (o_addr != null && o_addr.toString().equals(addr.toString())) {
								tx_appears.add(tx);
								addrAsOutput++;
								addrFound = true;
								break;
							}
						}
					}
				}
				addrFound = false;
			}//end tx_list loop
			stored_block = stored_block.getPrev(kit.store());
			currentSearchDepth++;
		}//end counter loop 

		System.out.println("Total Tx: " + totalTx);
		System.out.println("%Tx used: " + ((totalTx - exceptionCount) / totalTx) * 100);
		System.out.println("Address Appears As Input: " + addrAsInput);
		System.out.println("Address Appears As Output: " + addrAsOutput);

		return tx_appears;
	}

	public static void main(String[] args) throws BlockStoreException, IOException, InterruptedException,
			ExecutionException, AddressFormatException {
		Address addr = new Address(params, args[0]);
		LinkedList<Transaction> tx_list = getTransactionsTouching(addr);

		BufferedWriter outBuffer = new BufferedWriter(new FileWriter(args[0] + "-transactionsInvolved.txt"));
		for (Transaction tx : tx_list) {
			/*
			 * Format txt inputs
			 */
			/*
			 * TODO this gives us transactions as inputs now (as it should) we
			 * should print out which bitcoin address from said transaction is
			 * used as the input
			 */
			StringBuilder outStr = new StringBuilder();
			List<TransactionInput> tempTXInputs = tx.getInputs();
			for (int tempPos = 0; tempPos < tempTXInputs.size(); tempPos++) {
				outStr.append(tempTXInputs.get(tempPos));
				if (tempPos < tempTXInputs.size() - 1) {
					outStr.append(",");
				}
			}
			outStr.append("\n");

			//outputs
			List<TransactionOutput> tempTxOutputs = tx.getOutputs();
			for (int tempPos = 0; tempPos < tempTxOutputs.size(); tempPos++) {
				Address o_addr = tempTxOutputs.get(tempPos).getAddressFromP2PKHScript(params);
				if (o_addr == null) {
					o_addr = tempTxOutputs.get(tempPos).getAddressFromP2SH(params);

				}
				outStr.append(o_addr);
				if (tempPos < tempTxOutputs.size() - 1) {
					outStr.append(",");
				}
			}
			outBuffer.write(outStr.toString());
			outBuffer.write("\n\n");
			/* NEED TO PRINT TRANSACTION VALUE HERE */
		}
		outBuffer.close();
	}

}
