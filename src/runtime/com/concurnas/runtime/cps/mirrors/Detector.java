/* Copyright (c) 2006, Sriram Srinivasan
 *
 * You may distribute this software under the terms of the license 
 * specified in the file "License"
 */
package com.concurnas.runtime.cps.mirrors;

import static com.concurnas.runtime.cps.Constants.D_OBJECT;

import java.util.ArrayList;
import java.util.HashSet;

import com.concurnas.runtime.cps.Constants;
import com.concurnas.runtime.cps.NotPausable;
import com.concurnas.runtime.cps.Pausable;
import com.concurnas.runtime.cps.analysis.AsmDetector;


/**
 * Utility class to check if a method has been marked pausable
 * 
 */
public class Detector {
    public static final int METHOD_NOT_FOUND_OR_PAUSABLE = 0; // either not found, or not pausable if found.
    public static final int PAUSABLE_METHOD_FOUND = 1; // known to be pausable
    public static final int METHOD_NOT_PAUSABLE = 2; // known to be not pausable
    

    // Note that we don't have the kilim package itself in the following list.
    static final String[] STANDARD_DONT_CHECK_LIST = { "java.", "javax." };//TODO: find a better way to do this

    //public static final Detector DEFAULT = new Detector(new RuntimeClassMirrors());

    public final CachedClassMirrors mirror;

    public Detector(CachedClassMirrors mirror) {
        this.mirror = mirror;

        NOT_PAUSABLE = mirror.mirror(NotPausable.class);//nasty
        PAUSABLE = mirror.mirror(Pausable.class);
        OBJECT = mirror.mirror(Object.class);

    }

    public boolean isActor(String name){
    	boolean ret = false;
		try {
			String findName = name.replace('.', '/');
			while(findName!=null && !findName.equals("java/lang/Object")){
				if(findName.equals("com/concurnas/lang/Actor")){
					ret=true;
					break;
	    		}
				ClassMirror cm = this.classForName(findName);
				findName = null != cm?cm.getSuperclassNoEx():null;
			}
		} catch (ClassMirrorNotFoundException e) {
			
		}
		return ret;	
    }
    
    ClassMirror NOT_PAUSABLE, PAUSABLE, OBJECT;

    public boolean isPausable(String className, String methodName, String desc) {
        return getPausableStatus(className, methodName, desc) == PAUSABLE_METHOD_FOUND;
    }

    /**
     * @return one of METHOD_NOT_FOUND, PAUSABLE_METHOD_FOUND, METHOD_NOT_PAUSABLE
     */

    static boolean isNonPausableClass(String className) {
    	//TODO: improve this to exclude legacy code more robustly
        return className == null || className.charAt(0) == '[' || className.startsWith("java.") || className.startsWith("javax.");
    }
    
    static boolean isNonPausableMethod(String methodName) {
    	//TODO: Permit constructors
        return methodName.endsWith("<init>");
    }

    
    public int getPausableStatus(String className, String methodName, String desc) {
        int ret = METHOD_NOT_FOUND_OR_PAUSABLE;
        // array methods (essentially methods deferred to Object (clone, wait etc)
        // and constructor methods are not pausable
        if (isNonPausableClass(className) || isNonPausableMethod(methodName)) {
            ret= METHOD_NOT_FOUND_OR_PAUSABLE; 
        }
        else{
        	 className = className.replace('/', '.');
             try {
                 MethodMirror m = findPausableMethod(className, methodName, desc);
                 if (m != null) {
                	 
                	 //seems to be the key
                	 
                	/* if(m.hasBeenConced()){
                		 ret= PAUSABLE_METHOD_FOUND;
                	 }
                	 else{
                		 ret= METHOD_NOT_PAUSABLE;
                	 }*/
                	 
                	 
                	 ret= PAUSABLE_METHOD_FOUND;
                	 
                     /*for (String ex : m.getExceptionTypes()) {
                         if (isNonPausableClass(ex)) continue;
                         ClassMirror c = classForName(ex);
                         if (NOT_PAUSABLE.isAssignableFrom(c)) {
                        	 System.err.println(String.format("className: %s, methodName: %s, desc: %s -> %s", className, methodName, desc, METHOD_NOT_PAUSABLE));
                             return METHOD_NOT_PAUSABLE;
                         }
                         else if (PAUSABLE.isAssignableFrom(c)) {
                        	 System.err.println(String.format("className: %s, methodName: %s, desc: %s -> %s", className, methodName, desc, PAUSABLE_METHOD_FOUND));
                             return PAUSABLE_METHOD_FOUND;
                         }
                     }*/
                     
                 }
             } catch (ClassMirrorNotFoundException ignore) {

             } catch (VerifyError ve) {
                 ret = AsmDetector.getPausableStatus(className, methodName, desc, this);
             }
        }
       
        //System.err.println(String.format("className: %s, methodName: %s, desc: %s -> %s", className, methodName, desc, ret));
        
        return ret;
    }

