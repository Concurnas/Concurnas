//##1. declare simple

def decare() {
	a  int: = 9 //Ref[int]
	a2 int:     //Ref[int]
	b  := 9     //Ref[int]
	//b2 :        //Ref[Object]
	
	c int::=9   //Ref[Ref[int]]
	c2 int::=9  //Ref[Ref[int]]
	d  ::=9     //Ref[Ref[int]]
	//d2 ::       //Ref[Ref[Object]]
}

~~~~~
//##1.b declare simple - check

def decare() {
	a  int: = 9 //Ref[int]
	b  := 9     //Ref[int]
	c int::=9   //Ref[Ref[int]]
	d  ::=9     //Ref[Ref[int]]
	
	a2 = a
	b2 = b
	c2 = c
	d2 = d
	
	a3 = a:
	b3 = b:
	c3 = c:
	d3 = d:
	
	a4 Integer = a
	b4 Integer = b
	c4 Integer: = c
	d4 Integer: = d
	
	a5 Integer: = a:
	b5 Integer: = b:
	c5 Integer:: = c:
	d5 Integer:: = d:
	
	a6 int: = a:
	b6 int: = b:
	c6 int:: = c:
	d6 int:: = d:
	
	a7 = a
	b7 = b
	c7 : = c//wrong?
	d7 : = d
	c72 :: = c
	d72 :: = d
	
	z = [a is int:, b is int:, c is int::, d is int::]
	//implicit...
	z2 = [a2 is Integer, b2 is Integer, c2 is int:, d2 is int:]
	z3 = [a3 is int:, b3 is int:, c3 is int::, d3 is int::]
	//explicit...
	z4 = [a4 is Integer, b4 is Integer, c4 is int:, d4 is int:]
	z5 = [a5 is int:, b5 is int:, c5 is int::, d5 is int::]
	z6 = [a6 is int:, b6 is int:, c6 is int::, d6 is int::]
	z7 = [a7 is int:, b7 is int:, c7 is int::, d7 is int::, c72 is int::, d72 is int::]
}

~~~~~
//##2. array of ref cases fail

def refFailCases(){
	afail  int:[] = 9//fail  
	afail2  int:[] = [9 9]//fail  
}


~~~~~
//##3. array of ref cases ok
//of array of ref --> Ref[int][]
z := 9
x = [z,z]
a  Integer:[] = [z: z] 
a1  Integer[] = [z z] 
a2 int:[]

~~~~~
//##4. ref of array ok

def ars(){
	//ref of array --> Ref[int[]]
	a  int[]: = [9 9] 
	a2 int[]:     
	b  := [9]     
}

~~~~~
//##4.b ref of array fail
c  []: = [9,] //error consumes two lines.. doesnt matter this is invalid syntax anyway


~~~~~
//##5. ret from async function

def fromFunc() {
	func = def () int { return 5}

	f = func()!
	f2 := func()!
	f3 int:= func()!
	//^all same
}


~~~~~
//##5.a ret from async function - defined already fail

def fromFunc() {
	func = def () int { return 5}

	f = func()!
	f := func()! //ok because we are replacing the ref itself
	f int:= func()!
	//^all same
}

~~~~~
//##6. setting

def setter(){
	a := 7
	a=6
	a=9
	//no listners so no history preserved
}

~~~~~
//##7. useage

def passes(){
	a int:
	a=9
	
	b = a      //int -> a.get()
	
	c := a     //int: -> a.get() - as ref again
	c2 int:= a //as above
	
	d = a:       //d -> ref[int] - copy of pointer to a
	d2 int:= a:  //as above
	d3 := a:     //as above
}

~~~~~
//##8. instanceof on refs is ok

def doings() String{

	a int:= 6
	
	b = a  //b is int
	b = a  //b is int still
	
	d := a //d is int:
	d = a //d is still int:
	
	c = a: //c int:
	c = a //c is still int:

	return "" + [ a is int:, d is int:, c is int: ]
}

~~~~~
//##9. lhs must be ref
g = 5: //this is ok now as g is created as int:

~~~~~
//##10. more simple assignments

a int: = 6   //int:
b int = a 	 //int
c = a //int
d int: = a: 
e int = a://error as forced by the :

