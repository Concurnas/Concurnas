package com.concurnas.compiler.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.GenericType;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.NullStatus;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.visitors.ErrorRaiseable;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.runtime.Pair;

public class GenericTypeUtils {
	
	public static Type mapFuncTypeEraseEnerics(Type input)
	{//replace all generic instances with the same 'generic' generic type
		return mapFuncTypeGenericsToOtherGenerics(input, mapAllGenSameObj, true);
	}
	
	public static GenericType isGenTypeInList(String name, ArrayList<GenericType> candidates)
	{
		for(GenericType can : candidates)
		{
			if(can.name.equals(name))
			{
				return can;
			}
		}
		return null;
	}
	
	public static Type mapFuncTypeGenericsToOtherGenerics(Type input, Map<Type, Type> superclassTypeToClsGeneric, boolean namedTypeGenericsGetErased){
		return mapFuncTypeGenericsToOtherGenerics(input, superclassTypeToClsGeneric, namedTypeGenericsGetErased, null);
	}
	
	public static Type mapFuncTypeGenericsToOtherGenerics(Type input, Map<Type, Type> superclassTypeToClsGeneric, boolean namedTypeGenericsGetErased, HashMap<String, Type> nametoType)
	{//generic func type,
		if(null == superclassTypeToClsGeneric || input == null)
		{
			return input;
		}
		
		if(input instanceof GenericType)
		{
			GenericType gen  = (GenericType)input;
			Type ret = superclassTypeToClsGeneric.get(gen);
			if(null == ret){//yeah, its possible to get here...
				ret = gen;
			}
			else{
				ret = (Type)ret.copy();
				ret.setOrigonalGenericTypeUpperBound(gen.upperBound);
				gen.upperBound.setArrayLevels(gen.getArrayLevels());
				if(gen.getArrayLevels() > 0){//the rhs thing must have levels if the gens does...
					ret.setArrayLevels(ret.getArrayLevels() + gen.getArrayLevels());
				}
				
				NullStatus origNullS = input.getNullStatus();
				if(origNullS != NullStatus.NONNULL) {
					if(ret.getNullStatus() != NullStatus.NULLABLE) {
						ret.setNullStatus(origNullS);
					}
				}
				
			}
			
			return ret;
		}
		else if(input instanceof NamedType)
		{
			NamedType nt = (NamedType) ((NamedType)input).copy();
			
			if(namedTypeGenericsGetErased && nt.getOrigonalGenericTypeUpperBoundRaw() != null){
				return theONE;
			}
			
			List<Type> gens = nt.getGenTypes();
			
			if(/*gens.isEmpty() &&*/ null == nt.getSetClassDef() && null != nametoType){
				String lename = nt.getNamedTypeStr();
				if(nametoType.containsKey(lename)){
					Type resolved = nametoType.get(lename);//ugly
					resolved = (Type)resolved.copy();//i think this doesnt work with typedefs like x<y<z>>, needs a more recursive approach?
					if(resolved instanceof NamedType){
						NamedType asNamed = (NamedType)resolved;
						
						ArrayList<Type> genn = nt.getGenericTypeElements();
						if(!genn.isEmpty()){
							asNamed.setGenTypes(genn);
							resolved= mapFuncTypeGenericsToOtherGenerics(asNamed, superclassTypeToClsGeneric, namedTypeGenericsGetErased, nametoType);
						}
					}
					
					return resolved;
				}
			}
			
			if(gens.isEmpty() && null != nt.getSetClassDef()){
				for(Type t : nt.getSetClassDef().getClassGenricList()){
					gens.add(t);
				}
			}
			
			ArrayList<Type> newGens = new ArrayList<Type>();
			for(Type g : gens)
			{
				if(namedTypeGenericsGetErased)
				{
					newGens.add(theONE);
				}
				else
				{
					newGens.add(mapFuncTypeGenericsToOtherGenerics(g, superclassTypeToClsGeneric, namedTypeGenericsGetErased, nametoType));
				}
				
			}
			nt.setGenTypes(newGens);
			
			ArrayList<Pair<String, ArrayList<Type>>> newTup = new ArrayList<Pair<String, ArrayList<Type>>>();
			for(Pair<String, ArrayList<Type>> tup: nt.nestorSegments)
			{
				ArrayList<Type> gensa = tup.getB();
				ArrayList<Type> newgen = new ArrayList<Type>(gensa.size());
				for(Type g :  gensa)
				{
					if(namedTypeGenericsGetErased)
					{
						newgen.add(theONE);
					}
					else
					{
						newgen.add(mapFuncTypeGenericsToOtherGenerics(g, superclassTypeToClsGeneric, namedTypeGenericsGetErased, nametoType));
					}
				}
				
				newTup.add(new Pair<String, ArrayList<Type>>(tup.getA(), newgen) );
			}
			nt.nestorSegments = newTup;
			
			if(namedTypeGenericsGetErased){
				nt.setFromClassGenericToQualifiedType(new HashMap<GenericType, Type>());
			}
			
			//nt.setGenTypes(nt.getGenTypes());
			//nt.setFromClassGenericToQualifiedType(new HashMap<GenericType, Type>());
			
			/*if( nt.equals(input)){//that didnt work, now try to see if we can treat the NamedType as a GenericType
				return mapFuncTypeGenericsToOtherGenerics(new GenericType(0,0, nt.getNamedTypeStr(), 0), superclassTypeToClsGeneric, namedTypeGenericsGetErased);
			}*/
			
			return nt;
		}
		else if(input instanceof FuncType)
		{
			FuncType fun = (FuncType)input;
			Type ret = mapFuncTypeGenericsToOtherGenerics(fun.retType, superclassTypeToClsGeneric, namedTypeGenericsGetErased, nametoType);
			ArrayList<Type> inputs = new ArrayList<Type>();
			boolean hasBeenInputsGenericTypeQualified = fun.hasBeenInputsGenericTypeQualified;//setting the bool her makes this idempotent
			for(Type arg : fun.getInputs())
			{
				Type mapped = null;
				if(arg instanceof FuncType && namedTypeGenericsGetErased)
				{//functype arguments are erased at runtime
					ArrayList<Type> theoneinputs = new ArrayList<Type>();
					for(Type x : ((FuncType)arg).getInputs())
					{
						theoneinputs.add(theONE);
					}
					mapped = new FuncType(theoneinputs, theONE); 
				}
				else
				{
					mapped= mapFuncTypeGenericsToOtherGenerics(arg, superclassTypeToClsGeneric, namedTypeGenericsGetErased, nametoType);
				}
				 
				if(mapped == null || !mapped.equals(arg) ){
					hasBeenInputsGenericTypeQualified = true;
				}
				inputs.add(mapped);
			}
			FuncType reta = new FuncType(inputs, ret);
			reta.setAbstarct(fun.isAbstarct());
			reta.setFinal(fun.isFinal());
			reta.hasBeenInputsGenericTypeQualified = hasBeenInputsGenericTypeQualified;
			reta.setLambdaDetails(fun.getLambdaDetails());
			reta.setArrayLevels(fun.getArrayLevels());
			reta.origonatingFuncDef = fun.origonatingFuncDef;//MAYBE BUG
			reta.setOrigonalGenericTypeUpperBound(input.getOrigonalGenericTypeUpperBoundRaw());
			reta.definedLocation = fun.definedLocation;
			reta.extFuncOn = fun.extFuncOn;
			reta.setNullStatus(fun.getNullStatus());
			
			if(null != fun.getLocalGenerics()) {
				reta.setLocalGenerics(fun.getLocalGenerics());
			}
			
			reta.localGenBindingLast = fun.localGenBindingLast;
			
			return reta;
		}
		else
		{
			return input;
		}
	}
	
