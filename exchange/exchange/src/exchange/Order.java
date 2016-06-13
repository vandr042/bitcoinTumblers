package exchange;


public class Order {
    
    private final String type, //Ask = Sell; Bid = Buy
                         SYMB_PAIR, //ex. "BTC_USD"
                         EXCH, //ex. "BTC_USD"
                         prov, //Where we got the info from ex. "btc-e.com"
                         first_seen, 
                         last_seen;
    
    private final double volume, //amount bought or sold
                         price; //price at which trade was made
    
    /**
     *
     * @param type
     * @param SYMB_PAIR
     * @param EXCH
     * @param prov
     * @param volume
     * @param price
     * @param first_seen
     * @param last_seen
     */
    public Order(String type, String SYMB_PAIR, String EXCH, String prov, double volume, double price, String first_seen, String last_seen){
        this.type = type;
        this.SYMB_PAIR = SYMB_PAIR;
        this.EXCH = EXCH;
        this.prov = prov;
        this.volume = volume;     
        this.price = price;
        this.first_seen = first_seen;
        this.last_seen = last_seen;            
    }   
    
    //Returns a String representation of the Order that can be appended to the CSV.
    public String getCSV(){
        return prov+","+EXCH+","+SYMB_PAIR+","+type+","+price+","+volume+","+first_seen+","+last_seen;
    }
    
}
