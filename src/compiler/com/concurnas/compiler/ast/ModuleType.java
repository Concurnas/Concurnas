package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.Objects;

import com.concurnas.compiler.visitors.Visitor;

/**
 * A royal hack
 * @author jason
 *
 */
public class ModuleType extends AbstractType  {

	private final String moduleNameSoFar;
	
	public ModuleType(int line, int column, String name)
	{
		super(line,column);
		this.moduleNameSoFar = name;
	}
	
	@Override
	public boolean equals(Object an) {
		if(an instanceof ModuleType) {
			return Objects.equals(moduleNameSoFar, ((ModuleType)an).moduleNameSoFar);
		}
		return false;
	}
	
	@Override
	public ModuleType copyTypeSpecific() {
		return new ModuleType(super.getLine(), super.getColumn(), moduleNameSoFar);
	}
	
	@Override
	public String getPrettyName() {
		return this.moduleNameSoFar;
	}
	
	@Override
	public String toString() {
		//this is a super hack
		return String.format("%s cannot be resolved to a variable" , this.getPrettyName() );
	}

	@Override
	public String getNonGenericPrettyName() {
		return null;
	}

	@Override
	public Object accept(Visitor printSourceTestVisitor) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setArrayLevels(int levels) {
		//throw new RuntimeException("Unable to set arraylevels on module definition: " + this.moduleNameSoFar);
	}

	@Override
	public int getArrayLevels() {
		return 0;
	}

	@Override
	public boolean hasArrayLevels() {
		return false;
	}

	@Override
	public boolean isChangeable() {
		return false;
	}
	
	public String getNameSoFar()
	{
		return this.moduleNameSoFar;
	}
	
	@Override
	public String getBytecodeTypeWithoutArray() {throw new RuntimeException("getBytecodeType not callable");
	}

	@Override
	public String getGenericBytecodeTypeWithoutArray() {
		return "ERROR";//throw new RuntimeException("getBytecodeType not callable");
	}
	
	@Override
	public String getJavaSourceType() {
		return "ERROR";
	}
	
	@Override
	public String getJavaSourceTypeNoArray() {
		return getJavaSourceType();
	}
	
	@Override
	public Type getTaggedType(){
		return null;
	}

	@Override
	public Type copyIgnoreReturnTypeAndGenerics() {
		return copyTypeSpecific();
	}
	
	@Override
	public Type copyIgnoreReturnType() {
		return copyTypeSpecific();
	}
}
