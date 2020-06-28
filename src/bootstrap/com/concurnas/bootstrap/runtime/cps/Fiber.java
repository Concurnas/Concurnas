package com.concurnas.bootstrap.runtime.cps;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.Stack;

import com.concurnas.bootstrap.lang.TypedActorInterface;
import com.concurnas.bootstrap.runtime.ref.Ref;
import com.concurnas.bootstrap.runtime.transactions.Transaction;
import com.concurnas.bootstrap.runtime.transactions.TransactionHandler;

public final class Fiber {
	public State curState;
	public int pc;

	private State[] stateStack = new State[10];// One State object for each activation frame in the call hierarchy.
	public int iStack = -1;// Index into stateStack and equal to depth of call hierarchy - 1

	boolean isPausing;
	boolean isDone;

	//public boolean debug = true;
	
	public volatile Iso iso;// The task to which this Fiber belongs

	private Stack<SyncTracker> syncTrackers = new Stack<SyncTracker>();

	private TypedActorInterface<?> primeActor;

	public LinkedList<TransactionHandler> transactions = new LinkedList<TransactionHandler>();
	private static final State PAUSE_STATE = new State();// Special marker state used by pause
	public static final int NOT_PAUSING__NO_STATE = 0;// normal return, nothing to restore
	public static final int NOT_PAUSING__HAS_STATE = 1;// Normal return, have saved state to restore before resuming
	public static final int PAUSING__NO_STATE = 2;// Pausing, and need to save state before returning
	public static final int PAUSING__HAS_STATE = 3;// Pausing, and have saved state from an earlier invocation, so nothing left to do.


	public void reset() {
		curState=null;
		primeActor=null;
		pc = 0;
		iStack = -1;
		stateStack = new State[10];
		isPausing=false;
		isDone=false;
		syncTrackers = new Stack<SyncTracker>();
		transactions = new LinkedList<TransactionHandler>();
	}
	
/*	public ConcClassloader getClassLoader(){
		System.err.println("what?");
		return (ConcClassloader)Fiber.class.getClassLoader();
	}
	*/
	public boolean inTransaction(){
		return !transactions.isEmpty();
	}
	
	public void enterTransaction(TransactionHandler h){
		transactions.add(h);
	}
	
	public void endTransaction(TransactionHandler h){
		transactions.removeLast();
	}
	
	//sync blocks
	
	public void enterSync(){
		syncTrackers.push(new SyncTracker());
	}
	
	public void exitsync() throws Throwable{
		syncTrackers.pop().awaitAll();
	}
	
	public SyncTracker getCurrentSyncTracker(){
		return syncTrackers.isEmpty()?null:syncTrackers.peek();
	}
	
	///
	
	
	public static ThreadLocal<Fiber> currentFiber = new ThreadLocal<Fiber>();
	/*set this in instances where one needs to obtain a reference to the current fiber but cannot because executing a 
	 * synchronized block (only valid case is on iso initialization where globals are being copied)
	 * e.g. specifically during global initalization
	 * */
	 
	
	static {
		PAUSE_STATE.pc = 1;
	}

	public Fiber(Iso t) {
		iso = t;
	}

	public boolean isDone() {
		return isDone;
	}

	public static void pause() {
		//throw new IllegalStateException("Fiber method: 'pause' must be invoked via concurnas, missing fiber information, @Uninterruptible cannot be used");
	}

	public static void pause(Fiber f) {
		f.togglePause();
	}

	public static Scheduler getScheduler() {
		throw new RuntimeException(
				"Fiber method: 'getScheduler' must be invoked via concurnas, missing fiber information, @Uninterruptible cannot be used");
	}

	public static Scheduler getScheduler(Fiber f) {
		return f.iso.worker.scheduler;
	}

