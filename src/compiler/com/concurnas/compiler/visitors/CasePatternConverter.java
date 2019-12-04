package com.concurnas.compiler.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.concurnas.compiler.CaseExpression;
import com.concurnas.compiler.CaseExpressionAnd;
import com.concurnas.compiler.CaseExpressionAssign;
import com.concurnas.compiler.CaseExpressionAssignTuple;
import com.concurnas.compiler.CaseExpressionOr;
import com.concurnas.compiler.CaseExpressionPost;
import com.concurnas.compiler.CaseExpressionPre;
import com.concurnas.compiler.CaseExpressionTuple;
import com.concurnas.compiler.CaseExpressionWrapper;
import com.concurnas.compiler.JustAlsoCaseExpression;
import com.concurnas.compiler.TypedCaseExpression;
import com.concurnas.compiler.ast.AndExpression;
import com.concurnas.compiler.ast.Assign;
import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.ast.AssignStyleEnum;
import com.concurnas.compiler.ast.AssignTupleDeref;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.CaseOperatorEnum;
import com.concurnas.compiler.ast.CastExpression;
import com.concurnas.compiler.ast.DotOperator;
import com.concurnas.compiler.ast.DuffAssign;
import com.concurnas.compiler.ast.EqReExpression;
import com.concurnas.compiler.ast.FuncInvoke;
import com.concurnas.compiler.ast.GrandLogicalOperatorEnum;
import com.concurnas.compiler.ast.IfStatement;
import com.concurnas.compiler.ast.InExpression;
import com.concurnas.compiler.ast.Is;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.OrExpression;
import com.concurnas.compiler.ast.PrimativeType;
import com.concurnas.compiler.ast.RedirectableExpression;
import com.concurnas.compiler.ast.RefBoolean;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.visitors.util.ErrorRaiseableSupressErrors;
import com.concurnas.runtime.Pair;

public class CasePatternConverter extends AbstractVisitor {

	private static class RefsUsed extends AbstractVisitor{
		//JPT: enhance because the block could define its own version of the variable x and then it would be tagged as used anyway - minor improvement
		public HashSet<String> refnames = new HashSet<String>();
		
		@Override
		public Object visit(RefName refName){
			String name = refName.name;
			if(name.contains("$n")) {//yuck - if we have nested variable then the vriable assignment should be tagged with $n as well...
				name = name.substring(0, name.indexOf("$n"));
			}
			refnames.add(name);
			return null;
		}
	}
	
	private RefName ref;
	private Expression refExpression;
	private Type refType;
	private int tmpcnt;
	public Thruple<String, Type, String> varUsedInBlock;//temp name, type, usedname
	public AssignTupleDeref tuplevarUsedInBlock;
	
	private Block caseBlock;

	private CasePatternConverter prevCPC = null;
	
	public CasePatternConverter(RefName ref, Type refType, int tmpcnt, Block caseBlock){
		this.ref = ref;
		this.refExpression = ref;
		this.refType = refType;
		this.tmpcnt = tmpcnt;
		this.caseBlock = caseBlock;
	}
	
	private String getTempVar(){
		return "tmpVar$matchcase$" + tmpcnt;
	}
	
	@Override
	public Object visit(CaseExpressionWrapper caseExpressionWrapper) {
		Expression ret = new EqReExpression(caseExpressionWrapper.getLine(), caseExpressionWrapper.getColumn(), refExpression,  GrandLogicalOperatorEnum.EQ, caseExpressionWrapper.e);
		return ret;
	}

