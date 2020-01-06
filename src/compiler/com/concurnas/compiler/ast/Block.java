package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Label;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.bytecode.DefaultConstuctorFieldInitlizator;
import com.concurnas.compiler.bytecode.TryCatchLabelTagVisitor.TryCatchBlockPreallocatedLabelsEtc;
import com.concurnas.compiler.typeAndLocation.Location;
import com.concurnas.compiler.typeAndLocation.LocationLocalVar;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.compiler.visitors.WarningVariant;
import com.concurnas.compiler.visitors.datastructs.TheScopeFrame;
import com.concurnas.runtime.Pair;

public class Block extends CompoundStatement{
	
	public Boolean canContainAContinueOrBreak = false;
	public Boolean canContainAReturnStmt = false;
	public boolean isClass = false;
	public boolean isolated = false;
	public boolean isModuleLevel = false;
	public boolean defoReturns = false; //Useful for bytecode gennerator to avoid putting goto's after blocks which have def returned (especially in if else stmsts etc)
	private int loc = 0;
	public ArrayList<TryCatchBlockPreallocatedLabelsEtc> stuffTovisitTryCatchBlock;
	public boolean hasDefoThrownException;
	public DefaultConstuctorFieldInitlizator defFeildInit;
	public boolean staticFuncBlock = false;
	public boolean isAsyncBlock = false;//e.g. {/* stuff in here */}!
	public boolean isLocalizedLambda = false;
	public int localArgVarOffset = -1;
	public boolean isConstructor=false;
	public boolean isMethodBlock=false;
	public boolean isClassFieldBlock=false;
	public boolean hasTryBlockBreakout = false;
	public Label mustVisitLabelBeforeRet;
	public ArrayList<LineHolder> lines = new ArrayList<LineHolder>();
	private boolean shouldBePresevedOnStack=false;
	
	//public Type popTypeOnEntry = null; //used in finally blocks where u want to kill off whatever is stuck on the stack
	
