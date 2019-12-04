package com.concurnas.compiler.visitors.util;

import java.util.ArrayList;
import java.util.stream.Collectors;

import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.GenericType;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.PrimativeType;
import com.concurnas.compiler.ast.PrimativeTypeEnum;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.visitors.TypeCheckUtils;

/** Used for encoding and decoding trait fields held within annotations (and not part of the class definition)
	Y -> TY:Ljava/lang/Object<>;
	Number -> Ljava/lang/Number<>;
	ArrayList<X> -> Ljava/util/ArrayList<TX:Ljava/lang/Number;>;
	int -> I
	int[2] -> [[I
 */
public class TraitFieldEncoderDecoder {

	public static String encode(Type type) {
		if(type == null) {
			return "";
		}
		
		if(type instanceof FuncType) {
			//type 
			type = TypeCheckUtils.convertfuncTypetoNamedType(type, null);
		}
		
		StringBuilder ret = new StringBuilder();
		
		if(type.hasArrayLevels()) {
			type = (Type)type.copy();
			for(int n=0; n< type.getArrayLevels(); n++){
				ret.append("[");
			}
			type.setArrayLevels(0);
		}
		
		if(type instanceof PrimativeType) {
			ret.append(type.getBytecodeType());
		}else if(type instanceof NamedType) {
			NamedType asNamed = (NamedType)type;
			ClassDef cd = asNamed.getSetClassDef();
			if(null == cd) {
				return null;
			}
			ret.append("L" + cd.bcFullName() );
			ArrayList<Type> gens = asNamed.getGenericTypeElements();
			ret.append("<");
			if(!gens.isEmpty()) {
				ret.append(String.join("", gens.stream().map(a -> encode(a)).collect(Collectors.toList())) );
			}
			ret.append(">;");
		}else if(type instanceof GenericType) {
			GenericType gt = (GenericType)type;
			ret.append("T" + gt.name + ":" +  (gt.upperBound == null? "java/lang/Object" : encode(gt.upperBound)));
		}
		
		return ret.toString();
	}

	public static Type decode(String field) {
		return decode(new PositionHolder(), field);
	}
	
	private static class PositionHolder{
		public int pos = 0;
	}
	
	private static Type decode(PositionHolder ph, String field) {
		
		char c = field.charAt(ph.pos);
		
		int arrayLevels = 0;
		while(c == '[') {
			arrayLevels++;
			c = field.charAt(++ph.pos);
		}
		
		Type ret = null;
		
		switch(c) {
			case 'B': ret = new PrimativeType(PrimativeTypeEnum.BYTE); break;
		    case 'C': ret = new PrimativeType(PrimativeTypeEnum.CHAR); break;
		    case 'D': ret = new PrimativeType(PrimativeTypeEnum.DOUBLE); break;
		    case 'F': ret = new PrimativeType(PrimativeTypeEnum.FLOAT); break;
		    case 'I': ret = new PrimativeType(PrimativeTypeEnum.INT); break;
		    case 'J': ret = new PrimativeType(PrimativeTypeEnum.LONG); break;
		    case 'S': ret = new PrimativeType(PrimativeTypeEnum.SHORT); break;
		    case 'Z': ret = new PrimativeType(PrimativeTypeEnum.BOOLEAN); break;
		}
		
		if(null == ret) {
			if(c == 'T') {//generic Type. e.g. TY:Ljava/lang/Object<>;
				int start = ++ph.pos;
				c = field.charAt(ph.pos);
				while(c != ':') {
					c = field.charAt(++ph.pos);
				}
				String genName = field.substring(start, ph.pos++);
				NamedType upeprbound = (NamedType)decode(ph, field);
				GenericType retx = new GenericType(0, 0, genName, 0);
				retx.upperBound = upeprbound;
				ret = retx;
			}else if(c == 'L') {//NamedType
				int start = ++ph.pos;
				c = field.charAt(ph.pos);
				while(c != '<') {
					c = field.charAt(++ph.pos);
				}
				String namedT = field.substring(start, ph.pos);
				
				ArrayList<Type> genTypes = new ArrayList<Type>();
				c = field.charAt(++ph.pos);
				while(c != '>') {
					Type genComp = decode(ph, field);
					genTypes.add(genComp);
					c = field.charAt(++ph.pos);
				}
				++ph.pos;//;
				ret = new NamedType(0, 0, namedT, genTypes);
			}
			
		}
		
		if(arrayLevels > 0) {
			ret.setArrayLevels(arrayLevels);
		}
		
		
		return ret;
	}
	
}


