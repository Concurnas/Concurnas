package com.concurnas.compiler.bytecode;

import org.objectweb.asm.Opcodes;

import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.GenericType;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.PrimativeType;
import com.concurnas.compiler.ast.PrimativeTypeEnum;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.TypeCheckUtils;

public class StringBuffHelper {

	public static void init(BytecodeGennerator bv) {
		bv.bcoutputter.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
		bv.bcoutputter.visitInsn(Opcodes.DUP);//JPT: this is magic to me
	}
	
	public static void start(BytecodeGennerator bv, Type toAdd) {
		String vo = toAdd.getBytecodeType();
		if(!vo.equals("Ljava/lang/String;"))
		{
			if(toAdd.hasArrayLevels())
			{
				append(bv, toAdd);
				bv.bcoutputter.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/String;)Ljava/lang/String;");
			}
			else if(toAdd instanceof PrimativeType)
			{
				bv.bcoutputter.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "("+vo+")Ljava/lang/String;");
			}
			else if(toAdd instanceof NamedType && TypeCheckUtils.isTypedActor(bv.errorRaisableSupressionFromSatc, (NamedType)toAdd)){
				
				String callOn = toAdd.getCheckCastType();
				int opcode = Opcodes.INVOKEVIRTUAL;
				if(callOn.equals("com/concurnas/lang/Actor") || callOn.equals("com/concurnas/lang/TypedActor")){
					callOn = "x" + ((NamedType)toAdd).getGenTypes().get(0).getCheckCastType() + "$$ActorIterface";
					opcode=Opcodes.INVOKEINTERFACE;
				}
				
				bv.bcoutputter.visitMethodInsn(opcode, callOn, "toString$ActorCall", "()Ljava/lang/String;");
			}
			else
			{
				//bv.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
				//below is better
				bv.bcoutputter.visitMethodInsn(Opcodes.INVOKESTATIC, "com/concurnas/bootstrap/lang/Stringifier", "stringify", "(Ljava/lang/Object;)Ljava/lang/String;");
			}
		}
		bv.bcoutputter.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
	}
	
	
	public static void end(BytecodeGennerator bv) {
		bv.bcoutputter.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
	}
	
	
	public static void append(BytecodeGennerator bv, Type tryingToPassType){
		append( bv,  tryingToPassType, false);
	}
		
	public static void doAppend(BytecodeGennerator bv, String bcType){
		
		if(bcType.equals("S") || bcType.equals("B") ){
			bcType = "I";//short/byte cast to int as there is no append method for these (weirdly)
		}
		
		bv.bcoutputter.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",  String.format("(%s)Ljava/lang/StringBuilder;", bcType)) ;
	}
	
	public static void append(BytecodeGennerator bv, Type tryingToPassType, boolean supressSB){
		//supressSB is a hack lol
		boolean callToString = false;
		if(!tryingToPassType.hasArrayLevels() && tryingToPassType instanceof NamedType){
			NamedType asNamed = (NamedType)tryingToPassType;
			if(!TypeCheckUtils.objectNT.equals(asNamed)){
				if(null == TypeCheckUtils.checkSubType(bv.errorRaisableSupressionFromSatc, ScopeAndTypeChecker.map_object, asNamed, 42, 42, 42, 42) && 
						null == TypeCheckUtils.checkSubType(bv.errorRaisableSupressionFromSatc, ScopeAndTypeChecker.list_object, asNamed, 42, 42, 42, 42) &&
						null == TypeCheckUtils.checkSubType(bv.errorRaisableSupressionFromSatc, ScopeAndTypeChecker.set_object, asNamed, 42, 42, 42, 42))
				{//if not map and not list
					callToString=true;
				}
			}
		}
		
		String bcType;
		if(tryingToPassType instanceof PrimativeType && tryingToPassType.hasArrayLevels()){
			bcType="Ljava/lang/Object;";
		}
		else{
			bcType= (tryingToPassType instanceof GenericType || tryingToPassType instanceof NamedType || tryingToPassType instanceof FuncType || (tryingToPassType instanceof PrimativeType)&& ((PrimativeType)tryingToPassType).type == PrimativeTypeEnum.LAMBDA )?"Ljava/lang/Object;":tryingToPassType.getBytecodeType();
		}
		
		if(bcType.equals("S")){
			bcType = "I";//short cast to int
		}
		
		if(callToString || TypeCheckUtils.isNonArrayStringOrPrimative(tryingToPassType)){//JPT: imperfectly unit tested, anything which has no functional impact is here... :(
			//primative with no array or a String - no need to call stringifier
			if(tryingToPassType instanceof NamedType && TypeCheckUtils.isTypedActor(bv.errorRaisableSupressionFromSatc, (NamedType)tryingToPassType)){
				String callOn = tryingToPassType.getCheckCastType();
				int opcode = Opcodes.INVOKEVIRTUAL;
				if(callOn.equals("com/concurnas/lang/Actor") || callOn.equals("com/concurnas/lang/TypedActor")){
					callOn = "x" + ((NamedType)tryingToPassType).getGenTypes().get(0).getCheckCastType() + "$$ActorIterface";
					opcode=Opcodes.INVOKEINTERFACE;
				}
				bv.bcoutputter.visitMethodInsn(opcode, callOn, "toString$ActorCall", "()Ljava/lang/String;");
			}
			if(!supressSB){
				doAppend(bv, bcType);
			}
		}
		else{//uh oh, something tricky, call stringifier
			bv.bcoutputter.visitMethodInsn(Opcodes.INVOKESTATIC, "com/concurnas/bootstrap/lang/Stringifier", "stringify", String.format("(%s)Ljava/lang/String;",bcType)) ;
			bcType = "Ljava/lang/String;";
			if(!supressSB){
				doAppend(bv, bcType);
			}
		}
	}
}
