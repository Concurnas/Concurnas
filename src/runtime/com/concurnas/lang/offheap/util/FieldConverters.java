package com.concurnas.lang.offheap.util;

import com.concurnas.bootstrap.lang.offheap.Decoder;
import com.concurnas.bootstrap.runtime.cps.CObject;

public class FieldConverters {
	public static byte toByte(byte input){ return input; }
	public static byte toByte(char input){ return (byte)input; }
	public static byte toByte(double input){ return (byte)input; }
	public static byte toByte(float input){ return (byte)input; }
	public static byte toByte(int input){ return (byte)input; }
	public static byte toByte(long input){ return (byte)input; }
	public static byte toByte(short input){ return (byte)input; }
	public static byte toByte(boolean input){ return input?(byte)1:(byte)0; }

	@SuppressWarnings("incomplete-switch")
	public static byte toByte(Decoder dec, TypeConvert typeConvert) {
		FieldCastType fromType = typeConvert.from.type;
		if(fromType.isPrimative){
			switch(fromType){
				//case PRIM_BYTE: return toByte(dec.getByte());
				case PRIM_CHAR: return toByte(dec.getChar());
				case PRIM_DOUBLE: return toByte(dec.getDouble());
				case PRIM_FLOAT: return toByte(dec.getFloat());
				case PRIM_INT: return toByte(dec.getInt());
				case PRIM_LONG: return toByte(dec.getLong());
				case PRIM_SHORT: return toByte(dec.getShort());
				case PRIM_BOOLEAN: return toByte(dec.getBoolean());
			}
		}else{
			Object got = dec.getObject();
			if(got != null){
				switch(fromType){
					case BOXED_BYTE: return toByte((Byte)got);
					case BOXED_CHAR: return toByte((Character)got);
					case BOXED_DOUBLE: return toByte((Double)got);
					case BOXED_FLOAT: return toByte((Float)got);
					case BOXED_INT: return toByte((Integer)got);
					case BOXED_LONG: return toByte((Long)got);
					case BOXED_SHORT: return toByte((Short)got);
					case BOXED_BOOLEAN: return toByte((Boolean)got);
				}
			}
		}
		
		return (byte)0;
	}
	
	
	public static char toChar(byte input){ return (char)input; }
	public static char toChar(char input){ return input; }
	public static char toChar(double input){ return (char)input; }
	public static char toChar(float input){ return (char)input; }
	public static char toChar(int input){ return (char)input; }
	public static char toChar(long input){ return (char)input; }
	public static char toChar(short input){ return (char)input; }
	public static char toChar(boolean input){ return input?(char)1:(char)0; }
	
	@SuppressWarnings("incomplete-switch")
	public static char toChar(Decoder dec, TypeConvert typeConvert) {
		FieldCastType fromType = typeConvert.from.type;
		
		if(fromType.isPrimative){
			switch(fromType){
				case PRIM_BYTE: return toChar(dec.getByte());
				//case PRIM_CHAR: return toChar(dec.getChar());
				case PRIM_DOUBLE: return toChar(dec.getDouble());
				case PRIM_FLOAT: return toChar(dec.getFloat());
				case PRIM_INT: return toChar(dec.getInt());
				case PRIM_LONG: return toChar(dec.getLong());
				case PRIM_SHORT: return toChar(dec.getShort());
				case PRIM_BOOLEAN: return toChar(dec.getBoolean());
			}
		}else{
			Object got = dec.getObject();
			if(got != null){
				switch(fromType){
					case BOXED_BYTE: return toChar((Byte)got);
					case BOXED_CHAR: return toChar((Character)got);
					case BOXED_DOUBLE: return toChar((Double)got);
					case BOXED_FLOAT: return toChar((Float)got);
					case BOXED_INT: return toChar((Integer)got);
					case BOXED_LONG: return toChar((Long)got);
					case BOXED_SHORT: return toChar((Short)got);
					case BOXED_BOOLEAN: return toChar((Boolean)got);
				}
			}
		}
		
		return (char)0;
	}

	public static double toDouble(byte input){ return (double)input; }
	public static double toDouble(char input){ return (double)input; }
	public static double toDouble(double input){ return input; }
	public static double toDouble(float input){ return (double)input; }
	public static double toDouble(int input){ return (double)input; }
	public static double toDouble(long input){ return (double)input; }
	public static double toDouble(short input){ return (double)input; }
	public static double toDouble(boolean input){ return input?1.:0.; }

