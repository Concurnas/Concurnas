package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.Objects;

import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Visitor;

public class MultiType  extends AbstractType   {

	private int arrayLevels = 0;
	public ArrayList<Type> multitype;
	public boolean isValidAtThisLocation=false;
	//public boolean ignore;
	public Type astOverride;
	
	@Override
	public Node copyTypeSpecific() {
		MultiType prim = new MultiType(line, column,  (ArrayList<Type>) Utils.cloneArrayList(multitype));
		prim.isValidAtThisLocation=isValidAtThisLocation;
		prim.arrayLevels = arrayLevels;
		prim.iIsTypeInFuncref = iIsTypeInFuncref;
		if(astOverride != null) {
			prim.astOverride = (Type)astOverride.copy();
		}
		prim.nullStatus = this.nullStatus;
		super.cloneMeTo(prim);
		//prim.ignore=ignore;
		return prim;
	}

	public MultiType(int line, int col, ArrayList<Type> multitype) {
		super(line, col);
		this.multitype = multitype;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		if(null != astOverride) {
			return astOverride.accept(visitor);
		}
		
		return visitor.visit(this);
	}
	
	
	@Override
	public void setArrayLevels(int levels) {
		this.arrayLevels= levels;
	}

	@Override
	public int getArrayLevels() {
		return this.arrayLevels;
	}

	@Override
	public boolean hasArrayLevels() {
		return this.arrayLevels > 0;
	}
	
	@Override
	public boolean isChangeable() {
		return false;
	}
	
	@Override
	public String toString(){
		return getPrettyName() + (this.getNullStatus() == NullStatus.NULLABLE?"?":"");
	}

	@Override
	public int hashCode(){
		int acc = 0;
		for(Type mm : multitype) {
			acc += mm.hashCode();
		}
		acc += (int)this.arrayLevels;
		if(this.astOverride != null) {
			acc += this.astOverride.hashCode();
		}
		return acc ;
	}
	
	@Override
	public boolean equals(Object comp)
	{//TODO: should this compare array levels?
		if(comp instanceof MultiType)
		{
			MultiType asmt = (MultiType)comp;
			if(this.arrayLevels == asmt.arrayLevels) {
				int sz = asmt.multitype.size();
				if(sz == this.multitype.size()) {
					for(int n=0; n < sz; n++) {
						if(!asmt.multitype.get(n).equals(this.multitype.get(n))) {
							return false;
						}
					}
					
					return Objects.equals(this.astOverride, asmt.astOverride);
				}
			}
		}else if(this.astOverride != null) {
			return this.astOverride.equals(comp);
		}
		return false;
	}

	@Override
	public String getPrettyName() {
		int sz = this.multitype.size();
		StringBuilder sb = new StringBuilder(); 
		for(int n=0; n < sz; n++) {
			sb.append(this.multitype.get(n).getPrettyName());
			if(n != sz-1) {
				sb.append("|");
			}
		}
		int m=0;
		while(m++ < this.arrayLevels) {
			sb.append("[]");
		}
		
		return sb.toString();
	}

	@Override
	public String getNonGenericPrettyName() {
		return "";
	}

	@Override
	protected String getBytecodeTypeWithoutArray() {
		return "";
	}

	@Override
	public String getGenericBytecodeTypeWithoutArray() {
		StringBuilder sb = new StringBuilder();
		
		int sz = this.multitype.size();
		for(int n=0; n < sz; n++) {
			sb.append( ((AbstractType)this.multitype.get(n)).getGenericBytecodeTypeWithoutArray() );
			if(n != sz -1) {
				sb.append("|");
			}
		}
		
		return sb.toString();
	}

	@Override
	public String getJavaSourceType() {
		return "";
	}

	@Override
	public String getJavaSourceTypeNoArray() {
		return "";
	}
	

	@Override
	public String getCheckCastType() {
		if(this.astOverride != null){
			return astOverride.getCheckCastType();
		}
		return this.getCheckCastType();
	}

	@Override
	public Type getTaggedType(){
		if(this.astOverride != null){
			return astOverride;
		}
		return this;
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
