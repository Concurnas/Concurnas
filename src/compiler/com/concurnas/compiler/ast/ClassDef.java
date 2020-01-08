package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.misc.OrderedHashSet;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.bytecode.FuncLocation.ClassFunctionLocation;
import com.concurnas.compiler.constants.UncallableMethods;
import com.concurnas.compiler.typeAndLocation.Location;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.Fiveple;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.utils.GenericTypeUtils;
import com.concurnas.compiler.utils.Sevenple;
import com.concurnas.compiler.utils.Sixple;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.visitors.ErrorRaiseable;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.compiler.visitors.Unskippable;
import com.concurnas.compiler.visitors.Utils;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.compiler.visitors.datastructs.TheScopeFrame;
import com.concurnas.compiler.visitors.lca.ClassDefTree;
import com.concurnas.compiler.visitors.util.ErrorRaiseableSupressErrors;
import com.concurnas.compiler.visitors.util.VarAtScopeLevel;
import com.concurnas.runtime.Pair;

public class ClassDef extends CompoundStatement implements ClassDefI, AttachedScopeFrame, HasAnnotations, REPLTopLevelComponent {

	public String className;
	public String packageName = "xxx.package.xxx"; //TODO: implement package tracking
	public boolean isObject;
	public ArrayList<GenericType> classGenricList = new ArrayList<GenericType>();
	public String superclass;
	public List<Type> superClassGenricList; //extends
	public ArrayList<Expression> superClassExpressions = new ArrayList<Expression>(); //class Child1(x int, y int) extends Parent1(x,y)
	public Map<Type, Type> superclassTypeToClsGeneric = new HashMap<Type, Type>();
	
	
	public Block classBlock;
	public ClassDefArgs classDefArgs;
	public Map<String, GenericType> nameToGenericMap = new HashMap<String, GenericType>();
	private ClassDef parentNestor = null; // class Nestor { class Inner{}} //Inner has parentNestor := Nestor
	private boolean isAbstract=false;
	public boolean isFinal=false;
	public String isFinalDefined=null;//fiddle in scopeandTypechecker
	public boolean javaSystemLib;
	public boolean isActor = false;
	public boolean isGennerated = false;

	//actor
	public NamedType typedActorOn;
	public boolean isAnonClass;
	public boolean istypedActor;
	public boolean needsActorFunctionsTobeAdded = true;
	public ArrayList<Expression> acteeClassExpressions;
	
	//enum
	public boolean isEnumSubClass;
	
	//annotation
	public boolean isAnnotation;
	
	//
	private boolean hasAbstractMethods = false;
	public AccessModifier accessModifier = AccessModifier.PUBLIC;
	//
	private HashMap<Pair<String, FuncType>, LineHolder> prependedStuff = new HashMap<Pair<String, FuncType>, LineHolder>();
	public Annotations annotations;

	//mixins
	public ArrayList<ImpliInstance> traits = new ArrayList<ImpliInstance>();

	public boolean hasNestedChildren = false;
	public boolean isEnum = false;
	//public ArrayList<ClassDef> resolvedIfaces;
	public FuncType isActorOfClassRef = null;
	public boolean isLocalClass=false;
	public Set<New> callingConstructors;
	public Set<ClassDef> classesHavingMeAsTheSuperClass;
	public Set<SuperConstructorInvoke> localsuperInvokations;
	public Set<RefName> callingfuncRefsAsRefNames;
	public boolean isLocalClassDef = false;
	public boolean isShared = false;
	public boolean isTransient = false;
	public boolean isTrait = false;
	public HashSet<Pair<String, FuncType>> traitSuperRefs = new  HashSet<Pair<String, FuncType>>();//e.g. public abstract int traitSuper$fullpackageName$classname$methodName(sig);
	public List<Fourple<String, String, Boolean, TypeAndLocation>> traitSuperRefsImpls = new ArrayList<Fourple<String, String, Boolean, TypeAndLocation>>(0); 
	public List<Thruple<String, Type, Type>> traitVarsNeedingImpl = null;//name, expected, definedAs
	public List<TypeAndLocation> addMethodsToTraits;
	public ArrayList<Pair<String, Type>> traitVarsNeedingFieldDef;
	
	//class A[A,B](-a A, -b B) {}//not valid as A is the clas name and already declared
	
	public ClassDef(int line, int col, AccessModifier accessModifier, boolean isObject, boolean isActor, String className,
			ArrayList<Pair<String, NamedType>> classGenricList, 
			ClassDefArgs classDefArgs,
			String superclass, ArrayList<Type> superClassGenricList, ArrayList<Expression> superClassExpressions, 
			ArrayList<ImpliInstance> impls,
			Block classBlock,
			boolean isAbstract, String isFinalDefined, NamedType typedActorOn, boolean istypedActor, ArrayList<Expression> acteeClassExpressions, 
			boolean isTransient,
			boolean isShared,
			boolean isTrait) {
		this(line, col);
		this.isObject = isObject;
		this.isActor = isActor;
		this.accessModifier  = accessModifier;
		this.className = className;
		setGenericList(classGenricList);
		this.classDefArgs = classDefArgs;
		this.superclass = superclass;
		this.superClassGenricList = superClassGenricList;
		this.superClassExpressions = superClassExpressions;
		this.traits = impls;
		this.classBlock = classBlock;
		this.isAbstract = isAbstract;
		this.isFinalDefined = isFinalDefined;
		this.typedActorOn = typedActorOn;
		this.istypedActor = istypedActor;
		this.acteeClassExpressions = acteeClassExpressions;
		this.isTransient  = isTransient;
		this.isShared  = isShared;
		this.isTrait  = isTrait;
	}
	
	/*public ClassDef typeCopy() {
		//copy for purposes of untagging as a local class
	}*/

	@Override
	public Node copyTypeSpecific() {
		return this;//only one...
	}
	private Expression preceedingExpression;
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
	
	/**
	 * minimal needed so u can do lambdas etc
	 */
	public ClassDef(int line, int col, String className, String packageName){
		this(line, col);
		this.className = className;
		this.packageName = packageName;
		bcFullName = (( !this.packageName.equals("") ? this.packageName + "." : "") + this.className).replace('.', '/');
	}
	
	protected ClassDef(int line, int col)
	{
		super(line, col, true);
	}
	
