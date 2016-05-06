package slurpdetector;


public class IPLink {
    
    private final String txId;
    private final String ipAddr;
    private final String ipBlock;
    
    public IPLink(String txId, String ipAddr, String ipBlock){
        this.txId = txId;
        this.ipAddr = ipAddr;
        this.ipBlock = ipBlock;
    }
    
    public IPLink(String txId, String ipAddr){
        this.txId = txId;
        this.ipAddr = ipAddr;
        this.ipBlock = null;
    }
    
    public String getTxID(){
        return this.txId;
    }
    
    public String getIP(){
        return this.ipAddr;
    }
    
    public String getIPBlock(){
        return this.ipBlock;
    }
    
    @Override
    public String toString(){
        return "TXID:" + txId + " IP:" + ipAddr + " IP Block: " + ipBlock;
    }
    
}