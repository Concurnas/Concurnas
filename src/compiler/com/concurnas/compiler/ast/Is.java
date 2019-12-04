package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class Is extends AbstractExpression implements Expression, CanBeInternallyVectorized/*, VectorizableElements*/ {

	public Expression e1;
	public ArrayList<Type> typees;
	public boolean inverted;
	//public boolean isDefaultActor;
	private Expression preceedingExpression;
	//public boolean isas;
	public boolean canBeUsedAsIsas = false;

	public Is(int line, int col, Expression e1, Type typee, boolean inverted) {
		this(line, col, e1, new ArrayList<Type>(), inverted);
		typees.add(typee);
	}
	
	public Is(int line, int col, Expression e1, ArrayList<Type> typees, boolean inverted) {
		super(line, col);
		this.e1 = e1;
		this.typees = typees;
		this.inverted = inverted;
	}

	@Override
	public boolean getCanBeOnItsOwnLine(){
		return true;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(null != vectorizedRedirect){
			if(!(visitor instanceof VectorizedRedirector)){
				return vectorizedRedirect.accept(visitor);
			}
		}
		
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		Is ret = new Is(super.getLine(), super.getColumn(), (Expression)e1.copy(), (ArrayList<Type>) Utils.cloneArrayList(this.typees), inverted);
		//ret.isDefaultActor = isDefaultActor;
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		//ret.isas=isas;
		ret.canBeUsedAsIsas=canBeUsedAsIsas;
		ret.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();

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


	public ArrayList<Pair<Boolean, NullStatus>> depth = null;
	private Block vectorizedRedirect=null;
	
	@Override
	public boolean hasVectorizedRedirect() {
		return this.vectorizedRedirect != null;
	}

	@Override
	public void setVectorizedRedirect(Node vectRedirect) {
		this.vectorizedRedirect = (Block)vectRedirect;
	}


	
	private boolean hasErrored=false;
	@Override
	public boolean hasErroredAlready() {
		return hasErrored;
	}
	@Override
	public void setHasErroredAlready(boolean hasError) {
		this.hasErrored=hasError;
	}
	
	/*@Override
	public List<VectorizationConfig> getAllElements() {
		
		List<VectorizationConfig> allExprs = new ArrayList<VectorizationConfig>(2);
		
		allExprs.add(new VectorizationConfig(this.e1, true, null, false, false, false));
		
		return allExprs;
	}
	
	@Override
	public void setAllElements(List<Expression> newones) {
		e1 = newones.get(0);
	}*/
	
}
