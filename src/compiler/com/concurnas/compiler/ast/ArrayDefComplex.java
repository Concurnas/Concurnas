package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;

public class ArrayDefComplex extends AbstractExpression implements Expression, ArrayElementGettable {
	private Expression preceedingExpression;
	private ArrayList<Expression> items;
	public ArrayList<Expression> bcarrayElements;
	public boolean concatValid=true;
	private ArrayList<Expression> disambiguatedArrayElements = null;
	public Boolean removedAmig = null;
	
	public ArrayDefComplex(int line, int column, ArrayList<Expression> items) {
		super(line, column);
		this.items = items;
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
	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		ArrayDefComplex ret = new ArrayDefComplex(line, column, (ArrayList<Expression>) Utils.cloneArrayList(items));
		ret.bcarrayElements = bcarrayElements==null?null:(ArrayList<Expression>) Utils.cloneArrayList(bcarrayElements);
		ret.concatValid = concatValid;
		ret.removedAmig = removedAmig;
		return ret;
	}
	
	public ArrayList<Expression> getArrayElements(Visitor askee){
		if(askee instanceof ScopeAndTypeChecker){
			return items;
		}
		
		return disambiguatedArrayElements != null? disambiguatedArrayElements:items;
	}
	
	public void setDisambiguatedElements(ArrayList<Expression> dis){
		this.disambiguatedArrayElements = dis;
	}
	
	@Override
	public Type setTaggedType(Type tt) {
		if(this.items.size() == 1){
			items.get(0).setTaggedType(tt);
			if(null != bcarrayElements){
				bcarrayElements.get(0).setTaggedType(tt);
			}
		}
		return super.setTaggedType(tt);
	}	

}
