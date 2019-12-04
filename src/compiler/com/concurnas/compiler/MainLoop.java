package com.concurnas.compiler;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.antlr.v4.misc.OrderedHashMap;

import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.bytecode.FuncLocation.DummyFuncLocation;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.CompiledClassUtils;
import com.concurnas.compiler.utils.ITEM_TYPE;
import com.concurnas.compiler.utils.Profiler;
import com.concurnas.compiler.utils.StringTrie;
import com.concurnas.compiler.utils.TypeDefTypeProvider;
import com.concurnas.runtime.ClassPathUtils;
import com.concurnas.runtime.ConcurnasClassLoader;
import com.concurnas.runtime.Pair;

//classpath: directory | single file .class | single jar | or wildcard...

public class MainLoop {
	
	private final String rootDir;
	public boolean VERBOSE_ERRORS  = false;
	public boolean VERBOSE_INFO  = false;
	//private boolean DEBUG_MODE=false;
	private final FileLoader loader;
	private final FileWriter filewriter;

	private ClassLoader mainClassLoader;
	private ConcurnasClassLoader langExtClassLoader;
	private String outputDirOverride = null;
	private boolean warnsAsErrors = false;
	private boolean printBytecode = false;
	
	private SchedulerRunner langExtScheduler = null;
	
	
	public SchedulerRunner getLangExtScheduler() throws Throwable {
		if(langExtScheduler == null) {
			langExtScheduler = new SchedulerRunner(this.getLangExtClassLoader(), "langExt");
		}
		
		return langExtScheduler;
	}
	
	public MainLoop(String rootDir, FileLoader loader, boolean verboseErrors, boolean verboseInfo, FileWriter filewriter, boolean printBytecode /*, boolean debugMode*/)
	{
		this.rootDir = rootDir;
		//DEBUG_MODE = debugMode;
		VERBOSE_ERRORS = verboseErrors;
		VERBOSE_INFO = verboseInfo;
		this.printBytecode = printBytecode;
		
		mainClassLoader = Thread.currentThread().getContextClassLoader();
		
		
		this.loader = loader;
		this.filewriter = filewriter;
	}
	
	public void setWarnsAsErrors(boolean warnsAsErrors) {
		this.warnsAsErrors  = warnsAsErrors;
	}
	
	public void setCustomClassPath(URL[] classloaderOverride) {
		assert this.langExtClassLoader == null;
		mainClassLoader = URLClassLoader.newInstance(classloaderOverride, mainClassLoader);
	}
	
	public void setOutputDirectoryOverride(String outputDirOverride) {
		this.outputDirOverride  = outputDirOverride;
	}
	
	
	
	public static class SharedConcClassLoader extends ConcurnasClassLoader{
		public SharedConcClassLoader(){
			super(ClassPathUtils.getSystemClassPathAsPaths(), ClassPathUtils.getInstallationPath());
		}

		
	}
	
	private static SharedConcClassLoader sharedConcClassLoader = new SharedConcClassLoader();//one of these across all MainLoopInstances
	
	public ConcurnasClassLoader getLangExtClassLoader() {//assumes setCustomClassPath has been called etc
		if(this.langExtClassLoader == null) {
			/*
			 * Path[] cpele = null; if(mainClassLoader instanceof URLClassLoader) {
			 * URLClassLoader asur = (URLClassLoader)mainClassLoader; URL[] urls =
			 * asur.getURLs(); cpele = new Path[urls.length]; for(int n=0; n < urls.length;
			 * n++) { try { cpele[n] = Paths.get(urls[n].toURI());//new
			 * File(urls[n].toURI()).getAbsolutePath(); } catch (URISyntaxException e) {
			 * cpele[n] = Paths.get("."); } } }
			 */
				
				
			this.langExtClassLoader = new CCLoaderIncAddedNow(new ConcurnasClassLoader(ClassPathUtils.getSystemClassPathAsPaths(), ClassPathUtils.getInstallationPath(), sharedConcClassLoader));
		}
		//todo: hook into filewriter
		return this.langExtClassLoader;
	}
	
	
	/*
	 * ConcurnasClassLoader which supports classloading of classes defined during this compilation cycle
	 */
	public class CCLoaderIncAddedNow extends ConcurnasClassLoader {
		private ConcurnasClassLoader parent;

