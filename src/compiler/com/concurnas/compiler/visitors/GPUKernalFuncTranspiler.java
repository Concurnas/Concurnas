package com.concurnas.compiler.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Stack;

import com.concurnas.compiler.CaseExpressionAnd;
import com.concurnas.compiler.CaseExpressionAssign;
import com.concurnas.compiler.CaseExpressionAssignTuple;
import com.concurnas.compiler.CaseExpressionObjectTypeAssign;
import com.concurnas.compiler.CaseExpressionOr;
import com.concurnas.compiler.CaseExpressionPost;
import com.concurnas.compiler.CaseExpressionPre;
import com.concurnas.compiler.CaseExpressionTuple;
import com.concurnas.compiler.CaseExpressionWrapper;
import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.JustAlsoCaseExpression;
import com.concurnas.compiler.TypedCaseExpression;
import com.concurnas.compiler.ast.*;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.ast.util.GPUKernelFuncDetails;
import com.concurnas.compiler.ast.util.JustLoad;
import com.concurnas.compiler.bytecode.FuncLocation;
import com.concurnas.compiler.bytecode.FuncLocation.ClassFunctionLocation;
import com.concurnas.compiler.bytecode.FuncLocation.StaticFuncLocation;
import com.concurnas.compiler.typeAndLocation.Location;
import com.concurnas.compiler.typeAndLocation.LocationStaticField;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.visitors.datastructs.TheScopeFrame;
import com.concurnas.runtime.Pair;
import com.google.common.base.Objects;

public class GPUKernalFuncTranspiler implements Visitor {

	private final LinkedHashSet<ErrorHolder> errors = new LinkedHashSet<ErrorHolder>();
	protected final String fullPathFileName;
	
	@Override
	public Object visit(LineHolder lineHolder) {
		this.enterLine();
		
		String prefix = "";
		for(int n=0; n < indentLevel; n++) {
			prefix += "  ";
		}
		this.addItem(prefix);
		
		Type ret = (Type)lineHolder.l.accept(this);
		this.leaveLine();
		this.addItem(";\n");
		return ret;
	}
	
	public GPUKernalFuncTranspiler(String fullPathFileName)
	{
		this.fullPathFileName = fullPathFileName;
	}
	
	
	public LinkedHashSet<ErrorHolder> getErrors() {
		return this.errors;
	}
	
	Stack<HashMap<Integer, ErrorHolder>> errorForLine = new Stack<HashMap<Integer, ErrorHolder>>();
	
	protected void enterLine()
	{
		errorForLine.add(new HashMap<Integer, ErrorHolder>());
	}
	
	protected void leaveLine()
	{
		if(!errorForLine.isEmpty())
		{
			HashMap<Integer, ErrorHolder> errs = errorForLine.pop();
			if(null != errs)
			{
				for(ErrorHolder err: errs.values())
				{
					this.errors.add(err);
				}
			}
		}
	}
	
	protected ArrayList<REPLTopLevelComponent> errorLocation = new ArrayList<REPLTopLevelComponent>();
	public void pushErrorContext(REPLTopLevelComponent xxx) {
		errorLocation.add(xxx);
	}
	public REPLTopLevelComponent popErrorContext(){
		return errorLocation.remove(errorLocation.size()-1);
	}
	
	public void raiseError(int line, int column, String error)
	{
		boolean isEmpty = errorForLine.isEmpty();
		if(isEmpty){//JPT: see if u can remove this, its a bit ugly
			enterLine();
		}
		
		HashMap<Integer, ErrorHolder> currentLineToErr = errorForLine.peek();
		if(null != currentLineToErr)
		{
			if(!currentLineToErr.containsKey(line))
			{//add if one has not already been assigned
				currentLineToErr.put(line, new ErrorHolder(this.fullPathFileName, line, column, error, null, Utils.tagErrorChain(errorLocation)  ) );
			}
		}
		
		if(isEmpty){
			leaveLine();
		}
	}
	
	// above should be mixin...
	
	
	private static class GPUFuncDefScanner extends AbstractVisitor implements Unskippable{
		private GPUKernalFuncTranspiler transpilerToApply;
		private TheScopeFrame moduleScopeFrame;

		public GPUFuncDefScanner(TheScopeFrame moduleScopeFrame, GPUKernalFuncTranspiler transpilerToApply) {
			this.moduleScopeFrame = moduleScopeFrame;
			this.transpilerToApply = transpilerToApply;
		}
		
		@Override
		public Object visit(FuncDef funcDef) {//here's one!
			
			if(funcDef.isGPUKernalOrFunction != null) {
				this.transpilerToApply.visit(funcDef);
				GPUKernelFuncDetails details = funcDef.gpuKernelFuncDetails;

				this.moduleScopeFrame.addGPUFuncOrKernel(details);
			}
			return null;
		}
	}
	
	public static LinkedHashSet<ErrorHolder> performTranspilation(TheScopeFrame moduleScopeFrame, String fullPathName, Block lexedAndParsedAST) {
		LinkedHashSet<ErrorHolder> ret;
		{//top level constant variables...
			GPUKernalFuncTranspiler topLevelTranspi = new GPUKernalFuncTranspiler(fullPathName);

			topLevelTranspi.gpuKorFuncStrRep = new StringBuilder();
			boolean anyconst = false;
			for(LineHolder lh : lexedAndParsedAST.lines) {
				Line l = lh.l;
				boolean wasConst=false;
				if(l instanceof AssignNew) {
					wasConst = (Boolean)topLevelTranspi.visit((AssignNew)l);
				}else if(l instanceof AssignExisting) {
					wasConst = (Boolean)topLevelTranspi.visit((AssignExisting)l);
				}else if(l instanceof AssignMulti) {
					wasConst = (Boolean)topLevelTranspi.visit((AssignMulti)l);
				}
				
				if(wasConst) {
					anyconst = wasConst;
					topLevelTranspi.addItem(";\n");
				}
			}
			if(anyconst) {
				moduleScopeFrame.gpuKernelFuncDetails = new GPUKernelFuncDetails(-1, moduleScopeFrame.moduleName, "", topLevelTranspi.gpuKorFuncStrRep.toString(), new GPUKernelFuncDetails[0], "", "");
			}
			
			ret = topLevelTranspi.getErrors();
			
		}
		
		GPUKernalFuncTranspiler transpiler = new GPUKernalFuncTranspiler(fullPathName);
		
		GPUFuncDefScanner scanner = new GPUFuncDefScanner(moduleScopeFrame, transpiler);
		scanner.visit(lexedAndParsedAST);
		
		ret.addAll(transpiler.getErrors());
		
		return ret;
	}
	
	private String getTheMessage(String what, String advice) {
		if(advice != null) {
			return String.format("%s may not be used within a gpudef or gpukernel, %s", what, advice);
		}else {
			return String.format("%s may not be used within a gpudef or gpukernel", what);
		}
	}
	
	private void raiseErrorNoUse(int line, int column, String what) {
		raiseErrorNoUse(line, column, what, null);
	}
	private void raiseErrorNoUse(int line, int column, String what, String advice) {

		String msg = getTheMessage(what, advice);
		this.raiseError(line, column, msg);
	}
	
	public StringBuilder gpuKorFuncStrRep = null;

	private void addItem(String what) {
		if(null != gpuKorFuncStrRep) {
			gpuKorFuncStrRep.append(what);
		}
	}
	
	@Override
	public Object visit(Additive addMinusExpression) {
		if(addMinusExpression.getFoldedConstant() != null) {
			this.addItem(addMinusExpression.getFoldedConstant().toString());
		}else {
			this.addItem("("); addMinusExpression.head.accept(this); 
			for(AddMinusExpressionElement i : addMinusExpression.elements){
				i.accept(this);
			}
			this.addItem(")");
		}
		
		return null;
	}
	
	@Override
	public Object visit(AddMinusExpressionElement addMinusExpressionElement) {
		//visitList.add("AddMinusExpressionElement");
		this.addItem(addMinusExpressionElement.isPlus?" + ":" - ");
		addMinusExpressionElement.exp.accept(this);
		return null;
	}
	
	@Override
	public Object visit(AndExpression andExpression) {
		//visitList.add("AndExpression");
		this.addItem("("); andExpression.head.accept(this); 
		for(Expression i : andExpression.things)
		{
			this.addItem(" && ");
			i.accept(this);
		}
		
		this.addItem(")"); 
		return null;
	}

	@Override
	public Object visit(Annotation annotation){
		raiseErrorNoUse(annotation.getLine(), annotation.getColumn(), "annoations");
		return null;//ignore non functional
	}

	@Override
	public Object visit(AnnotationDef annotationDef){
		raiseErrorNoUse(annotationDef.getLine(), annotationDef.getColumn(), "annoation definitions");
		return null;
	}

