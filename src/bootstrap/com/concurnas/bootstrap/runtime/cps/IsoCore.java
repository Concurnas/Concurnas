package com.concurnas.bootstrap.runtime.cps;

import com.concurnas.bootstrap.runtime.transactions.Transaction;

public class IsoCore extends Iso {
    protected AbstractIsoTask func;
    
    public boolean waitingForNotification = false;
    
    public IsoCore(Worker worker, AbstractIsoTask func, String description){//gods of ioc will be offended
    	super(worker, description);
    	this.func = func;
    }
    
    protected void _runExecute(Transaction unused, boolean alsoUnused) throws Throwable{
    	this.fiber.down();
    	this.func.apply();//add in fiber at runtime
    	
    	boolean isDone = this.fiber.end();
    	if(isDone){
    		super.done = isDone;

    		this.fiber.down();
        	this.func.teardownGlobals();//add in fiber at runtime
    		this.func.getIsInitCompleteFlag().set(true);//signal
    	}
    }
}
