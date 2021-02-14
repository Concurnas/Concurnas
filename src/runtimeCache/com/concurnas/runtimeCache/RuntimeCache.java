package com.concurnas.runtimeCache;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.runtime.ClassloaderUtils;
import com.concurnas.runtime.ClassloaderUtils.ClassProvider;
import com.concurnas.runtime.ConcurnasClassLoader;
import com.concurnas.runtime.CpsifierBCProvider;
import com.concurnas.runtime.ISOAugmentorUniversal;
import com.concurnas.runtime.InitConverter2;
import com.concurnas.runtime.OffHeapAugmentor;

public abstract class RuntimeCache implements Opcodes {
	protected ProgressTracker pt;
	protected RuntimeCacheWeaver weaver;
	protected final String toDirectory;
	protected final boolean log;
	protected String[] classpath;
	private int count;
	protected BootstrapLoader modloader;

	public RuntimeCache(String[] classpath, String toDirectory, boolean log) throws IOException {
		this.classpath = classpath;
		this.log = log;
		this.toDirectory = toDirectory;
		Path td = Paths.get(toDirectory);
		if (!Files.exists(td)) {
			Files.createDirectory(td);
		}
	}

	public void doAgumentation(boolean modules) throws Exception {
		int cores = Runtime.getRuntime().availableProcessors();

		HashSet<String> findStaticLambdas = new HashSet<String>();
		ExecutorService executorService = Executors.newFixedThreadPool((int) (cores * 1.5));
		Queue<AugErrorAndException> errorsInAug = new ConcurrentLinkedQueue<AugErrorAndException>();
		
		try {
			this.count = doAug(executorService, findStaticLambdas, errorsInAug);
		} catch (Throwable e) {
			errorsInAug.add(new AugErrorAndException("General Cache genneration", e));
		} finally {
			executorService.shutdown();
			executorService.awaitTermination(1, TimeUnit.HOURS);
		}

		try {
			this.createConcCoreGo(modules, findStaticLambdas);
		} catch (Throwable e) {
			errorsInAug.add(new AugErrorAndException("ConcurnasCore", e));
		}
		
		if(!errorsInAug.isEmpty()) {
			System.err.println(String.format("\nFailure in cache genneration. %s errors detected...", errorsInAug.size()));
			for(AugErrorAndException inst : errorsInAug) {
				if(inst.className != null) {
					System.err.println(String.format("Error in cache genneration for %s: %s" , inst.module, inst.className));
				}else {
					System.err.println(String.format("General error in cache genneration for %s" , inst.module));
				}
				inst.err.printStackTrace();
			}

			System.err.println(String.format("Failure in cache genneration. %s errors detected... Aborting", errorsInAug.size()));
			System.exit(1);
		}
	}

	protected abstract int doAug(ExecutorService executorService, HashSet<String> findStaticLambdas, Queue<AugErrorAndException> errorsInAug) throws Exception;

	protected void assignProgressTracker(int cnt) {
		this.pt = new ProgressTracker(cnt + 1);
	}

	private void addEntry(HashSet<String> collectedPackages, FileSystem zf, HashMap<String, ClassProvider> clsToClasspath, String className, boolean needsWeave, boolean assumeNoPrimordials, boolean doIsoAugment) throws Exception {
		ClassProvider cp = clsToClasspath.get(className);
		byte[] code = cp.provide(className);

		ConcurnasClassLoader ccl = new ConcurnasClassLoader(new Path[] {});
		code = OffHeapAugmentor.addOffHeapMethods(code, ccl);

		if (needsWeave) {

			if (doIsoAugment) {
				CpsifierBCProvider bcp = new CpsifierBCProvider(weaver.rTJarEtcLoader);
				InitConverter2 ic = new InitConverter2(code, bcp, className, assumeNoPrimordials, true);// isGlob =>globals already have init
				code = ic.transform();

				code = ISOAugmentorUniversal.augment(code, weaver.rTJarEtcLoader);
				Path pp = zf.getPath(className + ".class");
				if (!Files.exists(pp.getParent())) {
					Files.createDirectories(pp.getParent());
				}
				Files.write(pp, code);
			} else {
				HashMap<String, byte[]> nameToTrans = weaver.weave(code, log, assumeNoPrimordials);

				for (String name : nameToTrans.keySet()) {
					byte[] ccode = nameToTrans.get(name);

					Path pp = zf.getPath(name + ".class");
					if (!Files.exists(pp.getParent())) {
						Files.createDirectories(pp.getParent());
					}
					Files.write(pp, ccode);
				}
			}
		} else {
			Path pp = zf.getPath(className + ".class");
			if (!Files.exists(pp.getParent())) {
				Files.createDirectories(pp.getParent());
			}
			Files.write(pp, code);
		}

		String cc = className.substring(0, className.lastIndexOf('/'));
		collectedPackages.add(cc);
	}

