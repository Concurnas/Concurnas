package com.concurnas.compiler.visitors;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.ast.AddMinusExpressionElement;
import com.concurnas.compiler.ast.Additive;
import com.concurnas.compiler.ast.ArrayDef;
import com.concurnas.compiler.ast.ArrayRefElement;
import com.concurnas.compiler.ast.ArrayRefElementPostfixAll;
import com.concurnas.compiler.ast.ArrayRefElementPrefixAll;
import com.concurnas.compiler.ast.ArrayRefElementSubList;
import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.ast.CastExpression;
import com.concurnas.compiler.ast.ElifUnit;
import com.concurnas.compiler.ast.EqReExpression;
import com.concurnas.compiler.ast.FuncInvoke;
import com.concurnas.compiler.ast.FuncRef;
import com.concurnas.compiler.ast.FuncRefInvoke;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.GrandLogicalElement;
import com.concurnas.compiler.ast.IfStatement;
import com.concurnas.compiler.ast.IsAMapElement;
import com.concurnas.compiler.ast.MapDef;
import com.concurnas.compiler.ast.MapDefElement;
import com.concurnas.compiler.ast.MapDefaultElement;
import com.concurnas.compiler.ast.MulerElement;
import com.concurnas.compiler.ast.MulerExpression;
import com.concurnas.compiler.ast.New;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.PowOperator;
import com.concurnas.compiler.ast.SuperConstructorInvoke;
import com.concurnas.compiler.ast.SuperOrThisConstructorInvoke;
import com.concurnas.compiler.ast.ThisConstructorInvoke;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.interfaces.Expression;


/**
 * Tag postfix operations with returning their ref value on the stack instead of the ref itself
 * caters for the following case:
 * 	      	   a := 5; b = a++ //b should be 5, so we have to dupe the previous value contained in the ref
		note:: b := a++// b will still be 6 (well, 6:) because geting pointer to ref
 *
 */
public class NonRefPostfixVisitorTagger extends AbstractErrorRaiseVisitor {
	public NonRefPostfixVisitorTagger(String fullPathFileName) {
		super(fullPathFileName);
	}
	
	
	private void tagNodeWithExpectNonRef(Type gotType, Type expected, Node offender){
		int gotLevels = TypeCheckUtils.getRefLevels(gotType);
		if(gotLevels > 0 && TypeCheckUtils.getRefLevels(expected) < gotLevels){
			offender.setExpectNonRef(true);	
		}
	}
	
	private void tagNodeWithExpectNonRef(Type expected, Node offender){
		tagNodeWithExpectNonRef(offender.getTaggedType(), expected, offender);
	}
	
	@Override
	public Object visit(AssignNew assignNew) {
		
		
		if(null != assignNew.expr){
			Type rhs = assignNew.expr.getTaggedType();
			Type expected  = assignNew.getTaggedType();
			
			tagNodeWithExpectNonRef(rhs, expected, (Node)assignNew.expr);
		}
	
		return super.visit(assignNew);
	}
	
	@Override
	public Object visit(AssignExisting assignExisting) {
		
		
		Type rhs = assignExisting.expr.getTaggedType();
		Type expected  = assignExisting.getTaggedType();
		
		tagNodeWithExpectNonRef(rhs, expected, (Node)assignExisting.expr);
		
		return super.visit(assignExisting);
	}
	
	@Override
	public Object visit(FuncInvoke funcInvoke) {
		
		if(funcInvoke.resolvedFuncTypeAndLocation != null){
			List<Expression> args = funcInvoke.args.getArgumentsWNPs();
			ArrayList<Type> expectedArgs = ((FuncType)TypeCheckUtils.getRefType(funcInvoke.resolvedFuncTypeAndLocation.getType())).getInputs();
			
			for(int n = 0; n < args.size(); n++){
				Node argee = (Node)args.get(n);
				if(null != argee){
					Type got = argee.getTaggedType();
					Type expected = expectedArgs.get(n);
							
					tagNodeWithExpectNonRef(got, expected, argee);
				}
			}
		}
		
		
		return super.visit(funcInvoke);
	}
	
	
	@Override
	public Object visit(New namedConstructor) {
		if(null != namedConstructor.constType){
			namedConstructor.typeee.accept(this);
			
			if(null != namedConstructor.args){//null for actor refs?
				List<Expression> args = namedConstructor.args.getArgumentsWNPs();
				ArrayList<Type> expectedArgs = namedConstructor.constType.getInputs();
				
				for(int n = 0; n < args.size(); n++){
					Node argee = (Node)args.get(n);
					if(null != argee){
						Type got = argee.getTaggedType();
						Type expected = expectedArgs.get(n);
								
						tagNodeWithExpectNonRef(got, expected, argee);
					}
				}
			}
			
		}
		
		return super.visit(namedConstructor);
	}
	
/*	@Override
	public Object visit(IfExpr ifExpr) {
		
		Type expected = ifExpr.getTaggedType();
		
		tagNodeWithExpectNonRef(ifExpr.op1.getTaggedType(), expected, (Node)ifExpr.op1);
		tagNodeWithExpectNonRef(ifExpr.op2.getTaggedType(), expected, (Node)ifExpr.op2);
		
		return super.visit(ifExpr);
	}*/
	
