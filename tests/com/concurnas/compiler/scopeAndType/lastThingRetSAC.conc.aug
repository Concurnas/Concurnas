//##1. simple loner - On OWN

def voidThing() void {}

def doings() String{
	12 //no cannot be on its own
	a6=3
	return "" + a6
}


~~~~~
//##2. simple loner - if - On OWN
def ff() => false
def voidThing() void {}

def doings() String{
	if(ff()){23;a=23}
	a6=3
	return "" + a6
}


~~~~~
//##3. simple loner - if fail 2 - On OWN

def voidThing() void {}
def ff() => false
def doings() String{
	if(ff()){23} //i think its just vars which cannot be declard on their own...
	a6=3
	return "" + a6
}

~~~~~
//##4. map stuff - On OWN
def doings() String{
	{12}
	{12->4}[12] //no cannot be on its own
	g={12->4}[12] //yes!
	a6=3
	return "" + a6
}


~~~~~
//##5. ltr - ifs
def ff() => true
def doings() String{
	a = if(ff()){ 12} else { }
	return "" + a //a is recognised to be something from partial match on int (12)
}

~~~~~
//##5.b ltr - ifs
def ff() => true
def doings() String{
	a = if(ff()) {} else {}
	return "" + a //a cannot be resolved to a variable - //JPT: TODO: fix this such that a resolves to something?
}

~~~~~
//##6. ltr - blocks

def doings() String{
	a = {}
	return "" + a 
}

~~~~~
//##7. ltr - while

def doings() String{
	b=0
	a = while(b++ < 10){ }
	return "" + a 
}

~~~~~
//##8. ltr - nested 
def ff() => true
def doings() String{
	b=0
	a = while(b++ < 10){ if(ff()){12} else {} }
	return "" + a //a should be ok
}

~~~~~
//##9. ltr - for new 

def doings() String{
	b=0
	a = for(a in [1,2,3,4]){ }
	return "" + a 
}

~~~~~
//##10. ltr - for old 

def doings() String{
	b=0
	a = for(a=0; a<10; a++){ }
	return "" + a 
}

~~~~~
//##11. ltr - try catch

def doings() String{
	a = try{ 3 } catch(e) {}
	return "" + a 
}

~~~~~
//##11.b ltr - try catch fin

def doings() String{
	a = try{ 
	3 } catch(e) {//cannot return this either, so not on its own line
	2} //no cannot return this
	 
}

~~~~~
//##12. fin overrides all

def doings() String {
	x=try{ 
		12
		} finally{
		13
		} //the fin in the final block overrides the 12 thus 12 flagged as error on its own line
	return "" + x
}

~~~~~
//##13. a more complex case

za = [1 2 3 4 5 6 7 8 9]

def gofa() int{ return 8; } 

def doings() String{
	d = for(n = 0; n < za.length; n++){
		a = za[n]
		
		try{
			try{
				gofa()
			}
			catch(e){
				gofa()
			}
			finally{
				55 //this should fail own it's own anlaysis!
			}
		}
		catch(e){
			gofa()
		}
		finally{
			33
		}
		
	} 
			
	return "" + d
}

~~~~~
//##14. break and continue cannot return void

za = [1 2 3 4 5 6 7 8 9]

def aVoid() void {}
def ff() => true; def ffxx() => true; 
def doings() String{
	d = for(n = 0; n < za.length; n++){
		if(ff()){
			break aVoid()
		}
		elif(ffxx()){
			12
		}
		else{
			continue aVoid()
		}//also we can consider the if to be ok [no error flaggged], and be of returning an int
	} 
			
	return "" + d
}

~~~~~
//##15. last thing in function is ret

def doings() String{
	"ok"
}

~~~~~
//##16. last thing in lambda is ret

def doings() String{
	xxx = def (a int) String { ""+a }
	
	xxx(12)
}

~~~~~
//##17. cannot ret this

def doings() String{
	12 //not a string!
}

