package com.concurnas.compiler.ast;

import org.objectweb.asm.Label;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.bytecode.BytecodeGennerator;
import com.concurnas.compiler.visitors.Visitor;

public class ForBlockOld extends CompoundStatement implements ForBlockMaybeParFor {

	public Expression assignExpr;
	
	//or...
	public String assignName;
	public Type assigType;//optional
	public AssignStyleEnum assignStyle;//optional
	public Expression assigFrom;
	
	public Type fiddleNewVarType;
	public boolean fiddleIsNew;
	
	
	public Expression check;
	public Expression postExpr;
	public Block block;

	public Label startOfWorkBlock;

	public Label startOfPostOp;
	
	public boolean skipGotoStart=false;

	public boolean defoEndsInGotoStmtAlready = false;

	public ForBlockVariant origParFor;

	private Block repointed;
	
	public ForBlockOld(
			int line, 
			int column, 
				Expression assignExpr, 
				String assignName, Type assigType, AssignStyleEnum assignStyle, Expression assigFrom, 
			Expression check, 
			Expression postExpr, 
			Block block, ForBlockVariant pfv) {
		super(line, column);
		this.assignExpr = assignExpr;
		
		this.assignName = assignName;
		this.assigType = assigType;
		this.assignStyle = assignStyle;
		this.assigFrom = assigFrom;
		
		this.check = check;
		this.postExpr = postExpr;
		this.block = block;
		this.origParFor = pfv;
		
		if(null != assigFrom){
			((Node)assigFrom).setShouldBePresevedOnStack(false);
		}
		
		if(null != check){
			((Node)check).setShouldBePresevedOnStack(true);
		}
		
		if(null != postExpr){
			((Node)postExpr).setShouldBePresevedOnStack(false);
		}
		
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		Object ret = repointed != null?visitor.visit(repointed):visitor.visit(this);
		return ret;
	}
	
	@Override
	public Node copyTypeSpecific() {
		ForBlockOld ret = new ForBlockOld(super.line, super.column, 
				assignExpr==null?null:(Expression)assignExpr.copy(), assignName, assigType==null?null:(Type)assigType.copy(),
				assignStyle==null?null:(AssignStyleEnum)assignStyle.copy(), assigFrom==null?null:(Expression)assigFrom.copy(),
				check==null?null:(Expression)check.copy(), postExpr==null?null:(Expression)postExpr.copy(), block==null?null:(Block)block.copy(), origParFor );
		ret.defoEndsInGotoStmtAlready=defoEndsInGotoStmtAlready;
		ret.shouldBePresevedOnStack=shouldBePresevedOnStack;
		ret.resolvedassigType=resolvedassigType;
		ret.repointed=repointed==null?null:(Block)repointed.copy();
		ret.elseblock = elseblock==null?null:(Block)elseblock.copy();
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();

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
		
		//System.err.println("for block pres: " + should);
		
		
		//if(should){
			this.block.setShouldBePresevedOnStack(should);
		//}
			
		if(this.elseblock != null){
			this.elseblock.setShouldBePresevedOnStack(should);
		}
	}
	
	public boolean getShouldBePresevedOnStack()	{
		return shouldBePresevedOnStack;
	}
	
	private Label labelBeforeRetLoadIfStackPrese;

	public Label beforeAdder;

	public Type resolvedassigType;

	public AssignExisting idxExpression;

	public PostfixOp postIdxIncremement;

	public Block elseblock;

	public String newforTmpVar;

	public Label getLabelBeforeRetLoadIfStackPrese() {
		return labelBeforeRetLoadIfStackPrese;
	}
	
	public void setLabelBeforeRetLoadIfStackPrese(Label labelBeforeRetLoadIfStackPrese) {
		this.labelBeforeRetLoadIfStackPrese = labelBeforeRetLoadIfStackPrese;
	}
	
	public boolean getHasAttemptedNormalReturn() {
		return this.block.hasDefoReturned;
	}

	@Override
	public void setRepointed(Block repointed) {
		this.repointed = repointed;
	}

	@Override
	public void setMainBlock(Block block) {
		this.block = block;
	}

	@Override
	public Block getMainBlock() {
		return block;
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
