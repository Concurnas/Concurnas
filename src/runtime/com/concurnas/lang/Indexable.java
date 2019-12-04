package com.concurnas.lang;

public interface Indexable<Value> {
	public Value get(long index);
	public Value out(long index, Value value);
	
}
