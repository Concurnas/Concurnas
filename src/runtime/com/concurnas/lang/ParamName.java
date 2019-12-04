package com.concurnas.lang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value=ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParamName {
	String name();
	boolean hasDefaultValue() default false;
	boolean isVararg() default false;
}
