package com.concurnas.compiler.visitors;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.concurnas.compiler.ast.Additive;
import com.concurnas.compiler.ast.AndExpression;
import com.concurnas.compiler.ast.ArrayConstructor;
import com.concurnas.compiler.ast.ArrayDef;
import com.concurnas.compiler.ast.ArrayDefComplex;
import com.concurnas.compiler.ast.ArrayRef;
import com.concurnas.compiler.ast.ArrayRefElement;
import com.concurnas.compiler.ast.ArrayRefElementPostfixAll;
import com.concurnas.compiler.ast.ArrayRefElementPrefixAll;
import com.concurnas.compiler.ast.ArrayRefElementSubList;
import com.concurnas.compiler.ast.AssertStatement;
import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.ast.AsyncBlock;
import com.concurnas.compiler.ast.AsyncRefRef;
import com.concurnas.compiler.ast.BitwiseOperation;
import com.concurnas.compiler.ast.CastExpression;
import com.concurnas.compiler.ast.DMANewFromExpression;
import com.concurnas.compiler.ast.DeleteStatement;
import com.concurnas.compiler.ast.DotOperator;
import com.concurnas.compiler.ast.ElifUnit;
import com.concurnas.compiler.ast.EqReExpression;
import com.concurnas.compiler.ast.ForBlock;
import com.concurnas.compiler.ast.ForBlockOld;
import com.concurnas.compiler.ast.FuncInvoke;
import com.concurnas.compiler.ast.FuncRef;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.IfStatement;
import com.concurnas.compiler.ast.InExpression;
import com.concurnas.compiler.ast.Is;
import com.concurnas.compiler.ast.IsAMapElement;
import com.concurnas.compiler.ast.MapDef;
import com.concurnas.compiler.ast.MapDefElement;
import com.concurnas.compiler.ast.MapDefaultElement;
import com.concurnas.compiler.ast.MulerExpression;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.New;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.NotExpression;
import com.concurnas.compiler.ast.OnChange;
import com.concurnas.compiler.ast.OnEvery;
import com.concurnas.compiler.ast.OrExpression;
import com.concurnas.compiler.ast.PostfixOp;
import com.concurnas.compiler.ast.PowOperator;
import com.concurnas.compiler.ast.PrefixOp;
import com.concurnas.compiler.ast.ShiftExpression;
import com.concurnas.compiler.ast.SizeofStatement;
import com.concurnas.compiler.ast.ThrowStatement;
import com.concurnas.compiler.ast.TryCatch;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.WhileBlock;
import com.concurnas.compiler.ast.WithBlock;
import com.concurnas.compiler.ast.interfaces.Expression;

/**
 * In cases such as this:
 * 
 * @com.concurnas.lang.DeleteOnUnusedReturn
 * def refRetCall() => 12:
 * 
 * a int = refRetCall()
 * above, we need to call the delete method before we unref the thing
 * 
 * likewiese
 * 	acall(refRetCall())
 *  
 * and
 * 	[refRetCall() 2 3]
 * 
 * but not:
 * 	[refRetCall(), 2:] 
 *
 */
public class ImplicitUnrefNeedsDelete extends AbstractVisitor implements Visitor, Unskippable {

	private Stack<Boolean> refFromFuncInvokeNeedsTag = new Stack<Boolean>();
	
	private void tagProducerOfTypeIfUnreffed(Expression toEval, Type potentiallyUnreffedTo) {
		tagProducerOfTypeIfUnreffed(toEval, potentiallyUnreffedTo, false);
	}
	
	
	private void evalSetUnrefStack(Node toEval) {
		if(null != toEval) {
			refFromFuncInvokeNeedsTag.push(true);
			toEval.accept(this);
			refFromFuncInvokeNeedsTag.pop();
		}
		
	}
	
	private void tagProducerOfTypeIfUnreffed(Expression toEval, Type potentiallyUnreffedTo, boolean clearRefTagStack) {
		if(null != toEval && potentiallyUnreffedTo != null) {
			boolean tag = false;
			Type origin = toEval.getTaggedType();
			
			potentiallyUnreffedTo = TypeCheckUtils.boxTypeIfPrimative(potentiallyUnreffedTo, false);
			
			if(origin != null && !origin.equals(potentiallyUnreffedTo) && TypeCheckUtils.hasRefLevels(origin)) {	
				
				for(Type rr : TypeCheckUtils.extractRefTypes(origin)) {
					if(rr.equals(potentiallyUnreffedTo)) {
						tag=true;
						break;
					}
				}
			}
			if(tag || clearRefTagStack) {
				refFromFuncInvokeNeedsTag.push(tag);
				toEval.accept(this);
				refFromFuncInvokeNeedsTag.pop();
			}else {
				toEval.accept(this);
			}
		}
	}
	
