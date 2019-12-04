package com.concurnas.compiler.bytecode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.objectweb.asm.Label;

import com.concurnas.compiler.ast.AsyncBlock;
import com.concurnas.compiler.ast.AsyncBodyBlock;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.BreakStatement;
import com.concurnas.compiler.ast.CanEndInReturnOrException;
import com.concurnas.compiler.ast.CatchBlocks;
import com.concurnas.compiler.ast.ContinueStatement;
import com.concurnas.compiler.ast.ElifUnit;
import com.concurnas.compiler.ast.ForBlock;
import com.concurnas.compiler.ast.ForBlockOld;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.IfStatement;
import com.concurnas.compiler.ast.LambdaDef;
import com.concurnas.compiler.ast.Line;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.NotExpression;
import com.concurnas.compiler.ast.OnChange;
import com.concurnas.compiler.ast.PreEntryCode;
import com.concurnas.compiler.ast.ReturnStatement;
import com.concurnas.compiler.ast.ThrowStatement;
import com.concurnas.compiler.ast.TryCatch;
import com.concurnas.compiler.ast.WhileBlock;
import com.concurnas.compiler.utils.Sevenple;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.visitors.AbstractVisitor;
import com.concurnas.runtime.Pair;

/**
 * @author Jason hacker
 *
 */
public class TryCatchLabelTagVisitor {
	
	public static class StartEndHandle{
		Label startScope;
		Label endScope;
		Label catchHandler;
		String type;
		public StartEndHandle(Label startScope, Label endScope, Label nextHandler, String type)
		{
			this.startScope= startScope;
			this.endScope= endScope;
			this.catchHandler= nextHandler;
			this.type= type;
		}
		
		@Override
		public String toString(){
			return String.format("%s, %s, %s -> %s", startScope, endScope, catchHandler, type);
		}
		
		public StartEndHandle clone(Label nextHandler)
		{
			return new StartEndHandle( startScope,  endScope,  nextHandler,  type);
		}
		
	}
	
	public class TryCatchBlockPreallocatedLabelsEtc
	{
	}
	
	
	private static class LastLineChecker extends AbstractVisitor{
		private boolean lastThingRet = false;
		private boolean lastThingThrow = false;
		private Line lastLine=null;
		
		public Pair<Boolean, Boolean> isLastThingARetOrThrow(Block blk){
			lastThingRet=false;
			lastLine=null;
			isCompoundStatmentOwnLine = new Stack<Boolean>();
			super.visit(blk);
			return new Pair<Boolean, Boolean>(lastThingRet, lastThingThrow);
		}
		
		public Line getLastLine(Block blk){
			isLastThingARetOrThrow(blk);
			return lastLine;
		}
		
		private Stack<Boolean> isCompoundStatmentOwnLine = new Stack<Boolean>();
		
		private boolean isParentCmpdStatementItsOwnLine(){
			return isCompoundStatmentOwnLine.isEmpty() || isCompoundStatmentOwnLine.peek();
		}
		
		@Override
		public Object visit(OnChange onChange) {
			for(Node e: onChange.exprs){
				e.accept(this);
			}

			lastThingRet=false;
			lastThingThrow=false;
			
			if(null !=onChange.body){
				onChange.body.accept(this);
			}

			lastThingRet=false;
			lastThingThrow=false;
			
			return null;
		}
		
		@Override
		public Object visit(LineHolder lh) {
			Line l = lh.l;
			lastLine = l;
			lastThingRet=false;
			lastThingThrow=false;
			
			if(isParentCmpdStatementItsOwnLine()){
				//only do check when throw etc are on within compound statemnts that are on their own and not returning anything
				if(l instanceof ReturnStatement)
				{
					lastThingRet =true;
				}
				else if(l instanceof BreakStatement && ((BreakStatement)l).breaksOutOfTryCatchLevel>0  ){
					lastThingRet =true;
				}
				else if(l instanceof ContinueStatement && ((ContinueStatement)l).breaksOutOfTryCatchLevel>0  ){
					lastThingRet =true;
				}
				
				if(l instanceof ThrowStatement)
				{
					lastThingThrow = true;
				}
			}
			else{
				int g=9;
				g=9;
			}
			
			if(lh.l instanceof CanEndInReturnOrException){
				super.visit(lh);
			}
			
			
			//WONGOLA is thing direct or via another inditaction, e.g. a = {if a; else throw e}//last thing is not throw, but an assign to a
			//likewise, fff({if a; else throw e})//last thing is function invokation, so ret and throw being last is irrelvant, cos aint!
			//on new tc set director to false
			 if(l instanceof TryCatch){//TODO: or with statement
				TryCatch asTC = (TryCatch)l;
				if(asTC.hasFinal() && !asTC.finalBlock.defoReturns && asTC.blockToTry.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()){
					//if it has a final block, and it dont return then its going to throw an exception if the block to try defo does!
					//jvm spec is specific....
					lastThingThrow=true;
				}
			}
			
			
			return null;
		}
		
		
		@Override
		public Object visit(TryCatch tryCatch) {
			
			if(tryCatch.astRepoint!=null){
				return tryCatch.astRepoint.accept(this);
			}
			
			tryCatch.blockToTry.accept(this);
			for(CatchBlocks cat : tryCatch.cbs)
			{//reset on each exception block
				lastThingRet = false;
				lastThingThrow = false;
				cat.accept(this);
			}
			if(tryCatch.finalBlock != null && !tryCatch.finalBlock.isEmpty())
			{
				tryCatch.finalBlock.accept(this);
			}
			return null;
		}
		
	}
	
