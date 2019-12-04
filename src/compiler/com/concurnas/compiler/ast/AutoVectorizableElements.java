package com.concurnas.compiler.ast;

import java.util.List;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.utils.Thruple;

public interface AutoVectorizableElements {
	public static class VectorizationConfig {
		public Expression expr;
		public Boolean canBeObject;
		public String opOverloadString;
		public Boolean canBeString;
		public boolean isUnary;
		public boolean oneMustBeScalar;

		public VectorizationConfig(Expression expr, Boolean canBeObject, String opOverloadString, Boolean canBeString, boolean isUnary, boolean mustDifferByOneLevel) {
			this.expr=expr;
			this.canBeObject=canBeObject;
			this.opOverloadString=opOverloadString;
			this.canBeString=canBeString;
			this.isUnary=isUnary;
			this.oneMustBeScalar=mustDifferByOneLevel;
		}
	}
	
	public List<VectorizationConfig> getAllElements();
	public void setAllElements(List<Expression> newones);
}
