package com.concurnas.runtime.channels;

import java.util.HashSet;

import com.concurnas.bootstrap.runtime.cps.Fiber;
import com.concurnas.runtime.bootstrapCloner.Cloner;

/**
 *	Priority 1 comes before priority 2 etc 
 */
public class PriorityQueue<X> {
	
	private java.util.PriorityQueue<QueueItem<X>> priorityToStuff = new java.util.PriorityQueue<QueueItem<X>>();
	protected HashSet<Fiber> waiters = new HashSet<Fiber>(); 
	
	private static class QueueItem<X> implements Comparable<QueueItem<X>>{
		public final X x;
		private final int priority;
		
		public QueueItem(int priority, X x){
			this.priority = priority;
			this.x = x;
		}
		
		@Override
		public int compareTo(QueueItem<X> o) {
			//1 - 2 =? -1 (so comes first)
			return this.priority - o.priority;
		}
		
		@Override
		public boolean equals(Object o){
			if(o instanceof QueueItem){
				QueueItem<X> buddy = (QueueItem<X>)o;
				return buddy.x.equals(x) && buddy.priority == priority;
			}
			return false;
		}
	}
	
	public void add(int priority, X x){
		synchronized(this){

			//System.err.println("add to actor q: " );
			//new RuntimeException("add").printStackTrace();
			
			priorityToStuff.add(new QueueItem<X>(priority, Cloner.cloner.clone(x)));
			if(!waiters.isEmpty()){
				for(Fiber f : waiters){//notify-all
					Fiber.wakeup(f, null);
				}
			}
		}
	}
	
	public X pop(){
		while(true){
			Fiber currentFiber = Fiber.getCurrentFiber();
			synchronized(this){
				if(!priorityToStuff.isEmpty()){
					return priorityToStuff.remove().x;
				}
				waiters.add(currentFiber);
			}
				
			Fiber.pause();//go away come back later
		}
	}
	
	//no peek yet
	
}