		public CCLoaderIncAddedNow(ConcurnasClassLoader parent){
			this.parent=parent;
		}
				
		public byte[] getBytecode(String name){
			byte[] got = parent.getBytecode(name);
			if(got == null) {
				name = name.replace('.', '/');
				if(null != filewriter && filewriter.hasWrittenClass(name)) {
					return filewriter.getWrittenClass(name);
				}
			}
			return got;
		}
		
		public Class<?> loadClass(String name) throws ClassNotFoundException  {
			Class<?> ret = parent.loadClass(name);
			if(ret == null) {
				String sname = name.replace('.', '/');
				if(null != filewriter && filewriter.hasWrittenClass(sname)) {
					return super.defineClass(name, filewriter.getWrittenClass(sname));
				}
			}
			
			return ret;
		}
	}
	
	
	public static class LangExtSchedulClsAndObj{
		public Class<?> schedulerCls;
		public Object shceduler;

		public LangExtSchedulClsAndObj(Class<?> schedulerCls, Object shceduler){
			this.schedulerCls=schedulerCls;
			this.shceduler=shceduler;
		}
		
	}
	
	
	private final HashSet<ErrorHolder> errors = new HashSet<ErrorHolder>();//set because many pahs can resolve to a modulecompiler
	private final HashSet<ErrorHolder> warnings = new HashSet<ErrorHolder>();//set because many pahs can resolve to a modulecompiler
	private final Map<ModuleCompiler, HashSet<ModuleCompiler>> dependanciesOfModule = new HashMap<ModuleCompiler, HashSet<ModuleCompiler>>();
	public final Map<String, ModuleCompiler> fullPathToModuleCompiler = new OrderedHashMap<String, ModuleCompiler>(); //can be many to one...
	private final Map<String, ArrayList<Pair<ITEM_TYPE, Object>>> precompiledBytecodeExistsCache = new  HashMap<String, ArrayList<Pair<ITEM_TYPE, Object>>>();
	private final Map<String, ModuleCompiler> fileRefToModuleCompiler = new HashMap<String, ModuleCompiler>();
	
	private Block rootBlock;
	public Block getRootBlock()
	{//some tests use this
		return this.rootBlock;
	}

	public CompilationMessages compileFile(String srcCanonicalRef){
		ArrayList<SourceCodeInstance> items = new ArrayList<SourceCodeInstance>();
		items.add(makeSourceCodeInstance(this.rootDir, srcCanonicalRef));
		return compileFile(items);
	}
	
	public CompilationMessages compileFiles(ArrayList<String> srcCanonicalRefs){
		ArrayList<SourceCodeInstance> items = new ArrayList<SourceCodeInstance>();
		
		for(String srcCanonicalRef : srcCanonicalRefs) {
			items.add(makeSourceCodeInstance(this.rootDir, srcCanonicalRef));	
		}
		
		return compileFile(items);
	}

	public static SourceCodeInstance makeSourceCodeInstance(String rootdir, String compilePath) {
		String fullFName = rootdir + File.separator + compilePath;
		
		String packagename;
		int lastSlash = compilePath.lastIndexOf(File.separator);
		if(lastSlash > -1) {
			String[] items = (compilePath.substring(0, lastSlash)).split(File.separator + File.separator);
			packagename = String.join(".", items);
		}else {
			packagename = "";
		}
		
		return new SourceCodeInstance(fullFName, packagename, true);
	}
	
	
	public static class SourceCodeInstance{
		public boolean mustCompile;
		public String packageName;
		public String path;
		
		public SourceCodeInstance(String path, String packageName, boolean mustCompile) {
			this.path = path;
			this.packageName = packageName;
			this.mustCompile = mustCompile;
		}
		
		@Override 
		public String toString() {
			return String.format("%s[%s %s]", path, packageName, mustCompile);
		}
		
