package com.concurnas.lang.offheap.storage;

import com.concurnas.lang.Uninterruptible;

@Uninterruptible
public class OffHeapRAM<T> extends OffHeapPutGettable<T> {

	public OffHeapRAM(long bytesize) {
		super.setCapacity(bytesize);
	}

}
