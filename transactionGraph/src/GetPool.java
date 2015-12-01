import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.store.BlockStoreException;

public class GetPool {

	private AddressFinder addrFinder;
	private HashSet<Address> depKeys;
	private HashSet<Address> poolKeys;
	private static NetworkParameters params;
	
	public GetPool(){
		params = MainNetParams.get();
		addrFinder = new AddressFinder(params);
		depKeys = new HashSet<Address>();
		poolKeys = new HashSet<Address>();
	}
	
	/* takes in pool address and an a list as an argument and updates depKeys with new deposit keys */
	private LinkedList<Address> getInputs(LinkedList<Address> addrList, LinkedList<Address> newDKeys) throws InterruptedException, ExecutionException, BlockStoreException{
		LinkedList<Transaction> outputTx = addrFinder.addrAsOutput(addrList);
		for (Transaction tx:outputTx){
			List<TransactionInput> txInputs = tx.getInputs();
			for (TransactionInput txi:txInputs){
				boolean added = depKeys.add(txi.getFromAddress());
				if (added == true){
					newDKeys.add(txi.getFromAddress());
				}
			}
		}
		return newDKeys;
	}
	
	/* takes in deposit key as an argument and finds all of the pool keys known */
	private LinkedList<Address> getOutputs(LinkedList<Address> addrList, LinkedList<Address> newPKeys) throws InterruptedException, ExecutionException, BlockStoreException{
		LinkedList<Transaction> inputTx = addrFinder.addrAsInput(addrList);
		for (Transaction tx:inputTx){
			List<TransactionOutput> txOutputs = tx.getOutputs();
			for (TransactionOutput txo:txOutputs){
				boolean added;
				try{ 
					added = poolKeys.add(txo.getAddressFromP2PKHScript(params));
				}catch (ScriptException e){
					added = poolKeys.add(txo.getAddressFromP2SH(params));
				}
				if (added = true){
					newPKeys.add(txo.getAddressFromP2PKHScript(params));
				}
			}
		}
		return newPKeys;
	}
	
	/* takes in a pool key and builds sets of pool keys and deposit keys
	 * by calling getOutputs and getInputs until no new pool keys
	 * or deposit keys are found. 
	 */
	public int buildPool(Address poolAddr) throws InterruptedException, ExecutionException, BlockStoreException{
		LinkedList <Address> newDKeys = new LinkedList<Address>();
		LinkedList <Address> newPKeys = new LinkedList<Address>();
		newPKeys.add(poolAddr);
		getInputs(newPKeys, newDKeys);
		int rounds = 0;
		long currTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - currTime < 600000){
			rounds++;
			while (newPKeys.isEmpty() == false){ //empty newPKeys
				newPKeys.removeFirst();
			}
			this.getOutputs(newDKeys, newPKeys);
			if (newPKeys.size() != 0){ 
				while(newDKeys.isEmpty() == false){ //empty newDKeys
					newDKeys.removeFirst();
				}
				this.getInputs(newPKeys,newDKeys);
			}else{
				break;
			}
		}
		return rounds;
	}
	
	public HashSet<Address> getDepositKeys(){
		return depKeys;
	}
	public HashSet<Address> getPoolKeys(){
		return poolKeys;
	}
	
	public static void main(String[] args) throws AddressFormatException, InterruptedException, ExecutionException, BlockStoreException {
		GetPool poolBuilder = new GetPool();
		Address addr = new Address(params, args[0]);
		int rounds = poolBuilder.buildPool(addr);
		System.out.println(rounds);
		System.out.println(poolBuilder.poolKeys);
		System.out.println(poolBuilder.depKeys);
		
	}
}
