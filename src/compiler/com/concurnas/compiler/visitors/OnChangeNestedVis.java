package com.concurnas.compiler.visitors;


import com.concurnas.compiler.ast.Await;
import com.concurnas.compiler.ast.BreakStatement;
import com.concurnas.compiler.ast.ContinueStatement;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.LambdaDef;
import com.concurnas.compiler.ast.OnChange;
import com.concurnas.compiler.ast.OnEvery;
import com.concurnas.compiler.ast.ReturnStatement;

/**
 * Gennerally when processing an onchange (e.g. to determine which types are returned from break, continue statements etc) we do not wish to
 * visit nested elements: functions, lambdas, onchange( + await, every) etc (+classes if we permit them to be locally defined etc etc).
 *
 */
public abstract class OnChangeNestedVis extends AbstractVisitor {
/*		@Override
		public abstract Object visit(ReturnStatement returnStatement);
		
		@Override
		public abstract Object visit(ContinueStatement continueStatement);
		
		@Override
		public abstract Object visit(BreakStatement breakStatement);*/
		
		@Override//skip
		public Object visit(OnChange onChange){return null;}
		@Override//skip
		public Object visit(Await onChange){return null;}
		@Override//skip
		public Object visit(OnEvery onChange){return null;}
		@Override//skip
		public Object visit(FuncDef onChange){return null;}
		@Override//skip
		public Object visit(LambdaDef onChange){return null;}
		//JPT: above is a bit nasty, we do this to avoid issues with nested onchange etc
	
}
