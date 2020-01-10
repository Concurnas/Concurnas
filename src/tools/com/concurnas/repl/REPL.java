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
import java.util.Set;
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
import com.concurnas.compiler.ast.Assign;
import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignMulti;
import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.DuffAssign;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.ImportStatement;
import com.concurnas.compiler.ast.Line;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.REPLTopLevelComponent;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.Statement;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.bytecode.BytecodeOutputter;
import com.concurnas.compiler.utils.BytecodePrettyPrinter;
import com.concurnas.compiler.visitors.REPLDepGraphManager.REPLComponentWrapper;
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
	public boolean printBytecode = false;
	public boolean debugmode = false;
	
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
	
	private REPLExecutor exec = null;
	private SchedulerRunner sr = null;
	public REPLExecutor getExecutor() {
		//return new REPLExecutor(new ConcurnasClassLoader(cpele, ClassPathUtils.getInstallationPath(), sharedConcClassLoader));
		
		if(null == exec) {
			exec = new REPLExecutor(ClassPathUtils.getSystemClassPathAsPaths());
		}
		
		return exec;
	}
	
	public SchedulerRunner getScheduler() throws Throwable {
		if(null == sr) {
			sr = new SchedulerRunner(exec, "REPLExe");
		}
		return sr;
	}
	
	
	LinkedHashMap<Pair<String, Type>, Line> persistedTopLevelElementSet = new LinkedHashMap<Pair<String, Type>, Line>();
	
	private Pair<List<Pair<REPLTopLevelComponent, Boolean>>, List<REPLComponentWrapper>> appendPrevCode(Block blk) {
		List<Pair<REPLTopLevelComponent, Boolean>> newlyDefined = new ArrayList<Pair<REPLTopLevelComponent, Boolean>>();
		List<REPLComponentWrapper> newTLEs = new ArrayList<REPLComponentWrapper>();
		
		LinkedHashMap<REPLComponentWrapper, Type> funcs = new LinkedHashMap<REPLComponentWrapper, Type>();
		//funcs for later iterations...
		for(LineHolder lh : blk.lines) {
			Line lin = lh.l;
			
			if(lin instanceof REPLTopLevelComponent) {
				REPLComponentWrapper wrap = new REPLComponentWrapper((REPLTopLevelComponent)lin);

				newTLEs.add(wrap);
				
				if(!((REPLTopLevelComponent) lin).persistant() ) {
					continue;//skip classdef, assignmentnew assignexisting etc
				}
				
				Type tt;
				if(lin instanceof FuncDef) {
					tt = ((FuncDef)lin).getFuncType().getErasedFuncTypeNoRet();
				}else if(lin instanceof ClassDef){
					tt = null;
				}else {
					tt = ((REPLTopLevelComponent)lin).getFuncType();
				}
				funcs.put(wrap, tt);
			}
		}
		
		for(REPLComponentWrapper replcom : funcs.keySet()) {
			REPLTopLevelComponent comp = replcom.comp;
			Type ft = funcs.get(replcom);
			Pair<String, Type> defFT = new Pair<String, Type>(comp.getName(), ft);
			boolean isNew = true ;
			if(persistedTopLevelElementSet.containsKey(defFT)) {
				//overwrite persisted version
				//this.moduleCompiler.moduleLevelFrame.removeFuncDef(defFT.getA(), defFT.getB(), true);
				persistedTopLevelElementSet.remove(defFT);
				isNew=false;
			}
			
			//if(replcom.comp instanceof FuncDef) {
				newlyDefined.add(new Pair<REPLTopLevelComponent, Boolean>(replcom.comp, isNew));
			//}
		}
		//now add funcs from prevous definitions unless redefined above....
		persistedTopLevelElementSet.values().forEach(a -> blk.addPenultimate(new LineHolder(a)));
		
		funcs.keySet().forEach(a -> persistedTopLevelElementSet.put( new Pair<String, Type>((a.comp).getName(), funcs.get(a)), (Line)a.comp));
		
		//ensure all imports are specified first...
		if(blk.lines.stream().anyMatch(a -> a.l instanceof ImportStatement)) {
			List<LineHolder> reordered = blk.lines.stream().filter(a -> a.l instanceof ImportStatement).collect(Collectors.toList());
			blk.lines.stream().filter(a -> !(a.l instanceof ImportStatement)).forEach(a -> reordered.add(a));
			blk.lines = new ArrayList<LineHolder>(reordered);
		}
		
		
		return new Pair<List<Pair<REPLTopLevelComponent, Boolean>>, List<REPLComponentWrapper>>(newlyDefined, newTLEs);
	}
	
	private static List<ErrorHolder> remoteSupressedErrors(HashSet<ErrorHolder> ers){
		ArrayList<ErrorHolder> ret = new ArrayList<ErrorHolder>();
		
		for(ErrorHolder er : ers) {
			if(er.getAnyContextHavingErrSupression()) {
				continue;//skip
			}
			
			REPLTopLevelComponent  replCtxt = er.getHeadContext();
			if(null != replCtxt) {
				String prefix = formatTopLevelElement(replCtxt);
				if(!prefix.contains("$")) {
					er = er.copyWithErPrefix(prefix);
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
			REPLTopLevelComponent item = wra.comp;
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
	
	private void processAssignments(Statement inst, ArrayList<String> ret) {
		if(inst instanceof AssignNew) {
			AssignNew ae = (AssignNew)inst;
			ret.add(ae.name);
		}else if(inst instanceof AssignExisting) {
			AssignExisting ae = (AssignExisting)inst;
			if(ae.assignee instanceof RefName) {
				ret.add(((RefName)ae.assignee).name);
			}
		}else if(inst instanceof AssignMulti) {
			AssignMulti am = (AssignMulti)inst;
			am.assignments.forEach(a -> processAssignments(a, ret));
		}
	}
	
	private ArrayList<String> removeLastLineIfJustRefName(Block blk) {
		LineHolder lhLast = blk.lines.get(blk.lines.size()-1);
		
		ArrayList<String> ret = new ArrayList<String>(1);
		
		if(lhLast.l instanceof DuffAssign) {
			DuffAssign da = (DuffAssign)lhLast.l;
			if(da.e instanceof RefName) {//remove it in this case
				blk.lines.remove(blk.lines.size()-1);
				ret.add(((RefName)da.e).name);
				return ret;
			}
		}else if(lhLast.l instanceof Assign || lhLast.l instanceof AssignMulti) {
			processAssignments((Statement)lhLast.l, ret);
		}
		
		return ret.isEmpty()?null:ret;
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
				
				ArrayList<String> varToShow = removeLastLineIfJustRefName(obtained.block);
				
				Pair<List<Pair<REPLTopLevelComponent, Boolean>>, List<REPLComponentWrapper>> newStuiffAndTLEs = appendPrevCode(obtained.block);
				
				List<Pair<REPLTopLevelComponent, Boolean>> newfuncs = newStuiffAndTLEs.getA();
				List<REPLComponentWrapper> newTopLevelItemsDeclared = newStuiffAndTLEs.getB();
				
				this.moduleCompiler.progressCompilationREPL(obtained.block, srcName, fw);
				HashSet<REPLComponentWrapper> depsUpdated = this.replState.replDepGraph.getAndResetThingsModified();
				
				List<ErrorHolder> warns = remoteSupressedErrors(this.moduleCompiler.warnings);
				if(warns != null && !warns.isEmpty()) {
					output.add(formatErrors(warns, "|  WARN"));
				}
								
				List<ErrorHolder> ersx = remoteSupressedErrors(this.moduleCompiler.getLastErrorSet());
				
				this.replState.topLevelItemsToSkip.forEach(a -> a.comp.setSupressErrors(true));//we only need these errors reported once
				
				if(ersx != null && !ersx.isEmpty()) {
					//unless all errors are within functions
					output.add(formatErrors(ersx, "|  ERROR"));
					if(ersx.stream().noneMatch(a -> a.hasContext())){
						return String.join("\n", output.stream().map(a -> a.trim()).collect(Collectors.toList()) );
					}
				}
								
				
				REPLExecutor instanceExecutor = getExecutor();
				//start scheduler if not already
				SchedulerRunner sch = getScheduler();
				
				try {
					{//repoint any other newly created instances
						List<String> items = fw.nametoCode.keySet().stream().sorted().collect(Collectors.toList());
						for(String name : fw.nametoCode.keySet()) {
							if(!name.equals(mastSrcName)) {
								byte[] rawcode = fw.nametoCode.get(name);
								byte[] codeRepointed = REPLCodeRepointStateHolder.repointToREPLStateHolder(rawcode, mastSrcName, srcName, false);
								instanceExecutor.nameToCode.put(name, codeRepointed);

								printByteCode(output, name, rawcode,  codeRepointed);
							}
						}
					}
					
					
					byte[] rawcode = fw.nametoCode.get(mastSrcName);
					byte[] codeRepointed = REPLCodeRepointStateHolder.repointToREPLStateHolder(rawcode, mastSrcName, srcName, true);
					
					printByteCode(output, "main", rawcode,  codeRepointed);
					
					/*
					 * HashSet<String> newvars = this.moduleCompiler.replState.getNewVars();
					 * if(input.trim().endsWith(";")) { newvars = null; }else if(varToShow != null)
					 * { newvars.add(varToShow); }
					 */
					
					Set<String> newvars = null;
					if(!input.trim().endsWith(";")){
						if(varToShow != null) {
							newvars = new HashSet<String>();
							newvars.addAll(varToShow);
						}else {
							newvars = this.moduleCompiler.replState.getNewVars();
							if(newvars.stream().anyMatch(a -> a.contains("$"))) {
								newvars = newvars.stream().filter(a -> a.contains("$")).collect(Collectors.toSet());
							}
						}
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
						String toAdd = processNewFuncsDefined(newfuncs);
						if(!toAdd.equals("")) {
							output.add(toAdd);
						}
					}
					
					removeTopLevelItemsFromUpdateSet(newTopLevelItemsDeclared, depsUpdated);
					
					if(!depsUpdated.isEmpty()) {
						//|    update modified method volume(double)
						List<String> reduced = depsUpdated.stream().map(a -> formatTopLevelElement(a)).filter(a -> !a.contains("$")).sorted().collect(Collectors.toList());
						if(!reduced.isEmpty()) {
							output.add("|    update modified " + String.join(", ", reduced));
						}
					}
					
					String got = (String)getMethod(executorTasCls, "getResult", 0).invoke(exeTaObject);
					
					if(got != null && !got.trim().isEmpty()) {
						if(!output.isEmpty()) {
							output.add("\n");
						}
						
						output.add(got);
					}
			
					return String.join("\n", output.stream().map(a -> a.trim()).collect(Collectors.toList()) );
					
				}catch(Throwable e){
					//TODO: format exception
					throw e;
				}finally {
					//sch.stop();
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
	
	private void printByteCode(ArrayList<String> output, String classname, byte[] rawcode, byte[] codeRepointed) throws Exception {
		if(printBytecode || debugmode) {
			StringBuilder pp = new StringBuilder();
			pp.append("|  Expression Bytecode["+classname+"]:");
			pp.append(BytecodePrettyPrinter.print(rawcode, true, null, "|  "));
			output.add(pp.toString());
		}
		if(debugmode) {
			StringBuilder pp = new StringBuilder();
			pp.append("|  Expression Bytecode["+classname+"] (post runtime adaptation):");
			pp.append(BytecodePrettyPrinter.print(codeRepointed, true, null, "|  "));
			output.add(pp.toString());
		}
	}
	
	private static String formatTopLevelElement(FuncDef funcDef) {
		StringBuilder sb = new StringBuilder();
		sb.append(funcDef.funcName);
		sb.append('(');
		
		ArrayList<Type> inputs = funcDef.getFuncType().getInputs();
		sb.append(String.join(", ", inputs.stream().map(a -> a.toString()).collect(Collectors.toList())));
		
		return sb.append(")").toString();
	}

	
	private static String formatTopLevelElement(REPLComponentWrapper item) {
		return formatTopLevelElement(item.comp).replace("repl$.", "");
	}
	
	private static String formatTopLevelElement(REPLTopLevelComponent item) {
		if(item instanceof FuncDef) {
			return formatTopLevelElement((FuncDef)item);
		}
		return item.getName();
	}

	private static String processNewFuncsDefined(List<Pair<REPLTopLevelComponent, Boolean>> newfuncs) {
		StringBuilder sb = new StringBuilder();
		for(Pair<REPLTopLevelComponent, Boolean> itemx : newfuncs) {
			REPLTopLevelComponent comp = itemx.getA();
			boolean isNew = itemx.getB();
			String formattedName = formatTopLevelElement(comp);
			
			if(null != formattedName) {
				sb.append("|  ");
				if(isNew) {
					sb.append("created ");
					comp.setSupressErrors(true);
				}else {
					sb.append("redefined ");
				}
				
				if(comp instanceof FuncDef) {
					FuncDef funcDef = (FuncDef)comp;
					if(funcDef.extFunOn == null) {
						sb.append("function ");
					}else {
						sb.append("extension function ");
						sb.append(funcDef.extFunOn.toString());
						sb.append('.');
					}
				}
				
				sb.append(formattedName);
				sb.append('\n');
			}
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
