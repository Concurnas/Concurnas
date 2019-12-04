package com.concurnas.compiler.visitors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import com.concurnas.compiler.ast.AnonLambdaDef;
import com.concurnas.compiler.ast.ArrayDef;
import com.concurnas.compiler.ast.ArrayRef;
import com.concurnas.compiler.ast.ArrayRefElement;
import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.CatchBlocks;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.DotOperator;
import com.concurnas.compiler.ast.ElifUnit;
import com.concurnas.compiler.ast.ForBlock;
import com.concurnas.compiler.ast.ForBlockOld;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.FuncInvoke;
import com.concurnas.compiler.ast.FuncParam;
import com.concurnas.compiler.ast.FuncRef;
import com.concurnas.compiler.ast.FuncRefArgs;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.GenericType;
import com.concurnas.compiler.ast.IfStatement;
import com.concurnas.compiler.ast.LambdaDef;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.New;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.ReturnStatement;
import com.concurnas.compiler.ast.TryCatch;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.WhileBlock;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.GenericTypeUtils;
import com.concurnas.runtime.Pair;

public class GenericTypeInferencer extends AbstractVisitor {
	
	private ScopeAndTypeChecker satc;
	private ErrorRaiseable ers;
	private boolean madeChanges = false;
	
	public GenericTypeInferencer(ScopeAndTypeChecker satc) {
		this.satc = satc;
		this.ers = satc.getErrorRaiseableSupression();
	}
	
	//private static abstract class Qualifier{
		
	//}
	private interface NeedsQualification{
		public void addQualifiedBy(Type qualifier);
		public void addQualifiedBy(NeedsQualification qualifier);
		public Set<UnqualifiedGenericType> getUnderlingUnqualifieds();
		public void setPartialBinding(Map<Type, Type> fromGenToThing);
		public void setSinglePartialBinding(Type from, Type to);
	}
	
	private abstract class AbstractNeedsQualification implements NeedsQualification{
		protected AbstractNeedsQualification(){
			addUnqualifiedGenericType(this);
		}
	}
	
	private class NeedsQualificationCollection extends AbstractNeedsQualification{
		public List<NeedsQualification> needsq;
		public boolean isArryOrList;
		public NeedsQualificationCollection (List<NeedsQualification> needsq, boolean isArryOrList){
			this.needsq = needsq;
			this.isArryOrList =isArryOrList;
		}
		
		@Override
		public void addQualifiedBy(Type qualifier) {
			if(isArryOrList) {
				if(qualifier.hasArrayLevels()) {
					int levels = qualifier.getArrayLevels();
					qualifier = (Type)qualifier.copy();
					qualifier.setArrayLevels(levels-1);
				}else if(TypeCheckUtils.isList(ers, qualifier, false)) {
					qualifier = ((NamedType)qualifier).getGenericTypeElements().get(0);
				}
			}
			
			Type qq = qualifier;
			
			needsq.forEach(a -> a.addQualifiedBy(qq));
		}
		
		@Override
		public void addQualifiedBy(NeedsQualification qualifier) {
			needsq.forEach(a -> a.addQualifiedBy(qualifier));
		}

		@Override
		public Set<UnqualifiedGenericType> getUnderlingUnqualifieds() {
			Set<UnqualifiedGenericType> ret = new HashSet<UnqualifiedGenericType>();
			needsq.forEach(a -> ret.addAll(a.getUnderlingUnqualifieds()));
			return ret;
		}

		@Override
		public void setPartialBinding(Map<Type, Type> fromGenToThing) {
			needsq.forEach(a -> a.setPartialBinding(fromGenToThing));
		}
		
		@Override
		public void setSinglePartialBinding(Type from, Type to) {
			needsq.forEach(a -> a.setSinglePartialBinding(from, to));
		}
	}
	

	private class RefNamePassThrough extends AbstractNeedsQualification{
		private NeedsQualification passto;
		private String name;
		
		public RefNamePassThrough(RefName lhs, NeedsQualification what) {
			this.name = lhs.name;
			this.passto = what;
		}
		
		@Override public String toString() {
			return name + "-> ned";
		}
		
		@Override
		public void addQualifiedBy(Type qualifier) {
			passto.addQualifiedBy(qualifier);
		}

		@Override
		public void addQualifiedBy(NeedsQualification qualifier) {
			passto.addQualifiedBy(qualifier);
		}
		

		@Override
		public Set<UnqualifiedGenericType> getUnderlingUnqualifieds() {
			Set<UnqualifiedGenericType> ret = new HashSet<UnqualifiedGenericType>();
			ret.addAll(passto.getUnderlingUnqualifieds());
			return ret;
		}

		@Override
		public void setPartialBinding(Map<Type, Type> fromGenToThing) {
			passto.setPartialBinding(fromGenToThing);
		}
		
		@Override
		public void setSinglePartialBinding(Type from, Type to) {
			passto.setSinglePartialBinding(from, to);
		}
	}
	
