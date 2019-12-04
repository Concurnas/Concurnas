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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.ArrayList;

import com.concurnas.bootstrap.runtime.cps.Fiber;
import com.concurnas.lang.Uninterruptible;
import com.concurnas.lang.offheap.InsufficientFreeSpace;
import com.concurnas.lang.offheap.OffHeapOutOfMemoryError;
import com.concurnas.lang.offheap.util.OffHeapRWLock;
import com.concurnas.runtime.Pair;


@Uninterruptible
public class OffHeapMalloc implements MallocProvider, AutoCloseable{

	private static final int VERSION_OFFSET 		= 0;//easier to read
	private static final int SIGNATURE_OFFSET 		= 8;//easier to read
	private static final int CAPACITY_OFFSET  		= 16;
	private static final int PROTECTED_REGION_START     = 24;//8
	private static final int PROTECTED_REGION_SIZE      = 32;//int - 4 bytes
	private static final int BASE_OFFSET    		= 36;
	
	private static final long FILE_VERSION    		= 1l;
	
	
	private static final int MAX_OBJ_SIZE     = 1024*1024*1024;//1 gig

	private static final int HEADER_SIZE      = 8;
	private static final int SIZE_OFFSET      = 0;
	private static final int LEFT_OFFSET      = 4;
	private static final int NEXT_OFFSET      = 8;
	private static final int PREV_OFFSET      = 16;

	private static final int BIN_COUNT        = 120;
	private static final int BIN_SIZE         = 8;
	private static final int BIN_SPACE        = BIN_COUNT * BIN_SIZE + 64;

	private static final int MAX_CHUNK        = HEADER_SIZE + 1024 * 1024 * 1024;//1 gig
	private static final int MIN_CHUNK        = HEADER_SIZE + 16;

	private static final int OCCUPIED_MASK    = 0x80000000;
	private static final int FREE_MASK        = 0x7ffffff8;

	private static final int MAX_SCAN_COUNT   = 127;
	
	static final int SEGMENT_SIZE     = 2147483647;

	//statics above
	
    private final int base=0;
    private Long capacity =null;

    private long freeMemory;
    //private OffHeapMalloc next;
    private int mask = OCCUPIED_MASK;
    
    private static long maxCapacity = 9_223_372_036_854_775_807l;//huge
    private static long minCapacity = 128l;//small
    
	private boolean started = false;
  	private ByteBuffer[] buffers;
    private boolean preallocate = false;
	private boolean cleanOnStart;
	private File path;
	private boolean cleanOnClose;
	private ArrayList<Pair<File, FileChannel>> createdFiles;
	private final long fileSignature;

    public OffHeapMalloc(long fileSignature, long capacity) {    
    	this(fileSignature);
    	this.setCapacity(capacity);
    }
    
    public OffHeapMalloc(long fileSignature) {    	
    	this.fileSignature = fileSignature;
    }
    
    private IndexRegionManager indexRegionManager = null;
    void setIndexRegionManager(IndexRegionManager indexRegionManager){
    	this.indexRegionManager = indexRegionManager;
    }
    
    public void setCapacity(long capacity){
    	if(capacity > maxCapacity || capacity < minCapacity){
    		throw new IllegalArgumentException(String.format("heap space allocation (of %s) must be between %s and %s", capacity, minCapacity, maxCapacity));
    	}
    	capacity = capacity & ~7;
    	if(this.capacity != null && this.started){//adjustment required whilst running
    		adjustCapacityWhilstRunning(capacity);
    	}
    	
    	this.capacity = capacity;
    }
    
    public Long getCapacity(){
    	return this.capacity;
    }
    
    public long getFreeSpace(){
    	return this.freeMemory;
    }
    
    private synchronized void reduceCapacity(final long newCap){
    	//entries from index above limit
    	
    	long oldCapacity = this.capacity;
    	
    	if(this.freeMemory < (this.capacity - newCap)){
			throw new InsufficientFreeSpace("Insufficient free space to reduce capacity");
    	}
    	
    	this.capacity = newCap;
    	
    	try{//move elements below mark, move values below mark
    		boolean nonAboveNewCap = compactBelow(newCap);
    		
    		if(!nonAboveNewCap){//stuff above cap, cannot move below, try a defrag
    			if(!defragOnDemand){
    				throw new InsufficientFreeSpace("Insufficient free space to reduce capacity. Defrag not attempted as defragOnDemand is set to false");
    			}
    			else{//oh no, we were unable to move all enties below, now try compaction/defrag
    				
    				try{
        				defragPostLocked();
    				}catch(OffHeapOutOfMemoryError oom){
    					throw new InsufficientFreeSpace("Insufficient free space to reduce capacity");
    				}
    				
    			}
    		}
    	}
    	finally{
    		OffHeapRWLock[][] locksforMaps = indexRegionManager.getRegionLocks();
    		for(OffHeapRWLock[] locks : locksforMaps){
    			if(null != locks){
    				int locklen = locks.length;
            		
            		for(int n=0; n < locklen; n++){
            			locks[n].unlockWrite();
            		}
    			}
    		}
    		this.capacity=oldCapacity;
    	}
    	
    	this.capacity = newCap;
		buffers[0].putLong(base + CAPACITY_OFFSET, capacity);
    }
    

