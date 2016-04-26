package org.bitcoinj.core;

import java.util.List;

public interface AddressUser {
	
	public void getAddresses(AddressMessage m, Peer rcvingPeer);
	
	public void getInventory(List<InventoryItem> incTx, Peer rcvingPeer);

}
