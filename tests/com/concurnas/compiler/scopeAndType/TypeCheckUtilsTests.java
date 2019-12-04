package com.concurnas.compiler.scopeAndType;

import junit.framework.TestCase;
import com.concurnas.compiler.ast.AssignStyleEnum;
import com.concurnas.compiler.ast.PrimativeType;
import com.concurnas.compiler.ast.PrimativeTypeEnum;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.compiler.visitors.util.DummyErrorRaiseable;

public class TypeCheckUtilsTests extends TestCase{

	@Override
	public void setUp()
	{
		er = new DummyErrorRaiseable();
	}
	
	DummyErrorRaiseable er = null;
	
	private final static Type inta = new PrimativeType(PrimativeTypeEnum.INT);
	private final static Type doublea = new PrimativeType(PrimativeTypeEnum.DOUBLE);
	private final static Type floata = new PrimativeType(PrimativeTypeEnum.FLOAT);
	private final static Type longa = new PrimativeType(PrimativeTypeEnum.LONG);
	
	public void testCheckAssignPrimIntOK() {
		Type retType = TypeCheckUtils.checkAssignmentCanBeDone(er, AssignStyleEnum.EQUALS, inta, inta, 42, 42, 42, 42, "");
		assertEquals(inta, retType);
		assertEquals("", er.getErrors());
	}
	
	public void testCheckAssignPrimDbouleIntOK() {
		Type retType = TypeCheckUtils.checkAssignmentCanBeDone(er, AssignStyleEnum.EQUALS, doublea, floata, 42, 42, 42, 42, "");
		assertEquals(doublea, retType);
		assertEquals("", er.getErrors());
	}
	
	public void testCheckAssignPrimFloatToDoubleOk() {
		Type retType = TypeCheckUtils.checkAssignmentCanBeDone(er, AssignStyleEnum.EQUALS, doublea, inta, 42, 42, 42, 42, "");
		assertEquals(doublea, retType);
		assertEquals("", er.getErrors());
	}
	
	public void testCheckAssignPrimIntDbouleFailOK() {
		Type retType = TypeCheckUtils.checkAssignmentCanBeDone(er, AssignStyleEnum.EQUALS, inta, doublea, 42, 42, 42, 42, "");
		assertEquals(null, retType);
		assertEquals("Error on line: 42 col: 42. Error: Type mismatch: cannot convert from double to int;", er.getErrors());
	}
	
	public void testCheckUCannotSetALongToAnInt() {
		Type retType = TypeCheckUtils.checkAssignmentCanBeDone(er, AssignStyleEnum.EQUALS, inta, longa, 42, 42, 42, 42, "");
		assertEquals(null, retType);
		assertEquals("Error on line: 42 col: 42. Error: Type mismatch: cannot convert from long to int;", er.getErrors());
	}
	
	//return checkSubType(invoker, lhs, rhs, lhsLine, lhsColumn, rhsLine, rhsColumn);
	
	
	

}