	private class UnqualifiedGenericType extends AbstractNeedsQualification{
		public NamedType type;
		public HashSet<Type> qualifiers = new HashSet<Type>();
		public HashSet<NeedsQualification> dependsOn = new HashSet<NeedsQualification>();
		public Map<Type, Set<Type>> partialBindings = new HashMap<Type, Set<Type>>();
		public FuncInvoke alsoTagFuncInvoke;
		
		public UnqualifiedGenericType (NamedType type){
			this.type = type;
		}
		
		@Override
		public void addQualifiedBy(Type qualifier) {
			if(null != qualifier) {
				qualifiers.add(qualifier);
			}
		}
		
		@Override
		public void addQualifiedBy(NeedsQualification dependa) {
			dependsOn.add(dependa);
		}
		

		@Override
		public Set<UnqualifiedGenericType> getUnderlingUnqualifieds() {
			Set<UnqualifiedGenericType> ret = new HashSet<UnqualifiedGenericType>();
			ret.add(this);
			return ret;
		}
		
		@Override
		public void setPartialBinding(Map<Type, Type> fromGenToThing) {
			for(Type key : fromGenToThing.keySet()) {
				Set<Type> addTo;
				if(partialBindings.containsKey(key)) {
					addTo = partialBindings.get(key);
				}else {
					addTo = new HashSet<Type>();
					partialBindings.put(key, addTo);
				}
				
				addTo.add(fromGenToThing.get(key));
			}
		}

		@Override
		public void setSinglePartialBinding(Type from, Type to) {
			Set<Type> addTo;
			if(partialBindings.containsKey(from)) {
				addTo = partialBindings.get(from);
			}else {
				addTo = new HashSet<Type>();
				partialBindings.put(from, addTo);
			}
			addTo.add(to);
		}
		
	}
	private Stack<HashSet<NeedsQualification>> unqualifiedens = new Stack<HashSet<NeedsQualification>>();
	
	private void addUnqualifiedGenericType(NeedsQualification oinsance) {
		unqualifiedens.peek().add(oinsance);
	}
	
	private List<HashSet<UnqualifiedGenericType>> findDepClusters(HashMap<UnqualifiedGenericType, HashSet<UnqualifiedGenericType>> fromToDepsOn) {
		
		List<HashSet<UnqualifiedGenericType>> loopGroups = new ArrayList<HashSet<UnqualifiedGenericType>>();
		
		List<UnqualifiedGenericType> startingSet = fromToDepsOn.keySet().stream().filter(a -> !a.dependsOn.isEmpty()).collect(Collectors.toList());
		for(UnqualifiedGenericType start : startingSet) {
			//if start in loopGroup already - skip
			if(loopGroups.stream().anyMatch(a -> a.contains(start))) {
				continue;
			}
			
			boolean isLoopGroup = false;
			HashSet<UnqualifiedGenericType> inGroup = new HashSet<UnqualifiedGenericType>();
			LinkedList<UnqualifiedGenericType> visitNext = new LinkedList<UnqualifiedGenericType>();
			//visitNext.add(start);
			
			inGroup.add(start);
			visitNext.addAll(fromToDepsOn.get(start));
			
			while(!visitNext.isEmpty()) {
				UnqualifiedGenericType item = visitNext.pop();
				
				if(item == start) {
					isLoopGroup = true;
					continue;
				}
				
				if(inGroup.contains(item)) {
					continue;
				}
				
				inGroup.add(item);
				visitNext.addAll(fromToDepsOn.get(item));
			}
			
			if(isLoopGroup && !inGroup.isEmpty()) {
				loopGroups.add(inGroup);
			}
		}
		
		return loopGroups;
		
	}
	
