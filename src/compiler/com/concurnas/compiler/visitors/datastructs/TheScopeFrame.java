package com.concurnas.compiler.visitors.datastructs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.objectweb.asm.Opcodes;

import com.concurnas.compiler.ast.AccessModifier;
import com.concurnas.compiler.ast.Annotations;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.FuncParam;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.GPUFuncVariant;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.NullStatus;
import com.concurnas.compiler.ast.PrimativeType;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.util.GPUKernelFuncDetails;
import com.concurnas.compiler.typeAndLocation.Location;
import com.concurnas.compiler.typeAndLocation.LocationClassField;
import com.concurnas.compiler.typeAndLocation.LocationStaticField;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.ITEM_TYPE;
import com.concurnas.compiler.utils.StringUtils;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.utils.TypeDefTypeProvider;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.compiler.visitors.WarningVariant;
import com.concurnas.compiler.visitors.util.VarAtScopeLevel;
import com.concurnas.runtime.Pair;


/**
 * 
 * Ordered:
 * 
 * module         _O_
	impots        _O_
	vars          _O_
	funcs         _O_
	classes       _ _ - actually get setup as my.thing.pkg.moduleName$Class - effectively a child class
		classes   _ _
		vars      _ _
		funcs     _ _
			vars  _O_
			funcs _O_
 *
 */


/*
 * block, asnyc -if else etc
 * class
 * function
 * module
 * 
 * 
 * 
 */

public class TheScopeFrame {
	
	public static TheScopeFrame buildTheScopeFrame_Module()
	{
		return new TheScopeFrame(null, true, false, false, null);
	}
	
	public static TheScopeFrame buildTheScopeFrame_Class(TheScopeFrame parent, boolean isClass, ClassDef classDef)
	{
		TheScopeFrame child = new TheScopeFrame(parent, true, false, true, classDef);
		parent.addChild(classDef.classBlock, child);
		return child;
	}
	
	public static TheScopeFrame buildTheScopeFrame_Block(TheScopeFrame parent, Block source)
	{
		TheScopeFrame child = new TheScopeFrame(parent, true, false, false, null);
		if(null != source.suppressedWarnings){
			child.setSuppressedWarnings(source.suppressedWarnings);
		}
		parent.addChild(source.getIdentity(), child);
		//parent.addChild(source, child);
		return child;
	}
	
	
	public void addChild(Block source, TheScopeFrame child)
	{
		//StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		//System.out.println(String.format("addChild %s -> %s by: %s", source, child, stackTraceElements[5]));
		source.setScopeFrame(child);
		children.put(source, child);
	}
	
	public TheScopeFrame searchForChild(Node source){
		if(source instanceof Block){
			source = ((Block)source).getIdentity();
		}
		
		TheScopeFrame ret = this.children.get(source);
		if(ret == null){
			for(TheScopeFrame scf : this.children.values()){
				TheScopeFrame got = scf.searchForChild(source);
				if(got != null){
					return got;
				}
			}
		}
		
		return ret;
	}
	
	public TheScopeFrame getChild(Block source)
	{
		TheScopeFrame ret = this.children.get(source.getIdentity());
		if(null == ret){
			/*ret = searchForChild(source);
			if(null == ret){*/
			ret = source.getScopeFrame();//BAD, how did this happen?
				//throw new RuntimeException("Unknown child referd to in scope frame navigation");
			//}
		}
		ret.parent =this;
		return ret;
	}
	
	public HashMap<Node, TheScopeFrame> children = new HashMap<Node, TheScopeFrame>();
	
	//
	public enum ExisitsAlready {FALSE, TRUE, TRUE_IMPLICIT, TRUE_GT_ERASED} //helps to provide better err desc
	//
	
	/*
	 * vars to add on entry:
	 * if, elif
	 * catch
	 * functions
	 * 
	 */
	
	//////

	private boolean isPersisted;
	private boolean returnable;
	private boolean isClass;
	private ClassDef classDef;
	private TheScopeFrame parent;
	public boolean paThisIsModule = false; //at module level?
	//private boolean isREPL = false; //at module level?
	public String moduleName = null;
	public Boolean canContainAContinueOrBreak = false;//false, null or true - if null then thing doesnt care if u have a break in it, like a if stmt
	public Boolean canContainAReturnStmt = false;//false, null or true - if null then thing doesnt care if u have a break in it, like a if stmt
	
	public void setIsREPL() {
		replModLevelForcedNewvars = new HashMap<String, TypeAndLocation>();
		replModLevelForcedNewfuncs = new HashMap<String, HashSet<FuncType>>();
		replModLevelForcedNewClasses = new HashMap<String, ClassDef>();
		replNameToRemoveAtEndOfSessionVARS = new HashSet<String>();
		replNameToRemoveAtEndOfSessionFUNCS = new HashSet<String>();
		replNameToRemoveAtEndOfSessionTYPEDEF = new HashSet<String>();
		//isREPL=true;
	}
	
	public TheScopeFrame getParent()
	{
		return this.parent;
	}
	
	public void setParent(TheScopeFrame parent)
	{
		this.parent = parent;
	}
	
	//TODO:
	private TheScopeFrame(TheScopeFrame parent, boolean isPersisted, boolean returnable, boolean isClass, ClassDef classDef)
	{
		this.parent= parent;
		this.isPersisted = isPersisted;//i.e. for classes and modules - we have sequential loading as well
		//TODO: remove isPersisted?
		this.returnable = returnable;
		this.isClass = isClass;
		this.classDef = classDef;
		//but in the cycle of a class respect the ordering of variable declarations
		
		//init init of these
		this.classesSelfRequestor = new HashMap<String, ClassDef>();
		this.varsSelfRequestor    = new HashMap<String, TypeAndLocation>();
		this.funcsSelfRequestor   = new HashMap<String, HashSet<TypeAndLocation>>();
		this.consSelfRequestor   = new HashSet<FuncType>();
	}
	
