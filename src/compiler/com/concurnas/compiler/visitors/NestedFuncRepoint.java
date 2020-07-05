package com.concurnas.compiler.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.concurnas.compiler.ast.AccessModifier;
import com.concurnas.compiler.ast.Annotation;
import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.ast.AssignStyleEnum;
import com.concurnas.compiler.ast.AsyncBlock;
import com.concurnas.compiler.ast.AsyncBodyBlock;
import com.concurnas.compiler.ast.AsyncRefRef;
import com.concurnas.compiler.ast.Await;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.BreakStatement;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.ast.ContinueStatement;
import com.concurnas.compiler.ast.DotOperator;
import com.concurnas.compiler.ast.DuffAssign;
import com.concurnas.compiler.ast.ExpressionList;
import com.concurnas.compiler.ast.ForBlock;
import com.concurnas.compiler.ast.ForBlockOld;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.FuncInvoke;
import com.concurnas.compiler.ast.FuncParam;
import com.concurnas.compiler.ast.FuncParams;
import com.concurnas.compiler.ast.FuncRef;
import com.concurnas.compiler.ast.FuncRefArgs;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.HasExtraCapturedVars;
import com.concurnas.compiler.ast.IgnoreASTRepointForReturn;
import com.concurnas.compiler.ast.LambdaDef;
import com.concurnas.compiler.ast.Line;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.New;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.OnChange;
import com.concurnas.compiler.ast.OnEvery;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.RefThis;
import com.concurnas.compiler.ast.ReturnStatement;
import com.concurnas.compiler.ast.SuperConstructorInvoke;
import com.concurnas.compiler.ast.ThisConstructorInvoke;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.WhileBlock;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.ast.util.ExpressionListOrigin;
import com.concurnas.compiler.bytecode.FuncLocation;
import com.concurnas.compiler.bytecode.FunctionGenneratorUtils;
import com.concurnas.compiler.bytecode.OnChangeAwaitBCGennerator;
import com.concurnas.compiler.typeAndLocation.Location;
import com.concurnas.compiler.typeAndLocation.LocationClassField;
import com.concurnas.compiler.typeAndLocation.LocationLocalVar;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.visitors.NestedFuncRepoint.Step2AddExtraCapturedVarsEtc;
import com.concurnas.compiler.visitors.datastructs.TheScopeFrame;
import com.concurnas.runtime.Pair;
import com.concurnas.runtime.cps.RefStateTracker;

public class NestedFuncRepoint extends AbstractErrorRaiseVisitor {

	/**
	 * Add extra vars captured like so fun g() = { x=1; fun ff() = x+1; ff() }// x is captured within ff and is passed in as a hidden argument
	 * also for onchange capture, ensure that arguments are bound to state holder
	 */
	public class Step2AddExtraCapturedVarsEtc extends AbstractErrorRaiseVisitor
	{
		protected Step2AddExtraCapturedVarsEtc(String fullPathFileName) {
			super(fullPathFileName);
		}

		private Expression createVariable(int line, int col, String name, Type type){
			int refLevels = TypeCheckUtils.getRefLevels(type);
			Expression ret = new RefName(line, col, name);
			if(refLevels>0){
				ret = new AsyncRefRef(line, col, ret, refLevels);
			}//ensure that for spliced in arguments, we do not attempt to unref them
			return ret;
		}
	
		
		
		private int step2nestedLevel = 0;//nested level is used to track location of initial declaration of variable which may end up as funcparam
		//e.g. xs=10; fun getXS() = xs; fun moi() = getXS() //xs is going to need to be spliced in

		
		@Override
		public Object visit(FuncDef block) {
			step2nestedLevel++;
			super.visit(block);
			step2nestedLevel--;
			
			return null;
		}
		
		@Override
		public Object visit(ClassDef cd) {
			if(cd.isLocalClass){
				step2nestedLevel++;
				super.visit(cd);
				step2nestedLevel--;
			}else{
				super.visit(cd);
			}
			
			return null;
		}
		
		@Override
		public Object visit(LambdaDef block) {
			step2nestedLevel++;
			super.visit(block);
			step2nestedLevel--;
			
			return null;
		}
		
		
		//private Stack<HashMap<String, FuncParam>> localVarStack = new Stack<HashMap<String, FuncParam>>();
		
		@Override
		public Object visit(AssignNew assignNew) {
			//defo new
			//if(inFunction){
			if(!localVarStack.isEmpty()){
				FuncParam fp = new FuncParam(assignNew.getLine(), assignNew.getColumn(), assignNew.name, assignNew.getTaggedType(), false);
				localVarStack.peek().peek().put( assignNew.name, new Pair<Boolean, FuncParam>(inFunction, fp) );
			//}
			}
			return super.visit(assignNew);
		}

		@Override
		public Object visit(AssignExisting assignExisting) {
			if(!localVarStack.isEmpty() && /*inFunction &&*/ assignExisting.isReallyNew && assignExisting.assignee instanceof RefName){
				String name = ((RefName)assignExisting.assignee).name;
				FuncParam fp = new FuncParam(assignExisting.getLine(), assignExisting.getColumn(),  name, assignExisting.getTaggedType(), false );
				localVarStack.peek().peek().put(name, new Pair<Boolean, FuncParam>(inFunction, fp) );
			}
			return super.visit(assignExisting);
		}

		private ArrayList<Pair<ExpressionListOrigin, ArrayList<Expression>>> exprListToExtraPairs;
		
		@Override
		public Object visit(ExpressionList expressionList) {
			if(expressionList.astRedirect != null) {
				ArrayList<Pair<ExpressionListOrigin, ArrayList<Expression>>> prev = exprListToExtraPairs;
				exprListToExtraPairs = new ArrayList<Pair<ExpressionListOrigin, ArrayList<Expression>>>();
				
				expressionList.astRedirect.accept(this);
				
				if(hadMadeRepoints && !exprListToExtraPairs.isEmpty()) {
					//add args back to front to source expression list...
					int sz = exprListToExtraPairs.size()-1;
					for(int n = sz; n >= 0; n--) {
						Pair<ExpressionListOrigin, ArrayList<Expression>> items = exprListToExtraPairs.get(n);
						ExpressionListOrigin exprListOrigin = items.getA();
						exprListOrigin.origin.exprs.addAll(exprListOrigin.argEnd, items.getB());
					}
					
				}
				exprListToExtraPairs = prev;
				
				
			}else {
				for(Expression expr : expressionList.exprs){
					expr.accept(this);
				}
			}
			
			return null;
		}

		
		//extra stuff added here which is not locally defined, -> spit back up to calling funcdef (so rewrite moi)
		@Override
		public Object visit(FuncInvoke funcInvoke) {
			Object x = super.visit(funcInvoke);
			
			if(null != funcInvoke.resolvedFuncTypeAndLocation){
				Location loc = funcInvoke.resolvedFuncTypeAndLocation.getLocation();
				
				if (loc instanceof FuncLocation) {
					FuncLocation fl = (FuncLocation) loc;
					if (null != fl) {//TODO: remove this null check , not possible?
						FuncDef df = fl.getTaggedFuncDef();
						if (null != df) {
							if (!funcInvoke.funName.equals(df.funcName)) {
								funcInvoke.overrideFuncName(df.funcName);
								// add extra args, if there are any
								ArrayList<Expression> addParams = new ArrayList<Expression>();
								ArrayList<FuncParam> extrafp = df.params.params;
								for (int n = funcInvoke.args.getArgumentsWNPs().size(); n < extrafp.size(); n++) {
									FuncParam toAdd = extrafp.get(n);
									
									if(null != toAdd.sytheticDefinitionLevel && step2nestedLevel == toAdd.sytheticDefinitionLevel){
										//the variable is defined at this level, so revert the name back to its origonal name
										toAdd = (FuncParam)toAdd.copy();
										toAdd.name = toAdd.name.substring(0, toAdd.name.indexOf("$n"));;
									}
									
									Expression toAddv = createVariable(funcInvoke.getLine(), funcInvoke.getColumn(), toAdd.name, toAdd.getTaggedType());
									addParams.add(toAddv);
									funcInvoke.addNIFArg(toAddv);
																	
									
									if(!isVarDefinedLocally(toAdd.name)){//null -> defined locally so we dont bind it
										if(null != extraVarsWithinForOnChange && !extraVarsWithinForOnChange.containsKey(toAdd.name)){
											extraVarsWithinForOnChange.put(toAdd.name, toAdd);
										}
									}
								}
								
								if(funcInvoke.expressionListOrigin != null && !addParams.isEmpty()) {
									exprListToExtraPairs.add(new Pair<>(funcInvoke.expressionListOrigin, addParams));
								}

								setHadMadeRepoints();
							}
						}
					}
				}
				/*else if(loc instanceof LocationLocalVar){
					LocationLocalVar lambdaInstance = (LocationLocalVar)loc;
					int g=9;
				}*/
			}
			
			// super.visit(funcInvoke);
			return x;
		}
		
		@Override
		public Object visit(FuncRef funcRef) {
			//FMF: for i have sinned....
			Expression functo = funcRef.functo;
			//if(functo instanceof RefName){//TODO: check that only overriting the following examples is sufficient... what else could the functo be?
				/*
				 * fun inner(y int) int {
						return x+7 + y + somestat + hh
					}
					return inner&(12)
				 */
				//RefName refName = (RefName)functo;
				//if(null != funcRef.typeOperatedOn){
					Location loc = funcRef.typeOperatedOn == null?null:funcRef.typeOperatedOn.getLocation();
					//if(loc instanceof FuncLocation){
						//FuncLocation fl = (FuncLocation)loc;
						if(null != loc){
							FuncDef df = loc.getTaggedFuncDef();
							FuncParams classRefParams = null;
							if(df==null){
								Type opontype = funcRef.typeOperatedOn.getType();
								if(opontype instanceof FuncType){
									df = ((FuncType)opontype).origonatingFuncDef;
								}
								else if (opontype instanceof NamedType){//named type for MYClass& where MYClass is a local class
									NamedType asNamed = (NamedType)opontype;
									ClassDef cds = asNamed.getSetClassDef();
									if(null != cds && cds.isLocalClass && !asNamed.getAllConstructors(null).isEmpty()){
										classRefParams = asNamed.getAllConstructors(null).get(0).origonatingFuncDef.params;
									}
								}
								
							}
							
							if(null != df && null != df.funcName)
							{
								if(!funcRef.methodName.equals(df.funcName)){
									
									if(!df.funcName.equals("<init>") && functo instanceof RefName){
										funcRef.methodName = df.funcName;
										((RefName)functo).name = df.funcName;
									}
									//add extra args, if there are any
									ArrayList<FuncParam> extrafp = df.params.params;
									for(int n = funcRef.argsForNextCompCycle.exprOrTypeArgsList.size(); n < extrafp.size(); n++  ){
										FuncParam toAdd = extrafp.get(n);
										funcRef.argsForNextCompCycle.exprOrTypeArgsList.add(createVariable(funcRef.getLine(), funcRef.getColumn(), toAdd.name, toAdd.getTaggedType()));
									}
									
									FuncRefArgs preOone = funcRef.getArgsForScopeAndTypeCheck();
																		
									if(null != preOone) {//JPT: can this ever actually be null?
										for(FuncParam toAdd : extrafp){
											if(toAdd.name.contains("$n")){
												preOone.exprOrTypeArgsList.add(new RefName(funcRef.getLine(), funcRef.getColumn(),toAdd.name));
												if(null != extraVarsWithinForOnChange && !extraVarsWithinForOnChange.containsKey(toAdd.name)){
													extraVarsWithinForOnChange.put(toAdd.name, toAdd);
												}
											}
										}
										setHadMadeRepoints();
									}
									
									
								}
							}
						}
						//}
					//}
				//}
			//}
			
			return super.visit(funcRef);
		}
		
