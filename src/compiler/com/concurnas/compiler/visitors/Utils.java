package com.concurnas.compiler.visitors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.atn.PredictionMode;

import com.concurnas.compiler.ASTCreator;
import com.concurnas.compiler.ConcurnasLexer;
import com.concurnas.compiler.ConcurnasParser;
import com.concurnas.compiler.ErrorHolder;
import com.concurnas.compiler.LexParseErrorCapturer;
import com.concurnas.compiler.ConcurnasParser.LonleyExpressionContext;
import com.concurnas.compiler.ast.AccessModifier;
import com.concurnas.compiler.ast.Annotation;
import com.concurnas.compiler.ast.Annotations;
import com.concurnas.compiler.ast.AnonLambdaDef;
import com.concurnas.compiler.ast.ArrayDef;
import com.concurnas.compiler.ast.ArrayDefComplex;
import com.concurnas.compiler.ast.Assign;
import com.concurnas.compiler.ast.AssignExisting;
import com.concurnas.compiler.ast.AssignStyleEnum;
import com.concurnas.compiler.ast.AsyncRefRef;
import com.concurnas.compiler.ast.Block;
import com.concurnas.compiler.ast.CastExpression;
import com.concurnas.compiler.ast.CatchBlocks;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.ast.DotOperator;
import com.concurnas.compiler.ast.DuffAssign;
import com.concurnas.compiler.ast.ElifUnit;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.FuncInvoke;
import com.concurnas.compiler.ast.FuncInvokeArgs;
import com.concurnas.compiler.ast.FuncParam;
import com.concurnas.compiler.ast.FuncParams;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.IfStatement;
import com.concurnas.compiler.ast.LambdaDef;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.New;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.NullStatus;
import com.concurnas.compiler.ast.REPLTopLevelComponent;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.ReturnStatement;
import com.concurnas.compiler.ast.ThrowStatement;
import com.concurnas.compiler.ast.TryCatch;
import com.concurnas.compiler.ast.TupleExpression;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.TypeReturningExpression;
import com.concurnas.compiler.ast.VarNull;
import com.concurnas.compiler.ast.VarString;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.Thruple;
import com.concurnas.runtime.Pair;
import com.concurnas.compiler.LocationRepointCommonTokenFactory;

public class Utils {
	public static String listToStr(ArrayList<String> list)
	{
		return listToStr(list, "");
	}
	
	public static String getMiniTestFileName(String input)
	{
		//"cls" + input.replaceAll("[^A-Za-z0-9]", "_");
		return "x" + input.replaceAll(" ", "").replaceAll(",", "").replaceAll("\\.", "").replaceAll("-", "").replaceAll("\\[", "").replaceAll("\\]", "");//this is ok, java class files cannot have dots in them
	}
	
	public static HashSet<TypeAndLocation> funcsWithoutRets(HashSet<TypeAndLocation> xxx){
		if(xxx == null){
			return null;
		}
		
		HashSet<TypeAndLocation> ret = new HashSet<TypeAndLocation>();
		
		for(TypeAndLocation x: xxx){
			ret.add(x.cloneWithNoRetType());
		}
		
		return ret;
	}
	
	private static String pprintExpr(Expression path){
		PrintSourceVisitor visitor = new PrintSourceVisitor();
		path.accept(visitor);
		return "" + visitor;
	}
	
	public static String listToStr(ArrayList<String> list, String prefix)
	{
		StringBuilder sb = new StringBuilder();
		for(String s : list)
		{
			if(!s.trim().equals("") && !s.trim().startsWith("Test:"))
			{
				sb.append(prefix);
			}
			sb.append( s);
			sb.append('\n');
		}
		return sb.toString();
		
	}

	private static final NamedType THROWABLE_NAMEDTYPE = new NamedType(new ClassDefJava(Throwable.class));
	
	public static Block makeAssignToRefFromrhsBlock(Block rhs, Type ret, boolean includeRetAssignmentFromBlock){
		int line = rhs.getLine();
		int col = rhs.getColumn();
		Block newbodyHoldingTryCatch = new Block(line, col);
		
		
				
		Block blockToTry = includeRetAssignmentFromBlock?new Block(line, col):rhs;
		ArrayList<CatchBlocks> cbs = new ArrayList<CatchBlocks>();
		Block catchBlock = new Block(line, col);
		
		ArrayList<Type> caTypes = new ArrayList<Type>(1);
		caTypes.add(THROWABLE_NAMEDTYPE);
		CatchBlocks catcher = new CatchBlocks(line, col, "e", caTypes, catchBlock);
		cbs.add(catcher);
		TryCatch tc = new TryCatch(line, col, blockToTry, cbs, null);
		
		DotOperator doa = new DotOperator(line, col, new AsyncRefRef(line, col, new RefName(line, col, "ret$"), 1), new FuncInvoke(line, col, "setException", new RefName(0,0,"e") ) );
		
		catchBlock.add(new LineHolder(line, col, new DuffAssign(line, col,doa) ));
		
		newbodyHoldingTryCatch.add(new LineHolder(line, col, tc ));//ret = { a + 4; };
		//newbodyHoldingTryCatch.isOnItsOwnLine=true;
		
		if(includeRetAssignmentFromBlock){
			if(TypeCheckUtils.isVoid(ret) || TypeCheckUtils.isVoid(rhs.getTaggedType())){
				/*return something, therefore:
				res = { a = 4; }!
				->
				ret int:
				lambda: [ try{{ a = 4; }; ret = null}catch(Throwable e){ ret:setException(e) }; ]
				 */
				blockToTry.add(new LineHolder(line, col, new DuffAssign(line, col, rhs) ));//{ a = 4; };
				AssignExisting ae = new AssignExisting(line, col, "ret$", AssignStyleEnum.EQUALS, new New(line, col, ScopeAndTypeChecker.const_object, new FuncInvokeArgs(line, col), true));
				blockToTry.add(new LineHolder(line, col, ae));//;ret = null 
			}
			else{
				/*return something, therefore:
				res = { a + 4; }!
				->
				ret int:
				lambda: [ try{ret = { a + 4; }}catch(Throwable e){ ret:setException(e) }; ]
				 */
				AssignExisting ae = new AssignExisting(line, col, "ret$", AssignStyleEnum.EQUALS, rhs);
				ae.refCnt=TypeCheckUtils.getRefLevels(ret);//ensure that a:= {thing}// since a gets passed in as a ref:://i.e. we are creating a new one to which we wish the result to be set into for this: res = {12:}!
				blockToTry.add(new LineHolder(line, col, ae ));//ret = { a + 4; };
			}
		}
		
		return newbodyHoldingTryCatch;
	}
	
