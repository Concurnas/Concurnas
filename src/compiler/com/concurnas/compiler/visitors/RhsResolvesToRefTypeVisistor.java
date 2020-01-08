package com.concurnas.compiler.visitors;

import java.util.Collection;

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
import com.concurnas.compiler.ast.util.JustLoad;
import com.concurnas.compiler.visitors.util.MactchCase;

//not sure if this is a good idea
public class RhsResolvesToRefTypeVisistor implements Visitor {

	public void pushErrorContext(REPLTopLevelComponent xxx) {}
	public REPLTopLevelComponent popErrorContext() {return null;}
	
	private AssignExisting theAss;
	
	protected int lastLineVisited = -1;
	public void resetLastLineVisited(){
		lastLineVisited = -1;
	}
	public int getLastLineVisited(){
		return lastLineVisited;
	}
	public void setLastLineVisited(int lineNo){
		return;//ignore
	}
	
	public RhsResolvesToRefTypeVisistor(AssignExisting ass) {
		this.theAss = ass;
	}
	
	public boolean doesRhsResolve() {
		Boolean ret = (Boolean)theAss.expr.accept(this);
		return ret;// null != ret ? ret:false; - not expecting null
	}

	@Override
	public Object visit(Additive addMinusExpression) {
		return false;
	}

	@Override
	public Object visit(AddMinusExpressionElement addMinusExpressionElement) {
		return false;
	}

	@Override
	public Object visit(ExpressionList expressionList) {
		return false;
	}
	
	@Override
	public Object visit(AndExpression andExpression) {
		return false;
	}
	
	@Override
	public Object visit(BitwiseOperation bitwiseOperation) {
		return false;
	}

	@Override
	public Object visit(ArrayConstructor arrayConstructor) {
		return false;
	}

	@Override
	public Object visit(ArrayRefElementPrefixAll arrayRefElementPrefixAll) {
		return false;
	}

	@Override
	public Object visit(ArrayRefElementPostfixAll arrayRefElementPostfixAll) {
		return false;
	}

	@Override
	public Object visit(ArrayRefElementSubList arrayRef) {
		return false;
	}

	@Override
	public Object visit(ArrayRefElement arrayRefElement) {
		return false;
	}

	@Override
	public Object visit(ArrayRef arrayRef) {
		return false;
	}

	@Override
	public Object visit(ArrayDef arrayDef) {
		return false;
	}

	
	@Override
	public Object visit(ArrayDefComplex arrayDefComplex){
		return false;
	}
	
	@Override
	public Object visit(AsyncBlock asyncBlock) {
		asyncBlock.theAssToStoreRefIn = theAss;
		return true;
	}

	@Override
	public Object visit(AssignNew assignNew) {
		return false;
	}

	@Override
	public Object visit(AssignExisting assignExisting) {
		return assignExisting.expr.accept(this);
	}

	@Override
	public Object visit(AssertStatement assertStatement) {
		return false;
	}
	
	@Override
	public Object visit(DeleteStatement deleteStatement){
		return false;
	}
	
	@Override
	public Object visit(SizeofStatement deleteStatement){
		return false;
	}

	@Override
	public Object visit(New namedConstructor) {
		
		return false;
	}

	@Override
	public Object visit(MulerExpression mulerExpression) {
		
		return false;
	}

	@Override
	public Object visit(ShiftElement shiftElement) {
		return false;
	}
	@Override
	public Object visit(ShiftExpression shiftExpression) {
		return false;
	}
	
	@Override
	public Object visit(MulerElement mulerElement) {
		
		return false;
	}

	@Override
	public Object visit(MapDefElement mapDefElement) {
		return false;
	}
	
	@Override
	public Object visit(MapDefaultElement mapDefElement) {
		return false;
	}
	
	@Override
	public Object visit(MapDef mapDef) {
		
		return false;
	}

	@Override
	public Object visit(Is instanceOf) {
		
		return false;
	}

	@Override
	public Object visit(ImportStar importStar) {
		
		return false;
	}

	@Override
	public Object visit(ImportImport importImport) {
		
		return false;
	}

	@Override
	public Object visit(ImportFrom importFrom) {
		
		return false;
	}

	@Override
	public Object visit(ImportAsName importAsName) {
		
		return false;
	}

	@Override
	public Object visit(MatchStatement matchStatement) {
		boolean ret = true;
		for(MactchCase caz : matchStatement.cases){
			ret |= (Boolean)caz.getB().accept(this);
		}
		
		if(null != matchStatement.elseblok){
			ret |= (Boolean)matchStatement.elseblok.accept(this);
		}
		
		return ret;//if any branch into an ayncblock then the thing IS used!
	}
	