	@Override
	public Object visit(ArrayConstructor arrayConstructor) {
		raiseErrorNoUse(arrayConstructor.getLine(), arrayConstructor.getColumn(), "array definitions outside of assignment statements");
		//visitList.add("ArrayConstructor");
		/*this.addItem("new ");
		arrayConstructor.type.accept(this);
		for(Expression level : arrayConstructor.arrayLevels)
		{
			this.addItem("[");
			if(level != null){
				Object constx = level.getFoldedConstant();
				if(null == constx) {
					raiseErrorNoUse(arrayConstructor.getLine(), arrayConstructor.getColumn(), "array dimention which is not a constant");
				}
				this.addItem("" + constx);
			}
			this.addItem("]");
		}
		
		if(arrayConstructor.defaultValue != null) {
			raiseErrorNoUse(arrayConstructor.getLine(), arrayConstructor.getColumn(), "array default values");
		}*/
		return null;
	}
	
	@Override
	public Object visit(ArrayDef arrayDef) {
		//visitList.add("ArrayDef");
		if(!arrayDef.isArray){
			raiseErrorNoUse(arrayDef.getLine(), arrayDef.getColumn(), "list instantiation");
		}
		raiseErrorNoUse(arrayDef.getLine(), arrayDef.getColumn(), "array definitions outside of array initalizers");
		/*
		//{1000.0, 2.0, 3.4, 7.0, 50.0};		
		this.addItem("{");
		
		ArrayList<Expression> elms = arrayDef.getArrayElements(this);
		
		if(elms.isEmpty()) {
			raiseErrorNoUse(arrayDef.getLine(), arrayDef.getColumn(), "empty array instantiation");
		}
		int len = elms.size();
		for(int n=0; n < len; n++) {
			elms.get(n).accept(this);
			if(n != len-1) {
				this.addItem(", ");
			}
		}
		
		this.addItem("}");*/
		return null;
	}

	public void processArrayRefElements(ArrayRefLevelElementsHolder elements){
		List<ArrayRefElement> flat = new ArrayList<ArrayRefElement>();
		for(Pair<Boolean, ArrayList<ArrayRefElement>> bracksetx: elements.getAll()){
			ArrayList<ArrayRefElement> brackset = bracksetx.getB();
			
			if(bracksetx.getA()) {
				raiseErrorNoUse(brackset.get(0).getLine(), brackset.get(0).getColumn(), "safe array reference");
			}
			
			for(ArrayRefElement e : brackset) {
				flat.add(e);
			}
		}
		
		int len = flat.size();
		for(int n=0; n < len; n++) {
			this.addItem("[");
			flat.get(n).accept(this);
			this.addItem("]");
		}
	}

	
	@Override
	public Object visit(ArrayRef arrayRef) {
		//visitList.add("ArrayRef");
		arrayRef.expr.accept(this);
		processArrayRefElements(arrayRef.arrayLevelElements);
		return null;
	}

	@Override
	public Object visit(ArrayRefElement arrayRefElement) {
		//visitList.add("ArrayRefElement");
		arrayRefElement.e1.accept(this);
		return null;
	}

	@Override
	public Object visit(ArrayRefElementPostfixAll ar) {
		raiseErrorNoUse(ar.getLine(), ar.getColumn(), "array postfix reference");
		return null;
	}

	@Override
	public Object visit(ArrayRefElementPrefixAll ar) {
		raiseErrorNoUse(ar.getLine(), ar.getColumn(), "array prefix reference");
		return null;
	}
	@Override
	public Object visit(ArrayRefElementSubList ar) {
		raiseErrorNoUse(ar.getLine(), ar.getColumn(), "array sublist reference");
		return null;
	}
	
	
	@Override
	public Object visit(AssertStatement assertStatement) {
		raiseErrorNoUse(assertStatement.getLine(), assertStatement.getColumn(), "assert");
		return null;
	}

	private void processArrayDef(ArrayElementGettable ad) {
		//{1000.0, 2.0, 3.4, 7.0, 50.0};		
		this.addItem("{");
		
		ArrayList<Expression> elms = ad.getArrayElements(this);
		
		if(elms.isEmpty()) {
			raiseErrorNoUse(((Expression)ad).getLine(), ((Expression)ad).getColumn(), "empty array instantiation");
		}
		int len = elms.size();
		for(int n=0; n < len; n++) {
			Expression e = elms.get(n);
			if(e instanceof ArrayElementGettable) {
				processArrayDef((ArrayElementGettable)e);
			}else {
				e.accept(this);
			}
			
			if(n != len-1) {
				this.addItem(", ");
			}
		}
		
		this.addItem("}");
	}
	
	
	private ArrayList<Integer> validateArrayInstanitation(ArrayElementGettable ad){
		ArrayList<Integer> sizes = new ArrayList<Integer>();
		ArrayList<Expression> items = ad.getArrayElements(this);
		sizes.add(items.size());
		
		ArrayList<Integer> nextlevelsizes = null;
		for(Expression item : items) {
			if(item instanceof ArrayDef) {
				ArrayList<Integer> nevlevel = validateArrayInstanitation((ArrayDef)item );
				if(null == nextlevelsizes) {
					nextlevelsizes = nevlevel;
				}else {
					//ensue match with preoiuvs one
					if(!nextlevelsizes.equals(nevlevel)) {
						raiseErrorNoUse(((Expression)ad).getLine(), ((Expression)ad).getColumn(), "unequal n dimentional array subarray instantiation");
					}
				}
			}
		}
		
		if(null != nextlevelsizes) {
			sizes.addAll(nextlevelsizes);
		}
				
		return sizes;
	}
	
