package listeners;

import java.net.SocketAddress;

import org.bitcoinj.core.Peer;

import com.google.common.util.concurrent.FutureCallback;

import control.ConnectionTester;

public class ConnTestSlave implements FutureCallback<SocketAddress> {

	private Peer myPeer;
	private ConnectionTester myParent;


	public ConnTestSlave(Peer peerObj, ConnectionTester parent) {
		this.myPeer = peerObj;
		this.myParent = parent;
	}

	public void onFailure(Throwable t) {
		this.myParent.reportTCPFailure(this.myPeer, t.getMessage());
	}

	public void onSuccess(SocketAddress sockAddr) {
		this.myParent.reportWorkingPeer(this.myPeer);
	}

}