		public String getFullPathAndClassName(String separator) {
			String packageAndClassName = this.path.substring(0, this.path.length()-5);
			
			int idxLastSlash = packageAndClassName.lastIndexOf(separator);
			if(idxLastSlash > -1) {
				String[] namebits = packageAndClassName.split(Pattern.quote(separator));
				packageAndClassName = namebits[namebits.length-1];//just the file name minus the .conc
				if(!this.packageName.isEmpty()) {
					packageAndClassName = this.packageName + "." + packageAndClassName;
				}
			}
			packageAndClassName = packageAndClassName.replace(separator, ".");
			if(packageAndClassName.startsWith(".")) {
				packageAndClassName = packageAndClassName.substring(1);
			}
			
			return packageAndClassName;
		}
		
		@Override
		public int hashCode() {
			return this.path.hashCode();
		}
		
		@Override
		public boolean equals(Object an) {
			if(an instanceof SourceCodeInstance) {
				return ((SourceCodeInstance)an).path.equals(this.path);
			}
			return false;
		}
		
	}
	
	//private HashSet<String> itemsBeingCompiledRightNow = new HashSet<String>();
	
	private StringTrie<ModuleCompiler> pathsToModules = new StringTrie<ModuleCompiler>("\\.");
	
	private ArrayList<SourceCodeInstance> filterSrcInstances(ArrayList<SourceCodeInstance> srcInstances){
		//remove dupes and choose instance which must be compiled if there is a choice between them
		LinkedHashSet<SourceCodeInstance> filtered = new LinkedHashSet<SourceCodeInstance>();
		for(SourceCodeInstance srci : srcInstances) {
			if(filtered.contains(srci)) {
				if(srci.mustCompile) {
					filtered.remove(srci);
					filtered.add(srci);
				}
			}else {
				filtered.add(srci);
			}
		}
		return new ArrayList<SourceCodeInstance>(filtered);
	}
	
