package com.concurnas.lang.precompiled;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import com.concurnas.bootstrap.lang.Stringifier;

@Deprecated
public class AnnotationHelper {
	
	@Retention(RetentionPolicy.RUNTIME)
	public @interface AnnotSomeDefaults {
	    String firstName() default "Unknown";
	    String sirname();
	    String [] otherNames();
	    int age();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	public @interface SimpleAnnotation {
		
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	public @interface SimpleAnnotation2 {
		
	}
	@Retention(RetentionPolicy.RUNTIME)
	public @interface SimpleAnnotation3 {
		
	}
	
	public @interface AllowDupes {
		AllowDupe[] value();
	}
	
	//@Repeatable(AllowDupes.class)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface AllowDupe {//TODO: add support for this when we move to java8
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	public @interface AnnotOneArg {
		public String name();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	public @interface AnnotTwoArgOneDefault {
		public String name();
		public String name2() default "hi";
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	public @interface AnnotTwoArgAllDefault {
		public String name() default "hi1";
		public String name2() default "hi2";
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	public @interface AnnotTwoArg {
		public String name();
		public String name2();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	public @interface AnotTakesIntArray {
		public int[] theArg();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	public @interface AnotTakesIntArrayx2 {
		public int[] theArg();
		public int[] theArg2();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	public @interface TakesClass {
		public Class<?> theArg();
	}
	
	@Inherited
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface InheritedOne{
		public String value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	public @interface TakesEnum {
		public TheEnum theArg();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	public @interface TakesAnnotation {
		public TakesEnum theArg();
	}
	
	@Target(value=ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface FieldAnnot {
		public String name() default "nothing";
	}
	
	public enum TheEnum{ONE, TWO}
	
	public static class TestClass{
		public void leFunxc(@AnnotOneArg(name="hi") int a){}
	}
	
	@Deprecated
	public void canBeInspected(@Deprecated int a){
		
	}
	
	public static Method getMethodFromClass(Class<?> cls, String methodName){
		for(Method m : cls.getMethods()){
			if(m.getName().equals(methodName)){
				return m;
			}
		}
		
		return null;
	}
	
	public static String showAnnotations(Method m){
		StringBuilder sb = new StringBuilder();
		
		Annotation[] nn =  m.getAnnotations();
		for(int n=0; n< nn.length; n++){
			sb.append(Stringifier.stringify(nn[n]));//yeah, it handles annotations
			if(n < nn.length-1){
				sb.append(", ");
			}
		}
		
		return sb.toString();
	}
	
	
}
