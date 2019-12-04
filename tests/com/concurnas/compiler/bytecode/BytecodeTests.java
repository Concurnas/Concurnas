package com.concurnas.compiler.bytecode;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.bootstrap.runtime.cps.Iso;
import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.FileLoader;
import com.concurnas.compiler.MainLoop;
import com.concurnas.compiler.MainLoop.SourceCodeInstance;
import com.concurnas.compiler.ModuleCompiler;
import com.concurnas.compiler.StartUtils;
import com.concurnas.compiler.scopeAndType.MockFileLoader;
import com.concurnas.compiler.util.Concurrent;
import com.concurnas.compiler.util.ConcurrentJunitRunner;
import com.concurnas.compiler.utils.BytecodePrettyPrinter;
import com.concurnas.compiler.utils.FileUtils;
import com.concurnas.compiler.visitors.PrintSourceTestVisitorIndent;
import com.concurnas.compiler.visitors.PrintSourceVisitor;
import com.concurnas.compiler.visitors.Utils;
import com.concurnas.runtime.ClassPathUtils;
import com.concurnas.runtime.ConcurnasClassLoader;
import com.concurnas.runtime.MockFileWriter;
import com.concurnas.runtime.Pair;

import junit.framework.TestCase;

@RunWith(ConcurrentJunitRunner.class)
@Concurrent(threads = 8)
public class BytecodeTests extends TestCase implements Opcodes {
	private final static String SrcDir = (new File("tests/com/concurnas/compiler/bytecode")).getAbsolutePath();

	// private static boolean showTrans=true;

	private Pair<String, String> splitTestNameAndCode(String in) {
		StringBuilder preable = new StringBuilder();
		StringBuilder code = new StringBuilder();

		String[] inputs = in.split("\n");
		for (int n = 0; n < inputs.length; n++) {
			String toAdd = inputs[n];
			if (toAdd.startsWith("//##") && !toAdd.startsWith("//##MODULE")) {
				preable.append(toAdd.substring(4));
			} else {
				code.append(toAdd + "\n");
			}
		}

		return new Pair<String, String>(preable.toString(), code.toString());
	}

	@Test
	public void testTheBigByteCodeTest() throws Throwable {
		testBigLoadFiles("theBigBytecodeTest");
	}
	
	@Test
	public void testNoNull() throws Throwable {
		testBigLoadFiles("noNull");
	}
	@Test
	public void testLangExt() throws Throwable {
		testBigLoadFiles("langExtBC");
	}
	@Test
	public void testObjProviders() throws Throwable {
		testBigLoadFiles("objProvider");
	}
	
	@Test
	public void testConcCaseStudy() throws Throwable {
		testBigLoadFiles("concCaseStudy");
	}
	
	@Test
	public void testInferGenerics() throws Throwable {
		testBigLoadFiles("inferGenerics");
	}

	// /
	@Test
	public void testFieldAccess() throws Throwable {
		testBigLoadFiles("testAllFieldAccessBC");
	}

	@Test
	public void testAllTheArrays() throws Throwable {
		testBigLoadFiles("alltheArrays");
	}
	
	@Test
	public void testTraits() throws Throwable {
		testBigLoadFiles("traits");
	}

	@Test
	public void testCopy() throws Throwable {
		testBigLoadFiles("copy");
	}
	
	@Test
	public void testDistComp() throws Throwable {
		testBigLoadFiles("distComp", true);
	}
	

	@Test
	public void testMultiAssign() throws Throwable {
		testBigLoadFiles("multiAssign");
	}
	
	@Test
	public void testVectorization() throws Throwable {
		testBigLoadFiles("vectorization");
	}

	@Test
	public void testVectorizationAuto() throws Throwable {
		testBigLoadFiles("vectorizationAuto");
	}
	
	@Test
	public void testRefsCompact() throws Throwable {
		testBigLoadFiles("refsCompact");
	}
	@Test
	public void testMixins() throws Throwable {
		testBigLoadFiles("mixins");
	}
	
	@Test
	public void testRanges() throws Throwable {
		testBigLoadFiles("ranges");
	}
	
	@Test
	public void testVectorizationOnLists() throws Throwable {
		testBigLoadFiles("vectorizationOnLists");
	}

	@Test
	public void testExceptions() throws Throwable {
		testBigLoadFiles("exceptions");
	}

	@Test
	public void testRefs() throws Throwable {
		testBigLoadFiles("refsbc");
	}
	
	@Test
	public void testComplexArray() throws Throwable {
		testBigLoadFiles("complexarray");
	}
	
	@Test
	public void testEnums() throws Throwable {
		testBigLoadFiles("enums");
	}
	@Test
	public void testExtensionFuncs() throws Throwable {
		testBigLoadFiles("extfuncs");
	}
	
	@Test
	public void testBitshift() throws Throwable {
		testBigLoadFiles("bitshift");
	}
	
	@Test
	public void testTryWithResources() throws Throwable {
		testBigLoadFiles("tryWithResources");
	}
	
	@Test
	public void testAnnotations() throws Throwable {
		testBigLoadFiles("annotation");
	}
	
	@Test
	public void testExprLists() throws Throwable {
		testBigLoadFiles("exprLists");
	}
	
	@Test
	public void testModules() throws Throwable {
		testBigLoadFiles("modules");
	}
	
	@Test
	public void testDeleteStmt() throws Throwable {
		testBigLoadFiles("deleteStmt");
	}
	
	@Test
	public void testTrans() throws Throwable {
		testBigLoadFiles("trans");
	}
	
	@Test
	public void testMultitype() throws Throwable {
		testBigLoadFiles("multitype");
	}
	
	@Test
	public void testActors() throws Throwable {
		testBigLoadFiles("agents");
	}
	
