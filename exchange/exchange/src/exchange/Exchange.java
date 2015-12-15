package exchange;

import java.util.concurrent.LinkedBlockingQueue;
import exchange.Clients.http.HttpClient;
import exchange.Clients.websocket.WebSocketClient;
import exchange.Parsers.*;
import java.net.URISyntaxException;
import javax.net.ssl.SSLException;


public final class Exchange {
    
    static LinkedBlockingQueue<Trade> trades = new LinkedBlockingQueue<>(); //Queue to store trades
    static LinkedBlockingQueue<Order> orders = new LinkedBlockingQueue<>(); //Queue to store trades
    
    private static void initBitfinex() throws SSLException, URISyntaxException{
        
        //Initialize Bitfinex - required to make three sepearate requests (in JSON format) to WSS to get all three exchanges.
        BitfinexParser bitfinex_parser = new BitfinexParser(trades, orders);
        
        String BITFINEX_URL = "wss://api2.bitfinex.com:3000/ws";
        
        String BITFINEX_TRADE_REQEST_BTCUSD = "{\"event\":\"subscribe\", \"channel\":\"trades\", \"pair\":[\"btcusd\"]}"; //examples at http://docs.bitfinex.com/?javascript#order-books
        String BITFINEX_TRADE_REQEST_LTCUSD = "{\"event\":\"subscribe\", \"channel\":\"trades\", \"pair\":[\"ltcusd\"]}";
        String BITFINEX_TRADE_REQEST_LTCBTC = "{\"event\":\"subscribe\", \"channel\":\"trades\", \"pair\":[\"ltcbtc\"]}";
        
        String BITFINEX_ORDER_REQEST_BTCUSD = "{\"event\":\"subscribe\", \"channel\":\"book\", \"pair\":[\"btcusd\"]}"; //examples at http://docs.bitfinex.com/?javascript#order-books
        String BITFINEX_ORDER_REQEST_LTCUSD = "{\"event\":\"subscribe\", \"channel\":\"book\", \"pair\":[\"ltcusd\"]}";
        String BITFINEX_ORDER_REQEST_LTCBTC = "{\"event\":\"subscribe\", \"channel\":\"book\", \"pair\":[\"ltcbtc\"]}";
        
        WebSocketClient bitfinex_client_trades = new WebSocketClient(BITFINEX_URL, bitfinex_parser, false);
        WebSocketClient bitfinex_client_orders = new WebSocketClient(BITFINEX_URL, bitfinex_parser, true);
        
        try{
            bitfinex_client_trades.connect();
            bitfinex_client_trades.send(BITFINEX_TRADE_REQEST_BTCUSD);
            bitfinex_client_trades.send(BITFINEX_TRADE_REQEST_LTCUSD);
            bitfinex_client_trades.send(BITFINEX_TRADE_REQEST_LTCBTC);
            
            bitfinex_client_orders.connect();
            bitfinex_client_orders.send(BITFINEX_ORDER_REQEST_BTCUSD);
            bitfinex_client_orders.send(BITFINEX_ORDER_REQEST_LTCUSD);
            bitfinex_client_orders.send(BITFINEX_ORDER_REQEST_LTCBTC);
        } catch (InterruptedException | URISyntaxException e) { e.printStackTrace(); }
        
    }
    private static void initPoloniex() throws SSLException, URISyntaxException{
        
        /*PoloniexParser poloniex_parser = new PoloniexParser(trades);
        
        String POLONIEX_URL = "wss://api.poloniex.com";
        String POLONIEX_REQEST_BTCUSD = "subscribe";
        
        WebSocketClient poloniex_client = new WebSocketClient(POLONIEX_URL, poloniex_parser, false);
        
        try{
            poloniex_client.connect();
            //poloniex_client.send(POLONIEX_REQEST_BTCUSD);
        } catch (InterruptedException | URISyntaxException e) { e.printStackTrace(); }*/
        
    }
    private static void initBTCE(){
        //Initialize BTC-E - URL takes care of retreving all exchange pairs; retrieves 150 trades for each pair.
        BTCEParser btce_parser = new BTCEParser(trades, orders);
        
        String BTCE_URL_TRADES = "https://btc-e.com/api/3/trades/btc_usd-btc_rur-btc_eur-ltc_btc-ltc_usd-ltc_rur-ltc_eur-nmc_btc-nmc_usd-nvc_btc-nvc_usd-usd_rur-eur_usd-eur_rur-ppc_btc-ppc_usd?limit=150";
        String BTCE_URL_ORDERS = "https://btc-e.com/api/3/depth/btc_usd-btc_rur-btc_eur-ltc_btc-ltc_usd-ltc_rur-ltc_eur-nmc_btc-nmc_usd-nvc_btc-nvc_usd-usd_rur-eur_usd-eur_rur-ppc_btc-ppc_usd?limit=10";
        
        HttpClient btce_client_trades = new HttpClient(BTCE_URL_TRADES, btce_parser, false);
        HttpClient btce_client_orders = new HttpClient(BTCE_URL_ORDERS, btce_parser, true);
        
        btce_client_trades.worker.start();
        btce_client_orders.worker.start();
        btce_client_trades.run();
        btce_client_orders.run();
    }
    private static void initOKCoin() throws SSLException, URISyntaxException{
        
        //Initialize OKCoin - required to make three sepearate requests (in JSON format) to WSS to get all three exchanges.
        OKCoinParser okcoin_parser = new OKCoinParser(trades, orders);
        
        String OKCOIN_URL = "wss://real.okcoin.com:10440/websocket/okcoinapi";
        String OKCOIN_TRADE_REQEST_BTCUSD = "{\"event\":\"addChannel\", \"channel\":\"ok_btcusd_trades_v1\"}";
        String OKCOIN_TRADE_REQEST_LTCUSD = "{\"event\":\"addChannel\", \"channel\":\"ok_ltcusd_trades_v1\"}";
        
        String OKCOIN_ORDER_REQEST_BTCUSD = "{\"event\":\"addChannel\", \"channel\":\"ok_btcusd_depth\"}";
        String OKCOIN_ORDER_REQEST_LTCUSD = "{\"event\":\"addChannel\", \"channel\":\"ok_ltcusd_depth\"}";
        //LTCCNY and BTCCNY don't work, even thought crytocoinschart.info says...
        
        WebSocketClient okcoin_client_trade = new WebSocketClient(OKCOIN_URL, okcoin_parser, false);
        WebSocketClient okcoin_client_order = new WebSocketClient(OKCOIN_URL, okcoin_parser, true);
        
        try{
            okcoin_client_trade.connect();
            okcoin_client_trade.send(OKCOIN_TRADE_REQEST_BTCUSD);
            okcoin_client_trade.send(OKCOIN_TRADE_REQEST_LTCUSD);
            okcoin_client_order.connect();
            okcoin_client_order.send(OKCOIN_ORDER_REQEST_BTCUSD);
            okcoin_client_order.send(OKCOIN_ORDER_REQEST_LTCUSD);
        } catch (InterruptedException | URISyntaxException e) { e.printStackTrace(); }
        
    }
    
    
    public static void main(String[] args) throws Exception {
        
        Serializer serializer = new Serializer(trades, orders); //Serializes the trades and objects in the respective queues. 
        serializer.trade_serializer.start();
        serializer.order_serializer.start();
        serializer.run();

        initBitfinex(); //Initialize Bitfinex
        initBTCE(); //Initialize BTC-E
        initOKCoin(); //Initialize OKCoin
  
    }

    
}
