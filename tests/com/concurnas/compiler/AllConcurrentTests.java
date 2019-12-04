package com.concurnas.compiler;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.concurnas.compiler.ast.ASTConstructionTests;
import com.concurnas.compiler.bytecode.BytecodeTestJustCopier;
import com.concurnas.compiler.bytecode.BytecodeTests;
import com.concurnas.compiler.cps.CPSTests;
import com.concurnas.compiler.cps.CPSTestsCleanStack;
import com.concurnas.compiler.defoAssignment.DefoAssignmentTests;
import com.concurnas.compiler.fieldAccRepoint.FieldRepointTests;
import com.concurnas.compiler.misc.GenericRefCastTests;
import com.concurnas.compiler.misc.InstanceofGenericTests;
import com.concurnas.compiler.misc.LowestCommonAncestorTests;
import com.concurnas.compiler.misc.RefExtractTypeTests;
import com.concurnas.compiler.nestedFuncRepoint.NestedFuncRepointTests;
import com.concurnas.compiler.refConcurrentTests.RefConcurrentTests;
import com.concurnas.compiler.returnAnalysis.ReturnAnalysisTests;
import com.concurnas.compiler.scopeAndType.ScopeAndTypeTests;
import com.concurnas.compiler.scopeAndType.ScopeAndTypeTestsJustLambda;
import com.concurnas.compiler.scopeAndType.ScopeAndTypeTestsJustPlay;
import com.concurnas.compiler.scopeAndType.ScopeAndTypeTestsJustRef;
import com.concurnas.compiler.scopeAndType.TypeCheckUtilsTests;
import com.concurnas.compiler.syntax.SyntaxTests;
import com.concurnas.compiler.util.ConcurrentSuite;
import com.concurnas.compiler.util.ExprListExpanderTests;
import com.concurnas.compiler.utils.ArraysTest;
import com.concurnas.compiler.utils.EqualifierTests;
import com.concurnas.compiler.utils.TraitFieldEncoderDecoderTests;
import com.concurnas.runtime.cps.analysis.ANFTransformTests;
import com.concurnas.runtime.tests.TestTypeDescForRefGennerator;

/*

-Xbootclasspath:./installed/rt.jar;./installed/resources.jar;./installed/plugin.jar;./installed/management-agent.jar;./installed/jsse.jar;./installed/jfxswt.jar;./installed/jfr.jar;./installed/jce.jar;./installed/javaws.jar;./installed/deploy.jar;./installed/charsets.jar;./installed/ext/access-bridge-64.jar;./installed/ext/cldrdata.jar;./installed/ext/dnsns.jar;./installed/ext/jaccess.jar;./installed/ext/jfxrt.jar;./installed/ext/localedata.jar;./installed/ext/nashorn.jar;./installed/ext/sunec.jar;./installed/ext/sunjce_provider.jar;./installed/ext/sunmscapi.jar;./installed/ext/sunpkcs11.jar;./installed/ext/zipfs.jar;./installed/security/local_policy.jar;./installed/security/US_export_policy.jar;./installed/conccore.jar

*/
@RunWith(ConcurrentSuite.class)
//@RunWith(Suite.class)
@SuiteClasses({ 
	ScopeAndTypeTests.class,
	SyntaxTests.class, 
	ASTConstructionTests.class,
	//TestCompiledClassUtils.class,
	TypeCheckUtilsTests.class,
	LowestCommonAncestorTests.class,
	ScopeAndTypeTestsJustPlay.class,
	ScopeAndTypeTestsJustLambda.class,
	ReturnAnalysisTests.class,
	DefoAssignmentTests.class,
	BytecodeTests.class,
	FieldRepointTests.class,
	NestedFuncRepointTests.class,
	EqualifierTests.class,
	ScopeAndTypeTestsJustRef.class,
	InstanceofGenericTests.class,
	
	RefExtractTypeTests.class,
	GenericRefCastTests.class,
	ANFTransformTests.class,
	CPSTests.class,
	CPSTestsCleanStack.class,
	BytecodeTestJustCopier.class,
	RefConcurrentTests.class,
	TestChunking.class,
	ArraysTest.class,
	ExprListExpanderTests.class,
	TestTypeDescForRefGennerator.class,
	TraitFieldEncoderDecoderTests.class,
	})
public class AllConcurrentTests {
}
