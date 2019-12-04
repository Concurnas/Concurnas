package com.concurnas.concc;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Test;

import com.concurnas.compiler.bytecode.BytecodeTests;
import com.concurnas.compiler.bytecode.BytecodeTests.InMemoryClassLoader;
import com.concurnas.compiler.bytecode.BytecodeTests.SchedulClsAndObj;
import com.concurnas.compiler.utils.ListMaker;
import com.concurnas.concc.utils.ConccTestMockFileLoader;
import com.concurnas.concc.utils.ConccTestMockFileWriter;
import com.concurnas.concc.utils.Utils;
import com.concurnas.runtime.ClassPathUtils;
import com.concurnas.runtime.ConcurnasClassLoader;
import com.concurnas.runtime.Pair;

import junit.framework.TestCase;

public class ConccCompileTests {

	private static class Executor{
		private InMemoryClassLoader imcl;
		private ConcurnasClassLoader masterLoader;
		public Executor() {
			masterLoader = new ConcurnasClassLoader(ClassPathUtils.getSystemClassPathAsPaths(), ClassPathUtils.getInstallationPath());
			imcl = new InMemoryClassLoader(masterLoader);
		}
		
		public Executor addToExecute(String name, byte[] code) throws Exception {
			imcl.loadClass(name, code);
			return this;
		}
		
		private String execute(String name, byte[] code) throws Throwable {
			
			imcl.loadClass(name, code);
			
			SchedulClsAndObj shh = BytecodeTests.makeSchedulClsAndObj(masterLoader);
			
			String res = BytecodeTests.executeClass(imcl, null, name, false, new ArrayList<String>(), shh);
			return res;
		}
	}
	
