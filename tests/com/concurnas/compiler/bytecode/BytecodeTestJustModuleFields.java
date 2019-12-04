package com.concurnas.compiler.bytecode;

import static org.junit.Assert.*;

import org.junit.Test;

import com.concurnas.compiler.scopeAndType.ScopeAndTypeTests;

public class BytecodeTestJustModuleFields {

	@Test
	public void testModuleFields() throws Throwable {
		new BytecodeTests().testModuleFields();
	}

}
