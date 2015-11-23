package exchange;


public class Trade {

    // <editor-fold defaultstate="collapsed" desc="Trade Variables">
    private String type, //Ask = Sell; Bid = Buy
                   SYMB_PAIR, //ex. "BTC_USD"
                   EXCH, //ex. "BTC_USD"
                   prov, //Where we got the info from ex. "btc-e.com"
                   time_completed, //timestamp of the trade
                   tid; //trade ID
    
    private double volume, //amount bought or sold
                   price; //price at which trade was made
    // </editor-fold>
    
    public Trade(String type, String SYMB_PAIR, String EXCH, String prov, double price, double volume, String time_completed, String tid){
        this.type = type;
        this.SYMB_PAIR = SYMB_PAIR;
        this.EXCH = EXCH;
        this.prov = prov;
        this.volume = volume;     
        this.price = price;
        this.time_completed = time_completed;
        this.tid = tid;           
    }
    
    //Returns a String representation of the Trade that can be appended to the CSV.
    public String getCSV(){
        return tid+","+prov+","+EXCH+","+SYMB_PAIR+","+type+","+price+","+volume+","+time_completed;
    }
}
