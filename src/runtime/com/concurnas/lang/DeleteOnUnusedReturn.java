package com.concurnas.lang;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Used to indicate the caller should automatically call the delete method on the returend object
 * if it is not used.
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface DeleteOnUnusedReturn {

}
