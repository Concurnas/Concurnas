package com.concurnas.compiler.visitors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;

import com.concurnas.bootstrap.runtime.ref.Ref;
import com.concurnas.compiler.ast.AccessModifier;
import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.ast.AssignStyleEnum;
import com.concurnas.compiler.ast.AsyncRefRef;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.FuncInvokeArgs;
import com.concurnas.compiler.ast.FuncParam;
import com.concurnas.compiler.ast.FuncParams;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.New;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.PrimativeType;
import com.concurnas.compiler.ast.PrimativeTypeEnum;
import com.concurnas.compiler.ast.ReturnStatement;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.util.DummyErrorRaiseable;
import com.concurnas.runtime.Pair;

public class OnChangeMethodCodeGen {
	private final static ErrorRaiseable errorRaisableSupression = new DummyErrorRaiseable();
	
	private static String getRegSet(int asyncIndex){
		String ret = "$regSet";
		if(asyncIndex>-1){
			ret += asyncIndex;
		}
		return ret;
	}
	
	
	private static String getRefStateTracker(int asyncIndex){
		String ret = "refStateTracker$";
		if(asyncIndex>-1){
			ret += asyncIndex;
		}
		return ret;
	}
	
	
	
	static void addPostReturnToBlock(Block block, int line, int col, boolean isawait, boolean addReturn, String patheNAme, int asyncIndex ){
		addPostReturnToBlock(block, line, col, isawait, addReturn, patheNAme, asyncIndex, "true");
	}
	
	static void addPostReturnToBlock(Block block, int line, int col, boolean isawait, boolean addReturn, String patheNAme, int asyncIndex, String onAsyncIndex){
		
		//System.err.println(String.format("Block block: %s, int line: %s, int col: %s, boolean isawait: %s, boolean addReturn: %s, String patheNAme: %s, int asyncIndex: %s, String onAsyncIndex: %s",  block,  line,  col,  isawait,  addReturn,  patheNAme,  asyncIndex,  onAsyncIndex) );
		
		boolean isWithinAsyncBlock = asyncIndex>-1;
		String regSet = getRegSet(asyncIndex);
		/*Block blk;
		if(isawait) {
			blk = Utils.parseBlock("for(changeRef$ in changeRefs$){ if(false and changeRef$:isClosed()){  stateObject$."+regSet+".unregister(changeRef$ as Object:);  }}", patheNAme, line, true);
		}else {
			blk = Utils.parseBlock("for(changeRef$ in changeRefs$){ if(changeRef$:isClosed()){  stateObject$."+regSet+".unregister(changeRef$ as Object:);  }}", patheNAme, line, true);
		}
		*/
		Block blk = Utils.parseBlock("for(changeRef$ in changeRefs$){ if(changeRef$:isClosed()){  stateObject$."+regSet+".unregister(changeRef$ as Object:);  }}", patheNAme, line, true);
		
		blk.setShouldBePresevedOnStack(false);
		blk.isolated = true;
		block.add(new LineHolder(line, col, blk));
		if(addReturn){
			if(isawait){
				Block toAdd = Utils.parseBlock("return $wedoneYet;", patheNAme, line, true);
				toAdd.isolated=true;
				block.add(new LineHolder(line, col, toAdd));
			}else{
				if(isWithinAsyncBlock){//normally we just return true, as in, continue to process stuff
					block.add(new LineHolder(line, col, Utils.parseReturnStatement(String.format("return %s;", onAsyncIndex), patheNAme, line)));
				}
				else{
					block.add(new LineHolder(line, col, Utils.parseReturnStatement("return not (stateObject$."+regSet+".hasRegistrations())", patheNAme, line)));
					
				}
			}
		}
	}
	
	private static class ReturnTaggerAsInOnChange extends AbstractVisitor{
		private static ReturnTaggerAsInOnChange theRetta = new ReturnTaggerAsInOnChange();
		
		public static Block doApply(Block blk){
			blk.accept(theRetta);
			return blk;
		}
		
		@Override
		public Object visit(ReturnStatement ret){
			ret.withinOnChange = true;
			return null;
		}
	}
	
