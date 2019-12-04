//##1. this one is ok
from java.lang.Math import sin, round

mymatrix = [ 60. 120. ; 90. 360.]

def mysin(arg double, toRand boolean) => round(sin(arg)*100)/100.

calledD = 0
def getF() => calledD++; true

def doings(){
	res = mysin(mymatrix^, getF())
	
	"" + [res, calledD]
}

~~~~~
//##2. onyl one arg vec self
from java.lang.Math import sin, round

mymatrix = [ 60. 120. ; 90. 360.]

def mysin(arg double, toRand boolean) => round(sin(arg)*100)/100.

calledD = 0
def getF() => calledD++; true

def doings(){
	res = mysin(mymatrix^^, [false, true]^^)//no
	
	"" + [res, calledD]
}

~~~~~
//##3. all args same dimention
def mysin(arg double, arg2 double) => 'ok'

ve = [ 1 2. ]
ar = [ 1. 2; 3 4.]

def doings(){
	res = mysin(ve^, ar^)
	
	"" 
}

~~~~~
//##4. correct type to write back cast needed

from java.lang.Math import sin, round

Mvec = [ 60 120 ]

callmyseincnt = 0
def mysin(arg double, toRand boolean) => callmyseincnt++; round(sin(arg)*100)/100.

calledD = 0
def getF() => calledD++; true

def doings(){
	c1 = Mvec@
	c2 = Mvec@
	c3 = Mvec@
	c4 = Mvec@
	mysin(c1^, getF())//fine
	mysin(c2^^, getF())//nope
	r1= mysin(c3^, getF())//fine
	r2 =mysin(c4^^, getF())//nope
	
	"" + [r1, r2, c1, c2, c3, c4, callmyseincnt, calledD]
}

~~~~~
//##5. only one arg in ensted call may be of ^^ form

def fcall(prec String, arg1 String, arg2 int) => "fcall: {prec} {arg1} => {arg2}"

def outercall(an String) => "outer: {an}"

vec = ["a", "b"]

calledD = 0
def getF() => calledD++; calledD

def doings(){
	fcall('pre', outercall(vec^^)^^, getF())
	""
}

~~~~~
//##6. only vec array and lists

def fcall(prec String, arg1 String, arg2 int) => "fcall: {prec} {arg1} => {arg2}"

def outercall(an String) => "outer: {an}"

vec = ['a', 'b']

calledD = 0
def getF() => calledD++; calledD

def doings(){
	fcall('pre', 'david'^, getF())
	""
}

~~~~~
//##7. only vectorize certain places

def doings(){
	fail = [ 1 2 3 ]^
	""
}

~~~~~
//##8. expr element non self ref

def fcall(prec String, arg1 String, arg2 int) => "fcall: {prec} {arg1} => {arg2}"

def outercall(an String) => "outer: {an}"

vec = ["a", "b"]

calledD = 0
def getF() => calledD++; calledD

def doings(){
	vec5 = vec@
	vec6 = vec@

	r3 = fcall('pre', outercall(vec5^)^^, getF())
	fcall('pre', outercall(vec6^)^^, getF())
	
	"" 
}

~~~~~
//##9. Expression to vectorize has already been vectorized

def getF() => 100

def mysin(a double, an int) => a + an

def doings(){
	c4 = [ 1 2 ]
	"" +( mysin((c4^)^, getF())^ as int)
}

~~~~~
//##10. Expression to vectorize has already been vectorized vect func invoke

class MyClass{
	def anOp() => 22
}

myAr = [MyClass() MyClass() ; MyClass() MyClass()]

def doings(){
	res = (myAr^)^anOp()
	"" + res
}

~~~~~
//##11. Only array and lists can be vectorized vect func invoke

class MyClass{
	def anOp() => 22
}

myAr = MyClass()

def doings(){
	res = myAr^anOp()
	"" + res
}

~~~~~
//##12. Unable to find method with matching name func invoke

