package com.concurnas.runtime;

import static com.concurnas.runtime.cps.Constants.D_ARRAY_BOOLEAN;
import static com.concurnas.runtime.cps.Constants.D_ARRAY_BYTE;
import static com.concurnas.runtime.cps.Constants.D_ARRAY_CHAR;
import static com.concurnas.runtime.cps.Constants.D_ARRAY_DOUBLE;
import static com.concurnas.runtime.cps.Constants.D_ARRAY_FLOAT;
import static com.concurnas.runtime.cps.Constants.D_ARRAY_INT;
import static com.concurnas.runtime.cps.Constants.D_ARRAY_LONG;
import static com.concurnas.runtime.cps.Constants.D_ARRAY_SHORT;
import static com.concurnas.runtime.cps.Constants.D_BOOLEAN;
import static com.concurnas.runtime.cps.Constants.D_BYTE;
import static com.concurnas.runtime.cps.Constants.D_CHAR;
import static com.concurnas.runtime.cps.Constants.D_DOUBLE;
import static com.concurnas.runtime.cps.Constants.D_FLOAT;
import static com.concurnas.runtime.cps.Constants.D_INT;
import static com.concurnas.runtime.cps.Constants.D_LONG;
import static com.concurnas.runtime.cps.Constants.D_NULL;
import static com.concurnas.runtime.cps.Constants.D_SHORT;
import static com.concurnas.runtime.cps.Constants.D_VOID;
import static org.objectweb.asm.Opcodes.*;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.concurnas.compiler.bytecode.BytecodeOutputter;
import com.concurnas.runtime.cps.analysis.BasicBlock;
import com.concurnas.runtime.cps.analysis.TypeDesc;

public class BytecodeStackFrameModifier {

	public static boolean intepret(int i, int opcode, AbstractInsnNode ain, AbstractFrame frame, String name) {
		return intepret( i,  opcode,  ain,  frame,  name, null);
	}
	
