package com.concurnas.compiler.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import com.concurnas.compiler.ast.AnnotationDef;
import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.CatchBlocks;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.ConstructorDef;
import com.concurnas.compiler.ast.DotOperator;
import com.concurnas.compiler.ast.ElifUnit;
import com.concurnas.compiler.ast.EnumDef;
import com.concurnas.compiler.ast.ForBlock;
import com.concurnas.compiler.ast.ForBlockOld;
import com.concurnas.compiler.ast.FuncParam;
import com.concurnas.compiler.ast.IfStatement;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.RefThis;
import com.concurnas.compiler.ast.TryCatch;
import com.concurnas.compiler.ast.WithBlock;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.datastructs.TheScopeFrame;
import com.concurnas.compiler.visitors.util.VarAtScopeLevel;

public class DefoAssignmentVisitor extends AbstractErrorRaiseVisitor {
	public DefoAssignmentVisitor(String fullPathFileName) {
		super(fullPathFileName);
	}
	
	private Stack<HashSet<String>> stuffDefoAssignedAbove = new Stack<HashSet<String>>();
	private Stack<Boolean> isClassBlock = new Stack<Boolean>();//all vars in class blocks are automatically assigned to 
	
	private static class IsAssignedAndAssigner{
		public Boolean hasBeenAssigned;
		public AssignNew origionalAss;
		public IsAssignedAndAssigner(Boolean hasBeenAssigned, AssignNew origionalAss){
			this.hasBeenAssigned =hasBeenAssigned;
			this.origionalAss =origionalAss;
		}
	}
	
	private void enterHasDefo()
	{
		stuffDefoAssignedAbove.push(new HashSet<String>());
		stackFrame.push(new HashMap<String, IsAssignedAndAssigner>());
	}
	
	private HashSet<String> stuffToSetOnExit()
	{
		stackFrame.pop();
		return stuffDefoAssignedAbove.pop();
	}
	

	
	private Stack<HashMap<String, IsAssignedAndAssigner> /* varName -> hasdefoBeenSet*/ > stackFrame = new Stack<HashMap<String, IsAssignedAndAssigner>>();
	
	
	private boolean handleVarAssignmentOrNew(String varname, boolean isNew, boolean isAssigned, AssignNew ass)//returns true if set at this level
	{
		isAssigned |= isClassBlock.peek();//if at class level then automatically assigned
		
		if(ass != null) {
			isAssigned |= ass.isModuleLevelShared;
		}
		
		if(isNew)
		{//s int; y int =5
			stackFrame.peek().put(varname, new IsAssignedAndAssigner(isAssigned, ass));
			return false;
		}
		else
		{//not new
			if(isAssigned)
			{//g=9
				return setDefAssignedsWasSetHigher(varname);
			}
			else
			{//h = g, accessed?
				//possible?
				return getDefoAsignedVar(varname).hasBeenAssigned;
			}
		}
	}
	
	private void setDudesHigherUp(HashSet<String> dudes)
	{
		if(null != dudes){
			for(String dude : dudes)
			{
				if(setDefAssignedsWasSetHigher(dude))
				{
					stuffDefoAssignedAbove.peek().add(dude);//oh not assigned at this level must be one higher
				}
			}
		}
	}
	
	
	@Override
	public Object visit(AssignExisting assignExisting) {
		
		if(assignExisting.eq == null || !assignExisting.eq.isEquals()){
			return null;
		}
		
		if(assignExisting.expr != null)
		{
			assignExisting.expr.accept(this);
		}
				
		if(assignExisting.assignee instanceof RefName)
		{
			boolean isNew = assignExisting.isReallyNew;
			boolean isaSsigned = assignExisting.expr != null;
			String name = ((RefName)assignExisting.assignee).name;
			if(handleVarAssignmentOrNew(name, isNew, isaSsigned, null))
			{
				stuffDefoAssignedAbove.peek().add(name);
			}
		}
		if(null != valsSetInCurrentConstructor){
			//in construcotr - two valid approaches are this.y = 9, or y = 9;
			if(assignExisting.assignee instanceof RefName){
				valsSetInCurrentConstructor.add(((RefName)assignExisting.assignee).name);
			}
			else if(assignExisting.assignee instanceof DotOperator){
				DotOperator doo = (DotOperator)assignExisting.assignee;
				ArrayList<Expression> elements = doo.getElements(this);
				if(elements.size() == 2){
					Expression one = elements.get(0);
					Expression two = elements.get(1);
					if(one instanceof RefThis && two instanceof RefName){
						valsSetInCurrentConstructor.add(((RefName)two).name);
					}
				}
			}
			//
				
		}
		
		return null;
	}
	
