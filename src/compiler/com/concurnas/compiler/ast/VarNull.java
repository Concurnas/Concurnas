package com.concurnas.compiler.ast;

import java.util.List;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class VarNull extends AbstractType implements Expression {
	public VarNull(int line, int column) {
		super(line, column);
		super.nullStatus = NullStatus.NULLABLE;
	}
	public VarNull() {
		this(0, 0);
	}
	
	@Override
	public boolean hasBeenVectorized(){
		return false;
	}

	@Override
	public VarNull copyTypeSpecific( ) {
		VarNull ret = new VarNull(this.line, this.column);
		ret.setArrayLevels(this.getArrayLevels());
		ret.isLHSClass=isLHSClass;
		ret.isPartOfVarargArray = isPartOfVarargArray;
		ret.inout = inout;
		ret.iIsTypeInFuncref = iIsTypeInFuncref;
		ret.forceCheckCast = forceCheckCast==null?null:(Type)forceCheckCast.copy();
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		ret.setVectorized(super.getVectorized());//?
		ret.setTaggedType(super.getTaggedTypeRaw());
		ret.setNullStatus(this.getNullStatus());
		super.cloneMeTo(ret);

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
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}

	@Override
	public String getPrettyName() {
		StringBuilder sb = new StringBuilder("null");
		
		if(this.arrayLevels >0)
		{
			for(int n=0; n < this.arrayLevels; n++ )
			{
				sb.append("[]");
			}
		}
		
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return getPrettyName();
	}
	
	@Override
	public boolean equals(Object what) {
		return what instanceof VarNull;
	}
	
	public List<Type> entwineOnRefMutateSet;
	public Type forceCheckCast = null;
	
	@Override
	public String getNonGenericPrettyName() {
		return "null";
	}

	@Override
	public boolean isChangeable() {
		return false;
	}

	private static final ClassDefJava obj = new ClassDefJava(Object.class);
	
	
	@Override
	public String getBytecodeTypeWithoutArray( ) {
		/*if(this.hasArrayLevels()){
			return "Ljava/lang/Object;";//MHA
		}
		return "java/lang/Object";*/
		
		/*Type tt = this.getTaggedTypeRaw();
		if(tt != null && tt instanceof NamedType) {
			return ((NamedType)tt).getBytecodeTypeWithoutArray();
		}*/
		
		return "Ljava/lang/Object;";//MHA
	}
	
	@Override
	public String getBytecodeType() {
		Type tt = this.getTaggedTypeRaw();
		if(tt != null && tt instanceof NamedType) {
			return ((NamedType)tt).getBytecodeType();
		}

		String ret = "Ljava/lang/Object;";
		for(int n = 0; n < this.arrayLevels; n++) {
			ret = "[" + ret;
		}
		
		return ret;//MHA
	}

	@Override
	public String getCheckCastType() {
		Type tt = this.getTaggedTypeRaw();
		if(tt != null && tt instanceof NamedType) {
			return ((NamedType)tt).getCheckCastType();
		}

		return "java/lang/Object";//MHA
	}
	
	@Override
	public String getGenericBytecodeTypeWithoutArray() {
		return getBytecodeType();
	}
	
	@Override
	public String getJavaSourceType() {
		return "null";
	}	
	
	@Override
	public String getJavaSourceTypeNoArray() {
		return "null";
	}
	
	/*@Override
	public Type getTaggedType() {
		return ScopeAndTypeChecker.const_object;
	}*/

	
	
	
	@Override
	public Type copyIgnoreReturnTypeAndGenerics() {
		return (Type)copyTypeSpecific();
	}
	
	@Override
	public Type copyIgnoreReturnType() {
		return (Type)copyTypeSpecific();
	}

	public NullStatus getNullStatus() {
		if(this.hasArrayLevels()) {
			return NullStatus.NONNULL;
		}
		return NullStatus.NULLABLE;
	}
}
