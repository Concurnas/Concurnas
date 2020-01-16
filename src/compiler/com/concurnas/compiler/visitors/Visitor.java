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

public interface Visitor {
	
	public void pushErrorContext(REPLTopLevelComponent xxx);
	public REPLTopLevelComponent popErrorContext();
	
	Object visit(Additive addMinusExpression);
	
	Object visit(AddMinusExpressionElement addMinusExpressionElement);

	Object visit(AndExpression andExpression);

	Object visit(ArrayConstructor arrayConstructor);

	Object visit(ArrayRefElementPrefixAll arrayRefElementPrefixAll);

	Object visit(ArrayRefElementPostfixAll arrayRefElementPostfixAll);
	
	Object visit(ArrayRefElementSubList arrayRef);

	Object visit(ArrayRefElement arrayRefElement);

	Object visit(ArrayRef arrayRef);

	Object visit(ArrayDef arrayDef);

	Object visit(AsyncBlock asyncBlock);

	Object visit(AssignNew assignNew);

	Object visit(AssignExisting assignExisting);

	Object visit(AssertStatement assertStatement);

	Object visit(New namedConstructor);

	Object visit(MulerExpression mulerExpression);

	Object visit(MulerElement mulerElement);

	Object visit(MapDefElement mapDefElement);
	
	Object visit(MapDefaultElement mapDefElement);

	Object visit(MapDef mapDef);

	Object visit(Is instanceOf);

	Object visit(ImportStar importStar);

	Object visit(ImportImport importImport);

	Object visit(ImportFrom importFrom);

	Object visit(ImportAsName importAsName);

	Object visit(IfStatement ifStatement);

	Object visit(FuncType funcType);

	Object visit(FuncRefInvoke funcRefInvoke);

	Object visit(FuncRef funcRef);
	
	Object visit(FuncParams funcParams);

	Object visit(FuncParam funcParam);

	Object visit(FuncInvokeArgs funcInvokeArgs);

	Object visit(FuncInvoke funcInvoke);

	Object visit(FuncDef funcDef);
	Object visit(ConstructorDef funcDef);

	Object visit(ForBlock forBlock);

	Object visit(ElifUnit elifUnit);

	Object visit(DuffAssign duffAssign);

	Object visit(DottedNameList dottedNameList);

	Object visit(DottedAsName dottedAsName);

	Object visit(DotOperator dotOperator);

	Object visit(ContinueStatement continueStatement);

	Object visit(ClassDefArgs classDefArgs);

	Object visit(ClassDefArg classDefArg);

	Object visit(ClassDef classDef);

	Object visit(CatchBlocks catchBlocks);

	Object visit(CastExpression castExpression);

	Object visit(BreakStatement breakStatement);

	Object visit(Block block);

	Object visit(WithBlock withBlock);

	Object visit(WhileBlock whileBlock);

	Object visit(VarString varString);
	
	Object visit(VarRegexPattern varRegexPattern);

	Object visit(VarNull varNull);

	Object visit(VarInt varInt);

	Object visit(VarFloat varFloat);

	Object visit(VarDouble varDouble);


	Object visit(TryCatch tryCatch);

	Object visit(ThrowStatement throwStatement);

	//Object visit(SyncBlock syncBlock);

	Object visit(ReturnStatement returnStatement);

	Object visit(RefThis refThis);
	
	Object visit(RefOf refThis);

	Object visit(RefSuper refSuper);
	
	Object visit(RefClass refClass);

	Object visit(RefName refName);

	Object visit(RefBoolean refBoolean);

	Object visit(PrimativeType primativeType);

	Object visit(PrefixOp prefixOp);

	Object visit(PowOperator powOperator);

	Object visit(PostfixOp postfixOp);

	Object visit(OrExpression orExpression);

	Object visit(NotExpression notExpression);
	
	Object visit(CopyExpression copyExpression);
	
	Object visit(NamedType namedType);

	Object visit(VarLong varLong);

	//Object visit(IfExpr ifExpr);

	Object visit(LambdaDef lambdaDef);

	Object visit(GrandLogicalElement equalityElement);

	Object visit(EqReExpression equalityExpression);

