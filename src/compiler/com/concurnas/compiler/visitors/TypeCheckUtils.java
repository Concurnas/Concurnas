package com.concurnas.compiler.visitors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.misc.OrderedHashSet;

import com.concurnas.bootstrap.lang.Lambda;
import com.concurnas.bootstrap.lang.Lambda.Function1;
import com.concurnas.bootstrap.runtime.ref.DirectlyArrayAssignable;
import com.concurnas.bootstrap.runtime.ref.DirectlyArrayGettable;
import com.concurnas.bootstrap.runtime.ref.DirectlyAssignable;
import com.concurnas.bootstrap.runtime.ref.DirectlyGettable;
import com.concurnas.compiler.ast.AbstractType;
import com.concurnas.compiler.ast.AnonLambdaDef;
import com.concurnas.compiler.ast.AnonLambdaDefOrLambdaDef;
import com.concurnas.compiler.ast.ArrayDef;
import com.concurnas.compiler.ast.ArrayRef;
import com.concurnas.compiler.ast.AssignStyleEnum;
import com.concurnas.compiler.ast.AsyncRefRef;
import com.concurnas.compiler.ast.CastExpression;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.ast.CopyExpression;
import com.concurnas.compiler.ast.DotOperator;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.FuncParam;
import com.concurnas.compiler.ast.FuncParams;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.FuncTypeMany;
import com.concurnas.compiler.ast.GPUVarQualifier;
import com.concurnas.compiler.ast.GenericType;
import com.concurnas.compiler.ast.InoutGenericModifier;
import com.concurnas.compiler.ast.LambdaDef;
import com.concurnas.compiler.ast.ModuleType;
import com.concurnas.compiler.ast.MultiType;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.NamedTypeMany;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.NullStatus;
import com.concurnas.compiler.ast.PrimativeType;
import com.concurnas.compiler.ast.PrimativeTypeEnum;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.TypeReturningExpression;
import com.concurnas.compiler.ast.VarNull;
import com.concurnas.compiler.ast.Vectorized;
import com.concurnas.compiler.ast.VectorizedArrayRef;
import com.concurnas.compiler.ast.VectorizedFieldRef;
import com.concurnas.compiler.ast.Type.Vectorization;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.bytecode.FuncLocation;
import com.concurnas.compiler.bytecode.FuncLocation.StaticFuncLocation;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.utils.GenericTypeUtils;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.visitors.algos.GetMostSpecificAndTestAmbig;
import com.concurnas.compiler.visitors.lca.LowestCommonAncestor;
import com.concurnas.compiler.visitors.util.ErrorRaiseableSupressErrors;
import com.concurnas.compiler.visitors.util.ErrorRaiseableSupressErrorsAndLogProblem;
import com.concurnas.compiler.visitors.util.PermutorWithCache;
import com.concurnas.lang.DefaultMap;
import com.concurnas.runtime.Pair;
import com.concurnas.runtime.cps.ReferenceSet;

public class TypeCheckUtils {

	public static boolean isObjectType(Type input)
	{
		return !(input instanceof PrimativeType) || input.hasArrayLevels();
	}
	
	public static Expression checkCanBePrePostfiexed(Visitor vis, Expression e)
	{
		return checkCanBePrePostfiexed(vis, null, e, 0, 0);
	}
	
	public static ArrayList<Type> extractIntermediateVectTypes(ErrorRaiseable errorRaisableSupression, Type input, boolean justLast){
		ArrayList<Type> ret = new ArrayList<Type>();
		
		if(input != null) {
			boolean isList = input instanceof NamedType && null != TypeCheckUtils.checkSubType(errorRaisableSupression, list_object, input, 0, 0, 0, 0);
			boolean isArr = input.hasArrayLevels();
			
			input = (Type)input.copy();
			input = TypeCheckUtils.getRefType(input);
			
			while(isArr || isList) {
				if(isArr) {
					input.setArrayLevels(input.getArrayLevels()-1);
				}else {
					NamedType asNamed = (NamedType)input;
					NamedType listNT = asNamed.getSetClassDef().equals(list_object_cls)?asNamed:null;
					while(listNT == null) {
						for(NamedType iface : asNamed.getResolvedTraitsAsNamed()) {
							if(iface.getSetClassDef().equals(list_object_cls)) {
								listNT = iface;
								break;
							}
						}
						if(null == listNT) {
							asNamed = asNamed.getResolvedSuperTypeAsNamed();
							if(null == asNamed) {
								break;
							}
						}
					}
					
					if(listNT == null) {
						break;
					}
					
					input = listNT.getGenericTypeElements().get(0);
				}
				
				input = TypeCheckUtils.getRefTypeToLocked(input);//exract ref
				
				if(!justLast)
					ret.add((Type)input.copy()); {
				}
				
				isArr = input.hasArrayLevels();
				isList = input instanceof NamedType && null != TypeCheckUtils.checkSubType(errorRaisableSupression, list_object, input, 0, 0, 0, 0);
			}
		}
		
		
		
		if(justLast) {
			ret.add(input);
		}
		
		return ret;
	}
	
	public static Expression checkCanBePrePostfiexed(Visitor vis, ErrorRaiseable thisa, Expression e, int line, int col)
	{
		/*if(e instanceof RefNamedType){
			ArrayRef astRedirectToArrayRef =  ((RefNamedType)e).astRedirectToArrayRef;
			if(null != astRedirectToArrayRef){
				e = astRedirectToArrayRef;
			}
		}*/
		
		if(e instanceof Vectorized) {
			e= ((Vectorized)e).expr;
		}
		
		if(e instanceof VectorizedFieldRef) {
			e= ((VectorizedFieldRef)e).expr;
		}
		
		
		Expression eval = e;
		
		if(e instanceof DotOperator)
		{
			ArrayList<Expression> tails = ((DotOperator)e).getElements(vis);
			return checkCanBePrePostfiexed(vis, thisa, tails.get(tails.size()-1), line, col );
		}
		else if(e instanceof CopyExpression){
			return checkCanBePrePostfiexed(vis, thisa, ((CopyExpression)e).expr, line, col );
		}
		else if (e instanceof AsyncRefRef){
			return checkCanBePrePostfiexed(vis, thisa, ((AsyncRefRef)e).b,  line, col );
		}
		if(!(eval instanceof RefName || eval instanceof ArrayRef || eval instanceof VectorizedArrayRef))
		{
			if(null != thisa){
				thisa.raiseError(line, col, "Invalid argument to operation ++/--");
			}
		}
		
		if(eval instanceof ArrayRef){
			ArrayRef asAR = (ArrayRef)eval;
			Type t = asAR.getTaggedType();
			if(t != null) {
				asAR.dupLastThingIfRef = !TypeCheckUtils.hasRefLevels(t);
			}
		}
		
		
		
		if(eval instanceof RefName && ((RefName)eval).resolvesTo != null){
			RefName rn = (RefName)eval;
			if(vis instanceof ScopeAndTypeChecker){
				((ScopeAndTypeChecker)vis).checkFinalVarReassignment(eval.getLine(), eval.getColumn(), (rn).resolvesTo.getLocation(), rn.name, rn.getTaggedType(), rn.getTaggedType(), "cannot be incremented or decremented");
			}
		}
		
		return eval;
	}
	
	public static Type extractTypedActorType(ErrorRaiseable errorRaiseable, Type from){
		Type input = from;
		if(null != TypeCheckUtils.checkSubType(errorRaiseable.getErrorRaiseableSupression(), ScopeAndTypeChecker.const_typed_actor, from, 0, 0, 0, 0)){
			//its an actor, so check, track back all the way to actor
			NamedType asNamedx = (NamedType) from;
			boolean opOnTypedActor = ScopeAndTypeChecker.const_typed_actor_class.equals(asNamedx.getSetClassDef());
			if(!opOnTypedActor){
				while(asNamedx != null && !ScopeAndTypeChecker.const_typed_actor_class.equals(asNamedx.getSetClassDef())){//trace all the way back to the actor
					asNamedx = asNamedx.getResolvedSuperTypeAsNamed();
				}
			}
			
			if(null != asNamedx){
				List<Type> gens = asNamedx.getGenTypes();
				if(gens.isEmpty()){
					return input; 
				}
				else{
					Type tryActorThing = (Type)gens.get(0).copy();//always the first
					if(opOnTypedActor){//when as typed actor works ok
						return tryActorThing;
					}
					//else we need to set generics properly
					if(tryActorThing instanceof NamedType ){//if we're dealing with default actor splice in the gen types?
						NamedType asNamed = (NamedType)tryActorThing;
						HashMap<GenericType, Type> fromClassGenericToQualifiedType = ((NamedType)from).fromClassGenericToQualifiedType;
						if(asNamed.getSetClassDef() != null){
							ArrayList<GenericType> wanted = asNamed.getSetClassDef().getClassGenricList();
							ArrayList<Type> qualiGen = new ArrayList<Type>(wanted.size());
							for(GenericType gen : wanted){
								qualiGen.add(fromClassGenericToQualifiedType.get(gen));
							}
							
							asNamed.setGenTypes(qualiGen);
						}
						
						asNamed.fromClassGenericToQualifiedType = fromClassGenericToQualifiedType;
						return asNamed;
					}
					
					
				}
			}
		}
		return null;
	}
	
	public static boolean isAlreadyDefinedFuncReturnTypeCompatible(ErrorRaiseable errorRaiseable, Type subType, Type superType)
	{
		if(subType==null || superType == null || subType.equals(superType))
		{
			return true;
		}
		else if((subType instanceof NamedType || subType instanceof GenericType) && superType instanceof NamedType)
		{
			return null != TypeCheckUtils.checkSubType(errorRaiseable, superType, subType, 0, 0, 0, 0);
		}
		return false;
	}
	
	
	public static boolean isClassInstanceOfJavaCompiledClass(ErrorRaiseable invoker, Type checking, NamedType compiledType, int lhsLine, int lhsColumn)
	{
		return null != checkSubType(invoker.getErrorRaiseableSupression(), compiledType, checking, lhsLine,  lhsColumn, lhsLine,  lhsColumn);
	}
	
	public enum MatchType { NONE, DIRECT, DIRECT_VARARG, DIRECT_VARARG_VIASUPER, VIASUPER, DIRECT_BOXING, OUTPARAMASINPUT};
	
	//TODO: add GenericType to functions - basically treat this like an object for now
	
/*	public static boolean isString(Type input)
	{
		if(null!= input && input instanceof NamedType)
		{
			return ((NamedType)input).getPrettyName().equals("java.lang.String");
		}
		return false;
	}*/
	
	public static boolean isString(Type input){
		if(null!= input){
			if(input instanceof NamedType){
				if(((NamedType)input).getIsRef()){
					input = TypeCheckUtils.getRefType(input);
					return input instanceof NamedType && ((NamedType)input).getPrettyName().equals("java.lang.String");
				}
				return ((NamedType)input).getPrettyName().equals("java.lang.String");
			}
		}
		return false;
	}
	
	private static Set<String> mapTypes = new HashSet<String>();
	static{
		mapTypes.add("java.util.HashMap");
		mapTypes.add("java.util.Map");
		mapTypes.add("java.util.AbstractMap");
	}//JPT: this code and the next function have been stolen from MapDef, refactor!
	
	private static Pair<Type, Type> extractMapTypes(Type potent){
		if(potent instanceof NamedType){
			NamedType ntSrc = (NamedType)potent;
			ClassDef clsSrc = ntSrc.getSetClassDef();
			if(clsSrc != null && mapTypes.contains(clsSrc.toString())){
				ArrayList<Type> gensSrc = ntSrc.getGenericTypeElements();
				if(gensSrc.size() == 2){
					Type sourceKeyType = gensSrc.get(0);
					Type sourceValType = gensSrc.get(1);
					
					if(null == sourceKeyType || null == sourceValType){
						return null;
					}
					
					return new Pair<Type, Type>(sourceKeyType, sourceValType);
				}
			}
		}
		
		return null;
	}
	
	public static void mutatleRefType(int wantRefLEvels, NamedType refType, NamedType setRefTo){
		//convert from VarNull: to String: etc. So that this works ok: a String: = {null}!
		//we write over the returned type of the varnull
		
		NamedType finalRefType = refType;
		for(int n=0; n < wantRefLEvels-1; n++){
			finalRefType = (NamedType)finalRefType.getGenTypes().get(0);
		}
		
		VarNull shouldBe = (VarNull)finalRefType.getGenTypes().get(0);
		
		if(shouldBe.entwineOnRefMutateSet !=null){
			//should be set for cases like:  ok3 String:=(null)! if true else (null)!
			for(Type t : shouldBe.entwineOnRefMutateSet){
				mutatleRefType(TypeCheckUtils.getRefLevels(t), (NamedType)t, setRefTo);
			}
		}
		
		finalRefType.getGenTypes().set(0, setRefTo.copyTypeSpecific());
	}
	
	private static boolean hasNullListMatchingInHeirarchy(Type want, Type available){
		if(want instanceof VarNull && !want.hasArrayLevels() && (available instanceof NamedType || available.hasArrayLevels()) ) //what about if the avaiable type is an array (this is treated like an object 
		{//sepcial case, null is a type of any object
			return true;
		}
		
		
		int refLevelsWant = TypeCheckUtils.getRefLevels(want);
		if(refLevelsWant>0 && refLevelsWant==TypeCheckUtils.getRefLevels(available)){
			//another special case, null: is type of any object:
			Type refTypeWant = TypeCheckUtils.getRefType(want);
			Type refTypeAvail = TypeCheckUtils.getRefType(available);
			
			if(refTypeWant instanceof VarNull && !refTypeWant.hasArrayLevels() && (refTypeAvail instanceof NamedType) ) //what about if the avaiable type is an array (this is treated like an object 
			{//sepcial case, null: is a type of any object:
				//we also mutate the type inplace
				mutatleRefType(refLevelsWant, (NamedType)want, (NamedType)refTypeAvail);
				return true;
			}
		}
		
		if(want instanceof VarNull && want.hasArrayLevels() && (available instanceof NamedType || available.getArrayLevels() > want.getArrayLevels())  ) //what about if the avaiable type is an array (this is treated like an object 
		{//sepcial case, null is a type of any object
			return true;
		}
		
		if(want.getArrayLevels() == available.getArrayLevels()){
			Pair<Type, Type> wantMap = extractMapTypes(want);
			if(null != wantMap){
				Pair<Type, Type> avaiMap = extractMapTypes(available);
				if(null != avaiMap){
					Type wantKey = wantMap.getA();
					Type wantVal = wantMap.getB();
					
					Type avaiKey = avaiMap.getA();
					Type avaiVal = avaiMap.getB();
					
					return hasNullListMatchingInHeirarchy(wantKey, avaiKey) || hasNullListMatchingInHeirarchy(wantVal, avaiVal);
				}
			}
		}
		
		return false;
	}
	
	private static Type refUpIfNeeded(Type from, Type tola, int line, int col){

		int lhsRefLevels = TypeCheckUtils.getRefLevels(tola);
		int rhsRefLevels = TypeCheckUtils.getRefLevels(from);
		
		Type orignRef = null;
		
		//if(from instanceof NamedType){
			//NamedType nt = (NamedType)from;
			if(from != null && null != ((AbstractType)from).originRefType){
				orignRef = refUpIfNeeded(((AbstractType)from).originRefType, tola, line, col);
			}
		//}
		
		int tolevels = lhsRefLevels - rhsRefLevels;
		if(tolevels > 0){
			int x=0;
			while(x++ < tolevels){
				from = new NamedType(line, col, from);
			}
			
			//if(null != orignRef){
				((AbstractType)from).originRefType =orignRef;
			//}
		}
		
		
		return from;
	}
	
	/*private static FuncType convertInOutParameters(FuncType toConv){
		FuncType ftcop = toConv.copyTypeSpecific();
		ArrayList<Type> inputs = toConv.getInputs();
		ArrayList<Type> newinputs = new ArrayList<Type>(inputs.size());
		
		for(Type inputp : inputs){
			InoutGenericModifier inoutgen = inputp.getInOutGenModifier();
			if(inoutgen != null ){
				if(inoutgen == InoutGenericModifier.OUT){
					inputp=new ModuleType("out parameter cannot be used as a method argument");
				}else if(inoutgen == InoutGenericModifier.IN){
					inputp = inputp.getOrigonalGenericTypeUpperBound();
				}
			}
			newinputs.add(inputp);
		}
		
		Type retParam = ftcop.retType;
		InoutGenericModifier inoutgen = retParam.getInOutGenModifier();
		if(inoutgen != null ){
			if(inoutgen == InoutGenericModifier.OUT){
				retParam = retParam.getOrigonalGenericTypeUpperBound();
			}else if(inoutgen == InoutGenericModifier.IN){
				retParam=new ModuleType("in parameter cannot be returned from a method");
			}
		}
		
		ftcop.inputs = newinputs;
		ftcop.retType = retParam;
		
		return ftcop;
	}*/
	
	private static MatchType checkFunctionInvokationArgumentMatch(ScopeAndTypeChecker satc, ErrorRaiseable invoker, List<Type> argsWanted, List<Type> argsAviable, boolean refUpArgsIfNeeded, boolean ignoreGenericsOnNOMatch)
	{
		//note that we ignore the generic erasure and ONLY check the direct type not erasure generics stuff
		int cnt = argsWanted.size();
		boolean stillDirectMatch = true;
		boolean stillBoxing = true;
		int m=0;
		
		if(cnt != argsAviable.size()){
			return MatchType.NONE; 
		}
		
		for(int n = 0; n < cnt; n++)
		{//HERE OK
			Type passedInArg = convertfuncTypetoNamedType(argsWanted.get(n), argsAviable.get(m));
			
			Type argMatchingTo = convertfuncTypetoNamedType(argsAviable.get(m++), null);
			
			Type convertRetTo = TypeCheckUtils.canConvertRHSToArgLessLambda(satc, errorRaisableSupression, argMatchingTo, passedInArg);
			boolean convRetToIsVoid = false;
			if(convertRetTo != null) {
				argMatchingTo = convertRetTo;
				stillDirectMatch = false;
				convRetToIsVoid = TypeCheckUtils.isVoid(convertRetTo);
				if(convRetToIsVoid) {
					passedInArg = convertRetTo;
				}
			}
			
			if(null != TypeCheckUtils.checkSubType(errorRaisableSupression, ScopeAndTypeChecker.getLazyNT(), argMatchingTo) && !passedInArg.getIsTypeInFuncref() && argMatchingTo instanceof NamedType) {
				//if lhs is lazy, check rhs can be spliced in
				Type tt = ((NamedType)argMatchingTo).getGenericTypeElements().get(0);
				if(null != TypeCheckUtils.checkSubType(errorRaisableSupression, tt, passedInArg)){
					passedInArg = argMatchingTo;
					stillDirectMatch = false;
				}
			}			
			
			if(refUpArgsIfNeeded){
				passedInArg = refUpIfNeeded(passedInArg, argMatchingTo, 0, 0);
			}
			
			
			
			if(null == passedInArg || null == argMatchingTo )
			{
				return MatchType.NONE;
			}
			else if(passedInArg.equals(argMatchingTo)){
				continue;//all good
			}//TODO: next two lines concerning varnull and varnull arrays need to be consolidated, also this pattern can be found elsewhere in the code, go fix it k
			
			//PROBLEM HERE
			if(passedInArg instanceof GenericType /*&& !(argMatchingTo instanceof GenericType)*/){
				NamedType upper = ((GenericType)passedInArg).upperBound;
				if(null != upper) {
					passedInArg=upper;
				}
			}
			
			if(hasNullListMatchingInHeirarchy(passedInArg, argMatchingTo)){
				stillDirectMatch = false;
				stillBoxing=false;
				continue;
			}
						
			else if(((passedInArg instanceof PrimativeType && ((PrimativeType)passedInArg).type == PrimativeTypeEnum.VOID) ||
					(argMatchingTo instanceof PrimativeType && ((PrimativeType)argMatchingTo).type == PrimativeTypeEnum.VOID)) && !convRetToIsVoid)
			{
				return MatchType.NONE;
			}
			else if(argMatchingTo instanceof GenericType){
				NamedType gtUpper = ((GenericType) argMatchingTo).upperBound;
				if(null != gtUpper) {
					if(null == TypeCheckUtils.checkSubType(invoker, gtUpper, passedInArg)){
						return MatchType.NONE;
					}
				}
				
				//TODO: check generic type upper/lower bounds for conformance, for now assum object so all equal
				stillDirectMatch = false;
				stillBoxing=false;
				
			}
			else if(passedInArg.getArrayLevels() != argMatchingTo.getArrayLevels())
			{//TODO: test the below (with more than one arg)
				//special exceptions for jls 4.10.2....
				if(argMatchingTo instanceof NamedType && null != ((NamedType)argMatchingTo).getSetClassDef() && parentOfObjectArray(((NamedType)argMatchingTo).getSetClassDef().getPrettyName()))
				{
					if(argMatchingTo.hasArrayLevels())
					{
						/*
						Object e = new Object[2][2]; // - ok
						Object[] e1 = new Object[2][2]; // - ok
						Object[][] e2 = new Object[2][2]; // - ok
						java.io.Serializable[] e3 = new Object[2]; // not allowed
						java.io.Serializable[][] e4 = new Object[2][2]; // not allowed
						java.io.Serializable[] e5 = new Object[2][2];//ok
						*/
						
						if(((NamedType)argMatchingTo).getSetClassDef().getPrettyName().equals("java.lang.Object"))
						{
							if( argMatchingTo.getArrayLevels() <= passedInArg.getArrayLevels() )
							{
								stillDirectMatch = false;
								stillBoxing=false;
								continue;
							}
						}
						else
						{
							if( argMatchingTo.getArrayLevels() < passedInArg.getArrayLevels() )
							{//has to be less
								stillDirectMatch = false;
								stillBoxing=false;
								continue;
							}
						}
						return MatchType.NONE;
					}
					stillDirectMatch = false;
					stillBoxing=false;
					continue;
					
				}
				else
				{//if we pass in a RefArray to something expecting a RefArray then although the array levels dont match, we may still have a subtype match, so try this (cut and paste sorry!)
					if(/*passedInArg instanceof NamedType && argMatchingTo instanceof NamedType &&*/ null != checkSubType(invoker.getErrorRaiseableSupression(), argMatchingTo, passedInArg, 0, 0, 0,0, false))
					{//HashMap is a subtype of Map
						stillDirectMatch = false;
						stillBoxing=false;
					}else{
						return MatchType.NONE;
					}
				}
			}
			else if(passedInArg instanceof NamedType && argMatchingTo instanceof NamedType && null != checkSubType(invoker.getErrorRaiseableSupression(), argMatchingTo, passedInArg, 0, 0, 0,0, false))
			{//HashMap is a subtype of Map
				stillDirectMatch = false;
				stillBoxing=false;
			}
			else
			{
				if(passedInArg instanceof PrimativeType && TypeCheckUtils.PRIMS_TO_BOXED.get(((PrimativeType)passedInArg).type)[0].equals(argMatchingTo.getPrettyName())){
					stillDirectMatch = false;
				}
				else if(!passedInArg.getPrettyName().equals(argMatchingTo.getPrettyName()))
				{//not a 1:1 match but could be subtype
					stillDirectMatch = false;
					stillBoxing=false;
					//caller must be subtye of callee
					//TypeCheckUtils.checkAssignmentCanBeDone(invoker, op, lhs, rhs, lhsLine, lhsColumn, rhsLine, rhsColumn)
					passedInArg = convertRefTypeToArguprovided(invoker, argMatchingTo, passedInArg);
					
					if(ignoreGenericsOnNOMatch /*&& passedInArg instanceof NamedType && ((NamedType)passedInArg).requiresGenTypeInference*/) {
						if(null == checkSubType(invoker, argMatchingTo, passedInArg, false, 0, 0, 0,0, true, true, false, false, true)){
							return MatchType.NONE;
						}
					}else if(null == checkSubType(invoker, argMatchingTo, passedInArg, 0, 0, 0,0, false)){
						return MatchType.NONE;
					}
					
				}
			}
		}
		
		if(stillDirectMatch)
		{
			return MatchType.DIRECT;
		}
		else if(stillBoxing){
			return MatchType.DIRECT_BOXING;
		}
		else
		{
			return MatchType.VIASUPER;
		}
	}
	
	
	public static Type convertfuncTypetoNamedType(Type tt, Type lhs){
		if(tt instanceof FuncType){
			FuncType asft = (FuncType)tt;
			if(lhs != null && asft.implementSAM != null && asft.implementSAM.getA().equals(lhs)) {
				//rhs is a functype which can map to a SAM type, so check its an exact match of lhs
				return (Type)lhs.copy();
			}
			
			Type ret =  getNamedTypeForFuncType((FuncType)tt);
			ret.setIsPartOfVarargArray(tt.getIsPartOfVarargArray());
			ret.setArrayLevels(tt.getArrayLevels());
			return ret;
		}
		if(tt instanceof PrimativeType && ((PrimativeType)tt).type == PrimativeTypeEnum.LAMBDA){
			Type ret = lam_const.copyTypeSpecific();
			ret.setIsPartOfVarargArray(tt.getIsPartOfVarargArray());
			ret.setArrayLevels(tt.getArrayLevels());
			return ret;
		}
		return tt;
	}
	
	
	public static Type convertTypeToFuncType(Type inp) {
		if(inp instanceof NamedType &&  null != TypeCheckUtils.checkSubType(TypeCheckUtils.errorRaisableSupression, ScopeAndTypeChecker.const_lambda_nt, inp) ) {
			NamedType asNT = (NamedType)inp;
			
			boolean isVoidRetFuncref = asNT.getNamedTypeStr().endsWith("v");
			List<Type> gent = asNT.getGenTypes();
			FuncType ret = null;
			
			if(isVoidRetFuncref) {
				ArrayList<Type> inputs = new ArrayList<Type>(gent);
				ret = new FuncType(inputs, ScopeAndTypeChecker.const_void);
				
				
			}else if(!gent.isEmpty()) { //convert here, also output functype as signature from thing?
				ArrayList<Type> inputs = new ArrayList<Type>(gent);
				Type lastone = inputs.remove(gent.size()-1);
				ret = new FuncType(inputs, lastone);
			}
			
			if(ret != null) {
				FuncLocation.ClassFunctionLocation fcl = new FuncLocation.ClassFunctionLocation(null, inp);
				String bcloc = inp.getBytecodeType();
				fcl.setLambdaOwner(bcloc.substring(1, bcloc.length()-1));
				ret.setLambdaDetails(new TypeAndLocation(ret.copyTypeSpecific(), fcl));
				
				ret.setNullStatus(inp.getNullStatus());
				return ret;
			}
		}
		return inp;
	}
	
	
	private static final NamedType lam_const = new NamedType(new ClassDefJava(com.concurnas.bootstrap.lang.Lambda.class));
	static{
		lam_const.pretendNothingAbstract=true;
	}
	
	public static NamedType getNamedTypeForFuncType(FuncType ft){
		String ftCls;
		
		if(ft.isClassRefType){
			NamedType retla = ScopeAndTypeChecker.const_constructorRef.copyTypeSpecific();
			ArrayList<Type> genTypes = new ArrayList<Type>();
			Type tt = (Type)ft.retType.copy();
			tt.setInOutGenModifier(InoutGenericModifier.OUT);
			genTypes.add(tt);
			retla.setGenTypes(genTypes);
			return retla;
		}
		
		if(null == ft.getLambdaDetails()){
			ftCls="com/concurnas/bootstrap/lang/Lambda$Function" + ft.inputs.size();
		}
		else{
			ftCls = ft.getLambdaDetails().getLocation().getLambdaOwner();
		}
		
		boolean retVoid = false;
		if(ft.retType != null && ft.retType.equals(ScopeAndTypeChecker.const_void)) {
			retVoid=true;
			if(!ftCls.endsWith("v")) {
				ftCls+="v";
			}
		}
		
		NamedType ret = lam_const;
		try {
			Class<?> found = Class.forName(ftCls.replace("/", "."));
			ret = new NamedType(new ClassDefJava(found));
			ArrayList<Type> genTypes = new ArrayList<Type>();
			//
			genTypes.addAll(TypeCheckUtils.boxTypeIfPrimative(ft.inputs, false));
			if(!retVoid) {
				genTypes.add(TypeCheckUtils.boxTypeIfPrimative(ft.retType, false));
			}
			
			ret.setGenTypes(genTypes);
			ret.pretendNothingAbstract=true;
			ret.setNullStatus(ft.getNullStatus());
			
		} catch (ClassNotFoundException e) {//oops

		}
		return ret;
	}
	
	
	
	
	
	public static Type convertRefTypeToArguprovided(ErrorRaiseable invoker, final Type convertTo, Type from){
		//for instance, it's ok to do this: Object: > Integer: when the left hand side is within an argument. e.g. regsiter(Object:); can be called as register(xyz as int:) - normally refs cannot be treated as being subtypes of their refferential type
		if(TypeCheckUtils.hasRefLevels(convertTo) && TypeCheckUtils.hasRefLevels(from)){
			Type upcastTo = TypeCheckUtils.getRefType(convertTo);
			Type castee = TypeCheckUtils.getRefType(from);
			Type ret = TypeCheckUtils.checkSubType(invoker, upcastTo, castee, 0, 0, 0, 0);
			if(null != ret){
				//TODO: doesnt work with map refs? MapRef[X,Y]
				NamedType froma = (NamedType)from.copy();
				froma.overrideRefType(ret);
				from = froma;
			}
		}
		
		return from;
	}
	
	public static boolean isArgumentListSame(ArrayList<Type> a, ArrayList<Type> b)
	{
		if(a == null)
		{
			return b == null;
		}
		else if(b == null)
		{
			return false;
		}
		else if(a.size() == b.size())
		{
			for(int n =0; n< a.size(); n++)
			{
				Type aa = a.get(n);
				Type bb = b.get(n);
				if (aa instanceof GenericType && bb instanceof GenericType)
				{
					continue;
				}
				
				if((aa == null && bb != null) || (aa != null && bb == null) ){
					return false;
				}
				
				if(aa == null && bb == null){
					return true;
				}
				
				aa = TypeCheckUtils.convertTypeToFuncType(aa);
				bb = TypeCheckUtils.convertTypeToFuncType(bb);
				
				/*if(!aa.equals(a.get(n))) {
					System.err.println(String.format("%s -> %s", a.get(n), aa));
				}
				
				if(!bb.equals(b.get(n))) {
					System.err.println(String.format("%s -> %s", b.get(n), bb));
				}*/
				
				//if(!TypeCheckUtils.boxTypeIfPrimative(aa, false).equals(TypeCheckUtils.boxTypeIfPrimative(bb, false)))
				if(!aa.equals(bb))
				{
					return false;
				}
			}
			return true;
		}
		
		return false;
	}
	
	public static HashSet<GenericType> findUnboundedGenerics(Type thing) {
		HashSet<GenericType> ret = new HashSet<GenericType>();
		
		dofindUnboundedGenerics(thing, ret);
		
		return ret;
	}
	

	private static void dofindUnboundedGenerics(Type thing, HashSet<GenericType> ret) {
		if(thing instanceof GenericType){
			if(!thing.toString().equals("?")){
				ret.add((GenericType)thing);
			}
		}
		else if(thing instanceof NamedType){
			NamedType asNamed = (NamedType)thing;
			for(Type t : asNamed.getGenTypes()){
				dofindUnboundedGenerics(t, ret);
			}
		}
		else if(thing instanceof FuncType){
			FuncType asFunc = (FuncType)thing;
			for(Type t : asFunc.inputs){
				dofindUnboundedGenerics(t, ret);
			}
			dofindUnboundedGenerics(asFunc.retType, ret);
		}
	}
	
	public static Map<Type, Type> extractGenericBindings(NamedType asNamed) {
		/*
		 class Parent<To>{
			public class MyClass<T>(~a T){
			}
			fun getKidmaker3() = MyClass&("hi") //implicit ret
		}//so parent will be correctly qualified
		 */
		Map<Type, Type> ret = new HashMap<Type, Type>();
		
		HashMap<GenericType, Type> stuff = asNamed.getFromClassGenericToQualifiedType();
		for(GenericType gt : stuff.keySet()){
			ret.put(gt, stuff.get(gt));
		}
		
		
		return ret;
	}
	
	public static boolean isVarNull(Type inp){
		if(null == inp) {
			return true;
		}
		
		if(inp.hasArrayLevels()){
			return false;
		}
		
		if(inp instanceof VarNull){
			return true;
		}
		if(inp instanceof NamedType){
			return ((NamedType)inp).orignallyfromVarNull;
		}
		return false;
	}
	
	public static HashSet<String> localGenNames(ArrayList<GenericType> localGenerics){
		HashSet<String> ret = new HashSet<String>();
		
		if(null != localGenerics){
			for(GenericType gt : localGenerics){
				ret.add(gt.name);
			}
		}
		
		return ret;
	}
	
	public static boolean typeContainsAGenericType(Type testical, HashSet<String> localGenerics){
		if(testical instanceof GenericType){
			return true;
		}
		else if(testical instanceof NamedType){
			NamedType asNamed = (NamedType)testical;
			
			if(null != localGenerics && localGenerics.contains( asNamed.getJavaSourceTypeNoArray() )){
				return true;
			}
			
			for(Type tt : asNamed.getGenTypes()){
				if(typeContainsAGenericType(tt, localGenerics)){
					return true;
				}
			}
		}
		else if(testical instanceof FuncType){
			FuncType asFuncType = (FuncType)testical;
			for(Type tt : asFuncType.getInputs()){
				if(typeContainsAGenericType(tt, localGenerics)){
					return true;
				}
			}
			
			return typeContainsAGenericType(asFuncType.retType, localGenerics);
		}
		return false;
	}

/*	private static Map<Type, Type> attemptGenericBinding(ErrorRaiseable invoker, Type input, TypeargsPasse) {
		
		ArrayList<Type> inputs = new ArrayList<Type>();
		inputs.add(input);
		ArrayList<Type> argsPassed = new ArrayList<Type>();
		argsPassed.add(argsPasse);
		
		return attemptGenericBinding(invoker, inputs, argsPassed);
	}*/
	public static Map<Type, Type> attemptGenericBinding(ErrorRaiseable invoker, List<Type> inputs, List<Type> argsPassed) {
		return attemptGenericBinding(invoker, inputs, argsPassed, true);
	}
	
