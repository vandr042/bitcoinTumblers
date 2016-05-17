package slurpdetector;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

public class Detective {

    private ArrayList<IPLink> ip_links = null;
    private HashMap<String, Set<String>> ip_links_inverse = null;
    private ArrayList<String> suspected_slurp_keys = null;
    private ArrayList<String> suspected_slurp_txs = null;
    private ArrayList<String> mix_keys = null;
    private ArrayList<String> deposit_keys = null;

    public Detective() {
        this.ip_links = new ArrayList<>();
        this.suspected_slurp_keys = new ArrayList<>();
        this.suspected_slurp_txs = new ArrayList<>();
        this.mix_keys = new ArrayList<>();
        this.deposit_keys = new ArrayList<>();
        this.ip_links_inverse = new HashMap<>();
    }

    public void startAnalyzing() {
        for (IPLink link : ip_links) {
            callBlockExplorer(link.getTxID());
        }
    }

    //Used initially to import Connor's data
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

    //Used Initially to invert Connor's Data
    public void invertData() {
        for (IPLink e : this.ip_links) {
            String tx = e.getTxID();
            String ip = e.getIP();

            if (!this.ip_links_inverse.containsKey(ip)) {
                this.ip_links_inverse.put(ip, new HashSet<>());
            }
            this.ip_links_inverse.get(ip).add(tx);
        }
        System.out.println(this.ip_links_inverse);
    }

    //Checks if tx is a slurp tx and if so adds it to suspected_slurp_txs. Also appends suspect slurp keys to suspected_slurp_keys
    public void callBlockExplorer(String tx) {
        String urlString = "https://blockexplorer.com/api/tx/" + tx;
        String transactionData = getTransactionJSON(urlString);
        boolean passedF1 = singleInputOutputFilter(transactionData);
        if (passedF1) {
            String txBlockHash = getBlockHash(transactionData);
            String blockInfo = getTransactionJSON("https://blockchain.info/rawblock/" + txBlockHash);
            boolean passedF2 = blockFilter(blockInfo); //appends suspected slurp keys to the suspected_slurps list
            if (passedF2) {
                suspected_slurp_txs.add(tx);
            }
        }
    }

    //First filter: only passes if both vins and vouts are equal to 1
    private boolean singleInputOutputFilter(String tx_data) {
        try {
            InputStream stream = new ByteArrayInputStream(tx_data.getBytes(StandardCharsets.UTF_8));
            JSONTokener tokener = new JSONTokener(stream);
            JSONObject root = new JSONObject(tokener);
            JSONArray inputs = root.getJSONArray("vin");
            JSONArray outputs = root.getJSONArray("vout");
            if (inputs.length() == 1 && outputs.length() == 1) {
                return true;
            } else {
                return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    //Second filter: checks current block for all 1 to 1 transactions and puts the outAddr in a temp list suspectedSlurpkeys. Appends the mode of that list if existant.
    private boolean blockFilter(String block_data) {
        try {
            InputStream stream = new ByteArrayInputStream(block_data.getBytes(StandardCharsets.UTF_8));
            JSONTokener tokener = new JSONTokener(stream);
            JSONObject root = new JSONObject(tokener);
            JSONArray transactions_in_block = root.getJSONArray("tx");
            ArrayList<String> suspectedSlurpKeys = new ArrayList<String>();

            for (int i = 0; i < transactions_in_block.length(); i++) {
                JSONObject currentTxData = transactions_in_block.getJSONObject(i);
                int inputSize = currentTxData.getInt("vin_sz");
                int outputSize = currentTxData.getInt("vout_sz");
                if (inputSize == 1 && outputSize == 1) {
                    JSONArray outputInfo = currentTxData.getJSONArray("out");
                    JSONObject outputInfoObj = outputInfo.getJSONObject(0);
                    suspectedSlurpKeys.add(outputInfoObj.getString("addr"));
                }
            }

            String suspectAddress = getMode(suspectedSlurpKeys);
            System.out.println("Suspected Slurp Key: " + suspectAddress);
            suspected_slurp_keys.add(suspectAddress);
            return true;

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    //This is the function where we ask Connor's to get us the IP addresse of depost and mix keys.
    public void matchIPAddresses() {
        for (String slurptx : suspected_slurp_txs) {
            String urlString = "https://blockexplorer.com/api/tx/" + slurptx;
            String transactionData = getTransactionJSON(urlString);
            try {
                InputStream stream = new ByteArrayInputStream(transactionData.getBytes(StandardCharsets.UTF_8));
                JSONTokener tokener = new JSONTokener(stream);
                JSONObject root = new JSONObject(tokener);
                JSONArray inputs = root.getJSONArray("vin");
                JSONObject inputObject = inputs.getJSONObject(0);
                String inputterAddress = inputObject.getString("addr");
                if (isDepositKey(inputterAddress))
                    this.deposit_keys.add(inputterAddress);
                else
                    this.mix_keys.add(inputterAddress);
             
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    
    private boolean isDepositKey(String inputAddress){
        //Here goes the code where I ask Connor if this is a deposit key, or if it is BitBlender's key.
        return false;
    }

    //Returns the mode of a list
    private String getMode(ArrayList<String> list) {
        HashMap<String, Integer> counter = new HashMap<>();
        for (String s : list) {
            counter.putIfAbsent(s, 0);
            counter.replace(s, counter.get(s) + 1);
        }
        int greatest = 0;
        String key = "";
        for (String s : counter.keySet()) {
            if (counter.get(s) > greatest) {
                greatest = counter.get(s);
                key = s;
            }
        }
        return key;
    }

    //Returns the hash of the block to which this transaction belogs.
    private String getBlockHash(String tx_data) {
        try {
            InputStream stream = new ByteArrayInputStream(tx_data.getBytes(StandardCharsets.UTF_8));
            JSONTokener tokener = new JSONTokener(stream);
            JSONObject root = new JSONObject(tokener);
            String blockhash = root.getString("blockhash");
            return blockhash;
        } catch (JSONException e) {
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
