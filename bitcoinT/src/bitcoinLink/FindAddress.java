package bitcoinLink;

import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.store.*;
import org.bitcoinj.utils.BriefLogFormatter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
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
	private static LinkedList<Transaction> getEm(Address addr) throws BlockStoreException, FileNotFoundException, InterruptedException, ExecutionException{
		
		WalletAppKit kit = new WalletAppKit(params, new File("foo"), "test");
		kit.startAsync();
		kit.awaitRunning();
		PeerGroup pGroup = kit.peerGroup();
		Peer dlPeer = pGroup.getDownloadPeer();
		BlockChain chain = kit.chain();
		StoredBlock stored_block = chain.getChainHead();
		
		/* STATS */
		double eCount = 0.0;
		double totalTx = 0.0;
		int addrAppears = 0;
		
		LinkedList<Transaction> tx_appears = new LinkedList(); 
		
		/* Increase counter to scale blocks fetched */
		int counter = 0;
		while(counter <= 2 && stored_block != null) {
			Block tBlock = dlPeer.getBlock(stored_block.getHeader().getHash()).get();
			List<Transaction> tx_list = tBlock.getTransactions(); 
			totalTx += tx_list.size();
			for (Transaction tx : tx_list) {
				List<TransactionInput> tx_ilist = tx.getInputs();
				List<TransactionOutput> tx_olist = tx.getOutputs();
					
					boolean addrFound = false;
							
					/* iterate over input and look for address */ 
					for (TransactionInput tx_input : tx_ilist) {
						try{
							Address in_Addr = tx_input.getFromAddress();
							if (in_Addr.toString().equals(addr.toString()) == true){ //if it appears then add to list
								tx_appears.add(tx);
								addrFound = true;
								addrAppears++;
								break;
							}
							
						}catch (ScriptException e){
							eCount++; //num exceptions
							addrFound = true;
							break;
						}
					
					}//end i_list loop
					
					/* if address wasn't an input check if an output */
					if (addrFound == false){
						for(TransactionOutput tx_o : tx_olist){
							System.out.println(tx_o.getValue());
							Address o_addr = tx_o.getAddressFromP2PKHScript(params);
							if(o_addr != null && o_addr.toString().equals(addr.toString()) == true){ //if it appears then add to list
								tx_appears.add(tx);
								addrAppears++;
								break;
							}else{
								o_addr = tx_o.getAddressFromP2SH(params);
								if(o_addr != null && o_addr.toString().equals(addr.toString()) == true){
									tx_appears.add(tx);
									addrAppears++;
									break;
								}
							}
						}
					}
					addrFound = false;
			}//end tx_list loop
			stored_block = stored_block.getPrev(kit.store());
			counter++;
		}//end counter loop 
		
		System.out.println("Total Tx: " + totalTx);
		System.out.println("%Tx used: " + ((totalTx-eCount)/totalTx) * 100);
		System.out.println("Exceptions: " + eCount);
		System.out.println("Address Appears: " + addrAppears);
		
		System.out.println(tx_appears);
		
		return tx_appears;
	}
	
	public static void main(String[] args) throws BlockStoreException, FileNotFoundException, InterruptedException, ExecutionException, AddressFormatException {
		Address addr = new Address(params, args[0]);
		LinkedList<Transaction> tx_list = getEm(addr);
		File file1 = new File(args[0] + ".txt");
		PrintWriter pwriter = new PrintWriter(file1);
		for (Transaction tx : tx_list){
			//inputs
			for (TransactionInput tx_i:(tx.getInputs())){
				pwriter.print(tx_i.getFromAddress() + "|");
			}
			pwriter.println();
			//outputs
			for (TransactionOutput tx_o: tx.getOutputs()){
				Address o_addr = tx_o.getAddressFromP2PKHScript(params);
				if (o_addr == null){
					o_addr = tx_o.getAddressFromP2SH(params);
				
				}
				pwriter.print(o_addr + "|");
				pwriter.println();
			}
			pwriter.println();
			/* NEED TO PRINT TRANSACTION VALUE HERE */
			pwriter.close();
		
			
		}
	}

}
