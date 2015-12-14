package exchange.Clients.http;

import java.net.*;
import java.io.*;
import exchange.Parsers.Parser;


public class HttpClient implements Runnable{
    
    private final String URL;
    public Thread worker;
    private Parser parser = null;
    boolean isOrder;
    
    public HttpClient(String url, Parser p, boolean isorder){
        this.URL = url;
        this.parser = p;
        this.isOrder = isorder;
        worker = new Thread(this); 
    }
    
    //Standard GET request call in Java
    private String getHypertext(){
        StringBuffer response = new StringBuffer();
        
        try{
            URL obj = new URL(URL);
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

    @Override
    public void run() {
        while(Thread.currentThread() == this.worker){
            try{
                if(this.isOrder)
                    parser.parse_order(this.getHypertext());
                else if(!this.isOrder)
                    parser.parse(this.getHypertext());
            }
            catch(Exception e){
                e.printStackTrace();
            }
            
            try{
            System.out.println("Sleeping");
            Thread.sleep(10000);
            }
            catch(InterruptedException e){
               e.printStackTrace();
            }
        }
    }
    
}
