package com.concurnas.conc;

import java.util.ArrayList;

public class ConcBuilder extends ConcBaseVisitor<Object>{
	public ConcInstance concInstance;
	@Override
	public Object visitConc(ConcParser.ConcContext ctx) {
		concInstance = new ConcInstance();
		
		for(ConcParser.OptionsContext option : ctx.options()) {
			option.accept(this);
		}
		
		if(ctx.source != null) {
			concInstance.sourceFile = (String)ctx.source.accept(this);
		}
		
		int al =  ctx.arg().size();
		if(al > 0){
			String[] cmdLineArgs = new String[al];
			for(int n = 0; n < al; n++) {
				cmdLineArgs[n] = (String)ctx.arg().get(n).accept(this);
			}
			concInstance.cmdLineArgs = cmdLineArgs;
		}
		
		return concInstance;
	}
	
	@Override
	public Object visitOptionHelpMe(ConcParser.OptionHelpMeContext ctx) {
		concInstance.helpMe = true;
		return null;
	}
	
	@Override
	public Object visitServerMode(ConcParser.ServerModeContext ctx) {
		concInstance.serverMode = true;
		return null;
	}

	@Override
	public Object visitClasspath(ConcParser.ClasspathContext ctx) {
		ArrayList<String> ret = new ArrayList<String>();
		ctx.fileOrDirNode().forEach(a -> ret.add((String)a.accept(this)));
		concInstance.classpath = ret;
		return null;
	}

	@Override
	public String visitFileOrDirNode(ConcParser.FileOrDirNodeContext ctx) {
		String ctext = ctx.getText();
		return ctext.trim();
	}
	
	@Override
	public Object visitOptionClasspath(ConcParser.OptionClasspathContext ctx) {
		ctx.classpath().accept(this);
		return null;
	}
	

	@Override
	public Object visitArg(ConcParser.ArgContext ctx) {
		if(ctx.FILEORDIR() != null) {
			return ctx.FILEORDIR().getText();
		}else if(ctx.SINGLED() != null){
			return ctx.SINGLED().getText();
		}else {
			return ctx.stringArg().accept(this);
		}
		
	}
	
	@Override
	public Object visitStringArg(ConcParser.StringArgContext ctx) {
		String ret = ctx.STR_ARG().getText();
		return ret.substring(1, ret.length()-1);
	}
	
	@Override
	public Object visitOptionWError(ConcParser.OptionWErrorContext ctx) {
		concInstance.werror = true;
		return null;
	}

	@Override
	public Object visitOptionByteCode(ConcParser.OptionByteCodeContext ctx) {
		concInstance.bytecode = true;
		return null;
	}
}