    private long getBufferLong(ByteBuffer[] buffers, long address){
		//System.err.println(String.format("get long address: %s", address));
    	long ret= buffers[(int) (address / SEGMENT_SIZE)].getLong((int) (address % SEGMENT_SIZE));
    	return ret;
    }
    private int getBufferInt(ByteBuffer[] buffers, long address){
    	return buffers[(int) (address / SEGMENT_SIZE)].getInt((int) (address % SEGMENT_SIZE));
    }
    
    private byte[] getBufferBytes(ByteBuffer[] buffers, long address, int size){
    	byte[] dst = new byte[size];
    	ByteBuffer bb = buffers[(int) (address / SEGMENT_SIZE)];
    	bb.position((int) (address % SEGMENT_SIZE));
    	bb.get(dst);
    	return dst;
    }
    
    private long defragPostLocked(){
    	//create new buffers and copy stuff over
    	ArrayList<Pair<File, FileChannel>> oldcreatedFiles = null;

    	ByteBuffer[] oldBuffs = buffers;
    	
    	long oldfreemem = freeMemory;
    	
    	File oldPath = this.path;
    	
    	long highwatermark=0;
    	
    	boolean viaTempDefagFiles = this.path != null;
    	if(viaTempDefagFiles){
    		oldcreatedFiles = new ArrayList<Pair<File, FileChannel>>(this.createdFiles);
    	}
    	
    	try{
    		this.createdFiles = new ArrayList<Pair<File, FileChannel>>();
    		
    		if(viaTempDefagFiles){
    			this.path = new File(path.getAbsolutePath() + "Defrag");
    		}

    		boolean cleanOnStartprev = cleanOnStart;
        	try{//create new stuff. Copy all the dudes over...
        		cleanOnStart=true;
        		createNewBuffers();
        	}catch(IOException e){
        		throw new RuntimeException("Cannot create new off heap space to defrag into: " + e.getMessage(), e);
        	}
        	finally{
        		cleanOnStart = cleanOnStartprev; 
        	}
        	
        	init();

        	final long protectedRegionStart = oldBuffs[0].getLong(base + PROTECTED_REGION_START);
	    	final int size = oldBuffs[0].getInt(base + PROTECTED_REGION_SIZE);
	    	
			Pair<ByteBuffer, Long> basePointerbbl = this.malloc(size);
			ByteBuffer basePointerbb = basePointerbbl.getA();
			long newFirstIfxPos = basePointerbbl.getB();
			if(highwatermark < newFirstIfxPos+size){highwatermark = newFirstIfxPos+size;}
			setProtectedRegionAndSize(newFirstIfxPos, size);
        	
			basePointerbb.position((int)(newFirstIfxPos% SEGMENT_SIZE));
			basePointerbb.put(new byte[size]);//init zeros
			
			long[] regionOffsets = this.indexRegionManager.getRegionMapOffsets();
			int[] mapAddresseSizes = this.indexRegionManager.getRegionMapSizes();
			for(int idx=0; idx < regionOffsets.length; idx++){
				long mapRegionOffset = regionOffsets[idx];
				int ncount = mapAddresseSizes[idx];
				for(int n=0; n < ncount; n++){
	    			long currentPtr =  protectedRegionStart + mapRegionOffset + (n*8);//old and new same
	    			//long qq = currentPtr;
	        		long nextEntry = getBufferLong(oldBuffs, currentPtr);

	            	if(nextEntry != 0){
	            		currentPtr = nextEntry;
	            	}
	            	
	            	while(nextEntry != 0){
	            		//nexPnter[long] | keyhash[int] | keySize | key | pnterToValue ....
	            		nextEntry = getBufferLong(oldBuffs, currentPtr);
	            		
	            		int keyHash = getBufferInt(oldBuffs, currentPtr+8);
	            		int keySize = getBufferInt(oldBuffs, currentPtr+8+4);
	            		byte[] key = getBufferBytes(oldBuffs, currentPtr+8+4+4, keySize);
	            		//copy value...
	        			long pointerToValue = getBufferLong(oldBuffs, currentPtr+8+4+4+keySize);
	        			int valueSize = (getBufferInt(oldBuffs, pointerToValue-HEADER_SIZE) & FREE_MASK ) - HEADER_SIZE;
	        			
	        			byte[] value = getBufferBytes(oldBuffs, pointerToValue, valueSize); 
	        			
	        			Pair<ByteBuffer,Long> newValueLoc = this.malloc(valueSize);
	        			long newValueAddress = newValueLoc.getB();
	        			if(highwatermark < newValueAddress+valueSize){highwatermark = newValueAddress+valueSize;}
	        			ByteBuffer bb = newValueLoc.getA();
	        			bb.position((int) (newValueAddress % SEGMENT_SIZE));
	        			bb.put(value);
	        			
	        			long newHeadOfBucket = newFirstIfxPos + mapRegionOffset + (n*8);
	        			
	        			//copy entry...
	        			Pair<ByteBuffer,Long> newEntryLoc= this.malloc(8+4+4+keySize + 8);
	        			long newvalueAddress = newEntryLoc.getB();
	        			if(highwatermark < newvalueAddress+8+4+4+keySize + 8){highwatermark = newvalueAddress+8+4+4+keySize + 8;}
	        			ByteBuffer newEntryStore = newEntryLoc.getA();
	        			newEntryStore.putLong(this.getLong(newHeadOfBucket));
	        			newEntryStore.putInt(keyHash);
	        			newEntryStore.putInt(keySize);
	        			newEntryStore.put(key);
	        			newEntryStore.putLong(newValueAddress);
	        			
	        			this.putLong(newHeadOfBucket, newvalueAddress);
	            	}
	    		}
			}
        	//copy
    	}catch (Throwable thr){//e..g oom
    		if(viaTempDefagFiles){//remove any created defrag files
    			boolean prevcleanOnClose = cleanOnClose;
				cleanOnClose=true;//cleanup anything created
				try{
					closeBuffersAndFiles();
				}finally{
					cleanOnClose = prevcleanOnClose;
				}
    		}
    		this.createdFiles = oldcreatedFiles;
    		this.buffers = oldBuffs;
    		this.freeMemory = oldfreemem;
    		this.path=oldPath;
    		throw thr;
    	}
    	
		//now: close old, rename old files, rename defrag versions, delete old, done!
		for(ByteBuffer bb : oldBuffs){
			closeDirectBuffer(bb);
		}
		if(viaTempDefagFiles){
			{
				boolean prevcleanOnClose = cleanOnClose;
				cleanOnClose=false;//dont want files deleted on closure here...
				try{
					closeCreatedFiles(oldcreatedFiles);
				}finally{
					cleanOnClose = prevcleanOnClose;
				}
			}
			
			ArrayList<File> tempOld = new ArrayList<File>(oldcreatedFiles.size());
			for(Pair<File, FileChannel> prevFFc : oldcreatedFiles){
				File fn = prevFFc.getA();
				File pre = new File(fn.getAbsoluteFile() + "Pre" + DEFRAG_PATH_NAME);
				fn.renameTo(pre);
				tempOld.add(pre);
			}
			
			//close locks
			
			{
				boolean prevcleanOnClose = cleanOnClose;
				cleanOnClose=false;//dont want files deleted on closure here...
				try{
					closeBuffersAndFiles();
				}finally{
					cleanOnClose = prevcleanOnClose;
				}
			}

			for(Pair<File, FileChannel> createdFFc : this.createdFiles){
				File cfn = createdFFc.getA();
				String fname = cfn.getAbsoluteFile().toString();
				String newName= fname.substring(0, fname.length() - DEFRAG_PATH_NAME.length());
				cfn.renameTo(new File(newName));
			}
			
			//reopen
			for(File fn : tempOld){
				fn.delete();
			}

			this.path=oldPath;
			try{
				this.createdFiles = new ArrayList<Pair<File, FileChannel>>();
	    		createNewBuffers();
	    		init();
			}catch(Exception e){
				throw new RuntimeException("Cannot create off heap space: " + e.getMessage(), e);
			}
    	}
		return highwatermark;
    }
    
    
    /**
     * aquire map locks as one goes along...
     * @param newCap
     * @return
     */
    private boolean compactBelow(long newCap){
    	OffHeapRWLock[][] locks = indexRegionManager.getRegionLocks();
    	final long protectedRegionStart = getProtectedRegion();
    	
    	long[] offsets = indexRegionManager.getRegionMapOffsets();
    	int[] sizes = indexRegionManager.getRegionMapSizes();
    	
		//step 1 - see if we can move all entries spanning above new capacity level below.
		boolean nonAboveNewCap=true;
		for(int aoffset = 0; aoffset < offsets.length; aoffset++){
			long offset = offsets[aoffset];
			int size = sizes[aoffset];
			OffHeapRWLock[] lockset = locks[aoffset];
			
			for(int n=0; n < size; n++){
				if(null != lockset){
					lockset[n].lockWrite();
				}
				
	    		long currentPtr =  protectedRegionStart + offset + (n*8);
	    				
	    		long nextEntry = this.getLong(currentPtr);
	        	long prevEntry = currentPtr;
	    		
	        	if(nextEntry != 0){
	        		currentPtr = nextEntry;
	        	}
	        	
	        	while(nextEntry != 0){
	        		nextEntry = this.getLong(currentPtr);
	        		//nexPnter[long] | keyhash[int] | keySize | key | pnterToValue ....
	        		int keySize = this.getInt(currentPtr+8+4);
	    			long pointerToValue = this.getLong(currentPtr+8+4+4+keySize); 
	    			long newPointerToValueAddress=0;
	    			//ensure value does not extend beyond newCap
	    	        int valueSize = (getInt(pointerToValue - HEADER_SIZE + SIZE_OFFSET) & FREE_MASK) - 8 - 7;
	    	        {//value may be above limit
	    	        	long endAddress = pointerToValue + valueSize;
	        	        if(endAddress > newCap-8){//need to move valuepoint to different place
	        	        	newPointerToValueAddress = malloc(valueSize, false, false).getB();//mallocImpl(getBin(valueSizeNotIncHeader), valueSizeNotIncHeader);
	        	        	if (newPointerToValueAddress == 0) {
	        	        		nonAboveNewCap=false;
	        	            	break;
	        	            }else{//copy data across
	        	            	byte[] stuff = this.getBytes(pointerToValue, valueSize);
	        	            	this.putBytes(newPointerToValueAddress, stuff);
	        	            	//this.putBytes(pointerToValue, stuff);
	        	            	//System.err.println(String.format("putbtyes: %s - %s", newPointerToValueAddress, newPointerToValueAddress+stuff.length));
	        	            	free(pointerToValue);
	        	            }
	        	        }
	    	        }
	    	        
	    	        int sizeOfEntry = 8+4+4+keySize+8;
	    	        {//entry itself may be above limit
	    	        	long endAddress = currentPtr + sizeOfEntry;
	    	        	if(endAddress > newCap){
	    	        		long newEntryAddress = malloc(sizeOfEntry, false, false).getB();//mallocImpl(getBin(sizeOfEntry), sizeOfEntry);
	    	        		if (newEntryAddress == 0) {
	        	        		nonAboveNewCap=false;
	        	        		if(newPointerToValueAddress != 0){//we still moved the value, but we were unable to move the entry
	        	        			this.putLong(currentPtr+8+4+4+keySize, newPointerToValueAddress);	
	                	        }
	        	        		
	        	            	break;
	    	        		}else{
	    	        			//splice in
	    	        			byte[] stuff = this.getBytes(currentPtr, sizeOfEntry);
	        	            	this.putBytes(newEntryAddress, stuff);
	        	            	free(currentPtr);
	        	            	currentPtr=newEntryAddress;//for previous one...
	    	        			this.putLong(prevEntry, currentPtr);//set next pnter
	        	            	if(newPointerToValueAddress != 0){//we moved the value as well
	        	        			this.putLong(currentPtr+8+4+4+keySize, newPointerToValueAddress);	
	                	        }
	    	        		}
	    	        	}else if(newPointerToValueAddress != 0){
		        			this.putLong(currentPtr+8+4+4+keySize, newPointerToValueAddress);	
	    	        	}
	    	        }
	    	        //ensure that entry does not reside above newCap
	        		
	    	        prevEntry = currentPtr;
	        		currentPtr = nextEntry;
	        	}
	    	}
		}
		
		
		//FREE LIST
		if(nonAboveNewCap){//no entries above cap, now ensure free list does not extend above cap, else pointless to change this
			for (int bin = getBin(MIN_CHUNK); bin < BIN_COUNT; bin++) {
                long prev = base + bin * BIN_SIZE;
                for (long chunk; (chunk = getLong(prev + NEXT_OFFSET)) != 0; prev = chunk) {
                	int chunkSize = getInt(chunk + SIZE_OFFSET);//includes headers etc
                	
                	//if whole thing is in cutoff region: removeFreeChunk
                	//if thing is partiall in region
                	if(chunk > this.capacity){
                		removeFreeChunk(chunk);
                	}else if( chunk + chunkSize > this.capacity){//cuts over capacity boundry
                		int leftover = (int)((chunk + chunkSize) - this.capacity);
                		if(leftover >= MIN_CHUNK){//adjust block size
                			putInt(chunk + SIZE_OFFSET, leftover);
                            putInt(chunk + leftover + LEFT_OFFSET, leftover);
                		}
                	}
                }
                
            }
		}
		
		return nonAboveNewCap;
    }
    
