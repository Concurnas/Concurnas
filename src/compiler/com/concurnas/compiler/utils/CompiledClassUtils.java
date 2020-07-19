package com.concurnas.compiler.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.concurnas.bootstrap.lang.Lambda;
import com.concurnas.bootstrap.lang.Lambda.ClassRef;
import com.concurnas.bootstrap.runtime.DefaultMethodRequiresImplementation;
import com.concurnas.bootstrap.runtime.cps.CObject;
import com.concurnas.bootstrap.runtime.cps.Fiber;
import com.concurnas.bootstrap.runtime.ref.LocalArray;
import com.concurnas.bootstrap.runtime.ref.Ref;
import com.concurnas.compiler.ast.AccessModifier;
import com.concurnas.compiler.ast.Annotation;
import com.concurnas.compiler.ast.Annotations;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.FuncParam;
import com.concurnas.compiler.ast.FuncParams;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.GPUFuncVariant;
import com.concurnas.compiler.ast.GPUInOutFuncParamModifier;
import com.concurnas.compiler.ast.GPUVarQualifier;
import com.concurnas.compiler.ast.GenericType;
import com.concurnas.compiler.ast.InoutGenericModifier;
import com.concurnas.compiler.ast.ModuleType;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.NullStatus;
import com.concurnas.compiler.ast.PrimativeType;
import com.concurnas.compiler.ast.PrimativeTypeEnum;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.VarNull;
import com.concurnas.compiler.bytecode.FuncLocation.ClassFunctionLocation;
import com.concurnas.compiler.bytecode.FuncLocation.StaticFuncLocation;
import com.concurnas.compiler.constants.UncallableMethods;
import com.concurnas.compiler.typeAndLocation.Location;
import com.concurnas.compiler.typeAndLocation.LocationClassField;
import com.concurnas.compiler.typeAndLocation.LocationStaticField;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.TypeDefTypeProvider.TypeDef;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.util.ErrorRaiseableSupressErrors;
import com.concurnas.lang.ExtensionFunction;
import com.concurnas.lang.Inject;
import com.concurnas.lang.Named;
import com.concurnas.lang.NoNull.When;
import com.concurnas.lang.ParamName;
import com.concurnas.lang.util.LRUCache;
import com.concurnas.runtime.Pair;

public class CompiledClassUtils {

	public static Type ConvertCompiledClassToType(Class<?> cls)
	{
		return ConvertCompiledClassToType(cls, false);
	}
	
	public static Type ConvertCompiledClassToType(Class<?> cls, boolean dontSetupInterfaceGenerics)
	{
		int arryLevels = 0;
		while(cls.isArray())
		{
			arryLevels++;
			cls = cls.getComponentType();
		}
		
		Type ret;
		
		if(cls.isPrimitive())
		{/*
			java.lang.Boolean.TYPE			java.lang.Character.TYPE			java.lang.Byte.TYPE			java.lang.Short.TYPE
			java.lang.Integer.TYPE			java.lang.Long.TYPE			java.lang.Float.TYPE			java.lang.Double.TYPE
			java.lang.Void.TYPE
			*/
			String name = cls.getName();
			if(name.equals("boolean") ) { ret = new PrimativeType(0,0,PrimativeTypeEnum.BOOLEAN); }
			else if(name.equals("char") ) { ret = new PrimativeType(0,0,PrimativeTypeEnum.CHAR); }
			else if(name.equals("byte") ) { ret = new PrimativeType(0,0,PrimativeTypeEnum.BYTE); }
			else if(name.equals("short") ) { ret = new PrimativeType(0,0,PrimativeTypeEnum.SHORT); }
			else if(name.equals("int") ) { ret = new PrimativeType(0,0,PrimativeTypeEnum.INT); }
			else if(name.equals("long") ) { ret = new PrimativeType(0,0,PrimativeTypeEnum.LONG); }
			else if(name.equals("float") ) { ret = new PrimativeType(0,0,PrimativeTypeEnum.FLOAT); }
			else if(name.equals("double") ) { ret = new PrimativeType(0,0,PrimativeTypeEnum.DOUBLE); }
			else /*vois*/ {  ret = new PrimativeType(0,0,PrimativeTypeEnum.VOID); }
		}
		else //named
		{
			ClassDefJava comped = new ClassDefJava(dontSetupInterfaceGenerics, cls);
			TypeVariable<?>[] typeParams = cls.getTypeParameters();
			
			ArrayList<GenericType> genTypes = new ArrayList<GenericType>(typeParams.length);
			if(typeParams.length > 0)
			{
				int cnt=0;
				for(TypeVariable<?> param : typeParams)
				{
					genTypes.add( new GenericType(param.getName(), cnt++) );
				}
				comped.classGenricList = genTypes;
			}
			
			ret = new NamedType(0,0,comped, (ArrayList<Type>)(Object)genTypes);
		}
		ret.setArrayLevels(arryLevels);
		return ret;
	}
	
	private static boolean isValidMethod(java.lang.reflect.Parameter[] params){//not ending with a com.concurnas.bootstrap.runtime.cps.Fiber or containing a com.concurnas.runtime.InitUncreatable
		if(params.length > 0){
			Class<?> c = params[params.length - 1].getType();
			if(com.concurnas.bootstrap.runtime.InitUncreatable.class.isAssignableFrom(c) || com.concurnas.bootstrap.runtime.cps.Fiber.class.isAssignableFrom(c)){
				return false;
			}
		}
		
		return true;
		
	}
	
	private static Map<Class<?>, Constructor<?>[]> classtoConstructors = Collections.synchronizedMap(new LRUCache<Class<?>, Constructor<?>[]>(300));
	private static Constructor<?>[] getConstructorsForClass(Class<?> cls){
		Constructor<?>[] ret;
		if(classtoConstructors.containsKey(cls)) {
			ret = classtoConstructors.get(cls);
		}else {
			ret = cls.getDeclaredConstructors();
			classtoConstructors.put(cls, ret);
		}
		return ret;
	}
	
	private static Map<Class<?>,ArrayList<ClassDef>> classToNestedClasses = Collections.synchronizedMap(new LRUCache<Class<?>, ArrayList<ClassDef>>(300));
	public static ArrayList<ClassDef> getAllDeclaredNestedClasses(Class<?> cls) {
		ArrayList<ClassDef> ret;
		if(classToNestedClasses.containsKey(cls)) {
			ret = classToNestedClasses.get(cls);
		}else {
			
			Class<?>[] nested = cls.getClasses();
			ret = new ArrayList<ClassDef>(nested.length);
			for(int n = 0; n < nested.length; n++) {
				ret.add(new ClassDefJava(nested[n]));
			}
			
			classToNestedClasses.put(cls, ret);
		}
		return ret;
	}
	
	
	public static HashSet<FuncType> getAllConstructors(Class<?> cls, Map<String, GenericType> nameToGenericMap){
		HashSet<FuncType> ret = new HashSet<FuncType>();
		for(Constructor<?> con : getConstructorsForClass(cls)) {
			if((Modifier.isPublic(con.getModifiers()) || Modifier.isProtected(con.getModifiers()))  && isValidMethod(con.getParameters()) ){
				ret.add(convertConstructorToFuncType(con, nameToGenericMap));
			}
		}
		return ret;
	}
	
