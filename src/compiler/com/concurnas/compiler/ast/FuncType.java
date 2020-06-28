package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.Fourple;
import com.concurnas.compiler.utils.GenericTypeUtils;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.lang.GPUBuffer;
import com.concurnas.lang.GPUBufferInput;
import com.concurnas.lang.GPUBufferLocal;
import com.concurnas.lang.GPUBufferOutput;
import com.concurnas.runtime.Pair;


public class FuncType  extends AbstractType  implements Comparable<FuncType> {

	public ArrayList<Type> inputs;
	public Type retType;
	private boolean isAbstarct = false;
	private boolean isFinal = false;
	private boolean isConcurnasCallable = true;
	public  boolean hasBeenInputsGenericTypeQualified = false;
	public FuncDef origonatingFuncDef = null;
	public boolean isAutoGen = false;
	private FuncType gensErased = null;
	public FuncType previousFuncTypeIfCopy = null;
	public ClassDef origin = null;
	private TypeAndLocation lambdaDetails=null;

	public NamedType realReturnType = null;
	private ArrayList<GenericType> localGenerics;

	public String definedLocation = "";//myclass.function.innerfunction.innfunction //etc
	public boolean isImplicit = false;
	public List<Type> hackCalledArgumets;//its a hack
	public ArrayList<Type> defaultFuncArgs = null;
	public boolean varargsRequireUpCast;//its another hack! caters for this case calle(1,2) <= def calle(inp char[]) {} //this requires an upcast so it no longer considered a direct match anymore
	public boolean hasVarargs;//its another hack! caters for case where diambiguating between int... and int calls
	public boolean isClassRefType = false;//a = String&
	public boolean usedInMethodDesc = false;//ugly, used to supress convertion of (*) Thingy to interface form
	public boolean signatureExpectedToChange = false;
	
	public boolean extFuncOn;
	public String nameRedirect;
	
	public Fourple<ArrayList<Pair<Boolean, NullStatus>>, ArrayList<Integer>, Integer, Type> hasBeenVectorized = null;//degreeAndWhichVectorized - hack 
	
	public boolean hasBeenInputsGenericTypeQualified()
	{
		return this.hasBeenInputsGenericTypeQualified || hasGenericArgument();
	}
	

	public ArrayList<Type> getInputs() {
		if(null ==inputs){
			return  new ArrayList<Type>();
		}
		return inputs;
	}
	
	public boolean hasArgs(){
		return null != inputs && !inputs.isEmpty();
	}

	public AccessModifier getUnderlyingDefAccessMod(){
		return null == origonatingFuncDef? AccessModifier.PUBLIC : origonatingFuncDef.accessModifier;//JPT: super lazy, remove this
	}
	
	public void setInputs(ArrayList<Type> accInputs) {
		/*if(accInputs == null) {
			this.inputs = null;
		}else {
			this.inputs = new ArrayList<Type>(accInputs.size());
			for(Type tt: accInputs) {
				this.inputs.add(TypeCheckUtils.boxTypeIfPrimative(tt, false));
			}
		}*/
		this.inputs = accInputs;
		/*
		if(accInputs !=null ){
			this.inputs = new ArrayList<Type>();
			for(Type i : accInputs){
				this.inputs.add(TypeCheckUtils.boxTypeIfPrimative(i));
			}
		}*/
	}
	
