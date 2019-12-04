package com.concurnas.bootstrap.runtime.transactions;

import java.util.ArrayList;
import java.util.HashSet;

import com.concurnas.bootstrap.runtime.cps.Fiber;
import com.concurnas.bootstrap.runtime.ref.Ref;

public class TransactionHandler {
	
	private boolean isComplete = false;
	private boolean begun = false;

	private ArrayList<Ref<?>> refsToTrack = new ArrayList<Ref<?>>();
	
	public  final void begin(){//TODO: do these all need synchronized?
		if(begun){
			throw new RuntimeException("Transaction has already begun");
		}
		
		Fiber.getCurrentFiber().enterTransaction(this);
		
		begun = true;
	}
	
	
	public final boolean isComplete(){
		return isComplete;
	}
	
	public  final void comit(){
		Transaction trans = new LocalTransaction();
		HashSet<Fiber> toNotify = new HashSet<Fiber>();
		HashSet<Ref<?>> nochange = new HashSet<Ref<?>>();
		
		for(Ref<?> re : refsToTrack){
			ChangesAndNotifies ones = re.lock(this, trans);
			if(null==ones){
				//cannot lock, fail!
				//System.err.println("fail yuck");
				clearCache(false);
				return;
			}
			else if(!ones.changes){//oh, this ref was not actually changed at all, so was not locked
				nochange.add(re);
			}
			else{
				toNotify.addAll(ones.notifies);
			}
		}
		//nice, now we can comit all
		Fiber currentFiber = Fiber.getCurrentFiber();
		currentFiber.endTransaction(this);


		if(currentFiber.transactions.isEmpty()){//notification of change only when no longer inside nested transaction
			for(Fiber f : toNotify){
				Fiber.notif(f, trans, false);
			}
		}
		
		
		for(Ref<?> re : refsToTrack){
			if(!nochange.contains(re)){
				re.unlockAndSet();
			}
		}
		

		isComplete=true;

		clearCache(true);
	}
	
	private  final void clearCache(boolean sucsess){
		int refTrackSize = refsToTrack.size();
		for(Ref<?> re : refsToTrack){
			re.removeTransaction(this, sucsess);
		}
		
		refsToTrack = !isComplete?new ArrayList<Ref<?>>(refTrackSize):null;//nice trick to save a bit of allocation time
	}
	
	public final void abort(){
		if(!isComplete){
			//clear cache, unregister from fiber
			clearCache(false);
			Fiber.getCurrentFiber().endTransaction(this);
		}
	}
	
	public synchronized final void trackRef(Ref<?> refToTrack){
		refsToTrack.add(refToTrack);
	}
	
}
