package com.concurnas.lang.offheap.storage;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.HashSet;

import com.concurnas.bootstrap.lang.offheap.MissingField;
import com.concurnas.lang.offheap.util.FieldConverters;
import com.concurnas.lang.offheap.util.FieldConverters.FieldCastType;
import com.concurnas.lang.offheap.util.FieldConverters.FieldCastTypeInfo;
import com.concurnas.lang.offheap.util.FieldConverters.TypeConvert;

class OffHeapTransformationalDecoder<T> extends OffHeapDecoder<T> {

	@Override
	public boolean canThrowMissingFieldException(){
		return true;
	}
	
	static enum SkipFieldType{
		INT, LONG, DOUBLE, FLOAT, BOOLEAN, SHORT, BYTE, CHAR, OBJECT;
	}
	
	static class SkipFieldInfo{
		int arLevels;
		SkipFieldType skipType;

		SkipFieldInfo(SkipFieldType skipType, int arLevels){
			this.skipType = skipType;
			this.arLevels = arLevels;
		}
	}
	
	private final OffHeapDecoder<T> parent;
	private final HashMap<Integer, SkipFieldInfo> missingFields;
	private final HashSet<Integer> addedFields;
	private int srcFieldNo = -1;
	private int getCallCount = 0;
	private final boolean hasMissingFields;
	private final  boolean[] addedFieldsAr;
	private final HashMap<Integer, TypeConvert> castFields;
	
	private boolean firstIntCallForArray = false;//for array operations, first call is alays to determine length
	
	private int processingArrayLevels = 0;
	
	public OffHeapTransformationalDecoder(OffHeapDecoder<T> parent, int fieldCount, HashMap<Integer, SkipFieldInfo> missingFields, HashSet<Integer> addedFields, HashMap<Integer, TypeConvert> castFields) {
		super(parent.engine, parent.vidconverter);
		this.parent = parent;
		super.buffer = this.parent.buffer;
		this.missingFields = missingFields;
		this.addedFields = addedFields;
		this.castFields = castFields;
		
		super.endOfDataRegion=-1;
		super.collectedVids = this.parent.collectedVids;
		
		this.hasMissingFields = !missingFields.isEmpty();
		this.addedFieldsAr = makeAddedFieldsArray(fieldCount);
	}
	
	private boolean[] makeAddedFieldsArray(int fieldCount){
		if(addedFields.isEmpty()){
			return null;
		}else{
			boolean [] ret = new boolean[fieldCount];
			for(int n = 0; n < fieldCount; n++){
				ret[n] = addedFields.contains(n);
			}
			return ret;
		}
	}
	
	@Override
	public boolean[] getFieldsToDefault(){
		return this.addedFieldsAr;
	}
	
	
	private void checkMissing(){
		if(hasMissingFields){
			while(missingFields.containsKey(++srcFieldNo)){
				SkipFieldInfo sfi = missingFields.get(srcFieldNo);
				int arLevels = sfi.arLevels;
				if(arLevels > 0){
					switch(sfi.skipType){
						case INT : this.parent.getIntArray(arLevels); break;
						case LONG : this.parent.getLongArray(arLevels); break;
						case DOUBLE : this.parent.getDoubleArray(arLevels); break;
						case FLOAT : this.parent.getFloatArray(arLevels); break;
						case BOOLEAN : this.parent.getBooleanArray(arLevels); break;
						case SHORT : this.parent.getShortArray(arLevels); break;
						case BYTE : this.parent.getByteArray(arLevels); break;
						case CHAR : this.parent.getCharArray(arLevels); break;
						case OBJECT : this.parent.getObjectArray(arLevels); break;
					}
				}else{
					switch(sfi.skipType){
						case INT : this.parent.getInt(); break;
						case LONG : this.parent.getLong(); break;
						case DOUBLE : this.parent.getDouble(); break;
						case FLOAT : this.parent.getFloat(); break;
						case BOOLEAN : this.parent.getBoolean(); break;
						case SHORT : this.parent.getShort(); break;
						case BYTE : this.parent.getByte(); break;
						case CHAR : this.parent.getChar(); break;
						case OBJECT : this.parent.getObject(); break;
					}
				}
			}
		}
		else{
			srcFieldNo++;
		}
	}
	
