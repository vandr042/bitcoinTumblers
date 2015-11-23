package exchange;

import java.io.*;
import java.util.Date;
import java.time.Instant;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.LinkedBlockingQueue;


public class Serializer implements Runnable{
    
    private LinkedBlockingQueue<Trade> trades;
    Thread trade_serializer, order_serializer;
    private String CSVHead = "Id,Provenance,Exchange,Symbol_Pair,Type,Price,Volume,Timestamp";

    // <editor-fold defaultstate="collapsed" desc="Date Variables">
    Date today;
    DateFormat dateformat;
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="File Variables">
    File currentFile;
    FileOutputStream fos;
    // </editor-fold>
    
    
    public Serializer(LinkedBlockingQueue<Trade> queue){
        
        //Pass reference to our main queue into varible "trades"
        this.trades = queue;
        
        //Initialize Date Format for file nomenclature
        dateformat = new SimpleDateFormat("MMM-dd-yyyy");
        
        //Set a bogus date initially. 
        //This is modified when the thread starts, and a bogus value is needed to make sure trades are recorded for the first day.
        today = Date.from(Instant.EPOCH);
        
        trade_serializer = new Thread(this);
        
    }
    
    @Override
    public void run() {
        
        while(trade_serializer == Thread.currentThread()){
            
            SerializeTrades();
            try{
                Thread.sleep(1);
            }
            catch (InterruptedException e){
                e.printStackTrace();
            } 
            
        }
         
    }
    
    
    public void SerializeOrders(){
        //not implemented yet
    }
    
    public void SerializeTrades(){
        
        //Check if it is still today
        if(dateformat.format(Date.from(Instant.now())).equals(dateformat.format(today))){
            
            try{
                //Write the next Trade in the queue
                fos.write(trades.take().getCSV().getBytes());
                fos.write('\n');
            }
            catch(InterruptedException | IOException e){
                e.printStackTrace();
            }
            
        }
        
        //If the next day has arrived, lets create a new file.
        //NOTE: When the program starts, this will always be executed first, since we set "today" to initially be a bogus date.
        else{
            
            //First we'll update our variable "today" to the actual current date
            today = Date.from(Instant.now());
            System.out.println("Making new file!");
            //Then we'll create a new file
            currentFile = new File("Database/Trades/"+dateformat.format(today)+".csv");
            
            if(!currentFile.exists()){
                try{
                    currentFile.createNewFile();
                    fos = new FileOutputStream(currentFile, true);
                    //We need to write the CSV header to the file
                    fos.write(CSVHead.getBytes());
                    fos.write('\n');
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
    
            //This enables us to have a FileOutputStream if we are not opening a new file, but rather reconnecting to an old file to append to it,
            //in the case that the computer unexpectedly shuts down and we need to restart the program.
            else if(currentFile.exists()){
                try{
                    fos = new FileOutputStream(currentFile, true);
                }
                catch(IOException e){
                    e.printStackTrace();
                }
            }
            
            //Now that we've created a new file with CSV header, 
            //the next time around, we can start adding Trades to it.
        }
    }
 
}
