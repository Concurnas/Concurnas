package com.concurnas.compiler.ast;

import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;

public class GenericType extends AbstractType implements Comparable<GenericType> {

	public String name;
	public int genIndex;
	public NamedType upperBound = new NamedType(0,0, new ClassDefJava(java.lang.Object.class));
	public boolean isInstantiable = true;
	public boolean splicedIn = false;
	public boolean isWildcard = false;
	public boolean isReturnedUnqualifiedLocalGeneric = false;

	
	private static final NamedType refObje =  new NamedType(0,0, new ClassDefJava(java.lang.Object.class));

	@Override
	public int compareTo(GenericType o) {
		return this.toString().compareTo(o.toString());
	}
	
	public GenericType(String name, int genIndex)
	{//TODO: permit upper bound. also do complex stuff such as class MyList[X extends Set<String>] extends List[X]{};
		this(0,0, name,genIndex);
	}
	
	public GenericType(int line, int col,String name, int genIndex)
	{//TODO: permit upper bound. also do complex stuff such as class MyList[X extends Set<String>] extends List[X]{};
		super(line,col);
		this.name = name;
		this.genIndex = genIndex;
		
		if(name.equals("?")) {
			isWildcard = true;
		}
	}
	
	@Override
	public void setOrigonalGenericTypeUpperBound(NamedType upperbound){}
	
	@Override 
	public NamedType getOrigonalGenericTypeUpperBound(){
		return upperBound;
	}
	
	public GenericType clone()
	{
		GenericType ret = new GenericType(this.getLine(), this.getColumn(), name, this.genIndex);
		ret.setArrayLevels(this.getArrayLevels());
		ret.splicedIn=splicedIn;
		ret.isWildcard=isWildcard;
		ret.inout=inout;
		ret.iIsTypeInFuncref=iIsTypeInFuncref;
		ret.isLHSClass=isLHSClass;
		ret.isReturnedUnqualifiedLocalGeneric = isReturnedUnqualifiedLocalGeneric;
		ret.isPartOfVarargArray = isPartOfVarargArray;//relevant?
		if(null != upperBound && !refObje.equals(upperBound)){
			
			NamedType cpy = upperBound;
			if(!cpy.equals(ScopeAndTypeChecker.const_Enum)){
				cpy = cpy.copyTypeSpecific();
			}
			
			ret.upperBound = cpy;
		}
		ret.origonalGenericTypeUpperBound = this.origonalGenericTypeUpperBound == null?null:this.origonalGenericTypeUpperBound.copyTypeSpecific();
		ret.setVectorized(super.getVectorized());//?
		ret.nullStatus = this.nullStatus;
		super.cloneMeTo(ret);
		return ret;
	}
	
	public String getPrettyName() {
		return this.toString();
	}
	
	@Override
	public GenericType copyTypeSpecific() {
		return this.clone();
	}

	@Override
	public String toString()	{
		return toStringOptName(true);
	}
	
	public String toStringOptName(boolean includeName) {
		InoutGenericModifier inoutgen = super.getInOutGenModifier();
		StringBuilder sb = null != inoutgen?new StringBuilder(inoutgen.toString()+ " "):new StringBuilder();
		
		if(includeName) {
			sb.append(this.name);
		}
		
		if(name.equals("?") || (this.upperBound != null && !this.upperBound.equals(ScopeAndTypeChecker.const_object))){
			sb.append(" ");
			sb.append(upperBound.namedType);
		}
		
		
		for(int n=0; n < this.arrayLevels; n++)
		{
			sb.append("[]");
		}
		
		return sb.toString() + (this.getNullStatus() == NullStatus.NULLABLE?"?":"");
	}
	
	
	public String getNonGenericPrettyName() {
		return this.name;
		//throw new RuntimeException("method 'getNonGenericPrettyName', not implemented for GenericType");
	}

	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}


	public boolean isChangeable() {
		throw new RuntimeException("method 'isChangeable' not implemented for GenericType");
	}
	
	/*@Override
	public String getBytecodeType( )
	{
		if(this.hasArrayLevels()){
			StringBuilder sb = new StringBuilder();
			for (int n = 0; n < this.getArrayLevels(); n++) {
				sb.append("[");
			}
			return sb + "L"+this.upperBound.getCheckCastType() + ";";
		}
		
		else{
			return super.getBytecodeType();
		//}
	}*/
	
	@Override
	public String getCheckCastType() {
		if(this.hasArrayLevels()){
			StringBuilder sb = new StringBuilder();
			for (int n = 0; n < this.getArrayLevels(); n++) {
				sb.append("[");
			}
			return sb + "L"+this.getUpperBoundAsNamedType().getCheckCastType() + ";";
		}
		
		else{
			return this.getUpperBoundAsNamedType().getCheckCastType();
		}
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if(other instanceof MultiType) {
			other = ((MultiType)other).getTaggedType();
		}
		
		if(other instanceof GenericType )
		{
			GenericType otherGen = (GenericType)other;
			return otherGen.name.equals(this.name) && this.isInstantiable == otherGen.isInstantiable;//possible that we use as placeholder
		}
		else if (other instanceof NamedType )
		{//MHA: we add this caluse because otherwise when we have a generic constructor the constuctors get n-plicated
			NamedType ont = (NamedType)other;
			return name.equals(ont.getNamedTypeStr());
		}
		return false;
	}
	
	@Override
	public String getBytecodeTypeWithoutArray( ) {
		return upperBound.getBytecodeTypeWithoutArray();
	}

	@Override
	public String getGenericBytecodeTypeWithoutArray() {
		if(name.equals("?")) {
			return "*";
		}else {
			return "T"+name + ";";
		}
	}
	
	@Override
	public String getJavaSourceType() {
		return this.name;
	}
	
	@Override
	public String getJavaSourceTypeNoArray() {
		return getJavaSourceType();
	}
	
	public static NamedType object_const = new NamedType(new ClassDefJava(java.lang.Object.class));

	public Type getUpperBoundAsNamedType() {
		if(this.upperBound != null) {
			return this.upperBound.copyTypeSpecific();
		}
			
		return object_const;
	}

	@Override
	public Type copyIgnoreReturnTypeAndGenerics() {
		return object_const;
	}
	
	@Override
	public Type copyIgnoreReturnType() {
		return object_const;
	}

	

	/*@Override
	public Type getTaggedType(){
		return getUpperBoundAsNamedType();
	}*/
	
}
