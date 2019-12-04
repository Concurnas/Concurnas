package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;

public interface ForBlockMaybeParFor extends Expression {

	public void setRepointed(Block repointed);
	public void setMainBlock(Block block);
	public Block getMainBlock();
	public boolean getShouldBePresevedOnStack();
	public void setShouldBePresevedOnStack(boolean b);
	
}
