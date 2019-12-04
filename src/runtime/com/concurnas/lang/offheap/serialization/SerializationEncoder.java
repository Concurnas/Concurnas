package com.concurnas.lang.offheap.serialization;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;

import com.concurnas.bootstrap.lang.offheap.Encoder;
import com.concurnas.bootstrap.runtime.cps.CObject;
import com.concurnas.lang.Uninterruptible;


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
public class SerializationEncoder implements Encoder {
	private SerializationEncoder() {}
	
	public static byte[] encode(Object an) {
		SerializationEncoder ime = new SerializationEncoder();
		ime.put(an);
		return ime.getBytes();
	}
	
	private ArrayList<Object> toOutputx = new ArrayList<Object>();
	
	private IdentityHashMap<Object, Long> graphToObjectInstance = new IdentityHashMap<Object, Long>();
	private long objectCount = 0;
	
	//call this method after performing encoding
	private byte[] getBytes() {
		
		//byte, byte[] or long
		
		ByteBuffer buf = ByteBuffer.allocate((int)(bytesize));
		
		for(Object inst : toOutputx) {
			if(inst instanceof byte[]) {
				buf.put((byte[])inst);
			}else if(inst instanceof Long) {
				buf.putLong((long)inst);
			}else if(inst instanceof Integer) {
				buf.putInt((int)inst);
			}else if(inst instanceof Short) {
				buf.putShort((short)inst);
			}else if(inst instanceof Float) {
				buf.putFloat((float)inst);
			}else if(inst instanceof Double) {
				buf.putDouble((double)inst);
			}else if(inst instanceof Character) {
				buf.putChar((char)inst);
			}else if(inst instanceof Byte) {
				buf.put((byte)inst);
			}
		}
		
		return buf.array();
	}
	
	private long bytesize;
	
	/*private void addItem(long an) {
		bytesize += 8;
		toOutputx.add(an);
	}*/
	private void addItem(byte[] an) {
		bytesize += an.length*8;
		toOutputx.add(an);
	}
	
	@Override
	public int put(Object object) {
		//encode type of object
		//visited already?
		//offset?
		if(object == null) {
			put(0l);
		}else {
			Class<?> cls = object.getClass();
			Class<?> compArrayType = null;
			int arLevels = 1;
			byte[] clsName;
			if(cls.isArray()) {
				compArrayType = cls.getComponentType();
				while(compArrayType.isArray()){
					arLevels++;
					compArrayType = compArrayType.getComponentType();
				}
				String strName = compArrayType.getTypeName();
				
				for(int n=0; n < arLevels; n++) {
					strName = "[" + strName;
				}
				//System.err.println("~put class name: "+ strName);
				clsName = strName.getBytes();
			}else {
				//System.err.println("~put class name: "+ cls.getTypeName());
				clsName = (cls.getTypeName()).getBytes();
			}
			put((long)clsName.length);
			addItem(clsName);
			
			
			
			if(graphToObjectInstance.containsKey(object)) {
				put((long)graphToObjectInstance.get(object));//visited already, no cycle!
				objectCount++;
			}else {
				long value = objectCount++;
				put(value);
				graphToObjectInstance.put(object, value);
				if(object instanceof String) {
					putString((String)object);
				}else if(object instanceof Class){
					putString(((Class<?>)object).getName());
				}else if(cls.isEnum()){
					putString((((Enum<?>)object).name()));
				}else if(null != compArrayType){
					if(compArrayType.isPrimitive()){
						if(compArrayType==int.class){
							putIntArray(object, arLevels, false);
						}else if(compArrayType==long.class){
							putLongArray(object, arLevels, false);
						}else if(compArrayType==short.class){
							putShortArray(object, arLevels, false);
						}else if(compArrayType==double.class){
							putDoubleArray(object, arLevels, false);
						}else if(compArrayType==float.class){
							putFloatArray(object, arLevels, false);
						}else if(compArrayType==char.class){
							putCharArray(object, arLevels, false);
						}else if(compArrayType==boolean.class){
							putBooleanArray(object, arLevels, false);
						}else if(compArrayType==byte.class){
							putByteArray(object, arLevels, false);
						}
					}else{
						putObjectArray(object, arLevels, false);
					}
					
				}else {
					((CObject)object).toBinary(this);
				}
			}
		}
		
		return 0;
	}
	
	/*	public int putString(String object) {//special case for String
		if(object == null) {
			put(0l);
		}else {
			byte[] clsName = new String("java.lang.String").getBytes();
			put((long)clsName.length);
			addItem(clsName);
			
			if(graphToObjectInstance.containsKey(object)) {
				put((long)graphToObjectInstance.get(object));//visited already, no cycle!
			}else {
				long value = objectCount++;
				put(value);
				graphToObjectInstance.put(object, value);
			
				byte[] bin = object.getBytes();
				put((long)bin.length);//TODO: we can encode this using the id system to avoid repetition
				addItem(bin);
			}
		}
		return 0;
	}*/
	
