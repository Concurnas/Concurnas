package com.concurnas.bootstrap.runtime.cps;

import java.util.concurrent.TimeUnit;

import com.concurnas.bootstrap.runtime.transactions.Transaction;

public class IsoCore extends Iso {
    protected final AbstractIsoTask func;
    
    public boolean waitingForNotification = false;

    
    public IsoCore(Worker worker, AbstractIsoTask func, String description){//gods of ioc will be offended
    	super(worker, description);
    	this.func = func;
    	//System.err.println(String.format("i'm an IsoCore and ive been created like this iso: %s, worker: %s, fiber: %s", System.identityHashCode(this),  System.identityHashCode(worker), System.identityHashCode(fiber) ));
    }
    
    
    protected void _runExecute(Transaction unused, boolean alsoUnused){
    	//gets replaced during install process

    	this.fiber.down();
    	//this.fiber.printState();
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
