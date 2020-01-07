package com.concurnas.lang;

import java.util.HashSet;

import com.concurnas.bootstrap.lang.Lambda.Function0;
import com.concurnas.bootstrap.lang.offheap.Decoder;
import com.concurnas.bootstrap.lang.offheap.Encoder;
import com.concurnas.bootstrap.runtime.CopyTracker;
import com.concurnas.bootstrap.runtime.ReifiedType;
import com.concurnas.bootstrap.runtime.cps.AbstractIsoTask;
import com.concurnas.bootstrap.runtime.cps.Fiber;
import com.concurnas.bootstrap.runtime.ref.DefaultRef;
import com.concurnas.bootstrap.runtime.ref.DirectlyGettable;
import com.concurnas.runtime.Pair;
import com.concurnas.runtime.channels.PriorityQueue;
import com.concurnas.runtime.ref.Local;

@Shared
public class Actor  implements ReifiedType  {
	private final PriorityQueue<Pair<?, ?>> processQueue = new PriorityQueue<Pair<?, ?>>();//usually 2nd arg is Function1<ActingOn, ?> 
	//private final LinkedList<Function0<?>> initializers = new LinkedList<Function0<?>>();

	protected final void addCall(int priority, Pair<?, ?> x){
		processQueue.add(priority, x);
	}

	private boolean started=false;
	private boolean running=false;
	
	private Class<?>[] type;
	
	protected Actor(Class<?>[] type){
		Class<?>[] types = new Class<?>[type.length];
		System.arraycopy(type, 0, types, 0, type.length);
		this.type = types;
	}
	
	public final Class<?>[] getType() {
		//type itself is not immutable
		Class<?>[] ret=  new Class<?>[type.length];
		System.arraycopy(type, 0, ret, 0, type.length);
		return ret;
	}
	
	private final static class VoidFunction0PassThrough extends Function0<Void> {

		private Function0v passThrough;

		public VoidFunction0PassThrough(Function0v passThrough) {
			super(passThrough.actingOnType());
			this.passThrough = passThrough;
		}

		@Override
		public Void apply() {
			passThrough.apply();
			return null;
		}

		@Override
		public Object[] signature() {
			return passThrough.signature();
		}
		
		 public void bind(Object to){
			 passThrough.bind(to);
		 }
	}
	
	private final class ActorManager extends AbstractIsoTask{
		private Actor actorManagerObj;
		private transient Local<Boolean> started = new Local<Boolean>(new Class<?>[]{Boolean.class});
		
		public ActorManager(Actor actorManagerObj) {
			super(null);
			this.actorManagerObj = actorManagerObj;
			//this.actorManagerClass = actorManagerObj.getClass();
		}

		public DirectlyGettable<Boolean> getStarted() {
			return started;
		}

		@Override
		public Void apply() {
			boolean startOk = false;
			try{
				initialStart();
				
				//new Exception("uh oh on : " + "Fiber: " + System.identityHashCode(Fiber.getCurrentFiber())).printStackTrace();
				
				//System.err.println("c fiber" + Fiber.getCurrentFiber());
				started.set(true);
				startOk=true;
			}
			catch(Throwable e){
				//set execptions onto started ref for pickup by the init...
				started.setException(e);
			}
			
			while(startOk){
				//call actor initalizer references...
				Pair<?, ?> retAndDo = processQueue.pop();
				DefaultRef<Object> ret = (DefaultRef<Object>) retAndDo.getA();
				Object msg = retAndDo.getB();
				
				Function0<?> todo;
				if(msg instanceof Function0){
					todo = (Function0<?>)msg;
				}else if(msg instanceof Function0v) {
					Function0v fzero = (Function0v)msg;
					todo = new VoidFunction0PassThrough(fzero);
				}else {
					return null;
				}
				
				Class<?> actOn = todo.actingOnType();
				
				if(Actor.class.isAssignableFrom(actOn)){
					//System.err.println("actor call on manager");
					todo.bind(actorManagerObj);
					recieveActor(ret, todo);
					continue;
				}
				else{
					bindCall(todo);
				}
				
				if(!running){
					//System.err.println("recieve: " + todo);
					ret.setException(new Exception("Actor is not running"));
				}
				else{
					//System.err.println("recieve: " + todo);
					try{
						recieve(ret, todo);
					}catch(Throwable thr){
						ret.setException(thr);
					}
				}
			}
			return null;
			//TODO: error handler, termination
		}