	private void precall(){
		
		if(super.endOfDataRegion != -1){//indicates we are processing an array
			return;
		}
		
		//System.err.println("precall: " + getCallCount + " on: " + addedFields);
		//throw exception if field unknown
		if(addedFields.contains(getCallCount++)){
			//System.err.println("throw exep on: " + (getCallCount-1));
			throw new MissingField();
		}
		

		checkMissing();
	}
	
	//
	public int getInt(){
		precall();
		int ret;
		if(firstIntCallForArray){
			ret = this.parent.getInt();
			//this.parent.endOfDataRegion = super.endOfDataRegion;
			//this.parent.buffer.position(super.buffer.position());
			firstIntCallForArray=false;
			//System.err.println("got len: " + ret);
		}else{
			ret = !castFields.isEmpty() && castFields.containsKey(srcFieldNo)? FieldConverters.toInteger(this.parent, castFields.get(srcFieldNo)):this.parent.getInt();
		}
		return ret;
	}

	public long getLong(){
		precall();
		long ret = !castFields.isEmpty() && castFields.containsKey(srcFieldNo)? FieldConverters.toLong(this.parent, castFields.get(srcFieldNo)):this.parent.getLong();
		return ret;
	}

	public double getDouble(){
		precall();
		double ret = !castFields.isEmpty() && castFields.containsKey(srcFieldNo)? FieldConverters.toDouble(this.parent, castFields.get(srcFieldNo)):this.parent.getDouble();
		return ret;
	}

	public float getFloat(){
		precall();
		float ret = !castFields.isEmpty() && castFields.containsKey(srcFieldNo)? FieldConverters.toFloat(this.parent, castFields.get(srcFieldNo)):this.parent.getFloat();
		return ret;
	}

	public boolean getBoolean(){
		precall();
		boolean ret = !castFields.isEmpty() && castFields.containsKey(srcFieldNo)? FieldConverters.toBoolean(this.parent, castFields.get(srcFieldNo)):this.parent.getBoolean();
		return ret;
	}

	public short getShort(){
		precall();
		short ret = !castFields.isEmpty() && castFields.containsKey(srcFieldNo)? FieldConverters.toShort(this.parent, castFields.get(srcFieldNo)):this.parent.getShort();
		return ret;
	}

	public byte getByte(){
		precall();
		byte ret = !castFields.isEmpty() && castFields.containsKey(srcFieldNo)? FieldConverters.toByte(this.parent, castFields.get(srcFieldNo)):this.parent.getByte();
		return ret;
	}

	public char getChar(){
		precall();
		char ret = !castFields.isEmpty() && castFields.containsKey(srcFieldNo)? FieldConverters.toChar(this.parent, castFields.get(srcFieldNo)):this.parent.getChar();
		return ret;
	}

	public T get(long address){
		precall();
		T ret = this.parent.get(address);
		return ret;
	}

