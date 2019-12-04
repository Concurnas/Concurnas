package com.concurnas.conc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import com.concurnas.compiler.utils.ListMaker;
import com.concurnas.concc.Concc;
import com.concurnas.concc.utils.ConccTestMockFileLoader;
import com.concurnas.concc.utils.ConccTestMockFileWriter;

import junit.framework.TestCase;

public class ConcExeTests {
	
	public static void checkOutput(Conc conc, String expected) {
		PrintStream old = System.err;//this isn't the best method as clashes wth concurrent tests outputting to err
		try {
			try(ByteArrayOutputStream baos = new ByteArrayOutputStream(); PrintStream ps = new PrintStream(baos);){
				System.setErr(ps);
				TestCase.assertEquals("", conc.doit());
				
				System.err.flush();
				
				TestCase.assertEquals(expected, baos.toString().trim());
			}
		} catch (IOException e) {
		}finally {
			System.setErr(old);
		}
	}
	///////////////////////////////////////////////////////////
	
	
	@Test
	public void concFromJarArglessMain() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		
		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);
		
		Files.createDirectory(mockFL.fs.getPath("/bin"));
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("def main() => System.err.println('hey there')"), StandardCharsets.UTF_8);
		
		Concc concc = new Concc("-jar myJar[hg.MyClass] -d /bin -a /work", mockFL, mockWriter);
		
		TestCase.assertEquals("", concc.doit());
		TestCase.assertTrue(Files.exists(mockFL.fs.getPath("/bin/myJar.jar")));
		
		Conc conc = new Conc("/bin/myJar", mockFL);//ok
		checkOutput(conc, "hey there");
	}
	
	@Test
	public void concFromJarMainCmdLineArgs() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		
		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);

		Files.createDirectory(mockFL.fs.getPath("/bin"));
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("def main(args String[]) => System.err.println('got: ' + args)"), StandardCharsets.UTF_8);

		Concc concc = new Concc("-jar myJar[hg.MyClass] -d /bin -a /work", mockFL, mockWriter);
		
		TestCase.assertEquals("", concc.doit());
		TestCase.assertTrue(Files.exists(mockFL.fs.getPath("/bin/myJar.jar")));

		Conc conc = new Conc("/bin/myJar one two three", mockFL);//ok
		checkOutput(conc, "got: [one two three]");
	}
	
	@Test
	public void concFromJarNoMainMethod() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		
		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);
		
		Files.createDirectory(mockFL.fs.getPath("/bin"));
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("System.err.println('ran ok')"), StandardCharsets.UTF_8);
		
		Concc concc = new Concc("-jar myJar[hg.MyClass] -d /bin -a /work", mockFL, mockWriter);
		
		TestCase.assertEquals("", concc.doit());
		//BytecodePrettyPrinter.print(Files.readAllBytes(mockFL.fs.getPath("/bin/hg/MyClass.class")), true);
		TestCase.assertTrue(Files.exists(mockFL.fs.getPath("/bin/myJar.jar")));
		
		Conc conc = new Conc("/bin/myJar one two three", mockFL);//ok
		checkOutput(conc, "ran ok");
	}
	
	@Test
	public void concNoMainMethod() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		
		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);

		Files.createDirectory(mockFL.fs.getPath("/bin"));
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("System.err.println('ran ok')"), StandardCharsets.UTF_8);

		Concc concc = new Concc("-d /bin -a /work", mockFL, mockWriter);
		
		TestCase.assertEquals("", concc.doit());

		Conc conc = new Conc("-cp /bin hg.MyClass one two three", mockFL);//ok
		checkOutput(conc, "ran ok");
	}
	
	@Test
	public void runServerMode() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		
		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);

		Files.createDirectory(mockFL.fs.getPath("/bin"));
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("System.err.println('ran ok')"), StandardCharsets.UTF_8);

		Concc concc = new Concc("-d /bin -a /work", mockFL, mockWriter);
		
		TestCase.assertEquals("", concc.doit());

		Conc conc = new Conc("-s -cp /bin hg.MyClass one two three", mockFL);//ok
		conc.shutdown();
		checkOutput(conc, "ran ok");
	}
}