class MyClass{
	def anOp() => 22
}

myAr = [MyClass() MyClass() ; MyClass() MyClass()]

def doings(){
	res = myAr^anOpx()
	"" + res
}

~~~~~
//##13. only one self ref on func invoke vect chain

cnt = 0

class MyClass(val value = "ok"){
	def anOp() => cnt++; MyClass("anOp called")
	override toString() => "MyClass-{value}"
	def op2() => value.length()
}

myAr = [MyClass() MyClass()]

def doings(){
	res = myAr^^anOp()^^op2()
	"{cnt} {res} {myAr}"
}

~~~~~
//##14. Vectorized arguments must all be to the same degree

def getFunc(){
	def (a int, b int) => a + 100
}

myAr1 = [1 2 3 4]
myAr2 = [myAr1 ; myAr1]

def doings(){
	xx = getFunc()(myAr1^, myAr2^)
	"" + xx
}

~~~~~
//##15. When both elements of sublist array reference are vectorized, they must be vectorized to the same degree

origArray = [1 2 3 4 5 6 7 8 9 10]
ele = [ 0 1 0 1 0 2]
ele2 = [[ 0 1 0 1 0 2]]

def doings(){
	got = origArray[  (ele^) ... (ele2^) ]
	"" + got
}

~~~~~
//##16. op overload only one may inplace

origArray = [1 2 3 4 5 6 7 8 9 10]

def doings(){
	got = origArray^^ + origArray^^ //no
	"" + [origArray, got]
}

~~~~~
//##17. Vectorized Expression cannot appear on its own line

myAr = [ 1 2 3 4 ]

def doings(){
	myAr^ + 1//cannot be on its own
	"" + myAr
}

~~~~~
//##18. vectorized assignment lhs must be array or list

myAr = [ 1 2 3 4 ]
thing = [9]

def doings(){
	thing[0] += myAr^
	"" + myAr
}

~~~~~
//##19. when vect no direct

myAr = [ 1 2 3 4 ]
thing = [9]

def doings(){
	myAr = myAr^
	"" + myAr
}

~~~~~
//##20. all sides of operation need to be vectoried if arrays or lists

myAr1 = [ 1 2 3 4 ]

def doings(){
	myAr1^^ + myAr1 //no, rhs must be vectoried - ah but it doesnt as it gets auto vectorized
	"" 
}

~~~~~
//##21. rhs must be vectoried if list ass
//gets auto vectorized
myAr1 = [ 1 2 3 4 ]
myAr2 = [ 10 20 30 40 ]
myAr3 = [ 1 2 3 4 ]
myAr4 = [ 1 2 3 4 ]

def doings(){
	myAr1^ += myAr1
	myAr2 += myAr2^
	myAr3^ += 1 //fine
	myAr4^ += myAr4
	"" + myAr1//[myAr1 myAr2 myAr3 myAr4]
}

~~~~~
//##22. Vectorized calls in assignment may only be of

myAr1 = [ 1 2 3 4 ]

def doings(){
	myAr1^ += myAr1^^ //nope!
	"" + myAr1
}

~~~~~
//##23. new must act on class not a function or method

def MyClass(a int){
	8
}

def doings(){
	myAr1 = new MyClass(3)//err

	"" 
}

~~~~~
//##24. cannot vectorize this 1
class MyClass(public ~x Integer){
	override equals(an Object) => false
	override hashCode() => 1
	override toString() => ""+x
}

def doings() String {
	ax = [new MyClass(1) new MyClass(3) ] 
	ax^x = [Integer(1) 2]//really should map to: ax^x = [Integer(1) 2]^
	
	return "" + ax
}

~~~~~
//##25. cannot vectorize this 2

class MyClass<Xxx >(~x Xxx){
	override equals(an Object) => false
	override hashCode() => 1
	
}

def doings() String {
	a = new MyClass<Integer>([Integer(1) 3]) 
	a.x = [Integer(1) 3] //setter
	return ""
}