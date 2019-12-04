package com.concurnas.compiler.bytecode;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.compiler.ast.PrimativeType;
import com.concurnas.compiler.ast.PrimativeTypeEnum;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.TypeCheckUtils;

public class ArraySubListSetterHelper implements Opcodes {

	private static PrimativeType const_Int = new PrimativeType(PrimativeTypeEnum.INT);
	
	public static Label insertIntoArray(BytecodeGennerator bv, Type insertInto, Expression startExpr, Expression rhs, BytecodeGennerator bcv)
	{
		//TODO: this should crash out a = [1,2,3]; a[1:2] = [1,23,4,5,8,9,0,0,4,2,] etc
		BytecodeOutputter mv = bv.bcoutputter;
		Label onComplete = new Label();
		
		//toset array must be on the top of the stack
		String src = bv.getTempVarName();
		int srcSlot = bv.createNewLocalVar(src,  insertInto,  true);//stick temp var to register...
		
		String startInput = bv.getTempVarName();
		if(null != startExpr){
			Type got = (Type)startExpr.accept(bv);
			bcv.donullcheckForUnknown(got, null);
		}
		else{
			mv.visitInsn(ICONST_0);
		}
		int startInputSlot = bv.createNewLocalVar(startInput, const_Int, true);//stick temp var to register...
		
		String insert = bv.getTempVarName();
		Type toInsertType = (Type)rhs.accept(bv);
		TypeCheckUtils.unlockAllNestedRefs(toInsertType);
		int insertSlot = bv.createNewLocalVar(insert, toInsertType,  true);//stick temp var to register...
		
		String n = bv.getTempVarName();
		mv.visitInsn(ICONST_0);
		int nSlot = bv.createNewLocalVar(n,  const_Int, true);//stick temp var to register...
		
		Label l5 = new Label();
		mv.visitJumpInsn(GOTO, l5);
		
		Label l6 = new Label();
		mv.visitLabel(l6);
		mv.visitVarInsn(ALOAD, srcSlot);
		mv.visitVarInsn(ILOAD, startInputSlot);
		mv.visitVarInsn(ILOAD, nSlot);
		mv.visitInsn(IADD);
		mv.visitVarInsn(ALOAD, insertSlot);
		mv.visitVarInsn(ILOAD, nSlot);
		Utils.applyArrayLoad(bv.bcoutputter, toInsertType);//mv.visitInsn(IALOAD);
		Utils.applyArrayStore(bv.bcoutputter, insertInto, bcv);//mv.visitInsn(IASTORE);
		
		//Label l7 = new Label();
		//mv.visitLabel(l7);
		mv.visitIincInsn(nSlot, 1);//n++
		
		mv.visitLabel(l5);
		mv.visitVarInsn(ILOAD, nSlot);
		mv.visitVarInsn(ALOAD, insertSlot);
		mv.visitInsn(ARRAYLENGTH);
		
		mv.visitJumpInsn(IF_ICMPGE, onComplete);
		mv.visitVarInsn(ILOAD, startInputSlot);
		mv.visitVarInsn(ILOAD, nSlot);
		mv.visitInsn(IADD);
		mv.visitVarInsn(ALOAD, srcSlot);
		mv.visitInsn(ARRAYLENGTH);
		mv.visitJumpInsn(IF_ICMPLT, l6);
		return onComplete;
	}

	/*
	int[] src = {1,2,3,4};
	int srtartInput = 1;
	
	int[] insert = {1,2,3};
	
	for(int n = 0; n < insert.length && (srtartInput+n) < src.length; n++)
	{
		src[srtartInput+n] = insert[n];
	}
	*/
	
	/*
	* Label l0 = new Label();
	mv.visitLabel(l0);
	mv.visitLineNumber(13, l0);
	mv.visitInsn(ICONST_4);
	mv.visitIntInsn(NEWARRAY, T_INT);
	mv.visitInsn(DUP);
	mv.visitInsn(ICONST_0);
	mv.visitInsn(ICONST_1);
	mv.visitInsn(IASTORE);
	mv.visitInsn(DUP);
	mv.visitInsn(ICONST_1);
	mv.visitInsn(ICONST_2);
	mv.visitInsn(IASTORE);
	mv.visitInsn(DUP);
	mv.visitInsn(ICONST_2);
	mv.visitInsn(ICONST_3);
	mv.visitInsn(IASTORE);
	mv.visitInsn(DUP);
	mv.visitInsn(ICONST_3);
	mv.visitInsn(ICONST_4);
	mv.visitInsn(IASTORE);
	mv.visitVarInsn(ASTORE, 1);
	Label l1 = new Label();
	mv.visitLabel(l1);
	mv.visitLineNumber(14, l1);
	mv.visitInsn(ICONST_1);
	mv.visitVarInsn(ISTORE, 2);
	Label l2 = new Label();
	mv.visitLabel(l2);
	mv.visitLineNumber(16, l2);
	mv.visitInsn(ICONST_3);
	mv.visitIntInsn(NEWARRAY, T_INT);
	mv.visitInsn(DUP);
	mv.visitInsn(ICONST_0);
	mv.visitInsn(ICONST_1);
	mv.visitInsn(IASTORE);
	mv.visitInsn(DUP);
	mv.visitInsn(ICONST_1);
	mv.visitInsn(ICONST_2);
	mv.visitInsn(IASTORE);
	mv.visitInsn(DUP);
	mv.visitInsn(ICONST_2);
	mv.visitInsn(ICONST_3);
	mv.visitInsn(IASTORE);
	mv.visitVarInsn(ASTORE, 3);
	Label l3 = new Label();
	mv.visitLabel(l3);
	mv.visitLineNumber(18, l3);
mv.visitInsn(ICONST_0);
mv.visitVarInsn(ISTORE, 4);
	Label l4 = new Label();
	mv.visitLabel(l4);
	Label l5 = new Label();
	mv.visitJumpInsn(GOTO, l5);
	Label l6 = new Label();
	mv.visitLabel(l6);
	mv.visitLineNumber(20, l6);
	mv.visitFrame(Opcodes.F_FULL, 5, new Object[] {"[Ljava/lang/String;", "[I", Opcodes.INTEGER, "[I", Opcodes.INTEGER}, 0, new Object[] {});
	mv.visitVarInsn(ALOAD, 1);
	mv.visitVarInsn(ILOAD, 2);
	mv.visitVarInsn(ILOAD, 4);
	mv.visitInsn(IADD);
	mv.visitVarInsn(ALOAD, 3);
	mv.visitVarInsn(ILOAD, 4);
	mv.visitInsn(IALOAD);
	mv.visitInsn(IASTORE);
	Label l7 = new Label();
	mv.visitLabel(l7);
	mv.visitLineNumber(18, l7);
	mv.visitIincInsn(4, 1);
	mv.visitLabel(l5);
	mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
	mv.visitVarInsn(ILOAD, 4);
	mv.visitVarInsn(ALOAD, 3);
	mv.visitInsn(ARRAYLENGTH);
	Label l8 = new Label();
	mv.visitJumpInsn(IF_ICMPGE, l8);
	mv.visitVarInsn(ILOAD, 2);
	mv.visitVarInsn(ILOAD, 4);
	mv.visitInsn(IADD);
	mv.visitVarInsn(ALOAD, 1);
	mv.visitInsn(ARRAYLENGTH);
	mv.visitJumpInsn(IF_ICMPLT, l6);
	mv.visitLabel(l8);
	mv.visitLineNumber(22, l8);
	*/
}