	@Override
	public FuncType copyTypeSpecific() {
		FuncType ret = new FuncType(this.line, this.column);
		Type tt = this.getTaggedType();
		if(null != tt)
		{
			if(tt == this) {
				ret.setTaggedType(ret);
			}else {
				ret.setTaggedType((Type)tt.copy());
			}
		}
		if(null != inputs)
		{
			ArrayList<Type> inputsCopy = new ArrayList<Type>();
			for(Type t : inputs)
			{
				inputsCopy.add(t==null?null:(Type)t.copy());
			}
			ret.inputs = inputsCopy;
		}
		if(null != this.retType){
			ret.retType = (Type)this.retType.copy();
		}
		
		ret.isAbstarct = isAbstarct;
		ret.nameRedirect = nameRedirect;
		ret.isFinal = isFinal;
		ret.isAutoGen = isAutoGen;
		ret.isConcurnasCallable = isConcurnasCallable;
		ret.hasBeenInputsGenericTypeQualified = hasBeenInputsGenericTypeQualified;
		ret.origonatingFuncDef = origonatingFuncDef;
		ret.arrayLevels = arrayLevels;
		ret.lambdaDetails = lambdaDetails;
		ret.origonalGenericTypeUpperBound = origonalGenericTypeUpperBound;
		ret.realReturnType = realReturnType;
		ret.definedLocation = definedLocation;
		ret.isImplicit = isImplicit;
		ret.inout = inout;
		ret.usedInMethodDesc = usedInMethodDesc;
		ret.isLHSClass = isLHSClass;
		ret.isClassRefType = isClassRefType;
		ret.localGenerics = (ArrayList<GenericType>) Utils.cloneArrayList(localGenerics);
		ret.previousFuncTypeIfCopy = this;
		ret.extFuncOn = extFuncOn;
		ret.hasBeenVectorized = hasBeenVectorized;
		ret.anonLambdaSources = anonLambdaSources;
		ret.implementSAM = implementSAM;
		ret.iIsTypeInFuncref = iIsTypeInFuncref;
		ret.signatureExpectedToChange = signatureExpectedToChange;
		ret.localGenBindingLast = localGenBindingLast;//clone?
		ret.setVectorized(this.getVectorized());
		ret.nullStatus = this.nullStatus;
		
		if(null != gensErased){
			ret.gensErased = (FuncType)gensErased.copy();
		}
		ret.origin = origin;
		ret.isPartOfVarargArray = isPartOfVarargArray;
		super.cloneMeTo(ret);
		return ret;
	}
	
	@Override
	public FuncType copyIgnoreReturnTypeAndGenerics() {
		FuncType ret = this.copyTypeSpecific();
		ret.realReturnType=null;
		ret.retType = null;
		ret.inputs = new ArrayList<Type>(ret.inputs.stream().map(a -> a.copyIgnoreReturnTypeAndGenerics()).collect(Collectors.toList()));
		return ret;
	}
	
	@Override
	public FuncType copyIgnoreReturnType() {
		FuncType ret = this.copyTypeSpecific();
		ret.realReturnType=null;
		ret.retType = null;
		ret.inputs = new ArrayList<Type>(ret.inputs.stream().map(a -> a==null?null:a.copyIgnoreReturnType()).collect(Collectors.toList()));
		return ret;
	}
	
	public boolean isFinal() {
		return isFinal;
	}
	
	public void setFinal(boolean isFinal) {
		this.isFinal = isFinal;
	}


	public boolean isAbstarct() {
		return isAbstarct;
	}

	public void setAbstarct(boolean isAbstarct) {
		this.isAbstarct = isAbstarct;
	}
	
	public int argCount()
	{
		if(this.inputs!=null) {
			return this.inputs.size();
		}
		return 0;
	}

