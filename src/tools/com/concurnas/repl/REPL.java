package com.concurnas.repl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.RecognitionException;
import org.objectweb.asm.Opcodes;

import com.concurnas.compiler.ASTCreator;
import com.concurnas.compiler.ConcurnasParser;
import com.concurnas.compiler.DirectFileLoader;
import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.FileLoader;
import com.concurnas.compiler.LexParseErrorCapturer;
import com.concurnas.compiler.MainLoop;
import com.concurnas.compiler.ModuleCompiler;
import com.concurnas.compiler.SchedulerRunner;
import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.DuffAssign;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.Line;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.REPLDepGraphComponent;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.bytecode.BytecodeOutputter;
import com.concurnas.compiler.utils.BytecodePrettyPrinter;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.visitors.REPLDepGraph.REPLComponentWrapper;
import com.concurnas.compiler.visitors.Utils;
import com.concurnas.runtime.ClassPathUtils;
import com.concurnas.runtime.ConcurnasClassLoader;
import com.concurnas.runtime.MockFileWriter;
import com.concurnas.runtime.Pair;


//TODO: may call:
/*if(meth == null) {
		mv.visitMethodInsn(INVOKESTATIC, classBeingTested, BytecodeGennerator.metaMethodName, "()Ljava/lang/String;", false);
		mv.visitInsn(POP);
	}
	
instead of special method
			
*/

public class REPL implements Opcodes {
	
	private MainLoop mainLoop;
	//private TheScopeFrame theScopeFrame;
	private ModuleCompiler moduleCompiler;
	private ClassLoader mainClassLoader = Thread.currentThread().getContextClassLoader();
	private REPLState replState;
	private static final String mastSrcName = "repl$";
	private boolean warnAsError = false;
	private boolean printBytecode = false;
	private boolean debugmode = false;
	
	public REPL(boolean warnAsError, boolean printBytecode, boolean debugmode) throws Exception {
		this.mainLoop = new MainLoop(".", new DirectFileLoader(), true, false, null, false);
		boolean VERBOSE_ERRORS=true;
		boolean VERBOSE_OUTPUT=false;
		FileLoader fl =new DirectFileLoader();
		this.replState = new REPLState();
		this.moduleCompiler = new ModuleCompiler(replState, "", mastSrcName, this.mainLoop, fl, VERBOSE_ERRORS, VERBOSE_OUTPUT, null, null, System.out, false);
		this.warnAsError = false;
		this.printBytecode = printBytecode || this.debugmode;
		this.debugmode = debugmode;
	}
	
	
	public void replLoop() {
		//\exit, help, ctrl-d
		//up
		
		System.out.println("conc>");
		
		System.out.println(processInput("a = 3"));
		System.out.println(processInput("a += 3"));
		System.out.println(processInput("a += 3"));
		System.out.println(processInput("a "));
		System.out.println(processInput("10**2 "));
		System.out.println(processInput("5 if a > 5 else 2"));
		//bug above for you...
		
	}
	
	
	private static AtomicLong iteration = new AtomicLong();
	
	private static class ParseState{
		public Block block;
		public ArrayList<ErrorHolder> ers;
		public String srcName;

		public ParseState(Block block, ArrayList<ErrorHolder> ers, String srcName) {
			this.block = block;
			this.ers = ers;
			this.srcName = srcName;
		}
	}
	
	public ParseState parseInput(String text) {
		try{
			String name = "repl$" + iteration.getAndIncrement();
			LexParseErrorCapturer errors = new LexParseErrorCapturer(name);
			
			ConcurnasParser op = Utils.miniParse(text, name, -1, 0, errors);
			Block ret = (Block)new ASTCreator(name, errors).visit(op.code());

			if(!errors.errors.isEmpty()){
				return new ParseState(null, errors.errors, null);
			}

			ret.canContainAReturnStmt=true;
			ret.isModuleLevel = true;
			
			return new ParseState(ret, null, name);
		}
		catch(RecognitionException re){
			String err ="Cannot parse input" + re.getMessage();
			ErrorHolder rh = new ErrorHolder("",0,0,err, null, null);
			 ArrayList<ErrorHolder> ers = new  ArrayList<ErrorHolder>();
			 ers.add(rh);
			
			return new ParseState(null, ers, null);
		}
	}
	
	private String formatErrors(Collection<ErrorHolder> ers, String prefix) {
		//StringBuilder sb = new StringBuilder();
		//ers.forEach(a -> sb.append(String.format("%s:%s %s", a.getLine(), a.getColumn(), a.getMessage())));
		//return sb.toString();
		return String.join("\n", ers.stream().map(a -> String.format("%s %s:%s %s", prefix, a.getLine(), a.getColumn(), a.getMessage())).sorted().collect(Collectors.toList()));
	}
	