	@Test
	public void simpleCompile() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);

		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("def doings() => 'hi there'"), StandardCharsets.UTF_8);
		Concc concc = new Concc("/work", mockFL, mockWriter);
		
		TestCase.assertEquals("", concc.doit());
		
		byte[] code = Files.readAllBytes(mockFL.fs.getPath("/work/hg/MyClass.class"));
		
		String res = new Executor().execute("hg.MyClass", code);
		
		TestCase.assertEquals("hi there", res);
	}
	
	@Test
	public void overrideOutputDir() throws Throwable {
		
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);

		Path root = mockFL.fs.getPath("/work/hg");
		Path clsesdir = mockFL.fs.getPath("/classes");
		Files.createDirectory(root);
		Files.createDirectories(clsesdir);
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("def doings() => 'hi there'"), StandardCharsets.UTF_8);
		Concc concc = new Concc("-d /classes /work", mockFL, mockWriter);
		
		TestCase.assertEquals("", concc.doit());
		
		byte[] code = Files.readAllBytes(mockFL.fs.getPath("/classes/hg/MyClass.class"));
		
		String res = new Executor().execute("hg.MyClass", code);
		
		TestCase.assertEquals("hi there", res);
	}
	
	@Test
	public void globalRootOverride() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);

		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("def doings() => 'hi there'"), StandardCharsets.UTF_8);
		Concc concc = new Concc("-root /work /hg", mockFL, mockWriter);
		
		TestCase.assertEquals("", concc.doit());
		
		byte[] code = Files.readAllBytes(mockFL.fs.getPath("/work/hg/MyClass.class"));
		
		String res = new Executor().execute("hg.MyClass", code);
		
		TestCase.assertEquals("hi there", res);
	}
	
	@Test
	public void localRootOverride() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		
		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("def doings() => 'hi there'"), StandardCharsets.UTF_8);
		Concc concc = new Concc("/work[/hg]", mockFL, mockWriter);
		
		TestCase.assertEquals("", concc.doit());
		
		byte[] code = Files.readAllBytes(mockFL.fs.getPath("/work/hg/MyClass.class"));
		
		String res = new Executor().execute("hg.MyClass", code);
		
		TestCase.assertEquals("hi there", res);
	}
	
	@Test
	public void localRootWGlobalRootOverride() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		
		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);

		Path root2 = mockFL.fs.getPath("/work2");
		Files.createDirectory(root2);
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("def doings() => 'hi there'"), StandardCharsets.UTF_8);
		Concc concc = new Concc("-root /work2 /work[/hg]", mockFL, mockWriter);
		
		TestCase.assertEquals("", concc.doit());
		
		byte[] code = Files.readAllBytes(mockFL.fs.getPath("/work/hg/MyClass.class"));
		
		String res = new Executor().execute("hg.MyClass", code);
		
		TestCase.assertEquals("hi there", res);
	}
	
	@Test
	public void simpleCompileManyClass() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);

		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("class MySubClass{ def sayhi() { 'hi';} }def doings() => msc=MySubClass(); msc.sayhi()"), StandardCharsets.UTF_8);
		Concc concc = new Concc("/work", mockFL, mockWriter);
		
		TestCase.assertEquals("", concc.doit());
		
		byte[] code = Files.readAllBytes(mockFL.fs.getPath("/work/hg/MyClass.class"));
		byte[] codeDep = Files.readAllBytes(mockFL.fs.getPath("/work/hg/MyClass$MySubClass.class"));
		
		String res = new Executor().addToExecute("hg.MyClass$MySubClass", codeDep).execute("hg.MyClass", code);
		
		TestCase.assertEquals("hi", res);
	}
	

	@Test
	public void compileMultipleWithDependancies() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);

		Path root = mockFL.fs.getPath("/work/com/myorg");
		Files.createDirectories(root);
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("from com.myorg.code2 import myfunc; def doings() => myfunc() "), StandardCharsets.UTF_8);
		
		Path code2 = root.resolve("code2.conc"); 
		Files.write(code2, ListMaker.make("def myfunc() => 'hi there' "), StandardCharsets.UTF_8);
		
		Concc concc = new Concc("/work/[com/myorg/MyClass.conc]", mockFL, mockWriter);
		
		TestCase.assertEquals("", concc.doit());
		
		byte[] code = Files.readAllBytes(mockFL.fs.getPath("/work/com/myorg/MyClass.class"));
		byte[] codeDep = Files.readAllBytes(mockFL.fs.getPath("/work/com/myorg/code2.class"));
		
		//String res = new Executor().addToExecute("hg.MyClass$MySubClass", codeDep).execute("hg.MyClass", code);
		String res = new Executor().addToExecute("com.myorg.code2", codeDep).execute("com.myorg.MyClass", code);
		
		TestCase.assertEquals("hi there", res);
	}
	
	@Test
	public void compilationWithDuplicates() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);

		Path root = mockFL.fs.getPath("/work/com/myorg");
		Files.createDirectories(root);
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("from com.myorg.code2 import myfunc; def doings() => myfunc() "), StandardCharsets.UTF_8);
		
		Path code2 = root.resolve("code2.conc"); 
		Files.write(code2, ListMaker.make("def myfunc() => 'hi there' "), StandardCharsets.UTF_8);
		
		Concc concc = new Concc("/work/[com/myorg/MyClass.conc com/myorg/code2.conc]", mockFL, mockWriter);
		
		TestCase.assertEquals("", concc.doit());
		
		byte[] code = Files.readAllBytes(mockFL.fs.getPath("/work/com/myorg/MyClass.class"));
		byte[] codeDep = Files.readAllBytes(mockFL.fs.getPath("/work/com/myorg/code2.class"));
		
		//String res = new Executor().addToExecute("hg.MyClass$MySubClass", codeDep).execute("hg.MyClass", code);
		String res = new Executor().addToExecute("com.myorg.code2", codeDep).execute("com.myorg.MyClass", code);
		
		TestCase.assertEquals("hi there", res);
	}
	
	private ByteArrayOutputStream baos;
	private PrintStream ps;
	private final Charset charset = StandardCharsets.UTF_8;
	
	private PrintStream initPrintWriter() throws UnsupportedEncodingException {
		baos = new ByteArrayOutputStream();
		ps = new PrintStream(baos, true, charset.name());
		return ps;
		
	}
	
	private String getLastCallsToPrintStream() {
		String content = new String(baos.toByteArray(), charset);
		ps.close();
		return content;
	}
	
	@Test
	public void dontCompileSameDirStuffIfUnused() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);

		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("def doings() => 'hi there'"), StandardCharsets.UTF_8);
		Path myClass2 = root.resolve("MyClassIgnore.conc"); 
		Files.write(myClass2, ListMaker.make("irelevant = 99"), StandardCharsets.UTF_8);

		PrintStream ps = initPrintWriter();
		Concc concc = new Concc("/work[/hg/MyClass.conc]", mockFL, mockWriter);
		concc.setConsoleCaputure(ps);
		
		TestCase.assertEquals("", concc.doit());
		
		String consoleOutput = getLastCallsToPrintStream();

		String expectedConsole = "Finished compilation of: /work/hg/MyClass.conc -> /work/hg/MyClass.class [hg/MyClass]\r\n";
		TestCase.assertEquals(expectedConsole.trim(), consoleOutput.trim());
		
		byte[] code = Files.readAllBytes(mockFL.fs.getPath("/work/hg/MyClass.class"));
		
		String res = new Executor().execute("hg.MyClass", code);
		
		TestCase.assertEquals("hi there", res);
	}
	
	

	@Test
	public void captureErrors() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);

		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("def doings() => xxx"), StandardCharsets.UTF_8);
		Concc concc = new Concc("/work", mockFL, mockWriter);
		
		String expectErros = "/work/hg/MyClass.conc line 1:16 xxx cannot be resolved to a variable";
		
		TestCase.assertEquals(expectErros, concc.doit());
	}
	
	@Test
	public void captureSyntaxError() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);

		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("def warnme() if(true){a=9; } }\ndef doings() => 'hi'"), StandardCharsets.UTF_8);
		Concc concc = new Concc("/work", mockFL, mockWriter);
		
		TestCase.assertTrue(concc.doit().contains("1:29 extraneous input"));
	}
	
	@Test
	public void captureWarning() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);

		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("\nclass String{}\ndef doings() => 'hi there'"), StandardCharsets.UTF_8);
		Concc concc = new Concc("/work", mockFL, mockWriter);
		//check picks up line numbers too, oh it does!
		//also, ensure that auto imports cannot be overriten without a warning
		String expectErros = "/work/hg/MyClass.conc WARN line 2:0 Class name overwrites imported class: String";
		
		TestCase.assertEquals(expectErros, concc.doit());
		
		byte[] code = Files.readAllBytes(mockFL.fs.getPath("/work/hg/MyClass.class"));
		
		String res = new Executor().execute("hg.MyClass", code);
		
		TestCase.assertEquals("hi there", res);
	}
	
	@Test
	public void warningsAsErrors() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);

		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("\nclass String{}\ndef doings() => 'hi there'"), StandardCharsets.UTF_8);
		Concc concc = new Concc("-werror /work", mockFL, mockWriter);
		
		String expectErros = "/work/hg/MyClass.conc WARN line 2:0 Class name overwrites imported class: String";
		
		TestCase.assertEquals(expectErros, concc.doit());
	}
	
	@Test
	public void customClasspath() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		
		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);

		Path clsRoot = mockFL.fs.getPath("/classes");
		Files.createDirectories(clsRoot);

		Path dependancyCls = clsRoot.resolve("ConccDeps.class"); 
		
		byte[] realbc = Files.readAllBytes(Paths.get("./bin/test/com/concurnas/lang/precompiled/ConccDeps.class"));
		Files.write(dependancyCls, realbc);
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("from com.concurnas.lang.precompiled.ConccDeps import myString;\ndef doings() => 'hi there {myString}'"), StandardCharsets.UTF_8);
		Concc concc = new Concc("-cp /classes /work", mockFL, mockWriter);
		TestCase.assertEquals("", concc.doit());

		byte[] code = Files.readAllBytes(mockFL.fs.getPath("/work/hg/MyClass.class"));
		
		String res = new Executor().execute("hg.MyClass", code);
		
		TestCase.assertEquals("hi there conc dep String", res);
	}
	
	@Test
	public void copyResources() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		
		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);

		Files.createDirectory(mockFL.fs.getPath("/bin"));
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("def doings() => 'hi there'"), StandardCharsets.UTF_8);

		Path resrouce = root.resolve("resource.txt"); 
		Files.write(resrouce, ListMaker.make("def doings() => 'hi there'"), StandardCharsets.UTF_8);
		
		
		Concc concc = new Concc("-d /bin -a /work", mockFL, mockWriter);
		
		TestCase.assertEquals("", concc.doit());
		
		byte[] code = Files.readAllBytes(mockFL.fs.getPath("/bin/hg/MyClass.class"));
		String res = new Executor().execute("hg.MyClass", code);
		TestCase.assertEquals("hi there", res);

		byte[] copiedResource = Files.readAllBytes(mockFL.fs.getPath("/bin/hg/resource.txt"));
		TestCase.assertNotNull(copiedResource);
	}
	
	public static Pair<List<ZipEntry>, String> extractZipEntries(byte[] content) throws IOException {
		List<ZipEntry> entries = new ArrayList<>();
		
		byte[] buffer = new byte[2048];

		String mfest="";
		try ( ZipInputStream stream = new ZipInputStream(new ByteArrayInputStream(content))) {

			ZipEntry entry;
			while ((entry = stream.getNextEntry()) != null) {
				entries.add(entry);
				if(entry.toString().equals("META-INF/MANIFEST.MF")) {
					try (ByteArrayOutputStream bas = new ByteArrayOutputStream(); 
							BufferedOutputStream bos = new BufferedOutputStream(bas, buffer.length)) {
						int len;
						while ((len = stream.read(buffer)) > 0) {
							bos.write(buffer, 0, len);
						}
						bos.flush();
						mfest = bas.toString();
					}
				}
			}
		}
		
		return new Pair<List<ZipEntry>, String>(entries, mfest);
	}
	
	@Test
	public void copyResourcesMakeJar() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		
		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);

		Files.createDirectory(mockFL.fs.getPath("/bin"));
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("def doings() => 'hi there'"), StandardCharsets.UTF_8);

		Path resrouce = root.resolve("resource.txt"); 
		Files.write(resrouce, ListMaker.make("def doings() => 'hi there'"), StandardCharsets.UTF_8);
		
		Concc concc = new Concc("-jar myJar -d /bin -a /work", mockFL, mockWriter);
		
		TestCase.assertEquals("", concc.doit());
		
		byte[] code = Files.readAllBytes(mockFL.fs.getPath("/bin/hg/MyClass.class"));
		String res = new Executor().execute("hg.MyClass", code);
		TestCase.assertEquals("hi there", res);

		byte[] copiedResource = Files.readAllBytes(mockFL.fs.getPath("/bin/myJar.jar"));
		TestCase.assertNotNull(copiedResource);
		
		Pair<List<ZipEntry>, String> entriesAndMfest = extractZipEntries(copiedResource);
		List<ZipEntry> exntries = entriesAndMfest.getA();
		
		TestCase.assertEquals("[hg/MyClass.class, hg/resource.txt]", exntries.stream().filter(a -> !a.isDirectory()).map(a -> a.toString()).sorted().collect(Collectors.toList()).toString());
	}
	
	@Test
	public void copyResourcesMakeJarManifest() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		
		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);

		Files.createDirectory(mockFL.fs.getPath("/bin"));
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("def doings() => 'hi there'"), StandardCharsets.UTF_8);

		Path resrouce = root.resolve("resource.txt"); 
		Files.write(resrouce, ListMaker.make("def doings() => 'hi there'"), StandardCharsets.UTF_8);
		
		
		Concc concc = new Concc("-jar myJar[hg.MyClass] -d /bin -a /work", mockFL, mockWriter);
		
		
		TestCase.assertEquals("", concc.doit());
		
		byte[] code = Files.readAllBytes(mockFL.fs.getPath("/bin/hg/MyClass.class"));
		String res = new Executor().execute("hg.MyClass", code);
		TestCase.assertEquals("hi there", res);

		byte[] copiedResource = Files.readAllBytes(mockFL.fs.getPath("/bin/myJar.jar"));
		TestCase.assertNotNull(copiedResource);
		
		Pair<List<ZipEntry>, String> entriesAndMfest = extractZipEntries(copiedResource);
		List<ZipEntry> exntries = entriesAndMfest.getA();
		String ls = System.getProperty("line.separator");
		TestCase.assertEquals("[META-INF/MANIFEST.MF, hg/MyClass.class, hg/resource.txt]", exntries.stream().filter(a -> !a.isDirectory()).map(a -> a.toString()).sorted().collect(Collectors.toList()).toString());
		TestCase.assertEquals("Manifest-Version: 1.0"+ls+"Main-Class: hg.MyClass", entriesAndMfest.getB().trim());
	}
	
	@Test
	public void copyResourcesMakeJarManifestClean() throws Throwable {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		
		Path root = mockFL.fs.getPath("/work/hg");
		Files.createDirectory(root);

		Files.createDirectory(mockFL.fs.getPath("/bin"));
		
		Path myClass = root.resolve("MyClass.conc"); 
		Files.write(myClass, ListMaker.make("def doings() => 'hi there'"), StandardCharsets.UTF_8);

		Path resrouce = root.resolve("resource.txt"); 
		Files.write(resrouce, ListMaker.make("def doings() => 'hi there'"), StandardCharsets.UTF_8);
		
		
		Concc concc = new Concc("-clean -jar myJar[hg.MyClass] -d /bin -a /work", mockFL, mockWriter);
		
		TestCase.assertEquals("", concc.doit());
		TestCase.assertFalse(Files.exists(mockFL.fs.getPath("/bin/hg/MyClass.class")));
		TestCase.assertFalse(Files.exists(mockFL.fs.getPath("/bin/hg/resource.txt")));
		TestCase.assertTrue(Files.exists(mockFL.fs.getPath("/bin/myJar.jar")));
		
		byte[] copiedResource = Files.readAllBytes(mockFL.fs.getPath("/bin/myJar.jar"));
		TestCase.assertNotNull(copiedResource);
		
		Pair<List<ZipEntry>, String> entriesAndMfest = extractZipEntries(copiedResource);
		List<ZipEntry> exntries = entriesAndMfest.getA();
		String ls = System.getProperty("line.separator");
		TestCase.assertEquals("[META-INF/MANIFEST.MF, hg/MyClass.class, hg/resource.txt]", exntries.stream().filter(a -> !a.isDirectory()).map(a -> a.toString()).sorted().collect(Collectors.toList()).toString());
		TestCase.assertEquals("Manifest-Version: 1.0"+ls+"Main-Class: hg.MyClass", entriesAndMfest.getB().trim());
	}
	
	@Test
	public void testCreateJarWithCorrectPaths() throws Throwable {
		
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
			TestCase.assertTrue(Files.exists(bindir.resolve("MyClass.class")));
			Path outputJar = bindir.resolve("myJar.jar");
			TestCase.assertTrue(Files.exists(outputJar));
			
			Pair<List<ZipEntry>, String> entriesAndMfest = ConccCompileTests.extractZipEntries(Files.readAllBytes(outputJar));
			List<ZipEntry> exntries = entriesAndMfest.getA();
			
			//was incorrectly omitting abs path when checking roots previously...
			TestCase.assertEquals("[META-INF/MANIFEST.MF, MyClass.class]", exntries.stream().filter(a -> !a.isDirectory()).map(a -> a.toString()).sorted().collect(Collectors.toList()).toString());
		}finally {
			Utils.deleteDirectory(tempDirWithPrefix.toFile());
		}
	}
	
	@Test
	public void testCreateJarWithCorrectPathsTwice() throws Throwable {
		
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
			
			concc = new Concc(String.format("-jar myJar[MyClass] -d %s %s", bindir, srcdir));
			TestCase.assertEquals("", concc.doit());
			
			TestCase.assertTrue(Files.exists(bindir.resolve("MyClass.class")));
			Path outputJar = bindir.resolve("myJar.jar");
			TestCase.assertTrue(Files.exists(outputJar));
			
			Pair<List<ZipEntry>, String> entriesAndMfest = ConccCompileTests.extractZipEntries(Files.readAllBytes(outputJar));
			List<ZipEntry> exntries = entriesAndMfest.getA();
			
			//was incorrectly omitting abs path when checking roots previously...
			TestCase.assertEquals("[META-INF/MANIFEST.MF, MyClass.class]", exntries.stream().filter(a -> !a.isDirectory()).map(a -> a.toString()).sorted().collect(Collectors.toList()).toString());
			
		}finally {
			Utils.deleteDirectory(tempDirWithPrefix.toFile());
		}
	}
}