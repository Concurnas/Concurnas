package com.concurnas.compiler.visitors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

import com.concurnas.compiler.ast.Annotation;
import com.concurnas.compiler.ast.AnnotationDef;
import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.DotOperator;
import com.concurnas.compiler.ast.EnumDef;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.FuncInvoke;
import com.concurnas.compiler.ast.FuncRef;
import com.concurnas.compiler.ast.ImpliInstance;
import com.concurnas.compiler.ast.ImportFrom;
import com.concurnas.compiler.ast.ImportImport;
import com.concurnas.compiler.ast.ImportStar;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.ObjectProvider;
import com.concurnas.compiler.ast.REPLTopLevelComponent;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.TypedefStatement;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.util.ImportStarUtil;
import com.concurnas.repl.REPLState;
import com.concurnas.runtime.Pair;

/*
 * maintain dependancy graph of top level elements < - (depends on) <- name, type of top level item
 */
public class REPLDepGraphManager extends AbstractVisitor implements Unskippable {
	private HashMap<String, HashSet<REPLTopLevelComponent>> depMap;
	private HashMap<String, HashSet<Pair<Type, HashSet<Type>>>> topLevelNames;
	//private ArrayList<REPLDepGraphComponent> topLevelItems;
	private REPLState replState;
	

	private HashSet<ImportStarUtil.PackageOrClass> prevtopLevelImportStar = new HashSet<ImportStarUtil.PackageOrClass>();
	private HashSet<ImportStarUtil.PackageOrClass> topLevelImportStar;
	
	HashMap<String, HashSet<Pair<Type, HashSet<Type>>>> prevtopLevelItems = new HashMap<String, HashSet<Pair<Type, HashSet<Type>>>>();
	
	public REPLDepGraphManager(REPLState replState) {
		this.replState = replState;
	}

	/*
	 * create dependency map
	 * trigger recalc for all dependnecies of top level items which have changed on the graph. 'changed':
	 * + newly defined
	 * + changed signature since last iteration
	 */
	public boolean updateDepGraph(Block lexedAndParsedAST) {
		//update graph
		depMap = new HashMap<String, HashSet<REPLTopLevelComponent>>();
		topLevelImportStar = new HashSet<ImportStarUtil.PackageOrClass>();
		
		
		if(null != topLevelNames) {
			prevtopLevelItems.putAll(topLevelNames);
		}
		topLevelNames = new HashMap<String, HashSet<Pair<Type, HashSet<Type>>>>();
		
		super.visit(lexedAndParsedAST);
		
		//now check for changes: topLevelItems vs prevtopLevelItems
		HashSet<REPLTopLevelComponent> componentsToRefresh = new HashSet<REPLTopLevelComponent>();
		for(String topLevelItem : topLevelNames.keySet()) {
			HashSet<REPLTopLevelComponent> toAdd=null;
			if(prevtopLevelItems.containsKey(topLevelItem)) {
				HashSet<Pair<Type, HashSet<Type>>> newTypes = topLevelNames.get(topLevelItem);
				HashSet<Pair<Type, HashSet<Type>>> oldTypes = prevtopLevelItems.get(topLevelItem);
				if(!newTypes.equals(oldTypes)){//if there are changes then dependencies
					toAdd = depMap.get(topLevelItem);
				}
			}else {//new so add all deps
				toAdd = depMap.get(topLevelItem);
			}
			
			if(null != toAdd) {
				componentsToRefresh.addAll(toAdd);
			}
		}
		
		if(!topLevelImportStar.isEmpty()) {
			//only process new items since last iteration
			HashSet<ImportStarUtil.PackageOrClass> toproc = new HashSet<ImportStarUtil.PackageOrClass>(topLevelImportStar);
			toproc.removeAll(prevtopLevelImportStar);
			
			for(ImportStarUtil.PackageOrClass inst : toproc) {
				//see if any dependencies from any non-already-included dependencies can be satisfied by this instance, if so tag them for inclusion
				for(String dep : depMap.keySet()) {
					if(!dep.contains(".")) {
						if(null != inst.getResource(dep)) {
							componentsToRefresh.addAll(depMap.get(dep));
						}
					}
				}
			}
			
			prevtopLevelImportStar = topLevelImportStar;
		}
		
		if(!componentsToRefresh.isEmpty()) {
			componentsToRefresh.forEach(a -> {
				if(!a.getErrors()) {
					a.setSupressErrors(false);//no errors before, so check for them again
				}
				if(a.canSkip()) {
					a.setSkippable(false);
					this.replState.topLevelItemsToSkip.remove(new REPLComponentWrapper(a));
				}
			});
			
			itemsModifiedThisSession.addAll(componentsToRefresh);
			return true;
		}else {
			return false;
		}
	}
	
