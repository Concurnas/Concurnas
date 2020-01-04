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
		assertEquals("|  WARN 1:0 typedef qualifier is unused in right hand side definition: z\n\n$0 ==> 45", repl.processInput("typedef mything<x, y, z> = x<y>; 45"));
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
	public void fwdVariableDoesNotExistYet() throws Exception {
		assertEquals("|  ERROR 1:18 Unable to find method with matching name: foo\n"
				+ "|  created function bar(int)", repl.processInput("def bar(a int) => foo(a*2) + ab"));
		assertEquals("|  created function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => a*4"));
		assertEquals("|    update modified bar(int)", repl.processInput("ab = 10;"));
		assertEquals("$0 ==> 26", repl.processInput("bar(2)"));
		assertEquals("|    update modified bar(int)", repl.processInput("ab = 100;"));
		assertEquals("$1 ==> 116", repl.processInput("bar(2)"));
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
		assertEquals("|  ERROR 1:22 Unable to find method with matching name: anohter\n|  created function foo(java.lang.String)\n\n|  java.lang.Error: Unresolved compilation problem\n|    at foo(line:1)\n|    at init(line:1)", repl.processInput("def foo(a String)  {a=anohter();;} foo('uh oh')"));//how complaints
	}

	
	@Test
	public void funcWithErrorAndOp2() throws Exception {
		assertEquals("|  ERROR 1:19 numerical operation cannot be performed on type java.lang.String. No overloaded 'minus' operator found for type java.lang.String with signature: '(int)'\n"
				+ "|  created function foo(java.lang.String)\n"
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
	
	
	@Test
	public void funcWithError() throws Exception {
		//repl = new REPL(false, true, true);
		repl.processInput("def foo(a String) => a - 1");
		assertEquals("|  java.lang.Error: Unresolved compilation problem\n"
				+ "|    at foo(line:1)\n"
				+ "|    at init(line:1)", repl.processInput("foo('ok')"));//supress further recompilation and throws exception!
		
		
		assertEquals("|  created function id(java.lang.String)", repl.processInput("def id(a String) => a"));//no further complaints
		assertEquals("|  java.lang.Error: Unresolved compilation problem\n"
				+ "|    at foo(line:1)\n"
				+ "|    at init(line:1)", repl.processInput("foo('ok')"));//throws exception!
		
		assertEquals("|  redefined function foo(java.lang.String)", repl.processInput("def foo(a String) => a "));//correct error
		assertEquals("$0 ==> ok", repl.processInput("foo('ok')"));//now it;s ok
	}
		
	@Test
	public void funcCallsAnotherRedefined() throws Exception {
		assertEquals("|  created function foo(int)", repl.processInput("def foo(a int) => a*2"));
		assertEquals("|  created function bar(int)", repl.processInput("def bar(a int) => foo(a*4)"));
		assertEquals("$0 ==> 64", repl.processInput("bar(8)"));
		assertEquals("|  redefined function foo(int)", repl.processInput("def foo(a int) => a*3"));
		assertEquals("$1 ==> 96", repl.processInput("bar(8)"));
	}
	
	@Test
	public void fwdRefNotExist() throws Exception {
		assertEquals("|  ERROR 1:18 Unable to find method with matching name: foo\n"
				   + "|  created function bar(int)", repl.processInput("def bar(a int) => foo(a*2)"));
		assertEquals("|  created function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => a*4"));
		assertEquals("|  created function callBar()", repl.processInput("def callBar() => bar(2)"));
		assertEquals("$0 ==> 16", repl.processInput("callBar()"));
		assertEquals("$1 ==> 16", repl.processInput("bar(2)"));
	}
	
	
	@Test
	public void cycles() throws Exception {
		assertEquals("|  created function factorial(int)", repl.processInput("	def factorial(i int) int { match(i){ 0 => 1\n n => n * factorial(n-1) } }"));
		assertEquals("$0 ==> 24", repl.processInput("factorial(4)"));
	}
	
	@Test
	public void fwdVarDepVar() throws Exception {
		assertEquals("", repl.processInput("ab = 10;"));
		assertEquals("|  ERROR 1:18 Unable to find method with matching name: foo\n"
				+ "|  created function bar(int)", repl.processInput("def bar(a int) => foo(a*2) + ab"));
		assertEquals("|  created function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => a*4"));
		assertEquals("$0 ==> 26", repl.processInput("bar(2)"));
	}
	
	@Test
	public void fwdVariableDoesNotExistYetAssignNew() throws Exception {
		assertEquals("|  ERROR 1:18 Unable to find method with matching name: foo\n"
				+ "|  created function bar(int)", repl.processInput("def bar(a int) => foo(a*2) + ab"));
		assertEquals("|  created function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => a*4"));
		assertEquals("|    update modified bar(int)", repl.processInput("ab int = 10;"));
		assertEquals("$0 ==> 26", repl.processInput("bar(2)"));
		assertEquals("|    update modified bar(int)", repl.processInput("ab = 100;"));
		assertEquals("$1 ==> 116", repl.processInput("bar(2)"));
	}
	
	
	@Test
	public void fwdVariableDoesNotExistYetAssignMulti() throws Exception {
		assertEquals("|  ERROR 1:18 Unable to find method with matching name: foo\n"
				+ "|  created function bar(int)", repl.processInput("def bar(a int) => foo(a*2) + ab"));
		assertEquals("|  created function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => a*4"));
		assertEquals("|    update modified bar(int)", repl.processInput("ab =bb = 10;"));
		assertEquals("$0 ==> 26", repl.processInput("bar(2)"));
		assertEquals("|    update modified bar(int)", repl.processInput("ab = 100;"));
		assertEquals("$1 ==> 116", repl.processInput("bar(2)"));
	}
	
	@Test
	public void changeDepFuncType() throws Exception {//fwd ref, change types ok -> ok
		assertEquals("|  created function foo(int)", repl.processInput("def foo(a int) => 'str' + (a*4)"));
		assertEquals("|  created function bar(int)", repl.processInput("def bar(a int) => foo(a*2)"));
		assertEquals("$0 ==> str16", repl.processInput("bar(2)"));
		assertEquals("|  redefined function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => (a*4)"));
		assertEquals("$1 ==> 16", repl.processInput("bar(2)"));
	}
	
	@Test
	public void changeDepFuncTypeFwdRef() throws Exception {//fwd ref, change types ok -> ok
		assertEquals("|  ERROR 1:18 Unable to find method with matching name: foo\n|  created function bar(int)", repl.processInput("def bar(a int) => foo(a*2)"));
		assertEquals("|  created function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => 'str' + (a*4)"));
		assertEquals("$0 ==> str16", repl.processInput("bar(2)"));
		assertEquals("|  redefined function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => (a*4)"));
		assertEquals("$1 ==> 16", repl.processInput("bar(2)"));
	}
	
	@Test
	public void changeDepFuncTypeFwdRefErrToOk() throws Exception {//fwd ref, change types err -> ok
		assertEquals("|  ERROR 1:22 Unable to find method with matching name: foo\n|  created function bar(int)", repl.processInput("def bar(a int) int => foo(a*2)"));
		assertEquals("|  created function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => x='str' + (a*4);;"));//error!
		assertEquals("|  java.lang.Error: Unresolved compilation problem\n|    at bar(line:1)\n|    at init(line:1)", repl.processInput("bar(2)"));
		assertEquals("|  redefined function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => (a*4)"));//now its ok
		assertEquals("$1 ==> 16", repl.processInput("bar(2)"));
	}

	@Test
	public void changeDepFuncTypeFwdRefOkToError() throws Exception {//fwd ref, change types ok -> err
		assertEquals("|  ERROR 1:22 Unable to find method with matching name: foo\n|  created function bar(int)", repl.processInput("def bar(a int) int => foo(a*2)"));
		assertEquals("|  created function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => (a*4)"));//now its ok
		assertEquals("$0 ==> 16", repl.processInput("bar(2)"));
		assertEquals("|  redefined function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => x='str' + (a*4);;"));//error!
		assertEquals("|  java.lang.Error: Unresolved compilation problem\n|    at bar(line:1)\n|    at init(line:1)", repl.processInput("bar(2)"));
	}
	
	@Test
	public void transitiveDeps() throws Exception {
		assertEquals("|  created function foo(int)\n|  created function bar(int)", repl.processInput("def foo(a int)=> bar(a*2)\ndef bar(a int) => car(a*2)"));
		assertEquals("|  created function car(int)\n|    update modified bar(int)", repl.processInput("def car(a int) => a+100"));//updates, bar and foo
		assertEquals("$0 ==> 108", repl.processInput("foo(2)"));
	}
	
	
	@Test
	public void normalFuncRef() throws Exception {
		assertEquals("|  created function bar(int)",repl.processInput("def bar(a int) => a+100"));
		assertEquals("|  created function foo(int)", repl.processInput("def foo(a int) => bar&(2*a)"));
		assertEquals("$0 ==> 104", repl.processInput("foo(2)()"));
	}
	
	@Test
	public void funcRefFwdRef() throws Exception {
		assertEquals("|  ERROR 1:18 Unable to find method with matching name: bar\n|  created function foo(int)", repl.processInput("def foo(a int) => bar&(2*a)"));
		assertEquals("|  created function bar(int)\n|    update modified foo(int)",repl.processInput("def bar(a int) => a+100"));//updates, bar and foo
		assertEquals("$0 ==> 104", repl.processInput("foo(2)()"));
	}
	
	
	@Test
	public void normalTypeDef() throws Exception {
		assertEquals("", repl.processInput("typedef MyList<X> = java.util.ArrayList<X>"));
		assertEquals("|  created function foo(int)",repl.processInput("def foo(a int) => new MyList<String>(a)"));//updates, bar and foo
		assertEquals("$0 ==> []", repl.processInput("foo(2)"));
	}
	
	@Test
	public void typeDefFwdRef() throws Exception {
		assertEquals("|  ERROR 1:22 Unable to resolve type corresponding to name: MyList\n|  created function foo(int)",repl.processInput("def foo(a int) => new MyList<String>(a)"));//updates, bar and foo
		assertEquals("|    update modified foo(int)", repl.processInput("typedef MyList<X> = java.util.ArrayList<X>"));
		assertEquals("$0 ==> []", repl.processInput("foo(2)"));
	}
	
	@Test
	public void typeDefMoreThanOne() throws Exception {
		assertEquals("", repl.processInput("typedef MyList = java.util.ArrayList<Integer>"));
		assertEquals("|  ERROR 1:22 Unable to resolve type corresponding to name: MyList\n|  created function foo(int)",repl.processInput("def foo(a int) => new MyList<String>(a)"));//updates, bar and foo
		assertEquals("|    update modified foo(int)", repl.processInput("typedef MyList<X> = java.util.ArrayList<X>"));
		assertEquals("$0 ==> []", repl.processInput("foo(2)"));
	}
	
	@Test
	public void typeDefDependsOnAnother() throws Exception {
		assertEquals("|  WARN 1:0 typedef qualifier is unused in right hand side definition: X\n|  ERROR 1:20 Unable to resolve type corresponding to name: Thing", repl.processInput("typedef MyList<X> = Thing<X>"));
		assertEquals("|    update modified MyList", repl.processInput("typedef Thing<X> = java.util.ArrayList<X>"));
		assertEquals("|  created function foo(int)",repl.processInput("def foo(a int) => new MyList<String>(a)"));//updates, bar and foo
		assertEquals("$0 ==> []", repl.processInput("foo(2)"));
	}
	
	@Test
	public void typedefWithFwdRef() throws Exception {
		assertEquals("|  ERROR 1:22 Unable to resolve type corresponding to name: MyList\n|  created function foo(int)",repl.processInput("def foo(a int) => new MyList<String>(a)"));//updates, bar and foo
		assertEquals("|    update modified foo(int)", repl.processInput("typedef MyList<X> = Thing<X>"));
		assertEquals("|    update modified MyList, foo(int)", repl.processInput("typedef Thing<X> = java.util.ArrayList<X>"));
		assertEquals("$0 ==> []", repl.processInput("foo(2)"));
	}
	
	@Test
	public void useNonPrimative() throws Exception {
		assertEquals("", repl.processInput("v1 = new java.util.ArrayList<String>();"));
		assertEquals("v1 ==> []", repl.processInput("v1"));
	}
	
	
	
	@Test
	public void classdef() throws Exception {
		assertEquals("",repl.processInput("class Person(~name String, lastname String, yob int){ override toString() => 'Person({name}, {lastname}, {yob})'} "));//updates, bar and foo
		assertEquals("v1 ==> Person(dave, person, 1989)", repl.processInput("v1 = new Person('dave', 'person', 1989)"));
		assertEquals("$0 ==> dave",repl.processInput("v1.name"));
	}
	
	@Test
	public void varWithRHSDepInvalid() throws Exception {
	//no, this cannot work, tle vars dont permit fwd refs
		assertEquals("|  ERROR 1:5 Unable to find method with matching name: thing", repl.processInput("v1 = thing()"));
		assertEquals("|  created function thing()",repl.processInput("def thing() => 1231"));//updates, bar and foo
		assertEquals("|  ERROR 1:0 Expression cannot appear on its own line",repl.processInput("v1"));
	}
	
	@Test
	public void classdefredef() throws Exception {
		assertEquals("",repl.processInput("class Person(~name String, lastname String, yob int){ override toString() => 'Person({name}, {lastname}, {yob})'} "));//updates, bar and foo
		assertEquals("v1 ==> Person(dave, person, 1989)", repl.processInput("v1 = new Person('dave', 'person', 1989)"));
		
		assertEquals("",repl.processInput("class Person(name String, lastname String, yob int){ override toString() => 'Person({name}, {lastname}, {yob})'} "));//updates, bar and foo
		assertEquals("v2 ==> Person(dave, person, 1989)", repl.processInput("v2 = new Person('dave', 'person', 1989)"));
		assertEquals("$0 ==> true", repl.processInput("v1 == v2"));
		
		assertEquals("$1 ==> dave",repl.processInput("v1.name"));
		assertEquals("|  ERROR 1:3 The variable name is not visible",repl.processInput("v2.name"));
	}
	
	@Test
	public void classFwdRef() throws Exception {
		assertEquals("|  ERROR 1:18 Unable to resolve type corresponding to name: Person\n|  created function make()", repl.processInput("def make() => new Person('dave', 'person', 1989)"));
		assertEquals("|    update modified make()",repl.processInput("class Person(~name String, lastname String, yob int){ override toString() => 'Person({name}, {lastname}, {yob})'} "));//updates, bar and foo
		assertEquals("$0 ==> Person(dave, person, 1989)",repl.processInput("make()"));
	}
	
	@Test
	public void classFwdRefOmitNew() throws Exception {
		assertEquals("|  ERROR 1:14 Unable to find method with matching name: Person\n|  created function make()", repl.processInput("def make() => Person('dave', 'person', 1989)"));
		assertEquals("|    update modified make()",repl.processInput("class Person(~name String, lastname String, yob int){ override toString() => 'Person({name}, {lastname}, {yob})'} "));//updates, bar and foo
		assertEquals("$0 ==> Person(dave, person, 1989)",repl.processInput("make()"));
	}
	
	@Test
	public void constructorRefFwdRef() throws Exception {
		assertEquals("|  ERROR 1:18 Unable to find reference function Type for: <init>\n|  created function make()", repl.processInput("def make() => new Person&"));
		assertEquals("|    update modified make()",repl.processInput("class Person(~name String, lastname String, yob int){ override toString() => 'Person({name}, {lastname}, {yob})'} "));//updates, bar and foo
		assertEquals("$0 ==> Person(dave, person, 1989)",repl.processInput("make()('dave', 'person', 1989)"));
	}
	
	@Test
	public void classDepOnAnotherFwdRef() throws Exception {
		assertEquals("|  ERROR 1:33 Unable to resolve type corresponding to name: Person", repl.processInput("class Maker(){ def make() => new Person('dave', 'person', 1989); }"));
		assertEquals("|    update modified Maker",repl.processInput("class Person(~name String, lastname String, yob int){ override toString() => 'Person({name}, {lastname}, {yob})'} "));//updates, bar and foo
		assertEquals("$0 ==> Person(dave, person, 1989)",repl.processInput("Maker().make()"));
	}
	
	@Test
	public void dotOpFwdRef() throws Exception {
		assertEquals("|  ERROR 1:19 Unable to resolve type corresponding to name: BHolder\n|  ERROR 1:38 b cannot be resolved to a variable\n|  created function getter(java.lang.Object)",
				repl.processInput("def getter(bholder BHolder)=> bholder.b.thing()"));
		
		assertEquals("|    update modified getter(BHolder)",				
				repl.processInput("class BHolder(public b BClz)"));
		
		assertEquals("|    update modified BHolder, getter(BHolder)",				
				repl.processInput("class BClz() { def thing() => 'works'}"));
		
		assertEquals("$0 ==> works",repl.processInput("getter(new BHolder(new BClz()))"));
	}
	
	@Test
	public void typedefRedefine() throws Exception {
		assertEquals("", repl.processInput("typedef xx = set<String>"));
		assertEquals("", repl.processInput("typedef xx = set<String>"));
		assertEquals("$0 ==> []", repl.processInput("new xx()"));
	}
	
	@Test
	public void nestedClassCall() throws Exception {
		assertEquals("|  ERROR 1:50 Unable to find method with matching name: dep", repl.processInput("class Master{ public class Child{ def doThing() =>dep(); } }"));
		assertEquals("|  created function dep()\n|    update modified Master", repl.processInput("def dep() => 'ok'"));
		assertEquals("$0 ==> ok", repl.processInput("new Master().new Child().doThing()"));
	}
	
	@Test
	public void enumFwdref() throws Exception {
		assertEquals("|  ERROR 1:15 Unable to resolve reference to variable name: MyEnum.ONE\n|  created function thing()", repl.processInput("def thing() => MyEnum.ONE"));
		assertEquals("|    update modified thing()", repl.processInput("enum MyEnum{ONE}"));
		assertEquals("$0 ==> ONE", repl.processInput("thing()"));
	}
		
	@Test
	public void annotFwdRef() throws Exception {
		assertEquals("|  ERROR 1:0 Unable to resolve type corresponding to name: Something\n|  created function thing()", repl.processInput("@Something def thing() => 123"));
		assertEquals("|    update modified thing()", repl.processInput("annotation Something{}"));
		assertEquals("$0 ==> 123", repl.processInput("thing()"));
	}


	@Test
	public void traitFwdRef() throws Exception {
		assertEquals("|  ERROR 1:0 MyClass cannot resolve reference to trait: MyTrait", repl.processInput("class MyClass ~ MyTrait"));
		assertEquals("|    update modified MyClass", repl.processInput("trait MyTrait{ def thing() => 12 }"));
		assertEquals("$0 ==> 12", repl.processInput("MyClass().thing()"));
	}
	
	
	
	@Test
	public void traitSuperClass() throws Exception {
		assertEquals("|  ERROR 1:0 MyClass cannot resolve reference to superclass: SupClass", repl.processInput("class MyClass < SupClass"));
		assertEquals("|    update modified MyClass", repl.processInput("open class SupClass { def thingSup() => 100 }"));
		assertEquals("$0 ==> 100", repl.processInput("MyClass().thingSup()"));
	}
	
	
	*/

	
	
	@Test
	public void actorNormal() throws Exception {
		assertEquals("", repl.processInput("class MyClass { def thing() => 100 }"));
		assertEquals("", repl.processInput("aa = actor MyClass()"));
		assertEquals("$0 ==> 100", repl.processInput("aa().thing()"));
	}
	
	
	/*
	 * TLE:
	 * 
	 * default actor, typed, untyped
	 * fwd ref: default actor, typed, untpyed
	 * 
	 */
	
	
	//more than one dependency missing - dont double report errors - funcdef
	//above for classdef
	

	//imports + usings
	// /imports /debug /quit /exit
	//import fwd ref
	//import other packages when starting repl
	
	//do the /imports command
	//also: vars, classes, typedefs
	
	//check isolates work
	/*
	@Test
	public void isoRefsNormal() throws Exception {
		assertEquals("a ==> 3", repl.processInput("a = {1+2}!"));
	}
	 */
	
	//check a <= b + c works
	/*	
	
	@Test
	public void isoRefsNormal() throws Exception {
		assertEquals("", repl.processInput("a := 1;"));
		assertEquals("", repl.processInput("b := 1;"));
		assertEquals("", repl.processInput("c <= b + b"));
	}
	*/
	

	//the del keyword - del any top level item | deps need to break as approperiate
		//del a var and recreate
	//del a function
	//del other top level elements
	//del Thing | where Thing is a class and also a Var
	//remove Thing from scope etc
	
	
	
	//add nice UI - windows and linux
	
	//tab completion

	//import class/jar to classpath - still start in repl mode
	
	
	
	//cntlr+c etc
	//terminations
	
}
