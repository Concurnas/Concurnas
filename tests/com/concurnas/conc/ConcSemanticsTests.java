package com.concurnas.conc;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.concurnas.concc.Concc;
import com.concurnas.concc.utils.ConccTestMockFileLoader;
import com.concurnas.concc.utils.ConccTestMockFileWriter;
import com.concurnas.runtime.ConcurnasClassLoader;
import com.concurnas.runtime.Pair;
import com.concurnas.compiler.bytecode.BytecodeTests.InMemoryClassLoader;
import com.concurnas.compiler.utils.ListMaker;

import junit.framework.TestCase;

public class ConcSemanticsTests {
	private static class CLProviderToIMM implements ClassLoaderProvider{
		
		public HashMap<String, byte[]> nameToCode = new HashMap<String, byte[]>();
		
		public void toAdd(String name, byte[] code) {
			nameToCode.put(name, code);
		}
		
		@Override
		public ConcurnasClassLoader apply(Path[] classes, Path[] bootstrap) {
			InMemoryClassLoader ret = new InMemoryClassLoader(classes, bootstrap);
			ret.nameToCode.putAll(nameToCode);
			return ret;
		}
	}
	
	private Pair<ConccTestMockFileLoader, CLProviderToIMM> createMFL(String rootx, String dir, String fname, String code) throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);

		Path root = mockFL.fs.getPath(dir);
		Files.createDirectory(root);
		
		Path myClass = root.resolve(fname + ".conc"); 
		Files.write(myClass, ListMaker.make(code), StandardCharsets.UTF_8);
		Concc concc = new Concc(rootx, mockFL, mockWriter);
		
		TestCase.assertEquals("", concc.doit());
		
		CLProviderToIMM provider = new CLProviderToIMM();
		provider.toAdd(dir.substring(rootx.length()+1) + "." + fname, Files.readAllBytes(mockFL.fs.getPath(dir + "/" + fname + ".class")));
		
		return new Pair<ConccTestMockFileLoader, CLProviderToIMM>(mockFL, provider);
	}
	

	
	
	@Test
	public void simpleValidClassRequest() throws Exception {
		String code = "def main() void => System.err.println('hey')";
		
		Pair<ConccTestMockFileLoader, CLProviderToIMM> mAndP = createMFL("/work", "/work/hg", "MyFirstClass", code);
		Conc concc = new Conc("/work/hg/MyFirstClass.class", mAndP.getA(), mAndP.getB());//ok
		
		TestCase.assertNull(concc.validateConcInstance(concc.getConcInstance()).validationErrs);
	}
	
	
	@Test
	public void simpleValidClassRequestImplicitClass() throws IOException {
		String code = "def main() void => System.err.println('hey')";
		
		Pair<ConccTestMockFileLoader, CLProviderToIMM> mAndP = createMFL("/work", "/work/hg", "MyFirstClass", code);
		Conc concc = new Conc("/work/hg/MyFirstClass", mAndP.getA(), mAndP.getB());//implicit class ref
		
		TestCase.assertNull(concc.validateConcInstance(concc.getConcInstance()).validationErrs);
	}
	
	
	@Test
	public void invalidClassPathEntry() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		Path workDir = mockFL.fs.getPath("/work");
		Files.createDirectories(workDir);
		Files.createFile(mockFL.fs.getPath("myfile.class"));
		Conc concc = new Conc("-cp /work;/nonExist.class;/work/myfile.class;/work/thing.jar /work/MyFirstClass", mockFL);
		
		String got = concc.validateConcInstance(concc.getConcInstance()).validationErrs;
		String expect = "Classpath entry '/nonExist.class' does not exist\n" + 
				"Invalid classpath entry '/work/myfile.class', only directories or jar files can be class path entries\n" + 
				"Classpath entry '/work/thing.jar' does not exist\n" + 
				"Cannot find entry-point class to load: /work/MyFirstClass";
		
		TestCase.assertEquals(expect, got);
	}
	

	
	@Test
	public void simpleFileAmbigious() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		Path workDir = mockFL.fs.getPath("/work");
		Files.createDirectories(workDir);
		Files.createFile(mockFL.fs.getPath("MyFirstClass.class"));
		Files.createFile(mockFL.fs.getPath("MyFirstClass.jar"));
		Conc concc = new Conc("/work/MyFirstClass", mockFL);//ok
		
		TestCase.assertEquals("Ambigious source file refernce, /work/MyFirstClass.jar and /work/MyFirstClass.class found", concc.validateConcInstance(concc.getConcInstance()).validationErrs);
	}
	
	@Test
	public void noServerModeInREPL() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		Conc concc = new Conc("-s", mockFL);//ok
		
		TestCase.assertEquals("Server mode is applicable only when run in non-interactive mode", concc.validateConcInstance(concc.getConcInstance()).validationErrs);
	}
	
	@Test
	public void bcWerrorOnlyInREPLMode() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		Conc concc = new Conc("-bc -werror myfile", mockFL);//ok
		
		String expect = "-bc: Print bytecode option is applicable only when run in REPL mode\n"+
		"-werror: Treat warnings as errors option is applicable only when run in REPL mode\n"+
		"Cannot find entry-point class to load: myfile";
		
		TestCase.assertEquals(expect, concc.validateConcInstance(concc.getConcInstance()).validationErrs);
	}
	
	@Test
	public void bcWerrorOnlyInREPLModeOK() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		Conc concc = new Conc("-bc -werror", mockFL);//ok
		
		TestCase.assertEquals(null, concc.validateConcInstance(concc.getConcInstance()).validationErrs);
	}
	
	@Test
	public void cannotFindJar() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		Conc concc = new Conc("thing.jar", mockFL);//ok
		
		TestCase.assertEquals("Cannot find entry point file: thing.jar", concc.validateConcInstance(concc.getConcInstance()).validationErrs);
	}
	
	
	@Test
	public void noManifestDefinedInJar() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		
		Path jarPath = mockFL.fs.getPath("thing.jar");
		URI jarUri = URI.create("jar:" + jarPath.toUri());
		
		Map<String, String> env = new HashMap<String, String>(); 
		env.put("create", "true");
		FileSystems.newFileSystem(jarUri, env).close();
		/*try (FileSystem zipfs = FileSystems.newFileSystem(jarUri, env)) {
    	   
       }*/
		
		Conc concc = new Conc("thing.jar", mockFL);//ok
		
		TestCase.assertEquals("Manifest file META-INF/MANIFEST.MF is missing from jar: thing.jar", concc.validateConcInstance(concc.getConcInstance()).validationErrs);
	}
	
	
	@Test
	public void mainifestMissingEntryPointJar() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		
		Path jarPath = mockFL.fs.getPath("thing.jar");
		URI jarUri = URI.create("jar:" + jarPath.toUri());
		
		Map<String, String> env = new HashMap<String, String>();
		env.put("create", "true");
		try (FileSystem zipfs = FileSystems.newFileSystem(jarUri, env)) {
			Files.createDirectories(zipfs.getPath("/META-INF/"));
			Path manifest = zipfs.getPath("/META-INF/MANIFEST.MF");
			Files.write(manifest, ListMaker.make(""), StandardCharsets.UTF_8);
		}
		
		Conc concc = new Conc("thing.jar", mockFL);// ok
		
		TestCase.assertEquals("Manifest file META-INF/MANIFEST.MF is missing 'Main-Class' entry", concc.validateConcInstance(concc.getConcInstance()).validationErrs);
	}
	
	@Test
	public void manifestPointsToSomethingWeird() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();

		Path jarPath = mockFL.fs.getPath("thing.jar");
		URI jarUri = URI.create("jar:" + jarPath.toUri());

		Map<String, String> env = new HashMap<String, String>();
		env.put("create", "true");
		try (FileSystem zipfs = FileSystems.newFileSystem(jarUri, env)) {
			Files.createDirectories(zipfs.getPath("/META-INF/"));
			Path manifest = zipfs.getPath("/META-INF/MANIFEST.MF");
			Files.write(manifest, ListMaker.make("Manifest-Version: 1.0", "Main-Class: asdasd.asd"), StandardCharsets.UTF_8);
		}

		Conc concc = new Conc("thing.jar", mockFL);// ok

		TestCase.assertEquals("Cannot find entry-point class to load: asdasd.asd specified in manifest of: thing.jar", concc.validateConcInstance(concc.getConcInstance()).validationErrs);
	}
}