	private static Map<Type, Type> attemptGenericBinding(ErrorRaiseable invoker, List<Type> inputs, List<Type> argsPassed, boolean top) {
		//e.g. [T](t ArrayList[T]) passed ArrayList[String] : therefore we can infer T to be a String when its passed to this constructor
		Map<Type, Type> genTypeToBinding = new HashMap<Type, Type>();
		
		int isize = inputs.size();
		if(isize == argsPassed.size()){
			Map<Type, HashSet<Type>> genQualmany = new DefaultMap<Type, HashSet<Type>>(
					new Function1<Type, HashSet<Type>>(null) {
						@Override
						public HashSet<Type> apply(Type m) {return new HashSet<Type>();}

						@Override
						public Object[] signature() {return null;}
						});
			
			for(int n = 0; n < isize; n++){
				Type input = inputs.get(n);
				Type arg = argsPassed.get(n);
				
				if(input instanceof FuncType) {
					input = TypeCheckUtils.converToNamedType(input);
				}
				
				if(input instanceof GenericType){// T - nice so we can splice in whatever is bounded
					NamedType upperBound = input.getOrigonalGenericTypeUpperBound();
					
					boolean match = false;
					if(null == upperBound){
						match =  true;
					}else{//if the upper bound itself maps to the generic type we are trying to quality, then here we must color it in
						Map<Type, Type> paramToType = new HashMap<Type, Type>();
						paramToType.put(input, arg);
						upperBound = (NamedType) GenericTypeUtils.mapFuncTypeGenericsToOtherGenerics(upperBound.copyTypeSpecific(), paramToType, false);
						
						match = null != TypeCheckUtils.checkSubType(invoker, upperBound, arg, 0, 0, 0, 0);
					}
					
					if(match){ //TODO: and check lower bound
						GenericType inputI = ((GenericType) input).copyTypeSpecific();
						Type argI = (Type)arg.copy();
						if(inputI.hasArrayLevels()){
							inputI.setArrayLevels(0);
							argI.setArrayLevels(0);
						}
						
						if(argI.getInOutGenModifier() != null) {
							argI.setInOutGenModifier(null);
						}
						
						genQualmany.get(inputI).add(argI);
					}
				}
				else if(input instanceof NamedType){//input is not Generic, named type most likely
					//get arg into comparable format
					NamedType inputAsNamed = (NamedType)input;
					ClassDef matchclass = inputAsNamed.getSetClassDef();

					NamedType found = null;
					if(arg instanceof FuncType) {
						FuncType asFt = (FuncType)arg;
						
						if(asFt.implementSAM != null) {
							if(asFt.implementSAM.getA().equals(inputAsNamed)) {
								found = (NamedType)TypeCheckUtils.convertfuncTypetoNamedType(arg, null);
							}
						}else if(asFt.getLambdaDetails() != null) {
							found = (NamedType)TypeCheckUtils.convertfuncTypetoNamedType(arg, null);
						}
					}
					
					if(found == null && arg instanceof NamedType){
						NamedType asNamed = (NamedType)arg;
						if(null == asNamed.getSetClassDef()){
							continue;//if we've not figured out what the type is skip over this
						}
						
						LinkedList<NamedType> items = new LinkedList<NamedType>();
						items.add(asNamed);
						while(!items.isEmpty()){
							NamedType toTry = items.removeFirst();
							if(toTry.getSetClassDef().equals(matchclass)){
								found=toTry;
								break;
							}
							items.addAll(toTry.getResolvedTraitsAsNamed());
							NamedType sup = toTry.getSetClassDef().getSuperAsNamedType(0,0);
							if(sup != null){
								items.add(sup);
							}
						}
					}
					
					if(found != null && !found.getIgnoreForLocalGenericInference() && !hasReturnedUnqualifiedLocalGeneric(found)){//we're matching the type, now can extract generics...
						Map<Type, Type> insideFound = attemptGenericBinding(invoker, inputAsNamed.getGenTypes(), found.getGenTypes(), false);
						for(Type key : insideFound.keySet()){
							genQualmany.get(key).add(insideFound.get(key));
						}
					}
					
				}
			}
			
			for(Type key : genQualmany.keySet()){
				HashSet<Type> couldbe = genQualmany.get(key);
				//note that there has to be only one choice with generic type qualifications, we cannot map to more than one :(
				//e.g. Object as supertype of String and Integer - no can do!
				genTypeToBinding.put(key, couldbe.size() > 1?null : TypeCheckUtils.boxTypeIfPrimative(couldbe.iterator().next(), false) );
				//genTypeToBinding.put(key, TypeCheckUtils.getMoreGeneric(invoker, 0, 0, new ArrayList<Type>(couldbe), null));
			}
			
		}
		
		if(top && genTypeToBinding.values().contains(null)){//only do at top so as preserve the iterative nature of finding of this bug
			return new HashMap<Type, Type>();
		}
		
		return genTypeToBinding; 
	}
	

	public static Set<GenericType> getNestedGenericTypes(Type what) {
		Set<GenericType> ret = new HashSet<GenericType>();
		getNestedGenericTypes(what, ret);
		return ret;
	}
	private static void getNestedGenericTypes(Type what, Set<GenericType> ret) {
		if(what instanceof NamedType) {
			NamedType asNamed = (NamedType)what;
			for(Type inp : asNamed.getGenericTypeElements()) {
				getNestedGenericTypes(inp, ret);
			}
		}else if(what instanceof FuncType) {
			FuncType asFuncType = (FuncType)what;
			getNestedGenericTypes(asFuncType.retType, ret);

			for(Type inp : asFuncType.inputs) {
				getNestedGenericTypes(inp, ret);
			}
			
		}else if(what instanceof GenericType) {
			ret.add((GenericType)what);
		}
	}
	
	public static boolean hasReturnedUnqualifiedLocalGeneric(Type what) {
		if(what instanceof NamedType) {
			NamedType asNamed = (NamedType)what;
			for(Type inp : asNamed.getGenericTypeElements()) {
				if(hasReturnedUnqualifiedLocalGeneric(inp)) {
					return true;
				}
			}
		}else if(what instanceof FuncType) {
			FuncType asFuncType = (FuncType)what;
			if(hasReturnedUnqualifiedLocalGeneric(asFuncType.retType)) {
				return true;
			}

			for(Type inp : asFuncType.inputs) {
				if(hasReturnedUnqualifiedLocalGeneric(inp)) {
					return true;
				}
			}
			
		}else if(what instanceof GenericType) {
			GenericType asGenericType = (GenericType)what;
			if(asGenericType.isReturnedUnqualifiedLocalGeneric) {
				return true;
			}
		}
		return false;
	}
	
	public static Pair<FuncType, FuncType> getMostSpecificFunctionForChoicesFT(ErrorRaiseable invoker, ErrorRaiseable errorRaisableSupression, Set<FuncType> keySet, List<Type> argsWanted, ArrayList<Pair<String, Type>> namessMap, String name, int line, int col, boolean refUpArgsIfNeeded, ScopeAndTypeChecker satc, boolean ignoreGenericsOnNOMatch) {
		
		HashSet<TypeAndLocation> matchingFuncNames = new HashSet<TypeAndLocation>();
		for(FuncType k : keySet){
			matchingFuncNames.add(new TypeAndLocation(k,  new StaticFuncLocation( k.origin ==null?null: new NamedType(line, col, k.origin)  )));//TODO: hmm, null here is dirty, should info be thrown away?
		}
		FuncType ret = getMostSpecificFunctionForChoices(invoker, errorRaisableSupression, matchingFuncNames, argsWanted, namessMap, name, line, col, refUpArgsIfNeeded, satc, false);
		
		if(null == ret){
			if(argsWanted == null){
				return new Pair<FuncType, FuncType>(null, null);//cannot be more than one if no args - always ambigious
			}
			int sz = argsWanted.size();
			ArrayList<Integer> lockedRefs = new ArrayList<Integer>(sz);
			for(int n=0; n < sz; n++){
				if(TypeCheckUtils.hasRefLevelsAndIsLocked(argsWanted.get(n))){
					lockedRefs.add(n);
				}
			}
			
			if(!lockedRefs.isEmpty()){//it has locked ref levels, so lets try the iterations of the non locked versions
				HashSet<FuncType> potentials = new HashSet<FuncType>();
				List<Boolean[]> perms = PermutorWithCache.permutationsWithoutNullCase(lockedRefs.size());
				for(Boolean[] permset : perms){
					List<Type> argsCopy = Utils.cloneArgs(argsWanted);
					//1 match great, more than one fail
					for(int n=0; n < permset.length; n++){
						if(permset[n] != null){
							TypeCheckUtils.unlockAllNestedRefs(argsCopy.get(lockedRefs.get(n)));//in place unlock
							
							/*
							 int idx = lockedRefs.get(n);
							Type tryMe = TypeCheckUtils.getRefType(argsCopy.get(idx));
							tryMe = TypeCheckUtils.unboxTypeIfBoxed(tryMe);
							argsCopy.set(idx, tryMe);//in place unlock 
							 
							 */
						}
					}
					
					FuncType potential = getMostSpecificFunctionForChoices(invoker, errorRaisableSupression, matchingFuncNames, argsCopy, namessMap, name, line, col, refUpArgsIfNeeded, satc, false);
					if(null != potential){
						potentials.add(potential);
					}
					
					if(potentials.size() > 1){
						return new Pair<FuncType, FuncType>(null, null);//more than one match == ambigious, this is not permitted
					}
				}
				if(!potentials.isEmpty()){//will only contain one...
					return new Pair<FuncType, FuncType>(potentials.iterator().next(), null);
				}
			}
		}
		
		if(ret == null && ignoreGenericsOnNOMatch) {
			FuncType nextbest = getMostSpecificFunctionForChoices(invoker, errorRaisableSupression, matchingFuncNames, argsWanted, namessMap, name, line, col, refUpArgsIfNeeded, satc, true);
			return new Pair<FuncType, FuncType>(null, nextbest);
		}
		
		return new Pair<FuncType, FuncType>(ret, null);
		
	}
	
	public static <XXX> boolean typeListEquals(List<XXX> a, List<XXX> b){
		if(a==b){
			return true;
		}
		int ars = a.size();
		if(ars == b.size()){
			for(int n=0; n < ars; n++){
				XXX left = a.get(n);
				XXX right = b.get(n);
				if(left == null || right == null){
					//null is not a valid type
					return false;
				}
				
				if(!left.equals(right)){
					return false;
				}
			}
			return true;
		}

		return false;
	}
	
	public static FuncType getMostSpecificFunctionForChoices(ErrorRaiseable invoker, ErrorRaiseable errorRaisableSupression, 
			Set<TypeAndLocation> matchingFuncNames, List<Type> argsWanted, ArrayList<Pair<String, Type>> namessMap, 
			String funcName, int line, int col, boolean refUpArgsIfNeeded, ScopeAndTypeChecker satc, boolean ignoreGenericsOnNOMatch)
	{
		if(matchingFuncNames == null || matchingFuncNames.isEmpty())
		{
			invoker.raiseError(line, col, String.format("Unable to find method with matching name: %s", filterOutActorCall(funcName)));
			return null;
		}
		else
		{
			Set<FuncType> matchingFuncNamesArgs = new HashSet<FuncType>();
			
			for(TypeAndLocation funcDeflac : matchingFuncNames)
			{//narrow down to matching args...
				FuncType funcType = (FuncType)funcDeflac.getType();
				
				int argSz = funcType.getInputs()==null?0:funcType.getInputs().size();//upper bound
				int defaultArgCount = 0;
				
				boolean hasVararg = false;
				
				if(funcType.inputs.stream().allMatch(a -> a.getPointer() == 0) && funcType.origonatingFuncDef != null){
					FuncParams fps = funcType.origonatingFuncDef.params;
					if(fps != null && !fps.params.isEmpty()){
						ArrayList<FuncParam> fpspars = fps.params;
						int sz = fpspars.size();
						for(int n=0; n < sz; n++){
							FuncParam fp = fpspars.get(n);
							if(null != fp.defaultValue){
								defaultArgCount++;
							}
							if(fp.isVararg || (sz-1 == n && fp.getTaggedType().hasArrayLevels()) ){
								hasVararg = true;
							}
						}
					}
				}
				
				if(!hasVararg && !funcType.inputs.isEmpty()){
					hasVararg = funcType.inputs.get(funcType.inputs.size()-1).hasArrayLevels();
				}
				
				funcType.hasVarargs = hasVararg;
								
				int lowerBound = argSz - defaultArgCount + (hasVararg?-1:0);//args needed at a minimum (rest having defaults)
				
				List<Type> adjustedArgs = TypeCheckUtils.mapFunctionParameterNamesToNewArguments(satc, funcType, argsWanted, namessMap,0, true, false);
				if(null == argsWanted){
					matchingFuncNamesArgs.add(funcType);
				}
				else{
					if(!typeListEquals( adjustedArgs , argsWanted)){//oh, we made changes
						funcType.hackCalledArgumets = adjustedArgs;//yeah, a hack
					}
					
					int perfectMatchIncNamed = argsWanted.size()+ (namessMap==null?0:namessMap.size()); 
					
					if(hasVararg?true:adjustedArgs.size() == argSz && (hasVararg?true:argSz>=perfectMatchIncNamed) && perfectMatchIncNamed >= lowerBound)
					{
						matchingFuncNamesArgs.add(funcType);
					}
				}
				
				
			}
			
			if(matchingFuncNamesArgs.isEmpty())
			{
				invoker.raiseError(line, col, String.format("Unable to find method with matching number of arguments with name: %s", filterOutActorCall(funcName)));
				return null;
			}
			else
			{
				DefaultMap<MatchType, ArrayList<FuncType>> matchTypeToCandidates = new DefaultMap<MatchType, ArrayList<FuncType>>(
						new Function1<MatchType, ArrayList<FuncType>>(null) {
							@Override
							public ArrayList<FuncType> apply(MatchType m) {return new ArrayList<FuncType>();}

							@Override
							public Object[] signature() {return null;}
							});
				
				boolean vectorizationOccured = false;
				HashMap<FuncType, ArrayList<Pair<AnonLambdaDef, LambdaDef>>> matchToAnonLambdaQuali = new HashMap<FuncType, ArrayList<Pair<AnonLambdaDef, LambdaDef>>>(); 
				for(FuncType potential : matchingFuncNamesArgs)
				{//narrow it down to just the ones which have all the arguments matching either directly or via supertypes...
					ArrayList<Type> potentialMatchArgs = potential.getInputs();
					
					{
						ArrayList<Type> newpotentialMatchArgs = new ArrayList<Type>(potentialMatchArgs.size());
						boolean outParamUsedAsInput = false;
						boolean changesMade = false;
						for(Type arg : potentialMatchArgs){
							InoutGenericModifier inoutgen = arg.getInOutGenModifier();
							if(inoutgen != null ){
								if(inoutgen == InoutGenericModifier.OUT){
									outParamUsedAsInput=true;//uh oh!
								}else if(inoutgen == InoutGenericModifier.IN){
									arg = (Type)arg.copy();//arg.getOrigonalGenericTypeUpperBound();
									arg.setInOutGenModifier(null);
								}
								changesMade=true;
							}
							newpotentialMatchArgs.add(arg);
						}
						
						Type retParam = potential.retType;
						InoutGenericModifier inoutgen = retParam==null?null:retParam.getInOutGenModifier();
						if(inoutgen != null ){
							changesMade=true;
							if(inoutgen == InoutGenericModifier.OUT){
								retParam = (Type)retParam.copy();
								retParam.setInOutGenModifier(null);
							}else if(inoutgen == InoutGenericModifier.IN){
								//retParam = retParam.getOrigonalGenericTypeUpperBound();
								//retParam=new ModuleType("in parameter cannot be returned from a method");
							}
						}
						
						if(changesMade){
							potential = potential.copyTypeSpecific();
							potential.inputs = newpotentialMatchArgs;
							potentialMatchArgs = newpotentialMatchArgs;
							potential.retType = retParam;
						}
						
						if(outParamUsedAsInput){
							matchTypeToCandidates.get(MatchType.OUTPARAMASINPUT).add(potential);
						}
					}
					
					List<Type> calledargs = potential.hackCalledArgumets!=null?potential.hackCalledArgumets:argsWanted;
					
					boolean hasVectorizedArgs = validateVectorizedArgs(invoker, line, col, calledargs);

					ArrayList<Thruple<List<Type>, Fourple<ArrayList<Pair<Boolean, NullStatus>>, ArrayList<Integer>, Integer, Type>, Type>> argvariants;
					if(hasVectorizedArgs){//ArrayList<Thruple<List<Type>, Boolean, Type>> extractArgVariants
						argvariants = extractArgVariants(errorRaisableSupression, calledargs, potential.hasBeenVectorized != null ? potential.hasBeenVectorized.getD() : potential.retType , potential.hasBeenVectorized != null, potential);
					}else{
						argvariants = new ArrayList<Thruple<List<Type>, Fourple<ArrayList<Pair<Boolean, NullStatus>>, ArrayList<Integer>, Integer, Type>, Type>>();
						argvariants.add(new Thruple<List<Type>, Fourple<ArrayList<Pair<Boolean, NullStatus>>, ArrayList<Integer>, Integer, Type>, Type>(calledargs, null, null));
					}
					//extract variants of input argument until we arrive at 
					
					MatchType match = calledargs==null?MatchType.DIRECT:null;
					ArrayList<Pair<AnonLambdaDef, LambdaDef>> qualifiedAnonLambdas = null;
					while((match == null || match==MatchType.NONE) && !argvariants.isEmpty()){
						
						
						Thruple<List<Type>, Fourple<ArrayList<Pair<Boolean, NullStatus>>, ArrayList<Integer>, Integer, Type>, Type> argvariant = argvariants.remove(0);
						
						List<Type> calledargsx = new ArrayList<Type>(argvariant.getA());
						
						//qualify anon lambda def argument here: e.g. mylist.forEach(a -> a*2);//etc
						boolean failsAnonLambdaQuali = false;
						int n=0;
						qualifiedAnonLambdas = new ArrayList<Pair<AnonLambdaDef, LambdaDef>>();
						
						Boolean arsRemappedVararg = null;
						Type invararg = null;
						int m=0;
						int potentialMatchArgsSize = potentialMatchArgs.size();
						
						for(Type carg : calledargsx) {
							NamedType fromList = null;
							if(TypeCheckUtils.isList(errorRaisableSupression, carg, false)) {
								ArrayList<Type> gens = ((NamedType)carg).getGenericTypeElements();
								if(gens != null && !gens.isEmpty()) {
									fromList = (NamedType)carg;
									Type genArg = fromList.getGenericTypeElements().get(0);
									if(genArg instanceof FuncType) {
										carg = genArg;
									}
								}
								
								
							}
							
							if(arsRemappedVararg == null){
								List<?> ffargs = TypeCheckUtils.mapFunctionParameterNamesToNewArguments(satc, potential, calledargsx, namessMap, 0, true, true);
								if(ffargs != calledargsx && ffargs.size() != calledargsx.size()) {//vararged and consumed inputs...
									arsRemappedVararg = true;
								}else {
									arsRemappedVararg=false;
								}
							}
									
															
							Type toqualifyth=null;
							if(arsRemappedVararg) {//got vararged so lets map the args as appropierate
								//calledargsx - [def () int, def () int, int]
								while(toqualifyth == null) {
									if(invararg == null) {//see if we are in varrag
										Type lhsc = potentialMatchArgs.get(m++);
										
										if(lhsc instanceof NamedType && null != TypeCheckUtils.checkSubType(errorRaisableSupression, ScopeAndTypeChecker.getLazyNT(), lhsc)) {
											lhsc = ((NamedType)lhsc).getGenTypes().get(0);
										}
										
										if(lhsc.getArrayLevels() == carg.getArrayLevels() + 1) {
											invararg = (Type)lhsc.copy();
											invararg.setArrayLevels(invararg.getArrayLevels()-1);
											toqualifyth = invararg;
										}else {//not in a vararg
											toqualifyth = lhsc;
										}
									}else {//invararg, great!
										if(lhsIsSamType(satc, invararg, carg, false)) {
											toqualifyth = invararg;
										}else if(null != TypeCheckUtils.checkAssignmentCanBeDone(errorRaisableSupression, invararg, carg)) {//still in vararg
											toqualifyth = invararg;
										}else {//broken out of vararg
											if(lhsIsSamType(satc, invararg, carg, true)) {//{34} may be convertable here to SAM type
												//if it matches next type then we ignore this else use the invararg
												if(m <= potentialMatchArgsSize-1) {
													Type lhsc = potentialMatchArgs.get(m++);
													
													if(lhsc instanceof NamedType && null != TypeCheckUtils.checkSubType(errorRaisableSupression, ScopeAndTypeChecker.getLazyNT(), lhsc)) {
														lhsc = ((NamedType)lhsc).getGenTypes().get(0);
													}
													
													if(lhsc.getArrayLevels() == carg.getArrayLevels() + 1) {
														lhsc = (Type)lhsc.copy();
														lhsc.setArrayLevels(lhsc.getArrayLevels()-1);
													}
													if(null != TypeCheckUtils.checkAssignmentCanBeDone(errorRaisableSupression, lhsc, carg)) {
														toqualifyth = invararg;
														continue;
													}
												}
												
												toqualifyth = invararg;
												
											}else {
												invararg = null;
											}
										}
									}
								}
							}else {

								if(potentialMatchArgsSize <= n) {
									break;//fail
								}
								
								toqualifyth = potentialMatchArgs.get(n);
							}
							
							
							if(carg instanceof FuncType) {
								FuncType asFT = (FuncType)carg;
								if(asFT.anonLambdaSources != null) {
									
									if(null != fromList) {
										toqualifyth = ((NamedType)toqualifyth).getGenericTypeElements().get(0);
									}

									boolean process = true;
									if(asFT.anonLambdaSources.size() == 1) {//if acting on lambda def only do this if potentially mapepd to SAM type
										if(asFT.anonLambdaSources.get(0) instanceof LambdaDef) {
											process = toqualifyth instanceof NamedType;
										}
									}
									
									if(process) {
										for(Expression expr : asFT.anonLambdaSources) {
											AnonLambdaDefOrLambdaDef anonLamDef = (AnonLambdaDefOrLambdaDef)expr;
											LambdaDef lamAssigned = Utils.inferAnonLambda(satc, (Node)anonLamDef, toqualifyth);
											if(anonLamDef instanceof AnonLambdaDef) {
												((AnonLambdaDef)anonLamDef).astRedirect = null;//clear in case of other mappings
											}
											
											if(null == lamAssigned /*&& anonLamDef instanceof AnonLambdaDef*/) {//no match
												failsAnonLambdaQuali = true;
												break;
											}
											if(anonLamDef instanceof AnonLambdaDef) {
												qualifiedAnonLambdas.add(new Pair<AnonLambdaDef, LambdaDef>((AnonLambdaDef)anonLamDef, lamAssigned));
											}
											
										}
										if(failsAnonLambdaQuali) {//no match
											break;
										}else {

											if(null != fromList) {
												fromList = (NamedType)fromList.copy();
												fromList.setGenType(0, (Type)toqualifyth.copy());
												calledargsx.set(n, fromList);
											}else {
												calledargsx.set(n, (Type)toqualifyth.copy());
											}
										}
									}
									
								}
							}
							
							n++;
						}
						
						
						
						if(failsAnonLambdaQuali) {
							match=MatchType.NONE;
							continue;
						}
						
						
						match = TypeCheckUtils.checkFunctionInvokationArgumentMatch(satc, errorRaisableSupression, calledargsx, potentialMatchArgs, refUpArgsIfNeeded, ignoreGenericsOnNOMatch);
						
						//arg match
						Fourple<ArrayList<Pair<Boolean, NullStatus>>, ArrayList<Integer>, Integer, Type> degreeAndWhichVectorized = argvariant.getB();
						
						if(degreeAndWhichVectorized != null){
							potential = potential.copyTypeSpecific();
							//potential.retType = argvariant.getC();
							potential.hasBeenVectorized=degreeAndWhichVectorized;
							vectorizationOccured = true;
						}
						
					}
					
					if(match != MatchType.NONE){
						
						if(qualifiedAnonLambdas != null && !qualifiedAnonLambdas.isEmpty()) {
							//match to anon lambda qualification
							matchToAnonLambdaQuali.put(potential, qualifiedAnonLambdas);
						}
						
						
						if(potential.hackCalledArgumets!=null && match == MatchType.DIRECT){
							
							if(potential.hasVarargs){
								match = potential.varargsRequireUpCast?MatchType.DIRECT_VARARG_VIASUPER:MatchType.DIRECT_VARARG;
							}
							else if(potential.varargsRequireUpCast){
								match = MatchType.VIASUPER;
							}
						}
						ArrayList<FuncType> candniForMt = matchTypeToCandidates.get(match);
						if(vectorizationOccured && potential.hasBeenVectorized != null){
							/* So we can process the following in a non ambigious way
							   ar2 = [1 2 ; 3 4]
							   def lefunc(a int[]) = '2d: ' + a
							   def lefunc(a int) = '1d: ' + a
							   lefunc(ar2^) <- we want to match against int[] as this is less ambigious way to extract the vectorization
							 */
							int pdepth = potential.hasBeenVectorized.getA().size();
							boolean addit = true;
							for(FuncType alreadyMatched : candniForMt){
								if(alreadyMatched.hasBeenVectorized != null){
									int adepth = alreadyMatched.hasBeenVectorized.getA().size();
									//we want the one with the smallest jump
									if(pdepth < adepth){//the new one is smaller
										candniForMt.remove(alreadyMatched);
									}else{//alreaday have a smaller one
										addit=false;
									}
									break;
								}
							}
							
							if(addit){
								candniForMt.add(potential);
							}
							
						}else{
							candniForMt.add(potential);
						}
					}
				}
				
				matchTypeToCandidates = filterVectNonVectDupes(matchTypeToCandidates);
				
				ArrayList<FuncType> directMatches = matchTypeToCandidates.get(MatchType.DIRECT);
				ArrayList<FuncType> directMatchesVararg = matchTypeToCandidates.get(MatchType.DIRECT_VARARG);
				ArrayList<FuncType> directMatchesViaBoxing = matchTypeToCandidates.get(MatchType.DIRECT_BOXING);
				
				HashSet<FuncType> outParamsAsInputsERRROR = new HashSet<FuncType>(matchTypeToCandidates.get(MatchType.OUTPARAMASINPUT));//cant call if in here
				
				if(!directMatches.isEmpty())
				{//horray direct matches!
					if(directMatches.size() == 1){
						return complainIfOutParamsUsedAsInputs(invoker, line, col, funcName, outParamsAsInputsERRROR, directMatches.get(0), matchToAnonLambdaQuali);
					}
					else
					{//shouldnt be possible...
						boolean someWithnamessMap =  false;
						if(null != namessMap && !namessMap.isEmpty()){
							for(FuncType ft : directMatches){
								someWithnamessMap = ft.hackCalledArgumets != null;
								if(someWithnamessMap){break;}//no need to carry on checking
							}
						}
						
						invoker.raiseError(line, col, String.format("Ambiguous method detected '%s'. More than one direct match made%s", funcName, someWithnamessMap?" - due to ambiguous named parameter mapping":""));
						return null;
					}
				}
				else if(!directMatchesVararg.isEmpty())//lasy cut paste
				{//horray direct matches!
					if(directMatchesVararg.size() == 1){
						return complainIfOutParamsUsedAsInputs(invoker, line, col, funcName, outParamsAsInputsERRROR, directMatchesVararg.get(0), matchToAnonLambdaQuali);
					}
					else
					{//shouldnt be possible...
						boolean someWithnamessMap =  false;
						if(null != namessMap && !namessMap.isEmpty()){
							for(FuncType ft : directMatchesVararg){
								someWithnamessMap = ft.hackCalledArgumets != null;
								if(someWithnamessMap){break;}//no need to carry on checking
							}
						}
						
						invoker.raiseError(line, col, String.format("Ambiguous method detected '%s'. More than one direct vararg match made%s", funcName, someWithnamessMap?" - due to ambiguous named parameter mapping":""));
						return null;
					}
				} 
				else if(!directMatchesViaBoxing.isEmpty())
				{//horray direct matches!
					if(directMatchesViaBoxing.size() == 1){
						return complainIfOutParamsUsedAsInputs(invoker, line, col, funcName, outParamsAsInputsERRROR, directMatchesViaBoxing.get(0), matchToAnonLambdaQuali);
					}
					else
					{//shouldnt be possible...
						invoker.raiseError(line, col, String.format("Ambiguous method detected '%s'. More than one direct via boxing match made", funcName));
						return null;
					}
				}
				else
				{
					//indirect matches?
					//DIRECT_VARARG_VIASUPER
					for(MatchType mt : new MatchType[]{MatchType.VIASUPER, MatchType.DIRECT_VARARG_VIASUPER}){
						ArrayList<FuncType> indirectMatchs = matchTypeToCandidates.get(mt);
						if(!indirectMatchs.isEmpty())
						{
							if(indirectMatchs.size()==1){
								return complainIfOutParamsUsedAsInputs(invoker, line, col, funcName, outParamsAsInputsERRROR, indirectMatchs.get(0), matchToAnonLambdaQuali);
							}
							else
							{
								//hard work check ambigiousness... else get one that is least general
								
								ArrayList<Boolean> refPrio = new ArrayList<Boolean>();
								for(Type arg: argsWanted){
									refPrio.add(TypeCheckUtils.hasRefLevelsAndIsLocked(arg));
								}//only use this directly if there are no changes made due to named param mapping
								//ArrayList<FuncType>
								
								FuncType mostSpecifc = TypeCheckUtils.getMostSpecificMatch(errorRaisableSupression, indirectMatchs, refPrio);
								if(null == mostSpecifc)
								{
									invoker.raiseError(line, col, String.format("Ambiguous method detected '%s'. More than one indirect match made", funcName));
									return null;
								}
								else
								{
									return complainIfOutParamsUsedAsInputs(invoker, line, col, funcName, outParamsAsInputsERRROR, mostSpecifc, matchToAnonLambdaQuali);
								}
								
							}
						}
					}
				
					invoker.raiseError(line, col, String.format("Unable to find method with matching name: %s and arguments %s", filterOutActorCall(funcName), formatArgs(argsWanted, namessMap)));
					return null;
				}
			}
		}
	}
	
