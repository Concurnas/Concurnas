package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.compiler.visitors.util.LanguageExtensionRunner;

public class LangExt extends AbstractExpression implements Expression {

	public final String name;
	public final  String body;
	public Block astRedirectBlock;
	
	private Expression preceedingExpression;
	public boolean foundSubs = false;
	public String processed = null;
	public LanguageExtensionRunner langExtCompiler = null;
	public Long id = null;
	public boolean initalized=false;
	
	
	public LangExt(int line, int col, String name, String body) {
		super(line, col);
		this.name= name;
		this.body= body;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		if(null != astRedirectBlock && !(visitor instanceof ScopeAndTypeChecker)){
			Object last = null;
			for(LineHolder lh : astRedirectBlock.lines) {
				Line l = lh.l;
				if(l instanceof DuffAssign) {
					last = ((DuffAssign)l).e.accept(visitor);
				}else {
					last = lh.accept(visitor);
				}
			}
			
			return last;
		}
		
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		LangExt ret = new LangExt(line, column, name, body);
		ret.astRedirectBlock = astRedirectBlock == null?null:(Block)astRedirectBlock.copy();
		return ret;
	}
	
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
	
	@Override
	public void setShouldBePresevedOnStack(boolean should) {
		super.setShouldBePresevedOnStack(should);
		if(astRedirectBlock != null) {
			astRedirectBlock.setShouldBePresevedOnStack(should);
		}
	}

	@Override
	public boolean getCanBeOnItsOwnLine(){
		
		if(this.astRedirectBlock != null){
			return this.astRedirectBlock.getCanBeOnItsOwnLine();
		}
		
		return true;
	}
	
}