	@SuppressWarnings("incomplete-switch")
	public static double toDouble(Decoder dec, TypeConvert typeConvert) {
		FieldCastType fromType = typeConvert.from.type;
		
		if(fromType.isPrimative){
			switch(fromType){
				case PRIM_BYTE: return toDouble(dec.getByte());
				case PRIM_CHAR: return toDouble(dec.getChar());
				//case PRIM_DOUBLE: return toDouble(dec.getDouble());
				case PRIM_FLOAT: return toDouble(dec.getFloat());
				case PRIM_INT: return toDouble(dec.getInt());
				case PRIM_LONG: return toDouble(dec.getLong());
				case PRIM_SHORT: return toDouble(dec.getShort());
				case PRIM_BOOLEAN: return toDouble(dec.getBoolean());
			}
		}else{
			Object got = dec.getObject();
			if(got != null){
				switch(fromType){
					case BOXED_BYTE: return toDouble((Byte)got);
					case BOXED_CHAR: return toDouble((Character)got);
					case BOXED_DOUBLE: return toDouble((Double)got);
					case BOXED_FLOAT: return toDouble((Float)got);
					case BOXED_INT: return toDouble((Integer)got);
					case BOXED_LONG: return toDouble((Long)got);
					case BOXED_SHORT: return toDouble((Short)got);
					case BOXED_BOOLEAN: return toDouble((Boolean)got);
				}
			}
		}
		
		return 0.;
	}
	
	public static float toFloat(byte input){ return (float)input; }
	public static float toFloat(char input){ return (float)input; }
	public static float toFloat(double input){ return (float)input; }
	public static float toFloat(float input){ return input; }
	public static float toFloat(int input){ return (float)input; }
	public static float toFloat(long input){ return (float)input; }
	public static float toFloat(short input){ return (float)input; }
	public static float toFloat(boolean input){ return input?1.f:0.f; }

	@SuppressWarnings("incomplete-switch")
	public static float toFloat(Decoder dec, TypeConvert typeConvert) {
		FieldCastType fromType = typeConvert.from.type;
		
		if(fromType.isPrimative){
			switch(fromType){
				case PRIM_BYTE: return toFloat(dec.getByte());
				case PRIM_CHAR: return toFloat(dec.getChar());
				case PRIM_DOUBLE: return toFloat(dec.getDouble());
				//case PRIM_FLOAT: return toFloat(dec.getFloat());
				case PRIM_INT: return toFloat(dec.getInt());
				case PRIM_LONG: return toFloat(dec.getLong());
				case PRIM_SHORT: return toFloat(dec.getShort());
				case PRIM_BOOLEAN: return toFloat(dec.getBoolean());
				
			}
		}else{
			Object got = dec.getObject();
			if(got != null){
				switch(fromType){
					case BOXED_BYTE: return toFloat((Byte)got);
					case BOXED_CHAR: return toFloat((Character)got);
					case BOXED_DOUBLE: return toFloat((Double)got);
					case BOXED_FLOAT: return toFloat((Float)got);
					case BOXED_INT: return toFloat((Integer)got);
					case BOXED_LONG: return toFloat((Long)got);
					case BOXED_SHORT: return toFloat((Short)got);
					case BOXED_BOOLEAN: return toFloat((Boolean)got);
				}
			}
		}
		
		return 0.f;
	}
	

	public static int toInteger(byte input){ return (int)input; }
	public static int toInteger(char input){ return (int)input; }
	public static int toInteger(double input){ return (int)input; }
	public static int toInteger(float input){ return (int)input; }
	public static int toInteger(int input){ return input; }
	public static int toInteger(long input){ return (int)input; }
	public static int toInteger(short input){ return (int)input; }
	public static int toInteger(boolean input){ return input?1:0; }