		private HashMap<String, FuncParam> extraVarsWithinForOnChange = null;
		
		@Override
		public Object visit(OnChange onchange) {
			FuncDefIInOutWrapper inoutWrapper = new FuncDefIInOutWrapper();
			//inoutWrapper.enter(false);
			inoutWrapper.enter();
			
			HashMap<String, FuncParam> prev = extraVarsWithinForOnChange;
			extraVarsWithinForOnChange = new HashMap<String, FuncParam>();
			super.visit(onchange);
			//we call this in second phase because arguments may have been added to ref call which need to be added into calls for init and apply functions
			if(null!= onchange.toSpliceIn && !onchange.toSpliceIn.isEmpty()){
				
				//System.err.println(onchange.getLine());
				String currentSOClass =  onchange.getFullnameSO();
				LocalVarSplicerForOnChange lvs = new LocalVarSplicerForOnChange(onchange.toSpliceIn, onchange.namesOverridedInInit,currentSOClass, null);
				lvs.visit(onchange.initMethodNameFuncDef);
				lvs.visit(onchange.applyMethodFuncDef);
				lvs.visit(onchange.cleanUpMethodFuncDef);
					
				for(String n : onchange.toSpliceIn){
					if(extraVarsWithinForOnChange.containsKey(n)){
						extraVarsWithinForOnChange.remove(n);
					}
				}
				
				for(FuncParam fp : extraVarsWithinForOnChange.values()){
					((ClassDefStateObject)onchange.stateObjectClassDef).addSpliceName(fp);
					onchange.funcParams.add(fp);
				}
				
				if(!extraVarsWithinForOnChange.isEmpty()){
					Set<String> toSpliceIn = extraVarsWithinForOnChange.keySet();
					lvs = new LocalVarSplicerForOnChange(toSpliceIn, onchange.namesOverridedInInit, currentSOClass, null);
					lvs.visit(onchange.initMethodNameFuncDef);
					lvs.visit(onchange.applyMethodFuncDef);
					lvs.visit(onchange.cleanUpMethodFuncDef);
				}
			}
			extraVarsWithinForOnChange = prev;
			inoutWrapper.exit();
			return null;
		}
		
		
		@Override
		public Object visit(AsyncBodyBlock asyncBodyBlock) {
			super.visit(asyncBodyBlock);
			
			Map<String, String> overwriteRetTypeName = null;
			
			Set<String> tosplicein = asyncBodyBlock.toSpliceIn;
			
			HashSet<String> alwaysSpliceInEvenIfLocal = asyncBodyBlock.namesOverridedInInit;
			
			if(null != asyncBodyBlock.preBlockVars && !asyncBodyBlock.preBlockVars.isEmpty()){//vars declared in pre block
				Set<String> kvars = asyncBodyBlock.preBlockVars.keySet();
				
				tosplicein = new HashSet<String>(tosplicein);
				tosplicein.addAll(kvars);
				
				alwaysSpliceInEvenIfLocal = new HashSet<String>();//easy way to copy
				alwaysSpliceInEvenIfLocal.addAll(kvars);
			}
			
			
			if(!tosplicein.isEmpty()){
				String currentSOClass =  asyncBodyBlock.getFullnameSO();
				LocalVarSplicerForOnChange lvs = new LocalVarSplicerForOnChange(tosplicein, alwaysSpliceInEvenIfLocal, currentSOClass, overwriteRetTypeName);
				lvs.redirectNewToExstingForPreBlockDeclVars=true;
				lvs.visit(asyncBodyBlock.initMethodNameFuncDef);
				lvs.redirectNewToExstingForPreBlockDeclVars=false;
				
				for(LineHolder lh : asyncBodyBlock.mainBody.lines){
					Line lino = lh.l;
					if(lino instanceof OnChange){
						lvs.visit(((OnChange) lino).initMethodNameFuncDef);//just in case someone references the explicit return variable inside the onchange expr args
						lvs.visit(((OnChange) lino).applyMethodFuncDef);
					}
				}
				
				
				lvs.visit(asyncBodyBlock.cleanUpMethodFuncDef);
			}
			
			return null;
		}
	}
	
	
	
	private static class LocalVarSplicerForOnChange extends AbstractVisitor{
		private  Set<String> toSpliceIn;
		private  Set<String> alwaysSpliceEvenWhenLocallyDefined;
		private  final String currentSOClass;
		private final Map<String, String> overwriteVars;
		private boolean redirectNewToExstingForPreBlockDeclVars=false;//because we dont have the bytecode output logic to handle new being redirected
		
		public LocalVarSplicerForOnChange(final Set<String> toSpliceIn, final Set<String> alwaysSpliceEvenWhenLocallyDefined, final String currentSOClass, final Map<String, String> overwriteVars){
			this.toSpliceIn = toSpliceIn;
			this.alwaysSpliceEvenWhenLocallyDefined = alwaysSpliceEvenWhenLocallyDefined;
			this.currentSOClass = currentSOClass;
			mylocalVarStack.push(new HashSet<String>());
			this.overwriteVars=overwriteVars;
			
		}
		
		private String getNameToOverwriteWith(String varname){
			//usually normal name unless return variable: xxx=async xfs ... onchange(){ xfs = 9} //xfs gets remapped
			if(null != overwriteVars){
				String ret = overwriteVars.get(varname);
				if(ret == null){
					return varname;
				}
				return ret;
			}
			return varname;
		}
		
		private boolean somethingToSplice(String theName){
			return toSpliceIn.contains(theName) && (!isVarDefinedLocally(theName) || alwaysSpliceEvenWhenLocallyDefined.contains(theName));
		}
		
		@Override
		public Object visit(RefName refName) {
			String theName = refName.name;
			
			if(refName.isIsolated && somethingToSplice(theName) ){
				Location loc = null==refName.resolvesTo?null:refName.resolvesTo.getLocation();
				if(loc != null && !(loc instanceof LocationLocalVar)){
					return null;
				}
				
				int line = refName.getLine();
				int col = refName.getColumn();
				refName.astRedirectforOnChangeNesting = new DotOperator(line, col, new RefName(line, col, "stateObject$"), new RefName(line, col, getNameToOverwriteWith(theName))); //x-> stateObject.x //hack yuck
				//System.err.println("out: " + refName.name + " " + refName.getLine());
			}
			/*else{
				System.err.println("out: " + refName.name);
			}*/
			return null;
		}
		//for nested inner functions which have captured external local variables
		
		
		@Override
		public Object visit(AsyncBlock ab) {
			for(FuncParam fp : ab.getExtraCapturedLocalVars().params) {
				if(somethingToSplice(fp.name)) {
					fp.fromSOname = currentSOClass;
				}
			}
			

			if(ab.executor != null) {
				ab.executor.accept(this);
			}
			
			return null;//super.visit(ab);
		}
		
		
		@Override
		public Object visit(OnChange onChange){
			//we may have nested bindings within this fella on its initialization argumens which may need to be spliced in from higher up
			//e.g. res3 = onchange(a=xs){ onchange(a) { xs+6; } }
			if(null != onChange.toSpliceIn){
				for(String name : onChange.toSpliceIn){
					if(toSpliceIn.contains(name)){
						onChange.takeArgFromSO.put(name, currentSOClass);
					}
				}
			}
			
			return super.visit(onChange);
		}
		
		@Override
		public Object visit(AsyncBodyBlock asyncbb){
			//Copy paste from above
			if(null != asyncbb.toSpliceIn){
				for(String name : asyncbb.toSpliceIn){
					if(toSpliceIn.contains(name)){
						asyncbb.takeArgFromSO.put(name, currentSOClass);
					}
				}
			}
			
			return super.visit(asyncbb);
		}
		
		
		@Override
		public Object visit(FuncInvoke funcInvoke) {
			super.visit(funcInvoke);
			
	
			if(funcInvoke.resolvedFuncTypeAndLocation!=null && funcInvoke.astRedirectforOnChangeNesting ==null && funcInvoke.resolvedFuncTypeAndLocation.getLocation() instanceof LocationLocalVar && toSpliceIn.contains(funcInvoke.funName) && !isVarDefinedLocally(funcInvoke.funName)){
				//perform redirection
				int line = funcInvoke.getLine();
				int col = funcInvoke.getColumn();
				funcInvoke.astRedirectforOnChangeNesting = new DotOperator(line, col, new RefName(line, col, "stateObject$"), (FuncInvoke)funcInvoke.copy());
			}
			
			return null;
		}		
		
		@Override
		public Object visit(FuncRef funcRef) {
			//e.g. d2 = abb&()
			if(null != funcRef.argsForNextCompCycle){
				funcRef.argsForNextCompCycle.accept(this);
			}
			
			if(null != funcRef.args){
				funcRef.args.accept(this);
			}
			
			return null;
		}
		
		
		/////////////////////
		//JPT: below code should really be refactored as a mixin
		private Stack<HashSet<String>> mylocalVarStack = new Stack<HashSet<String>>();
		
		//local variable definition
		@Override
		public Object visit(FuncParam funcParam) {
			this.mylocalVarStack.peek().add(funcParam.name);
			
			return super.visit(funcParam);
		}
		
		@Override
		public Object visit(ForBlock forBlock){
			
			this.mylocalVarStack.peek().add(forBlock.localVarName);
			
			return super.visit(forBlock);
		}
		
		@Override
		public Object visit(ForBlockOld forBlock){
			if(null != forBlock.assignName){
				this.mylocalVarStack.peek().add(forBlock.assignName);
			}
			
			return super.visit(forBlock);
		}
		
		@Override
		public Object visit(AssignNew assignNew) {
			//defo new
			this.mylocalVarStack.peek().add( assignNew.name);
			return super.visit(assignNew);
		}

		@Override
		public Object visit(AssignExisting assignExisting) {
			if( assignExisting.isReallyNew && assignExisting.assignee instanceof RefName){
				String name = ((RefName)assignExisting.assignee).name;
				this.mylocalVarStack.peek().add(name);
			}
			return super.visit(assignExisting);
		}
		
		@Override
		public Object visit(Block block){
			mylocalVarStack.push(new HashSet<String>());

			LineHolder lh = block.startItr();
			
			while(lh != null){
				if(this.redirectNewToExstingForPreBlockDeclVars && lh.l instanceof AssignNew){
					AssignNew asNew = (AssignNew)lh.l;
					if(somethingToSplice(asNew.name)){//replace moi with assign existing which can be processed correctly at bytecode gen stage | unless no rhs as is really a new: 'name Type' style...
						lh.l = asNew.expr==null?asNew:new AssignExisting(asNew.getLine(), asNew.getColumn(), new RefName(asNew.getLine(), asNew.getColumn(), asNew.name), AssignStyleEnum.EQUALS, asNew.expr);
					}
				}
				
				lh.accept(this);
				lh = block.getNext();
			}
			
			
			mylocalVarStack.pop();
			
			return null;
		}

		boolean topLevelFuncDef = true;
		
