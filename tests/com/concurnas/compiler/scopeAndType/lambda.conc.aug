//##69. Lambda - creation def 1

xxx (int) int = 69 //ensure we force the correct def assignment
xxx2 (int) int = def (x int) int { return x+3; }
xxx3 (int) int = 69


~~~~~
//##69. Lambda - creation def 2
//prims
x (double) double = def (x float) float { return x+3; } //OK
x2 (double) double = def (x boolean) boolean { return x; }//FAIL, wrong types

~~~~~
//##69. Lambda - creation def 3 - ensure subbj correctly
//objects
x (Object) void = def (x String) void {  } //OK, as subtype
y (String) void = def (x Object) void {  } //FAIL, not subtype
//
open class A{}
class B extends A{}

z1 (A) void = def (a A) void {}//OK
z2 (B) void = def (a B) void {}//OK
z3 (A) void = def (a B) void {}//OK
z4 (B) void = def (a A) void {}//FAIL

~~~~~
//##69. Lambda - creation def 4 - arrays, as args, 
a (int[2]) int[6]? = def (a int[2]) int[6]? {  return null; } //OK
a2 (int[2]) int[]? = def (a int[1]) int[]? {  return null; }//FAIL as array levels dont mathc
a3 (int[2]) int[6]? = def (a int[2]) int[5]? {  return null; }//FAIL as array levels dont mathc

~~~~~
//##69. Lambda - creation def 5 - arrays of funcs -> wow!

some1 lambda[2] = new lambda[2,2]
some lambda[] = [def (x int) double { return x;} def (x int) int { return x;} def (x float) int { return 55;} ]
some3 ((float) double) [] = [def (x int) double { return x;} def (x int) int { return x;} def (x float) int { return 55;} ]
stuff float[] = [ 1 2 3 5.4f]

~~~~~
//##69. Lambda - creation def 5.2 - generics parameters 
open class Gen<X>(~x X){}

class Chil(f (double) float ) extends Gen< (double) float >(f)
{}

child = new Chil( def ( x double) float { return 4.5f;});
ff (double) float = child.getX();


~~~~~
//##69. Lambda - creation def 6 - lambda catch all

a lambda = def (a int) int { return a**a as int; }
b = a as lambda
c (int) double = b as (int) int
z = b as Object
z2 = (b as Object) as (int) double
y = z as lambda
x = c as Function1<Integer, Double>
xxx="" + c(8)

~~~~~
//##69. Lambda - creation def 7 - object is master of all
d Object = def (a int) int { return a**a as int } //TODO: this needs to be a bit better fleshed out


~~~~~
//##69. Lambda - currying 1 - curry existing lambda
masterNoArg2 = def () doulble { return 4.3; }//FAIL - CANNOT RESOLVE NAME
fail2 = def (iint) double { return 4.3; }    //FAIL - CANNOT RESOLVE NAME

masterSimple = def (a int, b int, c int) int { return a+b+c; }
child (int, int) int = masterSimple&(? int, 66,? int)
child2 () int = child&(2,3)
childweirdo (int, int, int) int = masterSimple&(? int, ? int, ? int)
childweirdo2 (int, int, int) Object = masterSimple&(? int, ? int, ? int)

childFail (int, int, int) Object = masterSimple&(? int, ? int, ? Object) //FAIL wrong type as well
childFail2 (int, int, String) Object = masterSimple&(? int, ? int, ? int) //FAIL wrong type

masterNoArg = def () double { return 4.3; }
noChilldArg () double = masterNoArg&()

noChilldArgarrFAIL () double [] = [masterNoArg&() masterNoArg&() masterNoArg&()] //wrong syntax! this would be an array of funcfers which themslves return double arrays
noChilldArgarr (() double) [] = [masterNoArg&() masterNoArg&() masterNoArg&()]

/* TODO:
dd double = 0.0
for( funcref  in noChilldArgarr)
{
	dd += funcref()
}
*/

~~~~~
//##69. Lambda - currying 11. - curry chain
masterchain (int, int, int) int = def (x int, y int, z int) int { return x+y+z; }
child () int = masterchain&(1,? int, ? int)&(2,? int)&(3)


~~~~~
//##69. Lambda - currying 2 - curry existing function - simple case
def existing(x int) int { return x **x as int; }
child () int = existing&(55)

