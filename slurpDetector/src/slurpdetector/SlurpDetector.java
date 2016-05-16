package slurpdetector;

public class SlurpDetector {
    
    public static void main(String[] args) {
        Detective sherlock = new Detective();
        sherlock.importData("data/iplinks.csv"); //imports Connor's data into a list of IPLink objects
        sherlock.invertData(); //inverts the TxId, and IP from Connor's data
        
        //sherlock.startAnalyzing();
        
        String tempTx = "68d2a1cd56e7f2ac1a7d14069650bb8bf786c30b94fe2e981b838c82f25b280a"; //Max's Bitcoin Slurp Tx
        sherlock.callBlockExplorer(tempTx);
        
        //sherlock.matchIPAddresses();
    }

}
