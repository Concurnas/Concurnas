package com.concurnas.bootstrap.lang.offheap;

public interface Decoder {

	public abstract int getInt();

	public abstract long getLong();

	public abstract double getDouble();

	public abstract float getFloat();

	public abstract boolean getBoolean();

	public abstract short getShort();

	public abstract byte getByte();

	public abstract char getChar();

	public abstract Object get(long address);

	public abstract Object getObject();

	public abstract Object getIntArray(int levels);

	public abstract Object getFloatArray(int levels);

	public abstract Object getDoubleArray(int levels);

	public abstract Object getLongArray(int levels);

	public abstract Object getShortArray(int levels);

	public abstract Object getCharArray(int levels);

	public abstract Object getByteArray(int levels);

	public abstract Object getBooleanArray(int levels);
	
	public abstract Object getObjectArray(int levels);
	
	public abstract boolean canThrowMissingFieldException();
	
	public abstract boolean[] getFieldsToDefault();
}