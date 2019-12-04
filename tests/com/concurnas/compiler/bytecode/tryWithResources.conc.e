//##1. basic try with resources
[TF:{[excepThrower] sup: 0:[]}, TT:{[excepThrower] sup: 1:[excepCloser]}, FT:{[excepCloser] sup: 0:[]}, FF:ok, 4]

~~~~~
//##2. basic but always throws
[F:{[excep] sup: 0:[]}, T:{[excep] sup: 1:[excepCloser]}, 2]

~~~~~
//##3. more than one to close
[
, TTF:{[excepThrower] sup: 1:[excepCloser1]}, 
, TTT:{[excepThrower] sup: 2:[excepCloser2, excepCloser1]}, 
, TFT:{[excepThrower] sup: 1:[excepCloser2]}, 
, TFF:{[excepThrower] sup: 0:[]}, 
, FTF:{[excepCloser1] sup: 0:[]}, 
, FTT:{[excepCloser2] sup: 1:[excepCloser1]}, 
, FFT:{[excepCloser2] sup: 0:[]}, 
, FFF:ok, 
, 16]

~~~~~
//##4. use of stuff to close inside the block itself
[
, TTF:{[excepThrower] sup: 1:[excepCloser1]}, 
, TTT:{[excepThrower] sup: 2:[excepCloser2, excepCloser1]}, 
, TFT:{[excepThrower] sup: 1:[excepCloser2]}, 
, TFF:{[excepThrower] sup: 0:[]}, 
, FTF:{[excepCloser1] sup: 0:[]}, 
, FTT:{[excepCloser2] sup: 1:[excepCloser1]}, 
, FFT:{[excepCloser2] sup: 0:[]}, 
, FFF:ok Closer:1,Closer:2, 
, 16]

~~~~~
//##5. try w resources returns stuff itself
[
, TTF:{[excepThrower] sup: 1:[excepCloser1]}, 
, TTT:{[excepThrower] sup: 2:[excepCloser2, excepCloser1]}, 
, TFT:{[excepThrower] sup: 1:[excepCloser2]}, 
, TFF:{[excepThrower] sup: 0:[]}, 
, FTF:{[excepCloser1] sup: 0:[]}, 
, FTT:{[excepCloser2] sup: 1:[excepCloser1]}, 
, FFT:{[excepCloser2] sup: 0:[]}, 
, FFF:Closer:1,Closer:2, 
, 16]

~~~~~
//##6. new things declared are respected as new
[
, TTF:{[excepThrower] sup: 1:[excepCloser1]}, 
, TTT:{[excepThrower] sup: 3:[excepCloser3, excepCloser2, excepCloser1]}, 
, TFT:{[excepThrower] sup: 2:[excepCloser3, excepCloser2]}, 
, TFF:{[excepThrower] sup: 0:[]}, 
, FTF:{[excepCloser1] sup: 0:[]}, 
, FTT:{[excepCloser3] sup: 2:[excepCloser2, excepCloser1]}, 
, FFT:{[excepCloser3] sup: 1:[excepCloser2]}, 
, FFF:[null, null], 
, 24]

~~~~~
//##7. try w resources on non assign operations
[TF:{[excepThrower] sup: 0:[]}, TT:{[excepThrower] sup: 1:[excepCloser]}, FT:{[excepCloser] sup: 0:[]}, FF:ok, 4]

~~~~~
//##8. try w resources on non assign operations x2
[TF:{[excepThrower] sup: 0:[]}, TT:{[excepThrower] sup: 2:[excepCloser, excepCloser]}, FT:{[excepCloser] sup: 1:[excepCloser]}, FF:ok, 8]

~~~~~
//##9. thing by itself
[TF:{[excepThrower] sup: 0:[]}, TT:{[excepThrower] sup: 1:[excepCloser]}, FT:{[excepCloser] sup: 0:[]}, FF:okCloser(false, 1):closed, 4]

~~~~~
//##10. works on refs
[TF:{[excepThrower] sup: 0:[]}, TT:{[excepThrower] sup: 1:[excepCloser]}, FT:{[excepCloser] sup: 0:[]}, FF:okCloser(false, 1):open, 4]