	@Override
	public Object visit(EnumDef enumDef){
		if(null!=enumDef.classDefArgs)
		{
			enumDef.classDefArgs.accept(this);
		}

		enumDef.fakeclassDef.accept(this);
		
		//enumDef.block.accept(this);
		
		return null;
	}
	
	@Override
	public Object visit(AnnotationDef annotationDef){
		
		annotationDef.fakeclassDef.accept(this);
		
		
		return null;
	}
	
	public Object visit(AssignNew assignNew) {
		boolean isNew = assignNew.isReallyNew;
		boolean isaSsigned = assignNew.expr != null;
		String name = assignNew.name;
		
		if(handleVarAssignmentOrNew(name, isNew, isaSsigned, isNew?assignNew:null))
		{
			stuffDefoAssignedAbove.peek().add(name);
		}
		
		if(assignNew.isFinal && assignNew.expr != null && assignNew.isClassField){
			valsAlreadySetWhenDeclaredAtClassLevel.add(name);
		}
		
		
		return null;
	}
	
	
	
	public boolean setDefAssignedsWasSetHigher(String name)
	{//defo assign and set at higher level if approperiate
		boolean setHigher = false;
		for (int i = stackFrame.size() - 1; i >= 0; i--) {
			final HashMap<String, IsAssignedAndAssigner> level = (HashMap<String, IsAssignedAndAssigner>) stackFrame.get(i);
			if (level.containsKey(name)) {
				if(!setHigher)
				{//only set if its in ur own scope, dont set if we've moved higher
					level.put(name, alwaysTrueOrigAssignment);
				}
				
				break;
			}
			setHigher = true;
		}
		return setHigher;
	}
	
	private static IsAssignedAndAssigner alwaysTrueOrigAssignment = new IsAssignedAndAssigner(true, null);
	
	private IsAssignedAndAssigner  getDefoAsignedVar(String name) {
		//reverse iterate through stack frame from bottum to top to find the variable

		for (int ix = this.stuffDefoAssignedAbove.size() - 1; ix >= 0; ix--) {
			HashSet<String> level = this.stuffDefoAssignedAbove.get(ix);
			if(level.contains(name)) {
				return alwaysTrueOrigAssignment;
			}
		}
		
		for (int i = stackFrame.size() - 1; i >= 0; i--) {
			final HashMap<String, IsAssignedAndAssigner> level = (HashMap<String, IsAssignedAndAssigner>) stackFrame.get(i);
			if (level.containsKey(name)) {
				return level.get(name);
			}
		}
		return alwaysTrueOrigAssignment;//if cannot find, then assume it was set to something, in any case if it really dont exist it would be flagged elsewhere...
	}
	
	
	private boolean inDotOperator = false;
	@Override
	public Object visit(DotOperator dotOperator) {
		boolean prevdo = inDotOperator;
		int n=0;
		for(Expression e: dotOperator.getElements(this))
		{
			e.accept(this);
			if(n == 0) {//ensure we process first element and dont considerit partof the dot operator
				inDotOperator = true;
			}
			n++;
		}
		inDotOperator = prevdo;
		return null;
	}

	private HashSet<String> varsToAddonblockentry = new HashSet<String>();
	private HashSet<Expression> nodesToPretendAreInBlock = new HashSet<Expression>();
	
	private HashSet<String> handleFunctionBlock(Block funcBlock)
	{//return stuff to set higher up
		enterHasDefo();
		if(!varsToAddonblockentry.isEmpty()){
			for(String varname : varsToAddonblockentry) {
				handleVarAssignmentOrNew(varname, true, true, null);
			}
			varsToAddonblockentry = new HashSet<String>();
		}
		
		if(!nodesToPretendAreInBlock.isEmpty()){
			HashSet<Expression> nodez = (HashSet<Expression>)nodesToPretendAreInBlock.clone();
			nodesToPretendAreInBlock = new HashSet<Expression>();//them them to avoid inf loop
			for(Expression node : nodez) {//for (nohoh=9;{nohoh++; nohoh<10};) {} //this case it could be equal
				if(funcBlock != node){
					node.accept(this);
				}
			}
			
		}
		
		LineHolder lh = funcBlock.startItr();
		while(lh != null)
		{
			lh.accept(this);
			lh = funcBlock.getNext();
		}
		return stuffToSetOnExit();
	}
	
