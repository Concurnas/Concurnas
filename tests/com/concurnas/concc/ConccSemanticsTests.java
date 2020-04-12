package com.concurnas.concc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import com.concurnas.concc.Concc.ValidationErrorsAndValidObject;
import com.concurnas.concc.utils.ConccTestMockFileLoader;
import com.concurnas.concc.utils.ConccTestMockFileWriter;

import junit.framework.TestCase;

public class ConccSemanticsTests {

	@Test
	public void outputDirAndSrc() {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		Concc concc = new Concc("-d c:/work", mockFL, mockWriter);
		
		String got = concc.validateConccInstance(concc.getConccInstance()).validationErrs;
		
		String expect = "There must be at least one source file or source directory specified\n" + 
				"No .conc files found to compile. Use: concc -help or concc --help for assistance.";
		
		TestCase.assertEquals(expect, got);
	}

	@Test
	public void outputDirOkNosources() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		Path workDir = mockFL.fs.getPath("work");
		Files.createDirectories(workDir);
		Concc concc = new Concc("-d work", mockFL, mockWriter);
		
		String got = concc.validateConccInstance(concc.getConccInstance()).validationErrs;
		
		String expect = "There must be at least one source file or source directory specified\n" + 
				"No .conc files found to compile. Use: concc -help or concc --help for assistance.";
		
