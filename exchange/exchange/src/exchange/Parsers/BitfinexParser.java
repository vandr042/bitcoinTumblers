package exchange.Parsers;

import java.io.*;
import org.json.*;
import exchange.Trade;
import exchange.Clients.websocket.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import javax.net.ssl.SSLException;


public class BitfinexParser{
    
    
    // <editor-fold defaultstate="collapsed" desc="WebSockets Variables">
    private final String url = "wss://api2.bitfinex.com:3000/ws";
    private WebSocketClient websocket = null;
    private String hypertext = "[6,[[\"425170-BTCUSD\",1449210983,358.15,-0.01],[\"425169-BTCUSD\",1449210952,358.15,-0.01],[\"425168-BTCUSD\",1449210906,358.11,-4],[\"425167-BTCUSD\",1449210887,358.37,1.68],[\"425166-BTCUSD\",1449210843,358.18,1.68943461],[\"425165-BTCUSD\",1449210803,358.2,1.5],[\"425164-BTCUSD\",1449210803,358.14,0.5],[\"425163-BTCUSD\",1449210787,358.1,-0.02579],[\"425162-BTCUSD\",1449210785,358.1,-0.04808815],[\"425161-BTCUSD\",1449210784,358.15,0.08801785],[\"425160-BTCUSD\",1449210774,358.1,-0.075528],[\"425159-BTCUSD\",1449210706,358.11,-6.5],[\"425158-BTCUSD\",1449210667,358.28,0.453],[\"425157-BTCUSD\",1449210667,358.25,0.547],[\"425156-BTCUSD\",1449210555,358.12,-0.85],[\"425155-BTCUSD\",1449210552,358.12,0.546],[\"425154-BTCUSD\",1449210424,358.14,0.923834],[\"425153-BTCUSD\",1449210424,358.14,0.076166],[\"425152-BTCUSD\",1449210347,358.11,-0.5],[\"425151-BTCUSD\",1449210329,358.18,0.01],[\"425150-BTCUSD\",1449210289,358.26,0.01],[\"425149-BTCUSD\",1449210229,358.11,-0.01],[\"425148-BTCUSD\",1449210227,358.11,-0.01],[\"425147-BTCUSD\",1449210225,358.11,-0.01],[\"425146-BTCUSD\",1449210224,358.11,-0.01]]]";
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Threading Variables">
    LinkedBlockingQueue<Trade> trades;
    // </editor-fold>
    
    
    public BitfinexParser(LinkedBlockingQueue<Trade> queue){
        
        //Pass reference to our main queue into varible "trades"
        this.trades = queue;
        
        try{
            parse(hypertext);
        }catch(Exception e){
            e.printStackTrace();
        }
        
        //Instantiate the WebSocketClient()
        /*try{
            websocket = new WebSocketClient(url); 
            websocket.connect();
            websocket.send("{\"event\":\"subscribe\", \"channel\":\"trades\", \"pair\":[\"btcusd\"]}"); //examples at http://docs.bitfinex.com/?javascript#order-books
            Thread.sleep(1000);
            //websocket.shutdown();
        }
        catch (SSLException | URISyntaxException | InterruptedException e){
            e.printStackTrace();
        }*/
        
    }
    
    
    public void parse(String hypertext) throws Exception{
        
        InputStream stream = new ByteArrayInputStream(hypertext.getBytes(StandardCharsets.UTF_8));
        JSONTokener tokener = new JSONTokener(stream);
        JSONArray array = new JSONArray(tokener);
            array = (JSONArray)array.get(1);
        JSONArray temp;
        
        //Loop through each trade in array
        for(int i = 0; i < array.length(); i++){
            temp = (JSONArray)array.get(i);
            
            //Create Trade Object
            if(temp.get(3).toString().startsWith("-")){
                Trade a = new Trade("Sell", 
                                    temp.get(0).toString().substring(temp.get(0).toString().length()-6, temp.get(0).toString().length()), 
                                    temp.get(0).toString().substring(temp.get(0).toString().length()-6, temp.get(0).toString().length()),
                                    "BITFINEX",
                                    Double.parseDouble(temp.get(2).toString()),
                                    Double.parseDouble(temp.get(3).toString().replace("-", "")),
                                    temp.get(1).toString(),
                                    temp.get(0).toString());
                //Put Trade object inside our queue
                trades.put(a);
            }
            else{
                Trade a = new Trade("Buy", 
                                    temp.get(0).toString().substring(temp.get(0).toString().length()-6, temp.get(0).toString().length()), 
                                    temp.get(0).toString().substring(temp.get(0).toString().length()-6, temp.get(0).toString().length()),
                                    "BITFINEX",
                                    Double.parseDouble(temp.get(2).toString()),
                                    Double.parseDouble(temp.get(3).toString().replace("-", "")),
                                    temp.get(1).toString(),
                                    temp.get(0).toString());
                //Put Trade object inside our queue
                trades.put(a);
            }
        }//Close  Loop
        
    }
    
}