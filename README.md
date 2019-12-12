<img width="80" src="https://github.com/Concurnas/Concurnas/blob/master/logo.png">

# The Concurnas programming Language
Concurnas is an open source programming language designed for building reliable, scalable, high performance concurrent, distributed and parallel systems

**The main website (including documentation) for Concurnas can be found at: [http://concurnas.com](http://concurnas.com)**

**Discord: [https://discordapp.com/invite/jFHfsqR](https://discordapp.com/invite/jFHfsqR)**

## Authors and major contributors
* [Jason Tatton](http://concurnas.com/concurnasltd/leadership.html), [jason.tatton@concurnas.com](jason.tatton@concurnas.com) - Founder of Concurnas

## Found a bug?
 1. Check the [existing issues](https://github.com/Concurnas/Concurnas/issues)
 2. Talk to us at [Concurnas Ltd](http://concurnas.com/concurnasltd/contact.html)
 3. Raise an [issue](https://github.com/Concurnas/Concurnas/issues)

### Reporting Security Issues
Please do not report security issues to the public issue tracker. Please send security issues to  [security@concurnas.com](mailto:security@concurnas.com).

## Want to contribute?
Before starting to work on a feature or a fix, please open an issue to discuss the use case or bug with us. This can save both you and us a lot of time. For any non-trivial change, we'll ask you to create a short design document explaining:

-   Why is this change done? What's the use case?
-   What will the API look like? (For new features)
-   What test cases should it have? What could go wrong?
-   How will it roughly be implemented? (We'll happily provide code pointers to save you time)

This can be done directly inside the GitHub issue or (for large changes) you can share a Google Doc with [us](http://concurnas.com/concurnasltd/contact.html).


### Contributing
We are friendly to pull requests and the team at Concurnas Ltd. will assist you in anyway we can. In order to protect yourself and other contributors to Concurnas all contributors to Concurnas must formally agree to abide by the Developer's Certificate of Origin, by signing on the bottom of the document.

To contribute:
 1. Fork the Concurnas github repository
 2. Make your changes
 3. **first time contributors: sign [contributors.txt](https://github.com/Concurnas/Concurnas/blob/master/contributors.txt) by adding your github userid, full name, email address (you can obscure your e-mail, but it must be computable by human), and date.*
 4. Commit your changes
 5. Send a pull request
 6. After you have signed once, you don't have to sign future pull requests. We can merge by checking to see your name is in the contributors file.

### Code Change Guidelines
All code contributions should contain the following:
* Appropriate Unit Tests (or modifications of existing tests if they are erroneous)
* All new and existing unit tests must pass.
* *If appropriate*: Updates to the reference manual (we will republish the ebook and update the [Concurnas website](http://concurnas.com/docs/manual.html))

Your code needs to run on all supported Java versions (at least 1.8) and operating systems (Windows and Linux). We will verify this, but here are some pointers that will avoid surprises:

* Be careful when using features introduced in Java 1.9 or later (modules etc), Concurnas is Java 1.8 compliant.
* Normalize file paths in tests.
* Watch out for Linux vs windows non incompatibilities: path separators, newline's etc.

After pull request acceptance we will manage the release process.

## Building Concurnas from scratch
The build process for Concurnas is more involved than for typical projects since much of Concurnas itself is written in Concurnas! As such an iterative build is employed which bootstraps us to the point where Concurnas can compile the remaining parts of itself. Luckily for us this iterative build is managed by Gradle.

The following commands can be used in order to build Concurnas from scratch:

### Windows
    gradlew clean build -x test   
	
### Linux
*first install gradle if you have not already done so...*

	gradle clean build -x test   

*It is recommended that one skip the automatic running of tests unless one has a machine which is powerful enough to run them.*

This will output a release zip which will look like: **Concurnas-1.13.108.zip**

### Recommended specs for machine to run test suite
 - Min:
	 - Java 1.8
	 - 4-core CPU
	 - 16GB RAM
	 - A GPU - NVIDIA GTX 590
- Recommended:
	- Java 1.8 and Java 1.9 - *some unit tests behave differently under each version*
	- 8-core CPU
	- 32GB RAM
	- A GPU - NVIDIA GTX 1060 3 GB

## Developing/building via an IDE
Using an IDE to make changes to Concurnas is recommended.
### Development in eclipse
The first time setup of Concurnas for eclipse is quite involved. 
#### Plugins required:
- ANTLR 4 IDE (0.3.6). (available in eclipse marketplace). Configured as follows:

![antlrSetup](https://github.com/Concurnas/Concurnas/blob/master/tools/eclipse/antlrSetup.png)

#### Additional plugins recommended:
 - Bytecode Outline (available in eclipse marketplace)

#### Generating eclipse configuration:
1. Either:

    ./gradlew eclipse
   
	or import the project into eclipse as a gradle project.
2. You may need to force the ANTLR plugin (configured as above) to detect the .g files under src\main\antlr\com\concurnas for the first time by opening them and re-saving them (this will clear up any errors about missing Visitors etc).

#### Generating remaining Concurnas Code:
Run the following code in eclipse (after each clean build) in order to complete the build:

 1. Generate the runtime cache:

	    com.concurnas.runtimeCache.RuntimeCacheCreator

 2. Compile the Concurnas libraries written in Concurnas:

	    com.concurnas.build.LibCompilation

 3. Compile the unit test helpers written in Concurnas:
 
		com.concurnas.concc.Concc -d ./bin/test ./tests[com/concurnas/tests/helpers]
#### Running unit tests:
It's recommended that you take the time to setup and run the unit tests within eclipse as it has good JUnit integration and you don't have to switch applications to run them interrupting your work flow. All the unit tests can be run via the following command *as a JUnit test suite* in eclipse:

		com.concurnas.compiler.AllTests
Specify the contents of the following file as VM arguments in order to run them correctly:

If running on Java 1.8: [vmArguments-Java8.txt](https://github.com/Concurnas/Concurnas/blob/master/tools/eclipse/vmArguments-Java8.txt)

If running on Java 9+: [vmArguments-Java9.txt](https://github.com/Concurnas/Concurnas/blob/master/tools/eclipse/vmArguments-Java9.txt)

#### The sandbox unit test:
The sandbox unit test is a nice way of testing Concurnas code end to end, it also provides nice profiling stats on the phases of Concurnas compilation:
 1. Edit this file: [bytecodeSandbox.conc](https://github.com/Concurnas/Concurnas/blob/master/tests/com/concurnas/compiler/bytecode/bytecodeSandbox.conc)
 2. Run this unit test:

		com.concurnas.compiler.bytecode.BytecodeTestJustSandbox
With the aforementioned VM arguments specified contingent upon your JDK.
