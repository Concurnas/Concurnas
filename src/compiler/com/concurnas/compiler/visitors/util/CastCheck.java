package com.concurnas.compiler.visitors.util;

import java.util.HashMap;
import java.util.HashSet;

import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.ast.GenericType;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.PrimativeType;
import com.concurnas.compiler.ast.PrimativeTypeEnum;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.VarNull;
import com.concurnas.compiler.visitors.ErrorRaiseable;
import com.concurnas.compiler.visitors.TypeCheckUtils;

public class CastCheck {

	private static final NamedType OBJ_NT = new NamedType(new ClassDefJava(Object.class));
	
	/*
	 public static Type checkCast(ErrorRaiseable er, int line, int column, Type castTo, Type exprTypeRhs, boolean IsCast)
	{//jls 5.5. Casting Conversion
		ErrorRaiseableSupressErrorsAndLogProblem erlog = new ErrorRaiseableSupressErrorsAndLogProblem(er);
		
		Type unRefCastTo = TypeCheckUtils.getRefType(castTo);
		Type unRefExpType = TypeCheckUtils.getRefType(exprTypeRhs);
		
		boolean pass = false;
		
		if(TypeCheckUtils.hasRefLevels(castTo) && TypeCheckUtils.hasRefLevels(exprTypeRhs)){
			//lhs and rhs type must match 1:1 if refs
			if(null!= unRefExpType && null!= unRefCastTo && !unRefCastTo.equals(unRefExpType)){
				pass=false;
			}
		}
		
		if(pass){

			Type isSub = TypeCheckUtils.checkSubType(erlog, unRefCastTo, unRefExpType, 0, 0, 0, 0);
			if(!erlog.isHasErrored() && null != isSub)
			{
				return isSub.equals(unRefCastTo)? castTo: exprTypeRhs;
			}
			
			
			if(castTo instanceof PrimativeType )
			{
				pass = checkCastToPrimativeType(er, line, column, (PrimativeType)castTo, exprTypeRhs);
			}
			else if(castTo instanceof NamedType || castTo instanceof GenericType )
			{
				if(castTo.getPrettyName().equals("java.lang.Object"))
				{
					pass = true;//everything can become an object
				}
				else if(TypeCheckUtils.isBoxedType(castTo)) //boxed type
				{
					pass = checkCastToBoxedType(er, line, column, (NamedType)castTo, exprTypeRhs);
				}
				else //not boxed, normal
				{
					if(castTo instanceof GenericType)
					{//TODO: this is a hack, you need to take the upper bound of the generic parameter
						pass = checkCastToOtherNamedType(er, line, column, OBJ_NT, exprTypeRhs);
					}
					else
					{
						pass = checkCastToOtherNamedType(er, line, column, (NamedType)castTo, exprTypeRhs);
					}
				}
			}
		}
		
		if(!pass)
		{
			if(IsCast){
				er.raiseError(line, column, String.format("Cannot cast from %s to %s", exprTypeRhs, castTo));
			}
			else{
				er.raiseError(line, column, String.format("Cannot compare an instance of %s with %s", exprTypeRhs, castTo));
			}
			
		}
		
		return castTo;
	} 
	 
	 */
	
