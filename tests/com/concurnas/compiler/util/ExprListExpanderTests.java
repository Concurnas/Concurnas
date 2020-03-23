package com.concurnas.compiler.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import org.antlr.v4.runtime.CharStreams;
import org.junit.Test;

import com.concurnas.compiler.DirectFileLoader;
import com.concurnas.compiler.MainLoop;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.DuffAssign;
import com.concurnas.compiler.ast.ExpressionList;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.FuncParam;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.VarInt;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.syntax.BatchSyntaxTest;
import com.concurnas.compiler.syntax.SyntaxTests;
import com.concurnas.compiler.visitors.PrintSourceVisitor;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Utils;
import com.concurnas.compiler.visitors.algos.ListSegemtizer;
import com.concurnas.compiler.visitors.algos.ExpressionListExpander.FunctionProvider;
import com.concurnas.compiler.visitors.algos.ExpressionListExpander.PathGennerator;
import com.concurnas.runtime.Pair;

public class ExprListExpanderTests {

	private class MockFunctionProvider implements FunctionProvider{
		@Override
		public ArrayList<FuncDef> functionsAvailable(Expression onStackSoFar, String nameWanted, int remainingArgs) {
			return functionsAvailable(onStackSoFar, nameWanted, remainingArgs, 0);
		}
		
		private ArrayList<FuncDef> functionsAvailable(Expression onStackSoFar, String nameWanted, int remainingArgs, int iniitalM) {
			ArrayList<FuncDef> ret = new ArrayList<FuncDef>();
			
			for(int n=iniitalM; n <= remainingArgs; n++){
				ArrayList<Type> inputs = new ArrayList<Type>(n);
				for(int m = 0; m < n; m++){
					inputs.add(ScopeAndTypeChecker.const_object);
				}
				
				ret.add(FuncDef.build(ScopeAndTypeChecker.const_object, inputs.toArray(new Type[0])));
			}
			
			return ret;
		}

		@Override
		public ArrayList<FuncDef> funcRefsAvailable(Expression onStackSoFar, int remainingArgs) {
			return functionsAvailable(onStackSoFar, "", remainingArgs, 1);
		}

		@Override
		public ArrayList<FuncDef> constructorsAvailable(Expression onStackSoFar, int remainingArgs) {
			return functionsAvailable(onStackSoFar, "<init>", remainingArgs, 1);
		}
	}
	
	private String pprintPaths(String src, String filename) throws Exception
	{
		MainLoop mainLoop = null;
		try{
			SyntaxTests st = new SyntaxTests();
			Block b = st.runTest(CharStreams.fromString(src, filename), filename);
			ExpressionList lk = (ExpressionList)((DuffAssign)b.lines.get(0).l).e;
			String ret="";
			ArrayList<Expression> exprs = lk.exprs;
			
			HashMap<String, ClassDef> typeDirectory = new HashMap<String, ClassDef>();
			mainLoop = new MainLoop(".", new DirectFileLoader(), true, false, null, false);
			
			
			ScopeAndTypeChecker satc = new ScopeAndTypeChecker(mainLoop, null, "fullPathFileName", "packageAndClassName", typeDirectory, true, null);
			for(Expression e : exprs){//this is done in getPossibilities
				satc.maskErrors(false);
				e.accept(satc);
				satc.maskedErrors();
			}
			

			PathGennerator gen = new PathGennerator(null, exprs, null, new MockFunctionProvider());
			
			for(Expression expr : gen.genneratePaths()){
				PrintSourceVisitor visitor = new PrintSourceVisitor();
				expr.accept(visitor);
				ret += Utils.listToString(visitor.items) + "\n";
			}
			
			return ret;
		}catch(Exception e){
			e.printStackTrace();
			return e.getMessage();
		}finally {
			if(null != mainLoop) {
				mainLoop.stop();
			}
		}
	}

	
	@Test
	public void easyTest() throws Exception {
		String eval = "foo bar 4";
		String evaled = pprintPaths(eval, "").trim();
		
		//System.out.println(evaled);
	}
	
		
			
