package com.concurnas.bootstrap.runtime.cps;

/*
 * onchange()
 */
public class IsoNotifiable extends IsoTrigger {
	public IsoNotifiable(Worker worker, IsoTaskNotifiable ntask, String description ) {
		super(worker, description, ntask, false, true);
	}
}
