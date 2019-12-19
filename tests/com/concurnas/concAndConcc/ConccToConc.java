package com.concurnas.concAndConcc;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.concurnas.compiler.utils.ListMaker;
import com.concurnas.conc.Conc;
import com.concurnas.conc.ConcExeTests;
import com.concurnas.concc.Concc;
import com.concurnas.concc.utils.Utils;

import junit.framework.TestCase;

/**
 * Use subprocesses to test concc compilation with execution via conc 
 *
 */
public class ConccToConc {
	public static void subprocExec(Class<?> exeCls, String cmd, int expectedEV, String expectedOutput, boolean shouldterm, Path tempDirWithPrefix) {
					
		try {
			String javaHome = System.getProperty("java.home");
			String javaBin = javaHome + File.separator + "bin" + File.separator + "java";

			String classpath = System.getProperty("java.class.path");
			//String className = com.concurnas.concc.Concc.class.getName();
			String className = exeCls.getName();

			RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
			List<String> jvmArgs = bean.getInputArguments();
			
			if(tempDirWithPrefix != null) {
				jvmArgs = jvmArgs.stream().map(a -> a.replace("./installed", "../../installed")).collect(Collectors.toList());
			}
			
			//javaBin, xAJVMArgs, "-cp", classpath, className, cmd
			List<String> procCmd = new ArrayList<String>(5 + jvmArgs.size());
			procCmd.add(javaBin);
			procCmd.addAll(jvmArgs);
			if(tempDirWithPrefix != null) {
				procCmd.add("-Dcom.concurnas.rtCache=../../installed");
			}
			procCmd.add("-cp");
			procCmd.add(classpath);
			procCmd.add(className);
			procCmd.add(cmd);
			
			ProcessBuilder builder = new ProcessBuilder(procCmd);
			
			if(tempDirWithPrefix != null) {
				builder.directory(tempDirWithPrefix.toFile());
			}
			
			builder
			    .redirectInput(ProcessBuilder.Redirect.INHERIT)
			    .redirectOutput(ProcessBuilder.Redirect.INHERIT);
			//builder.inheritIO();
			Process process = builder.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ( (line = reader.readLine()) != null) {
				sb.append(line);
				sb.append(System.getProperty("line.separator"));
				if(shouldterm && expectedOutput != null &&  sb.toString().trim().equals(expectedOutput)) {
					break;
				}
			}
			String result = sb.toString().trim();
			if(shouldterm) {
				TestCase.assertTrue(process.isAlive());
				process.destroy();
				TestCase.assertFalse(process.isAlive());
			}

			process.waitFor();
			TestCase.assertEquals(expectedEV, process.exitValue());
			
			if(null != expectedOutput) {
				TestCase.assertEquals(expectedOutput, result);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
				
		return;
	}
	
	
	@Test
	public void testConcctoConcViaJarNoSubproc() throws Throwable {
		
		Path tempDirWithPrefix = Files.createTempDirectory("c2cJar");
		try {
			Path bindir = tempDirWithPrefix.resolve("bin");
			Files.createDirectories(bindir);
			Path srcdir = tempDirWithPrefix.resolve("src");
			Files.createDirectories(srcdir);
			
			Path myClass = srcdir.resolve("MyClass.conc"); 
			Files.write(myClass, ListMaker.make("def main() => System.err.println('hey there')"), StandardCharsets.UTF_8);
			
			Concc concc = new Concc(String.format("-jar myJar[MyClass] -d %s %s", bindir, srcdir));
			
			TestCase.assertEquals("", concc.doit());
			//BytecodePrettyPrinter.print(Files.readAllBytes(mockFL.fs.getPath("/bin/hg/MyClass.class")), true);
			//TestCase.assertTrue(Files.exists(bindir.resolve("MyClass.class")));
			Path outputJar = bindir.resolve("myJar.jar");
			
			Conc conc = new Conc(String.format("%s", outputJar));//execute
			ConcExeTests.checkOutput(conc, "hey there");
			
		}finally {
			Utils.deleteDirectory(tempDirWithPrefix.toFile());
		}
	}
	
	@Test
	public void testConcctoConcViaJar() throws Throwable {
		
		Path tempDirWithPrefix = Files.createTempDirectory("c2cJar");
		try {
			Path bindir = tempDirWithPrefix.resolve("bin");
			Files.createDirectories(bindir);
			Path srcdir = tempDirWithPrefix.resolve("src");
			Files.createDirectories(srcdir);
			
			Path myClass = srcdir.resolve("MyClass.conc"); 
			Files.write(myClass, ListMaker.make("def main() => System.err.println('hey there')"), StandardCharsets.UTF_8);
			
			subprocExec(Concc.class, String.format("-jar myJar[MyClass] -d %s %s", bindir, srcdir), 0, null, false, null);
			Path outputJar = bindir.resolve("myJar.jar");
			
			subprocExec(Conc.class, String.format("%s", outputJar), 0, "hey there", false, null);
			
		}finally {
			Utils.deleteDirectory(tempDirWithPrefix.toFile());
		}
	}
	
	@Test
	public void testConcctoConcViaJarNoFilePostfix() throws Throwable {
		
		Path tempDirWithPrefix = Files.createTempDirectory("c2cJar");
		try {
			Path myJardir = tempDirWithPrefix.resolve("myJar");
			Files.createDirectories(myJardir );
			
			Path myClass = tempDirWithPrefix.resolve("MyClass.conc");
			Files.write(myClass, ListMaker.make("def main() => System.err.println('hey there')"), StandardCharsets.UTF_8);
			
			subprocExec(Concc.class, "-jar myJar[MyClass] -d . MyClass.conc", 0, null, false, tempDirWithPrefix);

			subprocExec(Conc.class, "myJar", 0, "hey there", false, tempDirWithPrefix);
			
		}finally {
			Utils.deleteDirectory(tempDirWithPrefix.toFile());
		}
	}
	
	
	
	@Test
	public void testOneLineFileNoSubproc() throws Throwable {
		
		Path tempDirWithPrefix = Files.createTempDirectory("c2cJar");
		try {
			Path bindir = tempDirWithPrefix.resolve("bin");
			Files.createDirectories(bindir);
			Path srcdir = tempDirWithPrefix.resolve("src");
			Files.createDirectories(srcdir);
			
			Path myClass = srcdir.resolve("MyClass.conc"); 
			Files.write(myClass, ListMaker.make("def main() => System.err.println('hey there')"), StandardCharsets.UTF_8);
			
			Concc concc = new Concc(String.format("-d %s %s", bindir, srcdir));
			
			TestCase.assertEquals("", concc.doit());
			
			Path myclass = bindir.resolve(Paths.get("MyClass.class"));
			
			Conc conc = new Conc(myclass.toString());//execute
			ConcExeTests.checkOutput(conc, "hey there");
			
		}finally {
			Utils.deleteDirectory(tempDirWithPrefix.toFile());
		}
	}
	
	@Test
	public void testOneLineFile() throws Throwable {
		
		Path tempDirWithPrefix = Files.createTempDirectory("c2cJar");
		try {
			Path bindir = tempDirWithPrefix.resolve("bin");
			Files.createDirectories(bindir);
			Path srcdir = tempDirWithPrefix.resolve("src");
			Files.createDirectories(srcdir);
			
			Path myClass = srcdir.resolve("MyClass.conc"); 
			Files.write(myClass, ListMaker.make("def main() => System.err.println('hey there')"), StandardCharsets.UTF_8);
			
			subprocExec(Concc.class, String.format("-d %s %s", bindir, srcdir), 0, null, false, null);
			Path myclass = bindir.resolve(Paths.get("MyClass.class"));
			subprocExec(Conc.class, myclass.toString(), 0, "hey there", false, null);
			
		}finally {
			Utils.deleteDirectory(tempDirWithPrefix.toFile());
		}
	}
	
	////////////////
	@Test
	public void testSingleFile() throws Throwable {
		
		Path tempDirWithPrefix = Files.createTempDirectory("c2cJar");
		try {
			
			Path myClass = tempDirWithPrefix.resolve("MyClass.conc"); 
			Files.write(myClass, ListMaker.make("def main() => System.err.println('hey there')"), StandardCharsets.UTF_8);
			
			subprocExec(Concc.class, String.format("%s", myClass), 0, null, false, null);
			Path myclass = tempDirWithPrefix.resolve(Paths.get("MyClass.class"));
			subprocExec(Conc.class, myclass.toString(), 0, "hey there", false, null);
			
		}finally {
			Utils.deleteDirectory(tempDirWithPrefix.toFile());
		}
	}
	
	////////
	@Test
	public void oneLinerWithDepsNoSupProc() throws Throwable {
		
		Path tempDirWithPrefix = Files.createTempDirectory("c2cJar");
		try {
			Path bindir = tempDirWithPrefix.resolve("bin");
			Files.createDirectories(bindir);
			
			Path srcdir = tempDirWithPrefix.resolve("src");
			Files.createDirectories(srcdir.resolve("com/mycompany/myproject/utils"));
			
			Path myClass = srcdir.resolve("com/mycompany/myproject/MyClass.conc"); 
			Files.write(myClass, ListMaker.make("def main() => xx = com.mycompany.myproject.utils.Deps.getMessage(); System.err.println(xx)"), StandardCharsets.UTF_8);
			
			Path depscls = srcdir.resolve("com/mycompany/myproject/utils/Deps.conc"); 
			Files.write(depscls, ListMaker.make("def getMessage() => 'hey there'"), StandardCharsets.UTF_8);
			
			
			Concc concc = new Concc(String.format("-d %s %s", bindir, srcdir));
			
			TestCase.assertEquals("", concc.doit());
			
			Path myclass = bindir.resolve(Paths.get("com/mycompany/myproject/MyClass.class"));
			
			Conc conc = new Conc(String.format("-cp %s %s", bindir, myclass) );//execute
			ConcExeTests.checkOutput(conc, "hey there");
			
		}finally {
			Utils.deleteDirectory(tempDirWithPrefix.toFile());
		}
	}

	@Test
	public void oneLinerWithDeps() throws Throwable {
		Path tempDirWithPrefix = Files.createTempDirectory("c2cJar");
		try {
			Path bindir = tempDirWithPrefix.resolve("bin");
			Files.createDirectories(bindir);
			
			Path srcdir = tempDirWithPrefix.resolve("src");
			Files.createDirectories(srcdir.resolve("com/mycompany/myproject/utils"));
			
			Path myClass = srcdir.resolve("com/mycompany/myproject/MyClass.conc"); 
			Files.write(myClass, ListMaker.make("def main() => xx = com.mycompany.myproject.utils.Deps.getMessage(); System.err.println(xx)"), StandardCharsets.UTF_8);
			
			Path depscls = srcdir.resolve("com/mycompany/myproject/utils/Deps.conc"); 
			Files.write(depscls, ListMaker.make("def getMessage() => 'hey there'"), StandardCharsets.UTF_8);
			
			
			subprocExec(Concc.class, String.format("-d %s %s", bindir, srcdir), 0, null, false, null);

			Path myclass = bindir.resolve(Paths.get("com/mycompany/myproject/MyClass.class"));
			
			subprocExec(Conc.class, String.format("-cp %s %s", bindir, myclass), 0, "hey there", false, null);
			
		}finally {
			Utils.deleteDirectory(tempDirWithPrefix.toFile());
		}
	}
	
	
	@Test
	public void oneLinerWithDepsAutoIncludeDir() throws Throwable {
		Path tempDirWithPrefix = Files.createTempDirectory("c2cJar");
		try {
			
			Path myClass = tempDirWithPrefix.resolve("MyClass.conc"); 
			Files.write(myClass, ListMaker.make("def main() => xx = Deps.getMessage(); System.err.println(xx)"), StandardCharsets.UTF_8);
			
			Path depscls = tempDirWithPrefix.resolve("Deps.conc"); 
			Files.write(depscls, ListMaker.make("def getMessage() => 'hey there'"), StandardCharsets.UTF_8);
			
			
			subprocExec(Concc.class, "./", 0, null, false, tempDirWithPrefix);

			
			subprocExec(Conc.class, "MyClass", 0, "hey there", false, tempDirWithPrefix);
			
		}finally {
			Utils.deleteDirectory(tempDirWithPrefix.toFile());
		}
	}
	
	

	////////
	
	@Test
	public void oneLinerWithDepsJARNoSupProc() throws Throwable {
		
		Path tempDirWithPrefix = Files.createTempDirectory("c2cJar");
		try {
			Path bindir = tempDirWithPrefix.resolve("bin");
			Files.createDirectories(bindir);
			
			Path srcdir = tempDirWithPrefix.resolve("src");
			Files.createDirectories(srcdir.resolve("com/mycompany/myproject/utils"));
			
			Path myClass = srcdir.resolve("com/mycompany/myproject/MyClass.conc"); 
			Files.write(myClass, ListMaker.make("def main() => xx = com.mycompany.myproject.utils.Deps.getMessage(); System.err.println(xx)"), StandardCharsets.UTF_8);
			
			Path depscls = srcdir.resolve("com/mycompany/myproject/utils/Deps.conc"); 
			Files.write(depscls, ListMaker.make("def getMessage() => 'hey there'"), StandardCharsets.UTF_8);
			
			
			Concc concc = new Concc(String.format("-jar myjar[com.mycompany.myproject.MyClass] -d %s %s", bindir, srcdir));
			
			TestCase.assertEquals("", concc.doit());
			
			Path myjar = bindir.resolve(Paths.get("myjar.jar"));
			
			Conc conc = new Conc(myjar.toString() );//execute
			ConcExeTests.checkOutput(conc, "hey there");
			
		}finally {
			Utils.deleteDirectory(tempDirWithPrefix.toFile());
		}
	}
	
	
	@Test
	public void oneLinerWithDepsJAR() throws Throwable {
		Path tempDirWithPrefix = Files.createTempDirectory("c2cJar");
		try {
			Path bindir = tempDirWithPrefix.resolve("bin");
			Files.createDirectories(bindir);
			
			Path srcdir = tempDirWithPrefix.resolve("src");
			Files.createDirectories(srcdir.resolve("com/mycompany/myproject/utils"));
			
			Path myClass = srcdir.resolve("com/mycompany/myproject/MyClass.conc"); 
			Files.write(myClass, ListMaker.make("def main() => xx = com.mycompany.myproject.utils.Deps.getMessage(); System.err.println(xx)"), StandardCharsets.UTF_8);
			
			Path depscls = srcdir.resolve("com/mycompany/myproject/utils/Deps.conc"); 
			Files.write(depscls, ListMaker.make("def getMessage() => 'hey there'"), StandardCharsets.UTF_8);
			
			
			subprocExec(Concc.class, String.format("-jar myjar[com.mycompany.myproject.MyClass] -d %s %s", bindir, srcdir), 0, null, false, null);

			Path myjar = bindir.resolve(Paths.get("myjar.jar"));
			
			subprocExec(Conc.class, myjar.toString(), 0, "hey there", false, null);
			
		}finally {
			Utils.deleteDirectory(tempDirWithPrefix.toFile());
		}
	}
	
	
	public void testRunProg(boolean subproc, String expected, String prog) throws Throwable {

		Path tempDirWithPrefix = Files.createTempDirectory("c2cJar");
		try {
			Path bindir = tempDirWithPrefix.resolve("bin");
			Files.createDirectories(bindir);
			Path srcdir = tempDirWithPrefix.resolve("src");
			Files.createDirectories(srcdir);
			
			Path myClass = srcdir.resolve("MyClass.conc"); 
			Files.write(myClass, ListMaker.make(prog), StandardCharsets.UTF_8);
			
			if(subproc) {
				
				subprocExec(Concc.class, String.format("-jar myJar[MyClass] -d %s %s", bindir, srcdir), 0, null, false, null);

				Path myjar = bindir.resolve(Paths.get("myjar.jar"));
				
				subprocExec(Conc.class, String.format("%s some args", myjar), 0, expected, false, null);
				
			}else {
				Concc concc = new Concc(String.format("-jar myJar[MyClass] -d %s %s", bindir, srcdir));
				
				TestCase.assertEquals("", concc.doit());
				
				Path outputJar = bindir.resolve("myJar.jar");
				
				Conc conc = new Conc(String.format("%s some args", outputJar));//execute
				ConcExeTests.checkOutput(conc, expected);
			}
			
		}finally {
			Utils.deleteDirectory(tempDirWithPrefix.toFile());
		}
	}
	
	@Test
	public void testServerMode() throws Throwable {
		Path tempDirWithPrefix = Files.createTempDirectory("c2cJar");
		try {
			Path bindir = tempDirWithPrefix.resolve("bin");
			Files.createDirectories(bindir);
			
			Path srcdir = tempDirWithPrefix.resolve("src");
			Files.createDirectories(srcdir.resolve("com/mycompany/myproject/utils"));
			
			Path myClass = srcdir.resolve("com/mycompany/myproject/MyClass.conc"); 
			Files.write(myClass, ListMaker.make("def main() => xx = com.mycompany.myproject.utils.Deps.getMessage(); System.err.println(xx)"), StandardCharsets.UTF_8);
			
			Path depscls = srcdir.resolve("com/mycompany/myproject/utils/Deps.conc"); 
			Files.write(depscls, ListMaker.make("def getMessage() => 'hey there'"), StandardCharsets.UTF_8);
			
			
			subprocExec(Concc.class, String.format("-jar myjar[com.mycompany.myproject.MyClass] -d %s %s", bindir, srcdir), 0, null, false, null);

			Path myjar = bindir.resolve(Paths.get("myjar.jar"));
			
			subprocExec(Conc.class, "-s " + myjar.toString(), 1, "hey there", true, null);
			
		}finally {
			Utils.deleteDirectory(tempDirWithPrefix.toFile());
		}
	}
	
	@Test
	public void testCurrentdir() throws Throwable {
		//treat '.' as './'
		Path tempDirWithPrefix = Files.createTempDirectory("c2cJar");
		try {
			
			Path myClass = tempDirWithPrefix.resolve("hello.conc"); 
			Files.write(myClass, ListMaker.make("class Proc{def doIt(args String[])void { System.err.println('hello' + args); }} def main(args String[]) => new Proc().doIt(args);; "), StandardCharsets.UTF_8);
			
			Concc concc = new Concc(String.format("-d . -jar myjar[hello] hello.conc"));
			TestCase.assertEquals("Unable to resolve source file/directory of: '.\\hello.conc' \nNo .conc files found to compile", concc.doit());
		}finally {
			Utils.deleteDirectory(tempDirWithPrefix.toFile());
		}
	}
	
	@Test
	public void testConcctoConccIsolates() throws Throwable {
		testRunProg(false, "hey there:", "def main(){ x = {'there'}!; System.err.println('hey {x}') }");
	}
	
	@Test
	public void testConcctoConccIsolatesSubProc() throws Throwable {
		testRunProg(true, "hey there:", "def main(){ x = {'there'}!; System.err.println('hey {x}') }");
	}
	
	@Test
	public void testConcctoConccIsolatesWithMainArgs() throws Throwable {
		testRunProg(false, "hey there: [some args]", "def main(args String[]){ x = {'there'}!; System.err.println('hey {x} {args}') }");
	}
	
	@Test
	public void testConcctoConccIsolatesWithMainArgsSubProc() throws Throwable {
		testRunProg(true, "hey there: [some args]", "def main(args String[]){ x = {'there'}!; System.err.println('hey {x} {args}') }");
	}
}
