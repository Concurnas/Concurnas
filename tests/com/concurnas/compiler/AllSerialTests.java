package com.concurnas.compiler;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.concurnas.conc.AllConcTests;
import com.concurnas.concAndConcc.ConccToConc;
import com.concurnas.concc.AllConccTests;
import com.concurnas.repl.REPLTests;

/*

-Xbootclasspath:./installed/rt.jar;./installed/resources.jar;./installed/plugin.jar;./installed/management-agent.jar;./installed/jsse.jar;./installed/jfxswt.jar;./installed/jfr.jar;./installed/jce.jar;./installed/javaws.jar;./installed/deploy.jar;./installed/charsets.jar;./installed/ext/access-bridge-64.jar;./installed/ext/cldrdata.jar;./installed/ext/dnsns.jar;./installed/ext/jaccess.jar;./installed/ext/jfxrt.jar;./installed/ext/localedata.jar;./installed/ext/nashorn.jar;./installed/ext/sunec.jar;./installed/ext/sunjce_provider.jar;./installed/ext/sunmscapi.jar;./installed/ext/sunpkcs11.jar;./installed/ext/zipfs.jar;./installed/security/local_policy.jar;./installed/security/US_export_policy.jar;./installed/conccore.jar

*/
@RunWith(Suite.class)
@SuiteClasses({ 
	AllConccTests.class,
	AllConcTests.class,
	REPLTests.class,
	ConccToConc.class
	//ConcInstallerTests.class - run seperatly ?
	})
public class AllSerialTests {
}