		@Override
		public Object visit(FuncDef fd){
			mylocalVarStack.push(new HashSet<String>());
			boolean prev = topLevelFuncDef;
			if(prev){//only process top level
				topLevelFuncDef=false;
				super.visit(fd);
				topLevelFuncDef = prev;
			}
			
			mylocalVarStack.pop();
			return null;
		}
		
		
		@Override
		public Object visit(LambdaDef fd){
			//localVarStack.push(new HashSet<String>());
			//super.visit(fd);
			
			if(null != fd.fakeFuncRef){
				//fd.fakeFuncRef.extraArgsForLambdaConst
				for(Expression expr : fd.fakeFuncRef.getArgsAndLambdaConsts()){
					expr.accept(this);
				}
				//int j=9;
				//fd.extraArgsForLambdaConst
			}
			
			/*FuncParams extras = fd.getExtraCapturedLocalVars();
			for(FuncParam e : extras.params){
				e.accept(this);
			}*/
			
			//localVarStack.pop();
			return null;
		}
		
		
		private boolean isVarDefinedLocally(String name){
			int sz = mylocalVarStack.size();
			for (int i = sz - 1; i >= 0; i--) {
				if(mylocalVarStack.get(i).contains(name)){
					return true;
				}
			 }
			
			return false;//must be static or otherwise imported etc 
		}
	}
	
	private final Step2AddExtraCapturedVarsEtc step2;
	
	private final String patheNAme;
	
	public NestedFuncRepoint(String fullPathFileName) {
		super(fullPathFileName);
		this.patheNAme = fullPathFileName;
		step2 = new Step2AddExtraCapturedVarsEtc(fullPathFileName);
	}

	private void setHadMadeRepoints() {
		this.hadMadeRepoints=true;
	}
	
	private boolean hadMadeRepoints = false;

	public boolean hadMadeRepoints() {
		return this.hadMadeRepoints;
	}

	public void resetRepoints() {
		this.hadMadeRepoints = false;
	}
	
	public void doNestedRepoint(Block input){
		renamedNestedFuncId=0;
		//this.nestedLevel++;
		//this.nestedLevel++;
		super.visit(input);
		if(hadMadeRepoints){//now second repoint of the funcinvokes...
			this.step2.visit(input);
		}
		//this.nestedLevel--;
		//this.nestedLevel--;
	}
	

	
	//private boolean inClass = false;
/*	@Override
	public Object visit(ClassDef classDef) {
		boolean prev = inClass;
		inClass = true;
		super.visit(classDef);
		inClass = prev;
		return null;
	}*/

	private boolean inFunction = false;
	//private boolean isFuncDefOrLambda = true;
	private boolean inNestedFunction = false;
	
	private Stack<LinkedHashMap<String, FuncParam>> externalParamsReferencedInBlock = new Stack<LinkedHashMap<String, FuncParam>>();
	private Stack<BrandedStack<HashMap<String, Pair<Boolean, FuncParam>>>> localVarStack = new Stack<BrandedStack<HashMap<String, Pair<Boolean, FuncParam>>>>();//first level -> fucntion, lambda, class etc, 2nd level nestings within said function e.g. fun x(){ if(true){s=10}} //a goes to 2nd level
	
	private int renamedNestedFuncId = 0;

	private Stack<HashMap<String, LinkedHashMap<String, FuncParam>>> netedFuncToAddedArgs = new Stack<HashMap<String, LinkedHashMap<String, FuncParam>>>();
	
	private static class BrandedStack<X> extends Stack<X>{
		public enum Type{LOCAL_CLASS, DEF, RHS_BLOCK;}
		private final Type type;
		public BrandedStack(Type type){
			this.type=type;
		}
		
		@Override public String toString(){
			return /*(isLocalClass?"class:":"def:")*/ type + super.toString();
		}
	}
	
	private class FuncDefIInOutWrapper{
		//TODO: this class seems to serve no purpose, remove it? - and see what breaks
		private boolean prevNestedFunction;
		private boolean prevInFunction;
		
		public void enter(){
			prevNestedFunction = inNestedFunction;
			if(inFunction){
				inNestedFunction = true;
			}
			
			prevInFunction = inFunction;
			inFunction = true;
			externalParamsReferencedInBlock.add(new LinkedHashMap<String, FuncParam>());
			localVarStack.add(new BrandedStack<HashMap<String, Pair<Boolean, FuncParam>>>(BrandedStack.Type.DEF));
			localVarStack.peek().add(new HashMap<String, Pair<Boolean, FuncParam>>());//initial layer
			netedFuncToAddedArgs.add(new HashMap<String, LinkedHashMap<String, FuncParam>>());
			nestedLevel++;
			//isFuncDefOrLambda = isFuncDefOrLambdaa;
		}
		
		public void exit(){
			inFunction = prevInFunction;
			inNestedFunction = prevNestedFunction;
			//isFuncDefOrLambda = previsFuncDefOrLambda;
			externalParamsReferencedInBlock.pop();
			localVarStack.pop();
			netedFuncToAddedArgs.pop();
			nestedLevel--;
		}
		
	}

	
	private class LocalClassDefInOutWrapper{
		private boolean prev_inFunction;
		private boolean prev_inNestedFunction;
		
		public void enter(){
			prev_inFunction = inFunction;
			prev_inNestedFunction = inNestedFunction;
			

			externalParamsReferencedInBlock.add(new LinkedHashMap<String, FuncParam>());
			localVarStack.add(new BrandedStack<HashMap<String, Pair<Boolean, FuncParam>>>(BrandedStack.Type.LOCAL_CLASS));
			localVarStack.peek().add(new HashMap<String, Pair<Boolean, FuncParam>>());//initial layer
			netedFuncToAddedArgs.add(new HashMap<String, LinkedHashMap<String, FuncParam>>());
			nestedLevel++;

									
			inFunction = false;
			inNestedFunction=true;
		}
		
		public void exit(){
			inFunction=prev_inFunction;
			inNestedFunction=prev_inNestedFunction;
			

			externalParamsReferencedInBlock.pop();
			localVarStack.pop();
			netedFuncToAddedArgs.pop();
			nestedLevel--;
			
		}
	}
	
	private class InAsyncBlockInOutWrapper{
		
		public void enter(){
			externalParamsReferencedInBlock.add(new LinkedHashMap<String, FuncParam>());
			localVarStack.add(new BrandedStack<HashMap<String, Pair<Boolean, FuncParam>>>(BrandedStack.Type.LOCAL_CLASS));
			localVarStack.peek().add(new HashMap<String, Pair<Boolean, FuncParam>>());//initial layer
			netedFuncToAddedArgs.add(new HashMap<String, LinkedHashMap<String, FuncParam>>());
			nestedLevel++;

									
		}
		
		public void exit(){
			externalParamsReferencedInBlock.pop();
			localVarStack.pop();
			netedFuncToAddedArgs.pop();
			nestedLevel--;
			
		}
	}
	
	
	
	//cleanup function, unrigstierAll
	
	private class RepointBreakContinueReturnInApplyMethod extends OnChangeNestedVis implements IgnoreASTRepointForReturn{
		
		private boolean isAwait;
		//private Type onChangeReturnType;
		private int asyncIndex;
		
		public RepointBreakContinueReturnInApplyMethod(boolean isAwait, Type onChangeReturnType, int asyncIndex){
			this.isAwait=isAwait;
			//this.onChangeReturnType=onChangeReturnType;
			this.asyncIndex=asyncIndex;
		}
		
		@Override
		public Object visit(BreakStatement breakStatement) {
			if(breakStatement.getIsAsyncEarlyReturn()){
				int line = breakStatement.getLine();
				int col = breakStatement.getColumn();
				breakStatement.astRepoint = new Block(line, col);//just a stub
				breakStatement.astRepoint.canContainAReturnStmt  =true;
				breakStatement.astRepoint.isolated=true;
				//breakStatement.astRepoint.isolated=true;
				
				if(null != breakStatement.returns){
					if(this.isAwait){
						addReturnForAwait(breakStatement.astRepoint, line, col, breakStatement.returns);
					}
					else{
						AssignExisting ae = new AssignExisting(line, col, "ret$", AssignStyleEnum.EQUALS, breakStatement.returns);
						//ae.refCnt=TypeCheckUtils.getRefLevels(onChangeReturnType);//ensure that a:= {thing}// since a gets passed in as a ref:://i.e. we are creating a new one to which we wish the result to be set into for this: res = {12:}!
						breakStatement.astRepoint.add(new LineHolder(line, col, ae ));//ret = { a + 4; };
					}
				}
				
				if(!this.isAwait){
					//is more to subscribe to etc
					if(asyncIndex>-1){
						//halt subsequent execution after processing break
						OnChangeMethodCodeGen.addPostReturnToBlock(breakStatement.astRepoint, breakStatement.getLine(), breakStatement.getColumn(), this.isAwait, true, patheNAme, asyncIndex, "false");
					}
					else{
						OnChangeMethodCodeGen.addPostReturnToBlock(breakStatement.astRepoint, breakStatement.getLine(), breakStatement.getColumn(), this.isAwait, true, patheNAme, asyncIndex);
					}
				}
			}
			
			return null;
		}
		
		@Override
		public Object visit(ContinueStatement continuetatement) {
			//JPT: copy past of above, naughtly!
			if(continuetatement.getIsAsyncEarlyReturn()){
				int line = continuetatement.getLine();
				int col = continuetatement.getColumn();
				continuetatement.astRepoint = new Block(line, col);//just a stub
				continuetatement.astRepoint.canContainAReturnStmt  =true;
				continuetatement.astRepoint.isolated=true;
				//breakStatement.astRepoint.isolated=true;
				
				if(null != continuetatement.returns){
					
					if(this.isAwait){
						addReturnForAwait(continuetatement.astRepoint, line, col, continuetatement.returns);
					}
					else{
						AssignExisting ae = new AssignExisting(line, col, "ret$", AssignStyleEnum.EQUALS, continuetatement.returns);
						//ae.refCnt=TypeCheckUtils.getRefLevels(onChangeReturnType);//ensure that a:= {thing}// since a gets passed in as a ref:://i.e. we are creating a new one to which we wish the result to be set into for this: res = {12:}!
						continuetatement.astRepoint.add(new LineHolder(line, col, ae ));//ret = { a + 4; };
					}
				}
				if(!this.isAwait){
					OnChangeMethodCodeGen.addPostReturnToBlock(continuetatement.astRepoint, continuetatement.getLine(), continuetatement.getColumn(), this.isAwait, false, patheNAme, asyncIndex);
				}
			}
			
			return null;
		}
		
		private void addReturnForAwait(Block astRepoint, int line, int col, Expression e ){
			astRepoint.add(new LineHolder(line, col, new AssignExisting(line, col, "$wedoneYetx", AssignStyleEnum.EQUALS, e) ));//ret = { a + 4; };
			astRepoint.add(new LineHolder(line, col, Utils.parseBlock("if($wedoneYetx) { stateObject$.$regSet.unregisterAll(); }", patheNAme, line, false)));
			astRepoint.add(new LineHolder(line, col, Utils.parseReturnStatement("return { $wedoneYetx;}", patheNAme, line)));
		}
		
