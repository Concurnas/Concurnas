package com.concurnas.bootstrap.runtime.cps;

import java.util.HashMap;

import com.concurnas.bootstrap.lang.Lambda.Function1;
import com.concurnas.bootstrap.runtime.transactions.Transaction;

public abstract class Iso extends CObject {
	public final Worker worker;
    public Fiber fiber;
    public String description;
    //for holding iso specific instances of objects (used in offheap)
    public HashMap<Object, Object> isoLocalCache = new HashMap<Object, Object>();
	protected boolean done = false;
	
    //public boolean waitingForNotification = false;

	public Iso(Worker worker, String description ) {
		this.worker = worker;
		this.description = description;
    	this.fiber = new Fiber(this);
    	//System.err.println("hi fib " + System.identityHashCode(this.fiber)) ;
	}
	
	/**
     * For use in exceptions where cannot figure out stack depth normally
     * @return the number of stack frames above _runExecute(), not including this fella
     */
    public int getStackDepth() {
        StackTraceElement[] stes;
        stes = new Exception().getStackTrace();
        int len = stes.length;
        for (int i = 0; i < len; i++) {
            StackTraceElement ste = stes[i];
            //System.err.println("level: " + ste.getMethodName());
            if (ste.getMethodName().equals("_runExecute") ){//TODO: and class name - check owner
                // discounting WorkerThread.run, Task._runExecute, and Scheduler.getStackDepth
            	return ste.getClassName().equals("com.concurnas.bootstrap.runtime.cps.IsoCore")?i-2:i;
            	//if we are running from iso core then knock off an extra couple of iso levels - i dunno why but this seems to work ok
            }
        }
        throw new RuntimeException("Code must be invoked via concurnas runtime");
    }
    
    protected abstract void _runExecute(Transaction triggeredOn, boolean isInitial);

	public void execute(Transaction triggeredOn, boolean isInitial) {
		 _runExecute(triggeredOn, isInitial);
	}

	protected Function1<Throwable, Void> exceptionHandler;
	
	public void setExceptionHandler(Function1<Throwable, Void> exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}
	
	public Function1<Throwable, Void> getExceptionHandler(){
		return this.exceptionHandler;
	}
}