	@Override
	public Object visit(AssignExisting assignExisting) {
		Type lhs = assignExisting.getTaggedType();
		tagProducerOfTypeIfUnreffed(assignExisting.expr, lhs);
		return null;
	}
	

	@Override
	public Object visit(AssignNew assignNew) {
		Type lhs = assignNew.getTaggedType();
		tagProducerOfTypeIfUnreffed(assignNew.expr, lhs);
		return null;
	}

	@Override
	public Object visit(FuncInvoke funcInvoke) {
		if(!refFromFuncInvokeNeedsTag.isEmpty() && refFromFuncInvokeNeedsTag.peek()) {
			funcInvoke.refShouldBeDeletedOnUsusedReturn=true;
		}
		
		if(null != funcInvoke.resolvedFuncTypeAndLocation) {
			Type resolve = funcInvoke.resolvedFuncTypeAndLocation.getType();
			if(resolve instanceof FuncType) {
				FuncType ft = (FuncType)resolve;
				List<Expression> leArgs = funcInvoke.args.getArgumentsWNPs();
				int sz = leArgs.size();
				int m=ft.extFuncOn?1:0;
				for(int n=0; n < sz; n++) {
					tagProducerOfTypeIfUnreffed(leArgs.get(n), ft.inputs.get(m), true);
					m++;
				}
			}
		}
		
		
		return funcInvoke.getTaggedType();
	}
	
	
/*	@Override
	public Object visit(IfExpr ifExpr) {
		ifExpr.test.accept(this);
		
		Type ret = ifExpr.getTaggedType();

		tagProducerOfTypeIfUnreffed(ifExpr.op1, ret);
		tagProducerOfTypeIfUnreffed(ifExpr.op2, ret);
		return ret;
	}*/

	@Override
	public Object visit(IfStatement ifStatement) {
		evalSetUnrefStack((Node)ifStatement.iftest);
		
		Type ret = ifStatement.getTaggedType();

		tagProducerOfTypeIfUnreffed(ifStatement.ifblock, ret); 
 		
		for(ElifUnit elif : ifStatement.elifunits) {
			evalSetUnrefStack((Node)elif.eliftest);
			tagProducerOfTypeIfUnreffed(elif.elifb, ret); 
		}

		if(null != ifStatement.elseb) {
			tagProducerOfTypeIfUnreffed(ifStatement.elseb, ret); 
		}
		
		return null;
	}
	
	@Override
	public Object visit(Additive addMinusExpression) {
		evalSetUnrefStack((Node)addMinusExpression.head);
		addMinusExpression.elements.forEach(a -> evalSetUnrefStack((Node)a));
		
		return null;
	}

	@Override
	public Object visit(AndExpression andExpression) {
		evalSetUnrefStack((Node)andExpression.head);
		andExpression.things.forEach(a -> evalSetUnrefStack((Node)a));
		
		return null;
	}

	@Override
	public Object visit(ArrayConstructor arrayConstructor) {
		evalSetUnrefStack((Node)arrayConstructor.defaultValue);
		return null;
	}

	@Override
	public Object visit(ArrayRefElementPrefixAll arrayRefElementPrefixAll) {
		evalSetUnrefStack((Node)arrayRefElementPrefixAll.e1);
		return null;
	}

	@Override
	public Object visit(ArrayRefElementPostfixAll arrayRefElementPostfixAll) {
		evalSetUnrefStack((Node)arrayRefElementPostfixAll.e1);
		return null;
	}

	@Override
	public Object visit(ArrayRefElementSubList arrayRef) {
		evalSetUnrefStack((Node)arrayRef.e1);
		evalSetUnrefStack((Node)arrayRef.e2);
		return null;
	}

	@Override
	public Object visit(ArrayRefElement arrayRefElement) {
		evalSetUnrefStack((Node)arrayRefElement.e1);
		return null;
	}

	@Override
	public Object visit(ArrayRef arrayRef) {
		evalSetUnrefStack((Node)arrayRef.expr);
		super.processArrayElements(arrayRef.arrayLevelElements);
		return null;
	}
	
	private Type arTypeMinus1(Type what) {
		Type ret;
		if(what.hasArrayLevels()) {
			ret = (Type)what.copy();
			ret.setArrayLevels(what.getArrayLevels()-1);
		}else if(what instanceof NamedType) {//list
			ret = ((NamedType)what).getGenericTypeElements().get(0);
		}else {
			ret = what;
		}
		return ret;
	}

	@Override
	public Object visit(ArrayDef arrayDef) {
		Type ret = arTypeMinus1(arrayDef.getTaggedType());
		
		arrayDef.getArrayElements(this).forEach(a -> tagProducerOfTypeIfUnreffed(a, ret) );
		
		return null;
	}