		@Override
		public Object visit(ReturnStatement returnStatement) {
			//JPT: copy past of above, naughtly!
			//if(returnStatement.getIsAsyncEarlyReturn()){
				int line = returnStatement.getLine();
				int col = returnStatement.getColumn();
				returnStatement.astRepoint = new Block(line, col);//just a stub
				returnStatement.astRepoint.canContainAReturnStmt  =true;
				returnStatement.astRepoint.isolated=true;
				//breakStatement.astRepoint.isolated=true;
				if(this.isAwait && null!= returnStatement.ret){//if ret is null then this is defunct anyway
					addReturnForAwait(returnStatement.astRepoint, line, col, returnStatement.ret);
				}
				else{
					if(null != returnStatement.ret){
						returnStatement.astRepoint.add(new LineHolder(line, col, new AssignExisting(line, col, "ret$", AssignStyleEnum.EQUALS, returnStatement.ret) ));//ret = { a + 4; };
					}
					if(asyncIndex == -2){
						//do nothing
					}
					else if(asyncIndex > -1){
						//unregister at parent level so as to supress further executions and leave block entierly since nothing more todo
						returnStatement.astRepoint.add(new LineHolder(line, col, Utils.parseReturnStatement("return {stateObject$.$regSetParent.unregisterAll(); false;} ", patheNAme, line)));
					}
					else{
						returnStatement.astRepoint.add(new LineHolder(line, col, Utils.parseReturnStatement("return {stateObject$.$regSet.unregisterAll(); false;} ", patheNAme, line)));
					}
				}
			//}
			
			return null;
		}
		
		
	}
	
	private void tweakToNestedFuncDef(FuncDef meth, Set<String> toSpliceIn){
		meth.alreadyNested = true;
		meth.isNestedFunc = true;
	}
	
	public static class ClassDefStateObject extends ClassDef{

		public HashMap<String, Type> spliceToName = new HashMap<String, Type>();
		private String stateObjClsName;
		
		protected ClassDefStateObject(int line, int col, FuncParams funcParams, String stateObjClsName) {
			super(line, col);
			
			for(FuncParam fp : funcParams.params){
				spliceToName.put(fp.name, fp.getTaggedType());
			}
			this.stateObjClsName = stateObjClsName;
		}
		protected ClassDefStateObject(int line, int col, FuncParams funcParams, String stateObjClsName, HashMap<String, Type> preBlockVars) {
			this(line, col, funcParams, stateObjClsName);
			if(null != preBlockVars){
				spliceToName.putAll(preBlockVars);
			}
		}
		
		public void addSpliceName(FuncParam fp){
			spliceToName.put(fp.name, fp.getTaggedType());
		}
		
		@Override
		public String bcFullName()
		{
			return this.stateObjClsName.replace('.','/');//TODO: check this on compiled inner classes
		}
		
		//private final static Type Const_PRIM_LONG = new PrimativeType(PrimativeTypeEnum.LONG);
		//private final static Type Const_OBJ =  new NamedType(new ClassDefJava(java.lang.Object.class));
		
		@Override
		public TypeAndLocation getVariable(TheScopeFrame notused, String name, boolean searchParent, boolean ignoreAutoGennerated)
		{
			Type obtained = spliceToName.get(name);
			
			if(obtained==null){
				/*if(name.equals("$refCount")){
					obtained =  Const_PRIM_LONG;
				}
				else*/ if(name.equals("$regSet")){
					obtained =  OnChangeAwaitBCGennerator.Const_RegSetType;
				}
				else if(name.equals("$regSetParent") ){
					obtained =  OnChangeAwaitBCGennerator.Const_RegSetTypeParent;
				}
				else if(name.startsWith("refStateTracker$") ){//0, 1, 2 etc
					obtained =  OnChangeAwaitBCGennerator.Const_RefStateTracker;
				}
				else if(name.startsWith("$regSet") ){//0, 1, 2 etc
					obtained =  OnChangeAwaitBCGennerator.Const_RegSetTypeAsyncChild;
				}
				else{
					//System.err.println("done: " +name);
					return null;//= Const_OBJ;//fallback. JPT: flag as error
				}
			}
			
			return  new TypeAndLocation(obtained, new LocationClassField(stateObjClsName, null));
		}
		
		@Override
		public HashSet<TypeAndLocation> getFuncDef(String name, boolean searchSuperClass, boolean ignoreLambdas, boolean extfunc)
		{
		/*	if(name.equals("shouldProcess")) {
				return stateObjShouldProcess;
			}*/
			
			Type obtained = spliceToName.get(name);
			if(obtained==null){
				obtained =  new NamedType(new ClassDefJava(java.lang.Object.class));//fallback
			}
			HashSet<TypeAndLocation> ret = new HashSet<TypeAndLocation>();
			ret.add(new TypeAndLocation(obtained, new LocationClassField(stateObjClsName, null)));
			
			return ret;
		}
	}
	
	private boolean blockHasDefoReturnedInNormalWay(Block onchangeBlock, Type retType){
		
		/*res = onchange(xs){
			if(xs==6){
				break 10
			}
			return 8
		}
		//defoo returns, so we dont want to translate this into ret$ = block// form
		*/
		
		Block onChangeBlockCopy = (Block)onchangeBlock.copy();
		ReturnVisitor rv = new ReturnVisitor("", true);
		rv.checkFuncOrLambda(retType, onChangeBlockCopy, 0,0, true, onChangeBlockCopy);//unfortunatly the ReturnVisitor is distructive, so we use this mechanism to determine if the onchange block defo resolves to a conventional set of return statement
		
		return onChangeBlockCopy.hasDefoReturned;
		
	}
	

	private int nestedLevel = 0;//nested level is used to track location of initial declaration of variable which may end up as funcparam
	//e.g. xs=10; fun getXS() = xs; fun moi() = getXS() //xs is going to need to be spliced in

	
	@Override
	public Object visit(Block block) {
		if((localVarStack!=null && !localVarStack.isEmpty() && !block.isClass)){
			localVarStack.peek().push(new HashMap<String, Pair<Boolean, FuncParam>>() );
			boolean prevNested = inNestedFunction;
			boolean previnFunction = inFunction;
			if(block.isClassFieldBlock){
				netedFuncToAddedArgs.add(new HashMap<String, LinkedHashMap<String, FuncParam>>());
				this.inNestedFunction=true;
				inFunction=true;
				
				this.nestedLevel++;
				localVarStack.add(new BrandedStack<HashMap<String, Pair<Boolean, FuncParam>>>(BrandedStack.Type.RHS_BLOCK));
				localVarStack.peek().add(new HashMap<String, Pair<Boolean, FuncParam>>());//initial layer

				externalParamsReferencedInBlock.add(new LinkedHashMap<String, FuncParam>());
			}
			//this.nestedLevel++;
			//localVarStack.add(new HashMap<String, FuncParam>());
			super.visit(block);
			
			if(block.isClassFieldBlock){
				netedFuncToAddedArgs.pop();
				this.inNestedFunction=prevNested;
				inFunction = previnFunction;

				localVarStack.pop();
				externalParamsReferencedInBlock.pop();
				this.nestedLevel--;
			}
			
			//localVarStack.pop();
			//this.nestedLevel--;
			localVarStack.peek().pop();
		}else if(block.canNestModuleLevelFuncDefs) {
			FuncDefIInOutWrapper warpper = new FuncDefIInOutWrapper();
			warpper.enter();
			super.visit(block);
			warpper.exit();
		}else{
			super.visit(block);
		}
		
		
		return null;
	}
	/*
	@Override
	public Object visit(ClassDef classDef) {
		//FuncDefIInOutWrapper inoutWrapper = new FuncDefIInOutWrapper();
		//inoutWrapper.enter();
		super.visit(classDef);
		//inoutWrapper.exit();
		return null;
	}*/
	
	public static boolean isDefaultFuncDef(FuncDef df){
		//DefaultParamUncreatable
		for(FuncParam fp : df.params.params){
			if(fp.getTaggedType().equals(ScopeAndTypeChecker.const_defaultParamUncre)){
				return true;
			}
		}
		return false;
	}
	
	private boolean adFuncParamToThisCallOfClass(FuncDef constructor, String paename){
		for(LineHolder lh : constructor.getBody().lines){
			Line lin = lh.l;
			if(lin instanceof DuffAssign){
				DuffAssign da = (DuffAssign)lin;
				if(da.e instanceof ThisConstructorInvoke){
					ThisConstructorInvoke thisCall = (ThisConstructorInvoke)da.e;
					thisCall.args.add(new RefName(paename));
					return true;
				}
			}
		}
		
		return false;
	}
	
	
	@Override
	public Object visit(ClassDef cd){
		//boolean inFunc = this.inFunction;
		if(/*inFunc ||*/ cd.isLocalClass){//it's a local class
			LocalClassDefInOutWrapper localcd = new LocalClassDefInOutWrapper();
			localcd.enter();
			//if(!inFunc){
			//	this.nestedLevel++;
				//externalParamsReferencedInBlock.add(new LinkedHashMap<String, FuncParam>());
			//}
			super.visit(cd);
			//if(!inFunc){
			//	this.nestedLevel--;
			//}
			//if(!inFunc){
				//externalParamsReferencedInBlock.add(new LinkedHashMap<String, FuncParam>());
			//}
			String origName = cd.getClassName();
				
			LinkedHashMap<String, FuncParam> stuffCaptureded =null;
			
			String newName = "NIC$" + renamedNestedFuncId++;
			if(!origName.equals( newName)){
				setHadMadeRepoints();	
				
				if(!cd.classesHavingMeAsTheSuperClass.isEmpty()){
					for(ClassDef cfd : cd.classesHavingMeAsTheSuperClass){
						cfd.superclass = newName;
					}
				}
				
				if(!cd.callingfuncRefsAsRefNames.isEmpty()){
					for(RefName rn : cd.callingfuncRefsAsRefNames){
						rn.name = newName;
					}
				}
			}
			cd.className = newName;
			
			//funcDef.isNestedFunc = true;
			
			stuffCaptureded = externalParamsReferencedInBlock.peek();
			if(!stuffCaptureded.isEmpty()){//oh, has captures, therefore u have to reweit me
				//setHadMadeRepoints();	
				//add to to all constructors and add fields to assign as well?
				int line = cd.getLine();
				int col = cd.getColumn();
				//FuncParams params = funcDef.getParams();
				for(FuncParam pae : stuffCaptureded.values()){
					//add field
					
					FuncParam cop = (FuncParam)pae.copy();
					if(cop.defaultValue != null) {
						Type vtype = cop.defaultValue.getTaggedType();
						cop.type = vtype;
						cop.defaultValue=null;
					}
					
					String paramName = cop.name;
					Type paramType = cop.getTaggedType();
					cop.addAnnotation(new Annotation(0,0, "com.concurnas.lang.SyntheticParam", null, null, new ArrayList<String>()) );
					
					AssignNew an = FunctionGenneratorUtils.addClassVariable(null, null, cd, paramName, null, paramType, line, col, false, AccessModifier.PRIVATE, null, false, false, false, false);
					pae = cop;
					if(null != an){
						an.localClassImportedField=true;//force inclusion
						//add param for all constructors
						for(FuncType con : cd.getAllConstructors()){
							FuncDef fd = con.origonatingFuncDef;
							
							cop = (FuncParam)pae.copy();
							if(cop.defaultValue != null) {
								Type vtype = cop.defaultValue.getTaggedType();
								cop.type = vtype;
								cop.defaultValue=null;
							}
							
							fd.params.add(cop);
							boolean addedToThisCall = adFuncParamToThisCallOfClass(fd, paramName);
							if(!addedToThisCall){
								ArrayList<Boolean> retself = new ArrayList<Boolean>();
								retself.add(false);
								ArrayList<Expression> postDot = new ArrayList<Expression>(); 
								postDot.add( new RefName(line, col, paramName) );
								ArrayList<Boolean> isDirect = new ArrayList<Boolean>();
								isDirect.add(true);
								ArrayList<Boolean> safecall = new ArrayList<Boolean>();
								safecall.add(false);
								ArrayList<Boolean> noNullAssertion = new ArrayList<Boolean>();
								noNullAssertion.add(false);
								AssignExisting ae = new AssignExisting(line, col, new DotOperator(line, col, new RefThis(line, col), postDot, isDirect, retself, safecall, noNullAssertion),  AssignStyleEnum.EQUALS, new RefName(line, col, paramName) );
								ae.refCnt = TypeCheckUtils.getRefLevels(paramType);
								fd.getBody().add(ae);
								setHadMadeRepoints();	
							}
						}
						//add to constructors
						for(New constru : cd.callingConstructors){
							if(null != constru.args){
								constru.args.add(new RefName(paramName));
							}
						}
						//add to super constructor invokations
						if(!cd.localsuperInvokations.isEmpty()){
							for(SuperConstructorInvoke sci : cd.localsuperInvokations){
								sci.args.add(new RefName(paramName));
							}
						}
					}
				}
				//funcDeftoNewName.put(new FuncDefHolder(funcDef), newName);
			}
			
			
			localcd.exit();
			
			if(null != stuffCaptureded && !netedFuncToAddedArgs.isEmpty()){//rewrite func definition
				netedFuncToAddedArgs.peek().put(origName, stuffCaptureded);
			}
			
			
		}else{

			localVarStack.add(new BrandedStack<HashMap<String, Pair<Boolean, FuncParam>>>(BrandedStack.Type.LOCAL_CLASS));
			localVarStack.peek().add(new HashMap<String, Pair<Boolean, FuncParam>>());//initial layer
			
			super.visit(cd);
			
			localVarStack.pop();
		}
		
		return null;
	}
	
	
	@Override
	public Object visit(FuncDef funcDef) {
		
		//applies to any class method nested function OR ANY nested function reference
		//does not apply the static funcrefs which are unnested
		FuncDefIInOutWrapper inoutWrapper = new FuncDefIInOutWrapper();
		//inoutWrapper.enter(true && !funcDef.getMethodName().startsWith("$$"));//$$ hack to filter out methods for onchange init etc
		inoutWrapper.enter();//$$ hack to filter out methods for onchange init etc
		
		if(funcDef.extFunOn != null){
			FuncParam fp =new FuncParam(funcDef.getLine(), funcDef.getColumn(), "this$extFunc", funcDef.extFunOn, false);
			fp.accept(this);
		}
		
		super.visit(funcDef);
		String origName = funcDef.getMethodName();
		LinkedHashMap<String, FuncParam> stuffCaptureded = null;
		
		if((inNestedFunction && !funcDef.alreadyNested && !isDefaultFuncDef(funcDef) && !funcDef.funcName.equals("<init>")) ){//rewrite func definition
			if(!funcDef.definedAtClassLevel){
				String newName = "NIF$" + renamedNestedFuncId++;
				if(!origName.equals( newName)){
					funcDef.setMethodName(newName);
					funcDef.isNestedFunc = true;
					stuffCaptureded = externalParamsReferencedInBlock.peek();
					if(!stuffCaptureded.isEmpty()){//oh, has captures, therefore u have to reweit me
						//map form old name to new name
						FuncParams params = funcDef.getParams();
						for(FuncParam pae : stuffCaptureded.values()){
							FuncParam fp = (FuncParam)pae.copy();
							//lockIfRef(fp.type);
							params.add(fp);
						}
						//funcDeftoNewName.put(new FuncDefHolder(funcDef), newName);
					}
					setHadMadeRepoints();
				}	
			}
			else if(funcDef.definedAtLocalClassLevel && !externalParamsReferencedInBlock.peek().isEmpty()){
				externalParamsReferencedInBlock.get(externalParamsReferencedInBlock.size() - 2).putAll(externalParamsReferencedInBlock.peek());
				
			}
		}
		
		//make sure the stuff captured gets passed up the chain...
		inoutWrapper.exit();
		
		if(null != stuffCaptureded){//rewrite func definition
			netedFuncToAddedArgs.peek().put(origName, stuffCaptureded);
		}
		
		return null;
	}
	
