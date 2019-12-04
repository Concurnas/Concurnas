package com.concurnas.lang.offheap;

import com.concurnas.lang.offheap.storage.OffHeapPutGettable;

public class ManagedOffHeapObject<T> implements OffHeapObject<T> {
	private final long address;
	private final OffHeapPutGettable<T> manager;

	public ManagedOffHeapObject(final long address, final OffHeapPutGettable<T> manager){
		this.address = address;
		this.manager = manager;
	}
	
	@Override
	public long getAddress(){
		return this.address;
	}
	
	@Override
	public OffHeapPutGettable<T> getManager(){
		return this.manager;
	}
	
	@Override
	public T get(){
		return this.manager.get(this);
	}
	
	public void delete(){
		finalize();
	}	
	
	@Override
	public void invalidate(){
	}
	
	@Override
	public void finalize(){
		//delete();
		manager.delete(this);
	}
}