	public int putString(String object) {//special case for String
		byte[] bin = object.getBytes();
		
		put((long)bin.length);
		addItem(bin);
		
		return 0;
	}
	
	@Override
	public int put(int aint) {
		bytesize += 4;
		toOutputx.add(aint);
		return 0;
	}

	@Override
	public int put(long along) {
		bytesize += 8;
		toOutputx.add(along);
		return 0;
	}

	@Override
	public int put(double adouble) {
		bytesize += 8;
		toOutputx.add(adouble);
		return 0;
	}

	@Override
	public int put(float aint) {
		bytesize += 4;
		toOutputx.add(aint);
		return 0;
	}

	@Override
	public int put(boolean aint) {
		bytesize += 4;
		toOutputx.add(aint?1:0);
		return 0;
	}

	@Override
	public int put(short aint) {
		bytesize += 2;
		toOutputx.add(aint);
		return 0;
	}

	@Override
	public int put(byte aint) {
		bytesize++;
		toOutputx.add(aint);
		return 0;
	}

	@Override
	public int put(char aint) {
		bytesize+=2;
		toOutputx.add(aint);
		return 0;
	}
	
	////////////////////////////////
	
	private void putIntArray(Object object, int levels, boolean doNullCheck){
		if(doNullCheck){
			if(object==null){
				put(null);
				return;
			}else {
				put((long)1);
			}
			
			if(graphToObjectInstance.containsKey(object)) {
				put((long)graphToObjectInstance.get(object));//visited already, no cycle!
				return;
			}else {
				long value = objectCount++;
				put(value);
				graphToObjectInstance.put(object, value);
			}
		}
	
		int len = Array.getLength(object);
		put(len);
		for(int n=0; n < len; n++){
			Object got = Array.get(object, n);
			if(levels == 1){
				put((int)got);
			}else{
				putIntArray(got, levels-1);
			}
		}
	}
	
	private void putBooleanArray(Object object, int levels, boolean doNullCheck){
		if(doNullCheck){
			if(object==null){
				put(null);
				return;
			}else {
				put((long)1);
			}
			if(graphToObjectInstance.containsKey(object)) {
				put((long)graphToObjectInstance.get(object));//visited already, no cycle!
			}else {
				long value = objectCount++;
				put(value);
				graphToObjectInstance.put(object, value);
			}
		}
		
		int len = Array.getLength(object);
		put(len);
		for(int n=0; n < len; n++){
			Object got = Array.get(object, n);
			if(levels == 1){
				put((boolean)got);
			}else{
				putBooleanArray(got, levels-1);
			}
		}
	}
	
	private void putLongArray(Object object, int levels, boolean doNullCheck){
		if(doNullCheck){
			if(object==null){
				put(null);
				return;
			}else {
				put((long)1);
			}
			
			if(graphToObjectInstance.containsKey(object)) {
				put((long)graphToObjectInstance.get(object));//visited already, no cycle!
			}else {
				long value = objectCount++;
				put(value);
				graphToObjectInstance.put(object, value);
			}
		}
		
		int len = Array.getLength(object);
		put(len);
		for(int n=0; n < len; n++){
			Object got = Array.get(object, n);
			if(levels == 1){
				put((long)got);
			}else{
				putLongArray(got, levels-1);
			}
		}
	}
	
	private void putDoubleArray(Object object, int levels, boolean doNullCheck){
		if(doNullCheck){
			if(object==null){
				put(null);
				return;
			}else {
				put((long)1);
			}
			
			if(graphToObjectInstance.containsKey(object)) {
				put((long)graphToObjectInstance.get(object));//visited already, no cycle!
			}else {
				long value = objectCount++;
				put(value);
				graphToObjectInstance.put(object, value);
			}
		}
		
		int len = Array.getLength(object);
		put(len);
		for(int n=0; n < len; n++){
			Object got = Array.get(object, n);
			if(levels == 1){
				put((double)got);
			}else{
				putDoubleArray(got, levels-1);
			}
		}
	}
	
	private void putFloatArray(Object object, int levels, boolean doNullCheck){
		if(doNullCheck){
			if(object==null){
				put(null);
				return;
			}else {
				put((long)1);
			}
			
			if(graphToObjectInstance.containsKey(object)) {
				put((long)graphToObjectInstance.get(object));//visited already, no cycle!
			}else {
				long value = objectCount++;
				put(value);
				graphToObjectInstance.put(object, value);
			}
		}
		
		int len = Array.getLength(object);
		put(len);
		for(int n=0; n < len; n++){
			Object got = Array.get(object, n);
			if(levels == 1){
				put((float)got);
			}else{
				putFloatArray(got, levels-1);
			}
		}
	}
	
