package com.concurnas.compiler.bytecode;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Stack;

import org.objectweb.asm.Label;

import com.concurnas.compiler.ast.AsyncBlock;
import com.concurnas.compiler.ast.AsyncBodyBlock;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.BreakStatement;
import com.concurnas.compiler.ast.CatchBlocks;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.ContinueStatement;
import com.concurnas.compiler.ast.ElifUnit;
import com.concurnas.compiler.ast.ForBlock;
import com.concurnas.compiler.ast.ForBlockOld;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.IfStatement;
import com.concurnas.compiler.ast.Line;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.NOP;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.NotExpression;
import com.concurnas.compiler.ast.OnChange;
import com.concurnas.compiler.ast.PreEntryCode;
import com.concurnas.compiler.ast.ReturnStatement;
import com.concurnas.compiler.ast.TryCatch;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.WhileBlock;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.ast.interfaces.FuncDefI;
import com.concurnas.compiler.visitors.AbstractVisitor;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.runtime.Pair;

public class LabelAllocator extends AbstractVisitor {
	
	private boolean isLastLineInBlock = false;
	private Stack<Pair<Label, Label>> labelNeedingConsumption = new  Stack<Pair<Label, Label>>(); //[End, Start]
	//tuple -> start of block (e.g. if while stmt), end of while
	
	private Stack<Label> onBreak = new Stack<Label>();
	public Stack<Label> startOfLoop = new Stack<Label>();
	
	
	@Override
	public Object visit(AsyncBlock block) {
		//TODO: have to expand this
		if(block.body.getShouldBePresevedOnStackAndImmediatlyUsed() && block.body.getIfReturnsExpectImmediateUse()){
			tagLastThingInBlockWithImmediateUse(block.body);
		}
		
		super.visit(block.fakeLambdaDef);
		
		return super.visit(block);
	}
	
	//private boolean alreadyCreatedNewLayer = false;
	
	@Override
	public Object visit(Block block) {
		ArrayList<LineHolder> lines = block.lines;
		int sz = lines.size();
		
		if(permitTaggingOfImmediateUse && block.getShouldBePresevedOnStackAndImmediatlyUsed() && block.getIfReturnsExpectImmediateUse() && block.isolated){
			//System.err.println(String.format("igi: %s - %s", block.getLine(), block.isolated));
			tagLastThingInBlockWithImmediateUse(block);
		}
		
		if(!block.isEmpty()){
			
			labelNeedingConsumption.push(new Pair<Label, Label>(null, null));
			
			for(int n=0; n < sz; n++){
				boolean isLast = n == sz-1;
				
				boolean prev = isLastLineInBlock;
								
				if(isLast){
					isLastLineInBlock = block.isolated?prev:true;
				}
				else{
					isLastLineInBlock=isLast;
				}
				
				
				LineHolder lh = lines.get(n);
				//if(!ignoreConsume){
				//	ignoreConsume=true;
					consumeLabelIfAny((Node)lh.l);
				//}
				super.visit(lh);
				
				isLastLineInBlock = prev;
			}
			
			Label unallocated = labelNeedingConsumption.pop().getB();
			 
			
			if(null != unallocated){//carry on up chain to next block
				if(!block.isMethodBlock){//if it's a method block then it will have been consumed already by the return statement
					//unless its a constructor which always has implicit returns
					if(labelNeedingConsumption.isEmpty() ){//oh noes no chain tag main blcok
						//System.err.println("allocate before ret of top level block: " + unallocated);
						block.mustVisitLabelBeforeRet = unallocated;
					}
					else{
						Pair<Label, Label> ab = labelNeedingConsumption.pop();
						labelNeedingConsumption.push(new Pair<Label, Label>(ab.getA(), unallocated));
					}
				}
				else if(block.isConstructor){
					block.mustVisitLabelBeforeRet = unallocated;
				}
			}
			
		}
		
		
		return null;
	}
	