	@Test
	public void testListCombos21eachx() throws Exception {
		ArrayList<ArrayList<Pair<Integer, Integer>>> res = ListSegemtizer.sublistCombs(3, 1);
		assertEquals("3: [[(0, 0)], [(0, 1)], [(0, 2)]]", res.size() + ": " + res);
		ArrayList<ArrayList<Pair<Integer, Integer>>> res2 = ListSegemtizer.sublistCombs(3, 2);
		assertEquals("3: [[(0, 0), (1, 1)], [(0, 0), (1, 2)], [(0, 1), (2, 2)]]", res2.size() + ": " + res2);
	}
	
		
	@Test
	public void testListCombos21each() throws Exception {
		ArrayList<ArrayList<Pair<Integer, Integer>>> res = ListSegemtizer.sublistCombs(2, 2);
				
		String expected = "1: [[(0, 0), (1, 1)]]";//1 each
		String strRep = res.size() + ": " + res;

		assertEquals(expected, strRep);
	}
		
	@Test
	public void testListCombosSimple() throws Exception {
		ArrayList<ArrayList<Pair<Integer, Integer>>> res = ListSegemtizer.sublistCombs(3, 1);
				
		String expected = "3: [[(0, 0)], [(0, 1)], [(0, 2)]]";
		String strRep = res.size() + ": " + res;
		
		assertEquals(expected, strRep);
	}
	
	
	@Test
	public void testPathEval() throws Exception {
		String inputfilename = "./tests/com/concurnas/compiler/util/basicPathEval.inp";
		String outpufilename = "./tests/com/concurnas/compiler/util/basicPathEval.out";
		String data = Utils.readFile(inputfilename).trim();
		
		String[] tests = data.split("~~~~~");
		StringBuilder evalal = new StringBuilder();
		for(String test : tests){
			String testName = BatchSyntaxTest.getMiniTestName(test);
			evalal.append("\n~~~~~\n" + testName + "\n" + pprintPaths(test, testName).trim() + "\n");
		}
		
		String got = evalal.toString().trim();
		String expected = Utils.readFile(outpufilename).trim();
		
		
		assertEquals(expected, got);
	}
	
	@Test
	public void testListSegemtizer2() throws Exception {
		ArrayList<Expression> exprs = new ArrayList<Expression>();
		exprs.add(new VarInt(1));
		exprs.add(new VarInt(2));
		exprs.add(new VarInt(2));
				
		ArrayList<ArrayList<Pair<Integer, Integer>>> resolvesTo = ListSegemtizer.getSegments(exprs.size());
		String expected = "4: [[(0, 2)], [(0, 0), (1, 2)], [(0, 1), (2, 2)], [(0, 0), (1, 1), (2, 2)]]";
		String strRep = resolvesTo.size() + ": " + resolvesTo;
		
		
		assertEquals(expected, strRep);
	}
	
	@Test
	public void testListSegemtizer3() throws Exception {
		ArrayList<Expression> exprs = new ArrayList<Expression>();
		exprs.add(new VarInt(1));
		exprs.add(new VarInt(2));
		exprs.add(new VarInt(3));
		exprs.add(new VarInt(4));
		exprs.add(new VarInt(5));
		
		ArrayList<ArrayList<Pair<Integer, Integer>>> resolvesTo = ListSegemtizer.getSegments(exprs.size());
		String expected = "16: [[(0, 4)], [(0, 0), (1, 4)], [(0, 1), (2, 4)], [(0, 2), (3, 4)], [(0, 3), (4, 4)], [(0, 0), (1, 1), (2, 4)], [(0, 0), (1, 2), (3, 4)], [(0, 0), (1, 3), (4, 4)], [(0, 1), (2, 2), (3, 4)], [(0, 1), (2, 3), (4, 4)], [(0, 2), (3, 3), (4, 4)], [(0, 0), (1, 1), (2, 2), (3, 4)], [(0, 0), (1, 1), (2, 3), (4, 4)], [(0, 0), (1, 2), (3, 3), (4, 4)], [(0, 1), (2, 2), (3, 3), (4, 4)], [(0, 0), (1, 1), (2, 2), (3, 3), (4, 4)]]";
		String strRep = resolvesTo.size() + ": " + resolvesTo;
		
		assertEquals(expected, strRep);
	}
	
