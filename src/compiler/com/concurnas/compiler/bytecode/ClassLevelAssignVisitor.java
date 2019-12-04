package com.concurnas.compiler.bytecode;

import java.util.Collection;
import java.util.HashMap;

import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.ClassDefArg;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.AbstractVisitor;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.runtime.Pair;

public class ClassLevelAssignVisitor extends AbstractVisitor {
	
	@Override
	public Collection<? extends ErrorHolder> getErrors() {
		return null;
	}
	
	private HashMap<String, Pair<Expression, Boolean>> nameToAssign = new HashMap<String, Pair<Expression, Boolean>>();
	
	public HashMap<String, Pair<Expression, Boolean>> initFields(ClassDef classDef) {
		nameToAssign = new HashMap<String, Pair<Expression, Boolean>>();
		
		if(classDef.classDefArgs != null){
			for(ClassDefArg cda : classDef.classDefArgs.aargs){
				if(null != cda.defaultValue){
					nameToAssign.put(cda.name, new Pair<Expression, Boolean>(cda.defaultValue, cda.isTransient));
				}
			}
		}
		
		super.visit(classDef);
		return nameToAssign;
	}
	
	@Override
	public Object visit(FuncDef funcDef) {
		return null;//dont process below
	}
	
	@Override
	public Object visit(ClassDef classDef) {
		return null;//dont process below
	}
	
	@Override
	public Object visit(AssignNew assignNew) {
		AssignNew toPut = null;
		
		if(assignNew.eq!=null || TypeCheckUtils.hasRefLevels(assignNew.getTaggedType()))
		{//dont gennerate for the likes of://private y int; as a field def
 			toPut = assignNew;
		}
		
		return nameToAssign.putIfAbsent(assignNew.name, new Pair<Expression, Boolean>(toPut==null?null:toPut.expr, assignNew.isTransient));
	}

	@Override
	public Object visit(AssignExisting assignExisting) {
		Expression asignTo = assignExisting.assignee;
		
		if(asignTo instanceof RefName){
			return nameToAssign.put(((RefName)asignTo).name, new Pair<Expression, Boolean>(assignExisting.expr, assignExisting.isTransient));
		}else{
			return null;
		}
		
	}
}
