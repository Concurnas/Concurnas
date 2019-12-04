package com.concurnas.runtime;


public abstract class AbstractFrame {
	
	public int numMonitorsActive;
	public int stacklen;

	public abstract Value push(Value make);
	public abstract Value popWord();
	public abstract Value pop();
	public abstract Value getLocal(int local, int opcode);
	public abstract void popn(int i);
	public abstract int setLocal(int var, Value v1);
	public abstract void clearStack();
	public abstract int getStackLen();
	public abstract String stacktoString();
}