		@Override
		public DefaultRef<Boolean> getIsInitCompleteFlag() {
			Local<Boolean> ll = new  Local<Boolean>(new Class<?>[]{Boolean.class} );
			ll.set(true);
			return ll;
		}

		@Override
		public HashSet<String> setupGlobals(Fiber isoFiber, CopyTracker tracker) {
			return new HashSet<String>();
		}

		@Override
		public HashSet<String> setupGlobals(Fiber isoFiber, CopyTracker tracker, Fiber parent) {
			return new HashSet<String>();
		}

		@Override
		public void teardownGlobals() {
		}
		
	}
	
	protected void bindCall(Function0<?> tobind){
		
	}

	protected final void onInit(){
		if(!started){
			ActorManager manager = new ActorManager(this);
			Fiber.getScheduler().scheduleTask(manager, "Actor work loop for: " + this.getClass().getName());
			started = manager.getStarted().get();//can throw exception if were unable to init correclty.
			started=true;
			//started=true;
		}
	}
	

	private void recieveActor(DefaultRef ret, Function0<?> func){
		try{
			ret.set( func.apply() );
		}
		catch(Throwable e){
			onFailActor(e, ret, func);
		}
	}
	
	private void recieveHandleException(DefaultRef ret, Object msg){
		try{
			recieve(ret, msg);
		}
		catch(Throwable e){
			onFail(e, ret, msg);
		}
	}

	/*public void recieve(Object msg){
		Function0<?> func = (Function0)msg;
		DefaultRef ret = new Local<Object>(new Class<?>[]{Object.class});//TODO: funcion0 should have a method stating its return type, dont use object here
		
		ret.set( func.apply() );
	}*/
	
	protected void recieve(DefaultRef<Object> ret, Object msg){
		Function0<?> func = (Function0)msg;
		
		//System.err.println("" + func);
		
		Object res = func.apply();
		if(res instanceof DirectlyGettable<?>) {
			do {
				DirectlyGettable<?> dga = (DirectlyGettable<?>)res;
				res = dga.get();
				
				ret = (DefaultRef<Object>)ret.get();
			} while (res instanceof DirectlyGettable<?>);
		}
		
		ret.set( res );
	}
	
	protected void initialStart(){
		start();
	}
	
	public void onFailActor(Throwable e, DefaultRef<Object> ret, Function0<?> func){
		//ret.setException(new Exception("Unhandled exception during actor processing of: " + func, e));
		ret.setException(e);
	}
	
	public void onFail(Throwable e, DefaultRef<Object> ret, Object msg){
		//ret.setException(new Exception("Unhandled exception during actor processing of: " + msg, e));
		ret.setException(e);
		restart();
	}
	
	
	public void start(){
		if(running){
			throw new RuntimeException("Actor is already running");
		}
		
		prestart();
		startOperation();
		poststart();
		running=true;
	}
	
	protected void startOperation(){
	}
	protected void prestart(){
	}
	protected void poststart(){
	}
	
	public void stop(){
		if(!running){
			throw new RuntimeException("Actor is not running");
		}
		
		preStop();
		stopOperation();
		postStop();
		running = false;
	}
	
	protected void stopOperation(){
	}
	protected void preStop(){
	}
	protected void postStop(){
	}
	

	public void restart(){
		
		prerestart();
		restartOperation();
		postrestart();

	}
	
	protected void restartOperation(){
		stop();
		start();
	}
	protected void prerestart(){
	}
	protected void postrestart(){
	}
	
	public boolean isRunning(){
		return this.running;
	}
	

	//in order to implicitly generate handlers for these methods we must explicitly define them below... hack
	@Override
	public String toString(){
		return super.toString();
	}
	
	@Override
	public int hashCode(){
		return super.hashCode();
	}
	
	@Override
	public boolean equals(Object obj){
		return super.equals(obj);
	}
	
	public void toBinary(Encoder enc){
		throw new RuntimeException("Actors cannot be converted into binary format: " + this.getClass() + " is an actor");
	}
	
	public void fromBinary(Decoder dec){
		throw new RuntimeException("Actors cannot be converted from binary format: " + this.getClass() + " is an actor");
	}
	
	/*public static String[] metaBinary(){
		throw new RuntimeException("Actors cannot be converted into/from binary format");
	}*/
}
