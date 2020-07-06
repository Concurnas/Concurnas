package com.concurnas.compiler.bytecode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.compiler.ast.AccessModifier;
import com.concurnas.compiler.ast.Annotations;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.NullStatus;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.utils.Sixple;
import com.concurnas.runtime.Pair;

public class BytecodeOutputter implements Opcodes {
	// TODO: needs a cleanup on release
	private static boolean t = true;
	private static boolean f = false;

	private static Boolean DEBUG_MODE_OVERRIDE = f;
	public static boolean PRINT_OPCODES = DEBUG_MODE_OVERRIDE;

	public FrameSateTrackingMethodVisitor mv = null;
	private Stack<FrameSateTrackingMethodVisitor> prevMV = new Stack<FrameSateTrackingMethodVisitor>();
	public ClassWriter cw;
	private static Boolean DEBUG_MODE = true;

	public BytecodeOutputter(ClassWriter cw, boolean intoDebug) {
		this.cw = cw;
		DEBUG_MODE = intoDebug;
		lastWroteThrow = false;
		lastWriteLabel = false;
		lastWroteRet = false;
	}

	public HashMap<Label, Label> labelOverride = new HashMap<Label, Label>();

	private HashSet<Label> visitAlread = new HashSet<Label>();// TODO: remove this later

	public Label visitLabel(Label lbl) {
		if (null == lbl) {
			throw new RuntimeException("null label passed");
		}

		if (!lastWriteLabel) {// hmm, two in a row, take the first one and ignore this one...
			lbl = labelOverride.containsKey(lbl) ? labelOverride.get(lbl) : lbl;
			lastLabelVisited = lbl;

			if (!visitAlread.contains(lbl)) {
				mv.visitLabel(lbl);
			}

			visitAlread.add(lbl);// JPT: memory leak? fix actual problem?

			if (PRINT_OPCODES) {
				System.out.println("   " + lbl);
			}
		} else {
			if (PRINT_OPCODES) {
				System.out.println("   " + lbl + " <- Fail skip as double in row visit");
			}
		}

		lastWroteThrow = false;
		lastWriteLabel = true;
		lastWroteRet = false;

		return lastLabelVisited;
	}

	public Stack<Label> nextLabel = new Stack<Label>();// TODO: should this be a stack?

	public void pushNextLabel(Label next) {
		if (next == null) {
			throw new RuntimeException("pushNextLabel passed null ref");
		}
		// assert next!=null;

		if (nextLabel.isEmpty() || !nextLabel.peek().equals(next)) {
			// dont add more than one
			nextLabel.push(next);
		}

		lastWroteThrow = false;
		lastWriteLabel = false;
		lastWroteRet = false;
	}

	private void consumeNextLabelIfExists() {
		if (!this.nextLabel.isEmpty()) {
			visitLabel(nextLabel.pop());
		}
		lastWroteThrow = false;
		lastWriteLabel = false;
		lastWroteRet = false;
	}

	public Label lastLabelVisited = null;// JPT: refacor out this should not be here
	//private int lastOutputLine = -10;

	public void visitNewLine(int line) {
		if (null == mv) {
			return;
		} else {
			Label label;
			if (!nextLabel.isEmpty()) {
				label = nextLabel.pop();
				/*
				 * if(null == label){ label = new Label(); }
				 */
			} else {
				label = new Label();
			}

			label = visitLabel(label);

			//if (line != lastOutputLine) {
				this.mv.visitLineNumber(line, label);
				if (PRINT_OPCODES) {
					System.out.println(String.format("    LINENUMBER %s %s", line, label));
				}
				
				//lastOutputLine = line;
			//}
		}
		lastWroteThrow = false;
		lastWriteLabel = false;
		lastWroteRet = false;
	}

	///////////

