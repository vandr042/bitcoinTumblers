package exchange;

import java.util.Date;


public class Order {
    
    //Don't exactly know what provenance should be...
    private String type, SYMB_PAIR, EXCH, prov;
    private float volume, price;
    private Date first_seen, last_seen;
    
    
    public void Order(String type, String SYMB_PAIR, String EXCH, String prov, float volume, float price, Date first_seen, Date last_seen){
        
        //Will add better functions {get(), set()} if needed...
        this.type = type;
        this.SYMB_PAIR = SYMB_PAIR;
        this.EXCH = EXCH;
        this.prov = prov;
        this.volume = volume;     
        this.price = price;
        this.first_seen = first_seen;
        this.last_seen = last_seen;
                
    }
    
}