	private static Type getFromMapOrObject(Map<GenericType, Type> fromClassGenericToQualifiedType, GenericType get)
	{
		Type ret = fromClassGenericToQualifiedType == null? null : fromClassGenericToQualifiedType.get(get);
		if(null == ret)
		{
			//ret = new NamedType(new ClassDefJava(Object.class));
			ret = get;
		}else {
			if(get.getNullStatus() == NullStatus.NULLABLE) {
				ret = (Type)ret.copy();
				ret.setNullStatus(NullStatus.NULLABLE);
			}
		}
		return ret;
	}
	
	public static Type filterOutGenericTypes(Type input, Map<GenericType, Type> fromClassGenericToQualifiedType) {
		return filterOutGenericTypes(input, fromClassGenericToQualifiedType, false);
	}
	
	public static Type filterOutGenericTypes(Type input, Map<GenericType, Type> fromClassGenericToQualifiedType, boolean isreturnType)
	{
		if(input == null)
		{
			return input;
		}
		
		if(input instanceof GenericType)
		{
			GenericType gen  = (GenericType)input;
			//int idx = gen.genIndex;
			Type got = getFromMapOrObject(fromClassGenericToQualifiedType, gen);
			Type replace = (Type)got.copy();
			
			/*if(replace instanceof NamedType)
			{//TODO: this is really awful code... should introduce HasGenericUpper mixin or something...
				NamedType namedReplace = (NamedType) ((NamedType)replace).copy();
				//namedReplace.setArrayLevels(namedReplace.getArrayLevels() + gen.getArrayLevels());
				//namedReplace.origonalGenericTypeUpperBound = gen.upperBound;
				//namedReplace.origonalGenericTypeUpperBound.setArrayLevels(gen.getArrayLevels());
				replace = namedReplace;
			}
			else if(replace instanceof FuncType){
				FuncType namedReplace = (FuncType) ((FuncType)replace).copy();
				//namedReplace.setArrayLevels(namedReplace.getArrayLevels() + gen.getArrayLevels());
				//namedReplace.origonalGenericTypeUpperBound = gen.upperBound;
				//namedReplace.origonalGenericTypeUpperBound.setArrayLevels(gen.getArrayLevels());
				replace = namedReplace;
			}*/
			replace.setArrayLevels((got == gen?0: replace.getArrayLevels()) + gen.getArrayLevels());
			//only set array thing if an actual replacement was made
			
			gen.upperBound.setArrayLevels(gen.getArrayLevels());
			replace.setOrigonalGenericTypeUpperBound(gen.upperBound);
			if(replace.getInOutGenModifier() == null && null != gen.getInOutGenModifier()) {
				replace.setInOutGenModifier(gen.getInOutGenModifier());
			}
			
			if(gen.getNullStatus() != NullStatus.NONNULL ) {
				//replace = (Type)replace.copy();
				if(isreturnType) {
					replace.setNullStatus(gen.getNullStatus());
				}else if(gen.getNullStatus() == NullStatus.UNKNOWN) {
					replace.setNullStatus(gen.getNullStatus());
				}
			}
			
			assert replace != null;
			return replace;
		}
		else if(input instanceof NamedType )
		{//bind the generic stuff of namedtypes
			NamedType named = (NamedType) ((NamedType)input).copy();
			ArrayList<Type> boundGenTypes = new ArrayList<Type>();
			for(Type t : named.getGenericTypeElements())
			{
				boundGenTypes.add(filterOutGenericTypes(t, fromClassGenericToQualifiedType));
			}
			//stuff taken by parent nestor play.Parent[XXX].Child[YYY] //ensuer binding not wiped out...
			
			HashMap<GenericType,Type> parentNestorBackIn = new HashMap<GenericType,Type>();
			for(Pair<String, ArrayList<Type>> item : named.nestorSegments)
			{
				for(Type t : item.getB())
				{
					//Type mappedTo = named.fromClassGenericToQualifiedType.get(t);
					//if(null != mappedTo)
					//{
					if(t instanceof GenericType)
					{
						parentNestorBackIn.put((GenericType)t, filterOutGenericTypes(t, fromClassGenericToQualifiedType));
					}
					//}
					//newbounded.add();
				}
			}
			//TODO: is above really needed - surely only if the classdef has not been set...
			ClassDef clsDef = named.getSetClassDef();
			if(clsDef != null){
				ClassDef par = clsDef.getParentNestor();
				while(par != null){
					for(GenericType tt : par.getClassGenricList()){
						parentNestorBackIn.put(tt, fromClassGenericToQualifiedType.get(tt));
					}
					par = par.getParentNestor();
				}
			}
			//TODO: why do we try both ways to qualify the nested generic params above? - can we remove the first attempt?
			

			//named.nestorSegments = newnestorSegments;
			named.setGenTypes(boundGenTypes);
			named.augmentfromClassGenericToQualifiedType(parentNestorBackIn);
			return named;
		}
		else if(input instanceof FuncType)
		{
			FuncType fun = (FuncType)input;
			Type ret = filterOutGenericTypes(fun.retType, fromClassGenericToQualifiedType, true);
			ArrayList<Type> inputs = new ArrayList<Type>();
			for(Type arg : fun.getInputs())
			{
				inputs.add(filterOutGenericTypes(arg, fromClassGenericToQualifiedType));
			}
			FuncType reto = new FuncType(inputs, ret);//JPT: Nasty..
			reto.setAbstarct(fun.isAbstarct());
			reto.setFinal(fun.isFinal());
			reto.hasBeenInputsGenericTypeQualified = fun.hasBeenInputsGenericTypeQualified;
			reto.setOrigonalGenericTypeUpperBound(fun.getOrigonalGenericTypeUpperBoundRaw());
			reto.setLambdaDetails(fun.getLambdaDetails());
			reto.setArrayLevels(fun.getArrayLevels());
			reto.origonatingFuncDef = fun.origonatingFuncDef;
			reto.setLocalGenerics(fun.getLocalGenerics());
			reto.isClassRefType = fun.isClassRefType;
			reto.signatureExpectedToChange = fun.signatureExpectedToChange;
			reto.setNullStatus(fun.getNullStatus());
			return reto;
		}
		else
		{
			return input;
		}
	}
	
	
	public static ArrayList<Type> repointGenericReferencesInSuperClass(int line, int col, ErrorRaiseable err, List<Type> supertypes, Map<String, GenericType> namesToGenericTypes)
	{
		ArrayList<Type> ret = new  ArrayList<Type>();
		for(Type convert : supertypes)
		{
			ret.add(repointGenericReferencesInSuperClassLeg(line, col, err, convert, namesToGenericTypes));
		}
		return ret;
		
	}
	
