package com.concurnas.compiler.visitors;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Stack;

import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.interfaces.Expression;

public class AbstractErrorRaiseVisitor extends AbstractVisitor {
	
	private final LinkedHashSet<ErrorHolder> errors = new LinkedHashSet<ErrorHolder>();
	protected final String fullPathFileName;
	
	protected Stack<FuncDef> errorLocation = new Stack<FuncDef>();
	public void pushErrorContext(FuncDef xxx) {
		errorLocation.push(xxx);
	}
	public FuncDef popErrorContext(){
		return errorLocation.pop();
	}
	
	
	@Override
	public Object visit(LineHolder lineHolder) {
		this.enterLine();
		Object got = lineHolder.l.accept(this);
		Type ret = null;
		if(got instanceof Type) {
			ret = (Type)got;
		}
		
		this.leaveLine();
		return ret;
	}
	
	protected AbstractErrorRaiseVisitor(String fullPathFileName)
	{
		this.fullPathFileName = fullPathFileName;
	}
	
	
	public LinkedHashSet<ErrorHolder> getErrors() {
		return this.errors;
	}
	
	Stack<HashMap<Integer, ErrorHolder>> errorForLine = new Stack<HashMap<Integer, ErrorHolder>>();
	
	protected void enterLine()
	{
		errorForLine.add(new HashMap<Integer, ErrorHolder>());
	}
	
	protected void leaveLine()
	{
		if(!errorForLine.isEmpty())
		{
			HashMap<Integer, ErrorHolder> errs = errorForLine.pop();
			if(null != errs)
			{
				for(ErrorHolder err: errs.values())
				{
					this.errors.add(err);
				}
			}
		}
	}
	
	public void raiseError(int line, int column, String error)
	{
		boolean isEmpty = errorForLine.isEmpty();
		if(isEmpty){//JPT: see if u can remove this, its a bit ugly
			enterLine();
		}
		
		HashMap<Integer, ErrorHolder> currentLineToErr = errorForLine.peek();
		if(null != currentLineToErr)
		{
			if(!currentLineToErr.containsKey(line))
			{//add if one has not already been assigned
				currentLineToErr.put(line, new ErrorHolder(this.fullPathFileName, line, column, error, null, errorLocation.isEmpty()?null:errorLocation.peek()  ) );
			}
		}
		
		if(isEmpty){
			leaveLine();
		}
	}

	public void visit(Expression cond) {
		cond.accept(this);
	}
}