	private void qualifyUnqualifieds(HashSet<NeedsQualification> allNeds) {
		if(allNeds.isEmpty()) {
			return;
		}
		
		HashSet<UnqualifiedGenericType> allUnqualifieds = new HashSet<UnqualifiedGenericType>();
		
		allNeds.forEach(a -> allUnqualifieds.addAll(a.getUnderlingUnqualifieds()));
		
		//find all items which we need to do analysis for...
		HashSet<UnqualifiedGenericType> allItems = new HashSet<UnqualifiedGenericType>();
		LinkedList<UnqualifiedGenericType> toAnalyise = new LinkedList<UnqualifiedGenericType>(allUnqualifieds);
		
		while(!toAnalyise.isEmpty()) {
			UnqualifiedGenericType ned = toAnalyise.pop();
			if(allItems.contains(ned)) {
				continue;//visited already
			}
			
			allItems.add(ned);
			toAnalyise.addAll(ned.getUnderlingUnqualifieds());
		}
		
		//figure out depends on
		HashMap<UnqualifiedGenericType, HashSet<UnqualifiedGenericType>> fromToDepsOn = new HashMap<UnqualifiedGenericType, HashSet<UnqualifiedGenericType>>();

		
		for(UnqualifiedGenericType unquali : allItems) {
			fromToDepsOn.put( unquali, new HashSet<UnqualifiedGenericType>());
		}
		for(UnqualifiedGenericType unquali : allItems) {
			for(NeedsQualification depof : unquali.dependsOn) {
				fromToDepsOn.get(unquali).addAll(depof.getUnderlingUnqualifieds());
			}
		}
				
		//detect loops: stuff in the loop set - satify indivudals without looping relation
		List<HashSet<UnqualifiedGenericType>> clusters = findDepClusters(fromToDepsOn);//a = b; b=c; c=d; d=b;//c, b, d are an island: result of which will affect value of a
		
		//a -> b; b -> a //if there is this sort of loop and none are qualified then we ignore the UnqualifiedGeneric depends on relationship and use external things to set it
		
		int itemCount = allItems.size();
		HashSet<UnqualifiedGenericType> alreadyQualified = new HashSet<UnqualifiedGenericType>();
		while(alreadyQualified.size() < itemCount) {
			//see if we can get some work done:
			//find instances with no unqualifeid deps:
			LinkedList<UnqualifiedGenericType> analyseNext = new LinkedList<UnqualifiedGenericType>();
			
			for(UnqualifiedGenericType ned : fromToDepsOn.keySet()) {
				if(!alreadyQualified.contains(ned)) {
					HashSet<UnqualifiedGenericType> dependancies = fromToDepsOn.get(ned);
					if(dependancies.isEmpty()) {
						analyseNext.add(ned);
					}else {//if{
						//all dependancies qualified? - then u can add it...
						if(dependancies.stream().allMatch(a -> alreadyQualified.contains(a))) {
							analyseNext.add(ned);
						}
						
					}
				}
			}
			
			if(!analyseNext.isEmpty()) {//nothing found, try the fellas which are clustered together
				while(!analyseNext.isEmpty()) {
					UnqualifiedGenericType unquali = analyseNext.pop();
					
					ArrayList<Type> gensQuali = runQualificationAlgo(unquali);
					if(null != gensQuali) {
						NamedType toQuali = unquali.type;

						boolean changesMade = true;
						if(unquali.alsoTagFuncInvoke != null) {
							changesMade = tagFuncInvokeLocalGens(unquali.alsoTagFuncInvoke, toQuali, gensQuali);
						}
						
						toQuali.setGenTypesInfered(gensQuali);
						toQuali.requiresGenTypeInference=false;//tidy up enviroment!
						toQuali.errorOnPrevoiusGenTypeQualification = false;
						madeChanges = changesMade;
					}
					
					alreadyQualified.add(unquali);//dont try again even if we failed
				}
			}else if(!clusters.isEmpty()) {//resolves via custers?
				boolean anychange = false;
				for(HashSet<UnqualifiedGenericType> cluster : clusters) {
					HashSet<Type> allquals = new HashSet<Type>();
					Map<Type, Set<Type>> allPartialBindings = new HashMap<Type, Set<Type>>();
					for(UnqualifiedGenericType member : cluster) {
						allquals.addAll(member.qualifiers);
						allPartialBindings.putAll(member.partialBindings);
					}
					
					for(UnqualifiedGenericType unquali : cluster) {
						ArrayList<Type> gensQuali = runQualificationAlgo(unquali, allquals, allPartialBindings);
						if(null != gensQuali) {
							NamedType toQuali = unquali.type;
							
							boolean changesMade = true;
							if(unquali.alsoTagFuncInvoke != null) {
								changesMade = tagFuncInvokeLocalGens(unquali.alsoTagFuncInvoke, toQuali, gensQuali);
							}
							
							toQuali.setGenTypesInfered(gensQuali);
							toQuali.requiresGenTypeInference=false;//tidy up enviroment!
							madeChanges = true;
							madeChanges = changesMade;
						}
						alreadyQualified.add(unquali);//dont try again even if we failed
					}
				}
				
				if(!anychange) {
					break;//no changes, then break, give up
				}
			}else {
				break;//nothing found, give up?
			}
		}
	}
	

