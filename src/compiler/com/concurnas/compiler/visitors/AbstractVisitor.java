package com.concurnas.compiler.visitors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;

import com.concurnas.compiler.CaseExpression;
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
import com.concurnas.compiler.ast.util.JustLoad;
import com.concurnas.compiler.visitors.util.MactchCase;
import com.concurnas.runtime.Pair;

public abstract  class AbstractVisitor implements Visitor {

	protected int lastLineVisited = -1;
	public void resetLastLineVisited(){
		lastLineVisited = -1;
	}
	public int getLastLineVisited(){
		return lastLineVisited;
	}
	public void setLastLineVisited(int lineNo){
		if(lineNo <= 0){
			return;//HACK: messy, what if it actually is zero? - rare so doesnt matter
		}
		this.lastLineVisited = lineNo;
	}
	
	public void pushErrorContext(FuncDef xxx) {
	}
	public FuncDef popErrorContext(){
		return null;
	}
	
	
	@Override
	public Object visit(Additive addMinusExpression) {
		//lastLineVisited=addMinusExpression.getLine();
		addMinusExpression.head.accept(this);
		for(AddMinusExpressionElement i : addMinusExpression.elements)
		{
			i.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(AddMinusExpressionElement addMinusExpressionElement) {
		//lastLineVisited=addMinusExpressionElement.getLine();
		addMinusExpressionElement.exp.accept(this);
		return null;
	}

	@Override
	public Object visit(AndExpression andExpression) {
		//lastLineVisited=andExpression.getLine();
		andExpression.head.accept(this);
		for (Expression i : andExpression.things) {
			i.accept(this);
		}
		return null;
	}
	
	@Override
	public Object visit(BitwiseOperation bitwiseOperation) {
		bitwiseOperation.head.accept(this);
		for (Expression i : bitwiseOperation.things) {
			i.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(ArrayConstructor arrayConstructor) {
		//lastLineVisited=arrayConstructor.getLine();
		arrayConstructor.type.accept(this);
		for(Expression level : arrayConstructor.arrayLevels)
		{
			if(level != null){
				level.accept(this);
			}
		}

		if(arrayConstructor.defaultValue != null) {
			arrayConstructor.defaultValue.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(ArrayRefElementPrefixAll arrayRefElementPrefixAll) {
		//lastLineVisited=arrayRefElementPrefixAll.getLine();
		arrayRefElementPrefixAll.e1.accept(this);
		return null;
	}

	@Override
	public Object visit(ArrayRefElementPostfixAll arrayRefElementPostfixAll) {
		//lastLineVisited=arrayRefElementPostfixAll.getLine();
		arrayRefElementPostfixAll.e1.accept(this);
		return null;
	}

	@Override
	public Object visit(ArrayRefElementSubList arrayRef) {
		//lastLineVisited=arrayRef.getLine();
		arrayRef.e1.accept(this);
		arrayRef.e2.accept(this);
		return null;
	}

	@Override
	public Object visit(ArrayRefElement arrayRefElement) {
		//lastLineVisited=arrayRefElement.getLine();
		arrayRefElement.e1.accept(this);
		return null;
	}

	protected void processArrayElements(ArrayRefLevelElementsHolder elements) {
		for(Pair<Boolean, ArrayList<ArrayRefElement>> levels : elements.getAll()){
			ArrayList<ArrayRefElement> brackset = levels.getB();
			for(int n = 0; n < brackset.size(); n++)
			{	
				brackset.get(n).accept(this);
			}
		}
	}
	
	@Override
	public Object visit(ArrayRef arrayRef) {
		//lastLineVisited=arrayRef.getLine();
		arrayRef.expr.accept(this);
		processArrayElements(arrayRef.arrayLevelElements);
		return null;
	}

	@Override
	public Object visit(ArrayDef arrayDef) {
		//lastLineVisited=arrayDef.getLine();
		for(Expression e : arrayDef.getArrayElements(this))
		{
			e.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(AsyncRefRef asyncRefRef) {
		//lastLineVisited=asyncRefRef.getLine();
		asyncRefRef.b.accept(this);
		return null;
	}

	@Override
	public Object visit(AsyncBlock asyncBlock) {
		//lastLineVisited=asyncBlock.getLine();
		asyncBlock.body.accept(this);
		if(null != asyncBlock.executor)
		{
			asyncBlock.executor.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(AssignNew assignNew) {
		//lastLineVisited=assignNew.getLine();
		if(assignNew.annotations != null){
			assignNew.annotations.accept(this);
		}
		
		if(assignNew.type!=null) assignNew.type.accept(this);
		if(assignNew.expr!=null) assignNew.expr.accept(this);
		
		return null;
	}

	@Override
	public Object visit(AssignExisting assignExisting) {
		//lastLineVisited=assignExisting.getLine();
		if(assignExisting.annotations != null){
			assignExisting.annotations.accept(this);
		}
		
		assignExisting.assignee.accept(this);
		if(null != assignExisting.expr) {
			assignExisting.expr.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(AssertStatement assertStatement) {
		//lastLineVisited=assertStatement.getLine();
		assertStatement.e.accept(this);
		if(assertStatement.message != null){
			assertStatement.message.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(New namedConstructor) {
		//lastLineVisited=namedConstructor.getLine();
		namedConstructor.typeee.accept(this);
		if(null != namedConstructor.args){
			namedConstructor.args.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(MulerExpression mulerExpression) {
		//lastLineVisited=mulerExpression.getLine();
		mulerExpression.header.accept(this);
		for(MulerElement e: mulerExpression.elements)
		{
			e.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(MulerElement mulerElement) {
		//lastLineVisited=mulerElement.getLine();
		mulerElement.expr.accept(this);
		return null;
	}
	
	
	@Override
	public Object visit(ShiftElement shiftElement) {
		shiftElement.expr.accept(this);
		return null;
	}
	@Override
	public Object visit(ShiftExpression shiftExpression) {
		shiftExpression.header.accept(this);
		for(ShiftElement e: shiftExpression.elements)
		{
			e.accept(this);
		}
		return null;
	}
	

	@Override
	public Object visit(MapDefElement mapDefElement) {
		//lastLineVisited=mapDefElement.getLine();
		mapDefElement.getKey(this).accept(this);
		mapDefElement.getValue(this).accept(this);
		return null;
	}
	
	@Override
	public Object visit(MapDefaultElement mapDefElement) {
		//lastLineVisited=mapDefElement.getLine();
		mapDefElement.value.accept(this);
		return null;
	}
	
	@Override
	public Object visit(MapDef mapDef) {
		//lastLineVisited=mapDef.getLine();
		for(IsAMapElement e: mapDef.elements){
			if(e instanceof MapDefElement){
				((MapDefElement)e).accept(this);
			}
			else{
				((MapDefaultElement)e).accept(this);
			}
		}
		return null;
	}

	@Override
	public Object visit(Is instanceOf) {
		//lastLineVisited=instanceOf.getLine();
		instanceOf.e1.accept(this);
		for(Type tt : instanceOf.typees){
			tt.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(ImportStar importStar) {
		//lastLineVisited=importStar.getLine();
		return null;
	}

	@Override
	public Object visit(ImportImport importImport) {
		//lastLineVisited=importImport.getLine();
		for(DottedAsName ian : importImport.imports)
		{
			ian.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(ImportFrom importFrom) {
		//lastLineVisited=importFrom.getLine();
		for(ImportAsName ian : importFrom.froms)
		{
			ian.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(ImportAsName importAsName) {
		//lastLineVisited=importAsName.getLine();
		return null;
	}

	@Override
	public Object visit(IfStatement ifStatement) {
		//lastLineVisited=ifStatement.getLine();
		ifStatement.iftest.accept(this);
		ifStatement.ifblock.accept(this);
		for(ElifUnit u : ifStatement.elifunits)
		{
			u.accept(this);
		}
		if(ifStatement.elseb!=null)
		{
			ifStatement.elseb.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(FuncType funcType) {
		//lastLineVisited=funcType.getLine();
		for(Type input: funcType.getInputs())
		{
			input.accept(this);
		}
		funcType.retType.accept(this);
		return null;
	}

	@Override
	public Object visit(FuncRefInvoke funcRefInvoke) {
		//lastLineVisited=funcRefInvoke.getLine();
		funcRefInvoke.funcRef.accept(this);
		funcRefInvoke.getArgsOrig().accept(this);
		
		return null;
	}

	@Override
	public Object visit(FuncRef funcRef) {
		//lastLineVisited=funcRef.getLine();
		if(null != funcRef){
			if(null != funcRef.functo){
				funcRef.functo.accept(this);
			}
			
			FuncRefArgs args = funcRef.argsForNextCompCycle;
			if(null == args){
				args = funcRef.getArgsForScopeAndTypeCheck();
			}
			if(null != args){
				args.accept(this);
			}
		}
		
		return null;
	}

	@Override
	public Object visit(FuncParams funcParams) {
		//lastLineVisited=funcParams.getLine();
		for(FuncParam f: funcParams.params)
		{
			f.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(FuncParam funcParam) {
		//lastLineVisited=funcParam.getLine();
		if(funcParam.annotations != null){
			funcParam.annotations.accept(this);
		}
		
		if(funcParam.type != null){
			funcParam.type.accept(this);
		}
		if(funcParam.defaultValue != null){
			funcParam.defaultValue.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(FuncInvokeArgs funcInvokeArgs) {
		//lastLineVisited=funcInvokeArgs.getLine();
		
		/*List<Expression> args = funcInvokeArgs.getArgumentsWNPs();
		if(args == null) {
			args = funcInvokeArgs.asnames;
		}*/
		
		for(Expression e : funcInvokeArgs.asnames )
		{
			if(null != e) {
				e.accept(this);
			}
		}
		
		
		
		for(Pair<String, Object> thing : funcInvokeArgs.nameMap){
			((Expression)thing.getB()).accept(this);
		}
		
		return null;
	}
	
	
	@Override
	public Object visit(FuncRefArgs funcRefArgs) {
		//lastLineVisited=funcRefArgs.getLine();
		for(Object item : funcRefArgs.exprOrTypeArgsList)
		{
			if(item instanceof Expression){
				((Expression) item).accept(this);
			}
			else if(item instanceof Type){
				((Type) item).accept(this);
			}
		}
		
		for(Pair<String, Object> thing : funcRefArgs.nameMap){
			Object item = thing.getB();
			if(item instanceof Expression){
				((Expression) item).accept(this);
			}
			else if(item instanceof Type){
				((Type) item).accept(this);
			}
			
		}
		
		return null;
	}

	@Override
	public Object visit(FuncInvoke funcInvoke) {
		//lastLineVisited=funcInvoke.getLine();
		if(null != funcInvoke.genTypes){
			for(Type t : funcInvoke.genTypes){
				t.accept(this);
			}
		}
		funcInvoke.args.accept(this);
		
		return null;
	}

	@Override
	public Object visit(FuncDef funcDef) {
		//lastLineVisited=funcDef.getLine();
		if(null != funcDef.annotations){
			funcDef.annotations.accept(this);
		}
		
		if(null!=funcDef.params) funcDef.params.accept(this);
		if(null!=funcDef.retType) funcDef.retType.accept(this);
		
		if(null != funcDef.funcblock)
		{
			//funcDef.funcblock.isolated=true;
			funcDef.funcblock.accept(this);
		}
		
		
		return null;
	}

	@Override
	public Object visit(ForBlock forBlock) {
		//lastLineVisited=forBlock.getLine();
		if(null!= forBlock.localVarType) forBlock.localVarType.accept(this);
		forBlock.expr.accept(this);
		forBlock.block.accept(this);
		
		if(forBlock.elseblock != null){
			forBlock.elseblock.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(ForBlockOld forBlockOld) {
		//lastLineVisited=forBlockOld.getLine();
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
		
		if(forBlockOld.elseblock != null){
			forBlockOld.elseblock.accept(this);
		}
		
		return null;
	}
	
	@Override
	public Object visit(ElifUnit elifUnit) {
		//lastLineVisited=elifUnit.getLine();
		elifUnit.eliftest.accept(this);
		elifUnit.elifb.accept(this);
		
		return null;
	}

	@Override
	public Object visit(DuffAssign duffAssign) {
		//lastLineVisited=duffAssign.getLine();
		
		return duffAssign.e.accept(this);
	}

	@Override
	public Object visit(DottedNameList dottedNameList) {
		//lastLineVisited=dottedNameList.getLine();
		return null;
	}

	@Override
	public Object visit(DottedAsName dottedAsName) {
		//lastLineVisited=dottedAsName.getLine();
		return null;
	}

	@Override
	public Object visit(DotOperator dotOperator) {
		//lastLineVisited=dotOperator.getLine();
		Object lastThing = null;
		for(Expression e: dotOperator.getElements(this))
		{
			lastThing = e.accept(this);
		}
		return lastThing;
	}

	@Override
	public Object visit(ContinueStatement continueStatement) {
		//lastLineVisited=continueStatement.getLine();
		if(null != continueStatement.returns){
			continueStatement.returns.accept(this);
		}
		
		return null;
	}
	
	@Override
	public Object visit(BreakStatement breakStatement) {
		//lastLineVisited=breakStatement.getLine();
		if(null != breakStatement.returns){
			breakStatement.returns.accept(this);
		}
		return null;
	}


	@Override
	public Object visit(ClassDefArgs classDefArgs) {
		//lastLineVisited=classDefArgs.getLine();
		for(ClassDefArg a : classDefArgs.aargs)
		{
			a.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(ClassDefArg classDefArg) {
		//lastLineVisited=classDefArg.getLine();
		if(null != classDefArg.annotations){
			classDefArg.annotations.accept(this);
		}
		
		if(classDefArg.type != null){
			classDefArg.type.accept(this);
		}
		if(classDefArg.defaultValue != null){
			classDefArg.defaultValue.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(ClassDef classDef) {
		//lastLineVisited=classDef.getLine();
		if(classDef.annotations != null){
			classDef.annotations.accept(this);
		}
		
		if(!classDef.classGenricList.isEmpty())
		{
			for(GenericType n : classDef.classGenricList)
			{
				n.accept(this);
			}
		}
		
		if(null!=classDef.classDefArgs)
		{
			classDef.classDefArgs.accept(this);
		}
		
		if(null != classDef.typedActorOn){
			classDef.typedActorOn.accept(this);
		}
		
		if(classDef.superclass!=null)
		{
			if(null != classDef.superClassGenricList && !classDef.superClassGenricList.isEmpty())
			{
				for(Type n : classDef.superClassGenricList)
				{
					if(null != n){
						n.accept(this);
					}
				}
			}
		}
		
		for(Expression e : classDef.superClassExpressions)
		{
			e.accept(this);
		}
		
		if(!classDef.traits.isEmpty()) {
			classDef.traits.forEach(a -> a.accept(this));
		}
		
		classDef.classBlock.accept(this);
		
		return null;
	}
	
	@Override
	public Object visit(ImpliInstance impliInstance) {
		if(!impliInstance.traitGenricList.isEmpty()) {
			for(Type n : impliInstance.traitGenricList)
			{
				n.accept(this);
			}
		}
		
		return null;
	}

	
	@Override
	public Object visit(EnumDef enumDef){
		//lastLineVisited=enumDef.getLine();
		if(enumDef.annotations != null){
			enumDef.annotations.accept(this);
		}
		
		if(null!=enumDef.classDefArgs)
		{
			enumDef.classDefArgs.accept(this);
		}

		enumDef.block.accept(this);
		
		return null;
	}
	

	@Override
	public Object visit(CatchBlocks catchBlocks) {
		//lastLineVisited=catchBlocks.getLine();
		for(Type ca : catchBlocks.caughtTypes){
			ca.accept(this);
		}
		
		catchBlocks.catchBlock.accept(this);
		
		return null;
	}

	@Override
	public Object visit(CastExpression castExpression) {
		//lastLineVisited=castExpression.getLine();
		castExpression.t.accept(this);
		castExpression.o.accept(this);
		
		return null;
	}

	@Override
	public Object visit(Block block) {
		//lastLineVisited=block.getLine();
		LineHolder lh = block.startItr();
		
		while(lh != null)
		{
			lh.accept(this);
			lh = block.getNext();
		}
		
		return null;
	}

	@Override
	public Object visit(WithBlock withBlock) {
		//lastLineVisited=withBlock.getLine();
		withBlock.expr.accept(this);
		withBlock.blk.accept(this);
		
		return null;
	}
	
/*	@Override
	public Object visit(TransBlock transBlock) {
		transBlock.body.accept(this);
		
		return null;
	}*/

	@Override
	public Object visit(WhileBlock whileBlock) {
		//lastLineVisited=whileBlock.getLine();
		whileBlock.cond.accept(this);
		whileBlock.block.accept(this);
		
		if(whileBlock.elseblock != null){
			whileBlock.elseblock.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(VarString varString) {
		//lastLineVisited=varString.getLine();
		
		if(varString.subExpressions != null){
			for(Expression expr : varString.subExpressions){
				expr.accept(this);
			}
		}
		
		return null;
	}
	
	@Override
	public Object visit(VarRegexPattern varString) {
		//lastLineVisited=varString.getLine();
		return null;
	}

	@Override
	public Object visit(VarChar varString) {
		//lastLineVisited=varString.getLine();
		return null;
	}
	
	@Override
	public Object visit(VarNull varNull) {
		//lastLineVisited=varNull.getLine();
		return null;
	}

	@Override
	public Object visit(VarInt varInt) {
		//lastLineVisited=varInt.getLine();
		return null;
	}

	@Override
	public Object visit(VarFloat varFloat) {
		//lastLineVisited=varFloat.getLine();
		return null;
	}

	@Override
	public Object visit(VarDouble varDouble) {
		//lastLineVisited=varDouble.getLine();
		return null;
	}

	@Override
	public Object visit(UsingStatement usingStatement) {
		//lastLineVisited=usingStatement.getLine();
		for(DottedAsName dan : usingStatement.asnames)
		{
			dan.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(TryCatch tryCatch) {
		//lastLineVisited=tryCatch.getLine();
		if(tryCatch.astRepoint!=null){
			return tryCatch.astRepoint.accept(this);
		}
		else{
			tryCatch.blockToTry.accept(this);
			for(CatchBlocks cat : tryCatch.cbs)
			{
				cat.accept(this);
			}
			if(tryCatch.finalBlock != null)
			{
				tryCatch.finalBlock.accept(this);
			}
			return null;
		}
	}

	@Override
	public Object visit(ThrowStatement throwStatement) {
		//lastLineVisited=throwStatement.getLine();
		throwStatement.thingTothrow.accept(this);
		return null;
	}

	@Override
	public Object visit(ReturnStatement returnStatement) {
		//lastLineVisited=returnStatement.getLine();
		if(null!=returnStatement.ret) returnStatement.ret.accept(this);
		
		return null;
	}

	@Override
	public Object visit(RefThis refThis) {
		//lastLineVisited=refThis.getLine();
		return null;
	}
	
	@Override
	public Object visit(RefOf refThis) {
		//lastLineVisited=refThis.getLine();
		return null;
	}
	
	@Override
	public Object visit(RefClass refClass) {
		//lastLineVisited=refClass.getLine();
		return null;
	}

	@Override
	public Object visit(RefSuper refSuper) {
		//lastLineVisited=refSuper.getLine();
		return null;
	}

	@Override
	public Object visit(RefName refName) {
		//lastLineVisited=refName.getLine();
		return null;
	}

	@Override
	public Object visit(RefBoolean refBoolean) {
		//lastLineVisited=refBoolean.getLine();
		return null;
	}

	@Override
	public Object visit(PrimativeType primativeType) {
		//lastLineVisited=primativeType.getLine();
		return null;
	}

	@Override
	public Object visit(PrefixOp prefixOp) {
		//lastLineVisited=prefixOp.getLine();
		prefixOp.p1.accept(this);
		return null;
	}

	@Override
	public Object visit(PowOperator powOperator) {
		//lastLineVisited=powOperator.getLine();
		powOperator.expr.accept(this);
		powOperator.raiseTo.accept(this);
		return null;
	}

	@Override
	public Object visit(PostfixOp postfixOp) {
		//lastLineVisited=postfixOp.getLine();
		postfixOp.p2.accept(this);
		return null;
	}

	@Override
	public Object visit(OrExpression orExpression) {
		//lastLineVisited=orExpression.getLine();
		orExpression.head.accept(this);
		for(Expression i : orExpression.things) {
			i.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(NotExpression notExpression) {
		//lastLineVisited=notExpression.getLine();
		notExpression.expr.accept(this);
		return null;
	}
	
	@Override
	public Object visit(CopyExpression copyExpression) {
		//lastLineVisited=copyExpression.getLine();
		copyExpression.expr.accept(this);
		return null;
	}
	
	@Override
	public Object visit(NamedType namedType) {
		//lastLineVisited=namedType.getLine();
		if(!namedType.getGenTypes().isEmpty())
		{
			for(Type t : namedType.getGenTypes())
			{
				if(null != t) {
					t.accept(this);
				}
			}
		}
		return null;
	}

	@Override
	public Object visit(VarLong varLong) {
		//lastLineVisited=varLong.getLine();
		return null;
	}

	@Override
	public Object visit(VarShort varLong) {
		//visitList.add("VarLong");
		return null;
	}
	
/*	@Override
	public Object visit(IfExpr ifExpr) {
		//lastLineVisited=ifExpr.getLine();
		ifExpr.test.accept(this);
		ifExpr.op1.accept(this);
		ifExpr.op2.accept(this);
		return null;
	}*/

	@Override
	public Object visit(LambdaDef lambdaDef) {
		//lastLineVisited=lambdaDef.getLine();
		if(null != lambdaDef.annotations){
			lambdaDef.annotations.accept(this);
		}
		
		if(null != lambdaDef.params)
			lambdaDef.params.accept(this);
		if(null != lambdaDef.returnType){
			lambdaDef.returnType.accept(this);
		}
		
		if(null != lambdaDef.body) {
			lambdaDef.body.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(GrandLogicalElement equalityElement) {
		//lastLineVisited=equalityElement.getLine();
		equalityElement.e2.accept(this);
		
		return null;
	}

	@Override
	public Object visit(EqReExpression equalityExpression) {
		//lastLineVisited=equalityExpression.getLine();
		equalityExpression.head.accept(this);
		for(GrandLogicalElement e: equalityExpression.elements)
		{
			e.accept(this);
		}
		return null;
	}
	
	@Override
	public Object visit(LineHolder lineHolder) {
		//lastLineVisited=lineHolder.getLine();
		return lineHolder.l.accept(this);
	}

	@Override
	public Object visit(ThisConstructorInvoke thisConstructorInvoke) {
		//lastLineVisited=thisConstructorInvoke.getLine();
		thisConstructorInvoke.args.accept(this);
		return null;
	}

	@Override
	public Object visit(SuperConstructorInvoke superConstructorInvoke) {
		//lastLineVisited=superConstructorInvoke.getLine();
		superConstructorInvoke.args.accept(this);
		return null;
	}

	@Override
	public Object visit(ConstructorDef funcDef) {
		//lastLineVisited=funcDef.getLine();
		return visit((FuncDef)funcDef);
	}

	@Override
	public Collection<? extends ErrorHolder> getErrors() {
		return null;
	}

	@Override
	public Object visit(GetSetOperation getSetOperation) {
		//lastLineVisited=getSetOperation.getLine();
		getSetOperation.toAddMinus.accept(this);
		return null;
	}
	
	@Override
	public Object visit(GenericType genericType) {
		//lastLineVisited=genericType.getLine();
		return null;//null is ok
	}
	
	@Override
	public Object visit(OnChange onChange){
		//lastLineVisited=onChange.getLine();
		for(Node e: onChange.exprs){
			e.accept(this);
		}
		
		if(null != onChange.applyMethodFuncDef){
			onChange.applyMethodFuncDef.accept(this);
			onChange.cleanUpMethodFuncDef.accept(this);
			onChange.initMethodNameFuncDef.accept(this);
		}
		else{
			if(null !=onChange.body){
				onChange.body.accept(this);
			}
		}		
		
		return null;//null is ok
	}
	
	
	@Override
	public Object visit(Await await){
		//lastLineVisited=await.getLine();
		return visit((OnChange)await);
	}
	
	@Override
	public Object visit(OnEvery every){
		//lastLineVisited=every.getLine();
		return visit((OnChange)every);
	}
	
	@Override
	public Object visit(Changed changed){
		//lastLineVisited=changed.getLine();
		return null;
	}
	
	@Override
	public Object visit(AsyncBodyBlock asyncBodyBlock) {
		//lastLineVisited=asyncBodyBlock.getLine();
		
		if(null != asyncBodyBlock.applyMethodFuncDef){
			asyncBodyBlock.applyMethodFuncDef.accept(this);
			asyncBodyBlock.cleanUpMethodFuncDef.accept(this);
			asyncBodyBlock.initMethodNameFuncDef.accept(this);
		}
		//else{
			for(Block pre : asyncBodyBlock.preBlocks){
				pre.accept(this);
			}
		
			asyncBodyBlock.mainBody.accept(this);
			
			for(Block post : asyncBodyBlock.postBlocks){
				post.accept(this);
			}
		//}
		
		return null;
	}

	@Override
	public Object visit(NOP nop){
		//lastLineVisited=nop.getLine();
		return null;
	}

	@Override
	public Object visit(InExpression cont){
		//lastLineVisited=cont.getLine();
		cont.thing.accept(this);
		cont.insideof.accept(this);

		return null;
	}

	@Override
	public Object visit(EnumItem enumItem){
		//lastLineVisited=enumItem.getLine();
		//enumItem.name
		if(enumItem.annotations != null){
			enumItem.annotations.accept(this);
		}
		
		enumItem.args.accept(this);
		if(enumItem.block != null){
			enumItem.block.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(EnumBlock enumBlock){
		//lastLineVisited=enumBlock.getLine();
		for(EnumItem ei :  enumBlock.enumItemz){
			ei.accept(this);
		}
		enumBlock.mainBlock.accept(this);
		
		return null;
	}

	@Override
	public Object visit(InitBlock initBlock){
		//lastLineVisited=initBlock.getLine();
		return null;//ignore
	}

	@Override
	public Object visit(Annotation annotation){
		//lastLineVisited=annotation.getLine();
		if(annotation.singleArg != null){
			annotation.singleArg.accept(this);
		}
		else if(annotation.manyArgs != null && !annotation.manyArgs.isEmpty()){
			//TODO: annotation
		}
		
		return null;//ignore non functional
	}
	
	@Override
	public Object visit(Annotations annotations){
		//lastLineVisited=annotations.getLine();
		for(Annotation annot : annotations.annotations){
			annot.accept(this);
		}
		
		return null;//ignore non functional
	}

	@Override
	public Object visit(AnnotationDef annotationDef){
		//lastLineVisited=annotationDef.getLine();
		if(annotationDef.annotations != null){
			annotationDef.annotations.accept(this);
		}
		
		if(!annotationDef.annotationDefArgs.isEmpty()){
			
			for(AnnotationDefArg ada : annotationDef.annotationDefArgs){
				if(null != ada.annotations){
					ada.annotations.accept(this);
				}
				if(null != ada.optionalType){
					ada.optionalType.accept(this);
				}
				if(ada.expression != null){
					ada.expression.accept(this);
				}
			}
		}
		
		annotationDef.annotBlock.accept(this);
		return null;
	}

	@Override
	public Object visit(TypedefStatement typedefStatement){
		//lastLineVisited=typedefStatement.getLine();
		typedefStatement.type.accept(this);
		
		return null;
	}

	@Override
	public Object visit(MatchStatement matchStatement) {
		//lastLineVisited=matchStatement.getLine();
		matchStatement.matchon.accept(this);
		
		if(!matchStatement.cases.isEmpty()){
			for(MactchCase caz : matchStatement.cases){
				caz.getA().accept(this);
				caz.getB().accept(this);
			}
		}
		
		
		if(null != matchStatement.elseblok){
			matchStatement.elseblok.accept(this);
		}

		return null;
	}

	@Override
	public Object visit(CaseExpressionWrapper caseExpressionWrapper) {
		//lastLineVisited=caseExpressionWrapper.getLine();
		
		if(caseExpressionWrapper.repointedToWithBlock != null) {
			caseExpressionWrapper.repointedToWithBlock.accept(this);		
		}else {
			caseExpressionWrapper.e.accept(this);		
		}
		
		if(null != caseExpressionWrapper.alsoCondition){
			caseExpressionWrapper.alsoCondition.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(CaseExpressionPre caseExpressionPre) {
		//lastLineVisited=caseExpressionPre.getLine();
		
		caseExpressionPre.e.accept(this);
		
		if(null != caseExpressionPre.alsoCondition){
			caseExpressionPre.alsoCondition.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(CaseExpressionPost caseExpressionPost) {
		//lastLineVisited=caseExpressionPost.getLine();
		
		caseExpressionPost.e.accept(this);
		
		if(null != caseExpressionPost.alsoCondition){
			caseExpressionPost.alsoCondition.accept(this);
		}
		return null;
	}

	

	@Override
	public Object visit(CaseExpressionAnd caseExpressionAnd) {
		//lastLineVisited=caseExpressionAnd.getLine();
		caseExpressionAnd.head.accept(this);
		for(CaseExpression ce : caseExpressionAnd.caseAnds){
			ce.accept(this);
		}	
		
		if(null != caseExpressionAnd.alsoCondition){
			caseExpressionAnd.alsoCondition.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(CaseExpressionOr caseExpressionOr) {
		//lastLineVisited=caseExpressionOr.getLine();
		caseExpressionOr.head.accept(this);
		for(CaseExpression ce : caseExpressionOr.caseOrs){
			ce.accept(this);
		}	
		
		if(null != caseExpressionOr.alsoCondition){
			caseExpressionOr.alsoCondition.accept(this);
		}
		
		return null;
	}
	
	
	@Override
	public Object visit(CaseExpressionAssign caseExpressionUntypedAssign){
		//lastLineVisited=caseExpressionUntypedAssign.getLine();
		for(Type tt : caseExpressionUntypedAssign.types){
			tt.accept(this);
		}
		
		if(null != caseExpressionUntypedAssign.expr){
			caseExpressionUntypedAssign.expr.accept(this);
		}	
		
		if(null != caseExpressionUntypedAssign.alsoCondition){
			caseExpressionUntypedAssign.alsoCondition.accept(this);
		}
		
		return null;
	}	

	@Override
	public Object visit(CaseExpressionAssignTuple caseExpressionAssignTuple){
		for(Assign tt : caseExpressionAssignTuple.lhss){
			if(tt != null) {
				tt.accept(this);
			}
		}
		
		if(null != caseExpressionAssignTuple.expr){
			caseExpressionAssignTuple.expr.accept(this);
		}
		
		if(caseExpressionAssignTuple.alsoCondition != null){
			caseExpressionAssignTuple.alsoCondition.accept(this);
		}
		
		return null;
	}
	
	
	@Override
	public Object visit(CaseExpressionTuple caseExpressionTuple) {
		int sz= caseExpressionTuple.getComponents().size();
		for(int n = 0; n < sz; n++)	{	
			CaseExpression expr = caseExpressionTuple.getComponents().get(n);
			if(null != expr) {
				expr.accept(this);
			}
		}
		
		if(null != caseExpressionTuple.caseExpression) {
			caseExpressionTuple.caseExpression.accept(this);
		}
		
		return null;
	}
	
	@Override
	public Object visit(TypedCaseExpression typedCaseExpression){
		//lastLineVisited=typedCaseExpression.getLine();
		
		for(Type tt : typedCaseExpression.types){
			tt.accept(this);
		}
		
		if(null != typedCaseExpression.caseExpression){
			typedCaseExpression.caseExpression.accept(this);
		}
				
		if(null != typedCaseExpression.alsoCondition){
			typedCaseExpression.alsoCondition.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(DeleteStatement deleteStatement){
		//lastLineVisited=deleteStatement.getLine();
		for(Expression ee : deleteStatement.exprs) {
			ee.accept(this);
		}
		return null;
	}
	
	@Override
	public Object visit(JustAlsoCaseExpression justAlsoCaseExpression){
		justAlsoCaseExpression.alsoCondition.accept(this);
		return null;
	}
	
	@Override
	public Object visit(SizeofStatement deleteStatement){
		//lastLineVisited=deleteStatement.getLine();
		deleteStatement.e.accept(this);
		return null;
	}
	

	@Override
	public Object visit(DMANewFromExpression dmaNewFromExpression){
		//lastLineVisited=dmaNewFromExpression.getLine();
		dmaNewFromExpression.e.accept(this);
		return null;
	}
	
/*	@Override
	public Object visit(RefNamedType refNamedType) {
		return refNamedType.accept(this);
	}*/
	
	@Override
	public Object visit(LocalClassDef localClassDef) {
		//lastLineVisited=localClassDef.getLine();
		return localClassDef.cd.accept(this);
	}
	
	@Override
	public Object visit(TypeReturningExpression localClassDef) {
		//lastLineVisited=localClassDef.getLine();
		return localClassDef.type.accept(this);
	}

	@Override
	public Object visit(TransBlock transBlock){
		return transBlock.blk.accept(this);
	}
	
	@Override
	public Object visit(ExpressionList expressionList) {
		for(Expression expr : expressionList.exprs){
			expr.accept(this);
		}
		return null;
	}

	
	@Override
	public Object visit(ArrayDefComplex arrayDefComplex){
		for(Expression e : arrayDefComplex.getArrayElements(this))
		{
			e.accept(this);
		}
		return null;
	}
	
	@Override
	public Object visit(Vectorized vectorized) {
		vectorized.expr.accept(this);
		
		return null;
	}
	
	@Override
	public Object visit(VectorizedFuncInvoke vectorizedFuncInvoke) {
		vectorizedFuncInvoke.expr.accept(this);
		
		vectorizedFuncInvoke.funcInvoke.accept(this);
		
		return null;
	}
	
	@Override
	public Object visit(VectorizedFuncRef vectorizedFuncInvoke) {
		vectorizedFuncInvoke.expr.accept(this);
		
		vectorizedFuncInvoke.funcRef.accept(this);
		
		return null;
	}
	
	@Override
	public Object visit(VectorizedFieldRef vectorizedFieldRef) {
		vectorizedFieldRef.expr.accept(this);
		
		vectorizedFieldRef.name.accept(this);
		
		return null;
	}
	
	@Override
	public Object visit(VectorizedNew vectorizedNew) {
		vectorizedNew.lhs.accept(this);
		
		vectorizedNew.constru.accept(this);
		
		return null;
	}
	
	@Override
	public Object visit(MultiType multiType) {
		int sz = multiType.multitype.size();
		for(int n=0; n < sz; n++) {
			multiType.multitype.get(n).accept(this);
		}
		return null;
	}
	

	@Override
	public Object visit(VectorizedArrayRef arrayRef) {
		arrayRef.expr.accept(this);
		processArrayElements(arrayRef.arrayLevelElements);
		return null;
	}
	

	@Override
	public Object visit(PointerAddress pointerAddress) {
		pointerAddress.rhs.accept(this);
		return null;
	}
	
	@Override
	public Object visit(PointerUnref pointerUnref) {
		pointerUnref.rhs.accept(this);
		return null;
	}
	
	@Override
	public Object visit(AssignMulti multiAssign) {
		for(Assign ass:  multiAssign.assignments) {
			ass.accept(this);
		}
		multiAssign.rhs.accept(this);
		return null;
	}

	@Override
	public Object visit(JustLoad justLoad) {
		return null;//ignore, used in bytecode genneration only
	}

	@Override
	public Object visit(TupleExpression tupleExpression) {
		int sz= tupleExpression.tupleElements.size();
		for(int n = 0; n < sz; n++)
		{	
			Expression expr = tupleExpression.tupleElements.get(n);
			if(expr != null) {
				expr.accept(this);
			}
		}
		return null;
	}

	@Override
	public Object visit(AssignTupleDeref assignTupleDeref) {
		int sz= assignTupleDeref.lhss.size();
		for(int n = 0; n < sz; n++)
		{	
			Assign ass = assignTupleDeref.lhss.get(n);
			if(ass != null) {
				ass.accept(this);
			}
		}
		
		assignTupleDeref.expr.accept(this);
		
		return null;
	}

	@Override
	public Object visit(AnonLambdaDef anonLambdaDef) {
		anonLambdaDef.body.accept(this);

		ArrayList<Pair<String, Type>> inputs = anonLambdaDef.getInputs();
		int sz= inputs.size();
		for(int n = 0; n < sz; n++){	
			Pair<String, Type> inst = inputs.get(n);
			
			Type tt = inst.getB();
			if(tt != null) {
				tt.accept(this);
			}
		}
		
		if(null != anonLambdaDef.retType) {
			anonLambdaDef.retType.accept(this);
		}
		
		return null;
	}
	
	@Override
	public Object visit(CaseExpressionObjectTypeAssign caseExpressionObjectTypeAssign) {
		if(caseExpressionObjectTypeAssign.isFinal) {
		}
		
		caseExpressionObjectTypeAssign.expr.accept(this);
		
		if(caseExpressionObjectTypeAssign.alsoCondition != null){
			caseExpressionObjectTypeAssign.alsoCondition.accept(this);
		}
		
		return null;
	}
	

	
	

	
	@Override
	public Object visit(ObjectProvider objectProvider) {
		if(objectProvider.annotations != null){
			objectProvider.annotations.accept(this);
		}
		
		if(!objectProvider.classGenricList.isEmpty())
		{
			for(Object n : objectProvider.classGenricList)
			{
				if(n instanceof GenericType) {
					((GenericType)n).accept(this);
				}
			}
		}
		
		if(null != objectProvider.classDefArgs) {
			objectProvider.classDefArgs.accept(this);
		}
		
		objectProvider.objectProviderBlock.accept(this);
		return null;
	}
	@Override
	public Object visit(ObjectProviderBlock objectProviderBlock) {
		
		for(ObjectProviderLine opl : objectProviderBlock.lines) {
			opl.accept(this);
		}

		return null;
	}
	@Override
	public Object visit(ObjectProviderLineDepToExpr objectProviderLineDepToExpr) {

		//lineGenerics(objectProviderLineDepToExpr.getLocalGens());
		
		objectProviderLineDepToExpr.dependency.accept(this);
		if(null != objectProviderLineDepToExpr.fulfilment) {
			objectProviderLineDepToExpr.fulfilment.accept(this);
		}else if(objectProviderLineDepToExpr.typeOnlyRHS != null){
			objectProviderLineDepToExpr.typeOnlyRHS.accept(this);
		}
		
		return null;
	}
	@Override
	public Object visit(ObjectProviderLineProvide objectProviderLineProvide) {
		
		//lineGenerics(objectProviderLineProvide.getLocalGens());
		
		objectProviderLineProvide.provides.accept(this);
		
		if(objectProviderLineProvide.provideExpr != null) {
			objectProviderLineProvide.provideExpr.accept(this);
		}else if(objectProviderLineProvide.nestedDeps != null) {
			objectProviderLineProvide.nestedDeps.forEach(a -> {a.accept(this); }); 
		}
		return null;
	}
	
	@Override
	public Object visit(NotNullAssertion notNullAssertion) {
		notNullAssertion.expr.accept(this);
		return null;
	}
	
	@Override
	public Object visit(ElvisOperator elvisOperator) {
		elvisOperator.lhsExpression.accept(this);
		elvisOperator.rhsExpression.accept(this);
		return null;
	}
	
	@Override
	public Object visit(LangExt langExt) {
		return null;
	}
}