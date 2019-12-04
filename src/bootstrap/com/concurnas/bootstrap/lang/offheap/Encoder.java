package com.concurnas.bootstrap.lang.offheap;


public interface Encoder {
	public abstract int put(int aint);
	public abstract int put(long along);
	public abstract int put(double adouble);
	public abstract int put(float afloat);
	public abstract int put(boolean aboolean);
	public abstract int put(short aShort);
	public abstract int put(byte a);
	public abstract int put(char aChar);
	
	public abstract int put(Object object);
	public abstract int putIntArray(Object object, int levels);
	public abstract int putLongArray(Object object, int levels);
	public abstract int putDoubleArray(Object object, int levels);
	public abstract int putFloatArray(Object object, int levels);
	public abstract int putShortArray(Object object, int levels);
	public abstract int putCharArray(Object object, int levels);
	public abstract int putByteArray(Object object, int levels);
	public abstract int putBooleanArray(Object object, int levels);
	public abstract int putObjectArray(Object object, int levels);
}