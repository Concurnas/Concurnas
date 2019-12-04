package com.concurnas.compiler.ast;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.Visitor;
import com.concurnas.compiler.visitors.datastructs.TheScopeFrame;

public class EnumDef extends CompoundStatement implements HasAnnotations {

	public AccessModifier accessModifier;
	public String enaumName;
	public ClassDefArgs classDefArgs;
	public EnumBlock block;

	public EnumDef(int line, int col, AccessModifier accessModifier, String enaumName, ClassDefArgs classDefArgs, EnumBlock b) {
		super(line, col, true);
		this.accessModifier = accessModifier;
		this.enaumName = enaumName;
		this.classDefArgs = classDefArgs;
		this.block = b;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}

	@Override
	public Node copyTypeSpecific() {
		return this;//really?
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

	private String bcFullName;
	
	public Annotations annotations;
	
	public void setCcFullName(String bcFullName) {
		this.bcFullName = bcFullName;
	}
	
	@Override
	public void setAnnotations(Annotations annotations) {
		this.annotations=annotations;
	}
	
	@Override
	public Annotations getAnnotations(){
		return annotations;
	}
	
	public String bcFullName()
	{
		return bcFullName;
	}

	private TheScopeFrame myScopeFrame = null;
	public ClassDef fakeclassDef;
		
	public TheScopeFrame  getScopeFrame(){
		return myScopeFrame;
	}
	/* 
	public TheScopeFrame getScopeFrameGenIfMissing(TheScopeFrame parent, EnumDef cls) {
		if(null == myScopeFrame)
		{
			myScopeFrame = TheScopeFrame.buildTheScopeFrame_Class(parent, true, cls);
		}
		else
		{
			parent.addChild(cls.classBlock, myScopeFrame);
			myScopeFrame.setParent(parent);
		}
		return myScopeFrame;
	}*/
	
}
