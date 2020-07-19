package com.concurnas.compiler.bytecode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import com.concurnas.compiler.ast.Annotations;
import com.concurnas.compiler.ast.BitwiseOperationEnum;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.GenericType;
import com.concurnas.compiler.ast.GrandLogicalOperatorEnum;
import com.concurnas.compiler.ast.MulerExprEnum;
import com.concurnas.compiler.ast.MultiType;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.PrimativeType;
import com.concurnas.compiler.ast.PrimativeTypeEnum;
import com.concurnas.compiler.ast.ShiftOperatorEnum;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.VarNull;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.Sixple;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.compiler.visitors.TypeCheckUtilsUtils;
import com.concurnas.runtime.Pair;

public class Utils implements Opcodes {

	public static void genericSswap(BytecodeOutputter mv, Type stackTop, Type belowTop) {
		//caters for double, int on stack - to swap
		int topSlots = varSlotsConsumedByType(stackTop);
		int lowSlots = varSlotsConsumedByType(belowTop);
		
	    if (topSlots == 1) {
	        if (lowSlots == 1) {
	            // Top = 1, below = 1
	            mv.visitInsn(Opcodes.SWAP);
	        } else {
	            // Top = 1, below = 2
	            mv.visitInsn(Opcodes.DUP_X2);
	            mv.visitInsn(Opcodes.POP);
	        }
	    } else {
	        if (lowSlots == 1) {
	            // Top = 2, below = 1
	            mv.visitInsn(Opcodes.DUP2_X1);
	        } else {
	            // Top = 2, below = 2
	            mv.visitInsn(Opcodes.DUP2_X2);
	        }
	        mv.visitInsn(Opcodes.POP2);
	    }
	}
	
	public static int varSlotsConsumedByType(Type varType){
		//useufl for when loading arguments from function header, double and long are 64 bit
		if(varType instanceof PrimativeType && !varType.hasArrayLevels()){
			PrimativeTypeEnum tt = ((PrimativeType)varType).type;
			if(tt == PrimativeTypeEnum.DOUBLE || tt == PrimativeTypeEnum.LONG)
			{//double and long take up 64 bit, so two slots...
				return 2;
			}
		}
		return 1;
	}
	
	public static void applyComparisonOperationOnPrimative(BytecodeOutputter mv, GrandLogicalOperatorEnum op, PrimativeTypeEnum wanted, PrimativeTypeEnum rhs, Label ifFalse)
	{
		if(rhs != wanted)
		{
			applyCastPrimativeType(mv, rhs, wanted);
		}

		//wanted 
		if(op == GrandLogicalOperatorEnum.EQ){//==
			if(wanted == PrimativeTypeEnum.DOUBLE || rhs == PrimativeTypeEnum.DOUBLE) {
				mv.visitInsn(Opcodes.DCMPL);  mv.visitJumpInsn(Opcodes.IFNE, ifFalse);
			}else if(wanted == PrimativeTypeEnum.FLOAT || rhs == PrimativeTypeEnum.FLOAT) {
				mv.visitInsn(Opcodes.FCMPL);  mv.visitJumpInsn(Opcodes.IFNE, ifFalse);
			}else if(wanted == PrimativeTypeEnum.LONG || rhs == PrimativeTypeEnum.LONG) {
				mv.visitInsn(Opcodes.LCMP);  mv.visitJumpInsn(Opcodes.IFNE, ifFalse);
			}else {
				mv.visitJumpInsn(Opcodes.IF_ICMPNE, ifFalse);
			}
		}
		else if(op == GrandLogicalOperatorEnum.NE){//<>
			if(wanted == PrimativeTypeEnum.DOUBLE || rhs == PrimativeTypeEnum.DOUBLE) {
				mv.visitInsn(Opcodes.DCMPL);  mv.visitJumpInsn(Opcodes.IFEQ, ifFalse);
			}else if(wanted == PrimativeTypeEnum.FLOAT || rhs == PrimativeTypeEnum.FLOAT) {
				mv.visitInsn(Opcodes.FCMPL);  mv.visitJumpInsn(Opcodes.IFEQ, ifFalse);
			}else if(wanted == PrimativeTypeEnum.LONG || rhs == PrimativeTypeEnum.LONG) {
				mv.visitInsn(Opcodes.LCMP);  mv.visitJumpInsn(Opcodes.IFEQ, ifFalse);
			}else {
				mv.visitJumpInsn(Opcodes.IF_ICMPEQ, ifFalse);
			}
		}
		else if(op == GrandLogicalOperatorEnum.LT){ //<
			if(wanted == PrimativeTypeEnum.DOUBLE || rhs == PrimativeTypeEnum.DOUBLE) {
				mv.visitInsn(Opcodes.DCMPG);  mv.visitJumpInsn(Opcodes.IFGE, ifFalse);
			}else if(wanted == PrimativeTypeEnum.FLOAT || rhs == PrimativeTypeEnum.FLOAT) {
				mv.visitInsn(Opcodes.FCMPG);  mv.visitJumpInsn(Opcodes.IFGE, ifFalse); 
			}else if(wanted == PrimativeTypeEnum.LONG || rhs == PrimativeTypeEnum.LONG) {
				mv.visitInsn(Opcodes.LCMP);  mv.visitJumpInsn(Opcodes.IFGE, ifFalse); 
			}else {
				mv.visitJumpInsn(Opcodes.IF_ICMPGE, ifFalse);
			}
		}
		else if(op == GrandLogicalOperatorEnum.GT){//>
			if(wanted == PrimativeTypeEnum.DOUBLE || rhs == PrimativeTypeEnum.DOUBLE) {
				mv.visitInsn(Opcodes.DCMPL);  mv.visitJumpInsn(Opcodes.IFLE, ifFalse);
			}else if(wanted == PrimativeTypeEnum.FLOAT || rhs == PrimativeTypeEnum.FLOAT) {
				mv.visitInsn(Opcodes.FCMPL);  mv.visitJumpInsn(Opcodes.IFLE, ifFalse);
			}else if(wanted == PrimativeTypeEnum.LONG || rhs == PrimativeTypeEnum.LONG) {
				mv.visitInsn(Opcodes.LCMP);  mv.visitJumpInsn(Opcodes.IFLE, ifFalse);
			}else {
				mv.visitJumpInsn(Opcodes.IF_ICMPLE, ifFalse);
			}
		}
		else if(op == GrandLogicalOperatorEnum.GTEQ){//>=
			if(wanted == PrimativeTypeEnum.DOUBLE || rhs == PrimativeTypeEnum.DOUBLE) {
				mv.visitInsn(Opcodes.DCMPL);  mv.visitJumpInsn(Opcodes.IFLT, ifFalse);
			}else if(wanted == PrimativeTypeEnum.FLOAT || rhs == PrimativeTypeEnum.FLOAT) {
				mv.visitInsn(Opcodes.FCMPL);  mv.visitJumpInsn(Opcodes.IFLT, ifFalse);
			}else if(wanted == PrimativeTypeEnum.LONG || rhs == PrimativeTypeEnum.LONG) {
				mv.visitInsn(Opcodes.LCMP);  mv.visitJumpInsn(Opcodes.IFLT, ifFalse);
			}else {
				mv.visitJumpInsn(Opcodes.IF_ICMPLT, ifFalse);
			}
		}
		else if(op == GrandLogicalOperatorEnum.LTEQ){//<=
			if(wanted == PrimativeTypeEnum.DOUBLE || rhs == PrimativeTypeEnum.DOUBLE) {
				mv.visitInsn(Opcodes.DCMPG);  mv.visitJumpInsn(Opcodes.IFGT, ifFalse);
			}else if(wanted == PrimativeTypeEnum.FLOAT || rhs == PrimativeTypeEnum.FLOAT) {
				mv.visitInsn(Opcodes.FCMPG);  mv.visitJumpInsn(Opcodes.IFGT, ifFalse);
			}else if(wanted == PrimativeTypeEnum.LONG || rhs == PrimativeTypeEnum.LONG) {
				mv.visitInsn(Opcodes.LCMP);  mv.visitJumpInsn(Opcodes.IFGT, ifFalse);
			}else {
				mv.visitJumpInsn(Opcodes.IF_ICMPGT, ifFalse);
			}
		}
		else if(op == GrandLogicalOperatorEnum.REFEQ){//==
			mv.visitJumpInsn(IF_ACMPNE, ifFalse);
		}
		else if(op == GrandLogicalOperatorEnum.REFNE){//<>
			mv.visitJumpInsn(IF_ACMPEQ, ifFalse);
		}
		
	}
	
	public static  boolean isPrimativeForIfCompy(Type lhs, Type rhs)
	{
		return  truePrimative(lhs) &&  truePrimative(rhs);
		//return lhs instanceof PrimativeType && rhs instanceof PrimativeType && (((PrimativeType)lhs).type != PrimativeTypeEnum.LAMBDA && ((PrimativeType)rhs).type != PrimativeTypeEnum.LAMBDA );
	}
	
	/*
	public static PrimativeTypeEnum getMoreGenericTypeForIfComp(PrimativeTypeEnum l, PrimativeTypeEnum r)
	{
		//double, float, long, int[short, byte, char]
		if(l == PrimativeTypeEnum.DOUBLE || r == PrimativeTypeEnum.DOUBLE){
			return PrimativeTypeEnum.DOUBLE;
		}
		else if(l == PrimativeTypeEnum.FLOAT || r == PrimativeTypeEnum.FLOAT){
			return PrimativeTypeEnum.FLOAT;
		}
		else if(l == PrimativeTypeEnum.LONG || r == PrimativeTypeEnum.LONG){
			return PrimativeTypeEnum.LONG;
		}
		else{
			return PrimativeTypeEnum.INT;
		}
	}
	*/
	
