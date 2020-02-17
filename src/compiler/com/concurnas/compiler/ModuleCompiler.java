package com.concurnas.compiler;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.ParseTree;

import com.concurnas.compiler.ConcurnasLexer;
import com.concurnas.compiler.ConcurnasParser;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.bytecode.BytecodeGennerator;
import com.concurnas.compiler.bytecode.FinallyBlockCodeCopier;
import com.concurnas.compiler.bytecode.LabelAllocator;
import com.concurnas.compiler.bytecode.ModuleLevelSharedVariableFuncGennerator;
import com.concurnas.compiler.utils.BytecodePrettyPrinter;
import com.concurnas.compiler.utils.CompiledCodeClassLoader;
import com.concurnas.compiler.utils.ITEM_TYPE;
import com.concurnas.compiler.utils.Profiler;
import com.concurnas.compiler.visitors.ConstantFolding;
import com.concurnas.compiler.visitors.DefaultActorGennerator;
import com.concurnas.compiler.visitors.DefoAssignmentVisitor;
import com.concurnas.compiler.visitors.ErrorRaiseable;
import com.concurnas.compiler.visitors.FieldAccessRepointer;
import com.concurnas.compiler.visitors.FlagLoners;
import com.concurnas.compiler.visitors.GPUKernalFuncTranspiler;
import com.concurnas.compiler.visitors.GenericTypeInferencer;
import com.concurnas.compiler.visitors.ImplicitUnrefNeedsDelete;
import com.concurnas.compiler.visitors.NestedFuncRepoint;
import com.concurnas.compiler.visitors.NonRefPostfixVisitorTagger;
import com.concurnas.compiler.visitors.ReturnVisitor;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.compiler.visitors.datastructs.TheScopeFrame;
import com.concurnas.repl.REPLState;
import com.concurnas.repl.REPLState.REPLTopLevelImports;
import com.concurnas.runtime.Pair;
import com.concurnas.runtimeCache.ReleaseInfo;
import com.concurnas.repl.REPLVariableState;


public class ModuleCompiler implements Comparable{
	//private final boolean DEBUG_MODE;
	
	private final MainLoop ml;
	public Block lexedAndParsedAST;
	private CompiledCodeClassLoader compiledClass = null;

	private final String srcFile;
	private final String classFile;
	private final String ouputfileNamePathNoType;
	//private final File compiledFileName;
	public String packageAndClassName;

	private final String fileNamePathNoType;
	private String outputClasstodir;
	private boolean interminalState = false;
	private final FileLoader loader;
	private final boolean verboseErrors;
	private final boolean verboseOutput;
	private FileWriter writer;
	private final PrintStream consoleOut;
	private boolean warnsAsErrors = false;
	private boolean printBytecode;
	private REPLState isREPL;
	public REPLVariableState replState;
	
	public ModuleCompiler(REPLState isREPL, String rootDir, String packageAndClassName, MainLoop ml, FileLoader loader, boolean verboseErrors, boolean verboseOutput, FileWriter writer, String outputDirOverride, PrintStream consoleOut, boolean printBytecode/*, boolean DEBUG_MODE*/) throws Exception
	{//path to src directory, how to compile java classes as well?
		this.ml = ml;
		this.packageAndClassName = packageAndClassName;
		this.loader = loader;
		this.verboseErrors = verboseErrors;
		this.verboseOutput = verboseOutput;
		this.consoleOut = consoleOut;
		this.printBytecode = printBytecode;
		//this.DEBUG_MODE = DEBUG_MODE;
		
		String sep = loader.getSeparator();
		
		fileNamePathNoType = rootDir + sep + packageAndClassName.replace(".", sep);
		if(null != outputDirOverride) {
			outputClasstodir = outputDirOverride;// + sep + packageAndClassName.replace(".", sep);
		}else {
			outputClasstodir = rootDir;
		}
		
		srcFile = loader.getCanonicalPath(fileNamePathNoType + ".conc" );
		classFile = loader.getCanonicalPath(outputClasstodir + ".class" ); //TODO: used incorrectly?
		
		ouputfileNamePathNoType = outputClasstodir + sep + packageAndClassName.replace(".", sep)+ ".class";
		
		this.writer = writer;
		this.isREPL = isREPL;
		
		if(isREPL == null) {
			checkSourceExists();
		}
	}
	
	private boolean hasBuiltInitialAST = false;
	
	public String getSrcFile() {
		return srcFile;
	}

	public String getClassFile() {
		return ouputfileNamePathNoType;
	}
	
