package com.concurnas.compiler.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.concurnas.compiler.ast.AccessModifier;
import com.concurnas.compiler.ast.GenericType;
import com.concurnas.compiler.ast.Type;

public class TypeDefTypeProvider {

	//public Type rhsToQualify;
	//public ArrayList<GenericType> generics;

	/*public TypeDefTypeProvider(Type rhsToQualify, ArrayList<GenericType> argToGeneric){
		//this.rhsToQualify = rhsToQualify;
		//this.generics = argToGeneric;
	}*/
	
	
	public HashMap<Integer, Fiveple<Type, ArrayList<GenericType>, AccessModifier, String, String>> argsToTypeAndGens = new HashMap<Integer, Fiveple<Type, ArrayList<GenericType>, AccessModifier, String, String>>();
	public List<Fiveple<Type, ArrayList<GenericType>, AccessModifier, String, String>> alltypeAndGens = new ArrayList<Fiveple<Type, ArrayList<GenericType>, AccessModifier, String, String>>();
	
	public void add(Type rhstpye, ArrayList<GenericType> args, AccessModifier am, String packagea, String name){
		Fiveple<Type, ArrayList<GenericType>, AccessModifier, String, String> inst = new Fiveple<Type, ArrayList<GenericType>, AccessModifier, String, String>(rhstpye, args, am, packagea, name);
		argsToTypeAndGens.put(args.size(), inst);
		alltypeAndGens.add(inst);
	}
	
	public TypeDefTypeProvider hasArgs(int argcnt){
		return argsToTypeAndGens.containsKey(argcnt)?this:null;
	}
	
	public Thruple<Type, AccessModifier, String> qualifyType(ArrayList<Type> toqualifyGens){//assume size matches
		int lenwant = toqualifyGens.size();
		
		Fiveple<Type, ArrayList<GenericType>, AccessModifier, String, String> items = argsToTypeAndGens.get(lenwant);
		
		if(null == items){
			return null;
		}
		
		Type ret = (Type)items.getA().copy();
		ArrayList<GenericType> generics = items.getB();
		if(!generics.isEmpty() && generics.size() == toqualifyGens.size()){
			HashMap<Type, Type> genMapping = new HashMap<Type, Type>();
			HashMap<String, Type> nameToArgMatch = new HashMap<String, Type>();
			for(int n = 0; n < generics.size(); n++){
				GenericType gt = (GenericType)generics.get(n);
				Type tola = toqualifyGens.get(n);
				genMapping.put(gt, tola);
				nameToArgMatch.put(gt.name, tola);
			}
			
			ret = GenericTypeUtils.mapFuncTypeGenericsToOtherGenerics(ret, genMapping, false, nameToArgMatch);
		}
		
		return new Thruple<Type, AccessModifier, String>(ret, items.getC(), items.getD() );
	}
	
	
	/*private void ignoreNonQualiGenerics(Type tola){
		if(tola instanceof NamedType){
			NamedType asNamed = (NamedType)tola;
			asNamed.ignoreNonQualificationfGenerics = true;
			for(Type tt : asNamed.getGenTypes()){
				ignoreNonQualiGenerics(tt);
			}
		}
	}*/
}
