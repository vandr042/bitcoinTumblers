package exchange.Parsers;

import org.json.*;
import exchange.Trade;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;


public class BitfinexParser implements Parser{
    
    LinkedBlockingQueue<Trade> trades;

    public BitfinexParser(LinkedBlockingQueue<Trade> queue){
        this.trades = queue; //Pass reference to our main queue into varible "trades"
    }

    @Override
    public void parse(String data) throws InterruptedException, JSONException, NumberFormatException{
        //If server sends a non-trade object, ignore
        if(data.startsWith("{")){
            return;
        }
        //If server sends a heartbeat, ignore
        if(data.contains("hb")){
            return;
        }
        
        //Only for the first data dump received from BITFINEX, all subsequent trades go to the "else"...
        if(data.contains(",[[")){
            InputStream stream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
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
          System.out.println("Serializing BITFINEX");
        }
        else{
            System.out.println("BITFINEX");
            InputStream stream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
            JSONTokener tokener = new JSONTokener(stream);
            JSONArray temp = new JSONArray(tokener);
            
            //Create Trade Object
            if(temp.get(4).toString().startsWith("-")){
                Trade a = new Trade("Sell", 
                                    temp.get(1).toString().substring(temp.get(1).toString().length()-6, temp.get(1).toString().length()), 
                                    temp.get(1).toString().substring(temp.get(1).toString().length()-6, temp.get(1).toString().length()),
                                    "BITFINEX",
                                    Double.parseDouble(temp.get(3).toString()),
                                    Double.parseDouble(temp.get(4).toString().replace("-", "")),
                                    temp.get(2).toString(),
                                    temp.get(1).toString());
                //Put Trade object inside our queue
                trades.put(a);
            }
            else{
                Trade a = new Trade("Buy", 
                                    temp.get(1).toString().substring(temp.get(1).toString().length()-6, temp.get(1).toString().length()), 
                                    temp.get(1).toString().substring(temp.get(1).toString().length()-6, temp.get(1).toString().length()),
                                    "BITFINEX",
                                    Double.parseDouble(temp.get(3).toString()),
                                    Double.parseDouble(temp.get(4).toString().replace("-", "")),
                                    temp.get(2).toString(),
                                    temp.get(1).toString());
                //Put Trade object inside our queue
                trades.put(a);
            }
          System.out.println("Serializing BITFINEX");  
        }//Close Else     
    }

    @Override
    public void parse_order(String data) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  
}