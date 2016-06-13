package bitcoinLink;

import java.util.*;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ScriptException;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.params.MainNetParams;


public class BCJTranslator {
	
	private static final NetworkParameters params = MainNetParams.get();

	
	public static List<String> getOutputKeys(List<TransactionOutput> txOut){
		List<String> retList = new ArrayList<String>(txOut.size());
		for (TransactionOutput tx_o : txOut) {
			Address o_addr = null;
			try {
				o_addr = tx_o.getAddressFromP2PKHScript(params);
			} catch (ScriptException e) {
				break;
			}
			if (o_addr == null) {
				try {
					o_addr = tx_o.getAddressFromP2SH(params);
				} catch (ScriptException e) {
					break;
				}
			}
			if (o_addr != null) {
				retList.add(o_addr.toString());
			}
		}
		return retList;
	}
}
