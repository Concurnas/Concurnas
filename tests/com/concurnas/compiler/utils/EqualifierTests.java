package com.concurnas.compiler.utils;

import junit.framework.TestCase;

import org.junit.Test;

import com.concurnas.lang.Equalifier;

public class EqualifierTests extends TestCase {

	@Test
	public void testEqauls() {
		int n=10000;
		int m = 0;
		
		while(m++ < n){
			Object iList1 = new Integer[]{1,2,3};
			Object iList2 = new Integer[]{1,2,3};
			Object iList3 = new Integer[]{1,2,3, 4};
			Object iList4 = new Integer[]{1,2,4};

			assertTrue( Equalifier.equals(iList1, iList1) );
			assertTrue( Equalifier.equals(iList1, iList2) );
			
			assertFalse( Equalifier.equals(iList1, iList3) );
			assertFalse( Equalifier.equals(iList1, iList4) );

			Object[] iList1AsObj = new Integer[]{1,2,3};
			Object[] iList2AsObj = new Integer[]{1,2,3};
			Object[] iList3AsObj = new Integer[]{1,2,3, 4};
			Object[] iList4AsObj = new Integer[]{1,2,4};
			
			assertTrue( Equalifier.equals(iList1AsObj, iList1AsObj) );
			assertTrue( Equalifier.equals(iList1AsObj, iList2AsObj) );
			assertFalse( Equalifier.equals(iList1AsObj, iList3AsObj) );
			assertFalse( Equalifier.equals(iList1AsObj, iList4AsObj) );
			
			
			Object iList1Prim = new int[]{1,2,3};
			Object iList2Prim = new int[]{1,2,3};
			Object iList3Prim = new int[]{1,2,3, 4};
			Object iList4Prim = new int[]{1,2,4};

			assertTrue( Equalifier.equals(iList1Prim, iList1Prim) );
			assertTrue( Equalifier.equals(iList1Prim, iList2Prim) );
			assertFalse( Equalifier.equals(iList1Prim, iList3Prim) );
			assertFalse( Equalifier.equals(iList1Prim, iList4Prim) );
			
			Object iList1PrimD = new double[]{1.,2.,3.};
			Object iList2PrimD = new double[]{1.,2.,3.};
			Object iList3PrimD = new double[]{1.,2.,3., 4.};
			Object iList4PrimD = new double[]{1.,2.,4.};

			assertTrue( Equalifier.equals(iList1PrimD, iList1PrimD) );
			assertTrue( Equalifier.equals(iList1PrimD, iList2PrimD) );
			assertFalse( Equalifier.equals(iList1PrimD, iList3PrimD) );
			assertFalse( Equalifier.equals(iList1PrimD, iList4PrimD) );
			
			//oddballs
			assertTrue( Equalifier.equals(iList1, iList1Prim) ); //prim vs obj
			assertTrue( Equalifier.equals(iList1, iList1AsObj) ); //prim vs obj
			assertTrue( Equalifier.equals(iList1Prim, iList1AsObj) ); //prim vs obj
			assertTrue( Equalifier.equals(iList1AsObj , iList1) ); //prim vs obj
			assertTrue( Equalifier.equals(iList1AsObj , iList1Prim) ); //prim vs obj
			
			//NOTE: this is considered to be non equals: [1.,2.,3.] != [1,2,3] 
			assertFalse( Equalifier.equals(iList1PrimD , iList1Prim) ); //diff prim vs obj
			
		}
	}
	

}
