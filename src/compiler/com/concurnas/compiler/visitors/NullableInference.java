package com.concurnas.compiler.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import com.concurnas.compiler.ast.AndExpression;
import com.concurnas.compiler.ast.AssertStatement;
import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.ast.AsyncBlock;
import com.concurnas.compiler.ast.AsyncBodyBlock;
import com.concurnas.compiler.ast.AsyncRefRef;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.BreakStatement;
import com.concurnas.compiler.ast.CatchBlocks;
import com.concurnas.compiler.ast.DotOperator;
import com.concurnas.compiler.ast.ElifUnit;
import com.concurnas.compiler.ast.EqReExpression;
import com.concurnas.compiler.ast.FuncInvoke;
import com.concurnas.compiler.ast.GrandLogicalElement;
import com.concurnas.compiler.ast.GrandLogicalOperatorEnum;
import com.concurnas.compiler.ast.IfStatement;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.NotExpression;
import com.concurnas.compiler.ast.NullStatus;
import com.concurnas.compiler.ast.OnChange;
import com.concurnas.compiler.ast.OrExpression;
import com.concurnas.compiler.ast.RedirectableExpression;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.RefThis;
import com.concurnas.compiler.ast.TryCatch;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.VarNull;
import com.concurnas.compiler.ast.WhileBlock;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.runtime.Pair;

public class NullableInference extends AbstractErrorRaiseVisitor {

	public NullableInference(String fullPathFileName) {
		super(fullPathFileName);
	}

	private boolean hadMadeRepoints = false;
	
	public boolean hadMadeRepoints(){
		return this.hadMadeRepoints;
	}
	
	public void restart(){
		 hadMadeRepoints=false;
	}
	
	
	
	
	private class TestedNonNullAndNull{
		public boolean inverted;
		public final boolean signleAssertion;

		public TestedNonNullAndNull(boolean inverted, boolean signleAssertion) {
			this.inverted = inverted;
			this.signleAssertion = signleAssertion;
		}
		
		public TestedNonNullAndNull() {
			this(false, false);
		}
		
		public String toString() {
			return String.format("Null: %s Not Null: %s\nInverse: Null: %s Not Null: %s", assertedNullAndNotNull.getA(), assertedNullAndNotNull.getB(), assertedNullAndNotNull_inverse.getA(), assertedNullAndNotNull_inverse.getB());
		}
		
		public Pair<HashSet<Pair<String, Boolean>>, HashSet<Pair<String, Boolean>>> assertedNullAndNotNull = new Pair<>(new HashSet<Pair<String, Boolean>>(), new HashSet<Pair<String, Boolean>>());
		private Pair<HashSet<Pair<String, Boolean>>, HashSet<Pair<String, Boolean>>> assertedNullAndNotNull_inverse = new Pair<>(new HashSet<Pair<String, Boolean>>(), new HashSet<Pair<String, Boolean>>());
		
		public TestedNonNullAndNull getInverted() {
			TestedNonNullAndNull ret = new TestedNonNullAndNull(!inverted, signleAssertion);
			ret.assertedNullAndNotNull = assertedNullAndNotNull_inverse;
			ret.assertedNullAndNotNull_inverse = assertedNullAndNotNull;
			
			return ret;
		}
		
		
		public HashMap<Pair<String, Boolean>, NullStatus> getAssertions() {
			//assertedNullAndNotNull - ignore if in both
			HashMap<Pair<String, Boolean>, NullStatus> ret = new HashMap<Pair<String, Boolean>, NullStatus>();
			
			HashSet<Pair<String, Boolean>> assertNull = assertedNullAndNotNull.getA();
			HashSet<Pair<String, Boolean>> assertNotNull = assertedNullAndNotNull.getB();
			
			for(Pair<String, Boolean> assNull : assertNull) {
				if(!assertNotNull.contains(assNull)) {
					ret.put(assNull, NullStatus.NULLABLE);
				}
			}
			
			for(Pair<String, Boolean> assNull : assertNotNull) {
				if(!assertNull.contains(assNull)) {
					ret.put(assNull, NullStatus.NOTNULL);
				}
			}
			
			return ret;
		}

		public void applyAssertions(HashMap<Pair<String, Boolean>, NullStatus> addTo) {
			// apply assertions to block
			HashMap<Pair<String, Boolean>, NullStatus> toAdd = getAssertions();
			for(Pair<String, Boolean> asserted :  toAdd.keySet()) {
				NullStatus assertedv = toAdd.get(asserted);
				//if(addTo.containsKey(asserted) && !addTo.get(asserted).equals(assertedv)) {
				//	continue;
				//}
				addTo.put(asserted, assertedv);
			}
			
		}

