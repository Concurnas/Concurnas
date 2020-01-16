package com.concurnas.compiler.visitors.algos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import com.concurnas.compiler.ast.AccessModifier;
import com.concurnas.compiler.ast.Annotation;
import com.concurnas.compiler.ast.Annotations;
import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.ast.AssignStyleEnum;
import com.concurnas.compiler.ast.AsyncRefRef;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.CastExpression;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.DotOperator;
import com.concurnas.compiler.ast.DuffAssign;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.FuncInvoke;
import com.concurnas.compiler.ast.FuncInvokeArgs;
import com.concurnas.compiler.ast.FuncParam;
import com.concurnas.compiler.ast.FuncParams;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.IfStatement;
import com.concurnas.compiler.ast.ImpliInstance;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.New;
import com.concurnas.compiler.ast.NotExpression;
import com.concurnas.compiler.ast.ObjectProvider;
import com.concurnas.compiler.ast.ObjectProviderLine;
import com.concurnas.compiler.ast.ObjectProviderLineDepToExpr;
import com.concurnas.compiler.ast.ObjectProviderLineProvide;
import com.concurnas.compiler.ast.RefBoolean;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.RefThis;
import com.concurnas.compiler.ast.ReturnStatement;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.VarNull;
import com.concurnas.compiler.ast.VarString;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.Sevenple;
import com.concurnas.compiler.utils.Sixple;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.visitors.ErrorRaiseable;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.compiler.visitors.Utils;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker.CapMaskedErrs;
import com.concurnas.compiler.visitors.datastructs.TheScopeFrame;
import com.concurnas.runtime.Pair;

public class ProviderCreator {

	private ObjectProvider objectProvider;
	private ScopeAndTypeChecker satc;
	private ErrorRaiseable er;

	public ProviderCreator(ScopeAndTypeChecker satc, ObjectProvider objectProvider) {
		this.satc = satc;
		this.er = satc.getErrorRaiseableSupression();
		this.objectProvider = objectProvider;
	}
	
	private static class DependancyExpressionRhsCheck{
		private int line;
		private int col;
		private Type dependancy;
		private Expression fulfilment;
		private Type typeOnlyRHS;
		private boolean isDependant;
		private FuncDef localGenFakeFuncDef;

		public DependancyExpressionRhsCheck(int line, int col, Type dependancy, Expression fulfilment, boolean isDependant, FuncDef localGenFakeFuncDef, Type typeOnlyRHS) {
			this.line = line;
			this.col = col;
			this.dependancy = dependancy;
			this.fulfilment = fulfilment;
			this.isDependant = isDependant;
			this.localGenFakeFuncDef = localGenFakeFuncDef;
			this.typeOnlyRHS = typeOnlyRHS;
		}
	}
	
	private static class TypeAndName{
		public final Type type;
		public final String name;
		private final TypeAndName nameLessCache;

		public TypeAndName(Type type, String name) {
			this.type = type;
			this.name = name;
			if (name == null) {
				nameLessCache = this;
			} else {
				nameLessCache = new TypeAndName(type, null);
			}
		}
		
		public TypeAndName getNameLess() {
			return nameLessCache;
		}
		
		public boolean isNameNull(){
			return this.name == null;
		}
		
		public boolean equals(Object an ) {
			if(an instanceof TypeAndName) {
				TypeAndName other = (TypeAndName)an;
				if(this.type.equals(other.type)) {
					if(this.isNameNull()) {
						return other.isNameNull();
					}else {
						return this.name.equals(other.name);
					}
				}
			}
			return false;
		}
		
		public int hashCode() {
			return this.type.hashCode() + (name == null?0:this.name.hashCode());
		}
		
		public String toString() {
			if(this.name == null) {
				return this.type.toString();
			}else {
				return String.format("'%s' %s", this.name, this.type.toString());
			}
		}
		
	}
	
	private static class TypeOnlyDef {
		public Type lhsType;
		public HashMap<TypeAndName, Thruple<Expression, Integer, Integer>> nestedDeps;
		public HashMap<TypeAndName, TypeOnlyDef> nestedtypeOnlyDepQuali;
		public int line;
		public int col;

		public TypeOnlyDef(int line, int col, Type lhsType, HashMap<TypeAndName, Thruple<Expression, Integer, Integer>> nestedDeps, HashMap<TypeAndName, TypeOnlyDef> nestedtypeOnlyDepQuali) {
			this.line=line;
			this.col=col;
			this.lhsType=lhsType;
			this.nestedDeps=nestedDeps;
			this.nestedtypeOnlyDepQuali=nestedtypeOnlyDepQuali;
		}
	}
	
	private boolean procesDepLine(ObjectProviderLineDepToExpr oplte, 
			HashMap<TypeAndName, Thruple<Expression, Integer, Integer>> dependancyQauli, 
			HashMap<TypeAndName, TypeOnlyDef> typeOnlyDepQuali, 
			ArrayList<DependancyExpressionRhsCheck> rhsChecks,
			HashMap<TypeAndName, Block> scopedDependenciesToFulfil) {
		int linea = oplte.getLine();
		int cola = oplte.getColumn();
		boolean fail = false;
		satc.maskErrors(true);
		Type dependancy = (Type)oplte.dependency.accept(satc);
		Type rhsTypeOnly = null;
		if(oplte.typeOnlyRHS != null) {
			rhsTypeOnly = (Type)oplte.typeOnlyRHS.accept(satc);
		}
		
		if(!oplte.single && !oplte.shared && oplte.fulfilment == null && oplte.typeOnlyRHS == null) {
			satc.raiseError(linea, cola, "Invalid provide dependency qualification: " + dependancy + " on its own");
			fail=true;
		}
		
		
		ArrayList<CapMaskedErrs> cap = satc.getmaskedErrors();
		if(!cap.isEmpty()) {
			satc.applyMaskedErrors(cap);
			fail=true;
		}else {
			TypeAndName tAndN = new TypeAndName(dependancy, oplte.name);
			rhsChecks.add(new DependancyExpressionRhsCheck(linea, cola, dependancy, oplte.fulfilment, true, null, oplte.typeOnlyRHS));
			if(dependancyQauli.containsKey(tAndN) || typeOnlyDepQuali.containsKey(tAndN)) {
				satc.raiseError(linea, cola, "Provide dependency qualifications must be unique. A provide dependency qualifications with name %s has already been defined", tAndN);
				fail=true;
			}else {
				if(null != rhsTypeOnly) {
					if(null == TypeCheckUtils.checkSubType(this.er, dependancy, rhsTypeOnly)) {
							satc.raiseError(linea, cola, "Type qualification for type only qualifier must be equal to or a subtype of: %s. %s is not", dependancy, rhsTypeOnly);
					}else {
						HashMap<TypeAndName, Thruple<Expression, Integer, Integer>> nesteddependancyQauli = null;
						HashMap<TypeAndName, TypeOnlyDef> nestedtypeOnlyDepQuali=null;
						if(oplte.nestedDeps != null) {
							nesteddependancyQauli = new HashMap<TypeAndName, Thruple<Expression, Integer, Integer>>();
							nestedtypeOnlyDepQuali = new HashMap<TypeAndName, TypeOnlyDef>();
							
							for(ObjectProviderLineDepToExpr ople : oplte.nestedDeps) {
								fail |= procesDepLine( ople, nesteddependancyQauli, nestedtypeOnlyDepQuali, rhsChecks, scopedDependenciesToFulfil);
							}
						}
						
						typeOnlyDepQuali.put(tAndN, new TypeOnlyDef(linea, cola, rhsTypeOnly, nesteddependancyQauli, nestedtypeOnlyDepQuali));
					}
				}else {
					Expression toPut;
					if(oplte.single || oplte.shared) {
						String retName = getTmpVariableName();
						Block funcBlock = new Block(linea, cola);
						Block addto = createScopedCode(linea, cola, retName, dependancy, funcBlock, oplte.single);
						
						if(oplte.fulfilment == null) {
							scopedDependenciesToFulfil.put(new TypeAndName(dependancy, oplte.name), addto);
						}
						
						addto.add(oplte.fulfilment);
						String funcName = getDepTmpFuncName();
						//add function
						FuncDef fd = FuncDef.build(dependancy, new FuncParams(linea, cola, new FuncParam(linea, cola, sharedObjName, sharedObjMapType, true)) );
						fd.funcName = funcName;
						fd.funcblock = funcBlock;
						fd.shouldInferFuncType=false;
						fd.accessModifier = AccessModifier.PRIVATE;
						classBlock.add(fd);
						
						toPut = DotOperator.buildDotOperatorOneNonDirect(linea, cola, new RefThis(linea, cola), FuncInvoke.makeFuncInvoke(linea, cola, funcName, new RefName(sharedObjName)));
					}else {
						toPut = oplte.fulfilment;
					}
					
					dependancyQauli.put(tAndN, new Thruple<Expression, Integer, Integer>(toPut, linea, cola));
				}
			}	
		}
		
		return fail;
	}
	
