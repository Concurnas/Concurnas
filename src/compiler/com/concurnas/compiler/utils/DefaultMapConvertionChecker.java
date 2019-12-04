package com.concurnas.compiler.utils;

import java.util.ArrayList;

import com.concurnas.compiler.ast.FuncRef;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.LambdaDef;
import com.concurnas.compiler.ast.MapDefaultElement;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.TypeCheckUtils;

public class DefaultMapConvertionChecker {

	public final Type origInputType;
	public final Type origOutputType;
	public boolean dropInputForCall = false;
	public final boolean evalsToNonFuncRefOrLambda;
	
	private boolean haveDifferentRefLevels(Type ta, Type ba){
		return TypeCheckUtils.getRefLevels(ta) != TypeCheckUtils.getRefLevels(ba);
	}
	
	public DefaultMapConvertionChecker(MapDefaultElement mde, Type inputType, Type outputType){
		//Type convertInputTo, Type convertOutputTo, boolean dropInPutForCall
		
		//having different ref levels is the only cases where we need to
		Expression vNode = mde.astRedirect != null ? mde.astRedirect : mde.value;
		
		this.evalsToNonFuncRefOrLambda = !(vNode instanceof LambdaDef || mde.valType instanceof FuncRef);
		
		if(!this.evalsToNonFuncRefOrLambda){
			FuncType dt = (FuncType)vNode.getTaggedType();
			ArrayList<Type> inputs = dt.getInputs();
			if(inputs.isEmpty()){
				this.origInputType = null;
				this.dropInputForCall = true;
			}
			else{
				this.origInputType = haveDifferentRefLevels(inputType, inputs.get(0)) ? inputs.get(0) : null;  
			}
			
			this.origOutputType = haveDifferentRefLevels(outputType, dt.retType) ? dt.retType : null;
		}
		else{
			this.origInputType=null;
			this.origOutputType=haveDifferentRefLevels(outputType, mde.getTaggedType()) ? mde.getTaggedType() : null;
		}
	}
}
