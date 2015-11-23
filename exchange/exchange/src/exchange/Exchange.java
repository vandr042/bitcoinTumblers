package exchange;

import exchange.Parsers.BTCEParser;
import java.util.concurrent.LinkedBlockingQueue;


public final class Exchange {

    public static void main(String[] args) throws Exception {
        
        LinkedBlockingQueue<Trade> trades = new LinkedBlockingQueue<>();
        
        Serializer serializer = new Serializer(trades);
        serializer.trade_serializer.start();
        serializer.run();
        
        BTCEParser btceparser = new BTCEParser(trades);
        btceparser.btce_parser.start();
        btceparser.run();
        
    }
}
