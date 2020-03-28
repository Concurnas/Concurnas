package com.concurnas.compiler.bytecode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.concurnas.compiler.ast.AccessModifier;
import com.concurnas.compiler.ast.AndExpression;
import com.concurnas.compiler.ast.Annotation;
import com.concurnas.compiler.ast.Annotations;
import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.ast.AssignStyleEnum;
import com.concurnas.compiler.ast.AsyncRefRef;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.CastExpression;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.ClassDefArg;
import com.concurnas.compiler.ast.ClassDefArgs;
import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.ast.ConstructorDef;
import com.concurnas.compiler.ast.DotOperator;
import com.concurnas.compiler.ast.DuffAssign;
import com.concurnas.compiler.ast.EqReExpression;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.FuncInvoke;
import com.concurnas.compiler.ast.FuncInvokeArgs;
import com.concurnas.compiler.ast.FuncParam;
import com.concurnas.compiler.ast.FuncParams;
import com.concurnas.compiler.ast.FuncRef;
import com.concurnas.compiler.ast.FuncRefArgs;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.GenericType;
import com.concurnas.compiler.ast.GrandLogicalOperatorEnum;
import com.concurnas.compiler.ast.IfStatement;
import com.concurnas.compiler.ast.InitBlock;
import com.concurnas.compiler.ast.Is;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.NamedConstructorRef;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.New;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.NotExpression;
import com.concurnas.compiler.ast.NotNullAssertion;
import com.concurnas.compiler.ast.NullStatus;
import com.concurnas.compiler.ast.OrExpression;
import com.concurnas.compiler.ast.PrimativeType;
import com.concurnas.compiler.ast.PrimativeTypeEnum;
import com.concurnas.compiler.ast.RedirectableExpression;
import com.concurnas.compiler.ast.RefBoolean;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.RefNamedType;
import com.concurnas.compiler.ast.RefQualifiedGenericNamedType;
import com.concurnas.compiler.ast.RefSuper;
import com.concurnas.compiler.ast.RefThis;
import com.concurnas.compiler.ast.ReturnStatement;
import com.concurnas.compiler.ast.SuperConstructorInvoke;
import com.concurnas.compiler.ast.ThisConstructorInvoke;
import com.concurnas.compiler.ast.TryCatch;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.VarChar;
import com.concurnas.compiler.ast.VarDouble;
import com.concurnas.compiler.ast.VarFloat;
import com.concurnas.compiler.ast.VarInt;
import com.concurnas.compiler.ast.VarLong;
import com.concurnas.compiler.ast.VarNull;
import com.concurnas.compiler.ast.VarString;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.constants.UncallableMethods;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.Sevenple;
import com.concurnas.compiler.utils.Sixple;
import com.concurnas.compiler.visitors.ErrorRaiseable;
import com.concurnas.compiler.visitors.PrintSourceVisitor;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.compiler.visitors.Utils;
import com.concurnas.compiler.visitors.datastructs.TheScopeFrame;
import com.concurnas.compiler.visitors.util.ConstArg;
import com.concurnas.runtime.Pair;

public class FunctionGenneratorUtils {

	public static String makeCamelName(String name)
	{
		return (""+name.charAt(0)).toUpperCase() + name.substring(1, name.length());
	}
	
	public static String getGetterName(String name, ErrorRaiseable sup, Type retrunType, int line, int col)
	{
		if(null != retrunType && TypeCheckUtils.isBoolean(sup, retrunType, line, col))		{//for booleans: getA :- isA
			return String.format("is%s",makeCamelName(name));
		}
		else		{
			return String.format("get%s", makeCamelName(name) );
		}
	}
	
	public static String getSetterName(String name)
	{
		return String.format("set%s", makeCamelName(name) );
	}
	
	public static void addGetter(ErrorRaiseable sup, ScopeAndTypeChecker errors, ClassDef classDef, String name, Type retrunType, int line, int col, Annotations annot, boolean viaSuper)
	{
		String funcName = getGetterName( name, sup,  retrunType, line, col);
		Block body = new Block(classDef.getLine(), classDef.getColumn());
		ArrayList<Expression> postDot = new ArrayList<Expression>();
		ArrayList<Boolean> isDirect = new ArrayList<Boolean>();
		isDirect.add(true);
		postDot.add( eqRefName(classDef.getLine(), classDef.getColumn(),name) );
		ArrayList<Boolean> retself = new ArrayList<Boolean>();
		retself.add(false);
		ArrayList<Boolean> safecall = new ArrayList<Boolean>();
		safecall.add(false);
		ReturnStatement ret = new ReturnStatement(classDef.getLine(), classDef.getColumn(), new DotOperator(classDef.getLine(), classDef.getColumn(),viaSuper? new RefSuper(classDef.getLine(), classDef.getColumn()):new RefThis(classDef.getLine(), classDef.getColumn()), postDot, isDirect, retself, safecall) );
		body.add(new LineHolder(classDef.getLine(), classDef.getColumn(), ret));
		
		FuncDef getter = new FuncDef(classDef.getLine(), classDef.getColumn(), null, AccessModifier.PUBLIC, funcName, new FuncParams(classDef.getLine(), classDef.getColumn()), body, retrunType, false, false, false, new ArrayList<Pair<String, NamedType>>());
		getter.setAnnotations(annot);
		getter.isAutoGennerated = true;
		//removePrevoiusFunctionIfTypeDefDoesntMatch(classDef, getter.getFuncType(), funcName);
		addFunctionToClassDef(errors, classDef, funcName, getter,line, col, false, false);
	}
	
	public static boolean hasGetterOrSetter(ClassDef classDef, String name, ErrorRaiseable sup, Type retrunType, boolean isGetter, int line, int col) {
		String[] searchfo;
		if(isGetter) {
			String getterName = getGetterName( name, sup,  retrunType, line, col);
			if(null == retrunType) {//check boolean version too...
				searchfo = new String[2];
				searchfo[0] = getterName;
				searchfo[1] = "is" + getterName.substring(2, getterName.length());
			}else {
				searchfo = new String[] {getterName};
			}
		}else {
			searchfo = new String[] {getSetterName(name)};
		}
		
		for(LineHolder lh : classDef.classBlock.lines){
			if(lh.l instanceof FuncDef){
				FuncDef asFuncDef = (FuncDef)lh.l;
				String mname = asFuncDef.getMethodName();
				for(String tryme : searchfo) {
					if(tryme.equals(mname)) {
						
						if(!asFuncDef.IsAutoGennerated()) {
							return true;//user defined so ignore me
						}else {
							if(isGetter) {//check return type
								if(retrunType.equals(asFuncDef.retType)) {
									return true;
								}
							}else {//check arg0 only
								if(retrunType.equals(asFuncDef.params.params.get(0).type)) {
									return true;
								}
							}
						}
					}
				}
			}
		}
		
		return false;
	}
	
	private static void removePrevoiusFunctionIfTypeDefDoesntMatch(ClassDef classDef, FuncType setType, String name, boolean isConstructor){
		if(isConstructor){
			FuncType toRemove = null;
			
			for(LineHolder lh : classDef.classBlock.lines){
				if(lh.l instanceof ConstructorDef){
					FuncDef asFuncDef = (FuncDef)lh.l;
					if(asFuncDef.isAutoGennerated  && !asFuncDef.getFuncType().equals(setType)){
						classDef.classBlock.lines.remove(lh);
						toRemove=asFuncDef.getFuncType();
						break;
					}
				}
			}
			
			if(null != toRemove){
				classDef.getScopeFrame().removeConstructorDef(toRemove);
			}
			
		}else{
			if(classDef.hasFuncDef(name, true, false)){
				//remove existing definition of variable
				FuncType toRemove=null;
				for(LineHolder lh : classDef.classBlock.lines){
					if(lh.l instanceof FuncDef){
						FuncDef asFuncDef = (FuncDef)lh.l;
						if(asFuncDef.isAutoGennerated && asFuncDef.getMethodName().equals(name) && !asFuncDef.getFuncType().equals(setType)){
							classDef.classBlock.lines.remove(lh);
							toRemove=asFuncDef.getFuncType();
							break;
						}
					}
				}
				
				if(toRemove != null){
					classDef.getScopeFrame().removeFuncDef(name, toRemove);
				}
			}
		}
	}
	
	
	public static void addFunctionToClassDef(ScopeAndTypeChecker errors, ClassDef classDef, String funcName, FuncDef toAdd, int line, int col, boolean isConstructor, boolean ignorePreExistingCheck)
	{
		HashSet<TypeAndLocation> check = classDef.getFuncDef(funcName, false, false, false);
		
		FuncType toAddFT = toAdd.getFuncType();
		for(TypeAndLocation tochecktal : check)
		{
			FuncType tocheck = (FuncType)tochecktal.getType();
			if(toAddFT.equals(tocheck))
			{
				return;
			}
		}

		if(!ignorePreExistingCheck){
			removePrevoiusFunctionIfTypeDefDoesntMatch(classDef, toAddFT, funcName, isConstructor);
		}
		
		//remove previous definition
		
		//classDef.setFuncDef(funcName, getter);
		classDef.classBlock.prepend(new LineHolder(classDef.getLine(), classDef.getColumn(), toAdd));
	}
	
	public static void addSetter(ErrorRaiseable sup, ScopeAndTypeChecker errors, ClassDef classDef, String name, Type setType, int line, int col, Annotations annot, boolean viaSuper)
	{
		String funcName = getSetterName(name);
		
		int refCnt = TypeCheckUtils.getRefLevels(setType);
		
		Block setterBlock = new Block(classDef.getLine(), classDef.getColumn());
		//this.x = x;
		
		ArrayList<Expression> postDot = new ArrayList<Expression>();
		postDot.add( eqRefName(classDef.getLine(), classDef.getColumn(), name) );
		
		ArrayList<Boolean> isDirect = new ArrayList<Boolean>();
		isDirect.add(true);
		ArrayList<Boolean> retself = new ArrayList<Boolean>();
		retself.add(false);
		ArrayList<Boolean> safecall = new ArrayList<Boolean>();
		safecall.add(false);
		AssignExisting ae = new AssignExisting(classDef.getLine(), classDef.getColumn(), new DotOperator(classDef.getLine(), classDef.getColumn(), viaSuper? new RefSuper(classDef.getLine(), classDef.getColumn()): new RefThis(classDef.getLine(), classDef.getColumn()), postDot, isDirect, retself, safecall),  
				AssignStyleEnum.EQUALS, 
				eqRefName(classDef.getLine(), classDef.getColumn(), name) );
		
		ae.refCnt = refCnt;
		
		setterBlock.add(new LineHolder(classDef.getLine(), classDef.getColumn(), ae));
		
		FuncDef setter = new FuncDef(classDef.getLine(), classDef.getColumn(), null, AccessModifier.PUBLIC, funcName, new FuncParams(classDef.getLine(), classDef.getColumn(), new FuncParam(classDef.getLine(), classDef.getColumn(), name, setType, true)), setterBlock, new PrimativeType(classDef.getLine(), classDef.getColumn(), PrimativeTypeEnum.VOID), false, false, false, new ArrayList<Pair<String, NamedType>>());
		setter.setAnnotations(annot);
		setter.isAutoGennerated = true;
		//removePrevoiusFunctionIfTypeDefDoesntMatch(classDef, setter.getFuncType(), funcName);
		addFunctionToClassDef(errors, classDef, funcName, setter, line, col, false, false);
	}
	
