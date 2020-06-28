package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.util.NullableArrayElementss;
import com.concurnas.compiler.utils.Thruple;

public class ArrayRefLevelElementsHolder {

	private ArrayList<NullableArrayElementss> items = new ArrayList<NullableArrayElementss>();
	private ArrayList<Thruple<Type, Type, ARElementType>> typeAtLevel = new ArrayList<Thruple<Type, Type, ARElementType>>();
	
	public enum ARElementType{LIST, MAP, ARRAY, OBJ};
	
	public ArrayRefLevelElementsHolder(){
		
	}
	
	private ArrayRefLevelElementsHolder(ArrayList<NullableArrayElementss> items, ArrayList<Thruple<Type, Type, ARElementType>> typeAtLevel){
		this.items = items;
		this.typeAtLevel = typeAtLevel;
	}
	
	
	public void add(boolean isNullSafe, boolean noNullAssertion, ArrayList<ArrayRefElement> item) {
		NullableArrayElementss itemx = new NullableArrayElementss(isNullSafe, noNullAssertion, item);
		items.add(itemx);
	}
	public void prepend(ArrayList<ArrayRefElement> item) {
		NullableArrayElementss itemx = new NullableArrayElementss(false, false, item);
		items.add(0, itemx);
	}
	
	public ArrayList<NullableArrayElementss> getAll() {
		return items;
	}
	
	public ArrayList<ArrayRefElement> flatten(){
		ArrayList<ArrayRefElement> ret = new ArrayList<ArrayRefElement>();
		
		for(NullableArrayElementss inst : items) {
			ret.addAll(inst.elements);
		}
		
		return ret;
	}

	public boolean isEmpty(){
		return items.isEmpty();
	}
	
	public ArrayRefElement getLastArrayRefElement(){
		NullableArrayElementss lastEsx = items.get(items.size()-1);
		ArrayList<ArrayRefElement> lastEs = lastEsx.elements;
		
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
		ArrayList<NullableArrayElementss> items = new ArrayList<NullableArrayElementss>();
		ArrayList<Thruple<Type, Type, ARElementType>> typeAtLevel = new ArrayList<Thruple<Type, Type, ARElementType>>();
		
		for(NullableArrayElementss aresx : this.items){
			ArrayList<ArrayRefElement> areae = new ArrayList<ArrayRefElement>();
			for(ArrayRefElement ar : aresx.elements){
				areae.add((ArrayRefElement)ar.copyTypeSpecific());
			}
			items.add(new NullableArrayElementss(aresx.nullsafe, aresx.nna, areae) );
		}
		
		for(Thruple<Type, Type, ARElementType> tal : this.typeAtLevel){
			typeAtLevel.add(new Thruple<Type, Type, ARElementType>((Type)((Node)tal.getA()).copyTypeSpecific(), (Type)((Node)tal.getB()).copyTypeSpecific(), tal.getC()));
		}
		
		ArrayRefLevelElementsHolder ret = new ArrayRefLevelElementsHolder(items,  typeAtLevel);
		
		
		return ret;
	}
}
