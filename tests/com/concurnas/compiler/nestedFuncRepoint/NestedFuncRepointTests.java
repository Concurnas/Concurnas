package com.concurnas.compiler.nestedFuncRepoint;

import java.io.File;
import java.util.ArrayList;

import junit.framework.ComparisonFailure;
import junit.framework.TestCase;

import org.junit.Test;

import com.concurnas.compiler.DirectFileLoader;
import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.FileLoader;
import com.concurnas.compiler.MainLoop;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.bytecode.BytecodeTestJustNestedFuncs;
import com.concurnas.compiler.scopeAndType.MockFileLoader;
import com.concurnas.compiler.utils.FileUtils;
import com.concurnas.compiler.visitors.PrintSourceVisitor;
import com.concurnas.compiler.visitors.Utils;
import com.concurnas.runtime.Pair;

public class NestedFuncRepointTests  extends TestCase{
	private final static String SrcDir = (new File("./tests/com/concurnas/compiler/nestedFuncRepoint")).getAbsolutePath();

	
	private Pair<String, String> splitTestNameAndCode(String in)
	{//remove this: //##c1. vars - 1. long
		StringBuilder preable = new StringBuilder();
		StringBuilder code = new StringBuilder();
		
		String[] inputs = in.split("\n");
		for(int n =0; n < inputs.length; n++)
		{
			String toAdd = inputs[n];
			if(toAdd.startsWith("//##"))
			{
				preable.append(toAdd.substring(4));
			}
			else
			{
				code.append(toAdd+ "\n");
			}
		}
		
		return new Pair<String, String>(preable.toString(), code.toString() );
	}
	
	@Test public void testAllNestedFuncRepoint() throws Throwable { 
		testBigLoadFiles("testAllNestedFuncRepoint");
	}
	
	@Test public void testAllNestedFuncRepointLambda() throws Throwable { 
		testBigLoadFiles("testAllNestedFuncRepointLambda");
	}
	
	@Test public void testNestedFuncRepointSandbox() throws Throwable {
		runCompilation("NestedFuncRepointSandbox"); 
	}
	@Test public void testNestedFuncRepointbytcode() throws Throwable {
		BytecodeTestJustNestedFuncs c = new BytecodeTestJustNestedFuncs();
		c.testJustNestedFuncs();
	}
	
	private void runCompilation(String inputFile) throws Throwable
	{
		String absSrcFilePath = SrcDir + File.separator + inputFile +".conc";
		
		//String expected = absSrcFilePath +".e";
		checkExists(absSrcFilePath);
		//File exp = checkExists(expected);
		
		MainLoop mainLoop = new MainLoop(SrcDir, new DirectFileLoader(), true, false, null, false);
		ArrayList<ErrorHolder> errs = mainLoop.compileFile(inputFile +".conc").messages;
		ComparisonFailure tf = null;
		assertNoPreBytecodeErrors(errs, true);
		
		PrintSourceVisitor codePrinter = new PrintSourceVisitor();
		
		Block b = mainLoop.getRootBlock();//note that we examine just the file we past, not all the related dependancies
		assertNotNull("Root block is null" , b);
		
		codePrinter.visit(b); 
		mainLoop.stop();
		String ret = Utils.listToString(codePrinter.items);
		System.out.println(ret);
	}
	
	
	private boolean shoProgress = false;
	
	private void testBigLoadFiles(String filename) throws Throwable { 
		String intputFile = SrcDir + File.separator + filename +".conc";
		String compareTo = SrcDir + File.separator + filename +".conc.e";
		
		checkExists(intputFile);
		checkExists(compareTo);
		
		String data = FileUtils.readFile(intputFile);
		String dataE = FileUtils.readFile(compareTo);
		
		String[] intputs = data.split("~~~~~");
		
		StringBuilder result = new StringBuilder();
		assertEquals(intputs.length, dataE.split("~~~~~").length);
		
		for(int n = 0; n < intputs.length; n++)
		{
			Pair<String, String> testnameAndCode = splitTestNameAndCode(intputs[n]);
			String testname = testnameAndCode.getA();
			String code = testnameAndCode.getB();
			String output=null;
			MainLoop mainLoop = null;
			try
			{
				assertNotNull(testname);
				String niceTestName = Utils.getMiniTestFileName(testname);
				String inputFileName =  niceTestName + ".conc";
				
				MockFileLoader mockLoader = new MockFileLoader(SrcDir);
				mockLoader.addFile(SrcDir + FileLoader.pathSeparatorChar + inputFileName, code);
				
				mainLoop = new MainLoop(SrcDir, mockLoader, true, false, null, false);
				ArrayList<ErrorHolder> errs = mainLoop.compileFile(inputFileName).messages;
				output = assertNoPreBytecodeErrors(errs, false);
				if(null == output)
				{
					PrintSourceVisitor codePrinter = new PrintSourceVisitor(new String[]{"equals", "hashCode"});
					
					Block b = mainLoop.getRootBlock();//note that we examine just the file we past, not all the related dependancies
					assertNotNull("Root block is null" , b);
					
					codePrinter.visit(b); 

					output = Utils.listToString(codePrinter.items);
				}
			}
			catch(Throwable e)
			{
				e.printStackTrace();
				
				output = e.getMessage();
			}finally {
				if(null!= mainLoop) {
					mainLoop.stop();
				}
			}
			
			result.append("//##" + testname +"\n" + output + "\n\n");
			if(n != intputs.length-1)
			{
				result.append("~~~~~\n");
			}
			
			if(shoProgress){
				System.out.println(String.format("Nestef Func report completed %s of %s", n, intputs.length));
			}
		}
		
		assertEquals(dataE.trim(), result.toString().trim());
	}
	
	
	private File checkExists(String fn)
	{
		File ref = new  File(fn);
		if(!ref.exists())
		{
			fail("Canot find file: "+ ref);
		}
		return ref;
	}
	
	private String assertNoPreBytecodeErrors(ArrayList<ErrorHolder> errs, boolean thr)
	{
		if(!errs.isEmpty())
		{
			ArrayList<String> expectedErrs = Utils.erListToStrList(errs);
			String gotStr = Utils.listToStr(expectedErrs).trim();
			
			if(!gotStr.equals(""))
			{
				if(thr)
				{
					System.err.println("No pre fieldop errors were expected...");
					for(String err : expectedErrs)
					{
						System.err.println(err);
					}
					throw new junit.framework.ComparisonFailure("No pre fieldop errors were expected", "", gotStr);
				}
				return gotStr;
			}
		}
		return null;
	}
}
