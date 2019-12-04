package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.visitors.ConstLocationAndType;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;

public class ConstructorDef extends FuncDef {

	public ConstructorDef(int line, int col, AccessModifier accessModifier, FuncParams params, Block bbk)
	{
		super(line, col, null, accessModifier, "<init>", params, bbk, false, false, false);//TODO: annotataions
	}

	//private final static Type Const_PRIM_VOID = new PrimativeType(PrimativeTypeEnum.VOID);
	
	public ConstLocationAndType getConstLocationAndType()
	{
		ArrayList<Type> inputs = new ArrayList<Type>();
		
		for(FuncParam fp :  params.params){
			inputs.add(fp.getTaggedType());
		}
		
		return new ConstLocationAndType(line, column, new FuncType(inputs, null));
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		if(funcDefVariants != null && !funcDefVariants.isEmpty() && !(visitor instanceof ScopeAndTypeChecker)) {
			Object last = null;
			for(FuncDef fd : funcDefVariants) {
				last = visitor.visit((ConstructorDef)fd);
			}
			return last;
		}
		
		return visitor.visit(this);
	}
	

	@Override
	public Node copyTypeSpecific() {
		ConstructorDef ret = new ConstructorDef(super.line, super.column, accessModifier==null?null:accessModifier.copy(), params==null?null:(FuncParams)params.copy(), funcblock==null?null:(Block)funcblock.copy());
		ret.annotations = (Annotations)(annotations==null?null:annotations.copy());
		ret.isInjected = isInjected;
		super.copySpecifics(ret);
		return ret;
	}
	
}