	public boolean isClass() {
		return isClass;
	}
	
	public ClassDef getClassDef()
	{
		return this.classDef;
	}
	
	public boolean isReturnable()
	{
		return this.returnable;
	}
	
	public void enterScope()
	{
		
		this.classesSelfRequestor = new HashMap<String, ClassDef>();
		this.varsSelfRequestor    = new HashMap<String, TypeAndLocation>();
		this.funcsSelfRequestor   = new HashMap<String, HashSet<TypeAndLocation>>();
		this.consSelfRequestor   = new HashSet<FuncType>();
	}
	
	public TheScopeFrame doleaveScope(boolean copyParentsUp, boolean isREPL)
	{
		if(copyParentsUp){
			for(Node nd : this.children.keySet()){
				this.parent.addChild(((Block)nd).getIdentity(), this.children.get(nd));
			}
		}

		this.classesSelfRequestor = new HashMap<String, ClassDef>();
		
		if(!isREPL) {
			this.varsSelfRequestor    = new HashMap<String, TypeAndLocation>();
			this.consSelfRequestor   = new HashSet<FuncType>();
		}
		this.funcsSelfRequestor   = new HashMap<String, HashSet<TypeAndLocation>>();
		
		return this.parent;
	}
	
	public void updatePrevSessionVars () {
		this.replPrevSessionVars.addAll(replModLevelForcedNewvars.keySet());
		this.replPrevSessionClasses.putAll(replModLevelForcedNewClasses);
		
		for(String name : replModLevelForcedNewfuncs.keySet()) {
			HashSet<FuncType> toAdd = replModLevelForcedNewfuncs.get(name);
			if(this.replPrevSessionFuncs.containsKey(name)) {
				this.replPrevSessionFuncs.get(name).addAll(toAdd);
			}else {
				this.replPrevSessionFuncs.put(name, new HashSet<FuncType>(toAdd));
			}
		}
		
	}
	
	public TheScopeFrame leaveScopeREPL(boolean isREPL)
	{
		updatePrevSessionVars();
		return doleaveScope(false, isREPL);
	}
	
	public TheScopeFrame leaveScope(boolean copyParentsUp) {
		return doleaveScope(copyParentsUp, false);
	}
	
	public TheScopeFrame leaveScope()
	{
		return leaveScope(false);
	}
	
	//when u load a class it's progressive just like functions etc, BUT stuff is globally visible outside the class.

	///helpers... sue me

	
	private HashMap<String, ClassDef> 			classes = new HashMap<String, ClassDef>();
	private HashMap<String, TypeAndLocation>     			vars    = new HashMap<String, TypeAndLocation>();
	////REPL ///
	public HashMap<String, TypeAndLocation>     			replModLevelForcedNewvars   ;
	public HashMap<String, HashSet<FuncType>>     			replModLevelForcedNewfuncs   ;
	public HashMap<String, ClassDef>     			replModLevelForcedNewClasses   ;
	
	//del
	public HashSet<String>  replNameToRemoveAtEndOfSessionVARS = new HashSet<String>();
	public HashSet<String>  replNameToRemoveAtEndOfSessionFUNCS = new HashSet<String>();
	public HashSet<String>  replNameToRemoveAtEndOfSessionCLASSES = new HashSet<String>();
	public HashSet<String>  replNameToRemoveAtEndOfSessionTYPEDEF = new HashSet<String>();
	//
	
	
	public HashSet<String> replPrevSessionVars = new HashSet<String>();
	public HashMap<String, HashSet<FuncType>> replPrevSessionFuncs = new HashMap<String, HashSet<FuncType>>();
	public HashMap<String, ClassDef> replPrevSessionClasses = new HashMap<String, ClassDef>();
	
	public HashSet<String> replAssignedthisIteration = new HashSet<String>();
	////REPL ///
	
	private HashSet<String>     			   asses    = new HashSet<String>();
	private HashSet<FuncType>                   cons    = new HashSet<FuncType>();
	private HashMap<String, HashSet<TypeAndLocation>>  funcs   = new HashMap<String, HashSet<TypeAndLocation>>();
	
	private HashMap<String, ClassDef> classesSelfRequestor = new HashMap<String, ClassDef>();
	private HashMap<String, TypeAndLocation>     varsSelfRequestor    = new HashMap<String, TypeAndLocation>();
	private HashMap<String, HashSet<TypeAndLocation>>  funcsSelfRequestor   = new HashMap<String, HashSet<TypeAndLocation>>();
	private HashSet<FuncType>  consSelfRequestor   = new HashSet<FuncType>();
	public boolean staticFunc = false;
	public boolean isconstructor=false;
	public boolean isAnnotation=false;;
	
	public void resetVars(){
		vars    = new HashMap<String, TypeAndLocation>();
		asses    = new HashSet<String>();
	}
	
	public boolean isRequestorScopeMeOrMyChild(TheScopeFrame requestor)
	{
		if(this == requestor) {
			if( this.paThisIsModule && this.replModLevelForcedNewvars != null) {
				return false;
			}
			
			return true;
		}
		
		return false;
	}
	
	
	////////////////////////////////////////////////////////////////////////////////////////
	public ArrayList<TypeAndLocation> getAllVaraiblesAtScopeLevelNeedingAccessor(){
		ArrayList<TypeAndLocation> ret = new ArrayList<TypeAndLocation>();
		for(String varname : this.vars.keySet())
		{
			TypeAndLocation tal = this.vars.get(varname);
			if(null != tal.getLocation().getPrivateStaticAccessorRedirectFuncGetter()){
				ret.add(tal);
			}
		
		}
		return ret;
	}
	
	
	public ArrayList<TypeAndLocation> getAllVaraiblesAtScopeLevelNeedingAccessorSetter(){
		ArrayList<TypeAndLocation> ret = new ArrayList<TypeAndLocation>();
		for(String varname : this.vars.keySet())
		{
			TypeAndLocation tal = this.vars.get(varname);
			if(null != tal.getLocation().getPrivateStaticAccessorRedirectFuncSetter()){
				ret.add(tal);
			}
		}
		return ret;
	}
	
