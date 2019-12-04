package com.concurnas.compiler.visitors.lca;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.concurnas.bootstrap.runtime.cps.CObject;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.ast.GenericType;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.utils.GenericTypeUtils;

public class ClassDefTree
{
	private List<ClassDefTree> superClases = new ArrayList<ClassDefTree>();
	private NamedType node;
	
	private static final String obj = "java.lang.Object";
	
	public ClassDefTree(NamedType node)
	{
		this.node = node;
		/*
		for(ClassDef s : node.getSetClassDef().getAllSuperClassesInterfaces())
		{
			superClases.add(new ClassDefTree(new NamedType(s)));
		}*/
		
		//next line needs to qualify generics
		ClassDef supClass = node == null?null:node.getSetClassDef();
		if(null == supClass){
			return;
		}
		ArrayList<GenericType> gens = supClass.classGenricList;
		
		if(!this.node.hasGenTypes()){
			ArrayList<Type> takeGens = new ArrayList<Type>(gens.size());
			for(GenericType gen : gens){
				takeGens.add(gen);
			}
			this.node.setGenTypes(takeGens);
		}
		
		if(null != supClass){
			List<NamedType>  supi = node.getSetClassDef().getAllSuperClassesInterfaces();
			
			HashMap<GenericType, Type> superGenTypes = node.getFromClassGenericToQualifiedType();
			
			for(NamedType s : supi)
			{
				s = (NamedType) GenericTypeUtils.filterOutGenericTypes(s, superGenTypes);
				superClases.add(new ClassDefTree(s));
			}
		}
	}

	private LinkedHashSet<NamedType> typeHeirarchy = null;
	
	private static final NamedType typeObj = new NamedType(new ClassDefJava(Object.class));//TODO: really need to define these just once
	private static final NamedType typeCObj = new NamedType(new ClassDefJava(CObject.class));//TODO: really need to define these just once
	
	public LinkedHashSet<NamedType> getTypeHierarchy() {
		if(null == typeHeirarchy)
		{
			//TODO: it would be able to create an anon class in here
			//Breadth first search
			Queue<ClassDefTree> q = new LinkedList<ClassDefTree>();
			
			LinkedHashSet<NamedType> ret = new LinkedHashSet<NamedType>();

			node = node.equals(typeCObj)?typeObj:node;
			
			int hreadArrayLevels =  (int)node.getArrayLevels();
			
			ret.add(  node  );// new NamedType(node.getSetClassDef(), hreadArrayLevels)   );
			//ret.add( new NamedType(node.getSetClassDef(), hreadArrayLevels)   );
			q.addAll(this.superClases);
			
			while(!q.isEmpty())
			{
				ClassDefTree head = q.remove();
				if(null!= head.node && !head.node.getPrettyName().equals(obj))
				{
					if(!ret.contains(head.node))
					{
						NamedType nodola = head.node.copyTypeSpecific();
						nodola = nodola.equals(typeCObj)?typeObj:nodola;
						nodola.setArrayLevels(hreadArrayLevels);
						ret.add(nodola);
					}
					for(ClassDefTree dude : head.superClases)
					{
						q.add(dude);
					}
				}
			}
			
			typeHeirarchy = ret;
		}
		
		return typeHeirarchy;
	}
	
	public boolean isThingPassedParent(ClassDef toCheck)
	{
		String ntNAme = toCheck.getPrettyName() ;
		LinkedHashSet<NamedType>  nts = getTypeHierarchy();
		for(NamedType nt : nts )
		{
			String pname = nt.getSetClassDef().getPrettyName();
			if(pname.equals(ntNAme))
			{
				return true;
			}
		}
		
		return false;
	}
	
}