	public void createConcCoreGo(boolean modules, HashSet<String> findStaticLambdas) throws Exception {
		HashMap<String, ClassProvider> clsToClasspath = new HashMap<String, ClassProvider>();
		for (String thing : this.classpath) {
			Path asPath = Paths.get(thing);
			ClassloaderUtils.populateClasses(asPath, asPath, clsToClasspath, true, null);
		}

		this.createConcCoreJar(modloader.getCpsStateClasses(), clsToClasspath, modules, findStaticLambdas);
		pt.onDone(this.count);
	}

	public void createConcCoreJar(HashMap<String, byte[]> cpsClasses, HashMap<String, ClassProvider> clsToClasspath, boolean modules, HashSet<String> findStaticLambdas) throws Exception {

		String outputfile;
		if (modules) {
			outputfile = "java.base.jar";
		} else {
			outputfile = "conccore.jar";
		}

		HashSet<String> collectedPackages = new HashSet<String>();

		Map<String, String> env = new HashMap<>();
		env.put("create", "true");

		String jarInit = "jar:file:";
		if (!this.toDirectory.startsWith("/")) {
			jarInit += "/";
		}

		URI uri = URI.create((jarInit + toDirectory + File.separator + outputfile).replace('\\', '/'));

		try (FileSystem zf = FileSystems.newFileSystem(uri, env)) {
			// Note: doesn't support lambdas defined at module level (lambda declated in
			// <clinit> method)
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/InitUncreatable", false, false, false);// last arg assume no primodials is false, i.e. permit routing to globalized
																																	// things
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/ref/Ref", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/ReifiedType", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/ClinitOnceIfaceHolder", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/ref/DefaultRef", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/ref/DirectlyAssignable", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/ref/DirectlyGettable", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/ref/DirectlyArrayAssignable", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/ref/DirectlyArrayGettable", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/transactions/TransactionHandler", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/transactions/ChangesAndNotifies", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/transactions/Transaction", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/transactions/LocalTransaction", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/util/LinkedIdentityHashMap", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/TypedActorInterface", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/CopyTracker", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/CopyDefinition", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/ref/LocalArray", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/offheap/Encoder", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/offheap/Decoder", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/offheap/MissingField", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/DefaultMethodRequiresImplementation", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/ref/LocalArray$LAIterator", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/State", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/SyncTracker", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/Fiber", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/Iso", true, false, true);// do iso augmentation
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/IsoTrigger", true, false, true);// do iso augmentation
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/IsoCore", true, false, true);// do iso augmentation
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/IsoNotifiable", true, false, true);// do iso augmentation
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/IsoAwait", true, false, true);// do iso augmentation
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/IsoEvery", true, false, true);// do iso augmentation
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/AbstractIsoTask", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/IsoTask", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/IsoTaskNotifiable", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/IsoTaskAwait", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/IsoTaskEvery", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/ConcurnasSecurityManager", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/Worker", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/Worker$Job", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/Scheduler", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/TerminationState", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/DefaultIsoExceptionHandler", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/Scheduler$SetExceptionToInit", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Stringifier", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Stringifier$NaturalOrderComparator", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Stringifier$1", true, false, false);// this has the comparitor anon class for sorting lists of methods in annotation
																																// lists - to ensure consistant ordering
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$ClassRef", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function0", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function1", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function2", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function3", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function4", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function5", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function6", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function7", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function8", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function9", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function10", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function11", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function12", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function13", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function14", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function15", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function16", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function17", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function18", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function19", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function20", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function21", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function22", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function23", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function0v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function1v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function2v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function3v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function4v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function5v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function6v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function7v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function8v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function9v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function10v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function11v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function12v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function13v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function14v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function15v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function16v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function17v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function18v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function19v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function20v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function21v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function22v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/lang/Lambda$Function23v", true, false, false);
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/CObject", true, false, false);
			// no conc needed for this one:
			addEntry(collectedPackages, zf, clsToClasspath, "com/concurnas/bootstrap/runtime/cps/ReflectionHelper", true, false, false);

