package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class OnChange extends CompoundStatement implements HasExtraCapturedVars, Expression, AsyncInvokable {

	public ArrayList<Node> exprs;
	public Block body;
	private FuncParams extraCapturedLocalVars = new FuncParams(0,0);
	public Pair<String, String> onChangeDets;
	public NamedType holderclass;
	public boolean isModuleLevel;
	public String initMethodName = "SETME";
	public String applyMethodName = "SETME";
	public String cleanupMethodName = "SETME";
	public FuncDef applyMethodFuncDef;
	public FuncDef cleanUpMethodFuncDef;
	public FuncDef initMethodNameFuncDef;
	public ClassDef stateObjectClassDef;
	public Set<String> toSpliceIn;
	public Map<String, String> takeArgFromSO = new HashMap<String, String>();
	public FuncParams funcParams;
	public boolean noReturn = false;
	public boolean returnsConventionally = false;
	public AssignExisting theAssToStoreRefIn;//the ass will have the 
	public HashSet<String> namesOverridedInInit = new HashSet<String>();
	public int asyncIndex = -1;//used to indicate position when directly nested inside async
	public Type asyncExplicitReturnVarType;
	public List<String> options;
	
	public String getFilenameSO(){
		return onChangeDets.getA() + "$SO";
	}
	
	@Override
	public String getFullnameSO(){
		return onChangeDets.getA() + "$SO";
	}
	
	public OnChange(int line, int col, ArrayList<Node> exprs, Block todo, List<String> options) {
		super(line, col);
		this.exprs = exprs;
		this.body = todo;
		this.options = options;
	}
	
	public String getName(){
		return "onchange";
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}

	protected OnChange copyCoreType(){
		return new OnChange(super.line, super.column, (ArrayList<Node>) Utils.cloneArrayList(this.exprs), body==null?null:(Block)body.copy(), options);
	}
	
	@Override
	public Node copyTypeSpecific() {
		OnChange ifola = copyCoreType();
		ifola.noReturn = noReturn;
		ifola.asyncIndex = asyncIndex;
		ifola.returnsConventionally = returnsConventionally;
		ifola.theAssToStoreRefIn = null==theAssToStoreRefIn?null:(AssignExisting)theAssToStoreRefIn.copy();
		ifola.asyncExplicitReturnVarType = asyncExplicitReturnVarType;
		
		ifola.applyMethodFuncDef = applyMethodFuncDef;
		ifola.cleanUpMethodFuncDef = cleanUpMethodFuncDef;
		ifola.initMethodNameFuncDef = initMethodNameFuncDef;
		ifola.stateObjectClassDef = stateObjectClassDef;
		ifola.extraCapturedLocalVars = extraCapturedLocalVars;
		ifola.toSpliceIn = toSpliceIn;
		ifola.onChangeDets = onChangeDets;
		ifola.namesOverridedInInit = namesOverridedInInit;
		ifola.funcParams = funcParams;
		ifola.takeArgFromSO = takeArgFromSO;
		ifola.isModuleLevel = isModuleLevel;
		ifola.holderclass = holderclass;
		ifola.initMethodName = initMethodName;
		ifola.applyMethodName = applyMethodName;
		ifola.cleanupMethodName = cleanupMethodName;
		ifola.onlyClose = onlyClose;
		ifola.options = options;
		ifola.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();

		return ifola;
	}
	private Expression preceedingExpression;
	public boolean onlyClose=false;
	
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}
	
	public boolean getShouldBePresevedOnStack(){
		return super.getShouldBePresevedOnStack();
	}
	
	public void setShouldBePresevedOnStack(boolean should)
	{
		super.setShouldBePresevedOnStack(should);
		//super.setShouldBePresevedOnStack(should);
		if(null!=this.body){
			this.body.setShouldBePresevedOnStack(should);
		}
	}
	
	@Override
	public FuncParams getExtraCapturedLocalVars() {
		return this.extraCapturedLocalVars;
	}

	@Override
	public void setExtraCapturedLocalVars(FuncParams extraCapturedLocalVars) {
		this.extraCapturedLocalVars = extraCapturedLocalVars;
	}

	public boolean getHasAttemptedNormalReturn() {
		return this.body.hasDefoReturned;
	}
	
	
	
	@Override
	public FuncDef getinitMethodNameFuncDef() {
		return initMethodNameFuncDef;
	}
	@Override
	public FuncDef getapplyMethodFuncDef() {
		return applyMethodFuncDef;
	}
	@Override
	public FuncDef getcleanUpMethodFuncDef() {
		return cleanUpMethodFuncDef;
	}
	@Override
	public Pair<String, String> getonChangeDets() {
		return onChangeDets;
	}

	@Override
	public HashSet<String> getnamesOverridedInInit() {
		return namesOverridedInInit;
	}

	@Override
	public Map<String, String> gettakeArgFromSO() {
		return takeArgFromSO;
	}

	@Override
	public boolean getnoReturn() {
		return noReturn;
	}

	@Override
	public AssignExisting gettheAssToStoreRefIn() {
		return theAssToStoreRefIn;
	}

	@Override
	public boolean getisModuleLevel() {
		return this.isModuleLevel;
	}

	@Override
	public NamedType getholderclass() {
		return holderclass;
	}
}
