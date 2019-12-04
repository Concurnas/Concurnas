package com.concurnas.lang.offheap.storage;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

import com.concurnas.bootstrap.lang.offheap.Decoder;
import com.concurnas.bootstrap.runtime.InitUncreatable;
import com.concurnas.bootstrap.runtime.cps.CObject;
import com.concurnas.lang.Uninterruptible;
import com.concurnas.lang.util.LRUCache;

@Uninterruptible
class OffHeapDecoder<T> implements Decoder {
	protected final MallocProvider engine;
	protected final OffHeap vidconverter;

	public OffHeapDecoder(MallocProvider engine, OffHeap vidconverter){
		this.engine = engine;
		this.vidconverter = vidconverter;
	}
	
	@Override
	public boolean canThrowMissingFieldException(){
		return false;
	}
	
	public HashMap<Long, Object> offsetToObject = new HashMap<Long, Object>();
	
	protected void checkBuffer(){
		if(endOfDataRegion <= buffer.position() ){
			throw new RuntimeException(String.format("Tried to decode data past allocated region: address: %s region: %s", buffer.position(), endOfDataRegion));
		}
	}
	
	@Override
	public int getInt(){
		checkBuffer();
		return buffer.getInt();
	}
	
	@Override
	public long getLong(){
		checkBuffer();
		return buffer.getLong();
	}
	
	@Override
	public double getDouble(){
		checkBuffer();
		return buffer.getDouble();
	}
	
	@Override
	public float getFloat(){
		checkBuffer();
		return buffer.getFloat();
	}
	
	@Override
	public boolean getBoolean(){
		checkBuffer();
		return (byte)1 == buffer.get();
	}
	
	@Override
	public short getShort(){
		checkBuffer();
		return buffer.getShort();
	}
	
	@Override
	public byte getByte(){
		checkBuffer();
		return buffer.get();
	}
	
	@Override
	public char getChar(){
		checkBuffer();
		return buffer.getChar();
	}
	
	protected ByteBuffer buffer=null;
	protected int startPosition;
	protected long address;
	
	public HashMap<Long, Long> getVids(long address) {
		this.address = address;
		HashMap<Long, Long> ret = new HashMap<Long, Long>();
		collectedVids = ret;
		getObject();
		collectedVids=null;
		return ret;
	}
	
/*	private void collectVid(long vid){
		//System.err.println("collected Vid: " + vid);
		collectedVids.put(vid, collectedVids.containsKey(vid)?collectedVids.get(vid)+1:1);
	}*/
	
	/* (non-Javadoc)
	 * @see com.concurnas.lang.offHeap.storage.Decoder#get(long)
	 */
	@Override
	public T get(long address){
		this.address = address;
		return getObject();
	}
	
	protected HashMap<Long, Long> collectedVids = null;

	
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
	
	protected int endOfDataRegion=0;//endOfDataRegion and start of vidmap
	
	private static final int CAM_TO_DECODER_CACHE_SUZE = 300;
	@SuppressWarnings("rawtypes")
	LRUCache<ClassAndMeta, DecoderProvider> cacheClassAndMetaToDecoder = new LRUCache<ClassAndMeta, DecoderProvider>(CAM_TO_DECODER_CACHE_SUZE);
	//public HashSet<ClassAndMeta> clearCAMCasheOfItemOnGet;
	
	@SuppressWarnings("rawtypes")
	public void onReplaceClassloader() {
		this.cacheClassAndMetaToDecoder = new LRUCache<ClassAndMeta, DecoderProvider>(CAM_TO_DECODER_CACHE_SUZE);
	}
	
	private static class Providethis<T> implements DecoderProvider<T>{
		private OffHeapDecoder<T> self;

		Providethis(OffHeapDecoder<T> self){
			this.self = self;
		}
		
		@Override
		public Decoder provide(OffHeapDecoder<T> dec) {
			return self;
		}
	}
	
