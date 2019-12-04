package com.concurnas.lang.offheap.storage;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;

import com.concurnas.lang.offheap.storage.OffHeapTransformationalDecoder.SkipFieldInfo;
import com.concurnas.lang.offheap.storage.OffHeapTransformationalDecoder.SkipFieldType;
import com.concurnas.lang.offheap.util.FieldConverters.FieldCastType;
import com.concurnas.lang.offheap.util.FieldConverters.FieldCastTypeInfo;
import com.concurnas.lang.offheap.util.FieldConverters.TypeConvert;
import com.concurnas.runtime.Pair;

public class Utils {

	public static class SingleBufferProvider implements MallocProvider{
		public ByteBuffer lastbb;
		@Override
		public Pair<ByteBuffer, Long> malloc(int size) {
			lastbb = ByteBuffer.allocate(size);//non direct/i.e. not offheap
			return new Pair<ByteBuffer, Long>(lastbb, 0l);
		}
		
		public ByteBuffer mallocRaw(int size){
			lastbb = ByteBuffer.allocate(size);
			return lastbb;
		}
		
		public byte[] getLastData(){
			return lastbb.array();
		}

		@Override
		public ByteBuffer getBuffer(long address) {
			lastbb.position(0);
			return this.lastbb;
		}
		
		public void setPositionZero(){
			lastbb.position(0);
		}

		@Override
		public void free(long address) {
			
		}
	}
	
	public static boolean twoStringArEquals(String[] a, String[] b){
		//99/100 times the same so quick method to check desc
		if(a == null || b == null || a.length % 2 != 0 || b.length % 2 != 0){//no metaBinary method so resolves to null, so you're on your own, assume binary compatability...
			return true;
		}
		if(a.length != b.length){
			return false;
		}else{
			int aLen = a.length;
			for(int n=0; n < aLen; n++){
				if(!a[n].equals(b[n])){
					return false;
				}
			}
		}
		
		return true;
	}
	
	public static String[] callMetaBinary(Class<?> cls){
		String[] meta;
		try{
			meta = (String[])cls.getMethod("metaBinary").invoke(null);
		}catch(InvocationTargetException | IllegalAccessException | NoSuchMethodException e){
			meta=null;
		}
		return meta;
	}
	
	private static int stringInArr(String what, String[] thing){//find matching name and return desc position
		int thinglen = thing.length;
		for(int n=0; n < thinglen; n+=2){
			if(what.equals(thing[n])){
				return n+1;
			}
		}
		return -1;
	}
	
	private static SkipFieldInfo createSkipFieldInfo(String desc){
		int arrLevels=0;
		while(desc.charAt(arrLevels) == '['){
			arrLevels++;
		}
		
		SkipFieldType ttype = SkipFieldType.OBJECT;
		
		switch(desc.charAt(arrLevels)){
			case 'B': ttype = SkipFieldType.BYTE; break;
			case 'C': ttype = SkipFieldType.CHAR; break;
			case 'D': ttype = SkipFieldType.DOUBLE; break;
			case 'F': ttype = SkipFieldType.FLOAT; break;
			case 'I': ttype = SkipFieldType.INT; break;
			case 'J': ttype = SkipFieldType.LONG; break;
			case 'S': ttype = SkipFieldType.SHORT; break;
			case 'Z': ttype = SkipFieldType.BOOLEAN; break;
		}
		return new SkipFieldInfo(ttype, arrLevels);
	}
	
	private static FieldCastTypeInfo createFieldCastType(String desc){
		int arrLevels=0;
		while(desc.charAt(arrLevels) == '['){
			arrLevels++;
		}
		

		FieldCastType fct = FieldCastType.OBJECT;
		
		Class<?> resolvesto = null;
		
		switch(desc.charAt(arrLevels)){
			case 'B': fct = FieldCastType.PRIM_BYTE; resolvesto=int.class; break;
			case 'C': fct = FieldCastType.PRIM_CHAR; resolvesto=char.class; break;
			case 'D': fct = FieldCastType.PRIM_DOUBLE; resolvesto=double.class; break;
			case 'F': fct = FieldCastType.PRIM_FLOAT; resolvesto=float.class; break;
			case 'I': fct = FieldCastType.PRIM_INT; resolvesto=int.class; break;
			case 'J': fct = FieldCastType.PRIM_LONG; resolvesto=long.class; break;
			case 'S': fct = FieldCastType.PRIM_SHORT; resolvesto=short.class; break;
			case 'Z': fct = FieldCastType.PRIM_BOOLEAN; resolvesto=boolean.class; break;
		}
		
		if(null == resolvesto){
			desc = desc.substring(arrLevels);
			switch(desc){
				case "Ljava/lang/Byte;" : fct = FieldCastType.BOXED_BYTE; resolvesto=Byte.class; break;
				case "Ljava/lang/Character;" : fct = FieldCastType.BOXED_CHAR; resolvesto=Character.class; break;
				case "Ljava/lang/Double;" : fct = FieldCastType.BOXED_DOUBLE; resolvesto=Double.class; break;
				case "Ljava/lang/Float;" : fct = FieldCastType.BOXED_FLOAT; resolvesto=Float.class; break;
				case "Ljava/lang/Integer;" : fct = FieldCastType.BOXED_INT; resolvesto=Integer.class; break;
				case "Ljava/lang/Long;" : fct = FieldCastType.BOXED_LONG; resolvesto=Long.class; break;
				case "Ljava/lang/Short;" : fct = FieldCastType.BOXED_SHORT; resolvesto=Short.class; break;
				case "Ljava/lang/Boolean;" : fct = FieldCastType.BOXED_BOOLEAN; resolvesto=Boolean.class; break;
				case "Ljava/lang/String;" : fct = FieldCastType.STRING; resolvesto=String.class; break;
			
			}
		}
		
		return new FieldCastTypeInfo(fct, arrLevels, resolvesto);
	}
	
