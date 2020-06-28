package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.ast.ArrayRefLevelElementsHolder.ARElementType;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.ast.util.NullableArrayElement;
import com.concurnas.compiler.ast.util.NullableArrayElementss;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class ArrayRef extends AbstractExpression implements Expression, CanBeInternallyVectorized {

	public ArrayRefLevelElementsHolder arrayLevelElements;//[2, 3:4][2][:3, var, 3:var2];
	public Expression expr;
	//public boolean isList =false;
	public boolean dupLastThingIfRef=false;
	public Type penultimateOperatingOnType;
	public ArrayList<Pair<Boolean, NullStatus>> vectDepth=null;
	public List<Pair<Boolean, NullStatus>> vectArgumentDepth = null;

	public void setPreceededByThis(boolean preceededByThis) {
		
		((Node)this.expr).setPreceededByThis(preceededByThis);
	}
	
	public void setPreceededBySuper(boolean preceededBysup) {
		
		((Node)this.expr).setPreceededBySuper(preceededBysup);
	}
	
	public ArrayRef(int line, int col, Expression expr, ArrayRefLevelElementsHolder arrayLevelElements) {
		super(line, col);
		this.expr = expr;
		this.arrayLevelElements =arrayLevelElements;
	}
	
	public static ArrayRef ArrayRefOne(int line, int col, Expression expr, Expression arrayLevelElements){
		ArrayRefLevelElementsHolder arleh = new ArrayRefLevelElementsHolder();
		ArrayList<ArrayRefElement> item = new ArrayList<ArrayRefElement>(1);
		item.add(new ArrayRefElement(line, col, arrayLevelElements));
		arleh.add(false, false, item);
		
		return new ArrayRef(line, col, expr, arleh);
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		if(null != vectorizedRedirect){
			if(!(visitor instanceof VectorizedRedirector)){
				return vectorizedRedirect.accept(visitor);
			}
		}
		
		if(astRedirectArrayCons != null && !(visitor instanceof ScopeAndTypeChecker)){
			return astRedirectArrayCons.accept(visitor);
		}
		
		return visitor.visit(this);
	}
	

	public boolean couldBeGenericMethodParams(){
		return arrayLevelElements.getAll().size()==1 && expr instanceof RefName;
	}
	
	public boolean isPermissableToGoOnRHSOfADot(Node preceededBy)
	{
		return true;
	}

	public ArrayRefElement getLastArrayRefElement(){
		return arrayLevelElements.getLastArrayRefElement();
	}
	
	public ArrayList<ArrayRefElement> getFlatALE()
	{
		ArrayList<ArrayRefElement> ret = new ArrayList<ArrayRefElement>();
		for(NullableArrayElementss levelx : arrayLevelElements.getAll())
		{
			ArrayList<ArrayRefElement> level = levelx.elements;
			ret.addAll(level);
		}
		return ret;
	}
	
	public ArrayList<NullableArrayElement> getFlatALEWithNullSafe()
	{
		ArrayList<NullableArrayElement> ret = new ArrayList<NullableArrayElement>();
		for(NullableArrayElementss levelx : arrayLevelElements.getAll())
		{
			boolean nullSafe = levelx.nullsafe;
			boolean nna = levelx.nna;
			for(ArrayRefElement level : levelx.elements) {
				ret.add(new NullableArrayElement(nullSafe, nna, level));
				nullSafe=false;
			}
		}
		return ret;
	}
	
	public Thruple<Type, Type, ARElementType> getLastTaggedType(){
		return arrayLevelElements.getLastTaggedType();
	}
	
	@Override
	public Node copyTypeSpecific() {
		ArrayRef ret =  new ArrayRef(super.line, super.column, (Expression)expr.copy(), arrayLevelElements.clone());
		ret.dupLastThingIfRef=dupLastThingIfRef;
		ret.penultimateOperatingOnType=penultimateOperatingOnType==null?null:(Type)penultimateOperatingOnType.copy();
		ret.astRedirectArrayCons=astRedirectArrayCons==null?null:(ArrayConstructor)astRedirectArrayCons.copy();
		ret.arrayConstructorExtraEmptyBracks=arrayConstructorExtraEmptyBracks;
		//ret.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();
		//ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		//hmm unlikely fix
		
		
		
		
		return ret;
	}
	private Expression preceedingExpression;
	public ArrayConstructor astRedirectArrayCons;
	public Integer arrayConstructorExtraEmptyBracks;
	
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}

	public void setlhsOfAssignment(Expression rhsOnAssignmentType, AssignStyleEnum eq) {
		for(NullableArrayElementss levelx  : arrayLevelElements.getAll()){
			ArrayList<ArrayRefElement> level = levelx.elements;
			for(ArrayRefElement are : level){
				are.rhsOfAssigmentType=rhsOnAssignmentType;
				are.rhsOfAssigmentEQ=eq;
			}
		}
	}
	
	public boolean hasBeenVectorized(){
		return this.expr.hasBeenVectorized();
	}

	private Expression vectorizedRedirect;
	
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
	
}
