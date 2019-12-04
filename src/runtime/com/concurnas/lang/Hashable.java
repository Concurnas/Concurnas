package com.concurnas.lang;

public interface Hashable<Key, Value> {

	public Value get(Key key);
	public Value get(Key key, Value defaultvalue);
	public void put(Key key, Value value);
}
