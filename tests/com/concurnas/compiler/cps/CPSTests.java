package com.concurnas.compiler.cps;

import java.io.File;
import org.junit.Test;

import com.concurnas.compiler.bytecode.BytecodeTests;

public class CPSTests {

	private final static String SrcDir = (new File("./tests/com/concurnas/compiler/cps")).getAbsolutePath() + File.separator;
	
	private BytecodeTests bct = new BytecodeTests();
	
	@Test
	public void cpsTests() throws Throwable {
		bct.testBigLoadFilesWithSrcDir(SrcDir + "cpsTests");
	}
}