~~~~~
//##11. twr has catch block
[TF:ok, TT:{[excepCloser] sup: 0:[]}, FT:{[excepCloser] sup: 0:[]}, FF:ok, 4, 2, 0]

~~~~~
//##12. twr has finally block
[TF:{[excepThrower] sup: 0:[]}, TT:{[excepThrower] sup: 1:[excepCloser]}, FT:{[excepCloser] sup: 0:[]}, FF:ok, 4, 4]

~~~~~
//##13. twr has finally block not catching exception
[TF:{[excepThrower] sup: 0:[]}, TT:{[excepThrower] sup: 1:[excepCloser]}, FT:{[excepCloser] sup: 0:[]}, FF:ok, 4, 0, 4]

~~~~~
//##14. twr has finally block catching exception
[TF:ok, TT:{[excepCloser] sup: 0:[]}, FT:{[excepCloser] sup: 0:[]}, FF:ok, 4, 2, 4]

~~~~~
//##15. nesting works thank god
[TF:{[excepThrower] sup: 0:[]}, TT:{[excepThrower] sup: 1:[excepCloser]}, FT:{[excepCloser] sup: 0:[]}, FF:ok, 4, 0, 4, 4]

~~~~~
//##16. nesting works thank god no ret
[TF:{[excepThrower] sup: 0:[]}, TT:{[excepThrower] sup: 1:[excepCloser]}, FT:{[excepCloser] sup: 0:[]}, FF:ok, 4, 0, 4, 4]

~~~~~
//##17. basic with block
[TTF:{[excepThrower] sup: 0:[]}, TTT:{[excepThrower] sup: 1:[onexit]}, TFT:{[excepThrower] sup: 1:[onexit]}, TFF:{[excepThrower] sup: 0:[]}, FTF:ok, FTT:{[onexit] sup: 0:[]}, FFT:{[onexit] sup: 0:[]}, FFF:ok, 8, 0]

~~~~~
//##18. nested with returns
[TTF:{[excepThrower] sup: 0:[]}, TTT:{[excepThrower] sup: 1:[onexit]}, TFT:{[excepThrower] sup: 1:[onexit]}, TFF:{[excepThrower] sup: 0:[]}, FTF:ok, FTT:{[onexit] sup: 0:[]}, FFT:{[onexit] sup: 0:[]}, FFF:ok, 8, 0, 8]

~~~~~
//##19. nested with does not return
[TTF:{[excepThrower] sup: 0:[]}, TTT:{[excepThrower] sup: 1:[onexit]}, TFT:{[excepThrower] sup: 1:[onexit]}, TFF:{[excepThrower] sup: 0:[]}, FTF:ok, FTT:{[onexit] sup: 0:[]}, FFT:{[onexit] sup: 0:[]}, FFF:ok, 8, 0, 8]

~~~~~
//##20. normal non multi catch
[got expceted: e1, got expceted: e2, mystery exception: xxx]

~~~~~
//##21. multi catch
[got expceted: e1, got expceted: e2, mystery exception: xxx]

~~~~~
//##22. bugfix on finaly used to wipe out return type on last thing of ret
[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

~~~~~
//##23. while bugfix 1 norm
66

~~~~~
//##24. while bugfix wrong label previously
[[ok0, ok1, ok2, ok3], nope]

~~~~~
//##25. was a bug now ok
excepCloser

~~~~~
//##26. many instanceof capture on same line
[true, true, true, true, true, true, true, true]

~~~~~
//##27. double dot
[12-33, hi, 12-33, 2, 2]

~~~~~
//##28. double dot weird useage
[12, 99]

~~~~~
//##29. bugfix on try catch inside block
lovelycoolhi

~~~~~
//##30. bugfix on try catch inside block more advanced
lovelycoolhi

~~~~~
//##31. used to blow up
110

~~~~~
//##32. bug with state restoration before
coolhi

~~~~~
//##33. more bug with state restoration before
lovelycoolhi

~~~~~
//##34. more bug with state restoration before x2
lovelycoolhi