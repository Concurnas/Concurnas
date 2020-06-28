package com.concurnas.bootstrap.runtime.ref;

public interface DirectlyArrayGettable<X> {
	public X[] get() throws Throwable;
	public X[] get(boolean withNoWait) throws Throwable;
	public X[] getNoWait() throws Throwable;
	
	public X get(int i) throws Throwable;
	public X get(int i, boolean withNoWait) throws Throwable;
	public X getNoWait(int i) throws Throwable;
}
