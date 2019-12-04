package com.concurnas.compiler.ast;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.misc.OrderedHashSet;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.bytecode.FuncLocation.ClassFunctionLocation;
import com.concurnas.compiler.typeAndLocation.Location;
import com.concurnas.compiler.typeAndLocation.LocationClassField;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.CompiledClassUtils;
import com.concurnas.compiler.utils.Fiveple;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.utils.GenericTypeUtils;
import com.concurnas.compiler.utils.ITEM_TYPE;
import com.concurnas.compiler.utils.Sixple;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.visitors.datastructs.TheScopeFrame;
import com.concurnas.compiler.visitors.util.ErrorRaiseableSupressErrors;
import com.concurnas.compiler.visitors.util.TraitFieldEncoderDecoder;
import com.concurnas.lang.NoNull;
import com.concurnas.lang.NoNull.When;
import com.concurnas.lang.Trait;
import com.concurnas.lang.TraitField;
import com.concurnas.lang.util.LRUCache;
import com.concurnas.runtime.Pair;

public class ClassDefJava extends ClassDef {
	
	private Class<?> cls;
	private ClassDef encloser = null;
	private boolean isRootObj = false;
	private final boolean isStatic;
	
	private static final Class<java.lang.Object> obj = java.lang.Object.class;

	public ClassDefJava(int line, int col, Class<?> cls)
	{
		this(line, col, cls, false);
	}
	public ClassDefJava(int line, int col, Class<?> cls, boolean dontInvestigateInterFacesTypes)
	{
		super(line, col);
		this.cls = cls;
		
		
		
		try{
			//MainLoop.mainClassLoader.loadClass(cls.getName());
			Thread.currentThread().getContextClassLoader().loadClass(cls.getName());
			this.javaSystemLib=true;
		}
		catch(ClassNotFoundException cnf){
			this.javaSystemLib = false;
		}
				
		isRootObj = this.cls.equals(obj);
		
		Package pckg = this.cls.getPackage();
		if(this.cls.isArray())
		{
			pckg = this.cls.getComponentType().getPackage();
		}
		
		if(pckg == null) {
			//throw new RuntimeException("No package defined for: " + this.cls.getName());
		}
		
		super.packageName = pckg==null?"":""+ pckg.getName();
		setGenericList(dontInvestigateInterFacesTypes);
		
		int modifiers = this.cls.getModifiers();
		isStatic = Modifier.isStatic(modifiers);
		
		if(Modifier.isPublic(modifiers)) {
			this.accessModifier = AccessModifier.PUBLIC;
		}
		else if(Modifier.isPrivate(modifiers)) {
			this.accessModifier = AccessModifier.PRIVATE;
		}
		else if(Modifier.isProtected(modifiers)) {
			this.accessModifier = AccessModifier.PROTECTED;
		}else {
			this.accessModifier = AccessModifier.PACKAGE;
		}
		
		this.className = this.cls.getSimpleName();
		
		//this.accessModifier
		
		if(!isStatic){
			Class<?> enc = cls.getEnclosingClass();
			if(null != enc)
			{//if this is an inner class
				encloser = new ClassDefJava(enc);
			}
		}
		
		
		this.isEnum = this.cls.isEnum();
		
		this.isTrait = this.cls.isInterface();
	}
	
	public ClassDefJava(Class<?> cls)
	{
		this(0,0, cls);
	}

	public ClassDefJava(Class<?> cls, boolean javaSystemLib){
		this(cls);
		this.javaSystemLib = javaSystemLib;
	}
	
	public ClassDefJava(boolean dontInvestigateInterFacesTypes, Class<?> cls){
		this(0,0, cls, dontInvestigateInterFacesTypes);
	}
	
	public String bcFullName()
	{
		return cls.getName().replace('.','/');//TODO: check this on compiled inner classes
	}
	
	private HashMap<Class<?>, ArrayList<Type> > interfaceToGens;
	