    public synchronized long defrag(){
    	if(indexRegionManager != null){
    		OffHeapRWLock[][] locksforMaps = indexRegionManager.getRegionLocks();
    		
        	try{
        		for(OffHeapRWLock[] locks : locksforMaps){
        			if(null != locks){
        				int locklen = locks.length;
                		
                		for(int n=0; n < locklen; n++){
                			locks[n].lockWrite();
                		}
        			}
        		}
            	
        		return defragPostLocked();
        	}
        	finally{
        		
        		for(OffHeapRWLock[] locks : locksforMaps){
        			if(null != locks){
        				int locklen = locks.length;
                		
                		for(int n=0; n < locklen; n++){
                			locks[n].unlockWrite();
                		}
        			}
        		}
        		
        	}
    	}else{
    		throw new RuntimeException("Off heap store does not support defragmentation");
    	}
    }
    
    
    private boolean defragOnDemand = true;
    
    public void setDefragOnDemand(boolean defragOnDemand){
    	this.defragOnDemand = defragOnDemand;//slow operation
    }
    
    public boolean getDefragOnDemand(){
    	return this.defragOnDemand;
    }
    
    private synchronized void increaseCapacity(long capacity){
    	//this.capacity = capacity;
		long oldCap = this.capacity;
		this.capacity = capacity;//asign early to make code in createBuffer more simple
		buffers[0].putLong(base + CAPACITY_OFFSET, capacity);
		int newbufArraySize = (int) (this.capacity / SEGMENT_SIZE) + ((capacity % SEGMENT_SIZE != 0) ? 1 : 0);
		
		//recalc final one
		int penultimate = buffers.length-1;
		ByteBuffer prevb = buffers[penultimate];
		if(prevb != null){//replace this buffer
			try {
				buffers[penultimate] = createBuffer(penultimate, prevb);
				this.closeDirectBuffer(prevb);//TODO: need to close file attached to buffer etc
			} catch (IOException e) {
				this.capacity=oldCap;
				throw new RuntimeException("Unable to ajust capacity: " + e.getMessage(), e);
			}
		}
		
		if(newbufArraySize > buffers.length){//add more buffer if necisary..
			ByteBuffer[] oldBuffs = buffers;
			ByteBuffer[] newBuffers = new ByteBuffer[newbufArraySize];
			System.arraycopy(this.buffers, 0, newBuffers, 0, buffers.length);
			buffers = newBuffers;
			if(this.preallocate){
				int lastone = buffers.length-1;
				try {
					buffers[lastone] = createBuffer(lastone, null);
				} catch (IOException e) {
					this.capacity=oldCap;
					this.buffers = oldBuffs;//seems more sensible safe thing to do
					throw new RuntimeException("Unable to ajust capacity: " + e.getMessage(), e);
				}
			}
		}
		
		//adjust free list with respect to new space allocated
		
		//recalculate startBin as if we had inited it
		long startBin = base + BIN_SPACE;
        long endBin = base + oldCap - HEADER_SIZE * 2;
        // Initialize the bins with the chunks of the maximum possible size
        do {
            int size = (int) Math.min(endBin - startBin, MAX_CHUNK);
            startBin += size + HEADER_SIZE;
        } while (endBin - startBin >= MIN_CHUNK);
		
	     endBin = base + this.capacity - HEADER_SIZE * 2;
		
		 do {
	         int size = (int) Math.min(endBin - startBin, MAX_CHUNK);
	         addFreeChunk(startBin, size);
	         addBoundary(startBin + size);
	         freeMemory += size;
	         startBin += size + HEADER_SIZE;
	     } while (endBin - startBin >= MIN_CHUNK);
		
		/*
		 
		 //bins
            long startBin = base + BIN_SPACE;
            long endBin = base + capacity - HEADER_SIZE * 2;
            if (endBin - startBin < MIN_CHUNK) {
                throw new IllegalArgumentException("Malloc area too small");
            }

            // Initialize the bins with the chunks of the maximum possible size
            do {
                int size = (int) Math.min(endBin - startBin, MAX_CHUNK);
                addFreeChunk(startBin, size);
                addBoundary(startBin + size);
                freeMemory += size;
                startBin += size + HEADER_SIZE;
            } while (endBin - startBin >= MIN_CHUNK);
            
            
		 */
		/*
        long prev = base + getBin((int)Math.min((long)SEGMENT_SIZE, addedspace)) * BIN_SIZE;
        putInt(chunk + SIZE_OFFSET)
        for (long chunk; (chunk = getLong(prev + NEXT_OFFSET)) != 0; prev = chunk) {
        	int chunkSize = getInt(chunk + SIZE_OFFSET);//includes headers etc
        	
        }*/
    }
    
