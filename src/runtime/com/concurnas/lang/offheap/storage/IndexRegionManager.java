package com.concurnas.lang.offheap.storage;

import com.concurnas.lang.offheap.util.OffHeapRWLock;

public interface IndexRegionManager {
	public OffHeapRWLock[][] getRegionLocks();
	public long[] getRegionMapOffsets();
	public int[] getRegionMapSizes();
}
