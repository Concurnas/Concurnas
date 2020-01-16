package com.concurnas.compiler.visitors;

import java.lang.reflect.Array;
import java.util.Collection;

import org.objectweb.asm.Opcodes;

import com.concurnas.compiler.CaseExpressionAssign;
import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.ast.*;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.runtime.Pair;

/**
 * Performs constant folding of expressions:
 * 	e.g. a = 5+6 => a = 11 (the rhs plus expression is tagged as having folded expression value of 11 )
 *  [note: null is not considered a constant to fold into]
 *  copied from java spec: 15.28. Constant Expressions
 *  
 *  Will also perform validation of certain elements - on annotion arugments and default values
 *  
 *  this could be better with fields and non local field mapping too (how to do this?) i.e.:
 *  TODO: extend this to precompiled fields, e.g. Math.PI - how does java compiled know this is a costant (yes i know final static, but u can still inint those via a static block so making non constant)
 *    
 *  This allows us to use annotations [though they are ok with arrays], also faciliates the use of direct instantiation for module level constant fields
 *    
 *  
 *  arrays dont count!
 *  
		A compile-time constant expression is an expression denoting a value of primitive type or a String that does not complete abruptly and is composed using only the following:
		Literals of primitive type and literals of type String (-3.10.1, -3.10.2, -3.10.3, -3.10.4, -3.10.5)
		Casts to primitive types and casts to type String (-15.16)
		The unary operators +, -, ~, and ! (but not ++ or --) (-15.15.3, -15.15.4, -15.15.5, -15.15.6)
		The multiplicative operators *, /, and % (-15.17)
		The additive operators + and - (-15.18)
		The shift operators <<, >>, and >>> (-15.19) - CANNOT DO THIS YET
		The relational operators <, <=, >, and >= (but not instanceof) (-15.20)
		The equality operators == and != (-15.21)
		The bitwise and logical operators &, ^, and | (-15.22) - CANT DO THIS YET
		The conditional-and operator && and the conditional-or operator || (-15.23, -15.24)
		The ternary conditional operator ? : (-15.25)
		Parenthesized expressions (-15.8.5) whose contained expression is a constant expression.
		
		CANNOT DO THE FOLLOWING re variable names:
		 -must be final (and static if externally referenced) e.g. Math.PI-
		Simple names (-6.5.6.1) that refer to constant variables (-4.12.4).
		Qualified names (-6.5.6.2) of the form TypeName . Identifier that refer to constant variables (-4.12.4).
		
		Compile-time constant expressions of type String are always "interned" so as to share unique instances, using the method String.intern.
		A compile-time constant expression is always treated as FP-strict (-15.4), even if it occurs in a context where a non-constant expression would not be considered to be FP-strict.

 * @author Jason
 *
 */
public class ConstantFolding extends AbstractErrorRaiseVisitor implements Visitor {
	//constant types foldable...
	//TODO: add fields: specifically final static fields
	//TODO: add constant strings with varags: "one %s" % something etc
	
	public ConstantFolding(String fullPathFileName) {
		super(fullPathFileName);
	}
	
	@Override
	public Object visit(VarString varString) {
		if(varString.subExpressions != null && !varString.subExpressions.isEmpty()){
			//TODO: ensure all are constants
			String soFar = "";
			for(Expression expr : varString.subExpressions){
				Object got = expr.accept(this);
				if(got != null){
					soFar += got;
				}
				else{
					return null;
				}
			}
			return varString.setFoldedConstant(soFar);
		}
		
		return varString.setFoldedConstant(varString.str);
	}
	@Override
	public Object visit(VarInt varInt) {
		return varInt.setFoldedConstant(varInt.inter);
	}
	@Override
	public Object visit(VarFloat varFloat) {
		return varFloat.setFoldedConstant(varFloat.floater);
	}
	@Override
	public Object visit(VarDouble varDouble) {
		return varDouble.setFoldedConstant(varDouble.doubler);
	}
	@Override
	public Object visit(RefBoolean refBoolean) {
		return refBoolean.setFoldedConstant(refBoolean.b);
	}
	@Override
	public Object visit(VarLong varLong) {
		return varLong.setFoldedConstant(varLong.longer);
	}
	@Override
	public Object visit(VarChar varChar) {
		return varChar.setFoldedConstant(varChar.chr);
	}

	/*@Override
	public Object visit(VarNull varNull) {
		return null;//no
	}
	 */
	
