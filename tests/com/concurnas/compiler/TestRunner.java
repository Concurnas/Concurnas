package com.concurnas.compiler;

import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.List;

import org.junit.internal.JUnitSystem;
import org.junit.internal.requests.ClassRequest;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import com.concurnas.compiler.bytecode.BytecodeTestJustCopier;


public class TestRunner {

	public static void main(String[] args) {
		JUnitCore core = new JUnitCore();
		TestListener listener = new TestListener();
        core.addListener(listener);

        System.out.println("Starting to run tests...");
        
        //core.run(new ClassRequest(AllTests.class));
        core.run(new ClassRequest(BytecodeTestJustCopier.class));
        
		System.exit(listener.failed);//0 is ok, >1 is fail
	}
	
	
	private static class TestListener extends RunListener {

	    private final PrintStream writer = System.out;

	    private volatile int started = 0;
	    private volatile int passed = 0;
	    public volatile int failed = 0;

	    @Override
	    public void testRunFinished(Result result) {
	        printHeader(result.getRunTime());
	        printFailures(result);
	        printFooter(result);
	    }

	    @Override
	    public void testStarted(Description description) {
    		started++;
	    	printStatus("");
	    }

	    @Override
	    public void testFailure(Failure failure) {
    		failed++;
	    	passed--;
	    	printStatus(String.format("Test Failed: %s\n", failure.getTestHeader() ));
	    }
	    
	    public void testFinished(Description description) throws Exception {
	    	passed++;
	    	printStatus("Last test completed: " + description.getDisplayName());
	    }
	    
	    private void printStatus(String lastComp) {
	    	//not threadsafe?
	    	int run = started - (passed+failed);
	    	
	    	writer.print(String.format("\rFail: %s, Pass: %s, Total: %s (Running: %s), %s", failed, passed, started, run, lastComp));
	    }

	    @Override
	    public void testIgnored(Description description) {
	    	
	    }

	    /*
	      * Internal methods
	      */


	    protected void printHeader(long runTime) {
	    	writer.println("\n");
	    	writer.println("Time: " + elapsedTimeAsString(runTime));
	    }

	    protected void printFailures(Result result) {
	        List<Failure> failures = result.getFailures();
	        if (failures.size() == 0) {
	            return;
	        }
	        if (failures.size() == 1) {
	        	writer.println("\nThere was " + failures.size() + " failure:");
	        } else {
	        	writer.println("\nThere were " + failures.size() + " failures:");
	        }
	        int i = 1;
	        for (Failure each : failures) {
	            printFailure(each, "" + i++);
	        }
	    }

	    protected void printFailure(Failure each, String prefix) {
	    	writer.println(prefix + ") " + each.getTestHeader());
	    }

	    protected void printFooter(Result result) {
	        if (result.wasSuccessful()) {
	        	writer.println("\n");
	        	writer.print("OK");
	        	writer.println(" (" + result.getRunCount() + " test" + (result.getRunCount() == 1 ? "" : "s") + ")");

	        } else {
	        	writer.println();
	        	writer.println("\nFAILURES!!!");
	        	writer.println("Tests run: " + result.getRunCount() + ",  Failures: " + result.getFailureCount());
	        }
	        writer.println();
	    }

	    /**
	     * Returns the formatted string of the elapsed time. Duplicated from
	     * BaseTestRunner. Fix it.
	     */
	    protected String elapsedTimeAsString(long runTime) {
	        return NumberFormat.getInstance().format((double) runTime / 1000);
	    }
	}	
}