	private void setGenericList(boolean dontInvestigateInterFacesTypes)
	{
		this.classGenricList = new ArrayList<GenericType>();
		TypeVariable<?>[] tt = this.cls.getTypeParameters();
		
		
		
		int n=0;
		for(TypeVariable<?> t : tt)
		{//TODO: add upper and lower bounds
			GenericType gen = new GenericType(this.getLine(), this.getColumn(), t.getName(), n++);
			gen.setNullStatus(NullStatus.UNKNOWN);
			
			java.lang.reflect.Type[] bounds = t.getBounds();
			if(bounds.length > 0) {
				java.lang.reflect.Type upperbound = bounds[0];
				if(upperbound instanceof Class<?>) {
					gen.upperBound = new NamedType(new ClassDefJava((Class<?>)upperbound)); 
					//gen.upperBound = (NamedType)CompiledClassGenericUtils.convertGenType(upperbound, nameToGenericMap, false);
				}else {
					boolean ignore = false;
					if(upperbound instanceof ParameterizedType) {
						java.lang.reflect.Type rt = ((ParameterizedType)upperbound).getRawType();
						if(rt instanceof Class<?>) {
							ignore = rt.equals(Enum.class) || rt.equals(this.cls);
						}
					}
					if(!ignore) {
						gen.upperBound = (NamedType)CompiledClassUtils.convertGenType(upperbound, nameToGenericMap, false);
					}
				}
			}
			
			NoNull nn = t.getAnnotation(com.concurnas.lang.NoNull.class);
			if(null != nn) {
				When when = nn.when();
				if( when == When.ALWAYS) {
					gen.setNullStatus(NullStatus.NONNULL);
				}else if(when == When.NEVER) {
					gen.setNullStatus(NullStatus.NULLABLE);
				}
			}
			
			classGenricList.add(gen);
			this.nameToGenericMap.put(t.getName(), gen); //TODO: when child class repoints generics, it may mess this up a bit
		}
		
		com.concurnas.lang.internal.NullStatus ns = cls.getAnnotation(com.concurnas.lang.internal.NullStatus.class);
		if(null != ns) {
			CompiledClassUtils.tagTypesWithNullable((ArrayList<Type>)(Object)classGenricList, null, ns.nullable());
		}
		
		//TODO: what if the superclass is a concurnas class not already loaded?
		
		//super.superClassGenricList = new ArrayList<Type>();
		if(!dontInvestigateInterFacesTypes)
		{//set dontInvestigateInterFacesTypes true to avoid inf loop on: Int -> Comparable<Int>
		
			Class<?> sup	=	this.cls.getSuperclass();
			if(null != sup){
				java.lang.reflect.Type supType = this.cls.getGenericSuperclass();
				if(supType instanceof ParameterizedType){
					super.superClassGenricList = CompiledClassUtils.extracQualifiedGenTypes((ParameterizedType)supType, nameToGenericMap, true);
				}
			}

			java.lang.reflect.Type[] ifaces = this.cls.getGenericInterfaces();
			if(null != ifaces && ifaces.length >0 ){
				interfaceToGens = new HashMap<Class<?>, ArrayList<Type>>();
				for(java.lang.reflect.Type supType: ifaces ){
					if(supType instanceof ParameterizedType){
						ParameterizedType asPar = (ParameterizedType)supType;
						interfaceToGens.put((Class<?>)asPar.getRawType(), CompiledClassUtils.extracQualifiedGenTypes((ParameterizedType)supType, nameToGenericMap, true));
					}
				}
			}
		}
		
		
	}
	
	public List<Pair<String, FuncType>> getAbstractMethods()
	{
		return CompiledClassUtils.getAllAbstractFunctionsFromCls(this.cls, nameToGenericMap);
		
	}
	
	public Class<?> getClassHeld()
	{
		return this.cls;
	}
	
	@Override
	public boolean isChangeable() {
		return true;
	}

	@Override
	public String getPrettyName()
	{
		return cls.getName();
	}
	
	@Override
	public String toString()
	{
		return this.getPrettyName();
	}

	public String javaClassName()
	{
		return "L" + this.toString().replace('.', '/') +";";
	}
	
	@Override
	public ClassDef getSuperclass()
	{
		if(isRootObj)
		{
			return null;
		}
		
		Class<?> sup = cls.getSuperclass();
		if(null == sup)
		{
			if(cls.isInterface()) {
				Trait traitAnnot = cls.getAnnotation(Trait.class);
				if(null!= traitAnnot) {
					sup = traitAnnot.annotationType();
				}else {
					return null;
				}
			}else {
				return null;
			}
		}
		return new ClassDefJava(sup);
	}
	
