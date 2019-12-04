package com.concurnas.compiler.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Stack;

import org.antlr.v4.misc.OrderedHashMap;

import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.ast.AddMinusExpressionElement;
import com.concurnas.compiler.ast.Additive;
import com.concurnas.compiler.ast.AndExpression;
import com.concurnas.compiler.ast.ArrayConstructor;
import com.concurnas.compiler.ast.ArrayRef;
import com.concurnas.compiler.ast.ArrayRefElement;
import com.concurnas.compiler.ast.ArrayRefElementPostfixAll;
import com.concurnas.compiler.ast.ArrayRefElementPrefixAll;
import com.concurnas.compiler.ast.ArrayRefElementSubList;
import com.concurnas.compiler.ast.ArrayRefLevelElementsHolder;
import com.concurnas.compiler.ast.Assign;
import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.ast.AssignStyleEnum;
import com.concurnas.compiler.ast.BitwiseOperation;
import com.concurnas.compiler.ast.BitwiseOperationEnum;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.BooleanAndOrExpression;
import com.concurnas.compiler.ast.CanBeInternallyVectorized;
import com.concurnas.compiler.ast.CastExpression;
import com.concurnas.compiler.ast.DotOperator;
import com.concurnas.compiler.ast.DuffAssign;
import com.concurnas.compiler.ast.ElvisOperator;
import com.concurnas.compiler.ast.EqReExpression;
import com.concurnas.compiler.ast.ExpressionList;
import com.concurnas.compiler.ast.FactorPostFixEnum;
import com.concurnas.compiler.ast.ForBlockOld;
import com.concurnas.compiler.ast.FuncInvoke;
import com.concurnas.compiler.ast.FuncInvokeArgs;
import com.concurnas.compiler.ast.FuncRef;
import com.concurnas.compiler.ast.FuncRefArgs;
import com.concurnas.compiler.ast.FuncRefInvoke;
import com.concurnas.compiler.ast.GrandLogicalElement;
import com.concurnas.compiler.ast.GrandLogicalOperatorEnum;
import com.concurnas.compiler.ast.InExpression;
import com.concurnas.compiler.ast.Is;
import com.concurnas.compiler.ast.ModuleType;
import com.concurnas.compiler.ast.MulerElement;
import com.concurnas.compiler.ast.MulerExprEnum;
import com.concurnas.compiler.ast.MulerExpression;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.New;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.NotExpression;
import com.concurnas.compiler.ast.NotNullAssertion;
import com.concurnas.compiler.ast.NullStatus;
import com.concurnas.compiler.ast.OrExpression;
import com.concurnas.compiler.ast.PostfixOp;
import com.concurnas.compiler.ast.PowOperator;
import com.concurnas.compiler.ast.PrefixOp;
import com.concurnas.compiler.ast.RedirectableExpression;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.RefSuper;
import com.concurnas.compiler.ast.RefThis;
import com.concurnas.compiler.ast.ShiftElement;
import com.concurnas.compiler.ast.ShiftExpression;
import com.concurnas.compiler.ast.ShiftOperatorEnum;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.VarInt;
import com.concurnas.compiler.ast.VarNull;
import com.concurnas.compiler.ast.Vectorized;
import com.concurnas.compiler.ast.VectorizedArrayRef;
import com.concurnas.compiler.ast.VectorizedFieldRef;
import com.concurnas.compiler.ast.VectorizedFuncInvoke;
import com.concurnas.compiler.ast.VectorizedFuncRef;
import com.concurnas.compiler.ast.VectorizedNew;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.visitors.util.ErrorRaiseableSupressErrors;
import com.concurnas.runtime.Pair;

public class VectorizedRedirector extends AbstractErrorRaiseVisitor {

	public VectorizedRedirector(String fullPathFileName) {
		super(fullPathFileName);
	}

	private boolean hadMadeRepoints = false;
	
	public boolean hadMadeRepoints(){
		return this.hadMadeRepoints;
	}
	
	private int vectorizedRedirectUID = -1;
	
	public void restart(){
		 vectorizedRedirectUID = -1;
		 hadMadeRepoints=false;
	}

	private static Expression extractExprFromDot(Expression arg) {
		if(arg instanceof VectorizedArrayRef) {
			ArrayRef ar = ((VectorizedArrayRef)arg).astOverridearrayRef;
			if(ar != null) {
				return extractExprFromDot(ar); 
			}
				
		}
		
		
		if(arg instanceof ExpressionList) {
			ExpressionList exprList = (ExpressionList)arg;
			if(exprList.astRedirect instanceof Expression) {
				arg = (Expression)exprList.astRedirect;
			}
		}
		
		if(arg instanceof DotOperator) {
			DotOperator asDot = (DotOperator)arg;
			ArrayList<Expression> elems = asDot.getElements(null);
			if(elems.size() == 1) {
				arg = elems.get(0);
			}
		}
		
		return arg;
	}
	
	private static boolean exprIsVectorizedFieldRef(Expression expr) {
		expr = extractExprFromDot(expr);
		return expr instanceof VectorizedFieldRef;
	}
	
	@Override
	public Object visit(Vectorized vectorized) {
		super.visit(vectorized);
		
		if(!vectorized.validAtThisLocation){
			this.raiseError(vectorized.getLine(), vectorized.getColumn(), "Expression cannot be vectorized at this location");
		}
		
		return null;
	}

	private static class ExpressionFinder{
		//myfunc(anotherfunc(aa^, b)^)
		//private int argCnt = 0;
		
		public ArrayList<Node> allArgs = new ArrayList<Node>();
		public HashSet<Integer> ignoreTmpArg = new HashSet<Integer>();
		public ArrayList<Integer> vectorizedArgs = new ArrayList<Integer> ();
		public ArrayList<Integer> makeTempVarEvenThoughVectorized = new ArrayList<Integer> ();
		public HashMap<Integer, Integer> dupVarsMap;
		public int vectorizedArgsSELF=-1;

		private VectorizedRedirector redir;
		private final  int col;
		private final int line;
		private final boolean isAssignExisting;
		private ArrayList<Integer> vectLevels = new ArrayList<Integer>();
		
		public ExpressionFinder(int line, int col, VectorizedRedirector redir, boolean isAssignExisting){
			this.redir = redir;
			this.line = line;
			this.col = col;
			this.isAssignExisting = isAssignExisting;
		}
		
		private boolean canBeVectorizedSelf(Expression item){//JPT: too simple?
			if(item instanceof FuncInvoke){
				return ((FuncInvoke)item).vectroizedDegreeAndArgs == null;
			}
			
			return true;
		}
		
		private boolean processExpressionElement(Object arg) {
			return processExpressionElement(arg, false);
		}
		
		private boolean processExpressionElement(Object arg, boolean isLHSAssignment) {
			boolean pass=true;
			
			if(arg instanceof Expression) {
				arg = extractExprFromDot((Expression)arg);
			}
			
			boolean isFieldRef = arg instanceof VectorizedFieldRef;
			if(isFieldRef) {
				((VectorizedFieldRef)arg).validAtThisLocation = true;
			}
			
			if(arg instanceof Vectorized){
				Vectorized asVec = (Vectorized)arg;
				asVec.validAtThisLocation=true;
				arg = ((Vectorized)arg).expr;
				
				arg = extractExprFromDot((Expression)arg);
				
				int vectl = TypeCheckUtils.getVectorizedStructure(errorRaisableSupression, ((Node)arg).getTaggedType()).size();
				if(!isArrayRefOperation.isEmpty() && isArrayRefOperation.peek()) {
					vectl--;
				}
				vectLevels.add(vectl);
				
				allArgs.add((Node)arg);
				
				int argToAdd = allArgs.size() - 1;
				
				if (asVec.doubledot) {
					if (!canBeVectorizedSelf((Expression)arg)) {
						redir.raiseError(line, col, "Expression element may not have vectorized call of '^^' form - only '^' can be used");
						pass = false;
					} else if (vectorizedArgsSELF != -1) {
						throwOneArgSelfOny();
						pass = false;
					}
					vectorizedArgsSELF = argToAdd;
				}
				vectorizedArgs.add(argToAdd);
				
				int vectSizeAdd = vectorizedArgs.size();
				if(!findExpressions((Expression)arg)){
					pass = false;//failed
				}else if(vectSizeAdd == vectorizedArgs.size()){//no further additions, then this arg doesnt contain anything vectorized within in
					//but we want it allocated to a temp var still 
					makeTempVarEvenThoughVectorized.add(argToAdd);
				}
			}else if(isFieldRef && isLHSAssignment) {//A^field = 99
				findExpressions((VectorizedFieldRef)arg);
			}
			else if(arg == null) {
				pass = false;
			}
			else {
				((Node)arg).accept(redir);//e.g. if function ensure non related items are fully evaluated
				ignoreTmpArg.add(allArgs.size());
				allArgs.add((Node)arg);
				if(arg instanceof Expression) {
					findExpressions((Expression)arg);
				}
			}
			return pass;
		}
		
		private void throwOneArgSelfOny() {
			String msg;
			if(isAssignExisting) {
				msg = "Vectorized calls in assignment may only be of '^' form";
			}else {
				msg  = "Only one argument in nested vectorized call may be of '^^' form";
			}
			redir.raiseError(line, col, msg);
		}
		
		private boolean processFuncVect(Expression expr, boolean doubledot, Expression funcInvokeOrRef) {
			boolean pass=true;
			
			if(expr instanceof VectorizedFuncInvoke || expr instanceof VectorizedFuncRef || expr instanceof VectorizedNew){
				if(!findExpressions(expr)){
					pass = false;//failed
				}
				vectorizedArgs.add(allArgs.size()-1);
			}else if(expr instanceof CanBeInternallyVectorized) {
				if(!findExpressions(expr)){
					pass = false;//failed
				}
			}else{
				allArgs.add((Node)expr);
				
				int vectl = TypeCheckUtils.getVectorizedStructure(errorRaisableSupression, ((Node)expr).getTaggedType()).size();
				vectLevels.add(vectl);
				
				vectorizedArgs.add(allArgs.size()-1);
				if(!findExpressions(expr)){
					pass = false;//failed
				}
			}
			
			if (doubledot) {
				if (!canBeVectorizedSelf(expr)) {
					redir.raiseError(line, col, "Vectorized function invocation may not have vectorized call of '^^' form - only '^' can be used");
					pass = false;
				} else if (vectorizedArgsSELF != -1) {
					throwOneArgSelfOny();
					pass = false;
				}
				vectorizedArgsSELF = allArgs.size() - 1;
			}
			
			if(!findExpressions(funcInvokeOrRef)){
				pass = false;//failed
			}
			
			return pass;
		}
		