	public void setWarnAsErrors(boolean werror) {
		this.warnsAsErrors  = werror;
	}
	
	@Override
	public String toString()
	{
		return this.srcFile;
	}
	
	public String getpackageAndClassName()
	{
		return this.packageAndClassName;
	}
	
	private void checkSourceExists() throws Exception
	{//Only rebuild if src file is more recent than binary if there is one
		if(!this.loader.fileExists(srcFile)) {
			throw new Exception("Unable to find source (.conc) or compiled (.class) code associated with: " + this.fileNamePathNoType + String.format(". Neither: %s or %s exist", classFile, srcFile));
		}
		
		/*boolean eClass = this.loader.fileExists(classFile);
		boolean eSrc =  this.loader.fileExists(srcFile);
		
		if(eSrc)
		{
			if(eClass)
			{
				long classLastChanged = this.loader.lastModified(classFile);
				long srcLastChanged = this.loader.lastModified(srcFile);
				if(srcLastChanged > classLastChanged)
					return true;
				return false;//TODO: list dependancies of class and recompile if any of them get changed.
			}
			return true;
		}
		else if(eClass)//class must exist surely
		{
			return false;//cannot do dependancy analysis cos nothing to recompile against...
		}
		else
		{
			//wtf?
			throw new Exception("Unable to find source (.conc) or compiled (.class) code associated with: " + this.fileNamePathNoType + String.format(". Neither: %s or %s exist", classFile, srcFile));
		}*/
	}
	
	public boolean finishedCompiling()
	{
		return interminalState || compiledClass != null;
	}
	
	public boolean wasProgressMade()
	{
		return wasProgressMade;
	}
	
	private void createCompileClass() throws Exception
	{//TODO: this should be mockable
		compiledClass = new CompiledCodeClassLoader(new File(classFile));
	}
	
	public boolean hasAnyResource(String dottedName, boolean isChildClassAsking)
	{//does this reolsve to anything?
		for(ITEM_TYPE var : ITEM_TYPE.values())
		{
			Object hasResource = getResource(var, dottedName, isChildClassAsking);
			
			if(null != hasResource)
			{
				if(hasResource instanceof Pair<?,?>){
					if(((Pair<?,?>)hasResource).getA() == null){
						continue;
					}
				}
				
				return true;
			}
		}
		return false;
	}
	
	public ClassDef getTopLevelClassDef()
	{
		if(compiledClass != null)
		{
			return new ClassDefJava(compiledClass.moduleLevelCls);
		}
		//else if(null!= moduleLevelFrame)
		//{
		// there is no such thing for concurnas as a top level class
		// package -> a/b/MyClasses.conc [package: a.b] <- MyClass module level top class
		// all class in here is static e.g Wroker:
		// from a.b.MyClasses import Wroker
		// doing this: a.b.MyClasses does nothing
		//u can have this in java though...
		//}
		return null;
	}
	
	public Object getResource(ITEM_TYPE type, String dottedName, boolean isChildClassAsking)
	{
		//e.g. com.business.myProject.adapter.B.x
		// -> [com.business.myProject.adapter.B] <- class, with x being the static global resource
		if(null!= moduleLevelFrame)
		{
			return moduleLevelFrame.getResource(type, dottedName, isChildClassAsking);
		} else if(compiledClass != null)
		{//this is only staic resources....
			return compiledClass.getResource(type, dottedName, isChildClassAsking);
		}
		return null;
	}
	
	public List<String> getAllStaticAssets(){
		ArrayList<String> ret = new ArrayList<String>();
		if(null != moduleLevelFrame) {
			ret.addAll(moduleLevelFrame.getAllTypeDefAtCurrentLevel().keySet());
			moduleLevelFrame.getAllClasses().forEach(a -> ret.add(a.getClassName()));
			ret.addAll(moduleLevelFrame.getAllFunctions().keySet());
			ret.addAll(moduleLevelFrame.getAllVars(null).keySet());
		}
		return ret;
	}
	
	
	//compiling real code...
	
	public HashSet<ErrorHolder> getLastErrorSet()
	{//dirty, very long winded way of getting last element, by iterating through the whole lot
		HashSet<ErrorHolder> ret = new HashSet<ErrorHolder>();
		for(HashSet<ErrorHolder> i: postASTErrorsHistory ){
			ret =i;
		}
		
		return ret;
	}
	

	public int getAttemptsAtCompilation()
	{
		return attemptsAtCompilation;
	}
	
	private Set<ErrorHolder> lexerAndParserErrors;//once only
	
