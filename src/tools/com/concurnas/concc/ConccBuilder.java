package com.concurnas.concc;

import java.util.ArrayList;

import com.concurnas.concc.ConccInstance.SourceToCompile;

public class ConccBuilder extends ConccBaseVisitor<Object> {

	public ConccInstance concInstance;
	
	@Override
	public Object visitConcc(ConccParser.ConccContext ctx) {
		
		concInstance = new ConccInstance();
		
		for(ConccParser.OptionsContext option : ctx.options()) {
			option.accept(this);
		}
		
		for(ConccParser.SourcesContext src : ctx.sources()) {
			src.accept(this);
			//concInstance.sources.add((SourceToCompile)src.accept(this));
		}
		
		return concInstance;
	}

	@Override
	public SourceToCompile visitSources(ConccParser.SourcesContext ctx) {
		String root = null;
		if(ctx.root != null) {
			root = (String)ctx.root.accept(this);
		}
		
		for(ConccParser.FileOrDirNodeContext fileOrDir : ctx.ford) {
			concInstance.sources.add(new SourceToCompile(root, (String)fileOrDir.accept(this)));
		}
		
		return null;
	}


	@Override
	public Object visitClasspath(ConccParser.ClasspathContext ctx) {
		ArrayList<String> ret = new ArrayList<String>();
		ctx.fileOrDirNode().forEach(a -> ret.add((String)a.accept(this)));
		concInstance.classpath = ret;
		return null;
	}

	@Override
	public String visitFileOrDirNode(ConccParser.FileOrDirNodeContext ctx) {
		String ctext = ctx.getText();
		//ctext = StringUtils.unescapeJavaString(ctext);
		return ctext.trim();
	}
	
	
	@Override
	public String visitOptionOutputDir(ConccParser.OptionOutputDirContext ctx) {
		concInstance.outputDirectory = (String) ctx.fileOrDirNode().accept(this);
		return null;
	}

	@Override
	public String visitOptionRoot(ConccParser.OptionRootContext ctx) {
		concInstance.globalRoot = (String) ctx.fileOrDirNode().accept(this);
		return null;
	}

	@Override
	public Object visitOptionClasspath(ConccParser.OptionClasspathContext ctx) {
		ctx.classpath().accept(this);
		return null;
	}

	@Override
	public Object visitOptionHelpMe(ConccParser.OptionHelpMeContext ctx) {
		concInstance.helpMe = true;
		return null;
	}


	@Override
	public Object visitOptionWError(ConccParser.OptionWErrorContext ctx) {
		concInstance.warnAsError = true;
		return null;
	}
	@Override
	public Object visitOptionVerbose(ConccParser.OptionVerboseContext ctx) {
		concInstance.verbose = true;
		return null;
	}

	@Override
	public Object visitAllCopy(ConccParser.AllCopyContext ctx) {
		concInstance.copyall = true;
		return null;
	}


	@Override
	public Object visitCreateJar(ConccParser.CreateJarContext ctx) {
		String jarname = (String)ctx.jarFile.accept(this);
		if(!jarname.endsWith(".jar")) {
			jarname += ".jar";
		}
		concInstance.outputJar = jarname;
		
		if(ctx.mfest != null) {
			concInstance.mfestEntryPoint = (String)ctx.mfest.accept(this);
		}
		
		return null;
	}
	

	@Override
	public Object visitOptionClean(ConccParser.OptionCleanContext ctx) {
		concInstance.doCleanup = true;
		return null;
	}

}
