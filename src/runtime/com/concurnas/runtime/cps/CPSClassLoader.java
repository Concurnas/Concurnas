package com.concurnas.runtime.cps;

/**
 * Extends Classloader just to have access to the (protected) findLoadedClass method
 */
public class CPSClassLoader extends ClassLoader {
    public CPSClassLoader(ClassLoader cl) {
        super(cl);
    }

    public Class<?> getLoadedClass(String className) {
        return super.findLoadedClass(className);
    }
    
    public boolean isLoaded(String className) {
        return getLoadedClass(className) != null;
    }
}
