package bitcoinT;

import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.store.*;
import org.bitcoinj.utils.BriefLogFormatter;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Link {

	/**
	 * @param args
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * @throws BlockStoreException 
	 */
	public static void main(String[] args) throws InterruptedException, ExecutionException, BlockStoreException {
		NetworkParameters params = MainNetParams.get();
		WalletAppKit kit = new WalletAppKit(params, new File("foo"), "test");
		kit.startAsync();
		kit.awaitRunning();
		PeerGroup pGroup = kit.peerGroup();
		Peer dlPeer = pGroup.getDownloadPeer();
		BlockChain chain = kit.chain();
		StoredBlock stored_block = chain.getChainHead();
		
		HashMap<Address,LinkedList<Address>> cluster_map = new HashMap();
		
		/* Increases counter to scale number of transactions */
		for (int counter = 10; counter < 11; counter++) {
			Block tBlock = dlPeer.getBlock(stored_block.getHeader().getHash()).get();
			List<Transaction> tx_list = tBlock.getTransactions();
			for (Transaction tx : tx_list) {
				List<TransactionInput> tx_ilist = tx.getInputs();
				if (tx_ilist.size() > 1) {
					
					/* Iterate over input list
					 * If address in map then iterate over cluster adding addresses that aren't already in the array 
					 * 
					 * NOTE: SHOULD ADD MULTIPLE TRY CATCH
					 * -case where first input is recognized and then another input after throws e 
					 */
					
					boolean cluster_in_map = false;
					for (TransactionInput tx_input : tx_ilist) {
						try{
							LinkedList<Address> a_list = cluster_map.get(tx_input.getFromAddress());
							if (a_list != null){
								cluster_in_map = true;
								
								//loop over inputs to check if already in address array 
								for (TransactionInput txi : tx_ilist){
									Address addr = txi.getFromAddress();
									Address current = a_list.getFirst();
								
									boolean found = false;
									for (int i = 0; i < a_list.size(); i++){ //loop through list checking for addr break if found
										if (addr == current){
											found = true;
											break;
										}
										current = a_list.get(i);
									}//
									if (found == false){ //if address was not found in list then add it
										a_list.add(addr);
									}
								}
							}
						}catch (ScriptException e){
							//tx_ilist.remove(tx_input);  Would like to remove but doesn't work
							cluster_in_map = true;
							break;
						}
							
					}//end i_list loop
					
					/* if cluster is not in cluster_map */
					if (cluster_in_map == false && tx_ilist.size() > 1){
						Address key = tx_ilist.get(0).getFromAddress();
						LinkedList<Address> new_cluster = new LinkedList();
						
						/* Add input addresses to new cluster */
						for (TransactionInput txi : tx_ilist){
							Address new_addr = txi.getFromAddress();
							new_cluster.add(new_addr);
						}
						
						/* Add <Key, Cluster> to cluster_map */
						cluster_map.put(key, new_cluster);
						
					}
				}
			}
			stored_block = stored_block.getPrev(kit.store());
		}//counter loop
		Set<Address> keyset = cluster_map.keySet();
		Collection<LinkedList<Address>> values = cluster_map.values();
		System.out.println(values.size());
		System.out.println(keyset);
	}//end main

}