	public static ArrayList<String> erListToStrList(ArrayList<ErrorHolder> list, boolean flatten){
		ArrayList<String> sb = new ArrayList<String>();
		for(ErrorHolder s : list)
		{
			String line = s.toString();
			if(flatten) {
				line = line.replace('\n', '~');
				line = line.replace('\r', '~');
			}
			sb.add(line);
		}
		return sb;
	}
	
	public static ArrayList<String> erListToStrList(ArrayList<ErrorHolder> list)
	{
		return erListToStrList(list, false);
	}
	
	public static ArrayList<String> erListToStrListNOFN(ArrayList<ErrorHolder> list) {
		
		ArrayList<String> sb = new ArrayList<String>();
		for(ErrorHolder s : list)
		{
			sb.add(s.toStringNoFileName());
		}
		return sb;
	}
	
	public static String listToStringAny(List<? extends Object> items)
	{
		StringBuilder sb = new StringBuilder();
		int ilen = items.size();
		for(int n=0; n < ilen; n++){
			sb.append(items.get(n));
			if(n != ilen-1){
				sb.append(", ");
			}
		}
		return sb.toString();
	}
	
	
	public static String listToString(ArrayList<String> items)
	{
		StringBuilder sb = new StringBuilder();
		for(String item : items)
		{
			sb.append(item);
			if(null == item)
			{
				sb.append("-NULL WARN-");
			}
			if(null!= item&& !item.endsWith("\n"))
			{
				sb.append(' ');
			}
		}
		String ret = sb.toString();
		return ret;
	}
	
	public static String readFile(String filename) throws Exception
	{
		BufferedReader in = null;
		StringBuilder ret = new StringBuilder();
		try
		{
			in = new BufferedReader(new FileReader(filename));
			String ll = in.readLine();
			while(null != ll)
			{
				ret.append(ll);
				ll = in.readLine();
				if(null != ll )
				{
					ret.append("\n");
				}
			}
		}
		catch(Exception e)
		{
			try
			{
				in.close();
			}
			catch(Exception ee)
			{
			}
			throw e;
		}
		in.close();
		return ret.toString();
	}

	public static ArrayList<Type> removeGenericUpperBounds(ArrayList<Type> argumentsThatNeedToBecurriedIn) {
		ArrayList<Type> ret = new ArrayList<Type>(argumentsThatNeedToBecurriedIn.size());
		for(Type t : argumentsThatNeedToBecurriedIn){
			ret.add(removeGenericUpperBounds(t));
		}
		return ret;
	}

	public static Pair<List<Pair<String, Boolean>>, List<Pair<String, Boolean>>> filterNullInferMap(List<HashMap<Pair<String, Boolean>, NullStatus>> inferedNullinBlocks) {
		HashMap<Pair<String, Boolean>, NullStatus> base = inferedNullinBlocks.remove(0);
		
		List<Pair<String, Boolean>> nonnull = new ArrayList<Pair<String, Boolean>>();
		List<Pair<String, Boolean>> nullable = new ArrayList<Pair<String, Boolean>>();
		
		for(Pair<String, Boolean> key : base.keySet() ) {
			NullStatus ns = base.get(key);
			Set<NullStatus> nss = new HashSet<NullStatus>();
			nss.add(ns);
			for(HashMap<Pair<String, Boolean>, NullStatus> inst : inferedNullinBlocks) {
				nss.add(inst.get(key));
			}
			
			List<Pair<String, Boolean>> addTo = (nss.size() > 1 || nss.contains(NullStatus.NULLABLE))? nullable:nonnull;
			addTo.add(key);
		}
		
		return new Pair<List<Pair<String, Boolean>>, List<Pair<String, Boolean>>>(nonnull, nullable);
	}
	
	public static Type removeGenericUpperBounds(Type retType) {
		if(null == retType){
			return retType;
		}
		if(retType instanceof FuncType){
			retType = (FuncType)retType.copy();
			((FuncType)retType).setInputs(removeGenericUpperBounds( ((FuncType)retType).getInputs()));
			((FuncType)retType).retType = removeGenericUpperBounds(((FuncType)retType).retType);
			((FuncType)retType).setOrigonalGenericTypeUpperBound(null);
		}
		else{
			retType = (Type)retType.copy();
			retType.setOrigonalGenericTypeUpperBound(null);
		}
		/*else if(retType instanceof NamedType){
			retType = retType.copy();
			((NamedType)retType).origonalGenericTypeUpperBound = null;
		}*/
		
		
		return retType; 
	}

	public static boolean isNarrowingScope(AccessModifier thisAM, AccessModifier supAM) {
		return thisAM.getCatagory() < supAM.getCatagory();
	}

	public static ConcurnasParser miniParse(String text, String name, int startingLine, int startingColumn, LexParseErrorCapturer errors) {
		CharStream input = CharStreams.fromString(text, name);
		
		ConcurnasLexer lexer = new ConcurnasLexer(input);
		if(startingLine > -1) {
			lexer.setTokenFactory(new LocationRepointCommonTokenFactory(startingLine,startingColumn));
		}
		
		lexer.permitDollarPrefixRefName = true;
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		ConcurnasParser parser = new ConcurnasParser(tokens);
		
		parser.getInterpreter().setPredictionMode(PredictionMode.SLL); 
		
		parser.removeErrorListeners(); // remove ConsoleErrorListener
		parser.addErrorListener(errors); // add ours
		
		lexer.removeErrorListeners(); // remove ConsoleErrorListener
		lexer.addErrorListener(errors); // add ours
		
		return parser;
	}
	
