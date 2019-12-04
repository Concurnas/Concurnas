package com.concurnas.lang.offheap.serialization;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;

import com.concurnas.bootstrap.lang.offheap.Decoder;
import com.concurnas.bootstrap.runtime.InitUncreatable;
import com.concurnas.bootstrap.runtime.cps.CObject;
import com.concurnas.lang.Uninterruptible;
import com.concurnas.lang.util.LRUCache;

/**
 * SerializationEncoder can be used along with SerializationDecoder to quickly serialise an object graph to a byte array
 * 
 * intermediate serialization format is:
 * 
 * contents: Object | primative | boxedType | array
 * 
 * Object: StringName, id(long) 0 ->, contents
 * 		  		 	           >0 -> no contents use pre existing object in stream, indicated by id
 * 		   Null-> 0
 * 
 * StringName -> nameLength (long), name (bytes[])
 * 
 * primative -> byte[] as approperiate
 * 
 * boxedType : primative
 * 
 * array: size(int), contents 
 */
@Uninterruptible
public class SerializationDecoder implements Decoder {

	private final ByteBuffer buf;
	private ClassLoader classLoader;

	private SerializationDecoder(ByteBuffer buf, ClassLoader classLoader) {
		this.buf = buf;
		this.classLoader = classLoader;
	}
	
	public static Object decode(ByteBuffer buf, ClassLoader classLoader) throws ClassNotFoundException {
		SerializationDecoder dec = new SerializationDecoder(buf, classLoader);
		return dec.getObject();
	}
	
	public static Object decode(ByteBuffer buf) throws ClassNotFoundException {
		return decode(buf, SerializationDecoder.class.getClassLoader());
	}
	
	public static Object decode(byte[] bytes) throws ClassNotFoundException {
		return decode(ByteBuffer.wrap(bytes), SerializationDecoder.class.getClassLoader());
	}
	
	public static Object decode(byte[] bytes, ClassLoader classLoader) throws ClassNotFoundException {
		return decode(ByteBuffer.wrap(bytes), classLoader);
	}
	
	private HashMap<Long, Object> idToInstance = new HashMap<Long, Object>();
	private long objectPopulatedSoFar = 0;

	@Override
	public Object getObject() {
		try {
			long nameLength = buf.getLong();
			if(nameLength == 0) {
				return null;
			}
			
			byte[] nameb = new byte[(int)nameLength];
			buf.get(nameb);
			
			long objectId = buf.getLong();

			//System.err.println("obj id:" + objectId);
			if(objectId < objectPopulatedSoFar){
				objectPopulatedSoFar++;
				//System.err.println("from cache:" + objectId);
				return idToInstance.get(objectId);
			}
			
			String clsName = new String(nameb);
			//System.err.println("obj class Name:" + clsName);
			Object instance;
			if(clsName.equals("java.lang.String")) {
				long strSize = buf.getLong();
				byte[] stData = new byte[(int)strSize];
				buf.get(stData);
				instance = new String(stData);
				//System.err.println("got str---- " + instance);
				idToInstance.put(objectPopulatedSoFar++, instance);
			}else if(clsName.equals("java.lang.Class")) {
				long strSize = buf.getLong();
				byte[] stData = new byte[(int)strSize];
				buf.get(stData);
				instance = getClassForName(new String(stData));
				
				idToInstance.put(objectPopulatedSoFar++, instance);
			}else if(clsName.startsWith("[")) {
				int levels = clsName.lastIndexOf("[")+1;
				//System.err.println(levels);
				String compTypeStr = clsName.substring(levels);
				switch(compTypeStr) {
					case "int": instance = getIntArray(levels, false); break;
					case "long": instance = getLongArray(levels, false); break;
					case "short": instance = getShortArray(levels, false); break;
					case "double": instance = getDoubleArray(levels, false); break;
					case "float": instance = getFloatArray(levels, false); break;
					case "boolean": instance = getBooleanArray(levels, false); break;
					case "char": instance = getCharArray(levels, false); break;
					case "byte": instance = getByteArray(levels, false); break;
					default: {
						Class<?> objClass = getClassForName(compTypeStr);
						long id = objectPopulatedSoFar++;
						instance = getObjectArrayKnownArray(levels, objClass);
						idToInstance.put(id, instance);
						break;
					}
				}
			}else {
				Class<?> objCls = getClassForName(clsName);
				if(objCls.isEnum()) {
					long strSize = buf.getLong();
					byte[] stData = new byte[(int)strSize];
					buf.get(stData);
					long id = objectPopulatedSoFar++;
					instance = Enum.valueOf((Class)objCls, new String(stData));
					idToInstance.put(id, instance);
				}else {
					//System.err.println("objCls: " + objCls);
					Constructor<?> con = getConstructorFromCache(objCls);
					instance = con.newInstance(new Object[]{null, null});//fields to default?
					idToInstance.put(objectPopulatedSoFar++, instance);
					((CObject)instance).fromBinary(this);
				}
			}
			return instance;
		}catch(Throwable thr) {
			throw new RuntimeException(thr);
		}
		
	}
	
