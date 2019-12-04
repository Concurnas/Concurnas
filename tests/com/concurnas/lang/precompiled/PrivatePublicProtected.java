package com.concurnas.lang.precompiled;

public class PrivatePublicProtected {

	public static class Cls{
		public void methpub() {}
		protected void methprotect() {}
		private void methprivat() {}
		void methdefault() {}
	}
	
	public static class MollyPreDef{
		public int x = 12;
		private MollyPreDef(int x){
			
		}
		
		protected MollyPreDef(int x , int y ){
			
		}		
		
		public MollyPreDef(){
		}
		
	}
	
	private static int privModLevel =99;
	public static int publicModLevel =99;
	
	public static class Vars{
		public int x  =8;
		protected int y  = 10;
		private int z  = 12;
	}
	
	private static String aFunctionPrivate(String input){
		return "oops";
	}
	
	private static String aFunctionProtected(String input){
		return "oops";
	}
	
	private static String aFunction(String input){
		return "oops";
	}
	
	public static String aFunction(Object input){
		return "Good Job";
	}
	
}
