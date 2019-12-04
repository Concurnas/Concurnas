package com.concurnas.bootstrap.runtime;

public interface ReifiedType {
	/*Helps address issues with erasure */
	public Class<?>[] getType();
}
