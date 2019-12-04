package com.concurnas.bootstrap.runtime.cps;

import java.util.concurrent.LinkedBlockingQueue;

import com.concurnas.bootstrap.lang.Lambda.Function1;
import com.concurnas.bootstrap.runtime.transactions.Transaction;

public class Worker extends Thread{

	public final Scheduler scheduler;
	
	private class Job{
		public  Iso iso;
		public  Transaction trans;
		public  TerminationState term;
		public  boolean isFirst;
		
		public Job(Iso iso, Transaction trans, boolean isFirst){
			this.iso = iso;
			this.trans = trans;
			this.isFirst = isFirst;
		}
		
		public Job(TerminationState term){
			this.term=term;
		}
	}
	
	private final LinkedBlockingQueue<Job> workQueue = new LinkedBlockingQueue<Job>();
	private final boolean dedicated;
	
	public Worker(Scheduler scheduler, String id, boolean dedicated){
        super(id);
		super.setDaemon(true);
		this.scheduler = scheduler;
		this.dedicated = dedicated;
	}
	
	private volatile TerminationState term = null;//volatile because we always need to check the last one
	
	private Iso currentIso;
	
	@Override
	public void run() {
		while(true){//TODO: when should this terminate?
			try {
				Job toRun = workQueue.take();
				Iso iso = toRun.iso;
				currentIso = iso;
				if(null == iso){
					term = toRun.term;
				}
				else {
					if(!iso.done){
						try{
							iso.execute(toRun.trans, toRun.isFirst);
							//TODO: work stealer
							//TODO: note, when stealing tasks must register self as being new owner of iso - bucketing of requests in some manor will be requied most likely
							//TODO: subtask splitter - e.g. for long running stuff to share processor
						}
						catch(VerifyError ve){
							ve.printStackTrace();
							throw ve;//TODO: have special catch on runtime/compiler errors?
						}
						catch(Throwable e){
							Function1<Throwable, Void> ieh = iso.getExceptionHandler();
							ieh.apply(e);
						}
					}
					
					if(this.dedicated && iso.done) {
						this.scheduler.removeDedicatedWorker(this);
						return;
					}
				}
				
			} catch(InterruptedException ignore) {
				//ignore.printStackTrace();
			}
			catch (Throwable e) {
				e.printStackTrace();
			}
			currentIso = null;
			
			if(term!=null){
				if(workQueue.isEmpty()){//we wait for the queue to empty out before finnishing
					term.setTermianted();
					if(!term.isCancel()){//if its a real termination and not just a cancel then we can terminate the thread
						term=null;
						return;
					}
					term=null;
				}
				
			}
		}
	}

	public void terminate(TerminationState term){
		workQueue.add(new Job(term));
	}
	public void createTask(Iso iso){
		if(term==null){
			workQueue.add(new Job(iso, null, false));
		}
	}
	
	public void wakeup(Iso iso) {
		//on unpause/resumption
		if(term==null){
			workQueue.add(new Job(iso, null, false));
		}
	}
	
	public void notif(Iso iso, Transaction trans, boolean isFirst) {
		if(term==null){
			workQueue.add(new Job(iso, trans, isFirst));
		}
	}
	
	public String getStatus() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getName());
		if(this.term!= null) {
			sb.append(" Termination State: [");
			sb.append(this.term!= null);
			sb.append("]: ");
		}else {
			if(null == currentIso && this.workQueue.isEmpty()) {
				return null;
			}
			
			sb.append(": ");
		}
		
		if(null != currentIso) {
			sb.append("current: " + currentIso.description + " ");
		}
		
		Job next = this.workQueue.peek();
		if(next == null) {
			sb.append("empty");
		}else {
			sb.append("[size: " + this.workQueue.size() + "] ");
			sb.append("next task: " + (next.iso == null ? "TERMINATION" : next.iso.description));
		}
		
		return sb.toString();
	}
}