	private static DefaultMap<MatchType, ArrayList<FuncType>> filterVectNonVectDupes(DefaultMap<MatchType, ArrayList<FuncType>> matchcandis){
		for(MatchType casz : matchcandis.keySet()) {
			ArrayList<FuncType> matches = matchcandis.get(casz);//always risky when playing with matches
			int sz = matches.size();
			if(sz > 1) {
				ArrayList<FuncType> newMatches = new ArrayList<FuncType>(sz);
				for(FuncType ft:matches) {
					if(ft.hasBeenVectorized == null) {
						newMatches.add(ft);
					}
				}
				matchcandis.put(casz, newMatches);
			}
		}
		
		return matchcandis;
	}
	
	
	public static Type applyVectStruct(List<Pair<Boolean, NullStatus>> vectStruct, Type rootType) {
		Type ret = (Type)rootType.copy();
		vectStruct = new ArrayList<Pair<Boolean, NullStatus>>(vectStruct);
		Collections.reverse(vectStruct);
		
		int n=0;
		for(Pair<Boolean, NullStatus> isArrayx : vectStruct) {
			boolean isArray = isArrayx.getA();
			NullStatus nsSet = isArrayx.getB();
			if(isArray) {
				ret.setArrayLevels(ret.getArrayLevels() + 1);
				if(nsSet != null) {
					ret.setNullStatusAtArrayLevel(nsSet);
				}
			}else {
				NamedType lo = (NamedType)(vectStruct.size()-1 == n?ScopeAndTypeChecker.const_arrayList  : ScopeAndTypeChecker.const_list ).copy();
				ArrayList<Type> genTypes = new ArrayList<Type>(1);
				Type tt = TypeCheckUtils.boxTypeIfPrimative(ret, false);
				if(nsSet != null) {
					tt.setNullStatus(nsSet);
				}
				genTypes.add(tt);//dont box int[][]
				lo.setGenTypes(genTypes);
				ret = lo;
			}
			n++;
		}
		
		return ret;
	}
	
/*	private static ArrayList<Thruple<List<Type>, Fourple<ArrayList<Boolean>, ArrayList<Integer>, Integer, Type>, Type>> extractArgVariants(ErrorRaiseable errorRaisableSupression, List<Type> calledargsinp, Type retType, boolean hasAlreadyBeenVectorized){//TOOD: add list support
		ArrayList<Thruple<List<Type>, Fourple<ArrayList<Boolean>, ArrayList<Integer>, Integer, Type>, Type>> argvariants = new ArrayList<Thruple<List<Type>, Fourple<ArrayList<Boolean>, ArrayList<Integer>, Integer, Type>, Type>>();
		int sz = calledargsinp.size();
		
		OrderedHashSet<Integer> vectroizedArg = new OrderedHashSet<Integer>();
		Integer returnSelfArg = null;
		for(int n=0; n < sz; n++){
			Type item = calledargsinp.get(n); 
			if(item.isVectorized()){
				if(item.getVectorized() == Vectorization.SELF){
					returnSelfArg = n;
				}
				vectroizedArg.add(n);
			}
		}
		ArrayList<Integer> vectroizedArgAsARray = new ArrayList<Integer>(vectroizedArg);
		
		List<Type> calledargs = Utils.cloneArgs(calledargsinp);
		boolean unvectorized = true;
		while(unvectorized){
			List<Type> variant = new ArrayList<Type>(sz);
			int veclevels = 0;
			for(int n=0; n < sz; n++){
				Type item = calledargs.get(n);
				veclevels = 0;
				if(unvectorized && vectroizedArg.contains(n)){
					ArrayList<Boolean> struct = getVectorizedStructure(errorRaisableSupression, item);
					
					if(!struct.isEmpty()){
						item.setVectorized(null);
						boolean isArray = struct.remove(0);
						if(isArray) {
							veclevels = item.getArrayLevels()-1;
							item.setArrayLevels(veclevels);
						}else {
							item = ((NamedType)item).getGenericTypeElements().get(0);
						}
						calledargs.set(n, item);
					}else{
						unvectorized = false;
						break;
					}
					
				}
				variant.add((Type)item.copy());
			}
			
			if(unvectorized){
				Type retTypeoverride;
				if(returnSelfArg != null){
					retTypeoverride = variant.get(returnSelfArg);
				}else
				if(retType == null){
					retType = new VarNull(0,0);
				}
				
				retTypeoverride = retType;
				if(!hasAlreadyBeenVectorized) {
					retTypeoverride = extractVectType(retTypeoverride);
					int retTypeARLevels = getVectorizedStructure(errorRaisableSupression, retType).size();
						int calledArgARLevels = getVectorizedStructure(errorRaisableSupression, calledargsinp.get(vectroizedArg.iterator().next())).size();
						retTypeoverride.setArrayLevels(retTypeARLevels + calledArgARLevels);
					 
					retTypeoverride = TypeCheckUtils.applyVectStruct(getVectorizedStructure(errorRaisableSupression, retType), retTypeoverride);
					retTypeoverride = TypeCheckUtils.applyVectStruct(getVectorizedStructure(errorRaisableSupression, calledargsinp.get(vectroizedArg.iterator().next())), retTypeoverride);
				}
				
				Type firstVectArg = variant.get(vectroizedArgAsARray.get(0));
				int funcInputArrayLevelsExpected = getVectorizedStructure(errorRaisableSupression, firstVectArg).size();
				
				ArrayList<Boolean> vectStrict = getVectorizedStructure(errorRaisableSupression, retTypeoverride);
				vectStrict.subList(veclevels, vectStrict.size());
				
				Fourple<ArrayList<Boolean>, ArrayList<Integer>, Integer, Type> vectData = new Fourple<ArrayList<Boolean>, ArrayList<Integer>, Integer, Type>(vectStrict, vectroizedArgAsARray, funcInputArrayLevelsExpected, retTypeoverride);
				
				
				Thruple<List<Type>, Fourple<ArrayList<Boolean>, ArrayList<Integer>, Integer, Type>, Type> entry = new Thruple<List<Type>, Fourple<ArrayList<Boolean>, ArrayList<Integer>, Integer, Type>, Type>(variant, vectData, retTypeoverride);
				argvariants.add(entry);
			}
		}
		
		//if self reference, then return
		return argvariants;
	}*/
	
	private static ArrayList<Thruple<List<Type>, Fourple<ArrayList<Pair<Boolean, NullStatus>>, ArrayList<Integer>, Integer, Type>, Type>> extractArgVariants(ErrorRaiseable errorRaisableSupression, List<Type> calledargsinp, Type retType, boolean hasAlreadyBeenVectorized, FuncType calledOn){//TOOD: add list support
		ArrayList<Thruple<List<Type>, Fourple<ArrayList<Pair<Boolean, NullStatus>>, ArrayList<Integer>, Integer, Type>, Type>> argvariants = new ArrayList<Thruple<List<Type>, Fourple<ArrayList<Pair<Boolean, NullStatus>>, ArrayList<Integer>, Integer, Type>, Type>>();
		int sz = calledargsinp.size();
		
		ArrayList<Type> wantedArgs = calledOn.inputs;
		
		OrderedHashSet<Integer> vectroizedArg = new OrderedHashSet<Integer>();
		Integer returnSelfArg = null;
		for(int n=0; n < sz; n++){
			Type item = calledargsinp.get(n); 
			if(null != item && item.isVectorized()){
				if(item.getVectorized() == Vectorization.SELF){
					returnSelfArg = n;
				}
				vectroizedArg.add(n);
			}
		}
		ArrayList<Integer> vectroizedArgAsARray = new ArrayList<Integer>(vectroizedArg);
		
		List<Type> calledargs = Utils.cloneArgs(calledargsinp);
		boolean unvectorized = true;
		while(unvectorized){
			List<Type> variant = new ArrayList<Type>(sz);
			int veclevels = 0;
			for(int n=0; n < sz; n++){
				Type item = calledargs.get(n);
				veclevels = 0;
				if(unvectorized && vectroizedArg.contains(n)){
					ArrayList<Pair<Boolean, NullStatus>> struct = getVectorizedStructure(errorRaisableSupression, item);
					
					if(!struct.isEmpty()){
						item.setVectorized(null);
						boolean isArray = struct.remove(0).getA();
						if(isArray) {
							veclevels = item.getArrayLevels()-1;
							item.setArrayLevels(veclevels);
						}else {
							item = ((NamedType)item).getGenericTypeElements().get(0);
						}
						
						calledargs.set(n, item);
						
						if( null == checkSubType(errorRaisableSupression, wantedArgs.get(n), item) ) {
							variant = null;
						}
						
					}else{
						unvectorized = false;
						break;
					}
				}
				
				if(null != variant) {
					variant.add(item == null?null:(Type)item.copy());
				}
			}
			
			if(unvectorized && null != variant){
				int firxtVecArgNo = vectroizedArgAsARray.get(0);
				Type firstVectArg = variant.get(firxtVecArgNo);
				int funcInputArrayLevelsExpected = getVectorizedStructure(errorRaisableSupression, firstVectArg).size();
				
				if(retType == null){
					retType = new VarNull(0,0);
				}
				
				Type orgifirstTypeeVect = calledargsinp.get(firxtVecArgNo);
				orgifirstTypeeVect = (Type)orgifirstTypeeVect.copy();
				orgifirstTypeeVect.setVectorized(null);
				ArrayList<Pair<Boolean, NullStatus>> vectStrict = getVectorizedStructure(errorRaisableSupression, orgifirstTypeeVect);
				ArrayList<Pair<Boolean, NullStatus>> vectStrictOrig = getVectorizedStructure(errorRaisableSupression, calledargs.get(firxtVecArgNo));
				
				vectStrict = new ArrayList<Pair<Boolean, NullStatus>>(vectStrict.subList(vectStrictOrig.size(), vectStrict.size()));
				
				
				
				Type retTypeoverride = retType;
				if(!hasAlreadyBeenVectorized) {
					retTypeoverride = TypeCheckUtils.applyVectStruct(vectStrict, retTypeoverride);
				}
				
				Fourple<ArrayList<Pair<Boolean, NullStatus>>, ArrayList<Integer>, Integer, Type> vectData = new Fourple<ArrayList<Pair<Boolean, NullStatus>>, ArrayList<Integer>, Integer, Type>(vectStrict, vectroizedArgAsARray, funcInputArrayLevelsExpected, retTypeoverride);
				
					
				Thruple<List<Type>, Fourple<ArrayList<Pair<Boolean, NullStatus>>, ArrayList<Integer>, Integer, Type>, Type> entry = new Thruple<List<Type>, Fourple<ArrayList<Pair<Boolean, NullStatus>>, ArrayList<Integer>, Integer, Type>, Type>(variant, vectData, retTypeoverride);
				argvariants.add(entry);
			}
		}
		
		//if self reference, then return
		return argvariants;
	}
	
	public final static Type extractVectType(Type inps) {
		inps = (Type)inps.copy();
		inps.setVectorized(null);
		
		while(true) {
			if(inps.hasArrayLevels()) {
				inps.setArrayLevels(0);
			}else if(TypeCheckUtils.isList(errorRaisableSupression, inps, false) && ((NamedType)inps).getGenericTypeElements() != null && !((NamedType)inps).getGenericTypeElements().isEmpty()){
				inps = ((NamedType)inps).getGenericTypeElements().get(0);
			}else {
				break;
			}
		}
		
		return inps;
	}
		
	public static ArrayList<Pair<Boolean, NullStatus>> getVectorizedStructure(ErrorRaiseable errorRaisableSupression, Type type){
		ArrayList<Pair<Boolean, NullStatus>> elems = new ArrayList<Pair<Boolean, NullStatus>>();
		while(type != null) {
			boolean noMatch=true;
			int ars = type.getArrayLevels();
			List<NullStatus> nullstatus = type.getNullStatusAtArrayLevel();
			if(ars > 0) {
				for(int m=0; m < ars; m++) {
					elems.add(new Pair<Boolean, NullStatus>(true, nullstatus.get(ars-1-m)));//ns is extracted in revrese
				}
				type = (Type)type.copy();
				type.setArrayLevels(0);
			}
			
			if(TypeCheckUtils.isList(errorRaisableSupression, type, false)){
				noMatch=false;
				ArrayList<Type> gens = ((NamedType)type).getGenericTypeElements();
				
				if(gens.isEmpty()) {
					noMatch=true;
					elems.add(new Pair<Boolean, NullStatus>(false, NullStatus.NOTNULL));
				}else {
					type = gens.get(0);
					elems.add(new Pair<Boolean, NullStatus>(false, type.getNullStatus()));
				}
			}
			
			if(noMatch) {
				type = null;
			}
		}
		
		return elems;
	}
	
	private static boolean validateVectorizedArgs(ErrorRaiseable invoker, int line, int col, List<Type> calledargs) {
		if(calledargs == null){
			return false;
		}
		
		List<Type> vectorized = calledargs.stream().filter(a -> a!=null && a.isVectorized()).collect(Collectors.toList());
		//TOOD: add list support
		if(!vectorized.isEmpty()){
			ArrayList<Pair<Boolean, NullStatus>> vectStruct = getVectorizedStructure(invoker.getErrorRaiseableSupression(), vectorized.get(0));
			int levels = vectStruct.size();//vectorized.get(0).getArrayLevels();
			if(levels == 0){//no array levels
				return false;//will have already been flagged as an error, e.g. sin("hi".)
			}
			
			if( vectorized.size() > 1){//more than one, ensure only one is self and all have same src level degree
				if(vectorized.stream().filter(a -> a.getVectorized() == Vectorization.SELF).count() > 1){
					invoker.raiseError(line, col, "Only one argument in vectorized call may be of '^^' form");
					return false;//give up at this point
				}else{
					int sz = vectorized.size();
					for(int n=1; n < sz; n++){
						ArrayList<Pair<Boolean, NullStatus>> mvectStruct = getVectorizedStructure(invoker.getErrorRaiseableSupression(), vectorized.get(n));
						int me = mvectStruct.size();
						if(me != levels){
							invoker.raiseError(line, col, String.format("All vectorized arguments in call must be of the same dimention. First item is: %s but item: %s is of: %s degree", levels, n+1, me));
							return false;//give up at this point
						}
						/*else if(!mvectStruct.equals(vectStruct)){
							invoker.raiseError(line, col,"All vectorized arguments in call must be of the same dimention and structure. List and array combination differs");
							return false;//give up at this point
						}*/
					}
					
				}
			}
			
			return true;
		}
		
		return false;
	}

	private static FuncType complainIfOutParamsUsedAsInputs(ErrorRaiseable invoker, int line, int col, String funcName, HashSet<FuncType> outParamsAsInputsERRROR, FuncType leFunc, HashMap<FuncType, ArrayList<Pair<AnonLambdaDef, LambdaDef>>> matchToAnonLambdaQuali){
		if(null != leFunc) {
			if(!outParamsAsInputsERRROR.isEmpty() && outParamsAsInputsERRROR.contains( leFunc)){//uh oh
				int n=1;
				ArrayList<Type> inputtypes = leFunc.getInputs();
				ArrayList<Integer> errargs = new  ArrayList<Integer>();
				for(Type input : inputtypes){
					if(input.getInOutGenModifier() == InoutGenericModifier.OUT){
						errargs.add(n);//TODO: add argument names here instead of numbers where avaailable?
					}
					n++;
				}
				
				invoker.raiseError(line, col, String.format("Method: %s is not callable as generic arguments: %s have been qualified as out types and so cannot be used as method inputs", funcName, Utils.justJoin(errargs, "and")));
				return null;
			}
			
			if(leFunc.retType != null && leFunc.retType.getInOutGenModifier() == InoutGenericModifier.IN) {
				invoker.raiseError(line, col, String.format("Method: %s is not callable as generic return type: %s has been qualified as in type and so cannot be used as method output", funcName, leFunc.retType));
				return null;
			}
		}
		
		
		
		if(!matchToAnonLambdaQuali.isEmpty()) {
			ArrayList<Pair<AnonLambdaDef, LambdaDef>> qualifiedAnonLambdas = matchToAnonLambdaQuali.get(leFunc);//qualify the 'winner'
			for(Pair<AnonLambdaDef, LambdaDef> qq : qualifiedAnonLambdas) {
				qq.getA().astRedirect = qq.getB();
			}
			
		}
		
		return leFunc;
	}
	
	public static String formatArgs(List<Type> argsWanted, ArrayList<Pair<String, Type>> namessMap){
		if(argsWanted == null){
			return "";
		}
		
		
		StringBuilder sb = new StringBuilder("(");
		int sz =namessMap==null?0:namessMap.size();
		for(int naa = 0; naa < argsWanted.size(); naa++){
			//argsWanted
			sb.append(argsWanted.get(naa));
			if(naa != argsWanted.size()-1 || sz>0){
				sb.append(", ");
			}
		}
		
		for(int na=0; na < sz; na++){// Tuple<String, Type> item: namessMap){
			Pair<String, Type> item = namessMap.get(na);
			sb.append(item.getA() + " = " + item.getB());
			if(na != namessMap.size()-1){
				sb.append(", ");
			}
		}
		sb.append(")");
		return sb.toString();
	}
	
	public static FuncType getMostSpecificMatch(ErrorRaiseable invoker, ArrayList<FuncType> toCheck, ArrayList<Boolean> prioritizeRef)
	{//returns null to indicate ambigiuty
		return GetMostSpecificAndTestAmbig.go(invoker, toCheck, prioritizeRef);
	}
	
	
	private static FuncTypeMany getCombinations(ErrorRaiseable invoker, int lhsLine, int lhsColumn, List<FuncType> inputs)
	{
		int len = inputs.get(0).getInputs().size();
		int inputLen = inputs.size();
		ArrayList<HashSet<Type>> colInputTypes = new ArrayList<HashSet<Type>>(len);
		HashSet<Type> ret = new HashSet<Type>();
		
		for(int n = 0; n < len; n++)
		{
			colInputTypes.add(new HashSet<Type>(inputLen));
		}
		
		for(FuncType f: inputs)
		{
			if( len != f.getInputs().size())
			{
				invoker.raiseError(lhsLine, lhsColumn, "All method type references must have the same number of arguments");
				return null;
			}
			
			for(int n=0; n < f.getInputs().size(); n++)
			{
				Type t = f.getInputs().get(n);
				HashSet<Type> col = colInputTypes.get(n);
				col.add(t);
			}
			ret.add(f.retType);
		}
		
		boolean isVoidRet = voidCount(ret);
		
		if(isVoidRet && ret.size() > 1  )
		{
			invoker.raiseError(lhsLine, lhsColumn, "All method types must either return something or void");
			return null;
		}
		
		//now u have colmum arguments, and return type...
		//get the generic combinations of those!
		
		ArrayList<List<Type>> manyParams = new ArrayList<List<Type>>(len+1);
		
		for(HashSet<Type> col : colInputTypes)
		{
			if(voidCount(col))
			{//should be impossible...
				invoker.raiseError(lhsLine, lhsColumn, "Void is not a valid function type input argument type");
				return null;
			}
			else
			{
				List<Type> items = new ArrayList<Type>(col.size());
				Map<Type, Pair<Integer,Integer>> offenders = new HashMap<Type, Pair<Integer,Integer>>();
				for(Type c : col)
				{
					items.add(c);
					offenders.put(c, new Pair<Integer,Integer>(lhsLine, lhsColumn)); //TODO: get correct line
				}
				Type genForCol = getMoreGeneric(invoker, null, lhsLine, lhsColumn, items, offenders);
				if(genForCol instanceof NamedTypeMany)
				{
					NamedTypeMany man = (NamedTypeMany)genForCol;
					manyParams.add(man.getGenTypes());
				}
				else
				{
					ArrayList<Type> gens = new ArrayList<Type>();
					gens.add(genForCol);
					manyParams.add(gens);//just one
				}
			}
		}
		
		if(isVoidRet)
		{
			ArrayList<Type> gens = new ArrayList<Type>();
			gens.add(new PrimativeType(PrimativeTypeEnum.VOID));
			manyParams.add(gens);//just one
		}
		else
		{
			List<Type> items = new ArrayList<Type>(ret.size());
			Map<Type, Pair<Integer,Integer>> offenders = new HashMap<Type, Pair<Integer,Integer>>();
			for(Type c : ret)
			{
				items.add(c);
				offenders.put(c, new Pair<Integer,Integer>(lhsLine, lhsColumn)); //TODO: get correct line
			}
			Type genForRet = getMoreGeneric(invoker, null, lhsLine, lhsColumn, items, offenders);
			if(genForRet instanceof NamedTypeMany)
			{
				NamedTypeMany man = (NamedTypeMany)genForRet;
				manyParams.add(new ArrayList<Type>(man.getMany()));
			}
			else
			{
				ArrayList<Type> gens = new ArrayList<Type>();
				gens.add(genForRet);
				manyParams.add(gens);//just one
			}
		}
		
		//now finally we have a list of arguments, from which we can derive the approperiate combinations
		//note that last argument is always the return type
		
		List<ArrayList<Type>> choices = FuncTypePermutor.getTheFunctTypePermutations(manyParams);
		
		ArrayList<FuncType> funcs = new ArrayList<FuncType>();
		
		for(ArrayList<Type> choice : choices)
		{
			Type retu = choice.remove(choice.size()-1);
			FuncType ftnew = new FuncType(choice, retu); 
			StaticFuncLocation loc = new StaticFuncLocation(null);
			loc.setLambda(true);
			loc.setLambdaOwner("com/concurnas/bootstrap/lang/Lambda$Function" + len + (retu.equals(ScopeAndTypeChecker.const_void)?"v":""));
			ftnew.setLambdaDetails( new TypeAndLocation(ftnew, loc));
			funcs.add(ftnew);
		}
		if(funcs.isEmpty()){
			return null;
		}
		
		return new FuncTypeMany(funcs);
	}
	
	
	private static boolean voidCount(HashSet<Type> input)
	{
		for(Type t : input)
		{
			if(t instanceof PrimativeType)
			{
				PrimativeType pt = (PrimativeType)t;
				if (pt.type == PrimativeTypeEnum.VOID)
				{
					return true;
				}
			}
		}
		return false;
	}
	
	
	public static boolean checkNumerical(ErrorRaiseable invoker, Type lhs, int lhsLine, int lhsColumn, boolean restrictToIntegral)
	{
		if(null == lhs)
		{
			return true;
		}
		
		Type unboxedLhs = unboxTypeIfBoxed(lhs);
		if(unboxedLhs instanceof PrimativeType)//all numerical except void and lamdba
		{
			PrimativeType pt= (PrimativeType)unboxedLhs;
			return restrictToIntegral?pt.type.isIntegral():pt.type.isNumerical();
		}
		
		return false;
	}
	
	
	private static class SortTypeByStrRep implements Comparator{

	    public int compare(Object o1, Object o2) {
	    	Type p1 = (Type) o1;
	    	Type p2 = (Type) o2;
	        return p1.toString().compareTo(p2.toString());
	    }
	}
	
	private final static SortTypeByStrRep sortTypeByStrRepObj = new SortTypeByStrRep();
	
	private static boolean isAllVarNull(HashSet<Type> xxx){
		for(Type x : xxx){
			if(!(x instanceof VarNull)){
				return false;
			}
		}
		return true;
	}

	public static Type getMoreGeneric(ErrorRaiseable invoker, ScopeAndTypeChecker satc,  int line, int col, List<Type> types, Map<Type, Pair<Integer,Integer>> offenders){
		return getMoreGeneric(invoker, satc, line, col, types,  offenders,  false);
	}
	
	/**
	 * May return more than one type e.g. a = [new A(), new B()]// where A < B, could be B or Object, could be!, or an interface of which both A and B implement
	 * A Many
	 */
	public static Type getMoreGeneric(ErrorRaiseable invoker, ScopeAndTypeChecker satc, int line, int col, List<Type> types, Map<Type, Pair<Integer,Integer>> offenders, boolean onlyInstantiables)
	{
		boolean anyNullable = types.stream().anyMatch(a -> a != null && a.getNullStatus()  == NullStatus.NULLABLE);
		
		Type ret = getMoreGenericDo(invoker, col, line, types, offenders, onlyInstantiables);
		//deal with any VarNull instances still left (actually just the VarNull! instances which need special tweaking)
		//also maybe xxx Integer[] = [null, null] if true else [12, null]
		
		if(anyNullable && null != ret) {
			ret = (Type)ret.copy();
			ret.setNullStatus(NullStatus.NULLABLE);
		}
		
		int availLevels = TypeCheckUtils.getRefLevels(ret);
		if(availLevels >0){
			boolean locked = true;
			for(Type t : types){
				int refLevelsWant = TypeCheckUtils.getRefLevels(t);
				if(refLevelsWant>0 && refLevelsWant==availLevels){
					if(TypeCheckUtils.hasRefLevelsAndIsNotLocked(t)){//not locked
						locked=false;
					}
					
					//another special case, null: is type of any object:
					Type refTypeWant = TypeCheckUtils.getRefType(t);
					Type refTypeAvail = TypeCheckUtils.getRefType(ret);
					
					if(refTypeWant instanceof VarNull && !refTypeWant.hasArrayLevels() && (refTypeAvail instanceof NamedType) ) //what about if the avaiable type is an array (this is treated like an object 
					{//sepcial case, null: is a type of any object:
						//we also mutate the type inplace
						mutatleRefType(refLevelsWant, (NamedType)t, (NamedType)refTypeAvail);
					}
				}
				else{
					locked=false;
				}
			}
			
			if(locked){
				((NamedType)ret).setLockedAsRef(true);
			}
		}
		
		if(ret == null){
			return ScopeAndTypeChecker.const_object;
		}
		
		if(ret instanceof NamedTypeMany){
			NamedTypeMany asNamedTypeMany = (NamedTypeMany)ret;
			if(null != satc) {
				HashSet<NamedType> individuals = asNamedTypeMany.getMany();
				//check accessable from call point (e.g. selected class is not private etc
				ret=null;
				for(NamedType option : individuals) {
					ClassDef root = ScopeAndTypeChecker.const_object_CD;
					if(!satc.currentlyInClassDef.isEmpty()) {
						root = satc.currentlyInClassDef.peek();
					}
					
					ClassDef setcls = option.getSetClassDef();
					
					boolean acc = setcls != null && satc.isAccesible(setcls.accessModifier, option, root, true, satc.justPackageName);//fixme?
					if(acc) {
						ret = option;
						break;
					}
				}
				
				if(null == ret) {
					return ScopeAndTypeChecker.const_object;
				}else {
					return ret;
				}
				//ret = ((NamedTypeMany)ret).getSelf();
			}else {
				ret = ((NamedTypeMany)ret).getSelf();
			}
		}
		
		if(ret instanceof FuncTypeMany){
			ret = ((FuncTypeMany)ret).getSelf();
		}
		
		
		if(ret instanceof FuncType) {
			FuncType retFT = (FuncType)ret;
			HashSet<Pair<NamedType, TypeAndLocation>> implementSAMs = new HashSet<Pair<NamedType, TypeAndLocation>>();
			for(Type orig : types) {
				if(orig instanceof FuncType) {
					FuncType asFT = (FuncType)orig;
					implementSAMs.add(asFT.implementSAM);
					if(asFT.anonLambdaSources != null) {
						if(retFT.anonLambdaSources == null) {
							retFT.anonLambdaSources = new ArrayList<>(asFT.anonLambdaSources);
						}else {
							retFT.anonLambdaSources.addAll(asFT.anonLambdaSources);
						}
					}
				}
			}
			if(implementSAMs.size() == 1) {
				retFT.implementSAM  = implementSAMs.iterator().next();
			}
			
			
			if(((FuncType) ret).implementSAM != null) {
				return ((FuncType) ret).implementSAM.getA();
			}
			
		}
		
		
		return ret;
	}
	
	private static boolean allRefsAndThoseThatArntArNulls(List<Integer> refLevels, HashSet<Type> typeUni){
		//e.g. ref levels [1, 1] => [null, String] - this is a match
		//[1, 0] => [null, String] - this is not a match
		//[1, 1,1] => [null, Double, String] - this is not a match
		if((new HashSet<Integer>(refLevels)).size()==1){
			 HashSet<Type> typeUniNoNull = new HashSet<Type>();
			 for(Type t : typeUni){
				 if(!(t instanceof VarNull)){
					 typeUniNoNull.add(t);
				 }
			 }
			 return typeUniNoNull.size() == 1;
		}
		return false;
	}
	
	/**
	 * May return more than one type e.g. a = [new A(), new B()]// where A < B, could be B or Object, could be!, or an interface of which both A and B implement
	 * A Many
	 */
	public static Type getMoreGenericDo(ErrorRaiseable invoker,  int col, int line, List<Type> types, Map<Type, Pair<Integer,Integer>> offenders, boolean onlyInstantiables)
	{
		if(types.size() == 1){
			return types.get(0);
		}
		
		//filter out refs, add back in later
		int maxRefLength = 0;
		ArrayList<ArrayList<NamedType>> refChoices = new ArrayList<ArrayList<NamedType>>();
		
		List<Type> origtypes = types;
		
		List<Type> typesRefFiltered = new ArrayList<Type>(types.size());
		
		List<Integer> refLevels = new ArrayList<Integer>(types.size());
		
		int refCnt = 0;//note: not actual ref cnt
		boolean hasRefArrays = false;
		for(Type t: types){
			if(t == null) {
				continue;
			}
			if(TypeCheckUtils.hasRefLevelsAndIsLocked(t)){//only for things that are refed
				ArrayList<NamedType> refComponents = TypeCheckUtils.getRefTypes(t);
				refChoices.add(refComponents);
				int meRef = refComponents.size();
				if(meRef > maxRefLength){
					maxRefLength = meRef;
				}
				refLevels.add(meRef);
				refCnt++;
			}
			else{
				if(TypeCheckUtils.isLocalArray(t)){
					hasRefArrays=true;
				}
				
				refLevels.add(0);
			}
			typesRefFiltered.add( TypeCheckUtils.getRefType(t) );//operate independant of refs
		}
		
		if(refCnt > 0){
			
			List<Type> typesRefFilteredBoxAll = new ArrayList<Type>(typesRefFiltered.size());
			
			for(Type t : typesRefFiltered){//therefore [8!, 8], gets converted to int:[]
				typesRefFilteredBoxAll.add(TypeCheckUtils.boxTypeIfPrimativeAndSetUpperBound(t));
			}
			
			/*HashSet<Type> typeUni = new HashSet<Type>(typesRefFilteredBoxAll);
			if(typeUni.size() > 1 && !isAllVarNull(typeUni) && !allRefsAndThoseThatArntArNulls(refLevels, typeUni)){
				//[Float:, Integer:] - have to return Object not Number: 
				return objectNT;
			}*/
			//why do we do the above?
		}
		
		types = typesRefFiltered;
		List<Type> notVarNullTypes = new ArrayList<Type>(types.size());
		
		int primEntries = 0;
		HashSet<Type> arrayTypes = new HashSet<Type>();
		boolean hasNulls = false;
		int minNullArCount = 1000;
		for (Type typ : types)
		{
			Pair<Integer, Integer> both = offenders==null?null:offenders.get(typ);
			int lhsLine = both==null?line:both.getA();
			int lhsColumn = both==null?col:both.getB();
			
			if(!isValidType(invoker, typ, lhsLine, lhsColumn))//will raise error if approperiate.
			{//TODO: isValidType needs to be added to all other method calls as well
				return null;
			}
			
			if(!(typ instanceof VarNull ))
			{
				notVarNullTypes.add(typ);
			}
			else{
				int arC = typ.getArrayLevels();
				if(arC < minNullArCount){
					minNullArCount = arC;
				}
						
				hasNulls = true;
			}
			
			if(typ instanceof PrimativeType)
			{
				primEntries++;
			}
			
			if(typ.hasArrayLevels() && !(typ instanceof VarNull ))
			{
				arrayTypes.add(typ);
			}
		}
		
		if(primEntries>0 && arrayTypes.size() > 1)
		{//e.g. [[1,2,3], [4., 6.] => Object[]
			return const_obj;
			/*
			
			StringBuilder typeStr = new StringBuilder();
			List<Type> arTypes = new ArrayList<Type>(arrayTypes);
			Collections.sort(arTypes, sortTypeByStrRepObj);
			
			int sz = arTypes.size();
			for(int n=0; n < sz; n++)
			{
				typeStr.append(""+arTypes.get(n));
				if(n != sz-1 )
				{
					typeStr.append(", ");
				}
			}
			
			invoker.raiseError(col, row, "Unable to generalize array types: " + typeStr);
			return null;*/
		}
		
		
		if(notVarNullTypes.isEmpty())
		{
			//return objectNT;
			VarNull vn = new VarNull(col, line);
			vn.setArrayLevels(minNullArCount);
			
			if(maxRefLength>0){//has refs, so entwine all the origonal VarNull's such that when main one type gets hacked in the other will follow
				Type asRef = TypeCheckUtils.makeRef(vn, maxRefLength);
				vn.entwineOnRefMutateSet = origtypes;  //MHA: oh dear this is ugly! on the mutate operation, remember to tag these guys too!
				//so now, when you later on mutate the type back in place
				//and also fix the varnull to have the correct ref levels
				return asRef;
			}
			else{
				return vn;
			}
		}
		
		
		/* primary type (but may also be of higher types too [bit of an edge case not much practical value])
		    < Z     : I
		 A  < B < C : S
		    < W < Y : I
		    
		    < W     : I
		 AL < C     : S
		 
		 B < W      : I
		 
		 (A, AL, B) -> W 
		 (A, B)     -> W
		 (A, AL)    -> C or W 
		 (A, C)     -> C
		 
		 [stop when you get to object
		 
		 have to compare all at the same time...
		 
		 ->
		 //all prime type, ret common prim type
		 //some prim, some object, upcast prim 
		  * -> if some are not boxed prim -> object
		  * else find common boxed prim
		//if all object
		 * -> find common dude among them
		//if any functype -> objec
		 //if all functype -> most generic functype
		 */
		
		/*
		Object[] a = {2, new float[]{4,5} };
		Integer[] av = {new int[]{1,2}, new int[]{4,5} };
		int[][] asv = {new int[]{1,2}, new int[]{4,5} };
		int[][] asvd = {{1,2}, {4,5} };
		Integer[][] aasvd = {{1,2}, {4,5} };
		Integer[][] ll = asvd;
		Object b = a;
		Object z  = new int[]{4};
		*/
		/*
		 primative
		reference (could be generic)
		functype
		
		any of the above could be array types
		 */
		
		Type ret;

		int arrayLevels = notVarNullTypes.get(0).getArrayLevels();
		
		List<NullStatus> arrayLevelNullStatusToSet = null;
		if(arrayLevels > 0) {
			for(Type elem : notVarNullTypes) {
				List<NullStatus> items = elem.getNullStatusAtArrayLevel();
				if(items.size() == arrayLevels) {
					if(null == arrayLevelNullStatusToSet) {
						arrayLevelNullStatusToSet = items;
					}else {
						//take most conservative: if either is true then true
						for(int n =0; n < arrayLevels; n++ ) {
							arrayLevelNullStatusToSet.set(n, (arrayLevelNullStatusToSet.get(n)==NullStatus.NULLABLE || items.get(n) ==NullStatus.NULLABLE) ? NullStatus.NULLABLE:NullStatus.NOTNULL);
						}
					}
				}
			}
		}
		
		if(arrayLevelsMisMatch(notVarNullTypes)){
			ret = const_obj;
		}
		else if(isAllPrimative(notVarNullTypes))
		{
			PrimativeType prim = null;
			int pntLevel = 0;
			if(notVarNullTypes.stream().anyMatch(a -> a.getPointer() > 0)) {//must all be of the same level
				Set<Integer> pntLEvels = notVarNullTypes.stream().map(a -> a.getPointer()).collect(Collectors.toSet());
				if(pntLEvels.size() > 1) {
					invoker.raiseError(line, col, "Cannot combine differing degrees of pointer type");
				}
				pntLevel = pntLEvels.iterator().next();
			}
			
			GPUVarQualifier memspace = null;
			if(notVarNullTypes.stream().anyMatch(a -> a.getGpuMemSpace() != null)) {//must all be of the same level
				Set<GPUVarQualifier> pntLEvels = notVarNullTypes.stream().map(a -> a.getGpuMemSpace()).collect(Collectors.toSet());
				if(pntLEvels.size() > 1) {
					invoker.raiseError(line, col, "Cannot combine variables from differing gpu memory spaces");
				}
				memspace = pntLEvels.iterator().next();
			}
			
			
			for(Type t: notVarNullTypes)
			{
				if(arrayLevels!= t.getArrayLevels() )
				{
					return const_obj;//TODO: remove this, redudnant as par above?
				}
				
				if(!(t instanceof PrimativeType))
				{
					Pair<Integer, Integer> both = offenders==null?null:offenders.get(t);
					int lhsLine = both==null?line:both.getA();
					int lhsColumn = both==null?col:both.getB();
					
					invoker.raiseError(lhsLine, lhsColumn, "Primative type expected instead of: " + t);
					ret = null;
				}
				else
				{
					PrimativeType pt = (PrimativeType)t;
					
					if(null == prim)
					{
						prim = pt; 
					}
					else
					{
						prim = bestPrim(prim, pt);
						if(prim == null){//boolean and something else, return Object since this is the only thing we can do
							return const_obj;
						}
					}
				}
			}
			ret = prim;
			ret.setArrayLevels(arrayLevels);
			
			if(pntLevel > 0) {
				ret.setPointer(pntLevel);
			}
			if(memspace != null) {
				ret.setGpuMemSpace(memspace);
			}
		}
		else if(maxRefLength == 0 && isAllPrimativeMaybeBoxed(notVarNullTypes))
		{//now all objects
			ret = null;
			if(notVarNullTypes.stream().anyMatch(a -> !(a instanceof PrimativeType))){
				List<Type> unboxed = notVarNullTypes.stream().map(a -> TypeCheckUtils.unboxTypeIfBoxed(a)).collect(Collectors.toList());
				Type ofPrim = getMoreGenericDo(invoker,   col,  line,  unboxed, offenders, onlyInstantiables);
				//if(ofPrim != null) {
					return TypeCheckUtils.boxTypeIfPrimative(ofPrim, false);
				//}
			}else {
				List<Type> objtypes = upcastPrimativesMany(notVarNullTypes);
				return LowestCommonAncestor.getLCA(objtypes, onlyInstantiables  && (refCnt>0 || hasRefArrays) && maxRefLength==0);
			}
		}
		else if(areAnyFuncTypes(notVarNullTypes))
		{
			if(areAllFuncTypes(notVarNullTypes))
			{
				// something = [(int, String) String {},  (int, Object) String {}]; // [type could be either: (int, String) String {} or (int, Object) String {}, or something else more stupid...]
				List<FuncType> funcz = new ArrayList<FuncType>(notVarNullTypes.size());
				for(Type t : notVarNullTypes)
				{
					funcz.add((FuncType)t);
				}
				ret = getCombinations(invoker, line, col, funcz);
				if(null != ret){
					ret.setArrayLevels(arrayLevels);
				}
				else{
					return null;
				}
			}
			else
			{
				ret = new NamedType(line, col, new ClassDefJava(java.lang.Object.class));
			}
		}
		else if(areAllSameGeneric(notVarNullTypes))
		{
			return notVarNullTypes.get(0);
		}
		else
		{//must all be objects so find the most common object (when upcast the primatives):
			List<Type> objtypes = upcastPrimativesMany(notVarNullTypes);
			if(objtypes.size()>1){
				ret = LowestCommonAncestor.getLCA(objtypes, onlyInstantiables && (refCnt>0 || hasRefArrays) && maxRefLength==0);
			}
			else{//skip if just one choice
				ret = objtypes.get(0);
			}
			
			ret.setArrayLevels(arrayLevels);
		}
		
		if(null != ret){
			if(hasNulls && ret instanceof PrimativeType && !ret.hasArrayLevels()){
				//[2., 4.], null => double[2] not Double[2], but[2., null] => Double[]
				ret = boxTypeIfPrimative(ret, true);
			}
			
			if(null != arrayLevelNullStatusToSet) {
				ret.setNullStatusAtArrayLevel(arrayLevelNullStatusToSet);
			}
		}
		
		if(maxRefLength > 0){
			//ref it back up again
			
			//HERE - add most generic ref sequenceing
			
			ret = buildRefTypeFromChoices(refChoices, maxRefLength, ret);
			
			/*for(int n=0; n< maxRefLength; n++){
				ret = new NamedType(0,0, ret);
			}*/
		}
		ret.setLine(line);
		ret.setColumn(col);
		
		return ret;
	}
	
