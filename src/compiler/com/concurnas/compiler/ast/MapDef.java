package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.compiler.visitors.Visitor;

public class MapDef extends AbstractExpression implements Expression {

	public ArrayList<IsAMapElement> elements;
	public MapDefaultElement defaultMapElement = null;

	public MapDef(int line, int col, ArrayList<IsAMapElement> elements) {
		super(line, col);
		this.elements = elements;
	}
	
	@Override
	public Node copyTypeSpecific() {
		MapDef ret = new MapDef(super.getLine(), super.getColumn(), (ArrayList<IsAMapElement>) Utils.cloneArrayList(elements));
		ret.defaultMapElement = this.defaultMapElement;
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();

		return ret;
	}
	private Expression preceedingExpression;
	@Override
	public void setPreceedingExpression(Expression expr) {
		this.preceedingExpression = expr;
	}
	@Override
	public Expression getPreceedingExpression() {
		return preceedingExpression;
	}

	public MapDef(int line, int col) {
		super(line, col);
		elements = null;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}

	private static Set<String> mapTypes = new HashSet<String>();
	static{
		mapTypes.add("java.util.HashMap");
		mapTypes.add("java.util.Map");
		mapTypes.add("java.util.AbstractMap");
		mapTypes.add("com.concurnas.lang.DefaultMap");
	}
	
	@Override
	public Type setTaggedType(Type proposed){
		Type currentTaggedType = super.getTaggedType();
		
		if(currentTaggedType == null){
			currentTaggedType = proposed;
		}
		
		if(currentTaggedType != null){
			NamedType ntSrc = (NamedType)currentTaggedType;
			ClassDef clsSrc = ntSrc.getSetClassDef();
			if(clsSrc != null && mapTypes.contains(clsSrc.toString())){
				ArrayList<Type> gensSrc = ntSrc.getGenericTypeElements();
				if(gensSrc.size() == 2){
					Type sourceKeyType = gensSrc.get(0);
					Type sourceValType = gensSrc.get(1);
					
					
					if(proposed != null  ){
						
						NamedType nt = (NamedType)proposed;
						ClassDef cls = nt.getSetClassDef();
						
						if(cls != null && mapTypes.contains(cls.toString())){
							ArrayList<Type> gens = nt.getGenericTypeElements();
							if(gens.size() == 2){
								Type keyType = gens.get(0);
								Type valType = gens.get(1);
								
								if(!TypeCheckUtils.isNamedTypeOrLambda(keyType)){
									keyType = sourceKeyType;//if it's an innaproperiate qualifier for a VarNull list
								}
								
								if(!TypeCheckUtils.isNamedTypeOrLambda(valType)){
									valType = sourceValType;//if it's an innaproperiate qualifier for a VarNull list
								}
								
								NamedType propFiddle = (NamedType)nt.copy();
								
								ArrayList<Type> newGenTypes = new ArrayList<Type>();
								newGenTypes.add(keyType);
								newGenTypes.add(valType);
								
								propFiddle.setGenTypes(newGenTypes);
								proposed = propFiddle;
								
								for(IsAMapElement ea : elements){
									if(ea instanceof MapDefElement){
										MapDefElement e = (MapDefElement)ea;
										
										//e.key.setTaggedType(keyType);
										//e.value.setTaggedType(valType);
										
										Utils.setTypeOnNullListDef((Node)e.getKey(null),  (Type)keyType.copy()  );
										Utils.setTypeOnNullListDef((Node)e.getValue(null),  (Type)valType.copy()  );
										
										e.keyType = keyType;
										e.valType = valType;
									}
									else{
										MapDefaultElement def = (MapDefaultElement)ea;

										Utils.setTypeOnNullListDef((Node)def.value,  (Type)valType.copy()  );
										def.valType = valType;
										def.keyType = keyType;
										
										//if returns straight null - very much edgecase, i.e,e default -> null
										
										if(def.astRedirect != null && def.astRedirect.returnType instanceof VarNull){
											def.astRedirect.returnType = valType;
										}
										
									}
								}
								
								
								
							}
						}
					}
				}
			}
		}
		return super.setTaggedType(proposed);
	}
	
}