	private static HashSet<ClassDef> transIFaceExtract(Class<?> cls, boolean inctrans){
		HashSet<ClassDef> ret = new OrderedHashSet<ClassDef>();
		for(Class<?> i: cls.getInterfaces() )
		{
			ret.add(new ClassDefJava(i));
			ret.addAll(transIFaceExtract(i, inctrans));
		}
		
		if(inctrans) {
			Class<?> sup = cls.getSuperclass();
			if(sup != null) {
				ret.addAll(transIFaceExtract(sup, inctrans));
			}
		}
		
		
		return ret;
	}
	
	@Override
	public HashSet<ClassDef> getTraits() {
		if(!isRootObj){	
			return transIFaceExtract(cls, false);
		}
		
		return new OrderedHashSet<ClassDef>();
	}
	
	@Override
	public HashSet<ClassDef> getTraitsIncTrans() {
		if(!isRootObj){	
			return transIFaceExtract(cls, true);
		}
		
		return new OrderedHashSet<ClassDef>();
	}
	
	@Override
	protected ArrayList<Pair<ClassDef, List<Type>>> getTraitsMapGens(boolean includeInterfaces) {

		ArrayList<Pair<ClassDef, List<Type>>> ret = new ArrayList<Pair<ClassDef, List<Type>>>();
		if(null != interfaceToGens) {
			for(ClassDef cd : getResolvedInterfaces()){
				if(includeInterfaces || classHasTraitAnnotation(cd)) {
					ret.add(new Pair<ClassDef, List<Type>>(cd, interfaceToGens.get(((ClassDefJava)cd).cls)));
				}
			}
		}
				
		return ret;
	}
	
	private static final HashSet<Class<?>> ignoredInterfaceClasses = new HashSet<Class<?>>();
	static {
		try {
			ignoredInterfaceClasses.add(Class.forName("java.lang.constant.Constable"));
			ignoredInterfaceClasses.add(Class.forName("java.lang.constant.ConstantDesc"));
		}catch(Exception e) {
			
		}
	}
	
	@Override
	public ArrayList<ClassDef> getResolvedInterfaces(){
		ArrayList<ClassDef> ret = new ArrayList<ClassDef>();
		if(!isRootObj){
			for(Class<?> i: cls.getInterfaces() ){
				if(ignoredInterfaceClasses.contains(i)) {
					continue;
				}
				ret.add( new ClassDefJava(i) );
			}
		}
		
		return ret;
	}
	
	public boolean isInstantiable()
	{
		return !this.cls.isInterface() && !getIsAbstract();
	}

	@Override
	public boolean getIsAbstract(){
		return Modifier.isAbstract( this.cls.getModifiers() );
	}
	
	public boolean isInterface()
	{
		return this.cls.isInterface();//TODO: interfaces
	}
	
	@Override
	public boolean isfinal()
	{
		return Modifier.isFinal(this.cls.getModifiers());
	}
	
	/////////////////
	
	
	///
	//// Scope frame interfacing stuff... Maybe a mega hack but meh
	///

/*	@Override
	public boolean hasNoArgConstructor()
	{
		return !this.getConstructor(0, null).isEmpty();
	}*/
	
	@Override
	public HashSet<FuncType> getConstructor(int typeArgsToMatch, ErrorRaiseableSupressErrors invoker){
		return CompiledClassUtils.getConstructor(this.cls,  typeArgsToMatch, invoker, nameToGenericMap);
	}
	
	@Override
	public HashSet<FuncType> getAllConstructors(){
		//return this.myScopeFrame.getConstructor(null);
		return CompiledClassUtils.getAllConstructors(this.cls, nameToGenericMap);
	}
	
	@Override
	public ArrayList<ClassDef> getAllNestedClasses(){
		return CompiledClassUtils.getAllDeclaredNestedClasses(this.cls);
	}
	
