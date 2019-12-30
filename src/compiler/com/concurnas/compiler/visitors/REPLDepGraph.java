package com.concurnas.compiler.visitors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignNew;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.FuncInvoke;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.REPLDepGraphComponent;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.Type;

/*
 * maintain dependancy graph of top level elements < - (depends on) <- name, type of top level item
 */
public class REPLDepGraph extends AbstractVisitor implements Unskippable {
	
	
	private HashMap<String, HashSet<REPLDepGraphComponent>> depMap;
	private HashMap<String, HashSet<Type>> topLevelNames;
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
		//topLevelItems = new ArrayList<REPLDepGraphComponent>();
		HashMap<String, HashSet<Type>> prevtopLevelItems = new HashMap<String, HashSet<Type>>();
		if(null != topLevelNames) {
			prevtopLevelItems.putAll(topLevelNames);
		}
		topLevelNames = new HashMap<String, HashSet<Type>>();
		
		super.visit(lexedAndParsedAST);
		
		//now check for changes: topLevelItems vs prevtopLevelItems
		
		HashSet<REPLDepGraphComponent> componentsToRefresh = new HashSet<REPLDepGraphComponent>();
		for(String topLevelItem : topLevelNames.keySet()) {
			HashSet<REPLDepGraphComponent> toAdd=null;
			if(prevtopLevelItems.containsKey(topLevelItem)) {
				HashSet<Type> newTypes = topLevelNames.get(topLevelItem);
				HashSet<Type> oldTypes = prevtopLevelItems.get(topLevelItem);
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
	
	
	private void visitSuperREPLGraphComp(REPLDepGraphComponent comp) {
		if(comp instanceof FuncDef) {
			super.visit((FuncDef)comp);
		}else if(comp instanceof AssignExisting) {
			super.visit((AssignExisting)comp);
		}else if(comp instanceof AssignNew) {
			super.visit((AssignNew)comp);
		}
	}
	
	public Object visit(REPLDepGraphComponent comp) {
		if(isTopLevelItem && comp.isNewComponent()) {
			boolean prevIsTop = isTopLevelItem;
			HashSet<String> prevCDeps = currentDependencies;
			
			isTopLevelItem = false;
			currentDependencies = new HashSet<String>();
			
			visitSuperREPLGraphComp(comp);

			
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
				HashSet<Type> typesForTopLe = topLevelNames.get(fname);
				if(null == typesForTopLe) {
					typesForTopLe = new HashSet<Type>();
					topLevelNames.put(fname, typesForTopLe);
				}
				typesForTopLe.add(comp.getFuncType());
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
		}
		
		return super.visit(refName);
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
	
	
	//+funcref
	//namedType
	
	
	@Override
	public Object visit(ClassDef classDef) {
		boolean prev = isTopLevelItem;
		isTopLevelItem = false;
		super.visit(classDef);
		isTopLevelItem = prev;
		
		return null;
	}
	
	//top level element depends on
}