	private static final Pattern nestedName = Pattern.compile("\\$n");
	
	public ArrayList<VarAtScopeLevel> getAllVariablesAtScopeLevel(boolean justFinals, boolean removeNestingIndicactorHelperVars, boolean ignoreIsOverride, boolean justNonNullable)
	{
		ArrayList<VarAtScopeLevel> ret = new ArrayList<VarAtScopeLevel>();
		for(String varname : this.vars.keySet())
		{
			TypeAndLocation tal = this.vars.get(varname);
			if(!justFinals || tal.getLocation().isFinal()){
				if(null != tal){
					Location loc = tal.getLocation();
					if(!removeNestingIndicactorHelperVars || !varname.contains("$n") || loc.localClassImportedField){
						int extraModifiers = 0;
						boolean assignedOnCreationAndFinal = false;
						boolean assignedOnCreation = false;
						if(loc instanceof LocationStaticField){
							LocationStaticField asstataic = (LocationStaticField)loc;
							if(asstataic.enumValue){
								extraModifiers += Opcodes.ACC_ENUM + Opcodes.ACC_STATIC;
							}
						}
						else if(loc instanceof LocationClassField) {
							if(ignoreIsOverride && ((LocationClassField)loc).isOverride) {
								continue;
							}
							
							assignedOnCreation = ((LocationClassField)loc).assignedOnCreation;
							assignedOnCreationAndFinal = assignedOnCreation && loc.isFinal();
						}
						
						if(loc.isTransient()){
							extraModifiers += Opcodes.ACC_TRANSIENT;
						}

						//TODO: add annotation to variable if shared						
						{
							Matcher m = nestedName.matcher(varname);//JPT: a bit hacky, better to find out where $n1$n2 is coming from
							if(m.find() && m.find()){//if there are >=2 nested $n then we ignore this
								continue;
							}
						}
						
						if(justNonNullable) {
							Type tt = tal.getType();
							if(assignedOnCreation) {
								continue;
							}
							
							if(tt instanceof PrimativeType && !tt.hasArrayLevels()) {
								continue;
							}
							
							if(tt.getNullStatus() == NullStatus.NULLABLE) {
								continue;
							}
							
							if(TypeCheckUtils.hasRefLevels(tt)) {
								continue;
							}
						}
						
						ret.add(new VarAtScopeLevel(varname, tal.getType(), loc.isFinal(), loc.getAccessModifier(), extraModifiers, loc.annotations, assignedOnCreationAndFinal, assignedOnCreation, loc.isInjected, loc.isShared()));
					}
				}
				
			}
		}
		
		return ret;
	}
	
	public ArrayList<Thruple<String, TypeAndLocation, AccessModifier>> getAllMethodsAtScopeLevel(boolean justFinals, boolean removeNestingIndicactorHelperVars){
		ArrayList<Thruple<String, TypeAndLocation, AccessModifier>> ret = new ArrayList<Thruple<String, TypeAndLocation, AccessModifier>>();
		for(String varname : this.funcs.keySet())
		{
			HashSet<TypeAndLocation> tals = this.funcs.get(varname);
			for(TypeAndLocation tal : tals){
				if(!justFinals || tal.getLocation().isFinal()){
					Location loc = tal.getLocation();
					if(!removeNestingIndicactorHelperVars || !varname.contains("$n")){
						ret.add(new Thruple<String, TypeAndLocation, AccessModifier>(varname, tal, loc.getAccessModifier() ));
					}
				}
			}
		}
		
		return ret;
	}
	
	private Map<String, TypeDefTypeProvider> typedefs = new LinkedHashMap<String, TypeDefTypeProvider>();
	
	public TypeDefTypeProvider getTypeDef(String name){
		if(typedefs.containsKey(name)){
			if(this.paThisIsModule && this.replModLevelForcedNewvars != null && replNameToRemoveAtEndOfSessionTYPEDEF.contains(name)) {
				return null;
			}
			
			return typedefs.get(name);
		}
		else if(parent != null){
			return parent.getTypeDef(name);
		}
		return null;
	}
	
	public Map<String, TypeDefTypeProvider> getAllTypeDefAtCurrentLevel(){
		return typedefs;
	}
	
	public void setTypeDef(String key, TypeDefTypeProvider value){
		
		if(this.paThisIsModule && this.replModLevelForcedNewvars != null) {
			replNameToRemoveAtEndOfSessionTYPEDEF.remove(key);
		}
		
		typedefs.put(key, value);
	}
	
	
	//name+siganure -> (name, signaure, source, dependancies)
	private List<GPUKernelFuncDetails> gPUFuncOrKernel = new ArrayList<GPUKernelFuncDetails>();
	
	public List<GPUKernelFuncDetails> getAllGPUFuncOrKernels(){
		return gPUFuncOrKernel;
	}
	
	public void addGPUFuncOrKernel(GPUKernelFuncDetails details){
		gPUFuncOrKernel.add(details);
	}
	
	
	
	
	
	
	/*public ArrayList<Fourple<String, Type, Boolean, AccessModifier>> getAllVariablesAtScopeLevel()	{
		return getAllVariablesAtScopeLevel(false);
	}*/
	
	public TypeAndLocation getVariable(TheScopeFrame requestor, String varname)
	{
		return getVariable(requestor, varname, true).getA();
	}
	public Pair<TypeAndLocation, Boolean> getVariable(TheScopeFrame requestor, String varname, boolean lookParent)
	{
		HashMap<String, TypeAndLocation> vars = this.vars;
		
		if(isRequestorScopeMeOrMyChild(requestor))
		{//apply sequential ordering if this scope is a class or module
			//so divert to special this instance only map
			vars = varsSelfRequestor;
		}
		
		if(vars.containsKey(varname))
		{
			
			if(this.replNameToRemoveAtEndOfSessionVARS != null && replNameToRemoveAtEndOfSessionVARS.contains(varname)) {
				return new Pair<TypeAndLocation, Boolean>(null, this.isClass);//nope
			}
			
			return new Pair<TypeAndLocation, Boolean>(vars.get(varname), this.isClass); 
		}
		else if(null != parent && lookParent)
		{
			return parent.getVariable(parent, varname, true);
		}
		else
		{
			return new Pair<TypeAndLocation, Boolean>(null, this.isClass);
		}
	}
	
