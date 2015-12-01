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
	
	private LinkedList<Address> getInputs(Address addr, LinkedList<Address> newDKeys) throws InterruptedException, ExecutionException, BlockStoreException{
		LinkedList<Transaction> outputTx = addrFinder.addrAsOutput(addr);
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
	
	private LinkedList<Address> getOutputs(Address addr, LinkedList<Address> newPKeys) throws InterruptedException, ExecutionException, BlockStoreException{
		LinkedList<Transaction> inputTx = addrFinder.addrAsInput(addr);
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
	
	public int buildPool(Address poolAddr) throws InterruptedException, ExecutionException, BlockStoreException{
		LinkedList <Address> newDKeys = new LinkedList<Address>();
		getInputs(poolAddr, newDKeys);
		int rounds = 0;
		long currTime = System.currentTimeMillis();
		while (System.currentTimeMillis() - currTime < 600000){
			rounds++;
			LinkedList<Address> newPKeys = new LinkedList<Address>();
			for (Address dkey:newDKeys){
				this.getOutputs(dkey, newPKeys);
			}
			if (newPKeys.size() != 0){
				while(newDKeys.isEmpty() == false){
					newDKeys.removeFirst();
				}
				for (Address pkey:newPKeys){
					this.getInputs(pkey,newDKeys);
				}
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
