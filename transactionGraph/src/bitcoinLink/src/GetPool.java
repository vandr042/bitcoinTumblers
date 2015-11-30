import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.store.BlockStoreException;

public class GetPool {

	private AddressFinder addrFinder;
	private HashSet<Address> depKeys;
	private HashSet<Address> poolKeys;
	private LinkedList<Address> newPKeys;
	private LinkedList<Address> newDKeys;
	private static NetworkParameters params;
	
	public GetPool(){
		params = MainNetParams.get();
		addrFinder = new AddressFinder(params);
		depKeys = new HashSet<Address>();
		poolKeys = new HashSet<Address>();
	}
	
	private boolean getInputs(Address addr) throws InterruptedException, ExecutionException, BlockStoreException{
		LinkedList<Transaction> outputTx = addrFinder.addrAsOutput(addr);
		boolean newAddr = false;
		for (Transaction tx:outputTx){
			LinkedList<TransactionInput> txInputs = (LinkedList<TransactionInput>) tx.getInputs();
			for (TransactionInput txi:txInputs){
				boolean added = depKeys.add(txi.getFromAddress());
				if (added == true){
					newDKeys.add(txi.getFromAddress());
					newAddr = true;
				}
			}
		}
		return newAddr;
	}
	
	private boolean getOutputs(Address addr) throws InterruptedException, ExecutionException, BlockStoreException{
		LinkedList<Transaction> inputTx = addrFinder.addrAsInput(addr);
		boolean newAddr = false;
		for (Transaction tx:inputTx){
			LinkedList<TransactionOutput> txOutputs = (LinkedList<TransactionOutput>) tx.getOutputs();
			for (TransactionOutput txo:txOutputs){
				boolean added = poolKeys.add(txo.getAddressFromP2PKHScript(params));
				if (added = true){
					newPKeys.add(txo.getAddressFromP2PKHScript(params));
					newAddr = true;
				}
			}
		}
		return newAddr;
	}
	
	public int buildPool(Address poolAddr) throws InterruptedException, ExecutionException, BlockStoreException{
		this.getInputs(poolAddr);
		int rounds = 0;
		while (newDKeys.size()!= 0){
			rounds++;
			newPKeys = new LinkedList<Address>();
			for (Address dkey:newDKeys){
				this.getOutputs(dkey);
			}
			if (newPKeys.size() != 0){
				newDKeys = new LinkedList<Address>();
				for (Address pkey:newPKeys){
					this.getInputs(pkey);
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
	}
}
