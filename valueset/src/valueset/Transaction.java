package valueset;

public class Transaction implements Comparable<Transaction> {

	private boolean isDeposit;
	private String keyResponsible;
	private long timeStamp;
	private double value;

	public Transaction(boolean isDeposit, String keyResponsible, long timeStamp, double value) {
		super();
		this.isDeposit = isDeposit;
		this.keyResponsible = keyResponsible;
		this.timeStamp = timeStamp;
		this.value = value;
	}
	
	public String toString(){
		String base = null;
		
		if(this.isDeposit){
			base = "Deposit: ";
		}else{
			base = "Withdrawl: ";
		}
		
		return base + this.keyResponsible + " at " + this.timeStamp + " of value " + value;
	}

	public boolean isDeposit() {
		return isDeposit;
	}

	public String getKeyResponsible() {
		return keyResponsible;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public double getValue() {
		return value;
	}

	@Override
	public int compareTo(Transaction o) {
		if (this.timeStamp < o.timeStamp) {
			return -1;
		} else if (this.timeStamp == o.timeStamp) {
			return 0;
		} else {
			return 1;
		}
	}

}