	private static NamedType buildRefTypeFromChoices(ArrayList<ArrayList<NamedType>> refChoices, int lenWanted, Type tagIn){
		ArrayList<ArrayList<NamedType>> aligned = new ArrayList<ArrayList<NamedType>>();
		for(ArrayList<NamedType> xx : refChoices){
			if(lenWanted == xx.size()){
				aligned.add(xx);
			}
		}
		
		ArrayList<NamedType> foundStructure = new ArrayList<NamedType>();
		for(int n =0; n < lenWanted; n++){
			ArrayList<Type> atSlot = new ArrayList<Type>();
			for(int m = 0; m < aligned.size(); m++){// ArrayList<NamedType> xx : aligned){
				
				NamedType detail = aligned.get(m).get(n);
				
				NamedType forLevel = new NamedType(detail.getLine(), detail.getColumn(), detail.getSetClassDef());
				
				//NamedType forLevel = detail.copyTypeSpecific();
				//forLevel.setGenTypes(new ArrayList<Type>());
				
				atSlot.add(forLevel);
			}
			
			NamedType nt = (NamedType)LowestCommonAncestor.getLCA(atSlot, false);
			
			foundStructure.add(nt);//find tpye for slot given options
		}
		
		NamedType leret = null;
		NamedType opOn = null;
		ArrayList<NamedType> toGen = new ArrayList<NamedType>(foundStructure.size());
		for(NamedType found : foundStructure){
			NamedType fc = found.copyTypeSpecific();
			if(fc instanceof NamedTypeMany){
				fc = (NamedType)((NamedTypeMany)fc).getSelf();
			}
			
			if(fc.equals(const_obj)){//function sometimes returns object when it means a single itemed ref
				fc = new NamedType(0,0, fc);
			}
			
			if(null == leret){
				leret = fc;
				opOn = leret;
			}
			else{
				opOn.getGenTypes().set(0, fc);
				opOn = fc;
			}
			opOn.setGenTypes(opOn.getGenTypes());//ensures that we trigger setupGenerics (for internal mapping from generic type to qualified type)
			
			toGen.add(opOn);
			
		}
		opOn.getGenTypes().set(0, tagIn);
		
		for(NamedType nt : toGen){//ensures that we trigger setupGenerics (for internal mapping from generic type to qualified type)
			nt.setGenTypes(nt.getGenTypes());
		}
		leret.setIsRef(true);
		return leret;
		
	}
	
	public static final ClassDefJava classDefJavaObj = new ClassDefJava(Object.class);
	public static final NamedType const_obj = new NamedType(classDefJavaObj);
	
	
	private static boolean arrayLevelsMisMatch(List<Type> types){
		int levels = -1;
		for(Type t : types){
			
			int thisOne = t.getArrayLevels();
			
			if(levels < 0){
				levels=thisOne;
			}
			else if(levels != thisOne){//break!
				return true;
			}
		}
		return false;
	}
	
	private static boolean areAllSameGeneric(List<Type> types)
	{
		if(types.isEmpty()){
			return false;
		}
		
		GenericType genSoFar = null;
		
		for(Type t: types)
		{
			if(t instanceof GenericType)
			{
				GenericType gen = (GenericType)t;
				if(null != genSoFar){
					if(!genSoFar.equals(gen)){//ensure all the same
						return false;
					}
				}
				genSoFar = gen;
			}
			else{
				return false;
			}
		}
		return true;
	}
	
	private static boolean areAnyFuncTypes(List<Type> types)
	{
		for(Type t: types)
		{
			if(t instanceof FuncType)
			{
				return true;
			}
		}
		return false;
	}
	
	private static boolean areAllFuncTypes(List<Type> types)
	{
		for(Type t: types)
		{
			if(!(t instanceof FuncType))
			{
				return false;
			}
		}
		return true;
	}
	
	
	private static List<Type> upcastPrimativesMany(List<Type> input/*, Map<Type, Tuple<Integer, Integer>> offenders*/)
	{
		List<Type> ret = new ArrayList<Type>(input.size());
		
		for(Type t : input)
		{
			ret.add(boxTypeIfPrimative(t, false));
		}
		
		return ret;
	}
	
	
	private static boolean isAllPrimativeMaybeBoxed(List<Type> types)
	{
		for(Type t : types)
		{
			if(t instanceof NamedType)
			{
				if(!isBoxedType(t, false))
				{
					return false;
				}
			}
			else if(!(t instanceof PrimativeType))
			{
				return false;
			}
		}
		return true;
	}
	
	private static boolean isAllPrimative(List<Type> types)
	{
		for(Type t : types)
		{
			if(!isPrimative(t))
			{
				return false;
			}
		}
		
		return true;
	}
	
	private static ClassDef getClassDef(ErrorRaiseable invoker, Type rhs)
	{
		ClassDef rhsCls = null;
		if(rhs instanceof NamedType)
		{
			rhsCls = invoker.getImportedOrDeclaredClassDef(((NamedType)rhs).getNamedTypeStr());
		}
		else
		{					
			rhsCls = (ClassDef)rhs;
		}
		return rhsCls;
	}
	
	private static ClassDef findCommonParent(ClassDef lhs, ClassDef rhs)
	{
		if(lhs.getPrettyName() == rhs.getPrettyName())
		{
			return lhs;
		}
		
		
		
		return rhs;
	}
	
	/*
	private static PrimativeType isBoxedPrimative(ClassDef cls)
	{
		PrimativeTypeEnum e = null;

		String name = cls.getPrettyName();
		if(name.equals("java.lang.Long"))
		{
			e = PrimativeTypeEnum.LONG;
		}
		else if(name.equals("java.lang.Integer"))
		{
			e = PrimativeTypeEnum.INT;
		}
		else if(name.equals("java.lang.Double"))
		{
			e = PrimativeTypeEnum.LONG;
		}
		else if(name.equals("java.lang.Float"))
		{
			e = PrimativeTypeEnum.FLOAT;
		}
		else if(name.equals("java.lang.Boolean"))
		{
			e = PrimativeTypeEnum.BOOLEAN;
		}
		
		if(e != null)
		{
			return new PrimativeType(e);
		}
		
		return null;
	}
	*/
	
	public static NamedType objectNT = new NamedType(new ClassDefJava(Object.class));
	
	public static Type boxTypeIfPrimativeAndSetUpperBound(Type thing)
	{
		if(null == thing){ return null;}
		Type ret = (Type)boxTypeIfPrimative(thing, false).copy();
		ret.setOrigonalGenericTypeUpperBound(objectNT);
		return ret;
	}
	

	public static ArrayList<Type> boxTypeIfPrimative(List<Type> things, boolean boxIfHasArrayLevels){
		ArrayList<Type> ret = new ArrayList<Type>(things.size());
		for(Type thing : things){
			ret.add(boxTypeIfPrimative(thing, boxIfHasArrayLevels));
		}
		return ret;
	}

	public static Type boxTypeIfPrimative(FuncType thing, boolean boxIfHasArrayLevels){
		ArrayList<Type> inputs = new ArrayList<Type>(thing.inputs.size());
		for(Type tt : thing.inputs){
			inputs.add(boxTypeIfPrimative(tt, boxIfHasArrayLevels, false));
		}
		FuncType ret = thing.copyTypeSpecific();
		ret.setInputs(inputs);
		ret.retType = boxTypeIfPrimative(thing.retType, boxIfHasArrayLevels, false);
		/*FuncType ret = new FuncType(inputs, boxTypeIfPrimative(thing.retType, boxIfHasArrayLevels, false));
		ret.setLambdaDetails(thing.getLambdaDetails());
		ret.setArrayLevels(thing.getArrayLevels());
		ret.anonLambdaSources = thing.anonLambdaSources;*/
		return ret;
	}
	

	public static Type boxTypeIfPrimative(Type thing, boolean boxIfHasArrayLevels)
	{
		return boxTypeIfPrimative(thing, boxIfHasArrayLevels, true);
	}
	
	public static Type boxTypeIfPrimative(Type thing, boolean boxIfHasArrayLevels, boolean boxvoid)
	{
		if(thing instanceof FuncType){
			return boxTypeIfPrimative((FuncType)thing, false);
		}
		
		if(thing instanceof PrimativeType && (boxIfHasArrayLevels || !thing.hasArrayLevels() ))
		{
			PrimativeTypeEnum typ = ((PrimativeType)thing).type;
			
			NamedType ret = null;
			
			if(typ == PrimativeTypeEnum.BOOLEAN)
			{
				ret = new NamedType(new ClassDefJava(Boolean.class));
			}
			else if(typ == PrimativeTypeEnum.SHORT)
			{
				ret = new NamedType(new ClassDefJava(Short.class));
			}
			else if(typ == PrimativeTypeEnum.BYTE)
			{
				ret = new NamedType(new ClassDefJava(Byte.class));
			}
			else if(typ == PrimativeTypeEnum.CHAR)
			{
				ret = new NamedType(new ClassDefJava(Character.class));
			}
			else if(typ == PrimativeTypeEnum.INT)
			{
				ret = new NamedType(new ClassDefJava(Integer.class));
			}
			else if(typ == PrimativeTypeEnum.DOUBLE)
			{
				ret = new NamedType(new ClassDefJava(Double.class));
			}
			else if(typ == PrimativeTypeEnum.FLOAT)
			{
				ret = new NamedType(new ClassDefJava(Float.class));
			}
			else if(typ == PrimativeTypeEnum.LONG)
			{
				ret = new NamedType(new ClassDefJava(Long.class));
			}
			else if(typ == PrimativeTypeEnum.VOID)
			{//TODO: this code path has not been tested, is ret namedtype correct?
				if(boxvoid) {
					ret = new NamedType(new ClassDefJava(java.lang.Void.class));
				}
			}
			else if(typ == PrimativeTypeEnum.LAMBDA)
			{//TODO: this code path has not been tested, is ret namedtype correct?
				ret = new NamedType(new ClassDefJava(Lambda.class));
			}
			if(ret != null)
			{
				//ret.setInOutGenModifier(thing.getInOutGenModifier());
				
				ret.setArrayLevels(thing.getArrayLevels());
				ret.setNullStatusAtArrayLevel(thing.getNullStatusAtArrayLevel());
				ret.setNullStatus(thing.getNullStatus());
				return ret;
			}
		}
		return thing;
	}
	
	public static Type unboxTypeIfBoxed(Type thing)
	{
		//TODO: this si wrong because classX extends String not catered for!
		if(thing instanceof NamedType)
		{
			NamedType asName = (NamedType)thing;
			if(asName.getIsRef() && !asName.getLockedAsRef()){
				return unboxTypeIfBoxed(asName.getGenTypes().get(0));
			}
			
			ClassDef cd = (asName).getSetClassDef();
			if(cd != null) {
				String named = cd.toString();
				
				PrimativeType ret = null;
				if(named.equals("java.lang.Boolean")) 
				{
					ret = new PrimativeType(PrimativeTypeEnum.BOOLEAN);
				}
				else if(named.equals("java.lang.Character"))
				{
					ret = new PrimativeType(PrimativeTypeEnum.CHAR);
				}
				else if(named.equals("java.lang.Short"))
				{
					ret = new PrimativeType(PrimativeTypeEnum.SHORT);
				}
				else if(named.equals("java.lang.Byte"))
				{
					ret = new PrimativeType(PrimativeTypeEnum.BYTE);
				}
				else if(named.equals("java.lang.Integer"))
				{
					ret = new PrimativeType(PrimativeTypeEnum.INT);
				}
				else if(named.equals("java.lang.Long"))
				{
					ret = new PrimativeType(PrimativeTypeEnum.LONG);
				}
				else if( named.equals("java.lang.Double"))//TODO: bug?
				{
					ret = new PrimativeType(PrimativeTypeEnum.DOUBLE);
				}
				else if(named.equals("java.lang.Float"))
				{
					ret = new PrimativeType(PrimativeTypeEnum.FLOAT);
				}
				
				if(null != ret)
				{
					ret.setArrayLevels(thing.getArrayLevels());
					return ret;
				}
			}
		}
		return thing;
	}
	
	private static boolean isTypeDeterminableRAndL(ErrorRaiseable invoker, Type lhs, Type rhs, 
									int lhsLine, int lhsColumn,
									int rhsLine, int rhsColumn)
	{
		if(null == lhs)
		{
			invoker.raiseError(lhsLine, lhsColumn, "unable to determine type");
			return false;
		}
		
		if(null == rhs)
		{
			invoker.raiseError(rhsLine, rhsColumn, "unable to determine type");
			return false;
		}
		
		return true;
	}
	
	private static Pair<PrimativeType, PrimativeType> leftAndRightMustBeDetAndPrim(ErrorRaiseable invoker, Type lhs, Type rhs, 
											int lhsLine, int lhsColumn,
											int rhsLine, int rhsColumn, String errorPostfix)
	{
		if(!isTypeDeterminableRAndL(invoker, lhs, rhs, lhsLine, lhsColumn, rhsLine, rhsColumn))
		{
			return null;
		}
		
		Type unboxedLhs = unboxTypeIfBoxed(lhs);
		Type unboxedRhs = unboxTypeIfBoxed(rhs);
		
		if(!(unboxedLhs instanceof PrimativeType))
		{
			invoker.raiseError(lhsLine, lhsColumn, String.format("numerical operation cannot be performed on type %s%s" , unboxedLhs, errorPostfix));
			return null;
		}
		if(!(unboxedRhs instanceof PrimativeType))
		{
			invoker.raiseError(rhsLine, rhsColumn, String.format("numerical operation cannot be performed on type %s%s" , unboxedLhs, errorPostfix));
			return null;
		}
		
		PrimativeType lhsPrim = (PrimativeType)unboxedLhs;
		PrimativeType rhsPrim = (PrimativeType)unboxedRhs;
		
		return new Pair<PrimativeType, PrimativeType>(lhsPrim, rhsPrim);
	}
	
	public static boolean isBoolean(ErrorRaiseable invoker, Type type, 
									int ling, int col)
	{
		Type unboxed = unboxTypeIfBoxed(type);
		
		if(unboxed instanceof PrimativeType && ((PrimativeType)unboxed).type == PrimativeTypeEnum.BOOLEAN)
		{
			return true;
		}
		else
		{
			if(invoker != null)
			{
				invoker.raiseError(ling, col, "Expected boolean type" + ( unboxed==null?"" : " not: " + unboxed  )   );
			}
			return false;
		}
	}
	
	public static PrimativeType checkNumericalInfix(ErrorRaiseable invoker, Type lhs, Type rhs, 
			int lhsLine, int lhsColumn,
			int rhsLine, int rhsColumn, String errorPostfix)
	{
		return checkNumericalOpGeneric(invoker, lhs, rhs, lhsLine, lhsColumn, rhsLine, rhsColumn, "numerical", errorPostfix, false, true);
	}
	
	public static PrimativeType checkNumericalOpGeneric(ErrorRaiseable invoker, Type lhs, Type rhs, 
									int lhsLine, int lhsColumn,
									int rhsLine, int rhsColumn,
									String errMsg, String errorPostfix, boolean integralTypeOnly, boolean bestprim)
	{
		TypeCheckUtils.assertRefIsGettable(invoker, lhsLine, lhsColumn, lhs, -1);
		TypeCheckUtils.assertRefIsGettable(invoker, rhsLine, rhsColumn, rhs, -1);
		
		Pair<PrimativeType, PrimativeType> landr = leftAndRightMustBeDetAndPrim(invoker, lhs, rhs, lhsLine, lhsColumn, rhsLine, rhsColumn, errorPostfix);
		
		if(null == landr) return null;
		
		PrimativeType lhsPrim = landr.getA();
		PrimativeType rhsPrim = landr.getB();
		
		if(lhsPrim.thrown || rhsPrim.thrown){
			return null;
		}
		
		if(lhsPrim.type == PrimativeTypeEnum.VOID || rhsPrim.type == PrimativeTypeEnum.VOID){
			return null;
		}
		
		if(lhsPrim.hasArrayLevels() || rhsPrim.hasArrayLevels() )
		{
			if(lhsPrim.hasArrayLevels() )
			{
				invoker.raiseError(lhsLine, lhsColumn, String.format("%s operation cannot be performed on type %s vs %s%s", errMsg, lhsPrim, rhsPrim, errorPostfix));
			}
			
			if(rhsPrim.hasArrayLevels() )
			{
				invoker.raiseError(rhsLine, rhsColumn, String.format("%s operation cannot be performed on type %s vs %s%s", errMsg, lhsPrim, rhsPrim, errorPostfix));
			}
			
			return null;
		}
		
		if(integralTypeOnly){
			if(!lhsPrim.type.isIntegral()) {
				invoker.raiseError(lhsLine, lhsColumn, String.format("%s operation cannot be performed on type %s%s", errMsg, lhsPrim.type, errorPostfix));
				return null;
			}
			
			if(!lhsPrim.type.isIntegral()) {
				invoker.raiseError(rhsLine, rhsColumn, String.format("%s operation cannot be performed on type %s%s", errMsg, rhsPrim.type, errorPostfix));
				return null;
			}
			
			if(lhsPrim.type != PrimativeTypeEnum.LONG){
				lhsPrim = (PrimativeType)ScopeAndTypeChecker.const_int.copy();
			}
			
		}else{
			if(!lhsPrim.type.isNumerical()) {
				invoker.raiseError(lhsLine, lhsColumn, String.format("%s operation cannot be performed on type %s%s", errMsg, lhsPrim.type, errorPostfix));
				return null;
			}
			
			if(!lhsPrim.type.isNumerical()) {
				invoker.raiseError(rhsLine, rhsColumn, String.format("%s operation cannot be performed on type %s%s", errMsg, rhsPrim.type, errorPostfix));
				return null;
			}
		}
		
		/*if(lhsPrim.type != PrimativeTypeEnum.DOUBLE && lhsPrim.type != PrimativeTypeEnum.FLOAT && lhsPrim.type != PrimativeTypeEnum.LONG && lhsPrim.type != PrimativeTypeEnum.INT && lhsPrim.type != PrimativeTypeEnum.BYTE && lhsPrim.type != PrimativeTypeEnum.SHORT)
		{
			invoker.raiseError(lhsLine, lhsColumn, String.format("%s operation cannot be performed on type %s%s", errMsg, lhsPrim.type, errorPostfix));
			return null;
		}
		
		if(rhsPrim.type != PrimativeTypeEnum.DOUBLE && rhsPrim.type != PrimativeTypeEnum.FLOAT && rhsPrim.type != PrimativeTypeEnum.LONG && rhsPrim.type != PrimativeTypeEnum.INT && lhsPrim.type != PrimativeTypeEnum.BYTE && lhsPrim.type != PrimativeTypeEnum.SHORT)
		{
			invoker.raiseError(rhsLine, rhsColumn, String.format("%s operation cannot be performed on type %s%s", errMsg, rhsPrim.type, errorPostfix));
			return null;
		}*/
		
		
		return bestprim?bestPrim(lhsPrim, rhsPrim):lhsPrim;
		
	}

	public static PrimativeType bestPrim(PrimativeType lhsPrim, PrimativeType rhsPrim)
	{
		if(lhsPrim.type == rhsPrim.type)
		{
			return new PrimativeType(lhsPrim.type);
		}
		if(lhsPrim.type==PrimativeTypeEnum.DOUBLE || rhsPrim.type==PrimativeTypeEnum.DOUBLE)
			return new PrimativeType(PrimativeTypeEnum.DOUBLE);
		else if(lhsPrim.type==PrimativeTypeEnum.FLOAT || rhsPrim.type==PrimativeTypeEnum.FLOAT)
			return new PrimativeType(PrimativeTypeEnum.FLOAT);
		if(lhsPrim.type==PrimativeTypeEnum.LONG || rhsPrim.type==PrimativeTypeEnum.LONG)
			return new PrimativeType(PrimativeTypeEnum.LONG);
		
		if((lhsPrim.type==PrimativeTypeEnum.BOOLEAN && rhsPrim.type !=PrimativeTypeEnum.BOOLEAN) || (rhsPrim.type==PrimativeTypeEnum.BOOLEAN && lhsPrim.type !=PrimativeTypeEnum.BOOLEAN) ){
			return null;//naughty
		}
		
		return new PrimativeType(PrimativeTypeEnum.INT);
	}
	
	private static PrimativeType checkLHSPrimCanBeAssigned(ErrorRaiseable invoker, PrimativeType lhsPrim, PrimativeType rhsPrim, int rhsLine, int rhsColumn)
	{
		if(rhsPrim.getPointer() == lhsPrim.getPointer()) {
			if(lhsPrim.type==PrimativeTypeEnum.BOOLEAN)
			{
				if(rhsPrim.type == PrimativeTypeEnum.BOOLEAN)
				{
					return lhsPrim;
				}
			}
			else if(lhsPrim.type==PrimativeTypeEnum.SIZE_T)
			{
				if(rhsPrim.type == PrimativeTypeEnum.SIZE_T ||rhsPrim.type == PrimativeTypeEnum.INT || rhsPrim.type == PrimativeTypeEnum.LONG)
				{
					return lhsPrim;
				}
			}
			else if(lhsPrim.type==PrimativeTypeEnum.INT)
			{
				if(rhsPrim.type == PrimativeTypeEnum.SIZE_T ||rhsPrim.type == PrimativeTypeEnum.INT || rhsPrim.type == PrimativeTypeEnum.BYTE || rhsPrim.type == PrimativeTypeEnum.SHORT || rhsPrim.type == PrimativeTypeEnum.CHAR)
				{
					return lhsPrim;
				}
			}
			else if(lhsPrim.type==PrimativeTypeEnum.LONG)
			{
				if(rhsPrim.type == PrimativeTypeEnum.SIZE_T || rhsPrim.type == PrimativeTypeEnum.INT || rhsPrim.type == PrimativeTypeEnum.LONG || rhsPrim.type == PrimativeTypeEnum.BYTE || rhsPrim.type == PrimativeTypeEnum.SHORT || rhsPrim.type == PrimativeTypeEnum.CHAR)
				{
					return lhsPrim;
				}
			}
			else if(lhsPrim.type==PrimativeTypeEnum.FLOAT)
			{
				if(rhsPrim.type == PrimativeTypeEnum.FLOAT || rhsPrim.type == PrimativeTypeEnum.INT || rhsPrim.type == PrimativeTypeEnum.LONG || rhsPrim.type == PrimativeTypeEnum.BYTE || rhsPrim.type == PrimativeTypeEnum.SHORT || rhsPrim.type == PrimativeTypeEnum.CHAR)
				{
					return lhsPrim;
				}
			}
			else if(lhsPrim.type==PrimativeTypeEnum.DOUBLE)
			{
				if(rhsPrim.type == PrimativeTypeEnum.DOUBLE || rhsPrim.type == PrimativeTypeEnum.FLOAT || rhsPrim.type == PrimativeTypeEnum.INT || rhsPrim.type == PrimativeTypeEnum.LONG || rhsPrim.type == PrimativeTypeEnum.BYTE || rhsPrim.type == PrimativeTypeEnum.SHORT || rhsPrim.type == PrimativeTypeEnum.CHAR)
				{
					return lhsPrim;
				}
			}
			else if(lhsPrim.type==PrimativeTypeEnum.CHAR)
			{
				if(rhsPrim.type == PrimativeTypeEnum.CHAR || rhsPrim.type == PrimativeTypeEnum.INT || rhsPrim.type == PrimativeTypeEnum.BYTE)
				{
					return lhsPrim;
				}
			}
			else if(lhsPrim.type==PrimativeTypeEnum.SHORT)
			{
				if(rhsPrim.type == PrimativeTypeEnum.SHORT || rhsPrim.type == PrimativeTypeEnum.INT || rhsPrim.type == PrimativeTypeEnum.BYTE)
				{
					return lhsPrim;
				}
			}
			else if(lhsPrim.type==PrimativeTypeEnum.BYTE)
			{
				if(rhsPrim.type == PrimativeTypeEnum.BYTE || rhsPrim.type == PrimativeTypeEnum.INT )
				{
					return lhsPrim;
				}
			}
		}
		invoker.raiseError(rhsLine, rhsColumn, String.format("Type mismatch: cannot convert from %s to %s",  rhsPrim, lhsPrim));
		return null;
	}

	
	private static Type checkClassDefEqual(ErrorRaiseable invoker, ClassDef lhsClassDef, ClassDef rhsClassDef,
			NamedType lhs, NamedType rhs,
			int lhsLine, int lhsColumn,
			int rhsLine, int rhsColumn,
			boolean canBeAParent)
	{
		if(null == lhsClassDef || null == rhsClassDef)
		{
			//invoker.raiseError(lhsLine, lhsColumn, String.format("Unimported Type: %s", lhsClassDef.getPrettyName()) );
			return null;
		}
		
		if(lhsClassDef.getPrettyName().equals(rhsClassDef.getPrettyName()))
		{
			return new NamedType(lhsLine, lhsColumn, lhsClassDef);
		}
		else
		{
			if(!canBeAParent)
			{
				invoker.raiseError(rhsLine, rhsColumn, String.format("type mismatch between %s and %s", rhs, lhs) );
				return null;
			}
			else if(rhsClassDef.isBeingBeingPastTheParentOfMe(lhsClassDef))//is the thing on the left a parent of the thing on the right a List = new ArrayList();
			{
				return new NamedType(lhsLine, lhsColumn, lhsClassDef);
			}
			else
			{
				invoker.raiseError(rhsLine, rhsColumn, String.format("%s is not a subtype of %s", rhs, lhs) );
				return null;
			}
		}
	}
	
	//TODO: actors, shared
	//TODO: generic
	//TODO: a = 9,6
	//TODO: a Matrix [1,2,3][4,5,6] <-boxers and unboxers
	//TODO: references a := ""; //this is the way we do STM
	

	
	private static boolean parentOfObjectArray(String prettyName)
	{//as par java lang spec 4.10.3. Subtyping among Array Types, object etc is root of all arrays (even object array)
		if(prettyName.equals("java.lang.Object"))
		{
			return true;
		}
		else if(prettyName.equals("java.lang.Cloneable"))
		{
			return true;
		}
		else if(prettyName.equals("java.io.Serializable"))
		{
			return true;
		}
		else
		{
			return false;
		}
				
	}
	
	private static boolean isPrimative(Type input)
	{
		return input instanceof PrimativeType && PRIMS_TO_BOXED.containsKey(((PrimativeType)input).type);
	}
	
	public static boolean isPurePrimativeNonArray(Type what){
		if(what instanceof PrimativeType && !what.hasArrayLevels()){
			PrimativeTypeEnum which = ((PrimativeType)what).type;
			return  which != PrimativeTypeEnum.LAMBDA;
		}
		return false;
	}
	
	private static final HashMap<String, PrimativeTypeEnum[]> BOXED_TO_PRIMS = TypeCheckUtilsUtils.buildToBoxedsPrims();
	public static final HashMap<PrimativeTypeEnum, String[]> PRIMS_TO_BOXED = TypeCheckUtilsUtils.buildPrimsToBoxeds();
	
	private static final HashMap<String, PrimativeType>  BOXED_TO_PRIM_REF_TYPE= new HashMap<String, PrimativeType>();
	
	static{
		for(String x : BOXED_TO_PRIMS.keySet()){
			BOXED_TO_PRIM_REF_TYPE.put(x, new PrimativeType(BOXED_TO_PRIMS.get(x)[0]));
		}
	}
	
	public static PrimativeType getUnboxedPrimativeType(Type intput){
		String pn = intput.getPrettyName();
		return BOXED_TO_PRIM_REF_TYPE.get(pn);
	}
	
	public static boolean isBoxedType(Type input) {
		return isBoxedType(input,true);
	}
	
	public static boolean isBoxedType(Type input, boolean includeNumber)
	{
		if(input instanceof NamedType && !input.hasArrayLevels())
		{
			ClassDef cd = ((NamedType)input).getSetClassDef();
			
			if(cd != null ) {
				
				String k = cd.toString();
				if(!includeNumber && k.equals("java.lang.Number")) {
					return false;
				}
						
				return BOXED_TO_PRIMS.containsKey(k);
			}
			
		
		}
		return false;
	}
	
	public static Type checkSubType(ErrorRaiseable invoker, Type lhs, Type rhs){
		if(invoker == null) {
			invoker = TypeCheckUtils.errorRaisableSupression;
		}
		return checkSubType(invoker, lhs, rhs,0,0,0,0);
	}
	
	public static Type checkSubType(ErrorRaiseable invoker, Type lhs, Type rhs,
			int lhsLine, int lhsColumn,
			int rhsLine, int rhsColumn)
	{
		return checkSubType(invoker, lhs, rhs, false, lhsLine, lhsColumn, rhsLine, rhsColumn, true, true, false, false, false);
	}
	
	public static Type checkSubType(ErrorRaiseable invoker, Type lhs, Type rhs, boolean strictRhsLockedRef,
			int lhsLine, int lhsColumn,
			int rhsLine, int rhsColumn)
	{
		return checkSubType(invoker, lhs, rhs, strictRhsLockedRef, lhsLine, lhsColumn, rhsLine, rhsColumn, true, true, false, false, false);
	}
	
	public static Type checkSubType(ErrorRaiseable invoker, Type lhs, Type rhs,
			int lhsLine, int lhsColumn,
			int rhsLine, int rhsColumn, boolean strictBoxing)
	{
		return checkSubType(invoker, lhs, rhs, false, lhsLine, lhsColumn, rhsLine, rhsColumn, true, true, strictBoxing, false, false);
	}
	