	public static Type checkCast(ErrorRaiseable er, int line, int column, Type castTo, Type exprTypeRhs, boolean IsCast) 
	{//jls 5.5. Casting Conversion
		ErrorRaiseableSupressErrorsAndLogProblem erlog = new ErrorRaiseableSupressErrorsAndLogProblem(er);
		
		if(castTo==null || exprTypeRhs == null){
			return null;//uh oh!
		}
		
		Type castToOrig = castTo;
		exprTypeRhs = TypeCheckUtils.convertfuncTypetoNamedType(exprTypeRhs, castTo);
		castTo = TypeCheckUtils.convertfuncTypetoNamedType(castTo, null);
		
		Type unRefCastTo = (Type)TypeCheckUtils.getRefTypeIgnoreRefArray(castTo).copy();
		Type unRefExpType = (Type)TypeCheckUtils.getRefTypeIgnoreRefArray(exprTypeRhs).copy();

		
		boolean pass = true;
		boolean checkOnRefs = TypeCheckUtils.hasRefLevels(castTo) && TypeCheckUtils.hasRefLevels(exprTypeRhs);
		if(checkOnRefs){
			//lhs and rhs type must match 1:1 if refs
			if(null!= unRefExpType &&  !TypeCheckUtils.isGenericAny(unRefExpType)  && null!= unRefCastTo && !unRefCastTo.equals(unRefExpType)) {
				
				if(unRefExpType instanceof VarNull){
					int wantLEvels = TypeCheckUtils.getRefLevels(exprTypeRhs);
					
					//if rhs VarNull then convert to lhs!
					if(wantLEvels >0 && wantLEvels == TypeCheckUtils.getRefLevels(castTo) &&unRefCastTo instanceof NamedType && unRefExpType instanceof VarNull){
						//ref levels match and wei're going from varnull to type; e.g. a String: = null!
						//TypeCheckUtils.mutatleRefType(wantLEvels, (NamedType)exprTypeRhs, (NamedType)unRefCastTo);
						unRefExpType=TypeCheckUtils.getRefTypeIgnoreRefArray(exprTypeRhs);
					}
				}
			}
		}
		if(pass){
			Type isSub = TypeCheckUtils.checkSubType(erlog, unRefCastTo, unRefExpType, 0, 0, 0, 0);
			if(!erlog.isHasErrored() && null != isSub)
			{//why is this here?
				isSub.setArrayLevels(0);
				unRefCastTo.setArrayLevels(0);
				return isSub.equals(unRefCastTo)? castToOrig: exprTypeRhs;
			}
			
			pass=false;
			
			if(castTo instanceof PrimativeType )
			{
				pass = checkCastToPrimativeType(er, line, column, (PrimativeType)castTo, exprTypeRhs);
			}
			else if(castTo instanceof NamedType || castTo instanceof GenericType )
			{
				if(castTo.getPrettyName().equals("java.lang.Object"))
				{
					pass = true;//everything can become an object
				}
				//MIGHT BE HERE
				else //not boxed, normal
				{
					if(castTo instanceof GenericType)
					{//TODO: this is a hack, you need to take the upper bound of the generic parameter
						pass = checkCastToOtherNamedType(er, line, column, OBJ_NT, exprTypeRhs);
					}
					else
					{
						if(exprTypeRhs instanceof NamedType &&  ((NamedType)exprTypeRhs).equals(OBJ_NT)){
							//every object can potentially be converted from object
							pass=true;
						}
						else{
							pass = checkCastToOtherNamedType(er, line, column, (NamedType)castTo, exprTypeRhs);
						}
					}
				}
			}
		}
		
		if(!pass)
		{
			if(IsCast){
				er.raiseError(line, column, String.format("Cannot cast from %s to %s", exprTypeRhs, castToOrig));
			}
			else{
				er.raiseError(line, column, String.format("Cannot compare an instance of %s with %s", exprTypeRhs, castToOrig));
			}
			
		}
		
		return castToOrig;
	}
	/*
	private static boolean checkLambdaCast(Type from, Type castTo)
	{
		boolean fromLambda = from instanceof PrimativeType && ((PrimativeType)from).type == PrimativeTypeEnum.LAMBDA;
		boolean castToLambda = castTo instanceof PrimativeType && ((PrimativeType)from).type == PrimativeTypeEnum.LAMBDA;
		
		if(fromLambda){
			if(castToLambda){
				return true;
			}
			else{
				return castTo instanceof FuncType;
			}
		}
		else if(castToLambda){
			
		}
		
		
		return false;
	}
	*/
	//note that dynamic binding is based on cast type, not runtime type
	