	private boolean isTypedActorAndOfAvailable(NamedType nt) {
		if(TypeCheckUtils.isTypedActor(this.er, nt)) {
			ArrayList<Type> gens = nt.getGenericTypeElements();
			if(gens != null && gens.size() > 0) {
				return true;
			}
			
		}
		return false;
	}
	
	private HashMap<NamedType, HashMap<TypeAndName, Thruple<Expression, Integer, Integer>>> provideToNestedDep;
	private HashMap<NamedType, HashMap<TypeAndName, TypeOnlyDef>> provideToNestedDeptypeOnly;
	private HashMap<NamedType, String> provideToFuncName;
	private Block classBlock;
	private ClassDef opClassDef;
	
	private static final NamedType sharedObjMapType;
	static {
		//ArrayList<Type> genTypesKey = new ArrayList<Type>();
		//genTypesKey.add(ScopeAndTypeChecker.const_class_nt);
		//genTypesKey.add(ScopeAndTypeChecker.const_string);
		
		//ArrayList<Type> genTypes = new ArrayList<Type>();
		//genTypes.add(new NamedType(0,0, "Tuple2", genTypesKey, new ArrayList<Tuple<String, ArrayList<Type>>>()));
		//genTypes.add(ScopeAndTypeChecker.const_object);
		

		ArrayList<Type> genTypes = new ArrayList<Type>();
		genTypes.add(ScopeAndTypeChecker.const_string);
		genTypes.add(ScopeAndTypeChecker.const_object);
		
		
		sharedObjMapType = new NamedType(0, 0, ScopeAndTypeChecker.const_HashMapCLS, genTypes);
	}
	
	private static final String sharedObjName = "sharedObjs$";
	