		public void assertedNull(Pair<String, Boolean> pair) {
			if(inverted) {
				assertedNullAndNotNull_inverse.getB().add(pair);
			}else {
				assertedNullAndNotNull.getA().add(pair);
				if(this.signleAssertion) {
					assertedNullAndNotNull_inverse.getB().add(pair);
				}
			}
		}

		public void assertedNotNull(Pair<String, Boolean> pair) {
			if(inverted) {
				assertedNullAndNotNull_inverse.getA().add(pair);
			}else {
				assertedNullAndNotNull.getB().add(pair);
				if(this.signleAssertion) {
					assertedNullAndNotNull_inverse.getA().add(pair);
				}
			}
		}

		public void addAssertions(TestedNonNullAndNull other) {
			for(Pair<String, Boolean> rf : other.assertedNullAndNotNull.getA()) {
				this.assertedNull(rf);
			}
			
			for(Pair<String, Boolean> rf : other.assertedNullAndNotNull.getB()) {
				this.assertedNotNull(rf);
			}
		}
		
		public void addAssertions(HashMap<Pair<String, Boolean>, NullStatus> otherMapping) {
			if(null != otherMapping) {
				for(Pair<String, Boolean> key : otherMapping.keySet()) {
					NullStatus ns = otherMapping.get(key);
					if(ns == NullStatus.NOTNULL) {
						this.assertedNotNull(key);
					}else if(ns == NullStatus.NULLABLE) {
						this.assertedNull(key);
					}
				}
			}
		}
		
		private TestedNonNullAndNull join(TestedNonNullAndNull another, boolean union) {
			TestedNonNullAndNull ret = new TestedNonNullAndNull();
			
			HashSet<Pair<String, Boolean>> assertedNull = this.assertedNullAndNotNull.getA();
			HashSet<Pair<String, Boolean>> assertedNotNull = this.assertedNullAndNotNull.getB();

			HashSet<Pair<String, Boolean>> assertedNullOther = another.assertedNullAndNotNull.getA();
			HashSet<Pair<String, Boolean>> assertedNotNullOther = another.assertedNullAndNotNull.getB();
			
			
			
			HashSet<Pair<String, Boolean>> newNull = new HashSet<Pair<String, Boolean>>(assertedNull);
			HashSet<Pair<String, Boolean>> newNotNull = new HashSet<Pair<String, Boolean>>(assertedNotNull);
			
			if(union) {
				newNull.addAll(assertedNullOther);
				newNotNull.addAll(assertedNotNullOther);
			}else {//intersection

				//if(a==null or b == null){} else{ }//a <> null, b <> null in else block
				
				{
					HashSet<Pair<String, Boolean>> assertedNullOtherInverted = another.assertedNullAndNotNull_inverse.getA();
					HashSet<Pair<String, Boolean>> assertedNotNullOtherInverted = another.assertedNullAndNotNull_inverse.getB();
					
					HashSet<Pair<String, Boolean>> newNullInverted = new HashSet<Pair<String, Boolean>>(this.assertedNullAndNotNull_inverse.getA());
					HashSet<Pair<String, Boolean>> newNotNullInverted = new HashSet<Pair<String, Boolean>>(this.assertedNullAndNotNull_inverse.getB());
					
					ret.inverted=true;
					
					newNullInverted.addAll(assertedNullOtherInverted);
					newNotNullInverted.addAll(assertedNotNullOtherInverted);
					
					for(Pair<String, Boolean> inst : newNullInverted) {
						ret.assertedNotNull(inst);
					}
					
					for(Pair<String, Boolean> inst : newNotNullInverted) {
						ret.assertedNull(inst);
					}
					
					ret.inverted=false;
				}
				
				newNull.retainAll(assertedNullOther);
				newNotNull.retainAll(assertedNotNullOther);
			}
			
			for(Pair<String, Boolean> inst : newNull) {
				ret.assertedNull(inst);
			}
			
			for(Pair<String, Boolean> inst : newNotNull) {
				ret.assertedNotNull(inst);
			}
			
			return ret;
		}
		
		public TestedNonNullAndNull intersection(TestedNonNullAndNull another) {//{A, B}, {A, C} => {A}
			return join(another, false);
		}
		
		public TestedNonNullAndNull union(TestedNonNullAndNull another) {//{A, B}, {A, C} => {A, B, C}
			return join(another, true);
		}
		

		public TestedNonNullAndNull unionIfnotPresent(TestedNonNullAndNull other) {
			
			TestedNonNullAndNull ret = new TestedNonNullAndNull();
			
			HashSet<Pair<String, Boolean>> currentNull = this.assertedNullAndNotNull.getA();
			HashSet<Pair<String, Boolean>> currentNonNull = this.assertedNullAndNotNull.getB();

			for(Pair<String, Boolean> inst : currentNull) {
				ret.assertedNull(inst);
			}
			
			for(Pair<String, Boolean> inst : currentNonNull) {
				ret.assertedNotNull(inst);
			}
			
			for(Pair<String, Boolean> otherNull : other.assertedNullAndNotNull.getA()) {
				if(!currentNull.contains(otherNull) && !currentNonNull.contains(otherNull)) {
					ret.assertedNull(otherNull);
				}
			}
			
			for(Pair<String, Boolean> otherNotNull : other.assertedNullAndNotNull.getB()) {
				if(!currentNull.contains(otherNotNull) && !currentNonNull.contains(otherNotNull)) {
					ret.assertedNotNull(otherNotNull);
				}
			}
			
			return ret;
		}
	}
	