	@Override
	public ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>> getAllFieldsDefined() {
		ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>> ret = new ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>>();
		
		if(this.isTrait) {
			Annotations annots = this.getAnnotations();
			if(null != annots) {
				Annotation annot = annots.getAnnotation("com.concurnas.lang.Trait");
				if(null != annot) {
					for(Pair<String, Expression> inst : annot.manyArgs) {
						if(inst.getA().equals("traitFields")) {
							Object traitFields = inst.getB().getFoldedConstant();
							if(traitFields instanceof TraitField[]) {
								for(TraitField tf : (TraitField[])traitFields) {
									String fname = tf.fieldName();
									boolean isAbastract = tf.isAbstract();
									Type tt = TraitFieldEncoderDecoder.decode(tf.fieldType()); 
									AccessModifier am = AccessModifier.getAccessModifier(tf.accessModifier());
									
									ret.add(new Sixple<String, Type, Boolean, AccessModifier, Boolean, String>(fname, tt, isAbastract, am, false, null));
								}
								return ret;
							}
						}
					}
				}
			}
		}
		
		ArrayList<Fiveple<String, Type, Boolean, String, AccessModifier>> allfields = CompiledClassUtils.getAllFields(cls, nameToGenericMap);
		
		allfields.forEach(a -> ret.add(new Sixple<String, Type, Boolean, AccessModifier, Boolean, String>(a.getA(), a.getB(), false, a.getE(), a.getC(), a.getD())));
		
		return ret;
	}
	
	
	@Override
	public HashSet<TypeAndLocation> getFuncDef(String name, boolean ignoreLambdas, boolean ExtensionFunctions)
	{
		return getFuncDef(name, true, true, ExtensionFunctions);
	}
	
	private Map<Fourple<String, Boolean, Boolean, Boolean>, HashSet<TypeAndLocation>> getFuncDefCache = Collections.synchronizedMap(new LRUCache<Fourple<String, Boolean, Boolean, Boolean>, HashSet<TypeAndLocation>>(1000));
	