	private void consumeLabelIfAny(Node nd){
		
		if(!(nd instanceof ClassDef || nd instanceof FuncDefI)){
			
			if(!labelNeedingConsumption.isEmpty() && null != labelNeedingConsumption.peek().getB()){
				Pair<Label, Label> startEnd = labelNeedingConsumption.pop();
				
				Label lowes = startEnd.getB();
				nd.setLabelOnEntry(lowes);
				labelNeedingConsumption.push(new Pair<Label, Label>(startEnd.getA(), null));
			}
		}
		
		
	
	}
	
	private Label getNextLabelToContinueFrom(Label startOfLoop){
		Label found = null;
		
		if(this.isLastLineInBlock){
			//pop from stack
			int sz = labelNeedingConsumption.size()-2;
			while(sz >= 0){	//from end to start
				
				Pair<Label, Label> whileStartEnd = labelNeedingConsumption.get(sz);
				Label blkStart = whileStartEnd.getA(); 
				Label blkEnd = whileStartEnd.getB(); 
				
				found = blkStart!=null?blkStart:blkEnd;//goto start if in while loop, otherwise not
				
				if(null != found){
					break;
				}
				sz--;
			}
		}
		
		if(null == found){
			found = new Label();
			labelNeedingConsumption.pop();
			labelNeedingConsumption.push(new Pair<Label, Label>(startOfLoop, found));
		}
		
		return found;
	}
	
	private Label overrideNextLabelCont = null;
	
	private Label getNextLabelToContinueFrom(){
		return getNextLabelToContinueFrom(labelNeedingConsumption.peek().getA());
	}
	
	private void overriteNextLabelToContinueOnFrom(Label with){
		Pair<Label, Label> whileStartEnd = labelNeedingConsumption.pop();
		labelNeedingConsumption.push(new Pair<Label, Label>(with, whileStartEnd.getB()));
	}
	
	private void overriteNextLabelToContinueOnFromBranch(Label with){
		Pair<Label, Label> whileStartEnd = labelNeedingConsumption.pop();
		labelNeedingConsumption.push(new Pair<Label, Label>(whileStartEnd.getA(), with));
	}
	
	
/*	private void addNewEndingLayer(Label with){
		Tuple<Label, Label> whileStartEnd = labelNeedingConsumption.peek();
		labelNeedingConsumption.push(new Tuple<Label, Label>(with, whileStartEnd.getB()));
		
	}*/

	/*private void popLayer(){
		labelNeedingConsumption.pop();
		
	}*/
	
	private void tagLastThingInBlockWithImmediateUse(Expression expr){
		if(null != expr){
			((Node)expr).setIfReturnsExpectImmediateUse(true);
		}
	}
	
	
	//JPT: remove the next code, refactor
	@Override
	public Object visit(WhileBlock whileBlock) {
		Label onEntry = whileBlock.getLabelOnEntry();
		if(null == onEntry){
			onEntry =new Label();
		}
		whileBlock.setLabelOnEntry(onEntry);
		
		if(whileBlock.getShouldBePresevedOnStack() || whileBlock.idxVariableCreator != null || whileBlock.idxVariableAssignment != null){
			//tagLastThingInBlockWithImmediateUse(whileBlock.block);
			onEntry = whileBlock.getLabelBeforeCondCheckIfStackPrese();
			if(null == onEntry){
				onEntry = new Label();
				whileBlock.setLabelBeforeCondCheckIfStackPrese(onEntry);
			}
		}
		
		Label afterIt = whileBlock.getShouldBePresevedOnStack()?new Label():getNextLabelToContinueFrom(onEntry);
		whileBlock.setLabelAfterCode(afterIt);
		
		if(whileBlock.getShouldBePresevedOnStack()){
			whileBlock.beforeAdder =  new Label();
			overrideNextLabelCont = whileBlock.beforeAdder;//TODO: this probably ought to be a Stack based approach but so far seems ok
			//tests are complex enough, but im sure there are some more bugs in here.... :/
		}
		
		//System.err.println("afterIt: " + afterIt);
		//System.err.println("onEntry: " + onEntry);
		
		this.onBreak.push(afterIt);
		this.startOfLoop.push(onEntry);
		Object ret = super.visit(whileBlock);
		this.startOfLoop.pop();
		this.onBreak.pop();
		
		if(whileBlock.getShouldBePresevedOnStack()){
			overrideNextLabelCont=null;
		}
		
		return ret;
	}
	
