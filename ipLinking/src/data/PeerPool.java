package data;

import java.util.*;

import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;

public class PeerPool {

	private HashMap<PeerAddress, Set<PeerAddress>> knownNodes;
	private HashMap<PeerAddress, Peer> nodeObjects;
	
	private HashMap<PeerAddress, Long> timeConnected;
	private HashMap<PeerAddress, Long> timeDisconnected;
	private HashMap<PeerAddress, Long> timeConnectionFailure;

	public PeerPool() {
		this.knownNodes = new HashMap<PeerAddress, Set<PeerAddress>>();
		this.nodeObjects = new HashMap<PeerAddress, Peer>();

		this.timeConnected = new HashMap<PeerAddress, Long>();
		this.timeDisconnected = new HashMap<PeerAddress, Long>();
		this.timeConnectionFailure = new HashMap<PeerAddress, Long>();
	}
	
	public synchronized Set<Peer> getActivePeers(){
		Set<Peer> retSet = new HashSet<Peer>();
		retSet.addAll(this.nodeObjects.values());
		return retSet;
	}

	public synchronized void learnedPeer(PeerAddress learnedAddress, PeerAddress learnedFrom) {
		if (!this.knownNodes.containsKey(learnedAddress)) {
			this.knownNodes.put(learnedAddress, new HashSet<PeerAddress>());
			this.timeConnected.put(learnedAddress, (long) -1);
			this.timeDisconnected.put(learnedAddress, (long) -1);
			this.timeConnectionFailure.put(learnedAddress, (long) -1);
		}
		if (learnedFrom != null) {
			this.knownNodes.get(learnedAddress).add(learnedFrom);
		}
	}

	public synchronized void signalConnected(PeerAddress connectedAddress, Peer peerObject) {
		if (!this.knownNodes.containsKey(connectedAddress)) {
			throw new RuntimeException("That address is not known!");
		}
		if (this.timeConnected.get(connectedAddress) != -1) {
			throw new RuntimeException("Already connected to that address!");
		}
		
		this.nodeObjects.put(connectedAddress, peerObject);
		this.timeConnected.put(connectedAddress, System.currentTimeMillis());
		this.timeDisconnected.put(connectedAddress, (long) -1);
		this.timeConnectionFailure.put(connectedAddress, (long) -1);
	}

	public synchronized void signalDisconnected(PeerAddress disconnectedAddress) {
		if (!this.knownNodes.containsKey(disconnectedAddress)) {
			throw new RuntimeException("That address is not known!");
		}
		if (this.timeConnected.get(disconnectedAddress) == -1) {
			throw new RuntimeException("Not connected!");
		}

		this.timeConnected.put(disconnectedAddress, (long) -1);
		this.timeDisconnected.put(disconnectedAddress, System.currentTimeMillis());
		this.knownNodes.remove(disconnectedAddress);
	}

	public synchronized void signalFailedConnect(PeerAddress failedAddress) {
		if (!this.knownNodes.containsKey(failedAddress)) {
			throw new RuntimeException("That address is not known!");
		}
		if (this.timeConnected.get(failedAddress) != -1) {
			throw new RuntimeException("Already connected to that address, why did we fail to connect!");
		}

		this.timeConnectionFailure.put(failedAddress, System.currentTimeMillis());
	}
	
	public synchronized int getKnownCount(){
		return this.knownNodes.size();
	}

	public synchronized List<PeerTimePair> getConnectionCandidates() {
		List<PeerTimePair> retList = new ArrayList<PeerTimePair>();

		for (PeerAddress tAddress : this.knownNodes.keySet()) {
			if (this.timeConnected.get(tAddress) == -1) {
				retList.add(new PeerTimePair(tAddress, this.timeConnectionFailure.get(tAddress)));
			}
		}
		Collections.sort(retList);
		
		return retList;
	}

}