	@Override
	public Object visit(ArrayDefComplex arrayDefComplex) {
		Type ret = arTypeMinus1(arrayDefComplex.getTaggedType());

		arrayDefComplex.getArrayElements(this).forEach(a -> tagProducerOfTypeIfUnreffed(a, ret) );
		
		return null;
	}
	
	@Override
	public Object visit(AsyncBlock asyncBlock) {
		tagProducerOfTypeIfUnreffed(asyncBlock.body, asyncBlock.getTaggedType());
		evalSetUnrefStack((Node)asyncBlock.executor);
		
		return null;
	}

	@Override
	public Object visit(AssertStatement assertStatement) {
		evalSetUnrefStack((Node)assertStatement.e);
		
		return null;
	}

	@Override
	public Object visit(New namedConstructor) {
		FuncType ft = namedConstructor.constType;
		if(ft != null && null != namedConstructor.args) {
			List<Expression> argz = namedConstructor.args.getArgumentsWNPs();
			int sz = argz.size();
			int m=namedConstructor.actingOn!=null?1:0;
			for(int n=0; n < sz; n++) {
				tagProducerOfTypeIfUnreffed(argz.get(n), ft.inputs.get(m), true);
				m++;
			}
		}
		
		return null;
	}

	@Override
	public Object visit(MulerExpression mulerExpression) {
		evalSetUnrefStack((Node)mulerExpression.header);
		mulerExpression.elements.forEach(a -> evalSetUnrefStack((Node)a));
		
		return null;
	}


	@Override
	public Object visit(MapDef mapDef) {
		
		for(IsAMapElement ea : mapDef.elements){
			if(ea instanceof MapDefElement){
				MapDefElement e = (MapDefElement)ea;
				tagProducerOfTypeIfUnreffed(e.getKey(this), e.keyType); 
				tagProducerOfTypeIfUnreffed(e.getValue(this), e.valType); 
			}
			else{
				MapDefaultElement def = (MapDefaultElement)ea;
				tagProducerOfTypeIfUnreffed(def.value, def.valType); 
			}
		}
		
		return null;
	}

	@Override
	public Object visit(Is instanceOf) {
		evalSetUnrefStack((Node)instanceOf.e1);
		return null;
	}


	@Override
	public Object visit(FuncRef funcRef) {
		if(funcRef.argsForNextCompCycle != null) {
			ArrayList<Expression> gotArgs = funcRef.argsForNextCompCycle.getBoundArgs();//.getArgsAndLambdaConsts();
			ArrayList<Type> inputArgs = funcRef.argumentsThatDontNeedToBecurriedIn; // expectedType.inputs;
			int sz = gotArgs.size();
			//if(inputArgs.size() == sz) {
				for(int n=0; n < sz; n++) {
					tagProducerOfTypeIfUnreffed(gotArgs.get(n), inputArgs.get(n)); 
				}
			//}
		}
		
		evalSetUnrefStack((Node)funcRef.functo);
		
		return null;
	}


	@Override
	public Object visit(ForBlock forBlock) {
		tagProducerOfTypeIfUnreffed(forBlock.expr, forBlock.localVarTypeToAssign);
		tagProducerOfTypeIfUnreffed(forBlock.block, forBlock.getTaggedType());
		tagProducerOfTypeIfUnreffed(forBlock.elseblock, forBlock.getTaggedType());
		return null;
	}



	@Override
	public Object visit(CastExpression castExpression) {
		tagProducerOfTypeIfUnreffed(castExpression.o, castExpression.getTaggedType()); 
		return null;
	}



	@Override
	public Object visit(WithBlock withBlock) {
		evalSetUnrefStack((Node)withBlock.expr);
		tagProducerOfTypeIfUnreffed(withBlock.blk, withBlock.getTaggedType());
		return null;
	}

	@Override
	public Object visit(WhileBlock whileBlock) {
		evalSetUnrefStack((Node)whileBlock.cond);
		tagProducerOfTypeIfUnreffed(whileBlock.block, whileBlock.getTaggedType());
		if(null != whileBlock.elseblock) {
			tagProducerOfTypeIfUnreffed(whileBlock.elseblock, whileBlock.getTaggedType());
		}
		return null;
	}


	@Override
	public Object visit(TryCatch tryCatch) {
		tagProducerOfTypeIfUnreffed(tryCatch.blockToTry, tryCatch.getTaggedType());
		
		if(tryCatch.finalBlock != null) {
			tagProducerOfTypeIfUnreffed(tryCatch.finalBlock, tryCatch.getTaggedType());
		}
		
		tryCatch.cbs.forEach(a-> tagProducerOfTypeIfUnreffed(a.catchBlock, tryCatch.getTaggedType()));
		
		return null;
	}

	@Override
	public Object visit(ThrowStatement throwStatement) {
		evalSetUnrefStack((Node)throwStatement.thingTothrow);
		return null;
	}

