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
	
	public void updateTS(long newTS){
		this.myTS = newTS;
	}
	
	public boolean equals(Object rhs){
		SanatizedRecord rhsRec = (SanatizedRecord)rhs;
		
		return this.myAddr.equals(rhsRec.myAddr) && this.myPort == rhsRec.myPort;
	}
	
	public SanatizedRecord clone(){
		return new SanatizedRecord(this.myAddr, this.myPort, this.myTS);
	}
	
	public String toString(){
		//TODO make this not include dns name
		return this.myAddr.getHostAddress() + ":" + this.myPort;
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
	
	public static void main(String[] args) throws Exception{
		SanatizedRecord a = new SanatizedRecord(InetAddress.getByName("1.2.3.4"), 8333, 0);
		SanatizedRecord b = new SanatizedRecord(InetAddress.getByName("1.2.3.4"), 8333, 10);
		System.out.println(a.equals(b));
		System.out.println(a.hashCode());
		System.out.println(b.hashCode());
	}
	
}