    private void adjustCapacityWhilstRunning(long capacity){
    	if(capacity < this.capacity){//contraction
			if(getProtectedRegion() == 0 || indexRegionManager==null){ //needs to have an index of contents to be able to do this
				throw new IllegalArgumentException(String.format("Cannot reduce capacity of off heap store from: %s to: %s as it is not marked as being compactable", this.capacity, capacity));
			}
			reduceCapacity(capacity);
		}
		else if(capacity > this.capacity){//expandable
			increaseCapacity(capacity);
		}
    	//same do nothing
    }
    
    
    synchronized void start(){
    	try{
    		if(started){
				throw new IllegalStateException("Off heap store has already been started");
    		}
    		
    		if(capacity == null){
    			if(this.path == null || !this.path.exists()){
    				throw new IllegalStateException("No capacity specified for off heap map, so must determine this from provided path, however, path provided does not exist: " + path);
    			}
    		}
    		
    		if(this.path != null && this.path.exists()){//load cap from first segment file
				File firstfile = new File(path.getAbsolutePath());
				try(RandomAccessFile rac = new RandomAccessFile(firstfile, "rw")){
    				rac.seek(base + CAPACITY_OFFSET);
    				this.capacity = rac.readLong();
				}catch(Exception e){
					if(this.capacity == null){
	    				throw new IllegalStateException("Unable to determine off heap capacity from path provided, file invalid format: " + path);
					}
				}
				
			}
    		
    		createNewBuffers();
    		init();//Initial setup of the empty heap
    		this.started  = true;
    	}
    	catch(Exception e){
    		throw new RuntimeException("Cannot create off heap space: " + e.getMessage(), e);
    	}
    }
    
