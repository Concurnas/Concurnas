package com.concurnas.bootstrap.runtime.cps;

public class TerminationState {
	private boolean terminated=false;
	private final boolean isCancel;
	
	public TerminationState(boolean isCancel) {
		this.isCancel =isCancel;
	}
	
	public synchronized void setTermianted(){
		this.terminated = true;
		this.notify();
	}
	
	public synchronized boolean hasTerminated() throws InterruptedException{
		while(!terminated){
			this.wait();
		}
		return terminated;
	}
	
	public boolean isCancel(){
		return isCancel;
	}
}
