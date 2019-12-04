package com.concurnas.compiler;

import java.util.ArrayList;
import java.util.HashSet;

import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.Utils;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class CaseExpressionTuple extends CaseExpression {

	private ArrayList<CaseExpression> components;
	public HashSet<Integer> toskip = null;
	public Expression caseExpression;

	public CaseExpressionTuple(int line, int col, ArrayList<CaseExpression> components) {
		super(line, col);
		this.components = components;
	}

	@Override
	public Object accept(Visitor visitor) {
		return visitor.visit(this);
	}

	public ArrayList<CaseExpression> getComponents(){
		ArrayList<CaseExpression> ret = new ArrayList<CaseExpression>();
		if(toskip == null) {
			ret.addAll(components);
		}else {
			int n=0;
			
			for(CaseExpression ce : components) {
				if(!toskip.contains(n++)) {
					ret.add(ce);
				}
			}
		}
		
		return ret;
	}
	
	public int getComponentCountRaw() {
		return components.size();
	}
	
	@Override
	public Node copyTypeSpecific() {
		CaseExpressionTuple ret = new CaseExpressionTuple(line, super.column, (ArrayList<CaseExpression>) Utils.cloneArrayList(components));
		ret.alsoCondition = this.alsoCondition == null?null:(Expression)this.alsoCondition.copy();
		ret.caseExpression = this.caseExpression == null?null:(Expression)this.caseExpression.copy();
		return ret;
	}
}