	private  class NullableFinder extends AbstractVisitor{
		private Stack<HashMap<Pair<String, Boolean>, NullStatus>> shortCircuited = new Stack<HashMap<Pair<String, Boolean>, NullStatus>>();
		
		@Override
		public Object visit(AndExpression andExpression) {
			shortCircuited.add(new HashMap<Pair<String, Boolean>, NullStatus>());
			TestedNonNullAndNull ret = processExpression(andExpression.head);
			for (Expression i : andExpression.things) {
				ret = ret.union(processExpression(i));
			}//TODO: add shortcutting
			shortCircuited.pop();
			return ret;
		}
		

		@Override
		public Object visit(RefName refName) {
			if(null != refName.nameAndLocKey) {
				if(!shortCircuited.isEmpty()) {
					NullStatus ns = shortCircuited.peek().get(refName.nameAndLocKey);
					if(ns != null) {
						refName.inferNonNullable = ns == NullStatus.NOTNULL;
					}
				}
				
				
			}
			
			return null;
		}	
		
		@Override
		public Object visit(OrExpression orExpression) {
			TestedNonNullAndNull ret = processExpression(orExpression.head);
			for (Expression i : orExpression.things) {
				ret = ret.intersection(processExpression(i));
			}
			return ret;
		}
		
		
		private void addNonNull(Pair<String, Boolean> refnameKey) {
			if(!shortCircuited.isEmpty()) {
				shortCircuited.peek().put(refnameKey, NullStatus.NOTNULL);
			}
		}
		
		@Override
		public TestedNonNullAndNull visit(EqReExpression equalityExpression) {
			TestedNonNullAndNull ret = new TestedNonNullAndNull(false, true);
			super.visit(equalityExpression);
			//TODO: bug where if(xyz and thing <> null)...else{}//cannot assuming thing is not null in else block unless on own
			Expression lhsExpression = equalityExpression.head;
			boolean lhsVarNull = lhsExpression instanceof VarNull;
			boolean lhsNullable = typeIsNullable(lhsExpression);
			lhsExpression = maybeResolvesToRefName(lhsExpression, this);
			for(GrandLogicalElement e: equalityExpression.elements)
			{
				Expression rhsExpression = e.e2;
				boolean rhsVarNull = rhsExpression instanceof VarNull;
				boolean rhsNullable = typeIsNullable(rhsExpression);
				rhsExpression = maybeResolvesToRefName(rhsExpression, this);
				
				if((rhsNullable && lhsVarNull) || (lhsNullable && rhsVarNull)) {
					if(e.compOp == GrandLogicalOperatorEnum.NE || e.compOp == GrandLogicalOperatorEnum.REFNE) {//certainly not null!
						if(rhsNullable && rhsExpression instanceof RefName) {//rhs vartoadd to non nullable
							RefName asRefName = (RefName)rhsExpression;
							addNonNull(asRefName.nameAndLocKey);
							//asRef.inferNonNullable = true;
							ret.assertedNotNull(asRefName.nameAndLocKey);
						}else if(lhsExpression instanceof RefName) {//lhs vartoadd to non nullable
							RefName asRefName = (RefName)lhsExpression;
							addNonNull(asRefName.nameAndLocKey);
							//asRef.inferNonNullable = true;
							ret.assertedNotNull(asRefName.nameAndLocKey);
						}
					}else if(e.compOp == GrandLogicalOperatorEnum.EQ || e.compOp == GrandLogicalOperatorEnum.REFEQ) {//certainly null!
						if(rhsNullable && rhsExpression instanceof RefName) {//rhs vartoadd to non nullable
							RefName asRefName = (RefName)rhsExpression;
							ret.assertedNull(asRefName.nameAndLocKey);
						}else if(lhsExpression instanceof RefName) {//lhs vartoadd to non nullable
							RefName asRefName = (RefName)lhsExpression;
							ret.assertedNull(asRefName.nameAndLocKey);
						}
					}
				}
				
				lhsVarNull = rhsVarNull;
				lhsNullable = rhsNullable;
				lhsExpression = rhsExpression;
			}
			
			return ret;
		}
		
		
		
