package com.concurnas.lang.offheap.storage;

import java.io.File;
import java.io.IOException;

import com.concurnas.lang.Uninterruptible;

@Uninterruptible
public class OffHeapDisk<T> extends OffHeapPutGettable<T>{
	private File path;
	private boolean preallocate;	
	
	public OffHeapDisk(long bytesize) {
		super.setCapacity(bytesize);
	}
	public OffHeapDisk(long bytesize, final File path) {
		this(bytesize);
		this.setPath(path);
	}
	
	@Override
	public void prepare(){
		super.prepare();
		if(null == path){
			try {
				path = File.createTempFile("offHeapDisk." + super.uid + ".",".tmp");
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot create temp file for off heap disk store: " + e.getMessage(), e);
			}
		}
		
		super.offHeapStorage.setPath(path);
		super.offHeapStorage.setPreallocate(preallocate);
		super.offHeapStorage.setCleanOnStart(true);
		super.offHeapStorage.setCleanOnClose(true);
	}
	
	public void setPath(File path){
		if(!path.isFile()){
			throw new IllegalArgumentException(String.format("Specified path must be a file: %s is not", path ));
		}
		this.path = path;
	}
	
	public File getPath(){
		return this.path;
	}
	
	@Override
	public void close(){
		super.close();
	}
	
	public void setPreallocate(boolean preallocate) {
		this.preallocate = preallocate;
	}
	
	public boolean getPreallocate() {
		return super.offHeapStorage != null?super.offHeapStorage.getPreallocate():preallocate;
	}
}