		public boolean findExpressions(AssignExisting asaddOp, boolean rhsVarsDupeLhs){
			boolean pass = true;
			if(rhsVarsDupeLhs) {
				int dupVarsfrom = this.allArgs.size();
				pass = pass && processExpressionElement(asaddOp.assignee, true);
				int dupVarsto = this.allArgs.size();
				int rhsVarsStart = dupVarsto;
				pass = pass && findExpressions(asaddOp.expr);
				//map rhs vars to lhs
				dupVarsMap = new HashMap<Integer, Integer>();
				while(dupVarsfrom < dupVarsto) {
					dupVarsMap.put(rhsVarsStart, dupVarsfrom);
					dupVarsfrom++;
					rhsVarsStart++;
				}
				
			}else {
				pass = pass && processExpressionElement(asaddOp.assignee, true);
				pass = pass && findExpressions(asaddOp.expr);
			}
			
			return pass;
		}
		
		private Stack<Boolean> isArrayRefOperation = new Stack<Boolean>();
		
		public boolean findExpressions(Expression expr){
			//find all arguments, mark those needing vectorization, mark those mark those needing self reference
			boolean pass = true;
			isArrayRefOperation.push(false);
			
			FuncInvokeArgs fia = null;
			if(expr instanceof New){
				fia = ((New)expr).args;
			}else if(expr instanceof FuncInvoke){
				fia = ((FuncInvoke)expr).args;
			}else if(expr instanceof FuncRefInvoke) {
				FuncRefInvoke fri = (FuncRefInvoke)expr;
				
				pass = findExpressions(fri.funcRef);
				
				fia = fri.getArgs();
			}
			
			if(fia != null){
				List<Expression> args = fia.getArgumentsWNPs();
				for(Expression arg : args){
					pass = pass && processExpressionElement(arg);
				}
			}else if(expr instanceof FuncRef) {//nasty duplication of logic above
				FuncRef asfuncref = (FuncRef)expr;
				
				FuncRefArgs argsz = asfuncref.argsForNextCompCycle;
				for(Object arg : argsz.exprOrTypeArgsList){
					pass = pass && processExpressionElement(arg);
				}
				
				for(Pair<String, Object> arg : argsz.getNameMap()) {
					pass = pass && processExpressionElement(arg.getB());
				}
				
			}else if(expr instanceof VectorizedFuncInvoke){
				VectorizedFuncInvoke asvfi = (VectorizedFuncInvoke)expr;
				pass = pass && processFuncVect(asvfi.expr, asvfi.doubledot, asvfi.funcInvoke);
			}else if(expr instanceof VectorizedFuncRef){
				VectorizedFuncRef asvfi = (VectorizedFuncRef)expr;
				pass = pass && processFuncVect(asvfi.expr, asvfi.doubledot, asvfi.funcRef);
			}else if(expr instanceof VectorizedNew){
				VectorizedNew asvfi = (VectorizedNew)expr;
				pass = pass && processFuncVect(asvfi.lhs, asvfi.doubledot, asvfi.constru);
			}else if(expr instanceof VectorizedFieldRef){
				VectorizedFieldRef asvfi = (VectorizedFieldRef)expr;
				asvfi.validAtThisLocation = true;
				pass = pass && processFuncVect(asvfi.expr, asvfi.doubledot, asvfi.name);
			}else if(expr instanceof CastExpression){
				CastExpression ascast = (CastExpression)expr;
				Expression arg = ascast.o;
				if(arg instanceof Vectorized){
					Vectorized asVec = (Vectorized)arg;
					asVec.validAtThisLocation=true;
					arg = asVec.expr;
					allArgs.add((Node)arg);
					
					vectorizedArgs.add(allArgs.size()-1);
					
					if(!findExpressions(arg)){
						pass = false;//failed
					}
				}
			}else if(expr instanceof DotOperator){
				DotOperator asDot = (DotOperator)expr;
				ArrayList<Expression> ele = asDot.getElements(null);
				if(!findExpressions(ele.get(ele.size()-1))){
					pass = false;//failed
				}
			}else if(expr instanceof ArrayRef) {
				ArrayRef ar = (ArrayRef)expr;
				if(ar.vectArgumentDepth != null) {
					boolean prev = isArrayRefOperation.pop();
					isArrayRefOperation.push(true);
					pass = pass && processExpressionElement(ar.expr);
					isArrayRefOperation.pop();
					isArrayRefOperation.push(prev);
				}else {
					pass = pass && findExpressions(ar.expr);
				}
				for(ArrayRefElement are : ar.getFlatALE()) {
					pass = pass && processExpressionElement(are.e1);
					if(are instanceof ArrayRefElementSubList) {
						pass = pass && processExpressionElement(((ArrayRefElementSubList)are).e2);
					}
				}
			}else if(expr instanceof Additive) {
				Additive asaddOp = (Additive)expr;
				pass = pass && processExpressionElement(asaddOp.head);
				
				for(AddMinusExpressionElement el : asaddOp.elements) {
					pass = pass && processExpressionElement(el.exp);
				}
			}else if(expr instanceof MulerExpression) {
				MulerExpression asaddOp = (MulerExpression)expr;
				pass = pass && processExpressionElement(asaddOp.header);
				
				for(MulerElement el : asaddOp.elements) {
					pass = pass && processExpressionElement(el.expr);
				}
			}else if(expr instanceof PowOperator) {
				PowOperator asaddOp = (PowOperator)expr;
				pass = pass && processExpressionElement(asaddOp.expr);
				pass = pass && processExpressionElement(asaddOp.raiseTo);
			}else if(expr instanceof InExpression) {
				InExpression asaddOp = (InExpression)expr;
				pass = pass && processExpressionElement(asaddOp.thing);
				pass = pass && processExpressionElement(asaddOp.insideof);
			}else if(expr instanceof EqReExpression) {
				EqReExpression asaddOp = (EqReExpression)expr;
				pass = pass && processExpressionElement(asaddOp.head);
				
				for(GrandLogicalElement el : asaddOp.elements) {
					pass = pass && processExpressionElement(el.e2);
				}
			}else if(expr instanceof ShiftExpression) {
				ShiftExpression asaddOp = (ShiftExpression)expr;
				pass = pass && processExpressionElement(asaddOp.header);
				
				for(ShiftElement el : asaddOp.elements) {
					pass = pass && processExpressionElement(el.expr);
				}
			}else if(expr instanceof NotExpression) {
				NotExpression asaddOp = (NotExpression)expr;
				pass = pass && processExpressionElement(asaddOp.expr);
			}else if(expr instanceof ElvisOperator) {
				ElvisOperator asaddOp = (ElvisOperator)expr;
				pass = pass && processExpressionElement(asaddOp.lhsExpression);
				pass = pass && processExpressionElement(asaddOp.rhsExpression);
			}else if(expr instanceof NotNullAssertion) {
				NotNullAssertion asaddOp = (NotNullAssertion)expr;
				pass = pass && processExpressionElement(asaddOp.expr);
			}else if(expr instanceof Is) {
				Is asaddOp = (Is)expr;
				pass = pass && processExpressionElement(asaddOp.e1);
			}else if(expr instanceof BooleanAndOrExpression) {
				BooleanAndOrExpression asaddOp = (BooleanAndOrExpression)expr;
				pass = pass && processExpressionElement(asaddOp.head);
				
				for(RedirectableExpression el : asaddOp.things) {
					pass = pass && processExpressionElement(el.exp);
				}
			}else if(expr instanceof BitwiseOperation) {
				BitwiseOperation asaddOp = (BitwiseOperation)expr;
				pass = pass && processExpressionElement(asaddOp.head);
				
				for(RedirectableExpression el : asaddOp.things) {
					pass = pass && processExpressionElement(el.exp);
				}
			}else if(expr instanceof PrefixOp) {
				PrefixOp asaddOp = (PrefixOp)expr;
				pass = pass && processExpressionElement(asaddOp.p1);
			}else if(expr instanceof PostfixOp) {
				PostfixOp asaddOp = (PostfixOp)expr;
				pass = pass && processExpressionElement(asaddOp.p2);
			}else if(expr instanceof Vectorized) {
				pass = pass && processExpressionElement(expr);
			}
			
			isArrayRefOperation.pop();
			
			return pass;
		}
	}
	
	
	private static class ConvertToArrayIndexForm{
		//create an expression list contingent on mapped epxressions if any
		//M^ + 1 => M[temp1][temp2] + 1 //etc
		private ExpressionFinder finder;
		private int line;
		private int col;
		private int x=0;
		private HashMap<Integer, Pair<String, Type>> tempArgs;
		private HashSet<Integer> toignore;
		
		public ConvertToArrayIndexForm(int line, int col, ExpressionFinder finder, HashMap<Integer, Pair<String, Type>> tempArgs, HashSet<Integer> toignore){
			this.line = line;
			this.col = col;
			this.finder = finder;
			this.tempArgs = tempArgs;
			this.toignore = toignore;
		}
		
		private void processElement(ArrayList<Object> ret, Expression arg, ArrayList<String> arrayArgs) {
			ret.add(processElement(arg, arrayArgs));
		}
		
		private Object processElement(Expression arg, ArrayList<String> arrayArgs) {
			Expression expr;
			//boolean needsVectorize = this.finder.vectorizedArgs.contains(x);
			
			arg = extractExprFromDot(arg);
			
			if(tempArgs.containsKey(x) && !this.toignore.contains(x)){
				expr = new RefName(line, col, tempArgs.get(x).getA());
				if(this.finder.vectorizedArgs.contains(x)){
					
					if(arg instanceof Vectorized && exprIsVectorizedFieldRef(((Vectorized)arg).expr) ) {
						expr = vectorize(arg, arrayArgs, null);
					}else {
						ArrayRefLevelElementsHolder arrayLevelElements = new ArrayRefLevelElementsHolder();
						ArrayList<ArrayRefElement> item = new ArrayList<ArrayRefElement>();
						for(String aa : arrayArgs){
							item.add(new ArrayRefElement(line, col, new RefName(line, col, aa)));
						}
						arrayLevelElements.add(false, item);
						expr = new ArrayRef(line, col, expr, arrayLevelElements);
						
						vectorize(arg, arrayArgs, null);//incrrements x correctly
					}
				}
				
				x++;
			}else{
				x++;
				expr = vectorize(arg, arrayArgs, null);
			}
			return expr;
		}
		
		
		private FuncInvokeArgs makeFuncInvokeArgs(FuncInvokeArgs input, ArrayList<String> arrayArgs){
			FuncInvokeArgs ret = new FuncInvokeArgs(line, col); 
			
			ArrayList<Object> retar = new ArrayList<Object>();
						
			for(Expression arg : input.getArgumentsWNPs()){
				processElement(retar, arg, arrayArgs);
			}
			
			for(Object obj : retar) {
				ret.add((Expression)obj);
			}
			
			
			return ret;
		}
		
