package com.concurnas.compiler.visitors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import com.concurnas.compiler.CaseExpression;
import com.concurnas.compiler.CaseExpressionAnd;
import com.concurnas.compiler.CaseExpressionAssign;
import com.concurnas.compiler.CaseExpressionAssignTuple;
import com.concurnas.compiler.CaseExpressionObjectTypeAssign;
import com.concurnas.compiler.CaseExpressionOr;
import com.concurnas.compiler.CaseExpressionPost;
import com.concurnas.compiler.CaseExpressionPre;
import com.concurnas.compiler.CaseExpressionTuple;
import com.concurnas.compiler.CaseExpressionWrapper;
import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.JustAlsoCaseExpression;
import com.concurnas.compiler.TypedCaseExpression;
import com.concurnas.compiler.ast.*;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.ast.util.JustLoad;
import com.concurnas.compiler.visitors.util.MactchCase;
import com.concurnas.runtime.Pair;

public class PrintSourceVisitor implements Visitor {

	protected int lastLineVisited = -1;
	public ArrayList<String> items = new ArrayList<String>();
	//public ArrayList<String> visitList = new ArrayList<String>();//TODO: remove the visitList
	private HashSet<String> synthNamesIgnore = new HashSet<String>();
	protected int indentLevel = 0;
	
	public boolean showGenericsAsUpperBound=false;
	private Stack<ArrayList<String>> steal = new Stack<ArrayList<String>>();
	public boolean showGenericInOut=true;
	public PrintSourceVisitor(){}	
	//TODO: get rid of visitList
	public PrintSourceVisitor(String[] sythnNamesToIgnore){
		for(String s : sythnNamesToIgnore){
			synthNamesIgnore.add(s);
		}
	}
	
	public void pushErrorContext(FuncDef xxx) {}
	public FuncDef popErrorContext() {return null;}
	
	@Override
	public Object visit(ArrayDefComplex arrayDefComplex){
		this.addItem("[");
		for(Expression e : arrayDefComplex.getArrayElements(this))
		{
			e.accept(this);
			this.addItemNoPreString(";");
		}
		if(!arrayDefComplex.getArrayElements(this).isEmpty()) popLastItem();
		this.addItem("]");
		return null;
	}
	
	@Override
	public Object visit(EnumBlock enumBlock){
		this.addItem("{\n");
		for(EnumItem ei :  enumBlock.enumItemz){
			ei.accept(this);
			this.addItemNoPreString(",");
		}
		popLastItem();
		
		if(!enumBlock.mainBlock.isEmpty()){
			this.addItem("\n");
		}
		
		visitBlock(enumBlock.mainBlock, false);
		this.addItem("\n");
		
		this.addItem("}\n\n");
		return null;
	}
	
	protected void addItem(String s, boolean ident){
		addItem(s);
	}
	
	protected void addItem(String s){
		items.add("" + s);
	}
	
	protected void addItemNoPreString(String s){
		items.add(s);
	}
	
	private void addNamePreDollarLevel(String name){
		if(name==null){
			name = "";
		}
		else if(name.contains("$n")){
			name = name.substring(0, name.lastIndexOf("$n"));
		}
		this.addItem(name);
		
	}
	
	@Override
	public Collection<? extends ErrorHolder> getErrors() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public int getLastLineVisited(){
		return lastLineVisited;
	}
	
	private void popLastItem()
	{
		this.items.remove(this.items.size()-1);
	}
	
	private void printInOutGenModi(Type thing){
		if(showGenericInOut){
			InoutGenericModifier genmod = thing.getInOutGenModifier();
			if(genmod != null){
				this.addItem(genmod.toString());
			}
		}
	}
	
	public void processArrayRefElements(ArrayRefLevelElementsHolder elements){
		for(Pair<Boolean, ArrayList<ArrayRefElement>> bracksetx : elements.getAll())
		{
			ArrayList<ArrayRefElement> brackset = bracksetx.getB();
			if(bracksetx.getA()) {
				this.addItem("?");
			}
			this.addItem("[");
			for(int n = 0; n < brackset.size(); n++)
			{	
				brackset.get(n).accept(this);
				if(n != brackset.size()-1)
				{
					this.addItemNoPreString(", ");
				}
			}
			this.addItem("]");
		}
	}
	
	public void reset(){
		items.clear();
		showGenericsAsUpperBound=false;
	}

	public void resetLastLineVisited(){
		lastLineVisited = -1;
	}
	
	public void setLastLineVisited(int lineNo){
		return;//ignore
	}

	private void stealItems(){
		steal.push(items);
		items = new ArrayList<String>();
	}

	private void stopStealingItems(){
		items = steal.pop();
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder(); 
		int sz = items.size();
		for(int idx =0; idx < sz; idx++){
			String i =  items.get(idx);
			sb.append(i);
			if(idx != sz-1){
				sb.append(" ");
			}
			
		}
		
		return sb.toString();
	}

	@Override
	public Object visit(Additive addMinusExpression) {
		//visitList.add("AddMinusExpression");
		addMinusExpression.head.accept(this);
		for(AddMinusExpressionElement i : addMinusExpression.elements)
		{
			i.accept(this);
		}
		return null;
	}
	
	@Override
	public Object visit(AddMinusExpressionElement addMinusExpressionElement) {
		//visitList.add("AddMinusExpressionElement");
		this.addItem(addMinusExpressionElement.isPlus?"+":"-");
		addMinusExpressionElement.exp.accept(this);
		return null;
	}
	