	public static Block parseBlock(String text, String name, int startingLine, boolean canRet) {
		//JPT: transition more code to use this style? - this is quite cool, massive time saver i think
		try{
			LexParseErrorCapturer errors = new LexParseErrorCapturer(name);
			
			ConcurnasParser op = miniParse(text, name, startingLine, 0, errors);
			Block ret = (Block)new ASTCreator(name, errors).visit(op.code());
			//OwlParser.lineOffset.set(0);
			if(!errors.errors.isEmpty()){
				throw new RuntimeException("Internal compiler error: " + errors.errors);
			}
			ret.canContainAReturnStmt=canRet;
			return ret;
		}
		catch(RecognitionException re){
			throw new RuntimeException(re);
		}
	}
	
	
	public static ClassDef parseClass(String text, String name, int startingLine, boolean canRet) {
		//JPT: transition more code to use this style? - this is quite cool, massive time saver i think
		try{
			LexParseErrorCapturer errors = new LexParseErrorCapturer(name);
			ConcurnasParser op = miniParse(text, name, startingLine, 0,  errors);
			ClassDef ret = (ClassDef)new ASTCreator(name, errors).visit(op.classdef()); 
			//OwlParser.lineOffset.set(0);
			if(!errors.errors.isEmpty()){
				throw new RuntimeException("Internal compiler error: " + errors.errors);
			}
			return ret;
		}
		catch(RecognitionException re){
			throw new RuntimeException(re);
		}
	}
	
	public static FuncDef parseFuncDef(String text, String name, int startingLine) {
		//JPT: transition more code to use this style? - this is quite cool, massive time saver i think
		try{
			LexParseErrorCapturer errors = new LexParseErrorCapturer(name);
			ConcurnasParser op = miniParse(text, name, startingLine,  0, errors);
			FuncDef ret = (FuncDef)new ASTCreator(name, errors).visit(op.funcdef()); 
			//OwlParser.lineOffset.set(0);
			if(!errors.errors.isEmpty()){
				throw new RuntimeException("Internal compiler error: " + errors.errors);
			}
			return ret;
		}
		catch(RecognitionException re){
			throw new RuntimeException(re);
		}
	}
	
	public static Assign parseAssignStmt(String text, String name, int startingLine) {
		//JPT: transition more code to use this style? - this is quite cool, massive time saver i think
		try{
			LexParseErrorCapturer errors = new LexParseErrorCapturer(name);
			ConcurnasParser op = miniParse(text, name, startingLine,  0, errors);
			Assign ret = (Assign)new ASTCreator(name, errors).visit(op.assignment()); 
			//OwlParser.lineOffset.set(0);
			if(!errors.errors.isEmpty()){
				throw new RuntimeException("Internal compiler error: " + errors.errors);
			}
			return ret;
		}
		catch(RecognitionException re){
			throw new RuntimeException(re);
		}
	}
	
	
	
	public static ReturnStatement parseReturnStatement(String text, String name, int startingLine) {
		//JPT: transition more code to use this style? - this is quite cool, massive time saver i think
		try{
			LexParseErrorCapturer errors = new LexParseErrorCapturer(name);
			ConcurnasParser op = miniParse(text, name, startingLine, 0,  errors);
			ReturnStatement ret = (ReturnStatement)new ASTCreator(name, errors).visit(op.return_stmt()); 
			//OwlParser.lineOffset.set(0);
			
			if(!errors.errors.isEmpty()){
				throw new RuntimeException("Internal compiler error: " + errors.errors);
			}
			return ret;
		}
		catch(RecognitionException re){
			throw new RuntimeException(re);
		}
	}
	
	

	public static ArrayList<Type> parseGenTypeList(String text) {
		try {
			LexParseErrorCapturer errors = new LexParseErrorCapturer("");
			ConcurnasParser op = miniParse(text, "", 0,  0, errors);
			//visitGenTypeList
			ArrayList<Type> ret = (ArrayList<Type>)new ASTCreator("", errors).visitGenTypeList(op.genTypeList()); 
			//OwlParser.lineOffset.set(0);
			if(!errors.errors.isEmpty()){
				throw new RuntimeException("Internal compiler error: " + errors.errors);
			}
			return ret;
		} catch (Exception e) {
			return null;
		}
	}
	
	
	
	public static Expression parseExpression(String text, String name, int startingLine, int startcolumn, ScopeAndTypeChecker sac) {
		//JPT: transition more code to use this style? - this is quite cool, massive time saver i think
		try{
			LexParseErrorCapturer errors = new LexParseErrorCapturer("");
			ConcurnasParser op = miniParse(text, name, startingLine, startcolumn, errors);
			//OwlParser.columOffset.set(startcolumn);
			LonleyExpressionContext pt = op.lonleyExpression();
			Expression ret = null;
			if(errors.errors.isEmpty()){
				DuffAssign da = (DuffAssign)new ASTCreator(name, errors).visit(pt); 
				ret = da.e;
			}
			//OwlParser.lineOffset.set(0);
			//OwlParser.columOffset.set(0);
			if(!errors.errors.isEmpty()){
				for(ErrorHolder er : errors.errors){
					if(er.isWarning()){
						sac.raiseWarning(startingLine-1 + er.getLine(), startcolumn + er.getColumn(), er.getMessage() + " in format string: {" + text + "}", er.getWarning());
					}
					else{
						sac.raiseError(startingLine-1 + er.getLine(), startcolumn + er.getColumn(), er.getMessage() + " in format string: {" + text + "}");
					}
				}
				return null;
			}
			
			return ret;
		}
		catch(RecognitionException re){
			sac.raiseError(startingLine, startcolumn, "cannot parse expression in string: " + re.getMessage());
		}
		return null;
	}
	
