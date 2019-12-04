package com.concurnas.lang.precompiled;

import java.util.HashSet;

import com.concurnas.bootstrap.runtime.CopyTracker;
import com.concurnas.bootstrap.runtime.cps.Fiber;
import com.concurnas.bootstrap.runtime.cps.IsoTask;

public abstract class BustedIsoTask extends IsoTask{

	public BustedIsoTask() {
		super(null);
	}

	@Override
	public Void apply() {
		return null;
	}

	@Override
	public HashSet<String> setupGlobals(Fiber isoFiber, CopyTracker tracker) {
		return new HashSet<String>();
	}

	@Override
	public HashSet<String> setupGlobals(Fiber isoFiber, CopyTracker tracker, Fiber parent) {
		return new HashSet<String>();
	}
}
