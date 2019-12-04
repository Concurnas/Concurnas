package com.concurnas.compiler.bytecode;


import static org.junit.Assert.assertNotSame;

import org.junit.Test;

import com.concurnas.bootstrap.runtime.CopyTracker;
import com.concurnas.runtime.bootstrapCloner.Cloner;


public class BytecodeTestJustCopier {

	@Test
	public void testCopierFunc() throws Throwable {
		new BytecodeTests().testCopierFunc();
	}

	public static class MyExcep extends Exception{
		public String xxx ="harry";
		
		public MyExcep(){
			super("exception");
		}
	}
	
	public final class A {
		
	}
	
	@Test
	public void testClonerFunc() throws Throwable {
		CopyTracker tracker = new CopyTracker();
		MyExcep ee = new MyExcep();
		MyExcep cloned = (MyExcep)Cloner.cloner.clone(tracker, ee, null);
		
		assertNotSame(ee, cloned);
		assert(ee.xxx != cloned.xxx);//diff objects!
		//new BytecodeTests().testBytecodeSandbox();
	}
	
}
