package com.concurnas.lang.offheap.storage;

import com.concurnas.lang.Uninterruptible;

@Uninterruptible
public class OffHeapMapRAM<K,V> extends OffHeapMap<K,V>{

	public OffHeapMapRAM(long bytesize) {
		super(bytesize);
	}
}
