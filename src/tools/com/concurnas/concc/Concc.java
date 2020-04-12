package com.concurnas.concc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.PredictionMode;

import com.concurnas.compiler.DirectFileLoader;
import com.concurnas.compiler.DirectFileWriter;
import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.FileLoader;
import com.concurnas.compiler.FileWriter;
import com.concurnas.compiler.MainLoop;
import com.concurnas.compiler.MainLoop.CompilationMessages;
import com.concurnas.compiler.MainLoop.SourceCodeInstance;
import com.concurnas.concc.ConccInstance.SourceToCompile;
import com.concurnas.runtime.Pair;

public class Concc {

	public final static String genericErrorMsg = "Usage: concc options* source files+\r\n"
			+ "Source files may be directories or files ending with .conc\r\n"
			+ "The root directory per source file entry may be overriden as: root[source files+]\r\n" 
			+ "Use option: --help to list all possible options";
	
	public final static String helpMeErrorMsg = genericErrorMsg + "\r\n\r\n" + 
			"-d directory: \r\n"
			+ "   Override the directory where .class files will be output. concc will try to create\r\n"
			+ "   this directory if it does not already exist.\r\n"
			+ "-a or -all:\r\n"
			+ "   Copy all non .conc files from source directories to \r\n"
			+ "   provided output directory (output overridden with the -d option.)\r\n"
			+ "-jar jarName[entryPoint]?:\r\n"
			+ "   Creates a jar file from all generated/copied files. Optional entryPoint class \r\n"
			+ "   name may be specified which will be used to populate a META-INF/MANIFEST.MF \r\n"
			+ "   file within the jar. Can only be used in conjunction with the -d option.\r\n"
			+ "-c or -clean:\r\n"
			+ "   Remove all generated files from output directory. Useful in conjunction \r\n" 
			+ "   with -jar option. Any generated jar files will not be removed.\r\n"
			+ "-classpath path or -cp path: \r\n"
			+ "   The classpath option enables one to override the CLASSPATH environment variable.\r\n"
			+ "   Elements are delimited via ';' and must be surrounded by \" \" under Unix based operating systems.\r\n"
			+ "   Elements may consist of .class file references directories or .jar file references.\r\n" 
			+ "-verbose: \r\n"
			+ "   Provide additional compilation information including bytecode output.\r\n"
			+ "-root directory: \r\n"
			+ "   Override the root directory of javacc. Package names for .conc files will be\r\n"
			+ "   generated relative to this.\r\n"
			+ "-werror: \r\n"
			+ "   Normally, warnings do not prevent class file generation, setting this tag will\r\n"
			+ "   treat warnings as errors, thus preventing class file generation if warnings\r\n"
			+ "   are generated for the input .conc source file in question.\r\n"
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
		Concc inst = new Concc(inputString);
		String result = inst.doit();
		
		System.out.println(result);
		
