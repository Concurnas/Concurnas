package com.concurnas.lang;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Used to indicate that the method or class is able to manage its own state when accessed from multiple iso's/threads. I.e. it is Uninterruptible.
 * Where used at class level, all class methods are affected.
 * Use of this annotation will result in the affected methods (and code which they invoke) not being callable as a continuation.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Uninterruptible {

}
