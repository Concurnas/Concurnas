package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.utils.Thruple;
import com.concurnas.runtime.Pair;

public class ArrayRefLevelElementsHolder {

	private ArrayList<Pair<Boolean, ArrayList<ArrayRefElement>>> items = new ArrayList<Pair<Boolean, ArrayList<ArrayRefElement>>>();
	private ArrayList<Thruple<Type, Type, ARElementType>> typeAtLevel = new ArrayList<Thruple<Type, Type, ARElementType>>();
	
	public enum ARElementType{LIST, MAP, ARRAY, OBJ};
	
	public ArrayRefLevelElementsHolder(){
		
	}
	
	private ArrayRefLevelElementsHolder(ArrayList<Pair<Boolean, ArrayList<ArrayRefElement>>> items, ArrayList<Thruple<Type, Type, ARElementType>> typeAtLevel){
		this.items = items;
		this.typeAtLevel = typeAtLevel;
	}
	
	public void add(boolean isNullSafe, ArrayList<ArrayRefElement> item) {
		Pair<Boolean, ArrayList<ArrayRefElement>> itemx = new Pair<Boolean, ArrayList<ArrayRefElement>>(isNullSafe, item);
		items.add(itemx);
	}
	public void prepend(ArrayList<ArrayRefElement> item) {
		Pair<Boolean, ArrayList<ArrayRefElement>> itemx = new Pair<Boolean, ArrayList<ArrayRefElement>>(false, item);
		 items.add(0, itemx);
	}
	
	public ArrayList<Pair<Boolean, ArrayList<ArrayRefElement>>> getAll() {
		return items;
	}
	
	public ArrayList<ArrayRefElement> flatten(){
		ArrayList<ArrayRefElement> ret = new ArrayList<ArrayRefElement>();
		
		for(Pair<Boolean, ArrayList<ArrayRefElement>> inst : items) {
			ret.addAll(inst.getB());
		}
		
		return ret;
	}

	public boolean isEmpty(){
		return items.isEmpty();
	}
	
	public ArrayRefElement getLastArrayRefElement(){
		Pair<Boolean,ArrayList<ArrayRefElement>> lastEsx = items.get(items.size()-1);
		ArrayList<ArrayRefElement> lastEs = lastEsx.getB();
		
		return lastEs.get(lastEs.size()-1);
	}

	public void tagType(Type retSoFar, Type castTo, ARElementType ele ) {
		typeAtLevel.add(new Thruple<Type, Type, ARElementType>(retSoFar, castTo, ele));
	}
	
	public Thruple<Type, Type, ARElementType> getTaggedType(int n){
		return typeAtLevel.get(n);
	}
	
	public Thruple<Type, Type, ARElementType> getLastTaggedType(){
		return typeAtLevel.get(typeAtLevel.size()-1);
	}

	public void reset() {
		typeAtLevel = new ArrayList<Thruple<Type, Type, ARElementType>>();
	}
	
	@Override
	public ArrayRefLevelElementsHolder clone(){
		ArrayList<Pair<Boolean, ArrayList<ArrayRefElement>>> items = new ArrayList<Pair<Boolean, ArrayList<ArrayRefElement>>>();
		ArrayList<Thruple<Type, Type, ARElementType>> typeAtLevel = new ArrayList<Thruple<Type, Type, ARElementType>>();
		
		for(Pair<Boolean, ArrayList<ArrayRefElement>> aresx : this.items){
			ArrayList<ArrayRefElement> areae = new ArrayList<ArrayRefElement>();
			for(ArrayRefElement ar : aresx.getB()){
				areae.add((ArrayRefElement)ar.copyTypeSpecific());
			}
			items.add(new Pair<Boolean, ArrayList<ArrayRefElement>>(aresx.getA(), areae) );
		}
		
		for(Thruple<Type, Type, ARElementType> tal : this.typeAtLevel){
			typeAtLevel.add(new Thruple<Type, Type, ARElementType>((Type)((Node)tal.getA()).copyTypeSpecific(), (Type)((Node)tal.getB()).copyTypeSpecific(), tal.getC()));
		}
		
		ArrayRefLevelElementsHolder ret = new ArrayRefLevelElementsHolder(items,  typeAtLevel);
		
		
		return ret;
	}
}