	private Expression processPrePost(int line, int col, CaseOperatorEnum ceo, Expression left, Expression right){
		switch(ceo){
			case EQ: return new EqReExpression(line, col, left,  GrandLogicalOperatorEnum.EQ, right); 
			case NEQ: return new EqReExpression(line, col, left,  GrandLogicalOperatorEnum.NE, right); 
			case REFEQ: return new EqReExpression(line, col, left,  GrandLogicalOperatorEnum.REFEQ, right); 
			case NREFEQ: return new EqReExpression(line, col, left,  GrandLogicalOperatorEnum.REFNE, right); 
		
			case LT: return new EqReExpression(line, col, left,  GrandLogicalOperatorEnum.LT, right); 
			case LTEQ: return new EqReExpression(line, col, left,  GrandLogicalOperatorEnum.LTEQ, right);
			case GT: return new EqReExpression(line, col, left,  GrandLogicalOperatorEnum.GT, right); 
			case GTEQ: return new EqReExpression(line, col, left,  GrandLogicalOperatorEnum.GTEQ, right);
			
			case IN: return new InExpression(line, col, left, right, false); 
			case NOTIN: return new InExpression(line, col, left, right, true); 
		}
		
	return null;//how get here?
	}
	
	@Override
	public Object visit(JustAlsoCaseExpression justAlsoCaseExpression){
		return justAlsoCaseExpression.alsoCondition;
	}
	
	@Override
	public Object visit(CaseExpressionPost caseExpressionPost) {
		return processPrePost(caseExpressionPost.getLine(), caseExpressionPost.getColumn(), caseExpressionPost.cop, caseExpressionPost.e, refExpression);
	}
	
	@Override
	public Object visit(CaseExpressionPre caseExpressionPre) {
		return processPrePost(caseExpressionPre.getLine(), caseExpressionPre.getColumn(), caseExpressionPre.cop, refExpression, caseExpressionPre.e);
	}
	
	private final ErrorRaiseable errorRaisableSupression = new ErrorRaiseableSupressErrors(null);
	
	private HashMap<String, RefName> varNametoRefName = new HashMap<String, RefName>();
	
	
	@Override
	public Object visit(CaseExpressionAssign caseExpressionAssign) {
		//x; x== 1
		int line = caseExpressionAssign.getLine();
		int col = caseExpressionAssign.getLine();
		
		RefsUsed ru = new RefsUsed();
		ru.visit(caseBlock);
		
		boolean isVarNameUsedInBlock=ru.refnames.contains(caseExpressionAssign.varname);
		String tmpVarName = null;
		
		Type leType = caseExpressionAssign.types==null || caseExpressionAssign.types.isEmpty()?null:TypeCheckUtils.getMoreGeneric(errorRaisableSupression,  null, line, col, caseExpressionAssign.types, null);
		
		if(isVarNameUsedInBlock){
			tmpVarName = getTempVar();
			varUsedInBlock = new Thruple<>(tmpVarName, null != leType?leType:refType, caseExpressionAssign.varname);
		}
		
		Node ret;
		if(null != leType){
			Type compareType = leType;
			if(compareType instanceof PrimativeType && !compareType.hasArrayLevels() && !(refType instanceof PrimativeType)){
				compareType = TypeCheckUtils.boxTypeIfPrimative(compareType, false);
			}
			ArrayList<Type> forInstanceof = new ArrayList<Type>();
			for(Type toIo : caseExpressionAssign.types){
				if(toIo instanceof PrimativeType && !toIo.hasArrayLevels() && !(refType instanceof PrimativeType)){
					toIo = TypeCheckUtils.boxTypeIfPrimative(toIo, false);
				}
				forInstanceof.add(toIo);
			}
			
			//if(a instanceof compareType){ x type = a as caseExpressionPost.type; stuff as normal }else{false}
			Block elseb = new Block(line, col);
			elseb.add(new LineHolder(new DuffAssign(new RefBoolean(line, col, false))));
			
			Block ifblock = new Block(line, col);
			
			CastExpression caste = new CastExpression(line, col, compareType, this.ref);
			
			if(null != caseExpressionAssign.expr){
				AssignNew an = new AssignNew(null, line, col, caseExpressionAssign.varname, leType, AssignStyleEnum.EQUALS, caste);
				an.isFinal = caseExpressionAssign.isFinal;
				ifblock.add(new LineHolder(an  ));
				
				if(isVarNameUsedInBlock){
					ifblock.add(new LineHolder(new AssignExisting(line, col, new RefName(tmpVarName), AssignStyleEnum.EQUALS, new RefName(caseExpressionAssign.varname) ) ));
				}
				
				ifblock.add(new LineHolder( new DuffAssign(caseExpressionAssign.expr)));
			}else{
				if(isVarNameUsedInBlock){
					ifblock.add(new LineHolder(new AssignExisting(line, col, new RefName(tmpVarName), AssignStyleEnum.EQUALS, caste ) ));
				}
				
				ifblock.add(new LineHolder(new DuffAssign(new RefBoolean(line, col, true))));
			}
		
			
			IfStatement ifstmt = new IfStatement(line, col, new Is(line, col, this.ref, forInstanceof, false), ifblock, null, elseb);
			
			ret= ifstmt;
		}else{//case(x; x==2)
			Block retx = new Block(line, col);
			retx.isolated=true;
			retx.setShouldBePresevedOnStack(true);
				
			if(caseExpressionAssign.isFinal){
				AssignNew an = new AssignNew(null, line, col, caseExpressionAssign.varname, this.refType, AssignStyleEnum.EQUALS, this.ref);
				an.isFinal = caseExpressionAssign.isFinal;
				retx.add(new LineHolder(an  ));
			}else{
				retx.add(new LineHolder(new AssignExisting(line, col, new RefName(caseExpressionAssign.varname), AssignStyleEnum.EQUALS, this.ref ) ));
			}
			
			if(isVarNameUsedInBlock){
				retx.add(new LineHolder(new AssignExisting(line, col, new RefName(tmpVarName), AssignStyleEnum.EQUALS, new RefName(caseExpressionAssign.varname) ) ));
			}
			
			if(null != caseExpressionAssign.expr){
				retx.add(new LineHolder( new DuffAssign(caseExpressionAssign.expr)));
			}
			else{
				retx.add(new LineHolder(new DuffAssign(new RefBoolean(line, col, true))));
			}
			
			ret= retx;
		}
		
		ret.setShouldBePresevedOnStack(true);
		
		return ret;
	}
	

