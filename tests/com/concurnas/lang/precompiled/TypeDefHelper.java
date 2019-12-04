package com.concurnas.lang.precompiled;

import com.concurnas.lang.Typedefs;
import com.concurnas.lang.Typedef;

@Typedefs(typedefs={@Typedef(name="mylistsimple", type="Ljava/util/ArrayList<Ljava/lang/String;>;", args = {}), 
		@Typedef(name="tdwargs", type="Ljava/util/ArrayList<Lx;>;", args = {"x"}),
		@Typedef(name="tdar", type="[I", args = {}),
		@Typedef(name="tdarBox", type="[Ljava/lang/Integer;", args = {}),
		@Typedef(name="tdAFuncref", type="Lcom/concurnas/bootstrap/lang/Lambda$Function2<Ljava/lang/Integer;Ljava/lang/Integer;Ljava/lang/Integer;>;", args = {}),
		@Typedef(name="tdhmintar", type="Ljava/util/HashMap<Lx;[Ljava/lang/Integer;>;", args = {"x"}),
		@Typedef(name="numericalPlusString", type="I|J|S|F|D|C|B|Ljava/lang/String;", args = {}),
		@Typedef(name="numerical", type="I|J|S|F|D|C|B", args = {})})
public class TypeDefHelper {
	
}