	@Override
	public Object visit(FuncType funcType) {
		return false;
	}

	@Override
	public Object visit(FuncRefInvoke funcRefInvoke) {
		return false;
	}

	@Override
	public Object visit(FuncRef funcRef) {
		return false;
	}

	@Override
	public Object visit(FuncParams funcParams) {
		return false;
	}

	@Override
	public Object visit(FuncParam funcParam) {
		return false;
	}

	@Override
	public Object visit(FuncInvokeArgs funcInvokeArgs) {
		return false;
	}

	@Override
	public Object visit(FuncInvoke funcInvoke) {
		return false;
	}

	@Override
	public Object visit(FuncDef funcDef) {
		return false;
	}

	@Override
	public Object visit(ConstructorDef funcDef) {
		return false;
	}

	@Override
	public Object visit(ForBlock forBlock) {
		return forBlock.block.accept(this);
	}

	@Override
	public Object visit(ElifUnit elifUnit) {
		return false;
	}

	@Override
	public Object visit(DuffAssign duffAssign) {
		return duffAssign.e.accept(this);
	}

	@Override
	public Object visit(DottedNameList dottedNameList) {
		return false;
	}

	@Override
	public Object visit(DottedAsName dottedAsName) {
		return false;
	}

	@Override
	public Object visit(DotOperator dotOperator) {
		return false;
	}

	@Override
	public Object visit(ContinueStatement continueStatement) {
		return false;
	}

	@Override
	public Object visit(ClassDefArgs classDefArgs) {
		return false;
	}

	@Override
	public Object visit(ClassDefArg classDefArg) {
		return false;
	}

	@Override
	public Object visit(ClassDef classDef) {
		return false;
	}
	
	@Override
	public Object visit(EnumDef classDef) {
		return false;
	}

	@Override
	public Object visit(CatchBlocks catchBlocks) {
		return false;
	}

	@Override
	public Object visit(CastExpression castExpression) {
		return castExpression.o.accept(this);//TODO: check this, x = {}! as int://yeah probably right
	}

	@Override
	public Object visit(BreakStatement breakStatement) {
		return false;
	}

	@Override
	public Object visit(Block block) {
		if(null != block && !block.isEmpty()){
			return block.getLast().accept(this);
		}
		return false;
	}

	@Override
	public Object visit(WithBlock withBlock) {
		return withBlock.blk.accept(this);
	}
	
/*	@Override
	public Object visit(TransBlock withBlock) {
		return withBlock.body.accept(this);
	}*/

	@Override
	public Object visit(WhileBlock whileBlock) {
		return whileBlock.block.accept(this);
	}

	@Override
	public Object visit(VarString varString) {
		return false;
	}
	
	@Override
	public Object visit(VarRegexPattern varString) {
		return false;
	}

	@Override
	public Object visit(VarNull varNull) {
		return false;
	}

	@Override
	public Object visit(VarInt varInt) {
		return false;
	}

	@Override
	public Object visit(VarFloat varFloat) {
		return false;
	}

	@Override
	public Object visit(VarDouble varDouble) {
		return false;
	}

	@Override
	public Object visit(TryCatch tryCatch) {
		
		if(tryCatch.astRepoint != null){
			return tryCatch.astRepoint.accept(this);
		}
		
		boolean ret = (Boolean)tryCatch.blockToTry.accept(this);
		for(CatchBlocks cat : tryCatch.cbs)
		{
			ret |= (Boolean)cat.accept(this);
		}
		if(tryCatch.finalBlock != null)
		{
			ret |= (Boolean)tryCatch.finalBlock.accept(this);
		}
		
		return ret;
	}

	@Override
	public Object visit(ThrowStatement throwStatement) {
		return false;
	}

	@Override
	public Object visit(ReturnStatement returnStatement) {
		return false;//i think this is impoossible/invalid
	}

	@Override
	public Object visit(RefThis refThis) {
		return false;
	}
	
	@Override
	public Object visit(RefOf refThis) {
		return false;
	}

	@Override
	public Object visit(RefSuper refSuper) {
		return false;
	}
	
	@Override
	public Object visit(RefClass refClass) {
		return false;
	}

	@Override
	public Object visit(RefName refName) {
		return false;
	}

	@Override
	public Object visit(RefBoolean refBoolean) {
		return false;
	}

