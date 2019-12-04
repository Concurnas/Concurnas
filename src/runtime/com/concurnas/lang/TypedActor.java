package com.concurnas.lang;

import com.concurnas.bootstrap.lang.TypedActorInterface;
import com.concurnas.bootstrap.lang.Lambda.Function0;
import com.concurnas.bootstrap.lang.Lambda.Function0v;
import com.concurnas.runtime.bootstrapCloner.Cloner;

public class TypedActor<On> extends Actor implements TypedActorInterface<On>{
	
	protected Function0<On> creator;
	protected On of;
	//protected Class<?> onClass;//TODO:what is onclass used for?
	
/*	private static Class<?>[] creationSetter(Class<?>[] type, Function0<?> creator){
		Class<?>[] types = new Class<?>[type.length + 1];
		//types[0] = creator.actingOnType();//slightly elaborate way of exactracing this information, why not have the callee pass it in?
		System.arraycopy(type, 0, types, 1, type.length);
		return types;
	}*/
	
	protected TypedActor(Class<?>[] types, Function0<On> creator){
		//super(creationSetter(types, creator));
		super(types);
		this.creator = creator;
	}
	
	@Override
	protected final void bindCall(Function0<?> tobind){
		tobind.bind(of);
	}
	
	/* (non-Javadoc)
	 * @see com.concurnas.lang.TypedActorInterface#getActeeClone()
	 */
	@Override
	public final On getActeeClone(){
		return Cloner.cloner.clone(this.of);
	}
	
	protected void stopOperation(){
		of=null;
	}
	protected void startOperation(){
		of = creator.apply();
		//onClass = of.getClass();
	}
}
