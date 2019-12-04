package com.concurnas.compiler.syntax;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.concurnas.compiler.utils.SyntaxParserTestUtil;

import junit.framework.TestCase;

/*
 * one off not included in main set ( as implicitly tested)
 */
public class BatchSyntaxTest{

	private ArrayList<String> skip = new  ArrayList<String>();
	{
		skip.add("testExpressions.owl");
		skip.add("testLambdas.owl");
		skip.add("fieldAccessSandbox.conc");
		skip.add("assign1.conc");
		skip.add("classes.owl");
	}
	
	@Test
	public void batchTest() throws Exception {
		List<File> ftt = filesToTest("./tests");
		boolean allpass = true;
		int n=0;
		for(File tt : ftt){
			String sname = tt.getName();
			if(skip.contains(sname)){
				System.out.println("skip: " + sname);
			}else{
				if(!testFile(tt, n, ftt.size())){
					allpass = false;
				}
			}
			n++;
		}
		
		if(!allpass){
			TestCase.fail("Not all tests passed");
		}
	}
	
	private void addFiles(Path place, List<File> retSoFar) throws IOException{
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(place)) {
			for (Path entry : stream) {
				if(Files.isDirectory(entry)){
					addFiles(entry, retSoFar);
				}else{
					String en = entry.toString();
					if(en.endsWith(".owl") || en.endsWith(".conc")){
						retSoFar.add(entry.toFile());
					}
				}
			}
		}
	}
	
	private List<File> filesToTest(String root) throws IOException {
		// find all owl and conc files nested under path
		ArrayList<File> ret = new ArrayList<File>();
		Path dir =  FileSystems.getDefault().getPath(root);
		System.out.println("Find files to perform BatchSyntaxTest on in directory: " + dir.toAbsolutePath());
		addFiles(dir, ret);
		Collections.sort(ret);
		return ret;
	}
	
	public static String getMiniTestName(String input)
	{
		for(String line : input.split("\n"))
		{
			if(line.startsWith("//##"))
			{
				return line.substring(4);
			}
		}
		return null;
	}
	
	private boolean testFile(File tt, int nn, int tot) throws Exception{
		String fname = tt.getAbsolutePath();
		System.out.println(String.format("%s/%s Process file: %s", nn, tot, fname));
		String data = readFile(fname);
		String[] tests = data.split("~~~~~");
		int tlen = tests.length;
		int inc = (int)Math.ceil(tlen / (double)10);
		
		long tick = System.currentTimeMillis();
		
		boolean pass = true;
		
		for(int n=0; n < tlen; n++){
			String test = tests[n];
			
			SyntaxParserTestUtil testerUtil = new SyntaxParserTestUtil();
			String testName = getMiniTestName(test);
			testName = tt.getName() +  (testName==null?"":": " + testName) + "|| ";
			try{
				testerUtil.parse( testName, test);
			}catch(Exception e){
				testerUtil.printLastErrors();
				System.out.println("");
				pass=false;
			}
			
			if(n % inc == 0){
				long toc = System.currentTimeMillis();
				System.out.println(String.format("%.2f%% complete %.0f sec", (n+1)/((double)tlen)*100, (toc - tick) / (double)1000));
			}
		}
		
		return pass;
		//break into subsections
		//log any errors
		//check errors against error file
		
	}

	private static String readFile(String filename) throws Exception
	{
		BufferedReader in = null;
		StringBuilder ret = new StringBuilder();
		try
		{
			in = new BufferedReader(new FileReader(filename));
			String ll = in.readLine();
			while(null != ll)
			{
				ret.append(ll);
				ll = in.readLine();
				if(null != ll )
				{
					ret.append("\n");
				}
			}
		}
		catch(Exception e)
		{
			try
			{
				in.close();
			}
			catch(Exception ee)
			{
			}
			throw e;
		}
		in.close();
		return ret.toString();
	}
}
