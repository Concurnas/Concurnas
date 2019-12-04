package com.concurnas.compiler.bytecode;

import com.concurnas.bootstrap.runtime.cps.Fiber;

public class MyTest {

	public MyTest(){}
	
	public static String doings(Fiber fib){
		short g = 2;
		return "" + g;
	}
	
}