	@Override
	public Object visit(ForBlockOld forBlockOld) {
		forBlockOld.startOfWorkBlock =new Label();
		//depends on if stuff exists
		Label afterIt = getNextLabelToContinueFrom(forBlockOld.startOfWorkBlock);
		
		forBlockOld.startOfPostOp = new Label();
		if(forBlockOld.postExpr != null){
			overriteNextLabelToContinueOnFrom(forBlockOld.startOfPostOp);
		}
		
		if(forBlockOld.getShouldBePresevedOnStack()){
			forBlockOld.beforeAdder =  new Label();
			overrideNextLabelCont = forBlockOld.beforeAdder;
		}
		
		forBlockOld.setLabelAfterCode(afterIt);
		
		if(forBlockOld.getShouldBePresevedOnStackAndImmediatlyUsed()){
			//tagLastThingInBlockWithImmediateUse(forBlockOld.block);
			afterIt = forBlockOld.getLabelBeforeRetLoadIfStackPrese();
			if(null == afterIt){
				afterIt = new Label();
				forBlockOld.setLabelBeforeRetLoadIfStackPrese(afterIt);
			}
		}
		
		this.onBreak.push(afterIt);
		
		this.startOfLoop.push(forBlockOld.postExpr != null ?  forBlockOld.startOfPostOp : forBlockOld.startOfWorkBlock);
		Object ret = super.visit(forBlockOld);
		this.startOfLoop.pop();
		this.onBreak.pop();
		
		if(forBlockOld.getShouldBePresevedOnStack()){
			overrideNextLabelCont=null;
		}
		
		return ret;
	}
	
	
	//JPT: refactor into one - ha yes, u even tricked urself, should have refacored!
	@Override
	public Object visit(ForBlock forBlockNew) {
		forBlockNew.startOfWorkBlock =new Label();
		//depends on if stuff exists
		Label afterIt = getNextLabelToContinueFrom(forBlockNew.startOfWorkBlock);
		
		forBlockNew.startOfPostOp = new Label();
		//if(forBlockOld.postExpr != null){ - always have postop //lol, 'postop'
		
		Type tta = forBlockNew.expr.getTaggedType();
		if(!tta.hasArrayLevels() || TypeCheckUtils.isLocalArray(tta))
		{
			forBlockNew.hasNextLabel = new Label();
			if(forBlockNew.getShouldBePresevedOnStackAndImmediatlyUsed()){
				Label x = new Label();
				overriteNextLabelToContinueOnFrom(x);//needed else go strait to hasnext block instead of ret.adder(x)
				afterIt=x;
			}
			else{
				overriteNextLabelToContinueOnFrom(forBlockNew.hasNextLabel);
			}
		}
		else{
			if(forBlockNew.getShouldBePresevedOnStackAndImmediatlyUsed()){
				Label x = new Label();
				overriteNextLabelToContinueOnFrom(x);
				afterIt=x;
			}
			else{
				overriteNextLabelToContinueOnFrom(forBlockNew.startOfPostOp);
			}
		}
		//}
		
		if(forBlockNew.getShouldBePresevedOnStack()){
			forBlockNew.beforeAdder =  new Label();
			overrideNextLabelCont = forBlockNew.beforeAdder;
		}
		
		forBlockNew.setLabelAfterCode(afterIt);
		
		if(forBlockNew.getShouldBePresevedOnStackAndImmediatlyUsed()){
			//tagLastThingInBlockWithImmediateUse(forBlockNew.block);
			afterIt = forBlockNew.getLabelBeforeRetLoadIfStackPrese();
			if(null == afterIt){
				afterIt = new Label();
				forBlockNew.setLabelBeforeRetLoadIfStackPrese(afterIt);
			}
		}
		
		this.onBreak.push(afterIt);
		this.startOfLoop.push(forBlockNew.hasNextLabel!=null?forBlockNew.hasNextLabel:forBlockNew.startOfPostOp);
		Object ret = super.visit(forBlockNew);
		this.startOfLoop.pop();
		this.onBreak.pop();
		
		if(forBlockNew.getShouldBePresevedOnStack()){
			overrideNextLabelCont=null;
		}
		
		return ret;
	}
	
	

