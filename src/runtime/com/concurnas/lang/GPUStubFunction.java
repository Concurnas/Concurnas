package com.concurnas.lang;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(METHOD)
public @interface GPUStubFunction {
	public String source() default "";
	public String[] sourcefiles() default {};
}