	protected Class<?> convertToArrayType(){
		if(!castFields.isEmpty() && castFields.containsKey(srcFieldNo)){
			return castFields.get(srcFieldNo).to.clazz;
		}
		
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public T getObject(){
		precall();
		T ret;
		if(processingArrayLevels >0){
			processingArrayLevels--;
			ret = (T)super.getObject();
			processingArrayLevels++;
		}else{
			if(!castFields.isEmpty() && castFields.containsKey(srcFieldNo)){
				TypeConvert typeConv = castFields.get(srcFieldNo);
				FieldCastTypeInfo toType = typeConv.to;
				if(toType.arLevels > 0 && super.endOfDataRegion == -1){//we're pulling an array out...

					firstIntCallForArray=true;
					int prevEndOfDataRegion = super.endOfDataRegion;
					super.endOfDataRegion = this.parent.endOfDataRegion;
					int prevlevels = processingArrayLevels;
					processingArrayLevels = typeConv.from.arLevels;
					
					
					FieldCastType fromTypex = typeConv.from.type;
					if(fromTypex.isPrimative){
						ret = null;
						srcFieldNo--;
						switch(toType.type){
							case BOXED_BYTE: ret    = (T)this.primToBoxedByteArray(typeConv.from.arLevels, typeConv); break;
							case BOXED_CHAR: ret    = (T)this.primToBoxedCharacterArray(typeConv.from.arLevels, typeConv); break;
							case BOXED_DOUBLE: ret  = (T)this.primToBoxedDoubleArray(typeConv.from.arLevels, typeConv); break;
							case BOXED_FLOAT: ret   = (T)this.primToBoxedFloatArray(typeConv.from.arLevels, typeConv); break;
							case BOXED_INT: ret     = (T)this.primToBoxedIntegerArray(typeConv.from.arLevels, typeConv); break;
							case BOXED_LONG: ret    = (T)this.primToBoxedLongArray(typeConv.from.arLevels, typeConv); break;
							case BOXED_SHORT: ret   = (T)this.primToBoxedShortArray(typeConv.from.arLevels, typeConv); break;
							case BOXED_BOOLEAN: ret = (T)this.primToBoxedBooleanArray(typeConv.from.arLevels, typeConv); break;
							case STRING: ret = (T)this.primToStringArray(typeConv.from.arLevels, typeConv); break;
							default: srcFieldNo++;//err in geting here but this resets the state
						}
						srcFieldNo++;
						
					}else{//boxed or object
						ret = this.getObject();
					}
					

					processingArrayLevels = prevlevels;
					this.parent.endOfDataRegion = super.endOfDataRegion;
					super.endOfDataRegion = prevEndOfDataRegion;
					firstIntCallForArray=false;
					
				}else{
					ret =  (T)FieldConverters.toObject(this.parent, typeConv);
				}
			}else{
				ret = (T)this.parent.getObject();
			}
		}
		//System.err.println("dec got obj: " + ret);
		return ret;
	}

	private Object primToBoxedByteArray(int levels, TypeConvert typeConvert) {
		this.parent.checkBuffer();
		byte objType = buffer.get();//test[cursor++];
		if(objType == OffHeapEncoder.OBJECT_NULL){
			return null;
		}
		
		int len=getInt();
		
		if(levels == 1){
			Byte[] ret = new Byte[len];
			for(int n=0; n < len; n++){
				ret[n] = FieldConverters.toByte(this.parent, typeConvert);
			}
			
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(Byte.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, primToBoxedByteArray(levels-1, typeConvert));
			}
			return ret;
		}
	}
	
