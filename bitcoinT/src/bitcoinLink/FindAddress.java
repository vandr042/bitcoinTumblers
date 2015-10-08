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
	private static List<Transaction> getEm(Address addr) throws BlockStoreException, FileNotFoundException, InterruptedException, ExecutionException{
		
		WalletAppKit kit = new WalletAppKit(params, new File("foo"), "test");
		kit.startAsync();
		kit.awaitRunning();
		PeerGroup pGroup = kit.peerGroup();
		Peer dlPeer = pGroup.getDownloadPeer();
		BlockChain chain = kit.chain();
		StoredBlock stored_block = chain.getChainHead();
		
		/*File file1 = new File("clustersParse.txt");
		File file2 = new File("clusters.txt");
		PrintWriter hwriter = new PrintWriter("clusters.txt");
		PrintWriter pwriter = new PrintWriter("clustersParse.txt");*/

		/* STATS */
		double eCount = 0.0;
		double totalTx = 0.0;
		int addrAppears = 0;
		
		LinkedList<Transaction> tx_appears = new LinkedList(); 
		
		/* Increase counter to scale blocks fetched */
		int counter = 0;
		while(counter <= 50 && stored_block != null) {
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
							
							Address o_addr = tx_o.getAddressFromP2PKHScript(params);
							if(o_addr != null && o_addr.toString().equals(addr.toString()) == true){ //if it appears then add to list
								tx_appears.add(tx);
								addrAppears++;
								break;
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
		Address addr = new Address(params, "18PGNHvYzpPGz29G6mbZsPbuC47Ykzega");
		getEm(addr);
	}

}