	public static class REPLComponentWrapper{
		public REPLTopLevelComponent comp;

		//hc and equals for funcdef ignore function name
		public REPLComponentWrapper(REPLTopLevelComponent comp) {
			this.comp = comp;
		}
		
		public int hashCode() {
			String nn = this.comp.getName();
			int ret = nn == null? 0: nn.hashCode();
			ret += this.comp.hashCode();
			return ret;
		}
		
		public boolean equals(Object an) {
			if(an instanceof REPLComponentWrapper) {
				REPLComponentWrapper other  = (REPLComponentWrapper)an;
				return Objects.equals(other.comp.getName(), this.comp.getName()) && Objects.equals(other.comp, this.comp);
			}
			return false;
		}
		
	}
	
	private HashSet<REPLTopLevelComponent> itemsModifiedThisSession = new HashSet<REPLTopLevelComponent>();
	public HashSet<REPLComponentWrapper> getAndResetThingsModified(){
		HashSet<REPLComponentWrapper> ret = new HashSet<REPLComponentWrapper>();
		itemsModifiedThisSession.forEach(a -> ret.add(new REPLComponentWrapper(a)));
		itemsModifiedThisSession = new HashSet<REPLTopLevelComponent>();
		return ret;
	}
	
	private boolean isTopLevelItem = true;
	private HashSet<String> currentDependencies;
	private HashSet<Type> currentTypeDependencies;
	
	@Override
	public Object visit(FuncDef funcDef) {
		return this.visit((REPLTopLevelComponent)funcDef);
	}
	
	@Override
	public Object visit(ObjectProvider funcDef) {
		return this.visit((REPLTopLevelComponent)funcDef);
	}
	
	@Override
	public Object visit(AssignExisting funcDef) {
		return this.visit((REPLTopLevelComponent)funcDef);
	}
	
	@Override
	public Object visit(AssignNew funcDef) {
		return this.visit((REPLTopLevelComponent)funcDef);
	}
	
	@Override
	public Object visit(TypedefStatement typedef) {
		return this.visit((REPLTopLevelComponent)typedef);
	}
	

	@Override
	public Object visit(ClassDef classDef) {
		return this.visit((REPLTopLevelComponent)classDef);
	}
	
	@Override
	public Object visit(EnumDef enumDef){
		return this.visit((REPLTopLevelComponent)enumDef);
	}
	
	@Override
	public Object visit(AnnotationDef annotDef){
		return this.visit((REPLTopLevelComponent)annotDef);
	}
	
	@Override
	public Object visit(ImportFrom annotDef){
		return this.visit((REPLTopLevelComponent)annotDef);
	}
	
	@Override
	public Object visit(ImportImport annotDef){
		return this.visit((REPLTopLevelComponent)annotDef);
	}
	
	
	@Override
	public Object visit(ImportStar importStar){
		if(isTopLevelItem) {//copy paste yuck
			String nameSoFar = importStar.from;
			boolean isPackage = Package.getPackage(nameSoFar) != null;
			
			Class<?> isClass = null;
			try {
				isClass = Class.forName(nameSoFar);
			}catch(ClassNotFoundException cnf) {
				
			}
			
			if(isPackage) {
				topLevelImportStar.add(new ImportStarUtil.PackageIS(nameSoFar));
			}
			
			if(null != isClass) {
				topLevelImportStar.add(new ImportStarUtil.ClassIS(isClass));
			}
		}
		
		return null;
	}
	
	
	private void visitSuperREPLGraphComp(REPLTopLevelComponent comp) {
		if(comp instanceof FuncDef) {
			super.visit((FuncDef)comp);
		}else if(comp instanceof AssignExisting) {
			super.visit((AssignExisting)comp);
		}else if(comp instanceof AssignNew) {
			super.visit((AssignNew)comp);
		}else if(comp instanceof TypedefStatement) {
			super.visit((TypedefStatement)comp);
		}else if(comp instanceof ClassDef) {
			ClassDef cd = (ClassDef)comp;
			
			if(null != currentDependencies) {
				currentDependencies.add(cd.superclass);
				ClassDef sup = cd.getSuperclass();
				if(null != sup) {
					new NamedType(sup).accept(this);
				}
			}
			
			super.visit(cd);
		}else if(comp instanceof ObjectProvider) {
			super.visit((ObjectProvider)comp);
		}else if(comp instanceof EnumDef) {
			super.visit((EnumDef)comp);
		}else if(comp instanceof AnnotationDef) {
			super.visit((AnnotationDef)comp);
		}
	}
	
