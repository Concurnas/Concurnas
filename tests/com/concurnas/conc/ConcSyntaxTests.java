package com.concurnas.conc;

import java.io.File;
import java.util.Arrays;

import org.junit.Test;


import junit.framework.TestCase;

public class ConcSyntaxTests{

	@Test
	public void longerExample() {
		Conc concc = new Conc("-cp /work;/nonExist.class;/work/myfile.class;/work/thing.jar /work/MyFirstClass.class", null);
		String expect = "-cp /work"+File.pathSeparator+"/nonExist.class"+File.pathSeparator+"/work/myfile.class"+File.pathSeparator+"/work/thing.jar /work/MyFirstClass.class";
		
		TestCase.assertEquals(expect, ""+concc.getConcInstance());
	}
	
	@Test
	public void withCmdLineArgs() {
		Conc concc = new Conc("-s myJar.jar my args 2 'hi there'", null);
		String expect = "-s myJar.jar my args 2 hi there";
		ConcInstance got = concc.getConcInstance();
		TestCase.assertEquals(expect, ""+got);
		TestCase.assertEquals("[my, args, 2, hi there]", ""+Arrays.toString(got.cmdLineArgs));
	}
	
	
	
	@Test
	public void testCommandLineSmall() {
		Conc concc = new Conc("-s myJar.jar", null);
		String expect = "-s myJar.jar";
		
		TestCase.assertEquals(expect, ""+concc.getConcInstance());
	}
	
	
	@Test
	public void testCommandLineSmallBroken() {
		Conc concc = new Conc("-asdasd", null);
		
		TestCase.assertTrue(concc.doit().startsWith("Usage:"));
	}
	
	@Test
	public void testCommandLineErr() {
		Conc concc = new Conc("-options ", null);
		TestCase.assertNull(concc.getConcInstance());
	}
	
	
	@Test
	public void testHelpme() {
		Conc concc2 = new Conc("--help", null);
		String expect = Conc.helpMeErrorMsg;
		
		String got = concc2.doit();
		System.out.println(got);
		TestCase.assertEquals(expect, got);
	}
	
	@Test
	public void repl() {
		Conc conc = new Conc("-bc -werror", null);
		
		TestCase.assertEquals("-werror -bc", ""+conc.getConcInstance());
	}
}
