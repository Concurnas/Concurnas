package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class RefName extends AbstractExpression implements Expression {

	public String name;
	public String bytecodename;
	public boolean isIsolated = true;//e.g. x.y //y as refName is not isolate, but y // y as refname IS isolated - dot operator will fiddle with this as required
	public TypeAndLocation resolvesTo;
	public boolean ignoreWhenGenByteCode = false;
	public boolean sourceOfNameGeneric = false;
	public Node astRedirectforOnChangeNesting;//hack
	public Node astRedirect;
	public Expression astRedirectForAll;
	//public boolean permitDollarPrefixRefName = true;//default ok to start name wiht $ for syntehitics etc
	public Fourple<Boolean, NamedType, String, FuncType> isMapGetter=null;
	//public boolean popOnEntry;
	

	public RefName(String name) {
		this(0, 0, name);
	}
	
	public RefName(int line, int col, String name) {
		super(line, col);
		this.name = name;
	}
	
	/*public RefName(int line, int col, String name, boolean permitDollarPrefixRefName) {
		this(line, col, name);
		this.permitDollarPrefixRefName = permitDollarPrefixRefName;
	}*/
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(astRedirectForAll != null){
			return astRedirectForAll.accept(visitor);
		}
		
		if(null != astRedirect && !(visitor instanceof ScopeAndTypeChecker)){
			return astRedirect.accept(visitor);
		}
		
		return visitor.visit(this);
	}
	
	@Override
	public void setPreceededByThis(boolean preceededByThis) {
		super.setPreceededByThis(preceededByThis);
		if(this.astRedirect != null) {
			this.astRedirect.setPreceededByThis(preceededByThis);
		}
	}
	
	@Override
	public boolean isPermissableToGoOnRHSOfADot(Node preceededBy)
	{
		return true;
	}
	
	@Override
	public String toString()
	{
		return this.getLine() +":"+name + "->" + resolvesTo;
	}
	
	@Override
	public Node copyTypeSpecific() {
		RefName ret = new RefName(super.getLine(), super.getColumn(), name);
		ret.bytecodename=bytecodename;
		ret.resolvesTo=resolvesTo;
		ret.ignoreWhenGenByteCode=ignoreWhenGenByteCode;
		//ret.popOnEntry=popOnEntry;
		ret.sourceOfNameGeneric=sourceOfNameGeneric;
		ret.astRedirectforOnChangeNesting=null==astRedirectforOnChangeNesting?null:astRedirectforOnChangeNesting.copy();
		ret.isMapGetter=isMapGetter==null?null:new Fourple<Boolean, NamedType, String, FuncType>(isMapGetter.getA(), isMapGetter.getB().copyTypeSpecific(), isMapGetter.getC(), (FuncType) (isMapGetter.getC()==null?null:isMapGetter.getD().copy()));
		ret.castTo = castTo==null?null:(Type)castTo.copy();
		ret.castFrom = castFrom==null?null:(Type)castFrom.copy();
		ret.inCastExpr = inCastExpr;
		ret.supressUnassign = supressUnassign;
		ret.origin = this;
		ret.inferNonNullable = inferNonNullable;
		ret.nameAndLocKey = nameAndLocKey;
		//ret.astRedirect = astRedirect == null?null:astRedirect.copy();
		//ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		//hmm unlikely fix

		//ret.permitDollarPrefixRefName=permitDollarPrefixRefName;
		//ret.setTaggedType(this.getTaggedType());
		return ret;
	}
	private Expression preceedingExpression;
	public Type castTo;
	public Type castFrom;
	public boolean inCastExpr=false;
	public boolean supressUnassign = false;
	private RefName origin;
	public boolean inferNonNullable;
	public Pair<String, Boolean> nameAndLocKey;
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
	
	@Override
	public Type getTaggedType(){
		if(astRedirect != null) {
			return astRedirect.getTaggedType();
		}
		
		Type ret = null != this.astRedirectforOnChangeNesting?this.astRedirectforOnChangeNesting.getTaggedType():super.getTaggedType();
		if(ret==null){//bit of a hack, if this has been scope and type checked correctly when why do we need to obtain the type from here?
			ret = super.getTaggedType();
		}
		
		return ret;
	}

	public void overrideFuncName(String with) {
		this.name = with;
		if(origin != null) {
			origin.overrideFuncName(with);
		}
		
	}
	
}