	@Override
	public Object visit(AndExpression andExpression) {
		//visitList.add("AndExpression");
		andExpression.head.accept(this);
		for(Expression i : andExpression.things)
		{
			this.addItem("and");
			i.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(Annotation annotation){
		StringBuilder locations = new StringBuilder();
		if(!annotation.locations.isEmpty()){
			locations.append("[");
			int sz = annotation.locations.size();
			for(int n=0; n < sz; n++){
				locations.append(annotation.locations.get(n));
				if(n != sz-1){
					locations.append(", ");
				}
			}
			locations.append("]");
		}
		
		this.addItem("@" + locations + annotation.className);
		boolean hasSingleArg=annotation.singleArg != null;
		if(hasSingleArg || (annotation.manyArgs != null && !annotation.manyArgs.isEmpty()) ){
			this.addItem("(");
			if(hasSingleArg){
				annotation.singleArg.accept(this);
			}
			else{
				for(Pair<String, Expression> arg : annotation.manyArgs){
					this.addItem(arg.getA());
					this.addItem("=");
					arg.getB().accept(this);
					this.addItemNoPreString(",");
				}
				this.popLastItem();
			}
			this.addItem(")");
		}
		return null;//ignore non functional
	}

	@Override
	public Object visit(AnnotationDef annotationDef){
		//visitList.add("AnnotationDef");

		if(annotationDef.annotations != null){
			annotationDef.annotations.accept(this);
			this.addItem("\n");
		}

		this.addItem(""+annotationDef.am);
		
		this.addItem("annotation");
		this.addItem(annotationDef.name);
		
		if(!annotationDef.annotationDefArgs.isEmpty()){
			this.addItem("(");
			
			for(AnnotationDefArg ada : annotationDef.annotationDefArgs){
				if(null != ada.annotations){
					ada.annotations.accept(this);
				}
				this.addItem(ada.name);
				if(null != ada.optionalType){
					ada.optionalType.accept(this);
				}
				if(ada.expression != null){
					this.addItem("=");
					ada.expression.accept(this);
				}
				
				this.addItemNoPreString(",");
			}
			this.popLastItem();
			this.addItem(")");
		}
		
		annotationDef.annotBlock.accept(this);
		return null;
	}

	@Override
	public Object visit(Annotations annotations){
		
		for(Annotation annot : annotations.annotations){
			annot.accept(this);
			this.addItem("");
		}
		this.popLastItem();
		
		if(annotations.annotations.size() > 1){
			this.addItem("\n");
		}
		
		
		return null;//ignore non functional
	}

	@Override
	public Object visit(ArrayConstructor arrayConstructor) {
		//visitList.add("ArrayConstructor");
		this.addItem("new");
		arrayConstructor.type.accept(this);
		for(Expression level : arrayConstructor.arrayLevels)
		{
			this.addItem("[");
			if(level != null){
				level.accept(this);
			}
			this.addItem("]");
		}
		
		if(arrayConstructor.defaultValue != null) {
			this.addItem("(");
			arrayConstructor.defaultValue.accept(this);
			this.addItem(")");
		}
		
		return null;
	}

	@Override
	public Object visit(ArrayDef arrayDef) {
		//visitList.add("ArrayDef");
		//if(!arrayDef.isArray){
		if(arrayDef.supressArrayConcat) {
			this.addItem("a["); 
		}else {
			this.addItem("["); 
		}
		
		String delim = arrayDef.isArray?" ":",";
		
		ArrayList<Expression> elms = arrayDef.getArrayElements(this);
		
		if(!arrayDef.isArray && elms.isEmpty()) {
			this.addItem(","); 
		}
		
		for(Expression e : elms)
		{
			e.accept(this);
			this.addItemNoPreString(delim);
		}
		if(!arrayDef.getArrayElements(this).isEmpty()) popLastItem();
		
		this.addItem("]");
		return null;
	}

	@Override
	public Object visit(ArrayRef arrayRef) {
		//visitList.add("ArrayRef");
		arrayRef.expr.accept(this);
		processArrayRefElements(arrayRef.arrayLevelElements);
		return null;
	}

	@Override
	public Object visit(ArrayRefElement arrayRefElement) {
		//visitList.add("ArrayRefElement");
		arrayRefElement.e1.accept(this);
		return null;
	}

	@Override
	public Object visit(ArrayRefElementPostfixAll arrayRefElementPostfixAll) {
		//visitList.add("ArrayRefElementPostfixAll");
		arrayRefElementPostfixAll.e1.accept(this);
		this.addItem("...");
		return null;
	}

	@Override
	public Object visit(ArrayRefElementPrefixAll arrayRefElementPrefixAll) {
		//visitList.add("ArrayRefElementPrefixAll");
		
		this.addItem("...");
		arrayRefElementPrefixAll.e1.accept(this);
		return null;
	}
	@Override
	public Object visit(ArrayRefElementSubList arrayRef) {
		//visitList.add("ArrayRefElementSubList");
		arrayRef.e1.accept(this);
		this.addItem("...");
		arrayRef.e2.accept(this);
		return null;
	}
	
	
	@Override
	public Object visit(AssertStatement assertStatement) {
		//visitList.add("AssertStatement");
		this.addItem("assert"); 
		assertStatement.e.accept(this);
		if(assertStatement.message != null){
			assertStatement.message.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(AssignExisting assignExisting) {
		//visitList.add("AssignExisting");
		
		if(assignExisting.annotations != null){
			assignExisting.annotations.accept(this);
			this.addItem("\n");
		}
		
		if(assignExisting.isOverride) { this.addItem("override");}
		
		if(assignExisting.isTransient) this.addItem("tansient");
		if(assignExisting.gpuVarQualifier != null) this.addItem(""+assignExisting.gpuVarQualifier);
		assignExisting.assignee.accept(this);
		
		Type tagged = assignExisting.getTaggedType();
		if(assignExisting.isReallyNew && null != tagged){
			tagged.accept(this);
		}
		
		this.addItem(""+assignExisting.eq); 
		if(null != assignExisting.expr){
			assignExisting.expr.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(AssignNew assignNew) {
		//visitList.add("AssignNew");
		
		if(assignNew.annotations != null){
			assignNew.annotations.accept(this);
			this.addItem("\n");
		}
		
		if(assignNew.accessModifier != null) this.addItem(assignNew.accessModifier.toString());
		if(assignNew.isOverride) { this.addItem("override");}
		if(assignNew.isTransient) this.addItem("tansient");
		if(assignNew.isShared) this.addItem("shared");
		if(assignNew.gpuVarQualifier != null) this.addItem(""+assignNew.gpuVarQualifier);
		if(assignNew.isFinal) this.addItem("val");
		if(assignNew.isVolatile) this.addItem("volatile");
		if(assignNew.prefix!=null) this.addItem(assignNew.prefix);
		if(assignNew.name!=null) this.addItem(assignNew.name);
		//if(assignNew.isActor) this.addItem("actor");
		if(assignNew.type!=null) assignNew.type.accept(this);
		if(assignNew.eq!=null && assignNew.expr!=null) this.addItem(""+assignNew.eq);
		if(assignNew.expr!=null) assignNew.expr.accept(this);
		
		return null;
	}
	
	@Override
	public Object visit(AsyncBlock asyncBlock) {
		//visitList.add("AsyncBlock");
		asyncBlock.body.accept(this);
		this.addItem("!");
		if(null != asyncBlock.executor)
		{
			this.addItem("(");
			asyncBlock.executor.accept(this);
			this.addItem(")");
		}
		return null;
	}

	@Override
	public Object visit(AsyncBodyBlock asyncBodyBlock) {
		this.addItem("async");
		
		this.addItem("{\n");
		if(!asyncBodyBlock.preBlocks.isEmpty()){
			this.addItem("pre");
			for(Block b : asyncBodyBlock.preBlocks){
				b.accept(this);
			}
		}
		
		
		indentLevel++;
		LineHolder lh = asyncBodyBlock.mainBody.startItr();
		
		while(lh != null)
		{
			Line l = lh.l;
			if(null != l){
				l.accept(this);
				if(!(l instanceof CompoundStatement))
				{
					//this.addItem("; <-- "+l.getClass() + "\n");
					this.addItem(";\n");
					
				}
			}
			
				
			lh = asyncBodyBlock.mainBody.getNext();
		}
		indentLevel--;
		
		
		if(!asyncBodyBlock.postBlocks.isEmpty()){
			this.addItem("post");
			for(Block b : asyncBodyBlock.postBlocks){
				b.accept(this);
			}
		}
		this.addItem("}\n");
		
		if(asyncBodyBlock.initMethodNameFuncDef != null){
			this.addItem("|=>");
			asyncBodyBlock.initMethodNameFuncDef.accept(this);
			asyncBodyBlock.applyMethodFuncDef.accept(this);
			asyncBodyBlock.cleanUpMethodFuncDef.accept(this);
			this.addItem("|");
		}
		
		
		return null;
	}

	@Override
	public Object visit(AsyncRefRef asyncRefRef) {
		//visitList.add("AsyncFuncInvoke");
		asyncRefRef.b.accept(this);
		int n=0;
		while(n++ < asyncRefRef.refCntLevels){
			this.addItem(":");
		}
		return null;
	}

	@Override
	public Object visit(Await await) {
		this.addItem("await");
		this.addItem("(");
		
		ArrayList<Node> exprs = await.exprs;
		for(int n=0; n < exprs.size(); n++){
			Node e = exprs.get(n);
			e.accept(this);
			if(n < exprs.size()-1){
				this.addItemNoPreString(",");
			}
		}
		if(await.body != null){
			this.addItem(";");
			await.body.accept(this);
		}
		this.addItem(")");
		

		if(await.initMethodNameFuncDef != null){
			this.addItem("|=>");
			await.initMethodNameFuncDef.accept(this);
			await.applyMethodFuncDef.accept(this);
			await.cleanUpMethodFuncDef.accept(this);
			this.addItem("|");
		}
		
		return null;//null is ok
	}

	@Override
	public Object visit(BitwiseOperation bitwiseOperation) {
		//visitList.add("AndExpression");
		bitwiseOperation.head.accept(this);
		String opName = bitwiseOperation.oper.operationName;
		for(Expression i : bitwiseOperation.things){
			this.addItem(opName);
			i.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(Block block) {
		visitBlock(block, true);
		return null;
	}

	@Override
	public Object visit(BreakStatement breakStatement) {
		//visitList.add("BreakStatement");
		
		this.addItem("break");
		
		if(null != breakStatement.returns){
			breakStatement.returns.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(CaseExpressionAnd caseExpressionAnd) {
		caseExpressionAnd.head.accept(this);
		for(CaseExpression ce : caseExpressionAnd.caseAnds){
			this.addItem("and");
			ce.accept(this);
		}
		if(caseExpressionAnd.alsoCondition != null){
			this.addItem("also");
			caseExpressionAnd.alsoCondition.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(CaseExpressionAssign caseExpressionUntypedAssign){
		if(caseExpressionUntypedAssign.isFinal) {
			this.addItem("val");
		}
		this.addItem(caseExpressionUntypedAssign.varname);
		
		if(!caseExpressionUntypedAssign.types.isEmpty()){
			for(Type tt : caseExpressionUntypedAssign.types){
				tt.accept(this);
				this.addItem("or");
			}
			this.popLastItem();
		}
		
		if(null != caseExpressionUntypedAssign.expr){
			this.addItem(";");
			caseExpressionUntypedAssign.expr.accept(this);
		}
		
		if(caseExpressionUntypedAssign.alsoCondition != null){
			this.addItem("also");
			caseExpressionUntypedAssign.alsoCondition.accept(this);
		}
		
		return null;
	}
	
	@Override
	public Object visit(CaseExpressionObjectTypeAssign caseExpressionObjectTypeAssign) {
		if(caseExpressionObjectTypeAssign.isFinal) {
			this.addItem("val");
		}
		this.addItem(caseExpressionObjectTypeAssign.varname);
		
		caseExpressionObjectTypeAssign.expr.accept(this);
		
		if(caseExpressionObjectTypeAssign.alsoCondition != null){
			this.addItem("also");
			caseExpressionObjectTypeAssign.alsoCondition.accept(this);
		}
		
		return null;
	}
	
	@Override
	public Object visit(CaseExpressionAssignTuple caseExpressionAssignTuple){
		for(Assign tt : caseExpressionAssignTuple.lhss){
			tt.accept(this);
			this.addItem(",");
		}
		this.popLastItem();
		
		if(null != caseExpressionAssignTuple.expr){
			this.addItem(";");
			caseExpressionAssignTuple.expr.accept(this);
		}
		
		if(caseExpressionAssignTuple.alsoCondition != null){
			this.addItem("also");
			caseExpressionAssignTuple.alsoCondition.accept(this);
		}
		
		return null;
	}
	
	@Override
	public Object visit(CaseExpressionOr caseExpressionOr) {
		caseExpressionOr.head.accept(this);
		for(CaseExpression ce : caseExpressionOr.caseOrs){
			this.addItem("or");
			ce.accept(this);
		}
		
		if(caseExpressionOr.alsoCondition != null){
			this.addItem("also");
			caseExpressionOr.alsoCondition.accept(this);
		}
		
		return null;
	}
	
	@Override
	public Object visit(CaseExpressionPost caseExpressionPost) {
		
		caseExpressionPost.e.accept(this);
		this.addItem(caseExpressionPost.cop.toString()); 
		
		if(caseExpressionPost.alsoCondition != null){
			this.addItem("also");
			caseExpressionPost.alsoCondition.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(CaseExpressionPre caseExpressionPre) {
		
		this.addItem(caseExpressionPre.cop.toString()); 
		caseExpressionPre.e.accept(this);
		
		if(caseExpressionPre.alsoCondition != null){
			this.addItem("also");
			caseExpressionPre.alsoCondition.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(CaseExpressionWrapper caseExpressionWrapper) {
		//this.addItem("(");
		caseExpressionWrapper.e.accept(this);
		//this.addItem(")");
		
		if(caseExpressionWrapper.alsoCondition != null){
			this.addItem("also");
			caseExpressionWrapper.alsoCondition.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(CastExpression castExpression) {
		//visitList.add("CastExpression");
		//this.addItem("(");
		
		//this.addItem(")");
		if(castExpression.o != null){
			castExpression.o.accept(this);
		}else{
			this.addItem("?err?");
		}
		
		this.addItem("as");
		
		castExpression.t.accept(this);
		
		return null;

	}

	@Override
	public Object visit(CatchBlocks catchBlocks) {
		//visitList.add("CatchBlocks");
		this.addItem("catch");
		this.addItem("(");
		this.addItem(catchBlocks.var);
		for(Type ca : catchBlocks.caughtTypes){
			ca.accept(this);
			this.addItem("or");
		}
		this.popLastItem();
		this.addItem(")");
		catchBlocks.catchBlock.accept(this);
		
		return null;
	}
	

	@Override
	public Object visit(Changed changed) {
		this.addItem("changed");//TODO: WTF does changed do?
		return null;
	}

	@Override
	public Object visit(ClassDef classDef) {
		//visitList.add("ClassDef");
		
		if(classDef.annotations != null){
			classDef.annotations.accept(this);
			this.addItem("\n");
		}
		
		if(!classDef.isLocalClassDef){
			if(null != classDef.accessModifier){
				this.addItem(""+classDef.accessModifier);
			}
		}
		
		if(classDef.isTransient) {
			this.addItem("transient");
		}
		
		if(classDef.isShared) {
			this.addItem("shared");
		}
		
		if(classDef.isActor){
			this.addItem("actor");
		}
		else{
			this.addItem(classDef.isObject?"object":"class");
		}
		if(!classDef.isLocalClassDef){
			this.addItem(classDef.className);
		}
		if(!classDef.classGenricList.isEmpty())
		{
			this.addItem("<");
			for(Object n : classDef.classGenricList)
			{
				if(n instanceof GenericType) {
					((GenericType)n).accept(this);
				}else {
					this.addItem(""+n);
				}
				this.addItemNoPreString(",");
			}
			popLastItem();
			this.addItem(">");
		}
		
		
		if(null!=classDef.classDefArgs)
		{
			this.addItem("(");
			classDef.classDefArgs.accept(this);
			this.addItem(")");
		}
		

		if(classDef.istypedActor){
			this.addItem("of");
			classDef.typedActorOn.accept(this);
		}
		
		if(!classDef.acteeClassExpressions.isEmpty()){
			this.addItem("(");
			for(Expression e : classDef.acteeClassExpressions){
				e.accept(this);
				this.addItemNoPreString(",");
			}
			popLastItem();
			this.addItem(")");
		}
			
		
		if(classDef.superclass!=null)
		{
			this.addItem("extends");
			this.addItem(classDef.superclass);
			
			if(null != classDef.superClassGenricList && !classDef.superClassGenricList.isEmpty())
			{
				this.addItem("<");
				for(Object n : classDef.superClassGenricList)
				{
					this.addItem(""+n);
					this.addItemNoPreString(",");
				}
				popLastItem();
				this.addItem(">");
			}
		}
		
		if(!classDef.superClassExpressions.isEmpty()){
			this.addItem("(");
			for(Expression e : classDef.superClassExpressions){
				e.accept(this);
				this.addItemNoPreString(",");
			}
			popLastItem();
			this.addItem(")");
		}
		
		if(!classDef.traits.isEmpty()) {
			this.addItem("~");
			
			int sz = classDef.traits.size();
			for(int n=0; n < sz; n++) {
				classDef.traits.get(n).accept(this);
				if(n != sz -1) {
					this.addItem(",");
				}
			}
		}
		
		classDef.classBlock.accept(this);
		
		return null;
	}

	@Override
	public Object visit(ImpliInstance impliInstance) {
		this.addItem(impliInstance.traitName); 
		if(!impliInstance.traitGenricList.isEmpty()) {
			this.addItem("<");
			for(Type n : impliInstance.traitGenricList)
			{
				this.addItem(""+n);
				this.addItemNoPreString(",");
			}
			popLastItem();
			this.addItem(">");
		}
		
		return null;
	}

	
	@Override
	public Object visit(ClassDefArg classDefArg) {
		//visitList.add("ClassDefArg");
		
		if(null != classDefArg.annotations){
			classDefArg.annotations.accept(this);
		}
		
		if(classDefArg.am != null){ this.addItem(classDefArg.am.toString()); }
		if(classDefArg.isOverride){this.addItem("override");}
		if(classDefArg.isLazy){this.addItem("lazy");}
		if(classDefArg.isTransient){this.addItem("transient");}
		if(classDefArg.isShared){this.addItem("shared");}
		if(classDefArg.isFinal) this.addItem("val");
		String prefix = classDefArg.prefix;
		if(null == prefix) prefix="";
		this.addItem(prefix + classDefArg.name);
		
		if(classDefArg.type != null){
			classDefArg.type.accept(this);
		}
		
		if(classDefArg.isVararg){
			this.addItem("...");
		}
		
		if(classDefArg.defaultValue != null){
			this.addItem("=");
			classDefArg.defaultValue.accept(this);
		}
		
		
		return null;
	}

	@Override
	public Object visit(ClassDefArgs classDefArgs) {
		//visitList.add("ClassDefArgs");
		boolean bob = false;
		for(ClassDefArg a : classDefArgs.aargs)
		{
			a.accept(this);
			this.addItemNoPreString(",");
			bob=true;
		}
		if(bob) popLastItem();
		
		return null;
	}

	@Override
	public Object visit(ConstructorDef funcDef) {
		return visit((FuncDef)funcDef);
	}

	@Override
	public Object visit(ContinueStatement continueStatement) {
		//visitList.add("ContinueStatement");
		
		
		this.addItem("continue");
		
		if(null != continueStatement.returns){
			continueStatement.returns.accept(this);
		}
		
		return null;
	}
	
	private void processCopyItems(List<CopyExprItem> copyItems, List<String> modifiers) {
		if(null != copyItems ) {
			this.addItem("(");
			for(CopyExprItem exprItem : copyItems) {
				if(exprItem instanceof CopyExprAssign) {
					CopyExprAssign ascea = (CopyExprAssign)exprItem;
					this.addItem(ascea.field);
					this.addItem("=");
					ascea.assignment.accept(this);
				}else if(exprItem instanceof CopyExprIncOnly) {
					this.addItem(((CopyExprIncOnly)exprItem).incOnly);
				}else if(exprItem instanceof CopyExprExclOnly) {
					CopyExprExclOnly asexcl = (CopyExprExclOnly)exprItem;
					this.addItem("<" + String.join(", ", asexcl.excludeOnly) + ">");
				}else if(exprItem instanceof CopyExprNested) {
					CopyExprNested cen = (CopyExprNested)exprItem;
					this.addItem(cen.fname);
					this.addItem("@");
					processCopyItems(cen.copyItems, cen.modifiers);
				}else if(exprItem instanceof CopyExprSuper) {
					CopyExprSuper cen = (CopyExprSuper)exprItem;
					this.addItem("super");
					this.addItem("@");
					processCopyItems(cen.copyItems, cen.modifiers);
				}
				this.addItem(", ");
			}
			this.popLastItem();
			
			if(null != modifiers && !modifiers.isEmpty()) {
				this.addItem("; " + String.format(", ", modifiers));
			}
			
			
			this.addItem(")");
		}
	}
	
	@Override
	public Object visit(CopyExpression copyExpression) {
		//visitList.add("CopyExpression");
		copyExpression.expr.accept(this);
		this.addItem("@");
		processCopyItems(copyExpression.copyItems, copyExpression.modifiers);
		return null;
	}

	@Override
	public Object visit(DeleteStatement deleteStatement){
		this.addItem("delete");
		int sz= deleteStatement.exprs.size();
		for(int n = 0; n < sz; n++)
		{	
			deleteStatement.exprs.get(n).accept(this);
			if(n !=  sz-1)
			{
				this.addItemNoPreString(", ");
			}
		}
		
		
		
		return null;
	}

	@Override
	public Object visit(DMANewFromExpression dmaNewFromExpression){
		dmaNewFromExpression.e.accept(this);
		this.addItem("#");
		return null;
	}

	@Override
	public Object visit(DotOperator dotOperator) {
		//visitList.add("DotOperator");
		//dotOperator.head.accept(this);
		//this.addItem(".");
		//boolean a = false;
		ArrayList<Expression> elements = dotOperator.getElements(this); 
		ArrayList<Boolean> isDirectAccess = dotOperator.getIsDirectAccess(this);
		
		//assert elements.size() == isDirectAccess.size()-1;
		
		for(int n=0; n<elements.size(); n++)
		{
			Expression e = elements.get(n);
			e.accept(this);
			
			if(n != elements.size()-1)
			{
				boolean isDirect = isDirectAccess.get(n);
				boolean retSelf = dotOperator.returnCalledOn.get(n);
				boolean issafe = dotOperator.safeCall.get(n);
				
				if(isDirect)
				{
					this.addItemNoPreString(retSelf?"\\..":"\\.");
				}
				else
				{
					if(issafe) {
						this.addItemNoPreString("?.");
					}else {
						this.addItemNoPreString(retSelf?"..":".");
					}
				}
			}
			//a=true;
		}
		
		//if(a) popLastItem();
		
		return null;
	}

	@Override
	public Object visit(DottedAsName dottedAsName) {
		//visitList.add("DottedAsName");
		this.addItem(dottedAsName.origonalName);
		this.addItem("as");
		this.addItem(dottedAsName.refName);
		
		return null;
	}

	@Override
	public Object visit(DottedNameList dottedNameList) {
		//visitList.add("DottedNameList");
		boolean b = false;
		StringBuilder sb = new StringBuilder();
		for(String n: dottedNameList.dottedNames)
		{
			sb.append(n);
			sb.append(", ");
			b = true;
		}
		String ll = sb.toString();
		if(b) ll = ll.substring(0, ll.length()-2);
		this.addItem(ll);
		
		return null;
	}

	@Override
	public Object visit(DuffAssign duffAssign) {
		//visitList.add("DuffAssign");
		duffAssign.e.accept(this);
		
		return null;
	}

	@Override
	public Object visit(ElifUnit elifUnit) {
		//visitList.add("ElifUnit");
		this.addItem("elif");
		this.addItem("(");
		elifUnit.eliftest.accept(this);
		this.addItem(")");
		elifUnit.elifb.accept(this);
		
		return null;
	}

	@Override
	public Object visit(EnumDef enumDef){
		//visitList.add("EnumDef");
		
		if(enumDef.annotations != null){
			enumDef.annotations.accept(this);
			this.addItem("\n");
		}
		
		if(null != enumDef.accessModifier) {
			this.addItem(""+enumDef.accessModifier);
		}
		
		this.addItem("enum");
		this.addItem(enumDef.enaumName);
		
		if(null!=enumDef.classDefArgs)
		{
			if(null!=enumDef.classDefArgs)
			{
				this.addItem("(");
				enumDef.classDefArgs.accept(this);
				this.addItem(")");
			}
		}

		enumDef.block.accept(this);
		
		return null;
	}


	
	@Override
	public Object visit(EnumItem enumItem){
		
		if(enumItem.annotations != null){
			enumItem.annotations.accept(this);
		}
		
		this.addItem(enumItem.name);
		
		if(!enumItem.args.isEmpty()){
			enumItem.args.accept(this);
		}
		
		if(enumItem.block != null){
			enumItem.block.accept(this);
		}
		
		return null;
	}
	
	@Override
	public Object visit(EqReExpression equalityExpression) {
		//visitList.add("EqReExpression");
		
		equalityExpression.head.accept(this);
		for(GrandLogicalElement e: equalityExpression.elements)
		{
			e.accept(this);
		}
		
		return null;
	}
	
	@Override
	public Object visit(ExpressionList expressionList) {
		for(Expression expr : expressionList.exprs){
			expr.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(ForBlock forBlock) {
		//visitList.add("ForBlock");
		this.addItem("for");
		this.addItem("(");
		
		if(forBlock.assignTuple != null) {
			forBlock.assignTuple.accept(this);
		}else {
			this.addItem(forBlock.localVarName);
			if(null!= forBlock.localVarType) forBlock.localVarType.accept(this);
		}
		
		
		this.addItem("in");
		forBlock.expr.accept(this);
		
		if(forBlock.idxVariableCreator != null){
			this.addItem(";");
			forBlock.idxVariableCreator.accept(this);
		}
		else if(forBlock.idxVariableAssignment != null){
			this.addItem(";");
			forBlock.idxVariableAssignment.accept(this);
		}
		
		this.addItem(")");
		forBlock.block.accept(this);
		
		if(forBlock.elseblock != null){
			this.addItem("else");
			forBlock.elseblock.accept(this);
		}
		
		return null;
	}
	
	@Override
	public Object visit(ForBlockOld forBlockOld) {
		//visitList.add("ForBlockOld");
		this.addItem("for");
		this.addItem("(");
		
		if(null != forBlockOld.assignExpr) 
		{
			forBlockOld.assignExpr.accept(this);
		}
		else if(null != forBlockOld.assignName)
		{
			this.addItem(forBlockOld.assignName);
			if( null != forBlockOld.assigType) forBlockOld.assigType.accept(this);
			if( null != forBlockOld.assigType) forBlockOld.assigType.accept(this);
			this.addItem("=");
			if( null != forBlockOld.assigFrom) forBlockOld.assigFrom.accept(this);
		}
		this.addItem(";");
		if(null != forBlockOld.check) forBlockOld.check.accept(this);
		this.addItem(";");
		if(null != forBlockOld.postExpr) forBlockOld.postExpr.accept(this);
		this.addItem(")");
		forBlockOld.block.accept(this);

		if(forBlockOld.elseblock != null){
			this.addItem("else");
			forBlockOld.elseblock.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(FuncDef funcDef) {
		
		if(!(funcDef.isAutoGennerated && synthNamesIgnore.contains(funcDef.funcName)) ){
			//visitList.add("FuncDef"); //TODO: sync only on class methods (non static)
			
			if(funcDef.annotations != null){
				funcDef.annotations.accept(this);
			}
			
			if(null != funcDef.accessModifier){
				this.addItem(""+funcDef.accessModifier);
			}
			
			if(funcDef.isInjected) {
				this.addItem("inject");
			}
			
			if("<init>".equals(funcDef.funcName)){
				this.addItem("this");
			}else{
				
				String normname = "def";
				if(funcDef.isGPUKernalOrFunction != null) {
					normname = funcDef.isGPUKernalOrFunction.toString();
				}
				
				this.addItem(funcDef.isOverride?"override":normname);
				
				if(funcDef.extFunOn != null){
					funcDef.extFunOn.accept(this);
					this.addItem(".");
				}
				
				this.addItem(funcDef.funcName);
			}
			
			lineGenerics(funcDef.methodGenricList);
			
			this.addItem("(");
			if(null!=funcDef.params) funcDef.params.accept(this);
			this.addItem(")");
			if(null == funcDef.retType){
				if(!"<init>".equals(funcDef.funcName)){
					this.addItem("void");
				}
			}
			else{
				funcDef.retType.accept(this);
			}
			if(funcDef.funcblock != null){
				funcDef.funcblock.accept(this);
			}else {
				this.addItem("\n");
			}
			
			
			if(funcDef.gpuKernelFuncDetails != null) {
				this.addItem("/*=>\n " + funcDef.gpuKernelFuncDetails.source + "*/\n", true);
			}
			
		}
	
		return null;
	}
	
	private void lineGenerics(ArrayList<Pair<String, NamedType>> methodGenricList) {
		if(methodGenricList != null && !methodGenricList.isEmpty()){
			this.addItem("<");
			for(Pair<String, NamedType> n : methodGenricList){
				this.addItem(""+n.getA());
				NamedType nt = n.getB();
				if(nt != null) {
					nt.accept(this);
				}
				this.addItemNoPreString(",");
			}
			popLastItem();
			this.addItem(">");
		}
	}

	@Override
	public Object visit(FuncInvoke funcInvoke) {
		//visitList.add("FuncInvoke");
		this.addItem(funcInvoke.funName);
		if(funcInvoke.genTypes != null && !funcInvoke.genTypes.isEmpty()){
			this.addItem("<");
			for(Type n : funcInvoke.genTypes)
			{
				n.accept(this);
				this.addItemNoPreString(",");
			}
			popLastItem();
			this.addItem(">");
		}
		funcInvoke.args.accept(this);
		
		return null;
	}
	
	@Override
	public Object visit(FuncInvokeArgs funcInvokeArgs) {
		//visitList.add("FuncInvokeArgs");
		this.addItemNoPreString("(");
		
		for(Expression e : funcInvokeArgs.getArgumentsWNPs())
		{
			if(e != null) {
				e.accept(this);
				this.addItemNoPreString(",");
			}
		}
		
		if(!funcInvokeArgs.nameMap.isEmpty()){
			for(Pair<String, Object> name: funcInvokeArgs.nameMap){
				this.addItem(name.getA());
				this.addItem("=");
				((Expression)name.getB()).accept(this);
				this.addItemNoPreString(",");
			}
		}
		if(!funcInvokeArgs.asnames.isEmpty() || !funcInvokeArgs.nameMap.isEmpty()){
			this.popLastItem();
		}
		
		this.addItemNoPreString(")");
		return null;
	}

	@Override
	public Object visit(FuncParam funcParam) {
		//visitList.add("FuncParam");
		
		if(funcParam.annotations != null){
			funcParam.annotations.accept(this);
		}
		
		if(funcParam.gpuVarQualifier != null) {
			this.addItem(funcParam.gpuVarQualifier.toString());
		}
		
		if(funcParam.gpuInOutFuncParamModifier != null) {
			this.addItem(funcParam.gpuInOutFuncParamModifier.toString());
		}
		
		if(funcParam.isLazy) {
			this.addItem("lazy");
		}
		
		this.addItem(funcParam.isFinal?"val":"var");
		
		
		addNamePreDollarLevel(funcParam.name);
		if(funcParam.type != null){
			funcParam.type.accept(this);
		}
		if(funcParam.isVararg){
			this.addItem("...");
		}
		
		if(funcParam.defaultValue != null){
			this.addItem("=");
			funcParam.defaultValue.accept(this);
		}
		
		return null;
	}
	
	@Override
	public Object visit(FuncParams funcParams) {
		//visitList.add("FuncParams");
		boolean a = false;
		for(FuncParam f: funcParams.params)
		{
			f.accept(this);
			this.addItemNoPreString(",");
			a=true;
		}
		if(a) popLastItem();
		
		return null;
	}

/*	@Override
	public Object visit(TransBlock withBlock) {
		//visitList.add("TransBlock");
		this.addItem("trans");
		withBlock.body.accept(this);
		
		return null;
	}*/
	
	@Override
	public Object visit(FuncRef funcRef) {
		if(funcRef != null){
			//visitList.add("FuncRef");
			funcRef.functo.accept(this);
			this.addItem("&");
			
			if(null != funcRef.genTypes && ! funcRef.genTypes.isEmpty())
			{
				this.addItem("<");
				for(Type t :  funcRef.genTypes)
				{
					t.accept(this);
					this.addItemNoPreString(",");
				}
				popLastItem();
				this.addItem(">");
			}
			
			FuncRefArgs args = funcRef.argsForNextCompCycle;
			if(null==args){
				args = funcRef.getArgsForScopeAndTypeCheck();
			}
			if(null != args){
				args.accept(this);
			}
		}
		
		return null;
	}

	@Override
	public Object visit(FuncRefArgs funcRefArgs) {
		//visitList.add("FuncRefArgs");
		this.addItem("(");
		for(Object item : funcRefArgs.exprOrTypeArgsList)
		{
			if(item instanceof Expression){
				((Expression) item).accept(this);
			}
			else if(item instanceof Type){
				this.addItem("?");
				((Type) item).accept(this);
			}
			this.addItemNoPreString(",");
		}
		
		if(!funcRefArgs.nameMap.isEmpty()){
			for(Pair<String, Object> name: funcRefArgs.nameMap){
				this.addItem(name.getA());
				this.addItem("=");
				Object item = name.getB();
				
				if(item instanceof Expression){
					((Expression) item).accept(this);
				}
				else if(item instanceof Type){
					this.addItem("?");
					((Type) item).accept(this);
				}
				
				this.addItemNoPreString(",");
			}
		}
		
		if(!funcRefArgs.exprOrTypeArgsList.isEmpty() || !funcRefArgs.nameMap.isEmpty()){
			this.popLastItem();
		}
		
		
		this.addItem(")");
		return null;
	}
	
	@Override
	public Object visit(FuncRefInvoke funcRefInvoke) {
		//visitList.add("FuncRefInvoke");
		funcRefInvoke.funcRef.accept(this);
		funcRefInvoke.getArgs().accept(this);
		
		return null;
	}

	@Override
	public Object visit(FuncType funcType) {
		//visitList.add("FuncType");
		
		printInOutGenModi(funcType);
		
		if(funcType.getLocalGenerics() != null && !funcType.getLocalGenerics().isEmpty())
		{
			this.addItem("<");
			for(GenericType n : funcType.getLocalGenerics())
			{
				this.addItem(n.toString());
				this.addItemNoPreString(", ");
			}
			popLastItem();
			this.addItem(">");
		}
		
		
		
		if(funcType.getArrayLevels() > 0){
			this.addItem("(");
		}
		

		this.addItem("(");
		if(funcType.isClassRefType){
			this.addItem("*");
		}else{
			boolean a = false;
			for(Type input: funcType.getInputs())
			{
				input.accept(this);
				this.addItemNoPreString(",");
				a=true;
			}
			if(a) popLastItem();
		}
		
		this.addItem(")");
		
		if(null != funcType.retType){
			funcType.retType.accept(this);
		}
		
		if(funcType.getArrayLevels() > 0){
			this.addItem(")");
		}
		
		addNullableStatus(funcType);
		
		return null;
	}
	
	@Override
	public Object visit(GenericType genericType) {
		//visitList.add("GenericType");
		
		printInOutGenModi(genericType);
		
		this.addItem(genericType.name);
		
		
		if(genericType.upperBound != null && /*showGenericsAsUpperBound*/ !genericType.upperBound.equals(ScopeAndTypeChecker.const_object) ){
			
			//this.addItem("<");
			
			genericType.upperBound.accept(this);
		}
		
		addNullableStatus(genericType);
		
		return null;
	}

	@Override
	public Object visit(GetSetOperation getSetOperation) {
		this.addItem(String.format("SYN-%s%s" , getSetOperation.getter, getSetOperation.setter) ) ;
		this.addItem("(");
		this.addItem( ""+getSetOperation.incOperation );
		this.addItemNoPreString(",");
		this.addItem( getSetOperation.ispostfix?"POST":"PRE");
		this.addItemNoPreString(",");
		getSetOperation.toAddMinus.accept(this);
		this.addItem(")");
		return null;
	}

	@Override
	public Object visit(GrandLogicalElement equalityElement) {
		//visitList.add("EqualityElement");
		this.addItem(""+equalityElement.compOp);
		equalityElement.e2.accept(this);
		
		return null;
	}

/*	@Override
	public Object visit(IfExpr ifExpr) {
		//visitList.add("IfExpr");
		
		ifExpr.op1.accept(this);
		this.addItem("if");
		ifExpr.test.accept(this);
		this.addItem("else");
		ifExpr.op2.accept(this);
		return null;
	}*/

	@Override
	public Object visit(IfStatement ifStatement) {
		//visitList.add("IfStatement");
		this.addItem("if");
		this.addItem("(");
		ifStatement.iftest.accept(this);
		this.addItem(")");
		ifStatement.ifblock.accept(this);
		for(ElifUnit u : ifStatement.elifunits)
		{
			u.accept(this);
		}
		if(ifStatement.elseb!=null)
		{
			this.addItem("else");
			ifStatement.elseb.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(ImportAsName importAsName) {
		//visitList.add("ImportAsName");
		this.addItem(importAsName.impt);
		if(importAsName.asName != null)
		{
			this.addItem("as");
			this.addItem(importAsName.asName);
		}

		return null;
	}

	@Override
	public Object visit(ImportFrom importFrom) {
		//visitList.add("ImportFrom");
		this.addItem("from");
		this.addItem(importFrom.from);
		this.addItem(importFrom.normalImport?"import":"using");
		for(int n=0; n < importFrom.froms.size(); n++ ){
			importFrom.froms.get(n).accept(this);
			if(n != importFrom.froms.size()-1){
				this.addItem(",");
			}
		}
		
		return null;
	}

	@Override
	public Object visit(ImportImport importImport) {
		//visitList.add("ImportImport");
		this.addItem(importImport.normalImport?"import":"using");
		for(int n = 0; n < importImport.imports.size(); n++){
			importImport.imports.get(n).accept(this);
			if(n != importImport.imports.size()-1){
				this.addItemNoPreString(",");
			}
		}
		
		return null;
	}

	@Override
	public Object visit(ImportStar importStar) {
		//visitList.add("ImportStar");
		this.addItem("from");
		this.addItem(importStar.from);
		this.addItem(importStar.normalImport?"import":"using");
		this.addItem("*");
		
		return null;
	}
	
	@Override
	public Object visit(InExpression cont){
		
		if(cont.containsMethodCall != null) {
			cont.containsMethodCall.accept(this);
		}else{
			cont.thing.accept(this);
			
			this.addItem(cont.inverted?"not in":"in");
			
			cont.insideof.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(InitBlock initBlock){
		this.addItem("init");
		initBlock.block.accept(this);
		return null;
	}
	
	@Override
	public Object visit(Is instanceOf) {
		//visitList.add("InstanceOf");
		instanceOf.e1.accept(this);
		this.addItem( instanceOf.inverted?"isnot":"is" );
		for(Type tt : instanceOf.typees){
			tt.accept(this);
			this.addItem("or");
		}
		this.popLastItem();
		return null;
	}
	
	@Override
	public Object visit(JustAlsoCaseExpression justAlsoCaseExpression){
		this.addItem("also");
		justAlsoCaseExpression.alsoCondition.accept(this);
		return null;
	}

	@Override
	public Object visit(LambdaDef lambdaDef) {
		//visitList.add("LambdaDef"); //TODO: sync only on class methods (non static)

		if(lambdaDef.annotations != null){
			lambdaDef.annotations.accept(this);
		}
		
		this.addItem("def");
		
		if(lambdaDef.methodGenricList != null && !lambdaDef.methodGenricList.isEmpty())
		{
			this.addItem("<");
			for(Pair<String, NamedType> n : lambdaDef.methodGenricList)
			{
				String name = n.getA();
				NamedType nt = n.getB();
				
				this.addItem(""+name);
				if(null != nt) {
					nt.accept(this);
				}
				
				this.addItemNoPreString(",");
			}
			popLastItem();
			this.addItem(">");
		}
		
		if(null != lambdaDef.params)
			this.addItem("(");
			lambdaDef.params.accept(this);
			this.addItem(")");
		if(null != lambdaDef.returnType){
			lambdaDef.returnType.accept(this);
		}
		
		if(lambdaDef.body != null){
			lambdaDef.body.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(LineHolder lineHolder) {
		//visitList.add("LineHolder");
		lineHolder.l.accept(this);
		
		return null;
	}

	@Override
	public Object visit(LocalClassDef localClassDef) {
		localClassDef.cd.accept(this);
		
		return null;
	}

	@Override
	public Object visit(MapDef mapDef) {
		//visitList.add("MapDef");
		this.addItem("{");
		boolean b = false;
		for(IsAMapElement ea: mapDef.elements){
			if(ea instanceof MapDefElement){
				((MapDefElement)ea).accept(this);
			}
			else{
				((MapDefaultElement)ea).accept(this);
			}

			this.addItemNoPreString(",");
			b =true;
			
		}
		
		if(b) popLastItem();
		this.addItem("}");
		return null;
	}

	@Override
	public Object visit(MapDefaultElement mapDefElement) {
		//visitList.add("MapDefaultElement");
		this.addItem("default");
		this.addItem("->");
		mapDefElement.value.accept(this);
		return null;
	}

	@Override
	public Object visit(MapDefElement mapDefElement) {
		//visitList.add("MapDefElement");
		mapDefElement.getKey(null).accept(this);
		this.addItem("->");
		mapDefElement.getValue(null).accept(this);
		return null;
	}

	@Override
	public Object visit(MatchStatement matchStatement) {
		this.addItem("match");
		this.addItem("(");
		matchStatement.matchon.accept(this);
		this.addItem(")");
		this.addItem("{\n");
		indentLevel++;
		
		if(!matchStatement.cases.isEmpty()){
			for(MactchCase caz : matchStatement.cases){
				this.addItem("case");
				this.addItem("(");
				caz.getA().accept(this);
				this.addItem(")");
				caz.getB().accept(this);
			}
		}
		
		if(null != matchStatement.elseblok){
			this.addItem("else");
			matchStatement.elseblok.accept(this);
		}

		indentLevel--;
		this.addItem("}\n");
		return null;
	}

	@Override
	public Object visit(MulerElement mulerElement) {
		//visitList.add("MulerElement");
		this.addItem(""+mulerElement.mulOper);
		mulerElement.expr.accept(this);
		return null;
	}
	
	@Override
	public Object visit(MulerExpression mulerExpression) {
		//visitList.add("MulerExpression");
		mulerExpression.header.accept(this);
		for(MulerElement e: mulerExpression.elements)
		{
			e.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(MultiType multiType) {
		this.addItem(""+multiType.getPrettyName());
		return null;
	}

	@Override
	public Object visit(NamedType namedType) {
		//visitList.add("NamedType");
		/*if(namedType.isDefaultActor){
			this.addItem("actor");
		}*/

		//this.addItem(namedType.toString());
		
		printInOutGenModi(namedType);
		
		if(namedType.getIsRef()){
			namedType.getGenTypes().get(0).accept(this);
			this.addItem(":");
			if(!namedType.getNamedTypeStr().equals("com.concurnas.runtime.ref.Local")){
				this.addItem(namedType.getNamedTypeStr());
			}
		}
		else{
			
			ClassDef cd =  namedType.getSetClassDef();
			String mainString;
			if(cd == null){
				mainString = namedType.getNamedTypeStr();
			}
			else{
				ClassDef parentNestor = cd==null?null : cd.getParentNestor();
				
				mainString = cd.toString();
				
				while(parentNestor!=null)
				{
					if(null != parentNestor)
					{
						stealItems();

						String nameNoGens = parentNestor.toString();
						
						this.addItem(nameNoGens);
						
						if(!parentNestor.classGenricList.isEmpty()){
							this.addItem("<");
							for(int n=0; n < parentNestor.classGenricList.size(); n++){
								GenericType gen = parentNestor.classGenricList.get(n);
								Type got = namedType.fromClassGenericToQualifiedType.get(gen);
								if(got != null){
									got.accept(this);
									
									if(n != parentNestor.classGenricList.size()-1){
										this.addItemNoPreString(",");
									}
								}
							}
							this.addItem(">");
						}
						String repl = this.toString();
						mainString = mainString.replace(nameNoGens, repl);
						
						stopStealingItems();
					}
					parentNestor = parentNestor.getParentNestor();
				}
			}
			
			
			this.addItem(mainString);
			
			if(namedType.getGenTypes()!=null && !namedType.getGenTypes().isEmpty())
			{
				this.addItem("<");
				for(Type t : namedType.getGenTypes())
				{
					if(t != null ){
						t.accept(this);
						this.addItemNoPreString(",");
					}
				}
				popLastItem();
				this.addItem(">");
			}
		}

		addNullableStatus(namedType);
		
		return null;
	}
	
	private void addNullableStatus(Type namedType) {
		if(namedType.hasArrayLevels()) {
			List<NullStatus> nsarlevel = namedType.getNullStatusAtArrayLevel();
			for(int n = 0; n < namedType.getArrayLevels(); n++)
			{
				if(nsarlevel.get(n) == NullStatus.NULLABLE) {
					this.addItem("?");
				}
				this.addItem("[]");
			}
		}
		
		if(namedType.getNullStatus() == NullStatus.NULLABLE) {
			this.addItemNoPreString("?");
		}
	}
	
	@Override
	public Object visit(New namedConstructor) {
		//visitList.add("NamedConstructor");
		this.addItem("new");
		if(null != namedConstructor.typeee){
			namedConstructor.typeee.accept(this);
		}
		
		if(null != namedConstructor.args){
			namedConstructor.args.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(NOP nop) {
		this.addItem(";;");
		return null;
	}

	@Override
	public Object visit(NotExpression notExpression) {
		//visitList.add("NotExpression");
		this.addItem("not");
		notExpression.expr.accept(this);
		return null;
	}

	@Override
	public Object visit(OnChange onChange) {
		this.addItem("onchange");
		this.addItem("(");
		if(!onChange.exprs.isEmpty()) {
			for(Node e: onChange.exprs){
				e.accept(this);
				this.addItemNoPreString(",");
			}
		}
		this.popLastItem();
		
		if(!onChange.options.isEmpty()) {
			this.addItemNoPreString(";");
			for(String opt : onChange.options) {
				this.addItem(opt);
				this.addItemNoPreString(",");
			}
			this.popLastItem();
		}
		
		this.addItem(")");
		onChange.body.accept(this);
		
		if(onChange.initMethodNameFuncDef != null){
			this.addItem("|=>");
			onChange.initMethodNameFuncDef.accept(this);
			onChange.applyMethodFuncDef.accept(this);
			onChange.cleanUpMethodFuncDef.accept(this);
			this.addItem("|");
		}
		
		return null;//null is ok
	}

	@Override
	public Object visit(OnEvery onChange) {
		this.addItem("every");
		this.addItem("(");
		if(!onChange.exprs.isEmpty()) {
			for(Node e: onChange.exprs){
				e.accept(this);
				this.addItemNoPreString(",");
			}
		}
		this.popLastItem();

		if(!onChange.options.isEmpty()) {
			this.addItemNoPreString(";");
			for(String opt : onChange.options) {
				this.addItem(opt);
				this.addItemNoPreString(",");
			}
			this.popLastItem();
		}
		
		this.addItem(")");
		onChange.body.accept(this);
		
		if(onChange.initMethodNameFuncDef != null){
			this.addItem("|=>");
			onChange.initMethodNameFuncDef.accept(this);
			onChange.applyMethodFuncDef.accept(this);
			onChange.cleanUpMethodFuncDef.accept(this);
			this.addItem("|");
		}
		
		return null;//null is ok
	}
	
	@Override
	public Object visit(OrExpression orExpression) {
		//visitList.add("OrExpression");
		orExpression.head.accept(this);
		for(Expression i : orExpression.things)
		{
			this.addItem("or");
			i.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(PostfixOp postfixOp) {
		//visitList.add("PostfixOp");
		postfixOp.p2.accept(this);
		this.addItem(""+postfixOp.postfix);
		return null;
	}

	@Override
	public Object visit(PowOperator powOperator) {
		//visitList.add("PostfixOp");
		powOperator.expr.accept(this);
		this.addItem("**");
		powOperator.raiseTo.accept(this);
		return null;
	}

	@Override
	public Object visit(PrefixOp prefixOp) {
		//visitList.add("PrefixOp");
		this.addItem(""+prefixOp.prefix);
		prefixOp.p1.accept(this);
		return null;
	}

	@Override
	public Object visit(PrimativeType primativeType) {
		//visitList.add("PrimativeType");

		printInOutGenModi(primativeType);
		
		String str = "";
		
		int pnt = primativeType.getPointer();
		if(pnt != 0) {
			for(int n =0; n < pnt; n++) {
				str += "*";
			}
		}
		
		str+=primativeType.getJavaSourceType();
		
		this.addItem(str);//this contains array levels already
		/*for(int n = 0; n < primativeType.getArrayLevels(); n++)
		{
			this.addItem("[]"); 
		}*/
		
		if(primativeType.getNullStatus() == NullStatus.NULLABLE) {
			this.addItemNoPreString("?");
		}
		
		return null;
	}

	@Override
	public Object visit(RefBoolean refBoolean) {
		//visitList.add("RefBoolean");
		this.addItem(""+refBoolean.b);
		return null;
	}


	@Override
	public Object visit(RefClass refClass) {
		//visitList.add("RefClass");
		refClass.lhsType.accept(this);
		this.addItem(".");
		this.addItem("class");
		return null;
	}
	
	@Override
	public Object visit(RefName refName) {
		if(null != refName.astRedirectforOnChangeNesting){
			return refName.astRedirectforOnChangeNesting.accept(this);
		}
		else{
			//visitList.add("RefName");
			addNamePreDollarLevel(refName.name);
			return null;
		}
	}
	
	@Override
	public Object visit(RefOf refThis) {
		//visitList.add("RefOf");
		this.addItem("of");
		return null;
	}

	@Override
	public Object visit(RefSuper refSuper) {
		//visitList.add("RefSuper");
		this.addItem("super");
		
		if(refSuper.superQuali != null) {
			this.addItem("[" + refSuper.superQuali + "]");
		}
		
		return null;
	}

	@Override
	public Object visit(RefThis refThis) {
		//visitList.add("RefThis");
		this.addItem("this");
		
		if(refThis.qualifier != null){
			this.addItem("[");
			this.addItem(refThis.qualifier);
			this.addItem("]");		
		}
		
		return null;
	}

	/*
	@Override
	public Object visit(SyncBlock syncBlock) {
		//visitList.add("SyncBlock");
		this.addItem("sync");
		this.addItem("(");
		syncBlock.syncOnObj.accept(this);
		this.addItem(")");
		syncBlock.b.accept(this);
		return null;
	}

*/
	@Override
	public Object visit(ReturnStatement returnStatement) {
		//visitList.add("ReturnStatement");
		this.addItem("return");
		if(null!=returnStatement.ret) returnStatement.ret.accept(this);
		
		return null;
	}
	

	@Override
	public Object visit(ShiftElement shiftElement) {
		//visitList.add("MulerElement");
		this.addItem(""+shiftElement.shiftOp);
		shiftElement.expr.accept(this);
		return null;
	}

	@Override
	public Object visit(ShiftExpression shiftExpression) {
		//visitList.add("MulerExpression");
		shiftExpression.header.accept(this);
		for(ShiftElement e: shiftExpression.elements)
		{
			e.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(SizeofStatement sizeofstmt){
		
		if(sizeofstmt.variant != null) {
			this.addItemNoPreString("sizeof<" + sizeofstmt.variant + ">");
		}else {
			this.addItem("sizeof");
		}
		
		sizeofstmt.e.accept(this);
		
		return null;
	}
	
	@Override
	public Object visit(SuperConstructorInvoke superConstructorInvoke) {
		//visitList.add("SuperConstructorInvoke");
		this.addItem("super");
		//this.addItem(".");
		superConstructorInvoke.args.accept(this);
		return null;
	}

	@Override
	public Object visit(ThisConstructorInvoke thisConstructorInvoke) {
		//visitList.add("ThisConstructorInvoke");
		this.addItem("this");
		this.addItem(".");
		thisConstructorInvoke.args.accept(this);
		return null;
	}

	@Override
	public Object visit(ThrowStatement throwStatement) {
		//visitList.add("ThrowStatement");
		this.addItem("throw");
		throwStatement.thingTothrow.accept(this);
		return null;
	}

	@Override
	public Object visit(TransBlock transBlock) {
		//visitList.add("IfStatement");
		if(null != transBlock.astRedirect){
			transBlock.astRedirect.accept(this);
		}else{
			this.addItem("trans");
			transBlock.blk.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(TryCatch tryCatch) {
		//visitList.add("TryCatch");
		
		if(tryCatch.astRepoint != null){
			return tryCatch.astRepoint.accept(this);
		}
		
		this.addItem("try");
		
		if(!tryCatch.tryWithResources.isEmpty()){
			this.addItem("(");
			for(Line lh : tryCatch.tryWithResources){
				lh.accept(this);
				this.addItem(";");
			}
			this.popLastItem();
			this.addItem(")");
		}
		
		tryCatch.blockToTry.accept(this);
		for(CatchBlocks cat : tryCatch.cbs)
		{
			cat.accept(this);
		}
		if(tryCatch.finalBlock != null)
		{
			this.addItem("finally");
			tryCatch.finalBlock.accept(this);
		}
		return null;
	}

	@Override
	public Object visit(TypedCaseExpression typedCaseExpression){
		if(!typedCaseExpression.types.isEmpty()){
			for(Type tt : typedCaseExpression.types){
				tt.accept(this);
				this.addItem("or");
			}
			this.popLastItem();
		}
		
		if(null != typedCaseExpression.caseExpression){
			this.addItem(";");
			typedCaseExpression.caseExpression.accept(this);
		}
		
		if(typedCaseExpression.alsoCondition != null){
			this.addItem("also");
			typedCaseExpression.alsoCondition.accept(this);
		}
		
		return null;
	}

	@Override
	public Object visit(TypedefStatement typedefStatement) {
		
		if(null != typedefStatement.accessModifier){
			this.addItem(""+typedefStatement.accessModifier);
		}
		
		this.addItem("typedef");
		
		this.addItem(typedefStatement.name);
		
		if(!typedefStatement.typedefargs.isEmpty()){
			this.addItem("<");
			for(String arg : typedefStatement.typedefargs){
				this.addItem(arg);
				this.addItemNoPreString(",");
			}
			this.popLastItem();
			this.addItem(">");
		}
		
		this.addItem("=");
		
		typedefStatement.type.accept(this);
		
		return null;
	}

	@Override
	public Object visit(TypeReturningExpression typeReturningExpression) {//not used
		return null;
	}
	
	@Override
	public Object visit(UsingStatement usingStatement) {
		//visitList.add("UsingStatement");
		this.addItem("using");
		boolean d = false;
		for(DottedAsName dan : usingStatement.asnames)
		{
			dan.accept(this);
			this.addItemNoPreString(",");
			d=true;
		}
		if(d) popLastItem();
		return null;
	}

	@Override
	public Object visit(VarChar varString) {
		//visitList.add("VarChar");
		this.addItem("c\"");
		this.addItem(""+varString.chr);
		this.addItem("\"");
		
		return null;
	}
	
	@Override
	public Object visit(VarDouble varDouble) {
		//visitList.add("VarDouble");
		this.addItem(""+varDouble.doubler);
		return null;
	}


	@Override
	public Object visit(VarFloat varFloat) {
		//visitList.add("VarFloat");
		this.addItem(""+varFloat.floater + "f");
		return null;
	}
	
	@Override
	public Object visit(VarInt varInt) {
		//visitList.add("VarInt");
		this.addItem(""+varInt.inter);
		return null;
	}

	@Override
	public Object visit(VarLong varLong) {
		//visitList.add("VarLong");
		this.addItem(""+varLong.longer + "L");
		return null;
	}

	@Override
	public Object visit(VarNull varNull) {
		//visitList.add("VarNull");
		this.addItem("null");
		
		return null;
	}

	@Override
	public Object visit(VarRegexPattern varString) {
		//visitList.add("VarRegexPattern");
		this.addItem("r\"");
		this.addItem(varString.str);
		this.addItem("\"");
		
		return null;
	}
	
	@Override
	public Object visit(VarShort varLong) {
		//visitList.add("VarLong");
		this.addItem(varLong.shortx + "s");
		return null;
	}
	@Override
	public Object visit(VarString varString) {
		//visitList.add("VarString");
		this.addItem("\"");
		this.addItem(varString.str);
		this.addItem("\"");
		
		return null;
	}
	
	@Override
	public Object visit(Vectorized vectorized) {
		vectorized.expr.accept(this);
		
		if(vectorized.nullsafe) {
			this.addItemNoPreString("?");
		}
		
		if(vectorized.doubledot){
			this.addItemNoPreString("^^");
		}else{
			this.addItemNoPreString("^");
		}
		
		return null;
	}
	
	@Override
	public Object visit(VectorizedArrayRef arrayRef) {
		arrayRef.expr.accept(this);

		if(arrayRef.nullsafe) {
			this.addItemNoPreString("?");
		}
		
		if(arrayRef.doubledot){
			this.addItemNoPreString("^^");
		}else{
			this.addItemNoPreString("^");
		}

		processArrayRefElements(arrayRef.arrayLevelElements);
		
		return null;
	}
	
	@Override
	public Object visit(VectorizedFieldRef vectorizedFieldRef) {
		vectorizedFieldRef.expr.accept(this);
		

		if(vectorizedFieldRef.nullsafe) {
			this.addItemNoPreString("?");
		}
		
		if(vectorizedFieldRef.doubledot){
			this.addItemNoPreString("^^");
		}else{
			this.addItemNoPreString("^");
		}
		
		vectorizedFieldRef.name.accept(this);
		
		return null;
	}
	
	@Override
	public Object visit(VectorizedFuncInvoke vectorizedFuncInvoke) {
		vectorizedFuncInvoke.expr.accept(this);
		
		if(vectorizedFuncInvoke.nullsafe) {
			this.addItemNoPreString("?");
		}
		
		if(vectorizedFuncInvoke.doubledot){
			this.addItemNoPreString("^^");
		}else{
			this.addItemNoPreString("^");
		}
		
		vectorizedFuncInvoke.funcInvoke.accept(this);
		
		return null;
	}
	
	@Override
	public Object visit(VectorizedFuncRef vectorizedFuncRef) {
		vectorizedFuncRef.expr.accept(this);

		if(vectorizedFuncRef.nullsafe) {
			this.addItemNoPreString("?");
		}
		
		if(vectorizedFuncRef.doubledot){
			this.addItemNoPreString("^^");
		}else{
			this.addItemNoPreString("^");
		}
		
		vectorizedFuncRef.funcRef.accept(this);
		
		return null;
	}
	
	@Override
	public Object visit(VectorizedNew vectorizedNew) {
		vectorizedNew.lhs.accept(this);
		
		if(vectorizedNew.nullsafe) {
			this.addItemNoPreString("?");
		}
		
		if(vectorizedNew.doubledot){
			this.addItemNoPreString("^^");
		}else{
			this.addItemNoPreString("^");
		}
		
		vectorizedNew.constru.accept(this);
		
		return null;
	}
	
	@Override
	public Object visit(WhileBlock whileBlock) {
		//visitList.add("WhileBlock");
		this.addItem("while");
		this.addItem("(");
		whileBlock.cond.accept(this);
		
		if(whileBlock.idxVariableCreator != null){
			this.addItem(";");
			whileBlock.idxVariableCreator.accept(this);
		}
		else if(whileBlock.idxVariableAssignment != null){
			this.addItem(";");
			whileBlock.idxVariableAssignment.accept(this);
		}
		
		this.addItem(")");
		whileBlock.block.accept(this);

		if(whileBlock.elseblock != null){
			this.addItem("else");
			whileBlock.elseblock.accept(this);
		}
		return null;
	}
	
	@Override
	public Object visit(WithBlock withBlock) {
		//visitList.add("WithBlock");
		this.addItem("with");
		this.addItem("(");
		withBlock.expr.accept(this);
		this.addItem(")");
		withBlock.blk.accept(this);
		
		return null;
	}
	
	private void visitBlock(Block block, boolean addBraces) {
		//visitList.add("Block");
		if(addBraces){
			this.addItem("{\n");
		}
		indentLevel++;
		LineHolder lh = block.startItr();
		
		while(lh != null)
		{
			Line l = lh.l;
			if(null != l){
				l.accept(this);
				if(l instanceof NOP){
					this.addItem("\n");
				}
				else if(!(l instanceof CompoundStatement)){
					//this.addItem("; <-- "+l.getClass() + "\n");
					this.addItem(";\n");
				}
			}
			
				
			lh = block.getNext();
		}
		indentLevel--;
		if(addBraces){
			this.addItem("}\n");
		}
	}
	
	@Override
	public Object visit(PointerAddress pointerAddress) {
		this.addItem("~");
		pointerAddress.rhs.accept(this);
		return null;
	}
	@Override
	public Object visit(PointerUnref pointerUnref) {
		String pnt = "*";
		for(int n=0; n < pointerUnref.size-1; n++) {
			pnt += "*";
		}
		this.addItem(pnt);
		pointerUnref.rhs.accept(this);
		return null;
	}
	
	@Override
	public Object visit(AssignMulti multiAssign) {
		for(Assign ass:  multiAssign.assignments) {
			Expression was = ass.setRHSExpression(null);
			ass.accept(this);
			ass.setRHSExpression(was);
		}
		multiAssign.rhs.accept(this);
		return null;
	}
	
	@Override
	public Object visit(JustLoad justLoad) {
		return null;//ignore, used in bytecode genneration only
	}
	
	@Override
	public Object visit(TupleExpression tupleExpression) {
		this.addItem("(");
		int sz= tupleExpression.tupleElements.size();
		for(int n = 0; n < sz; n++)
		{	
			tupleExpression.tupleElements.get(n).accept(this);
			if(n !=  sz-1)
			{
				this.addItemNoPreString(", ");
			}
		}
		this.addItem(")");
		return null;
	}
	

	@Override
	public Object visit(CaseExpressionTuple caseExpressionTuple) {
		this.addItem("(");
		int sz= caseExpressionTuple.getComponents().size();
		for(int n = 0; n < sz; n++)	{	
			CaseExpression expr = caseExpressionTuple.getComponents().get(n);
			if(null != expr) {
				expr.accept(this);
			}
			if(n !=  sz-1)
			{
				this.addItemNoPreString(", ");
			}
		}
		this.addItem(")");
		
		
		
		return null;
	}
	
	
	@Override
	public Object visit(AssignTupleDeref assignTupleDeref) {
		this.addItem("(");
		int sz= assignTupleDeref.lhss.size();
		for(int n = 0; n < sz; n++)
		{	
			Assign ass = assignTupleDeref.lhss.get(n);
			if(ass != null) {
				ass.accept(this);
			}
			
			if(n !=  sz-1)
			{
				this.addItemNoPreString(", ");
			}
		}
		this.addItem(")");
		this.addItem(""+assignTupleDeref.eq);
		
		if(null != assignTupleDeref.expr) {
			assignTupleDeref.expr.accept(this);
		}
		
		return null;
	}
	
	@Override
	public Object visit(AnonLambdaDef anonLambdaDef) {
		ArrayList<Pair<String, Type>> inputs = anonLambdaDef.getInputs();
		int sz= inputs.size();
		for(int n = 0; n < sz; n++){	
			Pair<String, Type> inst = inputs.get(n);
			
			this.addItem(inst.getA());
			Type tt = inst.getB();
			if(tt != null) {
				tt.accept(this);
			}
			
			if(n != sz-1){
				this.addItemNoPreString(", ");
			}
		}
		
		if(null != anonLambdaDef.retType) {
			anonLambdaDef.retType.accept(this);
		}
		
		this.addItem("=>");
		anonLambdaDef.body.accept(this);
		
		return null;
	}
	
	
	@Override
	public Object visit(ObjectProvider objectProvider) {
		if(objectProvider.annotations != null){
			objectProvider.annotations.accept(this);
			this.addItem("\n");
		}
		
		if(objectProvider.accessModifier != null) {
			this.addItem(objectProvider.accessModifier.toString());
		}
		
		if(objectProvider.isTransient) {
			this.addItem("transient");
		}
		
		if(objectProvider.isShared) {
			this.addItem("shared");
		}
		
		this.addItem("provider");
		
		this.addItem(objectProvider.providerName);
		
		if(!objectProvider.classGenricList.isEmpty())
		{
			this.addItem("<");
			for(Object n : objectProvider.classGenricList)
			{
				if(n instanceof GenericType) {
					((GenericType)n).accept(this);
				}else {
					this.addItem(""+n);
				}
				this.addItemNoPreString(",");
			}
			popLastItem();
			this.addItem(">");
		}
		
		if(null != objectProvider.classDefArgs) {
			this.addItem("(");
			objectProvider.classDefArgs.accept(this);
			this.addItem(")");
		}
		
		objectProvider.objectProviderBlock.accept(this);
		this.addItem("\n");
		return null;
	}
	@Override
	public Object visit(ObjectProviderBlock objectProviderBlock) {
		this.addItem("{\n");
		indentLevel++;
		
		for(ObjectProviderLine opl : objectProviderBlock.lines) {
			opl.accept(this);
			this.addItem("\n");
		}

		indentLevel--;
		this.addItem("}\n");
		return null;
	}
	@Override
	public Object visit(ObjectProviderLineDepToExpr objectProviderLineDepToExpr) {
		if(objectProviderLineDepToExpr.single) {
			this.addItem("single");
		}
		
		lineGenerics(objectProviderLineDepToExpr.getLocalGens());
		
		if(objectProviderLineDepToExpr.name != null) {
			this.addItem("'" + objectProviderLineDepToExpr.name + "'");
		}
		
		objectProviderLineDepToExpr.dependency.accept(this);
		
		if(null != objectProviderLineDepToExpr.fulfilment) {
			this.addItem("=>");
			objectProviderLineDepToExpr.fulfilment.accept(this);
		}else {
			this.addItem("<=");
			objectProviderLineDepToExpr.typeOnlyRHS.accept(this);
			if(objectProviderLineDepToExpr.nestedDeps != null) {
				this.addItem("{\n");
				indentLevel++;
				
				objectProviderLineDepToExpr.nestedDeps.forEach(a -> {a.accept(this); this.addItem("\n");}); 

				indentLevel--;
				this.addItem("}");
			}
		}
		
		
		return null;
	}
	@Override
	public Object visit(ObjectProviderLineProvide objectProviderLineProvide) {
		
		if(objectProviderLineProvide.single) {
			this.addItem("single");
		}
		
		this.addItem("provide");
		
		lineGenerics(objectProviderLineProvide.getLocalGens());
		
		if(objectProviderLineProvide.fieldName != null) {
			this.addItem("'" + objectProviderLineProvide.fieldName + "'");
		}
		
		if(objectProviderLineProvide.provName != null) {
			this.addItem(objectProviderLineProvide.provName);
		}
		objectProviderLineProvide.provides.accept(this);
		
		if(objectProviderLineProvide.provideExpr != null) {
			this.addItem("=>");
			objectProviderLineProvide.provideExpr.accept(this);
		}else if(objectProviderLineProvide.nestedDeps != null) {
			this.addItem("{\n");
			indentLevel++;
			
			objectProviderLineProvide.nestedDeps.forEach(a -> {a.accept(this); this.addItem("\n");}); 

			indentLevel--;
			this.addItem("}");
		}
		
		return null;
	}
	@Override
	public Object visit(NotNullAssertion notNullAssertion) {
		notNullAssertion.expr.accept(this);
		this.addItem("??");
		return null;
	}
	
	@Override
	public Object visit(ElvisOperator notNullAssertion) {
		notNullAssertion.lhsExpression.accept(this);
		this.addItem("?:");
		notNullAssertion.rhsExpression.accept(this);
		return null;
	}

	@Override
	public Object visit(LangExt langExt) {
		this.addItem(langExt.name + "||" + langExt.body +"||");
		return null;
	}
	
}
