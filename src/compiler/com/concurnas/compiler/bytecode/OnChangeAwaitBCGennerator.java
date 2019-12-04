package com.concurnas.compiler.bytecode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.ast.AccessModifier;
import com.concurnas.compiler.ast.Annotations;
import com.concurnas.compiler.ast.AsyncBodyBlock;
import com.concurnas.compiler.ast.Await;
import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.ast.FuncParam;
import com.concurnas.compiler.ast.FuncParams;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.OnChange;
import com.concurnas.compiler.ast.OnEvery;
import com.concurnas.compiler.ast.PrimativeType;
import com.concurnas.compiler.ast.PrimativeTypeEnum;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.utils.Fiveple;
import com.concurnas.compiler.utils.Sixple;
import com.concurnas.compiler.visitors.AbstractVisitor;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.runtime.Pair;
import com.concurnas.runtime.cps.IsoRegistrationSet;
import com.concurnas.runtime.cps.IsoRegistrationSetAsync;
import com.concurnas.runtime.cps.IsoRegistrationSetAsyncChild;
import com.concurnas.runtime.cps.RefStateTracker;

/*
 * For each onchange function, gennerates worker class with methods to:
 * constructor
 * init
 * apply(notificaiton)
 */
public class OnChangeAwaitBCGennerator extends AbstractVisitor implements Opcodes{

	private BytecodeGennerator bytecodeVisitor;
	
	public OnChangeAwaitBCGennerator(BytecodeGennerator bytecodeVisitor)
	{
		this.bytecodeVisitor = bytecodeVisitor; 
	}
	@Override
	public Collection<? extends ErrorHolder> getErrors() {
		return null;
	}
	
	private final static Type Const_PRIM_VOID = new PrimativeType(PrimativeTypeEnum.VOID);
	private final static Type Const_PRIM_LONG = new PrimativeType(PrimativeTypeEnum.LONG);
	public final static Type Const_RegSetType = new NamedType(new ClassDefJava(IsoRegistrationSet.class));
	public final static Type Const_RegSetTypeParent = new NamedType(new ClassDefJava(IsoRegistrationSetAsync.class));
	public final static Type Const_RefStateTracker = new NamedType(new ClassDefJava(RefStateTracker.class));
	public final static Type Const_RegSetTypeAsyncChild = new NamedType(new ClassDefJava(IsoRegistrationSetAsyncChild.class));
	