	public static class StartEndAllocator extends AbstractVisitor{
		private ArrayList<Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>> startEndRegions;
		private Label endLabel;
		private Label startLabel ;
		private int nestCount;
		private Line lastLogicallyVisitedLine;
		
		private static LastLineChecker lastLineChecker = new LastLineChecker();
		
		@Override public Object visit(LambdaDef lambdaDef) { return null; }
		@Override public Object visit(FuncDef funcDef) { return null; }
		//TODO:above may be a problem
		
		public Thruple<ArrayList<Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>>, Label, Label> process(Block blk, Label startLabela){
			startEndRegions = new ArrayList<Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>>();
			lastLogicallyVisitedLine = null;
			nestCount=0;
			endLabel=null;
			startLabel=startLabela;
						
			blk.accept(this);
			
			Line lastLine = lastLogicallyVisitedLine;
			//lastLine = lastLogicallyVisitedLine;
			
			if(!startEndRegions.isEmpty() && null != lastLine && null != startLabel){//if it doesnt end in a return function, then we need an extra region to capture this
				//if(!(lastLine instanceof ReturnStatement) && !(lastLine instanceof ThrowStatement)){
				if(!(lastLine instanceof ReturnStatement) ){
					endLabel = new Label();
					
					if(lastLine instanceof BreakStatement || lastLine instanceof ContinueStatement){
						lastLine.setLabelOnEntry(endLabel);
					}
					
					startEndRegions.add(new Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>(startLabel, endLabel, null, false, false, false, null));
				}
			}
			
			//coalese exception handlers resulting from exceptions being thrown
			
			ArrayList<Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>> coalesed  = new ArrayList<Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>>();
			
			Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer> lastOne=null;
			for(int n=0; n< startEndRegions.size(); n++){
				Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer> item = startEndRegions.get(n);
				if(null == lastOne){
					if(item.getE()){//it's an exception so coalesce later
						lastOne = item;
					}
					else{
						coalesed.add(item);
					}
				}
				else{
					//if(item.getE()){//coalesce
						lastOne = new Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>(lastOne.getA(), item.getB(), lastOne.getC(), lastOne.getD(), lastOne.getE(), lastOne.getF(), lastOne.getG());
					//}
					//else{
					//	coalesed.add(lastOne);
					//	lastOne=null;
					//}
				}
				
				if(n == startEndRegions.size()-1 && lastOne !=null ){//last so output it
					coalesed.add(lastOne);
				}
			}
			
			
			
			return new Thruple<ArrayList<Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean,Integer>>, Label, Label>(coalesed, startLabel, endLabel) ;
		}
		
		@Override
		public Object visit(Block block) {
			if(!block.isolated){
				nestCount++;
			}
			super.visit(block);
			
			if(!block.isolated){
				nestCount--;
			}
			return null;
		}
		//public ArrayList<Tuple<Label, ArrayList<Label>>> handlesInTryBlockWhenNested = new ArrayList<Tuple<Label, ArrayList<Label>>>();
		
		private void tagEndLabel(PreEntryCode retStmt, Label endLabel){
			retStmt.setLabelBeforeAction(endLabel);
		}
		
		private void logStartEndRegion(Label start, Label end, ArrayList<Label> finEndSegments, boolean retStmt, boolean excepStmt, boolean nestedSpecial, Integer relevanceLevel){
			startEndRegions.add(new Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>(start, end, finEndSegments, retStmt, excepStmt, nestedSpecial, relevanceLevel));
			endLabel = end;
		}
		
		private static class LastThingVisisted extends AbstractVisitor{
			private Line lastStatementTouched;
			
			public static final LastThingVisisted theOne = new LastThingVisisted();
			
			public static Line getLastThingVisisted(Node node){
				theOne.lastStatementTouched = null;
				node.accept(theOne);
				return theOne.lastStatementTouched;
			}
			
			@Override
			public Object visit(LineHolder lineHolder) {
				lastStatementTouched = lineHolder.l;
				lineHolder.l.accept(this);
				return null;
			}
			
		}
		
		private static class LabelToContinueOnFrom extends AbstractVisitor{
			public Label lastOne = null;
			
			private static final LabelToContinueOnFrom theInstance = new LabelToContinueOnFrom();
			
			public static Label getLastOne(Node rhs){
				theInstance.lastOne = null;
				rhs.accept(theInstance);
				if(null == theInstance.lastOne){
					theInstance.lastOne = new Label();
				}
				
				return theInstance.lastOne;
			}
			