	////////
	private FrameSateTrackingMethodVisitor getMethodVisitor(String methodName, ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> inputs, Type returnType, boolean isAtModuleLevel, boolean isAbstract, boolean isFinal, AccessModifier am, int extraModifiers, String classname, ArrayList<Pair<String, NamedType>> methodGenricList) {// TODO: refactor this to take inputs as: ArrayList<Type> inputs
		int access = ACC_PUBLIC;

		if (am == AccessModifier.PRIVATE) {
			access = ACC_PRIVATE;
		} else if (am == AccessModifier.PROTECTED) {
			access = ACC_PROTECTED;
		} else if (am == AccessModifier.PACKAGE) {
			access = 0;
		}

		if (isAtModuleLevel) {// at class level just public, at module leve it's static
			access += ACC_STATIC;
		}

		if (isAbstract) {
			access += ACC_ABSTRACT;
		}

		if (isFinal) {
			access += ACC_FINAL;
		}

		access += extraModifiers;

		StringBuilder sigNormalSB = new StringBuilder();
		StringBuilder sigGenericSB = new StringBuilder();

		sigNormalSB.append("(");

		StringBuilder localGenericUpperBoundPrevix = null;
		// for sigGenericSB, prefix with local generic upper bound binding
		if (methodGenricList != null && !methodGenricList.isEmpty()) {
			localGenericUpperBoundPrevix = new StringBuilder();
			for (Pair<String, NamedType> localGen : methodGenricList) {
				// localGenericUpperBoundPrevix.append(localGen+":Ljava/lang/Object;");
				String upBound = localGen.getA();
				NamedType upperBound = localGen.getB();
				if (null != upperBound) {
					upBound += ":" + upperBound.getBytecodeType();
				} else {
					upBound += ":Ljava/lang/Object;";
				}
				localGenericUpperBoundPrevix.append(upBound);
			}
		}

		sigGenericSB.append("(");
		if (inputs != null) {
			for (Sixple<Type, String, Annotations, Boolean, Boolean, Boolean> arg : inputs) {
				Type tt = arg.getA();
				if (tt instanceof FuncType) {
					((FuncType) tt).usedInMethodDesc = true;
				}

				sigNormalSB.append(tt.getBytecodeType());
				sigGenericSB.append(tt.getGenericBytecodeType());
			}
		}

		if (returnType instanceof FuncType) {
			((FuncType) returnType).usedInMethodDesc = true;
		}

		sigNormalSB.append(")");
		sigGenericSB.append(")");
		// "()Lcom/concurnas/bootstrap/runtime/ref/LocalArray<[Lcom/concurnas/runtime/ref/Local<[I>;>;"
		sigNormalSB.append(returnType.getBytecodeType());
		
		if(returnType instanceof NamedType) {
			((NamedType)returnType).fromisWildCardAny = false;
		}
		
		Type ret = (Type)returnType.copy();
		ret.setInOutGenModifier(null);
		
		sigGenericSB.append(ret.getGenericBytecodeType());

		String sigNormalString = sigNormalSB.toString();

		String sigGenericString;
		if (null == localGenericUpperBoundPrevix) {
			sigGenericString = sigGenericSB.toString();
		} else {
			sigGenericString = "<" + localGenericUpperBoundPrevix.toString() + ">" + sigGenericSB.toString();
		}

		if (sigGenericString.equals(sigNormalString)) {
			sigGenericString = null;
		}

		// mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "getit",
		// "()Lcom/concurnas/bootstrap/lang/Lambda$Function1;",
		// "()Lcom/concurnas/bootstrap/lang/Lambda$Function1<Ljava/lang/Integer;Ljava/lang/String;>;",
		// null);
		// mv = cw.visitMethod(ACC_PRIVATE + ACC_STATIC, "vv",
		// "(Ljava/util/ArrayList;)V", "(Ljava/util/ArrayList<Ljava/lang/String;>;)V",
		// null);
		lastWroteThrow = false;
		lastWriteLabel = false;
		lastWroteRet = false;
		if (PRINT_OPCODES) {
			System.out.println("\ndef " + methodName + sigNormalString + "==>");
		}

		//methodVisitor = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "asd", "()Ljava/lang/Integer;", "<X:Ljava/lang/Integer;>()TX;", null);

		
		return new FrameSateTrackingMethodVisitor(cw.visitMethod(access, methodName, sigNormalString, sigGenericString, null), access, sigNormalString, classname);
	}
	
	
	private static void processType(Type atype, ArrayList<Boolean> ret) {
		if(null == atype) {
			return;//why null?
		}
		
		if(atype.hasArrayLevels()) {
			//HAS ARRAY LEVELS
			atype.getNullStatusAtArrayLevel().forEach(a -> ret.add(a == NullStatus.NULLABLE));
		}
		
		if(atype instanceof NamedType) {
			NamedType asNamed = (NamedType)atype;
			asNamed.getGenericTypeElements().forEach(a -> processType(a, ret));
		}else if(atype instanceof FuncType) {
			FuncType asft = (FuncType)atype;
			asft.inputs.forEach(a -> processType(a, ret));
			processType(asft.retType, ret);
		}
		
		ret.add(atype.getNullStatus() == NullStatus.NULLABLE);
	}

	public static boolean[] makeNullStatusAnnot(List<Type> inputs, Type returnType) {
		ArrayList<Boolean> ret = new ArrayList<Boolean>();
		if(null != inputs) {
			for(Type inp : inputs) {
				processType(inp, ret);
			}
		}
		
		if(null != returnType) {
			processType(returnType, ret);
		}
		
		int n=0;
		int lastTrue=-1;
		for(boolean item : ret) {
			if(item) {
				lastTrue=n;
			}
			n++;
		}
		
		if(lastTrue == -1) {
			return new boolean[0];
		}
		
		boolean[] retx = new boolean[lastTrue+1];
		for(n=0; n <=lastTrue; n++) {
			retx[n] = ret.get(n);
			
		}
		return retx;
	}
	