	public static Type checkSubType(ErrorRaiseable invoker, Type lhs, Type rhs,
			int lhsLine, int lhsColumn,
			int rhsLine, int rhsColumn, boolean allowPrimTypefiddleCast, boolean allowParentNamedTypeInherit)
	{
		return checkSubType(invoker, lhs, rhs, false, lhsLine, lhsColumn, rhsLine, rhsColumn, allowPrimTypefiddleCast, allowParentNamedTypeInherit, false, false, false);
	}
	
	public static Type extractRawRefType(Type nextLevel){
		boolean isRefArray = false;
		
		if(nextLevel != null) {
			NullStatus ns = nextLevel.getNullStatus();
			
			while(hasRefLevels(nextLevel)){
				//ret += 1;
				isRefArray = isRefArrayGettable(nextLevel, -1);
				nextLevel = ((NamedType)nextLevel).getGenTypes().get(0);
			}
			
			if(isRefArray){
				nextLevel = (Type)nextLevel.copy();
				nextLevel.setArrayLevels(nextLevel.getArrayLevels() + 1);
			}
			
			if(ns == NullStatus.UNKNOWN) {
				nextLevel = (Type)nextLevel.copy();
				nextLevel.setNullStatus(NullStatus.UNKNOWN);
			}
		}
		
		
		return nextLevel;
	}
	
	public static boolean hasRefLevelsAndNotLocked(Type nextLevel){
		if( nextLevel != null && !nextLevel.hasArrayLevels() && nextLevel instanceof NamedType ){
			NamedType asNamed = ((NamedType)nextLevel);
			return asNamed.getIsRef() && !asNamed.getLockedAsRef();
		}
		return false;
	}
	
	public static boolean hasRefLevelsAndIsLocked(Type nextLevel){
		if( nextLevel != null && !nextLevel.hasArrayLevels() && nextLevel instanceof NamedType ){
			NamedType asNamed = ((NamedType)nextLevel);
			return asNamed.getIsRef() && asNamed.getLockedAsRef();
		}
		return false;
	}
	
	public static boolean hasArrayRefLevels(Type nextLevel){
		return nextLevel != null  && nextLevel instanceof NamedType && ((NamedType)nextLevel).getIsRef();
	}
	
	public static boolean hasRefLevels(Type nextLevel){
		return nextLevel != null && !nextLevel.hasArrayLevels() && nextLevel instanceof NamedType && ((NamedType)nextLevel).getIsRef();// && !((NamedType)nextLevel).getGenTypes().get(0).getLockedAsRef();
	}
	
	public static boolean hasRefLevelsAndIsNotLocked(Type nextLevel){
		return nextLevel != null && !nextLevel.hasArrayLevels() && nextLevel instanceof NamedType && ((NamedType)nextLevel).getIsRef() && !((NamedType)nextLevel).getLockedAsRef();
	}
	
	public static boolean hasArrayLevelsWhenUnrefed(Type nextLevel){
		
		while(TypeCheckUtils.hasRefLevels(nextLevel)){
			nextLevel = ((NamedType)nextLevel).getGenTypes().get(0);
		}
		
		return nextLevel.getArrayLevels()>0;
		
	}
	
	public static boolean hasRefLevelsAndIsArray(Type nextLevel){
		return nextLevel != null && nextLevel.hasArrayLevels() && nextLevel instanceof NamedType && ((NamedType)nextLevel).getIsRef();
	}
	
	public static int getRefLevelsAndIsArray(Type nextLevel){
		int ret = 0;
		while(hasRefLevelsAndIsArray(nextLevel)){
			ret += 1;
			nextLevel = ((NamedType)nextLevel).getGenTypes().get(0);
		}
			
		return ret;
	}
	
	
	public static int getRefLevelsIfNoeLockedAsRef(Type level){
		int ret = 0;
		while(TypeCheckUtils.hasRefLevelsAndIsNotLocked(level)){
			ret += 1;
			level = ((NamedType)level).getGenTypes().get(0);
		}
			
		return ret;
	}
	
	public static int getRefLevelsIfLockedAsRef(Type level){
		if(TypeCheckUtils.hasRefLevelsAndIsLocked(level)){
			return getRefLevels(level);
		}
		return 0;
	}
	
	public static int getRefLevels(Type nextLevel){
		int ret = 0;
		while(hasRefLevels(nextLevel)){
			ret += 1;
			nextLevel = ((NamedType)nextLevel).getGenTypes().get(0);
		}
			
		return ret;
	}
	
	public static int getRefLevelsToSetter(Type nextLevel){
		int ret = 0;
		while(hasRefLevels(nextLevel)){
			ret += 1;
			if(TypeCheckUtils.isRefArraySettable( nextLevel, -1)){
				return ret;
			}
			nextLevel = ((NamedType)nextLevel).getGenTypes().get(0);
		}
			
		return ret;
	}
	
	public static Type makeRef(Type gotTF, int refLevels) {
		Type ret = gotTF;
		for(int derp =0; derp < refLevels; derp++){
			ret = new NamedType(0,0,ret);
		}
		return ret;
	}
	
	public static Type getRefType(Type nextLevel){
		boolean isRefArray = false;
		Type ret = nextLevel;
		//while(ret != null && ret instanceof NamedType && ((NamedType)ret).getIsRef() && !ret.hasArrayLevels()){
		while(ret != null && ret instanceof NamedType && (TypeCheckUtils.hasRefLevels(ret) && !ret.hasArrayLevels())){
			isRefArray = isRefArrayGettable(ret, -1);
			ret = ((NamedType)ret).getGenTypes().get(0);
			if(isRefArray){
				break;
			}
		}
			

		if(isRefArray){
			ret = (Type)ret.copy();
			ret.setArrayLevels(ret.getArrayLevels() + 1);
			ret.setOrigonalGenericTypeUpperBound(ScopeAndTypeChecker.const_object_1ar);
		}
		
		return ret;
	}
	
/*	public static Type extractReturnedRefTypeFromGet(Type thing){
		if(TypeCheckUtils.isRefArrayGettable(thing, -1)){
			Type ret = ((NamedType)thing).getGenTypes().get(0);
			ret = (Type)ret.copy();
			ret.setArrayLevels(ret.getArrayLevels()+1);
			return ret;
		}else{
			return getRefType(thing);
		}
	}
	
	public static Type extractReturnedRefTypeToSet(Type thing){
		if(TypeCheckUtils.isRefArraySettable(thing, -1)){
			Type ret = ((NamedType)thing).getGenTypes().get(0);
			ret = (Type)ret.copy();
			ret.setArrayLevels(ret.getArrayLevels()+1);
			return ret;
		}else{
			return extractReturnedRefTypeToSet(thing);
		}
	}*/
	
	
	/**
	 * Just return the generic qualification of the ref. e.g. Integer:RefArry -> Integer (not Integer[]), Integer: -> Integer
	 */
	public static Type getRefTypeIgnoreRefArray(Type nextLevel){
		Type ret = nextLevel;
		//while(ret != null && ret instanceof NamedType && ((NamedType)ret).getIsRef() && !ret.hasArrayLevels()){
		while(ret != null && ret instanceof NamedType && (TypeCheckUtils.hasRefLevels(ret) && !ret.hasArrayLevels())){
			ret = ((NamedType)ret).getGenTypes().get(0);
		}
		
		return ret;
	}
	
	
	
	
	
	
	//do next two
	
	
	
	
	
	public static ArrayList<NamedType> getRefTypes(Type nextLevel){
		ArrayList<NamedType> ret = new ArrayList<NamedType>(1);//99% 1
		boolean cont=hasRefLevels(nextLevel);
		if(cont){
			ret.add((NamedType)nextLevel);
		}
		while(cont){
			nextLevel = ((NamedType)nextLevel).getGenTypes().get(0); 
			cont=hasRefLevels(nextLevel);
			if(cont){
				ret.add((NamedType)nextLevel);
			}
		}
		
		return ret;
	}
	
	public static ArrayList<Type> extractRefTypes(Type nextLevel){
		ArrayList<Type> ret = new ArrayList<Type>(1);//99% 1
		boolean cont=hasRefLevels(nextLevel);
		while(cont){
			nextLevel = ((NamedType)nextLevel).getGenTypes().get(0); 
			cont=hasRefLevels(nextLevel);
			ret.add(nextLevel);
		}
		
		return ret;
	}
	
	public static NamedType getRefTypeLastRef(Type nextLevel){
		Type ret = nextLevel;
		//while(ret != null && ret instanceof NamedType && ((NamedType)ret).getIsRef() && !ret.hasArrayLevels()){
		while(ret != null && ret instanceof NamedType && (TypeCheckUtils.hasRefLevels(ret) && !ret.hasArrayLevels())){
			Type child = ((NamedType)ret).getGenTypes().get(0);
			if(!TypeCheckUtils.hasRefLevels(child)){
				return (NamedType)ret;
			}
			ret = child;
		}
			
		return null;
	}
	
	public static Type getRefTypeToLocked(Type level){
		//unref until u arrive at something taht is locked

		boolean isRefArray = false;
		
		while(TypeCheckUtils.hasRefLevelsAndIsNotLocked(level)){
			isRefArray = isRefArrayGettable(level, -1);
			level = ((NamedType)level).getGenTypes().get(0);
		}
		
		if(isRefArray){
			level = (Type)level.copy();
			level.setArrayLevels(level.getArrayLevels() + 1);
		}
		
		return level;
	}
	
	public static int getRefLevlstoLocked(Type level){
		//unref until u arrive at something taht is locked
		int n=0;
		while(TypeCheckUtils.hasRefLevelsAndIsNotLocked(level)){
			level = ((NamedType)level).getGenTypes().get(0);
			n++;
		}
		return n;
	}
	
	public static boolean isObject(Type teste){
		return (teste instanceof NamedType) ? ((NamedType) teste).equals(const_obj) : false;
	}
	
	private static Type removeArrayAndLockVolOnCopy(Type xxx){
		if(xxx.hasArrayLevels()){
			Type lhsNoAr =  (Type)xxx.copy();
			lhsNoAr.setArrayLevels(0);
			
			if(TypeCheckUtils.hasRefLevels(lhsNoAr)){
				NamedType nt = (NamedType)lhsNoAr;
				nt.setLockedAsRef(true);
				xxx =nt;
			}
			else{
				xxx = lhsNoAr;
			}
		}
		
		return xxx;
	}
	
	private final static PrimativeType const_void_thrown = new PrimativeType(PrimativeTypeEnum.VOID);
	static{
		const_void_thrown.thrown=true;
	}
	
	private static ClassDef[] getMultilevelRefClasses(Type theRef, int myLevels, int leng){
		ClassDef[] ret = new ClassDef[leng];
		int idx = myLevels;
		for(int n=0; n<myLevels; n++){
			ret[(idx--)-1 ] = ((NamedType)theRef).getSetClassDef();
			theRef = (Type)((NamedType)theRef).getGenTypes().get(0);
		}
		
		return ret;
	}
	
	private static boolean isRefInstantiable(ErrorRaiseable invoker, Type lhs, Type rhs, int rhsLine, int rhsColumn){
		int lhsRefLevels = TypeCheckUtils.getRefLevels(lhs);
		int rhsRefLevels = TypeCheckUtils.getRefLevels(rhs);
		
		if(lhsRefLevels >= rhsRefLevels){//ensure that we can upref-cast into the type on the lhs
			//in particular, where rhs is not a ref, ensure that lhs is not a interface ref (i.e. Ref) - as this is not concrete we cannot upref into this
			ClassDef[] lhsClasses = getMultilevelRefClasses(lhs, lhsRefLevels, lhsRefLevels);
			ClassDef[] rhsClasses = getMultilevelRefClasses(rhs, rhsRefLevels, lhsRefLevels);
			for(int n = 0; n < lhsRefLevels; n++){
				ClassDef lcls = lhsClasses[n];
				ClassDef rcls = rhsClasses[n];
				if(rcls == null){
					//if(null != lcls){
					//null is a problem still...
						if(lcls != null && !lcls.isInstantiable()){//e.g. if ref
							invoker.raiseError(rhsLine, rhsColumn, String.format("Type mismatch: cannot create ref from %s to uninstantiable: %s", rhs , lhs ));
							return false;
						}
					//}
				}
				else{
					//check
					if(!rcls.isBeingBeingPastTheParentOfMe(lcls)){
						return false;
					}
					
					
				}
				
			}
			
		}
		
		return true;
		
	}
	