	public Object visit(REPLTopLevelComponent comp) {
		if(isTopLevelItem && comp.isNewComponent()) {
			boolean prevIsTop = isTopLevelItem;
			HashSet<String> prevCDeps = currentDependencies;
			
			isTopLevelItem = false;
			currentDependencies = new HashSet<String>();
			
			
			HashSet<Type> typeDependencies = null;
			if(comp instanceof ClassDef || comp instanceof ObjectProvider) {
				HashSet<Type> prevCTDeps = currentTypeDependencies;
				currentTypeDependencies = new HashSet<Type>();
				visitSuperREPLGraphComp(comp);
				typeDependencies = currentTypeDependencies;
				currentTypeDependencies=prevCTDeps;
			}else {
				visitSuperREPLGraphComp(comp);
			}
			
			
			if(!currentDependencies.isEmpty()) {
				for(String dep : currentDependencies) {
					
					HashSet<REPLTopLevelComponent> deps = depMap.get(dep);
					if(deps == null) {
						deps  = new HashSet<REPLTopLevelComponent>();
						depMap.put(dep, deps);
					}
					deps.add(comp);					
				}
			}
			
			{
				Type tt = comp.getFuncType();
				tt = tt == null?null:tt.getTaggedType();
				for(String fname : comp.getNames()) {
					HashSet<Pair<Type, HashSet<Type>>> typesForTopLe = topLevelNames.get(fname);
					if(null == typesForTopLe) {
						typesForTopLe = new HashSet<Pair<Type, HashSet<Type>>>();
						topLevelNames.put(fname, typesForTopLe);
					}
					//double call to validate whether the type returned is valid or not (e.g. on typedefs or namedTypes)
					Pair<Type, HashSet<Type>> typeAndDependantTypes = new Pair<Type, HashSet<Type>>(tt, typeDependencies);
					typesForTopLe.add(typeAndDependantTypes);
				}
			}
			
			isTopLevelItem = prevIsTop;
			currentDependencies = prevCDeps;
			
			this.replState.topLevelItemsToSkip.add(new REPLComponentWrapper(comp));
			
		}else {
			visitSuperREPLGraphComp(comp);
		}
		
		return null;
	}
	
	
	@Override
	public Object visit(FuncInvoke funcInvoke) {
		if(null != currentDependencies) {
			String fname = funcInvoke.funName;
			currentDependencies.add(fname);
		}
		
		return super.visit(funcInvoke);
	}
	
	@Override
	public Object visit(RefName refName) {
		if(null != currentDependencies) {
			currentDependencies.add(refName.name);
			Type tt = refName.getTaggedType();
			if(tt != null) {
				tt.accept(this);
			}
		}
		
		return super.visit(refName);
	}
	
	@Override
	public Object visit(Annotation annotation){
		
		if(null != currentDependencies) {
			currentDependencies.add(annotation.className);
			Type tt = annotation.getTaggedType();
			if(tt != null) {
				tt.accept(this);
			}
		}
		
		return super.visit(annotation);
	}
	
	@Override
	public Object visit(ImpliInstance implInstance){
		
		if(null != currentDependencies) {
			currentDependencies.add(implInstance.traitName);
			Type tt = implInstance.getTaggedType();
			if(tt != null) {
				tt.accept(this);
			}
		}
		
		return super.visit(implInstance);
	}
	
	@Override
	public Object visit(FuncRef funcRef) {
		if(null != currentDependencies) {
			String fname = funcRef.methodName;
			currentDependencies.add(fname);
		}
		
		return super.visit(funcRef);
	}
	
	//namedtype
	@Override
	public Object visit(NamedType namedType) {
		if(null != currentDependencies) {
			String fname = namedType.getNamedTypeStr();
			currentDependencies.add(fname);
		}
		
		if(null != currentTypeDependencies) {
			currentTypeDependencies.add(namedType);
		}
		
		return super.visit(namedType);
	}
	
	@Override
	public Object visit(Block block) {
		if(block.isolated) {
			boolean prev = isTopLevelItem;
			isTopLevelItem=false;
			super.visit(block);
			isTopLevelItem = prev;
		}else {
			super.visit(block);
		}
		return null;
	}
	
	
	@Override
	public Object visit(DotOperator dotOperator) {
		//lastLineVisited=dotOperator.getLine();
		Object lastThing = null;
		for(Expression e: dotOperator.getElements(this))
		{
			lastThing = e.accept(this);
		}
		return lastThing;
	}

	
}