	@SuppressWarnings("incomplete-switch")
	public static int toInteger(Decoder dec, TypeConvert typeConvert) {
		FieldCastType fromType = typeConvert.from.type;
		
		if(fromType.isPrimative){
			switch(fromType){
				case PRIM_BYTE: return toInteger(dec.getByte());
				case PRIM_CHAR: return toInteger(dec.getChar());
				case PRIM_DOUBLE: return toInteger(dec.getDouble());
				case PRIM_FLOAT: return toInteger(dec.getFloat());
				//case PRIM_INT: return toInteger(dec.getInt());
				case PRIM_LONG: return toInteger(dec.getLong());
				case PRIM_SHORT: return toInteger(dec.getShort());
				case PRIM_BOOLEAN: return toInteger(dec.getBoolean());
				
			}
		}else{
			Object got = dec.getObject();
			if(got != null){
				switch(fromType){
					case BOXED_BYTE: return toInteger((Byte)got);
					case BOXED_CHAR: return toInteger((Character)got);
					case BOXED_DOUBLE: return toInteger((Double)got);
					case BOXED_FLOAT: return toInteger((Float)got);
					case BOXED_INT: return toInteger((Integer)got);
					case BOXED_LONG: return toInteger((Long)got);
					case BOXED_SHORT: return toInteger((Short)got);
					case BOXED_BOOLEAN: return toInteger((Boolean)got);
				}
			}
		}
		
		return 0;
	}
	
	public static long toLong(byte input){ return (long)input; }
	public static long toLong(char input){ return (long)input; }
	public static long toLong(double input){ return (long)input; }
	public static long toLong(float input){ return (long)input; }
	public static long toLong(long input){ return input; }
	public static long toLong(int input){ return (long)input; }
	public static long toLong(short input){ return (long)input; }
	public static long toLong(boolean input){ return input?1:0; }

	@SuppressWarnings("incomplete-switch")
	public static long toLong(Decoder dec, TypeConvert typeConvert) {
		FieldCastType fromType = typeConvert.from.type;

		if(fromType.isPrimative){
			switch(fromType){
				case PRIM_BYTE: return toLong(dec.getByte());
				case PRIM_CHAR: return toLong(dec.getChar());
				case PRIM_DOUBLE: return toLong(dec.getDouble());
				case PRIM_FLOAT: return toLong(dec.getFloat());
				case PRIM_INT: return toLong(dec.getInt());
				//case PRIM_LONG: return toLong(dec.getLong());
				case PRIM_SHORT: return toLong(dec.getShort());
				case PRIM_BOOLEAN: return toLong(dec.getBoolean());
			}
		}else{
			Object got = dec.getObject();
			if(got != null){
				switch(fromType){
					case BOXED_BYTE: return toLong((Byte)got);
					case BOXED_CHAR: return toLong((Character)got);
					case BOXED_DOUBLE: return toLong((Double)got);
					case BOXED_FLOAT: return toLong((Float)got);
					case BOXED_INT: return toLong((Integer)got);
					case BOXED_LONG: return toLong((Long)got);
					case BOXED_SHORT: return toLong((Short)got);
					case BOXED_BOOLEAN: return toLong((Boolean)got);
				}
			}
		}
		
		
		return 0l;
	}
	
	public static short toShort(byte input){ return (short)input; }
	public static short toShort(char input){ return (short)input; }
	public static short toShort(double input){ return (short)input; }
	public static short toShort(float input){ return (short)input; }
	public static short toShort(short input){ return input; }
	public static short toShort(long input){ return (short)input; }
	public static short toShort(int input){ return (short)input; }
	public static short toShort(boolean input){ return input?(short)1:(short)0; }

	@SuppressWarnings("incomplete-switch")
	public static short toShort(Decoder dec, TypeConvert typeConvert) {
		FieldCastType fromType = typeConvert.from.type;

		if(fromType.isPrimative){
			switch(fromType){
				case PRIM_BYTE: return toShort(dec.getByte());
				case PRIM_CHAR: return toShort(dec.getChar());
				case PRIM_DOUBLE: return toShort(dec.getDouble());
				case PRIM_FLOAT: return toShort(dec.getFloat());
				case PRIM_INT: return toShort(dec.getInt());
				case PRIM_LONG: return toShort(dec.getLong());
				//case PRIM_SHORT: return toShort(dec.getShort());
				case PRIM_BOOLEAN: return toShort(dec.getBoolean());
			}
		}else{
			Object got = dec.getObject();
			if(got != null){
				switch(fromType){
					case BOXED_BYTE: return toShort((Byte)got);
					case BOXED_CHAR: return toShort((Character)got);
					case BOXED_DOUBLE: return toShort((Double)got);
					case BOXED_FLOAT: return toShort((Float)got);
					case BOXED_INT: return toShort((Integer)got);
					case BOXED_LONG: return toShort((Long)got);
					case BOXED_SHORT: return toShort((Short)got);
					case BOXED_BOOLEAN: return toShort((Boolean)got);
				}
			}
		}
		
		return (short)0;//err
	}
	
