package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

import org.objectweb.asm.Label;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class ContinueStatement extends Statement implements PreEntryCode, ContinueOrBreak {
	
	private boolean isValid=true;
	
	public boolean getIsValid(){
		return isValid && !isAsyncEarlyReturn;
	}
	
	public void setIsValid(boolean val){
		isValid = val;
	}

	private boolean isAsyncEarlyReturn=true;
	public void setIsAsyncEarlyReturn(boolean val){
		isAsyncEarlyReturn=val;
	}
	public boolean getIsAsyncEarlyReturn(){
		return isAsyncEarlyReturn;
	}
	
	public Label jumpTo;
	public Expression returns;
	
	private Label labelb4Opcode;
	public int breaksOutOfTryCatchLevel=0;
	public boolean isSynthetic=false;//added by compiler?
	
	public ContinueStatement(int line, int column) {
		super(line, column);
	}
	
	public ContinueStatement(int line, int column, Expression r) {
		super(line, column);
		this.returns = r;
	}
	
	public boolean hasReturns(){
		return this.returns != null;
	}
	
	public Block astRepoint;

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(null != this.astRepoint){
			visitor.visit(this.astRepoint);
			return null;
		}
		return visitor.visit(this);
	}
	
	
	private Stack<Pair<Label, Block>> linesToInactOntry = new Stack<Pair<Label, Block>>();

	@Override
	public void setLinesToVisitOnEntry(Stack<Pair<Label, Block>> blk) {
		linesToInactOntry = blk;
	}

	@Override
	public Stack<Pair<Label, Block>> getLinesToVisitOnEntry() {
		return linesToInactOntry;
	}

	@Override
	public Node copyTypeSpecific() {
		ContinueStatement ret = new ContinueStatement(super.line, super.column);
		ret.isValid = isValid;
		ret.isAsyncEarlyReturn = isAsyncEarlyReturn;
		ret.jumpTo = jumpTo;
		ret.breaksOutOfTryCatchLevel = breaksOutOfTryCatchLevel;
		ret.returns = returns;
		ret.isSynthetic = this.isSynthetic;
		ret.astRepoint = this.astRepoint;
		return ret;
	}

	
	@Override
	public Label getLabelToVisitJustBeforeReturnOpCode() {
		return labelb4Opcode;
	}
	@Override
	public void setLabelToVisitJustBeforeReturnOpCode(Label labelb4) {
		
		this.labelb4Opcode = labelb4;
	}
	@Override
	public ArrayList<Label> extractInsertedFinSegmentsList() {
		if(this.getLinesToVisitOnEntry().isEmpty()){
			return null;
		}
		
		ArrayList<Label> finSegmentLabels = new ArrayList<Label>();
		for( Pair<Label,Block> item : this.getLinesToVisitOnEntry()){
			finSegmentLabels.add(item.getA());
		}
		
		Collections.reverse(finSegmentLabels);
		
		//and add the one at the end just in case its full of nulls or last is null etc
		finSegmentLabels.add(this.getLabelToVisitJustBeforeReturnOpCode());
		
		return finSegmentLabels;
	}
	
	@Override
	public void setLabelBeforeAction(Label endLabel) {
		Stack<Pair<Label, Block>> onEntry = this.getLinesToVisitOnEntry();
		
		//they are stored in reverse order
		for(int n=onEntry.size()-1; n >= 0; n--){
			Pair<Label, Block> level = onEntry.get(n);
			if(level.getA()!=null){
				onEntry.set(n, new Pair<Label, Block>(endLabel, level.getB()));//overrite the label
				return;
			}
			
		}
		//nothing extra to insert so...
		setLabelToVisitJustBeforeReturnOpCode(endLabel);
	}
	
	@Override
	public boolean getCanReturnAValue(){//most things can
		if(null != returns) {
			return returns.getCanReturnAValue();
		}
		
		return true;//this.isSynthetic;
	}
}