		TestCase.assertEquals(expect, got);
	}
	
	@Test
	public void invalidClassPathEntry() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		Path workDir = mockFL.fs.getPath("/work");
		Files.createDirectories(workDir);
		Files.createFile(mockFL.fs.getPath("MyFirstClass.conc"));
		Files.createFile(mockFL.fs.getPath("myfile.class"));
		Concc concc = new Concc("-cp /work;/nonExist.class;/work/myfile.class;/work/thing.jar /work/MyFirstClass.conc", mockFL, mockWriter);
		
		String got = concc.validateConccInstance(concc.getConccInstance()).validationErrs;
		String expect = "Classpath entry '/nonExist.class' does not exist\n" + 
				"Invalid classpath entry '/work/myfile.class', only directories or jar files can be class path entries\n" + 
				"Classpath entry '/work/thing.jar' does not exist";
		
		TestCase.assertEquals(expect, got);
	}
	
	@Test
	public void classPathMissingFile() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		//Path workDir = mockFL.fs.getPath("work");
		//Files.createDirectories(workDir);
		Concc concc = new Concc("-cp work.jar;another.jar MyFirstClass.conc", mockFL, mockWriter);
		
		String got = concc.validateConccInstance(concc.getConccInstance()).validationErrs;
		String expect = "Classpath entry 'work.jar' does not exist\n" + 
				"Classpath entry 'another.jar' does not exist\n" + 
				"Unable to resolve source file/directory of: './MyFirstClass.conc' \n" + 
				"No .conc files found to compile. Use: concc -help or concc --help for assistance.";
		
		TestCase.assertEquals(expect, got);
	}
	
	@Test
	public void classpathOk() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		Path jar1 = mockFL.fs.getPath("work.jar");
		Path jar2 = mockFL.fs.getPath("another.jar");
		Path classx = mockFL.fs.getPath("/MyFirstClass.conc");
		Files.createFile(jar1);
		Files.createFile(jar2);
		Files.createFile(classx);
		
		Concc concc = new Concc("-cp work.jar;another.jar /MyFirstClass.conc", mockFL, mockWriter);
		
		String got = concc.validateConccInstance(concc.getConccInstance()).validationErrs;
		
		TestCase.assertEquals(null, got);
	}
	
	@Test
	public void allNeedsOutputDirOK() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		Path binDir = mockFL.fs.getPath("./bin");
		Files.createDirectories(binDir);
		Path jar1 = mockFL.fs.getPath("work.jar");
		Path jar2 = mockFL.fs.getPath("another.jar");
		Path classx = mockFL.fs.getPath("/MyFirstClass.conc");
		Files.createFile(jar1);
		Files.createFile(jar2);
		Files.createFile(classx);
		
		Concc concc = new Concc("-d ./bin -a -cp work.jar;another.jar /MyFirstClass.conc", mockFL, mockWriter);
		
		String got = concc.validateConccInstance(concc.getConccInstance()).validationErrs;
		
		TestCase.assertEquals(null, got);
	}
	
	@Test
	public void allNeedsOutputDir() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		Path jar1 = mockFL.fs.getPath("work.jar");
		Path jar2 = mockFL.fs.getPath("another.jar");
		Path classx = mockFL.fs.getPath("/MyFirstClass.conc");
		Files.createFile(jar1);
		Files.createFile(jar2);
		Files.createFile(classx);
		
		Concc concc = new Concc("-a -cp work.jar;another.jar /MyFirstClass.conc", mockFL, mockWriter);
		
		String got = concc.validateConccInstance(concc.getConccInstance()).validationErrs;
		
		TestCase.assertEquals("-a option may only be used when output directory is specified", got);
	}
	
	@Test
	public void jarDoesntNeedsOutputDir() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		Path jar1 = mockFL.fs.getPath("work.jar");
		Path jar2 = mockFL.fs.getPath("another.jar");
		Path classx = mockFL.fs.getPath("/MyFirstClass.conc");
		Files.createFile(jar1);
		Files.createFile(jar2);
		Files.createFile(classx);
		
		Concc concc = new Concc("-jar sdfsdf -cp work.jar;another.jar /MyFirstClass.conc", mockFL, mockWriter);
		
		ConccInstance  inst = concc.getConccInstance();
		String got = concc.validateConccInstance(inst).validationErrs;
		
		TestCase.assertEquals("-jar sdfsdf.jar -cp work.jar;another.jar /MyFirstClass.conc", inst.toString());
		TestCase.assertEquals("-jar option may only be used when output directory is specified", got);
	}
	
	@Test
	public void jarInfDir() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		Path jar1 = mockFL.fs.getPath("work.jar");
		Path jar2 = mockFL.fs.getPath("another.jar");
		Path classx = mockFL.fs.getPath("/MyFirstClass.conc");
		Files.createFile(jar1);
		Files.createFile(jar2);
		Files.createFile(classx);
		Files.createDirectories(mockFL.fs.getPath("/release"));
		
		Concc concc = new Concc("-d /release -jar sdfsdf -cp work.jar;another.jar /MyFirstClass.conc", mockFL, mockWriter);
		
		
		ValidationErrorsAndValidObject vvandv =  concc.validateConccInstance(concc.getConccInstance());
		
		TestCase.assertEquals(null, vvandv.validationErrs);
		TestCase.assertEquals("-d /release -jar /release/sdfsdf.jar -cp work.jar;another.jar /MyFirstClass.conc[ true]", vvandv.validconcObject.toString());
		
	}
	
	@Test
	public void overrideRootOk() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		Path workDir = mockFL.fs.getPath("work2");
		Files.createDirectories(workDir);
		Files.createFile(workDir.resolve("MyFirstClass.conc"));
		
		Concc concc = new Concc("-root work2 MyFirstClass.conc", mockFL, mockWriter);
		
		String got = concc.validateConccInstance(concc.getConccInstance()).validationErrs;
		
		TestCase.assertEquals(null, got);
	}
	
	
	@Test
	public void missingsrcLevelRootDir() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		/*Path workDir = mockFL.fs.getPath("work2");
		Files.createDirectories(workDir);
		Files.createFile(workDir.resolve("MyFirstClass.conc"));*/
		
		Concc concc = new Concc("work2[MyFirstClass.conc]", mockFL, mockWriter);
		
		String got = concc.validateConccInstance(concc.getConccInstance()).validationErrs;
		String expect = "Root directory: 'work2' for source element does not exist\n" + 
				"No .conc files found to compile. Use: concc -help or concc --help for assistance.";
		
		TestCase.assertEquals(expect, got);
	}
	
	
	@Test
	public void missingsrcLevelRootDirOK() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		Path workDir = mockFL.fs.getPath("work2");
		Files.createDirectories(workDir);
		Files.createFile(workDir.resolve("MyFirstClass.conc"));
		
		Concc concc = new Concc("work2[MyFirstClass.conc]", mockFL, mockWriter);
		
		String got = concc.validateConccInstance(concc.getConccInstance()).validationErrs;
		
		TestCase.assertEquals(null, got);
	}
	
	@Test
	public void misssingSrcFileOrDir() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		
		Concc concc = new Concc("missing.conc /missingDir", mockFL, mockWriter);
		
		String expect = "Unable to resolve source file/directory of: './missing.conc' \n" + 
				"Unable to resolve source file/directory of: './missingDir' \n" + 
				"No .conc files found to compile. Use: concc -help or concc --help for assistance.";
		
		String got = concc.validateConccInstance(concc.getConccInstance()).validationErrs;
		
		TestCase.assertEquals(expect, got);
	}
	
	@Test
	public void recusriveDirSearch() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		
		Path workDir = mockFL.fs.getPath("/work/com/thing/");
		Files.createDirectories(workDir);
		Files.createFile(workDir.resolve("MyFirstClass.conc"));
		Files.createFile(workDir.resolve("MyFirstClass2.conc"));
		
		Concc concc = new Concc("/work", mockFL, mockWriter);
		
		ValidationErrorsAndValidObject validAndVO = concc.validateConccInstance(concc.getConccInstance());
		
		TestCase.assertNull(validAndVO.validationErrs);
		String expect = "/work/com/thing/MyFirstClass.conc[com.thing true] /work/com/thing/MyFirstClass2.conc[com.thing true]";
		TestCase.assertEquals(expect, validAndVO.validconcObject.toString());
	}
	
	@Test
	public void includeFriendsFromDir() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);
		
		Path workDir = mockFL.fs.getPath("/work/com/thing/");
		Files.createDirectories(workDir);
		Files.createFile(workDir.resolve("MyFirstClass.conc"));
		Files.createFile(workDir.resolve("MyFirstClass2.conc"));
		
		Concc concc = new Concc("/work/[com/thing/MyFirstClass.conc]", mockFL, mockWriter);
		
		ValidationErrorsAndValidObject validAndVO = concc.validateConccInstance(concc.getConccInstance());
		
		TestCase.assertNull(validAndVO.validationErrs);
		String expect = "/work/com/thing/MyFirstClass.conc[com.thing true] /work/com/thing/MyFirstClass2.conc[com.thing false]";
		TestCase.assertEquals(expect, validAndVO.validconcObject.toString());
	}
	
	@Test
	public void includeFriendsFromDirManyFriends() throws IOException {
		ConccTestMockFileLoader mockFL = new ConccTestMockFileLoader();
		ConccTestMockFileWriter mockWriter = new ConccTestMockFileWriter(mockFL);

		Path workDir = mockFL.fs.getPath("/work/com/thing/");
		Files.createDirectories(workDir);
		Files.createFile(workDir.resolve("MyFirstClass.conc"));
		Files.createFile(workDir.resolve("MyFirstClass2.conc"));
		
		Concc concc = new Concc("/work/[com/thing/MyFirstClass.conc com/thing/MyFirstClass2.conc]", mockFL, mockWriter);
		
		ValidationErrorsAndValidObject validAndVO = concc.validateConccInstance(concc.getConccInstance());
		//no filtering at this point
		TestCase.assertNull(validAndVO.validationErrs);
		String expect = "/work/com/thing/MyFirstClass.conc[com.thing true] /work/com/thing/MyFirstClass2.conc[com.thing false] /work/com/thing/MyFirstClass2.conc[com.thing true] /work/com/thing/MyFirstClass.conc[com.thing false]";
		TestCase.assertEquals(expect, validAndVO.validconcObject.toString());
	}
}
