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
	
	public static class TypeDef{
		public Type rhstpye;
		public ArrayList<GenericType> args;
		public AccessModifier am;
		public String name;
		public String packagea;//?
		public Type defType;

		public TypeDef(Type rhstpye, ArrayList<GenericType> args, AccessModifier am, String packagea, String name, Type defType) {
			this.rhstpye=rhstpye;
			this.args=args;
			this.am=am;
			this.packagea=packagea;
			this.name=name;
			this.defType=defType;
		}
	}
	
	public HashMap<Integer, TypeDef> argsToTypeAndGens = new HashMap<Integer, TypeDef>();
	public List<TypeDef> alltypeAndGens = new ArrayList<TypeDef>();
	
	public void add(Type rhstpye, ArrayList<GenericType> args, AccessModifier am, String packagea, String name, Type defType){
		TypeDef inst = new TypeDef(rhstpye, args, am, packagea, name, defType);
		argsToTypeAndGens.put(args.size(), inst);
		alltypeAndGens.add(inst);
	}
	
	public TypeDefTypeProvider hasArgs(int argcnt){
		return argsToTypeAndGens.containsKey(argcnt)?this:null;
	}
	
	public Thruple<Type, AccessModifier, String> qualifyType(ArrayList<Type> toqualifyGens, boolean usedToCreateNewObj){//assume size matches
		int lenwant = toqualifyGens.size();
		
		TypeDef items = argsToTypeAndGens.get(lenwant);
		
		if(null == items){
			return null;
		}
		

		Type ret = (Type)items.rhstpye.copy();
		if(usedToCreateNewObj && null != items.defType) {
			//use default if there is one
			ret = (Type)items.defType.copy();
		}else {
			ret = (Type)items.rhstpye.copy();
		}
		
		ArrayList<GenericType> generics = items.args;
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
		
		return new Thruple<Type, AccessModifier, String>(ret, items.am, items.packagea );
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
