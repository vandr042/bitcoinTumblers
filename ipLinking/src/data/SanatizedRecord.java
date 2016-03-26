package data;

import java.net.InetAddress;

import org.bitcoinj.core.PeerAddress;

public class SanatizedRecord implements Comparable<SanatizedRecord>{
	
	private InetAddress myAddr;
	private int myPort;
	private long myTS;

	public SanatizedRecord(InetAddress addr, int port, long ts){
		this.myAddr = addr;
		this.myPort = port;
		this.myTS = ts;
	}
	
	public SanatizedRecord(PeerAddress tAddr){
		this(tAddr.getAddr(), tAddr.getPort(), tAddr.getTime());
	}
	
	public PeerAddress getPeerAddressObject(){
		return new PeerAddress(this.myAddr, this.myPort);
	}
	
	public long getTS(){
		return this.myTS;
	}
	
	public boolean equals(Object rhs){
		SanatizedRecord rhsRec = (SanatizedRecord)rhs;
		
		return this.myAddr.equals(rhsRec.myAddr) && this.myPort == rhsRec.myPort;
	}
	
	public String toString(){
		//TODO make this not include dns name
		return this.myAddr.toString() + ":" + this.myPort;
	}
	
	public int hashCode(){
		return this.myAddr.hashCode() + this.myPort;
	}

	public int compareTo(SanatizedRecord rhs) {
		if(this.myTS < rhs.myTS){
			return -1;
		} else if(this.myTS == rhs.myTS){
			return 0;
		} else{
			return 1;
		}
	}
	
}
