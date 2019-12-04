package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.Vector;

import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.ast.interfaces.FuncDefI;
import com.concurnas.compiler.visitors.NotifyOnError;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.VectorizedRedirector;
import com.concurnas.compiler.visitors.Visitor;

public class DotOperator extends AbstractExpression implements Expression, CanBeInternallyVectorized/*, NotifyOnError*/ {

	//public Expression head;
	private ArrayList<Expression> elements;
	public ArrayList<Expression> adjustedElements;
	
	private ArrayList<Boolean> isDirectAccess;
	public ArrayList<Boolean> adjustedisDirectAccess;
	
	public ArrayList<Boolean> returnCalledOn;
	public ArrayList<Boolean> safeCall;
	
	private Expression vectorizedRedirect;
	//public boolean didStaticAdjustments=false;

	public DotOperator(int line, int col, Expression head, ArrayList<Expression> elements, ArrayList<Boolean> isDirectAccess, ArrayList<Boolean> returnCalledOn, ArrayList<Boolean> safeCall) {
		super(line, col);//TODO: head stuff here needs refactoring it's insane
		//this.head = head;
		this.elements = elements;
		this.elements.add(0, head);
		this.isDirectAccess = isDirectAccess;
		this.returnCalledOn = returnCalledOn;
		this.safeCall = safeCall;
		posProcessElements();
	}
	
	public DotOperator(int line, int col, ArrayList<Expression> elements, ArrayList<Boolean> isDirectAccess, ArrayList<Boolean> returnCalledOn, ArrayList<Boolean> safeCall) {
		super(line, col);//TODO: head stuff here needs refactoring it's insane
		//this.head = head;
		this.elements = elements;
		this.isDirectAccess = isDirectAccess;
		this.returnCalledOn = returnCalledOn;
		this.safeCall = safeCall;
		posProcessElements();
	}
	
	public DotOperator(int line, int col, ArrayList<Expression> elements, String... variant) {
		super(line, col);
		this.elements = elements;
		int sz = variant.length;
		ArrayList<Boolean> isDirectAccess = new ArrayList<Boolean>(sz);
		ArrayList<Boolean> returnCalledOn = new ArrayList<Boolean>(sz);
		ArrayList<Boolean> safeCall = new ArrayList<Boolean>(sz);
		for(int n=0; n < sz; n++){
			//'.'|'\\.'|'..'|'?.'
			boolean da = false;
			boolean rca = false;
			boolean sc = false;
			
			switch(variant[n]) {
				case ".": break;
				case "\\.": da=true; break;
				case "..":  rca=true; break;
				case "?.":  sc=true; break;
			}
			
			isDirectAccess.add(da);
			returnCalledOn.add(rca);
			safeCall.add(sc);
		}
		
		this.returnCalledOn = returnCalledOn;
		this.isDirectAccess = isDirectAccess; 
		this.safeCall = safeCall; 
		posProcessElements();
	}
	
	@Override
	public boolean getCanBeOnItsOwnLine(){
		return elements.get(elements.size()-1).getCanBeOnItsOwnLine();
	}
	
	public DotOperator(int line, int col, Expression lhs, Expression rhs){
		super(line, col);
		this.elements = new ArrayList<Expression>();
		this.isDirectAccess = new ArrayList<Boolean>();
		this.elements.add(lhs);
		this.elements.add(rhs);
		this.returnCalledOn = new ArrayList<Boolean>();
		this.returnCalledOn.add(false);
		this.safeCall = new ArrayList<Boolean>();
		this.safeCall.add(false);
		
		this.isDirectAccess.add(false);
		posProcessElements();
	}
	

	/**
	 * remove last. If last is self ref or direct, then return null, else thing removed
	 * @return
	 */
	public Expression removeLast() {
		Expression ret = elements.remove(elements.size()-1);
		boolean isRetSelf = returnCalledOn.remove(returnCalledOn.size()-1);
		boolean isDirect = isDirectAccess.remove(isDirectAccess.size()-1);
		boolean isSafe = safeCall.remove(safeCall.size()-1);
		
		return isRetSelf || isDirect|| isSafe ?null: ret;
	}
	
	private DotOperator addToT(Expression header, boolean ret, boolean direct, boolean isSafe){
		elements.add(header);
		returnCalledOn.add(ret);
		isDirectAccess.add(direct);
		safeCall.add(isSafe);
		return this;
	}
	
	public DotOperator addToTail(Expression header){
		return addToT(header, false, false, false);
	}
	
	public DotOperator addToTailDirect(Expression header){
		return addToT(header, false, true, false);
	}
	