	//composites of constant expressions...
	
/*	@Override
	public Object visit(IfExpr ifExpr) {
		super.visit(ifExpr);//because we only evnluate the paths relevant here
		
		Object result = null;
		
		Object testResult = ifExpr.test.accept(this);
		if(null != testResult && testResult instanceof Boolean){//TODO: why would it not be boolean - avoid check?
			boolean truth = (Boolean)testResult;
			
			Expression choicer =truth?ifExpr.op1:ifExpr.op2;//go down the right path
			result = choicer.accept(this);
			
			this.raiseError(ifExpr.test.getLine(), ifExpr.test.getColumn(), String.format("if expresssion always resolves to %s, so there is no need to use an if expression", truth));
			
		}
		
		return ifExpr.setFoldedConstant(result);
	}*/

	
	@Override
	public Object visit(AndExpression andExpression) {
		super.visit(andExpression);//no harm
		
		Object header = andExpression.head.accept(this);
		if(null != header && header instanceof Boolean){
			boolean truthsoFar = (Boolean)header;
			
			for(RedirectableExpression expr : andExpression.things){
				Object itemres = expr.accept(this);
				if(null != itemres && itemres instanceof Boolean){
					truthsoFar = truthsoFar && (Boolean)itemres;
				}
				else{
					return null;
				}
			}
			
			return andExpression.setFoldedConstant(truthsoFar);
		}
		return null;
	}

	@Override
	public Object visit(OrExpression orExpression) {
		super.visit(orExpression);//no harm
		
		Object header = orExpression.head.accept(this);
		if(null != header && header instanceof Boolean){
			boolean truthsoFar = (Boolean)header;
			
			for(RedirectableExpression expr : orExpression.things){
				Object itemres = expr.accept(this);
				if(null != itemres && itemres instanceof Boolean){
					truthsoFar = truthsoFar || (Boolean)itemres;
				}
				else{
					return null;
				}
			}
			
			return orExpression.setFoldedConstant(truthsoFar);
		}
		
		
		return null;
	}

	@Override
	public Object visit(NotExpression notExpression) {
		super.visit(notExpression);//no harm
		
		Object header = notExpression.expr.accept(this);
		if(null != header && header instanceof Boolean){
			return notExpression.setFoldedConstant(!(Boolean)header);
		}
		
		return null;
	}

	@Override
	public Object visit(RefClass refClass) {
		super.visit(refClass);//no harm

		Type lhs = refClass.lhsType;
		
		if(lhs instanceof PrimativeType || lhs instanceof NamedType){
			Object value = lhs;
			if(lhs instanceof PrimativeType && !lhs.hasArrayLevels()){
				
				PrimativeType asPrim = (PrimativeType)lhs;
				//Number asNumber = (Number)value;
				
				switch(asPrim.type){
					 case BOOLEAN: value = boolean.class;   break;//nop
					 case INT: value = int.class;   break;
					 case LONG: value = long.class;  break;
					 case FLOAT: value = float.class;   break;
					 case DOUBLE: value = double.class;   break;
					 case SHORT: value = short.class;  break;
					 case BYTE: value = byte.class;   break;
					 case CHAR: value = char.class;   break;
					 default: return null;
				}
			}
			
			return refClass.setFoldedConstant(value);//? dunno
		}
		
		//anything else?
		return null;
	}


	@Override
	public Object visit(PowOperator powOperator) {
		super.visit(powOperator);//no harm
		
		Object lowerThing = powOperator.expr.accept(this);
		if(lowerThing != null && lowerThing instanceof Number){//concurnas we always convert these to double
			double asdouble = ((Number)lowerThing).doubleValue();
			
			Object raiseTo = powOperator.raiseTo.accept(this);
			if(raiseTo != null && raiseTo instanceof Number){
				double res = Math.pow(asdouble, ((Number)raiseTo).doubleValue());
				return powOperator.setFoldedConstant(res);
			}
		}
		return null;
	}
	