	public CompilationMessages compileFile(ArrayList<SourceCodeInstance> srcInstances)
	{//srcCanonicalRef  - pack/subpack/File.conc
		//TODO: make this take multiple file inputs and also star etc. e.g. a\b\c\*.conc

		srcInstances = filterSrcInstances(srcInstances);
		
		String separator =  loader.getSeparator();
		//srcInstances.forEach(srcInstace -> itemsBeingCompiledRightNow.add(srcInstace.getFullPathAndClassName(separator)));

		ArrayList<ErrorHolder> ret = new ArrayList<ErrorHolder>();
		
		HashMap<SourceCodeInstance, ModuleCompiler> srcToMod = new HashMap<SourceCodeInstance, ModuleCompiler>();
		HashMap<ModuleCompiler, SourceCodeInstance> modToSrc = new HashMap<ModuleCompiler, SourceCodeInstance>();
		HashSet<String> pathAlreadyEncountered = new HashSet<String>();
		for(SourceCodeInstance srcInstace  : srcInstances){//prepare
			try {
				if(pathAlreadyEncountered.contains(srcInstace.path)) {
					continue;
				}else {
					pathAlreadyEncountered.add(srcInstace.path);
				}
				
				if(loader.fileExists(srcInstace.path)){
					String packageAndClassName = srcInstace.getFullPathAndClassName(separator);
					
					String modRootDir = srcInstace.path.substring(0, srcInstace.path.lastIndexOf(packageAndClassName.replace(".", separator))-1);
					ModuleCompiler modCompiler = getModuleCompilerForFile(modRootDir, packageAndClassName);//new ModuleCompiler(this.rootDir, fullPackageName, this, this.loader, this.VERBOSE_ERRORS, filewriter, this.DEBUG_MODE);
					
					if(srcInstace.mustCompile) {//remove previous compiled versions if any
						//String rootSourceFile = modCompiler.getSrcFile();
						String rootOutput = modCompiler.getClassFile();
						//TODO: if already compiled, then skip compilation totally
						if(loader.fileExists(rootOutput)) {
							this.filewriter.removeClassAndAllNestedInstances(rootOutput);
						}
					}
					
					srcToMod.put(srcInstace, modCompiler);
					modToSrc.put(modCompiler, srcInstace);

					fullPathToModuleCompiler.put(packageAndClassName, modCompiler);
					fileRefToModuleCompiler.put(packageAndClassName, modCompiler);
					pathsToModules.add(packageAndClassName, modCompiler);
					
				}else {
					String er = String.format("Unable to compile as file %s not found", srcInstace.path);
					ret.add(new ErrorHolder(loader.getCanonicalPath(srcInstace.path), 0,0, er));
				}
			}
			catch(Exception e)
			{
				String er = "Unable to compile as " + e.getMessage();
				this.consoleOut.println(er);
				if(this.VERBOSE_ERRORS){
					e.printStackTrace(this.consoleOut);
				}
				
				ret.add(new ErrorHolder(""+srcInstace, 0,0, er));
			}
		}
		
		for(SourceCodeInstance srcInstace  : srcInstances){//duplicates...
			try{
				if(!srcInstace.mustCompile) {
					continue;
				}
				
				ModuleCompiler modCompiler = srcToMod.get(srcInstace);
				
				if(modCompiler.finishedCompiling()){//if dependancy of something we already compiled, and finnished, then nothing to do
					continue;
				}
				
				ArrayList<ModuleCompiler> pendingCompilationRound = new ArrayList<ModuleCompiler>();//TODO: replace with less setupid ds
				pendingCompilationRound.add(modCompiler);
				
				HashSet<ModuleCompiler> alreadyHadAGoThisTime;//sometimes we have circular dependnacies
				//this cleans up that loop by doing one compilation per iteration.
				
				boolean carryoncompile = true;
				
				while(carryoncompile)
				{
					alreadyHadAGoThisTime = new HashSet<ModuleCompiler>();
					
					HashSet<String> existingStuffLoaded = new HashSet<String>(fullPathToModuleCompiler.keySet());
					
					while(!pendingCompilationRound.isEmpty())
					{
						ModuleCompiler toCompile = pendingCompilationRound.remove(0);
						
						if(!toCompile.finishedCompiling())
						{
							toCompile.init();
							
							alreadyHadAGoThisTime.add(toCompile);
							//System.out.println("Try to progress compilation of: " + toCompile);
							toCompile.tryToProgressCompilation();
							//System.out.println("did: " + toCompile);
							boolean progressmade = toCompile.wasProgressMade();
							//toCompile.tryToProgressCompilation();
							//if(!toCompile.finishedCompiling())
							//goodie so all the stuff that depends on it may well have suceeded!
							if(dependanciesOfModule.containsKey(toCompile))
							{
								for(ModuleCompiler dependorToRecompile : dependanciesOfModule.get(toCompile))
								{
									if(!dependorToRecompile.finishedCompiling())
									{
										if(!pendingCompilationRound.contains(dependorToRecompile)) 
										{
											if(progressmade || !alreadyHadAGoThisTime.contains(dependorToRecompile))
											{
												pendingCompilationRound.add(dependorToRecompile);  
											}
										}
									}
								}
							}
							//and have another go with the dependor as well cos the previous thing may have fixed everything...
							//pendingCompilationRound.add(toCompile);
						}/*else {
							System.err.println("= finished: " + toCompile);
						}*/
					}
					
					//we've compiled all the direct stuff and dependancies affected of above, now try
					//to compile any new imports etc which may have come from the above.
					HashSet<String> keys = new HashSet<String>(fullPathToModuleCompiler.keySet());
					for(String newlyLoadedDependancy : keys)
					{
						if(!existingStuffLoaded.contains(newlyLoadedDependancy))
						{
							ModuleCompiler newUnit = fullPathToModuleCompiler.get(newlyLoadedDependancy);
							//if(!toCompile.finishedCompiling())
							if(!pendingCompilationRound.contains(newUnit)) { 
								newUnit.tryToProgressCompilation();
								pendingCompilationRound.add(newUnit);  
							}
						}
					}
					
					//looking potentially grim now, see if there is antyhing left to compiel overall...
					
					if(pendingCompilationRound.isEmpty()) 
					{//dependancies got us nowhere, maybe the whole thing has finnished compiling?
						boolean allDone = true;
						HashSet<ModuleCompiler> modcomp = new HashSet<ModuleCompiler>(fullPathToModuleCompiler.values());
						for(ModuleCompiler mc : modcomp)
						{
							if(!mc.finishedCompiling())
							{
								allDone=false;
								if(mc.wasProgressMade())
								{
									if(!pendingCompilationRound.contains(mc)) 
									{
										boolean skip = false;//if marked for optional compilation, then only do so if it has any dependancies
										if(!modToSrc.get(mc).mustCompile) {
											if(!this.dependanciesOfModule.containsKey(mc)){
												skip = true;
											}
										}
										if(!skip) {
											pendingCompilationRound.add(mc);
										}
										
										//alreadyHadAGoThisTime = new HashSet<ModuleCompiler>();
									}
								}
							}
						}
						
						if(allDone || pendingCompilationRound.isEmpty())
						{//if finnished, or nothing got progressed in this round...
							carryoncompile = false;
						}
						else
						{//not finnished
							if(pendingCompilationRound.isEmpty())
							{//but no progress so give up
								carryoncompile = false;
							}
						}
					}
				}
				
				for(ModuleCompiler mc : fullPathToModuleCompiler.values()){
					//ModuleCompiler mc = fullPathToModuleCompiler.get(k);
					errors.addAll(mc.getLastErrorSet());
					warnings.addAll(mc.warnings);
				}
				
				ArrayList<ErrorHolder> warningsAndErrors = new ArrayList<ErrorHolder>();
				
				if(!warnings.isEmpty()){
					warningsAndErrors.addAll(warnings);
					Collections.sort(warningsAndErrors);
				}
				
				if(!errors.isEmpty())
				{
					warningsAndErrors.addAll(errors);
					Collections.sort(warningsAndErrors);
					
					/*if(this.VERBOSE_ERRORS)
					{
						StringBuilder sb = new StringBuilder("Failed compilation of:\n");
						ArrayList<ModuleCompiler> items = new ArrayList<ModuleCompiler>(new HashSet<ModuleCompiler>(fullPathToModuleCompiler.values()));
						Collections.sort(items);
								
						for(ModuleCompiler mod : items)
						{
							//sb.append(String.format("%s cycle attempts: %s\n", mod, mod.getAttemptsAtCompilation() ));
							sb.append(""+mod);
						}
						consoleOut.print(sb.toString());
					}*/
					
				}
				else
				{//OMG we did it!
					this.rootBlock = modCompiler.getRootBlock();
				}
				ret.addAll(warningsAndErrors);
				//return new CompilationMessages(!errors.isEmpty(), warningsAndErrors);
			}
			catch(Exception e)
			{
				String er = "Unable to compile as " + e.getMessage();
				this.consoleOut.println(er);
				if(this.VERBOSE_ERRORS){
					e.printStackTrace(this.consoleOut);
				}
				
				//ArrayList<ErrorHolder> ret = new ArrayList<ErrorHolder>();
				ret.add(new ErrorHolder(""+srcInstace, 0,0, er));
				//return new CompilationMessages(true, ret);
			}
		}
		

		return new CompilationMessages(!ret.isEmpty(), ret);
		
	}
	
