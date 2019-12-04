package com.concurnas.bootstrap.runtime.cps;

/*
 * Nasty copy paste from IsoNotifiable
 */
public class IsoAwait extends IsoTrigger {
	public IsoAwait(Worker worker, IsoTaskNotifiable ntask, String description ) {
		super(worker, description, ntask, true, false);
	}
}