~~~~~
//##69. Lambda - currying 2.1 - simple case
masterchain (int, int, int) int = def (x int, y int, z int) int { return x+y+z; }
child () int = masterchain&(1,? int,?  int)&(2,? int)&(3)

innaproperiate int =77;
childFail () int = innaproperiate&(1,? int,?  int)&(2,? int)&(3) //FAIL

//find function
def masterchain2(x int, y int, z int) int { return x+y+z; }

child4 () int = masterchain2&(1,? int, ? int)&(2,? int)&(3)

//find variable, then look at function [in search block unit]

~~~~~
//##69. Lambda - currying 2.2 - avoid masking

def mask() boolean { return false; }
mask = def () int { return 55; }//FAIL variable masks funcdef, but this itself gennerates error
child5 () int = mask&()



~~~~~
//##69. Lambda - currying 2.3 - search upper nest
/*
ensure search order is respected:
arg, then funcdef
field, then method | superclass field, then method
global field, then method

imported filed then function
*/



masterchain (int, int, int) int = def (x int, y int, z int) int { return x+y+z; }

{
	child () int = masterchain&(1,? int, ? int)&(2,? int)&(3)
}


def top(x int) double { return x+6.0; } 

def ff( top (int) int ) 
{
	res int = top(6)//takes arg
}

class HasTop
{
	def top(x int) boolean { return false; } 

	def ff(  ) 
	{
		res boolean = top(6) //takes class def
	}
}

class FieldAndMeth
{
	~ffo ((int) boolean)?;

	def ffo(s int) int { return s+s; } //masks the method ref
	
	def main(str String[])
	{
		res boolean  = ffo??(5); //field first then method
	}
}

~~~~~
//##69. Lambda - currying 2.4 - search upper nest layered
class Nestor{

	~foo ((int) int)?;

	class Neste
	{
		def doing()
		{
			got int = foo??&(? int)(5)
		}
	}	
}


~~~~~
//##69. Lambda - currying 3 - curry existing method on object
s = "";
funcRef (float) String = s.valueOf&(? float)
res  String = funcRef(5.6f)


~~~~~
//##69. Lambda - currying 4 - curry existing method on class
open class Moi
{
	def foo(x int) double
	{
		return x+6.0
	}

	def other() (int) double
	{
		exp (int) double = this.foo&(? int)
		return foo&(? int)
	}
}

class BadKid
{
	def other2() (int) double { return foo&(? int); }
}

class Kid extends Moi
{
	def other2() (int) double {
		exp (int) double = this.foo&(? int) 
		exp2 (int) double = super.foo&(? int) 
		return foo&(? int); 
	}
}


~~~~~
//##69. Lambda - currying 5 - setting class members etc

bob lambda

class ZZ(~x (int) String)
{
	
}	

ref = def (x int) String { return "hi"+x;}

zz = new ZZ(ref);

zz.x = def (x int) String { return "hi"+x;}
zz.x = ref
zz.setX( def (x int) String { return "hi"+x;} ) 
found (int) String = zz.getX()
found2 (int) String = zz.x


bob = ref


~~~~~
//##69. Lambda - currying 6 - generics

class Cls<X>
{//i think this is enough, coloring clearly works
	~ss ((X, int) X)?;
	~y X?;
}

my = new Cls<String>()
my.ss = def (xx String, x int) String { return xx + "" + x; } 

//and make sure the type can be this as well
class Gen<X>(~x X) {}
mine = new Gen<(int) String>(def (s int) String { return ""+s;} )

~~~~~
//##69. Lambda - erasure 1 - simple

class My
{
	def x(arg (int) int) { }

	def x(arg (double) int) { }//FAIL due to same erasure
	
	def x(arg (double, double) int) { }//but this guy is ok
	
	//also constructoors
	this(arg (int) int) { }
	this(arg (double) int) { }//FAIL due to same erasure
	this(arg (double, double) int) { }//but this guy is ok
}

~~~~~
//##69. Lambda - erasure 2 - simple overrides

open class My
{
	def x(arg (int) int) { }
}

class BadChild extends My {
	def x(arg (int) int) { }//FAIL, needs override keyword
}

class GoodChild extends My {
	override def x(arg (int) int) { }//OK
}

~~~~~
//##69. Lambda - erasure 3 - simple abstract

open class My
{
	def x(arg (int) int) void
}

class GoodChild extends My {
	def x(arg (int) int) { }//FAIL, needs override keyword
}