	public static ThrowStatement parsethrowStmt(String text, String name, int startingLine, int startcolumn, ScopeAndTypeChecker sac) {
		//JPT: transition more code to use this style? - this is quite cool, massive time saver i think
		//TODO: fix awful copy paste
		try{
			LexParseErrorCapturer errors = new LexParseErrorCapturer("");
			ConcurnasParser op = miniParse(text, name, startingLine, 0,  errors);
			//OwlParser.columOffset.set(startcolumn);
			ThrowStatement ret = (ThrowStatement)new ASTCreator(name, errors).visit(op.throw_stmt()); 
			//OwlParser.lineOffset.set(0);
			//OwlParser.columOffset.set(0);
			if(!errors.errors.isEmpty()){
				for(ErrorHolder er : errors.errors){
					if(er.isWarning()){
						sac.raiseWarning(startingLine-1 + er.getLine(), startcolumn + er.getColumn(), er.getMessage() + " in format string: {" + text + "}", er.getWarning());
					}
					else{
						sac.raiseError(startingLine-1 + er.getLine(), startcolumn + er.getColumn(), er.getMessage() + " in format string: {" + text + "}");
					}
				}
				return null;
			}
			
			return ret;
		}
		catch(RecognitionException re){
			sac.raiseError(startingLine, startcolumn, "cannot parse expression in string: " + re.getMessage());
		}
		return null;
	}


	public static List<Type> cloneArgs(List<Type> argsWanted) {
		List<Type> inputsCopy = new ArrayList<Type>(argsWanted.size());
		for(Type t : argsWanted)		{
			inputsCopy.add(t==null?null:(Type)t.copy());
		}
		return inputsCopy;
	}
	
	
	public static String justJoin(List sortedList) {
		StringBuilder sb = new StringBuilder();
		int sz = sortedList.size();
		for(int n=0; n < sz; n++){
			sb.append(sortedList.get(n));
			if(n < sz-1){
				sb.append(", ");
			}
		}
		return sb.toString();
	}
	
	public static String justJoin(List sortedList, String penutimatedelim) {
		StringBuilder sb = new StringBuilder();
		int sz = sortedList.size();
		for(int n=0; n < sz; n++){
			sb.append(sortedList.get(n));
			if(sz > 1 && n == sz-2){
				sb.append(penutimatedelim);
			}else if(n < sz-1){
				sb.append(", ");
			}
			
		}
		return sb.toString();
	}

	public static String sortAndJoin(List unsortedList) {
		Collections.sort(unsortedList);
		return justJoin(unsortedList);
	}
	
	public static String sortAndJoinSet(Collection<?> yourHashSet) {
		List sortedList = new ArrayList<>(yourHashSet);
		return sortAndJoin(sortedList);
	}

	public static String StringFormatArgs(ArrayList<Type> argsWanted, String start, String end) {
		if(null == argsWanted){
			return "";
		}
		StringBuilder sb = new StringBuilder(start);
		int sz = argsWanted.size();
		for(int n=0; n < sz; n++){
			sb.append(""+argsWanted.get(n));
			if(n != sz-1){
				sb.append(", ");
			}
		}
		sb.append(end);
		return sb.toString();
	}
	
	public static String StringFormatArgs(ArrayList<Type> argsWanted) {
		return StringFormatArgs(argsWanted, "(", ")");
	}

	public static HashSet<WarningVariant> extractSuppressedWarningsFromAnnotations(Annotations annots, ScopeAndTypeChecker satc) {
		//WarningVariant
		HashSet<WarningVariant> ret = new HashSet<WarningVariant>();
		if(annots != null ){
			for(Annotation anot : annots.annotations){
				HashSet<String> got = extractSuppressedWarningsFromAnnotation(anot, satc);
				for(String item : got){
					WarningVariant gg = WarningVariant.WarningVariantMap.get(item);
					if(null != gg){
						ret.add(gg);
					}
				}
				
				/*NamedType nt = (NamedType)anot.getTaggedType();
				if(nt != null && nt.equals(ScopeAndTypeChecker.const_Annotation_SuppressWarningsCls)){
					for(Thruple<String, Expression,Type> thing : anot.getArguments()){
						if(thing.getA().equals("value")){
							Object got = thing.getB().getFoldedConstant();
							if(got != null){
								if(got instanceof WarningVariant[]){
									for(WarningVariant et : (WarningVariant[])got){
										ret.add(et);
									}
								}
								else if(got instanceof String[]){
									for(String g : (String[])got){
										WarningVariant gg = WarningVariant.WarningVariantMap.get(g);
										if(gg != null){
											ret.add(gg);
										}
									}
								}
								else if(got instanceof WarningVariant){
									ret.add((WarningVariant)got);
								}
								else if(got instanceof String){
									WarningVariant gg = WarningVariant.WarningVariantMap.get(got);
									if(gg != null){
										ret.add(gg);
									}
								}
							}
						}
					}
					break;//can only have one of these anyway
				}*/
			}
		}
		
		return ret;
	}
	
	
	public static HashSet<String> extractSuppressedWarningsFromAnnotation(Annotation annot, ScopeAndTypeChecker satc) {
		//WarningVariant
		HashSet<String> ret = new HashSet<String>();
		
		NamedType nt = (NamedType)annot.getTaggedType();
		if(nt != null && nt.equals(ScopeAndTypeChecker.const_Annotation_SuppressWarningsCls)){
			for(Thruple<String, Expression,Type> thing : annot.getArguments()){
				if(thing.getA().equals("value")){
					Expression got = thing.getB();
					if(got != null){
						if(got instanceof ArrayDef){
							ArrayDef ay = (ArrayDef)got;
							for(Expression el : ay.getArrayElements(satc)){
								if(el instanceof VarString){
									ret.add(((VarString)el).str);
								}
							}
						}
						else if(got instanceof VarString){
							ret.add(((VarString)got).str);
						}
					}
				}
			}
		}
		
		return ret;
	}
	
	public static HashSet<ElementType> extractUsagetargets(Annotations annots){
		HashSet<ElementType> ret = new HashSet<ElementType>();
		if(annots != null ){
			for(Annotation anot : annots.annotations){
				NamedType nt = (NamedType)anot.getTaggedType();
				if(nt != null && nt.equals(ScopeAndTypeChecker.const_Annotation_TargetCls)){
					for(Thruple<String, Expression,Type> thing : anot.getArguments()){
						if(thing.getA().equals("value")){
							Object got = thing.getB().getFoldedConstant();
							if(got != null){
								if(got instanceof ElementType[]){
									for(ElementType et : (ElementType[])got){
										ret.add(et);
									}
								}
								else if(got instanceof String[]){
									for(String g : (String[])got){
										ret.add(ElementType.valueOf(g));
									}
								}
								else if(got instanceof ElementType){
									ret.add((ElementType)got);
								}
								else if(got instanceof String){
									ret.add(ElementType.valueOf((String)got));
								}
							}
						}
					}
					break;//can only have one of these anyway
				}
			}
		}
		
		return ret;
	}
	
