//##1. simple translate to getter
{
public class Cls {
public x int ;
public def setX ( val x int ) void {
this \. x = x ;
return ;
}
public def getX ( ) int {
return this \. x ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) void {
cls x1simpletranslatetogetter.Cls = new x1simpletranslatetogetter.Cls ( ) ;
a int = cls . getX ( ) ;
b int = cls \. x ;
many x1simpletranslatetogetter.Cls [] = [ cls   cls ] ;
a2 int = many [ 0 ] . getX ( ) ;
b2 int = many [ 0 ] \. x ;
return ;
}
}


~~~~~
//##2. advanced translate to getter
{
public class Cls {
public y int ;
public def setY ( val y int ) void {
this \. y = y ;
return ;
}
public def getY ( ) int {
return this \. y ;
}
public this ( ) {
super ( ) ;
}
}
public class Cls2 {
public x x2advancedtranslatetogetter.Cls = new x2advancedtranslatetogetter.Cls ( ) ;
public def setX ( val x x2advancedtranslatetogetter.Cls ) void {
this \. x = x ;
return ;
}
public def getX ( ) x2advancedtranslatetogetter.Cls {
return this \. x ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) void {
obj x2advancedtranslatetogetter.Cls2 = new x2advancedtranslatetogetter.Cls2 ( ) ;
a int = obj . getX ( ) . getY ( ) ;
b int = obj \. x . getY ( ) ;
c int = obj . getX ( ) \. y ;
d int = obj \. x \. y ;
return ;
}
}


~~~~~
//##2b. advanced translate to getter - arrays
{
public class Cls {
public y int ;
public def setY ( val y int ) void {
this \. y = y ;
return ;
}
public def getY ( ) int {
return this \. y ;
}
public this ( ) {
super ( ) ;
}
}
public class Cls2 {
public x x2badvancedtranslatetogetterarrays.Cls [] = [ new x2badvancedtranslatetogetterarrays.Cls ( )   new x2badvancedtranslatetogetterarrays.Cls ( ) ] ;
public def setX ( val x x2badvancedtranslatetogetterarrays.Cls [] ) void {
this \. x = x ;
return ;
}
public def getX ( ) x2badvancedtranslatetogetterarrays.Cls [] {
return this \. x ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) void {
obj x2badvancedtranslatetogetterarrays.Cls2 = new x2badvancedtranslatetogetterarrays.Cls2 ( ) ;
a int = obj . getX ( ) [ 0 ] . getY ( ) ;
b int = obj \. x [ 0 ] . getY ( ) ;
c int = obj . getX ( ) [ 0 ] \. y ;
d int = obj \. x [ 0 ] \. y ;
return ;
}
}


~~~~~
//##3. setter - simple
{
public class Cls {
public x int ;
public def setX ( val x int ) void {
this \. x = x ;
return ;
}
public def getX ( ) int {
return this \. x ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) void {
cls x3settersimple.Cls = new x3settersimple.Cls ( ) ;
cls . setX ( 99 ) ;
cls \. x = 99 ;
return ;
}
}


~~~~~
//##3b. setter - simple arrays
{
public class Cls {
public x int ;
public def setX ( val x int ) void {
this \. x = x ;
return ;
}
public def getX ( ) int {
return this \. x ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) void {
cls x3bsettersimplearrays.Cls [] = [ new x3bsettersimplearrays.Cls ( )   new x3bsettersimplearrays.Cls ( ) ] ;
cls [ 0 ] . setX ( 99 ) ;
cls [ 0 ] \. x = 99 ;
return ;
}
}


~~~~~
//##3c. setter - simple arrays rhs
{
public class Cls {
public x int[] = [ 1   2   3 ] ;
public def setX ( val x int[] ) void {
this \. x = x ;
return ;
}
public def getX ( ) int[] {
return this \. x ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) void {
cls x3csettersimplearraysrhs.Cls = new x3csettersimplearraysrhs.Cls ( ) ;
cls . getX ( ) [ 0 ] = 99 ;
cls \. x [ 0 ] = 99 ;
return ;
}
}


~~~~~
//##4. setter - advanced
{
public class Cls {
public y int ;
public def setY ( val y int ) void {
this \. y = y ;
return ;
}
public def getY ( ) int {
return this \. y ;
}
public this ( ) {
super ( ) ;
}
}
public class Cls2 {
public x x4setteradvanced.Cls = new x4setteradvanced.Cls ( ) ;
public def setX ( val x x4setteradvanced.Cls ) void {
this \. x = x ;
return ;
}
public def getX ( ) x4setteradvanced.Cls {
return this \. x ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) void {
obj x4setteradvanced.Cls2 = new x4setteradvanced.Cls2 ( ) ;
obj . getX ( ) . setY ( 12 ) ;
obj \. x . setY ( 12 ) ;
obj . getX ( ) \. y = 12 ;
obj \. x \. y = 12 ;
return ;
}
}


~~~~~
//##5. no setter, no getter defined
{
public class Cls {
public x int ;
public this ( ) {
super ( ) ;
}
}
public def doings ( ) void {
cls x5nosetternogetterdefined.Cls = new x5nosetternogetterdefined.Cls ( ) ;
a int = cls . x ;
cls . x = a ;
a = cls \. x ;
cls \. x = a ;
return ;
}
}


~~~~~
//##6. no setter, no getter if in self
{
public class Cls {
protected x int ;
public def setX ( val x int ) void {
this \. x = x ;
return ;
}
public def getX ( ) int {
return this \. x ;
}
public def doWork ( ) void {
this . x = 88 ;
y int = this . x ;
x = 99 ;
y = x ;
return ;
}
public this ( ) {
super ( ) ;
}
}
}


~~~~~
//##7. no setter, no getter if reference to self
{
public class Cls {
protected x int ;
public def setX ( val x int ) void {
this \. x = x ;
return ;
}
public def getX ( ) int {
return this \. x ;
}
public def doWork ( var arg x7nosetternogetterifreferencetoself.Cls ) void {
arg . x = 88 ;
y int = arg . x ;
return ;
}
public this ( ) {
super ( ) ;
}
}
}


~~~~~
//##7b. do setter and gett if refernce to a super or a different child
{
public class Sup {
public x int ;
public def setX ( val x int ) void {
this \. x = x ;
return ;
}
public def getX ( ) int {
return this \. x ;
}
public this ( ) {
super ( ) ;
}
}
public class ClsOther extends Sup {
public this ( ) {
super ( ) ;
}
}
public class Cls extends Sup {
protected x int ;
public def setX ( val x int ) void {
this \. x = x ;
return ;
}
public def getX ( ) int {
return this \. x ;
}
public def doWork ( var arg x7bdosetterandgettifreferncetoasuperoradifferentchild.Sup ) void {
arg . setX ( 88 ) ;
y int = arg . getX ( ) ;
return ;
}
public def doWork ( var arg x7bdosetterandgettifreferncetoasuperoradifferentchild.ClsOther ) void {
arg . setX ( 88 ) ;
y int = arg . getX ( ) ;
return ;
}
public this ( ) {
super ( ) ;
}
}
}


~~~~~
//##7c. no setter and getter if ref via super
{
public class Sup {
public x int ;
public def setX ( val x int ) void {
this \. x = x ;
return ;
}
public def getX ( ) int {
return this \. x ;
}
public this ( ) {
super ( ) ;
}
}
public class Cls extends Sup {
protected x int ;
public def setX ( val x int ) void {
this \. x = x ;
return ;
}
public def getX ( ) int {
return this \. x ;
}
public def doWork ( ) void {
super . x = 88 ;
y int = super . x ;
return ;
}
public this ( ) {
super ( ) ;
}
}
}


~~~~~
//##10.1. non eq setters
{
public class Cls {
public x int ;
public def setX ( val x int ) void {
this \. x = x ;
return ;
}
public def getX ( ) int {
return this \. x ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) void {
obj x101noneqsetters.Cls = new x101noneqsetters.Cls ( ) ;
obj . SYN-getXsetX ( += , PRE , 69 ) ;
return ;
}
}


~~~~~
//##10.2 advanced non eq setters
{
public class Held {
public y int ;
public def setY ( val y int ) void {
this \. y = y ;
return ;
}
public def getY ( ) int {
return this \. y ;
}
public this ( ) {
super ( ) ;
}
}
public class Cls {
public x x102advancednoneqsetters.Held = new x102advancednoneqsetters.Held ( ) ;
public def setX ( val x x102advancednoneqsetters.Held ) void {
this \. x = x ;
return ;
}
public def getX ( ) x102advancednoneqsetters.Held {
return this \. x ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) void {
obj x102advancednoneqsetters.Cls = new x102advancednoneqsetters.Cls ( ) ;
obj . getX ( ) . SYN-getYsetY ( += , PRE , 69 ) ;
obj . getX ( ) . SYN-getYsetY ( *= , PRE , 2 ) ;
return ;
}
}


~~~~~
//##10.3 advanced non eq setters via arrastuff
{
public class Holder {
public x int ;
public def setX ( val x int ) void {
this \. x = x ;
return ;
}
public def getX ( ) int {
return this \. x ;
}
public this ( ) {
super ( ) ;
}
}
public def funcCall ( ) x103advancednoneqsettersviaarrastuff.Holder [] {
return [ new x103advancednoneqsettersviaarrastuff.Holder ( )   new x103advancednoneqsettersviaarrastuff.Holder ( ) ] ;
}
public def doings ( ) void {
funcCall ( ) [ 0 ] . SYN-getXsetX ( += , PRE , 12 ) ;
return ;
}
}


~~~~~
//##10.3b advanced non eq setters via otherarray stuff
{
public class Holder {
public x int[] = [ 1   2   3 ] ;
public def setX ( val x int[] ) void {
this \. x = x ;
return ;
}
public def getX ( ) int[] {
return this \. x ;
}
public this ( ) {
super ( ) ;
}
}
public def funcCall ( ) x103badvancednoneqsettersviaotherarraystuff.Holder [] {
return [ new x103badvancednoneqsettersviaotherarraystuff.Holder ( )   new x103badvancednoneqsettersviaotherarraystuff.Holder ( ) ] ;
}
public def doings ( ) void {
funcCall ( ) [ 0 ] . getX ( ) [ 0 ] += 12 ;
return ;
}
}


~~~~~
//##10.4 inc dec operations
{
public class Cls {
public x int = 10 ;
public def setX ( val x int ) void {
this \. x = x ;
return ;
}
public def getX ( ) int {
return this \. x ;
}
public y int = 10 ;
public def setY ( val y int ) void {
this \. y = y ;
return ;
}
public def getY ( ) int {
return this \. y ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) void {
cls x104incdecoperations.Cls = new x104incdecoperations.Cls ( ) ;
cls . SYN-getXsetX ( += , POST , 1 ) ;
cls . SYN-getXsetX ( += , PRE , 1 ) ;
a int = cls . SYN-getXsetX ( += , POST , 1 ) ;
b int = cls . SYN-getXsetX ( += , PRE , 1 ) ;
cls . SYN-getYsetY ( -= , POST , 1 ) ;
cls . SYN-getYsetY ( -= , PRE , 1 ) ;
a2 int = cls . SYN-getYsetY ( -= , POST , 1 ) ;
b2 int = cls . SYN-getYsetY ( -= , PRE , 1 ) ;
return ;
}
}


~~~~~
//##10.4a advanced inc dec operations
{
public class A {
protected x int ;
public def setX ( val x int ) void {
this \. x = x ;
return ;
}
public def getX ( ) int {
return this \. x ;
}
public this ( ) {
super ( ) ;
}
}
public class B {
protected h int ;
public def setH ( val h int ) void {
this \. h = h ;
return ;
}
public def getH ( ) int {
return this \. h ;
}
public this ( ) {
super ( ) ;
}
}
public def functo ( var inp int ) x104aadvancedincdecoperations.B {
return new x104aadvancedincdecoperations.B ( ) ;
}
public def doings ( ) void {
a x104aadvancedincdecoperations.A = new x104aadvancedincdecoperations.A ( ) ;
functo ( a . SYN-getXsetX ( += , POST , 1 ) ) . setH ( 9 ) ;
return ;
}
}


~~~~~
//##10.4b advanced inc dec operations
{
public class A {
protected x int ;
public def setX ( val x int ) void {
this \. x = x ;
return ;
}
public def getX ( ) int {
return this \. x ;
}
public this ( ) {
super ( ) ;
}
}
public class B {
protected h int = 90 ;
public def setH ( val h int ) void {
this \. h = h ;
return ;
}
public def getH ( ) int {
return this \. h ;
}
public this ( ) {
super ( ) ;
}
}
public def functo ( var inp int ) x104badvancedincdecoperations.B {
return new x104badvancedincdecoperations.B ( ) ;
}
public def doings ( ) void {
a x104badvancedincdecoperations.A = new x104badvancedincdecoperations.A ( ) ;
res int = functo ( a . SYN-getXsetX ( += , POST , 1 ) ) . SYN-getHsetH ( -= , POST , 1 ) ;
return ;
}
}


~~~~~
//##10.5 neg should be left as a pre-post op
{
public class Cls {
protected x int = 10 ;
public def setX ( val x int ) void {
this \. x = x ;
return ;
}
public def getX ( ) int {
return this \. x ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) void {
cls x105negshouldbeleftasaprepostop.Cls = new x105negshouldbeleftasaprepostop.Cls ( ) ;
x int = - cls . getX ( ) ;
return ;
}
}


~~~~~
//##10.6 string add
{
public class Cls {
protected x java.lang.String = " hi " ;
public def setX ( val x java.lang.String ) void {
this \. x = x ;
return ;
}
public def getX ( ) java.lang.String {
return this \. x ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) void {
cls x106stringadd.Cls = new x106stringadd.Cls ( ) ;
cls . SYN-getXsetX ( += , PRE , "  there " ) ;
return ;
}
}


~~~~~
//##11 array length remains the same
{
public def doings ( ) void {
ar int[] = [ 1   2   3   4 ] ;
ll int = ar . length ;
return ;
}
}


~~~~~
//##12.1 setter and getter different types - ok case
{
public class Cls {
protected x int = 89 ;
public def getX ( ) java.lang.String {
return "  " + x ;
}
public def setX ( var wow java.lang.String ) void {
x = 99 ;
return ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) void {
cc x121setterandgetterdifferenttypesokcase.Cls = new x121setterandgetterdifferenttypesokcase.Cls ( ) ;
hi java.lang.String = cc . getX ( ) ;
cc . setX ( " hi " ) ;
return ;
}
}


~~~~~
//##12.2 setter and getter different types - error case
line 16:10 incompatible type: int vs java.lang.String
line 17:4 Unable to find method with matching name: setX and arguments (int)

~~~~~
//##12.3 setter and getter different types - increment ok case
{
public class Cls {
protected x int = 89 ;
public def getX ( ) java.lang.String {
return "  " + x ;
}
public def setX ( var wow java.lang.String ) void {
x = 99 ;
return ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) java.lang.Object {
cc x123setterandgetterdifferenttypesincrementokcase.Cls = new x123setterandgetterdifferenttypesincrementokcase.Cls ( ) ;
cc . SYN-getXsetX ( += , PRE , " hi " ) ;
cc . SYN-getXsetX ( += , PRE , 99 ) ;
return cc . SYN-getXsetX ( += , POST , 1 ) ;
}
}


~~~~~
//##12.4 setter and getter different types - increment error case
line 20:4 numerical operation cannot be performed on type x124setterandgetterdifferenttypesincrementerrorcase.AnotherM
line 21:4 numerical operation cannot be performed on type x124setterandgetterdifferenttypesincrementerrorcase.AnotherM

~~~~~
//##13 def with ifexpr
{
public class Cls {
protected x int = 89 ;
public def setX ( val x int ) void {
this \. x = x ;
return ;
}
public def getX ( ) int {
return this \. x ;
}
public this ( ) {
super ( ) ;
}
}
z int = 5 ;
public def doingsfail ( ) void {
cc x13defwithifexpr.Cls = new x13defwithifexpr.Cls ( ) ;
ag int = if ( 4 > z ) {
cc . SYN-getXsetX ( += , POST , 1 ) ;
}
else {
cc . getX ( ) ;
}
;
return ;
}
}


~~~~~
//##14 lambda setter getter with invoke
{
public class MyClass {
protected funcTo ( java.lang.Integer , java.lang.Integer , java.lang.Integer ) out java.lang.Integer = def ( var a java.lang.Integer , var b java.lang.Integer , var c java.lang.Integer ) java.lang.Integer {
return a + b + c ;
}
;
public def setFuncTo ( val funcTo ( java.lang.Integer , java.lang.Integer , java.lang.Integer ) out java.lang.Integer ) void {
this \. funcTo = funcTo ;
return ;
}
public def getFuncTo ( ) ( java.lang.Integer , java.lang.Integer , java.lang.Integer ) out java.lang.Integer {
return this \. funcTo ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) java.lang.String {
myCls x14lambdasettergetterwithinvoke.MyClass = new x14lambdasettergetterwithinvoke.MyClass ( ) ;
myCls . setFuncTo ( def ( var a java.lang.Integer , var b java.lang.Integer , var c java.lang.Integer ) java.lang.Integer {
return a + b + c * 2 ;
}
) ;
return "  " + myCls . getFuncTo ( ) ( 2 , 3 , 4 ) ;
}
}


~~~~~
//##15 bugfix direct to setter or getter 
{
public class MyClass {
private ab int = 9 ;
public def getA ( ) int {
return ab ;
}
public def setA ( var a int ) void {
ab = a ;
return ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) java.lang.String {
mc x15bugfixdirecttosetterorgetter.MyClass = new x15bugfixdirecttosetterorgetter.MyClass ( ) ;
mc . setA ( 99 ) ;
return "  " + mc . getA ( ) ;
}
}