~~~~~
//##18. invalid

def voidRet() {}

def vernormal() int {
	try{
		12
	}catch(e){
		12
	}
	finally{
		voidRet()//void so does nothing
		return 19
	}
}

~~~~~
//##19.a final invalid 

def retSomethng() int {  22 }

def alright() int {
	try{
		12
	}catch(e){
		12
	}
	finally{
		retSomethng()//final ret overwrites the return
	}
}
def doings() String{
	"" + alright()
}

~~~~~
//##19.b final invalid - wrong type too 

def retSomethng() double {  22 }

def alright() int {
	try{
		12
	}catch(e){
		12
	}
	finally{
		retSomethng()//final ret overwrites the return
	}
}
def doings() String{
	"" + alright()
}


~~~~~
//##19.c final - not permitted

def voidRet()  {   }

def doings() String {
	try{
		voidRet()
	}catch(e){
		voidRet()
	}
	finally{
		//ret must return something as t/c doesnt
	}
}

~~~~~
//##19.d final - very ambigious so throwing lots of errors here seems reasonable

def voidRet()  {   }
def ff() => true
def doings() String {
	try{
		"12"
	}catch(e){
		"12"
	}
	finally{
		if(ff()){voidRet()} else{ "14" }
	}
}

~~~~~
//##20. sync block should return something

def doings() String {
	x=sync{
		//"hi"//TODO: with blocks
	}
	x
}

~~~~~
//##21. perfect the try catch err
//3 errors
def fail1(){//outputs two lines
	a=try{ 12
	}
	catch(e Exception){} //missing ret in catch
	catch(e Throwable){} //missing ret in catch
}

def fail2(){
	a=try{ 
		f=9
	}
	catch(e){12 }	
}

def fail3(){
	a=try{ 
		f=9
	}
	catch(e){ f=12 }	
}

def doings() => "ho"

~~~~~
//##22. no fin ret as expected plus deadcode

def doings() String{
	a=1
	try{
		return "" + a
	}
	finally{
		return "ok"
	}
	return "" + a//should get flagged as dead code!
}


~~~~~
//##23. missing fin set plus slready
//doesnt belong here but dont want to wait til end of time for the big one to finish
class Prot{
	private val z boolean
	this(){//err cos z not set
	}
	
}


class A{
	private val z boolean
	this(){
		z=false
	}
	
	this( a int ){
		this()
		z=false //flag as err cos already set
	}
}

class B{
	private val z boolean
	this(){
		z=false
	}
	
	this( a int ){
		this() //ok because z gets set
	}
}


def doings() => ""

~~~~~
//##24. ret cases

def gofail(i int) int {
	//this is clearly pointless...
	return if(i==0){	return 1	} elif(i==1) { return 15 } else { return 7 }
	//return a1
}

def gofail2(i int) int {
	//this is clearly pointless...
	a=  if(i==0){	return 1	} elif(i==1) { return 15 } else { return 7 }
	return a1
}

def gook(i int) int {
	a1 = if(i==0){	return 1	} elif(i==1) { return 15 } else { return 7 }
	return a1
}

def doings() String{
	return "" + [gook(0), gook(1), gook(2)]
}

~~~~~
//##25. some more on own errs
def tt() => true
def doings() String{
	a = {12} //ok
	{12}//fail
	12	//fail
	a3={ } if tt() else 7
	return "" + a
}

~~~~~
//##26. err as refmaker is not in the loop

def doings2() String { 
	ar = for(a =0; a < 10; a++) {  {continue a; }!	} //noes not in the loop
	return "" + ar// + [ar, ar2]
}


~~~~~
//##27. cont break not outside

def doings() String {
	n=0
	g="hi "
		
		if(n >5 and n<7 )
		{//miss out 6
			continue;//no
		}
		
		if(n >5 and n<7 )
		{//miss out 6
			break//no
		}
		
		g += n + " "
	
	g+="end"
	return g;
}

