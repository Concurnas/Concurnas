package com.concurnas.compiler.ast;

import junit.framework.TestCase;

import org.junit.Test;

import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.syntax.SyntaxTests;
import com.concurnas.compiler.visitors.PrintSourceVisitor;
import com.concurnas.compiler.visitors.Utils;

public class ASTConstructionTests  extends TestCase{

	private String getSrcPrintedString(String filename) throws Exception
	{
		SyntaxTests st = new SyntaxTests();
		Block b = st.runTest(filename);
		PrintSourceVisitor visitor = new PrintSourceVisitor();
		visitor.visit(b);

		String ret = Utils.listToString(visitor.items);
		//System.out.println(ret);
		//System.out.println(SourceFormatTests.listToString(visitor.visitList));
		
		return ret;
	}
	
	@Test
	public void testGenneral() throws Exception {
		assertEquals(Utils.readFile("./tests/com/concurnas/compiler/ast/testGenneral.owl.out").trim(), getSrcPrintedString("./tests/com/concurnas/compiler/ast/testGenneral.owl").trim());
	}
	
	@Test
	public void testRestart() throws Exception {
		assertEquals(Utils.readFile("./tests/com/concurnas/compiler/ast/restart.owl.out").trim(), 
				getSrcPrintedString("./tests/com/concurnas/compiler/ast/restart.owl").trim());
	}
	
	
	@Test
	public void testAsyncStuff() throws Exception {
		assertEquals(
		Utils.readFile("./tests/com/concurnas/compiler/ast/testAsyncStuff.owl.out").trim(), 
		getSrcPrintedString("./tests/com/concurnas/compiler/ast/testAsyncStuff.owl").trim()
		);
	}
	
	@Test
	public void testBranches() throws Exception {
		assertEquals(
		Utils.readFile("./tests/com/concurnas/compiler/ast/testBranches.owl.out").trim(), 
		getSrcPrintedString("./tests/com/concurnas/compiler/ast/testBranches.owl").trim()
		);
	}
	
	@Test
	public void testClasses() throws Exception {
		assertEquals(
		Utils.readFile("./tests/com/concurnas/compiler/ast/testClasses.owl.out").trim(), 
		getSrcPrintedString("./tests/com/concurnas/compiler/ast/testClasses.owl").trim()
		);
	}

	@Test
	public void testClassesAndNewlinesEtc() throws Exception {
		assertEquals(
		Utils.readFile("./tests/com/concurnas/compiler/ast/testClassesNwlinesetc.owl.out").trim(), 
		getSrcPrintedString("./tests/com/concurnas/compiler/ast/testClassesNwlinesetc.owl").trim()
		);
	}
	
	@Test
	public void testLambdas() throws Exception {
		assertEquals(
	    Utils.readFile("./tests/com/concurnas/compiler/ast/testLambdas.owl.out").trim(), 
		getSrcPrintedString("./tests/com/concurnas/compiler/ast/testLambdas.owl").trim()
		);
	}
	
	@Test
	public void testExpressions() throws Exception {
		assertEquals(
	    Utils.readFile("./tests/com/concurnas/compiler/ast/testExpressions.owl.out").trim(), 
		getSrcPrintedString("./tests/com/concurnas/compiler/ast/testExpressions.owl").trim()
		);
	}
	
}
