
import java.io.PrintStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.PeerGroup;

public class TestConnThread implements Runnable {

	private PeerFinder parent;
	private PeerGroup pg;
	private PrintStream testConnLog;

	public TestConnThread(PeerFinder parent, PeerGroup peerGroup, PrintStream log) {
		this.parent = parent;
		this.pg = peerGroup;
		this.testConnLog = log;
	}

	public void run() {
		try {
			/*
			 * Simple slave loop which gets an address to connect to, tries, and
			 * tells the parent if it worked or not
			 */
			while (true) {
				/*
				 * Step one, block till we have work to do
				 */
				PeerAddress testAddr = this.parent.getAddressToTest();
				System.out.println("I GET TO TEST " + testAddr);

				/*
				 * step two, try to start the connection
				 */
				Peer testPeer = pg.connectTo(testAddr.getSocketAddress());
				if (testPeer != null) {
					
					try {
						testPeer.getVersionHandshakeFuture().get(10, TimeUnit.SECONDS);
						this.parent.reportConnectionSuccess(testAddr, testPeer);
						synchronized (this.testConnLog) {
							this.testConnLog.println("Added working: " + testAddr);
						}
					} catch (ExecutionException e) {
						System.out.println("DERP");
						this.parent.reportConnectionFailure(testAddr);
					} catch (TimeoutException e) {
						System.out.println("TIMOEUT");
						this.parent.reportConnectionFailure(testAddr);
					}
				} else {
					this.parent.reportConnectionFailure(testAddr);
					synchronized (this.testConnLog) {
						this.testConnLog.println("Null in connection: " + testAddr);
					}
				}

				synchronized (this.testConnLog) {
					this.testConnLog.flush();
				}
				
				System.out.println("DONE TESTING!");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("HOLY SHIT I'M LOST");
	}
}
