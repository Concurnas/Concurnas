package com.concurnas.lang;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.concurnas.bootstrap.runtime.InitUncreatable;
import com.concurnas.bootstrap.runtime.cps.CObject;
import com.concurnas.compiler.ast.AccessModifier;
import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.ConstructorDef;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.FuncParam;
import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.typeAndLocation.TypeAndLocation;
import com.concurnas.compiler.utils.Sevenple;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.datastructs.TheScopeFrame;
import com.concurnas.lang.NoNull.When;
import com.concurnas.runtime.Pair;

public class LangExt {

	public static enum SourceLocation {
		CLASS, EXPRESSION, TOPLEVEL;
	}

	public static interface Location{}
	
	public static class Variable extends CObject{
		private String type;
		public Variable(String type) {
			this.type = type;
		}
		
		public String getType() {
			return type;
		}
	}
	
	public static class Field extends CObject{
		private String name;
		private String type;

		public Field(String name, String type) {
			this.name = name;
			this.type = type;
		}

		public String getName() {
			return name;
		}

		public String getType() {
			return type;
		}
	}
	

	public static class Constructor extends CObject implements Location{
		private ArrayList<String> arguments;
		public Constructor(ArrayList<String> arguments) {
			this.arguments = arguments;
		}
		
		public ArrayList<String> getArguments() {
			return arguments;
		}
	}
	
	
	public static class Method extends CObject implements Location{
		private String name;
		private String returnType;
		private String extensionFuncOf;
		private ArrayList<String> arguments;
		public Method(String name, String returnType, ArrayList<String> arguments, String extensionFuncOf) {
			this.name = name;
			this.returnType = returnType;
			this.arguments = arguments;
			this.extensionFuncOf = extensionFuncOf;
		}
		
		public String getReturnType() {
			return returnType;
		}
		
		public String getName() {
			return name;
		}
		
		public String getExtentionFunctionOf() {
			return extensionFuncOf;
		}
		
		public ArrayList<String> getArguments() {
			return arguments;
		}
	}
	
	public static class Function extends Method {
		public Function(String name, String returnType, ArrayList<String> arguments, String extensionFuncOf) {
			super(name, returnType, arguments, extensionFuncOf);
		}
	}
		
	
	
	public static class Class extends CObject implements Location{
		private final ClassDef cd;
		
		public Class(ClassDef cd) {
			this.cd = cd;
		}

		public String getName() {
			return cd.className;
		}
		
		public List<Field> getFields() {
			ArrayList<Sevenple<String, Type, Boolean, AccessModifier, ClassDef, Boolean, String>> vars = cd.getAllFields();
			return vars.stream().map(a -> new Field(a.getA(), a.getB()==null?"":a.getB().getBytecodeType())).collect(Collectors.toList());
		}
		
		public List<Method> getMethods() {
			List<Method> ret = new ArrayList<Method>();
			
			for( Pair<String, TypeAndLocation> item : cd.getAllMethods(false)) {
				String name = item.getA();
				TypeAndLocation tal = item.getB();
				if(tal != null) {
					Type tt = tal.getType();
					if(tt != null && tt instanceof FuncType) {
						FuncType asFT = (FuncType)tt;
						String rett = "V";
						if(asFT.realReturnType != null) {
							rett = asFT.realReturnType.getBytecodeType();
						}
						
						ArrayList<String> arguments = new ArrayList<String>();
						for(Type inp : asFT.getInputs()) {
							arguments.add(inp.getBytecodeType());
						}
						
						String extFunc = null;
						if(asFT.origonatingFuncDef != null) {
							FuncDef origin = asFT.origonatingFuncDef;
							
							if( origin.extFunOn != null) {
								extFunc = origin.extFunOn.getBytecodeType();
							}
						}
						
						ret.add( new Method(name, rett, arguments, extFunc));
					}
				}
			}
			
			return ret;
		}
		
		public List<Constructor> getConstructors() {
			List<Constructor> ret = new ArrayList<Constructor>();
			
			for( FuncType asFT : cd.getAllConstructors()) {
				ArrayList<String> arguments = new ArrayList<String>();
				for(Type inp : asFT.getInputs()) {
					arguments.add(inp.getBytecodeType());
				}
				
				ret.add( new Constructor(arguments));
			}
			
			return ret;
		}
		
		public List<Class> getNestedClasses() {
			return cd.getAllNestedClasses().stream().map(a -> new Class(a)).collect(Collectors.toList());
		}
	}
	
	public static class Context extends CObject{
		private ScopeAndTypeChecker satc;

		public Context(ScopeAndTypeChecker satc) {
			this.satc = satc;
		}
		
		@NoNull(when = When.NEVER )
		public Variable getVariable(String name) {
			TypeAndLocation  tal = this.satc.currentScopeFrame.getVariable(null, name);
			
			if(tal == null) {
				return null;
			}else {
				String type = null;
				
				Type tt = tal.getType();
				if(tt != null) {
					type = tt.getBytecodeType();
				}
				
				return new Variable(type);
			}
		}
		
		@NoNull(when = When.NEVER )
		public Class getclass(String name) {
			NamedType nt = new NamedType(0,0,name);
			
			satc.maskErrors(false);
			Type resolves = (Type)satc.visit(nt);
			if(!satc.maskedErrors() && null != resolves && resolves instanceof NamedType) {
				NamedType asNamed = (NamedType)resolves;
				ClassDef setCls = asNamed.getSetClassDef();
				if(null != setCls) {
					return new Class(setCls);
				}
			}
			
			return null;
		}
		
