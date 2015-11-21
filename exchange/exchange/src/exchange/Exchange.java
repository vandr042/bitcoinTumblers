package exchange;

import exchange.Clients.http.NettyHttpClient;
import exchange.Parsers.BTCEParser;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;


public final class Exchange {

    public static void main(String[] args) throws Exception {
        
        LinkedBlockingQueue<Trade> trades = new LinkedBlockingQueue<>();
        
        System.out.println(trades.size());
        BTCEParser btceparser = new BTCEParser(trades);
        System.out.println(trades.size());
        System.out.println(((Trade)(trades.peek())).getString());

    }
}