	@Override
	public Object visit(EqReExpression equalityExpression) {
		super.visit(equalityExpression);//no harm
		
		Object soFar = equalityExpression.head.accept(this);
		
		//we base the comparison used on the lhs expression type
		
		if(null != soFar){
			if(soFar instanceof Number){
				for(GrandLogicalElement gle : equalityExpression.elements){
					//can only be relational
					Object rhsValue = gle.e2.accept(this);
					if(null != rhsValue && rhsValue instanceof Number){
						Number rhsValueNumber = (Number)rhsValue;
						GrandLogicalOperatorEnum op = gle.compOp;
						
						if(op == GrandLogicalOperatorEnum.LT){ //<
							if(soFar instanceof Double){
								soFar = ((Double)soFar).doubleValue() < rhsValueNumber.doubleValue();
							}
							else if(soFar instanceof Float){
								soFar = ((Float)soFar).floatValue() < rhsValueNumber.floatValue();
							}
							else if(soFar instanceof Long){
								soFar = ((Long)soFar).longValue() < rhsValueNumber.longValue();
							}
							else{//soFar instanceof Integer
								soFar = ((Number)soFar).intValue() < rhsValueNumber.intValue();
							}
						}
						else if(op == GrandLogicalOperatorEnum.GT){//>
							if(soFar instanceof Double){
								soFar = ((Double)soFar).doubleValue() > rhsValueNumber.doubleValue();
							}
							else if(soFar instanceof Float){
								soFar = ((Float)soFar).floatValue() > rhsValueNumber.floatValue();
							}
							else if(soFar instanceof Long){
								soFar = ((Long)soFar).longValue() > rhsValueNumber.longValue();
							}
							else{//soFar instanceof Integer
								soFar = ((Number)soFar).intValue() > rhsValueNumber.intValue();
							}
						}
						else if(op == GrandLogicalOperatorEnum.GTEQ){//>=
							if(soFar instanceof Double){
								soFar = ((Double)soFar).doubleValue() >= rhsValueNumber.doubleValue();
							}
							else if(soFar instanceof Float){
								soFar = ((Float)soFar).floatValue() >= rhsValueNumber.floatValue();
							}
							else if(soFar instanceof Long){
								soFar = ((Long)soFar).longValue() >= rhsValueNumber.longValue();
							}
							else{//soFar instanceof Integer
								soFar = ((Number)soFar).intValue() >= rhsValueNumber.intValue();
							}
						}
						else if(op == GrandLogicalOperatorEnum.LTEQ){//<=
							if(soFar instanceof Double){
								soFar = ((Double)soFar).doubleValue() <= rhsValueNumber.doubleValue();
							}
							else if(soFar instanceof Float){
								soFar = ((Float)soFar).floatValue() <= rhsValueNumber.floatValue();
							}
							else if(soFar instanceof Long){
								soFar = ((Long)soFar).longValue() <= rhsValueNumber.longValue();
							}
							else{//soFar instanceof Integer
								soFar = ((Number)soFar).intValue() <= rhsValueNumber.intValue();
							}
						}
						else if(op == GrandLogicalOperatorEnum.EQ){//==
							if(soFar instanceof Double){
								soFar = ((Double)soFar).doubleValue() == rhsValueNumber.doubleValue();
							}
							else if(soFar instanceof Float){
								soFar = ((Float)soFar).floatValue() == rhsValueNumber.floatValue();
							}
							else if(soFar instanceof Long){
								soFar = ((Long)soFar).longValue() == rhsValueNumber.longValue();
							}
							else{//soFar instanceof Integer
								soFar = ((Number)soFar).intValue() == rhsValueNumber.intValue();
							}
						}
						else if(op == GrandLogicalOperatorEnum.NE){//<>
							if(soFar instanceof Double){
								soFar = ((Double)soFar).doubleValue() != rhsValueNumber.doubleValue();
							}
							else if(soFar instanceof Float){
								soFar = ((Float)soFar).floatValue() != rhsValueNumber.floatValue();
							}
							else if(soFar instanceof Long){
								soFar = ((Long)soFar).longValue() != rhsValueNumber.longValue();
							}
							else{//soFar instanceof Integer
								soFar = ((Number)soFar).intValue() != rhsValueNumber.intValue();
							}
						}
						else if(op == GrandLogicalOperatorEnum.REFEQ){//&==
							return null;//probably should be able to do this: dave == dave?
						}
						else if(op == GrandLogicalOperatorEnum.REFNE){//&<>
							return null;//probably should be able to do this: dave == dave?
						}
						else{//ugly but works consistant with rest of language behavour in these cases
							return null;
						}
					}
					else{
						return null;
					}
				}
				
				return equalityExpression.setFoldedConstant(soFar);
			}
			else if(soFar instanceof Boolean){
				Boolean soFarAsBoolean = (Boolean)soFar;
				for(GrandLogicalElement gle : equalityExpression.elements){
					//can only be comparative if boolean
					Object rhsValue = gle.e2.accept(this);
					if(null != rhsValue && rhsValue instanceof Boolean){
						Boolean rhsValueAsBoolean = (Boolean)rhsValue;
						GrandLogicalOperatorEnum op = gle.compOp;
						if(op == GrandLogicalOperatorEnum.EQ){//==
							soFarAsBoolean = soFarAsBoolean == rhsValueAsBoolean;
						}
						else if(op == GrandLogicalOperatorEnum.NE){//<>
							soFarAsBoolean = soFarAsBoolean != rhsValueAsBoolean;
						}
						else if(op == GrandLogicalOperatorEnum.REFEQ){//&==
							return null;//probably should be able to do this: dave == dave?
						}
						else if(op == GrandLogicalOperatorEnum.REFNE){//&<>
							return null;//probably should be able to do this: dave == dave?
						}
					}
					else{
						return null;
					}
				}

				return equalityExpression.setFoldedConstant(soFarAsBoolean);
			}
		}
		return null;
	}
	
