//##1. simple pulsars
called: [yup, yup, true, true]

~~~~~
//##2. repeat forever
called: [12345678910, true]

~~~~~
//##3. repeat until certain time
called: true

~~~~~
//##4. repeat certain number of times
called: [12345678910, true]

~~~~~
//##5. test schedule
called: [yup, yup, true, true, 21]

~~~~~
//##6. not negative intervals
java.lang.AssertionError: Specified interval may not be negative

~~~~~
//##7. submit in past immediate execution
called: [yup, true]

~~~~~
//##8. stop processor on stop for actor being called
called: [yup, true]

~~~~~
//##9. stop and start event handler as approperiate
called: [true, true]

~~~~~
//##10. on stop of pulsar, close all childern
good: [true, true]

~~~~~
//##11. simple pulsars - frozen
called: [yup, yup, true, true, 21]

~~~~~
//##12. repeat forever - frozen
called: [10, 12345678910, true]

~~~~~
//##13. repeat until certain time - frozen
called: [12345678910, true]

~~~~~
//##14. repeat certain number of times - frozen
called: [12345678910, true]

~~~~~
//##15. test schedule - frozen
called: [yup, yup, true, true, 21]

~~~~~
//##18. stop processor on stop for actor being called - frozen
called: [yup, true]

~~~~~
//##20. on stop of pulsar, close all childern
good: [true, true]

~~~~~
//##21. example useage from book
ok true