	private LinkedHashSet<HashSet<ErrorHolder>> postASTErrorsHistory = new LinkedHashSet<HashSet<ErrorHolder>>();
	public HashSet<ErrorHolder> warnings = new HashSet<ErrorHolder>();
	private int attemptsAtCompilation = 0;
	private static final int MAX_COMP_ATTEMPTS = 50;
	private static final int MAX_COMP_INNER_ATTEMPTS = 50;
	private boolean wasProgressMade = true; //assume it all worked
	public TheScopeFrame moduleLevelFrame;
	
	//private int minAttempts = 2;//TODO
	
	private HashMap<String, ClassDef> typeDirectory = new HashMap<String, ClassDef>();
	
	private Visitor lastVisitor = null;//used to track current line, in case of internal compiler error
	
	private void prepareAndSetLastVisitor(Visitor vis){
		vis.resetLastLineVisited();
		lastVisitor = vis;
	}
	
	private long localLambdaCounthack = 0;
	
	public void progressCompilationREPL(Block lap, String srcName, FileWriter fw) throws Exception {
		lexedAndParsedAST = lap;
		postASTErrorsHistory = new LinkedHashSet<HashSet<ErrorHolder>>();
		warnings = new HashSet<ErrorHolder>();
		attemptsAtCompilation=0;
		hasBuiltInitialAST = true;
		wasProgressMade = true;
		//packageAndClassName = srcName;
		writer = fw;
		lastVisitor = null;
		typeDirectory = new HashMap<String, ClassDef>();
		interminalState = false;
		compiledClass = null;
		
		while(wasProgressMade) {
			tryToProgressCompilation();
			if(finishedCompiling()) {
				break;
			}
		}
	}
	
	public REPLTopLevelImports replLastTopLevelImports = null;//extractable and usable for repl
	
	private boolean errorsPermitCompilation(HashSet<ErrorHolder> latestRoundOfErrors) {
		//some errors, taking place in funcdefs permit partial compilation
		if(this.isREPL == null) {
			return latestRoundOfErrors.isEmpty();
		}else {
			//repl
			//all ers in thing
			if(latestRoundOfErrors.isEmpty()) {
				return true;
			}else {
				if(postASTErrorsHistory.contains(latestRoundOfErrors)) {//if we've had this set before
					return latestRoundOfErrors.stream().allMatch(a -> a.hasContext());
				}
				else {
					return false;
				}
			}
		}
	}
	
