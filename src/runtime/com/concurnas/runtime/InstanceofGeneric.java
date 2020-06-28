package com.concurnas.runtime;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import com.concurnas.bootstrap.lang.Stringifier;
import com.concurnas.bootstrap.runtime.ReifiedType;
import com.concurnas.bootstrap.runtime.ref.Ref;

public class InstanceofGeneric {
	public final static void assertGenericInstnaceof(Object passed, Class<?>[] isInstanceOf) throws Throwable{
		if(null == passed){
			ClassCastException cce = new ClassCastException(String.format("null cannot be cast to %s", RefUtils.formatTypeList(isInstanceOf) ));
			StackTraceElement[] es = cce.getStackTrace();
			StackTraceElement[] esnew = new StackTraceElement[es.length-5];
			System.arraycopy(es, 5, esnew, 0, es.length-5);
			cce.setStackTrace(esnew);
			throw cce;//TODO: macros - macros?
		}
		Class<?>[] decTypes = RefUtils.extractType(passed);
		if(!isGenericInstnaceof(decTypes, isInstanceOf)){
			
			ClassCastException cce = new ClassCastException(String.format("%s cannot be cast to %s", RefUtils.formatTypeList(decTypes), RefUtils.formatTypeList(isInstanceOf) ));
			StackTraceElement[] es = cce.getStackTrace();
			StackTraceElement[] esnew = new StackTraceElement[es.length-5];
			System.arraycopy(es, 5, esnew, 0, es.length-5);
			cce.setStackTrace(esnew);
			throw cce;//TODO: macros
		}
		
		//new ClassCastException(String.format("%s set %s ", RefUtils.formatTypeList(decTypes), RefUtils.formatTypeList(isInstanceOf))).printStackTrace();
		
	}
	
	public final static boolean isGenericInstnaceof(Object passed, Class<?>[] isInstanceOf) throws Throwable{
		if(null == passed){ return false; } 
		return isGenericInstnaceof( RefUtils.extractType(passed) , isInstanceOf);
	}
	
	
	public final static void assertisGenericInstnaceof(Class<?>[] decTypes, Class<?>[] isInstanceOf) throws ClassCastException{
		if(!isGenericInstnaceof(decTypes, isInstanceOf)){
			ClassCastException cce = new ClassCastException(String.format("%s cannot be cast to %s", RefUtils.formatTypeList(decTypes), RefUtils.formatTypeList(isInstanceOf) ));
			StackTraceElement[] es = cce.getStackTrace();
			StackTraceElement[] esnew = new StackTraceElement[es.length-1];
			System.arraycopy(es, 1, esnew, 0, es.length-1);
			cce.setStackTrace(esnew);
			throw cce;//TODO: macros
		}
	}
	
