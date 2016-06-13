package exchange.Parsers;

import exchange.Order;
import org.json.*;
import exchange.Trade;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;


public class BTCEParser implements Parser{
    
    LinkedBlockingQueue<Trade> trades;
    LinkedBlockingQueue<Order> orders;
    
    //Used to elimate redundant trades
    Map allTimeHighest = new HashMap();
    Map currentHighest = new HashMap();
    String[] symbpairs = {"btc_usd", "btc_rur", "btc_eur", "ltc_btc", "ltc_usd", "ltc_rur", "ltc_eur", "nmc_btc", "nmc_usd", "nvc_btc", "nvc_usd", "usd_rur", "eur_usd", "eur_rur", "ppc_btc", "ppc_usd"};

    
    public BTCEParser(LinkedBlockingQueue<Trade> queue_trades, LinkedBlockingQueue<Order> queue_orders){
        this.trades = queue_trades; //Pass reference to our main queue into varible "trades"
        this.orders = queue_orders;
        
        //Sets the initial highest trade ID to zero, since we haven't got any trades yet
        allTimeHighest.put("btc_usd", 0);
        allTimeHighest.put("btc_rur", 0);
        allTimeHighest.put("btc_eur", 0);
        allTimeHighest.put("ltc_btc", 0);
        allTimeHighest.put("ltc_usd", 0);
        allTimeHighest.put("ltc_rur", 0);
        allTimeHighest.put("ltc_eur", 0);
        allTimeHighest.put("nmc_btc", 0);
        allTimeHighest.put("nmc_usd", 0);
        allTimeHighest.put("nvc_btc", 0);
        allTimeHighest.put("nvc_usd", 0);
        allTimeHighest.put("usd_rur", 0);
        allTimeHighest.put("eur_usd", 0);
        allTimeHighest.put("eur_rur", 0);
        allTimeHighest.put("ppc_btc", 0);
        allTimeHighest.put("ppc_usd", 0);
        
        currentHighest.put("btc_usd", 0);
        currentHighest.put("btc_rur", 0);
        currentHighest.put("btc_eur", 0);
        currentHighest.put("ltc_btc", 0);
        currentHighest.put("ltc_usd", 0);
        currentHighest.put("ltc_rur", 0);
        currentHighest.put("ltc_eur", 0);
        currentHighest.put("nmc_btc", 0);
        currentHighest.put("nmc_usd", 0);
        currentHighest.put("nvc_btc", 0);
        currentHighest.put("nvc_usd", 0);
        currentHighest.put("usd_rur", 0);
        currentHighest.put("eur_usd", 0);
        currentHighest.put("eur_rur", 0);
        currentHighest.put("ppc_btc", 0);
        currentHighest.put("ppc_usd", 0);
    }

    @Override
    public void parse(String data) throws InterruptedException, JSONException, NumberFormatException{
        
        int serialized = 0; //Just a counter to display number of trades serialized.
        InputStream stream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        JSONTokener tokener = new JSONTokener(stream);
        JSONObject root = new JSONObject(tokener);
        JSONObject temp;

        //Loop through the Symbol Pair
        for(int i = 0; i < root.names().length(); i++){
            //Loop through each trade of the given Symbol Pair
            for(int j = 0; j < ((JSONArray)root.get(root.names().get(i).toString())).length(); j++){
                
                //Store the current JSON trade in "temp"
                temp = ((JSONObject)(((JSONArray)root.get(root.names().get(i).toString())).get(j)));
                
                //Confirm this is not a duplicate trade
                if(Long.parseLong(temp.get("tid").toString()) > Long.parseLong(allTimeHighest.get(root.names().get(i).toString()).toString())){
                    if(Long.parseLong(temp.get("tid").toString()) > Long.parseLong(currentHighest.get(root.names().get(i).toString()).toString()))
                        currentHighest.replace(root.names().get(i).toString(), Long.parseLong(temp.get("tid").toString()));
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
                    serialized++; //Increment counter
                }
            }//Close Inner Loop
        }//Close Outer Loop
        
        //Update highest TradeID
        for (String symbpair : symbpairs) {
            if (Long.parseLong(currentHighest.get(symbpair).toString()) > Long.parseLong(allTimeHighest.get(symbpair).toString())) {
                allTimeHighest.replace(symbpair, Long.parseLong(currentHighest.get(symbpair).toString()));
            }
        }
        System.out.println("Serialized BTC-E Trades: " + serialized);
    }//Close Function

    @Override
    public void parse_order(String data) throws Exception {
        InputStream stream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        JSONTokener tokener = new JSONTokener(stream);
        JSONObject root = new JSONObject(tokener); //Entire Object
        JSONObject temp; //To store separate currency pairs
        JSONArray asks, bids; //To store the bids and ask array of a given currency pair
        
        //Loop over each Currency Pair (16 pairs)
        for(int i = 0; i < root.names().length(); i++){
            temp = (JSONObject)root.get(root.names().get(i).toString());
            asks = ((JSONArray)temp.get("asks"));
            bids = ((JSONArray)temp.get("bids"));
            //Loop through each ask for said currency pair
            for(int j = 0; j < asks.length(); j++){
                //Create and Put Ask Order
                Order a = new Order("ask", root.names().get(i).toString(), root.names().get(i).toString(), "BTC-E", Double.parseDouble(((JSONArray)asks.get(j)).get(1).toString()), Double.parseDouble(((JSONArray)asks.get(j)).get(0).toString()), "NULL", "NULL");
                orders.put(a);
            }
            //Loop through each bid for said currency pair
            for(int j = 0; j < bids.length(); j++){
                //Create and Put Ask Order
                Order b = new Order("bid", root.names().get(i).toString(), root.names().get(i).toString(), "BTC-E", Double.parseDouble(((JSONArray)asks.get(j)).get(1).toString()), Double.parseDouble(((JSONArray)asks.get(j)).get(0).toString()), "NULL", "NULL");
                orders.put(b);
            }
        }
        System.out.println("Serialized BTC-E Orders");
    }
  
    
    
}