	public static void addwithDefaultsMethod(ErrorRaiseable sup, ScopeAndTypeChecker satc, FuncDef funcDef, TheScopeFrame myScopeFrame, Block currentBlock) {
		int line = funcDef.getLine();
		int col = funcDef.getColumn();
		String fname = funcDef.funcName;
		boolean iscon = funcDef.retType==null;//yeah
		
		FuncParams fps = new FuncParams(line, col);
		
		Block setterBlock = new Block(line, col);
		//com.concurnas.runtime.DefaultParamUncreatable
		FuncInvokeArgs fia = new FuncInvokeArgs(line, col);
		for(FuncParam fp : funcDef.params.params){
			String pname = fp.name;
			String pnameDN = pname + "$defaultNull";
			fps.add(new FuncParam(line, col, pname, fp.getTaggedType(), false));
			
			
			if(null != fp.defaultValue){
				/*
				 if(null == a$defaultNull){
				 	a = defaultExpr
				 }
				 */
				/*EqReExpression nullTest = new EqReExpression(line, col, new VarNull(line, col), GrandLogicalOperatorEnum.EQ, eqRefName(line, col, pnameDN));
				Block ifblock = new Block(line, col);
				AssignExisting ae = new AssignExisting(line, col, eqRefName(line, col, pname), AssignStyleEnum.EQUALS, (Expression)fp.defaultValue.copy());
				ifblock.add(new LineHolder(ae));
				//ifblock.setShouldBePresevedOnStack(false);
				IfStatement ifxex = new IfStatement(line, col, nullTest, ifblock, null);
				ifxex.setShouldBePresevedOnStack(false);
				setterBlock.add(new LineHolder(ifxex));*/
				
				EqReExpression nullTest = new EqReExpression(line, col, new VarNull(line, col), GrandLogicalOperatorEnum.EQ, eqRefName(line, col, pnameDN));
				
				Expression expr = (Expression)fp.defaultValue.copy();
				/*if(expr instanceof VarNull && fp.type instanceof GenericType){
					expr = new CastExpression(line, col, fp.type, expr);
				}*/
				
				//if type is invalid just resolve to input so as to gennerate valid code
				
				//IfExpr ifExpr = new IfExpr(line, col, nullTest, !fp.defaultOk?eqRefName(line, col, pname):expr, eqRefName(line, col, pname));
				
				IfStatement ifExpr = IfStatement.makeFromTwoExprs(line, col, !fp.defaultOk?eqRefName(line, col, pname):expr, eqRefName(line, col, pname), nullTest);
				
				Utils.inferAnonLambda(satc, (Node)ifExpr, fp.type);
				
				fia.add(ifExpr);
				
				fps.add(new FuncParam(line, col, pnameDN, ScopeAndTypeChecker.const_defaultParamUncre, true));
			}
			else{
				fia.add(eqRefName(line, col, pname));
			}
		}
		
		ArrayList<Type> genTypes = new ArrayList<Type>();
		ArrayList<Pair<String, NamedType>> genTypesStr = new ArrayList<Pair<String, NamedType>>();
		if(null != funcDef.methodGenricList){
			for(Pair<String, NamedType> ge : funcDef.methodGenricList){
				
				GenericType gt = new GenericType(ge.getA(), 0);
				NamedType nt = ge.getB();
				if(null != nt) {
					gt.upperBound = nt;
				}
				
				genTypes.add(gt);
				genTypesStr.add(ge);
			}
		}
		
		FuncDef wdefault;
		if(iscon){
			ThisConstructorInvoke theCall = new ThisConstructorInvoke(line, col, fia);
			setterBlock.add(new LineHolder(new DuffAssign(theCall)));
			wdefault = new ConstructorDef(line, col, funcDef.accessModifier, fps, setterBlock);
		}
		else{
			Expression theCall = new FuncInvoke(line, col, fname, fia, genTypes.isEmpty()?null:genTypes);
			
			if(funcDef.extFunOn!=null) {
				theCall = DotOperator.buildDotOperatorOne(line, col, new RefThis(line, col), theCall);
			}
			
			ReturnStatement ret = new ReturnStatement(line, col, theCall);
			setterBlock.add(new LineHolder(ret));
			wdefault = new FuncDef(line, col, null, funcDef.accessModifier, fname, fps, setterBlock, iscon?null:(Type)funcDef.retType.copy(), false, false, false, genTypesStr);
			
			if(funcDef.extFunOn!=null) {
				wdefault.extFunOn = (Type)funcDef.extFunOn.copy();
			}
		}
		
		wdefault.isAutoGennerated = true;
		//setter.alreadyNested = funcDef.alreadyNested;
		
		FuncType sig = wdefault.getFuncType();
		
		/*ArrayList<Type> newInputs = new ArrayList<Type>(sig.inputs.size());
		for(Type tt : sig.inputs) {
			newInputs.add(TypeCheckUtils.convertTypeToFuncType(tt));
		}
		sig.inputs = newInputs;*/
		
		/*String renamedTo =*/ myScopeFrame.removeFuncDef(fname, sig);
		
		/*if(null != renamedTo) {
			fname = renamedTo;
		}*/
		
		currentBlock.overwriteLineHolder(new Pair<String, FuncType>(fname, sig), new LineHolder(line, col, wdefault));
		
		wdefault.accept(satc);
	}
	
	
	
	
	public static AssignNew addClassVariable(ErrorRaiseable sup, ScopeAndTypeChecker errors, ClassDef classDef, String name, String prefix, Type setType, int line, int col, boolean isFinal, AccessModifier am, Annotations annotationsForField, boolean isTransient, boolean isShared, boolean isOverride, boolean isLazy)
	{
		return addClassVariable( sup,  errors,  classDef,  name,  prefix,  setType,  line,  col,  isFinal,  am,  annotationsForField, null, isTransient, isShared, isOverride, isLazy);
	}
	
	public static AssignNew addClassVariable(ErrorRaiseable sup, ScopeAndTypeChecker errors, ClassDef classDef, String name, String prefix, Type setType, int line, int col, boolean isFinal, AccessModifier am, Annotations annotationsForField, Expression expr, boolean isTransient, boolean isShared, boolean isOverride, boolean isLazy)
	{
		if(classDef.hasVariable(null, name, false, /* ignore already defined variable = */ true))
		{
			if(null != errors){
				errors.raiseError(line, col, String.format("Class variable: %s has already been defined", name ));
			}
		}
		else
		{
			if(classDef.hasVariable(null, name, false, false)){
				//remove existing definition of variable
				boolean found=false;
				for(LineHolder lh : classDef.classBlock.lines){
					if(lh.l instanceof AssignNew){
						AssignNew asAN = (AssignNew)lh.l;
						if(asAN.isautogenerated && asAN.name.equals(name) && !asAN.type.equals(setType)){
							classDef.classBlock.lines.remove(lh);
							found=true;
							break;
						}
					}
				}
				
				if(found){
					classDef.getScopeFrame().removeVariable(name);
				}
			}
			
			if(!classDef.hasVariable(null, name, false, false))
			{//skip if not autogennerated; if we ignore this check we gennerate the variables twice!
				AssignNew an = new AssignNew(am, classDef.getLine(), classDef.getColumn(), isFinal, false, name, prefix, setType, AssignStyleEnum.EQUALS, expr);
				an.annotations=annotationsForField;
				an.isautogenerated = true;
				an.isShared = isShared;
				an.isLazy = isLazy;
				if(isTransient){
					an.isTransient = true;
				}
				an.isOverride = isOverride;
				classDef.classBlock.reallyPrepend(new LineHolder(classDef.getLine(), classDef.getColumn(), an));
				return an;
			}
		}
		return null;
	}
	
	public static FuncType addConstructor(ErrorRaiseable sup, ScopeAndTypeChecker errors, ClassDef classDef, ArrayList<ConstArg> listOfNameAndTypes, ArrayList<Expression> superConstrThings, HashSet<String> superClassReferencedVariableNames, int line, int col, boolean isActor, boolean isEnum, Annotations annotations, boolean ignorePreExistingCheck, boolean isInjected)
	{
		FuncParams params = new FuncParams(line, col);
		Block constructorBody = new Block(line, col);
		if(!superConstrThings.isEmpty())
		{
			FuncInvokeArgs superArgs = new FuncInvokeArgs(line, col);
			
			for(Expression e: superConstrThings)
			{
				superArgs.add(e);
			}
			
			SuperConstructorInvoke superCon = new SuperConstructorInvoke(line, col, superArgs);
			constructorBody.add(new LineHolder(line, col, new DuffAssign(line, col, superCon)));
		}

		ArrayList<Type> argList = new ArrayList<Type>();
				
		for(ConstArg entry: listOfNameAndTypes)
		{
			String name = entry.name;
			ArrayList<Expression> postDot = new ArrayList<Expression>(); 
			postDot.add( eqRefName(line, col, name) );
			ArrayList<Boolean> isDirect = new ArrayList<Boolean>();
			isDirect.add(true);
			Type type = entry.argType;
			
			if(!superClassReferencedVariableNames.contains(name))
			{//passed to superconstructor so dont set here
				//this.x = x etc...
				ArrayList<Boolean> retself = new ArrayList<Boolean>();
				retself.add(false);
				ArrayList<Boolean> safecall = new ArrayList<Boolean>();
				safecall.add(false);
				AssignExisting ae = new AssignExisting(line, col, new DotOperator(line, col, new RefThis(line, col), postDot,isDirect, retself, safecall),  AssignStyleEnum.EQUALS, eqRefName(line, col, name) );
				ae.refCnt = TypeCheckUtils.getRefLevels(type);
				
				constructorBody.add(new LineHolder(line, col, ae));
			}
			
			
			Type forArgList = (Type)type.copy();
			
			if(entry.isLazy) {
				forArgList = Utils.convertToLazyType(forArgList);
			}
			
			if(entry.isVararg){
				forArgList.setArrayLevels(forArgList.getArrayLevels()+1);
			}
			argList.add(forArgList);
			
			
			boolean isfinal = entry.isFinal;
			FuncParam fp = new FuncParam(line, col, name, type, isfinal);
			fp.defaultValue = entry.defaultValue;
			//fp.accept(errors);
			fp.annotations = entry.annotationsForConstructor;
			fp.isVararg=entry.isVararg;
			fp.isLazy = entry.isLazy;
			fp.isShared = entry.isShared;
			params.add(fp);
		}
		
		if(isActor){
			FuncParam fp = new FuncParam(line, col, ScopeAndTypeChecker.TypesForActor, ScopeAndTypeChecker.const_classArray_nt_array, true);
			//fp.accept(errors);
			params.add(0, fp);
			argList.add(0, ScopeAndTypeChecker.const_classArray_nt_array);
		}

		
		
		if(classDef.isActor){
			constructorBody.add(new LineHolder(line, col, new DuffAssign(line, col, DotOperator.buildDotOperatorOne(line, col, new RefSuper(line, col), new FuncInvoke(line, col,"onInit")))));
		}
		
		if(classDef.classBlock != null && !classDef.classBlock.isEmpty()){
			
			for(LineHolder lh : classDef.classBlock.getLinesExcludeSynthetics()){
				if(lh.l instanceof InitBlock){
					InitBlock ib = (InitBlock)lh.l;
					constructorBody.add(new LineHolder(ib.getLine(), ib.getColumn(), ib.block));
				}
			}
		}
		
		ConstructorDef constDef = new ConstructorDef(line, col, isEnum?AccessModifier.PRIVATE:AccessModifier.PUBLIC, params, constructorBody);//default constuctor always public
		//default constructor has same access modifier as class itself, e.g. private if class is declared private
		constDef.isAutoGennerated = true;
		constDef.setAnnotations(annotations);
		constDef.isInjected = isInjected;
		//figure this out...
		
		FuncType f = constructorExistsAlready(classDef, argList);
		if(null != f){ return f;}//exists already
		
		addFunctionToClassDef(errors, classDef, classDef.className, constDef, line, col, true, ignorePreExistingCheck);
		return constDef.getFuncType();
	}
	
