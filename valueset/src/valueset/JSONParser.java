package valueset;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.json.*;



public class JSONParser {
    
    public JSONParser(){
    }
    
    public void parse(String data) throws InterruptedException, JSONException, NumberFormatException{
        
        InputStream stream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        JSONTokener tokener = new JSONTokener(stream);
        JSONObject root = new JSONObject(tokener);
        JSONArray txs = root.getJSONArray("txs");
        System.out.println(txs.getJSONObject(0).getJSONArray("vin"));
        
    }
    
}