	/**
	 * 
	 * @param strictBoxing - used to restict boxing convertions to the first native type only and not the castables: thus rendering this code non ambugous:
	 * fun cahpo2( o Long) String { return ""; } 
		fun cahpo2( o Integer) boolean { return false; } 
		res = cahpo2( 5);//this is ok
		
		strictRhsLockedRef :-
		class MyClas{
			var ~x Object: = "orig"
	 	}
		mc = new MyClas()
		mc.x = "newRef" //means the above is not ambigoius -as- call to check args here sets strictRhsLockedRef = Tre
	
		
	 * @return
	 */
	private static Type checkSubType(ErrorRaiseable invoker, Type lhs, Type rhs, boolean strictRhsLockedRef,
			int lhsLine, int lhsColumn,
			int rhsLine, int rhsColumn,
			boolean allowPrimTypefiddleCast,
			boolean allowParentNamedTypeInherit,
			boolean strictBoxing,
			boolean dontPermitBoxingUnboxing,
			boolean ignoreGenericParams)
	{
		
		//determine if one thing is a subtype of another
		//Set a = new HashSet[String]();
		
		if(null == lhs || null == rhs || lhs instanceof ModuleType || rhs instanceof ModuleType)
		{
			if(null != invoker){
				//invoker.raiseError(rhsLine, rhsColumn, "unable to determine type");
			}
			
			return null;
		}
		
		if(lhs.equals(rhs)) {
			return lhs;
		}
		
		/*if(lhs instanceof NamedType &&  ((NamedType)lhs).getSetClassDef().getPrettyName().equals("java.lang.Object")){
			//object is parent of all refs - this is a bit of a hack here
			if(!(rhs instanceof PrimativeType) && TypeCheckUtils.hasRefLevels(rhs)){
				return lhs;
			}
		}*/
		
		
		/*if(rhs instanceof NamedType)//here we just assumt ? is object, but if it were say Number, then cast to String would fail
		{
			NamedType rhsNamed = (NamedType)rhs;//? could be anything
			
			if(rhsNamed.isWildCardAny){//Ref[Object] -> Local[int] //could be, requires runtime check
				return lhs;
			}
		}*/
		
		rhs = convertfuncTypetoNamedType(rhs, lhs);
		lhs = convertfuncTypetoNamedType(lhs, null);
		
		boolean rhshasRefLevelsAndIsLocked =  TypeCheckUtils.hasRefLevelsAndIsLocked(rhs);
		boolean rhsLockedAsRef = rhshasRefLevelsAndIsLocked && strictRhsLockedRef;
		
		if(TypeCheckUtils.hasRefLevels(lhs) || TypeCheckUtils.hasRefLevels(rhs)){
			
			if(lhs instanceof GenericType) { 
				Type upperBound = ((GenericType)lhs).upperBound;
				if(null == upperBound) {
					upperBound = lhs.getOrigonalGenericTypeUpperBound();
				}

				int arlevels = lhs.getArrayLevels();
				
				if(null == upperBound) {
					lhs = new NamedType(((GenericType) lhs).getLine(), ((GenericType) lhs).getColumn(), new ClassDefJava(java.lang.Object.class)); 
				}else {
					lhs = (Type)upperBound.copy(); 
				}
				
				lhs.setArrayLevels(arlevels);
			}
			
			int lhsRefLevels = TypeCheckUtils.getRefLevels(lhs);
			int rhsRefLevels = TypeCheckUtils.getRefLevels(rhs);
			
			
			//check this case first against the origin ref type if there is one (i.e. we unreffed the ref cc := 9; funcla(cc) --cc is unreffed, but check the reffed version as well)
			
			boolean refInstantiable = false;
			/*if(rhs instanceof NamedType && null != ((NamedType) rhs).originRefType){//originRefType
				refInstantiable = isRefInstantiable(invoker, lhs, ((NamedType) rhs).originRefType, rhsLine, rhsColumn );
			}*/
			if(null != ((AbstractType)rhs).originRefType){//originRefType
				refInstantiable = isRefInstantiable(invoker, lhs, ((AbstractType) rhs).originRefType, rhsLine, rhsColumn );
			}
			
			if(!refInstantiable){//try against the type validating against now
				Type rhsRefInst = rhs;
				if(rhsRefInst instanceof GenericType) { 
					Type upperBound = ((GenericType)rhsRefInst).upperBound;
					if(null == upperBound) {
						upperBound = rhsRefInst.getOrigonalGenericTypeUpperBound();
					}
					
					if(null == upperBound) {
						rhsRefInst = new NamedType(new ClassDefJava(java.lang.Object.class)); 
					}else {
						rhsRefInst = (Type)upperBound.copy(); 
					}
				}
				
				if(!isRefInstantiable(invoker, lhs, rhsRefInst, rhsLine, rhsColumn )){
					invoker.raiseError(rhsLine, rhsColumn, String.format("Type mismatch: cannot convert from %s to %s", rhsRefInst , lhs ));
					return null;
				}
			}
			
			//this looks fun
			
			if(lhsRefLevels>0 && rhsRefLevels>0){
				Type unRefCastTo = TypeCheckUtils.getRefTypeIgnoreRefArray(lhs);
				Type unRefExpType = TypeCheckUtils.getRefTypeIgnoreRefArray(rhs);
				
				int wantLEvels = TypeCheckUtils.getRefLevels(rhs);
				
				//if rhs VarNull then convert to lhs!
				if(wantLEvels >0 && wantLEvels == TypeCheckUtils.getRefLevels(lhs) &&unRefCastTo instanceof NamedType && unRefExpType instanceof VarNull){
					//ref levels match and wei're going from varnull to type; e.g. a String: = null!
					mutatleRefType(wantLEvels, (NamedType)rhs, (NamedType)lhs);
				}
				else{
					//lhs and rhs type must match 1:1 if refs
					if(null!= unRefExpType && null!= unRefCastTo && null == TypeCheckUtils.checkSubType(invoker.getErrorRaiseableSupression(), unRefCastTo, unRefExpType, 0, 0, 0, 0)){
						
						if(unRefCastTo instanceof NamedType && ((NamedType)unRefCastTo).isWildCardAny)//here we just assumt ? is object, but if it were say Number, then cast to String would fail
						{//Ref[Object] -> Local[int] //could be, requires runtime check
							return lhs;
						}
						
						
						invoker.raiseError(rhsLine, rhsColumn, String.format("Type mismatch: cannot convert from %s to %s", rhs , lhs ));
						return null;
					}
				}
			}
			
			/*if(lhsRefLevels>rhsRefLevels){
				if(!isRefInstantiable(invoker, lhs, rhs, rhsLine, rhsColumn )){
					return null;
				}
			}*/
			
			if(rhs instanceof NamedType){
				
				NamedType rhsEch = (NamedType)rhs;
				
				if(rhsRefLevels==0 && lhsRefLevels>0 && rhs instanceof NamedType && null != rhsEch.originRefType){
					rhsEch = (NamedType)rhsEch.originRefType;
					rhsRefLevels = TypeCheckUtils.getRefLevels(rhsEch);
				}
				
				if(rhsRefLevels > lhsRefLevels){//requires explicit extraction of rhs element, a int = (12::)
					if(!assertRefIFaceImpl(invoker, String.format("Type mismatch: cannot convert to %s as ref type: %s does not support getting as it does not implement the DirectlyGettable or DirectlyArrayGettable interface", lhs, rhsEch), rhsLine, rhsColumn, rhsEch, -1, true, Cls_DirectlyGettable, Cls_DirectlyArrayGettable)){
						return null;
					}
				}
				else if(lhsRefLevels>rhsRefLevels){//requires lhs to be explicitly set, a int: = 12
					
					if(!assertRefIFaceImpl(invoker, String.format("Type mismatch: cannot assign to ref type: %s as it does not support assignment since the DirectlyAssignable or DirectlyArrayAssignable interface is not implemented", lhs), lhsLine, lhsColumn, lhs, -1, true, Cls_DirectlyAssignable, Cls_DirectlyArrayAssignable)){
						return null;
					}
				}
			}
			
			//f int[]: = [5!,5!,5!,5!] <- not permitted, so extract type from array and examine directly
			
			Type lhsNoRef = TypeCheckUtils.getRefType(lhs);
			Type rhsNoRef = TypeCheckUtils.getRefType(rhs);
			
			
			/*Type lhsNoRef = TypeCheckUtils.extractReturnedRefTypeFromGet(lhs);
			Type rhsNoRef = TypeCheckUtils.extractReturnedRefTypeFromGet(rhs);*/
			
			Type gotla = checkSubType(invoker.getErrorRaiseableSupression(), lhsNoRef, rhsNoRef, strictRhsLockedRef, lhsLine, lhsColumn,
																													rhsLine, rhsColumn,
																													allowPrimTypefiddleCast,
																													allowParentNamedTypeInherit, strictBoxing, dontPermitBoxingUnboxing, ignoreGenericParams);
			if(null == gotla){
				invoker.raiseError(rhsLine, rhsColumn, String.format("Type mismatch: cannot convert from %s to %s", rhs , lhs ));
			}
			
			if(lhsNoRef.hasArrayLevels() && (lhsNoRef.getArrayLevels()  == rhsNoRef.getArrayLevels())   ){//and lock everything
				Type lhsNoAr =  removeArrayAndLockVolOnCopy(lhsNoRef);
				Type rhsNoAr =  removeArrayAndLockVolOnCopy(rhsNoRef);
				
				Type got = checkSubType(invoker.getErrorRaiseableSupression(), lhsNoAr, rhsNoAr, strictRhsLockedRef,
						lhsLine, lhsColumn,
						rhsLine, rhsColumn,
						allowPrimTypefiddleCast,
						allowParentNamedTypeInherit, strictBoxing, dontPermitBoxingUnboxing, ignoreGenericParams);
				if(null == got){
					invoker.raiseError(rhsLine, rhsColumn, String.format("Type mismatch: cannot convert from %s to %s", rhs , lhs ));
					return null;
				}
				
				return lhsNoAr.equals(got)? lhs: rhs;
			}
			/*else if(!lhsNoRef.equals(rhsNoRef)){
				invoker.raiseError(rhsLine, rhsColumn, String.format("Type mismatch: cannot convert from %s to %s - can only create ref on type: %s", rhs , lhs, rhsNoRef ));
				return null;
			}*/
		}
		
		
		//dereference rhs
		
		if(rhs instanceof NamedType){
			NamedType rhsAsNamed = (NamedType)rhs;
			if(!rhsAsNamed.hasArrayLevels() && !TypeCheckUtils.hasRefLevels(lhs)){
				
				if(rhsAsNamed.getIsRef() && !rhsAsNamed.getLockedAsRef()) {
					rhs = ((NamedType)rhs).getGenTypes().get(0);
				}
			}
		}
		
		if(rhs instanceof NamedType)//here we just assumt ? is object, but if it were say Number, then cast to String would fail
		{
			NamedType rhsNamed = (NamedType)rhs;//? could be anything
			
			if(rhsNamed.isWildCardAny){//Ref[Object] -> Local[int] //could be, requires runtime check
				//check against upper bound
				NamedType checkvs = rhsNamed.getOrigonalGenericTypeUpperBound();
				if(null == checkvs) {
					checkvs = ScopeAndTypeChecker.const_object;
				}
				/*Type vsUpper =*/ return checkSubType(invoker, lhs, checkvs, strictRhsLockedRef, lhsLine, lhsColumn,rhsLine, rhsColumn, allowPrimTypefiddleCast, allowParentNamedTypeInherit, strictBoxing, dontPermitBoxingUnboxing, ignoreGenericParams);
				//return vsUpper==null?null:lhs;
			}
		}
		
		int rhsRefLevesl = TypeCheckUtils.getRefLevels(rhs);
		if(rhsRefLevesl > 0 && rhsRefLevesl == TypeCheckUtils.getRefLevels(lhs)){
			//ret int: = [5,5,5,5]! :: invalid
			/*Type tryMe = */return checkSubType(invoker.getErrorRaiseableSupression(), TypeCheckUtils.getRefTypeIgnoreRefArray(lhs), TypeCheckUtils.getRefTypeIgnoreRefArray(rhs),  strictRhsLockedRef,
					 lhsLine,  lhsColumn,
					 rhsLine,  rhsColumn,
					 allowPrimTypefiddleCast,
					 allowParentNamedTypeInherit,  strictBoxing, dontPermitBoxingUnboxing, ignoreGenericParams);
			
			//return tryMe==null?null:lhs;
		}
				
		//check for boxing...
		
		boolean isLhsPrim = isPrimative(lhs);
		boolean isRhsPrim = isPrimative(rhs);
		
		boolean isLhsboxedtype = false;
		boolean isRhsboxedtype = false;
		
		if(!isLhsPrim && !dontPermitBoxingUnboxing)//so, do permitBoxing
			isLhsboxedtype = isBoxedType(lhs);
		if(!isRhsPrim && !dontPermitBoxingUnboxing)//so, do permitBoxing
			isRhsboxedtype = isBoxedType(rhs);
		
		if(rhs instanceof NamedType && rhs.hasArrayLevels()){
			NamedType rhsNamed = (NamedType)rhs;
			
			if(rhs.getArrayLevels() == lhs.getArrayLevels()){
				if(rhsNamed.getIsRef() )
				{//asObjAr Object[] = [1!,2!] <- ok
					//asObjAr as Number:[] <-fail
					Type lhsOfTheRef = lhs;
					if(lhsOfTheRef instanceof GenericType){
						lhsOfTheRef = lhsOfTheRef.getOrigonalGenericTypeUpperBound();
					}
					
					if(lhsOfTheRef instanceof NamedType){
						NamedType lhsNamed = (NamedType)lhsOfTheRef;
						if(lhsNamed.getIsRef()){
							return lhsOfTheRef;
						}
						else if(lhsNamed.getSetClassDef().equals(classDefJavaObj)) {
							return lhsOfTheRef;
						}
					}
					invoker.raiseError(rhsLine, rhsColumn, String.format("Type mismatch: cannot convert from %s to %s", rhs , lhs ));
					return null;
				}
				else if(classDefJavaObj.equals(rhsNamed.getSetClassDef())) {
					//Object[] -> Number:[] <-fail
					if(lhs instanceof NamedType &&  ((NamedType)lhs).getIsRef()){
						invoker.raiseError(rhsLine, rhsColumn, String.format("Type mismatch: cannot convert from %s to %s", rhs , lhs ));
						return null;
					}
				}else if(lhs instanceof PrimativeType) {
					invoker.raiseError(rhsLine, rhsColumn, String.format("Type mismatch: cannot convert from %s to %s", rhs , lhs ));
					return null;
				}
				Type rhsNoAr = (Type)rhs.copy();
				Type lhsNoAr = (Type)lhs.copy();
				rhsNoAr.setArrayLevels(0);
				lhsNoAr.setArrayLevels(0);
				//Object supertype of primative type
				/*if(TypeCheckUtils.isBoxedType(lhsNoAr, false) || TypeCheckUtils.isBoxedType(rhsNoAr, false)){//if boxed then must match
					
					if(rhsNoAr instanceof NamedType && lhsNoAr instanceof NamedType){
						if(!lhsNoAr.equals(rhsNoAr)){
							if(!lhsNoAr.equals(ScopeAndTypeChecker.const_object)){//object rules all, always lhs
								return null;
							}
						}
					}
					
				}*/
				
				if(TypeCheckUtils.getRefLevels(rhsNoAr) != TypeCheckUtils.getRefLevels(lhsNoAr)){
					invoker.raiseError(rhsLine, rhsColumn, String.format("Type mismatch: cannot convert from %s to %s", rhs , lhs ));
					return null;
				}
				
				//check without the arrays
				Type tryMe =  checkSubType(invoker.getErrorRaiseableSupression(), lhsNoAr, rhsNoAr,  strictRhsLockedRef,
						 lhsLine,  lhsColumn,
						 rhsLine,  rhsColumn,
						 allowPrimTypefiddleCast,
						 allowParentNamedTypeInherit,  strictBoxing, true, ignoreGenericParams);
				
				if(tryMe == null) {
					invoker.raiseError(rhsLine, rhsColumn, String.format("Type mismatch: cannot convert from %s to %s", rhs , lhs ));
				}
				
				return tryMe==null?null:lhs;
				
			}
			
		}
		
		if(isLhsPrim && isRhsboxedtype){
			PrimativeTypeEnum[] rhsPrims = BOXED_TO_PRIMS.get( ((NamedType) rhs).getSetClassDef().toString() );
			if(null != rhsPrims && lhs.getArrayLevels() == rhs.getArrayLevels())
			{
				PrimativeTypeEnum toMatch = ((PrimativeType)lhs).type;
				boolean isFirst = true;
				for(PrimativeTypeEnum rhsPrim : rhsPrims)
				{
					if(toMatch.equals(rhsPrim)) {
						return lhs;
					}
					if(isFirst){
						isFirst=false;
					}
					else if(strictBoxing){
						break;
					}
					
				}
			}
			
			if(null != invoker){
				invoker.raiseError(rhsLine, rhsColumn, String.format("Type mismatch: cannot convert from %s to %s", rhs , lhs ));
			}
			
			return null;
		}
		
		if(isLhsboxedtype && isRhsPrim)
		{
			String[] rhsboxedchoices= PRIMS_TO_BOXED.get(((PrimativeType)rhs).type);
			
			if(null != rhsboxedchoices && lhs.getArrayLevels() == rhs.getArrayLevels())
			{
				String pret = ((NamedType)lhs).getSetClassDef().toString();
				boolean isFirst=true;
				for(String rhsboxed : rhsboxedchoices)
				{
					if(pret.equals(rhsboxed)){
						return lhs;
					}
					
					if(isFirst){
						isFirst=false;
					}
					else if(strictBoxing){
						break;
					}
				}
			}
			
			if(null != invoker){
				invoker.raiseError(rhsLine, rhsColumn, String.format("Type mismatch: cannot convert from %s to %s", rhs , lhs ));
			}
			
			return null;
		}
		
		if(isLhsboxedtype && isRhsboxedtype && !lhs.hasArrayLevels() && !rhs.hasArrayLevels()) {
			
			Type boxlhs = BOXED_TO_PRIM_REF_TYPE.get(((NamedType) lhs).getSetClassDef().toString());
			Type brhs = BOXED_TO_PRIM_REF_TYPE.get(((NamedType) rhs).getSetClassDef().toString());
			
			if(null == checkSubType(invoker.getErrorRaiseableSupression(), boxlhs, brhs, strictRhsLockedRef,
					lhsLine, lhsColumn,
					rhsLine, rhsColumn,
					allowPrimTypefiddleCast,
					allowParentNamedTypeInherit,
					strictBoxing,
					dontPermitBoxingUnboxing, ignoreGenericParams)) {
				invoker.raiseError(rhsLine, rhsColumn, String.format("Cannot convert boxed primative types: %s to %s", rhs, lhs ));
				return null;
			}
			return lhs;
		}
		

		//MHA: - fails? [MyClassZ < X < MyInterfaceMaster] - so upper type is MyInterfaceMaster and lower is MyClassZ
		
		if(lhs instanceof GenericType && rhs instanceof GenericType && !lhs.equals(rhs))
		{//prevent: a ArrayList[B] = (new Object() ) as ArrayList[A];
			if(null != invoker){
				invoker.raiseError(rhsLine, rhsColumn, String.format("Unbound generic types must match. %s does not equal %s", lhs, rhs ));
			}
			
			NamedType lhsUpperBound = ((GenericType)lhs).upperBound;
			NamedType rhsUpperBound = ((GenericType)rhs).upperBound;
			
			if(null == TypeCheckUtils.checkSubType(invoker.getErrorRaiseableSupression(), lhsUpperBound, rhsUpperBound)) {
				invoker.raiseError(rhsLine, rhsColumn, String.format("Upper bound of generic type: %s not compatible with upper bound of qualifying generic type: %s", lhsUpperBound, rhsUpperBound ));
				return null;
			}
			
			
			return lhs;
		}
		
		if(lhs instanceof VarNull && rhs instanceof VarNull && lhs.getArrayLevels() == rhs.getArrayLevels()){
			return lhs;
		}
		
		if(lhs instanceof GenericType && rhs instanceof GenericType){
			if(!lhs.equals(rhs)){
				invoker.raiseError(rhsLine, rhsColumn, String.format("Generic types do not match: %s vs %s", lhs, rhs));
			}
			return lhs;//they must eq otherwise
		}
		
		if(lhs instanceof GenericType && rhs instanceof VarNull){//null can be any generic type
			return lhs;
		}
				
		
		//if(lhs instanceof GenericType) { lhs = new NamedType(new ClassDefJava(java.lang.Object.class)); }
		if(rhs instanceof GenericType) { 
			Type upperBound = ((GenericType)rhs).upperBound;
			if(null == upperBound) {
				upperBound = rhs.getOrigonalGenericTypeUpperBound();
			}
			
			int arlevels = rhs.getArrayLevels();
			
			if(null == upperBound) {
				rhs = new NamedType(new ClassDefJava(java.lang.Object.class)); 
			}else {
				rhs = (Type)upperBound.copy(); 
			}
			rhs.setArrayLevels(arlevels);
		}
		//oops
		
		//special exceptions for jls 4.10.2....
		if(lhs instanceof NamedType &&   null != ((NamedType)lhs).getSetClassDef() &&  parentOfObjectArray(((NamedType)lhs).getSetClassDef().getPrettyName()) && !rhsLockedAsRef  )
		{
			if(lhs.hasArrayLevels())
			{
				/*
				Object e = new Object[2][2]; // - ok
				Object[] e1 = new Object[2][2]; // - ok
				Object[][] e2 = new Object[2][2]; // - ok
				java.io.Serializable[] e3 = new Object[2]; // not allowed
				java.io.Serializable[][] e4 = new Object[2][2]; // not allowed
				java.io.Serializable[] e5 = new Object[2][2];//ok
				
				
				
				//CHECK ME: as as as
				
				Object[] = int[] //no
				
				*/
				
				if(rhs instanceof VarNull )
				{
					return lhs;
				}
				else if(((NamedType)lhs).getSetClassDef().getPrettyName().equals("java.lang.Object"))
				{
					if( lhs.getArrayLevels() < rhs.getArrayLevels() ||( lhs.getArrayLevels() <= rhs.getArrayLevels() && rhs.hasArrayLevels() && (!(rhs instanceof PrimativeType) || (rhs instanceof PrimativeType && ((PrimativeType)rhs).type == PrimativeTypeEnum.LAMBDA  )  )))
					{
						return lhs;
					}
				}
				else
				{
					if( lhs.getArrayLevels() < rhs.getArrayLevels() )
					{//has to be less
						return lhs;
					}
				}
				if(null != invoker){
					invoker.raiseError(rhsLine, rhsColumn, String.format("Type mismatch: cannot convert from %s to %s", rhs , lhs ));
				}
				
				return null;
			}
			return lhs;
			
		}
		
		int lhsArrLevels;
		
		if(TypeCheckUtils.hasRefLevels(lhs) && !TypeCheckUtils.hasRefLevels(rhs))
		{//permits this: a int[]: = [1,2]
			lhsArrLevels = ((NamedType)lhs).getGenTypes().get(0).getArrayLevels();
		}
		else{//normal cae
			lhsArrLevels = lhs.getArrayLevels();
		}
		
		//if((!lhs.hasArrayLevels() && lhs instanceof NamedType && ((NamedType) lhs).getIsRef())){
		//	lhsArrLevels = ((NamedType) lhs).getGenTypes().get(0).getArrayLevels();
		//}
		
		//java.lang.Integer:[]
		//int[]:
		
		if(lhs instanceof GenericType){
			if(rhs instanceof NamedType && ((NamedType)rhs).orignallyfromVarNull){//a T = null//this is ok
				return lhs;
			}
			invoker.raiseError(lhsLine, lhsColumn, String.format("Type must match generic type: %s", lhs));
			/*if(rhs instanceof PrimativeType && rhs.hasArrayLevels()){
				return null;//this is not going to be converted to a generic tpye array
			}*/
			
			NamedType upperBound = ((GenericType)lhs).getOrigonalGenericTypeUpperBound().copyTypeSpecific();
			upperBound.setArrayLevels(lhsArrLevels);
			
			if(null != TypeCheckUtils.checkSubType(invoker, upperBound, rhs)){
				return lhs;
			}else{
				return null;
			}
			
		}
		
		//if(!(lhs instanceof NamedType && ((NamedType) lhs).getIsRef())){//skip this check for refs
		if(lhsArrLevels !=  rhs.getArrayLevels() ){
			//TODO: Cannot create a generic array of ArrayList<String> - see if you can create bytecode anyway...
			int rhsArrLevels = rhs.getArrayLevels();
			if( rhsArrLevels < lhsArrLevels  && rhs instanceof VarNull)
			{//manyobjnullalright  int[][] = [null, null];//ok
				return lhs;
			}
			else{
				if(null != invoker){
					invoker.raiseError(rhsLine, rhsColumn, String.format("Type array levels don't match. Expected: %s vs %s", lhsArrLevels , rhsArrLevels ));
					return null;
				}
				
				return lhs;
			}
		}
		//}
			
		if(lhs instanceof PrimativeType)
		{
			PrimativeType lhsPrimative = (PrimativeType)lhs;
			
			if(lhsPrimative.type == PrimativeTypeEnum.LAMBDA)
			{
				if(rhs instanceof FuncType || (rhs instanceof PrimativeType && ((PrimativeType)rhs).type == PrimativeTypeEnum.LAMBDA))
				{
					return lhs;
				}
				else if(rhs instanceof NamedType && ((NamedType)rhs).equals(new NamedType(new ClassDefJava(java.lang.Object.class))) )
				{//TODO: turn new NamedType(new ClassDefJava into a static ref
					return lhs;
				}
				else
				{
					//com.concurnas.bootstrap.lang.Lambda
					if(rhs instanceof NamedType  )
					{//TODO: turn new NamedType(new ClassDefJava into a static ref
						NamedType asNamed = (NamedType)rhs;
						ClassDef cd = asNamed.getSetClassDef();
						if(cd.getSuperAsNamedType(0, 0).equals(new NamedType(new ClassDefJava(Lambda.class)))){
							return lhs;
						}
					}
					
					if(null != invoker){
						invoker.raiseError(rhsLine, rhsColumn, "Incompatible type. Expecting lambda, method reference not: " + rhs);
					}
					
					return lhs;
				}
				
			}
			else if(TypeCheckUtils.isVoidPrimativePure(lhsPrimative)) 
			{
				//if(rhs instanceof NamedType && ((NamedType)rhs).toString().equals("java.lang.Void") )
				if(TypeCheckUtils.isVoidAdObj(rhs)){
					return lhs;
				}
				else
				{
					if(null != invoker){
						invoker.raiseError(lhsLine, lhsColumn, "void is not an instantiable type");
					}
					return null;
				}
			}
			else if(rhs instanceof PrimativeType)
			{
				PrimativeType rhsPrimative = (PrimativeType)rhs;

				if(lhs.hasArrayLevels())
				{//primative array rhs must match 1:1
					//i.e. a int[] = [1,2,3] as int[]; //ok
					// b double[] = a; //fail
					// c double[] = a as double[]; //fail
					if(!lhs.getPrettyName().equals(rhs.getPrettyName()))
					{
						if(null != invoker){
							invoker.raiseError(rhsLine, rhsColumn, String.format("Type mismatch: cannot convert from %s to %s", rhs, lhs ));
						}
						
						return null;
					}
				}
				
				if(TypeCheckUtils.isVoidPrimativePure(rhsPrimative))
				{
					if(null != invoker){invoker.raiseError(rhsLine, rhsColumn, "void is not an instantiable type");}
					
					return null;
				}
				else
				{
					if(lhsPrimative.type == rhsPrimative.type)
					{//have to match exactly
						return lhs;//BOOLEAN, INT, LONG, FLOAT, DOUBLE
					}
					else if(allowPrimTypefiddleCast)
					{
						return checkLHSPrimCanBeAssigned(invoker, lhsPrimative, rhsPrimative, rhsLine, rhsColumn);
						/*
						if(lhsPrimative.type ==  PrimativeTypeEnum.LONG && (lhsPrimative.type ==  PrimativeTypeEnum.LONG || lhsPrimative.type ==  PrimativeTypeEnum.INT))
						{//int -> long ok
							return lhs;
						}
						else if (lhsPrimative.type ==  PrimativeTypeEnum.DOUBLE && (lhsPrimative.type ==  PrimativeTypeEnum.FLOAT || lhsPrimative.type ==  PrimativeTypeEnum.DOUBLE))
						{//float -> doulble ok
							return lhs;
						}
						else
						{
							invoker.raiseError(rhsLine, rhsColumn, String.format("Incompadtible types %s vs %s", rhsPrimative.type, lhsPrimative.type));
							return null;
						}
						*/
					}
					else
					{
						if(null != invoker){
							invoker.raiseError(rhsLine, rhsColumn, String.format("Type %s not equal to %s", lhsPrimative.type, rhsPrimative.type));
						}
						
						return null;
					}
				}
			}
			else if(lhs.getPointer() > 0 && rhs instanceof VarNull) {
				return lhs;
			}
			else
			{
				if(!isVoidPrimativeThrown(lhs) && null != invoker){
					invoker.raiseError(rhsLine, rhsColumn, String.format("incompatible type: %s vs %s", lhs, rhs));
				}
				
				return null;
			}
		}
		else if(lhs instanceof NamedType)
		{//TODO: error here, we're not checking the generic types defined if there are any
			if(rhs instanceof VarNull)
			{
				if(!rhs.hasArrayLevels() || ((NamedType)lhs).equals(new NamedType(new ClassDefJava(java.lang.Object.class))) )
				{
					return lhs;
				}
				else{
					invoker.raiseError(rhsLine, rhsColumn, String.format("list of null cannot be converted treated as type: %s", lhs));
					return null;
				}
			}
			NamedType lhsNamedType = (NamedType)lhs;
			
			ClassDef lhsNamedClassDef = lhsNamedType.getSetClassDef();
			
			if(null == lhsNamedClassDef)
			{//a Strong = "";
				if(null != invoker){
					invoker.raiseError(lhsLine, lhsColumn, String.format("Unable to determine type of: %s", lhs) );
				}
				
				return null;
			}
			
			if( ((NamedType)lhs).toString().equals("java.lang.Void") && rhs instanceof PrimativeType && ((PrimativeType)rhs).type==PrimativeTypeEnum.VOID )
			{
				return lhs;
			}
			else
			{
				if(rhshasRefLevelsAndIsLocked){
					int lref = TypeCheckUtils.getRefLevels(lhs);
					if(lref>0 && lref < TypeCheckUtils.getRefLevels(rhs)){//this is ok a:: = 8:, this is not: b: = 8:: where a-> int:: and b -> int:
						invoker.raiseError(rhsLine, rhsColumn, String.format("Type mismatch: cannot convert from %s to %s", rhs , lhs ));
						return null;
					}
				}
				
				if( TypeCheckUtils.hasRefLevelsAndNotLocked(lhs)  &&  !(rhs instanceof NamedType &&  ((NamedType)rhs).getIsRef())  ){ //((NamedType) lhs).getIsRef()
					
					NamedType ref = (NamedType)lhs;
					
					if(ref.hasArrayLevels()){//for: afail2  int:[] = [9,9]
						invoker.raiseError(rhsLine, rhsColumn, String.format("Type mismatch: cannot convert from %s to %s", rhs , lhs ));
						return null;
					}
					else{
						Type refType = ref.getGenTypes().get(0);
						return checkSubType(invoker, refType, rhs, strictRhsLockedRef, lhsLine, lhsColumn, rhsLine, rhsColumn, allowPrimTypefiddleCast, allowParentNamedTypeInherit, strictBoxing, dontPermitBoxingUnboxing, ignoreGenericParams);
					}
					
				}
				else{
					//if(lhs instanceof NamedType && ((NamedType)lhs).isGeneric())
					if( ((NamedType)lhs).isGeneric() && !((NamedType)lhs).getIsRef() )
					{
						//TODO: check that type referenced actuall permits generics...
						/*
						Map<Object, String> oo = new HashMap<ArrayList<Object>, String>();        //<- no
						Object oo2 = new HashMap<ArrayList<Object>, String>();
						Map<List<Object>, String> oo3 = new HashMap<ArrayList<Object>, String>();        //<- no
						Map<ArrayList<Object>, String> oo4 = new HashMap<ArrayList<Object>, String>();
						List<Object> lo = new ArrayList<String>(); //no: this is a java error that requires some fixing later TODO: do better job than java
						List<String> low = new ArrayList<String>(); 
						List<int[]> g = new ArrayList<int[]>();
						ArrayList<String> asdasd  = new ArrayList();
						*/			
						
						if(rhs instanceof PrimativeType && !rhs.hasArrayLevels()) {
							rhs = TypeCheckUtils.boxTypeIfPrimative(rhs, false);
						}
						
						if(!(rhs instanceof NamedType)){
							 invoker.raiseError(rhsLine, rhsColumn, String.format("cannot assign type of %s to %s", rhs, lhs) );
							 return null;
						}
						
						
						NamedType lhsNTG = (NamedType)lhs;
						NamedType rhsNTG = (NamedType)rhs;
						ClassDef lhsClassDef = lhsNTG.getSetClassDef();
						ClassDef rhsClassDef = rhsNTG.getSetClassDef();
						
						if(rhsClassDef == null || lhsClassDef ==null) {
							return null;
						}
						
						boolean inInterface = rhsClassDef.getTraitsIncTrans().contains(lhsClassDef);
						
						if(ignoreGenericParams && null != rhsClassDef.isParentNestorEQOrSUperClass(lhsClassDef) ) { 
								return lhs;
						}
						
						if(!lhsClassDef.equals(rhsClassDef) && !inInterface){
							//upcast rhs until we get to match
							
							/*NamedType lhsAsNamed = (NamedType)lhs;
						 	if(lhsAsNamed.isInterface()){
						 		//Map <= HashMap
							 	if(rhsClassDef.getInterfaces().contains(lhsAsNamed.getSetClassDef())){
							 		return lhs;
							 	}
						 	}*/
						 	
						 	//however the parent of the rhs may be the generic type e.g. class D extends C[String]{}
							NamedType supNamed = ((NamedType)rhs).getResolvedSuperTypeAsNamed();//try to see if superclass implements it
							
							Type gotViasup = checkSubType(invoker.getErrorRaiseableSupression(), lhs, supNamed, lhsLine, lhsColumn, rhsLine, rhsColumn, allowPrimTypefiddleCast, allowParentNamedTypeInherit);
							
							if(null == gotViasup)
							{
								invoker.raiseError(rhsLine, rhsColumn, String.format("cannot assign type of %s to %s", rhs, lhs) );
							}
							
							return gotViasup;  
						}/*else if(inInterface){
							ArrayList<NamedType> gear = rhsClassDef.getInterfacesAsNamedType();
							return lhs;
						}*/
						else if(((NamedType)rhs).isGeneric() && !((NamedType)rhs).getIsRef() )
						{
							Type baseGenMatch = checkClassDefEqual(invoker, lhsClassDef, rhsClassDef, lhsNTG, rhsNTG, lhsLine, lhsColumn,rhsLine, rhsColumn, allowParentNamedTypeInherit);//alow parents inherit
							//nested generic types are not permitted
							if(baseGenMatch == null)
							{
								return null;
							}
							else
							{//check the children. Children must match type EXACTLY
								int refsLhs = getRefLevels(lhsNTG); //val a int: = 99; a = 9 //this case is ok
								int refsRhs = getRefLevels(rhsNTG);
								
								boolean isRefiedType = TypeCheckUtils.isReifiedType(lhsNTG);//if refied then lhs generics >= rhs generics, otherwise they must match: lhs generics == rhs generics
								
								if(refsLhs > 0){
									//if(refsLhs<refsRhs || !getRefType(lhsNTG).equals(getRefType(rhsNTG))){
									if(refsLhs<refsRhs ||  null == checkSubType(invoker.getErrorRaiseableSupression(), getRefTypeIgnoreRefArray(lhsNTG), getRefTypeIgnoreRefArray(rhsNTG), strictRhsLockedRef, lhsLine, lhsColumn, rhsLine, rhsColumn,allowPrimTypefiddleCast, allowParentNamedTypeInherit, strictBoxing, dontPermitBoxingUnboxing, ignoreGenericParams)   ){
										//fail
										invoker.raiseError(rhsLine, rhsColumn, String.format("Ref type mismatch: %s vs %s", lhsNTG, rhsNTG) );
										return null;
									}
									return lhs;
								}
								else{
									ArrayList<Type> lhsGenTypes = lhsNTG.getGenericTypeElements();
									ArrayList<Type> rhsGenTypes = rhsNTG.getGenericTypeElements();
									
									if(lhsGenTypes.size() != rhsGenTypes.size())
									{
										invoker.raiseError(rhsLine, rhsColumn, String.format("Generic Type argument count mismatch: %s vs %s", lhsGenTypes.size(), rhsGenTypes.size()) );
										return null;
									}
									else
									{
										for(int n=0; n <lhsGenTypes.size(); n++)
										{//? < Object <- can either be a generic type or it can be a NamedType
											Type aType = lhsGenTypes.get(n);
											Type bType = rhsGenTypes.get(n);
											//Type matches = checkSubType(invoker, aType, bType, lhsLine, lhsColumn, rhsLine, rhsColumn, false, false); //false as array types must match exactly
											
											boolean isGenAny  = (aType instanceof GenericType && (((GenericType)aType).name.equals("?")  || ((GenericType)aType).isWildcard));
											boolean isNamedAny = aType instanceof NamedType && (((NamedType)aType).isWildCardAny || ((NamedType)aType).fromisWildCardAny);
											//boolean isNamedAny = aType instanceof NamedType && (((NamedType)aType).isWildCardAny );
											
											if(!isGenAny && !isNamedAny){//"?" matches everything
												boolean pass = true;
												if(isRefiedType) {
													pass = null != TypeCheckUtils.checkSubType(invoker.getErrorRaiseableSupression(), aType, bType);
												}else if(!ensureGenericParamMatch(invoker, aType, bType)){
													pass=false;
												}
												
												if(!pass) {
													invoker.raiseError(rhsLine, rhsColumn, String.format("Generic Type argument type mismatch: %s vs %s", aType, bType) );
													return null;
												}
												
											}
											
										}
										NamedType lhsNestorParent = lhsNTG.getparentNestorFakeNamedType();
										NamedType rhsNestorParent = rhsNTG.getparentNestorFakeNamedType();
										
										if(rhsNestorParent != null && lhsNestorParent != null)
										{
											if(null == checkSubType(invoker.getErrorRaiseableSupression(), lhsNestorParent, rhsNestorParent, lhsLine,  lhsColumn, rhsLine,  rhsColumn, allowPrimTypefiddleCast, allowParentNamedTypeInherit))
											{
												invoker.raiseError(rhsLine, rhsColumn, String.format("Parent nestor type argument type mismatch: %s vs %s", lhsNestorParent, rhsNestorParent) );
												return lhs;
											}
										}
										return lhs;
									}
								}
							}
						}
						else
						{//see if the dude on the rhs implements the lhs which is an interface
						 	NamedType lhsAsNamed = (NamedType)lhs;
						 	if(lhsAsNamed.isInterface()){
						 		//Map <= HashMap
						 		ClassDef rhsCls = ((NamedType)rhs).getSetClassDef();
							 	if(rhsCls.getTraitsIncTrans().contains(lhsAsNamed.getSetClassDef())){
							 		return lhs;
							 	}
						 	}
						 	
						 	//however the parent of the rhs may be the generic type e.g. class D extends C[String]{}
							NamedType supNamed = ((NamedType)rhs).getResolvedSuperTypeAsNamed();//try to see if superclass implements it
							//TODO: implements interfaces as well
							
							Type gotViasup = checkSubType(invoker.getErrorRaiseableSupression(), lhs, supNamed, lhsLine, lhsColumn, rhsLine, rhsColumn, allowPrimTypefiddleCast, allowParentNamedTypeInherit);
							
							if(null == gotViasup)
							{
								invoker.raiseError(rhsLine, rhsColumn, String.format("cannot assign type of %s to %s", rhs, lhs) );
							}
							
							return gotViasup;
						}
					}
				
					else if(((NamedType)lhs).equals(new NamedType(new ClassDefJava(com.concurnas.bootstrap.lang.Lambda.class)))){//TODO: make this a constant
						if(rhs instanceof FuncType || (rhs instanceof PrimativeType && ((PrimativeType)rhs).type == PrimativeTypeEnum.LAMBDA))
						{
							return lhs;
						}
						/*else if(rhs instanceof NamedType && ((NamedType)rhs).equals(new NamedType(new ClassDefJava(java.lang.Object.class))) )
						{//TODO: turn new NamedType(new ClassDefJava into a static ref
							return lhs;
						}*/
						else if(rhs instanceof NamedType && ((NamedType)rhs).equals(new NamedType(new ClassDefJava(com.concurnas.bootstrap.lang.Lambda.class))) )
						{//TODO: turn new NamedType(new ClassDefJava into a static ref
							return lhs;
						}
						else
						{
							//com.concurnas.bootstrap.lang.Lambda
							if(rhs instanceof NamedType  )
							{//TODO: turn new NamedType(new ClassDefJava into a static ref
								NamedType asNamed = (NamedType)rhs;
								ClassDef cd = asNamed.getSetClassDef();
								
								NamedType sup = cd == null ? null : cd.getSuperAsNamedType(0, 0);
								
								if(sup != null && sup.equals(new NamedType(new ClassDefJava(Lambda.class)))){
									return lhs;
								}
							}
							
							if(null != invoker){
								invoker.raiseError(rhsLine, rhsColumn, "Incompatible type. Expecting lambda, method reference not: " + rhs);
								return null;
							}else {
								return lhs;
							}
						}
					}
					else
					{
						NamedType lhsNamed = (NamedType)lhs;
						//ClassDef lhsClassDef = invoker.getImportedClassDef(lhsNamed.namedType);
						ClassDef lhsClassDef = lhsNamed.getSetClassDef();
						
						//a ClassA = new ClassB()
						//b ClassB = new ClassB()
						//also (ClassA)b <- upcast, actuall downcast is ok, this occurs at runtime only...
						
						//a = actor B(); //a is an actor holding B
						//c = shared C() //c is a shared holding C
						if(rhs instanceof NamedType)
						{
							NamedType rhsNamed = (NamedType)rhs;
							//ClassDef rhsClassDef = invoker.getImportedClassDef(rhsNamed.namedType, true);
							ClassDef rhsClassDef = rhsNamed.getSetClassDef();
							
							return checkClassDefEqual(invoker, lhsClassDef, rhsClassDef, lhsNamed, rhsNamed, lhsLine, lhsColumn,rhsLine, rhsColumn, allowParentNamedTypeInherit);
						}
						else if(rhs instanceof VarNull)
						{
							return lhs;
						}
						else
						{
							if(!rhs.equals(const_void_thrown)){//if thrown then its already been tagged as an error
								invoker.raiseError(rhsLine, rhsColumn, String.format("%s is not a subtype of %s", rhs, lhs) );
							}
							return null;
						}
					}
				}
			}
		}
		else if (lhs instanceof FuncType)
		{
			//compare arguments, number of argument and return type
			if(rhs instanceof FuncType)
			{
				FuncType lhsFuncType = (FuncType)lhs;
				FuncType rhsFuncType = (FuncType)rhs;
				if(lhsFuncType.getInputs().size() !=  rhsFuncType.getInputs().size())
				{
					invoker.raiseError(rhsLine, rhsColumn, String.format("Number of function inputs do not match: %s vs %s", lhsFuncType.getInputs().size() ,  rhsFuncType.getInputs().size() ));
					return null;
				}
				//funno (int, float) String = func(a int, b float) String {return "" + (a +b);}
				for(int n = 0; n < lhsFuncType.getInputs().size(); n++)
				{
					Type inputLhs = lhsFuncType.getInputs().get(n);
					Type inputRhs = rhsFuncType.getInputs().get(n);
					
					ErrorRaiseableSupressErrorsAndLogProblem problog = new ErrorRaiseableSupressErrorsAndLogProblem(invoker);
					
					Type matches = checkSubType(problog, inputLhs, inputRhs, lhsLine, lhsColumn, rhsLine, rhsColumn, true, true);
					if(null == matches || problog.isHasErrored())
					{
						invoker.raiseError(rhsLine, rhsColumn, String.format("Function type input argument type do not match: expected: %s vs %s in: %s vs %s", inputLhs, inputRhs, lhs, rhs));
						return null;
					}
				}
				Type retLhs = lhsFuncType.retType;
				Type retRhs = rhsFuncType.retType;
				
				if(!retLhs.equals(retRhs)){
					ErrorRaiseableSupressErrorsAndLogProblem problog = new ErrorRaiseableSupressErrorsAndLogProblem(invoker);
					Type matches = checkSubType(problog, retLhs, retRhs, lhsLine, lhsColumn, rhsLine, rhsColumn, true, true);
					if(null == matches || problog.isHasErrored())
					{
						invoker.raiseError(rhsLine, rhsColumn, String.format("Function type return type does not match: %s vs %s", retLhs, retRhs));
						return null;
					}
					/*else if( TypeCheckUtils.isBoxedType(retRhs) && ! TypeCheckUtils.isBoxedType(retLhs) ){
						invoker.raiseError(rhsLine, rhsColumn, String.format("Function type return type is boxed: %s (expecting unboxed type: %s). This cannot be unboxed as the boxed instance could be null", retRhs, retLhs ));
						return null;
					}*/
				}
				
				return lhs;
			}
			else if(rhs instanceof NamedType && ((NamedType)rhs).equals(new NamedType(new ClassDefJava(java.lang.Object.class))) )
			{
				return lhs;
			}
			else if(rhs instanceof NamedType && ((NamedType)rhs).equals(new NamedType(new ClassDefJava(com.concurnas.bootstrap.lang.Lambda.class))) )
			{
				return lhs;
			}
			else if(rhs instanceof PrimativeType && ((PrimativeType)rhs).type == PrimativeTypeEnum.LAMBDA)
			{
				return lhs;
			}
			else 
			{
				if(!(rhs instanceof VarNull))
				{
					invoker.raiseError(rhsLine, rhsColumn, "Function type expected in place of: " + rhs);
					return null;
				}
				return lhs;
			}
		}else if(lhs instanceof MultiType) {
			return null;
		}
		else
		{
			invoker.raiseError(lhsLine, lhsColumn, String.format("Unexpected type of %s", lhs.getPrettyName()));
			return null;
		}
	}
	
	private static boolean ensureGenericParamMatch(ErrorRaiseable invoker, Type aType, Type bType) {
		
		if(null == aType || null == bType){
			return false;
		}
		
		if(aType.equals(bType)){//shortcut if equal
			return true;
		}
		
		if(aType instanceof VarNull) {
			Type tt = ((Node)aType).getTaggedTypeRaw(); 
			if(tt != null) {
				aType = tt;
			}
		}
		
		if(bType instanceof VarNull) {
			Type tt = ((Node)bType).getTaggedTypeRaw();
			if(tt != null) {
				bType = tt;
			}
		}
		
		if(aType instanceof NamedType && bType instanceof NamedType){
			if(TypeCheckUtils.hasRefLevels(aType) && TypeCheckUtils.hasRefLevels(bType)){//refs just need to be subtypes of eachother
				return null != TypeCheckUtils.checkSubType(invoker, aType, bType, 0, 0, 0, 0); 
			}
			else{//not normal classes must match 1:1
				 //class 1:1 check and then run this on generics
				NamedType aNamed = ((NamedType)aType);
				NamedType bNamed = ((NamedType)bType);
				
				
				ClassDef aCls = aNamed.getSetClassDef();
				ClassDef bCls = bNamed.getSetClassDef();
				if(aCls == null || bCls==null || !aCls.equals(bCls) || aNamed.getInOutGenModifier() != bNamed.getInOutGenModifier()){
					
					boolean notWildCardAny = !aNamed.isWildCardAny;
					boolean notOutGenParam = aNamed.getInOutGenModifier() != InoutGenericModifier.OUT || null == TypeCheckUtils.checkSubType(invoker.getErrorRaiseableSupression(), new NamedType(aCls), new NamedType(bCls));
					boolean notInGenParam = aNamed.getInOutGenModifier() != InoutGenericModifier.IN || null == TypeCheckUtils.checkSubType(invoker.getErrorRaiseableSupression(), new NamedType(bCls ), new NamedType(aCls));//other way around
					//mastero ArrayList<in Integer> = new ArrayList<Number>();//ok

					boolean rhsIsInType = bNamed.getInOutGenModifier() == InoutGenericModifier.IN;
					
					if(notWildCardAny && notOutGenParam && notInGenParam){
						
						if(rhsIsInType) {
							return null != TypeCheckUtils.checkSubType(invoker.getErrorRaiseableSupression(), new NamedType(aCls), new NamedType(bCls));
						}
						
						return false;
					}
					else if(aNamed.getInOutGenModifier() == InoutGenericModifier.OUT){
						if(null != TypeCheckUtils.checkSubType(invoker.getErrorRaiseableSupression(), aNamed, bNamed)){
							return true;
						}
					}else if(aNamed.getInOutGenModifier() == InoutGenericModifier.IN){
						if(null != TypeCheckUtils.checkSubType(invoker.getErrorRaiseableSupression(), bNamed, aNamed)){
							return true;
						}
					}
				}
				
				List<Type> aGens =  aNamed.getGenTypes();
				List<Type> bGens =  bNamed.getGenTypes();
				
				if(aGens.size() != bGens.size()){
					return false;
				}
				
				for(int n=0; n < aGens.size(); n++){
					if(!ensureGenericParamMatch(invoker, aGens.get(n), bGens.get(n))){
						return false;
					}
				}
			}
		}
		else if(aType instanceof FuncType && bType instanceof FuncType){
			FuncType aAsFuncType = (FuncType)aType;
			FuncType bAsFuncType = (FuncType)bType;
			int asize = aAsFuncType.inputs.size();
			if(asize != bAsFuncType.inputs.size()){
				return false;
			}else{
				for(int n=0; n < asize; n++){
					Type inputA = aAsFuncType.inputs.get(n);
					Type inputB = bAsFuncType.inputs.get(n);
					
					if(!ensureGenericParamMatch(invoker, inputA, inputB)){
						return false;
					}
				}
				
				if(!ensureGenericParamMatch(invoker, aAsFuncType.retType, bAsFuncType.retType)){
					return false;
				}
				
			}
		}
		else{
			if(aType instanceof GenericType && bType instanceof GenericType){
				GenericType aGen = ((GenericType)aType);
				GenericType bGen = ((GenericType)bType);
				
				return aGen.name.equals(bGen.name);
			}
			
			return false;
		}
		
		
		return true;
	}

	private static String join(String inter, Collection<?> c)
	{
		StringBuilder sb = new StringBuilder();
		
		int len = c.size();
		int n=0;
		for(Object o : c)
		{
			sb.append(o.toString());
			if(n != len-1)
			{
				sb.append(inter);
			}
			n++;
		}
		
		return sb.toString();
		
	}

	public static Type checkAssignmentCanBeDone(ErrorRaiseable invoker, Type lhs, Type rhs) {
		return checkAssignmentCanBeDone(invoker, AssignStyleEnum.EQUALS, lhs, rhs, 0,0,0,0, null);
	}
	
	public static Type checkAssignmentCanBeDone(ErrorRaiseable invoker, AssignStyleEnum op, Type lhs, Type rhs, 
			int lhsLine, int lhsColumn,
			int rhsLine, int rhsColumn, String errorPostfix)
	{
		if(rhs == null || lhs == null)
		{
			return null;
		}
		
		if(op.isEquals()){
			if(rhs instanceof NamedTypeMany)
			{
				NamedTypeMany many = (NamedTypeMany)rhs;
				HashSet<NamedType> manys =  many.getMany();
				for(NamedType tr : manys)
				{
					ErrorRaiseableSupressErrorsAndLogProblem errs = new ErrorRaiseableSupressErrorsAndLogProblem(invoker);
					Type ret = checkSubType(errs, lhs, tr, lhsLine, lhsColumn, rhsLine, rhsColumn);
					if(!errs.isHasErrored() && null != ret)
					{
						return ret;
					}
				}
				invoker.raiseError(lhsLine, lhsColumn, String.format("Type mismatch: cannot convert any of: %s to %s", join(", ", manys), lhs) );
				return null;//no match
			}
			else if(rhs instanceof FuncTypeMany)
			{
				FuncTypeMany many = (FuncTypeMany)rhs;
				//ErrorRaiseable sup = invoker.getErrorRaiseableSupression();
				List<FuncType> manys =  many.getMany();
				
				boolean justOne = manys.size() == 1;
				ErrorRaiseable er = justOne? invoker: new ErrorRaiseableSupressErrorsAndLogProblem(invoker);
				
				for(FuncType tr : manys)
				{
					Type ret = checkSubType(er, lhs, tr, lhsLine, lhsColumn, rhsLine, rhsColumn);
					if (null != ret) {
						return ret;
					}
				}
				if(!justOne){//if just one then the above will have catered for this
					invoker.raiseError(lhsLine, lhsColumn, String.format("Type mismatch: cannot convert any of: %s to %s %s", join(", ", manys), lhs) );
				}
				
				return null;//no match
			}
			else
			{
				return checkSubType(invoker, lhs, rhs, lhsLine, lhsColumn, rhsLine, rhsColumn);
			}
		}
		else if(op == AssignStyleEnum.OR_EQUALS || op == AssignStyleEnum.AND_EQUALS){
			assertThingIsBoolean(invoker, lhs, lhsLine, lhsColumn, errorPostfix);
			return lhs;
		}
		else if(op == AssignStyleEnum.PLUS_EQUALS && isString(lhs) )	{
			return lhs;
		}
		else if(op == AssignStyleEnum.LSH || op == AssignStyleEnum.RSH || op == AssignStyleEnum.RHSU  )	{
			return checkNumericalOpGeneric(invoker, lhs, rhs, lhsLine, lhsColumn, rhsLine, rhsColumn, "bit shift", errorPostfix, true, false);
		}
		else if(op == AssignStyleEnum.BAND || op == AssignStyleEnum.BOR || op == AssignStyleEnum.BXOR  )	{
			return checkNumericalOpGeneric(invoker, lhs, rhs, lhsLine, lhsColumn, rhsLine, rhsColumn, "bitwise", errorPostfix, true, false);
		}
		else{//+= - lhs string and rhs string
			return checkNumericalInfix(invoker, lhs, rhs, lhsLine, lhsColumn, rhsLine, rhsColumn, errorPostfix);
		}
		//return checkSubtype(invoker, op, lhs, rhs,lhsLine, lhsColumn, rhsLine, rhsColumn, "Assignment");
	}
	
	private static void assertThingIsBoolean(ErrorRaiseable invoker, Type lhs, int lhsLine, int lhsColumn, String errorPostfix){
		lhs = TypeCheckUtils.unboxTypeIfBoxed(lhs);
		
		if(lhs instanceof PrimativeType){
			PrimativeType asPrim = (PrimativeType)lhs;
			if(asPrim.type == PrimativeTypeEnum.BOOLEAN){
				return;
			}
		}
		
		invoker.raiseError(lhsLine, lhsColumn, "Expected boolean" + errorPostfix);
		
	}
	
