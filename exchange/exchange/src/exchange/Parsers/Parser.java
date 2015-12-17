package exchange.Parsers;


public interface Parser {
    
    public abstract void parse(String data) throws Exception;
    public abstract void parse_order(String data) throws Exception;
    
}