	@Test
	public void testPathSingle() throws Exception {
		String eval = "1 a b";
		String evaled = pprintPaths(eval, "").trim().replace('\n', ',');
		
		String expect = "1 ( a ) ( b ) ,1 ( a ) ( b ( ) ) ,1 ( a ) . b ,1 ( a ) \\. b ,1 ( a ) . b ( ) ,1 ( a ( ) ) ( b ) ,1 ( a ( ) ) ( b ( ) ) ,1 ( a ( ) ) . b ,1 ( a ( ) ) \\. b ,1 ( a ( ) ) . b ( ) ,1 ( a ( b ) ) ,1 ( a ( b ( ) ) ) ,1 ( a . b ) ,1 ( a \\. b ) ,1 ( a . b ( ) ) ,1 ( a ( ) ( b ) ) ,1 ( a ( ) ( b ( ) ) ) ,1 ( a ( ) . b ) ,1 ( a ( ) \\. b ) ,1 ( a ( ) . b ( ) ) ,1 ( a ( b ) ) ,1 ( a ( b ( ) ) ) ,1 ( a , b ) ,1 ( a , b ( ) ) ,1 ( a ( ) , b ) ,1 ( a ( ) , b ( ) ) ,1 . a ( b ) ,1 . a ( b ( ) ) ,1 . a . b ,1 . a \\. b ,1 . a . b ( ) ,1 \\. a ( b ) ,1 \\. a ( b ( ) ) ,1 \\. a . b ,1 \\. a \\. b ,1 \\. a . b ( ) ,1 . a ( ) ( b ) ,1 . a ( ) ( b ( ) ) ,1 . a ( ) . b ,1 . a ( ) \\. b ,1 . a ( ) . b ( ) ,1 .. a ( ) ( b ) ,1 .. a ( ) ( b ( ) ) ,1 .. a ( ) . b ,1 .. a ( ) \\. b ,1 .. a ( ) . b ( ) ,1 . a ( b ) ,1 . a ( b ( ) )";
		assertEquals(expect, evaled);
		
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testCombinations() throws Exception {
		ArrayList<ArrayList<String>> lists = new ArrayList<ArrayList<String>>();
		
		ArrayList ar3 = new ArrayList<String>();
		ar3.add("1");
		ar3.add("2");
		ar3.add("3");
		
		ArrayList ar2 = new ArrayList<String>();
		ar2.add("A");
		ar2.add("B");
		
		lists.add(ar3);
		lists.add(ar2);
		lists.add(ar2);
		
		String evaled =  "" + ListSegemtizer.getCombinations(lists);
		String expect = "[[1, A, A], [1, A, B], [1, B, A], [1, B, B], [2, A, A], [2, A, B], [2, B, A], [2, B, B], [3, A, A], [3, A, B], [3, B, A], [3, B, B]]";
		
		assertEquals(expect, evaled);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testCombinationsSingle() throws Exception {
		ArrayList<ArrayList<String>> lists = new ArrayList<ArrayList<String>>();
		ArrayList ar3 = new ArrayList<String>();
		ar3.add("1");
		ar3.add("2");
		ar3.add("3");
		
		lists.add(ar3);//just one
		
		String evaled =  "" + ListSegemtizer.getCombinations(lists);
		String expect = "[[1], [2], [3]]";
		assertEquals(expect, evaled);
	}
}