		private Expression processFuncVect(ArrayList<String> arrayArgs, Expression expr, Expression funcRefOrInvoke, boolean nullsafe) {
			//VectorizedFuncInvoke asvfi = (VectorizedFuncInvoke)exprx;
			
			expr = extractExprFromDot(expr);
			
			Expression exprx;
			ArrayRefLevelElementsHolder arrayLevelElements = new ArrayRefLevelElementsHolder();
			ArrayList<ArrayRefElement> item = new ArrayList<ArrayRefElement>();
			for(String aa : arrayArgs){
				item.add(new ArrayRefElement(line, col, new RefName(line, col, aa)));
			}
			
			arrayLevelElements.add(false, item);
			
			Expression lhs;
			if(expr instanceof VectorizedFuncInvoke || expr instanceof VectorizedFuncRef || expr instanceof VectorizedNew || expr instanceof VectorizedFieldRef || expr instanceof CanBeInternallyVectorized){
				lhs = vectorize(expr, arrayArgs, null);
				x++;
				exprx = DotOperator.buildDotOperatorOneNonDirectNullSafe(line, col, lhs, vectorize(funcRefOrInvoke, arrayArgs, null), nullsafe);
			}else{
				if(tempArgs.containsKey(x) && !this.toignore.contains(x) ){
					lhs = new ArrayRef(line, col, new RefName(line, col, tempArgs.get(x).getA()), arrayLevelElements);
				}else{
					x++;
					lhs = vectorize(expr, arrayArgs, null);
				}
				x++;
				exprx = DotOperator.buildDotOperatorOneNonDirectNullSafe(line, col, lhs, vectorize(funcRefOrInvoke, arrayArgs, null), nullsafe);
			}
			return exprx;
		}
		
		public Expression vectorize(AssignExisting asadd, ArrayList<String> arrayArgs, Thruple<ArrayList<Expression>, Boolean, Boolean> preamble){
			//processElement(Expression arg, ArrayList<String> arrayArgs)
			Expression assignee = asadd.assignee;
			
			assignee = extractExprFromDot(assignee);
			
			if(assignee instanceof VectorizedFieldRef) {//A^field = 99
				assignee = vectorize(assignee, arrayArgs, preamble);
			}else {
				assignee = (Expression)processElement( assignee, arrayArgs);
			}
			
			Expression expr;
			if(asadd.expr instanceof Vectorized) {
				expr = (Expression)processElement( asadd.expr, arrayArgs);
			}else {
				expr = (Expression)vectorize( asadd.expr, arrayArgs, preamble);
			}
			
			Block blk = new Block(line, col);
			blk.add(new AssignExisting(asadd.getLine(), asadd.getColumn(), assignee, AssignStyleEnum.EQUALS_STRICT, expr));
			
			Expression exprx = blk;
			
			if(preamble != null){
				exprx = DotOperator.buildDotOperatorPrefixing(line, col, preamble.getA(), preamble.getB(), preamble.getC(), exprx);
			}
			
			return exprx;
		}
		
		public Expression vectorize(Expression exprx, ArrayList<String> arrayArgs, Thruple<ArrayList<Expression>, Boolean, Boolean> preamble){
			exprx = extractExprFromDot(exprx);
			
			if(exprx instanceof Vectorized){
				
				Expression tmpExp = ((Vectorized)exprx).expr;
				//tmpExp = extractExprFromDot(tmpExp);
				
				exprx = vectorize(tmpExp, arrayArgs,null);
			}
			else if(exprx instanceof FuncInvoke){
				FuncInvoke funcInvoke = (FuncInvoke)exprx;
				FuncInvoke funcInvokeCop = (FuncInvoke)funcInvoke.copy();
				funcInvokeCop.vectroizedDegreeAndArgs=null;
				
				if(funcInvokeCop.astRedirect instanceof New) {
					((New)funcInvokeCop.astRedirect).vectroizedDegreeAndArgs=null;//yuck!
				}
				
				funcInvokeCop.args = makeFuncInvokeArgs(funcInvoke.args, arrayArgs);
				if(funcInvokeCop.astRedirect instanceof New){//hmm, bit of a hack, where else does this need to be done?
					((New)funcInvokeCop.astRedirect).args = funcInvokeCop.args;
				}
				
				exprx = funcInvokeCop;
			}else if(exprx instanceof FuncRefInvoke){
				FuncRefInvoke funcInvoke = (FuncRefInvoke)exprx;
				FuncRefInvoke funcInvokeCop = (FuncRefInvoke)funcInvoke.copy();
				
				funcInvokeCop.args = makeFuncInvokeArgs(funcInvoke.args, arrayArgs);
				/*if(funcInvokeCop.astRedirect instanceof New){//hmm, bit of a hack, where else does this need to be done?
					((New)funcInvokeCop.astRedirect).args = funcInvokeCop.args;
				}*/
				
				exprx = funcInvokeCop;
			}else if(exprx instanceof New){
				New funcInvoke = (New)exprx;
				New funcInvokeCop = (New)funcInvoke.copy();
				funcInvokeCop.vectroizedDegreeAndArgs = null;
				
				funcInvokeCop.args = makeFuncInvokeArgs(funcInvoke.args, arrayArgs);
				
				exprx = funcInvokeCop;
			}else if(exprx instanceof FuncRef){
				FuncRef asFuncRef = (FuncRef)exprx;
				FuncRef asFuncRefCop = (FuncRef)asFuncRef.copy();
				asFuncRefCop.vectroizedDegreeAndArgs=null;
				
				ArrayList<Object> retar = new ArrayList<Object>();
							
				for(Object arg : asFuncRef.argsForNextCompCycle.exprOrTypeArgsList){
					if(arg instanceof Expression) {
						processElement(retar, (Expression)arg, arrayArgs);
					}else {
						retar.add(arg);
						x++;
					}
				}
				
				FuncRefArgs fra = new FuncRefArgs(asFuncRef.argsForNextCompCycle.getLine(), asFuncRef.argsForNextCompCycle.getColumn());
				fra.exprOrTypeArgsList = retar;
				
				for(Pair<String, Object> arg : asFuncRef.argsForNextCompCycle.getNameMap()) {
					Object obb = arg.getB();
					if(obb instanceof Expression) {
						fra.addName(arg.getA(), processElement((Expression)arg, arrayArgs));
					}else {
						fra.addName(arg.getA(), obb);
						x++;
					}
				}
				
				asFuncRefCop.argsForNextCompCycle = fra;
				asFuncRefCop.args = fra;
				
				exprx = asFuncRefCop;
			}else if(exprx instanceof VectorizedFuncInvoke){
				VectorizedFuncInvoke asvfi = (VectorizedFuncInvoke)exprx;
				exprx = processFuncVect(arrayArgs, asvfi.expr, asvfi.funcInvoke, asvfi.nullsafe);
			}else if(exprx instanceof VectorizedFuncRef){
				VectorizedFuncRef asvfi = (VectorizedFuncRef)exprx;
				exprx = processFuncVect(arrayArgs, asvfi.expr, asvfi.funcRef, asvfi.nullsafe);
			}else if(exprx instanceof VectorizedNew){
				VectorizedNew asvfi = (VectorizedNew)exprx;
				exprx = processFuncVect(arrayArgs, asvfi.lhs, asvfi.constru, asvfi.nullsafe);
			}else if(exprx instanceof VectorizedFieldRef){
				VectorizedFieldRef asvfi = (VectorizedFieldRef)exprx;
				exprx = processFuncVect(arrayArgs, asvfi.expr, asvfi.name, asvfi.nullsafe);
			}else if(exprx instanceof CastExpression){
				CastExpression cop = (CastExpression)exprx.copy();
				cop.vectorizedExpr=null;
				
				Expression expr;
				if(tempArgs.containsKey(x)){
					ArrayRefLevelElementsHolder arrayLevelElements = new ArrayRefLevelElementsHolder();
					ArrayList<ArrayRefElement> item = new ArrayList<ArrayRefElement>();
					for(String aa : arrayArgs){
						item.add(new ArrayRefElement(line, col, new RefName(line, col, aa)));
					}
					
					arrayLevelElements.add(false, item);
					expr = new ArrayRef(line, col, new RefName(line, col, tempArgs.get(x).getA()), arrayLevelElements);
					x++;
				}else{
					x++;
					expr = vectorize(cop.o, arrayArgs, null);
				}
				
				cop.o = expr;
				
				exprx = cop;
			}else if(exprx instanceof DotOperator){
				DotOperator asDot = (DotOperator)exprx.copy();
				ArrayList<Expression> ele = asDot.getElements(null);
				
				int lastn = ele.size()-1;
				Expression lastOne = ele.get(lastn);
				
				ele.set(lastn, vectorize(lastOne, arrayArgs, null));
				x++;
				exprx = asDot;
			}else if(exprx instanceof ArrayRef) {
				ArrayRef arr = (ArrayRef)exprx;
				ArrayRef arrret = (ArrayRef)arr.copy();

				/*arrret.expr = vectorize(arr.expr, arrayArgs, null);
				x++;*/
				
				if(arr.vectArgumentDepth != null) {
					arrret.expr = (Expression)processElement(arr.expr, arrayArgs);
				}
				
				
				ArrayList<ArrayRefElement> allelements = arr.getFlatALE();
				ArrayList<ArrayRefElement> allelementsNew = new ArrayList<ArrayRefElement>();
				for(ArrayRefElement ele : allelements) {
					
					Expression newE1 = (Expression)processElement(ele.e1, arrayArgs);
										
					ArrayRefElement newAre;
					if(ele instanceof ArrayRefElementSubList) {
						ArrayRefElementSubList asSublist = (ArrayRefElementSubList)ele;
						
						Expression newE2 = (Expression)processElement(asSublist.e2, arrayArgs);
						newAre = new ArrayRefElementSubList(ele.getLine(), ele.getColumn(), newE1, newE2);
					}else if(ele instanceof ArrayRefElementPostfixAll) {
						newAre = new ArrayRefElementPostfixAll(ele.getLine(), ele.getColumn(), newE1);
					}else if(ele instanceof ArrayRefElementPrefixAll) {
						newAre = new ArrayRefElementPrefixAll(ele.getLine(), ele.getColumn(), newE1);
					}else {
						newAre = new ArrayRefElement(ele.getLine(), ele.getColumn(), newE1);
					}
					
					allelementsNew.add(newAre);
				}

				ArrayRefLevelElementsHolder arrayLevelElementHolder = new ArrayRefLevelElementsHolder();
				arrayLevelElementHolder.add(false, allelementsNew);
				arrret.arrayLevelElements = arrayLevelElementHolder;
				
			/*	PrintSourceVisitor psv = new PrintSourceVisitor();
				arrret.accept(psv);
				System.err.println("arrret: " + psv.toString());*/
				
				exprx=arrret;
			}else if(exprx instanceof Additive) {
				//processElement(Expression arg, ArrayList<String> arrayArgs)
				Additive asadd = (Additive)exprx;
				Expression head = (Expression)processElement( asadd.head, arrayArgs);
				ArrayList<AddMinusExpressionElement> newelements = new ArrayList<AddMinusExpressionElement>();
				for(AddMinusExpressionElement el : asadd.elements) {
					el = (AddMinusExpressionElement)el.copy();
					
					el.exp = (Expression)processElement( el.exp, arrayArgs);
					
					newelements.add(el);
				}
				
				exprx= new Additive(asadd.getLine(), asadd.getColumn(), head, newelements);
			}else if(exprx instanceof MulerExpression) {
				//processElement(Expression arg, ArrayList<String> arrayArgs)
				MulerExpression asadd = (MulerExpression)exprx;
				Expression head = (Expression)processElement( asadd.header, arrayArgs);
				ArrayList<MulerElement> newelements = new ArrayList<MulerElement>();
				for(MulerElement el : asadd.elements) {
					el = (MulerElement)el.copy();
					
					el.expr = (Expression)processElement( el.expr, arrayArgs);
					
					newelements.add(el);
				}
				
				exprx= new MulerExpression(asadd.getLine(), asadd.getColumn(), head, newelements);
			}else if(exprx instanceof PowOperator) {
				//processElement(Expression arg, ArrayList<String> arrayArgs)
				PowOperator asadd = (PowOperator)exprx;
				Expression head = (Expression)processElement( asadd.expr, arrayArgs);
				Expression raiseTo = (Expression)processElement( asadd.raiseTo, arrayArgs);
				
				exprx= new PowOperator(asadd.getLine(), asadd.getColumn(), head, raiseTo);
			}else if(exprx instanceof InExpression) {
				//processElement(Expression arg, ArrayList<String> arrayArgs)
				InExpression asadd = (InExpression)exprx;
				Expression thing = (Expression)processElement( asadd.thing, arrayArgs);
				Expression insideof = (Expression)processElement( asadd.insideof, arrayArgs);
				
				exprx= new InExpression(asadd.getLine(), asadd.getColumn(), thing, insideof, asadd.inverted);
			}else if(exprx instanceof EqReExpression) {
				//processElement(Expression arg, ArrayList<String> arrayArgs)
				EqReExpression asadd = (EqReExpression)exprx;
				Expression head = (Expression)processElement( asadd.head, arrayArgs);
				ArrayList<GrandLogicalElement> newelements = new ArrayList<GrandLogicalElement>();
				for(GrandLogicalElement el : asadd.elements) {
					el = (GrandLogicalElement)el.copy();
					
					el.e2 = (Expression)processElement( el.e2, arrayArgs);
					
					newelements.add(el);
				}
				
				exprx= new EqReExpression(asadd.getLine(), asadd.getColumn(), head, newelements);
			}else if(exprx instanceof ShiftExpression) {
				//processElement(Expression arg, ArrayList<String> arrayArgs)
				ShiftExpression asadd = (ShiftExpression)exprx;
				Expression head = (Expression)processElement( asadd.header, arrayArgs);
				ArrayList<ShiftElement> newelements = new ArrayList<ShiftElement>();
				for(ShiftElement el : asadd.elements) {
					el = (ShiftElement)el.copy();
					
					el.expr = (Expression)processElement( el.expr, arrayArgs);
					
					newelements.add(el);
				}
				
				exprx= new ShiftExpression(asadd.getLine(), asadd.getColumn(), head, newelements);
			}else if(exprx instanceof NotExpression) {
				//processElement(Expression arg, ArrayList<String> arrayArgs)
				NotExpression asadd = (NotExpression)exprx;
				Expression head = (Expression)processElement( asadd.expr, arrayArgs);
				
				exprx= new NotExpression(asadd.getLine(), asadd.getColumn(), head);
			}
			else if(exprx instanceof ElvisOperator) {
				ElvisOperator asadd = (ElvisOperator)exprx;
				Expression lhsExpression = (Expression)processElement( asadd.lhsExpression, arrayArgs);
				Expression rhsExpression = (Expression)processElement( asadd.rhsExpression, arrayArgs);
				exprx = new ElvisOperator(asadd.getLine(), asadd.getColumn(), lhsExpression, rhsExpression);
			}else if(exprx instanceof NotNullAssertion) {
				NotNullAssertion asadd = (NotNullAssertion)exprx;
				Expression expr = (Expression)processElement( asadd.expr, arrayArgs);
				exprx = new NotNullAssertion(asadd.getLine(), asadd.getColumn(), expr);
			}else if(exprx instanceof Is) {
				//processElement(Expression arg, ArrayList<String> arrayArgs)
				Is asadd = (Is)exprx;
				Expression head = (Expression)processElement( asadd.e1, arrayArgs);
				
				exprx= new Is(asadd.getLine(), asadd.getColumn(), head, asadd.typees, asadd.inverted);
			}else if(exprx instanceof BooleanAndOrExpression) {
				//processElement(Expression arg, ArrayList<String> arrayArgs)
				BooleanAndOrExpression asadd = (BooleanAndOrExpression)exprx;
				Expression head = (Expression)processElement( asadd.head, arrayArgs);
				ArrayList<RedirectableExpression> newelements = new ArrayList<RedirectableExpression>();
				for(RedirectableExpression el : asadd.things) {
					el = (RedirectableExpression)el.copy();
					newelements.add(new RedirectableExpression((Expression)processElement( el.exp, arrayArgs)));
				}
				
				if(exprx instanceof AndExpression) {
					exprx= new AndExpression(asadd.getLine(), asadd.getColumn(), head, newelements);
				}else {
					exprx= new OrExpression(asadd.getLine(), asadd.getColumn(), head, newelements);
				}
			}else if(exprx instanceof BitwiseOperation) {
				//processElement(Expression arg, ArrayList<String> arrayArgs)
				BitwiseOperation asadd = (BitwiseOperation)exprx;
				Expression head = (Expression)processElement( asadd.head, arrayArgs);
				ArrayList<RedirectableExpression> newelements = new ArrayList<RedirectableExpression>();
				for(RedirectableExpression el : asadd.things) {
					el = (RedirectableExpression)el.copy();
					newelements.add(new RedirectableExpression((Expression)processElement( el.exp, arrayArgs)));
				}
				
				exprx= new BitwiseOperation(asadd.getLine(), asadd.getColumn(), asadd.oper, head, newelements);
				
			}else if(exprx instanceof PrefixOp) {
				//processElement(Expression arg, ArrayList<String> arrayArgs)
				PrefixOp asadd = (PrefixOp)exprx;
				Expression head = (Expression)processElement( asadd.p1, arrayArgs);
				
				exprx= new PrefixOp(asadd.getLine(), asadd.getColumn(), asadd.prefix, head);
			}else if(exprx instanceof PostfixOp) {
				//processElement(Expression arg, ArrayList<String> arrayArgs)
				PostfixOp asadd = (PostfixOp)exprx;
				Expression head = (Expression)processElement( asadd.p2, arrayArgs);
				
				exprx= new PostfixOp(asadd.getLine(), asadd.getColumn(), asadd.postfix, head);
			}	
			
			if(preamble != null){
				exprx = DotOperator.buildDotOperatorPrefixing(line, col, preamble.getA(), preamble.getB(), preamble.getC(), exprx);
			}
			
			return exprx;
		}
	}
	

