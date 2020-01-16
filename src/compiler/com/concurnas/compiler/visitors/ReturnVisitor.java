package com.concurnas.compiler.visitors;

import java.util.ArrayList;
import java.util.Stack;

import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.ast.AsyncBlock;
import com.concurnas.compiler.ast.AsyncBodyBlock;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.BreakStatement;
import com.concurnas.compiler.ast.CatchBlocks;
import com.concurnas.compiler.ast.ConstructorDef;
import com.concurnas.compiler.ast.ContinueStatement;
import com.concurnas.compiler.ast.DuffAssign;
import com.concurnas.compiler.ast.ElifUnit;
import com.concurnas.compiler.ast.ForBlock;
import com.concurnas.compiler.ast.ForBlockOld;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.IfStatement;
import com.concurnas.compiler.ast.IgnoreASTRepointForReturn;
import com.concurnas.compiler.ast.LambdaDef;
import com.concurnas.compiler.ast.Line;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.OnChange;
import com.concurnas.compiler.ast.PrimativeType;
import com.concurnas.compiler.ast.PrimativeTypeEnum;
import com.concurnas.compiler.ast.ReturnStatement;
import com.concurnas.compiler.ast.ThrowStatement;
import com.concurnas.compiler.ast.TryCatch;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.WhileBlock;
import com.concurnas.compiler.ast.WithBlock;
import com.concurnas.compiler.ast.interfaces.Expression;

/**
 * ensure returns takes place on definite branch statements: if elif else, try-catch
 * ensure that anon blocks et al. and while are considered valid places for return statements
 * 
 * deadcode analysis after retruns and after exception has definately been thrown
 * handle nested funcs and classes etc
 * also catch cases where there are break and continue statmeents, because after these u cannot have stuff!
 * but these dont bleed outside for and while blocks
 * 
 * also deals with no return, break or continue being permitted inside finally blocks
 * 
 * also track break out of try catch blocks
 * 
 * @author Jason
 *
 */
public class ReturnVisitor extends AbstractErrorRaiseVisitor {
	public ReturnVisitor(String fullPathFileName, boolean treatBreakContinueAsReturn) {
		super(fullPathFileName);
		this.treatBreakContinueAsReturn=treatBreakContinueAsReturn;
	}
	
	public ReturnVisitor(String fullPathFileName) {
		this(fullPathFileName, false);
	}

	private Stack<Integer> breakIsInTryCatch = new Stack<Integer>();//is a break inside a trycatch (i.e. going to break out of it?
	protected Stack<Boolean> isInAFinallySegment = new Stack<Boolean>();
	protected Stack<Boolean> isBreakContPermitted = new Stack<Boolean>();
	private Stack<Boolean> isInInfLoop = new Stack<Boolean>();
	
	private final boolean treatBreakContinueAsReturn;
	private Stack<Boolean> hasdefoReturned = new Stack<Boolean>();
	private Stack<Boolean> hasThrownException = new Stack<Boolean>();
	private Stack<Boolean> hasBroken = new Stack<Boolean>();
	private Stack<Boolean> hasContinue = new Stack<Boolean>();
	private Stack<Boolean> hasBrokenOrContinue = new Stack<Boolean>();
	private Stack<Boolean> hasEscaped = new Stack<Boolean>();//any of: return, except, break, continue
	private Stack<Boolean> hasBrokenTryCatch = new Stack<Boolean>();
	private Stack<Boolean> hasImplicitReturn = new Stack<Boolean>();
	
	
	private Stack<Boolean> defoBreaksExceptsOrReturnsOutOfTryCatch = new Stack<Boolean>();//e.g. by break/continue, return or exception being thrown 
	
	private boolean hadMadeRepoints = false;
	
	public boolean hadMadeRepoints(){
		return this.hadMadeRepoints;
	}
	
	public void doOperation(Block block){
		hadMadeRepoints=false;//reset
		this.visit(block);
	}
	
	private void enterHasDefo()
	{
		hasdefoReturned.push(false);
		hasThrownException.push(false);
		hasBroken.push(false);
		hasContinue.push(false);
		hasBrokenOrContinue.push(false);
		hasBrokenTryCatch.push(false);
		hasEscaped.push(false);
		isInInfLoop.push(false);
		//isInInfLoopFor.push(false);
		defoBreaksExceptsOrReturnsOutOfTryCatch.push(false);
		hasImplicitReturn.push(false);
	}
	
	private static class ReturnStates{
		private final boolean hasRet; 
		private final boolean hasExcep;
		private final boolean hasBr;
		private final boolean hasBrTryCatch; 
		private final boolean defoBreaksOutOfTryCatcha; 
		private final boolean hasCont;
		private final boolean hasEscapeda; 
		private final boolean infloop;
		private final boolean hasDefoCatchOrBroken;
		private final boolean hasImplicitReturn;
		
		public ReturnStates(final boolean hasRet, final boolean hasExcep, final boolean hasBr, final boolean hasBrTryCatch, final boolean defoBreaksOutOfTryCatcha, final boolean hasCont, final boolean hasEscapeda, final boolean infloop, final boolean hasDefoCatchOrBroken, final boolean hasImplicitReturna) {
			this.hasRet = hasRet;
			this.hasExcep = hasExcep;
			this.hasBr = hasBr;
			this.hasBrTryCatch = hasBrTryCatch;
			this.defoBreaksOutOfTryCatcha = defoBreaksOutOfTryCatcha;
			this.hasCont = hasCont;
			this.hasEscapeda = hasEscapeda;
			this.infloop = infloop;
			this.hasDefoCatchOrBroken = hasDefoCatchOrBroken;
			this.hasImplicitReturn = hasImplicitReturna;
		}
		
		public boolean getHasReturned() { return this.hasRet;	}
		public boolean getHasExceptioned() { return this.hasExcep;	}
		public boolean getHasBroken() { return this.hasBr;	}
		public boolean getHasBrokenTryCatch() { return this.hasBrTryCatch;	}
		public boolean getDefoBreaksOutOfTryCatch() { return this.defoBreaksOutOfTryCatcha;	}
		public boolean getHasContinue() { return this.hasCont;	}
		public boolean getHasEscaped() { return this.hasEscapeda;	}
		public boolean getHasInfLoop() { return this.infloop;	}
		public boolean getHasDefoCatchOrBroken() { return this.hasDefoCatchOrBroken;	}
		public boolean getHasImplicitReturn() { return this.hasImplicitReturn;	}
	}
	
	
	private ReturnStates exitHasDef()
	{
		boolean hasRet = hasdefoReturned.pop();
		boolean hasExcep = hasThrownException.pop();
		boolean hasBr = hasBroken.pop();
		boolean hasCont = hasContinue.pop();
		boolean hasBrTryCatch = hasBrokenTryCatch.pop();
		boolean defoBreaksOutOfTryCatcha = defoBreaksExceptsOrReturnsOutOfTryCatch.pop();
		boolean infloop = isInInfLoop.pop();// || isInInfLoopFor.pop();
		boolean hasDefoCatchOrBroken = hasBrokenOrContinue.pop();
		boolean hasImplicitReturna = hasImplicitReturn.pop();
		
		//isInInfLoopFor.push(false);
		//isInInfLoop.push(false);
		
		hasEscaped.pop();
		boolean hasEscapeda = hasRet || hasExcep || hasBr || hasCont;
		return new ReturnStates(hasRet, hasExcep, hasBr, hasBrTryCatch, defoBreaksOutOfTryCatcha, hasCont, hasEscapeda, infloop, hasDefoCatchOrBroken, hasImplicitReturna); 
	}
	