	public void terminate() {
		//stop main loop if spawned any threads
		this.mainLoop.stop();
		//System.exit(0);
	}
	
	public List<String> tabComplete(String input) {
		//vars, keywords, dot thing
		
		return null;
	}
	

	public static class REPLExecutor extends ConcurnasClassLoader {
		public REPLExecutor(Path[] cpele ){
			super(cpele, ClassPathUtils.getInstallationPath(), sharedConcClassLoader);
		}
		
		public HashMap<String, byte[]> nameToCode = new HashMap<String, byte[]>();

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
	}
	
	
	
	public static class SharedConcClassLoader extends ConcurnasClassLoader{
		public SharedConcClassLoader(){
			super(ClassPathUtils.getSystemClassPathAsPaths(), ClassPathUtils.getInstallationPath());
		}
	}
	
	private static SharedConcClassLoader sharedConcClassLoader = new SharedConcClassLoader();//one of these across all MainLoopInstances
	
	
	public REPLExecutor getExecutor() {//assumes setCustomClassPath has been called etc
		/*
		 * Path[] cpele = null; if(mainClassLoader instanceof URLClassLoader) {
		 * URLClassLoader asur = (URLClassLoader)mainClassLoader; URL[] urls =
		 * asur.getURLs(); cpele = new Path[urls.length]; for(int n=0; n < urls.length;
		 * n++) { try { cpele[n] = Paths.get(urls[n].toURI());//new
		 * File(urls[n].toURI()).getAbsolutePath(); } catch (URISyntaxException e) {
		 * cpele[n] = Paths.get("."); } } }
		 */
		
		//return new REPLExecutor(new ConcurnasClassLoader(cpele, ClassPathUtils.getInstallationPath(), sharedConcClassLoader));
		return new REPLExecutor(ClassPathUtils.getSystemClassPathAsPaths());
	}
	
	public SchedulerRunner getScheduler(REPLExecutor replExe) throws Throwable {
		return new SchedulerRunner(replExe, "REPLExe");
	}
	
	
	LinkedHashMap<Pair<String, FuncType>, FuncDef> persistedFunctionSet = new LinkedHashMap<Pair<String, FuncType>, FuncDef>();
	
	private Pair<List<Thruple<FuncDef, FuncType, Boolean>>, List<REPLComponentWrapper>> processBlockFuncs(Block blk) {
		List<Thruple<FuncDef, FuncType, Boolean>> newlyDefined = new ArrayList<Thruple<FuncDef, FuncType, Boolean>>();
		List<REPLComponentWrapper> newTLEs = new ArrayList<REPLComponentWrapper>();
		
		LinkedHashMap<REPLComponentWrapper, FuncType> funcs = new LinkedHashMap<REPLComponentWrapper, FuncType>();
		//funcs for later iterations...
		for(LineHolder lh : blk.lines) {
			Line lin = lh.l;
			REPLComponentWrapper wrap = null;
			if(lin instanceof FuncDef) {
				wrap = new REPLComponentWrapper((REPLDepGraphComponent)lin);
				FuncDef origFD = (FuncDef)lin;
				funcs.put(wrap, origFD.getFuncType().getErasedFuncTypeNoRet());
			}
			
			if(lin instanceof REPLDepGraphComponent) {
				if(null == wrap) {
					wrap = new REPLComponentWrapper((REPLDepGraphComponent)lin);
				}
				
				newTLEs.add(wrap);
			}
		}
		
		for(REPLComponentWrapper replcom : funcs.keySet()) {
			FuncDef toAdd = (FuncDef)replcom.comp;
			FuncType ft = funcs.get(replcom);
			Pair<String, FuncType> defFT = new Pair<String, FuncType>(toAdd.funcName, ft);
			boolean isNew = true ;
			if(persistedFunctionSet.containsKey(defFT)) {
				//overwrite persisted version
				//this.moduleCompiler.moduleLevelFrame.removeFuncDef(defFT.getA(), defFT.getB(), true);
				persistedFunctionSet.remove(defFT);
				isNew=false;
			}
			
			newlyDefined.add(new Thruple<FuncDef, FuncType, Boolean>(toAdd, ft, isNew));
		}
		//now add funcs from prevous definitions unless redefined above....
		persistedFunctionSet.values().forEach(a -> blk.addPenultimate(new LineHolder(a)));
		
		funcs.keySet().forEach(a -> persistedFunctionSet.put( new Pair<String, FuncType>(((FuncDef)a.comp).funcName, funcs.get(a)), (FuncDef)a.comp));
		
		return new Pair<List<Thruple<FuncDef, FuncType, Boolean>>, List<REPLComponentWrapper>>(newlyDefined, newTLEs);
	}
	
	
	