	@Override
	public void setAnnotations(Annotations annotations) {
		this.annotations=annotations;
	}
	
	@Override
	public Annotations getAnnotations(){
		return annotations;
	}
	
	
	public String bcShortName()
	{
		return this.className;
	}
	
	public String getClassName(){
		return this.className;
	}
	
	private String bcFullName;
	public void setCcFullName(String bcFullName) {
		this.bcFullName = bcFullName;
	}
	
	public String bcFullName()
	{
		return bcFullName;
	}
	
	
	
	
	private void setGenericList(ArrayList<Pair<String, NamedType>> classGenricList)
	{//TODO: add upper bound restriuctions class A[X extends List] etc -> A[X <: List]
		this.classGenricList = new ArrayList<GenericType>(classGenricList.size());
		int n=0;
		for(Pair<String, NamedType> nameandType: classGenricList){
			String name = nameandType.getA();
			GenericType gen = new GenericType(this.getLine(), this.getColumn(), name, n++);
			this.classGenricList.add(gen );
			NamedType upperBound = nameandType.getB();
			if(null != upperBound) {
				gen.upperBound = upperBound;
				gen.setNullStatus(upperBound.getNullStatus());
			}
			nameToGenericMap.put(name, gen);
		}
	}

	public NamedType getSuperAsNamedType(int line, int col){
		return getFullyqualifiedSuperClassRef(line, col);
	}

	private HashSet<TypeAndLocation> getFuncDefsForClassWithGenMapping(String funcName, boolean colorGenericTypes, ClassDef sup, Map<Type, Type> genMappiong){
		if (null == sup) {
			return null;
		}
		HashSet<TypeAndLocation> superFuncsfuncs = sup.getFuncDef(funcName, true, false);
		if (null == superFuncsfuncs) {
			return null;
		}

		HashSet<TypeAndLocation> ret = new HashSet<TypeAndLocation>();

		for (TypeAndLocation fa : superFuncsfuncs) {
			FuncType f = (FuncType) fa.getType();
			if (colorGenericTypes) {
				f = (FuncType) GenericTypeUtils.mapFuncTypeGenericsToOtherGenerics(f, genMappiong, false);
				fa = new TypeAndLocation(f, fa.getLocation());
			}
			ret.add(fa);
		}

		return ret;
	}

	public HashSet<TypeAndLocation> getSuperClassFuncDefsMatchingName(String funcName, boolean colorGenericTypes) {
		return getFuncDefsForClassWithGenMapping(funcName, colorGenericTypes, this.getSuperclass(), this.superclassTypeToClsGeneric);
	}
	
	public HashSet<TypeAndLocation> getTraitFuncDefsMatchingName(String funcName, boolean colorGenericTypes) {
		HashSet<TypeAndLocation> ret = new HashSet<TypeAndLocation>();
/*		for(Tuple<ClassDef, List<Type>> traitisnt : this.getTraitsMapGens(true)) {
			ret.addAll(getFuncDefsForClassWithGenMapping(funcName, colorGenericTypes, traitisnt.getA(), traitisnt.getB()));
		}
*/		for(ImpliInstance impli : this.traits) {
			ret.addAll(getFuncDefsForClassWithGenMapping(funcName, colorGenericTypes, impli.resolvedIface, impli.iffaceTypeToClsGeneric));
		}
		return ret;
	}

	
	
	public List<Pair<String, TypeAndLocation>> getAllLocallyDefinedMethods()
	{
		ArrayList<Pair<String, TypeAndLocation>> ret = new ArrayList<Pair<String, TypeAndLocation>>();
		if(null != this.myScopeFrame)
		{
			Map<String, HashSet<TypeAndLocation>> mmap = this.myScopeFrame.getAllFunctions();
			for(String name : mmap.keySet()){
				for(TypeAndLocation tt : mmap.get(name)){
					ret.add(new Pair<String, TypeAndLocation>(name, tt));
				}
			}
		}
		
		return ret;
	}
	
	protected Map<String, HashSet<TypeAndLocation>> getAllMethodsPlusNestorParent(){
		Map<String, HashSet<TypeAndLocation>> ret = new HashMap<String, HashSet<TypeAndLocation>>();
		ClassDef par = this.getSuperclass();
		if(par != null){
			ret.putAll(par.getAllMethodsPlusNestorParent());
		}
		ret.putAll(this.myScopeFrame.getAllFunctions());
		//child is more specific, even though its not possible to do this in real code, probably makes sense to use most specific for error handle etc (will have already thrown an error)
		return ret;
	}
	
	public List<Thruple<String, FuncType, Boolean>> getAllAnnotationMethods(){
		//name, type, hasdefualt
		List<Thruple<String, FuncType, Boolean>> ret = new ArrayList<Thruple<String, FuncType, Boolean>>();
		
		for(LineHolder lh : this.classBlock.lines){
			try{
				if(lh.l instanceof Assign){
					boolean hasDefault;
					Type retType;
					String name;
					
					Line lin = lh.l;
					
					if(lin instanceof DuffAssign){
						if(((DuffAssign)lin).e instanceof ExpressionList){
							Node astre = ((ExpressionList)((DuffAssign)lin).e).astRedirect;
							if(null != astre && astre instanceof AssignNew){
								lin = (AssignNew)astre;
							}
							
						}
					}
					
					if(lin instanceof AssignNew){
						AssignNew asnew = (AssignNew)lin;
						name = asnew.name;
						hasDefault = null != asnew.expr;
						retType=asnew.type;
					}
					else{// if(lh.l instanceof AssignExisting){
						hasDefault=true;//must do
						retType=lin.getTaggedType();
						name = ((RefName)((AssignExisting)lin).assignee).name;
					}
					ret.add(new Thruple<String, FuncType, Boolean>(name, new FuncType(retType), hasDefault));
				}
			}
			catch(Exception e){
				//meh lazy
			}
		}
		
		return ret;//CompiledClassUtils.getAllAnnotationMethods(this.cls);
	}
	
