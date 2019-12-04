package com.concurnas.bootstrap.runtime.cps;

public class IsoEvery extends IsoTrigger {
	public IsoEvery(Worker worker, IsoTaskNotifiable ntask, String description ) {
		super(worker, description, ntask, true, true);
	}
}
