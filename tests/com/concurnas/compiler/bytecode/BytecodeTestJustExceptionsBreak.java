package com.concurnas.compiler.bytecode;

import static org.junit.Assert.*;

import org.junit.Test;

import com.concurnas.compiler.scopeAndType.ScopeAndTypeTests;

public class BytecodeTestJustExceptionsBreak {

	@Test
	public void testExceptionsBreak() throws Throwable {
		new BytecodeTests().testExceptionsBreak();
	}

}
