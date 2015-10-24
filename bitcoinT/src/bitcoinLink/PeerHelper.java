package bitcoinLink;

import java.io.PrintStream;
import java.util.*;
import org.bitcoinj.core.*;
import java.net.InetAddress;
import java.util.concurrent.ExecutionException;

public class PeerHelper implements Runnable {

	private HashMap<InetAddress, Long> nodesLastSeenRemotely;
	private HashMap<InetAddress, Long> nodesWeSeeActive;

	private long guessAtTimeStampDelta;

	private Peer myPeer;
	private volatile boolean alive;

	private PrintStream writer = null;

	private static final Long INTER_HARVEST_TIME = (long) 10000;

	public PeerHelper(Peer peer, PrintStream outWriter) {
		this.nodesLastSeenRemotely = new HashMap<InetAddress, Long>();
		this.nodesWeSeeActive = new HashMap<InetAddress, Long>();
		this.guessAtTimeStampDelta = Long.MIN_VALUE;

		this.myPeer = peer;
		this.alive = false;
		this.writer = outWriter;
	}

	@Override
	public void run() {

		System.out.println("starting on " + this.myPeer);
		this.alive = true;

		/*
		 * keep asking peers for addresses until we have an issue doing such
		 */
		try {
			while (true) {

				/*
				 * Send getAddr request, block until it's done
				 */
				AddressMessage message = myPeer.getAddr().get();
				long addressMessageArrival = System.currentTimeMillis() / 1000;
				List<PeerAddress> addresses = message.getAddresses();
				
				/*
				 * Ok, now actually update our storage device
				 */
				synchronized (this) {
					for (PeerAddress addr : addresses) {
						InetAddress inetAddr = addr.getAddr();
						long remoteTS = addr.getTime();
						if (!nodesLastSeenRemotely.containsKey(inetAddr)) {
							synchronized (writer) {
								writer.println("NEW Peer" + inetAddr + ": " + addr.getTime());
							}
						} else {
							if (remoteTS > nodesLastSeenRemotely.get(inetAddr)) {
								synchronized (writer) {
									writer.println("UPDATED Peer" + inetAddr + ": " + remoteTS);
								}
								this.nodesWeSeeActive.put(inetAddr, addressMessageArrival);
							}
						}

						/*
						 * Do some checks to see if we can update our guess at
						 * the time stamp skew between us and the remote node
						 */
						long delta = remoteTS - addressMessageArrival;
						if (delta > this.guessAtTimeStampDelta) {
							this.guessAtTimeStampDelta = delta;
						}

						this.nodesLastSeenRemotely.put(inetAddr, remoteTS);
					}
				}

				synchronized (writer) {
					writer.flush();
				}

				/*
				 * In the words of the Sage Samuel L Jackson, go the fuck to
				 * sleep
				 */
				Thread.sleep(PeerHelper.INTER_HARVEST_TIME);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		this.alive = false;
	}

	public boolean isAlive() {
		return this.alive;
	}

	public Set<InetAddress> getNodesActiveWithin(long timeWindowInSeconds) {
		HashSet<InetAddress> retSet = new HashSet<InetAddress>();

		synchronized (this) {
			long currentTime = System.currentTimeMillis() / 1000;

			for (InetAddress tAddr : this.nodesWeSeeActive.keySet()) {
				if (currentTime - this.nodesWeSeeActive.get(tAddr) <= timeWindowInSeconds) {
					retSet.add(tAddr);
				}
			}
		}

		return retSet;
	}

}
