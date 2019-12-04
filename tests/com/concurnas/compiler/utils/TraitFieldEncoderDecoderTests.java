package com.concurnas.compiler.utils;

import java.util.HashMap;

import org.junit.Test;

import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.ast.GenericType;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.util.TraitFieldEncoderDecoder;

import junit.framework.TestCase;

public class TraitFieldEncoderDecoderTests extends TestCase {

	public static String encode(Type what) {
		return TraitFieldEncoderDecoder.encode(what);
	}
	public static Type decode(String what) {
		return TraitFieldEncoderDecoder.decode(what);
	}
	
	@Test
	public void testEncodeSimple() {
		assertEquals("I", encode(ScopeAndTypeChecker.const_int));
		
		Type primarrya = (Type)ScopeAndTypeChecker.const_int.copy();
		primarrya.setArrayLevels(2);
		assertEquals("[[I", encode(primarrya));
		
		NamedType number = new NamedType(0,0, new ClassDefJava(Number.class));

		assertEquals("Ljava/lang/Number<>;", encode(number));
		
		GenericType y = new GenericType(0,0,"Y",1);

		assertEquals("TY:Ljava/lang/Object<>;", encode(y));
		
		GenericType x = new GenericType(0,0,"XXX",1);
		x.upperBound = number;
		
		assertEquals("TXXX:Ljava/lang/Number<>;", encode(x));
		
		NamedType string = new NamedType(0,0, new ClassDefJava(String.class));
		
		NamedType hasmap = new NamedType(0,0, new ClassDefJava(HashMap.class));
		hasmap.setGenTypes(x, string);

		assertEquals("Ljava/util/HashMap<TXXX:Ljava/lang/Number<>;Ljava/lang/String<>;>;", encode(hasmap));
	}

	@Test
	public void testDecode() {
		String aprim = "I";
		String primar = "[[I";
		String number = "Ljava/lang/Number<>;";
		String geny = "TY:Ljava/lang/Object<>;";
		String genx = "TXXX:Ljava/lang/Number<>;";
		String hassmap = "Ljava/util/HashMap<TXXX:Ljava/lang/Number<>;Ljava/lang/String<>;>;";
		
		assertEquals("int", "" + decode(aprim));
		assertEquals("int[2]", "" + decode(primar));
		assertEquals("java/lang/Number", "" + decode(number));
		assertEquals("Y java/lang/Object", "" + decode(geny));
		assertEquals("XXX java/lang/Number", "" + decode(genx));
		assertEquals("java/util/HashMap<XXX java/lang/Number, java/lang/String>", "" + decode(hassmap));
	}

}