	public static Scheduler setPrimeActor(TypedActorInterface<?> act) {throw new RuntimeException("Fiber method: 'setPrimeActor' must be invoked via concurnas, missing fiber information, @Uninterruptible cannot be used"); }
	public static void setPrimeActor(TypedActorInterface<?> act, Fiber f) {
		//prime actor is that for which the fiber was spawned
		f.primeActor = act;
	}
	
	public static boolean checkPrimeActor(TypedActorInterface<?> act) {throw new RuntimeException("Fiber method: 'checkPrimeActor' must be invoked via concurnas, missing fiber information, @Uninterruptible cannot be used"); }
	public static boolean checkPrimeActor(TypedActorInterface<?> act, Fiber f) {
		//check passed actor to see if it was the one for which we spanwed the fiber for
		//if it is then you know that you dont need to call the actor manager to call the thing
		//and that you can call directly
		return f.primeActor == act;
	}
	
	
	public static Iso getIso() {
		throw new RuntimeException(
				"Fiber method: 'getIso' must be invoked via concurnas, missing fiber information, @Uninterruptible cannot be used");
	}

	public static Iso getIso(Fiber f) {
		return f.iso;
	}

	
	public static Fiber getCurrentFiberWithCreate() {
		Fiber loc = Fiber.currentFiber.get();
		
		if(loc == null) {
			loc = new Fiber(null);
			Fiber.currentFiber.set(loc);
		}
		
		return loc;
	}

	public static Fiber getCurrentFiberWithCreate(Fiber f) {
		return f;// TODO: this is a bit silly, we're returning the current fiber
					// - placeholder should be replaced in code
	}
	
	public static Fiber getCurrentFiber() {
		return Fiber.currentFiber.get();
	}

	public static Fiber getCurrentFiber(Fiber f) {
		return f;// TODO: this is a bit silly, we're returning the current fiber
					// - placeholder should be replaced in code
	}

	//public LinkedList<Ref<?>> woeknUpOn = new LinkedList<Ref<?>>();

	// public static ThreadLocal<Fiber> currentFiber = new
	// ThreadLocal<Fiber>();//TODO: this is used by toString etc to obtain fiber
	// indirectly. This shouldn't be directly accessable

	public static void wakeup(Fiber f, Ref<?> loc) {
		//throw new RuntimeException("Fiber method: 'wakeup' must be invoked via concurnas");
		wakeup(f, loc, null);
	}

	public static void wakeup(Fiber f, Ref<?> loc, Fiber fu) {
		//f.woeknUpOn.add(loc);
		Iso iso = f.iso;
		if(null != iso) {
			iso.worker.wakeup(f.iso);//find worker which last worked on iso, and wake it up with a reference to itself
		}
	}
	
	public static void notif(Fiber f, Transaction trans, boolean isFirst) {
		notif(f, trans, isFirst, getCurrentFiberWithCreate());
		//throw new RuntimeException("Fiber method: 'notif' must be invoked via concurnas, missing fiber information, @Uninterruptible cannot be used");
	}
	
	
	//public LinkedBlockingQueue<Ref<?>> notificationQueue = new LinkedBlockingQueue<Ref<?>>();
	
	public static void notif(Fiber f, Transaction trans, boolean isFirst, Fiber fu) {
		//System.err.println("This Fiber: " + f +" has been notified of change in loc: " + System.identityHashCode(loc));
		//f.notificationQueue.add(loc);
		f.iso.worker.notif(f.iso, trans, isFirst);
	}
	
	public int up() {
		int d = iStack;
		iStack = --d;
		
		if (isPausing) {
			return (stateStack[d] == null) ? PAUSING__NO_STATE : PAUSING__HAS_STATE;
			// not setting curState because the generated code is only
			// interested in knowing whether we have state or not.
		} else {
			// NO PAUSE:
			//move to caller
			State[] stack = stateStack;
			
			if(d == -1) {//erroneous state
				pc = 0;
				return NOT_PAUSING__NO_STATE;
			}
			
			State cs = curState = stack[d];
			if (cs == null) {
				pc = 0;
				return NOT_PAUSING__NO_STATE;
			} else {
				stack[d] = null; 
				pc = cs.pc;
				return NOT_PAUSING__HAS_STATE;
			}
		}
	}
	