	public static HashSet<FuncType> getConstructor(Class<?> cls, int argscnt, ErrorRaiseableSupressErrors invoker, Map<String, GenericType> nameToGenericMap){
		HashSet<FuncType> ret = new HashSet<FuncType>();
		for(Constructor<?> con : getConstructorsForClass(cls)) {
			if(con.getParameterTypes().length == argscnt && isValidMethod(con.getParameters()) ){
				FuncType ft =  convertConstructorToFuncType(con, nameToGenericMap);
				ret.add(ft);
			}
		}
		return ret;
	}
	
	private static FuncType convertConstructorToFuncType(Constructor<?> m, Map<String, GenericType> nameToGenericMap){
		//cut paste magic
		ArrayList<com.concurnas.compiler.ast.Type> inputs = new ArrayList<com.concurnas.compiler.ast.Type>();
		for(java.lang.reflect.Type type : m.getGenericParameterTypes() )
		{//TODO: check this maps stuff correctly on genericalsas
			
			inputs.add(convertGenType(type, nameToGenericMap, false));
		}
		
		FuncType sig = new FuncType(0,0,inputs, null );
		int modi = m.getModifiers();
		boolean isAbstract = Modifier.isAbstract(modi);
		sig.setFinal(Modifier.isFinal(modi));
		
		
		boolean isFinal = Modifier.isFinal(modi);
		AccessModifier accessModifier = AccessModifier.PRIVATE; 
		if(Modifier.isPublic(modi)){
			accessModifier = AccessModifier.PUBLIC;
		}
		else if(Modifier.isProtected(modi)){
			accessModifier = AccessModifier.PROTECTED;
		}

		FuncParams fp = extractFuncParams(m.getParameterAnnotations(), inputs);
		
		sig.origonatingFuncDef = new FuncDef(0, 0, null, accessModifier, m.getName(), fp,null, false, isAbstract, isFinal);//TODO: add annotations
		
		for (java.lang.annotation.Annotation annot : m.getAnnotations()) {
			
			Class<?> classdef = annot.annotationType();
			
			if(classdef.equals(nullStatusCls)) {
				try {
					boolean[] nullables = (boolean[])classdef.getMethod("nullable").invoke(annot);
					tagTypesWithNullable(inputs, sig, nullables);
					
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
					e.printStackTrace();
				}
				
			}else if(classdef.equals(defMethodReqImplAnnotCls)) {
				isAbstract = true;
			}
			
			if(classdef.equals(Inject.class)) {
				sig.origonatingFuncDef.isInjected = true;
			}
		}

		sig.setAbstarct(isAbstract);
		
		return sig;
	}
	
	private static FuncParams extractFuncParams(java.lang.annotation.Annotation[][] annots, ArrayList<com.concurnas.compiler.ast.Type> inputs){
		FuncParams fp = new FuncParams(0,0);
		//boolean canExtractArgNames = true;
		for(int n=0; n < annots.length; n++){
			com.concurnas.compiler.ast.Type tt = inputs.get(n);
			
			String argName = "arg$"+n;
			boolean argNameKnown = false;
			boolean hasDefaultValue = false;
			boolean isVararg = false;
			String namedAnnotationName = null;
			for(java.lang.annotation.Annotation anhaz : annots[n]){
				Class clz = anhaz.annotationType();
				if(clz.equals(ParamName.class)){//REMOVE REFLECTION
					ParamName asParamNAme = (ParamName)anhaz;
					argNameKnown=true;
					argName = asParamNAme.name();
					hasDefaultValue = asParamNAme.hasDefaultValue();
					isVararg = asParamNAme.isVararg();
					
					/*try {
						Method mx = clz.getMethod("hasDefaultValue", null);//new Class[]{null}
						hasDefaultValue = (Boolean)mx.invoke(anhaz, null);
					} catch (Throwable e) {
						//canExtractArgNames=false;
					}
					
					try {
						Method mx = clz.getMethod("isVararg", null);//new Class[]{null}
						isVararg = (Boolean)mx.invoke(anhaz, null);
					} catch (Throwable e) {
						//canExtractArgNames=false;
					}
					
					try {
						Method mx = clz.getMethod("name", null);//new Class[]{null}
						argName = (String)mx.invoke(anhaz, null);
						argNameKnown=true;
						break;
					} catch (Throwable e) {
						//canExtractArgNames=false;
					}*/
				}
				if(clz.equals(Named.class)) {
					Named asNamed = (Named)anhaz;
					namedAnnotationName = asNamed.value();
				}
			}
			FuncParam fpu = new FuncParam(0,0, argName, tt, true);
			fpu.argNameKnown = argNameKnown;
			fpu.namedAnnotationName = namedAnnotationName;
			if(hasDefaultValue) {
				fpu.defaultValue = new VarNull(0,0);//messy
				fpu.defaultValue.setTaggedType(tt);
				if(tt instanceof PrimativeType && !tt.hasArrayLevels()) {
					fpu.defaultOk = false;
				}else {
					fpu.defaultOk = true;
				}
				/*if(tt instanceof PrimativeType && !tt.hasArrayLevels()) {
					fpu.defaultValue.setTaggedType(tt);
				}else {
					fpu.defaultValue.setTaggedType((Type)fpu.defaultValue);
				}
				
				fpu.defaultOk = true;*/
			}
			if(isVararg) {
				fpu.isVararg = true;
			}
			
			fp.add(fpu);
		}
		
		
		return fp;
	}
	
	private static final Class<?> noNullAnotCls = com.concurnas.lang.NoNull.class; 
	private static final Class<?> nullStatusCls = com.concurnas.lang.internal.NullStatus.class;
	private static final Class<?> defMethodReqImplAnnotCls = DefaultMethodRequiresImplementation.class;
	
