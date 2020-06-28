package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.CaseExpression;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ConstantFolding;
import com.concurnas.compiler.visitors.PrintSourceVisitor;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.compiler.visitors.util.MactchCase;


public class MatchStatement extends CompoundStatement {
	public Statement matchon;
	public ArrayList<MactchCase> cases;
	public Block elseblok;
	public Block astRedirect;
	private String prevASTRedir = null;

	public void setAstRedirect(Block astRedirect) {
		PrintSourceVisitor psv = new PrintSourceVisitor();
		astRedirect.accept(psv);
		String newRep = psv.toString();
		if(null != prevASTRedir) {
			if(prevASTRedir.equals(newRep)) {
				this.astRedirect.setShouldBePresevedOnStack(super.getShouldBePresevedOnStack());
				return;
			}
		}
		
		prevASTRedir = newRep;
		
		astRedirect.setShouldBePresevedOnStack(super.getShouldBePresevedOnStack());
		this.astRedirect = astRedirect;
	}


	public MatchStatement(int line, int column, Statement matchon /*RefName matchon*/, ArrayList<MactchCase> cases, Block elseblok) {
		super(line, column);
		this.matchon = matchon;
		this.cases = cases;
		this.elseblok = elseblok;
	}


	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(null != astRedirect && !(visitor instanceof ScopeAndTypeChecker) && !(visitor instanceof ConstantFolding)){
			this.astRedirect.setIfReturnsExpectImmediateUse(super.getIfReturnsExpectImmediateUse());
			return visitor.visit(astRedirect);
		}
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		ArrayList<MactchCase> newcases = new ArrayList<MactchCase>(cases.size());
		for(MactchCase casex : cases){
			newcases.add(new MactchCase((CaseExpression)casex.getA().copy(), (Block)casex.getB().copy()));
		}
		
		MatchStatement ifola = new MatchStatement(super.line, super.column, (Statement) matchon.copy(), newcases, elseblok==null?null:(Block)elseblok.copy()  );
		ifola.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();

		//ifola.shouldBePresevedOnStack=shouldBePresevedOnStack;
		return ifola;
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
	
/*	public void setShouldBePresevedOnStack(boolean should)
	{
		//System.err.println("if should: " + should);
		
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
	}*/
	
	public void setIfReturnsExpectImmediateUse(boolean ifReturnsExpectImmediateUse) {
		super.setIfReturnsExpectImmediateUse(ifReturnsExpectImmediateUse);
		if(null != this.astRedirect){
			this.astRedirect.setIfReturnsExpectImmediateUse(ifReturnsExpectImmediateUse);
		}
	}
	
	public void setShouldBePresevedOnStack(boolean should)
	{
		//System.err.println("if should: " + should);
		super.setShouldBePresevedOnStack(should);
		if(null != this.astRedirect){
			this.astRedirect.setShouldBePresevedOnStack(true);
		}
	}

	public boolean getHasAttemptedNormalReturn(){
		//e.g. if, or if elses any legs attempt to return
		boolean ret = true;
		for(MactchCase casex : cases){
			ret |= casex.getB().defoReturns;
		}
		
		if(null!=elseblok){
			ret |= elseblok.defoReturns;
		}
		
		return ret;
	}
	
}