	public boolean hasGenericArgument()
	{
		if(null != this.inputs)
		{
			for(Type arg: this.inputs)
			{
				if(arg instanceof GenericType || arg instanceof FuncType)
				{
					return true;
				}
				else
				{
					Type fiddleOutTheGenerics = GenericTypeUtils.mapFuncTypeEraseEnerics(arg);
					if(!arg.equals(fiddleOutTheGenerics))
					{
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public void makeConcurnasNonCallable()
	{
		this.isConcurnasCallable = false;
	}
	
	public FuncType()
	{
		this(0,0);
	}
	
	public FuncType(int line, int col)
	{
		super(line, col);
		this.inputs = new ArrayList<Type>(0); 
	}
	
	public FuncType(ArrayList<Type> inputs, Type retType) {
		this(0,0, inputs, retType );
	}
	
	public FuncType(ArrayList<Type> inputs) {
		this(0,0, inputs, null );
	}
	
	public FuncType(Type retType) {
		this(new ArrayList<Type>(), retType );//just return lol
	}
	
	public FuncType(int line, int col, ArrayList<Type> inputs, Type retType) {
		this(line, col);
		setInputs(inputs);
		//this.retType = TypeCheckUtils.boxTypeIfPrimative(retType, false);
		this.retType = retType;
		
		/*if(null != retType){
			retType.setInOutGenModifier(InoutGenericModifier.OUT);
		}*/
		
	}

	@SuppressWarnings("unchecked")
	public FuncType clone()
	{//JPT: shouldn't this use the copy function?
		FuncType cl = new FuncType(super.line, super.column, inputs==null?null:(ArrayList<Type>)inputs.clone(), retType);
		cl.origonatingFuncDef = this.origonatingFuncDef;
		cl.lambdaDetails = this.lambdaDetails;
		cl.arrayLevels = this.arrayLevels;
		cl.origonalGenericTypeUpperBound = this.origonalGenericTypeUpperBound;
		cl.realReturnType = this.realReturnType;
		cl.localGenerics = (ArrayList<GenericType>) Utils.cloneArrayList(localGenerics);
		cl.isClassRefType = isClassRefType;
		cl.anonLambdaSources = anonLambdaSources;
		cl.signatureExpectedToChange = signatureExpectedToChange;
		super.cloneMeTo(cl);
		return cl;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	@Override
	public String getPrettyName() {
		//TODO: implement func type printer
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		boolean a = false;
		for(Type input: this.inputs)
		{
			sb.append(input.getPrettyName());
			sb.append(',');
			a=true;
		}
		if(a) sb.deleteCharAt(sb.length()-1);
		sb.append(") ");
		sb.append(this.retType != null ? this.retType.getPrettyName(): "");

		return sb.toString();
	}
	
	@Override 
	public int hashCode()
	{
		//System.err.println("hc for: " + this + " ===> " + getFormatStringWithPalceholderforMethName(true));
		return getFormatStringWithPalceholderforMethName(true, true).hashCode();
		//return this.inputs.size();
	}
	
	@Override 
	public boolean equals(Object compare)
	{
		if(compare == this){
			return true;
		}
		
		if(compare instanceof FuncType)
		{
			FuncType compy = (FuncType)compare;
			if(this.arrayLevels == compy.arrayLevels && TypeCheckUtils.isArgumentListSame(this.inputs, compy.inputs))
			{
				if(compy.retType == null)
				{
					if(this.retType == null)
					{
						return true;
					}
					return false;
				}
				else if(this.retType == null)
				{
					return false;
				}
				return compy.retType.equals(this.retType) 
						&& super.getVectorized() == compy.getVectorized();
				/*return TypeCheckUtils.boxTypeIfPrimative(compy.retType, false).equals(TypeCheckUtils.boxTypeIfPrimative(this.retType, false) ) 
						&& super.getVectorized() == compy.getVectorized();*/
			}
		}
		return false;
	}
	
	public boolean equalsIngoreReturn(Object compare)
	{
		if(compare instanceof FuncType)
		{
			FuncType compy = (FuncType)compare;
			return TypeCheckUtils.isArgumentListSame(this.inputs, compy.inputs);
		}
		return false;
	}

	@Override
	public String toString()
	{
		boolean ifNullable = this.getNullStatus() == NullStatus.NULLABLE;
		
		StringBuilder sb = new StringBuilder();
		
		if(ifNullable) {
			sb.append("(");
		}
		sb.append(String.format(getFormatStringWithPalceholderforMethName(false, false), ""));
		sb.append(isVectorizedToString());
		
		if(ifNullable) {
			sb.append(")?");
		}
		
		return sb.toString();
	}
	
	public String getFormatStringWithPalceholderforMethName( )
	{
		return getFormatStringWithPalceholderforMethName(false, false);
	}
	
	public String getFormatStringWithPalceholderforMethName(boolean ignoreInOUtModi, boolean ignoreLocalGenTypeName)
		{
		String fname = "def";
		if(this.origonatingFuncDef != null) {
			if(this.origonatingFuncDef.isGPUKernalFuncOrStub()){
				fname = this.origonatingFuncDef.kernelDim != null?"gpukernel":"gpudef";
			}
		}
		
		StringBuilder sb = new StringBuilder(fname + " %s");
		
		if(this.localGenerics != null && !localGenerics.isEmpty()){
			sb.append("<");
			for(int n = 0; n < this.localGenerics.size(); n++){
				GenericType localGen = this.localGenerics.get(n);
				if(ignoreLocalGenTypeName) {
					sb.append(localGen.toStringOptName(false));
				}else {
					sb.append(localGen);
				}
				
				if(n != this.localGenerics.size()-1)
					sb.append(", ");
			}
			sb.append(">");
		}
		
		sb.append('(');
		if(this.isClassRefType){
			sb.append('*');
		}else{
			if(null != this.inputs)
			{
				for(int n = 0; n < this.inputs.size(); n++)
				{
					Type item = this.inputs.get(n);
					if(ignoreInOUtModi && null != item){
						item = (Type)item.copy();
						item.setInOutGenModifier(null);
						item.setNullStatus(NullStatus.NOTNULL);
					}
					
					sb.append(item);
					if(n != this.inputs.size()-1)
						sb.append(", ");
				}
				
			}
		}
		
		sb.append(')');
		sb.append(' ');
		
		Type retType = this.retType;
		if(null != retType){
			if(ignoreInOUtModi){
				retType = (Type)retType.copy();
				retType.setInOutGenModifier(null);
				retType.setNullStatus(NullStatus.NOTNULL);
			}
			
			sb.append(retType);
		}else{
			sb.append("- missing return type");
		}
		
		if(this.arrayLevels >0)
		{
			sb = new StringBuilder("(" + sb + ")");
			
			if(this.arrayLevels == 1)
			{
				sb.append("[]");//TODO: show this as xxx[1] not xxx[]
			}
			else
			{
				sb.append("[" + this.arrayLevels + "]");
			}
		}
		
		return sb.toString();
	}
	
	@Override
	public boolean isChangeable() {
		// TODO isChangeable ???
		return true;
	}

	@Override
	public String getNonGenericPrettyName() {
		StringBuilder ret;
		if(this.isClassRefType) {
			ret = new StringBuilder("com/concurnas/bootstrap/lang/Lambda$ClassRef");
		}else {
			ret = new StringBuilder(("com/concurnas/bootstrap/lang/Lambda$Function"+inputs.size() + ( this.retType.equals(ScopeAndTypeChecker.const_void)?"v":"")));
		}
		
		//StringBuilder ret = new StringBuilder(getBytecodeSlashName().substring(1));
		return ret.toString();
	}


	public FuncType getErasedFuncTypeNoRet() {
		FuncType ret = getErasedFuncType();
		return ret.copyIgnoreReturnType();
	}
	
	public FuncType getErasedFuncType()
	{
		if(this.gensErased == null)
		{
			gensErased = (FuncType) GenericTypeUtils.mapFuncTypeEraseEnerics(this);
		}
		
		

		if(null != gensErased.origonatingFuncDef && gensErased.origonatingFuncDef.isGPUKernalOrFunction != null) {
			//myfunc(global a int) == myfunc(global a int) <> myfunc(global in a int)
			
			ArrayList<FuncParam> fparams = gensErased.origonatingFuncDef.params.params;
			
			ArrayList<Type> newtypes = new ArrayList<Type>(inputs.size());
			int cnt = 0;
			for(Type ttx : inputs) {
				if(fparams.get(cnt).gpuVarQualifier != null && !TypeCheckUtils.isGPUBuffer(ttx)) {
					Class<?> toUse;
					if(fparams.get(cnt).gpuVarQualifier == GPUVarQualifier.LOCAL) {
						toUse = GPUBufferLocal.class;
					}else {
						toUse = GPUBuffer.class;
						GPUInOutFuncParamModifier inoutmodifier = fparams.get(cnt).gpuInOutFuncParamModifier;
						if(inoutmodifier == GPUInOutFuncParamModifier.in) {
							toUse = GPUBufferInput.class;
						}else if(inoutmodifier == GPUInOutFuncParamModifier.out) {
							toUse = GPUBufferOutput.class;
						}
					}
					
					NamedType newtype = new NamedType(new ClassDefJava(toUse));
					
					ArrayList<Type> gens = new ArrayList<Type>();
					gens.add(ScopeAndTypeChecker.const_object);
					newtype.setGenTypes(TypeCheckUtils.boxTypeIfPrimative(gens, false));
					newtypes.add(newtype);
				}else {
					newtypes.add(ttx);
				}
				cnt++;
			}
			gensErased.inputs=newtypes;
		}
		
		return gensErased;
		
	}
	
	public boolean isSameWithGenericsErased(FuncType as)
	{
		if(this.equals(as))
		{
			return true;
		}
		else
		{
			gensErased = getErasedFuncType();
			FuncType asErased = as.getErasedFuncType();
			return gensErased.toString().equals(asErased.toString());
		}
	}

	public static final String classRefIfacePrefix = "x";
	public static final String classRefIfacePostfix = "$ClassRefIface";
	public static final int classRefIfacePostfixLength = classRefIfacePostfix.length();
	
	/*private String getBytecodeSlashName(){
		if(this.isClassRefType){
			NamedType asNAmed = (NamedType)this.retType;
			String ret = asNAmed.getBytecodeType();
			String xxx = "L" + FuncType.classRefIfacePrefix + ret.substring(1, ret.length() - 1)+classRefIfacePostfix;
			return xxx;
		}else{
			return "Lcom/concurnas/bootstrap/lang/Lambda$Function"+inputs.size();
		}
	}*/
	
	
	@Override
	public String getBytecodeTypeWithoutArray( ) {
		
		if(origonalGenericTypeUpperBound != null)
		{
			return origonalGenericTypeUpperBound.getSetClassDef().javaClassName();
		}
		
		//fun (int,  int) void => Function<2>
		//StringBuilder ret = new StringBuilder(this.isClassRefType?"Lcom/concurnas/bootstrap/lang/Lambda$ClassRef":("Lcom/concurnas/bootstrap/lang/Lambda$Function"+inputs.size()));
		
		//nameSlash.endsWith(FuncType.classRefIfacePostfix) && nameSlash.startsWith(FuncType.classRefIfacePrefix)){
		
		StringBuilder ret;
		if(this.isClassRefType){
			if(this.usedInMethodDesc ){
				ret = new StringBuilder("Lcom/concurnas/bootstrap/lang/Lambda$ClassRef");
			}else{
				String retbctype;
				NamedType retNamed = ((NamedType)this.retType);
				ClassDef cd = retNamed.getSetClassDef();
				if(cd.equals( ScopeAndTypeChecker.const_typed_actor_class)){
					retbctype = retNamed.getGenericTypeElements().get(0).getBytecodeType();
					retbctype = retbctype.substring(1, retbctype.length()-1) + "$$ActorIterface";
				}
				else if(cd.isActor && null != cd.typedActorOn){
					retbctype = cd.typedActorOn.getBytecodeType();
					retbctype = retbctype.substring(1, retbctype.length()-1) + "$$ActorIterface";
				}else{
					retbctype = this.retType.getBytecodeType();
					retbctype = retbctype.substring(1, retbctype.length()-1);
				}
				
				ret = new StringBuilder();
				ret.append("L" + FuncType.classRefIfacePrefix + retbctype);
				ret.append(FuncType.classRefIfacePostfix);
			}
		}else{
			ret = new StringBuilder("Lcom/concurnas/bootstrap/lang/Lambda$Function"+inputs.size() + ( this.retType.equals(ScopeAndTypeChecker.const_void)?"v":"") );
		}
		
		//StringBuilder ret = new StringBuilder(getBytecodeSlashName());
		ret.append(';');
		return ret.toString();
	}

	@Override
	public String getGenericBytecodeTypeWithoutArray() {
		if(origonalGenericTypeUpperBound != null)
		{//TODO: compared to NamedType version this looks wrong
			return origonalGenericTypeUpperBound.getSetClassDef().javaClassName();
		}
		
		StringBuilder ret = new StringBuilder();
		//ret.append(this.isClassRefType?"Lcom/concurnas/bootstrap/lang/Lambda$ClassRef":("Lcom/concurnas/bootstrap/lang/Lambda$Function" + inputs.size()));
		
		
		if(this.isClassRefType){
			ret = new StringBuilder("L" + FuncType.classRefIfacePrefix);
			String retbctype = this.retType.getBytecodeType();
			retbctype = retbctype.substring(1, retbctype.length()-1);
			ret.append(retbctype);
			ret.append(FuncType.classRefIfacePostfix);
			
		}else{
			boolean retVoid = this.retType!=null&&this.retType.equals(ScopeAndTypeChecker.const_void);
			
			if(retVoid) {
				int sz = inputs.size();
				ret = new StringBuilder("Lcom/concurnas/bootstrap/lang/Lambda$Function"+sz + "v");
				
				if(sz > 0) {
					ret.append('<');
					
					if(!this.isClassRefType){
						for(Type t : inputs){
							ret.append(TypeCheckUtils.boxTypeIfPrimative(t, false).getGenericBytecodeType());
						}
					}
					ret.append('>');
				}
			}else {
				ret = new StringBuilder("Lcom/concurnas/bootstrap/lang/Lambda$Function"+inputs.size());
				
				ret.append('<');
				
				if(!this.isClassRefType){
					for(Type t : inputs)
					{
						ret.append(TypeCheckUtils.boxTypeIfPrimative(t, false).getGenericBytecodeType());
					}
				}
				
				Type boxed = TypeCheckUtils.boxTypeIfPrimative(retType, false);
				ret.append(boxed == null ? ScopeAndTypeChecker.const_object.getGenericBytecodeType() : boxed.getGenericBytecodeType()  );
			
				ret.append('>');
			}
			
			
			
		}
		
		
		
		
		
		
		ret.append(';');
		return ret.toString();
	}

	public TypeAndLocation getLambdaDetails() {
		return this.lambdaDetails;
	}
	
	public void setLambdaDetails(TypeAndLocation lambdaDetails) {
		//System.err.println("Set lambda dets: [" + this + "]: " + lambdaDetails);	
		this.lambdaDetails = lambdaDetails;
	}


	public NamedType getPoshObjectStyleName()  {
		try{
			Class<?> myClass = ClassLoader.getSystemClassLoader().loadClass(this.isClassRefType?"com.concurnas.bootstrap.lang.Lambda$ClassRef":("com.concurnas.bootstrap.lang.Lambda$Function" + this.argCount() + ( this.retType.equals(ScopeAndTypeChecker.const_void)?"v":"") ));
			//Class<?> myClass = ClassLoader.getSystemClassLoader().loadClass(getBytecodeSlashName().replace('/',  '.').substring(1));
			return new NamedType(new ClassDefJava(myClass));
		}
		catch(Exception e){
			return null;
		}
       
	}
	
	@Override
	public String getJavaSourceType() {
		return "LambdaTypeTODO";
	}
	
	@Override
	public String getJavaSourceTypeNoArray() {
		return "LambdaTypeTODO";
	}
	
	@Override
	public String getCheckCastType()
	{
		String ret = this.getNonGenericPrettyName().replace('.', '/');
		if(this.hasArrayLevels())
		{
			StringBuilder prePendArLevels = new StringBuilder();
			if(this.arrayLevels > 0){
				for(int n =0; n < this.arrayLevels; n++){
					prePendArLevels.append("[");
				}
				prePendArLevels.append("L");
			}
			
			prePendArLevels.append(ret);
			ret = prePendArLevels.toString()+";";
		}
		
		return ret;
	}


	@Override
	public int compareTo(FuncType o) {
		return this.toString().compareTo(o.toString());
	}
	
	private Type type;
	public Type setTaggedType(Type type){
		this.type = type;
		return type;
	}
	
	public Type getTaggedType(){
		return type==null?this:type;
	}
	

	public ArrayList<AnonLambdaDefOrLambdaDef> anonLambdaSources;//used to reference source of functpye if its from an anon lambda def
	public Pair<NamedType, TypeAndLocation> implementSAM;
	public Map<Type, Type> localGenBindingLast;
	

	
	
	public ArrayList<GenericType> getLocalGenerics() {
		return localGenerics;
	}

	public void setLocalGenerics(ArrayList<GenericType> localGenerics) {
		this.localGenerics = localGenerics;
	}
	
	
	
}