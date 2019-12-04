package com.concurnas.lang;

import java.lang.annotation.ElementType;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface GPUKernelFunction {
	public int dims();
	public String name();
	public String signature();
	public String source();
	public GPUKernelFunctionDependancy[] dependancies();

	public String globalLocalConstant();
	public String inout();
}