	public static FuncDef makeOnChangeInitMethod(ArrayList<Node> exprs, String stateObjectClasName, String funcName, int line, int col, boolean addEarlyNotificationQ, String patheNAme, int asyncIndex, boolean onlyClosed){
		
		Block funcblock = new Block(line, col);
		FuncParams params = new FuncParams(line, col, new FuncParam(line, col, "stateObject$", new NamedType(line, col, stateObjectClasName), true) );
		
		//if(addEarlyNotificationQ){
		//everything gets the queue because it makes the init interface easier...
		//TODO: is this right?
		{
			NamedType refType = new NamedType(line, col, new ClassDefJava(Ref.class));
			ArrayList<Type> genTypesrt = new ArrayList<Type>();
			genTypesrt.add(new NamedType(new ClassDefJava(Object.class)));
			refType.setGenTypes(genTypesrt);
			
			ArrayList<Type> genTypes = new ArrayList<Type>();
			genTypes.add(refType);
			NamedType earlyQue = new NamedType(line, col, new ClassDefJava(LinkedHashSet.class));
			earlyQue.setGenTypes(genTypes);
			
			params.add(new FuncParam(line, col, "earlyNotifiQ", earlyQue, true));//add a ref to a queue such that if thing is set on entry its immediatly written
		}

			
		{
			//funcblock.add(new LineHolder(line, col, Utils.parseBlock("from java.util import Set;", this.patheNAme, line, true)));
			
			NamedType refType = new NamedType(new ClassDefJava(Ref.class));
			ArrayList<Type> genTypesrt = new ArrayList<Type>();
			genTypesrt.add(new NamedType(new ClassDefJava(Object.class)));
			refType.setGenTypes(genTypesrt);
			
			ArrayList<Type> genTypes = new ArrayList<Type>();
			genTypes.add(refType);
			NamedType changedAlready = new NamedType(line, col, new ClassDefJava(HashSet.class));
			changedAlready.setGenTypes(genTypes);
			
			//Constru
			//cc = changedAlready
			funcblock.add(new LineHolder(line, col, new AssignNew(null, line, col, true, true, "$changedAlready", changedAlready, AssignStyleEnum.EQUALS, new New(line, col, changedAlready, new FuncInvokeArgs(line, col), true) ) ));
		}
		
		String regSet = getRegSet(asyncIndex);
		
		FuncDef fd = new FuncDef( line,  col,  null, AccessModifier.PUBLIC, funcName, params, funcblock, new PrimativeType(PrimativeTypeEnum.VOID), false, false, false, new ArrayList<Pair<String, NamedType>>());
		//run the expression code and register on whatever pops out...
		int na=0;
		for(Node n : exprs){
			Type t = n.getTaggedType();
			String varName = "$x"+na++;//append to var since we're making new(?) declarations and dont want the type to be locked as the first one in the stmt
			
			String doingsString;
			

			if(t.hasArrayLevels() ){//array of refs
				if(n instanceof AssignNew){
					AssignNew an = (AssignNew)n;//onchange(a = getRef()){print a;}
					varName = an.name;
					funcblock.add(new LineHolder(line, col, new AssignExisting(line, col, varName, AssignStyleEnum.EQUALS, an.expr )));
				}
				else{
					funcblock.add(new LineHolder(line, col, (new AssignExisting(line, col, varName, AssignStyleEnum.EQUALS, (Expression)n ))));
				}
				
				if(addEarlyNotificationQ){//we get a deep copy of the localarray so its ok to assume no content changes between the registreation and adding to the queue
					if(onlyClosed) {
						doingsString = "stateObject$."+regSet+".register("+varName+");\n for($closedItem in "+varName+"){ if($closedItem:isClosed()){ $changedAlready.add($closedItem:)} };";
					}else {
						doingsString = "alreadyChanged = stateObject$."+regSet+".register("+varName+");\nfor($idx in alreadyChanged){ $changedAlready.add("+varName+" [$idx]:);  }";
					}
				}
				else{
					doingsString = "stateObject$."+regSet+".register("+varName+");";
				}
			}
			
			else if(!TypeCheckUtils.hasRefLevels(t) && (TypeCheckUtils.isList(errorRaisableSupression, t, false) || TypeCheckUtils.isSet(errorRaisableSupression, t, false) || TypeCheckUtils.isMap(errorRaisableSupression, t, false))){
				//t instanceof NamedType && null != TypeCheckUtils.checkSubType(this.errorRaisableSupression, list_object, t, 0, 0, 0, 0)
				if(n instanceof AssignNew){
					AssignNew an = (AssignNew)n;//onchange(a = getRef()){print a;}
					varName = an.name;
					funcblock.add(new LineHolder(line, col, new AssignExisting(line, col, varName, AssignStyleEnum.EQUALS, an.expr )));
				}
				else{
					funcblock.add(new LineHolder(line, col, (new AssignExisting(line, col, varName, AssignStyleEnum.EQUALS, (Expression)n ))));
				}
				
				doingsString = " for($xx in "+varName+"){";
				
				if(addEarlyNotificationQ) {
					if(onlyClosed) {
						doingsString += "stateObject$."+regSet+".register($xx:); \n if($xx:isClosed()){ $changedAlready.add($xx:)}";
					}else {
						doingsString += "if(stateObject$."+regSet+".register($xx:)){ $changedAlready.add($xx:); }";
					}
				}else {
					doingsString += "stateObject$."+regSet+".register($xx:);";
				}
				doingsString += " }";
				
			}
			else if(TypeCheckUtils.isRegistrationSet(errorRaisableSupression, t)){//its a ref set
				//JPT: copy paste tastic...
				if(n instanceof AssignNew){
					AssignNew an = (AssignNew)n;//onchange(a = getRef()){print a;}
					varName = an.name;
					funcblock.add(new LineHolder(line, col, new AssignExisting(line, col, varName, AssignStyleEnum.EQUALS, an.expr )));
				}
				else{
					funcblock.add(new LineHolder(line, col, (new AssignExisting(line, col, varName, AssignStyleEnum.EQUALS, (Expression)n ))));
				}
				
				if(addEarlyNotificationQ) {
					if(onlyClosed) {
						doingsString = "stateObject$."+regSet+".register("+varName+");  for($closedItem in "+varName+".toSet()){ if($closedItem:isClosed()){ $changedAlready.add($closedItem:)} } ";
					}else {
						doingsString = "$itemz = stateObject$."+regSet+".register("+varName+");  $changedAlready.addAll($itemz); ";
					}
				}else {
					doingsString = "stateObject$."+regSet+".register("+varName+");";
				}
				
			}
			else{//just a ref
				AsyncRefRef refref;
				if(n instanceof AssignNew){
					AssignNew an = (AssignNew)n;//onchange(a = getRef()){print a;}
					varName = an.name;
					refref=new AsyncRefRef(line, col, an.expr, 1);
				}
				else{
					refref=new AsyncRefRef(line, col, (Expression)n, 1);
				}
				
				if(addEarlyNotificationQ) {
					if(onlyClosed) {
						doingsString = "stateObject$."+regSet+".register("+varName+":); if("+varName+":isClosed()){$changedAlready.add("+varName+":);}";
					}else {
						doingsString = "if(stateObject$."+regSet+".register("+varName+":)){ $changedAlready.add("+varName+":); }";
					}
				}else {
					doingsString = "stateObject$."+regSet+".register("+varName+":);";
				}
				
				funcblock.add(new LineHolder(line, col, (new AssignExisting(line, col, varName, AssignStyleEnum.EQUALS, new AsyncRefRef(line, col, refref, 1) ))));
			}

			//System.err.println("ds: " + doingsString);
			
			funcblock.add(new LineHolder(line, col, Utils.parseBlock(doingsString, patheNAme, line, true)));
		}
		//list, map, refnotificationset
		
		funcblock.add(new LineHolder(line, col, Utils.parseBlock("if(not ($changedAlready.isEmpty())){ earlyNotifiQ.addAll($changedAlready);  } ", patheNAme, line, true)));
		
		funcblock.add(new LineHolder(line, col, new ReturnStatement(line, col)));
		
		return fd;
	}
	
