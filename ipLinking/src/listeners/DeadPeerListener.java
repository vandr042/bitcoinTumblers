package listeners;

import java.util.Set;

import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.listeners.PeerConnectionEventListener;

import control.Manager;

public class DeadPeerListener implements PeerConnectionEventListener {

	private Manager myParent;
	
	public DeadPeerListener(Manager parent){
		this.myParent = parent;
	}
	
	@Override
	public void onPeerConnected(Peer arg0, int arg1) {
		return;
	}

	@Override
	public void onPeerDisconnected(Peer dcPeer, int arg1) {
		this.myParent.getRecord(dcPeer.getAddress()).signalDisconnected();
		this.myParent.cleanupDeadPeer(dcPeer);
		this.myParent.logEvent("D/C peer " + dcPeer.getAddress(), Manager.CRIT_LOG_LEVEL);
	}

	@Override
	public void onPeersDiscovered(Set<PeerAddress> arg0) {
		return;
	}

}
