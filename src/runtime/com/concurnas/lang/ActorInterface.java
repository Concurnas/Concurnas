package com.concurnas.lang;

import com.concurnas.bootstrap.lang.Lambda.Function0;
import com.concurnas.bootstrap.runtime.ref.DefaultRef;

public interface ActorInterface {

	public abstract void setSupervisor();

	public abstract void recieve(DefaultRef<?> ret, Object msg);

	public abstract void onFailActor(Throwable e, DefaultRef<?> ret, Function0<?> func);

	public abstract void onFail(Throwable e, DefaultRef<?> ret, Object msg);

	public abstract void start();

	public abstract void stop();

	public abstract void restart();

}