	public static void addNullablefieldAnnotation(AnnotationVisitor av0, boolean[] nullstatus) {
		AnnotationVisitor av1 = av0.visitArray("nullable");
		for(boolean item : nullstatus) {
			av1.visit(null, item?Boolean.TRUE:Boolean.FALSE);
		}
		av1.visitEnd();
	}
	
	public MethodVisitor enterMethod(String name, ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> inputs, Type returnType, boolean isAtModuleLevel, boolean isAbstract, boolean isFinal, AccessModifier am, int extraModifiers, String classname, Annotations annots, boolean isOverride, ArrayList<Pair<String, NamedType>> methodGenricList) {
		if (mv != null) {
			prevMV.push(mv);
		}

		extraModifiers += Utils.getAnnotationDependantExtraModifiers(annots);

		mv = this.getMethodVisitor(name, inputs, returnType, isAtModuleLevel, isAbstract, isFinal, am, extraModifiers, classname, methodGenricList);

		if (null != annots) {
			annots.bcVisitor.visit(annots);// hack in the bc visitor
		}

		if (isOverride) {
			AnnotationVisitor av0 = mv.visitAnnotation("Ljava/lang/Override;", true);
			av0.visitEnd();
		}
		
		{
			AnnotationVisitor av0 = mv.visitAnnotation("Lcom/concurnas/lang/internal/NullStatus;", true);
			BytecodeOutputter.addNullablefieldAnnotation(av0, makeNullStatusAnnot(inputs.stream().map(a -> a.getA()).collect(Collectors.toList()), returnType));
			av0.visitEnd();
		}
		
		
		int n = 0;
		for (Sixple<Type, String, Annotations, Boolean, Boolean, Boolean> arg : inputs) {
			Annotations anots = arg.getC();
			if (null != anots) {
				anots.bcVisitor.visit(anots);
			}
			String namexx = arg.getB();
			if (null != namexx) {
				AnnotationVisitor av0 = mv.visitParameterAnnotation(n, "Lcom/concurnas/lang/ParamName;", true);
				av0.visit("name", namexx);
				if (arg.getD()) {
					av0.visit("hasDefaultValue", true);
				}

				if (arg.getE()) {
					av0.visit("isVararg", true);
				}

				av0.visitEnd();
			}
			if(arg.getF()) {//shared
				AnnotationVisitor av0 = mv.visitParameterAnnotation(n, "Lcom/concurnas/lang/Shared;", true);
				av0.visitEnd();
			}
			
			n++;
		}
		
		

		mv.visitCode();
		lastWroteThrow = false;
		lastWriteLabel = false;
		lastWroteRet = false;
		return mv;
	}

	public void enterMethod(int access, String name, String desc, String signature, String[] exceptions, String classname) {
		if (mv != null) {
			prevMV.push(mv);
		}
		mv = new FrameSateTrackingMethodVisitor(cw.visitMethod(access, name, desc, signature, exceptions), access, desc, classname);
		lastWroteThrow = false;
		lastWriteLabel = false;
		lastWroteRet = false;
		mv.visitCode();
	}

	public void exitMethod() {
		try {
			if (!DEBUG_MODE && !DEBUG_MODE_OVERRIDE) {
				mv.visitMaxs(1, 0);
			}
		} catch (Throwable t) {
			throw t;
		}
		mv.visitEnd();
		if (!prevMV.isEmpty()) {
			mv = prevMV.pop();
		} else {
			mv = null;
		}
		lastWroteThrow = false;
		lastWriteLabel = false;
		lastWroteRet = false;
		this.nextLabel = new Stack<Label>();// JPT: is this reset correct?
		if (PRINT_OPCODES) {
			System.out.println("\n\n");
		}
	}

	// methodvisitor functions...

	private boolean lastWroteThrow = false;// JPT: refactgor these out one day, bad hack for try catch
	private boolean lastWriteLabel = false;
	private boolean lastWroteRet = false;

	public void visitInsn(int dcmpl) {

		/*
		 * if(Opcodes.ARRAYLENGTH ==dcmpl){ System.err.println("visitInsn ACONST_NULL"
		 * ); }
		 */

		/*
		 * if(Opcodes.DUP2 ==dcmpl){ System.err.println("visitInsn DUP2" ); }
		 */

		/*
		 * if(Opcodes.DUP2 ==dcmpl){ System.err.println("visitInsn DUP2" ); }
		 */

		/*
		 * if(Opcodes.DUP2 ==dcmpl){ System.err.println("visitInsn POP" ); } else
		 * if(Opcodes.POP2 ==dcmpl){ System.err.println("visitInsn POP2" ); }
		 */

		/*
		 * if (Opcodes.ARRAYLENGTH == dcmpl) { System.err.println("visitInsn AASTORE");
		 * }
		 */

		consumeNextLabelIfExists();
		lastWroteThrow = dcmpl == Opcodes.ATHROW;
		lastWriteLabel = false;

		lastWroteRet = dcmpl >= 172 && dcmpl <= 177;

		if (PRINT_OPCODES) {
			System.out.println("    " + opcodeToBytecode.get(dcmpl));
		}
		this.mv.visitInsn(dcmpl);

	}

