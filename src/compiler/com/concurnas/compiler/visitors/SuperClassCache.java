package com.concurnas.compiler.visitors;

import java.util.HashMap;

import com.concurnas.compiler.ast.ClassDef;

public class SuperClassCache {//TODO: is this class used anywhere?
    // Private constructor prevents instantiation from other classes
    private SuperClassCache() { }

    /**
    * SingletonHolder is loaded on the first execution of Singleton.getInstance() 
    * or the first access to SingletonHolder.INSTANCE, not before.
    */
    private static class SingletonHolder { 
            public static final SuperClassCache INSTANCE = new SuperClassCache();
    }

    public static SuperClassCache getInstance() {
            return SingletonHolder.INSTANCE;
    }
    
    private HashMap<String, ClassDef> cache = new HashMap<String, ClassDef>();
    
    public ClassDef getSuperClass(ClassDef cls)
    {
    	String key = cls.getPrettyName();
    	if(!cache.containsKey(key))
    	{
    		ClassDef value = cls.getSuperclass();
    		cache.put(key, value);    		
    	}
    	
    	return cache.get(key);
    }
    
}