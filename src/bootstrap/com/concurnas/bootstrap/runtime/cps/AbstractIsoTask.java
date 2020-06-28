package com.concurnas.bootstrap.runtime.cps;

import java.util.HashSet;

import com.concurnas.bootstrap.lang.Lambda.Function0;
import com.concurnas.bootstrap.runtime.CopyTracker;
import com.concurnas.bootstrap.runtime.ref.DefaultRef;

public abstract class AbstractIsoTask extends Function0<Void> {

	public AbstractIsoTask(Class<?> type) {
		super(null);
	}

	@Override
	public abstract Void apply();

	@Override
	public Object[] signature() {
		return null;
	}

	public abstract DefaultRef<Boolean> getIsInitCompleteFlag() throws Throwable;

	public abstract HashSet<String> setupGlobals(Fiber isoFiber, CopyTracker tracker);

	public abstract HashSet<String> setupGlobals(Fiber isoFiber, CopyTracker tracker, Fiber parent);

	public abstract void teardownGlobals();

	//for CObject...
	public boolean equals(Object an) {
		return super.equals(an);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getName());
		sb.append("@");
		String h = Integer.toHexString(hashCode());
		sb.append(h);
		return sb.toString();
	}

	public int hashCode() {
		return super.hashCode();
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
