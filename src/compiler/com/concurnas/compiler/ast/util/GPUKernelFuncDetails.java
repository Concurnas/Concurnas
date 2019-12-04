package com.concurnas.compiler.ast.util;

public class GPUKernelFuncDetails {
	public int dims = 0;
	public String name;
	public String signature;
	public String source;
	public GPUKernelFuncDetails[] dependancies;
	public String globalLocalConstant;//GC L etc
	public String inout;//"  I O" etc
	public String dclass;//optional, for use with dependancies
	
	public GPUKernelFuncDetails(int dims, String name, String signature, 
			String source, GPUKernelFuncDetails[] dependancies
			, String globalLocalConstant, String inout) {
		this.dims = dims;
		this.name = name;
		this.signature = signature;
		this.source = source;
		this.dependancies = dependancies;
		this.globalLocalConstant = globalLocalConstant;
		this.inout = inout;
	}
	
}
