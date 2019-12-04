package com.concurnas.compiler.ast;

import java.util.ArrayList;

import org.objectweb.asm.Label;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class IfStatement extends CompoundStatement {

	public Expression iftest;
	public Block ifblock;
	public ArrayList<ElifUnit> elifunits = new ArrayList<ElifUnit>();
	public Block elseb;
	public Label onComplete;
	public Label elseLabelOnEntry;
	public Label onFailIfCheckLabel;
	private boolean shouldBePresevedOnStack = false;

	public IfStatement(int line, int col, Expression iftest, Block ifblock, ArrayList<ElifUnit> elifunits) {
		super(line, col);
		this.iftest = iftest;
		this.ifblock = ifblock;
		if(null != elifunits){
			this.elifunits = elifunits;
		}
	}
	
	public IfStatement(int line, int col, Expression iftest, Block ifblock) {
		this(line, col, iftest, ifblock, null);
	}

	public IfStatement(int line, int col, Expression iftest, Block ifblock, ArrayList<ElifUnit> elifunits, Block elseb) {
		this(line, col, iftest, ifblock, elifunits);
		this.elseb = elseb;
	}
	
	public static IfStatement makeFromTwoExprs(int line, int col, Expression op1, Expression op2, Expression test) {
		
		Block ifBlock = new Block(line, col);
		ifBlock.add(new LineHolder(new DuffAssign(op1)));

		Block elseBlock = new Block(line, col);
		elseBlock.add(new LineHolder(new DuffAssign(op2)));
		
		IfStatement ifsts= new IfStatement(line, col, test, 
				ifBlock, new ArrayList<ElifUnit>(), 
				elseBlock);
		
		ifsts.setShouldBePresevedOnStack(true);
		ifsts.canBeConvertedIntoIfExpr = true;
		return ifsts;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		IfStatement ifola = new IfStatement(super.line, super.column, (Expression) iftest.copy(), ifblock==null?null:(Block)ifblock.copy(), (ArrayList<ElifUnit>) Utils.cloneArrayList(elifunits), elseb==null?null:(Block)elseb.copy()  );
		ifola.shouldBePresevedOnStack=shouldBePresevedOnStack;
		ifola.canBeConvertedIntoIfExpr=canBeConvertedIntoIfExpr;
		ifola.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		
		/*ifola.onComplete = onComplete;
		ifola.elseLabelOnEntry = elseLabelOnEntry;
		ifola.onFailIfCheckLabel = onFailIfCheckLabel;*/
		return ifola;
	}
	private Expression preceedingExpression;
	public boolean canBeConvertedIntoIfExpr;
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
	
	public void setShouldBePresevedOnStack(boolean should)
	{
		shouldBePresevedOnStack = should;
		//if(should){
			ifblock.setShouldBePresevedOnStack(should);
			for(ElifUnit e : elifunits){
				e.elifb.setShouldBePresevedOnStack(should);
			}
			if(null!=elseb){
				elseb.setShouldBePresevedOnStack(should);
			}
		//}
	}
	
	public boolean getShouldBePresevedOnStack()
	{//TODO: is this needed?
		//extend this logic to statements in genneral?
		return shouldBePresevedOnStack;
	}

	public boolean getHasAttemptedNormalReturn(){
		//e.g. if, or if elses any legs attempt to return
		boolean ret = this.ifblock.defoReturns;
		if(!elifunits.isEmpty()){
			for(ElifUnit e : elifunits){
				ret &= e.elifb.defoReturns;
			}
		}
		
		if(null!=elseb){
			ret &= elseb.defoReturns;
		}
		
		return ret;
	}
	
	public Type setTaggedType(Type tt, boolean forceNull){
		if(tt == null && !forceNull) {
			return super.setTaggedType(tt);
		}else {
			Utils.setTypeOnNullListDef((Node)this.ifblock,  tt==null?null:(Type)tt.copy()  );
			for(ElifUnit eli : this.elifunits) {
				Utils.setTypeOnNullListDef((Node)eli.elifb,  tt==null?null:(Type)tt.copy()  );
			}
			
			if(null != this.elseb) {
				Utils.setTypeOnNullListDef((Node)this.elseb,  tt==null?null:(Type)tt.copy()  );
			}
			return super.setTaggedType(tt);
		}
	}
	
	@Override
	public Type setTaggedType(Type tt){
		return setTaggedType(tt, false);
	}
	
	@Override
	public void setIfReturnsExpectImmediateUse(boolean ifReturnsExpectImmediateUse) {
		super.setIfReturnsExpectImmediateUse(ifReturnsExpectImmediateUse);
		
		this.ifblock.setIfReturnsExpectImmediateUse(ifReturnsExpectImmediateUse);
		
		for(ElifUnit eli : this.elifunits) {
			eli.elifb.setIfReturnsExpectImmediateUse(ifReturnsExpectImmediateUse);
		}
		
		if(null != this.elseb) {
			this.elseb.setIfReturnsExpectImmediateUse(ifReturnsExpectImmediateUse);
		}
		
	}
	
}
