package com.concurnas.lang.offheap;

import com.concurnas.lang.Transient;
import com.concurnas.lang.offheap.storage.OffHeap;

@Transient
public interface OffHeapObject<T> {

	public abstract long getAddress();

	public abstract OffHeap getManager();

	public abstract T get();

	public abstract void delete();

	public abstract void invalidate();

	public abstract void finalize();

}