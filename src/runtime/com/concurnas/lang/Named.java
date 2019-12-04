package com.concurnas.lang;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This Annotation is auto imported and can be used in conjunction with the inject
 * keyword in order to identify a dependency to qualify as part of a object graph
 * produced by an object provider. 
 *
 */
@Retention(RUNTIME)
public @interface Named {
  String value();
}