	private boolean tagFuncInvokeLocalGens(FuncInvoke alsoTagFuncInvoke, NamedType toQuali, ArrayList<Type> gensQuali) {
		List<Type> curGens = toQuali.getGenTypes();
		int sz = curGens.size();
		
		Map<Type, Type> genericToBinding = new HashMap<Type, Type>();
		for(int n=0; n < sz; n++) {
			Type from = curGens.get(n);
			Type to = gensQuali.get(n);
			genericToBinding.put(from, to);
		}
		
		FuncType ft = (FuncType)alsoTagFuncInvoke.resolvedFuncTypeAndLocation.getType();
		
		ArrayList<Type> gens = new ArrayList<Type>();
		for(GenericType gen : ft.getLocalGenerics()) {
			Type boundTo = genericToBinding.get(gen);
			if(boundTo == null) {
				if(ft.localGenBindingLast != null) {
					boundTo = ft.localGenBindingLast.get(gen);
					if(genericToBinding.containsKey(boundTo)) {
						boundTo = genericToBinding.get(boundTo);
					}
				}
				
				if(boundTo == null) {
					return false;
				}
			}
			gens.add(boundTo);
		}
		
		alsoTagFuncInvoke.genTypes = gens;
		
		if(!alsoTagFuncInvoke.args.isEmpty()) {
			for(Expression expr : alsoTagFuncInvoke.args.asnames) {
				if(expr instanceof AnonLambdaDef) {
					AnonLambdaDef asAnon = (AnonLambdaDef)expr;
					if(null != asAnon.astRedirect) {
						LambdaDef ld = asAnon.astRedirect;
						//ld.returnType
						Map<GenericType, Type> genericToBindingGens = new HashMap<GenericType, Type>();
						for(Type key : genericToBinding.keySet()) {
							if(key instanceof GenericType) {
								genericToBindingGens.put((GenericType)key, genericToBinding.get(key));
							}
						}
						
						ld.returnType = GenericTypeUtils.filterOutGenericTypes(ld.returnType, genericToBindingGens);
						for(FuncParam fp : ld.params.params) {
							fp.type = GenericTypeUtils.filterOutGenericTypes(fp.type, genericToBindingGens);
						}
						
					}
				}
			}
		}
		
		//if there are any anon lambdas declared within the method invokation color these as well
		
		
		
		
		return true;
	}

	public ArrayList<Type> runQualificationAlgo(UnqualifiedGenericType unquali){
		return runQualificationAlgo(unquali, null, null);
	}
	
	public ArrayList<Type> runQualificationAlgo(UnqualifiedGenericType unquali, HashSet<Type> qualifers, Map<Type, Set<Type>> partialBindings){
		 NamedType toQuali = unquali.type;
		//find most generic of the qualifiers
		//find match to generic type
		//color in the generics
		
		 if(null == qualifers) {
			 HashSet<Type> quals = unquali.qualifiers;
			 
			 //if there are any non type qualifiers, add them now..
			 if(!unquali.dependsOn.isEmpty()) {//add all dependencies which have been qualified
				 unquali.dependsOn.stream().forEach(a -> a.getUnderlingUnqualifieds().stream().filter(b -> !b.type.getGenTypes().isEmpty()).forEach(b -> quals.add(b.type)));
			 }
			 qualifers = quals;
		 }
		 
		 if(null == partialBindings) {
			 partialBindings = unquali.partialBindings;
		 }
		 
		
		
		if(!qualifers.isEmpty() || !partialBindings.isEmpty()) {
			Type mostGen;

			ClassDef classWanted = toQuali.getSetClassDef();
			
			HashSet<NamedType> matchingOnClass = new HashSet<NamedType>();
			for(Type potential : qualifers) {
				if(potential instanceof NamedType) {
					NamedType potentialNT = (NamedType)potential;
					ClassDef potCD = potentialNT.getSetClassDef();
					if(potCD != null) {
						if(potCD.equals(classWanted)) {
							matchingOnClass.add(potentialNT);
						}else {//check parents...
							List<NamedType> potsups = potCD.getAllSuperClassesInterfaces();
							for(NamedType tryit : potsups) {
								if(classWanted.equals(tryit.getSetClassDef())) {
									matchingOnClass.add(tryit);
									break;
								}
							}
						}
					}
				}
			}
			

			ArrayList<GenericType> gensWanted = classWanted.classGenricList;
			//convert partialBindigs to generic names in local space:
			if(!toQuali.fromClassGenericToQualifiedType.isEmpty()) {
				Map<Type, Set<Type>> newpartialBindings = new HashMap<Type, Set<Type>>();
				for(GenericType gen : gensWanted) {
					Type mapTo = toQuali.fromClassGenericToQualifiedType.get(gen);
					Set<Type> relbinding = partialBindings.get(mapTo);
					if(relbinding == null) {
						newpartialBindings = null;
						break;
					}
					newpartialBindings.put(gen, relbinding);
				}
				
				if(null != newpartialBindings) {
					partialBindings = newpartialBindings;
				}
			}
			
			
			
			Map<Type, Set<Type>> argBindsings = new HashMap<Type, Set<Type>>(partialBindings);
			for(GenericType gt : gensWanted) {
				if(!argBindsings.containsKey(gt)) {
					argBindsings.put(gt, new HashSet<Type>());
				}
			}
			
			int wantSize = gensWanted.size();
			for(NamedType matcher : matchingOnClass) {
				ArrayList<Type> gens = matcher.getGenericTypeElements();
				if(wantSize == gens.size()) {
					for(int n=0; n < wantSize; n++) {
						argBindsings.get(gensWanted.get(n)).add(gens.get(n));
					}
				}
			}

			
			int line = toQuali.getLine();
			int col = toQuali.getColumn();
			//not search through each element for a most general binding:
			ArrayList<Type> binding = new ArrayList<Type>(wantSize);
			for(GenericType want : gensWanted) {
				Set<Type> choices = argBindsings.get(want);
				if(choices.isEmpty()) {
					return null;
				}
				
				List<Type> quals = new ArrayList<Type>(choices);
				Map<Type, Pair<Integer,Integer>> offenders = new HashMap<Type, Pair<Integer,Integer>>();
				quals.forEach(a -> offenders.put(a, new Pair<Integer,Integer>(line, col)));
				
				mostGen = TypeCheckUtils.getMoreGeneric(this.ers, this.satc, line, col, quals, offenders, false);
				
				if(null == TypeCheckUtils.checkSubType(this.ers, want, mostGen)) {
					return null;
				}
				binding.add(TypeCheckUtils.boxTypeIfPrimative(mostGen, false));
			}
			
			return binding;
		}
		return null;
	}
	
