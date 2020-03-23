package com.concurnas.compiler.visitors.algos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.antlr.v4.runtime.misc.OrderedHashSet;

import com.concurnas.compiler.ast.DotOperator;
import com.concurnas.compiler.ast.ExpressionList;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.FuncInvoke;
import com.concurnas.compiler.ast.FuncInvokeArgs;
import com.concurnas.compiler.ast.FuncParam;
import com.concurnas.compiler.ast.FuncRef;
import com.concurnas.compiler.ast.FuncRefArgs;
import com.concurnas.compiler.ast.FuncRefInvoke;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.GenericType;
import com.concurnas.compiler.ast.ModuleType;
import com.concurnas.compiler.ast.NamedConstructorRef;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.Node;
import com.concurnas.compiler.ast.NullStatus;
import com.concurnas.compiler.ast.RefName;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.ast.interfaces.Expression;
import com.concurnas.compiler.ast.util.ExpressionListOrigin;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.GenericTypeUtils;
import com.concurnas.compiler.visitors.ErrorRaiseable;
import com.concurnas.compiler.visitors.PrintSourceVisitor;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.compiler.visitors.Utils;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker.CapMaskedErrs;
import com.concurnas.runtime.Pair;

public class ExpressionListExpander {
	private ArrayList<Expression> exprs;
	//private ArrayList<String> exprsStrRep;
	private ExpressionList expressionList;
	
	
	public ExpressionListExpander(ExpressionList expressionList, ArrayList<Expression> exprs){
		this.expressionList= expressionList;
		this.exprs= exprs;
	}
	
	public Pair<Boolean, ArrayList<Expression>> getPossibilities(ScopeAndTypeChecker satc){
		//ArrayList<Expression> validPaths = new ArrayList<Expression>();
		//exprsStrRep = new ArrayList<String>();
		//do initial satc of elements
		for(Expression e : this.exprs){
			satc.maskErrors(false);
			e.accept(satc);
			satc.maskedErrors();
		}
		
		//generate all the possible paths
		PathGennerator gen = new PathGennerator(this.expressionList, this.exprs, satc, new DefaultFunctionProvider(satc));
		
		ArrayList<Expression> aths = gen.genneratePaths();
		
		boolean pass = true;
		if(aths.isEmpty()){
			pass = false;
			aths = gen.longestMatches;
		}
		
		//attempt disambiguate
		HashSet<String> matchAlready = new HashSet<String>();
		ArrayList<Expression> disamig = new ArrayList<Expression>();
		
		for(Expression path : aths){
			String rep = pprintExpr(path).replace("\\.", ".").replace("..", ".");
			if(!matchAlready.contains(rep)){
				disamig.add(path);
				matchAlready.add(rep);
			}
			
		}
		aths = disamig;
		
		return new Pair<>(pass, aths);
	}
	
	public static class PathGennerator{
		private List<Expression> exprs;
		private ExpressionList expressionList;
		private ScopeAndTypeChecker satc;
		private FunctionProvider funcProvider;
		
		private int longetsMatch=0;
		public ArrayList<Expression> longestMatches = new ArrayList<Expression>();
		private ErrorRaiseable supressException;

		public PathGennerator(ExpressionList expressionList, List<Expression> exprs, ScopeAndTypeChecker satc, FunctionProvider funcProvider){
			this.expressionList = expressionList;
			this.exprs = exprs;
			this.satc = satc;
			this.supressException = satc == null?null:satc.getErrorRaiseableSupression();
			this.funcProvider = funcProvider;
		}
		
		private ArrayList<Expression> rootResult;
		
		public ArrayList<Expression> genneratePaths(){
			ArrayList<Expression> possiblePaths = new ArrayList<Expression>();
			rootResult=possiblePaths;
			consume(exprs, 0, exprs.size()-1, null, possiblePaths);
			
			return possiblePaths;
		}
		
