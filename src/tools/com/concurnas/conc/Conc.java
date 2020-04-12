package com.concurnas.conc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.annotation.Retention;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.PredictionMode;

import com.concurnas.compiler.DirectFileLoader;
import com.concurnas.compiler.FileLoader;
import com.concurnas.compiler.SchedulerRunner;
import com.concurnas.repl.REPLShell;
import com.concurnas.runtime.ClassPathUtils;
//warpper:
//take arguments
//check installation for jvm, run/update as needed
//spawn and run jvm
import com.concurnas.runtime.ConcurnasClassLoader;
import com.concurnas.runtimeCache.ReleaseInfo;

public class Conc {

	public final static String genericErrorMsg = "Usage: conc options* entryPoint? cmdLineArguments*\r\n"
			+ "Entry-point may be a .class or .jar file reference\r\n\r\n"
			+ "If no entry-point specified conc will run in interactive REPL mode\r\n\r\n"
			+ "Any command line arguments will be passed to the main(String[] args) void method of the entry-point class if one is specified\r\n\r\n"
			+ "Use option: --help to list all possible options";
	
	public final static String helpMeErrorMsg = genericErrorMsg + "\r\n\r\n" 
			+ "-classpath path or -cp path: \r\n"
			+ "   The classpath option enables one to override the CLASSPATH environment variable.\r\n"
			+ "   Elements are delimited via ';' and must be surrounded by \" \" under Unix based operating systems.\r\n"
			+ "   Elements may consist of .class file references directories or .jar file references.\r\n" 
			+ "-s\r\n"
			+ "   Server mode. When running in non interactive mode (i.e an entry point is provided)\r\n"
			+ "   Concurnas will not terminate the process upon the entry point main method (or \r\n"
			+ "   alternative) completing execution. \r\n"
			+ "-bc: \r\n"
			+ "   Print bytecode. This option is applicable when running in REPL mode.\r\n"
			+ "   Print the bytecode generated from the provided input.\r\n"
			+ "-J: \r\n"
			+ "   JVM argument. Prefix jvm arguments with -J in order to pass them to the underlying JVM. \r\n"
			+ "   e.g. -J-Xms2G -J-Xmx5G.\r\n"
			+ "-D: \r\n"
			+ "   System property. Prefix system properties with -D in order to pass them to the\r\n"
			+ "   underlying JVM. e.g. -Dcom.mycompany.mysetting=108.\r\n"
			+ "--help: \r\n"
			+ "   List documentation for options above.";
	
	//0 is ok
	public static void main(String[] args) {
		String inputString = String.join(" ", args);
		Conc inst = new Conc(inputString);
		String result = inst.doit();
		
		System.out.println(result);
		
		System.exit(inst.returnCode);
	}

	private FileLoader fileLoader;
	private String inputString;
	private int returnCode = 0;

	private ClassLoaderProvider clp;
	public static class DefaultClassLoaderProvider implements ClassLoaderProvider{
		@Override
		public ConcClassLoader apply(Path[] classes, Path[] bootstrap) {
			return new ConcClassLoader(classes, bootstrap);
		}
	}
	
	
	public Conc(String inputString) {
		this(inputString, new DirectFileLoader(), new DefaultClassLoaderProvider());
	}
	
	public Conc(String inputString, FileLoader fileLoader) {
		this(inputString, fileLoader, new DefaultClassLoaderProvider());
	}
	
	public Conc(String inputString, FileLoader fileLoader, ClassLoaderProvider clp) {
		this.fileLoader = fileLoader;
		this.inputString = inputString;
		this.clp = clp;
	}
	
	public ConcInstance getConcInstance() {
		ConcBuilder builder = createBuilder();
		if(null == builder) {
			return null;
		}else {
			return  builder.concInstance;
		}
	}
	
	public static class ValidConcObject{
		public boolean serverMode;
		public String[] cmdLineArgs;
		public Class<?> entryClass;
		public ConcurnasClassLoader concClassLoader;
		public boolean bytecode;
		public boolean werror;
		
		public ValidConcObject( boolean serverMode, String[] cmdLineArgs, Class<?> entryClass, ConcurnasClassLoader concClassLoader, boolean bytecode, boolean werror) {
			this.serverMode = serverMode;
			this.cmdLineArgs = cmdLineArgs;
			this.entryClass = entryClass;
			this.concClassLoader = concClassLoader;
			this.bytecode = bytecode;
			this.werror = werror;
		}
		
