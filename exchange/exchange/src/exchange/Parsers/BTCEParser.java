package exchange.Parsers;

import exchange.Clients.http.*;
import exchange.Trade;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;
import java.util.concurrent.LinkedBlockingQueue;


public class BTCEParser {
    
    String url = "https://btc-e.com/api/3/trades/btc_usd-btc_rur-btc_eur-ltc_btc-ltc_usd-ltc_rur-ltc_eur-nmc_btc-nmc_usd-nvc_btc-nvc_usd-usd_rur-eur_usd-eur_rur-ppc_btc-ppc_usd";
    String hypertext = "";
    public LinkedBlockingQueue<Trade> trades;
    
    
    public BTCEParser(LinkedBlockingQueue<Trade> queue){
        this.trades = queue;
        
        HttpClient http = new HttpClient();
        hypertext = http.getHypertext(url);
        try{
            parse(hypertext);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    
    public void parse(String hypertext) throws Exception{
        InputStream stream = new ByteArrayInputStream(hypertext.getBytes(StandardCharsets.UTF_8));
        JSONTokener tokener = new JSONTokener(stream);
        JSONObject root = new JSONObject(tokener);
        
        JSONObject temp;
        
        for(int i = 0; i < root.names().length(); i++){
            for(int j = 0; j < ((JSONArray)root.get(root.names().get(i).toString())).length(); j++){
                
                temp = ((JSONObject)(((JSONArray)root.get(root.names().get(i).toString())).get(j)));
                
                Trade a = new Trade(temp.get("type").toString(), 
                                    root.names().get(i).toString(), 
                                    root.names().get(i).toString(),
                                    "btc-e",
                                    Double.parseDouble(temp.get("price").toString()),
                                    Double.parseDouble(temp.get("amount").toString()),
                                    temp.get("timestamp").toString(),
                                    temp.get("tid").toString()
                );
                
                trades.put(a);
            }
            break;
        }
    }
    
}
