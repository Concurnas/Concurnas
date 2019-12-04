package com.concurnas.lang.precompiled;

import com.concurnas.bootstrap.runtime.cps.Fiber;
import com.concurnas.bootstrap.runtime.cps.IsoTask;

public abstract class BustedIsoTask2 extends IsoTask{

	public BustedIsoTask2() {
		super(null);
	}

	@Override
	public Void apply() {
		return null;
	}

	@Override
	public void teardownGlobals() {
		//attempting to be too clever
	}

}