	private static HashMap<String, HashSet<PrimativeTypeEnum>> VALID_FROM_BOXED_TO_PRIM = new HashMap<String, HashSet<PrimativeTypeEnum>>();
	static
	{//this defines the set of stuff which you can cast from to
		HashSet<PrimativeTypeEnum> byteSet = new HashSet<PrimativeTypeEnum>();
		byteSet.add(PrimativeTypeEnum.BYTE);
		byteSet.add(PrimativeTypeEnum.SHORT);
		byteSet.add(PrimativeTypeEnum.INT);
		byteSet.add(PrimativeTypeEnum.LONG);
		byteSet.add(PrimativeTypeEnum.FLOAT);
		byteSet.add(PrimativeTypeEnum.DOUBLE);
		
		HashSet<PrimativeTypeEnum> shortSet = new HashSet<PrimativeTypeEnum>();
		shortSet.add(PrimativeTypeEnum.SHORT);
		shortSet.add(PrimativeTypeEnum.INT);
		shortSet.add(PrimativeTypeEnum.LONG);
		shortSet.add(PrimativeTypeEnum.FLOAT);
		shortSet.add(PrimativeTypeEnum.DOUBLE);
		
		HashSet<PrimativeTypeEnum> charSet = new HashSet<PrimativeTypeEnum>();
		charSet.add(PrimativeTypeEnum.INT);
		charSet.add(PrimativeTypeEnum.LONG);
		charSet.add(PrimativeTypeEnum.CHAR);
		charSet.add(PrimativeTypeEnum.FLOAT);
		charSet.add(PrimativeTypeEnum.DOUBLE);
		
		HashSet<PrimativeTypeEnum> intSet = new HashSet<PrimativeTypeEnum>();
		intSet.add(PrimativeTypeEnum.INT);
		intSet.add(PrimativeTypeEnum.LONG);
		intSet.add(PrimativeTypeEnum.FLOAT);
		intSet.add(PrimativeTypeEnum.DOUBLE);
		
		HashSet<PrimativeTypeEnum> longSet = new HashSet<PrimativeTypeEnum>();
		longSet.add(PrimativeTypeEnum.LONG);
		longSet.add(PrimativeTypeEnum.FLOAT);
		longSet.add(PrimativeTypeEnum.DOUBLE);
		
		HashSet<PrimativeTypeEnum> floatSet = new HashSet<PrimativeTypeEnum>();
		floatSet.add(PrimativeTypeEnum.FLOAT);
		floatSet.add(PrimativeTypeEnum.DOUBLE);
		
		HashSet<PrimativeTypeEnum> doubleSet = new HashSet<PrimativeTypeEnum>();
		doubleSet.add(PrimativeTypeEnum.DOUBLE);
		
		HashSet<PrimativeTypeEnum> booleanSet = new HashSet<PrimativeTypeEnum>();
		booleanSet.add(PrimativeTypeEnum.BOOLEAN);

		VALID_FROM_BOXED_TO_PRIM.put("java.lang.Byte",      byteSet);
		VALID_FROM_BOXED_TO_PRIM.put("java.lang.Short",     shortSet);
		VALID_FROM_BOXED_TO_PRIM.put("java.lang.Character", charSet);
		VALID_FROM_BOXED_TO_PRIM.put("java.lang.Integer",   intSet);
		VALID_FROM_BOXED_TO_PRIM.put("java.lang.Long",      longSet);
		VALID_FROM_BOXED_TO_PRIM.put("java.lang.Float",     floatSet);
		VALID_FROM_BOXED_TO_PRIM.put("java.lang.Double",    doubleSet);
		VALID_FROM_BOXED_TO_PRIM.put("java.lang.Boolean",   booleanSet);
	}
	
	
	private static boolean checkCastToPrimativeType(ErrorRaiseable er, int line, int column, PrimativeType castTo, Type exprTypeRhs)
	{
		PrimativeTypeEnum to = castTo.type;
		
		if(castTo.hasArrayLevels())
		{
			if(exprTypeRhs.equals(OBJ_NT)){
				return true;
			}
			if(!castTo.getBytecodeType().equals(exprTypeRhs.getBytecodeType()))
			{//arrays need to match 1:1, cannot do new int[3] as float[];
				return false;
			}
		}
		
		if(exprTypeRhs instanceof PrimativeType || exprTypeRhs.equals(OBJ_NT))
		{//Object -> int is ok
			/*PrimativeTypeEnum from = ((PrimativeType)exprTypeRhs).type;
			
			if( !((from == PrimativeTypeEnum.BOOLEAN || to == PrimativeTypeEnum.BOOLEAN) && from != to))
			{//all prim converstions are ok except from bool to bool
				return true;
			}*/
			return true;
		}
		else if(TypeCheckUtils.isBoxedType(exprTypeRhs))
		{//make sure it's the right boxed type on the rhs
			
			String from = ((NamedType)exprTypeRhs).getPrettyName();
			
			HashSet<PrimativeTypeEnum> okPrims = VALID_FROM_BOXED_TO_PRIM.get(from);
			return null != okPrims && okPrims.contains(to);
		}
		//fail, cannot cast this
		return false;
	}
	