	public final static boolean isGenericInstnaceof(Class<?>[] decTypes, Class<?>[] isInstanceOf){
		return isGenericInstnaceof(decTypes, isInstanceOf, false);
	}
	
	
	public final static boolean isGenericInstnaceof(Class<?>[] decTypes, Class<?>[] isInstanceOf, boolean precise){
		Class<?> declInst = decTypes[0];		
		Class<?> instofInst = isInstanceOf[0];		
		
		//if decl types contains a null e.g. series := new ArrayList[int]() - then we cannot check this properly, just truncate everything after the null
		/*for(int n = 0; n < decTypes.length; n++){
			if(decTypes[n] == null){
				Class<?>[] decTypesnew = new Class<?>[n];
				Class<?>[] isInstanceOfnew = new Class<?>[n];
				System.arraycopy(decTypes, 0, decTypesnew, 0, n);
				System.arraycopy(isInstanceOf, 0, isInstanceOfnew, 0, n);
				decTypes = decTypesnew;
				isInstanceOf = isInstanceOfnew;
				break;
			}
		}*/
		
		//System.out.println(String.format("declInst: %s, instofInst: %s: precise: %s", Stringifier.stringify(decTypes), Stringifier.stringify(isInstanceOf), precise));
		
		if(instofInst==null || declInst==null){
			return false;
		}
		
		if(precise ? instofInst!=declInst: !instofInst.isAssignableFrom(declInst)){//ensure first element is subtype
			return false;
		}

		Class<?> me = declInst;
		HashMap<String, Type[]> unboundNameToBinding = null;
		HashMap<String, Type[]> origBindings = findTypeOffsets(me,  decTypes);
		if(null == origBindings){
			return false;
		}
		
		boolean toInterface = instofInst.isInterface();//shift up concrete classes until one gets to the interface
		
		HashSet<Type> ifacetriedAlready = toInterface?new HashSet<Type>():null;
		
		while(instofInst != me){
			Type gotoInterface = null;
			if(toInterface ){
				Type[] ifaces = me.getGenericInterfaces();
				if(null != ifaces){
					LinkedList<Type> ifacesq = new LinkedList<Type>();
					for(Type iface : ifaces){
						ifacesq.add(iface);
						ifacetriedAlready.add(iface);//avoid cycles
					}
					
					while(!ifacesq.isEmpty()) {
						Class<?> ifaceRawclass ;
						Type iface = ifacesq.remove(0);
						if(iface instanceof ParameterizedType){
							ifaceRawclass = (Class<?>) ((ParameterizedType)iface).getRawType();
						}
						else{
							ifaceRawclass = (Class<?>)iface;
						}
						
						if(ifaceRawclass == instofInst){
							gotoInterface=iface;
							break;
						}else {
							Type[] ifacesx = ifaceRawclass.getGenericInterfaces();
							if(null != ifacesx){
								for(Type ifacex : ifacesx){
									if(!ifacetriedAlready.contains(ifacex)) {
										ifacetriedAlready.add(ifacex);
										ifacesq.add(ifacex);
									}
									
								}
							}
						}
					}
					
					
				}
			}
			Type parent = gotoInterface!=null?gotoInterface:me.getGenericSuperclass();//check above means must be subtype somehow
			
			HashMap<String, Type[]> unboundNameToBindingPrev = unboundNameToBinding;
			unboundNameToBinding = new HashMap<String, Type[]>();
			
			if(parent instanceof ParameterizedType){//has qualified supertypes in parent Me[Z] extends Parent[Z, String]
				ParameterizedType sup = (ParameterizedType)parent;
				Type[] qualifiedGenericSupers = sup.getActualTypeArguments();
				TypeVariable[] supParams = ((Class<?>) sup.getRawType()).getTypeParameters();
				
				for(int n=0; n <qualifiedGenericSupers.length;n++){
					Type bindingonExtendsDef = qualifiedGenericSupers[n];//extends SupType[x, String]//String is a class
					String bindToname =  ((TypeVariable)supParams[n]).getName();
					if(bindingonExtendsDef instanceof Class){
						unboundNameToBinding.put(bindToname, new Class<?>[]{(Class<?>)bindingonExtendsDef});
					}
					else if(bindingonExtendsDef instanceof ParameterizedType){
						//qualify
						ArrayList<Type> inplace = new ArrayList<Type>();
						qualifyGenerics(bindingonExtendsDef, (null == unboundNameToBindingPrev ? origBindings:unboundNameToBindingPrev), inplace);
						
						unboundNameToBinding.put(bindToname, inplace.toArray(new Type[0]));
					}
					else{
						//see if it was qualified already 'lower' down in the stack
						Type[] found = (null == unboundNameToBindingPrev ? origBindings:unboundNameToBindingPrev).get(bindingonExtendsDef.getTypeName());
						if(found != null){
							unboundNameToBinding.put(bindToname, found);
						}
					}
				}
			}
			
			me = gotoInterface!=null?instofInst:me.getSuperclass();
			if(instofInst == me){
				break;
			}
		}
		
		TypeVariable<?>[] supAgs = me.getTypeParameters();
		if(supAgs.length > 0){
			//lockedInPrecise=true;//TODO: has generic so ensure that each leg is matched precisly - actually like this for all but single element things so remove!
			
			//splice up into new decList
			ArrayList<Class<?>> newDecs = new ArrayList<Class<?>>();
			newDecs.add(me);
			for(int n=0; n < supAgs.length; n++){
				//create a new fully qualified list
				String genTName= supAgs[n].getName();

				/*System.out.println("" + genTName);
				System.out.println("" + origBindings);
				System.out.println("" + unboundNameToBinding);*/
				
				
				for(Type t : (unboundNameToBinding == null?origBindings:unboundNameToBinding).get(genTName)){
					//System.out.println("add: " + t);
					newDecs.add((Class<?>)t);
				}
			}
			decTypes = newDecs.toArray(new Class<?>[0]);

			//now do comparison on whole thing
			if(decTypes.length != isInstanceOf.length){
				boolean hasNull = false;
				for(Type t : decTypes){
					if(null==t){
						hasNull=true;
						break;
					}
				}
				//skip this check if the decl list contains a null - meaning that we cannot correctly check the generic types due to earasure
				if(!hasNull){
					//System.out.println(String.format("on noes! l %s -> l %s", decTypes.length, isInstanceOf.length));
					//System.out.println(String.format("declInst: %s, instofInst: %s", Stringifier.stringify(decTypes), Stringifier.stringify(isInstanceOf)));
					return false;
				}
			}
			
			int slotsInRef = -1;//if within this then we do permit subtypes for generic params, otherwise we do not, so local int < local object, but list str !< list obj
			int len = decTypes.length;
			for(int n=0; n < len; n++){
				Class<?> dec = decTypes[n];		
				Class<?> ins = isInstanceOf[n];	
				//System.out.println(String.format("check %s -> %s", dec, ins));
				//if(dec==null ||  ins==null || (ins != (dec) && (dec.isAssignableFrom(ins))) ){
				if(  ins==null){//dec==null ||
					//System.out.println(String.format("fail as null %s -> %s", dec, ins));
					return false;
				}
				else if (dec==null){
					//skip over nulls because this is beyond our ability to check - java just erases the type at runtime
				}
				else{
					
					if(slotsInRef>0 || slotsInRef==-1){
						if(!(ins.isAssignableFrom(dec))){
							//System.out.println(String.format("fail as not sub but inside ref %s -> %s", dec, ins));
							return false;
						}
					}
					else{//must be 1:1 match
						if(ins != dec){
						//if(!(ins.isAssignableFrom(dec))){
							//System.out.println(String.format("fail as need match %s -> %s", dec, ins));
							return false;
						}
					}
				}
				if(slotsInRef==-1){
					slotsInRef++;
				}
				
				if(Ref.class.isAssignableFrom(ins) || ReifiedType.class.isAssignableFrom(ins)){//TODO: support ref array
					slotsInRef += ins.getTypeParameters().length;
				}
				
			}
			return true;
		}
		else{
			return true;
		}
	}
	
