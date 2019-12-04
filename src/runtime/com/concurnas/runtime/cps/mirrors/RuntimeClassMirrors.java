package com.concurnas.runtime.cps.mirrors;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;

import org.objectweb.asm.Type;

import com.concurnas.lang.Shared;
import com.concurnas.runtime.cps.CPSClassLoader;

/**
 * This class provides the Mirrors facade over a set of Class objects
 * @see Mirrors 
 */

public class RuntimeClassMirrors  {
    // Weakly cache the mirror objects.
    Map<String, RuntimeClassMirror> cachedClasses = Collections.synchronizedMap(new WeakHashMap<String, RuntimeClassMirror>());
    //TODO: does above need weak cache?

    public final CPSClassLoader classLoader;
    
    public RuntimeClassMirrors(ClassLoader cl) {
        if (!(cl instanceof CPSClassLoader)) {
            cl = new CPSClassLoader(cl);
        }
        this.classLoader = (CPSClassLoader) cl;
    }

    public ClassMirror classForName(String className) throws ClassMirrorNotFoundException {
        try {
            RuntimeClassMirror ret = cachedClasses.get(className);
            if (ret == null) {
                ret = make(classLoader.loadClass(className));
            }
            return ret;
        } catch (ClassNotFoundException e) {
            throw new ClassMirrorNotFoundException(className, e);
        }
    }

    public ClassMirror mirror(Class<?> clazz) {
        if (clazz == null)
            return null;
        return make(clazz);
    }

    public ClassMirror mirror(String className, byte[] bytecode) {
        try {
            return classForName(className);
        } catch (ClassMirrorNotFoundException ignore) {}
        return null;
    }

    /**
     * Like classForName, but only if the class is already loaded. This does not force loading of a
     * class.
     * 
     * @param className
     * @return null if className not loaded, else a RuntimeClassMirror to represent the loaded
     *         class.
     */
    public ClassMirror loadedClassForName(String className) {
        Class<?> c = classLoader.getLoadedClass(className);
        return (c == null) ? null : make(c);
    }

    public Class<?> getLoadedClass(String className) {
        return classLoader.getLoadedClass(className);
    }

    public boolean isLoaded(String className) {
        return classLoader.isLoaded(className);
    }

    private RuntimeClassMirror make(Class<?> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        RuntimeClassMirror ret = new RuntimeClassMirror(c);
        cachedClasses.put(c.getName(), ret);
        return ret;
    }
}

class RuntimeConstructorMirror implements ConstructorMirror{
	 private final Constructor<?> con;

	    public RuntimeConstructorMirror(Constructor<?> con) {
	        this.con = con;
	    }

		@Override
		public String getMethodDescriptor() {
	        return Type.getConstructorDescriptor(con);
		}

		@Override
		public boolean isPublic() {
	    	//int mod = con.getModifiers();
	    	return Modifier.isPublic(con.getModifiers());// && !Modifier.isStatic(mod);
		}
}

class RuntimeMethodMirror implements MethodMirror {

    private final Method method;

    public RuntimeMethodMirror(Method method) {
        this.method = method;
    }

	@Override
	public boolean hasBeenConced() {
		return false;
	}
    
    public String getName() {
        return method.getName();
    }

    public String[] getExceptionTypes() {
        String[] ret = new String[method.getExceptionTypes().length];
        int i = 0;
        for (Class<?> excl : method.getExceptionTypes()) {
            ret[i++] = excl.getName();
        }
        return ret;
    }
    
    public boolean isPublicAndNonStatic(){
    	int mod = method.getModifiers();
    	return Modifier.isPublic(mod) && !Modifier.isStatic(mod);
    }

    public String getMethodDescriptor() {
        return Type.getMethodDescriptor(method);
    }
    
    public String getMethodsignature() {
        return null;
    }

    public boolean isBridge() {
        return method.isBridge();
    }
    
	public boolean isFinal(){
		return Modifier.isFinal(method.getModifiers());
	}
    
}

class RuntimeClassMirror implements ClassMirror {

    private final Class<?> clazz;
    private MethodMirror[] methods; 
    private HashMap<String, FieldMirror> fields; 
    private ConstructorMirror[] cons; 
    
