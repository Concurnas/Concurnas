package com.concurnas.compiler.bytecode;

import java.util.Stack;

import org.objectweb.asm.Label;

import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.BreakStatement;
import com.concurnas.compiler.ast.CatchBlocks;
import com.concurnas.compiler.ast.ConstructorDef;
import com.concurnas.compiler.ast.ContinueStatement;
import com.concurnas.compiler.ast.ForBlock;
import com.concurnas.compiler.ast.ForBlockOld;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.LambdaDef;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.ReturnStatement;
import com.concurnas.compiler.ast.TryCatch;
import com.concurnas.compiler.ast.WhileBlock;
import com.concurnas.compiler.visitors.AbstractVisitor;
import com.concurnas.runtime.Pair;

/**
 * Purpose: ensure that code in finally block is always executed, have to insert finally code 
 * 
 * fun doings() String{
	try{ 
		xxx();
		return "";
	}finally{
		x=9 //<- this code needs to get inserted before the return statement above
	}
}
 * 
 * @author Jason
 *
 */
public class FinallyBlockCodeCopier extends AbstractVisitor {
	//TODO: test with nested blocks
	
	//private Stack<Boolean> hasCatchBlocks = new Stack<Boolean>();
	
	@Override
	public Object visit(TryCatch tryCatch)	{
		
		if(tryCatch.astRepoint!=null){
			return tryCatch.astRepoint.accept(this);
		}
		
		if(tryCatch.hasFinal() ){
			//hasCatchBlocks.push(tryCatch.cbs !=null && !tryCatch.cbs.isEmpty());
			finallyTrackLayers.peek().finallyBlockToVisit.push(new Pair<Label, Block>(new Label(), tryCatch.finalBlock.copySetFirstLogical(null)));
			visitTryCatchNoFinally(tryCatch);
			finallyTrackLayers.peek().finallyBlockToVisit.pop();
			//hasCatchBlocks.pop();
		}
		else{
			//mark the fact that there isn't one
			finallyTrackLayers.peek().finallyBlockToVisit.push(new Pair<Label, Block>(null,null));
			visitTryCatchNoFinally(tryCatch);
			finallyTrackLayers.peek().finallyBlockToVisit.pop();
		}
		
		if(null != tryCatch.finalBlock){
			tryCatch.finalBlock.accept(this);
		}
		
		return null;
	}
	
	private void visitTryCatchNoFinally(TryCatch tryCatch)	{
		//Note:we skip the finally block as we dont want to do return code augmenttation here (inf loop lols)
		tryCatch.blockToTry.accept(this);
		for(CatchBlocks cat : tryCatch.cbs)
		{
			cat.accept(this);
		}
	}
	
	
	@Override
	public Object visit(Block block) {
		Object ret;
		if(block.isModuleLevel){
			finallyTrackLayers.push(new FinallyLayer() );
			ret = super.visit(block);
			finallyTrackLayers.pop();
		}
		else{
			ret= super.visit(block);
		}
		
		return ret;
	}
	
	
	@Override
	public Object visit(FuncDef funcDef) {
		finallyTrackLayers.push(new FinallyLayer() );
		Object ret = super.visit(funcDef);
		finallyTrackLayers.pop();
		return ret;
	}
	
	@Override
	public Object visit(ConstructorDef funcDef) {
		finallyTrackLayers.push(new FinallyLayer() );
		Object ret = super.visit(funcDef);
		finallyTrackLayers.pop();
		return ret;
	}
	
	@Override
	public Object visit(LambdaDef lambdaDef) {
		finallyTrackLayers.push(new FinallyLayer() );
		Object ret = super.visit(lambdaDef);
		finallyTrackLayers.pop();
		return ret;
	}

	private Stack<FinallyLayer> finallyTrackLayers = new Stack<FinallyLayer>();
	private class FinallyLayer{ 
		//public int breakableNestingDegree = 0; 
		public Stack<Pair<Label, Block>> finallyBlockToVisit = new Stack<Pair<Label, Block>>();
	}
	
	//for, while etc, break in here doesnt require calling of fincally code
	
	
	@Override
	public Object visit(ForBlock forBlock) {
		if(!finallyTrackLayers.isEmpty() && finallyTrackLayers.peek() != null){
			//finallyTrackLayers.peek().breakableNestingDegree++;
			super.visit(forBlock);
			//finallyTrackLayers.peek().breakableNestingDegree--;
		}
		else{
			super.visit(forBlock);
		}
		return null;
	}

	@Override
	public Object visit(ForBlockOld forBlockOld) {
		if(!finallyTrackLayers.isEmpty() && finallyTrackLayers.peek() != null){
			//finallyTrackLayers.peek().breakableNestingDegree++;
			super.visit(forBlockOld);
			//finallyTrackLayers.peek().breakableNestingDegree--;
		}
		else{
			super.visit(forBlockOld);
		}
		return null;
	}

	@Override
	public Object visit(WhileBlock whileBlock) {
		if(!finallyTrackLayers.isEmpty() && finallyTrackLayers.peek() != null){
			//finallyTrackLayers.peek().breakableNestingDegree++;
			super.visit(whileBlock);
			//finallyTrackLayers.peek().breakableNestingDegree--;
		}
		else{
			super.visit(whileBlock);
		}
		return null;
	}
	
	private Stack<Pair<Label, Block>> performDeepClone(Stack<Pair<Label, Block>> input){
		Stack<Pair<Label, Block>> output = new Stack<Pair<Label, Block>>();
		for(Pair<Label, Block> i : input){
			output.push(new Pair<Label, Block>(i.getA()==null?null: new Label(), i.getB()==null?null:(Block)i.getB().copy()));
		}
		return output;
	}
	
	@Override
	public Object visit(ReturnStatement returnStatement) {
		
		if(!finallyTrackLayers.isEmpty() && finallyTrackLayers.peek() != null && null != finallyTrackLayers.peek().finallyBlockToVisit && !finallyTrackLayers.peek().finallyBlockToVisit.isEmpty() ){
			returnStatement.setLinesToVisitOnEntry(performDeepClone((Stack<Pair<Label, Block>> )finallyTrackLayers.peek().finallyBlockToVisit.clone()));
		}
		return super.visit(returnStatement);
	}
	

	@Override
	public Object visit(BreakStatement breakStatement) {
		if(breakStatement.breaksOutOfTryCatchLevel>0){
			if(!finallyTrackLayers.isEmpty() && finallyTrackLayers.peek() != null && null != finallyTrackLayers.peek().finallyBlockToVisit && !finallyTrackLayers.peek().finallyBlockToVisit.isEmpty() ){
				//if(finallyTrackLayers.peek().breakableNestingDegree == 0){
					breakStatement.setLinesToVisitOnEntry(performDeepClone( (Stack<Pair<Label, Block>> )finallyTrackLayers.peek().finallyBlockToVisit.clone()));
				//}
			}
		}
		return null;
	}
	
	@Override
	public Object visit(ContinueStatement continueStatement) {
		if(continueStatement.breaksOutOfTryCatchLevel>0){
			if(!finallyTrackLayers.isEmpty() && finallyTrackLayers.peek() != null && null != finallyTrackLayers.peek().finallyBlockToVisit && !finallyTrackLayers.peek().finallyBlockToVisit.isEmpty() ){
				//if(finallyTrackLayers.peek().breakableNestingDegree == 0){
					continueStatement.setLinesToVisitOnEntry(performDeepClone((Stack<Pair<Label, Block>> )finallyTrackLayers.peek().finallyBlockToVisit.clone()));
				//}
			}
		}
		
		return null;
	}
}