	public DotOperator addToTailReturn(Expression header){
		return addToT(header, true, false, false);
	}
	

	public void add(DotOperator fullDop) {
		this.elements.addAll(fullDop.elements);
		this.returnCalledOn.add(false);
		this.isDirectAccess.add(false);
		
		this.returnCalledOn.addAll(fullDop.returnCalledOn);
		this.isDirectAccess.addAll(fullDop.isDirectAccess);
		this.safeCall.addAll(fullDop.safeCall);
	}

	public void add(Expression fullDop) {
		addToTail(fullDop);
	}
	
	private DotOperator addToH(Expression header, boolean ret, boolean direct, boolean isSafe){
		elements.add(0, header);
		returnCalledOn.add(0, ret);
		isDirectAccess.add(0, direct);
		safeCall.add(0, isSafe);
		return this;
	}
	public DotOperator addToHead(Expression header){
		return addToH(header, false, false, false);
	}
	
	public DotOperator addToHeadDirect(Expression header){
		return addToH(header, false, true, false);
	}
	
	public DotOperator addToHeadReturn(Expression header){
		return addToH(header, true, false, false);
	}
	
	private void posProcessElements(){
		for(Expression e: elements){
			if(e instanceof RefName){
				((RefName)e).isIsolated=true;
			}
		}
	}
	
	private DotOperator(int line, int col){super(line, col);};
	
	@Override
	public Node copyTypeSpecific() {
		DotOperator ret = new DotOperator(super.getLine(), super.getColumn());
		ret.elements = (ArrayList<Expression>) Utils.cloneArrayList(elements);
		ret.adjustedElements = this.adjustedElements==null?null:(ArrayList<Expression>) Utils.cloneArrayList(adjustedElements);
		ret.isDirectAccess=this.isDirectAccess==null?null:new ArrayList<Boolean>(isDirectAccess);
		ret.safeCall=this.safeCall==null?null:new ArrayList<Boolean>(safeCall);
		ret.returnCalledOn=this.returnCalledOn==null?null:new ArrayList<Boolean>(returnCalledOn);
		ret.preceedingExpression = preceedingExpression==null?null:(Expression)preceedingExpression.copy();
		ret.vectorizedRedirect = vectorizedRedirect==null?null: (Block)vectorizedRedirect.copy();

		//ret.didStaticAdjustments=didStaticAdjustments;
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
	
	public static DotOperator buildDotOperatorOne(int line, int col, Expression head, Expression other) {
		return buildDotOperatorOne(line, col, head, other, true, false, false);
	}
	
	public static DotOperator buildDotOperatorOneNonDirect(int line, int col, Expression head, Expression other) {
		return buildDotOperatorOne(line, col, head, other, false, false, false);
	}
	
	public static DotOperator buildDotOperatorOneNonDirectNullSafe(int line, int col, Expression head, Expression other, boolean nullsafe) {
		return buildDotOperatorOne(line, col, head, other, false, false, nullsafe);
	}
	
	public static DotOperator buildDotOperatorOneReturn(int line, int col, Expression head, Expression other) {
		return buildDotOperatorOne(line, col, head, other, false, true, false);
	}
	
	private static DotOperator buildDotOperatorOne(int line, int col, Expression head, Expression other, boolean direct, boolean ret, boolean isSafe) {
		ArrayList<Boolean> isDirectAccess = new ArrayList<Boolean>();
		isDirectAccess.add(direct);
		
		ArrayList<Boolean> returnCalledOn = new ArrayList<Boolean>();
		returnCalledOn.add(ret);
		
		ArrayList<Boolean> isSafec = new ArrayList<Boolean>();
		isSafec.add(isSafe);
		
		
		ArrayList<Expression> elements = new ArrayList<Expression>();
		elements.add(other);
		
		return new DotOperator(line, col, head, elements, isDirectAccess, returnCalledOn, isSafec);
	}
	
	public static DotOperator buildDotOperator(int line, int col, String... elements) {
		Expression[] refNames = new  Expression[elements.length];
		int n=0;
		for(String str: elements){
			refNames[n++] = new RefName(line, col, str);
		}
		
		return buildDotOperator(line, col, refNames);
	}
	
	public static DotOperator buildDotOperatorPrefixing(int line, int col, ArrayList<Expression> toRefName, boolean isDirect, boolean isRefSelf, Expression other) {
		ArrayList<Boolean> isDirectAccess = new ArrayList<Boolean>();
		ArrayList<Boolean> returnCalledOn = new ArrayList<Boolean>();
		ArrayList<Boolean> safeCall = new ArrayList<Boolean>();
		int n=0;
		int sz = toRefName.size()-1;
		while(n++ < sz){
			isDirectAccess.add(false);
			returnCalledOn.add(false);
			safeCall.add(false);
		}
		
		DotOperator ret = new DotOperator(line, col, toRefName, isDirectAccess, returnCalledOn, safeCall);
		
		
		ret.addToT(other, isRefSelf, isDirect, false);
		return ret;
	}
	
	public static DotOperator buildDotOperator(int line, int col, Expression... elements) {
		ArrayList<Expression> expr = new ArrayList<Expression>(elements.length);
		
		for(Expression e : elements){
			expr.add(e);
		}
		
		return new DotOperator(line, col, expr);
	}
	
	public DotOperator(int line, int col, ArrayList<Expression> elements) {
		this(line, col, elements, true);
	}
	
	public DotOperator(int line, int col, ArrayList<Expression> elements, boolean directAccess) {
		super(line, col);
		this.elements = elements;
		int sz = elements.size();
		ArrayList<Boolean> isDirectAccess = new ArrayList<Boolean>(sz);
		ArrayList<Boolean> returnCalledOn = new ArrayList<Boolean>(sz);
		ArrayList<Boolean> safeCall = new ArrayList<Boolean>(sz);
		for(int n=0; n < sz; n++){
			isDirectAccess.add(directAccess);
			returnCalledOn.add(false);
			safeCall.add(false);
		}
		
		this.returnCalledOn = returnCalledOn;
		this.isDirectAccess = isDirectAccess; 
		this.safeCall = safeCall; 
	}
	
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		
		if(null != vectorizedRedirect){
			if(!(visitor instanceof VectorizedRedirector)){
				return vectorizedRedirect.accept(visitor);
			}
		}
		
		if(this.elements.size() == 1) {
			return this.elements.get(0).accept(visitor);
		}
		
		
		return visitor.visit(this);
	}
	