	@Override
	public Object visit(IfStatement ifExpr) {
		
		Type expected = ifExpr.getTaggedType();
		
		tagNodeWithExpectNonRef(ifExpr.ifblock.getTaggedType(), expected, (Node)ifExpr.ifblock);
		for(ElifUnit eli : ifExpr.elifunits) {
			tagNodeWithExpectNonRef(eli.elifb.getTaggedType(), expected, (Node)eli.elifb);
		}
		
		if(ifExpr.elseb != null){
			tagNodeWithExpectNonRef(ifExpr.elseb.getTaggedType(), expected, (Node)ifExpr.elseb);
		}
		
		return super.visit(ifExpr);
	}
	
	@Override
	public Object visit(ArrayDef arrayDef) {
		Type expected = (Type)arrayDef.getTaggedType();
		//if(null != expected) {
			expected = (Type)expected.copy();
			expected.setArrayLevels(expected.getArrayLevels()-1);
			
			for(Expression e : arrayDef.getArrayElements(this)){
				tagNodeWithExpectNonRef(e.getTaggedType(), expected, (Node)e);
			}
		//}
		
		return super.visit(arrayDef);
	}
	

	@Override
	public Object visit(CastExpression castExpression) {
		tagNodeWithExpectNonRef(castExpression.o.getTaggedType(), castExpression.t, (Node)castExpression.o);
		
		return super.visit(castExpression);
	}
	
	@Override
	public Object visit(MapDef mapDef) {
		for(IsAMapElement ea: mapDef.elements)
		{
			if(ea instanceof MapDefElement){
				MapDefElement e  = (MapDefElement)ea;

				tagNodeWithExpectNonRef(e.getKey(this).getTaggedType(), e.keyType, (Node)e.getKey(this));
				tagNodeWithExpectNonRef(e.getValue(this).getTaggedType(), e.valType, (Node)e.getValue(this));
				
				((MapDefElement)e).accept(this);
			}
			else{
				MapDefaultElement e  = (MapDefaultElement)ea;

				tagNodeWithExpectNonRef(e.value.getTaggedType(), e.valType, (Node)e.value);//todo: if function etc?
				
				((MapDefaultElement)e).accept(this);
			}
		}
		return super.visit(mapDef);
	}
	
	
	
	
	@Override
	public Object visit(ArrayRefElementPrefixAll arrayRefElementPrefixAll) {
		tagNodeWithExpectNonRef(arrayRefElementPrefixAll.e1.getTaggedType(), arrayRefElementPrefixAll.getTaggedType(), (Node)arrayRefElementPrefixAll.e1);
		
		return super.visit(arrayRefElementPrefixAll);
	}

	@Override
	public Object visit(ArrayRefElementPostfixAll arrayRefElementPostfixAll) {
		tagNodeWithExpectNonRef(arrayRefElementPostfixAll.e1.getTaggedType(), arrayRefElementPostfixAll.getTaggedType(), (Node)arrayRefElementPostfixAll.e1);
		return super.visit(arrayRefElementPostfixAll);
	}

	@Override
	public Object visit(ArrayRefElementSubList arrayRef) {
		tagNodeWithExpectNonRef(arrayRef.e1.getTaggedType(), arrayRef.getTaggedType(), (Node)arrayRef.e1);
		tagNodeWithExpectNonRef(arrayRef.e2.getTaggedType(), arrayRef.getTaggedType(), (Node)arrayRef.e2);
		return super.visit(arrayRef);
	}