	@Override
	public Object visit(Additive addMinusExpression) {
		super.visit(addMinusExpression);//no harm
		
		boolean resolvesToString = ScopeAndTypeChecker.const_string.equals(addMinusExpression.getTaggedType());
		
		Object headValue = addMinusExpression.head.accept(this);
		
		if(resolvesToString && headValue instanceof Type){
			headValue = "class " + ((Type)headValue).getBytecodeType();
		}
		
		if(null != headValue){
			for(AddMinusExpressionElement ele : addMinusExpression.elements){
				Object rhsValue = ele.exp.accept(this);
				
				if(null!= rhsValue){
					if(headValue instanceof Number){
						if(rhsValue instanceof Number){
							if(ele.isPlus){
								if(headValue instanceof Long){
									headValue = ((Long)headValue).longValue() + ((Number)rhsValue).longValue();
								}
								else if(headValue instanceof Float){
									headValue = ((Float)headValue).floatValue() + ((Number)rhsValue).floatValue();
								}
								else if(headValue instanceof Double){
									headValue = ((Double)headValue).doubleValue() + ((Number)rhsValue).doubleValue();
								}
								else{//int
									headValue = ((Number)headValue).intValue() + ((Number)rhsValue).intValue();
								}
							}
							else{
								if(headValue instanceof Long){
									headValue = ((Long)headValue).longValue() - ((Number)rhsValue).longValue();
								}
								else if(headValue instanceof Float){
									headValue = ((Float)headValue).floatValue() - ((Number)rhsValue).floatValue();
								}
								else if(headValue instanceof Double){
									headValue = ((Double)headValue).doubleValue() - ((Number)rhsValue).doubleValue();
								}
								else{//int
									headValue = ((Number)headValue).intValue() - ((Number)rhsValue).intValue();
								}
							}
						}
						else{//something else, so musy be string concat
							headValue = ""+headValue + rhsValue;
						}
					}
					else if(resolvesToString && rhsValue instanceof Type){
						headValue = ""+headValue + "class " + ((Type)rhsValue).getBytecodeType();
					}
					else{//string
						headValue = ""+headValue + rhsValue;
					}
				}
				else{
					return null;
				}
			}
			
			return addMinusExpression.setFoldedConstant(headValue);
		}
		
		return null;
	}
	
	
	@Override
	public Object visit(PrefixOp prefixOp) {
		super.visit(prefixOp);
		if(prefixOp.prefix == FactorPrefixEnum.NEG || prefixOp.prefix == FactorPrefixEnum.PLUS){
			Object got = prefixOp.p1.accept(this);
			
			if(got != null){
				if(prefixOp.prefix == FactorPrefixEnum.NEG){//plus case is a nop for these instances
					if(got instanceof Number){
						if(got instanceof Long){
							got = -((Long)got).longValue();
						}
						else if(got instanceof Float){
							got = -((Float)got).floatValue();
						}
						else if(got instanceof Double){
							got = -((Double)got).doubleValue();
						}
						else{//int
							got = -((Number)got).intValue();
						}
					}
					else{
						return null;//possible?
					}
				}
				return prefixOp.setFoldedConstant(got);
			}
		}else if(prefixOp.prefix == FactorPrefixEnum.COMP){
			Object got = prefixOp.p1.accept(this);
			
			if(got != null){
				if(got instanceof Number){//JPT: fails for char
					if(got instanceof Long){
						got = ~((Long)got).longValue();
					}else{
						got = ~((Number)got).intValue();
					}
					return prefixOp.setFoldedConstant(got);
				}
			}
		}
		
		return null;
	}
	
	