	public Expression getHead(Visitor vis)
	{
		return this.getElements(vis).get(0);
	}
	
	public Expression getLastElement()
	{
		return this.elements.get(this.elements.size()-1);
	}
	
	public boolean getLastReturnCallOnSelf()
	{
		return returnCalledOn==null?false:returnCalledOn.get(returnCalledOn.size()-1);
	}
	
	public Expression getPenultimate()//one before last
	{
		return this.elements.size() >= 2?this.elements.get(this.elements.size()-2): null;
	}
	
	
	
	public void replaceLastElement(Expression with){
		this.elements.set(this.elements.size()-1, with);
	}
	
	@Override
	public void setShouldBePresevedOnStack(boolean should)
	{
		((Node)getLastElement()).setShouldBePresevedOnStack(should);
		super.setShouldBePresevedOnStack(should);
	}
	
	public boolean isPermissableToGoOnRHSOfADot(Node preceededBy){
		return this.elements.get(0).isPermissableToGoOnRHSOfADot(null);
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(Expression e : this.elements){
			sb.append(e.toString());
			sb.append("\n.\n");
		}
		
		return sb.toString();
	}

	public ArrayList<Expression> getElements(Visitor vis) {
		if(vis instanceof ScopeAndTypeChecker){
			return elements;
		}
		
		return this.adjustedElements != null? adjustedElements:elements;
	}

	public ArrayList<Boolean> getIsDirectAccess(Visitor vis) {
		if(vis instanceof ScopeAndTypeChecker){
			return isDirectAccess;
		}
		
		return this.adjustedElements != null? adjustedisDirectAccess:isDirectAccess;
	}
	
	public void snipOffEnd(){
		isDirectAccess.remove(isDirectAccess.size()-1);
		returnCalledOn.remove(returnCalledOn.size()-1);
		safeCall.remove(safeCall.size()-1);
		elements.remove(elements.size()-1);
	}
	
	
	@Override
	public boolean hasVectorizedRedirect() {
		return this.vectorizedRedirect != null;
	}

	@Override
	public void setVectorizedRedirect(Node vectRedirect) {
		this.vectorizedRedirect = (Expression)vectRedirect;
	}

	
	private boolean hasErrored=false;
	@Override
	public boolean hasErroredAlready() {
		return hasErrored;
	}
	@Override
	public void setHasErroredAlready(boolean hasError) {
		this.hasErrored=hasError;
	}

	
	@Override
	public void setPreceededByThis(boolean preceededByThis) {
		super.setPreceededByThis(preceededByThis);
		((Node)this.elements.get(0)).setPreceededByThis(preceededByThis);
	}
}