	private static class RemapRefName extends AbstractVisitor{
		private final HashMap<String, String> mmap;
		public RemapRefName(HashMap<String, String> mmap){
			this.mmap=mmap;
		}

		@Override
		public Object visit(RefName refName) {
			String mapTo = mmap.get(refName.name);
			if(null != mapTo){
				refName.name = mapTo;
				if(null != refName.astRedirectforOnChangeNesting){
					refName.astRedirectforOnChangeNesting.accept(this);
				}
			}
			
			return null;
		}
		

		@Override
		public Object visit(AssignNew assignNew) {
			String mapTo = mmap.get(assignNew.name);
			if(null != mapTo){
				assignNew.name = mapTo;
			}
			return null;
		}
	}

	private class AllNonLocallyUsedVars extends AbstractVisitor{
		private HashSet<String> nonLocalVars = new HashSet<String>();
		
		public Stack<HashSet<String>> localdefs;
		
		public void process(OnChange onChange){
			localdefs = new Stack<HashSet<String>>();
			localdefs.push(new HashSet<String>());
			super.visit(onChange);
			localdefs.pop();
		}
		
		public void process(Block block) {
			localdefs = new Stack<HashSet<String>>();
			localdefs.push(new HashSet<String>());
			super.visit(block);
			localdefs.pop();
		}
		
		private boolean isLocallydefined(String name){
			for(int n=localdefs.size()-1; n>=0; n--){//reverse iterate since most likely used in current or higher scope
				if(localdefs.get(n).contains(name)){
					return true;
				}
			}
			return false;
		}
		
		@Override
		public Object visit(RefName refName){
			if(!isLocallydefined(refName.name)){
				nonLocalVars.add(refName.name);
			}
			
			return super.visit(refName);
		}
		
		@Override
		public Object visit(ForBlock forBlock){
			if(!isLocallydefined(forBlock.localVarName)){
				nonLocalVars.add(forBlock.localVarName);
			}
			
			return super.visit(forBlock);
		}
		
		@Override
		public Object visit(ForBlockOld forBlock){
			
			if(null != forBlock.assignName && !isLocallydefined(forBlock.assignName) ){
				nonLocalVars.add(forBlock.assignName);
			}
			
			return super.visit(forBlock);
		}
		
		@Override
		public Object visit(FuncInvoke funcInvoke){
			if(funcInvoke.isReallyLambda && !isLocallydefined(funcInvoke.funName)){
				nonLocalVars.add(funcInvoke.funName);
			}
						
			return super.visit(funcInvoke);
		}
		
		@Override
		public Object visit(AssignNew an){
			localdefs.peek().add(an.name);
			return super.visit(an);
		}
		
		@Override
		public Object visit(FuncParam funcParam) {
			localdefs.peek().add(funcParam.name);
			return super.visit(funcParam);
		}
		
		@Override
		public Object visit(Block block) {
			localdefs.push(new HashSet<String>());
			super.visit(block);
			localdefs.pop();
			return null;
		}
	}
	
	private class AsyncBodyBlockPreBlockVars extends AbstractVisitor{
		private HashMap<String, Type> preBlockVars = new HashMap<String, Type>();
		private int nestlevel = -1;
		
		@Override
		public Object visit(AssignNew assignNew) {
			if(nestlevel <= 1){
				//String name = assignNew.name;
				if(!assignNew.name.contains("$n")){
					assignNew.name += "$n" + (nestedLevel-2);
				}
				
				//if(null == isVarLocalInplaceOtherThanHere(name)){
					//if this is a new declaration inside the pre block, then we make this as being a pre block var
					preBlockVars.put(assignNew.name, assignNew.getTaggedType());//"$n" + (nestedLevel-2)
				//}
			}
			
			return super.visit(assignNew);
		}
		
		@Override
		public Object visit(AssignExisting assignNew) {
			if(nestlevel <= 1 ){
				if(assignNew.assignee instanceof RefName){
					//String name = ((RefName)assignNew.assignee).name;
					if(!((RefName)assignNew.assignee).name.contains("$n")){
						((RefName)assignNew.assignee).name+= "$n" + (nestedLevel-2);
					}
					
					if(assignNew.isReallyNew){
						preBlockVars.put(((RefName)assignNew.assignee).name, assignNew.getTaggedType());//"$n" + (nestedLevel-2)
					}
					//if(null == isVarLocalInplaceOtherThanHere(name)){
						//if this is a new declaration inside the pre block, then we make this as being a pre block var
						//preBlockVars.put(name+ "$n" + (nestedLevel-2), assignNew.getTaggedType());
					//}
				}
			}
			
			return super.visit(assignNew);
		}
		
		@Override
		public Object visit(Block block) {
			nestlevel++;
			super.visit(block);
			nestlevel--;
			return null;
		}
		
		private String overrideVarNameWithLevel(String theName){
			String theNameWithLevel = theName;
			if(!theNameWithLevel.contains("$n")){
				theNameWithLevel = theNameWithLevel + "$n" + (nestedLevel-2);
			}
			
			if(preBlockVars.containsKey(theNameWithLevel)){//if this maps to somehting which has been augmented LOCALLY, then augment to point to this here
				theName = theNameWithLevel;
			}
			
			return theName;
		}
		
		@Override
		public Object visit(RefName refName) {
			refName.name = overrideVarNameWithLevel(refName.name);
			
			return super.visit(refName);
		}
		
		
		@Override
		public Object visit(FuncInvoke funcInvoke) {
			
			if(funcInvoke.isReallyLambda){
				funcInvoke.overrideFuncName(overrideVarNameWithLevel(funcInvoke.funName));
			}
			
			return super.visit(funcInvoke);
			
		}
		
		
	}
	