	public final List<Pair<String, TypeAndLocation>> getAllMethods(boolean filterOutAbstract)
	{
		ArrayList<Pair<String, TypeAndLocation>> ret = new ArrayList<Pair<String, TypeAndLocation>>();
		/*if(null != this.myScopeFrame)
		{*/
			Map<String, HashSet<TypeAndLocation>> mmap = this.getAllMethodsPlusNestorParent();
			for(String name : mmap.keySet()){
				for(TypeAndLocation tt : mmap.get(name)){
					if(filterOutAbstract) {
						Type tta = tt.getType();
						if(tta instanceof FuncType) {
							FuncType ft = (FuncType)tta;
							if(ft.isAbstarct()) {
								continue;
							}
							
						}
					}
					
					ret.add(new Pair<String, TypeAndLocation>(name, tt));
				}
			}
		//}
		
		return ret;
	}
	
	public List<Fiveple<String, ClassDef, Type, Boolean, AccessModifier>> getTraitFields(Map<Type, Type> superGenToChildGenBinding){
		HashMap<Pair<String, Type>, Thruple<Boolean, AccessModifier, ClassDef>> retx = new HashMap<Pair<String, Type>, Thruple<Boolean, AccessModifier, ClassDef>>();
		
		for(Sevenple<String, Type, Boolean, AccessModifier, ClassDef, Boolean, String> inst : getAllFields(true)) {//list order: supertype, trait then own type, so ok to write over
			Type tt = inst.getB();
			Type mapped =  GenericTypeUtils.mapFuncTypeGenericsToOtherGenerics(tt, superGenToChildGenBinding, false);
			retx.put(new Pair<String, Type>(inst.getA(), mapped), new Thruple<Boolean, AccessModifier, ClassDef>(inst.getC(), inst.getD(), inst.getE()));
		}
		
		
		
		List<Fiveple<String, ClassDef, Type, Boolean, AccessModifier>> ret = new ArrayList<Fiveple<String, ClassDef, Type, Boolean, AccessModifier>>();
		
		for(Pair<String, Type> item : retx.keySet()) {
			Thruple<Boolean, AccessModifier, ClassDef> pair = retx.get(item);
			ret.add(new Fiveple<String, ClassDef, Type, Boolean, AccessModifier>(item.getA(), pair.getC(), item.getB(), pair.getA(), pair.getB()));
		}
		
		return ret;
	}
	
	
	public List<Pair<String, TypeAndLocation>> getAbstractMethods(Map<Type, Type> superGenToChildGenBinding)
	{
		List<Pair<String, TypeAndLocation>> ret = new ArrayList<Pair<String, TypeAndLocation>>();
		
		for(Pair<String, TypeAndLocation> can : getAllLocallyDefinedMethods())
		{
			TypeAndLocation tandL = can.getB();
			FuncType fun = (FuncType)tandL.getType();
			if(fun.isAbstarct())
			{
				//map generics properly
				FuncType mapped =  (FuncType)GenericTypeUtils.mapFuncTypeGenericsToOtherGenerics(fun, superGenToChildGenBinding, false);
				ret.add(new Pair<String, TypeAndLocation>(can.getA(), new TypeAndLocation(mapped, tandL.getLocation())));
			}
		}
		
		
		//also we add the ones from the parent
		
		if(null != this.resolvedSuperType)
		{//filter out with the stuff that gets qualified
			List<Pair<String, TypeAndLocation>> parent = this.resolvedSuperType.getAbstractMethods(this.superclassTypeToClsGeneric);
			for(Pair<String, TypeAndLocation> pk : parent )
			{//JPT: replace with hashmap
				TypeAndLocation pkTandA = pk.getB();
				FuncType pk2 =  (FuncType)GenericTypeUtils.mapFuncTypeGenericsToOtherGenerics((FuncType)pkTandA.getType(), superGenToChildGenBinding, false);
				Pair<String, TypeAndLocation> cando = new Pair<String, TypeAndLocation>(pk.getA(), new TypeAndLocation(pk2, pkTandA.getLocation()) );
				
				if(!ret.contains(cando))
				{
					ret.add(cando);
				}
			}
		}
		
		return ret;
	}
	
	
	public boolean isGeneric()
	{
		return this.classGenricList != null && !classGenricList.isEmpty();
	}
	
	public ArrayList<GenericType> getClassGenricList()
	{
		return this.classGenricList;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		if(this.canSkipIterativeCompilation && !(visitor instanceof Unskippable)) {
			return null;
		}

		if(visitor instanceof ScopeAndTypeChecker) {
			this.hasErrors = false;
		}
		visitor.pushErrorContext(this);
		Object ret = visitor.visit(this);
		visitor.popErrorContext();
		return ret;
	}

	@Override
	public String getPrettyName() {
		return this.toString();
	}
	
	//After initial AST scan...
	
	public int getConstructorCount()
	{
		return this.myScopeFrame.getConstructor(null).size();
	}
	
	/*public int getNonAutoGenneratedConstructorCount()
	{
		HashSet<FuncType> ret = new HashSet<FuncType>();
		
		for(FuncType con: this.myScopeFrame.getConstructor(myScopeFrame)){
			if(con.origonatingFuncDef.isAutoGennerated){
				ret.add(con);
			}
		}
		
		return ret.size();
	}*/
	
	/*
	public void addDefaultConstructor() {
		FuncType toADd = new FuncType(new ArrayList<Type>(), null);
		toADd.isAutoGen = true;

		this.myScopeFrame.setConstructor(toADd, Modifier.PUBLIC);
	}
	*/
	
	public HashSet<FuncType> getAllConstructors()
	{
		return this.myScopeFrame.getConstructor(null);
	}
	
	public HashSet<FuncType> getConstructor(int argCountMatch, ErrorRaiseableSupressErrors invoker)
	{
		HashSet<FuncType> cons = getAllConstructors();
		
		HashSet<FuncType> matchingset = new HashSet<FuncType>();
		
		for(FuncType candidcateCon : cons)
		{
			List<Type> conargs = candidcateCon.getInputs();
			if(conargs.size() == argCountMatch)
			{
				matchingset.add(candidcateCon);
			}
		}
		
		return matchingset;
	}
	
	public boolean hasNoArgConstructor(ScopeAndTypeChecker sac)
	{
		if(TypeCheckUtils.isActor(sac, new NamedType(this))){
			HashSet<FuncType> oneargcons = getConstructor(1, null);
			if(!oneargcons.isEmpty()){
				for(FuncType con : oneargcons){
					return con.inputs.get(0).equals(ScopeAndTypeChecker.const_classArray_nt_array);
				}
			}
			return false;
		}
		else{
			return !getConstructor(0, null).isEmpty();
		}
	}
	