	@SuppressWarnings("unchecked")
	private Decoder getDecoder(ClassAndMeta classAndMeta){
		//cache converter for me!
		//System.err.println(String.format("get decoder for: %s | %s ", classAndMeta.objClass, Arrays.toString(classAndMeta.meta)));
		
		if(this.cacheClassAndMetaToDecoder.containsKey(classAndMeta)){
			//System.err.println(String.format("found decoder for: %s | %s ", classAndMeta.objClass, Arrays.toString(classAndMeta.meta)));
			/*if(null != clearCAMCasheOfItemOnGet && clearCAMCasheOfItemOnGet.contains(classAndMeta)){
				return this.cacheClassAndMetaToDecoder.remove(classAndMeta).provide(this);//for top level use only not for nested items
			}else{
				return this.cacheClassAndMetaToDecoder.get(classAndMeta).provide(this);
			}*/
			
			return this.cacheClassAndMetaToDecoder.get(classAndMeta).provide(this);
		}
		else{
			DecoderProvider<T> decoder = null;
			Class<?> objClass = classAndMeta.objClass;
			if(classAndMeta.meta != null){
				String[] declMeta = Utils.callMetaBinary(objClass);
				
				if(declMeta != null){
					//System.err.println("stored " + Arrays.toString(classAndMeta.meta));
					//System.err.println("to " + Arrays.toString(declMeta));
					//System.err.println("len stored: " + classAndMeta.meta.length) ;
					//System.err.println("len now: " + declMeta.length) ;
					if(!Utils.twoStringArEquals(classAndMeta.meta, declMeta)){
						//difference, so field added, remove or type changed
						
						decoder = Utils.createTransformationalDecoder(this, classAndMeta.meta, declMeta);
					}
				}
			}
			if(null == decoder){
				decoder = new Providethis<T>(this); 
			}
			
			this.cacheClassAndMetaToDecoder.put(classAndMeta, decoder);
			//System.err.println("dec: " + decoder.getClass());
			return decoder.provide(this);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.concurnas.lang.offHeap.storage.Decoder#getObject()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T getObject() {
		boolean bufferNull = null == buffer;
		int vidMapOffset=0;
		if(bufferNull){
			buffer =  engine.getBuffer(address);
			startPosition = buffer.position();
			vidMapOffset = buffer.getInt();
			endOfDataRegion = startPosition + vidMapOffset;
		}
		
		
		if(collectedVids!=null){//read from map at end of object def
			if(vidMapOffset > 0){
				int pos = buffer.position();
				//shift to location of vidmap
				buffer.position(endOfDataRegion);
				int entryCount = buffer.getInt();
				
				for(int n=0; n < entryCount; n++){
					long thevid = buffer.getLong();
					long theCount = buffer.getLong();
					collectedVids.put(thevid, collectedVids.containsKey(thevid)?collectedVids.get(thevid)+theCount:theCount);
				}
				buffer.position(pos);
			}
		}
		
		try{
			checkBuffer();
			byte objType = buffer.get();//test[cursor++];
			//System.err.println("obj type: " + objType);
			if(objType == OffHeapEncoder.OBJECT_NULL){
				return null;
			}
			else if(objType == OffHeapEncoder.OBJECT_SERIALIZED){//real object to decode
				long vid = getVid();
				//System.err.println(String.format("<< got vid: %s", vid));
				int pos = buffer.position();
				ClassAndMeta classAndMeta  = this.vidconverter.getClassForVid(vid);
				
				Class<?> objClass = classAndMeta.objClass;
				//System.err.println(String.format("<< decoder got obj cls: %s | meta: %s", objClass, Arrays.toString(classAndMeta.meta)));
				
				buffer.position(pos);
				try {
					long abspos = address + buffer.position() - startPosition;
					if(objClass.isEnum()){
						String name = new String((byte[])this.getByteArray(1));
						T got = (T)Enum.valueOf((Class)objClass, name);
						//System.err.println(String.format("enum name: %s -> %s", name, got));
						return got;
						//Object instance = getMethodFromCache(objClass).invoke(null, this);
						//this.offsetToObject.put(abspos, instance);
						//return (T)instance;
					}else{
						if(objClass == String.class){
							return (T)((Object)new String((byte[])this.getByteArray(1)));//lol
						}else if(objClass == Integer.class){
							return (T)new Integer(this.getInt());
						}else if(objClass == Double.class){
							return (T)new Double(this.getDouble());
						}else if(objClass == Float.class){
							return (T)new Float(this.getFloat());
						}else if(objClass == Long.class){
							return (T)new Long(this.getLong());
						}else if(objClass == Boolean.class){
							return (T)new Boolean(this.getBoolean());
						}else if(objClass == Character.class){
							return (T)new Character(this.getChar());
						}else if(objClass == Byte.class){
							return (T)new Byte(this.getByte());
						}else if(objClass == Short.class){
							return (T)new Short(this.getShort());
						}else if(objClass == Class.class){
							String cannonicalName = new String((byte[])this.getByteArray(1));
							//System.err.println("cannonicalName:" + cannonicalName);
							return (T)Class.forName(cannonicalName, true, this.vidconverter.getClassloader());
						}else{//most common path...
							//System.err.println("new inst of: " + objClass);
							//System.err.println("det decoder for vid: " + vid);
							Decoder dec = getDecoder(classAndMeta);
							//System.err.println("new using dec: " + dec);
							Object instance = getConstructorFromCache(objClass).newInstance(new Object[]{null, dec.getFieldsToDefault()});
							this.offsetToObject.put(abspos, instance);
							
							((CObject)instance).fromBinary(dec);
							
							return (T)instance;
						}
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			else if(objType == OffHeapEncoder.OBJECT_ARRAY){//real array to decode
				int dimentions = this.buffer.getInt();
				return (T)getObjectArrayKnownArray(dimentions);
			}
			else{//OBJECT_POINTER -> loop to existing object
				long objOffset = this.buffer.getLong();//which object?
				T ret = (T)this.offsetToObject.get(objOffset);
				//System.err.println("offset: " + objOffset + " pointed to: " + ret);
				return  ret;
			}
		}
		finally{
			if(bufferNull){
				buffer=null;
			}
		}
	}
	
	@Override
	public Object getIntArray( int levels){
		return getIntArray(levels, true);
	}
	
	@Override
	public Object getFloatArray( int levels){
		return getFloatArray(levels, true);
	}
	
	@Override
	public Object getDoubleArray( int levels){
		return getDoubleArray(levels, true);
	}
	
	@Override
	public Object getLongArray( int levels){
		return getLongArray(levels, true);
	}
	
	@Override
	public Object getShortArray( int levels){
		return getShortArray(levels, true);
	}
	
	@Override
	public Object getCharArray( int levels){
		return getCharArray(levels, true);
	}
	
	@Override
	public Object getByteArray( int levels){
		return getByteArray(levels, true);
	}
	
	@Override
	public Object getBooleanArray( int levels){
		return getBooleanArray(levels, true);
	}
	
	private Object getIntArray( int levels, boolean doNullCheck){
		if(doNullCheck){
			checkBuffer();
			byte objType = buffer.get();//test[cursor++];
			if(objType == OffHeapEncoder.OBJECT_NULL){
				return null;
			}
		}
		
		int len=getInt();
		
		if(levels == 1){
			int[] ret = new int[len];
			for(int n=0; n < len; n++){
				ret[n] = getInt();
			}
			
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(int.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, getIntArray(levels-1));
			}
			return ret;
		}
	}
	
	private Object getLongArray( int levels, boolean doNullCheck){
		if(doNullCheck){
			checkBuffer();
			byte objType = buffer.get();//test[cursor++];
			if(objType == OffHeapEncoder.OBJECT_NULL){
				return null;
			}
		}
		
		int len=getInt();
		
		if(levels == 1){
			long[] ret = new long[len];
			for(int n=0; n < len; n++){
				ret[n] = getLong();
			}
			
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(long.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, getLongArray(levels-1));
			}
			return ret;
		}
	}
	
	private Object getDoubleArray( int levels, boolean doNullCheck){
		if(doNullCheck){
			checkBuffer();
			byte objType = buffer.get();//test[cursor++];
			if(objType == OffHeapEncoder.OBJECT_NULL){
				return null;
			}
		}
		
		int len=getInt();
		
		if(levels == 1){
			double[] ret = new double[len];
			for(int n=0; n < len; n++){
				ret[n] = getDouble();
			}
			
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(double.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, getDoubleArray(levels-1));
			}
			return ret;
		}
	}
	
	private Object getFloatArray( int levels, boolean doNullCheck){
		if(doNullCheck){
			checkBuffer();
			byte objType = buffer.get();//test[cursor++];
			//System.err.println("objType: " + objType);
			if(objType == OffHeapEncoder.OBJECT_NULL){
				return null;
			}
		}
		
		int len=getInt();
		//System.err.println("len: " + len);
		
		if(levels == 1){
			float[] ret = new float[len];
			for(int n=0; n < len; n++){
				ret[n] = getFloat();
			}
			
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(float.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, getFloatArray(levels-1));
			}
			return ret;
		}
	}
	
	
	private Object getBooleanArray( int levels, boolean doNullCheck){
		if(doNullCheck){
			checkBuffer();
			byte objType = buffer.get();//test[cursor++];
			if(objType == OffHeapEncoder.OBJECT_NULL){
				return null;
			}
		}
		
		int len=getInt();
		
		if(levels == 1){
			boolean[] ret = new boolean[len];
			for(int n=0; n < len; n++){
				ret[n] = getBoolean();
			}
			
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(boolean.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, getBooleanArray(levels-1));
			}
			return ret;
		}
	}
	
	
	private Object getShortArray( int levels, boolean doNullCheck){
		if(doNullCheck){
			checkBuffer();
			byte objType = buffer.get();//test[cursor++];
			if(objType == OffHeapEncoder.OBJECT_NULL){
				return null;
			}
		}
		
		int len=getInt();
		
		if(levels == 1){
			short[] ret = new short[len];
			for(int n=0; n < len; n++){
				ret[n] = getShort();
			}
			
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(short.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, getShortArray(levels-1));
			}
			return ret;
		}
	}
	
	private Object getByteArray( int levels, boolean doNullCheck){
		if(doNullCheck){
			checkBuffer();
			byte objType = buffer.get();//test[cursor++];
			if(objType == OffHeapEncoder.OBJECT_NULL){
				return null;
			}
		}
		
		int len=getInt();
		
		if(levels == 1){
			byte[] ret = new byte[len];
			for(int n=0; n < len; n++){
				ret[n] = getByte();
			}
			
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(byte.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, getByteArray(levels-1));
			}
			return ret;
		}
	}
	
	private Object getCharArray( int levels, boolean doNullCheck){
		if(doNullCheck){
			checkBuffer();
			byte objType = buffer.get();//test[cursor++];
			if(objType == OffHeapEncoder.OBJECT_NULL){
				return null;
			}
		}
		
		int len=getInt();
		
		if(levels == 1){
			char[] ret = new char[len];
			for(int n=0; n < len; n++){
				ret[n] = getChar();
			}
			
			return ret;
		}else{
			int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
			dimensions[0]=len;
			Object ret = Array.newInstance(char.class, dimensions);
			
			for(int n=0; n < len; n++){
				Array.set(ret, n, getCharArray(levels-1));
			}
			return ret;
		}
	}
	
	private long getVid(){
		checkBuffer();
		return buffer.getLong();
	}
	
	private int getArrayLength(){
		checkBuffer();
		return buffer.getInt();
	}
	
	
	protected Object getObjectArrayKnownArray( int levels){
		long vid = getVid();
		
		//System.err.println("vid: " + vid);
		
		//Class<?> objClass = this.vidconverter.getClassForVid(vid);//this.getClassForVid(vid) /*expected*/;
		

		ClassAndMeta /*Class<?>*/ classAndMeta  = this.vidconverter.getClassForVid(vid);
		
		Class<?> objClass = classAndMeta.objClass;
		//System.err.println("class for vid: " + objClass);
		
		Class<?> convertTo = convertToArrayType();
		if(null != convertTo){
			objClass = convertTo;
			//System.err.println("override class to vid: " + objClass);
		}
		
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
			int len=getArrayLength();
			//System.err.println("getObjectArrayKnownArray len: " + len);
			if(collectedVids!=null){
				for(int n=0; n < len; n++){
					getObject();
				}
				return null;
			}else{
				int[] dimensions = new int[levels];//int[] dimentions = new int[] { 3, 0, 0 };
				dimensions[0]=len;
				Object ret = Array.newInstance(objClass, dimensions);
				for(int n=0; n < len; n++){
					Array.set(ret, n, getObject());
				}
				
				return ret;
			}
		}
	}
	
	protected Class<?> convertToArrayType(){
		return null;
	}

	@Override
	public Object getObjectArray(int levels) {
		return getObject();
	}
	
	@Override
	public boolean[] getFieldsToDefault(){
		return null;
	}
	
}