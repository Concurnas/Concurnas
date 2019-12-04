package com.concurnas.lang.offheap;

@SuppressWarnings("serial")
public class OffHeapOutOfMemoryError extends RuntimeException{

	public OffHeapOutOfMemoryError(String msg){
		super(msg);
	}
}