	public static class CompilationMessages{
		public final boolean hasErrors;
		public final ArrayList<ErrorHolder> messages;
		
		
		public CompilationMessages(boolean hasErrors, ArrayList<ErrorHolder> messages){
			this.hasErrors = hasErrors;
			this.messages = messages;
		}
	}
	
	public boolean doesImportedNameResolveToSomething(String fullPathName, ModuleCompiler dependor)
	{
		return doesImportedNameResolveToSomething(fullPathName, dependor, false);
	}
	
	private void addDependancy(ModuleCompiler master, ModuleCompiler dependor)
	{
		HashSet<ModuleCompiler> deps = this.dependanciesOfModule.get(master);
		if(null == deps) { deps = new HashSet<ModuleCompiler>(); dependanciesOfModule.put(master,  deps);}
		deps.add(dependor);
	}
	
	public ModuleCompiler getModuleCompiler(String fullPathName) {
		return fullPathToModuleCompiler.get(fullPathName);
	}
	
	public boolean doesImportedNameResolveToSomething(String fullPathName, ModuleCompiler dependor, boolean isChild)
	{
		try
		{
			/*if(itemsBeingCompiledRightNow.contains(fullPathName)) {
				return true;
			}*/
			
			if( precompiledBytecodeExistsCache.containsKey(fullPathName) )
			{
				return true;
			}
			else if(fullPathToModuleCompiler.containsKey(fullPathName))
			{
				ModuleCompiler mc = fullPathToModuleCompiler.get(fullPathName);
				addDependancy(mc, dependor) ;
				String packandclassname = mc.getpackageAndClassName();
				int upto = packandclassname.length()+1;
				if(upto > fullPathName.length()){
					return false;
				}
				
				String rest = fullPathName.substring(upto);
				return mc.hasAnyResource(rest, isChild);
			}
			else
			{
				//find via bootstrap OR...
				ArrayList<Pair<ITEM_TYPE, Object>> resolved = reslolvePathToBytecode(fullPathName, isChild, false);
				if(resolved != null && !resolved.isEmpty()) {
					precompiledBytecodeExistsCache.put(fullPathName, resolved);
					return true;
				} else {//see if the thing exists in the compile path
					ModuleCompiler comAssociatedWithFile = pathsToModules.get(fullPathName);

					if(null != comAssociatedWithFile) {
						fullPathToModuleCompiler.put(fullPathName, comAssociatedWithFile);
						addDependancy(comAssociatedWithFile, dependor) ;
						
						String rest  = fullPathName.substring(comAssociatedWithFile.packageAndClassName.length()+1);
						//extract last part
						
						return comAssociatedWithFile.hasAnyResource(rest, isChild);
					}
				}
			}
		}
		catch(Exception e)
		{
			if(fullPathName != null) {
				this.errors.add(new ErrorHolder(fullPathName, 0,0, "Unexpected compilation error: " + e.getMessage()) );
			}
		}
		
		return false;
	}
	
