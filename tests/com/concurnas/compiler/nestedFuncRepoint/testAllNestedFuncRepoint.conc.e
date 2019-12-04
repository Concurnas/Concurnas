//##0. zero capture case
{
somestat int = 9 ;
public def funto ( var x int ) int {
hh int = 9 ;
public def NIF$0 ( var y int ) int {
return 7 + y + somestat ;
}
return NIF$0 ( 12 ) ;
}
public def doings ( ) java.lang.String {
b int = funto ( 5 ) ;
return "  " + b ;
}
}


~~~~~
//##1. simple localvar ref
{
somestat int = 9 ;
public def funto ( var x int ) int {
hh int = 9 ;
public def NIF$0 ( var y int , var x int , var hh int ) int {
return x + 7 + y + somestat + hh ;
}
return NIF$0 ( 12 , x , hh ) ;
}
public def doings ( ) java.lang.String {
b int = funto ( 5 ) ;
return "  " + b ;
}
}


~~~~~
//##2. simple localvar ref 2 levels
{
somestat int = 9 ;
public def funto ( var x int ) int {
hh int = 9 ;
gg int = 121 ;
public def NIF$1 ( var y int , var gg int , var x int , var hh int ) int {
gg += 1 ;
public def NIF$0 ( var z int , var x int , var y int , var hh int , var gg int ) int {
return x + 7 + y + somestat + hh + z + gg ;
}
return NIF$0 ( 5 , x , y , hh , gg ) ;
}
return NIF$1 ( 12 , gg , x , hh ) ;
}
public def doings ( ) java.lang.String {
b int = funto ( 5 ) ;
return "  " + b ;
}
}