	private void markAllRootNodesAsIterativeVisitorSkippable(Block blk) {
		for(LineHolder lh : blk.lines) {
			Line lin = lh.l;
			if(lin instanceof REPLDepGraphComponent) {
				((REPLDepGraphComponent)lin).setSkippable(true);
			}
		}
		
	}
	
	private static List<ErrorHolder> remoteSupressedErrors(HashSet<ErrorHolder> ers){
		ArrayList<ErrorHolder> ret = new ArrayList<ErrorHolder>();
		
		for(ErrorHolder er : ers) {
			FuncDef ctx = er.getContext();
			if(ctx != null) {
				if(ctx.supressErrors) {
					continue;//skip
				}
			}
			ret.add(er);
		}
		
		
		return ret;
	}
	
	private void removeTopLevelItemsFromUpdateSet(List<REPLComponentWrapper> toremove, HashSet<REPLComponentWrapper> depsUpdated) {
		for(REPLComponentWrapper item : toremove) {
			depsUpdated.remove(item);
		}
		
		HashSet<REPLComponentWrapper> toRem = new HashSet<REPLComponentWrapper>();
		for(REPLComponentWrapper wra : depsUpdated) {
			REPLDepGraphComponent item = wra.comp;
			if(item instanceof AssignExisting) {
				if(!((AssignExisting)item).isReallyNew) {
					toRem.add(wra);
				}
			}else if(item instanceof AssignNew) {
				AssignNew an = (AssignNew)item;
				if(an.expr instanceof Block) {
					Block asblock = (Block)an.expr;
					if(asblock.lines.size() == 1) {
						LineHolder lh = asblock.getLast();
						if(lh.l instanceof DuffAssign && ((DuffAssign)lh.l).e instanceof RefName) {
							RefName rn = (RefName) ((DuffAssign)lh.l).e;
							if(rn.name.equals(an.name)) {
								toRem.add(wra);
							}
						}
					}
				}
			}
		}
		
		depsUpdated.removeAll(toRem);
	}
	
