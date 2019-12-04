package com.concurnas.concc;

import org.junit.Test;


import junit.framework.TestCase;

public class ConccSyntaxTests{

	@Test
	public void testCommandLineProcAll() {
		Concc concc = new Concc(" -cp c:\\jars\\include.jar;c:\\code\\myClass.class -d c:/compiled -root c:\\work thing.conc thing2.conc c:\\work\\stuff[ thing.conc thing2.conc ] \"anoth[]er1\" 'anoth[]er2' ", null, null);
		String expect = "-cp c:\\jars\\include.jar;c:\\code\\myClass.class -d c:/compiled -root c:\\work thing.conc thing2.conc c:\\work\\stuff[thing.conc] c:\\work\\stuff[thing2.conc] \"anoth[]er1\" 'anoth[]er2'";
		
		TestCase.assertEquals(expect, ""+concc.getConccInstance());
	}
	
	@Test
	public void testCommandLineSmall() {
		Concc concc = new Concc("  thing.conc /some/dir another/dir/ ", null, null);
		String expect = "thing.conc /some/dir another/dir/";
		
		TestCase.assertEquals(expect, ""+concc.getConccInstance());
	}
	
	@Test
	public void testWErrors() {
		Concc concc = new Concc("-a -werror -jar output.jar thing.conc /some/dir another/dir/ ", null, null);
		String expect = "-werror -a -jar output.jar thing.conc /some/dir another/dir/";
		
		TestCase.assertEquals(expect, ""+concc.getConccInstance());
	}
	
	@Test
	public void testOutputJarImlyName() {
		Concc concc = new Concc("-clean -a -werror -jar output thing.conc /some/dir another/dir/ ", null, null);
		String expect = "-werror -a -clean -jar output.jar thing.conc /some/dir another/dir/";
		
		TestCase.assertEquals(expect, ""+concc.getConccInstance());
	}
	
	@Test
	public void testCommandLineErr() {
		Concc concc = new Concc("-options ", null, null);
		TestCase.assertNull(concc.getConccInstance());
	}
	
	@Test
	public void testShowBasicUsage() {
		Concc concc2 = new Concc("-options ", null, null);
		String expect = Concc.genericErrorMsg;
		
		String got = concc2.doit();
		TestCase.assertEquals(expect, got);
		//System.err.println(got);
	}
	
	@Test
	public void testHelpme() {
		Concc concc2 = new Concc("--help", null, null);
		String expect = Concc.helpMeErrorMsg;
		
		String got = concc2.doit();
		System.out.println(got);
		TestCase.assertEquals(expect, got);
	}

	@Test
	public void jarWithManifest() {
		Concc concc = new Concc("-d bin -jar thing [com.mycompany.myMain]  thing.conc /some/dir another/dir/ ", null, null);
		String expect = "-jar thing.jar(com.mycompany.myMain) -d bin thing.conc /some/dir another/dir/";
		
		TestCase.assertEquals(expect, ""+concc.getConccInstance());
	}
	
}