			for (String name : cpsClasses.keySet()) {
				byte[] code = cpsClasses.get(name);
				Path pp = zf.getPath(name.replace(".", "/") + ".class");
				if (!Files.exists(pp.getParent())) {
					Files.createDirectories(pp.getParent());
				}
				
				Files.write(pp, code);
			}
			 //System.err.println("" + collectedPackages.stream().sorted().collect(Collectors.toList()));
			
			 if (!findStaticLambdas.isEmpty()) {
				 //System.err.println("" + String.join("\n", findStaticLambdas.stream().sorted().collect(Collectors.toList())));
				 addStaticLambdaClasses(zf, findStaticLambdas, ConcurnasClassLoader.staticLambdaClassesCls);
			 }

			// if(modules) {
			// add module-info.class
			// addModuleInfo(zf,
			// collectedPackages.stream().sorted().collect(Collectors.toList()));
			// }
		}
	}

	public static interface Thing{
		default int sdf() { return 12; }
	}
	
	private void addStaticLambdaClasses(FileSystem zf, HashSet<String> findStaticLambdas, String className) throws IOException {
		ClassWriter classWriter = new ClassWriter(0);
		classWriter.visit(V1_8, ACC_PUBLIC | ACC_SUPER, className, null, "java/lang/Object", null);
		classWriter.visitSource(className + ".java", null);

		{
			MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			methodVisitor.visitCode();
			methodVisitor.visitLabel(new Label());
			methodVisitor.visitVarInsn(ALOAD, 0);
			methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			methodVisitor.visitInsn(RETURN);
			methodVisitor.visitMaxs(1, 1);
			methodVisitor.visitEnd();
		}
		{
			MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "get", "()Ljava/util/HashSet;", "()Ljava/util/HashSet<Ljava/lang/String;>;", null);
			methodVisitor.visitCode();
			methodVisitor.visitLabel(new Label());
			methodVisitor.visitTypeInsn(NEW, "java/util/HashSet");
			methodVisitor.visitInsn(DUP);
			methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/HashSet", "<init>", "()V", false);
			methodVisitor.visitVarInsn(ASTORE, 0);

			findStaticLambdas.stream().sorted().forEach(entry -> {
				methodVisitor.visitLabel(new Label());
				methodVisitor.visitVarInsn(ALOAD, 0);
				methodVisitor.visitLdcInsn(entry);
				methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/util/HashSet", "add", "(Ljava/lang/Object;)Z", false);
				methodVisitor.visitInsn(POP);
			});

			methodVisitor.visitLabel(new Label());
			methodVisitor.visitVarInsn(ALOAD, 0);
			methodVisitor.visitInsn(ARETURN);
			methodVisitor.visitMaxs(2, 1);
			methodVisitor.visitEnd();
		}
		classWriter.visitEnd();

		Path pp = zf.getPath(className + ".class");
		if (!Files.exists(pp.getParent())) {
			Files.createDirectories(pp.getParent());
		}
		Files.write(pp, classWriter.toByteArray());
	}

	/*
	 * private void addModuleInfo(ZipOutputStream zf, List<String> exports) throws
	 * IOException {
	 * 
	 * ClassWriter classWriter = new ClassWriter(0);
	 * 
	 * classWriter.visit(V9, ACC_MODULE, "module-info", null, null, null);
	 * 
	 * classWriter.visitSource("module-info.java", null);
	 * 
	 * ModuleVisitor moduleVisitor = classWriter.visitModule("com.concurnas", 0,
	 * null);
	 * 
	 * moduleVisitor.visitRequire("java.base", ACC_MANDATED, null);
	 * 
	 * for(String exp : exports) { moduleVisitor.visitExport(exp, 0); }
	 * 
	 * classWriter.visitEnd();
	 * 
	 * byte[] code = classWriter.toByteArray(); zf.putNextEntry(new
	 * ZipEntry("module-info.class")); zf.write(code, 0, code.length);
	 * zf.closeEntry();
	 * 
	 * }
	 */

}