	private Expression returnCreator(int line, int col, String primeArg, Type fiRetType, ArrayList<Pair<Boolean, NullStatus>> vectStruct, ArrayList<Pair<Boolean, NullStatus>> vectStructOfVectElement, int n) {
		//from: T T F F T -> ArrayList<ArrayList<double[]>>[2]
		Expression ret = null;
		int sz = vectStruct.size();
		
		Type tt = (Type)fiRetType.copy();
		
		for(int i=sz-1 ; i >=n; i--) {
			
			int arLevels = 0;
			while(vectStruct.get(i).getA()) {
				arLevels++;
				if(--i < n) {
					break;
				}
			}

			if(arLevels > 0){
				tt.setArrayLevels(arLevels);
			}
			
			if(i >= n && !vectStruct.get(i).getA()) {
				
				NamedType li;
				if(i-1 >=n /*&& vectStruct.get(i-1)*/) {
					li = ScopeAndTypeChecker.const_list;
				}else{
					li = ScopeAndTypeChecker.const_arrayList;
				}
				
				li = (NamedType)li.copy();
				ArrayList<Type> genTypes = new ArrayList<Type>();
				genTypes.add(tt);
				tt.setInOutGenModifier(null);
				li.setGenTypes(genTypes);
				tt = li;   
			}
		}
		
		if(tt.hasArrayLevels()) {
			ArrayList<Expression> retARLevels = new ArrayList<Expression>();
			//retARLevels.add(DotOperator.buildDotOperator(line, col, new RefName(primeArg), new RefName("length") ));
			retARLevels.add(DotOperator.buildDotOperator(line, col, new RefName(primeArg), lengthOrSize(line, col, vectStructOfVectElement, n) ));
			for(int nd=0; nd < tt.getArrayLevels()-1; nd++){
				retARLevels.add(null);
			}
			tt.setArrayLevels(0);
			ret = new ArrayConstructor(line, col, tt, retARLevels, null);
		}else {
			ret = new New(line, col, tt, new FuncInvokeArgs(line, col), true);
		}
		
		return ret;
	}
	 
	 
	
	private Expression lengthOrSize(int line, int col, ArrayList<Pair<Boolean, NullStatus>> vectStruct, int n) {
		/*if(vectStruct.isEmpty()) {
			this.raiseError(line, col, "Unable to vectorize statement");///?
			rootItem.setHasErroredAlready(true);
			return null;
		}*/
		
		if(vectStruct.get(n/*-1-n*/).getA()) {//true -> array
			return new RefName("length");
		}else {
			return new FuncInvoke(line, col,"size");
		}
	}

	private final static ErrorRaiseable errorRaisableSupression = new ErrorRaiseableSupressErrors(null);
	