	@Test
	public void testComplexRefs() throws Throwable {
		testBigLoadFiles("complexRefs");
	}
	
	@Test
	public void testTuples() throws Throwable {
		testBigLoadFiles("tuples");
	}
	
	
	@Test
	public void testAnonLambda() throws Throwable {
		testBigLoadFiles("anonLambda");
	}
	
	
	@Test
	public void testDefaultMap() throws Throwable {
		testBigLoadFiles("defMap");
	}
	
	@Test
	public void testDMA() throws Throwable {
		testBigLoadFiles("dma");
	}
	
	@Test
	public void testClassloaders() throws Throwable {
		testBigLoadFiles("classloaders");
	}

	@Test
	public void testJustBugs1() throws Throwable {
		testBigLoadFiles("bugs1");
	}
	
	@Test
	public void testLastThingRet() throws Throwable {
		testBigLoadFiles("lastThingRet");
	}

	@Test
	public void testExceptionsPlus1() throws Throwable {
		testBigLoadFiles("exceptionsNestPlus1");
	}

	@Test
	public void testExceptionsBreak() throws Throwable {
		testBigLoadFiles("exceptionsBreak");
	}
	
	@Test
	public void testShared() throws Throwable {
		testBigLoadFiles("shared");
	}
	
	@Test
	public void testWith() throws Throwable {
		testBigLoadFiles("with");
	}

	@Test
	public void testExceptionsContinue() throws Throwable {
		testBigLoadFiles("exceptionsContinue");
	}

	@Test
	public void testAllTheObjects() throws Throwable {
		testBigLoadFiles("alltheObjects");
	}
	
	@Test
	public void testMethodPlus() throws Throwable {
		testBigLoadFiles("methodplus");
	}
	
	@Test
	public void testTypedefs() throws Throwable {
		testBigLoadFiles("typedefs");
	}
	
	@Test
	public void testMatch() throws Throwable {
		testBigLoadFiles("match");
	}
	
	@Test
	public void testOnChange() throws Throwable {
		testBigLoadFiles("bcOnChangeTests");
	}

	@Test
	public void testOpOverload() throws Throwable {
		testBigLoadFiles("opOverload");
	}
	
	@Test
	public void testConstructorsGoWild() throws Throwable {
		testBigLoadFiles("constructorsGoWild");
	}

	@Test
	public void testModuleFields() throws Throwable {
		testBigLoadFiles("moduleFields");
	}

	@Test
	public void testLambdaLambdaLambda() throws Throwable {
		testBigLoadFiles("lambdalambdalambda");
	}
	
	@Test
	public void testLocalClasses() throws Throwable {
		testBigLoadFiles("localClasses");
	}
	
	@Test
	public void testDMAConverters() throws Throwable {
		testBigLoadFiles("dmaConverters");
	}

	@Test
	public void testBytecodeSandbox() throws Throwable {
		boolean pops = BytecodeOutputter.PRINT_OPCODES;
		try {
			BytecodeOutputter.PRINT_OPCODES=true;
			runCompilation("bytecodeSandbox", true, false, true);
		}finally {
			BytecodeOutputter.PRINT_OPCODES=pops;
		}
	}
	

	@Test
	public void testGPU() throws Throwable {
		testBigLoadFiles("gpu");
	}
	
	@Test
	public void testPulsars() throws Throwable {
		testBigLoadFiles("pulsarTests*");
	}
	
	
	@Test
	public void testCopierFunc() throws Throwable {
		String intputFile = SrcDir + File.separator + "copierFunc" + ".conc";
		checkExists(intputFile);

		String data = FileUtils.readFile(intputFile);

		String[] intputs = data.split("~~~~~");

		String totalExpected = "";
		String totalgot = "";
		for (String testRun : intputs) {
			MainLoop mainLoop=null;
			try
			{
				String[] lines = testRun.split("\n");

				StringBuilder code = new StringBuilder();
				ArrayList<String> immutables = new ArrayList<String>();
				Map<String, ArrayList<String>> childrenCopied = new TreeMap<String, ArrayList<String>>();

				for (String line : lines) {
					if (line.startsWith("//::Immtuable:")) {
						String[] ims = line.substring(14).split(",");
						for (String i : ims) {
							immutables.add(i.trim());
						}
					} else if (line.startsWith("//::CopyKids:")) {// //::Copy:
																	// A[two], B[]
						line = line.substring(13).trim();
						line = line.substring(1, line.length() - 1);
						String[] ccs = line.split(",");
						for (String cs : ccs) {
							cs = cs.trim();
							int st = cs.indexOf("=[");
							String header = cs.substring(0, st);
							ArrayList<String> kidz = new ArrayList<String>();
							childrenCopied.put(header, kidz);

							String[] kidza = cs.substring(st + 2, cs.length() - 1).split(":");
							for (String k : kidza) {
								k = k.trim();
								if (!k.isEmpty()) {
									kidz.add(k.trim());
								}
							}
						}
					} else if (line.startsWith("//##") && !line.startsWith("//##MODULE") ) {
						totalExpected += line + "\n";
						totalgot += line + "\n";
					} else {
						code.append(line + "\n");
					}
				}

				MockFileLoader mockLoader = new MockFileLoader(SrcDir);
				mockLoader.addFile(intputFile, code.toString());

				MockFileWriter mfw = new MockFileWriter();
				mainLoop = new MainLoop(SrcDir, mockLoader, true, false, mfw, false);
				ArrayList<ErrorHolder> errs = mainLoop.compileFile("copierFunc.conc").messages;
				String output = assertNoPreBytecodeErrors(errs, false, false);
				if (null == output) {
					boolean hasNop = false;
					for (String name : mfw.nametoCode.keySet()) {
						hasNop |= BytecodePrettyPrinter.print(mfw.nametoCode.get(name), false).contains("NOP");
					}

					if (hasNop) {
						output = "NOP detected in bytecode";
					} else {
						InMemoryClassLoader myClassLoader = new InMemoryClassLoader();
						myClassLoader.loadClasses(mfw.nametoCode);

						output = findImmutables(myClassLoader);

						// validate correct children copied
						// checkChildrenCopied

						output += "\nCopyKids: " + fkindCopiedKids(myClassLoader);

					}
				}

				String expected = "Immutables: " + immutables;

				totalExpected += expected + "\nCopyKids: " + childrenCopied + "\n\n";
				totalgot += output + "\n\n";

			}
			catch(Throwable e){
				e.printStackTrace();
				totalgot += e.getMessage() + "\n\n";
			}finally {
				mainLoop.stop();
			}

		}

		assertEquals(totalExpected, totalgot);
	}

