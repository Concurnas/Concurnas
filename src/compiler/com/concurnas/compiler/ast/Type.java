package com.concurnas.compiler.ast;

import java.util.List;

import com.concurnas.compiler.visitors.Visitor;

public interface Type extends Copyable{
	public void setIsTypeInFuncref(boolean truth);
	public boolean getIsTypeInFuncref();
	public String getPrettyName();
	public String getNonGenericPrettyName();
	//public boolean isChangeable();
	public Object accept(Visitor printSourceTestVisitor);
	public void setArrayLevels(int levels);
	public int getArrayLevels();
	//used for when figuring out the type of something and dont caare about ref contents in LCA...
	public int getArrayLevelsRefOVerride();
	public boolean hasArrayLevels();
	public boolean isChangeable();
	public boolean getAutoGennerated();
	public void setAutoGenenrated(boolean x);
	//public boolean isAutoGennerated = false;
	public String getBytecodeType();
	public String getGenericBytecodeType();
	public Type getSelfOrElectChoice();
	//public Type copy();
	public NamedType getOrigonalGenericTypeUpperBound();
	public void setOrigonalGenericTypeUpperBound(NamedType origonalGenericTypeUpperBound);
	public NamedType getOrigonalGenericTypeUpperBoundRaw();
	
	public String getCheckCastType();
	public Type getSelf();
	public String getJavaSourceType();
	public String getJavaSourceTypeNoArray();
	public void setLine(int line);
	public void setColumn(int line);
	public void setIsLHSClass(boolean a);
	public boolean getIsLHSClass();
	
	public void setIsPartOfVarargArray(boolean xxx);
	public boolean getIsPartOfVarargArray();
	public void setInOutGenModifier(InoutGenericModifier inout);
	public InoutGenericModifier getInOutGenModifier();
	
	public static enum Vectorization{
		NORMAL("^"), SELF("^^");
		private final String ts;

		private Vectorization(String ts){
			this.ts=ts;
		}
		@Override public String toString(){
			return this.ts;
		}
	}
	
	public Vectorization setVectorized(Vectorization vec);
	public Vectorization getVectorized();
	public boolean isVectorized();
	public Type getTaggedType();
	
	public GPUVarQualifier getGpuMemSpace();
	public void setGpuMemSpace(GPUVarQualifier gpuMemSpace);
	public void setPointer(int pp);
	public int getPointer();
	public Type copyIgnoreReturnTypeAndGenerics();
	public Type copyIgnoreReturnType();
	public void setIgnoreForLocalGenericInference(boolean b);
	public boolean getIgnoreForLocalGenericInference();
	
	public NullStatus getNullStatus();
	public void setNullStatus(NullStatus what);
	public List<NullStatus> getNullStatusAtArrayLevel();
	public void setNullStatusAtArrayLevel(NullStatus nullable);
	public void setNullStatusAtArrayLevel(List<NullStatus> nullable);
	
}
