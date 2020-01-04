package com.concurnas.compiler.bytecode;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.concurnas.bootstrap.runtime.ref.LocalArray;
import com.concurnas.compiler.CaseExpressionAnd;
import com.concurnas.compiler.CaseExpressionAssign;
import com.concurnas.compiler.CaseExpressionAssignTuple;
import com.concurnas.compiler.CaseExpressionObjectTypeAssign;
import com.concurnas.compiler.CaseExpressionOr;
import com.concurnas.compiler.CaseExpressionPost;
import com.concurnas.compiler.CaseExpressionPre;
import com.concurnas.compiler.CaseExpressionTuple;
import com.concurnas.compiler.CaseExpressionWrapper;
import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.JustAlsoCaseExpression;
import com.concurnas.compiler.TypedCaseExpression;
import com.concurnas.compiler.ast.*;
import com.concurnas.compiler.ast.ArrayRefElement.LISTorMAPType;
import com.concurnas.compiler.ast.ArrayRefLevelElementsHolder.ARElementType;
import com.concurnas.compiler.ast.DeleteStatement.DSOpOn;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.ast.interfaces.FuncDefI;
import com.concurnas.compiler.ast.util.GPUKernelFuncDetails;
import com.concurnas.compiler.ast.util.JustLoad;
import com.concurnas.compiler.bytecode.FuncLocation.ClassFunctionLocation;
import com.concurnas.compiler.bytecode.FuncLocation.StaticFuncLocation;
import com.concurnas.compiler.bytecode.TopOfStack.VarClassField;
import com.concurnas.compiler.bytecode.TopOfStack.VarLocal;
import com.concurnas.compiler.bytecode.TopOfStack.VarStatic;
import com.concurnas.compiler.bytecode.TryCatchLabelTagVisitor.CatchBlockAllocator;
import com.concurnas.compiler.bytecode.TryCatchLabelTagVisitor.StartEndHandle;
import com.concurnas.compiler.typeAndLocation.Location;
import com.concurnas.compiler.typeAndLocation.LocationClassField;
import com.concurnas.compiler.typeAndLocation.LocationLocalVar;
import com.concurnas.compiler.typeAndLocation.LocationStaticField;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.Fiveple;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.utils.Sixple;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.utils.TypeDefTypeProvider;
import com.concurnas.compiler.visitors.AbstractErrorRaiseVisitor;
import com.concurnas.compiler.visitors.ErrorRaiseable;
import com.concurnas.compiler.visitors.NestedFuncRepoint;
import com.concurnas.compiler.visitors.RhsResolvesToRefTypeVisistor;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.compiler.visitors.Unskippable;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.compiler.visitors.datastructs.TheScopeFrame;
import com.concurnas.compiler.visitors.util.VarAtScopeLevel;
import com.concurnas.runtime.Pair;
import com.concurnas.runtime.Value;
import com.concurnas.runtimeCache.ReleaseInfo;

public class BytecodeGennerator implements Visitor, Opcodes, Unskippable {
	public static String metaMethodName = "$ConcurnasMetaVersion$";
	
	private final TheScopeFrame moduleScopeFrame;
	public TheScopeFrame currentScopeFrame;
	public final String packageAndClassName;
	// public MethodVisitor mv=null;

	private final static Type Const_PRIM_BOOl = new PrimativeType(PrimativeTypeEnum.BOOLEAN);
	private final static Type Const_PRIM_DOUBLE = new PrimativeType(PrimativeTypeEnum.DOUBLE);
	private final static Type Const_PRIM_INT = new PrimativeType(PrimativeTypeEnum.INT);
	private final static Type Const_PRIM_VOID = new PrimativeType(PrimativeTypeEnum.VOID);

	public BytecodeOutputter bcoutputter;

	private BytecodeGeneratorLambda lambdaGennerator = new BytecodeGeneratorLambda(this);
	private OnChangeAwaitBCGennerator onchangeGenenrator = new OnChangeAwaitBCGennerator(this);

	public final ErrorRaiseable errorRaisableSupressionFromSatc;

	public final HashMap<String, ClassDef> typeDirectory;

	public BytecodeGennerator(TheScopeFrame moduleScopeFrame, String packageAndClassName, ErrorRaiseable errorRaisableSupression, HashMap<String, ClassDef> typeDirectory) {
		this.moduleScopeFrame = moduleScopeFrame;
		this.packageAndClassName = packageAndClassName;
		this.errorRaisableSupressionFromSatc = errorRaisableSupression;
		this.typeDirectory = typeDirectory;
	}

	public void pushErrorContext(FuncDef xxx) {}
	public FuncDef popErrorContext() {return null;}
	
	public LinkedHashMap<String, byte[]> toByteArray() {
		return nameToBytecode;
	}

	protected int lastLineVisited = -1;
	public void resetLastLineVisited(){
		lastLineVisited = -1;
	}
	public int getLastLineVisited(){
		return lastLineVisited;
	}
	public void setLastLineVisited(int lineNo){
		if(lineNo <= 0){
			return;//HACK: messy, what if it actually is zero? - rare so doesnt matter
		}
		this.lastLineVisited = lineNo;
	}
	
	private ClassWriter cw;

	private LinkedHashMap<String, byte[]> nameToBytecode = new LinkedHashMap<String, byte[]>();

	private Stack<Thruple<String, ClassWriter, BytecodeOutputter>> currentCW = new Stack<Thruple<String, ClassWriter, BytecodeOutputter>>();

	private static Boolean DEBUG_MODE = false;

	public void slipIntoDebugMode() {
		DEBUG_MODE = true;
	}
	
	private String enterNewClass(ClassDef classDef, String genericSig, String superClass, boolean isAbstract, boolean isFinal, String[] ifaces, int accessorsToAdd, LinkedList<ClassDef> localClasses, boolean isTrait)// TODO: interfaces last arg
	{
		this.prepareAnyExtraLambdas();

		this.cw = new ConcClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS, typeDirectory);
		this.bcoutputter = new BytecodeOutputter(this.cw, DEBUG_MODE);

		String fullClassName = classDef == null ? this.packageAndClassName.replace('.',  '/') : classDef.bcFullName();

		if (null != classDef && classDef.hasNestedChildren) {
			// dummy entry to reverse this subsection of the ordered hashlist,
			// otherwise when testing the order is wrong for the custom
			// classloader :(
			// i.e. this results in parent's of nested classes being referenced
			// first, then the children themselves
			nameToBytecode.put(fullClassName, null);
		}

		boolean isAnnotation = (accessorsToAdd & Opcodes.ACC_ANNOTATION) == Opcodes.ACC_ANNOTATION;
		
		int acessors = (null == classDef ? ACC_PUBLIC : classDef.getAccessModifier().getByteCodeAccess());
		if(!isAnnotation && !isTrait){//include if not annotation
			acessors += ACC_SUPER;
		}
		if (isAbstract || isTrait) {
			acessors += ACC_ABSTRACT;
		}

		if(isTrait) {
			
			if(!classDef.getSuperclass().isTrait) {//extended non traitclass
				superClass =  "java/lang/Object";
			}
			
			acessors += ACC_INTERFACE;
		}
		
		if (isFinal) {
			acessors += ACC_FINAL;
		}
		
		acessors += accessorsToAdd;
		
		if(null != classDef){
			acessors += Utils.getAnnotationDependantExtraModifiers(classDef.annotations);
		}
		
		
		
		//cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER, "com/concurnas/compiler/misc/InstanceofGenericTests$MyActor", "<Axx:Ljava/lang/Object;>Lcom/concurnas/lang/TypedActor<Lcom/concurnas/compiler/misc/InstanceofGenericTests$MyClass<TAxx;>;>;", "com/concurnas/lang/TypedActor", null);
		//cw.visit(V1_8, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, "EdAndHenry$MyInterface", null, "java/lang/Object", null);
		if(isTrait) {
			if(!superClass.equals("java/lang/Object")) {
				ifaces = new String[] {superClass};
			}
			
			superClass ="java/lang/Object";
		}
		
		
		cw.visit(V1_8, acessors, fullClassName, isTrait?null:genericSig, superClass, ifaces);
		cw.visitSource(packageAndClassName + ".conc", null);

		
		
		{
			{
				MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_SYNTHETIC, metaMethodName, "()Ljava/lang/String;", null, null);
				mv.visitCode();
				mv.visitLabel(new Label());
				mv.visitLdcInsn(ReleaseInfo.getVersion());
				mv.visitInsn(ARETURN);
				mv.visitMaxs(1, 0);
				mv.visitEnd();
			}

		}
		
		
		if (classDef != null) {// if it's an inner nested class

			if(classDef.annotations != null){
				for(Annotation annot : classDef.annotations.annotations){
					annot.atClassLevel=true;
					
					if(annot.locations.isEmpty()){
						annot.accept(this);
					}
				}
			}
			
			AnnotationVisitor av0 = cw.visitAnnotation("Lcom/concurnas/lang/internal/NullStatus;", true);
			BytecodeOutputter.addNullablefieldAnnotation(av0, BytecodeOutputter.makeNullStatusAnnot(classDef.classGenricList.stream().map(a -> (Type)a).collect(Collectors.toList()), null));
			av0.visitEnd();
			
			
			if(classDef.isEnumSubClass){
				cw.visitOuterClass(classDef.getParentNestor().bcFullName(), null, null);
			}
			
			ClassDef theCurrentOne = classDef;
			ClassDef parent = theCurrentOne.getParentNestor();

			if(classDef.isLocalClass){
				String parentName = this.packageAndClassName;
				String bsFull = theCurrentOne.bcFullName();
				int access = theCurrentOne.accessModifier.getByteCodeAccess() + ACC_STATIC + accessorsToAdd;
				cw.visitInnerClass(bsFull, parentName.replace('.', '/'), theCurrentOne.bcShortName(), access + (isAnnotation?ACC_ABSTRACT:0) );
			}else{
				while (null != theCurrentOne) {
					String parentName = parent == null  ? this.packageAndClassName : parent.bcFullName();
					String bsFull = theCurrentOne.bcFullName();

					int access = theCurrentOne.accessModifier.getByteCodeAccess();

					if (null == parent ) {// first one is static, rest are not
						access += ACC_STATIC;
					}
					access += accessorsToAdd;//TODO: should have a seperate one of these?
					// cw.visitInnerClass("a/com/Child$Subclass$SubSub$SubSubSub3",
					// "a/com/Child$Subclass$SubSub", "SubSubSub3", ACC_PUBLIC);
					// cw.visitInnerClass("a/com/Child$Cls", "a/com/Child", "Cls",
					// ACC_PUBLIC + ACC_STATIC);
					cw.visitInnerClass(bsFull, parentName.replace('.', '/'), theCurrentOne.bcShortName(), access + ((isAnnotation || isTrait)?ACC_ABSTRACT:0) + (isTrait?ACC_INTERFACE:0) );
					theCurrentOne = parent;
					parent = null == theCurrentOne ? null : theCurrentOne.getParentNestor();
				}
			}
			
			
			
		}

		TheScopeFrame sfToCheck = this.currentScopeFrame;
		if (classDef != null) {
			sfToCheck = this.currentScopeFrame.getChild(classDef.classBlock);
		}

		ArrayList<ClassDef> classes = sfToCheck.getAllClasses();
		if (!classes.isEmpty()) {// now visit all immediate children if there
									// are any
			for (ClassDef innerCls : classes) {
				// cw.visitInnerClass("a/com/Child$Subclass$SubSub$SubSubSub3",
				// "a/com/Child$Subclass$SubSub", "SubSubSub3", ACC_PUBLIC);
				cw.visitInnerClass(innerCls.bcFullName(), fullClassName, innerCls.bcShortName(), ACC_PUBLIC );
			}
		}
		
		if(null != localClasses && !localClasses.isEmpty()){
			for(ClassDef innerCls : localClasses){
				cw.visitInnerClass(innerCls.bcFullName(), fullClassName, innerCls.bcShortName(), ACC_PUBLIC );
			}
		}

		currentCW.push(new Thruple<String, ClassWriter, BytecodeOutputter>(fullClassName, this.cw, this.bcoutputter));
		return fullClassName;
	}

	public void addNameBtyecode(String name, byte[] code) {
		nameToBytecode.put(name, code);
	}

	private void exitClass() {
		addAnyExtraLamdbas();

		Thruple<String, ClassWriter, BytecodeOutputter> cur = currentCW.pop();
		String name = cur.getA();
		ClassWriter cw = cur.getB();
		// BytecodeOutputter mv = cur.getC();
		cw.visitEnd();

		addNameBtyecode(name, cw.toByteArray());

		if (!currentCW.isEmpty()) {
			Thruple<String, ClassWriter, BytecodeOutputter> curNow = currentCW.peek();
			this.cw = curNow.getB();
			this.bcoutputter = curNow.getC();
			this.currentScopeFrame = this.currentScopeFrame.getParent();
		}
	}

	private Line lastStatementTouched = null;

	private void addModuleAnnotations(ArrayList<LineHolder> lines){
		ArrayList<Annotation> anots = new ArrayList<Annotation>();
		for(LineHolder lh : lines){
			Line l = lh.l;
			if(l instanceof DuffAssign){
				if(((DuffAssign) l).e instanceof Annotation){
					Annotation toAdd = (Annotation)(((DuffAssign) l).e);
					toAdd.atClassLevel = true;
					anots.add(toAdd);
				}
			}
		}
		
		if(!anots.isEmpty()){
			new Annotations(0,0,anots).accept(this);
		}
		
		
	}
	
	private void addModuleGPUKernelFunctions() {//for those for which we cannot attach to the main function definition since there isnt one for stub functions with no source
		List<GPUKernelFuncDetails> gpukfuncs = this.currentScopeFrame.getAllGPUFuncOrKernels();

		if(!gpukfuncs.isEmpty()){
			
			if(gpukfuncs.stream().anyMatch(a -> a.source == null))
			{
				AnnotationVisitor functions = cw.visitAnnotation("Lcom/concurnas/lang/GPUKernelFunctions;", true);
				{
					AnnotationVisitor funcs = functions.visitArray("gpuFuncs");
					
					for(GPUKernelFuncDetails inst : gpukfuncs) {
						if(inst.source != null) {
							continue;
						}
						visitGPUKernelFunctionAnnotation(funcs.visitAnnotation(null, "Lcom/concurnas/lang/GPUKernelFunction;"), inst);
						
					}
					funcs.visitEnd();
				}
			}
			
		}
	}
	
	private void visitGPUKernelFunctionAnnotation(AnnotationVisitor gpufunc, GPUKernelFuncDetails inst) {
		//name, sig, src, args
		gpufunc.visit("name", inst.name);
		gpufunc.visit("signature", inst.signature);
		gpufunc.visit("source", inst.source == null ? "" : inst.source);
		gpufunc.visit("globalLocalConstant", inst.globalLocalConstant);
		gpufunc.visit("inout", inst.inout);
		gpufunc.visit("dims", inst.dims);
		
		AnnotationVisitor arr = gpufunc.visitArray("dependancies");
		{
			for(GPUKernelFuncDetails dep : inst.dependancies) {
				{
					AnnotationVisitor av2 = arr.visitAnnotation(null, "Lcom/concurnas/lang/GPUKernelFunctionDependancy;");
					av2.visit("dims", dep.dims);
					av2.visit("dclass", dep.dclass);
					av2.visit("globalLocalConstant", dep.globalLocalConstant);
					av2.visit("inout", dep.inout);
					av2.visit("name", dep.name);
					av2.visit("signature", dep.signature);
					av2.visitEnd();
				}
			}
		}
		
		arr.visitEnd();
		gpufunc.visitEnd();
	}
	
	
	private void addModuleTypeDefs(){
		Map<String, TypeDefTypeProvider> moduletypedefs = this.currentScopeFrame.getAllTypeDefAtCurrentLevel();
		
		if(!moduletypedefs.isEmpty()){
			AnnotationVisitor typdefsholder = cw.visitAnnotation("Lcom/concurnas/lang/Typedefs;", true);
			{
				AnnotationVisitor typedefs= typdefsholder.visitArray("typedefs");
				
				for(String name : moduletypedefs.keySet()){
					TypeDefTypeProvider typeProvider = moduletypedefs.get(name);
					for( Fiveple<Type, ArrayList<GenericType>, AccessModifier, String, String> item : typeProvider.argsToTypeAndGens.values()){
						
						AnnotationVisitor typedef = typedefs.visitAnnotation(null, "Lcom/concurnas/lang/Typedef;");
						typedef.visit("name", name);
						
						Type rhsToQualify = item.getA();
						ArrayList<GenericType> generics = item.getB();
						AccessModifier am = item.getC();
						String location = item.getD();
						
						String bc = rhsToQualify.getGenericBytecodeType();
						typedef.visit("type", bc);
						{
							AnnotationVisitor av3 = typedef.visitArray("args");
							for(GenericType gt : generics){
								av3.visit(null, gt.name);
							}
							av3.visitEnd();
						}
						
						typedef.visit("accessModifier", am.toString());
						
						typedef.visit("location", location);
						
						typedef.visitEnd();
					}
					
					
					
				}
				typedefs.visitEnd();
			}
			typdefsholder.visitEnd();
		}
	}
	
	@Override
	public Object visit(Block block) {
		boolean isTopLeve = null == currentScopeFrame;

		if (!block.isolated && !block.isEmpty()) {
			lastStatementTouched = null;
		}

		boolean previsInclInitCreator = isInclInitCreator;
		boolean previsFieldBlockRhs = isFieldBlockRhs;
		Type gotType=null;
		
		if(block.canNestModuleLevelFuncDefs) {
			this.level++;
		}
		
		if (isTopLeve) {// module level, special

			
			
			// /first lests scan the whole module and create any lambda ref
			// classes if they are required (and also tag the lambda refs as
			// approerpiate
			lambdaGennerator.visit(block);
			onchangeGenenrator.visit(block);

			this.currentScopeFrame = moduleScopeFrame;
			
			

			NestedLocalClassFinderJustClassDef nlcf = new NestedLocalClassFinderJustClassDef();
			nlcf.visit(block);
			
			// header...
			enterNewClass(null, null, "java/lang/Object", false, true, null, 0, nlcf.localClasses, false);

			addModuleAnnotations(block.lines);
			
			if(null != this.currentScopeFrame.gpuKernelFuncDetails) {
				visitGPUKernelFunctionAnnotation(cw.visitAnnotation("Lcom/concurnas/lang/GPUKernelFunction;", true), this.currentScopeFrame.gpuKernelFuncDetails);
			}
			
			
			addModuleTypeDefs();
			addModuleGPUKernelFunctions();
			
			// first incase of exceptions in finally block
			/*
			 * TryCatchFinallyBlockCodeCopier tcfbcc = new
			 * TryCatchFinallyBlockCodeCopier(); tcfbcc.visit(block);
			 */

			/*
			 * LabelAllocator preAllocator = new LabelAllocator();
			 * preAllocator.visit(block);
			 */

			// ASM state incorrect - scan forward the trycatch heirachy for nested
			// instances and determine ranges, then preallocate and perform all
			// the visitTryCatchBlock based on those
			// TryCatchLabelTagVisitor tcLabelVisPrealloc = new
			// TryCatchLabelTagVisitor();
			// tcLabelVisPrealloc.visit(block);

			createAllModuleFields();

			if (createClinit(block)) {
				this.localvarStack.push(new HashMap<String, Pair<Type, Integer>>());
				this.localvarStackSize.push(0);

				boolean prevIsInclInitCreator = isInclInitCreator;
				isInclInitCreator = true;
				bcoutputter.enterMethod(ACC_STATIC, "<clinit>", "()V", null, null, getFullModuleAndClassName());
				visitTryCatchLabels(block);

				gotType = acceptLines(block, true);// initial pass to do static module vars

				if (null != block.mustVisitLabelBeforeRet) {
					bcoutputter.visitLabel(block.mustVisitLabelBeforeRet);
				}

				bcoutputter.visitInsn(RETURN);
				bcoutputter.exitMethod();
				isInclInitCreator = prevIsInclInitCreator;

				this.localvarStack.pop();
				this.localvarStackSize.pop();
			}

			// now we enter module level "constructor"
			bcoutputter.enterMethod(ACC_PUBLIC, "<init>", "()V", null, null, getFullModuleAndClassName());

			bcoutputter.visitVarInsn(ALOAD, 0);
			bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");

			// now we do the static initialization blocks
			bcoutputter.visitInsn(RETURN);
			bcoutputter.exitMethod();
		} else {
			/*
			 * LabelAllocator preAllocator = new LabelAllocator();
			 * preAllocator.visit(block);
			 */

			// TheScopeFrame prev = this.currentScopeFrame;//TODO: delete
			TheScopeFrame enterChild = this.currentScopeFrame.getChild(block);

			/*
			 * if(null == enterChild){ //if cannot find the block it's probably
			 * a lambda, which gets default instantiated within a default
			 * constrcutor, so check parent class for block }
			 */

			// System.err.println(String.format("==> %s, %s",
			// System.identityHashCode(currentScopeFrame),System.identityHashCode(enterChild)));
			this.currentScopeFrame = enterChild;

			if (enterChild == null) {
				throw new RuntimeException("currentScopeFrame cannot be null");
			}

			this.localvarStack.push(new HashMap<String, Pair<Type, Integer>>());
			int incSize = 0;

			if (block.localArgVarOffset > -1) {
				incSize = block.localArgVarOffset;
			} else {
				if (block.isLocalizedLambda) {// MHA: helps wit: class MyCls{
												// masterSimple = fun (a int, b
												// int, c int) int { return
												// a+b+c; } }
					incSize++;
				} else if (!enterChild.isClass() && !enterChild.staticFunc && !block.staticFuncBlock) {
					if (localvarStackSize.isEmpty()) {
						incSize = 1;
					} else {
						incSize = localvarStackSize.peek() + 1;
					}
				}

			}
			
			if (block.isMethodBlock || block.isLocalizedLambda || block.isClassFieldBlock) {
				if(block.isClassFieldBlock){
					isInclInitCreator=false;
					isFieldBlockRhs=true;
					if(!isInsideDefaultConstuctorFieldInit){
						this.level++;
					}
				}
				localvarStackSize.push(incSize);
			}/*else if(block.canNestModuleLevelFuncDefs) {
				this.level++;
			}*/

			for (Pair<Type, String> input : this.varsToAddToScopeOnEntry) {
				// TODO: ifentering a class method non satic, arg 0 will be the 'this' reference
				Type toSaveInMemAs = input.getA();
				// Type varType = TypeCheckUtils.hasRefLevels(toSaveInMemAs)?
				// Const_Object : toSaveInMemAs;
				createNewLocalVar(input.getB(), toSaveInMemAs, false);// TODO:ensure
																		// ref
																		// created?
			}
			this.varsToAddToScopeOnEntry = new ArrayList<Pair<Type, String>>();
			// }
		}

		gotType = acceptLines(block, false);

		if(block.isClassFieldBlock){
			isInclInitCreator=previsInclInitCreator;
			isFieldBlockRhs=previsFieldBlockRhs;
			if(!isInsideDefaultConstuctorFieldInit){
				this.level--;
			}
		}else if(block.canNestModuleLevelFuncDefs) {
			this.level--;
		}
		
		if (isTopLeve) {
			addTopLevelAccssors(this.currentScopeFrame);
		} else if (this.currentScopeFrame.isClass()) {
			addClassLevelAccssors(this.currentScopeFrame);
		}

		if (isTopLeve) {
			exitClass();
		} else if (!this.currentScopeFrame.isClass()) {
			//TheScopeFrame moi = this.currentScopeFrame;
			this.currentScopeFrame = this.currentScopeFrame.getParent();
			// ??correct?
		}

		if (!this.localvarStack.isEmpty()) {// JPT: try to remove this if
											// catcher
			this.localvarStack.pop();
		}
		
		if ((block.isMethodBlock || block.isLocalizedLambda || block.isClassFieldBlock) && !this.localvarStackSize.isEmpty()) {
			// only pop out if method, else can stay here, variabels in methods are allocatted in linier fashion
			this.localvarStackSize.pop();
		}

		Type retType = block.getTaggedType();
		
		if(retType != null && null != gotType && !gotType.equals(retType)){//ensure type returned from block matches that which we have tagged it as retunring (hence we cast as approperiate)
			Utils.applyCastImplicit(bcoutputter, gotType, retType, this, true);
		}
		
		return retType;
	}

	private void addClassLevelAccssors(TheScopeFrame currentScopeFrame) {
		final HashSet<String> psardoneAlready1 = new HashSet<String>();
		
		for (TypeAndLocation dude : this.currentScopeFrame.getAllVaraiblesAtScopeLevelNeedingAccessor()) {
			/*
			 * static access$0(I1) : int ALOAD 0 GETFIELD I1.g : int IRETURN
			 */

			Type retType = dude.getType();
			Location loc = dude.getLocation();

			Pair<String, String> accAndOrig = dude.getLocation().getPrivateStaticAccessorRedirectFuncGetter();

			if(!psardoneAlready1.contains(accAndOrig.getA())){
				String owner = ((LocationClassField) loc).getOwner().replace('.', '/');

				ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> inputs = new ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>();
				inputs.add(new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(((LocationClassField) loc).ownerType, null, null, false, false, false));

				this.enterMethod(accAndOrig.getA(), inputs, retType, false, AccessModifier.PUBLIC, ACC_STATIC + ACC_SYNTHETIC, false);
				bcoutputter.visitVarInsn(ALOAD, 0);
				this.bcoutputter.visitFieldInsn(GETFIELD, owner, accAndOrig.getB(), retType.getBytecodeType());
				this.bcoutputter.visitInsn(Utils.returnTypeToOpcode(retType));
				this.exitMethod();
				psardoneAlready1.add(accAndOrig.getA());
			}
			
			
		}

		final HashSet<String> psardoneAlready2 = new HashSet<String>();
		for (TypeAndLocation dude : this.currentScopeFrame.getAllVaraiblesAtScopeLevelNeedingAccessorSetter()) {
			/*
			 * static access$0(I1, int) : void ALOAD 0 ILOAD 1 PUTFIELD I1.g :
			 * int RETURN
			 * 
			 * { mv = cw.visitMethod(ACC_STATIC + ACC_SYNTHETIC, "access$0",
			 * "(LI1;I)V", null, null); mv.visitVarInsn(ALOAD, 0);
			 * mv.visitVarInsn(ILOAD, 1); mv.visitFieldInsn(PUTFIELD, "I1", "g",
			 * "I"); mv.visitInsn(RETURN); }
			 */

			Type retType = dude.getType();
			Location loc = dude.getLocation();
			Thruple<String, String, Type> accAndOrig = dude.getLocation().getPrivateStaticAccessorRedirectFuncSetter();

			if(!psardoneAlready2.contains(accAndOrig.getA())){
				Type inputType = accAndOrig.getC();
				ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> inputs = new ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>();
				inputs.add(new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(((LocationClassField) loc).ownerType, null, null, false, false, false));
				inputs.add(new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(inputType, null, null, false, false, false));

				this.enterMethod(accAndOrig.getA(), inputs, Const_PRIM_VOID, false, AccessModifier.PUBLIC, ACC_STATIC + ACC_SYNTHETIC, false);
				bcoutputter.visitVarInsn(ALOAD, 0);
				Utils.applyLoad(bcoutputter, inputType, 1);
				this.bcoutputter.visitFieldInsn(PUTFIELD, ((LocationClassField) dude.getLocation()).getOwner().replace('.', '/'), accAndOrig.getB(), retType.getBytecodeType());
				this.bcoutputter.visitInsn(RETURN);
				this.exitMethod();
				psardoneAlready2.add(accAndOrig.getA());
			}
			
			
		}

		final HashSet<String> psardoneAlready3 = new HashSet<String>();
		for (TypeAndLocation funcDude : this.currentScopeFrame.getAllFunctionsAtScopeLevelNeedingAccessor()) {
			// ?

			ClassFunctionLocation loc = (ClassFunctionLocation) funcDude.getLocation();
			FuncType funcType = ((FuncType) funcDude.getType());
			
			Pair<String, String> accAndOrig = funcDude.getLocation().getPrivateStaticAccessorRedirectFuncGetter();


			if(!psardoneAlready3.contains(accAndOrig.getA())){
				ArrayList<Type> inputs = funcType.getInputs();

				ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> args = new ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>();
				args.add(new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(loc.getOwnerType(), null, null, false, false, false));

				for (Type a : inputs) {
					//Utils.applyLoad(mv, a, space++);
					args.add(new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(a, null, null, false, false, false));
				}
				this.enterMethod(accAndOrig.getA(), args, funcType.retType, false, AccessModifier.PUBLIC, ACC_STATIC + ACC_SYNTHETIC, false);
				
				Utils.applyLoad(bcoutputter, loc.getOwnerType(), 0);
				int space = 1;
				for (Type a : inputs) {
					Utils.applyLoad(bcoutputter, a, space);
					space += Utils.varSlotsConsumedByType(a);
					//args.add(new Tuple<Type, String>(a, null));
				}
				
				String methodDesc = getNormalMethodInvokationDesc(inputs, funcType.retType);

				String ownerStr = ((NamedType) loc.getOwnerType()).getBytecodeType();
				ownerStr = ownerStr.substring(1, ownerStr.length() - 1);
				//mv.visitVarInsn(ALOAD, 0);
				bcoutputter.visitMethodInsn(INVOKESPECIAL, ownerStr, accAndOrig.getB(), methodDesc);
				this.bcoutputter.visitInsn(Utils.returnTypeToOpcode(funcType.retType));
				this.exitMethod();

				/*
				 * mv = cw.visitMethod(ACC_STATIC + ACC_SYNTHETIC, "access$0",
				 * "(LA/Waskle;)V", null, null); mv.visitCode();
				 * mv.visitVarInsn(ALOAD, 0); mv.visitMethodInsn(INVOKESPECIAL,
				 * "A/Waskle", "functo", "()V"); mv.visitInsn(RETURN);
				 * mv.visitEnd();
				 */

				psardoneAlready3.add(accAndOrig.getA());
			}
			
			

		}

	}

	private void addTopLevelAccssors(TheScopeFrame currentScopeFrame) {

		// check to see if any accessor methods need to be created
		final HashSet<String> psardoneAlready = new HashSet<String>();//sometimes using a var inside a nested function can caues the psar creation to otherwise be gennerated twice! :(
		for (TypeAndLocation dude : this.currentScopeFrame.getAllVaraiblesAtScopeLevelNeedingAccessor()) {
			Type retType = dude.getType();
			Location loc = dude.getLocation();

			Pair<String, String> accAndOrig = dude.getLocation().getPrivateStaticAccessorRedirectFuncGetter();
			
			if(!psardoneAlready.contains(accAndOrig.getA())){
				this.enterMethod(accAndOrig.getA(), new ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>(), retType, false, AccessModifier.PUBLIC, ACC_SYNTHETIC, false);
				this.bcoutputter.visitFieldInsn(GETSTATIC, ((LocationStaticField) loc).owner.replace('.', '/'), accAndOrig.getB(), retType.getBytecodeType());
				this.bcoutputter.visitInsn(Utils.returnTypeToOpcode(retType));
				this.exitMethod();
				psardoneAlready.add(accAndOrig.getA());
			}
			
		
		}

		final HashSet<String> psardoneAlready2 = new HashSet<String>();
		for (TypeAndLocation dude : this.currentScopeFrame.getAllVaraiblesAtScopeLevelNeedingAccessorSetter()) {
			/*
			 * static access$1(int) : void L0 LINENUMBER 5 L0 ILOAD 0 PUTSTATIC
			 * Glonas2.r : int RETURN MAXSTACK = 1 MAXLOCALS = 1 }
			 */
			Type retType = dude.getType();
			Thruple<String, String, Type> accAndOrig = dude.getLocation().getPrivateStaticAccessorRedirectFuncSetter();

			if(!psardoneAlready2.contains(accAndOrig.getA())){
				Type inputType = accAndOrig.getC();
				ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> inputs = new ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>();
				inputs.add(new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(inputType, null, null, false, false, false));

				this.enterMethod(accAndOrig.getA(), inputs, Const_PRIM_VOID, false, AccessModifier.PUBLIC, ACC_SYNTHETIC, false);
				Utils.applyLoad(bcoutputter, inputType, 0);
				this.bcoutputter.visitFieldInsn(PUTSTATIC, ((LocationStaticField) dude.getLocation()).owner.replace('.', '/'), accAndOrig.getB(), retType.getBytecodeType());
				this.bcoutputter.visitInsn(Opcodes.RETURN);
				this.exitMethod();
				psardoneAlready2.add(accAndOrig.getA());
			}
			
			
		}

		// and also functions
		final HashSet<String> psardoneAlready3 = new HashSet<String>();
		for (TypeAndLocation funcDude : this.currentScopeFrame.getAllFunctionsAtScopeLevelNeedingAccessor()) {
			StaticFuncLocation loc = (StaticFuncLocation) funcDude.getLocation();
			FuncType funcType = ((FuncType) funcDude.getType());
			Pair<String, String> accAndOrig = funcDude.getLocation().getPrivateStaticAccessorRedirectFuncGetter();

			if(!psardoneAlready3.contains(accAndOrig.getA())){
				ArrayList<Type> inputs = funcType.getInputs();
				ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> ugh = new ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>();
				for (Type a : inputs) {
					ugh.add(new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(a, null, null, false, false, false));
				}
				this.enterMethod(accAndOrig.getA(), ugh, funcType.retType, false, AccessModifier.PUBLIC, ACC_SYNTHETIC, false);

				int space = 0;
				for (Type a : inputs) {
					Utils.applyLoad(bcoutputter, a, space);
					space+=Utils.varSlotsConsumedByType(a);
				}
				
				String methodDesc = getNormalMethodInvokationDesc(inputs, funcType.retType);
				bcoutputter.visitMethodInsn(INVOKESTATIC, ((NamedType) loc.getOwnerType()).getNamedTypeStr().replace('.', '/'), accAndOrig.getB(), methodDesc);
				this.bcoutputter.visitInsn(Utils.returnTypeToOpcode(funcType.retType));
				this.exitMethod();
				psardoneAlready3.add(accAndOrig.getA());
			}
			
			
		}

	}

/*	private String xTractOwnerFromLocatin(Location loc) {
		return loc instanceof LocationStaticField ? ((LocationStaticField) loc).owner : ((LocationClassField) loc).getOwner();
	}
*/
	private Stack<Type> currentFuncReturnType = new Stack<Type>();

	private void enterMethod(String name, ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> inputs, Type returnType, boolean isAbstract, boolean isAtModuleLevel, boolean isFinal, AccessModifier am, int extraModifiers, Annotations annots, boolean isOverride, ArrayList<Pair<String, NamedType>> methodGenricList) {
		bcoutputter.enterMethod(name, inputs, returnType, isAtModuleLevel, isAbstract, isFinal, am, extraModifiers, getFullModuleAndClassName(), annots, isOverride, methodGenricList);
		currentFuncReturnType.push(returnType);
	}

	private void enterMethod(String name, ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> inputs, Type returnType, boolean isAbstract, AccessModifier am, int extraModifiers, boolean isOverride) {
		enterMethod(name, inputs, returnType, isAbstract, this.isAtModuleLevel(), false, am, extraModifiers, null, isOverride, null);
	}

	private void exitMethod() {
		bcoutputter.exitMethod();
		currentFuncReturnType.pop();
	}

	private boolean inspecialFieldInitConstrutor = false;

	private boolean isAtModuleLevel() {
		return this.currentScopeFrame.paThisIsModule || inspecialFieldInitConstrutor;
	}

	private boolean isAtClassLevel() {
		return (this.currentScopeFrame.isClass() || inspecialFieldInitConstrutor) && !isFieldBlockRhs ;
	}

	private boolean isAtModuleLevelOrClassLevel() {
		return isAtModuleLevel() || isAtClassLevel();
	}

	private HashMap<String, Type> createAllModuleFields() {
		HashMap<String, Type> fieldNameToType = new HashMap<String, Type>();
		boolean isMNmod = isAtModuleLevel();
		ArrayList<VarAtScopeLevel> fields = currentScopeFrame.getAllVariablesAtScopeLevel(false, true, true, false);
		for (VarAtScopeLevel field : fields) {
			Type tt = field.getType();
			
			if(ScopeAndTypeChecker.const_void_thrown.equals(tt)) {
				continue;
			}
			
			fieldNameToType.put(field.getVarName(), tt);

			String normalType = tt.getBytecodeType();
			String genericType = tt.getGenericBytecodeType();
			if (normalType.equals(genericType)) {
				genericType = null;
			}
			boolean isfinal = field.isFinal();

			AccessModifier am = field.getAccessModifier();// public, private, protected is
												// stored here

			int fieldSet = 0;
			if (null != am) {
				if (am == AccessModifier.PRIVATE) {
					fieldSet += Opcodes.ACC_PRIVATE;
				} else if (am == AccessModifier.PROTECTED) {
					fieldSet += Opcodes.ACC_PROTECTED;
				} else if(am == AccessModifier.PACKAGE) {// public
					fieldSet += 0;
				}else {// public
					fieldSet += Opcodes.ACC_PUBLIC;
				}
			}

			if (isMNmod) {
				fieldSet += ACC_STATIC;
			}
			Object defaultValue = null;
			if (isfinal) {
				fieldSet += ACC_FINAL;
				//defaultValue
				defaultValue = ((Node)tt).getFoldedConstant();
			}
			
			
			fieldSet += field.getExtraModifiers();
			
			if(field.isShared()) {
				fieldSet += ACC_VOLATILE;
			}
			

			FieldVisitor fv = cw.visitField(fieldSet, field.getVarName(), normalType, genericType, defaultValue);
			// fv = cw.visitField(0, "xxx", "[Ljava/lang/String;", null, null);
			// fv = cw.visitField(ACC_PUBLIC, "c", "[Ljava/lang/String;", null,
			// null);
			// fv = cw.visitField(ACC_PUBLIC, "d",
			// "[Lcom/concurnas/bootstrap/lang/Lambda$Function0;",
			// "[Lcom/concurnas/bootstrap/lang/Lambda$Function0<Ljava/lang/Integer;>;", null);
			// [Lcom/concurnas/bootstrap/lang/Lambda$Function0;
			// [Lcom/concurnas/bootstrap/lang/Lambda$Function0<Ljava/lang/Integer;>;
			Annotations annots = field.getAnnotations();
			if(null != annots){
				for(Annotation annot : annots.annotations){
					ArrayList<String> locations = annot.locations;
					//only visit this if there are no locations OR if field is explicitly requested
					if(locations.isEmpty() || locations.contains("field")){
						annot.fieldVisitor = fv;
						annot.accept(this);
					}
				}
			}
			
			if(!TypeCheckUtils.isPurePrimative(tt) || tt.hasArrayLevels()) {
				AnnotationVisitor av0 = fv.visitAnnotation("Lcom/concurnas/lang/internal/NullStatus;", true);
				BytecodeOutputter.addNullablefieldAnnotation(av0, BytecodeOutputter.makeNullStatusAnnot(null, tt));
				av0.visitEnd();
			}
			
			
			fv.visitEnd();
		}

		if (!this.currentClassDefObj.isEmpty()) {
			ClassDef curClass = this.currentClassDefObj.peek();
			ClassDef parent = curClass.getParentNestor();
			if (!curClass.isEnumSubClass && null != parent) {// in nested classes we ad an extra field which
									// holds a ref to nestor
				Type tt = new NamedType(parent);

				String normalTypePar = tt.getBytecodeType();
				String genericTypePar = tt.getGenericBytecodeType();

				// String parclasName = parent.bcFullName();
				FieldVisitor fv = cw.visitField(ACC_FINAL + ACC_SYNTHETIC, "this$" + curClass.getNestingLevel(), normalTypePar, genericTypePar, null);
				fv.visitEnd();
			}
		}
		return fieldNameToType;
	}

	public Stack<HashMap<String, Pair<Type, Integer>>> localvarStack = new Stack<HashMap<String, Pair<Type, Integer>>>();
	public Stack<Integer> localvarStackSize = new Stack<Integer>();

	// private Stack<Integer> localvarStackSizeModuleLevelTemps = new
	// Stack<Integer>();

	public int createNewLocalVar(String name, Type storeAs, boolean doStore) {
		return createNewLocalVar(name, false, storeAs, doStore, false, false, -1);
	}
	
	private int level = 0;
	
	public int createNewLocalVar(String name, boolean shouldMakeRef, Type storeAs, boolean doStore, boolean overriteLHSRef, boolean extraRefDup, int createOuuterLevelOnly) {
		int incNumber = localvarStackSize.pop();
		int slot = incNumber;
		localvarStack.peek().put(name, new Pair<Type, Integer>(storeAs, incNumber));
		localvarStack.peek().put(name+"$n"+level, new Pair<Type, Integer>(storeAs, incNumber));//alias
		 //System.err.println( String.format("CVAR: %s -> %s", name, incNumber));

		incNumber++;
		if (storeAs instanceof PrimativeType) {
			PrimativeTypeEnum tt = ((PrimativeType) storeAs).type;
			if (tt == PrimativeTypeEnum.DOUBLE || tt == PrimativeTypeEnum.LONG) {
				// double and longtake  up 64bit, so two slots...
				if(storeAs.getArrayLevels() ==0){//but arrays take up 32 bit regardless of the type
					incNumber++;
				}
			}
		}
		localvarStackSize.push(incNumber);

		if (doStore || shouldMakeRef) {
			if (shouldMakeRef) {
				// make new ref only if from is not ref
				Utils.createRef(bcoutputter, storeAs, slot, doStore, extraRefDup, createOuuterLevelOnly);
			} else {
				storeLocalVaraible(name, null, storeAs, overriteLHSRef/*, null*/);
			}
		}

		// maybe move the above?

		return slot;
	}

	/*private boolean skipAssignNewInInit(AssignNew assignNew){
		//to be skiped in clinit - must be final, at module level and have a folded constant
		if(isAtModuleLevel() ){
			if(((Node)assignNew.getTaggedType()).getFoldedConstant() != null){
				//ignore
				return assignNew.isFinal;
			}
		}
		return false;
	}*/
	
	public Object visit(AssignNew assignNew) {
		if(assignNew.isAnnotationField){
			return null;
		}

		Type type = assignNew.getTaggedType();
		boolean isRef = TypeCheckUtils.hasRefLevels(type);
		boolean isClassLevel = isAtClassLevel();
		if (isClassLevel && (assignNew.expr != null || isRef)) {
			/*
			 * at class level we do: L1 LINENUMBER 14 L1 ALOAD 0: this BIPUSH 9
			 * PUTFIELD Child$Cls.a : int
			 */
			bcoutputter.visitVarInsn(ALOAD, 0);
		}

		String name = assignNew.name;
		if(type instanceof NamedType && ((NamedType)type).astredirect != null){
			type = ((NamedType)type).astredirect;
		}
		

		Type rhsType = null;
		if (assignNew.expr != null) {
			//boolean previsInclInitCreator = isInclInitCreator;
			//isInclInitCreator=false;
			rhsType = (Type) (((Type) assignNew.expr.accept(this)).copy());
			//isInclInitCreator = previsInclInitCreator;
			
			donullcheckForUnknown(rhsType, type);
			
			if (rhsType.equals(Const_Object)) {
				// special case: o = z: as Object; o3 Object: = o //o could be
				// [is] a ref already!
				// so instead translate into this form: o3 Object: = o as
				// Object: // as cast logic correctly checks for lhs being of
				// the type of the thing already
				// introduce fake cast for this - since cast does it properly ;)
				// int g = 9;
				rhsType = doCast(rhsType, type);
			}

			boolean refMismatch = TypeCheckUtils.getRefLevels(rhsType) != TypeCheckUtils.getRefLevels(type);
			boolean unrefing = TypeCheckUtils.getRefLevels(rhsType) > TypeCheckUtils.getRefLevels(type);

			if (isInclInitCreator && isRef & refMismatch) {// but ensure u
															// create the ref
															// shell
				if (unrefing) {
					if (!type.equals(rhsType)) {
						Type cop = (Type) rhsType.copy();
						TypeCheckUtils.unlockAllNestedRefs(cop);

						Utils.applyCastImplicit(bcoutputter, cop, type, this, true);
					}
				} else {
					int slot = createNewLocalVar(name + "$tempVi", true, type, true, true, true, -1);// hacky...

					Utils.genericSswap(bcoutputter, type, rhsType);

					if (!type.equals(rhsType)) {
						Type cop = (Type) rhsType.copy();
						TypeCheckUtils.unlockAllNestedRefs(cop);
						Utils.applyCastImplicit(bcoutputter, cop, type, this, true);
					}

					bcoutputter.visitVarInsn(ALOAD, slot);// shudder
				}

				rhsType = type;
			}

		} else {
			if (isInclInitCreator) {// skip if nothing to assign to static var,
									// unless ur dealing with a ref, in which
									// case u need to create one
				if (isRef) {// but ensure u create the ref shell
					createNewLocalVar(name, true, type, true, true, true, -1);
					rhsType = type;
				} else {
					return null;
				}
			}
		}

		boolean rhsIsRef = TypeCheckUtils.hasRefLevels(rhsType);

		boolean rhsLessRef = TypeCheckUtils.getRefLevels(rhsType) < TypeCheckUtils.getRefLevels(type);
		boolean lhsLessRef = TypeCheckUtils.getRefLevels(rhsType) > TypeCheckUtils.getRefLevels(type);
		boolean diffRefLevels = TypeCheckUtils.getRefLevels(rhsType) != TypeCheckUtils.getRefLevels(type);

		boolean doStore = null != rhsType;
		if (doStore && !isRef) {
			if (!type.equals(rhsType)) {
				Utils.applyCastImplicit(bcoutputter, rhsType, type, this, true);
			}
		}

		if (assignNew.preStoreDup)// MHA: the for loop uses this to prevent an
									// exta load operation being required
		{
			bcoutputter.visitInsn(DUP);
		}

		boolean classLevelThing = (isClassLevel || (isInclInitCreator && this.isAtModuleLevelOrClassLevel())) && !assignNew.isTempVariableAssignment;

		if (classLevelThing) {
			// if we're in the isInclInitCreator, we use putstatic
			if (doStore && isRef) {
				Location loc = isClassLevel ? new LocationClassField(getFullModuleAndClassName(), type) : new LocationStaticField(getFullModuleAndClassName(), type);

				storeLocalVaraible(name, loc, type, rhsType, isInclInitCreator/*, null*/);
			} else {
				
				if(isClassLevel && this.currentClassDefObj.peek().isTrait) {
					//traits dont have fields
					bcoutputter.visitMethodInsn(INVOKEINTERFACE, getFullModuleAndClassName(), "traitVar$" + name, "("+type.getBytecodeType()+")V", true);
				}else {
					// 2. getvar code now needs to register static fields... go add
					// that to ur scopeandtypecheker (have fun!)
					bcoutputter.visitFieldInsn(isClassLevel ? PUTFIELD : PUTSTATIC, getFullModuleAndClassName(), name, type.getBytecodeType());
				}
			}

		} else {

			if (!lhsLessRef) {
				if (isRef && rhsIsRef) {// weird...
					// createNewLocalVar(name,
					// ((NamedType)type).getGenTypes().get(0), type, true,
					// true);
					createNewLocalVar(name, rhsLessRef, type, true, true, assignNew.expr != null, -1);
				} else {
					createNewLocalVar(name, rhsLessRef, type, doStore || ((isRef || rhsIsRef) && null == rhsType), false, assignNew.expr != null, -1);
				}
			}

			// createNewLocalVar(name, type, type, doStore);

			// if(doStore && isRef && !rhsIsRef){
			if (doStore && isRef && diffRefLevels) {
				if (rhsLessRef) {// x int::= 8!
					Utils.genericSswap(bcoutputter, type, rhsType);
				}

				if (!type.equals(rhsType)) {
					Type cop = (Type) rhsType.copy();
					TypeCheckUtils.unlockAllNestedRefs(cop);
					Utils.applyCastImplicit(bcoutputter, cop, type, this, true);
				}
				// mv.visitMethodInsn(INVOKEVIRTUAL,
				// "com/concurnas/runtime/ref/Local", "set",
				// "(Ljava/lang/Object;)V");

				if (lhsLessRef) {
					createNewLocalVar(name, rhsLessRef, type, true, true, true, -1);
				}
			} else if (lhsLessRef) {
				createNewLocalVar(name, rhsLessRef, type, true, true, true, -1);
			}
		}

		return null;
	}

	private String getFullModuleAndClassName() {
		StringBuilder sb = new StringBuilder(this.packageAndClassName.replace('.', '/'));

		for (int n = 0; n < this.currentClassDef.size(); n++) {
			sb.append("$");
			sb.append(this.currentClassDef.get(n));
		}

		return sb.toString();
	}

	private void setTerminalRefPreccededByThis(Expression terminatal, boolean preceededByThis) {
		((Node) terminatal).setPreceededByThis(preceededByThis);

		/*
		 * if(terminatal instanceof RefName) {//first ish (ok 2nd consdiering
		 * head) ((RefName)terminatal).preceededByThis = true; } else
		 * if(terminatal instanceof ArrayRef) { ArrayRef ar =
		 * (ArrayRef)terminatal; if(ar.expr instanceof RefName) {
		 * ((RefName)ar.expr).preceededByThis = true; } }
		 */
	}

	private boolean isRef(Type tt) {
		return tt instanceof NamedType && ((NamedType) tt).getIsRef() && !tt.hasArrayLevels();
	}

	private boolean rhsResolvesToRefType(AssignExisting ass) {
		// e.g. a int:; a= {{1+1}! if 4>6 else 12}//i.e. a is not new
		// so tag a as being used in the rhs since the first async block will
		// write directly to it (works for array refs etc too)

		// TODO: slightly inefficient, better would be one run through the whole
		// program to look for all instances, rather than doing each one
		// only really protects against nested ref invokations
		return new RhsResolvesToRefTypeVisistor(ass).doesRhsResolve();
	}

	@Override
	public Object visit(AssignExisting assignExisting) {
		// lhs can only be dooperator*[arrayref or refname]
		if(assignExisting.isAnnotationField){
			return null;
		}
		
		Expression terminatal = assignExisting.assignee;
		AssignStyleEnum eqStyle = assignExisting.eq;
		boolean firstDotOpElementIsRefThis = false;

		
		if(terminatal instanceof ExpressionList) {
			terminatal = (Expression)((ExpressionList)terminatal).astRedirect;
		}
		
		// boolean dupAlready = false;
		boolean wasRedirected = false;
		if (terminatal instanceof RefName) {// yuck!
			RefName asT = (RefName) terminatal;

			if (null != asT.astRedirectforOnChangeNesting) {
				terminatal = (Expression) asT.astRedirectforOnChangeNesting;
				wasRedirected=true;
			}
		}
		
		Expression rhsToProcess = assignExisting.expr;
		
		boolean popIfActingOnstatic = false;
		if (terminatal instanceof DotOperator) {
			/*
			 * dupe of above with some logic L4 // this.simple+=7; ALOAD 0:
			 * this DUP GETFIELD Child$Cls.simple : int BIPUSH 7 IADD PUTFIELD
			 * Child$Cls.simple : int L5 //simple=8; ALOAD 0: this BIPUSH 8
			 * PUTFIELD Child$Cls.simple : int
			 */
			DotOperator dotop = (DotOperator) terminatal;
			// we know this must resolve to either an arrayref or a field
			terminatal = dotop.getLastElement();

			this.processDotOperator(dotop, false, false);

			this.setTerminalRefPreccededByThis(terminatal, true);
			popIfActingOnstatic = !(dotop.getPenultimate() instanceof RefName);
			firstDotOpElementIsRefThis = true;// dotop.head instanceof RefThis
												// || dotop.head instanceof
												// RefSuper;
		}
		
		
		
		if (terminatal instanceof RefName) {
			RefName assigneeRefNAme = ((RefName) terminatal);
			TypeAndLocation refNameresolvesTo = assigneeRefNAme.resolvesTo;
			boolean isClassLevel = isAtClassLevel();
			Location loc = refNameresolvesTo == null ? null : refNameresolvesTo.getLocation();
			boolean isClassField = loc != null && loc instanceof LocationClassField;
			if (isClassLevel || (isClassField && !firstDotOpElementIsRefThis)) {
				/*
				 * at class level we do: //or if it's an assignment to a local
				 * field... this.x = 6 as=> x = 6 (if in dot operator ALOAD 0
				 * would have been proceesed already) L1 LINENUMBER 14 L1 ALOAD
				 * 0: this BIPUSH 9 PUTFIELD Child$Cls.a : int
				 */
				prefixNodeWithThisreference(loc, false);

				// mv.visitVarInsn(ALOAD, 0);
			}

			boolean isNew = assignExisting.isReallyNew;

			Type expectedType = assignExisting.getTaggedType();

			boolean isLHSRef = this.isRef(expectedType);
			String name = assigneeRefNAme.bytecodename != null ? assigneeRefNAme.bytecodename: assigneeRefNAme.name;
			if (isNew && !wasRedirected) { //if its been redirected then its not new...
				//boolean previsInclInitCreator = isInclInitCreator;
				//isInclInitCreator=false;
				Type got = (Type) rhsToProcess.accept(this);
				//isInclInitCreator = previsInclInitCreator;
				TypeCheckUtils.unlockAllNestedRefs(got);
				donullcheckForUnknown(got, expectedType);

				boolean gotIsRef = this.isRef(got);

				if (!isLHSRef && !expectedType.equals(got)) {
					Utils.applyCastImplicit(bcoutputter, got, expectedType, this, true);
				}
				if ((isClassLevel || (isInclInitCreator && !assignExisting.isTempVariableAssignment && this.isAtModuleLevel()))) {
					// if we're in the isInclInitCreator, we use putstatic
					// JPT: refactor this, the 4 conditions above are a bit
					// dirty...

					if (isLHSRef) {
						Location loca = isClassLevel ? new LocationClassField(getFullModuleAndClassName(), expectedType) : new LocationStaticField(getFullModuleAndClassName(), expectedType);
						storeLocalVaraible(name, loca, expectedType, got, gotIsRef/*, null*/);
					} else {
						
						if(isClassLevel && this.currentClassDefObj.peek().isTrait) {
							//traits dont have fields
							bcoutputter.visitMethodInsn(INVOKEINTERFACE, getFullModuleAndClassName(), "traitVar$" + name, "("+expectedType.getBytecodeType()+")V", true);
						}else {
							bcoutputter.visitFieldInsn(isClassLevel ? PUTFIELD : PUTSTATIC, getFullModuleAndClassName(), name, expectedType.getBytecodeType());
						}
					}

				} else {

					boolean diffRefLevels = TypeCheckUtils.getRefLevels(got) != TypeCheckUtils.getRefLevels(expectedType);
					boolean lhsHasLessEQRefLevels = TypeCheckUtils.getRefLevels(got) <= TypeCheckUtils.getRefLevels(expectedType);

					boolean extraDup = isLHSRef && !expectedType.equals(got);

					if (isLHSRef && gotIsRef) {
						if (lhsHasLessEQRefLevels) {
							createNewLocalVar(name, diffRefLevels, expectedType, true, gotIsRef, true, -1);
						}
					} else {
						createNewLocalVar(name, diffRefLevels, expectedType, true, false, extraDup, -1);
					}

					if (extraDup) {
						if (lhsHasLessEQRefLevels) {
							Utils.genericSswap(bcoutputter, expectedType, got);
						}

						Utils.applyCastImplicit(bcoutputter, got, expectedType, this, true);

						if (!lhsHasLessEQRefLevels) {
							createNewLocalVar(name, false, expectedType, true, gotIsRef, true, -1);
						}
					}
				}
			} else {
				if(loc != null && loc instanceof LocationStaticField && firstDotOpElementIsRefThis && popIfActingOnstatic) {
					bcoutputter.visitInsn(POP);//clean up stack as statuc call therefore we dont need whats on the lhs
				}
				
				if (eqStyle.isEquals()) {
					Type tryingToPassType = null;
					if (rhsToProcess != null) {
						boolean rhsToRef = isLHSRef && rhsResolvesToRefType(assignExisting);
						if (rhsToRef) {// no other case valid
							// load the existing ref
							Type loadedType = loadLocalVar(name, refNameresolvesTo);
							TypeCheckUtils.unlockAllNestedRefs(loadedType);
							// now need to create local var
							Type toType = rhsToProcess.getTaggedType();
							loadedType = Utils.applyCastImplicit(bcoutputter, loadedType, toType, this);

							int slot = createNewLocalVar(name + "$tempVirloc", false, loadedType, true, true, true, -1);
							assignExisting.localVarToCopyRefInto = slot;// for
																		// use
																		// in
																		// asyncblock
						}
						
						//boolean previsInclInitCreator = isInclInitCreator;
						//isInclInitCreator=false;
						tryingToPassType = (Type) rhsToProcess.accept(this);
						//isInclInitCreator = previsInclInitCreator;
						
						TypeCheckUtils.unlockAllNestedRefs(tryingToPassType);
						
						if(TypeCheckUtils.isNotNullable(expectedType)) {
							donullcheckForUnknown(tryingToPassType, null);
						}
						
						if (rhsToRef) {
							return null;// we're done here dont need to
										// explicitly store it
						}
					}

					// fail on : module field correct behavour <-figure out what
					// this should be

					// unlock , and rhs ref hmmmm

					// e.g. asd := ref
					boolean shouldOverriteRef = assignExisting.refCnt > 0;// ||
																			// TypeCheckUtils.hasRefLevelsAndIsLocked(tryingToPassType);//
																			// ||
																			// isLHSRef;
					// TODO: asd ::= ref

					if (!shouldOverriteRef) {
						if (TypeCheckUtils.hasRefLevelsAndNotLocked(tryingToPassType)) {
							tryingToPassType = Utils.unref(bcoutputter, tryingToPassType, this);
						}
						
						storeLocalVaraible(name, refNameresolvesTo, tryingToPassType, TypeCheckUtils.hasRefLevelsAndIsLocked(tryingToPassType)/*, assigneeRefNAme.isMapGetter*/);
						
					} else {// if(assignExisting.refCnt >=
							// TypeCheckUtils.getRefLevels(tryingToPassType)){
							// z := "newValue", rhs needs to be a ref - so turn
							// it into a ref and REPLACE the thing on the lhs
						int setTo = assignExisting.refCnt;
						int rhsRefLevels = TypeCheckUtils.getRefLevels(tryingToPassType);
						
						Type lhsType;
												
						lhsType = refNameresolvesTo.getType();
						
						int lhsResolvesRefLevels = TypeCheckUtils.getRefLevels(lhsType);

						tryingToPassType = convertToRefLevels(setTo, rhsRefLevels, tryingToPassType);

						boolean overwriteLHSRef = lhsResolvesRefLevels >= setTo;

						storeLocalVaraible(name, refNameresolvesTo, tryingToPassType, overwriteLHSRef/*, assigneeRefNAme.isMapGetter*/);
					}
				} else {
					TypeAndLocation tal = assigneeRefNAme.resolvesTo;
					Type assigType = tal == null ? null : tal.getType();

					boolean lhsStringConcatOperation = TypeCheckUtils.isString(assigType) && eqStyle == AssignStyleEnum.PLUS_EQUALS;

					Type loadedType;

					if (lhsStringConcatOperation) {
						if (isClassField) {
							// if(!dupAlready){mv.visitInsn(DUP);}
							bcoutputter.visitInsn(DUP);

							loadedType = loadLocalVar(name, tal);
							bcoutputter.visitTypeInsn(NEW, "java/lang/StringBuilder");
							bcoutputter.visitInsn(DUP_X1);
							bcoutputter.visitInsn(SWAP);
						} else {
							bcoutputter.visitTypeInsn(NEW, "java/lang/StringBuilder");
							bcoutputter.visitInsn(DUP);
							loadedType = loadLocalVar(name, tal);
						}

						if (TypeCheckUtils.hasRefLevels(loadedType)) {
							loadedType = Utils.unref(bcoutputter, loadedType, this);
						}

						bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
						bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
					} else {
						if (isClassField) {
							// if(!dupAlready){mv.visitInsn(DUP);}//since we
							// read the classfield, then write back to it dup
							// this pointer
							bcoutputter.visitInsn(DUP);// since we read the classfield, then write back to it dup this pointer
						}
						loadedType = loadLocalVar(name, tal);
						TypeCheckUtils.unlockAllNestedRefs(loadedType);

						if (TypeCheckUtils.hasRefLevels(loadedType)) {
							loadedType = Utils.unref(bcoutputter, loadedType, this);
						}
					}

					Type typeAsPrim = loadedType;
					boolean requiresBoxing = false;
					Label forBoolOpsIfFalse = null;
					if (eqStyle == AssignStyleEnum.POW_EQUALS)// cannot be string concat
					{// special case as power operator only works on doubles
						Utils.applyCastImplicit(bcoutputter, loadedType, RefDoublePrim, this);
						typeAsPrim = null;
					} 
					else {
						if (eqStyle == AssignStyleEnum.AND_EQUALS){
							Utils.applyCastImplicit(bcoutputter, loadedType, RefBoolPrim, this);
							typeAsPrim = RefBoolPrim;
							forBoolOpsIfFalse = Utils.applyInfixAndOrPart1(bcoutputter, true);
						}
						else if (eqStyle == AssignStyleEnum.OR_EQUALS){
							Utils.applyCastImplicit(bcoutputter, loadedType, RefBoolPrim, this);
							typeAsPrim = RefBoolPrim;
							forBoolOpsIfFalse = Utils.applyInfixAndOrPart1(bcoutputter, false);
						}
						else if (!lhsStringConcatOperation) {
							typeAsPrim = Utils.unbox(bcoutputter, loadedType, this);
						}
						requiresBoxing = !loadedType.equals(typeAsPrim);
					}

					Type tryingToPassType = null;
					if(forBoolOpsIfFalse != null){//its a and= or or= expression
						if (rhsToProcess != null) {
						//	boolean previsInclInitCreator = isInclInitCreator;
							//isInclInitCreator=false;
							convertToBoolean((Type)rhsToProcess.accept(this), forBoolOpsIfFalse);
							//isInclInitCreator = previsInclInitCreator;
						}
					}
					else{
						if (rhsToProcess != null) {
							//boolean previsInclInitCreator = isInclInitCreator;
							//isInclInitCreator=false;
							tryingToPassType = (Type) rhsToProcess.accept(this);
							//isInclInitCreator = previsInclInitCreator;
							
							TypeCheckUtils.unlockAllNestedRefs(tryingToPassType);
							donullcheckForUnknown(tryingToPassType, null);
						}

						if (!lhsStringConcatOperation) {
							if (eqStyle == AssignStyleEnum.POW_EQUALS) {
								Utils.applyCastImplicit(bcoutputter, tryingToPassType, new PrimativeType(PrimativeTypeEnum.DOUBLE), this);
							} else if (!tryingToPassType.equals(typeAsPrim)) {
								Utils.applyCastImplicit(bcoutputter, tryingToPassType, typeAsPrim, this);
							}
						}
					}
					

					if (eqStyle == AssignStyleEnum.DIV_EQUALS) {// /=
						Utils.applyMuler(bcoutputter, (PrimativeType) typeAsPrim, MulerExprEnum.DIV);
					} else if (eqStyle == AssignStyleEnum.MINUS_EQUALS) {// -=
						Utils.applyPlusMinusPrim(this.bcoutputter, false, (PrimativeType) typeAsPrim);
					} else if (eqStyle == AssignStyleEnum.MUL_EQUALS) {// *=
						Utils.applyMuler(bcoutputter, (PrimativeType) typeAsPrim, MulerExprEnum.MUL);
						
					} else if (eqStyle == AssignStyleEnum.RSH) {
						Utils.applyShift(bcoutputter, (PrimativeType) typeAsPrim, ShiftOperatorEnum.RS);
					} else if (eqStyle == AssignStyleEnum.LSH) {
						Utils.applyShift(bcoutputter, (PrimativeType) typeAsPrim, ShiftOperatorEnum.LS);
					} else if (eqStyle == AssignStyleEnum.RHSU) {
						Utils.applyShift(bcoutputter, (PrimativeType) typeAsPrim, ShiftOperatorEnum.URS);
						

					} else if (eqStyle == AssignStyleEnum.BAND) {
						Utils.applyBitwise(bcoutputter, (PrimativeType) typeAsPrim, BitwiseOperationEnum.AND);
					} else if (eqStyle == AssignStyleEnum.BOR) {
						Utils.applyBitwise(bcoutputter, (PrimativeType) typeAsPrim, BitwiseOperationEnum.OR);
					} else if (eqStyle == AssignStyleEnum.BXOR) {
						Utils.applyBitwise(bcoutputter, (PrimativeType) typeAsPrim, BitwiseOperationEnum.XOR);
						
					} else if (eqStyle == AssignStyleEnum.MOD_EQUALS) {// %=
						Utils.applyMuler(bcoutputter, (PrimativeType) typeAsPrim, MulerExprEnum.MOD);
					} else if (eqStyle == AssignStyleEnum.AND_EQUALS) {// %=
						Utils.applyInfixAndOrPart2(bcoutputter, true, forBoolOpsIfFalse);
					} else if (eqStyle == AssignStyleEnum.OR_EQUALS) {// %=
						Utils.applyInfixAndOrPart2(bcoutputter, false, forBoolOpsIfFalse);
					} else if (eqStyle == AssignStyleEnum.PLUS_EQUALS) {//
						if (lhsStringConcatOperation) {
							if (TypeCheckUtils.hasRefLevels(tryingToPassType)) {
								tryingToPassType = Utils.unref(bcoutputter, tryingToPassType, this);
							}

							StringBuffHelper.append(this, tryingToPassType);
							bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
						} else {
							Utils.applyPlusMinusPrim(this.bcoutputter, true, (PrimativeType) typeAsPrim);
						}
					} else if (eqStyle == AssignStyleEnum.POW_EQUALS) {// **=
						bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "pow", "(DD)D");
						Utils.applyCastImplicit(bcoutputter, RefDoublePrim, loadedType, this);
					}

					if (requiresBoxing) {
						Utils.box(bcoutputter, (PrimativeType) typeAsPrim);
					}

					storeLocalVaraible(name, refNameresolvesTo, loadedType/*, null*/);
				}
			}
		} else if (terminatal instanceof ArrayRef) {
			ArrayRef lhsAssigneeArrayRef = (ArrayRef) terminatal;
			assignArrayRef(lhsAssigneeArrayRef, rhsToProcess, assignExisting.eq, assignExisting);
		} else {
			throw new RuntimeException("Unexpected compilation error. Unexpected existing assignment operation");
		}
		return null;
	}

	private boolean shouldLineBeProcessed(Line l, boolean isClassLevel) {
		if (isClassLevel) {
			return (l instanceof ClassDef || l instanceof EnumDef || l instanceof FuncDefI || l instanceof AnnotationDef);
		} else {
			if (isInclInitCreator) {
				if(this.currentClassDef.isEmpty() && l instanceof ClassDef) {
					return ((ClassDef)l).isLocalClass;//MHA: to force bytecode gennerator to output local class info when defined inside module
				}
				
				if(l instanceof LambdaDef) {
					return true;
				}
				
				if(l instanceof FuncDef) {
					FuncDef fdi = (FuncDef)l;
					if(fdi.funcName.startsWith("NIF$")) {//top level nested inner function
						return true;
					}
				}
				
				return !(l instanceof ClassDef || l instanceof FuncDefI);
			} else {
				if(isFieldBlockRhs){
					return true;
				}else if (this.isAtModuleLevel()) {
					return (l instanceof ClassDef || l instanceof FuncDefI);
				}else {
					return true;
				}

			}
		}

	}

	private Stack<Boolean> isLastLineInBlock = new Stack<Boolean>();
	private Stack<Boolean> lastLineInBlockShouldPresev = new Stack<Boolean>();

	private Type acceptLines(Block block, boolean firstPass) {
		DefaultConstuctorFieldInitlizator defConstFieldInit = block.defFeildInit;
		// we may have to specially invoke the fields ifthis is/ a/ constructor block
		// super, fields then other stuff
		boolean isclass = this.currentScopeFrame.isClass();
		boolean isFirstLine = true;

		if(block.isModuleLevel){
			this.outputLocalClasses = false;
		}
		
	/*	if(block.isClassFieldBlock){
			this.level++;
		}*/
		Type got = null;
		
		ArrayList<LineHolder> lines = block.lines;
		int sz = lines.size();
		isLastLineInBlock.push(false);
		// boolean isFirst = true;
		for (int n = 0; n < sz; n++) {
			LineHolder lh = lines.get(n);
			Line l = lh.l;
			if (shouldLineBeProcessed(l, isclass)) {
				boolean isLast = n == sz - 1;
				if (isLast) {
					isLastLineInBlock.pop();
					isLastLineInBlock.push(true);
				}

				Label toVisit = l.getLabelOnEntry();
				if (null != toVisit) {// to support early termination of try
										// catch blocks
					// System.err.println("early access label: " + toVisit +
					// " for: " + l.getClass().getName());
					bcoutputter.pushNextLabel(toVisit);
				}

				/*
				 * if(isFirst){ isFirst=false; if(null !=
				 * block.popTypeOnEntry){//add pop on entry e.g. for a finally
				 * block on something that returns something
				 * 
				 * } }
				 */

				if (bcoutputter != null) {
					bcoutputter.visitNewLine(l.getLine());
				}

				lastStatementTouched = l;

				if (isLast) {
					lastLineInBlockShouldPresev.push(block.getShouldBePresevedOnStack());
				}

				if(block.isModuleLevel && l instanceof Block) {
					((Block)l).isMethodBlock = true;
				}
				
				got = (Type)l.accept(this);

				if (isLast) {
					lastLineInBlockShouldPresev.pop();
				}

				if (isFirstLine && defConstFieldInit != null) {
					visitDefConsFieldInit(defConstFieldInit);
				}
			}
			isFirstLine = false;
		}
		
		/*if(block.isClassFieldBlock){
			this.level--;
		}*/
		
		isLastLineInBlock.pop();
		
		if(!firstPass && block.isModuleLevel && !localClassDefs.isEmpty()){
			TheScopeFrame curscopeframee = this.currentScopeFrame;
			this.outputLocalClasses = true;
			
			while(!localClassDefs.isEmpty()){
				Thruple<ClassDef, TheScopeFrame, Integer> localcd = localClassDefs.remove();
				this.currentScopeFrame = localcd.getB();
				int curLevel = this.level;
				this.level = localcd.getC();
				localcd.getA().accept(this);
				this.level = curLevel;
				
			}
			
			/*for(Thruple<ClassDef, TheScopeFrame, Integer> localcd : localClassDefs){
				this.currentScopeFrame = localcd.getB();
				int curLevel = this.level;
				this.level = localcd.getC();
				localcd.getA().accept(this);
				this.level = curLevel;
			}*/
			
			localClassDefs.clear();
			this.currentScopeFrame = curscopeframee;
		}
		
		return got;
	}
	
	private void visitDefConsFieldInit(DefaultConstuctorFieldInitlizator defConstFieldInit){
		//setup 'env'
		isInclInitCreator = true;
		inspecialFieldInitConstrutor = true;
		// Stack<HashMap<String, Tuple<Type, Integer>>> lvsBefore =
		// localvarStack;
		// Stack<Integer> lvssBefore = localvarStackSize;
		// localvarStack = new Stack<HashMap<String, Tuple<Type,
		// Integer>>>();
		localvarStack.push(new HashMap<String, Pair<Type, Integer>>()); // add extra layer for locally invoked vars in initial setter
		// localvarStackSize = new Stack<Integer>();
		// localvarStackSize.push(1);

		defConstFieldInit.initFields(this.currentClassDefObj.peek());

		localvarStack.pop();

		// localvarStack = lvsBefore;
		// localvarStackSize = lvssBefore;
		isInclInitCreator = false;
		inspecialFieldInitConstrutor = false;
	}

	private boolean createClinit(Block block) {
		LineHolder lh = block.startItr();
		while (lh != null) {
			Line l = lh.l;
			if (!((isInclInitCreator && (l instanceof ClassDef || l instanceof FuncDefI)))) { return true;}
			/*if (l instanceof AssignNew) {
				if(!skipAssignNewInInit((AssignNew)l)){
					return true;
				}
			}
			else if (l instanceof AssignExisting){
				return true;
			}*/
			lh = block.getNext();
		}
		return false;
	}

	public boolean isInclInitCreator = false;
	private boolean isFieldBlockRhs = false;

	private int getStringConcatIndex(Additive findIn) {// check to see
																	// if there
																	// is string
																	// concatination
		Expression head = findIn.head;
		Type lhs = head.getTaggedType();

		assert !findIn.elements.isEmpty();
		// if(!findIn.elements.isEmpty())
		// {
		if (TypeCheckUtils.isString(lhs)) {
			return 1;
		} else {
			int stringConcatAtOp = 0;
			for (AddMinusExpressionElement i : findIn.elements) {
				stringConcatAtOp++;
				if (TypeCheckUtils.isString(i.exp.getTaggedType())) {
					return stringConcatAtOp;
				}
			}
		}
		// }

		return 0;
	}

	private int tempVarCnt = 0;

	public String getTempVarName() {
		return "$tvar" + tempVarCnt++;
	}

	public boolean hasLocalVar(String name) {
		for (int n = localvarStack.size() - 1; n >= 0; n--) {
			HashMap<String, Pair<Type, Integer>> level = localvarStack.get(n);
			if (level.containsKey(name)) {
				return true;
			}
		}
		return false;
	}

	public Pair<Type, Integer> getLocalVar(String name) {// ret name and number
		
		for (int n = localvarStack.size() - 1; n >= 0; n--) {
			HashMap<String, Pair<Type, Integer>> level = localvarStack.get(n);
			if (level.containsKey(name)) {
				return level.get(name);
			}
		}
		// return null;
		throw new RuntimeException("Local var not found: " + name);
	}

	private final static PrimativeType RefDoublePrim = new PrimativeType(PrimativeTypeEnum.DOUBLE);
	private final static PrimativeType RefBoolPrim = new PrimativeType(PrimativeTypeEnum.BOOLEAN);
	private static final NamedType Const_Object = new NamedType(new ClassDefJava(Object.class));

	@Override
	public Object visit(AddMinusExpressionElement addMinusExpressionElement) {
		addMinusExpressionElement.exp.accept(this);
		return null;// null is ok
	}

	@Override
	public Object visit(AsyncRefRef asyncRefRef) {
		//Type got = ((Type) asyncRefRef.b.accept(this));
		Type got = (Type)((Type) asyncRefRef.b.accept(this)).copy();
		TypeCheckUtils.unlockAllNestedRefs(got);
		Type ret = asyncRefRef.getTaggedType();

		Utils.applyCastImplicit(bcoutputter, got, ret, this);

		((NamedType) ret).setLockedAsRef(true);

		return ret;
	}

	@Override
	public Object visit(MapDefElement mapDefElement) {
		Type keyType = (Type) mapDefElement.getKey(this).accept(this);
		Utils.applyCastImplicit(bcoutputter, keyType, mapDefElement.keyType, this);
		// Utils.box(mv, keyType);

		Type valueType = (Type) mapDefElement.getValue(this).accept(this);
		Utils.applyCastImplicit(bcoutputter, valueType, mapDefElement.valType, this);
		// Utils.box(mv, valueType);

		bcoutputter.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
		return null;
	}
	
	@Override
	public Object visit(MapDefaultElement mapDefElement) {
		//Type keyType = (Type) mapDefElement.key.accept(this);
		//Utils.applyCastImplicit(mv, keyType, mapDefElement.keyType, this);
		// Utils.box(mv, keyType);
		
		//Type valueType = (Type) mapDefElement.value.accept(this);
		//Utils.applyCastImplicit(mv, valueType, mapDefElement.valType, this);
		// Utils.box(mv, valueType);
		
		//mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
		return mapDefElement.value.accept(this);
	}

	/*
	 * public void acceptMapMapping(MapDefElement mapDefElement, Type tokeyType,
	 * Type toValueType) { Type keyType = (Type)mapDefElement.key.accept(this);
	 * Utils.applyCastImplicit(mv, keyType, tokeyType, this); //Utils.box(mv,
	 * keyType);
	 * 
	 * Type valueType = (Type)mapDefElement.value.accept(this);
	 * Utils.applyCastImplicit(mv, valueType, toValueType, this);
	 * //Utils.box(mv, valueType);
	 * 
	 * mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put",
	 * "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"); }
	 */

	@Override
	public Object visit(MapDef mapDef) {

		if(mapDef.defaultMapElement != null){
			bcoutputter.visitTypeInsn(NEW, "com/concurnas/lang/DefaultMap");
			bcoutputter.visitInsn(DUP);
			
			String mdeClass = mapDef.defaultMapElement.mdeClass;
			if(null != mdeClass){
				bcoutputter.visitTypeInsn(NEW, mdeClass);
				bcoutputter.visitInsn(DUP);
			}
			
			mapDef.defaultMapElement.accept(this);
			
			if(null != mdeClass){
				bcoutputter.visitMethodInsn(INVOKESPECIAL, mdeClass, "<init>", "(L"+mapDef.defaultMapElement.origClass+";)V");
			}
			
			bcoutputter.visitMethodInsn(INVOKESPECIAL, "com/concurnas/lang/DefaultMap", "<init>", "(Lcom/concurnas/bootstrap/lang/Lambda$Function1;)V");
		}
		else{
			bcoutputter.visitTypeInsn(NEW, "java/util/HashMap");
			bcoutputter.visitInsn(DUP);
			bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V");
		}

		for (int n = 0; n < mapDef.elements.size(); n++) {
			if (n != mapDef.elements.size() - 1 || mapDef.getShouldBePresevedOnStack()) {
				// dup for all but the last instance since we dont need the ref,
				// unless it should be preserved
				bcoutputter.visitInsn(DUP);
			}
			
			IsAMapElement ea = mapDef.elements.get(n);
			
			if(ea instanceof MapDefElement){
				MapDefElement e = (MapDefElement)ea;
				if (bcoutputter != null) {//having to do this explicitly is a bit nasty
					bcoutputter.visitNewLine(e.getLine());
				}

				// acceptMapMapping(mapDef.elements.get(n), mapDef.keyType,
				// mapDef.valueType);
				e.accept(this);
			}
			else{
				/*MapDefaultElement e = (MapDefaultElement)ea;
				if (mv != null) {//having to do this explicitly is a bit nasty
					mv.visitNewLine(e.getLine());
				}

				// acceptMapMapping(mapDef.elements.get(n), mapDef.keyType,
				// mapDef.valueType);
				e.accept(this);*/
			}
			
			bcoutputter.visitInsn(POP);
		}
		return mapDef.getTaggedType();
	}

	@Override
	public Object visit(FuncType funcType) {
		return null; // null is ok
	}

	@Override
	public Object visit(FuncParams funcParams) {
		return null; // null is ok
	}

	@Override
	public Object visit(FuncParam funcParam) {
		return null; // null is ok
	}

	@Override
	public Object visit(ClassDefArgs classDefArgs) {
		return null; // null is ok
	}

	@Override
	public Object visit(ClassDefArg classDefArg) {
		return null; // null is ok
	}

	private boolean shouldInitDefaultNoargconstructor(HashSet<FuncType> chocies) {
		if (chocies.size() == 1) {
			FuncType ft = chocies.iterator().next();
			if ((ft.getInputs() == null || ft.getInputs().isEmpty()) && ft.isAutoGen) {// if
																						// no
																						// arg
																						// and
																						// not
																						// user
																						// created
				return true;
			}
			return false;
		}
		return false;
	}

	private Stack<String> currentClassDef = new Stack<String>();
	public Stack<ClassDef> currentClassDefObj = new Stack<ClassDef>();

	private Queue<Thruple<ClassDef, TheScopeFrame, Integer>> localClassDefs = new LinkedBlockingQueue<Thruple<ClassDef, TheScopeFrame, Integer>>();
	private boolean outputLocalClasses = false;

	private static class NestedLocalClassFinderJustClassDef extends AbstractErrorRaiseVisitor implements Unskippable{
		public LinkedList<ClassDef> localClasses = new LinkedList<ClassDef>();
		protected NestedLocalClassFinderJustClassDef( ) {
			super("");
		}
		
		@Override
		public Object visit(ClassDef classDef) {
			if(classDef.isLocalClass){
				localClasses.add(classDef);
			}
			return super.visit(classDef);
		}
	}
	
	
	private class NestedLocalClassFinder extends AbstractErrorRaiseVisitor implements Unskippable{
		//JPT: level stuff is nasty!
		protected NestedLocalClassFinder( ) {
			super("");
		}
		
		@Override
		public Object visit(ClassDef classDef) {
			if(classDef.isLocalClass){
				level++;
				localClassDefs.add(new Thruple<>(classDef, currentScopeFrame.searchForChild(classDef.classBlock).getParent(), level));
				level--;
			}
			return super.visit(classDef);
		}

		@Override
		public Object visit(Block block) {
			if(block.canNestModuleLevelFuncDefs) {
				level++;
				super.visit(block);
				level--;
			}else {
				super.visit(block);
			}
			return null;
		}
		
		@Override
		public Object visit(ConstructorDef funcDef) {
			level++;
			super.visit(funcDef);
			level--;
			return null;
		}
		
		
		@Override
		public Object visit(AsyncBodyBlock asyncBodyBlock) {
			level++;
			super.visit(asyncBodyBlock);
			level--;
			return null;
		}

		@Override
		public Object visit(LambdaDef lambdaDef) {
			level++;
			super.visit(lambdaDef);
			level--;
			return null;
		}
		
		@Override
		public Object visit(AsyncBlock asyncBlock) {
			level++;
			super.visit(asyncBlock);
			level--;
			return null;
		}

		@Override
		public Object visit(FuncDef funcDef) {
			level++;
			super.visit(funcDef);
			level--;
			return null;
		}
	}
	
	private boolean capturedLocalClassForLaterAlready(ClassDef can){
		if(!localClassDefs.isEmpty()){
			for(Thruple<ClassDef, TheScopeFrame, Integer> item : localClassDefs){
				if(item.getA().equals(can)){
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public Object visit(ClassDef classDef) {
		if(!outputLocalClasses && classDef.isLocalClass){
			if(!capturedLocalClassForLaterAlready(classDef)){//TODO: possible to step in here? As should all be added on top level call
				localClassDefs.add(new Thruple<>(classDef, this.currentScopeFrame, level));
				NestedLocalClassFinder nlcf = new NestedLocalClassFinder();
				nlcf.visit(classDef.classBlock);
			}
			
			return null;
		}
		
		boolean previsInclInitCreator = this.isInclInitCreator;
		this.isInclInitCreator = false;
		
		String className = classDef.className;
		
		TreeSet<String> ifaces=new TreeSet<String>();
		
		if(TypeCheckUtils.isTypedActor(this.errorRaisableSupressionFromSatc, classDef.getSuperAsNamedType(69, 69))){
			//TODO: remove when DefaultActorIsGennerated at runtime
			//bytecodeSandbox$MyClass$$ActorIterface
			Type opOn = (TypeCheckUtils.extractTypedActorType(this.errorRaisableSupressionFromSatc, classDef.getSuperAsNamedType(69, 69)));
			if(null == opOn){
				opOn = classDef.superClassGenricList.get(0);
			}
			
			String hh = opOn.getBytecodeType();
			hh = "x" + hh.substring(1, hh.length() - 1) +"$$ActorIterface";
			ifaces.add(hh);
			
			if(null != classDef.typedActorOnImplicitIfaces) {
				
				for(String inst : classDef.typedActorOnImplicitIfaces.stream().map(a -> a.getBytecodeType()).sorted().collect(Collectors.toList())) {
					ifaces.add(inst.substring(1, inst.length()-1));
				}
			}
		}
		
		if(!classDef.traits.isEmpty()){
			for(ImpliInstance ifaceHolder : classDef.traits){
				if(null != ifaceHolder.resolvedIface) {
					String hh = ifaceHolder.resolvedIface.javaClassName();
					ifaces.add(hh.substring(1, hh.length() - 1));
				}
				
			}
		}
		
		String genericSignature = null;
		{
			// "<Taa:Ljava/lang/Object;Moo:Ljava/lang/Object;>Ljava/lang/Object;"
			StringBuilder sb = new StringBuilder();
	
			if(null != classDef.classGenricList && !classDef.classGenricList.isEmpty()){
				sb.append('<');
				
				for (GenericType ll : classDef.classGenricList) {
					sb.append(ll.name);
					sb.append(':');
					sb.append("L" + ll.upperBound.getSetClassDef().bcFullName() + ";");// JPT: when doing proper upper bounds this bcFullName will fail
				}
	
				sb.append('>');
			}
			NamedType sup = classDef.getSuperAsNamedType(0, 0);
			//sup.setOrigonalGenericTypeUpperBound(null);
			sb.append(sup.getGenericBytecodeType());
	
			
			classDef.getTraitsAsNamedType(0, 0).stream().filter(a -> a != null).forEach(a -> sb.append(a.getGenericBytecodeType()));
			
			genericSignature = sb.toString();
		}
		
		String fullClassName = enterNewClass(classDef, genericSignature, classDef.getSuperclass().bcFullName(), classDef.getIsAbstract(), classDef.isFinal, ifaces.isEmpty()?null:ifaces.toArray(new String[0]), 0, null, classDef.isTrait);
		
		if(classDef.isLocalClass){//add parent class
			currentClassDef.add( classDef.bcFullName().substring(this.packageAndClassName.length() + 1) );//add parent class
		}else {
			currentClassDef.add(className);
		}
		
		currentClassDefObj.add(classDef);

		TheScopeFrame prev = this.currentScopeFrame;
		this.currentScopeFrame = this.currentScopeFrame.getChild(classDef.classBlock);

		if(!classDef.isTrait) {
			HashMap<String, Type> fieldNameToType = createAllModuleFields();
			createDefValueFieldInit(classDef, fieldNameToType, fullClassName);
			
			if (shouldInitDefaultNoargconstructor(this.currentScopeFrame.getConstructor(null))) {
				// no noarg constuctor deinfed, so we must define one
				this.enterMethod("<init>", null, Const_PRIM_VOID, false, AccessModifier.PUBLIC, 0, false);

				bcoutputter.visitVarInsn(ALOAD, 0);
				String supClsConsName = classDef.getSuperclass().bcFullName();
				bcoutputter.visitMethodInsn(INVOKESPECIAL, supClsConsName, "<init>", "()V");

				// do initial init on fields
				/*
				 * L1 LINENUMBER 14 L1 ALOAD 0: this BIPUSH 9 PUTFIELD Child$Cls.a :
				 * int
				 */
				DefaultConstuctorFieldInitlizator fieldInit = new DefaultConstuctorFieldInitlizator(this);
				fieldInit.initFields(classDef);

				bcoutputter.visitInsn(RETURN);
				this.exitMethod();
			}
		}else {
			ArrayList<Sixple<String, Type, Boolean, AccessModifier, Boolean, String>> dfs = classDef.getAllFieldsDefined();
			
			if(dfs.stream().anyMatch(a -> a.getC())) {//gennerate init
				gennerateTraitInit(classDef);
			}
			
			ArrayList<VarAtScopeLevel> fields = currentScopeFrame.getAllVariablesAtScopeLevel(false, true, false, false);
			if(!fields.isEmpty()) {
				addTraitVarsNeedingStubs(fields);
			}
		}

		this.currentScopeFrame = prev;

		if (classDef.hasNestedChildren && classDef.getParentNestor() != null)// sandwiched
		{// children will need an accessor
			createNestorParentAccessor(classDef, classDef.getLine());
		}
		boolean prevoutputLocalClasses = outputLocalClasses;
		outputLocalClasses=false;
		
		boolean addedStackSizeEle = false;
		if(null != localvarStackSize && !localvarStackSize.isEmpty()) {
			localvarStackSize.push(0);
			addedStackSizeEle=true;
		}
		
		if(classDef.isLocalClass){
			level++;
			classDef.classBlock.accept(this);
			level--;
		}else{
			classDef.classBlock.accept(this);
		}
		
		if(addedStackSizeEle) {
			localvarStackSize.pop();
		}
		
		
		outputLocalClasses = prevoutputLocalClasses;
		
		
		if(!classDef.bridgeMethodsToAdd.isEmpty()) {
			//add bridge methods
			addBridgeMethods(fullClassName, classDef.bridgeMethodsToAdd);
		}
		
		if(!classDef.bridgeMethodsForNonpublicClassSuperMethods.isEmpty()) {
			addBridgeToNonPublicSuperClassMethods(classDef.bridgeMethodsForNonpublicClassSuperMethods);
		}
		
		if(classDef.addMethodsToTraits != null) {
			addTraitDirectingMethods(classDef.addMethodsToTraits);
		}
		
		if(!classDef.traitSuperRefs.isEmpty()) {//stubs which concrete class needs to implement
			addTraitSuperRefStubs(classDef.traitSuperRefs);
		}
		
		if(!classDef.traitSuperRefsImpls.isEmpty()) {//concrete class needs to populate trait super references
			addTraitSuperRefStubsImpls(classDef.traitSuperRefsImpls);
		}
		
		if(null != classDef.traitVarsNeedingFieldDef) {
			addtraitVarsNeedingFieldDef(classDef.traitVarsNeedingFieldDef);
		}
		
		if(null != classDef.traitVarsNeedingImpl) {
			addTraitVarsNeedingStubImpl(classDef.bcFullName(), classDef.traitVarsNeedingImpl);
		}
		
		exitClass();
		currentClassDef.pop();
		currentClassDefObj.pop();

		this.isInclInitCreator = previsInclInitCreator;
		
		return null;
	}
	
	
	
	private void gennerateTraitInit(ClassDef classDef) {
		ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>  inputs = new ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>();
		inputs.add(0, new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(new NamedType(classDef), "this", null, false, false, false));
		
		enterMethod("$init$traitM", inputs, Const_PRIM_VOID, false, AccessModifier.PUBLIC, ACC_STATIC + ACC_SYNTHETIC, false);
		
		currentScopeFrame.isClass();
		DefaultConstuctorFieldInitlizator fieldInit = new DefaultConstuctorFieldInitlizator(this);
		fieldInit.initFieldsNoScopeFrameAdjustment(classDef);
		//fieldInit.visit(classDef);

		bcoutputter.visitInsn(RETURN);
		exitMethod();
	}

	private void addtraitVarsNeedingFieldDef(ArrayList<Pair<String, Type>> traitVarsNeedingFieldDef) {
		// TODO Auto-generated method stub
		for(Pair<String, Type> item : traitVarsNeedingFieldDef) {
			Type tt = item.getB();
			
			FieldVisitor fv = cw.visitField(ACC_PUBLIC, item.getA(), tt.getBytecodeType(), tt.getGenericBytecodeType(), null);
			fv.visitEnd();
		}
	}

	public void addTraitVarsNeedingStubImpl(String classBCName, List<Thruple<String, Type, Type>> fields) {
		for(Thruple<String, Type, Type> instance : fields) {
			//getter
			String origName = instance.getA();
			String fname = "traitVar$" + origName;
			Type expectedType = instance.getB();
			
			boolean hasGenExpect=false;
			Type expectedTypeForSig = expectedType;
			if(expectedType.getOrigonalGenericTypeUpperBoundRaw() != null) {
				expectedTypeForSig = expectedType.getOrigonalGenericTypeUpperBoundRaw();
				hasGenExpect = true;
			}
			
			Type definedType = instance.getC();
			String definedtBCType = definedType.getBytecodeType();
			
			enterMethod(fname, new ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>(), expectedTypeForSig, false, false, false, AccessModifier.PUBLIC, ACC_SYNTHETIC, null, false, null);
			
			this.bcoutputter.visitVarInsn(ALOAD, 0);
			bcoutputter.visitFieldInsn(GETFIELD, classBCName, origName, definedtBCType);
			Utils.applyCastExplicit(bcoutputter, definedType, expectedType, this);
			if(hasGenExpect) {
				Utils.applyCastExplicit(bcoutputter, expectedType, expectedTypeForSig, this);
			}
			
			this.bcoutputter.visitInsn(Utils.returnTypeToOpcode(expectedTypeForSig));
			exitMethod();
			
			//setter
			ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>  params =  new ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>();
			params.add(new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(expectedTypeForSig, "value", null, false, false, false));
			
			enterMethod(fname, params, ScopeAndTypeChecker.const_void, false, false, false, AccessModifier.PUBLIC, ACC_SYNTHETIC, null, false, null);

			this.bcoutputter.visitVarInsn(ALOAD, 0);
			Utils.applyLoad(this.bcoutputter, expectedTypeForSig, 1);

			Utils.applyCastExplicit(bcoutputter, expectedTypeForSig, definedType, this);
			bcoutputter.visitFieldInsn(PUTFIELD, classBCName, origName, definedtBCType);
			bcoutputter.visitInsn(RETURN);
			
			exitMethod();
		}
	}
	
	private void addTraitVarsNeedingStubs(ArrayList<VarAtScopeLevel> fields) {
		for(VarAtScopeLevel instance : fields) {
			//getter
			String fname = "traitVar$" + instance.getVarName();
			Type retType = instance.getType();
			
			enterMethod(fname, new ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>(), retType, true, false, false, AccessModifier.PUBLIC, ACC_SYNTHETIC, null, false, null);
			exitMethod();
			
			//setter
			
			ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>  params =  new ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>();
			params.add(new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(retType, "value", null, false, false, false));
			
			enterMethod(fname, params, ScopeAndTypeChecker.const_void, true, false, false, AccessModifier.PUBLIC, ACC_SYNTHETIC, null, false, null);
			exitMethod();
		}
	}

	private void addBridgeToNonPublicSuperClassMethods(ArrayList<FuncDef> bridgeMethodsToAdd) {
		for(FuncDef origonal : bridgeMethodsToAdd) {
			/*{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "getThing", "()V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitLineNumber(1, l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "com/one/EdAndHenry$Master", "getThing", "()V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
			} */
			ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>  origParams =  origonal.getParams().getAsTypesAndNames();
			enterMethod(origonal.getMethodName(), origParams, origonal.getReturnType(), false, false, false, origonal.getAccessModifier(), ACC_BRIDGE + ACC_SYNTHETIC, null, false, origonal.methodGenricList);// TODO:

			this.bcoutputter.visitVarInsn(ALOAD, 0);
			for(int n = 1; n <= origParams.size(); n++) {
				Utils.applyLoad(this.bcoutputter, origParams.get(n-1).getA(), n);
			}
			String clsname = new NamedType(origonal.origin).getCheckCastType();
			this.bcoutputter.visitMethodInsn(INVOKESPECIAL, clsname, origonal.getMethodName(), getNormalMethodInvokationDesc(origonal.getParams().getAsTypes(), origonal.retType), false);//call defined version
			this.bcoutputter.visitInsn(Utils.returnTypeToOpcode(origonal.retType));
			exitMethod();
		}
	}
	
	private void addBridgeMethods(String impClassName, ArrayList<Pair<FuncDef, FuncDef>> bridgeMethodsToAdd) {
		for(Pair<FuncDef, FuncDef> bmeth : bridgeMethodsToAdd) {
			//origonal, provided
			FuncDef origonal = bmeth.getA();
			FuncDef provided = bmeth.getB();
			
			/*if(origonal.equals(provided)) {
				continue;
			}*/
			
			ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>  origParams =  origonal.getParams().getAsTypesAndNames();
			enterMethod(origonal.getMethodName(), origParams, origonal.getReturnType(), false, false, false, origonal.getAccessModifier(), ACC_BRIDGE + ACC_SYNTHETIC, null, false, origonal.methodGenricList);// TODO:

			ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>  providedParams =  provided.getParams().getAsTypesAndNames();
			
			this.bcoutputter.visitVarInsn(ALOAD, 0);
			for(int n = 1; n <= origParams.size(); n++) {
				Type gotType = origParams.get(n-1).getA();
				Utils.applyLoad(this.bcoutputter, gotType, n);
				Type expectedType = providedParams.get(n-1).getA();
				Utils.applyCastExplicit(bcoutputter, gotType, expectedType, this);
			}
			
			this.bcoutputter.visitMethodInsn(INVOKEVIRTUAL, impClassName, origonal.getMethodName(), getNormalMethodInvokationDesc(provided.getParams().getAsTypes(), provided.retType), false);//call defined version
			
			this.bcoutputter.visitInsn(Utils.returnTypeToOpcode(origonal.retType));
			
			exitMethod();
		}
		
	}
	
	private final ClassLevelAssignVisitor clsLevelAssVisitor = new ClassLevelAssignVisitor();
	
	private void createDefValueFieldInit(ClassDef cd, HashMap<String, Type> fieldNameToType, String fullClassName){
		//add a defaultFieldInit$(InitUncreatable, fieldMap boolean[]) method - used by @ copier for transient fields and dma for new 'missing' fields and transient fields 
		HashMap<String, Pair<Expression, Boolean>> asses = clsLevelAssVisitor.initFields(cd);
		ArrayList<String> names = new ArrayList<String>(asses.keySet());
		Collections.sort(names);
		
		//MethodVisitor mv = this.mv.cw.visitMethod(ACC_PRIVATE + ACC_SYNTHETIC, "defaultFieldInit$", "(Lcom/concurnas/bootstrap/runtime/InitUncreatable;[Z)V", null, null);
		
		bcoutputter = new BytecodeOutputter(this.cw, DEBUG_MODE);
		bcoutputter.enterMethod(ACC_PRIVATE + ACC_SYNTHETIC, "defaultFieldInit$", "(Lcom/concurnas/bootstrap/runtime/InitUncreatable;[Z)V", null, null, fullClassName);
		
		//mv.visitCode();
		
		//Label xx = new Label();
		//mv.visitLabel(xx);
		//mv.visitLineNumber(cd.getLine(), xx);
		
		int n=0;
		for(String name: names){
			//Assign ass = asses.get(name);
			Pair<Expression, Boolean> what = asses.get(name);
			Expression ass = what.getA();
			
			Type storeFieldType = fieldNameToType.get(name);
			
			//if(!(ass instanceof DuffAssign)){//if(thing!=null && thing[0]){assigne}
				if(ass != null && storeFieldType != null){
					if(!what.getB()){
						Label ifFalse = new Label();
						bcoutputter.visitVarInsn(ALOAD, 2);
						bcoutputter.visitJumpInsn(IFNULL, ifFalse);
						bcoutputter.visitVarInsn(ALOAD, 2);
						Utils.intOpcode(bcoutputter, n);
						bcoutputter.visitInsn(BALOAD);
						bcoutputter.visitJumpInsn(IFEQ, ifFalse);
						bcoutputter.visitLabel(new Label());//ifTrue
						
						bcoutputter.visitVarInsn(ALOAD, 0);
						//default value here...
						if(ass instanceof Block){
							((Block)ass).isClassFieldBlock = true;
						}
						Type rhsResolvesToType = (Type)ass.accept(this);
						/*if(ass instanceof AssignNew){
							((AssignNew)ass).expr.accept(this);
						}else{
							((AssignExisting)ass).asignment.accept(this);
						}*/
						if(!rhsResolvesToType.equals(storeFieldType)){//e.g. class MyClass{a double = 10}
							Utils.applyCastImplicit(bcoutputter, rhsResolvesToType, storeFieldType, this);
						}
						
						bcoutputter.visitFieldInsn(PUTFIELD, fullClassName, name, storeFieldType.getBytecodeType());
						bcoutputter.visitLabel(ifFalse);
					}else{//isTransient
						bcoutputter.visitVarInsn(ALOAD, 0);
						ass.accept(this);
						bcoutputter.visitFieldInsn(PUTFIELD, fullClassName, name, fieldNameToType.get(name).getBytecodeType());
					}
				}
				n++;
			//}
		}
		
		bcoutputter.visitInsn(RETURN);
		bcoutputter.exitMethod();
		//mv.visitMaxs(1, 1);
		//mv.visitEnd();
	}

	private void createNestorParentAccessor(ClassDef cls, int line) {
		String me = cls.bcFullName();
		String parString = cls.getParentNestor().bcFullName();

		MethodVisitor mv = this.bcoutputter.cw.visitMethod(ACC_STATIC + ACC_SYNTHETIC, "access$0", "(L" + me + ";)L" + parString + ";", null, null);
		mv.visitCode();
		Label lab = new Label();// needed why?
		mv.visitLabel(lab);
		mv.visitLineNumber(line, lab);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, me, "this$" + cls.getNestingLevel(), "L" + parString + ";");
		mv.visitInsn(ARETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();

	}

	@Override
	public Object visit(WithBlock withBlock) {
		// treat as fake up a try, finally
		// but firist you need interfaces, and first you need mixins
		// TODO Auto-generated method stub - need to have interfaces before we
		// can do this
		return null;
	}

	@Override
	public Object visit(UsingStatement usingStatement) {
		// TODO Auto-generated method stub --custom langs?
		return null;
	}

	private void earlyProcessFinalBlock(Block blk) {

		TheScopeFrame prev = this.currentScopeFrame;
		prev.addChild(blk.getIdentity(), blk.getScopeFrame());

		this.currentScopeFrame = prev.getChild(blk);

		
		// blk.accept(this);
		acceptLines(blk, false);// in stack order

		this.currentScopeFrame = prev;

	}

	private boolean hasPreEntryCode(PreEntryCode thing) {
		Stack<Pair<Label, Block>> items = thing.getLinesToVisitOnEntry();
		if (items != null && !items.isEmpty()) {
			// ensure not full of nulls
			for (Pair<Label, Block> i : items) {
				if (i.getA() != null) {
					return true;
				}
			}
			return false;
		}
		return false;
	}

	@Override
	public Object visit(ReturnStatement returnStatement) {

		if(returnStatement.astRepoint !=null){
			return returnStatement.astRepoint.accept(this);
		}
		
		boolean hasPreEntryCode = hasPreEntryCode(returnStatement);

		Type shouldRetrun = currentFuncReturnType.peek();

		//shouldRetrun = TypeCheckUtils.convertFromGenericToNamedType(shouldRetrun);
		
		boolean innerBodyEndedWithReturn = false;
		
		if (null != returnStatement.ret) {// TODO: check return type against
											// expectation - so ensure that type
											// is returned on all things which
											// can go on the rhs of the return
											// stmt
			Type retType = (Type) returnStatement.ret.accept(this);
			Utils.applyCastImplicit(bcoutputter, retType, shouldRetrun, this);
			//Utils.applyCastExplicit(mv, retType, shouldRetrun, this);
			
			if(returnStatement.ret instanceof Block){
				Block asBlock = (Block) returnStatement.ret;
				if(!asBlock.lines.isEmpty()){
					LineHolder lh = asBlock.lines.get(asBlock.lines.size()-1);
					if(lh.l instanceof ReturnStatement){
						innerBodyEndedWithReturn=true;
					}
					else if(lh.l instanceof BreakStatement){
						//ends with break having been repointed to a return statement... somewhat elaborate...
						BreakStatement asbreak = (BreakStatement)lh.l;
						if(null !=asbreak.astRepoint){
							if(asbreak.astRepoint instanceof Block){
								Block asBlock2 = (Block) asbreak.astRepoint;
								LineHolder lh2 = asBlock2.lines.get(asBlock2.lines.size()-1);
								if(lh2.l instanceof ReturnStatement){
									innerBodyEndedWithReturn=true;
								}
							}
						}
					}
				}
			}
			
			
		}

		boolean retNotVoid = !(shouldRetrun instanceof PrimativeType && ((PrimativeType) shouldRetrun).type == PrimativeTypeEnum.VOID);

		Label afterAdded = returnStatement.getLabelToVisitJustBeforeReturnOpCode();
		if (hasPreEntryCode) {
			Stack<Pair<Label, Block>> items = returnStatement.getLinesToVisitOnEntry();
			boolean onFirst = true;
			int tempSlot = -1;
			for (int n = items.size() - 1; n >= 0; n--) {
				Pair<Label, Block> bl = items.get(n);
				Label entryLabel = bl.getA();
				Block blk = bl.getB();
				if (null != blk) {
					if (blk.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()) {
						bcoutputter.pushNextLabel(entryLabel);

						if (retNotVoid) {
							Utils.popFromStack(bcoutputter, shouldRetrun);
						}

						bcoutputter.visitJumpInsn(GOTO, onReturnsInDefoFinallyRetBlock.peek());
						if (null != afterAdded) {
							bcoutputter.pushNextLabel(afterAdded);
						}
						return null;// skip rest
					} else {
						/*
						 * If hasPreEntryCode, then do: retTempvar = xys
						 * normally return -: do fially code :- return
						 * retTempvar
						 */

						if (onFirst && retNotVoid) {// only on first
							onFirst = false;
							tempSlot = this.createNewLocalVar(this.getTempVarName(), shouldRetrun, false);
							Utils.applyStore(bcoutputter, shouldRetrun, tempSlot);
						}

						bcoutputter.pushNextLabel(entryLabel);

						Label nextLabel = afterAdded;
						int moo = n - 1;
						while (moo >= 0) {// reverse crap again
							if (items.get(moo).getA() != null) {
								nextLabel = items.get(moo).getA();
								break;
							}
							moo--;
						}

						// ensure branching occurs correctly to following block
						// for the inserted code
						bcoutputter.labelOverride.put(afterAdded, nextLabel);
						earlyProcessFinalBlock(blk);
						bcoutputter.labelOverride.remove(afterAdded);

						boolean lastOne = true;
						for (int m = n - 1; m >= 0; m--) {
							// is last one of the non throwing variety
							Block blkN = items.get(m).getB();
							if (null != blkN && !blkN.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()) {
								lastOne = false;// no there are more which are
												// of this type
								break;
							}
						}

						if (lastOne) {// only on last
							bcoutputter.visitLabel(afterAdded);
							if (retNotVoid) {
								Utils.applyLoad(bcoutputter, shouldRetrun, tempSlot);
							}
						}
					}
				}
			}
		} else if (null != afterAdded) {// to support early termination of try
										// catch blocks
			// System.err.println("visit label for final block v2: " +
			// afterAdded);
			bcoutputter.pushNextLabel(afterAdded);
		}

		if(!innerBodyEndedWithReturn){//skip if inner returned already
			this.bcoutputter.visitInsn(Utils.returnTypeToOpcode(shouldRetrun));
		}

		return null;
	}

	private void breakContPreCode(PreEntryCode returnStatement) {
		boolean hasPreEntryCode = hasPreEntryCode(returnStatement);
		Label afterAdded = returnStatement.getLabelToVisitJustBeforeReturnOpCode();
		if (hasPreEntryCode) {
			Stack<Pair<Label, Block>> items = returnStatement.getLinesToVisitOnEntry();
			int tempSlot = -1;
			for (int n = items.size() - 1; n >= 0; n--) {
				Pair<Label, Block> bl = items.get(n);
				Label entryLabel = bl.getA();
				Block blk = bl.getB();
				if (null != blk) {
					if (blk.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()) {
						bcoutputter.pushNextLabel(entryLabel);
						return;// skip rest
					} else {
						/*
						 * If hasPreEntryCode, then do: retTempvar = xys
						 * normally return -: do fially code :- return
						 * retTempvar
						 */
						bcoutputter.pushNextLabel(entryLabel);

						Label nextLabel = afterAdded;
						int moo = n - 1;
						while (moo >= 0) {// reverse crap again
							if (items.get(moo).getA() != null) {
								nextLabel = items.get(moo).getA();
								break;
							}
							moo--;
						}
						// ensure branching occurs correctly to following block
						// for the inserted code
						bcoutputter.labelOverride.put(afterAdded, nextLabel);
						earlyProcessFinalBlock(blk);
						bcoutputter.labelOverride.remove(afterAdded);

						boolean lastOne = true;
						for (int m = n - 1; m >= 0; m--) {
							// is last one of the non throwing variety
							Block blkN = items.get(m).getB();
							if (null != blkN && !blkN.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()) {
								lastOne = false;// no there are more which are
												// of this type
								break;
							}
						}

						if (lastOne) {// only on last
							bcoutputter.visitLabel(afterAdded);
						}
					}
				}
			}
		} else if (null != afterAdded) {// to support early termination of try
										// catch blocks
			// System.err.println("visit label for final block v2: " +
			// afterAdded);
			bcoutputter.pushNextLabel(afterAdded);
		}

	}

	@Override
	public Object visit(BreakStatement breakStatement) {
		breakContPreCode(breakStatement);

		Label jumpTo = breakStatement.jumpTo;
		if (breakStatement.breaksOutOfTryCatchLevel > 0 && !breakGotoLabelOverride.isEmpty()) {
			jumpTo = breakGotoLabelOverride.peek();
		}

		if (breakStatement.returns != null) {
			Type acRet = (Type) breakStatement.returns.accept(this);
			acRet = Utils.unref(bcoutputter, acRet, TypeCheckUtils.getRefTypeToLocked(acRet), this);

			Type from = Utils.box(bcoutputter, acRet);
			Utils.applyLoad(bcoutputter, from, slotForBreakContinueInLoop.peek());
			Utils.genericSswap(bcoutputter, from, from);
			bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/util/LinkedList", "add", "(Ljava/lang/Object;)Z");
			bcoutputter.visitInsn(POP);
		}

		bcoutputter.visitJumpInsn(GOTO, jumpTo);
		return null;
	}

	@Override
	public Object visit(ContinueStatement continueStatement) {

		breakContPreCode(continueStatement);

		Label jumpTo = continueStatement.jumpTo;
		if (continueStatement.breaksOutOfTryCatchLevel > 0 && !breakGotoLabelOverride.isEmpty()) {
			jumpTo = breakGotoLabelOverride.peek();
		}

		if (continueStatement.returns != null) {
			Type acRet = (Type) continueStatement.returns.accept(this);
			acRet = Utils.unref(bcoutputter, acRet, TypeCheckUtils.getRefTypeToLocked(acRet), this);

			Type from = Utils.box(bcoutputter, acRet);
			Utils.applyLoad(bcoutputter, from, slotForBreakContinueInLoop.peek());
			Utils.genericSswap(bcoutputter, from, from);
			bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/util/LinkedList", "add", "(Ljava/lang/Object;)Z");
			bcoutputter.visitInsn(POP);
		}

		// TODO: is the above correct?
		bcoutputter.visitJumpInsn(GOTO, jumpTo);
		return null;
	}

	@Override
	public Object visit(RefThis refThis) {
		//mv.visitVarInsn(ALOAD, 0);
		Utils.applyLoad(bcoutputter, refThis.getTaggedType(), 0);
		
		Type resolvesto = refThis.getTaggedType();
		
		if(refThis.qualifier != null){
			ClassDef cd = ((NamedType)resolvesto).getSetClassDef();
			for(int n = this.currentClassDefObj.size()-1; n >= 0; n--){//move up heirachy to get to qualifee
				ClassDef atThislevel = this.currentClassDefObj.get(n);
				if(atThislevel.equals(cd)){
					break;
				}else{//GETFIELD bytecodeSandbox$Outerclass$Innerclass.this$1 : LbytecodeSandbox$Outerclass;
					ClassDef oneAbove = this.currentClassDefObj.get(n-1);
					
					bcoutputter.visitFieldInsn(GETFIELD, atThislevel.bcFullName(), "this$"+n, "L" + oneAbove.bcFullName() + ";");
					
				}
			}
			
		}
		return resolvesto;
	}
	
	@Override
	public Object visit(RefClass refClass) {
		/*if(resolvedViaFoldedConstant(refClass.getFoldedConstant())){
			return refClass.getTaggedType(); 
		}*/
		
		if(refClass.lhsType instanceof PrimativeType && !refClass.lhsType.hasArrayLevels()){//on primatives, we must do something specialp[
			Utils.typeForPrimative(bcoutputter, refClass.lhsType);
		}
		else{
			String check = refClass.lhsType.getBytecodeType();
			bcoutputter.visitLdcInsn(org.objectweb.asm.Type.getType(check));
		}
	
		return refClass.getTaggedType();
	}
	
	@Override
	public Object visit(RefOf refOf) {
		return refOf.resolveToOf.accept(this);
	}

	@Override
	public Object visit(RefSuper refSuper) {
		bcoutputter.visitVarInsn(ALOAD, 0);
		
		Type ret = refSuper.getTaggedType();
		
		if(!this.currentClassDefObj.isEmpty() && this.currentClassDefObj.peek().isTrait) {
			String bcTypeStr = ret.getBytecodeType();
			bcTypeStr = bcTypeStr.substring(1, bcTypeStr.length()-1);
			bcoutputter.visitTypeInsn(CHECKCAST, bcTypeStr);
		}
		
		return ret;
	}

	private Type getTypeOfLocalVar(String name) {
		Pair<Type, Integer> got = this.getLocalVar(name);
		return got == null ? null : got.getA();
	}

	// private void storeLocalVaraible(String name, Type passedType)
	// {
	// Tuple<Type, Integer> already = getLocalVar(name);
	// Type expectedType = already.getA();
	// int space = already.getB();
	//
	// if(!expectedType.equals(passedType))
	// {
	// Utils.applyCastImplicit(mv, passedType, expectedType);
	// }
	//
	// Utils.applyStore(mv, expectedType, space);
	// }
	//

	private void storeLocalVaraible(String name, TypeAndLocation resovlesTo, Type passedType/*, Tuple<NamedType, String> fieldOnMap*/) {
		storeLocalVaraible(name, resovlesTo, passedType, false/*, fieldOnMap*/);
	}

	private void storeLocalVaraible(String name, TypeAndLocation resovlesTo, Type passedType, boolean overwriteLHSRef/*,  Tuple<NamedType, String> fieldOnMap*/) {
		Location loc = resovlesTo == null ? null : resovlesTo.getLocation();

		Type typeStoredTo = resovlesTo == null ? null : resovlesTo.getType();

		if (null != loc && overwriteLHSRef) {
			AccessModifier acc = loc.getAccessModifier();
			if (acc == AccessModifier.PRIVATE || acc == AccessModifier.PRIVATE) {
				if (!this.currentClassDefObj.isEmpty()) {
					String currentClassName = this.currentClassDefObj.peek().bcFullName();

					// dont overwrite ref with a new one

					/*
					 * if(loc instanceof LocationStaticField){
					 * if(!((LocationStaticField
					 * )loc).owner.equals(currentClassName)){//private static
					 * field therefore dont set ref overwriteLHSRef=false; } }
					 * else if(loc instanceof LocationClassField){
					 * if(!((LocationClassField
					 * )loc).owner.equals(currentClassName)){//private static
					 * field therefore dont set ref overwriteLHSRef=false; } }
					 */
				}
			}
		}

		storeLocalVaraible(name, loc, typeStoredTo, passedType, overwriteLHSRef/*, fieldOnMap*/);
	}

	private void storeLocalVaraible(String name, Location loc, Type typeStoredTo, Type passedType/*,  Tuple<NamedType, String> fieldOnMap*/) {
		storeLocalVaraible(name, loc, typeStoredTo, passedType, false/*, fieldOnMap*/);
	}

	private void storeLocalVaraible(String name, Location loc, Type typeStoredTo, Type passedType, boolean overwriteLHSRef/*,  Tuple<NamedType, String> fieldOnMap*/) {
		boolean isRef = (typeStoredTo == null ? false : isRef(typeStoredTo));

		int storeLevels = TypeCheckUtils.getRefLevels(typeStoredTo);
		int passedLevls = TypeCheckUtils.getRefLevels(passedType);

		int levelsNeedToExtractToFromStoreLocation = storeLevels - passedLevls;

		// if(!overwriteLHSRef){
		// passedType = Utils.unref(mv, passedType, this);
		// }

		if (null == loc || loc instanceof LocationLocalVar) {
			Pair<Type, Integer> already = getLocalVar(name);
			Type expectedType = already.getA();
			int space = already.getB();

			/*if (isRef && !overwriteLHSRef) {
				Utils.applyLoad(mv, expectedType, space);
				Utils.genericSswap(mv, expectedType, passedType);
			} else if (isRef && levelsNeedToExtractToFromStoreLocation > 0) {
				Utils.applyLoad(mv, expectedType, space);
				Utils.genericSswap(mv, expectedType, passedType);
			}*/

			if (isRef && (!overwriteLHSRef || levelsNeedToExtractToFromStoreLocation > 0 ) ) {
				Utils.applyLoad(bcoutputter, expectedType, space);
				Utils.genericSswap(bcoutputter, expectedType, passedType);
			}
			
			if (!expectedType.equals(passedType)) {
				Utils.applyCastImplicit(bcoutputter, passedType, expectedType, this, true);
			}

			if (isRef && levelsNeedToExtractToFromStoreLocation > 0) {

			} else if (!isRef || (isRef && overwriteLHSRef)) {// dont store if ref
				try{
					Utils.applyStore(bcoutputter, expectedType, space);
				}
				catch(Exception e){
					throw e;
				}
			}
		} else if (loc instanceof LocationStaticField) {
			LocationStaticField locStatic = (LocationStaticField) loc;
			Type ret = locStatic.type;

			if (isRef && !overwriteLHSRef && !isInclInitCreator) {
				// TODO: what if it's a ref? and going via init creator
				// do u need an accessor for this?
				Pair<String, String> accAndName = loc == null ? null : loc.getPrivateStaticAccessorRedirectFuncGetter();
				if (null != accAndName) {
					this.bcoutputter.visitMethodInsn(INVOKESTATIC, locStatic.owner, accAndName.getA(), "()" + ret.getBytecodeType());
				} else {
					if(shouldPopFromStackOnStaticCall()) {
						bcoutputter.visitInsn(POP);//clean stack for static calls
					}
					
					bcoutputter.visitFieldInsn(GETSTATIC, locStatic.owner, name, ret.getBytecodeType());
				}

				Utils.genericSswap(bcoutputter, ret, passedType);
			} else if (isRef && levelsNeedToExtractToFromStoreLocation > 0 && !isInclInitCreator) {
				if(shouldPopFromStackOnStaticCall()) {
					bcoutputter.visitInsn(POP);//clean stack for static calls
				}
				
				bcoutputter.visitFieldInsn(GETSTATIC, locStatic.owner, name, ret.getBytecodeType());
				Utils.genericSswap(bcoutputter, ret, passedType);
			}

			if (!ret.equals(passedType)) {
				Utils.applyCastImplicit(bcoutputter, passedType, ret, this, !isInclInitCreator);
			}

			Thruple<String, String, Type> accAndName = loc == null ? null : loc.getPrivateStaticAccessorRedirectFuncSetter();

			if (isRef && levelsNeedToExtractToFromStoreLocation > 0 && !isInclInitCreator) {

			} else {
				if (!isRef || (isRef && overwriteLHSRef) || isInclInitCreator) {// dont
																				// store
																				// if
																				// ref
					if (null != accAndName && !isInclInitCreator) {// cursed to
																	// access
																	// via
																	// accessor
						this.bcoutputter.visitMethodInsn(INVOKESTATIC, locStatic.owner, accAndName.getA(), "(" + accAndName.getC().getBytecodeType() + ")V");
					} else {
						bcoutputter.visitFieldInsn(PUTSTATIC, locStatic.owner, name, ret.getBytecodeType());
					}
				}
			}

		} else if (loc instanceof LocationClassField) {
			LocationClassField locClassField = (LocationClassField) loc;
			Type ret = locClassField.ownerType;

			// boolean useTheGetterOnly = storeLevels==passedLevls; //this.d2a1
			// :: = xxx (where xxx == (2!)!)

			if (isRef && !overwriteLHSRef && !isInclInitCreator) {
				Utils.genericSswap(bcoutputter, passedType, locClassField.ownerType);

				Pair<String, String> getterola = locClassField.getPrivateStaticAccessorRedirectFuncGetter();
				if (null != getterola) {
					this.bcoutputter.visitMethodInsn(INVOKESTATIC, locClassField.getOwner(), getterola.getA(), "(L" + locClassField.getOwner() + ";)" + typeStoredTo.getBytecodeType());
				} else {
					bcoutputter.visitFieldInsn(GETFIELD, locClassField.getOwner(), name, typeStoredTo.getBytecodeType());
				}

				Utils.genericSswap(bcoutputter, locClassField.ownerType, passedType);
			} else if (isRef && levelsNeedToExtractToFromStoreLocation > 0 && !isInclInitCreator) {
				Utils.genericSswap(bcoutputter, passedType, ret);
				bcoutputter.visitFieldInsn(GETFIELD, locClassField.getOwner(), name, typeStoredTo.getBytecodeType());
				Utils.genericSswap(bcoutputter, ret, passedType);
			}

			if (!typeStoredTo.equals(passedType)) {
				Utils.applyCastImplicit(bcoutputter, passedType, typeStoredTo, this, !isInclInitCreator);
			}

			Thruple<String, String, Type> accAndName = loc == null ? null : loc.getPrivateStaticAccessorRedirectFuncSetter();

			if (isRef && levelsNeedToExtractToFromStoreLocation > 0 && !isInclInitCreator) {

			} else {
				if ((!isRef) || (isRef && overwriteLHSRef) || isInclInitCreator) {// dont
																					// store
																					// if
																					// ref
					if (null != accAndName && !isInclInitCreator) {// cursed to
																	// access
																	// via
																	// accessor
						this.bcoutputter.visitMethodInsn(INVOKESTATIC, locClassField.getOwner(), accAndName.getA(), "(L" + locClassField.getOwner() + ";" + accAndName.getC().getBytecodeType() + ")V");
					} else {
						
						if(TypeCheckUtils.typeRequiresLocalArrayConvertion(typeStoredTo)){
							//thing was a X[] array, but we've passed in a int:Ref[] - this is exposed as a refarray internally to conc (so we can keep the type information at runtime)
							//we need to extract ar from refarray
							bcoutputter.visitFieldInsn(GETFIELD, "com/concurnas/bootstrap/runtime/ref/LocalArray", "ar", "[Ljava/lang/Object;");
							//GETFIELD com/concurnas/bootstrap/runtime/ref/LocalArray.ar : [Ljava/lang/Object;
						}
						
						String owner = locClassField.getOwner();
						if(locClassField.ownerType instanceof NamedType){
							NamedType asNamed = (NamedType)locClassField.ownerType;
							//asNamed.namedType
							
							ClassDef cd = asNamed.getSetClassDef();
							String clsName = cd.getClassName();
							if(null!= clsName && clsName.contains("NIC$")){//ensure  only applies to nested innner classes being name remapped
								if(!clsName.equals(asNamed.namedType)){
									owner = asNamed.getCheckCastType();
								}
							}
						}//this is a big ugly, fails for imports?
						
						
						if(locClassField.ownerType instanceof NamedType && ((NamedType)locClassField.ownerType) .getSetClassDef().isTrait) {
							//traits dont have fields
							bcoutputter.visitMethodInsn(INVOKEINTERFACE, locClassField.getOwner(), "traitVar$" + name, "("+typeStoredTo.getBytecodeType()+")V", true);
						}else {
							bcoutputter.visitFieldInsn(PUTFIELD, owner, name, typeStoredTo.getBytecodeType());
						}
					}
				}
			}

		} else {
			throw new RuntimeException("unknown location type on var store: " + loc.getClass().getName());
		}
	}

	private boolean shouldPopFromStackOnStaticCall() {
		if(!this.dorOpLHS.isEmpty()) {
			Type top = this.dorOpLHS.peek();
			if(top != null){
				if(top instanceof NamedType) {
					return !((NamedType)top).isEnumParent;//enums dont need to be poped as they are not loaded onto the stack to begin with
				}
				
				return true;
			}
		}
		
		return false;
	}
	
	private Type loadLocalVar(String name, TypeAndLocation resovlesTo) {
		Location loc = resovlesTo == null ? null : resovlesTo.getLocation();
		Type ret;
		TopOfStack nextTopOfStack;

		if (null == loc || loc instanceof LocationLocalVar) {
			Pair<Type, Integer> got = this.getLocalVar(name);
			if (null != got) {
				ret = got.getA();
				Utils.applyLoad(this.bcoutputter, ret, got.getB());
				nextTopOfStack = new VarLocal(got);
			} else {
				ret = null;
				nextTopOfStack = null;
			}
		} else if (loc instanceof LocationStaticField) {
			LocationStaticField locStatic = (LocationStaticField) loc;
			ret = locStatic.type;

			Pair<String, String> accessorNameOrig = loc.getPrivateStaticAccessorRedirectFuncGetter();
			if (null != accessorNameOrig && !isInclInitCreator) {
				bcoutputter.visitMethodInsn(INVOKESTATIC, locStatic.owner, accessorNameOrig.getA(), "()" + ret.getBytecodeType());
			} else {
				if(null != locStatic.owner){
					/*if(locStatic.type instanceof NamedType){//owner locations being incorrectly tagged at times, especially for enum elements nested in delcarations within local classes
						NamedType locType = (NamedType)locStatic.type;
						String newOwner = locType.getBytecodeType();
						newOwner = newOwner.substring(1, newOwner.length()-1);
						//if(!locStatic.owner.equals(newOwner)){
							locStatic.owner = newOwner;
						//}
					}*/
					if(shouldPopFromStackOnStaticCall()) {
						bcoutputter.visitInsn(POP);//clean stack for static calls
					}
					
					bcoutputter.visitFieldInsn(GETSTATIC, locStatic.owner, name, ret.getBytecodeType());
				}
			}

			nextTopOfStack = new VarStatic(ret, locStatic);
		} else if (loc instanceof LocationClassField) {
			LocationClassField locClassField = (LocationClassField) loc;
			ret = locClassField.getOriginatesFromConstructorRef();
			if(null == ret){
				ret = resovlesTo.getType();
			}

			Pair<String, String> accessorNameOrig = loc.getPrivateStaticAccessorRedirectFuncGetter();
			if (null != accessorNameOrig && !isInclInitCreator) {
				bcoutputter.visitMethodInsn(INVOKESTATIC, locClassField.getOwner(), accessorNameOrig.getA(), "(L" + locClassField.getOwner() + ";)" + ret.getBytecodeType());
			} else {
				if (((LocationClassField) loc).isArrayLength) {
					if (TypeCheckUtils.isLocalArray(locClassField.ownerType)) {
						Utils.extractAndCastArrayRef(bcoutputter, locClassField.ownerType);
					}
					
					bcoutputter.visitInsn(ARRAYLENGTH);
				} else {
					
					if(locClassField.ownerType instanceof NamedType && ((NamedType)locClassField.ownerType) .getSetClassDef().isTrait) {
						//traits dont have fields
						bcoutputter.visitMethodInsn(INVOKEINTERFACE, locClassField.getOwner(), "traitVar$" + name, "()" + ret.getBytecodeType(), true);
					}else {
						bcoutputter.visitFieldInsn(GETFIELD, locClassField.getOwner(), name, ret.getBytecodeType());
					}
				}
			}
			nextTopOfStack = new VarClassField(ret, locClassField);
		} else {
			throw new RuntimeException("unknown location type: " + loc.getClass().getName());
		}

		if (null != this.topOfStack) {
			this.topOfStack = nextTopOfStack;
		}

		return ret;

	}

	@Override
	public Object visit(RefBoolean refBoolean) {

		if (refBoolean.b) {
			bcoutputter.visitInsn(ICONST_1);
		} else {
			bcoutputter.visitInsn(ICONST_0);
		}

		return new PrimativeType(PrimativeTypeEnum.BOOLEAN);
	}

	@Override
	public Object visit(FuncRefArgs funcRefArgs) {
		return null;// null is ok
	}

	@Override
	public Collection<? extends ErrorHolder> getErrors() {
		return null;
	}

	public static String getNormalMethodInvokationDesc(ArrayList<Type> inputs, Type returnType, boolean boxargs, boolean boxReturn) {
		// "(Ljava/util/ArrayList;)D"
		StringBuilder sigNormalSB = new StringBuilder();

		sigNormalSB.append("(");
		for (Type arg : inputs) {
			if(arg instanceof NamedType){
				NamedType asNamed = (NamedType)arg;
				if(null != asNamed.astredirect){
					arg = asNamed.astredirect;
				}
			}
			
			if(arg instanceof FuncType){
				//arg = ((FuncType) arg).copyTypeSpecific();
				//arg.setOrigonalGenericTypeUpperBound(null);//yuck, why is this set in the first place?
				((FuncType)arg).usedInMethodDesc = true;
			}
			sigNormalSB.append((boxargs ? TypeCheckUtils.boxTypeIfPrimative(arg, false) : arg).getBytecodeType());
		}
		sigNormalSB.append(")");

		if(returnType instanceof FuncType){
			((FuncType)returnType).usedInMethodDesc = true;
		}
		
		if(returnType == null) {
			sigNormalSB.append(Const_PRIM_VOID.getBytecodeType());
		}else if(returnType.equals(ScopeAndTypeChecker.const_void) && !boxReturn){
			sigNormalSB.append( returnType.getBytecodeType());
		}else {
			sigNormalSB.append((boxargs? TypeCheckUtils.boxTypeIfPrimative(returnType, false):returnType).getBytecodeType());
		}
		

		return sigNormalSB.toString();
	}

	public static String getNormalMethodInvokationDesc(ArrayList<Type> inputs, Type returnType) {
		return getNormalMethodInvokationDesc(inputs, returnType, false, false);
	}

	private ArrayList<Pair<Type, String>> varsToAddToScopeOnEntry = new ArrayList<Pair<Type, String>>();

	/*
	 * private void visitThePreLabeledTryCAtches(FuncDefI funcDef) {//we have to
	 * reverse the order of the preallocated labels else it breaks nested
	 * exceptions //visitor pattern + nested exceptions => fail without this
	 * if(null != funcDef.stuffTovisitTryCatchBlock &&
	 * !funcDef.stuffTovisitTryCatchBlock.isEmpty()) {//we have to visit all the
	 * try catch stuff on entry to the method ??, there maybe moe
	 * than one nesting level hence arr list
	 * 
	 * ArrayList<TryCatchBlockPreallocatedLabelsEtc> levls =
	 * funcDef.stuffTovisitTryCatchBlock; //Collections.reverse(levls);
	 * 
	 * for(TryCatchBlockPreallocatedLabelsEtc stuff : levls) {
	 * ArrayList<ArrayList<StartEndHandle>> nestedHandles = new
	 * ArrayList<ArrayList<StartEndHandle>>();
	 * 
	 * while(null != stuff) { ArrayList<StartEndHandle> forThisLevel = new
	 * ArrayList<StartEndHandle>(); for(StartEndHandle seh :
	 * stuff.startEndHandles) { forThisLevel.add(seh); }
	 * nestedHandles.add(forThisLevel); stuff = stuff.child; }
	 * //Collections.reverse(nestedHandles);
	 * 
	 * for(ArrayList<StartEndHandle> level : nestedHandles) { for(StartEndHandle
	 * seh : level) { //System.err.println(String.format(
	 * "visitTryCatchBlock star: %s, end: %s, handle: %s | type: %s",
	 * seh.startScope, seh.endScope, seh.catchHandler, seh.type));
	 * mv.visitTryCatchBlock(seh.startScope, seh.endScope, seh.catchHandler,
	 * seh.type); } } } } }
	 */

	private CatchBlockAllocator catchBlockAllocator = new CatchBlockAllocator();

	private void visitTryCatchLabels(Block blk) {
		for (StartEndHandle seh : catchBlockAllocator.process(blk)) { // System.err.println(String.format("visitTryCatchBlock star: %s, end: %s, handle: %s | type: %s",
																		// seh.startScope,
																		// seh.endScope,
																		// seh.catchHandler,
																		// seh.type));
			bcoutputter.visitTryCatchBlock(seh.startScope, seh.endScope, seh.catchHandler, seh.type);
		}
	}

	private enum ConstruType {
		NONE, SUPER, THIS
	};

	private ConstruType isFirstLineConstructorInvokate(Line first, NamedType parNestor) {// super
																							// or
																							// this
																							// invokation
		if (null != first) {
			if (first instanceof DuffAssign) {
				DuffAssign da = (DuffAssign) first;
				if (da.e instanceof SuperConstructorInvoke) {
					SuperConstructorInvoke thi = (SuperConstructorInvoke) da.e;
					thi.parNestorToAdd = parNestor;

					return ConstruType.SUPER;
				} else if (da.e instanceof ThisConstructorInvoke) {
					ThisConstructorInvoke thi = (ThisConstructorInvoke) da.e;
					thi.parNestorToAdd = parNestor;

					return ConstruType.THIS;
				}
			}
			else if (first instanceof Block) {
				LineHolder lh = ((Block)first).getFirst();
				return isFirstLineConstructorInvokate(lh==null?null:lh.l, parNestor);
			}
		}
		return ConstruType.NONE;
	}

	@Override
	public Object visit(ConstructorDef funcDef) {
		ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> inputs = funcDef.params == null ? null : funcDef.params.getAsTypesAndNames();
		Type returnType = funcDef.retType == null ? Const_PRIM_VOID : funcDef.retType;

		ClassDef currentClass = this.currentClassDefObj.peek();
		ClassDef parentNestror = currentClass.getParentNestor();
		boolean isNestedClass = null != parentNestror && !currentClass.isEnumSubClass;

		//funcDef.isEnumconstru = currentClass.isEnum;//add extra arguments
		
		// mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(LA/Child2$Outer;I)V",
		// null, null);
		NamedType parNestor = null;
		if (isNestedClass) {// nested class first input arg is outer class ref
			parNestor = new NamedType(parentNestror);
			inputs.add(0, new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(parNestor, null, null, false, false, false));
		}

		if(funcDef.isEnumconstru){
			inputs.add(0, new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>((Type)ScopeAndTypeChecker.const_int.copy(), "enum$1", null, false, false, false));
			inputs.add(0, new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>((Type)ScopeAndTypeChecker.const_string.copy(), "enum$0", null, false, false, false));
		}
		
		//enterMethod(funcDef.funcName, inputs, returnType, false, funcDef.getAccessModifier(), 0);
		if(null != funcDef.annotations){ funcDef.annotations.bcVisitor=this;}
		
		int nn=0;
		for(Sixple<Type, String, Annotations, Boolean, Boolean, Boolean> arg : inputs ){
			Annotations annot = arg.getC();
			if(null != annot){
				annot.bcVisitor = this;
				for(Annotation an : annot.annotations){
					an.parameterAnnotaionArg = nn;
				}
			}
			nn++;
		}
		
		enterMethod(funcDef.funcName, inputs, returnType, false, false, false, funcDef.getAccessModifier(), 0, funcDef.annotations, false, null);
		
		visitTryCatchLabels(funcDef.getBody());

		ConstruType constTyp = isFirstLineConstructorInvokate(funcDef.funcblock.getFirstLine(), parNestor);
		if(constTyp == ConstruType.SUPER || constTyp == ConstruType.NONE){
			if (isNestedClass ) {// just one level thankfully!
				String currentClassName = currentClass.bcFullName();
				String parentClassName = "L" + parentNestror.bcFullName() + ";";
				
				bcoutputter.visitVarInsn(ALOAD, 0);
				bcoutputter.visitVarInsn(ALOAD, 1);
				
				// mv.visitFieldInsn(PUTFIELD, "A/Child2$Outer$InnerClass",
				// "this$1", "LA/Child2$Outer;");
				bcoutputter.visitFieldInsn(PUTFIELD, currentClassName, "this$" + currentClass.getNestingLevel(), parentClassName);
			}
		/*	else if(isEnumconstru ){
				Label l0 = new Label();
				mv.visitLabel(l0);
				mv.visitNewLine(funcDef.getLine());
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitVarInsn(ILOAD, 2);
				mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Enum", "<init>", "(Ljava/lang/String;I)V", false);
			}*/
		}
		

		// first line this - dont invoke fields, proces as normal
		// first line super - need to invoke fields
		// fist line nothing, need to invoke fields [insert artifical one]
		boolean removeFirstFromFB = false;
		if (constTyp == ConstruType.NONE) {
			// must call super, add implicit super call
			int line = funcDef.getLine();
			int col = funcDef.getColumn();

			FuncInvokeArgs args = new FuncInvokeArgs(line, col);
			ArrayList<Type> ttypes= new ArrayList<Type>();
			if(currentClass.isActor){
				args.add(new RefName(0,0, ScopeAndTypeChecker.TypesForActor));
				ttypes.add(ScopeAndTypeChecker.const_classArray_nt_array.copyTypeSpecific());
			}
			if(funcDef.isEnumconstru){

				args.add(new RefName(0,0, "enum$0"));
				ttypes.add((Type)ScopeAndTypeChecker.const_string.copyTypeSpecific());
				
				args.add(new RefName(0,0, "enum$1"));
				ttypes.add((Type)ScopeAndTypeChecker.const_int.copyTypeSpecific());

				if(funcDef.isEnumconstruSubClass){
					args.add(new VarNull());
					ttypes.add(new NamedType(parentNestror));
				}
			}
			
			SuperConstructorInvoke sup = new SuperConstructorInvoke(line, col, args);
			sup.resolvedFuncType = new FuncType(ttypes, Const_PRIM_VOID);
			DuffAssign da = new DuffAssign(line, col, sup);
			funcDef.funcblock.lines.add(0, new LineHolder(line, col, da));//nasty, means we cannot recall this when we later recompile
			removeFirstFromFB = true;
		}

		if (constTyp == ConstruType.SUPER || constTyp == ConstruType.NONE) {
			funcDef.funcblock.defFeildInit = new DefaultConstuctorFieldInitlizator(this);
		}

		if (null != funcDef.funcblock) {
			
			this.varsToAddToScopeOnEntry = stripOutAnnotations(inputs);
			level++;
			funcDef.funcblock.accept(this);
			level--;
			if (null != funcDef.funcblock.mustVisitLabelBeforeRet) {
				bcoutputter.visitLabel(funcDef.funcblock.mustVisitLabelBeforeRet);
			}
		}

		if(removeFirstFromFB) {
			funcDef.funcblock.lines.remove(0);
		}
		
		bcoutputter.visitInsn(RETURN);
		//level--;
		exitMethod();

		return null;
	}
	
	private ArrayList<Pair<Type, String>> stripOutAnnotations(ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> inputs){
		ArrayList<Pair<Type, String>> ret = new ArrayList<Pair<Type, String>>(inputs.size());
		
		for(Sixple<Type, String, Annotations, Boolean, Boolean, Boolean> input : inputs){
			ret.add(new Pair<Type, String>(input.getA(), input.getB()));
		}
		
		return ret;
	}

	/**
	 * DONE!!!!
	 */

	@Override
	public Object visit(VarString varString) {
		if(varString.subExpressions != null){
			if(varString.subExpressions.size() == 1){//boring
				Type got = (Type)varString.subExpressions.get(0).accept(this);
				if(!got.equals(ScopeAndTypeChecker.const_string)){//convert to string
					StringBuffHelper.append(this, got, true);//dirty hack
				}
			}
			else{//involves concat stuff together
				StringBuffHelper.init(this);
				StringBuffHelper.start(this, (Type)varString.subExpressions.get(0).accept(this));
				for(int n=1; n < varString.subExpressions.size(); n++){
					StringBuffHelper.append(this, (Type)varString.subExpressions.get(n).accept(this));
				}
				StringBuffHelper.end(this);
			}
		}
		else{
			bcoutputter.visitLdcInsn(varString.str.replace("\\{", "{"));
		}
		
		return new NamedType(new ClassDefJava(String.class));
	}
	
	@Override
	public Object visit(VarRegexPattern varString) {
		bcoutputter.visitLdcInsn(varString.str);
		bcoutputter.visitMethodInsn(INVOKESTATIC, "java/util/regex/Pattern", "compile", "(Ljava/lang/String;)Ljava/util/regex/Pattern;");
		
		return varString.getTaggedType();
	}

	@Override
	public Object visit(VarChar varChar) {
		bcoutputter.visitIntInsn(BIPUSH, varChar.chr.charAt(0));// JPT: to use this
														// method you need to
														// fiddle the \n stuff
														// in owl.antlr
		return new PrimativeType(PrimativeTypeEnum.CHAR);
	}

	@Override
	public Object visit(VarNull varNull) {
		this.bcoutputter.visitInsn(ACONST_NULL);
		if(varNull.forceCheckCast != null) {
			bcoutputter.visitTypeInsn(CHECKCAST, varNull.forceCheckCast.getCheckCastType());
			//this.doCast(ScopeAndTypeChecker.const_object, varNull.forceCheckCast);
		}
		return varNull;
	}

	@Override
	public Object visit(VarInt varInt) {
		Utils.intOpcode(this.bcoutputter, varInt.inter);
		return new PrimativeType(PrimativeTypeEnum.INT);
	}

	@Override
	public Object visit(VarLong varLong) {
		Utils.longOpcode(this.bcoutputter, varLong.longer);
		return new PrimativeType(PrimativeTypeEnum.LONG);
	}
	
	@Override
	public Object visit(VarShort varLong) {
		Utils.intOpcode(this.bcoutputter, varLong.shortx);
		return new PrimativeType(PrimativeTypeEnum.SHORT);
	}
	

	@Override
	public Object visit(VarFloat varFloat) {
		Utils.floatOpcode(this.bcoutputter, varFloat.floater);
		return new PrimativeType(PrimativeTypeEnum.FLOAT);
	}

	@Override
	public Object visit(VarDouble varDouble) {
		Utils.doubleOpcode(this.bcoutputter, varDouble.doubler);
		return new PrimativeType(PrimativeTypeEnum.DOUBLE);
	}

	@Override
	public Object visit(MulerExpression mulerExpression) {
		if(resolvedViaFoldedConstant(mulerExpression.getFoldedConstant(), mulerExpression.getTaggedType())){
			return mulerExpression.getTaggedType(); 
		}
		Type topType = (Type) mulerExpression.header.accept(this);
		
		for (MulerElement me : mulerExpression.elements) {
			
			if(me.astOverrideOperatorOverload != null){
				Utils.unref(bcoutputter, topType, this);
				topType = (Type)me.astOverrideOperatorOverload.accept(this);
			}
			else{
				// TODO: again we cheat here and take the lhs type, when we really shoudl take the more general of the two.... meh! e.g. 5 + 4l -> 5+4 but should be 5l+4;...
				TypeCheckUtils.unlockAllNestedRefs(topType);
				
				Type typeOfOverallOperation = me.getTaggedType();
				
				if (!topType.equals(typeOfOverallOperation)) {
					Utils.applyCastImplicit(bcoutputter, topType, typeOfOverallOperation, this);
				}

				Type rhs = (Type) me.expr.accept(this);
				TypeCheckUtils.unlockAllNestedRefs(rhs);
				if (!rhs.equals(typeOfOverallOperation)) {
					Utils.applyCastImplicit(bcoutputter, rhs, typeOfOverallOperation, this);
				}
				Utils.applyMuler(bcoutputter, (PrimativeType) typeOfOverallOperation, me.mulOper);
				topType = typeOfOverallOperation;
			}
		}
			
		return topType;
	}

	@Override
	public Object visit(MulerElement mulerElement) {
		return null;// null is ok here
	}

	@Override
	public Object visit(ShiftElement shiftElement) {
		return null;
	}

	@Override
	public Object visit(ShiftExpression shiftExpression) {		
		if(resolvedViaFoldedConstant(shiftExpression.getFoldedConstant(), shiftExpression.getTaggedType())){
			return shiftExpression.getTaggedType(); 
		}
		Type topType = (Type) shiftExpression.header.accept(this);
		
		for (ShiftElement me : shiftExpression.elements) {
			
			if(me.astOverrideOperatorOverload != null){
				Utils.unref(bcoutputter, topType, this);
				topType = (Type)me.astOverrideOperatorOverload.accept(this);
			}
			else{
				// TODO: again we cheat here and take the lhs type, when we really shoudl take the more general of the two.... meh! e.g. 5 + 4l -> 5+4 but should be 5l+4;...
				TypeCheckUtils.unlockAllNestedRefs(topType);
				
				Type typeOfOverallOperation = me.getTaggedType();
				
				if (!topType.equals(typeOfOverallOperation)) {
					Utils.applyCastImplicit(bcoutputter, topType, typeOfOverallOperation, this);
				}

				Type rhs = (Type) me.expr.accept(this);
				TypeCheckUtils.unlockAllNestedRefs(rhs);
				if (!rhs.equals(typeOfOverallOperation)) {
					Utils.applyCastImplicit(bcoutputter, rhs, ScopeAndTypeChecker.const_int, this);
				}
				Utils.applyShift(bcoutputter, (PrimativeType) typeOfOverallOperation, me.shiftOp);
				topType = typeOfOverallOperation;
			}
		}
			
		return topType;
	}
	
	
	

	@Override
	public Object visit(BitwiseOperation bitwiseOperation) {
		if(resolvedViaFoldedConstant(bitwiseOperation.getFoldedConstant(), bitwiseOperation.getTaggedType())){
			return bitwiseOperation.getTaggedType(); 
		}
		Type topType = (Type) bitwiseOperation.head.accept(this);
		
		for (RedirectableExpression me : bitwiseOperation.things) {
			if(me.astOverrideOperatorOverload != null){
				Utils.unref(bcoutputter, topType, this);
				topType = (Type)me.astOverrideOperatorOverload.accept(this);
			}
			else{
				// TODO: again we cheat here and take the lhs type, when we really shoudl take the more general of the two.... meh! e.g. 5 + 4l -> 5+4 but should be 5l+4;...
				TypeCheckUtils.unlockAllNestedRefs(topType);
				
				Type typeOfOverallOperation = me.getTaggedType();
				
				if (!topType.equals(typeOfOverallOperation)) {
					Utils.applyCastImplicit(bcoutputter, topType, typeOfOverallOperation, this);
				}

				Type rhs = (Type) me.exp.accept(this);
				TypeCheckUtils.unlockAllNestedRefs(rhs);
				if (!rhs.equals(typeOfOverallOperation)) {
					Utils.applyCastImplicit(bcoutputter, rhs, ScopeAndTypeChecker.const_int, this);
				}
				
				Utils.applyBitwise(bcoutputter, (PrimativeType) typeOfOverallOperation, bitwiseOperation.oper);
				
				topType = me.getTaggedType();
			}
		}
			
		return topType;
	}
	
	

	private TopOfStack topOfStack = null;

	@Override
	public Object visit(DuffAssign duffAssign) {

		if (isLastLineInBlock.peek()) {
			duffAssign.setShouldBePresevedOnStack(lastLineInBlockShouldPresev.peek());
		} else {
			duffAssign.setShouldBePresevedOnStack(false);
		}
		Expression expr = duffAssign.e;
		if(expr instanceof Annotation && ((Annotation)expr).ignoreWhenGenByteCode){
			return null;
		}
		else{
			return expr.accept(this);
		}
	}

	@Override
	public Object visit(GrandLogicalElement equalityElement) {
		return null;// null is ok
	}

	@Override
	public Object visit(AndExpression andExpression) {
		if(resolvedViaFoldedConstant(andExpression.getFoldedConstant(), andExpression.getTaggedType())){
			return andExpression.getTaggedType(); 
		}

		return headlessBooleanOperation(andExpression);
	}
	
	@Override
	public Object visit(NotExpression notExpression) {
		if(resolvedViaFoldedConstant(notExpression.getFoldedConstant(), notExpression.getTaggedType())){
			return notExpression.getTaggedType(); 
		}
		
		if(notExpression.astOverrideOperatorOverload != null){
			notExpression.expr.accept(this);
			return (Type)notExpression.astOverrideOperatorOverload.accept(this);
		}
		else{
			Label localcarryOn = notExpression.labelAfterNot;// new Label();
			if (null == localcarryOn) {
				localcarryOn = new Label();
			}
			Label onFail = new Label();
			convertToBoolean((Type) notExpression.expr.accept(this), onFail);
			bcoutputter.visitJumpInsn(IFEQ, onFail);
			bcoutputter.visitInsn(ICONST_0);
			bcoutputter.visitJumpInsn(GOTO, localcarryOn);
			bcoutputter.visitLabel(onFail);
			bcoutputter.visitInsn(ICONST_1);

			
			this.bcoutputter.visitLabel(localcarryOn);
			
			//this.mv.pushNextLabel(localcarryOn);
			return Const_PRIM_BOOl;
		}
		
		
		// return headlessBooleanOperation(notExpression);
	}
	
	private static final String copyTracker = "com/concurnas/bootstrap/runtime/CopyTracker";

	private void makeConctracker() {
		bcoutputter.visitTypeInsn(NEW, copyTracker);
		bcoutputter.visitInsn(DUP);
		bcoutputter.visitMethodInsn(INVOKESPECIAL, copyTracker, "<init>", "()V");
		
	}
	
	private void makeCopyDefinition(CopyExpression copyExpression) {
		if(copyExpression == null) {
			bcoutputter.visitInsn(ACONST_NULL);
		}else {
			makeCopyDefinition(copyExpression.copyItems, copyExpression.copySpecMustInclude, copyExpression.nodefault);
		}
	}
	
	private void makeCopyDefinition(List<CopyExprItem> copyItems, HashSet<String> copySpecMustInclude, boolean nodefaults) {
		
		if(null != copyItems) {
			bcoutputter.visitTypeInsn(NEW, "com/concurnas/runtime/copy$CopyDefinition");
			bcoutputter.visitInsn(DUP);
			
			ArrayList<CopyExprAssign> overrideExprs = new ArrayList<CopyExprAssign>(0);
			ArrayList<CopyExprNested> nesteds = new ArrayList<CopyExprNested>(0);
			CopyExprExclOnly copyExprExlOnly = null;
			CopyExprSuper superCopier = null;
			
			for (CopyExprItem item : copyItems) {
				if (item instanceof CopyExprAssign) {
					overrideExprs.add((CopyExprAssign)item);
				}else if (item instanceof CopyExprExclOnly) {
					copyExprExlOnly = (CopyExprExclOnly)item;
				}
				else if(item instanceof CopyExprNested) {
					nesteds.add((CopyExprNested)item);
				}else if(item instanceof CopyExprSuper) {
					superCopier = (CopyExprSuper)item;
				}
			}
			
			if(superCopier != null) {
				makeCopyDefinition(superCopier.copyItems, superCopier.incItems, superCopier.nodefault);
			}else {
				bcoutputter.visitInsn(ACONST_NULL);
			}
			
			
			if(!overrideExprs.isEmpty()) {
				bcoutputter.visitTypeInsn(NEW, "java/util/HashMap");
				bcoutputter.visitInsn(DUP);
				bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
				
				for(CopyExprAssign cop : overrideExprs) {
					bcoutputter.visitInsn(DUP);
					bcoutputter.visitLdcInsn(cop.field);
					Type got = (Type)cop.assignment.accept(this); Utils.box(bcoutputter, got);
					bcoutputter.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
					bcoutputter.visitInsn(POP);
				}
			}else {
				bcoutputter.visitInsn(ACONST_NULL);
			}
			
			if(null != copySpecMustInclude) {
				
				bcoutputter.visitTypeInsn(NEW, "java/util/HashSet");
				bcoutputter.visitInsn(DUP);
				Utils.intOpcode(bcoutputter, copySpecMustInclude.size());
				bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/util/HashSet", "<init>", "(I)V", false);
				
				for(String x : copySpecMustInclude) {
					bcoutputter.visitInsn(DUP);
					bcoutputter.visitLdcInsn(x);
					bcoutputter.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "add", "(Ljava/lang/Object;)Z", true);
					bcoutputter.visitInsn(POP);
				}
			}else {
				bcoutputter.visitInsn(ACONST_NULL);
			}
			
			if(null != copyExprExlOnly) {
				bcoutputter.visitTypeInsn(NEW, "java/util/HashSet");
				bcoutputter.visitInsn(DUP);
				Utils.intOpcode(bcoutputter, copyExprExlOnly.excludeOnly.size());
				bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/util/HashSet", "<init>", "(I)V", false);
				
				for(String x : copyExprExlOnly.excludeOnly) {
					bcoutputter.visitInsn(DUP);
					bcoutputter.visitLdcInsn(x);
					bcoutputter.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "add", "(Ljava/lang/Object;)Z", true);
					bcoutputter.visitInsn(POP);
				}
			}else {
				bcoutputter.visitInsn(ACONST_NULL);
			}
			
			if(!nesteds.isEmpty()) {
				
				bcoutputter.visitTypeInsn(NEW, "java/util/HashMap");
				bcoutputter.visitInsn(DUP);
				bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
				
				for(CopyExprNested cop : nesteds) {
					bcoutputter.visitInsn(DUP);
					bcoutputter.visitLdcInsn(cop.fname);
					
					makeCopyDefinition(cop.copyItems, cop.incItems, cop.nodefault);
					
					bcoutputter.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
					bcoutputter.visitInsn(POP);
				}
				
			}else {
				bcoutputter.visitInsn(ACONST_NULL);
			}
			
			bcoutputter.visitInsn(nodefaults?ICONST_0:ICONST_1);//include defaults?
			
			bcoutputter.visitMethodInsn(INVOKESPECIAL, "com/concurnas/runtime/copy$CopyDefinition", "<init>", "(Lcom/concurnas/bootstrap/runtime/CopyDefinition;Ljava/util/Map;Ljava/util/Set;Ljava/util/Set;Ljava/util/Map;Z)V", false);
		}else {
			bcoutputter.visitInsn(ACONST_NULL);
		}
		
	}
	
	private Type doCopy(Type tagged, Expression expr, int trackerSlot, CopyExpression copyExpression, String fromStateObject) {
		String clsName = tagged.getCheckCastType();
		if (tagged instanceof NamedType && !tagged.hasArrayLevels()) {
			ClassDef setClass = ((NamedType) tagged).getSetClassDef();
			if (!setClass.javaSystemLib && !setClass.isInterface()) {//interfaces have to be called the slow way
				// can call fast copier
				//Utils.unref(mv, (Type)expr.accept(this), this);
				if(null != expr){
					expr.accept(this);
					if(fromStateObject != null) {
						bcoutputter.visitFieldInsn(GETFIELD, fromStateObject, ((RefName)expr).name, tagged.getBytecodeType());//GETFIELD bytecodeSandbox$$onChange2$SO a1$n1 LbytecodeSandbox$Instance;
					}
				}
				
				if(trackerSlot == -1) {
					makeConctracker();
				}else {
					bcoutputter.visitVarInsn(ALOAD, trackerSlot);
				}
				
				makeCopyDefinition(copyExpression);
				
				int copcod = INVOKEVIRTUAL;
				if(setClass.isInterface()) {
					copcod = INVOKEINTERFACE;
				}
				bcoutputter.visitMethodInsn(copcod, clsName, "copy", String.format("(Lcom/concurnas/bootstrap/runtime/CopyTracker;Lcom/concurnas/bootstrap/runtime/CopyDefinition;)L%s;", clsName));
				return tagged;
			}
		}
		int tempSlot=0;
		if(null == expr){
			tempSlot = this.createNewLocalVar(this.getTempVarName(), tagged, true);
		}
		
		// call slow copier
		bcoutputter.visitFieldInsn(GETSTATIC, "com/concurnas/runtime/bootstrapCloner/Cloner", "cloner", "Lcom/concurnas/runtime/bootstrapCloner/Cloner;");
		
		if(trackerSlot == -1) {
			makeConctracker();
		}else {
			bcoutputter.visitVarInsn(ALOAD, trackerSlot);
		}
		
		if(null != expr){
			expr.accept(this);
			if(fromStateObject != null) {
				bcoutputter.visitFieldInsn(GETFIELD, fromStateObject, ((RefName)expr).name, tagged.getBytecodeType());//GETFIELD bytecodeSandbox$$onChange2$SO a1$n1 LbytecodeSandbox$Instance;
			}
		}
		else{
			Utils.applyLoad(bcoutputter, tagged, tempSlot);
		}

		makeCopyDefinition(copyExpression);
		
		bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/runtime/bootstrapCloner/Cloner", "clone", "(Lcom/concurnas/bootstrap/runtime/CopyTracker;Ljava/lang/Object;Lcom/concurnas/bootstrap/runtime/CopyDefinition;)Ljava/lang/Object;");
		bcoutputter.visitTypeInsn(CHECKCAST, clsName);
		return tagged;
	}

	@Override
	public Object visit(CopyExpression copyExpression) {
		return doCopy(copyExpression.getTaggedType(), copyExpression.expr, -1, copyExpression, null);
	}

	@Override
	public Object visit(OrExpression orExpression) {
		if(resolvedViaFoldedConstant(orExpression.getFoldedConstant(), orExpression.getTaggedType())){
			return orExpression.getTaggedType(); 
		}
		/*
		 * Label ifOk = new Label(); Label carryOn = new Label();
		 * orExpression.head.accept(this); visitLableIfRequired();
		 * mv.visitJumpInsn(IFNE, ifOk); for(Expression and : orExpression.ands)
		 * { and.accept(this); visitLableIfRequired(); mv.visitJumpInsn(IFNE,
		 * ifOk); } //fail... mv.visitInsn(ICONST_0); mv.visitJumpInsn(GOTO,
		 * carryOn); //ok mv.visitLabel(ifOk); mv.visitInsn(ICONST_1);
		 * nextLabel.push(carryOn);
		 * 
		 * return Const_PRIM_BOOl;
		 */
		
		if(orExpression.things.size() == 1){//special case where its just one overloaded thing
			RedirectableExpression item = orExpression.things.get(0);
			if(item.astOverrideOperatorOverload != null){
				Type got = (Type)orExpression.head.accept(this);
				got = Utils.unref(bcoutputter, got, this);
				return item.accept(this);
			}
		}
		
		return convertOrToAnd(orExpression).accept(this);// FMF

		// return headlessBooleanOperation(orExpression);
	}

	private Expression convertOrToAnd(OrExpression orExpression) {
		// Finnaly! a change to makepraticalapplicationof demorgans lawMHA:FMF: for i havesinned...
		
		ArrayList<RedirectableExpression> neworList = new ArrayList<RedirectableExpression>();
		
		Expression soFar = orExpression.head;//new NotExpression(orExpression.getLine(), orExpression.getColumn(), orExpression.head);

		for (RedirectableExpression e : orExpression.things) {
			if(e.astOverrideOperatorOverload != null){
				if(!neworList.isEmpty()){
					soFar = convertOrToAnd(new OrExpression(e.astOverrideOperatorOverload.getLine(), e.astOverrideOperatorOverload.getColumn(), soFar, neworList));
					neworList.clear();
				}
				soFar = new OrExpression(e.astOverrideOperatorOverload.getLine(), e.astOverrideOperatorOverload.getColumn(), soFar, e);
				
				soFar.setTaggedType(e.astOverrideOperatorOverload.getTaggedType());
			}
			else{
				neworList.add(e);
			}
		}//we first get this into or able form (i.e. wrap together the operator overloaded components
		
		//now we conver this into aand form
		if(!neworList.isEmpty()){
			soFar = new NotExpression(orExpression.getLine(), orExpression.getColumn(), soFar);
			ArrayList<RedirectableExpression> ands = new ArrayList<RedirectableExpression>();
			for (RedirectableExpression e : neworList) {
				
				RedirectableExpression item = new RedirectableExpression(new NotExpression(e.getLine(), e.getColumn(), e));
				item.setTaggedType(Const_PRIM_BOOl);
				
				ands.add(item);
			}
			
			return new NotExpression(orExpression.getLine(), orExpression.getColumn(), new AndExpression(orExpression.getLine(), orExpression.getColumn(), soFar, ands));
		}
		else{
			return soFar; 
		}
		
	}

	@Override
	public Object visit(EqReExpression equalityExpression) {
		if(resolvedViaFoldedConstant(equalityExpression.getFoldedConstant(), equalityExpression.getTaggedType())){
			return equalityExpression.getTaggedType(); 
		}
		// throw new
		// RuntimeException("EqualityExpression should be called via processBooleanOperation");
		return headlessBooleanOperation(equalityExpression);
	}

	private Object headlessBooleanOperation(Expression e) {// just returns true
															// or false
		Label ifFalse = new Label();
		Label carryOn = new Label();

		return processFalseCarryOnBooleanOperation(e, ifFalse, carryOn, false);
	}

	private Object processFalseCarryOnBooleanOperation(Expression e, Label ifFalse, Label carryOn, boolean visitLabel) {
		Object ret = processBooleanOperation(e, ifFalse, false);
		// iftrue...
		if(ret.equals(Const_PRIM_BOOl)){
			bcoutputter.visitInsn(ICONST_1);
			bcoutputter.visitJumpInsn(GOTO, carryOn);
			bcoutputter.visitLabel(ifFalse);
			// false
			bcoutputter.visitInsn(ICONST_0);

			if (visitLabel) {
				bcoutputter.visitLabel(carryOn);
			} else {
				bcoutputter.pushNextLabel(carryOn);
			}
		}

		return ret;
	}

	private boolean isEQREExpr(Expression top) {
		return top instanceof EqReExpression || top instanceof AndExpression;
	}

	private Object processBooleanOperation(Expression top, Label ifFalse, boolean isEntry) {
		return processBooleanOperation(top, ifFalse, isEntry, false);
	}
		
		private Object processBooleanOperation(Expression top, Label ifFalse, boolean isEntry, boolean expectBoolean) {
		Type lhstype =null;

		if (top instanceof OrExpression) {
			top = convertOrToAnd((OrExpression) top);
		}

		if (isEQREExpr(top)) {
			if (top instanceof AndExpression) {
				AndExpression andExpression = (AndExpression) top;
				
				RedirectableExpression nextAndItem = andExpression.things.get(0);
				
				if(nextAndItem.astOverrideOperatorOverload != null){
					lhstype = (Type)andExpression.head.accept(this);
				}
				else{
					if (isEQREExpr(andExpression.head)) {// nested
						Label localfalse = new Label();
						Label localcarryOn = new Label();
						lhstype = (Type) processFalseCarryOnBooleanOperation(andExpression.head, localfalse, localcarryOn, true);
					} else {
						lhstype = (Type) processBooleanOperation(andExpression.head, ifFalse, false, true);
					}

					bcoutputter.visitJumpInsn(IFEQ, ifFalse);
				}
				
				lhstype = (Type)lhstype.copy();
				TypeCheckUtils.unlockAllNestedRefs(lhstype);
				
				Utils.unbox(bcoutputter, lhstype, this);
				
				Label localcarryOn = null;
				for (int n = 0; n < andExpression.things.size(); n++) {
					RedirectableExpression nextItem = andExpression.things.get(n);

					if (null != localcarryOn) {
						this.bcoutputter.visitLabel(localcarryOn);
					}
					
					Type expected = nextAndItem.astOverrideOperatorOverload == null ? Const_PRIM_BOOl : nextItem.getTaggedType();
					
					if (isEQREExpr(nextItem)) {
						lhstype = (Type) headlessBooleanOperation(nextItem);
					} else {
						lhstype = (Type) processBooleanOperation(nextItem, ifFalse, false, isBoolean(expected));
					}
					
					lhstype = (Type)lhstype.copy();
					TypeCheckUtils.unlockAllNestedRefs(lhstype);
					
					lhstype = Utils.applyCastImplicit(this.bcoutputter, lhstype, expected, this);//Utils.unbox(mv, topOfStackLoc, this);
					if(isBoolean(expected)){
						bcoutputter.visitJumpInsn(IFEQ, ifFalse);
					}
					
				}
			} else if (top instanceof EqReExpression) {
				// TODO: finnish unifying the logic of these two...
				EqReExpression eq = (EqReExpression) top;
				int currentlyProcessingNullOp = -1;
				if (isEQREExpr(eq.head)) {
					Label localfalse = new Label();
					Label localcarryOn = new Label();
					lhstype = (Type) processFalseCarryOnBooleanOperation(eq.head, localfalse, localcarryOn, true);
				} else {
					GrandLogicalElement nextEE = eq.elements.get(0);
					GrandLogicalOperatorEnum op = nextEE.compOp;
					Expression nextExpr = nextEE.e2;
					if ((nextExpr instanceof VarNull || eq.head instanceof VarNull) && (op == GrandLogicalOperatorEnum.EQ || op == GrandLogicalOperatorEnum.NE)) {
						currentlyProcessingNullOp = op == GrandLogicalOperatorEnum.EQ ? IFNONNULL : IFNULL;
						if (eq.head instanceof VarNull) {
							lhstype = (Type) eq.head;
						} else {
							lhstype = (Type) processBooleanOperation(eq.head, ifFalse, false);
						}
					} else {
						lhstype = (Type) processBooleanOperation(eq.head, ifFalse, false);		
						
						if(op != GrandLogicalOperatorEnum.REFEQ && op != GrandLogicalOperatorEnum.REFNE){
							TypeCheckUtils.unlockAllNestedRefs(lhstype);
						}
					}
				}

				if (TypeCheckUtils.hasRefLevelsAndNotLocked(lhstype)) {
					lhstype = Utils.unref(bcoutputter, lhstype, this);
				}

				Label localcarryOn = null;
				for (int n = 0; n < eq.elements.size(); n++) {
					GrandLogicalElement ee = eq.elements.get(n);
					boolean isLast = n == eq.elements.size() - 1;
					if(ee.astOverrideOperatorOverload != null){
						ee.astOverrideOperatorOverload.accept(this);
						
						Label l1 = isLast?ifFalse:new Label();
						
						switch(ee.compOp){
							case GT: bcoutputter.visitJumpInsn(IFLE, l1); break;
							case GTEQ: bcoutputter.visitJumpInsn(IFLT, l1); break;
							case LT: bcoutputter.visitJumpInsn(IFGE, l1); break;
							case LTEQ: bcoutputter.visitJumpInsn(IFGT, l1); break;
						}
						if(!isLast){
							bcoutputter.visitInsn(ICONST_1);
							Label l2 = new Label();
							bcoutputter.visitJumpInsn(GOTO, l2);
							bcoutputter.visitLabel(l1);
							bcoutputter.visitInsn(ICONST_0);
						}
						lhstype = Const_PRIM_BOOl;
						continue;
						//mv.visitJumpInsn(Opcodes.IFEQ, ifFalse);
						
					}
					
					Expression nextItem = ee.e2;
					GrandLogicalOperatorEnum op = ee.compOp;

					if (null != localcarryOn) {
						this.bcoutputter.visitLabel(localcarryOn);
					}

					if (currentlyProcessingNullOp != -1) {
						if (!(nextItem instanceof VarNull)) {
							if (isEQREExpr(nextItem)) {
								headlessBooleanOperation(nextItem);
							} else {
								processBooleanOperation(nextItem, ifFalse, false);
							}
						}

						bcoutputter.visitJumpInsn(currentlyProcessingNullOp, ifFalse);
						if (!isLast) {
							bcoutputter.visitInsn(ICONST_1);
						}
						lhstype = Const_PRIM_BOOl;
						currentlyProcessingNullOp = -1;
					} else if (isLast) {// last element
						boolean boxedAndNonEQ = TypeCheckUtils.isBoxedType(lhstype) && !(op == GrandLogicalOperatorEnum.EQ || op == GrandLogicalOperatorEnum.NE || op == GrandLogicalOperatorEnum.REFEQ || op == GrandLogicalOperatorEnum.REFNE);
						if (Utils.truePrimative(lhstype) || boxedAndNonEQ) {
							// upcast to correct type - use correct if comparison

							if (boxedAndNonEQ) {
								lhstype = Utils.unbox(bcoutputter, lhstype, this);
							}

							PrimativeTypeEnum lhsPrim = ((PrimativeType) lhstype).type;
							PrimativeTypeEnum rhsPrim = ((PrimativeType) TypeCheckUtils.unboxTypeIfBoxed(TypeCheckUtils.getRefType(nextItem.getTaggedType()))).type;
							
							PrimativeTypeEnum convertTo = PrimativeTypeEnum.INT;
							if(lhsPrim == PrimativeTypeEnum.FLOAT || rhsPrim == PrimativeTypeEnum.FLOAT) {
								convertTo = PrimativeTypeEnum.FLOAT;
							}else if(lhsPrim == PrimativeTypeEnum.DOUBLE || rhsPrim == PrimativeTypeEnum.DOUBLE) {
								convertTo = PrimativeTypeEnum.DOUBLE;
							}else if(lhsPrim == PrimativeTypeEnum.LONG || rhsPrim == PrimativeTypeEnum.LONG) {
								convertTo = PrimativeTypeEnum.LONG;
							}
							
							if(!lhsPrim.equals(convertTo)) {
								Utils.applyCastImplicit(this.bcoutputter, lhstype, new PrimativeType(convertTo), this);
							}
							
							Type rhstype;
							if (isEQREExpr(nextItem)) {
								rhstype = (Type) headlessBooleanOperation(nextItem);
							} else {
								rhstype = (Type) processBooleanOperation(nextItem, ifFalse, false);
								if(op != GrandLogicalOperatorEnum.REFEQ && op != GrandLogicalOperatorEnum.REFNE){
									TypeCheckUtils.unlockAllNestedRefs(rhstype);
								}
							}
							
							if(!rhstype.equals(new PrimativeType(convertTo))) {
								Utils.applyCastImplicit(this.bcoutputter, rhstype, new PrimativeType(convertTo), this);
							}
							
							
							
						//	PrimativeTypeEnum lhsPrim = ((PrimativeType) lhstype).type;
							//PrimativeTypeEnum rhsPrim = ((PrimativeType)lhstype).type;//((PrimativeType) Utils.applyCastImplicit(this.bcoutputter, rhstype, ((PrimativeType) lhstype), this)).type;

							
							
							Utils.applyComparisonOperationOnPrimative(bcoutputter, op, convertTo, convertTo, ifFalse);
							// JPT: here we cheat and cast the rhs to be the lhs type
							// when really we should find the more generic of
							// the two and back cast the first one if needs
							// be...
						} else {// TODO: box up rhs if its a primative, x == new
								// Boolean(False) ??
								// TODO: gennerate code corresponding to array
								// eq not eq
							Label skipToTrue = new Label();

							if (op == GrandLogicalOperatorEnum.EQ || op == GrandLogicalOperatorEnum.NE) {
								int line = top.getLine();
								int col = top.getColumn();

								if (lhstype.hasArrayLevels()) {
									// oh loook it's an array, so invoke:
									// com.concurnas.lang.Equalifier.equals(lhs, rhs)
									Type rhstype;
									if (isEQREExpr(nextItem)) {
										rhstype = (Type) headlessBooleanOperation(nextItem);
									} else {
										rhstype = (Type) processBooleanOperation(nextItem, ifFalse, false);
									}
									bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/lang/Equalifier", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
									// TODO should this map to:
									// "([Ljava/lang/Object;[Ljava/lang/Object;)Z"
									// in some caes? who cares??

								} else {
									// a=topOfStack
									// b=thing u were comparing anyway
									// boolean f = null==a?b==null:a.equals(b);

									Label eqCheckFunc = new Label();

									// mv.visitInsn(DUP);
									String tempName = this.getTempVarName();
									int tempSlot = this.createNewLocalVar(tempName, Const_Object, true);

									Type got = (Type) nextItem.accept(this);
									TypeCheckUtils.unlockAllNestedRefs(got);
									got = Utils.unref(bcoutputter, got, this);
									got = Utils.box(bcoutputter, got);// if it's a prim

									String tempNameForExpr = this.getTempVarName();
									// mv.visitInsn(DUP);
									int tempSlotForExpr = this.createNewLocalVar(tempNameForExpr, got, true);

									bcoutputter.visitVarInsn(ALOAD, tempSlot);
									bcoutputter.visitJumpInsn(IFNONNULL, eqCheckFunc);
									// it's null... check comp is
									bcoutputter.visitVarInsn(ALOAD, tempSlotForExpr);
									if (op == GrandLogicalOperatorEnum.EQ) {
										bcoutputter.visitJumpInsn(IFNULL, skipToTrue);
										bcoutputter.visitJumpInsn(GOTO, ifFalse);
									} else {
										bcoutputter.visitJumpInsn(IFNONNULL, skipToTrue);
									}

									bcoutputter.visitLabel(eqCheckFunc);

									bcoutputter.visitVarInsn(ALOAD, tempSlot);
									bcoutputter.visitJumpInsn(IFNULL, ifFalse);// lol check for null again
									bcoutputter.visitVarInsn(ALOAD, tempSlot);

									FuncInvokeArgs args = new FuncInvokeArgs(line, col);
									// args.add(nextItem);
									bcoutputter.visitVarInsn(ALOAD, tempSlotForExpr);

									FuncInvoke eqFunc = new FuncInvoke(line, col, "equals", args);
									String bcName = lhstype.getBytecodeType();
									bcName = lhstype instanceof PrimativeType && lhstype.hasArrayLevels() ? "java/lang/Object" : bcName.substring(1, bcName.length() - 1);

									ArrayList<Type> eqFuncInputs = new ArrayList<Type>(1);
									eqFuncInputs.add(Const_Object);
									FuncType eqFuncType = new FuncType(eqFuncInputs, Const_PRIM_BOOl);
									TypeAndLocation eqTAL = new TypeAndLocation(eqFuncType, new ClassFunctionLocation(bcName, lhstype));
									eqFunc.resolvedFuncTypeAndLocation = eqTAL;
									eqFunc.setTaggedType(eqFuncType);
									eqFunc.setPreceededByThis(true);
									this.dorOpLHS.push(Const_Object);
									eqFunc.accept(this);
									this.dorOpLHS.pop();

								}
								// if false...
								// JPT: god... this next line of code is a sin..
								// you already have the boolean, so why have all
								// the jump logic at all?
								// meh jvm will optimize it out... i hope

								//
								if (op == GrandLogicalOperatorEnum.EQ) {// map == to .equals
									bcoutputter.visitJumpInsn(Opcodes.IFEQ, ifFalse);
								} else {
									bcoutputter.visitJumpInsn(Opcodes.IFNE, ifFalse);
								}
								bcoutputter.pushNextLabel(skipToTrue);

							} else {
								Type got;
								if (isEQREExpr(nextItem)) {
									got = (Type) headlessBooleanOperation(nextItem);
								} else {
									got = (Type) processBooleanOperation(nextItem, ifFalse, false);
								}
								
								if(op != GrandLogicalOperatorEnum.REFEQ && op != GrandLogicalOperatorEnum.REFNE){
									TypeCheckUtils.unlockAllNestedRefs(got);
								}
								
								Utils.unref(bcoutputter, got, this);

								if (op == GrandLogicalOperatorEnum.REFEQ) {// ==
									bcoutputter.visitJumpInsn(IF_ACMPNE, ifFalse);
								} else if (op == GrandLogicalOperatorEnum.REFNE) {// <>
									bcoutputter.visitJumpInsn(IF_ACMPEQ, ifFalse);
								}
							}
						}

					} else {// element in a continuation of many...
							// TODO: test this
						Label localfalse = new Label();
						if (Utils.truePrimative(lhstype)) {
							Type rhstype;
							if (isEQREExpr(nextItem)) {
								rhstype = (Type) headlessBooleanOperation(nextItem);
							} else {
								rhstype = (Type) processBooleanOperation(nextItem, ifFalse, false);
							}

							rhstype = Utils.unbox(bcoutputter, rhstype, this);

							PrimativeTypeEnum lhsPrim = ((PrimativeType) lhstype).type;
							PrimativeTypeEnum rhsPrim = ((PrimativeType) rhstype).type;
							Utils.applyComparisonOperationOnPrimative(bcoutputter, op, lhsPrim, rhsPrim, localfalse);
							// JPT: here we cheat and  cast the rhs to be the lhs type
						} else {
							if (op == GrandLogicalOperatorEnum.EQ || op == GrandLogicalOperatorEnum.NE) {
								int line = top.getLine();
								int col = top.getColumn();
								FuncInvokeArgs args = new FuncInvokeArgs(line, col);
								args.add(nextItem);
								FuncInvoke eqFunc = new FuncInvoke(line, col, "equals", args);
								String bcName = lhstype.getBytecodeType();
								bcName = lhstype instanceof PrimativeType && lhstype.hasArrayLevels() ? "java/lang/Object" : bcName.substring(1, bcName.length() - 1);

								ArrayList<Type> eqFuncInputs = new ArrayList<Type>(1);
								eqFuncInputs.add(Const_Object);
								FuncType eqFuncType = new FuncType(eqFuncInputs, Const_PRIM_BOOl);
								TypeAndLocation eqTAL = new TypeAndLocation(eqFuncType, new ClassFunctionLocation(bcName, lhstype));
								eqFunc.resolvedFuncTypeAndLocation = eqTAL;
								eqFunc.setTaggedType(eqFuncType);

								eqFunc.accept(this);

								if (op == GrandLogicalOperatorEnum.EQ) {// map == to .equals
									bcoutputter.visitJumpInsn(Opcodes.IFEQ, localfalse);
								} else {
									bcoutputter.visitJumpInsn(Opcodes.IFNE, localfalse);
								}
							} else {
								if (isEQREExpr(nextItem)) {
									headlessBooleanOperation(nextItem);
								} else {
									processBooleanOperation(nextItem, localfalse, false);
								}

								if (op == GrandLogicalOperatorEnum.REFEQ) {// ==
									bcoutputter.visitJumpInsn(IF_ACMPNE, localfalse);
								} else if (op == GrandLogicalOperatorEnum.REFNE) {// <>
									bcoutputter.visitJumpInsn(IF_ACMPEQ, localfalse);
								}
							}
						}
						localcarryOn = new Label();
						bcoutputter.visitInsn(ICONST_1);
						bcoutputter.visitJumpInsn(GOTO, localcarryOn);
						bcoutputter.visitLabel(localfalse);
						bcoutputter.visitInsn(ICONST_0);

					}

					lhstype = Const_PRIM_BOOl;
				}
			}// RelationalExpression follow same logic

		} else if (isEntry) {// e.g. if(true){}...
			convertToBoolean((Type) top.accept(this), ifFalse);
			lhstype = Const_PRIM_BOOl;
			
			bcoutputter.visitJumpInsn(IFEQ, ifFalse);
		} else {
			if(expectBoolean){//this param is a bit of a hack, used for and and or's which expect lhs and rhs to be boolean
				convertToBoolean((Type) top.accept(this), ifFalse);
				lhstype = Const_PRIM_BOOl;
			}
			else{
				lhstype = (Type) top.accept(this);
			}
			
		}

		return lhstype;
	}
		
	private static boolean isBoolean(Type inp){
		/*if(inp == null){
			return true;
		}
		*/
		return inp.equals(ScopeAndTypeChecker.const_boolean) || inp.equals(ScopeAndTypeChecker.const_boolean_nt);
	}

	private int convertToBoolean(Type tt, Label ifFalse){
		tt = (Type)tt.copy();
		TypeCheckUtils.unlockAllNestedRefs(tt);
		donullcheckForUnknown(tt, null);
		int tempSlot=-1;//not always is tempSlot used
		if(!tt.equals(Const_PRIM_BOOl)){
			if(tt.hasArrayLevels()){//array
				//check null and check length
				bcoutputter.visitInsn(DUP);
				tempSlot = this.createNewLocalVar(this.getTempVarName(), tt, true);//JPT: dont like having to do this
				bcoutputter.visitJumpInsn(IFNULL, ifFalse);
				Utils.applyLoad(bcoutputter, tt, tempSlot);
				bcoutputter.visitInsn(ARRAYLENGTH);
				bcoutputter.visitJumpInsn(IFLE, ifFalse);
				bcoutputter.visitInsn(ICONST_1);
			}
			else{
				if(TypeCheckUtils.hasRefLevels(tt)){
					tt = Utils.unref(bcoutputter, tt, this);//incase contains null
					bcoutputter.visitInsn(DUP);
					int anotherTempSlot = this.createNewLocalVar(this.getTempVarName(), tt, true);//JPT: dont like having to do this
					bcoutputter.visitJumpInsn(IFNULL, ifFalse);
					bcoutputter.visitVarInsn(ALOAD, anotherTempSlot);
				}
				
				if(TypeCheckUtils.isBoxedType(tt)) {//do null check on boxed type
					bcoutputter.visitInsn(DUP);
					tempSlot = this.createNewLocalVar(this.getTempVarName(), tt, true);
					bcoutputter.visitJumpInsn(IFNULL, ifFalse);
					Utils.applyLoad(bcoutputter, tt, tempSlot);
				}
				
				if(tt instanceof NamedType || tt instanceof GenericType) {//otherwise just do the null check...
					Type tta = Utils.unbox(bcoutputter, tt, this);
					if(null!=tta) {
						tt = tta;
					}
				}
				
				/*PrimativeType unboxed = 
				
				if(null != unboxed){
					tt = unboxed;
				}*/
				
				if(tt instanceof PrimativeType){//primative other than boolean
					switch(((PrimativeType) tt).type){
						case INT:    bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/lang/ToBoolean", "toBoolean", "(I)Z"); break;
						case LONG:   bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/lang/ToBoolean", "toBoolean", "(J)Z"); break;
						case SHORT:  bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/lang/ToBoolean", "toBoolean", "(S)Z"); break;
						case FLOAT:  bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/lang/ToBoolean", "toBoolean", "(F)Z"); break;
						case DOUBLE: bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/lang/ToBoolean", "toBoolean", "(D)Z"); break;
						case BYTE:   bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/lang/ToBoolean", "toBoolean", "(B)Z"); break;
						case CHAR:   bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/lang/ToBoolean", "toBoolean", "(C)Z"); break;
					}
				}
				else{
					//object
					String bcType = tt.getBytecodeType();
					String owner = bcType.substring(1,  bcType.length()-1);
					
					if(owner.equals("java/lang/Object")){//special case if object, slightly slower because we are testing arrayness
						owner="com/concurnas/bootstrap/runtime/cps/CObject";//all objects are CObject's anyway
						
						bcoutputter.visitInsn(DUP);
						tempSlot = this.createNewLocalVar(this.getTempVarName(), tt, true);//JPT: dont like having to do this
						bcoutputter.visitJumpInsn(IFNULL, ifFalse);
						Utils.applyLoad(bcoutputter, tt, tempSlot);
						
						bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
						bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "isArray", "()Z");
						Label notArray = new Label();
						bcoutputter.visitJumpInsn(IFEQ, notArray);

						//its an array...
						Utils.applyLoad(bcoutputter, tt, tempSlot);
						
						bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/reflect/Array", "getLength", "(Ljava/lang/Object;)I");
						bcoutputter.visitJumpInsn(IFLE, ifFalse);//fail
						
						Label ok = new Label();
						bcoutputter.visitInsn(ICONST_1);
						bcoutputter.visitJumpInsn(GOTO, ok);
						
						bcoutputter.visitLabel(notArray);
						//..its not an array
						Utils.applyLoad(bcoutputter, tt, tempSlot);
						bcoutputter.visitTypeInsn(CHECKCAST, owner);
						bcoutputter.visitMethodInsn(INVOKEVIRTUAL, owner, "toBoolean", "()Z");
						bcoutputter.visitLabel(ok);
					}
					else{//an object that is not referenced as Object
						bcoutputter.visitInsn(DUP);
						tempSlot = this.createNewLocalVar(this.getTempVarName(), tt, true);//JPT: dont like having to do this
						bcoutputter.visitJumpInsn(IFNULL, ifFalse);
						Utils.applyLoad(bcoutputter, tt, tempSlot);
						
						if(tt instanceof NamedType && (((NamedType)tt).isInterface()) ){
							owner="com/concurnas/bootstrap/runtime/cps/CObject";//all objects are CObject's anyway, so when called on interface, cast to CObject
							bcoutputter.visitTypeInsn(CHECKCAST, owner);
							bcoutputter.visitMethodInsn(INVOKEVIRTUAL, owner, "toBoolean", "()Z");
						}else{
							bcoutputter.visitMethodInsn(INVOKEVIRTUAL, owner, "toBoolean", "()Z");
						}
						
					}
				}
			}
		}
		
		return tempSlot;
	}
	
	@Override
	public Object visit(LineHolder lineHolder) {
		this.topOfStack = null;
		if (bcoutputter != null) {
			bcoutputter.visitNewLine(lineHolder.l.getLine());
		}
		lastStatementTouched = lineHolder.l;
		
		return lineHolder.l.accept(this);
	}

/*	@Override
	public Object visit(IfExpr ifExpr) {
		if(resolvedViaFoldedConstant(ifExpr.getFoldedConstant(), ifExpr.getTaggedType())){
			return ifExpr.getTaggedType(); 
		}
		
		Type got = ifExpr.getTaggedType();

		Label ifFalse = new Label();
		Label carryOn = new Label();// ifExpr.getLabelAfterCode();

		convertToBoolean((Type)ifExpr.test.accept(this), ifFalse);
		
		bcoutputter.visitJumpInsn(IFEQ, ifFalse);// eq to zero

		// iftrue...
		Type op1Type = (Type) ifExpr.op1.accept(this);
		Utils.applyCastImplicit(bcoutputter, op1Type, got, this);

		bcoutputter.visitJumpInsn(GOTO, carryOn);
		bcoutputter.visitLabel(ifFalse);

		Type op2Type = (Type) ifExpr.op2.accept(this);
		Utils.applyCastImplicit(bcoutputter, op2Type, got, this);

		if(!bcoutputter.nextLabel.isEmpty()) {
			bcoutputter.visitLabel(bcoutputter.nextLabel.pop());
		}
		
		bcoutputter.visitLabel(carryOn);

		return got;
	}*/

	/*
	 * @Override public Object visit(IfStatement ifStatement) { Label
	 * afterTheStatement = null;//cont after the end of it all Label onFail;
	 * 
	 * if(ifStatement.elseb != null || !ifStatement.elifunits.isEmpty()){
	 * afterTheStatement =
	 * ifStatement.preAssignNextLabelVisit!=null?ifStatement.
	 * preAssignNextLabelVisit:new Label(); onFail= new Label(); } else
	 * if(ifStatement.preAssignNextLabelVisit!=null){ onFail =
	 * ifStatement.preAssignNextLabelVisit; } else{ onFail = new Label(); }
	 * 
	 * 
	 * processBooleanOperation(ifStatement.iftest, onFail, true);
	 * ifStatement.ifblock.accept(this); if(null != afterTheStatement &&
	 * !ifStatement.ifblock.hasDefoReturnedOrThrownException() ){
	 * mv.visitJumpInsn(GOTO, afterTheStatement); }
	 * 
	 * if(!ifStatement.elifunits.isEmpty() || ifStatement.elseb != null) {
	 * 
	 * if(!ifStatement.elifunits.isEmpty()) { mv.visitLabel(onFail);
	 * 
	 * ArrayList<ElifUnit> elifs = ifStatement.elifunits; int sz = elifs.size();
	 * for(int n = 0; n < sz; n++) { ElifUnit elif = elifs.get(n); onFail = new
	 * Label();
	 * 
	 * Label preVisitElifTest = elif.getLabelToVisitBeforeThisNode();
	 * 
	 * if(null != preVisitElifTest){ mv.pushNextLabel(preVisitElifTest); }
	 * 
	 * processBooleanOperation(elif.eliftest, onFail, true);
	 * elif.elifb.accept(this); if(null != afterTheStatement &&
	 * !elif.elifb.defoReturns){ mv.visitJumpInsn(GOTO, afterTheStatement); }
	 * 
	 * if(sz != n-1) {//all but last one mv.visitLabel(onFail); } } }
	 * 
	 * if(ifStatement.elseb != null) { mv.visitLabel(onFail);
	 * ifStatement.elseb.accept(this); //carryOn = new Label(); }
	 * 
	 * } if(null != afterTheStatement ){
	 * if(!afterTheStatement.equals(ifStatement.preAssignNextLabelVisit)){//
	 * mv.pushNextLabel(afterTheStatement); }
	 * 
	 * } else{ if(!onFail.equals(ifStatement.preAssignNextLabelVisit)){//
	 * mv.pushNextLabel(onFail); }
	 * 
	 * }
	 * 
	 * return null; }
	 */

	@Override
	public Object visit(IfStatement ifStatement) {
		// NOTE: changes here need to be applied to PreBytecodeLabelAllocator as
		// well as TCBranchAllocator
		boolean hasElse = ifStatement.elseb != null && !ifStatement.elseb.isEmpty();
		boolean hasElif = !ifStatement.elifunits.isEmpty();

		// int presistResultOnStack = -1;
		Type expected = ifStatement.getTaggedType();

		/*
		 * if(ifStatement.getShouldBePresevedOnStack()){ String tempName =
		 * this.getTempVarName(); //presistResultOnStack =
		 * this.createNewLocalVar(tempName, Const_PRIM_INT, false); }
		 */

		processBooleanOperation(ifStatement.iftest, ifStatement.onFailIfCheckLabel, true);
		Type ifBlockType = (Type) ifStatement.ifblock.accept(this);

		if (ifStatement.getShouldBePresevedOnStack() && !ifStatement.ifblock.hasDefoBrokenOrContinued) {
			if (!expected.equals(ifBlockType)) {
				Utils.applyCastImplicit(bcoutputter, ifBlockType, expected, this);
			}
			// Utils.applyStore(mv, expected, presistResultOnStack);
		}

		if ((hasElse || hasElif) && !ifStatement.ifblock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()) {
			if (!(lastStatementTouched instanceof BreakStatement || lastStatementTouched instanceof ContinueStatement)) {
				bcoutputter.visitJumpInsn(GOTO, ifStatement.onComplete);
			}
		}

		if (hasElif || hasElse) {
			if (hasElif) {
				ArrayList<ElifUnit> elifs = ifStatement.elifunits;
				int sz = elifs.size();
				for (int n = 0; n < sz; n++) {
					ElifUnit elif = elifs.get(n);
					boolean isLast = n == sz - 1;

					bcoutputter.visitLabel(elif.labelOnCacthEntry);

					processBooleanOperation(elif.eliftest, elif.nextLabelOnCatchEntry, true);
					Type elifT = (Type) elif.elifb.accept(this);

					if (ifStatement.getShouldBePresevedOnStack() && !elif.elifb.hasDefoBrokenOrContinued) {
						if (!expected.equals(elifT)) {
							Utils.applyCastImplicit(bcoutputter, elifT, expected, this);
						}
						// Utils.applyStore(mv, expected, presistResultOnStack);
					}

					if (!(isLast && !hasElse) && !elif.elifb.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()) {

						if (!(lastStatementTouched instanceof BreakStatement || lastStatementTouched instanceof ContinueStatement)) {
							bcoutputter.visitJumpInsn(GOTO, ifStatement.onComplete);
						}
					}
				}
			}
			if (hasElse) {
				bcoutputter.visitLabel(ifStatement.elseLabelOnEntry);
				Type elseType = (Type) ifStatement.elseb.accept(this);

				if (ifStatement.getShouldBePresevedOnStack() && !ifStatement.elseb.hasDefoBrokenOrContinued) {
					if (!expected.equals(elseType)) {
						Utils.applyCastImplicit(bcoutputter, elseType, expected, this);
					}
					
					if(!bcoutputter.nextLabel.isEmpty()) {
						bcoutputter.visitLabel(bcoutputter.nextLabel.pop());
					}
				}
			}
		}

		if(null != ifStatement.getLabelAfterCode() /*&& ifStatement.getShouldBePresevedOnStackAndImmediatlyUsed()*/) {
			
			boolean pushLabel = ifStatement.getShouldBePresevedOnStackAndImmediatlyUsed();
			if(!pushLabel) {
				if(ifStatement.elseb != null && !ifStatement.elseb.isEmpty()) {
					pushLabel=false;//ifStatement.elseb.getShouldBePresevedOnStack();
				}else if(!ifStatement.elifunits.isEmpty()) {
					pushLabel=true;//ifStatement.elifunits.get(0).elifb.getShouldBePresevedOnStack();
				}
			}
			
			if(pushLabel) {
				// perform 'early' visit here such that code such as this will work
				// ok: a= if(4>3){12}
				Label l = ifStatement.getLabelAfterCode();
				bcoutputter.pushNextLabel(l);
				ifStatement.setLabelAfterCode(null);
			}
		}

		return ifStatement.getTaggedType();
	}

	@Override
	public Object visit(ElifUnit elifUnit) {
		return null; // null here is ok
	}

	@Override
	public Object visit(PowOperator powOperator) {
		if(!resolvedViaFoldedConstant(powOperator.getFoldedConstant(), ScopeAndTypeChecker.const_double)){
			Type lhs = (Type) powOperator.expr.accept(this);
			lhs = (Type)lhs.copy(); TypeCheckUtils.unlockAllNestedRefs(lhs);
			
			Utils.applyCastImplicit(bcoutputter, lhs, ScopeAndTypeChecker.const_double, this);
			Type rhs = (Type) powOperator.raiseTo.accept(this);
			rhs = (Type)rhs.copy(); TypeCheckUtils.unlockAllNestedRefs(rhs);
			
			Utils.applyCastImplicit(bcoutputter, rhs, ScopeAndTypeChecker.const_double, this);
			bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "pow", "(DD)D");
		}
		
		Type methReturned = Const_PRIM_DOUBLE;
		Type expected = powOperator.getTaggedType();

		if (!methReturned.equals(expected)) {// TODO: no prime i.e. new
												// Integer(3) ** new Double(4)
												// -> goes to Number or
												// something?
			Utils.applyCastImplicit(this.bcoutputter, methReturned, expected, this);
		}

		return expected;
	}

	// private Stack<Label> owhileCompleted = new Stack<Label>();
	// private Stack<Label> owhileStart = new Stack<Label>();

	/*
	 * private ContinueOrBreak takeNormalBreakContinueFromEnd(Block blk){
	 * ContinueOrBreak ret = null; if(!blk.lines.isEmpty()){ int lastLine =
	 * blk.lines.size()-1; Line l = blk.lines.get(lastLine).l; if(l instanceof
	 * ContinueStatement){ ContinueStatement c = (ContinueStatement)l; //if(null
	 * == c.returns){ blk.lines.remove(lastLine); ret = c; //} } else if(l
	 * instanceof BreakStatement){ BreakStatement c = (BreakStatement)l;
	 * //if(null == c.returns){ blk.lines.remove(lastLine); ret = c; //} } }
	 * return ret; }
	 */

	private Stack<Integer> slotForBreakContinueInLoop = new Stack<Integer>();

	private void sneakInBeforeLastContinue(Block blk, PostfixOp toSneak){
		
		if (!blk.lines.isEmpty()) {
			int lastLine = blk.lines.size() - 1;
			Line l = blk.lines.get(lastLine).l;
			int line = toSneak.getLine();
			int col = toSneak.getColumn();
			LineHolder toAdd = new LineHolder(line, col, new DuffAssign(line, col,toSneak));
			if (l instanceof ContinueStatement) {
				blk.lines.add(lastLine, toAdd);
			}
			else{
				blk.lines.add(toAdd);
			}
		}
		
		
	}
	
	@Override
	public Object visit(WhileBlock whileBlock) {
		
		Label afterElseBlock = whileBlock.elseblock != null?new Label():null;
		
		Type retType = whileBlock.getTaggedType();

		Label onDone = whileBlock.getLabelAfterCode();

		int slotForVisitCounterBool=-1;
		if(afterElseBlock != null){
			bcoutputter.visitInsn(ICONST_0);
			slotForVisitCounterBool = this.createNewLocalVar(this.getTempVarName(), Const_PRIM_BOOl, true);
		}
		
		if(null != whileBlock.idxVariableCreator){
			whileBlock.idxVariableCreator.accept(this);
		}
		
		int presistResultOnStack = -1;
		if (whileBlock.getShouldBePresevedOnStack()) {
			String tempName = this.getTempVarName();
			// new java.lang.LinkedList<String>();

			bcoutputter.visitTypeInsn(NEW, "java/util/LinkedList");
			bcoutputter.visitInsn(DUP);
			bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/util/LinkedList", "<init>", "()V");

			presistResultOnStack = this.createNewLocalVar(tempName, retType, true);
			slotForBreakContinueInLoop.push(presistResultOnStack);
		}

		if(whileBlock.idxVariableCreator != null || whileBlock.idxVariableAssignment != null){
			/*int line = whileBlock.getLine();
			int col = whileBlock.getColumn();
			PostfixOp postIdxIncremement = new PostfixOp(line, col, FactorPostFixEnum.PLUSPLUS, new RefName(line, col, whileBlock.idxVariableAssignment!=null?whileBlock.idxVariableAssignment.name:whileBlock.idxVariableCreator.name));
			postIdxIncremement.setShouldBePresevedOnStack(false);

			Label skipFirst = new Label();
			bcoutputter.visitJumpInsn(GOTO, skipFirst);
			bcoutputter.visitLabel( whileBlock.getLabelBeforeCondCheckIfStackPrese() );*/
			Label skipFirst = new Label();
			bcoutputter.visitJumpInsn(GOTO, skipFirst);
			bcoutputter.visitLabel( whileBlock.getLabelBeforeCondCheckIfStackPrese() );
			whileBlock.postIdxIncremement.accept(this);
			
			bcoutputter.visitLabel(skipFirst);
			
		}else if (whileBlock.getShouldBePresevedOnStack()) {
			bcoutputter.visitLabel(whileBlock.getLabelBeforeCondCheckIfStackPrese());
		}
		
		processBooleanOperation(whileBlock.cond, onDone, true);

		
		if(afterElseBlock != null){
			bcoutputter.visitInsn(ICONST_1);
			bcoutputter.visitVarInsn(ISTORE, slotForVisitCounterBool);
		}
		
		
		boolean didRet = false;
		if (presistResultOnStack > -1) {
			Block blk = whileBlock.block;
			ContinueStatement ret = null;
			if (!blk.lines.isEmpty()) {
				int lastLine = blk.lines.size() - 1;
				Line l = blk.lines.get(lastLine).l;
				if (l instanceof ContinueStatement) {
					ContinueStatement c = (ContinueStatement) l;
					if (c.isSynthetic) {// remove it from the end and process as normal
						blk.lines.remove(lastLine);
						ret = c;
					}
				}
			}
			Type blkType = (Type) blk.accept(this);

			if (ret != null || !whileBlock.block.hasDefoBrokenOrContinued) {
				bcoutputter.visitLabel(whileBlock.beforeAdder);
				blkType = Utils.unref(bcoutputter, blkType, TypeCheckUtils.getRefTypeToLocked(blkType), this);
				Type from = Utils.box(bcoutputter, blkType);
				Utils.applyLoad(bcoutputter, retType, presistResultOnStack);
				Utils.genericSswap(bcoutputter, from, retType);
				bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/util/LinkedList", "add", "(Ljava/lang/Object;)Z");
				bcoutputter.visitInsn(POP);
				// asdasdadasdasd
				
				if (ret != null) {
					ret.accept(this);
					didRet = true;
				}
			}
		} else {
			/*if(null != postIdxIncremement){
				sneakInBeforeLastContinue(whileBlock.block, postIdxIncremement);
			}*/
			whileBlock.block.accept(this);
		}
		
		if (!whileBlock.defoEndsInGotoStmtAlready /*&& !whileBlock.skipGotoStart*/) {
			bcoutputter.visitJumpInsn(GOTO, whileBlock.getLabelOnEntry());
		} else if (!whileBlock.block.hasDefoBrokenOrContinued && !didRet && whileBlock.getShouldBePresevedOnStack()) {
			bcoutputter.visitJumpInsn(GOTO, whileBlock.getLabelBeforeCondCheckIfStackPrese());// wtf?
		}
		
		
		if(afterElseBlock != null){
			bcoutputter.pushNextLabel(onDone);
			
			Label elseBlockEntry = new Label();
			bcoutputter.visitVarInsn(ILOAD, slotForVisitCounterBool);
			
			Label ifCount;
			if(presistResultOnStack > -1){
				onDone = new Label(); 
				ifCount = onDone;
			}
			else{
				ifCount = afterElseBlock;
			}
			
			bcoutputter.visitJumpInsn(IFNE, ifCount);
			whileBlock.elseblock.setLabelOnEntry(elseBlockEntry);
			bcoutputter.visitJumpInsn(GOTO, elseBlockEntry);	
		}
		

		if (presistResultOnStack > -1) {
			bcoutputter.pushNextLabel(onDone);
			Utils.applyLoad(bcoutputter, retType, presistResultOnStack);

			if(null != afterElseBlock){
				bcoutputter.visitJumpInsn(GOTO, afterElseBlock);	
			}
		}
		
		if(afterElseBlock != null){
			bcoutputter.visitLabel(whileBlock.elseblock.getLabelOnEntry());
			whileBlock.elseblock.accept(this); 
			bcoutputter.visitLabel(afterElseBlock);
		}
		
		
		/*
		 * if(whileBlock.getShouldBePresevedOnStackAndImmediatlyUsed()){
		 * mv.pushNextLabel(onDone); }
		 */

		return presistResultOnStack > -1?retType:null;
	}
	
	private void makeRefArray(Type consType, ArrayList<Expression> items) {
		int sz = items.size();
		Expression levelmake = items.remove(0);
		Utils.unbox(this.bcoutputter, (Type) levelmake.accept(this), this);

		bcoutputter.visitInsn(DUP);
		int isRefTypeTempSizeSlot = this.createNewLocalVar(this.getTempVarName(), ScopeAndTypeChecker.const_int, true);

		//bcoutputter.visitTypeInsn(ANEWARRAY, consType.getBytecodeType());
		//ANEWARRAY com/concurnas/runtime/ref/Local
		
		{
			Type tt = (Type)consType.copy();
			tt.setArrayLevels(sz);
			Utils.createNakedArray(this.bcoutputter, 1, tt);
		}
		
		
		
		Type compoentType = (Type)consType.copy();
		compoentType.setArrayLevels(compoentType.getArrayLevels()-1);

		bcoutputter.visitInsn(DUP);
		int arraySlot = this.createNewLocalVar(this.getTempVarName(), consType, true);
		
		{
			Label l22 = new Label();
			bcoutputter.visitLabel(l22);
			
			bcoutputter.visitInsn(ICONST_0);
			int counter = this.createNewLocalVar(this.getTempVarName(), ScopeAndTypeChecker.const_int, true);
			
			Label startFor = new Label();
			bcoutputter.visitJumpInsn(GOTO, startFor);
			Label startOfForBlody = new Label();
			bcoutputter.visitLabel(startOfForBlody);
			
			//arraySlot
			bcoutputter.visitVarInsn(ALOAD, arraySlot);
			bcoutputter.visitVarInsn(ILOAD, counter);
			
			if(sz == 1) {
				Utils.createRef(bcoutputter, compoentType, 0, false, false, -1);
			}else {
				makeRefArray(compoentType, items);
			}
			
			bcoutputter.visitInsn(AASTORE);
			
			
			Label l26 = new Label();
			bcoutputter.visitLabel(l26);
			bcoutputter.visitIincInsn(counter, 1);
			bcoutputter.visitLabel(startFor);
			
			bcoutputter.visitVarInsn(ILOAD, counter);
			bcoutputter.visitVarInsn(ILOAD, isRefTypeTempSizeSlot);
			bcoutputter.visitJumpInsn(IF_ICMPLT, startOfForBlody);
			
				//bcoutputter.visitVarInsn(ALOAD, arraySlot);
				Utils.createRefArray(bcoutputter, (NamedType) consType);
			
		}
		
	
	}
	
	

	@Override
	public Object visit(ArrayConstructor arrayConstructor) {
		Type consType = arrayConstructor.getTaggedType();
		int siz = arrayConstructor.arrayLevels.size();
		boolean isRefType = TypeCheckUtils.hasArrayRefLevels(consType);
		
		if(isRefType && siz > 1) {
			makeRefArray(consType, new ArrayList<Expression>(arrayConstructor.arrayLevels));
		}else {
			int nullcnt = 0;
			int isRefTypeTempSizeSlot=-1;
			for (Expression e : arrayConstructor.arrayLevels) {
				if(e == null){
					nullcnt++;
				}else{
					Utils.unbox(this.bcoutputter, (Type) e.accept(this), this);
					if(isRefType) {
						bcoutputter.visitInsn(DUP);
						isRefTypeTempSizeSlot = this.createNewLocalVar(this.getTempVarName(), ScopeAndTypeChecker.const_int, true);
					}
				}
			}
			
			Utils.createNakedArray(this.bcoutputter, siz-nullcnt, consType);

			if (isRefType) {
				Type compoentType = (Type)consType.copy();
				compoentType.setArrayLevels(compoentType.getArrayLevels()-1);
				
				if(isRefTypeTempSizeSlot > -1 ) {
					//for loop to create empty refs

					bcoutputter.visitInsn(DUP);
					int arraySlot = this.createNewLocalVar(this.getTempVarName(), consType, true);
					
					
					{
						Label l22 = new Label();
						bcoutputter.visitLabel(l22);
						
						bcoutputter.visitInsn(ICONST_0);
						int counter = this.createNewLocalVar(this.getTempVarName(), ScopeAndTypeChecker.const_int, true);
						
						Label startFor = new Label();
						bcoutputter.visitJumpInsn(GOTO, startFor);
						Label startOfForBlody = new Label();
						bcoutputter.visitLabel(startOfForBlody);
						
						//arraySlot
						bcoutputter.visitVarInsn(ALOAD, arraySlot);
						bcoutputter.visitVarInsn(ILOAD, counter);
						
						Utils.createRef(bcoutputter, compoentType, 0, false, false, -1);
						
						bcoutputter.visitInsn(AASTORE);
						
						
						Label l26 = new Label();
						bcoutputter.visitLabel(l26);
						bcoutputter.visitIincInsn(counter, 1);
						bcoutputter.visitLabel(startFor);
						
						bcoutputter.visitVarInsn(ILOAD, counter);
						bcoutputter.visitVarInsn(ILOAD, isRefTypeTempSizeSlot);
						bcoutputter.visitJumpInsn(IF_ICMPLT, startOfForBlody);
					}
				}
				
				Utils.createRefArray(bcoutputter, (NamedType) consType);
			}
		}
		
		if (!((Node) arrayConstructor).getShouldBePresevedOnStack()) {//no pop if void
			Utils.popFromStack(bcoutputter, consType);
		}

		return consType;
	}

	private boolean resolvedViaFoldedConstant(Object foldedConstant, Type expected){
		//can we skip runtime computation of the thing and use the folded constant instead?
		//TODO: may be beffer to avoid the implic cast here and just store thet thing as the value expected type anyway... [minor point]
		if(null != foldedConstant){
			if(foldedConstant instanceof String){
				bcoutputter.visitLdcInsn(foldedConstant);
				Utils.applyCastImplicit(bcoutputter, ScopeAndTypeChecker.const_string, expected, this);//JPT: possible?
				return true; 
			}
			else if(foldedConstant instanceof Boolean){
				bcoutputter.visitInsn(((Boolean)foldedConstant).booleanValue()?ICONST_1:ICONST_0);
				Utils.applyCastImplicit(bcoutputter, ScopeAndTypeChecker.const_boolean, expected, this);
				return true;
			}
			else if(foldedConstant instanceof Number){
				if(foldedConstant instanceof Long){
					Utils.longOpcode(this.bcoutputter, ((Number) foldedConstant).longValue());
					Utils.applyCastImplicit(bcoutputter, ScopeAndTypeChecker.const_long, expected, this);
				}
				else if(foldedConstant instanceof Float){
					Utils.floatOpcode(this.bcoutputter, ((Number) foldedConstant).floatValue());
					Utils.applyCastImplicit(bcoutputter, ScopeAndTypeChecker.const_float, expected, this);
				}
				else if(foldedConstant instanceof Double){
					Utils.doubleOpcode(this.bcoutputter, ((Number) foldedConstant).doubleValue());
					Utils.applyCastImplicit(bcoutputter, ScopeAndTypeChecker.const_double, expected, this);
				}
				else{//int
					Utils.intOpcode(this.bcoutputter, ((Number) foldedConstant).intValue());
					Utils.applyCastImplicit(bcoutputter, ScopeAndTypeChecker.const_int, expected, this);
				}
				return true; 
			}
		}
		return false;
	}
	
	@Override
	public Object visit(Additive addMinusExpression) {
		Type resTo = addMinusExpression.getTaggedType();
		if(resolvedViaFoldedConstant(addMinusExpression.getFoldedConstant(), addMinusExpression.getTaggedType())){
			return resTo; 
		}
		
		// JPT: refactor
		int stringConcatAtOp = getStringConcatIndex(addMinusExpression);

		if (stringConcatAtOp > 0) {
			StringBuffHelper.init(this);
		}

		Expression head = addMinusExpression.head;

		boolean headIsComoundStmt = head instanceof CompoundStatement && resTo.equals(ScopeAndTypeChecker.const_string);//weird way of doing things
		//JPT: can the above line be removed?

		boolean isFirst = true;

		Type lhs = addMinusExpression.headType;

		int idx = 0;

		boolean makeEmptyInitString = stringConcatAtOp > 0 && lhs.hasArrayLevels();

		if (makeEmptyInitString || headIsComoundStmt) {
			// MHA: i cant figure out how to gennerate the code with a start here so instead hack the init to take an empty string then
           // append the lhs i cannot make the thing work if the first arg is an arry! returns some thing about "Uninitialized object exists on backward branch..." need to fix this
			bcoutputter.visitLdcInsn("");
			bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
		}

		for (AddMinusExpressionElement i : addMinusExpression.elements) {
			idx++;
			
			boolean isPlusOperation = i.isPlus;
			Expression nextItem = i.exp;
			// Type rhs = nextItem.getTaggedType();
			Type rhs = i.getTaggedType();

			if (isFirst) {
				isFirst = false;
				Type got = (Type)((Type) head.accept(this)).copy();
				if(stringConcatAtOp == 0){//no str concat
					TypeCheckUtils.unlockAllNestedRefs(got);
				}

				if (!got.equals(lhs)) {
					Utils.applyCastImplicit(bcoutputter, got, lhs, this);
				}

			}

			if (stringConcatAtOp == 1 && idx == 1) {
				if (lhs.hasArrayLevels() || headIsComoundStmt) {
					// MHA: i cant figure out how to gennerate the code with a start here so instead hack the init to take an empty string then append the lhs StringBuffHelper.append(this, lhs);
					headIsComoundStmt = false;
					StringBuffHelper.append(this, lhs);
				} else {
					StringBuffHelper.start(this, lhs);
				}

				Type got = (Type) nextItem.accept(this);
				//TypeCheckUtils.unlockAllNestedRefs(got);
				got = Utils.unref(bcoutputter, got, this);

				StringBuffHelper.append(this, got);
			} else {
				if ((idx < stringConcatAtOp || stringConcatAtOp == 0))// no string concat crap
				{
					if(i.astOverrideOperatorOverload != null){
						i.astOverrideOperatorOverload.accept(this);
					}
					else{
						lhs = Utils.unbox(bcoutputter, lhs, this);

						Type thisOnesType = i.getTaggedType();

						if (!lhs.equals(thisOnesType)) {
							Utils.applyCastImplicit(bcoutputter, lhs, thisOnesType, this);
						}

						rhs = (Type)((Type) nextItem.accept(this)).copy();
						TypeCheckUtils.unlockAllNestedRefs(rhs);
						
						if (!rhs.equals(thisOnesType)) {
							Utils.applyCastImplicit(bcoutputter, rhs, thisOnesType, this);
						}

						Utils.applyPlusMinusPrim(this.bcoutputter, isPlusOperation, (PrimativeType) thisOnesType);
						lhs = thisOnesType;
					}
					
				}

				if (idx == stringConcatAtOp && idx != 1) {
					StringBuffHelper.start(this, lhs);
				}

				if (idx >= stringConcatAtOp && stringConcatAtOp > 0) {
					Type got = (Type) nextItem.accept(this);
					got = Utils.unref(bcoutputter, got, this);
					// if(!got.equals(rhs)){
					// Utils.applyCastImplicit(mv,got, rhs, this);
					// }
					// if(!nextLabel.isEmpty()){
					// Label label = nextLabel.pop();
					// mv.visitLabel(label);
					// }/

					StringBuffHelper.append(this, got);
				}
			}
		}

		if (stringConcatAtOp > 0) {
			StringBuffHelper.end(this);
			return new NamedType(new ClassDefJava(String.class));
		} else {
			return resTo;
		}
	}

	@Override
	public Object visit(ArrayRefElementPrefixAll arrayRefElementPrefixAll) {
		return null;
	}// null is ok

	@Override
	public Object visit(ArrayRefElementPostfixAll arrayRefElementPostfixAll) {
		return null;
	}// null is ok

	@Override
	public Object visit(ArrayRefElementSubList arrayRef) {
		return null;
	}// null is ok

	@Override
	public Object visit(ArrayRefElement arrayRefElement) {
		return null;
	}// null is ok

	@SuppressWarnings("incomplete-switch")
	private Object getDefaultValueForType(Type arType){
		if(arType instanceof PrimativeType){
			switch(((PrimativeType) arType).type){
				case BOOLEAN: return false;
				case INT: return 0;
				case LONG: return 0l;
				case FLOAT: return 0.f;
				case DOUBLE: return 0.;
				case SHORT: return (short)0;
				case BYTE: return (byte)0;
				case CHAR: return (char)0;
			}
		}
		return null;
	}
	
	@Override
	public Object visit(ArrayDef arrayDef) {
		Type ret = arrayDef.getTaggedType();
		if(!arrayDef.isArray && !ret.hasArrayLevels()) {//list
			/*ArrayList<Integer> xxx = new ArrayList<Integer>(10);
			xxx.add(1);
			xxx.add(2);
			xxx.add(3);*/
			ArrayList<Expression> arrayElements = arrayDef.getArrayElements(this);
			
			bcoutputter.visitTypeInsn(NEW, "java/util/ArrayList");
			bcoutputter.visitInsn(DUP);
			Utils.intOpcode(bcoutputter, arrayElements.size());
			bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "(I)V", false);
			
			Type wantedCompType = ((NamedType)ret).getGenericTypeElements().get(0);
			
			for(Expression ele : arrayElements) {
				bcoutputter.visitInsn(DUP);
				Utils.applyCastImplicit(bcoutputter, (Type)ele.accept(this), wantedCompType, this);
				bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "add", "(Ljava/lang/Object;)Z", false);
				bcoutputter.visitInsn(POP);
			}
			
			
			
			/*
		mv.visitTypeInsn(NEW, "java/util/ArrayList");
		mv.visitInsn(DUP);
		mv.visitIntInsn(BIPUSH, 10);
		mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "(I)V", false);
		mv.visitVarInsn(ASTORE, 3);
		mv.visitVarInsn(ALOAD, 3);
		mv.visitInsn(ICONST_1);
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "add", "(Ljava/lang/Object;)Z", false);
		mv.visitInsn(POP);
			 */
			
		}else {
			processArElements(arrayDef.getArrayElements(this), ret, false, arrayDef.isComplexAppendOp);
		}
		return ret;
	}
	
	
	private void processArElements(ArrayList<Expression> arrayElements, Type typewanted, boolean isComplexAraryDef, int iscomplexAppendOp){
		int size = arrayElements.size();
		boolean isconcatinateOp = isComplexAraryDef && size >= 2;
		boolean isComplexAppend = iscomplexAppendOp > -1;
		boolean isconcatOrAppendOP = isconcatinateOp || isComplexAppend;

		int arLevels = typewanted.getArrayLevels();

		if(!isconcatOrAppendOP){
			Utils.intOpcode(bcoutputter, size);

			Utils.createArray(this.bcoutputter, arLevels, typewanted, true);
		}
		

		Object defaultValueForType = getDefaultValueForType(typewanted);
		
		ArrayList<Thruple<Integer, Integer, Type>> concatops=null;//slot, levels
		
		if(isconcatOrAppendOP){
			concatops = new ArrayList<Thruple<Integer, Integer, Type>>(arrayElements.size());
		}
		
		for (int n = 0; n < size; n++) {
			Expression e = arrayElements.get(n);
			Object foldedConst = e.getFoldedConstant();
			
			if(foldedConst != null && defaultValueForType != null){//minor optimization to remove unecisary instantiation of default values
				if(defaultValueForType.equals(foldedConst)){
					continue;
				}
			}else if(e instanceof VarNull){
				continue;
			}
			
			
			if(!isconcatOrAppendOP){
				bcoutputter.visitInsn(DUP);
			}

			if (bcoutputter != null) {//having to do this explicitly is a bit nasty
				bcoutputter.visitNewLine(e.getLine());
			}
			
			if(!isconcatOrAppendOP){
				Utils.intOpcode(bcoutputter, n);
			}
			

			Type currentEleType = (Type) e.accept(this);
			
			//typewanted.setArrayLevels(arLevels - 1);//TODO: this a bit ugly, just copy with one less array level and use that instead, also next if stmt is ugly
			if (currentEleType.equals(typewanted)) {
				if (TypeCheckUtils.hasRefLevelsAndIsArray(currentEleType)) {
					// special case: a int:[] = [8! 8!]; z = [a a] //z is int:[2]
					bcoutputter.visitFieldInsn(GETFIELD, "com/concurnas/bootstrap/runtime/ref/LocalArray", "ar", "[Ljava/lang/Object;");
				}
			} else {
				typewanted.setArrayLevels(arLevels - 1);
				Utils.applyCastImplicit(bcoutputter, currentEleType, typewanted, this);
				typewanted.setArrayLevels(arLevels);
			}
			//typewanted.setArrayLevels(arLevels);
			
			
			
			
			if(!isComplexAraryDef && !isComplexAppend){
				Utils.applyArrayStore(bcoutputter, typewanted, this, true);
			}else if(isconcatOrAppendOP){
				//its a complex array, so concatinate as approperiate

				//store for now
				int tempSlot = this.createNewLocalVar(this.getTempVarName(), currentEleType, true);
				concatops.add(new Thruple<Integer, Integer, Type>(tempSlot, currentEleType.getArrayLevels(), currentEleType));
			}
		}
		
		
		if(isconcatOrAppendOP){
			/*if(nonNullElementCnt == 1){//only one is non null
				Utils.intOpcode(mv, nonNullElementCnt);
				Utils.createArray(this.mv, arLevels, typewanted, true);
				mv.visitInsn(DUP);
				Utils.intOpcode(mv, 0);
				mv.visitVarInsn(ALOAD, concatops.get(0).getA());
				Utils.applyArrayStore(mv, typewanted, this, true);
			}else*/{

				Type artyype = (Type)typewanted.copy();
				int wantedLevels = artyype.getArrayLevels();
				artyype.setArrayLevels(wantedLevels-1);
				if(iscomplexAppendOp > 1){//case for 1 adding is covered below and doing normal concat
					//horizontal append, slithly more involved
					handleHorizontalAppend(concatops, artyype);
				}else{//e.g. m=[1 2; 3 4]; [m m] or [m [1 2]] or [ [1 2 3 4] 5] covered by this: vertical concat
					Utils.intOpcode(bcoutputter, 0);
					int posCnter = this.createNewLocalVar(this.getTempVarName(), ScopeAndTypeChecker.const_int, true);//store result

					//figure out size...
					bcoutputter.visitInsn(ICONST_0);
					for(Thruple<Integer, Integer, Type> slotAndLength : concatops){
						if(slotAndLength.getB() == wantedLevels){
							bcoutputter.visitVarInsn(ALOAD, slotAndLength.getA());
							bcoutputter.visitInsn(ARRAYLENGTH);
						}else{
							bcoutputter.visitInsn(ICONST_1);
						}
						bcoutputter.visitInsn(IADD);
					}
					
					Utils.createArray(this.bcoutputter, typewanted.getArrayLevels(), typewanted, true);
					
					//add each element using assign or copy...
					int firstEQOne=-1;
					for(Thruple<Integer, Integer, Type> slotAndLength : concatops){
						int myslot = slotAndLength.getA();
						if(slotAndLength.getB() == wantedLevels){
							if(firstEQOne == -1){
								bcoutputter.visitInsn(Opcodes.DUP);
								firstEQOne = this.createNewLocalVar(this.getTempVarName(), typewanted, true);//store result
							}
								
							//System.arraycopy(two, 0, resutl, 1, 2);
							Utils.applyLoad(bcoutputter, slotAndLength.getC(), myslot);
							
							bcoutputter.visitInsn(ICONST_0);
							
							bcoutputter.visitVarInsn(ALOAD, firstEQOne);

							bcoutputter.visitVarInsn(ILOAD, posCnter);

							Utils.applyLoad(bcoutputter, slotAndLength.getC(), myslot);
							bcoutputter.visitInsn(ARRAYLENGTH);
							bcoutputter.visitInsn(Opcodes.DUP);
							bcoutputter.visitVarInsn(ILOAD, posCnter);//pos += length
							bcoutputter.visitInsn(IADD);
							bcoutputter.visitVarInsn(ISTORE, posCnter);
							
							bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", false);
							
						}else{//xxx[0] = one;
							bcoutputter.visitInsn(Opcodes.DUP);
							bcoutputter.visitVarInsn(ILOAD, posCnter);
							bcoutputter.visitIincInsn(posCnter, 1);
							Utils.applyLoad(bcoutputter, slotAndLength.getC(), myslot);
							Utils.applyArrayStore(bcoutputter, typewanted, this, true);
						}
					}
				}
			}
		}
		

		if (TypeCheckUtils.hasArrayRefLevels(typewanted)) {
			// oh it's a ref, in that case we need to wrap it inside container
			// so as to catch its generic type
			Utils.createRefArray(bcoutputter, (NamedType) typewanted);
		}
		
		/*if(isconcatOrAppendOP){
			int posCnter = this.createNewLocalVar(this.getTempVarName(), typewanted, true);//store result
			mv.visitLabel(new Label());
			mv.visitVarInsn(ALOAD, posCnter);
		}*/
	}
	
	private void handleHorizontalAppend(ArrayList<Thruple<Integer, Integer, Type>> concatops, Type artyype){
		//we know the dimentionality is the same...
		//TODO: assert all elements are same length
		
		/*
		int[][] res = new int[m1.length][];
		
		for(int n =0; n < m1.length; n++){
			int size = 0;
			size += m1[n].length;
			size += m2[n].length;

			int[] row = new int[size];
			int pos = 0;
			System.arraycopy(m1[n], 0, row, pos, m1[n].length);
			pos+=m1[n].length;
			System.arraycopy(m1[n], 0, row, pos, m2[n].length);
			pos+=m2[n].length;
			res[n] = row;
		}
		 */
		
		Type compType = (Type)artyype.copy();
		compType.setArrayLevels(compType.getArrayLevels() - 1);
		
		Thruple<Integer, Integer, Type> first = concatops.get(0);
		
		
		
		bcoutputter.visitVarInsn(ALOAD, first.getA());
		bcoutputter.visitInsn(ARRAYLENGTH);
		int compareTo = this.createNewLocalVar(this.getTempVarName(), ScopeAndTypeChecker.const_int, true);
		

		Label throwExcep = new Label();
		Label afterLengthCheck = new Label();
		
		int concatopssise = concatops.size();
		for(int n = 0; n < concatopssise; n++){
			bcoutputter.visitVarInsn(ALOAD, concatops.get(n).getA());
			bcoutputter.visitInsn(ARRAYLENGTH);
			bcoutputter.visitVarInsn(ILOAD, compareTo);
			
			if(n == concatopssise-1){//last
				bcoutputter.visitJumpInsn(IF_ICMPEQ, afterLengthCheck);
			}else{
				bcoutputter.visitJumpInsn(IF_ICMPNE, throwExcep);
			}
		}
				
		
		bcoutputter.visitLabel(throwExcep);
		bcoutputter.visitTypeInsn(NEW, "com/concurnas/runtime/HorizontalAppendArrayException");
		bcoutputter.visitInsn(DUP);
		bcoutputter.visitLdcInsn("Array element size mismatch");
		bcoutputter.visitMethodInsn(INVOKESPECIAL, "com/concurnas/runtime/HorizontalAppendArrayException", "<init>", "(Ljava/lang/String;)V", false);
		bcoutputter.visitInsn(ATHROW);
		
		
		
		bcoutputter.visitLabel(afterLengthCheck);
		bcoutputter.visitVarInsn(ALOAD, first.getA());
		bcoutputter.visitInsn(ARRAYLENGTH);
		bcoutputter.visitTypeInsn(ANEWARRAY, artyype.getBytecodeType());
		int resultPos = this.createNewLocalVar(this.getTempVarName(), artyype, true);
		
		Label l3 = new Label();
		bcoutputter.visitLabel(l3);
		bcoutputter.visitInsn(ICONST_0);
		int nTemp = this.createNewLocalVar(this.getTempVarName(), ScopeAndTypeChecker.const_int, true);
		Label l4 = new Label();
		bcoutputter.visitLabel(l4);
		Label endCheck = new Label();
		bcoutputter.visitJumpInsn(GOTO, endCheck);
		Label forStart = new Label();
		bcoutputter.visitLabel(forStart);
		
		bcoutputter.visitInsn(ICONST_0);
		for(Thruple<Integer, Integer, Type> slotAndLength : concatops){
			bcoutputter.visitVarInsn(ALOAD, slotAndLength.getA());
			bcoutputter.visitVarInsn(ILOAD, nTemp);
			bcoutputter.visitInsn(AALOAD);
			bcoutputter.visitInsn(ARRAYLENGTH);
			bcoutputter.visitInsn(IADD);
		}
		

		Utils.createArray(this.bcoutputter, compType.getArrayLevels(), compType, true);
		int row = this.createNewLocalVar(this.getTempVarName(), artyype, true);
		
		Label l10 = new Label();
		bcoutputter.visitLabel(l10);
		bcoutputter.visitInsn(ICONST_0);
		int pos = this.createNewLocalVar(this.getTempVarName(), ScopeAndTypeChecker.const_int, true);
		
		
		for(Thruple<Integer, Integer, Type> slotAndLength : concatops){
			
			Label l11 = new Label();
			bcoutputter.visitLabel(l11);
			bcoutputter.visitVarInsn(ALOAD, slotAndLength.getA());
			bcoutputter.visitVarInsn(ILOAD, nTemp);
			bcoutputter.visitInsn(AALOAD);
			bcoutputter.visitInsn(ICONST_0);
			bcoutputter.visitVarInsn(ALOAD, row);
			bcoutputter.visitVarInsn(ILOAD, pos);
			bcoutputter.visitVarInsn(ALOAD, slotAndLength.getA());
			bcoutputter.visitVarInsn(ILOAD, nTemp);
			bcoutputter.visitInsn(AALOAD);
			bcoutputter.visitInsn(ARRAYLENGTH);
			bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", false);
			
			Label l12 = new Label();
			bcoutputter.visitLabel(l12);
			bcoutputter.visitVarInsn(ILOAD, pos);
			bcoutputter.visitVarInsn(ALOAD, slotAndLength.getA());
			bcoutputter.visitVarInsn(ILOAD, nTemp);
			bcoutputter.visitInsn(AALOAD);
			bcoutputter.visitInsn(ARRAYLENGTH);
			bcoutputter.visitInsn(IADD);
			bcoutputter.visitVarInsn(ISTORE, pos);

			Label l15 = new Label();//res[n] = row;
			bcoutputter.visitLabel(l15);
			bcoutputter.visitVarInsn(ALOAD, resultPos);
			bcoutputter.visitVarInsn(ILOAD, nTemp);
			bcoutputter.visitVarInsn(ALOAD, row);
			bcoutputter.visitInsn(AASTORE);
		}
		
		Label l16 = new Label();
		bcoutputter.visitLabel(l16);
		bcoutputter.visitIincInsn(nTemp, 1);
		bcoutputter.visitLabel(endCheck);
		bcoutputter.visitVarInsn(ILOAD, nTemp);
		bcoutputter.visitVarInsn(ALOAD, first.getA());
		bcoutputter.visitInsn(ARRAYLENGTH);
		bcoutputter.visitJumpInsn(IF_ICMPLT, forStart);
		
		//result on stack
		bcoutputter.visitVarInsn(ALOAD, resultPos);
	}
	
	@Override
	public Object visit(ArrayDefComplex arrayDefComplex){
		Type ret = arrayDefComplex.getTaggedType();
		if(arrayDefComplex.bcarrayElements.size() == 1){//just one, nothing to concatinate
			arrayDefComplex.bcarrayElements.get(0).accept(this);
		}else{
			processArElements(arrayDefComplex.bcarrayElements, ret, arrayDefComplex.concatValid, -1);
		}
		return ret;
	}
	
	

	private Type arrayTypePrimToObj(Type origon) {
		if (!(origon instanceof PrimativeType) && origon.hasArrayLevels()) {
			NamedType ret = (NamedType) Const_Object.copy();
			ret.setArrayLevels(origon.getArrayLevels());
			origon = ret;
		}
		
		if(origon.getArrayLevels() > 1){
			NamedType ret = (NamedType) Const_Object.copy();
			ret.setArrayLevels(1);//capped at one level
			origon = ret;
		}
		
		return origon;
	}

	private void processArrayRefElementPrefixAll(ArrayRefElementPrefixAll are, Type origon, Type returns) {
		ArrayRefElementPrefixAll pre = (ArrayRefElementPrefixAll) are;
		ArrayList<Type> inputs = new ArrayList<Type>();

		inputs.add(arrayTypePrimToObj(origon));
		bcoutputter.visitInsn(ICONST_0);
		inputs.add(Const_PRIM_INT);
		inputs.add(Utils.unbox(this.bcoutputter, (Type) pre.e1.accept(this), this));

		String methodDesc = getNormalMethodInvokationDesc(inputs, arrayTypePrimToObj(returns));
		bcoutputter.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "copyOfRange", methodDesc);
		bcoutputter.visitTypeInsn(CHECKCAST, returns.getCheckCastType());
	}

	private void processArrayRefElementPostfixAll(ArrayRefElementPostfixAll are, Type origon, Type returns) {
		ArrayList<Type> inputs = new ArrayList<Type>();

		bcoutputter.visitInsn(Opcodes.DUP);
		this.bcoutputter.visitInsn(Opcodes.ARRAYLENGTH);
		String tempName = this.getTempVarName();
		int tempSlot = this.createNewLocalVar(tempName, Const_PRIM_INT, true);
		// dup means array still on top of stack
		inputs.add(arrayTypePrimToObj(origon));
		inputs.add(Utils.unbox(this.bcoutputter, (Type) are.e1.accept(this), this));
		bcoutputter.visitVarInsn(Opcodes.ILOAD, tempSlot);// load the length of the array
													// cos that's what we're
													// sublisting to
		inputs.add(Const_PRIM_INT);

		String methodDesc = getNormalMethodInvokationDesc(inputs, arrayTypePrimToObj(returns));
		bcoutputter.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "copyOfRange", methodDesc);
		bcoutputter.visitTypeInsn(CHECKCAST, returns.getCheckCastType());
	}

	private void processArrayRefElementSubList(ArrayRefElementSubList are, Type origon, Type returns) {
		ArrayList<Type> inputs = new ArrayList<Type>();

		/*
		 * NamedType nt = Const_Object.copy(); nt.setArrayLevels(1);
		 */

		inputs.add(arrayTypePrimToObj(origon));
		inputs.add(Utils.unbox(this.bcoutputter, (Type) are.e1.accept(this), this));
		inputs.add(Utils.unbox(this.bcoutputter, (Type) are.e2.accept(this), this));

		String methodDesc = getNormalMethodInvokationDesc(inputs, arrayTypePrimToObj(returns));
		bcoutputter.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "copyOfRange", methodDesc);
		bcoutputter.visitTypeInsn(CHECKCAST, returns.getCheckCastType());
	}

	private void processArrayRefElement(ArrayRefElement are, Type origon, boolean isLast, ArrayRef arrayRef) {
		ArrayRefElement simple = (ArrayRefElement) are;
		Utils.unbox(this.bcoutputter, (Type) simple.e1.accept(this), this);
		int levels = 0;
		if (null != origon) {
			levels = origon.getArrayLevels();
			origon.setArrayLevels(levels + 1);
		}
		// load...
		if (isLast && arrayRef.getDuplicateOnStack()) {// used for when u are
														// doing an pre/postfix
														// increment operation
														// on an array element
			if(!TypeCheckUtils.hasRefLevelsAndIsArray(origon)) {
				Utils.applyDup(bcoutputter, origon);
			}
		}
		Utils.applyArrayLoad(this.bcoutputter, origon);
		//mv.visitTypeInsn(CHECKCAST, "com/concurnas/runtime/ref/Local");
		if (null != origon) {
			origon.setArrayLevels(levels);
		}
	}

	private void processListArrayRefElementPrefixAll(ArrayRefElementPrefixAll are, Type returns) {
		bcoutputter.visitInsn(ICONST_0);
		Utils.unbox(this.bcoutputter, (Type) are.e1.accept(this), this);

		bcoutputter.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "subList", "(II)" + returns.getBytecodeType());
	}

	private void processListArrayRefElementPostfixAll(ArrayRefElementPostfixAll are, Type returns) {
		bcoutputter.visitInsn(Opcodes.DUP);
		bcoutputter.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I");
		String tempName = this.getTempVarName();
		int tempSlot = this.createNewLocalVar(tempName, Const_PRIM_INT, true);
		Utils.unbox(this.bcoutputter, (Type) are.e1.accept(this), this);
		bcoutputter.visitVarInsn(Opcodes.ILOAD, tempSlot);// load the length of the array
													// cos that's what we're
													// sublisting to

		bcoutputter.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "subList", "(II)" + returns.getBytecodeType());
	}

	private void processListArrayRefElementSubList(ArrayRefElementSubList are, Type returns) {
		Utils.unbox(this.bcoutputter, (Type) are.e1.accept(this), this);
		Utils.unbox(this.bcoutputter, (Type) are.e2.accept(this), this);

		bcoutputter.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "subList", "(II)" + returns.getBytecodeType());
	}

	@Override
	public Object visit(ArrayRef arrayRef) {
		Type typeOfThisLevel;
		if(arrayRef.expr instanceof NotNullAssertion) {//going to throw npe regardless
			typeOfThisLevel = (Type)((NotNullAssertion)arrayRef.expr).expr.accept(this);
		}else {
			typeOfThisLevel = (Type) arrayRef.expr.accept(this);
		}

		Type retType = arrayRef.getTaggedType();
		// typeOfThisLevel = Utils.unref(mv, typeOfThisLevel, this);

		// Type retType = arrayRef.getTaggedType();
		ArrayList<Pair<Boolean, ArrayRefElement>> arle = arrayRef.getFlatALEWithNullSafe();// arrayLevelElements.getAll();
		int sz = arle.size();
		for (int n = 0; n < sz; n++) {
			int thingRefLevels = TypeCheckUtils.getRefLevels(typeOfThisLevel);
			boolean doUnref = thingRefLevels > 0;
			if(n == arle.size()-1){
				doUnref = TypeCheckUtils.getRefLevels(arrayRef.penultimateOperatingOnType) != thingRefLevels;
			}
					
			if (doUnref) {
				typeOfThisLevel = Utils.unref(bcoutputter, typeOfThisLevel, 0, this, arrayRef.dupLastThingIfRef);

				if (arrayRef.dupLastThingIfRef) {
					bcoutputter.visitInsn(DUP);
				}
			}

			Thruple<Type, Type, ARElementType> typeAndARE = arrayRef.arrayLevelElements.getTaggedType(n);
			Type typeRetAtThisLevel = typeAndARE.getA();
			Type castToAtThisLevel = typeAndARE.getB();
			ARElementType areEle = typeAndARE.getC();
			Pair<Boolean, ArrayRefElement> elem =  arle.get(n);
			ArrayRefElement are = elem.getB();
			boolean isLast = n == arle.size() - 1;
			Pair<Label, Label> nullsafeCall = null;
			if(elem.getA()) {
				String tempName = this.getTempVarName();
				bcoutputter.visitInsn(DUP);
				int tempSlot = this.createNewLocalVar(tempName, typeOfThisLevel, true);
				Label ifNull = new Label();
				Label end = new Label();
				bcoutputter.visitJumpInsn(IFNULL, ifNull);
				nullsafeCall = new Pair<Label, Label>(ifNull, end);
				bcoutputter.visitVarInsn(ALOAD, tempSlot);
			}
			
			if(are.astOverrideOperatorOverload != null) {
				typeRetAtThisLevel = (Type)are.accept(this);
			}else {
				if (typeOfThisLevel.hasArrayLevels() || areEle == ARElementType.ARRAY) {
					// TODO: handle negative numbers in array sublists TODO: when more than 1d u need to make the subtype returned by the arrayref operation, u cant cheat and use the overall rettype...

					// public ArrayList<ArrayList<ArrayRefElement>>
					// arrayLevelElements;//[2, 3:4][2][:3, var, 3:var2];
					// for(ArrayRefElement are : ares)
					// {
					// TODO: wrong because this is not catered for: a = [[1,2,3],
					// [1,2,3], [1,2,3]]; xxx = a[1:2,1:2]
					Type origType = are.getTaggedType();

					if (are instanceof ArrayRefElementPrefixAll) {
						processArrayRefElementPrefixAll((ArrayRefElementPrefixAll) are, origType, typeRetAtThisLevel);
					} else if (are instanceof ArrayRefElementPostfixAll) {
						processArrayRefElementPostfixAll((ArrayRefElementPostfixAll) are, origType, typeRetAtThisLevel);
					} else if (are instanceof ArrayRefElementSubList) {
						processArrayRefElementSubList((ArrayRefElementSubList) are, origType, typeRetAtThisLevel);
					} else {// (are instanceof ArrayRefElement)
						processArrayRefElement((ArrayRefElement) are, typeRetAtThisLevel, isLast, arrayRef);
					}

				} else if (areEle == ARElementType.LIST) {
					// refactor there will be only one arrayref element... unless we support the likes of [1,2], for d2 lists (maybe later....) ArrayRefElement are = ares.get(0);
					if (are instanceof ArrayRefElementPrefixAll) {
						processListArrayRefElementPrefixAll((ArrayRefElementPrefixAll) are, typeRetAtThisLevel);
					} else if (are instanceof ArrayRefElementPostfixAll) {
						processListArrayRefElementPostfixAll((ArrayRefElementPostfixAll) are, typeRetAtThisLevel);
					} else if (are instanceof ArrayRefElementSubList) {
						processListArrayRefElementSubList((ArrayRefElementSubList) are, typeRetAtThisLevel);
					} else {// get 1 element...
						boolean isremove = are.liToMap == LISTorMAPType.REMOVE;
						listGetFunc((ArrayRefElement) are, (NamedType) typeOfThisLevel, typeRetAtThisLevel, arrayRef.getDuplicateOnStack() && isLast, isremove);
					}
				} else {// must be a map get operation
					boolean isremove = are.liToMap == LISTorMAPType.REMOVE;
					if (isremove || are.liToMap == LISTorMAPType.GET || are.liToMap == LISTorMAPType.GETANDPUT) {
						mapGeFunc(are, (NamedType) typeOfThisLevel, arrayRef.getDuplicateOnStack() && isLast, isremove);
					}
				}
			}

			if(isLast) {
				if(!retType.equals(typeRetAtThisLevel)) {
					Utils.applyCastImplicit(bcoutputter, typeRetAtThisLevel, retType, this);
					typeRetAtThisLevel = retType;
				}
			}
			
			if(nullsafeCall != null) {
				
				if(!typeRetAtThisLevel.equals(castToAtThisLevel)) {
					Utils.applyCastImplicit(bcoutputter, typeRetAtThisLevel, castToAtThisLevel, this);
				}
					
				bcoutputter.visitJumpInsn(GOTO, nullsafeCall.getB());//end
				bcoutputter.visitLabel(nullsafeCall.getA());//ifNull
				bcoutputter.visitInsn(ACONST_NULL);
				bcoutputter.visitLabel(nullsafeCall.getB());//end
				doCast(ScopeAndTypeChecker.const_object, castToAtThisLevel);
			}
			
			typeOfThisLevel = typeRetAtThisLevel;
		}
		
		return retType;
	}

	private void listFakeGetterFunc(FuncType sigF, NamedType typeLastIsOperatingOn) {

		int invokeOpCode = INVOKEVIRTUAL;
		if (typeLastIsOperatingOn.isInterface()) {
			invokeOpCode = INVOKEINTERFACE;
		}

		bcoutputter.visitMethodInsn(invokeOpCode, typeLastIsOperatingOn.getCheckCastType(), "get", "(I)Ljava/lang/Object;");
	}

	private void mapFakeGetterFunc(FuncType sigF, NamedType typeLastIsOperatingOn) {

		int invokeOpCode = INVOKEVIRTUAL;
		if (typeLastIsOperatingOn.isInterface()) {
			invokeOpCode = INVOKEINTERFACE;
		}
		
		Type inputta = sigF.getInputs().get(0);

		bcoutputter.visitMethodInsn(invokeOpCode, typeLastIsOperatingOn.getCheckCastType(), "get", "(" + inputta.getBytecodeType() +  ")Ljava/lang/Object;");
	}

	private void listMapPutFunc(FuncType sigF, NamedType typeLastIsOperatingOn, String FuncName) {
		listMapPutFunc(sigF, typeLastIsOperatingOn, FuncName, null);
	}
	
	private void listMapPutFunc(FuncType sigF, NamedType typeLastIsOperatingOn, String FuncName, Type argType) {
		String methodDesc = getNormalMethodInvokationDesc(sigF.getInputs(), sigF.retType);
		String operatingOn = typeLastIsOperatingOn.getCheckCastType();
		
		int invokeOpCode;
		if(sigF.extFuncOn) {
			operatingOn = sigF.origin.bcFullName();
			if(sigF.origin.classBlock == null) {//module level
				invokeOpCode = INVOKESTATIC;
			}else {
				ClassFunctionLocation loc = new ClassFunctionLocation(operatingOn, new NamedType(sigF.origin), false);
				prefixNodeWithThisreference(loc, true);
				bcoutputter.visitInsn(SWAP);
				invokeOpCode = INVOKEVIRTUAL;
			}
		}else if (typeLastIsOperatingOn.isInterface()) {
			invokeOpCode = INVOKEINTERFACE;
		}else {
			invokeOpCode = INVOKEVIRTUAL;
		}
		
		if(null != argType){
			Utils.applyCastImplicit(bcoutputter, argType, sigF.getInputs().get(1), this);
		}
		
		bcoutputter.visitMethodInsn(invokeOpCode, operatingOn, FuncName, methodDesc);
		// JPT: consdifer refacotring void check (also lambda check etc)
		if (!(sigF.retType == null || sigF.retType instanceof PrimativeType && ((PrimativeType) sigF.retType).type == PrimativeTypeEnum.VOID)) {
			// pop off stack if not void
			bcoutputter.visitInsn(POP);// maybe we want to return stuff here?
		}
	}

	private void listGetFunc(ArrayRefElement are, NamedType typeOfThisLevel, Type returns, boolean sneakInDup, boolean justLoad) {
		// FuncType sigF =
		// sneakInDup?are.mapOperationSignatureforGetter:are.mapOperationSignature;
		FuncType sigF = are.mapOperationSignatureforGetter != null ? are.mapOperationSignatureforGetter : are.mapOperationSignature;

		convertToKeyType((Type) are.e1.accept(this), sigF);
		
		if(!justLoad){
			if (sneakInDup) {
				bcoutputter.visitInsn(DUP2);
			}
	
			String methodDesc = getNormalMethodInvokationDesc(sigF.getInputs(), sigF.retType);
			String operatingOn = typeOfThisLevel.getCheckCastType();
			
			int invokeOpCode;
			if(sigF.extFuncOn) {
				operatingOn = sigF.origin.bcFullName();
				if(sigF.origin.classBlock == null) {//module level
					invokeOpCode = INVOKESTATIC;
				}else {
					ClassFunctionLocation loc = new ClassFunctionLocation(operatingOn, new NamedType(sigF.origin), false);
					prefixNodeWithThisreference(loc, true);
					bcoutputter.visitInsn(SWAP);
					invokeOpCode = INVOKEVIRTUAL;
				}
			}else if (typeOfThisLevel.isInterface()) {
				invokeOpCode = INVOKEINTERFACE;
			}else {
				invokeOpCode = INVOKEVIRTUAL;
			}
	
			bcoutputter.visitMethodInsn(invokeOpCode, operatingOn, "get", methodDesc);

		
			if (returns instanceof PrimativeType) {
				if (returns.hasArrayLevels()) {
					String gg = returns.getCheckCastType();
					bcoutputter.visitTypeInsn(CHECKCAST, gg);
				} else {
					NamedType boxed = (NamedType) TypeCheckUtils.boxTypeIfPrimative(returns, false);
					bcoutputter.visitTypeInsn(CHECKCAST, boxed.getCheckCastType());
					Utils.unbox(bcoutputter, boxed, this);
				}
			} else {
				bcoutputter.visitTypeInsn(CHECKCAST, ( returns).getCheckCastType());
			}
		}

		// mv.visitTypeInsn(CHECKCAST, sigF.retType.getCheckCastType() );
	}

	private void mapGeFunc(ArrayRefElement are, NamedType typeOfThisLevel, boolean sneakInDup, boolean justLoad) {
		FuncType sigF = are.mapOperationSignatureforGetter != null ? are.mapOperationSignatureforGetter : are.mapOperationSignature;

		String operatingOn = typeOfThisLevel.getCheckCastType();
		int invokeOpCode;
		if(sigF.extFuncOn) {
			operatingOn = sigF.origin.bcFullName();
			if(sigF.origin.classBlock == null) {//module level
				invokeOpCode = INVOKESTATIC;
			}else {
				ClassFunctionLocation loc = new ClassFunctionLocation(operatingOn, new NamedType(typeOfThisLevel.getLine(), typeOfThisLevel.getColumn(), sigF.origin), false);
				prefixNodeWithThisreference(loc, true);
				bcoutputter.visitInsn(SWAP);
				invokeOpCode = INVOKEVIRTUAL;
			}
		}else if (typeOfThisLevel.isInterface()) {
			invokeOpCode = INVOKEINTERFACE;
		}else {
			invokeOpCode = INVOKEVIRTUAL;
		}
		
		convertToKeyType((Type) are.e1.accept(this), sigF);
	
		if(are instanceof ArrayRefElementSubList){
			ArrayRefElementSubList aresl = (ArrayRefElementSubList)are;
			convertToKeyType((Type) aresl.e2.accept(this), sigF);//must both ints
		}
		

		if (sneakInDup) {
			bcoutputter.visitInsn(DUP2);
		}

		
		String methodDesc = getNormalMethodInvokationDesc(sigF.getInputs(), sigF.retType);

		bcoutputter.visitMethodInsn(invokeOpCode, operatingOn, sigF.origonatingFuncDef.funcName, methodDesc);
		// cast...
		if(sigF.retType.hasArrayLevels() || sigF.retType instanceof NamedType || sigF.retType instanceof FuncType){
			if(!sigF.retType.equals(ScopeAndTypeChecker.const_object)){
				bcoutputter.visitTypeInsn(CHECKCAST, sigF.retType.getCheckCastType());
			}
		}
	}

	private void convertToKeyType(Type input, FuncType keyFunctionHolder) {
		Type keyType = keyFunctionHolder.getInputs().get(keyFunctionHolder.extFuncOn? 1:0);
		if (keyType.equals(Const_Object)) {
			Utils.box(bcoutputter, input);
		} else {
			Utils.applyCastImplicit(bcoutputter, input, keyType, this);
		}
	}

	private boolean isRefHoldingArray(Type candi) {

		
		if (this.isRef(candi)) {
			boolean refAndGettable = TypeCheckUtils.hasRefLevelsAndIsNotLocked(candi) && TypeCheckUtils.assertRefIFaceImpl(null, null, 42, 42, candi, TypeCheckUtils.getRefLevels(candi) - TypeCheckUtils.getRefLevlstoLocked(candi), false, TypeCheckUtils.Cls_DirectlyGettable);
			if( !refAndGettable){
				return false;//no get on this fella
			}
			
			NamedType asNamed = (NamedType) candi;
			return asNamed.getGenTypes().get(0).hasArrayLevels();
		}
		return false;
	}

	private void loadRefAndDup(Type fromType) {
		// int::: => get get dup get. leaving 'lastRef and 'value on stack
		int levels = TypeCheckUtils.getRefLevels(fromType);
		for (int n = 0; n < levels; n++) {
			if (n == levels - 1) {
				bcoutputter.visitInsn(DUP);
			}
			bcoutputter.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/ref/DefaultRef", "get", "()Ljava/lang/Object;");
			if (n != levels - 1) {
				bcoutputter.visitTypeInsn(CHECKCAST, "com/concurnas/bootstrap/runtime/ref/DefaultRef");
			} else {
				bcoutputter.visitTypeInsn(CHECKCAST, TypeCheckUtils.getRefType(fromType).getCheckCastType());
			}
		}

		// return ;
	}

	private void storeRef(int levels) {
		// ref is on top of stack
		if (levels > 1) {// if check else we have a swap swap nop
			bcoutputter.visitInsn(SWAP);

			for (int n = 1; n < levels; n++) {
				bcoutputter.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/ref/DefaultRef", "get", "()Ljava/lang/Object;");
				bcoutputter.visitTypeInsn(CHECKCAST, "com/concurnas/bootstrap/runtime/ref/DefaultRef");
			}

			bcoutputter.visitInsn(SWAP);
		}
		// JPT: this could be better optimized, if we know the actual type of
		// the thing then we can invokevirtual instead of invoke interface -
		// referecning taht actual type
		bcoutputter.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/ref/DefaultRef", "set", "(Ljava/lang/Object;)V");
	}

	private Object assignArrayRef(ArrayRef arrayRef, Expression rhs, AssignStyleEnum eq, AssignExisting assignExisting) {
		Type origTypeForRet = (Type) arrayRef.expr.accept(this);

		Type retType = arrayRef.getTaggedType();
		Type retTypeNoRef = TypeCheckUtils.getRefTypeToLocked(retType);
		boolean retTypeIsRef = TypeCheckUtils.hasRefLevels(retType);
		boolean notEquals = !eq.isEquals();

		// TODO: handle negative numbers in array sublists AND when more than 1d
		// u need to make the subtype returned by the arrayref operation, u cant
		// cheat and use the overall rettype...
		// a[-1] etc

		ArrayList<ArrayRefElement> flattenALE = arrayRef.getFlatALE();
		// TODO: wrong because this is not catered for: a = [[1,2,3], [1,2,3],
		// [1,2,3]]; xxx = a[1:2,1:2]
		int flattenALESize = flattenALE.size();

		ArrayRefElement finalARE = flattenALE.get(flattenALESize - 1);
		Type finalorigType = finalARE.getTaggedType();
		boolean lhsStringConcatOperation = TypeCheckUtils.isString(finalorigType) && eq == AssignStyleEnum.PLUS_EQUALS;
		// boolean isEq = eq == AssignStyleEnum.EQUALS;
		Type typeLastIsOperatingOn = origTypeForRet;
		boolean isRef = false;
		boolean origHadArrayLevels = false;

		ArrayList<Pair<Integer, Integer>> refsNeedingDirectAssign = new ArrayList<Pair<Integer, Integer>>();//from a -> store to b
		
		int refslot = -1;
		
		
		for (int n = 0; n < flattenALESize; n++) {
			boolean isLast = n == flattenALESize - 1;
			
			int thingRefLevels = TypeCheckUtils.getRefLevels(origTypeForRet);
			boolean doUnref = thingRefLevels > 0;
			if(isLast){
				doUnref = TypeCheckUtils.getRefLevels(arrayRef.penultimateOperatingOnType) != thingRefLevels;
			}
			
			if(doUnref){
				
				if(TypeCheckUtils.isRefDirectlySettable(origTypeForRet, -1)){
					
					Type setsTo = TypeCheckUtils.extractRawRefType(origTypeForRet);
					boolean process = !TypeCheckUtils.isLocalArray(setsTo);
					
					if(!process){//dont store these unless we are overwriting the element
						int rhsRefs = TypeCheckUtils.getRefLevels(rhs.getTaggedType());
						
						Type tt = (Type)setsTo.copy();
						tt.setArrayLevels(0);
						
						int setToLevels = TypeCheckUtils.getRefLevels(tt);
						if(rhsRefs > 0  && setToLevels <= rhsRefs){
							process=true;
						}
						else if(assignExisting.refCnt == setToLevels){
							process=true;
						}
						
					}
					
					if(process){
						//mv.visitInsn(DUP);
						refslot = this.createNewLocalVar(this.getTempVarName(), origTypeForRet, false);
					}
				}
				
				if(refslot != -1){
					typeLastIsOperatingOn = Utils.unref(bcoutputter, origTypeForRet, 0, this, isLast, refslot);//TODO: tidy extra DUP here, pollute stack: b = [1,2]:; b[0]=23
				}else{
					typeLastIsOperatingOn = Utils.unref(bcoutputter, origTypeForRet, 0, this, false);
				}
				
				if(refslot != -1){
					bcoutputter.visitInsn(DUP);
					int valSlot = this.createNewLocalVar(this.getTempVarName(), origTypeForRet, true);
					refsNeedingDirectAssign.add(new Pair<Integer, Integer>(refslot, valSlot));
					refslot=-1;
				}
			}
			else{
				typeLastIsOperatingOn = origTypeForRet;
			}


			Thruple<Type, Type, ARElementType> typeAndARE = arrayRef.arrayLevelElements.getTaggedType(n);
			Type typeRetThisLevel = typeAndARE.getA();
			ARElementType areEle = typeAndARE.getC();
			ArrayRefElement are = flattenALE.get(n);
			
			if(are.astOverrideOperatorOverload != null){
				//a[3] += 5
				
				if(are.astOverrideOperatorOverloadForGetter != null) {//get first then setter, aa[3] += 9 etc
					//getter
					FuncInvoke fi = are.astOverrideOperatorOverloadForGetter;
					acceptFuncInvokeArgs(fi, (FuncType)fi.resolvedFuncTypeAndLocation.getType(), false, null);
					bcoutputter.visitInsn(DUP2);
					fi.args.asnames.remove(0);
					Type getterType = (Type)are.astOverrideOperatorOverloadForGetter.accept(this);
					
					if (!lhsStringConcatOperation) {
						if (eq == AssignStyleEnum.POW_EQUALS) {
							Utils.applyCastImplicit(bcoutputter, getterType, RefDoublePrim, this);
						} else {
							Utils.applyCastImplicit(bcoutputter, getterType, TypeCheckUtils.unboxTypeIfBoxed(retType), this, true);
						}
						//applyFancyAssignmentToArrayRef(eq, retType);
					}
					
					if (lhsStringConcatOperation) {
						bcoutputter.visitTypeInsn(NEW, "java/lang/StringBuilder");
						bcoutputter.visitInsn(DUP_X1);
						bcoutputter.visitInsn(SWAP);

						bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
						bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
					}
					
					Type got = (Type)rhs.accept(this);
					donullcheckForUnknown(got, null);
					
					//setter
					fi = are.astOverrideOperatorOverload;
					Type want = ((FuncType)are.astOverrideOperatorOverload.resolvedFuncTypeAndLocation.getType()).getInputs().get(1);
					if (!lhsStringConcatOperation) {
						Type ofOp;
						if (eq == AssignStyleEnum.POW_EQUALS) {
							ofOp = RefDoublePrim;
						} else {
							ofOp = TypeCheckUtils.unboxTypeIfBoxed(retType);
						}
						
						Utils.applyCastImplicit(bcoutputter, got, ofOp, this);
						
						applyFancyAssignmentToArrayRef(eq, ofOp);

						Utils.applyCastImplicit(bcoutputter, ofOp, want, this);
					}else {
						Utils.unref(bcoutputter, got, this);
						if (got.hasArrayLevels()) {
							StringBuffHelper.append(this, got);
						} else {
							String bcType = got instanceof NamedType ? "Ljava/lang/Object;" : got.getBytecodeType();
							StringBuffHelper.doAppend(this, bcType);
						}
						bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
					}
					
					fi.args.asnames.clear();
					are.astOverrideOperatorOverload.accept(this);
					return are.astOverrideOperatorOverload.getTaggedType();//?does this fail on: c1[1][3]=... / c1[1, 3] = ...?
					
				}else {
					if(!are.isSingleElementRefEle()) {
						if(isLast) {
							are.astOverrideOperatorOverload.setShouldBePresevedOnStack(false);//pop off the result if anything returned
							are.astOverrideOperatorOverload.accept(this);
							return null; 
						}
						origTypeForRet = (Type)are.astOverrideOperatorOverload.accept(this);
					}else {
						//we're doing c1['x'] = 12 on something that is no a map and just happens to override the get function
						//we really butcher the function invokation up here, hope nobody else wants to use this same ref!!
						FuncInvoke fi = are.astOverrideOperatorOverload;
						fi.args.asnames.remove(1);
						acceptFuncInvokeArgs(fi, (FuncType)fi.resolvedFuncTypeAndLocation.getType(), false, null);
						Type got = (Type)rhs.accept(this);
						donullcheckForUnknown(got, null);
						Type want = ((FuncType)are.astOverrideOperatorOverload.resolvedFuncTypeAndLocation.getType()).getInputs().get(1);
						Utils.applyCastImplicit(bcoutputter, got, want, this);
						
						fi.args.asnames.remove(0);
						are.astOverrideOperatorOverload.accept(this);
						return are.astOverrideOperatorOverload.getTaggedType();//?does this fail on: c1[1][3]=... / c1[1, 3] = ...?
					}
				}
			}else {

				Type origType = are.getTaggedType();

				if (typeLastIsOperatingOn.hasArrayLevels() || isRefHoldingArray(origTypeForRet)) {
					if (are instanceof ArrayRefElementPrefixAll) {
						if (!isLast) {
							processArrayRefElementPrefixAll((ArrayRefElementPrefixAll) are, origType, retType);
						}

					} else if (are instanceof ArrayRefElementPostfixAll) {
						if (!isLast) {
							processArrayRefElementPostfixAll((ArrayRefElementPostfixAll) are, origType, retType);
						}

					} else if (are instanceof ArrayRefElementSubList) {
						if (!isLast) {
							processArrayRefElementSubList((ArrayRefElementSubList) are, origType, retType);
						}
					} else// (are instanceof ArrayRefElement)
					{// allows u to have both, x[b] = 2 and x[b] +=2 etc

						ArrayRefElement simple = (ArrayRefElement) are;
						Type idxGotType = (Type)simple.e1.accept(this);//needs to be an int
						Utils.applyCastImplicit(bcoutputter, idxGotType, ScopeAndTypeChecker.const_int, this);
						

						if (notEquals && isLast && !retTypeIsRef) {// copy on last
							bcoutputter.visitInsn(DUP2);
							int levels = origType.getArrayLevels();
							origType.setArrayLevels(levels + 1);
							Utils.applyArrayLoad(bcoutputter, origType);
							origType.setArrayLevels(levels);
							isRef = TypeCheckUtils.hasRefLevels(origType);
							if (isRef && lhsStringConcatOperation) {
								bcoutputter.visitInsn(DUP);
								Utils.unref(bcoutputter, origType, this);
							}
						}

						if (!retTypeIsRef) {
							if (eq == AssignStyleEnum.POW_EQUALS && isLast) {// cast thing
								Utils.applyCastImplicit(bcoutputter, retType, RefDoublePrim, this);
								// retType.setArrayLevels(levels);
							} else if (notEquals && !lhsStringConcatOperation && are.getTaggedType() instanceof NamedType && isLast && !retTypeIsRef) {
								Utils.unbox(bcoutputter, are.getTaggedType(), this);
								// Utils.unbox(mv, origTypeForRet);
							}
						}

						if (!isLast) {// load all but the last one...a[v][d] = 7 (so
										// load only after a[v]
							int levels = 0;
							if (null != typeRetThisLevel) {
								levels = typeRetThisLevel.getArrayLevels();
								typeRetThisLevel.setArrayLevels(levels + 1);
							}
							Utils.applyArrayLoad(this.bcoutputter, typeRetThisLevel);
							if (null != origType) {
								typeRetThisLevel.setArrayLevels(levels);
							}
						}
					}
				} else if (areEle == ARElementType.LIST) {
					if (are instanceof ArrayRefElementPrefixAll) {
						if (!isLast) {
							processListArrayRefElementPrefixAll((ArrayRefElementPrefixAll) are, typeRetThisLevel);
						}

					} else if (are instanceof ArrayRefElementPostfixAll) {
						if (!isLast) {
							processListArrayRefElementPostfixAll((ArrayRefElementPostfixAll) are, typeRetThisLevel);
						}
					} else if (are instanceof ArrayRefElementSubList) {// TODO: dont need to worry about a[2:4] [+|-|/|*|etc]= [55,66], just direct overrite
						if (!isLast) {
							processListArrayRefElementSubList((ArrayRefElementSubList) are, typeRetThisLevel);
						}
					} else {// (are instanceof ArrayRefElement)
						if (!isLast || (isLast && notEquals && !TypeCheckUtils.hasRefLevels(typeRetThisLevel))) {
							listGetFunc((ArrayRefElement) are, (NamedType) typeLastIsOperatingOn, typeRetThisLevel, notEquals && isLast, false);
						}
					}
				} else { // if(areEle == ARElementType.MAP)
					if (!isLast || (isLast && notEquals && !TypeCheckUtils.hasRefLevels(typeRetThisLevel))) {
						mapGeFunc(are, (NamedType) typeLastIsOperatingOn, notEquals && isLast, false);
					}
				}

				origTypeForRet = typeRetThisLevel;
			}
			
		}

		// /final one, balls of the work
		int lastn = flattenALE.size() - 1;
		ArrayRefElement lastARE = flattenALE.get(lastn);

		Thruple<Type, Type, ARElementType> typeAndARE = arrayRef.arrayLevelElements.getTaggedType(lastn);
		Type retOfOperation = typeAndARE.getA();
		ARElementType areEle = typeAndARE.getC();

		
		
		if(TypeCheckUtils.getRefLevels(arrayRef.penultimateOperatingOnType) != TypeCheckUtils.getRefLevels(typeLastIsOperatingOn)){
			typeLastIsOperatingOn = Utils.unref(bcoutputter, typeLastIsOperatingOn, this);
		}
		
		
		

		if (typeLastIsOperatingOn.hasArrayLevels() || isRefHoldingArray(typeLastIsOperatingOn)) {
			if (lastARE instanceof ArrayRefElementPrefixAll) {
				Label cont = ArraySubListSetterHelper.insertIntoArray(this, retType, null, rhs, this);
				this.bcoutputter.pushNextLabel(cont);
			} else if (lastARE instanceof ArrayRefElementPostfixAll) {
				ArrayRefElementPostfixAll post = (ArrayRefElementPostfixAll) lastARE;
				Label cont = ArraySubListSetterHelper.insertIntoArray(this, retType, post.e1, rhs, this);
				this.bcoutputter.pushNextLabel(cont);
			} else if (lastARE instanceof ArrayRefElementSubList) {
				// TODO: these dont work very well when u want to do a[2:3] += ["", ""] //can u even do this?
				Label cont = ArraySubListSetterHelper.insertIntoArray(this, retType, lastARE.e1, rhs, this);
				this.bcoutputter.pushNextLabel(cont);
			} else// (are instanceof ArrayRefElement)
			{
				if (!retTypeIsRef) {// normal
					if (lhsStringConcatOperation) {
						bcoutputter.visitTypeInsn(NEW, "java/lang/StringBuilder");
						bcoutputter.visitInsn(DUP_X1);
						bcoutputter.visitInsn(SWAP);

						bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
						bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");

						Type rhsType = (Type) rhs.accept(this);
						donullcheckForUnknown(rhsType, null);
						Utils.unref(bcoutputter, rhsType, this);
						// TypeCheckUtils.unlockAllNestedRefs(rhsType);
						// Utils.applyCastImplicit(mv, rhsType, retType, this,
						// true);

						if (rhsType.hasArrayLevels()) {
							StringBuffHelper.append(this, rhsType);
						} else {
							String bcType = rhsType instanceof NamedType ? "Ljava/lang/Object;" : rhsType.getBytecodeType();
							StringBuffHelper.doAppend(this, bcType);
						}
						bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");

						Utils.applyArrayStore(bcoutputter, typeLastIsOperatingOn, this);
					} else {
						Type rhsType = (Type) rhs.accept(this);
						donullcheckForUnknown(rhsType, null);
						// TypeCheckUtils.unlockAllNestedRefs(rhsType);

						if (eq.isEquals()) {
							Utils.applyCastImplicit(bcoutputter, rhsType, retType, this, true);
						} else {// +=, -= etc, **=
							if (eq == AssignStyleEnum.POW_EQUALS) {
								Utils.applyCastImplicit(bcoutputter, rhsType, RefDoublePrim, this);
							} else {
								Utils.applyCastImplicit(bcoutputter, rhsType, retType, this, true);
							}

							applyFancyAssignmentToArrayRef(eq, retType);
						}
						Utils.applyArrayStore(bcoutputter, typeLastIsOperatingOn, this);
					}

				} else {
					// its a ref
					if (lhsStringConcatOperation) {
						Utils.applyArrayLoad(this.bcoutputter, typeLastIsOperatingOn);
						loadRefAndDup(retType);

						bcoutputter.visitTypeInsn(NEW, "java/lang/StringBuilder");
						bcoutputter.visitInsn(DUP_X1);
						bcoutputter.visitInsn(SWAP);

						bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
						bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");

						Type rhsType = (Type) rhs.accept(this);
						donullcheckForUnknown(rhsType, null);
						// TypeCheckUtils.unlockAllNestedRefs(rhsType);
						Utils.unref(bcoutputter, rhsType, this);

						if (rhsType.hasArrayLevels()) {
							StringBuffHelper.append(this, rhsType);
						} else {

							String bcType = rhsType instanceof NamedType ? "Ljava/lang/Object;" : rhsType.getBytecodeType();
							StringBuffHelper.doAppend(this, bcType);
						}
						bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");

						bcoutputter.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/ref/DefaultRef", "set", "(Ljava/lang/Object;)V");
					} else {
						int lhsResolvesRefLevels = TypeCheckUtils.getRefLevels(retType);
						if (eq.isEquals()) {
							boolean hasRefCnt = assignExisting.refCnt > 0;
							Type tryingToPassType = rhs.getTaggedType();
							if (!hasRefCnt) {
								// a[7] = 9
								Utils.applyArrayLoad(this.bcoutputter, typeLastIsOperatingOn);
								rhs.accept(this);

								if (TypeCheckUtils.hasRefLevelsAndNotLocked(tryingToPassType)) {
									tryingToPassType = Utils.unref(bcoutputter, tryingToPassType, this);
								}
								donullcheckForUnknown(tryingToPassType, retTypeNoRef);

								Utils.applyCastImplicit(bcoutputter, tryingToPassType, retTypeNoRef, this, true);

								storeRef(lhsResolvesRefLevels);
								

								// mv.visitMethodInsn(INVOKEVIRTUAL,
								// "com/concurnas/runtime/ref/Local", "set",
								// "(Ljava/lang/Object;)V");
							} else {// if(assignExisting.refCnt >=
									// TypeCheckUtils.getRefLevels(tryingToPassType)){
									// z := "newValue", rhs needs to be a ref -
									// so turn it into a ref and REPLACE the
									// thing on the lhs
								int setTo = assignExisting.refCnt;
								int rhsRefLevels = TypeCheckUtils.getRefLevels(tryingToPassType);
								if (lhsResolvesRefLevels == setTo) {
									// e.g. sdf = [ 0:::, 0:::, 0:::, 0:::];
									// sdf[3] :::= 9
									rhs.accept(this);
									donullcheckForUnknown(tryingToPassType, null);
									tryingToPassType = convertToRefLevels(setTo, rhsRefLevels, tryingToPassType);
									Utils.applyArrayStore(this.bcoutputter, typeLastIsOperatingOn, this);
								} else {
									Utils.applyArrayLoad(this.bcoutputter, typeLastIsOperatingOn);
									rhs.accept(this);
									donullcheckForUnknown(tryingToPassType, null);
									tryingToPassType = convertToRefLevels(setTo, rhsRefLevels, tryingToPassType);
									// now the rhs is the right level of ref
									storeRef(lhsResolvesRefLevels - setTo);
								}

								// TODO: += string concat
							}
						} else {// +=, -= etc
							Utils.applyArrayLoad(this.bcoutputter, typeLastIsOperatingOn);
							loadRefAndDup(retType);
							if (eq == AssignStyleEnum.POW_EQUALS) {
								Utils.applyCastImplicit(bcoutputter, retTypeNoRef, RefDoublePrim, this, true);

								Type rhsType = (Type) rhs.accept(this);
								donullcheckForUnknown(rhsType, null);
								// TypeCheckUtils.unlockAllNestedRefs(rhsType);
								Utils.applyCastImplicit(bcoutputter, rhsType, RefDoublePrim, this, true);
								applyFancyAssignmentToArrayRef(eq, RefDoublePrim);
								Utils.applyCastImplicit(bcoutputter, RefDoublePrim, retTypeNoRef, this, true);
							} else {
								Utils.applyCastImplicit(bcoutputter, retTypeNoRef, retTypeNoRef, this, true);
								PrimativeType primOp = Utils.unbox(bcoutputter, retTypeNoRef, this);

								Type rhsType = (Type) rhs.accept(this);
								donullcheckForUnknown(rhsType, null);
								// TypeCheckUtils.unlockAllNestedRefs(rhsType);
								Utils.applyCastImplicit(bcoutputter, rhsType, primOp, this, true);
								applyFancyAssignmentToArrayRef(eq, primOp);
								Utils.applyCastImplicit(bcoutputter, primOp, retTypeNoRef, this, true);
							}

							// storeRef(lhsResolvesRefLevels);

							bcoutputter.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/ref/DefaultRef", "set", "(Ljava/lang/Object;)V");
						}
					}
				}
			}
		} else if (areEle == ARElementType.LIST) {
			if (lastARE instanceof ArrayRefElementPrefixAll) {

			} else if (lastARE instanceof ArrayRefElementPostfixAll) {
				// TODO: missing a[2:3] //etc for lists?
			} else if (lastARE instanceof ArrayRefElementSubList) {// TODO: dont
																	// need to
																	// worry
																	// about
																	// a[2:4]
																	// [+|-|/|*|etc]=
																	// [55,66],
																	// just
																	// direct
																	// overrite

			} else {// (are instanceof ArrayRefElement)
					// Utils.unbox(this.mv, (Type)lastARE.e1.accept(this));//idx
				if (!retTypeIsRef) {// normal

					if (!lhsStringConcatOperation) {
						if (notEquals) {// += etc
							PrimativeType to = Utils.unbox(bcoutputter, retOfOperation, this);
							if (eq == AssignStyleEnum.POW_EQUALS) {
								Utils.applyCastImplicit(bcoutputter, to, RefDoublePrim, this);
							}
						} else {// setup stack
							Utils.unbox(this.bcoutputter, (Type) lastARE.e1.accept(this), this);
						}
					}

					if (lhsStringConcatOperation) {
						bcoutputter.visitTypeInsn(NEW, "java/lang/StringBuilder");
						bcoutputter.visitInsn(DUP_X1);
						bcoutputter.visitInsn(SWAP);

						bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
						bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
					}

					Type rhsType = (Type) rhs.accept(this);
					TypeCheckUtils.unlockAllNestedRefs(rhsType);
					donullcheckForUnknown(rhsType, null);

					/*
					 * if(toStoreType instanceof PrimativeType &&
					 * !lhsStringConcatOperation && !notEquals ){//prim must be
					 * boxed up to poshy object type Utils.box(this.mv,
					 * (PrimativeType)toStoreType);//idx }
					 */

					if (notEquals) {// if not equals, now we do some clever
									// stuff
						if (lhsStringConcatOperation) {
							if (rhsType.hasArrayLevels()) {
								StringBuffHelper.append(this, rhsType);
							} else {
								String bcType = rhsType instanceof NamedType ? "Ljava/lang/Object;" : rhsType.getBytecodeType();
								StringBuffHelper.doAppend(this, bcType);
							}
							bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
						} else {
							PrimativeType loadedType = Utils.unbox(bcoutputter, rhsType, this);

							PrimativeType castTo = TypeCheckUtils.getUnboxedPrimativeType(retOfOperation);

							if (eq == AssignStyleEnum.POW_EQUALS) {
								// rhsType= Utils.unbox(mv, rhsType);
								Utils.applyCastImplicit(bcoutputter, loadedType, RefDoublePrim, this);
							} else {
								Utils.applyCastImplicit(bcoutputter, loadedType, castTo, this);
							}

							applyListFancyAssignmentToArrayRef(eq, castTo);
							Utils.box(bcoutputter, castTo);
						}
					} else {
						if (!retOfOperation.equals(rhsType) && TypeCheckUtils.isBoxedType(retOfOperation) && !(rhsType instanceof VarNull)) {
							PrimativeType unboxedStoredAs = TypeCheckUtils.getUnboxedPrimativeType(retOfOperation);
							if (rhsType instanceof PrimativeType) {
								Utils.applyCastExplicit(bcoutputter, rhsType, unboxedStoredAs, this);
							} else {
								PrimativeType unboxedV = Utils.unbox(bcoutputter, rhsType, this);
								Utils.applyCastExplicit(bcoutputter, unboxedV, unboxedStoredAs, this);
							}
							Utils.box(bcoutputter, unboxedStoredAs);
						}
					}

					listMapPutFunc(lastARE.mapOperationSignature, (NamedType) typeLastIsOperatingOn, "set");

					if (origHadArrayLevels) {
						bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/runtime/ref/Local", "set", "(Ljava/lang/Object;)V");
					}

					// mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List",
					// "set", "(ILjava/lang/Object;)Ljava/lang/Object;");
					// mv.visitInsn(POP);
				} else {// it's a ref
					Utils.unbox(this.bcoutputter, (Type) lastARE.e1.accept(this), this);

					// JPT: this code could for arrays on ref types, lists and
					// maps could all be refectored together into one instance,
					// meh

					if (lhsStringConcatOperation) {
						listFakeGetterFunc(lastARE.mapOperationSignature, (NamedType) typeLastIsOperatingOn);
						bcoutputter.visitTypeInsn(CHECKCAST, "com/concurnas/runtime/ref/Local");

						loadRefAndDup(retType);

						bcoutputter.visitTypeInsn(NEW, "java/lang/StringBuilder");
						bcoutputter.visitInsn(DUP_X1);
						bcoutputter.visitInsn(SWAP);

						bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
						bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");

						Type rhsType = (Type) rhs.accept(this);
						// TypeCheckUtils.unlockAllNestedRefs(rhsType);
						Utils.unref(bcoutputter, rhsType, this);
						donullcheckForUnknown(rhsType, null);

						if (rhsType.hasArrayLevels()) {
							StringBuffHelper.append(this, rhsType);
						} else {
							String bcType = rhsType instanceof NamedType ? "Ljava/lang/Object;" : rhsType.getBytecodeType();
							StringBuffHelper.doAppend(this, bcType);
						}
						bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");

						bcoutputter.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/ref/DefaultRef", "set", "(Ljava/lang/Object;)V");
					} else {
						// listMapPutFunc(lastARE.mapOperationSignature,
						// (NamedType)typeLastIsOperatingOn, "set");
						int lhsResolvesRefLevels = TypeCheckUtils.getRefLevels(retType);
						if (eq.isEquals()) {
							boolean hasRefCnt = assignExisting.refCnt > 0;
							Type tryingToPassType = rhs.getTaggedType();
							if (!hasRefCnt) {
								// a[7] = 9
								listFakeGetterFunc(lastARE.mapOperationSignature, (NamedType) typeLastIsOperatingOn);
								bcoutputter.visitTypeInsn(CHECKCAST, "com/concurnas/runtime/ref/Local");

								rhs.accept(this);
								donullcheckForUnknown(tryingToPassType, null);

								if (TypeCheckUtils.hasRefLevelsAndNotLocked(tryingToPassType)) {
									tryingToPassType = Utils.unref(bcoutputter, tryingToPassType, this);
								}

								Utils.applyCastImplicit(bcoutputter, tryingToPassType, retTypeNoRef, this, true);

								storeRef(lhsResolvesRefLevels);
							} else {// if(assignExisting.refCnt >=
									// TypeCheckUtils.getRefLevels(tryingToPassType)){
									// z := "newValue", rhs needs to be a ref -
									// so turn it into a ref and REPLACE the
									// thing on the lhs
								int setTo = assignExisting.refCnt;
								int rhsRefLevels = TypeCheckUtils.getRefLevels(tryingToPassType);
								if (lhsResolvesRefLevels == setTo) {
									// e.g. sdf = [ 0:::, 0:::, 0:::, 0:::];
									// sdf[3] :::= 9
									rhs.accept(this);
									donullcheckForUnknown(tryingToPassType, null);
									tryingToPassType = convertToRefLevels(setTo, rhsRefLevels, tryingToPassType);
									// Utils.applyArrayStore(this.mv,
									// typeLastIsOperatingOn, this);
									listMapPutFunc(lastARE.mapOperationSignature, (NamedType) typeLastIsOperatingOn, "set");
								} else {
									listFakeGetterFunc(lastARE.mapOperationSignature, (NamedType) typeLastIsOperatingOn);
									bcoutputter.visitTypeInsn(CHECKCAST, "com/concurnas/runtime/ref/Local");
									rhs.accept(this);
									donullcheckForUnknown(tryingToPassType, null);
									tryingToPassType = convertToRefLevels(setTo, rhsRefLevels, tryingToPassType);
									// now the rhs is the right level of ref
									storeRef(lhsResolvesRefLevels - setTo);
								}

								// TODO: += string concat
							}
						} else {// +=, -= etc
							listFakeGetterFunc(lastARE.mapOperationSignature, (NamedType) typeLastIsOperatingOn);
							bcoutputter.visitTypeInsn(CHECKCAST, "com/concurnas/runtime/ref/Local");

							loadRefAndDup(retType);

							if (eq == AssignStyleEnum.POW_EQUALS) {
								Utils.applyCastImplicit(bcoutputter, retTypeNoRef, RefDoublePrim, this, true);

								Type rhsType = (Type) rhs.accept(this);
								donullcheckForUnknown(rhsType, null);
								// TypeCheckUtils.unlockAllNestedRefs(rhsType);
								Utils.applyCastImplicit(bcoutputter, rhsType, RefDoublePrim, this, true);
								applyFancyAssignmentToArrayRef(eq, RefDoublePrim);
								Utils.applyCastImplicit(bcoutputter, RefDoublePrim, retTypeNoRef, this, true);
							} else {
								Utils.applyCastImplicit(bcoutputter, retTypeNoRef, retTypeNoRef, this, true);
								PrimativeType primOp = Utils.unbox(bcoutputter, retTypeNoRef, this);

								Type rhsType = (Type) rhs.accept(this);
								donullcheckForUnknown(rhsType, null);
								// TypeCheckUtils.unlockAllNestedRefs(rhsType);
								Utils.applyCastImplicit(bcoutputter, rhsType, primOp, this, true);
								applyFancyAssignmentToArrayRef(eq, primOp);
								Utils.applyCastImplicit(bcoutputter, primOp, retTypeNoRef, this, true);
							}

							bcoutputter.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/ref/DefaultRef", "set", "(Ljava/lang/Object;)V");
						}
					}
				}
			}
		} else { // map operation!?
			if (!retTypeIsRef) {// normal

				if (!notEquals) {// if equals
					convertToKeyType((Type) lastARE.e1.accept(this), lastARE.mapOperationSignature);// key
				} else {
					Type ret = (Type) lastARE.mapOperationSignatureforGetter.retType.copy();
					PrimativeType unboxedas = Utils.unbox(bcoutputter, ret, this);
					if (null != unboxedas && eq == AssignStyleEnum.POW_EQUALS) {
						Utils.applyCastImplicit(bcoutputter, unboxedas, RefDoublePrim, this);
					}
				}

				if (lhsStringConcatOperation) {
					bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
					bcoutputter.visitTypeInsn(NEW, "java/lang/StringBuilder");
					bcoutputter.visitInsn(SWAP);
					bcoutputter.visitInsn(DUP2);
					bcoutputter.visitInsn(POP);
					bcoutputter.visitInsn(SWAP);// JPT: maybe better way to do the above
										// jiggling?

					bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
				}

				Type rhsType = (Type) rhs.accept(this);// vaue
				TypeCheckUtils.unlockAllNestedRefs(rhsType);
				donullcheckForUnknown(rhsType, null);

				if (notEquals) {
					if (lhsStringConcatOperation) {
						rhsType = Utils.box(this.bcoutputter, rhsType);
						if (rhsType.hasArrayLevels()) {
							StringBuffHelper.append(this, rhsType);
						} else {
							String bcType = rhsType instanceof NamedType ? "Ljava/lang/Object;" : rhsType.getBytecodeType();
							StringBuffHelper.doAppend(this, bcType);
						}
						bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
					} else {
						PrimativeType loadedType = Utils.unbox(bcoutputter, rhsType, this);

						PrimativeType castTo = TypeCheckUtils.getUnboxedPrimativeType(retOfOperation);
						if(null == castTo){
							castTo = (PrimativeType)retOfOperation;
						}

						if (eq == AssignStyleEnum.POW_EQUALS) {
							// rhsType= Utils.unbox(mv, rhsType);
							rhsType=Utils.applyCastImplicit(bcoutputter, loadedType, RefDoublePrim, this);
						} else {
							rhsType=Utils.applyCastImplicit(bcoutputter, loadedType, castTo, this);
						}

						applyListFancyAssignmentToArrayRef(eq, castTo);
						if(eq == AssignStyleEnum.POW_EQUALS) {
							rhsType=castTo;
						}
					}
				} else {
					//Utils.applyCastImplicit(mv, rhsType, retOfOperation, this);
				}
				
				Utils.applyCastImplicit(bcoutputter, rhsType, retOfOperation, this);

				listMapPutFunc(lastARE.mapOperationSignature, (NamedType) typeLastIsOperatingOn, "put", retOfOperation);

				if (origHadArrayLevels) {
					bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/runtime/ref/Local", "set", "(Ljava/lang/Object;)V");
				}
			} else {// its a ref

				convertToKeyType((Type) lastARE.e1.accept(this), lastARE.mapOperationSignature);// key

				// JPT: this code could for arrays on ref types, lists and maps
				// could all be refectored together into one instance, meh

				if (lhsStringConcatOperation) {
					mapFakeGetterFunc(lastARE.mapOperationSignature, (NamedType) typeLastIsOperatingOn);
					bcoutputter.visitTypeInsn(CHECKCAST, "com/concurnas/runtime/ref/Local");

					loadRefAndDup(retType);

					bcoutputter.visitTypeInsn(NEW, "java/lang/StringBuilder");
					bcoutputter.visitInsn(DUP_X1);
					bcoutputter.visitInsn(SWAP);

					bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
					bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");

					Type rhsType = (Type) rhs.accept(this);
					// TypeCheckUtils.unlockAllNestedRefs(rhsType);
					Utils.unref(bcoutputter, rhsType, this);
					donullcheckForUnknown(rhsType, null);

					if (rhsType.hasArrayLevels()) {
						StringBuffHelper.append(this, rhsType);
					} else {
						String bcType = rhsType instanceof NamedType ? "Ljava/lang/Object;" : rhsType.getBytecodeType();
						StringBuffHelper.doAppend(this, bcType);
					}
					bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");

					bcoutputter.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/ref/DefaultRef", "set", "(Ljava/lang/Object;)V");
				} else {
					// listMapPutFunc(lastARE.mapOperationSignature,
					// (NamedType)typeLastIsOperatingOn, "set");
					int lhsResolvesRefLevels = TypeCheckUtils.getRefLevels(retType);
					if (eq.isEquals()) {
						boolean hasRefCnt = assignExisting.refCnt > 0;
						Type tryingToPassType = rhs.getTaggedType();
						if (!hasRefCnt) {
							// a[7] = 9
							mapFakeGetterFunc(lastARE.mapOperationSignature, (NamedType) typeLastIsOperatingOn);
							bcoutputter.visitTypeInsn(CHECKCAST, "com/concurnas/runtime/ref/Local");

							rhs.accept(this);
							donullcheckForUnknown(tryingToPassType, null);

							if (TypeCheckUtils.hasRefLevelsAndNotLocked(tryingToPassType)) {
								tryingToPassType = Utils.unref(bcoutputter, tryingToPassType, this);
							}

							Utils.applyCastImplicit(bcoutputter, tryingToPassType, retTypeNoRef, this, true);

							storeRef(lhsResolvesRefLevels);
						} else {// if(assignExisting.refCnt >=
								// TypeCheckUtils.getRefLevels(tryingToPassType)){
								// z := "newValue", rhs needs to be a ref - so
								// turn it into a ref and REPLACE the thing on
								// the lhs
							int setTo = assignExisting.refCnt;
							int rhsRefLevels = TypeCheckUtils.getRefLevels(tryingToPassType);
							if (lhsResolvesRefLevels == setTo) {
								// e.g. sdf = [ 0:::, 0:::, 0:::, 0:::]; sdf[3]
								// :::= 9
								rhs.accept(this);
								donullcheckForUnknown(tryingToPassType, null);
								tryingToPassType = convertToRefLevels(setTo, rhsRefLevels, tryingToPassType);
								// Utils.applyArrayStore(this.mv,
								// typeLastIsOperatingOn, this);
								listMapPutFunc(lastARE.mapOperationSignature, (NamedType) typeLastIsOperatingOn, "put");
							} else {
								mapFakeGetterFunc(lastARE.mapOperationSignature, (NamedType) typeLastIsOperatingOn);
								bcoutputter.visitTypeInsn(CHECKCAST, "com/concurnas/runtime/ref/Local");
								rhs.accept(this);
								donullcheckForUnknown(tryingToPassType, null);
								tryingToPassType = convertToRefLevels(setTo, rhsRefLevels, tryingToPassType);
								// now the rhs is the right level of ref
								storeRef(lhsResolvesRefLevels - setTo);
							}

							// TODO: += string concat
						}
					} else {// +=, -= etc
						mapFakeGetterFunc(lastARE.mapOperationSignature, (NamedType) typeLastIsOperatingOn);
						bcoutputter.visitTypeInsn(CHECKCAST, "com/concurnas/runtime/ref/Local");

						loadRefAndDup(retType);

						if (eq == AssignStyleEnum.POW_EQUALS) {
							Utils.applyCastImplicit(bcoutputter, retTypeNoRef, RefDoublePrim, this, true);

							Type rhsType = (Type) rhs.accept(this);
							donullcheckForUnknown(rhsType, null);
							// TypeCheckUtils.unlockAllNestedRefs(rhsType);
							Utils.applyCastImplicit(bcoutputter, rhsType, RefDoublePrim, this, true);
							applyFancyAssignmentToArrayRef(eq, RefDoublePrim);
							Utils.applyCastImplicit(bcoutputter, RefDoublePrim, retTypeNoRef, this, true);
						} else {
							Utils.applyCastImplicit(bcoutputter, retTypeNoRef, retTypeNoRef, this, true);
							PrimativeType primOp = Utils.unbox(bcoutputter, retTypeNoRef, this);

							Type rhsType = (Type) rhs.accept(this);
							donullcheckForUnknown(rhsType, null);
							// TypeCheckUtils.unlockAllNestedRefs(rhsType);
							Utils.applyCastImplicit(bcoutputter, rhsType, primOp, this, true);
							applyFancyAssignmentToArrayRef(eq, primOp);
							Utils.applyCastImplicit(bcoutputter, primOp, retTypeNoRef, this, true);
						}

						bcoutputter.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/ref/DefaultRef", "set", "(Ljava/lang/Object;)V");
					}
				}

			}

		}
		
		if(!refsNeedingDirectAssign.isEmpty()){
			for(Pair<Integer, Integer> entry : refsNeedingDirectAssign){
				bcoutputter.visitVarInsn(ALOAD, entry.getA());
				bcoutputter.visitVarInsn(ALOAD, entry.getB());
				bcoutputter.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/ref/DefaultRef", "set", "(Ljava/lang/Object;)V");
			}
		}

		return retType;
	}

	private Type convertToRefLevels(int setTo, int rhsRefLevels, Type tryingToPassType) {
		if (setTo != rhsRefLevels) {// && (rhsRefLevels ==0 || (rhsRefLevels>0
									// && lhsResolvesRefLevels!=setTo))){
			Type tryingToPassAdjustedRightNum = (Type) (tryingToPassType).copy();
			tryingToPassAdjustedRightNum = TypeCheckUtils.boxTypeIfPrimative(tryingToPassAdjustedRightNum, false);// int
																													// ->
																													// Integer
																													// etc
			if (setTo > rhsRefLevels) {
				for (int n = 0; n < setTo - rhsRefLevels; n++) {
					tryingToPassAdjustedRightNum = new NamedType(0, 0, tryingToPassAdjustedRightNum);
				}
			} else {// extact to wanted type level
				for (int n = 0; n < rhsRefLevels - setTo; n++) {
					tryingToPassAdjustedRightNum = ((NamedType) tryingToPassAdjustedRightNum).getGenTypes().get(0);
				}
			}
			return Utils.applyCastImplicit(bcoutputter, tryingToPassType, tryingToPassAdjustedRightNum, this, false);
		}
		return tryingToPassType;
	}

	private void applyFancyAssignmentToArrayRef(AssignStyleEnum eqStyle, Type loadedTypes) {
		PrimativeType loadedType;
		if (eqStyle == AssignStyleEnum.POW_EQUALS) {
			loadedType = (PrimativeType) Const_PRIM_DOUBLE;
		} else {
			loadedType = Utils.unbox(bcoutputter, loadedTypes, this);
		}

		if (eqStyle == AssignStyleEnum.DIV_EQUALS) {// /=
			Utils.applyMuler(bcoutputter, loadedType, MulerExprEnum.DIV);
		} else if (eqStyle == AssignStyleEnum.MINUS_EQUALS) {// -=
			Utils.applyPlusMinusPrim(this.bcoutputter, false, loadedType);
		} else if (eqStyle == AssignStyleEnum.MUL_EQUALS) {// *=
			Utils.applyMuler(bcoutputter, loadedType, MulerExprEnum.MUL);
		} else if (eqStyle == AssignStyleEnum.MOD_EQUALS) {// %=
			Utils.applyMuler(bcoutputter, loadedType, MulerExprEnum.MOD);
		} else if (eqStyle == AssignStyleEnum.PLUS_EQUALS) {// +=
			Utils.applyPlusMinusPrim(this.bcoutputter, true, loadedType);
			
		} else if (eqStyle == AssignStyleEnum.RSH) {
			Utils.applyShift(bcoutputter, (PrimativeType) loadedType, ShiftOperatorEnum.RS);
		} else if (eqStyle == AssignStyleEnum.LSH) {
			Utils.applyShift(bcoutputter, (PrimativeType) loadedType, ShiftOperatorEnum.LS);
		} else if (eqStyle == AssignStyleEnum.RHSU) {
			Utils.applyShift(bcoutputter, (PrimativeType) loadedType, ShiftOperatorEnum.URS);
			

		} else if (eqStyle == AssignStyleEnum.BAND) {
			Utils.applyBitwise(bcoutputter, (PrimativeType) loadedType, BitwiseOperationEnum.AND);
		} else if (eqStyle == AssignStyleEnum.BOR) {
			Utils.applyBitwise(bcoutputter, (PrimativeType) loadedType, BitwiseOperationEnum.OR);
		} else if (eqStyle == AssignStyleEnum.BXOR) {
			Utils.applyBitwise(bcoutputter, (PrimativeType) loadedType, BitwiseOperationEnum.XOR);
			
			
		} else if (eqStyle == AssignStyleEnum.POW_EQUALS) {// **=
			bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "pow", "(DD)D");
			Utils.applyCastImplicit(bcoutputter, RefDoublePrim, loadedTypes, this);
			return;
		}
		Utils.applyCastImplicit(bcoutputter, loadedType, loadedTypes, this);

	}

	private void applyListFancyAssignmentToArrayRef(AssignStyleEnum eqStyle, PrimativeType loadedType) {//TODO: convert to switch to take advantage of tableswitch operand
		if (eqStyle == AssignStyleEnum.DIV_EQUALS) {// /=
			Utils.applyMuler(bcoutputter, (PrimativeType) loadedType, MulerExprEnum.DIV);
		} else if (eqStyle == AssignStyleEnum.MINUS_EQUALS) {// -=
			Utils.applyPlusMinusPrim(this.bcoutputter, false, (PrimativeType) loadedType);
		} else if (eqStyle == AssignStyleEnum.MUL_EQUALS) {// *=
			Utils.applyMuler(bcoutputter, (PrimativeType) loadedType, MulerExprEnum.MUL);
		} else if (eqStyle == AssignStyleEnum.MOD_EQUALS) {// %=
			Utils.applyMuler(bcoutputter, (PrimativeType) loadedType, MulerExprEnum.MOD);
		} else if (eqStyle == AssignStyleEnum.PLUS_EQUALS) {// +=
			Utils.applyPlusMinusPrim(this.bcoutputter, true, (PrimativeType) loadedType);
			

		} else if (eqStyle == AssignStyleEnum.RSH) {
			Utils.applyShift(bcoutputter, (PrimativeType) loadedType, ShiftOperatorEnum.RS);
		} else if (eqStyle == AssignStyleEnum.LSH) {
			Utils.applyShift(bcoutputter, (PrimativeType) loadedType, ShiftOperatorEnum.LS);
		} else if (eqStyle == AssignStyleEnum.RHSU) {
			Utils.applyShift(bcoutputter, (PrimativeType) loadedType, ShiftOperatorEnum.URS);


		} else if (eqStyle == AssignStyleEnum.BAND) {
			Utils.applyBitwise(bcoutputter, (PrimativeType) loadedType, BitwiseOperationEnum.AND);
		} else if (eqStyle == AssignStyleEnum.BOR) {
			Utils.applyBitwise(bcoutputter, (PrimativeType) loadedType, BitwiseOperationEnum.OR);
		} else if (eqStyle == AssignStyleEnum.BXOR) {
			Utils.applyBitwise(bcoutputter, (PrimativeType) loadedType, BitwiseOperationEnum.XOR);
			
		} else if (eqStyle == AssignStyleEnum.POW_EQUALS) {// **=
			bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "pow", "(DD)D");
			Utils.applyCastImplicit(bcoutputter, RefDoublePrim, loadedType, this);
		}

		//Utils.box(mv, loadedType);
	}

	private boolean isLastThingyAnArraRef(Expression e) {
		if (e instanceof ArrayRef) {
			return true;
		} else if (e instanceof DotOperator) {
			return ((DotOperator) e).getLastElement() instanceof ArrayRef;
		}
		return false;
	}

	private Object doGetSetnormal(GetSetOperation getSetOperation) {
		int line = getSetOperation.getLine();
		int col = getSetOperation.getColumn();

		AssignStyleEnum eqStyle = getSetOperation.incOperation;

		FuncInvoke getterFuncInvoke = new FuncInvoke(line, col, getSetOperation.getter, new FuncInvokeArgs(line, col));
		getterFuncInvoke.resolvedFuncTypeAndLocation = getSetOperation.getterTAL;
		getterFuncInvoke.setTaggedType(getSetOperation.getterTAL.getType());

		Type getterType = ((FuncType) getterFuncInvoke.getTaggedType()).retType;

		boolean lhsStringConcatOperation = TypeCheckUtils.isString(getterType) && eqStyle == AssignStyleEnum.PLUS_EQUALS;

		FuncInvoke setterFuncInvoke = new FuncInvoke(line, col, getSetOperation.setter, new FuncInvokeArgs(line, col));
		setterFuncInvoke.resolvedFuncTypeAndLocation = getSetOperation.setterTAL;
		setterFuncInvoke.setTaggedType(getSetOperation.setterTAL.getType());

		Type setterInputArg = ((FuncType) getSetOperation.setterTAL.getType()).getInputs().get(0);

		bcoutputter.visitInsn(DUP);

		if (lhsStringConcatOperation) {
			if(getSetOperation.ispostfix){//so we can do this: f = cc.x++ | where x is type str of class denoted by cc
				getterFuncInvoke.accept(this);
				if(getSetOperation.getShouldBePresevedOnStack()) {
					bcoutputter.visitInsn(DUP_X1);
				}
			}
			
			bcoutputter.visitTypeInsn(NEW, "java/lang/StringBuilder");
			bcoutputter.visitInsn(DUP_X1);
			bcoutputter.visitInsn(SWAP);
			
			if(!getSetOperation.ispostfix){//so we can do this: f = cc.x++ | where x is type str of class denoted by cc
				getterFuncInvoke.accept(this);
			}
		}
		else {
			getterFuncInvoke.accept(this);
		}
		
		if (lhsStringConcatOperation) {
			bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
			bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
		}

		if (!lhsStringConcatOperation && getSetOperation.ispostfix && getSetOperation.getShouldBePresevedOnStack()) {
			if (getterType.hasArrayLevels()) {
				Utils.applyDupX2(this.bcoutputter, getterType);
			} else {
				Utils.applyDupX1(this.bcoutputter, getterType);
			}
		}

		Type typeAsPrim = getterType;
		boolean requiresBoxing = false;

		if (!lhsStringConcatOperation) {
			typeAsPrim = Utils.unbox(bcoutputter, getterType, this);
			requiresBoxing = !getterType.equals(typeAsPrim);
		}

		if (eqStyle == AssignStyleEnum.POW_EQUALS) {
			Utils.applyCastImplicit(bcoutputter, typeAsPrim, new PrimativeType(PrimativeTypeEnum.DOUBLE), this);
		}

		getSetOperation.toAddMinus.accept(this);

		Type exprType = getSetOperation.toAddMinus.getTaggedType();

		if (eqStyle == AssignStyleEnum.POW_EQUALS) {
			Utils.applyCastImplicit(bcoutputter, exprType, new PrimativeType(PrimativeTypeEnum.DOUBLE), this);
		} else if (!exprType.equals(typeAsPrim)) {
			if (!lhsStringConcatOperation) {// note: you dont need to cast if
											// it's a string concat since it has
											// methods for everything already
											// (and would be stupid)
				Utils.applyCastImplicit(bcoutputter, exprType, typeAsPrim, this);
			}
		}

		if (eqStyle == AssignStyleEnum.DIV_EQUALS) {// /=
			Utils.applyMuler(bcoutputter, (PrimativeType) typeAsPrim, MulerExprEnum.DIV);
		} else if (eqStyle == AssignStyleEnum.MINUS_EQUALS) {// -=
			Utils.applyPlusMinusPrim(this.bcoutputter, false, (PrimativeType) typeAsPrim);
		} else if (eqStyle == AssignStyleEnum.MUL_EQUALS) {// *=
			Utils.applyMuler(bcoutputter, (PrimativeType) typeAsPrim, MulerExprEnum.MUL);
		} else if (eqStyle == AssignStyleEnum.MOD_EQUALS) {// %=
			Utils.applyMuler(bcoutputter, (PrimativeType) typeAsPrim, MulerExprEnum.MOD);
		} else if (eqStyle == AssignStyleEnum.PLUS_EQUALS) {// +=
			if (lhsStringConcatOperation) {
				if (exprType.hasArrayLevels()) {
					StringBuffHelper.append(this, exprType);
				} else {
					String bcType = exprType instanceof NamedType ? "Ljava/lang/Object;" : exprType.getBytecodeType();
					StringBuffHelper.doAppend(this, bcType);
				}
				bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
			} else {
				Utils.applyPlusMinusPrim(this.bcoutputter, true, (PrimativeType) typeAsPrim);
			}
		} else if (eqStyle == AssignStyleEnum.POW_EQUALS) {// **=
			bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "pow", "(DD)D");
			Utils.applyCastImplicit(bcoutputter, RefDoublePrim, typeAsPrim, this);
		}

		if (requiresBoxing && !lhsStringConcatOperation) {
			Utils.box(bcoutputter, (PrimativeType) typeAsPrim);
		}

		if (!getterType.equals(setterInputArg)) {// apply cast to setter input
													// type, you may have int
													// returned from getter but
													// double passed to setter
			Utils.applyCastImplicit(bcoutputter, getterType, setterInputArg, this);
		}

		if (!lhsStringConcatOperation && !getSetOperation.ispostfix && getSetOperation.getShouldBePresevedOnStack()) {
			if (getterType.hasArrayLevels()) {
				Utils.applyDupX2(this.bcoutputter, getterType);
			} else {
				Utils.applyDupX1(this.bcoutputter, getterType);
			}
		}

		if (lhsStringConcatOperation && !getSetOperation.ispostfix && getSetOperation.getShouldBePresevedOnStack()) {//so we can do this: f = ++cc.x | where x is type str of class denoted by cc
			bcoutputter.visitInsn(DUP_X1);
		}		
		setterFuncInvoke.accept(this);

		return getSetOperation.getTaggedType();
	}

	private Object doGetRef(GetSetOperation getSetOperation) {
		// TODO: add better array matrix operations here e.g. matrix *= vector,
		// or [1,2,3] + [2,4] etc etc etc
		int line = getSetOperation.getLine();
		int col = getSetOperation.getColumn();

		AssignStyleEnum eqStyle = getSetOperation.incOperation;

		FuncInvoke getterFuncInvoke = new FuncInvoke(line, col, getSetOperation.getter, new FuncInvokeArgs(line, col));
		getterFuncInvoke.resolvedFuncTypeAndLocation = getSetOperation.getterTAL;
		getterFuncInvoke.setTaggedType(getSetOperation.getterTAL.getType());

		Type getterType = ((FuncType) getterFuncInvoke.getTaggedType()).retType;

		boolean lhsStringConcatOperation = TypeCheckUtils.isString(getterType) && eqStyle == AssignStyleEnum.PLUS_EQUALS;

		FuncInvoke setterFuncInvoke = new FuncInvoke(line, col, getSetOperation.setter, new FuncInvokeArgs(line, col));
		setterFuncInvoke.resolvedFuncTypeAndLocation = getSetOperation.setterTAL;
		setterFuncInvoke.setTaggedType(getSetOperation.setterTAL.getType());

		Type setterInputArg = ((FuncType) getSetOperation.setterTAL.getType()).getInputs().get(0);

		boolean presStack = getSetOperation.getShouldBePresevedOnStack();
		int storeRet = -1;

		Type storeType = getterType;

		bcoutputter.visitInsn(DUP);

		getterFuncInvoke.accept(this);
		// store the origonal thing
		bcoutputter.visitInsn(DUP);
		Type origType = getterType;
		int origSlot = this.createNewLocalVar(this.getTempVarName(), getterType, false);
		Utils.applyStore(bcoutputter, origType, origSlot);

		if (presStack) {
			bcoutputter.visitInsn(DUP);
			if (getSetOperation.getExpectNonRef()) {
				storeType = Utils.unref(bcoutputter, storeType, this);
			}
			storeRet = this.createNewLocalVar(this.getTempVarName(), getterType, false);
			Utils.applyStore(bcoutputter, getterType, storeRet);
		}

		int lvels = TypeCheckUtils.getRefLevels(getterType);
		if (lhsStringConcatOperation) {
			if (lvels > 1) {
				getterType = Utils.unref(bcoutputter, getterType, 1, this, true);
				// Utils.unref(mv, getterType, 0, this, false);
			} else {
				bcoutputter.visitInsn(DUP);
				// Utils.unref(mv, getterType, 0, this, true);
			}
			bcoutputter.visitInsn(DUP);

			Utils.unref(bcoutputter, getterType, this);

			bcoutputter.visitTypeInsn(NEW, "java/lang/StringBuilder");
			bcoutputter.visitInsn(DUP_X1);
			bcoutputter.visitInsn(SWAP);

			bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
			bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
		} else {
			if (lvels > 1) {
				getterType = Utils.unref(bcoutputter, getterType, 1, this, true);
			} else {
				// getterType = TypeChec
				bcoutputter.visitInsn(DUP);
			}

			bcoutputter.visitInsn(DUP);
		}

		Type typeAsPrim = getterType;// Utils.unbox(mv, getterType, this);
		boolean requiresBoxing = false;

		if (!lhsStringConcatOperation) {
			typeAsPrim = Utils.unbox(bcoutputter, getterType, this);
			requiresBoxing = !getterType.equals(typeAsPrim);
		}

		if (eqStyle == AssignStyleEnum.POW_EQUALS) {
			Utils.applyCastImplicit(bcoutputter, typeAsPrim, new PrimativeType(PrimativeTypeEnum.DOUBLE), this);
		}

		getSetOperation.toAddMinus.accept(this);

		Type exprType = getSetOperation.toAddMinus.getTaggedType();

		if (eqStyle == AssignStyleEnum.POW_EQUALS) {
			Utils.applyCastImplicit(bcoutputter, exprType, new PrimativeType(PrimativeTypeEnum.DOUBLE), this);
		} else if (!exprType.equals(typeAsPrim)) {
			if (!lhsStringConcatOperation) {// note: you dont need to cast if
											// it's a string concat since it has
											// methods for everything already
											// (and would be stupid)
				Utils.applyCastImplicit(bcoutputter, exprType, typeAsPrim, this);
			}
		}

		if (eqStyle == AssignStyleEnum.DIV_EQUALS) {// /=
			Utils.applyMuler(bcoutputter, (PrimativeType) typeAsPrim, MulerExprEnum.DIV);
		} else if (eqStyle == AssignStyleEnum.MINUS_EQUALS) {// -=
			Utils.applyPlusMinusPrim(this.bcoutputter, false, (PrimativeType) typeAsPrim);
		} else if (eqStyle == AssignStyleEnum.MUL_EQUALS) {// *=
			Utils.applyMuler(bcoutputter, (PrimativeType) typeAsPrim, MulerExprEnum.MUL);
		} else if (eqStyle == AssignStyleEnum.MOD_EQUALS) {// %=
			Utils.applyMuler(bcoutputter, (PrimativeType) typeAsPrim, MulerExprEnum.MOD);
		} else if (eqStyle == AssignStyleEnum.PLUS_EQUALS) {// +=
			if (lhsStringConcatOperation) {
				if (exprType.hasArrayLevels()) {
					StringBuffHelper.append(this, exprType);
				} else {
					String bcType = exprType instanceof NamedType ? "Ljava/lang/Object;" : exprType.getBytecodeType();
					StringBuffHelper.doAppend(this, bcType);
				}
				bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
			} else {
				Utils.applyPlusMinusPrim(this.bcoutputter, true, (PrimativeType) typeAsPrim);
			}
		} else if (eqStyle == AssignStyleEnum.POW_EQUALS) {// **=
			bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "pow", "(DD)D");
			Utils.applyCastImplicit(bcoutputter, RefDoublePrim, typeAsPrim, this);
		}

		if (requiresBoxing && !lhsStringConcatOperation) {
			typeAsPrim = Utils.box(bcoutputter, (PrimativeType) typeAsPrim);
		}

		bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/runtime/ref/Local", "set", "(Ljava/lang/Object;)V");

		bcoutputter.visitInsn(POP);

		// load the orig again and set
		Utils.applyLoad(bcoutputter, origType, origSlot);
		setterFuncInvoke.accept(this);

		if (storeRet > -1) {
			Utils.applyLoad(bcoutputter, storeType, storeRet);
		}

		return storeType;// getSetOperation.getTaggedType();

	}

	@Override
	public Object visit(GetSetOperation getSetOperation) {
		// TODO: add better array matrix operations here e.g. matrix *= vector,
		// or [1,2,3] + [2,4] etc etc etc
		int line = getSetOperation.getLine();
		int col = getSetOperation.getColumn();
		FuncInvoke getterFuncInvoke = new FuncInvoke(line, col, getSetOperation.getter, new FuncInvokeArgs(line, col));
		getterFuncInvoke.resolvedFuncTypeAndLocation = getSetOperation.getterTAL;
		getterFuncInvoke.setTaggedType(getSetOperation.getterTAL.getType());
		Type getterType = ((FuncType) getterFuncInvoke.getTaggedType()).retType;
		boolean isRef = TypeCheckUtils.hasRefLevels(getterType);

		return isRef ? doGetRef(getSetOperation) : doGetSetnormal(getSetOperation);
	}

	private boolean getActingOnArrayRef(Expression e) {
		e = TypeCheckUtils.checkCanBePrePostfiexed(this, e);

		if (e != null && e instanceof ArrayRef) {
			ArrayRef thing = (ArrayRef) e;
			Type taggedd = thing.expr.getTaggedType();
			return thing.dupLastThingIfRef && TypeCheckUtils.hasRefLevels(taggedd) && TypeCheckUtils.isRefDirectlySettable(taggedd, -1);
		}
		return false;
	}

	@Override
	public Object visit(PostfixOp postfixOp) {
		// prepostOps[0]++
		// JPT: WORK HERE lazy

		Expression thingOperatedOn = postfixOp.p2;

		// TopOfStack top = this.topOfStack;

		boolean isArrayRef = isLastThingyAnArraRef(thingOperatedOn);
		boolean isFieldOperation = false;

		Type type = thingOperatedOn.getTaggedType();
		
		
		boolean actsOnRef = TypeCheckUtils.hasRefLevels(type);

		// a := 5; b = a++ //b should be 5, so we have to dupe the previous
		// value contained in the ref
		// note that b := a++// b will still be 6 (well, 6:) because geting
		// pointer to ref
		boolean expectNonRefPreservedOnStack = postfixOp.getExpectNonRef() && postfixOp.getShouldBePresevedOnStack() && actsOnRef;

		if (isArrayRef) {
			if (!actsOnRef) {// keep ref on stack
				thingOperatedOn.setDuplicateOnStack(true);
			}
		} else if (thingOperatedOn instanceof RefName) {
			RefName rf = (RefName) thingOperatedOn;
			Location loc = rf.resolvesTo == null ? null : rf.resolvesTo.getLocation();
			if (null != loc && loc instanceof LocationClassField) {
				if (!TypeCheckUtils.hasRefLevels(thingOperatedOn.getTaggedType())) {
					prefixNodeWithThisreference(loc, true);
				}

				// rf.setPreceededByThis(true);
				
				isFieldOperation = true;
			}
		}

		if (thingOperatedOn instanceof DotOperator) {
			DotOperator dotop = (DotOperator) thingOperatedOn;
			// we know this must resolve to either an arrayref or a field
			Expression lastThing = dotop.getLastElement();
			boolean fieldOp = false;
			if (lastThing instanceof RefName) {
				RefName rf = (RefName) lastThing;
				Location loc = rf.resolvesTo == null ? null : rf.resolvesTo.getLocation();
				if (null != loc && loc instanceof LocationClassField) {
					fieldOp = true;
				}
			}
			// we know this must resolve to either an arrayref or a field
			this.processDotOperator(dotop, !isArrayRef && fieldOp && !TypeCheckUtils.hasRefLevels(thingOperatedOn.getTaggedType()), false);
			thingOperatedOn = dotop.getLastElement();
			thingOperatedOn.setDuplicateOnStack(isArrayRef);
			isFieldOperation = !isArrayRef;
			// isArrayRef = thingOperatedOn instanceof ArrayRef;
			
			boolean includePop = !(dotop.getPenultimate() instanceof RefName);
			
			//clean up stack as statuc call therefore we dont need whats on the lhs...
			if(thingOperatedOn instanceof FuncInvoke) {
				if(((FuncInvoke)thingOperatedOn).resolvedFuncTypeAndLocation.getLocation() instanceof StaticFuncLocation) {
					if(includePop) {
						bcoutputter.visitInsn(POP);
					}
					isFieldOperation = false;
				}
			}else if(thingOperatedOn instanceof RefName) {
				if(((RefName)thingOperatedOn).resolvesTo.getLocation() instanceof LocationStaticField) {
					//if(!this.dorOpLHS.isEmpty() && this.dorOpLHS.peek() != null) {
					if(includePop) {
						bcoutputter.visitInsn(POP);
					}
					//}
					isFieldOperation = false;
				}
			}
		}

		type = (Type) thingOperatedOn.accept(this);
		boolean actingOnArrayRef = getActingOnArrayRef(thingOperatedOn);

		if (postfixOp.getShouldBePresevedOnStack()) {
			if (isArrayRef) {
				if (!actsOnRef) {
					Utils.applyDupX2(this.bcoutputter, type);
				}
				//
			} else if (isFieldOperation) {
				if (!actsOnRef) {
					Utils.applyDupX1(this.bcoutputter, type);
				} else if (expectNonRefPreservedOnStack) {
					bcoutputter.visitInsn(DUP);
					Utils.unref(bcoutputter, type, this);
					bcoutputter.visitInsn(SWAP);// put the ref back on the top...
					// mv.visitInsn(DUP);
				} else {
					bcoutputter.visitInsn(DUP);
					// mv.visitInsn(DUP);
				}
			} else {
				Utils.applyDup(this.bcoutputter, type);

				if (expectNonRefPreservedOnStack) {
					Utils.unref(bcoutputter, type, this);
					bcoutputter.visitInsn(SWAP);// put the ref back on the top...
				}

			}
		}

		TypeCheckUtils.unlockAllNestedRefs(type);
		if (actsOnRef) {
			if (isArrayRef) {// && !TypeCheckUtils.hasRefLevels(type)){
				if (!TypeCheckUtils.hasRefLevels(type)) {
					bcoutputter.visitInsn(DUP);
				}
				if (postfixOp.getShouldBePresevedOnStack()) {// this thing gets
																// returned
					bcoutputter.visitInsn(DUP);
					if (expectNonRefPreservedOnStack) {
						Utils.unref(bcoutputter, type, this);
						bcoutputter.visitInsn(SWAP);// put the ref back on the top...
					}
				}
			}
			// mv.visitInsn(DUP);
			// type = Utils.unref(mv, type, this);//to be unrefed again...
			type = Utils.unref(bcoutputter, type, 0, this, true);// to be unrefed
														// again...

			// here is the problem

		}

		boolean requiresBoxing = false;
		
		if(postfixOp.astOverrideOperatorOverload != null){
			type = (Type)postfixOp.astOverrideOperatorOverload.accept(this);
			
		}
		else{
			PrimativeType typeAsPrim = Utils.unbox(bcoutputter, type, this);
			requiresBoxing = !type.equals(typeAsPrim);
			FactorPostFixEnum operation = postfixOp.postfix;// -> MINUSMINUS,
															// PLUSPLUS

			if (operation == FactorPostFixEnum.MINUSMINUS) {
				switch (typeAsPrim.type) {
				case LONG:
					bcoutputter.visitInsn(LCONST_1);
					bcoutputter.visitInsn(LSUB);
					break;
				case FLOAT:
					bcoutputter.visitInsn(FCONST_1);
					bcoutputter.visitInsn(FSUB);
					break;
				case DOUBLE:
					bcoutputter.visitInsn(DCONST_1);
					bcoutputter.visitInsn(DSUB);
					break;
				default:
					bcoutputter.visitInsn(ICONST_1);
					bcoutputter.visitInsn(ISUB);
					break;// JPT: this is more efficient: mv.visitIincInsn(1, -1);
							// break;
				}
			} else// PLUSPLUS
			{
				switch (typeAsPrim.type) {
				case LONG:
					bcoutputter.visitInsn(LCONST_1);
					bcoutputter.visitInsn(LADD);
					break;
				case FLOAT:
					bcoutputter.visitInsn(FCONST_1);
					bcoutputter.visitInsn(FADD);
					break;
				case DOUBLE:
					bcoutputter.visitInsn(DCONST_1);
					bcoutputter.visitInsn(DADD);
					break;
				default:
					bcoutputter.visitInsn(ICONST_1);
					bcoutputter.visitInsn(IADD);
					break; // JPT: this is more efficient: mv.visitIincInsn(1, 1);
							// break;
				}
			}
			type = typeAsPrim;
		}
		

		if (requiresBoxing) {
			type = Utils.box(bcoutputter, type);
		}
		

		if (thingOperatedOn instanceof ArrayRef) {
			if (actsOnRef ) {
				bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/runtime/ref/Local", "set", "(Ljava/lang/Object;)V");
				// mv.visitInsn(POP);
			} else {
				// ok for arrays, but what about lists and maps???
				ArrayRef asArra = (ArrayRef) thingOperatedOn;
				Thruple<Type, Type, ARElementType> lastStuff = asArra.getLastTaggedType();
				ARElementType are = lastStuff.getC();

				if (are == ARElementType.LIST) {
					ArrayRefElement lastARE = asArra.getLastArrayRefElement();
					listMapPutFunc(lastARE.mapOperationSignature, (NamedType) lastARE.mapTypeOperatingOn, "set", type);
					// mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List",
					// "set", "(ILjava/lang/Object;)Ljava/lang/Object;");
					// mv.visitInsn(POP);
				} else if (are == ARElementType.MAP || are == ARElementType.OBJ) {
					ArrayRefElement lastARE = asArra.getLastArrayRefElement();
					
					if(lastARE.astOverrideOperatorOverload != null) {
						lastARE.astOverrideOperatorOverload.accept(this);
					}else {
						listMapPutFunc(lastARE.mapOperationSignature, (NamedType) lastARE.mapTypeOperatingOn, "put", type);
					}
					
					//actingOnArrayRef=false;
				} 
				else {// map
					int levels = type.getArrayLevels();
					type.setArrayLevels(levels + 1);
					Utils.applyArrayStore(bcoutputter, type, this);
					type.setArrayLevels(levels);
				}
				// TODO: map

				/*if(thingOperatedOn instanceof ArrayRef){
					type = ((ArrayRef)thingOperatedOn).expr.getTaggedType();
					if(TypeCheckUtils.hasArrayRefLevels(type)){
						mv.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/runtime/ref/Local", "set", "(Ljava/lang/Object;)V");
					}
				}*/
				
				if (actingOnArrayRef && are != ARElementType.OBJ) {
					if (postfixOp.getShouldBePresevedOnStack()) {// this thing
																	// gets
																	// returned
						bcoutputter.visitInsn(DUP_X2);
						bcoutputter.visitInsn(POP);
					}

					bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/runtime/ref/Local", "set", "(Ljava/lang/Object;)V");
				}
				
				else{
					if(postfixOp.getShouldBePresevedOnStack()){//if expcted to return stuff to stack
						Utils.applyCastImplicit(bcoutputter, type, postfixOp.getTaggedType(), this);//b=++c1['hi5'] - b should be original type of c1 getter
					}
				}

			}
		} else {
			if (!actsOnRef) {
				Expression eee = TypeCheckUtils.checkCanBePrePostfiexed(this, null, thingOperatedOn, 42, 42);
				assert eee instanceof RefName;

				RefName eeeRN = (RefName) eee;
				TypeAndLocation lat = eeeRN.resolvesTo;
				String name = eeeRN.name;
				storeLocalVaraible(name, lat, type/*, null*/);
			} else {
				bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/runtime/ref/Local", "set", "(Ljava/lang/Object;)V");
			}
		}
		
		return postfixOp.getTaggedType();
	}

	private void applyPrefixDup(Type type, boolean isArrayRef, boolean isFieldOperation) {
		if (isArrayRef) {
			// if(!((ArrayRef)thingOperatedOn).isList){
			Utils.applyDupX2(this.bcoutputter, type);
			// }
		} else if (isFieldOperation) {
			Utils.applyDupX1(this.bcoutputter, type);
		} else {
			Utils.applyDup(this.bcoutputter, type);
		}
	}

	@Override
	public Object visit(PrefixOp prefixOp) {
		if(resolvedViaFoldedConstant(prefixOp.getFoldedConstant(), prefixOp.getTaggedType())){
			return prefixOp.getTaggedType(); 
		}
		// TODO: maps, lists and things that have the isMappable mixin

		// TODO: ++xyz.d3a2// where d3a2 is a ref :- this should not call
		// setField on xyz [not needed]

		Expression thingOperatedOn = prefixOp.p1;

		boolean isArrayRef = isLastThingyAnArraRef(thingOperatedOn);
		boolean isFieldOperation = false;

		Type type = thingOperatedOn.getTaggedType();
		boolean actsOnRef = TypeCheckUtils.hasRefLevels(type);

		boolean actingOnArrayRef = getActingOnArrayRef(thingOperatedOn);

		if (isArrayRef) {
			if (!actsOnRef) {// keep ref on stack
				thingOperatedOn.setDuplicateOnStack(prefixOp.prefix.isDoubleThing);
			}
		} else if (thingOperatedOn instanceof RefName) {
			RefName rf = (RefName) thingOperatedOn;
			Location loc = rf.resolvesTo == null ? null : rf.resolvesTo.getLocation();
			if (null != loc && loc instanceof LocationClassField) {
				boolean precededByThis = prefixNodeWithThisreference(loc, true);
				rf.setPreceededByThis(precededByThis);
				if (precededByThis && prefixOp.prefix.isDoubleThing) {
					 bcoutputter.visitInsn(DUP);
				}
				/*if (prefixOp.getShouldBePresevedOnStack()) {
					// mv.visitInsn(DUP);
				}*/
				isFieldOperation = true;
			}
		}

		if (thingOperatedOn instanceof DotOperator) {
			DotOperator dotop = (DotOperator) thingOperatedOn;
			// we know this must resolve to either an arrayref or a field
			Expression lastThing = dotop.getLastElement();
			boolean fieldOp = false;
			if (lastThing instanceof RefName) {
				RefName rf = (RefName) lastThing;
				Location loc = rf.resolvesTo == null ? null : rf.resolvesTo.getLocation();
				if (null != loc && loc instanceof LocationClassField) {
					fieldOp = true;
				}
			}

			this.processDotOperator(dotop, !isArrayRef && fieldOp, false);
			thingOperatedOn = lastThing;
			thingOperatedOn.setDuplicateOnStack(isArrayRef);
			isFieldOperation = !isArrayRef;
			// isArrayRef = thingOperatedOn instanceof ArrayRef;

			boolean includePop = !(dotop.getPenultimate() instanceof RefName);
			
			//clean up stack as statuc call therefore we dont need whats on the lhs...
			if(thingOperatedOn instanceof FuncInvoke) {
				if(((FuncInvoke)thingOperatedOn).resolvedFuncTypeAndLocation.getLocation() instanceof StaticFuncLocation) {
					if(includePop) {
						bcoutputter.visitInsn(POP);
					}
					isFieldOperation = false;
				}
			}else if(thingOperatedOn instanceof RefName) {
				if(((RefName)thingOperatedOn).resolvesTo.getLocation() instanceof LocationStaticField) {
					//if(!this.dorOpLHS.isEmpty() && this.dorOpLHS.peek() != null) {
					if(includePop) {
						bcoutputter.visitInsn(POP);
					}
					//}
					isFieldOperation = false;
				}
			}
			
		}

		type = (Type) thingOperatedOn.accept(this);
		TypeCheckUtils.unlockAllNestedRefs(type);
		actsOnRef = TypeCheckUtils.hasRefLevels(type);

		FactorPrefixEnum operation = prefixOp.prefix;// -> MINUSMINUS, PLUSPLUS, NEG, PLUS
		if (actsOnRef) {
			if(operation.isDoubleThing) {
				if (prefixOp.getShouldBePresevedOnStack()) {
					if (!isArrayRef) {
						applyPrefixDup(type, isArrayRef, isFieldOperation);
					}
				}

				if (isArrayRef) {
					bcoutputter.visitInsn(DUP);
					if (prefixOp.getShouldBePresevedOnStack()) {// this thing gets returned
						bcoutputter.visitInsn(DUP);
					}
				}
			}

			type = Utils.unref(bcoutputter, type, this);
		}
		
		PrimativeType typeAsPrim = Utils.unbox(bcoutputter, type, this);
		boolean requiresBoxing = !type.equals(typeAsPrim);


		if (!operation.isDoubleThing /*== FactorPrefixEnum.NEG || operation == FactorPrefixEnum.PLUS || operation == FactorPrefixEnum.COMP*/) {
			if(prefixOp.astOverrideOperatorOverload != null){
				prefixOp.astOverrideOperatorOverload.accept(this);
			}
			else{
				if(operation == FactorPrefixEnum.COMP){
					switch (typeAsPrim.type) {
						case LONG:
							bcoutputter.visitLdcInsn(new Long(-1L));
							bcoutputter.visitInsn(LXOR);
							break;
						default:
							bcoutputter.visitInsn(ICONST_M1);
							bcoutputter.visitInsn(IXOR);
							typeAsPrim = ScopeAndTypeChecker.const_int;
							type = ScopeAndTypeChecker.const_integer_nt;
							break;
					}
				}else{
					switch (typeAsPrim.type) {
						case LONG:
							bcoutputter.visitInsn(LNEG);
							break;
						case FLOAT:
							bcoutputter.visitInsn(FNEG);
							break;
						case DOUBLE:
							bcoutputter.visitInsn(DNEG);
							break;
						default:
							bcoutputter.visitInsn(INEG);
							break;
					}
				}
			}
			
			if (requiresBoxing) {
				type = Utils.box(bcoutputter, typeAsPrim);
			}
		} else {
			
			if(prefixOp.astOverrideOperatorOverload != null){
				type = (Type)prefixOp.astOverrideOperatorOverload.accept(this);
			}
			else{
				type = typeAsPrim;
				if (operation == FactorPrefixEnum.MINUSMINUS) {
					
					switch (typeAsPrim.type) {
					case LONG:
						bcoutputter.visitInsn(LCONST_1);
						bcoutputter.visitInsn(LSUB);
						break;
					case FLOAT:
						bcoutputter.visitInsn(FCONST_1);
						bcoutputter.visitInsn(FSUB);
						break;
					case DOUBLE:
						bcoutputter.visitInsn(DCONST_1);
						bcoutputter.visitInsn(DSUB);
						break;
					default:
						bcoutputter.visitInsn(ICONST_1);
						bcoutputter.visitInsn(ISUB);
						break;// JPT: this is more efficient: mv.visitIincInsn(1,
								// -1); break;
					}
				} else// PLUSPLUS
				{
					switch (typeAsPrim.type) {
					case LONG:
						bcoutputter.visitInsn(LCONST_1);
						bcoutputter.visitInsn(LADD);
						break;
					case FLOAT:
						bcoutputter.visitInsn(FCONST_1);
						bcoutputter.visitInsn(FADD);
						break;
					case DOUBLE:
						bcoutputter.visitInsn(DCONST_1);
						bcoutputter.visitInsn(DADD);
						break;
					default:
						bcoutputter.visitInsn(ICONST_1);
						bcoutputter.visitInsn(IADD);
						break; // JPT: this is more efficient: mv.visitIincInsn(1,
								// 1); break;
					}
				}
			}
			
			if (requiresBoxing) {
				type = Utils.box(bcoutputter, type);
			}

			if (!actsOnRef && prefixOp.getShouldBePresevedOnStack()) {
				applyPrefixDup(type, isArrayRef, isFieldOperation);
			}

			if (thingOperatedOn instanceof ArrayRef) {

				if (actsOnRef) {
					storeRef(TypeCheckUtils.getRefLevels(thingOperatedOn.getTaggedType()));

					// mv.visitMethodInsn(INVOKEVIRTUAL,
					// "com/concurnas/runtime/ref/Local", "set",
					// "(Ljava/lang/Object;)V");
					// mv.visitInsn(POP);
				} else {
					ArrayRef asArra = (ArrayRef) thingOperatedOn;
					Thruple<Type, Type, ARElementType> lastStuff = asArra.getLastTaggedType();
					ARElementType are = lastStuff.getC();

					if (are == ARElementType.LIST) {
						ArrayRefElement lastARE = asArra.getLastArrayRefElement();
						listMapPutFunc(lastARE.mapOperationSignature, (NamedType) lastARE.mapTypeOperatingOn, "set", type);
						// mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List",
						// "set", "(ILjava/lang/Object;)Ljava/lang/Object;");
						// mv.visitInsn(POP);
					} else if (are == ARElementType.MAP || are == ARElementType.OBJ ) {
						ArrayRefElement lastARE = asArra.getLastArrayRefElement();
						listMapPutFunc(lastARE.mapOperationSignature, (NamedType) lastARE.mapTypeOperatingOn, "put", type);
						//actingOnArrayRef=false;
					} else {// map
						int levels = type.getArrayLevels();
						type.setArrayLevels(levels + 1);
						Utils.applyArrayStore(bcoutputter, type, this);
						type.setArrayLevels(levels);
					}

					if (actingOnArrayRef && are != ARElementType.OBJ) {
						if (prefixOp.getShouldBePresevedOnStack()) {// this
																	// thing
																	// gets
																	// returned
							bcoutputter.visitInsn(DUP_X2);
							bcoutputter.visitInsn(POP);
						}

						bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/runtime/ref/Local", "set", "(Ljava/lang/Object;)V");
					}
					else{
						if(prefixOp.getShouldBePresevedOnStack()){//if expcted to return stuff to stack
							Utils.applyCastImplicit(bcoutputter, type, prefixOp.getTaggedType(), this);//b=++c1['hi5'] - b should be original type of c1 getter
						}
					}
				}
			} else {// refname
				Expression eee = TypeCheckUtils.checkCanBePrePostfiexed(this, null, thingOperatedOn, 42, 42);
				assert eee instanceof RefName;

				RefName eeeRN = (RefName) eee;
				TypeAndLocation lat = eeeRN.resolvesTo;
				String name = eeeRN.name;
				storeLocalVaraible(name, lat, type/*, null*/);
				// Utils.applyStore(mv, type,
				// this.getLocalVar(((RefName)eee).name).getB());
			}

			// TODO: lists and maps see above
		}

		
		return prefixOp.getTaggedType();
	}

	/*
	 * private class ForBlockPreamble {//of use to for's only private final
	 * BytecodeVisitor self; private final Expression check; private final
	 * Expression postEpr; private final Label onDone; private Label
	 * gotoStartWhile; private final Assign assign; private final Label
	 * gotoJustBeforeCheck;
	 * 
	 * public ForBlockPreamble(Label onDone, Label gotoJustBeforeCheck,
	 * BytecodeVisitor self, Expression check, Expression postEpr, Label
	 * gotoStartWhile, Assign assign) { this.onDone = onDone;
	 * this.gotoJustBeforeCheck = gotoJustBeforeCheck; this.self = self;
	 * this.check = check; this.postEpr = postEpr; this.gotoStartWhile =
	 * gotoStartWhile; this.assign = assign; }
	 * 
	 * public void enter() { if(null != assign) { this.assign.accept(this.self);
	 * 
	 * //this.gotoStartWhile = new Label(); mv.visitLabel(gotoStartWhile);
	 * //mv.lastLabelVisited = gotoStartWhile; }
	 * 
	 * if(null != this.postEpr) { owhileStart.push(this.gotoJustBeforeCheck); }
	 * else { owhileStart.push(this.gotoStartWhile); }
	 * 
	 * if(null != this.check) { processBooleanOperation(this.check, onDone,
	 * true); } }
	 * 
	 * public void exit() { if(null != this.postEpr) {
	 * mv.visitLabel(this.gotoJustBeforeCheck); LineHolder lh = new
	 * LineHolder(this.postEpr.getLine(), this.postEpr.getColumn(), new
	 * DuffAssign(this.postEpr.getLine(), this.postEpr.getColumn(),
	 * this.postEpr)); lh.accept(self); }
	 * 
	 * mv.visitJumpInsn(GOTO, gotoStartWhile); } }
	 */

	/*
	 * @Override public Object visit(ForBlockOld forBlockOld) { Label
	 * gotoStartWhile = mv.lastLabelVisited;
	 * 
	 * Assign ass = null; if(null != forBlockOld.assignExpr) { ass = new
	 * DuffAssign(forBlockOld.postExpr.getLine(),
	 * forBlockOld.postExpr.getColumn(), forBlockOld.assignExpr); } else if(null
	 * != forBlockOld.assignName) {// (name1=NAME type1=type? '='
	 * assign1=expr_stmt) | expr1=expr_stmt ?
	 * 
	 * if(forBlockOld.assigType != null) { AssignNew assn = new AssignNew(null,
	 * forBlockOld.getLine(), forBlockOld.getColumn(), false, false,
	 * forBlockOld.assignName, null, forBlockOld.assigType,
	 * AssignStyleEnum.EQUALS, forBlockOld.assigFrom); assn.isReallyNew =true;
	 * ass = assn; ass.isTempVariableAssignment=true; } else { AssignExisting
	 * asse = new AssignExisting(forBlockOld.getLine(), forBlockOld.getColumn(),
	 * new RefName(forBlockOld.getLine(), forBlockOld.getColumn(),
	 * forBlockOld.assignName), forBlockOld.assignStyle, forBlockOld.assigFrom);
	 * asse.isReallyNew = forBlockOld.fiddleIsNew;
	 * asse.setTaggedType(forBlockOld.fiddleNewVarType); ass = asse;
	 * ass.isTempVariableAssignment=true; }
	 * 
	 * } //for(n int = 0; n< array.length; n++) Label gotoJustBeforeCheck=new
	 * Label(); //nextblockPreamble = new
	 * ForBlockPreamble(forBlockOld.getLabelAfterCode(), gotoJustBeforeCheck,
	 * this, forBlockOld.check, forBlockOld.postExpr, gotoStartWhile, ass);
	 * 
	 * forBlockOld.block.accept(this);
	 * 
	 * return null; //TODO: enhance by permitting for loop to return stuff }
	 */

	@Override
	public Object visit(ForBlockOld forBlockOld) {
		// for(n=0; n<= 10; n++){
		// assign | check | postExpr || block

		Label afterElseBlock = forBlockOld.elseblock != null?new Label():null;
		
		int slotForVisitCounterBool=-1;
		if(afterElseBlock != null){
			bcoutputter.visitInsn(ICONST_0);
			slotForVisitCounterBool = this.createNewLocalVar(this.getTempVarName(), Const_PRIM_BOOl, true);
		}
		
		Assign ass = null;
		if (null != forBlockOld.assignExpr) {
			ass = new DuffAssign(forBlockOld.postExpr.getLine(), forBlockOld.postExpr.getColumn(), forBlockOld.assignExpr);
		} else if (null != forBlockOld.assignName) {// (name1=NAME type1=type?
													// '=' assign1=expr_stmt) |
													// expr1=expr_stmt ?

			if (forBlockOld.assigType != null) {
				AssignNew assn = new AssignNew(null, forBlockOld.getLine(), forBlockOld.getColumn(), false, false, forBlockOld.assignName, null, forBlockOld.assigType, AssignStyleEnum.EQUALS, forBlockOld.assigFrom);
				assn.isReallyNew = true;
				ass = assn;
				ass.isTempVariableAssignment = true;
			} else {
				AssignExisting asse = new AssignExisting(forBlockOld.getLine(), forBlockOld.getColumn(), new RefName(forBlockOld.getLine(), forBlockOld.getColumn(), forBlockOld.assignName), forBlockOld.assignStyle, forBlockOld.assigFrom);
				asse.isReallyNew = forBlockOld.fiddleIsNew;
				asse.setTaggedType(forBlockOld.fiddleNewVarType);
				ass = asse;
				ass.isTempVariableAssignment = true;
			}

		}
		if (null != ass) {
			ass.accept(this);
		}
		
		if(forBlockOld.idxExpression != null){
			forBlockOld.idxExpression.accept(this);
			//this.createNewLocalVar(forBlockOld.idxExpression.name, forBlockOld.idxExpression.type, true);
		}
		
		Type retType = forBlockOld.getTaggedType();

		int presistResultOnStack = -1;
		if (forBlockOld.getShouldBePresevedOnStack()) {
			
			/*if(retType.hasArrayLevels()) {
				String varname = forBlockOld.newforTmpVar;
				
				loadLocalVar(varname, null);
				mv.visitInsn(ARRAYLENGTH);
				//Utils.singleLevelArrayConst(mv, retType);
				Utils.createArray(mv, retType.getArrayLevels(), retType, true);
				
			}else {*/
				bcoutputter.visitTypeInsn(NEW, "java/util/LinkedList");
				bcoutputter.visitInsn(DUP);
				bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/util/LinkedList", "<init>", "()V");
			//}

			String tempName = this.getTempVarName();
			presistResultOnStack = this.createNewLocalVar(tempName, retType, true);
			slotForBreakContinueInLoop.push(presistResultOnStack);
		}

		Label startOfCheck = null;

		boolean hasCheck = forBlockOld.check != null;

		Label afterFor = null;

		if (hasCheck) {
			if (!forBlockOld.block.hasRetExcepOrBreaked) {
				startOfCheck = new Label();
				// if(!forBlockOld.skipGotoStart){
				bcoutputter.visitJumpInsn(GOTO, startOfCheck);
				// }
			} else {
				afterFor = presistResultOnStack > -1 ? forBlockOld.getLabelBeforeRetLoadIfStackPrese() : new Label();
				processBooleanOperation(forBlockOld.check, afterFor, true);// inverted
																			// lol
			}
		}

		Label startOfBlock = forBlockOld.startOfWorkBlock;
		bcoutputter.visitLabel(startOfBlock);
		
		/*if(null != retType && retType.hasArrayLevels()) {
			//array, slot, valuetostore
			Utils.applyLoad(mv, retType, presistResultOnStack);
			loadLocalVar(forBlockOld.assignName, null);
		}
		*/
		Type forBlockType = (Type) forBlockOld.block.accept(this);

		if(afterElseBlock != null){
			bcoutputter.visitInsn(ICONST_1);
			bcoutputter.visitVarInsn(ISTORE, slotForVisitCounterBool);
		}
		
		
		// forBlockOld.block.

		if (presistResultOnStack > -1 && !forBlockOld.block.hasDefoBrokenOrContinued) {
			bcoutputter.visitLabel(forBlockOld.beforeAdder);
			
			/*if(null != retType && retType.hasArrayLevels()) {
				//array, slot, valuetostore
				Utils.applyArrayStore(mv, retType, this, false);
			}else {*/
				forBlockType = Utils.unref(bcoutputter, forBlockType, TypeCheckUtils.getRefTypeToLocked(forBlockType), this);
				Type from = Utils.box(bcoutputter, forBlockType);
				
				Utils.applyLoad(bcoutputter, retType, presistResultOnStack);
				Utils.genericSswap(bcoutputter, from, retType);
				bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/util/LinkedList", "add", "(Ljava/lang/Object;)Z");
				bcoutputter.visitInsn(POP);
			//}
		}

		if (!forBlockOld.block.hasRetExcepOrBreaked) {
			if (null != forBlockOld.postExpr) {
				bcoutputter.visitLabel(forBlockOld.startOfPostOp);
				// ((Node)forBlockOld.postExpr).setShouldBePresevedOnStack(false);
				isLastLineInBlock.add(false); // MHA: ugly line here
				new DuffAssign(forBlockOld.postExpr.getLine(), forBlockOld.postExpr.getColumn(), forBlockOld.postExpr).accept(this);
				
				if(forBlockOld.postIdxIncremement != null){
					forBlockOld.postIdxIncremement.accept(this);
				}
				
				isLastLineInBlock.pop();
			}

			if (hasCheck) {
				bcoutputter.visitLabel(startOfCheck);
				processBooleanOperation(new NotExpression(forBlockOld.getLine(), forBlockOld.getColumn(), forBlockOld.check), startOfBlock, true);// inverted
																																					// lol
				// processBooleanOperation( forBlockOld.check, startOfBlock,
				// true);//inverted lol
			} else {
				if (!forBlockOld.defoEndsInGotoStmtAlready) {
					if (!forBlockOld.skipGotoStart) {
						bcoutputter.visitJumpInsn(GOTO, startOfBlock);
					}
				}
			}
		}

		if (null != afterFor) {
			bcoutputter.pushNextLabel(afterFor);
		}
		
		if(afterElseBlock != null){
			Label elseBlockEntry = new Label();
			bcoutputter.visitVarInsn(ILOAD, slotForVisitCounterBool);
			Label ifCount = presistResultOnStack > -1?forBlockOld.getLabelBeforeRetLoadIfStackPrese():afterElseBlock;
			bcoutputter.visitJumpInsn(IFNE, ifCount);
			forBlockOld.elseblock.setLabelOnEntry(elseBlockEntry);
			bcoutputter.visitJumpInsn(GOTO, elseBlockEntry);	
		}
		
		
		if (presistResultOnStack > -1) {
			if (forBlockOld.getShouldBePresevedOnStackAndImmediatlyUsed()) {
				bcoutputter.visitLabel(forBlockOld.getLabelBeforeRetLoadIfStackPrese());
			}

			Utils.applyLoad(bcoutputter, retType, presistResultOnStack);
			//mv.visitInsn(POP);
			
			if(null != afterElseBlock){
				bcoutputter.visitJumpInsn(GOTO, afterElseBlock);	
			}
		}
		
		if(afterElseBlock != null){
			bcoutputter.visitLabel(forBlockOld.elseblock.getLabelOnEntry());
			forBlockOld.elseblock.accept(this); 
			bcoutputter.visitLabel(afterElseBlock);
		}
		
		
		return retType;
		/*
		 * fun doings() String { xxx = 0 for(n=0; n<= 10; n++){ xxx+=n } return
		 * ""// + xxx } doings() : String L0 ICONST_0 ISTORE 1 L1 ICONST_0 - ass
		 * ISTORE 2 L2 GOTO L3 --jump to check L4 ILOAD 1: xxx ILOAD 2: n IADD
		 * ISTORE 1: xxx L5 IINC 2: n 1 L3 ILOAD 2: n BIPUSH 10 IF_ICMPLE L4 L6
		 * LDC "" ARETURN
		 */
	}

	@Override
	public Object visit(ForBlock forBlock) {
		// new style

		// translate the new for block into an old one - note that there are two
		// inputs: iterator and arrays - only these can be interated over in the
		// new for loop

		// FTF: For The Future - when u introduce the dynamic type - u will have
		// to do an intanceof to see if the dynamic is an array or
		// a-thing-with-iterator and choose the approperiate bytecode from the
		// below

		// for(localVar optionalType in rhsExpr)
		Expression rhsExpr = forBlock.expr;
		Type exprType = rhsExpr.getTaggedType();
		//

		int line = forBlock.getLine();
		int col = forBlock.getColumn();

		Type retType = forBlock.getTaggedType();

		String rhsExprTemp = this.getTempVarName();
		String localVarName = forBlock.localVarName;
		Type localvarType = forBlock.localVarType;

		//String idxAssignment = "";
		
		if (exprType.hasArrayLevels() && !TypeCheckUtils.isLocalArray(exprType)) {// It's  an array!

			/*
			 * for(localVar optionalType in rhsExprArray) => for(int tmpN = 0;
			 * tmpN <= ResultOf[rhsExprArray]->tmpVar.arralength; tmpN++) {
			 * localVarName = mpVar[tmpN] as CastTo[localvarType] }
			 */

			// prepend block with localvar
			Block stufftodo = forBlock.block;

			String n = this.getTempVarName();

			AssignNew assignrhsExprTemp = new AssignNew(null, line, col, false, false, rhsExprTemp, null, exprType, AssignStyleEnum.EQUALS, rhsExpr);
			assignrhsExprTemp.isTempVariableAssignment = true;
			assignrhsExprTemp.isReallyNew = true;// hackadodledoo
			assignrhsExprTemp.accept(this);
			// assign rhsExpr to a tempvar

			LineHolder assignLocalV = null;
			{
				Type exprAssignmentType = (Type) exprType.copy();
				exprAssignmentType.setArrayLevels(exprType.getArrayLevels() - 1);
				ArrayRefLevelElementsHolder arrayLevelElements = new ArrayRefLevelElementsHolder();
				ArrayList<ArrayRefElement> l1 = new ArrayList<ArrayRefElement>();
				ArrayRefElement aree = new ArrayRefElement(line, col, new RefName(line, col, n));
				aree.setTaggedType(exprAssignmentType);
				l1.add(aree);
				arrayLevelElements.add(false, l1);// [n]
				// arrayLevelElements.tagType(new
				// PrimativeType(PrimativeTypeEnum.INT),
				// ARElementType.ARRAY);//TODO: go through code and remove all
				// the static object thingys like this [constatnts file is
				// needed]
				arrayLevelElements.tagType(exprAssignmentType, exprAssignmentType, ARElementType.ARRAY);

				ArrayRef ar = new ArrayRef(line, col, new RefName(line, col, rhsExprTemp), arrayLevelElements);// rhsExprTemp[n]
				ar.setTaggedType(exprAssignmentType);

				Assign assignLocalVar;
				
				if(forBlock.assignTuple != null) {
					forBlock.assignTuple.expr = ar;
					assignLocalVar = forBlock.assignTuple;
				}else {
					if (localvarType != null) {
						assignLocalVar = new AssignNew(null, line, col, false, false, localVarName, null, localvarType, AssignStyleEnum.EQUALS, ar);
						((AssignNew) assignLocalVar).isReallyNew = true;
						((AssignNew) assignLocalVar).isTempVariableAssignment = true;
					} else {
						RefName refN = new RefName(line, col, localVarName);
						refN.resolvesTo = new TypeAndLocation(exprAssignmentType, new LocationLocalVar(null));
						assignLocalVar = new AssignExisting(line, col, refN, AssignStyleEnum.EQUALS, ar);
						((AssignExisting) assignLocalVar).isReallyNew = true;
						assignLocalVar.setTaggedType(exprAssignmentType);
					}
				}

				assignLocalV = new LineHolder(line, col, assignLocalVar);
			}

			// going wrong somewhere fix it

			stufftodo.reallyPrepend(assignLocalV);

			ArrayList<Expression> lengAr = new ArrayList<Expression>();
			ArrayList<Boolean> isDirect = new ArrayList<Boolean>();
			lengAr.add(new RefName(line, col, "length"));
			isDirect.add(true);
			
			ArrayList<Boolean> retself = new ArrayList<Boolean>();
			retself.add(true);
			
			ArrayList<Boolean> safecall = new ArrayList<Boolean>();
			retself.add(false);
			
			ArrayList<GrandLogicalElement> elements = new ArrayList<GrandLogicalElement>();
			DotOperator dop = new DotOperator(line, col, new RefName(line, col, rhsExprTemp), lengAr, isDirect, retself, safecall);
			dop.setTaggedType(Const_PRIM_INT);// n.length -> INT
			elements.add(new GrandLogicalElement(line, col, GrandLogicalOperatorEnum.LT, dop));

			EqReExpression check = new EqReExpression(line, col, new RefName(line, col, n), elements);// n < tempv.length

			Expression postExpression = new PostfixOp(line, col, FactorPostFixEnum.PLUSPLUS, new RefName(line, col, n));
			
			ForBlockOld fakeMeUpScotty = new ForBlockOld(line, col, null, n, Const_PRIM_INT, AssignStyleEnum.EQUALS, new VarInt(line, col, 0), // n int = 0
					check, postExpression, // n++
					stufftodo, null);
			fakeMeUpScotty.startOfWorkBlock = forBlock.startOfWorkBlock;
			fakeMeUpScotty.startOfPostOp = forBlock.startOfPostOp;
			fakeMeUpScotty.defoEndsInGotoStmtAlready = forBlock.defoEndsInGotoStmtAlready;
			fakeMeUpScotty.setLabelAfterCode(forBlock.getLabelAfterCode());
			fakeMeUpScotty.setShouldBePresevedOnStack(forBlock.getShouldBePresevedOnStack());
			fakeMeUpScotty.setLabelBeforeRetLoadIfStackPrese(forBlock.getLabelBeforeRetLoadIfStackPrese());
			fakeMeUpScotty.setTaggedType(retType);
			fakeMeUpScotty.setIfReturnsExpectImmediateUse(forBlock.getIfReturnsExpectImmediateUse());
			fakeMeUpScotty.beforeAdder = forBlock.beforeAdder;
			fakeMeUpScotty.idxExpression = forBlock.idxVariableCreator;
			fakeMeUpScotty.elseblock = forBlock.elseblock;
			fakeMeUpScotty.newforTmpVar = rhsExprTemp;

			if(forBlock.idxVariableCreator != null || forBlock.idxVariableAssignment != null){
				fakeMeUpScotty.postIdxIncremement = new PostfixOp(line, col, FactorPostFixEnum.PLUSPLUS, new RefName(line, col, forBlock.idxVariableAssignment!=null?forBlock.idxVariableAssignment.name:forBlock.idxVariableCreator.name));
				fakeMeUpScotty.postIdxIncremement.setShouldBePresevedOnStack(false);
			}
			
			fakeMeUpScotty.accept(this);
		} else if (exprType instanceof NamedType) {
			this.localvarStack.push(new HashMap<String, Pair<Type, Integer>>());

			
			Label afterElseBlock = forBlock.elseblock != null?new Label():null;
			int presistResultOnStack = -1;
			if (forBlock.getShouldBePresevedOnStack()) {
				String tempName = this.getTempVarName();
				// new java.lang.LinkedList<String>();

				bcoutputter.visitTypeInsn(NEW, "java/util/LinkedList");
				bcoutputter.visitInsn(DUP);
				bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/util/LinkedList", "<init>", "()V");

				presistResultOnStack = this.createNewLocalVar(tempName, retType, true);
				slotForBreakContinueInLoop.push(presistResultOnStack);
			}


			NamedType nt = (NamedType) exprType;
			rhsExpr.accept(this);
			
			
			if(afterElseBlock != null){
				//mv.visitInsn(DUP);

				Label entrElseBlock = new Label();
				forBlock.elseblock.setLabelOnEntry(entrElseBlock);
				
			    int tempVar = convertToBoolean(nt, entrElseBlock);
			    bcoutputter.visitJumpInsn(IFEQ, entrElseBlock);
			    bcoutputter.visitVarInsn(ALOAD, tempVar);
			}

			if (forBlock.isMapSetType != null) {
				// to cover this case: for(a in f){ a } => for(a in f.keySet()){
				// a } //if f is a Map.
				FuncInvoke getKeySet = new FuncInvoke(line, col, "keySet", new FuncInvokeArgs(line, col));
				getKeySet.resolvedFuncTypeAndLocation = new TypeAndLocation(new FuncType(new ArrayList<Type>(), forBlock.isMapSetType), new ClassFunctionLocation(nt.getSetClassDef().bcFullName(), nt));
				getKeySet.setTaggedType(getKeySet.resolvedFuncTypeAndLocation.getType());

				nt = (NamedType) getKeySet.accept(this);
			}

			FuncInvoke iterator = new FuncInvoke(line, col, "iterator", new FuncInvokeArgs(line, col));

			boolean isLocalArray = TypeCheckUtils.isLocalArray(nt);

			NamedType iteratOn;
			Type genType;
			if (isLocalArray) {
				genType = nt.copyTypeSpecific();
				genType.setArrayLevels(nt.getArrayLevels() - 1);
				iteratOn = new NamedType(new ClassDefJava(LocalArray.class));
				iteratOn.setGenTypes(genType);
			} else {
				iteratOn = nt;
				genType = null;
				if(!TypeCheckUtils.isList(this.errorRaisableSupressionFromSatc, nt, false)) {
					//pull out iterator
					List<Pair<String, TypeAndLocation>> methods = nt.getAllMethods(null);
					for(Pair<String, TypeAndLocation> inst : methods) {
						if(inst.getA().equals("iterator")) {
							FuncType ft = (FuncType)inst.getB().getType();
							if(ft.inputs.isEmpty()) {
								//iteratOn = (NamedType)ft.retType;
								genType = ((NamedType) (NamedType)ft.retType ).getGenTypes().get(0);
								break;
							}
						}
					}
				}else {
					genType = nt.getGenericTypeElements().get(0);
				}
			}

			iterator.resolvedFuncTypeAndLocation = new TypeAndLocation(new FuncType(new ArrayList<Type>(), new NamedType(new ClassDefJava(Iterator.class))), new ClassFunctionLocation(iteratOn.getSetClassDef().bcFullName(), iteratOn));
			iterator.setTaggedType(iterator.resolvedFuncTypeAndLocation.getType());

			iterator.setPreceededByThis(rhsExpr instanceof RefThis);
			
			NamedType iterType = (NamedType) iterator.accept(this);

			String iteratorVar = this.getTempVarName();

			createNewLocalVar(iteratorVar, iterType, true);

			if(null != forBlock.idxVariableCreator){
				forBlock.idxVariableCreator.accept(this);
			}
			
			Label hasNextLabel = forBlock.hasNextLabel;
			
			//PostfixOp postIdxIncremement = null;
			if((forBlock.idxVariableCreator != null || forBlock.idxVariableAssignment != null ) && !forBlock.block.hasDefoBrokenOrContinued){
				//postIdxIncremement = new PostfixOp(line, col, FactorPostFixEnum.PLUSPLUS, new RefName(line, col, forBlock.idxVariableAssignment!=null?forBlock.idxVariableAssignment.name:forBlock.idxVariableCreator.name));
				//postIdxIncremement.setShouldBePresevedOnStack(false);
				
				hasNextLabel = new Label();
			}
			
			bcoutputter.visitJumpInsn(GOTO, hasNextLabel);// FIX ME!!!! - hasNext()

			Label nextStart = forBlock.startOfPostOp;
			bcoutputter.visitLabel(nextStart);
			
			this.loadLocalVar(iteratorVar, null);
			FuncInvoke next = new FuncInvoke(line, col, "next", new FuncInvokeArgs(line, col));

			Type localvTpe = localvarType != null ? localvarType : genType;

			if (localvTpe instanceof PrimativeType) {
				localvTpe = TypeCheckUtils.boxTypeIfPrimative(localvTpe, false);
			}else if(localvTpe instanceof FuncType ) {
				localvTpe = TypeCheckUtils.convertfuncTypetoNamedType(localvTpe, null);
			}
			
			Type genericNext = localvTpe;// new NamedType(new ClassDefJava(String.class));
			genericNext.setOrigonalGenericTypeUpperBound(Const_Object);
			next.resolvedFuncTypeAndLocation = new TypeAndLocation(new FuncType(new ArrayList<Type>(), genericNext), new ClassFunctionLocation(iterType.getSetClassDef().bcFullName(), iterType, true));
			next.setTaggedType(next.resolvedFuncTypeAndLocation.getType());
			
			
			if(forBlock.assignTuple != null) {
				//next.accept(this);
				forBlock.assignTuple.expr = next;
				
				for( Assign ass : forBlock.assignTuple.lhss) {
					if(ass instanceof AssignExisting) {
						((AssignExisting) ass).isReallyNew=true;
					}else if(ass instanceof AssignNew){
						((AssignNew) ass).isReallyNew=true;
					}
				}
				
				forBlock.assignTuple.accept(this);
			}else {
				Assign assignLocalVar;
				if (localvarType != null) {
					assignLocalVar = new AssignNew(null, line, col, false, false, localVarName, null, localvarType, AssignStyleEnum.EQUALS, next);
					((AssignNew) assignLocalVar).isReallyNew = true;
					((AssignNew) assignLocalVar).isTempVariableAssignment = true;
				} else {
					RefName refN = new RefName(line, col, localVarName);
					refN.resolvesTo = new TypeAndLocation(genType, new LocationLocalVar(null));
					assignLocalVar = new AssignExisting(line, col, refN, AssignStyleEnum.EQUALS, next);
					((AssignExisting) assignLocalVar).isReallyNew = true;
					((AssignExisting) assignLocalVar).isTempVariableAssignment = true;
					assignLocalVar.setTaggedType(genType);
				}
				
				/*ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> loopVar = new ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>();
				loopVar.add(new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(localvTpe, localVarName, null, false, false, false));
				
				this.varsToAddToScopeOnEntry = stripOutAnnotations(loopVar);
				
				*/
				
				
				assignLocalVar.accept(this);
			}
			
			
			Type forBlockType = (Type) forBlock.block.accept(this);
			
			if (presistResultOnStack > -1 && !forBlock.block.hasDefoBrokenOrContinued) {// ends in a break or a continue, then what, u dont need the bellow silly rabbit!
				// mv.visitLabel(forBlock.block.getLabelAfterCode());
				bcoutputter.visitLabel(forBlock.beforeAdder);
				forBlockType = Utils.unref(bcoutputter, forBlockType, TypeCheckUtils.getRefTypeToLocked(forBlockType), this);
				
				Type from = Utils.box(bcoutputter, forBlockType);
				Utils.applyLoad(bcoutputter, retType, presistResultOnStack);
				Utils.genericSswap(bcoutputter, from, retType);
				bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/util/LinkedList", "add", "(Ljava/lang/Object;)Z");
				bcoutputter.visitInsn(POP);
			}

			bcoutputter.visitLabel(forBlock.hasNextLabel);
			
			if(null != forBlock.postIdxIncremement){
				forBlock.postIdxIncremement.accept(this);
				//postIdxIncremement.accept(this);
				bcoutputter.visitLabel(hasNextLabel);
			}
			
			this.loadLocalVar(iteratorVar, null);

			FuncInvoke hasnext = new FuncInvoke(line, col, "hasNext", new FuncInvokeArgs(line, col));
			hasnext.resolvedFuncTypeAndLocation = new TypeAndLocation(new FuncType(new ArrayList<Type>(), Const_PRIM_BOOl), new ClassFunctionLocation(iterType.getSetClassDef().bcFullName(), iterType, true));
			hasnext.setTaggedType(hasnext.resolvedFuncTypeAndLocation.getType());

			hasnext.accept(this);

			bcoutputter.visitJumpInsn(IFNE, nextStart);
			
			//if noVisits -> afterElseBlock
			
			if (presistResultOnStack > -1) {
				Label labbeforeret = forBlock.getLabelBeforeRetLoadIfStackPrese();
				if(null != labbeforeret) {
					bcoutputter.visitLabel(labbeforeret);
				}
				Utils.applyLoad(bcoutputter, retType, presistResultOnStack);
			}
			
			if(null != afterElseBlock){
				bcoutputter.visitJumpInsn(GOTO, afterElseBlock);	
			}
			
			if(afterElseBlock != null){
				bcoutputter.visitLabel(forBlock.elseblock.getLabelOnEntry());
				forBlock.elseblock.accept(this); 
				bcoutputter.visitLabel(afterElseBlock);
			}
			

			/*
			 * this is what all the above replicates.... L4 ALOAD 1: strs
			 * INVOKEVIRTUAL ArrayList.iterator() : Iterator ASTORE 4 GOTO L5 L6
			 * ALOAD 4 INVOKEINTERFACE Iterator.next() : Object CHECKCAST String
			 * ASTORE 3 L7 ALOAD 3: a ASTORE 2: aba L5 ALOAD 4 INVOKEINTERFACE
			 * Iterator.hasNext() : boolean IFNE L6
			 * ^ when there is no else block
			 */
			this.localvarStack.pop();
		}
		
		
		return retType;
	}

	private Expression dotOpExprCurrentProcess = null;

	private Stack<Type> dorOpLHS = new Stack<Type>();

	/*private void adjustDotOperatorPreventUnRefOfElement(){
		//e.g. xx.get(), where it's being operated as x:get()
	}*/
	
	private Object evalDotOpElem(Expression what, boolean isLast) {
		if(!isLast && what instanceof NotNullAssertion) {
			NotNullAssertion asNNA = (NotNullAssertion)what;
			if(asNNA.vectorizedRedirect != null) {
				return asNNA.vectorizedRedirect.accept(this);
			}
			asNNA.expr.accept(this);
			return asNNA.expr.getTaggedType();
		}
			
		return what.accept(this);
	}
	
	public Object processDotOperator(DotOperator dotOperator, boolean dupOnFirst, boolean processLast) {
		// this funny function is useful for cases when you want to process the
		// likes of: this.d[this.x].g = 7; cos u need to insert a dup operation
		// and the handler deals with the last bit
		Expression prev = dotOperator.getHead(this);
		Type headType = (Type)evalDotOpElem(prev, false);
		
		Type retType = dotOperator.getTaggedType();
		ArrayList<Expression> elements = dotOperator.getElements(this);
		
		if (null != headType && headType.hasArrayLevels() && elements.size() == 2) {
			Expression first = elements.get(1);
			if (first instanceof RefName && ((RefName) first).name.equals("length")) {
				if (TypeCheckUtils.isLocalArray(headType)) {
					Utils.extractAndCastArrayRef(bcoutputter, headType);
					//HERE BE PROBLEMS
					//mv.visitFieldInsn(GETFIELD, "com/concurnas/bootstrap/runtime/ref/LocalArray", "ar", "[Ljava/lang/Object;");
					//mv.visitTypeInsn(CHECKCAST, "[Lcom/concurnas/runtime/ref/Local;");
				}
				bcoutputter.visitInsn(ARRAYLENGTH);
				return retType;
			}
		}

		if (dupOnFirst) {// some of these are magic to me
			bcoutputter.visitInsn(DUP);
		}

		//boolean prevSafeCall = false;
		for (int n = 1; n < elements.size(); n++) {
			boolean isLast = elements.size() - 1 == n;
			boolean shouldBePresevedOnStack = dotOperator.getShouldBePresevedOnStack();
			Expression e = elements.get(n);
			boolean shouldReturnSelf =dotOperator.returnCalledOn == null?false:dotOperator.returnCalledOn.get(n-1);
			boolean safeCall =dotOperator.safeCall == null?false:dotOperator.safeCall.get(n-1);
			
			if(shouldReturnSelf){
				if(processLast && isLast && !shouldBePresevedOnStack) {
					shouldReturnSelf=false;
				}
			} 
			
			Label onNull = null;
			if(safeCall) {
				onNull = new Label();
				
				bcoutputter.visitInsn(DUP);
				
				bcoutputter.visitJumpInsn(IFNULL, onNull);
			}
			
			
			if(shouldReturnSelf){
				bcoutputter.visitInsn(DUP);
			}
			
			if(e instanceof FuncInvoke && ((FuncInvoke)e).astRedirect != null){//a bit hacky but this is the only case where this logic applies so we can get away with it
				dotOpExprCurrentProcess=((FuncInvoke)e).astRedirect;//its used here: par = Parent<int>(); ins = par.MyClass<String>()
			}
			else if(e instanceof NamedConstructorRef){//another hack for this: par.new MyClass<String>&() 
				dotOpExprCurrentProcess=((NamedConstructorRef)e).funcRef;
			}
			else{
				dotOpExprCurrentProcess = e;
			}
			
			// if(n == 1)//first
			{
				this.setTerminalRefPreccededByThis(e, true);
				if (dotOperator.getHead(this) instanceof RefSuper) {
					((Node) e).setPreceededBySuper(true);
				}
			}

			Type prevHead = headType; 
			
			Type lhsToPush = prev instanceof RefNamedType ? null:headType;
			
			if (isLast) {// last
				if (processLast) {
					dorOpLHS.push(lhsToPush);
					e.setPreceedingExpression(prev);
					headType = (Type) evalDotOpElem(e, true);
					prev = e;
					dorOpLHS.pop();
				}
			} else {
				dorOpLHS.push(lhsToPush);
				e.setPreceedingExpression(prev);
				headType = (Type) evalDotOpElem(e, false);
				prev = e;
				dorOpLHS.pop();
			}
			
			if(shouldReturnSelf){
				if(!headType.equals(ScopeAndTypeChecker.const_void)){
					bcoutputter.visitInsn(Utils.varSlotsConsumedByType(headType) != 1?POP2:POP);
					if(null != prev) {
						prev = prev.getPreceedingExpression();
					}
				}
				//undo above
				headType=prevHead;
			}
			
			if(null != onNull) {
				if(isLast) {
					if(!shouldBePresevedOnStack) {//MHA - dummy object so we can pop it later...
						bcoutputter.visitTypeInsn(NEW, "com/concurnas/bootstrap/runtime/cps/CObject");
						bcoutputter.visitInsn(DUP);
						bcoutputter.visitMethodInsn(INVOKESPECIAL, "com/concurnas/bootstrap/runtime/cps/CObject", "<init>", "()V", false);
					}else {
						headType = Utils.box(bcoutputter, headType);
					}
					
					bcoutputter.visitLabel(onNull);
				}else {
					bcoutputter.visitLabel(onNull);
				}
				
				if(!(isLast && !shouldBePresevedOnStack)) {
					bcoutputter.visitTypeInsn(CHECKCAST, headType.getCheckCastType());
				}else {
					bcoutputter.visitInsn(Opcodes.POP);
				}
			}
		}

		return headType;

	}

	@Override
	public Object visit(DotOperator dotOperator) {
		return this.processDotOperator(dotOperator, false, true);
	}

	private boolean isOwnerAParentOrMoi(String owner, Type asNt) {
		// TODO: add mixins etc here
		if (owner.equals(this.getFullModuleAndClassName())) {
			return true;
		} else {
			if (!this.currentClassDef.isEmpty()) {
				ClassDef moi = this.currentClassDefObj.peek();
				
				HashSet<ClassDef> allTraits = moi.getTraitsIncTrans();
				if(!allTraits.isEmpty()) {
					if(asNt instanceof NamedType) {
						if(allTraits.contains(((NamedType)asNt).getSetClassDef())) {
							return true;
						}
					}
				}
				
				ClassDef sup = moi.getSuperclass();
				while (null != sup) {
					String tryola = sup.bcFullName();
					if (owner.equals(tryola)) {
						return true;
					}
					sup = sup.getSuperclass();
				}
			}

		}
		return false;
	}

	private boolean prefixNodeWithThisreference(Location loc, boolean prepostfixop) {// e.g. if u have
															// v(), or v=9, and
															// v is a field
															// thingy
		//returns whether ALOAD 0 alone has been placed onto the stack
		if (null != loc) {
			if (loc instanceof LocationClassField && ((LocationClassField) loc).getOwner().equals(this.getFullModuleAndClassName())) {
				bcoutputter.visitVarInsn(ALOAD, 0);
				
			} else if (loc instanceof LocationClassField && isOwnerAParentOrMoi(((LocationClassField) loc).getOwner(),  ((LocationClassField) loc).ownerType     )) {
				bcoutputter.visitVarInsn(ALOAD, 0);
				
				if(this.currentClassDefObj.peek().isTrait) {
					bcoutputter.visitTypeInsn(CHECKCAST, ((LocationClassField) loc).getOwner());
				}
				
			} else if (loc instanceof ClassFunctionLocation && isOwnerAParentOrMoi(((ClassFunctionLocation) loc).owner, ((ClassFunctionLocation) loc).ownerType)) {
				bcoutputter.visitVarInsn(ALOAD, 0);
				
				
			} else if ((loc instanceof LocationClassField || loc instanceof ClassFunctionLocation) && !this.currentClassDefObj.isEmpty()){
				Type ownerType;
				if (loc instanceof LocationClassField) {
					ownerType = ((LocationClassField) loc).ownerType;
				} else {
					ownerType = ((ClassFunctionLocation) loc).getOwnerType();
				}

				if (ownerType instanceof NamedType) {
					ClassDef ownerClass = ((NamedType) ownerType).getSetClassDef();
					ClassDef currentCD = this.currentClassDefObj.peek();
					if(currentCD.isEnumSubClass ||  currentCD.isLocalClass){
						
						if(currentCD.isLocalClass) {
							if(ownerClass.equals(currentCD)) {
								bcoutputter.visitVarInsn(ALOAD, 0);
							}
							
						}else {
							bcoutputter.visitVarInsn(ALOAD, 0);
						}
						
					}
					else if (null != currentCD.isParentNestorEQOrSUperClass(ownerClass)) {
						moveUpNestingHierarchyToGoal(currentCD, ownerClass);
					}
				}
				return false;
			}
		}
		return true;
	}

	private void moveUpNestingHierarchyToGoal(ClassDef currentClassLocationi, ClassDef parentTarget) {
		bcoutputter.visitVarInsn(ALOAD, 0);
		ClassDef parent = currentClassLocationi.getParentNestor();

		bcoutputter.visitFieldInsn(GETFIELD, currentClassLocationi.bcFullName(), "this$" + currentClassLocationi.getNestingLevel(), "L" + parent.bcFullName() + ";");

		while (parent.getParentNestor() != null && !parent.equals(parentTarget)) {
			ClassDef nestor = parent.getParentNestor();
			// mv.visitMethodInsn(INVOKESTATIC, "A/Child2$Outer$InnerClass",
			// "access$0", "(LA/Child2$Outer$InnerClass;)LA/Child2$Outer;");
			bcoutputter.visitMethodInsn(INVOKESTATIC, parent.bcFullName(), "access$0", "(L" + parent.bcFullName() + ";)L" + nestor.bcFullName() + ";");
			parent = nestor;
		}
		/*
		 * 1 level mv.visitFieldInsn(GETFIELD, "A/Child2$Outer$InnerClass",
		 * "this$1", "LA/Child2$Outer;");
		 * 
		 * 2 mv.visitFieldInsn(GETFIELD,"A/Child2$Outer$InnerClass$Inner2Class", "this$2","LA/Child2$Outer$InnerClass;");
		 *  mv.visitMethodInsn(INVOKESTATIC,"A/Child2$Outer$InnerClass", "access$0", "(LA/Child2$Outer$InnerClass;)LA/Child2$Outer;");
		 * 
		 * 3 etc u get the idea mv.visitFieldInsn(GETFIELD,	"A/Child2$Outer$InnerClass$Inner2Class$Inner3Class", "this$3",	 "LA/Child2$Outer$InnerClass$Inner2Class;");
		 * mv.visitMethodInsn(INVOKESTATIC,	 "A/Child2$Outer$InnerClass$Inner2Class", "access$0",	 "(LA/Child2$Outer$InnerClass$Inner2Class;)LA/Child2$Outer$InnerClass;"); 
		 * mv.visitMethodInsn(INVOKESTATIC, "A/Child2$Outer$InnerClass", "access$0", "(LA/Child2$Outer$InnerClass;)LA/Child2$Outer;");
		 * 
		 * //lster code does this: mv.visitFieldInsn(GETFIELD, "A/Child2$Outer", "x", "I");
		 */
	}

	@Override
	public Object visit(RefName refName) {
		if (refName.astRedirectforOnChangeNesting != null) {
			return refName.astRedirectforOnChangeNesting.accept(this);
		}

	/*	if(refName.popOnEntry){//only really if we are calling a static method
			mv.visitInsn(Opcodes.POP);
		}*/
		
		if(refName.isMapGetter != null){
			bcoutputter.visitLdcInsn(refName.name);
			NamedType calledOnNT = refName.isMapGetter.getB();
			
			boolean hasRefs = TypeCheckUtils.getRefLevlstoLocked(calledOnNT) > 0;
			if(hasRefs){
				bcoutputter.visitInsn(SWAP);
				calledOnNT = (NamedType)Utils.unref(bcoutputter, calledOnNT, this);
				bcoutputter.visitInsn(SWAP);
			}
			
			if(refName.isMapGetter.getA()){//normal map getter
				bcoutputter.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
				bcoutputter.visitTypeInsn(CHECKCAST, refName.isMapGetter.getB().getGenTypes().get(1).getCheckCastType());
			}
			else{//operator overload style...
				String calledOn = calledOnNT.getCheckCastType();
				String methodType = getNormalMethodInvokationDesc(refName.isMapGetter.getD().getInputs(), refName.isMapGetter.getD().retType);
				bcoutputter.visitMethodInsn(INVOKEVIRTUAL, calledOn, "get", methodType);
			}
			
			
			return refName.getTaggedType();
		}
		else{

			TypeAndLocation resovlesTo = refName.resolvesTo;

			if (!refName.isPreceededByThis())// HACK A DOODLE DO
			{
				// JPT: ugly - in gennerated code this can be null - autogen code
				// can only refer to localvar anyway so def behavour is ok
				prefixNodeWithThisreference(resovlesTo == null ? null : resovlesTo.getLocation(), false);
			}

			if (refName.ignoreWhenGenByteCode) {
				return null;
			}

			Location loc = resovlesTo == null ? null : resovlesTo.getLocation();
			Pair<String, String> accAndName = loc == null ? null : loc.getPrivateStaticAccessorRedirectFuncGetter();

			
			/*if (loc instanceof LocationStaticField) {
				this.unrefToHeadTypeOfDotOp((NamedType) ((LocationStaticField) loc).);
			} else */if (loc instanceof LocationClassField) {
				this.unrefToHeadTypeOfDotOp(((LocationClassField) loc).ownerType);
			}
			
			Type ret;
			if (null != accAndName && !isInclInitCreator) {// cursed to access via
															// accessor
				ret = resovlesTo.getType();

				String owner = (loc instanceof LocationStaticField ? ((LocationStaticField) loc).owner : ((LocationClassField) loc).getOwner()).replace('.', '/');
				String args = loc instanceof LocationClassField ? "(L" + owner + ";)" : "()";

				this.bcoutputter.visitMethodInsn(INVOKESTATIC, owner, accAndName.getA(), args + ret.getBytecodeType());
			} else {
				ret = loadLocalVar(refName.bytecodename!=null? refName.bytecodename: refName.name, resovlesTo);//fun (INT) INT[]:com.concurnas.compiler.typeAndLocation.LocationStaticField@569cfc36
				//above is being mapped to namedtype - is that correct?
				
			}

			// NOTE:!! These cannot be poped from stack cos reference to a field
			// without return etc would be pointless and a nop operation (whihc is
			// not alloweD)
			// i.e. this is not alloweD:
			// ...; a.h; ...; <- unless h were routed to a getter(no arg setter?)

			// if(ret instanceof NamedType && null !=
			// ((NamedType)ret).origonalGenericTypeUpperBound &&
			// !ret.equals(((NamedType)ret).origonalGenericTypeUpperBound) )
			if(refName.castTo != null){//e.g. called inside an if statement with an is expression
				return doCast(refName.castFrom, refName.getTaggedType());
				//return Utils.unref(mv, refName.getTaggedType(), this);
				//mv.visitTypeInsn(CHECKCAST, refName.castTo.getCheckCastType());
			} else if (null != ret.getOrigonalGenericTypeUpperBoundRaw() && !ret.equals(ret.getOrigonalGenericTypeUpperBoundRaw())) {
				// if this is a generic method invokation, it will return the upperbound object instead of what we want 
				//so we must cast to what we want on the stack String castTo = nt.getBytecodeType(); // nt.getSetClassDef().bcFullName(); //
				
				if(TypeCheckUtils.typeRequiresLocalArrayConvertion(ret)){
					//Utils.applyCastImplicit(mv, returnType.getOrigonalGenericTypeUpperBound(), returnType, this);
					Utils.createRefArray(bcoutputter, (NamedType)ret);
					ret.setOrigonalGenericTypeUpperBound(null);//to prevent this being extracted again on useage of variable
					//create ref
				}
				
				bcoutputter.visitTypeInsn(CHECKCAST, ret.getCheckCastType());
			}
			return ret;
		}
		

	}

	/*
	 * private boolean isConstructingHaveCommonParentNestor(ClassDef
	 * makingParent) { if(!this.currentClassDefObj.isEmpty()) { ClassDef
	 * parentToCheck = this.currentClassDefObj.peek().getParentNestor();
	 * while(null != parentToCheck) { if(parentToCheck.equals(makingParent)){
	 * return true; } parentToCheck = parentToCheck.getParentNestor(); } }
	 * return false; }
	 */

	private boolean isInClass() {
		return !this.currentClassDefObj.isEmpty();
	}

	private Stack<HashMap<String, LambdaMethodDefToAddLater>> lambdasToAdd = new Stack<HashMap<String, LambdaMethodDefToAddLater>>();

	private class LambdaMethodDefToAddLater {
		// deals with this little turd:
		/*
		 * class MyCls{ lambdavar = fun (a int, b int, c int) int { return
		 * a+b+c; } }
		 */
		private FuncDefI lambdaDef;
		private BytecodeGennerator vis;

		public LambdaMethodDefToAddLater(BytecodeGennerator vis, FuncDefI lambdaDef) {
			this.lambdaDef = lambdaDef;
			this.vis = vis;
		}

		public void apply() {
			createMethod(this.lambdaDef, this.vis);
		}

	}

	private void prepareAnyExtraLambdas() {
		lambdasToAdd.push(new HashMap<String, LambdaMethodDefToAddLater>());
	}

	private void addAnyExtraLamdbas() {
		HashMap<String, LambdaMethodDefToAddLater> occ = lambdasToAdd.pop();
		for (LambdaMethodDefToAddLater toAdd : occ.values()) {
			toAdd.apply();
		}
	}

	private Set<String> methAlreadyGen = new HashSet<String>();

	@Override
	public Object visit(FuncRef funcRef) {
		// nonStaticFuncRef = ar.get&(int)
		// ->
		// nonStaticFuncRef = new fn$lambda$1(ar)

		Type operatingOn = funcRef.operatingOn;
		
		if (funcRef.shouldVisitFunctoInBytcodeGen) {
			operatingOn = (Type)funcRef.functoFoBC.accept(this);
			//Utils.unref(mv, resolvesTo, this);
		}
		else{
			if(!this.dorOpLHS.isEmpty()){
				Type lhsType = this.dorOpLHS.peek();

				Utils.applyCastImplicit(bcoutputter, lhsType, funcRef.operatingOn, this);//e.g. unref if needs be etc
			}
		}

		// needs to load static feild and consider non static hmmm

		Pair<String, String> fullnameAndFilename = funcRef.getLambdaDetails();

		String fullname = fullnameAndFilename.getA();

		FuncType tagged = (FuncType)funcRef.getTaggedType();
		
		ArrayList<Type> inputArgs = funcRef.argumentsThatDontNeedToBecurriedIn; // expectedType.inputs;
		

		// operatingOn
		ArrayList<Type> inputArgsForMeth = new ArrayList<Type>();
		
		
		
		if(tagged.isClassRefType){
		
			ClassDef clsBeingInit = ((NamedType)tagged.retType).getSetClassDef();
			ClassDef parentClass = clsBeingInit.getParentNestor();
			
			bcoutputter.visitTypeInsn(NEW, fullname);
			
			if(!funcRef.unbounded && parentClass != null){
				bcoutputter.visitInsn(SWAP);
				bcoutputter.visitInsn(DUP2);
				bcoutputter.visitInsn(POP);
				bcoutputter.visitInsn(SWAP);
			}
			else{
				bcoutputter.visitInsn(DUP);
			}
			
			ArrayList<Expression> gotArgs = null;//funcRef.argumentsThatDontNeedToBecurriedIn;
		
			int n=0;
			if(null != funcRef.replaceFirstArgWithTypeArraOf){
				//Utils.createTypeArray(mv, funcRef.replaceFirstArgWithTypeArraOf, new ArrayList<String>(), null, null);
				Utils.addClassTypeArrayForActorRef(bcoutputter, funcRef.replaceFirstArgWithTypeArraOf);
				n++;
			}
			
			if(null != inputArgs){
				// load remaining args
				for (; n < inputArgs.size(); n++) {
					Type got = (Type) gotArgs.get(n).accept(this);//wtf?
					Type expect = inputArgs.get(n);
					
					donullcheckForUnknown(got, expect);
					
					if (!got.equals(expect)) {
						Utils.applyCastImplicit(bcoutputter, got, expect, this);
					}
				}

				for (Type i : inputArgs) {
					inputArgsForMeth.add(i);// TypeCheckUtils.boxTypeIfPrimative(i));
				}
			}
			
			if(!funcRef.unbounded){
				boolean isprefixedWithParentNestor = prefixNestedconstructor(clsBeingInit, funcRef);	
				if (parentClass != null && !isprefixedWithParentNestor) {
					moveUpNestingHierarchyToGoal(this.currentClassDefObj.peek(), parentClass);
				}

				if (parentClass != null) {
					inputArgsForMeth.add(0, new NamedType(parentClass));
				}
			}
			
			if(clsBeingInit.isLocalClass){
				FuncDef ofd = clsBeingInit.getAllConstructors().iterator().next().origonatingFuncDef;
				for(FuncParam fp : ofd.getParams().params){
					/*if(fp.nonLocalVariableResolvesTo != null){
						mv.visitVarInsn(ALOAD, 0);
						fp.nonLocalVariableResolvesTo.accept(this);
						inputArgsForMeth.add(fp.nonLocalVariableResolvesTo.getTaggedType());
					}
					else*/ if(fp.name.contains("$n") || fp.hasSyntheticParamAnnotation()){
						loadLocalVar(fp.name, new TypeAndLocation(fp.type, new LocationLocalVar(null)));
						inputArgsForMeth.add(fp.type);
					}
				}
			}
			
			
			String methodDesc = getNormalMethodInvokationDesc(inputArgsForMeth, Const_PRIM_VOID);
			bcoutputter.visitMethodInsn(INVOKESPECIAL, fullname, "<init>", methodDesc);
			
		}else{
			ArrayList<Expression> gotArgs = funcRef.getArgsAndLambdaConsts();
			boolean isStatic = !(funcRef.typeOperatedOn.getLocation() instanceof ClassFunctionLocation) && !funcRef.shouldVisitFunctoInBytcodeGen;
			// boolean isStatic = (funcRef.typeOperatedOn.getLocation() instanceof
			// StaticFuncLocation) && !funcRef.shouldVisitFunctoInBytcodeGen;
			// if static then we dont need to pass the object as a reference, and
			// not arg to function
			
			boolean popped=false;
			if(isStatic && !this.dorOpLHS.isEmpty() && this.dorOpLHS.peek() != null && !funcRef.isConstructor){
				bcoutputter.visitInsn(POP);//we dont want to include the instance on the stack
				popped=true;
			}

			if(!funcRef.unbounded && !funcRef.lhsHasNewOPOVerload){//unbounded we pass in no op op
				if (!isStatic && !funcRef.isPreceededByThis() && funcRef.typeOperatedOn.getLocation() instanceof ClassFunctionLocation) {
					
					boolean anymove = false;
					if (!this.currentClassDefObj.isEmpty()) {
						ClassFunctionLocation loc = (ClassFunctionLocation) funcRef.typeOperatedOn.getLocation();
						ClassDef parentClass = ((NamedType) loc.getOwnerType()).getSetClassDef();

						// TypeCheckUtils.
						ClassDef cur = this.currentClassDefObj.peek();
						if (cur.isParentNestor(parentClass)) {
							moveUpNestingHierarchyToGoal(this.currentClassDefObj.peek(), parentClass);
							anymove=true;
						}
					}
					
					if(!anymove) {
						int var = !this.currentClassDefObj.isEmpty() && funcRef.typeOperatedOn.getLocation().redirectExtFuncOrWithExpr != null?1:0;
						/*if(!popped && var == 0) {
							
						}else {*/
							bcoutputter.visitVarInsn(ALOAD, var);
						//}
						
					}
				}
			}
			
			ClassDef parentClass=null;
			boolean isprefixedWithParentNestor = false;
			if(funcRef.isConstructor){
				
				ClassDef clsBeingInit = ((NamedType)((FuncType)funcRef.typeOperatedOn.getType()).retType).getSetClassDef();
				parentClass = clsBeingInit.getParentNestor();
				
				isprefixedWithParentNestor = prefixNestedconstructor(clsBeingInit, funcRef);		
			}
			
			bcoutputter.visitTypeInsn(NEW, fullname);
			// we already loaded the args...
				
			if ((isStatic || funcRef.unbounded) && !isprefixedWithParentNestor) {
				bcoutputter.visitInsn(DUP);
			}
			else if(funcRef.unbounded && funcRef.isConstructor){//e.g. Parent<int>.MyClass<String>&()
				bcoutputter.visitInsn(DUP);
			} else {
				bcoutputter.visitInsn(SWAP);
				bcoutputter.visitInsn(DUP2);
				bcoutputter.visitInsn(POP);
				bcoutputter.visitInsn(SWAP);
			}
					
			if(funcRef.isConstructor && !funcRef.unbounded){
				if (parentClass != null && !isprefixedWithParentNestor) {
					moveUpNestingHierarchyToGoal(this.currentClassDefObj.peek(), parentClass);
				}

				if (parentClass != null) {
					inputArgsForMeth.add(0, new NamedType(parentClass));
				}
			}
			
			Location loc = funcRef.typeOperatedOn.getLocation();
			if(loc.isRHSOfTraitSuperChainable != null) {
				operatingOn = loc.isRHSOfTraitSuperChainable.getA();
			}
			
			if (!isStatic && !funcRef.unbounded) {// as above
				operatingOn.setOrigonalGenericTypeUpperBound(null);
				inputArgsForMeth.add(0, operatingOn);
			}
			for (Type i : inputArgs) {
				inputArgsForMeth.add(i);// TypeCheckUtils.boxTypeIfPrimative(i));
			}

			String methodDesc = getNormalMethodInvokationDesc(inputArgsForMeth, Const_PRIM_VOID);

			int n=0;
			if(null != funcRef.replaceFirstArgWithTypeArraOf){
				//Utils.createTypeArray(mv, funcRef.replaceFirstArgWithTypeArraOf, new ArrayList<String>(), null, null);
				//mv.visitInsn(ICONST_0);//should be empty
				//mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
				
				//createTypeArray(BytecodeOutputter mv, ArrayList<String> types)
				Utils.addClassTypeArrayForActorRef(bcoutputter, funcRef.replaceFirstArgWithTypeArraOf);
				
				n++;
			}
			
			// load remaining args
			for (; n < inputArgs.size(); n++) {
				this.dorOpLHS.push(null);
				Type got = (Type) gotArgs.get(n).accept(this);
				this.dorOpLHS.pop();
				Type expect = inputArgs.get(n);
				
				donullcheckForUnknown(got, expect);
				
				if (!got.equals(expect)) {
					Utils.applyCastImplicit(bcoutputter, got, expect, this);
				}
			}

			bcoutputter.visitMethodInsn(INVOKESPECIAL, fullname, "<init>", methodDesc);

			/*
			 * mv.visitTypeInsn(NEW, "com/concurnas/compiler/bytecode/Lam");
			 * mv.visitInsn(DUP); mv.visitVarInsn(ALOAD, 0);
			 * mv.visitMethodInsn(INVOKESPECIAL, "com/concurnas/compiler/bytecode/Lam",
			 * "<init>", "(Ljava/util/ArrayList;)V");
			 */
		}
		return tagged;
	}
	

	@Override
	public Object visit(AsyncBlock asyncBlock) {

		createOrDeferLambdaDef(asyncBlock.fakeLambdaDef);

		// doCopy(Type tagged, Expression expr){
		// FuncParams fp = asyncBlock.getExtraCapturedLocalVars();
		FuncRef funcRef = asyncBlock.fakeLambdaDef.fakeFuncRef;

		// another approach is to create the lambda and then call copy on the
		// lambda...
		// copy all args in and call lambda constructor
		boolean isStatic = !(funcRef.typeOperatedOn.getLocation() instanceof ClassFunctionLocation) && !funcRef.shouldVisitFunctoInBytcodeGen;

		String fullname = funcRef.getLambdaDetails().getA();

		if(asyncBlock.executor != null) {
			asyncBlock.executor.accept(this);
		}else {
			bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/runtime/cps/Fiber", "getScheduler", "()Lcom/concurnas/bootstrap/runtime/cps/Scheduler;");
			bcoutputter.visitInsn(DUP);
		}
		
		bcoutputter.visitTypeInsn(NEW, fullname);
		// we already loaded the args...
		// if(isStatic){
		bcoutputter.visitInsn(DUP);
		// }


		makeConctracker();
		int tempTrackerSlot = this.createNewLocalVar(this.getTempVarName(), Const_Object, true);
		
		if (!isStatic) {
			doCopy(new NamedType(this.currentClassDefObj.peek()), new RefThis(0, 0), tempTrackerSlot, null, null);
		}

		ArrayList<Type> inputArgsForMeth = new ArrayList<Type>();

		FuncParams extra = asyncBlock.getExtraCapturedLocalVars();

		ArrayList<Pair<String, Integer>> refTempVars = new ArrayList<Pair<String, Integer>>();
		
		ArrayList<FuncParam> gotArgs = extra.params;
		for (int n = 0; n < gotArgs.size() - (!asyncBlock.noReturn ? 1 : 0); n++) {
			FuncParam localVar = gotArgs.get(n);
			RefName asRef = new RefName(localVar.name);
			Type fType = localVar.getTaggedType();
			asRef.setTaggedType(fType);
			asRef.resolvesTo = new TypeAndLocation(fType, new LocationLocalVar(null));

			boolean isTypeShared = false;
			if(fType instanceof NamedType) {
				isTypeShared = ((NamedType)fType).getSetClassDef().isShared;
			}
			
			
			if (!isTypeShared && !localVar.isShared && !TypeCheckUtils.shouldNotBeCopied(fType, localVar.isShared)) {
				fType = doCopy(fType, asRef, tempTrackerSlot, null, localVar.fromSOname);
				
				//if(null != TypeCheckUtils.checkSubType(this.errorRaisableSupressionFromSatc, ScopeAndTypeChecker.const_ref_NT, fType)) {
				if(TypeCheckUtils.getRefLevels(fType) > 0) {
					bcoutputter.visitInsn(DUP);
					String clsName = fType.getCheckCastType();
					
					int tempSlot = this.createNewLocalVar(this.getTempVarName(), fType, false);
					Utils.applyStore(bcoutputter, fType, tempSlot);
					
					refTempVars.add( new Pair<String, Integer>(clsName, tempSlot));
				}
				//if ref having fields
				
				
			} else {//primvative or string...
				asRef.accept(this);
			}

			inputArgsForMeth.add(fType);
		}

		int newRefSlot = -1;
		if (!asyncBlock.noReturn) {// returns something
			// great! needs a ref to be passed in which it can return

			if (null == asyncBlock.theAssToStoreRefIn) {// we're creating a new ref as result
				
				int outputTolevels = 1 + endsInAsyncBlock(asyncBlock.body);
				
				//{12:}!   - only create first layer shell
				//{{12}!}! - 2 - requires 2nd layer in inner call...
				//supress full creation here unless ends in asyncblock
				
				newRefSlot = createNewLocalVar(this.getTempVarName() + "$tempVirloc", true, asyncBlock.getTaggedType(), true, true, true, outputTolevels);
			} else {
				bcoutputter.visitVarInsn(ALOAD, asyncBlock.theAssToStoreRefIn.localVarToCopyRefInto);
			}

			inputArgsForMeth.add(gotArgs.get(gotArgs.size() - 1).getTaggedType());
		}

		if (!isStatic) {// as above
			inputArgsForMeth.add(0, funcRef.operatingOn);// operatingOn
		}

		String methodDesc = getNormalMethodInvokationDesc(inputArgsForMeth, Const_PRIM_VOID);

		bcoutputter.visitMethodInsn(INVOKESPECIAL, fullname, "<init>", methodDesc);
		// cool, created lambda, now pass it over to the Fiber
		bcoutputter.visitLdcInsn(String.format("%s:%s", this.packageAndClassName, asyncBlock.getLine()));
		if(asyncBlock.executor != null) {
			bcoutputter.visitVarInsn(ALOAD, tempTrackerSlot);
			bcoutputter.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/runtime/cps/ISOExecutor", "execute", "(Lcom/concurnas/bootstrap/runtime/cps/IsoTask;Ljava/lang/String;Lcom/concurnas/bootstrap/runtime/CopyTracker;)V");
		}else {
			bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/bootstrap/runtime/cps/Scheduler", "prepare", "(Lcom/concurnas/bootstrap/runtime/cps/AbstractIsoTask;Ljava/lang/String;)Lcom/concurnas/bootstrap/runtime/cps/IsoCore;");
			//iso core, take fiber from this and pass it to custom refs so they can init any fields
			
			if(!refTempVars.isEmpty()) {
				//part 2 of copy init of refs
				bcoutputter.visitInsn(DUP2);
				bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/bootstrap/runtime/cps/Scheduler", "getFiber", "(Lcom/concurnas/bootstrap/runtime/cps/Iso;)Lcom/concurnas/bootstrap/runtime/cps/Fiber;");
				
				for(int n=0; n < refTempVars.size(); n++) {
					if(n != refTempVars.size()-1) {
						bcoutputter.visitInsn(DUP);
					}
					Pair<String, Integer> itm = refTempVars.get(n);
					bcoutputter.visitVarInsn(ALOAD, itm.getB());
					bcoutputter.visitInsn(SWAP);
					bcoutputter.visitVarInsn(ALOAD, tempTrackerSlot);
					
					bcoutputter.visitMethodInsn(INVOKEVIRTUAL, itm.getA(), "initFields$", "(Lcom/concurnas/bootstrap/runtime/cps/Fiber;Lcom/concurnas/bootstrap/runtime/CopyTracker;)V");
				}
			}
			
			
			bcoutputter.visitVarInsn(ALOAD, tempTrackerSlot);
			
			bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/bootstrap/runtime/cps/Scheduler", "scheduleTask", "(Lcom/concurnas/bootstrap/runtime/cps/IsoCore;Lcom/concurnas/bootstrap/runtime/CopyTracker;)V");
		}
		
		
		
		
		
		
		
		
		if (newRefSlot > -1) {
			bcoutputter.visitVarInsn(ALOAD, newRefSlot);// shudder
		}

		return asyncBlock.getTaggedType();
	}
	
	private int endsInAsyncBlock(Block body){
		Line lastLine = body.getLast().l;
		if(lastLine instanceof AsyncBlock){
			return 1 + endsInAsyncBlock(((AsyncBlock)lastLine).body);
		}
		else if(lastLine instanceof Block){
			Block blk = (Block)lastLine;
			if(blk.isolated){
				return endsInAsyncBlock(blk);
			}
		}
		return 0;
	}
	

	private void createOrDeferLambdaDef(LambdaDef lambdaDef) {
		String mName = lambdaDef.getMethodName();
		if (!methAlreadyGen.contains(mName)) {
			// creat method to call
			methAlreadyGen.add(mName);
			if (this.inspecialFieldInitConstrutor) {
				// may override existing, thats ok
				lambdasToAdd.peek().put(mName, new LambdaMethodDefToAddLater(this, lambdaDef));// defer
																								// for
																								// later
				// prepend with this
				// mv.visitVarInsn(ALOAD, 0);
			} else {// right now!
				createMethod(lambdaDef, this);
			}
		}
	}

	@Override
	public Object visit(LambdaDef lambdaDef) {
		createOrDeferLambdaDef(lambdaDef);

		if (!lambdaDef.isPreceededByThis() && this.isInClass()) {
			// mv.visitVarInsn(ALOAD, 0);//this probably has some unwated
			// sideffects, consider this...
		}

		visit(lambdaDef.fakeFuncRef); // invoke constructor for lambda defined
										// above

		return lambdaDef.getTaggedType();
	}

	private void createMethod(FuncDefI methodOrLambda, BytecodeGennerator vis) {
		level++;
		FuncParams pars = methodOrLambda.getParams();
		ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> inputs = pars == null ? null : pars.getAsTypesAndNames();
		
		if(methodOrLambda instanceof FuncDef){
			FuncDef asFd = (FuncDef)methodOrLambda;
			if(asFd.extFunOn != null){
				inputs.add(0,new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(asFd.extFunOn, "this$extFunc", null, false, false, false));
			}
		}
		
		Type returnType = methodOrLambda.getReturnType();
		Block body = methodOrLambda.getBody();

		Annotations annots = methodOrLambda.getAnnotations();
		if(null != annots){
			annots.bcVisitor = this;//bit of a hack
		}
		
		int nn=0;
		for(Sixple<Type, String, Annotations, Boolean, Boolean, Boolean> arg : inputs ){
			Annotations annot = arg.getC();
			if(null != annot){
				annot.bcVisitor = this;
				for(Annotation an : annot.annotations){
					an.parameterAnnotaionArg = nn;
				}
			}
			nn++;
		}
		
		boolean isOverride = methodOrLambda instanceof FuncDef && ((FuncDef)methodOrLambda).isOverride;
		
		AccessModifier am = methodOrLambda.getAccessModifier();
		
		if(!this.currentClassDefObj.isEmpty() && this.currentClassDefObj.peek().isTrait) {
			am = AccessModifier.PUBLIC;
		}
		
		
		if (null != body) {
			enterMethod(methodOrLambda.getMethodName(), inputs, returnType, methodOrLambda.isAbstract(), !vis.isInClass(), methodOrLambda.isFinal(), am, 0, annots, isOverride, methodOrLambda.methodGenricList);// TODO:
			visitTryCatchLabels(methodOrLambda.getBody());
			if(!methodOrLambda.hasErrors) {
				vis.varsToAddToScopeOnEntry = stripOutAnnotations(inputs);
			}
			

			if (vis.isInClass()) {
				body.isLocalizedLambda = true;
			} else {
				body.staticFuncBlock = true;
			}
			
			if(methodOrLambda.hasErrors) {
				Label lbk = new Label();
				this.bcoutputter.visitLabel(lbk);
				this.bcoutputter.mv.visitLineNumber(methodOrLambda.getLine(), lbk);
				this.bcoutputter.visitTypeInsn(NEW, "java/lang/Error");
				this.bcoutputter.visitInsn(DUP);
				this.bcoutputter.visitLdcInsn("Unresolved compilation problem");
				this.bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/lang/Error", "<init>", "(Ljava/lang/String;)V", false);
				this.bcoutputter.visitInsn(ATHROW);
			}else {
				body.accept(vis);
			}
			
			
			exitMethod();
		}
		else{//abstract method
			
			boolean isGPUStubFunctionAndAbstract = methodOrLambda.isAbstract() && methodOrLambda instanceof FuncDef && ((FuncDef)methodOrLambda).isGPUStubFunction();//then dont make it abstract at all, and add special body
			
			if(isGPUStubFunctionAndAbstract) {
				methodOrLambda.setAbstract(false);
			}
			
			{
				enterMethod(methodOrLambda.getMethodName(), inputs, returnType, methodOrLambda.isAbstract(), !vis.isInClass(), methodOrLambda.isFinal(), am, 0, annots, isOverride, methodOrLambda.methodGenricList);// TODO:
				
				if(isGPUStubFunctionAndAbstract) {
					bcoutputter.visitTypeInsn(NEW, "java/lang/RuntimeException");
					bcoutputter.visitInsn(DUP);
					bcoutputter.visitLdcInsn("Method %s is an abstract gpu stub function and cannot be directly invoked");
					bcoutputter.visitInsn(ICONST_1);
					bcoutputter.visitTypeInsn(ANEWARRAY, "java/lang/Object");
					bcoutputter.visitInsn(DUP);
					bcoutputter.visitInsn(ICONST_0);
					bcoutputter.visitLdcInsn(methodOrLambda.getMethodName());
					bcoutputter.visitInsn(AASTORE);
					bcoutputter.visitMethodInsn(INVOKESTATIC, "java/lang/String", "format", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);
					bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V", false);
					bcoutputter.visitInsn(ATHROW);
				}
				
				exitMethod();
			}
			
		}
		level--;
		// exitMethod();
	}
	
	@Override
	public Object visit(FuncDef funcDef) {
		if(funcDef.gpuKernelFuncDetails != null) {
			//returns GPUKernelFuncDetails { holding-classname, name, signature, source, GPUBufferManaged[] args,} 
	
			if(funcDef.gpuKernelFuncDetails.source == null) {
				return null;
			}
			
			GPUKernelFuncDetails details = funcDef.gpuKernelFuncDetails;
			StringBuilder normalSig = new StringBuilder("(");
			StringBuilder genericSig = new StringBuilder("(");
			for(FuncParam fp : funcDef.params.params) {//
				
				if(fp.gpuVarQualifier != null) {
					
					String bstring;
					if(fp.gpuVarQualifier == GPUVarQualifier.LOCAL) {
						bstring = "com/concurnas/lang/GPUBufferLocal";
					}else {
						bstring = "com/concurnas/lang/GPUBuffer";
					}
					
					String inout = "";
					if(fp.gpuInOutFuncParamModifier != null) {
						if(fp.gpuInOutFuncParamModifier == GPUInOutFuncParamModifier.in) {
							inout = "Input";
						}else {//out
							inout = "Output";
						}
					}
					
					normalSig.append(String.format("L%s%s;", bstring, inout));
					Type tt = TypeCheckUtils.unboxTypeIfBoxed(fp.type);
					genericSig.append(String.format("L%s%s<%s>;", bstring, inout, tt.getGenericBytecodeType()));
				}else {
					normalSig.append(fp.type.getBytecodeType());
					genericSig.append(fp.type.getGenericBytecodeType());
				}
			}
			
			normalSig.append(")Lcom/concurnas/lang/gpus$Kernel;");
			genericSig.append(")Lcom/concurnas/lang/gpus$Kernel;");
			
			String sig = normalSig.toString();
			String signGenerics = genericSig.toString();//"(Lcom/concurnas/lang/GPUBuffer<Ljava/lang/Integer;>;Lcom/concurnas/lang/GPUBuffer;)LElmo$Kernel;";
			FrameSateTrackingMethodVisitor  prev = bcoutputter.mv;
			bcoutputter.mv = new FrameSateTrackingMethodVisitor(cw.visitMethod(ACC_PUBLIC + ACC_STATIC, funcDef.funcName, sig, signGenerics, null), ACC_PUBLIC + ACC_STATIC, sig, getFullModuleAndClassName());
			
			if(null != funcDef.annotations) {
				funcDef.annotations.accept(this);
			}
			
			visitGPUKernelFunctionAnnotation(bcoutputter.mv.visitAnnotation("Lcom/concurnas/lang/GPUKernelFunction;", true), funcDef.gpuKernelFuncDetails);
			
			bcoutputter.mv.visitCode();
			bcoutputter.visitLabel(new Label());
			
			if(funcDef.kernelDim != null){
				
				if(funcDef.getBody() == null) {
					bcoutputter.visitTypeInsn(NEW, "com/concurnas/lang/gpus$GPUException");
					bcoutputter.visitInsn(DUP);
					bcoutputter.visitLdcInsn("GPU kernel stub functions may not be directly invoked");
					bcoutputter.visitMethodInsn(INVOKESPECIAL, "com/concurnas/lang/gpus$GPUException", "<init>", "(Ljava/lang/String;)V", false);
					bcoutputter.visitInsn(ATHROW);
				}else {
					bcoutputter.visitTypeInsn(NEW, "com/concurnas/lang/gpus$Kernel");
					bcoutputter.visitInsn(DUP);
					
					bcoutputter.visitLdcInsn(org.objectweb.asm.Type.getType("L" + getFullModuleAndClassName() + ";"));
					Utils.intOpcode(bcoutputter, details.dims);
					bcoutputter.visitLdcInsn(details.name);
					bcoutputter.visitLdcInsn(details.signature);
					bcoutputter.visitLdcInsn(details.source);
					
					
					//dependancies
					Utils.intOpcode(bcoutputter, details.dependancies.length);
					bcoutputter.visitTypeInsn(ANEWARRAY, "com/concurnas/lang/gpus$KernelDependancy");
					int cnt = 0;
					for(GPUKernelFuncDetails item : details.dependancies) {
						bcoutputter.visitInsn(DUP);
						Utils.intOpcode(bcoutputter, cnt++);
						
						bcoutputter.visitTypeInsn(NEW, "com/concurnas/lang/gpus$KernelDependancy");
						bcoutputter.visitInsn(DUP);
						bcoutputter.visitLdcInsn(item.dclass);
						bcoutputter.visitIntInsn(BIPUSH, item.dims);
						bcoutputter.visitLdcInsn(item.name);
						bcoutputter.visitLdcInsn(item.signature);
						bcoutputter.visitLdcInsn(item.globalLocalConstant);
						bcoutputter.visitLdcInsn(item.inout);
						bcoutputter.visitMethodInsn(INVOKESPECIAL, "com/concurnas/lang/gpus$KernelDependancy", "<init>", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
						
						bcoutputter.visitInsn(AASTORE);
					}
					
					
					//args
					Utils.intOpcode(bcoutputter, funcDef.params.params.size());
					bcoutputter.visitTypeInsn(ANEWARRAY, "com/concurnas/lang/gpus$KerenelArg");
					cnt = 0;
					while(cnt < funcDef.params.params.size()) {
						FuncParam fp = funcDef.params.params.get(cnt);
						
						bcoutputter.visitInsn(DUP);
						Utils.intOpcode(bcoutputter, cnt);
						bcoutputter.visitTypeInsn(NEW, "com/concurnas/lang/gpus$KerenelArg");
						bcoutputter.visitInsn(DUP);

						if(fp.gpuVarQualifier != null) {
							if(fp.gpuVarQualifier == GPUVarQualifier.LOCAL) {
								bcoutputter.visitFieldInsn(GETSTATIC, "com/concurnas/lang/gpus$KernelArgType", "LOCAL", "Lcom/concurnas/lang/gpus$KernelArgType;");
							}else {
								bcoutputter.visitFieldInsn(GETSTATIC, "com/concurnas/lang/gpus$KernelArgType", "BUFFER", "Lcom/concurnas/lang/gpus$KernelArgType;");
							}
							
							bcoutputter.visitVarInsn(ALOAD, cnt);
						}else {
							bcoutputter.visitFieldInsn(GETSTATIC, "com/concurnas/lang/gpus$KernelArgType", "NORMAL", "Lcom/concurnas/lang/gpus$KernelArgType;");
							Utils.applyLoad(bcoutputter, fp.type, cnt);
							Utils.box(bcoutputter, fp.type);
						}
						
						bcoutputter.visitMethodInsn(INVOKESPECIAL, "com/concurnas/lang/gpus$KerenelArg", "<init>", "(Lcom/concurnas/lang/gpus$KernelArgType;Ljava/lang/Object;)V", false);
						bcoutputter.visitInsn(AASTORE);
						cnt++;
					}
					
					bcoutputter.visitMethodInsn(INVOKESPECIAL, "com/concurnas/lang/gpus$Kernel", "<init>", "(Ljava/lang/Class;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;[Lcom/concurnas/lang/gpus$KernelDependancy;[Lcom/concurnas/lang/gpus$KerenelArg;)V", false);
					bcoutputter.visitInsn(ARETURN);
				}
			}else {//function, not callable return exception
				bcoutputter.visitTypeInsn(NEW, "com/concurnas/lang/gpus$GPUException");
				bcoutputter.visitInsn(DUP);
				bcoutputter.visitLdcInsn("GPU functions may not be directly invoked");
				bcoutputter.visitMethodInsn(INVOKESPECIAL, "com/concurnas/lang/gpus$GPUException", "<init>", "(Ljava/lang/String;)V", false);
				bcoutputter.visitInsn(ATHROW);
			}
			
			Label l1 = new Label();
			bcoutputter.visitLabel(l1);
			bcoutputter.mv.visitMaxs(11, 2);
			bcoutputter.mv.visitEnd();
			bcoutputter.mv = prev;
			
			return null;
		} else {
			if( isFieldBlockRhs && isInsideDefaultConstuctorFieldInit){
				return null;
			}
			if (funcDef.isNestedFunc) {
				Block body = funcDef.getBody();
				body.localArgVarOffset = !this.currentClassDef.isEmpty() ? 1 : 0;// in class argsstart from 1 [0 is this reference]
			}
			
			if(!funcDef.funcName.startsWith("NIF$") && this.currentScopeFrame.isFuncDefBlock && !funcDef.isNestedFunc && NestedFuncRepoint.isDefaultFuncDef(funcDef)) {
				return null;//skip as will have NIF version with correct repoints
			}
			
			createMethod(funcDef, this);
			
			if(funcDef.requiresBridgeMethodTo != null) {
				createBridgeMethod(funcDef);
			}
			
			if(funcDef.createTraitStaticMethod) {
				createTraitStaticMethod(funcDef);
			}
			
			return null;// null ok
		}
	}
	
	private void createBridgeMethod(FuncDef funcDef) {
		String name = funcDef.funcName;
		
		FuncParams pars = funcDef.requiresBridgeMethodTo.origonatingFuncDef.params;
		ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> inputs = pars == null ? null : pars.getAsTypesAndNames();
		
		enterMethod(name, inputs, funcDef.requiresBridgeMethodTo.retType, funcDef.isAbstract(), funcDef.accessModifier, ACC_BRIDGE + ACC_SYNTHETIC, true);
		//(String name, ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> inputs, Type returnType, boolean isAbstract, AccessModifier am, int extraModifiers, boolean isOverride)
		//MethodVisitor mv = this.cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, name, "(I)LElmo$Abs;", null, null);
		
		bcoutputter.visitNewLine(funcDef.getLine()); 
		
		bcoutputter.visitVarInsn(ALOAD, 0);
		//load args
		int space = 0;
		StringBuilder sig = new StringBuilder("(");
		for(Sixple<Type, String, Annotations, Boolean, Boolean, Boolean> inp : inputs) {
			Type tt = inp.getA();
			Utils.applyLoad(bcoutputter, tt, space);
			space += Utils.varSlotsConsumedByType(tt);
			sig.append(tt.getBytecodeType());
		}
		sig.append(")");
		sig.append(funcDef.retType.getBytecodeType());
		
		String callingclass = new NamedType(this.currentClassDefObj.peek()).getBytecodeType();
		callingclass=callingclass.substring(1, callingclass.length()-1);
		
		//correct signature
		
		bcoutputter.visitMethodInsn(INVOKEVIRTUAL, callingclass, name, sig.toString(), false);
		Utils.applyCastImplicit(bcoutputter, funcDef.retType, funcDef.requiresBridgeMethodTo.retType, this);
		bcoutputter.visitInsn( Utils.returnTypeToOpcode(funcDef.requiresBridgeMethodTo.retType));
		exitMethod();
	}
	
	
	private void addTraitSuperRefStubsImpls(List<Fourple<String, String, Boolean, TypeAndLocation>> toAdds) {
		
		for(Fourple<String, String, Boolean, TypeAndLocation> toAdd : toAdds) {
			TypeAndLocation tal = toAdd.getD();
			boolean IsSuperCall = toAdd.getC();
			ClassFunctionLocation clsFuncLoc = (ClassFunctionLocation)tal.getLocation();
			FuncType ft = (FuncType)tal.getType();
			FuncDef origonal = ft.origonatingFuncDef;
			
			ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>  inputs =  origonal.getParams().getAsTypesAndNames();
			enterMethod(toAdd.getA(), inputs, origonal.getReturnType(), false, false, false, AccessModifier.PUBLIC, ACC_SYNTHETIC, null, false, origonal.methodGenricList);// TODO:
			if(!IsSuperCall) {
				inputs.add(0, new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(new NamedType(origonal.origin), "this", null, false, false, false));
			}
			
			bcoutputter.visitVarInsn(ALOAD, 0);
			//load args
			int space = !IsSuperCall?0:1;
			StringBuilder sig = new StringBuilder("(");
			for(Sixple<Type, String, Annotations, Boolean, Boolean, Boolean> inp : inputs) {
				Type tt = inp.getA();
				sig.append(tt.getBytecodeType());
				if(space == 0) {
					space++;
					continue;
				}
				Utils.applyLoad(bcoutputter, tt, space);
				space += Utils.varSlotsConsumedByType(tt);
			}
			sig.append(")");
			sig.append(origonal.retType.getBytecodeType());
			
			String callingclass = clsFuncLoc.ownerType.getBytecodeType();
			callingclass=callingclass.substring(1, callingclass.length()-1);
			//correct signature
			
			if(IsSuperCall) {//is super call
				bcoutputter.visitMethodInsn(INVOKESPECIAL, callingclass, toAdd.getB(), sig.toString(), false);
			}else {
				bcoutputter.visitMethodInsn(INVOKESTATIC, callingclass, toAdd.getB()+"$traitM", sig.toString(), true);
			}
			
			bcoutputter.visitInsn( Utils.returnTypeToOpcode(origonal.retType));
			exitMethod();
		}
	}
	
	
	private void addTraitSuperRefStubs(HashSet<Pair<String, FuncType>> toAdds) {
		/*create likes of:
		   public boolean equals(java.lang.Object);
		    Code:
		       0: aload_0
		       1: aload_1
		       2: invokestatic  #25                 // InterfaceMethod example/EQHashFreee.equals$:(Lexample/EQHashFreee;Ljava/lang/Object;)Z
		       5: ireturn
		 */
		
		
		for(Pair<String, FuncType> tal : toAdds) {
			FuncType ft = tal.getB();
			FuncDef origonal = ft.origonatingFuncDef;
			
			ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>  inputs =  origonal.getParams().getAsTypesAndNames();
			enterMethod(tal.getA(), inputs, origonal.getReturnType(), true, false, false, AccessModifier.PUBLIC, ACC_SYNTHETIC , null, false, origonal.methodGenricList);// TODO:
			exitMethod();
		}
	}
	
	
	
	
	
	
	
	private void addTraitDirectingMethods(List<TypeAndLocation> toAdds) {
		/*create likes of:
		   public boolean equals(java.lang.Object);
		    Code:
		       0: aload_0
		       1: aload_1
		       2: invokestatic  #25                 // InterfaceMethod example/EQHashFreee.equals$:(Lexample/EQHashFreee;Ljava/lang/Object;)Z
		       5: ireturn
		 */
		
		
		for(TypeAndLocation tal : toAdds) {
			FuncType ft = (FuncType)tal.getType();
			FuncDef origonal = ft.origonatingFuncDef;
			if(origonal.origin == null) {
				continue;
			}
			
			ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>  inputs =  origonal.getParams().getAsTypesAndNames();
			enterMethod(origonal.getMethodName(), inputs, origonal.getReturnType(), false, false, false, origonal.getAccessModifier(), ACC_SYNTHETIC, null, false, origonal.methodGenricList);// TODO:
			inputs.add(0, new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(new NamedType(origonal.origin), "this", null, false, false, false));
			
			bcoutputter.visitVarInsn(ALOAD, 0);
			//load args
			int space = 0;
			StringBuilder sig = new StringBuilder("(");
			for(Sixple<Type, String, Annotations, Boolean, Boolean, Boolean> inp : inputs) {
				Type tt = inp.getA();
				sig.append(tt.getBytecodeType());
				if(space == 0) {
					space++;
					continue;
				}
				Utils.applyLoad(bcoutputter, tt, space);
				space += Utils.varSlotsConsumedByType(tt);
			}
			sig.append(")");
			sig.append(origonal.retType.getBytecodeType());
			
			String callingclass = new NamedType(origonal.origin).getBytecodeType();
			callingclass=callingclass.substring(1, callingclass.length()-1);
			//correct signature
			bcoutputter.visitMethodInsn(INVOKESTATIC, callingclass, origonal.getMethodName()+"$traitM", sig.toString(), true);//TODO: may not work on Java 1.8
			bcoutputter.visitInsn( Utils.returnTypeToOpcode(origonal.retType));
			exitMethod();
		}
	}
	
	private void createTraitStaticMethod(FuncDef funcDef) {
		//create for non abstract trait methods
		String name = funcDef.funcName;

		/*
		 public boolean equals(java.lang.Object);
		    Code:
		       0: iconst_0
		       1: ireturn
		  //add:
		 public static boolean equals$(example.EQHashFreee, java.lang.Object);
		    Code:
		       0: aload_0
		       1: aload_1
		       2: invokespecial #24                 // InterfaceMethod equals:(Ljava/lang/Object;)Z
		       5: ireturn
		  

		 */
				
		ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> inputs = new ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>();
		if(null != funcDef.params) {
			inputs.addAll(funcDef.params.getAsTypesAndNames());
		}
		ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>> inputsExtraArg = new ArrayList<Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>>(inputs);
		inputsExtraArg.add(0, new Sixple<Type, String, Annotations, Boolean, Boolean, Boolean>(new NamedType(funcDef.origin), "this", null, false, false, false));
		
		enterMethod(name + "$traitM", inputsExtraArg, funcDef.retType, false, AccessModifier.PUBLIC, ACC_SYNTHETIC + ACC_STATIC, true);
		
		bcoutputter.visitNewLine(funcDef.getLine()); 
		bcoutputter.visitVarInsn(ALOAD, 0);
		//load args
		int space = 1;
		StringBuilder sig = new StringBuilder("(");
		for(Sixple<Type, String, Annotations, Boolean, Boolean, Boolean> inp : inputs) {
			Type tt = inp.getA();
			Utils.applyLoad(bcoutputter, tt, space);
			space += Utils.varSlotsConsumedByType(tt);
			sig.append(tt.getBytecodeType());
		}
		sig.append(")");
		sig.append(funcDef.retType.getBytecodeType());
		
		String callingclass = new NamedType(this.currentClassDefObj.peek()).getBytecodeType();
		callingclass=callingclass.substring(1, callingclass.length()-1);
		//correct signature
		bcoutputter.visitMethodInsn(INVOKESPECIAL, callingclass, name, sig.toString(), true);//TODO: may not work on Java 1.8
		bcoutputter.visitInsn( Utils.returnTypeToOpcode(funcDef.retType));
		exitMethod();
	}
	
	

	private boolean isConstructingNestedChild(ClassDef rhs) {
		if (!this.currentClassDefObj.isEmpty()) {
			ClassDef parentMaybe = this.currentClassDefObj.peek();
			return rhs.isParentNestorOrSuper(parentMaybe);
		}
		return false;
	}

	private boolean prefixNestedconstructor(ClassDef meClass, Node namedConstructor){
		
		if(meClass.isLocalClass){
			return false;
		}
		
		
		boolean isprefixedWithParentNestor;
		if (isConstructingNestedChild(meClass)) {
			if (!namedConstructor.isPreceededByThis()) {
				bcoutputter.visitVarInsn(ALOAD, 0);
			}
			isprefixedWithParentNestor = true;
		} else {
			isprefixedWithParentNestor = dotOpExprCurrentProcess != null && dotOpExprCurrentProcess == namedConstructor;
		}
		return isprefixedWithParentNestor;
	}
	
	@Override
	public Object visit(New namedConstructor) {

		NamedType t = (NamedType)((Node) namedConstructor.typeee).getTaggedType();
		String tString = t.getBytecodeType();
		tString = tString.substring(1, tString.length() - 1);

		ClassDef meClass = t.getSetClassDef();

		boolean onActor = null != namedConstructor.actingOn;
		
		NamedType obtainParentFrom = onActor?namedConstructor.actingOn:t;
		boolean nestedActor = false;
		if(TypeCheckUtils.isTypedOrUntypedActor(this.errorRaisableSupressionFromSatc, t)){
			onActor=true;
			
			if(null == t.getparentNestorFakeNamedType()){//if the actor itself is not nested... as nested cannot operates on nested types
				//actually cheat and just not permit nested types to be actors
				Type theRet = TypeCheckUtils.extractTypedActorType(this.errorRaisableSupressionFromSatc, t);
				if(theRet != null && theRet instanceof NamedType){
					obtainParentFrom = (NamedType)theRet;
				}
			}
			else
			{
				nestedActor=true;
			}
		}
		
		
		ClassDef parentClass = obtainParentFrom.getSetClassDef().isNestedAndNonStatic() ? obtainParentFrom.getSetClassDef().getParentNestor() : null; 
		
		boolean isActorDeclLocalAndOperatingOnNestedType = false;//clearly
		
		if(null != parentClass && meClass.isActor){
			Expression prev = namedConstructor.getPreceedingExpression();
			if(null != prev){
				Type prevt = prev.getTaggedType();
				if(prevt instanceof NamedType){//what else could it be?
					if(((NamedType)prevt).getSetClassDef().equals(parentClass)){
						isActorDeclLocalAndOperatingOnNestedType=true;
						parentClass=null;
					}
				}
			}
		}
		
		
		if(null != namedConstructor.enumItemNameAndIndex){//enum subclasses though are nested, are not themselves nested
			parentClass=null;
		}
		
		boolean isprefixedWithParentNestor = prefixNestedconstructor(meClass, namedConstructor);

		Type theType = namedConstructor.getTaggedType();
		
		if(namedConstructor.newOpOverloaded != null){
			namedConstructor.newOpOverloaded.accept(this);
			if(namedConstructor.newOpOverloadedNeedsCast){
				doCast(namedConstructor.newOpOverloaded.getTaggedType(), theType);
			}
		}else{
			bcoutputter.visitTypeInsn(NEW, tString);
			

			if ((parentClass != null && isprefixedWithParentNestor) || isActorDeclLocalAndOperatingOnNestedType) {
				// nested so we have a stack with newobj, newobj, parent...
				// cos we already got parent, newobj on stack
				bcoutputter.visitInsn(SWAP);
				bcoutputter.visitInsn(DUP2);
				bcoutputter.visitInsn(POP);
				bcoutputter.visitInsn(SWAP);// lol
				/*
				 * -> mv.visitTypeInsn(NEW, "A/Child2$Outer$InnerClass");
				 * mv.visitInsn(DUP); mv.visitVarInsn(ALOAD, 1);
				 */
			} else {
				bcoutputter.visitInsn(DUP);
			}
			
			/*
			 * Add code for tricky case: fun watchout(o Outer) AnotherInner { return
			 * o.new AnotherInner(o.x) } --> mv.visitTypeInsn(NEW,
			 * "A/Child2$Outer$One"); mv.visitInsn(DUP); mv.visitVarInsn(ALOAD, 1);
			 * mv.visitIntInsn(BIPUSH, 12); mv.visitMethodInsn(INVOKESPECIAL,
			 * "A/Child2$Outer$One", "<init>", "(LA/Child2$Outer;I)V");
			 */

			if (parentClass != null && !isprefixedWithParentNestor /*&& !obtainParentFrom.getSetClassDef().isLocalClass*/) {
				moveUpNestingHierarchyToGoal(this.currentClassDefObj.peek(), parentClass);

				/*
				 * nestor where common nestor is held... if indirect refernce
				 * mv.visitTypeInsn(NEW, "A/Child2$Outer$One"); mv.visitInsn(DUP);
				 * 
				 * moveUpNestingHierarchyToGoal()-->
				 * 
				 * mv.visitVarInsn(ALOAD, 0); mv.visitFieldInsn(GETFIELD,
				 * "A/Child2$Outer$Twp$Trhee", "this$2", "LA/Child2$Outer$Twp;");
				 * mv.visitMethodInsn(INVOKESTATIC, "A/Child2$Outer$Twp",
				 * "access$0", "(LA/Child2$Outer$Twp;)LA/Child2$Outer;");
				 * mv.visitMethodInsn(INVOKESPECIAL, "A/Child2$Outer$One", "<init>",
				 * "(LA/Child2$Outer;)V");
				 */
			}

			
			FuncType funcType = namedConstructor.constType;
			ArrayList<Type> inputArgs = funcType.getInputs() == null ? new ArrayList<Type>() : (ArrayList<Type>) funcType.getInputs().clone();
			ArrayList<Type> inputArgsForMethod = funcType.defaultFuncArgs!=null?funcType.defaultFuncArgs: (ArrayList<Type>) inputArgs.clone(); 
			if ((parentClass != null && !onActor) || nestedActor) {//actor has this added explicitly
				inputArgs.add(0, new NamedType(parentClass));
				inputArgsForMethod.add(0, new NamedType(parentClass));
			}
			

			if(null != namedConstructor.enumItemNameAndIndex){
				bcoutputter.visitLdcInsn(namedConstructor.enumItemNameAndIndex.getA());
				Utils.intOpcode(bcoutputter, namedConstructor.enumItemNameAndIndex.getB());
				
				inputArgs.add(0, ScopeAndTypeChecker.const_int);
				inputArgsForMethod.add(0, ScopeAndTypeChecker.const_int);
				inputArgs.add(0, ScopeAndTypeChecker.const_string);
				inputArgsForMethod.add(0, ScopeAndTypeChecker.const_string);
			}
			
			/*
			 * dunno about this
			 * 
			 * if(returnType instanceof NamedType){//JPT: nasty hack, can we always
			 * assume that upper boiund is object, also, shoudln't this be done
			 * elsewhere in the code?
			 * ((NamedType)returnType).origonalGenericTypeUpperBound = object_const;
			 * } else if(returnType instanceof FuncType){//JPT: nasty hack, can we
			 * always assume that upper boiund is object, also, shoudln't this be
			 * done elsewhere in the code?
			 * ((FuncType)returnType).origonalGenericTypeUpperBound = object_const;
			 * }
			 */

			boolean inferTypesForTypeParam = !namedConstructor.genericInputsToQualifingArgsAtRuntime.isEmpty();
			
			/*
			 * int tempSlot = this.createNewLocalVar(this.getTempVarName(), Const_Object, false);
							mv.visitVarInsn(ASTORE, tempSlot);
			 */
			
			boolean isRefConstructor = TypeCheckUtils.hasRefLevels(t);
			
			if(onActor){//included in args already...
				//inputArgs.add(0, ScopeAndTypeChecker.const_classArray_nt_array);
			}
			String methodDesc = getNormalMethodInvokationDesc(inputArgsForMethod, Const_PRIM_VOID);		
			
			//HMMM
			
			if(inferTypesForTypeParam){//oh no, we must infer some or all of the type arugments
				int nestOffset = (onActor && parentClass != null && isprefixedWithParentNestor)?1:0;
				HashMap<Integer, Pair<Integer, Type>> argToTempAndType = constructorVisitarguments(namedConstructor, inputArgs, 1+nestOffset, true, onActor, funcType.defaultFuncArgs);
				//ok all things stored, now we need to construct the type array
				Utils.createTypeArray(bcoutputter, t, new ArrayList<String>(), namedConstructor.genericInputsToQualifingArgsAtRuntime, argToTempAndType);
				
				if (onActor && parentClass != null && isprefixedWithParentNestor){
					if(!nestedActor){bcoutputter.visitInsn(SWAP);}
				}
				
				//now reload everything again...
				for (int n = 0; n < namedConstructor.args.getArgumentsWNPs().size(); n++) {
					Pair<Integer, Type> tempAndType = argToTempAndType.get(n);
					Utils.applyLoad(bcoutputter, tempAndType.getB(), tempAndType.getA());
				}
			}
			else{
				int hiddenArgoffset = makeConstuctorTypeArray(isRefConstructor, onActor, t);
				
				if (parentClass != null){
					hiddenArgoffset+=1;
					if(onActor && isprefixedWithParentNestor && !nestedActor){
						bcoutputter.visitInsn(SWAP);
					}
				}
				else if(isActorDeclLocalAndOperatingOnNestedType){
					hiddenArgoffset+=1;
					bcoutputter.visitInsn(SWAP);
				}
				
				if(null != namedConstructor.enumItemNameAndIndex){
					hiddenArgoffset+=2;
				}
				
				constructorVisitarguments(namedConstructor, inputArgs, hiddenArgoffset, false, onActor, funcType.defaultFuncArgs);
			}
			
			// mv.visitMethodInsn(INVOKESPECIAL, tString, "<init>",
			// String.format("(%s)V", sb.toString()));
			bcoutputter.visitMethodInsn(INVOKESPECIAL, tString, "<init>", methodDesc);
		}

		if (!((Node) namedConstructor).getShouldBePresevedOnStack()) {// if void u dont need pop
			Utils.popFromStack(bcoutputter, theType);
		}

		return theType;
	}
	
	private int makeConstuctorTypeArray(boolean isRefConstructor, boolean onActor, NamedType t){
		int hiddenArgoffset;
		if(isRefConstructor){
			hiddenArgoffset = 1;
			//ArrayList<String> types = ;
			//types.add( "com/concurnas/runtime/ref/Local" );
			Utils.createTypeArray(bcoutputter, t, new ArrayList<String>(), null, null);
		}
		else if(onActor){
			hiddenArgoffset = 1;
			List<Type> gens = t.getGenTypes();
			if(gens == null || gens.isEmpty()){
				//no types to pass in
				bcoutputter.visitInsn(ICONST_0);
				bcoutputter.visitTypeInsn(ANEWARRAY, "java/lang/Class");
			}
			else{//TODO: really the createTypeArray should not create a array with the type of the thing being passed alone, that needs refactoring
				Utils.createTypeArray(bcoutputter, t, new ArrayList<String>(), null, null);
			}
		}
		else{
			hiddenArgoffset = 0;
		}
		
		return hiddenArgoffset;
	}
	
	private HashMap<Integer, Pair<Integer, Type>> constructorVisitarguments(New namedConstructor, ArrayList<Type> inputArgs, int hiddenArgoffset, final boolean storeTotemps, final boolean isActorConstructor, ArrayList<Type> defaultArgs){
		
		if(null == namedConstructor.args) {
			return null;
		}
		
		List<Expression> args = namedConstructor.args.getArgumentsWNPs();
		
		Pair<Integer, HashMap<Integer, Type>> defpp = defaultParamPrelude(defaultArgs, args, isActorConstructor);
		
		int defaultParamObjSlot = defpp.getA();
		HashMap<Integer, Type> toAddDefaultParamUncreate = defpp.getB();
		
		HashMap<Integer, Pair<Integer, Type>> argNumberToTempVariable = storeTotemps?new HashMap<Integer, Pair<Integer, Type>>():null;
		
		//HMMM
		if(args.size() > 0) {
			//HERHERHERH
			int tempTrackerSlot=-1;
			  if(isActorConstructor) {//isArrayLessPrimativeOrString to copy... use conctracker
				for (int n = 0; n < args.size(); n++) {
					Expression expr = args.get(n);
					
					boolean isShared = false;
					if(expr instanceof RefName) {
						isShared = ((RefName)expr).resolvesTo.getLocation().isShared();
					}
					
					if(null != expr){
						if (!TypeCheckUtils.shouldNotBeCopied(inputArgs.get(n+hiddenArgoffset), isShared)) {//TODO:isPrimativeOrString add other obvious immutable primatives
							makeConctracker();
							tempTrackerSlot = this.createNewLocalVar(this.getTempVarName(), Const_Object, true);
							break;
						}
					}
				}
			}
			
			for (int n = 0; n < args.size(); n++) {
				Expression expr = args.get(n);
				boolean isDefault = toAddDefaultParamUncreate.containsKey(n+hiddenArgoffset);
				
				if(null == expr){
					processNullArgExpression(toAddDefaultParamUncreate, n+hiddenArgoffset);
				}
				else{
					this.dorOpLHS.push(null);
					Type got = (Type) expr.accept(this);
					this.dorOpLHS.pop();
					Type expect = inputArgs.get(n+hiddenArgoffset);
					if (!got.equals(expect) || TypeCheckUtils.typeRequiresLocalArrayConvertion(expect)) {
						if(!TypeCheckUtils.hasRefLevels(expect) && !expect.equals(Const_Object)  ){
							TypeCheckUtils.unlockAllNestedRefs(got);
						}
						Utils.applyCastImplicit(bcoutputter, got, expect, this);
					}
					
					if(isActorConstructor){
						boolean isShared = false;
						if(expr instanceof RefName) {
							isShared = ((RefName)expr).resolvesTo.getLocation().isShared();
						}
						
						if (!TypeCheckUtils.shouldNotBeCopied(expect, isShared)) {//TODO:isPrimativeOrString add other obvious immutable primatives
							this.doCopy(expect, null, tempTrackerSlot, null, null);
						}
					}
					
					if(storeTotemps){
						int tempSlot = this.createNewLocalVar(this.getTempVarName(), expect, false);
						Utils.applyStore(bcoutputter, expect, tempSlot);
						argNumberToTempVariable.put(n, new Pair<Integer, Type>(tempSlot, expect));
					}
					if(isDefault){
						/*if(defaultParamObjSlot == -1) {
							bcoutputter.visitInsn(ACONST_NULL);
						}else {*/
							bcoutputter.visitVarInsn(ALOAD, defaultParamObjSlot);
						//}
					}
				}
			}
		}
		
		
		return argNumberToTempVariable;
	}
	

	private static NamedType object_const = new NamedType(new ClassDefJava(java.lang.Object.class));

	private void unrefToHeadTypeOfDotOp(Type unrefTo) {
		if (!this.dorOpLHS.isEmpty()) {
			Type toOfStack = this.dorOpLHS.peek();
			int target = TypeCheckUtils.getRefLevels(unrefTo);
			while (TypeCheckUtils.hasRefLevelsAndNotLocked(toOfStack) && TypeCheckUtils.getRefLevels(toOfStack) > target) {
				toOfStack = Utils.unref(bcoutputter, toOfStack, this);
			}
		}
	}

	private boolean isExtensionMethod(FuncType tt, Location loc){
		if(loc instanceof ClassFunctionLocation){
			return tt.extFuncOn;
		}
		return false;
	}
	
	@Override
	public Object visit(FuncInvoke funcInvoke) {
		if (funcInvoke.astRedirectforOnChangeNesting != null) {
			return funcInvoke.astRedirectforOnChangeNesting.accept(this);
		}

		/*if(funcInvoke.popOnEntry){//only really if we are calling a static method
			mv.visitInsn(Opcodes.POP);
		}*/
		
		String name = funcInvoke.funName;
		
		TypeAndLocation df = funcInvoke.resolvedFuncTypeAndLocation;
		FuncType funcType = (FuncType) TypeCheckUtils.getRefType(df.getType());
		
		/*if(funcType.origonatingFuncDef != null && null != funcType.origonatingFuncDef.funcName){
			if(!"<init>".equals(funcType.origonatingFuncDef.funcName) && !funcType.origonatingFuncDef.funcName.contains(".")){
				//name = funcType.origonatingFuncDef.funcName;
				//find a better way
			}
		}
		*/
		
		
		if(funcInvoke.bytecodefunName != null){
			name = funcInvoke.bytecodefunName;
		}
		
		boolean isLambda = funcType.getLambdaDetails() != null;

		Location funcLoc = df.getLocation();
		if(funcLoc instanceof ClassFunctionLocation){
			ClassFunctionLocation cfl = (ClassFunctionLocation)funcLoc;
			if(cfl.castToCOBject){
				bcoutputter.visitTypeInsn(CHECKCAST, "com/concurnas/bootstrap/runtime/cps/CObject");
			}
		}
		
		
		if (funcLoc instanceof StaticFuncLocation && !funcType.extFuncOn) {
			if (shouldPopFromStackOnStaticCall()) {
				bcoutputter.visitInsn(POP);// clean stack for static calls
			}
		}
			
		
		// thing := ref; thing.do() //needs to be unrefed
		// TODO: where to put this code: this.unrefToHeadTypeOfDotOp(owner);
		// //thing := ref; thing.do() //needs to be unrefed ?????
		
		if(!funcInvoke.bcGenStopThingCalledOnBeingUnreffed){
			if (funcLoc instanceof StaticFuncLocation) {
				this.unrefToHeadTypeOfDotOp((NamedType) ((StaticFuncLocation) funcLoc).getOwnerType());
			} else if (funcLoc instanceof ClassFunctionLocation) {
				this.unrefToHeadTypeOfDotOp(((ClassFunctionLocation) funcLoc).getOwnerType());
			}
		}

		Type returnType = funcType.retType;// .copy();

		ArrayList<Type> funcInputs = funcType.defaultFuncArgs != null?funcType.defaultFuncArgs: funcType.getInputs();
		if (isLambda && funcInputs != null) {
			ArrayList<Type> funcInputsrepl = new ArrayList<Type>();
			for (Type i : funcInputs) {
				// generic so all object...
				// funcInputsrepl.add(TypeCheckUtils.boxTypeIfPrimative(i));
				funcInputsrepl.add(object_const);
			}
			funcInputs = funcInputsrepl;
		}

		boolean extFuncNeedingThisReferenceAdded = isExtensionMethod(funcType, funcLoc);

		if(funcType.extFuncOn && null != funcInvoke.getPreceedingExpression()){
			Utils.applyCastExplicit(bcoutputter, funcInvoke.getPreceedingExpression().getTaggedType(), funcType.inputs.get(0), this);
		}
		
		if (!funcInvoke.isPreceededByThis() || extFuncNeedingThisReferenceAdded)// HACK A DOODLE DO
		{
			prefixNodeWithThisreference(funcLoc, false);
		}
		
		if(extFuncNeedingThisReferenceAdded){
			bcoutputter.visitInsn(SWAP);
		}
		
		if (funcLoc.isLambda() && !(funcLoc instanceof FuncLocation)) {
			Type gotFieldMaybe = loadLocalVar(name, df);

			gotFieldMaybe = Utils.unref(bcoutputter, gotFieldMaybe, this);

			if (gotFieldMaybe instanceof FuncType && null != ((FuncType) gotFieldMaybe).getOrigonalGenericTypeUpperBound() && !gotFieldMaybe.equals(((FuncType) gotFieldMaybe).getOrigonalGenericTypeUpperBound())) {
				// JPT:  copy paste code yuck FuncType nt = (FuncType)gotFieldMaybe; String castTo = nt.getBytecodeType();//nt.getSetClassDef().bcFullName();
				String castTo = ((FuncType) gotFieldMaybe).getPoshObjectStyleName().getCheckCastType();
				bcoutputter.visitTypeInsn(CHECKCAST, castTo);
			}
		}

		int invokeOpcode =INVOKEVIRTUAL;
		if (funcLoc.isLambda()) {
			String owner = funcLoc.getLambdaOwner();
			if(owner.startsWith(FuncType.classRefIfacePrefix) && owner.endsWith(FuncType.classRefIfacePostfix)){
				bcoutputter.visitTypeInsn(CHECKCAST, owner);
				invokeOpcode = INVOKEINTERFACE;
			}
		}
		this.dorOpLHS.push(null);
		acceptFuncInvokeArgs(funcInvoke, funcType, isLambda, funcType.defaultFuncArgs);
		this.dorOpLHS.pop();

		if (funcLoc.isLambda()) {
			Type returnTypeForMethodStr;
			Type boxedRet = TypeCheckUtils.boxTypeIfPrimative(returnType, false);
			if(returnType.equals(ScopeAndTypeChecker.const_void)) {
				returnTypeForMethodStr = returnType;
			}else {
				returnTypeForMethodStr = boxedRet;
				if (returnTypeForMethodStr.hasArrayLevels() || returnTypeForMethodStr instanceof GenericType || returnTypeForMethodStr instanceof NamedType || returnTypeForMethodStr instanceof FuncType || (returnTypeForMethodStr instanceof PrimativeType && ((PrimativeType) returnTypeForMethodStr).type == PrimativeTypeEnum.VOID)) {
					//lambdas are always generic, so object (and the bridge method does the magic)
					returnTypeForMethodStr = Const_Object.copyTypeSpecific();
				}
			}
			
			
			String methodDesc = getNormalMethodInvokationDesc(funcInputs, returnTypeForMethodStr);
			// mv.visitMethodInsn(INVOKEINTERFACE, funcLoc.getLambdaOwner(),
			// "apply", methodDesc);
			String owner = funcLoc.getLambdaOwner();
			
			bcoutputter.visitMethodInsn(invokeOpcode, owner, "apply", methodDesc);

			if (funcInvoke.getShouldBePresevedOnStack()) {
				// TODO: bug here because u can sometimes have two Checkcast in
				// a rows, seems not useful
				// TODO: i think there is another bug when the return type is a
				// FuncType... since this doesnt get preserved as such,... oh
				// well....
				if (!returnTypeForMethodStr.equals(boxedRet) && (boxedRet instanceof NamedType)) {
					// we had to convert from thingy to Object since it only
					// does generics (via the lambda bridge method of course)
					
					bcoutputter.visitTypeInsn(CHECKCAST, ((NamedType) boxedRet).getCheckCastType());
					// returnType = returnTypeForMethodStr;
				}

				if (!boxedRet.equals(returnType)) {
					Utils.applyCastImplicit(bcoutputter, boxedRet, returnType, this);
					// returnType = returnTypeForMethodStr;
				}else if(boxedRet.hasArrayLevels()){
					returnTypeForMethodStr.setArrayLevels(boxedRet.getArrayLevels());

					Utils.doCheckCastConvertArrays(bcoutputter, returnTypeForMethodStr, boxedRet.getCheckCastType());
					
					//mv.visitTypeInsn(CHECKCAST, returnTypeForMethodStr.getCheckCastType());//ensure cast to Object[] ready for next conversion from Object[] to Function[]
					
				}
			} else {
				returnType = returnTypeForMethodStr;
			}
		} else if (funcLoc instanceof StaticFuncLocation) {
			String methodDesc = getNormalMethodInvokationDesc(funcInputs, returnType);

			StaticFuncLocation sfl = (StaticFuncLocation) funcLoc;
			NamedType owner = (NamedType) sfl.getOwnerType();
			Pair<String, String> accessorNameOrig = sfl.getPrivateStaticAccessorRedirectFuncGetter();
			if (null != accessorNameOrig && !isInclInitCreator) {
				name = accessorNameOrig.getA();
			}
			bcoutputter.visitMethodInsn(INVOKESTATIC, owner.getNamedTypeStr().replace('.', '/'), name, methodDesc);
		} else if (funcLoc instanceof ClassFunctionLocation) {
			ClassFunctionLocation fl = (ClassFunctionLocation) funcLoc;
			Type ownerType = fl.getOwnerType();
			int invokeOpCode = INVOKEVIRTUAL;

			// if(fl.isInterface){
			if (ownerType instanceof NamedType && ((NamedType) ownerType).isInterface()) {
				invokeOpCode = INVOKEINTERFACE;
			} else if (funcInvoke.isPreceededBySuper() && !this.currentClassDefObj.isEmpty() && ownerType instanceof NamedType && (this.currentClassDefObj.peek().isBeingBeingPastTheParentOfMe(((NamedType) ownerType).getSetClassDef()) || ownerType.equals(Const_Object))) {
				//is preceeded by this else calling a method o a variable which is passed in externally results in it 
				//using invoke special and failing since its not really a superclass call
				invokeOpCode = INVOKESPECIAL;
			}
			
			if(fl.isInterface){
				invokeOpCode = INVOKEINTERFACE;
			}
			
			// invoke special for private and superclass of current class
			Type retTypeForFunction = funcType.realReturnType;
			if (null != retTypeForFunction && !retTypeForFunction.equals(returnType)) {
				// only really used here: ia2 int[] = ia1.clone(); //method
				// returns Object, so we cast to int[]
				// MHA: since this is only relevant for the above case with
				// arrays online, diverting the owner to =
				// returnType.getCheckCastType()!
				bcoutputter.visitMethodInsn(invokeOpCode, returnType.getCheckCastType(), name, getNormalMethodInvokationDesc(funcInputs, retTypeForFunction));
				bcoutputter.visitTypeInsn(CHECKCAST, returnType.getCheckCastType());
			} else {
				Pair<String, String> accessorNameOrig = fl.getPrivateStaticAccessorRedirectFuncGetter();
				if (null != accessorNameOrig && !isInclInitCreator) {
					/*
					 * { mv.visitFieldInsn(GETFIELD, "A/Waskle$Waskle2",
					 * "this$0", "LA/Waskle;"); mv.visitMethodInsn(INVOKESTATIC,
					 * "A/Waskle", "access$0", "(LA/Waskle;)V"); }
					 */

					name = accessorNameOrig.getA();

					ArrayList<Type> funcInputsAccessor = new ArrayList<Type>(funcInputs.size()+1);
					funcInputsAccessor.add(fl.getOwnerType());
					funcInputsAccessor.addAll(funcInputs);

					bcoutputter.visitMethodInsn(INVOKESTATIC, fl.owner, name, getNormalMethodInvokationDesc(funcInputsAccessor, returnType));
				} else {
					String owner = fl.owner;
					if (ownerType instanceof NamedType){//bit of a hack to address this.myFuncCall()//where this is a local class
						NamedType nt = (NamedType)ownerType;
						ClassDef setCD = nt.getSetClassDef();
						if(null != setCD){
							if(setCD.isLocalClass){
								owner = nt.getBytecodeType();
								owner = owner.substring(1, owner.length()-1);
							}
						}
					}
					boolean isIface = false;
					if(fl.isRHSOfTraitSuperChainable != null) {
						invokeOpCode = INVOKEINTERFACE;
						name =fl.isRHSOfTraitSuperChainable.getB();
						owner = this.currentClassDefObj.peek().bcFullName();
						isIface = true;
					}
					else if(fl.isRHSOfTraitSuper) {//redirect super trait call to static method
						invokeOpCode = INVOKESTATIC;
						name += "$traitM";
						funcInputs = new ArrayList<Type>(funcInputs);
						funcInputs.add(0, ownerType);
						isIface = true;
					}else if(invokeOpCode == INVOKEINTERFACE) {
						isIface = true;
					}
					
					bcoutputter.visitMethodInsn(invokeOpCode, owner, name, getNormalMethodInvokationDesc(funcInputs, returnType), isIface);
				}
			}

		} else {
			throw new RuntimeException(String.format("%s found which is unexpected", funcLoc.getClass()));
		}

		if (!((Node) funcInvoke).getShouldBePresevedOnStack() && !(returnType instanceof PrimativeType && ((PrimativeType) returnType).type == PrimativeTypeEnum.VOID)) {
			// if void u dont need pop
			
			boolean dopop=true;
			
			boolean delOnUnusedRet = testgetShouldBeDeletedOnUsusedReturn(funcType, funcLoc);
			
			if(funcInvoke.waitForRefToReturnSomething) {
				if(delOnUnusedRet) {//dup so can be deleted
					bcoutputter.visitInsn(DUP);
				}
				
				bcoutputter.visitInsn(DUP);
				Label onNull = new Label();
				bcoutputter.visitJumpInsn(IFNULL, onNull);
				Label after =  new Label();
				bcoutputter.visitTypeInsn(CHECKCAST, "com/concurnas/bootstrap/runtime/ref/Ref");
				bcoutputter.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/ref/Ref", "waitUntilSet", "()V", true);
				bcoutputter.visitJumpInsn(GOTO, after);
				bcoutputter.visitLabel(onNull);
				bcoutputter.visitInsn(POP);
				bcoutputter.visitLabel(after);
				
				dopop=false;
				
			}
			
			if(delOnUnusedRet) {
				//if(returnType instanceof NamedType) {//if method is tagged with DeleteOnUnusedReturn then we trigger the below

				bcoutputter.visitInsn(DUP);
				Label onNull = new Label();
				bcoutputter.visitJumpInsn(IFNULL, onNull);
				Label after =  new Label();
				bcoutputter.visitTypeInsn(CHECKCAST, "com/concurnas/bootstrap/runtime/cps/CObject");
				bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/bootstrap/runtime/cps/CObject", "delete", "()V", false);
				bcoutputter.visitJumpInsn(GOTO, after);
				bcoutputter.visitLabel(onNull);
				bcoutputter.visitInsn(POP);
				bcoutputter.visitLabel(after);
				
				
				dopop=false;
				//}
			}
			
			if(dopop) {
				Utils.popFromStack(bcoutputter, returnType);
			}
		}else if(funcInvoke.refShouldBeDeletedOnUsusedReturn && testgetShouldBeDeletedOnUsusedReturn(funcType, funcLoc)) {
			//callDeleteMethodIfNotNull(true);
			
			bcoutputter.visitInsn(DUP);
			bcoutputter.visitTypeInsn(CHECKCAST, "com/concurnas/bootstrap/runtime/cps/CObject");
			bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/bootstrap/runtime/cps/CObject", "delete", "()V", false);
			
		}
		else if (null != returnType.getOrigonalGenericTypeUpperBound() && !returnType.equals(returnType.getOrigonalGenericTypeUpperBound())) {// if
			// if this is a generic method invokation, it will return the upperbound object instead of what we want so we must cast to what we want on the stack
			Type orig = returnType.getOrigonalGenericTypeUpperBound();
			
			if(TypeCheckUtils.typeRequiresLocalArrayConvertion(returnType)){
				Utils.createRefArray(bcoutputter, (NamedType)returnType);
			}
			String toType = returnType.getCheckCastType();
			Utils.doCheckCastConvertArrays(bcoutputter, orig, toType);
			
		} else if (returnType instanceof FuncType) {
			Utils.doCheckCastConvertArrays(bcoutputter, returnType, ((FuncType) returnType).getCheckCastType());
		}

		return returnType; //funcInvoke.getTaggedType();
	}
	
	/*private void callDeleteMethodIfNotNull(boolean keepOnStack) {
		mv.visitInsn(DUP);
		Label onNull = new Label();
		mv.visitJumpInsn(IFNULL, onNull);
		Label after =  new Label();
		mv.visitTypeInsn(CHECKCAST, "com/concurnas/bootstrap/runtime/cps/CObject");
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/bootstrap/runtime/cps/CObject", "delete", "()V", false);
		mv.visitJumpInsn(GOTO, after);
		mv.visitLabel(onNull);
		mv.visitInsn(POP);
		mv.visitLabel(after);
	}*/
	
	private boolean testgetShouldBeDeletedOnUsusedReturn(FuncType funcType, Location funcLoc) {
		FuncDef origin = funcType.origonatingFuncDef;
		
		
		if(funcType.retType.equals(ScopeAndTypeChecker.const_class_nt) || funcType.retType.equals(ScopeAndTypeChecker.const_object)) {
			return false;//doesnt extend CObject
		}

		ArrayList<Type> inputArgs = new ArrayList<Type>();
		if(origin != null) {
			for(FuncParam fp : origin.params.params) {
				inputArgs.add(fp.getTaggedType());
			}
		}
		
		
		NamedType supType = null;
		if(funcLoc instanceof FuncLocation) {
			FuncLocation fl = (FuncLocation)funcLoc;
			Type own = fl.getOwnerType();
			if(own instanceof NamedType) {
				supType = ((NamedType) own).getResolvedSuperTypeAsNamed();
			}
		}
		
		return getShouldBeDeletedOnUsusedReturn(inputArgs, supType, origin);
	}
	
	private boolean getShouldBeDeletedOnUsusedReturn(ArrayList<Type> inputArgs, NamedType superNT, FuncDef fd) {
		if(fd == null) {
			return false;
		}
		
		if(fd.getShouldBeDeletedOnUsusedReturn()) {
			return true;
		}else if(superNT != null) {
			HashSet<TypeAndLocation> choices = superNT.getFuncDef(0, 0, fd.getMethodName(), inputArgs, null, null);//just 1

			if(choices != null && !choices.isEmpty()) {
				Set<FuncType> fts = new HashSet<FuncType>();
				for(TypeAndLocation choice  : choices) {
					Type tt = choice.getType();
					if(tt instanceof FuncType) {
						fts.add((FuncType)tt);
					}
				}
				FuncType theOne = TypeCheckUtils.getMostSpecificFunctionForChoicesFT(this.errorRaisableSupressionFromSatc, this.errorRaisableSupressionFromSatc, fts, inputArgs, null, "", 0, 0, false, null, false).getA();
				if(theOne != null) {
					if(theOne instanceof FuncType) {
						return getShouldBeDeletedOnUsusedReturn(inputArgs, superNT.getResolvedSuperTypeAsNamed(), ((FuncType)theOne).origonatingFuncDef);
					}
				}
			
				
			}
		}
		return false;
	}
	
	
	private Pair<Integer, HashMap<Integer, Type>> defaultParamPrelude(ArrayList<Type> defaultArgs, List<?> itemsChecked, boolean isActorConstructor){
		return defaultParamPrelude(bcoutputter, this, defaultArgs, itemsChecked, -1, isActorConstructor);
	}
	
	public static Pair<Integer, HashMap<Integer, Type>> defaultParamPrelude(BytecodeOutputter mv, BytecodeGennerator bv, ArrayList<Type> defaultArgs, List<?> itemsChecked, int defaultParamObjSlot, boolean isActorConstructor){
		HashMap<Integer, Type> toAddDefaultParamUncreate = new HashMap<Integer, Type>();
		if(defaultArgs != null){
			int m=0;
			Type lastType=null;
			for(Type tt : defaultArgs){
				if(tt.equals(ScopeAndTypeChecker.const_defaultParamUncre)){
					toAddDefaultParamUncreate.put(m-1, lastType);
				}
				else{
					m++;
					lastType = tt;
				}
			}
			
			boolean makeThing = false;
			int n=isActorConstructor?1:0;
			for(Object o : itemsChecked){//create thing if any non null entries with default
				if(o != null && toAddDefaultParamUncreate.containsKey(n)){
					makeThing=true;
					break;
				}
				n++;
			}
			
			if(makeThing){
				String defaultParamObj = ScopeAndTypeChecker.const_defaultParamUncre.getCheckCastType();
				
				mv.visitMethodInsn(INVOKESTATIC, defaultParamObj, "getInstance", "()L"+defaultParamObj+";", false);
				//above more efficient, then we are not creating objects for the poop of it
				
				/*mv.visitTypeInsn(NEW, defaultParamObj);
				mv.visitInsn(DUP);
				mv.visitMethodInsn(INVOKESPECIAL, defaultParamObj, "<init>", "()V");*/
			
				if(-1 == defaultParamObjSlot){
					defaultParamObjSlot = bv.createNewLocalVar(bv.getTempVarName(), ScopeAndTypeChecker.const_defaultParamUncre, false);
				}
				mv.visitVarInsn(ASTORE, defaultParamObjSlot);
			}
		}
		
		return new Pair<Integer, HashMap<Integer, Type>>(defaultParamObjSlot, toAddDefaultParamUncreate);
	}

	private void processNullArgExpression( HashMap<Integer, Type> toAddDefaultParamUncreate, int n){
		processNullArgExpression(bcoutputter, toAddDefaultParamUncreate,  n);
	}
	
	public static void processNullArgExpression(BytecodeOutputter mv, HashMap<Integer, Type> toAddDefaultParamUncreate, int n){
		Type tt = toAddDefaultParamUncreate.get(n);
		
		if(tt instanceof PrimativeType && !tt.hasArrayLevels()){
			switch(((PrimativeType) tt).type){
				case LONG:   mv.visitInsn(LCONST_1); break;
				case FLOAT:  mv.visitInsn(FCONST_0); break;
				case DOUBLE: mv.visitInsn(DCONST_0); break;
				default : mv.visitInsn(ICONST_0);
			}
		}
		else{
			mv.visitInsn(ACONST_NULL);
		}
		
		mv.visitInsn(ACONST_NULL);
	}
	
	private void acceptFuncInvokeArgs(FuncInvoke funcInvoke, FuncType funcType, boolean isLambda, ArrayList<Type> defaultArgs){
		//HMMM
		List<Expression> args = funcInvoke.args.getArgumentsWNPs();
		int sz = args.size();
		ArrayList<Type> inputs = funcType.getInputs();
		
		Pair<Integer, HashMap<Integer, Type>> defpp = defaultParamPrelude(defaultArgs, args, false);
		
		int defaultParamObjSlot = defpp.getA();
		HashMap<Integer, Type> toAddDefaultParamUncreate = defpp.getB();
		
		int argOffset;
		if(funcType.origonatingFuncDef != null && funcType.origonatingFuncDef.extFunOn != null){
			argOffset=1;
		}else{
			argOffset=0;
		}
		
		List<Boolean> assertNonNull = funcInvoke.args.assertNonNull;
		
		for (int n = 0; n < sz; n++) {
			Expression e = args.get(n);
			boolean isDefault = toAddDefaultParamUncreate.containsKey(n);
			if(e == null){//replace with default null for type
				processNullArgExpression(toAddDefaultParamUncreate, n);
			}
			else{
				if (bcoutputter != null) {//having to do this explicitly is a bit nasty
					bcoutputter.visitNewLine(e.getLine());
				}
				
				Type got = (Type) e.accept(this);
				
				if(assertNonNull != null) {
					if(assertNonNull.get(n)) {
						//arg null throw exception
						throwIfNull(true);
					}
				}
				
				/*boolean isShared = false;
				if(e instanceof RefName) {
					isShared = ((RefName)e).resolvesTo.getLocation().isShared();
				}*/
				
				Type expect = inputs.get(n+argOffset);
				if (isLambda) {
					expect = TypeCheckUtils.boxTypeIfPrimative(expect, false);
				}
				if (!got.equals(expect)  || TypeCheckUtils.typeRequiresLocalArrayConvertion(expect) ) {
					if(!TypeCheckUtils.hasRefLevels(expect) && !expect.equals(Const_Object)  ){
						TypeCheckUtils.unlockAllNestedRefs(got);
					}
					
					Utils.applyCastImplicit(bcoutputter, got, expect, this);
				}
				if(isDefault){
					bcoutputter.visitVarInsn(ALOAD, defaultParamObjSlot);
				}
				
				/*if(tempTrackerSlot > -1) {
					if (!TypeCheckUtils.shouldNotBeCopied(got, isShared)) {//TODO:isPrimativeOrString add other obvious immutable primatives
						this.doCopy(expect, null, tempTrackerSlot, null, null);
					}
				}*/
			}
		}
	}

	@Override
	public Object visit(FuncRefInvoke funcRefInvoke) {
		// make it into a FuncInvoke and return that:

		Type resolvesTo = (Type)funcRefInvoke.funcRef.accept(this);
		FuncType naturalAcceptance;
		if(resolvesTo instanceof NamedType){
			naturalAcceptance = (FuncType)TypeCheckUtils.convertTypeToFuncType(Utils.unref(bcoutputter, resolvesTo, this));
		}else{
			naturalAcceptance = (FuncType)resolvesTo;
		}
		
		//unref if approperiate
		
		 
		Type got = funcRefInvoke.resolvedInputType;
		if (TypeCheckUtils.hasRefLevelsAndIsLocked(got)) {
			TypeCheckUtils.unlockAllNestedRefs(got);
		}

		FuncType gotSoFar = (FuncType) Utils.unref(bcoutputter, got, this);

		FuncInvoke fakeFuncRef = new FuncInvoke(funcRefInvoke.getLine(), funcRefInvoke.getColumn(), "apply", funcRefInvoke.getArgs());
		Location loc = naturalAcceptance.getLambdaDetails().getLocation();
		loc.setLambda(true);
		
		Type lamType = naturalAcceptance.getLambdaDetails().getType();
		
		if(naturalAcceptance instanceof FuncType && ((FuncType)naturalAcceptance).isClassRefType && lamType instanceof FuncType){
			String newOwner = ((FuncType)lamType).retType.getBytecodeType();
			newOwner = newOwner.substring(1, newOwner.length()-1);
			loc.setLambdaOwner(FuncType.classRefIfacePrefix + newOwner + FuncType.classRefIfacePostfix);
		}
		
		TypeAndLocation tal = new TypeAndLocation(gotSoFar, loc);
		fakeFuncRef.resolvedFuncTypeAndLocation = tal;

		fakeFuncRef.setPreceededByThis(true);
		((Node) fakeFuncRef).setShouldBePresevedOnStack(funcRefInvoke.getShouldBePresevedOnStack());

		return fakeFuncRef.accept(this);
	}

	// private ForBlockPreamble nextblockPreamble = null;
	public boolean isInsideDefaultConstuctorFieldInit;

	@Override
	public Object visit(ImportStar importStar) {
		return null; // null is ok
	}

	@Override
	public Object visit(ImportImport importImport) {
		return null;// null is ok
	}

	@Override
	public Object visit(ImportFrom importFrom) {
		return null;// null is ok
	}

	@Override
	public Object visit(ImportAsName importAsName) {
		return null;// null is ok
	}

	@Override
	public Object visit(DottedNameList dottedNameList) {
		return null;// null is ok
	}

	@Override
	public Object visit(DottedAsName dottedAsName) {
		return null; // null is ok
	}

	@Override
	public Object visit(FuncInvokeArgs funcInvokeArgs) {
		return null; // null is ok
	}

	public Object processThisOrSuperTypeConstrInvoke(SuperOrThisConstructorInvoke superConstructorInvoke) {

		FuncType resolvesTo = superConstructorInvoke.resolvedFuncType;

		bcoutputter.visitVarInsn(ALOAD, 0);

		boolean isNestedClass = this.currentClassDefObj.peek().getParentNestor() != null;

		boolean addParNestorInvoke = isNestedClass && null != superConstructorInvoke.parNestorToAdd;

		if (addParNestorInvoke) {
			bcoutputter.visitVarInsn(ALOAD, 1);
		}
		
		if(superConstructorInvoke.isEnumconstru){
			//add extra stuff
			bcoutputter.visitVarInsn(ALOAD, 1);
			bcoutputter.visitVarInsn(ILOAD, 2);
		}

		ArrayList<Type> args = resolvesTo.defaultFuncArgs != null?resolvesTo.defaultFuncArgs: resolvesTo.getInputs();
		if(args != null) {
			args = new ArrayList<Type>(args);
		}
		
		ArrayList<Type> argsnorm = resolvesTo.getInputs();
		if(argsnorm != null) {
			argsnorm = new ArrayList<Type>(argsnorm);
		}
		
		List<Expression> argz = superConstructorInvoke.args.getArgumentsWNPs();
		if(argz != null) {
			argz = new ArrayList<Expression>(argz);
		}
		
		Pair<Integer, HashMap<Integer, Type>> defpp = defaultParamPrelude(resolvesTo.defaultFuncArgs, argz, TypeCheckUtils.isActor(this.errorRaisableSupressionFromSatc, new NamedType(resolvesTo.origin)));
		int defaultParamObjSlot = defpp.getA();
		HashMap<Integer, Type> toAddDefaultParamUncreate = defpp.getB();
		
		for (int n = 0; n < argz.size(); n++) {
			Expression expr = argz.get(n);
			boolean isDefault = toAddDefaultParamUncreate.containsKey(n);
			if(null == expr){
				processNullArgExpression(toAddDefaultParamUncreate, n);
			}else{
				Type got = (Type) expr.accept(this);
				Type expect = argsnorm.get(n);
				if (got != expect) {
					Utils.applyCastImplicit(bcoutputter, got, expect, this);
				}

				if(isDefault){
					bcoutputter.visitVarInsn(ALOAD, defaultParamObjSlot);
				}
			}
		}

		if(superConstructorInvoke.isEnumconstru){
			if(superConstructorInvoke.isEnumconstruSubClass){//add extra stuff
				bcoutputter.visitInsn(ACONST_NULL);//unsued?
			}
		}
		
		if (addParNestorInvoke) {
			args.add(0, superConstructorInvoke.parNestorToAdd);
		}

		if(superConstructorInvoke.isEnumconstru){
			
			args.add(0, ScopeAndTypeChecker.const_int);
			args.add(0, ScopeAndTypeChecker.const_string);
			
			if(superConstructorInvoke.isEnumconstruSubClass){
				args.add(new NamedType(this.currentClassDefObj.peek().getParentNestor()));
			}
		}

		String methodDesc = getNormalMethodInvokationDesc(args, resolvesTo.retType);

		ClassDef cls;
		if (superConstructorInvoke instanceof SuperConstructorInvoke) {
			cls = this.currentClassDefObj.peek().getSuperclass();
		} else {
			cls = this.currentClassDefObj.peek();
		}

		String supName = cls.bcFullName();

		bcoutputter.visitMethodInsn(INVOKESPECIAL, supName, "<init>", methodDesc);

		return null;
	}

	@Override
	public Object visit(SuperConstructorInvoke superConstructorInvoke) {
		Object ret = processThisOrSuperTypeConstrInvoke(superConstructorInvoke);
		
		if(!this.currentClassDefObj.isEmpty()) {
			ClassDef cd = this.currentClassDefObj.peek();
			if(!cd.isTrait /*&& cd.traitVarsNeedingFieldDef != null*/ && cd.linearizedTraitsInitCalls != null) {
				visitLinearlizedTraitInits(bcoutputter, cd.linearizedTraitsInitCalls);
			}
		}
		
		return ret;
	}
	
	public static void visitLinearlizedTraitInits(BytecodeOutputter bcoutputter, List<NamedType> linearizedTraitsInitCallsx) {
		//TODO: add init call to super call for concrete class
		List<NamedType> linearizedTraitsInitCalls = new ArrayList<NamedType>(linearizedTraitsInitCallsx);
		Collections.reverse(linearizedTraitsInitCalls);
		for(NamedType nt : linearizedTraitsInitCalls) {
			bcoutputter.visitVarInsn(ALOAD, 0);
			
			//example2/MyTrait1.$init$:(Lexample2/MyTrait1;)V
			ClassDef cdx = nt.getSetClassDef();
			String bcName = cdx.bcFullName();
			bcoutputter.visitMethodInsn(INVOKESTATIC, bcName, "$init$traitM", "(L"+bcName+";)V", true);
		}
	}
	

	@Override
	public Object visit(ThisConstructorInvoke thisConstructorInvoke) {
		/*
		 * LINENUMBER 24 L0 ALOAD 0: this INVOKESPECIAL Child$BBB.<init>() :
		 * void
		 */
		return processThisOrSuperTypeConstrInvoke(thisConstructorInvoke);
	}

	@Override
	public Object visit(PrimativeType primativeType) {
		return primativeType;
	}

	@Override
	public Object visit(NamedType namedType) {
		return namedType;
	}

	private void assertRefType(NamedType fromType, Type castToType) {
		/*
		 * JPT: this next code block is a bit more enviromentally friendly
		 * perhaps... ArrayList<String> types = new ArrayList<String>();
		 * types.add( "com/concurnas/runtime/ref/Local" );
		 * Utils.createTypeArray(mv, fromType, types);
		 * 
		 * mv.visitMethodInsn(INVOKESTATIC,
		 * "com/concurnas/runtime/InstanceofGeneric",
		 * "assertisGenericInstnaceof",
		 * "([Ljava/lang/Class;[Ljava/lang/Class;)Z");
		 * mv.visitTypeInsn(CHECKCAST, castToType.getCheckCastType());
		 */
		bcoutputter.visitInsn(DUP);
		Utils.createTypeArrayAddInitialRef(bcoutputter, (NamedType) castToType);
		bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/runtime/InstanceofGeneric", "assertGenericInstnaceof", "(Ljava/lang/Object;[Ljava/lang/Class;)V");
		bcoutputter.visitTypeInsn(CHECKCAST, castToType.getCheckCastType());
	}

	private Type doCast(Type fromType, Type castToType) {
		if (!castToType.equals(fromType)) {

			if (!Utils.truePrimative(castToType)) {
				fromType = Utils.box(bcoutputter, fromType);
			}

			boolean toTypeIsRef = TypeCheckUtils.hasArrayRefLevels(castToType);
			if (toTypeIsRef && castToType.hasArrayLevels()) {
				// check components
				bcoutputter.visitInsn(DUP);
				bcoutputter.visitTypeInsn(CHECKCAST, "com/concurnas/bootstrap/runtime/ref/LocalArray");

				Label onFail = new Label();
				Label onEnd = new Label();

				bcoutputter.visitFieldInsn(GETFIELD, "com/concurnas/bootstrap/runtime/ref/LocalArray", "type", "[Ljava/lang/Class;");

				ArrayList<String> types = new ArrayList<String>();
				// types.add( "com/concurnas/runtime/ref/Local" );
				
				NamedType varType = (NamedType) castToType;
				int upTo = varType.getArrayLevels();
				int n=1;
				while(n++ < upTo){
					types.add( "com/concurnas/bootstrap/runtime/ref/LocalArray" );
				}
				
				String prim = varType.getSetClassDef().javaClassName();
				prim = prim.substring(1, prim.length() -1);
				
				types.add( prim );
				
				Utils.createTypeArray(bcoutputter, varType, types, null, null);
				bcoutputter.visitInsn(ICONST_1);
				bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/runtime/InstanceofGeneric", "isGenericInstnaceof", "([Ljava/lang/Class;[Ljava/lang/Class;Z)Z");
				bcoutputter.visitJumpInsn(IFEQ, onFail);
				bcoutputter.visitJumpInsn(GOTO, onEnd);

				bcoutputter.visitLabel(onFail);

				bcoutputter.visitTypeInsn(NEW, "java/lang/ClassCastException");
				bcoutputter.visitInsn(DUP);
				bcoutputter.visitLdcInsn("Generic types of ref array do not match");
				bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/lang/ClassCastException", "<init>", "(Ljava/lang/String;)V");
				bcoutputter.visitInsn(ATHROW);

				bcoutputter.visitLabel(onEnd);
				bcoutputter.visitTypeInsn(CHECKCAST, castToType.getCheckCastType());
			} else if (toTypeIsRef) {

				int toRefLevels = TypeCheckUtils.getRefLevels(castToType);
				int fromRefLevels = TypeCheckUtils.getRefLevels(fromType);

				if (fromRefLevels > 0 && toRefLevels == fromRefLevels) {
					/*
					 * if x known statically to be ref and matching subtype,
					 * dont do anything if statcally known ref and not matching
					 * subtype - should have blown up in sematic checker WHEN
					 * here x is it ref already - check subtypes...
					 */
					assertRefType((NamedType) fromType, castToType);
					// how about ::?
				} else {
					/*
					 * x as Ref: if x NOT REF, ensure that subtype is castable
					 * to that thing referenced , now make into ref: chcek is
					 * stmts
					 * 
					 * 
					 * check if ref already if is, check subtype - err if not if
					 * not ref ref it up
					 */

					// if it can be wrapped up in a ref, then wrap it up else...

					if (fromRefLevels > 0 || TypeCheckUtils.isObject(fromType)) {
						// a Object
						// a as int: //a is either a int (ref it up), or it's a
						// int: already!
						// or its an int::: in which case it needs to be
						// eextracted out

						/*
						 * First check if thing matches 1:1, then skip if does
						 * If component type matches, then wrap up with +1 layer
						 * 
						 * else, go into assert code and deliberaly blow up (not
						 * optimal but easier than farting about with
						 * exceptions) TODO: fixme
						 */

						Label onEnd = new Label();

						bcoutputter.visitInsn(DUP);
						bcoutputter.visitInsn(DUP);

						int tempSlot = this.createNewLocalVar(this.getTempVarName(), Const_Object, false);
						bcoutputter.visitVarInsn(ASTORE, tempSlot);

						// 1. check for 1:1 match
						Utils.createTypeArrayAddInitialRef(bcoutputter, castToType);
						// mv.visitInsn(DUP);
						bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/runtime/InstanceofGeneric", "isGenericInstnaceof", "(Ljava/lang/Object;[Ljava/lang/Class;)Z");
						bcoutputter.visitJumpInsn(IFNE, onEnd);// matches component type

						// defo wrong!!!! with the dups etc

						// TODO: super check -
						/*
						 * if check component type [could be obj] shift levels
						 * up or down as approperiate else fail with exception
						 */

						Utils.createTypeArrayAddInitialRef(bcoutputter, castToType);
						bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/runtime/GenericRefCast", "genericRefCast", "(Ljava/lang/Object;[Ljava/lang/Class;)Ljava/lang/Object;");

						bcoutputter.visitLabel(onEnd);
						bcoutputter.visitTypeInsn(CHECKCAST, castToType.getCheckCastType());
					} else {
						// ref it up! easy from nothing
						Utils.applyCastExplicit(bcoutputter, fromType, castToType, this);

					}
				}
			}
			else if(castToType instanceof NamedType && (TypeCheckUtils.isTypedActorExactly(this.errorRaisableSupressionFromSatc, (NamedType)castToType) || ( TypeCheckUtils.isReifiedType((NamedType)castToType) && ((NamedType)castToType).hasGenTypes() ) )){
				assertRefType((NamedType) fromType, castToType);//multi-use
			}
			else {
				/*
				 * if(fromTypeIsRef){ NamedType asNamed =
				 * ((NamedType)fromType).copyTypeSpecific();
				 * asNamed.setLockedAsRef(false);
				 * 
				 * fromType = asNamed; }
				 */

				Utils.applyCastExplicit(bcoutputter, fromType, castToType, this);
			}
		}
		return castToType;
	}

	@Override
	public Object visit(CastExpression castExpression) {
		if(resolvedViaFoldedConstant(castExpression.getFoldedConstant(), castExpression.getTaggedType())){
			return castExpression.getTaggedType(); 
		}
		
		Type fromType = (Type) castExpression.o.accept(this);

		return doCast(fromType, castExpression.t.getTaggedType());
	}

	private void checkRefType(Type castToType) {
		Utils.createTypeArrayAddInitialRef(bcoutputter, (NamedType) castToType);
		bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/runtime/InstanceofGeneric", "isGenericInstnaceof", "(Ljava/lang/Object;[Ljava/lang/Class;)Z");
	}

	private Type dealWithInstaceofNoAcc(ArrayList<Type> typesx, Type eType, boolean inverted) {
		String instao;
		
		boolean moreThanOne = typesx.size()!=1;
		int tempSlothead = -1;
		if(moreThanOne){
			tempSlothead = this.createNewLocalVar(this.getTempVarName(), ScopeAndTypeChecker.const_object, false);
			bcoutputter.visitInsn(DUP);
			bcoutputter.visitVarInsn(ASTORE, tempSlothead);
		}
		Label onDone = new Label();
		
		for(int na=0; na < typesx.size(); na++){ 
			/*if(ttt instanceof NamedType && TypeCheckUtils.isTypedActor(this.errorRaisableSupressionFromSatc, (NamedType)ttt)){
				//if the thing is already an actor, i.e. a is actor SomeActor
				Type got = TypeCheckUtils.extractRootActor(ttt).getGenTypes().get(0);
				
				if(got instanceof NamedType && TypeCheckUtils.isActorOrTypedActor(errorRaisableSupressionFromSatc, (NamedType)got)){
					ttt = got;
				}
			}*/
			
			if(na != 0){
				bcoutputter.visitVarInsn(ALOAD, tempSlothead);
			}
			
			Type ttt = typesx.get(na);
			
			
			if (ttt instanceof FuncType) {
				instao = ((FuncType) ttt).getPoshObjectStyleName().getSetClassDef().bcFullName();
			} else if (ttt instanceof PrimativeType) {
				instao = ttt.getCheckCastType();// "com/concurnas/bootstrap/lang/Lambda";
			} else {
				NamedType nt = (NamedType) ttt;
				instao = nt.getCheckCastType();
			}

			boolean isCheckingRef = ttt instanceof NamedType && ((NamedType) ttt).getIsRef();

			boolean fromTypeIsRef = eType instanceof NamedType && ((NamedType) eType).getIsRef();

			if (isCheckingRef && ttt.hasArrayLevels()) {
				// arry of refs, extract its type map for comparison
				Label onFail = new Label();
				Label onEnd = new Label();

				bcoutputter.visitInsn(DUP);

				int tempSlot = this.createNewLocalVar(this.getTempVarName(), ttt, false);
				bcoutputter.visitVarInsn(ASTORE, tempSlot);

				bcoutputter.visitJumpInsn(IFNULL, onFail);
				bcoutputter.visitVarInsn(ALOAD, tempSlot);
				bcoutputter.visitTypeInsn(INSTANCEOF, "com/concurnas/bootstrap/runtime/ref/LocalArray");
				bcoutputter.visitJumpInsn(IFEQ, onFail);
				bcoutputter.visitVarInsn(ALOAD, tempSlot);
				bcoutputter.visitTypeInsn(CHECKCAST, "com/concurnas/bootstrap/runtime/ref/LocalArray");
				bcoutputter.visitFieldInsn(GETFIELD, "com/concurnas/bootstrap/runtime/ref/LocalArray", "ar", "[Ljava/lang/Object;");
				bcoutputter.visitJumpInsn(IFNULL, onFail);

				bcoutputter.visitVarInsn(ALOAD, tempSlot);
				bcoutputter.visitTypeInsn(CHECKCAST, "com/concurnas/bootstrap/runtime/ref/LocalArray");
				bcoutputter.visitFieldInsn(GETFIELD, "com/concurnas/bootstrap/runtime/ref/LocalArray", "type", "[Ljava/lang/Class;");

				ArrayList<String> types = new ArrayList<String>();
				// types.add( "com/concurnas/runtime/ref/Local" );
				
				NamedType varType = (NamedType) ttt;
				int upTo = varType.getArrayLevels();
				int n=1;
				while(n++ < upTo){
					types.add( "com/concurnas/bootstrap/runtime/ref/LocalArray" );
				}
				
				String prim = varType.getSetClassDef().javaClassName();
				prim = prim.substring(1, prim.length() -1);
				
				types.add( prim );
				
				
				Utils.createTypeArray(bcoutputter, varType, types, null, null);
				bcoutputter.visitInsn(ICONST_1);
				bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/runtime/InstanceofGeneric", "isGenericInstnaceof", "([Ljava/lang/Class;[Ljava/lang/Class;Z)Z");
				bcoutputter.visitJumpInsn(IFEQ, onFail);

				bcoutputter.visitInsn(ICONST_1);
				bcoutputter.visitJumpInsn(GOTO, onEnd);

				bcoutputter.visitLabel(onFail);

				bcoutputter.visitInsn(ICONST_0);

				bcoutputter.visitLabel(onEnd);
			} else if (isCheckingRef) {
				if (fromTypeIsRef) {
					/*
					 * if x known statically to be ref and matching subtype, dont do
					 * anything if statcally known ref and not matching subtype -
					 * should have blown up in sematic checker WHEN here x is it ref
					 * already - check subtypes...
					 */

					checkRefType(ttt);
					// boolean on top

					// how about ::?
				} else {
					/*
					 * x as Ref: if x NOT REF, ensure that subtype is castable to
					 * that thing referenced , now make into ref: chcek is stmts
					 * 
					 * check if ref already if is, check subtype - err if not if not
					 * ref ref it up
					 * 
					 * changed to make sure logic is 1:1
					 */
					/*
					 * mv.visitInsn(DUP);
					 * 
					 * Label notRefAlready = new Label(); Label onEnd = new Label();
					 * 
					 * int tempSlot = this.createNewLocalVar(this.getTempVarName(),
					 * Const_Object, false); mv.visitVarInsn(ASTORE, tempSlot);
					 * 
					 * /*NamedType refNObject = Const_Object; for(int n=0; n <
					 * TypeCheckUtils.getRefLevels(ttt); n++){ refNObject = new
					 * NamedType(0,0, refNObject); }
					 * Utils.createTypeArrayAddInitialRef(mv, refNObject);*
					 * Utils.createTypeArrayAddInitialRef(mv, ttt);
					 * mv.visitMethodInsn(INVOKESTATIC,
					 * "com/concurnas/runtime/InstanceofGeneric",
					 * "isGenericInstnaceof",
					 * "(Ljava/lang/Object;[Ljava/lang/Class;)Z");
					 * 
					 * mv.visitJumpInsn(IFEQ, notRefAlready); //already ref check
					 * compatible... mv.visitVarInsn(ALOAD, tempSlot);
					 * checkRefType(ttt); //if u got here then ur compaible
					 * mv.visitJumpInsn(GOTO, onEnd);
					 * 
					 * mv.visitLabel(notRefAlready); //now check that it can be made
					 * to be a ref like that.... mv.visitVarInsn(ALOAD, tempSlot);
					 * mv.visitTypeInsn(INSTANCEOF,
					 * ((NamedType)ttt).getGenericTypeElements
					 * ().get(0).getCheckCastType() );//TODO: BAD this makes no
					 * reference to refs, consider rewriting exception co
					 * 
					 * mv.visitLabel(onEnd);
					 */

					Utils.createTypeArrayAddInitialRef(bcoutputter, ttt);
					bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/runtime/InstanceofGeneric", "isGenericInstnaceof", "(Ljava/lang/Object;[Ljava/lang/Class;)Z");

				}
				/*
				 * NamedType asNamed = ((NamedType)ttt);
				 * 
				 * Label secondAttempt = new Label(); Label onFail = new Label();
				 * Label onEnd = new Label();
				 * 
				 * mv.visitInsn(DUP); int tempSlot =
				 * this.createNewLocalVar(this.getTempVarName(), eType, eType,
				 * false); mv.visitVarInsn(ASTORE, tempSlot);
				 * 
				 * mv.visitTypeInsn(INSTANCEOF, instao );
				 * 
				 * mv.visitJumpInsn(IFEQ, secondAttempt); Utils.applyLoad(mv, eType,
				 * tempSlot); mv.visitTypeInsn(CHECKCAST, instao);
				 * mv.visitFieldInsn(GETFIELD, "com/concurnas/runtime/ref/Local",
				 * "type", "[Ljava/lang/Class;");
				 * mv.visitLdcInsn(org.objectweb.asm.Type.getType("L" +
				 * asNamed.getGenTypes().get(0).getCheckCastType() + ";"));
				 * mv.visitMethodInsn(INVOKESTATIC,
				 * "com/concurnas/runtime/InstanceofGeneric", "isGenericInstnaceof",
				 * "([Ljava/lang/Class;Ljava/lang/Class;)Z"); mv.visitJumpInsn(IFEQ,
				 * onFail); //true, its a generic instance mv.visitInsn(ICONST_1);
				 * 
				 * mv.visitJumpInsn(GOTO, onEnd); mv.visitLabel(secondAttempt);
				 * //new Integer(7) is Integer: <-yes! //check to see if it's of
				 * compoent type
				 * 
				 * 
				 * Utils.applyLoad(mv, eType, tempSlot); mv.visitTypeInsn(CHECKCAST,
				 * "java/lang/Object");
				 * mv.visitLdcInsn(org.objectweb.asm.Type.getType("L" +
				 * asNamed.getGenTypes().get(0).getCheckCastType() + ";"));
				 * mv.visitMethodInsn(INVOKESTATIC,
				 * "com/concurnas/runtime/InstanceofGeneric", "isGenericInstnaceof",
				 * "(Ljava/lang/Object;Ljava/lang/Class;)Z");
				 * 
				 * mv.visitJumpInsn(IFEQ, onFail); //true, its a generic instance
				 * mv.visitInsn(ICONST_1); mv.visitJumpInsn(GOTO, onEnd);
				 * 
				 * mv.visitLabel(onFail); mv.visitInsn(ICONST_0);
				 * 
				 * mv.visitLabel(onEnd);
				 */

			} else if (eType instanceof NamedType && ((NamedType) eType).getIsRef()) {// oh
																						// but
																						// eType
																						// is
																						// ref...

				NamedType asNamed = (NamedType) eType;
				asNamed = asNamed.copyTypeSpecific();
				asNamed.setLockedAsRef(false);

				Utils.unref(bcoutputter, asNamed, this);
				bcoutputter.visitTypeInsn(INSTANCEOF, instao);
			} else {
				
				if(ttt instanceof NamedType && (TypeCheckUtils.isTypedActorExactly(this.errorRaisableSupressionFromSatc, (NamedType)ttt) || ( TypeCheckUtils.isReifiedType((NamedType)ttt) && ((NamedType)ttt).hasGenTypes() ) )){
					Utils.createTypeArrayAddInitialRef(bcoutputter, ttt);
					bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/runtime/InstanceofGeneric", "isGenericInstnaceof", "(Ljava/lang/Object;[Ljava/lang/Class;)Z");
					//mv.visitTypeInsn(INSTANCEOF, instao);
				}
				else if(ttt instanceof NamedType && TypeCheckUtils.isTypedOrUntypedActor(this.errorRaisableSupressionFromSatc, (NamedType)ttt) ){
					
					if(ttt.equals(ScopeAndTypeChecker.const_actor)){
						Utils.createTypeArray( bcoutputter,  ttt,  new ArrayList<String>(), null, null);
					}else{
						Utils.createTypeArrayAddInitialRef(bcoutputter, ttt);
					}
					
					bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/runtime/InstanceofGeneric", "isGenericInstnaceof", "(Ljava/lang/Object;[Ljava/lang/Class;)Z");
					//mv.visitTypeInsn(INSTANCEOF, instao);
				}
				else{
					bcoutputter.visitTypeInsn(INSTANCEOF, instao);
				}
			}
			
			if(moreThanOne){
				bcoutputter.visitJumpInsn(IFNE, onDone);
			}
		}
		
		if(moreThanOne){
			Label localcarryOn = new Label();
			bcoutputter.visitInsn(ICONST_0);
			bcoutputter.visitJumpInsn(GOTO, localcarryOn);
			bcoutputter.visitLabel(onDone);
			bcoutputter.visitInsn(ICONST_1);
			this.bcoutputter.pushNextLabel(localcarryOn);
		}
		
		if (inverted) {// the system flipped it
			Label localcarryOn = new Label();
			Label onFail = new Label();
			bcoutputter.visitJumpInsn(IFEQ, onFail);
			bcoutputter.visitInsn(ICONST_0);
			bcoutputter.visitJumpInsn(GOTO, localcarryOn);
			bcoutputter.visitLabel(onFail);
			bcoutputter.visitInsn(ICONST_1);
			this.bcoutputter.pushNextLabel(localcarryOn);
		}

		return Const_PRIM_BOOl;
	}

	@Override
	public Object visit(Is instanceOf) {
		Type eType = (Type) instanceOf.e1.accept(this);
		ArrayList<Type> types = new ArrayList<Type>(instanceOf.typees.size()); 
		
		instanceOf.typees.forEach(a -> types.add(a.getTaggedType()));
		
		return dealWithInstaceofNoAcc(types, eType, instanceOf.inverted);
	}

	@Override
	public Object visit(AssertStatement assertStatement) {
		Type tt = (Type)assertStatement.e.accept(this);
		Label onFail = new Label();
		convertToBoolean(tt, onFail);
		
		Label cont = new Label();
		bcoutputter.visitJumpInsn(IFNE, cont);
		bcoutputter.visitLabel(onFail);
		bcoutputter.visitTypeInsn(NEW, "java/lang/AssertionError");
		bcoutputter.visitInsn(DUP);
		if(null != assertStatement.message){
			assertStatement.message.accept(this);
			bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/lang/AssertionError", "<init>", "(Ljava/lang/Object;)V");
		}
		else if(null != assertStatement.messageFromExpr){
			bcoutputter.visitLdcInsn(assertStatement.messageFromExpr);
			bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/lang/AssertionError", "<init>", "(Ljava/lang/Object;)V");
		}
		else{
			bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/lang/AssertionError", "<init>", "()V");
		}
		bcoutputter.visitInsn(ATHROW);
		bcoutputter.visitLabel(cont);
		this.bcoutputter.pushNextLabel(cont);
		return null; // null is ok, cant do: d = assert o //etc
	}

	@Override
	public Object visit(ThrowStatement throwStatement) {
		Type what = (Type)throwStatement.thingTothrow.accept(this);
		Utils.unref(bcoutputter, what, this);
		bcoutputter.visitInsn(ATHROW);
		return null; // null is ok
	}

	@Override
	public Object visit(CatchBlocks catchBlocks) {
		return null;// null is ok
	}

	private Stack<Label> onReturnsInDefoFinallyRetBlock = new Stack<Label>();;

	private void setEntryLabelOfBlock(Block blk, Label labelOnEntry) {
		LineHolder lh = blk.getFirstLogical();
		if (null != lh) {
			// System.err.println("copyAndTagFirstLAbel as: " + labelOnEntry);
			lh.l.setLabelOnEntry(labelOnEntry);
		}
	}

	@Override
	public Object visit(GenericType genericType) {
		return null;// null is ok
	}

	private Stack<Label> breakGotoLabelOverride = new Stack<Label>();

	@Override
	public Object visit(TryCatch tryCatch) {
		if (tryCatch.astRepoint != null) {
			return tryCatch.astRepoint.accept(this);
		}

		
		if (tryCatch.gethasDefoReturnedConventionally()) {
			tryCatch.setShouldBePresevedOnStack(false);
		}

		boolean hasFinal = tryCatch.hasFinal();
		Label carryOn = tryCatch.getLabelAfterCode();
		Label startOfTheFinalyBlock = tryCatch.startOfTheFinalyBlock;

		boolean lastCatchDefRet = false;
		boolean lastCatchThrowExcepLastInstr = false;

		// ensure that all contained breaks in try catch's map to the finally
		// block:
		boolean mapTheNestedBreaks = hasFinal && tryCatch.finalBlock.hasDefoThrownException;

		if (mapTheNestedBreaks) {
			breakGotoLabelOverride.push(startOfTheFinalyBlock);
		}

		//boolean returnsSomething = tryCatch.getShouldBePresevedOnStack();
		// int presistResultOnStack = -1;
		Type expected = tryCatch.getTaggedType();

		/*
		 * if(tryCatch.getShouldBePresevedOnStack()){ String tempName =
		 * this.getTempVarName(); presistResultOnStack =
		 * this.createNewLocalVar(tempName, expected, false); }
		 */

		// save the stack state
		Stack<Value> currStack = this.bcoutputter.mv.currentFrame.stack;
		ArrayList<Pair<Type, Integer>> preStackState = new ArrayList<Pair<Type, Integer>>();
		while (!currStack.isEmpty()) {// &&
										// tryCatch.getShouldBePresevedOnStack()){//only
										// consider for cases where we expect a
										// return variable
			Value v = currStack.peek();// peek here not pop because the store
										// operation below which gets
										// interpreted will pop it for us
			Type storeMe = Utils.getTypeFromDesc(v.getTypeDesc());

			int tempSlot = this.createNewLocalVar(this.getTempVarName(), storeMe, false);
			Utils.applyStore(bcoutputter, storeMe, tempSlot);

			preStackState.add(new Pair<Type, Integer>(storeMe, tempSlot));
		}

		Label restoreStackLabel = null;
		
		//String storeReturnInVar = null;//for later reconstruction
		
		if(!preStackState.isEmpty()){
			
			/*if(tryCatch.getShouldBePresevedOnStack()){
				storeReturnInVar = this.getTempVarName();
				createNewLocalVar(storeReturnInVar, expected, false);
			}
			*/
			Label afterCode = tryCatch.getLabelAfterCode();
			if(null != afterCode){
				restoreStackLabel = new Label();
				bcoutputter.labelOverride.put(afterCode, restoreStackLabel);
			}
		}
		
		
		if (!tryCatch.blockToTry.isEmpty()) {
			if (hasFinal && tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()) {
				onReturnsInDefoFinallyRetBlock.push(tryCatch.startOfTheFinalyBlock);
			}

			Type bType = (Type) tryCatch.blockToTry.accept(this);

			if (tryCatch.getShouldBePresevedOnStack()) {
				if (!expected.equals(bType)) {
					Utils.applyCastImplicit(bcoutputter, bType, expected, this);
				}
				// Utils.applyStore(mv, expected, presistResultOnStack);
			}
			
			/*if(null != storeReturnInVar && tryCatch.blockToTry.getShouldBePresevedOnStack()){
				this.storeLocalVaraible(storeReturnInVar, null, expected, expected);
			}*/
			

			boolean wasLastBreakOrCont = this.lastStatementTouched instanceof BreakStatement || this.lastStatementTouched instanceof ContinueStatement;
			if ((!tryCatch.blockToTry.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch() || tryCatch.getShouldBePresevedOnStack()) && null != tryCatch.LabelPreGOTOOnBlockNonDefoRet && !wasLastBreakOrCont) {
				// || tryCatch.getShouldBePresevedOnStack()) means that this
				// works ok: try{ if(a%2 == 0){ throw new Exception("1") VVV }
				// else { throw new Exception("2") } } except(e){...}; - goto
				// inserted at point VVV
				bcoutputter.visitLabel(tryCatch.LabelPreGOTOOnBlockNonDefoRet);
				bcoutputter.visitJumpInsn(GOTO, hasFinal ? startOfTheFinalyBlock : carryOn);
			}

			for (int n = 0; n < tryCatch.cbs.size(); n++) {
				CatchBlocks cat = tryCatch.cbs.get(n);

				bcoutputter.mv.currentFrame.clearStack();
				bcoutputter.mv.currentFrame.push(Value.make(0, cat.getTaggedType().getBytecodeType()));

				boolean hasFinalAndCatchNotReturn = hasFinal && !cat.catchBlock.defoEscapesByBreakExceptRetu;
				lastCatchDefRet = cat.catchBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch();
				lastCatchThrowExcepLastInstr = cat.endsInException;
				bcoutputter.visitLabel(cat.entryLabel);

				int varSlot = localvarStackSize.pop() + 1;
				localvarStackSize.push(varSlot);
				bcoutputter.visitVarInsn(ASTORE, varSlot); // store e in localvar

				ArrayList<Pair<Type, String>> vToAddOnEntry = new ArrayList<Pair<Type, String>>();
				vToAddOnEntry.add(new Pair<Type, String>(cat.getTaggedType(), cat.var));
				varsToAddToScopeOnEntry = vToAddOnEntry;

				if (hasFinal && cat.endsInException && tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch() && !cat.catchBlock.hasDefoThrownException) {
					// ok, so, it has a final and also ends in an exception, to
					// there is a throw which takes us to the finally block
					bcoutputter.labelOverride.put(carryOn, startOfTheFinalyBlock);
					cat.catchBlock.accept(this);
					bcoutputter.labelOverride.remove(carryOn);
				} else if (hasFinalAndCatchNotReturn) {
					// we add the label override as normally code after
					// catchblock is logically the code after
					// the try, but when no exception is thrown and there is a
					// final we need to splice into it
					bcoutputter.labelOverride.put(carryOn, cat.attachedkFinalBlockEntryLabel);
					cat.catchBlock.accept(this);
					
					if (null != expected && !expected.equals(cat.blockType)) {
						Utils.applyCastImplicit(bcoutputter, cat.blockType, expected, this);
					}
					
					bcoutputter.labelOverride.remove(carryOn);
				} else {
					cat.catchBlock.accept(this);
					if (tryCatch.getShouldBePresevedOnStack() && cat.catchBlock.getShouldBePresevedOnStack()) {
						if (!expected.equals(cat.blockType)) {
							Utils.applyCastImplicit(bcoutputter, cat.blockType, expected, this);
						}
						// Utils.applyStore(mv, expected, presistResultOnStack);
						/*if(!cat.endsInException && null != storeReturnInVar){
							this.storeLocalVaraible(storeReturnInVar, null, expected, expected);
						}*/
					}
					
					
					
				}

				if (!hasFinal && tryCatch.cbs.size() - 1 != n && !cat.catchBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()) {
					bcoutputter.visitJumpInsn(GOTO, carryOn);
				} else if (hasFinalAndCatchNotReturn && n != tryCatch.cbs.size() - 1) {// skip
																						// for
																						// last
					Label beforeFin = tryCatch.endOfCatchToAttachedFinal.get(cat.entryLabel);
					if (!tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()) {
						setEntryLabelOfBlock(cat.attachedFinalBlock, beforeFin);
						earlyProcessFinalBlock(cat.attachedFinalBlock);
						bcoutputter.visitJumpInsn(GOTO, carryOn);
					} else if (lastCatchThrowExcepLastInstr) {
						// skip, the Throw will act like a goto to the final
						// block anyway
					} else {
						bcoutputter.visitLabel(beforeFin);
						Label after = hasFinal ? startOfTheFinalyBlock : carryOn;
						bcoutputter.visitJumpInsn(GOTO, after);
					}
				} else if (n == tryCatch.cbs.size() - 1 && hasFinal && !tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch() && !cat.catchBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()) {
					// if it's the last one and there is a final block which
					// doesnt defo return, process it here then jump over to
					// after the trycatch, but note that if the thing defo
					// expcetion
					// throws, your going into the final block whether you like
					// it or not... so ignore the next part
					// mv.visitLabel(tryCatch.finalEnd ); - inc below
					earlyProcessFinalBlock(cat.attachedFinalBlock);
					
					/*if(null != storeReturnInVar){
						this.storeLocalVaraible(storeReturnInVar, null, expected, expected);
					}*/
					
					bcoutputter.visitJumpInsn(GOTO, restoreStackLabel!=null?restoreStackLabel:carryOn);
				}

			}
		}

		if (hasFinal) {
			if (tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()) {
				onReturnsInDefoFinallyRetBlock.pop();
			}

			boolean defFinRet = tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch();
			if (!tryCatch.cbs.isEmpty() && defFinRet && !lastCatchDefRet && !lastCatchThrowExcepLastInstr) {
				bcoutputter.visitLabel(tryCatch.finalEnd);
				bcoutputter.visitJumpInsn(GOTO, tryCatch.startOfTheFinalyBlock);
			}

			bcoutputter.visitLabel(tryCatch.finalHandler);

			bcoutputter.mv.currentFrame.clearStack();
			bcoutputter.mv.currentFrame.push(Value.make(0, this.Const_Object.getBytecodeType()));

			if (defFinRet) {// if defo returns then we dont care about the
							// exception
				bcoutputter.visitInsn(Opcodes.POP);
				setEntryLabelOfBlock(tryCatch.finBlockWhenItDefoRets, startOfTheFinalyBlock);
				tryCatch.finBlockWhenItDefoRets.accept(this);
			} else {// have to rethrow the exception
				int tempSlot = this.createNewLocalVar(this.getTempVarName(), Const_PRIM_INT, false);
				bcoutputter.visitVarInsn(ASTORE, tempSlot);// TODO is astore right?

				if (!tryCatch.blockToTry.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()) {
					if(tryCatch.supressFinnallyException != null){//
						//if(thing !=null){thing.close();} => try{ if(thing !=null){thing.close();} } catch(e){ orig.setSuppressed(e);}
						bcoutputter.visitLabel(tryCatch.supressFinnallyException.getA());
						earlyProcessFinalBlock(tryCatch.finalBlockOnEndOfTry);
						bcoutputter.visitLabel(tryCatch.supressFinnallyException.getB());
					   	bcoutputter.visitJumpInsn(GOTO, tryCatch.finalBlockOnEndOfTry.getLabelAfterCode());
					   	
					   	bcoutputter.visitLabel(tryCatch.supressFinnallyException.getC());
						bcoutputter.mv.currentFrame.clearStack();
						bcoutputter.mv.currentFrame.push(Value.make(0, Const_Object.getBytecodeType()));
						bcoutputter.visitVarInsn(ALOAD, tempSlot);
					   	bcoutputter.visitInsn(SWAP);
					    bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "addSuppressed", "(Ljava/lang/Throwable;)V");
					}
					else{
						earlyProcessFinalBlock(tryCatch.finalBlockOnEndOfTry);
						

						/*if(null != storeReturnInVar && !tryCatch.cbs.isEmpty()){
							this.storeLocalVaraible(storeReturnInVar, null, expected, expected);
						}*/
						
					}

					bcoutputter.visitLabel(tryCatch.finalBlockOnEndOfTry.getLabelAfterCode());
					
					bcoutputter.visitVarInsn(ALOAD, tempSlot);
					bcoutputter.visitInsn(ATHROW);
				} else {//TODO: try with try-block which 100% throws exception
					Label beforetheReThrow = new Label();
					bcoutputter.labelOverride.put(carryOn, beforetheReThrow);
					
					if(tryCatch.supressFinnallyException != null){//
						
						
						bcoutputter.visitLabel(tryCatch.supressFinnallyException.getA());
						tryCatch.finalBlock.accept(this);
						bcoutputter.visitLabel(tryCatch.supressFinnallyException.getB());
					   	bcoutputter.visitJumpInsn(GOTO, carryOn);
					   	
					   	bcoutputter.visitLabel(tryCatch.supressFinnallyException.getC());
						bcoutputter.mv.currentFrame.clearStack();
						bcoutputter.mv.currentFrame.push(Value.make(0, Const_Object.getBytecodeType()));
						bcoutputter.visitVarInsn(ALOAD, tempSlot);
					   	bcoutputter.visitInsn(SWAP);
					    bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "addSuppressed", "(Ljava/lang/Throwable;)V");
						
						
					}
					else{
						tryCatch.finalBlock.accept(this);
					}
					
					
					bcoutputter.labelOverride.remove(carryOn);
					bcoutputter.visitLabel(beforetheReThrow);
					bcoutputter.visitVarInsn(ALOAD, tempSlot);
					bcoutputter.visitInsn(ATHROW);
				}
			}

			if (tryCatch.finalBlock.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()) {

			} else if (!tryCatch.blockToTry.hasDefoReturnedThrownExceptionOrBrokenFromTryCatch()) {
				Type fType = (Type) tryCatch.finalBlock.accept(this);

				if (tryCatch.getShouldBePresevedOnStack() && tryCatch.finalBlock.getShouldBePresevedOnStack()) {
					// mv.pushNextLabel(tryCatch.getLabelAfterCode());
					if (!expected.equals(fType)) {
						Utils.applyCastImplicit(bcoutputter, fType, expected, this);
					}
					// Utils.applyStore(mv, expected, presistResultOnStack);
					
					/*if(null != storeReturnInVar){
						this.storeLocalVaraible(storeReturnInVar, null, expected, expected);
					}*/
					
				}
				
			
			}
		}

		if (mapTheNestedBreaks) {
			breakGotoLabelOverride.pop();
		}

		if (tryCatch.getShouldBePresevedOnStack()) {
			// perform 'early' visit here such that code such as this will work
			// ok: a= if(4>3){12}
			
			if(restoreStackLabel != null){
				bcoutputter.pushNextLabel(restoreStackLabel);
				bcoutputter.labelOverride.remove(tryCatch.getLabelAfterCode());
			}else{
				bcoutputter.pushNextLabel(tryCatch.getLabelAfterCode());
				tryCatch.setLabelAfterCode(null);
			}
		}else if(restoreStackLabel != null){
			bcoutputter.pushNextLabel(restoreStackLabel);
			bcoutputter.labelOverride.remove(tryCatch.getLabelAfterCode());
		}

		// if we are preserving the results of the execution on the stack, we
		// must ensure that the return type is tagged as being on the top of the
		// frame (we probably got here from a goto if the excepion was not
		// thrown)
		if (tryCatch.getShouldBePresevedOnStack() && this.bcoutputter.mv.currentFrame.stack.isEmpty()) {
			// also note that the last try catch final may have dumped something
			// onto thte stak so if not empty then we can take this as being the
			// type to return, so nothing needs to be add in this case hence the
			// isEmpty check above
			//this.bcoutputter.mv.currentFrame.push(Value.make(0, TypeDesc.getInterned(tryCatch.getTaggedType().getBytecodeType())));
		}

		if (!preStackState.isEmpty()) {
			int tryRetState = -1;
			if(tryCatch.getShouldBePresevedOnStack()){
				tryRetState=createNewLocalVar(this.getTempVarName(), expected, true);
			}
			
			// restore pre state...
			Type varType = null;
			for (int n = preStackState.size(); n > 0; n--) {
				Pair<Type, Integer> vandSlot = preStackState.get(n - 1);
				varType = vandSlot.getA();
				Utils.applyLoad(bcoutputter, varType, vandSlot.getB());
			}
			
			if(tryRetState > -1) {//restore state from try catch in correct order
				Utils.applyLoad(bcoutputter, expected, tryRetState);
			}
			
			
			/*
			// restore pre state...
			Type varType = null;
			for (int n = preStackState.size(); n > 0; n--) {
				Tuple<Type, Integer> vandSlot = preStackState.get(n - 1);
				varType = vandSlot.getA();
				Utils.applyLoad(bcoutputter, varType, vandSlot.getB());
			}
			
			if(null != storeReturnInVar){
				this.loadLocalVar(storeReturnInVar, null);
			}*/
			
			
			
			
			
			
			
			
			
			//proceed as normal...
			if(null != tryCatch.getLabelAfterCode()){
				bcoutputter.pushNextLabel(tryCatch.getLabelAfterCode());
			}
			
		}

		// Utils.applyLoad(mv, expected, presistResultOnStack);

		return expected;
	}

	@Override
	public Object visit(Changed changed) {
		bcoutputter.visitVarInsn(ALOAD, changed.isModuleLevel ? 1 : 2);
		return changed.getTaggedType();
	}

	private Object genOnChangeAwait(AsyncInvokable onChange, String isoName) {
		// functionsToAddOnLeavingThisFunction.add(new Tuple<FuncDef,
		// TheScopeFrame>(onChange.initMethodNameFuncDef,
		// this.currentScopeFrame.getChild(onChange.initMethodNameFuncDef.funcblock)));
		// functionsToAddOnLeavingThisFunction.add(new Tuple<FuncDef,
		// TheScopeFrame>(onChange.applyMethodFuncDef,
		// this.currentScopeFrame.getChild(onChange.applyMethodFuncDef.funcblock)));
		// functionsToAddOnLeavingThisFunction.add(new Tuple<FuncDef,
		// TheScopeFrame>(onChange.cleanUpMethodFuncDef,
		// this.currentScopeFrame.getChild(onChange.cleanUpMethodFuncDef.funcblock)));

		onChange.getinitMethodNameFuncDef().accept(this);
		onChange.getapplyMethodFuncDef().accept(this);
		onChange.getcleanUpMethodFuncDef().accept(this);

		boolean isAwait = "IsoTaskAwait".equals(isoName);

		// spawn init, join, then continue proc once ref to designate completion
		// is done
		// public Ref<Boolean> scheduleTask(IsoTaskNotifiable func){

		bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/runtime/cps/Fiber", "getScheduler", "()Lcom/concurnas/bootstrap/runtime/cps/Scheduler;");
		bcoutputter.visitInsn(DUP);
		
		// create state class and splice in variables

		String onChangeFuncClass = onChange.getonChangeDets().getA();
		bcoutputter.visitTypeInsn(NEW, onChangeFuncClass);
		bcoutputter.visitInsn(DUP);

		// state object class instance
		String soname = onChange.getFullnameSO();
		ArrayList<Type> inputArgsForSO = new ArrayList<Type>();
		bcoutputter.visitTypeInsn(NEW, soname);
		bcoutputter.visitInsn(DUP);

		makeConctracker();
		int tempTrackerSlot = this.createNewLocalVar(this.getTempVarName(), Const_Object, true);
		
		
		FuncParams spliceIn = onChange.getExtraCapturedLocalVars();
		// store in var if we need to extract changed variables from the state
		// object

		HashSet<String> namesOverridedInInit = onChange.getnamesOverridedInInit();

		int rextractVarsFromSO = -1;
		ArrayList<RefName> rextractRefNames = null;
		if (isAwait && !spliceIn.params.isEmpty()) {
			rextractVarsFromSO = createNewLocalVar(this.getTempVarName() + "$tempSOCopy", false, Const_Object, true, true, true, -1);// cheat
																																	// and
																																	// use
																																	// object
																																	// type
																																	// since
																																	// ALOAD/ASTORE
			rextractRefNames = new ArrayList<RefName>(spliceIn.params.size());
			bcoutputter.visitInsn(DUP);
		}

		int newRefSlot = -1;
		Map<String, String> takeArgFromSO = onChange.gettakeArgFromSO();
		int line = ((Node)onChange).getLine();
		int col = ((Node)onChange).getColumn();
		
		ArrayList<Pair<String, Integer>> refTempVars = new ArrayList<Pair<String, Integer>>();
		
		for (FuncParam localVar : spliceIn.params) {
			if (!namesOverridedInInit.contains(localVar.name)) {

				RefName asRef = new RefName(localVar.name);
				Type fType = localVar.getTaggedType();
				asRef.setTaggedType(fType);
				String classNameSOToTakeVarFrom = takeArgFromSO.get(localVar.name);
				if (null != classNameSOToTakeVarFrom && !localVar.name.equals("ret$")) {

					RefName origRef = (RefName) asRef.copy();
					RefName parso = new RefName(line, col, "stateObject$");
					parso.resolvesTo = new TypeAndLocation(fType, new LocationLocalVar(null));
					origRef.resolvesTo = new TypeAndLocation(fType, new LocationClassField(classNameSOToTakeVarFrom, fType));
					asRef.astRedirectforOnChangeNesting = new DotOperator(line, col, parso, origRef); // x->
																										// stateObject.x
																										// //hack
																										// yuck

				} else {
					asRef.resolvesTo = new TypeAndLocation(fType, new LocationLocalVar(null));
				}

				if (localVar.name.equals("ret$") && !onChange.getnoReturn()) {
					AssignExisting datass = onChange.gettheAssToStoreRefIn();
					if (null == datass) {// we're creating a new ref as result
						newRefSlot = createNewLocalVar(this.getTempVarName() + "$tempVirloc", true, ((Node)onChange).getTaggedType(), true, true, true, -1);
					} else {
						bcoutputter.visitVarInsn(ALOAD, datass.localVarToCopyRefInto);
					}
				} else {
					if (!TypeCheckUtils.shouldNotBeCopied(fType, localVar.isShared) /*&& !TypeCheckUtils.hasRefLevels(fType)*/ && !isAwait) {
						fType = doCopy(fType, asRef, tempTrackerSlot, null, localVar.fromSOname);
						
						if(TypeCheckUtils.getRefLevels(fType) > 0) {
							bcoutputter.visitInsn(DUP);
							String clsName = fType.getCheckCastType();
							
							int tempSlot = this.createNewLocalVar(this.getTempVarName(), fType, false);
							Utils.applyStore(bcoutputter, fType, tempSlot);
							
							refTempVars.add( new Pair<String, Integer>(clsName, tempSlot));
						}
						
					} else if (TypeCheckUtils.isRegistrationSet(this.errorRaisableSupressionFromSatc, fType)) {
						fType = doCopy(fType, asRef, tempTrackerSlot, null, localVar.fromSOname);
					} else {
						asRef.accept(this);
					}
				}

				inputArgsForSO.add(fType);

				if (rextractVarsFromSO > 0) {
					rextractRefNames.add(asRef);
				}
			}
		}

		String methodDesc = getNormalMethodInvokationDesc(inputArgsForSO, Const_PRIM_VOID);
		bcoutputter.visitMethodInsn(INVOKESPECIAL, soname, "<init>", methodDesc);
		//now the state object is on top of the stack - we can create the remaining
		
		// public <init>(LbytecodeSandbox$$onChange0$SO;)V
		// opon second argument
		String onChangeFuncClassMD = "(L" + soname + ";";
		if (!onChange.getisModuleLevel()) {
			onChangeFuncClassMD += onChange.getholderclass().getBytecodeType();
			bcoutputter.visitVarInsn(ALOAD, 0);
		}
		bcoutputter.visitMethodInsn(INVOKESPECIAL, onChangeFuncClass, "<init>", onChangeFuncClassMD + ")V");

		//pass to scheduler to run job
		
		
		/*mv.visitMethodInsn(INVOKESTATIC, "com/concurnas/bootstrap/runtime/cps/Fiber", "getScheduler", "()Lcom/concurnas/bootstrap/runtime/cps/Scheduler;");
		mv.visitInsn(SWAP);
		mv.visitLdcInsn(String.format("%s:%s", this.packageAndClassName, line));
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/bootstrap/runtime/cps/Scheduler", "scheduleTask", "(Lcom/concurnas/runtime/cps/" + isoName + ";Ljava/lang/String;)Lcom/concurnas/bootstrap/runtime/ref/Ref;");
		mv.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/ref/DefaultRef", "waitUntilSet", "()V");
*/
		String isoObjectTypeName = isoObjectTypeNames.get(isoName);
		

		bcoutputter.visitLdcInsn(String.format("%s:%s", this.packageAndClassName, line));
		bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/bootstrap/runtime/cps/Scheduler", "prepare", "(Lcom/concurnas/bootstrap/runtime/cps/" + isoName + ";Ljava/lang/String;)Lcom/concurnas/bootstrap/runtime/cps/"+isoObjectTypeName+";");
		//iso core, take fiber from this and pass it to custom refs so they can init any fields
		
		if(!refTempVars.isEmpty()) {
			//part 2 of copy init of refs
			bcoutputter.visitInsn(DUP2);
			bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/bootstrap/runtime/cps/Scheduler", "getFiber", "(Lcom/concurnas/bootstrap/runtime/cps/Iso;)Lcom/concurnas/bootstrap/runtime/cps/Fiber;");
			
			for(int n=0; n < refTempVars.size(); n++) {
				if(n != refTempVars.size()-1) {
					bcoutputter.visitInsn(DUP);
				}
				Pair<String, Integer> itm = refTempVars.get(n);
				bcoutputter.visitVarInsn(ALOAD, itm.getB());
				bcoutputter.visitInsn(SWAP);
				bcoutputter.visitVarInsn(ALOAD, tempTrackerSlot);
				
				bcoutputter.visitMethodInsn(INVOKEVIRTUAL, itm.getA(), "initFields$", "(Lcom/concurnas/bootstrap/runtime/cps/Fiber;Lcom/concurnas/bootstrap/runtime/CopyTracker;)V");
			}
		}

		bcoutputter.visitVarInsn(ALOAD, tempTrackerSlot);
		bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/bootstrap/runtime/cps/Scheduler", "scheduleTask", "(Lcom/concurnas/bootstrap/runtime/cps/"+isoObjectTypeName+";Lcom/concurnas/bootstrap/runtime/CopyTracker;)Lcom/concurnas/bootstrap/runtime/ref/Ref;");
		bcoutputter.visitMethodInsn(INVOKEINTERFACE, "com/concurnas/bootstrap/runtime/ref/DefaultRef", "waitUntilSet", "()V");
		
		if (rextractVarsFromSO > -1) {
			// copy back in local bindings which may have been changed within
			// the await block
			// JPT: optimization: only need to copy back if actually chance to
			// be changed within block - oh this is quite complex actually...
			for (RefName localVar : rextractRefNames) {

				/*
				 * if(null!=onChange.takeArgFromSO.get(localVar.name) &&
				 * !localVar.name.equals("ret$")){
				 * 
				 * }
				 */
				
				Type tt = localVar.getTaggedType();
				String name = localVar.name;
				
				TypeAndLocation tal = localVar.resolvesTo;
				
				if(null == tal && localVar.astRedirectforOnChangeNesting != null ){
					DotOperator asDot = (DotOperator)localVar.astRedirectforOnChangeNesting;
					ArrayList<Expression> elements = asDot.getElements(this);
					RefName asRef = (RefName)elements.get(elements.size()-1);
					tal = asRef.resolvesTo;
					
					elements.get(0).accept(this);
				}
				
				bcoutputter.visitVarInsn(ALOAD, rextractVarsFromSO);
				bcoutputter.visitFieldInsn(GETFIELD, soname, name, tt.getBytecodeType());
				
				storeLocalVaraible(name, tal, tt, TypeCheckUtils.getRefLevels(tt) > 0/*, null*/);
			}

		}

		if (newRefSlot > -1) {
			bcoutputter.visitVarInsn(ALOAD, newRefSlot);// shudder
		}

		return ((Node)onChange).getTaggedType();
	}

	private final static HashMap<String, String> isoObjectTypeNames = new HashMap<String, String>();
	static {
		isoObjectTypeNames.put("IsoTaskEvery", "IsoEvery");
		isoObjectTypeNames.put("IsoTaskNotifiable", "IsoNotifiable");
		isoObjectTypeNames.put("IsoTaskAwait", "IsoAwait");
	}
	
	@Override
	public Object visit(Await onChange) {
		return genOnChangeAwait(onChange, "IsoTaskAwait");
	}

	@Override
	public Object visit(OnChange onChange) {
		

/*
		PrintSourceVisitor psv = new PrintSourceVisitor();
		psv.visit(onChange.applyMethodFuncDef);
		
		String ret = com.concurnas.compiler.visitors.Utils.listToString(psv.items);
		System.out.println(ret);*/
		
		return genOnChangeAwait(onChange, "IsoTaskNotifiable");
	}

	@Override
	public Object visit(OnEvery onChange) {
		return genOnChangeAwait(onChange, "IsoTaskEvery");
	}

	@Override
	public Object visit(AsyncBodyBlock asyncBodyBlock) {
		level++;
		level++;//wtf, magic variables here...
		if(!asyncBodyBlock.preBlockVars.isEmpty()){
			for(String name : asyncBodyBlock.preBlockVars.keySet()){
				Type tt = asyncBodyBlock.preBlockVars.get(name);
				createNewLocalVar(name, false, tt, false, false, false, -1);
			}
		}
		
		for(LineHolder lh : asyncBodyBlock.mainBody.lines){//add methods associated with nested onchange/every instances
			OnChange onChange = (OnChange)lh.l;
			onChange.getinitMethodNameFuncDef().accept(this);
			onChange.getapplyMethodFuncDef().accept(this);
			onChange.getcleanUpMethodFuncDef().accept(this);
		}
		level--;//MHA: just magic at 2:30am...
		Object x = genOnChangeAwait(asyncBodyBlock, asyncBodyBlock.hasEvery?"IsoTaskEvery":"IsoTaskNotifiable");
		
		level--;
		return x;
	}

	@Override
	public Object visit(NOP nop) {
		if(nop.oneAndPop) {
			bcoutputter.visitInsn(ICONST_0);
			bcoutputter.visitInsn(POP);
		}
		return null;
	}

	@Override
	public Object visit(InExpression cont) {
		/*if(cont.isArrayMatch){
			cont.thing.accept(this);
			cont.insideof.accept(this);
			

			return Const_PRIM_BOOl;
		}
		else*/ if(cont.isRegexPatternMatch){
			{
				cont.thing.accept(this);
				cont.insideof.accept(this);
				bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/util/regex/Pattern", "matcher", "(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;");
				bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "java/util/regex/Matcher", "find", "()Z");
			}
			
			if(cont.inverted){
				Label ifSomething = new Label();//whatever, works
				bcoutputter.visitJumpInsn(IFEQ, ifSomething);
				bcoutputter.visitInsn(ICONST_0);
				Label endSeg = new Label();
				bcoutputter.visitJumpInsn(GOTO, endSeg);
				bcoutputter.visitLabel(ifSomething);
				bcoutputter.visitInsn(ICONST_1);
				bcoutputter.visitLabel(endSeg);
			}
			
			return Const_PRIM_BOOl;
		}
		else{
			return (Type)cont.containsMethodCall.accept(this);
		}
		
	}
	
	private boolean buildEnumSubclasses(ArrayList<EnumItem> enumItemz, String parentClassName){
		//e.g. enum ASD{ PLUS{ f=8} }//plus has own stuff seperate from main enum cls
		boolean madeChanges=false;
		for(EnumItem itm : enumItemz){
			if(itm.fakeclassDef != null){
				//ok we need a custom class for this
				//itm.className=normalClassName + "$" + idx;
				ClassDef fakeclassDef=itm.fakeclassDef;
				String itemName = fakeclassDef.bcFullName(); 
				
				
				
				/*
				ClassWriter cw = new ClassWriter(0);
				FieldVisitor fv;
				MethodVisitor mv;
				AnnotationVisitor av0;

				cw.visit(V1_8, ACC_SUPER + ACC_ENUM, "concurnas/Lowie$XX$1", null, "concurnas/Lowie$XX", null);

				cw.visitSource("Lowie.java", null);

				cw.visitOuterClass("concurnas/Lowie$XX", null, null);

				cw.visitInnerClass("concurnas/Lowie$XX", "concurnas/Lowie", "XX", ACC_PUBLIC + ACC_STATIC + ACC_ENUM);

				cw.visitInnerClass("concurnas/Lowie$XX$1", null, null, ACC_ENUM);

				{
				mv = cw.visitMethod(0, "<init>", "(Ljava/lang/String;I)V", null, null);
				mv.visitCode();
				Label l0 = new Label();
				mv.visitLabel(l0);
				mv.visitLineNumber(92, l0);
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitVarInsn(ILOAD, 2);
				mv.visitInsn(ACONST_NULL);
				mv.visitMethodInsn(INVOKESPECIAL, "concurnas/Lowie$XX", "<init>", "(Ljava/lang/String;ILconcurnas/Lowie$XX;)V");
				Label l1 = new Label();
				mv.visitLabel(l1);
				mv.visitLineNumber(1, l1);
				mv.visitInsn(RETURN);
				Label l2 = new Label();
				mv.visitLabel(l2);
				mv.visitLocalVariable("this", "Lconcurnas/Lowie$XX$1;", null, l0, l2, 0);
				mv.visitMaxs(4, 3);
				mv.visitEnd();
				}
				cw.visitEnd();

				return cw.toByteArray();
				}
				}
				*/
				
				enterNewClass(fakeclassDef, null, parentClassName, false, true, null, Opcodes.ACC_ENUM, null, false);

				currentClassDef.add(itm.className);
				currentClassDefObj.add(fakeclassDef);
				
				/*{//init
					mv = new BytecodeOutputter(this.cw, DEBUG_MODE);
					mv.enterMethod(0, "<init>", "(Ljava/lang/String;I)V", null, null, this.currentClassDef.peek());
					//MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "<init>", "(Ljava/lang/String;I)V", null, null);
					//mv.visitCode();
					Label l0 = new Label();
					mv.visitLabel(l0);
					mv.visitNewLine(itm.getLine());
					mv.visitVarInsn(ALOAD, 0);
					mv.visitVarInsn(ALOAD, 1);
					mv.visitVarInsn(ILOAD, 2);
					mv.visitInsn(ACONST_NULL);
					mv.visitMethodInsn(INVOKESPECIAL, parentClassName, "<init>", "(Ljava/lang/String;IL"+parentClassName+";)V");
					
					boolean prev = isInclInitCreator;
					DefaultConstuctorFieldInitlizator defFeildInit = new DefaultConstuctorFieldInitlizator(this);
					visitDefConsFieldInit(defFeildInit);
					isInclInitCreator = prev;
					
					mv.visitInsn(RETURN);
					//mv.visitEnd();
					mv.exitMethod();
				}*/
				
				TheScopeFrame prev = this.currentScopeFrame;

				this.currentScopeFrame = this.currentScopeFrame.getChild(itm.block);
				
				createAllModuleFields();
				
				this.currentScopeFrame=prev;
				
				itm.block.accept(this);
				
				exitClass();
				currentClassDef.pop();
				currentClassDefObj.pop();
				madeChanges=true;
			}
			//itm.className=normalClassName;
		}
		
		return madeChanges;
	}
	
	@Override
	public Object visit(EnumDef enumDef) {
		String className = enumDef.enaumName;
		String fullClassName = enumDef.bcFullName();
		boolean preva = isInclInitCreator;

		boolean buildSubClasses = buildEnumSubclasses(enumDef.block.enumItemz, fullClassName);
		
		if(buildSubClasses){
			for( EnumItem itm : enumDef.block.enumItemz){
				if(null != itm.fakeclassDef){
					cw.visitInnerClass(itm.fakeclassDef.bcFullName(), null, null, ACC_ENUM);
				}
			}
		}
		
		String ggenericSig = "Ljava/lang/Enum<L"+fullClassName+";>;";
		
		//ACC_PUBLIC + ACC_FINAL + ACC_SUPER + ACC_ENUM
		enumDef.fakeclassDef.setParentNestor(null);//hack, its a static class so has no parent nestor
		enterNewClass(enumDef.fakeclassDef, ggenericSig, "java/lang/Enum", false, false, null, Opcodes.ACC_ENUM, null, false);
		
		String fullClassNameWithBits = "L" + fullClassName + ";";
		
		{
			FieldVisitor fv = cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC + ACC_SYNTHETIC, "ENUM$VALUES", "[L"+fullClassName +";", null, null);
			fv.visitEnd();
		}
						
		{//clinit

			bcoutputter = new BytecodeOutputter(this.cw, DEBUG_MODE);
			bcoutputter.enterMethod(ACC_STATIC, "<clinit>", "()V", null, null, className);
			
			//MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
			//mv.visitCode();
			
			int n=0;
			for(EnumItem itm : enumDef.block.enumItemz){
				//String itemName = itm.fakeclassDef==null?fullClassName:itm.fakeclassDef.bcFullName();
				Label itemL = new Label();
				bcoutputter.visitLabel(itemL);
				bcoutputter.visitNewLine(itm.getLine());
				
				itm.mappedConstructor.accept(this);
				
				//TODO: more stuff to constructor here
				bcoutputter.visitFieldInsn(PUTSTATIC, fullClassName, itm.name, fullClassNameWithBits);
				
			}
						
			Label initEnumLabel = new Label();
			bcoutputter.visitLabel(initEnumLabel);
			bcoutputter.visitNewLine(enumDef.getLine());
			
			Utils.intOpcode(bcoutputter, enumDef.block.enumItemz.size());
			
			bcoutputter.visitTypeInsn(ANEWARRAY, fullClassName);
			
			n=0;
			for(EnumItem itm : enumDef.block.enumItemz){
				bcoutputter.visitInsn(DUP);
				Utils.intOpcode(bcoutputter, n++);
				bcoutputter.visitFieldInsn(GETSTATIC, fullClassName, itm.name, fullClassNameWithBits);
				bcoutputter.visitInsn(AASTORE);
			}
						
			bcoutputter.visitFieldInsn(PUTSTATIC, fullClassName, "ENUM$VALUES", "["+fullClassNameWithBits);
			
			bcoutputter.visitInsn(RETURN);			
			//mv.visitEnd();

			bcoutputter.exitMethod();
		}
		

		currentClassDef.add(className);
		currentClassDefObj.add(enumDef.fakeclassDef);
		
		TheScopeFrame prev = this.currentScopeFrame;

		this.currentScopeFrame = this.currentScopeFrame.getChild(enumDef.fakeclassDef.classBlock);
		
		if(buildSubClasses){//for subclasses to call...
			
			HashSet<FuncType> cons = enumDef.fakeclassDef.getAllConstructors();
			for(FuncType con : cons){
				//fix this part in a minute
				ArrayList<Type> inpus = new ArrayList<Type>(con.getInputs()); 
				inpus.add(0, ScopeAndTypeChecker.const_int);
				inpus.add(0, ScopeAndTypeChecker.const_string);
				
				ArrayList<Type> inpusIncDummy = new ArrayList<Type>(inpus); 
				inpusIncDummy.add(new NamedType(enumDef.fakeclassDef));
				
				String methodDescIncDummy = getNormalMethodInvokationDesc(inpusIncDummy, ScopeAndTypeChecker.const_void);
				String methodDesc = getNormalMethodInvokationDesc(inpus, ScopeAndTypeChecker.const_void);
				
				//MethodVisitor mv = cw.visitMethod(ACC_SYNTHETIC, "<init>", methodDescIncDummy, null, null);
				//mv.visitCode();
				
				bcoutputter = new BytecodeOutputter(this.cw, DEBUG_MODE);
				bcoutputter.enterMethod(ACC_SYNTHETIC, "<init>", methodDescIncDummy, null, null, className);
				
				Label l0 = new Label();
				bcoutputter.visitLabel(l0);
				bcoutputter.visitNewLine(enumDef.getLine());
				bcoutputter.visitVarInsn(ALOAD, 0);
				
				int n=1;
				for(Type ta: inpus){
					Utils.applyLoad(bcoutputter, ta, n);
					n+=Utils.varSlotsConsumedByType(ta);
				}
				
				bcoutputter.visitMethodInsn(INVOKESPECIAL, fullClassName, "<init>", methodDesc, false);//call to normal one
				bcoutputter.visitInsn(RETURN);
				//mv.visitEnd();
				bcoutputter.exitMethod();
				
			}
			/*
			MethodVisitor mv = cw.visitMethod(ACC_SYNTHETIC, "<init>", "(Ljava/lang/String;I"+fullClassNameWithBits+")V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitLineNumber(enumDef.getLine(), l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitVarInsn(ILOAD, 2);
			mv.visitMethodInsn(INVOKESPECIAL, fullClassName, "<init>", "(Ljava/lang/String;I)V", false);//call to normal one
			mv.visitInsn(RETURN);
			mv.visitEnd();*/
		}
		
		
		{//values
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "values", "()["+fullClassNameWithBits, null, null);
			mv.visitCode();
			mv.visitFieldInsn(GETSTATIC, fullClassName, "ENUM$VALUES", "["+fullClassNameWithBits);
			mv.visitInsn(DUP);
			mv.visitVarInsn(ASTORE, 0);
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitInsn(ARRAYLENGTH);
			mv.visitInsn(DUP);
			mv.visitVarInsn(ISTORE, 1);
			mv.visitTypeInsn(ANEWARRAY, fullClassName);
			mv.visitInsn(DUP);
			mv.visitVarInsn(ASTORE, 2);
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ILOAD, 1);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V", false);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(5, 3);
			mv.visitEnd();
		}
		
		{//valueOf
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "valueOf", "(Ljava/lang/String;)"+fullClassNameWithBits, null, null);
			mv.visitCode();
			mv.visitLdcInsn(org.objectweb.asm.Type.getType(fullClassNameWithBits));
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Enum", "valueOf", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;", false);
			mv.visitTypeInsn(CHECKCAST, fullClassName);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(2, 1);
			mv.visitEnd();
		}
		
		
		/*HashMap<String, Type> fieldNameToType = */createAllModuleFields();
		/*createDefValueFieldInit(enumDef.fakeclassDef, fieldNameToType, fullClassName);*/
		
		this.currentScopeFrame=prev;
		
		enumDef.block.mainBlock.accept(this);
		
		exitClass();
		currentClassDef.pop();
		currentClassDefObj.pop();

		isInclInitCreator = preva;
		
		return null;
	}

	@Override
	public Object visit(EnumItem enumItem) {
		return null;//ignore dealt with elsewhere
	}

	@Override
	public Object visit(EnumBlock enumBlock) {
		return null;//ignore dealt with elsewhere
	}
	
	@Override
	public Object visit(InitBlock initBlock){
		return null;//ignore dealt with elsewhere
	}
	
	
	@Override
	public Object visit(Annotations annotations){
		//not interesting
		for(Annotation annot : annotations.annotations){
			annot.accept(this);
		}
		return null;//non functional
	}
	
	private Object extractAnnotationElement(Object xxx){//MHA: this is a bit hacky
		if(xxx instanceof Type){
			return org.objectweb.asm.Type.getType(((Type)xxx).getBytecodeType());
		}
		else if(xxx instanceof Class){
			return org.objectweb.asm.Type.getType((Class<?>)xxx);
		}
		else{
			return xxx;
		}
	}
	private AnnotationVisitor nextAV = null;
	
	@Override
	public AnnotationVisitor visit(Annotation annotation){
		if(annotation.getTaggedType() == null) {
			return null;
		}
		
		AnnotationVisitor av0;
		if(nextAV!=null){
			av0=nextAV;
		}
		else{
			if(annotation.fieldVisitor != null){
				av0 = annotation.fieldVisitor.visitAnnotation(annotation.getTaggedType().getBytecodeType(), true);
			}
			else if(annotation.atClassLevel){
				av0 = cw.visitAnnotation(annotation.getTaggedType().getBytecodeType(), true);
			}
			else{
				if(annotation.parameterAnnotaionArg != null){
					av0 = bcoutputter.visitParameterAnnotation(annotation.parameterAnnotaionArg, annotation.getTaggedType().getBytecodeType(), true);
				}
				else{
					av0 = bcoutputter.visitAnnotation(annotation.getTaggedType().getBytecodeType(), true);
				}
			}
		}
		
		ArrayList<Thruple<String, Expression, Type>> args = annotation.getArguments();
		
		if(null != args){
			for(Thruple<String, Expression, Type> kv : args){
				String key = kv.getA();
				Expression	expr = kv.getB();
				Type resolvedType = kv.getC();//expr.getTaggedType();
				if(null ==resolvedType){
					resolvedType=expr.getTaggedType();
				}
				Object foldedConstatnt = expr.getFoldedConstant();
				boolean isEnum = TypeCheckUtils.isEnum(resolvedType);
				
				if(resolvedType.hasArrayLevels() && !foldedConstatnt.getClass().isArray()){
					//turn it into an array!
					
					Object ar = Array.newInstance(TypeCheckUtils.getPrimativeClassIfRelevant(foldedConstatnt.getClass()), 1);
					Array.set(ar, 0, foldedConstatnt);
					foldedConstatnt = ar;
				}
				
				if(TypeCheckUtils.isNonPrimativeArray(resolvedType) /*&& foldedConstatnt.getClass().isArray()*/){//non primative array
					boolean isAnnotation = TypeCheckUtils.isAnnotation(resolvedType);
					AnnotationVisitor av1 = av0.visitArray(key);
					int sz = Array.getLength(foldedConstatnt);
					for(int n=0; n < sz; n++){
						Object val = extractAnnotationElement(Array.get(foldedConstatnt, n));
						if(isEnum){
							resolvedType.setArrayLevels(0);
							av1.visitEnum(null, resolvedType.getBytecodeType(), val.toString());
						}
						else if(isAnnotation){
							resolvedType.setArrayLevels(0);
							nextAV = av1.visitAnnotation(null, resolvedType.getBytecodeType());
							((Annotation)val).accept(this);
						}
						else{
							av1.visit(null, val);
						}
					}
					av1.visitEnd();
				}
				else if(isEnum){
					av0.visitEnum(key, resolvedType.getBytecodeType(), foldedConstatnt.toString());//folded resolves to a string
				}
				else if(expr instanceof Annotation){//nested annotation
					nextAV = av0.visitAnnotation(key, resolvedType.getBytecodeType());
					expr.accept(this);
				}
				else{
					av0.visit(key, extractAnnotationElement(foldedConstatnt));
				}
			}
		}
		
		av0.visitEnd();
		nextAV=null;
	
		return null;//ignore non functional
	}

	@Override
	public Object visit(AnnotationDef annotationDef) {
		TheScopeFrame prev = this.currentScopeFrame;
		
		//T/heScopeFrame prev = this.currentScopeFrame;
		
		//cw.visit(V1_7, annotationDef.am.getByteCodeAccess() + ACC_ANNOTATION + ACC_ABSTRACT + ACC_INTERFACE, annotationDef.bcFullName(), null, "java/lang/Object", new String[] { "java/lang/annotation/Annotation" });
		enterNewClass(annotationDef.fakeclassDef, null, "java/lang/Object", true, false, new String[] { "java/lang/annotation/Annotation" }, Opcodes.ACC_INTERFACE + Opcodes.ACC_ANNOTATION, null, false);
		
		//cw.visitSource(packageAndClassName + ".conc", null);

	/*	cw.visitInnerClass("concurnas/TheCaller$ASD", "concurnas/TheCaller", "ASD", ACC_PUBLIC + ACC_FINAL + ACC_STATIC + ACC_ENUM);
		cw.visitInnerClass("concurnas/TheCaller$Easy", "concurnas/TheCaller", "Easy", ACC_PUBLIC + ACC_STATIC + ACC_ANNOTATION + ACC_ABSTRACT + ACC_INTERFACE);
		cw.visitInnerClass("concurnas/TheCaller$Fella", "concurnas/TheCaller", "Fella", ACC_PUBLIC + ACC_STATIC + ACC_ANNOTATION + ACC_ABSTRACT + ACC_INTERFACE);*/

		for(LineHolder lh : annotationDef.annotBlock.lines){
			Line lin = lh.l;
			if(lin instanceof DuffAssign){
				lin = (Line)((ExpressionList)((DuffAssign)lin).e).astRedirect;
			}
			
			if(lin instanceof Assign){
				Object hasDefault;
				Type retType;
				String name;
				Expression leExpression;
				Annotations annots;
				if(lin instanceof AssignNew){
					AssignNew asnew = (AssignNew)lin;
					name = asnew.name;
					hasDefault = asnew.expr==null?null:asnew.expr.getFoldedConstant();
					leExpression = asnew.expr;
					retType=asnew.type;
					annots=asnew.annotations;
				}
				else{// if(lin instanceof AssignExisting){
					AssignExisting asse = (AssignExisting)lin;
					hasDefault=asse.expr.getFoldedConstant();//must do
					leExpression = asse.expr;
					retType=lin.getTaggedType();
					name = ((RefName)asse.assignee).name;
					annots = asse.annotations;
				}
				
				{
					BytecodeOutputter mv = new BytecodeOutputter(this.cw, DEBUG_MODE);
					mv.enterMethod(ACC_PUBLIC + ACC_ABSTRACT, name, "()" + retType.getBytecodeType(), null, null, annotationDef.name);
					//mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, name, "()" + retType.getBytecodeType(), null, null);
					if(hasDefault != null)
					{
						//clever, so the default value is wrapped up inside an annotation, so we can reuse the annotation processing code here...
						Annotation annot = new Annotation(annotationDef.getLine(), annotationDef.getColumn(), annotationDef.name, leExpression, null, new ArrayList<String>());
						this.nextAV = mv.visitAnnotationDefault();
						annot.accept(this);
					}
					
					if(null != annots){
						BytecodeOutputter prevx = mv;
						this.bcoutputter = mv;
						annots.accept(this);
						this.bcoutputter =prevx; 
					}

					mv.exitMethod();
				}
			}
		}
				
		
		//this.currentScopeFrame=prev;
		this.currentScopeFrame=prev;
		annotationDef.annotBlock.accept(this);//for nested stuff?
		exitClass();
		


		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object visit(TypedefStatement typedefStatement) {
		return null;
	}

	
	@Override
	public Object visit(MatchStatement matchStatement) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	@Override
	public Object visit(CaseExpressionWrapper caseExpressionWrapper) {
		return null;//ignore
	}

	@Override
	public Object visit(CaseExpressionPre caseExpressionPre) {
		return null;//ignore
	}

	@Override
	public Object visit(CaseExpressionPost caseExpressionPost) {
		return null;//ignore
	}
	
	@Override
	public Object visit(CaseExpressionTuple caseExpressionTuple) {
		return null;//ignore
	}	
	
	@Override
	public Object visit(CaseExpressionAnd caseExpressionAnd) {
		return null;//ignore
	}

	@Override
	public Object visit(CaseExpressionOr caseExpressionOr) {
		return null;//ignore
	}

	@Override
	public Object visit(CaseExpressionAssign caseExpressionUntypedAssign) {
		return null;//ignore
	}
	
	@Override
	public Object visit(CaseExpressionObjectTypeAssign caseExpressionObjectTypeAssign) {
		return null;//ignore
	}
	
	@Override
	public Object visit(CaseExpressionAssignTuple caseExpressionUntypedAssign) {
		return null;//ignore
	}
	
	@Override
	public Object visit(TypedCaseExpression typedCaseExpression) {
		return null;//ignore
	}	
	
	@Override
	public Object visit(DeleteStatement deleteStatement){
		for(Expression ee : deleteStatement.exprs) {
			Expression terminatal = ee;
			if(terminatal instanceof DotOperator){
				DotOperator dotop = (DotOperator)terminatal;
				terminatal = dotop.getLastElement();
				this.processDotOperator(dotop, false, false);
				this.setTerminalRefPreccededByThis(terminatal, true);
			}
			
			if(deleteStatement.operatesOn == DSOpOn.LOCALVAR){
				Type got = (Type)ee.accept(this);
				if(!(got instanceof PrimativeType)){
					
					bcoutputter.visitInsn(DUP);
					Label ifNull = new Label();
					bcoutputter.visitJumpInsn(IFNULL, ifNull);
					bcoutputter.visitLabel(new Label());
					bcoutputter.visitTypeInsn(CHECKCAST, "com/concurnas/bootstrap/runtime/cps/CObject");
					bcoutputter.visitMethodInsn(INVOKEVIRTUAL, "com/concurnas/bootstrap/runtime/cps/CObject", "delete", "()V", false);
					Label carryon = new Label();
					bcoutputter.visitJumpInsn(GOTO, carryon);
					
					bcoutputter.visitLabel(ifNull);
					bcoutputter.visitInsn(POP);
					bcoutputter.visitLabel(carryon);
				}else if(got.hasArrayLevels()) {
					bcoutputter.visitInsn(POP);
				}
			} else if(deleteStatement.operatesOn == DSOpOn.MAPREF || deleteStatement.operatesOn == DSOpOn.OBJREF){
				ArrayRef ar = (ArrayRef)terminatal;
				ar.accept(this);
				
				if(!deleteStatement.getShouldBePresevedOnStack()) {
					bcoutputter.visitInsn(POP);
				}
				
				//mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;");
			} else if(deleteStatement.operatesOn == DSOpOn.LISTREF){
				ArrayRef ar = (ArrayRef)terminatal;
				ar.accept(this);
				
				if(ar.arrayLevelElements.getLastArrayRefElement().e1.getTaggedType().equals(ScopeAndTypeChecker.const_int)){
					bcoutputter.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "remove", "(I)Ljava/lang/Object;");
				}else{
					bcoutputter.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "remove", "(Ljava/lang/Object;)Z");
				}

				if(!deleteStatement.getShouldBePresevedOnStack()) {
					bcoutputter.visitInsn(POP);
				}
			}
		}
		
		
		return null;
	}

	@Override
	public Object visit(DMANewFromExpression dmaNewFromExpression){
		dmaNewFromExpression.e.accept(this);
		return null;
	}

	@Override
	public Object visit(SizeofStatement sizeofStmt){
		Type got = (Type)sizeofStmt.e.accept(this);
		got = Utils.unref(bcoutputter, got, this);
		bcoutputter.visitMethodInsn(INVOKESTATIC, "com/concurnas/lang/offheap/storage/SizeofProvider", "sizeof", "(Ljava/lang/Object;)I");
		return sizeofStmt.getTaggedType();
	}

	@Override
	public Object visit(LocalClassDef localClassDef) {
		return null;//stub
	}

	@Override
	public Object visit(TypeReturningExpression typeReturningExpression) {
		return null;//not used
	}
	
	@Override
	public Object visit(TransBlock transBlock) {//unused as astredirect takes over
		return null;
	}
	
	@Override
	public Object visit(JustAlsoCaseExpression justAlsoCaseExpression){
		return null;
	}

	@Override
	public Object visit(ExpressionList expressionList) {
		return null;//should not get here as expression will have been redirected
	}

	@Override
	public Object visit(Vectorized vectorized) {
		return null;//should have been translated out
	}

	@Override
	public Object visit(VectorizedFuncInvoke vectorizedFuncInvoke) {
		return null;
	}
	
	@Override
	public Object visit(VectorizedFieldRef vectorizedFieldRef) {
		return null;//vectorizedFieldRef.getTaggedType();
	}
	
	@Override
	public Object visit(VectorizedFuncRef vectorizedFuncRef) {
		return null;
	}
	
	@Override
	public Object visit(VectorizedArrayRef vectorizedFieldRef) {
		return null;
	}
	
	@Override
	public Object visit(VectorizedNew vectorizedFuncRef) {
		return null;
	}

	@Override
	public Object visit(MultiType multiType) {
		return false;
	}

	@Override
	public Object visit(ImpliInstance impliInstance) {
		return null;//dealt with elsewhere
	}
	
	@Override
	public Object visit(PointerAddress pointerAddress) {
		return null;//dealt with elsewhere
	}
	
	@Override
	public Object visit(PointerUnref pointerAddress) {
		return null;//dealt with elsewhere
	}

	@Override
	public Object visit(TupleExpression tupleExpression) {
		NamedType tupleType = (NamedType)tupleExpression.getTaggedType();

		List<Type> types = tupleType.getGenTypes();
		String tname = tupleType.getBytecodeType();
		tname = tname.substring(1, tname.length()-1);
		bcoutputter.visitTypeInsn(NEW, tname);
		bcoutputter.visitInsn(DUP);
		
		Utils.createTypeArray(bcoutputter, tupleType);
		/*int typeSize = types.size();
		Utils.intOpcode(bcoutputter, typeSize);
		bcoutputter.visitTypeInsn(ANEWARRAY, "java/lang/Class");
		
		for(int n=0; n < typeSize; n++){//construct array as normal
			String ttt = types.get(n).getBytecodeType();
			bcoutputter.visitInsn(DUP);
			Utils.intOpcode(bcoutputter, n);
			bcoutputter.visitLdcInsn(org.objectweb.asm.Type.getType(ttt));
			bcoutputter.visitInsn(AASTORE);
		}*/
		
		int n=0;
		StringBuilder methodInv = new StringBuilder("([Ljava/lang/Class;");
		for(Expression expr: tupleExpression.tupleElements) {
			Type got = (Type)expr.accept(this);
			Type expected = types.get(n++);
			Utils.applyCastImplicit(bcoutputter, got, expected, this);
			methodInv.append("Ljava/lang/Object;");
		}
		methodInv.append(")V");

		bcoutputter.visitMethodInsn(INVOKESPECIAL, tname, "<init>", methodInv.toString(), false);
		
		return tupleType;
	}

	@Override
	public Object visit(AssignMulti multiAssign) {
		Type rhsType = (Type)multiAssign.rhs.accept(this);
		
		int tempSlot = this.createNewLocalVar(this.getTempVarName(), rhsType, false);
		Utils.applyStore(bcoutputter, rhsType, tempSlot);
		JustLoad jl = new JustLoad(0,0, tempSlot, rhsType);
		for(Assign ass : multiAssign.assignments) {
			ass.setRHSExpression(jl);
			ass.accept(this);
		}
		
		return null;
	}
	

	@Override
	public Object visit(JustLoad justLoad) {
		Utils.applyLoad(bcoutputter, justLoad.type, justLoad.slot);
		
		if(justLoad.tupleDecompSlot != null) {
			String strType = justLoad.type.getBytecodeType();
			strType = strType.substring(1,  strType.length()-1);
			
			bcoutputter.visitMethodInsn(INVOKEVIRTUAL, strType, "getF" + justLoad.tupleDecompSlot, "()Ljava/lang/Object;", false);
			bcoutputter.visitTypeInsn(CHECKCAST, justLoad.tupleDecompType.getCheckCastType());
			
			//Utils.applyCastImplicit(bcoutputter, ScopeAndTypeChecker.const_object, justLoad.tupleDecompType, this, true);
			return justLoad.tupleDecompType;
		}else {
			return justLoad.type;
		}
	}
	
	@Override
	public Object visit(AssignTupleDeref assignTupleDeref) {
		NamedType rhsType = (NamedType)assignTupleDeref.expr.accept(this);
		rhsType = (NamedType)Utils.unref(bcoutputter, rhsType, this);
		
		ArrayList<Type> tupleComps = rhsType.getGenericTypeElements();
		
		int tempSlot = this.createNewLocalVar(this.getTempVarName(), rhsType, false);
		Utils.applyStore(bcoutputter, rhsType, tempSlot);
		
		int n=0;
		for(Assign ass : assignTupleDeref.lhss) {
			if(ass != null) {
				JustLoad jl = new JustLoad(0,0, tempSlot, rhsType);
				jl.tupleDecompSlot = n;
				jl.tupleDecompType = tupleComps.get(n);
				
				ass.setRHSExpression(jl);
				ass.accept(this);
			}
			n++;
		}
		
		return null;
	}

	@Override
	public Object visit(AnonLambdaDef anonLambdaDef) {
		return null;//should have been converted into a Lambdadef already
	}
	
	@Override
	public Object visit(ObjectProvider objectProvider) {
		return null;
	}
	
	@Override
	public Object visit(ObjectProviderBlock objectProviderBlock) {
		return null;
	}
	@Override
	public Object visit(ObjectProviderLineDepToExpr objectProviderLineDepToExpr) {
		return null;
	}
	@Override
	public Object visit(ObjectProviderLineProvide objectProviderLineProvide) {
		return null;
	}
	
	public void donullcheckForUnknown(Type got, Type expect) {
		if(got instanceof PrimativeType) {
			if(!got.hasArrayLevels()) {
				return;
			}
		}
		
		if( TypeCheckUtils.isUnknown(got)  &&  (expect == null || TypeCheckUtils.isNoNull(expect) )) {
			throwIfNull(true);
		}
		
	}
	
	private void throwIfNull(boolean dup) {
		if(dup) {
			bcoutputter.visitInsn(DUP);
		}
		
		Label noNull = new Label();
		bcoutputter.visitJumpInsn(IFNONNULL, noNull);
		bcoutputter.visitTypeInsn(NEW, "java/lang/NullPointerException");
		bcoutputter.visitInsn(DUP);
		bcoutputter.visitMethodInsn(INVOKESPECIAL, "java/lang/NullPointerException", "<init>", "()V", false);
		bcoutputter.visitInsn(ATHROW);
		
		bcoutputter.visitLabel(noNull);
	}
	
	@Override
	public Object visit(NotNullAssertion notNullAssertion) {
		notNullAssertion.expr.accept(this);
		throwIfNull(notNullAssertion.getShouldBePresevedOnStack());
		return notNullAssertion.getTaggedType();
	}
	
	@Override
	public Object visit(ElvisOperator elvisOperator) {
		Type retType = elvisOperator.getTaggedType();
		Type tt = (Type)elvisOperator.lhsExpression.accept(this);
		String tempName = this.getTempVarName();
		bcoutputter.visitInsn(DUP);
		int tempSlot = this.createNewLocalVar(tempName, tt, true);
		
		Label ifnonnull = new Label();
		Label end = new Label();
		bcoutputter.visitJumpInsn(IFNONNULL, ifnonnull);
		Type got = (Type)elvisOperator.rhsExpression.accept(this);
		if(!got.equals(retType)) {
			Utils.applyCastImplicit(bcoutputter, got, retType, this);
		}

		bcoutputter.visitJumpInsn(GOTO, end);
		bcoutputter.visitLabel(ifnonnull);
		bcoutputter.visitVarInsn(ALOAD, tempSlot);
		bcoutputter.visitLabel(end);

		return elvisOperator.getTaggedType();
	}

	@Override
	public Object visit(LangExt langExt) {
		return null;//will go via ast redirect
	}
}