	private void topStackExcep(boolean setOnExit)
	{
		hasThrownException.pop();
		hasThrownException.push(setOnExit);
	}
	
	private void topStackReturn(boolean setOnExit)
	{
		//not needed as nothing can come after ret stmt anyway...
		hasdefoReturned.pop();
		hasdefoReturned.push(setOnExit);
	}
	
	private void topStackBreak(boolean setOnExit)
	{
		if(setOnExit){
			isInInfLoopFor.pop();//broker therefor not in inf loop anymore
			isInInfLoopFor.push(false);
		}
		//isInInfLoop.push(!(isInInfLoop.pop() && !setOnExit));
		hasBroken.pop();
		hasBroken.push(setOnExit);
		topStackBreakOrContinue(setOnExit);
		
	}
	
	private void topStackContinue(boolean setOnExit)
	{
		hasContinue.pop();
		hasContinue.push(setOnExit);
		topStackBreakOrContinue(setOnExit);
	}
	
	private void topStackBreakOrContinue(boolean setOnExit)
	{
		hasBrokenOrContinue.pop();
		hasBrokenOrContinue.push(setOnExit);
	}
	
	private void topStackEscaped(boolean setOnExit)
	{
		hasEscaped.pop();
		hasEscaped.push(setOnExit);
	}
	
	private void topStackInfLoop(boolean setOnExit){
		isInInfLoop.pop();
		isInInfLoop.push(setOnExit);
	}
	
	private void topStackBreakFromTryCatch(boolean setOnExit)
	{
		hasBrokenTryCatch.pop();
		hasBrokenTryCatch.push(setOnExit);
	}
	
	private void topStackBreakExceptsOrReturnOutOfTryCatch(boolean setOnExit)
	{
		defoBreaksExceptsOrReturnsOutOfTryCatch.pop();
		defoBreaksExceptsOrReturnsOutOfTryCatch.push(setOnExit);
	}
	
	private void topStackHasImplicitReturn(boolean setOnExit)
	{
		hasImplicitReturn.pop();
		hasImplicitReturn.push(setOnExit);
	}
	
	private ReturnStates handleFunctionBlock(Block block)
	{
		enterHasDefo();
		LineHolder lh = block.startItr();
		
		LineHolder last = null;
		while(lh != null)
		{
			lh.accept(this);
			last = lh;
			lh = block.getNext();
		}
		
		topStackHasImplicitReturn(last == null?false:last.l.getCanReturnAValue());
		
		block.hasDefoThrownException = (boolean)hasThrownException.peek();
		block.hasDefoBrokenOutOfTryCatch = (boolean)hasBrokenTryCatch.peek();
		block.defoEscapesByBreakExceptRetu = (boolean)defoBreaksExceptsOrReturnsOutOfTryCatch.peek();
		
		block.hasRetExcepOrBreaked = hasThrownException.peek() || hasdefoReturned.peek() || hasBroken.peek();
		block.hasDefoReturned = hasdefoReturned.peek();
		block.hasDefoBrokenOrContinued = hasBrokenOrContinue.peek();
		
		return exitHasDef();
	}

	private boolean isLastStatementReturn(Block body, boolean allowStuffwhichShouldReturn, boolean onlySynth)
	{
		LineHolder lastOne =  body.getLast();
		if(null != lastOne)
		{
			/*
			 * allowStuffwhichShouldReturn -> e.g fun something() { x} //where x is null in early compilation cycle - we expect this to be able to return somthing though
			 * so we should not add a returns statement there
			 */
			/*
			 
			//likely bug: where we are returning something froma  function
			//though that thing is null at initial time
			//go back and remove the syntetic return?
			 	 
			 */
			if(onlySynth && lastOne.l instanceof ReturnStatement) {
				ReturnStatement asret = (ReturnStatement)lastOne.l;
				return asret.isSynthetic && null != asret.ret;
			}else {
				return lastOne.l instanceof ReturnStatement;// || (allowStuffwhichShouldReturn && lastOne.l.getShouldBePresevedOnStack());
			}
		}
		return false;
	}
	
	
	private Expression extractLastThing(Block body){
		LineHolder lh = body.getLast();
		
		if(null == lh) {
			return null;
		}
		
		Line line = lh.l;
		if(line instanceof DuffAssign){
			return ((DuffAssign)line).e;
		}
		else if (line instanceof AssignNew) {
			AssignNew asAN = (AssignNew)line;
			if(asAN.astRedirect != null) {
				return (Expression)asAN.astRedirect;
			}
		}
		else if (line instanceof Expression){
			return (Expression)line;
		}
		
		return null;
	}
	
