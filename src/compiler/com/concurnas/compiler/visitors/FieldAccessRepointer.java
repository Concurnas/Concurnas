package com.concurnas.compiler.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import com.concurnas.compiler.ast.ArrayRef;
import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignStyleEnum;
import com.concurnas.compiler.ast.AsyncRefRef;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.CastExpression;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.DotOperator;
import com.concurnas.compiler.ast.DuffAssign;
import com.concurnas.compiler.ast.FactorPostFixEnum;
import com.concurnas.compiler.ast.FactorPrefixEnum;
import com.concurnas.compiler.ast.FuncInvoke;
import com.concurnas.compiler.ast.FuncInvokeArgs;
import com.concurnas.compiler.ast.FuncRefInvoke;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.GenericType;
import com.concurnas.compiler.ast.GetSetOperation;
import com.concurnas.compiler.ast.ImpliInstance;
import com.concurnas.compiler.ast.Line;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.ModuleType;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.PostfixOp;
import com.concurnas.compiler.ast.PrefixOp;
import com.concurnas.compiler.ast.PrimativeType;
import com.concurnas.compiler.ast.PrimativeTypeEnum;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.RefSuper;
import com.concurnas.compiler.ast.RefThis;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.VarInt;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.bytecode.FunctionGenneratorUtils;
import com.concurnas.compiler.typeAndLocation.Location;
import com.concurnas.compiler.typeAndLocation.LocationClassField;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.visitors.util.ErrorRaiseableSupressErrors;
import com.concurnas.runtime.Pair;

public class FieldAccessRepointer extends AbstractErrorRaiseVisitor {

	//TODO: when adding private, public etc, this should do the checking
	//has to be done here because you can have a public field and a private setter/getter, in which case u wouldn't want todo the repointing etc
	//or maybe you'd still want to add antoher step hmmm

	//private final FeildMapperER fmer;
	
	public FieldAccessRepointer(String fullPathFileName/*, ScopeAndTypeChecker satc*/) {
		super(fullPathFileName);
		//this.fmer = new FeildMapperER(satc);//should be first one as this has all the stuff we need already
	}
	
	ErrorRaiseableSupressErrors ersup = new ErrorRaiseableSupressErrors(null);
	
	private boolean hadMadeRepoints = false;
	
	public boolean hadMadeRepoints()
	{
		return this.hadMadeRepoints;
	}
	
	
	@Override
	public Object visit(Block block) {
		if(block.isModuleLevel){
			hadMadeRepoints=false;
		}
		
		return super.visit(block);
	}
	
	
	//can only set to a field when on lhs of: post/prefix op, assign existing, array
	
	private boolean repointableRhs(Expression e)
	{
		/*if(e instanceof RefNamedType){
			ArrayRef astRedirectToArrayRef =  ((RefNamedType)e).astRedirectToArrayRef;
			if(null != astRedirectToArrayRef){
				e = astRedirectToArrayRef;
			}
		}*/
		
		return e instanceof RefName || (e instanceof AsyncRefRef && ((AsyncRefRef)e).b instanceof RefName) || (e instanceof ArrayRef && ((ArrayRef)e).expr instanceof RefName)  || (e instanceof FuncInvoke && ((FuncInvoke)e).isReallyLambda);
	}

	private ClassDef extractClassDottedOn(Type lhs)
	{
		if(null != lhs && lhs instanceof NamedType){
			NamedType nt = (NamedType)lhs;
			ClassDef dottedOn = nt.getSetClassDef();
			if(this.currentClassDef.isEmpty() || !dottedOn.equals(this.currentClassDef.peek()))
			{
				if( TypeCheckUtils.isTypedActor(ersup, nt)){
					NamedType rootActorType = TypeCheckUtils.extractRootActor(nt);
					return extractClassDottedOn(rootActorType.getGenTypes().get(0));
				}
				
				return dottedOn;
			}
		}
		
		return null;
	}
	

	
	public Stack<ClassDef> currentClassDef = new Stack<ClassDef>();
	
