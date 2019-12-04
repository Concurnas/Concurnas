//##1. lets do operator overloading
[12.0+2.0i 2.0+2.0i 0.0+0.0i]
[16.0+6.0i 8.0-2.0i]
[48.0+8.0i 3.0+0.5i 0.0+0.0i]
[20736.0+16.0i]
[0.0+0.0i 1.0+1.0i]
[true true true true false false false false]
[nice and cool nice and number 69]
[1 - 3 1 ... ... 3]
{-->1, 69->2, check->3, x->4}
nice and one
{-->1, one->99}
[true true]
[6.0+6.0i -6.0-6.0i]
[-6.0-6.0i 6.0+6.0i]
[0.0+0.0i, 1.0+1.0i]
[well done well done: hi]

~~~~~
//##2. lets do operator overloadingon refs
[[12.0+2.0i 2.0+2.0i 0.0+0.0i]
[16.0+6.0i 8.0-2.0i]
[48.0+8.0i 3.0+0.5i 0.0+0.0i]
[20736.0+16.0i]
[0.0+0.0i 1.0+1.0i]
[true true true true false false false false]
[nice and cool nice and number 69]
[1 - 3 1 ... ... 3]
{-->1} nice and one
{-->1, one->99}
[true true]
[6.0+6.0i -6.0-6.0i]
[-6.0-6.0i 6.0+6.0i]
[0.0+0.0i false]
[well done well done: hi]]

~~~~~
//##3. lets do operator overloadingon refs with generic output
[12.0+2.0i 2.0+2.0i 0.0+0.0i]
[lout lout]
[lout lout lout]
[lout]
[lout lout]
[false true false true false true false true]
[lout lout]
[lout lout lout]
{_->2, check->3}
lout
{_->2}
[lout false]
[6.0+6.0i 6.0+6.0i]
[-6.0-6.0i -6.0-6.0i]
[0.0+0.0i lout]
[lout lout]

~~~~~
//##4. lets do operator overloadingon refs with generic input
[12.0+2.0i 2.0+2.0i 0.0+0.0i]
[strholdlout strholdlout]
[strholdlout strholdlout strholdlout]
[strholdlout]
[strholdlout strholdlout]
[false true false true false true false true]
[strholdlout strholdlout]
[strholdlout strholdlout strholdlout]
{_->2, check->3}
strholdlout
{_->2}
[strholdlout false]
[6.0+6.0i strholdlout]
[-6.0-6.0i strholdlout]
[0.0+0.0i strholdlout]
[strholdlout strholdlout]

~~~~~
//##5. operator overloading normal ish bools
[true, false, true, false, true]

~~~~~
//##6. operator overloading non eq assignemnt
[14.0+4.0i, 144.0+4.0i]

~~~~~
//##7. operator overloading escaped form
[12.0+2.0i 2.0+2.0i 0.0+0.0i]
[16.0+6.0i 8.0-2.0i]
[48.0+8.0i 3.0+0.5i 0.0+0.0i]
[20736.0+16.0i]
[0.0+0.0i 1.0+1.0i]
[true true true true false false false false]
[nice and cool nice and number 69]
[1 - 3 1 ... ... 3]
{-->1, 69->2, check->3, x->4}
nice and one
{-->1, one->99}
[true true]
[6.0+6.0i -6.0-6.0i]
[-6.0-6.0i 6.0+6.0i]
[0.0+0.0i 1.0+1.0i]
[well done well done: hi]

~~~~~
//##8. operator overloading map stuff in place
[{-->1, hi1->79, hi2->71, hi3->71, hi4->71, hi5->71}, 70, 71]

~~~~~
//##9. operator overloading map stuff with Integer convesion
[{-->1, a->10, hi1->19, hi2->11, hi3->11, hi4->11, hi5->11}, 10, 11]

~~~~~
//##10. operator overloading map stuff with sublist specifically
[1 - 3, 1 ..., ... 3]

~~~~~
//##11. op overloading cool string example that just works
[[f1->69cool, f1->69cool, f1->69cool, f1->69cool, f2->69coolxxx], 12.0-2.0i]

~~~~~
//##12. op overloading this is how we do inc and dec
[100.0, 98.0]

~~~~~
//##13. op overloading subtype etc
[12.0+2.0i 2.0+2.0i 0.0+0.0i]
[16.0+6.0i 8.0-2.0i]
[48.0+8.0i 3.0+0.5i 0.0+0.0i]
[20736.0+16.0i]
[0.0+0.0i 1.0+1.0i]
[true true true true false false false false]
[nice and cool nice and number 69]
[1 - 3 1 ... ... 3]
{-->1, 69->2, check->3, x->4}
nice and one
{-->1, one->99}
[true true]
[6.0+6.0i -6.0-6.0i]
[-6.0-6.0i 6.0+6.0i]
[0.0+0.0i 1.0+1.0i]
[well done well done: hi]

~~~~~
//##14. op overload assign
AssignOPOverload: 99

~~~~~
//##15. op overload assign override op overload
[AssignOPOverload: 10022 AssignOPOverload: 22]

~~~~~
//##16. op overload assign override op overload
[AssignOPOverload: 10022 AssignOPOverload: 22]

~~~~~
//##17 op overload if ref returned waitUntilSet
AssignOPOverload: 10022

~~~~~
//##18 op overload if ref returned waitUntilSet play nice with delete on return
AssignOPOverload: 10022

~~~~~
//##19 op overload sublists
[sub: 1 - 5 -> something1, sub: 1 ... -> something2, sub: ... 5 -> something3]

~~~~~
//##20. op overload ensure wait for ref set and delete called
AssignOPOverload: 10122

~~~~~
//##21. op overload ensure wait for sublist set and delete called
AssignOPOverload: 10122

~~~~~
//##22. op overload optional stuff still matches
AssignOPOverload: 10122

~~~~~
//##23. sublist extract with optional args
[ok ok ok][sub: 1 - 5 -> [], sub: 1 ...  -> [], sub: ... 5  -> []]

~~~~~
//##24. unassign op overload
[56 1000]

~~~~~
//##25. unassign op overload supress
[MyThing[56] 56 hiMyThing[56]]

~~~~~
//##26. unassign impl type as field
[MyThing[56] 56 hiMyThing[56]]

~~~~~
//##27. unassign with ref correctly
[56, 56, 56]

~~~~~
//##28. ignore unassign as it returns void
MyThing[56]

~~~~~
//##29. assign and unassign
100