package valueset;

import java.util.*;

public class valueset {
    
    public static void main(String[] args){
        //AccountManager manager = new AccountManager("../miscScripts/balance-synth.log");
    	AccountManager manager = new AccountManager("../miscScripts/balance-synth-small.log");
        //manager.generateAliases();
        List<Integer> anonSizes = manager.runExperiment(true);
        
        int sum = 0;
        for(int tempVal: anonSizes){
        	sum += tempVal;
        }
        System.out.println((double)sum / (double)anonSizes.size());
        
    }
    
}