	public final Fiber begin() {
		return down();
	}

	/**
	 * end() is the last up(). returns true if the fiber is not pausing.
	 */
	public final boolean end() {
		assert iStack == 0 : "Reset: Expected iStack == 0, not " + iStack
				+ "\n" + this;
		boolean isDone = !isPausing;

		if (isDone) {
			// clean up callee's state
			stateStack[0] = null;
		}
		// reset pausing for next round.
		isPausing = false;
		iStack = -1;
		//stateStack = new State[10];
		// if (debug) System.err.println("lastUp() " + this);
		// if (debug) ds();
		return isDone;
	}

	public Fiber down() {
		int d = ++iStack;
		if (d >= stateStack.length) {
			// System.out.println("size == " + d);
			ensureSize(d * 2);
			pc = 0;
			curState = null;
		} else {
			State s = stateStack[d];
			curState = s;
			pc = (s == null) ? 0 : s.pc;
		}
		// if (debug) ds();
		return this;
	}

	static void ds() {
		for (StackTraceElement ste : new Exception().getStackTrace()) {
			String cl = ste.getClassName();
			String meth = ste.getMethodName();
			if (cl.startsWith("kilim.Worker") || meth.equals("go")|| meth.equals("ds")){
				continue;
			}
			String line = ste.getLineNumber() < 0 ? "" : ":" + ste.getLineNumber();
			System.err.println('\t' + cl + '.' + ste.getMethodName() + '('	+ ste.getFileName() + line + ')');
		}
		System.err.println();
	}

	/**
	 * In the normal (non-exception) scheme of things, the iStack is incremented
	 * by down() on the way down and decremented by a corresponding up() when
	 * returning or pausing. If, however, an exception is thrown, we lose track
	 * of where we are in the hierarchy. We recalibrate iStack by creating a
	 * dummy exception and comparing it to the stack depth of an exception taken
	 * earlier. This is done in scheduler.getStackDepth(); A sample stack trace
	 * of the dummy exception looks as follows
	 * 
	 * <pre>
	 *   at kilim.Fiber.upEx(Fiber.java:250)
	 *   at kilim.test.ex.ExCatch.normalCatch(ExCatch.java)
	 *   at kilim.test.ex.ExCatch.test(ExCatch.java)
	 *   at kilim.test.ex.ExCatch.execute(ExCatch.java)
	 *   at kilim.Task.runExecute(Task.java)
	 *   at kilim.WorkerThread.run(WorkerThread.java:11)
	 * </pre>
	 * 
	 * We have to figure out the stack depth (iStack) of the method that caught
	 * the exception and called upEx ("normalCatch" here). The call stack below
	 * runExecute may be owned by the scheduler, which may permit more than one
	 * task to build up on the stack. For this reason, we let the scheduler tell
	 * us the depth of upEx below the task's execute().
	 * 
	 * @return Fiber.pc (note: in contrast up() returns status)
	 */
	public int upEx( ) {
		//System.err.println("called upEx in: " + where);
		//new Exception("").printStackTrace();
		// compute new iStack.
		int is = iso.getStackDepth() - 3; // remove upEx and convert to 0-based. hmmmm the apply methods are causing some bother here
						
		//System.err.println("called upEx is: " + is);
		// index.
		State cs = stateStack[is];

		for (int i = iStack; i >= is; i--) {
			stateStack[i] = null; // release state
		}

		iStack = is;
		curState = cs;
		return (cs == null) ? 0 : cs.pc;
	}