	private Class<?> getClassForName(String clsName) throws ClassNotFoundException{
		return Class.forName(clsName, true, classLoader);
	}
	
	//cache most commonly used method bindings
	private static final int META_CACHE_SUZE = 100;
	private final LRUCache<Class<?>, Method> cachefromBinary = new LRUCache<Class<?>, Method>(META_CACHE_SUZE);//only really used for enums
	private final LRUCache<Class<?>, Constructor<?>> cacheConstrctor = new LRUCache<Class<?>, Constructor<?>>(META_CACHE_SUZE);
	
	private Method getMethodFromCache(Class<?> objClass) throws NoSuchMethodException, SecurityException{
		//JPT: is this really needed?
		Method m = cachefromBinary.get(objClass);
		if(null == m){
			m=objClass.getMethod("fromBinary", Decoder.class);
			cachefromBinary.put(objClass, m);
		}
		
		return m;
	}
	
	private Constructor<?> getConstructorFromCache(Class<?> objClass) throws NoSuchMethodException, SecurityException{
		Constructor<?> m = cacheConstrctor.get(objClass);
		if(null == m){
			m=objClass.getConstructor(InitUncreatable.class, boolean[].class);
			m.setAccessible(true);
			cacheConstrctor.put(objClass, m);
		}
		
		return m;
	}
	
	@Override
	public int getInt() {
		return buf.getInt();
	}

	@Override
	public long getLong() {
		return buf.getLong();
	}

	@Override
	public double getDouble() {
		return buf.getDouble();
	}

	@Override
	public float getFloat() {
		return buf.getFloat();
	}

	@Override
	public boolean getBoolean() {
		return buf.getInt()==1;
	}

	@Override
	public short getShort() {
		return buf.getShort();
	}

	@Override
	public byte getByte() {
		return buf.get();
	}

	@Override
	public char getChar() {
		return buf.getChar();
	}

	@Override
	public Object get(long address) {
		//?
		return null;
	}

