package bitcoinLink;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.*;
import org.bitcoinj.core.*;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.store.*;
import org.bitcoinj.utils.BriefLogFormatter;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class PeerHelper implements Runnable {
	
	static HashMap<InetAddress, Long> addrMap;
	static Peer aPeer;
	File file1 = new File("connections.txt");
	static PrintStream writer;
	static PrintWriter writer1;
	static AddressMessage message;
	
	
	public PeerHelper(Peer peer, HashMap hm) throws FileNotFoundException{
		addrMap = hm;
		aPeer = peer;
		writer = new PrintStream("connections.txt");
	}
	
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
	}

	@Override
	public void run() {
		
		/*keep asking peers for addresses until main thread in peerFinder terminates */
		while(true){
		/* not sure what to do with an exception */
			try {
				message = aPeer.getAddr().get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			
			/* get inetAdress and check if in map, if not add else update if time updated */
			List<PeerAddress> addresses = message.getAddresses();
			for (PeerAddress addr: addresses){
				InetAddress inetAddr = addr.getAddr();
				writer.println(addr);
				writer.println("Trying to get inetADDR: " + addrMap.get(inetAddr));
				if (addrMap.get(inetAddr) == null){
					addrMap.put(inetAddr, addr.getTime());
					writer.println("Peer" + inetAddr + ": " + addr.getTime());
					writer.println("Peer" + inetAddr + ": " + addr.getTime());
				}else{
					Long time = addr.getTime();
					if (time > addrMap.get(inetAddr)){
						writer.println("Peer" + inetAddr + ": " + time);
						System.out.println(time);
					}
				}
			}
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			writer.close();
		}
	
	}

}
