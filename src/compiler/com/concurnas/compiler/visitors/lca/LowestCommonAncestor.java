package com.concurnas.compiler.visitors.lca;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.GenericType;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.NamedTypeMany;
import com.concurnas.compiler.ast.NullStatus;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.TypeCheckUtils;

public class LowestCommonAncestor {

	private Collection<NamedType> objTypes;
	private boolean onlyInstantiableRefTypes;
	private HashSet<Type> resolvedAlready;
	
	private final static NamedType failObj = new NamedType(new ClassDefJava(Object.class));
	
	public static Type getLCA(List<Type> types, boolean onlyInstantiables)
	{
		//List<ClassDef> classes = new ArrayList<ClassDef>(types.size());
		//ArrayList<Type> gens = new ArrayList<Type>();
		int arLevels = 0;
		boolean isRef = false;
		ArrayList<NamedType> namedTypes = new ArrayList<NamedType>();
		for(Type t : types) {
			arLevels = t.getArrayLevelsRefOVerride();
			if(t instanceof NamedType){
				if(((NamedType)t).getIsRef()){
					isRef = true;
				}
				t = (Type)t.copy();
				t.setInOutGenModifier(null);
				namedTypes.add((NamedType)t);
			}
			else if(t instanceof GenericType){
				//TODO: upper lower bound should be reflected here, NOT just upper bound being object
				namedTypes.add(failObj.copyTypeSpecific());
			}
			
		}
		//YEAH NASTY HERE
		LowestCommonAncestor lca = new LowestCommonAncestor(namedTypes, onlyInstantiables, new HashSet<Type>());//TODO: pass in lower bound if one is defined from generic eleiments
		NamedType ret = lca.go();
		//JPT: this is foul, assumes that they are identical
		//ret.setGenTypes(gens);
		ret.setArrayLevels(arLevels);
		
		if(isRef){//preserve refness on the lies of z:=9; g = [z,z,z]
			ret.setIsRef(true);
		}
		
		return ret;
	}
	
	public LowestCommonAncestor(Collection<NamedType> classes, boolean onlyInstantiables, HashSet<Type> resolvedAlready)
	{
		this.objTypes = classes;
		this.resolvedAlready = resolvedAlready;
		this.onlyInstantiableRefTypes = onlyInstantiables;
	}
	
	private LinkedHashSet<NamedType> justGetBFSOrder(NamedType obj)
	{
		return (new ClassDefTree(obj)).getTypeHierarchy();
	}
	
	private LinkedHashSet<NamedType> getShortestList(ArrayList<LinkedHashSet<NamedType>> searchLists)
	{
		LinkedHashSet<NamedType> shortSoFar = null;
		int shotest = -1;
		
		for(LinkedHashSet<NamedType> s : searchLists)
		{
			if(shotest == -1)
			{
				shortSoFar = s;
				shotest = s.size();
			}
			else
			{
				int ss = s.size();
				if(ss < shotest)
				{
					shortSoFar = s;
					shotest = ss;
				}
			}
			
		}
		
		return shortSoFar;
	}
	
