package bitcoinLink;

import java.util.*;

public class FinderResult {

	private Set<String> inputs;
	private Set<String> outputs;
	private HashMap<String, Double> payments;
	private Date timeStamp;

	public FinderResult(Date ts) {
		this.inputs = new HashSet<String>();
		this.outputs = new HashSet<String>();
		this.payments = new HashMap<String, Double>();
		this.timeStamp = ts;
	}
	
	public String toString(){
		StringBuilder strBuild = new StringBuilder();
		strBuild.append(this.timeStamp.toString());
		strBuild.append("\nin\n");
		for(String tInput : this.inputs){
			strBuild.append(tInput + "\n");
		}
		strBuild.append("out\n");
		for(String tOutput : this.outputs){
			strBuild.append(tOutput + "\n");
		}
		return strBuild.toString();
	}
	
	public boolean cotainsAnyAsOutput(Set<String> keys){
		for(String tKey: keys){
			if(this.containsOutput(tKey)){
				return true;
			}
		}
		return false;
	}
	
	public boolean containsAnyAsInput(Set<String> keys){
		for(String tKey: keys){
			if(this.containsInput(tKey)){
				return true;
			}
		}
		return false;
	}
	
	public boolean containsOutput(String key){
		return this.outputs.contains(key);
	}
	
	public boolean containsInput(String key){
		return this.inputs.contains(key);
	}

	public void addInput(String key){
		this.inputs.add(key);
	}
	
	public void addOutput(String key, double payment){
		this.outputs.add(key);
		this.payments.put(key, payment);
	}
	
	public Set<String> getInputs() {
		return this.inputs;
	}

	public Set<String> getOuputs() {
		return this.outputs;
	}
	
	public double getPayment(String key){
		if(!this.payments.containsKey(key)){
			throw new RuntimeException("No such output key");
		}
		
		return this.payments.get(key);
	}

	public Date getTimestamp() {
		return this.timeStamp;
	}

	public int hashCode() {
		return this.inputs.hashCode() + this.outputs.hashCode() + this.timeStamp.hashCode();
	}

	public boolean equals(Object rhs) {
		if (!(rhs instanceof FinderResult)) {
			return false;
		}

		FinderResult rhsFinder = (FinderResult) rhs;
		if (!this.timeStamp.equals(rhsFinder.timeStamp)) {
			return false;
		}

		if (this.inputs.size() != rhsFinder.inputs.size() || this.outputs.size() != rhsFinder.outputs.size()) {
			return false;
		}

		return this.inputs.containsAll(rhsFinder.inputs) && this.outputs.containsAll(rhsFinder.outputs);
	}
}