	@Override
	public Object visit(PrefixOp prefixOp) {
		evalSetUnrefStack((Node)prefixOp.p1);
		return null;
	}

	@Override
	public Object visit(PowOperator powOperator) {
		evalSetUnrefStack((Node)powOperator.expr);
		evalSetUnrefStack((Node)powOperator.raiseTo);
		
		return null;
	}

	@Override
	public Object visit(PostfixOp postfixOp) {
		evalSetUnrefStack((Node)postfixOp.p2);
		return null;
	}

	@Override
	public Object visit(OrExpression orExpression) {
		evalSetUnrefStack((Node)orExpression.head);
		orExpression.things.forEach(a -> evalSetUnrefStack((Node)a));
		return null;
	}

	@Override
	public Object visit(NotExpression notExpression) {
		evalSetUnrefStack((Node)notExpression.expr);
		return null;
	}

	@Override
	public Object visit(EqReExpression equalityExpression) {
		evalSetUnrefStack((Node)equalityExpression.head);
		equalityExpression.elements.forEach(a -> evalSetUnrefStack((Node)a));
		
		return null;
	}


	@Override
	public Object visit(ForBlockOld forBlockOld) {

		evalSetUnrefStack((Node)forBlockOld.assigFrom);
		evalSetUnrefStack((Node)forBlockOld.check);
		evalSetUnrefStack((Node)forBlockOld.postExpr);
		
		tagProducerOfTypeIfUnreffed(forBlockOld.block, forBlockOld.getTaggedType());
		tagProducerOfTypeIfUnreffed(forBlockOld.elseblock, forBlockOld.getTaggedType());
		
		return null;
	}

	@Override
	public Object visit(DotOperator dotOperator) {
		//lastLineVisited=dotOperator.getLine();
		ArrayList<Expression> items = dotOperator.getElements(this);
		int n=0;
		for(Expression e: items)
		{
			if(e instanceof AsyncRefRef) {
				AsyncRefRef asAsyncRefRef = (AsyncRefRef)e;
				if(n != items.size()-1) {
					if(asAsyncRefRef.b instanceof FuncInvoke) {
						((FuncInvoke)asAsyncRefRef.b).refShouldBeDeletedOnUsusedReturn=true;
					}
				}
				
				asAsyncRefRef.accept(this);
				
			}else if(e instanceof FuncInvoke) {
				if(n != items.size()-1) {
					if(!dotOperator.returnCalledOn.get(n)) {//exclude aa()..get(), as ret thing from aa()
						((FuncInvoke)e).refShouldBeDeletedOnUsusedReturn=true;
					}
				}else {
					//((FuncInvoke)e).refShouldBeDeletedOnUsusedReturn=true;
					e.accept(this);
				}
				
			}
			else {
				e.accept(this);
			}
			
			n++;
		}
		return null;
	}
	
	//private HashMap<AsyncRefRef, Expression> asyncrefRefToNextDude = new HashMap<AsyncRefRef, Expression>();
	
	@Override
	public Object visit(AsyncRefRef asyncRefRef) {
		tagProducerOfTypeIfUnreffed(asyncRefRef.b, asyncRefRef.getTaggedType());
		//is funcinvoke followed by a :get() - etc, then tag this, ignore .. though 
		
		return null;
	}

	@Override
	public Object visit(OnChange onChange) {
		super.visit(onChange);
		tagProducerOfTypeIfUnreffed(onChange.body, onChange.getTaggedType());
		return null;
	}

	@Override
	public Object visit(OnEvery onChange) {
		super.visit(onChange);
		tagProducerOfTypeIfUnreffed(onChange.body, onChange.getTaggedType());
		return null;
	}

	@Override
	public Object visit(InExpression cont) {
		evalSetUnrefStack((Node)cont.thing);
		evalSetUnrefStack((Node)cont.insideof);
		return null;
	}

	@Override
	public Object visit(DeleteStatement deleteStatement) {
		deleteStatement.exprs.forEach(a -> evalSetUnrefStack((Node)a));
		return null;
	}

	@Override
	public Object visit(DMANewFromExpression dmaNewFromExpression) {
		evalSetUnrefStack((Node)dmaNewFromExpression.e);
		return null;
	}

	@Override
	public Object visit(SizeofStatement sizeofStatement) {
		evalSetUnrefStack((Node)sizeofStatement.e);
		return null;
	}

	@Override
	public Object visit(ShiftExpression shiftExpression) {
		evalSetUnrefStack((Node)shiftExpression.header);
		shiftExpression.elements.forEach(a -> evalSetUnrefStack(a));
		return null;
	}

	@Override
	public Object visit(BitwiseOperation bitwiseOperation) {
		evalSetUnrefStack((Node)bitwiseOperation.head);
		bitwiseOperation.things.forEach(a -> evalSetUnrefStack(a));
		return null;
	}
}
