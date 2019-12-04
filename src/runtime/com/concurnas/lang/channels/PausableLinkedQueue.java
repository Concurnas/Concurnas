package com.concurnas.lang.channels;

import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

import com.concurnas.bootstrap.runtime.cps.Fiber;
import com.concurnas.lang.Shared;
import com.concurnas.runtime.bootstrapCloner.Cloner;

@Shared
public class PausableLinkedQueue<X> {

	private volatile LinkedBlockingQueue<X> queue = new LinkedBlockingQueue<X>();

	private volatile HashSet<Fiber> waiters = new HashSet<Fiber>();
	
	public X get(){
		
		X got = queue.poll();
		if(got == null) {
			Fiber currentFiber = Fiber.getCurrentFiberWithCreate();
			while(got == null){
				synchronized(waiters){
					waiters.add(currentFiber);
				}
				Fiber.pause();
				synchronized(waiters){
					waiters.remove(currentFiber);
				}
				got = queue.poll();
			}
			
		}
		
		return Cloner.cloner.clone(got);
	}

	public void add(X xx){
		queue.add(xx);
		wakeup();
	}

	private void wakeup(){
		synchronized(waiters){
			for(Fiber f : waiters){
				Fiber.wakeup(f, null);
			}
		}
	}
}