~~~~~
//##3. simple localvar ref via class
{
somestat int = 9 ;
public class X {
jk int = 9 ;
public def funto ( var x int ) int {
hh int = 9 ;
public def NIF$0 ( var y int , var x int , var hh int ) int {
return x + 7 + y + somestat + hh + jk ;
}
return NIF$0 ( 12 , x , hh ) ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) java.lang.String {
b int = new x3simplelocalvarrefviaclass.X ( ) . funto ( 5 ) ;
return "  " + b ;
}
}


~~~~~
//##4. simple localvar ref via class - 2 levels
{
somestat int = 9 ;
public class X {
jk int = 9 ;
public def funto ( var x int ) int {
hh int = 9 ;
gg int = 121 ;
public def NIF$1 ( var y int , var gg int , var x int , var hh int ) int {
gg += 1 ;
public def NIF$0 ( var z int , var x int , var y int , var hh int , var gg int ) int {
return x + 7 + y + somestat + hh + z + gg + jk ;
}
return NIF$0 ( 5 , x , y , hh , gg ) ;
}
return NIF$1 ( 12 , gg , x , hh ) ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) java.lang.String {
b int = new x4simplelocalvarrefviaclass2levels.X ( ) . funto ( 5 ) ;
return "  " + b ;
}
}


~~~~~
//##5. simple localvar ref - funcref
{
somestat int = 9 ;
public def funto ( var x int ) ( ) out java.lang.Integer {
hh int = 9 ;
public def NIF$0 ( var y int , var x int , var hh int ) int {
return x + 7 + y + somestat + hh ;
}
return NIF$0 & ( 12 , x , hh ) ;
}
public def doings ( ) java.lang.String {
b java.lang.Integer = funto ( 5 ) ( ) ;
return "  " + b ;
}
}


~~~~~
//##6. simple localvar ref 2 levels- funcref
{
somestat int = 9 ;
public def funto ( var x int ) ( ) out java.lang.Integer {
hh int = 9 ;
gg int = 121 ;
public def NIF$1 ( var y int , var gg int , var x int , var hh int ) int {
gg += 1 ;
public def NIF$0 ( var z int , var x int , var y int , var hh int , var gg int ) int {
return x + 7 + y + somestat + hh + z + gg ;
}
return NIF$0 ( 5 , x , y , hh , gg ) ;
}
return NIF$1 & ( 12 , gg , x , hh ) ;
}
public def doings ( ) java.lang.String {
b java.lang.Integer = funto ( 5 ) ( ) ;
return "  " + b ;
}
}


~~~~~
//##7. simple localvar ref via class- funcref
{
somestat int = 9 ;
public class X {
jk int = 9 ;
public def funto ( var x int ) ( ) out java.lang.Integer {
hh int = 9 ;
public def NIF$0 ( var y int , var x int , var hh int ) int {
return x + 7 + y + somestat + hh + jk ;
}
return NIF$0 & ( 12 , x , hh ) ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) java.lang.String {
b java.lang.Integer = new x7simplelocalvarrefviaclassfuncref.X ( ) . funto ( 5 ) ( ) ;
return "  " + b ;
}
}


~~~~~
//##8. simple localvar ref via class - 2 levels- funcref
{
somestat int = 9 ;
public class X {
jk int = 9 ;
public def funto ( var x int ) ( ) out java.lang.Integer {
hh int = 9 ;
gg int = 121 ;
public def NIF$1 ( var y int , var gg int , var x int , var hh int ) int {
gg += 1 ;
public def NIF$0 ( var z int , var x int , var y int , var hh int , var gg int ) int {
return x + 7 + y + somestat + hh + z + gg + jk ;
}
return NIF$0 ( 5 , x , y , hh , gg ) ;
}
return NIF$1 & ( 12 , gg , x , hh ) ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) java.lang.String {
b java.lang.Integer = new x8simplelocalvarrefviaclass2levelsfuncref.X ( ) . funto ( 5 ) ( ) ;
return "  " + b ;
}
}


~~~~~
//##9.a - funcref of funcref nested inner
{
somestat int = 9 ;
public def funto ( var x int ) ( ) out java.lang.Integer {
hh int = 9 ;
gg int = 121 ;
public def NIF$1 ( var y int , var gg int , var x int , var hh int ) ( ) out java.lang.Integer {
gg += 1 ;
public def NIF$0 ( var z int , var x int , var y int , var hh int , var gg int ) int {
return x + 7 + y + somestat + hh + z + gg ;
}
return NIF$0 & ( 5 , x , y , hh , gg ) ;
}
return NIF$1 ( 12 , gg , x , hh ) & ( ) ;
}
public def doings ( ) java.lang.String {
b java.lang.Integer = funto ( 5 ) ( ) ;
return "  " + b ;
}
}


~~~~~
//##9.b - funcref of funcref nested inner
{
somestat int = 9 ;
public class X {
jk int = 9 ;
public def funto ( var x int ) ( ) out java.lang.Integer {
hh int = 9 ;
gg int = 121 ;
public def NIF$1 ( var y int , var gg int , var x int , var hh int ) ( ) out java.lang.Integer {
gg += 1 ;
public def NIF$0 ( var z int , var x int , var y int , var hh int , var gg int ) int {
return x + 7 + y + somestat + hh + z + gg + jk ;
}
return NIF$0 & ( 5 , x , y , hh , gg ) ;
}
return NIF$1 ( 12 , gg , x , hh ) & ( ) ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) java.lang.String {
b java.lang.Integer = new x9bfuncrefoffuncrefnestedinner.X ( ) . funto ( 5 ) ( ) ;
return "  " + b ;
}
}


~~~~~
//##10 - funcref of funcref nested inner
{
public def ranger ( var func ( java.lang.Integer ) out java.lang.Integer , var upTo int ) java.lang.String {
ret java.lang.String = "  " ;
for ( n = 0 ; n <== upTo ; n ++ ) {
ret += "  " + func ( n ) ;
ret += " ,  " ;
}
return ret ;
}
somestat int = 9 ;
public def funto ( var x int ) java.lang.String {
h int = 8 ;
public def NIF$0 ( var n int , var x int , var h int ) int {
if ( n == 0 ) {
return 0 ;
}
elif ( n == 1 ) {
return 1 ;
}
else {
return NIF$0 ( n - 1 , x , h ) + NIF$0 ( n - 2 , x , h ) + x + h + somestat ;
}
}
return ranger ( NIF$0 & ( ? int , x , h ) , 10 ) ;
}
public def doings ( ) java.lang.String {
b java.lang.String = funto ( 5 ) ;
return "  " + b ;
}
}


~~~~~
//##11 - nested class def -1
{
somestat int = 9 ;
public class X {
fromouter int = 99 ;
public class Y {
jk int = 9 ;
public def funto ( var x int ) int {
hh int = 9 ;
public def NIF$0 ( var y int , var x int , var hh int ) int {
return x + 7 + y + somestat + hh + jk + fromouter ;
}
return NIF$0 ( 12 , x , hh ) ;
}
public this ( ) {
super ( ) ;
}
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) java.lang.String {
b int = new x11nestedclassdef1.X ( ) . new x11nestedclassdef1.X.Y ( ) . funto ( 5 ) ;
return "  " + b ;
}
}


~~~~~
//##11 - nested class def -2
{
somestat int = 9 ;
public class X {
fromouter int = 99 ;
public class Y {
jk int = 9 ;
public def funto ( var x int ) int {
hh int = 9 ;
gg int = 121 ;
public def NIF$1 ( var y int , var x int , var hh int ) int {
public def NIF$0 ( var xxx int , var x int , var y int , var hh int ) int {
return x + 7 + y + somestat + hh + jk + fromouter + xxx + y ;
}
return NIF$0 ( 5 , x , y , hh ) ;
}
return NIF$1 ( 12 , x , hh ) ;
}
public this ( ) {
super ( ) ;
}
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) java.lang.String {
b int = new x11nestedclassdef2.X ( ) . new x11nestedclassdef2.X.Y ( ) . funto ( 5 ) ;
return "  " + b ;
}
}


~~~~~
//##12 - nested function inside rhs of field assignment
{
public class MyClass {
xx int = 99 ;
public def thsignsd ( ) int {
return xx + 100 ;
}
mc1 int = {
public def NIF$0 ( var a int ) int {
return 2 + a ;
}
NIF$0 ( 89 ) ;
}
;
public override equals ( var a java.lang.Object ) boolean {
return false ;
}
public override hashCode ( ) int {
return 1 ;
}
public def doings ( ) java.lang.String {
return "  " + mc1 ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) java.lang.String {
return new x12nestedfunctioninsiderhsoffieldassignment.MyClass ( ) . doings ( ) ;
}
}


~~~~~
//##13 - nested function inside rhs of field assignment with deps
{
public class MyClass {
xx int = 99 ;
public def thsignsd ( ) int {
return xx + 100 ;
}
mc1 java.lang.String = {
fr java.lang.String = " frank " ;
public def NIF$0 ( var a int , var fr java.lang.String ) java.lang.String {
return fr + 2 + a ;
}
NIF$0 ( 89 , fr ) ;
}
;
public override equals ( var a java.lang.Object ) boolean {
return false ;
}
public override hashCode ( ) int {
return 1 ;
}
public def doings ( ) java.lang.String {
return "  " + mc1 ;
}
public this ( ) {
super ( ) ;
}
}
public def doings ( ) java.lang.String {
return new x13nestedfunctioninsiderhsoffieldassignmentwithdeps.MyClass ( ) . doings ( ) ;
}
}


~~~~~
//##14. lambda gen bug on more than one nested function
{
public def lafunc ( var f java.lang.String ) NIC$1 {
private class NIC$1 ( protected a java.lang.String ) {
public this ( var a java.lang.String ) {
super ( ) ;
this \. a = a ;
}
protected a java.lang.String ;
v int = 99 ;
public override toString ( ) java.lang.String {
g int = 911 ;
public def NIF$0 ( var g int ) java.lang.String {
return " ok  " + g ;
}
rr ( ) java.lang.Comparable < java.lang.Object > [] = toref & ( " 12 " ) ;
rr2 ( ) java.lang.String = NIF$0 & ( g ) ;
return " MyClass:  " + [ NIF$0 ( g ) , rr ( ) ] ;
}
public def toref ( var g java.lang.String ) java.lang.Comparable < java.lang.Object > [] {
return [ 22   g ] ;
}
public override equals ( var a java.lang.Object ) boolean {
return false ;
}
public override hashCode ( ) int {
return 1 ;
}
}
return new NIC$1 ( " ok  " + f ) ;
}
public def doings ( ) java.lang.String {
return "  " + lafunc ( " there " ) ;
}
}


~~~~~
//##15. was a bug now its fine
{
public def MyClass ( var a int ) void {
public def NIF$0 ( var a int ) int {
return a + 100 ;
}
return ;
}
public def doings ( ) java.lang.String {
return " cool " ;
}
}