    private void checkStarted(){
		if(!started){
			throw new IllegalStateException("Off heap malloc has not been started");
		}
	}
    
    public void setPreallocate(boolean preallocate){
    	this.preallocate = preallocate;
    }
    
    public boolean getPreallocate(){
    	return preallocate;
    }

    private void createNewBuffers() throws FileNotFoundException, IOException{
		int bufArraySize = (int) (this.capacity / SEGMENT_SIZE) + ((capacity % SEGMENT_SIZE != 0) ? 1 : 0);
		buffers = new MappedByteBuffer[bufArraySize];
		int bufIdx = 0;
		if(this.path != null){
			createdFiles = new ArrayList<Pair<File, FileChannel>>(bufArraySize);
		}
		
		if(this.preallocate){
			for(int n=0; n < bufArraySize; n++){
				buffers[bufIdx++] = createBuffer(n, null);
			}
		}else{
			buffers[bufIdx++] = createBuffer(0, null);//first one only
		}
    }
    
    private ByteBuffer createBuffer(int bufIdx, ByteBuffer copyFrom) throws FileNotFoundException, IOException{
    	long remainingFileSize = capacity - (SEGMENT_SIZE*bufIdx);
		int thisSegmentSize = (int) Math.min(SEGMENT_SIZE, remainingFileSize);
		ByteBuffer bb;
		if(this.path != null){
			File file = new File(path.getAbsolutePath() + (bufIdx > 0?File.separator + (bufIdx-1):""));
			if(cleanOnStart && file.exists() ){
				file.delete();
			}
			
			@SuppressWarnings("resource")
			FileChannel fc = new RandomAccessFile(file, "rw").getChannel();
			
			bb = fc.map(FileChannel.MapMode.READ_WRITE, 0, thisSegmentSize);
			createdFiles.add(new Pair<File, FileChannel>(file, fc));
		}else{
			bb = ByteBuffer.allocateDirect(thisSegmentSize);
			if(copyFrom != null){
				copyFrom.position(0);
				bb.put(copyFrom);
			}
		}
		
		return bb;
    }