~~~~~
//##11. as above but checking the types

def doings() String{
	a int: = 6   //int:
	b int = a 	 //int
	c = a //int
	d int: = a: 
	return "" + [ a is int:, d is int: ]//ok here, but real check is in bytecode
}

~~~~~
//##12. some random stuff which should be ok

a int: = 6   //int:
b int = a 	 //int
c = a        //int
d := a       //int:
//e int = a:   //ERROR
f int:: = a! //ref to a ref

ff = def () int{ return 6; }
c2 = ff()!  //int:
//c3 = ff()!:  //int: - ERROR

~~~~~
//##13. lhs no ref

ax = [8!, 8!]
z = ax[1]:
ax[0] = 9
ax[0]: = 10! ///ok

~~~~~
//##14. this is permitted but i think it's weird... cannot really prevent it

a := 7 
b :=8

a = b
a = b:

~~~~~
//##15. this is permitted but i think it's weird... cannot really prevent it

def doings() String{
	g = 6! //makes ref
	h = g
	return "" + [ g is int:, g: is int:] //int: is a problem? like int vs Integer
}

~~~~~
//##16. autocast from float to int

def doings() String{
	g = 6! //makes ref
	g = 6. //can this be done?	
	//Ref[Int], set a Ref[Float] to it...

	return "" + g
}

~~~~~
//##17. use single ref as function argument and in currying

def funcok(a int:) int{
	return 4
}

def funbroken(a :) int{
	return 4
}

def funbroken2(a ::) int{
	return 4
}

def doings() String{
	a : = 8
	b : =new Object()
		
	curry1 = funcok&(? int:)
	curry2 = funbroken&(:)
			
	return "" + [funcok(a:), funbroken(b:), funbroken2(b:), curry1(a:), curry2(b:)]
}

~~~~~
//##18. ref logic checks

getHC = def (a int:) int{ return System.identityHashCode(a:); }

def doings() String{
	ref1 := 5
	//ref2 := 6
	
	r1Orig = getHC(ref1:)
	
	child1 := 0
	child2 := 0
	
	c1Orig = getHC(child1:)
	c2Orig = getHC(child2:)
	
	//same ref but value changed
	child1 = ref1
	c1AfterAssign = getHC(child1:)
	t1 = "" + [c1Orig == c1AfterAssign, child1 == 5]
	
	//diff ref and == 5
	child2 = ref1:
	c2AfterAssign = getHC(child2:)
		
	t2 = "" + [c2Orig <> c2AfterAssign, c2Orig==r1Orig, child2 == 5]
	
	//new var should == to same referece
	newChild1 := ref1
	nc1Orig = getHC(newChild1:)
	t3 = "" + [nc1Orig==r1Orig, newChild1 == 5]
	
	//new var, diff reference
	newChild2 := ref1
	nc2Orig = getHC(newChild2:)
	t4 = "" + [nc2Orig <> r1Orig, newChild2 == 5]
	
	return "" + [t1, t2, t3, t4];
}

~~~~~
//##19. double ref to single

def doings() String{
	a:: = 9
	c = a
	b = a: //b is int::
	
	
	return "" + [b is int::, c is int:]
}

~~~~~
//##20. ops on ref ok 

def doings() String{
	a int: = 6   //int:
	
	res1 int = a:get() + 7 // where used, autoconvert to a.get()
	
	return "" + res1
}

~~~~~
//##21. ops on ref ok -fail

def doings() String{
	a int: = 6   //int:
	
	res1 int = a:getasdasd() + 7 //a fail
	
	return "" + res1
}

~~~~~
//##22. use, auto extract the get operation to unbox

def plus1(a int) int { return a+1; }

def doings() String{
	a int: = 6   //int:
	
	//where used, autoconvert to a.get()
	r1 = plus1(a)
	r2 = a + 7 
		
	return "" + [r1, r2]
}

~~~~~
//##23. async blocks

def doings() String{
	a int: //note that these are always initialized
	{a = 9}!
	return "" + a
}

~~~~~
//##24. can assign to final as shell

def doings() String{
	val a int: = 99
	a = 9 //ok
	a = (9)! //not allowed
			 
	return "" 
}

~~~~~
//##24.b can assign to final as shell