	@Override
	public Object visit(Block block) {
		isClassBlock.push(block.isClass);
		HashSet<String> setHigher = handleFunctionBlock(block);
		if(block.isolated) {
			setDudesHigherUp(setHigher);
		}
		//canset stuff: function, inner nested, barrier, async cannot, if etc try catch with
		isClassBlock.pop();
		return null;
	}
	
	@Override
	public Object visit(IfStatement ifStatement) {
		HashSet<String> setOnExit = null;
		if(ifStatement.elseb!=null)
		{
			if(!ifStatement.ifblock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()){
				setOnExit = handleFunctionBlock(ifStatement.ifblock);
			}
			for(ElifUnit u : ifStatement.elifunits)
			{	
				HashSet<String> visitedAtThisLevel = handleFunctionBlock(u.elifb);
				if(!u.elifb.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()){
					if(setOnExit==null){
						setOnExit = visitedAtThisLevel;
					}
					else{
						setOnExit.retainAll(visitedAtThisLevel);
					}
					
				}
			}
			
			HashSet<String> visitedAtThisLevel =handleFunctionBlock(ifStatement.elseb);
			if(!ifStatement.elseb.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()){
				if(setOnExit==null){
					setOnExit = visitedAtThisLevel;
				}
				else{
					setOnExit.retainAll(visitedAtThisLevel);
				}
			}
			
		}
		setDudesHigherUp(setOnExit);//only defo set if set in all places of the if
		return null;
	}
	
	@Override
	public Object visit(TryCatch tryCatch) {
		if(tryCatch.astRepoint!=null){
			return tryCatch.astRepoint.accept(this);
		}
		
		//same model as if elif else
		HashSet<String> setOnExit = null;
		
		if(!tryCatch.blockToTry.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()){
			setOnExit = handleFunctionBlock(tryCatch.blockToTry);
		}
		
		for(CatchBlocks u : tryCatch.cbs)
		{	
			varsToAddonblockentry.add(u.var);
			
			HashSet<String> visitedAtThisLevel =handleFunctionBlock(u.catchBlock);
			if(!u.catchBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()){
				if(setOnExit==null){
					setOnExit = visitedAtThisLevel;
				}
				else{
					setOnExit.retainAll(visitedAtThisLevel);
				}
			}
		}
		if(tryCatch.finalBlock!=null)
		{
			HashSet<String> visitedAtThisLevel =handleFunctionBlock(tryCatch.finalBlock);
			if(!tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()){
				if(setOnExit==null){
					setOnExit = visitedAtThisLevel;
				}
				else{
					if(tryCatch.cbs.isEmpty()){//try{ f=6} finally{ g=3 } //f is defo set as there is no catching of exceptions, and if thrown then invalidates later code is it would be in exception form
						setOnExit.addAll(visitedAtThisLevel);
					}else{
						setOnExit.retainAll(visitedAtThisLevel);
					}
				}
			}
		}
		setDudesHigherUp(setOnExit);
		return null;
	}
	
	@Override
	public Object visit(WithBlock withBlock) {
		setDudesHigherUp(handleFunctionBlock(withBlock.blk));
		return null;
	}

	@Override
	public Object visit(RefName refName) {
		//var access
		if(!inDotOperator){
			//we don't check the likes of a[2].gg = 4 etc
			String name = refName.name;
			IsAssignedAndAssigner hasBeenSet = getDefoAsignedVar(name);
			if(!hasBeenSet.hasBeenAssigned){
				AssignNew assignemnt = hasBeenSet.origionalAss;
				if(null != assignemnt && assignemnt.type instanceof NamedType && ((NamedType)assignemnt.type).getIsRef() && !((NamedType)assignemnt.type).hasArrayLevels() ){
					//this is ok, mark initial with stub
					assignemnt.createRefInitializer=true;
				}
				else{
					this.raiseError(refName.getLine(), refName.getColumn(), String.format("The variable %s may not have been initialized", name));
				}
			}
		}
		return null;
	}
	
