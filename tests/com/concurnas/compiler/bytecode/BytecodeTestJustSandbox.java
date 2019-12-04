package com.concurnas.compiler.bytecode;

import org.junit.Test;

import com.concurnas.compiler.visitors.ScopeAndTypeChecker;

public class BytecodeTestJustSandbox {

	@Test
	public void testSandbox() throws Throwable {

		/*
		 * try { System.err.println("cl: " +
		 * DeleteOnUnusedReturn.class.getClassLoader()); }catch(NoClassDefFoundError
		 * ndcf) { ndcf.getCause().printStackTrace(); }
		 */

		Object x = ScopeAndTypeChecker.const_actor;
		
		new BytecodeTests().testBytecodeSandbox();
		// new BytecodeTests().testBytecodeSandbox();
	}
}
