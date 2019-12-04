package com.concurnas.lang;

import org.jocl.cl_mem;

public interface GPUBuffer<Type> extends com.concurnas.lang.offheap.DMA {
	public cl_mem getBuffer();
}