	@Override
	public Object visit(CaseExpressionAssignTuple caseExpressionAssignTuple) {
		//(a int, b int, f, , g) => a
		int line = caseExpressionAssignTuple.getLine();
		int col = caseExpressionAssignTuple.getColumn();
		

		RefsUsed ru = new RefsUsed();
		ru.visit(caseBlock);
		
		ArrayList<Type> tupTypes = new ArrayList<Type>();
		boolean refNameUsed=false;
		for(Assign ass : caseExpressionAssignTuple.lhss) {
			Type tt = null;
			if(ass != null) {
				String name;
				if(ass instanceof AssignNew) {
					AssignNew an = (AssignNew)ass;
					tt = an.type;
					name = an.name;
				}else {
					tt = ScopeAndTypeChecker.const_object;
					AssignExisting ae = (AssignExisting)ass;
					name = ((RefName)ae.assignee).name;
				}
				
				if(ru.refnames.contains(name)) {
					refNameUsed=true;
				}
				
			}
			tupTypes.add(tt);
		}
		

		ArrayList<Pair<String, ArrayList<Type>>> nestorSegments = new ArrayList<Pair<String, ArrayList<Type>>>();
		NamedType tupleType = new NamedType(line, col, "Tuple" + tupTypes.size(), tupTypes, nestorSegments);

		Block elseb = new Block(line, col);
		elseb.add(new LineHolder(new DuffAssign(new RefBoolean(line, col, false))));
		
		Block ifblock = new Block(line, col);
		
		CastExpression caste = new CastExpression(line, col, tupleType, this.ref);
		
		AssignTupleDeref assign = new AssignTupleDeref(line, col, caseExpressionAssignTuple.lhss, AssignStyleEnum.EQUALS, caste) ;
		
		if(refNameUsed) {
			tuplevarUsedInBlock =assign; 
		}
		
		
		if(null != caseExpressionAssignTuple.expr){
			ifblock.add(new LineHolder( assign));
			ifblock.add(new LineHolder( new DuffAssign(caseExpressionAssignTuple.expr)));
		}else{
			ifblock.add(new LineHolder(assign ));
			ifblock.add(new LineHolder(new DuffAssign(new RefBoolean(line, col, true))));
		}
	
		
		IfStatement ifstmt = new IfStatement(line, col, new Is(line, col, this.ref, tupleType, false), ifblock, null, elseb);
		
		ifstmt.setShouldBePresevedOnStack(true);
		
		return ifstmt;
	}
	