	private FuncParams processHasExtraCaps(HasExtraCapturedVars hasE, int line, int col, boolean alwaysInNested, FuncParam addForAsyncWriteReturn){
		//applies to any class method nested function OR ANY nested function reference
		//does not apply the static funcrefs
		FuncDefIInOutWrapper inoutWrapper = new FuncDefIInOutWrapper();
		//inoutWrapper.enter( !(hasE instanceof AsyncBlock || hasE instanceof OnChange) );
		inoutWrapper.enter();
		
		HashSet<String> varsDeclInPreBlocks = null;
		
		//rewrite func definition
		if(hasE instanceof AsyncBlock){
			AsyncBlock asa = (AsyncBlock)hasE;
			super.visit(asa.body);//ignore the executor
		}
		else if(hasE instanceof OnChange){
			OnChange onChange = (OnChange)hasE;
			//this.nestedLevel++;
			for(Node e: onChange.exprs){
				if(e instanceof AssignNew){
					AssignNew assignNew = (AssignNew)e;
					Type tt = (Type)assignNew.getTaggedType().copy();
					TypeCheckUtils.unlockAllNestedRefs(tt);
					FuncParam fp = new FuncParam(assignNew.getLine(), assignNew.getColumn(), assignNew.name, tt, false);
					//add one level up so as to trick it into thinking the variable defined in the init is passed in (which of course it is via the state object)
					if(!fp.name.contains("$n")){
						fp.name = fp.name+ "$n" + (nestedLevel-1);
					}
					
					//assignNew.name = fp.name;
					
					localVarStack.get(localVarStack.size()-2).peek().put(assignNew.name, new Pair<Boolean, FuncParam>(true, fp) );
					if(onChange.asyncIndex>-1){//MHA: sneak the trigger var onto the state object for the async
						currAsycnBodBlock.peek().preBlockVars.put(fp.name, fp.getTaggedType());
					}
					
					this.externalParamsReferencedInBlock.peek().put(fp.name, fp);//force always usage/creation of obj on static block regardless of whether refered inside onchange
					
					//localVarStack.get(localVarStack.size()-1).peek().put(assignNew.name, fp );
					
					onChange.namesOverridedInInit.add(assignNew.name);
					
					super.visit(assignNew);
					//localVarStack.get(localVarStack.size()-2).remove(assignNew.name);
					if(!assignNew.name.contains("$n")){
						assignNew.name = fp.name;
					}
					
				}
				else{
					e.accept(this);
				}
			}
			//this.nestedLevel--;
			
			if(null != onChange.applyMethodFuncDef){//when func adding extra level?
				//int prev = levelOffset;
				//levelOffset=1;//MHA: levels seem incorrect here
				//this.nestedLevel--;
				inoutWrapper.exit();
				onChange.applyMethodFuncDef.accept(this);
				onChange.cleanUpMethodFuncDef.accept(this);
				//this.nestedLevel--;
				onChange.initMethodNameFuncDef.accept(this);

				inoutWrapper.enter();
				//this.nestedLevel++;
				//levelOffset=prev;
			}
			else{
				if(null !=onChange.body){
					//this.nestedLevel--;
					onChange.body.accept(this);
					//this.nestedLevel++;
				}
			}	
		}
		else if(hasE instanceof AsyncBodyBlock){

			FuncDefIInOutWrapper inoutWrapper2 = new FuncDefIInOutWrapper();
			inoutWrapper2.enter();
			
			AsyncBodyBlock asHasE = (AsyncBodyBlock)hasE;
			
			varsDeclInPreBlocks = new HashSet<String>();
			
			if(!asHasE.preBlocks.isEmpty()){
				Block theOne = asHasE.preBlocks.get(0);
				theOne.accept(this);
				AsyncBodyBlockPreBlockVars preblockVars = new AsyncBodyBlockPreBlockVars();
				preblockVars.visit(theOne);//here sort of!
				HashMap<String, Type> preBlockVars = preblockVars.preBlockVars;
				
				//filter out those not used within onchange blocks - these dont need to be stored on the state object
				AllNonLocallyUsedVars alnuv = new AllNonLocallyUsedVars();
				for(LineHolder lh : asHasE.mainBody.lines){
					if(lh.l instanceof OnChange){
						OnChange asOnChange = (OnChange)lh.l;
						alnuv.process(asOnChange);
					}
				}
				
				if(!asHasE.postBlocks.isEmpty()){//or used in post bloc... func call also does mapping
					alnuv.process(asHasE.postBlocks.get(0));
				}
				
				HashSet<String> missingUsage = new HashSet<String>();
				for(String inpre : preBlockVars.keySet()){
					//Type typeTp = preBlockVars.get(inpre);
					//if(!TypeCheckUtils.hasRefLevels(typeTp)){//we must keep the refs as they are to be created in the state object init method
						String inprea = inpre;
						if(inprea.contains("$n")){//its one of these nested synthetic things we need to space out
							inprea = inprea.substring(0, inprea.indexOf("$n"));
						}
						if(!alnuv.nonLocalVars.contains(inprea) && !alnuv.nonLocalVars.contains(inpre) ){
							missingUsage.add(inpre);
							missingUsage.add(inprea);
						}
					//}
					
				}
				
				if(!missingUsage.isEmpty()){
					for(String mis : missingUsage){
						preBlockVars.remove(mis);
					}
				}
				//filter done! lol so elaborate wihtout list comprehensions.... H1-B
				
				varsDeclInPreBlocks.addAll(preBlockVars.keySet());
				asHasE.preBlockVars = preBlockVars;
				
				if(!asHasE.preBlockVars.isEmpty()){
					for(String name : asHasE.preBlockVars.keySet()){
						Type type = asHasE.preBlockVars.get(name);
						FuncParam fp = new FuncParam(theOne.getLine(), theOne.getColumn(), name  , type, false, nestedLevel-2);
						this.localVarStack.get(this.localVarStack.size()-3).peek().put( name, new Pair<Boolean, FuncParam>(true, fp) );
					}
					
					for(String name : asHasE.preBlockVars.keySet()){
						Type type = asHasE.preBlockVars.get(name);
						name = name.substring(0, name.lastIndexOf("$n"));
						FuncParam fp = new FuncParam(theOne.getLine(), theOne.getColumn(), name  , type, false, nestedLevel-2);
						this.localVarStack.get(this.localVarStack.size()-3).peek().put( name, new Pair<Boolean, FuncParam>(true, fp) );
					}
					
				}
			}
			
			LinkedHashMap<String, FuncParam> capturedInsideAsync = new LinkedHashMap<String, FuncParam>();
			int duplicateIdRemapper = 0;
			for(LineHolder lh : asHasE.mainBody.lines){
				Line l = lh.l;
				if(l instanceof OnChange){
					OnChange asOnChange = (OnChange)l;
					asOnChange.accept(this);
					FuncParams paramsForOnChange = asOnChange.getExtraCapturedLocalVars();
					HashMap<String, String> remapOfExpVarRequired = new HashMap<String, String>();
					FuncParams remappedParams = new FuncParams(0,0);
					for(FuncParam fp : paramsForOnChange.params){
						if(asOnChange.namesOverridedInInit.contains(fp.name)){
							if(capturedInsideAsync.containsKey(fp.name)){
								FuncParam newfp = fp.copyWithName(fp.name + "$u" + duplicateIdRemapper++);
								remappedParams.add(newfp);
								remapOfExpVarRequired.put(fp.name, newfp.name);
								//place the new fp in here ^ above should be fp
								capturedInsideAsync.put(fp.name, fp);
								asOnChange.toSpliceIn.remove(fp.name);
								asOnChange.toSpliceIn.add(newfp.name);
								
								//((ClassDefStateObject)asOnChange.stateObjectClassDef).spliceToName.remove(fp.name);
								((ClassDefStateObject)asOnChange.stateObjectClassDef).spliceToName.put(newfp.name, newfp.getTaggedType());
								((ClassDefStateObject)asOnChange.stateObjectClassDef).spliceToName.put(fp.name, newfp.getTaggedType());//also map old name
							}
							else{
								capturedInsideAsync.put(fp.name, fp);
								remappedParams.add(fp);
							}
						}
					}
					
					if(!remapOfExpVarRequired.isEmpty()){
						asOnChange.setExtraCapturedLocalVars(remappedParams);
						RemapRefName mapper = new RemapRefName(remapOfExpVarRequired);
						mapper.visit(asOnChange);
					}
					
				}
			}
			

			if(!asHasE.postBlocks.isEmpty()){
				//FuncDefIInOutWrapper inoutWrapper2 = new FuncDefIInOutWrapper();
				
				//inoutWrapper2.enter(false);//use this to force all var access of existing vars from pre block to goto state object
				inoutWrapper2.enter();//use this to force all var access of existing vars from pre block to goto state object
				asHasE.postBlocks.get(0).accept(this);
				
				inoutWrapper2.exit();
			}
			
			inoutWrapper2.exit();
			
		}
		else{
			super.visit((LambdaDef)hasE);
		}
		
		FuncParams lfpnew = null;
		if(alwaysInNested || inNestedFunction){//may need to change lambda invokation definition if external args are captured
			LinkedHashMap<String, FuncParam> stuffCaptureded = externalParamsReferencedInBlock.peek();
			
			FuncParams prevlfp = hasE.getExtraCapturedLocalVars();
			//boolean isFirst = lfp==null || lfp.isEmpty();
			
					
			if(!stuffCaptureded.isEmpty() && (prevlfp==null || !prevlfp.hasParams() || prevlfp.getFirst().name.equals("ret$")) ){//oh, has captures, therefore u have to reweit me, null check to do only once
				lfpnew = new FuncParams(line, col);//bit of a hack with ret here...
				if(prevlfp!=null && prevlfp.hasParams() && prevlfp.getFirst().name.equals("ret$") && !(hasE instanceof AsyncBlock)){//MHA: exclude asyncblock from this wonderful hack, the ret must be explicitly referenced inside the block here and therefore doesnt need to be explicitly added back in
					lfpnew.add(prevlfp.getFirst());
				}
				
				
				for(FuncParam pae : stuffCaptureded.values()){
					FuncParam cc = (FuncParam)pae.copy();
					
					if(null==cc.getTaggedType()){
						//MHA: try again to pick this out... seems to fail with lambdas
						cc.setTaggedType( this.isVarLocalInplaceOtherThanHere(cc.name).getB().getTaggedType() );
					}
					
					lfpnew.add(cc);
				}
				
				
			}
			else if (prevlfp==null){
				lfpnew = new FuncParams(line, col);
			}
			else{
				lfpnew=prevlfp;
			}
			
			if(hasE instanceof AsyncBodyBlock){
				AsyncBodyBlock abb = (AsyncBodyBlock)hasE;
				
				//now remove those in onchange which are defined in pre
				for(String prevar : abb.preBlockVars.keySet()){
					lfpnew.remove(prevar);
				}
				
				//also remove allvars from inside pre block from child onchange as well
				for(LineHolder lh : abb.mainBody.lines){
					Line l = lh.l;
					if(l instanceof OnChange){
						FuncParams oc = ((OnChange) l).getExtraCapturedLocalVars();
						
						for(String prevar : varsDeclInPreBlocks){
							oc.remove(prevar);
						}
					}
				}
				
				
			}
			
			boolean diff = false;
			if(null != prevlfp && prevlfp.params.size() == lfpnew.params.size()){
				int sz = prevlfp.params.size();
				for(int n=0; n < sz; n++){
					if(!prevlfp.params.get(n).equalsCheckName(lfpnew.params.get(n))){
						diff=true;
						break;
					}
				}
			}
			else{
				diff = true;
			}
			
			
			if(null == prevlfp || diff){
				setHadMadeRepoints();
				hasE.setExtraCapturedLocalVars(lfpnew);//JPT: move to below lower statement for elegance?
			}
			
			if(null!= addForAsyncWriteReturn && !(lfpnew.params !=null && !lfpnew.params.isEmpty() && addForAsyncWriteReturn.toString().equals(lfpnew.params.get(lfpnew.params.size()-1).toString()) )){
				//dun add it twice! - bit of a hack with the toString
				lfpnew.add(addForAsyncWriteReturn);
			}
			
		}
		
		inoutWrapper.exit();
		return lfpnew;
	}
	
	@Override
	public Object visit(LambdaDef lambdaDef) {
		processHasExtraCaps(lambdaDef, lambdaDef.getLine(), lambdaDef.getColumn(), false, null);
		if(null != lambdaDef.forceNestFuncRepoint && lambdaDef.forceNestFuncRepoint){
			lambdaDef.forceNestFuncRepoint=false;
			setHadMadeRepoints();//MHA: force addition of 
		}
		
		return null;
	}
	
	@Override
	public Object visit(AsyncBlock asyncBlock) {
		
		FuncParam addForAsyncWriteReturn = null;
		if(!asyncBlock.noReturn ){
			 addForAsyncWriteReturn = new FuncParam(0,0,"ret$", asyncBlock.getTaggedType(), false);
		}
		
		InAsyncBlockInOutWrapper inoutWrapper = null;
		if(!inFunction) {
			inoutWrapper = new InAsyncBlockInOutWrapper();
		}
				
		
		if(null != inoutWrapper) {
			inoutWrapper.enter();
		}
		
		processHasExtraCaps(asyncBlock, asyncBlock.getLine(), asyncBlock.getColumn(), true, addForAsyncWriteReturn);//tag in the type here
		
		if(null != inoutWrapper) {
			inoutWrapper.exit();
		}
		
		if(asyncBlock.fakeLambdaDef !=null){
			asyncBlock.fakeLambdaDef.body.accept(this);
		}
		
		if(asyncBlock.executor != null) {
			asyncBlock.executor.accept(this);
		}
		
		return null;
	}
	
