package com.concurnas.bootstrap.runtime.cps;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;

import com.concurnas.bootstrap.runtime.CopyTracker;
import com.concurnas.bootstrap.runtime.InitUncreatable;


/**
 * Operates on Class methods: Here we always return the fiberized version of the method for possible subsequent invocation.
 * Operates on Method methods: Here we want to ensue that the fiber is hidden. Also, if invoke is called, then we want to add the fiber if appropriate.
 *
 */

//TODO: remove defaultFieldInit$, and the others used in offheapAgumentation

public class ReflectionHelper {
	///Operations on Class
	
	private static Method[] filterMethods(Class<?> cls, Method[] methods){
		boolean includeFiber =  null != Fiber.getCurrentFiber() && !cls.isAnnotation();//we dont want fibers on annotations
		
		ArrayList<Method> ret = new ArrayList<Method>(methods.length);
		for(Method m : methods){
			Class<?>[] params = m.getParameterTypes();
			int len = params.length;
			
			if(includeFiber){
				if(len == 0 || params[len-1] != Fiber.class){
					continue;
				}
			}else{//no fiber
				if(len > 0 && params[len-1] == Fiber.class){
					continue;
				}
			}
			
			if(m.getName().equals("defaultFieldInit$")) {
				ret.add(m);
			}else if(len == 0 || (params[len-1] != InitUncreatable.class && params[0] != InitUncreatable.class) && (params[len-1] != CopyTracker.class)){//empty or last is Fiber //TODO: add the one for cloner <-check this (i think the cloner has a special InitUncreatType as well)
				ret.add(m);
			}
			
			
		}
		return ret.toArray(new Method[ret.size()]);
	}
	
	
	public static Method[] getDeclaredMethods(Class<?> cls){
		return filterMethods(cls, cls.getDeclaredMethods());
	}
	public static Method[] getMethods(Class<?> cls){
		return filterMethods(cls, cls.getMethods());
	}
	
	public static Constructor<?>[] getConstructors(Class<?> cls){
		Constructor<?>[] methods = cls.getConstructors();
		ArrayList<Constructor<?>> ret = new ArrayList<Constructor<?>>(methods.length);
		for(Constructor<?> m : methods){
			Class<?>[] params = m.getParameterTypes();
			int len = params.length;
			if(len > 0 && params[0] == CopyTracker.class){//exclude copy constructor
				continue;
			}
			
			if(len == 0 || (params[len-1] != Fiber.class && params[0] != InitUncreatable.class && params[len-1] != InitUncreatable.class&& params[len-1] != CopyTracker.class)){//empty or last is Fiber //TODO: add the one for cloner <-check this (i think the cloner has a special InitUncreatType as well)
				ret.add(m);
			}
		}
		return ret.toArray(new Constructor<?>[ret.size()]);
	}
	
	public static Constructor<?> getConstructor(Class<?> cls, Class<?>... parameterTypes) throws NoSuchMethodException,SecurityException{
		boolean includeFiber =  null != Fiber.getCurrentFiber();
		
		if(includeFiber){
			int ptLen=0;
			if(null == parameterTypes || parameterTypes.length == 0){
				parameterTypes = new Class<?>[1];
			}else{
				ptLen = parameterTypes.length;
				Class<?>[] newParamTypes = new Class<?>[ptLen + 1];
				System.arraycopy(parameterTypes, 0, newParamTypes, 0, ptLen);
				parameterTypes = newParamTypes;
			}
			parameterTypes[ptLen] = Fiber.class;
		}
		
		return cls.getConstructor(parameterTypes);
	}
	
	
	public static Method getDeclaredMethod(Class<?> cls, String name, Class<?>... parameterTypes) throws NoSuchMethodException,SecurityException{
		return getMethod(cls, getDeclaredMethods(cls), name, parameterTypes);
	}
	
	public static Method getMethod(Class<?> cls, String name, Class<?>... parameterTypes) throws NoSuchMethodException{
		return getMethod(cls, getMethods(cls), name, parameterTypes);
	}
	
