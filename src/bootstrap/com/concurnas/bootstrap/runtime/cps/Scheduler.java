package com.concurnas.bootstrap.runtime.cps;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.concurnas.bootstrap.lang.Lambda.Function1;
import com.concurnas.bootstrap.runtime.CopyDefinition;
import com.concurnas.bootstrap.runtime.CopyTracker;
import com.concurnas.bootstrap.runtime.ref.Ref;

public class Scheduler {
	private final Worker[] workers;
	private final int workerCount;
	private int idx;
	private Scheduler parentScheduler;
	private final Scheduler masterGroupScheduler;

	private final ConcurrentHashMap<Worker, TerminationState> dedicatedWorkers;
	private final Set<Scheduler> children;

	public Scheduler(int cnt) {
		parentScheduler = null;
		masterGroupScheduler = null;
		children = Collections.newSetFromMap(new WeakHashMap<Scheduler, Boolean>());
		dedicatedWorkers = new ConcurrentHashMap<Worker, TerminationState>();
		idx = 0;
		workers = new Worker[cnt];
		workerCount = cnt;

		for (int n = 0; n < cnt; n++) {
			// create workers with scheduler allocation offset so as to trick into passing
			// workload to next holder
			Worker w = new Worker(new Scheduler(workers, (n + 1) % cnt, dedicatedWorkers, this), String.format("Core Concurnas Worker-%01d", n), false);
			workers[n] = w;
			w.start();
		}
	}

	public static AtomicLong workerPoolId = new AtomicLong();

	private Scheduler(int cnt, String workerNamePrefix, Scheduler parentScheduler) {
		this.parentScheduler = parentScheduler;
		masterGroupScheduler = null;
		children = Collections.newSetFromMap(new WeakHashMap<Scheduler, Boolean>());
		dedicatedWorkers = new ConcurrentHashMap<Worker, TerminationState>();
		idx = 0;
		workers = new Worker[cnt];
		workerCount = cnt;

		long wpid = workerPoolId.getAndIncrement();
		if (workerNamePrefix == null) {
			workerNamePrefix = "" + wpid;
		}

		for (int n = 0; n < cnt; n++) {
			// create workers with scheduler allocation offset so as to trick into passing
			// workload to next holder
			Worker w = new Worker(new Scheduler(workers, (n + 1) % cnt, dedicatedWorkers, this), String.format("Concurnas Pool Worker-%s-%01d", workerNamePrefix, n), false);
			workers[n] = w;
			w.start();
		}
		parentScheduler.registerChildGroupScheduler(this);
	}

	private Scheduler(final Worker[] workers, final int idx, ConcurrentHashMap<Worker, TerminationState> dedicatedWorkers, Scheduler parentScheduler) {
		this.parentScheduler = null;
		this.children = null;
		this.masterGroupScheduler = parentScheduler;
		this.workers = workers;
		this.workerCount = workers.length;
		this.idx = idx;
		this.dedicatedWorkers = dedicatedWorkers;
	}

	public Scheduler createDedicatedThreadWorkerPool(int workerCount, String workerNamePrefix) {
		return new Scheduler(workerCount, workerNamePrefix, this);
	}

	private synchronized void registerChildGroupScheduler(Scheduler child) {
		if (masterGroupScheduler != null) {
			masterGroupScheduler.registerChildGroupScheduler(child);
		} else {
			this.children.add(child);
		}
	}

	private synchronized void unregisterChildGroupScheduler(Scheduler child) {
		if (masterGroupScheduler != null) {
			masterGroupScheduler.unregisterChildGroupScheduler(child);
		} else {
			this.children.remove(child);
		}
	}

	public Scheduler getSchedulerForNonRootWorker() {
		return new Scheduler(workers, (idx + 1) % workers.length, dedicatedWorkers, this);
	}

	public Scheduler() {
		this(getProcessorCount() * 2);
	}

	public ArrayList<String> getWorkerStatus() {
		ArrayList<String> ret = new ArrayList<String>(workers.length);
		for (Worker w : workers) {
			String stat = w.getStatus();
			if (stat != null) {
				ret.add(stat);
			}
		}

		if (!this.dedicatedWorkers.isEmpty()) {
			for (Worker w : this.dedicatedWorkers.keySet()) {
				String stat = w.getStatus();
				if (stat != null) {
					ret.add(stat);
				}
			}
		}

		if (null != this.children) {
			for (Scheduler sch : this.children) {
				ret.addAll(sch.getWorkerStatus());
			}
		}

		return ret;
	}

	private static int getProcessorCount() {
		return Runtime.getRuntime().availableProcessors();
	}

	private static final DefaultIsoExceptionHandler defaultExHan = new DefaultIsoExceptionHandler();

	public Fiber getFiber(Iso core) {
		return core.fiber;
	}

