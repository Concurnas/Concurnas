//##69. Returns - 1. return must be possible - simple elifs
//TODO: switch statement can result in the case of returns being a problem. elif and try/catch are covered fully though!
def s() {5>6 } def fooFail1() int
{
	if(s()){ return 6 }
}


def fooFail2() int
{
	if(s()){ return 6 }
	else {  }
}

def fooFail3() int
{
	if(s()){ return 6 }
	elif(s()) {}
}

def fooFail4() int
{
	if(s()){ return 6 }
	elif(s()) {}
	else{return 55}
}

def fooFail5() int
{
	if(s()){ return 6 }
	elif(s()) {return 66 }
}

def fooOK() int{
	if(s()){ return 6 } 
	return 7
}

def fooOK2() int{
	if(s()){ return 6 } else { return 7}
}

def fooOK3() int
{
	if(s()){ return 6 }
	return 2
}


~~~~~
//##69. Returns - 2 - nested blocks

def foo1() int
{
	 {return 6 }
}

def foo2() int
{
	{ {return 6 }}
}

def ss() {5>6 } def foo3() int {
	if(ss()) { {return 6 } }
	else     { {return 6 } }
}

~~~~~
//##69. Returns - 3. return must be possible - simple exceptions

def fooAhItNoFail1() int
{//doesnt fail
	try{ return 6 } finally {  }
}


def fooAhItNoFail2() int
{//doesnt fail
	try{ return 6 }
	finally {  }
}

def fooFail3() int
{
	try{ return 6 }
	catch(e Exception) {}
}

def fooFail4() int //this doesnt fail because return always in finally block
{
	try{ return 6 }
	catch(e Exception) {}
	finally{}	return 55
}

def fooFail5() int
{
	try{ return 6 }
	catch(e Exception) {return 66 }
}

def fooOKFailUnreach() int{
	try{ return 6 } catch(e Exception) {return 66 }
	return 7
}

def fooOK() int{
	try{ return 6 } catch(e Exception) {return 66 }
}

def fooOK2() int{
	try{ return 6 } finally { }
}

def fooOK3FailUnreach() int
{
	try{ return 6 }catch(e Exception) {return 66 }
	return 2
}

def fooOK3() int
{
	try{ return 6 }catch(e Exception) {return 66 }
}


~~~~~
//##69. Returns - 4. deadcode analysis 1

def fail1() int
{
	return 69;
	a = 8;
}

def fail2() int
{
	{ return 69; }
	a = 8;
}


def fail3() int
{
	throw new Exception("");
	return 8;
}

def fail4() int
{
	{throw new Exception("");}
	return 8;
}

def s() {5>6 } def fail5() int
{
	if(s()){throw new Exception("")}
	else {throw new Exception("") }
	return 8;
}

def fail6() int
{
	if(s())
	{
		if(s()){throw new Exception("")}
		else {throw new Exception("") }
	}
	else{
		throw new Exception("");
	}
	
	return 8;
}

def fail7() int
{
	try
	{
		if(s()){throw new Exception("")}
		else {throw new Exception("") }
	}
	finally
	{
		throw new Exception("")
	}
	return 8;
}

def ok1() int
{
	try
	{
		if(s()){throw new Exception("")}
		else {throw new Exception("") }
	}
	catch(e Exception)
	{
	
	}
	return 8;
}

def ok2() int
{
	try
	{
		if(s()){throw new Exception("")}
		else {throw new Exception("") }
	}
	catch(e Exception)
	{
	
	}
	return 8;
}

~~~~~
//##69. Returns - 4. oh yeah, lambdas
def s() {5>6 } 
xxx = def () int { if(s()){ return 6 } else {  } }

~~~~~
//##69. Returns - on other special stuff..
//good enough for now but may need to fix this in the future
def aa() int{
	for(a in [1,2,3])	{//TODO: throws the wrong exception, should be - This method must return a result of type INT, problem with is last thing ret type logic 
		return a//ERROR as for etc dont complete returns
	}
}

def ggg() int{
	try(null)	{
		return 6; //with does! complete returs
	}
}

def ggg2() int{
	try(null)	{
		//back to fail
	}
}

def fail() int
{
	try(null)
	{
		throw new Exception("")
	}
	return 6//cannot reach this!
}


def fail2() int
{
	
	sync{throw new Exception("")}
	return 6 //no!
}

~~~~~
//##69. Returns-deadcode - in classes and also nested functions et al

def s() {5>6 }class Fail1
{
	def xx() int
	{
		if(s())
		{
			return 6;
		}
	}//fail
}

class ok1
{
	def xx() int
	{
		if(s())
		{
			return 6;
		} return 7
	}
}

class Fail2
{
	def xx() int
	{
		def inner() int
		{
			//also fail here as inner
		}
		return 6
	}
}

class ok2
{
	def xx() int
	{
		def inner() int
		{
			return 7
		}
		return 6
	}
}

class Fail3
{
	def xx() int
	{
		for(a in [1,2,3])
		{
			throw new Exception("")
			g = 8//err - deadcode
		}
		return 6//this is not deadcode
	}
}

