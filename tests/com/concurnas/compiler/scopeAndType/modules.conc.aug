//##1. module missing things in import
//##MODULE com.myorg.code2

class MYClass{
	def result() => 9
}

//##MODULE

from com.myorg.code2 import result
from com.myorg.code2 import MYClass2

def doings(){
	""
}

~~~~~
//##2. module respects public private etc
//##MODULE com.myorg.code2

public result = 9 //private by default
def lafunc() => "hi" //public by default

//##MODULE

from com.myorg.code2 import result, lafunc

def doings(){
	"" + [result, lafunc()]
}

~~~~~
//##3. tpyedef pppp

//##MODULE com.myorg.codexx

private typedef thingPriv = int
typedef thingDef = int
protected typedef thingProt = int
package typedef thingPack = int
public typedef thingPub = int

//##MODULE
from com.myorg.codexx import thingPriv //no
from com.myorg.codexx import thingDef //yes public
from com.myorg.codexx import thingProt//no
from com.myorg.codexx import thingPack//no
from com.myorg.codexx import thingPub

def doings(){
	a1 thingPriv = 3 
	a2 thingProt = 3 
	a3 thingPack = 3 
	a4 thingPub = 3 
	a5 thingDef = 3 //public so ok
	"fail" 
}

~~~~~
//##4. class pppp

//##MODULE com.myorg.codexx

private class thingPriv
class thingDef
protected class thingProt
package class thingPack
public class thingPub

//##MODULE
from com.myorg.codexx import thingPriv //no
from com.myorg.codexx import thingDef  //ok
from com.myorg.codexx import thingProt //no
from com.myorg.codexx import thingPack //no
from com.myorg.codexx import thingPub  //ok

def doings(){
	a1 = new thingPriv() 
	a2 = new thingDef() 
	a3 = new thingProt() 
	a4 = new thingPack() 
	a5 = new thingPub()
	"fail" 
}

~~~~~
//##5. enum pppp

//##MODULE com.myorg.codexx

private enum thingPriv{ONE, TWO}
enum thingDef{ONE, TWO}
protected enum thingProt{ONE, TWO}
package enum thingPack{ONE, TWO}
public enum thingPub{ONE, TWO}

//##MODULE
from com.myorg.codexx import thingPriv //no
from com.myorg.codexx import thingDef  //ok
from com.myorg.codexx import thingProt //no
from com.myorg.codexx import thingPack //no
from com.myorg.codexx import thingPub  //ok

def doings(){
	a1 = thingPriv.ONE
	a2 = thingDef.ONE
	a3 = thingProt.ONE
	a4 = thingPack.ONE
	a5 = thingPub.ONE
	"fail" 
}

~~~~~
//##6. annotation pppp

//##MODULE com.myorg.codexx

private annotation thingPriv
annotation thingDef
protected annotation thingProt
package annotation thingPack
public annotation thingPub

//##MODULE
from com.myorg.codexx import thingPriv //no
from com.myorg.codexx import thingDef  //ok
from com.myorg.codexx import thingProt //no
from com.myorg.codexx import thingPack //no
from com.myorg.codexx import thingPub  //ok

@thingPriv
def f1(){}

@thingDef
def f2(){}

@thingProt
def f3(){}

@thingPack
def f4(){}

@thingPub
def f5(){}

def doings(){
	"fail" 
}

~~~~~
//##7. respect for protected
//expect 8 errors... since wrong package

//##MODULE com.myorg.code

protected def myfunc() => "hi"
protected typedef thing = int
protected annotation Annot{}
protected avar = 99
protected alambda = def () { "almabda"}

@Annot
protected class MyClass(protected f int){
	protected this(g String) {}
	protected a int = 99
	protected def thing() => 77
}

protected enum MYEnum{ONE, TWO, THREE }

//##MODULE
from com.myorg.code import MYEnum, MyClass, Annot, myfunc, thing, avar, alambda

//different package

@Annot //nope protected
def cannotAnnot(){}

def doings(){
	h=avar //not accessable from outside mod
	a thing = 3  //not accessable from outside mod
	nope = myfunc()//not accessable from outside mod
	one = MYEnum.ONE //nope protected
	mc = MyClass(8) //nope protected
	h = mc.a //not accessable from outside mod
	g = mc.thing() //not accessable from outside mod
	lam = alambda() //not accessable from outside mod
	"" 
}

~~~~~
//##8. respect for package

//##MODULE com.myorg.code

package def myfunc() => "hi"
package typedef thing = int
package annotation Annot{}
package avar = 99
package alambda = def () { "almabda"}

@Annot
package class MyClass(package f int){
	package this(g String) {}
	package a int = 99
	package def thing() => 77
}

package enum MYEnum{ONE, TWO, THREE }

//##MODULE
from com.myorg.code import MYEnum, MyClass, Annot, myfunc, thing, avar, alambda

//different package

@Annot //nope package
def cannotAnnot(){}

def doings(){
	h=avar //not accessable from outside mod
	a thing = 3  //not accessable from outside mod
	nope = myfunc()//not accessable from outside mod
	one = MYEnum.ONE //nope package
	mc = MyClass(8) //nope package
	h = mc.a //not accessable from outside mod
	g = mc.thing() //not accessable from outside mod
	lam = alambda() //not accessable from outside mod
	"" 
}