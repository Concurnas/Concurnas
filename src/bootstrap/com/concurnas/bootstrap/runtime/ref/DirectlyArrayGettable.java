package com.concurnas.bootstrap.runtime.ref;

public interface DirectlyArrayGettable<X> {
	public X[] get();
	public X[] get(boolean withNoWait);
	public X[] getNoWait();
	
	public X get(int i);
	public X get(int i, boolean withNoWait);
	public X getNoWait(int i);
}