	public boolean createCD() {
		int line = objectProvider.getLine();
		int col = objectProvider.getColumn();

		boolean isAbstract = false;
		
		ImpliInstance provider = new ImpliInstance(line, col, "com.concurnas.lang.ObjectProvider", new ArrayList<Type>()); 
		ArrayList<ImpliInstance> imps = new ArrayList<ImpliInstance>();
		imps.add(provider);
		
		classBlock = new Block(line, col);
		
		this.opClassDef = new ClassDef(line, col, objectProvider.accessModifier, false, false, objectProvider.providerName,
				objectProvider.classGenricList, 
				objectProvider.classDefArgs,
				null, new ArrayList<Type>(), new ArrayList<Expression>(), 
				imps,
				classBlock,
				isAbstract, null, null, false, new ArrayList<Expression>(), 
				objectProvider.isTransient,
				objectProvider.isShared,
				false);
		opClassDef.setAnnotations(objectProvider.getAnnotations());
		
		satc.currentlyInClassDef.add(opClassDef);
		
		TheScopeFrame classSF = opClassDef.getScopeFrameGenIfMissing(satc.currentScopeFrame, opClassDef);
		satc.currentScopeFrame = classSF;
		satc.currentScopeFrame.enterScope();
		
		boolean fail=false;
		HashMap<String, Sevenple<NamedType, Integer, Integer, HashMap<TypeAndName, Thruple<Expression, Integer, Integer>>, AccessModifier, Pair<Boolean, Boolean>, HashMap<TypeAndName, TypeOnlyDef>> > nameToProvidee = new HashMap<String, Sevenple<NamedType, Integer, Integer, HashMap<TypeAndName, Thruple<Expression, Integer, Integer>>, AccessModifier, Pair<Boolean, Boolean>, HashMap<TypeAndName, TypeOnlyDef>>>();
		HashMap<String, Sevenple<NamedType, Expression, Integer, Integer, ArrayList<Pair<String, NamedType>>, AccessModifier, Pair<Boolean, Boolean>>> nameToProvideeExpr = new HashMap<String, Sevenple<NamedType, Expression, Integer, Integer, ArrayList<Pair<String, NamedType>>, AccessModifier, Pair<Boolean, Boolean>>>();
		
		HashMap<TypeAndName, Thruple<Expression, Integer, Integer>> dependancyQauli = new HashMap<TypeAndName, Thruple<Expression, Integer, Integer>>();
		HashMap<TypeAndName, TypeOnlyDef> typeOnlyDepQuali = new HashMap<TypeAndName, TypeOnlyDef>();
		
		HashSet<TypeAndName> dependancyQauliWithProvider = new HashSet<TypeAndName>();
		
		ArrayList<DependancyExpressionRhsCheck> rhsChecks = new ArrayList<DependancyExpressionRhsCheck>();
		provideToNestedDep = new HashMap<NamedType, HashMap<TypeAndName, Thruple<Expression, Integer, Integer>>>();
		provideToNestedDeptypeOnly = new HashMap<NamedType, HashMap<TypeAndName, TypeOnlyDef>>();
		provideToFuncName = new HashMap<NamedType, String>();
		
		HashMap<TypeAndName, Block> scopedDependenciesToFulfil = new HashMap<TypeAndName, Block>();
		
		for(ObjectProviderLine linex : objectProvider.objectProviderBlock.lines) {
			if(linex instanceof ObjectProviderLineProvide) {
				int linea = ((ObjectProviderLineProvide) linex).getLine();
				int cola = ((ObjectProviderLineProvide) linex).getColumn();
				ObjectProviderLineProvide oplp = (ObjectProviderLineProvide)linex;
				
				FuncDef localGenFakeFuncDef = null;
				ArrayList<Pair<String, NamedType>> localgens = null;
				{
					localgens = linex.getLocalGens();
					if(!localgens.isEmpty()) {
						localGenFakeFuncDef = FuncDef.build(ScopeAndTypeChecker.const_object);
						localGenFakeFuncDef.methodGenricList = localgens;
						satc.currentlyInFuncDef.push(localGenFakeFuncDef);
					}
				}
				
				satc.maskErrors(true);
				Type tta = (Type)oplp.provides.accept(satc);
				ArrayList<CapMaskedErrs> cap = satc.getmaskedErrors();
				if(tta == null) {
					satc.raiseError(linea, cola, String.format(String.format("Provide defintion type: %s not found", oplp.provides)));
					fail=true;
				}else if(!(tta instanceof NamedType) || tta.hasArrayLevels()) {
					satc.raiseError(linea, cola, String.format("Provide defintions can be expressed only for non array object types"));
					fail=true;
				}
				else if(!cap.isEmpty()) {
					satc.applyMaskedErrors(cap);
					fail=true;
				}else {
					NamedType nt = (NamedType)tta;
					String name;
					
					if(oplp.provName != null) {
						name = oplp.provName;
					}else {
						if(isTypedActorAndOfAvailable(nt)) {
							NamedType tt = (NamedType)nt.getGenericTypeElements().get(0);
							name = tt.getSetClassDef().getClassName();
						}else{
							NamedType extType = nt;
							
							if(null != TypeCheckUtils.checkSubType(er, ScopeAndTypeChecker.getLazyNT(), extType)) {
								extType = (NamedType)extType.getGenericTypeElements().get(0);
							}
							
							extType = (NamedType)TypeCheckUtils.getRefType(extType);
							name = extType.getSetClassDef().getClassName();
							
							if(TypeCheckUtils.isTypedActor(this.er, extType)) {
								NamedType tt = TypeCheckUtils.extractRootActor(extType);
								tt=(NamedType)tt.getGenericTypeElements().get(0);
								name = tt.getSetClassDef().getClassName();
							}
						}
					}
										
					
					if(nameToProvidee.containsKey(name) || nameToProvideeExpr.containsKey(name)) {
						satc.raiseError(linea, cola, String.format("Provide defintions must be unique. A provide with name %s has already been defined", name));
						fail=true;
					}else {
						
						if(oplp.provideExpr != null) {
							NamedType dependancy = nt;
							TypeAndName tandN = new TypeAndName(dependancy, oplp.fieldName);
							if(dependancyQauli.containsKey(tandN) || typeOnlyDepQuali.containsKey(tandN)) {
								satc.raiseError(linea, cola, "Provide with qualification must be unique. A provide dependency qualifications with name %s has already been defined", dependancy);
								fail=true;
							}else {
								rhsChecks.add(new DependancyExpressionRhsCheck(linea, cola, dependancy, oplp.provideExpr, false, localGenFakeFuncDef, null));
								
								DotOperator callThis = DotOperator.buildDotOperatorOneNonDirect(linea, cola, new RefThis(linea, cola), FuncInvoke.makeFuncInvoke(linea, cola, name, new RefName(sharedObjName)));
								dependancyQauli.put(tandN, new Thruple<Expression, Integer, Integer>(callThis, linea, cola));
								nameToProvideeExpr.put(name, new Sevenple<NamedType, Expression, Integer, Integer, ArrayList<Pair<String, NamedType>>, AccessModifier, Pair<Boolean, Boolean>>(nt, oplp.provideExpr, linea, cola, localgens, oplp.accessModi, new Pair<Boolean, Boolean>(oplp.single, oplp.shared)));
								dependancyQauliWithProvider.add(tandN);
							}
							
						}else {
							
							if(localGenFakeFuncDef != null) {
								satc.raiseError(linea, cola, "Generic parameters for provide definitions must have a right hand side expression");
								fail=true;
							}
							
							HashMap<TypeAndName, Thruple<Expression, Integer, Integer>> nesteddependancyQauli = null;
							HashMap<TypeAndName, TypeOnlyDef> nestedtypeOnlyDepQuali = null;
							if(oplp.nestedDeps != null) {
								nesteddependancyQauli = new HashMap<TypeAndName, Thruple<Expression, Integer, Integer>>();
								nestedtypeOnlyDepQuali = new HashMap<TypeAndName, TypeOnlyDef>();
								
								for(ObjectProviderLineDepToExpr ople : oplp.nestedDeps) {
									fail |= procesDepLine( ople, nesteddependancyQauli, nestedtypeOnlyDepQuali, rhsChecks, scopedDependenciesToFulfil);
								}
							}
							
							nameToProvidee.put(name, new Sevenple<NamedType, Integer, Integer, HashMap<TypeAndName, Thruple<Expression, Integer, Integer>>, AccessModifier, Pair<Boolean, Boolean>, HashMap<TypeAndName, TypeOnlyDef>>(nt, linea, cola, nesteddependancyQauli, oplp.accessModi, new Pair<Boolean, Boolean>(oplp.single, oplp.shared), nestedtypeOnlyDepQuali));
							provideToNestedDep.put(nt, nesteddependancyQauli);
							provideToNestedDeptypeOnly.put(nt, nestedtypeOnlyDepQuali);
							provideToFuncName.put(nt, name);
						}
					}
				}
				
				if(localGenFakeFuncDef != null) {
					satc.currentlyInFuncDef.pop();
				}
				
			}else if(linex instanceof ObjectProviderLineDepToExpr) {
				ObjectProviderLineDepToExpr asopldep = (ObjectProviderLineDepToExpr)linex;
				
				fail |= procesDepLine(asopldep, dependancyQauli, typeOnlyDepQuali, rhsChecks, scopedDependenciesToFulfil);
			}
		}
		
		//if(nameToProvidee.isEmpty() && nameToProvideeExpr.isEmpty()) {
		if(!objectProvider.objectProviderBlock.lines.stream().filter(a -> a instanceof ObjectProviderLineProvide).findFirst().isPresent()) {
			satc.raiseError(line, col, "Provider %s must have at least one provide definition", objectProvider.providerName);
			fail=true;
		}
		HashSet<TypeAndName> nestedDepsBeenUsedAlready = new HashSet<TypeAndName>();
		HashSet<TypeAndName> typesHavingBeenInjectedAlready = new HashSet<TypeAndName>();
		if(!fail) {
			for(TypeAndName tandName : scopedDependenciesToFulfil.keySet()) {//satisfy the likes of: single Bean; shared AnotherBean - by filling in method code with a staified dependancy code for these
				Block toFulfil = scopedDependenciesToFulfil.get(tandName);
				
				Type tt = tandName.type;
				if(tt instanceof NamedType) {
					NamedType dep = (NamedType)tandName.type;
					
					
					HashMap<TypeAndName, Thruple<Expression, Integer, Integer>> nestedDeps = new HashMap<TypeAndName, Thruple<Expression, Integer, Integer>>();
					HashMap<TypeAndName, TypeOnlyDef> nestedtypeOnlyDepQuali = new HashMap<TypeAndName, TypeOnlyDef>();
					
					//remove self
					HashMap<TypeAndName, Thruple<Expression, Integer, Integer>> depsForOwnUse = new HashMap<TypeAndName, Thruple<Expression, Integer, Integer>>(dependancyQauli);
					
					{
						depsForOwnUse.remove(tandName);
						Type rem = tandName.type;
						Set<TypeAndName> toRemove = new HashSet<TypeAndName>();
						depsForOwnUse.keySet().stream().filter(x -> x.type.equals(rem)).forEach(a -> toRemove.add(a));
						
						if(!toRemove.isEmpty()) {
							toRemove.forEach(a -> depsForOwnUse.remove(a));
						}
					}
					
					Expression satifaction = satifyDependancy(toFulfil.getLine(), toFulfil.getColumn(), dep, tandName, "constructor for: " + dep, depsForOwnUse, typeOnlyDepQuali, typesHavingBeenInjectedAlready, nestedDeps, nestedtypeOnlyDepQuali, nestedDepsBeenUsedAlready);
					if(null == satifaction) {
						fail=true;
					}else {
						toFulfil.add(new LineHolder(new DuffAssign(satifaction)));
					}
				}else {
					satc.raiseError(line, col, "Right hand side of dependency must be specified for: %s", tt);
					fail=true;
				}
			}
		}
		
		
		HashSet<TypeAndName> typesHavingBeenInjected = new HashSet<TypeAndName>(typesHavingBeenInjectedAlready);
		for(String name : nameToProvidee.keySet()) {
			Sevenple<NamedType, Integer, Integer, HashMap<TypeAndName, Thruple<Expression, Integer, Integer>>, AccessModifier, Pair<Boolean, Boolean>, HashMap<TypeAndName, TypeOnlyDef>> entry = nameToProvidee.get(name);
			int linea = entry.getB();
			int cola = entry.getC();
			NamedType nt = entry.getA();
			AccessModifier am = entry.getE();
			
			Pair<Boolean, Boolean> singleAndShared = entry.getF();
			boolean isSingle = singleAndShared.getA();
			boolean isShared = singleAndShared.getB();
			
			HashMap<TypeAndName, Thruple<Expression, Integer, Integer>> nestedDeps =  entry.getD();
			HashSet<TypeAndName> nestedDepsBeenUsed = nestedDeps == null ? null : new HashSet<TypeAndName>(nestedDepsBeenUsedAlready);
			HashMap<TypeAndName, TypeOnlyDef> nestedtypeOnlyDepQuali = entry.getG();
			
			//satisfy the likes of: single Bean; shared AnotherBean - by filling in method code with a staified dependancy code for these
			//HashMap<TypeAndName, Block> nestedscopedDependenciesToFulfil
			
			
			//inject constructor, fields, methods
			FuncDef constructorToInject = null;
			FuncType constructorToInjectType = null;
			boolean isTypedActor = false;
			
			boolean createLazy = false;
			if(null != TypeCheckUtils.checkSubType(er, ScopeAndTypeChecker.getLazyNT(), nt)) {
				nt = (NamedType)nt.getGenericTypeElements().get(0);
				createLazy=true;
			}
			
			if(isTypedActorAndOfAvailable(nt)){
				nt = (NamedType)nt.getGenericTypeElements().get(0);
				isTypedActor=true;
			}
			
			{
				ConstructorInfo cinfo = findInjectableConstructor(linea, cola, nt);
				constructorToInject = cinfo.constructorToInject;
				constructorToInjectType = cinfo.constructorToInjectType;
				fail |= cinfo.fail;
			}
			
			
			Pair<Boolean, ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>>> fieldsToInjectf = getFieldsToInject(linea, cola, nt, null, null);
			ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>> fieldsToInject = fieldsToInjectf.getB();
			fail |= fieldsToInjectf.getA();
			HashMap<String, ArrayList<TypeAndName>> methToDeps = getInjectableMethods(nt);
			
			if(null != constructorToInject) {//constructor
				List<TypeAndName> construInputs = refiedConstructorArgs(constructorToInjectType, (NamedType)TypeCheckUtils.getRefType(nt));
				//now lets gennerate the code for this instance
				
				
				Block blk = new Block(linea, cola);
				fail |= performInjection(linea, cola, blk, nt, nt, construInputs, fieldsToInject, methToDeps, dependancyQauli, typeOnlyDepQuali, typesHavingBeenInjected, "", nestedDeps, nestedtypeOnlyDepQuali, nestedDepsBeenUsed, isTypedActor, isSingle, isShared, createLazy);
				
				if(!fail) {
					FuncDef fd = FuncDef.build(entry.getA(), new FuncParams(linea, cola, new FuncParam(linea, cola, sharedObjName, sharedObjMapType, true)) );
					fd.setLine(linea);
					fd.setColumn(cola);
					fd.funcName = name;
					fd.funcblock = blk;
					fd.shouldInferFuncType=false;
					fd.accessModifier = AccessModifier.PRIVATE;
					classBlock.add(fd);
				}else {
					addStubMethod(linea, cola, name, nt, classBlock, am);
				}
			}else if(!fieldsToInject.isEmpty() || !methToDeps.isEmpty()){
				satc.raiseError(linea, cola, String.format("Class to provide: %s cannot have injectable fields or methods without an injectable constructor", nt));
				fail=true;
				addStubMethod(linea, cola, name, nt, classBlock, am);
			}else {
				satc.raiseError(linea, cola, String.format("Class to provide: %s has no public fields, methods or constructors marked inject", nt));
				fail=true;
				addStubMethod(linea, cola, name, nt, classBlock, am);
			}
			
			
			FuncDef fd = FuncDef.build(entry.getA());
			fd.setLine(linea);
			fd.setColumn(cola);
			fd.funcName = name;
			Block blk = new Block(linea, cola);
			
			FuncInvoke fi = FuncInvoke.makeFuncInvoke(linea, cola, name, new New(linea, cola, sharedObjMapType, new FuncInvokeArgs(linea, cola), true));
			blk.add(new DuffAssign(fi));
			fd.funcblock = blk;
			fd.shouldInferFuncType=false;
			fd.annotations = new Annotations( new Annotation(0,0, "com.concurnas.lang.Provide", null, null, new ArrayList<String>()) );
			fd.accessModifier = am;
			classBlock.add(fd);
			
			//argless caller
			
			if(null != nestedDeps || null != nestedtypeOnlyDepQuali){
				HashSet<TypeAndName> allDecl = new HashSet<TypeAndName>(nestedDeps.keySet());//satc: declared but unused + other one
				
				if(null != nestedDeps) {
					allDecl.addAll(nestedDeps.keySet());
				}
				
				if(null != nestedtypeOnlyDepQuali) {
					allDecl.addAll(nestedtypeOnlyDepQuali.keySet());
				}
				
				allDecl.removeAll(nestedDepsBeenUsed);
				if(!allDecl.isEmpty()) {
					for(TypeAndName what : allDecl) {
						Thruple<Expression, Integer, Integer> q = null!=nestedDeps?nestedDeps.get(what):null;
						int lined;
						int cold;
						if(null == q) {
							TypeOnlyDef itm = nestedtypeOnlyDepQuali.get(what);
							lined = itm.line;
							cold = itm.col;
						}else {
							lined = q.getB();
							cold = q.getC();
						}
						
						satc.raiseError(lined, cold, String.format("Provide specific declared dependency: %s is not used", what));
						fail=true;
					}
				}
			}
		}
		
		for(String name: nameToProvideeExpr.keySet()) {
			Sevenple<NamedType, Expression, Integer, Integer, ArrayList<Pair<String, NamedType>>, AccessModifier, Pair<Boolean, Boolean>> dets = nameToProvideeExpr.get(name);
			int linea = dets.getC();
			int cola = dets.getD();
			AccessModifier am = dets.getF();
			Pair<Boolean, Boolean> singleShared = dets.getG(); 
			boolean single = singleShared.getA();
			boolean isShared = singleShared.getB();
			NamedType retType = dets.getA();
			if(!fail) {
				{
					FuncDef fd = FuncDef.build(retType, new FuncParams(linea, cola, new FuncParam(linea, cola, sharedObjName, sharedObjMapType, true)) );
					
					fd.setLine(linea);
					fd.setColumn(cola);
					
					Expression expr = dets.getB();
					
					if(null != TypeCheckUtils.checkSubType(er, ScopeAndTypeChecker.getLazyNT(), retType) ) {
						Type lazyOf = retType.getGenericTypeElements().get(0);
						expr = Utils.createNewLazyObject(this.satc, linea, cola, lazyOf, lazyOf, expr, false);
					}
					
					
					Block blk = new Block(linea, cola);
					if(single || isShared) {
						String retName = getTmpVariableName();
						Block addto = createScopedCode(linea, cola, retName, retType, blk, single);
						addto.add(new LineHolder(new DuffAssign(linea, cola, expr)));
					}else {
						blk.add(new LineHolder(new DuffAssign(linea, cola, expr)));
					}
					
					fd.funcName = name;
					fd.funcblock = blk;
					fd.shouldInferFuncType=false;
					//fd.annotations = new Annotations( new Annotation(0,0, "com.concurnas.lang.Provide", null, null, new ArrayList<String>()) );
					fd.accessModifier = AccessModifier.PRIVATE;
					if(dets.getE() != null) {
						fd.methodGenricList = dets.getE();
					}
					
					classBlock.add(fd);
				}
				
				
				{
					FuncDef fd = FuncDef.build(retType);
					fd.setLine(linea);
					fd.setColumn(cola);
					fd.funcName = name;
					Block blk = new Block(linea, cola);
					
					FuncInvoke fi = FuncInvoke.makeFuncInvoke(linea, cola, name, new New(linea, cola, sharedObjMapType, new FuncInvokeArgs(linea, cola), true));
					blk.add(new DuffAssign(fi));
					fd.funcblock = blk;
					fd.shouldInferFuncType=false;
					fd.annotations = new Annotations( new Annotation(0,0, "com.concurnas.lang.Provide", null, null, new ArrayList<String>()) );
					fd.accessModifier = am;
					
					if(dets.getE() != null) {
						fd.methodGenricList = dets.getE();
					}
					
					classBlock.add(fd);
				}
				
				
			}else {
				addStubMethod(linea, cola, name, retType, classBlock, am);
			}
		}
		
		
		{
			HashSet<TypeAndName> allDecl = new HashSet<TypeAndName>(dependancyQauli.keySet());
			allDecl.addAll(typeOnlyDepQuali.keySet());
			allDecl.removeAll(typesHavingBeenInjected);
			allDecl.removeAll(dependancyQauliWithProvider);
			if(!allDecl.isEmpty()) {
				for(TypeAndName what : allDecl) {
					Thruple<Expression, Integer, Integer> q = dependancyQauli.get(what);
					int lined;
					int cold;
					if(null == q) {
						TypeOnlyDef tod = typeOnlyDepQuali.get(what);
						lined = tod.line;
						cold = tod.col;
					}else {
						lined = q.getB();
						cold = q.getC();
					}
					satc.raiseError(lined, cold, String.format("Declared dependency: %s is not used", what));
					fail=true;
				}
			}
		}
		
		satc.currentScopeFrame = satc.currentScopeFrame.leaveScope();
		satc.currentlyInClassDef.pop();
		
		if(!rhsChecks.isEmpty()) {
			
			if(null != objectProvider.classDefArgs) {
				satc.maskErrors(false);
				opClassDef.accept(satc);
				satc.maskedErrors();
			}
			
			satc.currentlyInClassDef.push(opClassDef);
			
			for(DependancyExpressionRhsCheck check : rhsChecks) {
				
				if(null != check.fulfilment) {
					if(check.localGenFakeFuncDef != null) {
						satc.currentlyInFuncDef.push(check.localGenFakeFuncDef);
					}
					
					satc.maskErrors(true);
					
					Type fulfilType = (Type)check.fulfilment.accept(satc);
					
					ArrayList<CapMaskedErrs> cap = satc.getmaskedErrors();
					if(!cap.isEmpty()) {
						fail=true;
						satc.applyMaskedErrors(cap);
					}else {
						
						if(check.dependancy instanceof NamedType && null != TypeCheckUtils.checkSubType(er, ScopeAndTypeChecker.getLazyNT(), check.dependancy) ) {
							Type lazyType = ((NamedType)check.dependancy).getGenericTypeElements().get(0);
							if(null != TypeCheckUtils.checkSubType(this.er, lazyType, fulfilType)) {
								fulfilType = check.dependancy;//great! rhs can satify lazy lhs
							}
						}
						
						if(null == TypeCheckUtils.checkSubType(this.er, check.dependancy, fulfilType)) {
							/*if(check.typeOnlyRHS != null) {
								satc.raiseError(check.line, check.col, "Type qualification for type only qualifier must be equal to or a subtype of: %s. %s is not", check.dependancy, fulfilType);
							}else*/ if(check.isDependant) {
								satc.raiseError(check.line, check.col, "Expression for provide dependency qualification must be equal to or a subtype of: %s. %s is not", check.dependancy, fulfilType);
							}else {
								satc.raiseError(check.line, check.col, "Expression on right hand side of provide must be equal to or a subtype of: %s. %s is not", check.dependancy, fulfilType);
							}
							fail=true;
						}
					}
					

					if(check.localGenFakeFuncDef != null) {
						satc.currentlyInFuncDef.pop();
					}
				}
			
			}
			satc.currentlyInClassDef.pop();
		}
		
		opClassDef.objProvider = true;
		
		objectProvider.astRedirect = opClassDef;
		
		return fail;
	}
	