	private HashMap<String, ModuleCompiler> modCForFile = new HashMap<String, ModuleCompiler>();
	

	public ArrayList<Profiler> profilers = null;
	public void setProfile(boolean profile) {
		profilers = new ArrayList<Profiler>();
	}
	
	
	private ModuleCompiler getModuleCompilerForFile(String rootDir, String packageAndClassName) throws Exception{
		ModuleCompiler ret; 
		if(!modCForFile.containsKey(packageAndClassName)){
			ret  = new ModuleCompiler(null, rootDir, packageAndClassName, this, this.loader, this.VERBOSE_ERRORS, this.VERBOSE_INFO, filewriter, this.outputDirOverride, consoleOut, this.printBytecode);
			
			if(null != profilers) {
				Profiler pp = new Profiler(packageAndClassName);
				profilers.add(pp);
				ret.enableProfiling(pp);
			}
			
			if(this.warnsAsErrors) {
				ret.setWarnAsErrors(true);
			}
			
			modCForFile.put(packageAndClassName, ret);
		}else{
			ret = modCForFile.get(packageAndClassName);
		}
		return ret;
	}
	
	private Pair<Boolean, Class<?>> drillasDeepUcan(Class<?> classSoFar, String dotRemaining, boolean isChild)
	{
		boolean nestedCls = false;
		
		for(String dotbit : dotRemaining.split("\\."))
		{
			//drill into nested classes...
			//drilling can be either $ or .
			Class<?>[] kids = classSoFar.getClasses();
			for(Class<?> kid : kids)
			{//find matching kid to drill into, take note if A.B.C() is not possible - isnested=true
				int mod = kid.getModifiers();
				if(dotbit.equals(kid.getSimpleName()) && !Modifier.isPrivate(mod)   )
				{
					if( Modifier.isPublic(mod) || (isChild &&  Modifier.isProtected(mod)  )  )
					{
						if(!Modifier.isStatic(mod))
						{
							nestedCls = true;
						}
						classSoFar = kid;
					}
				}
			}
		}
		
		return new Pair<Boolean, Class<?>>(nestedCls, classSoFar);
		
	}
	