	//////////////////////////////////////////////////
	//copy paste yuck!
	private Object getIntArray( int levels, boolean doNullCheck){
		if(doNullCheck){
			long objType = buf.getLong();//test[cursor++];
			if(objType == 0l){
				return null;
			}
			
			long objectId = getLong();
			if(objectId < objectPopulatedSoFar){
				objectPopulatedSoFar++;
				return idToInstance.get(objectId);
			}
		}
		
		int len=getInt();
		long objCnt = objectPopulatedSoFar++;
		if(levels == 1){
			int[] ret = new int[len];
			for(int n=0; n < len; n++){
				ret[n] = getInt();
			}

			idToInstance.put(objCnt, ret);
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(int.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, getIntArray(levels-1));
			}
			idToInstance.put(objCnt, ret);
			return ret;
		}
	}
	
	private Object getFloatArray( int levels, boolean doNullCheck){
		if(doNullCheck){
			long objType = buf.getLong();//test[cursor++];
			if(objType == 0l){
				return null;
			}
		
			long objectId = getLong();
			if(objectId < objectPopulatedSoFar){
				objectPopulatedSoFar++;
				return idToInstance.get(objectId);
			}
		}
		
		int len=getInt();
		long objCnt = objectPopulatedSoFar++;
		if(levels == 1){
			float[] ret = new float[len];
			for(int n=0; n < len; n++){
				ret[n] = getFloat();
			}

			idToInstance.put(objCnt, ret);
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(float.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, getFloatArray(levels-1));
			}
			idToInstance.put(objCnt, ret);
			return ret;
		}
	}
	
	private Object getDoubleArray( int levels, boolean doNullCheck){
		if(doNullCheck){
			long objType = buf.getLong();//test[cursor++];
			if(objType == 0l){
				return null;
			}
			long objectId = getLong();
			if(objectId < objectPopulatedSoFar){
				objectPopulatedSoFar++;
				return idToInstance.get(objectId);
			}
		}
		
		int len=getInt();
		long objCnt = objectPopulatedSoFar++;
		if(levels == 1){
			double[] ret = new double[len];
			for(int n=0; n < len; n++){
				ret[n] = getDouble();
			}

			idToInstance.put(objCnt, ret);
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(double.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, getDoubleArray(levels-1));
			}
			idToInstance.put(objCnt, ret);
			return ret;
		}
	}
	
	private Object getLongArray( int levels, boolean doNullCheck){
		if(doNullCheck){
			long objType = buf.getLong();//test[cursor++];
			if(objType == 0l){
				return null;
			}
			long objectId = getLong();
			if(objectId < objectPopulatedSoFar){
				objectPopulatedSoFar++;
				return idToInstance.get(objectId);
			}
		}
		
		
		
		int len=getInt();
		long objCnt = objectPopulatedSoFar++;
		if(levels == 1){
			long[] ret = new long[len];
			for(int n=0; n < len; n++){
				ret[n] = getLong();
			}

			idToInstance.put(objCnt, ret);
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(long.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, getLongArray(levels-1));
			}
			idToInstance.put(objCnt, ret);
			return ret;
		}
	}
	
	private Object getShortArray( int levels, boolean doNullCheck){
		if(doNullCheck){
			long objType = buf.getLong();//test[cursor++];
			if(objType == 0l){
				return null;
			}
			long objectId = getLong();
			if(objectId < objectPopulatedSoFar){
				objectPopulatedSoFar++;
				return idToInstance.get(objectId);
			}
		}
		
		
		
		int len=getInt();
		long objCnt = objectPopulatedSoFar++;
		if(levels == 1){
			short[] ret = new short[len];
			for(int n=0; n < len; n++){
				ret[n] = getShort();
			}

			idToInstance.put(objCnt, ret);
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(short.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, getShortArray(levels-1));
			}
			idToInstance.put(objCnt, ret);
			return ret;
		}
	}
	
	private Object getCharArray( int levels, boolean doNullCheck){
		if(doNullCheck){
			long objType = buf.getLong();//test[cursor++];
			if(objType == 0l){
				return null;
			}
			long objectId = getLong();
			if(objectId < objectPopulatedSoFar){
				objectPopulatedSoFar++;
				return idToInstance.get(objectId);
			}
		}
		
		
		
		int len=getInt();
		long objCnt = objectPopulatedSoFar++;
		if(levels == 1){
			char[] ret = new char[len];
			for(int n=0; n < len; n++){
				ret[n] = getChar();
			}

			idToInstance.put(objCnt, ret);
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(char.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, getCharArray(levels-1));
			}
			idToInstance.put(objCnt, ret);
			return ret;
		}
	}
	
	private Object getByteArray( int levels, boolean doNullCheck){
		if(doNullCheck){
			long objType = buf.getLong();//test[cursor++];
			if(objType == 0l){
				return null;
			}
			long objectId = getLong();
			if(objectId < objectPopulatedSoFar){
				objectPopulatedSoFar++;
				return idToInstance.get(objectId);
			}
		}
		
		
		
		int len=getInt();
		long objCnt = objectPopulatedSoFar++;
		if(levels == 1){
			byte[] ret = new byte[len];
			for(int n=0; n < len; n++){
				ret[n] = getByte();
			}

			idToInstance.put(objCnt, ret);
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(byte.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, getByteArray(levels-1));
			}
			idToInstance.put(objCnt, ret);
			return ret;
		}
	}
	
	private Object getBooleanArray( int levels, boolean doNullCheck){
		if(doNullCheck){
			long objType = buf.getLong();//test[cursor++];
			if(objType == 0l){
				return null;
			}
			long objectId = getLong();
			if(objectId < objectPopulatedSoFar){
				objectPopulatedSoFar++;
				return idToInstance.get(objectId);
			}
		}
		
		
		
		int len=getInt();
		long objCnt = objectPopulatedSoFar++;
		if(levels == 1){
			boolean[] ret = new boolean[len];
			for(int n=0; n < len; n++){
				ret[n] = getBoolean();
			}

			idToInstance.put(objCnt, ret);
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(boolean.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, getBooleanArray(levels-1));
			}
			idToInstance.put(objCnt, ret);
			return ret;
		}
	}
	
	
	
	//////////////////////////////////////////////////
	
	
	
	protected Object getObjectArrayKnownArray( int levels, Class<?> objClass ){
		if(objClass == int.class){
			return getIntArray(levels, false);
		}else if(objClass == long.class){
			return getLongArray(levels, false);
		}else if(objClass == float.class){
			return getFloatArray(levels, false);
		}else if(objClass == double.class){
			return getDoubleArray(levels, false);
		}else if(objClass == short.class){
			return getShortArray(levels, false);
		}else if(objClass == char.class){
			return getCharArray(levels, false);
		}else if(objClass == byte.class){
			return getByteArray(levels, false);
		}else if(objClass == boolean.class){
			return getBooleanArray(levels, false);
		}
		else{
			int len=getInt();
			//System.err.println("len: " + len);
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(objClass, dimensions);
			for(int n=0; n < len; n++){
				Object what = getObject();
				//System.err.println("obtained Object: " + what);
				Array.set(ret, n, what);
			}
			//System.err.println("len: " + len + " done");
			return ret;
		}
	}
	
	
	
	
	
	//////////////////////////////////////////////////
	
	
	@Override
	public Object getIntArray(int levels) {
		return getIntArray(levels, true);
	}

	@Override
	public Object getFloatArray(int levels) {
		return getFloatArray(levels, true);
	}

	@Override
	public Object getDoubleArray(int levels) {
		return getDoubleArray(levels, true);
	}

	@Override
	public Object getLongArray(int levels) {
		return getLongArray(levels, true);
	}

	@Override
	public Object getShortArray(int levels) {
		return getShortArray(levels, true);
	}

	@Override
	public Object getCharArray(int levels) {
		return getCharArray(levels, true);
	}

	@Override
	public Object getByteArray(int levels) {
		return getByteArray(levels, true);
	}

	@Override
	public Object getBooleanArray(int levels) {
		return getBooleanArray(levels, true);
	}

	@Override
	public Object getObjectArray(int levels) {
		return getObject();
	}

	@Override
	public boolean canThrowMissingFieldException() {
		
		return false;
	}

	@Override
	public boolean[] getFieldsToDefault() {
		
		return null;
	}

}
