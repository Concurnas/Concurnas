package com.concurnas.lang.offheap.storage;

import java.nio.ByteBuffer;

import com.concurnas.runtime.Pair;

public interface MallocProvider {
    public Pair<ByteBuffer, Long> malloc(int size);

	public ByteBuffer getBuffer(long address);

	public void free(long address);
}
