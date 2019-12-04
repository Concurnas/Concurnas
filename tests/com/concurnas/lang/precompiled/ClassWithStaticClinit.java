package com.concurnas.lang.precompiled;

import com.concurnas.bootstrap.runtime.cps.Fiber;

//refernces a ref
public class ClassWithStaticClinit {
	public static Fiber fib = Fiber.getCurrentFiber();
	
	public static boolean isFiberNotNull(){
		return fib!=null;
	}
	
	public static Fiber getTheCurrentFiber(){
		return Fiber.getCurrentFiber();
	}
}
