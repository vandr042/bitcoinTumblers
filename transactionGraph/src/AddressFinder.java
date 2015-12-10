

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
	private static final int SEARCH_DEPTH = 57000;
	private MaxBlockStore bstore;
	private Block stored_block;

	
	public AddressFinder(NetworkParameters parameters) throws IOException, InterruptedException, ExecutionException{
		bstore = new MaxBlockStore();
		params = parameters;
		stored_block = bstore.getBlock(bstore.getHeadOfChain());
		
	}

	/*
	 * addrAsOutput takes in an address and returns a 
	 * list of transaction in which that address 
	 * appears as an output.
	 */
	public LinkedList<Transaction> addrAsOutput(LinkedList<Address> addr) throws InterruptedException, ExecutionException, BlockStoreException{
		
		LinkedList<Transaction> tx_appears = new LinkedList<Transaction>();
		int currentSearchDepth = 0;
		int stepSize = (int)(Math.floor(AddressFinder.SEARCH_DEPTH)/10);
		int currStep = 1;
		
		/* stats */
		double exceptionCount = 0.0;
		double totalTx = 0.0;
		
		while (currentSearchDepth <= AddressFinder.SEARCH_DEPTH && stored_block != null) {
			
			if (currentSearchDepth >= stepSize * currStep) {
				System.out.println("" + currStep * 10 + "% done");
				currStep++;
			}
			List<Transaction> tx_list = stored_block.getTransactions();
			totalTx += tx_list.size();

			for (Transaction tx : tx_list) {
				List<TransactionOutput> tx_olist = tx.getOutputs();
				for (TransactionOutput tx_o : tx_olist) {
					Address o_addr = tx_o.getAddressFromP2PKHScript(params);
					boolean added = false;
					
					/* modified to check if any address from input list appears in transaction */
					for (Address address: addr){
						if (o_addr != null && o_addr.toString().equals(address.toString())) {
							tx_appears.add(tx);
							added = true;
							break;
						} else {
							o_addr = tx_o.getAddressFromP2SH(params);
							if (o_addr != null && o_addr.toString().equals(address.toString())) {
								tx_appears.add(tx);
								added = true;
								break;
							}
						}
					}
					if (added == true)
						break;
				}
			}
			stored_block = bstore.getBlock(stored_block.getPrevBlockHash());
			currentSearchDepth += 1;
		}
		return tx_appears;
	}
	
	
	
	
	/*
	 *  addrAsInput takes in an address and returns the 
	 *  transactions in which that address appears
	 *  as an input.
	 */
public LinkedList<Transaction> addrAsInput(LinkedList<Address> addr) throws InterruptedException, ExecutionException, BlockStoreException{
		
		LinkedList<Transaction> tx_appears = new LinkedList<Transaction>();
		int currentSearchDepth = 0;
		int stepSize = (int)(Math.floor(AddressFinder.SEARCH_DEPTH)/10);
		int currStep = 1;
		
		/* stats */
		double exceptionCount = 0.0;
		double totalTx = 0.0;
		
		while (currentSearchDepth <= AddressFinder.SEARCH_DEPTH && stored_block != null) {
			List<Transaction> tx_list = stored_block.getTransactions();
			totalTx += tx_list.size();
			
			if (currentSearchDepth >= stepSize * currStep) {
				System.out.println("" + currStep * 10 + "% done");
				currStep++;
			}
			for (Transaction tx : tx_list) {
				List<TransactionInput> tx_ilist = tx.getInputs();

				/* iterate over input and look for address */
				for (TransactionInput tx_input : tx_ilist) {
					boolean added = false;
					try {
						Address in_Addr = tx_input.getFromAddress();
						
						/* modified this to loop through address list and add transactions in which any of the addresses appear */
						for (Address address: addr){
							if (in_Addr.toString().equals(address.toString())) { //if it appears then add to list
								tx_appears.add(tx);
								added = true;
								break;
							}
						}
						

					} catch (ScriptException e) {
						exceptionCount++; //num exceptions
						break;
					}
					if (added == true)
						break;
				}
			}
			
			stored_block = bstore.getBlock(stored_block.getPrevBlockHash());
			currentSearchDepth += 1;
		}
		return tx_appears;
	}

	public void done(){
		bstore.done();
	}
	
	
	
	
	

	public static void main(String[] args) throws BlockStoreException, IOException, InterruptedException,
		ExecutionException, AddressFormatException {
	}

}
