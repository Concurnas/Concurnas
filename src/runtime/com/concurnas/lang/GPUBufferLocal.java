package com.concurnas.lang;

public interface GPUBufferLocal<Type> extends GPUBuffer<Type> {
	public int getByteSize();
}
