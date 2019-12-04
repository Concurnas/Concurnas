package com.concurnas.lang.offheap.storage;

import java.util.HashMap;
import java.util.HashSet;

import com.concurnas.bootstrap.lang.offheap.Decoder;
import com.concurnas.lang.offheap.storage.OffHeapTransformationalDecoder.SkipFieldInfo;
import com.concurnas.lang.offheap.util.FieldConverters.TypeConvert;

public class OffHeapTransformationalDecoderGennerator<T> implements DecoderProvider<T>{//like doing real oo programming

	private final int fieldCount;
	private final HashMap<Integer, SkipFieldInfo> missingFields; 
	private final HashSet<Integer> addedFields;
	private final HashMap<Integer, TypeConvert> castFields;
	
	public OffHeapTransformationalDecoderGennerator(int fieldCount, HashMap<Integer, SkipFieldInfo> missingFields, HashSet<Integer> addedFields, HashMap<Integer, TypeConvert> castFields) {
		this.fieldCount = fieldCount;
		this.missingFields = missingFields;
		this.addedFields = addedFields;
		this.castFields = castFields;
	}
		
	
	@Override
	public Decoder provide(OffHeapDecoder<T> dec) {
		return new OffHeapTransformationalDecoder<T>(dec, fieldCount,  missingFields,  addedFields,  castFields);
	}

	
	
}