	@Override
	public Object visit(PrimativeType primativeType) {
		return false;
	}

	@Override
	public Object visit(PrefixOp prefixOp) {
		
		return false;
	}

	@Override
	public Object visit(PowOperator powOperator) {
		
		return false;
	}

	@Override
	public Object visit(PostfixOp postfixOp) {
		
		return false;
	}

	@Override
	public Object visit(OrExpression orExpression) {
		
		return false;
	}

	@Override
	public Object visit(InExpression cont){
		return false;
	}
	
	
	@Override
	public Object visit(NotExpression notExpression) {
		
		return false;
	}

	@Override
	public Object visit(CopyExpression copyExpression) {
		
		return false;//cannot be the dude if it gets copied
	}

	@Override
	public Object visit(NamedType namedType) {
		
		return false;
	}

/*	@Override
	public Object visit(RefNamedType refNamedType) {
		return refNamedType.accept(this);
	}*/
	
	@Override
	public Object visit(VarLong varLong) {
		return false;
	}
	
	@Override
	public Object visit(VarShort varLong) {
		return false;
	}

/*	@Override
	public Object visit(IfExpr ifExpr) {
		Boolean ret = (Boolean)ifExpr.test.accept(this);
		ret = (Boolean)ifExpr.op1.accept(this);
		ret = (Boolean)ifExpr.op2.accept(this);
		return ret;
	}*/
	
	@Override
	public Object visit(IfStatement ifStatement) {
		boolean ret = (Boolean)ifStatement.iftest.accept(this);
		ret |= (Boolean)ifStatement.ifblock.accept(this);
		for(ElifUnit u : ifStatement.elifunits)
		{
			ret |= (Boolean)u.accept(this);
		}
		if(ifStatement.elseb!=null)
		{
			ret |= (Boolean)ifStatement.elseb.accept(this);
		}
		return ret;//if any branch into an ayncblock then the thing IS used!
	}

	@Override
	public Object visit(LambdaDef lambdaDef) {
		
		return false;
	}

	@Override
	public Object visit(GrandLogicalElement equalityElement) {
		
		return false;
	}

	@Override
	public Object visit(EqReExpression equalityExpression) {
		
		return false;
	}

	@Override
	public Object visit(LineHolder lineHolder) {
		return lineHolder.l.accept(this);
	}

	@Override
	public Object visit(ThisConstructorInvoke thisConstructorInvoke) {
		return false;
	}

	@Override
	public Object visit(SuperConstructorInvoke superConstructorInvoke) {
		return false;
	}

	@Override
	public Object visit(FuncRefArgs funcRefArgs) {
		return false;
	}

	@Override
	public Object visit(VarChar varChar) {
		
		return false;
	}

	@Override
	public Object visit(ForBlockOld forBlockOld) {
		return forBlockOld.block.accept(this);
	}

	@Override
	public Collection<? extends ErrorHolder> getErrors() {
		return null;
	}

	@Override
	public Object visit(GetSetOperation getSetOperation) {
		return false;
	}

	@Override
	public Object visit(GenericType genericType) {
		return false;
	}

	@Override
	public Object visit(AsyncRefRef asyncRefRef) {
		return false;
	}

	@Override
	public Object visit(Await await) {
		return visit((OnChange)await);//cannot go on rhs
	}
	
	@Override
	public Object visit(OnEvery await) {
		return visit((OnChange)await);//cannot go on rhs
	}
	
	@Override
	public Object visit(OnChange onChange) {
		onChange.theAssToStoreRefIn = theAss;
		
		if(null != onChange.applyMethodFuncDef){
			onChange.applyMethodFuncDef.accept(this);
		}
		if(null != onChange.cleanUpMethodFuncDef){
			onChange.cleanUpMethodFuncDef.accept(this);
		}
		if(null != onChange.initMethodNameFuncDef){
			onChange.initMethodNameFuncDef.accept(this);
		}
		
		return true;
	}

	@Override
	public Object visit(Changed changed) {
		return true;
	}

	@Override
	public Object visit(AsyncBodyBlock asyncBodyBlock) {
		for(Block b : asyncBodyBlock.preBlocks){
			b.accept(this);
		}
		
		asyncBodyBlock.mainBody.accept(this);
		
		for(Block b : asyncBodyBlock.postBlocks){
			b.accept(this);
		}
		
		asyncBodyBlock.theAssToStoreRefIn = theAss;
		
		return true;
	}

	@Override
	public Object visit(NOP nop) {
		return false;
	}