	private void addStubMethod(int line, int col, String name, NamedType ofType, Block classBlock, AccessModifier am) {
		FuncDef fd = FuncDef.build(ofType);
		fd.setLine(line);
		fd.setColumn(col);
		
		Block blk = new Block(line, col);

		blk.add(new LineHolder(new ReturnStatement(line, col, new VarNull(line, col))));
		fd.funcName = name;
		fd.funcblock = blk;
		fd.shouldInferFuncType=false;
		fd.annotations = new Annotations( new Annotation(0,0, "com.concurnas.lang.Provide", null, null, new ArrayList<String>()) );
		fd.accessModifier = am;
		classBlock.add(fd);
	}
	
	private Stack<Type> depChain = new Stack<Type>();//things qualifying already

	private Expression namedTypeToDotOp(int linea, int cola, NamedType justsRefType, ArrayList<Expression> refconArgs, Expression preNewLine) {
		ArrayList<Expression> refLevelInvoke = new ArrayList<Expression>();
		if(null != preNewLine) {
			refLevelInvoke.add(preNewLine);
		}
		
		ClassDef cd = justsRefType.getSetClassDef();
		String packageName = cd.packageName.replace('$', '.');
		for(String item : packageName.split(".")) {
			refLevelInvoke.add(new RefName(linea, cola,item));
		}
		
		if(refLevelInvoke.isEmpty()) {
			return new RefName(linea, cola, cd.getClassName());
		}
		
		if(null == refconArgs) {
			refLevelInvoke.add(new RefName(linea, cola, cd.getClassName()));
		}else {
			refLevelInvoke.add(FuncInvoke.makeFuncInvoke(linea, cola, cd.getClassName(), refconArgs));
		}
		
		return new DotOperator(linea, cola, refLevelInvoke, false);
	}
	
