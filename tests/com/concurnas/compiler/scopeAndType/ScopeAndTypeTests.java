package com.concurnas.compiler.scopeAndType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.junit.Test;

import com.concurnas.compiler.DirectFileLoader;
import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.FileLoader;
import com.concurnas.compiler.MainLoop;
import com.concurnas.compiler.StartUtils;
import com.concurnas.compiler.utils.FileUtils;
import com.concurnas.compiler.visitors.Utils;

public class ScopeAndTypeTests  extends TestCase{

	//TODO: also pass all these cases (the ones that pass ok) through the bytecode gennerator and see if any cause class validation exceptions
	
	private File checkExists(String fn)
	{
		File ref = new  File(fn);
		if(!ref.exists())
		{
			fail("Canot find file: "+ ref);
		}
		return ref;
	}
	private String listToStr(ArrayList<String> list)
	{
		return listToStr(list, "", false);
	}
	
	private String listToStr(ArrayList<String> list, String prefix, boolean replaceNewLine)
	{
		StringBuilder sb = new StringBuilder();
		for(String s : list)
		{
			if(!s.trim().equals("") && !s.trim().startsWith("Test:") && !s.trim().startsWith("-> "))
			{
				sb.append(prefix);
			}
			if(replaceNewLine) {
				s = s.replace('\n',  '~');
			}
			
			sb.append( s);
			sb.append('\n');
		}
		return sb.toString();
		
	}
	
	private void compareWithExpected(File expected, ArrayList<ErrorHolder> gota) throws Exception
	{
		BufferedReader br = new BufferedReader(new FileReader(expected));
		try
		{
			ArrayList<String> got = Utils.erListToStrList(gota);
			ArrayList<String> expectedErrs = new ArrayList<String>();
			
			String sCurrentLine = null;
			
			while((sCurrentLine = br.readLine()) != null)
			{
				expectedErrs.add(sCurrentLine);
			}
			
			if(!got.equals(expectedErrs))
			{
				String expStr = listToStr(expectedErrs);
				String gotStr = listToStr(got);
				throw new junit.framework.ComparisonFailure("Expected messages does not match obtained", expStr, gotStr);
				
			}
		}
		catch(Exception e)
		{
			throw e;
		}
		finally
		{
			br.close();
		}
	}
	
	private void compareWithExpected(ArrayList<String> expectedErrs, String expPrefix, ArrayList<String> got)
	{
		String expStr = listToStr(expectedErrs, expPrefix, true);
		String gotStr = listToStr(got);
		
		if(!expStr.equals(gotStr))
		{
			throw new junit.framework.ComparisonFailure("Expected messages does not match obtained", expStr, gotStr);
		}
	}
	
	//fluff...//
	
	private void runCompilation(String inputFile) throws Exception
	{
		//D:\work\concurnas\tests\com\concurnas\compiler\scopeAndType\resources\genneralTryIt.owl
		String SrcDir = (new File("./tests/com/concurnas/compiler/scopeAndType/resources")).getAbsolutePath();
		
		String absSrcFilePath = SrcDir + File.separator + inputFile;
		
		String expected = absSrcFilePath +".e";
		checkExists(absSrcFilePath);
		File exp = checkExists(expected);
		
		MainLoop mainLoop = new MainLoop(SrcDir, new DirectFileLoader(), true, false, null, false);
		ArrayList<ErrorHolder> errs = mainLoop.compileFile(inputFile).messages;
		mainLoop.stop();
		compareWithExpected(exp, errs);
	}
	
	private String getMiniTestName(String input)
	{
		for(String line : input.split("\n"))
		{
			if(line.startsWith("//##") && !line.startsWith("//##MODULE"))
			{
				return line.substring(4);
			}
		}
		return null;
	}
	
	private String filterOutCommentAndNL(String input)
	{
		StringBuilder sb = new StringBuilder();
		String[] lines = input.split("\n");
		int len = lines.length;
		for(int n = 0; n < len; n++)
		{
			String line = lines[n];
			if(!line.startsWith("//##") && !line.startsWith("//##MODULE") && !line.trim().equals("")){
				sb.append(line);
				if(n != len)
				{
					sb.append("\n");
				}
			}
			
		}
		return sb.toString();
	}
	
	private ArrayList<String> getErrorList(String input)
	{
		String[] lines = input.split("\n");
		ArrayList<String> ret = new ArrayList<String>(lines.length);
		
		for(String line : lines)
		{
			if(!line.trim().equals("")){
				ret.add(line);
			}
		}
		
		return ret;
	}
	
	private int[] allPost(int from, int start)
	{
		int cnt = from-start;
		int[] ret = new int[cnt];
		for(int n = 0; n < cnt; n++)
		{
			ret[n] = start++;
		}
		
		return ret;
	}
	
