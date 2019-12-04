package com.concurnas.lang.precompiled;

public class InOutParams {

	public static class AGenericClass<X  >{
		public AGenericClass(X ini){
			this.x = ini;
		}
		
		public X x;
		public X getX(){
			return x;
		}
		
		public void setX(X a){
			this.x = a;
		}
		
		@Override 
		public String toString(){
			return x+"";
		}
	}
	
	public static class ExpClass<X, Y>{

		public String expiri(X x, Y y){
			return "" + x + " " + y;
		}
	
	}
	
	
	public static <Z> ExpClass<? extends Number, Z> getExp(){
		return new ExpClass<Integer, Z>();
	}
	
	public static AGenericClass<? extends Number> getOutInstance(){
		return new AGenericClass<Integer>(88);
	}
	
	public static AGenericClass<? super Integer> getInInstance(){
		return new AGenericClass<Number>(88);
	}
	
	public static AGenericClass<Number> getNormal(){
		return new AGenericClass<Number>(12);
	}
	
}