class Fail4
{
	def xx() int
	{
		for(a in [1,2,3])
		{
			return 8
			g = 8//err - deadcode
		}
		return 6
	}
}

~~~~~
//##69. Returns-deadcode - 2 after break and cotinue

def Fail1() {
	for(a in [1,2,3]) {
		break;
		g = 9 //no!
	}
}

def Fail2() {
	for(a in [1,2,3]) {
		continue;
		g = 9
	}
}

def s() {5>6 }def OK() {
	for(a in [1,2,3]) {
		if(s())
		{
			break;
		}
		g = 9 //no!
	}
}

def Failcomplex3() {
	for(a in [1,2,3]) {
		if(s())
		{
			break;
		}
		else
		{
			continue;
		}
		g = 9 //no!
	}
}



~~~~~
//##69. break etc dont bleed beyond the for block, or while etc
def OK() int
{
	for(a in [1,2,3]) {
		break;
	}
	g int = 57
	return g
}


def OK2() int
{
	while(true) {
		break;
	}
	g int= 57
	return g
}

def FAILreallywillbreak() int
{
	for(a in [1,2,3]) {
		try{ break; } finally { } 
		kk=8
	}
	g int = 57
	return g
}

~~~~~
//##69. exceptions thrown in try catch make stuff unreachable
def exceper(n int) String {
	ret = "";
	
	try{
		ret = "mainBlock";
		//thrower()
		throw new Exception("Dd2d")
		h=9 //unreachable
	}
	catch(e Exception)
	{
		ret = "excep";
	}
	
	return ret;
}

~~~~~
//##70. double check returns inside lambda
//expected to all be ok
def one(a int) int{
	a+3; //missing ret
}

lam = def (a int) int{
	a+3; //missing ret
}

def retLambda() (int) int{
	x = def (a int) int { a + 12;}
	return def (a int) int { a + 12;}
}

~~~~~
//##71. unreachable code after exception raised inside catch block

class A<T> with Cloneable, java.io.Serializable {
    //length int = X ;
    override clone() T[]?  {
        try {
            return super.clone() as T[]; // unchecked warning
        } catch (e CloneNotSupportedException) {
            throw new InternalError(e.getMessage());
        }
        return null;//should be tagged as unreachable code
    }
}

class B<T> with Cloneable, java.io.Serializable {//this variant is ok
    override clone() T[]  {
        try {
            return super.clone() as T[]; // unchecked warning
        } catch (e CloneNotSupportedException) {
            throw new InternalError(e.getMessage());
        }
    }
}

~~~~~
//##72. this is fine

def doings() String {	
	try{
		return ""
	}
	finally{
	
	}
}

~~~~~
//##73. this is also fine
cnt = 0;
def s() {5>6 }def mycall(fail boolean) int { 
	if(fail){
		throw new Exception("ee");
	}
	else{ return ++cnt; }
}

def mycall2(fail boolean) int { //if stmt throws an exception, so no ret necisary really
	if(fail){
		throw new Exception("ee");
	}
	elif(s()){
		return 77
	}
	else{ return ++cnt; }
}

~~~~~
//##74. defo returns in all catches means all returns

def testMethAlwaysThrows1(fail1 boolean) int {
	try{ 
		if(fail1){ throw new Exception("") }
		else { throw new Exception("") }
	}
	catch(e Exception){
		 return 12
	}
	catch(e Throwable){
		 return 12
	}
	//return 99
}

~~~~~
//##75. defo returns in some catches not all return

def testMethAlwaysThrows1(fail1 boolean) int {
	try{ 
		if(fail1){ throw new Exception("") }
		else { throw new Exception("") }
	}
	catch(e Exception){
		 
	}
	catch(e Throwable){
		 return 12
	}
	return 99
}

~~~~~
//##76.1 misc

def tcFail1() { //synthetic return doesnt trigger inf loop
	x=1
	for (;;) { a=1}
}

~~~~~
//##76.1.1 inifinite loop - for - on own

def failola() String{
	for (;;) { a=1}
	"odd"//never gets here
}

~~~~~
//##76.1.2 inifinite loop - for - tcf

def tcFail1() String{
	try {
	    for (;;) { a=1}//defo goes here
	} catch (e Error) {
	    System.out.println(e + ", " + (1==1));
	}
	"uh oh"
}

def tcFail2() String{
	try {
	    x="uh oh"
	} catch (e Error) {
	    f=9
	}
	finally{
		for (;;) { a=1}//defo goes here
	}
	"odd"
}

def tcOk() String{
	
	try {
	    x="uh oh"
	} catch (e Error) {//may not go into
	    for (;;) { a=1}
	}
	"odd"
}

~~~~~
//##76.1.3 inifinite loop - for - if


def s() {5>6 }def tcFail1() String{
	if( s()){
		 for (;;) { a=1}//defo goes here
	}else{
		 for (;;) { a=1}//defo goes here
	}
	"uh oh"
}


def tcFail2() String{
	if( s()){
		 for (;;) { a=1}//defo goes here
	}elif( s()){
		 for (;;) { a=1}//defo goes here
	}else{
		 for (;;) { a=1}//defo goes here
	}
	"uh oh"
}


