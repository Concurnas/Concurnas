package com.concurnas.compiler.returnAnalysis;

import org.junit.Test;

import com.concurnas.compiler.scopeAndType.ScopeAndTypeTests;

public class ReturnAnalysisTests {

	@Test
	public void testReturnAnalysis() throws Exception {
		new ScopeAndTypeTests().atestReturns();
	}
}
