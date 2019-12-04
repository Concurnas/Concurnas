package com.concurnas.compiler.visitors;

import java.util.ArrayList;

import com.concurnas.compiler.ast.AsyncBlock;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.LambdaDef;
import com.concurnas.compiler.ast.Line;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.MatchStatement;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.OnChange;

/**
 * Checks
 *	Flag up lines on their own which cannot be on their own, e.g. err = { 2;2; }
 * TODO: maybe this can be combiend into the ScopeAndTypeChecker
 */
public class FlagLoners extends AbstractErrorRaiseVisitor {
	public FlagLoners(String fullPathFileName) {
		super(fullPathFileName);
	}
	
	public void doOperation(Block lexedAndParsedAST) {
		//reset
		this.visit(lexedAndParsedAST);
	}
	
	@Override
	public Object visit(AsyncBlock asyncBlock) {
		return super.visit(asyncBlock);
	}
	

	@Override
	public Object visit(OnChange onChange) {
		for(Node e: onChange.exprs){
			e.accept(this);
		}
		if(onChange.body != null){
			onChange.body.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(Block block){
		Object ret = super.visit(block);
		ArrayList<LineHolder> lines = block.getLinesExcludeSynthetics();
		int sz = lines.size()-1;
		
		for(int n=0; n < sz; n++){//look at all but last line
			Line line = lines.get(n).l;
			if(!line.getCanBeOnItsOwnLine()){
				this.raiseError(line.getLine(), line.getColumn(), "Expression cannot appear on its own line");
			}
		}
		
		if(sz > -1 && !block.getShouldBePresevedOnStack()) {
			Line lastLine = lines.get(sz).l;
			if(!lastLine.getCanBeOnItsOwnLine() && !block.getShouldBePresevedOnStack()){
				this.raiseError(lastLine.getLine(), lastLine.getColumn(), "Expression cannot appear on its own line");
			}
		}
		
		return ret;
	}
	
	
	@Override
	public Object visit(MatchStatement matchStatement) {
		if(matchStatement.astRedirect != null){
			return visit(matchStatement.astRedirect);
		}
		return null;
	}
	
	
	@Override
	public Object visit(FuncDef funcDef) {
		if(funcDef.ignore) {
			return null;
		}
		return super.visit(funcDef);
	}
	
	@Override
	public Object visit(LambdaDef lambdaDef) {
		if(lambdaDef.ignore) {
			return null;
		}
		return super.visit(lambdaDef);
	}
	
}