	//the following add vars to the block...
	
	@Override
	public Object visit(ForBlock forBlock) {
		varsToAddonblockentry.add(forBlock.localVarName);
		forBlock.expr.accept(this);
		forBlock.block.accept(this);
		
		return null;
	}
	
	
	@Override
	public Object visit(ForBlockOld forBlockOld) {
		if(null != forBlockOld.assignExpr) 
		{
			forBlockOld.assignExpr.accept(this);
		}
		else if(null != forBlockOld.assignName)
		{
			varsToAddonblockentry.add(forBlockOld.assignName);
			
			if( null != forBlockOld.assigType) forBlockOld.assigType.accept(this);
			if( null != forBlockOld.assigFrom) forBlockOld.assigFrom.accept(this);
		}
		//MHA: pretend that the next bits are at the top of the for loop for purposes of definite assignment (dirty dirty hack!)
		
		if(null != forBlockOld.check){nodesToPretendAreInBlock.add(forBlockOld.check);}
		if(null != forBlockOld.postExpr){nodesToPretendAreInBlock.add(forBlockOld.postExpr);} 
		
		forBlockOld.block.accept(this);
		
		return null;
	}
	
	//funcs and lambdas also
	@Override
	public Object visit(FuncParam funcParam) {
		varsToAddonblockentry.add(funcParam.name);
		return null;
	}
	
	
	//stuff for class vals...
	private HashSet<String> valsSetInCurrentConstructor = null;	
	private HashMap<ConstructorDef, HashSet<String>> constructorTovalsSetInConstructor = new HashMap<ConstructorDef, HashSet<String>>();
	private HashMap<ConstructorDef, ConstructorDef> constructorCallsOwnConstructor = new HashMap<ConstructorDef, ConstructorDef>();
	private HashSet<String> valsAlreadySetWhenDeclaredAtClassLevel;	
	
	@Override
	public Object visit(ConstructorDef funcDef) {
		HashSet<String> prev = valsSetInCurrentConstructor;
		valsSetInCurrentConstructor = new HashSet<String>();
		Object g = super.visit(funcDef);
		HashSet<String> found = valsSetInCurrentConstructor;
		valsSetInCurrentConstructor = prev;
		constructorTovalsSetInConstructor.put(funcDef, found);
		ConstructorDef another = funcDef.callsThisConstructor;
		//if(another != null){
			constructorCallsOwnConstructor.put(funcDef, another);
		//}
		
		return g;
	}
	
	private String extractVarNames(HashSet<String> finalFieldsInClass){
		int ss =finalFieldsInClass.size();
		
		ArrayList<String> toSort = new ArrayList<String>(ss);
		for(String e : finalFieldsInClass){
			toSort.add(e);
		}
		Collections.sort(toSort);//sorted!
		
		StringBuilder sb = new StringBuilder();
		int sz = ss-1;
		int n=0;
		for(String e : toSort){
			sb.append(e);
			if(n != sz){
				sb.append(", ");
			}
			n++;
		}
		
		return sb.toString();
	}
	
	private HashSet<String> justNames(ArrayList<VarAtScopeLevel> finalFieldsInClass){
		HashSet<String> ret = new HashSet<String>();
		for(int n=0; n < finalFieldsInClass.size(); n++){
			ret.add(finalFieldsInClass.get(n).getVarName());
		}
		
		return ret;
	}
	
	private ArrayList<VarAtScopeLevel> removeEnumFields(ArrayList<VarAtScopeLevel> items, NamedType enumType){
		ArrayList<VarAtScopeLevel> ret = new ArrayList<VarAtScopeLevel>(items.size());
		
		for(VarAtScopeLevel item : items){
			if(!item.getType().equals(enumType)){
				ret.add(item);
			}
		}
		
		return ret;
	}
	
