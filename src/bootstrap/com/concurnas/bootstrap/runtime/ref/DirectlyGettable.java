package com.concurnas.bootstrap.runtime.ref;

public interface DirectlyGettable<X> extends Ref<X> {

	public X getPrevious();//?
	public X get() throws Throwable;
	public X get(boolean withNoWait) throws Throwable;
	public X getNoWait() throws Throwable;

	public X last() throws Throwable;//TODO: remove?
	public X last(boolean withNoWait) throws Throwable;
	public X lastNoWait() throws Throwable;
}