	public ArrayList<Pair<ITEM_TYPE, Object>> reslolvePathToBytecode(String fullPathNamea, boolean isChild, boolean useLangExtLoader)
	{
		//find the class start from end and work back
		ArrayList<Pair<ITEM_TYPE, Object>> ret= new ArrayList<Pair<ITEM_TYPE, Object>>();
		
		String tryClassName = fullPathNamea;
		String dotRemaining = "";
		
		while(tryClassName != null && !tryClassName.equals(""))
		{
			try
			{
				Class<?> cls = (useLangExtLoader?this.getLangExtClassLoader():mainClassLoader).loadClass(tryClassName);
				if(null == cls) {
					throw new NoClassDefFoundError();
				}
				
				if(dotRemaining.equals(""))
				{//resolves to just a pure class...
					ret.add(new Pair<ITEM_TYPE, Object>(ITEM_TYPE.STATIC_CLASS, new ClassDefJava(cls)));
				}
				else
				{
					//query downwards along the dotRemaining [Left to Right]
					//$ means pure nested-class, . means static
					Pair<Boolean, Class<?>> nestedAndSoFar = drillasDeepUcan(cls, dotRemaining, isChild);
					boolean nestedCls = nestedAndSoFar.getA();
					cls = nestedAndSoFar.getB();
					
					int start = cls.getName().length();
					
					if(start <= fullPathNamea.length())
					{
						String remainnig = fullPathNamea.substring(start);
						if(remainnig.equals(""))
						{
							ret.add(new Pair<ITEM_TYPE, Object>(nestedCls ? ITEM_TYPE.NESTED_CLASS  : ITEM_TYPE.STATIC_CLASS, new ClassDefJava(cls)));
						}
						else if(0 == remainnig.indexOf('.') )
						{//should be no further dots because only one element
							//i.e. should now be just one bit left to drill into... a. [b.c] - cannot resolve because b must be a class.... but a.b.[c] is ok cos must be var or func
							//var or function...
							remainnig = remainnig.substring(1, remainnig.length());
							Object var = CompiledClassUtils.getResourceFromClass(cls, ITEM_TYPE.VARIABLE, remainnig, isChild, true /*can only be static stuff*/);
							if(var != null)
							{
								ret.add((new Pair<ITEM_TYPE, Object>(ITEM_TYPE.VARIABLE, ((HashSet<?>)var).iterator().next() )));
							}
							
							Object funcs = CompiledClassUtils.getResourceFromClass(cls, ITEM_TYPE.FUNC, remainnig, isChild, true /*can only be static stuff*/);
							if(funcs != null)
							{
								HashSet<TypeAndLocation> hs = (HashSet<TypeAndLocation>)funcs;
								if(!hs.isEmpty()){
									ret.add((new Pair<ITEM_TYPE, Object>(ITEM_TYPE.FUNC, funcs)));
								}
								
							}
							
							Object typdefs = CompiledClassUtils.getResourceFromClass(cls, ITEM_TYPE.TYPEDEF, remainnig, isChild, true /*can only be static stuff*/);
							if(funcs != null)
							{
								ret.add((new Pair<ITEM_TYPE, Object>(ITEM_TYPE.TYPEDEF, typdefs)));
							}
							
						}
					}
				}
				
				return ret;
			}
			catch(NoClassDefFoundError | ClassNotFoundException e)
			{
				int lastDot = tryClassName.lastIndexOf('.');
				if(-1 == lastDot)
				{
					break;
				}
				dotRemaining += "." + tryClassName.substring(lastDot+1);
				tryClassName = tryClassName.substring(0, lastDot);
			}
		}
		
		return ret;
	}
	
	
	private Object generalGet(ITEM_TYPE toGet, ModuleCompiler dependor, String fullPathNamea, boolean isChild)
	{
		if(doesImportedNameResolveToSomething(fullPathNamea, dependor, isChild))
		{
			if(precompiledBytecodeExistsCache.containsKey(fullPathNamea))
			{//could be map not list....
				 ArrayList<Pair<ITEM_TYPE, Object>> itms = precompiledBytecodeExistsCache.get(fullPathNamea);
				for(Pair<ITEM_TYPE, Object> res : itms )
				{
					if(res.getA() == toGet)
					{
						Object got =res.getB(); 
						
						if(got instanceof ClassDefJava){
							((ClassDefJava)got).javaSystemLib = true;
						}
						
						return res.getB();
					}
				}
			}
			else if(fullPathToModuleCompiler.containsKey(fullPathNamea))
			{
				ModuleCompiler mc = fullPathToModuleCompiler.get(fullPathNamea);
				
				String alreadyName = mc.getpackageAndClassName();
				
				if(alreadyName.length() > fullPathNamea.length())
				{
					errors.add(new ErrorHolder("", 0, 0, "Unknown compiler error when resolving imported name: " + fullPathNamea ));
				}
				else if (alreadyName.length() == fullPathNamea.length())
				{
					//class!
					if(toGet == ITEM_TYPE.NESTED_CLASS || toGet == ITEM_TYPE.STATIC_CLASS )
					{
						return mc.getTopLevelClassDef();
					}//better be a class or it all fails
				}
				else
				{
					String rest = fullPathNamea.substring(alreadyName.length() + 1);
					return mc.getResource(toGet, rest, isChild);
				}
				
			}
			else
			{
				errors.add(new ErrorHolder("",0,0,"Unknown compiler error when resolving imported: " + fullPathNamea ));
				return null;
			}
		}
		return null;
	}
	
