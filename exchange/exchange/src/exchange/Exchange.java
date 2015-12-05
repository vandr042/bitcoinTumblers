package exchange;

import java.util.concurrent.LinkedBlockingQueue;
import exchange.Clients.http.HttpClient;
import exchange.Clients.websocket.WebSocketClient;
import exchange.Parsers.*;
import java.net.URISyntaxException;


public final class Exchange {
    

    public static void main(String[] args) throws Exception {
        
        LinkedBlockingQueue<Trade> trades = new LinkedBlockingQueue<>();
        
        Serializer serializer = new Serializer(trades);
        serializer.trade_serializer.start();
        serializer.run();

        //Initialize Bitfinex
        BitfinexParser bitfinex_parser = new BitfinexParser(trades);
        String BITFINEX_URL = "wss://api2.bitfinex.com:3000/ws";
        String BITFINEX_REQEST_BTCUSD = "{\"event\":\"subscribe\", \"channel\":\"trades\", \"pair\":[\"btcusd\"]}"; //examples at http://docs.bitfinex.com/?javascript#order-books
        String BITFINEX_REQEST_LTCUSD = "{\"event\":\"subscribe\", \"channel\":\"trades\", \"pair\":[\"ltcusd\"]}";
        String BITFINEX_REQEST_LTCBTC = "{\"event\":\"subscribe\", \"channel\":\"trades\", \"pair\":[\"ltcbtc\"]}";
        WebSocketClient bitfinex_client = new WebSocketClient(BITFINEX_URL, bitfinex_parser);
        try{
            bitfinex_client.connect();
            bitfinex_client.send(BITFINEX_REQEST_BTCUSD);
            bitfinex_client.send(BITFINEX_REQEST_LTCUSD);
            bitfinex_client.send(BITFINEX_REQEST_LTCBTC);
        } catch (InterruptedException | URISyntaxException e) { e.printStackTrace(); }
        
        //Initialize BTC-E
        BTCEParser btce_parser = new BTCEParser(trades);
        String BTCE_URL = "https://btc-e.com/api/3/trades/btc_usd-btc_rur-btc_eur-ltc_btc-ltc_usd-ltc_rur-ltc_eur-nmc_btc-nmc_usd-nvc_btc-nvc_usd-usd_rur-eur_usd-eur_rur-ppc_btc-ppc_usd?limit=150";
        HttpClient btce_client = new HttpClient(BTCE_URL, btce_parser);
        btce_client.worker.start();
        btce_client.run();
    }
}