	// objects being copied into the iso may make use of global variables, so here
	// we enumerate them for capture in the setupGlobals
	private HashSet<String> extractIndirectGlobals(CopyTracker tracker) {
		HashSet<String> globalDeps = new HashSet<String>();
		if (null != tracker) {
			HashSet<Class<?>> classCaughtAlready = new HashSet<Class<?>>();
			for (Object obj : tracker.clonedAlready.keySet()) {
				Class<?> cls = obj.getClass();
				if (!classCaughtAlready.contains(cls)) {
					if (obj instanceof CObject) {// if obj is of type Class, then this does not extend CObject as jvm doesnt like
													// it being intercepted
						Collection<? extends String> deps = ((CObject) obj).getGlobalDependancies$();
						if (deps != null) {
							globalDeps.addAll(deps);
						}
					}

					classCaughtAlready.add(cls);
				}
			}
		}

		return globalDeps;
	}

	private void initImplicitGlobals(Fiber isoFiber, Fiber parent, ClassLoader provider, HashSet<String> implicitGlobals, CopyTracker tracker) {
		/*
		 * ALOAD 1 INVOKESTATIC
		 * com/concurnas/runtime/bootstrapCloner/Cloner$Globals$.getInstance?
		 * ()Lcom/concurnas/runtime/bootstrapCloner/Cloner$Globals$; GETFIELD
		 * com/concurnas/runtime/bootstrapCloner/Cloner$Globals$.cloner :
		 * Lcom/concurnas/runtime/bootstrapCloner/Cloner; ALOAD 2 DUP INVOKESTATIC
		 * com/concurnas/lang/Equalifier$Globals$.copyInstance?
		 * (Lcom/concurnas/runtime/ConcurnificationTracker;)Lcom/concurnas/lang/
		 * Equalifier$Globals$; INVOKEVIRTUAL
		 * com/concurnas/runtime/bootstrapCloner/Cloner.clone
		 * (Lcom/concurnas/runtime/ConcurnificationTracker;Ljava/lang/Object;)Ljava/lang
		 * /Object; CHECKCAST com/concurnas/lang/Equalifier$Globals$ INVOKESTATIC
		 * com/concurnas/lang/Equalifier$Globals$.setInstance?
		 * (Lcom/concurnas/bootstrap/runtime/cps/Fiber;Lcom/concurnas/lang/
		 * Equalifier$Globals$;)V
		 */
		Fiber.currentFiber.set(parent);

		try {
			Class<?> clonerGlobalClass = provider.loadClass("com.concurnas.runtime.bootstrapCloner.Cloner$Globals$");
			Class<?> clonerClass = provider.loadClass("com.concurnas.runtime.bootstrapCloner.Cloner");
			Object clonerGlobalObj = clonerGlobalClass.getMethod("getInstance?").invoke(null);
			Object cloner = clonerGlobalClass.getField("cloner").get(clonerGlobalObj);
			Method cloneMethod = clonerClass.getMethod("clone", CopyTracker.class, Object.class, CopyDefinition.class);

			for (String dependancy : implicitGlobals) {
				// System.out.println("dep: " + dependancy);

				Class<?> globalToDuplicate = provider.loadClass(dependancy.replace('/', '.'));
				Object instance = globalToDuplicate.getMethod("copyInstance?", CopyTracker.class).invoke(null, tracker);
				Object copy = cloneMethod.invoke(cloner, tracker, instance, null);
				globalToDuplicate.getMethod("setInstance?", Fiber.class, globalToDuplicate).invoke(null, isoFiber, copy, parent);
			}
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			// throw new RuntimeException(e);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException | NoSuchFieldException e) {
			// throw new RuntimeException(e);
			// why do we get here?
		}
	}

	public void spawnDedicatedWorker(IsoTask<Ref<?>> task, String desc, CopyTracker ct) throws Throwable {
		spawnDedicatedWorker(task, desc, ct, Fiber.getCurrentFiber());
	}

	public synchronized void spawnDedicatedWorker(IsoTask<Ref<?>> task, String desc, CopyTracker ct, Fiber parent) throws Throwable {
		Worker w = new Worker(this.getSchedulerForNonRootWorker(), "Dedicated Worker for: " + desc, true);
		w.start();

		IsoCore isoc = new IsoCore(w, task, desc);

		this.scheduleTask(isoc, ct, parent);
	}

	public void removeDedicatedWorker(Worker w) {
		this.dedicatedWorkers.remove(w);
	}

	public void scheduleTask(IsoCore iso, CopyTracker tracker) throws Throwable {
		scheduleTask(iso, tracker, null);
	}