	private Expression reduceExpression(Expression epxr) {
		if(epxr instanceof PointerUnref) {
			Expression astOver = ((PointerUnref)epxr).astOverride;
			if(null != astOver) {
				return reduceExpression(astOver);
			}
		}
		
		return epxr;
	}
	boolean ignorePrivateAccModier = false;
	@Override
	public Object visit(AssignNew assignNew) {
		// visitList.add("AssignNew");

		if (assignNew.annotations != null) {
			raiseErrorNoUse(assignNew.getLine(), assignNew.getColumn(), "assignment annotations");
		}

		if (assignNew.isTransient) {
			raiseErrorNoUse(assignNew.getLine(), assignNew.getColumn(), "transient variables");
		}

		if(assignNew.isLazy){
			raiseErrorNoUse(assignNew.getLine(), assignNew.getColumn(), "lazy variables");
		}
		
		if (assignNew.isVolatile) {//?
			raiseErrorNoUse(assignNew.getLine(), assignNew.getColumn(), "volatile variables");
		}
		if (assignNew.prefix != null) {
			raiseErrorNoUse(assignNew.getLine(), assignNew.getColumn(), "prefixed variables");
		}
		
		if(assignNew.gpuVarQualifier == GPUVarQualifier.GLOBAL) {
			if(assignNew.type == null || assignNew.type.getPointer() ==0 ) {
				this.raiseError(assignNew.getLine(), assignNew.getColumn(), "global variables may only be of pointer type");
			}
		}
		
		if(assignNew.gpuVarQualifier != null) {
			this.addItem(assignNew.gpuVarQualifier.openClStr() + " ");
		}
		
		if (assignNew.type != null && assignNew.type.hasArrayLevels()) {
			if (assignNew.isFinal) {
				this.addItem("const ");
			}
			
			Expression expr = reduceExpression(assignNew.expr);
			
			if(expr instanceof ArrayElementGettable) {
				ArrayElementGettable adrhs = (ArrayElementGettable)expr;
				if (assignNew.type != null) {
					assignNew.type.accept(this);
				}
				
				if (assignNew.name != null) {
					this.addItem(assignNew.name);
				}
				
				//find and check sizes
				for(Integer level : validateArrayInstanitation(adrhs)){
					this.addItem("[" + level + "]");
				}
				
				if(assignNew.gpuVarQualifier == GPUVarQualifier.LOCAL) {
					raiseErrorNoUse(assignNew.getLine(), assignNew.getColumn(), "local varaible array instantiation");
				}else {
					this.addItem(" = ");
				}

				processArrayDef(adrhs);
				
			}else if(expr instanceof ArrayConstructor) {
				
				ArrayConstructor ac = (ArrayConstructor)expr;
				if (assignNew.type != null) {
					assignNew.type.accept(this);
				}
				
				if (assignNew.name != null) {
					this.addItem(assignNew.name);
				}
				
				for(Expression level : ac.arrayLevels){
					this.addItem("[");
					if(level != null){
						Object constx = level.getFoldedConstant();
						if(null == constx) {
							raiseErrorNoUse(ac.getLine(), ac.getColumn(), "array dimention which is not a constant");
						}
						this.addItem("" + constx);
					}
					this.addItem("]");
				}
				
				
			}else {
				raiseErrorNoUse(assignNew.getLine(), assignNew.getColumn(), "Array type declared in this way");
			}
		}else {

			if (assignNew.isFinal) {
				this.addItem("const ");
			}
			
			if (assignNew.type != null) {
				assignNew.type.accept(this);
			}
			
			if (assignNew.name != null) {
				this.addItem(assignNew.name + " ");
			}
			
			if (assignNew.eq != null && assignNew.expr != null) {
				if(assignNew.gpuVarQualifier == GPUVarQualifier.LOCAL) {
					this.addItem(String.format("; %s = ", assignNew.name));
				}else {
					this.addItem("= ");
				}
			}
			if (assignNew.expr != null) {
				assignNew.expr.accept(this);
			}
		}
		
		return assignNew.gpuVarQualifier == GPUVarQualifier.CONSTANT;
	}
	
	
	
	
	@Override
	public Object visit(AssignExisting assE) {
		//visitList.add("AssignExisting");
		
		if(assE.annotations != null){
			raiseErrorNoUse(assE.getLine(), assE.getColumn(), "assignment annotations");
		}
		
		if(assE.isTransient) {
			raiseErrorNoUse(assE.getLine(), assE.getColumn(), "transient variables");
		}

		if(assE.isLazy){
			raiseErrorNoUse(assE.getLine(), assE.getColumn(), "lazy variables");
		}
		
		Type tagged = assE.getTaggedType();
		if(assE.gpuVarQualifier == GPUVarQualifier.GLOBAL) {
			if(tagged == null || tagged.getPointer() ==0 ) {
				this.raiseError(assE.getLine(), assE.getColumn(), "global variables may only be of pointer type");
			}
		}
		
		
		if(assE.gpuVarQualifier != null) {
			this.addItem(assE.gpuVarQualifier.openClStr() + " ");
		}
		
		if(!assE.isReallyNew) {
			boolean pass = true;
			if(tagged.getGpuMemSpace() == GPUVarQualifier.CONSTANT) {
				pass=false;
			}else if(assE.assignee instanceof ArrayRef){
				Expression expresse = ((ArrayRef)assE.assignee).expr;
				if(expresse.getTaggedType() != null && expresse.getTaggedType().getGpuMemSpace() == GPUVarQualifier.CONSTANT){
					pass=false;
				}
			}
			
			if(!pass) {
				this.raiseError(assE.getLine(), assE.getColumn(), "constant variables cannot be reassigned");
			}
		}
		
		if (assE.isReallyNew && null != tagged) {

			if (tagged.hasArrayLevels()) {

				if(tagged instanceof NamedType) {
					raiseErrorNoUse(assE.getLine(), assE.getColumn(), "non primative types");
					tagged = null;
				}
				
				if(null != tagged) {
					Expression expr = reduceExpression(assE.expr);
					
					if (expr instanceof ArrayElementGettable) {
						ArrayElementGettable adrhs = (ArrayElementGettable) expr;
						tagged.accept(this);

						assE.assignee.accept(this);

						// find and check sizes
						for (Integer level : validateArrayInstanitation(adrhs)) {
							this.addItem("[" + level + "]");
						}

						if(assE.gpuVarQualifier == GPUVarQualifier.LOCAL) {
							raiseErrorNoUse(assE.getLine(), assE.getColumn(), "local varaible array instantiation");
						}else {
							this.addItem(" = ");
						}
						
						processArrayDef(adrhs);

					} else if (expr instanceof ArrayConstructor) {

						ArrayConstructor ac = (ArrayConstructor)expr;
						tagged.accept(this);

						assE.assignee.accept(this);

						for (Expression level : ac.arrayLevels) {
							this.addItem("[");
							if (level != null) {
								Object constx = level.getFoldedConstant();
								if (null == constx) {
									raiseErrorNoUse(ac.getLine(), ac.getColumn(), "array dimention which is not a constant");
								}
								this.addItem("" + constx);
							}
							this.addItem("]");
						}

					} else {
						raiseErrorNoUse(assE.getLine(), assE.getColumn(), "Array type declared in this way");
					}
				}

				return assE.gpuVarQualifier == GPUVarQualifier.CONSTANT;
			} else {
				tagged.accept(this);
			}
		}
		
		assE.assignee.accept(this);
		Type lhsType = assE.getTaggedType();
		if(null!= lhsType && lhsType.hasArrayLevels()) {
			raiseError(assE.getLine(), assE.getColumn(), "Array items must be individually assigned when used within a gpudef or gpukernel");
		}
		
		if(null != assE.eq) {
			String exprOp = assE.eq.useInGPUFunc;
			Type rhsType = assE.assignee.getTaggedType();
			boolean rhsFloatetc = ScopeAndTypeChecker.const_float.equals(rhsType) || ScopeAndTypeChecker.const_double.equals(rhsType);
			boolean lhsFloatetc = ScopeAndTypeChecker.const_float.equals(lhsType) || ScopeAndTypeChecker.const_double.equals(lhsType);
			
			
			if(assE.eq == AssignStyleEnum.MOD_EQUALS && (rhsFloatetc || lhsFloatetc)) {
				raiseErrorNoUse(assE.getLine(), assE.getColumn(), assE.eq + " with floating point variables");
			}
			else if(null == exprOp) {
				raiseErrorNoUse(assE.getLine(), assE.getColumn(), ""+ assE.eq);
			}else {
				
				if(assE.gpuVarQualifier == GPUVarQualifier.LOCAL) {
					this.addItem("; ");
					assE.assignee.accept(this);
				}
				
				this.addItem(" "+exprOp + " "); 
				
				if(null != assE.expr){
					assE.expr.accept(this);
				}
			}
		}
		
		return assE.gpuVarQualifier == GPUVarQualifier.CONSTANT;
	}
	
	
	
	@Override
	public Object visit(AsyncBlock asyncBlock) {
		raiseErrorNoUse(asyncBlock.getLine(), asyncBlock.getColumn(), "async");
		return null;
	}

	@Override
	public Object visit(AsyncBodyBlock asyncBodyBlock) {
		raiseErrorNoUse(asyncBodyBlock.getLine(), asyncBodyBlock.getColumn(), "async");
		return null;
	}

	@Override
	public Object visit(AsyncRefRef asyncRefRef) {
		raiseErrorNoUse(asyncRefRef.getLine(), asyncRefRef.getColumn(), "refs");
		return null;
	}

	@Override
	public Object visit(Await await) {
		raiseErrorNoUse(await.getLine(), await.getColumn(), "await", "gpu barriers would most likely be suitable here");
		return null;
	}

	@Override
	public Object visit(BitwiseOperation bitwiseOperation) {
		if(bitwiseOperation.getFoldedConstant() != null) {
			this.addItem(bitwiseOperation.getFoldedConstant().toString());
		}else {
			//visitList.add("AndExpression");
			bitwiseOperation.head.accept(this);
			String opName = bitwiseOperation.oper.gpuName;
			for(Expression i : bitwiseOperation.things){
				this.addItem(" " + opName + " ");
				i.accept(this);
			}
		}
		
		return null;
	}

	@Override
	public Object visit(Block block) {
		visitBlock(block, true);

		
		if(block.getShouldBePresevedOnStack()) {
			boolean ok = true;
			LineHolder lh = block.getLastLogical();
			if(lh == null) {
				ok=false;
			}else {
				ok = lh.l instanceof ReturnStatement;
			}
			
			if(!ok) {
				//raiseErrorNoUse(block.getLine(), block.getColumn(), "compound statements which return a value");
			}
		}
		
		return null;
	}

	@Override
	public Object visit(BreakStatement breakStatement) {
		//visitList.add("BreakStatement");
		this.addItem("break");
		
		if(null != breakStatement.returns){
			raiseErrorNoUse(breakStatement.getLine(), breakStatement.getColumn(), "break returning a value");
		}
		return null;
	}

	@Override
	public Object visit(CaseExpressionAnd caseExpressionAnd) {
		raiseErrorNoUse(caseExpressionAnd.getLine(), caseExpressionAnd.getColumn(), "pattern matching");
		return null;
	}

	@Override
	public Object visit(CaseExpressionAssign caseExpressionUntypedAssign){
		raiseErrorNoUse(caseExpressionUntypedAssign.getLine(), caseExpressionUntypedAssign.getColumn(), "pattern matching");
		return null;
	}
	
	@Override
	public Object visit(CaseExpressionObjectTypeAssign caseExpressionObjectTypeAssign) {
		raiseErrorNoUse(caseExpressionObjectTypeAssign.getLine(), caseExpressionObjectTypeAssign.getColumn(), "pattern matching");
		return null;
	}
	
	@Override
	public Object visit(CaseExpressionAssignTuple caseExpressionAssignTuple) {
		raiseErrorNoUse(caseExpressionAssignTuple.getLine(), caseExpressionAssignTuple.getColumn(), "pattern matching");
		return null;
	}
	
	@Override
	public Object visit(CaseExpressionOr caseExpressionOr) {
		raiseErrorNoUse(caseExpressionOr.getLine(), caseExpressionOr.getColumn(), "pattern matching");
		return null;
	}
	
	@Override
	public Object visit(CaseExpressionPost caseExpressionPost) {
		raiseErrorNoUse(caseExpressionPost.getLine(), caseExpressionPost.getColumn(), "pattern matching");
		return null;
	}

	@Override
	public Object visit(CaseExpressionPre caseExpressionPre) {
		raiseErrorNoUse(caseExpressionPre.getLine(), caseExpressionPre.getColumn(), "pattern matching");
		return null;
	}
	