    void setProtectedRegionAndSize(long indexRegion, int size){
    	this.putLong(base + PROTECTED_REGION_START, indexRegion);
    	this.putInt(base + PROTECTED_REGION_SIZE, size);
    }
    
    long getProtectedRegion(){
    	return this.getLong(base + PROTECTED_REGION_START);
    }
    
    private void init() {
    	ByteBuffer firstBuffer = buffers[0];
    	
    	long fileVersion = firstBuffer.getLong(base + VERSION_OFFSET);
    	long signature = firstBuffer.getLong(base + SIGNATURE_OFFSET);

        // If the heap already contains data (e.g. backed by an existing file), do relocation instead of initialization
        if (fileVersion != 0 || signature != 0) {
            if (signature != this.fileSignature) {
                throw new IllegalArgumentException(String.format("Cannot read data from persisted store - unexpected signature, expected: %s got: %s", this.fileSignature, signature));
            }
            
            if(fileVersion != FILE_VERSION){//JPT: below not been tested
                throw new IllegalArgumentException(String.format("Cannot read data from persisted store - unexpected file version, expected: %s got: %s", FILE_VERSION, fileVersion));
            }
            
            /* else if ( != capacity) {
                throw new IllegalArgumentException("Malloc capacity mismatch");
            }*/
            
            capacity = firstBuffer.getLong(base + CAPACITY_OFFSET);
            //long oldBase = firstBuffer.getLong(base + BASE_OFFSET);
            firstBuffer.putLong(base + BASE_OFFSET, base);
            long freeMemory = 0;

            for (int bin = getBin(MIN_CHUNK); bin < BIN_COUNT; bin++) {
                long prev = base + bin * BIN_SIZE;
                for (long chunk; (chunk = getLong(prev + NEXT_OFFSET)) != 0; prev = chunk) {
                    freeMemory += getInt(chunk + SIZE_OFFSET);
                }
            }
            
            this.freeMemory = freeMemory;
        } else {
        	firstBuffer.putLong(base + VERSION_OFFSET, FILE_VERSION);
        	firstBuffer.putLong(base + SIGNATURE_OFFSET, this.fileSignature);
        	firstBuffer.putLong(base + CAPACITY_OFFSET, capacity);
        	firstBuffer.putLong(base + BASE_OFFSET, base);

            //bins
            long startBin = base + BIN_SPACE;
            long endBin = base + capacity - HEADER_SIZE * 2;
            if (endBin - startBin < MIN_CHUNK) {
                throw new IllegalArgumentException("Malloc area too small");
            }

            // Initialize the bins with the chunks of the maximum possible size
            do {
                int size = (int) Math.min(endBin - startBin, MAX_CHUNK);
                addFreeChunk(startBin, size);
                addBoundary(startBin + size);
                freeMemory += size;
                startBin += size + HEADER_SIZE;
            } while (endBin - startBin >= MIN_CHUNK);
        }
    }

  /*  private void setHighWaterMark(long usedUpto){
    	if(usedUpto > highWaterMark){
    		highWaterMark = usedUpto;
    		buffers[0].putLong(base + HIGH_WATER_MARK_OFFSET, highWaterMark);
    	}
    }*/
    
    private Pair<ByteBuffer, Long> malloc(int size, boolean thrownoome, boolean attemptDefrag) {
    	checkStarted();
    	if(size > MAX_OBJ_SIZE){
    		throw new IllegalArgumentException(String.format("max allocation size is: %s bytes", MAX_OBJ_SIZE));
    	}
    	
        int alignedSize = (Math.max(size, 16) + (HEADER_SIZE + 7)) & ~7;
        long address = mallocImpl(getBin(alignedSize), alignedSize);
        if (address != 0) {
        	//setHighWaterMark(address + alignedSize);
        	return new Pair<ByteBuffer, Long>(getBuffer(address), address);
        }else{
        	if(attemptDefrag && this.defragOnDemand){
        		this.defrag();
        		//attempt defrag then request again...
        		address = mallocImpl(getBin(alignedSize), alignedSize);
                if (address != 0) {
                	//setHighWaterMark(address + alignedSize);
                	return new Pair<ByteBuffer, Long>(getBuffer(address), address);
                }
        	}
        	
        	if(thrownoome){
                throw new OffHeapOutOfMemoryError("Failed to allocate contiguous " + size + " bytes");
            }else{
            	return new Pair<ByteBuffer, Long>(null, 0l); 
            }
        }
        
    }
    