	public static boolean intepret(int i, int opcode, AbstractInsnNode ain, AbstractFrame frame, String name, BasicBlock bb) {
		//int opcode = ain.getOpcode();
		String componentType = null;
		boolean propagateFrame = true;

		Value v, v1, v2, v3, v4;
		int val, var;
		
		switch (opcode) {
		case -1: // linenumbernode, framenode, etc.
			//conitnue
			//return null;
			break;
		case NOP:
			break;
		case ACONST_NULL:
			frame.push(Value.make(i, D_NULL));
			break;

		case ICONST_M1:
		case ICONST_0:
		case ICONST_1:
		case ICONST_2:
		case ICONST_3:
		case ICONST_4:
		case ICONST_5:
			frame.push(Value.make(i, D_INT, new Integer(opcode - ICONST_0)));
			break;

		case LCONST_0:
		case LCONST_1:
			frame.push(Value.make(i, D_LONG, new Long(opcode - LCONST_0)));
			break;

		case ILOAD:
		case LLOAD:
		case FLOAD:
		case DLOAD:
		case ALOAD:
			var = ((VarInsnNode) ain).var;
			v = frame.getLocal(var, opcode);
			frame.push(v);
			break;

		case FCONST_0:
		case FCONST_1:
		case FCONST_2:
			frame.push(Value.make(i, D_FLOAT, new Float(opcode - FCONST_0)));
			break;

		case DCONST_0:
		case DCONST_1:
			frame.push(Value.make(i, D_DOUBLE, new Double(opcode - DCONST_0)));
			break;

		case BIPUSH:
			val = ((IntInsnNode) ain).operand;
			frame.push(Value.make(i, D_BYTE, new Integer(val)));
			break;

		case SIPUSH:
			val = ((IntInsnNode) ain).operand;
			frame.push(Value.make(i, D_SHORT, new Integer(val)));
			break;

		case LDC:
			Object cval = ((LdcInsnNode) ain).cst;
			frame.push(Value.make(i, TypeDesc.getTypeDesc(cval), cval));
			break;

		case IALOAD:
		case LALOAD:
		case FALOAD:
		case DALOAD:
		case AALOAD:
		case BALOAD:
		case CALOAD:
		case SALOAD:
			// canThrowException = true;
			frame.popWord(); // pop index
			v = frame.popWord(); // array ref
			frame.push(Value.make(i, TypeDesc.getComponentType(v.getTypeDesc()))); // push
			// component
			// of
			// array
			break;

		case ISTORE:
		case FSTORE:
		case ASTORE:
			v1 = frame.pop();
			var = ((VarInsnNode) ain).var;
			frame.setLocal(var, v1);
			break;
		case LSTORE:
		case DSTORE:
			v1 = frame.popWord();
			var = ((VarInsnNode) ain).var;
			frame.setLocal(var, v1);
			break;

		case IASTORE:
		case LASTORE:
		case FASTORE:
		case DASTORE:
		case AASTORE:
		case BASTORE:
		case CASTORE:
		case SASTORE:
			// canThrowException = true;
			frame.popn(3);
			break;

		case POP:
			frame.popWord();
			break;

		case POP2:
			if (frame.pop().isCategory1()) {
				frame.popWord();
			}
			break;

		case DUP:
			// ... w => ... w w
			v = frame.popWord();
			frame.push(v);
			frame.push(v);
			break;

		case DUP_X1:
			// Insert top word beneath the next word
			// .. w2 w1 => .. w1 w2 w1
			v1 = frame.popWord();
			v2 = frame.popWord();
			frame.push(v1);
			frame.push(v2);
			frame.push(v1);
			break;

		case DUP_X2:
			// Insert top word beneath the next two words (or dword)
			v1 = frame.popWord();
			v2 = frame.pop();
			if (v2.isCategory1()) {
				v3 = frame.pop();
				if (v3.isCategory1()) {
					// w3,w2,w1 => w1,w3,w2,w1
					frame.push(v1);
					frame.push(v3);
					frame.push(v2);
					frame.push(v1);
					break;
				}
			} else {
				// dw2,w1 => w1,dw2,w1
				frame.push(v1);
				frame.push(v2);
				frame.push(v1);
				break;
			}
			//throw new InternalError("Illegal use of DUP_X2 in " + name);

		case DUP2:
			// duplicate top two words (or dword)
			v1 = frame.pop();
			if (v1.isCategory1()) {
				v2 = frame.pop();
				if (v2.isCategory1()) {
					// w2,w1 => w2,w1,w2,w1
					frame.push(v2);
					frame.push(v1);
					frame.push(v2);
					frame.push(v1);
					break;
				}
			} else {
				// dw1 => dw1,dw1
				frame.push(v1);
				frame.push(v1);
				break;
			}
			throw new InternalError("Illegal use of DUP2");

		case DUP2_X1:
			// insert two words (or dword) beneath next word
			v1 = frame.pop();
			if (v1.isCategory1()) {
				v2 = frame.pop();
				if (v2.isCategory1()) {
					v3 = frame.popWord();
					// w3,w2,w1 => w2,w1,w3,w2,w1
					frame.push(v2);
					frame.push(v1);
					frame.push(v3);
					frame.push(v2);
					frame.push(v1);
					break;
				}
			} else { // TypeDesc.isDoubleWord(t1)
				// w2,dw1 => dw1,w2,dw1
				v2 = frame.popWord();
				frame.push(v1);
				frame.push(v2);
				frame.push(v1);
				break;
			}
			throw new InternalError("Illegal use of DUP2_X1");
		case DUP2_X2:
			// insert two words (or dword) beneath next two words (or
			// dword)
			v1 = frame.pop();
			if (v1.isCategory1()) {
				v2 = frame.pop();
				if (v2.isCategory1()) {
					v3 = frame.pop();
					if (v3.isCategory1()) {
						v4 = frame.pop();
						if (v4.isCategory1()) {
							// w4,w3,w2,w1 => w2,w1,w4,w3,w2,w1
							frame.push(v2);
							frame.push(v1);
							frame.push(v4);
							frame.push(v3);
							frame.push(v2);
							frame.push(v1);
							break;
						}
					} else { // TypeDesc.isDoubleWord(t3)
						// dw3,w2,w1 => w2,w1,dw3,w2,w1
						frame.push(v2);
						frame.push(v1);
						frame.push(v3);
						frame.push(v2);
						frame.push(v1);
						break;
					}
				}
			} else { // TypeDesc.isDoubleWord(t1)
				v2 = frame.pop();
				if (v2.isCategory1()) {
					v3 = frame.pop();
					if (v3.isCategory1()) {
						// w3,w2,dw1 => dw1,w3,w2,dw1
						frame.push(v1);
						frame.push(v3);
						frame.push(v2);
						frame.push(v1);
						break;
					}
				} else {
					// dw2,dw1 => dw1,dw2,dw1
					frame.push(v1);
					frame.push(v2);
					frame.push(v1);
					break;
				}
			}
			throw new InternalError("Illegal use of DUP2_X2");

		case SWAP:
			// w2, w1 => w1, w2
			v1 = frame.popWord();
			v2 = frame.popWord();
			frame.push(v1);
			frame.push(v2);
			break;

		case IDIV:
		case IREM:
		case LDIV:
		case LREM:
			frame.pop(); // See next case
			// canThrowException = true;
			break;

		case IADD:
		case FADD:
		case ISUB:
		case LSUB:
		case FSUB:
		case DSUB:
		case IMUL:
		case FMUL:
		case FDIV:
		case FREM:
		case DREM:
		case ISHL:
		case LSHL:
		case ISHR:
		case LSHR:
		case IUSHR:
		case LUSHR:
		case IAND:
		case LAND:
		case IOR:
		case LOR:
		case IXOR:
		case LXOR:
			// Binary op.
			frame.pop();
			v = frame.pop();
			// The result is always the same type as the first arg
			frame.push(Value.make(i, v.getTypeDesc()));
			break;
		case LADD:
		case DADD:
		case LMUL:
		case DMUL:
		case DDIV:
			// Binary op.
			frame.popWord();
			v = frame.popWord();
			// The result is always the same type as the first arg
			frame.push(Value.make(i, v.getTypeDesc()));
			break;

		case LCMP:
		case FCMPL:
		case FCMPG:
		case DCMPL:
		case DCMPG:
			frame.popn(2);
			frame.push(Value.make(i, D_INT));
			break;

		case INEG:
		case LNEG:
		case FNEG:
		case DNEG:
			v = frame.pop();
			frame.push(Value.make(i, v.getTypeDesc()));
			break;

		case IINC:
			var = ((IincInsnNode) ain).var;
			frame.setLocal(var, Value.make(i, D_INT));
			break;

		case I2L:
		case F2L:
		case D2L:
			frame.pop();
			frame.push(Value.make(i, D_LONG));
			break;

		case I2D:
		case L2D:
		case F2D:
			frame.pop();
			frame.push(Value.make(i, D_DOUBLE));
			break;

		case I2F:
		case L2F:
		case D2F:
			frame.pop();
			frame.push(Value.make(i, D_FLOAT));
			break;

		case L2I:
		case F2I:
		case D2I:
			frame.pop();
			frame.push(Value.make(i, D_INT));
			break;

		case I2B:
			frame.popWord();
			frame.push(Value.make(i, D_BOOLEAN));
			break;

		case I2C:
			frame.popWord();
			frame.push(Value.make(i, D_CHAR));
			break;

		case I2S:
			frame.popWord();
			frame.push(Value.make(i, D_SHORT));
			break;

		case IFEQ:
		case IFNE:
		case IFLT:
		case IFGE:
		case IFGT:
		case IFLE:
		case IFNULL:
		case IFNONNULL:
			frame.popWord();
			break;

		case IF_ICMPEQ:
		case IF_ICMPNE:
		case IF_ICMPLT:
		case IF_ICMPGE:
		case IF_ICMPGT:
		case IF_ICMPLE:
		case IF_ACMPEQ:
		case IF_ACMPNE:
			frame.popn(2);
			break;

		case GOTO:
		case JSR: // note: the targetBB pushes the return address
			// itself
			// because it is marked with isSubroutine
		case RET:
			break;

		case TABLESWITCH:
		case LOOKUPSWITCH:
			frame.pop();
			break;

		case IRETURN:
		case LRETURN:
		case FRETURN:
		case DRETURN:
		case ARETURN:
		case RETURN:
			// canThrowException = true;
			if (opcode != RETURN) {
				frame.pop();
			}
			if (frame.getStackLen() != 0) {
				//throw new RuntimeException( String.format("Stack should be empty on %s opcode for: '%s' not: %s", BytecodeOutputter.opcodeToBytecode.get(opcode), name,  frame.stacktoString()));
			}
			break;

		case GETSTATIC:
			// canThrowException = true;
			v = Value.make(i, TypeDesc.getInterned(((FieldInsnNode) ain).desc));
			frame.push(v);
			break;

		case PUTSTATIC:
			// canThrowException = true;
			frame.pop();
			break;

		case GETFIELD:
			// canThrowException = true;
			v1 = frame.pop();
			v = Value.make(i, TypeDesc.getInterned(((FieldInsnNode) ain).desc));
			// if (TypeDesc.isRefType(v.getTypeDesc())) {
			// System.out.println("GETFIELD " + ((FieldInsnNode)ain).name + ": "
			// + v + "---->" + v1);
			// }
			frame.push(v);
			break;

		case PUTFIELD:
			// canThrowException = true;
			v1 = frame.pop();
			v = frame.pop();
			// if (TypeDesc.isRefType(v.getTypeDesc())) {
			// System.out.println("PUTFIELD " + ((FieldInsnNode)ain).name + ": "
			// + v + " ----> " + v1);
			// }
			break;

		case INVOKEVIRTUAL:
		case INVOKESPECIAL:
		case INVOKESTATIC:
		case INVOKEINTERFACE:
			// pop args, push return value
			//not used in conc code yet so we dont need worry about this for state tracking fully and have no ain == null protection
			String desc;
			if (ain instanceof MethodInsnNode) {
				MethodInsnNode min = ((MethodInsnNode) ain);
				desc = min.desc;
				
			} else {
				InvokeDynamicInsnNode min = ((InvokeDynamicInsnNode) ain);
				desc = min.desc;
			}

			if(frame.numMonitorsActive >0 && null != bb){
				bb.unsetFlag(BasicBlock.PAUSABLE);
			}
			
			/*
			 * if (flow.isPausableMethodInsn(min) && frame.numMonitorsActive >
			 * 0) { throw new CPSException(
			 * "Error: Can not call pausable nethods from within a synchronized block\n"
			 * + "Caller: " + this.flow.classFlow.name.replace('/', '.') + "." +
			 * this.flow.name + this.flow.desc + "\nCallee: " +
			 * ((MethodInsnNode)ain).name); //TODO: remove this, we dont have
			 * synchronized blocks (except in java call?) }
			 */
			
			
			// canThrowException = true;
			frame.popn(TypeDesc.getNumArgumentTypes(desc));
			if (opcode != INVOKESTATIC /*&& opcode != INVOKEDYNAMIC*/) {
				v = frame.pop(); // "this" ref
				// assert checkReceiverType(v, min) : "Method " + flow.name +
				// " calls " + min.name +
				// " on a receiver with incompatible type " + v.getTypeDesc() ;
			}
			desc = TypeDesc.getReturnTypeDesc(desc);
			if (desc != D_VOID) {
				frame.push(Value.make(i, desc));
			}
			break;

        case INVOKEDYNAMIC: {
            InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode)ain;
            String descx = indy.desc;
            frame.popn(TypeDesc.getNumArgumentTypes(descx));
            desc = TypeDesc.getReturnTypeDesc(descx);
            assert (desc != D_VOID) : "InvokeDynamic return value should be a functional interface";
            frame.push(Value.make(i, desc));
            break;
        }
        
		case NEW:
			// canThrowException = true;
			v = Value.make(i, TypeDesc.getInterned(((TypeInsnNode) ain).desc));
			frame.push(v);
			break;

		case NEWARRAY:
			// canThrowException = true;
			frame.popWord();
			int atype = ((IntInsnNode) ain).operand;
			String t;
			switch (atype) {
			case T_BOOLEAN:
				t = D_ARRAY_BOOLEAN;
				break;
			case T_CHAR:
				t = D_ARRAY_CHAR;
				break;
			case T_FLOAT:
				t = D_ARRAY_FLOAT;
				break;
			case T_DOUBLE:
				t = D_ARRAY_DOUBLE;
				break;
			case T_BYTE:
				t = D_ARRAY_BYTE;
				break;
			case T_SHORT:
				t = D_ARRAY_SHORT;
				break;
			case T_INT:
				t = D_ARRAY_INT;
				break;
			case T_LONG:
				t = D_ARRAY_LONG;
				break;
			default:
				throw new InternalError("Illegal argument to NEWARRAY: " + atype);
			}
			frame.push(Value.make(i, t));
			break;
		case ANEWARRAY:
			// canThrowException = true;
			frame.popWord();
			componentType = TypeDesc.getInterned(((TypeInsnNode) ain).desc);
			v = Value.make(i, TypeDesc.getInterned("[" + componentType));
			frame.push(v);
			break;

		case ARRAYLENGTH:
			// canThrowException = true;
			frame.popWord();
			frame.push(Value.make(i, D_INT));
			break;

		case ATHROW:
			// canThrowException = true;
			frame.pop();
			propagateFrame = false;
			break;

		case CHECKCAST:
			// canThrowException = true;
			frame.pop();
			v = Value.make(i, TypeDesc.getInterned(((TypeInsnNode) ain).desc));
			frame.push(v);
			break;

		case INSTANCEOF:
			// canThrowException = true;
			frame.pop();
			frame.push(Value.make(i, D_INT));
			break;

		case MONITORENTER:
		case MONITOREXIT:
			if (opcode == MONITORENTER) {
				frame.numMonitorsActive++;
			} else {
				frame.numMonitorsActive--;
			}
			// canThrowException = true;
			frame.pop();
			// canThrowException = true;
			break;

		case MULTIANEWARRAY:
			MultiANewArrayInsnNode minode = ain==null?null:(MultiANewArrayInsnNode) ain;
			int dims = minode.dims;
			frame.popn(dims);
			componentType = TypeDesc.getInterned(minode.desc);
			// StringBuffer sb = new StringBuffer(componentType.length() +
			// dims);
			// for (int j = 0; j < dims; j++)
			// sb.append('[');
			// sb.append(componentType);
			// v = Value.make(i, TypeDesc.getInterned(sb.toString()));
			v = Value.make(i, TypeDesc.getInterned(componentType));
			frame.push(v);
			break;
		default:
			throw new RuntimeException(String.format("Unexpected opcode: '%s' - %s", opcode, BytecodeOutputter.opcodeToBytecode.get(opcode)));
		}
		//System.out.println(">> " + frame.stacktoString());
		return propagateFrame;
	}

}