	private Block remapVectorization(int line, int col, CanBeInternallyVectorized rootItem, final int uid, boolean returnExpected, ArrayList<Pair<Boolean, NullStatus>> vectStruct, int vectToArrayLevels, final boolean isAssignExisting, boolean rhsVarsDupeLhs, AssignExisting extraVarToCreate){
		//scan through and find vectorized arguments and any other args we need to process (including directly nested)
		
		Thruple<ArrayList<Expression>, Boolean, Boolean> preamble = null;
		if(!dopSoFarTmpVars.isEmpty()){
			preamble = dopSoFarTmpVars.pop();
		}
		
		ExpressionFinder finder = new ExpressionFinder(line, col, this, isAssignExisting);
		if(rootItem instanceof AssignExisting) {
			if(!finder.findExpressions((AssignExisting)rootItem, rhsVarsDupeLhs)){
				rootItem.setHasErroredAlready(true);
				return null;//give up on err
			}
		}else {
			if(!finder.findExpressions((Expression)rootItem)){
				rootItem.setHasErroredAlready(true);
				return null;//give up on err
			}
		}
		
		
		//turn this into a iterator over the vectorized arguments
		/*
		 	tempM = m
		 	tempArg1 = myArgExpr()
		 	result = new double[tempM.lengthl][]
			for(temp1 = 0; temp1 < tempM.length; temp1++){
				tempAr = tempM[temp1]
				row = new double[tempAr.length]
				result[temp1] = row;
				for(temp2 = 0; temp2 < tempAr.length; temp2++){
					row[temp2] = sin(tempAr[temp2], tempArg1)
					row[temp2] = tempAr[temp2].sin(tempArg1)//for VectorizedFuncInvoke
				}
			}
			result
		 */
		
		String inplaceAssignment = null; 
		
		//ArrayList<Integer> whichArgsVect = funcInvoke.vectroizedDegreeAndArgs.getB();
		
		
		Block ret = new Block(line, col);
		if(null != extraVarToCreate) {
			ret.add(extraVarToCreate);
		}
		
		//temp args - so eval only once
		//List<Expression> args = funcInvoke.args.getArgumentsWNPs();
		OrderedHashMap<Integer, Pair<String, Type>> tempArgs = new OrderedHashMap<Integer, Pair<String, Type>>();//arg to tempname
		ArrayList<Integer> tmpArgCreated = new ArrayList<Integer>(finder.allArgs.size());
		int n=0;
		//int m=0;
		for(Node arg: finder.allArgs){
			if(finder.ignoreTmpArg.contains(n)) {//dont gennerate a tmp var for this
				n++;
				continue;
			}
			

			if(finder.dupVarsMap != null && finder.dupVarsMap.containsKey(n)) {
				int existing = finder.dupVarsMap.get(n);
				tempArgs.put(n, tempArgs.get(existing));
				n++;
				continue;
			}
			
			if(arg instanceof Expression) {
				arg = (Node)extractExprFromDot((Expression)arg);
			}
			
			if((arg instanceof Type && !(arg instanceof VarNull)) ) {//funcrefs can have Types as arguments
				tempArgs.put(n, new Pair<>("", null));
				n++;
				continue;
			}
			
			if(arg instanceof CanBeInternallyVectorized) {
				if(finder.vectorizedArgsSELF != n && finder.vectorizedArgs.contains(n) && !finder.makeTempVarEvenThoughVectorized.contains(n)){
					n++;//skipa funcinvoke which is vectorized
					continue; 
				}else if(arg instanceof VectorizedFieldRef) {
					while(arg instanceof VectorizedFieldRef) {
						arg = (Node)((VectorizedFieldRef)arg).expr;//extract to root
					}
				}
			}		
			
			String tempArg = "vec$tmparg$" + uid + "$" + n;
			if(finder.vectorizedArgsSELF == n){
				inplaceAssignment=tempArg;
			}
			
			Type tt = arg.getTaggedType();
			tt = TypeCheckUtils.getRefTypeToLocked(tt);
			tempArgs.put(n, new Pair<>(tempArg, tt));
			tmpArgCreated.add(n);
			//m++;
			n++;
			
			AssignNew tmpVarNew = new AssignNew(null, line, col, false, false, tempArg, null, null, AssignStyleEnum.EQUALS, (Expression)arg);
			ret.add(tmpVarNew);
		}
		
		boolean shouldReturn = returnExpected || inplaceAssignment != null;
		
		HashSet<Integer> extractVectSizes = new HashSet<Integer>();
		finder.vectLevels.stream().filter(a -> a > 0).map(a -> extractVectSizes.add(a));
		//next line concerining tmpargs is a bit messy, oh well
		
		//tempArgs.values().stream().filter(a -> a != null /*&& !a.getA().startsWith("vec$tmparg")*/).forEach(a -> extractVectSizes.add(TypeCheckUtils.getVectorizedStructure(errorRaisableSupression, a.getB()).size()));
		
		//tempArgs.values().stream().filter(a -> a != null && !a.getA().startsWith("vec$tmparg")).forEach(a -> extractVectSizes.add(TypeCheckUtils.getVectorizedStructure(errorRaisableSupression, a.getB()).size()));
		
		if(extractVectSizes.size() > 1) {
			this.raiseError(line, col, "Vectorized Expression may not contain elements differing in degree of extraction required for vectorization");
			rootItem.setHasErroredAlready(true);
			return null;
		}
		
		
		if(!shouldReturn && !rootItem.canBeNonSelfReferncingOnItsOwn()) {
			this.raiseError(line, col, "Vectorized Expression cannot appear on its own line");
			rootItem.setHasErroredAlready(true);
			return null;
		}

		hadMadeRepoints=true;
		
		int firstVectoredArg = finder.vectorizedArgs.get(0);
		String primeArg = null;
		ArrayList<Pair<Boolean, NullStatus>> primeVectArg = null;
		for(int tmpargcre : tmpArgCreated) {
			if(finder.vectorizedArgs.contains(tmpargcre)) {
				Pair<String, Type> nameAndType = tempArgs.get(tmpargcre);
				primeArg = nameAndType.getA();
				primeVectArg = TypeCheckUtils.getVectorizedStructure(errorRaisableSupression, nameAndType.getB());
				break;
			}
		}
		
		//String primeArg = tempArgs.get(firstVectoredArg);
		//if(primeArg == null) {//MHA: this looks wrong!
		//	primeArg = tempArgs.getElement(firstVectoredArg);
		//}
		
		String returnArg = inplaceAssignment != null? inplaceAssignment: "vec$ret$" + uid;
		
		Type fiRetType = (Type)((Node)rootItem).getTaggedType().copy();//(Type)TypeCheckUtils.applyVectStruct(vectStruct, ((Node)rootItem).getTaggedType());
		
		ArrayList<Pair<Boolean, NullStatus>> vectStructureOfReturnType = TypeCheckUtils.getVectorizedStructure(errorRaisableSupression, fiRetType);
		
		fiRetType = TypeCheckUtils.extractVectType(fiRetType);
		
		if(returnExpected && inplaceAssignment == null){
			Expression levelCreator = returnCreator(line, col, primeArg, fiRetType,  vectStructureOfReturnType, vectStruct, 0);
			ret.add(new AssignExisting(line, col, new RefName(line, col, returnArg), AssignStyleEnum.EQUALS, levelCreator));
		}
		
		String extractingFrom = primeArg;
		RefName writeTo = new RefName(returnArg);
		Block addto = ret;
		ArrayList<String> arrayArgs = new ArrayList<String>();
		Stack<Assign> afterFor = new Stack<Assign>();
		

		Type firstType = finder.allArgs.get(firstVectoredArg).getTaggedType();
		firstType = TypeCheckUtils.getRefTypeToLocked(firstType);
		
		//int inputToExpand= firstType.getArrayLevels() - vectToArrayLevels;
		int inputToExpand= /*TypeCheckUtils.getVectorizedStructure(errorRaisableSupression, firstType).size()-*/ vectToArrayLevels;
		
		int x = 0; 
		while(--inputToExpand >= 0){
			x++;
			/*
			 for(temp1 = 0; temp1 < tempM.length; temp1++){
				tempAr = tempM[temp1]
				row = new double[tempAr.length]
				result[temp1] = row;
				for(temp2 = 0; temp2 < tempAr.length; temp2++){
					row[temp2] = sin(tempAr[temp2], tempArg1)
				}
			}
			 */
			String forN = "vec$forN$" + uid + "$" + n++;
			arrayArgs.add(forN);
			
			Block body= new Block(line, col);

			ArrayList<GrandLogicalElement> elements = new ArrayList<GrandLogicalElement>(1);
			Expression lors = lengthOrSize(line, col, primeVectArg, x-1);
			if(null == lors) {
				return null;
			}
			elements.add( new GrandLogicalElement(line, col, GrandLogicalOperatorEnum.LT, DotOperator.buildDotOperator(line, col, new RefName(extractingFrom), lors ) ) );
			Expression check = new EqReExpression(line, col, new RefName(line, col, forN), elements);
			
			ForBlockOld forB = new ForBlockOld(line, col, null, forN, null, AssignStyleEnum.EQUALS, new VarInt(line, col, 0), 
												check, new PostfixOp(line, col, FactorPostFixEnum.PLUSPLUS, new RefName(line, col, forN)), 
												body, null);

			Assign addAfterFor = null;
			if(!afterFor.isEmpty()){
				addAfterFor = afterFor.pop();
			}
			
			if(inputToExpand == 0){//final inner level, assignment
				ConvertToArrayIndexForm exprVec = new ConvertToArrayIndexForm(line, col, finder, tempArgs, finder.ignoreTmpArg);
				Expression funcInvokeCop;
				
				if(rootItem instanceof AssignExisting) {
					funcInvokeCop = exprVec.vectorize((AssignExisting)rootItem, arrayArgs, preamble);
				}else {
					funcInvokeCop = exprVec.vectorize((Expression)rootItem, arrayArgs, preamble);
				}
				
				/*PrintSourceVisitor psv = new PrintSourceVisitor();
				funcInvokeCop.accept(psv);
				String gg = psv.toString();
				System.err.println("so far: " + gg);*/
				
				if(shouldReturn){
					((Node)funcInvokeCop).setShouldBePresevedOnStack(true);
					Boolean arrayOrList = (!vectStructureOfReturnType.isEmpty()?vectStructureOfReturnType:vectStruct).get(x-1).getA();//MHA
					
					if(arrayOrList || inplaceAssignment != null) {//array or in place assignment
						Expression lhsExprForAssignInPlace = null;
						if(inplaceAssignment != null) {
							Node selfArg = finder.allArgs.get(finder.vectorizedArgsSELF);
							if(selfArg instanceof Expression && exprIsVectorizedFieldRef ((Expression)selfArg)) {
								exprVec = new ConvertToArrayIndexForm(line, col, finder, tempArgs, finder.ignoreTmpArg);
								lhsExprForAssignInPlace = exprVec.vectorize((Expression)selfArg, arrayArgs, preamble);
							}
						}
						
						if(null == lhsExprForAssignInPlace) {
							lhsExprForAssignInPlace = ArrayRef.ArrayRefOne(line, col, (RefName)writeTo.copy(), new RefName(line, col, forN));
						}
						
						body.add(new AssignExisting(line, col, lhsExprForAssignInPlace, AssignStyleEnum.EQUALS_STRICT, funcInvokeCop));
					}else {
						body.add(new DuffAssign(DotOperator.buildDotOperator(line, col, (RefName)writeTo.copy(), new FuncInvoke(line, col, "add", funcInvokeCop) )));
					}
				}else{
					body.add(new DuffAssign(funcInvokeCop));
				}
				
			}else{
				/*
				 row = new double[tempAr.length]
				tempAr = tempM[temp1]
				result[temp1] = row;
				for(temp2 = 0; temp2 < tempAr.length; temp2++){
					row[temp2] = sin(tempAr[temp2], tempArg1)
				}
				*/

				//tempAr = tempM[temp1]
				String operatingon = "vec$tmparg$" + uid + "$" + n++;
				
				body.add(new AssignExisting(line, col, new RefName(line, col, operatingon), AssignStyleEnum.EQUALS, ArrayRef.ArrayRefOne(line, col, new RefName(line, col, extractingFrom), new RefName(line, col, forN))));
				extractingFrom = operatingon;
				
				
				//row = new double[tempAr.length]
				String resRow = "vec$row$" + uid + "$" + n++;
				if(shouldReturn){
					if(inplaceAssignment == null) {
						Expression levelCreator = returnCreator(line, col, extractingFrom, fiRetType,  vectStructureOfReturnType, vectStruct, x);
						body.add(new AssignExisting(line, col, new RefName(line, col, resRow), AssignStyleEnum.EQUALS, levelCreator));
						
						//result[temp1] = row;
						afterFor.push(new AssignExisting(line, col, ArrayRef.ArrayRefOne(line, col, (RefName)writeTo.copy(), new RefName(line, col, forN)), AssignStyleEnum.EQUALS_STRICT, new RefName(line, col, resRow)));
						
						if(vectStructureOfReturnType.get(x-1).getA()) {
							afterFor.push(new AssignExisting(line, col, ArrayRef.ArrayRefOne(line, col, (RefName)writeTo.copy(), new RefName(line, col, forN)), AssignStyleEnum.EQUALS_STRICT, new RefName(line, col, resRow)));
						}else {
							//ret.add(funcInvokeCop);
							afterFor.push(new DuffAssign(DotOperator.buildDotOperator(line, col, (RefName)writeTo.copy() , new FuncInvoke(line, col, "add", new RefName(line, col, resRow)) )));
						}
					}else {
						resRow = operatingon;
					}
				}
				
				//next one...
				writeTo = new RefName(resRow);
			}

			addto.add(forB);
			if(addAfterFor != null){
				addto.add(addAfterFor);
			}
			addto.isolated=true;
			addto = body;
		}
		
		if(returnExpected){
			ret.add(new DuffAssign(new RefName(line, col, returnArg)));
		}
		ret.setShouldBePresevedOnStack(returnExpected);
		
		/*PrintSourceVisitor psv2 = new PrintSourceVisitor();
		ret.accept(psv2);
		String gg2 = psv2.toString();*/
		
		return ret;
	}
	
	
	@Override
	public Object visit(FuncInvoke funcInvoke) {
		if(null != funcInvoke.vectroizedDegreeAndArgs){
			int uid = ++vectorizedRedirectUID;
			if(!funcInvoke.hasVectorizedRedirect()){
				//int depth = funcInvoke.vectroizedDegreeAndArgs.getA().size() - arrayRef.vectDepth;
				funcInvoke.setVectorizedRedirect(remapVectorization(funcInvoke.getLine(), funcInvoke.getColumn(), funcInvoke, uid, funcInvoke.getShouldBePresevedOnStack(), funcInvoke.vectroizedDegreeAndArgs.getA(), funcInvoke.vectroizedDegreeAndArgs.getA().size(), false, false, null));
			}
		}else{
			super.visit(funcInvoke);
		}
		
		//args...
		return null;
	}
	
	
	