	public boolean hasAbstractMethods()
	{
		return this.hasAbstractMethods;
	}
	
	public void setHasAbstractMethods()
	{
		hasAbstractMethods = true;
	}
	
	public boolean isInstantiable()
	{
		return !this.isAbstract;//TODO: intefaces
	}
	
	public boolean isInterface()
	{
		return this.isTrait;//TODO: interfaces
	}
	
	public boolean isfinal(){
		return !this.isTrait && this.isFinal; 
	}
	
	
	//default resolved supertype is Object
	private static final ClassDef obj = new ClassDefJava(java.lang.Object.class);
	public ClassDef resolvedSuperType = obj;
	private ArrayList<ClassDef> resolvedInterfaces = new ArrayList<ClassDef>();
	
	public void setResolvedSuperType(ErrorRaiseable err, ClassDef resolvedSuperType)
	{
		this.resolvedSuperType = resolvedSuperType;
		//we also remap generic types of supertype if any to the generic types of this class.
		//GenericTypeUtils.repointGenericReferencesInSuperClass(super.line, super.column, err, superClassGenricList, this.nameToGenericMap);
	}	
	
	public void setResolvedInterfaces(ArrayList<ClassDef> resolvedInterfaces)
	{
		this.resolvedInterfaces  = resolvedInterfaces;
	}
	
	public ArrayList<ClassDef> getResolvedInterfaces(){
		return this.resolvedInterfaces;
	}
	
	
	
	public boolean isBeingBeingPastTheParentOfMe(ClassDef lhsParent)
	{
		//oops wont work on concurnas non compiled classes? move this to ClassDefJava, and have own one here
		
		if(lhsParent == null) {
			return false;
		}
		
		ClassDefTree cdt = new ClassDefTree(new NamedType(lhsParent.getLine(), lhsParent.getColumn(), this));
		return cdt.isThingPassedParent(lhsParent);
		
		/*
		//is the thing being passed in a parent of this guy?
		//if parent is superclass or interface
		if(lhsParent.getPrettyName().equals(this.getPrettyName()))
		{
			return true;
		}
		else
		{
			///lhs may be an interface????
			
			if(this.getPrettyName().equals("java.lang.Object"))
			{
				return false;
			}
			else
			{
				return this.resolvedSuperType.isParent(lhsParent);
			//bug here .. yeah interfaces check
			}
		}
		//TODO: interfaces check
		 * */
	}

	/*
	public ArrayList<GenericType> getGenericParameters() //TODO: change this to List<ClassDef> when generic type demands are imposed
	{
		return this.genericTypeDeclarations;
	}
	*/
	
	@Override
	public boolean isChangeable() {
		return false; //TODO: is immmuatable?
	}

	
	public ClassDef getSuperclass() {
		if(null == this.resolvedSuperType)
		{
			return obj;
		}
		else
		{
			return this.resolvedSuperType;
		}
	}
	
	@Override
	public HashSet<ClassDef> getTraitsIncTrans() {
		HashSet<ClassDef> ret = getTraits();
		ret.addAll(this.getSuperclass().getTraitsIncTrans());
		return ret;
	}
	
	@Override
	public HashSet<ClassDef> getTraits() {
		HashSet<ClassDef> ret = new OrderedHashSet<ClassDef>();
		
		if(!this.traits.isEmpty()) {
			for(ImpliInstance impi : this.traits) {
				if(null != impi.resolvedIface) {
					ret.add(impi.resolvedIface);
				}
			}
		}
		
		return ret;
	}
	