	public void checkFuncOrLambda(Type retType, Block body, int line, int col, boolean isLambda, Node orig)
	{
		if(null != body)
		{
			isInAFinallySegment.push(false);
			isBreakContPermitted.push(true);
			
			if(orig instanceof FuncDef) {
				super.visit((FuncDef)orig);
			}else if(orig instanceof LambdaDef) {
				super.visit((LambdaDef)orig);
			}
			
			ReturnStates reb = handleFunctionBlock(body);
			Boolean hasDefReturned = reb.getHasReturned();
			Boolean hasExcep = reb.getHasExceptioned();
			
			if(null != retType && !(retType instanceof PrimativeType && ((PrimativeType)retType).type == PrimativeTypeEnum.VOID && !((PrimativeType)retType).errored ) )
			{
				
				if(!hasDefReturned && !hasExcep)
				{//if it's defo exception then we can ignore this
					//no explicit return statement
					
					if(!body.isEmpty() && body.getShouldBePresevedOnStack() && !isLastStatementReturn(body, false, false)){
						Expression lastOne = extractLastThing(body);
						if(null == lastOne){
							this.raiseError(line, col, String.format("This %s must return a result of type %s", isLambda?"lambda":"method", retType));
							//body.postpend(new LineHolder(line, col, new DuffAssign(lastOne)));
						}
						else{
							hadMadeRepoints=true;
							line= lastOne.getLine();
							col = lastOne.getColumn();
							body.popLast();
							body.postpend(new LineHolder(line, col, new ReturnStatement(line, col, lastOne, true)));
						}
					}
					else{
						this.raiseError(line, col, String.format("This %s must return a result of type %s", isLambda?"lambda":"method", retType));
					}
				}
			}
			else if(!hasDefReturned && !hasExcep && !(orig instanceof ConstructorDef))
			{//there is no explicit return statement, so we add one so as to not blow up java bytecode generation
				if(!(retType instanceof PrimativeType && ((PrimativeType)retType).errored)){
					if(!isLastStatementReturn(body, true, false))
					{
						hadMadeRepoints=true;
						
						Expression lastOne = extractLastThing(body);
						if(null != lastOne && !lastOne.getCanBeOnItsOwnLine()){
							hadMadeRepoints=true;
							line= lastOne.getLine();
							col = lastOne.getColumn();
							body.popLast();
							body.postpend(new LineHolder(line, col, new ReturnStatement(line, col, lastOne, true)));
						}else {
							body.postpend(new LineHolder(line, col, new ReturnStatement(line, col, null, true)));
						}
					}
				}
			}else if(null != retType && TypeCheckUtils.isVoidPrimativePure(retType)) {//sync return xyz when invalid to do so, reverse damage above:
				if(isLastStatementReturn(body, true, true)) {
					
					ReturnStatement rn = (ReturnStatement)body.getLast().l;
					if(rn.ret.getCanBeOnItsOwnLine()) {
						body.postpend(new LineHolder(new DuffAssign(line, col, ( (ReturnStatement)body.popLast().l).ret)));
						hadMadeRepoints=true;
						body.postpend(new LineHolder(line, col, new ReturnStatement(line, col, null, true)));
					}
				}
			}
			
		
			
			//body.hasDefoReturned = hasDefReturned;
			isInAFinallySegment.pop();
			isBreakContPermitted.pop();
		}
	}
	
	
	@Override
	public Object visit(FuncDef funcDef) {
		if(!funcDef.ignore) {
			checkFuncOrLambda(funcDef.retType, funcDef.funcblock, funcDef.getLine(), funcDef.getColumn(), false, funcDef);
		}
		return null;
	}
	
	@Override
	public Object visit(LambdaDef lambdaDef) {
		if(!lambdaDef.ignore) {
			checkFuncOrLambda(lambdaDef.returnType, lambdaDef.body, lambdaDef.getLine(), lambdaDef.getColumn(), true, lambdaDef);
		}
		return null;
	}

	@Override
	public Object visit(Block block) {
		/*
		if(!block.isolated && !block.isEmpty()){
			lastStatementTouched=null;
		}*/
		
		if(block.isolated)
		{
			ReturnStates hazRetAndExcepa = handleFunctionBlock(block);
			topStackReturn(hazRetAndExcepa.getHasReturned());
			topStackExcep(hazRetAndExcepa.getHasExceptioned());
			block.hasDefoThrownException =  hazRetAndExcepa.getHasExceptioned();
			topStackBreak(hazRetAndExcepa.getHasBroken());
			topStackBreakFromTryCatch(hazRetAndExcepa.getHasBrokenTryCatch());
			topStackBreakExceptsOrReturnOutOfTryCatch(hazRetAndExcepa.getDefoBreaksOutOfTryCatch());
			topStackContinue(hazRetAndExcepa.getHasContinue());
			topStackEscaped(hazRetAndExcepa.getHasEscaped());
			topStackInfLoop(block.isAsyncBody?false:hazRetAndExcepa.getHasInfLoop());//an infinite loop inside an async block can never resolve to unreachable code outside of it!
			topStackBreakOrContinue(hazRetAndExcepa.getHasDefoCatchOrBroken());
			//topStackInfLoop(false);
		}
		else
		{
			if(!block.isEmpty()){
				lastStatementTouched=null;
			}
			
			LineHolder lh = block.startItr();
			LineHolder last = null;
			
			enterHasDefo();
			while(lh != null)
			{
				lh.accept(this);
				last = lh;
				lh = block.getNext();
			}
			
			topStackHasImplicitReturn(last == null?false:last.l.getCanReturnAValue());
			
			block.hasRetExcepOrBreaked = hasThrownException.peek() || hasdefoReturned.peek() || hasBroken.peek();
						
			block.hasDefoThrownException = (boolean)hasThrownException.pop();
			hasdefoReturned.pop();
			block.hasDefoBroken = (boolean)hasBroken.pop();
			hasContinue.pop();
			
			block.hasDefoBrokenOrContinued = hasBrokenOrContinue.pop();
			hasEscaped.pop();
			this.isInInfLoop.pop();
			block.hasDefoBrokenOutOfTryCatch = (boolean)hasBrokenTryCatch.pop();
		}
		return null;
	}
	
	
	@Override
	public Object visit(AsyncBlock asyncBlock) {
		handleFunctionBlock(asyncBlock.body);
		
		if(null != asyncBlock.executor)
		{
			asyncBlock.executor.accept(this);
		}
		return null;
		
	}
	
	
	@Override
	public Object visit(IfStatement ifStatement) {
		boolean defoRet = false;
		boolean defoExcept = false;
		boolean defoBreak = false;
		boolean defoCont = false;
		boolean hasEscape = false;
		boolean defoBreakFromTryCatch = false;
		boolean escapesTheTryCathc=false;
		boolean hasDefoInfLoop = false;
		boolean defoBreakOrCont = false;
		boolean implicitRetrun = false;
		boolean anyimplicitRetrun = false;
		boolean anyExplicitRet = false;
		
		if(ifStatement.elseb!=null) {
			ReturnStates hazRetAndExcep = handleFunctionBlock(ifStatement.ifblock);
			defoRet = hazRetAndExcep.getHasReturned() || hazRetAndExcep.getHasExceptioned();
			ifStatement.ifblock.defoReturns = hazRetAndExcep.getHasReturned(); 
			defoExcept = hazRetAndExcep.getHasExceptioned(); 
			defoBreak = hazRetAndExcep.getHasBroken(); 
			defoCont = hazRetAndExcep.getHasContinue(); 
			hasEscape = hazRetAndExcep.getHasEscaped(); 
			hasDefoInfLoop = hazRetAndExcep.getHasInfLoop();
			defoBreakOrCont = hazRetAndExcep.getHasDefoCatchOrBroken();
			
			defoBreakFromTryCatch = hazRetAndExcep.getHasBrokenTryCatch(); 
			escapesTheTryCathc = hazRetAndExcep.getHasReturned() || hazRetAndExcep.getHasExceptioned() || hazRetAndExcep.getHasBroken()|| hazRetAndExcep.getHasContinue();
			
			implicitRetrun = hazRetAndExcep.getHasImplicitReturn();
			anyimplicitRetrun = implicitRetrun;
			anyExplicitRet = hazRetAndExcep.getHasReturned();
			
			for(ElifUnit u : ifStatement.elifunits)
			{	
				ReturnStates hazRetAndExcepa = handleFunctionBlock(u.elifb);
				u.elifb.defoReturns = hazRetAndExcepa.getHasReturned();
				defoRet &= hazRetAndExcepa.getHasReturned() || hazRetAndExcepa.getHasExceptioned();//all must ret
				defoExcept &= hazRetAndExcepa.getHasExceptioned();//all must ret
				defoBreak &= hazRetAndExcepa.getHasBroken();//all must ret
				defoCont &= hazRetAndExcepa.getHasContinue(); 
				hasEscape &= hazRetAndExcepa.getHasEscaped(); 
				defoBreakFromTryCatch &= hazRetAndExcepa.getHasBrokenTryCatch(); 
				escapesTheTryCathc &= hazRetAndExcepa.getHasReturned() || hazRetAndExcepa.getHasExceptioned() || hazRetAndExcepa.getHasBroken()|| hazRetAndExcepa.getHasContinue();
				hasDefoInfLoop &= hazRetAndExcepa.getHasInfLoop(); 
				defoBreakOrCont &= hazRetAndExcepa.getHasBroken() || hazRetAndExcepa.getHasContinue();
				implicitRetrun &= hazRetAndExcepa.getHasImplicitReturn();
				anyimplicitRetrun |= hazRetAndExcepa.getHasImplicitReturn();
				anyExplicitRet |= hazRetAndExcepa.getHasReturned();
			}
			
			ReturnStates hazRetAndExcepa = handleFunctionBlock(ifStatement.elseb);
			ifStatement.elseb.defoReturns = hazRetAndExcepa.getHasReturned();
			defoRet &= hazRetAndExcepa.getHasReturned() || hazRetAndExcepa.getHasExceptioned();//all must ret
			defoExcept &= hazRetAndExcepa.getHasExceptioned();//all must ret
			defoBreak &= hazRetAndExcepa.getHasBroken();//all must ret
			defoCont &= hazRetAndExcepa.getHasContinue(); 
			hasEscape &= hazRetAndExcepa.getHasEscaped(); 
			defoBreakFromTryCatch &= hazRetAndExcepa.getHasBrokenTryCatch(); 
			escapesTheTryCathc &= hazRetAndExcepa.getHasReturned() || hazRetAndExcepa.getHasExceptioned() || hazRetAndExcepa.getHasBroken()|| hazRetAndExcepa.getHasContinue();
			hasDefoInfLoop &= hazRetAndExcepa.getHasInfLoop(); 
			defoBreakOrCont &= hazRetAndExcepa.getHasBroken() || hazRetAndExcepa.getHasContinue();
			
			implicitRetrun &= hazRetAndExcepa.getHasImplicitReturn();
			anyimplicitRetrun |= hazRetAndExcepa.getHasImplicitReturn();
			anyExplicitRet |= hazRetAndExcepa.getHasReturned();
			
			
			if(ifStatement.getShouldBePresevedOnStack()) {
				if(!defoRet && !implicitRetrun) {
					this.raiseError(ifStatement.getLine(), ifStatement.getColumn(), "if statement must return something");
				}
			}
			
			if(!implicitRetrun && anyimplicitRetrun && !anyExplicitRet) {
				ifStatement.setShouldBePresevedOnStack(false);
			}
			
		}
		else{//for completeness, other code relies upon defoRet
			ifStatement.ifblock.defoReturns = handleFunctionBlock(ifStatement.ifblock).getHasReturned(); 
			for(ElifUnit u : ifStatement.elifunits) {	
				u.elifb.defoReturns = handleFunctionBlock(u.elifb).getHasReturned();
			}
		}
		topStackReturn(defoRet);
		topStackExcep(defoExcept);
		topStackBreak(defoBreak);
		topStackContinue(defoCont);
		topStackBreakOrContinue(defoBreakOrCont);
		topStackEscaped(hasEscape);
		topStackBreakFromTryCatch(defoBreakFromTryCatch);
		topStackBreakExceptsOrReturnOutOfTryCatch(escapesTheTryCathc);
		topStackInfLoop(hasDefoInfLoop);
		topStackHasImplicitReturn(implicitRetrun);
		
		
		return null;
	}