	public static FuncDef makeOnChangeCleanUpMethod(Block blk, String stateObjectClasName, String funcName, int line, int col, String patheNAme, int asyncIndex, boolean shouldReturn){
		
		String extra = "";
		/*if(asyncIndex==-1 && shouldReturn){//returns something so close this
			extra+= "ret$:close();";//returns and its not within an async
		}*/
		
		Block funcblock = Utils.parseBlock(String.format("stateObject$.%s.unregisterAll(); %s return;", getRegSet(asyncIndex), extra), patheNAme, line, true);
		return new FuncDef( line,  col, null,  AccessModifier.PUBLIC, funcName, new FuncParams(line, col, new FuncParam(line, col, "stateObject$", new NamedType(line, col, stateObjectClasName), true) ), funcblock, new PrimativeType(PrimativeTypeEnum.VOID), false, false, false, new ArrayList<Pair<String, NamedType>>());
	}
	
	private static final PrimativeType const_boolean = new PrimativeType(PrimativeTypeEnum.BOOLEAN);
	
	
	public static FuncDef makeOnChangeApplyMethod(Block e, String stateObjectClasName, String funcName, boolean isawait, boolean hasdefoReturnedNormally, String patheNAme, int asyncIndex, boolean onlyClose) {
		//state object and changed
		int line = e.getLine();
		int col = e.getColumn();
		
		e = ReturnTaggerAsInOnChange.doApply(e);
		
		Block funcblock = new Block(line, col);
		FuncParams params = new FuncParams(line, col, new FuncParam(line, col, "stateObject$", new NamedType(line, col, stateObjectClasName), true) );
		
		/*ArrayList<Type> genTypes = new ArrayList<Type>();
		genTypes.add(new NamedType(new ClassDefJava(Object.class)));
		NamedType refNT = new NamedType(new ClassDefJava(Ref.class));
		refNT.setGenTypes(genTypes);
		*/
		NamedType transNT = new NamedType(line, col, new ClassDefJava(com.concurnas.bootstrap.runtime.transactions.Transaction.class));
		
		//refNT.isRef = true;
		
		//NamedType refNT = new NamedType(line, col, new NamedType(new ClassDefJava(Object.class)));
		
		params.add( new FuncParam(line, col, "changed", transNT, true) );
		params.add( new FuncParam(line, col, "isFirst", const_boolean, true) );
		FuncDef fd = new FuncDef( line,  col, null, AccessModifier.PUBLIC, funcName, params, funcblock, const_boolean, false, false, false, new ArrayList<Pair<String, NamedType>>());
		
		String regSet = getRegSet(asyncIndex);
		String refStateTracker = getRefStateTracker(asyncIndex);
		
		funcblock.add(new LineHolder(line, col, Utils.parseAssignStmt("changeRefs$ java.lang.Object:com.concurnas.bootstrap.runtime.ref.Ref[] = changed.getChanged();", patheNAme, line)));//explicitly state this as being new - without this the bc compiler was reassigning the local slot variable - which is not right cos ists used for other things in nested calls
		
		funcblock.add(new LineHolder(line, col, Utils.parseBlock("crAnyRef$ = false\n for(changeRef$ in changeRefs$){if(stateObject$."+regSet+".isRegistered(changeRef$:)) { crAnyRef$ = true; break;  }  } if(not(crAnyRef$)) { return not(stateObject$."+regSet+".hasRegistrations()); }", patheNAme, line, true)));
		
		//early termiantion if we've recieved a notificaiton for something that we are not interested in
		
		if(isawait){
			funcblock.add(new LineHolder(line, col, Utils.parseBlock("for(changeRef$ in changeRefs$){if( changeRef$:isClosed()){  stateObject$."+regSet+".unregister(changeRef$ as Object:);  }}", patheNAme, line, true)));
			//funcblock.add(new LineHolder(line, col, Utils.parseBlock("if(changed:isClosed()){  stateObject$."+regSet+".unregister(changed as Object:);  }", patheNAme, line, true)));
			
			if(hasdefoReturnedNormally){
				e = (Block)e.copy();
				e.isolated=true;
				funcblock.add(new LineHolder(line, col, e));
				//funcblock.setShouldBePresevedOnStack(false);
			}
			else{
				AssignNew theOther = new AssignNew(null, line,  col, true, false, "$wedoneYet", e);
				theOther.setInsistNew(true);
				theOther.setTaggedType(ScopeAndTypeChecker.const_boolean);
				
				funcblock.add(new LineHolder(line, col, theOther));
			}
		}
		else{
			if(onlyClose) {
				funcblock.add(new LineHolder(line, col, Utils.parseBlock("crAnyRef$ = false\n for(changeRef$ in changeRefs$){ if(changeRef$:isClosed()){  stateObject$."+regSet+".unregister(changeRef$ as Object:); crAnyRef$ = true } } \n  if(not(crAnyRef$)) { return not(stateObject$."+regSet+".hasRegistrations()); }", patheNAme, line, true)));
			}else {
				funcblock.add(new LineHolder(line, col, Utils.parseBlock("crAnyRef$ = false\n for(changeRef$ in changeRefs$){ crAnyRef$ = stateObject$."+refStateTracker+".shouldProcess(changeRef$:); \n if(changeRef$:isClosed()){  stateObject$."+regSet+".unregister(changeRef$ as Object:); } } \n  if(not(crAnyRef$)) { return not(stateObject$."+regSet+".hasRegistrations()); }", patheNAme, line, true)));
			}
			//funcblock.add(new LineHolder(line, col, Utils.parseBlock("if(changed:isClosed()){  stateObject$."+regSet+".unregister(changed as Object:); return not(stateObject$."+regSet+".hasRegistrations()); }", patheNAme, line, true)));
			
			funcblock.add(new LineHolder(line, col, e));
		}
		
		if(isawait){
			if(!hasdefoReturnedNormally){
				//only add if doesnt return in normal way - e.g. await(xs|{ 	return true	})
				addPostReturnToBlock(funcblock, line, col, true, true, patheNAme, asyncIndex);
			}
		}
		else{
			ReturnVisitor rv = new ReturnVisitor("", false);
			rv.visit(fd);//hack to figure out if we need to add a return...
			
			if(!e.hasRetExcepOrBreaked){
				addPostReturnToBlock(funcblock, line, col, false, true, patheNAme, asyncIndex);
			}
		}

		/*PrintSourceVisitor psv = new PrintSourceVisitor();
		psv.visit(fd);
		
		String ret = com.concurnas.compiler.visitors.Utils.listToString(psv.items);
		System.out.println(ret);
	*/
		return fd;
	}
}
