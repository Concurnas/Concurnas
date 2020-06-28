package com.concurnas.runtime.cps;

import com.concurnas.bootstrap.runtime.cps.IsoTrigger;
import com.concurnas.bootstrap.runtime.ref.Ref;

public class IsoRegistrationSetAsyncChild extends IsoRegistrationSet {

	private final IsoRegistrationSetAsync parent;
	public final int order;
	
	public IsoTrigger isoTask;
	
	public IsoRegistrationSetAsyncChild(final IsoRegistrationSetAsync parent, final int order){
		super(false, false);//TODO: is this correct? one shared betwee all?
		this.parent=parent;
		this.order = order;
	}
	
	@Override
	public boolean register(Ref<?> x) throws Throwable{
		parent.register(this, x);
		return super.register(x);
	}
	
	@Override
	public void unregister(Ref<?> x) throws Throwable{
		parent.unregister(this, x);
		super.unregister(x);
	}
	
}
