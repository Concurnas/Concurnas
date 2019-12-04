package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.HashMap;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;


public class New extends ConstructorInvoke implements CanBeInternallyVectorized {

	public Type typeee; //think this can be only named..
	public FuncInvokeArgs args;
	public FuncType constType;
	public String defaultActorName;
	public String defaultActorNameFull;
	public NamedType actingOn;
	public HashMap<Integer, Integer> genericInputsToQualifingArgsAtRuntime = new HashMap<Integer, Integer>();
	public Pair<String, Integer> enumItemNameAndIndex;
	public boolean explicitNewKeywordUsed;
	public FuncType actorOfClassRef;
	public Fourple<ArrayList<Pair<Boolean, NullStatus>>, ArrayList<Integer>, Integer, Type> vectroizedDegreeAndArgs;
	private Block vectorizedRedirect=null;
	private boolean hasErrored=false;
	//public boolean preventActorRedirect = false;

	public New(int line, int col, Type typeee2, FuncInvokeArgs args, boolean explicitNewKeywordUsed) {
		super(line, col);
		this.typeee = typeee2;
		this.args = args;
		this.explicitNewKeywordUsed = explicitNewKeywordUsed;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		if(null != vectorizedRedirect){
			if(!(visitor instanceof VectorizedRedirector)){
				return vectorizedRedirect.accept(visitor);
			}
		}
		
		if(null != astRedirect && !(visitor instanceof ScopeAndTypeChecker)){
			return astRedirect.accept(visitor);
		}
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		New ret = new New(super.getLine(), super.getColumn(), (Type)typeee.copy(), args==null?null:(FuncInvokeArgs)args.copy(), explicitNewKeywordUsed);
		ret.constType=constType;
		//ret.isActor=isActor;
		ret.defaultActorName=defaultActorName;
		ret.defaultActorNameFull=defaultActorNameFull;
		ret.actingOn=actingOn;//no copy needed
		ret.genericInputsToQualifingArgsAtRuntime = new HashMap<Integer, Integer>(genericInputsToQualifingArgsAtRuntime);
		ret.enumItemNameAndIndex = null!=enumItemNameAndIndex?new Pair<String, Integer>(enumItemNameAndIndex.getA(), enumItemNameAndIndex.getB()):null;
		//ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		ret.astRedirect = astRedirect==null?null:(Node)astRedirect.copy();
		ret.actorOfClassRef = actorOfClassRef==null?null:(FuncType)actorOfClassRef.copy();
		ret.newOpOverloaded = newOpOverloaded==null?null:(FuncInvoke)newOpOverloaded.copy();
		ret.newOpOverloadedNeedsCast = newOpOverloadedNeedsCast;
		ret.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();
		//ret.preventActorRedirect = preventActorRedirect;

		return ret;
	}
	private Expression preceedingExpression;
	
	public Node astRedirect;
	public FuncInvoke newOpOverloaded;
	public boolean newOpOverloadedNeedsCast=false;
	public boolean calledInFuncRef=false;
	
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}

	@Override
	public boolean hasVectorizedRedirect() {
		return this.vectorizedRedirect != null;
	}

	@Override
	public void setVectorizedRedirect(Node vectRedirect) {
		this.vectorizedRedirect = (Block)vectRedirect;
	}
	
	@Override
	public boolean hasErroredAlready() {
		return hasErrored;
	}
	@Override
	public void setHasErroredAlready(boolean hasError) {
		this.hasErrored=hasError;
	}
	
	/*public void setShouldBePresevedOnStack(boolean should)
	{
		super.setShouldBePresevedOnStack(should);
	}*/
	
}
