package com.concurnas.lang.offheap.storage;

public interface OffHeapOutputBuffer {

	void putInt(int aint);

	void putLong(long along);

	void putDouble(double adouble);

	void putFloat(float afloat);

	void put(byte b);

	void putShort(short aShort);

	void putChar(char aChar);

	int position();

	void position(int pos);

}