		@Override 
		public String toString() {
			StringBuilder sb = new StringBuilder();
			
			if(serverMode) {
				sb.append("-s ");
			}
			
			if(null != entryClass) {
				sb.append(entryClass);
			}
			return sb.toString();
		}
		
	}
	
	public static class ValidationErrorsAndValidObject{
		public String validationErrs;
		public ValidConcObject validconcObject;
		
		public ValidationErrorsAndValidObject(String validationErrs, ValidConcObject validconcObject) {
			this.validationErrs = validationErrs;
			this.validconcObject = validconcObject;
		}
	}
	
	public String doit() {			
		ConcInstance concInstance = getConcInstance();
		if(null == concInstance) {
			returnCode=1;
			return genericErrorMsg;
		}else {
			if(concInstance.helpMe) {
				return helpMeErrorMsg;
			}
			
			ValidationErrorsAndValidObject errors = validateConcInstance(concInstance);
			if(null != errors.validationErrs) {
				return errors.validationErrs;
			}
			
			
			/*
			 * if(errors.validconcObject.entryClass == null) { //disable REPL for current
			 * release cycle returnCode=1; return genericErrorMsg; }
			 */
			
			//no errors proceed to execution
			return doMainLoop(errors.validconcObject);
		}
	}
	

	private ClassLoader mainClassLoader = Thread.currentThread().getContextClassLoader();
	
	public static class ConcClassLoader extends ConcurnasClassLoader{
		public ConcClassLoader(Path[] classpath, Path[] bootstrap){
			super(classpath, bootstrap);
		}
	}
	
	public ConcurnasClassLoader getConcClassloader(ArrayList<Path> classpath) {//assumes setCustomClassPath has been called etc
		Path[] cpele;
		{
			Path[] sysPath = ClassPathUtils.getSystemClassPathAsPaths();
			int ln = sysPath.length;
			if(!classpath.isEmpty()) {
				ln += classpath.size();
			}
			cpele = new Path[ln];
			
			int n=0;
			for(; n < sysPath.length; n++) {
				cpele[n] = sysPath[n];
			}
			
			if(!classpath.isEmpty()) {
				for(Path pp : classpath) {
					cpele[n++] = pp; 
				}
			}
		}
		
		return clp.apply(cpele, ClassPathUtils.getInstallationPath());
	}

	private static final String concEXEName = "$ConcEXE";
	
	private void doPreLoad() {
		//These classes require loading outside of continuations in order to function correctly
		Retention.class.getAnnotations();
	}
	
	private String doMainLoop(ValidConcObject validconcObject) {
		try {
			doPreLoad();
			
			Class<?> entryClass = validconcObject.entryClass;
			
			if(null == entryClass) {//delegate to REPL
				String concVersion = ReleaseInfo.getVersion();
				String vmname = System.getProperty("java.vm.name");
				
				String version = System.getProperty("java.version");
				if(null == version) {
					version = System.getProperty("java.specification.version");
				}
				
				returnCode = REPLShell.replLoop(concVersion, vmname, version, validconcObject.bytecode, false);
				return "";
			}else {
				ConcurnasClassLoader concClassLoader = validconcObject.concClassLoader;
				
				boolean consumeCmdLineParams = true;
				
				Method main = null;
				main = getMethod(entryClass, "main", String[].class);// = entryClass.getMethod("main", String[].class);
				
				if(main == null || main.getReturnType() != void.class) {
					consumeCmdLineParams=false;
					
					main = getMethod(entryClass, "main");
					//main = entryClass.getMethod("main", new Class<?>[] {});
					
					if(null == main || main.getReturnType() != void.class) {//run top level code
						main = null;
					}
				}
				byte[] executor =  new ConcTaskMaker(concEXEName, entryClass.getName().replace('.', '/'), main).gennerate();
				//BytecodePrettyPrinter.print(executor, true);
				concClassLoader.defineClass(concEXEName, executor);

				SchedulerRunner sch = new SchedulerRunner(concClassLoader, "Concurnas");
				
				Class<?> executorTasCls = concClassLoader.loadClass(concEXEName);
				Object exeTaObject = executorTasCls.newInstance();
				
				if(consumeCmdLineParams) {
					executorTasCls.getField(TaskMaker.CMDLineParamsStr).set(exeTaObject, validconcObject.cmdLineArgs);
				}
				
				sch.invokeScheudlerTask(exeTaObject);	
				Method isDoneMethod = getMethod(executorTasCls, "isDone");
					

				isDoneMethod.invoke(exeTaObject);
				if (validconcObject.serverMode) {
					// wait before stop...
					Runtime.getRuntime().addShutdownHook(new Thread() {
						public void run() {
							shutdown();
						}
					});

					synchronized (this) {
						while (running) {
							this.wait();
						}
					}
				}
				
				sch.stop();
			}
			
		}
		catch(Throwable thr) {
			returnCode=2;
			
			ByteArrayOutputStream out = new ByteArrayOutputStream(); 
			thr.printStackTrace(new PrintStream(out));
			
			return "Unknown error in conc: " + thr.getMessage() + "\n" + new String(out.toByteArray());
		}
		
		return "";
	}
	