    public ClassMirror classForNameNoExcep(String className)   {
    	try {
    		if(null == className) {
    			return null;
    		}
            return classForName(className);
    	}catch(ClassMirrorNotFoundException yuck) {
    		return null;
    	}
    }
    
    public ClassMirror classForName(String className) throws ClassMirrorNotFoundException {
        className = className.replace('/', '.');
        return mirror.classForName(className);
    }

    /**
     * Is the thing being invoked not from a primordial method.
     * e.g. toString on classX: 
     * 	(inst as ClassX).toString() <- calls pasuable via stack
     * 	(inst as Object).toString() <- calls pausable via getStatic 
     */
	public boolean invokedCallIsStackPausable(String className, String methodName, String desc) {
		return mirror.invokedCallIsStackPausable(className, methodName, desc);//cache?
	}

	public int getNestingLevels(String className){
		return mirror.isNestedClass(className);
	}
	
    public boolean overridesPrimordialMethod(String className, String methodName, String desc){
    	//e.g. "toString" from Object is primordial, as is equals etc
    	//anything which is overriding a method from a jdk class (which we cannot cps-ify)
    	//with this, gennerate the fiber on args version but also the getstatic version
    	return false;//mirror.overridesPrimordialMethod(className, methodName, desc);//cache?
    }
    

	public boolean isFinalPrimordialMethod(String owner, String name, String desc) {
		//TODO: cache this?
		return mirror.isFinalPrimordialMethod(owner, name, desc);
	}
    
    
    public MethodMirror findPausableMethod(String className, String methodName, String desc)
            throws ClassMirrorNotFoundException {
        
        if (isNonPausableClass(className) || isNonPausableMethod(methodName)) 
            return null;

        ClassMirror cl = classForName(className);
        if (cl == null) return null;
        
        for (MethodMirror om : cl.getDeclaredMethods()) {
            if (om.getName().equals(methodName)) {
                // when comparing descriptors only compare arguments, not return types
                String omDesc= om.getMethodDescriptor();
            
                if (omDesc.substring(0,omDesc.indexOf(")")).equals(desc.substring(0,desc.indexOf(")")))) {
                    if (om.isBridge())  continue;
                    return om;
                }
            }
        }

       // if (OBJECT.equals(cl))
        //    return null;

        MethodMirror m = findPausableMethod(cl.getSuperclass(), methodName, desc);
        if (m != null)
            return m;
        
        for (String ifname : cl.getInterfaces()) {
            if (isNonPausableClass(ifname)) continue;
            m = findPausableMethod(ifname, methodName, desc);
            if (m != null)
                return m;
        }
        return null;
    }

    public static String D_FIBER_ = Constants.D_FIBER + ")";

    @SuppressWarnings("unused")
    private static String statusToStr(int st) {
        switch (st) {
        case METHOD_NOT_FOUND_OR_PAUSABLE:
            return "not found or pausable";
        case PAUSABLE_METHOD_FOUND:
            return "pausable";
        case METHOD_NOT_PAUSABLE:
            return "not pausable";
        default:
            throw new RuntimeException("Unknown status");
        }
    }