	public String processInput(String input){
		if(input ==null || input.trim().equals("")) {
			return "";
		}
		
		ArrayList<String> output = new ArrayList<String>();
		try {
			if(this.debugmode) {
				BytecodeOutputter.PRINT_OPCODES=true;
			}
			ParseState obtained = parseInput(input);
			
			ArrayList<ErrorHolder> ers = obtained.ers;
			if(ers != null) {
				return formatErrors(ers, "ERROR");
			}
			else {
				MockFileWriter fw =new MockFileWriter();
				String srcName = obtained.srcName;
				
				
				Pair<List<Thruple<FuncDef, FuncType, Boolean>>, List<REPLComponentWrapper>> newStuiffAndTLEs = processBlockFuncs(obtained.block);
				
				List<Thruple<FuncDef, FuncType, Boolean>> newfuncs = newStuiffAndTLEs.getA();
				List<REPLComponentWrapper> newTopLevelItemsDeclared = newStuiffAndTLEs.getB();
				
				this.moduleCompiler.progressCompilationREPL(obtained.block, srcName, fw);
				HashSet<REPLComponentWrapper> depsUpdated = this.replState.replDepGraph.getAndResetThingsModified();
				
				List<ErrorHolder> warns = remoteSupressedErrors(this.moduleCompiler.warnings);
				if(warns != null && !warns.isEmpty()) {
					output.add(formatErrors(warns, "|  WARN"));
				}
								
				List<ErrorHolder> ersx = remoteSupressedErrors(this.moduleCompiler.getLastErrorSet());
				if(ersx != null && !ersx.isEmpty()) {
					//unless all errors are within functions
					output.add(formatErrors(ersx, "|  ERROR"));
					if(ersx.stream().noneMatch(a -> a.hasContext())){
						return String.join("\n", output.stream().map(a -> a.trim()).collect(Collectors.toList()) );
					}
				}
								
				
				REPLExecutor instanceExecutor = getExecutor();
				//start scheduler if not already
				SchedulerRunner sch = getScheduler(instanceExecutor);
				
				try {
					byte[] rawcode = fw.nametoCode.get(mastSrcName);
					byte[] codeRepointed = REPLCodeRepointStateHolder.repointToREPLStateHolder(rawcode, mastSrcName, srcName);
					
					if(printBytecode || debugmode) {
						StringBuilder pp = new StringBuilder();
						pp.append("|  Expression Bytecode:");
						pp.append(BytecodePrettyPrinter.print(rawcode, true, null, "|  "));
						output.add(pp.toString());
					}
					if(debugmode) {
						StringBuilder pp = new StringBuilder();
						pp.append("|  Expression Bytecode (post runtime adaptation):");
						pp.append(BytecodePrettyPrinter.print(codeRepointed, true, null, "|  "));
						output.add(pp.toString());
					}
					
					HashSet<String> newvars = this.moduleCompiler.replState.getNewVars();
					if(input.trim().endsWith(";")) {
						newvars = null;
					}
					
					String invokerclassName = srcName + "$EXE";
					byte[] executor =  new REPLTaskMaker(invokerclassName, srcName, newvars).gennerate();
					
					instanceExecutor.nameToCode.put(srcName, codeRepointed);
					instanceExecutor.nameToCode.put(invokerclassName, executor);
					
					//exe on scheduler
					Class<?> executorTasCls = instanceExecutor.loadClass(invokerclassName);
					Object exeTaObject = executorTasCls.newInstance();
					sch.invokeScheudlerTask(exeTaObject);	
					
					if(!newfuncs.isEmpty()) {
						output.add(processNewFuncsDefined(newfuncs));
					}
					
					removeTopLevelItemsFromUpdateSet(newTopLevelItemsDeclared, depsUpdated);
					
					if(!depsUpdated.isEmpty()) {
						//|    update modified method volume(double)
						List<String> reduced = depsUpdated.stream().map(a -> formatTopLevelElement(a)).filter(a -> !a.contains("$")).sorted().collect(Collectors.toList());
						if(!reduced.isEmpty()) {
							output.add("|    update modified " + String.join(", ", reduced));
						}
					}
					
					if(!input.trim().endsWith(";")) {
						if(newvars != null) {
							
							String got = (String)getMethod(executorTasCls, "getResult", 0).invoke(exeTaObject);
							
							if(got != null && !got.trim().isEmpty()) {
								if(!output.isEmpty()) {
									output.add("\n");
								}
								
								output.add(got);
							}
						}
					}
					
					return String.join("\n", output.stream().map(a -> a.trim()).collect(Collectors.toList()) );
					
				}catch(Throwable e){
					//TODO: format exception
					throw e;
				}finally {
					sch.stop();
				}
			}
		}catch(Throwable e) {
			//e.printStackTrace();
			StringWriter out = new StringWriter();
		    PrintWriter writer = new PrintWriter(out);
			e.printStackTrace(writer);
			output.add(out.toString());			

			return String.join("\n", output.stream().map(a -> a.trim()).collect(Collectors.toList()) );
		}finally{
			if(null != this.moduleCompiler.moduleLevelFrame) {
				this.replState.inc();
				this.replState.tliCache = this.moduleCompiler.replLastTopLevelImports;
				this.moduleCompiler.moduleLevelFrame.updatePrevSessionVars();
			}
			
			if(this.debugmode) {
				BytecodeOutputter.PRINT_OPCODES=false;
			}
		}
	}
	
	private String formatTopLevelElement(FuncDef funcDef) {
		StringBuilder sb = new StringBuilder();
		sb.append(funcDef.funcName);
		sb.append('(');
		
		ArrayList<Type> inputs = funcDef.getFuncType().getInputs();
		sb.append(String.join(", ", inputs.stream().map(a -> a.toString()).collect(Collectors.toList())));
		
		return sb.append(")").toString();
	}

	
	private String formatTopLevelElement(REPLComponentWrapper item) {
		return formatTopLevelElement(item.comp);
	}
	
	private String formatTopLevelElement(REPLDepGraphComponent item) {
		if(item instanceof FuncDef) {
			return formatTopLevelElement((FuncDef)item);
		}else {
			return item.getName();
		}
	}

	private String processNewFuncsDefined(List<Thruple<FuncDef, FuncType, Boolean>> newfuncs) {
		StringBuilder sb = new StringBuilder();
		for(Thruple<FuncDef, FuncType, Boolean> itemx : newfuncs) {
			FuncDef funcDef = itemx.getA();
			FuncType ft = itemx.getB();
			boolean isNew = itemx.getC();
			sb.append("|  ");
			if(isNew) {
				sb.append("created");
				funcDef.supressErrors = true;
			}else {
				sb.append("redefined");
			}
			
			if(funcDef.extFunOn == null) {
				sb.append(" function ");
			}else {
				sb.append(" extension function ");
				sb.append(funcDef.extFunOn.toString());
				sb.append('.');
			}
			
			sb.append(formatTopLevelElement(funcDef));
			sb.append('\n');
		}
		
		return sb.toString().trim();
	}

	private static Method getMethod(Class<?> cls, String name, int args){
		Method[] meths= cls.getMethods();
		for(Method m : meths){
			if(m.getParameterTypes().length == args && m.getName().equals(name)) {
				return m;
			}
		}
		return null;
	}
}