	private String fkindCopiedKids(InMemoryClassLoader myClassLoader) throws Throwable {
		// ArrayList<String> foundImms = new ArrayList<String>();
		// StringBuilder output= new StringBuilder("{");

		Map<String, ArrayList<String>> childrenCopied = new TreeMap<String, ArrayList<String>>();

		ArrayList<String> classes = new ArrayList<String>(myClassLoader.nameToCode.keySet());
		Collections.sort(classes);

		for (String name : classes) {
			ArrayList<String> copiedFeildContents = new ArrayList<String>();

			if (!name.contains("$")) {
				continue;
			}// TODO: copier for modules

			// System.out.println("find the im: " + name);
			
			Class<?> fiberCls = myClassLoader.loadClass("com.concurnas.bootstrap.runtime.cps.Fiber");
			Object fibInst = null;
			for(Constructor<?> c : fiberCls.getConstructors()){
				if(c.getParameterTypes().length == 1  && c.getParameterTypes()[0]==Iso.class){
					fibInst = c.newInstance(new Object[]{null});
					break;
				}
			}
			
			Method dd =null;
			for(Method m : fiberCls.getMethods()){
				if(m.getName() == "down" && m.getParameterTypes().length==0){
					dd = m;
					break;
				}
			}
			
			dd.invoke(fibInst);
			

			Class<?> cls = myClassLoader.loadClass(name);
			// Object inst =unsafe.allocateInstance(cls);
			// Object inst =cls.newInstance();
			Object inst = newInstance(cls);
			Method initFunc = null;
			Method[] meths = cls.getDeclaredMethods();
			for (Method mo : meths) {
				if (mo.getName().equals("init") && mo.getParameterTypes().length == 2 && mo.getParameterTypes()[0].getName().equals("com.concurnas.bootstrap.runtime.InitUncreatable") && mo.getParameterTypes()[1].getName().equals("com.concurnas.bootstrap.runtime.cps.Fiber")) {
					initFunc = mo;
					break;
				}
			}
			
			
			//Fiber fibInst = new Fiber(null);
			//fibInst.down();
			if(null != initFunc){
				initFunc.invoke(inst, new Object[]{null,fibInst});
			}
			else{//try for 3?
				for (Method mo : meths) {
					if (mo.getName().equals("init") && mo.getParameterTypes().length == 3 && mo.getParameterTypes()[1].getName().equals("com.concurnas.bootstrap.runtime.InitUncreatable") && mo.getParameterTypes()[2].getName().equals("com.concurnas.bootstrap.runtime.cps.Fiber")) {
						initFunc = mo;
						break;
					}
				}
				//make arg 1 which is the parent
				Object parent = initFunc.getParameterTypes()[0].newInstance();
				
				
				initFunc.invoke(inst, new Object[]{parent,null,fibInst});
			}
			//very nasty tests
			
			
			Method copyFunc = null;
			meths = cls.getDeclaredMethods();
			for (Method mo : meths) {
				if (mo.getName().equals("copy") && mo.getParameterCount()==3) {
					copyFunc = mo;
					break;
				}
			}

			Class<?> trackerCls = myClassLoader.loadClass("com.concurnas.bootstrap.runtime.CopyTracker");

			if (null == copyFunc) {
				copiedFeildContents.add("Cannot find copy function in class: " + name);
				// break;
			} else {
				try {

					Object trackerObj = trackerCls.newInstance();
					Field fa = trackerCls.getField("clonedAlready");
					fa.set(trackerObj, new IdentityHashMap<Object, Object>(16));//hack so dont need to call init
					
					Object ret = copyFunc.invoke(inst, new Object[]{trackerObj, null, fibInst});

					if (ret != inst) {// ret self therefore immutable
						Field[] fs = cls.getDeclaredFields();
						for (Field f : fs) {
							f.setAccessible(true);
							Object inOrig = f.get(inst);
							Object inCopy = f.get(ret);
							if (inOrig != inCopy) {
								copiedFeildContents.add(f.getName());
							}
						}
					}
				} catch (InvocationTargetException i) {
					i.printStackTrace();
					throw i.getTargetException();
				}
			}
			Collections.sort(copiedFeildContents);
			childrenCopied.put(name, copiedFeildContents);

		}
		return "" + childrenCopied;
	}

	@Test
	public void testNestedInnerFuncs() throws Throwable {
		testBigLoadFilesDiffOutput("../nestedFuncRepoint/testAllNestedFuncRepoint", "testAllNestedFuncRepoint");
	}

	@Test
	public void testNestedInnerFuncLambdas() throws Throwable {
		testBigLoadFilesDiffOutput("../nestedFuncRepoint/testAllNestedFuncRepointLambda", "testAllNestedFuncRepointLambda");
	}

	private boolean showTrans = true;