	@Override
	public Object visit(FuncRef funcRef) {
		if(null != funcRef.vectroizedDegreeAndArgs){
			int uid = ++vectorizedRedirectUID;
			if(!funcRef.hasVectorizedRedirect()){
				//int depth = funcRef.vectroizedDegreeAndArgs.getA().size() - funcRef.vectroizedDegreeAndArgs.getC();
				funcRef.setVectorizedRedirect(remapVectorization(funcRef.getLine(), funcRef.getColumn(), funcRef, uid, funcRef.getShouldBePresevedOnStack(), funcRef.vectroizedDegreeAndArgs.getA(), funcRef.vectroizedDegreeAndArgs.getA().size(), false, false, null));
			}
		}else{
			super.visit(funcRef);
		}
		
		//args...
		return null;
	}
	
	@Override
	public Object visit(ArrayRef arrayRef) {
		if(arrayRef.vectDepth != null || arrayRef.vectArgumentDepth != null){
			int uid = ++vectorizedRedirectUID;
			if(!arrayRef.hasVectorizedRedirect()){
				Type messy = arrayRef.getTaggedType();
				messy = (Type)messy.copy();
				messy.setArrayLevels(messy.getArrayLevels() -1);
				ArrayList<Pair<Boolean, NullStatus>> retTo = TypeCheckUtils.getVectorizedStructure(errorRaisableSupression, messy);
				
				ArrayList<Pair<Boolean, NullStatus>> vectStructure = new ArrayList<Pair<Boolean, NullStatus>>();
				
				if(arrayRef.vectArgumentDepth != null) {
					vectStructure.addAll(arrayRef.vectArgumentDepth);
				}
				else {
					vectStructure.addAll(arrayRef.vectDepth);
					vectStructure.addAll(retTo);
				}
				
				//vectStructure.addAll(arrayRef.vectArgumentDepth != null? arrayRef.vectArgumentDepth : arrayRef.vectDepth);
				//vectStructure.addAll(retTo);
				
				int depth = (arrayRef.vectArgumentDepth != null? arrayRef.vectArgumentDepth : arrayRef.vectDepth).size();
				
				arrayRef.setVectorizedRedirect(remapVectorization(arrayRef.getLine(), arrayRef.getColumn(), arrayRef, uid, arrayRef.getShouldBePresevedOnStack(), vectStructure, depth, false, false, null));
			}
		}else{
			super.visit(arrayRef);
		}
		
		//args...
		return null;
	}

	@Override
	public Object visit(FuncRefInvoke funcRefInvoke) {
		if(null != funcRefInvoke.depth){
			int uid = ++vectorizedRedirectUID;
			if(!funcRefInvoke.hasVectorizedRedirect()){
				funcRefInvoke.setVectorizedRedirect(remapVectorization(funcRefInvoke.getLine(), funcRefInvoke.getColumn(), funcRefInvoke, uid, funcRefInvoke.getShouldBePresevedOnStack(), funcRefInvoke.depth, funcRefInvoke.depth.size(), false, false, null));
			}
		}else{
			super.visit(funcRefInvoke);
		}
		
		//args...
		return null;
	}
	
	@Override
	public Object visit(New funcInvoke) {
		if(null != funcInvoke.vectroizedDegreeAndArgs){
			int uid = ++vectorizedRedirectUID;
			if(!funcInvoke.hasVectorizedRedirect()){
				int depth = funcInvoke.vectroizedDegreeAndArgs.getA().size() - funcInvoke.vectroizedDegreeAndArgs.getC();
				funcInvoke.setVectorizedRedirect(remapVectorization(funcInvoke.getLine(), funcInvoke.getColumn(), funcInvoke, uid, funcInvoke.getShouldBePresevedOnStack(), funcInvoke.vectroizedDegreeAndArgs.getA(), depth, false, false, null));
			}
		}else{
			super.visit(funcInvoke);
		}
		
		//args...
		return null;
	}
	
	
	@Override
	public Object visit(VectorizedFuncInvoke vectorizedFuncInvoke) {
		int uid = ++vectorizedRedirectUID;
		if(!vectorizedFuncInvoke.hasVectorizedRedirect()){
			/*int consuVecLevels = 0;//vectorizedFuncInvoke.doubledot?0:TypeCheckUtils.getVectorizedStructure(errorRaisableSupression, vectorizedFuncInvoke.getTaggedType()).size(); 
			if(null != vectorizedFuncInvoke.funcInvoke.resolvedFuncTypeAndLocation) {
				FuncType ft = (FuncType)vectorizedFuncInvoke.funcInvoke.resolvedFuncTypeAndLocation.getType();
				if(ft.extFuncOn) {
					consuVecLevels = TypeCheckUtils.getVectorizedStructure(errorRaisableSupression, ft.inputs.get(0)).size();
				}else {
					Location loc = vectorizedFuncInvoke.funcInvoke.resolvedFuncTypeAndLocation.getLocation();
					if(loc instanceof ClassFunctionLocation) {
						if(null != TypeCheckUtils.checkSubType(errorRaisableSupression, ScopeAndTypeChecker.list_object, ((ClassFunctionLocation)loc).ownerType)) {
							//if resolves to a call of a list method like get
							consuVecLevels = TypeCheckUtils.getVectorizedStructure(errorRaisableSupression, vectorizedFuncInvoke.getTaggedType()).size(); 
						}
					}
				}
			}*/
			int consuVecLevels = vectorizedFuncInvoke.getDepth().size();
			
			vectorizedFuncInvoke.setVectorizedRedirect(remapVectorization(vectorizedFuncInvoke.getLine(), vectorizedFuncInvoke.getColumn(), vectorizedFuncInvoke, uid, vectorizedFuncInvoke.getShouldBePresevedOnStack(), vectorizedFuncInvoke.getDepth(), consuVecLevels, false, false, null));
		}
		return null;
	}
	
	@Override
	public Object visit(VectorizedFuncRef vectorizedFuncRef) {
		int uid = ++vectorizedRedirectUID;
		if(!vectorizedFuncRef.hasVectorizedRedirect()){
			vectorizedFuncRef.setVectorizedRedirect(remapVectorization(vectorizedFuncRef.getLine(), vectorizedFuncRef.getColumn(), vectorizedFuncRef, uid, vectorizedFuncRef.getShouldBePresevedOnStack(), vectorizedFuncRef.getDepth(), vectorizedFuncRef.getDepth().size(), false, false, null));
		}
		return null;
	}
	
	@Override
	public Object visit(VectorizedNew vectorizedFuncRef) {
		int uid = ++vectorizedRedirectUID;
		if(!vectorizedFuncRef.hasVectorizedRedirect()){
			vectorizedFuncRef.setVectorizedRedirect(remapVectorization(vectorizedFuncRef.getLine(), vectorizedFuncRef.getColumn(), vectorizedFuncRef, uid, vectorizedFuncRef.getShouldBePresevedOnStack(), vectorizedFuncRef.getDepth(), vectorizedFuncRef.getDepth().size(), false, false, null));
		}
		return null;
	}
	
	@Override
	public Object visit(VectorizedFieldRef vectorizedFuncRef) {
		int uid = ++vectorizedRedirectUID;
		if(!vectorizedFuncRef.hasVectorizedRedirect()){
			Block what = remapVectorization(vectorizedFuncRef.getLine(), vectorizedFuncRef.getColumn(), vectorizedFuncRef, uid, vectorizedFuncRef.getShouldBePresevedOnStack(), vectorizedFuncRef.getDepth(), vectorizedFuncRef.getDepth().size(), false, false, null);
			
			if(!vectorizedFuncRef.validAtThisLocation){
				this.raiseError(vectorizedFuncRef.getLine(), vectorizedFuncRef.getColumn(), "Field referece cannot be vectorized at this location");
			}else{
				vectorizedFuncRef.setVectorizedRedirect(what);
			}
		}
		
		return null;
	}

	
	
	
	@Override
	public Object visit(Additive addMinusExpression) {
		ArrayList<Pair<Boolean, NullStatus>> depth = addMinusExpression.depth;
		if(depth != null){
			int uid = ++vectorizedRedirectUID;
			if(!addMinusExpression.hasVectorizedRedirect()){
				addMinusExpression.setVectorizedRedirect(remapVectorization(addMinusExpression.getLine(), addMinusExpression.getColumn(), addMinusExpression, uid, addMinusExpression.getShouldBePresevedOnStack(), depth, depth.size(), false, false, null));
			}
		}
		else{
			super.visit(addMinusExpression);
		}
		
		return null;
	}
	