	@Override
	public Object visit(MulerExpression mulerExpression) {
		super.visit(mulerExpression);//no harm
		//MulerExprEnum
		
		Object headValue = mulerExpression.header.accept(this);
		
		if(null != headValue && headValue instanceof Number){//something other than number?
			
			for(MulerElement ele : mulerExpression.elements){
				Object rhsValue = ele.expr.accept(this);
				if(null != rhsValue && rhsValue instanceof Number){
					MulerExprEnum op = ele.mulOper;//DIV("/", "div"), MOD("%", "mod"), MUL("*", "mul");
					Type toRet = TypeCheckUtils.unboxTypeIfBoxed(ele.getTaggedType());
					
					if(op == MulerExprEnum.DIV){
						if(ScopeAndTypeChecker.const_long.equals(toRet)){
							headValue = ((Number)headValue).longValue() / ((Number)rhsValue).longValue();
						}
						else if(ScopeAndTypeChecker.const_float.equals(toRet)){
							headValue = ((Number)headValue).floatValue() / ((Number)rhsValue).floatValue();
						}
						else if(ScopeAndTypeChecker.const_double.equals(toRet)){
							headValue = ((Number)headValue).doubleValue() / ((Number)rhsValue).doubleValue();
						}
						else{//int
							headValue = ((Number)headValue).intValue() / ((Number)rhsValue).intValue();
						}
					}
					else if(op == MulerExprEnum.MOD){
						if(ScopeAndTypeChecker.const_long.equals(toRet)){
							headValue = ((Number)headValue).longValue() % ((Number)rhsValue).longValue();
						}
						else if(ScopeAndTypeChecker.const_float.equals(toRet)){
							headValue = ((Number)headValue).floatValue() % ((Number)rhsValue).floatValue();
						}
						else if(ScopeAndTypeChecker.const_double.equals(toRet)){
							headValue = ((Number)headValue).doubleValue() % ((Number)rhsValue).doubleValue();
						}
						else{//int
							headValue = ((Number)headValue).intValue() % ((Number)rhsValue).intValue();
						}
					}
					else if(op == MulerExprEnum.MUL){
						if(ScopeAndTypeChecker.const_long.equals(toRet)){
							headValue = ((Number)headValue).longValue() * ((Number)rhsValue).longValue();
						}
						else if(ScopeAndTypeChecker.const_float.equals(toRet)){
							headValue = ((Number)headValue).floatValue() * ((Number)rhsValue).floatValue();
						}
						else if(ScopeAndTypeChecker.const_double.equals(toRet)){
							headValue = ((Number)headValue).doubleValue() * ((Number)rhsValue).doubleValue();
						}
						else{//int
							headValue = ((Number)headValue).intValue() * ((Number)rhsValue).intValue();
						}
					}
					else{
						return null;
					}
				}
				else{
					return null;
				}
			}
			
			return mulerExpression.setFoldedConstant(headValue);
		}
		
		return null;
	}
	
	
	@Override
	public Object visit(ShiftExpression shiftExpression) {
		super.visit(shiftExpression);//no harm
		//MulerExprEnum
		
		Object headValue = shiftExpression.header.accept(this);
		
		if(null != headValue && headValue instanceof Number){//TODO: this doesnt work for char
			for(ShiftElement ele : shiftExpression.elements){
				Object rhsValue = ele.expr.accept(this);
				if(null != rhsValue && rhsValue instanceof Number){
					ShiftOperatorEnum op = ele.shiftOp;
					
					if(headValue instanceof Long){
						if(op == ShiftOperatorEnum.RS){
							headValue = ((Long)headValue).longValue() >> ((Number)rhsValue).intValue();
						}
						else if(op == ShiftOperatorEnum.URS){
							headValue = ((Long)headValue).longValue() >>> ((Number)rhsValue).intValue();
						}else{
							headValue = ((Long)headValue).longValue() << ((Number)rhsValue).intValue();
						}
					}else{
						if(op == ShiftOperatorEnum.RS){
							headValue = ((Number)headValue).intValue() >> ((Number)rhsValue).intValue();
						}
						else if(op == ShiftOperatorEnum.URS){
							headValue = ((Number)headValue).intValue() >>> ((Number)rhsValue).intValue();
						}else{
							headValue = ((Number)headValue).intValue() << ((Number)rhsValue).intValue();
						}
					}
				}
				else{
					return null;
				}
			}
			
			return shiftExpression.setFoldedConstant(headValue);
		}
		
		return null;
	}
	
	
	@Override
	public Object visit(BitwiseOperation bitwiseOperation) {
		super.visit(bitwiseOperation);//no harm
		//MulerExprEnum
		
		Object headValue = bitwiseOperation.head.accept(this);
		BitwiseOperationEnum oper = bitwiseOperation.oper;
		
		if(null != headValue && headValue instanceof Number){//TODO: this doesnt work for char
			for(RedirectableExpression ele : bitwiseOperation.things){
				Object rhsValue = ele.exp.accept(this);
				if(null != rhsValue && rhsValue instanceof Number){
					
					if(headValue instanceof Long){
						if(oper == BitwiseOperationEnum.AND){
							headValue = ((Long)headValue).longValue() & ((Number)rhsValue).intValue();
						}
						else if(oper == BitwiseOperationEnum.OR){
							headValue = ((Long)headValue).longValue() | ((Number)rhsValue).intValue();
						}else{
							headValue = ((Long)headValue).longValue() ^ ((Number)rhsValue).intValue();
						}
					}else{
						if(oper == BitwiseOperationEnum.AND){
							headValue = ((Number)headValue).intValue() & ((Number)rhsValue).intValue();
						}
						else if(oper == BitwiseOperationEnum.OR){
							headValue = ((Number)headValue).intValue() | ((Number)rhsValue).intValue();
						}else{
							headValue = ((Number)headValue).intValue() ^ ((Number)rhsValue).intValue();
						}
					}
				}
				else{
					return null;
				}
			}
			
			return bitwiseOperation.setFoldedConstant(headValue);
		}
		
		return null;
	}
	
	
	@Override
	public Object visit(CastExpression castExpression) {
		super.visit(castExpression);
		
		Object value = castExpression.o.accept(this);
		
		if(null != value){
			Type type = castExpression.t;
			if(type.hasArrayLevels()){
				return null;
			}
			
			if(type instanceof PrimativeType && value instanceof Number){
				PrimativeType asPrim = (PrimativeType)type;
				Number asNumber = (Number)value;
				
				switch(asPrim.type){
					 case BOOLEAN: /*value = (boolean)value;*/   break;//nop
					 case INT: value = asNumber.intValue();   break;
					 case LONG: value = asNumber.longValue();   break;
					 case FLOAT: value = asNumber.floatValue();   break;
					 case DOUBLE: value = asNumber.doubleValue();   break;
					 case SHORT: value = asNumber.shortValue();   break;
					 case BYTE: value = asNumber.byteValue();   break;
					 case CHAR: value = (char)asNumber.intValue();   break;
					 default: return null;
				}
			}
			else if(type instanceof NamedType && type.equals(ScopeAndTypeChecker.const_string)){
				value = ""+value;
			}
			else{
				return null;
			}
			
			return castExpression.setFoldedConstant(value);
		}
		
		return null;
	}
	