	@Override
	public Object visit(CaseExpressionTuple caseExpressionTuple) {
		raiseErrorNoUse(caseExpressionTuple.getLine(), caseExpressionTuple.getColumn(), "pattern matching");
		return null;
	}

	@Override
	public Object visit(CaseExpressionWrapper caseExpressionWrapper) {
		raiseErrorNoUse(caseExpressionWrapper.getLine(), caseExpressionWrapper.getColumn(), "pattern matching");
		return null;
	}

	@Override
	public Object visit(CastExpression castExpression) {
		this.addItem("((");
		castExpression.t.accept(this);
		this.addItem(")(");
		castExpression.o.accept(this);
		this.addItem("))");
		return null;
	}

	@Override
	public Object visit(CatchBlocks catchBlocks) {
		raiseErrorNoUse(catchBlocks.getLine(), catchBlocks.getColumn(), "try catch");
		return null;
	}
	

	@Override
	public Object visit(Changed changed) {
		raiseErrorNoUse(changed.getLine(), changed.getColumn(), "changed");
		return null;
	}

	@Override
	public Object visit(ClassDef classDef) {
		raiseErrorNoUse(classDef.getLine(), classDef.getColumn(), "class definition");
		return null;
	}

	@Override
	public Object visit(ClassDefArg classDefArg) {
		raiseErrorNoUse(classDefArg.getLine(), classDefArg.getColumn(), "class arguments");
		return null;
	}

	@Override
	public Object visit(ClassDefArgs classDefArgs) {
		raiseErrorNoUse(classDefArgs.getLine(), classDefArgs.getColumn(), "class arguments");
		return null;
	}

	@Override
	public Object visit(ConstructorDef funcDef) {
		raiseErrorNoUse(funcDef.getLine(), funcDef.getColumn(), "constructors");
		return null;
	}

	@Override
	public Object visit(ContinueStatement continueStatement) {
		this.addItem("continue");
		
		if(null != continueStatement.returns){
			raiseErrorNoUse(continueStatement.getLine(), continueStatement.getColumn(), "continue returning a value");
		}
		return null;
	}
	
	@Override
	public Object visit(CopyExpression copyExpression) {
		raiseErrorNoUse(copyExpression.getLine(), copyExpression.getColumn(), "copy");
		return null;
	}

	@Override
	public Object visit(DeleteStatement deleteStatement){
		raiseErrorNoUse(deleteStatement.getLine(), deleteStatement.getColumn(), "del");
		return null;
	}

	@Override
	public Object visit(DMANewFromExpression dmaNewFromExpression){
		raiseErrorNoUse(dmaNewFromExpression.getLine(), dmaNewFromExpression.getColumn(), "offHeap");
		return null;
	}

	@Override
	public Object visit(DotOperator dotOperator) {
		
		if(dotOperator.getFoldedConstant() != null) {
			this.addItem(dotOperator.getFoldedConstant().toString());
		}else {
			ArrayList<Expression> elements = dotOperator.getElements(this); 
			ArrayList<Boolean> isDirectAccess = dotOperator.getIsDirectAccess(this);
			
			//assert elements.size() == isDirectAccess.size()-1;
			
			for(int n=0; n<elements.size(); n++)
			{
				Expression e = elements.get(n);
				
				e.accept(this);
				
				if(n != elements.size()-1)
				{
					boolean isDirect = isDirectAccess.get(n);
					boolean retSelf = dotOperator.returnCalledOn.get(n);
					boolean isSafe = dotOperator.safeCall.get(n);
					
					if(isDirect || retSelf || isSafe) {
						raiseErrorNoUse(e.getLine(), e.getColumn(), "direct, safe or self returning dot operator");
					}
					
					this.addItem(".");
				}
			}
		}
		
		
		
		return null;
	}

	@Override
	public Object visit(DottedAsName dottedAsName) {
		//visitList.add("DottedAsName");
		//ignore
		
		return null;
	}

	@Override
	public Object visit(DottedNameList dottedNameList) {
		//visitList.add("DottedNameList");
		//ignore
		
		return null;
	}

	@Override
	public Object visit(DuffAssign duffAssign) {
		duffAssign.e.accept(this);
		
		return null;
	}

	@Override
	public Object visit(ElifUnit elifUnit) {
		//visitList.add("ElifUnit");
		this.addItem("else if");
		this.addItem("(");
		elifUnit.eliftest.accept(this);
		this.addItem(")");
		elifUnit.elifb.accept(this);
		
		return null;
	}

	@Override
	public Object visit(EnumDef enumDef){
		raiseErrorNoUse(enumDef.getLine(), enumDef.getColumn(), "enums");
		return null;
	}


	
	@Override
	public Object visit(EnumItem enumItem){
		raiseErrorNoUse(enumItem.getLine(), enumItem.getColumn(), "enums");
		return null;
	}
	
	@Override
	public Object visit(EqReExpression equalityExpression) {
		if(equalityExpression.getFoldedConstant() != null) {
			this.addItem(equalityExpression.getFoldedConstant().toString());
		}else {
			//visitList.add("EqReExpression");
			
			equalityExpression.head.accept(this);
			for(GrandLogicalElement e: equalityExpression.elements)
			{
				e.accept(this);
			}
		}
		
		return null;
	}
	
	@Override
	public Object visit(ExpressionList expressionList) {
		return null;//should pass through to thing its meant to be
	}

	@Override
	public Object visit(ForBlock forBlock) {
		raiseErrorNoUse(forBlock.getLine(), forBlock.getColumn(), "this variant of for", "for( ; ; ) {} is valid");
		return null;
	}
	
	@Override
	public Object visit(ForBlockOld forBlockOld) {
		//visitList.add("ForBlockOld");
		
		if(forBlockOld.origParFor != null) {//no parforsync etc
			raiseErrorNoUse(forBlockOld.getLine(), forBlockOld.getColumn(), "" + forBlockOld.origParFor);
		}
		
		this.addItem("for("); 
		
		if(null != forBlockOld.assignExpr) 
		{
			forBlockOld.assignExpr.accept(this);
		}
		else if(null != forBlockOld.assignName)
		{
			if( null != forBlockOld.assigType) {
				forBlockOld.assigType.accept(this);
			}else {
				forBlockOld.assigFrom.getTaggedType().accept(this);
			}
			this.addItem(forBlockOld.assignName);
			this.addItem(" = ");
			if( null != forBlockOld.assigFrom) {
				forBlockOld.assigFrom.accept(this);
			}
		}
		this.addItem("; ");
		if(null != forBlockOld.check) forBlockOld.check.accept(this);
		this.addItem("; ");
		if(null != forBlockOld.postExpr) forBlockOld.postExpr.accept(this);
		this.addItem(")");
		forBlockOld.block.accept(this);

		if(forBlockOld.elseblock != null){
			raiseErrorNoUse(forBlockOld.getLine(), forBlockOld.getColumn(), "for block with else");
		}
		
		return null;
	}

	public static class ClinitOrFuncDef{
		public String isClinit;
		public FuncDef fd;

		public ClinitOrFuncDef(String isClinit) {
			this.isClinit = isClinit;
		}
		
		public ClinitOrFuncDef(FuncDef fd) {
			this.fd = fd;
		}
		
		@Override
		public boolean equals(Object an) {
			if(an instanceof ClinitOrFuncDef) {
				ClinitOrFuncDef asAn = (ClinitOrFuncDef)an;
				return asAn.isClinit == this.isClinit && Objects.equal(asAn.fd, this.fd);
			}
			return false;
		}

		@Override
		public int hashCode() {
			int ret = isClinit==null?0:isClinit.hashCode();
			ret += fd == null?0:fd.hashCode();
			return ret;
		}
		
		@Override
		public String toString() {
			if(isClinit != null) {
				return isClinit;
			}else {
				return this.fd.toString();
			}
		}
	}
	
	private Stack<HashSet<ClinitOrFuncDef>> dependancies = new Stack<HashSet<ClinitOrFuncDef>>(); 
	