	public List<NamedType> getAllSuperClassesInterfaces()
	{
		List<NamedType> ret = new ArrayList<NamedType>();
		
		NamedType nt = getSuperAsNamedType(this.getLine(), this.getColumn());
		if(null != nt){
			ret.add(nt);
		}
		
		//add interfaces
		for(NamedType i : getTraitsAsNamedType(true, this.getLine(), this.getColumn())){
			ret.add(i);
		}
		
		return ret;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof ClassDef)
		{
			return ((ClassDef)o).getPrettyName().equals(this.getPrettyName());
		}
		return false;
	}
	
	 @Override
    public int hashCode() {
        return this.getPrettyName().hashCode();
    }

	 private TheScopeFrame myScopeFrame = null;
	public ArrayList<Pair<FuncDef, FuncDef>> bridgeMethodsToAdd = new ArrayList<Pair<FuncDef, FuncDef>>(0);
	public List<NamedType> linearizedTraitsInitCalls;
	public boolean permitGenericInterOfTypedArg=true;
	public Set<NamedType> typedActorOnImplicitIfaces;
	public Boolean injectClassDefArgsConstructor=false;
	//public boolean constructorsAlreadyAdded = false;
	public ArrayList<FuncDef> bridgeMethodsForNonpublicClassSuperMethods = new ArrayList<FuncDef>(0);
	public boolean objProvider;
	
	public TheScopeFrame  getScopeFrame()
	{
		return myScopeFrame;
	}
	
	
	 
	@Override
	public TheScopeFrame getScopeFrameGenIfMissing(TheScopeFrame parent, ClassDef cls) {
		if(null == myScopeFrame)
		{
			myScopeFrame = TheScopeFrame.buildTheScopeFrame_Class(parent, true, cls);
		}
		else
		{
			parent.addChild(cls.classBlock, myScopeFrame);
			myScopeFrame.setParent(parent);
		}
		

		myScopeFrame.setSuppressedWarnings(Utils.extractSuppressedWarningsFromAnnotations(cls.annotations, null));
		
		return myScopeFrame;
	}

	
	
	///
	//// Scope frame interfacing stuff... Maybe a mega hack but meh
	///
	
	public HashSet<TypeAndLocation> getFuncDef(String name, boolean ignoreLambdas, boolean extensionFunction)
	{
		return getFuncDef(name, true, ignoreLambdas, extensionFunction);
	}
	

	private final static ErrorRaiseable errorRaisableSupression = new ErrorRaiseableSupressErrors(null);
	
	protected TypeAndLocation convertLocalVaraibletoFunction(TypeAndLocation tt) {
		Type tat = tt.getType();
		
		if(TypeCheckUtils.hasRefLevels(tat)){
			tat = TypeCheckUtils.getRefType(tat);
		}
		
		boolean islazyLambdaVar = false;
		if(tat instanceof NamedType && null != TypeCheckUtils.checkSubType(errorRaisableSupression, ScopeAndTypeChecker.getLazyNT(), tat)) {
			tat = ((NamedType)tat).getGenTypes().get(0);
			islazyLambdaVar=true;
		}
		
		if(tat instanceof FuncType || tat instanceof GenericType)
		{
			if(islazyLambdaVar) {
				tt.getLocation().islazyLambdaVar = islazyLambdaVar;
			}
			
			return islazyLambdaVar?new TypeAndLocation(tat, tt.getLocation()):tt;
		}
		return null;
	}
	
	public HashSet<TypeAndLocation> getFuncDef(String name, boolean searchSuperClass, boolean ignoreLambdas, boolean extensionFunction)
	{
		HashSet<TypeAndLocation> ret = new HashSet<TypeAndLocation>();
		if(null != myScopeFrame)
		{
			if(!ignoreLambdas)
			{
				if(myScopeFrame.hasVariable(null, name, false, false, false))
				{
					TypeAndLocation tt = myScopeFrame.getVariable(null, name, false).getA();
					
					tt = convertLocalVaraibletoFunction(tt);
					if(null != tt) {
						ret.add(new TypeAndLocation(tt.getType(), tt.getLocation()));
					}
				}
			}
			
			if(myScopeFrame.hasFuncDef(null, name, false, false, false))
			{//u cannot call finalize etc
				for(TypeAndLocation funca : myScopeFrame.getFuncDef(null, name, false).getA() )
				{
					FuncType func = (FuncType)funca.getType();
					boolean callable = true;
					
					if(extensionFunction != func.extFuncOn){
						continue;//skip if ext func wanted and thing isnt, or if not wanted and it is
					}
					
					if(UncallableMethods.GLOBAL_UNCALLABLE_METHODS.containsKey(name))
					{
					
						for(FuncType cand : UncallableMethods.GLOBAL_UNCALLABLE_METHODS.get(name))
						{
							if(cand.equals(func))
							{
								callable = false;
								break;
							}
						}
					}
					if(callable){
						ret.add(funca );
					}
				}
			}
			
			if(searchSuperClass && null != this.resolvedSuperType)
			{
				//if lower type already eixsts then it override the supertype...
				HashSet<TypeAndLocation> toAdd = new HashSet<TypeAndLocation>();
				
				ArrayList<Pair<ClassDef, Map<Type, Type>>> classes = new ArrayList<Pair<ClassDef, Map<Type, Type>>>();
				classes.add( new Pair<ClassDef, Map<Type, Type>>(this.resolvedSuperType, this.superclassTypeToClsGeneric) );//supertypes first
				

				for(Pair<ClassDef, List<Type>> traitGen : this.getInterfacesMapGens()) {
					ClassDef cd = traitGen.getA();
					if(null != cd) {
						List<Type> qualis = traitGen.getB();
						ArrayList<GenericType> definedgens = cd.getClassGenricList();
						
						Map<Type, Type> mappedGens = new HashMap<Type, Type>();
						int qsize = qualis.size();
						if(qsize == definedgens.size()) {
							for(int n=0; n < qsize; n++) {
								mappedGens.put(definedgens.get(n), TypeCheckUtils.boxTypeIfPrimative(qualis.get(n), false));
							}
						}
						
						classes.add( new Pair<ClassDef, Map<Type, Type>>(cd, mappedGens) );
					}
				}
				
				for(Pair<ClassDef, Map<Type, Type>> traitOrSuperAndGens : classes) {
					ClassDef traitOrSuper = traitOrSuperAndGens.getA();
					Map<Type, Type> typeToGens = traitOrSuperAndGens.getB();
					
					for(TypeAndLocation superdefGenLAC :  traitOrSuper.getFuncDef(name, false, extensionFunction))
					{//make sure superclass func def not been overriden (if has then just have the child one)
						FuncType superdef = (FuncType) GenericTypeUtils.mapFuncTypeGenericsToOtherGenerics( (FuncType)superdefGenLAC.getType(), typeToGens, false);
						
						Location loc = superdefGenLAC.getLocation();
						
						if(traitOrSuper.accessModifier != AccessModifier.PUBLIC && loc instanceof ClassFunctionLocation) {
							//non public superclass
							if(superdef.origonatingFuncDef != null && superdef.origonatingFuncDef.accessModifier == AccessModifier.PUBLIC) {
								//direct location to calling class not superclas - for call to be directed via BRIDGE method
								NamedType asnt = new NamedType(this);
								loc = (ClassFunctionLocation)loc.copy();
								((ClassFunctionLocation)loc).owner = asnt.getCheckCastType();
								((ClassFunctionLocation)loc).ownerType = asnt; 
										
							}
						}
						
						
						TypeAndLocation tAndL = new TypeAndLocation(superdef, loc);
						
						boolean canAdd = true;
						//TODO: refactor with approach from constructor...
						for(TypeAndLocation alreadyTandL : ret )
						{
							FuncType already = (FuncType)alreadyTandL.getType();
							
							if(extensionFunction != already.extFuncOn){
								continue;//skip if ext func wanted and thing isnt, or if not wanted and it is
							}
							
							if(TypeCheckUtils.isArgumentListSame(superdef.getInputs(), already.getInputs()))
							{
								canAdd = false;
								break;
							}
						}
						
						if(canAdd)
						{
							toAdd.add(tAndL);
						}
					}
					
				}
				
				ret.addAll( toAdd );
			}
		}
		
		return ret;
	}
	
	public boolean hasFuncDef(String name, boolean ignoreLambdas, boolean extensionFunction)
	{
		HashSet<TypeAndLocation> ret = getFuncDef(name,true, extensionFunction);
		return null != ret && !ret.isEmpty();
	}
	
	public boolean hasFuncDef(String name, FuncType ft)
	{
		HashSet<TypeAndLocation> ret = getFuncDef(name,true, false);
		if(null != ret){
			for(TypeAndLocation tal : ret){
				if(tal.getType().equals(ft)){
					return true;
				}
			}
		}
		
		return false;
	}
	
	public TypeAndLocation getVariable(TheScopeFrame currentScopeFrame, String name)
	{
		return getVariable(currentScopeFrame, name, true, false);
	}
	
	private TypeAndLocation doMapFuncTypeGensFiddle(TypeAndLocation input)
	{
		return doMapFuncTypeGensFiddle(input, this.superclassTypeToClsGeneric);
	}
	
	private TypeAndLocation doMapFuncTypeGensFiddle(TypeAndLocation input,  Map<Type, Type> mapping)
	{
		Type got = GenericTypeUtils.mapFuncTypeGenericsToOtherGenerics(input==null?null:input.getType(), mapping, false);
		return null==got?null:new TypeAndLocation(got, input==null?null:input.getLocation());
	}
	
	public TypeAndLocation getVariable(TheScopeFrame currentScopeFrame, String name, boolean searchParent, boolean ignoreAutoGen)
	{
		if(null != myScopeFrame)
		{
			if(myScopeFrame.hasVariable(currentScopeFrame, name, false, ignoreAutoGen, false))
			{
				return doMapFuncTypeGensFiddle(myScopeFrame.getVariable(currentScopeFrame, name, false).getA());
			}
			if(searchParent /*&& null != this.resolvedSuperType*/)
			{
				//traits first
				
				ArrayList<Pair<ClassDef, Map<Type, Type>>> classes = new ArrayList<Pair<ClassDef, Map<Type, Type>>>();
				classes.add( new Pair<ClassDef, Map<Type, Type>>(this.resolvedSuperType, this.superclassTypeToClsGeneric) );//supertypes first
				

				for(Pair<ClassDef, List<Type>> traitGen : this.getInterfacesMapGens()) {
					ClassDef cd = traitGen.getA();
					if(null != cd) {
						List<Type> qualis = traitGen.getB();
						ArrayList<GenericType> definedgens = cd.getClassGenricList();
						
						Map<Type, Type> mappedGens = new HashMap<Type, Type>();
						int qsize = qualis.size();
						if(qsize == definedgens.size()) {
							for(int n=0; n < qsize; n++) {
								mappedGens.put(definedgens.get(n), TypeCheckUtils.boxTypeIfPrimative(qualis.get(n), false));
							}
						}
						
						classes.add( new Pair<ClassDef, Map<Type, Type>>(cd, mappedGens) );
					}
				}
				
				for(Pair<ClassDef, Map<Type, Type>> item : classes) {
					ClassDef inst = item.getA();
					if(null != inst){
						TypeAndLocation tal = inst.getVariable(currentScopeFrame, name);
						if( null != tal) {
							return doMapFuncTypeGensFiddle(tal, item.getB());
						}
					}
				}
				//return doMapFuncTypeGensFiddle(this.resolvedSuperType.getVariable(currentScopeFrame, name));
			}
		}
		return null;
	}
	
	public HashMap<String, TypeAndLocation> getAllPublicVars(TheScopeFrame currentScopeFrame){
		if(null != myScopeFrame)
		{
			HashMap<String, TypeAndLocation> things = myScopeFrame.getAllVars(currentScopeFrame);

			HashMap<String, TypeAndLocation> ret = new HashMap<String, TypeAndLocation>();
			
			for(String key : things.keySet()){
				if(!key.contains("$")){
					TypeAndLocation var = doMapFuncTypeGensFiddle(things.get(key));
					ret.put(key, var);
				}
			}
			return ret;
		}
		return null;
	}
	
	
	public boolean hasVariable(TheScopeFrame currentScopeFrame, String name, boolean searchParent, boolean ignoreAutoGennerated)
	{
		TypeAndLocation ret = getVariable(currentScopeFrame, name, searchParent, ignoreAutoGennerated);
		if(null == ret)
		{
			return false;
		}
		else if(ignoreAutoGennerated && ret.getType().getAutoGennerated() )
		{
			return false;
		}
		return true;
	}
	
	public boolean hasVariable(TheScopeFrame currentScopeFrame, String name)
	{
		return null != getVariable(currentScopeFrame, name);
	}
	
	public ClassDef getClassDef(String name)
	{
		if(null != myScopeFrame)
		{
			//int dotpos = name.indexOf('.'); cannot do nested static classes in concurnas
			//if(dotpos == -1)
			//{
			if(myScopeFrame.hasClassDef(null, name, false, false))
			{
				return myScopeFrame.getClassDef(null, name, false, false);
			}
			else if(null != this.resolvedSuperType)
			{
				return this.resolvedSuperType.getClassDef(name);
			}
		}
		return null;
	}
	
	public boolean hasClassDef(String name)
	{
		return null != getClassDef(name);
	}
	
	public String toStringFunc(char dot)
	{
		StringBuilder sb = new StringBuilder();
		
		if(!this.isLocalClass){
			if(this.packageName != null && !this.packageName.equals(""))
			{
				sb.append(this.packageName);
				sb.append(dot);
			}
			//sb.append(this.packageName);
		}
		
		sb.append(this.className);
		return sb.toString();
	}
	
	@Override
	public String toString()
	{
		/*if(this.isAnonClass) {
			return "Anonymous Class";
		}*/
		
		return toStringFunc('.');
	}
	
	public String javaClassName()
	{
		return "L"+bcFullName() +";";
	}

	//in concurnas all nested classes are NON static
	
	public boolean isParentNestorOrEQ(ClassDef potential){
		return this.equals(potential) || isParentNestor(potential);
	}
	
	/**
	 * is the thing on the right a parent of me?
	 */
	public boolean isParentNestor(ClassDef potential){
		ClassDef par = this.getParentNestor();
		
		if(null != par)
		{
			if(par.equals(potential)){
				return true;
			}
			
			if(par.isParentNestor(potential)) {
				return true;
			}
		}
		

		ArrayList<ClassDef> ifaces = this.getResolvedInterfaces();
		for(ClassDef iface : ifaces) {
			if(potential.equals(iface)){
				return true;
			}
			
			if(potential.isParentNestor(iface)) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isParentNestorOrSuper(ClassDef potential){
		ClassDef par = this.getParentNestor();
		
		if(null != par)
		{
			if(potential.isBeingBeingPastTheParentOfMe(par)){
				return true;
			}
		}
		return false;
	}
	
	
	public ClassDef isParentNestorEQOrSUperClass(ClassDef dude)
	{
		/*if(dude.equals(this))
		{
			return true;
		}
		else*/ if(this.isBeingBeingPastTheParentOfMe(dude))
		{
			return dude;
		}
		else
		{
			ClassDef par = this.getParentNestor();
			if(null != par)
			{
				return par.isParentNestorEQOrSUperClass(dude);
			}
		}
		return null;
	}
	
	public void setParentNestor(ClassDef parentNestor)
	{
		this.parentNestor = parentNestor;
	}
	
	public boolean hasParentNestor(){
		return this.parentNestor != null;
	}
	
	public ClassDef getParentNestor()
	{
		return this.parentNestor;
	}
	
	public boolean isNestedAndNonStatic()
	{
		return this.parentNestor != null;
	}
	
	public int getNestingLevel()
	{
		int ret =0;
		ClassDef parent = this.parentNestor;
		while(null != parent){
			ret++;
			parent = parent.parentNestor;
		}
		return ret;
	}

	public void setIsLocalClass(boolean itslocal){
		if(itslocal){
			callingConstructors = new HashSet<New>();
			classesHavingMeAsTheSuperClass = new HashSet<ClassDef>();
			localsuperInvokations = new HashSet<SuperConstructorInvoke>();
			callingfuncRefsAsRefNames = new HashSet<RefName>();
		}
		isLocalClass = itslocal;
	}

	public ArrayList<NamedType> getTraitsAsNamedType(int line, int col){
		return getTraitsAsNamedType(false, line, col);
	}
		
	public ArrayList<NamedType> getTraitsAsNamedType(boolean useActorImplicitIfaces, int line, int col){
		if(useActorImplicitIfaces && null != this.typedActorOnImplicitIfaces) {
			return new ArrayList<NamedType>(this.typedActorOnImplicitIfaces);
		}
		
		return getTraitsAsNamedType(line, col, true);
	}
	
	/**
	 * @param includeInterfaces - include 'ordinary' java interfaces, i.e. not traits. Which go via the java method invokation approach of superclass first
	 */
	public ArrayList<NamedType> getTraitsAsNamedType(int line, int col, boolean includeInterfaces){
		ArrayList<NamedType> ret = new ArrayList<NamedType>();
		
		ArrayList<Pair<ClassDef, List<Type>>> ifaces = this.getTraitsMapGens(includeInterfaces);
		
		if(null != ifaces){
			for(Pair<ClassDef, List<Type>> entry : ifaces){
				ret.add(fullyQualifiedGens(line, col, entry.getA(), entry.getB()));
			}
		}
		
		Collections.reverse(ret);
		
		return ret;
	}
	
	protected ArrayList<Pair<ClassDef, List<Type>>> getInterfacesMapGens() {
		return getTraitsMapGens(true);
	}
	
	public static boolean classHasTraitAnnotation(ClassDef cd) {
		/*if(cd != null) {
			Annotations annots = cd.getAnnotations();
			if(null != annots) {
				return annots.hasAnnotation("com.concurnas.lang.Trait");
			}
		}
		return false;*/
		return classHasAnnotation(cd, "com.concurnas.lang.Trait", false);
	}
	
	public static boolean classHasAnnotation(ClassDef cd, String annotation, boolean checkSuperTypeAndIfaces) {
		if(cd != null) {
			Annotations annots = cd.getAnnotations();
			if(null != annots) {
				return annots.hasAnnotation(annotation);
			}
			if(checkSuperTypeAndIfaces) {
				if(classHasAnnotation(cd.getSuperclass(), annotation, checkSuperTypeAndIfaces)) {
					return true;
				}else {
					HashSet<ClassDef> traits = cd.getTraits();
					return traits.stream().anyMatch(tt -> tt != null && classHasAnnotation(tt, annotation, checkSuperTypeAndIfaces));
				}
				
			}
		}
		
		return false;
	}
	
	public boolean allTraitClassesResolved() {
		if(this.traits.isEmpty()) {
			return true;
		}else {
			return this.traits.stream().allMatch(a -> a.resolvedIface != null);
		}
	}
	
	protected ArrayList<Pair<ClassDef, List<Type>>> getTraitsMapGens(boolean includeInterfaces) {
		// TODO mixins - add generic mapping here
		ArrayList<Pair<ClassDef, List<Type>>> ret = new ArrayList<Pair<ClassDef, List<Type>>>();
		
		if(!this.traits.isEmpty()) {
			for(ImpliInstance impli : this.traits) {//mixins
				if(includeInterfaces || classHasTraitAnnotation(impli.resolvedIface)) {
					List<Type> qualifiedgens = impli.traitGenricList;
					if(impli.iffaceTypeToClsGeneric.size() == qualifiedgens.size()) {
						qualifiedgens =  qualifiedgens.stream().map(a -> impli.iffaceTypeToClsGeneric.getOrDefault(a, a)).collect(Collectors.toList());
					}
					
					ret.add(new Pair<ClassDef, List<Type>>(impli.resolvedIface, TypeCheckUtils.boxTypeIfPrimative(qualifiedgens, false)));
				}
			}
		}
		
		return ret;
	}


	private NamedType fullyQualifiedGens(int line, int col, ClassDef supCls, List<Type> genClassList){
		if(null == supCls){
			return null;
		}
		NamedType ret = new NamedType(line, col, supCls);
		
		if(null != genClassList){
			ArrayList<Type> genQuali = new ArrayList<Type>(genClassList.size());
			//ensure qualified properly...
			for(Type lhs : genClassList){
				if (this.superclassTypeToClsGeneric.containsKey(lhs)){
					genQuali.add(this.superclassTypeToClsGeneric.get(lhs));
				}
				else{
					genQuali.add(lhs);
				}
			}
			ret.setGenTypes(genQuali);
		}
		return ret;
	}
	
	public NamedType getFullyqualifiedSuperClassRef(int line, int col){//for casting etc
		return fullyQualifiedGens(line, col, this.getSuperclass(), this.superClassGenricList); 
	}

	//of use when adding defualt methods which then later need to be added again (cos u did it too early in the compilation cycle...)
	//yeah lazy, alternative would be to have an extra comp cycle which goes in and adds default methods...
	public void removeFuncDef(String name, FuncType signature){
		myScopeFrame.removeFuncDef(name, signature);
	}
	
	public void overwriteLineHolder(Pair<String, FuncType> key, LineHolder newlh){
		
		LineHolder oldLH = prependedStuff.get(key);
		if(null != oldLH){
			classBlock.replaceLineHolder(oldLH, newlh);
		}
		else{
			classBlock.prepend(newlh);
		}
		prependedStuff.put(key, newlh);
	}

	public ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>> getAllFieldsDefined() {
		return getAllFieldsDefined(false);
	}
	
	public ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>> getAllFieldsDefined(boolean assignedValuesMustBeFinalToBeAssigned) {
		ArrayList<VarAtScopeLevel>  stufff = this.myScopeFrame.getAllVariablesAtScopeLevel(false, true, false, false);
		
		ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>> ret = new ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>>(stufff.size());
		
		for(VarAtScopeLevel one: stufff){
			Annotations annots = one.getAnnotations();
			String namedField = null;
			if(annots != null && annots.annotations != null && !annots.annotations.isEmpty()) {
				for(Annotation ann : annots.annotations) {
					if(ScopeAndTypeChecker.const_Named.equals(ann.getTaggedType())) {
						namedField = ((VarString)ann.singleArg).str;
						break;
					}
				}
			}
			
			ret.add(new Sixple<String, Type, Boolean, AccessModifier, Boolean, String>(one.getVarName(), one.getType(), assignedValuesMustBeFinalToBeAssigned?one.getAssignedOnCreationAndFinal():one.getAssignedOnCreation(), one.getAccessModifier(), one.isInjected(), namedField));
		}
		
		return ret;
	}
	
	public ArrayList<Sevenple<String, Type, Boolean, AccessModifier, ClassDef, Boolean, String>> getAllFields() {
		return getAllFields(false);
	}
	
	public ArrayList<Sevenple<String, Type, Boolean, AccessModifier, ClassDef, Boolean, String>> getAllFields(boolean onlytraits) {
		ArrayList<Sevenple<String, Type, Boolean, AccessModifier, ClassDef, Boolean, String>> ret = new ArrayList<Sevenple<String, Type, Boolean, AccessModifier, ClassDef, Boolean, String>>(0);

		
		for(NamedType nt : this.getTraitsAsNamedType(0, 0)) {
			if(nt != null) {
				ClassDef origin = nt.getSetClassDef();
				for(Sixple<String, Type, Boolean, AccessModifier, Boolean, String> inst : nt.getAllFields(onlytraits)) {
					ret.add(new Sevenple<String, Type, Boolean, AccessModifier, ClassDef, Boolean, String>(inst.getA(), inst.getB(), inst.getC(), inst.getD(), origin, inst.getE(), inst.getF()));
				}
			}
		}
		
		ClassDef par = this.getSuperclass();
		if(par != null){
			ArrayList<Sevenple<String, Type, Boolean, AccessModifier, ClassDef, Boolean, String>> found = par.getAllFields(onlytraits);
			ret.addAll(found);
		}
		
		if(!onlytraits || this.isTrait) {
			for(Sixple<String, Type, Boolean, AccessModifier, Boolean, String> inst : getAllFieldsDefined()) {
				ret.add(new Sevenple<String, Type, Boolean, AccessModifier, ClassDef, Boolean, String>(inst.getA(), inst.getB(), inst.getC(), inst.getD(), this, inst.getE(), inst.getF()));
			}
		}else if(onlytraits) {
			//if trait field is defined here then ignore existing one from trait
			HashMap<String, Sevenple<String, Type, Boolean, AccessModifier, ClassDef, Boolean, String>> foundAlready = new HashMap<String, Sevenple<String, Type, Boolean, AccessModifier, ClassDef, Boolean, String>>();
			ret.forEach(a -> foundAlready.put(a.getA(), a));
			
			for(Sixple<String, Type, Boolean, AccessModifier, Boolean, String> inst : getAllFieldsDefined()) {
				if(foundAlready.containsKey(inst.getA())){
					ret.remove(foundAlready.get(inst.getA()));
					ret.add(new Sevenple<String, Type, Boolean, AccessModifier, ClassDef, Boolean, String>(inst.getA(), inst.getB(), inst.getC(), inst.getD(), this, inst.getE(), inst.getF()));
				}
			}
			
		}
		
		return ret;
	}
	
	public boolean isStatic() {
		return this.parentNestor==null;//top level classes dont have parent nestors therefore static
	}


	public boolean getIsAbstract() {
		return this.isAbstract;
	}


	public void setIsAbstract(boolean isAbstract) {
		 this.isAbstract = isAbstract;
	}


	public boolean hasClassDefArgs() {
		return this.classDefArgs != null && !this.classDefArgs.aargs.isEmpty();
	}

	public boolean isAnnotation() {
		return isAnnotation;
	}


	public AccessModifier getAccessModifier() {
		return this.isLocalClass?AccessModifier.PUBLIC:this.accessModifier;
	}

	public List<Pair<String, TypeAndLocation>> getAllStaticAssets() {
		return  new ArrayList<Pair<String, TypeAndLocation>>(0);//no supported! got via module compiler
	}

	public ArrayList<ClassDef> getAllNestedClasses() {
		return this.myScopeFrame.getAllClasses();
	}

	private boolean canSkipIterativeCompilation=false;
	@Override
	public boolean canSkip() {
		return canSkipIterativeCompilation;
	}

	@Override
	public void setSkippable(boolean skippable) {
		canSkipIterativeCompilation = skippable;
	}

	@Override
	public String getName() {
		return this.className;
	}

	@Override
	public Type getFuncType() {
		return new NamedType(this);
	}

	@Override
	public boolean isNewComponent() {
		return true;
	}
	
	@Override
	public boolean persistant() { 
		return true;
	}
	

	public boolean hasErrors = false;
	@Override
	public void setErrors(boolean hasErrors) {
		this.hasErrors = hasErrors;
	}
	@Override
	public boolean getErrors() {
		return hasErrors;
	}
	
	private boolean supressErrors = false;
	@Override
	public void setSupressErrors(boolean supressErrors) {
		this.supressErrors = supressErrors;
	}
	@Override
	public boolean getSupressErrors() {
		return supressErrors;
	}
}