	@Override
	public Object visit(ClassDef classDef) {
		
		currentClassDef.push(classDef);
		
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
		
		if(classDef.superclass!=null)
		{
			if(null != classDef.superClassGenricList && !classDef.superClassGenricList.isEmpty())
			{
				for(Type n : classDef.superClassGenricList)
				{
					if(null != null ){
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
		
		currentClassDef.pop();
		
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

	
	/*
	private class FeildMapperER implements ErrorRaiseable
	{

		private ScopeAndTypeChecker satc;

		public FeildMapperER(ScopeAndTypeChecker satc)
		{//recycling
			this.satc = satc;
		}
		
		@Override
		public void raiseError(int line, int column, String error) {
		}

		@Override
		public ClassDef getImportedOrDeclaredClassDef(String name) {
			return this.satc.getImportedOrDeclaredClassDef(name);
		}

		@Override
		public ClassDef getImportedClassDef(String namereftoresolve, boolean ingoreDotOp) {
			return this.satc.getImportedClassDef(namereftoresolve, ingoreDotOp);
		}

		@Override
		public ClassDef getImportedClassDef(String namedType) {
			return this.satc.getImportedClassDef(namedType);
		}

		@Override
		public ErrorRaiseable getErrorRaiseableSupression() {
			return this;
		}
	}*/
	
	private static final PrimativeType voidConst =  new PrimativeType(PrimativeTypeEnum.VOID);
	
	private Pair<Type, String> getGetterSetter(ClassDef dotOpOn, Expression exp, boolean isGetter)
	{
		return getGetterSetter(dotOpOn, exp, isGetter, false);
	}
	
	public static String getterOrSetterName(RefName rn, Type typeOfVar, boolean isGetter) {
		if(isGetter){
			return rn.sourceOfNameGeneric ? String.format("get%s", FunctionGenneratorUtils.makeCamelName(rn.name) ) : FunctionGenneratorUtils.getGetterName(rn.name, null, typeOfVar, 0, 0);
		}
		else{
			return FunctionGenneratorUtils.getSetterName(rn.name);
		}
	}
	
	private Pair<Type, String> getGetterSetter(ClassDef dotOpOn, Expression exp, boolean isGetter, boolean setterArgExpectRef)
	{//returns type [only if getter] and name of function
		if(dotOpOn == null){
			return null;
		}
		
		RefName rn;
		if(exp instanceof RefName){
			rn = (RefName)exp;
		}
		else if(exp instanceof FuncInvoke){
			rn = new RefName(((FuncInvoke)exp).funName);
		}
		else if(exp instanceof AsyncRefRef){
			rn = (RefName) ((AsyncRefRef)exp).b;
		}
		else{
			rn = (RefName) ((ArrayRef)exp).expr;
		}
		
		Type typeOfVar = exp.getTaggedType();
		
		String funcName = getterOrSetterName(rn, typeOfVar, isGetter);
		/*if(isGetter){
			funcName = rn.sourceOfNameGeneric ? String.format("get%s", FunctionGenneratorUtils.makeCamelName(rn.name) ) : FunctionGenneratorUtils.getGetterName(rn.name, null, typeOfVar, 0, 0);
		}
		else{
			funcName = FunctionGenneratorUtils.getSetterName(rn.name, null, typeOfVar);
		}*/
		
		HashSet<TypeAndLocation> choices = dotOpOn.getFuncDef(funcName, false, false);
		Type foundRet = null;
		boolean found = false;
		for(TypeAndLocation choice: choices)
		{
			Type cType = choice.getType();
			if(cType instanceof FuncType)
			{
				FuncType ft = (FuncType)cType;
				ArrayList<Type> inputs = ft.getInputs();
				if(isGetter)//there can be only 1 getter
				{
					if(null == inputs || inputs.isEmpty())
					{
						foundRet = ft.retType == null ? voidConst: ft.retType;
						found=true;break;
					}
				}
				else{
					if(null != inputs && inputs.size()==1)//setter equals as above
					{
						
						if(setterArgExpectRef){
							Type fir = inputs.get(0);
							if(!TypeCheckUtils.hasArrayRefLevels(fir)){
								continue;//skip this one, we need it to take only a ref type, this is forced
							}
						}
						
						foundRet = ft.retType == null ? voidConst: ft.retType;
						found=true;break;
					}
				}
				
			}
		}
		return found ? new Pair<Type, String>(foundRet, funcName) : null;
		
	}
	
	private FuncInvoke repointRefName(int line, int col, String funcName, Expression argExpression, Type orgiLhsType)
	{
		FuncInvokeArgs invokeArgs = new FuncInvokeArgs(line, col);
		FuncInvoke ret = new FuncInvoke(line, col, funcName, invokeArgs);
		
		if(null != argExpression){
			invokeArgs.add(argExpression);
		}
		ret.refRepointOrigLhsType =  orgiLhsType;
		ret.isSynth = true;
		return ret;
	}
	
	private Expression gennerateRepointGet(Expression rhs, String repointName)
	{//returns: wasSucssful, new Expression
		/*
		if(rhs instanceof RefNamedType){
			ArrayRef astRedirectToArrayRef =  ((RefNamedType)rhs).astRedirectToArrayRef;
			if(null != astRedirectToArrayRef){
				rhs = astRedirectToArrayRef;
			}
		}*/
		
		hadMadeRepoints=true;
		int line = rhs.getLine(); 
		int col  = rhs.getColumn();
		
		if(rhs instanceof RefName)
		{
			return repointRefName(line, col, repointName, null, null);
		}
		else if(rhs instanceof ArrayRef && ((ArrayRef)rhs).expr instanceof RefName)
		{
			ArrayRef ar = (ArrayRef)rhs;
			//wrap the field getter inside the arrayref
			return new ArrayRef(ar.getLine(), ar.getColumn(), repointRefName(line, col, repointName, null, null), ar.arrayLevelElements );
		}
		else if(rhs instanceof AsyncRefRef && ((AsyncRefRef)rhs).b instanceof RefName)
		{
			AsyncRefRef ar = (AsyncRefRef)rhs;
			//wrap the field getter inside the arrayref
			return new AsyncRefRef(ar.getLine(), ar.getColumn(), repointRefName(line, col, repointName, null, null), ar.refCntLevels );
		}
		else if(rhs instanceof FuncInvoke){
			//cl.funto(1) => cl.getFuncto()(1)
			FuncInvoke functo =  new FuncInvoke(line, col, repointName, new FuncInvokeArgs(line, col));
			functo.isSynth=true;
			return new FuncRefInvoke(line, col, functo, ((FuncInvoke)rhs).args);
		}
		throw new RuntimeException("Unexpected Expression type for field access repoint encountered: " + rhs.getClass().getName());
	}
	
	private ClassDef isEligableForRewrite(Boolean directAccess, Expression e, Expression prevE, Type typeSoFar, boolean isGetter)
	{
		if(!directAccess && !(prevE instanceof RefSuper) && repointableRhs(e))
		{
			Type tt = e.getTaggedType();
			if(tt instanceof ModuleType){
				Type pgt = TypeCheckUtils.getRefType(prevE.getTaggedType());
				if(pgt instanceof NamedType){
					ClassDef cd = ((NamedType)pgt).getSetClassDef();
					Pair<Type, String> typeAndFuncName = getGetterSetter(cd, e, isGetter, false);
					
					return typeAndFuncName==null?null:cd;
				}
				return null;
			}
			if(e instanceof RefName){
				RefName refName = (RefName)e;
				if(null != refName.resolvesTo){
					 Location loc = refName.resolvesTo.getLocation();
					 if(loc instanceof LocationClassField){
						 LocationClassField aslcf = (LocationClassField)loc;
						 Type owner = aslcf.ownerType;
						 if(owner != null && owner instanceof NamedType){
							 ClassDef set = ((NamedType)owner).getSetClassDef();
							 if(set!=null){
								 if(!this.currentClassDef.isEmpty()){
									 ClassDef cd = this.currentClassDef.peek();
									 if(cd.equals(set)){//if potentially calling a this or implicit this reference to own 'get/set' function, we wish to avoid inf loop
										 Expression preceeding = elementPreceededBy.get(e);
										 if(preceeding == null || preceeding instanceof RefSuper || preceeding instanceof RefThis){
											 return null;//abort else we end up, in inf loop
										 }
									 }
								 }
								 return set;
							 }
						 }
					 }
				}
			}
			
			
			ClassDef clsDottedOn = extractClassDottedOn(typeSoFar);
			if(null != clsDottedOn)
			{
				if(!(typeSoFar.hasArrayLevels() && e instanceof RefName && ((RefName)e).name.equals("length")) )
				{//exception: ar = [1,2,3]; ar.length <- we dont want to remap this
					
					
					if(typeSoFar instanceof NamedType && TypeCheckUtils.isTypedActor(ersup, (NamedType)typeSoFar)){
						NamedType rootActorType = TypeCheckUtils.extractRootActor((NamedType)typeSoFar);
						return extractClassDottedOn(rootActorType.getGenTypes().get(0));
					}
					
					return clsDottedOn;
				}
			}
		}
		return null;
	}
/*	private ClassDef isEligableForRewrite(Boolean directAccess, Expression e, Expression prevE, Type typeSoFar, boolean isGetter)
	{
		if(!directAccess && !(prevE instanceof RefSuper) && repointableRhs(e))
		{
			Type tt = e.getTaggedType();
			
			ClassDef ret=null;
			
			if(e instanceof RefName){
				RefName refName = (RefName)e;
				if(null != refName.resolvesTo){
					 Location loc = refName.resolvesTo.getLocation();
					 if(loc instanceof LocationClassField){
						 LocationClassField aslcf = (LocationClassField)loc;
						 Type owner = aslcf.ownerType;
						 if(owner != null && owner instanceof NamedType){
							 ClassDef set = ((NamedType)owner).getSetClassDef();
							 if(set!=null){
								 ret = set;
							 }
						 }
					 }
				}
			}
			
			if(ret == null){
				ClassDef clsDottedOn = extractClassDottedOn(typeSoFar);
				if(null != clsDottedOn)
				{
					if(!(typeSoFar.hasArrayLevels() && e instanceof RefName && ((RefName)e).name.equals("length")) )
					{//exception: ar = [1,2,3]; ar.length <- we dont want to remap this
						if(typeSoFar instanceof NamedType && TypeCheckUtils.isTypedActor(ersup, (NamedType)typeSoFar)){
							NamedType rootActorType = TypeCheckUtils.extractRootActor((NamedType)typeSoFar);
							return extractClassDottedOn(rootActorType.getGenTypes().get(0));
						}
						
						ret = clsDottedOn;
					}
				}
			}
			
			
			
			if(null == ret && tt instanceof ModuleType){
				
				Type pgt = TypeCheckUtils.getRefType(prevE.getTaggedType());
				if(pgt instanceof NamedType){
					ClassDef cd = ((NamedType)pgt).getSetClassDef();
					Tuple<Type, String> typeAndFuncName = getGetterSetter(cd, e, isGetter, false);
					
					return typeAndFuncName==null?null:cd;
				}
				
				
				
			}
			
		}
		return null;
	}*/
	
	private HashMap<Expression, Expression> elementPreceededBy = new HashMap<Expression, Expression>();
	
	private Type processDotOp(DotOperator dotOperator, Boolean processLast) {
		ArrayList<Type> typesSoFar = new ArrayList<Type>();
		ArrayList<Expression> elements = dotOperator.getElements(this);
		for (Expression e : elements) {
			typesSoFar.add(e.getTaggedType());
		}

		Expression head = dotOperator.getHead(this);
		head.accept(this);
		Expression preceeding = head;
		Type typeSoFar = head.getTaggedType();
		for (int n = 1; n < elements.size(); n++) {
			if (processLast || n != elements.size() - 1) {
				boolean directAccess = dotOperator.getIsDirectAccess(this).get(n - 1);
				Expression e = elements.get(n);
				
				ClassDef clsDottedOn = isEligableForRewrite(directAccess, e, elements.get(n-1), typeSoFar, true);
				
				if (null != clsDottedOn) {
					Pair<Type, String> typeAndFuncName = getGetterSetter(clsDottedOn, e, true);

					if (null != typeAndFuncName) {
						e = gennerateRepointGet(e, typeAndFuncName.getB());
						elements.set(n, e);
					}
				}

				elementPreceededBy.put(e, preceeding);
				e.accept(this);
				elementPreceededBy.remove(e);
				preceeding=e;
				// on fail revert to prev tagged types and carry on as if
				// nothing happened
				typeSoFar = typesSoFar.get(n);// assume no type change
			}
		}
		return typeSoFar;
	}
	
	
	@Override
	public Object visit(DotOperator dotOperator) {
		//asume it's a getter
		return processDotOp(dotOperator, true);
	}
	
	private boolean isLastElementArrayRef(DotOperator dop)
	{
		ArrayList<Expression> elements = dop.getElements(this);
		
		if( elements != null && !elements.isEmpty() ){
			Expression pen = elements.get(elements.size()-1);
			if(pen instanceof ArrayRef){
				return true;
			}
		/*	if(pen instanceof RefNamedType && ((RefNamedType)pen).astRedirectToArrayRef != null){
				return true;
			}*/
		}
		return false;
	}
	
	
	private Expression gennerateRepointSet(Expression rhs, String repointName, Expression argExpression, Type orgiLhsType)
	{//returns: wasSucssful, new Expression
		
		/*if(rhs instanceof RefNamedType){
			ArrayRef astRedirectToArrayRef =  ((RefNamedType)rhs).astRedirectToArrayRef;
			if(null != astRedirectToArrayRef){
				rhs = astRedirectToArrayRef;
			}
		}*/
		
		hadMadeRepoints=true;
		int line = rhs.getLine(); 
		int col  = rhs.getColumn();
		
		if(rhs instanceof RefName)
		{
			return repointRefName(line, col, repointName, argExpression, orgiLhsType);
		}
		else if(rhs instanceof ArrayRef && ((ArrayRef)rhs).expr instanceof RefName)
		{
			ArrayRef ar = (ArrayRef)rhs;
			//wrap the field getter inside the arrayref
			return new ArrayRef(ar.getLine(), ar.getColumn(), repointRefName(line, col, repointName, argExpression, orgiLhsType), ar.arrayLevelElements );
		}
		throw new RuntimeException("Unexpected Expression type for field access repoint encountered: " + rhs.getClass().getName());
	}
	
	private Expression createGetSetOperation(int line, int col, String getterName, String setterName, Expression argExpression, AssignStyleEnum isPlus, boolean ispostfix)
	{
		FuncInvokeArgs invokeArgs = new FuncInvokeArgs(line, col);
		if(null != argExpression){
			invokeArgs.add(argExpression);
		}
		
		GetSetOperation ret = new GetSetOperation(line, col, getterName, setterName, isPlus, ispostfix, argExpression);
		//ret.setExpectNonRef(var);
		return ret;
	}
	
	private Expression gennerateRepointGetSet(Expression rhs, String getterName, String setterName, Expression argExpression, AssignStyleEnum isPlus, boolean ispostfix)
	{
		/*
		if(rhs instanceof RefNamedType){
			ArrayRef astRedirectToArrayRef =  ((RefNamedType)rhs).astRedirectToArrayRef;
			if(null != astRedirectToArrayRef){
				rhs = astRedirectToArrayRef;
			}
		}
		*/
		hadMadeRepoints=true;
		int line = rhs.getLine(); 
		int col  = rhs.getColumn();
		
		if(rhs instanceof RefName)
		{
			return createGetSetOperation(line, col, getterName, setterName, argExpression, isPlus, ispostfix);
		}
		else if(rhs instanceof ArrayRef && ((ArrayRef)rhs).expr instanceof RefName)
		{
			ArrayRef ar = (ArrayRef)rhs;
			//wrap the field getter inside the arrayref
			return new ArrayRef(ar.getLine(), ar.getColumn(), createGetSetOperation(line, col, getterName, setterName, argExpression, isPlus, ispostfix), ar.arrayLevelElements );
		}
		throw new RuntimeException("Unexpected Expression type for field access repoint encountered: " + rhs.getClass().getName());
	}
	
	@Override
	public DotOperator visit(AssignExisting assignExisting) {
		
		Expression assignee = assignExisting.assignee;
		DotOperator reassignTo = null;
		if(assignee instanceof DotOperator && !isLastElementArrayRef((DotOperator)assignee) && ((DotOperator)assignee).getElements(this).size() > 1 )
		{//obj[2].x = 8 -> obj[2].setX(8)
			//cls.x[0] = 99; -> cls.getX()[0] = 99; - note that we avoid this case and revert to getter logic, not setter
			DotOperator doAssigne = (DotOperator)assignee;
			ArrayList<Expression> elements = doAssigne.getElements(this);
			ArrayList<Boolean> isDirectAccess = doAssigne.getIsDirectAccess(this);
			
			Expression rhsExpression = assignExisting.expr;
			
			Type clasSofar = processDotOp(doAssigne, false);
			int idxLastE = elements.size()-1;
			Expression laste = elements.get(idxLastE);
			Expression preve = elements.get(idxLastE-1);
			boolean lastdirectacc = isDirectAccess.get(isDirectAccess.size()-1);
			
			ClassDef clsDottedOn = isEligableForRewrite(lastdirectacc, laste, preve, clasSofar, false);
			if(null != clsDottedOn)
			{
				if(assignExisting.eq.isEquals()){
					boolean forcedRef = assignExisting.refCnt >0;
					Type rhsType = rhsExpression.getTaggedType();
					boolean rhsLockedRef = TypeCheckUtils.hasRefLevelsAndIsLocked(rhsType);
					boolean setterArgExpectRef = forcedRef || rhsLockedRef;
					
					Pair<Type, String> typeAndFuncName = getGetterSetter(clsDottedOn, laste, false, setterArgExpectRef);//setter

					if (null != typeAndFuncName) {
						//Uh ok
						if(setterArgExpectRef && !rhsLockedRef){ 
							if(!TypeCheckUtils.hasRefLevels(rhsType)){ //e.g. a.z := d. transform to: a.setZ(d!) [where d is not already a ref]
								//rhsExpression = new AsyncInvoke(rhsExpression.getLine(), rhsExpression.getColumn(), rhsExpression);
								//mc.x := "newRef2" -> mc.setX("newRef2" as Object:) where x is of type Object:
								rhsExpression = new CastExpression(rhsExpression.getLine(), rhsExpression.getColumn(), assignExisting.getTaggedType(), rhsExpression);
							}
							else{//e.g. a.z := d. transform to: a.setZ((d):) [where d is a ref already]
								rhsExpression = new AsyncRefRef(rhsExpression.getLine(), rhsExpression.getColumn(), rhsExpression, assignExisting.refCnt);
							}
						}
						
						Type origonalRhsType = TypeCheckUtils.hasRefLevels(rhsType)?assignExisting.getTaggedType():null;
						//because this: mc.x := "newRef2"! //where x is ~x Object: of a mcClass -  should blow up! (else it gets set via setX(Object) resutling in double ref, probably not what u want!
						
						laste = gennerateRepointSet(laste, typeAndFuncName.getB(), rhsExpression, origonalRhsType);
						elements.set(idxLastE, laste);
						reassignTo=doAssigne;
					}
				}
				else
				{//translate into a new GetSetOperation:
					//e.g. obj.x += 69 -> obj.SYN-getXSetX(PLUS, PREFIX, 69)//note prefix avoids unecisary dup operation
					Pair<Type, String> getter = getGetterSetter(clsDottedOn, laste, true);
					if(null != getter)
					{
						Pair<Type, String> setter = getGetterSetter(clsDottedOn, laste, false);
						if(null != setter)
						{
							laste = gennerateRepointGetSet(laste, getter.getB(), setter.getB(), rhsExpression, assignExisting.eq, false);
							elements.set(idxLastE, laste);
							reassignTo=doAssigne;
						}
						
					}
				}
			}
			laste.accept(this);
		}
		else
		{
			assignee.accept(this);
			if(null != assignExisting.expr) {
				assignExisting.expr.accept(this);
			}
		}
		
		return reassignTo;
	}
	
	
	@Override
	public Object visit(LineHolder lineHolder) {
		//if we need to perform a repoinrt of an assign existing operation we perform the following mapping:
		//x.g = 8; -> x.setG(8) [DuffAssign(Expr)
		//[AssignExisting(dotop, exp)]; ->[DuffAssign(Expr)]
		
		this.enterLine();
		
		Line l = lineHolder.l;
		if(l instanceof AssignExisting)
		{
			Node x = (Node)l.accept(this);
			if(null != x &&  x instanceof DotOperator) {
				DotOperator repointed = (DotOperator)x;
				lineHolder.l = new DuffAssign(l.getColumn(), l.getColumn(), repointed);
			}
		}
		else
		{
			l.accept(this);
		}
		
		this.leaveLine();
		return null;
	}
	
	private DotOperator handlePrefixPostFix(Expression expr,  AssignStyleEnum operator, boolean ispostfix, int line, int col)
	{
		DotOperator reassignTo = null;
		
		if(expr instanceof DotOperator && !isLastElementArrayRef((DotOperator)expr) && ((DotOperator)expr).getElements(this).size() > 1 )
		{//JPT: refactor this nasty copy paste job
			//cls.x[0] = 99; -> cls.getX()[0] = 99; - note that we avoid this case and revert to getter logic, not setter
			DotOperator doAssigne = (DotOperator)expr;
			Expression rhsExpression = new VarInt(line, col, 1);
			
			Type clasSofar = processDotOp(doAssigne, false);
			
			ArrayList<Expression> elements = doAssigne.getElements(this);
			ArrayList<Boolean> isDirectAccess = doAssigne.getIsDirectAccess(this);
			int idxLastE = elements.size()-1;
			Expression laste = elements.get(idxLastE);
			Expression preve = elements.get(idxLastE-1);
			boolean lastdirectacc = isDirectAccess.get(isDirectAccess.size()-1);
			
			ClassDef clsDottedOn = isEligableForRewrite(lastdirectacc, laste, preve, clasSofar, true);
			if(null != clsDottedOn)
			{
				//e.g. obj.x += 69 -> obj.SYN-getXSetX(PLUS, PREFIX, 69)//note prefix avoids unecisary dup operation
				Pair<Type, String> getter = getGetterSetter(clsDottedOn, laste, true);
				if(null != getter)
				{
					Pair<Type, String> setter = getGetterSetter(clsDottedOn, laste, false);
					if(null != setter)
					{
						laste = gennerateRepointGetSet(laste, getter.getB(), setter.getB(), rhsExpression, operator, ispostfix);
						elements.set(idxLastE, laste);
						reassignTo=doAssigne;
					}
					
				}
				
			}
			laste.accept(this);
		}
		else
		{//do nothing
			expr.accept(this);
		}
		
		return reassignTo;
	}
	
	@Override
	public Object visit(PrefixOp prefixOp) {
		FactorPrefixEnum prefix = prefixOp.prefix;
		if(prefix != FactorPrefixEnum.NEG ){
			prefixOp.ASTDivert = handlePrefixPostFix(prefixOp.p1, prefix==FactorPrefixEnum.PLUSPLUS?AssignStyleEnum.PLUS_EQUALS: AssignStyleEnum.MINUS_EQUALS, false, prefixOp.getLine(), prefixOp.getColumn());
		}
		else
		{
			prefixOp.p1.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(PostfixOp postfixOp) {
		postfixOp.ASTDivert =  handlePrefixPostFix(postfixOp.p2, postfixOp.postfix ==FactorPostFixEnum.PLUSPLUS?AssignStyleEnum.PLUS_EQUALS: AssignStyleEnum.MINUS_EQUALS, true, postfixOp.getLine(), postfixOp.getColumn());
		return null;
	}
}
