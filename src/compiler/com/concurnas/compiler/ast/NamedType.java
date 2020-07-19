package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.concurnas.compiler.typeAndLocation.Location;
import com.concurnas.compiler.typeAndLocation.LocationStaticField;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.CompiledClassUtils;
import com.concurnas.compiler.utils.GenericTypeUtils;
import com.concurnas.compiler.utils.Sevenple;
import com.concurnas.compiler.utils.Sixple;
import com.concurnas.compiler.visitors.ErrorRaiseable;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker.CapMaskedErrs;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.compiler.visitors.util.DummyErrorRaiseable;
import com.concurnas.compiler.visitors.util.ErrorRaiseableSupressErrors;
import com.concurnas.runtime.Pair;

public class NamedType  extends AbstractType {
	//TODO: consider tagging this with the real type reference
	public String namedType;
	public ClassDef classDef;
	private boolean isRef = false;
	public List<Type> genTypes = new ArrayList<Type>();
	//
	public HashMap<GenericType, Type> fromClassGenericToQualifiedType = new HashMap<GenericType, Type>();
	public ArrayList<Pair<String, ArrayList<Type>>> nestorSegments = new ArrayList<Pair<String, ArrayList<Type>>>();
	//public NamedType origonalGenericTypeUpperBound = null;
	private boolean lockedAsRef;
	public boolean isCalledInConsructor = false;
	//public boolean unresolveableLocalType = false;//e.g. for onChange stateObjects
	public boolean orignallyfromVarNull = false;
	//public FuncType forcedConstructorDueToGenTypeInference;
	public boolean isWildCardAny = false;
	public boolean isLockedAsActor = false;
	public boolean pretendNothingAbstract = false;
	public boolean isDefaultActor = false;//TODO: remove this is not actually needed, extending Actor is enough
	public boolean expectedToBeAbstractTypedActor = false;//TODO: remove this is not actually needed, extending Actor is enough
	public List<Type> origGenericBinding = null;;
	public boolean okToReferenceEvenIfUncreatable = false;//usually for own references only
	public Type astredirect;
	public boolean ignoreNonQualificationfGenerics=false;//e.g. when used like this:typedef  amyc = MyClass, then its ok for the NamedType to not have its generic parameters defined
	public boolean printNameInBytecodeRepresentation=false;
	public boolean isEnumParent;//e.g. MyEnum.ONE <-true, MyEnum.ONE.something <- false
	public boolean errorOnPrevoiusGenTypeQualification=false;//force generic types to be infered on every compilation wave
	public boolean ignorePPPCheck;
	public boolean fromisWildCardAny=false;
	public boolean requiresGenTypeInference = false;
	public boolean usedToCreateNewObj = false;
	
	public NamedType(ClassDef classDef, ArrayList<Type> genTypes)
	{
		this(0,0, classDef, genTypes);
	}
	
	public NamedType(int line, int col, ClassDef classDef, ArrayList<Type> genTypes)
	{
		this(line, col, classDef);
		this.setGenTypes(genTypes);
		//Class<?> h = Object.class;
		//mv.visitLdcInsn(Type.getType("[Ljava/lang/Object;"));
	}
	
	public boolean isInterface(){
		if(this.classDef == null){//JPT: mega hack so we dont have to gennerate the actor code at compilation time
			return this.namedType.endsWith("$$ActorIterface");
		}
		
		return this.classDef.isInterface();
	}
	
	
	public boolean isAbstract(){
		return this.classDef.getIsAbstract();
	}
	
	
	
	public NamedType(ClassDef classDef)
	{
		this(0,0,classDef);
	}
	
	public NamedType(int line, int col, ClassDef classDef)
	{
		this(line, col);
		/*
		if(classDef instanceof ClassDefJava)
		{//this ensures that arrays are managed at the NamedType level NOT at the src level...
			Tuple<Integer, ClassDefJava> res = ClassDefJava.stripArrayLevels((ClassDefJava)classDef);
			this.arrayLevels = res.getA();
			ArrayList<Object> genStuff = classDef.classGenricList;
			classDef = res.getB();
			classDef.classGenricList = genStuff;
		}
		*/
		this.setClassDef(classDef);
		this.namedType = classDef==null?null:classDef.getPrettyName();
	}

	public NamedType(ClassDef classDef, int arrayLevels)
	{
		this(0,0, classDef ,arrayLevels);
	}
	
	public NamedType(int line, int col, ClassDef classDef, int arrayLevels)
	{
		this(line, col);
		if(classDef instanceof ClassDefJava)
		{
			assert null == ((ClassDefJava)classDef).getClassHeld().getComponentType();
		}
		this.setClassDef(classDef);
		this.arrayLevels = arrayLevels;
	}
	
	
	public NamedType(int line, int col, String namedType) {
		this(line, col);
		this.namedType = namedType;
	}
	
	public NamedType(int line, int col, String namedType, ArrayList<Type> genTypes) {
		this(line, col, namedType);
		this.setGenTypes(genTypes);
	}
	
	
	public NamedType(int line, int col, String refType, Type refTypeHeld) {
		//e.g. int:RefArray -> 12, 23, RefArray, int
		this(line, col, refType);
		ArrayList<Type> genTypes = new ArrayList<Type>();
		genTypes.add(refTypeHeld);
		this.setGenTypes(genTypes);
		this.isRef=true;
	}
	
	private static final ClassDefJava RefLocal = new ClassDefJava(com.concurnas.runtime.ref.Local.class);
	public static final NamedType ntObj = new NamedType( new ClassDefJava(Object.class));
	
	public NamedType(int line, int col, Type refType) {
		//pass in null 3rd arg assumes type is object
		this(line, col, RefLocal, 0);
		ArrayList<Type> genTypes = new ArrayList<Type>();
		genTypes.add(refType==null?ntObj:refType);
		this.setGenTypes(genTypes);
		this.isRef=true;
		//this.lockedAsRef=true;
	}
	
	public NamedType(int line, int col, String namedType, ArrayList<Type> genTypes, ArrayList<Pair<String, ArrayList<Type>>> nestorSegments) {
		this(line, col, namedType, genTypes);
		this.nestorSegments  = nestorSegments;
	}
	

	
	public boolean hasNestorSegment(String name)
	{
		for(Pair<String, ArrayList<Type>> item : nestorSegments)
		{
			if(name.equals(item.getA()))
			{
				return true;
			}
		}
		return false;
	}
	
	
	public String getNamedTypeStr()
	{
		String ret = this.namedType;
		if(null == ret && null != classDef)
		{
			ret = classDef.toString();
		}
		return ret;
	}
	