	//private Stack<Label> afterTheTryCatchLabel = new Stack<Label>();
	
	@Override
	public Object visit(TryCatch tryCatch) {
		//printLabelState("" + tryCatch);
		if(tryCatch.astRepoint!=null){
			return tryCatch.astRepoint.accept(this);
		}
		
		//for x in y)
		
		//afterTheTryCatchLabel.push(afterTryCatch);
		boolean hasFinal = tryCatch.hasFinal();
		
		Label toGoOnContinue = null;
		if(null != overrideNextLabelCont && tryCatch.getShouldBePresevedOnStack()){
			toGoOnContinue = overrideNextLabelCont;
			//overrideNextLabelCont=null;
		}
		
		
		if(tryCatch.getShouldBePresevedOnStackAndImmediatlyUsed() && tryCatch.getIfReturnsExpectImmediateUse()){
			//Label overTry = toGoOnContinue==null?new Label():toGoOnContinue;
			Label overTry = new Label();
			overrideNextLabelCont=null;
			overriteNextLabelToContinueOnFrom(overTry);//needed else go strait to hasnext block instead of ret.adder(x)
			tryCatch.setLabelAfterCode(overTry);
		}
		else{
			Label afterTryCatch = toGoOnContinue==null?getNextLabelToContinueFrom():toGoOnContinue;
			tryCatch.setLabelAfterCode(afterTryCatch);
		}
		//printLabelState("" + tryCatch);
		
		//tryCatch.setLabelAfterCode(afterTryCatch);
		
		if(hasFinal ){
			if(!tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()){
				if(!tryCatch.blockToTry.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()){
					
					tryCatch.finalBlockOnEndOfTry = (Block)tryCatch.finalBlock.copy();
					
					Label labelAfterInsertedCode = new Label();
					labelNeedingConsumption.push(new Pair<Label, Label>(null, labelAfterInsertedCode));
					this.visit(tryCatch.finalBlockOnEndOfTry);
					labelNeedingConsumption.pop();
					
					tryCatch.finalBlockOnEndOfTry.setLabelAfterCode(labelAfterInsertedCode);
				}
			}
		}
		
		Line lastLine = tryCatch.blockToTry.getLast().l;
	
		//if ends in while, addd a nop operation
		if(lastLine instanceof WhileBlock) {
			WhileBlock first = (WhileBlock)tryCatch.blockToTry.getLast().l;
			if(!first.block.hasRetExcepOrBreaked || first.block.hasDefoBroken) {
				tryCatch.blockToTry.lines.add(new LineHolder(new NOP()));
			}
		}else if(lastLine instanceof ForBlock) {
			ForBlock first = (ForBlock)tryCatch.blockToTry.getLast().l;
			if(first.block.hasRetExcepOrBreaked) {
				tryCatch.blockToTry.lines.add(new LineHolder(new NOP(true)));//hack such that label is consumed correctly for for loop with certain return
			}
		}
		
		
		//if (hasFinal && (( (tryCatch.finalBlock.hasDefoReturnedOrThrownException() && tryCatch.cbs.isEmpty())  || !tryCatch.blockToTry.hasDefoReturnedOrThrownException()))) {
		if (hasFinal){// && tryCatch.finalBlock.hasDefoReturnedOrThrownException()){
			tryCatch.startOfTheFinalyBlock = new Label();
			
			labelNeedingConsumption.push(new Pair<Label, Label>(null, tryCatch.startOfTheFinalyBlock));
			
			tryCatch.blockToTry.accept(this);
			
			consumeLabelIfAny((Node) tryCatch.finalBlock.lines.get(0).l);
			labelNeedingConsumption.pop();
			
			
			
		}
		else{
			//labelNeedingConsumption.push(new Tuple<Label, Label>(null, tryCatch.endOfTryBlock));
			
			tryCatch.blockToTry.accept(this);
			
			//labelNeedingConsumption.pop();
			
		}
		
		//tag me, and should also be last logical part of code

		if(!tryCatch.cbs.isEmpty()){
			ArrayList<Pair<Label, Type>> catchBlockEntryLabels = new ArrayList<Pair<Label, Type>>();
			for(CatchBlocks cat : tryCatch.cbs)
			{
				/*if(cat.getShouldBePresevedOnStackAndImmediatlyUsed()){
					overriteNextLabelToContinueOnFrom(new Label());//needed else go strait to hasnext block instead of ret.adder(x)
				}*/
				
				cat.accept(this);//after block is fin final after the cathc?
				Label entryOfCatch = new Label();
				catchBlockEntryLabels.add(new Pair<Label, Type>(entryOfCatch, cat.getTaggedType()));
				cat.entryLabel = entryOfCatch;
				
				if(hasFinal){
					
					//if defo ret, then tag as fin block entry
					
					Label finForCath = new Label();
					cat.finLabel = finForCath;
					cat.attachedFinalBlock = (Block)tryCatch.finalBlock.copySetFirstLogical(finForCath);//may not always be needed
					cat.attachedFinalBlock.accept(this);
				}
			}
			tryCatch.catchBlockEntryLabels=catchBlockEntryLabels;
		}
		
		
		if(hasFinal)
		{
			if(tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()){
				tryCatch.finBlockWhenItDefoRets = (Block)tryCatch.finalBlock.copy();//may not always be needed
				tryCatch.finBlockWhenItDefoRets.accept(this);
			}
			
			//printLabelState(""+tryCatch);

			labelNeedingConsumption.push(new Pair<Label, Label>(tryCatch.getLabelAfterCode(), null));//any ending if statements inside the finally need to point to the end of the code 
			tryCatch.finalBlock.accept(this);
			labelNeedingConsumption.pop();
			//Label finalHandler = tryCatch.finalBlock.getFirstLogicalLabel(new Label());
			//tryCatch.finalHandler  = finalHandler;
		}
		
		
		
		return null;
	}
	
/*	private void printLabelState(String oni){
		StringBuilder sb = new StringBuilder(String.format("[%s] items needing consumption: ", oni));
		for(Tuple<Label, Label> item : this.labelNeedingConsumption){
			sb.append("["+item.getA() + " " + item.getB()+"]");
			sb.append(",");
		}
		System.err.println(sb);
	}*/
	
