package com.concurnas.compiler.visitors;

import java.util.ArrayList;

import com.concurnas.compiler.ast.AsyncBodyBlock;
import com.concurnas.compiler.ast.FuncDef;
import com.concurnas.compiler.ast.Line;
import com.concurnas.compiler.ast.LineHolder;
import com.concurnas.compiler.ast.OnChange;
import com.concurnas.compiler.ast.OnEvery;

public class OnChangeMethodCodeGenAsync {

	public static FuncDef makeOnChangeInitMethod(AsyncBodyBlock asyncBodyBlock, String patheNAme) {
		//call each init method for kids
		ArrayList<String> initMethods = new ArrayList<String>();
		
		for(LineHolder lh : asyncBodyBlock.mainBody.lines){
			Line l = lh.l;
			if(l instanceof OnChange){
				initMethods.add(((OnChange) l).initMethodName);
			}
		}
		
		String theFunc = "public def " + asyncBodyBlock.initMethodName + "(stateObject$ " + asyncBodyBlock.getFullnameSO() + ", earlyNotifiQ java.util.LinkedHashSet<com.concurnas.bootstrap.runtime.ref.Ref<Object>>) void {";
		
		for(String ini : initMethods){
			theFunc += String.format("\n %s(stateObject$, earlyNotifiQ);", ini);
		}
		
		theFunc += "\n return; }";
		
		FuncDef ret = Utils.parseFuncDef(theFunc, patheNAme, asyncBodyBlock.getLine());
		
		if(!asyncBodyBlock.preBlocks.isEmpty()){
			ret.funcblock.reallyPrepend(asyncBodyBlock.preBlocks.get(0).lines);
		}
		
		return ret;
	}

	public static FuncDef makeOnChangeApplyMethod(AsyncBodyBlock asyncBodyBlock, String patheNAme) {
		ArrayList<OnChange> onChanges = new ArrayList<OnChange>();
		for(LineHolder lh : asyncBodyBlock.mainBody.lines){
			Line l = lh.l;
			if(l instanceof OnChange){
				onChanges.add(((OnChange) l));
			}
		}
		
		String theFunc = "public def " + asyncBodyBlock.applyMethodName + "(stateObject$ " + asyncBodyBlock.getFullnameSO() + ", xxx com.concurnas.bootstrap.runtime.transactions.Transaction, isFirst boolean) boolean {";
		
		boolean hasEvery = asyncBodyBlock.hasEvery;
		
		if(onChanges.size() == 1){//little optimization, if just one we dont need to do it at all
			OnChange first = onChanges.get(0);
			if(!(first instanceof OnEvery) && hasEvery){
				theFunc += String.format("\n if(not(isFirst)){ %s(stateObject$, xxx, isFirst);}", first.applyMethodName);
			}
			else{
				theFunc += String.format("\n %s(stateObject$, xxx, isFirst);", first.applyMethodName);
			}
		}
		else{
			theFunc += "\n kids = stateObject$.$regSetParent.getMapping(xxx);";
			//theFunc += "System.err.println('kids count: ' + kids.size());";
			//theFunc += "System.err.println('isFirst: ' + isFirst);";
			//theFunc += "for(k in kids){ System.err.println('kis applies to ->' + k.order); } ;";
			theFunc += "\nfor(k in kids){ order = k.order";
			int n=0;
			for(OnChange apply : onChanges){
				//if apply call returns false then we halt execution and dont continue with other calls
				if(!(apply instanceof OnEvery) && hasEvery){
					theFunc += String.format("\n if (not(isFirst) ) { if( order == %s){ if(not(%s(stateObject$, xxx, isFirst))){ break; } } }", n++, apply.applyMethodName);
				}
				else{
					theFunc += String.format("\n if(order == %s){if(not(%s(stateObject$, xxx, isFirst))){ break; } }", n++, apply.applyMethodName);
				}
			}
			theFunc += "\n}";
		}
	
		
		theFunc += "\n return not(stateObject$.$regSetParent.hasRegistrations()); }";
		return Utils.parseFuncDef(theFunc, patheNAme, asyncBodyBlock.getLine());
	}

	public static FuncDef makeOnChangeCleanUpMethod(AsyncBodyBlock asyncBodyBlock, String patheNAme) {
		ArrayList<String> cleanMethods = new ArrayList<String>();
		for(LineHolder lh : asyncBodyBlock.mainBody.lines){
			Line l = lh.l;
			if(l instanceof OnChange){
				cleanMethods.add(((OnChange) l).cleanupMethodName);
			}
		}
		
		String theFunc = "public def " + asyncBodyBlock.cleanupMethodName + "(stateObject$ " + asyncBodyBlock.getFullnameSO() + ") void {";
		for(String ini : cleanMethods){
			theFunc += String.format("\n %s(stateObject$);", ini);
		}
		
		if(!asyncBodyBlock.noReturn){//returns something so close this
			theFunc += "\n ret$:close();";
		}
		
		theFunc += "\n stateObject$.$regSetParent.unregisterAll(); return; }";
		
		FuncDef toret = Utils.parseFuncDef(theFunc, patheNAme, asyncBodyBlock.getLine());
		
		//post block
		if(!asyncBodyBlock.postBlocks.isEmpty()){
			toret.funcblock.reallyPrepend(asyncBodyBlock.postBlocks.get(0).lines);
			//reallyPrependPenultimate
		}
		
		return toret;
	}

}