~~~~~
//##28. implicit returns must be universal

def ff()=> false
def sdf() void{}

def doer1(){
	if(ff()){
		b=9//no!
	}else{
		20
	}
}

def doer2(){
	x=if(ff()){
		b=9//no!
	}else{
		20
	}
	x
}

def doings(){
	"" + [doer1() doer2()]
}

~~~~~
//##29. one ret and one not implicit not permited

def ff()=> false
def sdf() void{}

def doer1(){
	if(ff()){
		return 9
	}else{
		b=20//no!
	}
}

def doings(){
	"" + doer1()
}


~~~~~
//##30. this is ok

def ff()=> false
def sdf() void{}

def doer1(){
	if(ff()){
		return 9
	}else{
		20
	}
}

def doings(){
	"" + doer1()
}

~~~~~
//##31. try block must return something

def dfff() => 34
def xyz() => 23

def doer1(){
	try{
		xyz()
	}catch(e){
		sdf=999		
	}finally{
		dfff()//cannot return
	}
	
}

def doer2(){
	try{
		return xyz()
	}catch(e){
		sdf=999		
	}finally{
		dfff()//cannot return
	}
	
}

def doer3(){
	try{
		return xyz()
	}catch(e){
		999
	}finally{
		dfff()//cannot return
	}
	
}

def ok(){//this one is ok
	try{
		xyz()
	}catch(e){
		999
	}finally{
		dfff()//cannot return
	}
	
}

def doings(){
	"" + [doer1() doer2() doer3() ok()]
}

~~~~~
//##32. more implicit return cases

def ff() => false

def doings1(){
	if(ff()){
		x=9
	}else{
		ff()//valid
	}
	
	"ok"
}


def doings2(){
	if(ff()){
		x=9
	}else{
		22//not valid
	}
	
	"ok"
}

def doings(){
	"" + [doings1(), doings2()]	
}

~~~~~
//##33. try restriction

def doings2(){
	x=try{
		x=9
		12
	}finally{
		22//not permitted here
	}
	
	"ok"
} 

~~~~~
//##34. try with inner with no implicit return

finNestPlus1 = 0; def finNplus1() { finNestPlus1++ }

catchCalll2a  = 0
catchCalll2b  = 0
catchCalll1a  = 0
catchCalll1b  = 0
f1=0; f2=0;

def catchCall2a() {	catchCalll2a++}
def catchCall2b() {	catchCalll2b++}
def catchCall1a() {	catchCalll1a++}
def catchCall1b() {	catchCalll1b++}

def fincall1() {	f1++}
def fincall2() {	f2++}
def fincall3() {	f2++}

open class Excep1 extends Exception{override equals(o Object) boolean { return true;}}
open class Excep2 extends Excep1{override equals(o Object) boolean { return true;}}

def mycall(fail boolean, ff int) int {
	if(fail){ 
		if(ff == 1){ throw new Excep1(); }	
		if(ff == 2){ throw new Excep2(); }	
	}
	return 888; 
}

fail = false; theOne =1;  throwa =true

ab = 0; r=-30;

def sxt(){
	try{
		while(ab++ < 2){
			finNplus1()  
			if(ab==2){ break;} 
			finNplus1() 
			try{
				try{ 
					mycall(fail, theOne)
					r =  9; return r
				}
				catch(he Excep1){
					catchCall1a();
				}
				finally{  
					fincall1(); 
				}
			}
			catch(e Excep2){
				catchCall2b();
			}
			finally{  
				fincall2(); 
			  }
			r =  77 ;  break
		}
		//x=8
	}
	catch(e){
		x=9
	}
	finally{fincall3()}
}

def doings() String{
	sxt();
	return "" + r + ": " + [ f1, f2 ] + " :: " + finNestPlus1
}

~~~~~
//##35. no implicit return in finally block

def doings(){
	x = try{
		23
	}finally{
		22
	}
	
	"erm: " + x
}