	public static boolean toBoolean(byte input){ return input==(byte)1; }
	public static boolean toBoolean(char input){ return input==(char)1; }
	public static boolean toBoolean(double input){ return Double.compare(input, 1.)==0; }
	public static boolean toBoolean(float input){ return Float.compare(input, 1.f)==0; }
	public static boolean toBoolean(int input){ return input==1; }
	public static boolean toBoolean(long input){ return input==1l; }
	public static boolean toBoolean(short input){ return input==(short)1; }
	public static boolean toBoolean(boolean input){ return input; }
	public static boolean toBoolean(Object input){ return input==null?false:((CObject)input).toBoolean(); }

	@SuppressWarnings("incomplete-switch")
	public static boolean toBoolean(Decoder dec, TypeConvert typeConvert) {
		FieldCastType fromType = typeConvert.from.type;

		if(fromType.isPrimative){
			switch(typeConvert.from.type){
				case PRIM_BYTE: return toBoolean(dec.getByte());
				case PRIM_CHAR: return toBoolean(dec.getChar());
				case PRIM_DOUBLE: return toBoolean(dec.getDouble());
				case PRIM_FLOAT: return toBoolean(dec.getFloat());
				case PRIM_INT: return toBoolean(dec.getInt());
				case PRIM_LONG: return toBoolean(dec.getLong());
				case PRIM_SHORT: return toBoolean(dec.getShort());
				//case PRIM_BOOLEAN: return toBoolean(dec.getBoolean());
			}
		}else{
			Object got = dec.getObject();
			if(got != null){
				switch(typeConvert.from.type){
					case BOXED_BYTE: return toBoolean((Byte)got);
					case BOXED_CHAR: return toBoolean((Character)got);
					case BOXED_DOUBLE: return toBoolean((Double)got);
					case BOXED_FLOAT: return toBoolean((Float)got);
					case BOXED_INT: return toBoolean((Integer)got);
					case BOXED_LONG: return toBoolean((Long)got);
					case BOXED_SHORT: return toBoolean((Short)got);
					case BOXED_BOOLEAN: return toBoolean((Boolean)got);
					default: return ((CObject)got).toBoolean();
				}
			}
		}
		return false;
	}
	
	
	//primative|boxed => boxed
	@SuppressWarnings("incomplete-switch")
	public static Object toObject(Decoder dec, TypeConvert typeConvert) {
		FieldCastType toType = typeConvert.to.type;
		FieldCastType fromType = typeConvert.from.type;
		
		if(toType == FieldCastType.STRING){
			if(fromType.isPrimative){
				switch(fromType){
					case PRIM_BYTE: return ""+dec.getByte();
					case PRIM_CHAR: return ""+dec.getChar();
					case PRIM_DOUBLE: return ""+dec.getDouble();
					case PRIM_FLOAT: return ""+dec.getFloat();
					case PRIM_INT: return ""+dec.getInt();
					case PRIM_LONG: return ""+dec.getLong();
					case PRIM_SHORT: return ""+dec.getShort();
					case PRIM_BOOLEAN: return ""+dec.getBoolean();
				}
			}
			else{
				return "" + dec.getObject();
			}
		}
		
		if(fromType.isPrimative){
			switch(fromType){
				case PRIM_BYTE: return toBoxedfrom(dec.getByte(), toType);
				case PRIM_CHAR: return toBoxedfrom(dec.getChar(), toType);
				case PRIM_DOUBLE: return toBoxedfrom(dec.getDouble(), toType);
				case PRIM_FLOAT: return toBoxedfrom(dec.getFloat(), toType);
				case PRIM_INT: return toBoxedfrom(dec.getInt(), toType);
				case PRIM_LONG: return toBoxedfrom(dec.getLong(), toType);
				case PRIM_SHORT: return toBoxedfrom(dec.getShort(), toType);
				case PRIM_BOOLEAN: return toBoxedfrom(dec.getBoolean(), toType);
			}
		}
		else if(fromType.isBoxed){
			Object got = dec.getObject();
			
			switch(fromType){
				case BOXED_BYTE: return toBoxedfrom(got==null?(byte)0:(Byte)got, toType);
				case BOXED_CHAR: return toBoxedfrom(got==null?(char)0:(Character)got, toType);
				case BOXED_DOUBLE: return toBoxedfrom(got==null?0.:(Double)got, toType);
				case BOXED_FLOAT: return toBoxedfrom(got==null?0.f:(Float)got, toType);
				case BOXED_INT: return toBoxedfrom(got==null?0:(Integer)got, toType);
				case BOXED_LONG: return toBoxedfrom(got==null?0l:(Long)got, toType);
				case BOXED_SHORT: return toBoxedfrom(got==null?(short)0:(Short)got, toType);
				case BOXED_BOOLEAN: return toBoxedfrom(got==null?false:(Boolean)got, toType);
			}
		}
		
		if(toType == FieldCastType.PRIM_BOOLEAN || toType == FieldCastType.BOXED_BOOLEAN){
			//everything can be converted to boolean
			return toBoolean(dec.getObject());
		}
		
		return null;//err
	}