	private Block createScopedCode(int linea, int cola, String retName, Type fieldType, Block blk, boolean isSingle) {
		Block addto = new Block(linea, cola);
		addto.isolated = true;
		addto.setShouldBePresevedOnStack(true);
		
		if(isSingle) {
			String checkVar = getTmpVarNameSingle(retName);

			//add fields to classdef
			AssignNew checkfield = new AssignNew(AccessModifier.PRIVATE, linea, cola, checkVar, ScopeAndTypeChecker.const_boolean, AssignStyleEnum.EQUALS, new RefBoolean(linea, cola, false));
			AssignNew theField = new AssignNew(AccessModifier.PRIVATE, linea, cola, retName, fieldType);
			this.classBlock.add(new LineHolder(checkfield));
			this.classBlock.add(new LineHolder(theField));
			
			Block holderBlock = new Block(linea, cola);
			
			Block ifblock = new Block(linea, cola);
			Expression iftest = new NotExpression(linea, cola, DotOperator.buildDotOperatorOneNonDirect(linea, cola, new RefThis(linea, cola), new RefName(linea, cola, checkVar)) );
			IfStatement ifs = new IfStatement(linea, cola, iftest, ifblock);
			
			ifblock.add(new LineHolder(new AssignExisting(linea, cola, DotOperator.buildDotOperatorOneNonDirect(linea, cola, new RefThis(linea, cola), new RefName(linea, cola, retName)), AssignStyleEnum.EQUALS, addto)));
			ifblock.add(new LineHolder(new AssignExisting(linea, cola, DotOperator.buildDotOperatorOneNonDirect(linea, cola, new RefThis(linea, cola), new RefName(linea, cola, checkVar)), AssignStyleEnum.EQUALS, new RefBoolean(linea, cola, true))));
			
			ifblock.isolated = true;
			ifblock.setShouldBePresevedOnStack(true);
			
			holderBlock.add(new LineHolder(ifs));
			
			Expression lastOne =  DotOperator.buildDotOperatorOneNonDirect(linea, cola, new RefThis(linea, cola), new RefName(linea, cola, retName));
			
			if(fieldType instanceof NamedType && null != TypeCheckUtils.checkSubType(er, ScopeAndTypeChecker.getLazyNT(), fieldType) ) {
				lastOne = new AsyncRefRef(linea, cola, lastOne, 1);
			}
			
			holderBlock.add(new LineHolder(new DuffAssign(lastOne)));
			holderBlock.isolated = true;
			holderBlock.setShouldBePresevedOnStack(true);
			
			blk.add(new LineHolder(holderBlock));
		}else {//shared
			//key = (type.class, name);
			//if( sharedObjs$.contains(key)){ sharedObjs$.get(key) } else { sharedObjs$.put(key, {addto}); sharedObjs$.get(key) } as fType
			Block holderBlock = new Block(linea, cola);
			
			//ArrayList<Expression> tupleElements = new ArrayList<Expression>(2);
			//tupleElements.add(  new RefClass(linea, cola, fieldType ) );
			//tupleElements.add( new VarString(linea, cola, retName) );
			
			//AssignExisting ae = new AssignExisting(linea, cola, new RefName("key$"), AssignStyleEnum.EQUALS, new TupleExpression(linea, cola, tupleElements));
			AssignExisting ae = new AssignExisting(linea, cola, new RefName("key$"), AssignStyleEnum.EQUALS, new VarString(linea, cola, retName));
			holderBlock.add(new LineHolder(ae));
			
			Expression iftest = DotOperator.buildDotOperator(linea, cola, new RefName(sharedObjName), FuncInvoke.makeFuncInvoke(linea, cola, "containsKey",  new RefName("key$")));
			Block ifblock = new Block(linea, cola);
			Block elseb = new Block(linea, cola);
			IfStatement ifs = new IfStatement(linea, cola, iftest, ifblock, null, elseb);
			ifblock.add(new DuffAssign(DotOperator.buildDotOperator(linea, cola, new RefName(sharedObjName), FuncInvoke.makeFuncInvoke(linea, cola, "get",  new RefName("key$")))));
			
			elseb.add(new DuffAssign(DotOperator.buildDotOperator(linea, cola, new RefName(sharedObjName), FuncInvoke.makeFuncInvoke(linea, cola, "put",  new RefName("key$"), addto))));
			elseb.add(new DuffAssign(DotOperator.buildDotOperator(linea, cola, new RefName(sharedObjName), FuncInvoke.makeFuncInvoke(linea, cola, "get",  new RefName("key$")))));
			
			ifblock.isolated = true;
			elseb.isolated = true;
			ifblock.setShouldBePresevedOnStack(true);
			elseb.setShouldBePresevedOnStack(true);
			
			Expression lastOne = new CastExpression(linea, cola, fieldType, ifs);
			
			if(fieldType instanceof NamedType && null != TypeCheckUtils.checkSubType(er, ScopeAndTypeChecker.getLazyNT(), fieldType) ) {
				lastOne = new AsyncRefRef(linea, cola, lastOne, 1);
			}
			
			holderBlock.add(new LineHolder(new DuffAssign(lastOne)));
			
			holderBlock.isolated = true;
			holderBlock.setShouldBePresevedOnStack(true);
			blk.add(new LineHolder(holderBlock));
		}
		
		
		
		return addto;
	}
	
