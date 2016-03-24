package valueset;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Scanner;
import java.util.Vector;
import java.util.ArrayList;


public class AccountManager {
    
    ArrayList deposits = new ArrayList();
    ArrayList withdrawls = new ArrayList();
    
    
    public AccountManager(){
        importData("balance-synth.log");
        FileOutputStream fos = null;
        String newline = "\n";
        
        try {
            fos = new FileOutputStream("anonimity_set.txt");
        } catch (FileNotFoundException e){
            e.printStackTrace();
        }
        int count = 0;
        for (Object i : withdrawls){
            String[] currentWithdrawl = (String[])i;
            try{
                fos.write(currentWithdrawl[1].getBytes());
                fos.write(newline.getBytes());
            }catch (Exception e){
                e.printStackTrace();
            }
            ArrayList temp = getAnonimitySet(currentWithdrawl);
            for (Object j : temp){
                String toWrite = "     " + j.toString();
                try{
                    fos.write(toWrite.getBytes());
                    fos.write(newline.getBytes());
                }catch (Exception e){
                    e.printStackTrace();
                }
            }   
            System.out.println(count);
            count++;
        }
        
    }
    
    void importData(String dataFileName){
        File dataFile = new File(dataFileName); 
        try{
            Scanner inputStream = new Scanner(dataFile);
            while(inputStream.hasNext()){
                String data = inputStream.next();
                String[] values = data.split(",");
                if(values[0].toLowerCase().startsWith("dep"))
                    deposits.add(values);
                else if(values[0].toLowerCase().startsWith("with"))
                    withdrawls.add(values);
            }
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }
    }
    
    ArrayList getAnonimitySet(String[] withdrawl){
        ArrayList anonimitySet = new ArrayList();
        for(int i = 0; i < deposits.size(); i++){
            String[] transaction = (String[])deposits.get(i);
            if(Integer.parseInt(transaction[2]) >= Integer.parseInt(withdrawl[2]) && Integer.parseInt(transaction[3]) < Integer.parseInt(withdrawl[3]))
                anonimitySet.add(transaction[1]);
        }
        return anonimitySet;
    }
    
}