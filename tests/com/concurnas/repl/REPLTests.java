package com.concurnas.repl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class REPLTests {

	private REPL repl;
	@Before
	public void before() throws Exception {
		REPLRuntimeState.reset();
		this.repl = new REPL(false, false, false, true);
	}
	
	@After
	public void after() {
		this.repl.terminate();
	}
	
	//////////////////////////////////////////////////////////
	
	
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
		assertEquals("y ==> 30", repl.processInput("x = 10; y = 30"));
		assertEquals("x ==> 10", repl.processInput("x"));
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
		assertEquals("|  WARN 1:0 typedef qualifier is unused in right hand side definition: z\n" + 
				"|  created mything\n" + 
				"\n" + 
				"$0 ==> 45", repl.processInput("typedef mything<x, y, z> = x<y>; 45"));
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
		assertEquals("av ==> 2", repl.processInput("z=9; f=9; av=2"));
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
		
		assertEquals("|  ERROR 1:14 in y - Variable y has already been defined in current scope", repl.processInput("val y = 'ok'; val y = 'ok'"));//no double define in expr
		assertEquals("|  ERROR 1:14 in x - Variable x has already been defined in current scope", repl.processInput("val x = 'ok'; val x = 'ok'"));//no double define in expr
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
		assertEquals("|  ERROR 1:18 in bar(int) - Unable to find method with matching name: foo", repl.processInput("def bar(a int) => foo(a*2) + ab"));
		assertEquals("|  created function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => a*4"));
		assertEquals("|    update modified bar(int)", repl.processInput("ab = 10;"));
		assertEquals("$0 ==> 26", repl.processInput("bar(2)"));
		assertEquals("", repl.processInput("ab = 100;"));
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
		assertEquals("|  created xx", repl.processInput("typedef xx = set<String>"));
		assertEquals("$0 ==> []", repl.processInput("new xx()"));
	}
	
	@Test
	public void typdefsRedef() throws Exception {
		assertEquals("|  created xx", repl.processInput("typedef xx = set<String>"));
		assertEquals("|  created xx", repl.processInput("typedef xx = list<String>"));
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
		assertEquals("|  ERROR 1:22 in foo(java.lang.String) - Unable to find method with matching name: anohter", repl.processInput("def foo(a String)  {a=anohter();;} foo('uh oh')"));//how complaints
	}

	
	@Test
	public void funcWithErrorAndOp2() throws Exception {
		assertEquals("|  ERROR 1:19 in foo(java.lang.String) - numerical operation cannot be performed on type java.lang.String. No overloaded 'minus' operator found for type java.lang.String with signature: '(int)'",
						repl.processInput("def foo(a String) {a - 1}; foo('uh oh')"));//complaints
	}
	
	@Test
	public void basicFuncsDefs() throws Exception {
		assertEquals("|  created function plus(int, int)", repl.processInput("def plus(a int, b int) int => return a + b"));
		assertEquals("|  redefined function plus(int, int)", repl.processInput("def plus(a int, b int) int => return a + b"));//redef
		assertEquals("|  ERROR 1:42 Function plus with matching argument definition exists already in current Scope", repl.processInput("def plus(a int, b int) int {return a + b} def plus(a int, b int) int {return a + b}"));//redef x2 no!
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
				+ "|    at line:1", repl.processInput("foo('ok')"));//supress further recompilation and throws exception!
		
		
		assertEquals("|  created function id(java.lang.String)", repl.processInput("def id(a String) => a"));//no further complaints
		assertEquals("|  java.lang.Error: Unresolved compilation problem\n"
				+ "|    at foo(line:1)\n"
				+ "|    at line:1", repl.processInput("foo('ok')"));//throws exception!
		
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
		assertEquals("|  ERROR 1:18 in bar(int) - Unable to find method with matching name: foo", repl.processInput("def bar(a int) => foo(a*2)"));
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
		assertEquals("|  ERROR 1:18 in bar(int) - Unable to find method with matching name: foo", repl.processInput("def bar(a int) => foo(a*2) + ab"));
		assertEquals("|  created function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => a*4"));
		assertEquals("$0 ==> 26", repl.processInput("bar(2)"));
	}
	
	@Test
	public void fwdVariableDoesNotExistYetAssignNew() throws Exception {
		assertEquals("|  ERROR 1:18 in bar(int) - Unable to find method with matching name: foo", repl.processInput("def bar(a int) => foo(a*2) + ab"));
		assertEquals("|  created function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => a*4"));
		assertEquals("|    update modified bar(int)", repl.processInput("ab int = 10;"));
		assertEquals("$0 ==> 26", repl.processInput("bar(2)"));
		assertEquals("", repl.processInput("ab = 100;"));
		assertEquals("$1 ==> 116", repl.processInput("bar(2)"));
	}
	
	
	@Test
	public void fwdVariableDoesNotExistYetAssignMulti() throws Exception {
		assertEquals("|  ERROR 1:18 in bar(int) - Unable to find method with matching name: foo", repl.processInput("def bar(a int) => foo(a*2) + ab"));
		assertEquals("|  created function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => a*4"));
		assertEquals("|    update modified bar(int)", repl.processInput("ab =bb = 10;"));
		assertEquals("$0 ==> 26", repl.processInput("bar(2)"));
		assertEquals("", repl.processInput("ab = 100;"));
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
		assertEquals("|  ERROR 1:18 in bar(int) - Unable to find method with matching name: foo", repl.processInput("def bar(a int) => foo(a*2)"));
		assertEquals("|  created function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => 'str' + (a*4)"));
		assertEquals("$0 ==> str16", repl.processInput("bar(2)"));
		assertEquals("|  redefined function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => (a*4)"));
		assertEquals("$1 ==> 16", repl.processInput("bar(2)"));
	}
	
	@Test
	public void changeDepFuncTypeFwdRefErrToOk() throws Exception {//fwd ref, change types err -> ok
		assertEquals("|  ERROR 1:22 in bar(int) - Unable to find method with matching name: foo", repl.processInput("def bar(a int) int => foo(a*2)"));
		assertEquals("|  created function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => x='str' + (a*4);;"));//error!
		assertEquals("|  java.lang.Error: Unresolved compilation problem\n|    at bar(line:1)\n|    at line:1", repl.processInput("bar(2)"));
		assertEquals("|  redefined function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => (a*4)"));//now its ok
		assertEquals("$1 ==> 16", repl.processInput("bar(2)"));
	}

	@Test
	public void changeDepFuncTypeFwdRefOkToError() throws Exception {//fwd ref, change types ok -> err
		assertEquals("|  ERROR 1:22 in bar(int) - Unable to find method with matching name: foo", repl.processInput("def bar(a int) int => foo(a*2)"));
		assertEquals("|  created function foo(int)\n|    update modified bar(int)", repl.processInput("def foo(a int) => (a*4)"));//now its ok
		assertEquals("$0 ==> 16", repl.processInput("bar(2)"));
		assertEquals("|  ERROR 1:22 in bar(int) - Return statement in method must return type of int", repl.processInput("def foo(a int) => x='str' + (a*4);;"));//error!
		assertEquals("|  java.lang.Error: Unresolved compilation problem\n|    at bar(line:1)\n|    at line:1", repl.processInput("bar(2)"));
	}
	
	@Test
	public void transitiveDeps() throws Exception {
		assertEquals("|  ERROR 2:18 in bar(int) - Unable to find method with matching name: car", repl.processInput("def foo(a int)=> bar(a*2)\ndef bar(a int) => car(a*2)"));
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
		assertEquals("|  ERROR 1:18 in foo(int) - Unable to find method with matching name: bar", repl.processInput("def foo(a int) => bar&(2*a)"));
		assertEquals("|  created function bar(int)\n|    update modified foo(int)",repl.processInput("def bar(a int) => a+100"));//updates, bar and foo
		assertEquals("$0 ==> 104", repl.processInput("foo(2)()"));
	}
	
	
	@Test
	public void normalTypeDef() throws Exception {
		assertEquals("|  created MyList", repl.processInput("typedef MyList<X> = java.util.ArrayList<X>"));
		assertEquals("|  created function foo(int)",repl.processInput("def foo(a int) => new MyList<String>(a)"));//updates, bar and foo
		assertEquals("$0 ==> []", repl.processInput("foo(2)"));
	}
	
	@Test
	public void typeDefFwdRef() throws Exception {
		assertEquals("|  ERROR 1:22 in foo(int) - Unable to resolve type corresponding to name: MyList",repl.processInput("def foo(a int) => new MyList<String>(a)"));//updates, bar and foo
		assertEquals("|  created MyList\n|    update modified foo(int)", repl.processInput("typedef MyList<X> = java.util.ArrayList<X>"));
		assertEquals("$0 ==> []", repl.processInput("foo(2)"));
	}
	
	@Test
	public void typeDefMoreThanOne() throws Exception {
		assertEquals("|  created MyList", repl.processInput("typedef MyList = java.util.ArrayList<Integer>"));
		assertEquals("|  ERROR 1:22 in foo(int) - Unable to resolve type corresponding to name: MyList",repl.processInput("def foo(a int) => new MyList<String>(a)"));//updates, bar and foo
		assertEquals("|  created MyList\n|    update modified foo(int)", repl.processInput("typedef MyList<X> = java.util.ArrayList<X>"));
		assertEquals("$0 ==> []", repl.processInput("foo(2)"));
	}
	
	@Test
	public void typeDefDependsOnAnother() throws Exception {
		assertEquals("|  WARN 1:0 typedef qualifier is unused in right hand side definition: X\n|  ERROR 1:20 in MyList - Unable to resolve type corresponding to name: Thing", repl.processInput("typedef MyList<X> = Thing<X>"));
		assertEquals("|  created Thing\n|    update modified MyList", repl.processInput("typedef Thing<X> = java.util.ArrayList<X>"));
		assertEquals("|  created function foo(int)",repl.processInput("def foo(a int) => new MyList<String>(a)"));//updates, bar and foo
		assertEquals("$0 ==> []", repl.processInput("foo(2)"));
	}
	
	@Test
	public void typedefWithFwdRef() throws Exception {
		assertEquals("|  ERROR 1:22 in foo(int) - Unable to resolve type corresponding to name: MyList",repl.processInput("def foo(a int) => new MyList<String>(a)"));//updates, bar and foo
		assertEquals("|  WARN 1:0 typedef qualifier is unused in right hand side definition: X\n|  ERROR 1:20 in MyList - Unable to resolve type corresponding to name: Thing", repl.processInput("typedef MyList<X> = Thing<X>"));
		assertEquals("|  created Thing\n|    update modified MyList, foo(int)", repl.processInput("typedef Thing<X> = java.util.ArrayList<X>"));
		assertEquals("$0 ==> []", repl.processInput("foo(2)"));
	}
	
	@Test
	public void useNonPrimative() throws Exception {
		assertEquals("", repl.processInput("v1 = new java.util.ArrayList<String>();"));
		assertEquals("v1 ==> []", repl.processInput("v1"));
	}
	
	@Test
	public void classdef() throws Exception {
		assertEquals("|  created Person",repl.processInput("class Person(~name String, lastname String, yob int){ override toString() => 'Person({name}, {lastname}, {yob})'} "));//updates, bar and foo
		assertEquals("v1 ==> Person(dave, person, 1989)", repl.processInput("v1 = new Person('dave', 'person', 1989)"));
		assertEquals("$0 ==> dave",repl.processInput("v1.name"));
	}
	
	@Test
	public void varFwdRef() throws Exception {
		assertEquals("|  ERROR 1:5 in v1 - Unable to find method with matching name: thing", repl.processInput("v1 = thing()"));
		assertEquals("|  created function thing()",repl.processInput("def thing() => 1231"));//updates, bar and foo
		assertEquals("|  ERROR variable v1 does not exist",repl.processInput("v1"));
	}
	
	@Test
	public void classdefredef() throws Exception {
		assertEquals("|  created Person",repl.processInput("class Person(~name String, lastname String, yob int){ override toString() => 'Person({name}, {lastname}, {yob})'} "));//updates, bar and foo
		assertEquals("v1 ==> Person(dave, person, 1989)", repl.processInput("v1 = new Person('dave', 'person', 1989)"));
		
		assertEquals("|  redefined Person",repl.processInput("class Person(name String, lastname String, yob int){ override toString() => 'Person({name}, {lastname}, {yob})'} "));//updates, bar and foo
		assertEquals("v2 ==> Person(dave, person, 1989)", repl.processInput("v2 = new Person('dave', 'person', 1989)"));
		assertEquals("$0 ==> ok", repl.processInput("try{x=v1 == v2; 'fail'}catch(e){'ok'}"));
		
		assertEquals("$1 ==> ok",repl.processInput("try{n=v1.name; 'fail'}catch(e){'ok'}"));
		assertEquals("|  ERROR 1:3 The variable name is not visible",repl.processInput("v2.name"));
	}
	
	@Test
	public void classFwdRef() throws Exception {
		assertEquals("|  ERROR 1:18 in make() - Unable to resolve type corresponding to name: Person", repl.processInput("def make() => new Person('dave', 'person', 1989)"));
		assertEquals("|  created Person\n|    update modified make()",repl.processInput("class Person(~name String, lastname String, yob int){ override toString() => 'Person({name}, {lastname}, {yob})'} "));//updates, bar and foo
		assertEquals("$0 ==> Person(dave, person, 1989)",repl.processInput("make()"));
	}
	
	@Test
	public void classFwdRefOmitNew() throws Exception {
		assertEquals("|  ERROR 1:14 in make() - Unable to find method with matching name: Person", repl.processInput("def make() => Person('dave', 'person', 1989)"));
		assertEquals("|  created Person\n|    update modified make()",repl.processInput("class Person(~name String, lastname String, yob int){ override toString() => 'Person({name}, {lastname}, {yob})'} "));//updates, bar and foo
		assertEquals("$0 ==> Person(dave, person, 1989)",repl.processInput("make()"));
	}
	
	@Test
	public void classDepOnAnotherFwdRef() throws Exception {
		assertEquals("|  ERROR 1:33 in Maker - Unable to resolve type corresponding to name: Person", repl.processInput("class Maker(){ def make() => new Person('dave', 'person', 1989); }"));
		assertEquals("|  created Person\n|    update modified Maker",repl.processInput("class Person(~name String, lastname String, yob int){ override toString() => 'Person({name}, {lastname}, {yob})'} "));//updates, bar and foo
		assertEquals("$0 ==> Person(dave, person, 1989)",repl.processInput("Maker().make()"));
	}
	
	@Test
	public void dotOpFwdRef() throws Exception {
		assertEquals("|  ERROR 1:19 in getter(java.lang.Object) - Unable to resolve type corresponding to name: BHolder\n|  ERROR 1:38 in getter(java.lang.Object) - b cannot be resolved to a variable",
				repl.processInput("def getter(bholder BHolder)=> bholder.b.thing()"));
		
		assertEquals("|  ERROR 1:23 in BHolder - Unable to resolve type corresponding to name: BClz",				
				repl.processInput("class BHolder(public b BClz)"));
		
		assertEquals("|  created BClz\n|    update modified BHolder, getter(BHolder)",				
				repl.processInput("class BClz() { def thing() => 'works'}"));
		
		assertEquals("$0 ==> works",repl.processInput("getter(new BHolder(new BClz()))"));
	}
	
	@Test
	public void typedefRedefine() throws Exception {
		assertEquals("|  created xx", repl.processInput("typedef xx = set<String>"));
		assertEquals("|  redefined xx", repl.processInput("typedef xx = set<String>"));
		assertEquals("$0 ==> []", repl.processInput("new xx()"));
	}
	
	@Test
	public void nestedClassCall() throws Exception {
		assertEquals("|  ERROR 1:50 in Master - Unable to find method with matching name: dep", repl.processInput("class Master{ public class Child{ def doThing() =>dep(); } }"));
		assertEquals("|  created function dep()\n|    update modified Master", repl.processInput("def dep() => 'ok'"));
		assertEquals("$0 ==> ok", repl.processInput("new Master().new Child().doThing()"));
	}
	
	@Test
	public void enumFwdref() throws Exception {
		assertEquals("|  ERROR 1:15 in thing() - Unable to resolve reference to variable name: MyEnum.ONE", repl.processInput("def thing() => MyEnum.ONE"));
		assertEquals("|  created MyEnum\n|    update modified thing()", repl.processInput("enum MyEnum{ONE}"));
		assertEquals("$0 ==> ONE", repl.processInput("thing()"));
	}
		
	@Test
	public void annotFwdRef() throws Exception {
		assertEquals("|  ERROR 1:0 in thing() - Unable to resolve type corresponding to name: Something", repl.processInput("@Something def thing() => 123"));
		assertEquals("|  created Something\n|    update modified thing()", repl.processInput("annotation Something{}"));
		assertEquals("$0 ==> 123", repl.processInput("thing()"));
	}


	@Test
	public void traitFwdRef() throws Exception {
		assertEquals("|  ERROR 1:0 in MyClass - MyClass cannot resolve reference to trait: MyTrait", repl.processInput("class MyClass ~ MyTrait"));
		assertEquals("|  created MyTrait\n|    update modified MyClass", repl.processInput("trait MyTrait{ def thing() => 12 }"));
		assertEquals("$0 ==> 12", repl.processInput("MyClass().thing()"));
	}
	
	
	
	@Test
	public void traitSuperClass() throws Exception {
		assertEquals("|  ERROR 1:0 in MyClass - MyClass cannot resolve reference to superclass: SupClass", repl.processInput("class MyClass < SupClass"));
		assertEquals("|  created SupClass\n|    update modified MyClass", repl.processInput("open class SupClass { def thingSup() => 100 }"));
		assertEquals("$0 ==> 100", repl.processInput("MyClass().thingSup()"));
	}
	
	
	@Test
	public void actorNormalNonTopLevel() throws Exception {
		assertEquals("|  created MyClass", repl.processInput("class MyClass { def thing() => 100 }"));
		assertEquals("|  created function aa()", repl.processInput("def aa() => actor MyClass()"));
		assertEquals("$0 ==> 100", repl.processInput("aa().thing()"));
	}
	
	@Test
	public void showExceptionsProperly() throws Exception {
		assertEquals("|  java.lang.Exception: okok\n" + 
				"|    at line:1", repl.processInput("throw new Exception('okok');"));
		assertEquals("", repl.processInput("a=100;"));
	}
	
	@Test
	public void actorNormal() throws Exception {
		assertEquals("|  created MyClass", repl.processInput("class MyClass { def thing() => 100 }"));
		assertEquals("", repl.processInput("aa = actor MyClass();"));
		assertEquals("$0 ==> 100", repl.processInput("aa.thing()"));
	}
	
	@Test
	public void actorUnknownClass() throws Exception {
		assertEquals("|  ERROR 1:6 in aa - Unable to resolve type corresponding to name: sdfsdf", repl.processInput("aa= { actor sdfsdf() }"));
	}
	
	
	
	@Test
	public void actorNormalViaExsitingClass() throws Exception {
		assertEquals("|  created function aa()", repl.processInput("def aa(){ actor java.util.ArrayList<String>();}"));
		assertEquals("$0 ==> 0", repl.processInput("aa().size()"));
	}


	@Test
	public void actorNormalViaFunc() throws Exception {
		assertEquals("|  created MyClass", repl.processInput("class MyClass { def thing() => 100 }"));
		assertEquals("|  created function aa()", repl.processInput("def aa(){ actor MyClass();}"));
		assertEquals("$0 ==> 100", repl.processInput("aa().thing()"));
	}
	
	
	@Test
	public void actorFwdRef() throws Exception {
		assertEquals("|  ERROR 1:10 in aa() - Unable to resolve type corresponding to name: MyClass", repl.processInput("def aa(){ actor MyClass();}"));
		assertEquals("|  created MyClass\n|    update modified aa()", repl.processInput("class MyClass { def thing() => 100 }"));
		assertEquals("$0 ==> 100", repl.processInput("aa().thing()"));
	}
	
	
	@Test
	public void redefineActor() throws Exception {
		assertEquals("|  ERROR 1:10 in aa() - Unable to resolve type corresponding to name: MyClass", repl.processInput("def aa(){ actor MyClass();}"));
		assertEquals("|  created MyClass\n|    update modified aa()", repl.processInput("class MyClass { def thing() => 100 }"));
		assertEquals("|  redefined function aa()", repl.processInput("def aa(){ actor MyClass();}"));//redefine actor
		assertEquals("$0 ==> 100", repl.processInput("aa().thing()"));
	}
	
	@Test
	public void isRefInsideFunc() throws Exception {
		assertEquals("|  created function syz()", repl.processInput("def syz(){a = {1+2}!; a: }"));
		assertEquals("$0 ==> 3:", repl.processInput("syz()"));
	}
	
	@Test
	public void normalTypedActor() throws Exception {
		assertEquals("|  created MyClass", repl.processInput("class MyClass{ def thing() => 1212 }"));
		assertEquals("|  created MyActor", repl.processInput("actor MyActor of MyClass"));
		assertEquals("$0 ==> 1212", repl.processInput("new MyActor().thing()"));
	}
	
	@Test
	public void normalTypedActorfwdRef() throws Exception {
		assertEquals("|  ERROR 1:17 in MyActor - Unable to resolve type corresponding to name: MyClass", repl.processInput("actor MyActor of MyClass"));
		assertEquals("|  created MyClass\n|    update modified MyActor", repl.processInput("class MyClass{ def thing() => 1212 }"));
		assertEquals("$0 ==> 1212", repl.processInput("new MyActor().thing()"));
	}
	
		
	@Test
	public void normalActor() throws Exception {
		assertEquals("|  created MyClass", repl.processInput("class MyClass{ def thing() => 1212 }"));
		assertEquals("|  created MyActor", repl.processInput("actor MyActor { def thing() => new MyClass().thing() }"));
		assertEquals("$0 ==> 1212", repl.processInput("new MyActor().thing()"));
	}
	
	@Test
	public void fwdRefActor() throws Exception {
		assertEquals("|  ERROR 1:35 in MyActor - Unable to resolve type corresponding to name: MyClass", repl.processInput("actor MyActor { def thing() => new MyClass().thing() }"));
		assertEquals("|  created MyClass\n|    update modified MyActor", repl.processInput("class MyClass{ def thing() => 1212 }"));
		assertEquals("$0 ==> 1212", repl.processInput("new MyActor().thing()"));
	}
	
	
	@Test
	public void objProviders() throws Exception {
		
		
		assertEquals("|  created AgeHolder", repl.processInput("inject class AgeHolder(age Integer){ override toString() => '{age}'}"));
		assertEquals("|  created User", repl.processInput("inject class User(name String, ah AgeHolder){ override toString() => 'User({name}, {ah})'}"));
		assertEquals("|  created UserProvider\n|    update modified UserProvider", repl.processInput("provider UserProvider{ provide User;  String => 'freddie';   Integer => 22; }"));
		assertEquals("$0 ==> User(freddie, 22)", repl.processInput("new UserProvider().User()"));
	}
	
	@Test
	public void moreThanOneDep() throws Exception {
		assertEquals("|  ERROR 1:15 in toDep() - Unable to find method with matching name: a", repl.processInput("def toDep() => a() + b()"));
		assertEquals("|  created function a()\n|    update modified toDep()", repl.processInput("def a() => 100"));
		assertEquals("|  created function b()\n|    update modified toDep()", repl.processInput("def b() => 20"));
		assertEquals("$0 ==> 120", repl.processInput("toDep()"));
	}
	
	@Test
	public void moreThanOneDepClass() throws Exception {
		assertEquals("|  ERROR 1:37 in Maker - Unable to find method with matching name: a", repl.processInput("class Maker() { def make() => return a() + b() } "));
		assertEquals("|  created function a()\n|    update modified Maker", repl.processInput("def a() => 100"));
		assertEquals("|  created function b()\n|    update modified Maker", repl.processInput("def b() => 20"));
		assertEquals("$0 ==> 120", repl.processInput("Maker().make()"));
	}	
	
	@Test
	public void moreThanOneDepClassImplicitReturn() throws Exception {
		assertEquals("|  ERROR 1:30 in Maker - Unable to find method with matching name: a", repl.processInput("class Maker() { def make() => a() + b() } "));
		assertEquals("|  created function a()\n|    update modified Maker", repl.processInput("def a() => 100"));
		assertEquals("|  created function b()\n|    update modified Maker", repl.processInput("def b() => 20"));
		assertEquals("$0 ==> 120", repl.processInput("Maker().make()"));
	}	
	
	
	
	@Test
	public void objProvidersFwdRef() throws Exception {
		assertEquals("|  ERROR 1:23 in UserProvider - Provide defintion type: User not found", repl.processInput("provider UserProvider{ provide User;  String => 'freddie';   Integer => 22; }"));
		assertEquals("|  created AgeHolder", repl.processInput("inject class AgeHolder(age Integer){ override toString() => '{age}'}"));
		assertEquals("|  created User\n|    update modified UserProvider, UserProvider", repl.processInput("inject class User(name String, ah AgeHolder){ override toString() => 'User({name}, {ah})'}"));
		assertEquals("$0 ==> User(freddie, 22)", repl.processInput("new UserProvider().User()"));
	}
	
	
	@Test
	public void normalImport() throws Exception {
		assertEquals("", repl.processInput("from java.util import ArrayList"));
		assertEquals("$0 ==> []", repl.processInput("new ArrayList<String>()"));
	}
	
	@Test
	public void normalImportRedef() throws Exception {
		assertEquals("", repl.processInput("from java.util import ArrayList"));
		assertEquals("", repl.processInput("from java.util import ArrayList"));
		assertEquals("$0 ==> []", repl.processInput("new ArrayList<String>()"));
	}
	
	@Test
	public void importFromFwdRef() throws Exception {
		assertEquals("|  ERROR 1:19 in thing() - Unable to resolve type corresponding to name: ArrayList", repl.processInput("def thing() => new ArrayList<String>()"));
		assertEquals("|    update modified thing()", repl.processInput("from java.util import ArrayList"));
		assertEquals("$0 ==> []", repl.processInput("thing()"));
	}
	
	
	@Test
	public void normalImportImport() throws Exception {
		assertEquals("", repl.processInput("import java.util.ArrayList"));
		assertEquals("$0 ==> []", repl.processInput("new ArrayList<String>()"));
	}
	
	@Test
	public void normalImportImportRedef() throws Exception {
		assertEquals("", repl.processInput("import java.util.ArrayList"));
		assertEquals("", repl.processInput("import java.util.ArrayList"));
		assertEquals("$0 ==> []", repl.processInput("new ArrayList<String>()"));
	}
	
	@Test
	public void importImportFwdRef() throws Exception {
		assertEquals("|  ERROR 1:19 in thing() - Unable to resolve type corresponding to name: ArrayList", repl.processInput("def thing() => new ArrayList<String>()"));
		assertEquals("|    update modified thing()", repl.processInput("import java.util.ArrayList"));
		assertEquals("$0 ==> []", repl.processInput("thing()"));
	}
	
	@Test
	public void normalImportStar() throws Exception {
		assertEquals("", repl.processInput("import java.util.*"));
		assertEquals("$0 ==> []", repl.processInput("new ArrayList<String>()"));
	}
	

	@Test
	public void normalImportStarRedef() throws Exception {
		assertEquals("", repl.processInput("import java.util.*"));
		assertEquals("", repl.processInput("import java.util.*"));
		assertEquals("$0 ==> []", repl.processInput("new ArrayList<String>()"));
	}
	
	@Test
	public void importStarFwdRef() throws Exception {
		assertEquals("|  ERROR 1:19 in thing() - Unable to resolve type corresponding to name: ArrayList", repl.processInput("def thing() => new ArrayList<String>()"));
		assertEquals("|    update modified thing()", repl.processInput("import java.util.*"));
		assertEquals("$0 ==> []", repl.processInput("thing()"));
	}
	
	@Test
	public void normalImportStarStaticImports() throws Exception {
		assertEquals("", repl.processInput("from com.concurnas.lang.precompiled.ImportStar import *"));
		assertEquals("$0 ==> [12 112]", repl.processInput("[anInteger afunction()]"));
	}
	

	@Test
	public void normalImportStarRedefStaticImports() throws Exception {
		assertEquals("", repl.processInput("from com.concurnas.lang.precompiled.ImportStar import *"));
		assertEquals("", repl.processInput("from com.concurnas.lang.precompiled.ImportStar import *"));
		assertEquals("$0 ==> [12 112]", repl.processInput("[anInteger afunction()]"));
	}
	
	@Test
	public void importStarFwdRefStaticImports() throws Exception {
		assertEquals("|  ERROR 1:16 in thing() - anInteger cannot be resolved to a variable", repl.processInput("def thing() => [anInteger afunction()]"));
		assertEquals("|    update modified thing()", repl.processInput("from com.concurnas.lang.precompiled.ImportStar import *"));
		assertEquals("$0 ==> [12 112]", repl.processInput("thing()"));
	}
	
	
	
	
	@Test
	public void isoNewRef() throws Exception {
		assertEquals("a ==> 3:", repl.processInput("a int:= {1+2}!"));
		assertEquals("a ==> 3:", repl.processInput("a"));
		assertEquals("b ==> 12:", repl.processInput("b int:= {10+2}!; b"));
		assertEquals("", repl.processInput("c int:= {100+2}!;"));
		assertEquals("", repl.processInput("d1 int:= {100+2}!; d2 = 99;"));
		assertEquals("a ==> 3:", repl.processInput("c int:= {100+2}!; a"));
		assertEquals("z1 ==> 33\nz2 ==> 33", repl.processInput("z1=z2=33"));
	}
	
	@Test
	public void isoNewRef2() throws Exception {//needs special logic for when top level var is of ref type - neds to be created?
		assertEquals("a ==> 3:", repl.processInput("a int:= {1+2}!; a"));
		assertEquals("b ==> 3:", repl.processInput("b int:= {1+2}!"));
		assertEquals("$0 ==> 3", repl.processInput("a:get()"));
		assertEquals("$1 ==> 3", repl.processInput("c int:= {1+2}!; c:get()"));
	}
	
	@Test
	public void isoNewRefExisting() throws Exception {//needs special logic for when top level var is of ref type - neds to be created?
		assertEquals("b ==> 3:", repl.processInput("b = {1+2}!"));
		assertEquals("a ==> 3:", repl.processInput("a = {1+2}!; a"));
		assertEquals("$0 ==> 3", repl.processInput("a:get()"));
		assertEquals("$1 ==> 3", repl.processInput("c = {1+2}!; c:get()"));
	}
	
	@Test
	public void changeVarValueDoesntUpdateFunc() throws Exception {
		assertEquals("|  created function dee()", repl.processInput("a = 1; def dee() => a;"));
		assertEquals("", repl.processInput("a = 2;"));
		assertEquals("$0 ==> 2", repl.processInput("dee()"));//broken, show err on thing
	}
	
	
	@Test
	public void okThenErrorsShowErrors() throws Exception {
		assertEquals("|  created function dee()\n|  created function thing()", repl.processInput("def dee() => 22\ndef thing() => dee() * 2"));
		assertEquals("$0 ==> 44", repl.processInput("thing()"));
		assertEquals("|  ERROR 2:15 in thing() - numerical operation cannot be performed on type java.lang.String. No overloaded 'mul' operator found for type java.lang.String with signature: '(int)'", 
				repl.processInput("def dee() => '22'"));//broken, show err on thing
		assertEquals("|  java.lang.Error: Unresolved compilation problem\n|    at thing(line:2)\n|    at line:1", repl.processInput("thing()"));//broken, as expected
	}
	
	
	
	@Test
	public void errBeforeErrNowStillShow() throws Exception {
		assertEquals("|  created function dee()", 
				repl.processInput("def dee() => '22'"));
		assertEquals("|  ERROR 1:15 in thing() - numerical operation cannot be performed on type java.lang.String. No overloaded 'mul' operator found for type java.lang.String with signature: '(int)'",
				repl.processInput("def thing() => dee() * 2"));//show error
		assertEquals("|  redefined function dee()", 
				repl.processInput("def dee() => '22'"));//dont show error again! ( still broken)
		assertEquals("|  java.lang.Error: Unresolved compilation problem\n|    at thing(line:1)\n|    at line:1", repl.processInput("thing()"));//broken, as expected
	}
	
	
	@Test
	public void updateFuncOnlyOnDepTypeChange() throws Exception {
		assertEquals("|  created function foo()", repl.processInput("a = 1; def foo() => a;"));
		assertEquals("$0 ==> 1", repl.processInput("foo()"));
		assertEquals("", repl.processInput("a = 11;"));//this shouldnt show an update since the type is unchanged
		assertEquals("$1 ==> 11", repl.processInput("foo()"));
		assertEquals("", repl.processInput("a = 11;"));//update foo
		assertEquals("$2 ==> 11", repl.processInput("foo()"));
	}
	
	@Test
	public void awaitInFunc() throws Exception {
		assertEquals("|  created function waiter(java.lang.Integer:, int)\n\na ==> 10:", repl.processInput("a int:= 1; def waiter(x int:, n int) {  await(x; x == n) }; a = 10; waiter(a:, 10); a"));
	}
	
	@Test
	public void awaitInTopLevel() throws Exception {
		assertEquals("", repl.processInput("a int:= 1;"));
		assertEquals("", repl.processInput("await(a; a == 1)"));
		assertEquals("a ==> 1:", repl.processInput("a"));
		assertEquals("", repl.processInput("{a = 10}!; await(a; a == 10)"));
		assertEquals("a ==> 10:", repl.processInput("a"));
	}
	
	@Test
	public void onchangeInFunc() throws Exception {
		assertEquals("", repl.processInput("a := 1;"));
		assertEquals("", repl.processInput("b := 1;"));
		assertEquals("|  created function thing()", repl.processInput("def thing() {c <= a + b; c:}"));
		assertEquals("x ==> 2", repl.processInput("x = thing()"));
		assertEquals("d ==> 2:", repl.processInput("d := thing()"));
		assertEquals("d ==> 2:", repl.processInput("d"));
		assertEquals("d2 ==> 2:", repl.processInput("d2 int: = thing()"));
		assertEquals("|  created function waiter(java.lang.Integer:, int)\n\na ==> 9:", repl.processInput("def waiter(x int:, n int) {  await(x; x == n) }; a = 9; waiter(a, 9); a"));
		assertEquals("d ==> 12:", repl.processInput("a = 11; await(d; d == 12); d"));//shouldnt update def unless err before
	}
	
	@Test
	public void onChangeTopLevel() throws Exception {
		assertEquals("", repl.processInput("a := 1;"));
		assertEquals("", repl.processInput("b := 1;"));
		assertEquals("c ==> 2:", repl.processInput("c <= a+b"));
		assertEquals("c ==> 101:", repl.processInput("{a = 100}!; await(c; c == 101); c"));
	}	
	
	@Test
	public void delTopLevelVar() throws Exception {
		assertEquals("a ==> 1\nb ==> 1", repl.processInput("a=b=1"));
		assertEquals("|  ERROR 2:3 in c - a cannot be resolved to a variable", repl.processInput("del a;\n c=a"));//no
		assertEquals("|  ERROR variable b does not exist", repl.processInput("del b;\n b"));//no
		assertEquals("|  ERROR variable b does not exist", repl.processInput("b"));//no
		assertEquals("|  ERROR variable bjk does not exist", repl.processInput("bjk"));//no

		assertEquals("", repl.processInput("a=10;\ndel a;"));
		assertEquals("a ==> hi", repl.processInput("a='hi'"));
		
		assertEquals("a ==> hi", repl.processInput("a"));
	}
	
	@Test
	public void delFunc() throws Exception {
		assertEquals("|  created function thing()", repl.processInput("def thing() => 12"));
		assertEquals("", repl.processInput("del thing"));
		assertEquals("|  ERROR 1:0 Unable to find method with matching name: thing", repl.processInput("thing()"));
		assertEquals("|  created function thing()", repl.processInput("def thing() => 50"));
		assertEquals("$0 ==> 50", repl.processInput("thing()"));
	}
	
	@Test
	public void delManyFuncs() throws Exception {
		assertEquals("|  created function thing()", repl.processInput("def thing() => 12"));
		assertEquals("|  created function thing(int)", repl.processInput("def thing(a int) => 12 + a"));
		assertEquals("$0 ==> 12", repl.processInput("thing()"));
		assertEquals("$1 ==> 24", repl.processInput("thing(12)"));
		assertEquals("", repl.processInput("del thing"));
		assertEquals("|  ERROR 1:0 Unable to find method with matching name: thing", repl.processInput("thing()"));
		assertEquals("|  ERROR 1:0 Unable to find method with matching name: thing", repl.processInput("thing(12)"));
		assertEquals("|  created function thing()", repl.processInput("def thing() => 50"));
		assertEquals("$2 ==> 50", repl.processInput("thing()"));
	}
	
	@Test
	public void delManyFuncAndVar() throws Exception {
		assertEquals("|  created function thing()", repl.processInput("def thing() => 12"));
		assertEquals("|  created function thing(int)", repl.processInput("def thing(a int) => 12 + a"));
		assertEquals("", repl.processInput("thing = 99;"));
		assertEquals("$0 ==> 12", repl.processInput("thing()"));
		assertEquals("$1 ==> 24", repl.processInput("thing(12)"));
		assertEquals("thing ==> 99", repl.processInput("thing"));
		assertEquals("", repl.processInput("del thing"));
		assertEquals("|  ERROR 1:0 Unable to find method with matching name: thing", repl.processInput("thing()"));
		assertEquals("|  ERROR 1:0 Unable to find method with matching name: thing", repl.processInput("thing(12)"));
		assertEquals("|  ERROR variable thing does not exist", repl.processInput("thing"));
		assertEquals("|  created function thing()", repl.processInput("def thing() => 50"));
		assertEquals("", repl.processInput("thing = 89;"));
		assertEquals("$2 ==> 50", repl.processInput("thing()"));
		assertEquals("thing ==> 89", repl.processInput("thing"));
	}
	
	@Test
	public void delFuncDefRedefSameSession() throws Exception {
		//redef here would be very elaborate
		assertEquals("|  ERROR 2:8 Function x with matching argument definition exists already in current Scope", repl.processInput("def x() => 12;\n del x; def x()=> 13;\n x()"));
	}
	
	@Test
	public void delClassAndFuncSameName() throws Exception {
		assertEquals("|  created Person", repl.processInput("class Person(~name String, lastname String = 'smith', yob int){ override toString() => 'Person({name}, {lastname}, {yob})'}"));//updates, bar and foo
		assertEquals("|  created function Person()\n|    update modified Person", repl.processInput("def Person() => 13"));//updates, bar and foo
		assertEquals("p1 ==> Person(dave, smith, 23)", repl.processInput("p1 = new Person('dave', 23)"));
		assertEquals("p2 ==> 13", repl.processInput("p2 = Person()"));
		assertEquals("", repl.processInput("del Person"));
		assertEquals("|  ERROR 1:9 in p3 - Unable to resolve type corresponding to name: Person", repl.processInput("p3 = new Person('dave', 23)"));
		assertEquals("|  ERROR 1:5 in p4 - Unable to find method with matching name: repl$.Person", repl.processInput("p4 = Person()"));
	}
	
	@Test
	public void delClass() throws Exception {
		assertEquals("|  created Person", repl.processInput("class Person(~name String, lastname String = 'smith', yob int){ override toString() => 'Person({name}, {lastname}, {yob})'}"));//updates, bar and foo
		assertEquals("p1 ==> Person(dave, smith, 23)", repl.processInput("p1 = Person('dave', 23)"));
		assertEquals("", repl.processInput("del Person"));
		assertEquals("|  ERROR 1:5 in p2 - Unable to find method with matching name: repl$.Person", repl.processInput("p2 = Person('dave', 23)"));
		
		//and redefine
		assertEquals("|  created Person", repl.processInput("class Person(~name String, lastname String = 'smith', yob int){ override toString() => 'PersonNew({name}, {lastname}, {yob})'}"));//updates, bar and foo
		assertEquals("p3 ==> PersonNew(dave, smith, 23)", repl.processInput("p3 = Person('dave', 23)"));//ERROR should be new def: PersonNew
	}
	
	@Test
	public void delEnum() throws Exception {
		assertEquals("|  created Color", repl.processInput("enum Color{BLUE, GREEN}"));
		assertEquals("$0 ==> GREEN", repl.processInput("Color.GREEN"));
		assertEquals("", repl.processInput("del Color"));
		assertEquals("|  ERROR 1:0 Expression cannot appear on its own line\n|  ERROR 1:0 Unable to resolve reference to variable name: repl$.Color.GREEN", repl.processInput("Color.GREEN"));
	}
	
	@Test
	public void delEnumAndVar() throws Exception {
		assertEquals("|  created Color", repl.processInput("enum Color{BLUE, GREEN}"));
		assertEquals("|  created function Color()", repl.processInput("def Color() => 78"));
		assertEquals("$0 ==> GREEN", repl.processInput("Color.GREEN"));
		assertEquals("$1 ==> 78", repl.processInput("Color()"));
		assertEquals("", repl.processInput("del Color"));
		assertEquals("|  ERROR 1:0 Expression cannot appear on its own line\n|  ERROR 1:0 Unable to resolve reference to variable name: repl$.Color.GREEN", repl.processInput("Color.GREEN"));
		assertEquals("|  ERROR 1:0 Unable to find method with matching name: repl$.Color", repl.processInput("Color()"));
	}
	
	@Test
	public void delAnnotation() throws Exception {
		assertEquals("|  created MyAnnot", repl.processInput("annotation MyAnnot"));
		assertEquals("|  created function thing()", repl.processInput("@MyAnnot def thing() => 12"));
		assertEquals("", repl.processInput("del MyAnnot"));
		assertEquals("|  ERROR 1:0 in thing2() - Unable to resolve type corresponding to name: MyAnnot", repl.processInput("@MyAnnot def thing2() => 12"));
	}
	
	@Test
	public void delObjProvider() throws Exception {
		assertEquals("|  created AgeHolder", repl.processInput("inject class AgeHolder(age Integer){ override toString() => '{age}'}"));
		assertEquals("|  created User", repl.processInput("inject class User(name String, ah AgeHolder){ override toString() => 'User({name}, {ah})'}"));
		assertEquals("|  created UserProvider\n|    update modified UserProvider", repl.processInput("provider UserProvider{ provide User;  String => 'freddie';   Integer => 22; }"));
		assertEquals("$0 ==> User(freddie, 22)", repl.processInput("new UserProvider().User()"));
		assertEquals("", repl.processInput("del UserProvider"));
		assertEquals("|  ERROR 1:4 Unable to resolve type corresponding to name: UserProvider", repl.processInput("new UserProvider().User()"));
	}
	
	
	@Test
	public void delTypeDef() throws Exception {
		assertEquals("|  created MyList", repl.processInput("typedef MyList<X> = java.util.ArrayList<X>"));
		assertEquals("$0 ==> []",repl.processInput("new MyList<String>(12)"));
		assertEquals("",repl.processInput("del MyList"));
		assertEquals("|  ERROR 1:4 Unable to resolve type corresponding to name: MyList",repl.processInput("new MyList<String>(12)"));

		//back to normal...
		assertEquals("|  created MyList", repl.processInput("typedef MyList<X> = java.util.ArrayList<X>"));
		assertEquals("$1 ==> []",repl.processInput("new MyList<String>(12)"));
	}
	
	@Test
	public void delDepGraph() throws Exception {
		assertEquals("|  ERROR 1:13 in bar() - Unable to find method with matching name: thing", repl.processInput("def bar() => thing()"));
		assertEquals("|  java.lang.Error: Unresolved compilation problem\n|    at bar(line:1)\n|    at line:1", repl.processInput("bar()"));
		assertEquals("|  created function thing()\n|    update modified bar()", repl.processInput("def thing() => 12"));
		assertEquals("$0 ==> 12", repl.processInput("bar()"));
		assertEquals("", repl.processInput("del thing"));
		assertTrue(repl.processInput("bar()").contains("java.lang.NoSuchMethodError"));
	}
	
	@Test
	public void delInvalidate() throws Exception {
		assertEquals("|  created function thing()\n|  created function bar()", repl.processInput("def thing() => 12\ndef bar() => thing()\ndel thing"));
		//assertEquals("|  java.lang.NoSuchMethodError: 'int repl$1$Globals$.thing(com.concurnas.bootstrap.runtime.cps.Fiber)'\n|    at bar(line:2)\n|    at init(line:1)", repl.processInput("bar()"));
		assertTrue(repl.processInput("bar()").contains("java.lang.NoSuchMethodError"));
	}
	
	@Test
	public void testCmds() throws Exception {
		assertEquals("a ==> 100\nb ==> 100\nc ==> 100", repl.processInput("a=b=c=100"));
		assertEquals("|  created function thing()", repl.processInput("def thing() => 23"));
		assertEquals("|  created function thing(int)", repl.processInput("def thing(a int) => 23 + a"));
		assertEquals("|  created function thing(int, int)", repl.processInput("def thing(a int, b int) => 23 + a + b"));
		assertEquals("|  created function thing(int, java.lang.String)", repl.processInput("def thing(a int, b String) => 23 + a + b"));
		assertEquals("|  created Fella", repl.processInput("class Fella<X>(name String, an X)"));
		assertEquals("|  created Person", repl.processInput("class Person(name String, age int)"));
		assertEquals("|  created StringFello", repl.processInput("typedef StringFello = Fella<String>"));
		assertEquals("", repl.processInput("from java.util import List, ArrayList"));
		assertEquals("", repl.processInput("from java.util import *"));
		
		assertEquals("a\nb\nc", repl.cmdHandler("vars"));
		assertEquals("thing() int\nthing(int) int\nthing(int, int) int\nthing(int, java.lang.String) java.lang.String", repl.cmdHandler("defs"));
		assertEquals("Fella\nPerson", repl.cmdHandler("classes"));
		assertEquals("StringFello", repl.cmdHandler("typedefs"));
		assertEquals("java.util.ArrayList\njava.util.List\njava.util.*", repl.cmdHandler("imports"));
	}

	@Test
	public void verboseModeOnOff() throws Exception {
		assertEquals("|  created function thing1()", repl.processInput("def thing1() => 99"));
		assertEquals("|  Set verbose mode: off", repl.cmdHandler("verbose"));
		assertEquals("", repl.processInput("def thing2() => 99"));
		assertEquals("|  Set verbose mode: on", repl.cmdHandler("verbose"));
		assertEquals("|  created function thing3()", repl.processInput("def thing3() => 99"));
	}
	
	
	
	@Test
	public void matrixOp() throws Exception {
		assertEquals("a ==> [1 2 ; 3 4]", repl.processInput("a = [1 2; 3 4]"));
		assertEquals("$0 ==> [11 12 ; 13 14]", repl.processInput("a + 10"));
	}

	
	@Test
	public void listComp() throws Exception {
		assertEquals("p ==> [1 2 3 4 1 2 3 1 5 6 7 8 9]", repl.processInput("p=[1 2 3 4 1 2 3 1 5 6 7 8 9]"));
		assertEquals("$0 ==> [3, 3, 6, 9]", repl.processInput("x for x in p if x mod 3 == 0"));
	}
	
	
	@Test
	public void isoAlone() throws Exception {
		assertEquals("$0 ==> hi:", repl.processInput("{'hi'}!"));
	}
	
	@Test
	public void doesNothing() throws Exception {
		assertEquals("|  created function sdf()", repl.processInput("def sdf() { x = 99; }"));//prev definition shouldnt pollute later defs
		assertEquals("", repl.processInput("sdf()"));//should be ok
	}
	
	
	@Test
	public void infGenGetsCleared() throws Exception {
		assertEquals("|  ERROR 1:6 in ar3 - Generic parameter count of: 0 does not equal: 1 - generic type parameters must be defined", repl.processInput("ar3 = list();"));//prev definition shouldnt pollute later defs
		assertEquals("ar3 ==> [8]", repl.processInput(" ar3 = list(); ar3.add(8); ar3"));//should be ok
	}
	
	@Test
	public void fwdRefTupleDeref() throws Exception {
		assertEquals("|  ERROR 1:13 in foo() - a cannot be resolved to a variable", repl.processInput("def foo() => a+b"));
		assertEquals("|    update modified foo()\n\na ==> 1\nb ==> 2", repl.processInput("(a, b) = (1, 2)"));
		assertEquals("", repl.processInput("(a, b) = (1, 2);"));
		assertEquals("$0 ==> 3", repl.processInput("foo()"));
	}
	
	@Test
	public void delTupleDecomp() throws Exception {
		assertEquals("", repl.processInput("(a, b) = (1,2);"));
		assertEquals("", repl.processInput("del a"));
		assertEquals("|  ERROR variable a does not exist", repl.processInput("a"));
		assertEquals("b ==> 2", repl.processInput("b"));
		assertEquals("a ==> 91\nb ==> 29", repl.processInput("(a, b) = (91,29)"));
	}
	
	@Test
	public void tuplederefAssign() throws Exception {
		assertEquals("a ==> 1\nb ==> 2", repl.processInput("(a, b) = (1, 2)"));
	}
	
	
	@Test
	public void varlisting() throws Exception {
		assertEquals("$0 ==> 12", repl.processInput("12"));
		assertEquals("v ==> 12", repl.processInput("v=12"));
		assertEquals("$0\nv", repl.cmdHandler("vars"));
	}
	
	@Test
	public void nullableTypes() throws Exception {
		assertEquals("v ==> hi", repl.processInput("v String? = 'hi'"));
		assertEquals("v ==> null", repl.processInput("v=null"));
	}
	
	@Test
	public void dontremovePrevOKVarDuetoFaultyAssign() throws Exception {
		assertEquals("v ==> hi", repl.processInput("v String = 'hi'"));
		assertEquals("|  ERROR 1:0 in v - Assingment can be null, but assignment type is not nullable", repl.processInput("v=null"));
		assertEquals("v ==> hi", repl.processInput("v"));
	}
	
	@Test
	public void doubleFuncDefine() throws Exception {
		assertEquals("|  created function sum(int[])", repl.processInput("def sum(what int[]){ ret = 0; for(x in what) { ret += x} return ret; }"));
		assertEquals("$0 ==> 15", repl.processInput("sum([1 2 3 4 5])"));
		assertEquals("|  redefined function sum(int[])", repl.processInput("def sum(what int[]){ ret = 0; for(x in what) { ret += x} return ret; }"));
		assertEquals("$1 ==> 15", repl.processInput("sum([1 2 3 4 5])"));
	}
	
	@Test
	public void sumExprList() throws Exception {
		assertEquals("|  created function sum(int[])", repl.processInput("def sum(what int[]){ ret = 0; for(x in what) { ret += x} return ret; }"));
		assertEquals("", repl.processInput("a = [1 2 3 4 5 6 7 8 9];"));
		assertEquals("$0 ==> 45", repl.processInput("sum a"));
	}
	

	@Test
	public void gpukernel() throws Exception {
		String gpuKernel = "gpukernel 2 matMult(M int, N int, K int, global in A float[2], global in B float[2], global out C float[2]) {\r\n" + 
				"	    globalRow = get_global_id(0) // Row ID of C (0..M)\r\n" + 
				"	    globalCol = get_global_id(1) // Col ID of C (0..N)\r\n" + 
				"	 \r\n" + 
				"	    // Compute a single element (loop over K)\r\n" + 
				"	    acc = 0.0f;\r\n" + 
				"	    for (k=0; k<K; k++) {\r\n" + 
				"			acc += A[k*M + globalRow] * B[globalCol*K + k]\r\n" + 
				"	    }\r\n" + 
				"	    // Store the result\r\n" + 
				"	    C[globalCol*M + globalRow] = acc;\r\n" + 
				"	}";
		
		assertEquals("|  created function matMult(int, int, int, float[2], float[2], float[2])", repl.processInput(gpuKernel));
		
		
		String dgroup ="def chooseGPUDeviceGroup(gpuDev gpus.DeviceGroup[], cpuDev gpus.DeviceGroup[]) {\r\n" + 
				"	if(not gpuDev){//no gpu return first cpu\r\n" + 
				"		cpuDev[0]\r\n" + 
				"	}else{//intel hd graphics not as good as nvidea dedicated gpu or ati so deprioritize\r\n" + 
				"		for(grp in gpuDev){\r\n" + 
				"			if('Intel' not in grp.platformName){\r\n" + 
				"				return grp\r\n" + 
				"			}\r\n" + 
				"		}\r\n" + 
				"		gpuDev[0]//intel device then...\r\n" + 
				"	}\r\n" + 
				"}";
		assertEquals("|  created function chooseGPUDeviceGroup(com.concurnas.lang.gpus$DeviceGroup[], com.concurnas.lang.gpus$DeviceGroup[])", repl.processInput(dgroup));
		
		
		String doings = "def doings(){\r\n" + 
				"	gps = gpus.GPU()\r\n" + 
				"	cpus = gps.getCPUDevices()\r\n" + 
				"	deviceGrp = chooseGPUDeviceGroup(gps.getGPUDevices(), cpus)\r\n" + 
				"	device = deviceGrp.devices[0]\r\n" + 
				"	inGPU1 = device.makeOffHeapArrayIn(float[2].class, 2, 2)\r\n" + 
				"	inGPU2 = device.makeOffHeapArrayIn(float[2].class, 2, 2)\r\n" + 
				"	result = device.makeOffHeapArrayOut(float[2].class, 2, 2)\r\n" + 
				"	\r\n" + 
				"	x = [ 1.f 2 ; 3.f 4]\r\n" + 
				"	\r\n" + 
				"	c1 := inGPU1.writeToBuffer(x)\r\n" + 
				"	c2 := inGPU2.writeToBuffer(x)\r\n" + 
				"	\r\n" + 
				"	inst = matMult(2, 2, 2, inGPU1, inGPU2, result)\r\n" + 
				"	compute := device.exe(inst, [2 2], c1, c2)//rest param detault\r\n" + 
				"	//remove null\r\n" + 
				"	\r\n" + 
				"	ret = result.readFromBuffer(compute)\r\n" + 
				"	\r\n" + 
				"	del inGPU1, inGPU2, result\r\n" + 
				"	del c1, c2, compute\r\n" + 
				"	del deviceGrp, device\r\n" + 
				"	del inst\r\n" + 
				"	\r\n" + 
				"	\r\n" + 
				"	'nice: ' + ret\r\n" + 
				"}";
		assertEquals("|  created function doings()", repl.processInput(doings));
		assertEquals("$0 ==> nice: [7.0 10.0 ; 15.0 22.0]", repl.processInput("doings()"));
	}
	
	@Test
	public void fwdVarChange() throws Exception {
		assertEquals("|  ERROR 1:13 in foo() - ab cannot be resolved to a variable", repl.processInput("def foo() => ab"));
		assertEquals("|    update modified foo()\n\nab ==> 100", repl.processInput("ab = 100"));
		assertEquals("$0 ==> 100", repl.processInput("foo()"));
		assertEquals("ab ==> 200", repl.processInput("ab = 200"));
		assertEquals("ab ==> 200", repl.processInput("ab"));
		assertEquals("$1 ==> 200", repl.processInput("foo()"));
	}
	
	@Test
	public void awaitInFuncManyLines() throws Exception {
		assertEquals("", repl.processInput("a int:= 1;"));
		assertEquals("|  created function getA()", repl.processInput("def getA() => a:"));
		assertEquals("", repl.processInput("d := getA();"));
		assertEquals("|  created function waiter(java.lang.Integer:, int)", repl.processInput("def waiter(x int:, n int) {  await(x; x == n) }"));
		assertEquals("a ==> 10:", repl.processInput("a = 10; waiter(a:, 10); a"));
	}
	
	@Test
	public void topLevelIsolateIsolation() throws Exception {
		assertEquals("a ==> 99", repl.processInput("a=99"));
		assertEquals("a ==> 100", repl.processInput("++a; a"));
		assertEquals("a ==> 100", repl.processInput("a"));
		assertEquals("$1 ==> 101:", repl.processInput("{++a}!"));
		assertEquals("a ==> 100", repl.processInput("a"));
	}
	
	@Test
	public void redefClass() throws Exception {
		assertEquals("|  created MyClass", repl.processInput("class MyClass(az int) { override toString() { 'MyClass({az})' } }"));
		assertEquals("d ==> 12", repl.processInput("d=12"));
		assertEquals("a ==> MyClass(12)", repl.processInput("a=new MyClass(12)"));
		assertEquals("a ==> MyClass(13)", repl.processInput("a=new MyClass(13)"));
		assertEquals("|  redefined MyClass", repl.processInput("class MyClass(a int) { override toString() { 'MyClass2({a})' } }"));
		assertEquals("b ==> MyClass2(12)", repl.processInput("b=new MyClass(12)"));
		assertEquals("a ==> MyClass(13)", repl.processInput("a"));
	}
	
	@Test
	public void redefClassSelfRef() throws Exception {
		assertEquals("|  created MyClass", repl.processInput("class MyClass(az int) { override toString() { 'MyClass({az})' } def thing(g MyClass) => [g g] }"));
		assertEquals("a ==> MyClass(12)", repl.processInput("a=new MyClass(12)"));
		assertEquals("b ==> [MyClass(12) MyClass(12)]", repl.processInput("b=a thing a"));
	}
	
	
	@Test
	public void constructorRefFwdRef() throws Exception {
		assertEquals("|  ERROR 1:18 in make() - Unable to find reference function Type for: <init>", repl.processInput("def make() => new Person&"));
		assertEquals("|  created Person\n|    update modified make()",repl.processInput("class Person(~name String, lastname String, yob int){ override toString() => 'Person({name}, {lastname}, {yob})'} "));//updates, bar and foo
		assertEquals("$0 ==> Person(dave, person, 1989)",repl.processInput("make()('dave', 'person', 1989)"));
		
	}
	
	@Test
	public void constructorRef() throws Exception {
		assertEquals("|  created Person",repl.processInput("class Person(~name String, lastname String, yob int){ override toString() => 'Person({name}, {lastname}, {yob})'} "));//updates, bar and foo
		assertEquals("|  created function make()", repl.processInput("def make() => new Person&"));
		assertEquals("$0 ==> Person(dave, person, 1989)",repl.processInput("make()('dave', 'person', 1989)"));
	}
	
	@Test
	public void bugVect() throws Exception {
	String another = "from java.util import Random\n" + 
			"from java.time import LocalDateTime\n" + 
			"\n" + 
			"class TSPoint(-dateTime LocalDateTime, -price double){\n" + 
			"//class with two fields having implicit getter functions automatically defined by prefixing them with -\n" + 
			"	override toString() => String.format(\"TSPoint(%S, %.2f)\", dateTime, price)\n" + 
			"}\n" + 
			"\n" + 
			"def createData(){//seed is an optional parameter with a default value\n" + 
			"	rnd = new Random(1337)\n" + 
			"	startTime = LocalDateTime.\\of(2020, 1, 1, 0, 0)//midnight 1st jan 2020\n" + 
			"	price = 100.\n" + 
			"	\n" + 
			"	def rnd2dp(x double) => Math.round(x*100)/100. //nested function\n" + 
			"	\n" + 
			"	ret = list()\n" + 
			"	for(sOffset in 0 to 60*60*24){//'x to y' - an integer range from 'x' to 'y'\n" + 
			"		time = startTime.plusSeconds(sOffset)\n" + 
			"		ret.add(TSPoint(time, price))\n" + 
			"		price += rnd2dp(rnd.nextGaussian()*0.01)\n" + 
			"	}\n" + 
			"\n" + 
			"	ret\n" + 
			"}";
		
		assertEquals("|  created TSPoint\n|  created function createData()",repl.processInput(another));
	}
	
	@Test
	public void bugReOpParams() throws Exception {
		String bug = "from java.util import Random\n" + 
				"from java.time import LocalDateTime\n" + 
				"\n" + 
				"class TSPoint(-dateTime LocalDateTime, -price double){\n" + 
				"//class with two fields having implicit getter functions automatically defined by prefixing them with -\n" + 
				"	override toString() => String.format(\"TSPoint(%S, %.2f)\", dateTime, price)\n" + 
				"}\n" + 
				"\n" + 
				"def createData(seed = 1337){//seed is an optional parameter with a default value\n" + 
				"	rnd = new Random(seed)\n" + 
				"	startTime = LocalDateTime.\\of(2020, 1, 1, 0, 0)//midnight 1st jan 2020\n" + 
				"	price = 100.\n" + 
				"	\n" + 
				"	def rnd2dp(x double) => Math.round(x*100)/100. //nested function\n" + 
				"	\n" + 
				"	ret = list()\n" + 
				"	for(sOffset in 0 to 60*60*24){//'x to y' - an integer range from 'x' to 'y'\n" + 
				"		time = startTime.plusSeconds(sOffset)\n" + 
				"		ret.add(TSPoint(time, price))\n" + 
				"		price += rnd2dp(rnd.nextGaussian()*0.01)\n" + 
				"	}\n" + 
				"\n" + 
				"	ret\n" + 
				"}";
		
		assertEquals("|  created TSPoint\n|  created function createData(int)",repl.processInput(bug));
		assertEquals("$0 ==> TSPoint(2020-01-01T00:00, 100.00)",repl.processInput("f = x for x in createData(); f[0]"));
	}
	

	@Test
	public void bugOptionalParams() throws Exception {
		assertEquals("|  created function thing(int)",repl.processInput("def thing(a = 12) => a"));
		assertEquals("$0 ==> 12",repl.processInput("thing()"));
	}
	
	@Test
	public void parfor() throws Exception {
		String gcd = "def gcd(x int, y int){//greatest common divisor of two integers\n" + 
				"  while(y){\n" + 
				"    (x, y) = (y, x mod y)\n" + 
				"  }\n" + 
				"  x\n" + 
				"}";
		
		assertEquals("|  created function gcd(int, int)",repl.processInput(gcd));
		assertEquals("result ==> [10:, 1:, 2:, 1:, 2:, 5:, 2:, 1:, 2:, 1:, 10:]", repl.processInput("result = parfor(b in 0 to 10){\n" + 
				"  gcd(b, 10-b)\n" + 
				"}"));
	}

	
	@Test
	public void getWithIt() throws Exception {
		String gcd = "class Accumilator(-state int){\n" + 
				"	def add(e int)   => state += e;;\n" + 
				"	def minus(e int) => state -= e;;\n" + 
				"	def mul(e int)   => state *= e;;\n" + 
				"}\n" + 
				"";
		
		String doWith = "def doings(){\n" + 
				"	res = with(Accumilator(2)){\n" + 
				"		add(23)\n" + 
				"		if(state > 16){\n" + 
				"			minus(16)\n" + 
				"		}else{\n" + 
				"			mul(8)\n" + 
				"			mul(2)\n" + 
				"		}\n" + 
				"		state\n" + 
				"	}\n" + 
				"\n" + 
				"	\"\" + res\n" + 
				"}";
		
		assertEquals("|  created Accumilator",repl.processInput(gcd));
		assertEquals("|  created function doings()", repl.processInput(doWith));
		assertEquals("$0 ==> 9", repl.processInput("doings()"));
	}
	
	
	@Test
	public void justRet() throws Exception {
		String gcd = "class Accumilator(-state int){\n" + 
				"	def add(e int)   => state += e;;\n" + 
				"	def minus(e int) => state -= e;;\n" + 
				"	def mul(e int)   => state *= e;;\n" + 
				"}\n" + 
				"";
		
		String doWith = "with(Accumilator(2)){\n" + 
				"		add(23)\n" + 
				"		if(state > 16){\n" + 
				"			minus(16)\n" + 
				"		}else{\n" + 
				"			mul(8)\n" + 
				"			mul(2)\n" + 
				"		}\n" + 
				"		state\n" + 
				"	}";
		
		assertEquals("|  created Accumilator",repl.processInput(gcd));
		assertEquals("$0 ==> 9", repl.processInput(doWith));
	}

	

	@Test
	public void localClasses() throws Exception {
		String traitDef = "trait IndAncDec{\n" + 
				"		-count int\n" + 
				"		-countdown int =0\n" + 
				"		def inc() => ++count + countdown\n" + 
				"		def dec() => --countdown + count\n" + 
				"	}\n" + 
				"\n" + 
				"	class HasIncAndDec ~ IndAncDec{\n" + 
				"		override count = 0\n" + 
				"	}";
		
		String localClass = "def foo(){\n" + 
				"	AnotherCounty = class ~ IndAncDec{\n" + 
				"		override count = 0\n" + 
				"		override toString() => 'ok'\n" + 
				"	}\n" + 
				"\n" + 
				"	new AnotherCounty()\n" + 
				"}";
		assertEquals("|  created IndAncDec\n|  created HasIncAndDec",repl.processInput(traitDef));
		assertEquals("|  created function foo()",repl.processInput(localClass));
		assertEquals("$0 ==> ok",repl.processInput("foo()"));
	}
	
	@Test
	public void blowsUpREPL() throws Exception {
		String oops = "class Holder<X>{\n" + 
				"	private +x X?\n" + 
				"	public toBoolean() =>  this.x != null\n" + 
				"}\n" + 
				"\n" + 
				"holder = new Holder<Integer>();\n" + 
				"\n" + 
				"if(holder) {\n" + 
				"	//x has been set inside holder!\n" + 
				"}";
		assertTrue(repl.processInput(oops).contains("extraneous input"));
	}
	
	@Test
	public void idxLocal() throws Exception {
		assertEquals("", repl.processInput("myarray = [1 2 3 4 5];"));
		assertEquals("res ==> [(1, 0), (2, 1), (3, 2), (4, 3), (5, 4)]", repl.processInput("res = for(n in myarray ; idx){ (n, idx) }"));
	}
}