	public boolean running = true;
	
	public synchronized void shutdown() {
		running=false;
		this.notifyAll();
	}
	

	private static Method getMethod(Class<?> cls, String name, Class<?>... inputs){
		int args = inputs.length;
		Method[] meths= cls.getMethods();
		for(Method m : meths){
			if(m.getName().equals(name)) {
				Class<?>[] params = m.getParameterTypes();
				if(params.length == args) {
					boolean pass = true;
					for(int n = 0; n< args; n++) {
						if(!inputs[n].equals(params[n])){
							pass=false;
							break;
						}
					}
					if(pass) {
						return m;
					}
				}
			}
		}
		return null;
	}
	
	public ValidationErrorsAndValidObject validateConcInstance(ConcInstance concInstance) {
		ArrayList<String> validationErrs = new ArrayList<String>();
		
		try {
			ArrayList<Path> classpath = new ArrayList<Path>();
			if(concInstance.classpath != null) {
				classpath = new ArrayList<Path>();
				for(String classpathEntry : concInstance.classpath) {
					Path path = this.fileLoader.getPath(classpathEntry);
					if(path == null) {
						validationErrs.add(String.format("Classpath entry '%s' does not exist", classpathEntry));
					}else if(Files.isRegularFile(path) && !path.toString().endsWith(".jar")) {
						validationErrs.add(String.format("Invalid classpath entry '%s', only directories or jar files can be class path entries", classpathEntry));
					}else if(!Files.isReadable(path)) {
						validationErrs.add(String.format("Classpath entry '%s' does not exist", classpathEntry));
					}else {
						classpath.add(path);
					}
				}
			}
			
			Class<?> entryClass = null;
			ConcurnasClassLoader concClassLoader = null;
			
			if(concInstance.sourceFile != null && !concInstance.sourceFile.trim().isEmpty()) {
				if(concInstance.bytecode) {
					validationErrs.add("-bc: Print bytecode option is applicable only when run in REPL mode");
				}
				
				if(concInstance.werror) {
					validationErrs.add("-werror: Treat warnings as errors option is applicable only when run in REPL mode");
				}
				
				Path srcFile = this.fileLoader.getPath(concInstance.sourceFile);
				String entryPoint = srcFile.toString();
				boolean haserr = false;
				if(!Files.exists(srcFile) || Files.isDirectory(srcFile)) {//try as class and jar
					if(!entryPoint.endsWith(".jar") && !entryPoint.endsWith(".class")) {
						//srcFile = null;
						Path asJar = this.fileLoader.getPath(concInstance.sourceFile + ".jar");
						Path asClass = this.fileLoader.getPath(concInstance.sourceFile + ".class");
						
						boolean jarExists = Files.exists(asJar);
						boolean clsExists = Files.exists(asClass);
						
						if(jarExists && clsExists) {
							haserr = true;
							validationErrs.add(String.format("Ambigious source file refernce, %s and %s found", asJar, asClass));
						}else if(jarExists && !clsExists) {
							srcFile = asJar;
						}else if(clsExists) {
							srcFile = asClass;
						}
					}else{
						haserr = true;
						validationErrs.add(String.format("Cannot find entry point file: %s", entryPoint));
						srcFile=null;
					}
				}
				
				if(srcFile == null) {
					if(!haserr) {
						validationErrs.add("Cannot find entry point: " + concInstance.sourceFile);
					}
				}else {
					entryPoint = srcFile.toString();
					String clasRef = entryPoint;
					boolean fromJar=false;
					boolean jarOrClass = false;
					if(entryPoint.endsWith(".jar")) {//search manifest for entry point, non found error
						jarOrClass=true;
						URI jarUri = URI.create("jar:" + srcFile.toUri());
						Map<String, String> env = new HashMap<String, String>(); 
				        env.put("create", "true");
				       try (FileSystem zipfs = FileSystems.newFileSystem(jarUri, env)) {
				            Path manifest = zipfs.getPath("/META-INF/MANIFEST.MF");
				            if(!Files.exists(manifest)) {
				            	haserr=true;
								validationErrs.add(String.format("Manifest file META-INF/MANIFEST.MF is missing from jar: %s", entryPoint));
				            }else {
				            	boolean foiundClsref = false;
					            for(String line : Files.readAllLines(manifest)) {
					            	if(line.startsWith("Main-Class: ")) {
					            		foiundClsref=true;
					            		clasRef = line.substring(12).trim();
					            		break;
					            	}
					            }
					            
					            if(!foiundClsref) {
					            	haserr=true;
									validationErrs.add("Manifest file META-INF/MANIFEST.MF is missing 'Main-Class' entry");
					            }
				            }
				        } 
						fromJar = true;
					}else if(entryPoint.endsWith(".class")) {
						jarOrClass=true;
						byte[] codex = Files.readAllBytes(srcFile);
						//parse in asm and read filename
						clasRef = ConcUtis.extractClassName(codex);
						clasRef = clasRef.replace('/', '.');
					}
					
					boolean wasEmptyCp = classpath.isEmpty();
					if(jarOrClass) {
						classpath.add(srcFile);
						
						if(wasEmptyCp && !srcFile.toString().contains(File.separator)) {//if it's in the root 
							classpath.add(this.fileLoader.getPath("./"));
						}
					}
					
					
					
					
					
					concClassLoader = getConcClassloader(classpath);
					
					try {
						entryClass = concClassLoader.loadClass(clasRef);
					}catch(ClassNotFoundException e) {
						entryClass = null;
					}
					
					if(null == entryClass && !haserr) {
						validationErrs.add("Cannot find entry-point class to load: " + clasRef + (fromJar?" specified in manifest of: " + entryPoint: ""));
					}
					
					
				}
				
			}else {//REPL mode
				if(concInstance.serverMode) {
					validationErrs.add("Server mode is applicable only when run in non-interactive mode");
				}
			}
	
			//lovely
			ValidConcObject vco = new ValidConcObject(concInstance.serverMode, concInstance.cmdLineArgs, entryClass, concClassLoader, concInstance.bytecode, concInstance.werror);
			return new ValidationErrorsAndValidObject(validationErrs.isEmpty()?null:String.join("\n", validationErrs), vco);
		}
		catch(Throwable thr) {
			returnCode=2;
			thr.printStackTrace();
			
			ByteArrayOutputStream out = new ByteArrayOutputStream(); 
			thr.printStackTrace(new PrintStream(out));
			
			validationErrs.add("Unknown error in conc: " + thr.getMessage() + "\n" + new String(out.toByteArray()));
		}
		
		return new ValidationErrorsAndValidObject(String.join("\n", validationErrs), null);
	}
	
	
	
	public static class ErrorCap extends BaseErrorListener{
		public boolean hasErrors = false;
		@Override
		public void syntaxError(Recognizer<?,?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String message, RecognitionException e) {
			//System.err.println("" + message);
			hasErrors = true;
		}
	}
	
	

	public ConcBuilder createBuilder() {
		ErrorCap errors = new ErrorCap();
		ConcBuilder builder;
		try {
			CharStream input = CharStreams.fromString(inputString, "cmd");
			
			ConcLexer lexer = new ConcLexer(input);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			ConcParser parser = new ConcParser(tokens);
			
			parser.getInterpreter().setPredictionMode(PredictionMode.SLL); 
			
			parser.removeErrorListeners(); // remove ConsoleErrorListener
			parser.addErrorListener(errors); // add ours
			
			lexer.removeErrorListeners(); // remove ConsoleErrorListener
			lexer.addErrorListener(errors); // add ours
			
			builder = new ConcBuilder();
			parser.conc().accept(builder);
			
		}catch(Throwable e) {
			return null;
		}
		
		if(errors.hasErrors) {
			return null;
		}
		
		return builder;
	}
	
	private PrintStream ps = null;
	public void setConsoleCaputure(PrintStream ps) {
		this.ps = ps;
	}
}
