package com.concurnas.lang.offheap.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.concurnas.lang.Uninterruptible;
import com.concurnas.lang.offheap.VidConstants;
import com.concurnas.bootstrap.runtime.cps.CObject;
import com.concurnas.lang.Transient;

@Uninterruptible
@Transient
public abstract class OffHeap implements AutoCloseable /*implements ObjectConverterProvider*/ {
	protected OffHeapMalloc offHeapStorage;
	private String storeName;
	private Long capacity;
	private boolean started = false;

	private final ClassLoader defaultClassLoader = OffHeap.class.getClassLoader();
	private ClassLoader classLoader = defaultClassLoader;
	
	private final static long defaultFileSynature = 0x436f6e634f48l;
	
	protected final long uid;
	public static final AtomicLong uidGen = new AtomicLong(1);
	
	OffHeap(){
		uid = uidGen.getAndIncrement();
	}
	
	//core
	protected void checkStarted(){
		if(!started){
			throw new IllegalStateException(String.format("Off heap store%s has not been started", storeName==null?"":": " + storeName));
		}
	}

	private boolean hasNonDefaultClassloaderDefined = false;
	
	public boolean getHasNonDefaultClassloaderDefined(){
		return hasNonDefaultClassloaderDefined;
	}
	
	public void setClassloader(final ClassLoader classloaderpased){
		ClassLoader classloader = classloaderpased;
		if( null == classloader){
			classloader = defaultClassLoader;
		}
		
		boolean replace = !this.classLoader.equals(classloader);
		
		this.classLoader=classloader;
		
		hasNonDefaultClassloaderDefined = defaultClassLoader != classloaderpased;
		
		if(replace && started){
			this.onReplaceClassloader();
		}
		
	}
	
	public ClassLoader getClassloader(){
		return this.classLoader;
	}
	
	
	//extra stuff...
	
	public String getStoreName() {
		return storeName;
	}
	public void setStoreName(String storeName) {
		this.storeName = storeName;
	}

	public void setCapacity(final long capacity){
		if(capacity < 0){
			throw new IllegalArgumentException(String.format("Invalid bytesize specified for off heap store%s: %s", storeName==null?"":": " + storeName, capacity));
		}
		if(started && this.offHeapStorage != null){
			offHeapStorage.setCapacity(capacity);
		}
		
		this.capacity = capacity;
	}
	
	public Long getCapacity(){
		return started && this.offHeapStorage != null ?offHeapStorage.getCapacity(): capacity;
	}
	
	protected long getFileSignature(){
		return defaultFileSynature;
	}
	
	public long getFreeSpace(){
		this.checkStarted();
		return offHeapStorage.getFreeSpace();
	}
	
	public final synchronized void start(){
		offHeapStorage = capacity!=null?new OffHeapMalloc(getFileSignature(), capacity):new OffHeapMalloc(getFileSignature());
		//offHeapStorage.setDiskStorePath(path);
		prepare();
		
		offHeapStorage.start();
		
		started = true;
		postStart();
	}
	
	protected void postStart(){}
	
	public String toString(){
		return "Off Heap Store" + this.storeName==null?"":": " +this.storeName;
	}
	
	protected void prepare() {}
	
	@Override
	public void close(){
		if(null != offHeapStorage){
			this.offHeapStorage.close();
		}
		this.started=false;
	}
	
	@Override
	protected void finalize() throws Throwable { 
		this.close();//not sure if this will work
	}
	
	
	
	//////////////////////////meta and vids

	
	public abstract long getAndIncNextVid();
	
	protected abstract boolean vidToUseContainsKey(long vid);
	protected abstract Long vidToUseGet(long vid);
	protected abstract Long vidToUseRemove(long vid);
	protected abstract Long vidToUsePut(long vid, long count);
	protected abstract Set<Long> vidToUseKeySet();
	
	protected abstract Long classToVidRemove(Class<?> key);
	protected abstract boolean classToVidContainsKey(Class<?> key);
	protected abstract Long classToVidGet(Class<?> key);
	protected abstract Long classToVidPut(Class<?> key, long vid);
	
	protected abstract boolean vidToClassAndMetaContainsKey(long key);
	protected abstract ClassAndMeta vidToClassAndMetaRemove(long key);
	protected  abstract ClassAndMeta vidToClassAndMetaGet(long key) ;
	protected  abstract ClassAndMeta vidToClassAndMetaPut(long vid, ClassAndMeta cam);
	
