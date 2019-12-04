package com.concurnas.bootstrap.runtime.ref;

public interface DirectlyGettable<X> extends Ref<X> {

	public X getPrevious();//?
	public X get();
	public X get(boolean withNoWait);
	public X getNoWait();

	public X last();//TODO: remove?
	public X last(boolean withNoWait);
	public X lastNoWait();
}
