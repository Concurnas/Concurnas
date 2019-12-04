package com.concurnas.lang.internal;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * NullStatus describes the nullability status of a method, constructor or field elements
 * For def myFunc(null1 String?, null2 String) String?
 * corresponds to: @NullStatus(nullStatus = [true, false, true])
 * 
 * Status array is populated only up to the final true value:
 * For def myFunc(null1 String?[]?, null2 int) String
 * corresponds to: @NullStatus(nullStatus = [true, true])
 * 
 * If no @NullStatus is present this indicates that no type in the signature is nullable:
 * 
 * For def myFunc(null1 String[], null2 int) String
 * corresponds to: no @NullStatus(nullStatus = [])
 * 
 * @author Jason
 *
 */

@Retention(RUNTIME)
@Target({ FIELD, METHOD, CONSTRUCTOR, TYPE })
public @interface NullStatus {
	public boolean[] nullable();
}
