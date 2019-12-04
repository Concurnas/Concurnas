package com.concurnas.runtime;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class BytecodeGenUtils {

	public static void intOpcode(MethodVisitor mv, int input)
	{//TODO: nasty copy paste
		if(input ==-1)
		{
			mv.visitInsn(Opcodes.ICONST_M1);
		}
		else if(input > -1 && input <= 5)
		{
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
		}
		else if( -128 <= input && input <= 127)
		{//8 bit
			mv.visitIntInsn(Opcodes.BIPUSH, input);
		}
		else if( -32768 <= input && input <= 32767)
		{//16 bit
			mv.visitIntInsn(Opcodes.SIPUSH, input);
		}
		else
		{//32 bit - ldc
			mv.visitLdcInsn(new Integer(input));
		}
	}
	
	public static void longOpcode(MethodVisitor mv, long input)
	{
		if(input ==0l)
		{
			mv.visitInsn(Opcodes.LCONST_0);
		}
		else if(input == 1l)
		{
			mv.visitInsn(Opcodes.LCONST_1);
		}
		else
		{
			mv.visitLdcInsn(new Long(input));
		}
	}
	
	public static void floatOpcode(MethodVisitor mv, float input)
	{
		if(input == 0.0f)
		{
			mv.visitInsn(Opcodes.FCONST_0);
		}
		else if(input == 1.0f)
		{
			mv.visitInsn(Opcodes.FCONST_1);
		}
		else if(input == 2.0f)
		{
			mv.visitInsn(Opcodes.FCONST_2);
		}
		else
		{
			mv.visitLdcInsn(new Float(input));
		}
	}
	
	public static void doubleOpcode(MethodVisitor mv, double input)
	{
		if(input == 0.0)
		{
			mv.visitInsn(Opcodes.DCONST_0);
		}
		else if(input == 1.0)
		{
			mv.visitInsn(Opcodes.DCONST_1);
		}
		else
		{
			mv.visitLdcInsn(new Double(input));
		}
	}
	
}
