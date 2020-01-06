package com.concurnas.compiler.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.New;
import com.concurnas.compiler.ast.ObjectProviderLineDepToExpr;
import com.concurnas.compiler.ast.ObjectProviderLineProvide;
import com.concurnas.compiler.ast.Type;
import com.concurnas.runtime.Pair;

/*
creates defualt actor for objects which are created as actor but are not already an actor class

my = actor MyClass(12)

=>

will create defaultActor for MyClass

 */
public class DefaultActorGennerator extends AbstractErrorRaiseVisitor {

	//private boolean hadMadeRepoints=false;

	public DefaultActorGennerator(String fullPathFileName) {
		super(fullPathFileName);
	}
	
	public void doDefaultActorCreation(Block input){
		this.visit(input);
		
		/*if(!toAdd.isEmpty()){
			input.addAllClasses(toAdd);
			toAdd.clear();
		}*/
	}
	public boolean changeMade = false;
	public void resetRepoints() {
		changeMade = false;
	}
	
	private HashSet<String> alreadyAdded = new HashSet<String>();
	
	
	/*private List<Tuple<String, TypeAndLocation>> agumentWithObjMethods(List<Tuple<String, TypeAndLocation>> from){
		//add toString etc if missing from definition set
		return from;
	}*/
	
/*	private static class PrintSourceVisitorShortLocalName extends PrintSourceVisitor{
		@Override
		public Object visit(NamedType namedType) {
			return super.visit(namedType);
		}
	}*/
	
