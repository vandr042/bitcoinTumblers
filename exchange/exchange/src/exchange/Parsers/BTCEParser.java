package exchange.Parsers;

import org.json.*;
import exchange.Trade;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;


public class BTCEParser implements Parser{
    
    LinkedBlockingQueue<Trade> trades;

    public BTCEParser(LinkedBlockingQueue<Trade> queue){
        this.trades = queue; //Pass reference to our main queue into varible "trades"
    }

    @Override
    public void parse(String data) throws Exception{
        System.out.println("BTCE");
        InputStream stream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
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