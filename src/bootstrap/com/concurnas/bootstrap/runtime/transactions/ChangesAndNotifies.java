package com.concurnas.bootstrap.runtime.transactions;

import java.util.HashSet;

import com.concurnas.bootstrap.runtime.cps.Fiber;

public class ChangesAndNotifies {
	public boolean changes;
	public HashSet<Fiber> notifies;
	
	public ChangesAndNotifies(boolean changes, HashSet<Fiber> notifies){
		this.changes= changes;
		this.notifies= notifies;
	}
}