	private String getTypeString(PrintSourceVisitor psv, Type tt, boolean gensAsUpper){
		//boolean prev = psv.showGenericsAsUpperBound;
		psv.showGenericsAsUpperBound=gensAsUpper;
		tt.accept(psv);
		String got = psv.toString();
		psv.reset();
		return got;
	}
	
/*	@Override
	public Object visit(NamedConstructor namedConstructor) {
		String name = namedConstructor.defaultActorName;
		if(name != null && !alreadyAdded.contains(name)){
			String fullname = namedConstructor.defaultActorNameFull;
			PrintSourceVisitor psv = new PrintSourceVisitor();

			String actingOnTypeHeader = getTypeString(psv, namedConstructor.actingOn, false);
			String actingOnType = getTypeString(psv, namedConstructor.actingOn, true);
			
			//String fields = "";
			StringBuilder methods = new StringBuilder("");
			
			//ClassDef actorParent = namedConstructor.defaultActorOrigType.getSetClassDef().getSuperclass();
			
			for(Tuple<String, TypeAndLocation> strTal : namedConstructor.actingOn.getAllMethods()){
				FuncType ft = (FuncType)strTal.getB().getType();
				
				if(ft.origonatingFuncDef.accessModifier != AccessModifier.PUBLIC){//only cover the public ones
					continue;
				}

				String methodName = strTal.getA();
				Type ret = ft.retType;
				int argCount = ft.inputs.size();
				
				if(methodName.equals("equals") && ScopeAndTypeChecker.const_boolean.equals(ret) && argCount == 1 && ft.inputs.get(0).equals(ScopeAndTypeChecker.const_object)){
					continue;//skip equals(a Object)boolean- this gets added especially below
				}
				
				boolean noReturn=false;
				if(ret.equals(ScopeAndTypeChecker.const_void)){
					ret = new NamedType(new ClassDefJava(Void.class));
					noReturn=true;
				}
				
				ArrayList<String> inputargs = new ArrayList<String>(argCount);
				ArrayList<String> inputTypes = new ArrayList<String>(argCount);
				int m=0;
				for(Type arg : ft.inputs){
					inputTypes.add(getTypeString(psv, arg, false));
					inputargs.add("a" + m++);
				}
				
				//all methods from class being acted on get $ActorCall postfix
				StringBuilder methodTitle = new StringBuilder("(");
				//and if they exist in the actor then an actor super caller too
				
				StringBuilder actingOnArgs = new StringBuilder();
				for(int nn=0; nn < argCount; nn++){
					methodTitle.append(inputargs.get(nn) + " " + inputTypes.get(nn));
					actingOnArgs.append(inputargs.get(nn));
					if(nn != argCount-1){
						methodTitle.append(", ");
						actingOnArgs.append(", ");
					}
				}
				methodTitle.append(")");
				
				
				HashSet<TypeAndLocation> itemz = ScopeAndTypeChecker.const_typed_actor_class.getFuncDef(methodName, true);
				boolean existsInParent = false;
				for(TypeAndLocation tal : itemz){
					FuncType candi = (FuncType)tal.getType();
					if(candi.equalsIngoreReturn(ft)){
						existsInParent = true;
						break;
					}
				}
				String methodTitleStr;
				if(existsInParent){//now create a $ActorSuperCall router
					//methods.append("def "+ methodName +"$ActorSuperCall"+methodTitle.toString() + String.format("= { super.%s(%s); }\n", methodName, actingOnArgs));
				}
				
				methodTitleStr = "def "+ methodName +"$ActorCall"+methodTitle.toString(); 
				
				if(methodTitleStr.equals("def getClass()")){
					continue;
				}
				
				boolean objMethod = notAsyncableAsFromObject.contains(methodTitleStr);
				
				String retType = getTypeString(psv, ret, false);
				
				String retla = noReturn?"":"ret";
				String retTypeHead = noReturn?"":retType;// + (objMethod?"":":");
				
				if(objMethod){
					methodTitleStr = "override" + methodTitleStr.substring(3);
				}
				//
				methods.append(String.format(methodTitleStr + " %s { ret %s:; super.addCall(2, new com.concurnas.lang.tuples.Tuple2(ret:, %s.%s&(%s))); return %s; }\n", retTypeHead, retType, actingOnType, methodName, actingOnArgs, retla));
			}
			
			methods.append("override toString() = { this.toString$ActorCall(); } ");
			methods.append("override hashCode() = { this.hashCode$ActorCall(); } ");
			methods.append("override equals(obj Object) = { this.equals$ActorCall(obj); } ");
			
			methods.append("public def equals$ActorCall(a0 Object) boolean {" +
					         "ret Boolean:;" +
					         String.format("super.addCall(2, new com.concurnas.lang.tuples.Tuple2<Boolean:, () boolean> ( ret:, %s.equals&((a0 as com.concurnas.lang.TypedActor<%s>).getActeeClone() if a0 is com.concurnas.lang.TypedActor<%s> else a0)));", actingOnType, actingOnType, actingOnType) +
					         "return ret;}");
			
			//constructors

			StringBuilder cons = new StringBuilder("");
			
			for(FuncType con : namedConstructor.actingOn.getAllConstructors()){
				//copy paste...
				int sz = con.inputs.size()+1;
				ArrayList<String> inputargs = new ArrayList<String>(sz);
				ArrayList<String> inputTypes = new ArrayList<String>(sz);
				
				inputargs.add(ScopeAndTypeChecker.TypesForActor);
				inputTypes.add("Class<?>[]");
				int firstmrk=0;
				
				NamedType parentNestor = namedConstructor.actingOn.getparentNestorFakeNamedType();
				if(null != parentNestor){
					inputargs.add("parnest");
					inputTypes.add(getTypeString(psv, parentNestor, false));
					sz++;
					firstmrk++;//not include in acting on
				}
				
				int m=0;
				for(Type arg : con.inputs){
					inputTypes.add(getTypeString(psv, arg, false));
					inputargs.add("a" + m++);
				}
				
				StringBuilder conInputArgs = new StringBuilder();
				StringBuilder actingOnArgs = new StringBuilder();
				for(int nn=0; nn < sz; nn++){
					boolean isnotFirst = nn > firstmrk;
					conInputArgs.append(inputargs.get(nn) + " " + inputTypes.get(nn));
					if(isnotFirst){
						actingOnArgs.append(inputargs.get(nn));
					}
					if(nn != sz-1){
						conInputArgs.append(", ");
						if(isnotFirst){
							actingOnArgs.append(", ");
						}
					}
				}
				
				String leRef = String.format("%s&(%s)", actingOnTypeHeader, actingOnArgs);
				if(null != parentNestor){
					leRef = String.format("{ x = %s; x.bind(parnest); x; }", leRef );
				}
				
				cons.append(String.format("this(%s){super(%s, %s); }", conInputArgs, ScopeAndTypeChecker.TypesForActor, leRef) );
			}
			
			cons.append("this( types Class<?>[], a bytecodeSandbox.Outer<X>) {" +
					     " super(types, bytecodeSandbox.Outer<X>.Inner<Y>&(a));" +
					     " }");
			
			
			//make unbounded func ref retuning result to variable, that ends up being  returned.
			//create local, and return it
			String code = String.format("actor %s extends com.concurnas.lang.TypedActor<%s>{\n%s\n%s}", fullname, actingOnTypeHeader, cons, methods);
			
			ClassDef mainFunction = Utils.parseClass(code, "defaultActor", namedConstructor.getLine(), false);
			mainFunction.isGennerated=true;
			toAdd.add(new Tuple<Integer, ClassDef>(namedConstructor.getLine(), mainFunction));
			
			alreadyAdded.add(name);
			
			namedConstructor.defaultActorName=null;//dont need to create this a second time...
		}
	
		return null;
	}
	*/
	
	
	private Block currentBlock = null;
	@Override
	public Object visit(Block block) {
		LineHolder lh = block.startItr();
		Block prevBlock = currentBlock;
		currentBlock = block;
		
		while(lh != null)
		{
			lh.accept(this);
			lh = block.getNext();
		}
		
		addItems(block);
		
		currentBlock = prevBlock;
		
		return null;
	}
	
