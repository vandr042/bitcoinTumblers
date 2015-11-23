import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.bitcoinj.core.Context;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.VersionMessage;
import org.bitcoinj.net.NioClientManager;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.MainNetParams;

public class MaxTest {

	public static void main(String[] args) throws Exception {

		/*
		 * Build the params and context objects which are needed by other data
		 * structures.
		 */
		NetworkParameters params = MainNetParams.get();
		@SuppressWarnings("unused")
		Context tempContext = new Context(params);
		
		/*
		 * Build a DNS discovery engine and harvest a set of socket addresses
		 */
		DnsDiscovery dnsDisc = new DnsDiscovery(params);
		InetSocketAddress[] dnsSeeds = dnsDisc.getPeers(0, 30, TimeUnit.SECONDS);
		PeerAddress[] initPeerAddresses = new PeerAddress[dnsSeeds.length];
		for(int pos = 0; pos < dnsSeeds.length; pos++){
			initPeerAddresses[pos] = new PeerAddress(params, dnsSeeds[pos]);
		}
		
		System.out.println("DNS discovery found " + initPeerAddresses.length + " hosts");
		
		/*
		 * We'll need a version message to send to folks
		 */
		VersionMessage myVMsg = new VersionMessage(params, 0);
		Peer.lobotomizeMe();
		Peer testPeer = new Peer(params, myVMsg, initPeerAddresses[0], null, false);
		NioClientManager nioManage = new NioClientManager();
		System.out.println("starting nio");
		nioManage.startAsync();
		nioManage.awaitRunning();
		System.out.println("done with nio");
		
		System.out.println(testPeer);
		nioManage.openConnection(testPeer.getAddress().getSocketAddress(), testPeer).get();
		System.out.println("Conn open?");
		Thread.sleep(120000);
		
	}

}