	@SuppressWarnings("incomplete-switch")
	private static Object toBoxedfrom(byte input, FieldCastType totype) {
		switch(totype){
			case BOXED_BYTE: return new Byte((byte)input);
			case BOXED_CHAR: return new Character((char)input);
			case BOXED_DOUBLE: return new Double((double)input);
			case BOXED_FLOAT: return new Float((float)input);
			case BOXED_INT: return new Integer((int)input);
			case BOXED_LONG: return new Long((long)input);
			case BOXED_SHORT: return new Short((short)input);
			case BOXED_BOOLEAN: return new Boolean(input==(byte)1);
		}
		return null;//err
	}

	@SuppressWarnings("incomplete-switch")
	private static Object toBoxedfrom(char input, FieldCastType totype) {
		switch(totype){
			case BOXED_BYTE: return new Byte((byte)input);
			case BOXED_CHAR: return new Character((char)input);
			case BOXED_DOUBLE: return new Double((double)input);
			case BOXED_FLOAT: return new Float((float)input);
			case BOXED_INT: return new Integer((int)input);
			case BOXED_LONG: return new Long((long)input);
			case BOXED_SHORT: return new Short((short)input);
			case BOXED_BOOLEAN: return new Boolean(input==(char)1);
		}
		return null;//err
	}

	@SuppressWarnings("incomplete-switch")
	private static Object toBoxedfrom(double input, FieldCastType totype) {
		switch(totype){
			case BOXED_BYTE: return new Byte((byte)input);
			case BOXED_CHAR: return new Character((char)input);
			case BOXED_DOUBLE: return new Double((double)input);
			case BOXED_FLOAT: return new Float((float)input);
			case BOXED_INT: return new Integer((int)input);
			case BOXED_LONG: return new Long((long)input);
			case BOXED_SHORT: return new Short((short)input);
			case BOXED_BOOLEAN: return new Boolean(Double.compare(1, input) == 0);
		}
		return null;//err
	}

	@SuppressWarnings("incomplete-switch")
	private static Object toBoxedfrom(float input, FieldCastType totype) {
		switch(totype){
			case BOXED_BYTE: return new Byte((byte)input);
			case BOXED_CHAR: return new Character((char)input);
			case BOXED_DOUBLE: return new Double((double)input);
			case BOXED_FLOAT: return new Float((float)input);
			case BOXED_INT: return new Integer((int)input);
			case BOXED_LONG: return new Long((long)input);
			case BOXED_SHORT: return new Short((short)input);
			case BOXED_BOOLEAN: return new Boolean(Float.compare(1, input) == 0);
		}
		return null;//err
	}