	private static Method getMethod(Class<?> cls, Method[] family, String name, Class<?>... parameterTypes) throws NoSuchMethodException{
		boolean includeFiber =  null != Fiber.getCurrentFiber();
		
		if(includeFiber){
			int ptLen=0;
			if(null == parameterTypes || parameterTypes.length == 0){
				parameterTypes = new Class<?>[1];
			}else{
				ptLen = parameterTypes.length;
				Class<?>[] newParamTypes = new Class<?>[ptLen + 1];
				System.arraycopy(parameterTypes, 0, newParamTypes, 0, ptLen);
				parameterTypes = newParamTypes;
			}
			parameterTypes[ptLen] = Fiber.class;
		}
		
		for(Method m : family){
			if(m.getName().equals(name)){
				Class<?>[] expectedCls = m.getParameterTypes();
				if(parameterTypes.length == expectedCls.length){
					int ecLen = expectedCls.length;
					boolean match = true;
					for(int n=0; n < ecLen; n++){
						if(expectedCls[n]!=parameterTypes[n]){
							match=false;
							break;
						}
					}
					
					if(match){
						return m;
					}
				}
			}
		}
		throw new NoSuchMethodException(cls.getName() + "." + name + argumentTypesToString(parameterTypes));
		//throw new NoSuchMethodException("Cannot find method: " + name + " with matching parameter types");
	}
	
	
	private static String argumentTypesToString(Class<?>[] argTypes) {
        StringBuilder buf = new StringBuilder();
        buf.append("(");
        if (argTypes != null) {
            for (int i = 0; i < argTypes.length; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                Class<?> c = argTypes[i];
                buf.append((c == null) ? "null" : c.getName());
            }
        }
        buf.append(")");
        return buf.toString();
    }
	
	///Operations on Method
	
	public static Class<?>[] getParameterTypes(Method meth){
		//boolean includeFiber =  null != Fiber.getCurrentFiber();
		Class<?>[] ret = meth.getParameterTypes();
		int len = ret.length;
		if(len == 0){
			return ret;
		}else if(ret[len-1]==Fiber.class){
			if(len == 1){
				return new Class[0];
			}else{
				Class<?>[] newret = new Class[len-1];
				System.arraycopy(ret, 0, newret, 0, len-1);
				return newret;
			}
		}
		return ret;
	}
	
	public static int getParameterCount(Method meth){
		Class<?>[] ret = meth.getParameterTypes();
		int len = ret.length;
		if(len == 0){
			return 0;
		}else if(ret[len-1]==Fiber.class){
			return len-1;
		}
		return len;
	}
	
	public static Parameter[] getParameters(Method meth){
		//boolean includeFiber =  null != Fiber.getCurrentFiber();
		Parameter[] ret = meth.getParameters();
		int len = ret.length;
		if(len == 0){
			return ret;
		}else if(ret[len-1].getType()==Fiber.class){
			if(len == 1){
				return new Parameter[0];
			}else{
				Parameter[] newret = new Parameter[len-1];
				System.arraycopy(ret, 0, newret, 0, len-1);
				return newret;
			}
		}
		return ret;
	}
	
	//method invokation
	
	public static Object invoke(Method meth, Object opon, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		Class<?>[] expectedArgs = meth.getParameterTypes();
		Fiber curFiber = Fiber.getCurrentFiber();
		int len = expectedArgs.length;
		//int v=0;
		if(len > 0 && expectedArgs[len-1]==Fiber.class){
			if(curFiber == null){
				throw new IllegalArgumentException("Fiber method: '"+meth.getName()+"' must be invoked via concurnas");
			}
			
			if(null == args || args.length == 0){
				args = new Object[1];
			}
			else{
				Object[] newargs = new Object[len];
				
				System.arraycopy(args, 0, newargs, 0, args.length);
				args = newargs;
			}
			args[args.length-1] = curFiber;
		}
		return meth.invoke(opon, args);
	}
}