		private boolean testLastModuleType(Expression potentialPath) {
			if(potentialPath instanceof DotOperator) {
				DotOperator asDot = (DotOperator)potentialPath;
				ArrayList<Expression> elements = asDot.getElements(satc);
				return testLastModuleType(elements.get(elements.size()-1));
			}else if(potentialPath instanceof RefName) {
				RefName asRefName =(RefName)potentialPath;
				if(asRefName.resolvesTo != null && asRefName.resolvesTo.getLocation() == null) {//java.lang.System <-module, no location
					return false;
				}
			}
			
			return true;
		}
		
		
		private void addAndConsumeRest(ArrayList<Expression> potentialPaths, int idx, int cutSize, ArrayList<Expression> result, boolean forced){
			if(!potentialPaths.isEmpty()){
				for(Expression potentialPath : potentialPaths){
					if(satc != null){
						satc.maskErrors(true);
						Type got = (Type)potentialPath.accept(satc);
						//satc.maskedErrors();
						ArrayList<CapMaskedErrs>  what = satc.getmaskedErrors();
						//boolean errors = satc.maskedErrors();
						boolean errors = !what.isEmpty();
						if(errors) {//take a closer looks...
							if(got instanceof ModuleType && forced) {
								errors=false;
							}
							else {
								errors = testLastModuleType(potentialPath);
							}
						}
						
						if(errors){
							continue;
						}else if(rootResult == result){//tagg longest so far
							if(idx > longetsMatch){
								longestMatches = new ArrayList<Expression>();
								longetsMatch = idx;
							}
							
							if(idx >= longetsMatch){
								longestMatches.add(potentialPath);
							}
						}
						potentialPath.setTaggedType(got);
					}
					
					if(idx == cutSize){//all consumed, match for subsection found!
						result.add(potentialPath);
					}else{//consume rest...
						if(canMapToFuncRef(potentialPath)){//funcref on thing
							int remainingArgs = cutSize - idx;
							
							ArrayList<FuncDef> functionsAvailable = this.funcProvider.funcRefsAvailable(potentialPath, remainingArgs);
							
							int line = potentialPath.getLine();
							int col = potentialPath.getColumn();
							
							FuncArgType opType = FuncArgType.FUNCREFINVOKE;
							consumeAsFunctionLikeThing(line, col, functionsAvailable, potentialPath, remainingArgs, idx, cutSize, potentialPath,  result, opType);
						}
						
						consume(exprs, idx+1, cutSize, potentialPath, result);
					}
				}
			}
		}
		
		private void addViaDotOperator(int line, int col, ArrayList<Expression> potentialPaths, Expression pathSoFar, Expression baseItem, boolean normalDot, boolean directDot, boolean doubleDot){
			if(null == pathSoFar){
				potentialPaths.add((Expression)baseItem.copy());
			} else if(pathSoFar instanceof DotOperator){
				if(normalDot){//.
					DotOperator dopsofar = (DotOperator)pathSoFar.copy();
					dopsofar.addToTail((Expression)baseItem.copy());
					potentialPaths.add(dopsofar);
				}
				
				if(directDot){// \.
					DotOperator dopsofarx = (DotOperator)pathSoFar.copy();
					dopsofarx.addToTailDirect((Expression)baseItem.copy());
					potentialPaths.add(dopsofarx);
				}
				
				if(doubleDot){// \..
					DotOperator dopsofarx = (DotOperator)pathSoFar.copy();
					dopsofarx.addToTailReturn((Expression)baseItem.copy());
					potentialPaths.add(dopsofarx);
				}
				
			}else{
				if(normalDot){//.
					DotOperator dopsofar = DotOperator.buildDotOperatorOneNonDirect(line, col, (Expression)pathSoFar.copy(), (Expression)baseItem.copy());
					potentialPaths.add(dopsofar);
				}
				
				if(directDot){// \.
					DotOperator dopsofarx = DotOperator.buildDotOperatorOne(line, col, (Expression)pathSoFar.copy(), (Expression)baseItem.copy());
					potentialPaths.add(dopsofarx);
				}
				
				if(doubleDot){// \..
					DotOperator dopsofarx = DotOperator.buildDotOperatorOneReturn(line, col, (Expression)pathSoFar.copy(), (Expression)baseItem.copy());
					potentialPaths.add(dopsofarx);
				}
				
			}
		}
		