	@SuppressWarnings("incomplete-switch")
	private static Object toBoxedfrom(int input, FieldCastType totype) {
		switch(totype){
			case BOXED_BYTE: return new Byte((byte)input);
			case BOXED_CHAR: return new Character((char)input);
			case BOXED_DOUBLE: return new Double((double)input);
			case BOXED_FLOAT: return new Float((float)input);
			case BOXED_INT: return new Integer((int)input);
			case BOXED_LONG: return new Long((long)input);
			case BOXED_SHORT: return new Short((short)input);
			case BOXED_BOOLEAN: return new Boolean(input==1);
		}
		return null;//err
	}

	@SuppressWarnings("incomplete-switch")
	private static Object toBoxedfrom(long input, FieldCastType totype) {
		switch(totype){
			case BOXED_BYTE: return new Byte((byte)input);
			case BOXED_CHAR: return new Character((char)input);
			case BOXED_DOUBLE: return new Double((double)input);
			case BOXED_FLOAT: return new Float((float)input);
			case BOXED_INT: return new Integer((int)input);
			case BOXED_LONG: return new Long((long)input);
			case BOXED_SHORT: return new Short((short)input);
			case BOXED_BOOLEAN: return new Boolean(input==1l);
		}
		return null;//err
	}

	@SuppressWarnings("incomplete-switch")
	private static Object toBoxedfrom(short input, FieldCastType totype) {
		switch(totype){
			case BOXED_BYTE: return new Byte((byte)input);
			case BOXED_CHAR: return new Character((char)input);
			case BOXED_DOUBLE: return new Double((double)input);
			case BOXED_FLOAT: return new Float((float)input);
			case BOXED_INT: return new Integer((int)input);
			case BOXED_LONG: return new Long((long)input);
			case BOXED_SHORT: return new Short((short)input);
			case BOXED_BOOLEAN: return new Boolean(input==(short)1);
		}
		return null;//err
	}

	@SuppressWarnings("incomplete-switch")
	private static Object toBoxedfrom(boolean input, FieldCastType totype) {
		switch(totype){
			case BOXED_BYTE: return new Byte(input?(byte)1:(byte)0);
			case BOXED_CHAR: return new Character(input?(char)1:(char)0);
			case BOXED_DOUBLE: return new Double(input?(double)1.:(double)0.);
			case BOXED_FLOAT: return new Float(input?(float)1.f:(float)0.f);
			case BOXED_INT: return new Integer(input?1:0);
			case BOXED_LONG: return new Long(input?1l:0l);
			case BOXED_SHORT: return new Short(input?(short)1:(short)0);
			case BOXED_BOOLEAN: return new Boolean(input);
		}
		return null;//err
	}
	
	
	
	//
	
	//structs
	public static enum FieldCastType{
		OBJECT(false, false),
		PRIM_BYTE(true, false),
		PRIM_CHAR(true, false),
		PRIM_DOUBLE(true, false),
		PRIM_FLOAT(true, false),
		PRIM_INT(true, false),
		PRIM_LONG(true, false),
		PRIM_SHORT(true, false),
		PRIM_BOOLEAN(true, true),
		BOXED_BYTE(false, true),
		BOXED_CHAR(false, true),
		BOXED_DOUBLE(false, true),
		BOXED_FLOAT(false, true),
		BOXED_INT(false, true),
		BOXED_LONG(false, true),
		BOXED_SHORT(false, true),
		BOXED_BOOLEAN(false, true),
		STRING(false, false);
		
		public boolean isPrimative;
		public boolean isBoxed;
		public boolean isBoxedOrPrimative;
		
		private FieldCastType(boolean isPrimative, boolean isBoxed){
			this.isPrimative = isPrimative;
			this.isBoxed = isBoxed;
			this.isBoxedOrPrimative = isPrimative || isBoxed;
		}
		
		
	}
	
	public static class FieldCastTypeInfo{
		public final int arLevels;
		public final FieldCastType type;
		public Class<?> clazz;

		public FieldCastTypeInfo(final FieldCastType skipType, final int arLevels, final Class<?> clazz){
			this.type = skipType;
			this.arLevels = arLevels;
			this.clazz = clazz;
		}
	}
	
	public static class TypeConvert{
		public final FieldCastTypeInfo from;
		public final FieldCastTypeInfo to;
		
		public TypeConvert(final FieldCastTypeInfo from, final FieldCastTypeInfo to){
			this.from = from;
			this.to = to;
		}
	}
	
}