	@Override
	public Object visit(ArrayRefElement arrayRefElement) {
		tagNodeWithExpectNonRef(arrayRefElement.e1.getTaggedType(), arrayRefElement.getTaggedType(), (Node)arrayRefElement.e1);
		return super.visit(arrayRefElement);
	}
	
	
	@Override
	public Object visit(Additive addMinusExpression) {
		Type tt = addMinusExpression.getTaggedType();
		tagNodeWithExpectNonRef(tt, (Node)addMinusExpression.head);
		for(AddMinusExpressionElement i : addMinusExpression.elements)
		{
			tagNodeWithExpectNonRef(tt, (Node)i.exp);
		}
		return super.visit(addMinusExpression);
	}

	
	
	@Override
	public Object visit(MulerExpression mulerExpression) {
		Type tt = mulerExpression.getTaggedType();
		tagNodeWithExpectNonRef(tt, (Node)mulerExpression.header);
		for(MulerElement i : mulerExpression.elements)
		{
			tagNodeWithExpectNonRef(tt, (Node)i.expr);
		}
		
		return super.visit(mulerExpression);
	}
	
	@Override
	public Object visit(PowOperator powOperator) {
		Type tt = powOperator.getTaggedType();
		
		tagNodeWithExpectNonRef(tt, (Node)powOperator.expr);
		tagNodeWithExpectNonRef(tt, (Node)powOperator.raiseTo);
		
		return super.visit(powOperator);
	}
	
	@Override
	public Object visit(EqReExpression equalityExpression) {
		Type tt = equalityExpression.getTaggedType();
		tagNodeWithExpectNonRef(tt, (Node)equalityExpression.head);
		for(GrandLogicalElement i : equalityExpression.elements)
		{
			tagNodeWithExpectNonRef(tt, (Node)i.e2);
		}
		
		return super.visit(equalityExpression);
	}
	
	
	
	
	public void processThisOrSuperTypeConstrInvoke(SuperOrThisConstructorInvoke superConstructorInvoke) {
		if(null != superConstructorInvoke.resolvedFuncType){
			ArrayList<Type> args = superConstructorInvoke.resolvedFuncType.getInputs();
			List<Expression>  exprs = superConstructorInvoke.args.getArgumentsWNPs();
			for(int n=0; n <exprs.size(); n++ ){
				Node expr = (Node)exprs.get(n);
				if(null != expr){
					tagNodeWithExpectNonRef(args.get(n), expr);
				}
			}
		}
		
	}
	
	@Override
	public Object visit(SuperConstructorInvoke superConstructorInvoke) {
		 processThisOrSuperTypeConstrInvoke(superConstructorInvoke);
		 return super.visit(superConstructorInvoke);
	}
	
	@Override
	public Object visit(ThisConstructorInvoke thisConstructorInvoke) {
		processThisOrSuperTypeConstrInvoke(thisConstructorInvoke);
		return super.visit(thisConstructorInvoke);
	}
	
	
	
	
	
	@Override
	public Object visit(FuncRefInvoke funcRefInvoke) {
		
		ArrayList<Type> args = ((FuncType)TypeCheckUtils.getRefType(funcRefInvoke.resolvedInputType/*.funcRef.getTaggedType()*/)).getInputs();
		for(int n=0; n <funcRefInvoke.getArgs().asnames.size(); n++ ){
			tagNodeWithExpectNonRef(args.get(n), (Node)funcRefInvoke.getArgs().asnames.get(n));
		}
		
		
		return super.visit(funcRefInvoke);
	}

	@Override
	public Object visit(FuncRef funcRef) {
		if(null != funcRef.argsForNextCompCycle){
			ArrayList<Type> argsNotCurryIn = funcRef.argumentsThatDontNeedToBecurriedIn;
			ArrayList<Expression> bound = funcRef.argsForNextCompCycle.getBoundArgs();
			
			//if(argsNotCurryIn.size() == bound.size()) {
				for(int n=0; n < bound.size(); n++ ){
					tagNodeWithExpectNonRef(argsNotCurryIn.get(n), (Node)bound.get(n));
				}
			//}
			
		}
		
		
		return super.visit(funcRef);
	}
}