    public RuntimeClassMirror(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public String getName() {
        return clazz.getName();
    }

    @Override
    public boolean isInterface() {
        return clazz.isInterface();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ClassMirror) {
            return ((ClassMirror) obj).getName().equals(this.getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return clazz.hashCode();
    }

    @Override
    public MethodMirror[] getDeclaredMethods() {
        if (methods == null) {
           Method[] declaredMethods = clazz.getDeclaredMethods();
           methods = new MethodMirror[declaredMethods.length];
           for (int i = 0; i < declaredMethods.length; i++) {
               methods[i] = new RuntimeMethodMirror(declaredMethods[i]);
           }
        }
        return methods;
    }

    

	@Override
	public ConstructorMirror[] getDeclaredConstructorsWithoutHiddenArgs() {
		if (cons == null) {
           Constructor[] declaredConstructors = clazz.getDeclaredConstructors();
           cons = new ConstructorMirror[declaredConstructors.length];
           for (int i = 0; i < declaredConstructors.length; i++) {
        	   cons[i] = new RuntimeConstructorMirror(declaredConstructors[i]);
           }
        }
        return cons;
	}
    
    @Override
    public String[] getInterfaces() {
        Class<?>[] ifs = clazz.getInterfaces(); 
        String[] result = new String[ifs.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = ifs[i].getName();
        }
        return result;
    }

    public String[] getInterfacesNoEx(){
    	try{
    		return getInterfaces();
    	}
    	catch(Exception e){
    		return null;
    	}
    }
    
    @Override
    public String getSuperclass() {
        Class<?> supcl = clazz.getSuperclass();
        return supcl != null ? supcl.getName() : null;
    }


	@Override
	public String getSuperclassNoEx() {
		try{
			return getSuperclass();
		}
		catch(Exception e){
			return null;
		}
	}
    
    @Override
    public boolean isAssignableFrom(ClassMirror c) {
        if (c instanceof RuntimeClassMirror) {
            RuntimeClassMirror cc = (RuntimeClassMirror) c;
            return clazz.isAssignableFrom(cc.clazz);
        } else if(c instanceof SimpleClassDefProperties){
        	SimpleClassDefProperties cScdp = (SimpleClassDefProperties)c;
        	try {
				if(this.isAssignableFrom(cScdp.getSuperClassMir())){
					return true;
				}
				else{
					for(ClassMirror cm : cScdp.getInterfacesM()){
						return this.isAssignableFrom(cm);
					}
				}
				
			} catch (ClassMirrorNotFoundException e) {
				return false;
			}
        }
        return false;
    }

    private static int getNestedClassLevels(Class<?> clazz){
		Class<?> enc = clazz.getEnclosingClass();
		if(null == enc){
			return 0;
		}
		else{
			return 1 + getNestedClassLevels(enc);
		}
		
    }
    
    public boolean isEnum(){
    	return this.clazz.isEnum();
    }
    
	public int isNestedClass() {
		if(!Modifier.isStatic(this.clazz.getModifiers()) && clazz.isMemberClass()){
			return getNestedClassLevels(clazz);
		}
		return 0;//no
		
	}

	public boolean isFinal() {
		return Modifier.isFinal(this.clazz.getModifiers());
	}

	@Override
	public HashSet<String> getAnnotations() {
		java.lang.annotation.Annotation[] annots = this.clazz.getAnnotations();
		HashSet<String> ret = new HashSet<String>();
		
		for(int n=0; n < annots.length; n++){
			ret.add(annots[n].annotationType().getName());
		}
		
		return ret;
	}

	@Override
	public String[] getGenericArguments() {
		TypeVariable<?>[] res = this.clazz.getTypeParameters();
		String classes[] = new String[res.length];
		int n=0;
		for(TypeVariable<?> tv : res) {
			classes[n++] = tv.getName();
		}
		
		return classes;
	}


	@Override
	public FieldMirror getField(String field) {
		return getFields().get(field);
	}
	
	@Override
	public HashMap<String, FieldMirror> getFields() {
		if(null == fields) {
			java.lang.reflect.Field[] fieldsx = this.clazz.getDeclaredFields();
			fields = new HashMap<String, FieldMirror>();
			for(java.lang.reflect.Field field : fieldsx) {
				String name = field.getName();
				//field itself or type it holds tagged as shared?
				fields.put(name, new FieldMirror(name, field.getType().getName(), null != field.getAnnotation(Shared.class) /*|| null != field.getType().getAnnotation(Shared.class)*/));
			}
		}
		
		return fields;
	}

	@Override
	public MethodMirror getDeclaredMethod(String name, String desc) {
		MethodMirror[] mm = this.getDeclaredMethods();
		for(MethodMirror m : mm) {
			if(m.getName().equals(name) && m.getMethodDescriptor().equals(desc)) {
				return m;
			}
		}
		
		return null;
	}
}