def tcOk() String{
	if( s()){
		 for (;;) { a=1}//defo goes here
	}
	
	"uh oh"
}

~~~~~
//##76.1.4 inifinite loop - for - for old

def tcFail1() String{

	for( n=0; n < 10; n++){
		for (;;) { a=1}//defo goes here
	}

	"uh oh"
}

~~~~~
//##76.1.5 inifinite loop - for - for old 
def tcFail1() String{
	for( n=0; {for (;;) { a=1}; n < 10}; n++){
		a=9//defo goes here
	}
	"uh oh"
}

~~~~~
//##76.1.5.b inifinite loop - for - for old cond 
n=9
def tcFail1() String{
	for( n=0; {for (;2>1;) { a=1}; n < 10}; n++){
		a=9//defo goes here
	}
	"uh oh"
}
 

~~~~~
//##76.1.6 inifinite loop - for - for new 

def tcFail1() String{

	for( x in [1,2,3] ){
		for (;;) { a=1}//defo goes here
	}

	"uh oh"
}

def fine() {

	for( x in [1,2,3] ){
		for (;;) { a=1}//a ok
	}
}

~~~~~
//##76.1.7 inifinite loop - for - while 

f = false

def aOK() {
	while(f){
		for (;;) { a=1}//defo goes here
	}
}

def fail() {
	while(f){
		for (;;) { a=1}//defo goes here
	}
	"oh no"
}

~~~~~
//##76.1.8 inifinite loop - for - ananon block 

def failos() String {
	{
		for (;;) { a=1}//defo goes here
	}
	"oh no"
	//return
}

~~~~~
//##76.1.9 inifinite loop - for - async block 

def compok1() String {

	{
		for (;;) { a=1}//defo goes here
	}!
	"oh no"
	//return
}

~~~~~
//##76.1.10 inifinite loop - for - barrier block and with block

def compok1() String {
	sync{
		for (;;) { a=1}//defo goes here
	}//and same for with but no test so meh! //its not an inf loop though...
	"oh no"
	//return
}

~~~~~
//##76.1.1 inifinite loop - while - goes on forever cases

//dont bother testing inside for loops etc, the code is the same
def forever1() String {
	while(true) { }
	"oh no"//goes on forever
}

def forever2() String {
	while(true or false) { }
	"oh no"//goes on forever
}

def forever3() String {
	while(not false) { }
	"oh no"//goes on forever
}

def forever4() String {
	while(true and true) { }
	"oh no"//goes on forever
}

~~~~~
//##77. misc bug, last thing ret doesnt have to be an expression

b={[1,1]}!

def doings() String{
	j1 = ++b[0] //not an expression, thus fail
}

~~~~~
//##78. onchange return analysis at least one must return here

def doings()  {
	xs int:
	log := ""
	res = onchange(xs){
		log += xs
		if(xs==6){
			log += " go into so ret"
			break//just escapes with no write to var
		}
		log += " got ret 9"
		return 
	}
	xs=24
	await(log;log=="24 got ret 9")
	xs=6
	await(log;log=="24 got ret 96 go into so ret")
	"" + [res, log]
}

~~~~~
//##79. await validation for ret

def doings()  {
	xs int: = 6
	await(xs;{ if(xs==6) {return 2}; true})//fail
	"" + xs
}

~~~~~
//##80. await validation for ret -2

def doings()  {
	xs int: = 6
	await(xs;{ if(xs==6) {return }; true})//fail - must define something
	"" + xs
}


~~~~~
//##81. this inf loop triggers no deadcode

from com.concurnas.runtime.channels import PriorityQueue

def doings(){
	pq = PriorityQueue<String?>()
	
	isDone:=false
	cnt:=0
	{
		while(true)
		{
			got = pq.pop()
			cnt++;
			if(null == got){
				isDone = true
			}
		}
	}!
	
	pq.add(1, "one")
	pq.add(1, null)
	
	await(isDone;isDone)
	
	"nice " + cnt
}

~~~~~
//##82. if statement resolves always to true or false

def something() => false

class Myc()

def t1(){
	if(true){a=""}
}

def t2(){
	if(false){a=""}
}

def t3(){
	if(true){""}elif(something()){""} else{""}
}

def t4(){
	if(false){""}elif(something()){""} else{""}
}

def t5(){
	if(something()){a=""}elif(true){a=""}
}

def t6(){
	if(something()){a=""}elif(false){a=""} 
}

def t7(){
	if(something()){a=""}elif(true){a=""} else{a=""}
}

def t8(){
	if(something()){a=""}elif(false){a=""} else{a=""}
}


def doings(){
	""  
}

~~~~~
//##83. if expresssion resolves always to true or false


def d1(){
	x = 1 if true else 2
}

def d2(){
	x = 1 if false else 2
}

def doings() => "ok"

~~~~~
//##84. impossible to assign on paths which return dont invalidate

def fff() => false

def doings(){

	a String
	if(fff()){
		a = "ok"
	}else{
		return 'ok'//a is not set but it doesnt matter as if we get to this path 
		//we have returned already
	}
	a
}