		System.exit(inst.returnCode);
	}

	private String inputString;
	private int returnCode = 0;

	private final FileLoader fileLoader;
	private final FileWriter fileWriter;
	
	public Concc(String inputString) {
		this(inputString, new DirectFileLoader(), new DirectFileWriter());
	}
	
	public Concc(String inputString, FileLoader fileLoader, FileWriter fileWriter) {
		this.fileLoader = fileLoader;
		this.fileWriter = fileWriter;
		this.inputString = inputString;
	}
	
	public static class ErrorCap extends BaseErrorListener{
		public boolean hasErrors = false;
		@Override
		public void syntaxError(Recognizer<?,?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String message, RecognitionException e) {
			hasErrors = true;
		}
	}
	
	public ConccInstance getConccInstance() {
		ConccBuilder builder = createBuilder();
		if(null == builder) {
			return null;
		}else {
			ConccInstance concInstance = builder.concInstance;
			return  concInstance;
		}
	}
	
	public String doit() {			
		ConccInstance concInstance = getConccInstance();
		if(null == concInstance) {
			returnCode=1;
			return genericErrorMsg;
		}else {
			if(concInstance.helpMe) {
				return helpMeErrorMsg;
			}
			
			ValidationErrorsAndValidObject errors = validateConccInstance(concInstance);
			if(null != errors.validationErrs) {
				return errors.validationErrs;
			}
			
			return doMain(errors.validconcObject);
		}
	}
	
	private static class FileWriterPassThrough implements FileWriter{

		private FileWriter passto;
		private ArrayList<Path> created;

		public FileWriterPassThrough(FileWriter passto, ArrayList<Path> created) {
			this.passto = passto;
			this.created = created;
		}
		
		@Override
		public Path writeClass(String path, String name, byte[] bytecode) throws IOException {
			Path ret = this.passto.writeClass(path, name, bytecode);
			created.add(ret);
			return ret;
		}

		@Override
		public boolean hasWrittenClass(String name) {
			return this.passto.hasWrittenClass(name);
		}

		@Override
		public byte[] getWrittenClass(String name) {
			return this.passto.getWrittenClass(name);
		}

		@Override
		public void removeClassAndAllNestedInstances(String path) {
			this.passto.removeClassAndAllNestedInstances(path);
		}

		@Override
		public Path getOutputPath(String path) throws IOException {
			return this.passto.getOutputPath(path);
		}

		@Override
		public Path getRoot() {
			return this.passto.getRoot();
		}
		
	}
	
	
	private String doMain(ValidConccObject validconcObject) {
		MainLoop ml = null;
		try {
			boolean verboseErrors=true;
			boolean verboseInfo=true;
			boolean printBytecode = validconcObject.verbose;
			
			ArrayList<Path> created = new ArrayList<Path>();
			
			FileWriter captureWritten = new FileWriterPassThrough(this.fileWriter, created);
			
			ml = new MainLoop("", this.fileLoader, verboseErrors, verboseInfo, captureWritten, printBytecode /*, boolean debugMode*/);
			if(null != validconcObject.outputDirectoryOverride) {
				ml.setOutputDirectoryOverride(validconcObject.outputDirectoryOverride.toAbsolutePath().toString());
			}
			
			if(null != ps) {
				ml.setConsoleOutput(ps);
			}
			
			if(validconcObject.warnAsError) {
				ml.setWarnsAsErrors(true);
			}
			
			if(validconcObject.classpath != null) {
				URL[] urls = new URL[validconcObject.classpath.size()];
				int n=0;
				for(Path pp : validconcObject.classpath) {
					urls[n++] = pp.toUri().toURL();
				}
				
				ml.setCustomClassPath(urls);
			}
			
			CompilationMessages msgs = ml.compileFile(validconcObject.sources);
			if(msgs.hasErrors) {
				ArrayList<String> items = new ArrayList<String>(msgs.messages.size());
				
				for(ErrorHolder er : msgs.messages) {
					items.add(er.toString());
				}
				
				return String.join("\n", items);
			}
			
			if(null != validconcObject.nonConcfiles) {
				
				final Path outputDir = validconcObject.outputDirectoryOverride;
				
				for(Pair<Path, Path> toCopyRoot : validconcObject.nonConcfiles) {
					Path toCopy = toCopyRoot.getA();
					Path root = toCopyRoot.getB();
					
					int ignoreFirst = 0;
					for(Path _rr : root) {
						ignoreFirst++;
					}
					Path outputfile = outputDir;
					for(Path comp : toCopy) {
						if(ignoreFirst-- <=0) {
							outputfile = outputfile.resolve(comp);
						}
					}
					
					Files.copy(toCopy, outputfile, StandardCopyOption.REPLACE_EXISTING);
					created.add(outputfile);
				}
			}
			
			if(validconcObject.outputJarPath != null) {	//create jar from following files:
		        Map<String, String> env = new HashMap<>(); 
		        env.put("create", "true");
				
				URI jarUri = URI.create("jar:" + validconcObject.outputJarPath.toUri());
				try (FileSystem zipfs = FileSystems.newFileSystem(jarUri, env)) {
					for (Path cr : created) {
						cr = cr.toAbsolutePath();
						int ignoreFirst = 0;
						for (Path _rr : validconcObject.outputDirectoryOverride.toAbsolutePath()) {
							ignoreFirst++;
						}
						Path outputPathIjnZip = zipfs.getPath("/");
						for (Path comp : cr) {
							if (ignoreFirst-- <= 0) {
								outputPathIjnZip = outputPathIjnZip.resolve(zipfs.getPath(comp.toString()));
							}
						}
						if(!Files.exists(outputPathIjnZip)) {
							Files.createDirectories(outputPathIjnZip);
						}
						// copy a file into the zip file
						Files.copy(cr, outputPathIjnZip, StandardCopyOption.REPLACE_EXISTING);
					}
					
					if(null != validconcObject.mfestEntryPoint) {
						Path outputPathIjnZip = zipfs.getPath("/");
						Path manifestFile = outputPathIjnZip.resolve(zipfs.getPath("META-INF"));
						Files.createDirectories(manifestFile);
						manifestFile = manifestFile.resolve(zipfs.getPath("MANIFEST.MF"));
						
						List<String> li = new ArrayList<String>();
						li.add("Manifest-Version: 1.0");
						li.add("Main-Class: " + validconcObject.mfestEntryPoint);
						Files.write(manifestFile, li, StandardCharsets.UTF_8);
					}
					
				} 
			}
			
			if(validconcObject.clean) {
				for (Path cr : created) {
					Files.delete(cr);
				}
			}
		}
		catch(Throwable thr) {
			returnCode=2;
			
			ByteArrayOutputStream out = new ByteArrayOutputStream(); 
			thr.printStackTrace(new PrintStream(out));
			
			return "Unknown error in concc: " + thr.getMessage() + "\n" + new String(out.toByteArray());
		}finally {
			if(ml != null) {
				ml.stop();
			}
		}
		
		return "";
	}
	
	
	public static class ValidationErrorsAndValidObject{
		public String validationErrs;
		public ValidConccObject validconcObject;
		
		public ValidationErrorsAndValidObject(String validationErrs, ValidConccObject validconcObject) {
			this.validationErrs = validationErrs;
			this.validconcObject = validconcObject;
		}
	}
	
	public static class ValidConccObject{
		public Path outputDirectoryOverride;
		public ArrayList<Path> classpath;
		public ArrayList<SourceCodeInstance> sources;
		public boolean warnAsError;
		public boolean verbose;
		public ArrayList<Pair<Path, Path>> nonConcfiles;
		public Path outputJarPath;
		public String mfestEntryPoint;
		public boolean clean;
		
		public ValidConccObject(Path outputDirectoryOverride, ArrayList<Path> classpath, ArrayList<SourceCodeInstance> sources, boolean warnAsError, boolean verbose, ArrayList<Pair<Path, Path>> nonConcfiles, Path outputJarPath, String mfestEntryPoint, boolean doCleanup) {
			this.outputDirectoryOverride = outputDirectoryOverride;
			this.classpath = classpath;
			this.sources = sources;
			this.warnAsError = warnAsError;
			this.verbose = verbose;
			this.nonConcfiles = nonConcfiles;
			this.outputJarPath = outputJarPath;
			this.mfestEntryPoint = mfestEntryPoint;
			this.clean = doCleanup;
		}
		
		@Override 
		public String toString() {
			StringBuilder sb = new StringBuilder();
			
			if(nonConcfiles != null) {
				sb.append("-a ");
			}
			if(clean) {
				sb.append("-c ");
			}
			
			if(null != outputDirectoryOverride) {
				sb.append("-d ");
				sb.append(outputDirectoryOverride + " ");
			}
			
			if(null != outputJarPath) {
				sb.append("-jar ");
				sb.append(outputJarPath);
				if(null != mfestEntryPoint) {
					sb.append("(");
					sb.append(mfestEntryPoint);
					sb.append(")");
				}
				sb.append(" ");
			}
			
			if(null != classpath) {
				sb.append("-cp ");
				ArrayList<String> cpString = new ArrayList<String>();
				classpath.forEach(a -> cpString.add(a.toString()));
				sb.append(String.join(File.pathSeparator, cpString) + " ");
			}
			
			ArrayList<String> srcString = new ArrayList<String>();
			sources.forEach(a -> srcString.add(a.toString()));
			sb.append(String.join(" ", srcString));
			
			return sb.toString();
		}
		
	}
	
	
	public ValidationErrorsAndValidObject validateConccInstance(ConccInstance concInstance) {
		ArrayList<String> validationErrs = new ArrayList<String>();
		
		
		try {
			if(concInstance.sources.isEmpty()) {
				validationErrs.add("There must be at least one source file or source directory specified");
			}
			
			Path root = this.fileLoader.getPath(".");
			if(root == null || !Files.isDirectory(root)) {
				validationErrs.add("Unable to resolve working directory");
				root = null;
			}
			
			//override root
			if(null != concInstance.globalRoot) {
				Path overrideRoot = this.fileLoader.getPath(concInstance.globalRoot);
				if(overrideRoot == null || !Files.isDirectory(overrideRoot)) {
					validationErrs.add(String.format("Root directory override: '%s' does not exist", concInstance.globalRoot));
					overrideRoot = null;
				}
				
				if(null != overrideRoot) {
					root = overrideRoot;
				}
			}
			
			//output dir
			ArrayList<Pair<Path, Path>> nonConcfiles = null;
			Path outputDirOverride = null;
			if(null != concInstance.outputDirectory) {
				outputDirOverride = this.fileLoader.getPath(concInstance.outputDirectory);
				
				/*
				 * if(outputDirOverride == null) { this.fileLoader. }
				 */
				
				if(outputDirOverride == null || !Files.isDirectory(outputDirOverride)) {
					if(Files.exists(outputDirOverride)) {
						validationErrs.add(String.format("Cannot write to output directory '%s' - it is a file", concInstance.outputDirectory));
						outputDirOverride=null;
					}else {
						try {
							Files.createDirectories(outputDirOverride);
						}catch(Exception e){
							validationErrs.add(String.format("Cannot create output directory '%s' - %s", concInstance.outputDirectory, e.getMessage()));
							outputDirOverride=null;
						}
						
					}
				}
				
				if(concInstance.copyall) {
					nonConcfiles = new ArrayList<Pair<Path, Path>>();
				}
				//root = path;
			}else {
				if(concInstance.copyall) {
					validationErrs.add("-a option may only be used when output directory is specified");
				}
				if(concInstance.outputJar != null) {
					validationErrs.add("-jar option may only be used when output directory is specified");
				}
			}
			
			Path outputJarPath = null;
			if(concInstance.outputJar != null) {
				outputJarPath = this.fileLoader.getPath(concInstance.outputJar);
				if(outputDirOverride != null) {
					Path aggre = outputDirOverride.resolve(outputJarPath);
					
					{
						try {
							File f = new File(aggre.toString());
							f.getCanonicalPath();
							outputJarPath = aggre;
						} catch (Exception e) {//no try what user input
						}
					}
				}
			}
			
			
			if(root == null) {
				throw new Exception("Unable to resolve working directory");
			}
			
			ArrayList<Path> classpath = null;
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
			
			ArrayList<SourceCodeInstance> srcInstances = new ArrayList<SourceCodeInstance>();
			
			String sep = this.fileLoader.getSeparator();
			
			for(SourceToCompile src : concInstance.sources) {
				Path rootInstance = root;
				if(null != src.root) {
					Path rootForSource = this.fileLoader.getPath(src.root);
					if(rootForSource == null || !Files.isDirectory(rootForSource)) {
						validationErrs.add(String.format("Root directory: '%s' for source element does not exist", src.root));
						continue;
					}
					rootInstance = rootForSource;
				}
				
				String fileOrDir = src.dirOrFile;
				
				Path fileOrDirPath = this.fileLoader.getPath(fileOrDir);
				
				if(!(fileOrDirPath != null && Files.exists(fileOrDirPath))) {
					//doesnt resolve to something already, so append to root
					try {
						String wtf = rootInstance.toString();
						fileOrDirPath = this.fileLoader.getPath(wtf + sep + fileOrDir.toString());
					}catch(Throwable e) {
						fileOrDirPath=null;
						throw e;
					}
				}else if(Files.isDirectory(fileOrDirPath)){
					rootInstance = fileOrDirPath;
				}
				
				if(fileOrDirPath != null && Files.exists(fileOrDirPath)) {
					//great!
					if(Files.isDirectory(fileOrDirPath)) {//nested search, with root as start of package name
						traverseDir(srcInstances, nonConcfiles, fileOrDirPath, rootInstance, true, null, sep);
					}else {//search for other files in same directory for secondary inclusion
						srcInstances.add(makeSourceCodeInstance(rootInstance, fileOrDirPath, true, sep));
						Path rootx = fileOrDirPath.getParent();
						if(null == rootx) {
							rootx = Paths.get(".");
						}
						traverseDir(srcInstances, nonConcfiles, rootx, rootInstance, false, fileOrDirPath, sep);
					}
				}else {
					if(fileOrDirPath == null) {
						validationErrs.add(String.format("Unable to resolve source file/directory of: '%s' ", fileOrDir));
					}else {
						validationErrs.add(String.format("Unable to resolve source file/directory of: '%s' ", fileOrDirPath));
					}
				}
			}
			
			if(srcInstances.isEmpty()) {
				validationErrs.add("No .conc files found to compile. Use: concc -help or concc --help for assistance.");
			}
			
			//lovely
			ValidConccObject vco = new ValidConccObject(outputDirOverride, classpath, srcInstances, concInstance.warnAsError, concInstance.verbose, nonConcfiles, outputJarPath, concInstance.mfestEntryPoint, concInstance.doCleanup);
			return new ValidationErrorsAndValidObject(validationErrs.isEmpty()?null:String.join("\n", validationErrs), vco);
		}
		catch(Throwable thr) {
			returnCode=2;
			
			ByteArrayOutputStream out = new ByteArrayOutputStream(); 
			thr.printStackTrace(new PrintStream(out));
			
			validationErrs.add("Unknown error in concc: " + thr.getMessage() + "\n" + new String(out.toByteArray()));
		}
		
		String ers = String.join("\n", validationErrs);
		if(!validationErrs.isEmpty()) {
			ers += "\n";
		}
		
		return new ValidationErrorsAndValidObject(ers, null);
	}
	
	private static SourceCodeInstance makeSourceCodeInstance(Path root, Path file, boolean mustCompile, String sep) {
		String absPath = root.toAbsolutePath().toString();
		String absInst = file.toAbsolutePath().toString();
		
		String postPath = absInst.substring(absPath.length()+1);
		
		String packagename;
		int lai = postPath.lastIndexOf(sep);
		if(lai > -1) {
			String regex = sep.replaceAll("\\\\", "\\\\\\\\");
			String[] comps = postPath.substring(0, lai).split(regex);
			packagename = String.join(".", comps);
		}else {
			packagename = "";
		}
		
		return new SourceCodeInstance(file.toAbsolutePath().toString(), packagename, mustCompile);
	}
	
	private static void traverseDir(ArrayList<SourceCodeInstance> addto, ArrayList<Pair<Path, Path>> nonConcfiles, Path path, Path root, boolean mustCompile, Path toIgnore, String sep) {
	    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
	        for (Path entry : stream) {
	            if (Files.isDirectory(entry)) {
	            	if(toIgnore == null) {
		                traverseDir(addto, nonConcfiles, entry, root, mustCompile, null, sep);
	            	}
	            } else {
	            	if(null != toIgnore && toIgnore.equals(entry) ) {
	            		continue;//skip this one
	            	}
	            	
	            	if(entry.toString().endsWith(".conc")) {
	            		addto.add(makeSourceCodeInstance(root, entry, mustCompile, sep));
	            	}else if(null != nonConcfiles){
	            		nonConcfiles.add(new Pair<Path, Path>(entry, root));
	            	}
	            }
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	public ConccBuilder createBuilder() {
		ErrorCap errors = new ErrorCap();
		ConccBuilder builder;
		try {
			CharStream input = CharStreams.fromString(inputString, "cmd");
			
			ConccLexer lexer = new ConccLexer(input);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			ConccParser parser = new ConccParser(tokens);
			
			parser.getInterpreter().setPredictionMode(PredictionMode.SLL); 
			
			parser.removeErrorListeners(); // remove ConsoleErrorListener
			parser.addErrorListener(errors); // add ours
			
			lexer.removeErrorListeners(); // remove ConsoleErrorListener
			lexer.addErrorListener(errors); // add ours
			
			builder = new ConccBuilder();
			parser.concc().accept(builder);
			
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