	//private LinkedList<FuncDef> toCheckOnExit = new LinkedList<FuncDef>();
	
	private Stack<FuncDef> currentFD = new Stack<FuncDef>();
	
	private IdentityHashMap<FuncDef, LinkedList<FuncDef>> toCheckOnExit = new IdentityHashMap<FuncDef, LinkedList<FuncDef>>(); //id cos somehow nested methods can have same signature sometimes, weird...
	
	private void addToCheckOnExit(FuncDef toCheck){
		toCheckOnExit.get(currentFD.peek()).add(toCheck);
	}
	
	@Override
	public Object visit(FuncDef df){
		currentFD.push(df);
		toCheckOnExit.put(df, new LinkedList<FuncDef>());
		Object ret = super.visit(df);

		LinkedList<FuncDef> toCheckOnExitx = toCheckOnExit.get(df);
		
		if(null != toCheckOnExitx && !toCheckOnExitx.isEmpty()){
			//LinkedList<FuncDef> proce = toCheckOnExitx;
			while(!toCheckOnExitx.isEmpty()){
				super.visit(toCheckOnExitx.pop());
				/*if(!toCheckOnExitx.isEmpty()){
					proce.push(toCheckOnExitx.pop());
				}*/
			}
		}
		
		
		//toCheckOnExit.remove(df);//comment out side something is causing early exit... maybe a copy?
		currentFD.pop();
		
		return ret;
	}
	
