package com.concurnas.lang;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface TraitField {
	String fieldName();
	String fieldType();
	boolean isAbstract();
	String accessModifier();
}