	@Override
	public Object visit(ClassDef classDef) {
		HashMap<ConstructorDef, HashSet<String>> prevconstructorTovalsSetInConstructor = constructorTovalsSetInConstructor;
		HashMap<ConstructorDef, ConstructorDef>  prevconstructorCallsOwnConstructor = constructorCallsOwnConstructor;
		
		constructorTovalsSetInConstructor = new HashMap<ConstructorDef, HashSet<String>>();
		constructorCallsOwnConstructor = new HashMap<ConstructorDef, ConstructorDef>();
		
		HashSet<String> prev = valsAlreadySetWhenDeclaredAtClassLevel;
		
		valsAlreadySetWhenDeclaredAtClassLevel = new HashSet<String>();
		Object ret = super.visit(classDef);
		HashSet<String> setAlready = valsAlreadySetWhenDeclaredAtClassLevel;
		valsAlreadySetWhenDeclaredAtClassLevel = prev;
		
		//follow the trail and ensure all final vars are set
		TheScopeFrame tsf = classDef.getScopeFrame();
		if(null!= tsf){
			ArrayList<VarAtScopeLevel> finalFieldsInClass = tsf.getAllVariablesAtScopeLevel(true, true, false, false);
			if(!finalFieldsInClass.isEmpty()) {
				processFieldsAndErr(classDef, finalFieldsInClass, setAlready, "These variables have been declared val");
			}
			
			if(!classDef.objProvider) {
				ArrayList<VarAtScopeLevel> nonNullFields = tsf.getAllVariablesAtScopeLevel(false, true, false, true);
				if(!nonNullFields.isEmpty()) {
					processFieldsAndErr(classDef, nonNullFields, setAlready, "These variables have been declared non nullable");
				}
			}
		}
		
		constructorTovalsSetInConstructor = prevconstructorTovalsSetInConstructor;
		constructorCallsOwnConstructor = prevconstructorCallsOwnConstructor;
		
		return ret;
	}
	
	private void processFieldsAndErr(ClassDef classDef, ArrayList<VarAtScopeLevel> finalFieldsInClass, HashSet<String> setAlready, String errprefix) {
		if(classDef.isEnum){
			finalFieldsInClass = removeEnumFields(finalFieldsInClass, new NamedType(classDef));
		}
		
		HashSet<String>  allNamesNeeded = justNames(finalFieldsInClass);
		allNamesNeeded.removeAll(setAlready);//remove all final fields that are initially set
		if(!finalFieldsInClass.isEmpty()){
			//now for each constructor node path ensure that all the vals are just once and once only
			
			if(constructorCallsOwnConstructor.isEmpty()){
				if(!allNamesNeeded.isEmpty()){
					String names = extractVarNames(allNamesNeeded);
					this.raiseError(classDef.getLine(), classDef.getColumn(), String.format("%s but have not been assigned a value by any constructor: %s", errprefix, names));
				}
			}
			else{//evaluate all constructor paths - there are no recusrive cycles possible
				//JPT: this could be optimized
				for(ConstructorDef startConst: constructorCallsOwnConstructor.keySet()){//start points
					HashSet<String> hasDefoSet = new HashSet<String>();
					ConstructorDef constr = startConst;
					boolean isFirst =true;
					HashSet<String> doubleSetted = new HashSet<String>();
					while(null != constr){
						
						HashSet<String> atThisLevel = constructorTovalsSetInConstructor.get(constr);
						if(!isFirst){
							for(String parAdd : atThisLevel){
								if( hasDefoSet.contains(parAdd)){
									doubleSetted.add(parAdd);
								}
							}
						}
						
						hasDefoSet.addAll( atThisLevel );
						if(isFirst){
							isFirst=false;
						}
						//if thing visited already in parent, then fail in child, put this in a list of fails
						constr = constructorCallsOwnConstructor.get(constr);
					}
					
					if(!doubleSetted.isEmpty()){
						String names = extractVarNames(doubleSetted);
						this.raiseError(startConst.getLine(), startConst.getColumn(), String.format("%s and can only be set once in constructor call hierarchy: %s", errprefix, names));
					}
					else{
						HashSet<String>  allNamesNeededForThisRound = (HashSet<String>) allNamesNeeded.clone();
						allNamesNeededForThisRound.removeAll(hasDefoSet);
						
						if(!allNamesNeededForThisRound.isEmpty()){
							String names = extractVarNames(allNamesNeededForThisRound);
							String auto = startConst.isAutoGennerated?"the auto gennerated":"this";
							
							this.raiseError(startConst.getLine(), startConst.getColumn(), String.format("%s but have not been assigned a value in %s constructor: %s", errprefix, auto, names));
						}
					}
				}
			}
		}
	}
	
}