	private boolean performInjection(int linea, int cola, 
			Block blk, 
			NamedType rootDependancy, 
			NamedType nt, 
			List<TypeAndName> construInputs, 
			ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>> fieldsToInject, 
			HashMap<String, ArrayList<TypeAndName>> methToDeps,
			HashMap<TypeAndName, Thruple<Expression, Integer, Integer>> dependancyQauli,
			HashMap<TypeAndName, TypeOnlyDef> typeOnlyDepQuali,
			HashSet<TypeAndName> typesHavingBeenInjected,
			String errorPrefix, 
			HashMap<TypeAndName, Thruple<Expression, Integer, Integer>> nestedDeps,
			HashMap<TypeAndName, TypeOnlyDef> nestedtypeOnlyDepQuali,
			HashSet<TypeAndName> nestedDepsBeenUsed,
			boolean isTypedActor,
			boolean isSingle, 
			boolean isShared,
			boolean createLazy) {
		
		if(depChain.contains(nt)){
			satc.raiseError(linea, cola, String.format("Class to provide: %s has circular dependency in chain: [%s]", rootDependancy, errorPrefix));
			return true;
		}else{
			depChain.push(nt);
		}
		
		if(!errorPrefix.equals("")) {
			errorPrefix = String.format("%s -> ", errorPrefix);
		}
		
		boolean fail = false;
		ArrayList<Expression> construArgs = new ArrayList<Expression>();
		for(TypeAndName cinput : construInputs) {
			Expression satifaction = satifyDependancy(linea, cola, rootDependancy, cinput, errorPrefix + "constructor arg of type: " + cinput, dependancyQauli, typeOnlyDepQuali, typesHavingBeenInjected, nestedDeps, nestedtypeOnlyDepQuali, nestedDepsBeenUsed);
			if(null == satifaction) {
				fail=true;
			}
			construArgs.add(satifaction);
		}

		String retName = getTmpVariableName();
		
		Block addto = blk;
		
		if(isSingle || isShared) {
			addto = createScopedCode(linea, cola, retName, nt, blk, isSingle);
		}
		
		if(createLazy) {
			Block mainAddTo = new Block(linea, cola);
			//create new, with lambda etc
			Expression expr = Utils.createNewLazyObject(this.satc, linea, cola, nt, nt, mainAddTo, false);
			if(expr != null) {
				addto.add(new LineHolder(new DuffAssign(expr)));
			}
			addto = mainAddTo;
		}
		
		
		Expression newThing;
		//Type typeForNew = nt;
		if(isTypedActor) {
			NamedType typeee = ScopeAndTypeChecker.const_typed_actor.copyTypeSpecific();
			typeee.setLine(linea);
			typeee.setColumn(cola);
			typeee.setGenTypes(nt);
			typeee.isDefaultActor = true;

			newThing = new New(linea, cola, typeee, FuncInvokeArgs.manyargs(linea, cola, construArgs), true);
			//typeForNew = typeee;
		}else {
			
			NamedType tt = nt;
			ArrayList<NamedType> rts = TypeCheckUtils.getRefTypes(tt); 
			if(!rts.isEmpty()) {

				tt = (NamedType)TypeCheckUtils.getRefType(tt);
				newThing = new New(linea, cola, tt, FuncInvokeArgs.manyargs(linea, cola, construArgs), true);
				
				if(rts.stream().allMatch(a -> ScopeAndTypeChecker.const_Local_class.equals(a.getSetClassDef()))) {
					//if it's of form MyClass:::
					newThing = new AsyncRefRef(linea, cola, newThing, rts.size());
				}else {//if it's of form MyClass:GPURef
					Expression preNewLine = namedTypeToDotOp(linea, cola, tt, null, null);
					
					for(NamedType refItem : rts) {
						
						NamedType justsRefType = new NamedType(linea, cola, refItem.namedType);
						justsRefType.setClassDef(refItem.getSetClassDef());
						
						//New refLevel = new New(linea, cola, tt, FuncInvokeArgs.manyargs(linea, cola, construArgs), true);
						preNewLine = new AsyncRefRef(linea, cola, preNewLine, 1);
						if(!ScopeAndTypeChecker.const_Local_class.equals(justsRefType.getSetClassDef())) {
							
							FuncType constructorToInjectType;
							{
								ConstructorInfo cinfo = findInjectableConstructor(linea, cola, justsRefType, true);
								constructorToInjectType = cinfo.constructorToInjectType;
								fail |= cinfo.fail;
							}

							ArrayList<Expression> refconArgs = new ArrayList<Expression>();
							if(null != constructorToInjectType) {
								List<TypeAndName> refLevelConArgs = refiedConstructorArgs(constructorToInjectType, justsRefType);
								
								for(TypeAndName cinput : refLevelConArgs) {
									Expression satifaction = satifyDependancy(linea, cola, rootDependancy, cinput, errorPrefix + String.format("constructor arg for: %s of type: %s", justsRefType, cinput), dependancyQauli, typeOnlyDepQuali, typesHavingBeenInjected, nestedDeps, nestedtypeOnlyDepQuali, nestedDepsBeenUsed);
									if(null == satifaction) {
										fail=true;
									}
									refconArgs.add(satifaction);
								}
							}//zero arg version

							preNewLine = namedTypeToDotOp(linea, cola, justsRefType,  refconArgs, preNewLine);
						}
					}
					
					AssignExisting an = new AssignExisting(linea, cola, retName, AssignStyleEnum.EQUALS, preNewLine);							
					addto.add(new LineHolder(an));
				}
			}else {
				newThing = new New(linea, cola, tt, FuncInvokeArgs.manyargs(linea, cola, construArgs), true);
			}
		}
		AssignExisting an = new AssignExisting(linea, cola, retName, AssignStyleEnum.EQUALS, newThing);								
		addto.add(new LineHolder(an));
		
		if(!fieldsToInject.isEmpty() || !methToDeps.isEmpty()) {
			//AssignNew an = new AssignNew(null, linea, cola, retName, typeForNew, AssignStyleEnum.EQUALS, newThing);								
			
			//assign above to temp variable
			
			for(String mname : methToDeps.keySet()) {
				ArrayList<Expression> mtehodArgs = new ArrayList<Expression>();
				ArrayList<TypeAndName> args = methToDeps.get(mname);
				for(TypeAndName toast : args) {
					Expression satifaction = satifyDependancy(linea, cola, rootDependancy, toast, errorPrefix + "method arg " + mname + " of type: " + toast, dependancyQauli, typeOnlyDepQuali, typesHavingBeenInjected, nestedDeps, nestedtypeOnlyDepQuali, nestedDepsBeenUsed);
					if(null == satifaction) {
						fail=true;
					}
					mtehodArgs.add(satifaction);
				}
				
				if(!fail) {
					DotOperator fi = DotOperator.buildDotOperatorOneNonDirect(linea, cola, new RefName(retName), FuncInvoke.makeFuncInvoke(linea, cola, mname, mtehodArgs));
					addto.add(new LineHolder(new DuffAssign(fi)));
				}
			}
			
			for(Sixple<String, Type, Boolean, AccessModifier, Boolean, String> field : fieldsToInject) {
				String fname = field.getF();
				if(null == fname) {
					fname = field.getA();
				}
				String normalFName = field.getA();
				Expression satifaction = satifyDependancy(linea, cola, rootDependancy, new TypeAndName(field.getB(), fname), errorPrefix + "field " + normalFName + " of type: " + field.getB(), dependancyQauli, typeOnlyDepQuali, typesHavingBeenInjected, nestedDeps, nestedtypeOnlyDepQuali, nestedDepsBeenUsed);
				if(null == satifaction) {
					fail=true;
				}else {
					AssignExisting ae = new AssignExisting(linea, cola, DotOperator.buildDotOperator(linea, cola, new RefName(retName), new RefName(normalFName)), AssignStyleEnum.EQUALS, satifaction);
					addto.add(new LineHolder(ae));
				}
			}
			
			if(!fail) {
				addto.add(new LineHolder(new DuffAssign(linea, cola, new RefName(linea, cola, retName))));
			}
			
		}else {
			addto.add(new LineHolder(new DuffAssign(linea, cola, new RefName(linea, cola, retName))));
		}
		
		
		
		depChain.pop();
		return fail;
	}
	
	
	
	private static class ConstructorInfo{
		public final FuncDef constructorToInject;
		public final FuncType constructorToInjectType;
		public final boolean fail;
		
		public ConstructorInfo(FuncDef constructorToInject, FuncType constructorToInjectType, boolean fail) {
			this.constructorToInject = constructorToInject;
			this.constructorToInjectType = constructorToInjectType;
			this.fail = fail;
		}
	}
	
	
	private ConstructorInfo findInjectableConstructor(int line, int col, NamedType nt) {
		return findInjectableConstructor(line, col, nt, false);
	}
	