	////////////////////
	//checks...

	private boolean isAnnotationArgument = false;
	
	@SuppressWarnings("incomplete-switch")
	private Object checkAnnotationArgument(Expression expr){
		//special thing about these is that they permit array defs, whereas normally this is not accepted as a constant
		boolean previsAnnotationArgument = isAnnotationArgument;
		isAnnotationArgument=true;
		
		try{
			Expression origExpr = expr;
			if(expr instanceof ArrayDefComplex){
				ArrayDefComplex adc = (ArrayDefComplex)expr;
				if(adc.bcarrayElements != null && adc.bcarrayElements.size() == 1){
					Expression first =  adc.bcarrayElements.get(0);
					if(first instanceof ArrayDef){
						expr = first;
					}
				}
			}
			
			if(expr instanceof ArrayDef){
				ArrayDef asArrayDef = (ArrayDef)expr;
				super.visit(asArrayDef);
				
				Type taggedType = asArrayDef.getTaggedType();
				if(taggedType.getArrayLevels() == 1){
					boolean isPrim = taggedType instanceof PrimativeType;
					boolean isClass = TypeCheckUtils.isClass(taggedType);
					boolean isAnnotation = TypeCheckUtils.isAnnotation(taggedType);
					if(isPrim || isClass ||isAnnotation || taggedType.equals(ScopeAndTypeChecker.const_string1dAr) || TypeCheckUtils.isEnum(taggedType)){
						int size = asArrayDef.getArrayElements(this).size();
						if(size>0){
							Class<?> cls = isAnnotation?Annotation.class:isClass?Type.class:String.class;
							if(isPrim){
								switch(((PrimativeType)taggedType).type){
									 case BOOLEAN: cls=boolean.class;   break;
									 case INT:     cls=int.class;   break;
									 case LONG:    cls=long.class;   break;
									 case FLOAT:   cls=float.class;   break;
									 case DOUBLE:  cls=double.class;   break;
									 case SHORT:   cls=short.class;   break;
									 case BYTE:    cls=byte.class;   break;
									 case CHAR:    cls=char.class;   break;
								}
							}
							
							Object ret = Array.newInstance(cls, size);
							
							int i=0;
							for(Expression ele : asArrayDef.getArrayElements(this)){
								Object thisOne = ele.accept(this);
								if(thisOne != null){
									if(isPrim){
										switch(((PrimativeType)taggedType).type){
											 case BOOLEAN: Array.set(ret, i, ((Boolean)thisOne).booleanValue());   break;
											 case LONG:    Array.set(ret, i, ((Number)thisOne).longValue());   break;
											 case FLOAT:   Array.set(ret, i, ((Number)thisOne).floatValue());   break;
											 case DOUBLE:  Array.set(ret, i, ((Number)thisOne).doubleValue());   break;
											 default:     Array.set(ret, i, ((Number)thisOne).intValue());   break;
										}
									}
									else if(isClass || isAnnotation){
										Array.set(ret, i, thisOne);
									}
									else{
										Array.set(ret, i, thisOne.toString());
									}
								}
								else{
									return null;
								}
								i++;
							}
							return origExpr.setFoldedConstant(ret);
						}//no empty array
					}
				}
			}
			else if(expr instanceof ArrayConstructor){
				ArrayConstructor ac = (ArrayConstructor)expr;
				Type taggedType=ac.type;
				boolean isPrim = taggedType instanceof PrimativeType;
				if(isPrim || taggedType.equals(ScopeAndTypeChecker.const_string)){
					Class<?> cls = String.class;
					if(isPrim){
						switch(((PrimativeType)taggedType).type){
							 case BOOLEAN: cls=boolean.class;   break;
							 case INT:     cls=int.class;   break;
							 case LONG:    cls=long.class;   break;
							 case FLOAT:   cls=float.class;   break;
							 case DOUBLE:  cls=double.class;   break;
							 case SHORT:   cls=short.class;   break;
							 case BYTE:    cls=byte.class;   break;
							 case CHAR:    cls=char.class;   break;
						}
					}
					
					return ac.setFoldedConstant(Array.newInstance(cls, 0));
				}
			}
			else{
				return expr.accept(this);
			}
			
			return null;
		}
		finally{
			isAnnotationArgument=previsAnnotationArgument;
		}
	}
	
	
	
	@Override
	public Object visit(RefName refName) {
		super.visit(refName);
		if(isAnnotationArgument){
			Type taggedType = refName.getTaggedType();
			if(TypeCheckUtils.isEnum(taggedType) && refName.isPreceededByDotInDotOperator() && taggedType.equals(prevDop.getTaggedType())){
				//myclass.thing -> whatever, MYEcnum.ONE -> "ONE" 
				return refName.setFoldedConstant(refName.name);
			}
		}else {
			Type taggedType = refName.getTaggedType();
			if(null != taggedType) {
				return refName.setFoldedConstant(((Node)taggedType).getFoldedConstant());
			}
		}
		
		
		return null;
	}
	
	private Expression prevDop; 
	