	@Override
	public Object visit(TryCatch tryCatch) {
		if(tryCatch.astRepoint!=null){
			return tryCatch.astRepoint.accept(this);
		}
		
		//same model as if elif else
		boolean defoRet = false;
		boolean defoExcept = false;
		boolean defoBreak = false;
		boolean defoContinue = false;
		boolean defoBreakOrContinue = false;
		boolean hasEscape = false;
		boolean defoBreakTryCatch = false;
		boolean escapesTheTryCathc = false;
		boolean defoInfLoop = false;
		boolean implicitRetrun = false;
		boolean anyRet = false;
		boolean anyimplicitRetrun = false;
		boolean anyExplicitRet = false;
		
		if(!breakIsInTryCatch.isEmpty()){ breakIsInTryCatch.push(breakIsInTryCatch.pop()+1);};
		//breakIsInTryCatch.push(true);
		
		ReturnStates hazRetAndExcep = handleFunctionBlock(tryCatch.blockToTry);
		defoRet = hazRetAndExcep.getHasReturned() || hazRetAndExcep.getHasExceptioned();// || tryCatch.blockToTry.getShouldBePresevedOnStack();
		anyRet = defoRet;
		tryCatch.blockToTry.defoReturns = defoRet;
		defoExcept = hazRetAndExcep.getHasExceptioned(); 
		tryCatch.blockToTry.hasDefoThrownException = defoExcept;
		defoBreak = hazRetAndExcep.getHasBroken(); 
		defoContinue = hazRetAndExcep.getHasContinue(); 
		//hasEscape = hazRetAndExcep.getG(); 
		defoBreakTryCatch = hazRetAndExcep.getHasBrokenTryCatch(); 
		escapesTheTryCathc = hazRetAndExcep.getHasReturned() || hazRetAndExcep.getHasExceptioned() || hazRetAndExcep.getHasBroken()|| hazRetAndExcep.getHasContinue();
		hasEscape = hazRetAndExcep.getHasEscaped();
		defoInfLoop =  hazRetAndExcep.getHasInfLoop();
		defoBreakOrContinue = hazRetAndExcep.getHasDefoCatchOrBroken();
		
		implicitRetrun = hazRetAndExcep.getHasImplicitReturn();
		anyimplicitRetrun = implicitRetrun;
		anyExplicitRet = hazRetAndExcep.getHasReturned();
		
		for(CatchBlocks u : tryCatch.cbs)
		{	
			//ret, excep, break
			//hasRet, hasExcep, hasBr, hasBrTryCatch, defoBreaksOutOfTryCatcha, hasCont, hasEscapeda
			ReturnStates hazRetAndExcepa = handleFunctionBlock(u.catchBlock); /*ret, except, broken*/
			u.catchBlock.defoReturns = hazRetAndExcepa.getHasReturned();// || u.catchBlock.getShouldBePresevedOnStack();
			defoRet &= hazRetAndExcepa.getHasReturned() || hazRetAndExcepa.getHasExceptioned();//all must ret - but throwing a new exception is same as returning
			anyRet |= hazRetAndExcepa.getHasReturned();// || hazRetAndExcepa.getHasExceptioned();
			defoExcept &= hazRetAndExcepa.getHasExceptioned();//all must except
			u.catchBlock.hasDefoThrownException = hazRetAndExcepa.getHasExceptioned();
			if(tryCatch.blockToTry.hasDefoThrownException){
				defoBreak = hazRetAndExcepa.getHasBroken();//the thing has broken, thus we're 100% certain to come here
				defoContinue = hazRetAndExcepa.getHasContinue();
				escapesTheTryCathc = hazRetAndExcep.getHasReturned() || hazRetAndExcep.getHasExceptioned() || hazRetAndExcep.getHasBroken()|| hazRetAndExcepa.getHasContinue();
			}
			else{
				defoBreak &= hazRetAndExcepa.getHasBroken();//all must ret
				defoContinue &= hazRetAndExcepa.getHasContinue();
				escapesTheTryCathc &= hazRetAndExcep.getHasReturned() || hazRetAndExcep.getHasExceptioned() || hazRetAndExcep.getHasBroken()|| hazRetAndExcepa.getHasContinue(); 
			}
			
			defoBreakOrContinue &= defoContinue || defoBreak;
			
			defoBreakTryCatch &= hazRetAndExcepa.getHasBrokenTryCatch() || hazRetAndExcepa.getHasEscaped();//all must ret
			hasEscape &= hazRetAndExcepa.getHasEscaped();//all must escape

			implicitRetrun &= hazRetAndExcepa.getHasImplicitReturn();
			anyimplicitRetrun |= hazRetAndExcepa.getHasImplicitReturn();
			anyExplicitRet |= hazRetAndExcepa.getHasReturned();
		}
		
		if(tryCatch.finalBlock!=null)
		{
			isInAFinallySegment.push(true);
			isBreakContPermitted.push(false);
			
			ReturnStates hazRetAndExcepa = handleFunctionBlock(tryCatch.finalBlock);
			tryCatch.finalBlock.defoReturns = hazRetAndExcepa.getHasReturned();
			//defoRet = hazRetAndExcepa.getA();
			//defoRet |= hazRetAndExcepa.getA();
			//defoRet &= hazRetAndExcepa.getA();//all must ret
			//defoExcept &= hazRetAndExcepa.getB();//all must ret
			tryCatch.finalBlock.hasDefoThrownException = hazRetAndExcepa.getHasExceptioned();//hmm err here?
			defoExcept |= hazRetAndExcepa.getHasExceptioned();
			//defoBreak &= hazRetAndExcepa.getC();//all must ret
			//no break in fin
			
			defoInfLoop = hazRetAndExcepa.getHasInfLoop();//fin always done
			
			//defoBreakOrContinue &= defoContinue || defoBreak; //cannot be in fin
			
			//override
			escapesTheTryCathc = hazRetAndExcep.getHasReturned() || hazRetAndExcep.getHasExceptioned() || hazRetAndExcep.getHasBroken()|| hazRetAndExcepa.getHasContinue();
			
			isInAFinallySegment.pop();
			isBreakContPermitted.pop();
			
			//if(tryCatch.blockToTry.defoReturns){
				tryCatch.finalBlock.setShouldBePresevedOnStack(false);
			//}
			
			if(tryCatch.finalBlock.hasDefoThrownException){
				tryCatch.blockToTry.setShouldBePresevedOnStack(false);
				for(CatchBlocks u : tryCatch.cbs){
					u.catchBlock.setShouldBePresevedOnStack(false);
				}
				
			}
			
		}

		topStackReturn(defoRet);
		topStackExcep(defoExcept);
		topStackBreak(defoBreak);
		topStackContinue(defoContinue);
		topStackBreakOrContinue(defoBreakOrContinue);
		topStackEscaped(hasEscape);
		topStackBreakFromTryCatch(defoBreakTryCatch);
		topStackBreakExceptsOrReturnOutOfTryCatch(escapesTheTryCathc);
		topStackInfLoop(defoInfLoop);
		topStackHasImplicitReturn(implicitRetrun);
	
		tryCatch.hasDefoThrownException = defoExcept;
			
		if(!breakIsInTryCatch.isEmpty()){ breakIsInTryCatch.push(breakIsInTryCatch.pop()-1);};
		//breakIsInTryCatch.pop();
		
		if(tryCatch.getShouldBePresevedOnStack()) {
			if(!defoRet && !implicitRetrun) {
				if(!tryCatch.blockToTry.isEmpty()) {
					this.raiseError(tryCatch.getLine(), tryCatch.getColumn(), "try catch must return something");
				}
			}
		}
		
		//if(!implicitRetrun && anyimplicitRetrun && !anyExplicitRet) {
			//tryCatch.setShouldBePresevedOnStack(false);
		//}
		
		//tryCatch.setShouldBePresevedOnStack(false);
		//tryCatch.blockToTry.setShouldBePresevedOnStack(false);
		
		//tryCatch.setShouldBePresevedOnStack(tryCatch.blockToTry.getShouldBePresevedOnStack());
		
		return null;
	}
	
/*	@Override
	public Object visit(BarrierBlock barrierBlock) {
		ReturnStates hazRetAndExcepa = handleFunctionBlock(barrierBlock.b);
		barrierBlock.b.defoReturns = hazRetAndExcepa.getA();
		topStackReturn(hazRetAndExcepa.getA());
		topStackExcep(hazRetAndExcepa.getB());
		topStackBreak(hazRetAndExcepa.getC());
		topStackContinue(hazRetAndExcepa.getF());
		topStackBreakFromTryCatch(hazRetAndExcepa.getD());
		topStackEscaped(hazRetAndExcepa.getG());
		topStackBreakExceptsOrReturnOutOfTryCatch(hazRetAndExcepa.getE());
		topStackInfLoop(hazRetAndExcepa.getH());
		topStackBreakOrContinue(hazRetAndExcepa.getI());
		return null;
	}
	*/
	@Override
	public Object visit(WithBlock withBlock) {
		ReturnStates hazRetAndExcepa = handleFunctionBlock(withBlock.blk);
		withBlock.blk.defoReturns = hazRetAndExcepa.getHasReturned();
		topStackReturn(hazRetAndExcepa.getHasReturned());
		topStackExcep(hazRetAndExcepa.getHasExceptioned());
		topStackBreak(hazRetAndExcepa.getHasBroken());
		topStackContinue(hazRetAndExcepa.getHasContinue());
		topStackBreakFromTryCatch(hazRetAndExcepa.getHasBrokenTryCatch());
		topStackEscaped(hazRetAndExcepa.getHasEscaped());
		topStackBreakExceptsOrReturnOutOfTryCatch(hazRetAndExcepa.getDefoBreaksOutOfTryCatch());
		topStackInfLoop(hazRetAndExcepa.getHasInfLoop());
		topStackBreakOrContinue(hazRetAndExcepa.getHasDefoCatchOrBroken());
		topStackHasImplicitReturn(hazRetAndExcepa.getHasImplicitReturn());
		
		return null;
	}
	
	
	
	
	