	@SuppressWarnings("unchecked")
	@Override
	public HashSet<TypeAndLocation> getFuncDef(String name, boolean searchSuperClass, boolean ignoreLambdas, boolean extensionFunctions)
	{
		Fourple<String, Boolean, Boolean, Boolean> key = new Fourple<String, Boolean, Boolean, Boolean>(name, searchSuperClass, ignoreLambdas, extensionFunctions);
		HashSet<TypeAndLocation> ret;
		if(getFuncDefCache.containsKey(key)) {
			ret = getFuncDefCache.get(key);
		}else {
			ret = new HashSet<TypeAndLocation>();
			
			{
				int n=0;
				for(ClassDef iface : this.getTraitsIncTrans()) {
					HashSet<TypeAndLocation> genMapped = new HashSet<TypeAndLocation>();
					for(TypeAndLocation tal : iface.getFuncDef(name, searchSuperClass, ignoreLambdas, extensionFunctions)) {
						Class<?> keyx = ((ClassDefJava)iface).cls;
						if(null != interfaceToGens && interfaceToGens.containsKey(keyx)) {
							Type tt = tal.getType();
							if(tt instanceof FuncType) {
								ArrayList<Type> mapsTo = interfaceToGens.get(keyx);
								int mp2Size = mapsTo.size();
								if(iface.classGenricList.size() == mp2Size) {
									Map<Type, Type> genMappiong = new HashMap<Type, Type>();
									for(int nx=0; nx < mp2Size; nx++) {
										genMappiong.put(iface.classGenricList.get(nx), mapsTo.get(nx));
									}
									
									FuncType asFT = (FuncType) GenericTypeUtils.mapFuncTypeGenericsToOtherGenerics(tt, genMappiong, false);
									tal = tal.cloneWithRetFuncType(asFT);
								}
								
							}
						}
						genMapped.add(tal);
					}
					
					
					ret.removeAll( genMapped );
					ret.addAll( genMapped );
					n++;
				}
			}
			
			
			ClassDef superCls = this.getSuperclass();
			if(null != superCls) {
				HashSet<TypeAndLocation> fromSuper = superCls.getFuncDef(name, searchSuperClass, ignoreLambdas, extensionFunctions);
				//ret.addAll( fromSuper.stream().filter(a -> !((FuncType)a.getType()).isAbstarct()).collect(Collectors.toList()) );
				ret.removeAll( fromSuper );
				
				if(superCls.accessModifier != AccessModifier.PUBLIC) {
					HashSet<TypeAndLocation> newfromSuper = new HashSet<TypeAndLocation>();
					for(TypeAndLocation tal : fromSuper) {
						Location loc = tal.getLocation();
						
						if(loc instanceof ClassFunctionLocation) {
							Type tt = tal.getType();
							
							if(tt instanceof FuncType) {
								FuncType superdef = (FuncType)tt;
								//non public superclass
								if(superdef.origonatingFuncDef.accessModifier == AccessModifier.PUBLIC) {
									//direct location to calling class not superclas - for call to be directed via BRIDGE method
									NamedType asnt = new NamedType(this);
									loc = (ClassFunctionLocation)loc.copy();
									((ClassFunctionLocation)loc).owner = asnt.getCheckCastType();
									((ClassFunctionLocation)loc).ownerType = asnt; 
									tal = new TypeAndLocation(superdef, loc);	
								}
							}
						}
						newfromSuper.add(tal);
					}
					fromSuper = newfromSuper;
				}
				
				ret.addAll( fromSuper );
			}
			
			HashSet<TypeAndLocation> retx =(HashSet<TypeAndLocation>)CompiledClassUtils.getResourceFromClass(this.cls, ITEM_TYPE.FUNC, name, true, false /* allow static and non static stuff*/, nameToGenericMap, extensionFunctions) ;
			
			if(!ignoreLambdas)
			{
				HashSet<TypeAndLocation> asVar =(HashSet<TypeAndLocation>)CompiledClassUtils.getResourceFromClass(this.cls, ITEM_TYPE.VARIABLE, name, true, false /* allow static and non static stuff*/, nameToGenericMap, extensionFunctions) ;
				if(null != asVar) {
					for(TypeAndLocation tt : asVar) {
						tt = super.convertLocalVaraibletoFunction(tt);
						if(null != tt) {
							Location loc = tt.getLocation();
							loc.setLambda(true);
							ret.add(new TypeAndLocation(tt.getType(), loc));
						}
					}
				}
			}
			
			HashMap<Type, TypeAndLocation> asmap = new HashMap<Type, TypeAndLocation>();
			
			for(TypeAndLocation inst : ret) {
				FuncType tt = (FuncType)inst.getType();
				tt = tt.copyIgnoreReturnTypeAndGenerics();
				asmap.put(tt, inst);
			}
			
			if(retx != null) {
				for(TypeAndLocation inst : retx) {
					FuncType tt = (FuncType)inst.getType();
					tt = tt.copyIgnoreReturnTypeAndGenerics();
					
					if(tt.getInputs().stream().noneMatch(a -> (a instanceof NamedType) && ((NamedType)a).getGenericTypeElements().contains(null) )) {
						asmap.put(tt, inst);
					}
				}
			}
			ret = new HashSet<TypeAndLocation>(asmap.values());
			getFuncDefCache.put(key, ret);
		}
		return ret;
	}
	@Override
	public boolean hasFuncDef(String name, boolean ignoreLambdas, boolean extensionFuncions)
	{
		HashSet<TypeAndLocation> ret = getFuncDef(name, ignoreLambdas, extensionFuncions);
		return null != ret && !ret.isEmpty();
	}
	@Override
	public TypeAndLocation getVariable(TheScopeFrame notused,String name)
	{
		return getVariable(null, name, true, false);
	}
	@Override
	public TypeAndLocation getVariable(TheScopeFrame notused, String name, boolean searchParent, boolean ignoreAutoGennerated)
	{
		if(this.isTrait && classHasTraitAnnotation(this)) {
			List<Fiveple<String, ClassDef, Type, Boolean, AccessModifier>> traitFields = this.getTraitFields(new HashMap<Type,Type>());
			for(Fiveple<String, ClassDef, Type, Boolean, AccessModifier> field : traitFields) {
				if(field.getA().equals(name)) {
					Type got = field.getC();
					ClassDef relevantClass = field.getB();
					
					String ownerOfField = relevantClass.bcFullName().replaceAll("\\.", "/");
					Location loc = new LocationClassField(ownerOfField, new NamedType(relevantClass));
					loc.setAccessModifier(field.getE());
						
					return new TypeAndLocation(got, loc);
				}
					
			}
			
			return null;
		}else {
			HashSet<TypeAndLocation> got = (HashSet<TypeAndLocation>)CompiledClassUtils.getResourceFromClass(this.cls, ITEM_TYPE.VARIABLE, name, true, false /* allow static and non static stuff*/, this.nameToGenericMap, false);
			return got==null?null:got.iterator().next();//TODO: dunno if it's right...
		}
	}
	
