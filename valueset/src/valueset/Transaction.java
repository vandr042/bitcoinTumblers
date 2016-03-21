package valueset;


//  1. Create a net client
//  2. Pull JSON
//  3. Parse JSON



public class Transaction {
    
    private String ID, amount, timestamp;
    
    public Transaction(String id, String amt, String ts){
        this.ID = id;
        this.amount = amt;
        this.timestamp = ts;
    }
    
}
