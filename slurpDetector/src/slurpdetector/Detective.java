package slurpdetector;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

public class Detective {
    
    private ArrayList<IPLink> ip_links = null;
    private HashMap<String, Set<String>> ip_links_inverse = null;
    private ArrayList<String> suspected_slurps = null;
    
    public Detective(){
        this.ip_links = new ArrayList<>();
        this.suspected_slurps = new ArrayList<>();
        this.ip_links_inverse = new HashMap<>();
    }

    public void importData(String dataFileName) {
        File dataFile = new File(dataFileName);
        try {
            Scanner inputStream = new Scanner(dataFile);
            while (inputStream.hasNext()) {
                String data = inputStream.next();
                String[] values = data.split(",");
                ip_links.add(new IPLink(values[0], values[1]));
            }
            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public void invertData(){
        for (IPLink e : this.ip_links){
            String tx = e.getTxID();
            String ip = e.getIP();
            
            if (!this.ip_links_inverse.containsKey(ip)){
                this.ip_links_inverse.put(ip, new HashSet<>());  
            }
            this.ip_links_inverse.get(ip).add(tx);
        }
        System.out.println(this.ip_links_inverse);
    }
    
    public void callBlockExplorer(String tx) {
        String urlString = "https://blockexplorer.com/api/tx/" + tx;
        String transactionData = getTransactionJSON(urlString);
        boolean passedF1 = singleInputOutputFilter(transactionData);
        if(passedF1){
            String txBlockHash = getBlockHash(transactionData);
            String blockInfo = getTransactionJSON("https://blockchain.info/rawblock/" + txBlockHash);
            boolean passedF2 = slurpBlockFilter(blockInfo);
        }
    }
    
    private boolean singleInputOutputFilter(String tx_data){
        try{
            InputStream stream = new ByteArrayInputStream(tx_data.getBytes(StandardCharsets.UTF_8));
            JSONTokener tokener = new JSONTokener(stream);
            JSONObject root = new JSONObject(tokener);
            JSONArray inputs = root.getJSONArray("vin");
            JSONArray outputs = root.getJSONArray("vout");
            if (inputs.length() == 1 && outputs.length() == 1)
                return true;
            else
                return false;
        } catch(JSONException e){
            e.printStackTrace();
            return false;
        } 
    }
    
    private boolean slurpBlockFilter(String block_data){
        try{
            InputStream stream = new ByteArrayInputStream(block_data.getBytes(StandardCharsets.UTF_8));
            JSONTokener tokener = new JSONTokener(stream);
            JSONObject root = new JSONObject(tokener);
            JSONArray transactions_in_block = root.getJSONArray("tx");
            ArrayList<String> suspectedSlurpKeys = new ArrayList<String>();
            
            for (int i = 0; i < transactions_in_block.length(); i++){
                JSONObject currentTxData = transactions_in_block.getJSONObject(i);
                int inputSize = currentTxData.getInt("vin_sz");
                int outputSize = currentTxData.getInt("vout_sz");
                if(inputSize == 1 && outputSize == 1){
                    JSONArray outputInfo = currentTxData.getJSONArray("out");
                    JSONObject outputInfoObj = outputInfo.getJSONObject(0);
                    suspectedSlurpKeys.add(outputInfoObj.getString("addr"));
                }
            }

            System.out.println("Suspected Slurp Key: " + getMode(suspectedSlurpKeys));
            
            return true;
        } catch(JSONException e){
            e.printStackTrace();
            return false;
        } 
    }
    
    private String getMode(ArrayList<String> list){
        HashMap<String, Integer> counter = new HashMap<>();
        for (String s : list){
            counter.putIfAbsent(s, 0);
            counter.replace(s, counter.get(s) + 1);  
        }
        int greatest = 0;
        String key = "";
        for (String s : counter.keySet()){
            if(counter.get(s) > greatest){
                greatest = counter.get(s);
                key = s;
            }
        }
        /*for (String s : counter.keySet()){
            System.out.println(s + ": " + counter.get(s));
        }*/
        return key;
    }
    
    private String getBlockHash(String tx_data){
        try{
            InputStream stream = new ByteArrayInputStream(tx_data.getBytes(StandardCharsets.UTF_8));
            JSONTokener tokener = new JSONTokener(stream);
            JSONObject root = new JSONObject(tokener);
            String blockhash = root.getString("blockhash");
            return blockhash;
        } catch(JSONException e){
            e.printStackTrace();
            return null;
        }
    }
    
    private String getTransactionJSON(String url) {
        StringBuffer response = new StringBuffer();
        String transactionJSON = ""; //Will contain the JSON

        //Handle the HTTP Connection
        try {
            URL urlObj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            con.disconnect();
            transactionJSON = response.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return transactionJSON;
    }
    
}