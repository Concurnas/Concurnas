package com.concurnas.concc;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ConccSyntaxTests.class, 
	ConccSemanticsTests.class, 
	ConccCompileTests.class})
public class AllConccTests {

}