	public boolean hadMadeRepoints() {
		return madeChanges;
	}
	
	private class LocalizedScopeFrame{
		private LocalizedScopeFrame parent;
		private HashMap<String, NeedsQualification> vars = new HashMap<String, NeedsQualification>();
		
		public LocalizedScopeFrame(LocalizedScopeFrame parent) {
			this.parent = parent;
		}
		
		public void addVar(String var, NeedsQualification ned) {
			vars.put(var, ned);
		}
		
		public NeedsQualification getVar(String var) {
			if(this.vars.containsKey(var)) {
				return vars.get(var);
			}
			
			if(parent != null) {
				return parent.getVar(var);
			}
			
			return null;
		}
	}
	
	private LocalizedScopeFrame currentScopeFrame = null;
	
	@Override
	public Object visit(Block block) {
		unqualifiedens.add(new HashSet<NeedsQualification>());
		LocalizedScopeFrame prevScopeFrame = currentScopeFrame;
		currentScopeFrame = new LocalizedScopeFrame(prevScopeFrame);
		///////
		LineHolder lh = block.startItr();
		Object ret=null;
		while(lh != null)
		{
			ret = lh.accept(this);
			lh = block.getNext();
		}	
		
		
		qualifyUnqualifieds(unqualifiedens.pop());
		
		currentScopeFrame = prevScopeFrame;
		
		return ret;
	}
	
	private final NewTypeVisitor newTypeVisitor = new NewTypeVisitor();
	private class NewTypeVisitor extends AbstractVisitor{
		@Override
		public Object visit(NamedType asNamed) {
			if(asNamed.requiresGenTypeInference && !asNamed.ignoreNonQualificationfGenerics) {
				if(asNamed.getSetClassDef() != null) {
					return new UnqualifiedGenericType(asNamed);
				}
			}
			return null;
		}
	}
	
	
	
	@Override
	public Object visit(AssignNew assignNew) {
		
		if(assignNew.expr!=null) {
			Object exprNed = assignNew.expr.accept(this);
			if(exprNed instanceof NeedsQualification) {
				this.currentScopeFrame.addVar(assignNew.name, new RefNamePassThrough(new RefName(assignNew.name) , (NeedsQualification)exprNed));
				
				if(assignNew.type!=null) {
					
					if(assignNew.type instanceof NamedType) {
						Object typeNed = assignNew.type.accept(newTypeVisitor);
						if(typeNed instanceof NeedsQualification) {
							((NeedsQualification)typeNed).addQualifiedBy((NeedsQualification)exprNed);
							//this.currentScopeFrame.addVar(assignNew.name, exprNed);
							return null;
						}
					}
					
					((NeedsQualification)exprNed).addQualifiedBy(assignNew.type.getTaggedType());
				}
			}
		}else {
			Object typeNed = assignNew.type.accept(newTypeVisitor);
			if(typeNed instanceof NeedsQualification) {
				this.currentScopeFrame.addVar(assignNew.name, new RefNamePassThrough(new RefName(assignNew.name) , (NeedsQualification)typeNed));
			}
		}
		
		return null;
	}
	
	
	