	private static FuncType constructorExistsAlready(ClassDef classDef, ArrayList<Type> argList){
		return constructorExistsAlready(classDef, new FuncType(argList, null));
	}

	private static FuncType constructorExistsAlready(ClassDef classDef, FuncType sig) {
		for(FuncType f: classDef.getConstructor(sig.getInputs().size(), null)){
			if( f.equals(sig)){
				return f;//if the thing has already been autogennerated, then dont autogennerate it again
			}
		}
		return null;
	}
	
	private static NamedType object_const = new NamedType(new ClassDefJava(java.lang.Object.class));
	private static PrimativeType const_boolean = new PrimativeType(PrimativeTypeEnum.BOOLEAN);
	private static PrimativeType const_int = new PrimativeType(PrimativeTypeEnum.INT);
	
	private static boolean isEqualsDefAbstract(ClassDef classDef){
		ArrayList<Type> inputs = new ArrayList<Type>(1);
		inputs.add(object_const);
		FuncType sig = new FuncType(inputs, const_boolean);
		for(TypeAndLocation tal: classDef.getFuncDef("equals", true, false)){
			Type t = tal.getType();
			if(t.equals(sig) && ((FuncType)t).isAbstarct()){
				return true;
			}
		}
		return false;
	}
	
	private static Expression makeAsyncRefRefForName(int line, int col, String name, int levels){
		return levels>0?new AsyncRefRef(line, col, eqRefName(line, col, name), levels):eqRefName(line, col, name);
	}
	
	private static Expression makeAsyncRefRefForFuncInvoke(int line, int col, String name, int levels){
		return levels>0?new AsyncRefRef(line, col, new FuncInvoke(line, col, name), levels):new FuncInvoke(line, col, name);
	}
	
	private static NamedType upperBoundGenerics(NamedType input){
		input = input.copyTypeSpecific(); 
		List<Type> genz = input.getGenTypes();
		ArrayList<Type> genzNew = new ArrayList<Type>(genz.size());
		for(Type g: genz){
			if(g instanceof GenericType){
				g=((GenericType)g).upperBound;
			}
			else if(g instanceof NamedType){
				g = upperBoundGenerics((NamedType)g);
			}
			genzNew.add(g);
		}
		input.setGenTypes(genzNew);
		return input;
	}
	
	public static RefName eqRefName(int line, int col, String name) {
		RefName ret = new RefName(line, col, name);
		ret.supressUnassign = true;
		return ret;
	}
	
