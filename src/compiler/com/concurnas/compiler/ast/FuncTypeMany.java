package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.visitors.Visitor;

public class FuncTypeMany extends FuncType {

	private final List<FuncType> many;
	private final FuncType head;
	public FuncTypeMany(List<FuncType> many)
	{
		this(0,0, many);
	}
	
	public FuncTypeMany(int line, int col, List<FuncType> many)
	{
		super(line, col);
		assert null != many;
		assert !many.isEmpty();
		this.many = many;
		if(many.size() == 0){
			int g=9;
		}
		this.head = many.get(0);
		this.setLambdaDetails(this.head.getLambdaDetails());
		this.origonatingFuncDef = this.head.origonatingFuncDef;
		this.arrayLevels = this.head.arrayLevels;
		this.inout = this.head.inout;
		this.retType = this.head.retType;
		this.setInputs(this.head.getInputs());
	}
	
	public FuncTypeMany(List<FuncType> copy, FuncType first1) {
		this(0, 0, copy);
		/*
		 * 		super(0, 0);
		this.many = copy;
		this.head = first1;
		 */
	}

	@Override
	public FuncTypeMany copyTypeSpecific() {
		ArrayList<FuncType> copy = new ArrayList<FuncType>(many.size());
		FuncType first1 = null;
		for(FuncType m : many)
		{
			FuncType cpy = (FuncType)m.copy();
			if(null == first1)
			{
				first1 = cpy;
			}
			copy.add(cpy);
		}
		return new FuncTypeMany(copy, first1);
	}
	
	@SuppressWarnings("unchecked")
	public FuncType clone()
	{//JPT: please forgive me, this is terrible!
		return this.head.clone();
	}
	
	public List<FuncType> getMany()
	{
		return this.many;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this.head);
	}
	@Override
	public String getPrettyName() {
		return this.head.getPrettyName();
	}
	
	
	@Override
	public void setArrayLevels(int levels) {
		this.head.arrayLevels= levels;
		for (FuncType m: many)
		{
			m.setArrayLevels(levels);
		}
		this.arrayLevels = levels;//ugh
	}

	@Override
	public int getArrayLevels() {
		return this.head.arrayLevels;
	}

	@Override
	public boolean hasArrayLevels() {
		return this.head.arrayLevels > 0;
	}
	

	@Override
	public String getBytecodeTypeWithoutArray( ) {
		return head.getBytecodeTypeWithoutArray();
	}

	@Override
	public String getGenericBytecodeTypeWithoutArray() {
		return head.getGenericBytecodeTypeWithoutArray();
	}
	
	@Override
	public TypeAndLocation getLambdaDetails() {
		return this.head.getLambdaDetails();
	}

	@Override
	public Type getSelf(){
		return this.head;
	}
	
	@Override
	public void setOrigonalGenericTypeUpperBound(NamedType origonalGenericTypeUpperBound) {
		super.setOrigonalGenericTypeUpperBound(origonalGenericTypeUpperBound);
		if(many != null){
			for(FuncType m: many){
				m.setOrigonalGenericTypeUpperBound( origonalGenericTypeUpperBound);
			}
		}
	}
	
	@Override
	public boolean getIsTypeInFuncref() {
		return this.head.getIsTypeInFuncref();
	}
	
	@Override
	public NullStatus getNullStatus() {
		return this.head.getNullStatus();

	}

	@Override
	public void setNullStatus(NullStatus nullStatus) {
		for (FuncType m: many)
		{
			m.setNullStatus(nullStatus);
		}
	}
}

