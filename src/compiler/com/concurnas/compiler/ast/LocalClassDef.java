package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.bytecode.BytecodeGennerator;
import com.concurnas.compiler.visitors.PrintSourceVisitor;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;

public class LocalClassDef extends AbstractExpression implements Expression {
	public ClassDef cd;
	public Block astRedirect;
	
	public LocalClassDef(int line, int column, ClassDef cd) {
		super(line, column);
		this.cd = cd;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(astRedirect != null && !(visitor instanceof ScopeAndTypeChecker) && !(visitor instanceof PrintSourceVisitor)){
			return visitor.visit(astRedirect);
		}
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		LocalClassDef ret = new LocalClassDef(super.line, super.column, cd);
		ret.astRedirect = astRedirect==null?null:(Block)astRedirect.copy();
		return ret;//haha
	}

	@Override
	public void setPreceedingExpression(Expression expr) {
	}

	@Override
	public Expression getPreceedingExpression() {
		return null;
	}
	
}
