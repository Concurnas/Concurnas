package com.concurnas.compiler;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.concurnas.compiler.bytecode.BytecodeTests;
import com.concurnas.compiler.util.Concurrent;
import com.concurnas.compiler.util.ConcurrentJunitRunner;


@RunWith(ConcurrentJunitRunner.class)
@Concurrent(threads = 4)
public class JustRefsAndTransBytecode {
	
	@Test
	public void testRefs() throws Throwable {
		new BytecodeTests().testRefs();
	}
	
	@Test
	public void testComplexRefs() throws Throwable {
		new BytecodeTests().testComplexRefs();
	}
	
	@Test
	public void testTrans() throws Throwable {
		new BytecodeTests().testTrans();
	}

	@Test
	public void testBigONes() throws Throwable {//quad core primary pc so may as well get 100% use with this additional test
		new BytecodeTests().testTheBigByteCodeTest();
	}
	
	@Test
	public void testJustOnChange() throws Throwable {
		new BytecodeTests().testOnChange();
	}
	
}
