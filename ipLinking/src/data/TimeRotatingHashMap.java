package data;

import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class TimeRotatingHashMap<K, V> {

	private HashMap<K, V> currentGen;
	private HashMap<K, V> oldGen;

	private long lastClean;
	private long cleanIntervalMS;

	public static final long DEFAULT_CLEAN_INTERVAL_MS = 600000;

	public TimeRotatingHashMap() {
		this(TimeRotatingHashMap.DEFAULT_CLEAN_INTERVAL_MS);
	}

	public TimeRotatingHashMap(long cleanInterval) {
		this.currentGen = new HashMap<K, V>();
		this.oldGen = new HashMap<K, V>();
		this.lastClean = System.currentTimeMillis();
		this.cleanIntervalMS = cleanInterval;
	}

	public void put(K theKey, V theValue) {
		/*
		 * Rotate generations if we need to
		 */
		long time = System.currentTimeMillis();
		if(time >= this.lastClean + this.cleanIntervalMS){
			this.oldGen = this.currentGen;
			this.currentGen = new HashMap<K, V>();
			this.lastClean = time;
		}
		
		/*
		 * Move into current gen if it exists in old gen (don't duplicate entries)
		 */
		if (this.oldGen.containsKey(theKey)) {
			this.oldGen.remove(theKey);
		}
		this.currentGen.put(theKey, theValue);
	}

	public V get(K theKey) {
		V retObj = null;
		if (this.currentGen.containsKey(theKey)) {
			retObj = this.currentGen.get(theKey);
		} else if (this.oldGen.containsKey(theKey)) {
			retObj = this.oldGen.get(theKey);
		}

		return retObj;
	}

	public boolean containsKey(K theKey) {
		return this.currentGen.containsKey(theKey) || this.oldGen.containsKey(theKey);
	}
	
	public Set<K> keySet(){
		HashSet<K> retSet = new HashSet<K>();
		retSet.addAll(this.currentGen.keySet());
		retSet.addAll(this.oldGen.keySet());
		return retSet;
	}
}
