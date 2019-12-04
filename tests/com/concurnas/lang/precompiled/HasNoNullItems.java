package com.concurnas.lang.precompiled;

import java.util.List;

import com.concurnas.lang.NoNull;
import com.concurnas.lang.NoNull.When;

public class HasNoNullItems {

	public static @NoNull List<@NoNull String> addToList(@NoNull List<@NoNull String> addTo, @NoNull String item ){
		addTo.add(item);
		return addTo;
	}
	
	public static <@NoNull X> @NoNull List<@NoNull X> addToListLG(@NoNull List<X> addTo, @NoNull X item ){
		addTo.add(item);
		return addTo;
	}
	
	
	public static @NoNull(when = When.NEVER) List<@NoNull(when = When.NEVER) String> addToListNULL(@NoNull(when = When.NEVER) List<@NoNull(when = When.NEVER) String> addTo, @NoNull(when = When.NEVER) String item ){
		addTo.add(item);
		return addTo;
	}
	
	public static <@NoNull(when = When.NEVER) X> @NoNull(when = When.NEVER) List<@NoNull(when = When.NEVER) X> addToListLGNULL(@NoNull(when = When.NEVER) List<X> addTo, @NoNull(when = When.NEVER) X item ){
		addTo.add(item);
		return addTo;
	}
	
	public static @NoNull(when = When.MAYBE) List<@NoNull(when = When.MAYBE) String> addToListMAYBE(@NoNull(when = When.MAYBE) List<@NoNull(when = When.MAYBE) String> addTo, @NoNull(when = When.MAYBE) String item ){
		addTo.add(item);
		return addTo;
	}
	
	public static <@NoNull(when = When.MAYBE) X> @NoNull(when = When.MAYBE) List<@NoNull(when = When.MAYBE) X> addToListLGNMAYBE(@NoNull(when = When.MAYBE) List<X> addTo, @NoNull(when = When.MAYBE) X item ){
		addTo.add(item);
		return addTo;
	}
	
	public static List<String> addToListDefault(List<String> addTo, String item ){
		addTo.add(item);
		return addTo;
	}

	public static <X> List<X> addToListLGNDefault(List<X> addTo, X item ){
		addTo.add(item);
		return addTo;
	}
	
	
	public @NoNull(when = When.ALWAYS) String afieldALWAUS1 = "ok";
	public @NoNull(when = When.ALWAYS) String afieldALWAUS2 = "ok";
	public @NoNull(when = When.NEVER) String afieldNEVER = "ok";
	public @NoNull(when = When.MAYBE) String afieldMAYBE = "ok";
	public String afieldDefault = "ok";
	
}
