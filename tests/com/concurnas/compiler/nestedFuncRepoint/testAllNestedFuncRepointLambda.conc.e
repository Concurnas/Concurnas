//##0. zero capture case
{
somestat int = 9 ;
public def funto ( var x int ) int {
hh int = 9 ;
inner ( java.lang.Integer ) java.lang.Integer = def ( var y java.lang.Integer ) java.lang.Integer {
return 7 + y + somestat ;
}
;
return inner ( 12 ) ;
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
inner ( java.lang.Integer ) java.lang.Integer = def ( var y java.lang.Integer ) java.lang.Integer {
return x + 7 + y + somestat + hh ;
}
;
return inner ( 12 ) ;
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
inner ( java.lang.Integer ) java.lang.Integer = def ( var y java.lang.Integer ) java.lang.Integer {
gg += 1 ;
inner2 ( java.lang.Integer ) java.lang.Integer = def ( var z java.lang.Integer ) java.lang.Integer {
return x + 7 + y + somestat + hh + z + gg ;
}
;
return inner2 ( 5 ) ;
}
;
return inner ( 12 ) ;
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
inner ( java.lang.Integer ) java.lang.Integer = def ( var y java.lang.Integer ) java.lang.Integer {
return x + 7 + y + somestat + hh + jk ;
}
;
return inner ( 12 ) ;
}
public this ( ) {
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
inner ( java.lang.Integer ) java.lang.Integer = def ( var y java.lang.Integer ) java.lang.Integer {
gg += 1 ;
inner2 ( java.lang.Integer ) java.lang.Integer = def ( var z java.lang.Integer ) java.lang.Integer {
return x + 7 + y + somestat + hh + z + gg + jk ;
}
;
return inner2 ( 5 ) ;
}
;
return inner ( 12 ) ;
}
public this ( ) {
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
inner ( java.lang.Integer ) java.lang.Integer = def ( var y java.lang.Integer ) java.lang.Integer {
return x + 7 + y + somestat + hh ;
}
;
return inner & ( 12 ) ;
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
inner ( java.lang.Integer ) java.lang.Integer = def ( var y java.lang.Integer ) java.lang.Integer {
gg += 1 ;
inner2 ( java.lang.Integer ) java.lang.Integer = def ( var z java.lang.Integer ) java.lang.Integer {
return x + 7 + y + somestat + hh + z + gg ;
}
;
return inner2 ( 5 ) ;
}
;
return inner & ( 12 ) ;
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
inner ( java.lang.Integer ) java.lang.Integer = def ( var y java.lang.Integer ) java.lang.Integer {
return x + 7 + y + somestat + hh + jk ;
}
;
return inner & ( 12 ) ;
}
public this ( ) {
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
inner ( java.lang.Integer ) java.lang.Integer = def ( var y java.lang.Integer ) java.lang.Integer {
gg += 1 ;
inner2 ( java.lang.Integer ) java.lang.Integer = def ( var z java.lang.Integer ) java.lang.Integer {
return x + 7 + y + somestat + hh + z + gg + jk ;
}
;
return inner2 ( 5 ) ;
}
;
return inner & ( 12 ) ;
}
public this ( ) {
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
inner ( java.lang.Integer ) ( ) out java.lang.Integer = def ( var y java.lang.Integer ) ( ) out java.lang.Integer {
gg += 1 ;
inner2 ( java.lang.Integer ) java.lang.Integer = def ( var z java.lang.Integer ) java.lang.Integer {
return x + 7 + y + somestat + hh + z + gg ;
}
;
return inner2 & ( 5 ) ;
}
;
return inner ( 12 ) & ( ) ;
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
inner ( java.lang.Integer ) ( ) out java.lang.Integer = def ( var y java.lang.Integer ) ( ) out java.lang.Integer {
gg += 1 ;
inner2 ( java.lang.Integer ) java.lang.Integer = def ( var z java.lang.Integer ) java.lang.Integer {
return x + 7 + y + somestat + hh + z + gg + jk ;
}
;
return inner2 & ( 5 ) ;
}
;
return inner ( 12 ) & ( ) ;
}
public this ( ) {
}
}
public def doings ( ) java.lang.String {
b java.lang.Integer = new x9bfuncrefoffuncrefnestedinner.X ( ) . funto ( 5 ) ( ) ;
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
inner ( java.lang.Integer ) java.lang.Integer = def ( var y java.lang.Integer ) java.lang.Integer {
return x + 7 + y + somestat + hh + jk + fromouter ;
}
;
return inner ( 12 ) ;
}
public this ( ) {
}
}
public this ( ) {
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
inner ( java.lang.Integer ) java.lang.Integer = def ( var y java.lang.Integer ) java.lang.Integer {
inner2 ( java.lang.Integer ) java.lang.Integer = def ( var xxx java.lang.Integer ) java.lang.Integer {
return x + 7 + y + somestat + hh + jk + fromouter + gg + xxx ;
}
;
return inner2 ( 77 ) ;
}
;
return inner ( 12 ) ;
}
public this ( ) {
}
}
public this ( ) {
}
}
public def doings ( ) java.lang.String {
b int = new x11nestedclassdef2.X ( ) . new x11nestedclassdef2.X.Y ( ) . funto ( 5 ) ;
return "  " + b ;
}
}


~~~~~
//##12a - mix of lambda and funcref 
{
somestat int = 9 ;
public def funto ( var x int ) ( ) out java.lang.Integer {
hh int = 9 ;
gg int = 121 ;
inner ( java.lang.Integer ) ( ) out java.lang.Integer = def ( var y java.lang.Integer ) ( ) out java.lang.Integer {
gg += 1 ;
public def NIF$0 ( var z int , var x int , var y java.lang.Integer , var hh int , var gg int ) int {
return x + 7 + y + somestat + hh + z + gg ;
}
return NIF$0 & ( 5 , x , y , hh , gg ) ;
}
;
return inner ( 12 ) & ( ) ;
}
public def doings ( ) java.lang.String {
b java.lang.Integer = funto ( 5 ) ( ) ;
return "  " + b ;
}
}


~~~~~
//##12b - mix of lambda and funcref 
{
somestat int = 9 ;
public def funto ( var x int ) ( ) out java.lang.Integer {
hh int = 9 ;
gg int = 121 ;
public def NIF$0 ( var y int , var gg int , var x int , var hh int ) ( ) out java.lang.Integer {
gg += 1 ;
inner2 ( java.lang.Integer ) java.lang.Integer = def ( var z java.lang.Integer ) java.lang.Integer {
return x + 7 + y + somestat + hh + z + gg ;
}
;
return inner2 & ( 5 ) ;
}
return NIF$0 ( 12 , gg , x , hh ) & ( ) ;
}
public def doings ( ) java.lang.String {
b java.lang.Integer = funto ( 5 ) ( ) ;
return "  " + b ;
}
}