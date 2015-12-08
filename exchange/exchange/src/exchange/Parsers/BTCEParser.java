package exchange.Parsers;

import org.json.*;
import exchange.Trade;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;


public class BTCEParser implements Parser{
    
    LinkedBlockingQueue<Trade> trades;
    Map allTimeHighest = new HashMap();
    Map currentHighest = new HashMap();
    String[] symbpairs = {"btc_usd", "btc_rur", "btc_eur", "ltc_btc", "ltc_usd", "ltc_rur", "ltc_eur", "nmc_btc", "nmc_usd", "nvc_btc", "nvc_usd", "usd_rur", "eur_usd", "eur_rur", "ppc_btc", "ppc_usd"};

    
    public BTCEParser(LinkedBlockingQueue<Trade> queue){
        this.trades = queue; //Pass reference to our main queue into varible "trades"
        
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
        int serialized = 0;
        System.out.println("BTCE");
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
                    serialized++;
                }
            }//Close Inner Loop
        }//Close Outer Loop
        for (String symbpair : symbpairs) {
            if (Long.parseLong(currentHighest.get(symbpair).toString()) > Long.parseLong(allTimeHighest.get(symbpair).toString())) {
                allTimeHighest.replace(symbpair, Long.parseLong(currentHighest.get(symbpair).toString()));
            }
        }
        System.out.println("Trades Serialized: " + serialized);
    }
  
}