package bitcoinLink;

import java.io.PrintStream;
import java.util.*;
import org.bitcoinj.core.*;
import java.net.InetAddress;
import java.util.concurrent.ExecutionException;

public class PeerHelper implements Runnable {

	private HashMap<PeerAddress, Long> nodesLastSeenRemotely;
	private HashSet<PeerAddress> observedConnections;

	private long guessAtTimeStampDelta;

	private Peer myPeer;
	private volatile boolean alive;

	private PrintStream writer = null;

	private static final Long INTER_HARVEST_TIME = (long) 10000;

	public PeerHelper(Peer peer, PrintStream outWriter) {
		this.nodesLastSeenRemotely = new HashMap<PeerAddress, Long>();
		this.observedConnections = new HashSet<PeerAddress>();
		if(peer == null){
			System.out.println("DAAAAAAAAAAAAAAAAA FUCK");
		}
		this.guessAtTimeStampDelta = peer.getPeerVersionMessage().time - (System.currentTimeMillis() / 1000);
		System.out.println("guessed ts delta for " + peer.getAddress() + " is " + this.guessAtTimeStampDelta);

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
				List<PeerAddress> addresses = message.getAddresses();

				/*
				 * Ok, now actually update our storage device
				 */
				synchronized (this) {
					for (PeerAddress addr : addresses) {
						long remoteTS = addr.getTime();
						if (!nodesLastSeenRemotely.containsKey(addr)) {
							synchronized (writer) {
								writer.println("NEW Peer" + addr + ": " + addr.getTime() + " learned from "
										+ this.myPeer.getAddress());
							}
						} else {
							if (remoteTS > nodesLastSeenRemotely.get(addr)) {
								System.out.println("found updated peer");
								synchronized (writer) {
									writer.println("LOGON Peer" + addr + ": " + System.currentTimeMillis() / 1000
											+ " observed at " + this.myPeer.getAddress());
								}
								this.observedConnections.add(addr);
							}
						}

						this.nodesLastSeenRemotely.put(addr, remoteTS);
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

	public Set<PeerAddress> getNodesActiveWithin(long timeWindowInSeconds) {
		HashSet<PeerAddress> retSet = new HashSet<PeerAddress>();

		synchronized (this) {
			/*
			 * Build the time horizon, which is our current time, skewed by the
			 * peer's clock view (which gets us what the peer thinks now is) and
			 * then the back of the window
			 */
			long timeHorizon = System.currentTimeMillis() / 1000 + this.guessAtTimeStampDelta - timeWindowInSeconds;

			/*
			 * Look through their list of peers/last connected to mappings,
			 * return all nodes which fall inside the time horizon
			 */
			for (PeerAddress tAddr : this.nodesLastSeenRemotely.keySet()) {
				if (this.nodesLastSeenRemotely.get(tAddr) >= timeHorizon) {
					retSet.add(tAddr);
				}
			}
		}
		return retSet;
	}

	public Set<PeerAddress> getConnectedNodes() {
		/*
		 * Save the current set of observed new connections and create an empty
		 * set for the next round
		 */
		Set<PeerAddress> outSet = null;
		synchronized (this) {
			outSet = this.observedConnections;
			this.observedConnections = new HashSet<PeerAddress>();
		}

		return outSet;
	}

}
