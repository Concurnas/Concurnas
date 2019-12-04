package com.concurnas.bootstrap.runtime.cps;

import com.concurnas.bootstrap.runtime.ref.Ref;

public abstract class IsoTask<ResultType extends Ref<?>> extends AbstractIsoTask{

	public IsoTask(Class<?> type) {
		super(type);
	}
	
	public ResultType getResultRef() {
		return null;
	}
	
	@Override
	public abstract Void apply() ;
}
