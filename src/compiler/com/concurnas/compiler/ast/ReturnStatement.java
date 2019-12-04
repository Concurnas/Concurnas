package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

import org.objectweb.asm.Label;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class ReturnStatement extends Statement implements PreEntryCode, CanEndInReturnOrException {

	public Expression ret;
	public boolean isAllowedHere = true;
	public boolean withinOnChange = false;
	
	private Stack<Pair<Label, Block>> linesToInactOntry = new Stack<Pair<Label, Block>>();
	private Label labelb4Opcode = null;
	//private Label labelAfterRetrunCode = null;
	public final boolean isSynthetic;
	public Block astRepoint;

	public boolean getIsAsyncEarlyReturn(){
		return withinOnChange;
	}
	
	public ReturnStatement(int line, int col, Expression ret) {
		this(line, col, ret, false);
	}
	
	public ReturnStatement(int line, int col, Expression ret, boolean isSynthetic) {
		super(line, col);
		this.ret = ret;
		this.isSynthetic = isSynthetic;
	}

	public ReturnStatement(int line, int col) {
		this(line, col, null, false);
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(null != astRepoint && !(visitor instanceof IgnoreASTRepointForReturn)){
			return visitor.visit(astRepoint);
		}
		
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		ReturnStatement ret = new ReturnStatement(super.getLine(), super.getColumn(), this.ret==null?null:(Expression)this.ret.copy(), isSynthetic);
		ret.isAllowedHere=isAllowedHere;
		ret.withinOnChange=withinOnChange;
		ret.astRepoint = this.astRepoint==null?null:(Block)this.astRepoint.copy();
		
		Stack<Pair<Label, Block>> addlinesToInactOntry = new Stack<Pair<Label, Block>>();
		for(Pair<Label, Block> item: linesToInactOntry){
			addlinesToInactOntry.add(new Pair<Label, Block>(new Label(), (Block)item.getB().copy()));
		}
		
		ret.linesToInactOntry=addlinesToInactOntry;
		return ret;
	}

	@Override
	public void setLinesToVisitOnEntry(Stack<Pair<Label, Block>> blk) {
		linesToInactOntry = blk;
	}

	@Override
	public Stack<Pair<Label, Block>> getLinesToVisitOnEntry() {
		return linesToInactOntry;
	}
	
	public Label getLabelToVisitJustBeforeReturnOpCode() {
		return labelb4Opcode;
	}

	public void setLabelToVisitJustBeforeReturnOpCode(Label labelb4) {
		
		this.labelb4Opcode = labelb4;
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
}