	private Object primToBoxedCharacterArray(int levels, TypeConvert typeConvert) {
		this.parent.checkBuffer();
		byte objType = buffer.get();//test[cursor++];
		if(objType == OffHeapEncoder.OBJECT_NULL){
			return null;
		}
		
		int len=getInt();
		
		if(levels == 1){
			Character[] ret = new Character[len];
			for(int n=0; n < len; n++){
				ret[n] = FieldConverters.toChar(this.parent, typeConvert);
			}
			
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(Character.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, primToBoxedCharacterArray(levels-1, typeConvert));
			}
			return ret;
		}
	}
	
	private Object primToBoxedFloatArray(int levels, TypeConvert typeConvert) {
		this.parent.checkBuffer();
		byte objType = buffer.get();//test[cursor++];
		if(objType == OffHeapEncoder.OBJECT_NULL){
			return null;
		}
		
		int len=getInt();
		
		if(levels == 1){
			Float[] ret = new Float[len];
			for(int n=0; n < len; n++){
				ret[n] = FieldConverters.toFloat(this.parent, typeConvert);
			}
			
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(Float.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, primToBoxedFloatArray(levels-1, typeConvert));
			}
			return ret;
		}
	}
	
	private Object primToBoxedIntegerArray(int levels, TypeConvert typeConvert) {
		this.parent.checkBuffer();
		byte objType = buffer.get();//test[cursor++];
		if(objType == OffHeapEncoder.OBJECT_NULL){
			return null;
		}
		
		int len=getInt();
		
		if(levels == 1){
			Integer[] ret = new Integer[len];
			for(int n=0; n < len; n++){
				ret[n] = FieldConverters.toInteger(this.parent, typeConvert);
			}
			
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(Integer.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, primToBoxedIntegerArray(levels-1, typeConvert));
			}
			return ret;
		}
	}
	
	private Object primToBoxedLongArray(int levels, TypeConvert typeConvert) {
		this.parent.checkBuffer();
		byte objType = buffer.get();//test[cursor++];
		if(objType == OffHeapEncoder.OBJECT_NULL){
			return null;
		}
		
		int len=getInt();
		
		if(levels == 1){
			Long[] ret = new Long[len];
			for(int n=0; n < len; n++){
				ret[n] = FieldConverters.toLong(this.parent, typeConvert);
			}
			
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(Long.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, primToBoxedLongArray(levels-1, typeConvert));
			}
			return ret;
		}
	}
	
	private Object primToBoxedShortArray(int levels, TypeConvert typeConvert) {
		this.parent.checkBuffer();
		byte objType = buffer.get();//test[cursor++];
		if(objType == OffHeapEncoder.OBJECT_NULL){
			return null;
		}
		
		int len=getInt();
		
		if(levels == 1){
			Short[] ret = new Short[len];
			for(int n=0; n < len; n++){
				ret[n] = FieldConverters.toShort(this.parent, typeConvert);
			}
			
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(Short.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, primToBoxedShortArray(levels-1, typeConvert));
			}
			return ret;
		}
	}
	
	private Object primToBoxedBooleanArray(int levels, TypeConvert typeConvert) {
		this.parent.checkBuffer();
		byte objType = buffer.get();//test[cursor++];
		if(objType == OffHeapEncoder.OBJECT_NULL){
			return null;
		}
		
		int len=getInt();
		
		if(levels == 1){
			Boolean[] ret = new Boolean[len];
			for(int n=0; n < len; n++){
				ret[n] = FieldConverters.toBoolean(this.parent, typeConvert);
			}
			
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(Boolean.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, primToBoxedBooleanArray(levels-1, typeConvert));
			}
			return ret;
		}
	}
	
	private Object primToStringArray(int levels, TypeConvert typeConvert) {
		this.parent.checkBuffer();
		byte objType = buffer.get();//test[cursor++];
		if(objType == OffHeapEncoder.OBJECT_NULL){
			return null;
		}
		
		int len=getInt();
		
		if(levels == 1){
			String[] ret = new String[len];
			for(int n=0; n < len; n++){
				ret[n] = (String)FieldConverters.toObject(this.parent, typeConvert);
			}
			
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(String.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, primToStringArray(levels-1, typeConvert));
			}
			return ret;
		}
	}
	
	private Object primToBoxedDoubleArray(int levels, TypeConvert typeConvert) {
		this.parent.checkBuffer();
		byte objType = buffer.get();//test[cursor++];
		if(objType == OffHeapEncoder.OBJECT_NULL){
			return null;
		}
		
		int len=getInt();
		
		if(levels == 1){
			Double[] ret = new Double[len];
			for(int n=0; n < len; n++){
				ret[n] = FieldConverters.toDouble(this.parent, typeConvert);
			}
			
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(Double.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, primToBoxedDoubleArray(levels-1, typeConvert));
			}
			return ret;
		}
	}

	public Object getFloatArray(int levels){
		precall();
		
		firstIntCallForArray=true;
		int prevEndOfDataRegion = super.endOfDataRegion;
		super.endOfDataRegion = this.parent.endOfDataRegion;
		int prevlevels = processingArrayLevels;
		processingArrayLevels = levels;
		Object ret;
		if(!castFields.isEmpty() && castFields.containsKey(srcFieldNo)){
			TypeConvert converter = castFields.get(srcFieldNo);
			if(converter.from.type.isPrimative){
				ret = super.getFloatArray(levels);
			}else{
				ret = super.getObject();
			}
		}else{
			ret = super.getFloatArray(levels);
		}
		
		processingArrayLevels = prevlevels;
		this.parent.endOfDataRegion = super.endOfDataRegion;
		super.endOfDataRegion = prevEndOfDataRegion;
		firstIntCallForArray=false;
		
		return ret;
	}
	
	public Object getIntArray(int levels){
		precall();
		
		firstIntCallForArray=true;
		int prevEndOfDataRegion = super.endOfDataRegion;
		super.endOfDataRegion = this.parent.endOfDataRegion;
		int prevlevels = processingArrayLevels;
		processingArrayLevels = levels;
		Object ret;
		if(!castFields.isEmpty() && castFields.containsKey(srcFieldNo)){
			TypeConvert converter = castFields.get(srcFieldNo);
			if(converter.from.type.isPrimative){
				ret = super.getIntArray(levels);
			}else{
				ret = super.getObject();
			}
		}else{
			ret = super.getIntArray(levels);
		}
		
		processingArrayLevels = prevlevels;
		this.parent.endOfDataRegion = super.endOfDataRegion;
		super.endOfDataRegion = prevEndOfDataRegion;
		firstIntCallForArray=false;
		
		return ret;
	}
	

	public Object getDoubleArray(int levels){
		precall();
		
		firstIntCallForArray=true;
		int prevEndOfDataRegion = super.endOfDataRegion;
		super.endOfDataRegion = this.parent.endOfDataRegion;
		int prevlevels = processingArrayLevels;
		processingArrayLevels = levels;
		Object ret;
		if(!castFields.isEmpty() && castFields.containsKey(srcFieldNo)){
			TypeConvert converter = castFields.get(srcFieldNo);
			if(converter.from.type.isPrimative){
				ret = super.getDoubleArray(levels);
			}else{
				ret = super.getObject();
			}
		}else{
			ret = super.getDoubleArray(levels);
		}
		
		processingArrayLevels = prevlevels;
		this.parent.endOfDataRegion = super.endOfDataRegion;
		super.endOfDataRegion = prevEndOfDataRegion;
		firstIntCallForArray=false;
		
		return ret;
	}

	public Object getLongArray(int levels){
		precall();
		
		firstIntCallForArray=true;
		int prevEndOfDataRegion = super.endOfDataRegion;
		super.endOfDataRegion = this.parent.endOfDataRegion;
		int prevlevels = processingArrayLevels;
		processingArrayLevels = levels;
		Object ret;
		if(!castFields.isEmpty() && castFields.containsKey(srcFieldNo)){
			TypeConvert converter = castFields.get(srcFieldNo);
			if(converter.from.type.isPrimative){
				ret = super.getLongArray(levels);
			}else{
				ret = super.getObject();
			}
		}else{
			ret = super.getLongArray(levels);
		}
		
		processingArrayLevels = prevlevels;
		this.parent.endOfDataRegion = super.endOfDataRegion;
		super.endOfDataRegion = prevEndOfDataRegion;
		firstIntCallForArray=false;
		
		return ret;
	}

	public Object getShortArray(int levels){
		precall();
		
		firstIntCallForArray=true;
		int prevEndOfDataRegion = super.endOfDataRegion;
		super.endOfDataRegion = this.parent.endOfDataRegion;
		int prevlevels = processingArrayLevels;
		processingArrayLevels = levels;
		Object ret;
		if(!castFields.isEmpty() && castFields.containsKey(srcFieldNo)){
			TypeConvert converter = castFields.get(srcFieldNo);
			if(converter.from.type.isPrimative){
				ret = super.getShortArray(levels);
			}else{
				ret = super.getObject();
			}
		}else{
			ret = super.getShortArray(levels);
		}
		
		processingArrayLevels = prevlevels;
		this.parent.endOfDataRegion = super.endOfDataRegion;
		super.endOfDataRegion = prevEndOfDataRegion;
		firstIntCallForArray=false;
		
		return ret;
	}

	public Object getCharArray(int levels){
		precall();
		
		firstIntCallForArray=true;
		int prevEndOfDataRegion = super.endOfDataRegion;
		super.endOfDataRegion = this.parent.endOfDataRegion;
		int prevlevels = processingArrayLevels;
		processingArrayLevels = levels;
		Object ret;
		if(!castFields.isEmpty() && castFields.containsKey(srcFieldNo)){
			TypeConvert converter = castFields.get(srcFieldNo);
			if(converter.from.type.isPrimative){
				ret = super.getCharArray(levels);
			}else{
				ret = super.getObject();
			}
		}else{
			ret = super.getCharArray(levels);
		}
		
		processingArrayLevels = prevlevels;
		this.parent.endOfDataRegion = super.endOfDataRegion;
		super.endOfDataRegion = prevEndOfDataRegion;
		firstIntCallForArray=false;
		
		return ret;
	}

	public Object getByteArray(int levels){
		precall();
		
		firstIntCallForArray=true;
		int prevEndOfDataRegion = super.endOfDataRegion;
		super.endOfDataRegion = this.parent.endOfDataRegion;
		int prevlevels = processingArrayLevels;
		processingArrayLevels = levels;
		Object ret;
		if(!castFields.isEmpty() && castFields.containsKey(srcFieldNo)){
			TypeConvert converter = castFields.get(srcFieldNo);
			if(converter.from.type.isPrimative){
				ret = super.getByteArray(levels);
			}else{
				ret = super.getObject();
			}
		}else{
			ret = super.getByteArray(levels);
		}
		
		processingArrayLevels = prevlevels;
		this.parent.endOfDataRegion = super.endOfDataRegion;
		super.endOfDataRegion = prevEndOfDataRegion;
		firstIntCallForArray=false;
		
		return ret;
	}

	public Object getBooleanArray(int levels){
		precall();
		
		firstIntCallForArray=true;
		int prevEndOfDataRegion = super.endOfDataRegion;
		super.endOfDataRegion = this.parent.endOfDataRegion;
		int prevlevels = processingArrayLevels;
		processingArrayLevels = levels;
		Object ret;
		if(!castFields.isEmpty() && castFields.containsKey(srcFieldNo)){
			TypeConvert converter = castFields.get(srcFieldNo);
			if(converter.from.type.isPrimative){
				ret = super.getBooleanArray(levels);
			}else{
				ret = super.getObject();
			}
		}else{
			ret = super.getBooleanArray(levels);
		}
		
		processingArrayLevels = prevlevels;
		this.parent.endOfDataRegion = super.endOfDataRegion;
		super.endOfDataRegion = prevEndOfDataRegion;
		firstIntCallForArray=false;
		
		return ret;
	}
	
	public Object getObjectArray(int levels){
		precall();
		
		firstIntCallForArray=true;
		int prevEndOfDataRegion = super.endOfDataRegion;
		super.endOfDataRegion = this.parent.endOfDataRegion;
		int prevlevels = processingArrayLevels;
		processingArrayLevels = levels;
		Object ret;
		if(!castFields.isEmpty() && castFields.containsKey(srcFieldNo)){
			TypeConvert converter = castFields.get(srcFieldNo);
			if(converter.from.type.isPrimative){
				ret = super.getObjectArray(levels);
			}else{
				ret = super.getObject();
			}
		}else{
			ret = super.getObjectArray(levels);
		}
		
		processingArrayLevels = prevlevels;
		this.parent.endOfDataRegion = super.endOfDataRegion;
		super.endOfDataRegion = prevEndOfDataRegion;
		firstIntCallForArray=false;
		
		return ret;
	}
}