	@Override
	public Object visit(TypedCaseExpression typedCaseExpression){//neat
		CaseExpressionAssign cea = new CaseExpressionAssign(typedCaseExpression.getLine(), typedCaseExpression.getColumn(), ref.name, typedCaseExpression.caseExpression==null?null:(Expression)typedCaseExpression.caseExpression.accept(this), typedCaseExpression.types, false, false);
		//cea.alsoCondition = typedCaseExpression.alsoCondition;
		return cea.accept(this);
	}

	@Override
	public Object visit(CaseExpressionAnd caseExpressionAnd) {
		ArrayList<RedirectableExpression> ands = new ArrayList<RedirectableExpression>();
		for(CaseExpression ce : caseExpressionAnd.caseAnds){
			Expression got = (Expression)ce.accept(this);
			ands.add(new RedirectableExpression(got));
		}
		
		return new AndExpression(caseExpressionAnd.getLine(), caseExpressionAnd.getColumn(), (Expression)caseExpressionAnd.head.accept(this), ands);
	}
	
	@Override
	public Object visit(CaseExpressionTuple caseExprTuple) {
		ArrayList<RedirectableExpression> ands = new ArrayList<RedirectableExpression>();
		int n=0;
		for(CaseExpression ce : caseExprTuple.getComponents()){
			if(null != ce) {
				int line = ce.getLine();
				int col = ce.getColumn();
				Expression prevRef = refExpression;
				refExpression =  DotOperator.buildDotOperatorOneNonDirect(line, col, ref, FuncInvoke.makeFuncInvoke(line, col, "getF" + n) );
				Expression got = (Expression)ce.accept(this);
				refExpression = prevRef;
				ands.add(new RedirectableExpression(got));
			}
			n++;
		}
		
		
		
		if(null != caseExprTuple.caseExpression) {
			RedirectableExpression toAdd;
			if(null != tuplevarUsedInBlock) {
				Block blk = new Block(caseExprTuple.getLine(), caseExprTuple.getColumn());
				blk.add((tuplevarUsedInBlock));
				blk.add(new DuffAssign(caseExprTuple.caseExpression));
				toAdd = new RedirectableExpression(blk);
			}else {
				toAdd = new RedirectableExpression( caseExprTuple.caseExpression);
			}
			
			ands.add(toAdd );
		}
		
		if(ands.isEmpty()) {//e.g. (a, b)
			return new RefBoolean(caseExprTuple.getLine(), caseExprTuple.getColumn(), true);
		}else {
			Expression head = ands.remove(0);
			if(ands.isEmpty()) {
				return head;
			}else {
				return new AndExpression(caseExprTuple.getLine(), caseExprTuple.getColumn(), head, ands);
			}
		}
	}

	
	@Override
	public Object visit(CaseExpressionOr caseExpressionOr) {

		ArrayList<RedirectableExpression> ands = new ArrayList<RedirectableExpression>();
		for(CaseExpression ce : caseExpressionOr.caseOrs){
			Expression got = (Expression)ce.accept(this);
			ands.add(new RedirectableExpression(got));
		}
		
		return new OrExpression(caseExpressionOr.getLine(), caseExpressionOr.getColumn(), (Expression)caseExpressionOr.head.accept(this), ands);
	}
	
	
	
}