	public void tryToProgressCompilation() throws Exception
	{
		boolean isREPL = this.isREPL != null;
		if(!this.finishedCompiling()  )
		{
			this.init();
			HashSet<ErrorHolder> latestRoundOfErrors = new HashSet<ErrorHolder>();
			warnings = new HashSet<ErrorHolder>();
			String fullPathName = this.loader.getCanonicalPath(this.srcFile);
			if(null != lexerAndParserErrors) {
				latestRoundOfErrors.addAll(lexerAndParserErrors);
			}
		
			///have a go at formatting ast even if there are lexer and parser errors
			try
			{
				if(latestRoundOfErrors.isEmpty())
				{//dont even bother with rest of compilation if syntatically incorrect... this seems a bit lazy...
					//scope and type and other stuff
					ScopeAndTypeChecker scopeTypeChecker;
					
					if(null != moduleLevelFrame){
						scopeTypeChecker = new ScopeAndTypeChecker(this.ml, this, fullPathName, packageAndClassName, moduleLevelFrame, typeDirectory, true, this.isREPL);
					}
					else{
						scopeTypeChecker = new ScopeAndTypeChecker(this.ml, this, fullPathName, packageAndClassName, typeDirectory, true, this.isREPL);
						if(isREPL) {
							replState = new REPLVariableState(scopeTypeChecker.moduleLevelFrame);
						}
					}
					replLastTopLevelImports = scopeTypeChecker.replLastTopLevelImports;
					//return and dead code analysis
					ReturnVisitor 		      returnVisitor = new ReturnVisitor(fullPathName);
					//Last Thing Ret...
					FlagLoners 	 lastThingRet = new FlagLoners(fullPathName);
					//definite assignment
					DefoAssignmentVisitor defoAssignmentVis = new DefoAssignmentVisitor(fullPathName);
					
					NestedFuncRepoint nestedFuncRepoint = new NestedFuncRepoint(fullPathName);
					ConstantFolding constFolder = new ConstantFolding(fullPathName);
					
					DefaultActorGennerator defaultActorCreator = new DefaultActorGennerator(fullPathName);
					
					FieldAccessRepointer  fieldAccessReport = new FieldAccessRepointer(fullPathName/*, scopeTypeChecker1*/);
					
					VectorizedRedirector vectorizedRedirector = new VectorizedRedirector(fullPathName/*, scopeTypeChecker1*/);
					
					moduleLevelFrame = scopeTypeChecker.moduleLevelFrame;
					moduleLevelFrame.paThisIsModule=true;
					if(isREPL) {
						moduleLevelFrame.setIsREPL();
					}

					//System.err.println("start sac 1");
					prepareAndSetLastVisitor(scopeTypeChecker);
					scopeTypeChecker.visit(lexedAndParsedAST);
					if(this.profiler != null) { profiler.mark("Initial SATC: " + attemptsAtCompilation); }
					localLambdaCounthack = scopeTypeChecker.localLambdaCount;
					
					if(attemptsAtCompilation ==0){
						//TODO: remove this hack. For some reason in order to setup the scope frame properly u need to do at least two runs through the ScopeAndtpye Checker [bad]
						//here is code to replicate the issue:
						/*
						class Cont { }

						fun doings() String { return "" }
						 */
						//System.err.println("start sac 2");
						
						scopeTypeChecker = new ScopeAndTypeChecker(this.ml, this, fullPathName, packageAndClassName, moduleLevelFrame, typeDirectory, true, this.isREPL);
						scopeTypeChecker.localLambdaCount=localLambdaCounthack;
						if(isREPL) {
							replLastTopLevelImports = scopeTypeChecker.replLastTopLevelImports;
						}
						
						//moduleLevelFrame = scopeTypeChecker.moduleLevelFrame;
						//moduleLevelFrame.paThisIsModule=true;
						prepareAndSetLastVisitor(scopeTypeChecker);
						scopeTypeChecker.visit(lexedAndParsedAST);
						if(this.profiler != null) { profiler.mark("Additional SATC"); }
						localLambdaCounthack = scopeTypeChecker.localLambdaCount;
					}
					
					
					LinkedHashSet<ErrorHolder> stcErrs = scopeTypeChecker.getErrors();
					LinkedHashSet<ErrorHolder> stcWarns = scopeTypeChecker.getWarnss();
					
					if(stcErrs.isEmpty()){
						//JPT: needed when ur infering the return type of a async block
						prepareAndSetLastVisitor(nestedFuncRepoint);
						nestedFuncRepoint.doNestedRepoint(lexedAndParsedAST);
						if(this.profiler != null) { profiler.mark("Initial nestedFuncRepoint"); }
						prepareAndSetLastVisitor(vectorizedRedirector);
						vectorizedRedirector.visit(lexedAndParsedAST);
						if(this.profiler != null) { profiler.mark("Initial vectorizedRedirector"); }
						stcErrs.addAll(vectorizedRedirector.getErrors());
					}

					prepareAndSetLastVisitor(constFolder);
					constFolder.visit(lexedAndParsedAST);//safe here?
					if(this.profiler != null) { profiler.mark("Initial constFolder"); }

					stcErrs.addAll(constFolder.getErrors());
					
					prepareAndSetLastVisitor(returnVisitor);
					returnVisitor.doOperation(lexedAndParsedAST);
					if(this.profiler != null) { profiler.mark("Initial Retrun Visitor"); }
					stcErrs.addAll(returnVisitor.getErrors());
					
					prepareAndSetLastVisitor(fieldAccessReport);
					fieldAccessReport.visit(lexedAndParsedAST);
					prepareAndSetLastVisitor(defaultActorCreator);
					if(this.profiler != null) { profiler.mark("Initial defaultActorCreator"); }
					defaultActorCreator.doDefaultActorCreation(lexedAndParsedAST);
					
					int n=0;
					boolean visitedModLevelSharevVarGen=false;
					boolean anychagnes = fieldAccessReport.hadMadeRepoints() || nestedFuncRepoint.hadMadeRepoints() 
							|| returnVisitor.hadMadeRepoints() || vectorizedRedirector.hadMadeRepoints()
							|| scopeTypeChecker.hasSharedModuleLevelVars
							|| scopeTypeChecker.attemptGenTypeInference
							|| defaultActorCreator.changeMade;
					
					
					if(isREPL) {
						this.isREPL.replDepGraph.reset();
						
						if(!anychagnes) {//see if what we have added to the repl will result in changes to other areas of the graph...
							//output true if there are things needing recalculation
							if(this.profiler != null) { profiler.mark("Next REPLDepGraph"); }
							prepareAndSetLastVisitor(this.isREPL.replDepGraph);
							anychagnes = this.isREPL.replDepGraph.updateDepGraph(lexedAndParsedAST, this.moduleLevelFrame);
						}
					}
					
					
					
					int iter = 0;
					while( anychagnes){//we carry on with the cycle until there are no more repoints to make...
						iter++;
						//prepareAndSetLastVisitor(nestedFuncRepoint);
						nestedFuncRepoint.resetRepoints();
						//prepareAndSetLastVisitor(defaultActorCreator);
						//defaultActorCreator.resetRepoints();
						constFolder = new ConstantFolding(fullPathName);//just make a new one easier than clearing out existing errors etc

						scopeTypeChecker = new ScopeAndTypeChecker(this.ml, this, fullPathName, packageAndClassName, moduleLevelFrame, typeDirectory, !scopeTypeChecker.getErrors().isEmpty(), this.isREPL);//TODO: why do we create a new one?
						scopeTypeChecker.localLambdaCount=localLambdaCounthack;
						if(isREPL) {
							replLastTopLevelImports = scopeTypeChecker.replLastTopLevelImports;
						}

						prepareAndSetLastVisitor(scopeTypeChecker);
						scopeTypeChecker.visit(lexedAndParsedAST);
						if(this.profiler != null) { profiler.mark("Next SATC: " + iter); }
						localLambdaCounthack = scopeTypeChecker.localLambdaCount;

						GenericTypeInferencer genTypeInfer = null;
						if(scopeTypeChecker.attemptGenTypeInference) {
							genTypeInfer = new GenericTypeInferencer(scopeTypeChecker);
							prepareAndSetLastVisitor(genTypeInfer);
							genTypeInfer.visit(lexedAndParsedAST);
							if(this.profiler != null) { profiler.mark("Next GenericTypeInferencer"); }
						}
						
						vectorizedRedirector.restart();
						
						stcErrs = scopeTypeChecker.getErrors();
						stcWarns = scopeTypeChecker.getWarnss();
						
						if(stcErrs.isEmpty() || scopeTypeChecker.attemptGenTypeInference){//JPT: maybe this check can be removed - and u can run this code whatever
							
							if(stcErrs.isEmpty()) {
								prepareAndSetLastVisitor(nestedFuncRepoint);
								nestedFuncRepoint.doNestedRepoint(lexedAndParsedAST);
								if(this.profiler != null) { profiler.mark("Next nestedFuncRepoint"); }
							}
							
							prepareAndSetLastVisitor(constFolder);
							constFolder.visit(lexedAndParsedAST);
							if(this.profiler != null) { profiler.mark("Next constFolder"); }
							prepareAndSetLastVisitor(vectorizedRedirector);
							vectorizedRedirector.visit(lexedAndParsedAST);
							if(this.profiler != null) { profiler.mark("Next vectorizedRedirector"); }
							stcErrs.addAll(vectorizedRedirector.getErrors());
						}
						stcErrs.addAll(constFolder.getErrors());

						prepareAndSetLastVisitor(fieldAccessReport);
						fieldAccessReport.visit(lexedAndParsedAST);
						if(this.profiler != null) { profiler.mark("Next fieldAccessReport"); }
						prepareAndSetLastVisitor(returnVisitor);
						returnVisitor.doOperation(lexedAndParsedAST);
						if(this.profiler != null) { profiler.mark("Next returnVisitor"); }
						
						
						stcErrs.addAll(returnVisitor.getErrors());
						if(n++ > MAX_COMP_INNER_ATTEMPTS) {
							stcErrs.add(new ErrorHolder(fullPathName, 0,0, "Exceeded max internal compilation cycle attempts: " + MAX_COMP_INNER_ATTEMPTS));//
							break;
						}
						
						anychagnes = (genTypeInfer != null && genTypeInfer.hadMadeRepoints()) || fieldAccessReport.hadMadeRepoints() || nestedFuncRepoint.hadMadeRepoints() || returnVisitor.hadMadeRepoints() || vectorizedRedirector.hadMadeRepoints();
						
						if(!anychagnes) {
							if(scopeTypeChecker.hasSharedModuleLevelVars && !visitedModLevelSharevVarGen) {
								visitedModLevelSharevVarGen = true;
								ModuleLevelSharedVariableFuncGennerator mlsvfg = new ModuleLevelSharedVariableFuncGennerator();
								prepareAndSetLastVisitor(mlsvfg);
								if(this.profiler != null) { profiler.mark("Next ModuleLevelSharedVariableFuncGennerator"); }
								mlsvfg.visit(lexedAndParsedAST);
								anychagnes=true;
							}
						}
						
						
						
						if(isREPL && !anychagnes) {//see if what we have added to the repl will result in changes to other areas of the graph...
							//output true if there are things needing recalculation
							if(this.profiler != null) { profiler.mark("Next REPLDepGraph"); }
							prepareAndSetLastVisitor(this.isREPL.replDepGraph);
							anychagnes = this.isREPL.replDepGraph.updateDepGraph(lexedAndParsedAST, moduleLevelFrame);
						}
					}
					
					latestRoundOfErrors.addAll(stcErrs);
					
					if(!stcWarns.isEmpty()) {
						if(warnsAsErrors) {
							latestRoundOfErrors.addAll(stcWarns);
						}else {
							warnings.addAll(stcWarns);
						}
					}
					
					
					for(Visitor step : new Visitor[]{ defoAssignmentVis, lastThingRet}){
						step.visit(lexedAndParsedAST);
						latestRoundOfErrors.addAll(step.getErrors());
					}
					//TODO: last thing ret is called twice?
					prepareAndSetLastVisitor(lastThingRet);
					lastThingRet.visit(lexedAndParsedAST);
					if(this.profiler != null) { profiler.mark("lastThingRet"); }
					latestRoundOfErrors.addAll(lastThingRet.getErrors());
				
					
					ErrorRaiseable erSup = scopeTypeChecker.getErrorRaiseableSupression();
					
					if(errorsPermitCompilation(latestRoundOfErrors))
					{//comple if other aspects above correct
						
						if(isREPL) {
							this.isREPL.topLevelItemsToSkip.forEach(a -> {
								a.comp.setSkippable(true);
							});
						}
						
						
						//gennerate C99 compliant opencl code if applicable and possible
						if(scopeTypeChecker.hasGPUFuncorkernals) {
							LinkedHashSet<ErrorHolder> errors = GPUKernalFuncTranspiler.performTranspilation(moduleLevelFrame, fullPathName, lexedAndParsedAST);
							if(this.profiler != null) { profiler.mark("GPUKernalFuncTranspiler"); }
							latestRoundOfErrors.addAll(errors);
						}
						
						if(!scopeTypeChecker.hasGPUFuncorkernals || errorsPermitCompilation(latestRoundOfErrors)) {
							//tag postfix operations that return refs where the callee doesnt want to recieve a ref on the stack but instead the value of the ref
							NonRefPostfixVisitorTagger nonRefPostfixVisitorTagger = new NonRefPostfixVisitorTagger(fullPathName);
							prepareAndSetLastVisitor(nonRefPostfixVisitorTagger);
							nonRefPostfixVisitorTagger.visit(lexedAndParsedAST);
							if(this.profiler != null) { profiler.mark("fin nonRefPostfixVisitorTagger"); }
							
							//preallocate labels
							FinallyBlockCodeCopier tcfbcc = new FinallyBlockCodeCopier();
							prepareAndSetLastVisitor(tcfbcc);
							tcfbcc.visit(lexedAndParsedAST);
							if(this.profiler != null) { profiler.mark("fin FinallyBlockCodeCopier"); }
							
							LabelAllocator preAllocator = new LabelAllocator();
							prepareAndSetLastVisitor(preAllocator);
							preAllocator.visit(lexedAndParsedAST);
							if(this.profiler != null) { profiler.mark("fin LabelAllocator"); }
							
							ImplicitUnrefNeedsDelete impliocitUnrefTagger = new ImplicitUnrefNeedsDelete();
							prepareAndSetLastVisitor(impliocitUnrefTagger);
							impliocitUnrefTagger.visit(lexedAndParsedAST);
							if(this.profiler != null) { profiler.mark("fin ImplicitUnrefNeedsDelete"); }
							
							BytecodeGennerator bytecodeVisitor = new BytecodeGennerator(moduleLevelFrame, this.packageAndClassName, erSup, typeDirectory, this.isREPL != null);
							try{
								prepareAndSetLastVisitor(bytecodeVisitor);
								bytecodeVisitor.visit(lexedAndParsedAST);
								if(this.profiler != null) { profiler.mark("fin BytecodeGennerator"); }
							}
							catch(Exception e){
								throw e;
							}
							
							if(null != this.writer){
								LinkedHashMap<String, byte[]> mmap = bytecodeVisitor.toByteArray();
								ArrayList<String> wroteFiles = new ArrayList<String>();
								
								for(String clsFile : mmap.keySet()){
									String outputPath = this.outputClasstodir + loader.getSeparator() + clsFile + ".class";

									byte[] code = mmap.get(clsFile);
									
									if(this.printBytecode) {
										consoleOut.println("Created Bytecode for: " + clsFile + ":");
										
										BytecodePrettyPrinter.print(code, true, this.consoleOut, null);
										
										consoleOut.println();
									}
									
									this.writer.writeClass(outputPath, clsFile, code);
									
									wroteFiles.add(String.format("%s [%s]" , outputPath, clsFile ));
								}
								
								if(verboseOutput) { 
									this.consoleOut.println(String.format("Finished compilation of: %s -> %s", this.srcFile,  String.join(", ", wroteFiles)) );
								}
							}
						}
					}
				}
			}
			catch(Throwable e){
				String errMsg = String.format("\n\nBug Report: Internal compiler error on compilation of: %s due to %s: %s", this.srcFile, e, e.getMessage());
				if(this.verboseErrors){
					StringBuilder expanded = new StringBuilder();
					expanded.append("\n"+String.join("", Collections.nCopies(errMsg.length()-2, "-")));

					expanded.append("\nFile name: " + this.srcFile);
					expanded.append(String.format("\n\nCompiler build number: %s" , ReleaseInfo.getVersion(), ReleaseInfo.getVersionDate()));
					
					int lineAtfault = -1;
					//find line number at fault
					if(lastVisitor != null){
						expanded.append("\nException in phase: " + lastVisitor.getClass().getSimpleName());
						lineAtfault = lastVisitor.getLastLineVisited();
						if(lineAtfault != -1){
							expanded.append("\nException at line: " + lineAtfault);
						}
					}
					
					expanded.append("\nException: " + e);
					expanded.append("\nException message: " + e.getMessage());
										
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					expanded.append("\nException stacktrace:\n" + sw);
					
					if(null != this.origInpu) {
						expanded.append("\nSource:\n");
						
						//add line numbers
						String[] fileContents = this.origInpu.toString().split("\n");//yuck
						int fsize = fileContents.length;
						int spacesForLines = (int)Math.ceil(Math.log(fsize)/log10);
						
						if(this.srcFile.endsWith("bytecodeSandbox.conc")){
							for(int n=0; n < fsize; n++){
								if(fileContents[n].equals("//##EOF")){
									fsize = n;//HACK: sandbox is massive, so this truncates the output. Remove before release
									break;
								}
							}
						}
						
						for(int n=0; n < fsize; n++){
							int sizeLineCnt = n==0 | n == 1?0:(int)Math.floor(Math.log(n+1)/log10);
							int spaces = spacesForLines - sizeLineCnt;
							if(lineAtfault-1 == n){
								expanded.append(String.join("", Collections.nCopies(spaces+1, "-")));
								expanded.append(">");
							}else{
								expanded.append(String.join("", Collections.nCopies(spaces+2, " ")));
							}
							expanded.append(n+1);
							expanded.append(": ");
							expanded.append(fileContents[n]);
							expanded.append("\n");
						}
						

						expanded.append("\n"+String.join("", Collections.nCopies(errMsg.length()-2, "-")));
					}
					
					errMsg += expanded;
				}
								
				latestRoundOfErrors.add(new ErrorHolder(this.loader.getCanonicalPath(this.srcFile), 0,0, errMsg));
				postASTErrorsHistory.add(latestRoundOfErrors);
				
				this.wasProgressMade = true;
				interminalState = true;
				return;
			}
			
			boolean progmade = false;
			
			attemptsAtCompilation++;
			
			if(errorsPermitCompilation(latestRoundOfErrors))
			{//horray!
				createCompileClass();
				//this.consoleOut.println("Compiled: " + this.srcFile);
				progmade = true;
				postASTErrorsHistory.add(latestRoundOfErrors);
				if(this.profiler != null) { profiler.end(); }
			}
			else if(attemptsAtCompilation > MAX_COMP_ATTEMPTS-1)
			{
				postASTErrorsHistory.add(latestRoundOfErrors);
				if(this.profiler != null) { profiler.mark("Comp attempt: " + attemptsAtCompilation); }
			}
			else if(!postASTErrorsHistory.contains(latestRoundOfErrors))
			{//never had these erorrs before so must be something new going wrong...
				//System.err.println("errors: " + latestRoundOfErrors);
				postASTErrorsHistory.add(latestRoundOfErrors);
				progmade = true;
				if(this.profiler != null) { profiler.mark("Comp attempt: " + attemptsAtCompilation); }
			}
			
			this.wasProgressMade = progmade;
		}
		else
		{
			this.wasProgressMade = true;
		}
	}
	