	public HashMap<String, TypeAndLocation> getAllVars(TheScopeFrame requestor){
		HashMap<String, TypeAndLocation> vars = this.vars;
		
		if(isRequestorScopeMeOrMyChild(requestor)){
			vars = varsSelfRequestor;
		}
		
		return vars;
	}
	
	public boolean hasVariable(TheScopeFrame requestor, String varname, boolean lookParent, boolean ignoreAutoGeneratedvar, boolean isCreation)
	{
		TypeAndLocation ret = getVariable(requestor, varname, lookParent).getA();
		
		if(null == ret || ret.getType()==null)
		{
			return false;
		}
		else
		{
			if(!( ret.getType().getAutoGennerated() && ignoreAutoGeneratedvar)  ){
				if(isCreation && this.paThisIsModule && this.replModLevelForcedNewvars != null && replPrevSessionVars.contains(varname)) {
					return false;
				}
				
				return true;
			}
			return false;
		}
	}
	
	public void setVariable(TheScopeFrame requestor, String varname, TypeAndLocation var, boolean hasBeenAssigned, int level, Annotations annotations, boolean isAssignNew)
	{
		if(var != null){
			
			Location loc = var.getLocation();
			if(loc != null){
				loc.annotations = annotations;
			}
			
			vars.put(varname, var);
			vars.put(varname + "$n"+level, var);
			
			if(this.paThisIsModule && this.replModLevelForcedNewvars != null) {
				replNameToRemoveAtEndOfSessionVARS.remove(varname);
				
				replModLevelForcedNewvars.put(varname, var);
				if(isAssignNew) {
					replPrevSessionVars.remove(varname);
				}
				replAssignedthisIteration.add(varname);
			}
			
			if(isPersisted)
			{
				//copyLocationIfRelevant(varsSelfRequestor, varname, var);
				varsSelfRequestor.put(varname, var);
				//copyLocationIfRelevant(varsSelfRequestor, varname + "$n"+level, var);
				varsSelfRequestor.put(varname + "$n"+level, var);
			}
			
			if(hasBeenAssigned){
				setVariableAssigned(varname);
				setVariableAssigned(varname + "$n"+level);
			}
		}
		
	}
/*	
 * messes up cases where owner is redirected to NIC$ version
	private void copyLocationIfRelevant(HashMap<String, TypeAndLocation> varz, String varname, TypeAndLocation var){
		
		TypeAndLocation aalready = varz.get(varname);
		if(null != aalready && null != var.getLocation()){
			if(aalready.getLocation().getClass() ==  var.getLocation().getClass()){
				//var.setLocation(aalready.getLocation());//overwrite with existing one whihc may have redirection information
			}
		}
		
	}*/
	
	public void setVariable(String varname) {
		replAssignedthisIteration.add(varname);
		replNameToRemoveAtEndOfSessionVARS.remove(varname);
	}
	
	public void setVariableAssigned(String varname){
		asses.add(varname);
	}
	
	/*public void removeVariable(String varname){
		asses.remove(varname);
	}*/
	
