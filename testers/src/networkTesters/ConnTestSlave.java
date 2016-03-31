package networkTesters;

import java.net.SocketAddress;

import org.bitcoinj.core.Peer;

import com.google.common.util.concurrent.FutureCallback;

public class ConnTestSlave implements FutureCallback<SocketAddress> {

	private Peer myPeer;
	private NetworkConnector myParent;


	public ConnTestSlave(Peer peerObj, NetworkConnector parent) {
		this.myPeer = peerObj;
		this.myParent = parent;
	}

	public void onFailure(Throwable t) {
		this.myParent.reportFail();
	}

	public void onSuccess(SocketAddress sockAddr) {
		this.myParent.reportWorking(this.myPeer);
	}

}
