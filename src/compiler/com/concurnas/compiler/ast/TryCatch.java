package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.HashMap;

import org.objectweb.asm.Label;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.bytecode.TryCatchLabelTagVisitor.CatchBlockAllocator.NestedStartEndContContainer;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class TryCatch extends CompoundStatement {

	public Node astRepoint = null;

	public Block blockToTry;
	public ArrayList<CatchBlocks> cbs;
	public Block finalBlock;
	// public TryCatchBlockPreallocatedLabelsEtc stuff;
	public boolean hasDefoThrownException;
	public Label carryOn;
	public Block finalBlockOnEndOfTry;
	public Label startOfTheFinalyBlock;
	public Label endOfTryBlock = new Label();
	public Block finBlockWhenItDefoRets;
	public ArrayList<Pair<Label, Type>> catchBlockEntryLabels;
	public Label finalHandler;
	public Label LabelPreGOTOOnBlockNonDefoRet;
	// public Label finalEnd;
	public HashMap<Label, Label> endOfCatchToAttachedFinal;
	public Label finalEnd;
	public NestedStartEndContContainer nestedStartEndContContainer;
	public ArrayList<Line> tryWithResources = new ArrayList<Line>();
	public Thruple<Label, Label, Label> supressFinnallyException = null;
	public Block tryWithResourcesastRepoint;
	
	public TryCatch(int line, int col, Block blockToTry, ArrayList<CatchBlocks> cbs, Block finalBlock) {
		super(line, col);
		this.blockToTry = blockToTry;
		this.cbs = cbs;
		this.finalBlock = finalBlock;
	}

	@Override
	public Node copyTypeSpecific() {
		TryCatch ret = new TryCatch(super.line, super.column, null == blockToTry ? null : (Block) blockToTry.copy(), (ArrayList<CatchBlocks>) Utils.cloneArrayList(cbs), null == finalBlock ? null : (Block) finalBlock.copy());
		// ret.stuff=stuff;
		ret.hasDefoThrownException = hasDefoThrownException;
		ret.finalBlockOnEndOfTry = finalBlockOnEndOfTry;
		ret.startOfTheFinalyBlock = startOfTheFinalyBlock;
		ret.astRepoint = astRepoint;//TODO: shouldnt this be a copy?
		ret.shouldBePresevedOnStack = shouldBePresevedOnStack;// ughly
		ret.isReallyA = isReallyA;// ughly
		ret.supressFinnallyException = supressFinnallyException;// ughly
		ret.tryWithResources = (ArrayList<Line>) Utils.cloneArrayList(this.tryWithResources);
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();

		// no carryoncopy - should be redirected
		return ret;
	}
	private Expression preceedingExpression;
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}

	public TryCatch(int line, int col, Block blockToTry, Block finalBlock) {
		this(line, col, blockToTry, new ArrayList<CatchBlocks>(), finalBlock);
	}

	public TryCatch(int line, int col, Block blockToTry, ArrayList<CatchBlocks> cbs) {
		this(line, col, blockToTry, cbs, null);
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(null != tryWithResourcesastRepoint){
			return visitor.visit(tryWithResourcesastRepoint);
		}
		return visitor.visit(this);
	}

	public boolean hasFinal() {
		return this.finalBlock != null && !this.finalBlock.isEmpty();
	}

	private boolean shouldBePresevedOnStack = false;

	public boolean finalBlockRetrunsVoid =false;

	public String isReallyA;

	public boolean gethasDefoReturnedConventionally() {
		boolean ret = this.blockToTry.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch();
		
		for(CatchBlocks cbk : this.cbs){
			ret &= cbk.catchBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch();
		}
				
		return ret;
	}

	public void setShouldBePresevedOnStack(boolean should) {
		//System.err.println("setShouldBePresevedOnStack: " + should);
		
		shouldBePresevedOnStack = should;

		if(this.tryWithResourcesastRepoint != null){
			this.tryWithResourcesastRepoint.setShouldBePresevedOnStack(should);
			return;
		}
		
		if (null != this.finalBlock ) {
			if(this.finalBlockRetrunsVoid){
				this.finalBlock.setShouldBePresevedOnStack(false);
				this.blockToTry.setShouldBePresevedOnStack(should);
				for (CatchBlocks e : this.cbs) {
					e.catchBlock.setShouldBePresevedOnStack(should);
				}
			}
			else{
				this.finalBlock.setShouldBePresevedOnStack(false);
				//this.blockToTry.setShouldBePresevedOnStack(isReallyA != null?true: false);
				this.blockToTry.setShouldBePresevedOnStack(should);
				for (CatchBlocks e : this.cbs) {
					e.catchBlock.setShouldBePresevedOnStack(should);
				}
			}
			
		} else {
			//System.err.println("tc: " + should);
			this.blockToTry.setShouldBePresevedOnStack(should);
			for (CatchBlocks e : this.cbs) {
				e.catchBlock.setShouldBePresevedOnStack(should);
			}
		}
		 
	}

	public boolean getShouldBePresevedOnStack() {// TODO: extend this logic to
													// statements in genneral?
		if(this.tryWithResourcesastRepoint != null){
			return this.tryWithResourcesastRepoint.getShouldBePresevedOnStack();
		}
		return shouldBePresevedOnStack;
	}

	public boolean hasEcapedNormally() {
		if(gethasDefoReturnedConventionally()){
			return true;
		}
		if(this.hasDefoThrownException){
			return true;
		}
		return false;
	}
	
	
	public boolean getHasAttemptedNormalReturn(){
		//e.g. if, or if elses any legs attempt to return
		boolean ret = this.blockToTry.defoReturns;
		for (CatchBlocks e : this.cbs) {
			ret &= e.catchBlock.defoReturns;
		}
		//finnally cannot have a conventional return stmt [cos too complex for analysis and a bit of an antipattern anyway]
		return ret;
	}

}
