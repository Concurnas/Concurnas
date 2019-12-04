package com.concurnas.lang.offheap.storage;
/*
 * Copyright 2015 Odnoklassniki Ltd, Mail.Ru Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Modifications copyright (C) 2017 Concurnas
 */

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.concurnas.lang.Uninterruptible;
import com.concurnas.lang.offheap.VidConstants;
import com.concurnas.lang.offheap.storage.Utils.SingleBufferProvider;
import com.concurnas.lang.offheap.util.OffHeapRWLock;
import com.concurnas.lang.util.LRUCache;
import com.concurnas.runtime.Pair;

//TODO: some of the operations can be optimized by first acquring a read lock then upgrading to a write lock if appropriate
/*
 * Map Entry format: nexPnter[long] | keyhash[int] | keySize[int] | key[...] | pnterToValue ....
 * TODO: reorg above so that varsize elements are at end
 */
@Uninterruptible
public abstract class OffHeapMap<K, V> extends OffHeap implements Map<K,V>, IndexRegionManager {

    protected static final int CONCURRENCY_LEVEL = 65536;  // 2^16, quite big  must be power of 2
    protected static final int META_CONCURRENCY_LEVEL = 16384;  // 2^14, smaller

    //protected long lockWaitTime = 10;
    ///////

    protected OffHeapRWLock[] mainLocks;// = createLocks();
    protected OffHeapRWLock[] metalocks;// = createLocks();
    protected OffHeapRWLock metaNextVidLock;
	

	private Boolean preallocate;
	private Boolean cleanOnStart = false;
	private Boolean removeOnClose = false;	
	
	//private SingleBufferProvider keyEncoderByteBuffer;
	//private SingleBufferProvider keyEncoderByteBufferPersi;
	private SingleBufferProvider vidToClassKeyEncoderByteBuffer;
	private SingleBufferProvider classToVidKeyEncoderByteBuffer;
	//private OffHeapEncoder<K> keyEncoder;
	private OffHeapEncoder<Long> vidToClassMetakeyEncoder;
	private OffHeapEncoder<String> classToVidMetakeyEncoder;
	private OffHeapEncoder<Object> valueEncoder;
	private OffHeapEncoder<Object> valueEncoderMetaString;
	private OffHeapEncoder<Object> valueEncoderMetaLong;
	private OffHeapDecoder<K> keyDecoder;
	private OffHeapDecoder<V> valueDecoder;
	private OffHeapDecoder<String> valueDecoderVidToClass;
	private OffHeapDecoder<Long> valueDecoderClasstoVid;

	private static long MIN_CAPACITY = 1024*1024*2;

	private boolean useFairLockingStrategy=false;
	
	private long count=0;

	private final static long fileSignature = 0x436f4f484d6170l;//CoOHMap

	public OffHeapMap(long bytesize ) {
		setCapacity(bytesize);
	}
	public OffHeapMap( ) {
	}
	
	private static class ClassAndMetaHelper{
		/*private String classa;
		private String[] meta;

		public ClassAndMetaHelper(String classa, String[] meta){
			this.classa = classa;
			this.meta = meta;
		}
		
		public ClassAndMeta toClassAndMeta(ClassLoader clsLoader) throws ClassNotFoundException{
			return new ClassAndMeta(clsLoader.loadClass(this.classa), this.meta);
		}
		
		public static ClassAndMetaHelper toHelper(ClassAndMeta from){
			return new ClassAndMetaHelper(from.objClass.getName(), from.meta);
		}*/
		
		public static ClassAndMeta toClassAndMeta(String from, ClassLoader clsLoader) throws ClassNotFoundException{
			String[] items = from.split(",");
			
			String[] meta;
			if(items[1] == null){
				meta=null;
			}else{
				meta = new String[items.length-1];
				System.arraycopy(items, 1, meta, 0, items.length-1);
			}
			/*System.err.println("src: " + Arrays.toString(items));
			System.err.println("first: " + items[0]);
			System.err.println("meta: " + Arrays.toString(meta));
			*/
			return new ClassAndMeta(clsLoader.loadClass(items[0]) , meta);
		}
		
		public static String toHelper(ClassAndMeta from){
			StringBuilder sbn = new StringBuilder(from.objClass.getName());
			sbn.append(',');
			
			if(null == from.meta){
				sbn.append("null");
			}else{
				int lenz = from.meta.length;
				for(int n=0; n < lenz; n++){
					sbn.append(from.meta[n]);
					if(n != lenz-1){
						sbn.append(',');
					}
				}
			}
			/*

			System.err.println("zz first: " + from.objClass);
			System.err.println("zz meta: " + Arrays.toString(from.meta));
			System.err.println("zz to: " + sbn.toString());*/
			
			return sbn.toString();
		}
		
		
	}
	
	@Override
	protected long getFileSignature(){
		return fileSignature;
	}
	