	@Override
	public Object visit(ReturnStatement returnStatement) {
		if(returnStatement.ret !=null){
			returnStatement.ret.accept(this);
		}
		
		if(returnStatement.isAllowedHere)
		//if(null!=returnStatement.ret && returnStatement.isAllowedHere) <- might have to have explicit test to ensure that fun invoked in is void ret
		{//returnStatement.isAllowedHere catches cases where u define a return in like a class or somethign and so it's already erroreded, u dont need "after ret" error here!
			topStackReturn(true);
			topStackBreakExceptsOrReturnOutOfTryCatch(true);
			
			if(isInAFinallySegment.peek()){
				this.raiseError(returnStatement.getLine(), returnStatement.getColumn(), "Fianlly blocks cannot contain return statements");
			}
		}
		//returnStatement.ret.accept(this);
		
		return null;
	}
	
	@Override
	public Object visit(ThrowStatement throwStatement) {
		topStackExcep(true);
		topStackBreakExceptsOrReturnOutOfTryCatch(true);
		
		/*if(isInAFinallySegment.peek()){
			this.raiseError(throwStatement.getLine(), throwStatement.getColumn(), "Fianlly blocks cannot contain throw statements");
		}*/ //- ok
		
		return null;
	}
	
	@Override
	public Object visit(BreakStatement breakStatement) {
		if(breakStatement.getIsValid())//if break is put in random place ignore it!
		{
			if(!isBreakContPermitted.peek()){ this.raiseError(breakStatement.getLine(), breakStatement.getColumn(), "Fianlly blocks cannot contain break statements"); }
			topStackBreak(true);
			/*boolean breaksInTryCatch = breakIsInTryCatch.peek();
			if(breaksInTryCatch){
				breakStatement.breaksOutOfTryCatch = breaksInTryCatch;
				topStackBreakFromTryCatch(breaksInTryCatch);
				topStackBreakExceptsOrReturnOutOfTryCatch(breaksInTryCatch);
			}*/
			
			if(!breakIsInTryCatch.isEmpty()){//JPT: could this ever be empty?
				int level = breakIsInTryCatch.peek();
				if(level > 0){
					breakStatement.breaksOutOfTryCatchLevel = level;
					topStackBreakFromTryCatch(true);
					topStackBreakExceptsOrReturnOutOfTryCatch(true);
				}
			}
		}
		
		if(treatBreakContinueAsReturn){
			topStackReturn(true);
			topStackBreakExceptsOrReturnOutOfTryCatch(true);
		}
		
		return null;
	}
	