	private void createStateObjectClass(String fileName, String fullname, FuncParams spliceInArgs, HashSet<String> stuffToNull, boolean isEvery, boolean isOnlyClosed){
		//YUCK!
		this.bytecodeVisitor.localvarStackSize.push(0);
		this.bytecodeVisitor.localvarStack.push(new HashMap<String, Pair<Type, Integer>>());

		ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, bytecodeVisitor.typeDirectory);
		cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER + ACC_SYNTHETIC, fullname, null, "java/lang/Object", null);
		
		{//Fields
			for(FuncParam o : spliceInArgs.params){
				Type fieldType = Utils.copyRemoveGenericUpperBound(o.getTaggedType());
				FieldVisitor fv = cw.visitField(ACC_PUBLIC, o.name, fieldType.getBytecodeType(), fieldType instanceof PrimativeType?null:fieldType.getGenericBytecodeType(), null);
				fv.visitEnd();
			}
			
			cw.visitField(ACC_PUBLIC, "$regSet", Const_RegSetType.getBytecodeType(), null, null).visitEnd();
			cw.visitField(ACC_PUBLIC, "refStateTracker$", Const_RefStateTracker.getBytecodeType(), null, null).visitEnd();
		}
		
		BytecodeOutputter boCasterHelpder = new BytecodeOutputter(cw, false);
		
		{//"<init>"
			ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> splicedInAsTypeAndStr = spliceInArgs.getAsTypesAndNames();
			ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> forMeth = Utils.filterOutNameFromtuples(splicedInAsTypeAndStr, stuffToNull);
			MethodVisitor mv = boCasterHelpder.enterMethod("<init>", forMeth, Const_PRIM_VOID, false, false, false, AccessModifier.PUBLIC, 0, fullname, null, false, null);

			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			//mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			
			int cnt = 1;
			for(Sixple<Type, String, Annotations, Boolean, Boolean, Boolean> tAndName : splicedInAsTypeAndStr){
				mv.visitVarInsn(ALOAD, 0);//this
				Type tt =  tAndName.getA();
				if(stuffToNull.contains(tAndName.getB())){//just null ref it
					mv.visitInsn(Opcodes.ACONST_NULL);
				}
				else{
					Utils.applyLoad(boCasterHelpder, tt, cnt++);
					if (tt instanceof PrimativeType) {
						PrimativeTypeEnum tta = ((PrimativeType) tt).type;
						if (tta == PrimativeTypeEnum.DOUBLE || tta == PrimativeTypeEnum.LONG) {// double, long
							cnt++;
						}
					}
				}
				mv.visitFieldInsn(PUTFIELD, fullname, tAndName.getB(), tt.getBytecodeType());
			}
			
			
			{
				mv.visitVarInsn(ALOAD, 0);
				mv.visitTypeInsn(NEW, "com/concurnas/runtime/cps/IsoRegistrationSet");
				mv.visitInsn(DUP);
				
				mv.visitInsn(isEvery?ICONST_1:ICONST_0);
				mv.visitInsn(isOnlyClosed?ICONST_1:ICONST_0);
				
				mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/runtime/cps/IsoRegistrationSet", "<init>", "(ZZ)V", false);
				mv.visitFieldInsn(PUTFIELD, fullname, "$regSet", "Lcom/concurnas/runtime/cps/IsoRegistrationSet;");
			}
			
			{
				mv.visitVarInsn(ALOAD, 0);
				mv.visitTypeInsn(NEW, "com/concurnas/runtime/cps/RefStateTracker");
				mv.visitInsn(DUP);
				mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/runtime/cps/RefStateTracker", "<init>", "()V", false);
				mv.visitFieldInsn(PUTFIELD, fullname, "refStateTracker$", "Lcom/concurnas/runtime/cps/RefStateTracker;");
			}
			
			
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		
		cw.visitEnd();
		
		this.bytecodeVisitor.localvarStackSize.pop();
		this.bytecodeVisitor.localvarStack.pop();

		this.bytecodeVisitor.addNameBtyecode(fileName, cw.toByteArray());
		
	}
	
	
	
	private void createStateObjectClassForAsyncBodyBlock(String fileName, String fullname, FuncParams spliceInArgs, List< FuncParam> extrafieldsDeclInOnChanges, HashSet<String> stuffToNull, int childrenCount, HashMap<String, Type> preBlockVars){
		//YUCK!
		//extrafieldsDeclInOnChanges - e.g. async{ every(f=xs){ } } //f here is considered an extrafieldsDeclInOnChanges
		this.bytecodeVisitor.localvarStackSize.push(0);
		this.bytecodeVisitor.localvarStack.push(new HashMap<String, Pair<Type, Integer>>());

		//boolean isNestedInsideAsync = asycnIdx>-1;
		
		ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, bytecodeVisitor.typeDirectory);
		
		cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER + ACC_SYNTHETIC, fileName, null, "java/lang/Object", null);
		
		//HashSet<String> splicedArgs = new HashSet<String>(); //splicedArgs.add(o.name);
		
		//stuffToNull.add(null)
		
		{//Fields
			for(FuncParam o : spliceInArgs.params){
				//if(o.type!=null){//if nul then decared inside preblock
					Type fieldType = Utils.copyRemoveGenericUpperBound(o.getTaggedType());
					FieldVisitor fv = cw.visitField(ACC_PUBLIC, o.name, fieldType.getBytecodeType(), fieldType instanceof PrimativeType?null:fieldType.getGenericBytecodeType(), null);
					fv.visitEnd();
				//}
			}
			
			for(FuncParam o : extrafieldsDeclInOnChanges){
				Type fieldType = Utils.copyRemoveGenericUpperBound(o.getTaggedType());
				FieldVisitor fv = cw.visitField(ACC_PUBLIC, o.name, fieldType.getBytecodeType(), fieldType instanceof PrimativeType?null:fieldType.getGenericBytecodeType(), null);
				fv.visitEnd();
			}
			
			if(null != preBlockVars){
				for(String vname : preBlockVars.keySet()){
					Type vType = preBlockVars.get(vname);
					FieldVisitor fv = cw.visitField(ACC_PUBLIC, vname, vType.getBytecodeType(), vType instanceof PrimativeType?null:vType.getGenericBytecodeType(), null);
					fv.visitEnd();
				}
			}
			
			//cw.visitField(ACC_PUBLIC, "$refCount", Const_PRIM_LONG.getBytecodeType(), null, null).visitEnd();//JPT: is Long enough?
			cw.visitField(ACC_PUBLIC, "$regSetParent", Const_RegSetTypeParent.getBytecodeType(), null, null).visitEnd();
			for(int n =0; n < childrenCount; n++){
				cw.visitField(ACC_PUBLIC, "$regSet" + n, Const_RegSetTypeAsyncChild.getBytecodeType(), null, null).visitEnd();
				cw.visitField(ACC_PUBLIC, "refStateTracker$" + n, Const_RefStateTracker.getBytecodeType(), null, null).visitEnd();
			}
		}
		
		BytecodeOutputter boCasterHelpder = new BytecodeOutputter(cw, false);
		
		{//"<init>"
			ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> splicedInAsTypeAndStr = spliceInArgs.getAsTypesAndNames();
			ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> forMeth = Utils.filterOutNameFromtuples(splicedInAsTypeAndStr, stuffToNull);
			//forMeth = Utils.filterOutNameFromtuples(forMeth, preBlockVars.keySet());//we dont want preblock vars to be passed in
			
			MethodVisitor mv = boCasterHelpder.enterMethod("<init>", forMeth, Const_PRIM_VOID, false, false, false, AccessModifier.PUBLIC, 0, fullname, null, false, null);

			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			
			//int cnt = isNestedInsideAsync?2:1;//arg1 is the parent
			int cnt = 1;
			for(Sixple<Type, String, Annotations, Boolean, Boolean, Boolean> tAndName : splicedInAsTypeAndStr){
				mv.visitVarInsn(ALOAD, 0);//this
				Type tt =  tAndName.getA();
				if(stuffToNull.contains(tAndName.getB())){//just null ref it
					mv.visitInsn(Opcodes.ACONST_NULL);
				}
				else{
					Utils.applyLoad(boCasterHelpder, tt, cnt++);
					if (tt instanceof PrimativeType) {
						PrimativeTypeEnum tta = ((PrimativeType) tt).type;
						if (tta == PrimativeTypeEnum.DOUBLE || tta == PrimativeTypeEnum.LONG) {// double, long
							cnt++;
						}
					}
				}
				mv.visitFieldInsn(PUTFIELD, fileName, tAndName.getB(), tt.getBytecodeType());
			}
			
			
			{
				mv.visitVarInsn(ALOAD, 0);
				mv.visitTypeInsn(NEW, "com/concurnas/runtime/cps/IsoRegistrationSetAsync");
				mv.visitInsn(DUP);
				mv.visitInsn(DUP);
				
				int tempVarForIsoSetAsync = forMeth.size()+cnt;
				
				mv.visitVarInsn(ASTORE, tempVarForIsoSetAsync);
				
				mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/runtime/cps/IsoRegistrationSetAsync", "<init>", "()V", false);
				mv.visitFieldInsn(PUTFIELD, fileName, "$regSetParent", "Lcom/concurnas/runtime/cps/IsoRegistrationSetAsync;");
				
				for(int n =0; n < childrenCount; n++){//kids...
					//public <init>
					mv.visitVarInsn(ALOAD, 0);
					mv.visitTypeInsn(NEW, "com/concurnas/runtime/cps/IsoRegistrationSetAsyncChild");
					mv.visitInsn(DUP);
					mv.visitVarInsn(ALOAD, tempVarForIsoSetAsync);
					Utils.intOpcode(boCasterHelpder, n);
					mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/runtime/cps/IsoRegistrationSetAsyncChild", "<init>", "(Lcom/concurnas/runtime/cps/IsoRegistrationSetAsync;I)V", false);
					mv.visitFieldInsn(PUTFIELD, fileName, "$regSet" + n, "Lcom/concurnas/runtime/cps/IsoRegistrationSetAsyncChild;");
					
					mv.visitVarInsn(ALOAD, 0);
					mv.visitTypeInsn(NEW, "com/concurnas/runtime/cps/RefStateTracker");
					mv.visitInsn(DUP);
					mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/runtime/cps/RefStateTracker", "<init>", "()V", false);
					mv.visitFieldInsn(PUTFIELD, fullname, "refStateTracker$" + n, "Lcom/concurnas/runtime/cps/RefStateTracker;");
				}
			}
			
			{
				//set initial values for ref fields
				//preBlockVars
				for(String name : preBlockVars.keySet()){
					Type typola = preBlockVars.get(name);
					if(TypeCheckUtils.hasRefLevels(typola)){
						mv.visitVarInsn(ALOAD, 0);
						Utils.createRef(boCasterHelpder, typola, 0,  false,  false, -1);
						mv.visitFieldInsn(PUTFIELD, fileName, name, typola.getBytecodeType());
					}
				}
			}
			
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		
		cw.visitEnd();
		
		this.bytecodeVisitor.localvarStackSize.pop();
		this.bytecodeVisitor.localvarStack.pop();

		this.bytecodeVisitor.addNameBtyecode(fileName, cw.toByteArray());
	}
	
	
	
	private void gennerateOnChangeClass(OnChange onChange, String isoTaskName, int asyncNum){
		String fileName = onChange.onChangeDets.getA();
		String fullname= fileName;//onChange.onChangeDets.getB();
		NamedType holderclass = onChange.holderclass;
		String fileNameSO = onChange.getFullnameSO();
		String initMethodName = onChange.initMethodName;
		String cleanupMethodName = onChange.cleanupMethodName;
		String applyMethodName = onChange.applyMethodName;
		
		boolean isInAsync = asyncNum > -1;
		
		//YUCK!
		this.bytecodeVisitor.localvarStackSize.push(0);
		this.bytecodeVisitor.localvarStack.push(new HashMap<String, Pair<Type, Integer>>());
		//eww remember to pop!
		ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, bytecodeVisitor.typeDirectory);
		
		String superclassName ="com/concurnas/bootstrap/runtime/cps/" +  isoTaskName ;
		
		cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER + ACC_SYNTHETIC, fileName, null, superclassName, null);
		cw.visitSource(this.bytecodeVisitor.packageAndClassName + ".conc", null);

		String fileNameSOClassName = "L" + fileNameSO+";";

		String holderclassnameNoLs = holderclass.getBytecodeType();
		holderclassnameNoLs = holderclassnameNoLs.substring(1, holderclassnameNoLs.length() -1);
		String holderclassname = holderclass.getBytecodeType();
		
		{//Fields
			FieldVisitor fv = cw.visitField(ACC_PRIVATE, "stateObj", fileNameSOClassName, null, null);//Lcom/concurnas/runtime/ref/Local;
			fv.visitEnd();
			
			if(!onChange.isModuleLevel){
				fv = cw.visitField(ACC_PRIVATE, "opon", holderclassname, null, null);
				fv.visitEnd();
			}
			
			fv = cw.visitField(ACC_PRIVATE, "$isInitCompleteFlag", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef;", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef<Ljava/lang/Boolean;>;", null);
			fv.visitEnd();
		}
		
		
		{//"<init>"
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(" + fileNameSOClassName + (onChange.isModuleLevel?"":holderclassname) + ")V", "(" + fileNameSOClassName + (onChange.isModuleLevel?"":holderclass.getGenericBytecodeType())  + ")V", null);

			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, superclassName, "<init>", "()V", false);
			
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitFieldInsn(PUTFIELD, fullname, "stateObj", fileNameSOClassName);
			
			if(!onChange.isModuleLevel){
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 2);
				mv.visitFieldInsn(PUTFIELD, fullname, "opon", holderclassname);
			}
			

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
			
			if(isInAsync){//register self with IsoRegistrationSetAsyncChild iso state
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, fileNameSO, "$regSet" + asyncNum, "Lcom/concurnas/runtime/cps/IsoRegistrationSetAsyncChild;");
				mv.visitVarInsn(ALOAD, 1);
				mv.visitFieldInsn(PUTFIELD, "com/concurnas/runtime/cps/IsoRegistrationSetAsyncChild", "isoTask", "Lcom/concurnas/bootstrap/runtime/cps/IsoTrigger;");
			}
			
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2); 
			mv.visitEnd(); 
		}
		
		{//init
			String methodSig = "(Ljava/util/LinkedHashSet;)V";
			String methodSigGen = "(Ljava/util/LinkedHashSet<Lcom/concurnas/bootstrap/runtime/ref/DefaultRef<*>;>;)V";
			String callingClassArg = "("+fileNameSOClassName+"Ljava/util/LinkedHashSet;)V";
			
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "init", methodSig, methodSigGen, null);
			
			if(!onChange.isModuleLevel){
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, fullname, "opon", holderclassname);
			}

			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, fullname, "stateObj", fileNameSOClassName);
				mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(onChange.isModuleLevel?INVOKESTATIC:INVOKEVIRTUAL, holderclassnameNoLs, initMethodName, callingClassArg, false);
			
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		
		{//cleanup
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "cleanup", "()V", null, null);
			
			if(!onChange.isModuleLevel){
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, fullname, "opon", holderclassname);
			}

			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, fullname, "stateObj", fileNameSOClassName);
			mv.visitMethodInsn(onChange.isModuleLevel?INVOKESTATIC:INVOKEVIRTUAL, holderclassnameNoLs, cleanupMethodName, "("+fileNameSOClassName+")V", false);
			
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		

		{//apply
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "apply", "(Lcom/concurnas/bootstrap/runtime/transactions/Transaction;Z)Z", null, null);
			
			if(!onChange.isModuleLevel){
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, fullname, "opon", holderclassname);
			}

			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, fullname, "stateObj", fileNameSOClassName);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitVarInsn(ILOAD, 2);
			mv.visitMethodInsn(onChange.isModuleLevel?INVOKESTATIC:INVOKEVIRTUAL, holderclassnameNoLs, applyMethodName, "("+fileNameSOClassName+"Lcom/concurnas/bootstrap/runtime/transactions/Transaction;Z)Z", false);
			
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitInsn(IRETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		
		{//getIsInitCompleteFlag
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
		
		cw.visitEnd();
		
		this.bytecodeVisitor.localvarStackSize.pop();
		this.bytecodeVisitor.localvarStack.pop();

		this.bytecodeVisitor.addNameBtyecode(fileName, cw.toByteArray());
		
	}
	
	private void gennerateOnChangeClassForAsyncBodyBlock(AsyncBodyBlock onChange){
		String fileName = onChange.onChangeDets.getA();
		String fullname= onChange.onChangeDets.getB();
		NamedType holderclass = onChange.holderclass;
		String fileNameSO = onChange.getFullnameSO();
		String initMethodName = onChange.initMethodName;
		String cleanupMethodName = onChange.cleanupMethodName;
		String applyMethodName = onChange.applyMethodName;
		
		//YUCK!
		this.bytecodeVisitor.localvarStackSize.push(0);
		this.bytecodeVisitor.localvarStack.push(new HashMap<String, Pair<Type, Integer>>());
		//eww remember to pop!
		ClassWriter cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, bytecodeVisitor.typeDirectory);
		
		String superclassName ="com/concurnas/bootstrap/runtime/cps/" +  (onChange.hasEvery?"IsoTaskEvery":"IsoTaskNotifiable");
		
		cw.visit(V1_8, ACC_PUBLIC + ACC_SUPER + ACC_SYNTHETIC, fullname, null, superclassName, null);
		cw.visitSource(this.bytecodeVisitor.packageAndClassName + ".conc", null);

		String fileNameSOClassName = "L" + fileNameSO+";";

		String holderclassnameNoLs = holderclass.getBytecodeType();
		holderclassnameNoLs = holderclassnameNoLs.substring(1, holderclassnameNoLs.length() -1);
		String holderclassname = holderclass.getBytecodeType();
		
		{//Fields
			FieldVisitor fv = cw.visitField(ACC_PRIVATE, "stateObj", fileNameSOClassName, null, null);//Lcom/concurnas/runtime/ref/Local;
			fv.visitEnd();
			
			if(!onChange.isModuleLevel){
				fv = cw.visitField(ACC_PRIVATE, "opon", holderclassname, null, null);
				fv.visitEnd();
			}
			
			fv = cw.visitField(ACC_PRIVATE, "$isInitCompleteFlag", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef;", "Lcom/concurnas/bootstrap/runtime/ref/DefaultRef<Ljava/lang/Boolean;>;", null);
			fv.visitEnd();
		}
		
		
		{//"<init>"
			
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(" + fileNameSOClassName + (onChange.isModuleLevel?"":holderclassname) + ")V", "(" + fileNameSOClassName + (onChange.isModuleLevel?"":holderclass.getGenericBytecodeType())  + ")V", null);

			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, superclassName, "<init>", "()V", false);
			
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitFieldInsn(PUTFIELD, fullname, "stateObj", fileNameSOClassName);
			
			if(!onChange.isModuleLevel){
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 2);
				mv.visitFieldInsn(PUTFIELD, fullname, "opon", holderclassname);
			}
			

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
			
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2); 
			mv.visitEnd(); 
		}
		
		{//init
			String methodSig = "(Ljava/util/LinkedHashSet;)V";
			String methodSigGen = "(Ljava/util/LinkedHashSet<Lcom/concurnas/bootstrap/runtime/ref/DefaultRef<*>;>;)V";
			String callingClassArg = "("+fileNameSOClassName+"Ljava/util/LinkedHashSet;)V";
			
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "init", methodSig, methodSigGen, null);
			
			if(!onChange.isModuleLevel){
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, fullname, "opon", holderclassname);
			}

			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, fullname, "stateObj", fileNameSOClassName);
				mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(onChange.isModuleLevel?INVOKESTATIC:INVOKEVIRTUAL, holderclassnameNoLs, initMethodName, callingClassArg, false);
			
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		
		{//cleanup
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "cleanup", "()V", null, null);
			
			if(!onChange.isModuleLevel){
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, fullname, "opon", holderclassname);
			}

			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, fullname, "stateObj", fileNameSOClassName);
			mv.visitMethodInsn(onChange.isModuleLevel?INVOKESTATIC:INVOKEVIRTUAL, holderclassnameNoLs, cleanupMethodName, "("+fileNameSOClassName+")V", false);
			
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitInsn(RETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		

		{//apply
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "apply", "(Lcom/concurnas/bootstrap/runtime/transactions/Transaction;Z)Z", null, null);
			
			if(!onChange.isModuleLevel){
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, fullname, "opon", holderclassname);
			}

			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, fullname, "stateObj", fileNameSOClassName);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitVarInsn(ILOAD, 2);
			mv.visitMethodInsn(onChange.isModuleLevel?INVOKESTATIC:INVOKEVIRTUAL, holderclassnameNoLs, applyMethodName, "("+fileNameSOClassName+"Lcom/concurnas/bootstrap/runtime/transactions/Transaction;Z)Z", false);
			
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitInsn(IRETURN);
			mv.visitMaxs(2, 2);
			mv.visitEnd();
		}
		
		{//getIsInitCompleteFlag
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
		
		cw.visitEnd();
		
		this.bytecodeVisitor.localvarStackSize.pop();
		this.bytecodeVisitor.localvarStack.pop();

		this.bytecodeVisitor.addNameBtyecode(fileName, cw.toByteArray());
		
	}
	
	@Override
	public Object visit(OnChange onChange){
		if(onChange.asyncIndex == -1){
			createStateObjectClass(onChange.getFilenameSO(), onChange.getFullnameSO(), onChange.getExtraCapturedLocalVars(), onChange.namesOverridedInInit, false, onChange.onlyClose);//ArrayList<Expression> spliceInArgs
		}
		
		gennerateOnChangeClass(onChange, "IsoTaskNotifiable", onChange.asyncIndex );
		super.visit(onChange);
		
		return null;//null is ok
	}
	
	@Override
	public Object visit(Await onChange){
		if(onChange.asyncIndex == -1){
			createStateObjectClass(onChange.getFilenameSO(), onChange.getFullnameSO(), onChange.getExtraCapturedLocalVars(), onChange.namesOverridedInInit, false, onChange.onlyClose);//ArrayList<Expression> spliceInArgs
		}
		gennerateOnChangeClass(onChange, "IsoTaskAwait", onChange.asyncIndex);
		super.visit((OnChange)onChange);//yuck
		return null;//null is ok
	}
	
	@Override
	public Object visit(OnEvery onChange){
		if(onChange.asyncIndex == -1){
			createStateObjectClass(onChange.getFilenameSO(), onChange.getFullnameSO(), onChange.getExtraCapturedLocalVars(), onChange.namesOverridedInInit, true, onChange.onlyClose);//ArrayList<Expression> spliceInArgs
		}
		gennerateOnChangeClass(onChange, "IsoTaskEvery", onChange.asyncIndex);
		super.visit((OnChange)onChange);//yuck
		return null;//null is ok
	}
	
	@Override
	public Object visit(AsyncBodyBlock asyncBodyBlock){
		
		
		FuncParams existing = asyncBodyBlock.getExtraCapturedLocalVars();
		ArrayList< FuncParam> extraLocalFields = new ArrayList< FuncParam>();
		HashSet<String> capturedVarsAlready = new HashSet<String>();
		
		for(FuncParam fp : existing.params ){
			capturedVarsAlready.add(fp.name);
		}
		
		Set<String> fromPre = asyncBodyBlock.preBlockVars.keySet();
		
		for(LineHolder lh : asyncBodyBlock.mainBody.lines){
			for(FuncParam fp : ((OnChange)lh.l).getExtraCapturedLocalVars().params ){
				//exclude vars from preblock
				if(!fromPre.contains(fp.name) && !capturedVarsAlready.contains(fp.name) ){
					extraLocalFields.add(fp);
					capturedVarsAlready.add(fp.name);
				}
			}
		}
		
		//OK: Work on populating the below
		createStateObjectClassForAsyncBodyBlock(asyncBodyBlock.getFilenameSO(), asyncBodyBlock.getFullnameSO(), asyncBodyBlock.getExtraCapturedLocalVars(), extraLocalFields, asyncBodyBlock.namesOverridedInInit, asyncBodyBlock.childrenCount, asyncBodyBlock.preBlockVars);//ArrayList<Expression> spliceInArgs
		
		for(LineHolder lh : asyncBodyBlock.mainBody.lines){
			lh.l.accept(this);
		}
		
		gennerateOnChangeClassForAsyncBodyBlock(asyncBodyBlock);
		
		return null;
	}
	
}
