package com.concurnas.bootstrap.runtime.cps;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import com.concurnas.bootstrap.lang.Lambda.Function1;
import com.concurnas.bootstrap.runtime.ref.Ref;
import com.concurnas.bootstrap.runtime.transactions.LocalTransaction;
import com.concurnas.bootstrap.runtime.transactions.Transaction;

public abstract class IsoTrigger extends Iso {

	protected boolean initDone = false;
	final IsoTaskNotifiable ntask;
	public Transaction lastNotifiedUpon = null;
	private ArrayList<Transaction> notificationsReceivedDuringPaused = new ArrayList<Transaction>(); 
	private final boolean every;
	private final boolean setCompleteAtStart;
	
	public IsoTrigger(Worker worker, String description, IsoTaskNotifiable ntask, final boolean every, final boolean setCompleteAtStart) {
		super(worker, description);
		this.ntask = ntask;
		this.every = every;
		this.setCompleteAtStart = setCompleteAtStart;
	}
	
	private Function1<Throwable, Void> initExceptionHandler;
	
	public void setInitExceptionHandler(Function1<Throwable, Void> initExceptionHandler) {
		this.initExceptionHandler = initExceptionHandler;
	}
	
	public Function1<Throwable, Void> getInitExceptionHandler(){
		return this.initExceptionHandler;
	}
	
	@Override
	public Function1<Throwable, Void> getExceptionHandler(){
		return !initDone?this.initExceptionHandler:this.exceptionHandler;
	}
	
	private LinkedHashSet<Ref<?>> toProc;
	
	protected void _runExecute(Transaction trans, boolean isFirst) throws Throwable{//gets replaced during install process
		if(!initDone){
			if(toProc == null && this.every){
				toProc = new LinkedHashSet<Ref<?>>();//set initial stuff to process
				//remember, we re-enter this code if the init pauses, so we must make this global and set it only once
			}
			super.fiber.down();
			this.ntask.init(toProc);//first call does pre calls and initalization
			initDone = super.fiber.end();
			if(!initDone){
				return;
			}
			
			super.fiber.reset();
			//all later calls are in response to notifications
			

			if(this.every){//add notifications for initial subscription. BEFORE we mark onchange as completed startup!
				Transaction transx = new LocalTransaction(toProc);
				Fiber.notif(this.fiber, transx, true, null);
			}
			
			if(setCompleteAtStart){
				//super.fiber.down();
				super.fiber.down();
				ntask.getIsInitCompleteFlag().set(true);//mark as completed startup
			}

			super.fiber.reset();
			
			return;
		}
		
		//can get notified when puased
		boolean isDone=false;
		if(null == trans){//wakeup - no notification so wakeup as normal using last thing
			trans = lastNotifiedUpon;
		}
		else{//its a notification
			if(super.fiber.isPausing){
				//paused so queue this for later
				notificationsReceivedDuringPaused.add(trans);
				return;
			}
			lastNotifiedUpon = trans;
			super.fiber.down();
			trans.onNotify();
		}
		super.fiber.down();
		isDone = this.ntask.apply(trans, isFirst);//add in fiber at runtime - we also do weird trick with mapping to Function0 on the ISOAugmentor
		super.fiber.up();
		
		if(isDone){//correct?
			super.done=true;
			if(!setCompleteAtStart){
				super.fiber.down();
				ntask.getIsInitCompleteFlag().set(true);
			}
			
    		this.fiber.down();
    		this.ntask.teardownGlobals();//add in fiber at runtime
    		
    		this.fiber.down();
    		this.ntask.cleanup();
			
    		return;//done!
    	}
		else if(!super.fiber.isPausing && !notificationsReceivedDuringPaused.isEmpty()){
			//we are still not done, and we are not paused, replay these notificaitons
			while(!notificationsReceivedDuringPaused.isEmpty()){
				Fiber.notif(this.fiber, notificationsReceivedDuringPaused.remove(0), false, null);
			}
		}
		
		return;//carry on
    }

}