	@Override
	public Object visit(InExpression inExpression) {
		ArrayList<Pair<Boolean, NullStatus>> depth = inExpression.depth;
		if(depth != null){
			int uid = ++vectorizedRedirectUID;
			if(!inExpression.hasVectorizedRedirect()){
				inExpression.setVectorizedRedirect(remapVectorization(inExpression.getLine(), inExpression.getColumn(), inExpression, uid, inExpression.getShouldBePresevedOnStack(), depth, depth.size(), false, false, null));
			}		
		}
		else if(inExpression.containsMethodCall != null) {
			inExpression.containsMethodCall.accept(this);
		}
		else{
			super.visit(inExpression);
		}
		
		return null;
	}
	
	@Override
	public Object visit(MulerExpression mulerExpr) {
		ArrayList<Pair<Boolean, NullStatus>> depth = mulerExpr.depth;
		if(depth != null){
			int uid = ++vectorizedRedirectUID;
			if(!mulerExpr.hasVectorizedRedirect()){
				mulerExpr.setVectorizedRedirect(remapVectorization(mulerExpr.getLine(), mulerExpr.getColumn(), mulerExpr, uid, mulerExpr.getShouldBePresevedOnStack(), depth, depth.size(), false, false, null));
			}		
		}
		else{
			super.visit(mulerExpr);
		}
		
		return null;
	}
	
	@Override
	public Object visit(PowOperator mulerExpr) {
		ArrayList<Pair<Boolean, NullStatus>> depth = mulerExpr.depth;
		if(depth != null){
			int uid = ++vectorizedRedirectUID;
			if(!mulerExpr.hasVectorizedRedirect()){
				mulerExpr.setVectorizedRedirect(remapVectorization(mulerExpr.getLine(), mulerExpr.getColumn(), mulerExpr, uid, mulerExpr.getShouldBePresevedOnStack(), depth, depth.size(), false, false, null));
			}		
		}
		else{
			super.visit(mulerExpr);
		}
		
		return null;
	}
	
	@Override
	public Object visit(PrefixOp prefixOp) {
		ArrayList<Pair<Boolean, NullStatus>> depth = prefixOp.depth;
		if(depth != null){
			int uid = ++vectorizedRedirectUID;
			if(!prefixOp.hasVectorizedRedirect()){
				prefixOp.setVectorizedRedirect(remapVectorization(prefixOp.getLine(), prefixOp.getColumn(), prefixOp, uid, prefixOp.getShouldBePresevedOnStack(), depth, depth.size(), false, false, null));
			}		
		}
		else{
			super.visit(prefixOp);
		}
		
		return null;
	}
	
	
	@Override
	public Object visit(PostfixOp prefixOp) {
		ArrayList<Pair<Boolean, NullStatus>> depth = prefixOp.depth;
		if(depth != null){
			int uid = ++vectorizedRedirectUID;
			if(!prefixOp.hasVectorizedRedirect()){
				prefixOp.setVectorizedRedirect(remapVectorization(prefixOp.getLine(), prefixOp.getColumn(), prefixOp, uid, prefixOp.getShouldBePresevedOnStack(), depth, depth.size(), false, false, null));
			}		
		}
		else{
			super.visit(prefixOp);
		}
		
		return null;
	}
	
	private int translateAssignExistingTmpVarCnt = 0;
	
	private Thruple<CanBeInternallyVectorized, Boolean, AssignExisting> translateAssignExisting(AssignExisting ass) {
		int line = ass.getLine();
		int col = ass.getColumn();
		
		//normal scheme: x += 1 -> x^^ + 1
		boolean useAlternativeScheme = false;//alternative scheeme : x = x + 1 
		
		Expression lhs = (Expression)ass.assignee.copy();
		if(lhs instanceof Vectorized) {
			
			Vectorized asVect = (Vectorized)ass.assignee;
			Expression ofVect = asVect.expr;
			
			if(ofVect instanceof VectorizedArrayRef) {
				lhs = ((VectorizedArrayRef)ofVect).astOverridearrayRef;
				ofVect = lhs;
			}
			
			if(ofVect instanceof ArrayRef) {
				ArrayRef ar = (ArrayRef)ofVect;
				if(ar.vectArgumentDepth != null || ar.vectDepth != null) {
					useAlternativeScheme=true;
				}
				lhs = ofVect;
			}
			
			if(!useAlternativeScheme) {
				((Vectorized)lhs).doubledot=true;
			}
		}else {
			lhs = new Vectorized(lhs.getLine(), lhs.getColumn(), lhs, true, false);
		}
		
		Expression rhs = ass.expr;

		CanBeInternallyVectorized ret=null;
		AssignExisting extraVarToCreate=null;
		switch(ass.eq) {
			case PLUS_EQUALS: ret= new Additive(line, col, lhs, true, rhs); break;
			case MINUS_EQUALS: ret = new Additive(line, col, lhs, false, rhs); break;
			case MUL_EQUALS: ret = new MulerExpression( line,  col, lhs, MulerExprEnum.MUL , rhs); break;
			case DIV_EQUALS: ret = new MulerExpression( line,  col, lhs, MulerExprEnum.DIV , rhs); break;
			case MOD_EQUALS: ret = new MulerExpression( line,  col, lhs, MulerExprEnum.MOD , rhs); break;
			case POW_EQUALS: ret = new PowOperator( line,  col, lhs, rhs); break;
			case OR_EQUALS: ret =  new OrExpression(line, col, lhs, new RedirectableExpression(rhs)); break;
			case AND_EQUALS: ret = AndExpression.AndExpressionBuilder(line, col, lhs, new RedirectableExpression(rhs)); break;
			case LSH: ret = new ShiftExpression(line, col, lhs, ShiftOperatorEnum.LS, rhs); break;
			case RSH: ret = new ShiftExpression(line, col, lhs, ShiftOperatorEnum.RS, rhs); break;
			case RHSU: ret = new ShiftExpression(line, col, lhs, ShiftOperatorEnum.URS, rhs); break;
			case BAND: ret = new BitwiseOperation(line, col, BitwiseOperationEnum.AND, lhs, new RedirectableExpression(rhs)); break;
			case BOR: ret = new BitwiseOperation(line, col, BitwiseOperationEnum.OR, lhs, new RedirectableExpression(rhs)); break;
			case BXOR: ret = new BitwiseOperation(line, col, BitwiseOperationEnum.XOR, lhs, new RedirectableExpression(rhs)); break;
			case EQUALS_STRICT: ret = ass; break;
			case EQUALS: ret = ass; break;
		}
		
		if(ret != null) {
			
			if(useAlternativeScheme) {
				
				//if(lhs instanceof ArrayRef) {
				
				ArrayRef fromAR = (ArrayRef)lhs;
				ArrayRef arCopy = (ArrayRef)fromAR.copy();
				arCopy.vectArgumentDepth = fromAR.vectArgumentDepth == null?null:new ArrayList<Pair<Boolean, NullStatus>>( fromAR.vectArgumentDepth );
				arCopy.vectDepth = fromAR.vectDepth == null?null:new ArrayList<Pair<Boolean, NullStatus>>( fromAR.vectDepth );
				
				ArrayRef assignlhs = arCopy;
				
				//so as to avid invoking the lhs expression twice we must create a temp variable
				Expression tmpVarType = fromAR.expr;
				Type ttype = fromAR.expr.getTaggedType();
				String tmpLHSExprVar = "tmp$vectARExt" + translateAssignExistingTmpVarCnt++;
				
				arCopy.expr = new RefName(tmpLHSExprVar);
				arCopy.expr.setTaggedType(ttype);
				fromAR.expr = new RefName(tmpLHSExprVar); 
				fromAR.expr.setTaggedType(ttype);
				
				if(tmpVarType instanceof Vectorized) {
					arCopy.expr = new Vectorized(arCopy.expr);
					fromAR.expr = new Vectorized(fromAR.expr); 
					tmpVarType = ((Vectorized)tmpVarType).expr;
				}
				
				

				extraVarToCreate = new AssignExisting(line, col, tmpLHSExprVar, AssignStyleEnum.EQUALS, tmpVarType); 
				
				/*}else {
					assignlhs = lhs;
				}*/
				ret = new AssignExisting(line, col, assignlhs, AssignStyleEnum.EQUALS_STRICT, (Expression)ret);
			}
			
			((Node)ret).setTaggedType(ass.getTaggedType());
		}else {
			throw new RuntimeException("Cannot translate assignment of: " + ass.eq);
		}
		
		return new Thruple<CanBeInternallyVectorized, Boolean, AssignExisting>(ret, useAlternativeScheme, extraVarToCreate);
	}
	
	@Override
	public Object visit(AssignExisting assignExisting) {
		ArrayList<Pair<Boolean, NullStatus>> depth = assignExisting.depth;
		if(depth != null){
			int uid = ++vectorizedRedirectUID;
			if(!assignExisting.hasVectorizedRedirect()){
				//translate into easier to work with form:
				//myAr^ += 1 => myAr^^ + 1
				Thruple<CanBeInternallyVectorized, Boolean, AssignExisting> asExprAndExtraVar = translateAssignExisting(assignExisting);
				CanBeInternallyVectorized asExpr = asExprAndExtraVar.getA();
				boolean rhsVarsDupeLhs = asExprAndExtraVar.getB();
				AssignExisting extraVarToCreate = asExprAndExtraVar.getC();
				assignExisting.setVectorizedRedirect(remapVectorization(assignExisting.getLine(), assignExisting.getColumn(), asExpr, uid, assignExisting.getShouldBePresevedOnStack(), depth, depth.size(), true, rhsVarsDupeLhs, extraVarToCreate));
			}		
		}
		else{
			super.visit(assignExisting);
		}
		
		return null;
	}
	
	
	@Override
	public Object visit(BitwiseOperation bitwiseop) {
		ArrayList<Pair<Boolean, NullStatus>> depth = bitwiseop.depth;
		if(depth != null){
			int uid = ++vectorizedRedirectUID;
			if(!bitwiseop.hasVectorizedRedirect()){
				bitwiseop.setVectorizedRedirect(remapVectorization(bitwiseop.getLine(), bitwiseop.getColumn(), bitwiseop, uid, bitwiseop.getShouldBePresevedOnStack(), depth, depth.size(), false, false, null));
			}		
		}
		else{
			super.visit(bitwiseop);
		}
		
		return null;
	}
	

	@Override
	public Object visit(Is instanceoOF) {
		ArrayList<Pair<Boolean, NullStatus>> depth = instanceoOF.depth;
		if(depth != null){
			int uid = ++vectorizedRedirectUID;
			if(!instanceoOF.hasVectorizedRedirect()){
				instanceoOF.setVectorizedRedirect(remapVectorization(instanceoOF.getLine(), instanceoOF.getColumn(), instanceoOF, uid, instanceoOF.getShouldBePresevedOnStack(), depth, depth.size(), false, false, null));
			}		
		}
		else{
			super.visit(instanceoOF);
		}
		
		return null;
	}
	
	@Override
	public Object visit(AndExpression andExpr) {
		ArrayList<Pair<Boolean, NullStatus>> depth = andExpr.depth;
		if(depth != null){
			this.visit((BooleanAndOrExpression)andExpr);	
		}
		else{
			super.visit(andExpr);
		}
		
		return null;
	}
	@Override
	public Object visit(OrExpression orExpr) {
		ArrayList<Pair<Boolean, NullStatus>> depth = orExpr.depth;
		if(depth != null){
			this.visit((BooleanAndOrExpression)orExpr);	
		}
		else{
			super.visit(orExpr);
		}
		
		return null;
	}
	
