package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.Unskippable;
import com.concurnas.compiler.visitors.Visitor;

public class AnnotationDef extends CompoundStatement implements HasAnnotations, REPLTopLevelComponent {

	public ArrayList<AnnotationDefArg> annotationDefArgs;
	public Annotations annotations;
	public AccessModifier am;
	public String name;
	public Block annotBlock;

	public AnnotationDef(int line, int column, AccessModifier am, String name, Block annotBlock, ArrayList<AnnotationDefArg> annotationDefArgs) {
		super(line, column , true);
		this.am = am;
		this.name = name;
		this.annotBlock = annotBlock;
		this.annotationDefArgs = annotationDefArgs;
	}
	
	@Override
	public void setAnnotations(Annotations annotations) {
		this.annotations = annotations;
	}

	@Override
	public Annotations getAnnotations() {
		return annotations;
	}

	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		

		if(this.canSkipIterativeCompilation && !(visitor instanceof Unskippable)) {
			return null;
		}

		if(visitor instanceof ScopeAndTypeChecker) {
			this.hasErrors = false;
		}
		visitor.pushErrorContext(this);
		Object ret = visitor.visit(this);
		visitor.popErrorContext();
		return ret;
	}

	@Override
	public Node copyTypeSpecific() {
		return null;
	}
	
	private String bcFullName;
	public ClassDef fakeclassDef;
	
	public void setCcFullName(String bcFullName) {
		this.bcFullName = bcFullName;
	}
	
	public String bcFullName()
	{
		return bcFullName;
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
	public boolean getCanReturnAValue(){
		return false;
	}
	

	private boolean canSkipIterativeCompilation=false;
	@Override
	public boolean canSkip() {
		return canSkipIterativeCompilation;
	}

	@Override
	public void setSkippable(boolean skippable) {
		canSkipIterativeCompilation = skippable;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Type getFuncType() {
		return new NamedType(fakeclassDef);
	}

	@Override
	public boolean isNewComponent() {
		return true;
	}
	
	@Override
	public boolean persistant() { 
		return true;
	}
	

	public boolean hasErrors = false;
	@Override
	public void setErrors(boolean hasErrors) {
		this.hasErrors = hasErrors;
	}
	@Override
	public boolean getErrors() {
		return hasErrors;
	}
	
	private boolean supressErrors = false;
	@Override
	public void setSupressErrors(boolean supressErrors) {
		this.supressErrors = supressErrors;
	}
	@Override
	public boolean getSupressErrors() {
		return supressErrors;
	}
}