	static String readFile(String path) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, Charset.defaultCharset());
	}
	
	private ArrayList<String> foundLines(String cont){
		return foundLines(cont, "\r\n");
	}
	
	private ArrayList<String> foundLines(String cont, String split){
		ArrayList<String> ret = new  ArrayList<String>();
		StringBuilder cani = null;
		for(String line : cont.split(split)){
			if(cani != null){
				if(!line.startsWith("*/")){
					cani.append(line);
					cani.append("\n");
				}
				else{
					ret.add(cani.toString());
					cani=null;
				}
			}
			
			if(line.startsWith("/*#Find lines:")){
				cani = new StringBuilder();
				//cani.append(line);
			}
		}
		return ret;
	}

	private static ArrayList<Pair<String, String>> extractToModules(String fname, String data){
		//modname, data
		ArrayList<Pair<String, String>> ret = new ArrayList<Pair<String, String>>();
		if(!data.contains("//##MODULE")){
			ret.add(new Pair<String, String>(fname, data));
		}else{
			String[] modules = data.split("//##MODULE");
			for(String mod : modules){
				if(!mod.trim().equals("")){//skip empty
					int firstNL = mod.indexOf("\n", 0);
					String modname = mod.substring(0, firstNL).trim();
					if(modname.equals("")){
						modname = fname;
					}else{
						modname = modname.replace('.', File.separatorChar) + ".conc";
					}
					String datax = mod.substring(firstNL);
					ret.add(new Pair<String, String>(modname, datax));
				}
			}
			
			if(ret.isEmpty()){//no modules, dump it all in
				ret.add(new Pair<String, String>(fname, data));
			}
		}
		
		return ret;
		
	}
	
	
	private InMemoryClassLoader runCompilation(String inputFileorig, boolean printSource, boolean immutableStuff, boolean profile) throws Throwable {
		String inputFile = inputFileorig + ".conc";
		String absSrcFilePath = SrcDir + File.separator + inputFile;

		checkExists(absSrcFilePath);

		MockFileWriter mfw = new MockFileWriter();

		String data = FileUtils.readFile(absSrcFilePath);
		
		MockFileLoader mockLoader = produceLoader(inputFile, data);
		
		MainLoop mainLoop = new MainLoop(SrcDir, mockLoader, true, false, mfw, false);
		if(profile) {
			mainLoop.setProfile(profile);
		}
		
		ArrayList<String> tocomp = new ArrayList<String>(mockLoader.getAllFiles());
		Collections.sort(tocomp);
		

		ArrayList<SourceCodeInstance> srcItems = new ArrayList<SourceCodeInstance>();
		for(String pathComp : tocomp) {
			srcItems.add(MainLoop.makeSourceCodeInstance(SrcDir, pathComp));
		}
		
		ArrayList<ErrorHolder> errs = mainLoop.compileFile(srcItems).messages;
		//D:\work\concurnas\.\tests\com\concurnas\compiler\bytecode\bytecodeSandbox.conc not found
		ArrayList<String> linesToFind =  foundLines(readFile(absSrcFilePath));
		
		try {
			for (String name : mfw.nameToJavaRep.keySet()) {
				System.out.println("Intermediate Javacode of: " + name + ":");
				System.out.println(mfw.nameToJavaRep.get(name));
				System.out.println();
			}
		} catch (Exception err) {
		}

		if (printSource) {
			for (ModuleCompiler mc : new HashSet<ModuleCompiler>(mainLoop.fullPathToModuleCompiler.values())) {
				
				
				PrintSourceVisitor visitor = new PrintSourceTestVisitorIndent();
				try{
					visitor.visit(mc.lexedAndParsedAST);
				} catch (Exception e) {
					assertNoPreBytecodeErrors(errs, true, false);
					throw e;
				}

				String ret = ""+visitor;//Utils.listToString(visitor.items);
				System.out.println(ret);
				System.out.println();
			}
		}
		
		try {
			assertNoPreBytecodeErrors(errs, true, false);
			boolean hasNop = false;
			for (String name : mfw.nametoCode.keySet()) {
				byte[] code = mfw.nametoCode.get(name);
				// code = Concurnifier.concurnifyClass(code);
				System.out.println("Bytecode of: " + name + ":");
				
				hasNop |= BytecodePrettyPrinter.print(code, true).contains("NOP");
				System.out.println();
			}
			
			if (hasNop) {
				throw new Exception("NOP detected in bytecode");
			}
			
		} catch (Exception e) {
			throw e;
		} finally {
			mainLoop.stop();
		}

		InMemoryClassLoader myClassLoader = new InMemoryClassLoader();
		// Thread.currentThread().setContextClassLoader(myClassLoader);

		myClassLoader.loadClasses(mfw.nametoCode);

		SchedulClsAndObj shh = makeSchedulClsAndObj(myClassLoader);
		
		String res = executeClass(myClassLoader, "Hello world", inputFileorig, showTrans, linesToFind, shh);

		if (immutableStuff) {
			System.out.println(findImmutables(myClassLoader));
			System.out.println(fkindCopiedKids(myClassLoader));
		}
		if(mainLoop.profilers != null) {
			System.out.println("Profiling:\n==========");
			mainLoop.profilers.forEach(a -> a.printEvents());
			System.out.println();
		}
		
		System.out.println(res);
		
		
		return myClassLoader;
	}
	
	public static String microCompilation(String code) throws Throwable{
		MockFileWriter mfw = new MockFileWriter();
		MockFileLoader mfl = new MockFileLoader(SrcDir);
		
		mfl.addFile(SrcDir + File.separator + "test.conc", code);
		
		MainLoop mainLoop = new MainLoop(SrcDir, mfl, true, false, mfw, false);
		ArrayList<ErrorHolder> errs = mainLoop.compileFile("test.conc").messages;
		
		if(errs!=null && !errs.isEmpty()){
			throw new RuntimeException("Unexpected errors in compilation: " + errs);
		}
		
		ConcurnasClassLoader masterLoader = new ConcurnasClassLoader(ClassPathUtils.getSystemClassPathAsPaths(), ClassPathUtils.getInstallationPath(), sharedConcClassLoader);
		InMemoryClassLoader myClassLoader = new InMemoryClassLoader(masterLoader);
		
		myClassLoader.loadClasses(mfw.nametoCode);
		SchedulClsAndObj shh = makeSchedulClsAndObj(masterLoader);

		mainLoop.stop();
		return executeClass(myClassLoader, null, "test", false, new ArrayList<String>(), shh);
		
	}

	/*
	 * private static Unsafe unsafe; static{ try { Field f =
	 * Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true); unsafe
	 * = (Unsafe) f.get(null); } catch (Exception e) { //bad } }
	 */

	private Object newInstance(Class<?> cls) throws Throwable {
		Constructor<?>[] cons = cls.getConstructors();

		Constructor<?> noArg = null;

		for (Constructor<?> c : cons) {
			if (c.getParameterTypes().length == 0) {
				noArg = c;
				break;
			}
		}

		if (null != noArg) {
			return noArg.newInstance();
		} else {
			for (Constructor<?> c : cons) {
				Class<?>[] pararms = c.getParameterTypes();
				if (pararms.length == 1) {// assume 1st is parent invoker
					Object parent = pararms[0].newInstance();
					return c.newInstance(parent);
				}
			}
		}

		return null;

	}

	private String findImmutables(InMemoryClassLoader myClassLoader) throws Throwable {
		ArrayList<String> foundImms = new ArrayList<String>();
		String output = null;
		
		Class<?> fiberCls = myClassLoader.loadClass("com.concurnas.bootstrap.runtime.cps.Fiber");
		Constructor<?> fiberCon = null;
		for(Constructor<?> c : fiberCls.getConstructors()){
			if(c.getParameterTypes().length == 1 && c.getParameterTypes()[0]==Iso.class){
				fiberCon = c;
				break;
			}
		}
		
		
		for (String name : myClassLoader.nameToCode.keySet()) {
			if (!name.contains("$")) {
				continue;
			}// TODO: copier for modules

			// System.out.println("find the im: " + name);

			Class<?> cls = myClassLoader.loadClass(name);
			// Object inst =unsafe.allocateInstance(cls);
			Object inst = newInstance(cls);

			Object fibInst = fiberCon.newInstance(new Object[]{null});
			
			Method dd =null;
			for(Method m : fiberCls.getMethods()){
				if(m.getName() == "down" && m.getParameterTypes().length==0){
					dd = m;
					break;
				}
			}
			
			dd.invoke(fibInst);	
			
			
			
			Class<?> trackerCls = myClassLoader.loadClass("com.concurnas.bootstrap.runtime.CopyTracker");
			//Class<?> trackerCls = myClassLoader.loadClass("com.concurnas.bootstrap.runtime.CopyTracker");
			// ConcurnificationTracker ct = new ConcurnificationTracker();//
			// (ConcurnificationTracker) trackerCls.newInstance();

			cls.getConstructors();// ret no arg else one with 1 parent obj ref,
									// then go make parent

			Method copyFunc = null;
			for (Method mo : cls.getMethods()) {
				if (mo.getName().equals("copy") && mo.getParameterCount()==3) {
					copyFunc = mo;
				}
			}

			if (null == copyFunc) {
				output = output == null ? "" : output;
				output += "Cannot find copy function in class: " + name;
				// break;
			} else {
				try {
					assertTrue(copyFunc.getParameterTypes()[0].equals(trackerCls) && copyFunc.getParameterTypes()[2].equals(fiberCls));
					
					Object trackerObj = trackerCls.newInstance();
					Field f = trackerCls.getField("clonedAlready");
					f.set(trackerObj, new IdentityHashMap<Object, Object>(16));//hack so dont need to call init
					
					Object ret = copyFunc.invoke(inst, trackerObj, null, fibInst);
					if (ret == inst) {// ret self therefore immutable
						foundImms.add(name);
					}
				} catch (InvocationTargetException i) {
					throw i.getTargetException();
				}

			}
		}
		Collections.sort(foundImms);
		output = output == null ? "" : output;
		output += "Immutables: " + foundImms;
		return output;
	}

	public static class SharedConcClassLoader extends ConcurnasClassLoader{
		public SharedConcClassLoader(){
			super(ClassPathUtils.getSystemClassPathAsPaths(), ClassPathUtils.getInstallationPath());
		}
	}
	
	private static SharedConcClassLoader sharedConcClassLoader = new SharedConcClassLoader();//one of these across all tests
	
	public static class InMemoryClassLoader extends ConcurnasClassLoader {
		public InMemoryClassLoader(ConcurnasClassLoader parent){
			super(ClassPathUtils.getSystemClassPathAsPaths(), ClassPathUtils.getInstallationPath(), parent);
		}
		
		public InMemoryClassLoader( ){
			super(ClassPathUtils.getSystemClassPathAsPaths(), ClassPathUtils.getInstallationPath(), sharedConcClassLoader);
		}
		
		public InMemoryClassLoader(Path[] cp, Path[] bootsrap) {
			super(cp, bootsrap);
		}
		
		public HashMap<String, byte[]> nameToCode = new HashMap<String, byte[]>();

		boolean showTrans = false;
		public StringBuilder captureTransformedBytecode = null;

		public void loadClasses(HashMap<String, byte[]> nameToCodea) throws Exception {
			for (String name : nameToCodea.keySet()) {
				nameToCode.put(name.replace('/','.'), nameToCodea.get(name));
			}
		}
		public void loadClass(String name, byte[] code) throws Exception {
			nameToCode.put(name.replace('/','.'), code);
		}

		public ClassLoader getParentCL(){
			return this;
		}
		
		public byte[] getBytecode(String name, boolean searchSystemClassloader){
			name = name.replace('/', '.');
			byte[] got = nameToCode.get(name);
			if (null == got && searchSystemClassloader) {
				got = super.getBytecode(name, searchSystemClassloader);
			}
			return got;
		}
		
		public byte[] getBytecode(String name) {
			return getBytecode(name, true);
		}

		public Class<?> findClass(String name) throws ClassNotFoundException {
			byte[] code = nameToCode.get(name);
			if(code == null && name.endsWith("$Globals$")){
				name = name.substring(0, name.length() - 9);
				code = nameToCode.get(name);
			}
			return null != code ? defineClass(name, code) : super.findClass(name);// parent.loadClass(name);
		}

		protected void registerTransformed(String name, byte[] trans) {
			if(null != captureTransformedBytecode){
				try {
					captureTransformedBytecode.append(BytecodePrettyPrinter.print(trans, false));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}

		public Class<?> defineClassDirectNoTransform(String name, byte[] code) {
			return super.defineClass(name, code, 0, code.length);
		}
	}


	private static byte[] gennerateIso(String invokerclassName, String classBeingTested) {
		// invokerclassName = "com/concurnas/compiler/bytecode/IsoTester"
		// classBeingTested = "com/concurnas/compiler/bytecode/MyTest"
		//TestIsoInvoker
		ClassWriter cw = new ClassWriter(0);
		FieldVisitor fv;
		MethodVisitor mv;

		cw.visit(52, ACC_PUBLIC + ACC_SUPER, invokerclassName, "Lcom/concurnas/bootstrap/runtime/cps/IsoTask;", "com/concurnas/bootstrap/runtime/cps/IsoTask", null);

		cw.visitSource(invokerclassName + ".java", null);
		cw.visitInnerClass("com/concurnas/bootstrap/lang/Lambda$Function0", "com/concurnas/bootstrap/lang/Lambda", "Function0", ACC_PUBLIC + ACC_STATIC + ACC_ABSTRACT);
		
		{
			fv = cw.visitField(ACC_PRIVATE, "result", "Ljava/lang/String;", null, null);
			fv.visitEnd();
			
			fv = cw.visitField(ACC_PRIVATE, "$isInitCompleteFlag", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef;", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef<Ljava/lang/Boolean;>;", null);
			fv.visitEnd();
			
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);

			mv.visitInsn(ACONST_NULL);
			
			mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/bootstrap/runtime/cps/IsoTask", "<init>", "(Ljava/lang/Class;)V", false);
			
			mv.visitVarInsn(ALOAD, 0);
			mv.visitTypeInsn(NEW, "com/concurnas/runtime/ref/Local");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_1);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitLdcInsn(org.objectweb.asm.Type.getType("Ljava/lang/Boolean;"));
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/runtime/ref/Local", "<init>", "([Ljava/lang/Class;)V", false);
			mv.visitFieldInsn(PUTFIELD, invokerclassName, "$isInitCompleteFlag", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef;");
		
			
			
			mv.visitInsn(RETURN);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "apply", "()Ljava/lang/Void;", null, null);
			mv.visitCode();
			Label l0 = new Label();
			Label l1 = new Label();
			Label l2 = new Label();
			mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Throwable");
			mv.visitLabel(l0);
			mv.visitMethodInsn(INVOKESTATIC, classBeingTested, "doings", "()Ljava/lang/String;", false);
			mv.visitVarInsn(ASTORE, 1);
			Label l3 = new Label();
			mv.visitLabel(l3);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKESPECIAL, invokerclassName, "setResult", "(Ljava/lang/String;)V", false);
			mv.visitLabel(l1);
			Label l4 = new Label();
			//java.lang.System.ide
			mv.visitJumpInsn(GOTO, l4);
			
			mv.visitLabel(l2);
			mv.visitVarInsn(ASTORE, 2);
			Label l6 = new Label();
			mv.visitLabel(l6);
			
	
			mv.visitVarInsn(ALOAD, 2);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false);
						
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "getMessage", "()Ljava/lang/String;", false);
			mv.visitMethodInsn(INVOKESPECIAL, invokerclassName, "setResult", "(Ljava/lang/String;)V", false);
			mv.visitLabel(l4);
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PRIVATE + ACC_SYNCHRONIZED, "setResult", "(Ljava/lang/String;)V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);//convert null to "null"
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
			mv.visitFieldInsn(PUTFIELD, invokerclassName, "result", "Ljava/lang/String;");
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "notifyAll", "()V", false);
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_SYNCHRONIZED, "getResult", "()Ljava/lang/String;", null, new String[] { "java/lang/InterruptedException" });
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			Label l1 = new Label();
			mv.visitJumpInsn(GOTO, l1);
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "wait", "()V", false);
			mv.visitLabel(l1);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, invokerclassName, "result", "Ljava/lang/String;");
			mv.visitJumpInsn(IFNULL, l2);
			Label l3 = new Label();
			mv.visitLabel(l3);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, invokerclassName, "result", "Ljava/lang/String;");
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "signature", "()[Ljava/lang/Object;", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "apply", "()Ljava/lang/Object;", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, invokerclassName, "apply", "()Ljava/lang/Void;", false);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		
		{//getIsInitCompleteFlag
			mv = cw.visitMethod(ACC_PUBLIC, "getIsInitCompleteFlag", "()Lcom/concurnas/bootstrap/runtime/ref/DefaultRef;", "()Lcom/concurnas/bootstrap/runtime/ref/DefaultRef<Ljava/lang/Boolean;>;", null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, invokerclassName, "$isInitCompleteFlag", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef;");
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		
		cw.visitEnd();

		return cw.toByteArray();
	}
	
	public static class SchedulClsAndObj{
		public Class<?> schedulerCls;
		public Object shceduler;

		public SchedulClsAndObj(Class<?> schedulerCls, Object shceduler){
			this.schedulerCls=schedulerCls;
			this.shceduler=shceduler;
		}
		
	}
	
	public static SchedulClsAndObj makeSchedulClsAndObj(ConcurnasClassLoader myClassLoader) throws ClassNotFoundException, InstantiationException, IllegalAccessException{
		Class<?> schedulerCls = myClassLoader.loadClass("com.concurnas.bootstrap.runtime.cps.Scheduler");
		Object shceduler = schedulerCls.newInstance();
		return new SchedulClsAndObj(schedulerCls, shceduler);
	}

	public static String executeClass(InMemoryClassLoader myClassLoader, String expectedresult, String exename, boolean showTrans, ArrayList<String> linesToFind, SchedulClsAndObj shh) throws Throwable {
		String res = null;
		try {
			myClassLoader.showTrans = showTrans;
			if(!linesToFind.isEmpty()){
				myClassLoader.captureTransformedBytecode = new StringBuilder();
			}
			
			boolean found = false;
			
			Class<?> schedulerCls = shh.schedulerCls;
			Object shceduler = shh.shceduler;
			
			String invokerCls = "TestIsoInvoker";
			byte[] isocode = gennerateIso(invokerCls, exename.replaceAll("\\.", "/"));
			myClassLoader.nameToCode.put(invokerCls, isocode);
			Class<?> testLambdaCls = myClassLoader.defineClass(invokerCls, isocode);
			Object taskLAmbda = testLambdaCls.newInstance();

			
			Method schedTsk = getMethod(schedulerCls, "public void com.concurnas.bootstrap.runtime.cps.Scheduler.scheduleTask(com.concurnas.bootstrap.runtime.cps.AbstractIsoTask,java.lang.String)");
			
			schedTsk.invoke(shceduler, taskLAmbda, "Test Harness executeClass");
			
			res = "" + getMethod(testLambdaCls, "getResult", 0).invoke(taskLAmbda, new Object[]{});
			
			getMethod(schedulerCls, "cancelAll", 0).invoke(shceduler, new Object[]{});
			
			found=true;
			
			if (null != expectedresult) {
				assertTrue(found);
			}
			if (!found) {
				res = "fail - doings not found";
			}
			else if(!linesToFind.isEmpty()){
				String allMethodBd = myClassLoader.captureTransformedBytecode.toString();
				
				for(String toFind : linesToFind){
					if(!allMethodBd.contains(toFind)){
						fail("Missing transformed bytecode: " + toFind);
					}
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
			throw e;// helpful lol
		}
		return res;
	}

	private static Method getMethod(Class<?> cls, String name, int args){
		for(Method m : cls.getMethods()){
			if(m.getParameterTypes().length == args && m.getName().equals(name)) {
				return m;
			}
		}
		return null;
	}
	
	private static Method getMethod(Class<?> cls, String name){
		for(Method m : cls.getMethods()){
			if(m.toString().equals(name)){
				return m;
			}
		}
		return null;
	}
	
	private void testBigLoadFilesDiffOutput(String input, String errFile) throws Throwable {
		String intputFile = SrcDir + File.separator + input + ".conc";
		String compareTo = SrcDir + File.separator + errFile + ".conc.e";
		testBigLoadFiles(intputFile, compareTo, false);
	}

	private void testBigLoadFiles(String filename) throws Throwable {
		testBigLoadFiles(filename, false);
	}
	
	private void testBigLoadFiles(String filename, boolean newSchedulerEveryRun) throws Throwable {
		boolean addStar = false;
		if(filename.endsWith("*")) {
			filename =  filename.substring(0, filename.length()-1);
			addStar=true;
		}
		String intputFile = SrcDir + File.separator + filename + ".conc";
		String compareTo = SrcDir + File.separator + filename + ".conc.e";
		
		if(addStar) {
			intputFile += "*";
		}
		
		testBigLoadFiles(intputFile, compareTo, newSchedulerEveryRun);
	}

	public void testBigLoadFilesWithSrcDir(String filename) throws Throwable {
		String intputFile = filename + ".conc";
		String compareTo = filename + ".conc.e";
		testBigLoadFiles(intputFile, compareTo, false);
	}

	private static class ShowProgress{
		public volatile String latestProg = "not Started";
		private volatile ScheduledExecutorService executor = null;
		
		public void go() {
			Runnable helloRunnable = new Runnable() {
			    public void run() {
			        System.err.println("Latest test to run: " + latestProg);
			    }
			};

			executor = Executors.newScheduledThreadPool(1);
			executor.scheduleAtFixedRate(helloRunnable, 0, 10, TimeUnit.SECONDS);
		}
		
		public void stop() {
			if(null != executor) {
				executor.shutdownNow();
			}
		}
		
	}
	
	private void testBigLoadFiles(String intputFile, String compareTo, boolean newSchedulerEveryRun) throws Throwable {
		boolean showProgress = StartUtils.isRunningInEclipse();
		
		ShowProgress sp = null;
		if(intputFile.endsWith("*") && showProgress) {
			intputFile =  intputFile.substring(0, intputFile.length()-1);
			//create progress monitor thread:
			sp = new ShowProgress();
			sp.go();
		}
		
		checkExists(intputFile);
		checkExists(compareTo);

		String data = FileUtils.readFile(intputFile);
		String dataE = FileUtils.readFile(compareTo);

		String[] intputs = data.split("~~~~~");

		StringBuilder result = new StringBuilder();
		assertEquals(intputs.length, dataE.split("~~~~~").length);

		ConcurnasClassLoader masterLoader = new ConcurnasClassLoader(ClassPathUtils.getSystemClassPathAsPaths(), ClassPathUtils.getInstallationPath());

		SchedulClsAndObj shh = newSchedulerEveryRun?null:makeSchedulClsAndObj(masterLoader);
		
		
		for (int n = 0; n < intputs.length; n++) {

			if(newSchedulerEveryRun) {
				shh = makeSchedulClsAndObj(masterLoader);
			}
			
			Pair<String, String> testnameAndCode = splitTestNameAndCode(intputs[n]);
			String testname = testnameAndCode.getA();
			
			if(sp != null) {
				sp.latestProg = testname;
			}
			
			String code = testnameAndCode.getB();
			String output = null;
			double comTime = 0;
			double exeTime=0;
			double totTime=System.currentTimeMillis();
			MainLoop mainLoop=null;
			try {
				assertNotNull(testname);
				
				ArrayList<String> linesToFind =  foundLines(testnameAndCode.getB(), "\n");
				
				String niceTestName = Utils.getMiniTestFileName(testname);
				String inputFileName = niceTestName + ".conc";

				//MockFileLoader mockLoader = new MockFileLoader();
				//mockLoader.addFile(SrcDir + FileLoader.pathSeparatorChar + inputFileName, code);


				MockFileLoader mockLoader = produceLoader(inputFileName, code);
				
				
				MockFileWriter mfw = new MockFileWriter();
				mainLoop = new MainLoop(SrcDir, mockLoader, true, false, mfw, false);
				long comTimeStart = System.currentTimeMillis();
				
				
				//TODO: convert to multi input version
				
				
				ArrayList<ErrorHolder> errs = mainLoop.compileFiles(mockLoader.getAllFiles()).messages;
				comTime = (System.currentTimeMillis() - comTimeStart)/1000.;
				output = assertNoPreBytecodeErrors(errs, false, code.contains("//#Ignore WARN"));
				if (null == output) {
					boolean hasNop = false;
					for (String name : mfw.nametoCode.keySet()) {
						// fix nopper
						hasNop |= BytecodePrettyPrinter.print(mfw.nametoCode.get(name), false).contains("NOP");
					}

					if (hasNop) {
						output = "NOP detected in bytecode";
					} else {
						InMemoryClassLoader myClassLoader = new InMemoryClassLoader();
						
						myClassLoader.loadClasses(mfw.nametoCode);
						long tick = System.currentTimeMillis();
						output = executeClass(myClassLoader, null, niceTestName, false, linesToFind, shh);
						exeTime = (System.currentTimeMillis()-tick)/1000.;
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();

				output = e.getMessage();
				if(showProgress) {
					System.out.println(String.format("Compilation fail on test %s of %s in %s: %s", n, intputs.length, intputFile, testname));
				}
			}finally{
				if(mainLoop != null) {
					mainLoop.stop();
				}
			}

			result.append("//##" + testname + "\n" + output + "\n\n");
			if (n != intputs.length - 1) {
				result.append("~~~~~\n");
			}

			if (showProgress) {
				totTime = (System.currentTimeMillis()-totTime)/1000.;
				double overhead = totTime-(comTime + exeTime);
				System.out.println(String.format("Complete so far: %6.2f%% - % 3d of % 3d [c: %.3f e: %.3f t:%.3f o:%.3f] in %s: %s", ( (n+1) /(double) intputs.length ) * 100, n+1, intputs.length, comTime, exeTime, totTime, overhead, intputFile, testname));
			}
			
			if(newSchedulerEveryRun) {
				Class<?> schedulerCls = shh.schedulerCls;
				Object shceduler = shh.shceduler;
				getMethod(schedulerCls, "terminate", 0).invoke(shceduler, new Object[]{});
				//System.gc();
			}
		}
		
		if(!newSchedulerEveryRun) {
			Class<?> schedulerCls = shh.schedulerCls;
			Object shceduler = shh.shceduler;
			getMethod(schedulerCls, "terminate", 0).invoke(shceduler, new Object[]{});
		}
		
		if(sp != null) {
			sp.stop();
		}
		
		assertEquals(dataE.trim(), result.toString().trim());
	}
	
	public static MockFileLoader produceLoader(String inputFile, String code){
		return produceLoader(SrcDir, inputFile, code);
	}
	
	public static MockFileLoader produceLoader(String srcdir, String inputFile, String code){
		ArrayList<Pair<String, String>> fnameAndCodes =  extractToModules(inputFile, code);
		
		MockFileLoader mockLoader = new MockFileLoader(srcdir);
		
		for(Pair<String, String> fnameData : fnameAndCodes){
			mockLoader.addFile(srcdir +  FileLoader.pathSeparatorChar+  fnameData.getA(), fnameData.getB());
		}
		
		return mockLoader;
	}

	private File checkExists(String fn) {
		File ref = new File(fn);
		if (!ref.exists()) {
			fail("Canot find file: " + ref);
		}
		return ref;
	}

	private String assertNoPreBytecodeErrors(ArrayList<ErrorHolder> errs, boolean thr, boolean ignoreWarning) {
		if (!errs.isEmpty()) {
			if(ignoreWarning){
				ArrayList<ErrorHolder> newErrs = new ArrayList<ErrorHolder>(errs.size());
				for(ErrorHolder aas : errs){
					if(ignoreWarning && aas.isWarning()){
						continue;
					}
					newErrs.add(aas);
				}
				errs = newErrs;
			}
			ArrayList<String> expectedErrs = Utils.erListToStrList(errs);
			String gotStr = Utils.listToStr(expectedErrs).trim();

			if (!gotStr.equals("")) {
				if (thr) {
					//System.out.println();
					System.out.println("No pre bytecode errors were expected...");
					for (String err : expectedErrs) {
						System.out.println(err);
					}
					throw new junit.framework.ComparisonFailure("No pre bytecode errors were expected", "", gotStr);
				}
				return gotStr;
			}
		}
		return null;
	}
}