	@Override
	public Object visit(AssignExisting assignExisting) {
		
		if(assignExisting.isReallyNew) {
			
			Expression lhs = assignExisting.assignee;
			if(lhs instanceof RefName) {
				//Object lhsInst = lhs.accept(this);
				
				if(null != assignExisting.expr) {
					Object what = assignExisting.expr.accept(this);

					if(what instanceof NeedsQualification) {
						RefName lhsRefName = (RefName)lhs;
						RefNamePassThrough ret = new RefNamePassThrough(lhsRefName , (NeedsQualification)what);
						this.currentScopeFrame.addVar(lhsRefName.name, ret);
						return ret;
					}
				}
			}
		}else {
			Expression lhs = assignExisting.assignee;
			if(null != assignExisting.expr){
				if(lhs instanceof RefName ) {

					RefName lhsRefName = (RefName)lhs;
					
					NeedsQualification ned = this.currentScopeFrame.getVar(lhsRefName.name);
					if(ned != null) {
						Object what = assignExisting.expr.accept(this);

						if(what instanceof NeedsQualification) {
							ned.addQualifiedBy((NeedsQualification)what);
							((NeedsQualification)what).addQualifiedBy((NeedsQualification)ned);
						}else {
							Type tt = assignExisting.expr.getTaggedType();
							if(null != tt) {
								ned.addQualifiedBy(tt);
							}
						}
					}
				}else if(lhs instanceof ArrayRef) {
					//mm[12] = '12'
					//li[3] = "sith"

					ArrayRef arr = (ArrayRef)lhs;
					Object lhsItem = arr.expr.accept(this);
					if(lhsItem instanceof NeedsQualification) {
						ArrayList<Pair<Boolean, ArrayList<ArrayRefElement>>> allitems = arr.arrayLevelElements.getAll();
						if(allitems.size() == 1) {
							ArrayList<ArrayRefElement> alitems = allitems.get(0).getB();
							if(alitems.size() == 1) {
								ArrayRefElement are = alitems.get(0);
								if(null != are.mapOperationSignature) {
									Type key = are.e1.getTaggedType();
									Type value = assignExisting.expr.getTaggedType();
									
									FuncType ft = are.mapOperationSignature;
									((NeedsQualification)lhsItem).setSinglePartialBinding(ft.inputs.get(0), key);
									((NeedsQualification)lhsItem).setSinglePartialBinding(ft.inputs.get(1), value);
								}
							}
						}
					}
					
					
				}else {
					Type lhsType = lhs.getTaggedType();
					if(lhsType != null) {
						Object what = assignExisting.expr.accept(this);
						if(what instanceof NeedsQualification) {
							((NeedsQualification) what).addQualifiedBy(lhsType);
						}else if(lhsType instanceof GenericType){
							if(assignExisting.assignee instanceof DotOperator) {
								DotOperator dop = (DotOperator)assignExisting.assignee;
								ArrayList<Expression> elms = dop.getElements(this);
								if(elms.size() >=2) {
									Expression prev = elms.get(elms.size() - 2);
									if(prev instanceof RefName) {
										Object lhsWhate = this.visit((RefName)prev);
										if(lhsWhate instanceof NeedsQualification) {
											NeedsQualification needq = (NeedsQualification)lhsWhate;
											if(needq != null) {
												needq.setSinglePartialBinding(lhsType, assignExisting.expr.getTaggedType());
											}
										}
									}
								}
							}
						}
					}

					
				}
			}
		}
		
		return null;
	}
	
	@Override
	public Object visit(RefName refname) {
		String vname = refname.name;
		
		return this.currentScopeFrame.getVar(vname);
	}
	
	
	@SuppressWarnings("unchecked")
	private NeedsQualification collectBindables(List<Object> bindTos, boolean isArryOrList) {
		List<NeedsQualification> needsQualies = (List<NeedsQualification>)(Object)bindTos.stream().filter(a -> a instanceof NeedsQualification).collect(Collectors.toList());
		if(!needsQualies.isEmpty()) {
			return new NeedsQualificationCollection(needsQualies, isArryOrList);
		}
		return null;
	}
	
	@Override
	public Object visit(IfStatement ifStatement) {
		ifStatement.iftest.accept(this);
		
		List<Object> bindTos = new ArrayList<Object>();
		
		bindTos.add(ifStatement.ifblock.accept(this));
		for(ElifUnit u : ifStatement.elifunits)	{
			bindTos.add(u.accept(this));
		}
		
		if(ifStatement.elseb!=null)	{
			bindTos.add(ifStatement.elseb.accept(this));
		}
		
		return collectBindables(bindTos, false);
	}
	
	
	

	@Override
	public Object visit(ArrayDef arrayDef) {
		List<Object> bindTos = new ArrayList<Object>();
		for(Expression e : arrayDef.getArrayElements(this))
		{
			bindTos.add(e.accept(this));
		}
		return collectBindables(bindTos, true);
	}


	@Override
	public Object visit(TryCatch tryCatch) {
		//lastLineVisited=tryCatch.getLine();
		if(tryCatch.astRepoint!=null){
			return tryCatch.astRepoint.accept(this);
		}
		else{
			List<Object> bindTos = new ArrayList<Object>();
			
			bindTos.add(tryCatch.blockToTry.accept(this));
			for(CatchBlocks cat : tryCatch.cbs)
			{
				bindTos.add(cat.accept(this));
			}
			if(tryCatch.finalBlock != null)
			{
				bindTos.add(tryCatch.finalBlock.accept(this));
			}
			return collectBindables(bindTos, false);
		}
	}
	
	
	@Override
	public Object visit(ForBlock forBlock) {
		//lastLineVisited=forBlock.getLine();
		if(null!= forBlock.localVarType) forBlock.localVarType.accept(this);
		forBlock.expr.accept(this);
		
		List<Object> bindTos = new ArrayList<Object>();
		bindTos.add(forBlock.block.accept(this));
		
		if(forBlock.elseblock != null){
			bindTos.add(forBlock.elseblock.accept(this));
		}

		return collectBindables(bindTos, false);
	}