	@Override
	public NamedType copyTypeSpecific()
	{
		NamedType ret = new NamedType(this.line, this.column);
		ret.setClassDef(this.classDef);
		ret.arrayLevels = this.arrayLevels;
	
		if(this.origGenericBinding != null){
			ArrayList<Type> origGenericBindingCopy = new ArrayList<Type>(this.origGenericBinding.size());
			for(Type t : this.origGenericBinding){
				origGenericBindingCopy.add(null==t?null:(Type)t.copy());
			}
			ret.origGenericBinding = origGenericBindingCopy;
		}
	
		
		ArrayList<Type> genTypesCopy = new ArrayList<Type>(this.genTypes==null?0:this.genTypes.size());
		if(null != this.genTypes){
			for(Type t : this.genTypes){
				genTypesCopy.add(null==t?null:(Type)t.copy());
			}
		}
		
		ret.setGenTypes(genTypesCopy);
		
		ret.namedType = this.namedType;
		ret.isRef = this.isRef;
		ret.lockedAsRef = this.lockedAsRef;
		ret.inout = this.inout;
		//ret.unresolveableLocalType = this.unresolveableLocalType;
		ret.origonalGenericTypeUpperBound = this.origonalGenericTypeUpperBound;
		ret.fromClassGenericToQualifiedType = new HashMap<GenericType, Type>(this.fromClassGenericToQualifiedType);
		ret.orignallyfromVarNull = orignallyfromVarNull;
		//ret.forcedConstructorDueToGenTypeInference = forcedConstructorDueToGenTypeInference;
		ret.isWildCardAny = isWildCardAny;
		ret.fromisWildCardAny = fromisWildCardAny;
		ret.isDefaultActor = isDefaultActor;
		ret.expectedToBeAbstractTypedActor = expectedToBeAbstractTypedActor;
		ret.isLockedAsActor = isLockedAsActor;
		ret.isLHSClass = isLHSClass;
		ret.isEnumParent = isEnumParent;
		ret.iIsTypeInFuncref = iIsTypeInFuncref;
		ret.errorOnPrevoiusGenTypeQualification = errorOnPrevoiusGenTypeQualification;
		ret.pretendNothingAbstract = pretendNothingAbstract;
		ret.ignoreNonQualificationfGenerics = ignoreNonQualificationfGenerics;
		ret.printNameInBytecodeRepresentation = printNameInBytecodeRepresentation;
		ret.requiresGenTypeInference = requiresGenTypeInference;
		ret.usedToCreateNewObj = usedToCreateNewObj;
		ArrayList<Pair<String, ArrayList<Type>>> rnestorSegments = new ArrayList<Pair<String, ArrayList<Type>>>();
		for(Pair<String, ArrayList<Type>> row : this.nestorSegments){
			ArrayList<Type> orig = row.getB();
			ArrayList<Type> shadow = new ArrayList<Type>(orig.size() );
			for(Type tt : orig){
				shadow.add((Type)tt.copy());
			}
			
			rnestorSegments.add(new Pair<String, ArrayList<Type>>(row.getA(), shadow));
		}
		
		ret.nestorSegments = rnestorSegments;
		ret.isPartOfVarargArray = isPartOfVarargArray;
		ret.astredirect=astredirect==null?null:(Type)astredirect.copy();
		ret.setVectorized(super.getVectorized());
		ret.nullStatus = this.nullStatus;
		ret.okToReferenceEvenIfUncreatable = this.okToReferenceEvenIfUncreatable;
		super.cloneMeTo(ret);
		return ret;
	}
	
	public List<Type> getGenTypes()
	{
		return this.genTypes;
	}
	
	public NamedType getResolvedSuperTypeAsNamed()
	{
		if(null != this.classDef)
		{
			ClassDef sup = this.classDef.getSuperclass();
			if(null!= sup)
			{
				List<Type> supergens = this.classDef.superClassGenricList;
				ArrayList<Type> colouredInSuperGens;
				if(null != supergens)
				{
					colouredInSuperGens = new ArrayList<Type>(supergens.size());
					//now we color in the generic mapping
					
					for(Type t: supergens)
					{
						/* example:
						 open class SuperClass<X>
						 class Child<Y> < SuperClass<Holder<Y>>
						 
						 X-> SuperClass<Holder<Y>
						 
						 Y -> whatever named type has been defined as
						 
						 */
						
						Type par = GenericTypeUtils.mapFuncTypeGenericsToOtherGenerics(t, this.classDef.superclassTypeToClsGeneric, false);
						HashMap<Type, Type> fromClassGenericToQualifiedTypex = new HashMap<Type, Type>(fromClassGenericToQualifiedType);//we do this because refied generics!
						par = GenericTypeUtils.mapFuncTypeGenericsToOtherGenerics(par, fromClassGenericToQualifiedTypex, false);
						
						
						//open class Middle[X] extends Sup[X, String]{ next line permints this to be mapped
						if(this.fromClassGenericToQualifiedType.containsKey(par)){
							par = this.fromClassGenericToQualifiedType.get(par);
						}
						colouredInSuperGens.add(par);
					}
				}
				else
				{
					colouredInSuperGens = new ArrayList<Type>();
				}
				
				
				return new NamedType(this.line, this.column, sup, colouredInSuperGens);
			}
		}
		return null;
	}
	
	public List<NamedType> getResolvedTraitsAsNamed(){
		//TODO: this is a bit of a hack and doesnt map generics properly, need to improve when supporting mixins
		//works ok for the simple stuff, not the elaborate stuff, actually maybe it does work ok...
		List<NamedType> ret = new ArrayList<NamedType>();
		if(null != this.classDef){
			for(NamedType nat : this.classDef.getTraitsAsNamedType(0,  0)){
				HashMap<Type, Type> fromClassGenericToQualifiedTypex = new HashMap<Type, Type>(fromClassGenericToQualifiedType);//we do this because refied generics!
				nat = (NamedType)GenericTypeUtils.mapFuncTypeGenericsToOtherGenerics(nat, fromClassGenericToQualifiedTypex, false);
				ret.add(nat);
			}
		}
		
		return ret.stream().filter(a-> a != null).collect(Collectors.toList());
	}
	
	public ClassDef getSetClassDef()
	{
		if(this.astredirect != null && this.astredirect instanceof NamedType){
			return ((NamedType)this.astredirect).getSetClassDef();
		}
		return this.classDef; 
	}
	
	public NamedType()
	{
		this(0, 0);
	}
	
	public NamedType(int line, int col)
	{
		super(line, col);
	}
	
	
	public void augmentfromClassGenericToQualifiedType(HashMap<GenericType, Type> addstuff)
	{
		this.fromClassGenericToQualifiedType.putAll(addstuff);
	}
	
	
	
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(astredirect != null && !(visitor instanceof ScopeAndTypeChecker)){
			return astredirect.accept(visitor);
		}
				