	private ConstructorInfo findInjectableConstructor(int line, int col, NamedType nt, boolean keepAsRef) {
		FuncDef constructorToInject = null;
		FuncType constructorToInjectType = null;
		FuncDef constructorToInjectZA = null;
		FuncType constructorToInjectTypeZA = null;
		boolean fail=false;

		if(!keepAsRef) {
			nt = (NamedType)TypeCheckUtils.getRefType(nt);
		}
		
		List<FuncType> ntas = nt.getAllConstructors(satc);
		
		for(FuncType constru : ntas) {
			FuncDef origin = constru.origonatingFuncDef;
			if(origin.isInjected) {//if injected then must be public
				if(null != constructorToInject) {
					satc.raiseError(line, col, String.format("Ambiguous constructor injection definition in class to provide, more than one constructor for: %s has been marked inject", nt));
					fail=true;
				}else {
					constructorToInject = origin;
					constructorToInjectType = constru;
				}
			}/*else if(constru.inputs.isEmpty()) {
				if(satc.isAccesible(origin.accessModifier, nt, this.opClassDef, true, this.opClassDef.packageName)) {
					constructorToInjectZA = origin;
					constructorToInjectTypeZA = constru;
				}
			}*/
		}
		
		if(null == constructorToInject) {
			constructorToInject = constructorToInjectZA;
			constructorToInjectType = constructorToInjectTypeZA;
		}
			
		
		return new ConstructorInfo(constructorToInject, constructorToInjectType, fail);
		
	}
	
	private int tmpVarCnt = 0;
	private int dependancySingleCount = 0;
	
	private String getTmpVariableName() {
		return "objProvi$tmpVar" + tmpVarCnt++;
	}
	
	private String getDepTmpFuncName() {
		return "objProvi$depFunc" + dependancySingleCount++;
	}
	
	private String getTmpVarNameSingle(String myString) {
		return myString + "$BeenSet";
	}
	
	private Pair<Boolean, ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>>> getFieldsToInject(int line, int col, NamedType nt, NamedType rootDependancy, String ofType){
		ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>> fieldsToInject = new ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>>();
		
		HashSet<String> fnames = new HashSet<String>();
		for(Sixple<String, Type, Boolean, AccessModifier, Boolean, String> field : nt.getAllFields(false)) {
			if(field.getE()) {//isinjected
				if(!field.getD().equals(AccessModifier.PUBLIC)) {
					fnames.add(field.getA());
				}
				else {
					fieldsToInject.add(field);
				}
			}
		}
		boolean fail = false;
		if(!fnames.isEmpty()) {
			String fnamesStr = String.join(", ", fnames.stream().sorted().collect(Collectors.toList()));
			if(null != rootDependancy) {
				satc.raiseError(line, col, String.format("Class to provide: %s has nested dependant class having injectable fields marked as being injectable but it is not public: [%s] : %s", rootDependancy, ofType, fnamesStr));
			}else {
				satc.raiseError(line, col, "Fields %s of %s are marked as being injectable but are not public", fnamesStr, nt);
			}
			fail=true;
		}
		
		return new Pair<Boolean, ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>>>(fail, fieldsToInject);
	}
	
	private TypeAndName depQualiHolderContains(HashMap<TypeAndName, Thruple<Expression, Integer, Integer>> nestedDeps, TypeAndName what) {
		if(nestedDeps.containsKey(what)) {
			return what;
		}else {
			TypeAndName nameLess = what.getNameLess();
			if(nestedDeps.containsKey(nameLess)) {
				return nameLess;
			}
		}
		return null;
	}
	

