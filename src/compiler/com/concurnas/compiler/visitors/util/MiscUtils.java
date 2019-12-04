package com.concurnas.compiler.visitors.util;

import java.util.Set;

import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.PrimativeType;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.visitors.ErrorRaiseable;
import com.concurnas.compiler.visitors.TypeCheckUtils;

public class MiscUtils {
	public static TypeAndLocation getMatchingFuncType(ErrorRaiseable er, Set<TypeAndLocation> superFuncsfuncs, TypeAndLocation matchTotal)
	{
		FuncType matchTo = (FuncType)matchTotal.getType();
		for(TypeAndLocation cano : superFuncsfuncs)
		{
			FuncType can = (FuncType) cano.getType();
			
			if(TypeCheckUtils.isArgumentListSame(can.getInputs(), matchTo.getInputs() ))
			{
				if(can.retType == null || matchTo.retType == null){
					if(can.retType == null && matchTo.retType == null){
						return cano;//match, if one null both must be null
					}
					continue;
				}
				
				if(can.retType instanceof PrimativeType || matchTo.retType instanceof PrimativeType){
					if(!(can.retType instanceof PrimativeType && matchTo.retType instanceof PrimativeType)){
						continue; //avoids case where Integer and int types are considered the same thing (which they are not for return type analysis purposes)
					}
				}
				
				if(can.retType.equals(matchTo.retType) || null!=TypeCheckUtils.checkSubType(er, can.retType, matchTo.retType , 0, 0, 0, 0, true)){
					return cano;
				}
			}
		}
		return null;
	}
	
	public static TypeAndLocation getMatchingFuncTypeDontConsiderReturnType(Set<TypeAndLocation> superFuncsfuncs, TypeAndLocation matchTotal)
	{
		FuncType matchTo = (FuncType)matchTotal.getType();
		
		FuncType mte = matchTo.getErasedFuncType();
		
		for(TypeAndLocation cano : superFuncsfuncs)
		{
			FuncType can = (FuncType) cano.getType();
			if(mte.getInputs().equals(can.getInputs()))
			{
				return cano;
			}
		}
		return null;
	}
}
