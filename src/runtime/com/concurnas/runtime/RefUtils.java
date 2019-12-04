package com.concurnas.runtime;

import com.concurnas.bootstrap.runtime.ReifiedType;
import com.concurnas.bootstrap.runtime.ref.DirectlyGettable;
import com.concurnas.bootstrap.runtime.ref.LocalArray;
import com.concurnas.bootstrap.runtime.ref.Ref;
import com.concurnas.runtime.ref.Local;

public class RefUtils {
	/**
	 * Type array is expected to already contain known types with nulls correspnding to a point
	 * @param existing - what we've pulled out so far
	 * @param slotFromEnd - references a null offset from end of array to fill in with type of object
	 * @param obj - will figure out the type of this. Works even if ref type etc
	 * @param ifNull - type to us if object is null, array means that we can have a ref as an upper bound later on
	 * @return
	 */
	public final static Class<?>[] extractTypeAndAugmentTypeArray(Class<?>[] existing, int slotFromEnd, Object obj, Class<?>[] ifNull){
		Class<?>[] found = obj == null?ifNull:extractType(obj);
		
		Class<?>[] ret = new Class<?>[existing.length + found.length - 1];
		
		System.arraycopy(existing, 0, ret, 0, existing.length - (slotFromEnd+1));//before
		
		System.arraycopy(existing, existing.length-(slotFromEnd), ret,  ret.length-(slotFromEnd), slotFromEnd);//after
		
		System.arraycopy(found, 0, ret, existing.length-(slotFromEnd+1), found.length);//replace middle fella
				
		return ret;
		
	}
	
	public final static Class<?>[] extractType(Object obj){
		//cut and paste ugh!
		Class<?>[] decTypes;
		
		if(obj instanceof Ref || obj instanceof LocalArray){
			boolean isLocal = obj instanceof LocalArray;
			Class<?>[] locTypes = isLocal?((LocalArray<?>)obj).type:((Ref<?>)obj).getType();
			decTypes = new Class<?>[locTypes.length+1];
			decTypes[0] = obj.getClass();//isLocal?Ref.class:LocalArray.class;
			
			System.arraycopy(locTypes, 0, decTypes, 1, locTypes.length);
			return decTypes;//job done! as local has all the information that we need wrapped up already?
		}
		else if(obj instanceof ReifiedType){
			Class<?>[] locTypes = ((ReifiedType)obj).getType();
			decTypes = new Class<?>[locTypes.length+1];
			
			decTypes[0] = obj.getClass();
			
			System.arraycopy(locTypes, 0, decTypes, 1, locTypes.length);
			return decTypes;//job done! as local has all the information that we need wrapped up already?
			
			//locTypes[0] = obj.getClass();
			//return locTypes;
		}
		else{
			Class<?> cls = obj.getClass();
			int typParams = cls.getTypeParameters().length;
			if(typParams > 0){
				decTypes = new Class<?>[typParams+1];
				decTypes[0] = cls;
				
				return decTypes;
			}
			else{
				decTypes = new Class<?>[]{cls};
			}
		}
		
		//fix the component type dependant upon actual type sotred in nested refs 
		
		//Class<?> declComponentType = decTypes[decTypes.length-1];
		//extract actual declType
		
		Object top = obj;
		int na=0;
		while(null!= top && top.getClass().isAssignableFrom(DirectlyGettable.class)){
			top = ((DirectlyGettable)top).get(true);
			na++;
		}
		
		Class<?> declComponentType = null==top?decTypes[decTypes.length-1]:top.getClass();
		
		if(null != top){//overwrite component type (note not always last to cope with HashMap[String, String]!
			
			if(na >= decTypes.length){//e.g. going from Obj: -> obj itself may be a ref, the outer ref would not know this
				//obj: -> [Local, Obj] -> [Local, Local, Obj]
				int refsToAdd = na - (decTypes.length-1);
				int ln = decTypes.length + refsToAdd;
				Class<?>[] newdecTypes = new Class<?>[ln];
				System.arraycopy(decTypes, 0, newdecTypes, 0, decTypes.length-refsToAdd);
				
				int h = decTypes.length-refsToAdd;
				for(int m =0; m < refsToAdd; m++){
					newdecTypes[h++]=Local.class;
				}
				
				decTypes = newdecTypes;
			}
			
			decTypes[na] = declComponentType;
		}
		
		return decTypes;
	}
	
	private final static int formatTypeList(Class<?>[] decTypes, int offset, int stopset, StringBuilder sb){
		int n = offset;
		for(; n < stopset;){
			if(n >= decTypes.length){
				sb.append("#error");
				return n;
			}
			Class<?> cls = decTypes[n];
			n++;
			int consumed = 0;
			if(null == cls){
				sb.append("_");
			}
			else{
				if(cls.equals(Local.class)){
					consumed = formatTypeList(decTypes, n, stopset, sb);
					sb.append(":");
				}
				else{
					sb.append(cls.getName());			
					
					int typParams = cls.getTypeParameters().length;
					if(typParams > 0){
						sb.append("[");
						consumed = formatTypeList(decTypes, n, n+typParams, sb);
						sb.append("]");
						
					}
				}
			}
			
			n += consumed;
			if(!(n >= stopset)){
				sb.append(", ");
			}
		}
		
		return n;
	}
	
	final static String formatTypeList(Class<?>[] decTypes){
		StringBuilder sb = new StringBuilder();
		formatTypeList(decTypes, 0, decTypes.length, sb);
				
		return sb.toString();
	}
	
}