	public Object visit(BooleanAndOrExpression booleanAndOrExpr) {
		ArrayList<Pair<Boolean, NullStatus>> depth = booleanAndOrExpr.depth;
		int uid = ++vectorizedRedirectUID;
		if(!booleanAndOrExpr.hasVectorizedRedirect()){
			booleanAndOrExpr.setVectorizedRedirect(remapVectorization(booleanAndOrExpr.getLine(), booleanAndOrExpr.getColumn(), booleanAndOrExpr, uid, booleanAndOrExpr.getShouldBePresevedOnStack(), depth, depth.size(), false, false, null));
		}		
		
		return null;
	}
	
	@Override
	public Object visit(NotExpression notExpr) {
		ArrayList<Pair<Boolean, NullStatus>> depth = notExpr.depth;
		if(depth != null){
			int uid = ++vectorizedRedirectUID;
			if(!notExpr.hasVectorizedRedirect()){
				notExpr.setVectorizedRedirect(remapVectorization(notExpr.getLine(), notExpr.getColumn(), notExpr, uid, notExpr.getShouldBePresevedOnStack(), depth, depth.size(), false, false, null));
			}		
		}
		else{
			super.visit(notExpr);
		}
		
		return null;
	}
	
	

	@Override
	public Object visit(ElvisOperator elvisOperator) {
		ArrayList<Pair<Boolean, NullStatus>> depth = elvisOperator.depth;
		if(depth != null){
			int uid = ++vectorizedRedirectUID;
			if(!elvisOperator.hasVectorizedRedirect()){
				elvisOperator.setVectorizedRedirect(remapVectorization(elvisOperator.getLine(), elvisOperator.getColumn(), elvisOperator, uid, elvisOperator.getShouldBePresevedOnStack(), depth, depth.size(), false, false, null));
			}		
		}
		else{
			super.visit(elvisOperator);
		}
		
		return null;
	}
	
	@Override
	public Object visit(NotNullAssertion nnAssertion) {
		ArrayList<Pair<Boolean, NullStatus>> depth = nnAssertion.depth;
		if(depth != null){
			int uid = ++vectorizedRedirectUID;
			if(!nnAssertion.hasVectorizedRedirect()){
				nnAssertion.setVectorizedRedirect(remapVectorization(nnAssertion.getLine(), nnAssertion.getColumn(), nnAssertion, uid, nnAssertion.getShouldBePresevedOnStack(), depth, depth.size(), false, false, null));
			}		
		}
		else{
			super.visit(nnAssertion);
		}
		
		return null;
	}
	
	@Override
	public Object visit(ShiftExpression shiftExpr) {
		ArrayList<Pair<Boolean, NullStatus>> depth = shiftExpr.depth;
		if(depth != null){
			int uid = ++vectorizedRedirectUID;
			if(!shiftExpr.hasVectorizedRedirect()){
				shiftExpr.setVectorizedRedirect(remapVectorization(shiftExpr.getLine(), shiftExpr.getColumn(), shiftExpr, uid, shiftExpr.getShouldBePresevedOnStack(), depth, depth.size(), false, false, null));
			}		
		}
		else{
			super.visit(shiftExpr);
		}
		
		return null;
	}
	
	@Override
	public Object visit(EqReExpression eqREExpr) {
		ArrayList<Pair<Boolean, NullStatus>> depth = eqREExpr.depth;
		if(depth != null){
			int uid = ++vectorizedRedirectUID;
			if(!eqREExpr.hasVectorizedRedirect()){
				eqREExpr.setVectorizedRedirect(remapVectorization(eqREExpr.getLine(), eqREExpr.getColumn(), eqREExpr, uid, eqREExpr.getShouldBePresevedOnStack(), depth, depth.size(), false, false, null));
			}		
		}
		else{
			super.visit(eqREExpr);
		}
		
		return null;
	}

	@Override
	public Object visit(CastExpression castExpression) {
		
		if(castExpression.vectorizedExpr != null){
			int uid = ++vectorizedRedirectUID;
			if(!castExpression.hasVectorizedRedirect()){
				//int depth = castExpression.getTaggedType().getArrayLevels();
				int depth = castExpression.vectorizedExpr.size();
				castExpression.setVectorizedRedirect(remapVectorization(castExpression.getLine(), castExpression.getColumn(), castExpression, uid, castExpression.getShouldBePresevedOnStack(), castExpression.vectorizedExpr, depth, false, false, null));
			}		
		}
		else{
			super.visit(castExpression);
		}
		
		return null;
	}

	private Stack<Thruple<ArrayList<Expression>, Boolean, Boolean>> dopSoFarTmpVars = new Stack<Thruple<ArrayList<Expression>, Boolean, Boolean>>();//name, isdirect, isreturnself
	
	private boolean isThisOrSuper(Expression expr){
		return expr != null && (expr instanceof RefThis || expr instanceof RefSuper);
	}
	
	public Object visit(DotOperator dotOperator){
	
		ArrayList<Expression> ele = dotOperator.getElements(this);
		if(ele.stream().anyMatch(a -> a.hasBeenVectorized())){
			int uid = ++vectorizedRedirectUID;
			if(!dotOperator.hasVectorizedRedirect()){
				int line = dotOperator.getLine();
				int col = dotOperator.getColumn();
				
				Block vecMappedBlk = null;
				
				ArrayList<Expression> newElements = new ArrayList<Expression>();
				
				ArrayList<Boolean> directAccess = dotOperator.getIsDirectAccess(this);
				ArrayList<Boolean> newDirectAccess = new ArrayList<Boolean>();
				
				ArrayList<Boolean> returnAccess = dotOperator.returnCalledOn;
				ArrayList<Boolean> newReturnAccess = new ArrayList<Boolean>();
				
				ArrayList<Boolean> safeAccess = dotOperator.safeCall;
				ArrayList<Boolean> newSafeAccess = new ArrayList<Boolean>();

				int remapCnt = 0;
				Boolean isDirect=null;
				Boolean isReturn=null;
				Boolean isSafe=null;
				Boolean isunSafe=null;
				int sz = ele.size();
				Expression prevExpr = null;
				for(int n=0; n < sz; n++){//iterate through oirgonals until we arrive at an instance to vectorize
					Expression orig = ele.get(n);
					
					if(n != sz-1){
						isDirect = directAccess.get(n);
						isReturn = returnAccess.get(n);
						isSafe = safeAccess.get(n);
					}
					
					if(orig.hasBeenVectorized() && n > 0){
						vecMappedBlk = new Block(line, col);
						Expression lhs;
						if(newElements.size() == 1){
							lhs = newElements.get(0);
						}else{
							lhs = new DotOperator(line, col,  newElements, newDirectAccess, newReturnAccess, newSafeAccess);
						}
						
						
						ArrayList<Expression> prefixthingWith = new ArrayList<Expression>();
						if(isThisOrSuper(prevExpr)){
							prefixthingWith.add((Expression)prevExpr.copy());
						}else if(prevExpr.getTaggedType() instanceof ModuleType){
							prefixthingWith.addAll(ele.subList(0, n));
						}else{
							String dopSoFarTempVar = "vec$dop$" + uid + "$" + remapCnt++;
							vecMappedBlk.add(new AssignExisting(line, col, new RefName(line, col, dopSoFarTempVar), AssignStyleEnum.EQUALS, lhs));
							prefixthingWith.add(new RefName(line, col, dopSoFarTempVar));
						}
						
						dopSoFarTmpVars.add(new Thruple<ArrayList<Expression>, Boolean, Boolean>(prefixthingWith, newDirectAccess.get(n-1), newReturnAccess.get(n-1)));
						
						orig.accept(this);
						
						newElements = new ArrayList<Expression>();
						vecMappedBlk.add(new DuffAssign(orig));
						
						if(n != sz-1){
							((Node)vecMappedBlk).setShouldBePresevedOnStack(true);
						}
						
						
						newDirectAccess = new ArrayList<Boolean>();
						newReturnAccess = new ArrayList<Boolean>();
					}
					else{
						orig.accept(this);
						newElements.add(orig);
						if(null != isDirect){
							newDirectAccess.add(isDirect);
							newReturnAccess.add(isReturn);
							newSafeAccess.add(isSafe);
							if(n != sz-1){
								isDirect=isReturn=null;
							}
						}
					}
					
					prevExpr = orig;
				}
				
				Expression vecRedir = vecMappedBlk;
				if(!newElements.isEmpty()){//add remainder to a new dop
					if(null == vecMappedBlk){
						vecRedir = new DotOperator(line, col, newElements.remove(0), newElements, newDirectAccess, newReturnAccess, newSafeAccess);
					}else{
						vecRedir = new DotOperator(line, col, vecMappedBlk, newElements, newDirectAccess, newReturnAccess, newSafeAccess); 
					}
				}
				
				dotOperator.setVectorizedRedirect((Node)vecRedir);
				((Node)vecRedir).setShouldBePresevedOnStack(dotOperator.getShouldBePresevedOnStack());
				
				//newdop.setNewElements(this, newElements);
				//newdop.setNewDirectAccess(this, newElements);
				//newdop.setNewReturnAccess(this, newElements);
				
			}
					
			
		}else{
			super.visit(dotOperator);
		}
		
		return null;
	}

	//e.g. if we have proceesed a vectorized statement and then for some reason a nested vectorized statement still persists
	private class FindStrayVectorizees extends AbstractErrorRaiseVisitor {
		public FindStrayVectorizees(String fullPathFileName) {
			super(fullPathFileName);
		}

		@Override
		public Object visit(Vectorized vectorized) {
			this.raiseError(vectorized.getLine(), vectorized.getColumn(), "Expression cannot be vectorized at this location");
			return super.visit(vectorized);
		}
		
		@Override
		public Object visit(FuncInvokeArgs funcInvokeArgs) {
			//lastLineVisited=funcInvokeArgs.getLine();
			
			for(Expression e : funcInvokeArgs.getArgumentsWNPs() ){
				if(null != e) {
					e.accept(this);
				}
			}
			
			for(Pair<String, Object> thing : funcInvokeArgs.nameMap){
				((Expression)thing.getB()).accept(this);
			}
			
			return null;
		}
		
	}
	
	private LinkedHashSet<ErrorHolder> lastErrosFromStrayChecker = new LinkedHashSet<ErrorHolder>();
	
	@Override
	public Object visit(Block block) {
		FindStrayVectorizees finder = new FindStrayVectorizees(super.fullPathFileName);
		finder.visit(block);
		lastErrosFromStrayChecker = finder.getErrors();
		
		return super.visit(block);
	}
	
	public LinkedHashSet<ErrorHolder> getErrors() {
		LinkedHashSet<ErrorHolder> errs = super.getErrors();
		
		return errs.isEmpty()? lastErrosFromStrayChecker : errs;
	}
}