	//these methods for the import thingy as par above as well
	public TypeAndLocation getVariableFromPath(String name, ModuleCompiler mc, boolean isChild) {
		Object got =  generalGet(ITEM_TYPE.VARIABLE, mc, name, isChild );
		
		if(got != null )
		{
			if(got instanceof Pair<?,?>){
				got = ((Pair<?,?>)got).getA();
			}
			
			if(got instanceof TypeAndLocation){
				return (TypeAndLocation)got;
			}
		}
		return null;
	}
	
	public TypeDefTypeProvider getTypeProviderFromPath(String name, ModuleCompiler mc, boolean isChild) {
		Object obj = generalGet(ITEM_TYPE.TYPEDEF, mc, name, isChild );
		
		return obj instanceof TypeDefTypeProvider?(TypeDefTypeProvider)obj:null;
	}
	
	@SuppressWarnings("unchecked")
	public HashSet<TypeAndLocation> getFunctionFromPath(String name, ModuleCompiler mc, boolean isChild) {
		//TODO: check me, i never get called ever!
		Object gotVar =  generalGet(ITEM_TYPE.VARIABLE, mc, name, isChild );
		if(gotVar != null && gotVar instanceof FuncType)
		{
			HashSet<TypeAndLocation> ret = new HashSet<TypeAndLocation>();
			ret.add(new TypeAndLocation((FuncType)gotVar, new DummyFuncLocation(null)));
			return ret;
		}
		else
		{
			Object got =  generalGet(ITEM_TYPE.FUNC, mc, name, isChild );
			
			if(got != null )
			{
				if(got instanceof Pair<?,?>){
					got = ((Pair<?,?>)got).getA();
				}
				
				if(got instanceof Set){
					HashSet<TypeAndLocation> ret = new HashSet<TypeAndLocation>();
					for(TypeAndLocation item : (Set<TypeAndLocation>)got ) {
						ret.add(item);
					}
					return ret;
				}
			}
		}
		
		return null;
	}

	public ClassDef getClassDefFromPath(String name, ModuleCompiler mc, boolean isChild) {
		
		Object nc =  generalGet(ITEM_TYPE.NESTED_CLASS, mc, name, isChild );
		if(nc != null && nc instanceof NamedType)
		{
			return (ClassDef)nc;
		}
		
		Object sc =  generalGet(ITEM_TYPE.STATIC_CLASS, mc, name, isChild );
		if(sc != null && sc instanceof ClassDef)
		{
			return (ClassDef)sc;
		}
		
		return null;
	}

	private PrintStream consoleOut = System.out;
	public void setConsoleOutput(PrintStream consoleOut) {
		this.consoleOut = consoleOut;
	}

	public void stop() {
		if(langExtScheduler != null) {
			langExtScheduler.stop();
		}
	}
}
