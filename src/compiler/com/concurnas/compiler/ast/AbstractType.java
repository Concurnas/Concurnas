package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.visitors.ScopeAndTypeChecker;

public abstract class AbstractType extends Node implements Type {
	
	public AbstractType(int line, int column) {
		super(line, column);
	}
	
	
	public GPUVarQualifier gpuMemSpace = null;
	public GPUVarQualifier getGpuMemSpace() {
		return gpuMemSpace;
	}
	public void setGpuMemSpace(GPUVarQualifier gpuMemSpace) {
		this.gpuMemSpace = gpuMemSpace;
	}
	

	public Type originRefType = null;
	protected InoutGenericModifier inout;
	
	public void setInOutGenModifier(InoutGenericModifier inout){
		if(this instanceof PrimativeType){
			return;
		}
		
		this.inout = inout;
	}
	public InoutGenericModifier getInOutGenModifier(){
		return inout;
	}
	
	
	private boolean isAutoGennerated = false;
	
	@Override
	public boolean getAutoGennerated() {
		return isAutoGennerated;
	}

	@Override
	public void setAutoGenenrated(boolean isAutoGennerated) {
		this.isAutoGennerated = isAutoGennerated;
	}
	
	@Override
	public int getArrayLevelsRefOVerride() {
		return this.getArrayLevels();
	}
	
	@Override
	public String getCheckCastType()
	{
		return this.getBytecodeType();
	}
	
	protected boolean isLHSClass;
	
	public void setIsLHSClass(boolean isLHSClass){
		this.isLHSClass = isLHSClass;
	}
	
	public boolean getIsLHSClass(){
		return isLHSClass;
	}

	public NamedType origonalGenericTypeUpperBound = null;

	@Override
	public final NamedType getOrigonalGenericTypeUpperBoundRaw() {
		return origonalGenericTypeUpperBound;
	}
	
	@Override
	public NamedType getOrigonalGenericTypeUpperBound() {
		if(null == origonalGenericTypeUpperBound){
			return (NamedType)ScopeAndTypeChecker.const_object.copy();
		}else{
			return origonalGenericTypeUpperBound;
		}
	}

	@Override
	public void setOrigonalGenericTypeUpperBound(NamedType origonalGenericTypeUpperBound) {
		this.origonalGenericTypeUpperBound = origonalGenericTypeUpperBound;
	}

	protected abstract String getBytecodeTypeWithoutArray( );
	public abstract String getGenericBytecodeTypeWithoutArray();
	
