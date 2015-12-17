package exchange.Parsers;

import exchange.Order;
import org.json.*;
import exchange.Trade;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class BitfinexParser implements Parser {

    LinkedBlockingQueue<Trade> trades;
    LinkedBlockingQueue<Order> orders;
    Map channels = new HashMap();

    public BitfinexParser(LinkedBlockingQueue<Trade> trade_queue, LinkedBlockingQueue<Order> order_queue) {
        this.trades = trade_queue; //Pass reference to our main queue into varible "trades"
        this.orders = order_queue;
    }

    @Override
    public void parse(String data) throws InterruptedException, JSONException, NumberFormatException {
        //If server sends a non-trade object, ignore
        if (data.startsWith("{")) {
            return;
        }
        //If server sends a heartbeat, ignore
        if (data.contains("hb")) {
            return;
        }

        //Only for the first data dump received from BITFINEX, all subsequent trades go to the "else"...
        if (data.contains(",[[")) {
            InputStream stream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
            JSONTokener tokener = new JSONTokener(stream);
            JSONArray array = new JSONArray(tokener);
            array = (JSONArray) array.get(1);
            JSONArray temp;

            //Loop through each trade in array
            for (int i = 0; i < array.length(); i++) {
                temp = (JSONArray) array.get(i);

                //Create Trade Object
                if (temp.get(3).toString().startsWith("-")) {
                    Trade a = new Trade("Sell",
                            temp.get(0).toString().substring(temp.get(0).toString().length() - 6, temp.get(0).toString().length()),
                            temp.get(0).toString().substring(temp.get(0).toString().length() - 6, temp.get(0).toString().length()),
                            "BITFINEX",
                            Double.parseDouble(temp.get(2).toString()),
                            Double.parseDouble(temp.get(3).toString().replace("-", "")),
                            temp.get(1).toString(),
                            temp.get(0).toString());
                    //Put Trade object inside our queue
                    trades.put(a);
                } else {
                    Trade a = new Trade("Buy",
                            temp.get(0).toString().substring(temp.get(0).toString().length() - 6, temp.get(0).toString().length()),
                            temp.get(0).toString().substring(temp.get(0).toString().length() - 6, temp.get(0).toString().length()),
                            "BITFINEX",
                            Double.parseDouble(temp.get(2).toString()),
                            Double.parseDouble(temp.get(3).toString().replace("-", "")),
                            temp.get(1).toString(),
                            temp.get(0).toString());
                    //Put Trade object inside our queue
                    trades.put(a);
                }
            }//Close  Loop
        }
        else {
            InputStream stream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
            JSONTokener tokener = new JSONTokener(stream);
            JSONArray temp = new JSONArray(tokener);

            //Create Trade Object
            if (temp.get(4).toString().startsWith("-")) {
                Trade a = new Trade("Sell",
                        temp.get(1).toString().substring(temp.get(1).toString().length() - 6, temp.get(1).toString().length()),
                        temp.get(1).toString().substring(temp.get(1).toString().length() - 6, temp.get(1).toString().length()),
                        "BITFINEX",
                        Double.parseDouble(temp.get(3).toString()),
                        Double.parseDouble(temp.get(4).toString().replace("-", "")),
                        temp.get(2).toString(),
                        temp.get(1).toString());
                //Put Trade object inside our queue
                trades.put(a);
            } else {
                Trade a = new Trade("Buy",
                        temp.get(1).toString().substring(temp.get(1).toString().length() - 6, temp.get(1).toString().length()),
                        temp.get(1).toString().substring(temp.get(1).toString().length() - 6, temp.get(1).toString().length()),
                        "BITFINEX",
                        Double.parseDouble(temp.get(3).toString()),
                        Double.parseDouble(temp.get(4).toString().replace("-", "")),
                        temp.get(2).toString(),
                        temp.get(1).toString());
                //Put Trade object inside our queue
                trades.put(a);
            }
        }//Close Else
        System.out.println("Serializing BITFINEX Trades");
    }

    
    @Override
    public void parse_order(String data) throws JSONException, InterruptedException, NumberFormatException{
        
        InputStream stream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        JSONTokener tokener = new JSONTokener(stream);

        if (data.startsWith("{\"event\":\"subscribed\",\"channel\":\"book\"")) {
            JSONObject subscription_info = new JSONObject(tokener);

            if (subscription_info.get("pair").equals("BTCUSD")) {
                channels.put(subscription_info.getInt("chanId"), "BTCUSD");
            }
            if (subscription_info.get("pair").equals("LTCUSD")) {
                channels.put(subscription_info.getInt("chanId"), "LTCUSD");
            }
            if (subscription_info.get("pair").equals("LTCBTC")) {
                channels.put(subscription_info.getInt("chanId"), "LTCBTC");
            }
        }
        
        else if(data.matches("\\[[0-9].*$") && !data.contains("\"hb\"]")){
            
            JSONArray order = new JSONArray(tokener);
            JSONArray temp;
            
            //Executes if this is a Snapshot (a list of orders)
            if (order.get(1).toString().startsWith("[[")){ 
                JSONArray list = (JSONArray)order.get(1);
                for (int i = 0; i < list.length(); i++){
                    temp = (JSONArray)list.get(i);
                    if (temp.get(2).toString().startsWith("-")){
                        Order a = new Order("ask", channels.get(order.get(0)).toString(), channels.get(order.get(0)).toString(), "BITFINEX", Double.parseDouble(temp.get(0).toString()), Double.parseDouble(temp.get(2).toString()), "NULL", "NULL");
                        orders.put(a);
                    }
                    else{
                        Order a = new Order("bid", channels.get(order.get(0)).toString(), channels.get(order.get(0)).toString(), "BITFINEX", Double.parseDouble(temp.get(0).toString()), Double.parseDouble(temp.get(2).toString()), "NULL", "NULL");
                        orders.put(a);
                    }
                }
            }
            //Executes if this is an update
            else{ 
                if (order.get(3).toString().startsWith("-")){
                    Order a = new Order("ask", channels.get(order.get(0)).toString(), channels.get(order.get(0)).toString(), "BITFINEX", Double.parseDouble(order.get(1).toString()), Double.parseDouble(order.get(3).toString()), "NULL", "NULL");
                    orders.put(a);
                }
                else{
                    Order a = new Order("bid", channels.get(order.get(0)).toString(), channels.get(order.get(0)).toString(), "BITFINEX", Double.parseDouble(order.get(1).toString()), Double.parseDouble(order.get(3).toString()), "NULL", "NULL");
                    orders.put(a);
                }
            }
            
            System.out.println("Serializing BITFINEX Orders");
            
        }
    }

}