	public boolean hasVariable(String name, boolean searchParent)
	{
		return null != getVariable(null, name, searchParent, false);
	}
	@Override	
	public boolean hasVariable(TheScopeFrame notused,String name)
	{
		return null != getVariable(notused, name);
	}
	@Override
	public ClassDef getClassDef(String name)
	{
		HashSet<TypeAndLocation> got =(HashSet<TypeAndLocation>)CompiledClassUtils.getResourceFromClass(this.cls, ITEM_TYPE.NESTED_CLASS, name, true, false /* allow static and non static stuff*/);
		TypeAndLocation ret =got==null?null: got.iterator().next();
		
		if(ret!=null && null != ret.getType())
		{
			return ((NamedType)ret.getType()).getSetClassDef();
		}
		
		return null;
	}
	@Override
	public boolean hasClassDef(String name)
	{
		return null != getClassDef(name);
	}

	
	
	protected Map<String, HashSet<TypeAndLocation>> getAllMethodsPlusNestorParent(){
		Map<String, HashSet<TypeAndLocation>> ret = new HashMap<String, HashSet<TypeAndLocation>>();
		ClassDef par = this.getParentNestor();
		if(par != null){
			ret.putAll(par.getAllMethodsPlusNestorParent());
		}
		
		ret.putAll(CompiledClassUtils.getAllMethods(this.cls, nameToGenericMap, true));
		//child is more specific, even though its not possible to do this in real code, probably makes sense to use most specific for error handle etc (will have already thrown an error)
		return ret;
	}
	

	public List<Pair<String, TypeAndLocation>> getAllLocallyDefinedMethods(){
		List<Pair<String, TypeAndLocation>> ret = new ArrayList<Pair<String, TypeAndLocation>>();
		
		Map<String, HashSet<TypeAndLocation>>  items = CompiledClassUtils.getAllMethods(this.cls, nameToGenericMap, false);
		for(String key : items.keySet()) {
			HashSet<TypeAndLocation> hs = items.get(key);
			for(TypeAndLocation tal : hs) {
				ret.add(new Pair<String, TypeAndLocation>(key, tal));
			}
		}
		
		return ret;
	}
	
	
	
	
	public final List<Thruple<String, FuncType, Boolean>> getAllAnnotationMethods(){
		//name, type, hasdefault
		return CompiledClassUtils.getAllAnnotationMethods(this.cls);
	}
	
	
	
	//in java classes can be nested static or not
	@Override
	public void setParentNestor(ClassDef parentNestor)
	{
		//cannot get here
	}
	
	@Override
	public ClassDef getParentNestor()
	{
		
		return encloser;
	}
	
	@Override
	public boolean isNestedAndNonStatic()
	{
		return !Modifier.isStatic(this.cls.getModifiers()) && cls.isMemberClass() && null != encloser;
	}
	
	public boolean isStatic() {
		return this.isStatic;
	}
	
	
	public boolean isAnnotation() {
		return this.cls.isAnnotation();
	}
	
	private Annotations anotscache = null; 
	
	@Override
	public Annotations getAnnotations(){
		if(anotscache == null) {
			ArrayList<Annotation> annotations = new ArrayList<Annotation>();
			
			java.lang.annotation.Annotation[] xxx = this.cls.getAnnotations();
			for(java.lang.annotation.Annotation an : xxx){
				Class<?> cls = an.annotationType();
				ArrayList<Pair<String, Expression>> args = new ArrayList<Pair<String, Expression>>();
				
				for(Method mx : cls.getDeclaredMethods()){
					try {
						Object what = mx.invoke(an, null);
						VarNull dummy = new VarNull();
						dummy.setFoldedConstant(what);
						Pair<String, Expression> item = new Pair<String, Expression>(mx.getName(), dummy);
						args.add(item);
					} catch (Throwable e) {}//meh
				}
				
				Annotation anot = new Annotation(0,0, cls.getCanonicalName(), null, args, new ArrayList<String>());
				anot.setTaggedType(new NamedType(0,0, new ClassDefJava(cls)));
				annotations.add(anot);
			}
			anotscache = new Annotations(0,0, annotations);
		}
		
		return anotscache;
	}

	@Override
	public boolean allTraitClassesResolved() {
		return true;
	}

	
	@Override
	public List<Pair<String, TypeAndLocation>> getAllStaticAssets() {
		//get all methods, variables, typdefs, classes, traits, enums
		return CompiledClassUtils.getAllStaticAssets(this.cls);
	}
	
	
}
