package com.concurnas.lang.offheap.storage;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.concurnas.lang.Uninterruptible;
import com.concurnas.lang.offheap.ManagedOffHeapObject;
import com.concurnas.lang.offheap.OffHeapObject;
import com.concurnas.lang.offheap.OffHeapOutOfMemoryError;
import com.concurnas.lang.offheap.VidConstants;

@Uninterruptible
public abstract class OffHeapPutGettable<T> extends OffHeap implements AutoCloseable {

	private OffHeapEncoder<T> encoder;
	private OffHeapDecoder<T> decoder;
	
	public OffHeapObject<T> put(T obj){
		checkStarted();
		long address;
		try{
			address = encoder.encode(obj);
		} catch (OffHeapOutOfMemoryError oom){
			System.gc();//might call finalizers and free up memory of created objects 
			address = encoder.encode(obj);
			throw oom;
		}
		//TODO: gennerate DMAObject class, conforming to DMAObject interface
		return new ManagedOffHeapObject<T>(address, this);
	}
	
	@Override
	public void prepare(){
		encoder = new OffHeapEncoder<T>(offHeapStorage, this);
		decoder = new OffHeapDecoder<T>(offHeapStorage, this);
	}

	public T get(OffHeapObject<T> ofobj){
		checkStarted();
		if(ofobj.getManager() != this){
			String storeName = super.getStoreName();
			throw new IllegalStateException(String.format("Off heap object is not managed by store", storeName==null?"":": " + storeName));//TODO: test me
		}
		return (T)decoder.get(ofobj.getAddress());
	}

	public void delete(OffHeapObject<T> ofobj){
		//find vids used
		checkStarted();
		HashMap<Long, Long> vidsToCntToDelete = decoder.getVids(ofobj.getAddress());
		offHeapStorage.free(ofobj.getAddress());
		for(Long vid : vidsToCntToDelete.keySet()){
			unregisterVidUse(vid, vidsToCntToDelete.get(vid));
		}
		
		ofobj.invalidate();
	}
	
	//vids...
	
	
	
	

	private long nextVid = VidConstants.startVid+1;//on persisted version needs to be stored
	private ConcurrentHashMap<Long, ClassAndMeta> vidToClassAndMeta = new ConcurrentHashMap<Long, ClassAndMeta>();
	private ConcurrentHashMap<Class<?>, Long> classToVid = new ConcurrentHashMap<Class<?>, Long>();
	private HashMap<Long, Long> vidToUse = new HashMap<Long, Long>();
	
	@Override
	public long getAndIncNextVid(){
		return nextVid++;
	}
	
	@Override
	protected boolean vidToUseContainsKey(long vid){
		return vidToUse.containsKey(vid);
	}
	
	@Override
	protected Long vidToUseGet(long vid){
		return vidToUse.get(vid);
	}
	
	@Override
	protected Long vidToUseRemove(long vid){
		return vidToUse.remove(vid);
	}
	
	@Override
	protected Long vidToUsePut(long vid, long count){
		return vidToUse.put(vid, count);
	}
	
	@Override
	protected Set<Long> vidToUseKeySet(){
		return vidToUse.keySet();
	}
	
	
	@Override
	protected Long classToVidRemove(Class<?> key){
		return classToVid.remove(key);
	}
	
	@Override
	protected boolean classToVidContainsKey(Class<?> key){
		return classToVid.containsKey(key);
	}
	
	@Override
	protected Long classToVidGet(Class<?> key){
		return classToVid.get(key);
	}
	
	@Override
	protected Long classToVidPut(Class<?> key, long vid){
		return classToVid.put(key, vid);
	}
	
	@Override
	protected boolean vidToClassAndMetaContainsKey(long key){
		return vidToClassAndMeta.containsKey(key);
	}
	
	@Override
	protected ClassAndMeta vidToClassAndMetaRemove(long key){
		return vidToClassAndMeta.remove(key);
	}
	
	@Override
	protected  ClassAndMeta vidToClassAndMetaGet(long key){
		return vidToClassAndMeta.get(key);
	}
	
	@Override
	protected  ClassAndMeta vidToClassAndMetaPut(long vid, ClassAndMeta cls){
		return vidToClassAndMeta.put(vid, cls);
	}
	
	@Override
	protected synchronized void onReplaceClassloader(){
		ConcurrentHashMap<Long, ClassAndMeta> newvidToClassAndMeta = new ConcurrentHashMap<Long, ClassAndMeta>();
		
		for(long vid : this.vidToClassAndMeta.keySet()){
			ClassAndMeta cam = this.vidToClassAndMeta.get(vid);
			
			try {
				Class<?> oldversion = cam.objClass;
				Class<?> newVersionOfClass = this.getClassloader().loadClass(oldversion.getName());
				
				ClassAndMeta newcam = new ClassAndMeta(newVersionOfClass, cam.meta);
				newvidToClassAndMeta.put(vid, newcam);
			} catch (ClassNotFoundException e) {
			}
		}
		
		this.vidToClassAndMeta = newvidToClassAndMeta;
		this.decoder.onReplaceClassloader();
		//this.encoder.onReplaceClassloader();
	}
	
}