	protected abstract void onReplaceClassloader();//need to invalidate cache's as approperiate and reload class definitions
	
	/*synchronized void unregisterVidUse(long vid){
		unregisterVidUse(vid, 1);
	}
	*/
	synchronized void unregisterVidUse(long vid, long countx){
		//on delete
		if(vid >= VidConstants.startVid){
			//System.err.println("removing: " + vid + ": " + countx );
			//System.err.println("removing: " + vid + ": " + countx + "  - " + (vid > 100 ? this.vidToClassGet(vid) : ""));
			if(vidToUseContainsKey(vid)){
				long count = vidToUseGet(vid);
				count-=countx;
				if(count <= 0){//how could it be less than 0?
					vidToUseRemove(vid);
					classToVidRemove(vidToClassAndMetaRemove(vid).objClass);
				}else{
					vidToUsePut(vid, count);
				}
			}else{
				throw new IllegalStateException("Class with associated vid not present in vid mapping: " + vid);
			}
		}
		
	}
	
	public synchronized HashMap<Class<?>, Long> getStoredNonPrimativeObjectUsage()  {
		//return some kind of map
		HashMap<Class<?>, Long> ret = new HashMap<Class<?>, Long>();
		
		for(long key : vidToUseKeySet()){
			Class<?> objCls = vidToClassAndMetaGet(key).objClass;
			if(ret.containsKey(objCls)){
				ret.put(objCls, ret.get(objCls) + vidToUseGet(key));
			}else{

				ret.put(objCls, vidToUseGet(key));
			}
			
		}
		
		return ret;
	}
	
	synchronized void registerVidUsage(long vid, long count){
		//System.err.println("registerVidUsage: " + vid + " - " + (vid > 100 ? this.vidToClassGet(vid) : ""));
		if(vid >= VidConstants.startVid){
			vidToUsePut(vid, vidToUseContainsKey(vid)?vidToUseGet(vid)+count:count);
		}
	}
	synchronized void registerVidUsage(Map<Long, Long> vidToCount){
		//System.err.println("addings: " + vidToCount.keySet());
		for(Long vid : vidToCount.keySet()){
			if(vid >= VidConstants.startVid){
				long cnt = vidToCount.get(vid);
				
				vidToUsePut(vid, vidToUseContainsKey(vid)?vidToUseGet(vid)+cnt:cnt);
			}
		}
		
		
	}
	
	synchronized long getObjectClassUIDAndTrackUsage(Class<?> cls, Class<?> overwriteClassInCAM){
		long vid;
		
		if(cls.isPrimitive()){
			if(cls == int.class){
				vid = VidConstants.prim_vid_int;
			}else if(cls == long.class){
				vid = VidConstants.prim_vid_long;
			}else if(cls == double.class){
				vid = VidConstants.prim_vid_double;
			}else if(cls == float.class){
				vid = VidConstants.prim_vid_float;
			}else if(cls == boolean.class){
				vid = VidConstants.prim_vid_boolean;
			}else if(cls == short.class){
				vid = VidConstants.prim_vid_short;
			}else if(cls == byte.class){
				vid = VidConstants.prim_vid_byte;
			}else if(cls == char.class){
				vid = VidConstants.prim_vid_char;
			}else{
				vid = VidConstants.prim_vid_int;
			}
		}else if(cls == Integer.class){
			vid = VidConstants.prim_vid_Integer;
		}else if(cls == Long.class){
			vid = VidConstants.prim_vid_Long;
		}else if(cls == Double.class){
			vid = VidConstants.prim_vid_Double;
		}else if(cls == Float.class){
			vid = VidConstants.prim_vid_Float;
		}else if(cls == Boolean.class){
			vid = VidConstants.prim_vid_Boolean;
		}else if(cls == Short.class){
			vid = VidConstants.prim_vid_Short;
		}else if(cls == Byte.class){
			vid = VidConstants.prim_vid_Byte;
		}else if(cls == Character.class){
			vid = VidConstants.prim_vid_Character;
		}else if(cls == String.class){
			vid = VidConstants.prim_vid_String;
		}else if(cls == ClassAndMeta.class){
			vid = VidConstants.prim_vid_ClassAndMeta;
		} else{
			if(classToVidContainsKey(cls)){
				vid = classToVidGet(cls);
			}
			else{
				vid = this.getAndIncNextVid();
				classToVidPut(cls, vid);
				String[] meta = Utils.callMetaBinary(cls);
				
				ClassAndMeta cam = new ClassAndMeta(overwriteClassInCAM!=null?overwriteClassInCAM:cls, meta);
				
				vidToClassAndMetaPut(vid, cam);
			}
		}
		
		return vid;
	}
	
