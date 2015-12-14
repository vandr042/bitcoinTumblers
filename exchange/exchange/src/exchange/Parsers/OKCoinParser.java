package exchange.Parsers;

import org.json.*;
import exchange.Trade;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;


public class OKCoinParser implements Parser{
    
    LinkedBlockingQueue<Trade> trades;

    public OKCoinParser(LinkedBlockingQueue<Trade> queue){
        this.trades = queue; //Pass reference to our main queue into varible "trades"
    }

    @Override
    public void parse(String data) throws InterruptedException, JSONException, NumberFormatException{
        InputStream stream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        JSONTokener tokener = new JSONTokener(stream);
        JSONArray array = new JSONArray(tokener);
        JSONObject root = array.getJSONObject(0);
        
        String channel = root.get("channel").toString();
        JSONArray trade_data = (JSONArray)root.get("data");
        JSONArray temp;
        
        if(channel.equals("ok_btcusd_trades_v1")){
            
            for(int i = 0; i < trade_data.length(); i++){
                
                temp = (JSONArray)trade_data.get(i);
                Trade a = new Trade(temp.get(4).toString(), 
                                    "BTCUSD", 
                                    "BTCUSD", 
                                    "OKCoin", 
                                    Double.parseDouble(temp.get(1).toString()), 
                                    Double.parseDouble(temp.get(2).toString()), 
                                    temp.get(3).toString(), 
                                    temp.get(0).toString());
                
                trades.put(a);
            }
          System.out.println("Serializing OKCoin - 'BTCUSD'");
        }
        else if(channel.equals("ok_ltcusd_trades_v1")){
            
            for(int i = 0; i < trade_data.length(); i++){
                
                temp = (JSONArray)trade_data.get(i);
                Trade a = new Trade(temp.get(4).toString(), 
                                    "LTCUSD", 
                                    "LTCUSD", 
                                    "OKCoin", 
                                    Double.parseDouble(temp.get(1).toString()), 
                                    Double.parseDouble(temp.get(2).toString()), 
                                    temp.get(3).toString(), 
                                    temp.get(0).toString());
                
                trades.put(a);
            } 
          System.out.println("Serializing OKCoin - 'LTCUSD'");  
        }
         
    }

    @Override
    public void parse_order(String data) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  
}