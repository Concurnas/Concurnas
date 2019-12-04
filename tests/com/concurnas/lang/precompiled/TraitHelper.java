package com.concurnas.lang.precompiled;

import com.concurnas.lang.Trait;
import com.concurnas.lang.TraitField;

public class TraitHelper {
	public static interface MyTrait<T extends Number>{
		public default T converter(T t) { return t; }
	}
	
	public static interface MyTraitx{
		public default String thing(int x) { return "trait1 " + x; }
	}
	
	public static interface MyTrait2x{
		public default String thing(int x) { return "trait2 " + x; }
	}
	
	@Trait(nonTraitSuperclass = String.class, traitFields = { @TraitField(fieldName = "tfield", fieldType = "I", isAbstract = false, accessModifier = "public") })
	public static interface TraitWithField{
		
	}
	
	@Trait(traitFields = { @TraitField(fieldName = "thing", fieldType = "I", isAbstract = true, accessModifier = "public") })
	public static interface TraitWithFieldThing{
		
	}
	
	@Trait()
	public static interface TraitExtends extends TraitWithFieldThing{
		
	}
	
	@Trait(traitFields = { @TraitField(fieldName = "thing", fieldType = "TXYZ:Ljava/lang/Object<>;", isAbstract = false, accessModifier = "public") })
	public static interface GenericTrait<XYZ>{
		
	}
	
	@Trait()
	public static interface GenericTraitChild<FFF> extends GenericTrait<FFF>{
		
	}
	
}