	@Override
	public Object visit(ContinueStatement continueStatement) {
		if(continueStatement.getIsValid())//if continue is put in random place ignore it!
		{
			if(!isBreakContPermitted.peek()){ this.raiseError(continueStatement.getLine(), continueStatement.getColumn(), "Fianlly blocks cannot contain continue statements"); }
			topStackContinue(true);
			
			if(!breakIsInTryCatch.isEmpty()){//JPT: could this ever be empty?
				int level = breakIsInTryCatch.peek();
				if(level > 0){
					continueStatement.breaksOutOfTryCatchLevel = level;
					topStackBreakFromTryCatch(true);
					topStackBreakExceptsOrReturnOutOfTryCatch(true);
				}
			}
		}
		//fix here, then set the broke, then convert all to block has ret, excep or breaked out
		if(!forContainsAContinue.isEmpty() && !forContainsAContinue.peek()){//only empty if continue is outside of a for or while loop
			forContainsAContinue.pop();
			forContainsAContinue.push(true);
		}
		
		if(treatBreakContinueAsReturn){
			topStackReturn(true);
			topStackBreakExceptsOrReturnOutOfTryCatch(true);
		}
		
		return null;
	}

	private Line lastStatementTouched = null;
	
	private boolean isSynthetic(Line line){
		if(line instanceof ReturnStatement){
			ReturnStatement asRet = (ReturnStatement)line;
			return asRet.isSynthetic && asRet.ret==null;
		}
		else if(line instanceof ContinueStatement){
			ContinueStatement asCont = (ContinueStatement)line;
			return asCont.isSynthetic;
		}
		return false;
	}
	
	@Override
	public Object visit(LineHolder lineHolder) {
		this.enterLine();
		
		if(!isSynthetic(lineHolder.l)){
			if(this.hasThrownException.peek())
			{
				this.raiseError(lineHolder.getLine(), lineHolder.getColumn(), "Unreachable code after exception thrown");
			}
			else if(this.hasdefoReturned.peek())
			{
				this.raiseError(lineHolder.getLine(), lineHolder.getColumn(), "Unreachable code after return statement");
			}
			else if(this.hasBroken.peek())
			{
				this.raiseError(lineHolder.getLine(), lineHolder.getColumn(), "Unreachable code after break");
			}
			else if(this.hasContinue.peek())
			{
				this.raiseError(lineHolder.getLine(), lineHolder.getColumn(), "Unreachable code after continue");
			}
			else if(this.hasEscaped.peek() || this.isInInfLoop.peek())
			{
				this.raiseError(lineHolder.getLine(), lineHolder.getColumn(), "Unreachable code");
			}
		}
		
		lastStatementTouched = lineHolder.l;
		lineHolder.l.accept(this);
		this.leaveLine();
		return null;
	}
	
	
/*	
	*//**
	 * Potential for infinite loop:
	 * while(true)
	 * while(true || false) 
	 * while(true && true) 
	 * while(!false)
	 * 
	 * 
	 * constant folder does this now :)
	 * 
	 * @author Jason
	 *
	 *//*
	private static class ExpressionAlwaysTrue extends AbstractVisitor {
		//JPT: TODO: expand to constant evaluations, e.g. 1==1, 1>0 etc
		//TODO: also expand this such that if(true){}//throws an error, cos if true u dont need the true...
		public Boolean eval(Expression e){
			return tryCast(e.accept(this));
		}
		
		private Boolean tryCast(Object o){
			return (o !=null && o instanceof Boolean)? (Boolean)o : null;
		}
		
		@Override
		public Object visit(AndExpression andExpression) {
			Boolean alwaysTrue = tryCast(andExpression.head.accept(this));
			if(alwaysTrue==null){return null;}
			
			for(Expression i : andExpression.things){
				Boolean thisOne = tryCast(i.accept(this));
				if(thisOne==null){return null;}
				alwaysTrue &= thisOne;
			}
			return alwaysTrue;
		}
		
		@Override
		public Object visit(OrExpression orExpression) {
			Boolean alwaysTrue = tryCast(orExpression.head.accept(this));
			if(alwaysTrue==null){return null;}
			
			for(Expression i : orExpression.things){
				Boolean thisOne = tryCast(i.accept(this));
				if(thisOne==null){return null;}
				alwaysTrue |= thisOne;
			}
			return alwaysTrue;
		}
		
		@Override
		public Object visit(RefBoolean refBoolean) {
			return refBoolean.b;
		}
		
		@Override
		public Object visit(NotExpression notExpression) {
			Boolean alwaysTrue =tryCast(notExpression.expr.accept(this));
			if(alwaysTrue==null){return null;}
			return !alwaysTrue;
		}
	}*/
	