	@Override
	public Object visit(DotOperator dotOperator) {
		Object got = null;
		prevDop=null;
		for(Expression e: dotOperator.getElements(this)){
			got = e.accept(this);
			prevDop=e;
		}
		
		if(got != null){
			return dotOperator.setFoldedConstant(got);
		}
		return null;
	}
	
	@Override
	public Object visit(Annotation annotation) {
		super.visit(annotation);
		
		if(annotation.singleArg != null){
			Object got = checkAnnotationArgument(annotation.singleArg);//.accept(this);
			
			if(null == got){//doesnt resolved to a constant expression
				PrintSourceVisitor psv = new PrintSourceVisitor();
				annotation.singleArg.accept(psv);
				
				this.raiseError(annotation.singleArg.getLine(), annotation.singleArg.getColumn(), String.format("Argument passed to annotation must resolve to constant at compilation time. This does not: %s", psv));
			}
			
		}
		else if(annotation.manyArgs != null && !annotation.manyArgs.isEmpty()){
			
			for(Pair<String, Expression> kv : annotation.manyArgs){
				String key = kv.getA();
				Expression v = kv.getB();
				Object value = checkAnnotationArgument(v);
				
				if(value == null){
					PrintSourceVisitor psv = new PrintSourceVisitor();
					v.accept(psv);
					
					this.raiseError(annotation.getLine(), annotation.getColumn(), String.format("Argument passed to annotation must resolve to constant at compilation time. This does not: %s = %s", key, psv));
				}
			}
		}
		
		return annotation.setFoldedConstant(annotation);
	}

	//////////
	//cool stuff...

	@Override
	public Object visit(AssignNew assignNew) {
		super.visit(assignNew);
		//only applies to val declarations which are all classed as assignNew (so we can ignore assignExisting)
		if(null != assignNew.expr){
			Object got = null;
			if(assignNew.isAnnotationField) {//special check for if annotation field, scope is wider
				got = checkAnnotationArgument(assignNew.expr);
			}
			else if(assignNew.isFinal) {//private val cons =99
				got = assignNew.expr.accept(this);
			}
			
			if(got != null){//splice in the constant to the type (nasty hack lol)
				Node tt = ((Node)assignNew.getTaggedType());
				if(null != tt){
					tt.setFoldedConstant(got);
				}
			}
			
			if(assignNew.isAnnotationField){
				if( got ==null){
					//if these have an expression it must resolve to something
					PrintSourceVisitor psv = new PrintSourceVisitor();
					assignNew.expr.accept(psv);
					this.raiseError(assignNew.getLine(), assignNew.getColumn(), String.format("Annotation field defualt value must resolve to constant at compilation time. This does not: %s = %s", assignNew.name, psv));
				}
			}
		}
		
		if(assignNew.isAnnotationField){
			//JPT: checks dont really belong here but easier than defining them in satc
			checkTypeValidForAnnotationField(assignNew.getLine(), assignNew.getColumn(), assignNew.name, assignNew.getTaggedType());//hmmm, can this ever trigger?
			
			if(assignNew.accessModifier != null){
				this.raiseError(assignNew.getLine(), assignNew.getColumn(), String.format("Annotation field %s cannot have an access modifier", assignNew.name));
			}
			
			if(assignNew.isFinal){
				this.raiseError(assignNew.getLine(), assignNew.getColumn(), String.format("Annotation field %s cannot be declared val", assignNew.name));
			}
			
			if(assignNew.prefix != null){
				this.raiseError(assignNew.getLine(), assignNew.getColumn(), String.format("Annotation field %s cannot be declared with a prefix", assignNew.name));
			}
			
			/*if(assignNew.eq != AssignStyleEnum.EQUALS){//how would this be possible?
				this.raiseError(assignNew.getLine(), assignNew.getColumn(), String.format("Annotation field %s default value cannot be assigned with anything but =", assignNew.name));
			}*/
		}
		
		return null;
	}
	
	private void checkTypeValidForAnnotationField(int line, int col, String anem, Type theType){//no refs etc
		if(!checkTypeValidForAnnotationField(theType)){
			this.raiseError(line, col, String.format("Invalid type %s for the annotation attribute %s; only primitive type, String, Class, annotation, enumeration are permitted or 1-dimensional arrays thereof", theType, anem));
		}
	}
	
	private boolean checkTypeValidForAnnotationField(Type theType){//no refs etc
		if(theType.getArrayLevels() > 1){
			return false;//no has to be 0 or 1 level
		}
		
		//any primative
		if(theType instanceof PrimativeType){
			PrimativeType asPrim = (PrimativeType)theType;
			if(asPrim.type == PrimativeTypeEnum.VOID || asPrim.type == PrimativeTypeEnum.LAMBDA){
				return false;//except these!
			}
			return true;
		}
		//stirng or class
		if(theType instanceof NamedType){
			NamedType asNamed = (NamedType)theType;
			ClassDef setcls = asNamed.getSetClassDef();//maybenull
			if(ScopeAndTypeChecker.const_cls_String.equals(setcls) || ScopeAndTypeChecker.const_class.equals(setcls)){
				return true;
			}
			
		/*	asNamed.setArrayLevels(0);//cheat, but acting on copy anyway
			
			if(TypeCheckUtils.isBoxedType(asNamed)){
				return true;
			}*/
			
		}
		
		//enum or another annotation
		if(TypeCheckUtils.isEnum(theType) || TypeCheckUtils.isAnnotation(theType) ){
			return true;
		}
		
		return false;
	}
	
