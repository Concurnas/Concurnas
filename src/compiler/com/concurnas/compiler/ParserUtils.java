package com.concurnas.compiler;

import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.Type;

public class ParserUtils {

	public static Type createObjectRef(int line, int col, int refCnt){
		 Type taa = new NamedType(line, col, new ClassDefJava(Object.class));
         for(int na=0; na < refCnt; na++){
           taa = new NamedType(line, col, taa);
         }
         return taa;
	}
}