	private static void qualifyGenerics(Type x, HashMap<String, Type[]> qualifciation, ArrayList<Type> inplace){
		//e.g. extends Fella<HashSet<X>>{}
		if(x instanceof ParameterizedType)
		{
			ParameterizedType asp = (ParameterizedType)x;
			inplace.add(asp.getRawType());
			
			for(Type ele : asp.getActualTypeArguments()){
				if(ele instanceof TypeVariable){
					String name = ((TypeVariable<?>)ele).getName();
					Type[] boundTo = qualifciation.get(name);
					if(null != boundTo){
						for(Type mate : boundTo){
							inplace.add(mate);
						}
					}
					else{
						inplace.add(ele);
					}
				}
				else{
					qualifyGenerics(ele, qualifciation, inplace);
				}
			}
		}
		else{
			inplace.add(x);//could be anything, most likely a class, other stuff will need to be filled in from class[] definition provided
		}
	}
	
	private static int slotsConsumed(Class<?> mea, Class<?>[] decTypes, int decLenCur){//fully qualified type
		int ret = 1;
		if(decLenCur>decTypes.length-1){
			return -1;//oops
		}
		
		if(mea==null){
			return ret;
		}
		
		for(TypeVariable<?> qn : mea.getTypeParameters()){
			int i = slotsConsumed(decTypes[decLenCur+ret], decTypes, decLenCur+ret);
			if(i == -1){ return -1;}
			ret += i;
		}
		
		return ret;
	}
	
	private static HashMap<String,Type[]> findTypeOffsets(Class<?> me, Class<?>[] decTypes){
		HashMap<String,Type[]> origBindingOffsetLocation = new HashMap<String, Type[]>();
		int decLenCur=1;
		for(TypeVariable<?> tv : me.getTypeParameters()){
			int startLoc = decLenCur;
			
			if(decLenCur >= decTypes.length){
				return null;//oops off the end, malformed list
			}
			
			int cons = slotsConsumed(decTypes[decLenCur], decTypes, decLenCur);
			if(cons==-1){
				return null;//failure
			}
			decLenCur += cons;
			
			Class<?>[] typeDef = new Class<?>[cons];
			System.arraycopy(decTypes, startLoc, typeDef, 0, cons);
			
			origBindingOffsetLocation.put(tv.getName(), typeDef);
		}
		
		if(decLenCur != decTypes.length){
			return null;//definition list is wrong length as qualification
		}
		
		return origBindingOffsetLocation;
	}
}