	private TypeAndName depQualiHolderContainsTO(HashMap<TypeAndName, TypeOnlyDef> typeOnlyDepQuali, TypeAndName what) {
		if(typeOnlyDepQuali.containsKey(what)) {
			return what;
		}else {
			TypeAndName nameLess = what.getNameLess();
			if(typeOnlyDepQuali.containsKey(nameLess)) {
				return nameLess;
			}
		}
		return null;
	}
	
	
	private Expression satifyDependancy(int line, int col, 
			NamedType rootDependancy, 
			TypeAndName wanted, 
			String ofType, 
			HashMap<TypeAndName, Thruple<Expression, Integer, Integer>> dependancyQauli, 
			HashMap<TypeAndName, TypeOnlyDef> typeOnlyDepQuali, 
			HashSet<TypeAndName> typesHavingBeenInjected, 
			HashMap<TypeAndName, Thruple<Expression, Integer, Integer>> nestedDeps, 
			HashMap<TypeAndName, TypeOnlyDef> nestedtypeOnlyDepQuali,
			HashSet<TypeAndName> nestedDepsBeenUsed) {
		
		boolean createLazy = false;
		
		if(wanted.type instanceof NamedType) {
			if(null != TypeCheckUtils.checkSubType(er, ScopeAndTypeChecker.getLazyNT(), wanted.type) ) {
				//see if we can obtain a direct match for the lazy type
				TypeAndName inNested = nestedDeps == null?null:depQualiHolderContains(nestedDeps, wanted);
				if(null != inNested) {
					nestedDepsBeenUsed.add(inNested);
					return nestedDeps.get(inNested).getA();
				}
				
				TypeAndName inNormalDQ = depQualiHolderContains(dependancyQauli, wanted);
				if(null != inNormalDQ) {
					typesHavingBeenInjected.add(inNormalDQ);
					return dependancyQauli.get(inNormalDQ).getA();
				}
				
				wanted = new TypeAndName( ((NamedType)wanted.type).getGenericTypeElements().get(0), wanted.name);
			}else if(null != TypeCheckUtils.checkSubType(er, ScopeAndTypeChecker.getProviderNT(), wanted.type)) {//provider
				TypeAndName inNested = nestedDeps == null?null:depQualiHolderContains(nestedDeps, wanted);
				if(null != inNested) {
					nestedDepsBeenUsed.add(inNested);
					return nestedDeps.get(inNested).getA();
				}
				
				TypeAndName inNormalDQ = depQualiHolderContains(dependancyQauli, wanted);
				if(null != inNormalDQ) {
					typesHavingBeenInjected.add(inNormalDQ);
					return dependancyQauli.get(inNormalDQ).getA();
				}
				
				//remap the returned expression
				TypeAndName innerType = new TypeAndName(((NamedType)wanted.type).getGenericTypeElements().get(0), wanted.name);
				Expression forProvide = satifyDependancy(line, col, rootDependancy, innerType, ofType, dependancyQauli, typeOnlyDepQuali,
						 typesHavingBeenInjected, nestedDeps, nestedtypeOnlyDepQuali, nestedDepsBeenUsed);
				
				return Utils.createNewProvider(line, col, innerType.type, forProvide);
			}else if(null != TypeCheckUtils.checkSubType(er, ScopeAndTypeChecker.const_Optional, wanted.type)) {//optional, code is a bit repetative
				TypeAndName inNested = nestedDeps == null?null:depQualiHolderContains(nestedDeps, wanted);
				if(null != inNested) {
					nestedDepsBeenUsed.add(inNested);
					return nestedDeps.get(inNested).getA();
				}
				
				TypeAndName inNormalDQ = depQualiHolderContains(dependancyQauli, wanted);
				if(null != inNormalDQ) {
					typesHavingBeenInjected.add(inNormalDQ);
					return dependancyQauli.get(inNormalDQ).getA();
				}
				
				//remap the returned expression
				TypeAndName innerType = new TypeAndName(((NamedType)wanted.type).getGenericTypeElements().get(0), wanted.name);
				satc.maskErrors(false);
				Expression forOptional = satifyDependancy(line, col, rootDependancy, innerType, ofType, dependancyQauli, typeOnlyDepQuali,
						 typesHavingBeenInjected, nestedDeps, nestedtypeOnlyDepQuali, nestedDepsBeenUsed);
				satc.maskedErrors();
				
				return Utils.createNewOptional(line, col, innerType.type, forOptional);
			}
		}
		
		TypeAndName inNested = nestedDeps == null?null:depQualiHolderContains(nestedDeps, wanted);
		if(null != inNested) {
			nestedDepsBeenUsed.add(inNested);
			return nestedDeps.get(inNested).getA();
		}
		
		TypeAndName inNormalDQ = depQualiHolderContains(dependancyQauli, wanted);
		if(null != inNormalDQ) {
			typesHavingBeenInjected.add(inNormalDQ);
			return dependancyQauli.get(inNormalDQ).getA();
		}
		
		{
			TypeAndName nestedTO = null;
			TypeOnlyDef inst = null;
			if(null != nestedtypeOnlyDepQuali) {
				nestedTO = depQualiHolderContainsTO(nestedtypeOnlyDepQuali, wanted);
				inst = nestedtypeOnlyDepQuali.get(nestedTO);
			}
			
			if(null == nestedTO) {
				nestedTO = depQualiHolderContainsTO(typeOnlyDepQuali, wanted);
				inst = typeOnlyDepQuali.get(nestedTO);
			}
			
			if(null != nestedTO) {
				if(null != nestedDepsBeenUsed) {
					nestedDepsBeenUsed.add(nestedTO);
				}
				typesHavingBeenInjected.add(nestedTO);
				HashSet<TypeAndName> nestedDepsBeenUsedTO = new HashSet<TypeAndName>();
				
				Expression expr =  satifyDependancy(line, col, rootDependancy, new TypeAndName(inst.lhsType, nestedTO.name), ofType, dependancyQauli, typeOnlyDepQuali,
						 typesHavingBeenInjected, inst.nestedDeps, inst.nestedtypeOnlyDepQuali, nestedDepsBeenUsedTO);
				
				
				if(null != inst.nestedDeps || null != inst.nestedtypeOnlyDepQuali){
					boolean fail=false;
					HashSet<TypeAndName> allDecl = new HashSet<TypeAndName>();//satc: declared but unused + other one
					if(null != inst.nestedDeps) {
						allDecl.addAll(inst.nestedDeps.keySet());
					}
					if(null != inst.nestedtypeOnlyDepQuali) {
						allDecl.addAll(inst.nestedtypeOnlyDepQuali.keySet());
					}
					allDecl.removeAll(nestedDepsBeenUsedTO);
					if(!allDecl.isEmpty()) {
						for(TypeAndName what : allDecl) {
							Thruple<Expression, Integer, Integer> q = inst.nestedDeps==null?null:inst.nestedDeps.get(what);
							
							int lined;
							int cold;
							if(null == q) {
								TypeOnlyDef tod = inst.nestedtypeOnlyDepQuali.get(what);
								lined = tod.line;
								cold = tod.col;
							}else {
								lined = q.getB();
								cold = q.getC();
							}
							
							satc.raiseError(lined, cold, String.format("Provide specific declared dependency for type only dependency qualifier: %s is not used", what));
							fail=true;
						}
					}
					
					if(fail) {
						return null;
					}
				}
				
				return expr;
			}
		}
		
		//no direct match but can we inject into it?
		if(wanted.type instanceof NamedType) {
			NamedType wantedAsNamedType = (NamedType)wanted.type;
			//constructor
			ConstructorInfo cinfo = findInjectableConstructor(line, col, wantedAsNamedType);
			//fields
			Pair<Boolean, ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>>> fieldsToInjectf = getFieldsToInject(line, col, wantedAsNamedType, rootDependancy, ofType);
			ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>> fieldsToInject = fieldsToInjectf.getB();
			if(fieldsToInjectf.getA()) {
				return null;
			}
			
			//methods
			HashMap<String, ArrayList<TypeAndName>> methToDeps = getInjectableMethods(wantedAsNamedType);
			
			if(!cinfo.fail && cinfo.constructorToInject != null) {
				FuncType constructorToInjectType = cinfo.constructorToInjectType;
				
				Block toRet = new Block(line, col);
				toRet.isolated = true;
				toRet.setShouldBePresevedOnStack(true);
				
				if(provideToFuncName.containsKey(wanted.type) && !depChain.contains(wantedAsNamedType)) {//if depChain already has this then ifnore this call, errored later
					toRet.add(new DuffAssign(DotOperator.buildDotOperatorOneNonDirect(line, col, new RefThis(line, col), FuncInvoke.makeFuncInvoke(line, col, provideToFuncName.get(wanted.type), new RefName(sharedObjName)))));
					return toRet;
				}
				
				
				List<TypeAndName> construInputs = refiedConstructorArgs(constructorToInjectType, wantedAsNamedType);
				
				boolean useProviderNestedDep=false;;
				if(provideToNestedDep.containsKey(wanted.type)) {
					nestedDeps = provideToNestedDep.get(wanted.type);
					useProviderNestedDep=null != nestedDeps;
					nestedDepsBeenUsed = new HashSet<TypeAndName>();
				}
				
				boolean fail = performInjection(line, col, toRet, rootDependancy, wantedAsNamedType, construInputs, fieldsToInject, methToDeps, dependancyQauli, typeOnlyDepQuali, typesHavingBeenInjected, ofType, nestedDeps, nestedtypeOnlyDepQuali, nestedDepsBeenUsed, false, false, false, createLazy);
				
				if(useProviderNestedDep || nestedtypeOnlyDepQuali != null) {
				
					HashSet<TypeAndName> allDecl = new HashSet<TypeAndName>();
					if(null != nestedDeps) {
						allDecl.addAll(nestedDeps.keySet());
					}
					if(null != nestedtypeOnlyDepQuali) {
						allDecl.addAll(nestedtypeOnlyDepQuali.keySet());
					}
					allDecl.removeAll(nestedDepsBeenUsed);
					if(!allDecl.isEmpty()) {
						for(TypeAndName what : allDecl) {
							Thruple<Expression, Integer, Integer> q = nestedDeps==null?null:nestedDeps.get(what);
							
							int lined;
							int cold;
							if(null == q) {
								TypeOnlyDef tod = nestedtypeOnlyDepQuali.get(what);
								lined = tod.line;
								cold = tod.col;
							}else {
								lined = q.getB();
								cold = q.getC();
							}
							
							satc.raiseError(lined, cold, String.format("Provide specific declared dependency: %s is not used", what));
							fail=true;
						}
					}
					
				}
				
				return fail?null:toRet;
			}else if(fieldsToInject.isEmpty() && methToDeps.isEmpty()){
				satc.raiseError(line, col, String.format("Class to provide: %s has nested dependant class having injectable fields or methods without an injectable constructor: [%s]", rootDependancy, ofType));
				return null;
			}	
		}		
		
		satc.raiseError(line, col, String.format("Class to provide: %s is missing dependency of type: %s to provide for [%s]", rootDependancy, wanted, ofType));
		return null;	
	}

	private HashMap<String, ArrayList<TypeAndName>> getInjectableMethods(NamedType nt) {
		HashMap<String, ArrayList<TypeAndName>> methToDeps = new HashMap<String, ArrayList<TypeAndName>>();
		for(Pair<String, TypeAndLocation> meth : nt.getAllMethods(satc)) {
			Type tt = meth.getB().getType();
			if(tt instanceof FuncType) {
				FuncDef origin = ((FuncType)tt).origonatingFuncDef;
				if(origin.isInjected) {
					ArrayList<TypeAndName> tAndNs = makeTypeAndNameFromParams(((FuncType) tt).getInputs(), origin);
					//method to deps
					methToDeps.put(meth.getA(), tAndNs);
				}
			}
		}
		return methToDeps;
	}
	
	private ArrayList<TypeAndName> makeTypeAndNameFromParams(List<Type> deps, FuncDef origin){
		ArrayList<FuncParam> funcParams = origin.params.params;
		ArrayList<TypeAndName> tAndNs = new ArrayList<TypeAndName>();
		int n=0;
		for(Type dd : deps) {
			FuncParam fp = funcParams.get(n++);

			String pname = fp.argNameKnown?fp.name:null;
			if(fp.namedAnnotationName != null) {
				pname = fp.namedAnnotationName;
			}else {
				if(fp.annotations != null && fp.annotations.annotations != null && !fp.annotations.annotations.isEmpty()) {
					for(Annotation ant : fp.annotations.annotations) {
						if(ScopeAndTypeChecker.const_Named.equals(ant.getTaggedType())) {
							pname = ((VarString)ant.singleArg).str;
						}
					}
				}
			}
			
			tAndNs.add(new TypeAndName(dd, pname));
		}
		
		return tAndNs;
	}
	
	
	private List<TypeAndName> refiedConstructorArgs(FuncType constructorToInjectType, NamedType wantedAsNamedType){
		List<Type> construInputs = constructorToInjectType.getInputs();
		List<TypeAndName> construInputsret = makeTypeAndNameFromParams(construInputs, constructorToInjectType.origonatingFuncDef);
		
		boolean skipfirstConstArg = !isTypedActorAndOfAvailable(wantedAsNamedType) && TypeCheckUtils.isReifiedType(wantedAsNamedType);// TypeCheckUtils.isActor(this.er, wantedAsNamedType);
		
		if(skipfirstConstArg && construInputsret.size() > 0) {
			construInputsret = construInputsret.subList(1, construInputsret.size());
		}
		return construInputsret;
	}
	
}