	//This is the key method
	public NamedType go()
	{
		if(!this.objTypes.isEmpty())
		{
			ArrayList<LinkedHashSet<NamedType>> searchLists = new ArrayList<LinkedHashSet<NamedType>>(this.objTypes.size()); 
			
			for(NamedType cls : this.objTypes)
			{
				searchLists.add(justGetBFSOrder(cls));
			}

			//Start with shotest list cos if only 1 element then much quicker than searching everything
			LinkedHashSet<NamedType> shortestList = getShortestList(searchLists);
			
			//merge in nullableGenercs
			//shortestList = mergeInNullgenerics(shortestList, searchLists);
			
			
			if(searchLists.isEmpty())
			{
				NamedType headofShortestList = shortestList.iterator().next();
				if(null == headofShortestList)
				{
					headofShortestList = failObj;
				}
				return headofShortestList;
			}
			
			LinkedHashSet<NamedType> foundMatches = new LinkedHashSet<NamedType>();
			
			
			for(NamedType searchItem : shortestList)
			{
				if(!this.resolvedAlready.contains(searchItem)) {//ensure that we've not already visited this type
					//avoids inf loop on things like X < Enum<X> and Y < Enum<Y>
					this.resolvedAlready.add(searchItem);
					NamedType found =findInKidOthers(searchItem, searchLists);
					if(found != null)
					{
						foundMatches.add(found);
					}
				}
			}
			
			if(!foundMatches.isEmpty())
			{
				if(onlyInstantiableRefTypes){//filter out everything which we cannot directly instantiate (e.g. interfaces, abstract types etc) - used for ArrayDefs etc
					LinkedHashSet<NamedType> foundMatchesfiltered = new LinkedHashSet<NamedType>();
					
					for(NamedType potential : foundMatches)
					{
						ClassDef cd = potential.getSetClassDef();
						if(null != cd){
							if(!cd.isInstantiable()){
								continue;
							}
						}

						foundMatchesfiltered.add(potential);
					}
					
					if(!foundMatchesfiltered.isEmpty()){
						return new NamedTypeMany(foundMatchesfiltered);
					}
				}else{
					return new NamedTypeMany(foundMatches);
				}
			}
		}
		//everything inherits from object
		return failObj;
	}

	/*
	 * String, String -> String
	 * String, Object -> Object
	 * ArrayList<String> ArrayList<Integer> -> ArrayList<? extends Object> or ArrayList<?>
	 */
/*	private boolean isgenericWith(NamedType base, NamedType matchwith) {
		if(base.equals(matchwith)) {
			return true;
		}else if(base.hasGenTypes() && matchwith.hasGenTypes()){
			ClassDef baseCD = base.getSetClassDef();
			ClassDef matchWithCD = matchwith.getSetClassDef();
			if(baseCD.equals(matchWithCD)) {
				return true;
				//more generic of the gens...
				ArrayList<Type> matchGens = matchwith.getGenericTypeElements();
				ArrayList<Type> baseGens = base.getGenericTypeElements();
				
				int msize = matchGens.size();
				if(msize == baseGens.size()) {
					ArrayList<Type> newGens = new ArrayList<Type>(msize);
					for(int n = 0; n < msize; n++) {
						List<NamedType> two = new ArrayList<NamedType>(2);
						two.add((NamedType)matchGens.get(n));
						two.add((NamedType)baseGens.get(n));
						LowestCommonAncestor gencom = new LowestCommonAncestor(two, false);
						NamedType what = gencom.go();
						if(what == null) {//?
							return null;
						}
						newGens.add(what);
					}
					NamedType ret = new NamedType(baseCD);
					ret.setGenTypes(newGens);
					return ret;
				}
								
			}
		}
		return false;
	}*/
	
	
	private NamedType findInKidOthers(NamedType searchItem, ArrayList<LinkedHashSet<NamedType>> others)
	{
		ArrayList<NamedType> matches = new ArrayList<NamedType>();
		ClassDef searchItemCD = searchItem.getSetClassDef();
		boolean sHasGenerics = searchItem.hasGenTypes();
		int sHasGenericsSize = sHasGenerics?searchItem.getGenericTypeElements().size():0;
		
		for(LinkedHashSet<NamedType> other : others){
			NamedType oMatch = null;
			
			if(other.contains(searchItem)){
				
				for(NamedType inst : other) {
					if(inst.equals(searchItem)) {
						oMatch = inst;
						break;//direct match
					}
				}
				
				//oMatch = searchItem;
			}else if(sHasGenerics){
				//might match if we strip out the generic information
				for(NamedType matchwith : other) {
					if(matchwith.getGenericTypeElements().size() == sHasGenericsSize){
						ClassDef matchWithCD = matchwith.getSetClassDef();
						if(searchItemCD.equals(matchWithCD)) {
							oMatch = matchwith;
							break;
						}
					}
				}
			}
			
			if(null == oMatch) {
				return null;
			}else{
				matches.add(oMatch);
			}
		}
		
		if(others.size() == matches.size()) {//missing at least one, give up
			
			HashSet<NamedType> entries = new HashSet<NamedType>(matches);
			if(entries.size() == 1) {//all same
				return combineNullables(matches);
			}else if(sHasGenerics){//combine GENERICS types...
				ArrayList<Type> newGens = new ArrayList<Type>(sHasGenericsSize);
				NamedType ret = new NamedType(searchItemCD);
				boolean isrefiedType = TypeCheckUtils.isReifiedType(ret);
				
				for(int n = 0; n < sHasGenericsSize; n++) {
					List<NamedType> choices = new ArrayList<NamedType>(2);
					
					for(NamedType m : matches) {
						Type tt = m.getGenericTypeElements().get(n);
						NamedType asNamed;
						if(tt instanceof GenericType) {
							GenericType gt = (GenericType)tt;
							asNamed = gt.upperBound;
						}else if((tt instanceof NamedType)){
							asNamed = (NamedType)tt;
						}/*else if(tt instanceof VarNull) {
							continue;
						}*/else {
							asNamed = ScopeAndTypeChecker.const_object.copyTypeSpecific();
						}
						choices.add(asNamed);
					}
					
					LowestCommonAncestor gencom = new LowestCommonAncestor(choices, false, this.resolvedAlready);
					NamedType what = gencom.go();
					if(what == null) {//?
						return null;
					}
					if(what instanceof NamedTypeMany) {
						what = (NamedType)((NamedTypeMany)what).getSelf();
					}
					
					if(isrefiedType) {
						newGens.add(what.copyTypeSpecific());
					}else {//have to use wildcard
						NamedType nt = new NamedType(what.getSetClassDef());
						nt.isWildCardAny = true;
						nt.setOrigonalGenericTypeUpperBound(what);
						newGens.add(nt);//generic with upper bound being...
					}
				}
				ret.setGenTypes(newGens);
				return ret;
				
			}
			
			return null;
		}
		
		//
		
		return null;
	}

