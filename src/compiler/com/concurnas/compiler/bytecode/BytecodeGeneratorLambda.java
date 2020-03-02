package com.concurnas.compiler.bytecode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.ast.AbstractType;
import com.concurnas.compiler.ast.AccessModifier;
import com.concurnas.compiler.ast.Annotations;
import com.concurnas.compiler.ast.AsyncBlock;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.FuncParam;
import com.concurnas.compiler.ast.FuncRef;
import com.concurnas.compiler.ast.FuncRefArgs;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.IsAMapElement;
import com.concurnas.compiler.ast.LambdaDef;
import com.concurnas.compiler.ast.MapDef;
import com.concurnas.compiler.ast.MapDefElement;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.PrimativeType;
import com.concurnas.compiler.ast.PrimativeTypeEnum;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.VarNull;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.bytecode.FuncLocation.StaticFuncLocation;
import com.concurnas.compiler.typeAndLocation.Location;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.DefaultMapConvertionChecker;
import com.concurnas.compiler.utils.Fiveple;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.utils.Sixple;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.visitors.AbstractVisitor;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.compiler.visitors.Unskippable;
import com.concurnas.compiler.visitors.Utils.CurriedVararg;
import com.concurnas.runtime.Pair;

public class BytecodeGeneratorLambda extends AbstractVisitor implements Opcodes, Unskippable {

	private BytecodeGennerator bytecodeVisitor;

	public BytecodeGeneratorLambda(BytecodeGennerator bytecodeVisitor) {
		this.bytecodeVisitor = bytecodeVisitor;
	}

	private final static Type const_void = new PrimativeType(PrimativeTypeEnum.VOID);
	
	@Override
	public Collection<? extends ErrorHolder> getErrors() {
		return null;
	}

	// we just want to gennerate the lambda classes once only
	// signatureCache

	/*
	 * 
	 * 
	 * ClassWriter cw = new ClassWriter(0); FieldVisitor fv; MethodVisitor mv;
	 * AnnotationVisitor av0;
	 * 
	 * cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER, "com/concurnas/compiler/bytecode/Lam",
	 * "Lcom/concurnas/bootstrap/lang/Lambda$Function1<Ljava/lang/Integer;Ljava/lang/String;>;"
	 * , "com/concurnas/bootstrap/lang/Lambda$Function1", null);
	 * 
	 * cw.visitSource("Lam.java", null);
	 * 
	 * cw.visitInnerClass("com/concurnas/bootstrap/lang/Lambda$Function1",
	 * "com/concurnas/bootstrap/lang/Lambda", "Function1", ACC_PUBLIC + ACC_STATIC +
	 * ACC_ABSTRACT);
	 * 
	 * { fv = cw.visitField(ACC_PRIVATE, "opon", "Ljava/util/ArrayList;",
	 * "Ljava/util/ArrayList<Ljava/lang/String;>;", null); fv.visitEnd(); } { mv
	 * = cw.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/util/ArrayList;)V",
	 * "(Ljava/util/ArrayList<Ljava/lang/String;>;)V", null); mv.visitCode();
	 * Label l0 = new Label(); mv.visitLabel(l0); mv.visitLineNumber(11, l0);
	 * mv.visitVarInsn(ALOAD, 0); mv.visitMethodInsn(INVOKESPECIAL,
	 * "com/concurnas/bootstrap/lang/Lambda$Function1", "<init>", "()V"); Label l1 = new
	 * Label(); mv.visitLabel(l1); mv.visitLineNumber(13, l1);
	 * mv.visitVarInsn(ALOAD, 0); mv.visitVarInsn(ALOAD, 1);
	 * mv.visitFieldInsn(PUTFIELD, "com/concurnas/compiler/bytecode/Lam", "opon",
	 * "Ljava/util/ArrayList;"); Label l2 = new Label(); mv.visitLabel(l2);
	 * mv.visitLineNumber(14, l2); mv.visitInsn(RETURN); Label l3 = new Label();
	 * mv.visitLabel(l3); mv.visitLocalVariable("this",
	 * "Lcom/concurnas/compiler/bytecode/Lam;", null, l0, l3, 0);
	 * mv.visitLocalVariable("opon", "Ljava/util/ArrayList;",
	 * "Ljava/util/ArrayList<Ljava/lang/String;>;", l0, l3, 1); mv.visitMaxs(2,
	 * 2); mv.visitEnd(); } { mv = cw.visitMethod(ACC_PUBLIC, "apply",
	 * "(Ljava/lang/Integer;)Ljava/lang/String;", null, null); mv.visitCode();
	 * Label l0 = new Label(); mv.visitLabel(l0); mv.visitLineNumber(18, l0);
	 * mv.visitVarInsn(ALOAD, 0); mv.visitFieldInsn(GETFIELD,
	 * "com/concurnas/compiler/bytecode/Lam", "opon", "Ljava/util/ArrayList;");
	 * mv.visitVarInsn(ALOAD, 1); mv.visitMethodInsn(INVOKEVIRTUAL,
	 * "java/lang/Integer", "intValue", "()I");
	 * mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "get",
	 * "(I)Ljava/lang/Object;"); mv.visitTypeInsn(CHECKCAST,
	 * "java/lang/String"); mv.visitInsn(ARETURN); Label l1 = new Label();
	 * mv.visitLabel(l1); mv.visitLocalVariable("this",
	 * "Lcom/concurnas/compiler/bytecode/Lam;", null, l0, l1, 0);
	 * mv.visitLocalVariable("i1", "Ljava/lang/Integer;", null, l0, l1, 1);
	 * mv.visitMaxs(2, 2); mv.visitEnd(); }
	 * 
	 * cw.visitEnd();
	 * 
	 * return cw.toByteArray();
	 */

	private final static Type Const_PRIM_VOID = new PrimativeType(PrimativeTypeEnum.VOID);//TODO: move to constants class
	private final static NamedType CONST_OBJ = new NamedType(new ClassDefJava(Object.class));
	private final static NamedType CONST_Obj_Void = new NamedType(new ClassDefJava(Void.class));

	// private final static ClassDefJava const_class_object = new
	// ClassDefJava(Object.class);

	
	private void gennerateLambdaClassForClassRef(FuncRef funcRef, String fullname, String fileName, FuncType taggedType){
		NamedType classOperatingOn = (NamedType)taggedType.retType;
		boolean operatingOnActor = classOperatingOn.getSetClassDef().isActor;
		String classConstrucrtedStr = classOperatingOn.getBytecodeType();
		String classConstrucrtedStrJustName  = classConstrucrtedStr.substring(1, classConstrucrtedStr.length()-1);
		
		// YUCK!
		this.bytecodeVisitor.localvarStackSize.push(0);
		this.bytecodeVisitor.localvarStack.push(new HashMap<String, Pair<Type, Integer>>());

		ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, bytecodeVisitor.typeDirectory);
		BytecodeOutputter boCasterHelpder = new BytecodeOutputter(cw, false);
		//FieldVisitor fv;

		String iface = classConstrucrtedStrJustName;
		if(operatingOnActor){
			NamedType typedActorOn = classOperatingOn.getSetClassDef().typedActorOn;
			if(null != typedActorOn){
				iface = typedActorOn.getBytecodeType();
				iface = iface.substring(1, iface.length()-1) + "$$ActorIterface";
			}
		}
		
		String[] ifaces = new String[]{ FuncType.classRefIfacePrefix + iface + FuncType.classRefIfacePostfix};
		
		cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER + ACC_SYNTHETIC, fullname, "Lcom/concurnas/bootstrap/lang/Lambda$ClassRef<"+classConstrucrtedStr+">;", "com/concurnas/bootstrap/lang/Lambda$ClassRef", ifaces);
		cw.visitSource(this.bytecodeVisitor.packageAndClassName + ".conc", null);
		cw.visitInnerClass("com/concurnas/bootstrap/lang/Lambda$Function1", "com/concurnas/bootstrap/lang/Lambda", "Function1", ACC_PUBLIC + ACC_STATIC + ACC_ABSTRACT);
		

		List<FuncType> constructors = classOperatingOn.getAllConstructorsExcludeHiddenArgs(null);
		//List<FuncType> allConstructors = classOperatingOn.getAllConstructors();
		
		List<FuncParam> splicedInArguments = new ArrayList<FuncParam>();
		if(classOperatingOn.getSetClassDef().isLocalClass){
			for(FuncType aCon : classOperatingOn.getAllConstructors(null)){
				if(!constructors.contains(aCon)){
					for(FuncParam fp : aCon.origonatingFuncDef.getParams().params){
						if(fp.name.contains("$n") ||  fp.hasSyntheticParamAnnotation()){
							splicedInArguments.add(fp);
						}
					}
					break;
				}
			}
		}
		
				
		NamedType nestedConsturctorParent = null;
		{
			ClassDef currentClass = classOperatingOn.getSetClassDef();
			ClassDef parentNestror = currentClass.getParentNestor();

			if (null != parentNestror) {// nested class first input arg is outer class ref
				nestedConsturctorParent = new NamedType(parentNestror);
			}
		}
		
		{// Fields
			if(nestedConsturctorParent != null){
				FieldVisitor fv = cw.visitField(ACC_PRIVATE, "$parentNestor", nestedConsturctorParent.getBytecodeType(), nestedConsturctorParent.getGenericBytecodeType(), null);
				fv.visitEnd();
			}
			
			if(!splicedInArguments.isEmpty()){
				for(FuncParam fp : splicedInArguments){
					FieldVisitor fv = cw.visitField(ACC_PRIVATE, fp.name, fp.type.getBytecodeType(), fp.type.getGenericBytecodeType(), null);
					fv.visitEnd();
				}
			}
			
		}
		
		/*{//fields
			fv = cw.visitField(ACC_PRIVATE, "leOrig", "L"+origonalClass+";", null, null);
			fv.visitEnd();
		}*/
		
		{// <init>
			
			ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> masterConstInputs = new ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>();
			if(null != nestedConsturctorParent && !funcRef.unbounded){
				masterConstInputs.add(new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(nestedConsturctorParent, null, null, false, false, false));
			}
			
			if(!splicedInArguments.isEmpty()){
				for(FuncParam fp : splicedInArguments){
					masterConstInputs.add(new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(fp.type, null, null, false, false, false));
				}
			}
			
			MethodVisitor mv = boCasterHelpder.enterMethod("<init>", masterConstInputs, Const_PRIM_VOID, false, false, false, AccessModifier.PUBLIC, 0, fullname, null, false, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/bootstrap/lang/Lambda$ClassRef", "<init>", "()V", false);
			
			int argoffset = 1;
			if(null != nestedConsturctorParent && !funcRef.unbounded){
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, argoffset++);
				mv.visitFieldInsn(PUTFIELD, fullname, "$parentNestor", nestedConsturctorParent.getBytecodeType());
			}
			
			if(!splicedInArguments.isEmpty()){
				for(FuncParam fp : splicedInArguments){
					mv.visitVarInsn(ALOAD, 0);
					Utils.applyLoad(boCasterHelpder, fp.type, argoffset);
					mv.visitFieldInsn(PUTFIELD, fullname, fp.name, fp.type.getBytecodeType());
					
					argoffset+=Utils.varSlotsConsumedByType(fp.type);
				}
			}
			
			//int inputArg =  (null == nestedConsturctorParent || funcRef.unbounded) ? 1 : 2;
			
			
			/*Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitFieldInsn(PUTFIELD, fullname, "leOrig", "L"+origonalClass+";");
			Label l2 = new Label();
			mv.visitLabel(l2);*/
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		
		
		{//applys + birdge method
			boolean isActor = TypeCheckUtils.isActor(TypeCheckUtils.dummyErrors, classOperatingOn);
			
			for(FuncType ft : constructors){
				if(ft.origonatingFuncDef.getAccessModifier() != AccessModifier.PUBLIC){
					continue;
				}
				
				ArrayList<Type> inputs = new ArrayList<Type>(ft.getInputs());
				
				ArrayList<Type> inputsforRealInit = new ArrayList<Type>(ft.getInputs());
				
				if(isActor){
					inputs.remove(0);
				}
				
				if(null != nestedConsturctorParent){
					inputsforRealInit.add(0, nestedConsturctorParent);
				}
				
				if(!splicedInArguments.isEmpty()){
					for(FuncParam fp : splicedInArguments){
						inputsforRealInit.add(fp.type);
					}
				}
				
				
				String inputsx = BytecodeGennerator.getNormalMethodInvokationDesc(inputs, classOperatingOn);
				String calledInit = BytecodeGennerator.getNormalMethodInvokationDesc(inputsforRealInit, Const_PRIM_VOID);
				{
					FrameSateTrackingMethodVisitor mv = new FrameSateTrackingMethodVisitor(cw.visitMethod(ACC_PUBLIC, "apply", inputsx, null, null), ACC_PUBLIC, inputsx, fullname);
					boCasterHelpder.mv = mv;
					mv.visitCode();

					Label l0 = new Label();
					mv.visitLabel(l0);
					if(null != nestedConsturctorParent){
						mv.visitVarInsn(ALOAD, 0);
						mv.visitFieldInsn(GETFIELD, fullname, "$parentNestor", nestedConsturctorParent.getBytecodeType());
						mv.visitTypeInsn(NEW, classConstrucrtedStrJustName);
						mv.visitInsn(SWAP);
						mv.visitInsn(DUP2);
						mv.visitInsn(POP);
						mv.visitInsn(SWAP);// lol
					}else{
						mv.visitTypeInsn(NEW, classConstrucrtedStrJustName);
						mv.visitInsn(DUP);
					}
					
					if(isActor){
						Utils.addClassTypeArrayForActorRef(boCasterHelpder, classOperatingOn);
					}
					
					int cnt = 1;
					for(Type input : inputs){
						cnt += Utils.applyLoad(boCasterHelpder, input, cnt);
					}
					
					if(!splicedInArguments.isEmpty()){
						for(FuncParam fp : splicedInArguments){
							inputsforRealInit.add(fp.type);
							mv.visitVarInsn(ALOAD, 0);
							mv.visitFieldInsn(GETFIELD, fullname, fp.name, fp.type.getBytecodeType());
						}
					}
					
					mv.visitMethodInsn(INVOKESPECIAL, classConstrucrtedStrJustName, "<init>", calledInit, false);
					mv.visitInsn(ARETURN);
					mv.visitMaxs(3, 2);
					mv.visitEnd();
					
					{//apply bridge method
						String bridgeMethodName = BytecodeGennerator.getNormalMethodInvokationDesc(inputs, ScopeAndTypeChecker.const_object);
						
						if (!bridgeMethodName.equals(inputsx)) {
							FrameSateTrackingMethodVisitor mvBridge = new FrameSateTrackingMethodVisitor(cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "apply", bridgeMethodName, null, null), ACC_PUBLIC, bridgeMethodName, fullname);
							boCasterHelpder.mv = mvBridge;
							mvBridge.visitCode();
							mvBridge.visitVarInsn(ALOAD, 0);
							
							cnt=1;
							for(Type input : inputs){
								cnt += Utils.applyLoad(boCasterHelpder, input, cnt);
							}
							
							mvBridge.visitMethodInsn(INVOKEVIRTUAL, fullname, "apply", inputsx);
							mvBridge.visitInsn(ARETURN);// must be object return if ret aything!
							mvBridge.visitMaxs(2, 2);
							mvBridge.visitEnd();
						}
					}
					
				}
			}
		}
		
		
		{//bind
			
			
			if(null != nestedConsturctorParent &&  funcRef.unbounded){//unbounded and linked to parent. i.e.: xx =  Parent<int>.MyClass<String>&()
				MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "bind", "(Ljava/lang/Object;)V", null, null);
				mv.visitCode();
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				
				String nsPn = nestedConsturctorParent.getBytecodeType();
				String justType = nsPn.substring(1, nsPn.length()-1);
				mv.visitTypeInsn(CHECKCAST, justType);
				mv.visitFieldInsn(PUTFIELD, fullname, "$parentNestor", nsPn);
				mv.visitInsn(RETURN);
				mv.visitMaxs(2, 2);
				mv.visitEnd();
			}
			
		}
		
		{//signature - manditory
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "signature", "()[Ljava/lang/Object;", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			//add fields if any in here...
			
			/*mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, fullname, "leOrig", "L"+origonalClass+";");
			mv.visitMethodInsn(INVOKEVIRTUAL, origonalClass, "signature", "()[Ljava/lang/Object;", false);*/
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		
		addEqualsAndHashCode(cw, fullname);
		
		
		cw.visitEnd();

		this.bytecodeVisitor.localvarStackSize.pop();
		this.bytecodeVisitor.localvarStack.pop();

		this.bytecodeVisitor.addNameBtyecode(fullname, cw.toByteArray());
	}
	
	private void addEqualsAndHashCode(ClassWriter cw, String fullname){
		{
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
			mv.visitCode();

			Label l1 = new Label();
			mv.visitVarInsn(ALOAD, 1);
			mv.visitJumpInsn(IFNULL, l1);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitTypeInsn(INSTANCEOF, "com/concurnas/bootstrap/lang/Lambda");
			mv.visitJumpInsn(IFEQ, l1);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitTypeInsn(CHECKCAST, "com/concurnas/bootstrap/lang/Lambda");
			mv.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/bootstrap/lang/Lambda", "signature", "()[Ljava/lang/Object;", false);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, fullname, "signature", "()[Ljava/lang/Object;", false);
			mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/lang/Equalifier", "equals", "([Ljava/lang/Object;[Ljava/lang/Object;)Z", false);
			mv.visitInsn(IRETURN);

			mv.visitLabel(l1);
			mv.visitInsn(ICONST_0);
			mv.visitInsn(IRETURN);

			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}

		{
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, fullname, "signature", "()[Ljava/lang/Object;");
			mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/lang/Hasher", "hashCode", "([Ljava/lang/Object;)I");
			mv.visitInsn(IRETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
	}
	
	/*private Type ifGenericUseUpperBound(Type tt) {
		if(tt instanceof GenericType) {
			Type upper = ((GenericType)tt).getOrigonalGenericTypeUpperBound();
			if(null != upper) {
				tt = upper;
			}
		}
		
		return tt;
	}*/
	
	private void gennerateLambdaClass(FuncRef funcRef, String fullname, String  fileName, FuncRefArgs args, TypeAndLocation tal, FuncType taggedType, DefaultMapConvertionChecker specialApplier, boolean isConstructor) {

		// YUCK!
		this.bytecodeVisitor.localvarStackSize.push(0);
		this.bytecodeVisitor.localvarStack.push(new HashMap<String, Pair<Type, Integer>>());
		// eww remember to pop!

		// get clever with: args - bound vs unbound args

		boolean isreallyStatic = tal.getLocation() instanceof StaticFuncLocation;
		
		//boolean isOperatingOnStatic = isreallyStatic || funcRef.unbounded;
		// boolean isOperatingOnStatic = !(funcRef.typeOperatedOn.getLocation()
		// instanceof ClassFunctionLocation) &&
		// !funcRef.shouldVisitFunctoInBytcodeGen;

		ArrayList<Type> argumentsThatDontNeedToBecurriedIn = funcRef.argumentsThatDontNeedToBecurriedIn;
		String emthodName = funcRef.methodName;

		ConcClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, bytecodeVisitor.typeDirectory);
		BytecodeOutputter boCasterHelpder = new BytecodeOutputter(cw, false);
		FieldVisitor fv;
		// MethodVisitor mv;

		FuncType origonalFuncBeingInvoked = (FuncType) TypeCheckUtils.getRefType(tal.getType());
		ArrayList<Type> origInputArgs = origonalFuncBeingInvoked.getInputs();
		Type origRetType = origonalFuncBeingInvoked.retType;

		boolean returnVoid = funcRef.superClassName == null && origRetType.equals(ScopeAndTypeChecker.const_void);//ignore the return void functionality for refs
		Type taggedReturnType = taggedType.retType;
		
		String signature;
		if (funcRef.superClassName != null) {
			signature = "L" + funcRef.superClassName + ";";// IsoTask
		} else {
			if(returnVoid && taggedType.getInputs().isEmpty()) {
				signature = "Lcom/concurnas/bootstrap/lang/Lambda$Function0v;";
			}else {
				StringBuilder sb = new StringBuilder("Lcom/concurnas/bootstrap/lang/Lambda$Function" + args.unboundCount() + (returnVoid?"v":"")  + "<");

				for (Type t : taggedType.getInputs()) {
					sb.append( ((AbstractType) (TypeCheckUtils.boxTypeIfPrimative(t, false))).getGenericBytecodeTypeNoGens());//.getGenericBytecodeTypeWithoutArray());
				}
				if(!returnVoid) {
					sb.append( ((AbstractType)  (  TypeCheckUtils.boxTypeIfPrimative(taggedReturnType, false))).getGenericBytecodeTypeNoGens());
				}
				
				sb.append(">;");
				signature = sb.toString();
			}
		}
		
		// Type typeOperatingOn =
		// ((FuncLocation)tal.getLocation()).getOwnerType();
		String classOfThingGettingCalledgenericType;
		String classOfThingGettingCalled;

		boolean isClassRef = origonalFuncBeingInvoked.isClassRefType;
		boolean isIsolate=signature.equals("Lcom/concurnas/bootstrap/runtime/cps/IsoTask;");
		
		boolean returnsSomething = taggedReturnType != null && !((taggedReturnType instanceof PrimativeType) && ((PrimativeType) taggedReturnType).type == PrimativeTypeEnum.VOID);

		if (!returnsSomething && !returnVoid) {
			taggedReturnType = isIsolate?CONST_Obj_Void:CONST_OBJ;
		}

		Location loccc = tal.getLocation();
		
		Type typeOperatingOn;
		if (loccc instanceof FuncLocation) {
			typeOperatingOn = ((FuncLocation) loccc).getOwnerType();
			
			if(loccc.isRHSOfTraitSuperChainable != null) {//e.g. x = super.aMethod&(a) in a trait
				typeOperatingOn = loccc.isRHSOfTraitSuperChainable.getA();
			}
			
			
			classOfThingGettingCalledgenericType = typeOperatingOn.getGenericBytecodeType();// .getNonGenericPrettyName();
			classOfThingGettingCalled = typeOperatingOn.getBytecodeType();
		} else {// applied to a lambda -> so call apply
				// classOfThingGettingCalledgenericType = "L" +
				// loccc.getLambdaOwner() + ";";
				// classOfThingGettingCalled = "L" + loccc.getLambdaOwner() +
				// ";";
			typeOperatingOn = tal.getType();

			classOfThingGettingCalledgenericType = typeOperatingOn.getGenericBytecodeType();
			classOfThingGettingCalled = typeOperatingOn.getBytecodeType();

			emthodName = "apply";

			// apply is always on the boxed variant of a primative type
			for (Object o : args.exprOrTypeArgsList) {
				if (o instanceof Expression) {
					((Expression) o).setTaggedType(TypeCheckUtils.boxTypeIfPrimativeAndSetUpperBound(((Expression) o).getTaggedType()));
				}
			}
			ArrayList<Type> neworigInputArgs = new ArrayList<Type>(origInputArgs.size());
			for (Type t : origInputArgs) {
				neworigInputArgs.add(isClassRef?t:TypeCheckUtils.boxTypeIfPrimativeAndSetUpperBound(t));
			}
			origInputArgs = neworigInputArgs;

			origRetType = TypeCheckUtils.boxTypeIfPrimativeAndSetUpperBound(origRetType);
		}
		
		String[] ifaces = null;
		
		if(taggedType.implementSAM != null) {
			NamedType nt = taggedType.implementSAM.getA().copyTypeSpecific();
			nt.setArrayLevels(0);
			String samiface = nt.getBytecodeType();
			samiface = samiface.substring(1,  samiface.length()-1);
			ifaces = new String[] {samiface};
		}

		String lambdaClassBeingOverriden = "com/concurnas/bootstrap/lang/Lambda$Function" + args.unboundCount() + (returnVoid?"v":"")  ;//void?
		
		cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER + ACC_SYNTHETIC, fullname, signature, funcRef.superClassName == null ? lambdaClassBeingOverriden : funcRef.superClassName, ifaces);
		cw.visitSource(this.bytecodeVisitor.packageAndClassName + ".conc", null);
		cw.visitInnerClass("com/concurnas/bootstrap/lang/Lambda$Function" + args.unboundCount() + (returnVoid?"v":""), "com/concurnas/bootstrap/lang/Lambda", "Function" + args.unboundCount() + (returnVoid?"v":""), ACC_PUBLIC + ACC_STATIC + ACC_ABSTRACT);


		NamedType nestedConsturctorParent = null;
		if(isConstructor){
			if(typeOperatingOn instanceof NamedType){
				ClassDef currentClass = ((NamedType)typeOperatingOn).getSetClassDef();
				
				ClassDef parentNestror = currentClass.getParentNestor();
				boolean isNestedClass = null != parentNestror;

				if (isNestedClass) {// nested class first input arg is outer class ref
					nestedConsturctorParent = new NamedType(parentNestror);
					//inputs.add(0, new Tuple<Type, String>(parNestor, null));
				}
			}
		}
		
		// inputs and outputs are always in Object type (not int, float etc)

		Thruple<String, String, String> refReturned = null;
		
		{// Fields
			if (!isreallyStatic  ) {//if static not needed ever, but if its unbounded then we need this 
				fv = cw.visitField(ACC_PRIVATE, "opon", classOfThingGettingCalled, classOfThingGettingCalledgenericType, null);
				fv.visitEnd();
			}
			
			if(nestedConsturctorParent != null){
				fv = cw.visitField(ACC_PRIVATE, "$parentNestor", nestedConsturctorParent.getBytecodeType(), nestedConsturctorParent.getGenericBytecodeType(), null);
				fv.visitEnd();
			}
			
			{
				int fieldcnt = 0;
				for (Object o : args.exprOrTypeArgsList) {
					if (o instanceof Expression) {// -> type, unbound, therefor
													// input argument
						Type fieldType = Utils.copyRemoveGenericUpperBound(((Expression) o).getTaggedType());
						String fname = "f" + fieldcnt;
						fv = cw.visitField(ACC_PRIVATE, fname, fieldType.getBytecodeType(), fieldType.getGenericBytecodeType(), null);
						fv.visitEnd();

						if(isIsolate && o instanceof RefName) {
							if(((RefName)o).name.equals("ret$")) {
								refReturned = new Thruple<String, String, String>(fname, fieldType.getBytecodeType(), fieldType.getGenericBytecodeType());
							}
						}
						
						fieldcnt++;
					}
				}
			}

			if (signature.equals("Lcom/concurnas/bootstrap/runtime/cps/IsoTask;") || signature.equals("Lcom/concurnas/bootstrap/runtime/cps/AbstractIsoTask;") ) {
				fv = cw.visitField(ACC_PRIVATE, "$isInitCompleteFlag", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef;", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef<Ljava/lang/Boolean;>;", null);
				fv.visitEnd();
			}

		}

		
		//bytecodeSandboxMyActor$$Lambda72
		
		
		//we gennerate a init method for the bounded and unbounded cases - really we ought to just gennerate for the cases (bounded or unbounded, that are acutlaly used, but meh...)
		
		boolean[] toGennerates = !isreallyStatic?new boolean[]{false, true}:new boolean[]{ true};
		
		for(boolean isOperatingOnStatic : toGennerates)
		{// "<init>"
			ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> masterConstInputs = new ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>();
			
			if(null != nestedConsturctorParent && !funcRef.unbounded){
				masterConstInputs.add(new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(nestedConsturctorParent, null, null, false, false, false));
			}
			
			if (!isOperatingOnStatic) {
				masterConstInputs.add(new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(typeOperatingOn, null, null, false, false, false));
			}

			for (Type i : argumentsThatDontNeedToBecurriedIn) {
				masterConstInputs.add(new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(i, null, null, false, false, false));
			}
			// mv = cw.visitMethod(ACC_PUBLIC, "<init>",
			// "(Ljava/util/ArrayList;)V",
			// "(Ljava/util/ArrayList<Ljava/lang/String;>;)V", null);
			MethodVisitor mv = boCasterHelpder.enterMethod("<init>", masterConstInputs, Const_PRIM_VOID, false, false, false, AccessModifier.PUBLIC, 0, fullname, null, false, null);

			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			
			if(isreallyStatic && !funcRef.isConstructor){
				mv.visitInsn(ACONST_NULL);
			}
			else{
				mv.visitLdcInsn(org.objectweb.asm.Type.getType(classOfThingGettingCalled));
			}
			
			mv.visitMethodInsn(INVOKESPECIAL, funcRef.superClassName != null ? funcRef.superClassName : lambdaClassBeingOverriden, "<init>", "(Ljava/lang/Class;)V", false);
			Label l1 = new Label();
			mv.visitLabel(l1);

			if (!isOperatingOnStatic) {
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitFieldInsn(PUTFIELD, fullname, "opon", classOfThingGettingCalled);
			}
			else if(null != nestedConsturctorParent && !funcRef.unbounded){
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitFieldInsn(PUTFIELD, fullname, "$parentNestor", nestedConsturctorParent.getBytecodeType());
			}
			// TODO: put other fields here, ensure they are cast tothe correct
			// expected input type before sotring...

			int fieldcnt = 0;
			// int taggedTypeInputCnt = 0;
			int inputArg = (isOperatingOnStatic && (null == nestedConsturctorParent || funcRef.unbounded)) ? 1 : 2;
			int posInStuffToNotCurryIn = 0;

			for (Object o : args.exprOrTypeArgsList) {
				if (o instanceof Expression) {// -> type, unbound, therefor
												// input argument
					Type got = argumentsThatDontNeedToBecurriedIn.get(posInStuffToNotCurryIn);
					// Type expected =
					// TypeCheckUtils.boxTypeIfPrimative(taggedInputs.get(taggedTypeInputCnt));
					Type fieldType = Utils.copyRemoveGenericUpperBound(((Expression) o).getTaggedType());
					mv.visitVarInsn(ALOAD, 0);// this
					Utils.applyLoad(boCasterHelpder, got, inputArg);
					if (!got.equals(fieldType)) {
						Utils.applyCastImplicit(boCasterHelpder, got, fieldType, bytecodeVisitor);
					}
					mv.visitFieldInsn(PUTFIELD, fullname, "f" + fieldcnt, fieldType.getBytecodeType());

					fieldcnt++;
					inputArg += Utils.varSlotsConsumedByType(fieldType);
					posInStuffToNotCurryIn++;
					// taggedTypeInputCnt++;
				}
			}

			if (signature.equals("Lcom/concurnas/bootstrap/runtime/cps/IsoTask;")) {
				mv.visitVarInsn(ALOAD, 0);
				mv.visitTypeInsn(NEW, "com/concurnas/runtime/ref/Local");
				mv.visitInsn(DUP);
				mv.visitInsn(ICONST_1);
				mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
				mv.visitInsn(DUP);
				mv.visitInsn(ICONST_0);
				mv.visitLdcInsn(org.objectweb.asm.Type.getType("Ljava/lang/Boolean;"));
				mv.visitInsn(AASTORE);
				mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/runtime/ref/Local", "<init>", "([Ljava/lang/Class;)V", false);
				mv.visitFieldInsn(PUTFIELD, fullname, "$isInitCompleteFlag", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef;");
			}

			
			
			
			if(taggedType.implementSAM != null) {//call trait init for sam class gennerated on the fly
				NamedType asNamed = (NamedType)taggedType.implementSAM.getA();
				Pair<List<NamedType>, ArrayList<Fourple<String, String, Boolean, TypeAndLocation>>> res = ScopeAndTypeChecker.createTraitSuperRefImpls(null, 0,0, new ArrayList<NamedType>(), asNamed);
				
				List<NamedType> linears = res.getA();
				if(!linears.isEmpty()) {
					BytecodeGennerator.visitLinearlizedTraitInits(boCasterHelpder, linears);
				}
				
			}
			
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		ArrayList<Type> applyInputs = args.getUnBoundArgs();
		// Type retTypeNoGenericals = taggedType.retType;
		String applyMethodSig = BytecodeGennerator.getNormalMethodInvokationDesc(applyInputs, taggedReturnType, true, false);
		// String applyMethodSig = "(Ljava/lang/Integer;)Ljava/lang/String;";
		String methodBeingCalledOnOrigonalObject;

		ArrayList<Fourple<String, String, String, Type>> fieldsForSig = new ArrayList<Fourple<String, String, String, Type>>();
		String ownerClassBeingCalledThingy = "";
		{// "apply"
			FrameSateTrackingMethodVisitor mv = new FrameSateTrackingMethodVisitor(cw.visitMethod(ACC_PUBLIC, "apply", applyMethodSig, null, null), ACC_PUBLIC, applyMethodSig, fullname);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);

			boCasterHelpder.mv = mv;

			Type finOpOn = typeOperatingOn;
			String finOpOnStr = finOpOn.getBytecodeType();
			finOpOnStr = finOpOnStr.substring(1, finOpOnStr.length() - 1);

			if(isConstructor){
				/*
			     	NEW bytecodeSandbox$$Lambda0
				    DUP
				    BIPUSH 12
				    INVOKESPECIAL bytecodeSandbox$$Lambda0.<init> (I)V
				 */
				
				if(null != nestedConsturctorParent){
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, fullname, "$parentNestor", nestedConsturctorParent.getBytecodeType());
					mv.visitTypeInsn(NEW, finOpOnStr);
					mv.visitInsn(SWAP);
					mv.visitInsn(DUP2);
					mv.visitInsn(POP);
					mv.visitInsn(SWAP);// lol
				}
				else{
					mv.visitTypeInsn(NEW, finOpOnStr);
					mv.visitInsn(DUP);
				}
				
			}
			else if (!isreallyStatic) {
				mv.visitVarInsn(ALOAD, 0);// this
				mv.visitFieldInsn(GETFIELD, fullname, "opon", classOfThingGettingCalled);
				mv.visitInsn(DUP);
				
				Label skipToOnNotNull = new Label();
				mv.visitJumpInsn(IFNONNULL, skipToOnNotNull);
				mv.visitTypeInsn(NEW, "com/concurnas/lang/LambdaException");
				mv.visitInsn(DUP);
				mv.visitLdcInsn("Method reference has not been bound to instance object, call bind() before invocation");
				mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/lang/LambdaException", "<init>", "(Ljava/lang/String;)V", false);
				mv.visitInsn(ATHROW);
				mv.visitLabel(skipToOnNotNull);//business as usual
				
				finOpOn = Utils.unref(boCasterHelpder, typeOperatingOn, bytecodeVisitor);
			}
			
			String opOnIsClassRef = null;
			if(finOpOn instanceof FuncType && ((FuncType)finOpOn).isClassRefType){
				opOnIsClassRef = ((FuncType)finOpOn).retType.getBytecodeType();
				opOnIsClassRef = FuncType.classRefIfacePrefix + opOnIsClassRef.substring(1, opOnIsClassRef.length()-1) + FuncType.classRefIfacePostfix;
				mv.visitTypeInsn(CHECKCAST, opOnIsClassRef);
			}

			finOpOnStr = finOpOn.getBytecodeType();
			finOpOnStr = finOpOnStr.substring(1, finOpOnStr.length() - 1);

			
			int argcnt = 1;
			int fieldcnt = 0;
			int posInExpectedInputs = 0;

			int defaultParamObjSlot = args.exprOrTypeArgsList.size() + 1;
			
			Pair<Integer, HashMap<Integer, Type>> defpp = BytecodeGennerator.defaultParamPrelude(boCasterHelpder, null, origonalFuncBeingInvoked.defaultFuncArgs, args.exprOrTypeArgsList, defaultParamObjSlot, false);//last arg false?
			HashMap<Integer, Type> toAddDefaultParamUncreate = defpp.getB();
			
			int n=0;
			
			CurriedVararg curriedVararg = args.curriedVararg;
			int startOfRangeforVarargCurry = -1;
			int endOfRangeforVarargCurry = -1;
			Type varargCurryArrayType = null;
			Type varargCurryArrayTypeNMinus1 = null;
			int vaargcurrearryM=0;
			if(null !=curriedVararg ){
				startOfRangeforVarargCurry = curriedVararg.startOfRangeforVarargCurry;
				endOfRangeforVarargCurry = curriedVararg.endOfRangeforVarargCurry;
				varargCurryArrayType = curriedVararg.varargCurryArrayType;
				varargCurryArrayTypeNMinus1 = (Type)varargCurryArrayType.copy();
				varargCurryArrayTypeNMinus1.setArrayLevels(varargCurryArrayTypeNMinus1.getArrayLevels()-1);
			}
			
			
			for (Object o : args.exprOrTypeArgsList) {
				boolean isDefault = toAddDefaultParamUncreate.containsKey(n);
				
				if(o == null){
					BytecodeGennerator.processNullArgExpression(boCasterHelpder, toAddDefaultParamUncreate, n);
				}
				else{
					Type expected = null;
					if (posInExpectedInputs < origInputArgs.size()) {
						if(null !=curriedVararg ){
							expected = ((Node)o).getTaggedType();
						}else{
							expected = origInputArgs.get(posInExpectedInputs);
						}
					}
					int arLevels = -1;
					if(n == startOfRangeforVarargCurry){//create array for curried varags
						int size = endOfRangeforVarargCurry - startOfRangeforVarargCurry;
						arLevels = varargCurryArrayType.getArrayLevels();
						
						Utils.intOpcode(boCasterHelpder, size);
						Utils.createArray(boCasterHelpder, arLevels, varargCurryArrayType, true);
					}
					
					if(endOfRangeforVarargCurry > n && n >= startOfRangeforVarargCurry){
						mv.visitInsn(DUP);
						Utils.intOpcode(boCasterHelpder, vaargcurrearryM++);
					}
					
					if (o instanceof Type && !(o instanceof VarNull)) {// -> type, unbound, therefor input argument
						Type got = TypeCheckUtils.boxTypeIfPrimative((Type) o, false);// input will be boxed (how else could u do it)
						mv.visitVarInsn(ALOAD, argcnt);
						if (!got.equals(expected == null ? got : expected)) {
							Utils.applyCastImplicit(boCasterHelpder, got, expected, bytecodeVisitor);
						}
						argcnt++;
					} else{// expression, gets passed in constructor ->field
						// Type fieldArg =
						// TypeCheckUtils.boxTypeIfPrimative(taggedInputs.get(posInStuffNotToCurryIn));
						Type fieldArg = Utils.copyRemoveGenericUpperBound((((Expression) o).getTaggedType()));
						// Type fieldArg =
						// Utils.copyRemoveGenericUpperBound(TypeCheckUtils.getRefTypeToLocked(((Expression)o).getTaggedType()));

						mv.visitVarInsn(ALOAD, 0);// this
						String fname = "f" + fieldcnt;
						String fbcType = fieldArg.getBytecodeType();
						fieldsForSig.add(new Fourple<String, String, String, Type>(fullname, fname, fbcType, fieldArg));
						mv.visitFieldInsn(GETFIELD, fullname, fname, fbcType);

						if (!fieldArg.equals(expected == null ? fieldArg : expected)) {
							Utils.applyCastImplicit(boCasterHelpder, fieldArg, expected, bytecodeVisitor);
						}

						if(fbcType.endsWith("$ClassRefIface;")) {
							mv.visitTypeInsn(CHECKCAST, "com/concurnas/bootstrap/lang/Lambda$ClassRef");
						}
						
						fieldcnt++;
						// posInStuffNotToCurryIn++;
					}

					//expected
					if(n >= startOfRangeforVarargCurry && n < endOfRangeforVarargCurry){
						if(expected == null){//wtf?
							expected = ((Node)o).getTaggedType();
						}
						if (!expected.equals(varargCurryArrayTypeNMinus1)) {
							Utils.applyCastImplicit(boCasterHelpder, expected, varargCurryArrayTypeNMinus1, bytecodeVisitor);
						}

						Utils.applyArrayStore(boCasterHelpder, varargCurryArrayTypeNMinus1, bytecodeVisitor, true);
					}
					
					if(isDefault){
						mv.visitVarInsn(ALOAD, defaultParamObjSlot);
					}
					
					posInExpectedInputs++;
				}
				n++;
			}

			ArrayList<Type> methToCallARgs = new ArrayList<Type>();
			
			if(null != nestedConsturctorParent){
				methToCallARgs.add(nestedConsturctorParent);
			}
			
			for (Type t : origonalFuncBeingInvoked.defaultFuncArgs!=null?origonalFuncBeingInvoked.defaultFuncArgs:origInputArgs) {
				methToCallARgs.add(t);
			}
			if (null != funcRef.extraArgsForLambdaConst) {
				for (Type t : funcRef.extraArgsForLambdaConst.getBoundArgsTypes()) {
					methToCallARgs.add(t);
				}
			}

			methodBeingCalledOnOrigonalObject = BytecodeGennerator.getNormalMethodInvokationDesc(methToCallARgs, isConstructor?const_void:origRetType, false, false);
			// "(I)Ljava/lang/Object;"
			ownerClassBeingCalledThingy = classOfThingGettingCalled.substring(1, classOfThingGettingCalled.length() - 1);

			
			if(isConstructor){
				/*
			     	NEW bytecodeSandbox$$Lambda0
				    DUP
				    BIPUSH 12
				    INVOKESPECIAL bytecodeSandbox$$Lambda0.<init> (I)V
				 */
				mv.visitMethodInsn(INVOKESPECIAL, finOpOnStr, "<init>", methodBeingCalledOnOrigonalObject, false); // generic
			}
			else{
				boolean isActorCall = finOpOnStr.endsWith("$$ActorIterface");
				//finOpOn
				boolean isIFace = (finOpOn instanceof NamedType && ((NamedType)finOpOn).isInterface()) || opOnIsClassRef != null;
				
				int invokeType = (isActorCall || isIFace)? INVOKEINTERFACE: (isreallyStatic ? INVOKESTATIC : INVOKEVIRTUAL);
				String opOns = opOnIsClassRef != null?opOnIsClassRef:finOpOnStr;
				
				if(loccc.isRHSOfTraitSuperChainable != null) {
					invokeType = INVOKEINTERFACE;
					emthodName = loccc.isRHSOfTraitSuperChainable.getB();
					opOns = loccc.isRHSOfTraitSuperChainable.getA().getSetClassDef().bcFullName();//"FIGURE ME OUT";//this.currentClassDefObj.peek().bcFullName();
				}
				else if(invokeType == INVOKEINTERFACE && loccc.isRHSOfTraitSuper) {
					
					invokeType = INVOKESTATIC;
					emthodName += "$traitM";
					methodBeingCalledOnOrigonalObject = "(L" + opOns +";" + methodBeingCalledOnOrigonalObject.substring(1);
				}
				
				if(!isActorCall && finOpOn instanceof NamedType && ((NamedType)finOpOn).getSetClassDef().isActor ) {
					if(!emthodName.endsWith("$ActorCall") &&!emthodName.endsWith("$ActorSuperCall") && !emthodName.endsWith("$ActorSuperCallObjM")) {
						emthodName += "$ActorSuperCall";
					}
					
				}
				
				mv.visitMethodInsn(invokeType, opOns, emthodName, methodBeingCalledOnOrigonalObject, (isActorCall || isIFace)); // generic
				// so returns this...
			}
			
			if (origRetType.getOrigonalGenericTypeUpperBound() != null && !origRetType.equals(origRetType.getOrigonalGenericTypeUpperBound())) {// mv.visitTypeInsn(CHECKCAST,
																																				// "java/lang/String");
				
				Utils.doCheckCastConvertArrays(boCasterHelpder, origRetType, origRetType.getCheckCastType());
				
			}
			// elif void
			else if (!TypeCheckUtils.isVoid(origRetType)) {
				Type boxed = TypeCheckUtils.boxTypeIfPrimative(origRetType, false);
				if (!origRetType.equals(boxed)) {
					Utils.applyCastImplicit(boCasterHelpder, origRetType, boxed, bytecodeVisitor);
				}
			}

			if(returnVoid) {
				mv.visitInsn(RETURN);// must be object return if ret aything!
			}else {
				if (!returnsSomething) {
					mv.visitInsn(ACONST_NULL);
				}

				// mv.visitInsn(returnsSomething?ARETURN:RETURN);//must be object
				// return if ret aything!
				mv.visitInsn(ARETURN);// must be object return if ret aything!
			}

			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}

		// ok, we need a bridge from apply object to apply as Integer
		String bridgeMethodsignature;
		{// bridge - "apply"

			StringBuilder bridgeApplyStr = new StringBuilder("(");
			for (int n = 0; n < applyInputs.size(); n++) {
				bridgeApplyStr.append("Ljava/lang/Object;");
			}
			if(returnVoid) {
				bridgeApplyStr.append(")V");
			}else if(isIsolate){
				bridgeApplyStr.append(")Ljava/lang/Void;");
			}else {
				bridgeApplyStr.append(")Ljava/lang/Object;");
			}
			
			
			bridgeMethodsignature = bridgeApplyStr.toString();
			if (!bridgeMethodsignature.equals(applyMethodSig)) {// only output if it
															// does something...
				// MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE +
				// ACC_SYNTHETIC, "apply",
				// "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
				MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "apply", bridgeMethodsignature, null, null);
				mv.visitCode();
				mv.visitVarInsn(ALOAD, 0);

				for (int n = 0; n < applyInputs.size(); n++) {// perform
																// necisary
																// casts to
																// boxed types
																// of the above
																// Object types
					mv.visitVarInsn(ALOAD, n + 1);
					Type toType = TypeCheckUtils.boxTypeIfPrimative(applyInputs.get(n), false);
					// String pretName =
					// toType.getNonGenericPrettyName().replace('.', '/');
					String pretName = toType.getBytecodeType();
					pretName = pretName.substring(1, pretName.length() - 1);

					if (!pretName.equals("java/lang/Object")) {
						mv.visitTypeInsn(CHECKCAST, toType.getCheckCastType());
					}
				}

				mv.visitMethodInsn(INVOKEVIRTUAL, fullname, "apply", applyMethodSig);
				if(returnVoid) {
					mv.visitInsn(RETURN);
				}else {
					mv.visitInsn(ARETURN);// must be object return if ret aything!
				}
				
				mv.visitMaxs(2, 2);
				mv.visitEnd();
			}
		}
		
		if(isIsolate){//getResultRef
			if(refReturned == null) {
				MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getResultRef", "()Lcom/concurnas/bootstrap/runtime/ref/Ref;", null, null);
				mv.visitCode();
				mv.visitInsn(ACONST_NULL);
				mv.visitInsn(ARETURN);
			}else {
				String rtype = refReturned.getB();
				String genrtype = refReturned.getC();
				MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getResultRef", "()"+rtype, "()" + genrtype, null);
				mv.visitCode();
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, fullname, refReturned.getA(), rtype);
				mv.visitInsn(ARETURN);
				
				if(!rtype.equals("Lcom/concurnas/bootstrap/runtime/ref/Ref;")) {//bridge method
					mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "getResultRef", "()Lcom/concurnas/bootstrap/runtime/ref/Ref;", null, null);
					mv.visitCode();
					mv.visitVarInsn(ALOAD, 0);
					mv.visitMethodInsn(INVOKEVIRTUAL, fullname, "getResultRef", "()"+rtype, false);
					mv.visitInsn(ARETURN);
				}
				
			}
		}
		
		
		if(taggedType.implementSAM != null) {
			//gennerate a 'bridge' from the sam iface to the apply method
			Pair<NamedType, TypeAndLocation> samdets = taggedType.implementSAM;
			FuncType samFT = (FuncType)samdets.getB().getType();
			
			StringBuilder bridgeApplyStr = new StringBuilder("(");
			ArrayList<Type> inputs = samFT.getInputs();
			for(Type input : inputs) {
				if(null != input.getOrigonalGenericTypeUpperBound()) {
					input = input.getOrigonalGenericTypeUpperBound();
				}
				bridgeApplyStr.append(input.getBytecodeType());
			}
			
			Type retType = samFT.retType;
			if(retType.getOrigonalGenericTypeUpperBound() != null) {
				retType = retType.getOrigonalGenericTypeUpperBound();
			}
			
			bridgeApplyStr.append(")" + retType.getBytecodeType());
			String samMethodStr = bridgeApplyStr.toString();
			
			boolean gennerate = true;
			if(samFT.origonatingFuncDef.funcName.equals("apply")) {//avoid name clash
				gennerate = !samMethodStr.equals(applyMethodSig) && !samMethodStr.equals(bridgeMethodsignature);
			}
			
			if (gennerate) {
				FrameSateTrackingMethodVisitor mv = new FrameSateTrackingMethodVisitor(cw.visitMethod(ACC_PUBLIC , samFT.origonatingFuncDef.funcName, samMethodStr, null, null), ACC_PUBLIC, samMethodStr, samFT.origonatingFuncDef.funcName);
				mv.visitCode();
				boCasterHelpder.mv = mv;
				
				mv.visitVarInsn(ALOAD, 0);

				for (int n = 0; n < applyInputs.size(); n++) {
					Type methType = inputs.get(n);
					Type callInput = TypeCheckUtils.boxTypeIfPrimative(applyInputs.get(n), false);

					/*Utils.applyLoad(boCasterHelpder, inputType, n + 1);
					Type boxedInput = TypeCheckUtils.boxTypeIfPrimative(inputType, false);
					
					if (!inputType.equals(boxedInput)) {
						Utils.applyCastImplicit(boCasterHelpder, inputType, boxedInput, bytecodeVisitor);
					}*/
					Utils.applyLoad(boCasterHelpder, methType, n + 1);
					if (!methType.equals(callInput)) {
						Utils.applyCastImplicit(boCasterHelpder, methType, callInput, bytecodeVisitor);
					}
					
				}

				mv.visitMethodInsn(INVOKEVIRTUAL, fullname, "apply", applyMethodSig);
				
				if(TypeCheckUtils.isVoid(retType)) {
					if(!taggedReturnType.equals(ScopeAndTypeChecker.const_void)) {
						mv.visitInsn(POP);
					}
				}else {
					Type applyRet = TypeCheckUtils.boxTypeIfPrimative(taggedReturnType, false);
					if (!applyRet.equals(retType)) {
						Utils.applyCastImplicit(boCasterHelpder, applyRet, retType, bytecodeVisitor);
					}
				}
		
				mv.visitInsn(Utils.returnTypeToOpcode(retType));
				
				mv.visitMaxs(2, 2);
				mv.visitEnd();
			}
			
			{//extract traitFields to add stubs for
				List<Fiveple<String, ClassDef, Type, Boolean, AccessModifier>> traitFields = new ArrayList<Fiveple<String, ClassDef, Type, Boolean, AccessModifier>>();
				traitFields.addAll(samdets.getA().getSetClassDef().getTraitFields(new HashMap<Type, Type>()));
				
				if(!traitFields.isEmpty()) {
					List<Thruple<String, Type, Type>> traitVarsNeedingImpl = new ArrayList<Thruple<String, Type, Type>>();
					
					HashMap<String, HashMap<Type, HashSet<Pair<ClassDef, Boolean>>>> traitVarToClassDef = ScopeAndTypeChecker.calctraitVarToClassDef(traitFields);
					
					if(!traitVarToClassDef.isEmpty()) {
						for(String itm : traitVarToClassDef.keySet()) {
							HashMap<Type, HashSet<Pair<ClassDef, Boolean>>> hases = traitVarToClassDef.get(itm);
							for(Type tt : hases.keySet()) {
								HashSet<Pair<ClassDef, Boolean>> items = hases.get(tt);
								
								for(Pair<ClassDef, Boolean> item : items) {
									Thruple<String, Type, Type> toAdd = new Thruple<String, Type, Type>(itm, tt, tt) ;
									traitVarsNeedingImpl.add(toAdd);
								}
							}
						}
					}
					
					if(!traitVarsNeedingImpl.isEmpty()) {

						for(Thruple<String, Type, Type> inst : traitVarsNeedingImpl) {
							Type it = inst.getB();
							fv = cw.visitField(ACC_PRIVATE, inst.getA(), it.getBytecodeType(), it.getGenericBytecodeType(), null);
							fv.visitEnd();
						}
						
						BytecodeOutputter prevbco = bytecodeVisitor.bcoutputter;
						bytecodeVisitor.bcoutputter = boCasterHelpder;
						bytecodeVisitor.addTraitVarsNeedingStubImpl(fullname, traitVarsNeedingImpl);
						bytecodeVisitor.bcoutputter = prevbco;
					}
				}
			}
		}
		

		// two seperate modules may gennerated by a seperate compilation unit -
		// thus we have to perform equality based on the array returned from
		// this function
		{
			/*
			 * public Object[] signature(){ Object[] ret = new Object[4]; ret[0]
			 * = opon; //can be null if on interface ret[1] =
			 * bytecodeSandBox.MyClass
			 * .myFunction(II)I.innerfunction()//packageangclass ret[2] =
			 * "pairMeth(ILjava/lang/Object;I)I"; ret[3] = new Object[]{f0, f1};
			 * ret[4] = "Integer";//typesToBindIn return ret; }
			 */

			FrameSateTrackingMethodVisitor mv = new FrameSateTrackingMethodVisitor(cw.visitMethod(ACC_PUBLIC, "signature", "()[Ljava/lang/Object;", null, null), ACC_PUBLIC, "()[Ljava/lang/Object;", fullname);
			boCasterHelpder.mv = mv;

			mv.visitCode();
			mv.visitInsn(ICONST_5);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
			mv.visitVarInsn(ASTORE, 1);

			mv.visitVarInsn(ALOAD, 1);
			mv.visitInsn(ICONST_0);
			if (isreallyStatic) {
				mv.visitInsn(ACONST_NULL);
			} else {
				mv.visitTypeInsn(NEW, "java/lang/Integer");
				mv.visitInsn(DUP);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, fullname, "opon", classOfThingGettingCalled);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I");
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
				// System.identityHashCode() unique hashcode
			}
			mv.visitInsn(AASTORE);

			mv.visitVarInsn(ALOAD, 1);
			mv.visitInsn(ICONST_1);
			mv.visitLdcInsn(ownerClassBeingCalledThingy);
			mv.visitInsn(AASTORE);

			mv.visitVarInsn(ALOAD, 1);
			mv.visitInsn(ICONST_2);
			mv.visitLdcInsn(emthodName + methodBeingCalledOnOrigonalObject);
			mv.visitInsn(AASTORE);

			mv.visitVarInsn(ALOAD, 1);
			mv.visitInsn(ICONST_3);

			int fieldArSize = fieldsForSig.size();

			Utils.intOpcode(boCasterHelpder, fieldArSize);// n
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
			int n = 0;
			for (Fourple<String, String, String, Type> fieldola : fieldsForSig) {
				mv.visitInsn(DUP);
				Utils.intOpcode(boCasterHelpder, n++);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, fieldola.getA(), fieldola.getB(), fieldola.getC());
				// convert to object if prim
				Utils.box(boCasterHelpder, fieldola.getD());
				mv.visitInsn(AASTORE);
			}
			mv.visitInsn(AASTORE);

			mv.visitVarInsn(ALOAD, 1);
			mv.visitInsn(ICONST_4);
			mv.visitLdcInsn(applyMethodSig);
			mv.visitInsn(AASTORE);

			mv.visitVarInsn(ALOAD, 1);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(6, 2);
			mv.visitEnd();
		}

		/*
		 * @Override public boolean equals(Object o){ if(o != null && o
		 * instanceof Lambda){ return Equalifier.equals(((Lambda)o).signature(),
		 * this.signature()); } return false; }
		 */

		addEqualsAndHashCode(cw, fullname);

		if(isreallyStatic){
			if(null != nestedConsturctorParent &&  funcRef.unbounded){//unbounded and linked to parent. i.e.: xx =  Parent<int>.MyClass<String>&()
				MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "bind", "(Ljava/lang/Object;)V", null, null);
				mv.visitCode();
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				
				String nsPn = nestedConsturctorParent.getBytecodeType();
				String justType = nsPn.substring(1, nsPn.length()-1);
				mv.visitTypeInsn(CHECKCAST, justType);
				mv.visitFieldInsn(PUTFIELD, fullname, "$parentNestor", nsPn);
				mv.visitInsn(RETURN);
				mv.visitMaxs(2, 2);
				mv.visitEnd();
			}else{//this is not supported
				MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "bind", "(Ljava/lang/Object;)V", null, null);
				mv.visitCode();
				mv.visitTypeInsn(NEW, "com/concurnas/lang/LambdaException");
				mv.visitInsn(DUP);
				mv.visitLdcInsn("Cannot bind top level function references");
				mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/lang/LambdaException", "<init>", "(Ljava/lang/String;)V");
				mv.visitInsn(ATHROW);
				mv.visitMaxs(3, 2);
				mv.visitEnd();
			}
		}
		/*else if(!funcRef.unbounded){
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "bind", "(Ljava/lang/Object;)V", null, null);
			mv.visitCode();
			mv.visitTypeInsn(NEW, "com/concurnas/lang/LambdaException");
			mv.visitInsn(DUP);
			mv.visitLdcInsn("Cannot bind");
			mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/lang/LambdaException", "<init>", "(Ljava/lang/String;)V");
			mv.visitInsn(ATHROW);
			mv.visitMaxs(3, 2);
			mv.visitEnd();
		}*/
		else{//binding method funcRef.unbounded
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "bind", "(Ljava/lang/Object;)V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			String justType = classOfThingGettingCalled.substring(1, classOfThingGettingCalled.length()-1);
			mv.visitTypeInsn(CHECKCAST, justType);
			mv.visitFieldInsn(PUTFIELD, fullname, "opon", classOfThingGettingCalled);
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		
		if (signature.equals("Lcom/concurnas/bootstrap/runtime/cps/IsoTask;")) {
			{// getIsInitCompleteFlag
				MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getIsInitCompleteFlag", "()Lcom/concurnas/bootstrap/runtime/ref/DefaultRef;", "()Lcom/concurnas/bootstrap/runtime/ref/DefaultRef<Ljava/lang/Boolean;>;", null);
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, fullname, "$isInitCompleteFlag", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef;");
				mv.visitInsn(ARETURN);
				mv.visitMaxs(1, 1);
				mv.visitEnd();
			}

		}

		cw.visitEnd();

		this.bytecodeVisitor.localvarStackSize.pop();
		this.bytecodeVisitor.localvarStack.pop();

		this.bytecodeVisitor.addNameBtyecode(fullname, cw.toByteArray(fullname));
	}

	private HashSet<Pair<String, String>> genLambdaAlready = new HashSet<Pair<String, String>>();

	@Override
	public Object visit(FuncRef funcRef) {
		visit(funcRef, null);
		return null;
	}

	private void visit(FuncRef funcRef, Pair<NamedType, TypeAndLocation> implementSAM) {
		/*
		 * nonStaticFuncRef = ar.get&(int)
		 * 
		 * =>
		 * 
		 * class fn$lambda$1(opon ArrayList[String]) extends
		 * Lambda.Function1[Int, String] { fun apply(i1 Int) String { return
		 * this.opon.get(i1) } }
		 */

		if (funcRef.functo != null) {
			funcRef.functo.accept(this);// acc in case functo is a lambda itself
		}
		
		if(funcRef.argsForNextCompCycle != null) {
			funcRef.argsForNextCompCycle.accept(this);
		}

		Pair<String, String> lambdaDetails = funcRef.getLambdaDetails();

		if (!genLambdaAlready.contains(lambdaDetails)) {
			FuncType typeopon = (FuncType) funcRef.getTaggedType();
			if(typeopon.isClassRefType){
				gennerateLambdaClassForClassRef(funcRef, lambdaDetails.getA(), lambdaDetails.getB(), typeopon);//fullname, filename
			} else{
				FuncType theFuncType = (FuncType)funcRef.getTaggedType();
				theFuncType.implementSAM = implementSAM;
				gennerateLambdaClass(funcRef, lambdaDetails.getA(), lambdaDetails.getB(), funcRef.getArgsAndLambdaConstsAsFuncRefArgs(), funcRef.typeOperatedOn, theFuncType, null, funcRef.isConstructor);
			}
			
			genLambdaAlready.add(lambdaDetails);
		}
	}
	
	private void gennerateLambdaClassForDefaultMethod(String fileName, String fullname, String origonalClass, Type expectedInput, Type actualInput, Type expectedOutput, Type actualOutput, boolean dropInputForCall ) {
		//we just invoke the apply method of the origonal thing and cast as needs be
		
		
		// YUCK!
		this.bytecodeVisitor.localvarStackSize.push(0);
		this.bytecodeVisitor.localvarStack.push(new HashMap<String, Pair<Type, Integer>>());

		ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, bytecodeVisitor.typeDirectory);
		BytecodeOutputter boCasterHelpder = new BytecodeOutputter(cw, false);
		FieldVisitor fv;

		String expInput = expectedInput.getGenericBytecodeType();
		String expOutput = expectedOutput.getGenericBytecodeType();
		
		cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER + ACC_SYNTHETIC, fullname, "Lcom/concurnas/bootstrap/lang/Lambda$Function1<"+expInput+expOutput+">;", "com/concurnas/bootstrap/lang/Lambda$Function1", null);
		cw.visitSource(this.bytecodeVisitor.packageAndClassName + ".conc", null);
		cw.visitInnerClass("com/concurnas/bootstrap/lang/Lambda$Function1", "com/concurnas/bootstrap/lang/Lambda", "Function1", ACC_PUBLIC + ACC_STATIC + ACC_ABSTRACT);
		
		
		{//fields
			fv = cw.visitField(ACC_PRIVATE, "leOrig", "L"+origonalClass+";", null, null);
			fv.visitEnd();
		}
		{// <init>
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(L"+origonalClass+";)V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitInsn(ACONST_NULL);
			mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/bootstrap/lang/Lambda$Function1", "<init>", "(Ljava/lang/Class;)V", false);
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitFieldInsn(PUTFIELD, fullname, "leOrig", "L"+origonalClass+";");
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		{
			String applyMethodSig = "("+expectedInput.getBytecodeType()+")"+expectedOutput.getBytecodeType();
			FrameSateTrackingMethodVisitor mv = new FrameSateTrackingMethodVisitor(cw.visitMethod(ACC_PUBLIC, "apply", applyMethodSig, null, null), ACC_PUBLIC, applyMethodSig, fullname);
			
			boCasterHelpder.mv = mv;
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, fullname, "leOrig", "L"+origonalClass+";");
			//cast?
			if(dropInputForCall){
				mv.visitMethodInsn(INVOKEVIRTUAL, origonalClass, "apply", "()"+actualOutput.getBytecodeType(), false);
			}
			else{
				mv.visitVarInsn(ALOAD, 1);
				if(!expectedInput.equals(actualInput)){
					Utils.applyCastExplicit(boCasterHelpder, expectedInput, actualInput, bytecodeVisitor);
				}
				mv.visitMethodInsn(INVOKEVIRTUAL, origonalClass, "apply", "("+actualInput.getBytecodeType()+")"+actualOutput.getBytecodeType(), false);
			}
			
			if(!expectedOutput.equals(actualOutput)){
				Utils.applyCastExplicit(boCasterHelpder, actualOutput, expectedOutput, bytecodeVisitor);
			}
			
			mv.visitInsn(ARETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		{
			//check cast if not correct type then return null
			String simpleExpcetedInputType = expectedInput.getBytecodeType();
			simpleExpcetedInputType = simpleExpcetedInputType.substring(1, simpleExpcetedInputType.length()-1);
			
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitTypeInsn(INSTANCEOF, simpleExpcetedInputType);
			Label l1 = new Label();
			mv.visitJumpInsn(IFEQ, l1);
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitTypeInsn(CHECKCAST, simpleExpcetedInputType);
			mv.visitMethodInsn(INVOKEVIRTUAL, fullname, "apply", "("+expectedInput.getBytecodeType()+")" + expectedOutput.getBytecodeType(), false);
			mv.visitInsn(ARETURN);
			mv.visitLabel(l1);
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		
		{
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "signature", "()[Ljava/lang/Object;", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, fullname, "leOrig", "L"+origonalClass+";");
			mv.visitMethodInsn(INVOKEVIRTUAL, origonalClass, "signature", "()[Ljava/lang/Object;", false);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		
		cw.visitEnd();

		this.bytecodeVisitor.localvarStackSize.pop();
		this.bytecodeVisitor.localvarStack.pop();

		this.bytecodeVisitor.addNameBtyecode(fileName, cw.toByteArray());
	}

	@Override
	public Object visit(MapDef mapDef) {
		for (IsAMapElement e : mapDef.elements) {
			if (e instanceof MapDefElement) {
				((MapDefElement) e).accept(this);
			}
		}

		if (null != mapDef.defaultMapElement) {
			mapDef.defaultMapElement.accept(this);

			DefaultMapConvertionChecker checker = new DefaultMapConvertionChecker(mapDef.defaultMapElement, mapDef.defaultMapElement.keyType, mapDef.defaultMapElement.valType);
			// and now we gennerate a lambda class to point to the lambda class
			// already created above - but with correct wrapping etc

			Expression vNode = mapDef.defaultMapElement.astRedirect != null ? mapDef.defaultMapElement.astRedirect : mapDef.defaultMapElement.value;
			
			if (!checker.evalsToNonFuncRefOrLambda) {
				Pair<String, String> lambdaDetails;
				FuncRef theOrigFuncRef;
				if (vNode instanceof FuncRef) {
					theOrigFuncRef = ((FuncRef) vNode);
					lambdaDetails = theOrigFuncRef.getLambdaDetails();
				} else {
					theOrigFuncRef = ((LambdaDef) vNode).fakeFuncRef;
					lambdaDetails = ((LambdaDef) vNode).lamDets;
				}

				Pair<String, String> lambdaDetailsForMDE = new Pair<String, String>(lambdaDetails.getA() + "MDE", lambdaDetails.getB() + "MDE");

				if (!genLambdaAlready.contains(lambdaDetailsForMDE)) {
					String mdeClass = lambdaDetailsForMDE.getA();
					String origClass = lambdaDetails.getA();
					
					Type expectedInput = mapDef.defaultMapElement.keyType;
					Type actualInput = TypeCheckUtils.boxTypeIfPrimative(checker.origInputType, false);
					if(actualInput ==null){
						actualInput = expectedInput;
					}
					
					Type expectedOutput = mapDef.defaultMapElement.valType;
					Type actualOutput = TypeCheckUtils.boxTypeIfPrimative(checker.origOutputType, false);
					if(actualOutput ==null){
						actualOutput = expectedOutput;
					}
					
					gennerateLambdaClassForDefaultMethod(lambdaDetailsForMDE.getA(), lambdaDetailsForMDE.getA(), origClass,  expectedInput,  actualInput,  expectedOutput,  actualOutput, checker.dropInputForCall);
					genLambdaAlready.add(lambdaDetailsForMDE);
					
					mapDef.defaultMapElement.mdeClass=mdeClass;
					mapDef.defaultMapElement.origClass=origClass;
				}
			}
		}

		return null;
	}
	
	@Override
	public Object visit(LambdaDef lambdaDef) {
		if(!lambdaDef.hasErrors) {
			visit(lambdaDef.fakeFuncRef, lambdaDef.implementSAM);
		}
		return super.visit(lambdaDef);
	}
	@Override
	public Object visit(FuncDef fd) {
		if(fd.hasErrors) {
			return null;
		}
		return super.visit(fd);
	}


	@Override
	public Object visit(AsyncBlock asyncBlock) {

		/*
		 * res = { a + 4; }!
		 * 
		 * =>
		 * 
		 * fun LambdaInner0() int:{ ret = { a + 4; } ret }
		 * 
		 * 
		 * class fn$lambda$1(ret int:) extends Lambda.Function0[int:] { fun
		 * apply() String { return lambdafunc() } }
		 */

		visit(asyncBlock.fakeFuncRef);
		return super.visit(asyncBlock);
	}

	/*
	 * dont bother with this... bridge method we dont support (should we?)
	 * 
	 * { mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "apply",
	 * "(Ljava/lang/Object;)Ljava/lang/Object;", null, null); mv.visitCode();
	 * Label l0 = new Label(); mv.visitLabel(l0); mv.visitLineNumber(1, l0);
	 * mv.visitVarInsn(ALOAD, 0); mv.visitVarInsn(ALOAD, 1);
	 * mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
	 * mv.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/compiler/bytecode/Lam",
	 * "apply", "(Ljava/lang/Integer;)Ljava/lang/String;");
	 * mv.visitInsn(ARETURN); mv.visitMaxs(2, 2); mv.visitEnd(); }
	 */
}