	private void putShortArray(Object object, int levels, boolean doNullCheck){
		if(doNullCheck){
			if(object==null){
				put(null);
				return;
			}else {
				put((long)1);
			}
			
			if(graphToObjectInstance.containsKey(object)) {
				put((long)graphToObjectInstance.get(object));//visited already, no cycle!
			}else {
				long value = objectCount++;
				put(value);
				graphToObjectInstance.put(object, value);
			}
		}
		
		int len = Array.getLength(object);
		put(len);
		for(int n=0; n < len; n++){
			Object got = Array.get(object, n);
			if(levels == 1){
				put((short)got);
			}else{
				putShortArray(got, levels-1);
			}
		}
	}
	
	private void putCharArray(Object object, int levels, boolean doNullCheck){
		if(doNullCheck){
			if(object==null){
				put(null);
				return;
			}else {
				put((long)1);
			}
			
			if(graphToObjectInstance.containsKey(object)) {
				put((long)graphToObjectInstance.get(object));//visited already, no cycle!
			}else {
				long value = objectCount++;
				put(value);
				graphToObjectInstance.put(object, value);
			}
		}
		
		int len = Array.getLength(object);
		put(len);
		for(int n=0; n < len; n++){
			Object got = Array.get(object, n);
			if(levels == 1){
				put((char)got);
			}else{
				putCharArray(got, levels-1);
			}
		}
	}
	
	private void putByteArray(Object object, int levels, boolean doNullCheck){
		if(doNullCheck){
			if(object==null){
				put(null);
				return;
			}else {
				put((long)1);
			}
			
			if(graphToObjectInstance.containsKey(object)) {
				put((long)graphToObjectInstance.get(object));//visited already, no cycle!
			}else {
				long value = objectCount++;
				put(value);
				graphToObjectInstance.put(object, value);
			}
		}
		
		int len = Array.getLength(object);
		put(len);
		for(int n=0; n < len; n++){
			Object got = Array.get(object, n);
			if(levels == 1){
				put((byte)got);
			}else{
				putByteArray(got, levels-1);
			}
		}
	}
	
	private void putObjectArray(Object object, int levels, boolean doNullCheck){
		if(doNullCheck){
			if(object==null){
				put(null);
				return;
			}else {
				Class<?> objCls = object.getClass();
				while(objCls.isArray()) {
					objCls = objCls.getComponentType();
				}
				String compName = objCls.getTypeName();
				for(int n=0; n < levels; n++) {
					compName = "[" + compName;
				}
				byte[] tname = compName.getBytes();
				put((long)tname.length);
				addItem(tname);
			}
			
			//put an object so see if it's already in the graph
			if(graphToObjectInstance.containsKey(object)) {
				put((long)graphToObjectInstance.get(object));//visited already, no cycle!
				return;
			}else {
				long value = objectCount++;
				put(value);
				graphToObjectInstance.put(object, value);
			}
		}
	
		int len = Array.getLength(object);
		put(len);
		for(int n=0; n < len; n++){
			Object got = Array.get(object, n);
			if(levels == 1){
				put(got);
			}else{
				putObjectArray(got, levels-1);
			}
		}
	}
	
	////////////////////////////////
	
	
	@Override
	public int putIntArray(Object object, int levels) {
		putIntArray(object, levels, true);
		return 0;
	}

	@Override
	public int putLongArray(Object object, int levels) {
		putLongArray(object, levels, true);
		return 0;
	}

	@Override
	public int putDoubleArray(Object object, int levels) {
		putDoubleArray(object, levels, true);
		return 0;
	}

	@Override
	public int putFloatArray(Object object, int levels) {
		putFloatArray(object, levels, true);
		return 0;
	}

	@Override
	public int putShortArray(Object object, int levels) {
		putShortArray(object, levels, true);
		return 0;
	}

	@Override
	public int putCharArray(Object object, int levels) {
		putCharArray(object, levels, true);
		return 0;
	}

	@Override
	public int putByteArray(Object object, int levels) {
		putByteArray(object, levels, true);
		return 0;
	}

	@Override
	public int putBooleanArray(Object object, int levels) {
		putBooleanArray(object, levels, true);
		return 0;
	}

	@Override
	public int putObjectArray(Object object, int levels) {
		putObjectArray(object, levels, true);	
		return 0;
	}

}
