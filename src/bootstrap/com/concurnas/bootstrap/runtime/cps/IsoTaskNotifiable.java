package com.concurnas.bootstrap.runtime.cps;

import java.util.HashSet;
import java.util.LinkedHashSet;

import com.concurnas.bootstrap.runtime.CopyTracker;
import com.concurnas.bootstrap.runtime.ref.DefaultRef;
import com.concurnas.bootstrap.runtime.ref.Ref;
import com.concurnas.bootstrap.runtime.transactions.Transaction;

public abstract class IsoTaskNotifiable {

	public abstract void init(LinkedHashSet<Ref<?>> as) ;
	
	public abstract boolean apply(Transaction changed, boolean isFirst) ;
	
	public abstract HashSet<String> setupGlobals(Fiber isoFiber, CopyTracker tracker);
	public abstract HashSet<String> setupGlobals(Fiber isoFiber, CopyTracker tracker, Fiber parent);
	
	public abstract DefaultRef<Boolean> getIsInitCompleteFlag();
	
	public abstract void teardownGlobals();
	public abstract void cleanup();
}
