package com.concurnas.lang.offheap.storage;

/**
 *	Signal that space off heap store can be defraged or contracted in size on startup.  
 */
public interface Defragable {
	public void setDefragOnstart(boolean defragOnStart);
}