	@Override
	public Object visit(FuncDef funcDef) {
		
		if(!funcDef.isAutoGennerated ){
			if(funcDef.isGPUKernalOrFunction != null ){
				//visitList.add("FuncDef"); //TODO: sync only on class methods (non static)
				
				if(funcDef.annotations != null){
					//raiseErrorNoUse(funcDef.getLine(), funcDef.getColumn(), "function annotations");
				}
				
				gpuKorFuncStrRep = new StringBuilder();
				
				if("<init>".equals(funcDef.funcName)){
					raiseErrorNoUse(funcDef.getLine(), funcDef.getColumn(), "constructors");
				}else{
					if(funcDef.isGPUKernalOrFunction == GPUFuncVariant.gpukernel) {
						this.addItem("__kernel void ");
					}else {
						this.addItem(funcDef.retType + " ");
					}
					
					if(funcDef.extFunOn != null){
						raiseErrorNoUse(funcDef.getLine(), funcDef.getColumn(), "extension functions");
					}
					
					this.addItem(getFuncDefName(funcDef, funcDef.funcName));
				}
				
				if(funcDef.methodGenricList != null && !funcDef.methodGenricList.isEmpty()){
					raiseErrorNoUse(funcDef.getLine(), funcDef.getColumn(), "local generics");
				}
				
				this.addItem("(");
				if(null!=funcDef.params) funcDef.params.accept(this);
				this.addItem(")");
				
				dependancies.push(new HashSet<ClinitOrFuncDef>());
				
				if(funcDef.funcblock != null){
					funcDef.funcblock.accept(this);
				}
				
				HashSet<ClinitOrFuncDef> deps = dependancies.pop();
				
				String cversion = gpuKorFuncStrRep.toString();
				
				if(funcDef.isGPUStubFunction()) {
					Annotations annots = funcDef.getAnnotations();
					for(Annotation annot : annots.annotations) {
						if(ScopeAndTypeChecker.const_Annotation_GPUStubFunction.equals(annot.getTaggedType())) {
							
							if(annot.getArguments().isEmpty()) {//if its a stub function to something which exists already
								cversion=null;
							}else {
								cversion = "";
							}
							break;
							
						}
					}
				}
				
				funcDef.gpuKernelFuncDetails = makeGPUKernelFuncDetails(funcDef, cversion, deps);
				
				//System.err.println("" + gpuKorFuncStrRep.toString());
				
				gpuKorFuncStrRep = null; 
				return null;
			}
			else if(gpuKorFuncStrRep != null) {//funcdef nested within a gpu func etc - not permitted!
				this.raiseError(funcDef.getLine(), funcDef.getColumn(), "functions may not be nested within gpu kernels or gpu functions");
			}
		}
		
		return null;
	}
	
	private static String getFuncDefName(FuncDef fd, String normal) {
		if(fd.isGPUKernalFuncOrStub()) {
			return normal;
		}
		
		return fd.origin.toStringFunc('_') + "_" + normal;
	}
	
	private static GPUKernelFuncDetails makeGPUKernelFuncDetails(FuncDef funcDef, String source, HashSet<ClinitOrFuncDef> deps) {

		//TOOD: analyize dependancies

		StringBuilder sigBuilder= new StringBuilder("(");
		String globalLocalConstant = "";
		String inout = "";
		
		for(FuncParam fp : funcDef.params.params) {
			sigBuilder.append(fp.type.getBytecodeType());
			globalLocalConstant += fp.gpuVarQualifier == null ? " " : fp.gpuVarQualifier.shortName();
			inout +=  fp.gpuInOutFuncParamModifier== null ? " " : fp.gpuInOutFuncParamModifier.getShortName();
		}
		
		sigBuilder.append(")");
		if(null != funcDef.retType) {
			sigBuilder.append(funcDef.retType.getBytecodeType());
		}
		
		String signature = sigBuilder.toString();
		
		GPUKernelFuncDetails[] dependancyList;
		if(deps != null) {
			dependancyList = new GPUKernelFuncDetails[deps.size()];
			int n=0;
			for(ClinitOrFuncDef cfd : deps) {
				GPUKernelFuncDetails dep;
				if( null != cfd.fd) {
					dep = makeGPUKernelFuncDetails(cfd.fd, null, null);
					dep.dclass = cfd.fd.origin.getClassName();
				}else {
					dep = new GPUKernelFuncDetails(-1, "<clinit>", "", "", new GPUKernelFuncDetails[0], "", "");
					dep.dclass = cfd.isClinit;
				}
				dependancyList[n++]=dep;
			}
		}else {
			dependancyList = new GPUKernelFuncDetails[0];
		}
		
		
		int dims = 0;
		if(funcDef.kernelDim instanceof VarInt) {
			dims = ((VarInt)funcDef.kernelDim).inter;
		}
		
		
		return new GPUKernelFuncDetails(dims, source==null?funcDef.funcName:getFuncDefName(funcDef, funcDef.funcName), signature, source, dependancyList, globalLocalConstant, inout);
	}
	
	public NamedType getGPUbuiltinClass(){
		if(null == gpubuiltinClass) {//load only once
			try {
				gpubuiltinClass = new NamedType(new ClassDefJava(Class.forName("com.concurnas.lang.gpubuiltin")));
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Cannot load gpubuiltin definitions", e);
			}
		}
		return gpubuiltinClass;
	}
	
	private NamedType gpubuiltinClass = null; 
	
	
	@Override
	public Object visit(FuncInvoke funcInvoke) {
		//visitList.add("FuncInvoke");
		TypeAndLocation tal = funcInvoke.resolvedFuncTypeAndLocation;
		Type finv = tal.getType();
		
		if(!(finv instanceof FuncType)) {
			this.raiseError(funcInvoke.getLine(), funcInvoke.getColumn(), String.format("cannot invoke %s within a gpu kernel or function as %s", funcInvoke.funName, "it's not a gpu function or gpu kernel"));
		}else {
			Location loc = tal.getLocation();
			if((loc instanceof ClassFunctionLocation) || !(finv instanceof FuncType)){
				//if(!((ClassFunctionLocation)loc).owner.equals("com/concurnas/compiler/visitors/util/GPUThis")) {
				this.raiseError(funcInvoke.getLine(), funcInvoke.getColumn(), String.format("cannot invoke %s within a gpu kernel or function as %s", funcInvoke.funName, "it's a method and not a gpu function or gpu kernel"));
				//}
			}else {
				FuncType ft = (FuncType)finv;
				FuncDef ofd = ft.origonatingFuncDef;
				if( null == ofd ) {
					this.raiseError(funcInvoke.getLine(), funcInvoke.getColumn(), String.format("cannot invoke %s within a gpu kernel or function as %s", funcInvoke.funName,"it's not a gpu function or gpu kernel"));
				}else {
					if(!ofd.isGPUKernalFuncOrStub()) {
						this.raiseError(funcInvoke.getLine(), funcInvoke.getColumn(), String.format("cannot invoke %s within a gpu kernel or function as %s", funcInvoke.funName, "it's not a gpu function or gpu kernel"));
					}else {
						//great!
						boolean include = true;
						
						if(loc instanceof StaticFuncLocation) {//if from com.concurnas.lang.gpubuiltin ingnore as a dependancy
							StaticFuncLocation asSFL = (StaticFuncLocation)loc;
							include = !getGPUbuiltinClass().equals(asSFL.ownerType);
						}
						
						if(include) {
							this.dependancies.peek().add(new ClinitOrFuncDef(ofd));
						}
						

						this.addItem(!include?funcInvoke.funName: getFuncDefName(ofd, funcInvoke.funName));
						if(funcInvoke.genTypes != null && !funcInvoke.genTypes.isEmpty()){
							raiseErrorNoUse(funcInvoke.getLine(), funcInvoke.getColumn(), "generic types");
						}
						funcInvoke.args.accept(this);
						
					}
				}
			}
			
			
		}
		//TODO: move above to SATC?
		return null;
		
		
	}
	
	@Override
	public Object visit(FuncInvokeArgs funcInvokeArgs) {
		//visitList.add("FuncInvokeArgs");
		this.addItem("(");

		List<Expression> args = funcInvokeArgs.getArgumentsWNPs();
		
		int len = args.size();
		for(int n = 0; n < len; n++) {
			Expression e = args.get(n);
			if(e != null) {
				e.accept(this);
			}
			
			if(n != len-1) {
				this.addItem(", ");
			}
		}
		
		
		if(!funcInvokeArgs.nameMap.isEmpty()){
			raiseErrorNoUse(funcInvokeArgs.getLine(), funcInvokeArgs.getColumn(), "named params");
		}
		
		this.addItem(")");
		return null;
	}

	@Override
	public Object visit(FuncParam funcParam) {
		//visitList.add("FuncParam");
		boolean globallocalconst = false;
		if(funcParam.gpuVarQualifier != null) {
			this.addItem(funcParam.gpuVarQualifier.openClStr() + " ");
			globallocalconst=true;
		}
		
		if(funcParam.isFinal) {
			this.addItem("const ");
		}
		
		if(funcParam.type != null){
			funcParam.type.accept(this);
			if(globallocalconst /*&& funcParam.type.hasArrayLevels()*/) {
				this.addItem("*");
			}
		}
		
		this.addItem(funcParam.name);
		
		if(funcParam.isLazy){
			raiseErrorNoUse(funcParam.getLine(), funcParam.getColumn(), "lazy variables");
		}
		
		return null;
	}
	
	@Override
	public Object visit(FuncParams funcParams) {
		//visitList.add("FuncParams");
		int len = funcParams.params.size();
		
		for(int n=0; n < len; n++)
		{
			FuncParam f = funcParams.params.get(n);
			f.accept(this);
			if(n != len-1) {
				this.addItem(", ");
			}
		}
		
		return null;
	}
	
	@Override
	public Object visit(FuncRef funcRef) {
		raiseErrorNoUse(funcRef.getLine(), funcRef.getColumn(), "method references");
		return null;
	}

	@Override
	public Object visit(FuncRefArgs funcRefArgs) {
		raiseErrorNoUse(funcRefArgs.getLine(), funcRefArgs.getColumn(), "method references");
		return null;
	}
	