	public static FuncDef addDefaultEquals(int line, int col, ClassDef classDef, ScopeAndTypeChecker satc, boolean isLambda, boolean isActor){
		//equals, hashcode, copier, isimmutable
		ClassDef sup = classDef.getSuperclass();
		NamedType qualifiedSuper = upperBoundGenerics(classDef.getFullyqualifiedSuperClassRef(line, col));
		
		
		
		if(qualifiedSuper != null
				&& !UncallableMethods.UNAVAILABLE_CLASSES.contains(sup)//no extending of Thread etc
				&& !isEqualsDefAbstract(sup)//if parent is abstract, then we cannot auto do this, user must define
				){ 
			/*
			 
			 if(com.concurnas.lang.Equalifier.visitedDefaultEQMethodsInCurrentStack.contains(this)){
			 		return false;
			 	}
			 
			 				 	
			 	com.concurnas.lang.Equalifier.visitedDefaultEQMethodsInCurrentStack.put(this, this);
			 	
			 	if(other not null && other is ScopeAndTypeChecker<?, ?>){
		 			if( other.getClass().equals(this.getClass()) ){
					theOther ScopeAndTypeChecker = other as ScopeAndTypeChecker<DDD, YYY>;
					if(theOther.isFirstStmtInBlock != this.isFirstStmtInBlock){
					com.concurnas.lang.Equalifier.visitedDefaultEQMethodsInCurrentStack.remove(this); 
					return false; }
					
					if(theOther.localLambdaCount != this.localLambdaCount){
					com.concurnas.lang.Equalifier.visitedDefaultEQMethodsInCurrentStack.remove(this); 
					return false; }
					if( not Equalifier.equals(theOther.arr1 , this.arr1) ){
					com.concurnas.lang.Equalifier.visitedDefaultEQMethodsInCurrentStack.remove(this); 
					return false; }
					
					if(not (theOther.fieldola == null && this.fieldola == null)){//if both null then ok
						if( theOther.fieldola == null or this.fieldola == null or not theOther.fieldola.equals(this.fieldola) ){
							com.concurnas.lang.Equalifier.visitedDefaultEQMethodsInCurrentStack.remove(this);
							return false //if one null then fail, or if not eq
						}
				}
				com.concurnas.lang.Equalifier.visitedDefaultEQMethodsInCurrentStack.remove(this);
				return super.equals(theOther as supertype<DDD, YYY>);
			 }
			 
			return false;
			 */
			
			//note when comparing ref types, ensure they are locked in place when being passed around the various equals fuctions
			
			List<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>> allVars = getAllVars(satc, classDef, isActor);
			allVars = allVars.stream().filter(a -> !a.getC()).collect(Collectors.toList());//ignore items which have been set already
			
			
			//function begins - which needs to be moved to bytecode gen stage lols
			
			FuncParams inputs = new FuncParams(line, col);
			inputs.add(new FuncParam(line, col, "other", object_const, true));
			
			Block mainBlock = new Block(line, col);
			Block ifNullBlock = new Block(line, col);
			String className = classDef.getClassName();
			NamedType castToGen = new NamedType(line, col, className);
			
			ArrayList<GenericType> clsGenericList = classDef.getClassGenricList();
			
			if(null != clsGenericList && !clsGenericList.isEmpty()){
				ArrayList<Type> genTypes = new ArrayList<Type>();
				ArrayList<Type> genTypesO = new ArrayList<Type>();
				
				for(Type e : clsGenericList){
					genTypes.add(new GenericType(line, col, "?", 0));
					
					if(e instanceof GenericType){
						genTypesO.add(((GenericType)e).upperBound);// theOther as ArrayList<X> -> theOther as ArrayList<Object> //if x < Object (upper bound is object) 
					}
					else{
						genTypesO.add(e);
					}
				}
				castToGen.setGenTypes(genTypesO);
			}
			
			if(classDef.hasParentNestor()){
				ClassDef parNest = classDef.getParentNestor();
				while(parNest != null){
					ArrayList<Type> parGenQualifications = new ArrayList<Type>();
					for(GenericType gen : parNest.classGenricList){
						parGenQualifications.add(gen.getUpperBoundAsNamedType());
					}
					castToGen.nestorSegments.add(0, new Pair<String, ArrayList<Type>>(parNest.className, parGenQualifications));
					parNest = parNest.getParentNestor();
				}
			}
			
			//ifNullBlock.add(getRemoveFromDefEQ(line, col));
			ifNullBlock.add(new LineHolder(line, col, new ReturnStatement(line, col, new RefBoolean(line, col, false))));
			
			EqReExpression ifNull = new EqReExpression(line, col, eqRefName(line, col, "other"), GrandLogicalOperatorEnum.EQ, new VarNull(line, col));
			IfStatement ifNullStmt = new IfStatement(line, col, ifNull , ifNullBlock, null);//new ArrayList<ElifUnit>()
			
			//Expression instExpTest = new InstanceOf(line, col, eqRefName(line, col, "other"), castTo, false);
			//other.getClass().equals(this.getClass())
			DotOperator otherGetClass = DotOperator.buildDotOperatorOne(line, col, eqRefName(line, col, "other"), new FuncInvoke(line, col, "getClass"));
			DotOperator thisGetClass = DotOperator.buildDotOperatorOne(line, col, new RefThis(line, col), new FuncInvoke(line, col, "getClass"));
			
			DotOperator callEQ = DotOperator.buildDotOperatorOne(line, col, thisGetClass, new FuncInvoke(line, col, "equals", otherGetClass));
			
			IfStatement ifInst = new IfStatement(line, col, callEQ , mainBlock, null);//new ArrayList<ElifUnit>()
			
			//mainBlock
			//cast
			AssignNew theOther = new AssignNew(null, line, col, true, false, "theOther", castToGen, AssignStyleEnum.EQUALS, new CastExpression(line, col,  (Type) castToGen.copy(), eqRefName(line, col, "other"))); 
			theOther.setInsistNew(true);
			mainBlock.add(new LineHolder(line, col, theOther));
			
			for(Sixple<String, Type, Boolean, AccessModifier, Boolean, String> var : allVars){
				//if(theOther.isFirstStmtInBlock != this.isFirstStmtInBlock){ return false; }
				String name = var.getA();
				Type type = var.getB();

				Block ifFail = new Block(line, col);
				Expression iftest;
				if(type.hasArrayLevels()){
					//array
					//com.concurnas.lang.Equalifier.equals(this.iList1AsObj , theOther.iList1Prim)

					DotOperator thisField = DotOperator.buildDotOperatorOne(line, col, new RefThis(line, col), isActor?new FuncInvoke(line, col, name):eqRefName(line, col, name));
					DotOperator otherField = DotOperator.buildDotOperatorOne(line, col, eqRefName(line, col, "theOther"), isActor?new FuncInvoke(line, col, name):eqRefName(line, col, name));
					
					FuncInvokeArgs EQTestfuncArgs = new FuncInvokeArgs(line, col);
					EQTestfuncArgs.add(thisField);
					EQTestfuncArgs.add(otherField);
					
					FuncInvoke eqTestFuncCall = new FuncInvoke(line, col, "equals", EQTestfuncArgs);
					
					ArrayList<Expression> doExpr = new ArrayList<Expression>();
					doExpr.add(new RefName(line, col, "com"));
					doExpr.add(new RefName(line, col, "concurnas"));
					doExpr.add(new RefName(line, col, "lang"));
					doExpr.add(new RefName(line, col, "Equalifier"));
					doExpr.add(eqTestFuncCall);
					
					DotOperator fullNameFuncInvoke = new DotOperator(line, col, doExpr);
					
					iftest = new NotExpression(line, col, fullNameFuncInvoke);
					
					IfStatement indivIf = new IfStatement(line, col, iftest, ifFail, null);
					//ifFail.add(getRemoveFromDefEQ(line, col));
					ifFail.add(new LineHolder(line, col, new ReturnStatement(line, col, new RefBoolean(line, col, false))));
					mainBlock.add(new LineHolder(line, col, indivIf));
					
				}
				else if(type instanceof PrimativeType  && ((PrimativeType)type).type != PrimativeTypeEnum.LAMBDA ){
					//prim type...
					
					
					iftest = new EqReExpression(line, col, 
							DotOperator.buildDotOperatorOne(line, col, eqRefName(line, col, "theOther"), isActor?new FuncInvoke(line, col, name):eqRefName(line, col, name)), 
							GrandLogicalOperatorEnum.NE, 
							DotOperator.buildDotOperatorOne(line, col, new RefThis(line, col), isActor?new FuncInvoke(line, col, name):eqRefName(line, col, name)));

					IfStatement indivIf = new IfStatement(line, col, iftest, ifFail, null);
					//ifFail.add(getRemoveFromDefEQ(line, col));
					ifFail.add(new LineHolder(line, col, new ReturnStatement(line, col, new RefBoolean(line, col, false))));
					mainBlock.add(new LineHolder(line, col, indivIf));
					
				}
				else{
					//oh it's an object or a func type, either way call equals
					/*
					if(not (theOther.fieldola &== null and this.fieldola &== null)){//if both null then ok
						if( theOther.fieldola &== null or this.fieldola &== null or not theOther.fieldola.equals(this.fieldola) ){
							return false //if one null then fail, or if not eq
						}
					}
					*/
					
					//little ref name gennerator such taht in all ops, name::: us used instead (with approperiate ::: levels
					
					int refLevels = TypeCheckUtils.getRefLevels(type);//TODO: would be more optimnal to have the ref levels tagged at the top leve of the type
										  
					EqReExpression thisFieldIsNull = new EqReExpression(line, col, 
							DotOperator.buildDotOperatorOne(line, col, new RefThis(line, col), isActor?makeAsyncRefRefForFuncInvoke(line, col, name, refLevels):makeAsyncRefRefForName(line, col, name, refLevels)), 
							GrandLogicalOperatorEnum.REFEQ, 
							new VarNull(line, col) );
					
					
					EqReExpression otherFieldIsNull = new EqReExpression(line, col, 
							DotOperator.buildDotOperatorOne(line, col, eqRefName(line, col, "theOther"), isActor?makeAsyncRefRefForFuncInvoke(line, col, name, refLevels):makeAsyncRefRefForName(line, col, name, refLevels)), 
							GrandLogicalOperatorEnum.REFEQ, 
							new VarNull(line, col) );
					
					//REFEQ
					NotExpression ifNotBothNullTest =  new NotExpression(line, col, AndExpression.AndExpressionBuilder(line, col, thisFieldIsNull, new RedirectableExpression(otherFieldIsNull)));
					Block onifNotBothNullFail = new Block(line, col);
					IfStatement ifNotBothNull = new IfStatement(line, col, ifNotBothNullTest, onifNotBothNullFail, null);
					mainBlock.add(new LineHolder(line, col, ifNotBothNull));
					
					
					//now we check that one or other is null or they are not equals -> then false
					EqReExpression thisFieldIsNull2 = new EqReExpression(line, col, 
							DotOperator.buildDotOperatorOne(line, col, new RefThis(line, col), isActor?makeAsyncRefRefForFuncInvoke(line, col, name, refLevels):makeAsyncRefRefForName(line, col, name, refLevels)), 
							GrandLogicalOperatorEnum.REFEQ, 
							new VarNull(line, col) );
					
					EqReExpression otherFieldIsNull2 = new EqReExpression(line, col, 
							DotOperator.buildDotOperatorOne(line, col, eqRefName(line, col, "theOther"), isActor?makeAsyncRefRefForFuncInvoke(line, col, name, refLevels):makeAsyncRefRefForName(line, col, name, refLevels)), 
							GrandLogicalOperatorEnum.REFEQ, 
							new VarNull(line, col) );
					
					//not theOther.fieldola.equals(this.fieldola)
					
					FuncInvokeArgs args = new FuncInvokeArgs(line, col);
					
					
					boolean typeIsPotentiallyNull = typeIsPotentiallyNull(type, classDef);
					
					{
						Expression testVsArg = isActor?makeAsyncRefRefForFuncInvoke(line, col, name, refLevels):makeAsyncRefRefForName(line, col, name, refLevels);
						
						Expression expr = DotOperator.buildDotOperatorOne(line, col, new RefThis(line, col), testVsArg);
						
						if(typeIsPotentiallyNull) {
							expr = new NotNullAssertion(line, col, expr);
						}
								
						args.add( expr );
					}
					
					
					//DotOperator callfieldHashcode = new DotOperator(line, col, putterExpr, "\\.", "??.");
					
					/*Expression funInvoke = DotOperator.buildDotOperator(line, col, 
							DotOperator.buildDotOperatorOne(line, col, eqRefName(line, col, "theOther"), isActor?makeAsyncRefRefForFuncInvoke(line, col, name, refLevels):makeAsyncRefRefForName(line, col, name, refLevels)),
							new FuncInvoke(line, col, "equals", args)
							);*/
					
					ArrayList<Expression> eqItems = new ArrayList<Expression>();

					{
						Expression exxxp = DotOperator.buildDotOperatorOne(line, col, eqRefName(line, col, "theOther"), isActor?makeAsyncRefRefForFuncInvoke(line, col, name, refLevels):makeAsyncRefRefForName(line, col, name, refLevels));
						exxxp = typeIsPotentiallyNull?new NotNullAssertion(line, col, exxxp):exxxp;
						eqItems.add(exxxp);
					}
					
					eqItems.add(new FuncInvoke(line, col, "equals", args));
					
					Expression funInvoke = new DotOperator(line, col, eqItems, ".");
					NotExpression notequalsFunc =  new NotExpression(line, col, funInvoke);
					
					OrExpression ifTest2 = OrExpression.buildOrExpression(line, col, new RedirectableExpression(thisFieldIsNull2), new RedirectableExpression(otherFieldIsNull2), new RedirectableExpression(notequalsFunc)); 
					
					Block  ifoneOrOtherNullOrNotEQBlock = new Block(line, col);
					IfStatement ifoneOrOtherNullOrNotEQ=new IfStatement(line, col, ifTest2, ifoneOrOtherNullOrNotEQBlock, null);
					//ifoneOrOtherNullOrNotEQBlock.add(getRemoveFromDefEQ(line, col));
					ifoneOrOtherNullOrNotEQBlock.add(new LineHolder(line, col, new ReturnStatement(line, col, new RefBoolean(line, col, false))));
					
					onifNotBothNullFail.add(new LineHolder(line, col, ifoneOrOtherNullOrNotEQ)); 
				}
			}
			
			if(sup.equals(new ClassDefJava(Object.class) ) || isLambda){
				//mainBlock.add(getRemoveFromDefEQ(line, col));
				mainBlock.add(new LineHolder(line, col, new ReturnStatement(line, col, new RefBoolean(line, col, true))));
			}
			else{
				//inovke super class eq
				//return super.equals(theOther as superclass );
				FuncInvokeArgs args = new FuncInvokeArgs(line, col);
				qualifiedSuper = qualifiedSuper.copyTypeSpecific();
				List<Type> gens = qualifiedSuper.getGenTypes();
				if(gens != null) {
					List<Type> newgens = new ArrayList<Type>(gens.size());
					for(Type tt : gens) {
						if(tt instanceof NamedType) {
							((NamedType)tt).isWildCardAny=true;
						}
						newgens.add(tt);
					}
					qualifiedSuper.setGenTypes(newgens);
				}
				
				args.add(  new CastExpression(line, col, qualifiedSuper, eqRefName(line, col, "theOther") ) );
				Expression funInvoke = DotOperator.buildDotOperatorOne(line, col, new RefSuper(line, col), new FuncInvoke(line, col, "equals", args));
				mainBlock.add(getRemoveFromDefEQ(line, col));
				mainBlock.add(new LineHolder(line, col, new ReturnStatement(line, col, funInvoke)));
			}
			//have all the checks in an if block thingy [enhance later for objcs]
			
			Block functionBody = new Block(line, col);
			
			/*if(com.concurnas.lang.Equalifier.visitedDefaultEQMethodsInCurrentStack.contains(this))
				{ return false; }
			  else
			    { com.concurnas.lang.Equalifier.visitedDefaultEQMethodsInCurrentStack.put(this, this); }
			*/
			{
				FuncInvokeArgs contArgs = new FuncInvokeArgs(line, col);
				contArgs.add(new RefThis(line, col));
				
				ArrayList<Expression> contExpr = new ArrayList<Expression>();
				contExpr.add(new RefName(line, col, "com"));
				contExpr.add(new RefName(line, col, "concurnas"));
				contExpr.add(new RefName(line, col, "lang"));
				contExpr.add(new RefName(line, col, "Equalifier"));
				contExpr.add(new RefName(line, col, "defEQVisitAlready"));
				contExpr.add(new FuncInvoke(line, col, "containsKey", contArgs));
				
				Block failBlock = new Block(line, col);
				Block elseBlock = new Block(line, col);
				IfStatement checkNotCont = new IfStatement(line, col, new DotOperator(line, col, contExpr), failBlock, null, elseBlock);
				
				failBlock.add(new LineHolder(line, col, new ReturnStatement(line, col, new RefBoolean(line, col, true))));
				//elseBlock
				
				FuncInvokeArgs putterArgs = new FuncInvokeArgs(line, col);
				putterArgs.add(new RefThis(line, col));
				putterArgs.add(new RefThis(line, col));
				
				ArrayList<Expression> putterExpr = new ArrayList<Expression>();
				putterExpr.add(new RefName(line, col, "com"));
				putterExpr.add(new RefName(line, col, "concurnas"));
				putterExpr.add(new RefName(line, col, "lang"));
				putterExpr.add(new RefName(line, col, "Equalifier"));
				putterExpr.add(new RefName(line, col, "defEQVisitAlready"));
				putterExpr.add(new FuncInvoke(line, col, "put", putterArgs));//JPT: optimization would be to only call the put if you had objects with objects or calling super
				
				elseBlock.add(new LineHolder(line, col, new DuffAssign(line, col, new DotOperator(line, col, putterExpr))));
				
				functionBody.add(new LineHolder(line, col, checkNotCont));
			}
			
			functionBody.add(new LineHolder(line, col, ifNullStmt));
			functionBody.add(new LineHolder(line, col, ifInst));
			//functionBody.add(getRemoveFromDefEQ(line, col));
			functionBody.add(new LineHolder(line, col, new ReturnStatement(line, col, new RefBoolean(line, col, false))));
			
			Block theRealFunctionBody = new Block(line, col);//functionBody
			//we put this also into a try catch finally just in case someone manages to throw an exception...
			
			Block finalBlock = new Block(line, col); 
			
			{//potentiall for inf loop, note before we call super
				FuncInvokeArgs putterArgs = new FuncInvokeArgs(line, col);
				putterArgs.add(new RefThis(line, col));
				ArrayList<Expression> putterExpr = new ArrayList<Expression>();
				putterExpr.add(new RefName(line, col, "com"));
				putterExpr.add(new RefName(line, col, "concurnas"));
				putterExpr.add(new RefName(line, col, "lang"));
				putterExpr.add(new RefName(line, col, "Equalifier"));
				putterExpr.add(new RefName(line, col, "defEQVisitAlready"));
				FuncInvoke fi = new FuncInvoke(line, col, "remove", putterArgs);
				fi.setShouldBePresevedOnStack(false);
				putterExpr.add(fi);
				//finalBlock.add(new LineHolder(line, col, new DuffAssign(line, col, new DotOperator(line, col, putterExpr))));
				finalBlock.add(getRemoveFromDefEQ(line, col));
			}
			
			TryCatch oopsTryFin = new TryCatch( line,  col, functionBody, finalBlock);
			theRealFunctionBody.add(new LineHolder(line, col, oopsTryFin));
			

			String fname = isActor?"equals$ActorSuperCallObjM" : "equals";
			
			FuncDef eqfunc = new FuncDef(line, col, null, AccessModifier.PUBLIC, fname, inputs, theRealFunctionBody, new PrimativeType(PrimativeTypeEnum.BOOLEAN), !isActor, false, false, new ArrayList<Pair<String, NamedType>>());
			eqfunc.isAutoGennerated=true;
			
			FuncType sig = eqfunc.getFuncType();
			
			classDef.removeFuncDef(fname, sig);//remove because will have been processed already by prev cycle
			classDef.overwriteLineHolder(new Pair<String, FuncType>(fname, sig), new LineHolder(line, col, eqfunc));
			
			eqfunc.accept(satc);
			
			return eqfunc;
		}
		
		return null;
	}
	