class BadChild  extends My {
	override def x(arg (int) int) { }//FAIL, because abstract
}

~~~~~
//##69. Lambda - erasure 3 - super has thing already
open class My
{
	def x(arg (int) int) { }
	def x2(arg int) { }
}

class Child2 extends My {
	def x(arg (double) int) { }//FAIL, erasure problems
}

class Child3OK extends My {
	def x(arg (double, int) int) { }//FINE
}

class Child3OKAlso extends My {
	def x2(arg (int) int) { }//FINE also
}

~~~~~
//##69. Lambda - generics mist - amazing this works
open class Gen<X> {}

open class Par
{
	def x(arg Gen<(int) int>) { }
}

class Child3 extends Par
{
	override def x(arg Gen<(int) int>) { }
}

~~~~~
//##69. Lambda - equals is always false - should this warn?
x = def (x int) int {return x+2; }
y = def (x int) int {return x+2; }
no boolean= x==y //JPT: warning?
no2 boolean= x &==y //JPT: also warn

~~~~~
//##69. Lambda - cast

x = def (x int) int {return x+2; }

y = x as (int, double) int ///FAILS - omg i didnt write any code for this it just works!
yok = x as (int) int ///fine args match
yok2 = x as (int) Object ///fine args match as well
yok2 = x as (int) String ///FAIL - thisis better than scala
nice = (new Object()) as (int) String //fine! - object could be a functype
nice = (new String()) as (int) String //FAIL - no way string could be this!

~~~~~
//##69. Lambda - is isnot
x = def (x int) int {return x+2; }

y = x is (int, double) int ///FAILS - omg i didnt write any code for this it just works!
yok = x is (int) int ///fine args match
yok2 = x is (int) Object ///fine args match as well
yok2 = x is (int) String ///FAIL - thisis better than scala
nice = (new Object()) is (int) String //fine! - object could be a functype
nice = (new String()) is (int) String //FAIL - no way string could be this!

~~~~~
//##69. Lambda - example
funcref = def () int { return 4; }

dd double = 0.0
for( funcrefola  in [funcref])
{
	dd += funcrefola()
}

~~~~~
//##69. Lambda - FunctionN type...
a Function2<Integer, Integer, String>? = null //can set funcref to null and these are commutable....
res String =a??(2,3)// translate the function2 into a FuncType
a = def (a int, b int) String { return ""; }//now a cannot be null
res  =a(2,3)// translate the function2 into a FuncType

voidfunc Function3<String, String, String, Void>? = null
voidfunc = def (a String, b String, c String) void {} 

sth () void = (new Object()) as Function0<Void> //interchangable, also Void return type

oops Function1<Void, Void>? = null //FAIL - this is not permitted, void is not an input parameter

//nested gen types in Function2

nested Function2<Integer, (int) int, String> = def (x int, y (int) int) String {return "" } 

~~~~~
//##69. Lambda - of a inner function...
def iamthemaster() (int) int
{
	scale = 3
	def iamthemaster(x int, y int) int
	{
		return x*y * scale;
	}
	
	return iamthemaster&(6,? int)//refers to localinstance
}

~~~~~
//##69. Lambda - misc voids
def a() (int) int
{
	return def (x int) void { return x+ 2 } ;
}



~~~~~
//##69. Lambda - call externally a function
//JPT: i had to do some mega hacking here, it's ugly sorry
class JJ
{
	public xxx = def (x int) int { return x+1; } //TODO: lambda is not getting picked up
	//def aa() int {return 7; }//wrong
}

(new JJ()).xxx(4)//problem, cannot call lambda externally...


~~~~~
//##69. Lambda - make sure both maskings are present
class Gumpy1{
	def xxx() int { return 6; }
	xxx = def () int { return 6; }
}

class Gumpy2{
	xxx = def () int { return 6; }
	def xxx() int { return 6; }
}

~~~~~
//##69. if defo exception, then u can ignore that fact that it doesnt return properly

def  getBytecodeType() String{
	throw new RuntimeException("getBytecodeType not callable");
}


//JPT: you should add a realy complex map filter type etc test here - also do the bytecode for it


~~~~~
//##70. rhs of dot operator is ok
class Child{
	def aFun() int { return 66; }
	def funRefRet() () int { return aFun&() }
}

def doings() String {
	child = new Child()
	return "" + child.funRefRet()()
}