	public void scheduleTask(IsoCore iso, CopyTracker tracker, Fiber parent) throws Throwable {
		SyncTracker st = parent == null ? null : parent.getCurrentSyncTracker();

		HashSet<String> implicitGlobals = extractIndirectGlobals(tracker);

		if (null != st) {
			st.awaitFor(iso.func.getIsInitCompleteFlag());
		}

		if (parent != null) {
			Fiber.currentFiber.set(iso.fiber);
			try {
				HashSet<String> explicitGlobals = iso.func.setupGlobals(iso.fiber, tracker == null ? new CopyTracker() : tracker, parent);
				implicitGlobals.removeAll(explicitGlobals);
				if (!implicitGlobals.isEmpty()) {// load...
					initImplicitGlobals(iso.fiber, parent, iso.func.getClass().getClassLoader(), implicitGlobals, tracker);
				}
			} finally {
				Fiber.currentFiber.set(null);
			}
		}

		iso.setExceptionHandler(defaultExHan);
		iso.worker.createTask(iso);
	}

	public void scheduleTask(AbstractIsoTask func, String desc) throws Throwable{	scheduleTask(func, desc, null);}
	public void scheduleTask(AbstractIsoTask func, String desc, Fiber parent) throws Throwable{
		scheduleTask(prepare(func, desc, parent), null, parent);
	}
	
	public IsoCore prepare(AbstractIsoTask func, String desc) {
		return prepare(func, desc);
	}

	public IsoCore prepare(AbstractIsoTask func, String desc, Fiber parent) {
		// TODO: allocation scheme needs improvement/load balance?
		idx = (idx + 1) % workerCount;
		return new IsoCore(workers[idx], func, desc);
	}

	public IsoNotifiable prepare(IsoTaskNotifiable func, String desc, Fiber parent) {
		// TODO: allocation scheme needs improvement/load balance?
		idx = (idx + 1) % workerCount;
		return new IsoNotifiable(workers[idx], func, desc);
	}

	public IsoAwait prepare(IsoTaskAwait func, String desc, Fiber parent) {
		// TODO: allocation scheme needs improvement/load balance?
		idx = (idx + 1) % workerCount;
		return new IsoAwait(workers[idx], func, desc);
	}

	public IsoEvery prepare(IsoTaskEvery func, String desc, Fiber parent) {
		// TODO: allocation scheme needs improvement/load balance?
		idx = (idx + 1) % workerCount;
		return new IsoEvery(workers[idx], func, desc);
	}

	public Ref<Boolean> scheduleTask(IsoTaskNotifiable func, String desc) {
		return scheduleTask(func, desc, null);
	}

	public Ref<Boolean> scheduleTask(IsoTaskNotifiable func, String desc, Fiber parent) {
		return scheduleTask(prepare(func, desc, parent), null, parent);
	}

	public Ref<Boolean> scheduleTask(IsoNotifiable iso, CopyTracker tracker, Fiber parent) {
		HashSet<String> implicitGlobals = extractIndirectGlobals(tracker);

		if (parent != null) {
			Fiber.currentFiber.set(iso.fiber);
			try {
				HashSet<String> explicitGlobals = iso.ntask.setupGlobals(iso.fiber, tracker == null ? new CopyTracker() : tracker, parent);
				implicitGlobals.removeAll(explicitGlobals);
				if (!implicitGlobals.isEmpty()) {// load...
					initImplicitGlobals(iso.fiber, parent, iso.ntask.getClass().getClassLoader(), implicitGlobals, tracker);
				}

			} finally {
				Fiber.currentFiber.set(null);
			}
		}

		iso.setExceptionHandler(defaultExHan);
		iso.setInitExceptionHandler(new SetExceptionToInit(iso.ntask.getIsInitCompleteFlag()));

		iso.worker.createTask(iso);
		return iso.ntask.getIsInitCompleteFlag();// wait for initilizer to state it's completed
	}

	public Ref<Boolean> scheduleTask(IsoTaskAwait func, String desc) {
		return scheduleTask(func, desc, null);
	}

	public Ref<Boolean> scheduleTask(IsoTaskAwait func, String desc, Fiber parent) {
		return scheduleTask(prepare(func, desc, parent), null, parent);
	}

	public Ref<Boolean> scheduleTask(IsoAwait iso, CopyTracker tracker, Fiber parent) {
		HashSet<String> implicitGlobals = extractIndirectGlobals(tracker);

		if (parent != null) {
			Fiber.currentFiber.set(iso.fiber);
			try {
				HashSet<String> explicitGlobals = iso.ntask.setupGlobals(iso.fiber, tracker == null ? new CopyTracker() : tracker, parent);
				implicitGlobals.removeAll(explicitGlobals);
				if (!implicitGlobals.isEmpty()) {// load...
					initImplicitGlobals(iso.fiber, parent, iso.ntask.getClass().getClassLoader(), implicitGlobals, tracker);
				}
			} finally {
				Fiber.currentFiber.set(null);
			}

		}

		Ref<Boolean> isComplete = iso.ntask.getIsInitCompleteFlag();
		SetExceptionToInit handleerr = new SetExceptionToInit(isComplete);
		iso.setExceptionHandler(handleerr);
		iso.setInitExceptionHandler(handleerr);

		iso.worker.createTask(iso);

		return isComplete;// flag is set once apply block completes
	}

