package com.concurnas.conc;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ConcSyntaxTests.class,
	ConcSemanticsTests.class,
	ConcExeTests.class})
public class AllConcTests {

}
