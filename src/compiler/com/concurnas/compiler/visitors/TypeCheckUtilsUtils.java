package com.concurnas.compiler.visitors;

import java.util.HashMap;
import java.util.HashSet;

import com.concurnas.compiler.ast.PrimativeTypeEnum;

public class TypeCheckUtilsUtils {

	private static PrimativeTypeEnum booleana = PrimativeTypeEnum.BOOLEAN;
	private static PrimativeTypeEnum bytea = PrimativeTypeEnum.BYTE;
	private static PrimativeTypeEnum shorta = PrimativeTypeEnum.SHORT;
	private static PrimativeTypeEnum charactera = PrimativeTypeEnum.CHAR;
	private static PrimativeTypeEnum inta = PrimativeTypeEnum.INT;
	private static PrimativeTypeEnum size_t = PrimativeTypeEnum.SIZE_T;
	private static PrimativeTypeEnum longa = PrimativeTypeEnum.LONG;
	private static PrimativeTypeEnum floata = PrimativeTypeEnum.FLOAT;
	private static PrimativeTypeEnum doublea = PrimativeTypeEnum.DOUBLE;
	private static PrimativeTypeEnum lambdaa = PrimativeTypeEnum.LAMBDA;
	private static PrimativeTypeEnum voidp = PrimativeTypeEnum.VOID;
	
	private static String[] fiddle(String[] arg, String front)
	{
		String[] ret = new String[arg.length];
		ret[0] = front;
		//ret[1] = "java.lang.Number";
		int m=1;
		for(int n=0; n< ret.length; n++){
			String item = arg[n];
			if(!item.equals(front)){
				ret[m++] = item;
			}
		}
		return ret;
	}
	
	private static PrimativeTypeEnum[] fiddle(PrimativeTypeEnum[] arg, PrimativeTypeEnum front)
	{
		PrimativeTypeEnum[] ret = new PrimativeTypeEnum[arg.length];
		ret[0] = front;
		int m=1;
		for(int n=0; n< ret.length; n++){
			PrimativeTypeEnum item = arg[n];
			if(!item.equals(front)){
				ret[m++] = item;
			}
		}
		return ret;
	}
	
	public static HashMap<PrimativeTypeEnum, String[]> buildPrimsToBoxeds()
	{
		/*
		BOXED_TO_PRIMS.put("java.lang.Boolean",   booleana);
		BOXED_TO_PRIMS.put("java.lang.Byte",      bytea);
		BOXED_TO_PRIMS.put("java.lang.Short",     shorta);
		BOXED_TO_PRIMS.put("java.lang.Character", charactera);
		BOXED_TO_PRIMS.put("java.lang.Integer",   inta);
		BOXED_TO_PRIMS.put("java.lang.Long",      longa);
		BOXED_TO_PRIMS.put("java.lang.Float",     floata);
		BOXED_TO_PRIMS.put("java.lang.Double",    doublea);
		*/
		/*
		 byte to short, int, long, float, or double
		 
		short to int, long, float, or double, byte or char
		char to int, long, float, or double, byte or short
		int to long, float, or double, byte, short, or char
		long to float or double, byte, short, char, or int
		float to double,  byte, short, char, int, or long
		double to byte, short, char, int, long, or float
		
		 */
		
		HashMap<PrimativeTypeEnum, String[]> PRIMS_TO_BOXED = new HashMap<PrimativeTypeEnum, String[]>();
		
		String[] standard = new String[]{"java.lang.Number", "java.lang.Double", "java.lang.Float", "java.lang.Integer", "java.lang.Long", "java.lang.Byte", "java.lang.Short", "java.lang.Character"};
		String[] forByte =  new String[]{"java.lang.Number", "java.lang.Double", "java.lang.Float", "java.lang.Integer", "java.lang.Long", "java.lang.Byte", "java.lang.Short"};
		
		PRIMS_TO_BOXED.put(booleana,   new String[]{"java.lang.Boolean"});
		PRIMS_TO_BOXED.put(bytea,      fiddle(forByte, "java.lang.Byte"));//odd one out
		PRIMS_TO_BOXED.put(shorta,     fiddle(standard, "java.lang.Short"));
		PRIMS_TO_BOXED.put(charactera, fiddle(standard, "java.lang.Character"));
		PRIMS_TO_BOXED.put(inta,       fiddle(standard, "java.lang.Integer"));
		PRIMS_TO_BOXED.put(size_t,     standard);
		PRIMS_TO_BOXED.put(longa,      fiddle(standard, "java.lang.Long"));
		PRIMS_TO_BOXED.put(floata,     fiddle(standard, "java.lang.Float"));
		PRIMS_TO_BOXED.put(doublea,    fiddle( standard, "java.lang.Double"));
		PRIMS_TO_BOXED.put(lambdaa,    new String[]{"com.concurnas.bootstrap.lang.Lambda"});
		PRIMS_TO_BOXED.put(voidp,    new String[]{"java.lang.Void"});
		
		return PRIMS_TO_BOXED;
		
	}

	public static HashMap<String, PrimativeTypeEnum[]> buildToBoxedsPrims() {
		
		PrimativeTypeEnum booleana   = PrimativeTypeEnum.BOOLEAN;
		PrimativeTypeEnum bytea      = PrimativeTypeEnum.BYTE;
		PrimativeTypeEnum shorta     = PrimativeTypeEnum.SHORT;
		PrimativeTypeEnum charactera = PrimativeTypeEnum.CHAR;
		PrimativeTypeEnum inta       = PrimativeTypeEnum.INT;
		PrimativeTypeEnum longa      = PrimativeTypeEnum.LONG;
		PrimativeTypeEnum floata     = PrimativeTypeEnum.FLOAT;
		PrimativeTypeEnum doublea    = PrimativeTypeEnum.DOUBLE;
		//PrimativeTypeEnum lambdaa    = PrimativeTypeEnum.LAMBDA;
		
		HashMap<String, PrimativeTypeEnum[]> BOXED_TO_PRIMS = new HashMap<String, PrimativeTypeEnum[]>();
		
		PrimativeTypeEnum[] standard = new PrimativeTypeEnum[]{doublea, floata, inta, longa, bytea, shorta, charactera};
		PrimativeTypeEnum[] forByte =  new PrimativeTypeEnum[]{doublea, floata, inta, longa, bytea, shorta};
		PrimativeTypeEnum[] forNumber =  new PrimativeTypeEnum[]{doublea, floata, inta, longa, bytea, shorta};
		
		BOXED_TO_PRIMS.put("java.lang.Boolean",   new PrimativeTypeEnum[]{booleana});
		BOXED_TO_PRIMS.put("java.lang.Byte",      fiddle(forByte, bytea));
		BOXED_TO_PRIMS.put("java.lang.Short",     fiddle(standard, shorta));
		BOXED_TO_PRIMS.put("java.lang.Character", fiddle(standard, charactera));
		BOXED_TO_PRIMS.put("java.lang.Integer",   fiddle(standard, inta));
		BOXED_TO_PRIMS.put("java.lang.Long",      fiddle(standard, longa));
		BOXED_TO_PRIMS.put("java.lang.Float",     fiddle(standard, floata));
		BOXED_TO_PRIMS.put("java.lang.Double",    fiddle(standard, doublea));
		BOXED_TO_PRIMS.put("java.lang.Number",    forNumber);
		//BOXED_TO_PRIMS.put("com.concurnas.bootstrap.lang.Lambdan",   new PrimativeTypeEnum[]{lambdaa});
		
		return BOXED_TO_PRIMS;
	}
}