	@Override
	public Object visit(ForBlockOld forBlockOld) {
		//lastLineVisited=forBlockOld.getLine();
		if(null != forBlockOld.assignExpr) 
		{
			forBlockOld.assignExpr.accept(this);
		}
		else if(null != forBlockOld.assignName)
		{
			if( null != forBlockOld.assigType) forBlockOld.assigType.accept(this);
			if( null != forBlockOld.assigFrom) forBlockOld.assigFrom.accept(this);
		}
		if(null != forBlockOld.check) forBlockOld.check.accept(this);
		if(null != forBlockOld.postExpr) forBlockOld.postExpr.accept(this);
		

		List<Object> bindTos = new ArrayList<Object>();
		bindTos.add(forBlockOld.block.accept(this));
		
		if(forBlockOld.elseblock != null){
			bindTos.add(forBlockOld.elseblock.accept(this));
		}

		return collectBindables(bindTos, false);
	}

	@Override
	public Object visit(WhileBlock whileBlock) {
		//lastLineVisited=whileBlock.getLine();
		whileBlock.cond.accept(this);
		
		List<Object> bindTos = new ArrayList<Object>();
		
		bindTos.add(whileBlock.block.accept(this));
		
		if(whileBlock.elseblock != null){
			bindTos.add(whileBlock.elseblock.accept(this));
		}

		return collectBindables(bindTos, false);
	}
	
	private HashMap<Expression, NeedsQualification> dotOpElementPreceededByNed = new HashMap<Expression, NeedsQualification>();
	
	@Override
	public Object visit(DotOperator dotOperator) {
		//lastLineVisited=dotOperator.getLine();
		Object lastThing = null;
		for(Expression e: dotOperator.getElements(this))
		{
			if(lastThing instanceof NeedsQualification) {
				dotOpElementPreceededByNed.put(e, (NeedsQualification)lastThing);
				lastThing = e.accept(this);
				dotOpElementPreceededByNed.remove(e);
			}else {
				lastThing = e.accept(this);
			}
		}
		return lastThing;
	}
	
	@Override
	public Object visit(FuncInvoke funcInvoke) {
		TypeAndLocation tal = funcInvoke.resolvedFuncTypeAndLocation;
		if(null != tal) {
			Type tt = tal.getType();
			if(tt instanceof FuncType) {
				NeedsQualification calledOnLhsNed = dotOpElementPreceededByNed.get(funcInvoke);
				
				FuncType ft = (FuncType)tt;
				if(!ft.inputs.isEmpty()) {
					List<Expression> args = funcInvoke.args.getArgumentsWNPs();
					
					List<Type> argsQuali = null;
					if(calledOnLhsNed != null) {
						argsQuali = new ArrayList<Type>();
					}
					
					int sz = args.size();
					if(sz == ft.inputs.size()) {
						for(int n=0; n < sz; n++) {
							Type boundto = ft.inputs.get(n);
							Expression expr = args.get(n);
							
							Object what = null == expr?null:expr.accept(this);
							
							if(what instanceof NeedsQualification) {
								((NeedsQualification)what).addQualifiedBy(boundto);
							}
							
							if(calledOnLhsNed != null) {
								//may be ablt to qualify generic arguments here...!
								argsQuali.add(args.get(n).getTaggedType());
								//build up generic mapping like we do with local generics
							}
						}
						
						if(calledOnLhsNed != null) {
							FuncDef fd = ft.origonatingFuncDef;
							if(fd != null) {
								ft = fd.getFuncType();
							}
							
							Map<Type, Type> fromGenToThing = TypeCheckUtils.attemptGenericBinding(this.ers, ft.inputs, argsQuali);
							
							calledOnLhsNed.setPartialBinding(fromGenToThing);
						}
					}
				}
				
				if(funcInvoke.requiresGenTypeInference) {
					Object resolvesTo = ft.retType.accept(newTypeVisitor);
					if(resolvesTo instanceof UnqualifiedGenericType) {//should be, what if it's not?
						((UnqualifiedGenericType)resolvesTo).alsoTagFuncInvoke = funcInvoke;
					}
					return resolvesTo;
				}
			}
		}
		
		return null;
	}

	
	
