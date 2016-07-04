#Latte-Lang

![](http://latte-lang.org/images/highlight.png)

Latte is a JVM language. It's highly readable and extensible.

Click [here](http://latte-lang.org/index.html#theVideo) to watch a video about Latte.

[Wiki Pages](https://github.com/wkgcass/Latte-lang/wiki)  
[Latte WebSite](http://latte-lang.org/)

`Atom` Extensions :

[atom-latte-lang-highlighting](https://atom.io/packages/Atom-Latte-lang-Highlighting)  
[atom-latte-lang-ide](https://atom.io/packages/atom-latte-lang-ide)

Latte supports 

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
* many other features

`Latte` is based on java 8. It's compiled to JVM byte code, and can collaborate with any java library.

最下面有中文 ：）

#How to build

`JDK 8` is the only thing required. 

The project is managed by `Maven`, you can use `Maven 3` to build automatically

clone the repository, and run

	mvn clean package
	
You will get a runnable jar (`repl.jar`) and two shell scripts (`latte` and `latte.bat`). The shell scripts can help you run the `repl`.

run:

	./latte
	
then the [REPL](https://github.com/wkgcass/Latte-lang/blob/master/src/main/java/lt/repl/REPL.java) starts
    
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
		
	You can write a `script` to configure the settings. Check [build.lts](https://github.com/wkgcass/Latte-lang/blob/master/src/main/resources/build.lts) for more info.
	
#Scripts

* you can run a script directly

		latte -s script-location script-arguments...
		
or:
	
* start the `REPL` interpreter  
	type `:script <script file>` and Enter

	then use `script run` or `script run ['string array']` to run the script
	
#Syntax
For Language Syntax Help, please visit the [Specification](https://github.com/wkgcass/Latte-lang/blob/master/ltls.md)

or visit the [Latte WebSite](http://latte-lang.org/)

#中文版 Chinese Version README

Latte是一种JVM编程语言。 它非常可读，同时也非常可扩展。

点击 [这里](http://latte-lang.org/index.html#theVideo) 观看有关Latte的视频。

[Wiki Pages](https://github.com/wkgcass/Latte-lang/wiki)  
[Latte 主页](http://latte-lang.org/)

`Atom`上的扩展插件:

[atom-latte-lang-highlighting](https://atom.io/packages/Atom-Latte-lang-Highlighting)  
[atom-latte-lang-ide](https://atom.io/packages/atom-latte-lang-ide)

Latte 支持如下功能

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
* 许多其它特性

`Latte`基于java8。它被编译到JVM字节码，可以与任何Java类库完美互通。

#如何构建工程

环境仅仅需要 `JDK 8`

本工程使用 `Maven` 进行管理，所以您也可以使用 `Maven 3` 进行自动Build

clone这个仓库,然后执行

	mvn clean package
	
你将会获取一个可执行jar (`repl.jar`) 以及两个shell脚本 (`latte` 和 `latte.bat`), shell脚本可以快捷地开启`repl`.
                       
执行:
                       
	./latte
	
接着, [REPL](https://github.com/wkgcass/Latte-lang/blob/master/src/main/java/lt/repl/REPL.java) 将开始运行
    
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
		
	您可以编写一个脚本 `script` 来配置这些属性。查看 [build.lts](https://github.com/wkgcass/Latte-lang/blob/master/src/main/resources/build.lts) 以获取更多信息。
	
#Scripts
* 你可以直接运行脚本

		latte -s script-file-path script-arguments...
		
或者：

*  开启 `REPL` 解释器  
	输入 `:script <script file>` 并回车

	然后使用 `script run` 或者 `script run ['string array']` 来运行这个脚本
	
#语法
您可以从这两个地方获取语法规则

[语法规范 (只写了英文的)](https://github.com/wkgcass/Latte-lang/blob/master/ltls.md)

[Latte 主页](http://latte-lang.org/)
