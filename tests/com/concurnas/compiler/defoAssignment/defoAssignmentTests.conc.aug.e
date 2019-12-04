//##69. DefoAssignment - 1. Simple stuff
x69DefoAssignment1Simplestuff.conc line 5:5 The variable f may not have been initialized
x69DefoAssignment1Simplestuff.conc line 13:5 The variable f may not have been initialized
x69DefoAssignment1Simplestuff.conc line 31:5 The variable f may not have been initialized
 
~~~~~
//##69. DefoAssignment - 1. for loops dont result in defo assignment
x69DefoAssignment1forloopsdontresultindefoassignment.conc line 10:8 The variable j may not have been initialized
 
~~~~~
//##69. DefoAssignment - 1. for loops declare vars

~~~~~
//##69. DefoAssignment - 1. misc if missing var cos fwd ref its ok

~~~~~
//##69. DefoAssignment - 2 - nested blocks
x69DefoAssignment2nestedblocks.conc line 14:5 Resource specified in try with resource block must implement close method, 'null' does not
 
~~~~~
//##69. DefoAssignment - 2 - stuff must be set
x69DefoAssignment2stuffmustbeset.conc line 24:5 The variable f may not have been initialized
x69DefoAssignment2stuffmustbeset.conc line 33:5 The variable f may not have been initialized
  
~~~~~
//##69. DefoAssignment - 4. oh yeah, lambdas
x69DefoAssignment4ohyeahlambdas.conc line 4:59 The variable f may not have been initialized
  
~~~~~
//##69. DefoAssignment - 5. misc

~~~~~
//##70 return or exception in if chain gets ignored
x70returnorexceptioninifchaingetsignored.conc line 53:12 The variable g may not have been initialized
 
~~~~~
//##71 return or exception in try catch chain gets ignored

~~~~~
//##72 refs always start off assigned
x72refsalwaysstartoffassigned.conc line 11:1 The variable a may not have been initialized
 
~~~~~
//##73 assign existing non eq
x73assignexistingnoneq.conc line 11:1 The variable a may not have been initialized
 
~~~~~
//##74. bug - ensure that first arg in dot op is processed correctly
x74bugensurethatfirstargindotopisprocessedcorrectly.conc line 13:5 The variable a may not have been initialized

~~~~~
//##75. bug used to think call inside if was unset
