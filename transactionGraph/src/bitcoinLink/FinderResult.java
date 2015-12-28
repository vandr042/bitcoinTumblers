package bitcoinLink;

import java.util.*;

public class FinderResult {

	private Set<String> inputs;
	private Set<String> outputs;
	private Date timeStamp;

	public FinderResult(Date ts) {
		this.inputs = new HashSet<String>();
		this.outputs = new HashSet<String>();
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

	public void addInput(String key){
		this.inputs.add(key);
	}
	
	public void addOutput(String key){
		this.outputs.add(key);
	}
	
	public Set<String> getInputs() {
		return this.inputs;
	}

	public Set<String> getOuputs() {
		return this.outputs;
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
