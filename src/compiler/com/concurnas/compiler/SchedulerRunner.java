package com.concurnas.compiler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.concurnas.compiler.visitors.Utils;
import com.concurnas.runtime.ConcurnasClassLoader;

public class SchedulerRunner{
	private final Class<?> schedulerCls;
	private final Method schedTskMethod;
	private final Method schedulerTerminate;
	private Object shceduler;
	private final String forwhat;
	
	public SchedulerRunner(ConcurnasClassLoader localisedClasslaoder, String forwhat) throws Throwable {
		this.forwhat = forwhat;
		schedulerCls = localisedClasslaoder.loadClass("com.concurnas.bootstrap.runtime.cps.Scheduler");
		
		schedTskMethod = Utils.getMethod(schedulerCls, "public void com.concurnas.bootstrap.runtime.cps.Scheduler.scheduleTask(com.concurnas.bootstrap.runtime.cps.AbstractIsoTask,java.lang.String) throws java.lang.Throwable", 2);
		
		schedulerTerminate = Utils.getMethod(schedulerCls, "public void com.concurnas.bootstrap.runtime.cps.Scheduler.terminate()", 0);
		
		shceduler = schedulerCls.newInstance();
	}
	
	public void invokeScheudlerTask(Object task) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		schedTskMethod.invoke(shceduler, task, forwhat);
	}
	
	public void stop() {
		try {
			schedulerTerminate.invoke(shceduler);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