	@Override
	public Object visit(FuncRefInvoke funcRefInvoke) {
		raiseErrorNoUse(funcRefInvoke.getLine(), funcRefInvoke.getColumn(), "method references");
		return null;
	}

	@Override
	public Object visit(FuncType funcType) {
		//visitList.add("FuncType");
		return null;
	}
	
	@Override
	public Object visit(GenericType genericType) {
		//visitList.add("GenericType");
		return null;
	}

	@Override
	public Object visit(GetSetOperation getSetOperation) {
		return null;
	}

	@Override
	public Object visit(GrandLogicalElement equalityElement) {
		//visitList.add("EqualityElement");
		String got = equalityElement.compOp.gpuVariant;
		if(got == null) {
			raiseErrorNoUse(equalityElement.getLine(), equalityElement.getColumn(), ""+equalityElement.compOp, "as there is no equivalent for this");
		}else {
			this.addItem(" " + got + " ");
		}

		this.addItem("(");
		equalityElement.e2.accept(this);
		this.addItem(")");
		
		return null;
	}

/*	@Override
	public Object visit(IfExpr ifExpr) {
		if(ifExpr.getFoldedConstant() != null) {
			this.addItem(ifExpr.getFoldedConstant().toString());
		}else {
			//result = a > b ? x : y;
			this.addItem("(");
			ifExpr.test.accept(this);
			this.addItem(") ? (");
			ifExpr.op1.accept(this);
			this.addItem(") : (");
			ifExpr.op2.accept(this);
			this.addItem(")");
		}
		
		return null;
	}
*/
	@Override
	public Object visit(IfStatement ifStatement) {
		if(ifStatement.getFoldedConstant() != null) {
			this.addItem(ifStatement.getFoldedConstant().toString());
		}else if(ifStatement.canBeConvertedIntoIfExpr) {
			this.addItem("(");
			ifStatement.iftest.accept(this);
			this.addItem(") ? (");
			ifStatement.ifblock.getFirstLine().accept(this);
			this.addItem(") : (");
			ifStatement.elseb.getFirstLine().accept(this);
			this.addItem(")");
		}else {
			//visitList.add("IfStatement");
			this.addItem("if");
			this.addItem("(");
			ifStatement.iftest.accept(this);
			this.addItem(")");
			ifStatement.ifblock.accept(this);
			for(ElifUnit u : ifStatement.elifunits)
			{
				u.accept(this);
			}
			if(ifStatement.elseb!=null)
			{
				this.addItem("else");
				ifStatement.elseb.accept(this);
			}
		}
		
		return null;
	}

	@Override
	public Object visit(ImportAsName importAsName) {
		return null;
	}

	@Override
	public Object visit(ImportFrom importFrom) {
		return null;
	}

	@Override
	public Object visit(ImportImport importImport) {
		return null;
	}

	@Override
	public Object visit(ImportStar importStar) {
		return null;
	}
	
	@Override
	public Object visit(InExpression cont){
		raiseErrorNoUse(cont.getLine(), cont.getColumn(), "in");
		return null;
	}

	@Override
	public Object visit(InitBlock initBlock){
		raiseErrorNoUse(initBlock.getLine(), initBlock.getColumn(), "init");
		return null;
	}
	
	@Override
	public Object visit(Is instanceOf) {
		raiseErrorNoUse(instanceOf.getLine(), instanceOf.getColumn(), "is");
		return null;
	}
	
	@Override
	public Object visit(JustAlsoCaseExpression justAlsoCaseExpression){
		raiseErrorNoUse(justAlsoCaseExpression.getLine(), justAlsoCaseExpression.getColumn(), "pattern matching");
		return null;
	}

	@Override
	public Object visit(LambdaDef lambdaDef) {
		raiseErrorNoUse(lambdaDef.getLine(), lambdaDef.getColumn(), "lambdas");
		return null;
	}


	@Override
	public Object visit(LocalClassDef localClassDef) {
		raiseErrorNoUse(localClassDef.getLine(), localClassDef.getColumn(), "local classes");
		return null;
	}

	@Override
	public Object visit(MapDef mapDef) {
		raiseErrorNoUse(mapDef.getLine(), mapDef.getColumn(), "maps");
		return null;
	}

	@Override
	public Object visit(MapDefaultElement mapDefElement) {
		raiseErrorNoUse(mapDefElement.getLine(), mapDefElement.getColumn(), "maps");
		return null;
	}

	@Override
	public Object visit(MapDefElement mapDef) {
		raiseErrorNoUse(mapDef.getLine(), mapDef.getColumn(), "maps");
		return null;
	}

	@Override
	public Object visit(MatchStatement matchStatement) {
		raiseErrorNoUse(matchStatement.getLine(), matchStatement.getColumn(), "pattern matching");
		return null;
	}
	

	@Override
	public Object visit(MulerElement mulerElement) {
		return null;
	}

	private void processMulEle(MulerElement mulerElement, Type lhsType) {
		Type rhsType = mulerElement.expr.getTaggedType();
		
		boolean rhsFloatetc = ScopeAndTypeChecker.const_float.equals(rhsType) || ScopeAndTypeChecker.const_double.equals(rhsType);
		boolean lhsFloatetc = ScopeAndTypeChecker.const_float.equals(lhsType) || ScopeAndTypeChecker.const_double.equals(lhsType);
		
		if(mulerElement.mulOper == MulerExprEnum.MOD && (lhsFloatetc || rhsFloatetc)) {
			//remap: a/b mod 2.f => fmod(a/b, 2.f)
			String prev = gpuKorFuncStrRep.toString();
			gpuKorFuncStrRep = new StringBuilder();
			
			gpuKorFuncStrRep.append(String.format("fmod(%s, ", prev));
			mulerElement.expr.accept(this);
			this.addItem(")");
			
			
		}else {
			this.addItem(" " + mulerElement.mulOper.gpuVariant + " ");

			this.addItem("(");
			mulerElement.expr.accept(this);
			this.addItem(")");
		}
	}
	
	@Override
	public Object visit(MulerExpression mulerExpression) {
		
		if(mulerExpression.getFoldedConstant() != null) {
			this.addItem(mulerExpression.getFoldedConstant().toString());
		}else {
			StringBuilder prev = gpuKorFuncStrRep;
			gpuKorFuncStrRep = new StringBuilder();
			
			this.addItem("(");
			mulerExpression.header.accept(this);
			Type lhsType = mulerExpression.header.getTaggedType();
			this.addItem(")");
			
			for(MulerElement e: mulerExpression.elements){
				if(e.astOverrideOperatorOverload != null) {
					e.astOverrideOperatorOverload.accept(this);
				}else {
					processMulEle(e, lhsType);
				}
				lhsType = e.getTaggedType();
			}
			
			prev.append(gpuKorFuncStrRep);
			gpuKorFuncStrRep = prev;
		}
		
		
		
		return null;
	}

	@Override
	public Object visit(MultiType multiType) {
		raiseErrorNoUse(multiType.getLine(), multiType.getColumn(), "multi types");
		return null;
	}

	@Override
	public Object visit(NamedType namedType) {
		//this.addItem(namedType.toString());
		raiseErrorNoUse(namedType.getLine(), namedType.getColumn(), "non primative types");
		return null;
	}
	
	@Override
	public Object visit(New nc) {
		//visitList.add("NamedConstructor");
		/*this.addItem("new");
		if(null != namedConstructor.typeee){
			namedConstructor.typeee.accept(this);
		}
		
		if(null != namedConstructor.args){
			namedConstructor.args.accept(this);
		}*/

		raiseErrorNoUse(nc.getLine(), nc.getColumn(), "new");
		return null;
		
	}

	@Override
	public Object visit(NOP nop) {
		return null;
	}

	@Override
	public Object visit(NotExpression notExpression) {
		if(notExpression.getFoldedConstant() != null) {
			this.addItem(notExpression.getFoldedConstant().toString());
		}else {
			this.addItem("!");

			this.addItem("(");
			notExpression.expr.accept(this);
			this.addItem(")");
		}
		
		return null;
	}

	@Override
	public Object visit(OnChange onChange) {
		raiseErrorNoUse(onChange.getLine(), onChange.getColumn(), "onchange");
		return null;//null is ok
	}

	@Override
	public Object visit(OnEvery onEvery) {
		raiseErrorNoUse(onEvery.getLine(), onEvery.getColumn(), "every");
		return null;//null is ok
	}
	
	@Override
	public Object visit(OrExpression orExpression) {
		if(orExpression.getFoldedConstant() != null) {
			this.addItem(orExpression.getFoldedConstant().toString());
		}else {
			//visitList.add("OrExpression");
			orExpression.head.accept(this);
			for(Expression i : orExpression.things)
			{
				this.addItem(" || ");
				i.accept(this);
			}
		}
		
		
		return null;
	}