		@NoNull()
		public List<Location> getNesting(){
			ArrayList<Location> ret = new ArrayList<Location>();
			
			TheScopeFrame tsf = satc.currentScopeFrame;
			while(tsf != null) {
				if(tsf.isClass()) {
					ret.add(new Class(tsf.getClassDef()));
				}else if(tsf.isFuncDefBlock) {
					String name = tsf.funcDef.getMethodNameIgnoreNIF();
					Type rt = tsf.funcDef.getReturnType();
					String returnType = "V";
					if(null != rt) {
						returnType = rt.getBytecodeType();
					}
					ArrayList<String> arguments = new ArrayList<String>();
					
					if(null != tsf.funcDef.params) {
						if(null != tsf.funcDef.params.params) {
							for(FuncParam fp : tsf.funcDef.params.params) {
								String arg = "";
								Type at = fp.getTaggedType();
								if(at != null) {
									arg = at.getBytecodeType();
								}
								arguments.add(arg);
							}
						}
					}

					if(tsf.funcDef instanceof ConstructorDef) {
						ret.add(new Constructor(arguments));
					}else {
						String extFunc = tsf.funcDef.extFunOn != null?tsf.funcDef.extFunOn.getBytecodeType():null;
						
						if(tsf.funcDef.definedAtClassLevel || tsf.funcDef.definedAtLocalClassLevel) {
							ret.add(new Method(name, returnType, arguments, extFunc));
						}else {
							ret.add(new Function(name, returnType, arguments, extFunc));
						}
					}
				}
				
				tsf = tsf.getParent();
			}
			return ret;
		}
		
		@NoNull()
		public List<Method> getMethods(String name){
			ArrayList<Method> ret = new ArrayList<Method>();
			
			
			HashSet<TypeAndLocation> choices = satc.currentScopeFrame.getFuncDef(null, name);
			
			if(choices != null) {
				for(TypeAndLocation choice : choices) {
					Type tt = choice.getType();
					if(tt instanceof FuncType) {
						FuncType asft = (FuncType)tt;
						String retType = "V";
						if(null != asft.realReturnType) {
							retType = asft.realReturnType.getBytecodeType();
						}
						
						ArrayList<String> arguments = new ArrayList<String>();
						for(Type ar : asft.getInputs()) {
							String argType = "V";
							if(ar != null) {
								argType = ar.getBytecodeType();
							}
							arguments.add(argType);
						}
						
						String extFunc = null;
						if(asft.origonatingFuncDef != null) {
							FuncDef origin = asft.origonatingFuncDef;
							
							if( origin.extFunOn != null) {
								extFunc = origin.extFunOn.getBytecodeType();
							}
							
							if(origin.definedAtClassLevel || origin.definedAtLocalClassLevel) {
								ret.add(new Method(name, retType, arguments, extFunc));
							}else {
								ret.add(new Function(name, retType, arguments, extFunc));
							}
						}else {
							ret.add(new Method(name, retType, arguments, extFunc));
						}
					}
				}
				
			}
			
			return ret;
		}
	}
	
	public static class ErrorOrWarning extends CObject{
		private String text;
		private int col;
		private int line;


		public ErrorOrWarning(int line, int col, String text) {
			this.line = line;
			this.col = col;
			this.text = text;
		}
		
		public ErrorOrWarning(InitUncreatable ignore) { }
		
		public void init(int line, int col, String text, InitUncreatable ignore) {
			this.line = line;
			this.col = col;
			this.text = text;
		}

		public String getText() {
			return text;
		}

		public int getCol() {
			return col;
		}

		public int getLine() {
			return line;
		}
	}
	
	public static class Result extends CObject{
		private List<ErrorOrWarning> errors;
		private List<ErrorOrWarning> warnings;
		
		public Result(List<ErrorOrWarning> errors, List<ErrorOrWarning> warnings) {
			this.errors = errors;
			this.warnings = warnings;
		}
		
		public Result(InitUncreatable ignore) { }

		public void init(List<ErrorOrWarning> errors, List<ErrorOrWarning> warnings, InitUncreatable ignore) {
			this.errors = errors;
			this.warnings = warnings;
		}

		public List<ErrorOrWarning> getWarnings() {
			return warnings;
		}

		public List<ErrorOrWarning> getErrors() {
			return errors;
		}	
	}
	
	public static class IterationResult extends Result{
		private String outputCode;

		public IterationResult(List<ErrorOrWarning> errors, List<ErrorOrWarning> warnings, String outputCode) {
			super(errors, warnings);
			this.outputCode = outputCode;
		}
		
		
		public IterationResult(InitUncreatable ignore){ super(ignore); }
		public void init(List<ErrorOrWarning> errors, List<ErrorOrWarning> warnings, String outputCode, InitUncreatable ignore) {
			super.init(errors, warnings, ignore);
			this.outputCode = outputCode;
		}

		public String getOutputCode() {
			return outputCode;
		}
	}

	public static interface LanguageExtension {
		public Result initialize(int line, int col, SourceLocation location, String source);

		public IterationResult iterate(Context ctx);
	}

}
