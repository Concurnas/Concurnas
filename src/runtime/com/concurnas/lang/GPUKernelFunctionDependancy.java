package com.concurnas.lang;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(TYPE)
public @interface GPUKernelFunctionDependancy {
	public int dims();
	public String dclass();
	public String name();
	public String signature();
	public String globalLocalConstant();//along with signature used to identify function to call
	public String inout();//along with signature used to identify function to call
}
