package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class AsyncBodyBlock extends CompoundStatement implements HasExtraCapturedVars, AsyncInvokable {
	
	public ArrayList<Block> preBlocks;
	public ArrayList<Block> postBlocks;
	public Block mainBody;
	public int childrenCount;
	public Pair<String, String> onChangeDets;
	public HashSet<String> namesOverridedInInit = new HashSet<String>();
	public FuncParams extraCapturedLocalVars = new FuncParams(0,0);
	public boolean isModuleLevel;
	public NamedType holderclass;
	public FuncParams funcParams;
	public boolean hasEvery=false;
	public String applyMethodName = "SETME";
	public String initMethodName = "SETME";
	public String cleanupMethodName = "SETME";
	public boolean noReturn = true;
	public AssignExisting theAssToStoreRefIn;
	public FuncDef initMethodNameFuncDef;
	public FuncDef applyMethodFuncDef;
	public FuncDef cleanUpMethodFuncDef;
	public Set<String> toSpliceIn = new HashSet<String>();
	public ClassDef stateObjectClassDef;
	public Map<String, String> takeArgFromSO = new HashMap<String, String>();
	public HashMap<String, Type> preBlockVars = new HashMap<String, Type>();
	
	public AsyncBodyBlock(int line, int column, ArrayList<Block> preBlocks, ArrayList<Block> postBlocks, Block mainBody) {
		super(line, column);
		this.preBlocks = preBlocks;
		this.postBlocks =  postBlocks;
		this.mainBody = mainBody;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}

	
	public String getFilenameSO(){
		return onChangeDets.getA() + "$SO";
	}
	public String getFullnameSO(){
		return onChangeDets.getA() + "$SO";
	}
	
	@Override
	public Node copyTypeSpecific() {
		AsyncBodyBlock ret = new AsyncBodyBlock(line, column, (ArrayList<Block>) Utils.cloneArrayList(preBlocks), (ArrayList<Block>) Utils.cloneArrayList(postBlocks), (Block)mainBody.copy());
		ret.childrenCount=this.childrenCount;
		ret.extraCapturedLocalVars = null==extraCapturedLocalVars?null:(FuncParams)extraCapturedLocalVars.copy();
		ret.isModuleLevel = this.isModuleLevel;
		ret.holderclass = this.holderclass;
		ret.hasEvery = this.hasEvery;
		ret.theAssToStoreRefIn = null==theAssToStoreRefIn?null:(AssignExisting)theAssToStoreRefIn.copy();
		ret.preBlockVars=this.preBlockVars;
		ret.onChangeDets=this.onChangeDets;
		ret.initMethodNameFuncDef=this.initMethodNameFuncDef;
		ret.applyMethodFuncDef=this.applyMethodFuncDef;
		ret.cleanUpMethodFuncDef=this.cleanUpMethodFuncDef;
		ret.toSpliceIn=this.toSpliceIn;
		
		ret.applyMethodName=this.applyMethodName;
		ret.initMethodName=this.initMethodName;
		ret.cleanupMethodName=this.cleanupMethodName;
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		
		return ret;
	}
	private Expression preceedingExpression;
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
		
		for(LineHolder lh: this.mainBody.lines){
			lh.l.setShouldBePresevedOnStack(should);
		}
		
		if(null != this.preBlocks && !preBlocks.isEmpty()){
			/*for(Block blk : this.preBlocks){
				blk.setShouldBePresevedOnStack(should);
			}*/
			this.preBlocks.get(this.preBlocks.size() - 1).setShouldBePresevedOnStack(should);
		}
		
		if(null != this.postBlocks && !postBlocks.isEmpty()){
			/*for(Block blk : this.postBlocks){
				blk.setShouldBePresevedOnStack(should);
			}*/
			
			this.postBlocks.get(this.postBlocks.size() - 1).setShouldBePresevedOnStack(should);
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
		return isModuleLevel;
	}

	@Override
	public NamedType getholderclass() {
		return holderclass;
	}
	
}