			@Override
			public Object visit(IfStatement ifStatement) {
				lastOne = ifStatement.getLabelAfterCode();
				return super.visit(ifStatement);
			}
			
		/*	@Override
			public Object visit(IfExpr ifExpr) {
				lastOne = ifExpr.getLabelAfterCode();
				return super.visit(ifExpr);
				
			}*/
			
			@Override
			public Object visit(ForBlock ifStatement) {
				lastOne = ifStatement.getLabelAfterCode();
				return super.visit(ifStatement);
			}
			
			@Override
			public Object visit(ForBlockOld ifStatement) {
				lastOne = ifStatement.getLabelAfterCode();
				return super.visit(ifStatement);
			}
			
			private Label extractLabelIfEndsInBreakContinue(Node xxx){
				Line got = LastThingVisisted.getLastThingVisisted(xxx);
				
				if(got instanceof BreakStatement){
					return ((BreakStatement)got).jumpTo;
				}
				
				if(got instanceof ContinueStatement){
					return ((ContinueStatement)got).jumpTo;
				}
				
				return null;
			}
			
			@Override
			public Object visit(WhileBlock ifStatement) {
				Label fromLastContBreak = extractLabelIfEndsInBreakContinue(ifStatement.block);
				lastOne=null!=fromLastContBreak?fromLastContBreak:ifStatement.getLabelAfterCode();
				return super.visit(ifStatement);
			}
			
			@Override
			public Object visit(NotExpression ifStatement) {
				lastOne = ifStatement.labelAfterNot;
				return super.visit(ifStatement);
			}
			
		}
		
		
		@Override
		public Object visit(ReturnStatement retStmt) {
			Label endLabel = retStmt.ret!=null?LabelToContinueOnFrom.getLastOne((Node)retStmt.ret):new Label();
			if(nestCount > 0){//not for top level
				tagEndLabel(retStmt, endLabel);
				//direct to normal endLabel if this has no final stuff attached
				//build stack of entry labels
				logStartEndRegion(startLabel, endLabel, retStmt.extractInsertedFinSegmentsList(), true, false, false, null);//last null since has revance right up to func root
				super.visit(retStmt);
				startLabel=null;
			}
			else{
				tagEndLabel(retStmt, endLabel);
				super.visit(retStmt);
			}
			
			return true;
		}
		
		@Override
		public Object visit(ContinueStatement continueStatement) {
			if(continueStatement.breaksOutOfTryCatchLevel > 0){
				Label endLabel = null!=continueStatement.returns?LabelToContinueOnFrom.getLastOne((Node)continueStatement.returns):new Label();
				tagEndLabel(continueStatement, endLabel);
				logStartEndRegion(startLabel, endLabel, continueStatement.extractInsertedFinSegmentsList(), false, false, true, continueStatement.breaksOutOfTryCatchLevel);
				startLabel=null;
			}
			
			//no supervisit needed doesnt do anything
			return null;
		}
		
		@Override
		public Object visit(BreakStatement breakStatement) {
			if(breakStatement.breaksOutOfTryCatchLevel > 0){
				Label endLabel = null!=breakStatement.returns?LabelToContinueOnFrom.getLastOne((Node)breakStatement.returns):new Label();
				tagEndLabel(breakStatement, endLabel);
				logStartEndRegion(startLabel, endLabel, breakStatement.extractInsertedFinSegmentsList(), false, false, true, breakStatement.breaksOutOfTryCatchLevel);
				startLabel=null;
			}
			//no supervisit needed doesnt do anything
			return null;
		}
		
		
		@Override
		public Object visit(ThrowStatement throwStatement) {
			Label endLabel = new Label();
			logStartEndRegion(startLabel, endLabel, null, false, true, false, null);//last null since has revance right up to func root
			
			super.visit(throwStatement);
			
			return null;
		}
		
		
		@Override
		public Object visit(LineHolder lh) {
			lastLogicallyVisitedLine = lh.l;
			tagStartLabel(lastLogicallyVisitedLine);
			super.visit(lh);
			return null;
		}
		
		private void tagStartLabel(Node l){
			if(null == startLabel){
				Label onentry = l.getLabelOnEntry();
				if(null == onentry){
					onentry = new Label();
					l.setLabelOnEntry(onentry);
				}
				startLabel = onentry;
			}
		}
		
		@Override
		public Object visit(IfStatement ifStatement) {//special handling for this case only
			ifStatement.iftest.accept(this);
			ifStatement.ifblock.accept(this);
			
			int elifUnitsCnt = ifStatement.elifunits.size();
			for(int n =0; n < elifUnitsCnt; n++){
				//check for allocation before consumption of next...
				ElifUnit u = ifStatement.elifunits.get(n);
				
				if(null == startLabel){
					startLabel = u.labelOnCacthEntry;
				}
				
				//tagStartLabel((Node)u.eliftest);
				u.accept(this);
			}
			
			boolean hasElse = ifStatement.elseb !=null && !ifStatement.elseb.isEmpty();
			
			if(hasElse){
				//check for allocation before consumption of next...
				if(null == startLabel){
					startLabel = ifStatement.elseLabelOnEntry;
				}
				
				ifStatement.elseb.accept(this);
			}
			
			return null;
		}
		
