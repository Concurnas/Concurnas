package com.concurnas.lang;

import java.lang.reflect.Array;

public class ArrayUtils {
	@SuppressWarnings("unchecked")
	public static <T,U> T[] cast(U obj, Class<? extends T[]> newType){
		if(null == obj){
			return null;
		}
		final Class<U> clz = (Class<U>) obj.getClass();
		
		if(newType.isAssignableFrom(clz)){//if its already the type we want, no copy is needed
			return (T[]) obj;
		}
		
		Class<?> childType = clz.getComponentType();
		Class<?> newTypechild = newType.getComponentType();
		
		int length = Array.getLength(obj);
		
		if(childType.isArray()){
			final T[] newInstance = (T[]) Array.newInstance(newTypechild, length);
			for (int i = 0; i < length; i++)
			{
				final Object v = Array.get(obj, i); 
				final Object clone = v == null ? null : cast((U[]) v, (Class<? extends T[]>) newTypechild);
				Array.set(newInstance, i, clone);
			}
			return newInstance;
		}
		else{//great, last instance to cast out of
			return java.util.Arrays.copyOf((U[])obj, length, newType);
		}
		
	}
	
}
