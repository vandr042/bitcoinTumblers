package exchange.Parsers;

import exchange.Order;
import org.json.*;
import exchange.Trade;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;


public class OKCoinParser implements Parser{
    
    LinkedBlockingQueue<Trade> trades;
    LinkedBlockingQueue<Order> orders;
    
    public OKCoinParser(LinkedBlockingQueue<Trade> trade_queue, LinkedBlockingQueue<Order> order_queue){
        this.trades = trade_queue; //Pass reference to our main queue into varible "trades"
        this.orders = order_queue;
    }

    @Override
    public void parse(String data) throws InterruptedException, JSONException, NumberFormatException{
        InputStream stream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        JSONTokener tokener = new JSONTokener(stream);
        JSONArray array = new JSONArray(tokener);
        JSONObject root = array.getJSONObject(0);
        
        String channel = root.getString("channel").substring(3, 9);
        JSONArray trade_data = (JSONArray)root.get("data");
        JSONArray temp;
        
        for(int i = 0; i < trade_data.length(); i++){
                
                temp = (JSONArray)trade_data.get(i);
                Trade a = new Trade(temp.get(4).toString(), 
                                    channel, 
                                    channel, 
                                    "OKCoin", 
                                    Double.parseDouble(temp.get(1).toString()), 
                                    Double.parseDouble(temp.get(2).toString()), 
                                    temp.get(3).toString(), 
                                    temp.get(0).toString());
                
                trades.put(a);
            }
          System.out.println("Serializing OKCoin Trades");         
    }

    @Override
    public void parse_order(String data) throws InterruptedException, JSONException, NumberFormatException{
        InputStream stream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        JSONTokener tokener = new JSONTokener(stream);
        JSONArray root_array = new JSONArray(tokener);
        JSONObject root_object = root_array.getJSONObject(0);
        
        //Get the exchange pair and array of orders data
        String pair = root_object.getString("channel").substring(3, 9);
        JSONObject order_data = root_object.getJSONObject("data");
        
        //Split the array of orders into bids and asks
        JSONArray bids_array = order_data.getJSONArray("bids");
        JSONArray asks_array = order_data.getJSONArray("asks");
        //Get the timestamp
        String timestamp = order_data.getString("timestamp");
        
        //Create temp json-array to store current order during iteration 
        JSONArray temp;
        
        //Iterate over bids
        for (int i = 0; i < bids_array.length(); i++){
            temp = bids_array.getJSONArray(i);
            Order a = new Order("bid", pair, pair, "OKCoin", Double.parseDouble(temp.get(1).toString()), Double.parseDouble(temp.get(0).toString()), timestamp, "NULL");
            orders.put(a);
        }
        //Iterate over asks
        for (int i = 0; i < asks_array.length(); i++){
            temp = asks_array.getJSONArray(i);
            Order a = new Order("ask", pair, pair, "OKCoin", Double.parseDouble(temp.get(1).toString()), Double.parseDouble(temp.get(0).toString()), timestamp, "NULL");
            orders.put(a);
        } 
        System.out.println("Serializing OKCoin Orders");
    }
  
}