package valueset;

import java.net.*;
import java.io.*;


public class HttpClient implements Runnable{
    
    private final String URL;
    public Thread worker;
    private JSONParser parser = null;
    
    public HttpClient(String url, JSONParser p){
        this.URL = url;
        this.parser = p;
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
                    parser.parse(this.getHypertext()); //Method in parser to parse trades
            }
            catch(Exception e){
                e.printStackTrace();
            }
            
            try{
            System.out.println("Sleeping");
            Thread.sleep(10000); //Wait for 10 seconds
            }
            catch(InterruptedException e){
               e.printStackTrace();
            }
        }
    }
    
}