	private static boolean checkCastToBoxedType(ErrorRaiseable er, int line, int column, NamedType castTo, Type exprTypeRhs)
	{
		//has to be approperiate primative as input or 
		if(exprTypeRhs instanceof PrimativeType)
		{//from prim to boxed
			PrimativeTypeEnum from = ((PrimativeType)exprTypeRhs).type;
			String[] boxed = TypeCheckUtils.PRIMS_TO_BOXED.get(from);
			
			if(null != boxed)
			{
				String cTo = castTo.getPrettyName();
				for(String rhsboxed : boxed)
				{
					if(cTo.equals(rhsboxed)){
						return true;
					}
				}
			}
		}
		else if(TypeCheckUtils.isBoxedType(exprTypeRhs) && exprTypeRhs.getPrettyName().equals(castTo.getPrettyName()))
		{//can go from boxed to boxed only
			return true;
		}
		//last check, we can go from object to a namedtype
		return exprTypeRhs instanceof NamedType && ((NamedType)exprTypeRhs).getPrettyName().equals("java.lang.Object");
	}
	
	private static ClassDefJava classDefJavaObj = new ClassDefJava(Object.class);
	
	private static boolean checkCastToOtherNamedType(ErrorRaiseable er, int line, int column, NamedType castTo, Type exprTypeRhs)
	{
		//if exprTypeRhs is primative then fail, else regular object type cast (and not much stuff u can does.. but check)
		
		//haMHA: ck this in here...
		if(exprTypeRhs instanceof NamedType && exprTypeRhs.hasArrayLevels()){
			NamedType rhsNamed = (NamedType)exprTypeRhs;
			
			if(exprTypeRhs.getArrayLevels() == castTo.getArrayLevels()){
				if(rhsNamed.getIsRef() )
				{//asObjAr Object[] = [1!,2!] <- ok
					//asObjAr as Number:[] <-fail
					if(castTo instanceof NamedType){
						NamedType lhsNamed = (NamedType)castTo;
						if(lhsNamed.getIsRef()){
							return true;
						}
						else if(lhsNamed.getSetClassDef().equals(classDefJavaObj)) {
							return true;
						}
					}
					return false;
				}
				else if(rhsNamed.getSetClassDef().equals(classDefJavaObj)) {
					//Object[] -> Number:[] <-fail
					if(castTo instanceof NamedType &&  ((NamedType)castTo).getIsRef()){
						return false;
					}
				}
			}
			else{
				return false;
			}
		}
		
		
		
		if(exprTypeRhs instanceof NamedType)
		{//rhs must be named
			//x <: Y || Y <: X
			ErrorRaiseable supressed = er.getErrorRaiseableSupression();
			return null != TypeCheckUtils.checkSubType(supressed, castTo, exprTypeRhs, line, column, line, column) || null != TypeCheckUtils.checkSubType(supressed, exprTypeRhs, castTo, line, column, line, column);
		}
		
		//p List[String] = (ArrayList[String])something; 
		//p List[Object] = (ArrayList[String])something; 
		//String p = (String)new Object(); - this is ok
		return false;
	}
}