		return visitor.visit(this);
	}

	public boolean isInstantiable()
	{
		ClassDef cd = this.astredirect != null && this.astredirect instanceof NamedType?((NamedType)this.astredirect).classDef:this.classDef;
		if(null != cd)
		{
			return cd.isInstantiable();
		}
		return false;
	}
	
	public NamedType getparentNestorFakeNamedType()
	{//for a 'boy' -> Child[String].Boy[Integer]
		NamedType ret = null;
		ClassDef parentNestor = this.classDef==null?null:this.classDef.getParentNestor();
		if(null != parentNestor)
		{
			ArrayList<Type> genTypes = new ArrayList<Type>();
			for(GenericType gen :parentNestor.classGenricList)
			{
				Type got = this.fromClassGenericToQualifiedType.get(gen);
				if(got != null)
				{
					genTypes.add(got);
				}
			}
			ret = new NamedType(this.getLine(), this.getColumn(), parentNestor);
			ret.setGenTypes(genTypes);
			ret.fromClassGenericToQualifiedType = this.fromClassGenericToQualifiedType;
			ret.nestorSegments = new ArrayList<Pair<String, ArrayList<Type>>>(this.nestorSegments.size());
			for(int n=0; n <= this.nestorSegments.size()-1; n++){//copy over the n-1 th nestor segment
				ret.nestorSegments.add(this.nestorSegments.get(n));
			}
		}
		return ret;
	}
	
	public String prefixWithParentGeneric(ClassDef parentClass, String toPrefixOver)
	{//i.e. boy Child[String].Boy[Float] = (new Child[String]() ).new Boy[Float]();
		if(null != parentClass)
		{
			ArrayList<Type> genTypes = new ArrayList<Type>();
			for(GenericType gen :parentClass.classGenricList)
			{
				Type got = this.fromClassGenericToQualifiedType.get(gen);
				if(got != null)
				{
					genTypes.add(got);
				}
			}
			
			NamedType parentNT = new NamedType(this.getLine(), this.getColumn(), parentClass);
			parentNT.setGenTypes(genTypes);
			
			String prefixReplace = parentClass.toString();
			String replaceWith = parentNT.toString();
			
			if(toPrefixOver.startsWith(prefixReplace))
			{
				return replaceWith + toPrefixOver.substring(prefixReplace.length());
			}
			
		}
		return toPrefixOver;
	}
	
	private String pn(boolean withGen){
		StringBuilder sb = new StringBuilder();

		InoutGenericModifier genmod = this.getInOutGenModifier();
		if(genmod != null){
			sb.append(genmod.toString() + " ");
		}
		
		
		if(isWildCardAny || fromisWildCardAny){
			sb.append("? ");
		}
		
		if(this.getIsRef()){
			Type in = this.getGenTypes().get(0);
			boolean ft = in instanceof FuncType;
			if(ft){sb.append("("); }
			sb.append(in.getPrettyName());
			if(ft){sb.append(")"); }
			String postDot = ""+this.classDef;
					
			sb.append(":" + (postDot.equals("com.concurnas.runtime.ref.Local")?"":postDot));
		}
		else{
			if(null!=classDef ){
				sb.append(classDef.getPrettyName());
			}
			else  if(namedType != null ){
				sb.append(namedType);
			}
			
			if(this.classDef != null && this.classDef.equals(ScopeAndTypeChecker.const_enumClass)) {
				withGen=false;
			}
			
			if(withGen ){
				if(null != this.genTypes && !this.genTypes.isEmpty())
				{
					sb.append('<');
					List<Type> gens = this.genTypes;
					int cnt = gens.size();
					for(int n=0; n < cnt; n++){
						sb.append(gens.get(n)==null?"null":gens.get(n).getPrettyName());
						if(n < cnt-1){
							sb.append(", ");
						}
					}
					sb.append('>');
				}
			}
		}
		
		if(this.arrayLevels >0){
			List<NullStatus> nsarlevel = this.getNullStatusAtArrayLevel();
			for(int n=0; n < this.arrayLevels; n++ ){
				if(nsarlevel.get(n) == NullStatus.NULLABLE) {
					sb.append("?");
				}
				sb.append("[]");
			}
		}
		
		String ret = sb.toString();
		
		ClassDef parentNestor = this.classDef==null?null : this.classDef.getParentNestor();
		if(withGen) {
			while(parentNestor!=null){
				ret = prefixWithParentGeneric(parentNestor, ret);
				parentNestor = parentNestor.getParentNestor();
			}
		}
		
		return ret + (this.getNullStatus() == NullStatus.NULLABLE?"?":"");
	}
	
	@Override
	public String getPrettyName() {
		return pn(true);
	}
	
	@Override
	public String toString()
	{
		/*InoutGenericModifier genmod = this.getInOutGenModifier();
		String ret = genmod != null ? genmod.toString() + " ":"";
		*/
		String later = (null != originRefType ? this.getPrettyName() + " or "+originRefType.toString():this.getPrettyName());
		if(isDefaultActor){
			if(later.contains("DefaultActor$")){//if its a default actor then we just show the class name
				List<Type> rt = TypeCheckUtils.extractRootActor(this).getGenTypes();
				if(!rt.isEmpty()){
					later = rt.get(0).toString();
				}
			}
		}
		
		return later + isVectorizedToString();
	}
	
	public String toStringNoGeneric()
	{
		return pn(false);
	}
	
	@Override
	public String getNonGenericPrettyName()
	{
		return pn(false);
	}
	
	public ArrayList<Type> getGenericTypeElements()
	{
		ArrayList<Type> ret = new ArrayList<Type>();
		if(null != genTypes){
			for(Type t : this.genTypes){//ret.add(TypeCheckUtils.boxTypeIfPrimative(t));
				ret.add(t);
			}
		}
		
		
		return ret;
	}
	
	public boolean isGeneric(){
		return !this.genTypes.isEmpty();
	}

	@Override
	public int getArrayLevelsRefOVerride() {
		return this.arrayLevels;
	}
	
	
	@Override
	public boolean hasArrayLevels() {
		if(this.astredirect!=null){
			return this.astredirect.hasArrayLevels();
		}
		return this.arrayLevels > 0;
	}
	
	
	
	@Override
	public boolean equals(Object other){
		if(other instanceof MultiType) {
			other = ((MultiType)other).getTaggedType();
		}
		
		if(other instanceof GenericType) {
			GenericType asGen = (GenericType)other;
			
			other = new NamedType(asGen.getLine(), asGen.getColumn(), asGen.name);
		}
		
		if(other instanceof NamedType )
		{
			NamedType asNamed = (NamedType)other;
			while(asNamed instanceof NamedTypeMany) {
				asNamed = (NamedType)((NamedTypeMany)asNamed).getSelf();
			}
			
			if( (asNamed.getArrayLevels() == this.getArrayLevels() ) && super.getVectorized() == asNamed.getVectorized()) {
				ClassDef lhsCD = asNamed.getSetClassDef();
				ClassDef rhsCD = this.getSetClassDef();
				if((lhsCD == null ? rhsCD == null : lhsCD.equals(rhsCD))) {
					
					ArrayList<Type> lhsgens = this.getGenericTypeElements();
					ArrayList<Type> rhsgens = asNamed.getGenericTypeElements();
					int lhsgensSize = lhsgens.size();
					if(lhsgensSize == rhsgens.size()) {
						for(int nx = 0; nx < lhsgensSize; nx++) {
							if(!Objects.equals(lhsgens.get(nx), lhsgens.get(nx))){
								return false;
							}
						}
					}
				}
				
				NamedType lhsupper = this.getOrigonalGenericTypeUpperBound();
				NamedType rhsUpper = asNamed.getOrigonalGenericTypeUpperBound();
				
				if (lhsupper == null || rhsUpper == null) {
					if (null != asNamed) {
						return TypeCheckUtils.regularEQ(this, asNamed);
					}

				} else {
					return lhsupper.equals(rhsUpper);

				}
			}
			
			
		}
		return false;
	}
	
	 @Override
    public int hashCode() {
		 String ts = this.toString();
		 ts = ts.replaceAll("\\?", "");//ignore nullable
		 return ts.hashCode();
		//return this.classDef.hashCode() + (int)this.arrayLevels;
    }

	@Override
	public boolean isChangeable() {
		return null != this.classDef ? this.classDef.isChangeable() : true; 
	}
	
	public boolean hasGenTypes()
	{
		return this.genTypes != null && !this.genTypes.isEmpty();
	}
	
	public void setGenTypesInfered(List<Type> genTypes) {
		//when generic types are infered we use the class level nullability declaration to adjust the inputs as approperiate
		if(!genTypes.isEmpty() && this.classDef != null){
			ArrayList<GenericType> gens = this.classDef.getClassGenricList();
			int sz = genTypes.size();
			if(gens.size() == sz) {
				ArrayList<Type> newgenTypes = new ArrayList<Type>(sz);
				int n=0;
				for(Type tt : genTypes) {
					tt = (Type)tt.copy();
					tt.setNullStatus(gens.get(n).getNullStatus());
					n++;
					newgenTypes.add(tt);
				}
				
				setGenTypes(newgenTypes);
				
				return;
			}
		}

		setGenTypes(genTypes);
	}
	
	public void setGenTypes(List<Type> genTypes)
	{
		
		
		if(null == origGenericBinding){
			origGenericBinding = genTypes;
		}
		
		this.genTypes = genTypes;
		setupGenMap();
	}
	
	public void setGenTypes(Type... genTypes)
	{
		ArrayList<Type> a = new ArrayList<Type>();
		for(Type genType:genTypes){
			a.add(genType);
		}
		
		this.setGenTypes(a);
	}
	
	public void setGenType(int n, Type genType)
	{
		ArrayList<Type> a = new ArrayList<Type>(genTypes);
		a.set(n, genType);
		
		this.setGenTypes(a);
	}
	
	
	public void setClassDef(ClassDef classDef)
	{
		this.classDef = classDef;
		setupGenMap();
	}
	
	/////////////////////
	
	
	private void setupGenMap()
	{
		if(this.classDef != null && this.genTypes != null)
		{
			List<GenericType> classGens = this.classDef.getClassGenricList();
			List<Type> qualified = this.genTypes;
			
			int fromSize = classGens.size();
			int toSize = qualified.size();
			int mon =Math.min(fromSize,toSize);
			if(null != classGens && qualified != null)// && fromSize == toSize)
			{
				for(int n = 0; n < mon; n++)
				{//try our best to map all the gens
					fromClassGenericToQualifiedType.put(classGens.get(n), qualified.get(n));
				}
			}
		}
	}
	
	
	////////////////////////////////////////////////////////////////////////////////////////////////
	
	

	
	
	public HashSet<TypeAndLocation> getFuncDef(int line, int col, String name,List<Type> argsWanted, ArrayList<Pair<String, Type>> namessMap, ScopeAndTypeChecker errorRaiser){
		return getFuncDef(line, col, name, true, argsWanted, namessMap, errorRaiser);
	}
	
	private static class GenericTypeAndQuali{
		public final GenericType q;
		public final Type quali;
		public GenericTypeAndQuali(GenericType q, Type quali){
			this.q = q;
			this.quali = quali;
		}
	}
	
	private static void qualifyMethodGenericFromArg(List<GenericType> genericsWanted, Type input, Type argWanted, List<GenericTypeAndQuali> found ){
		//box prim qualificaiton
		if(input instanceof GenericType && genericsWanted.contains(input)){//match!
			found.add(new GenericTypeAndQuali((GenericType)input, TypeCheckUtils.boxTypeIfPrimative(argWanted, false) ));
		}
		else if(input instanceof FuncType && argWanted instanceof FuncType){
			FuncType ift = (FuncType)input;
			FuncType aft = (FuncType)argWanted;
			ArrayList<Type> ifti = ift.getInputs();
			ArrayList<Type> afti = aft.getInputs();
			
			int inpis = ifti.size();//repetition?
			if(inpis == afti.size()){
				for(int n=0; n < inpis; n++){
					Type inputola = ifti.get(n);
					Type wantedarg = afti.get(n);//has qualification within in
					qualifyMethodGenericFromArg(genericsWanted, inputola, wantedarg, found);
				}
			}
			
			qualifyMethodGenericFromArg(genericsWanted, ift.retType, aft.retType, found);
		}
		else if(input instanceof NamedType && argWanted instanceof NamedType){
			NamedType ift = (NamedType)input;
			NamedType aft = (NamedType)argWanted;
			ArrayList<Type> ifti = ift.getGenericTypeElements();
			ArrayList<Type> afti = aft.getGenericTypeElements();
			
			int inpis = ifti.size();//repetition?
			if(inpis == afti.size()){
				for(int n=0; n < inpis; n++){
					Type inputola = ifti.get(n);
					Type wantedarg = afti.get(n);//has qualification within in
					qualifyMethodGenericFromArg(genericsWanted, inputola, wantedarg, found);
				}
			}
		}
		else if(input instanceof GenericType){
			found.add(new GenericTypeAndQuali((GenericType)input, input ));
		}
		//cannot primative etc
	}
	
	public static HashMap<GenericType, Type> qualifyGenericMethodTypes(ScopeAndTypeChecker satc, List<GenericType> genericsWanted, List<Type> inputs, List<Type> argsWanted ){
		//TODO: oops i think this got implemented as in attemptGenericBinding of TypeCheckUtils
		HashMap<GenericType, HashSet<Type>> qualifications = new HashMap<GenericType, HashSet<Type>>();
		
		int inpis = inputs.size();
		if(inpis == argsWanted.size()){//have to match else no hope!
			for(int n=0; n < inpis; n++){
				Type input = inputs.get(n);
				Type wantedarg = argsWanted.get(n);//has qualification within in
				if(wantedarg instanceof VarNull) {
					continue;
				}
				
				//have to search down the nesting of the type for one of the genericals
				ArrayList<GenericTypeAndQuali> qualiFound = new ArrayList<GenericTypeAndQuali>();
				qualifyMethodGenericFromArg(genericsWanted, input, wantedarg, qualiFound);
				if(!qualiFound.isEmpty()){
					for(GenericTypeAndQuali qualified : qualiFound){
						HashSet<Type> addTo;
						if(!qualifications.containsKey(qualified.q)) {
							addTo = new HashSet<Type>();
							qualifications.put(qualified.q, addTo);
						}else {
							addTo = qualifications.get(qualified.q);
						}
						addTo.add(qualified.quali);
					}
				}
			}
		}

		HashMap<GenericType, Type> ret = new HashMap<GenericType, Type>();
		for(GenericType key : qualifications.keySet()) {
			ErrorRaiseable ers = satc!=null?satc.getErrorRaiseableSupression():new DummyErrorRaiseable();
			//Type qual = TypeCheckUtils.getMoreGeneric(ers , satc, 0, 0, new ArrayList<Type>(qualifications.get(key).stream().filter(a -> !key.equals(a)).collect(Collectors.toList())), null, true);
			
			HashSet<Type> quals = qualifications.get(key);
			ArrayList<Type> cadis;
			if(quals.size() == 1) {
				cadis = new ArrayList<Type>(quals);
			}else {//filter out: T from T -> [F, T]
				cadis = new ArrayList<Type>(quals.stream().filter(a -> !key.equals(a)).collect(Collectors.toList()));
			}
			
			Type qual = TypeCheckUtils.getMoreGeneric(ers , satc, 0, 0, cadis, null, true);
			ret.put(key, qual);
		}
		
		
		return ret;
	}
	
	private Type transformGenerics(ScopeAndTypeChecker satc, Type preuref, List<Type> argsWanted){
		int refLevels = TypeCheckUtils.getRefLevels(preuref);
		if(refLevels > 0){
			preuref = TypeCheckUtils.getRefType(preuref);
		}
		
		//should be done here!
		//examine funcType to see if it has any locally defined generic types
		
		if(fromClassGenericToQualifiedType.isEmpty()){
			this.setupGenMap();
		}
		
		Map<GenericType, Type> genToDefined = new HashMap<GenericType, Type>();//fromClassGenericToQualifiedType
		
		if(preuref instanceof FuncType){
			FuncType asF = (FuncType)preuref;
			
			//Local generic types here
			
			if(asF.getLocalGenerics() != null && !asF.getLocalGenerics().isEmpty()){
				if(argsWanted !=null && !argsWanted.isEmpty()){
					//attempt to extract generic type from
					HashMap<GenericType, Type> found = qualifyGenericMethodTypes(satc, asF.getLocalGenerics(), asF.getInputs(), argsWanted);
					//add generic qualification to fromClassGenericToQualifiedType
					
					List<GenericType> fkeyset = new ArrayList<>(found.keySet());
					
					for(GenericType t: asF.getLocalGenerics()){
						Type obtain = found.get(t);//TODO: above doesnt actually do anything?

						if(obtain != null && obtain.hasArrayLevels()  ) {//if mapped onto something having array level, remove these from instance qualification
							//T[] -> String[]
							Type mapKey = fkeyset.get(fkeyset.indexOf(t));
							if(mapKey.hasArrayLevels()) {
								obtain = (Type)obtain.copy();
								obtain.setArrayLevels(0);
							}
						}
						genToDefined.put(t, obtain);
					}
				}
				else if(argsWanted == null){//if non defined, then just remove the locally defined generics so that they override the class level defined ones
					for(GenericType t: asF.getLocalGenerics()){
						genToDefined.remove(t);
					}
				}
				
			}
		}
		//(FuncType)...
		Type localMap = GenericTypeUtils.filterOutGenericTypes(preuref, genToDefined);
		/*if(localMap instanceof FuncType) {
			//((FuncType)localMap).localGenerics=null;
		}*/
		return GenericTypeUtils.filterOutGenericTypes(localMap, fromClassGenericToQualifiedType);
	}
	
	private TypeAndLocation genericizeAndLambdaizeTAL(ScopeAndTypeChecker satc, TypeAndLocation fdal, List<Type> argsWanted){
		Type preuref = fdal.getType();
		int refLevels = TypeCheckUtils.getRefLevels(preuref);
		Type got = transformGenerics(satc, preuref, argsWanted);
		
		if(got instanceof FuncType){
			FuncType gotTF = (FuncType)got.copy();
			if(pretendNothingAbstract){
				gotTF.setAbstarct(false);
			}
			gotTF.origin = this.classDef;
			Location loc = fdal.getLocation();
			TypeAndLocation tlam = gotTF.getLambdaDetails();
			if(null != tlam){
				Location ll = tlam.getLocation();
				if(null != ll){
					loc.setLambda(true);
					loc.setLambdaOwner(ll.getLambdaOwner());
				}
			}
			
			Type setType = refLevels > 0 ? TypeCheckUtils.makeRef(gotTF, refLevels): gotTF;

			return new TypeAndLocation(setType, loc);		
		}
		return null;
	}
	
	//// Scope frame interfacing stuff... Maybe a mega hack but meh
	public HashSet<TypeAndLocation> getFuncDef(int line, int col, String name, boolean searchSuperClass, List<Type> argsWanted, ArrayList<Pair<String, Type>> namessMap, ScopeAndTypeChecker errorRaiser){
		HashSet<TypeAndLocation> ret = new HashSet<TypeAndLocation>();
		if(null != this.classDef){
			
			HashMap<TypeAndLocation, HashSet<TypeAndLocation>> choicesMatchingFuncType= new HashMap<TypeAndLocation, HashSet<TypeAndLocation>>();//e.g. this(int[]) and this(X) where X is generic and qualified to int[] meaning you now have two choices which can take int[]
			
			for(TypeAndLocation fdal : this.classDef.getFuncDef(name, searchSuperClass, false, false)){ 
				List<Type> argsWantedx = TypeCheckUtils.mapFunctionParameterNamesToNewArguments(null, fdal.getType(), argsWanted, namessMap, 0, true, false);
				TypeAndLocation tal = genericizeAndLambdaizeTAL(errorRaiser, fdal, argsWantedx);
				if(tal != null){//TODO: why would this be null?
					if(!choicesMatchingFuncType.containsKey(tal)){
						choicesMatchingFuncType.put(tal, new HashSet<TypeAndLocation>());
					}
					
					choicesMatchingFuncType.get(tal).add(tal.cloneWithRetFuncType(fdal.getType() instanceof FuncType && ((FuncType)fdal.getType()).origonatingFuncDef != null?((FuncType)fdal.getType()).origonatingFuncDef.getFuncType():(fdal.getType())));
				}
			}
			
			
			for(TypeAndLocation val : choicesMatchingFuncType.keySet()){
				HashSet<TypeAndLocation> origs = choicesMatchingFuncType.get(val);
				if(origs.size() > 1){//we prioritize direct matches over indirect matches via super qualification
					HashMap<FuncType, TypeAndLocation> origToTal = new HashMap<FuncType, TypeAndLocation>();
					HashSet<FuncType> ftsToCompare = new HashSet<FuncType>();
					for(TypeAndLocation orig : origs){
						FuncType resTo = (FuncType)orig.getType();
						origToTal.put(resTo, orig);
						ftsToCompare.add(resTo);
					}
					
					if(null != errorRaiser){
						errorRaiser.maskErrors(true);
					}
					FuncType theOne = TypeCheckUtils.getMostSpecificFunctionForChoicesFT(errorRaiser, errorRaiser, ftsToCompare, argsWanted, namessMap, name, line, col, false, errorRaiser, false).getA();
					
					ArrayList<CapMaskedErrs> errs = errorRaiser.getmaskedErrors();//errors may relate to ambigious calls etc
					if(null != theOne){
						TypeAndLocation tal = origToTal.get(theOne);
						
						FuncType mapAgain = (FuncType)GenericTypeUtils.filterOutGenericTypes(theOne, fromClassGenericToQualifiedType);//TODO: is this line needed,
						ret.add(tal.cloneWithRetFuncType(mapAgain));
					}else{
						ret.add(val);
					}
					if(errorRaiser != null && !errs.isEmpty()){
						errorRaiser.applyMaskedErrors(errs);
						ret.clear();
						return ret;
					}
					
				}else{
					ret.add(val);
				}
			}
			
			
			
		}
		return ret;
	}
	
	
	
	public Set<FuncType> getConstructor(int line, int col, List<Type> typeArgsToMatch, ArrayList<Pair<String, Type>> namessMap, ErrorRaiseableSupressErrors invoker, ScopeAndTypeChecker errorRaiser)
	{//TODO: check generic stuff here is ok???... class MyClass[T] { fun MyClass(T hh){} }
		if(null != this.astredirect && this.astredirect instanceof NamedType){
			return ((NamedType)this.astredirect).getConstructor(line, col, typeArgsToMatch, namessMap, invoker, errorRaiser);
		}
		
		HashMap<FuncType, HashSet<FuncType>> choicesMatchingFuncType = new HashMap<FuncType, HashSet<FuncType>>();//e.g. this(int[]) and this(X) where X is generic and qualified to int[] meaning you now have two choices which can take int[]
		if(null != this.classDef){
			for(FuncType ft : this.classDef.getAllConstructors()){
				List<Type> argsWantedx = TypeCheckUtils.mapFunctionParameterNamesToNewArguments(null, ft, typeArgsToMatch, namessMap, 0, true, false);
				if(ft.getInputs().size() == argsWantedx.size()){
					FuncType mapped = (FuncType)GenericTypeUtils.filterOutGenericTypes(ft, fromClassGenericToQualifiedType);
					if(!choicesMatchingFuncType.containsKey(mapped)){
						choicesMatchingFuncType.put(mapped, new HashSet<FuncType>());
					}
					choicesMatchingFuncType.get(mapped).add(ft.origonatingFuncDef != null?ft.origonatingFuncDef.getFuncType():ft);
				}
			}
		}
		
		HashSet<FuncType> ret = new HashSet<FuncType>();
		
		for(FuncType val : choicesMatchingFuncType.keySet()){
			HashSet<FuncType> origs = choicesMatchingFuncType.get(val);
			if(origs.size() > 1){//we prioritize direct matches over indirect matches via super qualification
				
				errorRaiser.maskErrors(true);
				FuncType theOne = TypeCheckUtils.getMostSpecificFunctionForChoicesFT(errorRaiser, errorRaiser, origs, typeArgsToMatch, namessMap, "constructor", line, col, false, errorRaiser, false).getA();
				ArrayList<CapMaskedErrs> errs = errorRaiser.getmaskedErrors();//errors may relate to ambigious calls etc
				if(null != theOne){
					FuncType mapAgain = (FuncType)GenericTypeUtils.filterOutGenericTypes(theOne, fromClassGenericToQualifiedType);//TODO: is this line needed,
					ret.add(mapAgain);
				}else{
					ret.add(val);
				}
				if(!errs.isEmpty()){
					errorRaiser.applyMaskedErrors(errs);
				}
				
			}else{
				ret.add(val);
			}
		}
		
		
		HashSet<FuncType> toRemove = new HashSet<FuncType>();
		//filter out the versions from which those with synthetic parameters were derived
		for(FuncType ft : ret){
			FuncType filt = funcTypeWithoutSyntheticInputArgs(ft);
			if(!ft.equals(filt) || anyArgInitUncreatable(ft)){
				toRemove.add(filt);
			}
		}
		
		ret.removeAll(toRemove);
		
		return ret;
	}
	
	
	private static boolean anyArgInitUncreatable(FuncType ft) {
		return ft.inputs.stream().anyMatch(a -> a.equals(ScopeAndTypeChecker.const_initUncreatable));
	}
	
	
	
	
	
	
	
	
	
	
	private FuncType funcTypeWithoutSyntheticInputArgs(FuncType car){
		ArrayList<Type> inputs = new ArrayList<Type>(car.inputs.size());
		
		int n=0;
		for(FuncParam fp : car.origonatingFuncDef.params.params){
			if(fp.sytheticDefinitionLevel == null){
				/*if(null == fp.type){
					inputs.add(null);
				}else{
					Type tt = (Type)fp.type.copy();
					if(fp.isVararg){
						tt.setArrayLevels(tt.getArrayLevels()+1);
					}
					inputs.add(tt);
				}*/
				inputs.add(car.inputs.get(n));
			}
			n++;
		}
		
		return  new FuncType(inputs, car.retType);
	}
	
	/*public List<FuncType> getAllConstructors(){
		ArrayList<FuncType> ret;
		if(this.classDef != null){
			HashSet<FuncType> got = this.classDef.getAllConstructors();
			ret = new ArrayList<FuncType>(got.size());
			for(FuncType ft : got){
				ret.add((FuncType)transformGenerics(ft, ft.getInputs()));
			}
		}else{
			ret = new ArrayList<FuncType>(0);
		}
		
		return ret;
	}*/
	
	public List<FuncType> getAllConstructors(ScopeAndTypeChecker satc){
		if(this.classDef != null){
			HashSet<FuncType> got = this.classDef.getAllConstructors();
			HashSet<FuncType> ret = new HashSet<FuncType>();
			for(FuncType ft : got){
				ret.add((FuncType)transformGenerics(satc, ft, ft.getInputs()));
			}
			return new ArrayList<FuncType>(ret);
		}else{
			return new ArrayList<FuncType>(0);
		}
	}
	
	public List<FuncType> getAllConstructorsExcludeHiddenArgs(ScopeAndTypeChecker satc){
		List<FuncType> choices = getAllConstructors(satc);
		List<FuncType> ret = new ArrayList<FuncType>(choices.size());
		
		for(FuncType cc : choices){
			boolean valid = true;
			int inputsize = cc.inputs.size();
			ArrayList<FuncParam> pparams = cc.origonatingFuncDef.getParams().params;
			if(!pparams.isEmpty()){//empty cannot be any hidden params
				for(int n = 0; n < inputsize; n++){
					FuncParam fp = pparams.get(n);
					if(fp.name.contains("$n") || fp.hasSyntheticParamAnnotation() ){
						valid=false;
						break;
					}
				}
			}
			
			if(valid){
				ret.add(cc);
			}
		}
		
		return ret;
	}
	
	public Type getField(String name) {
		List<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>> searcgh =  getAllFields(false);
		for(Sixple<String, Type, Boolean, AccessModifier, Boolean, String> inst : searcgh) {
			if(inst.getA().equals(name)) {
				return inst.getB();
			}
		}
		return null;
	}
	
	public List<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>> getAllFields(boolean onlytraits){
		List<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>> ret;
		if(this.classDef != null) {
			ArrayList<Sevenple<String, Type, Boolean, AccessModifier, ClassDef, Boolean, String>> allf = this.classDef.getAllFields(onlytraits);
			ret = new ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>>(allf.size());
			
			for(Sevenple<String, Type, Boolean, AccessModifier, ClassDef, Boolean, String> item : allf) {
				Type tt = item.getB();
				tt = GenericTypeUtils.filterOutGenericTypes(tt, fromClassGenericToQualifiedType);
				
				ret.add(new Sixple<String, Type, Boolean, AccessModifier, Boolean, String>(item.getA(), tt, item.getC(), item.getD(), item.getF(), item.getG()));
			}
		}else {
			ret = new ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>>(0); 
		}
		
		return ret;
	}
	
	public HashMap<Pair<String, TypeAndLocation>, HashSet<NamedType>> getAllMethodOrigins(ScopeAndTypeChecker satc) {
		return getAllMethodOrigins(satc, false);
	}
	
	public HashMap<Pair<String, TypeAndLocation>, HashSet<NamedType>> getAllMethodOrigins(ScopeAndTypeChecker satc, boolean onlyNonAbstract ) {
		//origin to typedef and location
		HashMap<Pair<String, TypeAndLocation>, HashSet<NamedType>> methodToOrigins = new HashMap<Pair<String, TypeAndLocation>, HashSet<NamedType>>();
		for(Pair<String, TypeAndLocation> method : this.getAllLocallyDefinedMethods(satc, false, onlyNonAbstract)) {
			HashSet<NamedType> origins = new HashSet<NamedType>();
			origins.add(this);
			methodToOrigins.put(method, origins);
		}
		
		
		NamedType superInst = this.classDef.getSuperAsNamedType(0, 0);
		while(superInst != null) {
			methodToOrigins.putAll(superInst.getAllMethodOrigins(satc));
			superInst = superInst.getResolvedSuperTypeAsNamed();
		}
		Set<Pair<String, TypeAndLocation>> methodsDefinedHere = new HashSet<Pair<String, TypeAndLocation>>(methodToOrigins.keySet());
		
		for(NamedType trait : this.getResolvedTraitsAsNamed()) {
			if(null != trait) {
				HashMap<Pair<String, TypeAndLocation>, HashSet<NamedType>> traitM2o = trait.getAllMethodOrigins(satc, true);
				//merge into one
				for(Pair<String, TypeAndLocation> methd : traitM2o.keySet()) {
					if(methodsDefinedHere.contains(methd)) {
						continue;//already defined so dont include
					}
					
					HashSet<NamedType> locs = methodToOrigins.get(methd);
					if(locs == null) {
						locs = new HashSet<NamedType>();
						methodToOrigins.put(methd, locs);
					}
					locs.addAll(traitM2o.get(methd));
				}
			}
		}
		
		//xxx = xxx.stream().filter(a -> !((FuncType)a.getB().getType()).isAbstarct() ).collect(Collectors.toList());
		
		return methodToOrigins;
	}
	
	public List<Pair<String, TypeAndLocation>> getAllMethods(ScopeAndTypeChecker satc){
		
		HashMap<String, Pair<String, TypeAndLocation>> meths = new HashMap<String, Pair<String, TypeAndLocation>>();
		
		List<NamedType> parents = new ArrayList<NamedType>(1);
		parents.add(this.getResolvedSuperTypeAsNamed());
		parents.addAll(this.getResolvedTraitsAsNamed());
		for(NamedType parent : parents) {
			if(null != parent) {
				List<Pair<String, TypeAndLocation>> fromParent = parent.getAllMethods(satc);
				for(Pair<String, TypeAndLocation> toAdd : fromParent) {
					meths.put(toAdd.getA() + toAdd.getB().getType().getGenericBytecodeType(), toAdd);
				}
			}
		}

		List<Pair<String, TypeAndLocation>> ret = new ArrayList<Pair<String, TypeAndLocation>>();
		
		if(this.classDef != null) {
			for(Pair<String, TypeAndLocation> item : this.classDef.getAllMethods(false)) {
				meths.put(item.getA() + item.getB().getType().getGenericBytecodeType(), item);
			}
			
			for( String key : meths.keySet()){
				Pair<String, TypeAndLocation> xx = meths.get(key);
				TypeAndLocation tal = xx.getB();
				tal = genericizeAndLambdaizeTAL(satc, tal, ((FuncType)tal.getType()).getInputs());
				ret.add(new Pair<String, TypeAndLocation>(xx.getA(), tal));
			}
		}
		
		
		return ret;
	}
	
	public List<Pair<String, TypeAndLocation>> getAllLocallyDefinedMethods(ScopeAndTypeChecker satc, boolean onlyAbstract, boolean onlyNonAbastract){
		List<Pair<String, TypeAndLocation>> xxx = this.classDef.getAllLocallyDefinedMethods();
		
		if(onlyAbstract) {
			//filter out those which are defined by supertype - and are therefore already
			NamedType sup = this.getResolvedSuperTypeAsNamed();
			if(null == sup) {
				sup = ScopeAndTypeChecker.const_object;
			}
			
			List<Pair<String, TypeAndLocation>> allsupers = sup.getAllMethods(satc);
			List<Pair<String, TypeAndLocation>> allnonAbstractupsers = allsupers.stream().filter(a -> !((FuncType)a.getB().getType()).isAbstarct() ).collect(Collectors.toList());
			
			xxx = xxx.stream().filter(a -> !allnonAbstractupsers.contains(a) && ((FuncType)a.getB().getType()).isAbstarct() ).collect(Collectors.toList());
		}else if(onlyNonAbastract) {
			xxx = xxx.stream().filter(a -> !((FuncType)a.getB().getType()).isAbstarct() ).collect(Collectors.toList());
		}
		
		List<Pair<String, TypeAndLocation>> ret = new ArrayList<Pair<String, TypeAndLocation>>(xxx.size());
		for(Pair<String, TypeAndLocation> xx : xxx){
			TypeAndLocation tal = xx.getB();
			tal = genericizeAndLambdaizeTAL(satc, tal, ((FuncType)tal.getType()).getInputs());
			ret.add(new Pair<String, TypeAndLocation>(xx.getA(), tal));
		}
		
		return ret;
	}
	
	
	
	
	/*public boolean hasFuncDef(String name)
	{
		HashSet<TypeAndLocation> ret = getFuncDef(name, null, null);
		return null != ret && !ret.isEmpty();
	}*/
	
	public TypeAndLocation getVariable(String name)
	{
		return getVariable(name, true);
	}
	
	public HashSet<String> getEnumVars(ScopeAndTypeChecker satc){
		//HashMap<String, TypeAndLocation> ret = new HashMap<String, TypeAndLocation>();
		HashSet<String> ret = new HashSet<String>();
		
		if(null != this.classDef){
			HashMap<String, TypeAndLocation> pubz = this.classDef.getAllPublicVars(null);

			for(String key : pubz.keySet()){
				TypeAndLocation tal = pubz.get(key);
				if(null != TypeCheckUtils.checkSubType(satc.getErrorRaiseableSupression(), this, tal.getType())){
					ret.add(key);
				}
			}
		}
		
		return ret;
	}
	
	public TypeAndLocation getVariable(String name, boolean searchParent)
	{
		if(null == this.classDef){
			return null;
		}
		
		TypeAndLocation tandl = this.classDef.getVariable(null, name, searchParent, false);
		Type foundvar = null==tandl?null:tandl.getType();
		
		if(foundvar==null){//could be a inner class?
			ClassDef got = this.classDef.getClassDef(name);
			if(got != null && got.isEnum){
				NamedType tt = new NamedType(got);
				LocationStaticField lsf = new LocationStaticField(null, tt);
				lsf.setAccessModifier(got.accessModifier);
				return new TypeAndLocation(tt, lsf);
			}
		}
		
		foundvar = GenericTypeUtils.filterOutGenericTypes(foundvar, fromClassGenericToQualifiedType);
		
		return foundvar==null?null:new TypeAndLocation(foundvar, tandl==null?null:tandl.getLocation());
	}
	
	public boolean hasVariable(String name, boolean searchParent)
	{
		return null != getVariable(name, searchParent);
	}
	
	public boolean hasVariable(String name)
	{
		return null != getVariable(name);
	}
	
	public ClassDef getClassDef(String name)
	{
		return null == this.classDef? null:this.classDef.getClassDef(name);
		//if has genertic types, extended make sure they get carried through on upper bound
		//TODO: generic subtype should respect upper bound from inheritec generic type: class A[T extends List]{ class Sub[TT extends T]{}};
	}
	
	public boolean hasClassDef(String name)
	{
		return null != getClassDef(name);
	}
	

	private static final ClassDef obj = new ClassDefJava(java.lang.Object.class);
	@Override
	public String getBytecodeTypeWithoutArray( ) {
		if(printNameInBytecodeRepresentation){
			return "L"+this.namedType+";";
		}
		
		if(origonalGenericTypeUpperBound != null){//generic upper bound takes precidence over anything else
			return origonalGenericTypeUpperBound.getSetClassDef().javaClassName();
		}
		
		if(this.hasArrayLevels() && this.getIsRef()){
			return "Lcom/concurnas/bootstrap/runtime/ref/LocalArray;";
		}
		
		ClassDef toGetJavaName;
				
		if(classDef == null){
			if(this.namedType.endsWith("$$ActorIterface")){//JPT: mega hack so we dont have to gennerate the actor code at compilation time
				return "L" + this.namedType + ";";
			}
			
			toGetJavaName=obj;
		}
		else{
			toGetJavaName=classDef;
		}
		
		/*if(!this.genTypes.isEmpty()){
			String s = toGetJavaName.javaClassName();
			StringBuilder sb = new StringBuilder(s.substring(0, s.length()-1));
			//e.g. Lcom/concurnas/runtime/ref/Local<Ljava/lang/String;Ljava/util/List<Ljava/lang/Integer;>
			sb.append("<");
			for(Type tt: this.genTypes){
				sb.append(tt.getGenericBytecodeType());
			}
			sb.append(">;");
			return sb.toString();
		}
		else{*/
			return toGetJavaName.javaClassName();
		//}
	}
	
	@Override
	public final String getBytecodeType()
	{
		String xxx =  getBytecodeType(true);
		return xxx;
	}
	
	
	protected String prependArrayLevels()
	{
		if(this.hasArrayLevels() && this.getIsRef()){
			return "";
		}
		return super.prependArrayLevels();
	}
	
	
	public final String getBytecodeType(boolean includeArrays )
	{//overrideden because if there is upperbound then we don;t permit array levels		
		
		/*if(unresolveableLocalType){
			return this.namedType;
		}*/
		
		StringBuilder sb = new StringBuilder();
		
		NamedType upperBound = getOrigonalGenericTypeUpperBoundRaw();
		
		if(upperBound != null){
			if(includeArrays){
				for(int n =0; n < upperBound.getArrayLevels(); n++)
				{
					sb.append("[");
				}
			}
			
 			return  sb + getBytecodeTypeWithoutArray();
		}
		else{
			if(includeArrays){
				sb.append(prependArrayLevels());
			}
			sb.append(getBytecodeTypeWithoutArray());
			return  sb.toString();
		}
	}

	@Override
	public String getGenericBytecodeTypeWithoutArray() {
		if(this.fromisWildCardAny) {
			return "*";
		}
		
		String ret = getBytecodeType(false);
		
		if(this.hasArrayLevels() && this.getIsRef()){
			StringBuilder sb = new StringBuilder(ret.substring(0, ret.length()-1));
			sb.append('<');
			
			Type me2 = this.copyTypeSpecific();
			me2.setArrayLevels(0);
			//int[]:[] -> localarray<local<int[]>>
			sb.append(me2.getGenericBytecodeType());
			
			sb.append(">;");
			ret = sb.toString();
		}
		else if(!genTypes.isEmpty())
		{
			StringBuilder sb = new StringBuilder(ret.substring(0, ret.length()-1));
			sb.append('<');
			for(Type t : genTypes)
			{
				sb.append(t.getGenericBytecodeType());
			}
			sb.append(">;");
			ret = sb.toString();
		}
		return ret;
		/*}*/
		
	}
	
	@Override
	public String getCheckCastType() {
		/*
		Integer[] ->
		mv.visitTypeInsn(CHECKCAaST, "[Ljava/lang/Integer;");

		Integer ->
		mv.visitTypeInsn(CHECKCASaT, "java/lang/Integer");
		
		OOg<String> ->
		CHECaKCAST a/com/C$OOg
		
		OOg<String>[] ->
		CHEaCKCAST [La/com/C$OOg;
		*/
		
		String ret = null!=classDef? classDef.javaClassName() : "Ljava/lang/Object;";
		
		if(this.hasArrayLevels())
		{
			if(this.getIsRef()){
				ret= "com/concurnas/bootstrap/runtime/ref/LocalArray";
			}
			else{
				StringBuilder prePendArLevels = new StringBuilder();
				for(int n =0; n < this.arrayLevels; n++){
					prePendArLevels.append("[");
				}
				prePendArLevels.append(ret);
				ret = prePendArLevels.toString();
			}
		}
		else
		{
			ret = ret.substring(1, ret.length() -1);
		}
		
		return ret;
	}

	public HashMap<GenericType, Type> getFromClassGenericToQualifiedType() {
		return this.fromClassGenericToQualifiedType;
	}

	public void setFromClassGenericToQualifiedType(HashMap<GenericType, Type> hashMap) {
		this.fromClassGenericToQualifiedType = hashMap;
	}
	
	@Override
	public String getJavaSourceType() {
		String ret = namedType;
		
		if(null == ret){
			if(this.getIsRef()){
				Type first = this.genTypes.get(0);
				if(first instanceof NamedType){
					ret = ((NamedType)first).getJavaSourceType();
					if(this.lockedAsRef){
						ret += ":";
					}
				}
			}
		}
		
		if(this.hasArrayLevels())
		{
			int arLevels = this.getArrayLevels();
			for(int n =0; n < arLevels; n++){
				ret += "[]";
			}
		}
		return ret;
	}
	
	@Override
	public String getJavaSourceTypeNoArray() {
		return namedType;
	}
	
	public void overrideRefType(Type refee){
		NamedType thing = (NamedType)TypeCheckUtils.getRefTypeLastRef(this);
		thing.genTypes.set(0, refee);
	}
	
	public boolean getIsRef(){
		boolean isR = this.isRef;
		if( !isR ){
			ClassDef set = this.getSetClassDef();
			
			if(set instanceof ClassDefJava &&  CompiledClassUtils.refCls.isAssignableFrom( ((ClassDefJava)set).getClassHeld() ))
			{
				isR=true;
			}
		}
		
		return isR && this.genTypes != null && this.genTypes.size()>0;// && !this.hasArrayLevels();
	}

	public void setIsRef(boolean b) {
		this.isRef=b;
		//this.lockedAsRef=b;
	}
	
	public boolean getLockedAsRef(){
		return lockedAsRef;
	}
	
	public void setLockedAsRef(boolean b) {
		lockedAsRef = b;
	}

	/**
	 * Returns generic version of NamedType, e.g. from MyClass<String, ArrayList<String>> -> MyClass<X, Y>
	 * @return
	 */
	public NamedType getGenericVersion() {
		NamedType nt = this.copyTypeSpecific();
		/*
		HashMap<Type, GenericType> invert = new HashMap<Type, GenericType>();
		for(GenericType gt : nt.fromClassGenericToQualifiedType.keySet()){
			Type tt = nt.fromClassGenericToQualifiedType.get(gt);
			invert.put(tt, gt);
		}
				
		ArrayList<Type> newgens = new ArrayList<Type>(nt.genTypes.size());
		for(Type from : nt.genTypes){
			newgens.add(invert.get(from));
		}
		*/
		
		ClassDef mycls = this.getSetClassDef();
		if(null == mycls){
			return null;
		}
		else{
			nt.genTypes = new ArrayList<Type>(mycls.classGenricList);
			nt.fromClassGenericToQualifiedType = new HashMap<GenericType, Type>();
			
			mycls = mycls.getParentNestor();
			while(null != mycls){
				for(Type t: mycls.classGenricList){
					nt.fromClassGenericToQualifiedType.put((GenericType)t, t);
				}
				mycls = mycls.getParentNestor();
			}
			
			return nt;
		}
	}
	
	public boolean allGenericTypesExistant(){
		//ensure no null in generic types, e.g. calling a constructor which doesnt take all the generics passed
		boolean ok = true;
		for(Type thing : this.genTypes){
			if(thing == null){
				ok = false;
				break;
			}
		}
		return ok;
	}

	
	public Type getTaggedType(){
		if(this.astredirect != null){
			return ((Node)astredirect).getTaggedType();
		}
		return this;
	}


	@Override
	public NamedType getOrigonalGenericTypeUpperBound() {
		return origonalGenericTypeUpperBound;//avoid hack in AbstractType concerning this being null
	}

	@Override
	public Type copyIgnoreReturnTypeAndGenerics() {
		NamedType ret = this.copyTypeSpecific();
		ret.genTypes = new ArrayList<Type>(0);
		return ret;
	}
	@Override
	public Type copyIgnoreReturnType() {
		return this.copyTypeSpecific();
	}

	public List<Pair<String, TypeAndLocation>> getAllStaticAssets() {
		if(this.classDef != null) {
			return this.classDef.getAllStaticAssets();
		}else {
			return new ArrayList<Pair<String, TypeAndLocation>>(0);
		}
	}
}


