package exchange;

import java.util.Date;


public class Trade {
    
    //Don;t exactly know what provenance should be...
    private String type, SYMB_PAIR, EXCH, prov;
    private float volume, price;
    private Date time_completed;
    private int Trade_ID;
    
    public void Trade(String type, String SYMB_PAIR, String EXCH, String prov, float volume, float price, Date time_completed, int Trade_ID){
        
        //Will add better functions {get(), set()} if needed...
        this.type = type;
        this.SYMB_PAIR = SYMB_PAIR;
        this.EXCH = EXCH;
        this.prov = prov;
        this.volume = volume;     
        this.price = price;
        this.Trade_ID = Trade_ID;
                
    }
    
}
