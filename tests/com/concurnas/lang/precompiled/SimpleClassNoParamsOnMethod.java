package com.concurnas.lang.precompiled;

import com.concurnas.lang.precompiled.AnnotationHelper.AnnotOneArg;

public class SimpleClassNoParamsOnMethod {
	public int foo(@AnnotOneArg(name="hi") int a, int b) {
		return a + 2;
	}
}
