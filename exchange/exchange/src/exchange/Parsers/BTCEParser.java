package exchange.Parsers;

import java.io.*;
import org.json.*;
import exchange.Trade;
import exchange.Clients.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;


public class BTCEParser implements Runnable{
    
    
    // <editor-fold defaultstate="collapsed" desc="HTTP Variables">
    private final String url = "https://btc-e.com/api/3/trades/btc_usd-btc_rur-btc_eur-ltc_btc-ltc_usd-ltc_rur-ltc_eur-nmc_btc-nmc_usd-nvc_btc-nvc_usd-usd_rur-eur_usd-eur_rur-ppc_btc-ppc_usd?limit=150";
    private final HttpClient http;
    private String hypertext = "";
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Threading Variables">
    LinkedBlockingQueue<Trade> trades;
    public Thread btce_parser;
    // </editor-fold>
    
    
    public BTCEParser(LinkedBlockingQueue<Trade> queue){
        
        //Pass reference to our main queue into varible "trades"
        this.trades = queue;
        //Instantiate the HTTPClient()
        http = new HttpClient(); 
        //Initialize Parser Thread for BTC-E
        btce_parser = new Thread(this);
        
    }
    
    @Override
    public void run() {
        
        while(Thread.currentThread() == btce_parser){
            
            try{
                hypertext = http.getHypertext(url); //Make GET Request
                parse(hypertext);
                Thread.sleep(5000); //Execute every 5 seconds
             } catch (Exception e){
                e.printStackTrace();
            }
            
        }  
        
    }
    
    
    public void parse(String hypertext) throws Exception{
        
        InputStream stream = new ByteArrayInputStream(hypertext.getBytes(StandardCharsets.UTF_8));
        JSONTokener tokener = new JSONTokener(stream);
        JSONObject root = new JSONObject(tokener);
        JSONObject temp;
        
        //Loop through all 16 Symbol Pairs
        for(int i = 0; i < root.names().length(); i++){
            //Loop through each trade of the given Symbol Pair
            for(int j = 0; j < ((JSONArray)root.get(root.names().get(i).toString())).length(); j++){
                
                //Store the current JSON trade in "temp"
                temp = ((JSONObject)(((JSONArray)root.get(root.names().get(i).toString())).get(j)));
                
                //Create a Trade object
                Trade a = new Trade(temp.get("type").toString(), 
                                    root.names().get(i).toString(), 
                                    root.names().get(i).toString(),
                                    "BTC-E",
                                    Double.parseDouble(temp.get("price").toString()),
                                    Double.parseDouble(temp.get("amount").toString()),
                                    temp.get("timestamp").toString(),
                                    temp.get("tid").toString());
                //Put Trade object inside our queue
                trades.put(a);
            }//Close Inner Loop
        }//Close Outer Loop
        
    }
    
}