import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
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
	private LinkedList<Address> getInputs(LinkedList<Address> addrList, LinkedList<Address> newDKeys, PrintWriter dwriter) throws InterruptedException, ExecutionException, BlockStoreException{
		LinkedList<Transaction> outputTx = addrFinder.addrAsOutput(addrList);
		for (Transaction tx:outputTx){
			List<TransactionInput> txInputs = tx.getInputs();
			for (TransactionInput txi:txInputs){
				Address addr = txi.getFromAddress();
				boolean added = depKeys.add(addr);
				if (added == true){
					dwriter.write(addr.toString());
					newDKeys.add(addr);
				}
			}
		}
		return newDKeys;
	}
	
	/* takes in deposit key as an argument and finds all of the pool keys known */
	private LinkedList<Address> getOutputs(LinkedList<Address> addrList, LinkedList<Address> newPKeys, PrintWriter pwriter) throws InterruptedException, ExecutionException, BlockStoreException{
		LinkedList<Transaction> inputTx = addrFinder.addrAsInput(addrList);
		for (Transaction tx:inputTx){
			List<TransactionOutput> txOutputs = tx.getOutputs();
			for (TransactionOutput txo:txOutputs){
				boolean added;
				Address addr;
				try{ 
					addr = (txo.getAddressFromP2PKHScript(params));
					added = poolKeys.add(addr);
				}catch (ScriptException e){
					addr = (txo.getAddressFromP2SH(params));
					added = poolKeys.add(addr);
				}
				if (added = true){
					pwriter.write(addr.toString());
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
	public int buildPool(Address depAddr,PrintWriter pwriter, PrintWriter dwriter) throws InterruptedException, ExecutionException, BlockStoreException{
		LinkedList <Address> newDKeys = new LinkedList<Address>();
		LinkedList <Address> newPKeys = new LinkedList<Address>();
		newDKeys.add(depAddr);
		getOutputs(newDKeys, newPKeys, pwriter);
		int rounds = 0;
		//long currTime = System.currentTimeMillis();
		while (true){
			rounds++;
			while (newDKeys.isEmpty() == false){ //empty newPKeys
				newDKeys.removeFirst();
			}
			this.getInputs(newPKeys, newDKeys, pwriter);
			if (newDKeys.size() != 0){ 
				while(newPKeys.isEmpty() == false){ //empty newDKeys
					newPKeys.removeFirst();
				}
				this.getOutputs(newPKeys,newPKeys, dwriter);
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
	
	public static void main(String[] args) throws AddressFormatException, InterruptedException, ExecutionException, BlockStoreException, FileNotFoundException {
		GetPool poolBuilder = new GetPool();
		File f1 = new File("pkeys.txt");
		PrintWriter pwriter = new PrintWriter("pkeys.txt");
		File f2 = new File("dkeys.txt");
		PrintWriter dwriter = new PrintWriter("dkeys.txt");
		Address addr = new Address(params, args[0]);
		int rounds = poolBuilder.buildPool(addr, pwriter, dwriter);
		System.out.println(rounds);
	}
}
