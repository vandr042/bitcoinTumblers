package slurpdetector;

public class SlurpDetector {
    
    public static void main(String[] args) {
        Detective sherlock = new Detective();
        sherlock.importData("data/iplinks.csv");
        sherlock.invertData();
        //Max's Bitcoin Slurp Tx
        String tempTx = "68d2a1cd56e7f2ac1a7d14069650bb8bf786c30b94fe2e981b838c82f25b280a";//"5756ff16e2b9f881cd15b8a7e478b4899965f87f553b6210d0f8e5bf5be7df1d";
        sherlock.callBlockExplorer(tempTx);
    }

}