	private static final ErrorRaiseableSupressErrors suppersor = new ErrorRaiseableSupressErrors(null);
	
	/*
	public static Type checkSubtype(ErrorRaiseable invoker, AssignStyleEnum op, Type lhs, Type rhs, 
			int lhsLine, int lhsColumn,
			int rhsLine, int rhsColumn, String msg)
	{
		if(op == AssignStyleEnum.EQUALS)
		{
			if(!isTypeDeterminableRAndL(invoker, lhs, rhs, lhsLine, lhsColumn, rhsLine, rhsColumn))
			{
				return null;
			}
			else
			{
				Type unboxedLhs = unboxTypeIfBoxed(lhs);
				Type unboxedRhs = unboxTypeIfBoxed(rhs);
				
				if((unboxedLhs instanceof PrimativeType && 
						!(unboxedRhs instanceof PrimativeType))
						|| 
					(!(unboxedLhs instanceof PrimativeType) && 
							unboxedRhs instanceof PrimativeType)
						)
				{
					invoker.raiseError(lhsLine, lhsColumn, String.format("%s type of %s not compatible with %s", msg, unboxedLhs.getPrettyName(), unboxedRhs.getPrettyName()) );
					return null;
				}
				else if(unboxedLhs instanceof PrimativeType && 
						unboxedRhs instanceof PrimativeType )
				{
					PrimativeType rhsToAssign = checkNumericalOpGeneric(invoker, unboxedLhs, unboxedRhs, lhsLine, lhsColumn, rhsLine, rhsColumn, msg);
					if(null != rhsToAssign)
					{
						return checkLHSPrimCanBeAssigned(invoker, (PrimativeType)unboxedLhs, rhsToAssign, lhsLine, lhsColumn);
					}
					else
					{
						return null;
					} 
				}
				else
				{//both are non primative
					//ensure types are compatible
					return null;
				}
			}
		}
		else
		{
			//has to be numerical... or string can do the += case...
			return checkNumericalInfix(invoker, lhs, rhs, lhsLine, lhsColumn, rhsLine, rhsColumn);
		}
	}
	*/
	
	public static Type checkLogicalAndOrInfix(ErrorRaiseable invoker, Type lhs, Type rhs, 
			int lhsLine, int lhsColumn,
			int rhsLine, int rhsColumn, String postfixErrMsg) {
		
		Pair<PrimativeType, PrimativeType> landr = leftAndRightMustBeDetAndPrim(invoker, lhs, rhs, lhsLine, lhsColumn, rhsLine, rhsColumn, postfixErrMsg);
		
		if(null == landr) return null;
		
		PrimativeType lhsPrim = landr.getA();
		PrimativeType rhsPrim = landr.getB();
		
		if(lhsPrim.type != PrimativeTypeEnum.BOOLEAN)
		{
			invoker.raiseError(lhsLine, lhsColumn, String.format("boolean operation cannot be performed on type %s%s" , lhsPrim.type, postfixErrMsg));
			return null;
		}
		
		if(rhsPrim.type != PrimativeTypeEnum.BOOLEAN)
		{
			invoker.raiseError(rhsLine, rhsColumn, String.format("boolean operation cannot be performed on type %s%s" , lhsPrim.type, postfixErrMsg));
			return null;
		}
		
		return lhsPrim;
	}
	
	public static void assertInteger(ErrorRaiseable invoker, Type given, int givenLine, int givenColumn, String postfixError)
	{
		if(TypeCheckUtils.hasRefLevelsAndNotLocked(given)){
			given = TypeCheckUtils.getRefType(given);
		}
		
		if(given instanceof PrimativeType)
		{
			if(((PrimativeType)given).type.equals(PrimativeTypeEnum.INT))
			{
				return;
			}else if(((PrimativeType)given).type.equals(PrimativeTypeEnum.SIZE_T))
			{
				return;
			}
		}
		else if(given instanceof NamedType && ((NamedType)given).toString().equals("java.lang.Integer"))  {
			return;
		}
		
		invoker.raiseError(givenLine, givenColumn, "Expected type of int, Integer or size_t but recieved: " + given + postfixError);
	}
	
	public static boolean isValidType(ErrorRaiseable invoker, Type given, int givenLine, int givenColum){
		return isValidType( invoker,  given,  givenLine,  givenColum, false);
	}
	
	public static boolean isVoidPrimativeThrown(  Type given){
		if(given instanceof PrimativeType)
		{
			PrimativeType asPrim = (PrimativeType)given;
			
			if(asPrim.type.equals(PrimativeTypeEnum.VOID) )
			{
				if(asPrim.thrown){
					return true;//its ok!
				}
			}
		}
		return false;
	}
	
	private final static NamedType Str_const_nt = new NamedType(new ClassDefJava(String.class));
	
	/**
	 * no lambda, no void only real primatives
	 */
	public static boolean isPurePrimative(Type given){
		if(given instanceof PrimativeType){
			PrimativeType asPrim = (PrimativeType)given;
			return asPrim.type != PrimativeTypeEnum.LAMBDA && asPrim.type != PrimativeTypeEnum.VOID;
		}
		
		return false;
	}
	

	public static boolean isNonArrayStringOrPrimative(Type given){
		if(given.hasArrayLevels()){
			return false;
		}
		
		if(given instanceof PrimativeType){
			PrimativeType asPrim = (PrimativeType)given;
			return asPrim.type != PrimativeTypeEnum.LAMBDA && asPrim.type != PrimativeTypeEnum.VOID;
		}
		
		return given.equals(Str_const_nt);
	}
	
	public static boolean shouldNotBeCopied(Type given, boolean shared){
		if(shared) {
			return true;
		}
		
		if(given instanceof NamedType) {
			ClassDef cd = ((NamedType)given).getSetClassDef();
			if(cd != null && cd.isShared) {
				return true;
			}
		}
		
		return isNonArrayStringOrPrimative(given);
	}
	
	public static boolean isTransientClass(Type given) {
		if(given != null && given instanceof NamedType) {
			ClassDef cd = ((NamedType)given).getSetClassDef();
			while(cd != null) {
				if(cd.isTransient) {
					return true;
				}
				cd = cd.getSuperclass();
			}
		}
		return false;
	}
	
	public static boolean isVoidPrimativePure(  Type given){
		if(given instanceof PrimativeType)
		{
			PrimativeType asPrim = (PrimativeType)given;
			
			if(asPrim.type.equals(PrimativeTypeEnum.VOID) )
			{
				if(!asPrim.thrown){
					return true;//its ok!
				}
			}
		}
		return false;
	}
	
	public static Type assertNotVoid(Type given, ErrorRaiseable invoker, int line, int column, String thing){
		if(isVoidPrimativePure(given)){
			invoker.raiseError(line, column, "unexpected type: void for " + thing);
		}
		
		return ScopeAndTypeChecker.const_boolean;
	}
	
	public static boolean isVoidAdObj( Type given){
		return given instanceof NamedType && ((NamedType)given).toString().equals("java.lang.Void");
	}
	
	public static boolean isVoid( Type given){
		return isVoidPrimativePure(given) || isVoidAdObj(given);
	}
	
	public static boolean isValidType(ErrorRaiseable invoker, Type given, int givenLine, int givenColumn, boolean isVoidIDOK) 
	{
		if(given == null)
		{
			//if(null!= invoker){ invoker.raiseError(givenLine, givenColumn, "unable to determine type"); }
			return false;
		}
		else if(given instanceof PrimativeType)
		{
			PrimativeType asPrim = (PrimativeType)given;
			
			if(asPrim.type.equals(PrimativeTypeEnum.VOID) )
			{
				if(isVoidIDOK && asPrim.thrown){
					return true;//its ok!
				}
				
				return false;
			}
		}else if(given instanceof ModuleType) {
			return false;
		}
		
		/*else if(given instanceof VarNull) {
			return false;
		}*/
	
		/*else if(isVoidAdObj(given)){
			return false;
		}*/
		return true;
	}
	
	public static boolean isValidType(Type given)
	{
		return isValidType(null, given, 0, 0, false);
	}
	
	public static boolean isValidType(Type given, boolean isVoidIDOK)
	{
		return isValidType(null, given, 0, 0, isVoidIDOK);
	}

	public static boolean isLambda(Type type) { 
		if(TypeCheckUtils.hasRefLevelsAndNotLocked(type)){
			return isLambda(TypeCheckUtils.getRefType(type));
		}
		
		return type instanceof FuncType 
				|| (type instanceof PrimativeType && ((PrimativeType)type).type == PrimativeTypeEnum.LAMBDA)
				//|| null != TypeCheckUtils.checkSubType(TypeCheckUtils.errorRaisableSupression, ScopeAndTypeChecker.const_lambda_nt, type)
				;
	}

	public static boolean isNamedTypeOrLambda(Type thing) {
		return thing!=null && (thing instanceof NamedType 
								|| thing instanceof FuncType 
								|| (thing instanceof PrimativeType && ((PrimativeType)thing).type == PrimativeTypeEnum.LAMBDA)
								|| thing.getArrayLevels()>1
							)
				;
	}

	public static void unlockAllNestedRefs(Type got) {
		if(TypeCheckUtils.hasRefLevels(got)){
			NamedType asNamed = (NamedType)got;
			asNamed.setLockedAsRef(false);
			unlockAllNestedRefs(((NamedType)got).getGenTypes().get(0));
		}
	}


	public static final ClassDefJava list_object_cls = new ClassDefJava(java.util.List.class, true);
	public static final Type list_object = new NamedType(new ClassDefJava(java.util.List.class, true));//JPT: move to global consts
	//public static final Type map_object = new NamedType(new ClassDefJava(java.util.Map.class, true));//JPT: move to global consts
	public static final Type set_object = new NamedType(new ClassDefJava(java.util.Set.class, true));//JPT: move to global consts
	public static final Type regSet_object = new NamedType(new ClassDefJava(ReferenceSet.class, true));//JPT: move to global consts
	
	public static final ClassDef Cls_DirectlyGettable = new ClassDefJava(DirectlyGettable.class, true);
	public static final ClassDef Cls_DirectlyArrayGettable = new ClassDefJava(DirectlyArrayGettable.class, true);
	public static final ClassDef Cls_DirectlyAssignable = new ClassDefJava(DirectlyAssignable.class, true);
	public static final ClassDef Cls_DirectlyArrayAssignable = new ClassDefJava(DirectlyArrayAssignable.class, true);
	
	private static boolean checkMapFromGenericRefType(NamedType asNamed){
		List<Type> tt = asNamed.getGenTypes();
		if(null != tt && !tt.isEmpty()){
			return TypeCheckUtils.hasRefLevels(tt.get(0));
		}
		
		return false;
	}
	
	public static boolean isRegistrationSet(ErrorRaiseable errorRaisableSupression, Type t) {
		return t instanceof NamedType && null != TypeCheckUtils.checkSubType(errorRaisableSupression, regSet_object, t, 0, 0, 0, 0);
	}
	
	public static boolean isList(ErrorRaiseable errorRaisableSupression, Type t, boolean checkHeldTypeIsRef) {
		boolean ret = t instanceof NamedType && null != TypeCheckUtils.checkSubType(errorRaisableSupression, list_object, t, 0, 0, 0, 0);
		if(ret && checkHeldTypeIsRef){
			ret = checkMapFromGenericRefType((NamedType)t);
		}
		return ret;
	}
	
	public static boolean isMap(ErrorRaiseable errorRaisableSupression, Type t, boolean checkHeldTypeIsRef) {
		boolean ret =  t instanceof NamedType && null != TypeCheckUtils.checkSubType(errorRaisableSupression, ScopeAndTypeChecker.map_object, t, 0, 0, 0, 0);
		if(ret && checkHeldTypeIsRef){
			ret = checkMapFromGenericRefType((NamedType)t);
		}
		return ret;
	}
	
	public static boolean isSet(ErrorRaiseable errorRaisableSupression, Type t, boolean checkHeldTypeIsRef) {
		boolean ret =  t instanceof NamedType && null != TypeCheckUtils.checkSubType(errorRaisableSupression, set_object, t, 0, 0, 0, 0);
		if(ret && checkHeldTypeIsRef){
			ret = checkMapFromGenericRefType((NamedType)t);
		}
		return ret;
	}

	public static boolean isLocalArray(Type exprType) {
		if(exprType instanceof NamedType){
			NamedType asNamed =  (NamedType)exprType;
			return asNamed.getIsRef() && asNamed.hasArrayLevels();
		}
		return false;
	}

	public static boolean isGenericAny(Type unRefExpType) {
		if(unRefExpType instanceof NamedType){
			NamedType ntr = (NamedType)unRefExpType;
			return ntr.isWildCardAny;
		}
		return false;
	}

	public static boolean assertRefIFaceImpl(ErrorRaiseable invoker, String err, int line, int column, Type rhs, int to, boolean allowLocked, ClassDef... clses) {
		int n=0;
		int refLevels = TypeCheckUtils.getRefLevels(rhs);
		boolean ret = false;
		
		
		while( (to==-1 || (refLevels-to) > n) && null != rhs && rhs instanceof NamedType && (allowLocked?TypeCheckUtils.getRefLevels(rhs):TypeCheckUtils.getRefLevelsIfNoeLockedAsRef(rhs)) >0){//TODO: remove null check here as catered for by instnaceof?
			ret=true;
			ClassDef rhsCls = ((NamedType)rhs).getSetClassDef();
			if(rhsCls !=null){
				
				HashSet<ClassDef> implIfaces = rhsCls.getTraitsIncTrans();
				
				boolean containsNone = true;
				
				for(ClassDef cls : clses){
					if(implIfaces.contains(cls)){
						containsNone=false;
						break;
					}
				}
				
				if(containsNone){
					if(null != invoker){
						invoker.raiseError(line, column, err);
					}
					return false;
				}
				
			}
			
			rhs = ((NamedType)rhs).getGenTypes().get(0);
			n++;
		}
		return ret;
	}
	
	public static boolean isRefArrayGettable(Type rhs, int to){
		//return assertRefIFaceImpl(null, null, 0, 0 ,rhs, to, true, Cls_DirectlyArrayGettable);
		if(TypeCheckUtils.hasRefLevels(rhs)){
			ClassDef rhsCls = ((NamedType)rhs).getSetClassDef();
			if(rhsCls !=null){
				HashSet<ClassDef> implIfaces = rhsCls.getTraitsIncTrans();
				
				if(implIfaces.contains(Cls_DirectlyArrayGettable)){
					return true;
				}
			}
		}
		
		return false;
	}
	
	public static boolean isRefArraySettable(Type rhs, int to){
		//return assertRefIFaceImpl(null, null, 0, 0 ,rhs, to, true, Cls_DirectlyArrayAssignable);
		if(TypeCheckUtils.hasRefLevels(rhs)){
			ClassDef rhsCls = ((NamedType)rhs).getSetClassDef();
			if(rhsCls !=null){
				HashSet<ClassDef> implIfaces = rhsCls.getTraitsIncTrans();
				
				if(implIfaces.contains(Cls_DirectlyArrayAssignable)){
					return true;
				}
			}
		}
		
		return false;
	}
	
	public static boolean isRefArray(Type rhs){
		if(TypeCheckUtils.hasRefLevels(rhs)){
			ClassDef rhsCls = ((NamedType)rhs).getSetClassDef();
			if(rhsCls !=null){
				HashSet<ClassDef> implIfaces = rhsCls.getTraitsIncTrans();
				
				if(implIfaces.contains(Cls_DirectlyArrayAssignable) && implIfaces.contains(Cls_DirectlyArrayGettable)){
					return true;
				}
			}
		}
		
		return false;
	}
	
	public static boolean isRefDirectlySettable(Type rhs, int to){
		return assertRefIFaceImpl(null, null, 0, 0 ,rhs, to, true, Cls_DirectlyAssignable);
	}
	
	public static boolean assertRefIsGettable(ErrorRaiseable invoker, int line, int column, Type rhs, int to) {
		
		return assertRefIFaceImpl(invoker, String.format("Type mismatch: cannot extract value from ref type: %s as it does not implement the DirectlyGettable interface", rhs), line, column, rhs, to, false, Cls_DirectlyGettable, Cls_DirectlyArrayGettable);
	}
	
	public static boolean assertRefIsAssignable(ErrorRaiseable invoker, int line, int column, Type rhs, int to) {
		
		return assertRefIFaceImpl(invoker, String.format("Type mismatch: set value to ref type: %s as it does not implement the DirectlyAssignable interface", rhs), line, column, rhs, to, false, Cls_DirectlyAssignable);
	}

	public static void assertAllSubtypeIfUnrefNeeded(ErrorRaiseable invoker, int line, int column, Type lhs, Map<Type, Pair<Integer, Integer>> offenders) {
		boolean proc = false;
		for(Type x : offenders.keySet()){
			if(TypeCheckUtils.hasRefLevels(x)){
				proc = true;
				break;
			}
		}
		
		if(proc){
			int lhsRefLevels = TypeCheckUtils.getRefLevels(lhs);
			for(Type x : offenders.keySet()){//check each to ensure that they can be unreffed to the lsh thing if needs be
				Pair<Integer, Integer> lc = offenders.get(x);
				
				int rhsRefs = TypeCheckUtils.getRefLevels(x);
				if(lhsRefLevels > rhsRefs){//if there are not enough ref levels, then add them as default Local type
					int toAdd = lhsRefLevels-rhsRefs;
					int n=0;
					while(n++ < toAdd){
						x = new NamedType(lc.getA(), lc.getB(), x);
					}
				}
				
				if(null == checkSubType(invoker, lhs, x, line, column, lc.getA(), lc.getB(), false))
				{
					return;
				}
				
			}
		}
	}

	public static Type convertFromGenericToNamedType(Type retTupe) {
		if(retTupe instanceof GenericType){
			GenericType asGen = (GenericType)retTupe;
			NamedType newRet = asGen.upperBound.copyTypeSpecific();
			newRet.setArrayLevels(asGen.getArrayLevels());
			return newRet;
		}
		else{
			return retTupe;
		}
	}

	public static boolean typeRequiresLocalArrayConvertion(Type expect) {
		if(expect instanceof NamedType){
			NamedType nt = (NamedType)expect;
			NamedType ub = nt.getOrigonalGenericTypeUpperBound();
			if(null != ub){
				return ub.hasArrayLevels() && nt.getIsRef();
			}
		}
		
		return false;
	}

	public static boolean isActor(ErrorRaiseable er, NamedType nt) {
		return null != TypeCheckUtils.checkSubType(er.getErrorRaiseableSupression(), ScopeAndTypeChecker.const_actor, nt, 0, 0, 0, 0);
	}
	
	public static boolean isTypedOrUntypedActor(ErrorRaiseable er, NamedType nt) {//TODO: check just one?
		return nt.isDefaultActor || null != TypeCheckUtils.checkSubType(er.getErrorRaiseableSupression(), ScopeAndTypeChecker.const_typed_actor, nt, 0, 0, 0, 0) || null != TypeCheckUtils.checkSubType(er.getErrorRaiseableSupression(), ScopeAndTypeChecker.const_actor, nt, 0, 0, 0, 0);
	}
	
	public static ErrorRaiseableSupressErrors dummyErrors = new ErrorRaiseableSupressErrors(null);
	
	public static boolean isTypedActor(ErrorRaiseable er, NamedType nt) {
		return nt.isDefaultActor || null != TypeCheckUtils.checkSubType(er.getErrorRaiseableSupression(), ScopeAndTypeChecker.const_typed_actor, nt, 0, 0, 0, 0);
	}
	
	public static boolean isTypedActorExactly(ErrorRaiseable er, NamedType nt) {
		return nt.isDefaultActor;
	}

	public static NamedType extractRootActor(Type from) {
		NamedType rootActorType = (NamedType) from;
		while(rootActorType != null && !ScopeAndTypeChecker.const_typed_actor_class.equals(rootActorType.getSetClassDef())){//trace all the way back to the actor
			rootActorType = rootActorType.getSetClassDef().getSuperAsNamedType(0, 0);
		}
		
		return rootActorType;
	}

	public static Type shiftTypeToUpperBounds(Type actingOnType) {
		//all geneics in named type are returned as their upper bound
		if(actingOnType instanceof NamedType){
			NamedType asNamed = (NamedType) actingOnType;
			asNamed = asNamed.copyTypeSpecific();
			
			ArrayList<Type> genTypes = new ArrayList<Type>(asNamed.getGenTypes().size());
			for(Type tt : asNamed.getGenTypes()){
				genTypes.add( shiftTypeToUpperBounds(tt) );
			}
			
			asNamed.setGenTypes(genTypes);
			return asNamed;
		}
		else if (actingOnType instanceof GenericType){
			GenericType asGen = (GenericType)actingOnType;
			return shiftTypeToUpperBounds(asGen.getUpperBoundAsNamedType());//could it have more bindings?
		}else if (actingOnType instanceof FuncType){
			FuncType asFuncType = (FuncType)actingOnType;
			
			ArrayList<Type> orgii = asFuncType.getInputs();
			ArrayList<Type> inputs = new ArrayList<Type>(orgii.size());
			
			for(Type oo : orgii){
				inputs.add(shiftTypeToUpperBounds(oo));
			}
			FuncType ret = new FuncType( inputs, shiftTypeToUpperBounds(asFuncType.retType)); 
			ret.setNullStatus(actingOnType.getNullStatus());
			return ret;
		}
		else{
			return actingOnType;
		}
	}

	public static boolean isEnum(Type resolvesTo) {
		if(null != resolvesTo && resolvesTo instanceof NamedType){
			NamedType asNamed = (NamedType)resolvesTo;
			ClassDef setCd = asNamed.getSetClassDef();
			if(null != setCd){
				return setCd.isEnum;//naughty, should be via getter
			}
		}
		
		return false;
	}
	
	public static boolean isClass(Type resolvesTo) {
		if(resolvesTo instanceof NamedType){
			NamedType asNamed = (NamedType)resolvesTo;
			ClassDef setCd = asNamed.getSetClassDef();
			return (null != setCd && setCd.equals(ScopeAndTypeChecker.const_class));//naughty, should be via getter
		}
		
		return false;
	}

	public static boolean isAnnotation(Type resolvesTo) {
		if(resolvesTo instanceof NamedType){
			NamedType asNamed = (NamedType)resolvesTo;
			ClassDef setCd = asNamed.getSetClassDef();
			if(null != setCd){
				return setCd.isAnnotation();
			}
		}
		
		return false;
	}

	public static boolean isNonPrimativeArray(Type retType) {
		return retType.hasArrayLevels()&& !(retType instanceof PrimativeType);
	}

	public static Class<?> getPrimativeClassIfRelevant(Class<? extends Object> class1) {
		
		if(class1.equals(Boolean.class)
			|| class1.equals(Character.class)
			|| class1.equals(Byte.class)
			|| class1.equals(Short.class)
			|| class1.equals(Integer.class)
			|| class1.equals(Long.class)
			|| class1.equals(Float.class)
			|| class1.equals(Double.class)
			){
			try {
				return (Class<?>) class1.getField("TYPE").get(null);
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				return class1;//meh
			}
		}
		return class1;
	}

	public static boolean hasMethod(int line, int col, Type resolvedTo, String mname, ScopeAndTypeChecker errorRaiser) {
		if(null != resolvedTo && resolvedTo instanceof NamedType){
			NamedType asNamed = (NamedType)resolvedTo;
			Type got = TypeCheckUtils.getRefTypeToLocked(asNamed);
			if(got instanceof NamedType){
				asNamed = (NamedType)got;
				return !asNamed.getFuncDef(line, col, mname, new ArrayList<Type>(), null, errorRaiser).isEmpty();
			}
		}
		
		return false;
	}
	
	private static HashSet<String> genericsApplicableToFunction(FuncDef fd){
		HashSet<String> ret = new HashSet<String>();
		
		if(null != fd.methodGenricList){
			fd.methodGenricList.forEach(a -> ret.add(a.getA()));
		}//TODO: what about nested functions? having generic params...?
		
		if(null != fd.origin){
			ClassDef cd = fd.origin;
			while(cd != null){
				for(GenericType gt : fd.origin.classGenricList){
					ret.add(gt.name);
				}
				cd=cd.getParentNestor();
			}
		}
		
		return ret;
		
	}
	
	private final static ErrorRaiseable errorRaisableSupression = new ErrorRaiseableSupressErrors(null);
	
	public static boolean isFunction0(ErrorRaiseable ers, Type vaType ) {
		return null != TypeCheckUtils.checkSubType(TypeCheckUtils.errorRaisableSupression, ScopeAndTypeChecker.const_Function0, vaType)
				|| null != TypeCheckUtils.checkSubType(TypeCheckUtils.errorRaisableSupression, ScopeAndTypeChecker.const_Function0v, vaType);
	}
	
	private static Type typeIsAr1ZeroArgFuncType(ScopeAndTypeChecker satc, Type vaType) {
		if(!TypeCheckUtils.isValidType(vaType) || TypeCheckUtils.isVarNull(vaType)) {
			return null;
		}
		
		if(null != TypeCheckUtils.checkSubType(TypeCheckUtils.errorRaisableSupression, ScopeAndTypeChecker.getLazyNT(), vaType)) {
			//so check rhs value to lazy value needed for type...
			vaType = ((NamedType)vaType).getGenTypes().get(0);
		}
		
		if(vaType.getArrayLevels() != 1) {
			return null;
		}else {
			vaType = (Type)vaType.copy();
			vaType.setArrayLevels(0);
		}
		
		Type got = convertfuncTypetoNamedType(vaType, null);
		if(got instanceof NamedType) {
			NamedType lhsNT = (NamedType)got;
			Type retTypeOfLHS = null;
			if(TypeCheckUtils.isFunction0(TypeCheckUtils.errorRaisableSupression, lhsNT)) {
				retTypeOfLHS = lhsNT.getGenTypes().isEmpty()?ScopeAndTypeChecker.const_void:lhsNT.getGenTypes().get(0);
				
			}
			
			if(lhsNT.isInterface()) {//perhaps it's a SAM function?
				List<Pair<String, TypeAndLocation>> methods = lhsNT.getAllLocallyDefinedMethods(satc, true, false);
				if(methods.size() == 1) {
					retTypeOfLHS = ((FuncType)methods.get(0).getB().getType()).retType;
				}
			}
			
			if(null != retTypeOfLHS && !(retTypeOfLHS instanceof GenericType)) {
				return retTypeOfLHS;
			}
		}
		
		return null;
	}
	