	private Pair<Boolean, FuncParam> definedAtLevel(Stack<HashMap<String, Pair<Boolean, FuncParam>>> inFuncion, String name, boolean allowNonFunctionDefinedVars){
		for(HashMap<String, Pair<Boolean, FuncParam>> sublevel : inFuncion){
			if(name.contains("$n")){
				name = name.substring(0, name.indexOf("$n"));
			}
			
			Pair<Boolean, FuncParam> got = sublevel.get(name);
			
			if(null != got){
				boolean defInFunction = got.getA();
				
				if(!defInFunction && !allowNonFunctionDefinedVars){
					return null;
				}
				
				return got;//.getB();
			}
		}
		return null;
	}
	
	private boolean isVarDefinedLocally(String name){
		int sz = localVarStack.size();
		for (int i = sz - 1; i >= 0; i--) {
			Pair<Boolean, FuncParam> atThisLevel = definedAtLevel(localVarStack.get(i), name, false);
		    if(null != atThisLevel){
		    	return true;
		    }
		 }
		
		return false;
	}
	
	//private int levelOffset = 0;
	
	private Fourple<Integer, FuncParam, Boolean, Boolean> isVarLocalInplaceOtherThanHere(String name){
		if(null==localVarStack || null == localVarStack.peek() || localVarStack.peek().isEmpty() || localVarStack.peek().peek().containsKey(name)){
			return null;
		}
		int levelOffset=0;
		int colourups = levelOffset;
		int sz = localVarStack.size();
		boolean isLocalClass = false;
		//boolean inRHSBlock = false;
		for (int i = sz - (1+levelOffset); i >= 0; i--) {
			BrandedStack<HashMap<String, Pair<Boolean, FuncParam>>> curLevel = localVarStack.get(i);
			boolean thisLevelIsLocalClass = false;
			if(curLevel.type == BrandedStack.Type.LOCAL_CLASS && colourups == 1){//at class level, i.e. redirect to this.a$n1 iff one leve up (i.e. dont do this for nested fellas)
				isLocalClass = curLevel.type == BrandedStack.Type.LOCAL_CLASS;
				thisLevelIsLocalClass = true;
			}
			
			Pair<Boolean, FuncParam> atThisLevel = definedAtLevel(curLevel, name, isLocalClass);
		    if(null != atThisLevel){
		    	
		    	if(thisLevelIsLocalClass) {
		    		return null;//mapped to a local class field, so no mapping needed
		    	}
		    	
				boolean isNestedLocalClass =colourups == 2 && isLocalClass && curLevel.type == BrandedStack.Type.LOCAL_CLASS;
		    	
		    	return new Fourple<Integer, FuncParam, Boolean, Boolean>(colourups, atThisLevel.getB(), !isNestedLocalClass && isLocalClass, atThisLevel.getA());
		    }
		    colourups++;
		 }
		
		return null;//must be static or otherwise imported etc 
	}
	
	public void capturedParam(String name, FuncParam par, int upTo){
		//we populate the fact that the local defined var inside a nestor (i.e. outside of the nested function)
		int sz = externalParamsReferencedInBlock.size();
		
		for (int i = upTo; i > 0; i--) {
			externalParamsReferencedInBlock.get(sz - i).put(name, par);
		}
		
		//next line as covered above?
		//this.externalParamsReferencedInBlock.peek().put(name, par);
	}
	
	@Override
	public Object visit(RefName refName) {
		
		/*if(null != refName.resolvesTo) {
			Location loc = refName.resolvesTo.getLocation();
			if(loc instanceof LocationClassField) {
				LocationClassField ascf = (LocationClassField)loc;
				if(null != ascf.ownerType && ascf.ownerType instanceof NamedType) {
					ClassDef cd = ((NamedType)ascf.ownerType).getSetClassDef();
					if(cd != null && cd.isLocalClass) {
						return null;
					}
				}
			}
		}*/
		
		if(inNestedFunction  && !refName.isPreceededByDotInDotOperator()){
			//check if obtained higher
			String name = refName.name;
			
			if(name.equals("stateObject$")){
				return null;
			}
			if(name.contains(ScopeAndTypeChecker.TypesForActor)){
				return null;
			}
						
			Fourple<Integer, FuncParam, Boolean, Boolean> par = isVarLocalInplaceOtherThanHere(name);
			if(null != par){
				//refName.name = isFuncDefOrLambda?name+ "$n" + par.getA() :refName.name ;
				if(!refName.name.contains("$n") || !par.getD()){
					//int minusXtra = this.currAsycnBodBlock.size() * 2;
					if(par.getD()){//if false indicates that variable is not from a function, e.g. could be from a class in which case we don't need to add the $'s
						refName.name = name + "$n" + (nestedLevel  - par.getA());//(this.nestedLevel-1);//par.getA() ;
					}
					
					
					if(par.getC()){
						int line = refName.getLine();
						int col = refName.getColumn();
						ArrayList<Boolean> retself = new ArrayList<Boolean>();
						retself.add(false);
						ArrayList<Expression> postDot = new ArrayList<Expression>(); 
						postDot.add( new RefName(line, col, refName.name) );
						ArrayList<Boolean> isDirect = new ArrayList<Boolean>();
						isDirect.add(true);
						ArrayList<Boolean> safecall = new ArrayList<Boolean>();
						safecall.add(false);
						ArrayList<Boolean> noNullAssertion = new ArrayList<Boolean>();
						noNullAssertion.add(false);

						refName.astRedirectForAll = new DotOperator(line, col, new RefThis(line, col), postDot, isDirect, retself, safecall, noNullAssertion);
						this.setHadMadeRepoints();
					}
					
				}
				
				FuncParam fp = par.getB().copyWithName(refName.name);//correct nesting level name for param
				fp.sytheticDefinitionLevel = this.nestedLevel - par.getA();
				/*if(!par.getD()){
					fp.nonLocalVariableResolvesTo = new RefName(refName.getLine(), refName.getColumn(), name);
				}*/
				
				capturedParam(refName.name, fp, par.getA());
				//this.externalParamsReferencedInBlock.peek().put(name,par);
				
			}
		}
		return null;
	}
	
	
	@Override
	public Object visit(FuncInvoke funcInvoke) {
		String name = funcInvoke.funName;
		if(inNestedFunction  && !funcInvoke.isPreceededByDotInDotOperator() && null != funcInvoke.resolvedFuncTypeAndLocation && funcInvoke.resolvedFuncTypeAndLocation.getLocation() instanceof LocationLocalVar){
			Fourple<Integer, FuncParam, Boolean, Boolean> par = isVarLocalInplaceOtherThanHere(name);
			if(null != par){
				FuncParam fp = par.getB();
				
				Location loc = funcInvoke.resolvedFuncTypeAndLocation.getLocation();
				if(loc instanceof LocationLocalVar){//locally captured lambda
					LocationLocalVar llv = (LocationLocalVar)loc;
					if(llv.isLambda()){
						if(!name.contains("$n")){
							name = name+ "$n" + (nestedLevel  - par.getA());
						}
						
						fp = fp.copyWithName(name);//correct nesting level name for param
						fp.sytheticDefinitionLevel = this.nestedLevel;
						
						funcInvoke.overrideFuncName(name);
					}
				}
				
				capturedParam(name, fp, par.getA());
				//this.externalParamsReferencedInBlock.peek().put(name,par);
			}
		}
		
		for(int n = this.netedFuncToAddedArgs.size()-1; n >=0; n--){
			LinkedHashMap<String, FuncParam> pars = this.netedFuncToAddedArgs.get(n).get(name);
			if(null != pars){
				for(String vName : pars.keySet()){
					FuncParam par = pars.get(vName);
					String vNameNormal = vName.substring(0, vName.indexOf("$n"));
					int nestingLevelWanted = Integer.parseInt(vName.substring(vName.indexOf("$n")+2));
					
					if(!(isVarDefinedLocally(vNameNormal)  && nestingLevelWanted == this.nestedLevel)){//if the var required is not locally defined then search for it higher up
						//so if locally defined and that definition is at this level wanted, do not include below as an extra var to capture
						this.externalParamsReferencedInBlock.peek().put(vName, par);
					}
				}
				break;
			}
		}
		
		return super.visit(funcInvoke);
	}	
	
	
	//local variable definition
	@Override
	public Object visit(FuncParam fp) {
		if(inFunction){
			addVarToLocalStack(fp.name, true, fp);
		}
		return super.visit(fp);
	}
	
	@Override
	public Object visit(ForBlock forblock) {
		LocalClassDefInOutWrapper inoutWrapper = null;
		
		if(!inFunction) {
			inoutWrapper = new LocalClassDefInOutWrapper();
		}
				
		
		if(null != inoutWrapper) {
			inoutWrapper.enter();
		}
		
		
		//if(inFunction){
		
		
			FuncParam fp = new FuncParam(forblock.getLine(), forblock.getColumn(), forblock.localVarName + "$n" + nestedLevel, forblock.localVarTypeToAssign, false, nestedLevel);
			addVarToLocalStack(forblock.localVarName, true, fp);
			if(null != forblock.idxVariableCreator && null != forblock.idxVariableAssignment) {
				String ifxName = forblock.idxVariableAssignment.name;
				FuncParam fpidx = new FuncParam(forblock.getLine(), forblock.getColumn(), ifxName + "$n" + nestedLevel, forblock.idxVariableCreator.getTaggedType(), false, nestedLevel);
				addVarToLocalStack(ifxName, true, fpidx);
			}
		//}
		
			
			

		//FuncDefIInOutWrapper inoutWrapper = new FuncDefIInOutWrapper();
		//inoutWrapper.enter();
		
		/*
		 * boolean attopLevel = nestedLevel==1; if(attopLevel) { nestedLevel++; }
		 */
		
		Object ret= super.visit(forblock);
		
		/*
		 * if(attopLevel) { nestedLevel--; }
		 */
		
		//inoutWrapper.exit();
		
		if(null != inoutWrapper) {
			inoutWrapper.exit();
		}
		
		return ret;
	}
	
	
	@Override
	public Object visit(WhileBlock whileBlock) {
		if(inFunction){
			if(null != whileBlock.idxVariableCreator && null != whileBlock.idxVariableAssignment) {
				String ifxName = whileBlock.idxVariableAssignment.name;
				FuncParam fpidx = new FuncParam(whileBlock.getLine(), whileBlock.getColumn(), ifxName + "$n" + nestedLevel, whileBlock.idxVariableCreator.getTaggedType(), false, nestedLevel);
				addVarToLocalStack(ifxName, true, fpidx);
			}
		}

		return super.visit(whileBlock);
	}
	
	
	
	@Override
	public Object visit(ForBlockOld forblock) {
		if(inFunction && null != forblock.assignName){
			FuncParam fp = new FuncParam(forblock.getLine(), forblock.getColumn(), forblock.assignName + "$n" + nestedLevel, forblock.resolvedassigType, false, nestedLevel);
			addVarToLocalStack(forblock.assignName, true, fp);
		}
		return super.visit(forblock);
	}
	
	@Override
	public Object visit(AssignNew assignNew) {
		//defo new
		//if(inFunction){
		if(!this.localVarStack.isEmpty()){
			//FuncParam fp = new FuncParam(assignNew.getLine(), assignNew.getColumn(), isFuncDefOrLambda?assignNew.name + "$n" + nestedLevel : assignNew.name, assignNew.getTaggedType(), false, nestedLevel);
			FuncParam fp = new FuncParam(assignNew.getLine(), assignNew.getColumn(), assignNew.name + "$n" + nestedLevel, assignNew.getTaggedType(), false, nestedLevel);
			fp.isShared = assignNew.isShared;
			fp.isLazy = assignNew.isLazy;
			addVarToLocalStack(assignNew.name, inFunction, fp);
		}
		//}
		return super.visit(assignNew);
	}

