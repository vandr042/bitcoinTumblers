package exchange.Parsers;

import exchange.Clients.http.*;
import exchange.Trade;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;
import javafx.collections.ObservableList;


public class BTCEParser {
    
    String url = "https://btc-e.com/api/3/trades/btc_usd-btc_rur-btc_eur-ltc_btc-ltc_usd-ltc_rur-ltc_eur-nmc_btc-nmc_usd-nvc_btc-nvc_usd-usd_rur-eur_usd-eur_rur-ppc_btc-ppc_usd";
    String hypertext = "";
    
    
    
    public BTCEParser(){
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
        System.out.println(root.length());
        
        //Next I will write the code to loop through each set inside root, and convert them into Trade objects that will be put into the Observable list, 
        //which is serialized when its count gets to 1 Million or maybe 100,000.
    }
    
}
