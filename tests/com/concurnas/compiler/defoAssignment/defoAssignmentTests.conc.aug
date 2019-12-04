//##69. DefoAssignment - 1. Simple stuff
def err1()
{
	f int
	g = f;//ERR as f may not be assigned to something...
}

def xxx() => 5>6; 
def err2()
{
	f int
	if(xxx())	{ f=6 }
	g = f;//ERR as f may not be assigned to something...
}

def ok1()
{
	f int
	if(xxx())	{ f=6 }
	else { f=66 } 
	g = f;//fine as defo assigned
}


def err3()
{
	f int
	{
		f int = 8
	}
	g = f;//this f is still not assigned
}

def ok2()
{
	f int
	{
		f = 8
	}
	g = f;//this f is still assigned :)
}

~~~~~
//##69. DefoAssignment - 1. for loops dont result in defo assignment

def fails() int
{
	j int
	for(a in [1,2,3]) {
		j=9
	}
	return j
}

~~~~~
//##69. DefoAssignment - 1. for funcs and try catch declare args

def ok1() int{
	for(a in [1,2,3])	{
		return a
	}
	return 7
}

def ok2() int{
	for(xxx = 9; xxx < 6; xxx++)	{
		return xxx
	}
	return 7
}

def ok3(x int) int{
	return x
}

ok4 = def (x int) int{ return x}//lambda form

def ok5(x int) {
	try{ x = 9 }
	catch(xa Exception)
	{
		ooh = xa
	}
}

~~~~~
//##69. DefoAssignment - 1. misc if missing var cos fwd ref its ok

open class Gen<X>(~x X){}

class Chil(f (double) float ) extends Gen< (double) float >(f)
{}

class Two
{
	def sss() int{ return c;} 
	c int = 9;
}


~~~~~
//##69. DefoAssignment - 2 - nested blocks

def foo1()
{
	f int
	{f = 7 }
	g = f
}

def foo12()
{
	f int
	try(null)
	{f = 7 }
	g = f
}

def foo2()
{
	f int
	{{f = 7 }}
	g = f
}

def xxx() => true; 
def foo3() {
	f int
	if(xxx()) {{f = 7 }}
	else     {{f = 7 }}
	g = f
}

~~~~~
//##69. DefoAssignment - 2 - stuff must be set
def fooFail1()
{
	f int
	try{ f= 6 } finally {  }
	g = f//ok
}


def fooFail2()
{
	f int
	try{ f= 6 }
	finally {  }
	g = f//ok
}

def fooFail3()
{
	f int
	try{ f= 6 }
	catch(e Exception) {}
	g = f//no f for you!
}

def foook()
{
	f int
	try{ f= 6 }
	catch(e Exception) {}
	finally{f= 6}
	g = f
}

def fooFail5()
{
	f int
	try{ f= 6 }
	catch(e Exception) {f= 6 }
	g = f
}

def fooOK() {
	f int
	try{ f= 6 } catch(e Exception) { f= 6 }
	g = f
}

def fooOK2() {
	f int
	try{ f= 6 } finally {  f= 6}
	g = f
}

def fooOK3() 
{
	f int
	try{ f= 6 }catch(e Exception) { f= 6 }
	g = f
}

~~~~~
//##69. DefoAssignment - 4. oh yeah, lambdas
def xxx() => 5>6
fail = def () void { f int; if(xxx()){ f=6; } else {  }; g=f  }
ok = def () void { f int; if(xxx()){ f=6; } else { f=7 }; g=f  }

~~~~~
//##69. DefoAssignment - 5. misc

def ok() int
{
	f int
	sync{f=6}
	return f
}

def ok2() int
{
	f int
	try{ f=6} finally{ g=3 }//this is actually a slightly simplified expasion of the above
	//this is ok, it will be initialized, or somehow
	//an exception thrown and rethrown, thus invalidating the useage below
	return f
}

~~~~~
//##70 return or exception in if chain gets ignored

def doings1() String{
	g int;
	something=false
	if(something){
		g=7;
	}
	else{
		return ""+5//if returns then g should be set
	}
	return ""+g
}

def doings2() String{
	g int;
	something=true
	if(something){
		g=7;
	}
	else{//exception variant
		throw new Exception("")
	}
	return ""+g
}

def doings3() String{
	g int;
	something=false
	something2=false
	if(something){
		g=7;
	}
	else{
		if(something2){
			return ""+5//if returns then g should be set
		}
		else{
			g=7;
		}
	}
	return ""+g
}

def doings4() String{
	g int;
	something=false
	if(something){
		g=7;
	}
	else{
		return ""+g//error as not initalized
	}
	return ""+g
}

~~~~~
//##71 return or exception in try catch chain gets ignored
//as above but with try catch
def doings1() String{
	g int;
	something=false
	try{
		g=7;
	}
	catch(e Exception){
		return ""+5//if returns then g should be set
	}
	return ""+g
}


def doings2() String{
	g int;
	something=true
	try{
		g=7;
	}
	catch(e Exception)
	{//exception variant
		throw new Exception("")
	}
	return ""+g
}


def doings3() String{
	g int;
	something=false
	something2=false
	try{
		g=7;
	}
	catch(e Exception){
		try{
			return ""+5//if returns then g should be set
		}
		catch(e Exception){
			g=7;
		}
	}
	return ""+g
}


def doings() String{
	return "";
}

~~~~~
//##72 refs always start off assigned

def aaa( ) =>  {
	a String:
	a //ok because a is created as a ref
}

def aaa2( ) =>  {
	a String
	a //not ok because a is created as a ref
}


def doings() => "hi" 

~~~~~
//##73 assign existing non eq

def doings(){
	a int
	b String
	
	a+=1
	b+=""//may not have been assigned

	a
}

~~~~~
//##74. bug - ensure that first arg in dot op is processed correctly

def tt() => false

def doings(){
	a String
	if(tt()){
	}else{
		a = "no"
	}
	
	f = a.length()
	
	"" 
}

~~~~~
//##75. bug used to think call inside if was unset
 
def tt() => false

def doings(){
	a String
	if(tt()){
		a = "hi"
		g=a//used to think it as unset
	}else{
		a = "no"
	}
	
	"" + a
}
 