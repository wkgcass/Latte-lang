#LessTyping

![](http://lesstyping.cassite.net/images/highlight.png)

LessTyping is a JVM language. It's highly readable and extensible.

Click [here](http://lesstyping.cassite.net/#theVideo) to watch a video about LessTyping.

[Wiki Pages](https://github.com/wkgcass/LessTyping/wiki)  
[LessTyping WebSite](http://lesstyping.cassite.net/)

`Atom` Extensions :

[atom-lesstyping-highlighting](https://atom.io/packages/Atom-LessTyping-Highlighting)  
[atom-lesstyping-ide](https://atom.io/packages/atom-lesstyping-ide)

LessTyping supports 

* Operator Binding
* DSL
* Data Class
* Pre-Processing define/undef
* Inner Method
* Lambda
* JSON Literal
* Read Eval Print Loop
* many other features

`LessTyping` is based on java 8. It's compiled to JVM byte code, and can collaborate with any java library.

最下面有中文 ：）

#How to build

`JRE 8` is the only thing required. 

The project is managed by `Maven`, you can use `Maven 3` to build automatically

clone the repository, and run

	mvn clean package
	
You will get a runnable jar (`repl.jar`) and two shell scripts (`lesstyping` and `lesstyping.bat`). The shell scripts can help you run the `repl`.

run:

	./lesstyping
	
then the [REPL](https://github.com/wkgcass/LessTyping/blob/master/src/main/java/lt/repl/REPL.java) starts
    
	Welcome to LessTyping
	Type in expressions and double Enter to have them evaluated.
	Type :help for more information.
	for syntax help, please visit https://github.com/wkgcass/LessTyping/
    
    >1+1
    |
    res0 : java.lang.Integer = 2
    
    >

#Compile `lt` Files
There are two ways of compiling `lt` files

* use program command

		lesstyping -c <source-directory>
		
	the detailed commands and options can be found in
	
		lesstyping -help
		
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
	
	usually `filesInDirectory('...')` is used, e.g.
	
		compiler compile filesInDirectory('...')
	
	these method invocations can be chained up

		Compiler() + '...cp...' >> '...output...' compile filesInDirectory('...source...')
		
	You can write a `script` to configure the settings. Check [build.lts](https://github.com/wkgcass/LessTyping/blob/master/src/main/resources/build.lts) for more info.
	
#Scripts

* you can run a script directly

		lesstyping -s script-location script-arguments...
		
or:
	
* start the `REPL` interpreter  
	type `:script <script file>` and Enter

	then use `script run` or `script run ['string array']` to run the script
	
#Syntax
For Language Syntax Help, please visit the [Wiki Pages](https://github.com/wkgcass/LessTyping/wiki)

or visit the [LessTyping WebSite](http://lesstyping.cassite.net/)

#中文版 Chinese Version README

LessTyping是一种JVM编程语言。 它非常可读，同时也非常可扩展。

点击 [这里](http://lesstyping.cassite.net/#theVideo) 观看有关LessTyping的视频。

[Wiki Pages](https://github.com/wkgcass/LessTyping/wiki)  
[LessTyping 主页](http://lesstyping.cassite.net/)

`Atom`上的扩展插件:

[atom-lesstyping-highlighting](https://atom.io/packages/Atom-LessTyping-Highlighting)  
[atom-lesstyping-ide](https://atom.io/packages/atom-lesstyping-ide)

LessTyping 支持如下功能

* 运算符绑定
* DSL
* Data Class
* 预处理 define/undef
* 内部方法
* Lambda
* JSON 字面量
* Read Eval Print Loop
* 许多其它特性

`LessTyping`基于java8。它被编译到JVM字节码，可以与任何Java类库完美互通。

#如何构建工程

环境仅仅需要 `JRE 8`

本工程使用 `Maven` 进行管理，所以您也可以使用 `Maven 3` 进行自动Build

clone这个仓库,然后执行

	mvn clean package
	
你将会获取一个可执行jar (`repl.jar`) 以及两个shell脚本 (`lesstyping` 和 `lesstyping.bat`), shell脚本可以快捷地开启`repl`.
                       
执行:
                       
	./lesstyping
	
接着, [REPL](https://github.com/wkgcass/LessTyping/blob/master/src/main/java/lt/repl/REPL.java) 将开始运行
    
	Welcome to LessTyping
	Type in expressions and double Enter to have them evaluated.
	Type :help for more information.
	for syntax help, please visit https://github.com/wkgcass/LessTyping/
        
    >1+1
    |
    res0 : java.lang.Integer = 2
    
    >

#编译 `lt` 文件

* 使用程序命令

		lesstyping -c <source-directory>
		
	详细的命令与选项可以这样找到
	
		lesstyping -help

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
	
	通常来说会使用 `filesInDirectory('...')`, e.g.

		compiler compile filesInDirectory('/Users/me/src')
	
	这些方法调用可以被串联起来

		Compiler() + '...cp...' >> '...output...' compile filesInDirectory('...source...')
		
	您可以编写一个脚本 `script` 来配置这些属性。查看 [build.lts](https://github.com/wkgcass/LessTyping/blob/master/src/main/resources/build.lts) 以获取更多信息。
	
#Scripts
* 你可以直接运行脚本

		lesstyping -s script-file-path script-arguments...
		
或者：

*  开启 `REPL` 解释器  
	输入 `:script <script file>` 并回车

	然后使用 `script run` 或者 `script run ['string array']` 来运行这个脚本
	
#语法
您可以从这两个地方获取语法规则

[Wiki Pages](https://github.com/wkgcass/LessTyping/wiki)

[LessTyping 主页](http://lesstyping.cassite.net/)
