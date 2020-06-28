package com.concurnas.runtime;

import java.lang.reflect.Constructor;

import com.concurnas.bootstrap.runtime.ref.DirectlyAssignable;
import com.concurnas.bootstrap.runtime.ref.DirectlyGettable;
import com.concurnas.bootstrap.runtime.ref.Ref;
import com.concurnas.runtime.ref.Local;


public class GenericRefCast {
	private static int getRefLevels(Class<?>[] convertTo){
		int r = 0;
		for(int n=0; n < convertTo.length; n++){
			if(Ref.class.isAssignableFrom(convertTo[n])){
				r+=1;
			}
		}
		return r;
	}
	
	/**
	 * If check component type [could be obj]
	 * shift levels up or down as approperiate
	 * else fail with exception
	 * 
	 * Note that if somehow the from Object is already a ref, we check the componant type for a 1:1 match  
	 * 
	 * @param from
	 * @param int:: -? [Local, Local, Integer]
	 * @return 'casted' entity
	 * @throws Throwable 
	 */
	public static Object genericRefCast(Object from, Class<?>[] convertTo) throws Throwable{
		/*if(from==null){
			return null;
		}
		
		Class<?>[] decTypes = RefUtils.extractType(from);*/
		//System.err.println(String.format("from: %s, convertTo: %s", null, Stringifier.stringify(convertTo)));
		
		//if its a null then component type must match
		Class<?>[] decTypes = from==null ? new Class<?>[]{convertTo[convertTo.length-1]} : RefUtils.extractType(from);
		
		Class<?> declComponentType = decTypes[decTypes.length-1];
		Class<?> converToComponentType = convertTo[convertTo.length-1];
		
		
		//if(converToComponentType!=null && declComponentType!=null && (decTypes[0].isAssignableFrom(Local.class) ? converToComponentType.equals(declComponentType) : converToComponentType.isAssignableFrom(declComponentType))){
		String because = "";
		try {
			if(converToComponentType!=null && declComponentType!=null && converToComponentType.isAssignableFrom(declComponentType)){
			//if(converToComponentType!=null && declComponentType!=null){
				//component type matches, phew!
				//now cast up and down as approperiate if you can
				
				int decRefLevels = getRefLevels(decTypes);
				int constToRefLevels = getRefLevels(convertTo);
				//System.err.println(String.format("decRefLevels: %s, constToRefLevels: %s" , decRefLevels, constToRefLevels ));
				if(InstanceofGeneric.isGenericInstnaceof(decTypes, convertTo)){//TODO: calling this twice is quite slow - find a way to clean this up
					return from;
				}
				
				if(decRefLevels < constToRefLevels){//add more
					int toAdd = constToRefLevels - decRefLevels;
					Class<?>[] decTypesLast = decTypes;
					Object ret = from;
					for(int n=0; n < toAdd; n++){
						Class<?>[] decTypesNew;// = new Class<?>[decTypesLast.length + 1];
						
						if(n > 0){
							decTypesNew = new Class<?>[decTypesLast.length + 1];
							decTypesNew[0] = convertTo[0];//Local.class;
							System.arraycopy(decTypesLast, 0, decTypesNew, 1, decTypesLast.length);
						}
						else{
							decTypesNew=decTypesLast;
						}
						
						DirectlyAssignable reta = null;
							Constructor<DirectlyAssignable> got;
							try {
								if(convertTo[0].isInterface()) {
									convertTo[0] = Local.class;//hack
								}
								
								got = (Constructor<DirectlyAssignable>) convertTo[0].getConstructor(Class[].class);
								reta = got.newInstance(new Object[] {decTypesNew});
								reta.set(ret);
							} catch (NoSuchMethodException e) {
								throw new RuntimeException("no zero arg constructor defined for type: " +  convertTo[0].getCanonicalName() );
							} catch (Throwable e) {
								throw new RuntimeException(e);
							}
						
						decTypesLast = decTypesNew;
						ret = reta;
					}
					
					if(decRefLevels==0 || InstanceofGeneric.isGenericInstnaceof(ret, convertTo)){//e.g. int:: no cast to Obj:: 
						//bit dirty
						//not if the thing was not already a ref then we can upgrade it here...
						return ret;
					}
				}
				else if(decRefLevels > constToRefLevels && decRefLevels>0){//remove?
					//is isAssignableFrom required here?
					int popOff = decRefLevels - constToRefLevels;
					DirectlyGettable ret = (DirectlyGettable)from;
					for(int n=0; n < popOff; n++){
						try {
							ret = (DirectlyGettable)ret.get(true);
						} catch (Throwable e) {
							throw new RuntimeException(e);
						}
					}
					
					if(InstanceofGeneric.isGenericInstnaceof(ret, convertTo)){//e.g. int:: no cast to Obj:: 
						//bit dirty
						return ret;
					}
				}
				/*else if(decRefLevels == constToRefLevels){//remove
					//is isAssignableFrom required here?
					return from;
				}*/
			}	
		}
		catch(Exception e) {
			because = ", as " + e.getMessage();
		}
		
		ClassCastException cce = new ClassCastException(String.format("%s cannot be cast to %s%s", RefUtils.formatTypeList(decTypes), RefUtils.formatTypeList(convertTo), because ));
		StackTraceElement[] es = cce.getStackTrace();
		StackTraceElement[] esnew = new StackTraceElement[es.length-5];
		System.arraycopy(es, 5, esnew, 0, es.length-5);
		cce.setStackTrace(esnew);
		throw cce;//TODO: macros
		
	}
}