	public void setCapacity(final long capacity){
		if(capacity < MIN_CAPACITY){
			String storeName= super.getStoreName();
			throw new IllegalArgumentException(String.format("Invalid bytesize specified for off heap store%s: %s, min size: %s bytes", storeName==null?"":": " + storeName, capacity, MIN_CAPACITY));
		}
		
		super.setCapacity(capacity);
	}
	
	@Override
	protected void prepare(){
		//keyEncoderByteBuffer = new SingleBufferProvider();
		///keyEncoderByteBufferPersi = new SingleBufferProvider();
		vidToClassKeyEncoderByteBuffer = new SingleBufferProvider();
		classToVidKeyEncoderByteBuffer = new SingleBufferProvider();
		
		//keyEncoder = new OffHeapEncoder<K>(keyEncoderByteBuffer, this);
		//keyEncoderPersi = new OffHeapEncoder<K>(keyEncoderByteBufferPersi, this);
		vidToClassMetakeyEncoder = new OffHeapEncoder<Long>(vidToClassKeyEncoderByteBuffer, this);
		classToVidMetakeyEncoder = new OffHeapEncoder<String>(classToVidKeyEncoderByteBuffer, this);
		keyDecoder = new OffHeapDecoder<K>(offHeapStorage, this);
		valueEncoder = new OffHeapEncoder<Object>(offHeapStorage, this);
		valueEncoderMetaString = new OffHeapEncoder<Object>(offHeapStorage, this);
		valueEncoderMetaLong = new OffHeapEncoder<Object>(offHeapStorage, this);
		valueDecoder = new OffHeapDecoder<V>(offHeapStorage, this);
		valueDecoderVidToClass = new OffHeapDecoder<String>(offHeapStorage, this);
		valueDecoderClasstoVid = new OffHeapDecoder<Long>(offHeapStorage, this);
		
		/*if(this.isDisk()){
			if(null == path){
				try {
					path = File.createTempFile("offHeapDisk." + super.uid + ".",".tmp");
				} catch (IOException e) {
					throw new IllegalArgumentException("Cannot create temp file for off heap disk store: " + e.getMessage(), e);
				}
			}
			super.offHeapStorage.setPath(path);
		}*/
		
		if(preallocate != null){
			super.offHeapStorage.setPreallocate(preallocate);
		}
		
		if(cleanOnStart != null){
			super.offHeapStorage.setCleanOnStart(cleanOnStart);
		}
		
		if(removeOnClose != null){
			super.offHeapStorage.setCleanOnClose(removeOnClose);
		}
		
		if(defragOnDemand != null){
			offHeapStorage.setDefragOnDemand(defragOnDemand);
		}
		
		super.offHeapStorage.setIndexRegionManager(this);
	}
	
	
	
	@Override
	public void close(){
		super.close();
		//TODO: close the meta store
	}

	/**
	 *  
	 * Warning: slow operation
	 * @return last last byte offset ('high water mark') where data was written
	 */
	public long defrag(){
		return this.offHeapStorage.defrag();
	}
	
	
	public void setPreallocate(boolean preallocate) {
		this.preallocate = preallocate;
	}
	
	public Boolean getPreallocate() {
		return super.offHeapStorage != null?super.offHeapStorage.getPreallocate():preallocate;
	}
	
	public void setCleanOnStart(boolean cleanOnStart){
		this.cleanOnStart=cleanOnStart;
	}
	
	
	public Boolean getCleanOnStart() {
		return super.offHeapStorage != null?super.offHeapStorage.getCleanOnStart():cleanOnStart;
	}
	
	public void setRemoveOnClose(boolean removeOnClose){
		this.removeOnClose=removeOnClose;
	}
	
	public boolean getRemoveOnClose() {
		return this.removeOnClose;
	}
	
	public Boolean getCleanOnClose() {
		return super.offHeapStorage != null?super.offHeapStorage.getRemoveOnClose():removeOnClose;
	}
	
	
	public boolean getUseFairLockingStrategy(){
		if(this.offHeapStorage.isStarted()){
			//obtain from here
			this.useFairLockingStrategy = false;
		}
		return this.useFairLockingStrategy;
	}
	
	/**
	 * defaults to false
	 * @param useFairLockingStrategy
	 */
	public void setUseFairLockingStrategy(boolean useFairLockingStrategy){
		this.useFairLockingStrategy = useFairLockingStrategy;
	}
	
	
	//compaction is permitted but we cannot move around the reserved map
	
	///////business end....
	
	
	
    private void createLocks() {
    	OffHeapRWLock[] mainLocks = new OffHeapRWLock[CONCURRENCY_LEVEL];
        for (int i = 0; i < CONCURRENCY_LEVEL; i++) {
            mainLocks[i] = new OffHeapRWLock(useFairLockingStrategy);
        }
        this.mainLocks = mainLocks;
        
        OffHeapRWLock[] metalocks = new OffHeapRWLock[META_CONCURRENCY_LEVEL];
        for (int i = 0; i < META_CONCURRENCY_LEVEL; i++) {
        	metalocks[i] = new OffHeapRWLock(useFairLockingStrategy);
        }
        this.metalocks = metalocks;//one set of locks for all, should be ok, doesnt get written to a lot
        this.metaNextVidLock = new OffHeapRWLock(useFairLockingStrategy);
    }

