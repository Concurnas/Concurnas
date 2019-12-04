package com.concurnas.compiler.ast;

import com.concurnas.compiler.visitors.Visitor;

public class PrimativeType  extends AbstractType   {

	public PrimativeTypeEnum type;

	public boolean thrown=false;
	public boolean errored=false;
	
	
	@Override
	public Node copyTypeSpecific() {
		PrimativeType prim = new PrimativeType(this.line, this.column, type);
		prim.setArrayLevels(this.arrayLevels);
		prim.origonalGenericTypeUpperBound = origonalGenericTypeUpperBound;
		prim.thrown = thrown;
		prim.isLHSClass = isLHSClass;
		prim.isPartOfVarargArray = isPartOfVarargArray;
		prim.inout = inout;
		prim.setVectorized(super.getVectorized());
		prim.gpuMemSpace= gpuMemSpace;
		prim.iIsTypeInFuncref= iIsTypeInFuncref;
		prim.setPointer(this.getPointer());
		prim.nullStatus = this.nullStatus;
		super.cloneMeTo(prim);
		return prim;
	}

	public PrimativeType( PrimativeTypeEnum type) {
		this(0,0, type);
	}
	
	public PrimativeType(int line, int col, PrimativeTypeEnum type) {
		super(line, col);
		this.type = type;
	}
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public String getPrettyName() {
		InoutGenericModifier inoutgen = super.getInOutGenModifier();
		StringBuilder sb = null != inoutgen?new StringBuilder(inoutgen.toString()+ " "):new StringBuilder();
		
		int pnt = this.getPointer();
		String str = "";
		
		if(super.gpuMemSpace != null) {
			str += super.gpuMemSpace + " ";
		}
		
		if(pnt != 0) {
			for(int n =0; n < pnt; n++) {
				str += "*";
			}
			sb.append(str);
		}
			
		sb.append(type==null?"null":type.toString());
		
		if(this.arrayLevels >0)
		{
			if(this.arrayLevels == 1)
			{
				sb.append("[]");//TODO: show this as xxx[1] not xxx[]
			}
			else
			{
				sb.append("[" + this.arrayLevels + "]");
			}
		}
		
		sb.append(super.isVectorizedToString());
		
		return sb.toString();
	}
	

	
	@Override
	public String getNonGenericPrettyName() {
		return ""+this.type.getNonGenericPrettyName();
	}
	@Override
	public boolean isChangeable() {
		return false;
	}
	
	@Override
	public String toString()
	{
		return getPrettyName() + (this.getNullStatus() == NullStatus.NULLABLE?"?":"");
	}
	

	@Override
	public int hashCode()
	{
		return this.type.hashCode() + (int)this.arrayLevels;
	}
	
	@Override
	public boolean equals(Object comp)
	{//TODO: should this compare array levels?
		if(comp instanceof PrimativeType){
			PrimativeType compp = (PrimativeType)comp;
			return compp.type == this.type 
					&& this.arrayLevels == compp.arrayLevels
					&& this.thrown == compp.thrown
					&& this.errored == compp.errored
					&& this.getPointer() == compp.getPointer()
					&& super.getVectorized() == compp.getVectorized();
		}
		return false;
	}
	
	@Override
	public String getBytecodeTypeWithoutArray( ) {
		if(null != getOrigonalGenericTypeUpperBound())
		{
			return origonalGenericTypeUpperBound.getSetClassDef().javaClassName();
		}
		
		return this.type.getBytecodeType();
	}

	private String prependArrayLevelsForCastType()
	{
		if(this.hasArrayLevels())
		{
			StringBuilder sb = new StringBuilder();
			for(int n =0; n < this.getArrayLevels(); n++)
			{
				sb.append('[');
			}
			return sb.toString();
		}
		else
		{
			return "";
		}
	}
	
	@Override
	public String getCheckCastType() {
		if(this.type==PrimativeTypeEnum.LAMBDA){
			return "com/concurnas/bootstrap/lang/Lambda";
		}
		else{
			return  prependArrayLevelsForCastType() + this.type.getBytecodeType();
		}
		
	}
	
	@Override
	public NamedType getOrigonalGenericTypeUpperBound() {
		return hasArrayLevels()?origonalGenericTypeUpperBound:null;
	}
	
	@Override
	public String getGenericBytecodeTypeWithoutArray() {
		if(getOrigonalGenericTypeUpperBound() != null )
		{//TODO: copy pasted from functype (oooh bad) - consider refactoring int abstracttype
			return origonalGenericTypeUpperBound.getSetClassDef().javaClassName();
		}
		
		return getBytecodeTypeWithoutArray();
	}

	@Override
	public String getJavaSourceType() {
		if(type == null){
			return "void";
		}
		
		String ret =  type.getJavaName();
		if(this.hasArrayLevels())
		{
			int arLevels = this.getArrayLevels();
			for(int n =0; n < arLevels; n++){
				ret += "[]";
			}
		}
		return ret;
	}
	
	@Override
	public String getJavaSourceTypeNoArray() {
		return type.getJavaName();
	}
	
	@Override
	public Type copyIgnoreReturnTypeAndGenerics() {
		return (Type)copyTypeSpecific();
	}
	
	@Override
	public Type copyIgnoreReturnType() {
		return (Type)copyTypeSpecific();
	}
}
