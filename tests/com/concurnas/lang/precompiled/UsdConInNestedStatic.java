package com.concurnas.lang.precompiled;

/**
 * Test class used to validate behvaour of UsedPrivConstruFinder
 */
public class UsdConInNestedStatic {
	private UsdConInNestedStatic(){} //gets converted to public
	protected UsdConInNestedStatic(int x){} //gets converted to public
	
	public static void Metho(){
		UsdConInNestedStatic a = new UsdConInNestedStatic();//because used here
		UsdConInNestedStatic b = new UsdConInNestedStatic(2);
	}
	
	public static class Inner{//inside here
		public void Metho(){
			UsdConInNestedStatic a = new UsdConInNestedStatic();
			UsdConInNestedStatic b = new UsdConInNestedStatic(2);
		}
	}
	
	public static UsdConInNestedStatic insto = new UsdConInNestedStatic();//and inside the static clinit here as well
	public static UsdConInNestedStatic insto2 = new UsdConInNestedStatic(2);
	
	public String user(){
		UsdConInNestedStatic instoa = insto;
		UsdConInNestedStatic instob = insto2;
		Inner in = new Inner();
		in.Metho();
		Metho();
		
		return "ok";
	}
	
}
