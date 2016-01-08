package org.bitcoinj.core;

public interface AddressUser {
	
	public void getUnsolicitedAddresses(AddressMessage m, Peer rcvingPeer);
	
	public void getSolicitedAddresses(AddressMessage m, Peer rcvingPeer);

}
