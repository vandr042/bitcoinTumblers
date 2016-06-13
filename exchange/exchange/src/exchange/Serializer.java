package exchange;

import java.io.*;
import java.util.Date;
import java.time.Instant;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.LinkedBlockingQueue;


public class Serializer implements Runnable{
    
    private LinkedBlockingQueue<Trade> trades; //Handle to the queues created in Main class
    private LinkedBlockingQueue<Order> orders;
    Thread trade_serializer, order_serializer;
    private final String CSVHeadTrade = "Id,Provenance,Exchange,Symbol_Pair,Type,Price,Volume,Timestamp";
    private final String CSVHeadOrder = "Provenance,Exchange,Symbol_Pair,Type,Price,Volume,First Seen,Last Seen";

    // <editor-fold defaultstate="collapsed" desc="Date Variables">
    Date today; //Used for file naming conventions
    DateFormat dateformat;
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="File Variables">
    File currentTradeFile, currentOrderFile;
    FileOutputStream fos_trade, fos_order;
    // </editor-fold>
    
    
    public Serializer(LinkedBlockingQueue<Trade> queue_trades, LinkedBlockingQueue<Order> queue_order){
        
        //Pass reference to our main queue into varible "trades" and "orders"
        this.trades = queue_trades;
        this.orders = queue_order;
        
        //Initialize Date Format for file nomenclature
        dateformat = new SimpleDateFormat("MMM-dd-yyyy");
        
        //This is modified when the thread starts, and a bogus value is needed to make sure trades and orders are recorded for the first day.
        today = Date.from(Instant.EPOCH); //Set a bogus date initially, so that it has to create a new file.
        
        trade_serializer = new Thread(this);
        order_serializer = new Thread(this);
        
    }
    
    @Override
    public void run() {
        while(trade_serializer == Thread.currentThread()){
            SerializeTrades();
        } 
        while(order_serializer == Thread.currentThread()){
            SerializeOrders();
        } 
    }
    
    
    public void SerializeOrders(){
        //Check if it is still today
        if(dateformat.format(Date.from(Instant.now())).equals(dateformat.format(today))){
            
            try{
                //Write the next Order in the queue
                fos_order.write(orders.take().getCSV().getBytes());
                fos_order.write('\n');
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
            System.out.println("Making new Order file!");
            //Then we'll create a new file
            currentOrderFile = new File("Database/Orders/"+dateformat.format(today)+".csv");
            
            if(!currentOrderFile.exists()){
                try{
                    currentOrderFile.createNewFile();
                    fos_order = new FileOutputStream(currentOrderFile, true);
                    //We need to write the CSV header to the file intially
                    fos_order.write(CSVHeadOrder.getBytes());
                    fos_order.write('\n');
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
    
            //This enables us to have a FileOutputStream if we are not opening a new file, but rather reconnecting to an old file to append to it,
            //in the case that the computer unexpectedly shuts down and we need to restart the program.
            else if(currentOrderFile.exists()){
                try{
                    fos_order = new FileOutputStream(currentOrderFile, true);
                }
                catch(IOException e){
                    e.printStackTrace();
                }
            }
            
            //Now that we've created a new file with CSV header, the next time around, we can start adding Orders to it.
        }
    }
    
    public void SerializeTrades(){
        
        //Check if it is still today
        if(dateformat.format(Date.from(Instant.now())).equals(dateformat.format(today))){
            
            try{
                //Write the next Trade in the queue
                fos_trade.write(trades.take().getCSV().getBytes());
                fos_trade.write('\n');
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
            System.out.println("Making new Trade file!");
            //Then we'll create a new file
            currentTradeFile = new File("Database/Trades/"+dateformat.format(today)+".csv");
            
            if(!currentTradeFile.exists()){
                try{
                    currentTradeFile.createNewFile();
                    fos_trade = new FileOutputStream(currentTradeFile, true);
                    //We need to write the CSV header to the file initially
                    fos_trade.write(CSVHeadTrade.getBytes());
                    fos_trade.write('\n');
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
    
            //This enables us to have a FileOutputStream if we are not opening a new file, but rather reconnecting to an old file to append to it,
            //in the case that the computer unexpectedly shuts down and we need to restart the program.
            else if(currentTradeFile.exists()){
                try{
                    fos_trade = new FileOutputStream(currentTradeFile, true);
                }
                catch(IOException e){
                    e.printStackTrace();
                }
            }
            
            //Now that we've created a new file with CSV header, the next time around, we can start adding Trades to it.
        }
    }
 
}
