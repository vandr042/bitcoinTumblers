package control;

import java.util.*;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;

public class AddressHarvest implements Runnable {

	private Manager myParent;

	private static final long INTER_HARVEST_TIME_SEC = 30;

	public AddressHarvest(Manager parent) {
		this.myParent = parent;
	}

	@Override
	public void run() {
		while (true) {
			int unsolCount = 0;
			int solCount = 0;

			Set<Peer> activePeers = this.myParent.getPeerPool().getActivePeers();
			for (Peer tPeer : activePeers) {
				PeerAddress tAddrLearnedFrom = tPeer.getAddress();
				Set<PeerAddress> unsolPeers = tPeer.getUncolicitedAddrs();
				unsolCount += unsolPeers.size();
				for (PeerAddress tLearnedAdedress : unsolPeers) {
					this.myParent.getPeerPool().learnedPeer(tLearnedAdedress, tAddrLearnedFrom);
				}
				Set<PeerAddress> solPeers = tPeer.getSolcitedAddrs();
				solCount += solPeers.size();
				for (PeerAddress tLearnedAddress : solPeers) {
					this.myParent.getPeerPool().learnedPeer(tLearnedAddress, tAddrLearnedFrom);
				}
			}

			this.myParent.logEvent("Harvested total solicited addrs " + solCount + " and unsolicited " + unsolCount);

			activePeers = this.myParent.getPeerPool().getActivePeers();
			for (Peer tPeer : activePeers) {
				tPeer.getAddr();
			}

			try {
				Thread.sleep(AddressHarvest.INTER_HARVEST_TIME_SEC * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