	public static HashSet<String> getAllGenericTypesDeclInHierarchy(ClassDef cls)
	{
		HashSet<String> ret = new HashSet<String>();
		ClassDef par = cls.getParentNestor();
		while(null != par)
		{
			for(GenericType gen : par.getClassGenricList())
			{
				ret.add(gen.name);
			}
			par=par.getParentNestor();
		}
		
		return ret;
	}
	
	private static Type repointGenericReferencesInSuperClassLeg(int line, int col, ErrorRaiseable err, Type input, Map<String, GenericType> namesToGenericTypes)
	{
		if(input instanceof GenericType)
		{
			String name = ((GenericType)input).name;
			GenericType replace = namesToGenericTypes.get(name);
			
			if(null == replace)
			{
				err.raiseError(line, col, String.format("%s cannot be resolved to a type", name));
				return input;
			}
			else
			{
				return replace;
			}
		}
		else if(input instanceof NamedType )
		{//bind the generic stuff of namedtypes
			NamedType named = (NamedType) ((NamedType)input).copy();
			List<Type> gens = repointGenericReferencesInSuperClass(line, col, err, named.getGenTypes(), namesToGenericTypes);
			named.setGenTypes(gens);
			
			return named;
		}
		else if(input instanceof FuncType)
		{
			FuncType fun = (FuncType)input;
			Type ret = repointGenericReferencesInSuperClassLeg(line, col, err, fun.retType, namesToGenericTypes);
			ArrayList<Type> gens = repointGenericReferencesInSuperClass(line, col, err, fun.getInputs(), namesToGenericTypes);
			return new FuncType(gens, ret);
		}
		else
		{//dunno if this is right, would you have an int as a generic thing?
			return input;
		}
	}
	
