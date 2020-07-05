package com.concurnas.compiler.ast;

import org.objectweb.asm.Label;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class WhileBlock extends CompoundStatement implements IdxVarHaver{

	public Block block;
	public Expression cond;
	//public Label startOfWorkBlock;
	public Boolean defoEndsInGotoStmtAlready=false;
	public Label labelAsLastLineInBlock;
	public boolean isTransWhileBlock = false;
	public AssignExisting idxVariableCreator;
	public RefName idxVariableAssignment;
	
	public WhileBlock(int line, int col, Expression cond, Block blk) {
		super(line, col);
		this.cond = cond; //resolves to boolean
		this.block = blk;
	}
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		WhileBlock ret = new WhileBlock(super.line, super.column, (Expression)cond.copy(), (Block)block.copy());
		ret.defoEndsInGotoStmtAlready = defoEndsInGotoStmtAlready;
		ret.labelAsLastLineInBlock = labelAsLastLineInBlock;
		ret.skipGotoStart = skipGotoStart;
		ret.shouldBePresevedOnStack = shouldBePresevedOnStack;
		ret.idxVariableCreator = idxVariableCreator==null?null:(AssignExisting)idxVariableCreator.copy();
		ret.idxVariableAssignment = idxVariableAssignment==null?null:(RefName)idxVariableAssignment.copy();
		ret.elseblock = elseblock==null?null:(Block)elseblock.copy();
		//ret.beforeAdder = beforeAdder;
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		ret.postIdxIncremement = postIdxIncremement==null?null:(PostfixOp)postIdxIncremement.copy();

		return ret;
	}
	private Expression preceedingExpression;
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
	
	private boolean shouldBePresevedOnStack=false;
	
	public void setShouldBePresevedOnStack(boolean should){
		shouldBePresevedOnStack = should;
		this.block.setShouldBePresevedOnStack(should);
		if(null != this.elseblock){
			this.elseblock.setShouldBePresevedOnStack(should);
		}
	}
	
	public boolean getShouldBePresevedOnStack()	{
		return shouldBePresevedOnStack;
	}
	
	private Label labelBeforeCondCheckIfStackPrese;
	public boolean skipGotoStart = false;
	public Label beforeAdder;
	public Block elseblock;
	public PostfixOp postIdxIncremement;
	
	public void setLabelBeforeCondCheckIfStackPrese(Label onEntry) {
		this.labelBeforeCondCheckIfStackPrese = onEntry;
	}
	
	public Label getLabelBeforeCondCheckIfStackPrese() {
		return this.labelBeforeCondCheckIfStackPrese;
	}
	
	public boolean getHasAttemptedNormalReturn() {
		return this.block.hasDefoReturned || this.elseblock==null?false:this.elseblock.hasDefoReturned;
	}
	
	@Override
	public AssignExisting getIdxVariableCreator() {
		return this.idxVariableCreator;
	}
	
	@Override
	public RefName getIdxVariableAssignment() {
		return this.idxVariableAssignment;
	}
	
	@Override
	public void setIdxVariableCreator(AssignExisting xxx) {
		this.idxVariableCreator = xxx;
	}
	

	private boolean canRetValue=false;
	@Override
	public boolean getCanReturnAValue() {
		return this.canRetValue;
	}
	
	public void setCanReturnValue(Boolean pop) {
		canRetValue = pop;
	}
}
