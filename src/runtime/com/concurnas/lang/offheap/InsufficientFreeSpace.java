package com.concurnas.lang.offheap;

@SuppressWarnings("serial")
public class InsufficientFreeSpace extends RuntimeException {
	public InsufficientFreeSpace(String xxx){
		super(xxx);
	}
}
