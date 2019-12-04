package com.concurnas.lang;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ TYPE, FIELD, PARAMETER, TYPE_PARAMETER, TYPE_USE })
public @interface NoNull {
	public static enum When{ALWAYS, NEVER, MAYBE;}
	When when() default When.ALWAYS;
}
