package com.concurnas.runtime.cps;

import com.concurnas.bootstrap.runtime.CopyTracker;
import com.concurnas.bootstrap.runtime.cps.IsoTask;
import com.concurnas.bootstrap.runtime.ref.Ref;

public interface ISOExecutor<ResultType extends Ref<?>> {
	public void execute(IsoTask<ResultType> task, String desc, CopyTracker tracker);
}