	private static boolean typeIsPotentiallyNull(Type type, ClassDef cd) {
		if(type.getNullStatus() != NullStatus.NONNULL){
			return true;
		}else if(type instanceof GenericType ) {
			GenericType gt = (GenericType)type;
			String name = gt.name;
			while(cd != null) {
				if(cd.nameToGenericMap.containsKey(name)) {
					return cd.nameToGenericMap.get(name).getNullStatus() != NullStatus.NONNULL;
				}
				
				cd = cd.getParentNestor();
			}
		}
		return false;
	}
	
	private static ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>> getAllVars(ScopeAndTypeChecker satc, ClassDef classDef, boolean isActor){
		//ignore vars which are declared val and have a value set
		ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>> allVars = classDef.getAllFieldsDefined(true);
		
		if(isActor && !allVars.isEmpty()){//check getters corresponding to vars
			ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>> allVarsFromFuncs = new ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>>(allVars.size());
			for(Sixple<String, Type, Boolean, AccessModifier, Boolean, String> nameAndType : allVars){
				String getFunc = getGetterName(nameAndType.getA(), satc.getErrorRaiseableSupression(), nameAndType.getB(), 0,0);
				
				Type tt = ScopeAndTypeChecker.const_object.copyTypeSpecific();//on error default to object for comparison
				
				HashSet<TypeAndLocation> tals = satc.currentScopeFrame.getFuncDef(satc.currentScopeFrame, getFunc);
				
				if(tals != null && !tals.isEmpty()){
					for(TypeAndLocation tal : tals){
						FuncType t = (FuncType)tal.getType();
						if(t.getInputs().isEmpty()){
							tt = t.retType;
						}
					}
				}
				allVarsFromFuncs.add(new Sixple<String, Type, Boolean, AccessModifier, Boolean, String>(getFunc, tt, nameAndType.getC(), AccessModifier.PUBLIC, nameAndType.getE(), null));
			}
			allVars = allVarsFromFuncs;
		}
		
		return allVars;
	}
	
