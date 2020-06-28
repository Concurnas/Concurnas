package com.concurnas.lang;

import com.concurnas.runtime.ref.Local;

/**
 * This ref blocks on get if no value has been set, it does not pause
 * @author Jason
 *
 * @param <X>
 */
public class BlockingLocalRef<X> extends Local<X>{

	public BlockingLocalRef(Class<?>[] type) {
		super(type);
	}

	@Override
	public  X get() throws Throwable  {
		if(!this.isSet()) {
			synchronized(this) {
				while(!this.isSet()) {
					try {
						this.wait();
					} catch (InterruptedException e) {
					}
				}
			}
		}
		
		return super.get();
	}
	
	@Override
	public void set(X x) throws Throwable{
		synchronized(this) {
			super.set(x);
			this.notifyAll();
		}
	}
	
	@Override
	public void setException(Throwable e){
		synchronized(this) {
			super.setException(e);
			this.notifyAll();
		}
	}
	
	
	
}
