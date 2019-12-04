package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.TypeCheckUtils;

public class Utils {

	public static List cloneArrayList(List elements) {
		if(null == elements){
			return null;
		}
		List ret = new ArrayList(elements.size());
		
		for(Object e : elements){
			ret.add(e==null?null:((Copyable)e).copy());
		}
				
		return ret;
	}

	
	public static void setTypeOnNullListDef(Node child, Type toType){
		if(child instanceof LineHolder) {
			child = ((LineHolder)child).l;
		}
		
		if(child instanceof ArrayDef){
			ArrayDef asar = (ArrayDef)child;
			
			boolean isNullList = true;
			for(Expression e : asar.arrayElements){
				if(!(e instanceof VarNull)){
					isNullList = false;
					break;
				}
			}
			
			if(isNullList){
				child.setTaggedType(toType);
			}
		}
		else if(child instanceof MapDef){
			child.setTaggedType(toType);//casscade down
		}
		/*else if(child instanceof IfExpr){
			child.setTaggedType(toType);//casscade down
		}*/
		else if(child instanceof AsyncRefRef){//e.g. ok3 String:=(null:) if true else (null): ; assert (ok3 is String:)
			child.setTaggedType(toType);//casscade down
		}else if(child instanceof Block) {
			Block asBlock = (Block)child;
			Type tt = asBlock.getTaggedType();
			if(tt == null || TypeCheckUtils.isVarNull(TypeCheckUtils.getRefType(tt))) {
				asBlock.setTaggedType(toType);
			}
			setTypeOnNullListDef(asBlock.getLast(), toType);
		}
		else if(child instanceof DuffAssign) {
			setTypeOnNullListDef((Node)((DuffAssign)child).e, toType);
		}
	}
	
	
}