	private static final ClassAndMeta CObjectCAndM = new ClassAndMeta(CObject.class, null);
	private static final ClassAndMeta prim_vid_intCANDM = new ClassAndMeta(int.class, null);
	private static final ClassAndMeta prim_vid_longCANDM = new ClassAndMeta(long.class, null);
	private static final ClassAndMeta prim_vid_doubleCANDM = new ClassAndMeta(double.class, null);
	private static final ClassAndMeta prim_vid_floatCANDM = new ClassAndMeta(float.class, null);
	private static final ClassAndMeta prim_vid_booleanCANDM = new ClassAndMeta(boolean.class, null);
	private static final ClassAndMeta prim_vid_shortCANDM = new ClassAndMeta(short.class, null);
	private static final ClassAndMeta prim_vid_byteCANDM = new ClassAndMeta(byte.class, null);
	private static final ClassAndMeta prim_vid_charCANDM = new ClassAndMeta(char.class, null);
	private static final ClassAndMeta prim_vid_IntegerCANDM = new ClassAndMeta(Integer.class, null);
	private static final ClassAndMeta prim_vid_LongCANDM = new ClassAndMeta(Long.class, null);
	private static final ClassAndMeta prim_vid_DoubleCANDM = new ClassAndMeta(Double.class, null);
	private static final ClassAndMeta prim_vid_FloatCANDM = new ClassAndMeta(Float.class, null);
	private static final ClassAndMeta prim_vid_BooleanCANDM = new ClassAndMeta(Boolean.class,null);
	private static final ClassAndMeta prim_vid_ShortCANDM = new ClassAndMeta(Short.class, null);
	private static final ClassAndMeta prim_vid_ByteCANDM = new ClassAndMeta(Byte.class, null);
	private static final ClassAndMeta prim_vid_CharacterCANDM = new ClassAndMeta(Character.class, null);
	private static final ClassAndMeta prim_vid_StringCANDM = new ClassAndMeta(String.class, null);
	
	synchronized ClassAndMeta getClassForVid(long vid)  {
		if(vidToClassAndMetaContainsKey(vid)){
			return vidToClassAndMetaGet(vid);
		}
		else if(vid < VidConstants.startVid){
			switch((int)vid){
				case VidConstants.prim_vid_int : return prim_vid_intCANDM;
				case VidConstants.prim_vid_long : return prim_vid_longCANDM;
				case VidConstants.prim_vid_double : return prim_vid_doubleCANDM;
				case VidConstants.prim_vid_float : return prim_vid_floatCANDM;
				case VidConstants.prim_vid_boolean : return prim_vid_booleanCANDM;
				case VidConstants.prim_vid_short : return prim_vid_shortCANDM;
				case VidConstants.prim_vid_byte : return prim_vid_byteCANDM;
				case VidConstants.prim_vid_char : return prim_vid_charCANDM;
				
				case VidConstants.prim_vid_Integer : return prim_vid_IntegerCANDM;
				case VidConstants.prim_vid_Long : return prim_vid_LongCANDM;
				case VidConstants.prim_vid_Double : return prim_vid_DoubleCANDM;
				case VidConstants.prim_vid_Float : return prim_vid_FloatCANDM;
				case VidConstants.prim_vid_Boolean : return prim_vid_BooleanCANDM;
				case VidConstants.prim_vid_Short : return prim_vid_ShortCANDM;
				case VidConstants.prim_vid_Byte : return prim_vid_ByteCANDM;
				case VidConstants.prim_vid_Character : return prim_vid_CharacterCANDM;
				case VidConstants.prim_vid_String : return prim_vid_StringCANDM;
			}
		}
		return CObjectCAndM;
	}
}
