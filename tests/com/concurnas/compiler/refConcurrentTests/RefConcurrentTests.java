package com.concurnas.compiler.refConcurrentTests;

import java.io.File;
import org.junit.Test;

import com.concurnas.compiler.bytecode.BytecodeTests;

public class RefConcurrentTests {

	private final static String SrcDir = (new File("./tests/com/concurnas/compiler/refConcurrentTests")).getAbsolutePath() + File.separator;
	
	private BytecodeTests bct = new BytecodeTests();
	
	@Test
	public void cpsTests() throws Throwable {
		bct.testBigLoadFilesWithSrcDir(SrcDir + "refTests");
	}
}
