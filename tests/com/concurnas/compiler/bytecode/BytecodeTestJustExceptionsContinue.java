package com.concurnas.compiler.bytecode;

import static org.junit.Assert.*;

import org.junit.Test;

import com.concurnas.compiler.scopeAndType.ScopeAndTypeTests;

public class BytecodeTestJustExceptionsContinue {

	@Test
	public void testExceptionsContinue() throws Throwable {
		new BytecodeTests().testExceptionsContinue();
	}

}