	/**
	 * Called by the weaved code while rewinding the stack. If we are about to
	 * call a virtual pausable method, we need an object reference on which to
	 * call that method. The next state has that information in state.self
	 */
	public Object getCallee() {
		assert stateStack[iStack] != PAUSE_STATE : "No callee: this state is the pause state";
		assert stateStack[iStack] != null : "Callee is null";
		
		if(stateStack[iStack + 1] == null){
			throw new RuntimeException("Callee is null 1");
		}
		
		if(stateStack[iStack + 1].self == null){
			throw new RuntimeException("Callee is null 1");
		}
		
		return stateStack[iStack + 1].self;
	}

	private State[] ensureSize(int newsize) {
		// System.out.println("ENSURE SIZE = " + newsize);
		State[] newStack = new State[newsize];
		System.arraycopy(stateStack, 0, newStack, 0, stateStack.length);
		stateStack = newStack;
		return newStack;
	}

	/**
	 * Called by the generated code before pausing and unwinding its stack
	 * frame.
	 * 
	 * @param state
	 */
	public void setState(State state) {
		stateStack[iStack] = state;
		isPausing = true;
		/*if(debug){
			ds();
			StringBuilder sb = new StringBuilder("Fiber: " + System.identityHashCode(this) +  " setState[" + + iStack + "] = ");
			stateToString(sb, state);
			System.err.println(sb);
		}*/
	}
	
	/*public void printState() {
		StringBuilder sb = new StringBuilder("Fiber: " + System.identityHashCode(this) +  " setState[" + + iStack + "] = ");
		stateToString(sb, state);
		System.err.println(sb);
	}*/

	public State getState() {
		return stateStack[iStack];
	}

	void togglePause() {
		// The client code would have called fiber.down()
		// before calling Task.pause. curStatus would be
		// upto date.

		if (curState == null) {
			setState(PAUSE_STATE);
		} else {
			assert curState == PAUSE_STATE : "togglePause: Expected PAUSE_STATE, instead got: iStack == "+ iStack + ", state = " + curState;
			stateStack[iStack] = null;
			isPausing = false;
		}
	}

	public void wrongPC(String atplace) {
		StringBuilder sb = new StringBuilder("Wrong pc ["+atplace+"]: ");

		stateToString(sb, this.stateStack[0]);

		sb.append(":: ");
		
		for(State s : this.stateStack){
			sb.append(null==s?"null":s.toString());
			sb.append(", ");
		}
		//System.err.println(sb.toString());
		throw new IllegalStateException(sb.toString()  );// calling StringBuilder here results in inf loop
	}

	static private void stateToString(StringBuilder sb, State s) {
		if (s == PAUSE_STATE) {
			sb.append("PAUSE\n");
			return;
		}
		if(s==null){
			return;
		}
		Field[] fs = s.getClass().getFields();
		for (int i = 0; i < fs.length; i++) {
			Field f = fs[i];
			String name = f.getName();
			sb.append(name).append("=");
			Object v;
			try {
				v = f.get(s);
			} catch (IllegalAccessException iae) {
				v = "?";
			}
			sb.append(v==null?null:v.getClass());
			if(name.equals("pc") ){
				sb.append(" [" + v.toString() + "]");
			}
			
			sb.append(", ");
		}
		sb.append('\n');
	}

	
	public void printState() {
		printState(this);
	}
	
	public void printState(Fiber f) {
		StringBuilder sb = new StringBuilder(40);
		sb.append("Fiber: " + System.identityHashCode(this) + " ");
		sb.append("iStack = ").append(iStack).append(", pc=").append(pc);
		if (isPausing) {
			sb.append(" pausing");
		}
		sb.append('\n');
		for (int i = 0; i < stateStack.length; i++) {
			State st = stateStack[i];
			if (st != null) {
				sb.append(st.getClass().getName()).append('[').append(i).append("]: ");
				stateToString(sb, stateStack[i]);
			}
		}
		System.err.println(sb);
	}
	
	void clearPausing() {
		isPausing = false;
	}

}