    public Pair<ByteBuffer, Long> malloc(int size) {
    	if(size <= 0){
    		throw new RuntimeException("Invalid malloc allocation, must be at least 1 byte in size");
    	}
    	return malloc(size, true, true);
    }
    

	/*public ByteBuffer getBuffer(long address) {
		return getBuffer(address, null);
	}*/
	
	public ByteBuffer getBuffer(long address) {
		ByteBuffer bb = getBufferForAddress(address); 
    	
    	ByteBuffer ret;
    	Fiber fiber = Fiber.getCurrentFiber();
    	if(fiber != null){//avoid creating extra bytebuffers if we dont have to - have these iso specific instances
    		ret = (ByteBuffer) fiber.iso.isoLocalCache.get(bb);
    		if(null == ret){
    			ret = bb.duplicate();
    			fiber.iso.isoLocalCache.put(bb, ret);
    		}
    	}else{
    		ret = bb.duplicate();//TODO: return threadlocal cached version of buffer
    	}
    	
    	ret.position((int) (address % SEGMENT_SIZE));
        return ret;
	}

    final synchronized long mallocImpl(int bin, int size) {
        do {//check from bucket upwards until we arrive at a free space big enough
            long address = findChunk(base + bin * BIN_SIZE, size);
            if (address != 0) {
                return address + HEADER_SIZE;
            }
        } while (++bin < BIN_COUNT);

        return 0;
    }

    private ByteBuffer getBufferForAddress(long address) {// must fit in 'block'
    	int slot = (int) (address / SEGMENT_SIZE);
		ByteBuffer currentBuffer = buffers[slot];
		if(!this.preallocate && null == currentBuffer){
			synchronized(this){
				if(null == buffers[slot]){
					this.checkStarted();
					try {
						buffers[slot] = createBuffer(slot, null);
					} catch (Exception e) {
						throw new RuntimeException("Unable to create buffer associated with address: " + e.getMessage(), e);
					}
				}
			}
			currentBuffer = buffers[slot];
		}
		currentBuffer.position((int )(address % SEGMENT_SIZE));
		return currentBuffer;
	}
	
	protected int getInt(long address){
		return getBufferForAddress(address).getInt((int) (address % SEGMENT_SIZE));
	}
	
	protected long getLong(long address){
		return getBufferForAddress(address).getLong((int) (address % SEGMENT_SIZE));
	}
	
	protected byte get(long address){
		return getBufferForAddress(address).get((int) (address % SEGMENT_SIZE));
	}
	
	protected byte[] getBytes(long address, int size){
		ByteBuffer bb = getBufferForAddress(address);
		bb.position((int) (address % SEGMENT_SIZE));
		byte[] ret = new byte[size];
		bb.get(ret);
		return ret; 
	}
	
	protected void putBytes(long address, byte[] what){
		getBufferForAddress(address).put(what);
	}
	
	protected void putInt(long address, int what){
		getBufferForAddress(address).putInt( (int) (address % SEGMENT_SIZE), what);
	}
	protected void putLong(long address, long what){
		getBufferForAddress(address).putLong((int) (address % SEGMENT_SIZE), what);
	}
	
	@Override
    public synchronized void free(long address) {
    	checkStarted();
        address -= HEADER_SIZE;
        
        // Calculate the addresses of the neighbour chunks
        int size = getInt(address + SIZE_OFFSET) & FREE_MASK;
        long leftChunk = address - getInt(address + LEFT_OFFSET);
        long rightChunk = address + size;
        int leftSize = getInt(leftChunk + SIZE_OFFSET);
        int rightSize = getInt(rightChunk + SIZE_OFFSET);

        freeMemory += size;

        // Coalesce with left neighbour chunk if it is free
        if (leftSize > 0) {
            size += leftSize;
            removeFreeChunk(leftChunk);
            address = leftChunk;
        }

        // Coalesce with right neighbour chunk if it is free
        if (rightSize > 0) {
            size += rightSize;
            removeFreeChunk(rightChunk);
        }

        // Return the combined chunk to the bin
        addFreeChunk(address, size);
    }

    // Separate large chunks by occupied boundaries to prevent coalescing
    private void addBoundary(long address) {
        putInt(address + SIZE_OFFSET, HEADER_SIZE | mask);
        putInt(address + HEADER_SIZE + LEFT_OFFSET, HEADER_SIZE);
    }

