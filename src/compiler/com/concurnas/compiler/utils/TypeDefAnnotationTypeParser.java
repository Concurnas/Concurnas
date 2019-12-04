package com.concurnas.compiler.utils;

import java.util.ArrayList;

import com.concurnas.compiler.ast.MultiType;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.PrimativeType;
import com.concurnas.compiler.ast.PrimativeTypeEnum;
import com.concurnas.compiler.ast.Type;
import com.concurnas.runtime.cps.analysis.TypeDesc;

/**
 * used for typedef bytecode parsing rhs, e.g. typedef thing<x> = ArraList<HashMap<String, x>> [i.e. the ArraList<HashMap<String, x>> part]
 */
public class TypeDefAnnotationTypeParser {
	//TODO: needs to handle generic upper/lower bounds and wildcards
	private int offset = 0;
	private String typeString;
	
	public TypeDefAnnotationTypeParser(String typeString){
		this.typeString = typeString;
	}
	
	public Type toType() throws InternalError{
		//primative
		//namedtype
		//generic type
		//function type
		
		//"Ljava/util/ArrayList<Ljava/lang/String;>;"
		//<X:Ljava/lang/Object;>Ljava/lang/Object; <upper bounded generic type
		//LMyClass<X:Ljava/lang/Object;>; <upper bounded generic type?s
		
		//TypeDesc.typelen(typeString.toCharArray(), 0);
		String typeString = this.typeString;
		
		Type ret = null;
		MultiType multiType = null;
		
		while(true) {
			switch(typeString.charAt(offset)){
			    case 'L':
			    	int startPoint = ++offset;
			        while (typeString.charAt(offset) != ';' && typeString.charAt(offset) != '<') {offset++;}
			        int endPoint = offset;
			        
			        NamedType retnt = new NamedType(0,0,typeString.substring(startPoint, endPoint).replace('/', '.').replace('$', '.'));
			        
			        if(typeString.charAt(offset++)=='<'){//uh oh, generic params
			        	ArrayList<Type> gens = new ArrayList<Type>();
			        	while (typeString.charAt(offset) != '>'){
			        		Type tt =  toType();
			        		gens.add(tt);
			        	}
			        	retnt.setGenTypes(gens);
			        }
			        ret = retnt;
			        //ret;//off - start;
			        break;
			    case 'B': ret = new PrimativeType(PrimativeTypeEnum.BYTE); break;
			    case 'C': ret = new PrimativeType(PrimativeTypeEnum.CHAR); break;
			    case 'D': ret = new PrimativeType(PrimativeTypeEnum.DOUBLE); break;
			    case 'F': ret = new PrimativeType(PrimativeTypeEnum.FLOAT); break;
			    case 'I': ret = new PrimativeType(PrimativeTypeEnum.INT); break;
			    case 'J': ret = new PrimativeType(PrimativeTypeEnum.LONG); break;
			    case 'S': ret = new PrimativeType(PrimativeTypeEnum.SHORT); break;
			    case 'Z': ret = new PrimativeType(PrimativeTypeEnum.BOOLEAN); break;
			    case 'V': ret = new PrimativeType(PrimativeTypeEnum.VOID); break;
			    case '[':
			    	offset++;
			        Type tt =  toType();
			        tt.setArrayLevels(tt.getArrayLevels()+1);
			        ret= tt;
			        break;
			    default:
			        throw new InternalError(String.format("Unknown descriptor type: %s at position: %s of: %s", typeString.charAt(offset), offset, typeString));
			}
			
			if(null != multiType) {
				multiType.multitype.add(ret);
				offset+= ret instanceof PrimativeType?2:1;
			}
			
			if(offset >= typeString.length()-2) {
				if(multiType != null) {
					ret = multiType;
				}
				break;
			}else if (multiType == null && typeString.charAt(1+offset) == '|') {
				multiType = new MultiType(0, 0, new ArrayList<Type>());
				multiType.multitype.add(ret);
				offset+=2;
			}
			else if (multiType == null){
				return ret;
			}
			//above is a bit messy
		}
		
		return ret;
	}
}