	@Override
	public Object visit(FuncRef funcRef) {
		//lastLineVisited=funcRef.getLine();
		if(null != funcRef){
			if(null != funcRef.functo){
				funcRef.functo.accept(this);
			}
			
			
			FuncRefArgs args = funcRef.argsForNextCompCycle;
			if(null == args){
				args = funcRef.getArgsForScopeAndTypeCheck();
			}
			
			if(null != args && null != funcRef.typeOperatedOn && funcRef.typeOperatedOn.getType() instanceof FuncType) {
				FuncType ft = (FuncType)funcRef.typeOperatedOn.getType();
				if(!ft.inputs.isEmpty()) {
					NeedsQualification calledOnLhsNed = dotOpElementPreceededByNed.get(funcRef);
					int sz = args.exprOrTypeArgsList.size();
					if(sz == ft.inputs.size()) {
						
						List<Type> argsQuali = null;
						if(calledOnLhsNed != null) {
							argsQuali = new ArrayList<Type>();
						}
						
						for(int n=0; n < sz; n++) {
							Type boundto = ft.inputs.get(n);
							Object thing = args.exprOrTypeArgsList.get(n);
							if(thing instanceof Expression) {
								Object what = ((Node)thing).accept(this);
								
								if(what instanceof NeedsQualification) {
									((NeedsQualification)what).addQualifiedBy(boundto);
								}
								
								if(calledOnLhsNed != null) {
									//may be ablt to qualify generic arguments here...!
									argsQuali.add(((Expression)thing).getTaggedType());
									//build up generic mapping like we do with local generics
								}
							}else if(thing instanceof Type){
								if(calledOnLhsNed != null) {
									argsQuali.add((Type)thing);
								}
							}else {
								calledOnLhsNed = null;
							}
						}
						
						if(calledOnLhsNed != null && ft.origonatingFuncDef != null  ) {
							Map<Type, Type> fromGenToThing = TypeCheckUtils.attemptGenericBinding(this.ers, ft.origonatingFuncDef.getFuncType().inputs, argsQuali);
							
							calledOnLhsNed.setPartialBinding(fromGenToThing);
						}
						
					}
				}
			}
		}
		
		return null;
	}
	
	
	
	
	@Override
	public Object visit(New constructorInvoke) {
		FuncType ft = constructorInvoke.constType;
		
		List<Type> argsQuali = null;
		
		if(ft != null && !ft.inputs.isEmpty()) {
			argsQuali = new ArrayList<Type>();
			List<Expression> args = constructorInvoke.args==null? new ArrayList<Expression>():constructorInvoke.args.getArgumentsWNPs();
			int sz = args.size();
			if(sz == ft.inputs.size()) {
				for(int n=0; n < sz; n++) {
					Type boundto = ft.inputs.get(n);
					if(boundto instanceof NamedType) {
						if(((NamedType)boundto).getGenericTypeElements().stream().anyMatch(a-> a instanceof GenericType)) {
							continue;
						}
					}
					if(args == null) {
						return null;
					}
					Expression expr = args.get(n);
					if(expr == null) {
						return null;
					}
					
					Object what = expr.accept(this);
					
					if(what instanceof NeedsQualification) {
						((NeedsQualification)what).addQualifiedBy(boundto);
					}
					
					argsQuali.add(args.get(n).getTaggedType());
					
				}
				
			}
		}
		
		
		Type constType = constructorInvoke.typeee;
		if(constType instanceof NamedType) {
			Object what = constType.accept(newTypeVisitor);
			if(what instanceof NeedsQualification && null != argsQuali && !argsQuali.isEmpty()) {
				NeedsQualification calledOnLhsNed = (NeedsQualification)what;
				
				FuncDef fd = ft.origonatingFuncDef;
				if(fd != null) {
					ft = fd.getFuncType();
				}
				Map<Type, Type> fromGenToThing = TypeCheckUtils.attemptGenericBinding(this.ers, ft.getInputs(), argsQuali);
				
				calledOnLhsNed.setPartialBinding(fromGenToThing);
			}
			
			return what;
		}
		
		return null;
	}
	
	private Type currentReturn = null;
	
	/*	@Override
	public Object visit(FuncDef funcDef) {
		if(null != funcDef.funcblock){
			Type prevReturn = currentReturn;
			currentReturn = funcDef.retType == null?null:funcDef.retType.getTaggedType();
			////////////////
			
			if(null!=funcDef.params) {
				funcDef.params.accept(this);
			}
			
			funcDef.funcblock.accept(this);
			
			////////////////
			currentReturn = prevReturn;
		}
		
		return null;
	}*/
	
	@Override
	public Object visit(FuncDef funcDef) {
		Type prevReturn = currentReturn;
		currentReturn = funcDef.retType == null?null:funcDef.retType.getTaggedType();
		super.visit(funcDef);
		currentReturn = prevReturn;
		
		return null;
	}

	@Override
	public Object visit(FuncParam funcParam) {
		
		if(funcParam.type != null){
			
			if(funcParam.type instanceof NamedType) {
				Object typeNed = funcParam.type.accept(newTypeVisitor);
				if(typeNed instanceof NeedsQualification) {
					this.currentScopeFrame.addVar(funcParam.name, new RefNamePassThrough(new RefName(funcParam.name) , (NeedsQualification)typeNed));
				}
			}
		}
		return null;
	}
	
	@Override
	public Object visit(LambdaDef lambdaDef) {
		Type prevReturn = currentReturn;
		currentReturn = lambdaDef.returnType == null?null:lambdaDef.returnType.getTaggedType();
		super.visit(lambdaDef);
		currentReturn = prevReturn;
		
		return null;
	}
	
	@Override
	public Object visit(ReturnStatement returnStatement) {
		if(null!=returnStatement.ret) {
			if(currentReturn != null) {
				Object what = returnStatement.ret.accept(this);
				if(what instanceof NeedsQualification) {
					((NeedsQualification)what).addQualifiedBy(currentReturn);
				}
			}
		}
		
		return null;
	}
}