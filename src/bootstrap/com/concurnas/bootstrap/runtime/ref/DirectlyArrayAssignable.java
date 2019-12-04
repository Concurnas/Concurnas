package com.concurnas.bootstrap.runtime.ref;

public interface DirectlyArrayAssignable<X> extends Ref<X>{
	public void set(X[] x);
}
