package data;

import java.util.HashMap;

public class TimeRotatingHashMap<K, V> implements Runnable {

	private HashMap<K, V> currentGen;
	private HashMap<K, V> oldGen;

	private long cleanIntervalMS;
	private Thread myThread;

	public static final long DEFAULT_CLEAN_INTERVAL_MS = 600000;

	public TimeRotatingHashMap() {
		this(TimeRotatingHashMap.DEFAULT_CLEAN_INTERVAL_MS);
	}

	public TimeRotatingHashMap(long cleanInterval) {
		this.currentGen = new HashMap<K, V>();
		this.oldGen = new HashMap<K, V>();
		this.cleanIntervalMS = cleanInterval;

		this.myThread = new Thread(this);
		this.myThread.setDaemon(true);
		this.myThread.setName("Rot Hash Cleaner");
		this.myThread.start();
	}

	public void run() {

		try {
			while (true) {
				Thread.sleep(this.cleanIntervalMS);
				
				synchronized(this){
					this.oldGen = this.currentGen;
					this.currentGen = new HashMap<K, V>();
				}
			}
		} catch (InterruptedException e) {
			//Currently do nothing, just end
		}
	}

	public Thread getCleanerThreadRef() {
		return this.myThread;
	}

	public void put(K theKey, V theValue){
		synchronized(this){
			if(this.oldGen.containsKey(theKey)){
				this.oldGen.remove(theKey);
			}
			this.currentGen.put(theKey, theValue);
		}
	}
	
	public V get(K theKey){
		V retObj = null;
		synchronized(this){
			if(this.currentGen.containsKey(theKey)){
				retObj = this.currentGen.get(theKey);
			}else if(this.oldGen.containsKey(theKey)){
				retObj = this.oldGen.get(theKey);
			}
		}
		
		return retObj;
	}
	
	public boolean hasKey(K theKey){
		boolean retVal = false;
		synchronized(this){
			retVal = this.currentGen.containsKey(theKey) || this.oldGen.containsKey(theKey);
		}
		return retVal;
	}
}
