package com.concurnas.compiler.bytecode;

import java.util.Collection;

import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.ConstructorDef;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.LambdaDef;
import com.concurnas.compiler.visitors.AbstractVisitor;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.compiler.visitors.datastructs.TheScopeFrame;

public class DefaultConstuctorFieldInitlizator extends AbstractVisitor {

	private BytecodeGennerator mainVistitor;
	
	@Override
	public Collection<? extends ErrorHolder> getErrors() {
		return null;
	}
	
	public DefaultConstuctorFieldInitlizator(BytecodeGennerator mainVistitor){
		this.mainVistitor = mainVistitor;
	}
	
	public void initFields(ClassDef classDef) {
		boolean prev = this.mainVistitor.isInsideDefaultConstuctorFieldInit;//TODO: what does this code do? remove isInsideDefaultConstuctorFieldInit?
		this.mainVistitor.isInsideDefaultConstuctorFieldInit = true;
		TheScopeFrame prevscopeFrame = mainVistitor.currentScopeFrame;
		mainVistitor.currentScopeFrame = mainVistitor.currentScopeFrame.getParent();
		super.visit(classDef.classBlock);
		mainVistitor.currentScopeFrame = prevscopeFrame;
		this.mainVistitor.isInsideDefaultConstuctorFieldInit = prev;
	}
	
	public void initFieldsNoScopeFrameAdjustment(ClassDef classDef) {
		super.visit(classDef);
	}
	
	@Override
	public Object visit(FuncDef funcDef) {
		return null;//dont process below
	}
	@Override
	public Object visit(ConstructorDef funcDef) {
		return null;//dont process below
	}

/*	@Override
	public Object visit(LambdaDef funcDef) {
		return null;//dont process below
	}*/
	
	@Override
	public Object visit(ClassDef classDef) {
		return null;//dont process below
	}
	
	/*
	   L1
	    LINENUMBER 14 L1
	    ALOAD 0: this
	    BIPUSH 9
	    PUTFIELD Child$Cls.a : int
	*/
	
	@Override
	public Object visit(AssignNew assignNew) {
 		if(assignNew.eq!=null || TypeCheckUtils.hasRefLevels(assignNew.getTaggedType()))
		{//dont gennerate for the likes of://private y int; as a field def
			return mainVistitor.visit(assignNew);
		}
		return null;
	}
	
	/*@Override
	public Object visit(Block block) {
		if(block.isClassFieldBlock){
			return mainVistitor.visit(block);
		}
		return super.visit(block);
	}*/

	@Override
	public Object visit(AssignExisting assignExisting) {
		/*if(assignExisting.asignment instanceof Block){
			((Block)assignExisting.asignment).isClassFieldBlock=false;
		}*/
		
		return mainVistitor.visit(assignExisting);
	}
}
