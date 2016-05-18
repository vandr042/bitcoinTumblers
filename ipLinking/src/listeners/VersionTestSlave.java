package listeners;

import org.bitcoinj.core.Peer;

import com.google.common.util.concurrent.FutureCallback;

import control.ConnectionTester;

public class VersionTestSlave implements FutureCallback<Peer> {
	
	private ConnectionTester myParent;
	private Peer myPeer;

	public VersionTestSlave(ConnectionTester parent, Peer thePeer){
		this.myParent = parent;
		this.myPeer = thePeer;
	}
	
	public void onFailure(Throwable t) {
		this.myParent.reportNoVersionPeer(myPeer, t.getMessage());
	}

	public void onSuccess(Peer peerWorking) {
		this.myParent.reportWorkingPeer(peerWorking);
	}
}