	public static HashSet<RetentionPolicy> extractARetentionPolicy(Annotations annots){
		//copy paste
		HashSet<RetentionPolicy> ret = new HashSet<RetentionPolicy>();
		if(annots != null ){
			for(Annotation anot : annots.annotations){
				NamedType nt = (NamedType)anot.getTaggedType();
				if(null != nt && nt.equals(ScopeAndTypeChecker.const_Annotation_RetentionCls)){
					for(Thruple<String, Expression, Type> thing : anot.getArguments()){
						if(thing.getA().equals("value")){
							Object got = thing.getB().getFoldedConstant();
							if(got != null){
								if(got instanceof RetentionPolicy){
									ret.add((RetentionPolicy)got);
								}
								else if(got instanceof String){

									ret.add(RetentionPolicy.valueOf((String)got));
								}
							}
							else{
								ret.add(RetentionPolicy.RUNTIME);//cheat
							}
						}
					}
					break;//can only have one of these anyway
				}
			}
		}
		
		return ret;
	}

	public static boolean funcParamsHaveDefaults(FuncParams params) {
		for(FuncParam fp : params.params){
			if(null != fp.defaultValue){return true;}
		}
		
		return false;
	}
	
	
	
	public static class CurriedVararg{
		public final int startOfRangeforVarargCurry;
		public final int endOfRangeforVarargCurry;
		public final List<Object> expandedArgsIncVarargCapture;
		public final Type varargCurryArrayType;
		
		public CurriedVararg(final int startOfRangeforVarargCurry, final int endOfRangeforVarargCurry, final List<Object> expandedArgsIncVarargCapture, final Type varargCurryArrayType){
			this.startOfRangeforVarargCurry=startOfRangeforVarargCurry;
			this.endOfRangeforVarargCurry=endOfRangeforVarargCurry;
			this.expandedArgsIncVarargCapture=expandedArgsIncVarargCapture;
			this.varargCurryArrayType=varargCurryArrayType;
		}
	}
	
	
	public static CurriedVararg tagCurriedVararg(List<Object> exprArgs, ScopeAndTypeChecker satc){
		boolean hasVarargsNeedingCurryArgs = false;
		List<Object> expandedArgsIncVarargCapture = new ArrayList<Object>(exprArgs.size());
		int argn=0;
		int startOfRangeforVarargCurry = -1;
		int endOfRangeforVarargCurry = -1;
		Type varargCurryArrayType=null;
		for(Object arg: exprArgs){//what if we want a funcref on a vararg function.. e.g. fu(an Object...){} fu&('hi', String)//we need to pull out the sucked in args
			if(arg instanceof ArrayDef && !hasVarargsNeedingCurryArgs){
				ArrayDef ad = (ArrayDef)arg;
				for(Expression expr: ad.getArrayElements(satc)){
					if(expr instanceof TypeReturningExpression){
						hasVarargsNeedingCurryArgs = true;
						break;
					}
				}
				
				if(hasVarargsNeedingCurryArgs){//pull out and add individually
					startOfRangeforVarargCurry = argn;
					for(Expression expr: ad.getArrayElements(satc)){
						if(expr instanceof TypeReturningExpression){
							expandedArgsIncVarargCapture.add(((TypeReturningExpression)expr).type);
							argn++;
						}else{
							expandedArgsIncVarargCapture.add(expr);
							argn++;
						}
						varargCurryArrayType = (Type)ad.getTaggedType();//.copy();
						//varargCurryArrayType.setArrayLevels(varargCurryArrayType.getArrayLevels()-1);
					}
					endOfRangeforVarargCurry = argn;
				}else{
					expandedArgsIncVarargCapture.add(arg);
					argn++;
				}
				
			}else{
				expandedArgsIncVarargCapture.add(arg);
				argn++;
			}
		}
		
		return hasVarargsNeedingCurryArgs?new CurriedVararg(startOfRangeforVarargCurry, endOfRangeforVarargCurry, expandedArgsIncVarargCapture, varargCurryArrayType):null;
	}
	
	private static Pair<FuncType, Pair<NamedType, TypeAndLocation>> getFuncTypeForAnonLambdaAssign(ScopeAndTypeChecker satc, Type potential) {
		//see if we are mapping to a functype or a Single abstract method
		if(potential instanceof FuncType) {
			return new Pair<FuncType, Pair<NamedType, TypeAndLocation>>((FuncType)potential, null);
		}else if(potential instanceof NamedType){
			NamedType asNamed = (NamedType)potential;
			
			if(!asNamed.equals(ScopeAndTypeChecker.const_lambda_nt)) {
				if(asNamed.isInterface() || asNamed.isAbstract()) {
					List<Pair<String, TypeAndLocation>> methods = asNamed.getAllLocallyDefinedMethods(satc, true, false);
					
					if(methods.size() == 1) {
						Pair<String, TypeAndLocation> inst = methods.get(0);
						FuncType ft = (FuncType)inst.getB().getType();
						if(!ft.signatureExpectedToChange) {
							return new Pair<FuncType, Pair<NamedType, TypeAndLocation>>(ft, new Pair<NamedType, TypeAndLocation>(asNamed, inst.getB()));
						}
					}
				}
			}
		}

		return null;
	}
	