	private final static MapAllGnericToSame mapAllGenSameObj = new MapAllGnericToSame();
	private final static Type theONE = TypeCheckUtils.objectNT;// //new GenericType("$$X$$", -1);
	//TODO: should generic type be bound up or down instead of just to object
	
	/*static
	{//but what if the gen type == X string!?
		//theONE.isInstantiable=false;
	}*/

	public static class MapAllGnericToSame implements Map<Type, Type>
	{//not a hack, honest!
		
		@Override
		public void clear() {
			
		}

		@Override
		public boolean containsKey(Object key) {
			return false;
		}

		@Override
		public boolean containsValue(Object value) {
			return true;
		}

		@Override
		public Set<java.util.Map.Entry<Type, Type>> entrySet() {
			return null;
		}

		@Override
		public Type get(Object key) {
			return theONE;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public Set<Type> keySet() {
			return null;
		}

		@Override
		public Type put(Type key, Type value) {
			return null;
		}

		@Override
		public void putAll(Map<? extends Type, ? extends Type> m) {
			
		}

		@Override
		public Type remove(Object key) {
			return null;
		}

		@Override
		public int size() {
			return 0;
		}

		@Override
		public Collection<Type> values() {
			return null;
		}
		
	}

	public static Type repointGenericTypesToUpperBound(Type tt) {
		if(tt instanceof GenericType) {
			//tt = ((GenericType)tt).upperBound;
			((GenericType)tt).isWildcard=true;
		}else if(tt instanceof FuncType) {
			FuncType asft = (FuncType)tt;
			asft.retType = repointGenericTypesToUpperBound(asft.retType);
			
			ArrayList<Type> newinputs = new ArrayList<Type> (asft.getInputs().size());
			for(Type inp : asft.getInputs()) {
				newinputs.add(repointGenericTypesToUpperBound(inp));
			}
			asft.inputs = newinputs;
		}else if(tt instanceof NamedType) {
			NamedType nat = (NamedType)tt;
			ArrayList<Type> gens = new ArrayList<Type>(nat.getGenericTypeElements().size());
			for(Type inp : nat.getGenericTypeElements()) {
				gens.add(repointGenericTypesToUpperBound(inp));
			}
			nat.setGenTypes(gens);
		}

		tt.setInOutGenModifier(null);
		return tt;
	}
	
}