	@Override
	public Object visit(OnChange onChange){
		
		if(null != onChange.applyMethodFuncDef){
			//toCheckOnExit.add(onChange.applyMethodFuncDef);
			addToCheckOnExit(onChange.applyMethodFuncDef);
		}
		if(null != onChange.initMethodNameFuncDef){
			//toCheckOnExit.add(onChange.initMethodNameFuncDef);
			addToCheckOnExit(onChange.initMethodNameFuncDef);
		}
		if(null != onChange.cleanUpMethodFuncDef){
			//toCheckOnExit.add(onChange.cleanUpMethodFuncDef);
			addToCheckOnExit(onChange.cleanUpMethodFuncDef);
		}
		
		return null;//null is ok
	}
	
	@Override
	public Object visit(AsyncBodyBlock asyncbb){
		
		for(LineHolder lh : asyncbb.mainBody.lines){
			Line l = lh.l;
			if(l instanceof OnChange){
				((OnChange)l).accept(this);
			}
		}
		
		if(null != asyncbb.applyMethodFuncDef){
			//toCheckOnExit.add(asyncbb.applyMethodFuncDef);
			addToCheckOnExit(asyncbb.applyMethodFuncDef);
		}
		if(null != asyncbb.initMethodNameFuncDef){
			//toCheckOnExit.add(asyncbb.initMethodNameFuncDef);
			addToCheckOnExit(asyncbb.initMethodNameFuncDef);
		}
		if(null != asyncbb.cleanUpMethodFuncDef){
			//toCheckOnExit.add(asyncbb.cleanUpMethodFuncDef);
			addToCheckOnExit(asyncbb.cleanUpMethodFuncDef);
		}
		
		return null;
		
	}

	
	@Override
	public Object visit(IfStatement ifStatement) {
		//System.err.println("if on complete: " + ifStatement.onComplete);
		
		ifStatement.iftest.accept(this);
		
		boolean hasElse = ifStatement.elseb !=null && !ifStatement.elseb.isEmpty();
		boolean hasElif = !ifStatement.elifunits.isEmpty();
	
		
		Label toGoOnContinue = null;
		if(null != overrideNextLabelCont && ifStatement.getShouldBePresevedOnStack()){
			toGoOnContinue = overrideNextLabelCont;
			//overrideNextLabelCont=null;
		}
		
		
		if(ifStatement.getShouldBePresevedOnStackAndImmediatlyUsed()){
			overrideNextLabelCont=null;
			
			ifStatement.onComplete = new Label();
			
			overriteNextLabelToContinueOnFromBranch(ifStatement.onComplete);//needed else go strait to hasnext block instead of ret.adder(x)
			//overriteNextLabelToContinueOnFrom(ifStatement.onComplete);//needed else go strait to hasnext block instead of ret.adder(x)
		}
		else{
			ifStatement.onComplete = toGoOnContinue==null?getNextLabelToContinueFrom(): toGoOnContinue;
		}
		//System.err.println("afeter code: " + ifStatement.onComplete);
		ifStatement.setLabelAfterCode(ifStatement.onComplete);
		
		Label onFail = !hasElse && !hasElif ?ifStatement.onComplete:new Label();
		
		ifStatement.onFailIfCheckLabel = onFail;
		
		//System.err.println("if on complete: " + ifStatement.onComplete);
		//System.err.println("if on fail: " + ifStatement.onFailIfCheckLabel);
		
		ifStatement.ifblock.accept(this);
		
		if(hasElif || hasElse)
		{
			if(hasElif){
				ArrayList<ElifUnit> elifs = ifStatement.elifunits;
				int sz = elifs.size();
				for(int n = 0; n < sz; n++) {
					ElifUnit elif = elifs.get(n);
					boolean prev = permitTaggingOfImmediateUse;
					//permitTaggingOfImmediateUse=false;
					
					elif.accept(this);
					//permitTaggingOfImmediateUse = prev;
					boolean isLast = n == sz-1;
					
					elif.labelOnCacthEntry = onFail;
					
					if(isLast && !hasElse ){
						onFail = ifStatement.onComplete;
					}
					else{
						onFail = new Label();
					}
					elif.nextLabelOnCatchEntry = onFail;
				}
			}
			if(hasElse){
				boolean prev = permitTaggingOfImmediateUse;
				//permitTaggingOfImmediateUse=false;
				ifStatement.elseb.accept(this);
				//permitTaggingOfImmediateUse = prev;
				ifStatement.elseLabelOnEntry = onFail;
			}
		}
		
		if(ifStatement.getShouldBePresevedOnStackAndImmediatlyUsed()){
			labelNeedingConsumption.pop();
			labelNeedingConsumption.push(new Pair<Label, Label>(null,null));
		}
		
		return null;//super.visit(ifStatement);
	}
	
	
	
	
	private boolean permitTaggingOfImmediateUse = true;
	