	public Ref<Boolean> scheduleTask(IsoTaskEvery func, String desc) {
		return scheduleTask(func, desc, null);
	}

	public Ref<Boolean> scheduleTask(IsoTaskEvery func, String desc, Fiber parent) {
		return scheduleTask(prepare(func, desc, parent), null, parent);
	}

	public Ref<Boolean> scheduleTask(IsoEvery iso, CopyTracker tracker, Fiber parent) {
		HashSet<String> implicitGlobals = extractIndirectGlobals(tracker);

		if (parent != null) {
			Fiber.currentFiber.set(iso.fiber);
			try {
				HashSet<String> explicitGlobals = iso.ntask.setupGlobals(iso.fiber, tracker == null ? new CopyTracker() : tracker, parent);
				implicitGlobals.removeAll(explicitGlobals);
				if (!implicitGlobals.isEmpty()) {// load...
					initImplicitGlobals(iso.fiber, parent, iso.ntask.getClass().getClassLoader(), implicitGlobals, tracker);
				}
			} finally {
				Fiber.currentFiber.set(null);
			}
		}

		Ref<Boolean> isComplete = iso.ntask.getIsInitCompleteFlag();

		iso.setExceptionHandler(defaultExHan);
		iso.setInitExceptionHandler(new SetExceptionToInit(isComplete));
		// System.err.println("end of scheduleTask - create task");
		iso.worker.createTask(iso);
		// System.err.println("end of scheduleTask created so ret");

		return isComplete;// flag is set once apply block completes
	}

	private static final class SetExceptionToInit extends Function1<Throwable, Void> {
		private Ref<Boolean> isComplete;

		public SetExceptionToInit(Ref<Boolean> isComplete) {
			super(null);
			this.isComplete = isComplete;
		}

		@Override
		public Void apply(Throwable e) {
			/*
			 * StringWriter errors = new StringWriter();
			 * errors.write("Unexpected exception thrown during iso operation. Cause:\n");
			 * thro.printStackTrace(new PrintWriter(errors));
			 * 
			 * System.err.println(errors.toString());
			 */
			isComplete.setException(e);

			return null;
		}

		@Override
		public Object[] signature() {
			return null;
		}
	}

	/*
	 * Termination levels:
	 * 
	 * bum - complete current job, complete pending, allow new new jobs, stop when
	 * all in pause state
	 * 
	 * terminate - complete current job, complete pending, dont allow new new jobs,
	 * stop when all in pause state
	 * 
	 * kill - complete current job, ignore pending, dont allow new new jobs, stop
	 * when all in pause state = throw little error if pending jobs waiting
	 * 
	 * super kill - dont complete current job, ignore pending, dont allow new new
	 * jobs, stop when all in pause state
	 * 
	 * TODO: need to code the above up and test
	 * 
	 */

	public void cancelAll() {
		terminate(true);
	}

	public void terminate() {
		terminate(false);
	}

	private void terminate(boolean cancel) {
		if (masterGroupScheduler != null) {// call group instance master
			this.masterGroupScheduler.terminate(cancel);
			return;
		}

		// TODO: what happens if this gets called more than once?
		TerminationState[] terms = new TerminationState[workerCount];

		for (int n = 0; n < workerCount; n++) {
			TerminationState term = new TerminationState(cancel);
			workers[n].terminate(term);
			terms[n] = term;
		}

		for (int n = 0; n < workerCount; n++) {
			try {
				terms[n].hasTerminated();
			} catch (InterruptedException e) {
				defaultExHan.apply(e);// e.printStackTrace();
			}
		}

		for (Scheduler sch : new HashSet<Scheduler>(this.children)) {
			sch.terminate(cancel);
		}

		for (Worker w : this.dedicatedWorkers.keySet()) {
			try {
				TerminationState t = this.dedicatedWorkers.get(w);
				if (t != null) {
					t.hasTerminated();
				}
			} catch (InterruptedException e) {
				defaultExHan.apply(e);
			}
		}

		if (!cancel && this.parentScheduler != null) {
			parentScheduler.unregisterChildGroupScheduler(this);
			parentScheduler = null;
		}

	}

	public void kill() {
		// TODO: force ps -9 on Thread, err if stuff todo still?
	}

	public void setContextClassloader(ClassLoader cls) {
		for (int n = 0; n < workerCount; n++) {
			workers[n].setContextClassLoader(cls);
		}
	}

	public int getWorkerCount() {
		return this.workerCount;
	}

}
