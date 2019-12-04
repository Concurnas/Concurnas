package com.concurnas.lang.offheap.storage;


public class SizeofProvider {

	public static int sizeof(Object xxx){
		OffHeapEncoder<Object> provider = new OffHeapEncoder<Object>(null,null);
		return provider.sizeof(xxx);
	}
	
}
