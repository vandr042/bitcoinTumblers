package data;

import java.util.HashMap;

public class WaitMap<E> {

	private E nextExpiringKey = null;
	private Long nextExpireTime = null;
	private HashMap<E, Long> waitTimes = null;

	public WaitMap() {
		this.waitTimes = new HashMap<E, Long>();
		this.nextExpiringKey = null;
		this.nextExpireTime = Long.MAX_VALUE;
	}
	
	private void updateNextExpire(){
		this.nextExpireTime = Long.MAX_VALUE;
		this.nextExpiringKey = null;
		for(E tKey: this.waitTimes.keySet()){
			if(this.waitTimes.get(tKey) < this.nextExpireTime){
				this.nextExpireTime = this.waitTimes.get(tKey);
				this.nextExpiringKey = tKey;
			}
		}
	}

	public boolean deleteWait(E key) {
		/*
		 * Step 1, delete the key, if was never there to start with then we
		 * didn't update the nearest wait time
		 */
		if (this.waitTimes.remove(key) == null) {
			return false;
		}

		if (this.nextExpiringKey.equals(key)) {
			this.updateNextExpire();
			return true;
		}

		return false;
	}

	public boolean updateObject(E key, long newWait) {

		this.waitTimes.put(key, newWait);

		/*
		 * Handle checking if the nearest expiring key has changed
		 */
		if (nextExpiringKey == null) {
			this.nextExpireTime = newWait;
			this.nextExpiringKey = key;
			return true;
		} else if (this.nextExpireTime > newWait) {
			this.nextExpireTime = newWait;
			this.nextExpiringKey = key;
			return true;
		} else if (this.nextExpiringKey.equals(key) && newWait > this.nextExpireTime) {
			this.updateNextExpire();
			return !this.nextExpiringKey.equals(key);
		}

		return false;
	}
	
	public E popNext(){
		E holder = this.nextExpiringKey;
		this.deleteWait(holder);
		return holder;
	}

	public int getPendingWaits() {
		return this.waitTimes.size();
	}

	public long getNextExpire() {
		return this.nextExpireTime;
	}

}
