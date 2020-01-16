package com.concurnas.lang;

import com.concurnas.bootstrap.lang.TypedActorInterface;
import com.concurnas.bootstrap.lang.Lambda.Function0;
import com.concurnas.runtime.bootstrapCloner.Cloner;

public class TypedActor<On> extends Actor implements TypedActorInterface<On>{
	
	protected Function0<On> creator;
	protected On of;
	
	protected TypedActor(Class<?>[] types, Function0<On> creator){
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
	}
}
