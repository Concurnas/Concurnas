package com.concurnas.compiler.bytecode;

import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.DuffAssign;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.FuncInvoke;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.AbstractVisitor;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Unskippable;

/**
Turns this:
shared abs = new Integer(80)

into:
abs Integer //module level variable
abs$sharedInit()

def abs$sharedInit() => new Integer(80)

Note that this: 
shared abs Integer

stays as:
shared abs Integer

 */
public class ModuleLevelSharedVariableFuncGennerator extends AbstractVisitor  implements Unskippable{

	private Block currentBlock;
	private int cblockno = 0;
	
	@Override
	public Object visit(Block block) {
		Block prevBlock = currentBlock;
		currentBlock = block;
		
		LineHolder lh = block.startItr();
		
		int n=0;
		while(lh != null)
		{
			n++;
			cblockno = n;
			lh.accept(this);
			lh = block.getNext();
		}
		
		currentBlock = prevBlock;
		return null;
	}
	
	@Override
	public Object visit(AssignNew assignNew) {
		if(assignNew.isModuleLevelShared && null != assignNew.expr) {
			FuncDef repoint = FuncDef.build((Type)ScopeAndTypeChecker.const_void.copy());
			repoint.funcName = assignNew.name + "$sharedInit";
			
			int line = assignNew.getLine();
			int col = assignNew.getColumn();
			
			//refs?
			repoint.funcblock.add(new AssignExisting(line, col, new RefName(assignNew.name), assignNew.eq, (Expression)assignNew.expr.copy()));
			
			assignNew.expr = null;
			
			currentBlock.add(cblockno, repoint);
			currentBlock.add(cblockno+1, new LineHolder(new DuffAssign(FuncInvoke.makeFuncInvoke(line, col, repoint.funcName))));
			
		}
		
		
		return null;
	}
	
	
}