	public static int returnTypeToOpcode(Type ret)
	{
		if(ret == null)
		{
			return Opcodes.RETURN;//void
		}
		else if(ret instanceof PrimativeType && !ret.hasArrayLevels())
		{
			PrimativeTypeEnum pte = ((PrimativeType)ret).type;
			switch(pte)
			{
				case VOID: return Opcodes.RETURN;
				case BOOLEAN:
				case BYTE:
				case SHORT:
				case CHAR:
				case INT: return Opcodes.IRETURN;
				case LONG: return Opcodes.LRETURN;
				case FLOAT: return Opcodes.FRETURN;
				case DOUBLE: return Opcodes.DRETURN;
				
				default: return Opcodes.ARETURN;//PrimativeTypeEnum.LAMBDA
			}
		}
		return Opcodes.ARETURN; //object
		
	}
	
	public static void intOpcode(BytecodeOutputter mv, int input)
	{
		if(input ==-1)
		{
			mv.visitInsn(Opcodes.ICONST_M1);
		}
		else if(input >= 0 && input <= 5)
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
	
	/*public static void intOpcode(MethodVisitor mv, int input)
	{//TODO: nasty copy paste
		if(input ==-1)
		{
			mv.visitInsn(Opcodes.ICONST_M1);
		}
		else if(input >= 0 && input <= 5)
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
	}*/
	
	public static void longOpcode(BytecodeOutputter mv, long input)
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
	
	public static void floatOpcode(BytecodeOutputter mv, float input)
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
	
	public static void doubleOpcode(BytecodeOutputter mv, double input)
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
	
	@SuppressWarnings("incomplete-switch")
	private static void applyCastPrimativeType(BytecodeOutputter mv, PrimativeTypeEnum fromPrim, PrimativeTypeEnum toPrim)
	{
		if(fromPrim==PrimativeTypeEnum.INT ||fromPrim== PrimativeTypeEnum.BYTE ||fromPrim== PrimativeTypeEnum.SHORT || fromPrim== PrimativeTypeEnum.CHAR || fromPrim== PrimativeTypeEnum.BOOLEAN)
		{
			switch(toPrim)
			{
				case LONG  : mv.visitInsn(Opcodes.I2L); break;
				case DOUBLE: mv.visitInsn(Opcodes.I2D); break;
				case FLOAT : mv.visitInsn(Opcodes.I2F); break;
				case BYTE  : mv.visitInsn(Opcodes.I2B); break;
				case CHAR  : mv.visitInsn(Opcodes.I2C); break;
				case SHORT : mv.visitInsn(Opcodes.I2S); break;
			}

		}
		else if(fromPrim==PrimativeTypeEnum.LONG)
		{
			switch(toPrim)
			{
				case INT   : mv.visitInsn(Opcodes.L2I); break;
				case DOUBLE: mv.visitInsn(Opcodes.L2D); break;
				case FLOAT : mv.visitInsn(Opcodes.L2F); break;
				case BOOLEAN : mv.visitInsn(Opcodes.L2I); break;
				
				case BYTE  : mv.visitInsn(Opcodes.L2I); mv.visitInsn(Opcodes.I2C); break;
				case CHAR  : mv.visitInsn(Opcodes.L2I); mv.visitInsn(Opcodes.I2C); break;
				case SHORT : mv.visitInsn(Opcodes.L2I); mv.visitInsn(Opcodes.I2C); break;
				
			}
		}
		else if(fromPrim==PrimativeTypeEnum.DOUBLE)
		{
			switch(toPrim)
			{
				case INT   : mv.visitInsn(Opcodes.D2I); break;
				case LONG  : mv.visitInsn(Opcodes.D2L); break;
				case FLOAT : mv.visitInsn(Opcodes.D2F); break;
				
				case BOOLEAN : mv.visitInsn(Opcodes.D2I); break;
				
				case BYTE  : mv.visitInsn(Opcodes.D2I); mv.visitInsn(Opcodes.I2C); break;
				case CHAR  : mv.visitInsn(Opcodes.D2I); mv.visitInsn(Opcodes.I2C); break;
				case SHORT : mv.visitInsn(Opcodes.D2I); mv.visitInsn(Opcodes.I2C); break;
			}
		}
		else if(fromPrim==PrimativeTypeEnum.FLOAT)
		{
			switch(toPrim)
			{
				case INT   : mv.visitInsn(Opcodes.F2I); break;
				case LONG  : mv.visitInsn(Opcodes.F2L); break;
				case DOUBLE: mv.visitInsn(Opcodes.F2D); break;
				
				case BOOLEAN : mv.visitInsn(Opcodes.F2I); break;
				
				case BYTE  : mv.visitInsn(Opcodes.F2I); mv.visitInsn(Opcodes.I2C); break;
				case CHAR  : mv.visitInsn(Opcodes.F2I); mv.visitInsn(Opcodes.I2C); break;
				case SHORT : mv.visitInsn(Opcodes.F2I); mv.visitInsn(Opcodes.I2C); break;
				
			}
		}
		/*
		    int I2L = 133; // visitInsn
		    int I2F = 134; // -
		    int I2D = 135; // -
		    
		    int I2B = 145; // -
		    int I2C = 146; // -
		    int I2S = 147; // -
		    
		    int L2I = 136; // -
		    int L2F = 137; // -
		    int L2D = 138; // -
		    
		    int F2I = 139; // -
		    int F2L = 140; // -
		    int F2D = 141; // -
		    
		    int D2I = 142; // -
		    int D2L = 143; // -
		    int D2F = 144; // -
		 */
	}
	
	private static HashMap<String, PrimativeTypeEnum> boxedTypes = new HashMap<String, PrimativeTypeEnum>();
	
	static
	{
		HashMap<String, PrimativeTypeEnum[]> btoppte = TypeCheckUtilsUtils.buildToBoxedsPrims();
		for(String tt : btoppte.keySet())
		{
			if(tt.equals("java.lang.Number")) {
				continue;
			}
			boxedTypes.put(tt, btoppte.get(tt)[0]);
		}
	}
	
	private static String checkValidBoxedType(String toThing, PrimativeTypeEnum onFail)
	{
		if(toThing.equals("java/lang/Number")){
			switch(onFail)
			{//first call permits int -> Long convertion as: Long.valueOf((long)int);
				case BOOLEAN : toThing = "java/lang/Boolean"; break; 
				case INT: toThing =  "java/lang/Integer"; break; 
				case LONG:toThing = "java/lang/Long"; break;   
				case FLOAT:toThing = "java/lang/Float"; break; 
				case DOUBLE:toThing = "java/lang/Double"; break; 
				case BYTE:toThing = "java/lang/Byte"; break; 
				case CHAR:toThing = "java/lang/Character"; break; 
				//case LAMBDA:toThing = "com/concurnas/bootstrap/lang/Lambda"; break; 
				default : toThing ="java/lang/Short"; break;  //short
			}
		}
		else if(!boxedTypes.containsKey(toThing.replace('/', '.'))){//if not boxed and also not number
			switch(onFail)
			{//first call permits int -> Long convertion as: Long.valueOf((long)int);
				case BOOLEAN : toThing = "java/lang/Boolean"; break; 
				case INT: toThing =  "java/lang/Integer"; break; 
				case LONG:toThing = "java/lang/Long"; break;   
				case FLOAT:toThing = "java/lang/Float"; break; 
				case DOUBLE:toThing = "java/lang/Double"; break; 
				case BYTE:toThing = "java/lang/Byte"; break; 
				case CHAR:toThing = "java/lang/Character"; break; 
				//case LAMBDA:toThing = "com/concurnas/bootstrap/lang/Lambda"; break; 
				default : toThing ="java/lang/Short"; break;  //short
			}
		}
		return toThing;
	}
	
	public static void doCheckCastConvertArrays(BytecodeOutputter mv, Type orig, String toType){//&& !"[Ljava/lang/Object;".equals(toType)
		if(toType.startsWith("[") && toType.endsWith(";") && orig.hasArrayLevels() ){//apply to objects only and when we started from an object array of n>0 dimentions (i.e. not just a vanilla object)
			mv.visitLdcInsn(org.objectweb.asm.Type.getType(toType));
			mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/lang/ArrayUtils", "cast", "(Ljava/lang/Object;Ljava/lang/Class;)[Ljava/lang/Object;", false);
			mv.visitTypeInsn(CHECKCAST, toType);
		}
		else{
			mv.visitTypeInsn(CHECKCAST, toType);
		}
	}
	
	public static Type applyCastImplicit(BytecodeOutputter mv, Type from, Type to, BytecodeGennerator bcv)
	{
		return applyCastImplicit(mv, from, to, bcv, false);
	}
	
	
	public static void createTypeArrayAddInitialRef(BytecodeOutputter mv, Type nt){
		ArrayList<String> types = new ArrayList<String>();
		types.add( nt.getCheckCastType() );
		createTypeArray( mv,  nt,  types, null, null);
	}
	
	public static void createTypeArray(BytecodeOutputter mv, Type nt){
		ArrayList<String> types = new ArrayList<String>();
		createTypeArray( mv,  nt,  types, null, null);
	}
	
	public static void processItem(Type asNamed, ArrayList<String> types, LinkedList<Type> toProc){
		if (TypeCheckUtils.hasRefLevelsAndIsArray(asNamed)) {
			int upTo = asNamed.getArrayLevels();
			int n = 0;
			while (n++ < upTo) {
				types.add("com/concurnas/bootstrap/runtime/ref/LocalArray");
			}

			String prim = ((NamedType) asNamed).getSetClassDef().javaClassName();
			prim = prim.substring(1, prim.length() - 1);

			types.add(prim);
		} else {
			types.add(asNamed.getCheckCastType());
		}

		if (asNamed instanceof NamedType && ((NamedType) asNamed).hasGenTypes()) {
			toProc.addAll(((NamedType) asNamed).getGenTypes());
		} else if (asNamed instanceof FuncType) {
			FuncType asft = (FuncType) asNamed;
			toProc.addAll(asft.inputs);
			toProc.add(asft.retType);
		}
	}

	private static ArrayList<String> backupTypes(Type from) {
		ArrayList<String> types = new ArrayList<String>();
		LinkedList<Type> toProc = new LinkedList<Type>();
		toProc.add(from);
		while (!toProc.isEmpty()) {
			Type asNamed = toProc.remove(0);
			asNamed = TypeCheckUtils.boxTypeIfPrimative(asNamed, false);// sometimes func type can introduce primative types....
			processItem(asNamed, types, toProc);
		}

		return types;
	}
	
	public static void createTypeArray(BytecodeOutputter mv, Type nt, ArrayList<String> types, HashMap<Integer, Integer> gensToArgsAtRuntime, HashMap<Integer, Pair<Integer, Type>> argToTempAndType){
		//e.g. HashMap<String, Integer> -> [HashMap.class, String.class, Integer.class]
		//Integer -> [Integer]
		ArrayList<Thruple<Integer, Type, ArrayList<String>>> argAndBackupType = null==gensToArgsAtRuntime?null:new ArrayList<Thruple<Integer, Type, ArrayList<String>>>();
			
		
		if(nt instanceof NamedType && ((NamedType)nt).hasGenTypes()){
			LinkedList<Type> toProc = new LinkedList<Type>();
			List<Type> topLevelGens = ((NamedType)nt).getGenTypes();
			int numberOfArgs = topLevelGens.size();
			
			toProc.addAll(topLevelGens);
			while(!toProc.isEmpty()){
				Type asNamed = toProc.remove(0);

				asNamed = TypeCheckUtils.boxTypeIfPrimative(asNamed, false);//sometimes func type can introduce primative types....
				
				if(null != gensToArgsAtRuntime && gensToArgsAtRuntime.containsKey(numberOfArgs - (toProc.size()+1))){//check to see if this is an argument which we need to load the type of at runtime
					types.add(null);
					int arg = gensToArgsAtRuntime.get(numberOfArgs - (toProc.size()+1));
					Pair<Integer, Type> argumentAndType = argToTempAndType.get(arg);
					
					argAndBackupType.add(new Thruple<Integer, Type, ArrayList<String>>(argumentAndType.getA(), argumentAndType.getB(), backupTypes(asNamed)));
				}
				else{
					processItem(asNamed, types, toProc);
				}
			}
		}
		else{
			types.add( nt.getCheckCastType() );
		}
		
		int typeSize = types.size();
		Utils.intOpcode(mv, typeSize);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
		
		ArrayList<Integer> nullAtPoint = new ArrayList<Integer>();
		for(int n=0; n < typeSize; n++){//construct array as normal
			String ttt = types.get(n);
			mv.visitInsn(DUP);
			Utils.intOpcode(mv, n);
			if(null == ttt){
				//Tuple<Integer, ArrayList<String>> whatsTheNull = argAndBackupType.remove(0);
				mv.visitInsn(ACONST_NULL);
				nullAtPoint.add(n);
			}
			else{
				mv.visitLdcInsn(org.objectweb.asm.Type.getType(ttt.endsWith(";")? ttt: "L" + ttt  + ";"));//only add when not at end...
			}
			mv.visitInsn(AASTORE);
		}
		
		//now go through and add missing stuff from args which needs to be extracted at runtime, held as nulls currettly.
		if(null != argAndBackupType){
			//Collections.reverse(argAndBackupType);//?
			for(Thruple<Integer, Type, ArrayList<String>> argTypeAndBackupToAdd : argAndBackupType){
				//Class<?>[] existing, int slotFromEnd, Object obj, Class<?>[] ifNull
				intOpcode(mv, typeSize - 1 - nullAtPoint.remove(0));
				Utils.applyLoad(mv, argTypeAndBackupToAdd.getB(), argTypeAndBackupToAdd.getA());
				createTypeArray(mv, argTypeAndBackupToAdd.getC());
				mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/runtime/RefUtils", "extractTypeAndAugmentTypeArray", "([Ljava/lang/Class;ILjava/lang/Object;[Ljava/lang/Class;)[Ljava/lang/Class;");
			}
		}
		
		
		/*
		mv.visitInsn(ICONST_3);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_0);
		mv.visitLdcInsn(org.objectweb.asm.Type.getType("Ljava/util/HashMap;"));
		mv.visitInsn(AASTORE);
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_1);
		mv.visitLdcInsn(org.objectweb.asm.Type.getType("Ljava/lang/String;"));
		mv.visitInsn(AASTORE);
		*/
	}
	
	public static void addClassTypeArrayForActorRef(BytecodeOutputter mv, NamedType item){
		ArrayList<String> types = new ArrayList<String>();
		//add all but first item 
		LinkedList<Type> toProc = new LinkedList<Type>();
		Utils.processItem(item, types, toProc);
		
		while(!toProc.isEmpty()){
			Type head = toProc.pop();
			Utils.processItem(head, types, toProc);
		}
		types.remove(0);
		Utils.createTypeArray(mv, types);
	}
	
	
	public static void createTypeArray(BytecodeOutputter mv, ArrayList<String> types){
		int typeSize = types.size();
		Utils.intOpcode(mv, typeSize);
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
		
		for(int n=0; n < typeSize; n++){//construct array as normal
			String ttt = types.get(n);
			mv.visitInsn(DUP);
			Utils.intOpcode(mv, n);
			mv.visitLdcInsn(org.objectweb.asm.Type.getType(ttt.endsWith(";")? ttt: "L" + ttt  + ";"));//only add when not at end...
			mv.visitInsn(AASTORE);
		}
	}
	
	public static void createRefArray(BytecodeOutputter mv, NamedType varType){
		/* wrapper for arrays of references, so we can capture the type of the ref */
		
		mv.visitTypeInsn(NEW, "com/concurnas/bootstrap/runtime/ref/LocalArray");
		mv.visitInsn(DUP);
		
		ArrayList<String> types = new ArrayList<String>();
		//types.add( "com/concurnas/runtime/ref/Local" );
		
		int upTo = varType.getArrayLevels();
		int n=1;
		while(n++ < upTo){
			types.add( "com/concurnas/bootstrap/runtime/ref/LocalArray" );
		}
		
		String prim = varType.getSetClassDef().javaClassName();
		prim = prim.substring(1, prim.length() -1);
		
		types.add( prim );
		Utils.createTypeArray(mv, varType, types, null, null);
		
		
		mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/bootstrap/runtime/ref/LocalArray", "<init>", "([Ljava/lang/Class;)V");

		
		mv.visitInsn(DUP_X1);
		mv.visitInsn(SWAP);
		
		mv.visitFieldInsn(PUTFIELD, "com/concurnas/bootstrap/runtime/ref/LocalArray", "ar", "[Ljava/lang/Object;");
		
	}
	
	public static void createRef(BytecodeOutputter mv, Type varType, int slot, boolean doStore, boolean extraRefDup, int createOuuterLevelOnly){
		//createOuuterLevelOnly used for: d int:: = {12:}! //because the inner level gets created inside the asyncblock
		int levels;
		if( createOuuterLevelOnly > -1){
			levels = createOuuterLevelOnly;
		}else{
			if(TypeCheckUtils.isRefArray(varType)){
				levels = 1;
			}else{
				levels = TypeCheckUtils.getRefLevels(varType);
			}
		}
		
		
		String refType = varType.getBytecodeType();
		refType = refType.substring(1, refType.length()-1);//e.g. "com/concurnas/runtime/ref/Local" or "com/concurnas/runtime/ref/Local"
		
		for(int n=0; n < levels; n++){
			
			mv.visitTypeInsn(NEW, refType);
			mv.visitInsn(DUP);
			
			if(n!=levels-1 && levels>1){
				mv.visitInsn(DUP);
			}
			
			Utils.createTypeArray(mv, varType);
			mv.visitMethodInsn(INVOKESPECIAL, refType, "<init>", "([Ljava/lang/Class;)V");
			varType = ((NamedType)varType).getGenTypes().get(0);
		}
		
		for(int n=1; n < levels; n++){
			mv.visitMethodInsn(INVOKEVIRTUAL, refType, "set", "(Ljava/lang/Object;)V");
		}
	
		
		if(doStore){
			if(extraRefDup ){//&& !createOnlyFirst
				mv.visitInsn(DUP);
			}
			
			mv.visitVarInsn(ASTORE, slot);
		}
	}
	
	public static Type createRefInline(BytecodeOutputter mv, BytecodeGennerator bcv, Type from, Type to, boolean supressRefGenneration){
		
		Type storeType = TypeCheckUtils.getRefType(to);
		
		int fromLevels = TypeCheckUtils.getRefLevels(from);
		int refLevels = TypeCheckUtils.getRefLevelsToSetter(to) - fromLevels;
		
		List<String> refTypes = null;
		if(refLevels > 0) {//YUCK
			ArrayList<NamedType> reftypes = TypeCheckUtils.getRefTypes(to);
			Collections.reverse(reftypes);
			refTypes = reftypes.stream().map(a -> a.getSetClassDef().bcFullName()).collect(Collectors.toList());
		}
		
		for(int m=0; m < fromLevels; m++){
			//wrap it up in existing levels
			storeType = new NamedType( 0,0,storeType);
		}
		//bit silly?
		applyCastImplicit(mv, from, storeType, bcv );
		
		if(!supressRefGenneration){
			Type varType = to;
			
			for(int n=0; n < refLevels; n++){
				String thisLevel = refTypes.get(n);
				
				mv.visitTypeInsn(NEW, thisLevel);
				mv.visitInsn(DUP);
				
				if(n!=refLevels-1 && refLevels>1){
					mv.visitInsn(DUP);//TODO: first and more than one? what about 3?
				}
				
				createTypeArray(mv, varType);
				mv.visitMethodInsn(INVOKESPECIAL, thisLevel, "<init>", "([Ljava/lang/Class;)V");

				varType = ((NamedType)varType).getGenTypes().get(0);
			}
			
			for(int n=1; n < refLevels; n++){
				mv.visitMethodInsn(INVOKEVIRTUAL, refTypes.get(n), "set", "(Ljava/lang/Object;)V");
			}
			//mv.visitInsn(SWAP);
		}
			
		
		if(supressRefGenneration && refLevels > 1){
			mv.visitInsn(SWAP);
			for(int n=0; n < refLevels-1; n++){
				String thisLevel = refTypes.get(n);
				mv.visitMethodInsn(INVOKEVIRTUAL, thisLevel, "get", "()Ljava/lang/Object;");
				mv.visitTypeInsn(CHECKCAST, thisLevel);
			}
			mv.visitInsn(SWAP);
		}
		
		
		if(!supressRefGenneration){
			//mv.visitInsn(SWAP);
			mv.visitInsn(DUP_X1);
			
			//what does this do...?
			
			for(int n=0; n < refLevels-1; n++){
				String thisLevel = refTypes.get(n);
				mv.visitMethodInsn(INVOKEVIRTUAL, thisLevel, "get", "()Ljava/lang/Object;");
				mv.visitTypeInsn(CHECKCAST, thisLevel);
			}
			
			mv.visitInsn(SWAP);
			
		}
		
		if(TypeCheckUtils.isRefArraySettable(to, -1)){
			mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/ref/DirectlyArrayAssignable", "set", "([Ljava/lang/Object;)V");//TODO: needs change to DirectlyAssignable iface when we offer more than Local, Remote maybe?
		}else{
			mv.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/runtime/ref/Local", "set", "(Ljava/lang/Object;)V");//TODO: needs change to DirectlyAssignable iface when we offer more than Local, Remote maybe?
		}
		
		return to;
		
	}
	
	private final static PrimativeType const_void_thrown = new PrimativeType(PrimativeTypeEnum.VOID);
	static{
		const_void_thrown.thrown=true;
	}
	
	public static Type applyCastImplicit(BytecodeOutputter mv, Type from, Type to, BytecodeGennerator bcv, boolean supressRefGenneration){
		return applyCastImplicit(mv, from, to, bcv, supressRefGenneration, null);
	}
	
	public static Type applyCastImplicit(BytecodeOutputter mv, Type from, Type to, BytecodeGennerator bcv, boolean supressRefGenneration, Label forBoolOpsIfFalse)
	{
		//if(from instanceof VarNull){ from = TypeCheckUtils.getRefType(to);}
		if(from == null || TypeCheckUtils.isVoidPrimativePure(from)){
			return to;//shouldnt really happen...
		}
		
		if(from instanceof MultiType) {
			from = from.getTaggedType();
		}
		
		if(to instanceof MultiType) {
			to = to.getTaggedType();
		}
		
		
		/*
		 * if(from instanceof VarNull && TypeCheckUtils.hasRefLevels(to)) { //to deal
		 * with likes of: ref3 int:? = RefHelper.getNullRef2(): if tt() else null//ref
		 * created inline for null createRefInline(mv, bcv, from, to, false); return to;
		 * }
		 */
		
		
		if(TypeCheckUtils.isRefArraySettable(to, -1) && from instanceof VarNull){//dont set null on arrayrefs
			mv.visitInsn(Opcodes.POP2);//clean up stack
			//mv.visitInsn(Opcodes.POP);
			return to;
		}
		
		if(from.equals(const_void_thrown)){
			return to;
		}
		
		if(!from.equals(to)){
			
			if(TypeCheckUtils.hasRefLevelsAndNotLocked(from) && (TypeCheckUtils.getRefLevels(from) > TypeCheckUtils.getRefLevels(to) ) )
			{
				Type fro2 = Utils.unref(mv, from, to, bcv);
				return applyCastImplicit(mv, fro2, to, bcv);
			}
			
			if(TypeCheckUtils.hasRefLevelsAndIsArray(from)){
				Type toForAr = to;
				if(toForAr instanceof GenericType){
					toForAr = ((GenericType)toForAr).upperBound;
				}
				if(toForAr.hasArrayLevels() && toForAr instanceof NamedType && ((NamedType)toForAr).getSetClassDef().equals(const_obj)  )			{
					//special case to deal with this: xxx = [1!,2!] as Object[]; 
					//or: funto(a object[])// functo(xxx)//call to this!
					mv.visitFieldInsn(GETFIELD, "com/concurnas/bootstrap/runtime/ref/LocalArray", "ar", "[Ljava/lang/Object;");
				}
			}
			
			if(to instanceof NamedType && to.getPrettyName().equals("java.lang.Object") && (!(from instanceof PrimativeType) || from instanceof PrimativeType && from.hasArrayLevels()  ) ){
				return to;//everything is object
			}
			
			if(to instanceof PrimativeType)
			{//copy pasted horribvle
				PrimativeTypeEnum toPrim = ((PrimativeType)to).type;
				if(from instanceof PrimativeType){
					PrimativeTypeEnum fromPrim = ((PrimativeType)from).type;
					applyCastPrimativeType(mv, fromPrim, toPrim);
				}
				else if(from.equals(ScopeAndTypeChecker.const_Number)){
					convertFromNumberToPrimative(mv, toPrim);
				}
				else if(from instanceof NamedType){
					PrimativeType got = (PrimativeType)unbox(mv, from,  bcv);
					if(null != got){
						applyCastPrimativeType(mv, got.type, toPrim);
					}
				}
			}
			else if(TypeCheckUtils.getRefLevelsToSetter(to) > TypeCheckUtils.getRefLevels(from)  ){//and from is not already a ref
				
				//createRefInline(mv, bcv, from, to, supressRefGenneration);
				
				//if(from.equals(const_Obj_nt) ){
				
				//e.g. int to double: (first cast the int to double)
				if(!TypeCheckUtils.hasRefLevels(from)){
					Type castTo = TypeCheckUtils.getRefType(to);
					
					if(TypeCheckUtils.typeRequiresLocalArrayConvertion(castTo)){//dont perform localarray extraction here
						castTo = (Type)castTo.copy();
						castTo.setOrigonalGenericTypeUpperBound(null);
					}
					
					from = applyCastImplicit(mv, (from), castTo, bcv, supressRefGenneration);
				}
				
				if(!supressRefGenneration){
					from = box(mv, from);
					
					//ArrayList[String] -> ArrayList[String]: - it just needs ref levels to be added
					int cnt = TypeCheckUtils.getRefLevelsToSetter(to) - TypeCheckUtils.getRefLevels(from);
					Type fromWithMatchLevel = TypeCheckUtils.makeRef((Type)from.copy(), cnt);
					if(fromWithMatchLevel.equals(to) && !from.equals(const_obj_nt)){
						//create just add levels
						createRefInline(mv, bcv, from, to, false);
					}
					else{
						Utils.createTypeArrayAddInitialRef(mv, to);
						mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/runtime/GenericRefCast", "genericRefCast", "(Ljava/lang/Object;[Ljava/lang/Class;)Ljava/lang/Object;");
						
						String tt = to.getBytecodeType();
						tt = tt.substring(1, tt.length()-1);
						mv.visitTypeInsn(CHECKCAST, tt);
					}
				}
				else{//MHA: this code is a bit messy
					to = createRefInline(mv, bcv, from, to, supressRefGenneration);
				}
				
				//}
				//else{
					//int cnt = TypeCheckUtils.getRefLevels(to)-TypeCheckUtils.getRefLevels(from);
					//for(int n= 0; n < cnt; n++ ){
						//createRefInline(mv, bcv, from, to, supressRefGenneration);
					//}
				//}
			}
			else if(from instanceof PrimativeType && ((PrimativeType)from).type != PrimativeTypeEnum.LAMBDA && !from.hasArrayLevels())
			{
				//convert rhs from primative to object
				PrimativeTypeEnum fromPrim = ((PrimativeType)from).type;
				String toThing = to.getCheckCastType();
				//toThing = toThing.substring(1, toThing.length()-1);
				
				toThing = checkValidBoxedType(toThing, fromPrim);//if wanted tpye is Object then this will convert an int to Integer (and not try to convert to default short - which would be the case without this call)
				
				//if(!"java/lang/Number".equals(toThing)){
					switch(toThing)
					{//first call permits int -> Long convertion as: Long.valueOf((long)int);
						case "java/lang/Boolean": applyCastPrimativeType(mv, fromPrim, PrimativeTypeEnum.BOOLEAN); mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;"); break;
						case "java/lang/Integer":     applyCastPrimativeType(mv, fromPrim, PrimativeTypeEnum.INT); mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;"); break;
						case "java/lang/Long":    applyCastPrimativeType(mv, fromPrim, PrimativeTypeEnum.LONG); mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;"); break;
						case "java/lang/Float":   applyCastPrimativeType(mv, fromPrim, PrimativeTypeEnum.FLOAT); mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;"); break;
						case "java/lang/Double":  applyCastPrimativeType(mv, fromPrim, PrimativeTypeEnum.DOUBLE); mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;"); break;
						case "java/lang/Byte":    applyCastPrimativeType(mv, fromPrim, PrimativeTypeEnum.BYTE); mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;"); break;
						case "java/lang/Character":    applyCastPrimativeType(mv, fromPrim, PrimativeTypeEnum.CHAR); mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;"); break;
						default :     applyCastPrimativeType(mv, fromPrim, PrimativeTypeEnum.SHORT); mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;"); break; //short
					}
				//}
			}
			//meh, objects are objects
		}
		
		if(TypeCheckUtils.typeRequiresLocalArrayConvertion(to)){
			//thing was a X[] array, but we've passed in a int:Ref[] - this is exposed as a refarray internally to conc (so we can keep the type information at runtime)
			//we need to extract ar from refarray
			mv.visitFieldInsn(GETFIELD, "com/concurnas/bootstrap/runtime/ref/LocalArray", "ar", "[Ljava/lang/Object;");
			//GETFIELD com/concurnas/bootstrap/runtime/ref/LocalArray.ar : [Ljava/lang/Object;
			//to.setOrigonalGenericTypeUpperBound(null);
		}
		
		if(from!= null && to != null && !from.equals(to) && !from.hasArrayLevels() && !to.hasArrayLevels() && from instanceof NamedType && to instanceof NamedType) {
			String fromCls = ((NamedType) from).getSetClassDef().toString();
			String toCls = ((NamedType) to).getSetClassDef().toString();
			if(	boxedTypes.containsKey(fromCls) && boxedTypes.containsKey(toCls)) {
				//from one boxed type to another...
				PrimativeType unboxedFrom = unbox(mv, from, bcv);
				applyCastExplicit(mv, unboxedFrom, to, bcv);
			}
			
		}
		
		
		return to;
	}
	
	private static ClassDef const_obj = new ClassDefJava(Object.class);
	
	public static void applyCastExplicit(BytecodeOutputter mv, Type from, Type to, BytecodeGennerator bcv)
	{
		if(from == null || from.equals(to)){//from null shouldnt really happen...
			return;
		}
		
		if(from instanceof MultiType) {
			from = from.getTaggedType();
		}
		
		if(to instanceof MultiType) {
			to = to.getTaggedType();
		}		
		
		if(to.hasArrayLevels() && to instanceof NamedType )			{
			ClassDef toSet = ((NamedType)to).getSetClassDef();
			if( toSet==null || toSet.equals(const_obj)  ){
				if(TypeCheckUtils.hasRefLevelsAndIsArray(from)){
					//special case to deal with this: xxx = [1!,2!] as Object[]
					mv.visitFieldInsn(GETFIELD, "com/concurnas/bootstrap/runtime/ref/LocalArray", "ar", "[Ljava/lang/Object;");
				}
				else if( from instanceof NamedType && ((NamedType)from).getSetClassDef().equals(const_obj)  ){
					//special case to deal with this: xxx = [1!,2!] as Object; xxx2 = xxx as Object[]
					
					//check if localarray if is, then extract
					
					mv.visitInsn(Opcodes.DUP);
					mv.visitTypeInsn(INSTANCEOF, "com/concurnas/bootstrap/runtime/ref/LocalArray");
					Label onDone = new Label();
					mv.visitJumpInsn(IFEQ, onDone);
					
					mv.visitTypeInsn(CHECKCAST, "com/concurnas/bootstrap/runtime/ref/LocalArray");
					mv.visitFieldInsn(GETFIELD, "com/concurnas/bootstrap/runtime/ref/LocalArray", "ar", "[Ljava/lang/Object;");
					
					mv.visitLabel(onDone);
				}
			}
			
		}
		
		if((TypeCheckUtils.getRefLevels(from) > TypeCheckUtils.getRefLevels(to) ) )
		{//TODO: maybe remove the explicit check for lockness
			if(TypeCheckUtils.hasRefLevelsAndNotLocked(from)) {
				Type fro2 = Utils.unref(mv, from, bcv);
				applyCastExplicit(mv, fro2, to, bcv);
				return;
			}else if(!to.equals(ScopeAndTypeChecker.const_object)){
				from = (Type)from.copy();
				TypeCheckUtils.unlockAllNestedRefs(from);
				Type fro2 = Utils.unref(mv, from, bcv);
				applyCastExplicit(mv, fro2, to, bcv);
				return;
			}
		}
		
		if( TypeCheckUtils.getRefLevels(to) > TypeCheckUtils.getRefLevels(from)  ){//create new ref
			//TODO: refactor so u can do 78 as Integer::
			createRefInline(mv, bcv, from, to, false);
		}
		
		if(from instanceof PrimativeType && to instanceof PrimativeType )
		{
			PrimativeTypeEnum fromPrim = ((PrimativeType)from).type;
			PrimativeTypeEnum toPrim = ((PrimativeType)to).type;
			applyCastPrimativeType(mv, fromPrim, toPrim);
		}
		else
		{
			//String genType = to.getNonGenericPrettyName().replace('.', '/');/*"java/util/List"*/
			
			if(to instanceof PrimativeType)
			{//MHA: copy paste yuck
				PrimativeTypeEnum toPrim = ((PrimativeType)to).type;
				if(from instanceof PrimativeType){
					PrimativeTypeEnum fromPrim = ((PrimativeType)from).type;
					applyCastPrimativeType(mv, fromPrim, toPrim);
				}
				else if(from instanceof NamedType){
					
					if(toPrim == PrimativeTypeEnum.LAMBDA ){
						mv.visitTypeInsn(CHECKCAST,"com/concurnas/bootstrap/lang/Lambda" );//vs top of stack
					}
					else if(from.equals(ScopeAndTypeChecker.const_Number)){
						convertFromNumberToPrimative(mv, toPrim);
					}
					else if(to.hasArrayLevels()){
						mv.visitTypeInsn(CHECKCAST, to.getCheckCastType() );//vs top of stack
					}
					else{
						if(from.equals(ScopeAndTypeChecker.const_object)){
							from = primToBoxedType(((PrimativeType)to).type);
							
							mv.visitTypeInsn(CHECKCAST, from.getCheckCastType() );
						}
						
						PrimativeType got = (PrimativeType)unbox(mv, from,  bcv);
						if(null != got){
							applyCastPrimativeType(mv, got.type, toPrim);
						}
					}
				}
			}
			else{
				if(to instanceof NamedType)
				{
					String toBoxed = ((NamedType) to).getSetClassDef() == null ? null:((NamedType) to).getSetClassDef().toString();
					if(from instanceof NamedType && ((NamedType) from).getSetClassDef() != null && boxedTypes.containsKey(((NamedType) from).getSetClassDef().toString()) ){
						if( boxedTypes.containsKey(toBoxed) ) {
							//unbox first and cast up...
							PrimativeType unboxedFrom = unbox(mv, from, bcv);
							
							applyCastExplicit(mv, unboxedFrom, to, bcv);
							return;
						}
					}

					
					if(from instanceof PrimativeType && !from.hasArrayLevels() ){
						//convert to appropriate primitive type for boxed type 
						if( boxedTypes.containsKey(toBoxed) ) {
							PrimativeTypeEnum fromPrim = ((PrimativeType)from).type;
							PrimativeTypeEnum toPrim = boxedTypes.get(toBoxed);
							applyCastPrimativeType(mv, fromPrim, toPrim);
							box(mv, new PrimativeType(toPrim));
							return;
						}else {
							//else, but box up from and hope..
							box(mv, from);
						}
					}
									
					//NamedType tnt = (NamedType)to;
					//genType = tnt.getBytecodeType();
					//genType = genType.substring(1, genType.length()-1);
				}
				
				String checkT = to.getCheckCastType();
				if(!checkT.equals("java/lang/Object")){
					//mv.visitTypeInsn(CHECKCAST, genType );//vs top of stack
					mv.visitTypeInsn(CHECKCAST,checkT );//vs top of stack
				}
			}
		}
	}
	
	public static NamedType primToBoxedType(PrimativeTypeEnum from){
		switch(from){
			 case BOOLEAN: return ScopeAndTypeChecker.const_boolean_nt;
			 case INT: return ScopeAndTypeChecker.const_integer_nt;
			 case LONG: return ScopeAndTypeChecker.const_long_nt;
			 case FLOAT: return ScopeAndTypeChecker.const_float_nt;
			 case DOUBLE: return ScopeAndTypeChecker.const_double_nt;
			 case SHORT: return ScopeAndTypeChecker.const_short_nt;
			 case BYTE: return ScopeAndTypeChecker.const_byte_nt;
			 case CHAR: return ScopeAndTypeChecker.const_char_nt;
			 case VOID: return ScopeAndTypeChecker.const_void_nt;
			 case LAMBDA: return ScopeAndTypeChecker.const_lambda_nt;
		}
		return null;
	}
	
	private static void convertFromNumberToPrimative(BytecodeOutputter mv, PrimativeTypeEnum toPrim){
		switch(toPrim){
			case BYTE : mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "byteValue", "()B"); break;
			case DOUBLE : mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D"); break;
			case FLOAT : mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F"); break;
			case LONG : mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J"); break;
			case SHORT : mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "shortValue", "()S"); break;
			default : mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I");
		}
	}
	

	public static void applyStore(BytecodeOutputter mv, Type varType, int space) {
		
		if( truePrimative(varType))
		{
			PrimativeTypeEnum tt = ((PrimativeType)varType).type;
			switch(tt)
			{
				case LONG: mv.visitVarInsn(Opcodes.LSTORE, space); break;
				case FLOAT:mv.visitVarInsn(Opcodes.FSTORE, space); break;
				case DOUBLE: mv.visitVarInsn(Opcodes.DSTORE, space); break;
				default: mv.visitVarInsn(Opcodes.ISTORE, space); break;//int etc
			}
		}
		else
		{//object
			mv.visitVarInsn(Opcodes.ASTORE, space);
		}
		//TODO: optimize to make use of istore_1 etc
	}
	
	public static int applyLoad(BytecodeOutputter mv, Type varType, int space) {
		if( truePrimative(varType))
		{
			PrimativeTypeEnum tt = ((PrimativeType)varType).type;
			switch(tt)
			{
				case LONG: mv.visitVarInsn(Opcodes.LLOAD, space); return 2;
				case FLOAT:mv.visitVarInsn(Opcodes.FLOAD, space); break;
				case DOUBLE: mv.visitVarInsn(Opcodes.DLOAD, space); return 2;
				default: mv.visitVarInsn(Opcodes.ILOAD, space); break;//int etc
			}
		}
		else
		{//object
			mv.visitVarInsn(Opcodes.ALOAD, space);
		}
		
		return 1;
	}

	public static void applyMuler(BytecodeOutputter mv, PrimativeType type, MulerExprEnum mulOper) {
		
		PrimativeTypeEnum tt = type.type;
		
		if(tt == PrimativeTypeEnum.LONG)
		{
			switch(mulOper)
			{
				case DIV: mv.visitInsn(Opcodes.LDIV); break;
				case MOD: mv.visitInsn(Opcodes.LREM); break;
				default: mv.visitInsn(Opcodes.LMUL); break;//mul
			}
		}
		else if (tt == PrimativeTypeEnum.FLOAT)
		{
			switch(mulOper)
			{
				case DIV: mv.visitInsn(Opcodes.FDIV); break;
				case MOD: mv.visitInsn(Opcodes.FREM); break;
				default: mv.visitInsn(Opcodes.FMUL); break;//mul
			}
		}
		else if (tt == PrimativeTypeEnum.DOUBLE)
		{
			switch(mulOper)
			{
				case DIV: mv.visitInsn(Opcodes.DDIV); break;
				case MOD: mv.visitInsn(Opcodes.DREM); break;
				default: mv.visitInsn(Opcodes.DMUL); break;//mul
			}
		}
		else
		{//int et al.
			switch(mulOper)
			{
				case DIV: mv.visitInsn(Opcodes.IDIV); break;
				case MOD: mv.visitInsn(Opcodes.IREM); break;
				default: mv.visitInsn(Opcodes.IMUL); break;//mul
			}
		}
	}
	public static void applyShift(BytecodeOutputter mv, PrimativeType type, ShiftOperatorEnum  mulOper) {
		if(type.type == PrimativeTypeEnum.LONG){
			switch(mulOper){
				case LS: mv.visitInsn(LSHL); break;
				case RS: mv.visitInsn(LSHR); break;
				case URS: mv.visitInsn(LUSHR); break;
			}
		}else{//int 32 bit max thing (inc byte etc)
			switch(mulOper){
				case LS: mv.visitInsn(ISHL); break;
				case RS: mv.visitInsn(ISHR); break;
				case URS: mv.visitInsn(IUSHR); break;
			}
		}
	}
	public static void applyBitwise(BytecodeOutputter mv, PrimativeType type, BitwiseOperationEnum  bitwiseOper) {
		if(type.type == PrimativeTypeEnum.LONG){
			switch(bitwiseOper){
				case AND: mv.visitInsn(LAND); break;
				case OR: mv.visitInsn(LOR); break;
				case XOR: mv.visitInsn(LXOR); break;
			}
		}else{//int 32 bit max thing (inc byte etc)
			switch(bitwiseOper){
				case AND: mv.visitInsn(IAND); break;
				case OR: mv.visitInsn(IOR); break;
				case XOR: mv.visitInsn(IXOR); break;
			}
		}
	}
	
	public static Label applyInfixAndOrPart1(BytecodeOutputter mv, boolean isAnd) {
		Label ifFalse = new Label();
		if(isAnd){
			mv.visitJumpInsn(IFEQ, ifFalse);
		}
		else{//or
			mv.visitJumpInsn(IFNE, ifFalse);
		}
		
		return ifFalse;
	}
	
	public static void applyInfixAndOrPart2(BytecodeOutputter mv, boolean isAnd, Label ifFalse) {
		
		Label after = new Label();
		if(isAnd){
			mv.visitJumpInsn(IFEQ, ifFalse);
			mv.visitInsn(ICONST_1);
			
			mv.visitJumpInsn(GOTO, after);
			mv.visitLabel(ifFalse);
			mv.visitInsn(ICONST_0);
			
		}else{
			mv.visitJumpInsn(IFNE, ifFalse);
			mv.visitInsn(ICONST_0);
			
			mv.visitJumpInsn(GOTO, after);
			mv.visitLabel(ifFalse);
			mv.visitInsn(ICONST_1);
		}

		mv.visitLabel(after);
	}
	
	public static void applyDup(BytecodeOutputter mv, Type top)
	{
		if(top.hasArrayLevels() || ( top instanceof PrimativeType && (((PrimativeType)top).type ==PrimativeTypeEnum.DOUBLE || ((PrimativeType)top).type ==PrimativeTypeEnum.LONG )))
		{
			mv.visitInsn(Opcodes.DUP2);
		}
		else
		{
			mv.visitInsn(Opcodes.DUP);
		}
	}
	
	public static void applyDupX2(BytecodeOutputter mv, Type top)
	{
		if(top instanceof PrimativeType && (((PrimativeType)top).type ==PrimativeTypeEnum.DOUBLE || ((PrimativeType)top).type ==PrimativeTypeEnum.LONG ))
		{
			mv.visitInsn(Opcodes.DUP2_X2);
		}
		else
		{
			mv.visitInsn(Opcodes.DUP_X2);
		}
	}

	public static void applyDupX1(BytecodeOutputter mv, Type top)
	{
		//mv.visitInsn(Opcodes.DUP_X1);
		
		if(top instanceof PrimativeType && (((PrimativeType)top).type ==PrimativeTypeEnum.DOUBLE || ((PrimativeType)top).type ==PrimativeTypeEnum.LONG ))
		{
			mv.visitInsn(Opcodes.DUP2_X1);
		}
		else
		{
			mv.visitInsn(Opcodes.DUP_X1);
		}
	}
	
	public static void popFromStack(BytecodeOutputter mv, Type top) {
		
		//mv.clearPendingCheckCast();
		Type upperBound = top.getOrigonalGenericTypeUpperBound();
		if(null != upperBound){
			top = upperBound;//useful for when doing put on a map say, e.g. mymap.put(x, y), and y is a long or long[] (as this is generic we use the generic pop not pop2)
		}
		
		if(!top.hasArrayLevels() && top instanceof PrimativeType && (((PrimativeType)top).type ==PrimativeTypeEnum.DOUBLE || ((PrimativeType)top).type ==PrimativeTypeEnum.LONG ))
		{
			mv.visitInsn(Opcodes.POP2);
		}
		else
		{
			mv.visitInsn(Opcodes.POP);
		}
		
	}
	
	@SuppressWarnings("incomplete-switch")
	public static Type box(BytecodeOutputter mv, Type from)
	{//TODO: what about boxing a lambda? is this even possible?
		if(from instanceof PrimativeType && !from.hasArrayLevels()){
			switch(((PrimativeType)from).type){
				case BOOLEAN: mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;"); from = new NamedType(new ClassDefJava(Boolean.class)); break;
				case INT:     mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;"); from = new NamedType(new ClassDefJava(Integer.class)); break;
				case LONG:    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;"); from = new NamedType(new ClassDefJava(Long.class)); break;
				case FLOAT:   mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;"); from = new NamedType(new ClassDefJava(Float.class)); break;
				case DOUBLE:  mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;"); from = new NamedType(new ClassDefJava(Double.class)); break;
				case BYTE:    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;"); from = new NamedType(new ClassDefJava(Byte.class)); break;
				case CHAR:     mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;"); from = new NamedType(new ClassDefJava(Character.class)); break;
				case SHORT :     mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;"); from = new NamedType(new ClassDefJava(Short.class)); break; //short
			}
		}
		return from;
	}
	
	@SuppressWarnings("incomplete-switch")
	public static void typeForPrimative(BytecodeOutputter mv, Type from)
	{
		if(from instanceof PrimativeType && !from.hasArrayLevels()){
			switch(((PrimativeType)from).type){
			case BOOLEAN: mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "TYPE", "Ljava/lang/Class;"); break;
			case INT:     mv.visitFieldInsn(GETSTATIC, "java/lang/Integer", "TYPE", "Ljava/lang/Class;"); break;
			case LONG:    mv.visitFieldInsn(GETSTATIC, "java/lang/Long", "TYPE", "Ljava/lang/Class;"); break;
			case FLOAT:   mv.visitFieldInsn(GETSTATIC, "java/lang/Float", "TYPE", "Ljava/lang/Class;"); break;
			case DOUBLE:  mv.visitFieldInsn(GETSTATIC, "java/lang/Double", "TYPE", "Ljava/lang/Class;"); break;
			case BYTE:    mv.visitFieldInsn(GETSTATIC, "java/lang/Byte", "TYPE", "Ljava/lang/Class;"); break;
			case CHAR:    mv.visitFieldInsn(GETSTATIC, "java/lang/Character", "TYPE", "Ljava/lang/Class;"); break;
			case SHORT :  mv.visitFieldInsn(GETSTATIC, "java/lang/Short", "TYPE", "Ljava/lang/Class;"); break;
			}
		}
	}

	public static NamedType const_obj_nt = new NamedType(const_obj);
	
	public static Type unref(BytecodeOutputter mv, Type from, Type to, BytecodeGennerator bcv){
		return unref(mv,  from, TypeCheckUtils.getRefLevels(to),  bcv, false);
	}
	
	public static Type unref(BytecodeOutputter mv, Type from, BytecodeGennerator bcv){
		return unref(mv, from, 0, bcv, false);
	}
	
	public static Type unref(BytecodeOutputter mv, Type from, int toLevels, BytecodeGennerator bcv, boolean dupOnPenultimate){
		return unref(mv, from, toLevels, bcv, dupOnPenultimate, -1);
	}
	
	public static Type unref(BytecodeOutputter mv, Type from, int toLevels, BytecodeGennerator bcv, boolean dupOnPenultimate, int storeLastRefTo){
		
		if(TypeCheckUtils.hasRefLevelsAndNotLocked(from)){
			int fromLevels = (toLevels > 0) ? TypeCheckUtils.getRefLevels(from): TypeCheckUtils.getRefLevelsIfNoeLockedAsRef(from);
			
			while(fromLevels-- > toLevels){
				//if null ret null and dont call get
				
				boolean isRefArrayGettable = fromLevels >= 1 && TypeCheckUtils.isRefArrayGettable(from, -1);
				if(isRefArrayGettable){
					fromLevels = 0;
				}
				
				NamedType asFromNamed = (NamedType)from;
				ClassDef cls = asFromNamed.getSetClassDef();
				String ownerClassName = cls.bcFullName();
				HashSet<TypeAndLocation> getFuncs = asFromNamed.getFuncDef(0,0,"get", new ArrayList<Type>(0), null, null);//JPT: should this be here? Better at SATC time?
				
				FuncType getFunc = null;
				for(TypeAndLocation tal : getFuncs){
					FuncType can = (FuncType)tal.getType();
					if(can.inputs.size()==0){
						getFunc = can;
						break;
					}
				}
				
				/*Type upper = getFunc.retType.getOrigonalGenericTypeUpperBound();
				String genericReturnTye;
				if(null !=upper){
					genericReturnTye = upper.getGenericBytecodeType();
				}
				else{
					genericReturnTye = getFunc.retType.getGenericBytecodeType();
				}*/
				
				String genericReturnTye = getFunc.retType.getOrigonalGenericTypeUpperBound().getGenericBytecodeType();
				
				from = getFunc.retType;//asFromNamed.getGenTypes().get(0);
				
				//int tempSlot = bcv.createNewLocalVar(bcv.getTempVarName(), const_Obj_nt, false);
				//mv.visitInsn(Opcodes.DUP);
				boolean alreadydub=false;
				if(dupOnPenultimate && fromLevels==toLevels){
					mv.visitInsn(Opcodes.DUP);
					alreadydub=true;
				}
				
				if(storeLastRefTo > -1 && fromLevels==toLevels){
					if(!alreadydub) {
						mv.visitInsn(Opcodes.DUP);
					}
					
					mv.visitVarInsn(ASTORE, storeLastRefTo);
				}
				
				//mv.visitVarInsn(ASTORE, tempSlot);
				
				//Label itsNotNull = new Label();
				//mv.visitJumpInsn(IFNONNULL, itsNotNull);
				//mv.visitInsn(ACONST_NULL);
				//Label after = new Label();
				//mv.visitJumpInsn(GOTO, after);
				
				//mv.visitLabel(itsNotNull);
							
				//mv.visitVarInsn(ALOAD, tempSlot);

				mv.visitMethodInsn(cls.isInterface()?INVOKEINTERFACE:INVOKEVIRTUAL, ownerClassName, "get", "()"+genericReturnTye);
				Type returnType=getFunc.retType;
				if(TypeCheckUtils.typeRequiresLocalArrayConvertion(returnType)){
					if(TypeCheckUtils.typeRequiresLocalArrayConvertion(returnType)){
						Utils.createRefArray(mv, (NamedType)returnType);
					}
				}else{
					String hhh = returnType.getCheckCastType();
					mv.visitTypeInsn(CHECKCAST, hhh);
				}
				
				//mv.visitLabel(after);
				//from = ((NamedType)from).getGenTypes().get(0);
			}
		}
		from.setOrigonalGenericTypeUpperBound(null);//MAYBE: this may cause some problems
		
		return from;
	}
	
	public static PrimativeType unbox(BytecodeOutputter mv, Type from, BytecodeGennerator bcv)
	{
		
		if(from instanceof MultiType) {
			from = from.getTaggedType();
		}
		
		if(TypeCheckUtils.hasRefLevelsAndNotLocked(from)){
			return unbox(mv, unref(mv, from, bcv), bcv);
		}
		
		if(from instanceof GenericType) {
			from = ((GenericType)from).getOrigonalGenericTypeUpperBoundRaw();
			if(from == null) {
				return null;
			}
		}
		
		if(from instanceof NamedType)
		{
			from=(NamedType)from.copy();//JPT this is so that getBytecodeTypeWithoutArray prodces the right thing - ugly... should have genneral method for this...
			from.setOrigonalGenericTypeUpperBound(null);
			String genericName = from.getGenericBytecodeType();
			if(genericName.equals("*")) {
				return null;
			}
			
			
			genericName = genericName.substring(1, genericName.length() - 1);
			String boxedUpObject = ((NamedType) from).getBytecodeTypeWithoutArray();// .getPrettyName().replace('.', '/');
			boxedUpObject = boxedUpObject.substring(1, boxedUpObject.length()-1);
			
			if(!genericName.equals(boxedUpObject))
			{
				mv.visitTypeInsn(CHECKCAST, boxedUpObject);
			}
			
			PrimativeTypeEnum fromPrim;
			switch(boxedUpObject)
			{
				case "java/lang/Boolean":   mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z"); fromPrim = PrimativeTypeEnum.BOOLEAN; break;
				case "java/lang/Byte":      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B");	   fromPrim = PrimativeTypeEnum.BYTE; break;
				case "java/lang/Character": mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C");  fromPrim = PrimativeTypeEnum.CHAR; break;
				case "java/lang/Float":     mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F"); 	   fromPrim = PrimativeTypeEnum.FLOAT; break;
				case "java/lang/Double":    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D");   fromPrim = PrimativeTypeEnum.DOUBLE; break;
				case "java/lang/Integer":   mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");     fromPrim = PrimativeTypeEnum.INT; break;
				case "java/lang/Long":      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J");	   fromPrim = PrimativeTypeEnum.LONG; break;
				case "java/lang/Short":     mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S");     fromPrim = PrimativeTypeEnum.SHORT; break; //"java/lang/Short"
				default :                   return null;/*throw new RuntimeException("unable to perfom unbox operation on type: " + boxedUpObject);*/
			}
			PrimativeType prim =  new PrimativeType(fromPrim);
			prim.setArrayLevels(from.getArrayLevels());
			return prim;
		}
		else
		{
			return (PrimativeType)from;
		}
	}
	
	public static void applyPlusMinusPrim(BytecodeOutputter mv, boolean isPlusOperation, PrimativeType lhs) {
		
		
		if(isPlusOperation)
		{
			switch(lhs.type)
			{
				case LONG  : mv.visitInsn(Opcodes.LADD); break;
				case FLOAT : mv.visitInsn(Opcodes.FADD); break;
				case DOUBLE: mv.visitInsn(Opcodes.DADD); break;
				default   : mv.visitInsn(Opcodes.IADD); break;
			}
		}
		else
		{//minus
			switch(lhs.type)
			{
				case LONG  : mv.visitInsn(Opcodes.LSUB); break;
				case FLOAT : mv.visitInsn(Opcodes.FSUB); break;
				case DOUBLE: mv.visitInsn(Opcodes.DSUB); break;
				default   : mv.visitInsn(Opcodes.ISUB); break;
			}
		}
	}

	public static boolean truePrimative(Type varType)
	{
		return varType instanceof PrimativeType && PrimativeTypeEnum.LAMBDA != ((PrimativeType)varType).type && !varType.hasArrayLevels();
	}
	
	public static void singleLevelArrayConst(BytecodeOutputter mv, Type consType) {
		if(consType instanceof PrimativeType )
		//if(consType instanceof PrimativeType && PrimativeTypeEnum.LAMBDA != ((PrimativeType)consType).type && consType.getArrayLevels() == 1)
		{
			PrimativeTypeEnum ee = ((PrimativeType)consType).type;
			int op;
			switch(ee)
			{
				case BOOLEAN: op = Opcodes.T_BOOLEAN; break; 
				case CHAR: op = Opcodes.T_CHAR; break; 
				case FLOAT: op = Opcodes.T_FLOAT; break; 
				case DOUBLE: op = Opcodes.T_DOUBLE; break; 
				case SHORT: op = Opcodes.T_SHORT; break; 
				case INT: op = Opcodes.T_INT; break; 
				case BYTE: op = Opcodes.T_BYTE; break; 
				default: op = Opcodes.T_LONG; break;//long 
			}
			
			 mv.visitIntInsn(Opcodes.NEWARRAY, op);
		}
		else
		{
			if( (consType instanceof NamedType && ((NamedType)consType).getIsRef() )){
				consType = ((NamedType)consType).copyTypeSpecific();
				consType.setArrayLevels(0);
			}
			String tt = consType.getBytecodeType();// -> [Ljava/lang/Object;		//java.lang.String[]	
			tt = tt.substring(1, tt.length()-1);
			if(consType.hasArrayLevels() && (consType.getOrigonalGenericTypeUpperBoundRaw() == null || consType instanceof GenericType)){
				tt = tt.substring(1, tt.length());
			}
			
			mv.visitTypeInsn(Opcodes.ANEWARRAY, tt);
		}
	}

	public static void extractAndCastArrayRef(BytecodeOutputter mv, Type origType) {
		//Bug Here999
		mv.visitFieldInsn(GETFIELD, "com/concurnas/bootstrap/runtime/ref/LocalArray", "ar", "[Ljava/lang/Object;");
		
		String xxx;
		
		if(origType.getArrayLevels() >= 2){
			xxx = "com/concurnas/bootstrap/runtime/ref/LocalArray";
		}
		else{
			xxx = ((NamedType)origType).getSetClassDef().bcFullName();
		}
		 
		mv.visitTypeInsn(CHECKCAST, "[L"+xxx+";");
	}
	
	public static void applyArrayLoad(BytecodeOutputter mv, Type origType) {
		if(origType instanceof PrimativeType && origType.getArrayLevels() == 1 )
		{
			PrimativeTypeEnum ee = ((PrimativeType)origType).type;
			int op;
			switch(ee)
			{
				case BOOLEAN: op = Opcodes.BALOAD; break; 
				case CHAR: op = Opcodes.CALOAD; break; 
				case FLOAT: op = Opcodes.FALOAD; break; 
				case DOUBLE: op = Opcodes.DALOAD; break; 
				case SHORT: op = Opcodes.SALOAD; break; 
				case INT: op = Opcodes.IALOAD; break; 
				case BYTE: op = Opcodes.BALOAD; break; 
				default: op = Opcodes.LALOAD; break;//long 
			}
			mv.visitInsn(op);
			
		}
		else
		{//HERE BE PROBLEMS
			if(TypeCheckUtils.hasRefLevelsAndIsArray(origType)){
				mv.visitInsn(Opcodes.SWAP);
				extractAndCastArrayRef(mv, origType);
				mv.visitInsn(Opcodes.SWAP);
			}
			
			mv.visitInsn(Opcodes.AALOAD);
		}
	}
	
	public static void applyArrayStore(BytecodeOutputter mv, Type origType, BytecodeGennerator bcv) {
		applyArrayStore( mv,  origType,  bcv, false);
	}
	
	public static void applyArrayStore(BytecodeOutputter mv, Type origType, BytecodeGennerator bcv, boolean onInit) {
		if(origType instanceof PrimativeType && origType.getArrayLevels() == 1 )
		{
			PrimativeTypeEnum ee = ((PrimativeType)origType).type;
			int op;
			switch(ee)
			{
				case BOOLEAN: op = Opcodes.BASTORE; break; 
				case CHAR: op = Opcodes.CASTORE; break; 
				case FLOAT: op = Opcodes.FASTORE; break; 
				case DOUBLE: op = Opcodes.DASTORE; break; 
				case SHORT: op = Opcodes.SASTORE; break; 
				case INT: op = Opcodes.IASTORE; break; 
				case BYTE: op = Opcodes.BASTORE; break; 
				default: op = Opcodes.LASTORE; break;//long 
			}
			mv.visitInsn(op);
			
			
		}
		else
		{
			if(!onInit && TypeCheckUtils.hasRefLevelsAndIsArray(origType)){
				int tempSlot = bcv.createNewLocalVar(bcv.getTempVarName(), origType, false);
				mv.visitVarInsn(ASTORE, tempSlot);
				
				mv.visitInsn(Opcodes.SWAP);
				extractAndCastArrayRef(mv, origType);
				//mv.visitFieldInsn(GETFIELD, "com/concurnas/bootstrap/runtime/ref/LocalArray", "ar", "[Ljava/lang/Object;");
				//mv.visitTypeInsn(CHECKCAST, "[Lcom/concurnas/runtime/ref/Local;");
				mv.visitInsn(Opcodes.SWAP);
				
				mv.visitVarInsn(ALOAD, tempSlot);
			}
			
			mv.visitInsn(Opcodes.AASTORE);
		}
	}

	public static void createArray(BytecodeOutputter mv, int siz, Type consType, boolean isdecl) {
		if(siz > 1 )
		{
			consType.setArrayLevels(consType.getArrayLevels()-1);
			
			//String arName = consType.getBytecodeType();
			//mv.visitMultiANewArrayInsn("[[I", siz);
			//mv.visitMultiANewArrayInsn(arName, siz - (isdecl?1:0));
			
			//String tt = consType.getBytecodeType();// -> [Ljava/lang/Object;
			
			String xxx = consType.getBytecodeType();
			if(xxx.startsWith("L")){
				xxx = xxx.substring(1, xxx.length()-1);
			}
			mv.visitTypeInsn(Opcodes.ANEWARRAY, xxx);
			
			consType.setArrayLevels(consType.getArrayLevels()+1);//just in case
		}
		else
		{
			singleLevelArrayConst(mv, consType);
		}
	}
	
	public static void createNakedArray(BytecodeOutputter mv, int siz, Type consType) {
		int typeSize = consType.getArrayLevels();
		if(siz == typeSize){
			if(siz > 1 ){
				mv.visitMultiANewArrayInsn(consType.getBytecodeType(), siz );
			} else {
				singleLevelArrayConst(mv, consType);
			}
		}else{
			if(siz >= 2){
				mv.visitMultiANewArrayInsn(consType.getBytecodeType(), siz);
			}else{
				consType = (Type)consType.copy();
				consType.setArrayLevels(consType.getArrayLevels() - siz);
				String xx = consType.getBytecodeType();
				if(xx.endsWith(";") && xx.startsWith("L")) {
					xx = xx.substring(1, xx.length()-1);
				}
				mv.visitTypeInsn(ANEWARRAY, xx);
			}
		}
	}
	

	public static Type copyRemoveGenericUpperBound(Type taggedType) {
		Type ret = (Type)taggedType.copy();
		/*if(ret instanceof NamedType){
			((NamedType)ret).origonalGenericTypeUpperBound=null;
		}*/
		ret.setOrigonalGenericTypeUpperBound(null);
		return ret;
	}

	public static ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> filterOutNameFromtuples(ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> splicedInAsTypeAndStr, Set<String> stuffToNull) {
		ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> ret = new ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>(splicedInAsTypeAndStr.size());
		for( Sixple<Type, String, Annotations, Boolean, Boolean, Boolean> xx : splicedInAsTypeAndStr){
			if(!stuffToNull.contains(xx.getB())){
				ret.add(xx);
			}
		}
		return ret;
	}
	
	public static final PrimativeType const_byte = new PrimativeType(PrimativeTypeEnum.BYTE);
	public static final PrimativeType const_char = new PrimativeType(PrimativeTypeEnum.CHAR);
	public static final PrimativeType const_double = new PrimativeType(PrimativeTypeEnum.DOUBLE);
	public static final PrimativeType const_float = new PrimativeType(PrimativeTypeEnum.FLOAT);
	public static final PrimativeType const_int = new PrimativeType(PrimativeTypeEnum.INT);
	public static final PrimativeType const_long = new PrimativeType(PrimativeTypeEnum.LONG);
	public static final PrimativeType const_short = new PrimativeType(PrimativeTypeEnum.SHORT);
	public static final PrimativeType const_bool = new PrimativeType(PrimativeTypeEnum.BOOLEAN);
	

	public static Type getTypeFromDesc(String typeDesc) {
		if(typeDesc.length()==1){
			switch(typeDesc){
				case "B": return const_byte;
				case "C": return const_char;
				case "D": return const_double;
				case "F": return const_float;
				case "I": return const_int;
				case "J": return const_long;
				case "S": return const_short;
				case "Z": return const_bool;
			}
		}
		return const_obj_nt;
	}

	public static int getAnnotationDependantExtraModifiers(Annotations annots) {
		if(null != annots){
			boolean deprecated = false;
			for(com.concurnas.compiler.ast.Annotation ano : annots.annotations){
				if(ScopeAndTypeChecker.const_Annotation_DeprecatedCls.equals(ano.getTaggedType())){
					deprecated=true;
					break;
				}
			}
			if(deprecated){
				return Opcodes.ACC_DEPRECATED;
			}
		}
		return 0;
	}
}