	@Override
	public Object visit(AssignExisting assignExisting) {
		if(!this.localVarStack.isEmpty() && /*inFunction && */assignExisting.isReallyNew && assignExisting.assignee instanceof RefName){
			String name = ((RefName)assignExisting.assignee).name;
			//FuncParam fp = new FuncParam(assignExisting.getLine(), assignExisting.getColumn(),  isFuncDefOrLambda?name + "$n" + nestedLevel : name, assignExisting.getTaggedType(), false,nestedLevel );
			FuncParam fp = new FuncParam(assignExisting.getLine(), assignExisting.getColumn(),  name + "$n" + nestedLevel, assignExisting.getTaggedType(), false,nestedLevel );
			addVarToLocalStack(name, inFunction, fp);
		}
		return super.visit(assignExisting);
	}
	
	private void addVarToLocalStack(String name, boolean infunction, FuncParam asFp){
		this.localVarStack.peek().peek().put(name, new Pair<Boolean, FuncParam>(inFunction, asFp) );
	}
	
	@Override
	public Object visit(OnChange onchange) {
		onchange.funcParams = processHasExtraCaps(onchange, onchange.getLine(), onchange.getColumn(), true, null);
		//add the init and apply methods to owner class
		
		if(onchange.initMethodNameFuncDef == null){//first time only
			setHadMadeRepoints();
			
			boolean isawait = onchange instanceof Await;
			boolean awaitOrEvery = isawait ||  onchange instanceof OnEvery;
			
			if(onchange.body==null){//dealing with await, add this implicitly
				onchange.body = Utils.parseBlock("{true;}", this.patheNAme, onchange.getLine(), true);
				onchange.body.setTaggedType(ScopeAndTypeChecker.const_boolean);
			}
			
			Block onChangeBlock = onchange.body;
			Type retType = onchange.getTaggedType();

			boolean hasdefoReturnedNormally = blockHasDefoReturnedInNormalWay(onChangeBlock, retType);
			
			if(null != retType && !onchange.noReturn && onchange.getShouldBePresevedOnStack()){
				onchange.funcParams.params.add(new FuncParam(0,0,"ret$", retType, true));
				
				if(!hasdefoReturnedNormally){
					//repoint to assign results to onChangeBlock...
					//onChangeBlock.isolated=true;
					onChangeBlock = Utils.makeAssignToRefFromrhsBlock(onChangeBlock, retType, true); //xxx => ret := {xxx}
				}
				else{
					onChangeBlock = Utils.makeAssignToRefFromrhsBlock(onChangeBlock, retType, false); //xxx => ret := {xxx}
				}
			}
			else if(null != onchange.asyncExplicitReturnVarType){
				onchange.funcParams.params.add(new FuncParam(0,0,"ret$", onchange.asyncExplicitReturnVarType, true));
			}
			
			RepointBreakContinueReturnInApplyMethod rbcram = new RepointBreakContinueReturnInApplyMethod(isawait, onchange.getTaggedType(), onchange.asyncIndex);
			rbcram.visit(onChangeBlock);
			
			Set<String> toSpliceIn = new HashSet<String>();
			for(FuncParam fp : onchange.funcParams.params){
				if(fp.sytheticDefinitionLevel != null && fp.sytheticDefinitionLevel == this.nestedLevel){//if the thingy was defined as a local arg that gets spliced in...
					//fp.name = fp.name.substring(0, fp.name.indexOf("$n"));
				}
				
				toSpliceIn.add(fp.name);
			}

			onchange.toSpliceIn = toSpliceIn;
			if(onchange.asyncIndex==-1){
				onchange.stateObjectClassDef = new ClassDefStateObject(onchange.getLine(), onchange.getColumn(), onchange.funcParams, onchange.getFullnameSO());
			}
			
			FuncDef initMethod = (FuncDef)OnChangeMethodCodeGen.makeOnChangeInitMethod(onchange.exprs, onchange.getFullnameSO(), onchange.initMethodName, onchange.getLine(), onchange.getColumn(), awaitOrEvery, patheNAme, onchange.asyncIndex, onchange.onlyClose).copy();//copy so that refName ast change doesnt infect origonal version
			tweakToNestedFuncDef(initMethod, toSpliceIn);
			onchange.initMethodNameFuncDef = initMethod;
			
			//create the init and apply here, keep note of splices to make
			
			//copy function code over for these - apply splice to stateObject where approperiate
			FuncDef applyMethodFuncDef = (FuncDef)OnChangeMethodCodeGen.makeOnChangeApplyMethod(onChangeBlock, onchange.getFullnameSO(), onchange.applyMethodName, isawait, hasdefoReturnedNormally, patheNAme, onchange.asyncIndex, onchange.onlyClose).copy();
			tweakToNestedFuncDef(applyMethodFuncDef, toSpliceIn);
			onchange.applyMethodFuncDef = applyMethodFuncDef;
			
			//cleanup
			FuncDef cleanUpMethodFuncDef = (FuncDef)OnChangeMethodCodeGen.makeOnChangeCleanUpMethod(new Block(onchange.getLine(), onchange.getColumn()), onchange.getFullnameSO(), onchange.cleanupMethodName, onchange.getLine(), onchange.getColumn(), this.patheNAme, onchange.asyncIndex, !onchange.noReturn).copy();
			tweakToNestedFuncDef(cleanUpMethodFuncDef, toSpliceIn);
			onchange.cleanUpMethodFuncDef = cleanUpMethodFuncDef;
		}
		
		return null;
	}
	
	
	
	
	private Stack<AsyncBodyBlock> currAsycnBodBlock = new Stack<AsyncBodyBlock>();
	
	@Override
	public Object visit(AsyncBodyBlock asyncBodyBlock) {

		//super.visit(asyncBodyBlock);
		currAsycnBodBlock.push(asyncBodyBlock);
		asyncBodyBlock.funcParams = processHasExtraCaps(asyncBodyBlock, asyncBodyBlock.getLine(), asyncBodyBlock.getColumn(), true, null);
		//currAsycnBodBlock.pop();
		//add the init and apply methods to owner class
		
		
		if(asyncBodyBlock.initMethodNameFuncDef == null){//first time only
			setHadMadeRepoints();
			
			//special logic depends on if every is within....
			
			Block onChangeBlock = asyncBodyBlock.mainBody;
			Type retType = asyncBodyBlock.getTaggedType();
			
			HashMap<ArrayList<Block>, Block> toRev = null; 
			
			
			if(null != retType && !asyncBodyBlock.noReturn){
				toRev = new HashMap<ArrayList<Block>, Block>();
				FuncParam laret = new FuncParam(0,0,"ret$", retType, true);
				asyncBodyBlock.funcParams.params.add(laret);
				if(asyncBodyBlock.extraCapturedLocalVars != asyncBodyBlock.funcParams){//MHA: it seems that normally thes two lists in infact the same unless a var referenced in pre block is referened within one of the onchange blocks
					asyncBodyBlock.extraCapturedLocalVars.params.add(laret);
				}
				
				if(!blockHasDefoReturnedInNormalWay(onChangeBlock, retType)){
					//repoint to assign results to onChangeBlock...
					onChangeBlock = Utils.makeAssignToRefFromrhsBlock(onChangeBlock, retType, true); //xxx => ret := {xxx}
				}
				else{
					onChangeBlock = Utils.makeAssignToRefFromrhsBlock(onChangeBlock, retType, false); //xxx => ret := {xxx}
				}
				//TODO: remove the above if/else block as made redundant by check inside onchange
				
				RepointBreakContinueReturnInApplyMethod rbcram = new RepointBreakContinueReturnInApplyMethod(false, retType, -2);//1= break halts
				
				for(ArrayList<Block> blks : new ArrayList[]{asyncBodyBlock.preBlocks==null || asyncBodyBlock.preBlocks.isEmpty()?null : asyncBodyBlock.preBlocks,
															asyncBodyBlock.postBlocks==null || asyncBodyBlock.postBlocks.isEmpty()?null : asyncBodyBlock.postBlocks}){
					if(blks != null){
						
						Block blk = blks.get(0);
						if(blk.getTaggedType() != null && blk.getShouldBePresevedOnStack()){
							Block newBlock = null;
							if(!blockHasDefoReturnedInNormalWay(blk, retType)){
								//repoint to assign results to onChangeBlock...
								newBlock = Utils.makeAssignToRefFromrhsBlock(blk, retType, true); //xxx => ret := {xxx}
							}
							else{
								newBlock = Utils.makeAssignToRefFromrhsBlock(blk, retType, false); //xxx => ret := {xxx}
							}
							
							rbcram.visit(newBlock);
							
							blks.set(0,  newBlock);//overwrite with amended version
							toRev.put(blks, blk);
						}
					}
				}
			}
			
			//RepointBreakContinueReturnInApplyMethod rbcram = new RepointBreakContinueReturnInApplyMethod(false, asyncBodyBlock.getTaggedType());
			//rbcram.visit(onChangeBlock);
			
			Set<String> toSpliceIn = new HashSet<String>();
			for(FuncParam fp : asyncBodyBlock.funcParams.params){
				toSpliceIn.add(fp.name);
			}

			asyncBodyBlock.toSpliceIn = toSpliceIn;
			
			ClassDefStateObject thestateObjectClassDef = new ClassDefStateObject(asyncBodyBlock.getLine(), asyncBodyBlock.getColumn(), asyncBodyBlock.funcParams, asyncBodyBlock.getFullnameSO(), asyncBodyBlock.preBlockVars);
			asyncBodyBlock.stateObjectClassDef = thestateObjectClassDef;
			
			for(LineHolder lh : asyncBodyBlock.mainBody.lines){
				Line l = lh.l;
				if(l instanceof OnChange){
					((OnChange) l).stateObjectClassDef = thestateObjectClassDef;
				}
			}
			
			FuncDef initMethod = (FuncDef)OnChangeMethodCodeGenAsync.makeOnChangeInitMethod(asyncBodyBlock, this.patheNAme).copy();//copy so that refName ast change doesnt infect origonal version
			tweakToNestedFuncDef(initMethod, toSpliceIn);
			asyncBodyBlock.initMethodNameFuncDef = initMethod;
			
			//create the init and apply here, keep note of splices to make
			
			//copy function code over for these - apply splice to stateObject where approperiate
			FuncDef applyMethodFuncDef = (FuncDef)OnChangeMethodCodeGenAsync.makeOnChangeApplyMethod(asyncBodyBlock, this.patheNAme).copy();
			tweakToNestedFuncDef(applyMethodFuncDef, toSpliceIn);
			asyncBodyBlock.applyMethodFuncDef = applyMethodFuncDef;
			
			//cleanup
			FuncDef cleanUpMethodFuncDef = (FuncDef)OnChangeMethodCodeGenAsync.makeOnChangeCleanUpMethod(asyncBodyBlock, this.patheNAme).copy();
			tweakToNestedFuncDef(cleanUpMethodFuncDef, toSpliceIn);
			asyncBodyBlock.cleanUpMethodFuncDef = cleanUpMethodFuncDef;
			
			if(null != toRev){
				for(ArrayList<Block> orig : toRev.keySet()){
					orig.set(0, toRev.get(orig));
				}
			}
		}
		
		currAsycnBodBlock.pop();
		
		return null;
	}
}
