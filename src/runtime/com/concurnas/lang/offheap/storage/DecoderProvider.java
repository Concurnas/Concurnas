package com.concurnas.lang.offheap.storage;

import com.concurnas.bootstrap.lang.offheap.Decoder;

public interface DecoderProvider<T> {
	Decoder provide(OffHeapDecoder<T> dec);
}
