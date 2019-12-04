package com.concurnas.runtime;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class CodeGenUtils {
	
    static void intOpcode(MethodVisitor mv, int input)
	{
		if(input ==-1){
			mv.visitInsn(Opcodes.ICONST_M1);
		} else if(input >= 0 && input <= 5) {
			int res = Opcodes.ICONST_5;
			switch(input)
			{
				case 0: res =  Opcodes.ICONST_0; break;
				case 1: res =  Opcodes.ICONST_1; break;
				case 2: res =  Opcodes.ICONST_2; break;
				case 3: res =  Opcodes.ICONST_3; break;
				case 4: res =  Opcodes.ICONST_4; break;
			}
			mv.visitInsn(res);
		} else if( -128 <= input && input <= 127){//8 bit
			mv.visitIntInsn(Opcodes.BIPUSH, input);
		} else if( -32768 <= input && input <= 32767){//16 bit
			mv.visitIntInsn(Opcodes.SIPUSH, input);
		} else{//32 bit - ldc
			mv.visitLdcInsn(new Integer(input));
		}
	}

}
