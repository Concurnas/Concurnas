//##1 basic maps default takes function with one arg
hi 200

~~~~~
//##2 default maps check type if not type then null
hi [4, 104, true]

~~~~~
//##3 default maps convertion on input only
[4, 104]

~~~~~
//##4 default maps convertion on output only
[122, 104]

~~~~~
//##5 default maps drop arg no conv
[100, 100]

~~~~~
//##6 default maps drop arg conv input
[100, null]

~~~~~
//##7 default maps drop arg conv output
[true, 100]

~~~~~
//##8 default maps drop arg conv in and out
[100, true]

~~~~~
//##9 default maps no func no conv
[2, null, 102]

~~~~~
//##10 default maps no func conv both
[2, true, 102]

~~~~~
//##11 default maps no func conv output
[2, true, 102]

~~~~~
//##12 default maps no func conv intput
[2, true, 102]

~~~~~
//##13 default maps no func can explicitly return null
[false, true]

~~~~~
//##14 ensure invokation only called once
[2, hi, hi, 1]

~~~~~
//##15 ret to a funcref
[12, 21]

~~~~~
//##16 ret to a funcref alt form
[12, 21]

~~~~~
//##17 generic upper bound bugfix
[12, 99]

~~~~~
//##18 default map typical use
hi {10->111}

~~~~~
//##19 use map creation function
{1->3, 2->6, 3->4, 4->3}

~~~~~
//##20 bugfix on generic type inferrence when arg is functype
ok

~~~~~
//##21. infer generics for default map
{1->3, 2->6, 3->4, 4->3}

~~~~~
//##22. null gotcha
java.lang.NullPointerException

~~~~~
//##23. this is fine
{1->2}

~~~~~
//##24. this is also fine
{1->11}

~~~~~
//##25. string keys
1

~~~~~
//##26. string keys
nested value

~~~~~
//##27. refname as string
twelve

~~~~~
//##28. refname mixed with string
anotherOne

~~~~~
//##29. gotcha maps mut key state
(9, null)

~~~~~
//##30. del on maps
{4->88}

~~~~~
//##31. maps with lambda inference of types default
{1->3, 2->6, 3->4, 4->3}