	@Override
	public Object visit(EnumItem enumItem) {
		return false;
	}

	@Override
	public Object visit(EnumBlock enumBlock) {
		return false;
	}
	
	@Override
	public Object visit(InitBlock initBlock){
		return false;//point is?
	}
	
	@Override
	public Object visit(Annotation annotation){
		return false;//ignore non functional
	}
	
	@Override
	public Object visit(Annotations annotations){
		return false;//ignore non functional
	}
	
	@Override
	public Object visit(AnnotationDef annotations){
		return false;//ignore non functional
	}

	@Override
	public Object visit(TypedefStatement typedefStatement) {
		return false;
	}

	@Override
	public Object visit(CaseExpressionWrapper caseExpressionWrapper) {
		return false;
	}

	@Override
	public Object visit(CaseExpressionPre caseExpressionPre) {
		return false;
	}

	@Override
	public Object visit(CaseExpressionPost caseExpressionPost) {
		return false;
	}


	@Override
	public Object visit(CaseExpressionAnd caseExpressionAnd) {
		return false;
	}

	@Override
	public Object visit(CaseExpressionOr caseExpressionOr) {
		return false;
	}
	
	@Override
	public Object visit(JustAlsoCaseExpression justAlsoCaseExpression){
		return false;
	}
	
	@Override
	public Object visit(CaseExpressionAssign caseExpressionUntypedAssign){
		return false;
	}
	
	@Override
	public Object visit(CaseExpressionAssignTuple caseExpressionAssignTuple) {
		return false;
	}

	@Override
	public Object visit(TypedCaseExpression typedCaseExpression) {
		return false;
	}	

	@Override
	public Object visit(DMANewFromExpression dmaNewFromExpression){
		return false;
	}

	@Override
	public Object visit(LocalClassDef localClassDef) {
		return false;
	}

	@Override
	public Object visit(TypeReturningExpression typeReturningExpression) {
		return false;
	}
	@Override
	public Object visit(TransBlock transBlock) {
		return false;
	}
	@Override
	public Object visit(Vectorized vectorized) {
		return false;
	}
	@Override
	public Object visit(VectorizedFuncInvoke vectorizedFuncInvoke) {
		return false;
	}
	
	@Override
	public Object visit(VectorizedFuncRef vectorizedFuncInvoke) {
		return false;
	}

	@Override
	public Object visit(MultiType multiType) {
		return false;
	}
	
	
	@Override
	public Object visit(VectorizedFieldRef vectorizedFieldRef) {
		return false;
	}
	

	@Override
	public Object visit(VectorizedNew vectorizedNew) {
		return false;
	}
	@Override
	public Object visit(VectorizedArrayRef vectArrayRef) {
		return false;
	}
	@Override
	public Object visit(ImpliInstance impliInstance) {
		return false;
	}
	@Override
	public Object visit(PointerAddress pointerAddress) {
		return false;
	}
	@Override
	public Object visit(PointerUnref pointerAddress) {
		return false;
	}
	@Override
	public Object visit(AssignMulti multiAssign) {
		return multiAssign.rhs.accept(this);
	}
	
	@Override
	public Object visit(JustLoad justLoad) {
		return false;//ignore, used in bytecode genneration only
	}
	@Override
	public Object visit(TupleExpression tupleExpression) {
		return false;
	}
	
	@Override
	public Object visit(AssignTupleDeref assignTupleDeref) {
		return assignTupleDeref.expr.accept(this);
	}
	@Override
	public Object visit(AnonLambdaDef anonLambdaDef) {

		return false;
	}
	@Override
	public Object visit(CaseExpressionTuple caseExpressionTuple) {
		return false;
	}
	
	@Override
	public Object visit(CaseExpressionObjectTypeAssign caseExpressionObjectTypeAssign) {
		return false;
	}
	
	@Override
	public Object visit(ObjectProvider objectProvider) {
		return false;
	}
	
	@Override
	public Object visit(ObjectProviderBlock objectProviderBlock) {
		return false;
	}
	@Override
	public Object visit(ObjectProviderLineDepToExpr objectProviderLineDepToExpr) {
		return false;
	}
	
	@Override
	public Object visit(ObjectProviderLineProvide objectProviderLineProvide) {
		return false;
	}
	
	@Override
	public Object visit(NotNullAssertion notNullAssertion) {
		return false;
	}
	@Override
	public Object visit(ElvisOperator notNullAssertion) {
		return false;
	}
	@Override
	public Object visit(LangExt langExt) {
		return false;
	}
}