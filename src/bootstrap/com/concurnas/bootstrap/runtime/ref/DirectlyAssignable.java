package com.concurnas.bootstrap.runtime.ref;

public interface DirectlyAssignable<X> extends Ref<X>{
	public void set(X x);
}
