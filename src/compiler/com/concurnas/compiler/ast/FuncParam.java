package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;

public class FuncParam extends Node implements HasAnnotations, Comparable<FuncParam> {

	public String name;
	public Type type;
	public boolean isFinal;
	public boolean isShared;
	public boolean isLazy;
	public boolean isVararg;
	public Integer sytheticDefinitionLevel;

	public Annotations annotations;
	public Expression defaultValue = null;
	public boolean defaultOk;
	//public RefName nonLocalVariableResolvesTo = null;
	public GPUVarQualifier gpuVarQualifier = null;
	public GPUInOutFuncParamModifier gpuInOutFuncParamModifier;
	public boolean argNameKnown = true;
	public String namedAnnotationName;
	public String fromSOname = null;
	
	
	public FuncParam(int line, int col, String name, Type type, boolean isFinal) {
		//TODO: add isVolatile
		super(line, col);
		this.name = name;
		this.type = type;
		this.isFinal = isFinal;
	}
	
	public FuncParam(int line, int col, String name, Type type, boolean isFinal, int sytheticDefinitionLevel) {
		this(line, col, name, type, isFinal);
		this.sytheticDefinitionLevel = sytheticDefinitionLevel;
	}
	
	
	@Override
	public void setAnnotations(Annotations annotations) {
		this.annotations=annotations;
	}
	
	@Override
	public Annotations getAnnotations(){
		return annotations;
	}
	
	@Override
	public Node copyTypeSpecific() {
		FuncParam ret = new FuncParam(line, column, name, type==null?null:(Type)type.copy(), isFinal);
		ret.sytheticDefinitionLevel=this.sytheticDefinitionLevel;
		ret.annotations=annotations==null?null:(Annotations)this.annotations.copy();
		ret.defaultValue = defaultValue==null?null:(Expression)defaultValue.copy();
		ret.isVararg=isVararg;
		ret.gpuVarQualifier=gpuVarQualifier;
		ret.isShared=isShared;
		ret.isLazy=isLazy;
		ret.gpuInOutFuncParamModifier=gpuInOutFuncParamModifier;
		ret.argNameKnown=argNameKnown;
		ret.namedAnnotationName=namedAnnotationName;
		ret.fromSOname=fromSOname;
		//ret.nonLocalVariableResolvesTo=nonLocalVariableResolvesTo==null?null:(RefName)nonLocalVariableResolvesTo.copy();
		return ret;
	}
	
	public FuncParam copyWithName(String name) {
		FuncParam ret = new FuncParam(line, column, name, type==null?null:(Type)type.copy(), isFinal);
		ret.sytheticDefinitionLevel=this.sytheticDefinitionLevel;
		ret.annotations=annotations==null?null:(Annotations)this.annotations.copy();
		ret.defaultValue = defaultValue==null?null:(Expression)defaultValue.copy();
		ret.defaultOk=defaultOk;
		ret.isVararg=isVararg;
		ret.gpuVarQualifier=gpuVarQualifier;
		ret.isShared=isShared;
		ret.isLazy=isLazy;
		ret.fromSOname=fromSOname;
		ret.gpuInOutFuncParamModifier=gpuInOutFuncParamModifier;
		//ret.nonLocalVariableResolvesTo=nonLocalVariableResolvesTo==null?null:(RefName)nonLocalVariableResolvesTo.copy();
		return ret;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public boolean equals(Object comp)
	{
		if(comp instanceof FuncParam)
		{//check closure only
			FuncParam comppd = ((FuncParam)comp);
			
			if(comppd.type == null || this.type==null){
				return comppd.type == this.type;
			}
			
			return comppd.type.getNonGenericPrettyName().equals(this.type.getNonGenericPrettyName()) && this.isVararg == comppd.isVararg;
		}
		return false;
	}
	
	public boolean equalsCheckName(Object comp)	{
		return this.equals(comp) && ( ((FuncParam)comp).name.equals(this.name) );
	}
	
	@Override
	public int hashCode()
	{
		//return this.toString().hashCode();
		if(this.getTaggedType()==null) {
			return 0;
		}else {
			String gnos = this.getTaggedType().getNonGenericPrettyName();
			if(null == gnos) {
				return 0;
			}else {
				return gnos.hashCode();
			}
		}
	}
	
	@Override
	public String toString()
	{
		return String.format("%s%s%s%s%s %s%s%s", this.gpuVarQualifier==null?"":this.gpuVarQualifier + " ", this.isLazy?"lazy ":"",this.isShared?"shared ":"", this.gpuInOutFuncParamModifier==null?"":this.gpuInOutFuncParamModifier + " ", name, type==null?"":type.toString(), this.isVararg?"...":"" ,this.defaultValue != null?"="+this.defaultValue:"=null");
	}

	public void setDefaultValue(Expression defaultValue) {
		this.defaultValue =defaultValue;
	}
	
	
	public Type getTaggedType(){
		Type ret = super.getTaggedType();
		return ret==null?type:ret;
	}

	public void addAnnotation(Annotation annotation) {
		if(this.annotations == null){
			annotations = new Annotations();
			annotations.annotations.add(annotation);
		}
		
	}

	public boolean hasSyntheticParamAnnotation(){
		if(this.annotations != null){
			for(Annotation annot : this.annotations.annotations){
				if(annot.className.equals("com.concurnas.lang.SyntheticParam")){
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public int compareTo(FuncParam o) {
		return this.toString().compareTo(o.toString());
	}

	
}