	public static FuncDef addDefaultHashCode(int line, int col, ClassDef classDef, ScopeAndTypeChecker satc, boolean isLambda, boolean isActor){
		//equals, hashcode, copier, isimmutable
		ClassDef sup = classDef.getSuperclass();
		NamedType qualifiedSuper = classDef.getFullyqualifiedSuperClassRef(line, col);
		if(qualifiedSuper != null
				&& !UncallableMethods.UNAVAILABLE_CLASSES.contains(sup)//no extending of Thread etc
				&& !isEqualsDefAbstract(sup)//if parent is abstract, then we cannot auto do this, user must define
				){ 
			
			//ArrayList<Tuple<String, Type>> allVars = getAllVars(satc, classDef, isActor);
			List<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>> allVars = classDef.getAllFieldsDefined(true);
			allVars = allVars.stream().filter(a -> !a.getC()).collect(Collectors.toList());//ignore items which have been set already
			
			//function begins - which needs to be moved to bytecode gen stage lols
			//we put this also into a try catch finally just in case someone manages to throw an exception...

			Block functionBody = new Block(line, col);
			boolean gonnaCallSuper = !sup.equals(new ClassDefJava(Object.class));
			if(!gonnaCallSuper && allVars.isEmpty()){//empty always has same hashcode
				functionBody.add(new LineHolder(line, col, new ReturnStatement(line, col,  new VarInt(line, col, 0))));
			}
			else{
				
				{
					FuncInvokeArgs contArgs = new FuncInvokeArgs(line, col);
					contArgs.add(new RefThis(line, col));
					
					ArrayList<Expression> contExpr = new ArrayList<Expression>();
					contExpr.add(new RefName(line, col, "com"));
					contExpr.add(new RefName(line, col, "concurnas"));
					contExpr.add(new RefName(line, col, "lang"));
					contExpr.add(new RefName(line, col, "Hasher"));
					contExpr.add(new RefName(line, col, "defVisitAlready"));
					contExpr.add(new FuncInvoke(line, col, "containsKey", contArgs));
					
					Block failBlock = new Block(line, col);
					IfStatement checkNotCont = new IfStatement(line, col, new DotOperator(line, col, contExpr), failBlock, null, null);
					
					failBlock.add(new LineHolder(line, col, new ReturnStatement(line, col,  new VarInt(line, col, 0))));
					
					functionBody.add(new LineHolder(line, col, checkNotCont));
				}
				
				boolean allFieldsPrimative = true;//default all prim
				
				for(Sixple<String, Type, Boolean, AccessModifier, Boolean, String> v : allVars){
					Type its = v.getB();
					if(!(its instanceof PrimativeType && !its.hasArrayLevels() && !(((PrimativeType)its).type == PrimativeTypeEnum.LAMBDA))){
						//not primative
						allFieldsPrimative=false;
						break;
					}
				}
				
				if(gonnaCallSuper || !allFieldsPrimative){//potentiall for inf loop
					FuncInvokeArgs putterArgs = new FuncInvokeArgs(line, col);
					putterArgs.add(new RefThis(line, col));
					putterArgs.add(new RefThis(line, col));
					ArrayList<Expression> putterExpr = new ArrayList<Expression>();
					putterExpr.add(new RefName(line, col, "com"));
					putterExpr.add(new RefName(line, col, "concurnas"));
					putterExpr.add(new RefName(line, col, "lang"));
					putterExpr.add(new RefName(line, col, "Hasher"));
					putterExpr.add(new RefName(line, col, "defVisitAlready"));
					putterExpr.add(new FuncInvoke(line, col, "put", putterArgs));
					functionBody.add(new LineHolder(line, col, new DuffAssign(line, col, new DotOperator(line, col, putterExpr))));
				}
				
				//iterate through each of the items, cast to int and increment or call assistance function as approperiate
	
				boolean first = true;
				for(Sixple<String, Type, Boolean, AccessModifier, Boolean, String> v : allVars){
					String name = v.getA();
					Type ttype = v.getB();
					AssignStyleEnum asStyle = first?AssignStyleEnum.EQUALS:AssignStyleEnum.PLUS_EQUALS;
					if(first){first=false;}
					if(ttype instanceof PrimativeType && !ttype.hasArrayLevels() && !(((PrimativeType)ttype).type == PrimativeTypeEnum.LAMBDA)){
						//hash += this.field as int
						
						ArrayList<Expression> putterExpr = new ArrayList<Expression>();
						putterExpr.add(new RefThis(line, col));
						//putterExpr.add(isActor?new FuncInvoke(line, col, name):eqRefName(line, col, name));
						putterExpr.add(eqRefName(line, col, name));
						
						DotOperator thisDotThat = new DotOperator(line, col, putterExpr);
						CastExpression toInt = new CastExpression(line, col, const_int, thisDotThat);
						
						functionBody.add(new LineHolder(line, col, new AssignExisting( line,  col, "hash$", asStyle, toInt)));
						
					}
					else if(ttype.hasArrayLevels()){//has array levels
						//com.concurnas.lang.Hasher.hashCode(this.field3Array)
						
						ArrayList<Expression> thisDotName = new ArrayList<Expression>();
						thisDotName.add(new RefThis(line, col));
						//thisDotName.add(isActor?new FuncInvoke(line, col, name):eqRefName(line, col, name));
						thisDotName.add(eqRefName(line, col, name));
						DotOperator thisDotThat = new DotOperator(line, col, thisDotName);
						
						FuncInvokeArgs hashCArgs = new FuncInvokeArgs(line, col);
						hashCArgs.add(thisDotThat);
						
						ArrayList<Expression> callHasher = new ArrayList<Expression>();
						callHasher.add(new RefName(line, col, "com"));
						callHasher.add(new RefName(line, col, "concurnas"));
						callHasher.add(new RefName(line, col, "lang"));
						callHasher.add(new RefName(line, col, "Hasher"));//TODO:change to lowercase or hide in some manor
						callHasher.add(new FuncInvoke(line, col, "hashCode", hashCArgs));
						

						DotOperator hashCall = new DotOperator(line, col, callHasher);
						
						functionBody.add(new LineHolder(line, col, new AssignExisting( line,  col, "hash$", asStyle, hashCall)));
					}
					else{
						//object
						//hash += this.field.hashCode()
						//hash += this.field&==null?0:this.field.hashCode()
						
						
						int refLevels = TypeCheckUtils.getRefLevels(ttype);
						
						ArrayList<Expression> putterExpr = new ArrayList<Expression>();
						putterExpr.add(new RefThis(line, col));
						//putterExpr.add(isActor?makeAsyncRefRefForFuncInvoke(line, col, name, refLevels):makeAsyncRefRefForName(line, col, name, refLevels));
						putterExpr.add(makeAsyncRefRefForName(line, col, name, refLevels));
						DotOperator callfieldHashcodea = new DotOperator(line, col, putterExpr, "\\." );
						

						ArrayList<Expression> putterExpr2 = new ArrayList<Expression>();
						

						boolean typeIsPotentiallyNull = typeIsPotentiallyNull(ttype, classDef);
						
						putterExpr2.add(typeIsPotentiallyNull?new NotNullAssertion(line, col, callfieldHashcodea):callfieldHashcodea);
						//putterExpr2.add(callfieldHashcodea);
						putterExpr2.add(new FuncInvoke(line, col, "hashCode", new FuncInvokeArgs(line, col)));
						
						DotOperator callfieldHashcode = new DotOperator(line, col, putterExpr2, "\\." );
						
						
						ArrayList<Expression> fieldRef = new ArrayList<Expression>();
						fieldRef.add(new RefThis(line, col));
						//fieldRef.add(isActor?makeAsyncRefRefForFuncInvoke(line, col, name, refLevels):makeAsyncRefRefForName(line, col, name, refLevels));
						fieldRef.add(makeAsyncRefRefForName(line, col, name, refLevels));
						
						EqReExpression refEQ = new EqReExpression(line, col, new DotOperator(line, col, fieldRef), GrandLogicalOperatorEnum.REFEQ, new VarNull(line, col));
						
						//IfExpr ifNullCheck = new IfExpr(line,  col, refEQ, new VarInt(line, col, 0), callfieldHashcode);
						
						functionBody.add(new LineHolder(line, col, new AssignExisting( line,  col, "hash$", asStyle, IfStatement.makeFromTwoExprs(line, col, new VarInt(line, col, 0), callfieldHashcode, refEQ))));
					}
					
				}
				
				
				if(gonnaCallSuper || !allFieldsPrimative){//potentiall for inf loop, note before we call super
					FuncInvokeArgs putterArgs = new FuncInvokeArgs(line, col);
					putterArgs.add(new RefThis(line, col));
					ArrayList<Expression> putterExpr = new ArrayList<Expression>();
					putterExpr.add(new RefName(line, col, "com"));
					putterExpr.add(new RefName(line, col, "concurnas"));
					putterExpr.add(new RefName(line, col, "lang"));
					putterExpr.add(new RefName(line, col, "Hasher"));
					putterExpr.add(new RefName(line, col, "defVisitAlready"));
					FuncInvoke fi = new FuncInvoke(line, col, "remove", putterArgs);
					fi.setShouldBePresevedOnStack(false);
					putterExpr.add(fi);
					functionBody.add(new LineHolder(line, col, new DuffAssign(line, col, new DotOperator(line, col, putterExpr))));
				}
				
				if(gonnaCallSuper){
					AssignStyleEnum asStyle = first?AssignStyleEnum.EQUALS:AssignStyleEnum.PLUS_EQUALS;
					//if(first){first=false;}
					
					ArrayList<Expression> putterExpr = new ArrayList<Expression>();
					
					
					
					
					putterExpr.add(new RefSuper(line, col));
					putterExpr.add(new FuncInvoke(line, col, "hashCode", new FuncInvokeArgs(line, col)));
					DotOperator callfieldHashcode = new DotOperator(line, col, putterExpr);
					
					functionBody.add(new LineHolder(line, col, new AssignExisting( line,  col, "hash$", asStyle, callfieldHashcode)));
				}

				
				functionBody.add(new LineHolder(line, col, new ReturnStatement(line, col,  eqRefName(line, col, "hash$"))));
			}
			
			
			Block theRealFunctionBody = new Block(line, col);//functionBody
			//we put this also into a try catch finally just in case someone manages to throw an exception...
			
			Block finalBlock = new Block(line, col); 
			
			{//potentiall for inf loop, note before we call super
				FuncInvokeArgs putterArgs = new FuncInvokeArgs(line, col);
				putterArgs.add(new RefThis(line, col));
				ArrayList<Expression> putterExpr = new ArrayList<Expression>();
				putterExpr.add(new RefName(line, col, "com"));
				putterExpr.add(new RefName(line, col, "concurnas"));
				putterExpr.add(new RefName(line, col, "lang"));
				putterExpr.add(new RefName(line, col, "Hasher"));
				putterExpr.add(new RefName(line, col, "defVisitAlready"));
				FuncInvoke fi = new FuncInvoke(line, col, "remove", putterArgs);
				fi.setShouldBePresevedOnStack(false);
				putterExpr.add(fi);
				finalBlock.add(new LineHolder(line, col, new DuffAssign(line, col, new DotOperator(line, col, putterExpr))));
			}
			
			TryCatch oopsTryFin = new TryCatch( line,  col, functionBody, finalBlock);
			theRealFunctionBody.add(new LineHolder(line, col, oopsTryFin));
			
			String fname = isActor?"hashCode$ActorSuperCallObjM" : "hashCode";
			
			FuncDef hcFunc = new FuncDef(line, col, null, AccessModifier.PUBLIC, fname, new FuncParams(line, col), theRealFunctionBody, new PrimativeType(PrimativeTypeEnum.INT), !isActor, false, false, new ArrayList<Pair<String, NamedType>>());
			hcFunc.isAutoGennerated=true;
			
			FuncType sig = hcFunc.getFuncType();
			
			classDef.removeFuncDef(fname, sig);//remove because will have been processed already by prev cycle
			classDef.overwriteLineHolder(new Pair<String, FuncType>(fname, sig), new LineHolder(line, col, hcFunc));
			
			/*
			 * PrintSourceVisitor psv = new PrintSourceVisitor(); psv.visit(hcFunc);
			 * System.err.println("" + psv);
			 */
			
			hcFunc.accept(satc);
			
			return hcFunc;
		}
		
		return null;
	}
	
	private static LineHolder getRemoveFromDefEQ(int line, int col){
		//com.concurnas.lang.Equalifier.visitedDefaultEQMethodsInCurrentStack.remove(this);
		FuncInvokeArgs rmFunArfs = new FuncInvokeArgs(line, col);
		rmFunArfs.add(new RefThis(line, col));
		
		ArrayList<Expression> rmExpr = new ArrayList<Expression>();
		rmExpr.add(new RefName(line, col, "com"));
		rmExpr.add(new RefName(line, col, "concurnas"));
		rmExpr.add(new RefName(line, col, "lang"));
		rmExpr.add(new RefName(line, col, "Equalifier"));
		rmExpr.add(new RefName(line, col, "defEQVisitAlready"));
		
		FuncInvoke fi = new FuncInvoke(line, col, "remove", rmFunArfs);
		fi.setShouldBePresevedOnStack(false);
		rmExpr.add(fi);
		
		return new LineHolder(line, col, new DuffAssign(line, col, new DotOperator(line, col, rmExpr)));
	}

	/* adds:
	  public def <init> ( types java.lang.Class < ? > [] ) void {
	      super . ( types , bytecodeSandbox . MyClass & ( ) ) ;
	      super \. onInit ( ) ;
	    }
	    
	    or
	    
	    
	    public def <init> ( types$forActor java.lang.Class < ? > [] , parnest bytecodeSandbox.Outer ) void {
	      super . ( types$forActor , {
								        x ( ) bytecodeSandbox.Outer.Inner = bytecodeSandbox . Outer . Inner & ( ) ;
								        x . bind ( parnest ) ;
								        x ;
								      }
	      		) ;
	      super \. onInit ( ) ;
    }
	    
	 */
	public static FuncType addActorConstructor(ErrorRaiseable scopeAndTypeChecker, ScopeAndTypeChecker errors, ClassDef classDef, NamedType actee, FuncType sig, int line, int col, boolean justPassThroughArg, Annotations annotations, boolean isInjected){
		int a=0;
		FuncParams params = getActorFuncParams(line, col, actee);
		FuncInvokeArgs superArgs = getActorSuperParams(line, col);
		FuncInvokeArgs confia = new FuncInvokeArgs(line, col);
		
		
		if(classDef.isActorOfClassRef != null){
			params.add(new FuncParam(0, 0, "classRef$forActor", classDef.isActorOfClassRef.copyTypeSpecific(), true ));
		}
		
				
		if(sig.origonatingFuncDef != null && sig.origonatingFuncDef.params != null){
			int n=0;
			for(FuncParam fp : sig.origonatingFuncDef.params.params){
				String name = fp.name;
				Type fType = fp.getTaggedType();
				
				if(TypeCheckUtils.containsGenericTypeRef(fType)){
					fType = sig.getInputs().get(n);//hack out the generic types
				}
				
				FuncParam fps = new FuncParam(line, col, name, fType, false);
				fps.defaultValue = fp.defaultValue;
				fps.isVararg = fp.isVararg;
				if(fps.isVararg){//oops, tag one down
					fType = (Type)fType.copy();
					fType.setArrayLevels(fType.getArrayLevels()-1);
					fps.type = fType;
				}
				
				params.add(fps);
				confia.add(eqRefName(line, col, name));
				n++;
			}
		}
		else{
			for(Type argType : sig.getInputs()){
				String name = "a" + a++; 
				params.add(new FuncParam(line, col, name, argType, false));
				confia.add(eqRefName(line, col, name));
			}
		}
		

		ArrayList<Type> args = new ArrayList<Type>(params.params.size());
		for(FuncParam fp : params.params){
			args.add(fp.getTaggedType());
		}
		FuncType f = constructorExistsAlready(classDef, args);
		if(null != f){
			//classDef.getScopeFrame().removeConstructorDef(f);
			return f;
		}//exists already
		
		Block constructorBody = new Block(line, col);
		
		if(justPassThroughArg){
			superArgs.add(eqRefName(line, col,"a0"));
		}
		else{
			Expression refToThingy;
			if(classDef.isActorOfClassRef != null){
				refToThingy = new FuncRef(line, col, eqRefName(line, col, "classRef$forActor"), new FuncRefArgs(confia));
			}else{
				refToThingy = createConstructorRef(line, col, actee, confia);
			}
			superArgs.add(refToThingy);
		}

		SuperConstructorInvoke superCon = new SuperConstructorInvoke(line, col, superArgs);
		constructorBody.add(new LineHolder(line, col, new DuffAssign(line, col, superCon)));
		constructorBody.add(new LineHolder(line, col, new DuffAssign(line, col, DotOperator.buildDotOperatorOne(line, col, new RefSuper(line, col), new FuncInvoke(line, col,"onInit")))));
		
		ConstructorDef constDef = new ConstructorDef(line, col, AccessModifier.PUBLIC, params, constructorBody);//default constuctor always public
		//default constructor has same access modifier as class itself, e.g. private if class is declared private
		constDef.isAutoGennerated = true;
		constDef.setAnnotations(annotations);
		constDef.isInjected = isInjected;
		
		
		addFunctionToClassDef(errors, classDef, classDef.className, constDef, line, col, true, true);
		return constDef.getFuncType();
	}