		@Override
		public Object visit(AsyncBlock asyncBlock) {
			return null;//ignore try's inside  here, as contents is packaged off to own lambda
		}
		
		@Override
		public Object visit(TryCatch tryCatch) {
			//this will have nested regions allocated previously
			
			if(tryCatch.astRepoint!=null){
				return tryCatch.astRepoint.accept(this);
			}
			
			boolean isFirst = true;
			for(Thruple<Label, ArrayList<Label>, Integer> se : tryCatch.nestedStartEndContContainer.startEnd){
				Integer relevance = se.getC();
				if(relevance == null || relevance-1 > 0)
				{
					Label begin = se.getA();
					if(isFirst){
						isFirst=false;
						if(null != this.startLabel){//sew in the first instance by extending the start label from before to incude this...
							begin=this.startLabel;
						}
					}
					 ArrayList<Label> cutOffFirst = new ArrayList<Label>(se.getB().subList(1, se.getB().size()));
					 
					 Label nestedEnd = null;
					 for(Label candi : cutOffFirst){
						 if(candi!=null){
							 nestedEnd = candi;
							 break;
						 }
					 }
					
					 if(relevance != null) { 
						 relevance-=1;
					 }
					 
					logStartEndRegion(begin, nestedEnd, cutOffFirst, false, false, true, relevance);//caluclate depth and select correct one
				}

			}
			
			this.startLabel = tryCatch.nestedStartEndContContainer.cont;
			
			//ensure that last visited line is recorded...
			if(tryCatch.blockToTry != null && !tryCatch.blockToTry.isEmpty()){
				lastLogicallyVisitedLine = tryCatch.blockToTry.getLastLogical().l;
			}
			for(CatchBlocks cat : tryCatch.cbs) {
				if(cat.catchBlock != null && !cat.catchBlock.isEmpty()){
					//if(cat.catchBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()){
					if(cat.catchBlock.hasDefoBrokenOutOfTryCatch || cat.catchBlock.defoReturns ){
						lastLogicallyVisitedLine = new ReturnStatement(1, 2);
					}
					else{ lastLogicallyVisitedLine = cat.catchBlock.getLastLogical().l; }
				}
			}
			if(tryCatch.finalBlock != null && !tryCatch.finalBlock.isEmpty()){
				if(tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()){
					//fake me up scotty, so as to trick the nest caller into thinking a ret was last visted, and not a break or whatever
					lastLogicallyVisitedLine = new ReturnStatement(1, 2);
				}
				else{
					lastLogicallyVisitedLine = tryCatch.finalBlock.getLastLogical().l;
				}
			}
			else if(tryCatch.cbs.isEmpty()){
				//in case someone does try{ ... } finally{} //empty finally!, sneaky basters
				//this is an incredible hack!
				//this ensures that we capture the final, final region if there is one defined
				lastLogicallyVisitedLine = this.startLabel == null? new ReturnStatement(1, 2): tryCatch;//placeholder
			}
			
			return null;
		}
	}
	
	
	public static class CatchBlockAllocator extends  AbstractVisitor{
		
		private ArrayList<StartEndHandle> excpetionHandlers = new ArrayList<StartEndHandle>();
		private StartEndAllocator allocator = new StartEndAllocator();
		
		private LastLineChecker lastLineChecker = new LastLineChecker();
		
		public ArrayList<StartEndHandle> process(Block blk){
			excpetionHandlers = new ArrayList<StartEndHandle>();
						
			blk.accept(this);
			
			/*System.err.println("Exception Handlers: ");
			for(StartEndHandle seh: excpetionHandlers){
				System.err.println(":: " + seh);
			}*/
			
			return excpetionHandlers;
		}
		
		@Override
		public Object visit(AsyncBodyBlock asyncBodyBlock) {
			//FIX HERE
			
			if(null != asyncBodyBlock.applyMethodFuncDef){
				asyncBodyBlock.applyMethodFuncDef.accept(this);
				asyncBodyBlock.cleanUpMethodFuncDef.accept(this);
				asyncBodyBlock.initMethodNameFuncDef.accept(this);
			}
			
			return null;
		}
		
		@Override public Object visit(LambdaDef lambdaDef) { return null; }
		@Override public Object visit(FuncDef funcDef) { return null; }
		@Override public Object visit(AsyncBlock asyncBlock) { return null; }
		
		public static class NestedStartEndContContainer{
			public ArrayList<Thruple<Label, ArrayList<Label>, Integer>> startEnd = new ArrayList<Thruple<Label, ArrayList<Label>, Integer>>();
			
			public Label cont = null;
			
			private Label curStart = null;
			private ArrayList<Label> curEnd = null;
			
			public void addStart(Label start){
				if(null == curStart){
					curStart = start;
				}
			}
			
