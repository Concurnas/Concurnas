package com.concurnas.compiler.visitors.util;

import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.Type;

public class ActorInterfaceCreator {

	private static class ActorIfaceClassDef extends ClassDef{
		private NamedType of;
		
		public ActorIfaceClassDef(NamedType of) {
			super(0,0);
			this.of = of;
		}
		
		//add stuff as required
		
	}
	
	public static Type createInterfacefor(NamedType lhsType) {
		if(null == lhsType){
			return null;
		}
		
		ClassDef ifaceDef = new ActorIfaceClassDef(lhsType);
		
		return new NamedType(ifaceDef);
	}

}