	private static Expression createConstructorRef(int line, int col, NamedType actee, FuncInvokeArgs confia){
		NamedType parNest = actee.getparentNestorFakeNamedType();
		Expression expr;
		if(null != parNest){
			Block pNestBlock = new Block(line, col);
			String name = actee.getNamedTypeStr();
			int lastDot = name.lastIndexOf('.');
			if(lastDot != -1){
				name = name.substring(lastDot+1);
			}
			if(name.contains("<")){
				name = name.substring(0, name.indexOf('<'));
			}
			FuncRefArgs fra = new FuncRefArgs(line, col);
			for(Expression exora : confia.asnames){
				fra.addExpr(exora);
			}
			
			DotOperator uBoundConstrRef = DotOperator.buildDotOperator(line, col, new RefNamedType(line, col, parNest), 
					new FuncRef(line, col, new RefQualifiedGenericNamedType(line, col, name, actee.getGenericTypeElements()), fra) );
			
			AssignExisting x = new AssignExisting(line, col, "x", AssignStyleEnum.EQUALS, uBoundConstrRef);
			
			FuncInvokeArgs fia = new FuncInvokeArgs(line, col);
			fia.add(eqRefName(line, col, ScopeAndTypeChecker.NestedTypeRefForActor));
			DotOperator bind = DotOperator.buildDotOperator(line, col, eqRefName(line, col, "x"), new FuncInvoke(line, col, "bind", fia));
			
			pNestBlock.add(new LineHolder(line, col, x));
			pNestBlock.add(new LineHolder(line, col, new DuffAssign(line, col, bind)));
			pNestBlock.add(new LineHolder(line, col, new DuffAssign(line, col,eqRefName(line, col, "x"))));
			pNestBlock.setShouldBePresevedOnStack(true);
			
			expr = pNestBlock;
		}
		else{
			FuncRefArgs fra = new FuncRefArgs(confia);
			expr = new NamedConstructorRef(line, col, new New(line, col, actee, confia, true), fra);
		}
		
		return expr;
	}
	
	
	
	/* adds:
	  public def <init> ( types java.lang.Class < ? > [], a int, b int ) void {
	      super . ( types , bytecodeSandbox . MyClass & (a+b ) ) ;
	      super \. onInit ( ) ;
	    }
	 */
	
	private static FuncParams getActorFuncParams(int line, int col, NamedType actee){
		FuncParams params = new FuncParams(line, col);
		
		if(actee!= null && actee.getSetClassDef() != null && actee.getSetClassDef().isActor){
			return params;
		}
		
		params.add(new FuncParam(line, col, ScopeAndTypeChecker.TypesForActor, ScopeAndTypeChecker.const_classArray_nt_array.copyTypeSpecific(), false));
		return params;
	}
	
	private static FuncInvokeArgs getActorSuperParams(int line, int col){
		FuncInvokeArgs superArgs = new FuncInvokeArgs(line, col);
		//add first type arg
		superArgs.add(eqRefName(line, col, ScopeAndTypeChecker.TypesForActor));
		return superArgs;
		
	}

	public static FuncType addActorConstructorWithArgs(ErrorRaiseable errorRaisableSupression, ScopeAndTypeChecker errors, ClassDef classDef, NamedType actee, ClassDefArgs classDefArgs, ArrayList<Expression> acteeClassExpressions, ArrayList<Expression> superConstrThings, HashSet<String> superClassReferencedVariableNames, HashSet<String> acteeClassReferencedVariableNames, int line, int col, Annotations annotations, boolean isInjected) {
		
		FuncParams params = getActorFuncParams(line, col, actee);
		FuncInvokeArgs superArgs = getActorSuperParams(line, col);
		FuncInvokeArgs confia = new FuncInvokeArgs(line, col);
		
		if(null != classDefArgs){
			for(int n= 0; n < classDefArgs.aargs.size(); n++){
				ClassDefArg argType = classDefArgs.aargs.get(n);
				Type fType = argType.type;
				FuncParam fp = new FuncParam(line, col, argType.name, fType, false);
				fp.defaultValue = argType.defaultValue;
				//fp.accept(errors);//JPT: may need to accept in othe methods defined in this class, force mapping from
				fp.annotations = argType.annotations;
				fp.isVararg = argType.isVararg;
				
				/*if(fp.isVararg){//oops, tag one down
					fType = (Type)fType.copy();
					fType.setArrayLevels(fType.getArrayLevels()-1);
					fp.type = fType;
				}*/
				params.add(fp);
			}
		}
		
		ArrayList<Type> args = new ArrayList<Type>(params.params.size());
		for(FuncParam fp : params.params){
			args.add(fp.getTaggedType());
		}
		FuncType f = constructorExistsAlready(classDef, args);
		if(null != f){ return f;}//exists already
		
		
		for(Expression e : acteeClassExpressions){
			confia.asnames.add((Expression)e.copy());
		}

		Block constructorBody = new Block(line, col);
		if(null == actee){//typed abstract actor, so just pass through args
			superArgs.add( eqRefName(line, col, classDefArgs.aargs.get(0).name) );
		}
		else{
			superArgs.add(createConstructorRef(line, col, actee, confia));
		}
		
		if(!superConstrThings.isEmpty()){
			for(Expression expr : superConstrThings){
				superArgs.add( expr  );
			}
			
		}

		SuperConstructorInvoke superCon = new SuperConstructorInvoke(line, col, superArgs);
		constructorBody.add(new LineHolder(line, col, new DuffAssign(line, col, superCon)));
		
		if(null != classDefArgs){
			for(int n= 0; n < classDefArgs.aargs.size(); n++){
				if(actee == null && n==0){
					continue;
				}
				ClassDefArg argType = classDefArgs.aargs.get(n);
				String name = argType.name;
				if(!superClassReferencedVariableNames.contains(name) && !acteeClassReferencedVariableNames.contains(name)){//pass to super constructor...
					AssignExisting ae = new AssignExisting(classDef.getLine(), classDef.getColumn(), DotOperator.buildDotOperator(classDef.getLine(), classDef.getColumn(), new RefThis(classDef.getLine(), classDef.getColumn()), eqRefName(classDef.getLine(), classDef.getColumn(), name)),  
							AssignStyleEnum.EQUALS, 
							eqRefName(classDef.getLine(), classDef.getColumn(), name) );		
					constructorBody.add(new LineHolder(classDef.getLine(), classDef.getColumn(), ae));
				}
			}
		}
		
		//add setters
		constructorBody.add(new LineHolder(line, col, new DuffAssign(line, col, DotOperator.buildDotOperatorOne(line, col, new RefSuper(line, col), new FuncInvoke(line, col,"onInit")))));
		
		ConstructorDef constDef = new ConstructorDef(line, col, AccessModifier.PUBLIC, params, constructorBody);//default constuctor always public
		//default constructor has same access modifier as class itself, e.g. private if class is declared private
		constDef.isAutoGennerated = true;
		constDef.setAnnotations(annotations);
		constDef.isInjected = isInjected;
		
		addFunctionToClassDef(errors, classDef, classDef.className, constDef, line, col, true, true);
		
		return constDef.getFuncType();
	}
	
	
	
	
	
	
	
	
	
	
	