		private boolean typeIsNullable(Expression expr) {
			if(expr instanceof RedirectableExpression) {
				expr = ((RedirectableExpression)expr).exp;
			}
			
			/*
			 * if(expr instanceof RefName) { if(((RefName)expr).inferNonNullable) { return
			 * true; } }
			 */
			
			Type what = expr.getTaggedType();
			return what != null && what.getNullStatus() == NullStatus.NULLABLE;
		}
		
		public TestedNonNullAndNull processExpression(Expression test) {
			TestedNonNullAndNull ret = null;
			if(test instanceof RefName) {
				RefName asRefName = (RefName)test;
				if( typeIsNullable(test)) {
					//asRefName.inferNonNullable = true;
					//HashMap<String, RefName> nonNull = new HashMap<String, RefName>();
					//nonNull.put(asRefName.name, asRefName);
					//ret = new TestedNonNullAndNull();
					ret=new TestedNonNullAndNull(false, true);
					ret.assertedNotNull(asRefName.nameAndLocKey);
				}else {
					ret = new TestedNonNullAndNull();
				}
			}else if(test instanceof AsyncRefRef) {
				ret = processExpression(((AsyncRefRef)test).b);
			}else if(test instanceof RedirectableExpression) {
				ret = processExpression(((RedirectableExpression)test).exp);
			}else if(test instanceof DotOperator) {//shouldn't this process the previous keys to last?
				ret = processExpression(((DotOperator)test).getLastElement());
			}else if(test instanceof NotExpression){
				TestedNonNullAndNull toinvert = processExpression(((NotExpression)test).expr);
				ret= toinvert.getInverted();
			}else {
				ret = (TestedNonNullAndNull)test.accept(this);
				if(null == ret) {
					ret = new TestedNonNullAndNull();
				}
			}
			
			return ret;
		}
	}

	private final NullableFinder nullableTypeLogic = new NullableFinder();
	
	private NullStatus nullStatusInferedAsOp(Pair<String, Boolean> key) {
		NullStatus inferedAs = null;
		for(int n = this.nullableOverwriteType.size() - 1;  n >=0; n--){
			HashMap<Pair<String, Boolean>, NullStatus> level = this.nullableOverwriteType.get(n);
			if(null != level && level.containsKey(key)){
				inferedAs = level.get(key);
				break;
			}
		}
		return inferedAs;
	}
	
	private NullStatus nullStatusInferedAs(Pair<String, Boolean> key) {
		NullStatus inferedAs = null;
		if(key != null) {
			inferedAs = nullStatusInferedAsOp(key);
			if(null == inferedAs) {
				String fname = key.getA();
				if(fname.contains("$n")) {
					key = new Pair<String, Boolean>(fname.substring(0, fname.lastIndexOf("$n")), key.getB());
					inferedAs = nullStatusInferedAsOp(key);
				}
			}
		}
		return inferedAs;
	}
	
	///////////////////////visits//////////////////////////
	
	//refName
	//funcInvoke - where lambda...
	
	private Stack<HashMap<Pair<String, Boolean>, NullStatus>> nullableOverwriteType = new Stack<HashMap<Pair<String, Boolean>, NullStatus>>();
	
	@Override
	public Object visit(AssertStatement assertStatement) {
		super.visit(assertStatement);
		TestedNonNullAndNull testNullables = nullableTypeLogic.processExpression(assertStatement.e);
		
		testNullables.applyAssertions(nullableOverwriteType.peek());
		
		return null;
	}
	
	

	@Override
	public Object visit(AssignNew assignNew) {
		//lastLineVisited=assignNew.getLine();
		if(assignNew.annotations != null){
			assignNew.annotations.accept(this);
		}
		
		if(assignNew.type!=null) assignNew.type.accept(this);
		if(assignNew.expr!=null) {
			assignNew.expr.accept(this);
			Type rhsType = assignNew.expr.getTaggedType();
			if(!assignNew.isClassField) {//class fields can always be set to be nullable post initial assign
				if(!assignNew.skipNullableCheck && null != rhsType && !assignNew.isSharedVariableStrict()) {
					NullStatus ns = rhsType.getNullStatus();
					//if(ns != NullStatus.UNKNOWN) {
						Pair<String, Boolean> key = new Pair<String, Boolean>(assignNew.name, false);
						nullableOverwriteType.peek().put(key, ns);
					//}
				}
			}
		}
		
		return null;
	}
	
	private static Expression maybeResolvesToRefName(Expression rhs, Visitor vis) {
		//refname or this.refname
		if(rhs instanceof DotOperator) {
			DotOperator asDot = (DotOperator)rhs;
			ArrayList<Expression> elems = asDot.getElements(vis);
			if(elems.size() == 2) {
				Expression e1 = elems.get(0);
				Expression e2 = elems.get(1);
				if(e1 instanceof RefThis) {
					e2 = maybeResolvesToRefName(e2, vis);
					if(e2 instanceof RefName) {
						return e2;
					}
				}
			}
		}else if(rhs instanceof AsyncRefRef) {
			return maybeResolvesToRefName(((AsyncRefRef)rhs).b, vis);
		}
		
		return rhs;
	}
	
