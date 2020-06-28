package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.HashSet;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.NestedFuncRepoint;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;

public class ArrayDef extends AbstractExpression implements Expression, ArrayElementGettable {

	ArrayList<Expression> arrayElements = new ArrayList<Expression>();
	private ArrayList<Expression> disambiguatedArrayElements = null;
	//public boolean isNullList=false;
	
	public ArrayDef(int line, int col, ArrayList<Expression> arrayElements) {
		super(line, col);
		this.arrayElements =arrayElements;
	}
	
	public ArrayDef(int line, int col, Expression... arrayElements) {
		super(line, col);
		
		ArrayList<Expression> exps = new ArrayList<Expression>(arrayElements.length);
		for(Expression e: arrayElements){
			exps.add(e);
		}
		
		this.arrayElements =exps;
	}

	public ArrayDef(int line, int col) {
		//empty array a = [];
		super(line, col);
	}
	
	public ArrayList<Expression> getArrayElements(Visitor askee){
		if(askee instanceof ScopeAndTypeChecker || askee instanceof NestedFuncRepoint){
			return arrayElements;
		}
		
		return disambiguatedArrayElements != null? disambiguatedArrayElements:arrayElements;
	}
	
	public void setDisambiguatedElements(ArrayList<Expression> dis){
		this.disambiguatedArrayElements = dis;
	}
	

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public Type setTaggedType(Type tt){
		if(tt != null && tt.getArrayLevels() >= 2 ){//TODO: remove null check, why are things setting this to be null?
			Type ct = (Type)tt.copy();
			ct.setArrayLevels(ct.getArrayLevels()-1);
			
			for(Expression e : arrayElements){//ensure that the components know their type (place)
				Type setTo = ct;
				Type already = e.getTaggedType();
				if(already != null) {
					if(already.getNullStatus() != setTo.getNullStatus()) {
						setTo = (Type)setTo.copy();
						setTo.setNullStatus(already.getNullStatus());
					}
				}
				
				e.setTaggedType(setTo);
			}
		}
		return super.setTaggedType(tt);
	}
	
	@Override
	public Node copyTypeSpecific() {
		ArrayDef ret = new ArrayDef(super.line, super.column, (ArrayList<Expression>) Utils.cloneArrayList(arrayElements));
		ret.disambiguatedArrayElements = (ArrayList<Expression>) Utils.cloneArrayList(disambiguatedArrayElements);
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		ret.notes = notes==null?null:new HashSet<String>(notes);
		ret.isArray=isArray;
		ret.emptyArrayOk=emptyArrayOk;
		ret.isComplexAppendOp=isComplexAppendOp;
		ret.removedAmig=removedAmig;
		ret.supressArrayConcat=supressArrayConcat;
		return ret;
	}
	private Expression preceedingExpression;
	public boolean isArray = false;
	public int isComplexAppendOp=-1;
	public Boolean removedAmig = null;
	public boolean emptyArrayOk=false;
	public boolean supressArrayConcat;
	
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
}