	/**
	 * Maps named params, default values and varargs  
	 */
	@SuppressWarnings("unchecked")
	public static <Item> List<Item> mapFunctionParameterNamesToNewArguments(ScopeAndTypeChecker satc, Type funcactingOn, List<Item> argsWanted, ArrayList<Pair<String, Item>> namessMap, int argOffset, boolean itemIsType, boolean mapVATypes){
		if(funcactingOn instanceof FuncType){
			if(null == argsWanted){ argsWanted = new ArrayList<Item>(0); }
			
			FuncType asFuncType = (FuncType)funcactingOn;
			FuncDef fd = asFuncType.origonatingFuncDef;
			
			if(null == fd) {//e.g. can be null for funcrefs
				fd = FuncDef.build(asFuncType.retType, asFuncType.inputs.toArray(new Type[0]));
			}
			
			FuncParams params = fd.getParams();//null==fd?null:fd.getParams();
			
			if(fd.isGPUKernalFuncOrStub()) {
				//adjust all array params to be be pointers
				params = (FuncParams)params.copy();
				ArrayList<FuncParam> fps = params.params;
				for(int n=0; n < fps.size(); n++){
					Type normal = asFuncType.inputs.get(n).getTaggedType();
					fps.get(n).setTaggedType(normal);
				}
				
			}
			
			
			if( fd.extFunOn != null){
				params = (FuncParams)params.copy();
				params.params.add(0, new FuncParam(0, 0, "$extendeeType", fd.extFunOn, false));
				if(!itemIsType){
					argOffset++;
				}
			}
			
			ArrayList<String> funcParamNames = params.getAsNames();
			int sz = funcParamNames.size();
			
			HashMap<Integer, Pair<String, Item>> idxToNamedParam = new HashMap<Integer, Pair<String, Item>>();
			if(null != namessMap){
				for(Pair<String, Item> nameT : namessMap){
					String namex = nameT.getA();//ignore type checking for now
					for(int n = 0; n < sz; n++){
						String pname = funcParamNames.get(n);
						if(namex.equals(pname)){
							idxToNamedParam.put(n-argOffset, nameT);
							break;
						}
					}
				}
				
				if(namessMap.size() != idxToNamedParam.size()){//unable to match all... give up
					return argsWanted;
				}
			}
			
			int tryFuncParamsSize = asFuncType.argCount() /*params.params.size()*/ - argOffset;
			HashMap<Integer, Type> withDefault = new HashMap<Integer, Type>();
			HashMap<Integer, Type> withDefaultRealType = new HashMap<Integer, Type>();
			
			int vararg = -1;
			HashSet<String> decGenericTypes = genericsApplicableToFunction(fd);
			ArrayList<FuncParam> paramsz = params.params;
			int paramszsize = paramsz.size();
			for(int nz=0; nz < paramszsize; nz++){
				FuncParam fp = paramsz.get(nz);
				if(fp.defaultValue != null){
					Type got;
					if(!fp.defaultOk){//use lhs if there is a problem (i.e. foo(a String = 69) etc )
						got = fp.getTaggedType();
					}
					else{
						got = fp.defaultValue.getTaggedType();
						/*if(got instanceof VarNull) {
							got = ((VarNull)got).getTaggedTypeRaw();
						}*/
					}
					
					if(itemIsType && isVarNull(got) && typeContainsAGenericType(fp.getTaggedType(), decGenericTypes)){
						//got = fp.type;
						got = asFuncType.inputs.get(nz);//has the generic binding if there is one
					}else if(itemIsType && got instanceof VarNull) {
						Type gotx = ((VarNull)got).getTaggedTypeRaw();
						if(null != gotx) {
							got = gotx;
						}
					}
					
					withDefault.put(nz, got);
					withDefaultRealType.put(nz, fp.getTaggedType());
				}
				
				if(vararg == -1 && (fp.isVararg || (paramszsize-1 == nz && fp.getTaggedType().hasArrayLevels() && fp.defaultValue ==null))){//is vararg or is last with array type
					vararg = nz-argOffset;//expect only one of these
				}
			}
			
			if(vararg == -1 && tryFuncParamsSize>paramszsize && paramszsize==0 ){
				//if params missing from funcdef (i.e. annotations not present)
				int lastOne = asFuncType.argCount()-1;
				if(asFuncType.inputs.get(lastOne).hasArrayLevels()){
					vararg = lastOne;
				}
			}
			
			
			int minParamset = tryFuncParamsSize - withDefault.size() + (vararg > -1?-1:0);
			
			int totalArgCount = idxToNamedParam.size() + argsWanted.size();
			
			if(!((vararg > -1?true:tryFuncParamsSize >= totalArgCount) && totalArgCount >= minParamset)){
				return argsWanted;
			}
			
			int excessParamsForVarArg = 0; 
			
			if(vararg > -1) {
				excessParamsForVarArg = totalArgCount - tryFuncParamsSize;//arguments in excess that we can use
				if(excessParamsForVarArg < 0) {
					excessParamsForVarArg += withDefault.size();
				}
			}
			boolean excessVAParamsConsumed = false;
			//above is -1?

			int providedArgcount = argsWanted.size();
			ArrayList<Item> newargsWanted = new ArrayList<Item>(providedArgcount);
			int consumed=0;
			int consumedD=0;
			boolean divertToDefaultVersion = false;
			int itemsThatCanGoIntoDefaultSlots = providedArgcount - (tryFuncParamsSize - withDefault.size());
			
			int lastVararg=-1;
			boolean addedOneOnOne=false;
			for(int n=0; n < tryFuncParamsSize; n++){
				Pair<String, Item> item = idxToNamedParam.get(n);
				if(null != item){
					newargsWanted.add(item.getB());
				}
				else{//take from orig, non named args
					if(!excessVAParamsConsumed && vararg > -1 && vararg <= n && !idxToNamedParam.containsKey(vararg)){//also ensure that idx has not been mapped to a vararg
						Type fullyQualified = asFuncType.inputs.get(n+argOffset);
						Type vaType = params.params.size()==0?fullyQualified:params.params.get(n+argOffset).getTaggedType();
						
						Type canBeZeroArgLambdaRetType = typeIsAr1ZeroArgFuncType(satc, vaType);
						
						NamedType isLazy = null;
						if(vaType instanceof NamedType && null != TypeCheckUtils.checkSubType(errorRaisableSupression, ScopeAndTypeChecker.getLazyNT(), vaType)) {
							isLazy = (NamedType)vaType;
							vaType = isLazy.getGenericTypeElements().get(0);
						}
						
						
						if(!vaType.hasArrayLevels()){
							return argsWanted;//fail
						}
						
						boolean vaArgsIsArrayRef = false;
						if(TypeCheckUtils.hasArrayLevelsWhenUnrefed(vaType)) {
							vaArgsIsArrayRef=true;
						}
						
						Type vaTypeWithoutAr = (Type)vaType.copy();
						vaTypeWithoutAr.setArrayLevels(vaTypeWithoutAr.getArrayLevels()-1);
						
						if(itemIsType){
							ArrayList<Type> vatypes = new ArrayList<Type>();
							for(int i=0; i <= excessParamsForVarArg  ; i++){
								if(addedOneOnOne) {
									return argsWanted;//fail, already added oneOnOne for vararg, cannot consume more
								}
								
								if(providedArgcount <= consumed){
									return argsWanted;//fail, there is no match here
								}
								
								//if()
								
								Object got = argsWanted.get(consumed + i);
								
								if(got == null){
									return argsWanted;
								}
								
								Type argType = (Type)got;
								if(TypeCheckUtils.checkSubType(errorRaisableSupression, vaType, argType, 0, 0, 0, 0) != null){
									//1:1 match on array type
									
									
									//check lazy	
									if(isLazy != null) {
										got = convertInputArgToRequiredType(satc, isLazy, itemIsType, (Item)got, argsWanted, consumed + i);
									}
									
									
									if(excessParamsForVarArg - withDefault.size() == 0){
										return argsWanted;//fail
									}else if(!vatypes.isEmpty()){
										return argsWanted;//fail, already things within in
									}
									
									vatypes.add(vaTypeWithoutAr);
									addedOneOnOne = true;
									continue;
								}
								
								if(!vaTypeWithoutAr.equals(argType)){//not equals
									if(TypeCheckUtils.checkSubType(errorRaisableSupression, vaTypeWithoutAr, argType, 0, 0, 0, 0) == null){
										boolean fail = true;
										if(null != canBeZeroArgLambdaRetType) {
											if(null != TypeCheckUtils.checkSubType(errorRaisableSupression, canBeZeroArgLambdaRetType, argType)) {
												got = convertInputArgToRequiredType(satc, vaTypeWithoutAr, itemIsType, (Item)got, argsWanted, consumed + i);
												fail = false;
											}
											
											
											
											
											if(fail && mapVATypes) {//e.g. vararg call like: takesSAM(def () { 34 }, def () {45}) to a sam type
												if(lhsIsSamType(satc, vaTypeWithoutAr, argType, false)) {
													fail=false;
												}
											}
										}
										
										if(fail) {
											return argsWanted;//fail
										}
									}else {
										asFuncType.varargsRequireUpCast = true;
									}
								}
								asFuncType.hasVarargs = true;
								Type vaTypeToAdd = vaTypeWithoutAr;
								if(vaTypeToAdd instanceof GenericType){
									if(argType instanceof PrimativeType){
										vaTypeToAdd = TypeCheckUtils.boxTypeIfPrimative(argType, false);
									}else{
										vaTypeToAdd = ((GenericType)vaTypeToAdd).getUpperBoundAsNamedType();							
									}
								}
								
								vatypes.add(vaTypeToAdd);
							}
							
							if(!vatypes.isEmpty()){
								if(asFuncType.varargsRequireUpCast || vaArgsIsArrayRef) {
									ArrayList<Type> newVATypes = new ArrayList<Type>();
									for(Type tt: vatypes) {
										if(TypeCheckUtils.hasRefLevels(tt)) {
											tt = (Type)tt.copy();
											((NamedType)tt).setLockedAsRef(true);
										}
										newVATypes.add(tt);
									}
									vatypes = newVATypes;
								}
								
								Type foundVaType =  TypeCheckUtils.getMoreGeneric(errorRaisableSupression, null, 0,0, vatypes, null );
								if(null == foundVaType){
									newargsWanted.add((Item)vaType);
								}else{
									foundVaType = (Type)foundVaType.copy();
									if(vaType instanceof GenericType || vaType instanceof NamedType){
										foundVaType = TypeCheckUtils.boxTypeIfPrimative(foundVaType, false);
									}
									
									foundVaType.setArrayLevels(foundVaType.getArrayLevels()+1);
									newargsWanted.add((Item)foundVaType);
								}
								
							}else{
								newargsWanted.add((Item)vaType);
								
								if(consumed <= argsWanted.size() - 1 && TypeCheckUtils.checkSubType(errorRaisableSupression, vaType, (Type)argsWanted.get(consumed), 0, 0, 0, 0) != null){
									consumed++;//consume+=1 unless we have created an empty varg array
								}
								
								
							}
							
						}
						else{//expression, so create a new expression
							ArrayList<Expression> arrayElements = new ArrayList<Expression>(excessParamsForVarArg<0?0:excessParamsForVarArg);
							boolean foundMatch = false;
							for(int i=0; i <= excessParamsForVarArg; i++){
								
								Item itemOfInterest = argsWanted.get(consumed + i);
								Node expr = (Node)itemOfInterest;
								Type argType = expr instanceof Type?(Type)expr:expr.getTaggedType();
								
								if(TypeCheckUtils.checkSubType(errorRaisableSupression, vaType, argType, 0, 0, 0, 0) != null){
									//1:1 match on array type
									
									//check lazy	
									if(isLazy != null) {
										itemOfInterest = convertInputArgToRequiredType(satc, isLazy, itemIsType, itemOfInterest, argsWanted, consumed + i);
										expr = (Node)itemOfInterest;
									}
									
									if(i!=0 || i != excessParamsForVarArg){//ensure that this is the first and only match (i.e. not 1, [1,2,3]  or [1,2,3],8 etc)
										return argsWanted;//fail
									}
									newargsWanted.add((Item)expr);
									foundMatch=true;
									break;
								}
								
								if(null == argType){
									return argsWanted;//fail
								}
								if(TypeCheckUtils.checkSubType(errorRaisableSupression, vaTypeWithoutAr, argType, 0, 0, 0, 0) == null){
									
									boolean fail=true;
									if(null != canBeZeroArgLambdaRetType) {
										if(null != TypeCheckUtils.checkSubType(errorRaisableSupression, canBeZeroArgLambdaRetType, argType)) {
											itemOfInterest = convertInputArgToRequiredType(satc, vaTypeWithoutAr, itemIsType, itemOfInterest, argsWanted, consumed + i);
											expr = (Node)itemOfInterest;
											
											Utils.inferAnonLambda(satc, (Node)expr, vaTypeWithoutAr);
											
											fail=false;
										}
									}
									
									if(fail) {
										return argsWanted;//fail
									}
								}
								
								if(expr instanceof Type){
									expr = new TypeReturningExpression((Type) expr); 
								}
								
								arrayElements.add((Expression)expr);
							}
							
							if(!foundMatch){
								Expression expr = providedArgcount > consumed?(Expression)argsWanted.get(consumed):null;
								Type argType = null!=expr?expr.getTaggedType():null;
								
								if(expr != null && TypeCheckUtils.checkSubType(errorRaisableSupression, vaType, argType, 0, 0, 0, 0) != null){
									newargsWanted.add((Item)expr);
								}
								else if(!arrayElements.isEmpty() || excessParamsForVarArg <= 0){//if there is nothing for us to add then create an empty list
									//if these was stuff to add then create typed arraydef
									
									boolean empty = arrayElements.isEmpty();
									Node ret;
									/*if(isLazy != null && empty) {
										ArrayList<Expression> arrayLevels = new ArrayList<Expression>();
										arrayLevels.add(new VarInt(0));
										ArrayConstructor ac = new ArrayConstructor(0,0, vaTypeWithoutAr, arrayLevels, null);
										ac.setTaggedType(vaType);
										ret = ac;
									} else {*/
										ArrayDef ad = new ArrayDef(0, 0, arrayElements);
										ad.isArray=true;
										ad.setTaggedType(vaType);
										ad.emptyArrayOk=true;
										ret=ad;
										/*ret = new CastExpression(0,0, vaType, (Expression)ret);
										ret.setTaggedType(vaType);*/
									//}
									
									//check lazy	
									if(isLazy != null) {
										ret = (Node)convertInputArgToRequiredType(satc, isLazy, itemIsType, (Item)ret, argsWanted, consumed, empty);
										//remove excess which have been included in the arraydef
										for(int nu = consumed+excessParamsForVarArg; nu > consumed; nu--) {
											argsWanted.remove(nu);
										}
									}
									
									newargsWanted.add((Item)ret);
								}
							}
						}
						lastVararg = newargsWanted.size();
						excessVAParamsConsumed=true;
						consumed+=excessParamsForVarArg+1;
						
					}
					else{
						boolean hazDefault = withDefault.containsKey(n+argOffset);
						if(!hazDefault){
							if(providedArgcount <= consumed){
								return argsWanted;//fail, there is no match here
							}
							else{
								Item wantedArg = argsWanted.get(consumed++);
								Type desired = ((FuncType) funcactingOn).getInputs().get(n + argOffset);
								
								
								
								wantedArg = convertInputArgToRequiredType(satc, desired, itemIsType, wantedArg, argsWanted, consumed-1);
								
								Pair<Boolean, Item> autoVectArg = autoVectorizeArg(itemIsType, wantedArg, desired);
								wantedArg = autoVectArg.getB();
								newargsWanted.add(wantedArg);
							}
						}
						else{//has a default
							Type itm = withDefault.get(n+argOffset);
							boolean needsdefault = true;
							
							
							boolean useDefault = itemsThatCanGoIntoDefaultSlots > consumedD ;
							if(!useDefault && consumed<argsWanted.size()) {
								if(itemsThatCanGoIntoDefaultSlots == 0 && vararg>=providedArgcount ) {
									//itemsThatCanGoIntoDefaultSlots+=1;
									//ensure type matches, then you can use it anway
									Item whatx = argsWanted.get(consumed);
									boolean canConsume = itm instanceof VarNull;
									if(!canConsume) {
										Pair<Boolean, Item> autoVectArg = autoVectorizeArg(itemIsType, whatx, itm);
										canConsume = autoVectArg.getA();
										whatx = autoVectArg.getB();
									}
									Type rhsType = whatx instanceof Type ? (Type)whatx : ((Node)whatx).getTaggedType();
									
									useDefault = (null != TypeCheckUtils.checkSubType(dummyErrors, withDefaultRealType.get(n+argOffset), rhsType));
									
									if(useDefault && consumed == argsWanted.size()-1) {
										excessParamsForVarArg--;//last one so there cannot be any varargs consumed after thing!
									}
									
								}
							}
									
							
							
							if( useDefault ){
								Item what = argsWanted.get(consumed);
								if(what == null) {
									return argsWanted;//fail 
								}
								
								boolean canConsume = itm instanceof VarNull;
								if(!canConsume) {
									Pair<Boolean, Item> autoVectArg = autoVectorizeArg(itemIsType, what, itm);
									canConsume = autoVectArg.getA();
									what = autoVectArg.getB();
								}
								//boolean cannotBeconsumed = false;
								
								Type defType = withDefaultRealType.get(n+argOffset);
										
								what = convertInputArgToRequiredType(satc, defType, itemIsType, what, argsWanted, consumed);
								
								Type rhsType = what instanceof Type ? (Type)what : ((Node)what).getTaggedType();
								
								boolean ignoreGens = false;
								Set<GenericType> gens = getNestedGenericTypes(defType);
								if(asFuncType.getLocalGenerics() != null && !asFuncType.getLocalGenerics().isEmpty()){
									ignoreGens = asFuncType.getLocalGenerics().stream().anyMatch(a -> gens.contains(a));
								}
								
								if(ignoreGens || TypeCheckUtils.hasReturnedUnqualifiedLocalGeneric(rhsType)) {
									if(null != checkSubType(dummyErrors, defType, rhsType, false, 0, 0, 0, 0, true, true, false, false, true)) {
										canConsume=true;
									}
								}else if(null == checkSubType(dummyErrors, defType, rhsType, false, 0, 0, 0, 0, true, true, false, false, true)) {
									canConsume=false;
								}
								
								if(!canConsume && rhsType instanceof FuncType) {
									FuncType asft = (FuncType)rhsType;
									ArrayList<AnonLambdaDefOrLambdaDef> anonLambdaSrcs = asft.anonLambdaSources;
									
									if(null != anonLambdaSrcs && !anonLambdaSrcs.isEmpty()) {
										AnonLambdaDefOrLambdaDef anon = anonLambdaSrcs.get(0);
										anon = (AnonLambdaDefOrLambdaDef)anon.copy();
										if(null != Utils.inferAnonLambda(satc, (Node)anon, defType)) {
											canConsume = true;
										}
									}
									
									if(asft.implementSAM != null ) {//infer anon lambda already
										canConsume = true;
									}
								}
								
								//canConsume=true;
								
								if(canConsume){
									//oh, we can consume an argument, if the type can be consumed though
									
									newargsWanted.add(what);
									consumed++;
									consumedD++;
									needsdefault=false;
								}
								else if(vararg > -1){
									//incorrect type, prehaps we can bind it on to the prevoius point if it's a vararg
									if(lastVararg > -1 && lastVararg == newargsWanted.size()){
										//prevous point was a vararg
										//so see if we can consume this item
										Item prevpoint = newargsWanted.get(lastVararg-1);
										Type itmx = (Type)getTypeFromItem(prevpoint).copy();
										itmx.setArrayLevels(itmx.getArrayLevels()-1);
										
										Item toConsume = argsWanted.get(consumed);
										Type toConsumeType = itemIsType?(Type)toConsume:((Expression)toConsume).getTaggedType();
										
										if(TypeCheckUtils.checkSubType(errorRaisableSupression, itmx, toConsumeType, 0, 0, 0, 0) != null){
											//oh nice it fits
											if(!itemIsType){
												ArrayDef ad = (ArrayDef)prevpoint;
												ad.getArrayElements(null).add((Expression)toConsume);
												
											}
											consumed++;
											consumedD++;
										}
										else{
											return argsWanted;//fail
										}
										
									}
									else if(n == vararg-1){//the next item is the vararg, so offset the consumption to factor this in
										excessParamsForVarArg++;
									}
								}else {//fail
									newargsWanted.add(what);
									return newargsWanted;//fail
								}
								
							}
							
							if(needsdefault){//use the default value type if we want that else blank it out with a null expression
								divertToDefaultVersion=true;
								//newargsWanted.add(itemIsType?(Item)itm:null);
								
								if(itemIsType) {
									itm = (Type)itm.copy();
									((Type)itm).setIgnoreForLocalGenericInference(true);
									newargsWanted.add((Item)itm);
								}
								else {
									newargsWanted.add(null);
								}
								
							}
						}
					}
				}
			}
			
			if(divertToDefaultVersion){
				ArrayList<Type> dvTypes = new ArrayList<Type>();
				for(FuncParam fp : params.params){
					dvTypes.add(fp.getTaggedType());
					if(fp.defaultValue != null){
						dvTypes.add(ScopeAndTypeChecker.const_defaultParamUncre);
					}
				}
				
				asFuncType.defaultFuncArgs = dvTypes;
			}
			
			argsWanted=newargsWanted;
		}
		return argsWanted;//HERE OK
	}
	
	public static boolean isLambdaaOrLikelyASAMTypeOrTuple(ScopeAndTypeChecker satc, Type lhs) {
		if(lhs instanceof FuncType) {
			return true;
		}else if(lhs instanceof NamedType) {
			NamedType asNamed = (NamedType)lhs;
			if(asNamed.isInterface()) {
				List<Pair<String, TypeAndLocation>> methods = asNamed.getAllLocallyDefinedMethods(satc, true, false);

				if(methods.size() == 1) {
					return true;
				}
			}else {
				NamedType tupleType = ScopeAndTypeChecker.getTupleNamedType(false);
				if(tupleType != null && null != TypeCheckUtils.checkSubType(null, tupleType, lhs)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private static boolean lhsIsSamType(ScopeAndTypeChecker satc, Type lhs, Type rhs, boolean checklambdaUpgradable) {
		if(lhs instanceof NamedType){
			NamedType asNamed = (NamedType)lhs;
			if(asNamed.isInterface()) {
				List<Pair<String, TypeAndLocation>> methods = asNamed.getAllLocallyDefinedMethods(satc, true, false);
				
				if(methods.size() == 1) {
					Pair<String, TypeAndLocation> inst = methods.get(0);
					FuncType samType = (FuncType)inst.getB().getType();
					if(null != TypeCheckUtils.checkSubType(errorRaisableSupression, samType, rhs)) {
						return true;
					}else if(checklambdaUpgradable) {
						if(samType.inputs.isEmpty()) {
							if(null != TypeCheckUtils.checkSubType(errorRaisableSupression, samType.retType, rhs)) { 
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private static <Item> Item convertInputArgToRequiredType(ScopeAndTypeChecker satc, Type desired, boolean itemIsType, Item wantedArg, List<Item> argsWanted, int consumed) {
		return convertInputArgToRequiredType( satc,  desired,  itemIsType,  wantedArg,  argsWanted,  consumed, false);
	}
	
	/**
	 * e.g. convert to lazy, convert to funcdef
	 */
	private static <Item> Item convertInputArgToRequiredType(ScopeAndTypeChecker satc, Type desired, boolean itemIsType, Item wantedArg, List<Item> argsWanted, int consumed, boolean supressReplace) {
		if(satc != null && wantedArg != null && !(desired instanceof VarNull )) {
			Type wantedtype = itemIsType?(Type)wantedArg:((Node)wantedArg).getTaggedType();
			
			if(wantedArg instanceof Type != itemIsType ) {
				return wantedArg;
			}
			
			if(wantedtype != null && desired != null && TypeCheckUtils.stripInOutParams((Type)wantedtype.copy()).equals(TypeCheckUtils.stripInOutParams((Type)desired.copy()))) {
				return wantedArg;
			}
			
			boolean doit = !(!itemIsType && wantedArg instanceof Type);
			if(itemIsType && ((Type)wantedArg).getIsTypeInFuncref()) {
				doit=false;
			}
			
			if(doit) {
				Type convertRetTo = TypeCheckUtils.canConvertRHSToArgLessLambda(satc, errorRaisableSupression, desired, wantedtype);
				if(null != convertRetTo) {
					Expression asExpr;
					if(itemIsType) {
						asExpr = new TypeReturningExpression((Type)wantedArg);
					}else {
						asExpr = (Expression)wantedArg;
					}
					
					Pair<Type, Expression> conv = satc.convertExprToArgLessLambda(((Node)wantedArg).getLine(), ((Node)wantedArg).getColumn(), convertRetTo, wantedtype, asExpr);
					if(null != conv) {
						if(itemIsType) {
							wantedArg = (Item)desired;
						}else {
							wantedArg = (Item)conv.getB();
							if(!supressReplace) {
								argsWanted.set(consumed, wantedArg);//splice back into the calling code
							}else {
								argsWanted.add(consumed, wantedArg);
							}
						}
					}
				}
				
				if(null != TypeCheckUtils.checkSubType(errorRaisableSupression, ScopeAndTypeChecker.getLazyNT(), desired) ) {
					//wantedArg = (Item) ((Node)wantedArg).copy();
					Type tt = ((NamedType)desired).getGenericTypeElements().get(0);
					if(null != TypeCheckUtils.checkSubType(errorRaisableSupression, tt, wantedtype)){
						Expression asExpr;
						if(itemIsType) {
							asExpr = new TypeReturningExpression((Type)wantedArg);
						}else {
							asExpr = (Expression)wantedArg;
						}
						
						Pair<Type, Expression> conv = satc.convertExpressionToNewLazy(((Node)wantedArg).getLine(), ((Node)wantedArg).getColumn(), tt, wantedtype, asExpr, false);
						if(null != conv) {
							if(itemIsType) {
								wantedArg = (Item)desired;
							}else {
								wantedArg = (Item)conv.getB();
								if(!supressReplace) {
									argsWanted.set(consumed, wantedArg);//splice back into the calling code
								}else {
									argsWanted.add(consumed, wantedArg);
								}
							}
						}
					}
				}
			}
			
		}
		
		return wantedArg;
	}
	
	private static Type stripInOutParams(Type wantedtype) {
		if(null != wantedtype) {
			wantedtype.setInOutGenModifier(null);
			if(wantedtype instanceof FuncType) {
				FuncType asft = (FuncType)wantedtype;
				for(Type tt : asft.inputs) {
					stripInOutParams(tt);
				}
				stripInOutParams(asft.retType);
			}
			
		}
		
		return wantedtype;
	}

	public static <Item> Pair<Boolean, Item> autoVectorizeArg(boolean itemIsType, Item providedItem, Type wantedItem){
		if(wantedItem instanceof GenericType) {//no when generic
			return new Pair<Boolean, Item>(true, providedItem);
		}
		
		
		Type origRtye = getTypeFromItem(providedItem);
		if(null == origRtye) {
			return new Pair<Boolean, Item>(true, providedItem);
		}
		
		origRtye = (Type)origRtye.copy();
		Type rtye = (Type)origRtye.copy();
		Vectorization prev = rtye.setVectorized(null);
		
		if(prev!=null) {
			return new Pair<Boolean, Item>(true, providedItem);
		}
		
		boolean canConsume = TypeCheckUtils.checkSubType(errorRaisableSupression, wantedItem, rtye, 0, 0, 0, 0) != null;
		if(!canConsume) {
			ArrayList<Type> eTypes = TypeCheckUtils.extractIntermediateVectTypes(errorRaisableSupression, rtye, false);
			
			for(Type possibleMatch : eTypes) {
				canConsume = TypeCheckUtils.checkSubType(errorRaisableSupression, wantedItem, possibleMatch, 0, 0, 0, 0) != null;
				if(canConsume) {
					if(itemIsType) {
						rtye.setVectorized(Vectorization.NORMAL);
						providedItem = (Item)rtye;
					}else {
						if(!(providedItem instanceof Type) && !(providedItem instanceof Vectorized)){
							Vectorized vv =new Vectorized((Expression)providedItem);
							Type ttv = (Type)rtye.copy();
							ttv.setVectorized(Vectorization.NORMAL);
							vv.setTaggedType(ttv);
							providedItem = (Item)vv;
						}
					}
					break;
				}
			}
		}
		
		return new Pair<Boolean, Item>(canConsume, providedItem);
	}
	
	
	private static <Item> Type getTypeFromItem(Item itm){
		
		if(itm == null) {
			return null;
		}
		
		if(itm instanceof Type){
			return (Type)itm;
		}
		else{
			return ((Expression)itm).getTaggedType();
		}
	}
/*
	public static boolean isClassRef(Type tgto) {
		if(tgto instanceof FuncType){
			FuncType asfuncT = (FuncType)tgto;
			return asfuncT.isConstructorFuncType;
		}
		return false;
	}*/

	public static NamedType converToNamedType(Type constInput) {
		if(constInput instanceof NamedType){
			return (NamedType)constInput;
		}else if(constInput instanceof PrimativeType){
			return (NamedType)TypeCheckUtils.boxTypeIfPrimative(constInput, true);
		}else if(constInput instanceof GenericType){
			return (NamedType)((GenericType)constInput).getUpperBoundAsNamedType();
		}else if(constInput instanceof FuncType){
			return (NamedType)TypeCheckUtils.convertfuncTypetoNamedType(constInput, null);
		}
		return ScopeAndTypeChecker.const_object;
	}
	
	public static String filterOutActorCall(String name){
		return name.replace("$ActorCall", "").replace("$ActorSuperCall", "");
	}

	public static boolean containsGenericTypeRef(Type what){
		if(what instanceof GenericType){
			return true;
		}else if (what instanceof NamedType){
			for(Type thing : ((NamedType) what).getGenTypes()){
				if(containsGenericTypeRef(thing)){
					return true;
				}
			}
		}else if (what instanceof FuncType){
			FuncType asFT = (FuncType)what;
			for(Type inp : asFT.getInputs()){
				if(containsGenericTypeRef(inp)){
					return true;
				}
			}
			
			if(containsGenericTypeRef(asFT.retType)){
				return true;
			}
		}
		
		return false;
	}

	public static boolean isGPUBuffer(Type ttx) {
		if(ttx instanceof NamedType) {
			NamedType asnamed = (NamedType)ttx;
			ClassDef cd = asnamed.getSetClassDef();
			if(cd!= null) {
				boolean what = cd.isParentNestor(ScopeAndTypeChecker.const_GPUBuffer);
				return what;
			}
		}
		
		return false;
	}

	public static boolean isReifiedType(NamedType asNamed) {
		return null != TypeCheckUtils.checkSubType(TypeCheckUtils.errorRaisableSupression, ScopeAndTypeChecker.const_reifiedType, asNamed);
	}
	
	public static boolean typeIsTrait(Type theType) {
		if(theType instanceof NamedType) {
			NamedType supTypeNT = (NamedType)theType;
			ClassDef cd = supTypeNT.getSetClassDef();
			if(ClassDef.classHasTraitAnnotation(cd)) {
				return true;
			}
		}
		return false;
	}

	private static Type checkFunction0(ErrorRaiseable ers, Type got, Type rhsType) {
		if(got instanceof NamedType) {
			NamedType lhsNT = (NamedType)got;
		
			if(TypeCheckUtils.isFunction0(TypeCheckUtils.errorRaisableSupression, lhsNT)) {
				Type retTypeOfLHS = lhsNT.getGenTypes().isEmpty()?(Type)ScopeAndTypeChecker.const_void.copy():lhsNT.getGenTypes().get(0);
				
				if(retTypeOfLHS instanceof GenericType) {
					return null;
				}
				
				if(null != TypeCheckUtils.checkSubType(ers, retTypeOfLHS, rhsType)) {
					return retTypeOfLHS;
				}
			}
		}
		return null;
	}
	

	public static Type canConvertRHSToArgLessLambda(ScopeAndTypeChecker satc, ErrorRaiseable ers, Type lhsType, Type rhsType) {
		return canConvertRHSToArgLessLambda(satc, ers, lhsType, rhsType, false);
	}
	
	public static Type canConvertRHSToArgLessLambda(ScopeAndTypeChecker satc, ErrorRaiseable ers, Type lhsType, Type rhsType, boolean checkSAMDirectType) {
		if(!TypeCheckUtils.isValidType(lhsType) || TypeCheckUtils.isVarNull(lhsType)) {
			return null;
		}
		
		if(rhsType == null) {
			rhsType = (Type)ScopeAndTypeChecker.const_void.copy();
		}
		
		if(rhsType.equals(lhsType)) {
			return null;
		}
		
		rhsType = (Type)rhsType.copy();
		lhsType = (Type)lhsType.copy();
		if(null != TypeCheckUtils.checkSubType(ers, lhsType, rhsType)) {
			return null;
		}
		
		if(null != TypeCheckUtils.checkSubType(ers, ScopeAndTypeChecker.getLazyNT(), lhsType)) {
			//so check rhs value to lazy value needed for type...
			lhsType = ((NamedType)lhsType).getGenTypes().get(0);
		}
		
		Type got = convertfuncTypetoNamedType(lhsType, null);
		if(got instanceof NamedType) {
			NamedType lhsNT = (NamedType)got;
			Type res = checkFunction0(ers, got, rhsType);
			if(res != null) {
				/*if(res.equals(ScopeAndTypeChecker.const_void_CLnt)) {
					if(lhsType instanceof FuncType) {
						res = ((FuncType)lhsType).retType;
					}
				}*/
				
				
				return res;
			}
			
			if(lhsNT.isInterface()) {//perhaps it's a SAM function?
				List<Pair<String, TypeAndLocation>> methods = lhsNT.getAllLocallyDefinedMethods(satc, true, false);
				
				if(methods.size() == 1) {
					Pair<String, TypeAndLocation> inst = methods.get(0);
					FuncType samret = (FuncType)inst.getB().getType();
					got = convertfuncTypetoNamedType(samret, null);
					
					res = checkFunction0(ers, got, rhsType);
					if(res != null) {
						return res;
					}else if(checkSAMDirectType) {
						if(rhsType instanceof FuncType) {
							rhsType = convertfuncTypetoNamedType(rhsType, null);
							if(null != TypeCheckUtils.checkSubType(ers, got, rhsType)) {
								return got;
							}
						}
					}
					
				}
			}
		}
		return null;
	}

	public static boolean eitherPointer(Type lhsType, Type rhsType) {
		return (null != lhsType && lhsType.getPointer() > 0)
					|| (null != rhsType && rhsType.getPointer() > 0);
	}
	
	private static boolean isPrimativeNotArray(Type tt) {
		return !tt.hasArrayLevels() && (tt instanceof PrimativeType);
	}
	
	public static boolean isNoNull(Type lhsType) {
		if(null == lhsType) {
			return false;
		}
		
		if(isPrimativeNotArray(lhsType)) {
			return true;
		}
		
		return lhsType.getNullStatus() == NullStatus.NOTNULL ;
	}
	
	public static boolean isNullable(Type lhsType) {
		if(null == lhsType) {
			return false;
		}
		
		if(isPrimativeNotArray(lhsType)) {
			return false;
		}
		
		return lhsType.getNullStatus() == NullStatus.NULLABLE;
	}
	
	public static boolean isUnknown(Type lhsType) {
		if(null == lhsType) {
			return false;
		}
		
		if(isPrimativeNotArray(lhsType)) {
			return false;
		}
		
		return lhsType.getNullStatus() == NullStatus.UNKNOWN;
	}
	
	public static boolean isNotNullable(Type lhsType) {
		if(null == lhsType) {
			return true;
		}
		
		if(isPrimativeNotArray(lhsType)) {
			return true;
		}
		
		NullStatus what = lhsType.getNullStatus();
		return what != NullStatus.NULLABLE && what != NullStatus.UNKNOWN; 
	}

	

	public static boolean regularEQ(GenericType me, GenericType asNamed) {
		if(me.getInOutGenModifier() != asNamed.getInOutGenModifier()) {
			return false;
		}
		
		if(!me.name.equals(asNamed.name)) {
			return false;
		}
		
		if(me.getArrayLevels() != asNamed.getArrayLevels()) {
			return false;
		}
		
		NamedType upBoundme = null;
		if(me.name.equals("?") || (me.upperBound != null && !me.upperBound.equals(ScopeAndTypeChecker.const_object))){
			upBoundme = me.upperBound;
		}
		
		NamedType upBoundAsNamed = null;
		if(asNamed.name.equals("?") || (asNamed.upperBound != null && !asNamed.upperBound.equals(ScopeAndTypeChecker.const_object))){
			upBoundAsNamed = asNamed.upperBound;
		}
		
		if(upBoundme!=null) {
			if(upBoundAsNamed == null) {
				return false;
			}else {
				if(!regularEQ(upBoundme, upBoundAsNamed)) {
					return false;
				}
			}
		}else if(upBoundAsNamed != null) {
			return false;
		}

		return true;
	}
	
	public static boolean regularEQ(NamedType me, NamedType asNamed) {

		InoutGenericModifier genmod = me.getInOutGenModifier();
		InoutGenericModifier genmodAN = asNamed.getInOutGenModifier();
		
		if(genmod != genmodAN) {
			return false;
		}
		
		if(me.getArrayLevels() != asNamed.getArrayLevels()) {
			return false;
		}
		
		if ((me.isWildCardAny || me.fromisWildCardAny) != (asNamed.isWildCardAny || asNamed.fromisWildCardAny)) {
			return false;
		}
		
		if(!Objects.equals(me.originRefType, asNamed.originRefType)) {
			return false;
		}
		
		if(me.getVectorized() != asNamed.getVectorized()) {
			return false;
		}
		
		boolean lhsRef = me.getIsRef();
		boolean rhsRef = asNamed.getIsRef();
		
		if(lhsRef || rhsRef) {
			if(lhsRef != rhsRef) {
				return false;
			}
			
			if(!me.getGenTypes().get(0).equals(asNamed.getGenTypes().get(0))) {
				return false;
			}
			
			return Objects.equals(me.classDef, asNamed.classDef);
		}
		
		boolean withGen = true;
		boolean cdSet = false;
		if(me.classDef != null && asNamed.classDef != null) {
			if(!me.classDef.equals(asNamed.classDef)) {
				return false;
			}
			cdSet=true;
			withGen = !me.classDef.equals(ScopeAndTypeChecker.const_enumClass);
		}else {//if(me.classDef == null && asNamed.classDef == null) {
			String lhs = null!=me.classDef?me.classDef.getPrettyName():me.namedType;
			String rhs = null!=asNamed.classDef?asNamed.classDef.getPrettyName():asNamed.namedType;
			
			if(!Objects.equals(lhs, rhs)) {
				return false;
			}
		}
		
		if(withGen){
			int glen = me.genTypes == null ? -1 : me.genTypes.size();
			int olen = asNamed.genTypes == null ? -1 : asNamed.genTypes.size();
			
			if(glen != olen) {
				return false;
			}else if(glen > 0) {
				
				if(cdSet) {
					HashMap<GenericType, Type> gotClassgenQMe = me.fromClassGenericToQualifiedType;
					HashMap<GenericType, Type> gotClassgenQAN = asNamed.fromClassGenericToQualifiedType;
					
					if(gotClassgenQMe != null) {
						if(gotClassgenQAN == null) {
							return false;
						}else{
							ClassDef parentME = me.classDef.getParentNestor();
							while(null != parentME){
								for(GenericType gen :parentME.classGenricList)
								{
									Type tme = gotClassgenQMe.get(gen);
									Type tan = gotClassgenQAN.get(gen);
									
									if(!regularEQ(tme, tan)) {
										return false;
									}
								}
								
								parentME  = parentME.getParentNestor();
							}
						}
					}else if(gotClassgenQAN != null) {
						return false;
					}
				}
				
				for(int n=0; n < glen; n++) {
					Type mine =  me.genTypes.get(n);
					Type their =  asNamed.genTypes.get(n);
					
					if(!regularEQ(mine, their)) {
						return false;
					}
					
				}
			}
		}
		
		return true;
	}
	

	public static boolean regularEQ(Type mine, Type their) {
		if(mine == null) {
			if(their != null) {
				return false;
			}
		}else if(their == null) {
			return false;
		}else {
			if(mine instanceof NamedType && their instanceof NamedType) {
				if(!regularEQ((NamedType)mine, (NamedType)their)) {
					return false;
				}
			}else if(mine instanceof GenericType && their instanceof GenericType) {
				if(!regularEQ((GenericType)mine, (GenericType)their)) {
					return false;
				}
			}else {
				if(!Objects.equals(mine, their)) {
					return false;
				}
			}
		}
		
		return true;
	}
	
}