	private void compareTestAndExpSet(String[] miniTests, String[] miniTestsE, int startingoffset) throws Exception
	{
		boolean showProgress = StartUtils.isRunningInEclipse();
		
		assertEquals(miniTests.length, miniTestsE.length);
		
		String SrcDir = (new File("./tests/com/concurnas/compiler/scopeAndType/resources")).getCanonicalPath();
	
		//MockFileLoader mockLoader = new MockFileLoader();
		
		ArrayList<String> expectedErrs = new ArrayList<String>();
		ArrayList<String> actualErrs = new ArrayList<String>();
		for(int n=0; n< miniTests.length; n++)
		{
			String miniTest = miniTests[n];
			String testName = getMiniTestName(miniTest);
			assertNotNull(testName);
			
			String inputFileName =  Utils.getMiniTestFileName(testName) + ".conc";//FileLoader.pathSeparatorChar + 
			
			//mockLoader.addFile(SrcDir + FileLoader.pathSeparatorChar + inputFileName, miniTest);
			MockFileLoader mockLoader = com.concurnas.compiler.bytecode.BytecodeTests.produceLoader(SrcDir, inputFileName, miniTest);
			
			MainLoop mainLoop = new MainLoop(SrcDir, mockLoader, true, false, null, false);
			String nnonon = String.format("Test: %s - %s" ,n + startingoffset, testName);
			expectedErrs.add(nnonon);
			actualErrs.add(nnonon);
			
			expectedErrs.addAll(getErrorList(filterOutCommentAndNL( miniTestsE[n])));
			
			
			actualErrs.addAll(  Utils.erListToStrList( mainLoop.compileFiles(mockLoader.getAllFiles()).messages, true));
			expectedErrs.add(" ");
			actualErrs.add(" ");
			mainLoop.stop();
			if(showProgress) {
				System.out.println(String.format("Complete so far: %6.2f%% - % 3d of % 3d in %s", ( (n+1) / (double)miniTests.length) *100, n+1, miniTests.length, testName));
			}
		}
		compareWithExpected(expectedErrs, SrcDir+File.separator, actualErrs);
	}
	
	private void runTest(String baseName) throws Exception{
		String data = FileUtils.readFile((new File(baseName)).getCanonicalPath());
		String dataE = FileUtils.readFile((new File(baseName + ".e")).getCanonicalPath());
		
		compareTestAndExpSet(data.split("~~~~~"), dataE.split("~~~~~"), 0);
	}
	
	@Test public void atestLambda() throws Exception{
		runTest(new File("./tests/com/concurnas/compiler/scopeAndType/lambda.conc.aug").getCanonicalPath());
	}
	
	@Test public void testTraits() throws Exception{
		runTest(new File("./tests/com/concurnas/compiler/scopeAndType/traits.conc").getCanonicalPath());
	}
	
	@Test public void testNoNull() throws Exception{
		runTest(new File("./tests/com/concurnas/compiler/scopeAndType/noNull.conc").getCanonicalPath());
	}
	
	@Test public void testLangExt() throws Exception{
		runTest(new File("./tests/com/concurnas/compiler/scopeAndType/langExt.conc").getCanonicalPath());
	}
	
	@Test public void atestRef() throws Exception 	{ 
		runTest(new File("./tests/com/concurnas/compiler/scopeAndType/ref.conc.aug").getCanonicalPath());
	}
	
	@Test public void atestDefoAssignment() throws Exception	{
		runTest(new File("./tests/com/concurnas/compiler/defoAssignment/defoAssignmentTests.conc.aug").getCanonicalPath());
	}
	
	@Test public void atestReturns() throws Exception	{
		runTest(new File("./tests/com/concurnas/compiler/returnAnalysis/returnTests.conc.aug").getCanonicalPath());
	}
	
	@Test public void testLastThingRetSAC() throws Exception	{
		runTest(new File("./tests/com/concurnas/compiler/scopeAndType/lastThingRetSAC.conc.aug").getCanonicalPath());
	}
	
	@Test public void testModules() throws Exception 	{
		runTest(new File("./tests/com/concurnas/compiler/scopeAndType/modules.conc.aug").getCanonicalPath());
	}
	
	@Test public void testVectorization() throws Exception 	{ 
		runTest(new File("./tests/com/concurnas/compiler/scopeAndType/vectorizationSATC.conc.aug").getCanonicalPath());
	}
	
	@Test public void testMultitype() throws Exception 	{ 
		runTest(new File("./tests/com/concurnas/compiler/scopeAndType/multitypeSATC.conc.aug").getCanonicalPath());
	}
	
	@Test public void testObjProviders() throws Exception 	{ 
		runTest(new File("./tests/com/concurnas/compiler/scopeAndType/objProvidersSATC.conc").getCanonicalPath());
	}
	
	@Test public void testtheBigOne() throws Exception
	{
		//C:\workp\concurnas\tests\com\concurnas\compiler\scopeAndType\theBigOne.conc.aug
		//String SrcDir = (new File("./tests/com/concurnas/compiler/scopeAndType/resources")).getCanonicalPath();
		String srcFile = (new File("./tests/com/concurnas/compiler/scopeAndType/theBigOne.conc")).getCanonicalPath();
		String srcFileE = (new File("./tests/com/concurnas/compiler/scopeAndType/theBigOne.conc.e")).getCanonicalPath();
		String data = FileUtils.readFile(srcFile);
		String dataE = FileUtils.readFile(srcFileE);
		
		String[] miniTests = data.split("~~~~~");
		String[] miniTestsE = dataE.split("~~~~~");
		
		int[] specific = {};
		specific = new int[]{   112   };
		//specific = new int[]{    29  };
		int startingoffset = 0;
		// startingoffset = 102;
		
		specific = allPost(miniTests.length, startingoffset);
		
		if(specific.length > 0)
		{
			String[] newminiTests = new String[specific.length];
			String[] newminiTestsE = new String[specific.length];
			
			if(miniTests.length != miniTestsE.length){
				throw new RuntimeException(String.format("mismach in # tests: %s, results: %s", miniTests.length, miniTestsE.length));
			}
			
			for(int n =0; n < specific.length; n++)
			{
				int which = specific[n];
				newminiTests[n] = miniTests[which];
				newminiTestsE[n] = miniTestsE[which];
			}
			
			miniTests=newminiTests;
			miniTestsE=newminiTestsE;
		}
		
		compareTestAndExpSet(miniTests, miniTestsE, 0);
	}
	

	@Test public void atestPlay() throws Exception 	{ runCompilation("play.conc"); 	}
}