		private boolean canMapToFuncRef(Expression pathSoFar) {
			if (satc == null) {
				return true;
			}
			
			Type headType = TypeCheckUtils.getRefTypeToLocked(pathSoFar.getTaggedType());
			return headType instanceof FuncType;
		}
		
		private void consume(List<Expression> exprs, int idx, int cutSize, Expression pathSoFar, ArrayList<Expression> result){
			Expression baseItem = (Expression)exprs.get(idx).copy();
			int line = baseItem.getLine();
			int col = baseItem.getColumn();
						
			if(null != pathSoFar){
				if(!baseItem.isPermissableToGoOnRHSOfADot(null)){
					return;
				}
			}
			
			{
				{//direct as field
					boolean canAdd = true;
					boolean forced=false;
					if(baseItem instanceof RefName){
						canAdd = canHaveVariable(pathSoFar, ((RefName)baseItem).name);
						
						if(!canAdd) {//if not last one and next one is a RefName as well, check anyway (e.g. java lang System)
							if(exprs.size() > idx+1) {
								if(exprs.get(idx+1) instanceof RefName) {
									canAdd = true;
									forced = true;
								}
							}
						}
					}
					
					if(canAdd ){
						ArrayList<Expression> potentialPaths = new ArrayList<Expression>();
						addViaDotOperator(line, col, potentialPaths, pathSoFar, baseItem, true, true, false);
						addAndConsumeRest(potentialPaths, idx, cutSize, result, forced);
					}
					
				}
				
				if(baseItem != null){//as method call
					ArrayList<Pair<Expression, Expression>> variantsOfInput = new ArrayList<Pair<Expression, Expression>>();
					variantsOfInput.add(new Pair<Expression, Expression>(pathSoFar, baseItem));
					
					if(baseItem instanceof DotOperator && (pathSoFar == null || pathSoFar instanceof DotOperator)) {
						//cater for calls like this which are partially dotted System.err.println "ok"
						DotOperator fullDop = (DotOperator)baseItem.copy();
						ArrayList<Expression> elements = fullDop.getElements(satc);
						if(elements.size() > 1) {
							Expression lastOne = fullDop.removeLast();
							if(null != lastOne) {
								
								Expression pathsofaradd = elements.size()==1?elements.get(0): fullDop;
								
								if(pathSoFar == null) {
									variantsOfInput.add(new Pair<Expression, Expression>(pathsofaradd, lastOne));
								}else if(pathSoFar instanceof DotOperator) {
									DotOperator aggPath = (DotOperator)pathSoFar.copy();
									aggPath.add(pathsofaradd);
									variantsOfInput.add(new Pair<Expression, Expression>(aggPath, lastOne));
								}
								
							}
						}
					}
					
					for(Pair<Expression, Expression> variantAndPath : variantsOfInput) {
						Expression vPathSoFar = variantAndPath.getA(); 
						Expression variant = variantAndPath.getB();
						int remainingArgs = cutSize - idx;
						FuncArgType opType = categorizeThing(variant);
						
						ArrayList<FuncDef> functionsAvailable=null;
						if(variant instanceof RefName){
							functionsAvailable = this.funcProvider.functionsAvailable(vPathSoFar, ((RefName)variant).name, remainingArgs);
						}else if(variant instanceof FuncRef && ((FuncRef)variant).args == null ){
							functionsAvailable = this.funcProvider.functionsAvailable(vPathSoFar, ((FuncRef)variant).methodName, remainingArgs);
						}else if(variant instanceof NamedConstructorRef && ((NamedConstructorRef)variant).argz == null ){//construcotrs
							functionsAvailable=this.funcProvider.constructorsAvailable(((NamedConstructorRef)variant).namedConstructor, remainingArgs);
						}
						
						if(null != functionsAvailable){
							consumeAsFunctionLikeThing(line, col, functionsAvailable, variant, remainingArgs, idx, cutSize, vPathSoFar,  result, opType);
							
						}
					}
				}
			}
			return;
		}
		
