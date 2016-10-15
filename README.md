#Latte-Lang

![](http://latte-lang.org/images/highlight.png)

Latte is a JVM language. It's highly readable and extensible.

Click [here](http://latte-lang.org/index.html#theVideo) to watch a video about Latte.

[Syntax Specification (deprecated)](https://github.com/wkgcass/Latte-lang/blob/master/ltls-deprecated.md)  
[Syntax Mannual (NEW)](https://github.com/wkgcass/Latte-lang/blob/master/mannual-zh.md)  
[Latte WebSite](http://latte-lang.org/)

`Atom` Extensions :

[atom-latte-lang-highlighting](https://atom.io/packages/Atom-Latte-lang-Highlighting)  
[atom-latte-lang-ide](https://atom.io/packages/atom-latte-lang-ide)

`Maven` Plugin :

[latte-maven-plugin](#mvn-plugin)

##中文版戳[这里](#readme-ch)

###Latte supports

* Operator Binding
* DSL
* Data Class
* Pre-Processing define/undef
* Inner Method
* Lambda
* JSON Literal
* Regular Expression
* Generator Specifying
* Read Eval Print Loop
* Compiling to JavaScript (based on `Generator Specifying`)
* Latte Maven Plugin
* many other features

`Latte` is based on java 8. It's compiled to JVM byte code, and can collaborate with any java library.

#How to build

`JDK 8` is the only thing required.

The project is managed by `Maven`, you can use `Maven 3` to build automatically

clone the repository, and run

	mvn clean package

You will get two shell scripts (`latte` and `latte.bat`). The shell scripts can help you run the `repl`.

run:

	./latte

then the [REPL](https://github.com/wkgcass/Latte-lang/blob/master/latte-compiler/src/main/java/lt/repl/REPL.java) starts

	Welcome to Latte lang
	Type in expressions and double Enter to have them evaluated.
	Type :help for more information.
	for syntax help, please visit https://github.com/wkgcass/Latte-lang/

    lt> 1+1
    |
    res0 : java.lang.Integer = 2

    lt>

#Compile `lt` Files
There are two ways of compiling `lt` files

* use program command

		latte -c <source-directory>

	the detailed commands and options can be found in

		latte -help

or:

* start the `REPL` interpreter, and construct a `Compiler`

		compiler = Compiler()

	use `>>` operator to specify output directory

		compiler >> '...'
		// or
		compiler >> File('...')

	use `+` operator to add class-path

		compiler + '...'

	use `compile MAP` to specify source codes to be compiled and start compiling

		compiler compile {'fileName':FileObject}

	usually `filesInDirectory('...', regex)` is used, e.g.

		compiler compile filesInDirectory('...', //.*\.lt//)

	these method invocations can be chained up

		Compiler() + '...cp...' >> '...output...' compile filesInDirectory('...source...', //.*\.lt//)

	You can write a `script` to configure the settings. Check [build.lts.template](https://github.com/wkgcass/Latte-lang/blob/master/latte-compiler/src/main/resources/build.lts.template) for more info.

#Scripts

* you can run a script directly

		latte -s script-location script-arguments...

or:

* start the `REPL` interpreter  
	type `:script <script file>` and Enter

	then use `script run` or `script run ['string array']` to run the script

<h1 id='mvn-plugin'>Maven Plugin</h1>
A plugin for `Maven 3` is provided, which helps you compile latte source codes or run latte scripts.

###How to use
###step1
add the plugin configuration:

```xml
<plugin>
	<groupId>org.latte-lang</groupId>
	<artifactId>latte-maven-plugin</artifactId>
	<version>LATEST</version>
	<executions>
		<execution>
			<id>compile</id>
			<phase>compile</phase>
			<goals>
				<goal>compile</goal>
			</goals>
		</execution>
		<execution>
			<id>test-compile</id>
			<phase>test-compile</phase>
			<goals>
				<goal>test-compile</goal>
			</goals>
		</execution>
	</executions>
</plugin>
```

Not all executions are required. For example, you can omit the `test-compile` execution if the project only contains `main` source code.

###step2
create a folder named `latte` in the same parent directory. The directory tree should be:

	src
	├── main
	│   ├── java
	│   │   └── *.java    ; java source
	│   ├── latte
	│   │   └── *.lt      ; latte source
	│   └── resources
	│       │── *.lts     ; latte scripts
	│       └── other resources
	└── test
	    ├── java
	    │   └── *.java
	    ├── latte
	    │   └── *.lt
	    └── resources
	        ├── *.lts
	        └── other resources

###step3
run

	mvn clean package

###step4
you can also run latte scripts with the `latte-maven-plugin`.

run

	mvn clean latte:run -Dscript=<the script in classpath>

The `run` goal is bond to `test` phase, so all classes would be compiled and tested before executing the script.

>Note that the plugin ends as soon as the script main thread finishes. If you are running multiple thread application, a loop which blocks current thread should be explicitly given.  
>Or use api of the multiple thread application to block the thread, e.g. `jettyServer.join()`.

#Syntax
For Language Syntax Help, please visit the [Specification](https://github.com/wkgcass/Latte-lang/blob/master/ltls-deprecated.md) (However it's deprecated. You can still refer to it since the basic syntax changes are very few)

or visit the [Latte WebSite](http://latte-lang.org/)

or read the chinese version [mannual](https://github.com/wkgcass/Latte-lang/blob/master/mannual-zh.md)

<h1 id='readme-ch'>中文版 Chinese Version README</h1>

Latte是一种JVM编程语言。 它非常可读，同时也非常可扩展。

点击 [这里](http://latte-lang.org/index.html#theVideo) 观看有关Latte的视频。

[语法规则](https://github.com/wkgcass/Latte-lang/blob/master/mannual-zh.md)  
[Latte 主页](http://latte-lang.org/)

`Atom`上的扩展插件:

[atom-latte-lang-highlighting](https://atom.io/packages/Atom-Latte-lang-Highlighting)  
[atom-latte-lang-ide](https://atom.io/packages/atom-latte-lang-ide)

`Maven` Plugin :

[latte-maven-plugin](#mvn-plugin-ch)

###Latte 支持如下功能

* 运算符绑定
* DSL
* Data Class
* 预处理 define/undef
* 内部方法
* Lambda
* JSON 字面量
* 正则表达式
* 生成器指定
* Read Eval Print Loop
* 编译到JavaScript (基于`生成器指定`)
* Latte Maven Plugin
* 许多其它特性

`Latte`基于java8。它被编译到JVM字节码，可以与任何Java类库完美互通。

#如何构建工程

环境仅仅需要 `JDK 8`

本工程使用 `Maven` 进行管理，所以您也可以使用 `Maven 3` 进行自动Build

clone这个仓库,然后执行

	mvn clean package

你将会获取两个shell脚本 (`latte` 和 `latte.bat`), shell脚本可以快捷地开启`repl`.

执行:

	./latte

接着, [REPL](https://github.com/wkgcass/Latte-lang/blob/master/latte-compiler/src/main/java/lt/repl/REPL.java) 将开始运行

	Welcome to Latte lang
	Type in expressions and double Enter to have them evaluated.
	Type :help for more information.
	for syntax help, please visit https://github.com/wkgcass/Latte-lang/

    lt> 1+1
    |
    res0 : java.lang.Integer = 2

    lt>

#编译 `lt` 文件

* 使用程序命令

		latte -c <source-directory>

	详细的命令与选项可以这样找到

		latte -help

或者:

* 开启`REPL`, 然后构造一个`Compiler`

		compiler = Compiler()

	使用 `>>` 运算符来指定编译输出目录

		compiler >> '...'
		// or
		compiler >> File('...')

	使用 `+` 运算符来添加 class-path

		compiler + '...'

	使用 `compile MAP` 来确定源代码并立即开始编译

		compiler compile {'fileName':FileObject}

	通常来说会使用 `filesInDirectory('...', regex)`, e.g.

		compiler compile filesInDirectory('/Users/me/src', //.*\.lt//)

	这些方法调用可以被串联起来

		Compiler() + '...cp...' >> '...output...' compile filesInDirectory('...source...', //.*\.lt//)

	您可以编写一个脚本 `script` 来配置这些属性。查看 [build.lts.template](https://github.com/wkgcass/Latte-lang/blob/master/latte-compiler/src/main/resources/build.lts.template) 以获取更多信息。

#Scripts
* 你可以直接运行脚本

		latte -s script-file-path script-arguments...

或者：

*  开启 `REPL` 解释器  
	输入 `:script <script file>` 并回车

	然后使用 `script run` 或者 `script run ['string array']` 来运行这个脚本

<h1 id='mvn-plugin-ch'>Maven 插件</h1>
提供了一个`Maven 3`的插件， 这个插件可以用来编译和运行`latte`源文件和脚本（script）。

###如何使用
###step1
添加如下maven plugin配置：

```xml
<plugin>
	<groupId>org.latte-lang</groupId>
	<artifactId>latte-maven-plugin</artifactId>
	<version>LATEST</version>
	<executions>
		<execution>
			<id>compile</id>
			<phase>compile</phase>
			<goals>
				<goal>compile</goal>
			</goals>
		</execution>
		<execution>
			<id>test-compile</id>
			<phase>test-compile</phase>
			<goals>
				<goal>test-compile</goal>
			</goals>
		</execution>
	</executions>
</plugin>
```

并不是所有`execution`都是必须的。比如说，如果工程内只有`main`，你就可以省略`test-compile`。

###step2
在同一个上级目录中创建名称为`latte`的目录。目录结构树应当为：

	src
	├── main
	│   ├── java
	│   │   └── *.java    ; java source
	│   ├── latte
	│   │   └── *.lt      ; latte source
	│   └── resources
	│       │── *.lts     ; latte scripts
	│       └── other resources
	└── test
	    ├── java
	    │   └── *.java
	    ├── latte
	    │   └── *.lt
	    └── resources
	        ├── *.lts
	        └── other resources

###step3
运行

	mvn clean package

###step4
你可以使用 `latte-maven-plugin` 来执行脚本。
run

	mvn clean latte:run -Dscript=<the script in classpath>

`run` goal 绑定在 `test` 阶段，所以在执行前，所有的类都会被编译并测试。

>请注意该插件在脚本主线程结束时结束。如果你运行了一个多线程应用，请务必加上一个阻塞线程的循环。  
>或者使用多线程应用提供的api来阻塞现场，比如说 `jettyServer.join()`

# 语法
您可以从这两个地方获取语法规则

[语法规则](https://github.com/wkgcass/Latte-lang/blob/master/mannual-zh.md)

[Latte 主页](http://latte-lang.org/)