	private static FuncType convertMethodToFuncType(Method m, Map<String, GenericType> nameToGenericMap)
	{
		TypeVariable<Method>[] tps = m.getTypeParameters();
		ArrayList<GenericType> localGenerics = null;
		if(tps!=null && tps.length > 0){
			localGenerics = new ArrayList<GenericType>();
			for(TypeVariable<Method> t : tps){
				java.lang.reflect.Type upperBound = t.getBounds()[0];
				String name = t.getName();
				GenericType gt = new GenericType(name, 0);
				localGenerics.add(gt);
				//add here as well
				nameToGenericMap = nameToGenericMap==null?new HashMap<String, GenericType>():new HashMap<String, GenericType>(nameToGenericMap);//clone existing 
				nameToGenericMap.put(name, gt);
				gt.setNullStatus(NullStatus.UNKNOWN);

				gt.upperBound = (NamedType)convertGenType(upperBound, nameToGenericMap, false);
				
				processAnnotsAndDecorate(t.getAnnotations(), gt);
			}
		}
		
		ArrayList<com.concurnas.compiler.ast.Type> inputs = new ArrayList<com.concurnas.compiler.ast.Type>();
		//Class<?>[] ptypes = m.getParameterTypes();
		int n=0;
		java.lang.annotation.Annotation[][]  paramannots = m.getParameterAnnotations();
		for(java.lang.reflect.Type type : m.getGenericParameterTypes() )
		{//TODO: check this maps stuff correctly on genericalsas
			Type tt = convertGenType(type, nameToGenericMap, false);
			processAnnotsAndDecorate(paramannots[n], tt);
			inputs.add(tt);
			n++;
		}
		
		FuncParams fp = extractFuncParams(m.getParameterAnnotations(), inputs);
		
		Type retType = convertGenType(m.getGenericReturnType(), nameToGenericMap, false);
		processAnnotsAndDecorate(m.getAnnotatedReturnType().getAnnotations(), retType);
		
		FuncType sig = new FuncType(0,0,inputs, retType );
		int modi = m.getModifiers();
		boolean isAbstract = Modifier.isAbstract(modi);
		sig.setFinal(Modifier.isFinal(modi));
		
		
		int modies = m.getModifiers();
		boolean isFinal = Modifier.isFinal(modies);
		AccessModifier accessModifier = AccessModifier.PRIVATE; 
		if(Modifier.isPublic(modies)){
			accessModifier = AccessModifier.PUBLIC;
		}
		else if(Modifier.isProtected(modies)){
			accessModifier = AccessModifier.PROTECTED;
		}
		
		if(null != localGenerics){
			sig.setLocalGenerics(localGenerics);
		}
		
		Annotations annots = new Annotations();
		
		for (java.lang.annotation.Annotation annot : m.getAnnotations()) {//capture annotation on mehod
			Class<?> classdef = annot.annotationType();
			
			if(classdef.equals(nullStatusCls)) {
				try {
					boolean[] nullables = (boolean[])classdef.getMethod("nullable").invoke(annot);
					tagTypesWithNullable(inputs, retType, nullables);
					
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				}
			}else if(classdef.equals(defMethodReqImplAnnotCls)) {
				isAbstract = true;
			}else {
				Annotation annotz = new Annotation(0, 0, "", null, null, new ArrayList<String>());//JPT: ignore arguments to annotation
				annotz.setTaggedType(new NamedType(new ClassDefJava(classdef, true)));
				annots.annotations.add(annotz);
			}
		}
		
		sig.setAbstarct(isAbstract);
		
		sig.origonatingFuncDef = new FuncDef(0, 0, annots, accessModifier, m.getName(), fp, null, false, isAbstract, isFinal);//TODO: add annotations
		
		if(null != sig.getLocalGenerics()) {
			ArrayList<Pair<String, NamedType>> methodGenricList = new ArrayList<Pair<String, NamedType>>();
			for(GenericType gt : sig.getLocalGenerics()) {
				methodGenricList.add(new Pair<String, NamedType>(gt.name, gt.getOrigonalGenericTypeUpperBoundRaw()));
			}
			sig.origonatingFuncDef.methodGenricList = methodGenricList;
		}
		
		sig.origonatingFuncDef.retType = convertGenType(m.getGenericReturnType(), nameToGenericMap, false);
		processAnnotsAndDecorate(m.getAnnotatedReturnType().getAnnotations(), sig.origonatingFuncDef.retType);
		
		Type first = inputs.size() > 0 ? inputs.get(0) : null;
		for (java.lang.annotation.Annotation annot : m.getAnnotations()) {
			Class<?> at = annot.annotationType();
			if (first != null && at.equals(ExtensionFunction.class)) {
				sig.origonatingFuncDef.setExtFuncOn(first);//(NamedType)
				sig.extFuncOn = true;
				fp.params.remove(0);
				break;
			}
			else if(at.equals(Inject.class)) {
				sig.origonatingFuncDef.isInjected = true;
			}
		}
		
		
		
		
		return sig;
	}

	
	private static class IntHolder{
		int anInt = 0;
	}
	
	 private static void processType(Type atype, boolean[] nullables, IntHolder pos) {
		if(null == atype) {
			return;//why null?
		}
		
		if( pos.anInt >= nullables.length) {
			atype.setNullStatus(NullStatus.NOTNULL);
		}
		else {
			if(atype.hasArrayLevels()) {
				//HAS ARRAY LEVELS
				int sz = atype.getArrayLevels();
				List<NullStatus> nas = new ArrayList<NullStatus>(sz);
				for(int n = 0; n < sz; n++) {
					nas.add(nullables[pos.anInt]?NullStatus.NULLABLE:NullStatus.NOTNULL);
					pos.anInt++;
				}
				atype.setNullStatusAtArrayLevel(nas);
			}
			
			if(atype instanceof NamedType) {
				NamedType asNamed = (NamedType)atype;
				asNamed.getGenericTypeElements().forEach(a -> processType(a, nullables, pos) );
			}else if(atype instanceof FuncType) {
				FuncType asft = (FuncType)atype;
				asft.inputs.forEach(a -> processType(a, nullables, pos));
				processType(asft.retType, nullables, pos);
			}
			
			if( pos.anInt >= nullables.length) {
				atype.setNullStatus(NullStatus.NOTNULL);
			}else {
				atype.setNullStatus(nullables[pos.anInt]?NullStatus.NULLABLE:NullStatus.NOTNULL);
			}
		}
		
		pos.anInt++;
	}
	
	public static void tagTypesWithNullable(List<Type> inputs, Type retType, boolean[] nullables) {
		IntHolder pos = new IntHolder();
		if(null != inputs) {
			inputs.forEach(a-> processType(a, nullables, pos));
		}
		
		processType(retType, nullables, pos);
	}
	
	
	private static void processAnnotsAndDecorate(java.lang.annotation.Annotation[] annots, Type tt) {
		if(null != annots) {
			for(java.lang.annotation.Annotation anot : annots) {
				if(noNullAnotCls.equals(anot.annotationType())) {
					try {
						When when = (When)noNullAnotCls.getMethod("when").invoke(anot);
						if( when == When.ALWAYS) {
							tt.setNullStatus(NullStatus.NOTNULL);
						}else if(when == When.NEVER) {
							tt.setNullStatus(NullStatus.NULLABLE);
						}/*else if(when == When.MAYBE) {
							tt.setNullStatus(NullStatus.UNKNOWN);
						}*/
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
					}
				}
			}
		}
	}
	
	public static Object getResourceFromClass(Class<?> relevantClass, ITEM_TYPE type, String remainingDottedName, boolean isChildClassAsking, boolean onlyStaticStuffAvailable)
	{
		return getResourceFromClass(relevantClass,  type,  remainingDottedName,  isChildClassAsking,  onlyStaticStuffAvailable, null, null);
	}
	
	
	public static List<Pair<String, FuncType>> getAllAbstractFunctionsFromCls(Class<?> cls, Map<String, GenericType> nameToGenericMap)
	{
		ArrayList<Pair<String, FuncType>> ret = new ArrayList<Pair<String, FuncType>>();
		Method[] meths = cls.getMethods();
		for(Method m : meths)
		{
			int modifiders = m.getModifiers();
			
			if(Modifier.isAbstract(modifiders) && !Modifier.isStatic(modifiders) && ( Modifier.isPublic(modifiders) || Modifier.isProtected(modifiders) ) )
			{
				FuncType sig = convertMethodToFuncType(m, nameToGenericMap);
				ret.add(new Pair<String, FuncType>(m.getName(), sig));
			}
		}
		return ret;
	}
	
