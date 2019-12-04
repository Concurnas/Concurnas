package com.concurnas.lang;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used to indicate that the method or class may not be copied between iso's
 * Where used at class level, all class methods are affected.
 * Use of this annotation will result in the affected methods and code which they call not being callable as continuations.
 * Though they themselves may interact with refs etc as normal
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Transient {

}