	//Thing<String>, Thing<String?> => Thing<String?>
	private NamedType combineNullables(ArrayList<NamedType> matches) {
		NamedType ret = matches.get(0).copyTypeSpecific();
		for(NamedType inst : matches) {
			foldInNullable(ret, inst);
		}
		
		return ret;
	}
	
	private void foldInNullable(Type left, Type right) {
		if(left instanceof NamedType) {
			NamedType asnamedLeft = (NamedType)left;
			NamedType asnamedRight = (NamedType)right;
			
			List<Type> lgens = asnamedLeft.getGenTypes();
			List<Type> rgens = asnamedRight.getGenTypes();
			
			int sz = lgens.size();
			for(int n=0; n < sz; n++) {
				foldInNullable(lgens.get(n), rgens.get(n));
			}
		}else if(left instanceof FuncType) {
			FuncType lFuncType = (FuncType)left;
			FuncType rFuncType = (FuncType)right;
			
			List<Type> lgens = lFuncType.inputs;
			List<Type> rgens = rFuncType.inputs;
	
			int sz = lgens.size();
			for(int n=0; n < sz; n++) {
				foldInNullable(lgens.get(n), rgens.get(n));
			}
					
			foldInNullable(lFuncType.retType, rFuncType.retType);
		}
		
		if(left.hasArrayLevels()) {
			List<NullStatus> lns = left.getNullStatusAtArrayLevel();
			List<NullStatus> rns = right.getNullStatusAtArrayLevel();
			
			if(lns != null && rns != null) {
				List<NullStatus> combined = new ArrayList<NullStatus>();

				int sz = lns.size();
				for(int n=0; n < sz; n++) {
					combined.add((lns.get(n)== NullStatus.NULLABLE || rns.get(n)== NullStatus.NULLABLE )? NullStatus.NULLABLE:NullStatus.NONNULL);
				}
				
				left.setNullStatusAtArrayLevel(combined);
			}
		}
		
		if(right.getNullStatus() == NullStatus.NULLABLE) {
			left.setNullStatus(NullStatus.NULLABLE);
		}
	}
	
}
