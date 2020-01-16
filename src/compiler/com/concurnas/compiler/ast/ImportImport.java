package com.concurnas.compiler.ast;

import java.util.ArrayList;

import com.concurnas.compiler.visitors.Visitor;

public class ImportImport extends ImportStatement  {
	public ArrayList<DottedAsName> imports = new ArrayList<DottedAsName>();
	
	public ImportImport(int line, int col, boolean isNormalImport) {//TODO: isNormalImport remove its unused
		super(line, col,isNormalImport);
	}
	
	public void add(DottedAsName imp) {
		this.imports.add(imp);
	}
	
	public void add(DottedAsName prim, DottedAsName latter) {
		String fromla = prim.origonalName.substring(0, prim.origonalName.lastIndexOf('.')) + "." + latter.origonalName;
		
		DottedAsName store = new DottedAsName(prim.getLine(), prim.getColumn(), fromla, latter.refName);
		
		this.imports.add(store);
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(this);
	}
	
	@Override
	public Node copyTypeSpecific() {
		return this;
	}
	
	@Override
	public ArrayList<String> getNames() {
		ArrayList<String> ret = new ArrayList<String>();
		
		for(DottedAsName entry : this.imports)
		{
			if(entry.singleEntry)
			{
				int lastDot = entry.refName.lastIndexOf('.');
				if( lastDot != -1)
				{
					ret.add(entry.refName.substring(lastDot+1));
					continue;
				}
			}
			
			ret.add(entry.refName);
		}
		
		return ret;
	}
}
