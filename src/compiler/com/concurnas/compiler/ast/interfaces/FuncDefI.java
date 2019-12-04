package com.concurnas.compiler.ast.interfaces;

import java.util.ArrayList;
import java.util.HashMap;

import com.concurnas.compiler.ast.AccessModifier;
import com.concurnas.compiler.ast.Annotations;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.CompoundStatement;
import com.concurnas.compiler.ast.FuncParams;
import com.concurnas.compiler.ast.GPUFuncVariant;
import com.concurnas.compiler.ast.GenericType;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.bytecode.TryCatchLabelTagVisitor.TryCatchBlockPreallocatedLabelsEtc;
import com.concurnas.runtime.Pair;

public abstract class FuncDefI extends CompoundStatement implements Expression {

	public FuncDefI(int line, int col, boolean validAtClassLevel) {
		super(line, col, validAtClassLevel);
	}
	
	public abstract Type getRetuType();
	
	public ArrayList<TryCatchBlockPreallocatedLabelsEtc> stuffTovisitTryCatchBlock;
	
	public abstract FuncParams getParams();
	public abstract Type getReturnType();
	//public abstract void setReturnType(Type tt);
	public abstract String getMethodName();
	public abstract void setMethodName(String replace);
	public abstract Block getBody() ;
	public abstract boolean isAbstract();
	public abstract void setAbstract(boolean asbtr);
	public abstract boolean isFinal();
	
	public boolean isNestedFunc;
	public GPUFuncVariant isGPUKernalOrFunction = null;

	public abstract AccessModifier getAccessModifier();
	
	public abstract boolean IsAutoGennerated();
	
	public ArrayList<Pair<String, NamedType>> methodGenricList;
	public boolean isInjected = false;
	public boolean hasErrors = false;
	
	public HashMap<String, GenericType> getStringToGenerics(){
		HashMap<String, GenericType> ret = new HashMap<String, GenericType>();
		
		int n=0;
		if(null != methodGenricList){
			for(Pair<String, NamedType> nameAndUpperBound: methodGenricList){
				NamedType upperBound = nameAndUpperBound.getB();
				String name = nameAndUpperBound.getA();
				
				GenericType gt = new GenericType(name, n++);
				if(null != upperBound) {
					gt.upperBound = upperBound;
					gt.setNullStatus(upperBound.getNullStatus());
				}
				
				ret.put(name, gt);
			}
		}
		
		return ret;
	}
	
	public abstract boolean getShouldInferFuncType();
	
	public abstract Annotations getAnnotations();
}
