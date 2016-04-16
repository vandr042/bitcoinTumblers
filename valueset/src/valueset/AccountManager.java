package valueset;

import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

public class AccountManager {

    private ArrayList<Transaction> deposits = null;
    private ArrayList<Transaction> withdrawls = null;
    private HashMap<String, Set<String>> aliases = null;
    private HashMap<String, String> aliasReverse = null;
    private HashMap<String, Set<String>> currentAnonimitySetsForAlises = null;
    private int currentAliasVirtualKey = 1;

    public AccountManager() {
        this.deposits = new ArrayList<Transaction>();
        this.withdrawls = new ArrayList<Transaction>();
        this.aliases = new HashMap<String, Set<String>>();
        this.aliasReverse = new HashMap<String, String>();
        this.currentAnonimitySetsForAlises = new HashMap<String, Set<String>>();
        this.importData("../miscScripts/balance-synth.log");
        Collections.sort(this.deposits);
        Collections.sort(this.withdrawls);
    }

    public void generateAliases() {
        //for (Transaction currentWithdrawl : withdrawls){
        goToBlockchainExplorer(new Transaction(false, "35DafUtd6RPEi9JRK4gEc3Z6s9FRkh9ohW", 1457868000, 1.000)); //Just for temporary use
        //}
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

    public void goToBlockchainExplorer(Transaction withdrawl) {
        String urlString = "https://blockexplorer.com/api/txs/?address=" + withdrawl.getKeyResponsible();
        String transactionData = getTransactionJSON(urlString);
        
        if (!aliasReverse.containsKey(withdrawl.getKeyResponsible())){
            String vKey = "VirtualKey" + currentAliasVirtualKey;
            aliasReverse.put(withdrawl.getKeyResponsible(), vKey);
            aliases.put(vKey, new HashSet<String>());
            aliases.get(vKey).add(withdrawl.getKeyResponsible());
            currentAliasVirtualKey++;
        }
        
        //Handle the parsing of the JSON
        try {
            InputStream stream = new ByteArrayInputStream(transactionData.getBytes(StandardCharsets.UTF_8));
            JSONTokener tokener = new JSONTokener(stream);
            JSONObject root = new JSONObject(tokener);
            JSONArray txs = root.getJSONArray("txs");

            for (int i = 0; i < txs.length(); i++) {
                JSONObject txInfo = txs.getJSONObject(i);
                //String valueOut = txInfo.get("valueOut").toString();
                JSONArray vIn = txInfo.getJSONArray("vin");

                //Loop through all the vIn's of the transaction
                for (int j = 0; j < vIn.length(); j++) {
                    JSONObject valueOutInfo = vIn.getJSONObject(j);
                    String aliasAddress = valueOutInfo.getString("addr");
                    if (!aliasReverse.containsKey(aliasAddress)) {
                        aliasReverse.put(aliasAddress, aliasReverse.get(withdrawl.getKeyResponsible()));
                    }
                    aliases.get(aliasReverse.get(withdrawl.getKeyResponsible())).add(aliasAddress);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public List<Integer> runExperiment(boolean reportKeys) {
        BufferedWriter fos = null;
        List<Integer> anonSetSizes = new ArrayList<Integer>(this.withdrawls.size());

        try {
            fos = new BufferedWriter(new FileWriter("anonimity_set.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        int count = 0;
        int current = 10;
        for (Transaction currentWithdrawl : this.withdrawls) {
            try {
                fos.write(currentWithdrawl + "\n");
                Set<String> anonSet = getAnonimitySet(currentWithdrawl);
                Set<String> intersection = new HashSet<String>(anonSet);
                
                if (!currentAnonimitySetsForAlises.containsKey(aliasReverse.get(currentWithdrawl.getKeyResponsible()))){
                    currentAnonimitySetsForAlises.put(aliasReverse.get(currentWithdrawl.getKeyResponsible()), anonSet);
                }
                intersection.retainAll(currentAnonimitySetsForAlises.get(aliasReverse.get(currentWithdrawl.getKeyResponsible())));
                currentAnonimitySetsForAlises.replace(aliasReverse.get(currentWithdrawl.getKeyResponsible()), intersection);
                
                anonSetSizes.add(intersection.size());
                fos.write("anon set size: " + intersection.size() + "\n");
                if (reportKeys) {
                    for (String j : intersection) {
                        fos.write("\t" + j + "\n");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (count == this.withdrawls.size() * current / 100) {
                System.out.println(current + "% done");
                current += 10;
            }
            count++;
        }

        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return anonSetSizes;
    }

    private void importData(String dataFileName) {
        File dataFile = new File(dataFileName);
        try {
            Scanner inputStream = new Scanner(dataFile);
            while (inputStream.hasNext()) {
                String data = inputStream.next();
                String[] values = data.split(",");
                if (values[0].toLowerCase().startsWith("dep")) {
                    this.deposits.add(
                            new Transaction(true, values[1], Long.parseLong(values[3]), Double.parseDouble(values[2])));
                } else if (values[0].toLowerCase().startsWith("with")) {
                    this.withdrawls.add(new Transaction(false, values[1], Long.parseLong(values[3]),
                            Double.parseDouble(values[2])));
                }
            }
            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Set<String> getAnonimitySet(Transaction withdrawl) {
        Set<String> anonimitySet = new HashSet<String>();
        HashMap<String, Double> balances = new HashMap<String, Double>();
        double summedWithdrawalValues = withdrawl.getValue();

        /*
         * Build the total deposits for all keys at the point in time when the
         * withdrawl happens
         */
        for (Transaction tDeposit : this.deposits) {
            /*
             * Check to see if we're later than the withdrawl, if so we can stop
             * accumulating deposit
             */
            if (tDeposit.getTimeStamp() > withdrawl.getTimeStamp()) {
                break;
            }

            /*
             * Updating the total amount the key has deposited
             */
            if (!balances.containsKey(tDeposit.getKeyResponsible())) {
                balances.put(tDeposit.getKeyResponsible(), 0.0);
            }
            balances.put(tDeposit.getKeyResponsible(),
                    balances.get(tDeposit.getKeyResponsible()) + tDeposit.getValue());
        }

        /*
         * Now we increase summedWithdrawalValues if an alias made a withdrawl earlier
         */
        for (Transaction tWithdrawal : this.withdrawls) {

            //Check if this withdrawl is previous to our current withdrawl
            if (tWithdrawal.getTimeStamp() > withdrawl.getTimeStamp()) {
                break;
            }

            //Check if this withdrawal is in our current withdrawal's anonymity set
            if (aliasReverse.containsKey(withdrawl.getKeyResponsible())) {
                if (aliases.get(aliasReverse.get(withdrawl.getKeyResponsible())).contains(tWithdrawal.getKeyResponsible())) {
                    //If it is, then increase the value of the actual withdrawal
                    summedWithdrawalValues += tWithdrawal.getValue();
                }
            }
            
        }

        /*
         * Find all keys that had sufficient value to ask for the withdrawal
         */
        for (String tempDepositKey : balances.keySet()) {
            if (balances.get(tempDepositKey) >= summedWithdrawalValues) {
                anonimitySet.add(tempDepositKey);
            }
        }

        return anonimitySet;
    }

}