	public boolean hasVariableAssigned(String varname){
		if(asses.contains(varname))
		{
			return true;
		}
		else if(null != parent)
		{
			return parent.hasVariableAssigned( varname);
		}
		return false;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////
	
	
	
	////////////////////////////////////////////////////////////////////////////////////////
	
	public ArrayList<ClassDef> getAllClasses()
	{
		ArrayList<ClassDef> ret = new ArrayList<ClassDef>();
		ret.addAll(this.classes.values());
		return ret;
	}
	
	public ClassDef getClassDef(TheScopeFrame requestor, String varname, boolean lookParent, boolean isCreation)
	{
		HashMap<String, ClassDef> classes = this.classes;
		
		if (isRequestorScopeMeOrMyChild(requestor)  && isCreation)//only modules have null parent
		{//apply sequential ordering if this scope is a module
			//so divert to special this instance only map
			classes = this.classesSelfRequestor;
		}
		//ss = new C.B()
		if(null == varname){
			return null;
		}
		int dotpos = varname.indexOf('.');
		//also: bytecodeSandbox.A -> A
		
		boolean startsWithpack = null != moduleName && varname.startsWith(moduleName) && !moduleName.equals(varname);
		
		if(dotpos == -1 || startsWithpack)
		{
			ArrayList<String> varTry = new ArrayList<String>();
			varTry.add(varname);
			
			if(startsWithpack){
				varname = varname.substring(moduleName.length()+1);
				varTry.add(varname);
			}
			
			for(String vntry : varTry) {
				if(classes.containsKey(vntry))
				{
					return classes.get(vntry);
				}
				else if(null != parent && lookParent)
				{
					return parent.getClassDef(parent, vntry, true, isCreation);
				}
			}
			
			
		}
		else
		{//uh oh, we need to drill into the class heirarchy (and we only need to do this with classes)
			//nested classes
			String firstPart = varname.substring(0,dotpos);
			String rest = varname.substring(dotpos+1);
			ClassDef soFar = null;
			if(classes.containsKey(firstPart))
			{
				soFar = classes.get(firstPart);
			}
			else if(null != parent && lookParent)
			{
				soFar= parent.getClassDef(parent, firstPart, true, isCreation);
			}
			if(null != soFar && (Object)soFar instanceof ClassDefJava)//only java does static nested classse
			{
				return soFar.getClassDef(rest);
			}
		}
		return null;
	}
	
	public boolean hasClassDef(TheScopeFrame requestor, String varname, boolean lookParent, boolean isCreation)
	{
		if(null == getClassDef(requestor, varname, lookParent, isCreation)) {
			return false;
		}
		
		if(isCreation && this.paThisIsModule && this.replModLevelForcedNewClasses != null && replPrevSessionClasses.containsKey(varname)) {
			return false;
		}
		
		return true;
	}
	
	public void setClassDef(TheScopeFrame requestor, String varname, ClassDef var, int modifier)
	{
		classes.put(varname, var);
		
		if(isPersisted)
		{
			classesSelfRequestor.put(varname, var);
		}
		
		if(this.paThisIsModule && this.replModLevelForcedNewClasses != null) {
			
			replModLevelForcedNewClasses.put(varname, var);
			replPrevSessionClasses.remove(varname);
		}
	}
	
	public void removeClassDef(String varname) {
		classes.remove(varname);
		if(isPersisted)
		{
			classesSelfRequestor.remove(varname);
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////
	
	////////////////////////////////////////////////////////////////////////////////////////
	/* Lambda currying function searching */
	
	
	public boolean hasLambdable(TheScopeFrame requestor/*, FunctionSearchLocationTracker funcSearchLocTracker*/, String varname, boolean lookParent, boolean extFuncs)
	{
		return null != getLambdable(requestor, /*funcSearchLocTracker, */varname, lookParent, extFuncs);
	}
	
	public Pair<Pair<Boolean /*is functions */, HashSet<TypeAndLocation>>, Boolean > getLambdable(TheScopeFrame requestor, String varname, boolean lookParent, boolean extFuncs)
	{
		//this can find: local defs/lambdas, class methods, outerclass methods, module level methods
		//NOT: superclass methods, imported methds
		
		HashMap<String, HashSet<TypeAndLocation>> funcs = this.funcs;
		HashMap<String, TypeAndLocation> vars = this.vars;
		
		if(isRequestorScopeMeOrMyChild(requestor))
		{//apply sequential ordering if this scope is a class or module
			//so divert to special this instance only map
			funcs = this.funcsSelfRequestor;
			vars = varsSelfRequestor;
		}
		
		if(funcs.containsKey(varname)){//priortize functions first, then vars
			HashSet<TypeAndLocation> items = funcs.get(varname);
			HashSet<TypeAndLocation> filtered = new HashSet<TypeAndLocation>();
			
			if(extFuncs){
				for(TypeAndLocation tal : items){
					Type got = tal.getType();
					if(got instanceof FuncType && ((FuncType)got).extFuncOn ){
						filtered.add(tal);
					}
				}
			}else{
				for(TypeAndLocation tal : items){
					Type got = tal.getType();
					if(got instanceof FuncType && ((FuncType)got).extFuncOn ){
						continue;
					}
					filtered.add(tal);
				}
			}
			
			if(filtered.isEmpty()){
				return null;
			}
			
			return new Pair<Pair<Boolean, HashSet<TypeAndLocation>>, Boolean>(new Pair<Boolean, HashSet<TypeAndLocation>>(true, filtered), this.isClass);
		}else if(vars.containsKey(varname)){
			TypeAndLocation tal = vars.get(varname);
			
			HashSet<TypeAndLocation> ret = new HashSet<TypeAndLocation>();
			ret.add(tal);
			return new Pair<Pair<Boolean, HashSet<TypeAndLocation>>, Boolean>(new Pair<Boolean, HashSet<TypeAndLocation>>(false, ret), this.isClass);
		}
		else if(null != parent && lookParent)
		{
			//funcSearchLocTracker.lookParentLogicalScope();
			return parent.getLambdable(requestor, /*funcSearchLocTracker*/ varname, true, extFuncs);
		}
		else
		{
			return null;
		}
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////
	
	
	
	////////////////////////////////////////////////////////////////////////////////////////
	public ArrayList<TypeAndLocation> getAllFunctionsAtScopeLevelNeedingAccessor(){
		ArrayList<TypeAndLocation> ret = new ArrayList<TypeAndLocation>();
		for(String funcName : this.funcs.keySet()){
			for(TypeAndLocation tal : this.funcs.get(funcName)){
				if(null != tal.getLocation().getPrivateStaticAccessorRedirectFuncGetter()){
					ret.add(tal);
				}
			}
		}
		return ret;
	}
	
	public HashSet<TypeAndLocation> getFuncDef(TheScopeFrame requestor, String varname)
	{
		return getFuncDef(requestor, varname, true).getA();
	}
	public Pair<HashSet<TypeAndLocation>, Boolean> getFuncDef(TheScopeFrame requestor, String varname, boolean lookParent)
	{
		HashMap<String, HashSet<TypeAndLocation>> vars = this.funcs;
		
		if(isRequestorScopeMeOrMyChild(requestor))
		{//apply sequential ordering if this scope is a class or module
			//so divert to special this instance only map
			vars = this.funcsSelfRequestor;
		}
		
		if(vars.containsKey(varname))
		{
			return new Pair<HashSet<TypeAndLocation>, Boolean>(vars.get(varname), this.isClass );
		}
		else if(null != parent && lookParent)
		{
			return parent.getFuncDef(requestor, varname, true);
		}
		else
		{
			return new Pair<HashSet<TypeAndLocation>, Boolean>(null, this.isClass );
		}
	}
	

	public Map<String, HashSet<TypeAndLocation>> getAllFunctions()
	{
		return this.funcs;
	}
	
/*	public Map<String, HashSet<TypeAndLocation>> getAllFunctionsIncParent()
	{
		Map<String, HashSet<TypeAndLocation>> ret = new HashMap<String, HashSet<TypeAndLocation>>();
		if(null != parent)
		{
			ret.putAll(parent.getAllFunctionsIncParent());
		}
		
		ret.putAll(this.funcs);
		return ret;
	}*/
	
	public boolean hasFuncDef(TheScopeFrame requestor, String varname, boolean lookParent, boolean ignoreAutogenenrated, boolean isCreation)
	{
		return ExisitsAlready.FALSE != this.hasFuncDef(requestor, varname, null, lookParent, ignoreAutogenenrated, false, false, isCreation);
	}
	
	private FuncType ftWithNoDefaults(FuncType sigCheck){
		sigCheck = sigCheck.copyTypeSpecific();
		int n=0;
		ArrayList<FuncParam> fps = sigCheck.origonatingFuncDef.params.params;
		ArrayList<Integer> toignore = new ArrayList<Integer>(fps.size());
		for(FuncParam fp : fps){
			if(fp.defaultValue != null){
				toignore.add(n);
			}
			n++;
		}
		
		Collections.reverse(toignore);
		for(int torm : toignore){
			sigCheck.inputs.remove(torm);
		}
		
		return sigCheck;
	}
	
	public ExisitsAlready hasFuncDef(TheScopeFrame requestor, String varname, FuncType sigCheck, boolean lookParent, boolean ignoreAutogenenrated, boolean ignoreReturn, boolean ignoreDefaultParams, boolean isCreation)
	{//yuck, string comparison...
		HashSet<TypeAndLocation> got = getFuncDef(requestor, varname, lookParent).getA();
		if(null != got && !got.isEmpty())
		{
			if(null == sigCheck)
			{
				return ExisitsAlready.TRUE;
			}
			else
			{
				if(ignoreDefaultParams){
					sigCheck = ftWithNoDefaults(sigCheck);
				}
				
				String mySig = sigCheck.toString();
				for (TypeAndLocation ga : got) {
					FuncType g = (FuncType)ga.getType(); 
					if (!(ignoreAutogenenrated && g.getAutoGennerated())) {
						if(ignoreDefaultParams){
							g = ftWithNoDefaults(g);
						}
						
						
						boolean exactMatch = ignoreReturn? sigCheck.equalsIngoreReturn(g): g.toString().startsWith(mySig);
						boolean isStubFunction = false;
						if(exactMatch && (g.origonatingFuncDef != null && sigCheck.origonatingFuncDef != null)) {//gpu params...
							
							boolean exactMatchStub = true;
							boolean exactMatchKernel = true;
							isStubFunction = g.origonatingFuncDef.isGPUStubFunction() || sigCheck.origonatingFuncDef.isGPUStubFunction(); 
							if(isStubFunction ) {
								int sz = g.origonatingFuncDef.params.params.size();
								for(int n=0; n < sz; n++) {
									FuncParam a = g.origonatingFuncDef.params.params.get(n);
									FuncParam b = sigCheck.origonatingFuncDef.params.params.get(n);
									
									if(a.gpuVarQualifier != b.gpuVarQualifier) {
										exactMatchStub=false;
										break;
									}
								}
							}
							
							if(g.origonatingFuncDef.isGPUKernalOrFunction == GPUFuncVariant.gpukernel || sigCheck.origonatingFuncDef.isGPUKernalOrFunction == GPUFuncVariant.gpukernel ){
								int sz = g.origonatingFuncDef.params.params.size();
								for(int n=0; n < sz; n++) {
									FuncParam a = g.origonatingFuncDef.params.params.get(n);
									FuncParam b = sigCheck.origonatingFuncDef.params.params.get(n);
									
									if((a.gpuVarQualifier != null) != (b.gpuVarQualifier != null)) {//global/local/const must be present or not
										exactMatchKernel=false;
										break;
									}
								}
								exactMatch = exactMatchStub && exactMatchKernel;
							}else {
								exactMatch = exactMatchStub;
							}
						}
						
						
						
						ExisitsAlready ret = null;
						if (exactMatch) {
							ret = g.isImplicit ? ExisitsAlready.TRUE_IMPLICIT : ExisitsAlready.TRUE;
						} else if (!isStubFunction && g.isSameWithGenericsErased(sigCheck)) {
							ret = ExisitsAlready.TRUE_GT_ERASED;
						}
						
						//fix here, also from create/replacer
						if(isCreation && this.paThisIsModule && this.replModLevelForcedNewfuncs != null) {
							if(replPrevSessionFuncs.containsKey(varname) && replPrevSessionFuncs.get(varname).contains(g.getErasedFuncTypeNoRet())) {
								ret=null;
							}
						}
						
						if(ret != null) {
							return ret;
						}
						
					}
				}
			}
		}
		return ExisitsAlready.FALSE;
	}
	
	public void replaceFuncDef(TheScopeFrame requestor, String varname, TypeAndLocation old, TypeAndLocation newLoc, int modifier){
		
		HashSet<TypeAndLocation> got = this.funcs.get(varname);
		got.remove(old);
		
		if(this.replPrevSessionFuncs != null) {//replace ignoring return type if in repl mode
			if(!got.isEmpty()) {
				Type tt = newLoc.getType();
				if(tt instanceof FuncType) {
					FuncType wanted = ((FuncType)tt).copyIgnoreReturnType();
					
					HashSet<TypeAndLocation> toRemove = new HashSet<TypeAndLocation>();
					for(TypeAndLocation tal : got) {
						Type ttx = tal.getType();
						if(ttx instanceof FuncType) {
							FuncType ft = (FuncType)ttx;
							if(wanted.equals(ft.copyIgnoreReturnType())) {
								toRemove.add(tal);
							}
						}
					}
					got.removeAll(toRemove);
				}
			}
		}
		
		if(isPersisted)
		{
			HashSet<TypeAndLocation> got2 = this.funcsSelfRequestor.get(varname);
			if(null != got2) {
				got2.remove(old);
			}
		}
		
		setFuncDef(requestor, varname, newLoc, modifier);
	}
	
	public void setFuncDef(TheScopeFrame requestor, String varname, TypeAndLocation var, int modifier)
	{
		HashSet<TypeAndLocation> got = this.funcs.get(varname);
		if(got == null)
		{
			got = new HashSet<TypeAndLocation>();
			this.funcs.put(varname, got);
		}
		got.add(var);
		
		
		if(this.paThisIsModule && this.replModLevelForcedNewfuncs != null) {
			FuncType ft = ((FuncType)var.getType()).getErasedFuncTypeNoRet();
			{
				HashSet<FuncType> addTo;
				if(!replModLevelForcedNewfuncs.containsKey(varname)) {
					addTo = new HashSet<FuncType>();
					replModLevelForcedNewfuncs.put(varname, addTo);
				}else {
					addTo = replModLevelForcedNewfuncs.get(varname);
				}
				addTo.add(ft);
			}
			
			{
				if(replPrevSessionFuncs.containsKey(varname)) {
					HashSet<FuncType> items = replPrevSessionFuncs.get(varname);
					items.remove(ft);
					if(items.isEmpty()) {
						replPrevSessionFuncs.remove(varname);
					}
				}
			}
		}
		
		
		if(isPersisted)
		{
			HashSet<TypeAndLocation> got2 = this.funcsSelfRequestor.get(varname);
			if(got2 == null)
			{
				got2 = new HashSet<TypeAndLocation>();
				this.funcsSelfRequestor.put(varname, got2);
			}
			got2.add(var);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////
	
	
	////////////////////////////////////////////////////////////////////////////////////////
	public HashSet<FuncType> getConstructor(TheScopeFrame requestor )
	{
		if(isRequestorScopeMeOrMyChild(requestor))
		{//apply sequential ordering if this scope is a class or module
			//so divert to special this instance only map
			return this.consSelfRequestor;
		}
		else
		{
			return this.cons;
		}
	}
	
	public ExisitsAlready hasConstructor( TheScopeFrame requestor, FuncType sigCheck, boolean ignoreAutogenenrated, boolean ignoreDefaultParams)
	{//1==1 match
		HashSet<FuncType> cons = getConstructor(requestor);
		
		if(null != cons && !cons.isEmpty())
		{
			if(null == sigCheck)
			{
				return ExisitsAlready.TRUE;
			}
			else
			{
				String sigString = sigCheck.toString();
				
				if(ignoreDefaultParams){
					sigCheck = ftWithNoDefaults(sigCheck);
				}
				
				for(FuncType g : cons)
				{
					if(!(ignoreAutogenenrated && g.getAutoGennerated()))
					{
						
						if(ignoreDefaultParams){
							g = ftWithNoDefaults(g);
						}
						
						if(g.toString().equals(sigString))
						{
							return ExisitsAlready.TRUE;
						}
						else if(g.isSameWithGenericsErased(sigCheck))
						{
							return ExisitsAlready.TRUE_GT_ERASED;
						}
					}
				}
			}
		}
		return ExisitsAlready.FALSE;
	}
	
	public void setConstructor(FuncType car)
	{//TODO: public private etc constructors
		
		//remove car without the synthetic args
		
		this.cons.remove(car);//MHA: we remove because init(MyGenType -as Named -) == init(MyGenType -Gegneric Type-)//probably should not be equal but this hack gets around it..
		//FuncType carNonSynth = funcTypeWithoutSyntheticInputArgs(car);
		
		/*if(!carNonSynth.equals(car)){
			int h=8;
		}
		*/
		//this.cons.remove(carNonSynth);//same probem as above?
		this.cons.add(car);
		
		if(isPersisted)
		{
			HashSet<FuncType> addTo = this.consSelfRequestor;
			if(addTo == null)
			{
				addTo = new HashSet<FuncType>();
			}
			this.consSelfRequestor.add(car);
		}
	}
	
	
	
	
	
	
	////////////////////////////////////////////////////////////////////////////////////////
	
	public Object getResource(ITEM_TYPE type, String dottedName, boolean isChildClassAsking) {
		// TODO Auto-generated method stub
		/*
		String[] bits = dottedName.split("\\.");
		
		TheScopeFrame currentSF = this;
		
		for(int n=0; n <= bits.length; n++)
		{
			String bit = bits[n];
			ClassDef cls = currentSF.getClassDef(currentSF, bit, false);
			if(cls==null)
			{
				return null;
			}
			currentSF = cls.getScopeFrame();
			if(currentSF == null)
			{
				return null;
			}
		}
		*/
		//concurnas only supports top level statics
		//this should only return static stuff ,oops
		
		//JPT: not 100% optimal cos if dot then cannot be a func can it?
		if(!dottedName.contains("."))
		{
			if(type == ITEM_TYPE.FUNC)
			{
				return this.getFuncDef(null, dottedName, false);
			}
			else if (type == ITEM_TYPE.VARIABLE)
			{
				return this.getVariable(null, dottedName, false);
			}
			else if (type == ITEM_TYPE.NESTED_CLASS || type == ITEM_TYPE.STATIC_CLASS)
			{
				return this.getClassDef(null, dottedName, false, false);
			}
			else if(type == ITEM_TYPE.TYPEDEF){
				return this.typedefs.get(dottedName);
			}
		}
		else
		{//dotted... so drill into it
			String[] bits = dottedName.split("\\.");
			
			String firstBit = bits[0];
			ClassDef cls = this.getClassDef(null, firstBit, false, false);
			
			if(null != cls)
			{
				TheScopeFrame frame = cls.getScopeFrame();
				if(null != frame)
				{
					String restPastFirstBit = StringUtils.join(bits, 1);
					return frame.getResource(type, restPastFirstBit, isChildClassAsking);
				}
			}
		}
		return null;//we dont do static classes
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("childs: %s \n", this.children.size()));
		
		for(String name : this.vars.keySet())
		{
			sb.append(String.format("var %s %s\n", name, this.vars.get(name)));
		}
		
		for(FuncType name : this.cons)
		{
			sb.append(String.format("constructor %s\n", name));
		}
		
		for(String name : this.funcs.keySet())
		{
			sb.append(String.format("def %s\n", name));
		}
		
		for(String name : this.classes.keySet())
		{
			sb.append(String.format("class %s\n", name));
		}
		
		return sb.toString();
	}

	public void removeVariable(String name){
		if(this.vars.containsKey(name)){
			
			if(this.replModLevelForcedNewvars == null) {
				this.vars.remove(name);
			}else if(null != this.replNameToRemoveAtEndOfSessionVARS){
				this.replNameToRemoveAtEndOfSessionVARS.add(name);
			}
			
			this.varsSelfRequestor.remove(name);
		}else if(this.replModLevelForcedNewvars == null){
			parent.removeVariable(name);
		}
		
	}
	
	
	/*public void removeFuncDef(String name, FuncType signature) {
		removeFuncDef(name, signature, false);
	}*/
	
	public void removeFuncDef(String name, FuncType signature/*, boolean ignoreGensAndRetType*/) {
		
		if(name.equals("<init>")){
			removeConstructorDef(signature);
			return;
		}
		
		boolean removeAllAtModLevel = signature == null;
		
		if(!removeAllAtModLevel && this.replModLevelForcedNewfuncs != null) {//we can overwrite a functioin defintion from a previous incarnation when in REPL mode
			signature = signature.copyIgnoreReturnType();
		}
		
		
		{
			if(removeAllAtModLevel) {
				if(null != this.replNameToRemoveAtEndOfSessionFUNCS){
					this.replNameToRemoveAtEndOfSessionFUNCS.add(name);
				}
			}else {
				HashSet<TypeAndLocation> choices = this.funcs.get(name);
				if(null != choices){
					for(TypeAndLocation tal : choices){
						Type tt = tal.getType();
						if(this.replModLevelForcedNewfuncs != null) {
							tt = tt.copyIgnoreReturnType();
						}
						
						if(tt.equals(signature)){
							choices.remove(tal);
							if(choices.isEmpty()) {
								this.funcs.remove(name);
							}
							break;
						}
					}
				}
			}
		}
		
		{//JPT: optimization here, if above is null then return? which one, one cannot be null without the other hmm, i forget which is more strict
			if(removeAllAtModLevel) {
				this.funcsSelfRequestor.remove(name);
			}else {
				HashSet<TypeAndLocation> choices = this.funcsSelfRequestor.get(name);
				if(null != choices){
					for(TypeAndLocation tal : choices){
						
						Type tt = tal.getType();
						if(this.replModLevelForcedNewfuncs != null) {
							tt = tt.copyIgnoreReturnType();
						}
						
						if(tt.equals(signature)){
							choices.remove(tal);
							if(choices.isEmpty()) {
								this.funcsSelfRequestor.remove(name);
							}
							break;
						}
					}
				}
			}
		}
	}
	
	public void removeConstructorDef(FuncType signature) {
		{
			HashSet<FuncType>   choices = this.cons;
			if(null != choices && choices.contains(signature)){
				choices.remove(signature);
			}
		}
		
		{//JPT: optimization here, if above is null then return? which one, one cannot be null without the other hmm, i forget which is more strict
			HashSet<FuncType>   choices = this.consSelfRequestor;
			if(null != choices && choices.contains(signature)){
				choices.remove(signature);
			}
		}
	}
	
	
	public String getFormalLocation() {
		String par = this.parent !=null ? this.parent + "." : ""; 
		return par + this.toString();
	}

	private HashSet<WarningVariant> suppressedWarnings = new HashSet<WarningVariant>();
	public boolean isExtFunc=false;
	public boolean isFuncDefBlock=false;
	public FuncDef funcDef;
	public GPUKernelFuncDetails gpuKernelFuncDetails;
	
	public void setSuppressedWarnings(HashSet<WarningVariant> variants){
		suppressedWarnings=variants;
	}
	
	public void addSuppressedWarnings(HashSet<WarningVariant> variants){
		suppressedWarnings.addAll(variants);
	}
	
	public HashSet<WarningVariant> getSupressedWarnings() {
		HashSet<WarningVariant> ret = new HashSet<WarningVariant>(this.suppressedWarnings);
		if(null != parent){
			ret.addAll(this.parent.getSupressedWarnings());
		}
		
		return ret;
	}

	public HashSet<String> getAllItemsDeleted(){
		HashSet<String> ret = new HashSet<String>();
		ret.addAll(replNameToRemoveAtEndOfSessionVARS);
		ret.addAll(replNameToRemoveAtEndOfSessionFUNCS);
		ret.addAll(replNameToRemoveAtEndOfSessionCLASSES);
		ret.addAll(replNameToRemoveAtEndOfSessionTYPEDEF);
		return ret;
	}
	
	public void cleanUpAtEndOfREPLCycle() {
		for(String name : replNameToRemoveAtEndOfSessionVARS) {
			this.vars.remove(name);
		}
		
		for(String name : replNameToRemoveAtEndOfSessionFUNCS) {
			this.funcs.remove(name);
		}
		
		for(String name : replNameToRemoveAtEndOfSessionCLASSES) {
			this.classes.remove(name);
		}
	}

	public void removeTypeDefREPL(String varname) {
		this.replNameToRemoveAtEndOfSessionTYPEDEF.add(varname);
		this.typedefs.remove(varname);
	}

	public void delREPLTolLevelElement(String toRemove) {
		this.replPrevSessionClasses.remove(toRemove);
		this.replPrevSessionFuncs.remove(toRemove);
		this.replPrevSessionVars.remove(toRemove);
		
		this.replNameToRemoveAtEndOfSessionCLASSES.add(toRemove);
		this.replNameToRemoveAtEndOfSessionFUNCS.add(toRemove);
		this.replNameToRemoveAtEndOfSessionTYPEDEF.add(toRemove);
		this.replNameToRemoveAtEndOfSessionVARS.add(toRemove);
	}
}
