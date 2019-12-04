package com.concurnas.repl;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class REPLTests {

	private REPL repl;
	@Before
	public void before() throws Exception {
		this.repl = new REPL(false, false, false);
	}
	
	@After
	public void after() {
		this.repl.terminate();
	}
	
	//////////////////////////////////////////////////////////
	/*
	@Test
	public void createVar()  {
		assertEquals("x ==> 10", repl.processInput("x = 10"));
	}

	@Test
	public void invalidSyntax() throws Exception {
		assertTrue(repl.processInput("x = ").startsWith("ERROR 1:2 extraneous input '=' expecting"));
	}
	
	
	@Test
	public void createVarLong() throws Exception {
		assertEquals("x ==> 10.0", repl.processInput("x = 10."));
	}
	@Test
	public void quietOutput() throws Exception {
		assertEquals("", repl.processInput("x = 10.;"));
	}
	
	@Test
	public void createMany() throws Exception {
		assertEquals("x ==> 10", repl.processInput("x = 10"));
		assertEquals("y ==> 20", repl.processInput("y = 20"));
		assertEquals("z ==> 20", repl.processInput("z = 20"));
		assertEquals("z1 ==> 20", repl.processInput("z1 = 20"));
	}
	
	@Test
	public void createAndUsevar() throws Exception {
		assertEquals("x ==> 10", repl.processInput("x = 10"));
		assertEquals("y ==> 20", repl.processInput("y = x + 10"));
	}
	
	@Test
	public void quiteAndUse() throws Exception {
		assertEquals("", repl.processInput("x = 10;"));
		assertEquals("y ==> 20", repl.processInput("y = x + 10"));
	}
	
	@Test
	public void createTwoVars() throws Exception {
		assertEquals("x ==> 10\ny ==> 30", repl.processInput("x = 10; y = 30"));
	}
	
	@Test
	public void simpleEval() throws Exception {
		assertEquals("$0 ==> 4", repl.processInput("2*2"));
	}
	
	
	
	@Test
	public void errorOnInput() throws Exception {
		assertEquals("|  ERROR 1:0 Expression cannot appear on its own line\n|  ERROR 1:0 x cannot be resolved to a variable", repl.processInput("x+2"));
	}
	
	
	@Test
	public void simpleEvalMany() throws Exception {
		assertEquals("$0 ==> 4", repl.processInput("2*2"));
		assertEquals("$1 ==> 90", repl.processInput("90"));
		assertEquals("$2 ==> 243", repl.processInput("3**5"));
		assertEquals("$3 ==> hey", repl.processInput("'hey'"));
		assertEquals("$4 ==> [1 2 ; 4 5]", repl.processInput("[1 2; 4 5]"));
	}
	
	@Test
	public void testWarn() throws Exception {
		assertEquals("|  WARN 1:0 typedef qualifier is unused in right hand side definition: z\n$0 ==> 45", repl.processInput("typedef mything<x, y, z> = x<y>; 45"));
	}
	
	@Test
	public void testImports() throws Exception {
		assertEquals("", repl.processInput("from java.util import ArrayList"));
		assertEquals("xx ==> []", repl.processInput("xx = new ArrayList<String>()"));
	}
	
	@Test
	public void testImportsoverrite() throws Exception {
		assertEquals("", repl.processInput("from java.util import HashMap as ArrayList"));
		assertEquals("", repl.processInput("from java.util import ArrayList"));//redefine import
		assertEquals("xx ==> []", repl.processInput("xx = new ArrayList<String>()"));
		//no overwrite in same expr
		assertEquals("|  ERROR 1:28 Import name has already been declared: List as: java.util.List", repl.processInput("from java.util import List; from java.util import List"));//cannot do two in one go
	}
	
	@Test
	public void testUsing() throws Exception {
		assertEquals("", repl.processInput("from com.concurnas.tests.helpers.miniLangs using SimpleLisp"));
		assertEquals("", repl.processInput("from com.concurnas.tests.helpers.miniLangs using SimpleLisp"));//ok to redefine
		assertEquals("|  ERROR 1:59 Using name has already been declared: SimpleLang as: com.concurnas.tests.helpers.langExt.SimpleLang", repl.processInput("from com.concurnas.tests.helpers.langExt using SimpleLang; from com.concurnas.tests.helpers.langExt using SimpleLang;"));//no to redefine in two
		assertEquals("$0 ==> 8", repl.processInput("SimpleLisp||(+ 4 4 )||"));
	}
	
	@Test
	public void varsPrintInOrder() throws Exception {
		assertEquals("a ==> 2\nf ==> 9\nz ==> 9", repl.processInput("z=9; f=9; a=2"));
	}
	

	@Test
	public void varInNonTopScope() throws Exception {
		assertEquals("", repl.processInput("{val x = 'ok'}"));
		assertEquals("$0 ==> ok", repl.processInput("{val x = 'ok'; x}"));//creates tmp
		assertEquals("", repl.processInput("{y=10; y++;;}"));//returns nothing
	}
	
	@Test
	public void testOverwriteVar() throws Exception {
		assertEquals("x ==> ok", repl.processInput("val x = 'ok'"));
		assertEquals("x ==> 10", repl.processInput("val x = 10"));//redefined - as var val
		
		assertEquals("|  ERROR 1:14 Variable y has already been defined in current scope", repl.processInput("val y = 'ok'; val y = 'ok'"));//no double define in expr
		assertEquals("|  ERROR 1:14 Variable x has already been defined in current scope", repl.processInput("val x = 'ok'; val x = 'ok'"));//no double define in expr
	}
	@Test
	public void printReassigned() throws Exception {
		assertEquals("x ==> 10", repl.processInput("x = 10"));//ok
		assertEquals("x ==> 10", repl.processInput("x = 10"));//ok
	}
	
	@Test
	public void printReassignedVal() throws Exception {
		assertEquals("x ==> 10", repl.processInput("x = 10"));//ok
		assertEquals("xp ==> 60", repl.processInput("xp = 60"));//ok
		assertEquals("x ==> 10", repl.processInput("val x = 10"));//ok
	}
	
	
	@Test
	public void onlyPrintThatAssigned() throws Exception {
		assertEquals("x ==> 10", repl.processInput("x=10"));
		assertEquals("$0 ==> ok", repl.processInput("'ok'"));//creates tmp
	}

	
	
	@Test
	public void errWorkflow() throws Exception {
		repl.processInput("x = 'ok");
		assertEquals("x ==> csdsd", repl.processInput("x = 'c' + 'sdsd'"));//ok
		repl.processInput("x = 'ok");//oops
		assertEquals("xy ==> 10", repl.processInput("xy = 10"));//ok
		assertEquals("xy ==> 10", repl.processInput("xy = 10"));//assigned again, print it out
	}
	
	@Test
	public void printOnRefName() throws Exception {
		assertEquals("xy ==> 10", repl.processInput("xy = 10"));//ok
		assertEquals("xy ==> 10", repl.processInput("xy"));//ok
		//print xy as referenced
	}

	@Test
	public void printOnRefNameAlsoAssign() throws Exception {
		assertEquals("xy ==> 10", repl.processInput("xy = 10; xy"));//ok
		//print xy as referenced
	}

	@Test
	public void doNothing() throws Exception {
		assertEquals("", repl.processInput("  "));//do nothing
		assertEquals("", repl.processInput(""));//do nothing
	}
	
	@Test
	public void doubleImport() throws Exception {
		assertEquals("", repl.processInput("from java.util import ArrayList"));
		assertEquals("", repl.processInput("from java.util import ArrayList; from java.util import ArrayList"));//fail expected
	}
	
	@Test
	public void typdefs() throws Exception {
		assertEquals("", repl.processInput("typedef xx = set<String>"));
		assertEquals("$0 ==> []", repl.processInput("new xx()"));
	}
	
	@Test
	public void typdefsRedef() throws Exception {
		assertEquals("", repl.processInput("typedef xx = set<String>"));
		assertEquals("", repl.processInput("typedef xx = list<String>"));
		assertEquals("$0 ==> []", repl.processInput("new xx()"));
	}
	
	@Test
	public void basicFuncsCollectedCall() throws Exception {
		assertEquals("|  created function plus(int, int)\n\n$0 ==> 4", repl.processInput("def plus(a int, b int) int { return a + b} plus(2,2)"));
	}
	
	@Test
	public void basicFuncs() throws Exception {
		assertEquals("|  created function plus(int, int)", repl.processInput("def plus(a int, b int) int => return a + b"));
		assertEquals("$0 ==> 5", repl.processInput("plus(2, 3)"));
	}
	
	@Test
	public void basicFuncsCompactTypeInf() throws Exception {
		assertEquals("|  created function plus(int, int)", repl.processInput("def plus(a int, b int) => return a + b"));
		assertEquals("$0 ==> 5", repl.processInput("plus(2, 3)"));
	}
	
	@Test
	public void basicFuncsCompact() throws Exception {
		assertEquals("|  created function plus(int, int)", repl.processInput("def plus(a int, b int) => {a + b}"));
		assertEquals("$0 ==> 5", repl.processInput("plus(2, 3)"));
	}
	
	
	@Test
	public void funcCreationMessage() throws Exception {
		assertEquals("|  created function plus(int, int)", repl.processInput("def plus(a int, b int) => {a + b}"));
	}
	
	@Test
	public void funcsDuplicate() throws Exception {
		assertEquals("|  created function plus(int, int)", repl.processInput("def plus(a int, b int) => {a + b}"));
		assertEquals("|  created function plus(int, int, int)", repl.processInput("def plus(a int, b int, c int) => plus(a, b) + c"));
		assertEquals("ff ==> 6", repl.processInput("ff = plus(2, 3, 1)"));
	}
		
	@Test
	public void funcredef() throws Exception {
		assertEquals("|  created function foo(int, int)", repl.processInput("def foo(a int, b int) => a + b"));
		assertEquals("|  redefined function foo(int, int)", repl.processInput("def foo(a int, b int) => a ** b as double"));
	}
	 
	@Test
	public void extFunc() throws Exception {
		assertEquals("|  created extension function int.pow(int, int)", repl.processInput("def int pow(a int) => this ** a"));
		assertEquals("$0 ==> 100", repl.processInput("10 pow 2"));
		assertEquals("|  redefined extension function int.pow(int, int)", repl.processInput("def int pow(a int) => this ** (a + 1)"));
		assertEquals("$1 ==> 1000", repl.processInput("10 pow 2"));
	}
	
	
	@Test
	public void funcCallsAnother() throws Exception {
		assertEquals("|  created function foo(int)", repl.processInput("def foo(a int) => a*2"));
		assertEquals("|  created function bar(int)", repl.processInput("def bar(a int) => foo(a*4)"));
		assertEquals("$0 ==> 64", repl.processInput("bar(8)"));
	}
	
	
	@Test
	public void funcWithErrorAndOp() throws Exception {
		assertEquals("|  ERROR 1:22 Unable to find method with matching name: anohter\n|  created function foo(String)\n\n|  java.lang.Error: Unresolved compilation problem\n|    at foo(line:1)\n|    at init(line:1)", repl.processInput("def foo(a String)  {a=anohter();;} foo('uh oh')"));//how complaints
	}

	
	@Test
	public void funcWithErrorAndOp2() throws Exception {
		assertEquals("|  ERROR 1:19 Expression cannot appear on its own line\n"
				+ "|  ERROR 1:19 numerical operation cannot be performed on type java.lang.String. No overloaded 'minus' operator found for type java.lang.String with signature: '(int)'\n|  created function foo(String)\n"
				+ "\n"
				+ "|  java.lang.Error: Unresolved compilation problem\n"
				+ "|    at foo(line:1)\n"
				+ "|    at init(line:1)",
						repl.processInput("def foo(a String) {a - 1}; foo('uh oh')"));//complaints
	}
	
	@Test
	public void basicFuncsDefs() throws Exception {
		assertEquals("|  created function plus(int, int)", repl.processInput("def plus(a int, b int) int => return a + b"));
		assertEquals("|  redefined function plus(int, int)", repl.processInput("def plus(a int, b int) int => return a + b"));//redef
		assertEquals("|  ERROR 1:42 Method plus with matching argument definition exists already in current Scope", repl.processInput("def plus(a int, b int) int {return a + b} def plus(a int, b int) int {return a + b}"));//redef x2 no!
	}
	

	@Test
	public void expressionEval() throws Exception {
		repl.processInput("a=10");
		assertEquals("$0 ==> 5", repl.processInput("5 if a > 5 else 2"));
		assertEquals("", repl.processInput("5 if a > 5 else 2;"));//supress
	}
	*/
	
	
	@Test
	public void funcWithError() throws Exception {
		//repl = new REPL(false, true, true);
		repl.processInput("def foo(a String) => a - 1");
		assertEquals("|  java.lang.Error: Unresolved compilation problem\n"
				+ "|    at foo(line:1)\n"
				+ "|    at init(line:1)", repl.processInput("foo('ok')"));//supress further recompilation and throws exception!
		
		
		assertEquals("|  created function id(String)", repl.processInput("def id(a String) => a"));//no further complaints
		assertEquals("|  java.lang.Error: Unresolved compilation problem\n"
				+ "|    at foo(line:1)\n"
				+ "|    at init(line:1)", repl.processInput("foo('ok')"));//throws exception!
		
		assertEquals("|  redefined function foo(String)", repl.processInput("def foo(a String) => a "));//correct error
		assertEquals("$0 ==> ok", repl.processInput("foo('ok')"));//now it;s ok
	}
	
	
	
	//fwd ref
	
	
	/*
	@Test
	public void funcCallsAnotherRedefined() throws Exception {
		assertEquals("|  created function foo(int)", repl.processInput("def foo(a int) => a*2"));
		assertEquals("|  created function bar(int)", repl.processInput("def bar(a int) => foo(a*4)"));
		assertEquals("$0 ==> 64", repl.processInput("bar(8)"));
		assertEquals("|  redefined function foo(int)", repl.processInput("def foo(a int) => a*3"));
		assertEquals("$1 ==> 96", repl.processInput("bar(8)"));
	}
	*/
	
	
	
}
