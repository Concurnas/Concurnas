package com.concurnas.compiler.bytecode;

import static org.junit.Assert.*;

import org.junit.Test;

import com.concurnas.compiler.scopeAndType.ScopeAndTypeTests;

public class BytecodeTestJustArrays {

	@Test
	public void testAllTheArrays() throws Throwable {
		new BytecodeTests().testAllTheArrays();
	}

}