	private static void tagIfVarNumm(ScopeAndTypeChecker satc, Node rhsExpr, Type tagWith) {
		ErrorRaiseable ers = satc.getErrorRaiseableSupression();
		
		if(rhsExpr instanceof VarNull) {
			rhsExpr.setTaggedType(tagWith);
		}else if(rhsExpr instanceof LineHolder){
			tagIfVarNumm(satc, ((LineHolder) rhsExpr).l, tagWith);
		}else if(rhsExpr instanceof Block) {
			tagIfVarNumm(satc,  (Node)((Block) rhsExpr).getLastLogical(), tagWith);
		}else if(rhsExpr instanceof DuffAssign) {
			tagIfVarNumm(satc, (Node)((DuffAssign) rhsExpr).e, tagWith);
		}else if(rhsExpr instanceof IfStatement) {
			IfStatement asif = (IfStatement)rhsExpr;

			tagIfVarNumm(satc, asif.ifblock, tagWith);
			tagIfVarNumm(satc, asif.elseb, tagWith);
			for(ElifUnit eli : asif.elifunits) {
				tagIfVarNumm(satc, eli.elifb, tagWith);
			}
			
		}
		else if(rhsExpr instanceof TryCatch) {
			TryCatch asTryCatch = (TryCatch)rhsExpr;
			tagIfVarNumm(satc, asTryCatch.blockToTry, tagWith);
			tagIfVarNumm(satc, asTryCatch.finalBlock, tagWith);
			
			if(null != asTryCatch.cbs) {
				for(CatchBlocks cb : asTryCatch.cbs) {
					tagIfVarNumm(satc, cb.catchBlock, tagWith);
				}
			}
		}else if(rhsExpr instanceof ArrayDef) {
			ArrayDef asAD = (ArrayDef)rhsExpr;
			
			if(tagWith.hasArrayLevels()) {
				tagWith = (Type)tagWith.copy();
				tagWith.setArrayLevels(tagWith.getArrayLevels()-1);
			}else if(TypeCheckUtils.isList(ers, tagWith, false)) {
				tagWith = ((NamedType)tagWith).getGenericTypeElements().get(0);
			}else {
				return;
			}
			
			for(Expression expr : asAD.getArrayElements(satc)) {
				tagIfVarNumm(satc, (Node)expr, tagWith);
			}
			
			
		}else if(rhsExpr instanceof ArrayDefComplex) {
			ArrayDefComplex asAD = (ArrayDefComplex)rhsExpr;
			
			if(tagWith.hasArrayLevels()) {
				tagWith = (Type)tagWith.copy();
				tagWith.setArrayLevels(tagWith.getArrayLevels()-1);
				for(Expression expr : asAD.getArrayElements(satc)) {
					tagIfVarNumm(satc,  (Node)expr, tagWith);
				}
			}
		}
	}
	
	private static void inferTypeForNullsInTuple(ScopeAndTypeChecker satc, Node rhsExpr, NamedType tupleType) {
		ErrorRaiseable ers = satc.getErrorRaiseableSupression();
		
		if(rhsExpr instanceof TupleExpression) {
			TupleExpression asTuple = (TupleExpression)rhsExpr;
			List<Type> matchToTypes = tupleType.getGenTypes();
			int sz = matchToTypes.size();
			if(asTuple.tupleElements.size() == sz) {
				for(int n = 0; n < sz; n++) {
					Type expected = matchToTypes.get(n);
					//expected = boxTypeIfPrimative(expected, false, boolean boxvoid);
					Expression rhs = asTuple.tupleElements.get(n);
					tagIfVarNumm(satc, (Node)rhs, expected);
				}
			}
		}else if(rhsExpr instanceof LineHolder){
			inferTypeForNullsInTuple(satc, ((LineHolder) rhsExpr).l, tupleType);
		}else if(rhsExpr instanceof Block) {
			inferTypeForNullsInTuple(satc, ((Block) rhsExpr).getLastLogical(), tupleType);
		}else if(rhsExpr instanceof DuffAssign) {
			inferTypeForNullsInTuple(satc, (Node) ((DuffAssign) rhsExpr).e, tupleType);
		}else if(rhsExpr instanceof IfStatement) {
			IfStatement asif = (IfStatement)rhsExpr;

			inferTypeForNullsInTuple(satc, asif.ifblock, tupleType);
			inferTypeForNullsInTuple(satc, asif.elseb, tupleType);
			for(ElifUnit eli : asif.elifunits) {
				inferTypeForNullsInTuple(satc, eli.elifb, tupleType);
			}
			
		}
		else if(rhsExpr instanceof TryCatch) {
			TryCatch asTryCatch = (TryCatch)rhsExpr;
			inferTypeForNullsInTuple(satc, asTryCatch.blockToTry, tupleType);
			inferTypeForNullsInTuple(satc, asTryCatch.finalBlock, tupleType);
			
			if(null != asTryCatch.cbs) {
				for(CatchBlocks cb : asTryCatch.cbs) {
					inferTypeForNullsInTuple(satc, cb.catchBlock, tupleType);
				}
			}
		}else if(rhsExpr instanceof ArrayDef) {
			ArrayDef asAD = (ArrayDef)rhsExpr;
			
			if(tupleType.hasArrayLevels()) {
				tupleType = (NamedType)tupleType.copy();
				tupleType.setArrayLevels(tupleType.getArrayLevels()-1);
			}else if(TypeCheckUtils.isList(ers, tupleType, false)) {
				tupleType = (NamedType)((NamedType)tupleType).getGenericTypeElements().get(0);
			}else {
				return;
			}
			
			for(Expression expr : asAD.getArrayElements(satc)) {
				inferTypeForNullsInTuple(satc, (Node)expr, tupleType);
			}
			
			
		}else if(rhsExpr instanceof ArrayDefComplex) {
			ArrayDefComplex asAD = (ArrayDefComplex)rhsExpr;
			
			if(tupleType.hasArrayLevels()) {
				tupleType = (NamedType)tupleType.copy();
				tupleType.setArrayLevels(tupleType.getArrayLevels()-1);
				for(Expression expr : asAD.getArrayElements(satc)) {
					inferTypeForNullsInTuple(satc, (Node)expr, tupleType);
				}
			}
		}
	}

