package com.concurnas.bootstrap.runtime.cps;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.concurnas.bootstrap.lang.Lambda.Function1;

public class DefaultIsoExceptionHandler extends Function1<Throwable, Void> {

	public DefaultIsoExceptionHandler() {
		super(null);
	}

	@Override
	public Void apply(Throwable thro) {
		StringWriter errors = new StringWriter();
		errors.write("Unexpected exception thrown during iso operation. Cause:\n");
		thro.printStackTrace(new PrintWriter(errors));
		
		System.err.println(errors.toString());
		
		return null;
	}

	@Override
	public Object[] signature() {
		return null;
	}
}
