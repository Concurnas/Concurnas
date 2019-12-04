package com.concurnas.compiler.visitors;

import com.concurnas.compiler.visitors.PrintSourceVisitor;

public class PrintSourceTestVisitorIndent extends PrintSourceVisitor{

	protected void addItem(String s){
		addItem(s, false);
	}
	
	protected void addItem(String s, boolean indentInsidePasedString){
		
		String ident = new String(new char[super.indentLevel]).replace("\0", "  ");
		
		if(!super.items.isEmpty() && super.items.get(super.items.size()-1).endsWith("\n")){
			s = ident + s;
		}
		
		if(indentInsidePasedString) {
			s = s.replace("\n", "\n" + ident);
		}
		
		super.addItem(s);
	}
	
}