	protected String prependArrayLevels()
	{
		if(this.hasArrayLevels() )//&& this.getOrigonalGenericTypeUpperBound() ==null )
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
	public String getBytecodeType( )
	{
		StringBuilder sb = new StringBuilder();
		
		NamedType upperBound = getOrigonalGenericTypeUpperBoundRaw();
		
		if(upperBound != null){
				for(int n =0; n < upperBound.getArrayLevels(); n++)
				{
					sb.append("[");
				}
			
			return  sb + getBytecodeTypeWithoutArray();
		}
		else{
			return prependArrayLevels() + getBytecodeTypeWithoutArray();
		}
		
		
	}
	
	public final String getGenericBytecodeTypeNoGens() {
		return prependArrayLevels() + getGenericBytecodeTypeWithoutArray();
	}
	
	@Override
	public final String getGenericBytecodeType()
	{
		InoutGenericModifier gm = this.getInOutGenModifier();
		
		if(null != gm) {
			return gm.typePrefix() + getGenericBytecodeTypeNoGens();
		}else {
			return getGenericBytecodeTypeNoGens();
		}
	}

	public AbstractType getSelfOrElectChoice()
	{
		return this;//Override this in order to choose from many different tpyes (NamedTypeMany in mind)
	}
	

	public Type getSelf(){
		return this;
	}
	
	public abstract String getJavaSourceType();
	public abstract String getJavaSourceTypeNoArray();
	
	protected boolean isPartOfVarargArray=false;
	
	public void setIsPartOfVarargArray(boolean isPartOfVarargArray){
		this.isPartOfVarargArray = isPartOfVarargArray;
	}
	public boolean getIsPartOfVarargArray(){
		return isPartOfVarargArray;
	}

	private Vectorization vectorized=null;
	
	public Vectorization setVectorized(Vectorization vec){
		Vectorization prev = vectorized;
		this.vectorized = vec;
		return prev;
	}
	
	public Vectorization getVectorized(){
		return this.vectorized;
	}
	
	public boolean isVectorized(){
		return this.vectorized != null;
	}
	public Object isVectorizedToString() {
		return this.vectorized != null?vectorized.toString():"";
	}
	

	@Override
	public Type getTaggedType(){
		return this;
	}
	
	private int pointer = 0;

	@Override
	public void setPointer(int pp) {
		pointer = pp;
	}

	@Override
	public int getPointer() {
		return pointer;
	}
	
	protected boolean iIsTypeInFuncref;
	@Override
	public void setIsTypeInFuncref(boolean truth) {
		this.iIsTypeInFuncref = truth;
	}
	
	@Override
	public boolean getIsTypeInFuncref() {
		return this.iIsTypeInFuncref;
	}
	
	private boolean ignoreForLocalGenericInference=false;
	@Override
	public void setIgnoreForLocalGenericInference(boolean b) {
		ignoreForLocalGenericInference=b;
	}
	
	@Override
	public boolean getIgnoreForLocalGenericInference() {
		return ignoreForLocalGenericInference;
	}
	
	protected NullStatus nullStatus = NullStatus.NOTNULL;
	
	public NullStatus getNullStatus() {
		return this.nullStatus;
	}
	public void setNullStatus(NullStatus nullStatus) {
		this.nullStatus = nullStatus;
	}

	protected int arrayLevels = 0;
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
	
	
	protected void cloneMeTo(AbstractType instance) {
		instance.nsforArrayLevels = null==nsforArrayLevels?null:new ArrayList<NullStatus>(nsforArrayLevels);
	}
	
	
	protected List<NullStatus> nsforArrayLevels = null;
	
	@Override
	public List<NullStatus> getNullStatusAtArrayLevel(){
		int len = this.getArrayLevels();
		if(null == nsforArrayLevels) {
			nsforArrayLevels = new ArrayList<NullStatus>(len);
			for(int n=0; n < len; n++) {
				nsforArrayLevels.add(NullStatus.NOTNULL);
			}
		}else {
			int clen=0;
			try {
				clen = nsforArrayLevels.size();
			}catch(Exception e) {
				throw e;
			}
			
			
			if(clen > len) {//trim
				nsforArrayLevels = new ArrayList<NullStatus>(nsforArrayLevels.subList(0, clen-1));
			}else if(clen < len) {//extend
				for(; clen < len; clen++) {
					nsforArrayLevels.add(NullStatus.NOTNULL);
				}
			}
		}
		return nsforArrayLevels;
	}
	
	@Override
	public void setNullStatusAtArrayLevel(NullStatus nullable) {
		int len = this.getArrayLevels();
		if(null == nsforArrayLevels) {
			nsforArrayLevels = new ArrayList<NullStatus>(len);
			for(int n=0; n < len; n++) {
				if(0 == n) {
					nsforArrayLevels.add(nullable);
				}else {
					nsforArrayLevels.add(NullStatus.NOTNULL);
				}
			}
			
		}else {
			int clen = nsforArrayLevels.size();
			
			if(clen > len) {//trim
				nsforArrayLevels = new ArrayList<NullStatus>(nsforArrayLevels.subList(0, clen-1));
			}else if(clen < len) {//extend
				for(; clen < len; clen++) {
					nsforArrayLevels.add(NullStatus.NOTNULL);
				}
			}
			nsforArrayLevels.set(len-1, nullable);
		}
	}

	@Override
	public void setNullStatusAtArrayLevel(List<NullStatus> nullable) {
		nsforArrayLevels = nullable;
	}
	
}