	@Override
	public Object visit(PostfixOp postfixOp) {
		//visitList.add("PostfixOp");
		if(postfixOp.getTaggedType().getGpuMemSpace() == GPUVarQualifier.CONSTANT) {
			this.raiseError(postfixOp.getLine(), postfixOp.getColumn(), "constant variables cannot be reassigned");
		}
		
		postfixOp.p2.accept(this);
		this.addItem(""+postfixOp.postfix);
		return null;
	}
	
	@Override
	public Object visit(NotNullAssertion notNullAssertion) {
		notNullAssertion.expr.accept(this);
		this.raiseError(notNullAssertion.getLine(), notNullAssertion.getColumn(), "not null assertion ??");
		return null;
	}
	@Override
	public Object visit(ElvisOperator elvisOperator) {
		elvisOperator.lhsExpression.accept(this);
		elvisOperator.rhsExpression.accept(this);
		this.raiseError(elvisOperator.getLine(), elvisOperator.getColumn(), "elvis operator ?:");
		return null;
	}

	@Override
	public Object visit(PowOperator powOperator) {
		if(powOperator.getFoldedConstant() != null) {
			this.addItem(powOperator.getFoldedConstant().toString());
		}else {
			PrimativeTypeEnum castTo = null;
			
			PrimativeTypeEnum expPrimT = null;
			PrimativeTypeEnum raiseTypeT = null;
			
			Type expType = powOperator.expr.getTaggedType();
			if(expType instanceof PrimativeType) {
				expPrimT = ((PrimativeType) expType).type;
				
				castTo = expPrimT == PrimativeTypeEnum.FLOAT?PrimativeTypeEnum.FLOAT:PrimativeTypeEnum.DOUBLE;
			}else {
				this.raiseError(powOperator.expr.getLine(), powOperator.expr.getColumn(), "non primative type in power operator");
			}
			
			Type raiseType = powOperator.raiseTo.getTaggedType();
			if(raiseType instanceof PrimativeType) {

				raiseTypeT = ((PrimativeType) expType).type;
				
				if(!(null != castTo && castTo == PrimativeTypeEnum.DOUBLE)) {
					castTo = raiseTypeT == PrimativeTypeEnum.FLOAT?PrimativeTypeEnum.FLOAT:PrimativeTypeEnum.DOUBLE;
				}
			}else {
				this.raiseError(powOperator.raiseTo.getLine(), powOperator.raiseTo.getColumn(), "non primative type in  power operator");
			}
			
			if(null != expPrimT && null != raiseTypeT) {
				//visitList.add("PostfixOp");
				
				if(castTo != null) {
					//this.addItem(String.format("((%s)pow(((%s)%s), ((%s)%s)))",  ));
					this.addItem("((");
					Type castBack = TypeCheckUtils.bestPrim((PrimativeType)expType, (PrimativeType)raiseType);
					castBack.accept(this);
					this.addItem(")pow(((");
					new PrimativeType(castTo).accept(this);
					this.addItem(")");
					powOperator.expr.accept(this);
					this.addItem("), ((");
					new PrimativeType(castTo).accept(this);
					this.addItem(")");
					powOperator.raiseTo.accept(this);
					this.addItem(")))");
					
				}else {
					this.addItem("pow(");
					powOperator.expr.accept(this);
					this.addItem(", ");
					powOperator.raiseTo.accept(this);
					this.addItem(")");
				}
				
			}
			
		}
		
		
		return null;
	}

	@Override
	public Object visit(PrefixOp prefixOp) {
		//visitList.add("PrefixOp");
		if(prefixOp.getTaggedType().getGpuMemSpace() == GPUVarQualifier.CONSTANT) {
			this.raiseError(prefixOp.getLine(), prefixOp.getColumn(), "constant variables cannot be reassigned");
		}
		this.addItem(" " + prefixOp.prefix.gpuVersion + " ");
		prefixOp.p1.accept(this);
		return null;
	}

	@Override
	public Object visit(PrimativeType primativeType) {
		String ret;
		if(primativeType.type == PrimativeTypeEnum.BOOLEAN) {
			ret = "_Bool ";
		}else {
			ret = primativeType.type + " ";
		}
		
		int pnt = primativeType.getPointer();
		if(pnt != 0) {
			StringBuilder str = new StringBuilder("*");
			for(int n =0; n < pnt-1; n++) {
				str.append("*");
			}
			ret += str;
		}
		//this.addItem(primativeType.toString());
		this.addItem(ret);
		return null;
	}

	@Override
	public Object visit(RefBoolean refBoolean) {
		this.addItem(""+refBoolean.b);
		return null;
	}


	@Override
	public Object visit(RefClass refClass) {
		raiseErrorNoUse(refClass.getLine(), refClass.getColumn(), ".class");
		return null;
	}
	
	@Override
	public Object visit(RefName refName) {
		//visitList.add("RefName");
		TypeAndLocation tal = refName.resolvesTo;
		boolean addName = true;
		if(tal != null) {
			Location loc = tal.getLocation();
			if(loc != null) {
				
				if(loc instanceof FuncLocation) {
					raiseErrorNoUse(refName.getLine(), refName.getColumn(), "non local or constant top level variable");
				}
				else if (loc instanceof LocationStaticField /* && ((LocationStaticField)loc) */ ) { 
					LocationStaticField lsf = (LocationStaticField)loc;
					Type tt = lsf.type;
					if(tt != null) {
						if(tt.getGpuMemSpace() == GPUVarQualifier.CONSTANT) {
							if(!this.dependancies.isEmpty()) {
								this.dependancies.peek().add(new ClinitOrFuncDef(lsf.owner));
							}
						}else {
							raiseErrorNoUse(refName.getLine(), refName.getColumn(), "non constant top level variable");
						}
					}
				}
			}
		}
		
		if(addName) {
			this.addItem(refName.name);
		}
		
		return null;
	}
	
	@Override
	public Object visit(RefOf refThis) {
		raiseErrorNoUse(refThis.getLine(), refThis.getColumn(), "of");
		return null;
	}

	@Override
	public Object visit(RefSuper refSuper) {
		raiseErrorNoUse(refSuper.getLine(), refSuper.getColumn(), "super");
		return null;
	}

	@Override
	public Object visit(RefThis refThis) {
		raiseErrorNoUse(refThis.getLine(), refThis.getColumn(), "this");
		return null;
	}

	@Override
	public Object visit(ReturnStatement returnStatement) {
		this.addItem("return ");
		if(null!=returnStatement.ret) returnStatement.ret.accept(this);
		
		return null;
	}

	@Override
	public Object visit(ShiftElement shiftElement) {
		//visitList.add("MulerElement");
		if(!shiftElement.shiftOp.validForGPU) {
			raiseErrorNoUse(shiftElement.getLine(), shiftElement.getColumn(), ""+shiftElement.shiftOp);
		}
		
		this.addItem(" "+shiftElement.shiftOp + " ");
		shiftElement.expr.accept(this);
		return null;
	}

	@Override
	public Object visit(ShiftExpression shiftExpression) {
		if(shiftExpression.getFoldedConstant() != null) {
			this.addItem(shiftExpression.getFoldedConstant().toString());
		}else {
			shiftExpression.header.accept(this);
			for(ShiftElement e: shiftExpression.elements)
			{
				e.accept(this);
			}
		}
		
		
		return null;
	}

	@Override
	public Object visit(SizeofStatement sizeofstmt){
		if(sizeofstmt.variant != null) {
			raiseErrorNoUse(sizeofstmt.getLine(), sizeofstmt.getColumn(), "sizeof with qualification");
		}
		
		this.addItem("sizeof(");
		sizeofstmt.e.accept(this);
		this.addItem(")");
		
		return null;
	}
	
	@Override
	public Object visit(SuperConstructorInvoke superConstructorInvoke) {
		raiseErrorNoUse(superConstructorInvoke.getLine(), superConstructorInvoke.getColumn(), "constructor invocation");
		return null;
	}

	@Override
	public Object visit(ThisConstructorInvoke thisConstructorInvoke) {
		raiseErrorNoUse(thisConstructorInvoke.getLine(), thisConstructorInvoke.getColumn(), "constructor invocation");
		return null;
	}

	@Override
	public Object visit(ThrowStatement throwStatement) {
		raiseErrorNoUse(throwStatement.getLine(), throwStatement.getColumn(), "exceptions");
		return null;
	}

	@Override
	public Object visit(TransBlock transBlock) {
		raiseErrorNoUse(transBlock.getLine(), transBlock.getColumn(), "transactions");
		return null;
	}

	@Override
	public Object visit(TryCatch tryCatch) {
		raiseErrorNoUse(tryCatch.getLine(), tryCatch.getColumn(), "exceptions");
		return null;
	}

	@Override
	public Object visit(TypedCaseExpression typedCaseExpression){
		raiseErrorNoUse(typedCaseExpression.getLine(), typedCaseExpression.getColumn(), "pattern matching");
		
		return null;
	}

	@Override
	public Object visit(TypedefStatement typedefStatement) {
		//raiseErrorNoUse(typedefStatement.getLine(), typedefStatement.getColumn(), "typedef declarations");
		return null;
	}

	@Override
	public Object visit(TypeReturningExpression typeReturningExpression) {//not used
		return null;
	}
	