			public void addEnd(ArrayList<Label> end, Integer relevancy){
				curEnd=end;
				
				if(curStart != null && curEnd != null){
					startEnd.add(new Thruple<Label, ArrayList<Label>, Integer>(curStart, curEnd, relevancy) );
					curStart=null;
					curEnd=null;
				}
			}
			
			public void fin(){
				cont = curStart;
			}
		}
		
		@Override
		public Object visit(TryCatch tryCatch) {
			
			super.visit(tryCatch);
			
			if(tryCatch.astRepoint!=null){
				return tryCatch.astRepoint.accept(this);
			}
			
			NestedStartEndContContainer nestedStartEndContContainer = new NestedStartEndContContainer();
			tryCatch.nestedStartEndContContainer = nestedStartEndContContainer;
			
			
			boolean lastThingInBlockIsARet = false;
			boolean lastTHingInBlockExcep = false;
			boolean hasFinal = tryCatch.hasFinal();
			boolean finBlockDefRet =  hasFinal && tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch();

			ArrayList<Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>> tcstartEndRegions=null;
			Label startLabel=null;
			Label endLabel=null;
			
			if(!tryCatch.blockToTry.isEmpty()){
				
				boolean defoExxceptioned = tryCatch.blockToTry.hasDefoThrownException;
				
				if((!hasFinal ||(hasFinal && !tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch())) && !defoExxceptioned){//if the fin block doesnt retun then we just have one massiv region
					//tryCatch.blockToTry.accept(this);
					Thruple<ArrayList<Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>>, Label, Label> gear = allocator.process(tryCatch.blockToTry, null);
					tcstartEndRegions = gear.getA();
					startLabel = gear.getB();
					endLabel = gear.getC();
				}
				else{
					tcstartEndRegions=new ArrayList<Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>>();
				}
				
				Pair<Boolean, Boolean> throwExcep = lastLineChecker.isLastThingARetOrThrow(tryCatch.blockToTry);
				
				lastThingInBlockIsARet = throwExcep.getA();
				lastTHingInBlockExcep = throwExcep.getB();

				if(tcstartEndRegions.isEmpty()){//ensure always at least one
					Line l = tryCatch.blockToTry.getFirstLogical().l;
					startLabel = l.getLabelOnEntry();
					if(null == startLabel){ startLabel = new Label(); l.setLabelOnEntry(startLabel); } 
					
					//endLabel = tryCatch.endOfTryBlock;//new Label();
					endLabel = new Label();
					
					tcstartEndRegions.add(new Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>(startLabel, endLabel, null, false, defoExxceptioned || lastTHingInBlockExcep, false, null));
				}
				
				
				if(!tcstartEndRegions.get(tcstartEndRegions.size()-1).getD() && !lastThingInBlockIsARet && !lastTHingInBlockExcep){
					//if last block region is non returning, then add a goto after visiting the below to the final code at the end
					tryCatch.LabelPreGOTOOnBlockNonDefoRet = endLabel;
				}
				else{
					tryCatch.LabelPreGOTOOnBlockNonDefoRet = null;
				}
				
			}
			
			int nonSpecialCnt = 0;
			for(Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer> xxx : tcstartEndRegions){
				nestedStartEndContContainer.addStart(xxx.getA());
				if(null != xxx.getC()){
					//nestedStartEndContContainer.addEnd(hasFinal?xxx.getC():xxx.getB());//only if this thing adds stuff via fin block
					nestedStartEndContContainer.addEnd(xxx.getC(), xxx.getG());//only if this thing adds stuff via fin block
				}
				
				if(!xxx.getF()){ nonSpecialCnt++; }
			}
			
			boolean justOneBranchInBlock = nonSpecialCnt == 1;
			
			//catch block
			for(int c=0; c < tryCatch.cbs.size(); c++){
				boolean firstCatch = c==0;
				boolean isLast = c==tryCatch.cbs.size()-1;
				CatchBlocks cbk = tryCatch.cbs.get(c);
				
				for(int ser=0; ser < tcstartEndRegions.size(); ser++){
					boolean lastSer = ser==tcstartEndRegions.size()-1;//is first non special one (hence -specialCnt as this processes bot special and non special)
					Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer> xxx = tcstartEndRegions.get(ser);//JPT: use ret info here?
					
					boolean isSpecial =xxx.getF();
					
					if(!isSpecial){//ignore this logic for special added extras
						if(firstCatch && lastSer && (lastThingInBlockIsARet||lastTHingInBlockExcep) && (hasFinal && finBlockDefRet) && justOneBranchInBlock){
							//if first and the block to ret last exectued a ret, then overrite the catchblockentry label, combine with end region
							xxx = new Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>(xxx.getA(), cbk.entryLabel, xxx.getC(), xxx.getD(), xxx.getE(), xxx.getF(), xxx.getG());
							tcstartEndRegions.set(ser, xxx);
						}
						else if(firstCatch && lastSer && lastTHingInBlockExcep &&  !hasFinal && justOneBranchInBlock){
							//if it has no finally, but is an exception last visited, and just one, then we enter here
							xxx = new Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>(xxx.getA(), cbk.entryLabel, xxx.getC(), xxx.getD(), xxx.getE(), xxx.getF(), xxx.getG());
							tcstartEndRegions.set(ser, xxx);
						}
						else if(firstCatch && lastSer && lastTHingInBlockExcep){
							//if ends in a defo exception being thrown
							xxx = new Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>(xxx.getA(), cbk.entryLabel, xxx.getC(), xxx.getD(), xxx.getE(), xxx.getF(), xxx.getG());
							tcstartEndRegions.set(ser, xxx);
						}
					}
					
					Label start   = xxx.getA();
					Label end     = xxx.getB();
					Label handle  = cbk.entryLabel;
					
					nestedStartEndContContainer.addStart(handle);
					for(com.concurnas.compiler.ast.Type ca : cbk.caughtTypes){
						excpetionHandlers.add(new StartEndHandle(start, end, handle, ca.getCheckCastType()));
					}
					
				}
				
				if(hasFinal){
					//now add the nested segments captured by the catch block's attached final block
					boolean hasFinalAndCatchNotReturn = hasFinal && !cbk.catchBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch();
					if(!hasFinal && !isLast  && !cbk.catchBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch() ){
					}
					else if(hasFinalAndCatchNotReturn && !isLast){//skip for last
						if(!tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch())
						{
							addNestedRegionsForFinBlock(cbk.attachedFinalBlock, nestedStartEndContContainer);
						}
					}
					else if(isLast && hasFinal && !tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch() && !cbk.catchBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()){
						addNestedRegionsForFinBlock(cbk.attachedFinalBlock, nestedStartEndContContainer);
					}
				}
			}
			
			if(hasFinal)
			{
				tryCatch.finalHandler=new Label();
				
				tryCatch.endOfCatchToAttachedFinal = new HashMap<Label, Label>();

				Label thstart = startLabel;
				boolean finBlockDefoExcepOrRet = tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch();
				
				if(!tryCatch.cbs.isEmpty()){//haz catches
					
					int finBlocksAdded = 0;
					if(!finBlockDefRet ){//thus the first fin block doesnt carry on beyond the block to try into the first catch block
						for(Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer> se : tcstartEndRegions){
							//change me to thruple, such that the first case is captured - second is a non defo ret
							if(se.getD() || se.getF()){
								excpetionHandlers.add(new StartEndHandle(se.getA(), se.getB(), tryCatch.finalHandler, null));
								finBlocksAdded++;
							}
						}
					}
					
					
					boolean catchDefRet = false;
					
					for(int c=0; c < tryCatch.cbs.size(); c++){
						boolean isLast = c==tryCatch.cbs.size()-1;
						boolean firstCatch = c==0;
						CatchBlocks cbk = tryCatch.cbs.get(c);
						
						catchDefRet = cbk.catchBlock.defoEscapesByBreakExceptRetu;
						
						Pair<Boolean, Boolean> lastThingRetExcep = lastLineChecker.isLastThingARetOrThrow(cbk.catchBlock);
						boolean lastThingInCatchRet = lastThingRetExcep.getA();
						cbk.endsInException = lastThingRetExcep.getB();
						
						ArrayList<Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>> catchBlockFinRegions;
						
						if(!hasFinal ||(hasFinal && !tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()) || (cbk.catchBlock.hasDefoThrownException||cbk.endsInException)){//if the fin block doesnt retun then we just have one massiv region
							Thruple<ArrayList<Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>>, Label, Label> gear = allocator.process(cbk.catchBlock, cbk.entryLabel);
							catchBlockFinRegions = gear.getA();
						}
						else{
							catchBlockFinRegions=new ArrayList<Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>>();
						}
						
						if(catchBlockFinRegions.isEmpty()){
							if(!cbk.catchBlock.isEmpty() && cbk.catchBlock.getLastLogical().l instanceof ReturnStatement){
								ReturnStatement retStmt = (ReturnStatement)cbk.catchBlock.getLastLogical().l;
								Label endLabelforcatch = new Label();
								retStmt.setLabelBeforeAction(endLabelforcatch);
								catchBlockFinRegions.add(new Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>(cbk.entryLabel, endLabelforcatch, retStmt.extractInsertedFinSegmentsList(), true, false, true, null));
							}
							else{
								//TODO' check if it's a break statement here to set the correct relevancy?
								catchBlockFinRegions.add(new Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>(cbk.entryLabel, cbk.finLabel, null, false, false, false, null));
							}
						}
						
						for(int m = 0; m < catchBlockFinRegions.size(); m++ ){
							Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer> cbkStartEnd = catchBlockFinRegions.get(m);
							boolean isFirstCse = m==0;
							boolean isLastCse = m==catchBlockFinRegions.size()-1;
							
							boolean defoExcep = cbk.catchBlock.hasDefoThrownException || (cbkStartEnd.getE() && catchBlockFinRegions.size()>1) ;
							
							if(isFirstCse && firstCatch){
								thstart = cbk.entryLabel;
							}
							else if(null == thstart){
								thstart = cbkStartEnd.getA();
							}
							//last one, use the final block region starter, as long as the thing doesnt return something
							Label endCBLabel = (isLastCse && !catchDefRet )?cbk.finLabel:cbkStartEnd.getB();
							
							boolean endsWithExceptionAndFinBlockReturns = cbk.endsInException && finBlockDefRet; 
							
							if(isLast || ( !defoExcep && !endsWithExceptionAndFinBlockReturns &&(   !catchDefRet || !finBlockDefRet) ) ){//we need to encompas this (if it dont ret then the fin block is jumped to after
								//but we always output to encompass the final block
								
								//next line pretty much captures all the cases where you need to merge the end and final handler together, complex and concerning the catch block endings as well as fin block endigns....
								if(isLast && isLastCse && ( (!finBlockDefRet && catchDefRet) ||  finBlockDefRet || ( defoExcep && !finBlockDefRet  ) || cbk.endsInException   ) ){
									//ugly logics
									if((lastThingInCatchRet && finBlockDefoExcepOrRet)  || (defoExcep|| endsWithExceptionAndFinBlockReturns )){//special case, we combine the final region with the start of the final block
										cbk.finLabel = tryCatch.finalHandler;
										endCBLabel=cbk.finLabel;
									}
								}

								tryCatch.endOfCatchToAttachedFinal.put(cbk.entryLabel, endCBLabel);
								tryCatch.finalEnd =  endCBLabel; 
								//last one last chance to output and none output so far, include whole region - start is startLabel
								excpetionHandlers.add(new StartEndHandle( ( finBlocksAdded==0)?startLabel:thstart, endCBLabel, tryCatch.finalHandler, null));
								finBlocksAdded++;
								
								thstart=null;
							}
							
							cbk.attachedkFinalBlockEntryLabel = endCBLabel;//continue on from finally, when the fin block does not return  and should be placed at the end of the catch block code
							
							if(!finBlockDefoExcepOrRet){//skip over if defo ret in fin block
								nestedStartEndContContainer.addStart(cbk.entryLabel);
								if(null != cbkStartEnd.getC()){
									//nestedStartEndContContainer.addEnd(hasFinal?cbkStartEnd.getC():cbkStartEnd.getB());//only if this thing adds stuff via fin block
									nestedStartEndContContainer.addEnd(cbkStartEnd.getC(), cbkStartEnd.getG());//only if this thing adds stuff via fin block
								}
							}
						}
					}
				}
				else{//no catch block
					//just process last entry - this covers all the cases
					
					//oh! excep the nested cases, they need to be processed as well
					
					if(!finBlockDefRet ){//thus the first fin block doesnt carry on beyond the block to try into the first catch block
						for(Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer> se : tcstartEndRegions.subList(0, tcstartEndRegions.size()-1)){
							if(se.getD() || se.getF()){
								excpetionHandlers.add(new StartEndHandle(se.getA(), se.getB(), tryCatch.finalHandler, null));
							}
						}
					}
					
					
					Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer> handler = tcstartEndRegions.get(tcstartEndRegions.size()-1);
					
					boolean btcDefo =  tryCatch.blockToTry.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch();
					
					//Label endSegment = btcDefo||finDefo ?tryCatch.finalHandler:handler.getB();//if return then use generated, else use finalHandler
					Label endSegment = handler.getB();//if return then use generated, else use finalHandler
					
					if( (lastThingInBlockIsARet||lastTHingInBlockExcep) && finBlockDefRet && justOneBranchInBlock){
						//if first and the block to ret last exectued a ret, then overrite the catchblockentry label, combine with end region
						endSegment = tryCatch.finalHandler;
					}
					else if(lastTHingInBlockExcep &&   justOneBranchInBlock){
						//if it has no finally, but is an exception last visited, and just one, then we enter here
						endSegment = tryCatch.finalHandler;
					}
					else if(!btcDefo && justOneBranchInBlock){
						endSegment = tryCatch.finalHandler;
					}
					
					excpetionHandlers.add(new StartEndHandle(handler.getA(), endSegment, tryCatch.finalHandler, null));
					
				}
				
				nestedStartEndContContainer.addStart(tryCatch.finalHandler);
				
				
				// now add the nested segments captured by the catch block's
				//mv.visitLabel(tryCatch.finalHandler);
				if(tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()){//if defo returns then we dont care about the exception
					addNestedRegionsForFinBlock(tryCatch.finBlockWhenItDefoRets, nestedStartEndContContainer);
				}
				else{//have to rethrow the exception
					if(!tryCatch.blockToTry.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()){
						addNestedRegionsForFinBlock(tryCatch.finalBlockOnEndOfTry, nestedStartEndContContainer);
					}
					else{
						addNestedRegionsForFinBlock(tryCatch.finalBlock, nestedStartEndContContainer);
					}
				}
				
				if(tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()){ 				}
				else if(!tryCatch.blockToTry.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()){
					addNestedRegionsForFinBlock(tryCatch.finalBlock, nestedStartEndContContainer);
				}
				
				 
			}
			/*else if(tryCatch.cbs.isEmpty()){
				//special case in which there is no catch block and also no code in the finally as well
				//fake up a neste
				nestedStartEndContContainer.addStart(tryCatch.finalHandler);
				ArrayList<Label> ar = new ArrayList<Label>();
				ar.add(new Label());
				nestedStartEndContContainer.addEnd(ar);
			}*/
			else{
				//still process these for cases where you have nested divergence etc
				for(CatchBlocks cbk : tryCatch.cbs){

					Pair<Boolean, Boolean> lastThingRetExcep = lastLineChecker.isLastThingARetOrThrow(cbk.catchBlock);
					boolean lastThingInCatchRet = lastThingRetExcep.getA();
					cbk.endsInException = lastThingRetExcep.getB();
					
					//COPY PASTE ALERT!
					ArrayList<Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>> catchBlockFinRegions;
					
					if(!hasFinal ||(hasFinal && !tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()) || (cbk.catchBlock.hasDefoThrownException || cbk.endsInException)){//if the fin block doesnt retun then we just have one massiv region
						Thruple<ArrayList<Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>>, Label, Label> gear = allocator.process(cbk.catchBlock, cbk.entryLabel);
						catchBlockFinRegions = gear.getA();
					}
					else{
						catchBlockFinRegions=new ArrayList<Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>>();
					}
					
					if(catchBlockFinRegions.isEmpty()){
						if(!cbk.catchBlock.isEmpty() && cbk.catchBlock.getLastLogical().l instanceof ReturnStatement){
							ReturnStatement retStmt = (ReturnStatement)cbk.catchBlock.getLastLogical().l;
							Label endLabelforcatch = new Label();
							retStmt.setLabelBeforeAction(endLabelforcatch);
							catchBlockFinRegions.add(new Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>(cbk.entryLabel, endLabelforcatch, retStmt.extractInsertedFinSegmentsList(), true, false, true, null));
						}
						else{
							catchBlockFinRegions.add(new Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>(cbk.entryLabel, cbk.finLabel, null, false, false, false, null));
						}
					}
					
					for(int m = 0; m < catchBlockFinRegions.size(); m++ ){
						Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer> cbkStartEnd = catchBlockFinRegions.get(m);
					
						nestedStartEndContContainer.addStart(cbk.entryLabel);
						if(null != cbkStartEnd.getC()){
							nestedStartEndContContainer.addEnd(cbkStartEnd.getC(), cbkStartEnd.getG());
						}
					}
				}
			}
			
			nestedStartEndContContainer.fin();
			
			if(null != tryCatch.supressFinnallyException){
				excpetionHandlers.add(new StartEndHandle(tryCatch.supressFinnallyException.getA(), tryCatch.supressFinnallyException.getB(), tryCatch.supressFinnallyException.getC(), "java/lang/Throwable"));
			}
			
			
			return null;
		}
		
		
		private void addNestedRegionsForFinBlock(Block finBlock, NestedStartEndContContainer nestedStartEndContContainer){
			//More copy past nastyness
			ArrayList<Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>> catchBlockFinRegions;
			
			Label entryLabel = finBlock.getFirstLogical().l.getLabelOnEntry();
			if(null == entryLabel){
				entryLabel = new Label();
				finBlock.getFirstLogical().l.setLabelOnEntry(entryLabel);
			}
			
			boolean endsInException = lastLineChecker.isLastThingARetOrThrow(finBlock).getB();
			
			//if( finBlock.hasDefoThrownException||endsInException){//if the fin block doesnt retun then we just have one massiv region
				Thruple<ArrayList<Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer>>, Label, Label> gear = allocator.process(finBlock, entryLabel);
				catchBlockFinRegions = gear.getA();
			//}
			//else{
			//	catchBlockFinRegions=new ArrayList<Sixple<Label, Label, Label, Boolean, Boolean, Boolean>>();
			///}
			
			/*if(catchBlockFinRegions.isEmpty()){
				Line lastLogicalLine = finBlock.getLastLogical().l;
				if(lastLogicalLine instanceof ReturnStatement){
					ReturnStatement retStmt = (ReturnStatement)lastLogicalLine;
					Label endLabel = new Label();
					retStmt.setLabelToVisitJustBeforeReturnOpCode(endLabel);
					Label forNested = retStmt.getLinesToVisitOnEntry().isEmpty()?endLabel:retStmt.getLabelToVisitAfterReturnOpCode();
					
					catchBlockFinRegions.add(new Sixple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean>(entryLabel, null, forNested, false, false, true));
				}
			}*/ //no return statemetns inside a fin block
			
			for(Sevenple<Label, Label, ArrayList<Label>, Boolean, Boolean, Boolean, Integer> item : catchBlockFinRegions){
				nestedStartEndContContainer.addStart(item.getA());
				if(null != item.getC()){
					nestedStartEndContContainer.addEnd(item.getC(), item.getG());//only if this thing adds stuff via fin block
				}
			}
		}
	}
}