	@Override
	public Object visit(AssignExisting assignExisting) {
		super.visit(assignExisting);
		//only applies to val declarations which are all classed as assignNew (so we can ignore assignExisting)
		if(null != assignExisting.expr && /*null != assignExisting.asignment && */assignExisting.assignee instanceof RefName){//if not ref name will be caught already at satc time
			Object got = assignExisting.isAnnotationField?checkAnnotationArgument(assignExisting.expr):assignExisting.expr.accept(this);//special check for if annotation field, scope is wider
			
			if(assignExisting.isAnnotationField ){
				String name = ((RefName)assignExisting.assignee).name;
				if(got ==null){
					//if these have an expression it must resolve to something
					PrintSourceVisitor psv = new PrintSourceVisitor();
					assignExisting.expr.accept(psv);
					this.raiseError(assignExisting.getLine(), assignExisting.getColumn(), String.format("Annotation field default value must resolve to constant at compilation time. This does not: %s = %s", name, psv));
				}else{
					checkTypeValidForAnnotationField(assignExisting.getLine(), assignExisting.getColumn(), name, assignExisting.getTaggedType());
					//rest of checks are quite easy and done already 
					((Node)assignExisting.getTaggedType()).setFoldedConstant(got);//splice in the constant to the type (nasty hack lol)
				}
			}
		}
		else if(assignExisting.isAnnotationField ){//TODO: i think the below can never be triggered....
			PrintSourceVisitor wtf = new PrintSourceVisitor();
			assignExisting.assignee.accept(wtf);
			this.raiseError(assignExisting.getLine(), assignExisting.getColumn(), String.format("Invalid field name for default field for annotation: %s", wtf));
		}
		
		return null;
	}
	
	@Override
	public Object visit(CaseExpressionAssign caseExpressionUntypedAssign) {
		if(null != caseExpressionUntypedAssign.expr){
			caseExpressionUntypedAssign.expr.accept(this);
		}
		
		return null;
	}
	
	//////Check for always true/false on control statments [patterns are done in scopeandtypechecker]
	
	private Boolean fcResovlesTrue(Object xxx){
		if(xxx != null && xxx instanceof Boolean){
			return (Boolean)xxx;
		}
		return null;
	}
	
	@Override
	public Object visit(IfStatement ifStatement){
		Object ret = super.visit(ifStatement);
		
		Boolean resTrue = fcResovlesTrue(ifStatement.iftest.getFoldedConstant());
		if(null != resTrue){
			if(ifStatement.elseb !=null || !ifStatement.elifunits.isEmpty()){
				if(ifStatement.canBeConvertedIntoIfExpr) {
					this.raiseError(ifStatement.iftest.getLine(), ifStatement.iftest.getColumn(), String.format("if expresssion always resolves to %s, so there is no need to use an if expression", resTrue));
				}else {
					if(resTrue){
						this.raiseError(ifStatement.iftest.getLine(), ifStatement.iftest.getColumn(), "if test always resolves to true - as such elseif or else blocks will never be executed");
					} else{
						this.raiseError(ifStatement.iftest.getLine(), ifStatement.iftest.getColumn(), "if test always resolves to false - as such code in if block will never be executed");
					}
				}
				
			}else{
				this.raiseError(ifStatement.iftest.getLine(), ifStatement.iftest.getColumn(), String.format("if test always resolves to %s, so there is no need to use an if statement" , resTrue));
			}
		}
		
		if(!ifStatement.elifunits.isEmpty()){
			int ss = ifStatement.elifunits.size();
			for(int n=0; n < ss; n++){
				boolean islast = ss-1 == n;
				Expression unitTest = ifStatement.elifunits.get(n).eliftest;
				
				resTrue = fcResovlesTrue(unitTest.getFoldedConstant());
				if(null != resTrue){
					if(!islast || ifStatement.elseb != null){//if there are more blocks
						if(resTrue){
							this.raiseError(unitTest.getLine(), unitTest.getColumn(), "elif test always resolves to true - as such later elseif or else blocks will never be executed");
						}else{
							this.raiseError(unitTest.getLine(), unitTest.getColumn(), "elif test always resolves to false - as such code in elif block will never be executed");
						}
					}
					else{
						if(resTrue){
							this.raiseError(unitTest.getLine(), unitTest.getColumn(), "elif test always resolves to true, use an else block instead");
						} else{
							this.raiseError(unitTest.getLine(), unitTest.getColumn(), "elif test always resolves to false, so there is no need to use an elif statement");
						}
					}
				}
				
			}
		}
		
		return ret;
	}
}