    @Override
    public V get(Object key) {
    	return getOrDefault(key, null);
    }

    @SuppressWarnings("unchecked")
	@Override
	public V getOrDefault(Object key, V defaultValue) {
		return (V)getOrDefault(key, defaultValue, this.mainMapEntriesBase, CONCURRENCY_LEVEL, valueDecoder);
	}
	
	@SuppressWarnings("unchecked")
	private <X> Object  getOrDefault(Object key, X defaultValue, long baseAddress, int bucketCount, OffHeapDecoder<X> valueDecoder) {
		SingleBufferProvider keyEncoderByteBuffer = new SingleBufferProvider();
		OffHeapEncoder<K> keyEncoder = new OffHeapEncoder<K>(keyEncoderByteBuffer, this);
		//not very efficient to gennerate a new object here - can this be removed?
		keyEncoder.encode((K)key);
    	byte[] keyAsBinary = keyEncoderByteBuffer.getLastData();
    	
        int hashCode = hashCode(keyAsBinary);
        long currentPtr = bucketFor(hashCode, baseAddress, bucketCount);

        OffHeapRWLock lock = lockFor(hashCode, bucketCount==META_CONCURRENCY_LEVEL).lockRead();
        try {
        	long nextEntry = offHeapStorage.getLong(currentPtr);
        	if(nextEntry != 0){
        		currentPtr = nextEntry;
        	}
        	
        	while(nextEntry != 0){
        		//nexPnter[long] | keyhash[int] | keySize | key | pnterToValue ....
        		nextEntry = offHeapStorage.getLong(currentPtr);
        		int keyHash = offHeapStorage.getInt(currentPtr+8);
        		if(keyHash == hashCode){
        			int keySize = offHeapStorage.getInt(currentPtr+8+4);
            		if(keyAsBinary.length == keySize && compareKey(currentPtr+8+4+4, keySize, keyAsBinary)){//TODO: replace with calls to buffer directly
            			//compare hash and value of bytes in entry
                		long pointerToValue = offHeapStorage.getLong(currentPtr+8+4+4+keySize);
                		//System.err.println("get key: " + key + " address: " + pointerToValue);
                		return valueDecoder.get(pointerToValue);
            		}
        		}
        		currentPtr = nextEntry;
        	}
        } finally {
            lock.unlockRead();
        }

        return defaultValue;
	}

    @Override
	public Set<K> keySet(){
		Set<K> ret = new HashSet<K>();
		for(int n=0; n < CONCURRENCY_LEVEL; n++){
			OffHeapRWLock lock = mainLocks[n];
			try{
				lock.lockRead();
				long currentPtr = mainMapEntriesBase + (n*8);
				
	        	long nextEntry = offHeapStorage.getLong(currentPtr);
	        	
	        	if(nextEntry != 0){
	        		currentPtr = nextEntry;
	        	}
	        	
	        	while(nextEntry != 0){
	        		nextEntry = offHeapStorage.getLong(currentPtr);
	        		//nexPnter[long] | keyhash[int] | keySize | key | pnterToValue ....
	        		//System.err.println("keySet key loc: " + (currentPtr+8+4+4) );
	        		K va = this.keyDecoder.get(currentPtr+8+4+4);
	        		//System.err.println("keySet key: " + va +" loc: " + (currentPtr+8+4+4) );
	        		ret.add(va);
	        		currentPtr = nextEntry;
	        	}
				
			}
			finally{
				lock.unlockRead();
			}
		}
		
		return ret;
	}
    
    @SuppressWarnings("unchecked")
	@Override
	public V put(K key, V value) {
    	return (V)put(key, value, true, false, false, null);
    }
    
	@SuppressWarnings("unchecked")
	@Override
	public V putIfAbsent(K key, V value) {
    	return (V)put(key, value, false, false, false, null);
	}
    
	private Object put(K key, V value, boolean overwrite, boolean replace, boolean checkOldVaue, V oldValue) {
		SingleBufferProvider keyEncoderByteBuffer = new SingleBufferProvider();
		OffHeapEncoder<K> keyEncoder = new OffHeapEncoder<K>(keyEncoderByteBuffer, this);
		return put(key, value, overwrite, replace, checkOldVaue, oldValue, this.mainMapEntriesBase, CONCURRENCY_LEVEL, keyEncoder, keyEncoderByteBuffer, this.valueEncoder);
	}
	