def doings() String{
	val a int:: = 99
	
	a = 9 //ok
	a = (9)! //ok
	a = ((9)!)! //not ok
			 
	return "" 
}

~~~~~
//##25. ensure no ignore unboxing

def doings() String{
	a: = 9
	c = a
	
	x  = c as int
	
	return "" + x // + [b is int::, c is int:]
}

~~~~~
//##26. more of prev case

def doings() String{
	a:: = 9
	c int: = a
	c2  = a
	
	b int:: = a: //b is int::
	b2 = a: //b is int::
	
	x boolean = c is int:
	x2 boolean = c2 is int:
	x3 boolean = b is int::
	x4 boolean = b2 is int::
	
	return ""// + [b is int::, c is int:]
}

~~~~~
//##27. instanceof moan

def doings() String{
	a double: 
	return "" + [a is double:, a isnot int:]
}

~~~~~
//##28. cast checks

def doings() String{
	ref1 := 5
	cont Object = ref1: as Integer //cast has no respect for anything! :)
	cont2 Object = ref1 as Integer
			
	return ""
}

~~~~~
//##29. cannot cast from ref to obj when locked in

from com.concurnas.lang.precompiled.RefHelper import getIntegerRef, getIntegerRefAsObject

def doings() String{
 	tg = 9
	refasObj1 Object = getIntegerRefAsObject(7)
	
	refasObj1IntRef Integer = refasObj1 as Integer://no
	
	return ""
}

~~~~~
//##30. math on locked refs is ok

def doings() String{
	a int: =9   //int:
	e = a: + 8//yeah this is ok
				
	return ""
}

~~~~~
//##31. arrays of ref type must be init

def doings() String{
	a int:[]
	
	z = [a,a,a,a,a] //invalid as a has not been instsantiated
	
	return ""
}


~~~~~
//##32.a ambigous - ambig case

//correctly stated as being ambigious :)
def xxx(a String, b String:){ }
def xxx(a String:, b String ){ }

def doings() String{
	xxx("", "")
	return ""
}

~~~~~
//##32.b ambigous - non ambig case

def xxx(a Object){ }
def xxx(a :){ } //these are not ambigious

def doings() String{
	xxx( "" )
	return ""
}

~~~~~
//##33. arrays of refs casting

def doings() String{
	//orig Number[] = [1 new Integer(2)]
	ok1 Integer:[] = [1! 2!]
	ok2 Number:[] = [1! 2!]
	fail1 Integer[] = [1! 2!]
	fail2 Number[] = [1! 2!]
	
	return ""
}

~~~~~
//##33.b arrays of refs casting - part 2

def doings() String{
	
	xxx = [1! 2!] as Object[] //ok
	no = [1! 2!] as Number[] //not ok
	
	return ""
}

~~~~~
//##34. array of ref - some op fail more
def doings() String{
	
	asObj Object = [1! 2!]
	ok = asObj as Number:[] //type info not lost	
	
	asObjAr Object[] = [1! 2!]
	fail = asObjAr as Number:[] //cannot do this as we've lost the type info
	
	return "" 
}


~~~~~
//##35. this should return a ref

def okla( ) String: {
	 "hi"!
}

def doingsa() {
	d=""+ okla()
	"" + d 
}

def functo() int:{ return 454! }

def doings() String{
	xxx := functo()
	r = "" + [xxx:get(), doingsa()] 
	assert r == "[454, hi]"
	return r
}

~~~~~
//##36. check subtype

def getRef() int:{
	ret int: = [5 5 5 5]! //not good, it aint an array
	return ret
}

~~~~~
//##36.b check subtype

def getRef() int:{
	return [5 5 5 5]! //not good, it aint an array
}

~~~~~
//##37. inc on raw not ret copy respects refness