	public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
		if (PRINT_OPCODES) {
			System.out.println(String.format("param annotation %s, %s, %s", parameter, desc, visible));
		}
		return mv.visitParameterAnnotation(parameter, desc, visible);
	}

	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if (PRINT_OPCODES) {
			System.out.println("@" + desc + " [" + visible + "]");
		}
		return mv.visitAnnotation(desc, visible);
	}

	public void visitJumpInsn(int ifne, Label ifFalse) {
		if (!((lastWroteThrow || lastWroteRet) && ifne == Opcodes.GOTO)) {
			consumeNextLabelIfExists();
			if (null == ifFalse) {
				throw new RuntimeException("null label");
			}
			lastWroteThrow = false;
			lastWriteLabel = false;
			lastWroteRet = false;
			Label togo = labelOverride.containsKey(ifFalse) ? labelOverride.get(ifFalse) : ifFalse;
			if (PRINT_OPCODES) {
				System.out.println("    " + opcodeToBytecode.get(ifne) + " " + togo);
			}
			this.mv.visitJumpInsn(ifne, togo);
			// int t = 8;
		} // skip otherwise
	}

	public void visitIntInsn(int bipush, int inputa) {

		consumeNextLabelIfExists();
		lastWroteThrow = false;
		lastWriteLabel = false;
		lastWroteRet = false;
		this.mv.visitIntInsn(bipush, inputa);
		if (PRINT_OPCODES) {
			System.out.println("    " + opcodeToBytecode.get(bipush) + " " + inputa);
		}
	}

	public void visitLdcInsn(Integer integer) {
		consumeNextLabelIfExists();
		lastWroteThrow = false;
		lastWriteLabel = false;
		lastWroteRet = false;
		
		if (PRINT_OPCODES) {
			System.out.println("    LDC " + integer);
		} // TODO: remove all this PRINT_OPCODES
		this.mv.visitLdcInsn(integer);
	}

	public AnnotationVisitor visitAnnotationDefault() {
		return mv.visitAnnotationDefault();
	}

	public void visitLdcInsn(Object long1) {
		consumeNextLabelIfExists();
		lastWroteThrow = false;
		lastWriteLabel = false;
		lastWroteRet = false;

		if (PRINT_OPCODES) {
			System.out.println("    LDC " + long1 + "");
		}
		this.mv.visitLdcInsn(long1);
	}

	public void visitTypeInsn(int checkcast, String genType) {
		// if(checkcast == CHECKCAST &&
		// genType.equals("com/concurnas/bootstrap/lang/Lambda$Function0")){
		// CHECKCAST java/lang/Integer
		// CHECKCAST xbytecodeSandbox$localClassDef$0$ClassRefIface
		// CHECKCAST bytecodeSandbox$Outer$MyEnum
		// NEW com/concurnas/bootstrap/runtime/ref/LocalArray
		// NEW, "java/lang/StringBuilder"
		// ANEWARRAY
		// CHECKCAST com/concurnas/bootstrap/runtime/ref/LocalArray
		// NEW com/concurnas/runtime/ref/LocDUP
		// CHECKCAST java/lang/Integer
		// NEW com.myorg.code$$Lambda0
		//NEW com/concurnas/runtime/ref/Local
		//CHECKCAST com/concurnas/bootstrap/lang/Lambda$Function1
		//NEW com/concurnas/bootstrap/runtime/CopyTracker
		//CHECKCAST java/lang/Integer
		//NEW java/lang/Object
		//CHECKCAST com/concurnas/runtime/ref/Local

		if(checkcast == ANEWARRAY && genType.equals("java/lang/Class")) {
			int h=9;
		}
		
		
		if (!(checkcast == CHECKCAST && genType.equals("java/lang/Object"))) {
			// everything is already an object so this represents a nop operation
			consumeNextLabelIfExists();
			lastWroteThrow = false;
			lastWroteRet = false;
			if (PRINT_OPCODES) {
				System.out.println("    " + opcodeToBytecode.get(checkcast) + " " + genType);
			}
			this.mv.visitTypeInsn(checkcast, genType);
		}
	}

	public void visitVarInsn(int lstore, int space) {
		/*
		 * if(space==4 && lstore==Opcodes.ALOAD) {
		 * System.out.println("did a backet case"); }
		 */
		
		  if(space == 2){ 
			  int j=8; }
		 
		// ALOAD 0
		// ALOAD 9
		// ASTORE 2
		// ISTORE 1
		// ILOAD 5
		consumeNextLabelIfExists();
		lastWroteThrow = false;
		lastWriteLabel = false;
		lastWroteRet = false;
		if (PRINT_OPCODES) {
			System.out.println("    " + opcodeToBytecode.get(lstore) + " " + space);
		}
		this.mv.visitVarInsn(lstore, space);
	}

	public void visitMultiANewArrayInsn(String arName, int i) {
		consumeNextLabelIfExists();
		lastWroteThrow = false;
		lastWriteLabel = false;
		lastWroteRet = false;
		this.mv.visitMultiANewArrayInsn(arName, i);
		if (PRINT_OPCODES) {
			System.out.println("    " + arName + i);
		}
	}

	public void visitFieldInsn(int dowhat, String fullModuleAndClassName, String name, String bytecodeType) {
		// GETFIELD bytecodeSandbox$Myclass.field : I
		// PUTFIELD bytecodeSandbox$NIC$0.ascending : Z
		// PUTFIELD bytecodeSandbox$MyArrayList trasitems [Ljava/lang/String;
		// PUTFIELD bytecodeSandbox$MyTrait1 count I
		// PUTFIELD bytecodeSandbox$Master.thing : I
		// GETSTATIC bytecodeSandbox flipper Lcom/concurnas/runtime/ref/Local;
		// PUTFIELD bytecodeSandbox$SubMyClass f Ljava/lang/String;
		//GETFIELD bytecodeSandbox$bytecodeSandbox$$onChange0$SO.lastVal$n1 : Ljava/lang/Integer;
		//GETSTATIC repl$.a : Lcom/concurnas/runtime/ref/Local;
		//PUTSTATIC bytecodeSandbox.a : Lcom/concurnas/runtime/ref/Local;
		
		//GETSTATIC bytecodeSandbox client Lcom/concurnas/runtime/ref/Local;
		
		if (dowhat == GETSTATIC && fullModuleAndClassName.equals("bytecodeSandbox") && name.equals("client")) {
			int g = 9;
		}

		consumeNextLabelIfExists();
		lastWroteThrow = false;
		lastWriteLabel = false;
		lastWroteRet = false;
		if (PRINT_OPCODES) {
			System.out.println(String.format("    %s %s %s %s", opcodeToBytecode.get(dowhat), fullModuleAndClassName, name, bytecodeType));
		}
		mv.visitFieldInsn(dowhat, fullModuleAndClassName, name, bytecodeType);
	}

	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		visitMethodInsn(opcode, owner, name, desc, opcode == Opcodes.INVOKEINTERFACE);
	}

	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
		// INVOKESTATIC bytecodeSandbox$ID.operate$traitM (LbytecodeSandbox$ID;I)I
		// INVOKEVIRTUAL bytecodeSandbox$Cls.setX (Ljava/lang/String;)V
		//INVOKESTATIC com/concurnas/lang/Actor.psar$7 (Lcom/concurnas/lang/Actor;)V
		//INVOKEVIRTUAL bytecodeSandbox$MyThing.assign (I)V
		//INVOKEVIRTUAL bytecodeSandbox$Instance.copy (Lcom/concurnas/bootstrap/runtime/CopyTracker;Lcom/concurnas/bootstrap/runtime/CopyDefinition;)LbytecodeSandbox$Instance;
		//INVOKEVIRTUAL java/nio/Buffer.clear ()Ljava/nio/Buffer;
		///INVOKEVIRTUAL com/concurnas/bootstrap/lang/Lambda$Function1.apply (Ljava/lang/Object;)Ljava/lang/Object;
		//INVOKEVIRTUAL com/concurnas/bootstrap/lang/Lambda$Function1.apply (Ljava/lang/Object;)V
		//INVOKEVIRTUAL java/lang/Integer.intValue ()I
		//INVOKESPECIAL java/lang/Enum
		//INVOKEVIRTUAL java/util/HashMap.get (Ljava/lang/Object;)Ljava/lang/Object;
		//INVOKEVIRTUAL com/myorg/code2$Master.thecall ()I
		//INVOKESTATIC com/concurnas/runtime/InstanceofGeneric.isGenericInstnaceof (Ljava/lang/Object;[Ljava/lang/Class;)Z
		//INVOKESTATIC com/concurnas/runtime/InstanceofGeneric.assertGenericInstnaceof (Ljava/lang/Object;[Ljava/lang/Class;)V
		//INVOKESTATIC java/lang/Boolean.valueOf (Z)Ljava/lang/Boolean;
		//INVOKESPECIAL bytecodeSandbox$bytecodeSandbox$$Lambda2.<init> (Ljava/util/ArrayList;Ljava/lang/Object;)V
		//INVOKEVIRTUAL java/lang/Integer.intValue ()I
		//INVOKESPECIAL com/concurnas/runtime/ref/Local.<init> ([Ljava/lang/Class;)V
		//INVOKESTATIC java/lang/Short.valueOf (S)Ljava/lang/Short;
		//INVOKESPECIAL bytecodeSandbox$bytecodeSandbox$$Lambda0.<init> ()V
		//INVOKESPECIAL bytecodeSandbox$MyTrait.ifaceMethod ()I
		//INVOKESTATIC bytecodeSandbox$MyTrait.thing$traitM (LbytecodeSandbox$MyTrait;)Ljava/lang/String;
		//INVOKEINTERFACE bytecodeSandbox$MyTrait.ifaceMethod ()I
		//INVOKESPECIAL java/lang/Object.<init> (Lrepl$$Master;Lrepl$$Master;)V
		//    INVOKESPECIAL com/concurnas/runtime/ref/Local.<init> ([Ljava/lang/Class;)V

		//INVOKEVIRTUAL com/concurnas/runtime/ref/Local.set (Ljava/lang/Object;)V
		//    INVOKESPECIAL com/concurnas/runtime/ref/Local.<init> ([Ljava/lang/Class;)V

		
		if(opcode == INVOKEVIRTUAL && owner.equals("com/concurnas/runtime/ref/Local") && name.equals("set") ) {
			int h=9;
		}
		
		// INVOKEVIRTUAL bytecodeSandbox$MyClass.getLa
		// (Ljava/lang/Object;)Ljava/lang/Object;
		// INVOKEVIRTUAL com/concurnas/runtime/ref/RefArray.get
		// (Ljava/lang/Object;)Ljava/lang/Object;

		consumeNextLabelIfExists();
		lastWroteThrow = false;
		lastWriteLabel = false;
		lastWroteRet = false;

		// System.out.println("=== " + name);

		if (PRINT_OPCODES) {
			System.out.println("    " + opcodeToBytecode.get(opcode) + " " + owner + "." + name + " " + desc);
		}
		mv.visitMethodInsn(opcode, owner, name, desc, isInterface);
	}

	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		// consumeNextLabelIfExists(); - always at top of class?

		start = labelOverride.containsKey(start) ? labelOverride.get(start) : start;
		end = labelOverride.containsKey(end) ? labelOverride.get(end) : end;
		handler = labelOverride.containsKey(handler) ? labelOverride.get(handler) : handler;
		lastWroteThrow = false;
		lastWriteLabel = false;
		lastWroteRet = false;
		mv.visitTryCatchBlock(start, end, handler, type);
		if (PRINT_OPCODES) {
			System.out.println(String.format("    TRYCATCHBLOCK  %s %s %s %s", start, end, handler, type));
		}
	}

	public void visitIincInsn(int nslot, int i) {
		consumeNextLabelIfExists();
		lastWroteThrow = false;
		lastWriteLabel = false;
		lastWroteRet = false;
		mv.visitIincInsn(nslot, i);
		if (PRINT_OPCODES) {
			System.out.println("    " + opcodeToBytecode.get(nslot) + " " + i);
		}
	}

	public static HashMap<Integer, String> opcodeToBytecode = new HashMap<Integer, String>(); // should be private
	static {
		opcodeToBytecode.put(0, "NOP");
		opcodeToBytecode.put(1, "ACONST_NULL");
		opcodeToBytecode.put(2, "ICONST_M1");
		opcodeToBytecode.put(3, "ICONST_0");
		opcodeToBytecode.put(4, "ICONST_1");
		opcodeToBytecode.put(5, "ICONST_2");
		opcodeToBytecode.put(6, "ICONST_3");
		opcodeToBytecode.put(7, "ICONST_4");
		opcodeToBytecode.put(8, "ICONST_5");
		opcodeToBytecode.put(9, "LCONST_0");
		opcodeToBytecode.put(10, "LCONST_1");
		opcodeToBytecode.put(11, "FCONST_0");
		opcodeToBytecode.put(12, "FCONST_1");
		opcodeToBytecode.put(13, "FCONST_2");
		opcodeToBytecode.put(14, "DCONST_0");
		opcodeToBytecode.put(15, "DCONST_1");
		opcodeToBytecode.put(16, "BIPUSH");
		opcodeToBytecode.put(17, "SIPUSH");
		opcodeToBytecode.put(18, "LDC");
		opcodeToBytecode.put(19, "LDC_W");
		opcodeToBytecode.put(20, "LDC2_W");
		opcodeToBytecode.put(21, "ILOAD");
		opcodeToBytecode.put(22, "LLOAD");
		opcodeToBytecode.put(23, "FLOAD");
		opcodeToBytecode.put(24, "DLOAD");
		opcodeToBytecode.put(25, "ALOAD");
		opcodeToBytecode.put(26, "ILOAD_0");
		opcodeToBytecode.put(27, "ILOAD_1");
		opcodeToBytecode.put(28, "ILOAD_2");
		opcodeToBytecode.put(29, "ILOAD_3");
		opcodeToBytecode.put(30, "LLOAD_0");
		opcodeToBytecode.put(31, "LLOAD_1");
		opcodeToBytecode.put(32, "LLOAD_2");
		opcodeToBytecode.put(33, "LLOAD_3");
		opcodeToBytecode.put(34, "FLOAD_0");
		opcodeToBytecode.put(35, "FLOAD_1");
		opcodeToBytecode.put(36, "FLOAD_2");
		opcodeToBytecode.put(37, "FLOAD_3");
		opcodeToBytecode.put(38, "DLOAD_0");
		opcodeToBytecode.put(39, "DLOAD_1");
		opcodeToBytecode.put(40, "DLOAD_2");
		opcodeToBytecode.put(41, "DLOAD_3");
		opcodeToBytecode.put(42, "ALOAD_0");
		opcodeToBytecode.put(43, "ALOAD_1");
		opcodeToBytecode.put(44, "ALOAD_2");
		opcodeToBytecode.put(45, "ALOAD_3");
		opcodeToBytecode.put(46, "IALOAD");
		opcodeToBytecode.put(47, "LALOAD");
		opcodeToBytecode.put(48, "FALOAD");
		opcodeToBytecode.put(49, "DALOAD");
		opcodeToBytecode.put(50, "AALOAD");
		opcodeToBytecode.put(51, "BALOAD");
		opcodeToBytecode.put(52, "CALOAD");
		opcodeToBytecode.put(53, "SALOAD");
		opcodeToBytecode.put(54, "ISTORE");
		opcodeToBytecode.put(55, "LSTORE");
		opcodeToBytecode.put(56, "FSTORE");
		opcodeToBytecode.put(57, "DSTORE");
		opcodeToBytecode.put(58, "ASTORE");
		opcodeToBytecode.put(59, "ISTORE_0");
		opcodeToBytecode.put(60, "ISTORE_1");
		opcodeToBytecode.put(61, "ISTORE_2");
		opcodeToBytecode.put(62, "ISTORE_3");
		opcodeToBytecode.put(63, "LSTORE_0");
		opcodeToBytecode.put(64, "LSTORE_1");
		opcodeToBytecode.put(65, "LSTORE_2");
		opcodeToBytecode.put(66, "LSTORE_3");
		opcodeToBytecode.put(67, "FSTORE_0");
		opcodeToBytecode.put(68, "FSTORE_1");
		opcodeToBytecode.put(69, "FSTORE_2");
		opcodeToBytecode.put(70, "FSTORE_3");
		opcodeToBytecode.put(71, "DSTORE_0");
		opcodeToBytecode.put(72, "DSTORE_1");
		opcodeToBytecode.put(73, "DSTORE_2");
		opcodeToBytecode.put(74, "DSTORE_3");
		opcodeToBytecode.put(75, "ASTORE_0");
		opcodeToBytecode.put(76, "ASTORE_1");
		opcodeToBytecode.put(77, "ASTORE_2");
		opcodeToBytecode.put(78, "ASTORE_3");
		opcodeToBytecode.put(79, "IASTORE");
		opcodeToBytecode.put(80, "LASTORE");
		opcodeToBytecode.put(81, "FASTORE");
		opcodeToBytecode.put(82, "DASTORE");
		opcodeToBytecode.put(83, "AASTORE");
		opcodeToBytecode.put(84, "BASTORE");
		opcodeToBytecode.put(85, "CASTORE");
		opcodeToBytecode.put(86, "SASTORE");
		opcodeToBytecode.put(87, "POP");
		opcodeToBytecode.put(88, "POP2");
		opcodeToBytecode.put(89, "DUP");
		opcodeToBytecode.put(90, "DUP_X1");
		opcodeToBytecode.put(91, "DUP_X2");
		opcodeToBytecode.put(92, "DUP2");
		opcodeToBytecode.put(93, "DUP2_X1");
		opcodeToBytecode.put(94, "DUP2_X2");
		opcodeToBytecode.put(95, "SWAP");
		opcodeToBytecode.put(96, "IADD");
		opcodeToBytecode.put(97, "LADD");
		opcodeToBytecode.put(98, "FADD");
		opcodeToBytecode.put(99, "DADD");
		opcodeToBytecode.put(100, "ISUB");
		opcodeToBytecode.put(101, "LSUB");
		opcodeToBytecode.put(102, "FSUB");
		opcodeToBytecode.put(103, "DSUB");
		opcodeToBytecode.put(104, "IMUL");
		opcodeToBytecode.put(105, "LMUL");
		opcodeToBytecode.put(106, "FMUL");
		opcodeToBytecode.put(107, "DMUL");
		opcodeToBytecode.put(108, "IDIV");
		opcodeToBytecode.put(109, "LDIV");
		opcodeToBytecode.put(110, "FDIV");
		opcodeToBytecode.put(111, "DDIV");
		opcodeToBytecode.put(112, "IREM");
		opcodeToBytecode.put(113, "LREM");
		opcodeToBytecode.put(114, "FREM");
		opcodeToBytecode.put(115, "DREM");
		opcodeToBytecode.put(116, "INEG");
		opcodeToBytecode.put(117, "LNEG");
		opcodeToBytecode.put(118, "FNEG");
		opcodeToBytecode.put(119, "DNEG");
		opcodeToBytecode.put(120, "ISHL");
		opcodeToBytecode.put(121, "LSHL");
		opcodeToBytecode.put(122, "ISHR");
		opcodeToBytecode.put(123, "LSHR");
		opcodeToBytecode.put(124, "IUSHR");
		opcodeToBytecode.put(125, "LUSHR");
		opcodeToBytecode.put(126, "IAND");
		opcodeToBytecode.put(127, "LAND");
		opcodeToBytecode.put(128, "IOR");
		opcodeToBytecode.put(129, "LOR");
		opcodeToBytecode.put(130, "IXOR");
		opcodeToBytecode.put(131, "LXOR");
		opcodeToBytecode.put(132, "IINC");
		opcodeToBytecode.put(133, "I2L");
		opcodeToBytecode.put(134, "I2F");
		opcodeToBytecode.put(135, "I2D");
		opcodeToBytecode.put(136, "L2I");
		opcodeToBytecode.put(137, "L2F");
		opcodeToBytecode.put(138, "L2D");
		opcodeToBytecode.put(139, "F2I");
		opcodeToBytecode.put(140, "F2L");
		opcodeToBytecode.put(141, "F2D");
		opcodeToBytecode.put(142, "D2I");
		opcodeToBytecode.put(143, "D2L");
		opcodeToBytecode.put(144, "D2F");
		opcodeToBytecode.put(145, "I2B");
		opcodeToBytecode.put(146, "I2C");
		opcodeToBytecode.put(147, "I2S");
		opcodeToBytecode.put(148, "LCMP");
		opcodeToBytecode.put(149, "FCMPL");
		opcodeToBytecode.put(150, "FCMPG");
		opcodeToBytecode.put(151, "DCMPL");
		opcodeToBytecode.put(152, "DCMPG");
		opcodeToBytecode.put(153, "IFEQ");
		opcodeToBytecode.put(154, "IFNE");
		opcodeToBytecode.put(155, "IFLT");
		opcodeToBytecode.put(156, "IFGE");
		opcodeToBytecode.put(157, "IFGT");
		opcodeToBytecode.put(158, "IFLE");
		opcodeToBytecode.put(159, "IF_ICMPEQ");
		opcodeToBytecode.put(160, "IF_ICMPNE");
		opcodeToBytecode.put(161, "IF_ICMPLT");
		opcodeToBytecode.put(162, "IF_ICMPGE");
		opcodeToBytecode.put(163, "IF_ICMPGT");
		opcodeToBytecode.put(164, "IF_ICMPLE");
		opcodeToBytecode.put(165, "IF_ACMPEQ");
		opcodeToBytecode.put(166, "IF_ACMPNE");
		opcodeToBytecode.put(167, "GOTO");
		opcodeToBytecode.put(168, "JSR");
		opcodeToBytecode.put(169, "RET");
		opcodeToBytecode.put(170, "TABLESWITCH");
		opcodeToBytecode.put(171, "LOOKUPSWITCH");
		opcodeToBytecode.put(172, "IRETURN");
		opcodeToBytecode.put(173, "LRETURN");
		opcodeToBytecode.put(174, "FRETURN");
		opcodeToBytecode.put(175, "DRETURN");
		opcodeToBytecode.put(176, "ARETURN");
		opcodeToBytecode.put(177, "RETURN");
		opcodeToBytecode.put(178, "GETSTATIC");
		opcodeToBytecode.put(179, "PUTSTATIC");
		opcodeToBytecode.put(180, "GETFIELD");
		opcodeToBytecode.put(181, "PUTFIELD");
		opcodeToBytecode.put(182, "INVOKEVIRTUAL");
		opcodeToBytecode.put(183, "INVOKESPECIAL");
		opcodeToBytecode.put(184, "INVOKESTATIC");
		opcodeToBytecode.put(185, "INVOKEINTERFACE");
		opcodeToBytecode.put(186, "INVOKEDYNAMIC");
		opcodeToBytecode.put(187, "NEW");
		opcodeToBytecode.put(188, "NEWARRAY");
		opcodeToBytecode.put(189, "ANEWARRAY");
		opcodeToBytecode.put(190, "ARRAYLENGTH");
		opcodeToBytecode.put(191, "ATHROW");
		opcodeToBytecode.put(192, "CHECKCAST");
		opcodeToBytecode.put(193, "INSTANCEOF");
		opcodeToBytecode.put(194, "MONITORENTER");
		opcodeToBytecode.put(195, "MONITOREXIT");
		opcodeToBytecode.put(196, "WIDE");
		opcodeToBytecode.put(197, "MULTIANEWARRAY");
		opcodeToBytecode.put(198, "IFNULL");
		opcodeToBytecode.put(199, "IFNONNULL");
		opcodeToBytecode.put(200, "GOTO_W");
		opcodeToBytecode.put(201, "JSR_W");
	}
}