	Object visit(LineHolder lineHolder);

	Object visit(ThisConstructorInvoke thisConstructorInvoke);

	Object visit(SuperConstructorInvoke superConstructorInvoke);

	Object visit(FuncRefArgs funcRefArgs);

	Object visit(VarChar varChar);

	Object visit(ForBlockOld forBlockOld);

	Collection<? extends ErrorHolder> getErrors();

	Object visit(GetSetOperation getSetOperation);

	Object visit(GenericType genericType);

	Object visit(AsyncRefRef asyncRefRef);

	Object visit(OnChange onChange);
	
	Object visit(Await onChange);
	
	Object visit(OnEvery onChange);
	
	Object visit(Changed changed);
	
	Object visit(AsyncBodyBlock asyncBodyBlock);

	Object visit(NOP nop);
	
	Object visit(InExpression cont);

	Object visit(EnumDef enumDef);

	Object visit(EnumItem enumItem);

	Object visit(EnumBlock enumBlock);

	Object visit(InitBlock initBlock);

	Object visit(Annotation annotation);

	Object visit(Annotations annotations);

	Object visit(AnnotationDef annotationDef);

	Object visit(TypedefStatement typedefStatement);

	Object visit(MatchStatement matchStatement);

	Object visit(CaseExpressionWrapper caseExpressionWrapper);
	
	Object visit(CaseExpressionPre caseExpressionPre);

	Object visit(CaseExpressionTuple caseExpressionTuple);
	
	Object visit(CaseExpressionPost caseExpressionPost);

	Object visit(CaseExpressionAnd caseExpressionAnd);

	Object visit(CaseExpressionOr caseExpressionOr);

	Object visit(CaseExpressionAssign caseExpressionUntypedAssign);
	
	Object visit(CaseExpressionAssignTuple caseExpressionAssignTuple);

	Object visit(TypedCaseExpression typedCaseExpression);

	Object visit(JustAlsoCaseExpression justAlsoCaseExpression);
	
	Object visit(DeleteStatement deleteStatement);

	Object visit(DMANewFromExpression dmaNewFromExpression);

	Object visit(SizeofStatement sizeofStatement);

	Object visit(LocalClassDef localClassDef);

	Object visit(TypeReturningExpression typeReturningExpression);

	Object visit(TransBlock transBlock);
	
	public void resetLastLineVisited();
	public int getLastLineVisited();
	public void setLastLineVisited(int lineNo);

	Object visit(ShiftElement shiftElement);

	Object visit(ShiftExpression shiftExpression);

	Object visit(BitwiseOperation bitwiseOperation);

	Object visit(ExpressionList expressionList);

	Object visit(ArrayDefComplex arrayDefComplex);

	Object visit(Vectorized vectorized);

	Object visit(VectorizedFuncInvoke vectorizedFuncInvoke);

	Object visit(VectorizedFuncRef vectorizedFuncRef);

	Object visit(MultiType multiType);

	Object visit(VectorizedFieldRef vectorizedFieldRef);
	
	Object visit(VectorizedNew vectorizedNew);
	
	Object visit(VectorizedArrayRef vectArrayRef);

	Object visit(VarShort varShort);

	Object visit(ImpliInstance impliInstance);

	Object visit(PointerAddress pointerAddress);

	Object visit(PointerUnref pointerUnref);

	Object visit(AssignMulti multiAssign);

	Object visit(JustLoad justLoad);

	Object visit(TupleExpression tupleExpression);

	Object visit(AssignTupleDeref assignTupleDeref);

	Object visit(AnonLambdaDef anonLambdaDef);

	Object visit(CaseExpressionObjectTypeAssign caseExpressionObjectTypeAssign);

	Object visit(ObjectProvider objectProvider);
	Object visit(ObjectProviderBlock objectProviderBlock);
	Object visit(ObjectProviderLineDepToExpr objectProviderLineDepToExpr);
	Object visit(ObjectProviderLineProvide objectProviderLineProvide);

	Object visit(NotNullAssertion notNullAssertion);
	Object visit(ElvisOperator evlisOperator);

	Object visit(LangExt langExt);
}