package com.concurnas.lang.offheap.storage;

import java.io.File;
import java.io.IOException;

import com.concurnas.lang.Uninterruptible;

@Uninterruptible
public class OffHeapMapDisk<K,V> extends OffHeapMap<K,V>{

	private File path;
	
	public OffHeapMapDisk( ) {
		super();
	}
	public OffHeapMapDisk(long bytesize) {
		super(bytesize);
	}
	
	public OffHeapMapDisk(long bytesize, final File path) {
		this(bytesize);
		this.setPath(path);
	}
	
	public OffHeapMapDisk( final File path) {
		this.setPath(path);
	}
	
	@Override
	protected void prepare(){
		super.prepare();
		
		if(null == path){
			try {
				path = File.createTempFile("offHeapDisk." + super.uid + ".",".tmp");
				if(this.getCapacity() == null){
					path.delete();
    				throw new IllegalStateException("Capacity must be provided when for off heap map when no path is provided");
				}
				if(!hasSetRemoveOnCloseBeenCalled) {
					this.setRemoveOnClose(true);
				}
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot create temp file for off heap disk store: " + e.getMessage(), e);
			}
		}
		super.offHeapStorage.setPath(path);
	}	
	
	
	public void setPath(File path){
		if(path.exists() && !path.isFile()){
			throw new IllegalArgumentException(String.format("Specified path must exist: %s does not", path ));
		}
		this.setRemoveOnClose(false);
		this.path = path;
	}
	
	private boolean hasSetRemoveOnCloseBeenCalled = false;
	
	public void setRemoveOnClose(boolean removeOnClose) {
		hasSetRemoveOnCloseBeenCalled=true;
		super.setRemoveOnClose(removeOnClose);
	}
	
	public File getPath(){
		return this.path;
	}
	
	@Override
	public void close(){
		super.close();
		if(super.getRemoveOnClose()) {
			path.delete();
		}
	}
}