	public static void something(Integer ina, Boolean bb){
		
	}
	
	/*public static <T> EncodingTranslator<T> createEncodingTranslator(OffHeapEncoder<T> encoder, String[] from, String[] to, Class<?> fromclass, Class<?> finalClass){
		
		SingleBufferProvider sbp = new SingleBufferProvider();
		
		OffHeapDecoder<T> normalDecoder = new OffHeapDecoder<T>(sbp, encoder.vidprovider);
		
		OffHeapTransformationalDecoderGennerator<T> transDecoderGennerator = createTransformationalDecoder( normalDecoder,  from,  to);
		
		ClassAndMeta key = new ClassAndMeta(finalClass, to);
		
		normalDecoder.cacheClassAndMetaToDecoder.put(key, transDecoderGennerator);
		normalDecoder.clearCAMCasheOfItemOnGet = new HashSet<ClassAndMeta>();
		normalDecoder.clearCAMCasheOfItemOnGet.add(key);
		
		return  new EncodingTranslator<T>(encoder, sbp, normalDecoder,  finalClass);
	}*/
	
	public static <T> OffHeapTransformationalDecoderGennerator<T> createTransformationalDecoder(OffHeapDecoder<T> decoder, String[] from, String[] to){
		int fromlen = from.length;
		HashMap<Integer, SkipFieldInfo> missingFields = new HashMap<Integer, SkipFieldInfo>();
		HashSet<Integer> addedFields = new HashSet<Integer>();
		HashMap<Integer, TypeConvert> castFields = new HashMap<Integer, TypeConvert>();
		
		for(int srcFieldNo=0; srcFieldNo < fromlen; srcFieldNo+=2){
			SkipFieldInfo sfi = createSkipFieldInfo(from[srcFieldNo+1]);
			
			int toDescPos = stringInArr(from[srcFieldNo], to);
			if(toDescPos == -1){
				missingFields.put(srcFieldNo/2, sfi);
				//System.err.println("missing: " + from[srcFieldNo] + " desc " + from[srcFieldNo] + " " + srcFieldNo/2 + " " + sfi.skipType);
			}else{
				//it's present so we need to check the types
				String fromDesc = from[srcFieldNo+1];
				String toDesc = to[toDescPos];
				if(!fromDesc.equals(toDesc)){
					FieldCastTypeInfo fromType = createFieldCastType(fromDesc);
					FieldCastTypeInfo toType = createFieldCastType(toDesc);
					/*
					System.err.println("from field: " + fromType.type);
					System.err.println("to field: " + toType.type);*/
					
					if(fromType.arLevels != toType.arLevels){//array levels must match to convert
						addedFields.add(toDescPos-1);
					}
					else if(fromType.type==FieldCastType.OBJECT && toType.type==FieldCastType.OBJECT){
						boolean convertToSuperClassOrIface = false;
						try {
							ClassLoader clsldr = decoder.vidconverter.getClassloader();
							Class<?> fromClass = Class.forName(fromDesc.substring(1, fromDesc.length()-1), true, clsldr);
							Class<?> toClass   = Class.forName(toDesc.substring(1, toDesc.length()-1),     true, clsldr);
							
							if(toClass.isAssignableFrom(fromClass)){
								convertToSuperClassOrIface = true;
								fromType.clazz = fromClass;
								toType.clazz = toClass;
							}
							
							
						} catch (ClassNotFoundException e) {
							//throw new RuntimeException("Class definition is missing in classloader: " + e.getMessage(), e);
						}
						
						if(!convertToSuperClassOrIface){
							addedFields.add(toDescPos-1);
						}
					}
					else if(fromType.type.isBoxedOrPrimative == toType.type.isBoxedOrPrimative){
						castFields.put(srcFieldNo/2, new TypeConvert(fromType, toType));
					}else if(toType.type == FieldCastType.STRING){
						castFields.put(srcFieldNo/2, new TypeConvert(fromType, toType));
					}else if(toType.type == FieldCastType.PRIM_BOOLEAN || toType.type == FieldCastType.BOXED_BOOLEAN){
						//anything can be converted to boolean
						castFields.put(srcFieldNo/2, new TypeConvert(fromType, toType));
					}else{
						//not castable, add to added fields to stick with default value for item:
						addedFields.add(toDescPos-1);
					}
				}
			}
		}
		
		int tolen = to.length;
		
		for(int getCallNo=0; getCallNo < tolen; getCallNo+=2){
			if(-1 == stringInArr(to[getCallNo], from)){
				addedFields.add(getCallNo/2);
			}
		}
		
		/*System.err.println("missing fields: " + missingFields);
		System.err.println("add fields: " + addedFields);
		System.err.println("cast fields: " + castFields);*/
		
		//return new OffHeapTransformationalDecoder<T>(decoder, tolen, missingFields, addedFields, castFields);
		return new OffHeapTransformationalDecoderGennerator<T>(tolen, missingFields, addedFields, castFields);
	}
	
}