	public static ArrayList<Type> extracQualifiedGenTypes(ParameterizedType asParType,  Map<String, GenericType> nameToGenericMap, boolean dontSetupInterfaceGenerics){
		java.lang.reflect.Type[] parTypes =  asParType.getActualTypeArguments();
		ArrayList<Type> QualifiedgenTypes = new ArrayList<Type>(parTypes.length);
		
		for( java.lang.reflect.Type partype : parTypes ){
			//TODO: cater for all of these: GenericArrayType
			//so these todo: GenericArrayType, ParameterizedType
			//System.err.println("gens: " + partype);
			if(partype instanceof TypeVariable){//generic
				//String genParName = (((TypeVariable<?>)partype).getName());
				//QualifiedgenTypes.add(nameToGenericMap.get(genParName));
				
				QualifiedgenTypes.add(convertGenType(partype, nameToGenericMap, dontSetupInterfaceGenerics));
				
			}
			else if (partype instanceof WildcardType){
				WildcardType wc = (WildcardType)partype;
				java.lang.reflect.Type[] bound = wc.getLowerBounds();
				boolean hasLower = bound != null && bound.length!=0;
				if(!hasLower){
					bound = wc.getUpperBounds(); 
				}
				
				Type tt = convertGenType(bound[0], nameToGenericMap, dontSetupInterfaceGenerics);
				tt = tt==null?null:(Type)tt.copy();//why would tt be null?
				
				if(!hasLower && tt.equals(ScopeAndTypeChecker.const_object)){
					if(tt instanceof NamedType){
						((NamedType)tt).isWildCardAny = true;
					}
					if(tt instanceof GenericType){
						((GenericType)tt).isWildcard=true;
					}
				}else{
					if(null == wc.getUpperBounds() && null == wc.getLowerBounds()){//ArrayList<?>
						if(tt instanceof NamedType){
							((NamedType)tt).isWildCardAny = true;
						}
						if(tt instanceof GenericType){
							((GenericType)tt).isWildcard=true;
						}
					}
					else if(tt != null){//ArrayList<? extends Number>
						tt.setInOutGenModifier(hasLower?InoutGenericModifier.IN:InoutGenericModifier.OUT);
						
						/*if(tt instanceof NamedType){
							((NamedType)tt).isWildCardAny = true;
						}
						if(tt instanceof GenericType){
							((GenericType)tt).isWildcard=true;
						}*/
						
					}
				}
				
				
				
				QualifiedgenTypes.add(tt);
			}
			else if(partype instanceof Class<?>){
				QualifiedgenTypes.add(convertGenType(partype, nameToGenericMap, dontSetupInterfaceGenerics));
				//dontSetupInterfaceGenerics - prevents infinite loop with Int -> Comparable<Int> which gets expanded forever
			}
			else if(partype instanceof ParameterizedType){
				QualifiedgenTypes.add(convertGenType(partype, nameToGenericMap, dontSetupInterfaceGenerics));
			}
			else if(partype instanceof GenericArrayType) {
				GenericArrayType gat = (GenericArrayType)partype;
				
				QualifiedgenTypes.add(convertGenType(gat.getGenericComponentType(), nameToGenericMap, dontSetupInterfaceGenerics));
			}else{
				throw new RuntimeException("Unexpected Parameterized type in convertGenType: " + partype);
			}
		}
		return QualifiedgenTypes;
	}
	
	/*private final static HashSet<ClassDef> lambdaTypes = new HashSet<ClassDef>();
	static{//ugly
		lambdaTypes.add(new ClassDefJava(Function0.class));
		lambdaTypes.add(new ClassDefJava(Function1.class));
		lambdaTypes.add(new ClassDefJava(Function2.class));
		lambdaTypes.add(new ClassDefJava(Function3.class));
		lambdaTypes.add(new ClassDefJava(Function4.class));
		lambdaTypes.add(new ClassDefJava(Function5.class));
		lambdaTypes.add(new ClassDefJava(Function6.class));
		lambdaTypes.add(new ClassDefJava(Function7.class));
		lambdaTypes.add(new ClassDefJava(Function8.class));
		lambdaTypes.add(new ClassDefJava(Function9.class));
		lambdaTypes.add(new ClassDefJava(Function10.class));
		lambdaTypes.add(new ClassDefJava(Function11.class));
		lambdaTypes.add(new ClassDefJava(Function12.class));
		lambdaTypes.add(new ClassDefJava(Function13.class));
		lambdaTypes.add(new ClassDefJava(Function14.class));
		lambdaTypes.add(new ClassDefJava(Function15.class));
		lambdaTypes.add(new ClassDefJava(Function16.class));
		lambdaTypes.add(new ClassDefJava(Function17.class));
		lambdaTypes.add(new ClassDefJava(Function18.class));
		lambdaTypes.add(new ClassDefJava(Function19.class));
		lambdaTypes.add(new ClassDefJava(Function20.class));
		lambdaTypes.add(new ClassDefJava(Function21.class));
		lambdaTypes.add(new ClassDefJava(Function22.class));
		lambdaTypes.add(new ClassDefJava(Function23.class));
		lambdaTypes.add(new ClassDefJava(Function23.class));
	}*/
	
	private static final ClassDef lambdaClass = new ClassDefJava(Lambda.class);
	private static final ClassDef classRefClass = new ClassDefJava(ClassRef.class);
	
	public static com.concurnas.compiler.ast.Type convertGenType(java.lang.reflect.Type type, Map<String, GenericType> nameToGenericMap, boolean dontSetupInterfaceGenerics){
		if(type instanceof TypeVariable){//generic
			return nameToGenericMap.get(((TypeVariable<?>)type).getName());
		}
		com.concurnas.compiler.ast.Type ret;
		
		if(type instanceof ParameterizedType)
		{
			ParameterizedType asParType = (ParameterizedType)type;
			Class<?> classDef = (java.lang.Class<?>)asParType.getRawType();
			ArrayList<Type> QualifiedgenTypes = extracQualifiedGenTypes(asParType, nameToGenericMap, false);
			
			ret = new NamedType(0,0, new ClassDefJava(dontSetupInterfaceGenerics, classDef), (ArrayList<Type>)QualifiedgenTypes);
		}
		/*else if(type instanceof Class<?> && !((Class<?>) type).isPrimitive()){
			ret = new NamedType(new ClassDefJava((Class<?>)type));
		}*/
		//TODO: wildcard type should really be covered here
		else if(type instanceof GenericArrayType){
			GenericArrayType asGenAt = (GenericArrayType)type;
			
			ret = convertGenType(asGenAt.getGenericComponentType(), nameToGenericMap, dontSetupInterfaceGenerics);
			ret = (Type) ret.copy();
			ret.setArrayLevels(ret.getArrayLevels() + 1);
			
			//HERE IS THE PROBLEOMO
		}
		else{//not generic
			ret= CompiledClassUtils.ConvertCompiledClassToType((java.lang.Class<?>)type, dontSetupInterfaceGenerics);
		}
		
		if(ret instanceof NamedType){
			NamedType asNamed=  ((NamedType)ret);
			ClassDef set = asNamed.getSetClassDef();
			
			if(set instanceof ClassDefJava){
				Class<?> clsHeld =  ((ClassDefJava)set).getClassHeld() ;
				if(refCls.isAssignableFrom(clsHeld)){
					asNamed.setIsRef(true);
				}
				else if(localArrCls.isAssignableFrom(clsHeld) ){
					ret = (Type)asNamed.getGenTypes().get(0).copy();
					ret.setArrayLevels(ret.getArrayLevels()+1);
				}
				else if(lambdaClass.equals(set.getSuperclass())){//convert to FuncType if possible
					ArrayList<Type> gens = asNamed.getGenericTypeElements();
					if(gens != null && !gens.isEmpty()){
						
						for(Type gt : gens){
							if(gt instanceof NamedType){
								NamedType asNamedg = (NamedType)gt;
								if(asNamedg.isWildCardAny){
									return new ModuleType(asNamedg.getLine(), asNamedg.getColumn(), "Constructor, Method or Class references cannot return or take as an input wildcards parameters");
								}
							}
						}
					}
					
					boolean retVoid = set.toString().endsWith("v");
					
					FuncType ft = retVoid?new FuncType(gens, ScopeAndTypeChecker.const_void):new FuncType(new ArrayList<Type>(gens.subList(0, gens.size()-1)), gens.get(gens.size()-1));
					//no ? allowed
					
					String owwnerelambda;
					if(set.equals(classRefClass)){
						ft.isClassRefType=true;
						ft.inputs = null;
						
						owwnerelambda = ft.retType.getBytecodeType();
						owwnerelambda = FuncType.classRefIfacePrefix + owwnerelambda.substring(1, owwnerelambda.length()-1) + FuncType.classRefIfacePostfix;
						
					}
					else{
						owwnerelambda = asNamed.getBytecodeType();//lambdaDetails.getA();//"com/concurnas/bootstrap/lang/Lambda$ClassRef";
						owwnerelambda = owwnerelambda.substring(1, owwnerelambda.length()-1);
					}
					
					ClassFunctionLocation cflloc = new ClassFunctionLocation(owwnerelambda, asNamed, false);
					cflloc.setLambdaOwner(owwnerelambda);
					TypeAndLocation lambdaTal = new TypeAndLocation(asNamed, cflloc);//doesnt resolve to anything, dummy value
					
					ft.setLambdaDetails(lambdaTal);
					ret = ft;
				}
			}
		}
		
		ret.setNullStatus(NullStatus.UNKNOWN);
		
		return ret;
		//TODO: need to deal with wildcard case and generic array case
	}
	
