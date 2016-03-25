package valueset;

import java.util.*;

public class valueset {
    
    public static void main(String[] args){
        AccountManager manager = new AccountManager();
        manager.goToBlockchainExplorer();
        List<Integer> anonSizes = manager.runExperiment(false);
        
        int sum = 0;
        for(int tempVal: anonSizes){
        	sum += tempVal;
        }
        System.out.println((double)sum / (double)anonSizes.size());
        
    }
    
}
