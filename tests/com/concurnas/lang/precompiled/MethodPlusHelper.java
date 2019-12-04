package com.concurnas.lang.precompiled;

import com.concurnas.lang.ParamName;

public class MethodPlusHelper {

	private int b;
	private int a;

	public MethodPlusHelper(){}
	
	public MethodPlusHelper(@ParamName(name="a") int a, @ParamName(name="b") int b){
		this.a=a;
		this.b=b;
	}
	
	public String toString(){
		return String.format("its%s%s", a, b);
	}
	
	public int afunc(@ParamName(name="a") int a, @ParamName(name="b") int b, @ParamName(name="c") int c){
		return (int) (Math.pow(a, b)+c);
	}
	
	public int afuncLackInfo(int a, int b, int c){
		return (int) (Math.pow(a, b)+c);
	}
	
	public static interface Myface{
		public String leFunction(@ParamName(name="zc") int zc, @ParamName(name="zv") int zv);
		
	}
	
	public static class MyCls implements Myface{
		public String leFunction(@ParamName(name="a") int a, @ParamName(name="b") int b){
			return String.format("%s %s", a, b);
		}
		
	}
	
}
