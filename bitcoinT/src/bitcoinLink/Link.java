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

public class Link {

	/**
	 * @param args
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * @throws BlockStoreException 
	 * @throws FileNotFoundException 
	 * @throws AddressFormatException 
	 */
	public static void main(String[] args) throws InterruptedException, ExecutionException, BlockStoreException, FileNotFoundException, AddressFormatException {
		NetworkParameters params = MainNetParams.get();
		WalletAppKit kit = new WalletAppKit(params, new File("foo"), "test");
		kit.startAsync();
		kit.awaitRunning();
		PeerGroup pGroup = kit.peerGroup();
		Peer dlPeer = pGroup.getDownloadPeer();
		BlockChain chain = kit.chain();
		StoredBlock stored_block = chain.getChainHead();
		HashMap<Address,LinkedList<Address>> cluster_map = new HashMap();
		
		File file1 = new File("clustersParse.txt");
		File file2 = new File("clusters.txt");
		PrintWriter hwriter = new PrintWriter("clusters.txt");
		PrintWriter pwriter = new PrintWriter("clustersParse.txt");

		/* STATS */
		int mergeCount = 0;
		int largestCluster = 0;
		double eCount = 0.0;
		double totalTx = 0.0;
		
		
		/* Increase counter to scale blocks fetched */
		int counter = 0;
		while(counter <= 26280 && stored_block != null) {
			Block tBlock = dlPeer.getBlock(stored_block.getHeader().getHash()).get();
			List<Transaction> tx_list = tBlock.getTransactions();
			System.out.println("has: " + tx_list.size() + "transactions"); 
			totalTx += tx_list.size();
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
					boolean exc = false;
					for (TransactionInput tx_input : tx_ilist) {
						try{
							LinkedList<Address> a_list = cluster_map.get(tx_input.getFromAddress());
							if (a_list != null){
								cluster_in_map = true;
								
								//loop over inputs to check if already in address array 
								for (TransactionInput txi : tx_ilist){
									Address addr = txi.getFromAddress();
								
									boolean found = false;
									for (int i = 0; i < a_list.size(); i++){ //loop through list checking for addr break if found
										if (addr.equals(a_list.get(i))) {
											found = true;
											break;
										}
									}//
									if (found == false){ //if address was not found in list then add it
										a_list.add(addr);
									}
								}
								mergeCount ++; //number times clusters merged
								break;
							}
						}catch (ScriptException e){
							//tx_ilist.remove(tx_input);  Would like to remove but doesn't work
							exc = true;
							cluster_in_map = true;
							eCount++; //num exceptions
							break;
						}
					
					}//end i_list loop
					
					/* if cluster is not in cluster_map */
					if (cluster_in_map == false){
						
						Address key = tx_ilist.get(0).getFromAddress();
						LinkedList<Address> new_cluster = new LinkedList();
						new_cluster.add(key);
						
						/* Add input addresses to new cluster */
						for (int i = 1; i < tx_ilist.size(); i++){
							Address new_addr = tx_ilist.get(i).getFromAddress();
							
							/* Check for duplicate addresses */
							boolean in_list = false;
							for (Address j: new_cluster){
								if (j.equals(new_addr)){
									in_list = true;
									break;
								}
							}
							/* address not already in list so add */
							if (in_list == false){
								new_cluster.add(new_addr);
							}
						}
						
						/* Add <Key, Cluster> to cluster_map if size > 1*/
						if (new_cluster.size() > 1) {
						
							cluster_map.put(key, new_cluster);
							
							if (new_cluster.size() > largestCluster){
								largestCluster = new_cluster.size();
							}
						}
						
						
						
					}
				}
			}
			stored_block = stored_block.getPrev(kit.store());
			counter++;
		}//end counter loop 
		Set<Address> keyset = cluster_map.keySet();
		Collection<LinkedList<Address>> values = cluster_map.values();
		Address[] keyArray = keyset.toArray(new Address[keyset.size()]);
		Object[] vArray = values.toArray();
		for (int i = 0; i< keyArray.length; i ++){
			pwriter.println(vArray[i]);
		}
		hwriter.println("Largest cluster :" + largestCluster);
		hwriter.println("Num Clusters: " + vArray.length);
		hwriter.println("Total Tx: " + totalTx);
		hwriter.println("%Tx used: " + (totalTx-eCount)/totalTx);
		hwriter.println("Clusters merged: " + mergeCount);
		hwriter.println("Exceptions: " + eCount);
	}//end main

}
