package com.concurnas.compiler.ast;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.objectweb.asm.FieldVisitor;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.runtime.Pair;

public class Annotation extends AbstractExpression implements Expression {

	public static enum AnnotationLocation{
		//these are locations for which it is valid to use the locations modifier below, .e.g @[param, setter]MyAnnot("seting")
		FIELD, CLASS, CLASSDEFPARAM;
	}
	
	public List<Pair<String, Expression>> manyArgs;
	public Expression singleArg;
	public String className;
	public String singleArgFeildName;
	public Integer parameterAnnotaionArg = null;
	public boolean atClassLevel = false;
	public FieldVisitor fieldVisitor;
	public ArrayList<String> locations;
	public AnnotationLocation location = null;
	public ElementType usedAt = null;
	public boolean validAtThisLocation=false;
	public HashMap<String, Type> keyToType =new HashMap<String, Type>();
	public boolean ignoreWhenGenByteCode = false;

	public Annotation(int line, int column, String className, Expression singleArg,List<Pair<String, Expression>> manyArgs, ArrayList<String> locations) {
		super(line, column);
		this.className = className;
		this.singleArg = singleArg;
		this.manyArgs = manyArgs;
		this.locations = locations;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public boolean getCanBeOnItsOwnLine(){
		return true;
	}

	@Override
	public Node copyTypeSpecific() {
		ArrayList<Pair<String, Expression>> manyArgscopy = null;
		if(manyArgs != null){
			manyArgscopy = new ArrayList<Pair<String, Expression>>();
			for(Pair<String, Expression> mm : manyArgs){
				manyArgscopy.add(new Pair<String, Expression>(mm.getA(), mm.getB()));
			}
		}
		
		Annotation ret = new Annotation(line, column, className, singleArg==null?null:(Expression)singleArg.copy(), manyArgscopy, new ArrayList<String>(locations));
		ret.parameterAnnotaionArg=parameterAnnotaionArg;
		ret.location=location;
		ret.ignoreWhenGenByteCode=ignoreWhenGenByteCode;
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();

		
		ret.keyToType = new HashMap<String, Type>();
		for(String key : keyToType.keySet()){
			ret.keyToType.put(key, keyToType.get(key));
		}
		return ret;
	}

	public ArrayList<Thruple<String, Expression, Type>> getArguments() {
		ArrayList<Thruple<String, Expression, Type>> args = new ArrayList<Thruple<String, Expression, Type>>();
		if(this.singleArg != null){
			args.add(new Thruple<String, Expression, Type>(this.singleArgFeildName, this.singleArg, this.keyToType.get(this.singleArgFeildName)));
		}
		else if(this.manyArgs != null && !this.manyArgs.isEmpty()){
			for(Pair<String, Expression> ma : this.manyArgs){
				args.add(new Thruple<String, Expression, Type>(ma.getA(), ma.getB(), this.keyToType.get(ma.getA())));
			}
			//args = this.manyArgs;//:null;
		}
		
		return args;
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

	@Override
	public boolean equals(Object comp){
		if(comp instanceof  Annotation){//good enough for what we need
			return ((Annotation) comp).className.equals(this.className);
		}
		return false;
	}
	
	public int hashCode(){
		return this.className.hashCode();//good enough for what we need
	}
	
}
