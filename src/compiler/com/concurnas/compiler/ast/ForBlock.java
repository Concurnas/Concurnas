package com.concurnas.compiler.ast;

import org.objectweb.asm.Label;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class ForBlock extends CompoundStatement implements ForBlockMaybeParFor, IdxVarHaver{

	public String localVarName;
	public Type localVarType;
	public Expression expr;
	public Block block;
	public Label startOfWorkBlock;
	public ForBlockVariant origParFor;
	public Label startOfPostOp;
	public boolean skipGotoStart=false;
	private Block repointed;
	public AssignTupleDeref assignTuple;
	
	public ForBlock(int line, int col, String localVarName, Type localVarType, Expression expr, Block block, ForBlockVariant fbv) {
		super(line, col);
		this.localVarName = localVarName;
		this.localVarType = localVarType;
		this.expr = expr;
		this.block = block;
		this.origParFor = fbv;
	}
	public ForBlock(int line, int col, AssignTupleDeref assignTuple, Expression expr, Block block, ForBlockVariant fbv) {
		super(line, col);
		this.assignTuple = assignTuple;
		this.expr = expr;
		this.block = block;
		this.origParFor = fbv;
	}
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return repointed != null?visitor.visit(repointed):visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		ForBlock ret = new ForBlock(super.line, super.column, localVarName, localVarType==null?null:(Type)localVarType.copy(), (Expression)expr.copy(), (Block)block.copy(), origParFor);
		ret.shouldBePresevedOnStack = shouldBePresevedOnStack;
		ret.localVarTypeToAssign = localVarTypeToAssign;
		ret.repointed=repointed==null?null:(Block)repointed.copy();
		ret.idxVariableCreator = idxVariableCreator==null?null:(AssignNew)idxVariableCreator.copy();
		ret.idxVariableAssignment = idxVariableAssignment==null?null:(RefName)idxVariableAssignment.copy();
		ret.elseblock = elseblock==null?null:(Block)elseblock.copy();
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		ret.postIdxIncremement = postIdxIncremement==null?null:(PostfixOp)postIdxIncremement.copy();
		ret.assignTuple = assignTuple ==null?null:(AssignTupleDeref)assignTuple.copy();
		ret.flagErrorListCompri = flagErrorListCompri;
		ret.isListcompri = isListcompri;
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
		//
		//if(should){
			this.block.setShouldBePresevedOnStack(should);
		//}
		if(this.elseblock != null){
			this.elseblock.setShouldBePresevedOnStack(should);
		}
		
		if(null != repointed){
			repointed.setShouldBePresevedOnStack(should);
		}
		
	}
	
	public boolean getShouldBePresevedOnStack()	{
		return shouldBePresevedOnStack;
	}
	
	private Label labelBeforeRetLoadIfStackPrese;
	public boolean defoEndsInGotoStmtAlready;
	
	public Label hasNextLabel;
	public Label beforeAdder;
	public NamedType isMapSetType = null;
	public Type localVarTypeToAssign;
	public AssignNew idxVariableCreator;
	public RefName idxVariableAssignment;
	public Block elseblock;
	public boolean flagErrorListCompri=false;
	public boolean isListcompri = false;
	public PostfixOp postIdxIncremement;
	
	public Label getLabelBeforeRetLoadIfStackPrese() {
		return labelBeforeRetLoadIfStackPrese;
	}
	
	public void setLabelBeforeRetLoadIfStackPrese(Label labelBeforeRetLoadIfStackPrese) {
		this.labelBeforeRetLoadIfStackPrese = labelBeforeRetLoadIfStackPrese;
	}
	public boolean getHasAttemptedNormalReturn() {
		return this.block.hasDefoReturned || (elseblock==null?false:this.elseblock.hasDefoReturned  ) ;
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
	
	@Override
	public AssignNew getIdxVariableCreator() {
		return this.idxVariableCreator;
	}
	
	@Override
	public RefName getIdxVariableAssignment() {
		return this.idxVariableAssignment;
	}

	@Override
	public void setIdxVariableCreator(AssignNew xxx) {
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