    static private final ThreadLocal<Detector> DETECTOR = new ThreadLocal<Detector>();

    public static Detector getDetector() {
        Detector d = DETECTOR.get();
        //if (d == null)
         //   return Detector.DEFAULT;
        return d;
    }

    public static Detector setDetector(Detector d) {
        Detector res = DETECTOR.get();
        DETECTOR.set(d);
        return res;
    }

    public String getCommonSuperClass(String oa, String ob) throws ClassMirrorNotFoundException {
    	//System.err.println("a -> b: " + oa + " -> " + ob);
    	
        String a = toClassName(oa);
        String b = toClassName(ob);
        
        if (a.equals(b)) {
        	return oa;
        }
        
        try {
            ClassMirror ca = classForName(a);
            ClassMirror cb = classForName(b);
            if(ca==null || cb == null){//will throw not found class later in code
            	return "java/lang/Object";
            }
            if (ca.isAssignableFrom(cb))
                return oa;
            if (cb.isAssignableFrom(ca))
                return ob;
            if (ca.isInterface() && cb.isInterface()) {
                return "java/lang/Object"; // This is what the java bytecode verifier does
            }
        } catch (ClassMirrorNotFoundException e) {
            // try to see if the below works...
        	//return oa;
        }
        
        
        ArrayList<String> sca = getSuperClasses(a);
        ArrayList<String> scb = getSuperClasses(b);
        int lasta = sca.size() - 1;
        int lastb = scb.size() - 1;
        do {
            if (sca.get(lasta).equals(scb.get(lastb))) {
                lasta--;
                lastb--;
            } else {
                break;
            }
        } while (lasta >= 0 && lastb >= 0);
        
        if (sca.size() == lasta+1) {
        	return "java/lang/Object";
        }
        
        return sca.get(lasta + 1).replace('.', '/');
    }
       

    final private static ArrayList<String> EMPTY_STRINGS = new ArrayList<String>(0);
    public ArrayList<String> getSuperClasses(String name) throws ClassMirrorNotFoundException {
        if (name == null) {
            return EMPTY_STRINGS;
        }
        ArrayList<String> ret = new ArrayList<String>(3);
        while (name != null) {
            ret.add(name);
            ClassMirror c = classForName(name);
            name = c == null?null:c.getSuperclass();
        }
        return ret;
    }
    
    public HashSet<String> getTraitsAndTraitSuperClasses(String name) throws ClassMirrorNotFoundException {
    	ClassMirror c = classForName(name);
    	HashSet<String> acc = new HashSet<String>();
    	if(c != null) {
    		String[] ifaces = c.getInterfacesNoEx();
        	
        	for(String tt : ifaces) {
        		getTraitsAndTraitSuperClasses(tt, acc);
        	}
    	}
    	
    	return acc;
    }
    
    private void getTraitsAndTraitSuperClasses(String name, HashSet<String> acc) throws ClassMirrorNotFoundException {
    	acc.add(name);
    	
    	ArrayList<String> allClasses = getSuperClasses(name);
    	
    	for(String sup : allClasses.subList(1, allClasses.size())) {
    		getTraitsAndTraitSuperClasses(sup, acc);
    	}
    	
    	ClassMirror c = classForName(name);
    	String[] ifaces = c.getInterfacesNoEx();
    	for(String iffac : ifaces) {
    		getTraitsAndTraitSuperClasses(iffac, acc);
    	}
    }
    

    private static String toDesc(String name) {
        return (name.equals(JAVA_LANG_OBJECT)) ? D_OBJECT : "L" + name.replace('.', '/') + ';';
    }

    private static String toClassName(String s) {
    	if (s.endsWith(";"))
    		return s.replace('/', '.').substring(1, s.length() - 1);
    	else
    		return s.replace('/', '.');
    }

    static String JAVA_LANG_OBJECT = "java.lang.Object";
}