	/*@Override
	public Object visit(WithBlock withBlock) {
		return super.visit(withBlock);//TODO when with impl later
	}
	*/
	private void allocateLabelsPriorToInsertedFinalCode(PreEntryCode retStmt){
		
		Stack<Pair<Label, Block>> finCodes = retStmt.getLinesToVisitOnEntry();
		if(!finCodes.isEmpty()){
			Label labelAfterInsertedCode =new Label();
			//the label to conitue on from is the next which is non null after this one unless you get to the end in which case revert to  labelAfterInsertedCode
			int fcSize = finCodes.size();
			for(int n =0; n < fcSize; n++){
				Pair<Label, Block> level = finCodes.get(n);
				if(level.getA() != null){
					
					Label nextLabel=labelAfterInsertedCode;
					int m=n+1;
					while(m<=fcSize-1){
						if(finCodes.get(m).getA() !=null){
							nextLabel=finCodes.get(m).getA();
							break;
						}
						m++;
					}
					
					labelNeedingConsumption.push(new Pair<Label, Label>(null, nextLabel));
					this.visit(level.getB());
					labelNeedingConsumption.pop();
				}
			}
			retStmt.setLabelToVisitJustBeforeReturnOpCode(labelAfterInsertedCode);
		}
		
	}
	
	@Override
	public Object visit(NotExpression note){
		note.labelAfterNot = overrideNextLabelCont==null?new Label():overrideNextLabelCont;
		
		return super.visit(note);
	}
	
	@Override
	public Object visit(ReturnStatement retStmt) {
		if(retStmt.isSynthetic){
			//((Node)retStmt.ret).ifReturnsExpectImmediateUse = true;
			tagLastThingInBlockWithImmediateUse(retStmt.ret);
		}
		
		allocateLabelsPriorToInsertedFinalCode(retStmt);
		
		/*Label afterAdded = retStmt.getLabelToVisitJustBeforeReturnOpCode();
		if(null != afterAdded){
			overrideNextLabelCont = afterAdded;
		}*/
		
		return super.visit(retStmt);
	}
	
	@Override
	public Object visit(BreakStatement breakStatement) {
		if(breakStatement.breaksOutOfTryCatchLevel>0){
			allocateLabelsPriorToInsertedFinalCode(breakStatement);
		}
		
		breakStatement.jumpTo = onBreak.peek();
		return null;
	}
	
	@Override
	public Object visit(ContinueStatement continueStatement) {
		if(continueStatement.breaksOutOfTryCatchLevel>0){
			allocateLabelsPriorToInsertedFinalCode(continueStatement);
		}
		
		continueStatement.jumpTo = this.startOfLoop.peek();//back to start
		return null;
	}
}