	/*
	 adds:
	     public def funcla$ActorCall ( ) java.lang.String {
	      ret java.lang.String : ;
	      super . addCall ( 2 , new com.concurnas.lang.tuples.Tuple2 < java.lang.String : , ( ) java.lang.String > ( ret : , bytecodeSandbox . MyClass . funcla & ( ) ) ) ;
	      
	      useExtractor::
	      super . addCall ( 2 , new com.concurnas.lang.tuples.Tuple2 < java.lang.Boolean : , ( ) boolean > ( ret : , bytecodeSandbox . MyClass . equals & ( a0 is com.concurnas.lang.TypedActor < bytecodeSandbox.MyClass > ?
	       a0 as com.concurnas.lang.TypedActor < bytecodeSandbox.MyClass > . getActeeClone$ActorSuperCall ( ) 
	       : a0 ) ) ) ;
	       ^
      		String.format("super.addCall(2, new com.concurnas.lang.tuples.Tuple2<Boolean:, () boolean> ( ret:, %s.equals&((a0 as com.concurnas.lang.TypedActor<%s>).getActeeClone() if a0 is com.concurnas.lang.TypedActor<%s> else a0)));", actingOnType, actingOnType, actingOnType) +
					         
	      
	      return ret ;
	    }
	 */
	public static void addActorMethod(ErrorRaiseable scopeAndTypeChecker, ScopeAndTypeChecker errors, String funcName, String funcToCall, ClassDef classDef, NamedType actingOnType, FuncType sig, ArrayList<Type> inputs, Type retType, int line, int col, boolean callIsOnActorItself, boolean useExtractor) {
		int a=0;
		FuncParams params = new FuncParams(line, col);
		FuncRefArgs confia = new FuncRefArgs(line, col);
		
		//actingOnType = (NamedType)TypeCheckUtils.shiftTypeToUpperBounds(actingOnType);
		
		if(useExtractor){
			String name = "a0";
			params.add(new FuncParam(line, col, name, ScopeAndTypeChecker.const_object.copyTypeSpecific(), false));
			
			ArrayList<Type> taGens = new ArrayList<Type>();
			taGens.add(actingOnType);
			NamedType typedActor = new NamedType(line, col, "com.concurnas.lang.TypedActor", taGens);
			Is isFella = new Is(line, col, eqRefName(line, col, name), typedActor, false);
			
			CastExpression castTo = new CastExpression(line, col, typedActor.copyTypeSpecific(), eqRefName(line, col, name));
			FuncInvoke getActeeClone = new FuncInvoke(line, col, "getActeeClone", new FuncInvokeArgs(line, col));
			
			//IfExpr ifExpr = new IfExpr(line, col, isFella, DotOperator.buildDotOperator(line,  col, castTo, getActeeClone), eqRefName(line, col, name));
			
			confia.addExpr(IfStatement.makeFromTwoExprs(line, col, DotOperator.buildDotOperator(line,  col, castTo, getActeeClone), eqRefName(line, col, name), isFella));
		}
		else{
			
			if(null != sig.origonatingFuncDef && sig.origonatingFuncDef.params != null){
				int n=0;
				for(FuncParam fp : sig.origonatingFuncDef.params.params){
					String name = fp.name;
					Type fType = fp.getTaggedType();
					/*if(TypeCheckUtils.containsGenericTypeRef(fType)){
						fType = sig.getInputs().get(n);//hack out the generic types
					}*/
					
					//fType = (Type)fType.copy();
					//fType.setOrigonalGenericTypeUpperBound(null);
					
					FuncParam fps = new FuncParam(line, col, name, fType, false);
					fps.defaultValue = fp.defaultValue;
					fps.isVararg = fp.isVararg;
					
					if(fp.isVararg){//oops, tag one down
						fType = (Type)fType.copy();
						fType.setArrayLevels(fType.getArrayLevels()-1);
						fps.type = fType;
					}
					
					params.add(fps);
					
					Expression paramArg = eqRefName(line, col, name);
					
					if(TypeCheckUtils.containsGenericTypeRef(fType)){
						Type castToType = sig.getInputs().get(n);//hack out the generic types
						castToType = (Type)castToType.copy();
						castToType.setOrigonalGenericTypeUpperBound(null);
						paramArg = new CastExpression(line, col, castToType, new CastExpression(line, col, ScopeAndTypeChecker.const_object, paramArg));
					}
					confia.addExpr(paramArg);
					n++;
				}
			}
			else{
				for(Type argType : inputs){
					String name = "a" + a++; 
					params.add(new FuncParam(line, col, name, argType, false));
					confia.addExpr(eqRefName(line, col, name));
				}
			}
		
		}
		
		Block body = new Block(line, col);
		
		NamedType retRef = new NamedType(line, col, TypeCheckUtils.boxTypeIfPrimative((Type)retType.copy(), false));
		body.add(new LineHolder(line, col, new AssignNew(null, line, col, "ret", retRef)));
		
		FuncInvokeArgs addCallExprars = new FuncInvokeArgs(line, col);
		addCallExprars.add(new VarInt(line, col, callIsOnActorItself?1:2));//actor calls get higher priority than actee
		
		//tuplegenTypes.add(retRef.copyTypeSpecific());
		//tuplegenTypes.add((FuncType)TypeCheckUtils.shiftTypeToUpperBounds(new FuncType(sig.retType)));//shifting the return type to upper bound is a little bit of a hack...
		//tuplegenTypes.add(new FuncType(sig.retType));//shifting the return type to upper bound is a little bit of a hack...
		
		int returnRefLevels = TypeCheckUtils.getRefLevels(retType);
		
		FuncInvokeArgs newTupleArgs = new FuncInvokeArgs(line, col);
		newTupleArgs.add(new AsyncRefRef(line, col, eqRefName(line, col, "ret"), returnRefLevels  + 1));
		
		FuncRef funcRef = new FuncRef(line, col, eqRefName(line, col, funcToCall), confia);
		
		DotOperator refToEQCall =  DotOperator.buildDotOperator(line, col, callIsOnActorItself?new RefThis(line, col):new RefNamedType(line, col,actingOnType), funcRef );
		newTupleArgs.add(refToEQCall );
		New newTuple = new New(line, col, new NamedType(line, col, "com.concurnas.runtime.Pair"), newTupleArgs, true);
		addCallExprars.add(newTuple);
		
		
		body.add(new LineHolder(line, col, new DuffAssign(line, col, DotOperator.buildDotOperator(line,  col, new RefSuper(line, col), new FuncInvoke(line, col, "addCall", addCallExprars)))));
		
		if(retType.equals(ScopeAndTypeChecker.const_void)){
			body.add(new LineHolder(line, col, new DuffAssign(line, col, DotOperator.buildDotOperator(line, col, new AsyncRefRef(line, col, eqRefName(line, col, "ret"), 1), new FuncInvoke(line, col, "get")))));
			body.add(new LineHolder(line, col, new ReturnStatement(line, col)));
		}
		else{//TODO: add cast?
			if(returnRefLevels > 0) {
				body.add(new LineHolder(line, col, new ReturnStatement(line, col, new AsyncRefRef(line, col, eqRefName(line, col, "ret"), returnRefLevels) )));
			}else {
				body.add(new LineHolder(line, col, new ReturnStatement(line, col, eqRefName(line, col, "ret"))));
			}
		}
		
		ArrayList<Pair<String, NamedType>> localGens = new ArrayList<Pair<String, NamedType>>();
		if(null != sig.getLocalGenerics()){
			funcRef.genTypes = new ArrayList<Type>(sig.getLocalGenerics());
			for(GenericType gen : sig.getLocalGenerics()){
				localGens.add(new Pair<String, NamedType>(gen.name, gen.upperBound));
			}
		}
		
		
		FuncDef getter = new FuncDef(classDef.getLine(), classDef.getColumn(), null, AccessModifier.PUBLIC, funcName, params, body, (Type)retType.copy(), false, false, false, localGens);
		getter.isAutoGennerated = true;
		
		Annotations annots = new Annotations();;
		annots.annotations.add(new Annotation(line,col, "SuppressWarnings", new VarString(line, col, "generic-cast"), null, new ArrayList<String>()));
		getter.setAnnotations(annots);
		
		if(sig.origonatingFuncDef != null) {
			getter.isInjected = sig.origonatingFuncDef.isInjected;
		}
		
		addFunctionToClassDef(errors, classDef, funcName, getter,line,col, false, true);
		
		
	}
	
	public static void addAbstractMethod(int line, int col, ScopeAndTypeChecker satc, String fname, ClassDef classDef, FuncType sig) {
		FuncParams params = new FuncParams(line, col);
		FuncInvokeArgs confia = new FuncInvokeArgs(line, col);
		int a = 0;
		for(Type argType : sig.inputs){
			String name = "a" + a++; 
			params.add(new FuncParam(line, col, name, argType, false));
			confia.add(eqRefName(line, col, name));
		}

		
		FuncDef getter = new FuncDef(classDef.getLine(), classDef.getColumn(), null, AccessModifier.PUBLIC, fname, params, null, (Type)sig.retType.copy(), true, true, false, new ArrayList<Pair<String, NamedType>>());
		getter.isAutoGennerated = true;
		addFunctionToClassDef(satc, classDef, fname, getter, line, col, false, false);
	}

	public static void addActorCallerMethod(ErrorRaiseable scopeAndTypeChecker, ScopeAndTypeChecker errors, String fname, ClassDef classDef, NamedType actingOnType, FuncType sig, ArrayList<Type> inputs, Type retType, int line, int col) {
		String funcName = fname + "$ActorCall";
		
		int a=0;
		FuncParams params = new FuncParams(line, col);
		FuncInvokeArgs confia = new FuncInvokeArgs(line, col);
		
		for(Type argType : inputs){
			String name = "a" + a++; 
			params.add(new FuncParam(line, col, name, argType, false));
			confia.add(eqRefName(line, col, name));
		}

		Block body = new Block(line, col);
		body.add(new LineHolder(line, col, new ReturnStatement(line, col, DotOperator.buildDotOperator(line, col, new RefThis(line, col), new FuncInvoke(line, col, funcName, confia) ))));
		
		FuncDef getter = new FuncDef(classDef.getLine(), classDef.getColumn(), null, AccessModifier.PUBLIC, fname, params, body, (Type)retType.copy(), false, false, false, new ArrayList<Pair<String, NamedType>>());
		getter.isAutoGennerated = true;
		addFunctionToClassDef(errors, classDef, fname, getter,line,col, false, false);
	}

	public static void addCallSuperMethod(ErrorRaiseable scopeAndTypeChecker, ScopeAndTypeChecker errors, String funcName, String toCall, ClassDef classDef, FuncType toStringsig, int line, int col) {
		//ignores input args at the moment, only works for no arg methods (only really needed for these anyway)
		FuncInvokeArgs confia = new FuncInvokeArgs(line, col);
		FuncParams params = new FuncParams(line, col);
		Block body = new Block(line, col);
		body.add(new LineHolder(line, col, new ReturnStatement(line, col, DotOperator.buildDotOperator(line, col, new RefSuper(line, col), new FuncInvoke(line, col, toCall, confia) ))));
		
		FuncDef getter = new FuncDef(line, col, null, AccessModifier.PUBLIC, funcName, params, body, (Type)toStringsig.retType.copy(), false, false, false, new ArrayList<Pair<String, NamedType>>());
		getter.isAutoGennerated = true;
		addFunctionToClassDef(errors, classDef, funcName, getter,line,col, false, false);
	}
	
	public static Expression defaultValueExpressionForType(int line, int col, Type forType){
		if(forType instanceof PrimativeType){
			PrimativeType asp = (PrimativeType)forType;
			switch(asp.type){
				case BOOLEAN : return new RefBoolean(line, col, false);
				case INT : return new VarInt(line, col, 0);
				case LONG : return new VarLong(line, col, 0l);
				case FLOAT : return new VarFloat(line, col, .0f);
				case DOUBLE : return new VarDouble(line, col, .0);
				case SHORT : return new CastExpression(line, col, forType, new VarInt(line, col, 0));
				case BYTE : return new CastExpression(line, col, forType, new VarInt(line, col, 0));
				case CHAR : return new VarChar(line, col, "");
				default: return new VarNull();  
			}
		}
		
		return new VarNull();  
	}
	
}