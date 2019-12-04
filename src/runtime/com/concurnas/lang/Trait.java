package com.concurnas.lang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Signifies that a interface is a Trait
 */
@Target(value=ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Trait {
	Class<?> nonTraitSuperclass() default Object.class;
	TraitField[] traitFields() default {};
}
