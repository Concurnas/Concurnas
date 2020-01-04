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
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.REPLDepGraphComponent;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.TypedefStatement;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.runtime.Pair;

/*
 * maintain dependancy graph of top level elements < - (depends on) <- name, type of top level item
 */
public class REPLDepGraphManager extends AbstractVisitor implements Unskippable {
	private HashMap<String, HashSet<REPLDepGraphComponent>> depMap;
	private HashMap<String, HashSet<Pair<Type, HashSet<Type>>>> topLevelNames;
	//private ArrayList<REPLDepGraphComponent> topLevelItems;
	
	/*
	 * create dependency map
	 * trigger recalc for all dependnecies of top level items which have changed on the graph. 'changed':
	 * + newly defined
	 * + changed signature since last iteration
	 */
	public boolean updateDepGraph(Block lexedAndParsedAST) {
		//update graph
		depMap = new HashMap<String, HashSet<REPLDepGraphComponent>>();
		
		HashMap<String, HashSet<Pair<Type, HashSet<Type>>>> prevtopLevelItems = new HashMap<String, HashSet<Pair<Type, HashSet<Type>>>>();
		if(null != topLevelNames) {
			prevtopLevelItems.putAll(topLevelNames);
		}
		topLevelNames = new HashMap<String, HashSet<Pair<Type, HashSet<Type>>>>();
		
		super.visit(lexedAndParsedAST);
		
		//now check for changes: topLevelItems vs prevtopLevelItems
		HashSet<REPLDepGraphComponent> componentsToRefresh = new HashSet<REPLDepGraphComponent>();
		for(String topLevelItem : topLevelNames.keySet()) {
			HashSet<REPLDepGraphComponent> toAdd=null;
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
		
		if(!componentsToRefresh.isEmpty()) {
			componentsToRefresh.forEach(a -> a.setSkippable(false));
			
			itemsModifiedThisSession.addAll(componentsToRefresh);
			return true;
		}else {
			return false;
		}
	}
	
	public static class REPLComponentWrapper{
		public REPLDepGraphComponent comp;

		//hc and equals for funcdef ignore function name
		public REPLComponentWrapper(REPLDepGraphComponent comp) {
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
	
	private HashSet<REPLDepGraphComponent> itemsModifiedThisSession = new HashSet<REPLDepGraphComponent>();
	public HashSet<REPLComponentWrapper> getAndResetThingsModified(){
		HashSet<REPLComponentWrapper> ret = new HashSet<REPLComponentWrapper>();
		itemsModifiedThisSession.forEach(a -> ret.add(new REPLComponentWrapper(a)));
		itemsModifiedThisSession = new HashSet<REPLDepGraphComponent>();
		return ret;
	}
	
	private boolean isTopLevelItem = true;
	private HashSet<String> currentDependencies;
	private HashSet<Type> currentTypeDependencies;
	
	@Override
	public Object visit(FuncDef funcDef) {
		return this.visit((REPLDepGraphComponent)funcDef);
	}
	
	@Override
	public Object visit(AssignExisting funcDef) {
		return this.visit((REPLDepGraphComponent)funcDef);
	}
	
	@Override
	public Object visit(AssignNew funcDef) {
		return this.visit((REPLDepGraphComponent)funcDef);
	}
	
	@Override
	public Object visit(TypedefStatement typedef) {
		return this.visit((REPLDepGraphComponent)typedef);
	}
	

	@Override
	public Object visit(ClassDef classDef) {
		return this.visit((REPLDepGraphComponent)classDef);
	}
	
	@Override
	public Object visit(EnumDef enumDef){
		return this.visit((REPLDepGraphComponent)enumDef);
	}
	
	@Override
	public Object visit(AnnotationDef annotDef){
		return this.visit((REPLDepGraphComponent)annotDef);
	}
	
	
	private void visitSuperREPLGraphComp(REPLDepGraphComponent comp) {
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
		}else if(comp instanceof EnumDef) {
			super.visit((EnumDef)comp);
		}else if(comp instanceof AnnotationDef) {
			super.visit((AnnotationDef)comp);
		}
	}
	
	public Object visit(REPLDepGraphComponent comp) {
		if(isTopLevelItem && comp.isNewComponent()) {
			boolean prevIsTop = isTopLevelItem;
			HashSet<String> prevCDeps = currentDependencies;
			
			isTopLevelItem = false;
			currentDependencies = new HashSet<String>();
			
			
			HashSet<Type> typeDependencies = null;
			if(comp instanceof ClassDef) {
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
					
					HashSet<REPLDepGraphComponent> deps = depMap.get(dep);
					if(deps == null) {
						deps  = new HashSet<REPLDepGraphComponent>();
						depMap.put(dep, deps);
					}
					deps.add(comp);					
				}
			}
			
			{
				String fname = comp.getName();
				HashSet<Pair<Type, HashSet<Type>>> typesForTopLe = topLevelNames.get(fname);
				if(null == typesForTopLe) {
					typesForTopLe = new HashSet<Pair<Type, HashSet<Type>>>();
					topLevelNames.put(fname, typesForTopLe);
				}
				Type tt = comp.getFuncType();
				tt = tt == null?null:tt.getTaggedType();
				//double call to validate whether the type returned is valid or not (e.g. on typedefs or namedTypes)
				
				Pair<Type, HashSet<Type>> typeAndDependantTypes = new Pair<Type, HashSet<Type>>(tt, typeDependencies);
				typesForTopLe.add(typeAndDependantTypes);
			}
			
			isTopLevelItem = prevIsTop;
			currentDependencies = prevCDeps;
			
			comp.setSkippable(true);	
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