def doings() String{
	a := 5
	c = (a:)++ //ok but pointless
	c = (a:#)++ //ok but pointless
	return "" +c //+ [b, c]
}

~~~~~
//##38. index op not valid on ref

def doings() String{
	a := 5
	
	x15 = [0 0 0 0 0 5 0 0][a:]
	
	return "" + x15
}

~~~~~
//##39 be careful with array ref types

def getRef() int[]:{
	f int[]: = [5! 5! 5! 5!] 
	return f
}


def doings() String{
	f int[]: = [5! 5! 5! 5!] 
	return "" + f
}

~~~~~
//##40 asyncrefref levels

x = (8!)!

def doings() String{
	f1 = "" + x::
	f2 = "" + x:
	f3 = "" + x::://used to blow up - but now ok

	return "" +[f1, f2] 
}

~~~~~
//##41. create an empty obj ref but not like this

def doings() String{
	a : //invalid syntax for declaration of an object ref
	
	return ""
}

~~~~~
//##42. no refied types thus following cast is not possible

from java.util import HashMap

z  = (new HashMap<String, Integer>())!
o = (z as Object)

def doings() String{
	f = o is HashMap<String, Integer> //does not belong here but i wanted to test the formatting of the error message (i.e. does [?, ?] text now)
			
	return "" + f
}

~~~~~
//##42.b no refied types thus following cast IS possible

from java.util import HashMap

z  = (new HashMap<String, Integer>())!
o = (z as Object) //extracted

def doings() String{
	f = o is HashMap<String, Integer>: //should return true at runtime (go via ref checker as rhs is a ref)
	f2 = o is not HashMap<String, String>: //should return false at runtime (go via ref checker as rhs is a ref)
			
	return "" + [f, f2]
}

~~~~~
//##42.c no refied types thus following cast IS possible - null not ref

from java.util import HashMap

o = null

def doings() String{
	f = o is HashMap<String, Integer>: //should return true at runtime (go via ref checker as rhs is a ref)
	f2 = o is not HashMap<String, String>: //should return false at runtime (go via ref checker as rhs is a ref)
			
	return "" + [f, f2]
}

~~~~~
//##42.d no refied types thus following cast IS possible - null

from java.util import HashMap

o = ('')! //double check created as Object:
o2 := ('')! 
o3 Object:= ('')! 

def doings() String{
	f = o is HashMap<String, Integer>: //should return true at runtime (go via ref checker as rhs is a ref)
	f2 = o is not HashMap<String, String>: //should return false at runtime (go via ref checker as rhs is a ref)
			
	f = o2 is HashMap<String, Integer>: //should return true at runtime (go via ref checker as rhs is a ref)
	f2 = o2 is not HashMap<String, String>: //should return false at runtime (go via ref checker as rhs is a ref)
			
	f = o3 is HashMap<String, Integer>: //should return true at runtime (go via ref checker as rhs is a ref)
	f2 = o3 is not HashMap<String, String>: //should return false at runtime (go via ref checker as rhs is a ref)
			
	return "" + [f, f2]
}


~~~~~
//##43. cast checks -sac

def takesO(arg Object:) {}

def doings() String{
	a := 9
	//all these should fail:
	o Object: = a //ok
	o = a as Object: //ok
	takesO(a:)//ok (though just calling a 
	return ""
}

~~~~~
//##44. simple case

def doings() String{
	ok Object: = ""
	notok Object: = ""! //actually this is fine
	return ""
}

~~~~~
//##45. asign exsiting ref levels

x int::

def doings() String{
	x ::=8//ok
	x :=8//ok
	x =8//ok
	x :::=8//should blow up
	
	return "" 
}


~~~~~
//##46. puzzle 1

def doings() String{
	x Object:
	x = "newRef2" //this is ok because Stirng is a subtype of object
	x := "newRef2" //this is ok as well, we make an Object ref with the String as the arg to satify the lhs requirement
	x := "newRef2"! //this blows up because on the rhs we've made a String: (and that cannot goto an Object ref - no this is fine
	x = "newRef2"! //same as above but with implicit overwrite of Object ref (cos rhs is locked as ref)
		
	return "" +  x: 
}

~~~~~
//##46.b puzzle 2 - a tricky one where ref is of type Object:

class MyClas{
	public var ~x Object: = "orig" //as Object - its ok
	override equals(o Object) boolean {return true;}
	override hashCode() int {return 6;}
}

mc = new MyClas()
//orig := mc.x

def doings() String{
	mc.x := "newRef2"! //should blow up
	mc.x := "newRef2" //is fine
		
	return "" +  mc.x: //+ [nomatch] + "" + mc.x:
}

~~~~~
//##47. cannot ref assign something that is not a ref

def doings() String{
	z  = ["initial"!]
	
	z := ["newValue"!] //this is a new ref
		 
	return "" 
}

~~~~~
//##48. obj: arg defo not upcast from int:

def fSame(x2 :) boolean {
	//this is not going to get entered into
	y Object:= x2
	return (x2: &== y:)
}

def fSame2(x2 int:) boolean {
	//more complaints from this
	y Object:= x2
	return (x2: &== y:)
}

def doings() String{
	return "" + [fSame(7!), fSame2(7!)] //seems fine
}

~~~~~
//##49. correct bevahour for ref creation

x = 8!
x2 = (8!)!

def doings() String{
	ok = 8: //explicit ish ref creation
	ok2 = 8:://upgraded to int::
	ok3 = x:
	ok4 = x:: //upgraded to int::
	ok5 = x2: //extracted to int:

	"" + [ok, ok2, ok3, ok4, ok5]
}

~~~~~
//##50. this async ref syntax is not permitted

def doings() {
	x = [1:1:] //-> [AsyncRef  AsyncRef] //its fine 
	"" 
}

~~~~~
//##51. cusr ref type must match

import com.concurnas.runtime.ref.RefArray 

def checkIsA(arg int:RefArray) {
	xx int:RefArray = 12: //nope
	xx2 int:RefArray : = 12:: //nope2
	xx is int:RefArray
}

def doings() => "hi"

~~~~~
//##52. cannot get from a raw Ref type

class MyCls(an Object){}

def ff(a Object) => "k"

def doings() {
	
	xs = [1:, 1:]
	output := ""
	cnt:=0
	every(xs){ 
		h=999
		stuff = changed.getChanged()
		got = stuff[0]: 
		got2 = stuff[1] //not permitted
		
		stuff[1]++ //no
		++stuff[1] //no
		ha = got * got //no
		
		f int = got //not permitted
		//f2 int = got2
		
		output += "" + f
		output += "" + got //not permitted
		output += "" + ff(got) //-ok it can be extracted as object
		output += "" + MyCls(got) //not permitted
		cnt++ //this is fine because int:Local has set and get
	}
	
	trans{
		xs[0] = 98
		xs[1]:close()
	}
	
	await(cnt ; cnt == 1)
	
	"" + [output]
}

~~~~~
//##52.b cannot get from a raw Ref type as above

def doings() {
	
	xs = [1:, 1:]
	
	output :=""
	cnt :=0
	
	onchange(xs){ 
		h=999
		stuff = changed.getChanged()
		got = stuff[0]: 
		got2 = stuff[1]:
		k = {got ->
				got}//doesnt like this either
		output += "" + [stuff.length, 
						got, //not permitted
						not(got:isClosed()), 
						got2:isClosed()]
		cnt++
	}
	
	trans{
		//System.err.println("109.23")
		xs[0] = 98
		xs[1]:close()
		//System.err.println("1.88")
	}
	
	await(cnt ; cnt == 1)
	
	"" + [output]
}

~~~~~
//##52.c cannot get from a raw Ref type as above MORE

from com.concurnas.lang.precompiled import RefHelper

cc  :::= "me not ref"
def uppy1(a com.concurnas.runtime.ref.Local<com.concurnas.bootstrap.runtime.ref.Ref<Object>>) => a
b com.concurnas.runtime.ref.Local<com.concurnas.bootstrap.runtime.ref.Ref<Object>> = 9
def doings() => "" +	[b, 
						uppy1(cc).getClass(),
						uppy1(cc):.getClass()]//this one is ok

~~~~~
//##52.d cannot get from a raw Ref type as above MORE MORE
def xxx() => true
cc  ::= "me not ref"
//returns Ref Ref in thes cases not Ref Local etc
def uppy(a com.concurnas.runtime.ref.Local<com.concurnas.bootstrap.runtime.ref.Ref<Object>>,		
	b com.concurnas.bootstrap.runtime.ref.Ref<com.concurnas.runtime.ref.Local<Object>>,		
	c boolean) 
	=> (a) if xxx() else (b:)//this should complain

~~~~~
//##52.e cannot get from a raw Ref type as above MORE MORE MORE
def xxx() => true
cc  ::= "me not ref"

def uppy(a com.concurnas.runtime.ref.Local<com.concurnas.bootstrap.runtime.ref.Ref<Object>>,		
	b com.concurnas.bootstrap.runtime.ref.Ref<com.concurnas.runtime.ref.Local<Object>>,		
	c boolean) 
	=> (a:) if xxx() else (b:) //you cant do this once because we can't get from a ref

def doings() =>""

~~~~~
//##52.F MORE COMPLAINTS
	def xxx() => true
cc  ::= "me not ref"

def uppy2(a com.concurnas.runtime.ref.Local<com.concurnas.bootstrap.runtime.ref.Ref<Object>>,		
	b com.concurnas.bootstrap.runtime.ref.Ref<com.concurnas.runtime.ref.Local<Object>>,		
	c boolean) 
	=> (a:) if xxx() else (b:)


def doings() {
	z = uppy2&(cc, cc, ? boolean) //not its ok when refering to refs, cos types were being incorrectly set
 	""  + [z(true), z(false)]
}	
	
~~~~~
//##52. g doesnt like this

from com.concurnas.lang.precompiled import RefHelper

b = RefHelper.getThingAsRef(12)//not permitted, as expected

def doings() {
	""	
}

~~~~~
//##53. more cases like the above

from com.concurnas.lang.precompiled import RefHelper

cc  :::= "me not ref"
//the next functions are expected to return the Object
def uppy1(a com.concurnas.runtime.ref.Local<com.concurnas.runtime.ref.Local<Object>>) => a
def uppy2(a com.concurnas.runtime.ref.Local<com.concurnas.bootstrap.runtime.ref.Ref<Object>>) => a //cannot do this
def uppy3(a Object::) => a

def doings() => "" +	[uppy1(cc), //ok
				uppy2(cc), //moans
				uppy3(cc)] //add extra ref level on cast operation

~~~~~
//##54. can create local for ref type

a int:com.concurnas.bootstrap.runtime.ref.Ref
a=9    //fail - cost setting to a current one
a := 9 //a ok - no set called so ok

def doings() => "" + (a as int:)

~~~~~
//##55. some errs

from com.concurnas.lang.precompiled import RefHelper

cc  ::= "me not ref"

def xxx(a Object::) => a //ok, others fail
def xxx2(a com.concurnas.bootstrap.runtime.ref.Ref<com.concurnas.bootstrap.runtime.ref.Ref<Object>>) => a
def xxx3(a com.concurnas.runtime.ref.Local<com.concurnas.bootstrap.runtime.ref.Ref<Object>>) => a
def xxx4(a com.concurnas.bootstrap.runtime.ref.Ref<com.concurnas.runtime.ref.Local<Object>>) => a

def doings() => "" +	[xxx(cc),//all ok
					RefHelper.actonref2(cc),	
					xxx2(cc),
					xxx3(cc),
					xxx4(cc)
					]

~~~~~
//##56. check ref type extraction errs in return statement

def ran() => false

def wally() int{
	a int:com.concurnas.bootstrap.runtime.ref.Ref = 12:
	if(ran()){
		return a //no, cannot get from above
	}
	else{
		b=a as int:
		return b //yes this is fine
	}	
}

def doings() {

	"" + wally()
}



~~~~~
//##57. for while loop from Ref type not permitted

from java.util import ArrayList

ar = new ArrayList<Integer>()
for(a in [1,2,3]){ ar.add(a) }

def wally() {
	a int:com.concurnas.bootstrap.runtime.ref.Ref = 12:
	one = for(ab in [1,2,3]){
		a//no!
	}
	
	two = for(ab in [1,2,3]){
		break a//no!
	}
	
	
	one = for(ab in ar){
		a//no!
	}
	
	two = for(ab in ar){
		break a//no!
	}
	
	[one, two]
}


def whila(){
	a int:com.concurnas.bootstrap.runtime.ref.Ref = 13:
	n1 = 0
	one = while(n1++ < 3){ a//no
	}
	n2 = 0
	two = while(n2++ < 3){ 
		if(n2 == 1){
			continue a//no
		}
		break a//no
  	}
	
	"" + [one, two]
}

def doings() {
	"" + wally()
}