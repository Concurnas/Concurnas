package com.concurnas.runtime.cps.mirrors;

import java.util.HashMap;
import java.util.HashSet;

public interface ClassMirror {

	public abstract MethodMirror[] getDeclaredMethods();
	
	public abstract MethodMirror getDeclaredMethod(String name, String desc);
	
	public abstract ConstructorMirror[] getDeclaredConstructorsWithoutHiddenArgs();

	public abstract boolean isAssignableFrom(ClassMirror c) throws ClassMirrorNotFoundException;

	public abstract String getSuperclass() throws ClassMirrorNotFoundException;
	
	public abstract String getSuperclassNoEx();

	public abstract String[] getInterfaces() throws ClassMirrorNotFoundException;

	public abstract boolean isInterface();

	public abstract String getName();

	public abstract String[] getInterfacesNoEx();

	public abstract int isNestedClass();
	
    public abstract boolean isEnum();
    
    public abstract HashSet<String> getAnnotations();
    
    public abstract String[] getGenericArguments();
    
    public abstract HashMap<String, FieldMirror> getFields();
    
    public abstract FieldMirror getField(String field);
}
