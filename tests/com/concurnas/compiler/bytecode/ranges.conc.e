//##1. simple range
0 to 5

~~~~~
//##2. simple range non int
0 to 5

~~~~~
//##3. simple range parser precidence order correct
true

~~~~~
//##4. ax length is fine here
0 to 3

~~~~~
//##5. big test
1 to 10 -> [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
0 to 100 step 9 -> [0, 9, 18, 27, 36, 45, 54, 63, 72, 81, 90, 99]
0 to ... -> [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]
0 to ... step 10 -> [0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120]
10 to 1 -> [10, 9, 8, 7, 6, 5, 4, 3, 2, 1]
10 to ... step -2 -> [10, 8, 6, 4, 2, 0, -2, -4, -6, -8, -10, -12, -14]
10 to -6 step 2 -> [10, 8, 6, 4, 2, 0, -2, -4, -6]
-6 to 10 step 2 -> [-6, -4, -2, 0, 2, 4, 6, 8, 10]
FAILS:
step of -2 must be greater than 0 (unless sequence is infinite)
Only bounded sequences can be reversed
Ins
[true true true true true true true true]
extras:
[[A, B, C, D, E, F]]

~~~~~
//##6. big test long
1 to 10 -> [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
0 to 100 step 9 -> [0, 9, 18, 27, 36, 45, 54, 63, 72, 81, 90, 99]
0 to ... -> [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]
0 to ... step 10 -> [0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120]
10 to 1 -> [10, 9, 8, 7, 6, 5, 4, 3, 2, 1]
10 to ... step -2 -> [10, 8, 6, 4, 2, 0, -2, -4, -6, -8, -10, -12, -14]
10 to -6 step 2 -> [10, 8, 6, 4, 2, 0, -2, -4, -6]
-6 to 10 step 2 -> [-6, -4, -2, 0, 2, 4, 6, 8, 10]
FAILS:
step of -2 must be greater than 0 (unless sequence is infinite)
Only bounded sequences can be reversed
Ins
[true true true true true true true true]
extras:
[[A, B, C, D, E, F]]


