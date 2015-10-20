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

	HashMap<InetAddress, Long> addrMap;
	Peer aPeer;
	File file1 = new File("connections.txt");
	PrintStream writer = null;
	static PrintWriter writer1;
	static AddressMessage message;

	public PeerHelper(Peer peer, HashMap<InetAddress, Long> hm, PrintStream outWriter) throws FileNotFoundException {
		addrMap = hm;
		aPeer = peer;
		this.writer = outWriter;
	}

	public static void main(String[] args) throws InterruptedException, ExecutionException {
	}

	@Override
	public void run() {
		
		System.out.println("starting on " + this.aPeer);

		/*
		 * keep asking peers for addresses until main thread in peerFinder
		 * terminates
		 */
		while (true) {
			/* not sure what to do with an exception */
			try {
				message = aPeer.getAddr().get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}

			/*
			 * get inetAdress and check if in map, if not add else update if
			 * time updated
			 */
			List<PeerAddress> addresses = message.getAddresses();
			for (PeerAddress addr : addresses) {
				InetAddress inetAddr = addr.getAddr();
				if (!addrMap.containsKey(inetAddr)) {
					addrMap.put(inetAddr, addr.getTime());
					synchronized (writer) {
						writer.println("NEW Peer" + inetAddr + ": " + addr.getTime());
					}
				} else {
					Long time = addr.getTime();
					if (time > addrMap.get(inetAddr)) {
						synchronized (writer) {
							writer.println("UPDATED Peer" + inetAddr + ": " + time);
						}
						System.out.println(time);
					}
				}
			}
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			synchronized (writer) {
				writer.flush();
			}
		}

	}

}