	private final static double log10 = Math.log(10);
	
	/*
	 * private static int countOccurrences(String input, char thing) { int count =
	 * 0; for (int i = 0; i < input.length(); i++) { if (input.charAt(i) == thing) {
	 * count++; } } return count; }
	 * 
	 * private static int cntSemis(String s){ int counter = 0; for( int i=0;
	 * i<s.length(); i++ ) { if( s.charAt(i) == ';' ) { counter++; } } return
	 * counter; }
	 */
	
	
	private CharStream origInpu;
	
	private Profiler profiler = null;
	
	public void enableProfiling(Profiler profiler) {
		this.profiler = profiler;
	}
	
	private void buildInitialAST()
	{
		this.lexerAndParserErrors = new HashSet<ErrorHolder>();
		try
		{
			String filename = this.srcFile;
			//CharStream input = CharStreams.fromPath(Paths.get(filename));
			CharStream input =this.loader.readFile(this.srcFile);
			
			origInpu = input;

			LexParseErrorCapturer lexerErrors = new LexParseErrorCapturer(filename);
			LexParseErrorCapturer parserErrors = new LexParseErrorCapturer(filename);
			
			ConcurnasLexer lexer = new ConcurnasLexer(input);
			if(this.profiler != null) { profiler.mark("make lexer"); }
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			if(this.profiler != null) { profiler.mark("make tokens"); }
			ConcurnasParser parser = new ConcurnasParser(tokens);
			parser.setInputStream(new CommonTokenStream(lexer));
			if(this.profiler != null) { profiler.mark("make parser"); }

			parser.getInterpreter().setPredictionMode(PredictionMode.SLL);  
			
			parser.removeErrorListeners(); // remove ConsoleErrorListener
			parser.addErrorListener(lexerErrors); // add ours
			
			lexer.removeErrorListeners(); // remove ConsoleErrorListener
			lexer.addErrorListener(parserErrors); // add ours
			
			
			ParseTree tree = parser.code();
			if(this.profiler != null) { profiler.mark("Parse"); }
			
			ArrayList<ErrorHolder> lexerErrorsAR = lexerErrors.errors;
			ArrayList<ErrorHolder> parserErrorsAR = parserErrors.errors;
			
			int lexerErrorsCnt = lexerErrorsAR.size();
			int parserErrorsCnt = parserErrorsAR.size();
			if((lexerErrorsCnt + parserErrorsCnt) > 0){
				if(lexerErrorsCnt > 0){  for(ErrorHolder s : lexerErrorsAR) { lexerAndParserErrors.add(s); }; lexerErrorsAR.clear(); }
				if(parserErrorsCnt > 0){ for(ErrorHolder s : parserErrorsAR) { lexerAndParserErrors.add(s); }; parserErrorsAR.clear(); }
				return;
			}
			
			lexedAndParsedAST = (Block)new ASTCreator(filename, parserErrors).visit(tree);
			lexedAndParsedAST.isModuleLevel = true;
		
			lexerErrorsCnt = lexerErrorsAR.size();
			parserErrorsCnt = parserErrorsAR.size();
			if((lexerErrorsCnt + parserErrorsCnt) > 0){
				if(lexerErrorsCnt > 0){  for(ErrorHolder s : lexerErrorsAR) { lexerAndParserErrors.add(s); } }
				if(parserErrorsCnt > 0){ for(ErrorHolder s :parserErrorsAR) { lexerAndParserErrors.add(s); } }
			}
			
		}
		catch(Exception e)
		{
			lexerAndParserErrors.add(new ErrorHolder(""+this.srcFile, 0,0, "Unable to compile because: " + e.getMessage()) );
			if(verboseErrors)
			{
				e.printStackTrace();
			}
		}
	}
	
	
	@Override
	public int hashCode()
	{
		return this.fileNamePathNoType.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof ModuleCompiler)
		{
			return ((ModuleCompiler)obj).fileNamePathNoType.equals(this.fileNamePathNoType);
		}
		return false;
	}

	public Block getRootBlock() {
		return this.lexedAndParsedAST;
	}

	@Override
	public int compareTo(Object o) {
		if(o instanceof ModuleCompiler) {
			return ((ModuleCompiler)o).packageAndClassName.compareTo(this.packageAndClassName);
		}
		return 0;
	}

	public void init() {
		if(!this.hasBuiltInitialAST){
			buildInitialAST();
			hasBuiltInitialAST = true;
		}
	}
	
	public String replInit(String input) {
		
		
		
		return null;
	}
	
}