	//private static final ExpressionAlwaysTrue isAlways = new ExpressionAlwaysTrue();
	
	@Override
	public Object visit(WhileBlock whileBlock) {
		

		Object fc = whileBlock.cond.getFoldedConstant();
		
		Boolean evalCondGoesToFixed =(Boolean) (fc!= null && fc instanceof Boolean?fc:false); 
				//isAlways.eval(whileBlock.cond);
		
		isInInfLoopFor.push( evalCondGoesToFixed!=null && evalCondGoesToFixed);//while(true) - is infinite
		
		breakIsInTryCatch.push(0);
		isBreakContPermitted.push(true);
		forContainsAContinue.push(false);
		whileBlock.cond.accept(this);
		this.hasImplicitReturn.push(false);
		this.visit(whileBlock.block);
		forContainsAContinue.pop();//ignore this here, of relevance to for loops
		isBreakContPermitted.pop();
		breakIsInTryCatch.pop();
		boolean containsInfLoop=isInInfLoopFor.peek();
		whileBlock.setCanReturnValue(this.hasImplicitReturn.pop());
		
		topStackInfLoop(containsInfLoop);
		
		if(containsInfLoop){//if it contains something which loops around forever
			whileBlock.skipGotoStart = true;
		}
		
		if(null != whileBlock.block && !whileBlock.block.isEmpty()){
			//tag this here so we dont have to GOTO's at the end of a while block
			//we cannot tag it on the block itself else the break will get tagged to its parent :(
			//dont need an extra GOTO [aka break] if it has one already...
			//LineHolder lh = whileBlock.blk.getLastLogical();
			//whileBlock.defoEndsInBreakStmt =lh.l instanceof BreakStatement || lh.l instanceof ContinueStatement;
			
			//whileBlock.defoEndsInBreakStmt = whileBlock.blk.hasDefoBroken;
			//whileBlock.defoEndsInGotoStmtAlready = lastStatementTouched instanceof BreakStatement || lastStatementTouched instanceof ContinueStatement;
			Node lastStatementTouched = whileBlock.block.getLast().l;
			if(lastStatementTouched instanceof TryCatch) {
				lastStatementTouched = this.lastStatementTouched;
			}
			whileBlock.defoEndsInGotoStmtAlready = whileBlock.block.hasDefoBrokenOrContinued || lastStatementTouched instanceof BreakStatement || lastStatementTouched instanceof ContinueStatement;
		}
		
		
		
		if(!containsInfLoop&& !whileBlock.defoEndsInGotoStmtAlready && !whileBlock.block.hasDefoBrokenOutOfTryCatch && !whileBlock.block.hasRetExcepOrBreaked && !whileBlock.block.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch() ){
			//add continue at end
			int line = whileBlock.getLine();
			int col = whileBlock.getColumn();
			ContinueStatement cs = new ContinueStatement(line,col);
			cs.isSynthetic = true;
			
			whileBlock.block.add(new LineHolder(line, col, cs));
			whileBlock.defoEndsInGotoStmtAlready =true;
			
			//hasDefoBrokenOutOfTryCatch properly
			
		}
		
		if(null != whileBlock.elseblock){
			this.visit(whileBlock.elseblock);
		}
		
		return null;
	}
	
	@Override
	public Object visit(OnChange onChange) {
		if(null != onChange.applyMethodFuncDef){

			//onChange.applyMethodFuncDef.funcblock.setShouldBePresevedOnStack(false);
			onChange.applyMethodFuncDef.accept(this);
		}
		
		return null;
	}
	
	@Override
	public Object visit(ForBlock forBlock) {
		isInInfLoopFor.push(false);
		breakIsInTryCatch.push(0);
		isBreakContPermitted.push(true);
		forContainsAContinue.push(false);
		this.hasImplicitReturn.push(false);
		
		if(null!= forBlock.localVarType){
			forBlock.localVarType.accept(this);
		}
		forBlock.expr.accept(this);
		forBlock.block.accept(this);
		
		boolean containsContinue = forContainsAContinue.pop();
		isBreakContPermitted.pop();
		breakIsInTryCatch.pop();
		boolean containsInfLoop=isInInfLoopFor.peek();
		forBlock.setCanReturnValue(this.hasImplicitReturn.pop());
		
		topStackInfLoop(containsInfLoop);
		
		if(containsInfLoop){//if it contains something which loops around forever
			forBlock.skipGotoStart = true;
		}
		
		if(null != forBlock.block && !forBlock.block.isEmpty()){
			//Node lastStatementTouched = forBlock.block.getLast().l;
			forBlock.defoEndsInGotoStmtAlready = lastStatementTouched instanceof BreakStatement || lastStatementTouched instanceof ContinueStatement;
			
			
			//Node lastStatementTouched = forBlock.block.getLast().l;
			//forBlock.defoEndsInGotoStmtAlready = forBlock.block.hasDefoBrokenOrContinued || lastStatementTouched instanceof BreakStatement || lastStatementTouched instanceof ContinueStatement;
		}
		
		if(containsContinue && forBlock.block.hasRetExcepOrBreaked){
			forBlock.block.hasRetExcepOrBreaked = false;
		}
		
		if(null != forBlock.elseblock){
			this.visit(forBlock.elseblock);
		}
		
		return null;
	}

	private Stack<Boolean> forContainsAContinue = new Stack<Boolean>();
	private Stack<Boolean> isInInfLoopFor = new Stack<Boolean>();
	