	public static LambdaDef inferAnonLambda(ScopeAndTypeChecker satc, Node rhsExpr, Type lhsType) {
		if(satc != null && rhsExpr != null && TypeCheckUtils.isValidType(lhsType)) {
			ErrorRaiseable ers = satc.getErrorRaiseableSupression();
			if(null != TypeCheckUtils.checkSubType(ers, ScopeAndTypeChecker.getLazyNT(), lhsType) ) {
				lhsType = ((NamedType)lhsType).getGenTypes().get(0);
			}
			
			NamedType tupleType = ScopeAndTypeChecker.getTupleNamedType(false);
			
			if(tupleType != null && null != TypeCheckUtils.checkSubType(ers, tupleType, lhsType)) {
				inferTypeForNullsInTuple(satc, rhsExpr, (NamedType)lhsType);
			}
			
			if(rhsExpr instanceof LambdaDef) {
				LambdaDef ld = (LambdaDef)rhsExpr;
				
				lhsType = (Type)lhsType.copy();
				lhsType.setArrayLevels(0);

				Pair<FuncType, Pair<NamedType, TypeAndLocation>> funcAndSAM = getFuncTypeForAnonLambdaAssign(satc, lhsType);
				if(null != funcAndSAM) {
					FuncType checkto = funcAndSAM.getA();
					
					if(null != checkto && ld.params.params.size() == checkto.inputs.size()) {
						
						int n=0;
						for(FuncParam input : ld.params.params) {
							Type argType = input.getTaggedType();
							Type expected = checkto.inputs.get(n);
							if(null != argType) {
								argType = (Type)argType.accept(satc);
								if(null == TypeCheckUtils.checkSubType(ers, TypeCheckUtils.boxTypeIfPrimative(expected, false), TypeCheckUtils.boxTypeIfPrimative(argType, false))) {
									return null;
								}
							}
						}
						
						Type retType = ld.returnType;
						if(retType != null) {
							if(null == TypeCheckUtils.checkSubType(ers, TypeCheckUtils.boxTypeIfPrimative(checkto.retType, false), TypeCheckUtils.boxTypeIfPrimative((Type)retType.accept(satc), false))) {
								return null;
							}
							//check vs defined
						}
						
						ld.implementSAM = funcAndSAM.getB();
					}
					return ld;
				}
				
				
				return null;
			}
			else if(rhsExpr instanceof AnonLambdaDef) {
				AnonLambdaDef asAnonLD = (AnonLambdaDef)rhsExpr;
				if(asAnonLD.isFullyTyped() /*|| asAnonLD.astRedirect != null*/) {//the anon lambda itself will resolve to a LambdaDef since all args qualiifed 
					return null;
				}
				
				Pair<FuncType, Pair<NamedType, TypeAndLocation>> funcAndSAM = getFuncTypeForAnonLambdaAssign(satc, lhsType);
				if(null == funcAndSAM) {
					return null;
				}
				FuncType inferTo = funcAndSAM.getA();
				
				ArrayList<Pair<String, Type>> inputs = asAnonLD.getInputs();
				int line = asAnonLD.getLine();
				int col = asAnonLD.getColumn();
				if(null != inferTo && inputs.size() == inferTo.inputs.size()) {
					//accept and check types
					FuncParams params = new FuncParams(line, col);
					
					int n=0;
					for(Pair<String, Type> input : inputs) {
						Type argType = input.getB();
						Type expected = inferTo.inputs.get(n);
						
						if(expected != null) {
							expected = (Type)expected.copy();
							expected.setInOutGenModifier(null);
						}
						
						if(null != argType) {
							argType = (Type)argType.accept(satc);
							if(null == TypeCheckUtils.checkSubType(ers, TypeCheckUtils.boxTypeIfPrimative(expected, false), TypeCheckUtils.boxTypeIfPrimative(argType, false))) {
								return null;
							}
							expected = argType;
						}
						
						params.add(new FuncParam(line, col, input.getA(), TypeCheckUtils.boxTypeIfPrimative(expected, false), false));
						n++;
					}
					
					Type retType = asAnonLD.getReturnType();
					if(retType != null) {
						if(null == TypeCheckUtils.checkSubType(ers, TypeCheckUtils.boxTypeIfPrimative(inferTo.retType, false), TypeCheckUtils.boxTypeIfPrimative((Type)retType.accept(satc), false))) {
							return null;
						}
						//check vs defined
					}/*else if(asAnonLD.astRedirect!= null) {
						retType = asAnonLD.astRedirect.returnType;
					}*/
					
					if(asAnonLD.astRedirect != null) {
						if(asAnonLD.astRedirect.params.equals(params)) {
							return asAnonLD.astRedirect;//same then we dont need to regerenate below...
						}
					}
					
					LambdaDef ld = new LambdaDef(line, col, null, params, asAnonLD.body, retType, new ArrayList<Pair<String, NamedType>>());
					ld.setShouldBePresevedOnStack(true);
					ld.implementSAM = funcAndSAM.getB();
					ld.omitAnonLambdaSources = true;
					//check gennerated lambda regen if inputs changed
					asAnonLD.astRedirect = ld;
					
					return asAnonLD.astRedirect;
				}
			}else if(rhsExpr instanceof LineHolder){
				return inferAnonLambda(satc, ((LineHolder) rhsExpr).l, lhsType);
			}else if(rhsExpr instanceof Block) {
				return inferAnonLambda(satc, ((Block) rhsExpr).getLastLogical(), lhsType);
			}else if(rhsExpr instanceof DuffAssign) {
				return inferAnonLambda(satc, (Node) ((DuffAssign) rhsExpr).e, lhsType);
			}else if(rhsExpr instanceof IfStatement) {
				IfStatement asif = (IfStatement)rhsExpr;

				ArrayList<LambdaDef> res = new ArrayList<LambdaDef>(2);
				res.add(inferAnonLambda(satc, asif.ifblock, lhsType));
				res.add(inferAnonLambda(satc, asif.elseb, lhsType));
				for(ElifUnit eli : asif.elifunits) {
					res.add(inferAnonLambda(satc, eli.elifb, lhsType));
				}
				
				return null;
			}
			else if(rhsExpr instanceof TryCatch) {
				TryCatch asTryCatch = (TryCatch)rhsExpr;
				ArrayList<LambdaDef> res = new ArrayList<LambdaDef>(2);
				res.add(inferAnonLambda(satc, asTryCatch.blockToTry, lhsType));
				res.add(inferAnonLambda(satc, asTryCatch.finalBlock, lhsType));
				
				if(null != asTryCatch.cbs) {
					for(CatchBlocks cb : asTryCatch.cbs) {
						res.add(inferAnonLambda(satc, cb.catchBlock, lhsType));
					}
				}
			}else if(rhsExpr instanceof ArrayDef) {
				ArrayDef asAD = (ArrayDef)rhsExpr;
				
				if(lhsType.hasArrayLevels()) {
					lhsType = (Type)lhsType.copy();
					lhsType.setArrayLevels(lhsType.getArrayLevels()-1);
				}else if(TypeCheckUtils.isList(ers, lhsType, false)) {
					lhsType = ((NamedType)lhsType).getGenericTypeElements().get(0);
				}else {
					return null;
				}
				
				ArrayList<LambdaDef> res = new ArrayList<LambdaDef>();
				for(Expression expr : asAD.getArrayElements(satc)) {
					res.add(inferAnonLambda(satc, (Node)expr, lhsType));
				}
				
				
			}else if(rhsExpr instanceof ArrayDefComplex) {
				ArrayDefComplex asAD = (ArrayDefComplex)rhsExpr;
				
				if(lhsType.hasArrayLevels()) {
					lhsType = (Type)lhsType.copy();
					lhsType.setArrayLevels(lhsType.getArrayLevels()-1);
					ArrayList<LambdaDef> res = new ArrayList<LambdaDef>();
					for(Expression expr : asAD.getArrayElements(satc)) {
						res.add(inferAnonLambda(satc, (Node)expr, lhsType));
					}
				}
			}
			
			//
			
		}
		return null;
	}
	