    // Find a suitable chunk in the given bin using best-fit strategy
    private long findChunk(long binAddress, int size) {
        int bestFitSize = Integer.MAX_VALUE;
        long bestFitChunk = 0;
        int scanCount = MAX_SCAN_COUNT;

        for (long chunk = binAddress; (chunk = getLong(chunk + NEXT_OFFSET)) != 0; ) {
            int chunkSize = getInt(chunk + SIZE_OFFSET);
            int leftoverSize = chunkSize - size;
            
            if (leftoverSize < 0) {
                // Continue search
            } else if(chunkSize - 1 + ((int)chunk / SEGMENT_SIZE) > SEGMENT_SIZE){//ensure will not 'escape' current buffer region
            	//i.e. we do not permit allocations to span multiple 2 gig bytebuffer regions
            	// continue search...
            } else if(chunk + chunkSize > this.capacity){
            	//skip if allocation would take us beyond the capacity (only triggered if we are currently reducing the capacity of the store)
            }
            else if (leftoverSize < MIN_CHUNK) {
                // Allocated memory perfectly fits the chunk
                putInt(chunk + SIZE_OFFSET, chunkSize | mask);
                freeMemory -= chunkSize;
                removeFreeChunk(chunk);
                return chunk;
            } else if (leftoverSize < bestFitSize) {
                // Search for a chunk with the minimum leftover size
                bestFitSize = leftoverSize;
                bestFitChunk = chunk;
            } else if (--scanCount <= 0 && bestFitChunk != 0) {
                // Do not let scan for too long
                break;
            }
        }

        if (bestFitChunk != 0) {
            // Allocate memory from the best-sized chunk
            putInt(bestFitChunk + SIZE_OFFSET, size | mask);
            freeMemory -= size;
            removeFreeChunk(bestFitChunk);

            // Cut off the remaining tail and return it to the bin as a smaller chunk
            long leftoverChunk = bestFitChunk + size;
            addFreeChunk(leftoverChunk, bestFitSize);
            putInt(leftoverChunk + LEFT_OFFSET, size);
        }

        return bestFitChunk;
    }

    // Insert a new chunk in the head of the linked list of free chunks of a suitable bin
    private void addFreeChunk(long address, int size) {
        putInt(address + SIZE_OFFSET, size);
        putInt(address + size + LEFT_OFFSET, size);

        long binAddress = base + getBin(size) * BIN_SIZE;
        long head = getLong(binAddress + NEXT_OFFSET);
        putLong(address + NEXT_OFFSET, head);
        putLong(address + PREV_OFFSET, binAddress);
        putLong(binAddress + NEXT_OFFSET, address);
        if (head != 0) {
            putLong(head + PREV_OFFSET, address);
        }
    }

    // Remove a chunk from the linked list of free chunks
    private void removeFreeChunk(long address) {
        long next = getLong(address + NEXT_OFFSET);
        long prev = getLong(address + PREV_OFFSET);

        putLong(prev + NEXT_OFFSET, next);
        if (next != 0) {
            putLong(next + PREV_OFFSET, prev);
        }
    }

    // Calculate the address of the smallest bin which holds chunks of the given size.
    // Bins grow somewhat logarithmically: 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192 ...
    static int getBin(int size) {
        size -= HEADER_SIZE + 1;
        int index = 29 - Integer.numberOfLeadingZeros(size);
        return (index << 2) + ((size >>> index) & 3);
    }

    static int binSize(int bin) {
        bin++;
        return (4 + (bin & 3)) << (bin >>> 2);
    }

	public void setCleanOnStart(boolean cleanOnStart) {
		this.cleanOnStart = cleanOnStart;
	}
	
	private final static String DEFRAG_PATH_NAME = "defrag";

	public void setPath(File path) {
		if(path.getAbsolutePath().contains(DEFRAG_PATH_NAME)){
			throw new IllegalArgumentException("File path name may not contain reserved word: " + DEFRAG_PATH_NAME);
		}
		
		this.path = path;
	}

	public void setCleanOnClose(boolean cleanOnClose) {
		this.cleanOnClose = cleanOnClose;
	}

	protected void finalize() throws Throwable { 
		this.close();
	}
	
	private void closeDirectBuffer(ByteBuffer cb) {
		
		Fiber fiber = Fiber.getCurrentFiber();
		if(null != fiber){
			fiber.iso.isoLocalCache.remove(cb);
		}
		
	    if (!cb.isDirect()) return;

	    try {
	        Method cleaner = cb.getClass().getMethod("cleaner");
	        cleaner.setAccessible(true);
	        Object cleanerInstance = cleaner.invoke(cb);
	        Class<?> cleanerClass = cleanerInstance.getClass();
	        Method clean = cleanerClass.getMethod("clean");
	        clean.setAccessible(true);
	        clean.invoke(cleanerInstance);
	    } catch(Exception ex) { 
	    	throw new RuntimeException( ex);
	    }
	    cb = null;
	}

	public void close() {
		this.started=false;
		closeBuffersAndFiles();
	}
	
	private void closeBuffersAndFiles(){
		for(ByteBuffer bb : buffers){
			closeDirectBuffer(bb);
		}
		
		closeCreatedFiles(createdFiles);
		
	}
	
	private void closeCreatedFiles(ArrayList<Pair<File, FileChannel>> cfss){
		if(this.path != null && cfss != null && !cfss.isEmpty()){
			for(Pair<File, FileChannel> fu : cfss){
				FileChannel fc = fu.getB();
				
				try {
					fc.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
			
			for(Pair<File, FileChannel> fu : cfss){
				File ff = fu.getA();
				
				if(this.cleanOnClose && ff.exists()){
					try {
						Files.delete(ff.toPath());
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}

	public Boolean getCleanOnStart() {
		return this.cleanOnStart;
	}

	public Boolean getRemoveOnClose() {
		return this.cleanOnClose;
	}

	public boolean isStarted() {
		return this.started;
	}
	
}