	@Override
	public Object visit(ForBlockOld forBlockOld) {
		
		Object fc = forBlockOld.check ==null?null:forBlockOld.check.getFoldedConstant();
		
		boolean forcheckresolvesToTrue = fc != null && fc instanceof Boolean?(Boolean)fc:false;
		
		isInInfLoopFor.push(null == forBlockOld.check || forcheckresolvesToTrue);//infinite loop potential: e.g. for (n=0;;n++) {} //cos no check!
		breakIsInTryCatch.push(0);
		isBreakContPermitted.push(true);
		forContainsAContinue.push(false);
		this.hasImplicitReturn.push(false);
		
		if(null != forBlockOld.assignExpr) 
		{
			forBlockOld.assignExpr.accept(this);
		}
		else if(null != forBlockOld.assignName)
		{
			if( null != forBlockOld.assigType) forBlockOld.assigType.accept(this);
			if( null != forBlockOld.assigFrom) forBlockOld.assigFrom.accept(this);
		}
		if(null != forBlockOld.check) forBlockOld.check.accept(this);
		if(null != forBlockOld.postExpr) forBlockOld.postExpr.accept(this);
		forBlockOld.block.accept(this);
		
		boolean containsContinue = forContainsAContinue.pop();
		isBreakContPermitted.pop();
		breakIsInTryCatch.pop();
		
		boolean containsInfLoop=isInInfLoopFor.peek();
		forBlockOld.setCanReturnValue(hasImplicitReturn.pop());
		
		topStackInfLoop(containsInfLoop);
		
		if(containsInfLoop){//if it contains something which loops around forever
			forBlockOld.skipGotoStart = true;
		}
		
		if(null != forBlockOld.block && !forBlockOld.block.isEmpty()){
			//Node lastStatementTouched = forBlockOld.block.getLast().l;
			forBlockOld.defoEndsInGotoStmtAlready = lastStatementTouched instanceof BreakStatement || lastStatementTouched instanceof ContinueStatement;
			//Node lastStatementTouched = forBlockOld.block.getLast().l;
			//forBlockOld.defoEndsInGotoStmtAlready = forBlockOld.block.hasDefoBrokenOrContinued || lastStatementTouched instanceof BreakStatement || lastStatementTouched instanceof ContinueStatement;
		}
		
		if(containsContinue && forBlockOld.block.hasRetExcepOrBreaked){
			forBlockOld.block.hasRetExcepOrBreaked = false;//to permit conttect processing of 
			/*
			 fun doings() String { 
				z=-99
				for(a =0; a < 10; a++) {if(a==0){continue} z=1; break ;	} //ok
				return "" + z
			} 
			 */
			//this contains a break, but it does not break out with certainty (uncertain case is where the continue is hit 'early')
		}
		
		if(null != forBlockOld.elseblock){
			this.visit(forBlockOld.elseblock);
		}
		
		return null;
	}
	
	
	private static class RVIgnoreReturnASTOverride extends ReturnVisitor implements IgnoreASTRepointForReturn{
		public RVIgnoreReturnASTOverride(String fullPathFileName, boolean treatBreakContinueAsReturn) {
			super(fullPathFileName, treatBreakContinueAsReturn);
		}
		
		public void start(){
			isInAFinallySegment.push(false);
			isBreakContPermitted.push(true);
		}
		
		public void end(){
			isInAFinallySegment.pop();
			isBreakContPermitted.pop();
		}
	}

	/*private RVIgnoreReturnASTOverride rVIgnoreInstace=null;
	private RVIgnoreReturnASTOverride getReturnVisitorInstanceIgnoreReturnASTOverride(){
		if(null == rVIgnoreInstace){
			rVIgnoreInstace = new RVIgnoreReturnASTOverride(super.fullPathFileName, treatBreakContinueAsReturn); 
		}
		return rVIgnoreInstace;
	}*/
	
	private boolean hasBlockInAsyncBodyBlockRetOrBreakEtc(Block thing){
		thing = (Block)thing.copy();
		RVIgnoreReturnASTOverride inst = new RVIgnoreReturnASTOverride(super.fullPathFileName, treatBreakContinueAsReturn);
		inst.start();
		try{
			inst.visit(thing);
		}finally{
			inst.end();
		}
		
		return testBlockForReturnForAsyncBodyBlock(thing);
	}
	
	private boolean testBlockForReturnForAsyncBodyBlock(Block thing){
		boolean validRet = TypeCheckUtils.isValidType(thing.getTaggedType());
		
		return  (thing.hasRetExcepOrBreaked && validRet) || validRet;
	}
	
	private boolean setAsyncBodyBlockPrePost(boolean shouldBePres, ArrayList<Block> items){
		boolean lastPreBlockReturns=false;
		int sblocks = items.size();
		if(sblocks > 0){
			Block item = null;
			for(int n=0; n < sblocks; n++){
				item = items.get(n);
				boolean returns = hasBlockInAsyncBodyBlockRetOrBreakEtc(item);
				item.setShouldBePresevedOnStack(shouldBePres && returns);//optionally set here
			}
			lastPreBlockReturns = testBlockForReturnForAsyncBodyBlock(item);//last one
		}
		return lastPreBlockReturns;
	}
	
	//here fix me
	@Override
	public Object visit(AsyncBodyBlock asyncBodyBlock) {
		super.visit(asyncBodyBlock);
		
		boolean shouldBePres = asyncBodyBlock.getShouldBePresevedOnStack();

		boolean lastPostBlockReturns = false;
		boolean lastPreBlockReturns = false;
		
		if(null != asyncBodyBlock.preBlocks){
			lastPreBlockReturns = setAsyncBodyBlockPrePost(shouldBePres, asyncBodyBlock.preBlocks);
/*			
			int sblocks = asyncBodyBlock.preBlocks.size();
			if(sblocks > 0){
				Block item = null;
				for(int n=0; n < sblocks; n++){
					item = asyncBodyBlock.preBlocks.get(n);
					boolean returns = hasBlockInAsyncBodyBlockRetOrBreakEtc(item);
					item.setShouldBePresevedOnStack(shouldBePres && returns);//optionally set here
				}
				lastPreBlockReturns = testBlockForReturnForAsyncBodyBlock(item);//last one
			}
*/		}

		if(null != asyncBodyBlock.postBlocks){
			lastPostBlockReturns = setAsyncBodyBlockPrePost(shouldBePres, asyncBodyBlock.postBlocks);
			/*int sblocks = asyncBodyBlock.postBlocks.size();
			if(sblocks > 0){
				for(int n=0; n < sblocks; n++){
					asyncBodyBlock.postBlocks.get(n).setShouldBePresevedOnStack(shouldBePres);
				}
				lastPostBlockReturns = hasBlockInAsyncBodyBlockRetOrBreakEtc(asyncBodyBlock.postBlocks.get(sblocks-1));
			}*/
		}
		
		for(LineHolder lh : asyncBodyBlock.mainBody.lines){
			if(lh.l instanceof OnChange){
				OnChange asOnchange = (OnChange)lh.l;
				if(shouldBePres){
					boolean onchangeret = hasBlockInAsyncBodyBlockRetOrBreakEtc(asOnchange.body);
					if(onchangeret){
						asOnchange.setShouldBePresevedOnStack(true);
					}else{//should return but block doesnt
						if(lastPostBlockReturns || lastPreBlockReturns){//ok doesnt matter
							asOnchange.setShouldBePresevedOnStack(onchangeret);
						}else{
							asOnchange.setShouldBePresevedOnStack(true);//needs to return, throw error
						}
					}
				}else{//no return
					asOnchange.setShouldBePresevedOnStack(false);
				}
			}
		}
		
		return null;
	}
	
}