	private void addItems(Block block) {
		ArrayList<Pair<Integer, ClassDef>> items = toAdd.get(block);
		
		if(null != items && !items.isEmpty()){
			block.addAllClasses(items);
			items.clear();
		}
	}
	
	/*@Override
	public Object visit(ObjectProviderBlock objectProviderBlock) {
		//to support creation of actors within object providers
		Block prevBlock = currentBlock;
		currentBlock = objectProviderBlock.block;
		
		for(ObjectProviderLine opl : objectProviderBlock.lines) {
			opl.accept(this);
		}

		addItems(objectProviderBlock.block);
		
		currentBlock = prevBlock;
		
		return null;
	}*/
	
	
	
	
	
	@Override
	public Object visit(ObjectProviderLineDepToExpr objectProviderLineDepToExpr) {
		Block prevBlock = currentBlock;
		currentBlock = objectProviderLineDepToExpr.fulfilment;
		
		super.visit(objectProviderLineDepToExpr);

		addItems(objectProviderLineDepToExpr.fulfilment);
		
		currentBlock = prevBlock;
		
		return null;
	}
	@Override
	public Object visit(ObjectProviderLineProvide objectProviderLineProvide) {

		if(objectProviderLineProvide.provideExpr == null) {
			super.visit(objectProviderLineProvide);
		}else {

			Block prevBlock = currentBlock;
			currentBlock = objectProviderLineProvide.provideExpr;
			
			super.visit(objectProviderLineProvide);

			addItems(objectProviderLineProvide.provideExpr);
			
			currentBlock = prevBlock;
		}
		
		return null;
	}
	
	
	
	
	
	

	private HashMap<Block, ArrayList<Pair<Integer, ClassDef>>> toAdd = new HashMap<Block, ArrayList<Pair<Integer, ClassDef>>>();
	
	private static final PrintSourceVisitor psv = new PrintSourceVisitor();
	static{
		psv.showGenericInOut = false;
	}
	
	@Override
	public Object visit(New namedConstructor) {
		String name = namedConstructor.defaultActorName;
		if(name != null && !alreadyAdded.contains(name)){
			String fullname = namedConstructor.defaultActorNameFull;
			
			String actingOnTypeHeader = getTypeString(psv, namedConstructor.actingOn, false);
			
			String code = String.format("actor %s of %s{}", fullname, actingOnTypeHeader);
			
			ClassDef defaultActorClass = Utils.parseClass(code, "defaultActor", namedConstructor.getLine(), false);
			defaultActorClass.isGennerated=true;
			defaultActorClass.permitGenericInterOfTypedArg = false;
			
			if(null != namedConstructor.actorOfClassRef){
				defaultActorClass.isActorOfClassRef = namedConstructor.actorOfClassRef;//.copyTypeSpecific();
			}
			
			if(!toAdd.containsKey(currentBlock)){
				toAdd.put(currentBlock, new ArrayList<Pair<Integer, ClassDef>>());
			}
			toAdd.get(currentBlock).add(new Pair<Integer, ClassDef>(namedConstructor.getLine(), defaultActorClass));
			
			alreadyAdded.add(name);
			changeMade=true;
			namedConstructor.defaultActorName=null;//dont need to create this a second time...
		}
		
		
		//functons
		//constructors
		
	
		return null;
	}
	

	//these cannot be called asynchronouisly as that would change the return type and invalidate the return type contract
	public final static HashSet<String> notAsyncableAsFromObject = new HashSet<String>();
	{//there are others like wait, but these are not called
		notAsyncableAsFromObject.add("def getClass()");
		notAsyncableAsFromObject.add("def toString()");
		notAsyncableAsFromObject.add("def hashCode()");
		notAsyncableAsFromObject.add("def equals(a0 java.lang.Object)");
	}
}