	@Override
	public Object visit(AssignExisting assignExisting) {
		//lastLineVisited=assignExisting.getLine();
		if(assignExisting.annotations != null){
			assignExisting.annotations.accept(this);
		}
		
		if(!(maybeResolvesToRefName(assignExisting.assignee, this) instanceof RefName)){
			assignExisting.assignee.accept(this);
		}
		
		if(null != assignExisting.expr) {
			assignExisting.expr.accept(this);
		}
		
		
		
		if(assignExisting.eq != null && assignExisting.eq.isEquals() && !assignExisting.isShared && !assignExisting.isDefinedAtClassLevelOrModuleLevel ){
			Type rhs = assignExisting.expr.getTaggedType();
			
			if(rhs != null) {
				NullStatus ns = rhs.getNullStatus();
				//if(ns != NullStatus.UNKNOWN) {
					Expression opon = assignExisting.assignee;
					
					if(opon instanceof DotOperator) {//this.thing
						DotOperator dop = (DotOperator)opon;
						ArrayList<Expression> elms = dop.getElements(this);
						int sz = elms.size();
						if(sz == 2) {
							Expression last = elms.get(1);
							Expression penul = elms.get(0);
							if(penul instanceof RefThis) {
								if(last instanceof RefName) {
									opon = last;
								}
							}
						}
					}
					
					if(opon instanceof RefName) {
						RefName asrefname = (RefName)opon;
						
						if(!(asrefname.resolvesTo != null && asrefname.resolvesTo.getLocation() != null && asrefname.resolvesTo.getLocation().isShared())) {
							Pair<String, Boolean> key =  asrefname.nameAndLocKey;
							if(!this.nullableOverwriteType.isEmpty() && key != null) {
								nullableOverwriteType.peek().put(key, ns);
							}
						}
					}
				//}
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
		
		if(funcInvoke.nameAndLocKey != null) {
			NullStatus inferedAs = null;
			
			if(!this.nullableOverwriteType.isEmpty()){
				inferedAs = nullStatusInferedAs(funcInvoke.nameAndLocKey);
			}
			
			
			boolean nullableLambda = inferedAs == null || inferedAs == NullStatus.NULLABLE;
			
			if(nullableLambda) {
				this.raiseError(funcInvoke.getLine(), funcInvoke.getColumn(), String.format("Lamdba: %s is nullable and may be null" , funcInvoke.funName));
			}
		}
			
		
		//invalidable non null status of all class variables
		//e.g. def nullable(){	aString = "ok";	foo();	aString.length()//aString might be nullable }
		for(HashMap<Pair<String, Boolean>, NullStatus> level : this.nullableOverwriteType) {
			HashSet<Pair<String, Boolean>> levelrem = null;
			for(Pair<String, Boolean> key : level.keySet()) {
				NullStatus ns = level.get(key);
				if( ns == NullStatus.NOTNULL && key.getB()) {
					if(null == levelrem) {
						levelrem = new HashSet<Pair<String, Boolean>>();
					}
					levelrem.add(key);
				}
			}
			
			if(null != levelrem) {
				levelrem.forEach(a -> level.remove(a));
			}
		}
		
		return null;
	}
	
	@Override
	public Object visit(Block block) {
		nullableOverwriteType.push(new HashMap<Pair<String, Boolean>, NullStatus>());
		//lastLineVisited=block.getLine();
		LineHolder lh = block.startItr();
		
		while(lh != null)
		{
			lh.accept(this);
			lh = block.getNext();
		}
		
		HashMap<Pair<String, Boolean>, NullStatus> infered = nullableOverwriteType.pop();
		
		if(block.isolated && !block.isAsyncBlock && !block.isAsyncBody) {
			nullableOverwriteType.peek().putAll(infered);
		}else {
			block.inferedNullability = infered;
		}
		
		return null;
	}
	
	
	@Override
	public Object visit(IfStatement ifStatement) {

		nullableOverwriteType.push(new HashMap<Pair<String, Boolean>, NullStatus>());
		ifStatement.iftest.accept(this);
		TestedNonNullAndNull testNullables = nullableTypeLogic.processExpression(ifStatement.iftest);
		TestedNonNullAndNull mainTestInvertedAssertions = testNullables.getInverted();
		nullableOverwriteType.pop();
		
		HashMap<Pair<String, Boolean>, NullStatus> inFitBlock = new HashMap<Pair<String, Boolean>, NullStatus>();
		testNullables.applyAssertions(inFitBlock);
		nullableOverwriteType.push(inFitBlock);
		ifStatement.ifblock.accept(this);
		nullableOverwriteType.pop();

		ArrayList<TestedNonNullAndNull> prevelifInvertions = new ArrayList<TestedNonNullAndNull>();
		HashMap<ElifUnit, TestedNonNullAndNull> elifToTestedNonNull = new HashMap<ElifUnit, TestedNonNullAndNull>();
		for(ElifUnit elifUnit : ifStatement.elifunits)
		{
			nullableOverwriteType.push(new HashMap<Pair<String, Boolean>, NullStatus>());
			elifUnit.eliftest.accept(this);
			TestedNonNullAndNull eliftestNullables = nullableTypeLogic.processExpression(elifUnit.eliftest);
			elifToTestedNonNull.put(elifUnit, eliftestNullables);
			nullableOverwriteType.pop();
			
			HashMap<Pair<String, Boolean>, NullStatus> inFitBlockelif = new HashMap<Pair<String, Boolean>, NullStatus>();
			nullableOverwriteType.push(inFitBlockelif);
			
			eliftestNullables.applyAssertions(inFitBlockelif);
			for(TestedNonNullAndNull previnverts : prevelifInvertions) {
				previnverts.applyAssertions(inFitBlockelif);
			}
			prevelifInvertions.add(eliftestNullables.getInverted());
			
			mainTestInvertedAssertions.applyAssertions(inFitBlockelif);//determined null from if test
			elifUnit.elifb.accept(this);
			nullableOverwriteType.pop();	
		}
		if(ifStatement.elseb!=null)
		{
			HashMap<Pair<String, Boolean>, NullStatus> inelseBlock = new HashMap<Pair<String, Boolean>, NullStatus>();
			mainTestInvertedAssertions.applyAssertions(inelseBlock);
			for(TestedNonNullAndNull previnverts : prevelifInvertions) {
				previnverts.applyAssertions(inelseBlock);
			}
			
			nullableOverwriteType.push(inelseBlock);
			ifStatement.elseb.accept(this);
			nullableOverwriteType.pop();
		}
		
		
		HashMap<Pair<String, Boolean>, NullStatus> inferedNullinBlock = ifStatement.ifblock.inferedNullability;
		
		if (null == ifStatement.elseb && ifStatement.elifunits.isEmpty()) {
			TestedNonNullAndNull stateAfterBranch = new TestedNonNullAndNull();
			
			if(ifStatement.ifblock.defoReturns || ifStatement.ifblock.hasDefoThrownException) {
				TestedNonNullAndNull invertedFromTest = testNullables.getInverted();
				stateAfterBranch.addAssertions(invertedFromTest);
			}else {
				stateAfterBranch.addAssertions(inferedNullinBlock);
				stateAfterBranch = stateAfterBranch.intersection(testNullables.getInverted());
			}

			stateAfterBranch.applyAssertions(nullableOverwriteType.peek());
			
		}else {//elif, else...
			TestedNonNullAndNull stateAfterBranch = new TestedNonNullAndNull();

			TestedNonNullAndNull invertedFromTest = testNullables.getInverted();
			if(ifStatement.ifblock.defoReturns || ifStatement.ifblock.hasDefoThrownException) {
				stateAfterBranch.addAssertions(invertedFromTest);
			}else {
				TestedNonNullAndNull combinedIf = new TestedNonNullAndNull();
				combinedIf.addAssertions(inferedNullinBlock);
				stateAfterBranch = combinedIf.unionIfnotPresent(testNullables);
			}
			
			for(ElifUnit eu : ifStatement.elifunits) {
				{
					if(eu.elifb.defoReturns || eu.elifb.hasDefoThrownException) {
						stateAfterBranch.addAssertions(elifToTestedNonNull.get(eu).getInverted());
					}else {
						TestedNonNullAndNull combinedIf = new TestedNonNullAndNull();
						combinedIf.addAssertions(eu.elifb.inferedNullability);
						stateAfterBranch = combinedIf.unionIfnotPresent(elifToTestedNonNull.get(eu));
					}
				}
			}
			
			if (null != ifStatement.elseb){
				{
					if(ifStatement.elseb.defoReturns || ifStatement.elseb.hasDefoThrownException) {
						//ignore if defo returns
					}else {
						TestedNonNullAndNull elsePostState = new TestedNonNullAndNull();
						elsePostState.addAssertions(ifStatement.elseb.inferedNullability);
						elsePostState = elsePostState.unionIfnotPresent(testNullables.getInverted());
						
						stateAfterBranch = stateAfterBranch.intersection(elsePostState);
					}
				}
			}
			

			stateAfterBranch.applyAssertions(nullableOverwriteType.peek());
			
			/*
			 * if(!inferedNullinBlocks.isEmpty()) { Pair<List<Pair<String, Boolean>>,
			 * List<Pair<String, Boolean>>> nonnullAndNullable =
			 * Utils.filterNullInferMap(inferedNullinBlocks); HashMap<Pair<String, Boolean>,
			 * NullStatus> addto = this.nullableOverwriteType.peek();
			 * 
			 * if(!fail) { for(Pair<String, Boolean> nonNull : nonnullAndNullable.getA()) {
			 * addto.put(nonNull, NullStatus.NOTNULL); } }
			 * 
			 * for(Pair<String, Boolean> nullableNull : nonnullAndNullable.getB()) {
			 * addto.put(nullableNull, NullStatus.NULLABLE); } }
			 */
			
			/*
			boolean fail = false;
			
			List<HashMap<Pair<String, Boolean>, NullStatus>> inferedNullinBlocks = new ArrayList<HashMap<Pair<String, Boolean>, NullStatus>>();
			
			
			if (null != inferedNullinBlock && !inferedNullinBlock.isEmpty()) {
				inferedNullinBlocks.add(inferedNullinBlock);
			}else {
				fail=true;
			}
			
			for(ElifUnit eu : ifStatement.elifunits) {
				if (null != eu.elifb.inferedNullability && !eu.elifb.inferedNullability.isEmpty()) {
					inferedNullinBlocks.add(eu.elifb.inferedNullability);
				}else {
					fail=true;
				}
			}
			
			if (null != ifStatement.elseb){
				if (null != ifStatement.elseb.inferedNullability && !ifStatement.elseb.inferedNullability.isEmpty()) {
					inferedNullinBlocks.add(ifStatement.elseb.inferedNullability);
				}else {
					fail=true;
				}
			}
			
			if(!inferedNullinBlocks.isEmpty()) {
				Pair<List<Pair<String, Boolean>>, List<Pair<String, Boolean>>> nonnullAndNullable = Utils.filterNullInferMap(inferedNullinBlocks);
				HashMap<Pair<String, Boolean>, NullStatus> addto = this.nullableOverwriteType.peek();
				
				if(!fail) {
					for(Pair<String, Boolean> nonNull : nonnullAndNullable.getA()) {
						addto.put(nonNull, NullStatus.NOTNULL);
					}
				}
				
				for(Pair<String, Boolean> nullableNull : nonnullAndNullable.getB()) {
					addto.put(nullableNull, NullStatus.NULLABLE);
				}
			}*/
		}
		
		return null;
		
	}
	
	private static class WhileBreakFinder extends AbstractVisitor{
		@Override
		public Object visit(BreakStatement breakStatement) {
			super.visit(breakStatement);
			hasbreak=true;
			return null;
		}
		
		private boolean hasbreak = false;
		
		public boolean containsBreak(Block whileblokc) {
			super.visit(whileblokc);
			return hasbreak;
		}
		
	}
	
	
	@Override
	public Object visit(WhileBlock whileBlock) {
		//lastLineVisited=whileBlock.getLine();
		nullableOverwriteType.push(new HashMap<Pair<String, Boolean>, NullStatus>());
		whileBlock.cond.accept(this);
		TestedNonNullAndNull testNullables = nullableTypeLogic.processExpression(whileBlock.cond);
		nullableOverwriteType.pop();
		
		HashMap<Pair<String, Boolean>, NullStatus> inFitBlock = new HashMap<Pair<String, Boolean>, NullStatus>();
		nullableOverwriteType.push(inFitBlock);
		testNullables.applyAssertions(inFitBlock);
		whileBlock.block.accept(this);
		nullableOverwriteType.pop();

		boolean containsbreak = new WhileBreakFinder().containsBreak(whileBlock.block);
		if(!containsbreak) {//if while loop contains break we may never "normally" escape so we cannot draw conclusions
			testNullables.getInverted().applyAssertions(nullableOverwriteType.peek());
		}
		
		if(whileBlock.elseblock != null){
			whileBlock.elseblock.accept(this);
		}
		
		return null;
	}

	
	
	
	@Override
	public Object visit(RefName refName) {
		NullStatus inferedAs = null;
		
		/*
		 * Type tt = refName.getTaggedType(); if(tt != null) { TypeCheckUtils.check
		 * //TODO: check if the thing is a special transient class }
		 */
		
		if (refName.inferNonNullable) {
			inferedAs = NullStatus.NOTNULL;
			nullableOverwriteType.peek().put(refName.nameAndLocKey, inferedAs);
		} else {
			if (!this.nullableOverwriteType.isEmpty()) {
				inferedAs = nullStatusInferedAs(refName.nameAndLocKey);
			}
		}
		
		if(null != inferedAs) {
			Type returnType = refName.getTaggedType();
			if(null != returnType) {
				Object constf = ((Node)returnType).getFoldedConstant();
				returnType = (Type)returnType.copy();
				((Node)returnType).setFoldedConstant(constf);
				if(returnType.getNullStatus() != inferedAs) {
					returnType.setNullStatus(inferedAs);
					refName.setTaggedType(returnType);
					this.hadMadeRepoints=true;
					
					if(refName.astRedirectforOnChangeNesting != null) {
						refName.astRedirectforOnChangeNesting.accept(this);
					}
					
				}
			}
			
		}
		
		return null;
	}	
	
	
	@Override
	public Object visit(TryCatch tryCatch) {
		//lastLineVisited=tryCatch.getLine();
		if(tryCatch.astRepoint!=null){
			tryCatch.astRepoint.accept(this);
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
		}
		
		
		if(tryCatch.hasFinal()) {
			this.nullableOverwriteType.peek().putAll(tryCatch.finalBlock.inferedNullability);
		}else  {
			boolean fail=false;
			
			List<HashMap<Pair<String, Boolean>, NullStatus>> inferedNullinBlocks = new ArrayList<HashMap<Pair<String, Boolean>, NullStatus>>();
			if(tryCatch.blockToTry.inferedNullability != null && !tryCatch.blockToTry.inferedNullability.isEmpty()) {
				inferedNullinBlocks.add(tryCatch.blockToTry.inferedNullability);
			}else {
				fail=true;
			}
			
			for(CatchBlocks cbs : tryCatch.cbs) {
				if(cbs.catchBlock.inferedNullability != null && !cbs.catchBlock.inferedNullability.isEmpty()) {
					inferedNullinBlocks.add(cbs.catchBlock.inferedNullability);
				}else {
					fail=true;
					break;
				}
			}
			if(!inferedNullinBlocks.isEmpty()) {
				Pair<List<Pair<String, Boolean>>, List<Pair<String, Boolean>>> nonnullAndNullable = Utils.filterNullInferMap(inferedNullinBlocks);
				
				HashMap<Pair<String, Boolean>, NullStatus> addto = this.nullableOverwriteType.peek();
				
				if(!fail) {
					for(Pair<String, Boolean> nonNull : nonnullAndNullable.getA()) {
						addto.put(nonNull, NullStatus.NOTNULL);
					}
				}
				
				for(Pair<String, Boolean> nullableNull : nonnullAndNullable.getB()) {
					addto.put(nullableNull, NullStatus.NULLABLE);
				}
			}
		}
		
		return null;
		
	}

	
	private HashMap<Pair<String, Boolean>, NullStatus> allKnownNonNullUnknown(){
		//mark everything within the isolate as being of unknown nullability
		HashMap<Pair<String, Boolean>, NullStatus>  flatstate = new HashMap<Pair<String, Boolean>, NullStatus>();
		for(HashMap<Pair<String, Boolean>, NullStatus> level : nullableOverwriteType) {
			for(Pair<String, Boolean> key : level.keySet()) {
				flatstate.put(key, level.get(key));
			}
		}
		
		HashMap<Pair<String, Boolean>, NullStatus>  unknownLevel = new HashMap<Pair<String, Boolean>, NullStatus>();
		for(Pair<String, Boolean> key : flatstate.keySet()) {
			if(flatstate.get(key) == NullStatus.NOTNULL) {
				unknownLevel.put(key, NullStatus.UNKNOWN);
			}
		}
		return unknownLevel;
	}
	
	@Override
	public Object visit(AsyncBlock asyncBlock) {
		//invalidate all assumptions about that defined as non null
		nullableOverwriteType.push(allKnownNonNullUnknown());
		asyncBlock.body.accept(this);
		nullableOverwriteType.pop();
		
		//and restore state as it was
		
		if(null != asyncBlock.executor)
		{
			asyncBlock.executor.accept(this);
		}
		
		return null;
	}	
	
	
	@Override
	public Object visit(OnChange onChange){
		//lastLineVisited=onChange.getLine();
		for(Node e: onChange.exprs){
			e.accept(this);
		}
		nullableOverwriteType.push(allKnownNonNullUnknown());
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
		nullableOverwriteType.pop();		
		
		return null;//null is ok
	}

	@Override
	public Object visit(AsyncBodyBlock asyncBodyBlock) {
		//lastLineVisited=asyncBodyBlock.getLine();

		nullableOverwriteType.push(allKnownNonNullUnknown());
		if(null != asyncBodyBlock.applyMethodFuncDef){
			asyncBodyBlock.applyMethodFuncDef.accept(this);
			asyncBodyBlock.cleanUpMethodFuncDef.accept(this);
			asyncBodyBlock.initMethodNameFuncDef.accept(this);
		}
		
		for(Block pre : asyncBodyBlock.preBlocks){
			pre.accept(this);
		}
	
		asyncBodyBlock.mainBody.accept(this);
		
		for(Block post : asyncBodyBlock.postBlocks){
			post.accept(this);
		}
		nullableOverwriteType.pop();	
		
		return null;
	}
	
}
