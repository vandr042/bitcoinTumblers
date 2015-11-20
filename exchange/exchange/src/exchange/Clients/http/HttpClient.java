package exchange.Clients.http;

import java.net.*;
import java.io.*;


public class HttpClient {
    
    public HttpClient(){
        
    }
    
    public String getHypertext(String url){
        StringBuffer response = new StringBuffer();
        
        try{
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
		response.append(inputLine);
            }
            in.close();
        }
        catch(IOException e){
            e.printStackTrace();
        }
        return response.toString();
    }
    
}