	@Override
	public Node copyTypeSpecific() {
		Block ret = new Block(super.line, super.column);
		// omg
		ret.canContainAContinueOrBreak = this.canContainAContinueOrBreak;
		ret.canContainAReturnStmt = this.canContainAReturnStmt;
		ret.isClass = this.isClass;
		ret.isolated = this.isolated;
		ret.isModuleLevel = this.isModuleLevel;
		ret.defoReturns = this.defoReturns;
		ret.hasDefoBrokenOutOfTryCatch = hasDefoBrokenOutOfTryCatch;
		ret.hasDefoBroken = hasDefoBroken;
		ret.defoEscapesByBreakExceptRetu=defoEscapesByBreakExceptRetu;
		ret.hasRetExcepOrBreaked=hasRetExcepOrBreaked;
		ret.loc = this.loc;
		ret.stuffTovisitTryCatchBlock = (ArrayList<TryCatchBlockPreallocatedLabelsEtc>) Utils.cloneArrayList(this.stuffTovisitTryCatchBlock);
		ret.hasDefoThrownException = this.hasDefoThrownException;
		ret.hasDefoBrokenOutOfTryCatch = this.hasDefoBrokenOutOfTryCatch;
		ret.hasDefoBroken = this.hasDefoBroken;
		ret.defoEscapesByBreakExceptRetu = this.defoEscapesByBreakExceptRetu;
		ret.defFeildInit = this.defFeildInit;
		ret.staticFuncBlock = this.staticFuncBlock;
		ret.isAsyncBlock = this.isAsyncBlock;
		ret.isLocalizedLambda = this.isLocalizedLambda;
		ret.localArgVarOffset = this.localArgVarOffset;
		ret.isConstructor = this.isConstructor;
		ret.isMethodBlock = this.isMethodBlock;
		ret.isClassFieldBlock = this.isClassFieldBlock;
		ret.hasTryBlockBreakout = this.hasTryBlockBreakout;
		ret.isSynthetic = this.isSynthetic;
		ret.isAsyncBody = this.isAsyncBody;
		ret.isEnum = this.isEnum;
		ret.isAnnotation = this.isAnnotation;
		//ret.popTypeOnEntry = this.popTypeOnEntry;
		ret.mustVisitLabelBeforeRet = this.mustVisitLabelBeforeRet;
		ret.lines = (ArrayList<LineHolder>) Utils.cloneArrayList(this.lines);
		ret.originId = originId;//hack for when navigating around stack frames (which are keyed on Block ref)
		ret.scopeFrame = scopeFrame;//hack for when navigating around stack frames (which are keyed on Block ref)
		ret.shouldAddsetterAccessorIfGetterAdded = shouldAddsetterAccessorIfGetterAdded;
		ret.shouldBePresevedOnStack = shouldBePresevedOnStack;
		ret.returnExpectedOfEveryStatement = returnExpectedOfEveryStatement;
		ret.suppressedWarnings = suppressedWarnings==null?null:new HashSet<WarningVariant>(suppressedWarnings);
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		ret.isDirectParentABlock = isDirectParentABlock; 
		ret.isFuncDefBlock = isFuncDefBlock; 
		ret.canNestModuleLevelFuncDefs = canNestModuleLevelFuncDefs; 

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
	
	public Block copySetFirstLogical(Label tola){
		Block ret = (Block)super.copy();
		LineHolder lh = ret.getFirstLogical();
		if(null != lh){
			lh.l.setLabelOnEntry(tola);
		}
		return ret;
	}
	
	public Block originId = this;
	
	public Block getIdentity() {
		return originId;
	}
	
	public Block(int line, int column) {
		super(line, column);
	}
	
	/*
	 * Convenient
	 */
	public Block(int line, int column, Expression one) {
		this(line, column);
		this.add(new LineHolder(line, column, new DuffAssign(line, column, one)));
	}
	/*
	 * Convenient
	 */
	public Block(int line, int column, LineHolder one) {
		this(line, column);
		this.add(one);
	}
	
	/*
	 * Convenient x2
	 */
	public Block(int line, int column, Expression... ones) {
		this(line, column);
		for(Expression one : ones){
			this.add(new LineHolder(line, column, new DuffAssign(line, column, one)));
		}
	}
	
	public LineHolder getLast()
	{
		return lines == null || lines.isEmpty() ?null:lines.get(lines.size()-1);
	}
	
	public LineHolder popLast(){
		return lines.remove(lines.size()-1);
	}
	
	public LineHolder getLastLogical(){
		LineHolder lh = getLast();
		if(lh != null){
			Line l = lh.l;
			if(l instanceof Block){
				return ((Block)l).getLastLogical();
			}
		}
		return lh;
	}
	
	public LineHolder getFirst()
	{
		return lines == null || lines.isEmpty() ?null:lines.get(0);
	}
	
	public LineHolder getFirstLogical(){
		LineHolder lh = getFirst();
		if(lh != null && lh.l instanceof Block){
			return ((Block)lh.l).getFirstLogical();
		}
		return lh;
	}
	

	public boolean isEmpty()
	{
		return lines.isEmpty();
	}
	
	public Line getFirstLine()
	{
		return lines == null || lines.isEmpty() ?null:lines.get(0).l;
	}
	
	public void add(LineHolder li) {
		if(li ==null || li.l == null )
		{
			return;
		}
		this.lines.add(li);
	}
	public void addPenultimate(LineHolder li) {
		if(li ==null || li.l == null )
		{
			return;
		}
		
		this.lines.add(this.lines.size()-1, li);
	}
	
	
	public void add(Line li) {
		if(li == null){
			return;
		}
		add(new LineHolder(li));
	}
	
	public void add(int pos, LineHolder li) {
		if(li ==null || li.l == null )
		{
			return;
		}
		this.lines.add(pos, li);
	}
	
	
	
	
	public void add(int pos, Line li) {
		if(li == null){
			return;
		}
		add(pos, new LineHolder(li));
	}
	
	public void addAll(List<LineHolder> li) {
		assert li != null;
		for(LineHolder l : li){
			this.lines.add(l);
		}
	}
	
	public void addAllClasses(List<Pair<Integer, ClassDef>> blk) {
		for(Pair<Integer, ClassDef> lnAndl : blk){
			ClassDef l = lnAndl.getB();
			
			String clsName = l.getPrettyName();
			LineHolder hasAlready = classDefNmaeToLineHoler.get(clsName);
			if(null != hasAlready){//no replace, we got it right the first time, probably
				//hasAlready.l = l;
				
			}
			else{
				int srcLine = lnAndl.getA();
				
				int insertLocation = 0;
				
				for(LineHolder lh : this.lines){
					if(lh.getLine() >= srcLine){
						break;
					}
					insertLocation++;
				}
				LineHolder toAdd = new LineHolder(l.getLine(), l.getColumn(), l);
				this.lines.add(insertLocation, toAdd);
				classDefNmaeToLineHoler.put(clsName, toAdd);
			}
		}
		
	}
	
	private Map<String, LineHolder> classDefNmaeToLineHoler = new HashMap<String, LineHolder>();
	
	public boolean hasDefoReturnedThrownExceptionOrBrokenFromTryCatch(){
		return (defoReturns || hasDefoThrownException ||hasDefoBrokenOutOfTryCatch );// && !this.getShouldBePresevedOnStack();
		//return defoBreaksExceptsOrReturnsOutOfTryCatch;
	}
	
	public void reallyPrepend(LineHolder toPrepend )
	{//stick at top
		this.lines.add(0, toPrepend);
	}
	
	public void reallyPrepend(List<LineHolder> li) {//stick at top
		int n=0;
		for(LineHolder l : li){
			this.lines.add(n++, l);
		}
	}
	
	public void prepend(LineHolder toPrepend )
	{//yeah, this doesnt actually work as expected in all cases, it's a prepend from the current index... (useful if ur in the block atm)
		//Add line to next slot in lines.
		if(loc > this.lines.size())
		{
			this.lines.add(toPrepend);
		}
		else
		{
			this.lines.add(loc, toPrepend);
		}
	}
	
	public void reallyPrependPenultimate(List<LineHolder> li) {//stick at top
		int n=this.lines.size()-1;
		for(LineHolder l : li){
			this.lines.add(n++, l);
		}
	}
	
	public void postpend(LineHolder toPostpend )
	{
		this.lines.add(toPostpend);
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}


	public LineHolder startItr(){
		this.loc = 0;
		if(!this.lines.isEmpty())
		{
			LineHolder lh = this.lines.get(this.loc);
			
			if(this.lines.size()-1 == loc){
				lh.lastLine = true;
			}
			
			this.loc++;
			return lh;
		}
		return null;
	}
/*	
	public boolean currentLast(){
		return this.loc >= this.lines.size();
	}*/
	
	public LineHolder getNext()
	{
		if(this.loc >= this.lines.size())
		{
			return null;
		}
		else
		{
			LineHolder lh = this.lines.get( this.loc);
			
			if(this.lines.size()-1 == loc){
				lh.lastLine = true;
			}
			
			loc++;
			return lh;
		}
	}


	public void replaceLineHolder(LineHolder oldLH, LineHolder newlh) {
		int idx = lines.indexOf(oldLH);
		if(idx == -1) {
			idx=0;//JPT: why would this be called if the line were missing?
		}
		lines.set(idx, newlh);
	}
	
	public void replaceLast(LineHolder newlh) {
		lines.set(lines.size()-1, newlh);
	}

	private TheScopeFrame scopeFrame;
	public boolean hasDefoBrokenOutOfTryCatch = false;
	public boolean hasDefoBroken = false;
	public boolean defoEscapesByBreakExceptRetu=false;
	public boolean hasRetExcepOrBreaked;
	public Boolean hasDefoReturned=false;
	public boolean hasDefoBrokenOrContinued=false;
	public boolean returnExpectedOfEveryStatement = false;
	public boolean isSynthetic=false;
	public boolean isAsyncBody=false;
	public boolean isEnum=false;
	public boolean isAnnotation=false;
	public HashSet<WarningVariant> suppressedWarnings = new HashSet<WarningVariant>();
	
	public void setScopeFrame(TheScopeFrame scoeFrame) {
		if(null != this.scopeFrame){
			//remap local vars to here
			for(TypeAndLocation tal : this.scopeFrame.getAllVars(null).values()){//this occurs in cases where we need to remain consistant with
				//the block and the scopeFrame defined on the localvar - used in ExprTryBlockRepointer
				Location loc = tal.getLocation();
				if(loc instanceof  LocationLocalVar){
					LocationLocalVar lolcalvar = (LocationLocalVar)loc;
					lolcalvar.scopeFramDefinedIn = scoeFrame;
				}
			}
		}
		this.scopeFrame=scoeFrame;
	}
	
	public TheScopeFrame getScopeFrame( ) {
		return this.scopeFrame;
	}

	public Label getFirstLogicalLabel(Label label) {
		Node fifrst = this.getFirstLogical();
		Label x = fifrst.getLabelOnEntry();
		if(null==x){
			x = label;
			fifrst.setLabelOnEntry(x);
		}
		return x; 
		
	}

	public boolean containsSingleVoidReturn() {
		
		if(this.isEmpty() || this.lines.size() > 1){
			return false;
		}
		
		LineHolder last = this.getLastLogical();
		if(last.l instanceof Block && ((Block)last.l).isolated){
			return ((Block)last.l).containsSingleVoidReturn();
		}
		return last.l instanceof ReturnStatement && ((ReturnStatement)last.l).ret==null;
	}

	public static int cnt = 0;
	
	public void setShouldBePresevedOnStack(boolean should)
	{
		this.shouldBePresevedOnStack=should;
		//super.setShouldBePresevedOnStack(should);
		if(!lines.isEmpty()){
			lines.get(lines.size()-1).setShouldBePresevedOnStack(should);
		}
	}
	
	public boolean getShouldBePresevedOnStack()
	{
		return this.shouldBePresevedOnStack;
	}
	
	/**
	 * i.e. without synthetic Conitnue statement at the end of the while block
	 */
	public ArrayList<LineHolder> getLinesExcludeSynthetics() {
		ArrayList<LineHolder> ret = new ArrayList<LineHolder>(this.lines.size());
		for(LineHolder lh : this.lines){
			if(!(lh.l instanceof ContinueStatement && ((ContinueStatement)lh.l).isSynthetic)){
				//if not conitnue and synthetic
				ret.add(lh);
			}
		}
		
		return ret;
	}
	

	private HashMap<Pair<String, FuncType>, LineHolder> prependedStuff = new HashMap<Pair<String, FuncType>, LineHolder>();
	public boolean isDirectParentABlock = false;
	public boolean isFuncDefBlock=false;
	public boolean canNestModuleLevelFuncDefs=false;
	public boolean isExtFunc=false;
	public HashMap<Pair<String, Boolean>, NullStatus> inferedNullability;
	public FuncDef funcDef;
	public void overwriteLineHolder(Pair<String, FuncType> key, LineHolder newlh){
		Pair<String, FuncType> keyNoRet = new Pair<String, FuncType>(key.getA(), key.getB().copyIgnoreReturnType());
		
		LineHolder oldLH = prependedStuff.get(key);
		
		if(null == oldLH){
			if(!prependedStuff.isEmpty()) {
				prependedStuff = new HashMap<Pair<String, FuncType>, LineHolder>(prependedStuff);//recalculate hashcodes incase they have changed
				oldLH = prependedStuff.get(key);
			}
		}
		
		if(null == oldLH){
			oldLH = prependedStuff.get(keyNoRet);
		}
		
		if(null != oldLH){
			this.replaceLineHolder(oldLH, newlh);
		}
		else{
			this.prepend(newlh);
		}
		prependedStuff.put(key, newlh);
		prependedStuff.put(keyNoRet, newlh);
	}
	
	@Override
	public void setIfReturnsExpectImmediateUse(boolean ifReturnsExpectImmediateUse) {
		ArrayList<LineHolder> lines = this.lines;
		if(!(lines==null || lines.isEmpty())){
			LineHolder last = lines.get(lines.size()-1);
			Node lastOne = (Node)last.l;
			if(lastOne instanceof TryCatch && ((TryCatch)lastOne).hasFinal()) {
				return;//MHA! does this really not apply for try catch with final?
			}
			lastOne.setIfReturnsExpectImmediateUse( ifReturnsExpectImmediateUse);
		}
	}
	
}