 	//private static final Class<?> refCls = Local.class;  //TODO: or remoteRef
 	public static final Class<?> refCls = Ref.class;  //TODO: or remoteRef
 	public static final Class<?> localArrCls = LocalArray.class;  //TODO: or remoteRef
	
	private static HashSet<MethodHolder> basicObjectMethods = new HashSet<MethodHolder>();// = Object.class.getDeclaredMethods();
	static{
		for(Method m : Object.class.getDeclaredMethods()){
			basicObjectMethods.add(new MethodHolder(m));
		}
	}
	
	private static class MethodHolder{
		//compare method name and arguments, ignore return type
		public Method held;
		
		public MethodHolder(Method held){
			this.held = held;
		}
		@Override
		public int hashCode(){
			int ret = held.getName().hashCode();
			java.lang.reflect.Type[] types = held.getGenericParameterTypes();
			for(java.lang.reflect.Type t : types){
				ret += t.hashCode();
			}
			
			return ret;
		}
		
		@Override 
		public boolean equals(Object obj){
			MethodHolder to = (MethodHolder)obj;
			if(!to.held.getName().equals(this.held.getName())){
				return false;
			}
			
			java.lang.reflect.Type[] theirTypes = to.held.getGenericParameterTypes();
			java.lang.reflect.Type[] myTypes = held.getGenericParameterTypes();
			
			if(theirTypes.length != myTypes.length){
				return false;
			}
			
			for(int n = 0; n  < theirTypes.length; n++){
				if(!theirTypes[n].equals(myTypes[n])){
					return false;
				}
			}
			
			return true;
		}
		
	}
	
	private static Map<Class<?>, HashSet<MethodHolder>> classToMethodCache = Collections.synchronizedMap(new LRUCache<Class<?>, HashSet<MethodHolder>>(200));
	//TODO: cache results
	private static HashSet<MethodHolder> getAllMethods(Class<?> cls) {
		HashSet<MethodHolder> ret;
		if(classToMethodCache.containsKey(cls)) {
			return classToMethodCache.get(cls);
		}else {
			ret = new HashSet<MethodHolder>();
			//System.err.println("get all methods for: " + cls);
			for(Method m : cls.getDeclaredMethods()){
				if(!m.isBridge()){
					Parameter[] params = m.getParameters();
					if(params.length > 0) {
						Parameter param = params[params.length-1];
						if(param.getParameterizedType().equals(Fiber.class)) {
							continue;//skip all instances which have bee fiberized
						}
					}
					
					ret.add(new MethodHolder(m));
				}
			}

			if(cls.equals(com.concurnas.lang.Actor.class)) {
				ret.addAll(basicObjectMethods);
			}
			else if(cls.isInterface()){
				//add all the basic object methods as well
				ret.addAll(basicObjectMethods);
			}
			
			classToMethodCache.put(cls, ret);
		}
		

		return ret;
	}
	
	/**
	 * @return name, type, hasdefault
	 */
	public static List<Thruple<String, FuncType, Boolean>> getAllAnnotationMethods(Class<?> relevantClass){
		List<Thruple<String, FuncType, Boolean>> ret = new ArrayList<Thruple<String, FuncType, Boolean>>();
		
		HashMap<String, GenericType> empty = new HashMap<String, GenericType>();
		for(Method m : relevantClass.getDeclaredMethods()){
			FuncType sig = convertMethodToFuncType(m, empty);
			ret.add(new Thruple<>(m.getName(), sig, null !=m.getDefaultValue()));
		}
		
		return ret;
	}
	
	private static Map<Class<?>, Field[]> classToFieldsCache = Collections.synchronizedMap(new LRUCache<Class<?>, Field[]>(300));
	
	public static ArrayList<Fiveple<String, Type, Boolean, String, AccessModifier>> getAllFields(Class<?> relevantClass, Map<String, GenericType> nameToGenericMap){
		ArrayList<Fiveple<String, Type, Boolean, String, AccessModifier>> ret = new ArrayList<Fiveple<String, Type, Boolean, String, AccessModifier>>(0);
		
		Field[] fields;
		if(classToFieldsCache.containsKey(relevantClass)) {
			fields = classToFieldsCache.get(relevantClass);
		}else {
			fields = relevantClass.getDeclaredFields();
			classToFieldsCache.put(relevantClass, fields);
		}
		
		for(Field ff : fields){
			String name = ff.getName();
			boolean iInjected = ff.getAnnotation(Inject.class) != null;
			
			String fname = null;
			Named namedAnnot = ff.getAnnotation(Named.class);
			if(null != namedAnnot) {
				fname = namedAnnot.value();
			}
			
			int modi = ff.getModifiers();
			AccessModifier am = AccessModifier.PACKAGE;
			if(Modifier.isPublic(modi)) {
				am = AccessModifier.PUBLIC;
			}else if(Modifier.isPrivate(modi)) {
				am = AccessModifier.PRIVATE;
			}else if(Modifier.isProtected(modi)) {
				am = AccessModifier.PROTECTED;
			}
			
			
			Type obtainedType = convertGenType(ff.getType(), nameToGenericMap, false);//CompiledClassUtils.ConvertCompiledClassToType(got.getType(), false);
			processAnnotsAndDecorate(ff.getAnnotations(), obtainedType);

			com.concurnas.lang.internal.NullStatus ns = ff.getAnnotation(com.concurnas.lang.internal.NullStatus.class);
			if(null != ns) {
				tagTypesWithNullable(null, obtainedType, ns.nullable());
			}
			
			ret.add(new Fiveple<String, Type, Boolean, String, AccessModifier>(name, obtainedType, iInjected, fname, am));
		}
		
		return ret;
	}
	
	
	public static Map<String, HashSet<TypeAndLocation>> getAllMethods(Class<?> relevantClass, Map<String, GenericType> nameToGenericMap, boolean publicOnly)
	{//non static, public methods
		Map<String, HashSet<TypeAndLocation>> ret = new HashMap<String, HashSet<TypeAndLocation>>();
		
		HashSet<MethodHolder> methods = getAllMethods(relevantClass);
		if(!methods.isEmpty()){

			ClassDefJava clsDef = new ClassDefJava(relevantClass);
			NamedType ownerNt = new NamedType(clsDef);
			
			for(MethodHolder mh : methods){
				Method m = mh.held;
				int modifiders =  m.getModifiers();
				String name = m.getName();
								
				boolean isPublic = !publicOnly || Modifier.isPublic(modifiders);
				
				if(isPublic && !Modifier.isStatic(modifiders) && !name.equals("<init>") && isValidMethod(m.getParameters()) ){
					FuncType sig = convertMethodToFuncType(m, nameToGenericMap);
					boolean callable=true;
					if(UncallableMethods.GLOBAL_UNCALLABLE_METHODS.containsKey(name)  )
					{
						//this method is not callable in concurnas
						for(FuncType can : UncallableMethods.GLOBAL_UNCALLABLE_METHODS.get(name))
						{
							if(can.equals(sig))
							{
								callable = false;
								break;
							}
						}
					}
					if(callable){//remove notify etc
						String bcName = clsDef.bcFullName();
												
						HashSet<TypeAndLocation> got = ret.get(name);
						if(got == null){
							got = new HashSet<TypeAndLocation>();
							ret.put(name, got);
						}
						got.add(new TypeAndLocation(sig, new ClassFunctionLocation(bcName, ownerNt)));
					}
				}
			}
		}
		
		return ret;
	}
	