	public static Type convertToLazyType(Type lhsType) {
		lhsType = TypeCheckUtils.boxTypeIfPrimative(lhsType, false);
		NamedType asLazy = new NamedType(ScopeAndTypeChecker.getLazyCD());
		asLazy.setGenTypes(lhsType);
		return asLazy;
	}
	
	
	public static Expression createNewLazyObject(ScopeAndTypeChecker satc, int line, int col, Type lhsType, Type rhsType, Expression rhsExpr, boolean justLambda){
		Expression ret = null;
		
		if(null != satc) {
			Utils.inferAnonLambda(satc, (Node)rhsExpr, lhsType);
		}
		
		
		if(null != rhsExpr) {
			Block ldBlock = new Block(line, col);
			
			if(!lhsType.equals(rhsType)) {
				rhsExpr = new CastExpression(line, col, (Type)lhsType.copy(), rhsExpr);
			}
			
			ldBlock.add(new LineHolder(new DuffAssign(rhsExpr)));
			LambdaDef ld = new LambdaDef(line, col, null, new FuncParams(line, col), ldBlock, (Type)lhsType.copy(), new ArrayList<Pair<String, NamedType>>());
			ld.origSource = "lazy variable qualification";
			ret = ld;
		}
		
		if(!justLambda) {
			NamedType lazyWithGens = ScopeAndTypeChecker.getLazyNT().copyTypeSpecific();
			lazyWithGens.setGenTypes((Type)lhsType.copy());
			
			FuncInvokeArgs fia = ret == null?FuncInvokeArgs.manyargs(line, col):FuncInvokeArgs.manyargs(line, col, ret);
			
			ret = new New(line, col, lazyWithGens, fia, true);
		}
		
		return ret;
	}
	
	public static LambdaDef createNewNoArgLambda(int line, int col, Type toType, Type rhsType, Expression rhsExpr) {
		Block ldBlock = new Block(line, col);
		
		if(TypeCheckUtils.isVoid(toType)) {
			toType = ScopeAndTypeChecker.const_void;
		}else if(!toType.equals(rhsType)) {
			rhsExpr = new CastExpression(line, col, (Type)toType.copy(), rhsExpr);
		}
		
		ldBlock.add(new LineHolder(new DuffAssign(rhsExpr)));
		
		return new LambdaDef(line, col, null, new FuncParams(line, col), ldBlock, toType, new ArrayList<Pair<String, NamedType>>());
	}
	
	
	public static Expression createNewProvider(int line, int col, Type innerType, Expression innerExpr){
		Expression ret = null;
		
		Block ldBlock = new Block(line, col);
		
		ldBlock.add(new LineHolder(new DuffAssign(innerExpr)));
		LambdaDef ld = new LambdaDef(line, col, null, new FuncParams(line, col), ldBlock, (Type)innerType.copy(), new ArrayList<Pair<String, NamedType>>());
		ld.origSource = "lazy variable qualification";
		ret = ld;
		
		NamedType lazyWithGens = ScopeAndTypeChecker.getProviderNT().copyTypeSpecific();
		lazyWithGens.setGenTypes((Type)innerType.copy());
		
		FuncInvokeArgs fia = FuncInvokeArgs.manyargs(line, col, ret);
		
		ret = new New(line, col, lazyWithGens, fia, true);
		
		return ret;
	}
	
	public static Expression createNewOptional(int line, int col, Type innerType, Expression innerExpr){
		
		FuncInvoke eqTestFuncCall;
		if(innerExpr == null) {
			eqTestFuncCall = new FuncInvoke(line, col, "empty" );
		}else {
			eqTestFuncCall = new FuncInvoke(line, col, "ofNullable",  new CastExpression(line, col, (Type)innerType.copy(), innerExpr)   );
		}
		
		eqTestFuncCall.genTypes = new ArrayList<Type>();
		eqTestFuncCall.genTypes.add((Type)innerType.copy());
		
		ArrayList<Expression> doExpr = new ArrayList<Expression>();
		doExpr.add(new RefName(line, col, "java"));
		doExpr.add(new RefName(line, col, "util"));
		doExpr.add(new RefName(line, col, "Optional"));
		doExpr.add(eqTestFuncCall);
		
		return new DotOperator(line, col, doExpr); 
	}

	public static String stackTrackToString(Throwable e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
	
	public static Method getMethod(Class<?> cls, String name, int args){
		Method[] ethso = cls.getMethods();
		for(Method m : ethso){
			//System.err.println("" + m);
			if(m.getParameterTypes().length == args && m.toString().equals(name)) {
				return m;
			}
		}
		throw new RuntimeException("Cannot find method");
	}

	public static ArrayList<REPLTopLevelComponent> tagErrorChain(ArrayList<REPLTopLevelComponent> errorLocation) {
		if(errorLocation.isEmpty()) {
			return null;
		}
		
		for(REPLTopLevelComponent lastCtxt : errorLocation) {
			lastCtxt.setErrors(true);
		}
		return new ArrayList<REPLTopLevelComponent>(errorLocation);//make a copy as other things change this
	}
	
}