	private  <X> Object put(X key, Object value, boolean overwrite, boolean replace, boolean checkOldVaue, V oldValue, long baseAddress, int bucketCount, OffHeapEncoder<X> keyEncoder, SingleBufferProvider keyBinBuffer, OffHeapEncoder<Object> valueEncoder) {
    	//byte[] the key
		keyEncoder.encode(key);
    	byte[] keyAsBinary = keyBinBuffer.getLastData();
    	//System.err.println("put key: " + key + " bin: " + keyAsBinary[0]);
    	
        int hashCode = hashCode(keyAsBinary);
        long startPnter = bucketFor(hashCode, baseAddress, bucketCount);
        long currentPtr = startPnter; 
        //int newSize = sizeOf(value);
        
        OffHeapRWLock lock = lockFor(hashCode, bucketCount==META_CONCURRENCY_LEVEL).lockWrite();
        
        V previous = null;
        
        try {
        	long nextEntry = offHeapStorage.getLong(currentPtr);
        	long firstnextEntry = nextEntry;
        	
        	if(nextEntry != 0){
        		currentPtr = nextEntry;
        	}
        	
        	while(nextEntry != 0){
        		//nexPnter[long] | keyhash[int] | keySize | key | pnterToValue ....
        		nextEntry = offHeapStorage.getLong(currentPtr);
        		int keyHash = offHeapStorage.getInt(currentPtr+8);
        		if(keyHash == hashCode){
        			int keySize = offHeapStorage.getInt(currentPtr+8+4);
            		if(keyAsBinary.length == keySize && compareKey(currentPtr+8+4+4, keySize, keyAsBinary)){
            			//replace existing entry!
                		long pointerToValue = offHeapStorage.getLong(currentPtr+8+4+4+keySize);
                		previous =  this.valueDecoder.get(pointerToValue);
                		if(!overwrite){
            				return previous;
            			}
                		
                		if(checkOldVaue && !Objects.equals(oldValue, previous)){
                			return null;
                		}
                		
                		offHeapStorage.free(pointerToValue);
                		long newValueAdress = valueEncoder.encode(value);
                		offHeapStorage.putLong(currentPtr+8+4+4+keySize, newValueAdress);
                		
                		//System.err.println(String.format("put: key: %s value size: %s value location: %s - %s",  key, this.valueEncoder.lastEncodedSize, newValueAdress, this.valueEncoder.lastEncodedSize + newValueAdress));
                		//System.err.println("put key: %s" + key + "address: " + pointerToValue);
                		
                		return checkOldVaue?this:previous;
            		}
        		}
        		currentPtr = nextEntry;
        	}
        	
        	if(!replace){//if we must replace the item then we've failed as it was not present already
        		//not found above so create new entry
        		long newValueAdress = valueEncoder.encode(value);
            	
        		//nexPnter[long] | keyhash[int] | keySize | key | pnterToValue ....
        		int entrySize = 8+4+4+(keyAsBinary.length)+8;
        		//replace 'first' entry in list with above
        		Pair<ByteBuffer, Long> bandloc = offHeapStorage.malloc(entrySize);
        		ByteBuffer buffer = bandloc.getA();
        		long mallocAddress = bandloc.getB();
        		//put entry
        		buffer.putLong(firstnextEntry);
        		buffer.putInt(hashCode);
        		buffer.putInt(keyAsBinary.length);
        		buffer.put(keyAsBinary);
        		buffer.putLong(newValueAdress);
        		//link
        		offHeapStorage.putLong(startPnter, mallocAddress);
        		//System.err.println("put key: " + key + " address: " + newValueAdress);
        		//System.err.println(String.format("put: key: %s value size: %s value location: %s - %s value adress: %s",  key, this.valueEncoder.lastEncodedSize, newValueAdress, this.valueEncoder.lastEncodedSize + newValueAdress, mallocAddress));
        	}
        	
        } finally {
            lock.unlockWrite();
        }
        count++;
        return null;
    }
	
	
	@SuppressWarnings("unchecked")
	@Override
	public V replace(K key, V value) {
		return (V)put(key, value, true, true, false, null);
	}
	
	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		return put(key, newValue, true, true, true, oldValue)==this;
	}
	

    
    @Override
	public boolean remove(Object key, Object value) {
		return remove(key, value, true) == this;//match value varient returns object guranteed not to be null to signify true
	}
    
    
    @SuppressWarnings("unchecked")
	@Override
	public V remove(Object key) {
    	return (V) remove(key, null, false);
    }
    	
	private Object remove(Object key, Object value, boolean matchValue) {
    	return remove(key, value, matchValue, this.mainMapEntriesBase, CONCURRENCY_LEVEL);
    }
    	
	@SuppressWarnings("unchecked")
	private Object remove(Object key, Object value, boolean matchValue, long baseAddress, int bucketCount) {
    	//byte[] the key
		SingleBufferProvider keyEncoderByteBuffer = new SingleBufferProvider();
		OffHeapEncoder<K> keyEncoder = new OffHeapEncoder<K>(keyEncoderByteBuffer, this);
		
		keyEncoder.encode((K)key);
    	byte[] keyAsBinary = keyEncoderByteBuffer.getLastData();
		
    	/*this.keyEncoder.encode((K)key);
    	byte[] keyAsBinary = this.keyEncoderByteBuffer.getLastData();*/
	 
        int hashCode = hashCode(keyAsBinary);
        long currentPtr = bucketFor(hashCode, baseAddress, bucketCount);

        OffHeapRWLock lock = lockFor(hashCode, bucketCount==META_CONCURRENCY_LEVEL).lockWrite();
        try {
        	//nexPnter[long] | keyhash[int] | keySize | key | pnterToValue ....
        	long nextEntry = offHeapStorage.getLong(currentPtr);
        	long prevAddress = currentPtr;
        	
        	if(nextEntry != 0){
        		currentPtr = nextEntry;
        	}
        	
        	while(nextEntry != 0){
        		nextEntry = offHeapStorage.getLong(currentPtr);
        		
        		int keyHash = offHeapStorage.getInt(currentPtr+8);
        		if(keyHash == hashCode){
        			int keySize = offHeapStorage.getInt(currentPtr+8+4);
            		if(keyAsBinary.length == keySize && compareKey(currentPtr+8+4+4, keySize, keyAsBinary)){
            			//replace existing entry!
                		long pointerToValue = offHeapStorage.getLong(currentPtr+8+4+4+keySize);
                		V previous =  this.valueDecoder.get(pointerToValue);
                		if(matchValue){
                			if(!Objects.equals(value, previous)){
                				return null;
                			}
                		}
                		
                		HashMap<Long, Long> vidsToCntToDelete = valueDecoder.getVids(pointerToValue);
                		//remove value
                		offHeapStorage.free(pointerToValue);
                		
                		for(Long vid : vidsToCntToDelete.keySet()){
                			unregisterVidUse(vid, vidsToCntToDelete.get(vid));
                		}
                		
                		//remove this entry
                		offHeapStorage.free(currentPtr);
                		//point to next entry
                		offHeapStorage.putLong(prevAddress, nextEntry);
                		count--;
                		
                		//System.err.println(String.format("remove: key: %s value size: %s value location: %s,  key location: %s",  key, 0, pointerToValue, currentPtr));
                    	
                		
                		return matchValue?this:previous;
            		}
        		}
        		prevAddress = currentPtr;
        		currentPtr = nextEntry;
        	}
        } finally {
            lock.unlockWrite();
        }

        return null;
    }
	 
	@SuppressWarnings("unchecked")
	private boolean containsKey(Object key, long baseAddress, int bucketCount){
    	//System.err.println("what key: " + key);
    	SingleBufferProvider keyEncoderByteBuffer = new SingleBufferProvider();
		OffHeapEncoder<K> keyEncoder = new OffHeapEncoder<K>(keyEncoderByteBuffer, this);
		
		keyEncoder.encode((K)key);
    	byte[] keyAsBinary = keyEncoderByteBuffer.getLastData();
    	
    	/*this.keyEncoderPersi.encode((K)key);
    	byte[] keyAsBinary = this.keyEncoderByteBufferPersi.getLastData();*/
    	//System.err.println("cont key: " + key + " bin: " + keyAsBinary[0]);
        int hashCode = hashCode(keyAsBinary);
        long currentPtr = bucketFor(hashCode, baseAddress, bucketCount);

        OffHeapRWLock lock = lockFor(hashCode, bucketCount==META_CONCURRENCY_LEVEL).lockRead();
        try {
        	long nextEntry = offHeapStorage.getLong(currentPtr);
        	
        	if(nextEntry != 0){
        		currentPtr = nextEntry;
        	}
        	
        	while(nextEntry != 0){
        		//nexPnter[long] | keyhash[int] | keySize | key | pnterToValue ....
        		nextEntry = offHeapStorage.getLong(currentPtr);
        		int keyHash = offHeapStorage.getInt(currentPtr+8);
        		if(keyHash == hashCode){
        			int keySize = offHeapStorage.getInt(currentPtr+8+4);
            		if(keyAsBinary.length == keySize && compareKey(currentPtr+8+4+4, keySize, keyAsBinary)){//TODO: replace with calls to buffer directly
            			//compare hash and value of bytes in entry
                		return true;
            		}
        		}
        		currentPtr = nextEntry;
        	}
        } finally {
            lock.unlockRead();
        }

        return false;
	}
	

	@Override
	public boolean containsKey(Object key){
		return containsKey(key, this.mainMapEntriesBase, CONCURRENCY_LEVEL);
	}
	
	

    @Override
	public void clear(){
		for(int n=0; n < CONCURRENCY_LEVEL; n++){
			OffHeapRWLock lock = mainLocks[n];
			try{
				lock.lockWrite();
				long currentPtr = mainMapEntriesBase + (n*8);
				
	        	long nextEntry = offHeapStorage.getLong(currentPtr);
	        	
	        	if(nextEntry != 0){
	        		currentPtr = nextEntry;
	        	}
	        	
	        	while(nextEntry != 0){
	        		nextEntry = offHeapStorage.getLong(currentPtr);
	        		//nexPnter[long] | keyhash[int] | keySize | key | pnterToValue ....
	        		int keySize = offHeapStorage.getInt(currentPtr+8+4);
        			long pointerToValue = offHeapStorage.getLong(currentPtr+8+4+4+keySize);
	        		offHeapStorage.free(pointerToValue);
            		//remove this entry
            		offHeapStorage.free(currentPtr);
            		
	        		currentPtr = nextEntry;
	        	}
	        	offHeapStorage.putLong(mainMapEntriesBase + (n*8), 0);//reset to zero;
				
			}
			finally{
				lock.unlockWrite();
			}
		}
		count = 0;
	}

    @Override
	public Set<V> values(){
		Set<V> ret = new HashSet<V>();
		for(int n=0; n < CONCURRENCY_LEVEL; n++){
			OffHeapRWLock lock = mainLocks[n];
			try{
				lock.lockRead();
				long currentPtr = mainMapEntriesBase + (n*8);
				
				long nextEntry = offHeapStorage.getLong(currentPtr);
				
				if(nextEntry != 0){
					currentPtr = nextEntry;
				}
				
				while(nextEntry != 0){
					nextEntry = offHeapStorage.getLong(currentPtr);
					//nexPnter[long] | keyhash[int] | keySize | key | pnterToValue ....
					int keySize = offHeapStorage.getInt(currentPtr+8+4);
					long pointerToValue = offHeapStorage.getLong(currentPtr+8+4+4+keySize);
					ret.add(this.valueDecoder.get(pointerToValue));
					currentPtr = nextEntry;
				}
				
			}
			finally{
				lock.unlockRead();
			}
		}
		
		return ret;
	}

    @Override
	public void putAll(Map<? extends K,? extends V> toAdd){
		for(K key : toAdd.keySet()){
			this.put(key, toAdd.get(key));
		}
	}
    
	public int size(){
		throw new UnsupportedOperationException("size not supported, call sizeLong for entry count (as a long)");
	}
	
	public long sizeLong(){
		return count;
	}
	
	public boolean isEmpty(){
		return 0 == sizeLong();
	}

	
    private boolean compareKey(long startPos, int keySize, byte[] keyAsBinary) {
    	for(int n=0; n < keySize; n++){
    		if(offHeapStorage.get(startPos + n) != keyAsBinary[n]){//TODO: ugly, quite slow pulling out each byte by byte
    			return false;
    		}
    	}
    	return true;
	}
	
    protected long bucketFor(int hashCode, long baseAddress, int bucketCount) {
        //return mainMapEntriesBase + (hashCode & Integer.MAX_VALUE) % CONCURRENCY_LEVEL * 8;
        return baseAddress + (hashCode & Integer.MAX_VALUE) % bucketCount * 8;
    }

    protected OffHeapRWLock lockFor(long hashCode, boolean isMeta) {
        return isMeta? metalocks[(int) hashCode & (META_CONCURRENCY_LEVEL - 1)] : mainLocks[(int) hashCode & (CONCURRENCY_LEVEL - 1)];
    }
    
    
    private int hashCode(byte[] key) {
        int h=0;//null and empty resolve to null
        if (key != null && key.length > 0) {
            for (int i = 0; i < key.length; i++) {
                h = 31 * h + key[i];
            }
        }
        //System.err.println(String.format("hc %s -> %s", Stringifier.stringify(key), h));
        return h;
    }
	
	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException("containsValue not supported");
	}
	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException("entrySet not supported");
	}
	@Override
	public void forEach(BiConsumer<? super K, ? super V> action) {
		throw new UnsupportedOperationException("forEach not supported");
	}
	@Override
	public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
		throw new UnsupportedOperationException("replaceAll not supported");
	}
	@Override
	public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
		throw new UnsupportedOperationException("computeIfAbsent not supported");
	}
	@Override
	public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		throw new UnsupportedOperationException("computeIfPresent not supported");
	}
	@Override
	public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		throw new UnsupportedOperationException("compute not supported");
	}
	@Override
	public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		throw new UnsupportedOperationException("merge not supported");
	}
	
	private Boolean defragOnDemand;
	
	/**
	 * Warning: slow operation
	 * @param defragOnDemand
	 */
	public void setDefragOnDemand(boolean defragOnDemand) {
		this.defragOnDemand = defragOnDemand;
		if(null != offHeapStorage){
			offHeapStorage.setDefragOnDemand(defragOnDemand);
		}
	}
	
	public boolean getDefragOnDemand(){
    	return this.offHeapStorage != null ? offHeapStorage.getDefragOnDemand():this.defragOnDemand;
    }
	
	///meta data and other settings...

	private long getProtectedRegionAddress(){
		return offHeapStorage.getProtectedRegion();
	}
	

    private long meta_nextvid_address;
    private long mainMapEntriesBase;
    private long metaMap_classToVid;
    private long metaMap_vidtoClass;
    //private long metaMap_vidToCount;
	
    
	/*
	 * protected region: 
	 * nextvid 					 -> 8
	 * mainBuckets 				 -> 2^16 = 65536
	 * classToVid 				 -> 2^14 = 16384
	 * vidtoClass 				 -> 2^14 = 16384
	 * vidToCount 				 -> 2^14 = 16384
	 */
	
	protected void postStart(){
		//mapBase = offHeapStorage.getLong(OffHeapMalloc.SIGNATURE);//
		createLocks();
		long protectedRegion = getProtectedRegionAddress();
		if(0 == protectedRegion){
			int totalprotectedsize = 8 + CONCURRENCY_LEVEL*8 + (META_CONCURRENCY_LEVEL*8*3);
			
			Pair<ByteBuffer, Long> basePointerbbl = offHeapStorage.malloc(totalprotectedsize);
			ByteBuffer basePointerbb = basePointerbbl.getA();
			offHeapStorage.setProtectedRegionAndSize(basePointerbb.position(), totalprotectedsize);
			
			long protectedAddress = basePointerbbl.getB();
			
			meta_nextvid_address = protectedAddress;
			mainMapEntriesBase = protectedAddress+8;
		    metaMap_classToVid = protectedAddress+8 + (CONCURRENCY_LEVEL*8);
		    metaMap_vidtoClass = protectedAddress+8 + (CONCURRENCY_LEVEL*8) + (META_CONCURRENCY_LEVEL*8);
		    //metaMap_vidToCount = protectedAddress+8 + (CONCURRENCY_LEVEL*8) + (META_CONCURRENCY_LEVEL*8*2);
			
			basePointerbb.putLong(VidConstants.startVid);//put the nextvid in first
			basePointerbb.put(new byte[(totalprotectedsize-1)/8]);//allocate all in one go, exclude the nextvid
		}else{
			//reload...
			
			//nextVid = this.offHeapStorage.getLong(protectedRegion);
			mainMapEntriesBase = protectedRegion+8;
		    metaMap_classToVid = protectedRegion+8 + (CONCURRENCY_LEVEL*8);
		    metaMap_vidtoClass = protectedRegion+8 + (CONCURRENCY_LEVEL*8) + (META_CONCURRENCY_LEVEL*8);
		    //metaMap_vidToCount = protectedRegion+8 + (CONCURRENCY_LEVEL*8) + (META_CONCURRENCY_LEVEL*8*2);
		    
			//initialize item count and vid count
			long count = 0;
			for(int n=0; n < CONCURRENCY_LEVEL; n++){
				//note: we dont do locking here as concurrent access cannot occur prior to initialization
				long currentPtr = mainMapEntriesBase + (n*8);
	        	long nextEntry = offHeapStorage.getLong(currentPtr);
	        	
	        	if(nextEntry != 0){
	        		currentPtr = nextEntry;
	        	}
	        	
	        	while(nextEntry != 0){
	        		nextEntry = offHeapStorage.getLong(currentPtr);
	        		//nexPnter[long] | keyhash[int] | keySize[int] | key | pnterToValue ....
	        		
	        		int ksize = offHeapStorage.getInt(currentPtr+8+4);
	        		long valueAddress = offHeapStorage.getLong(currentPtr+8+4+4+ksize);
	        		
	        		HashMap<Long, Long> vidToCountUsed = this.valueDecoder.getVids(valueAddress);
	        		this.registerVidUsage(vidToCountUsed);
	        		
	        		count++;
            		
	        		currentPtr = nextEntry;
	        	}
			}
			this.count=count;
			
			
		}
	}
	
	@Override
	public long[] getRegionMapOffsets() {
		return new long[]{8, 8 + (CONCURRENCY_LEVEL*8), 8 + (CONCURRENCY_LEVEL*8) + (META_CONCURRENCY_LEVEL*8), 8 + (CONCURRENCY_LEVEL*8) + (META_CONCURRENCY_LEVEL*8*2)};
		//return new long[]{mainMapEntriesBase, metaMap_classToVid, metaMap_vidtoClass, metaMap_vidToCount};
	}
	
	@Override
	public int[] getRegionMapSizes(){
		return new int[]{CONCURRENCY_LEVEL, META_CONCURRENCY_LEVEL, META_CONCURRENCY_LEVEL, META_CONCURRENCY_LEVEL};
	}
	
	@Override
	public  OffHeapRWLock[][] getRegionLocks() {
		return new OffHeapRWLock[][]{mainLocks, null, null, null};
	}
		
	
	//cache top 100 classes
	private static final int META_CACHE_SUZE = 100;
	private LRUCache<Long, ClassAndMeta> vidToClassCache = new LRUCache<Long, ClassAndMeta>(META_CACHE_SUZE);
	private LRUCache<Class<?>, Long> classToVidCache = new LRUCache<Class<?>, Long>(META_CACHE_SUZE);
	private HashMap<Long, Long> vidToUse = new HashMap<Long, Long>();
	
	
	@Override
	protected boolean vidToClassAndMetaContainsKey(long key){
		//System.err.println("metaMap_vidtoClass contains: " + metaMap_vidtoClass);
		if(vidToClassCache.containsKey(key)){
			return true;
		}else{//load - and cache?
			return containsKey(key, metaMap_vidtoClass, META_CONCURRENCY_LEVEL);
		}
	}
	
	@Override
	protected  ClassAndMeta vidToClassAndMetaGet(long key){
		//System.err.println("metaMap_vidtoClass get: " + metaMap_vidtoClass);
		if(vidToClassCache.containsKey(key)){
			return vidToClassCache.get(key);
		}else{//load and cache
			String value = (String)getOrDefault(key, null, this.metaMap_vidtoClass, META_CONCURRENCY_LEVEL, valueDecoderVidToClass);
			ClassAndMeta ret;
			try {
				ret = ClassAndMetaHelper.toClassAndMeta(value, this.getClassloader());
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
			//System.err.println("key: " + key + "want: " + value);
			//Class<?> clsvalue = Class.forName(value, true, super.getClassloader());
			vidToClassCache.put(key, ret/*clsvalue*/);
			return ret;
		}
	}
	
	@Override
	protected ClassAndMeta vidToClassAndMetaRemove(long key){
		vidToClassCache.remove(key);
		String clsName = (String) remove(key, null, false, this.metaMap_vidtoClass, META_CONCURRENCY_LEVEL);
		//System.err.println(String.format("vidToClassRemove: %s -> %s", key, clsName));
		try {
			return ClassAndMetaHelper.toClassAndMeta(clsName, this.getClassloader());//clsName.toClassAndMeta(this.getClassloader());//Class.forName(clsName, true, super.getClassloader());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected  ClassAndMeta vidToClassAndMetaPut(long key, ClassAndMeta cam){
		vidToClassCache.put(key, cam);
		//System.err.println("metaMap_vidtoClass put: " + metaMap_vidtoClass);
		
		String ret = (String)put((Long)key, ClassAndMetaHelper.toHelper(cam), true, false, false, null, this.metaMap_vidtoClass, META_CONCURRENCY_LEVEL, vidToClassMetakeyEncoder, vidToClassKeyEncoderByteBuffer, valueEncoderMetaString);
		
		//System.err.println("vidToClassPut: key: " + key);
		
		if(ret != null){
			try{
				//return Class.forName(ret, true, super.getClassloader());
				return ClassAndMetaHelper.toClassAndMeta(ret, this.getClassloader());//ret.toClassAndMeta(this.getClassloader());
			}
			catch(ClassNotFoundException excep){
				throw new RuntimeException(excep);
			}
		}
		return null;
	}
	
	@Override
	public long getAndIncNextVid(){
		try{
			this.metaNextVidLock.lockWrite();

			long ret = this.offHeapStorage.getLong(meta_nextvid_address);
			this.offHeapStorage.putLong(meta_nextvid_address, ret+1);
			return ret;
		}
		finally{
			this.metaNextVidLock.unlockWrite();
		}
	}
	
	
	
	@Override
	protected boolean classToVidContainsKey(Class<?> key){
		if(classToVidCache.containsKey(key)){
			return true;
		}else{//load - and cache?
			return containsKey(key.getName(), this.metaMap_classToVid, META_CONCURRENCY_LEVEL);
		}
	}
	
	@Override
	protected Long classToVidRemove(Class<?> key){
		classToVidCache.remove(key);
		return (Long)remove(key.getName(), null, false, this.metaMap_classToVid, META_CONCURRENCY_LEVEL); 
	}
	
	
	
	@Override
	protected Long classToVidGet(Class<?> key){
		if(classToVidCache.containsKey(key)){
			return classToVidCache.get(key);
		}else{//load and cache
			Long value = (Long)getOrDefault(key.getName(), null, this.metaMap_classToVid, META_CONCURRENCY_LEVEL, valueDecoderClasstoVid);
			classToVidCache.put(key, value);
			return value;
		}
	}
	
	@Override
	protected Long classToVidPut(Class<?> key, long vid){
		classToVidCache.put(key, vid);
		Long ret = (Long)put(key.getName(), vid, true, false, false, null, this.metaMap_classToVid, META_CONCURRENCY_LEVEL, classToVidMetakeyEncoder, classToVidKeyEncoderByteBuffer, valueEncoderMetaLong);
		return ret;
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
	protected void onReplaceClassloader() {
		
		LRUCache<Long, ClassAndMeta> vidToClassCacheNew = new LRUCache<Long, ClassAndMeta>(META_CACHE_SUZE);		
		
		for(long vid : this.vidToClassCache.keySet()){
			ClassAndMeta cam = this.vidToClassCache.get(vid);
			
			try {
				Class<?> oldversion = cam.objClass;
				Class<?> newVersionOfClass = this.getClassloader().loadClass(oldversion.getName());
				
				ClassAndMeta newcam = new ClassAndMeta(newVersionOfClass, cam.meta);
				vidToClassCacheNew.put(vid, newcam);
			} catch (ClassNotFoundException e) {
			}
		}
		
		this.vidToClassCache = vidToClassCacheNew;
		
		this.keyDecoder.onReplaceClassloader();
		this.valueDecoder.onReplaceClassloader();
		this.valueDecoderVidToClass.onReplaceClassloader();
		this.valueDecoderClasstoVid.onReplaceClassloader();
	}	
}