		private void consumeAsFunctionLikeThing(int line, int col, ArrayList<FuncDef> functionsAvailable, Expression baseItem, int remainingArgs, int idx, int cutSize, Expression pathSoFar, ArrayList<Expression> result, FuncArgType opType){
			if(!functionsAvailable.isEmpty()){
				HashMap<Integer, OrderedHashSet<Pair<ArrayList<Type>, Type>>> argCountToFuncs = new HashMap<Integer, OrderedHashSet<Pair<ArrayList<Type>, Type>>>();
				
				for(FuncDef fd : functionsAvailable){
					if(fd == null){
						continue;
					}
					int fromArgSize= fd.params.params.size();
					int toSize = fromArgSize;
					boolean hasVararg=false;
					for(FuncParam fp : fd.params.params){
						if(fp.isVararg){
							hasVararg = true;
						}else if(fp.defaultValue != null) {
							fromArgSize--;//default values are optional
						}
					}
					if(hasVararg){//varargs mean we can cover a range of buckets with a single argument
						fromArgSize--;
						toSize = remainingArgs;
					}
					
					for(int n = fromArgSize; n <= toSize; n++){
						OrderedHashSet<Pair<ArrayList<Type>, Type>> bnucket = argCountToFuncs.get(n);
						if(null == bnucket){
							bnucket = new OrderedHashSet<Pair<ArrayList<Type>, Type>>();
							argCountToFuncs.put(n, bnucket);
						}
						
						ArrayList<Type> args = new ArrayList<Type>(fd.params.params.size());
						
						for(FuncParam fp : fd.params.params){
							
							if(fp.isVararg){
								int excessForVararg = n - fromArgSize;
								for(int m=0; m < excessForVararg; m++){
									args.add(fp.type);
								}
							}else{
								Type tt = ((Node)fp).getTaggedType();

								HashSet<GenericType> unbounded = TypeCheckUtils.findUnboundedGenerics(tt);
								if(!unbounded.isEmpty()) {
									tt = GenericTypeUtils.repointGenericTypesToUpperBound((Type)tt.copy());
								}
								args.add(tt);
							}
						}
						
						bnucket.add(new Pair<ArrayList<Type>, Type>(args, fd.retType));
					}
				}
				
				//try to fit remaining args into buckets provided
				for(int argsWanted : argCountToFuncs.keySet()){
					if(remainingArgs >= argsWanted){
						//see if we can splice them in
						if(argsWanted == 0){
							//funcCalls.add(new FuncInvoke(line, col, ((RefName)baseItem).name, new FuncInvokeArgs(line, col)));//zero arg call
							//funcCalls.add( addFuncArgsItem(line, col, baseItem, new FuncInvokeArgs(line, col), opType) );//zero arg call
							HashSet<Pair<ArrayList<Type>, Type>> funcs = argCountToFuncs.get(0);
							Type fretType = funcs.iterator().next().getB();
							addFuncCall(line, col, addFuncArgsItem(line, col, baseItem, new FuncInvokeArgs(line, col), opType, idx, idx + argsWanted), opType, pathSoFar, idx + argsWanted, cutSize, result, fretType);
						}else{
							HashSet<Pair<ArrayList<Type>, Type>> funcs = argCountToFuncs.get(argsWanted);
							
							ArrayList<ArrayList<Pair<Integer, Integer>>> options = ListSegemtizer.sublistCombs(cutSize - idx, argsWanted);
							for(ArrayList<Pair<Integer, Integer>> option : options){
								
								ArrayList<ArrayList<Expression>> argsx = new ArrayList<ArrayList<Expression>>();
								int tocap=0;
								for(Pair<Integer, Integer> item : option){
									ArrayList<Expression> argPossibilities = new ArrayList<Expression>();
									int fifx = idx + item.getA()+1;
									tocap = idx+ item.getB()+1;
									consume(exprs, fifx, tocap, null, argPossibilities);
									argsx.add(argPossibilities);
								}
								
								ArrayList<ArrayList<Expression>> argcombinations = ListSegemtizer.getCombinations(argsx);
								for(ArrayList<Expression> acombination : argcombinations){
									int argsize = acombination.size();
									
									for(Pair<ArrayList<Type>, Type> func : funcs){
										ArrayList<Type> expecteds = func.getA();

										boolean isMatch = true;
										for(int arg = 0; arg < argsize; arg++){
											Type got = acombination.get(arg).getTaggedType();
											Type expected = expecteds.get(arg);
											
											if(satc != null){
												if(got != null && expected != null){
													boolean argMatch=false;
													if(!got.isVectorized()){//arg is vectorized, see if we can decompose it to its componenents and if they will match

														if(null != TypeCheckUtils.checkSubType(supressException, ScopeAndTypeChecker.getLazyNT(), expected)) {
															if(null == TypeCheckUtils.checkSubType(supressException, expected, got)) {
																expected = ((NamedType)expected).getGenTypes().get(0);
															}
														}
														
														Type convertRetTo = TypeCheckUtils.canConvertRHSToArgLessLambda(satc, supressException, expected, got, true);
														if(null != convertRetTo) {
															got = expected;
														}
														
														argMatch = null != TypeCheckUtils.checkSubType(supressException, expected, got);
														
														if(!argMatch) {
															Pair<Boolean, Type> autoVectArg = TypeCheckUtils.autoVectorizeArg(true, got, expected);
															got = autoVectArg.getB();
														}
													}
													
													if(got.isVectorized()){//arg is vectorized, see if we can decompose it to its componenents and if they will match
														int esize = TypeCheckUtils.getVectorizedStructure(supressException, expected).size();
														ArrayList<Pair<Boolean, NullStatus>> mstruct = TypeCheckUtils.getVectorizedStructure(supressException, got);
														int msize = mstruct.size()-1;
														Type baseType = TypeCheckUtils.extractVectType(got);
														
														while(msize >= esize){
															Type tryMe = TypeCheckUtils.applyVectStruct(mstruct.subList(0, msize), baseType);
															argMatch = null != TypeCheckUtils.checkSubType(supressException, expected, tryMe);
															if(argMatch){
																break;
															}
															msize--;
														}
													}
													
													isMatch = argMatch;
													
												}else{
													isMatch=false;
												}
											}
											
											if(!isMatch){
												break;
											}
										}
										
										if(isMatch){
											FuncInvokeArgs fias = new FuncInvokeArgs(line, col);
											fias.addAll(acombination);
											//funcCalls.add(addFuncArgsItem(line, col, baseItem, fias, opType));//zero arg call
											
											Type fretType = func.getB();
											addFuncCall(line, col, addFuncArgsItem(line, col, baseItem, fias, opType, idx, tocap), opType, pathSoFar, tocap, cutSize, result, fretType);
											
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		
		private void addFuncCall(int line, int col, Expression funcCall, FuncArgType opType, Expression pathSoFar, int idx, int cutSize, ArrayList<Expression> result, Type fretType){
			ArrayList<Expression> potentialPaths = new ArrayList<Expression>();	
			if(opType == FuncArgType.FUNCREFINVOKE){
				potentialPaths.add(funcCall);
			}else{
				//dont check via dot if void, just goto double dot
				//dont access via double dot if last item
				addViaDotOperator(line, col, potentialPaths, pathSoFar, funcCall, true /*!TypeCheckUtils.isVoid(fretType)*/, false, idx != cutSize);
			}
			
			addAndConsumeRest(potentialPaths, idx, cutSize, result, false);
		}
		
		
		private enum FuncArgType{FUNCINVOKE, FUNCREF, NAMEDCON, FUNCREFINVOKE;}
		
		private static FuncArgType categorizeThing(Expression baseItem){
			if(baseItem == null){
				return FuncArgType.FUNCREFINVOKE;
			}
			
			if(baseItem instanceof RefName){
				return FuncArgType.FUNCINVOKE;
			}else if(baseItem instanceof FuncRef && ((FuncRef)baseItem).args == null ){
				return FuncArgType.FUNCREF;
			}else if(baseItem instanceof NamedConstructorRef && ((NamedConstructorRef)baseItem).argz == null ){
				return FuncArgType.NAMEDCON;
			}else{
				return FuncArgType.FUNCREFINVOKE;
			}
		}
		
		private Expression addFuncArgsItem(int line, int col, Expression baseItem, FuncInvokeArgs args, FuncArgType dowhat, int idx, int cutSize){
			if(dowhat == FuncArgType.FUNCINVOKE){
				RefName ba = ((RefName)baseItem);
				FuncInvoke ret = new FuncInvoke(line, col, ba.name, args);
				ret.expressionListOrigin = new ExpressionListOrigin(this.expressionList, idx, cutSize);
				ret.origName = ba;
				return ret;
			}else if(dowhat == FuncArgType.FUNCREF ){
				FuncRef cpy = (FuncRef)baseItem.copy();
				cpy.args = new FuncRefArgs((FuncInvokeArgs)args.copy());
				return cpy;
			}else if(dowhat == FuncArgType.NAMEDCON ){
				NamedConstructorRef cpy = (NamedConstructorRef)baseItem.copy();
				args = (FuncInvokeArgs)args.copy();
				cpy.argz = new FuncRefArgs(args);
				//cpy.namedConstructor.args = args;
				cpy.funcRef.args = cpy.argz;
				return cpy;
			}else{//dowhat == FuncArgType.FUNCREFINVOKE
				if(!args.asnames.isEmpty()){
					Expression firstOne = args.asnames.get(0);
					line = firstOne.getLine();
					col  = firstOne.getColumn();
				}
				
				return new FuncRefInvoke(line, col, baseItem, args);
			}
		}
		
		private boolean canHaveVariable(Expression onStackSoFar, String nameWanted){
			if(null == satc){
				return true;
			}
					
			if(onStackSoFar == null){
				return null != satc.currentScopeFrame.getVariable(null, nameWanted) || null != satc.getImportBeenRegistered(nameWanted);
			}else{//see if thing has variable
				Type got = onStackSoFar.getTaggedType();
				got = TypeCheckUtils.getRefTypeToLocked(got);
				
				if(got.hasArrayLevels() && nameWanted.equals("length")){
					return true;
				}
				
				if(got instanceof NamedType){
					return null != ((NamedType) got).getVariable(nameWanted, true);
				}
			}
			
			//length
			
			return false;
		}
		
	}
	
	

	public static interface FunctionProvider{
		public ArrayList<FuncDef> functionsAvailable(Expression onStackSoFar, String nameWanted, int remainingArgs);
		public ArrayList<FuncDef> funcRefsAvailable(Expression onStackSoFar, int remainingArgs);
		public ArrayList<FuncDef> constructorsAvailable(Expression onStackSoFar, int remainingArgs);
	}
	
	
	private static class DefaultFunctionProvider implements FunctionProvider{
		private ScopeAndTypeChecker satc;
		public DefaultFunctionProvider(ScopeAndTypeChecker satc){
			this.satc = satc;
		}
		
		public ArrayList<FuncDef> functionsAvailable(Expression onStackSoFar, String nameWanted, int remainingArgs){
			ArrayList<FuncDef> ret = new ArrayList<FuncDef>();
			
			HashSet<TypeAndLocation> res = new HashSet<TypeAndLocation>(); 
			
			if(null == onStackSoFar){
				HashSet<TypeAndLocation> items = this.satc.currentScopeFrame.getFuncDef(null, nameWanted);
				if(null != items && !items.isEmpty()){
					res.addAll(items);
				}else{//see if tfunction refers to a constructor
					NamedType nt = new NamedType(0,0,nameWanted);
					
					this.satc.maskErrors(false);
					nt.accept(this.satc);
					if(!this.satc.maskedErrors()){
						//consFromNamedType((NamedType)got);
						ret.addAll(consFromNamedType(nt));
					}else{
						//see if its a variable pointing to a namedtype having overriden the invoke method
						RefName asrn = new RefName(nameWanted);
						this.satc.maskErrors(false);
						Type got = (Type)asrn.accept(this.satc);
						if(!this.satc.maskedErrors() && got != null){
							got = TypeCheckUtils.getRefTypeToLocked(got);
							if(got instanceof NamedType){
								res.addAll(((NamedType)got).getFuncDef(0, 0, "invoke", null, null, null));
							}
						}
						else {
							//see if gloabl imported function
							
							
							
							if(satc.hasImportBeenRegistered(nameWanted))
							{//and finnaly search top level imports for the name
								String nameolaz = satc.getImportBeenRegistered(nameWanted);
								if(!nameolaz.equals(nameWanted)){
									String[] parts = nameolaz.split("\\.");
									//nameRedirect = parts[parts.length-1];
									nameWanted = nameolaz;
								}
							}
							HashSet<TypeAndLocation> gotz = satc.mainLoop.getFunctionFromPath(nameWanted, satc.mc, false);
							if(gotz != null) {
								res.addAll(gotz);
							}
							
						}
					}
				}
			}else{
				Type got = onStackSoFar.getTaggedType();
				got = TypeCheckUtils.getRefTypeToLocked(got);
				if(got instanceof NamedType){
					HashSet<TypeAndLocation> tals = ((NamedType) got).getFuncDef(0, 0, nameWanted, null, null, null);
					res.addAll(tals);
				}
				
				HashSet<TypeAndLocation> extf = this.satc.currentScopeFrame.getFuncDef(null, nameWanted);
				
				if(null == extf || extf.isEmpty()){
					
					//String nameRedirect = null;
					String namola = nameWanted;
					if(this.satc.hasImportBeenRegistered(nameWanted))
					{//and finnaly search top level imports for the name
						namola = this.satc.getImportBeenRegistered(nameWanted);
					}
					
					extf = this.satc.mainLoop.getFunctionFromPath(namola, this.satc.mc, false);
				}
				
				if(null != extf){
					for(TypeAndLocation ext : extf){
						Type gota = ext.getType();
						if(gota instanceof FuncType){
							FuncType ft = (FuncType)gota;
							if(ft.extFuncOn){
								Type head = ft.inputs.get(0);
								if(null != TypeCheckUtils.checkSubType(satc.getErrorRaiseableSupression(), head, got)){
									res.add(ext);
								}
							}
						}
					}
				}
				
			}
			
			for(TypeAndLocation tal : res){
				Type tt = tal.getType();
				if(tt instanceof FuncType){
					ret.add(((FuncType)tt).origonatingFuncDef);
				}
			}
			
			return ret;
		}

		@Override
		public ArrayList<FuncDef> funcRefsAvailable(Expression onStackSoFar, int remainingArgs) {
			ArrayList<FuncDef> functionsAvailable = new ArrayList<FuncDef>();
			FuncType mapFuncRef = (FuncType)onStackSoFar.getTaggedType();
			functionsAvailable.add(mapFuncRef.origonatingFuncDef);
			return functionsAvailable;
		}

		private ArrayList<FuncDef> consFromNamedType(NamedType nt){
			ArrayList<FuncDef> ret = new ArrayList<FuncDef>();
		
			for(FuncType ft : nt.getAllConstructors(satc)){
				FuncDef fd = ft.origonatingFuncDef;
				if(fd != null){
					fd = (FuncDef)fd.copy();
					fd.retType = nt;
				}
				ret.add(fd);
			}
			
			return ret;
		}
		
		@Override
		public ArrayList<FuncDef> constructorsAvailable(Expression onStackSoFar, int remainingArgs) {
			ArrayList<FuncDef> ret = new ArrayList<FuncDef>();
			
			if(null != onStackSoFar){
				Type got = onStackSoFar.getTaggedType();
				got = TypeCheckUtils.getRefTypeToLocked(got);
				if(got instanceof NamedType){
					ret = consFromNamedType((NamedType)got);
				}
			}
			

			return ret;
		}
	}
	
	private static String pprintExpr(Expression path){
		PrintSourceVisitor visitor = new PrintSourceVisitor();
		path.accept(visitor);
		return visitor.toString();
	}
}