	@Override
	public Object visit(VarChar varString) {
		this.addItem("'"+varString.chr+"'");
		
		return null;
	}
	
	@Override
	public Object visit(VarDouble varDouble) {
		this.addItem(""+varDouble.doubler);
		return null;
	}


	@Override
	public Object visit(VarFloat varFloat) {
		this.addItem(""+varFloat.floater + "f");
		return null;
	}
	
	@Override
	public Object visit(VarInt varInt) {
		this.addItem(""+varInt.inter);
		return null;
	}

	@Override
	public Object visit(VarLong varLong) {
		this.addItem(""+varLong.longer + "L");
		return null;
	}

	@Override
	public Object visit(VarNull varNull) {
		this.addItem("NULL");
		
		return null;
	}

	@Override
	public Object visit(VarRegexPattern varString) {
		raiseErrorNoUse(varString.getLine(),varString.getColumn(), "regex");
		return null;
	}
	
	@Override
	public Object visit(VarShort varShort) {
		this.addItem("(short)"+varShort.shortx);
		return null;
	}
	@Override
	public Object visit(VarString varString) {
		raiseErrorNoUse(varString.getLine(),varString.getColumn(), "strings");
		return null;
	}
	
	@Override
	public Object visit(Vectorized vectorized) {
		return null;//should goto ast version
	}
	
	@Override
	public Object visit(VectorizedArrayRef arrayRef) {
		return null;//should goto ast version
	}
	
	@Override
	public Object visit(VectorizedFieldRef vectorizedFieldRef) {
		return null;//should goto ast version
	}
	
	@Override
	public Object visit(VectorizedFuncInvoke vectorizedFuncInvoke) {
		return null;//should goto ast version
	}
	
	@Override
	public Object visit(VectorizedFuncRef vectorizedFuncRef) {
		return null;//should goto ast version
	}
	
	@Override
	public Object visit(VectorizedNew vectorizedNew) {
		return null;//should goto ast version
	}
	
	@Override
	public Object visit(WhileBlock whileBlock) {
		//visitList.add("WhileBlock");
		this.addItem("while");
		this.addItem("(");
		whileBlock.cond.accept(this);
		
		if(whileBlock.idxVariableCreator != null){
			raiseErrorNoUse(whileBlock.getLine(),whileBlock.getColumn(), "while block with idx");
		}
		else if(whileBlock.idxVariableAssignment != null){
			raiseErrorNoUse(whileBlock.getLine(),whileBlock.getColumn(), "while block with idx");
		}
		
		this.addItem(")");
		whileBlock.block.accept(this);

		if(whileBlock.elseblock != null){
			raiseErrorNoUse(whileBlock.getLine(),whileBlock.getColumn(), "while block else");
		}
		return null;
	}
	

	@Override
	public Object visit(Annotations annotations) {
		raiseErrorNoUse(annotations.getLine(),annotations.getColumn(), "annotations");
		
		return null;
	}
	
	@Override
	public Object visit(WithBlock withBlock) {
		raiseErrorNoUse(withBlock.getLine(),withBlock.getColumn(), "with");
		
		return null;
	}
	
	private int indentLevel = 0;
	
	private void visitBlock(Block block, boolean addBraces) {
		//visitList.add("Block");
		if(addBraces){
			this.addItem("{\n");
		}
		indentLevel++;
		LineHolder lh = block.startItr();
		
		while(lh != null)
		{
			Line l = lh.l;
			if(null != l){
				String prefix = "";
				for(int n=0; n < indentLevel; n++) {
					prefix += " ";
				}
				this.addItem(prefix);
				
				l.accept(this);
				if(l instanceof NOP){
					this.addItem("\n");
				}
				else if(!(l instanceof CompoundStatement)){
					//this.addItem("; <-- "+l.getClass() + "\n");
					this.addItem(";\n");
				}
			}
			
				
			lh = block.getNext();
		}
		indentLevel--;
		if(addBraces){
			this.addItem("}\n");
		}
	}


	@Override
	public Object visit(EnumBlock enumBlock) {
		return null;
	}

	@Override
	public void resetLastLineVisited() {
		
	}

	@Override
	public int getLastLineVisited() {
		return 0;
	}

	@Override
	public void setLastLineVisited(int lineNo) {
		
	}

	@Override
	public Object visit(ArrayDefComplex arrayDefComplex) {
		//visitList.add("ArrayDef");
		raiseErrorNoUse(arrayDefComplex.getLine(), arrayDefComplex.getColumn(), "array definition outside of new array varaible initalizer");
		return null;
	}

	@Override
	public Object visit(ImpliInstance impliInstance) {
		return null;
	}
	
	
	@Override
	public Object visit(PointerAddress pointerAddress) {
		this.addItem("&");
		pointerAddress.rhs.accept(this);
		return null;
	}
	
	
	@Override
	public Object visit(PointerUnref pointerAddress) {
		StringBuilder str = new StringBuilder("*");
		int pnt = pointerAddress.size;
		if(pnt != 0) {
			for(int n =0; n < pnt-1; n++) {
				str.append("*");
			}
		}

		this.addItem(str.toString());
		pointerAddress.rhs.accept(this);
		return null;
	}
	
	@Override
	public Object visit(JustLoad justLoad) {
		return null;//ignore, used in bytecode genneration only
	}

	@Override
	public Object visit(AssignMulti multiAssign) {
		int inst = multiAssign.assignments.size();
		ArrayList<Pair<String, Type>> toCreate = new ArrayList<Pair<String, Type>>(inst);
		ArrayList<Assign> toAssign = new ArrayList<Assign>(inst);
		
		for(Assign ass:  multiAssign.assignments) {
			if(ass instanceof AssignNew) {
				AssignNew asNew = (AssignNew)ass;
				
				if(asNew.eq != AssignStyleEnum.EQUALS && asNew.eq != AssignStyleEnum.EQUALS_STRICT) {
					raiseErrorNoUse(ass.getLine(), ass.getColumn(), "assignors in multi assign statements other than =");
				}
				
				toCreate.add(new Pair<String, Type>(asNew.name, asNew.type));
				
				AssignNew cop = (AssignNew)asNew.copy();
				cop.type = null;
				ass = cop;
			}
			else if(ass instanceof AssignExisting) {
				AssignExisting ase = (AssignExisting)ass;
				
				if(ase.eq != AssignStyleEnum.EQUALS && ase.eq != AssignStyleEnum.EQUALS_STRICT) {
					raiseErrorNoUse(ass.getLine(), ass.getColumn(), "assignors in multi assign statements other than =");
				}
				
				if(ase.isReallyNew) {
					if(ase.assignee instanceof RefName) {
						toCreate.add(new Pair<String, Type>(((RefName)ase.assignee).name, ase.getTaggedType()));
					}else {
						raiseErrorNoUse(ass.getLine(), ass.getColumn(), "assignment of this form");
					}

					ase = (AssignExisting)ase.copy();
					ase.setTaggedType(null);
					ass = ase;
				}
			}
			
			if(ass.gpuVarQualifier != null) {
				raiseError(ass.getLine(), ass.getColumn(), String.format("%s may not be used in a multi assign", ass.gpuVarQualifier));
			}
				
			
			toAssign.add(ass);
		}
		
		
		
		for(Pair<String, Type> toc : toCreate) {
			toc.getB().accept(this);
			this.addItem(toc.getA() + ";");
		}
		
		boolean anyConst=false;
		for(Assign ass: toAssign) {
			
			Expression was = ass.setRHSExpression(null);
			anyConst |= (Boolean)ass.accept(this);
			ass.setRHSExpression(was);
		}
		multiAssign.rhs.accept(this);
		return anyConst;
	}
	
	
	@Override
	public Object visit(TupleExpression tupleExpression) {
		raiseErrorNoUse(tupleExpression.getLine(),tupleExpression.getColumn(), "tuples");
		return null;
	}

	@Override
	public Object visit(AssignTupleDeref assignTupleDeref) {
		raiseErrorNoUse(assignTupleDeref.getLine(),assignTupleDeref.getColumn(), "tuple decomposition");
		return null;
	}

	@Override
	public Object visit(AnonLambdaDef anonLambdaDef) {
		raiseErrorNoUse(anonLambdaDef.getLine(),anonLambdaDef.getColumn(), "lambdas");
		return null;
	}
	
	@Override
	public Object visit(ObjectProvider objectProvider) {
		raiseErrorNoUse(objectProvider.getLine(),objectProvider.getColumn(), "providers");
		return null;
	}
	
	@Override
	public Object visit(ObjectProviderBlock objectProviderBlock) {
		return null;
	}
	@Override
	public Object visit(ObjectProviderLineDepToExpr objectProviderLineDepToExpr) {
		return null;
	}
	@Override
	public Object visit(ObjectProviderLineProvide objectProviderLineProvide) {
		return null;
	}

	@Override
	public Object visit(LangExt langExt) {
		raiseErrorNoUse(langExt.getLine(),langExt.getColumn(), "language extensions");
		return null;
	}
}