	private static FuncType parseGPUdefStubfunction(String sig, String fname, String globalLocalConstant, String inout, int dims){
		ArrayList<Type> inputs = new ArrayList<Type>();
		Type retType = null;
		
		int offset=0;
		boolean inputsDone=false;
		boolean isArray=false;
		int len = sig.length();
		while(offset < len) {//only 1d arrays and primatives...
			char thechar = sig.charAt(offset++);
			Type ret = null;
			switch(thechar) {
			    case 'B': ret = new PrimativeType(PrimativeTypeEnum.BYTE); break;
			    case 'C': ret = new PrimativeType(PrimativeTypeEnum.CHAR); break;
			    case 'D': ret = new PrimativeType(PrimativeTypeEnum.DOUBLE); break;
			    case 'F': ret = new PrimativeType(PrimativeTypeEnum.FLOAT); break;
			    case 'I': ret = new PrimativeType(PrimativeTypeEnum.INT); break;
			    case 'J': ret = new PrimativeType(PrimativeTypeEnum.LONG); break;
			    case 'S': ret = new PrimativeType(PrimativeTypeEnum.SHORT); break;
			    case 'Z': ret = new PrimativeType(PrimativeTypeEnum.BOOLEAN); break;
			    case 'V': ret = new PrimativeType(PrimativeTypeEnum.VOID); break;
			    case 'T': ret = new PrimativeType(PrimativeTypeEnum.SIZE_T); break;
			    case '[':
			    	isArray = true;
			        break;
			    case ')':
			    	inputsDone=true;
			    	break;
			    case '(':
			    	break;
			    default:
			    	throw new RuntimeException(String.format("Unexpected character in gpu function/kernel signature: %s at position: %s in: %s", thechar, offset-1, sig));
			}

		    if(ret != null) {
		    	
		    	if(!inputsDone) {
		    		inputs.add(ret);
		    	}else {
		    		retType = ret;
		    	}
		    	
		    	if(isArray) {
		    		ret.setArrayLevels(1);
		    		isArray=false;
		    	}
		    }
		}
		
		FuncType ft = new FuncType(inputs, retType);
		
		FuncParams fps = new FuncParams(0,0);
		for(int n=0; n< inputs.size(); n++){
			FuncParam fp  = new FuncParam(0,0, "x" + n, inputs.get(n), false);
			fp.gpuVarQualifier = GPUVarQualifier.shortnametoModifier.get(""+globalLocalConstant.charAt(n));
			fp.gpuInOutFuncParamModifier = GPUInOutFuncParamModifier.shortnametoModifier.get("" + inout.charAt(n));
			fps.add(fp); 
		}
		
		/*
			public GPUVarQualifier gpuVarQualifier = null;
			public GPUInOutFuncParamModifier gpuInOutFuncParamModifier;
		 */
		FuncDef fd = new FuncDef(0,0, null, AccessModifier.PUBLIC, fname, fps, new Block(0,0), false, false, false);
		fd.isGPUKernalOrFunction = dims > 0?GPUFuncVariant.gpukernel : GPUFuncVariant.gpudef;
		ft.origonatingFuncDef = fd;
		return ft;
	}
	
	
	/*
	 * @GPUStubFunction gpudef get_global_id(dim int) int - ab abstract gpudef which is marked as a stub is collected as an annotation without source
	 * this is to avoid potentialname clashes between say  get_global_id(dim int) int vs get_global_id(global dim int) int which is ok in opencl but not in conc
	 * These are created as a normal GPUKernelFunction annotation but with no source (they are abstract after all)
	 */
	private static HashSet<TypeAndLocation> getGPUdefStubfunctions(Class<?> relevantClass, String namewanted) {
		HashSet<TypeAndLocation> ret = new HashSet<TypeAndLocation>();
		try {
			java.lang.annotation.Annotation[] annots = relevantClass.getAnnotations();
			if (annots.length > 0) {
				for (java.lang.annotation.Annotation anot : annots) {
					Class<?> annotcls = anot.annotationType();
					if (annotcls.equals(com.concurnas.lang.GPUKernelFunctions.class)) {

						Method gettds = annotcls.getMethod("gpuFuncs", null);
						java.lang.annotation.Annotation[] gpuFunctions = (java.lang.annotation.Annotation[]) gettds.invoke(anot, null);

						for (java.lang.annotation.Annotation typedefanot : gpuFunctions) {
							Class<?> cls = typedefanot.annotationType();
							if (cls.equals(com.concurnas.lang.GPUKernelFunction.class)) {
								String ssrc = (String) cls.getMethod("source", null).invoke(typedefanot, null);
								if (ssrc.equals("")) {// translate into a fake method invocation
									String name = (String) cls.getMethod("name", null).invoke(typedefanot, null);
									if (null == namewanted || name.equals(namewanted)) {
										String signature = (String) cls.getMethod("signature", null).invoke(typedefanot, null);
										String globalLocalConstant = (String) cls.getMethod("globalLocalConstant", null).invoke(typedefanot, null);
										String inout = (String) cls.getMethod("inout", null).invoke(typedefanot, null);
										int dims = (Integer) cls.getMethod("dims", null).invoke(typedefanot, null);
										
										FuncType ft = parseGPUdefStubfunction(signature, namewanted==null?name:namewanted, globalLocalConstant, inout, dims);
										
										ClassDefJava clsDef = new ClassDefJava(relevantClass);
										Location loc = new StaticFuncLocation(new NamedType(clsDef));
										
										TypeAndLocation tal = new TypeAndLocation(ft, loc);
										ret.add(tal);
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return ret;
	}
	
	/**
	 * 
	 * 
	 * @param relevantClass
	 * @param type
	 * @param remainingDottedName
	 * @param isChildClassAsking
	 * @param onlyStaticStuffAvailable  - If you are making a call without an instance of the object then u can only get statuc stuff
	 * @param nameToGenericMap - map from generic return type, field, class or function argument to a generic type  
	 * @return
	 */
	public static Object getResourceFromClass(Class<?> relevantClass, ITEM_TYPE type, String remainingDottedName, boolean isChildClassAsking, boolean onlyStaticStuffAvailable, Map<String, GenericType> nameToGenericMap, Boolean mustBeExtensionFunction)
	{
		if(remainingDottedName == null) {
			return null;
		}
		
		try
		{
			String[] moveDown = remainingDottedName.split(".");
			if(moveDown.length == 0)
			{
				moveDown = new String[]{remainingDottedName};
			}
			
			for(int n = 0; n < moveDown.length; n++)
			{
				String item = moveDown[n];
				if(n==(moveDown.length-1) && (type != ITEM_TYPE.NESTED_CLASS && type != ITEM_TYPE.STATIC_CLASS))
				{//last one STATIC
					if(type == ITEM_TYPE.FUNC)           
					{
						HashSet<TypeAndLocation> ret = new HashSet<TypeAndLocation>();
						
						ret.addAll(getGPUdefStubfunctions(relevantClass, item));
						
						//Method[] meths = relevantClass.getDeclaredMethods();
						HashSet<MethodHolder> meths = getAllMethods(relevantClass);
					
						/*if(item.equals("get_global_id")) {
							StringBuilder sb = new StringBuilder();
							for(MethodHolder mh : meths) {
								sb.append(""+mh.held.getName());
								sb.append(",");
							}
							System.err.println(sb);
						}*/
						
						for(MethodHolder mh : meths)
						{
							Method m = mh.held;
							if(m.getName().equals(item))
							{
								int modifiders =  m.getModifiers();
								boolean isSTat = Modifier.isStatic(modifiders);
								
								if(!onlyStaticStuffAvailable ||(onlyStaticStuffAvailable && isSTat))
								{//beacuse it's a static function then we know that there cannot be any generictype taking plce
									FuncType sig = convertMethodToFuncType(m, nameToGenericMap);
									
									if(mustBeExtensionFunction != null && mustBeExtensionFunction != sig.extFuncOn){
										continue;
									}
									
									boolean callable = true;
									if(UncallableMethods.GLOBAL_UNCALLABLE_METHODS.containsKey(item)  )
									{
										//this method is not callable in concurnas
										for(FuncType can : UncallableMethods.GLOBAL_UNCALLABLE_METHODS.get(item))
										{
											if(can.equals(sig))
											{
												callable = false;
												break;
											}
										}
									}
									if(callable)
									{
										Location loc;
										ClassDefJava clsDef = new ClassDefJava(relevantClass);
										NamedType ownerNt = new NamedType(clsDef);
										
										if(isSTat){
											loc = new StaticFuncLocation(ownerNt);
										}
										else{
											String bcName = clsDef.bcFullName();
											loc = new ClassFunctionLocation(bcName, ownerNt);
										}
										
										sig.origin = new ClassDefJava(relevantClass);
										
										ret.add( new TypeAndLocation(sig, loc) );
									}
								}
								
							}
							
						}
						
						/*if(ret.isEmpty() && item.equals("clone"))
						{//TODO: figure out how this interacts with copy...
							FuncType cloneSig = new FuncType(0,0, new ArrayList<com.concurnas.compiler.ast.Type>(), new NamedType(new ClassDefJava(Object.class)) );
							ClassDefJava clsDef = new ClassDefJava(relevantClass);
							NamedType ownerNt = new NamedType(clsDef);
							ret.add( new TypeAndLocation(cloneSig, new ClassFunctionLocation(clsDef.bcFullName(), ownerNt)) );
						}
						*/
						if(ret.isEmpty() && item.equals("toBoolean")){//TODO: ugly way of doing things
							FuncType cloneSig = new FuncType(0,0, new ArrayList<com.concurnas.compiler.ast.Type>(), ScopeAndTypeChecker.const_boolean );
							ClassDefJava clsDef = new ClassDefJava(CObject.class);//this is the true origin of this method...
							
							ClassFunctionLocation cfl = new ClassFunctionLocation(clsDef.bcFullName(), new NamedType(clsDef));
							cfl.castToCOBject = true;//function is not really on Object, so we must explicitly cast to CObject before calling
							
							ret.add( new TypeAndLocation(cloneSig, cfl) );
						}
						
						if(ret.isEmpty() && item.equals("toBinary")){//TODO: ugly way of doing things
							ArrayList<com.concurnas.compiler.ast.Type> arg = new ArrayList<com.concurnas.compiler.ast.Type>();
							arg.add(ScopeAndTypeChecker.const_cls_Encoder_nt);
							FuncType cloneSig = new FuncType(0,0, arg, ScopeAndTypeChecker.const_void );
							ClassDefJava clsDef = new ClassDefJava(CObject.class);//this is the true origin of this method...
							
							ClassFunctionLocation cfl = new ClassFunctionLocation(clsDef.bcFullName(), new NamedType(clsDef));
							cfl.castToCOBject = true;//function is not really on Object, so we must explicitly cast to CObject before calling
							
							ret.add( new TypeAndLocation(cloneSig, cfl) );
						}
						
						if(ret.isEmpty() && item.equals("fromBinary")){//TODO: ugly way of doing things
							ArrayList<com.concurnas.compiler.ast.Type> arg = new ArrayList<com.concurnas.compiler.ast.Type>();
							arg.add(ScopeAndTypeChecker.const_cls_Decoder_nt);
							FuncType cloneSig = new FuncType(0,0, arg, ScopeAndTypeChecker.const_void );
							ClassDefJava clsDef = new ClassDefJava(CObject.class);//this is the true origin of this method...
							
							ClassFunctionLocation cfl = new ClassFunctionLocation(clsDef.bcFullName(), new NamedType(clsDef));
							cfl.castToCOBject = true;//function is not really on Object, so we must explicitly cast to CObject before calling
							
							ret.add( new TypeAndLocation(cloneSig, cfl) );
						}
						
						if(ret.isEmpty() && item.equals("metaBinary")){//TODO: ugly way of doing things
							ArrayList<com.concurnas.compiler.ast.Type> arg = new ArrayList<com.concurnas.compiler.ast.Type>();
							FuncType cloneSig = new FuncType(0,0, arg, ScopeAndTypeChecker.const_string1dAr );
							ClassDefJava clsDef = new ClassDefJava(CObject.class);//this is the true origin of this method...
							
							ClassFunctionLocation cfl = new ClassFunctionLocation(clsDef.bcFullName(), new NamedType(clsDef));
							cfl.castToCOBject = true;//function is not really on Object, so we must explicitly cast to CObject before calling
							
							ret.add( new TypeAndLocation(cloneSig, cfl) );
						}
						
						if(ret.isEmpty() && item.equals("delete")){//TODO: ugly way of doing things
							FuncType cloneSig = new FuncType(0,0, new ArrayList<com.concurnas.compiler.ast.Type>(), ScopeAndTypeChecker.const_void );
							ClassDefJava clsDef = new ClassDefJava(CObject.class);//this is the true origin of this method...
							
							ClassFunctionLocation cfl = new ClassFunctionLocation(clsDef.bcFullName(), new NamedType(clsDef));
							cfl.castToCOBject = true;//function is not really on Object, so we must explicitly cast to CObject before calling
							
							ret.add( new TypeAndLocation(cloneSig, cfl) );
						}
						
						return ret;
					}
					else if(type == ITEM_TYPE.TYPEDEF){
						TypeDefTypeProvider tp = getTypeDef(relevantClass, item);
						if(null != tp) {
							return tp;
						}
						
					}
					else /* type == ITEM_TYPE.VARIABLE STATIC */
					{
						Field got = getField(relevantClass, item);
						if(got==null){
							return null;
						}
						int modifiders = got.getModifiers();
						
						boolean isStatic = Modifier.isStatic(modifiders);
						if(!onlyStaticStuffAvailable ||(onlyStaticStuffAvailable && isStatic))
						{
							Type obtainedType = convertGenType(got.getGenericType(), nameToGenericMap, false);//CompiledClassUtils.ConvertCompiledClassToType(got.getType(), false);
							processAnnotsAndDecorate(got.getAnnotations(), obtainedType);
							
							com.concurnas.lang.internal.NullStatus ns = got.getAnnotation(com.concurnas.lang.internal.NullStatus.class);
							if(null != ns) {
								tagTypesWithNullable(null, obtainedType, ns.nullable());
							}
							
							Location loc;
							String ownerOfField = relevantClass.getName().replaceAll("\\.", "/");
							if(isStatic){
								loc = new LocationStaticField( ownerOfField, obtainedType);
							}
							else{
								loc = new LocationClassField(ownerOfField, new NamedType(new ClassDefJava(relevantClass)));
							}
							boolean isFinal = Modifier.isFinal(modifiders);
							if(isFinal){
								loc.setFinal(isFinal);
							}
							loc.setAccessModifier(getAM(modifiders));
								
							HashSet<TypeAndLocation> ret = new HashSet<TypeAndLocation>();
							ret.add(new TypeAndLocation(obtainedType, loc));
							return ret;
						}
						return null;
						
					}
					
				}
				else
				{//subclass OR if you really want a class ref then cheat and use this
					//not really STATIC
					Class<?>[] choices = relevantClass.getClasses();
					boolean found = false;
					for(Class<?> choice : choices)
					{
						String longName = choice.getCanonicalName();
						int lastDollar = longName.lastIndexOf(".");
						String subla = longName.substring(lastDollar+1);
						if(lastDollar != -1 && subla.equals(item))
						{
							//TODO: ensure that modifiers are respected
							
							relevantClass = choice;
							found=true;
							break;
						}
					}
					if(!found)
					{
						return null;
					}
				}
			}
			HashSet<TypeAndLocation> ret = new HashSet<TypeAndLocation>();
			ret.add(new TypeAndLocation(CompiledClassUtils.ConvertCompiledClassToType(relevantClass, false), null));
			return ret;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static TypeDefTypeProvider getTypeDef(Class<?> relevantClass, String item) {
		TypeDefTypeProvider tp = null;
		try{
			java.lang.annotation.Annotation[] annots = relevantClass.getAnnotations();
			if(annots.length > 0){
				
				for(java.lang.annotation.Annotation anot : annots){
					Class<?> annotcls = anot.annotationType();
					if(annotcls.equals(com.concurnas.lang.Typedefs.class)){
						
						Method gettds = annotcls.getMethod("typedefs", null);
						java.lang.annotation.Annotation[] typedefAnnotations = (java.lang.annotation.Annotation[]) gettds.invoke(anot, null);
						
						for(java.lang.annotation.Annotation typedefanot : typedefAnnotations){
							Class cls = typedefanot.annotationType();
							if(cls.equals(com.concurnas.lang.Typedef.class)){
								String name = (String)cls.getMethod("name", null).invoke(typedefanot, null);
								if(item == null || name.equals(item)){
									String typestr = (String)cls.getMethod("type", null).invoke(typedefanot, null);
									String defaultTypeStr = (String)cls.getMethod("defaultType", null).invoke(typedefanot, null);
									String[] arguments = (String[])cls.getMethod("args", null).invoke(typedefanot, null);
									ArrayList<GenericType> generics = new ArrayList<GenericType>(arguments.length);
									
									for(String arg : arguments){
										generics.add(new GenericType(arg, 0));
									}
									
									Type resolvedType = new TypeDefAnnotationTypeParser(typestr).toType();
									
									Type defaultType = defaultTypeStr.equals("")?null:new TypeDefAnnotationTypeParser(defaultTypeStr).toType();
									
									if(tp == null){
										tp = new TypeDefTypeProvider();
									}
									
									AccessModifier am = null;
									String location = "";
									try{
										am = AccessModifier.getAccessModifier((String)cls.getMethod("accessModifier", null).invoke(typedefanot, null));
									}
									catch(NoSuchMethodException e){
										am = AccessModifier.PUBLIC;
									}
									
									try {
										location = (String)cls.getMethod("location", null).invoke(typedefanot, null);
									}catch(NoSuchMethodException e){///?
									}
									
									tp.add(resolvedType, generics, am, location, name, defaultType);
								}
							}
						}
					}
				}
			}
		}
		catch(Exception e){
			//ignore
			return null;
		}
		
		return tp;
	}

	public static Field getField(Class<?> clazz, String name) {
	    Field field = null;
	    while (clazz != null && field == null) {
	        try {
	            field = clazz.getDeclaredField(name);
	        } catch (Exception e) {
	        }
	        clazz = clazz.getSuperclass();
	    }
	    return field;
	}
	
	private static AccessModifier getAM(int modies){
		AccessModifier accessModifier = AccessModifier.PRIVATE; 
		if(Modifier.isPublic(modies)){
			accessModifier = AccessModifier.PUBLIC;
		}
		else if(Modifier.isProtected(modies)){
			accessModifier = AccessModifier.PROTECTED;
		}
		return accessModifier;
	}
	
	
	public static List<Pair<String, TypeAndLocation>> getAllStaticAssets(Class<?> relevantClass){
		//get all methods, variables, typdefs, classes, traits, enums
		
		ClassDefJava clsDef = new ClassDefJava(relevantClass);
		NamedType ownerNt = new NamedType(clsDef);
		
		List<Pair<String, TypeAndLocation>> ret = new ArrayList<Pair<String, TypeAndLocation>>();
		
		//gpustub functions
		for(TypeAndLocation gpustub : getGPUdefStubfunctions(relevantClass, null)) {
			FuncType ft = (FuncType)gpustub.getType();
			ret.add(new Pair<String, TypeAndLocation>(ft.origonatingFuncDef.getMethodName(), gpustub));
		}
		
		//methods
		HashSet<MethodHolder> meths = getAllMethods(relevantClass);
		for(MethodHolder m : meths) {
			Method meth = m.held;
			if(Modifier.isStatic(meth.getModifiers())) {
				FuncType sig = convertMethodToFuncType(meth, null);
				
				ret.add(new Pair<String, TypeAndLocation>(meth.getName(), new TypeAndLocation(sig, new StaticFuncLocation(ownerNt))));				
			}
		}
		
		//typedefs
		TypeDefTypeProvider tp = getTypeDef(relevantClass, null);
		if(null != tp) {
			for(TypeDef argsToTypeAndGen : tp.alltypeAndGens) {
				AccessModifier am = argsToTypeAndGen.am;
				if(am == AccessModifier.PUBLIC || am == AccessModifier.PACKAGE) {
					ret.add(new Pair<String, TypeAndLocation>(argsToTypeAndGen.name, new TypeAndLocation(argsToTypeAndGen.rhstpye, new StaticFuncLocation(ownerNt))));	
				}
			}
		}
		
		//fields
		ArrayList<Fiveple<String, Type, Boolean, String, AccessModifier>> allfieds = getAllFields(relevantClass, null);
		for(Fiveple<String, Type, Boolean, String, AccessModifier> field : allfieds) {
			AccessModifier am = field.getE();
			if(am == AccessModifier.PUBLIC || am == AccessModifier.PACKAGE) {
				ret.add(new Pair<String, TypeAndLocation>(field.getA(), new TypeAndLocation(field.getB(), new StaticFuncLocation(ownerNt))));		
			}
		}
		
		//classes
		for(Class<?> decl : relevantClass.getDeclaredClasses()) {
			ret.add(new Pair<String, TypeAndLocation>(decl.getSimpleName(), new TypeAndLocation(null, new StaticFuncLocation(ownerNt))));	
		}
		
		
		return ret;
	}
	
}
