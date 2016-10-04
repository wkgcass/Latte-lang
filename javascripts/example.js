$(document).ready(function () {
    // create sidebar and attach to menu open
    $('.ui.sidebar').sidebar('attach events', '.toc.item');

    var app = angular.module('example', []);
    app.controller('controller', ['$scope', '$sce', function ($scope, $sce) {
        var zh = useZh();

        $scope.navs = common_navs();
        $scope.navs[4].active = true;

        $scope.git_repo = common_git_repo();

        $scope.showExample = 0;

        $scope.examples = [
            {
                title: zh ? '你好, Latte' : "Hello! Latte",
                content: zh ? $sce.trustAsHtml('' +
                    '<p>进入REPL交互程序,你将会看到 " <code>lt&gt;&nbsp;</code> "</p>' +
                    '<p>在右侧输入语句或表达式, 并按两次回车, REPL将编译并执行输入的表达式</p>' +
                    '<p>输入 ' + highlighting('test.lt', "println('Hello Latte')", {}) + '</p><p>控制台将打印 <code>Hello Latte</code></p>')
                    : $sce.trustAsHtml('' +
                    '<p>Launch the REPL, you will see " <code>lt&gt;&nbsp;</code> "</p>' +
                    '<p>Type statements or expressions and double Enter, REPL will compile and execute the inputs.</p>' +
                    '<p><code>lt&gt;&nbsp;</code>' + highlighting('test.lt', "println('Hello Latte')", {}) + '</p><p>then the console logs: <code>Hello Latte</code></p>')
            },
            {
                title: zh ? '九九乘法表' : 'Multiplication Table',
                content: $sce.trustAsHtml('' +
                    (zh ? '<p>我们想得到的结果是这样的:</p>' : '<p>We want the following output:</p>') +
                    '<pre>' +
                    '1*1=1\n' +
                    '1*2=2   2*2=4\n' +
                    '1*3=3   2*3=6   3*3=9\n' +
                    '1*4=4   2*4=8   3*4=12  4*4=16\n' +
                    '1*5=5   2*5=10  3*5=15  4*5=20  5*5=25\n' +
                    '1*6=6   2*6=12  3*6=18  4*6=24  5*6=30  6*6=36\n' +
                    '1*7=7   2*7=14  3*7=21  4*7=28  5*7=35  6*7=42  7*7=49\n' +
                    '1*8=8   2*8=16  3*8=24  4*8=32  5*8=40  6*8=48  7*8=56  8*8=64\n' +
                    '1*9=9   2*9=18  3*9=27  4*9=36  5*9=45  6*9=54  7*9=63  8*9=72  9*9=81' +
                    '</pre>' +
                    (zh ? '<p>只需双重循环即可完成</p>' : '<p>Simply run a nested `for` loop</p>') +
                    '<pre>' + highlighting('test.lt', "" +
                        "for i in 1..9\n" +
                        "    for j in 1..i\n" +
                        "        print('${j}*${i}=${j*i}\\t'\n" +
                        "    println()"
                        , {}) +
                    '</pre>' +
                    (zh ? '<p>这条循环语句也可以在REPL中完成. 在全部输入完成后输入两次回车即可得到结果</p>'
                        : '<p>The loop be written in REPL. Double Enter to have the statement evaluated.</p>') +
                    (zh ? (
                    '<p>其中 <code>1..9</code> 表示构建一个从1到9(包括)的列表</p>' +
                    '<p>类似的还有 <code>1.:9</code> 表示一个从1到9(不包括)的列表</p>')
                        : (
                    '<p><code>1..9</code> represents a List ranges from 1 to 9(inclusive)</p>' +
                    '<p>Similarly, <code>1.:9</code> represents a List ranges from 1 to 9(exclusive)</p>'))
                )
            },
            {
                title: 'Rational',
                content: $sce.trustAsHtml('' +
                    (zh ? '<p>Rational表示分数, 通过这个例子, 我们可以得知如何<b>覆写方法</b>以及<b>绑定运算符</b></p>'
                        : '<p>We will learn how to <b>override methods</b> and how to <b>bind operators</b></p>') +
                    (zh ? '<p>首先, 要构造Rational则需要两个整数,分别表示分子和分母</p>'
                        : '<p>Constructing a Rational requires two integers, which represents the numerator and the denominator.</p>') +
                    '<pre>' + highlighting('test.lt', "" +
                        "class Rational(n:int, d:int)"
                        , {}) +
                    '</pre>' +
                    (zh ? (
                        '<p>定义了一个类, 它拥有一个"带两个整数参数"的构造函数</p>' +
                        '<p>在 <code>Latte</code> 中,构造函数的参数将直接视为字段(Field)</p>' +
                        '<p>同时,构造块中定义的变量同样视为字段. 在 <code>Latte</code> 中字段默认为私有, 所以不用担心这些值被污染</p>'
                    )
                        : (
                        '<p>defines a class, who has a constructor with two integer parameters</p>' +
                        '<p>In <code>Latte</code>, parameters in constructor are considered as Fields.</p>' +
                        '<p>And variables in constructing block are considered as Fields. These fields won\'t be polluted, because Fields are private as default in <code>Latte</code>.</p>'
                    )) +
                    '<br>' +
                    (zh ? '<p>输入时需要限定分母不为0, 在分母为0时需要抛出异常</p>'
                        : '<p>An exception should be thrown if the denominator is 0.</p>') +
                    '<pre>' + highlighting('test.lt', "" +
                        "if d is 0\n" +
                        "    throw IllegalArgumentException(\"divider cannot be 0\")"
                        , {}) +
                    '</pre>' +
                    '<br>' +
                    (zh ? '<p>现在可以确保任何输入都是合法的了. 如何在控制台看到这些值呢? 我们需要重载 <code>toString</code> 方法</p>'
                        : '<p>Now, all inputs are legal. How to see these values from console? We should override <code>toString</code> method.</p>') +
                    '<pre>' + highlighting('test.lt', "" +
                        "toString():String = n + (\n" +
                        "    if d==1\n" +
                        "        return \"\"\n" +
                        "    return \"/\" + d)"
                        , {}) +
                    '</pre>' +
                    (zh ? '<p>重载了Object类的toString()方法, 这样就可以直接在控制台打印Rational对象了, 结果类似于 <code>1/2</code></p>'
                        : '<p>It overrides toString() methods of class Object, then we can print Rational objects to console, the result looks like <code>1/2</code></p>') +
                    '<br>' +
                    (zh ? '<p>输入有可能可以约分, 我们需要增加一个gcd(int,int)函数来获取最大公约数</p>'
                        : '<p>The inputs might be reduced, we need to add a method `gcd(int,int)` to retrieve the greatest common divisor.</p>') +
                    '<pre>' + highlighting('test.lt', "" +
                        "static\n" +
                        "    private gcd(a:int, b:int)\n" +
                        "        if b\n" +
                        "            return gcd(b, a % b)\n" +
                        "        else\n" +
                        "            return a"
                        , {}) +
                    '</pre>' +
                    (zh ? (
                            '<p>定义了一个静态gcd(int,int)方法来获取最大公约数</p>' +
                            '<p>在构造块中对传入的数进行约分</p>'
                        ) : (
                            '<p>It defines a static gcd(int,int) method to get the gcd.</p>' +
                            '<p>Reduce the inputs in constructing block:</p>'
                        )
                    ) +
                    '<pre>' + highlighting('test.lt', "" +
                        "neg= n * d < 0\n" +
                        "n=Math.abs(n)\n" +
                        "d=Math.abs(d)\n" +
                        "g=gcd(n,d)\n" +
                        "n/=g\n" +
                        "d/=g\n" +
                        "\n" +
                        "if neg\n" +
                        "    n=-n"
                        , {}) +
                    '</pre>' +
                    (zh ? '<p>现在, 所有Rational都是最简形式</p>'
                        : '<p>Now, all Rationals are reduced to the minimum form.</p>') +
                    '<br>' +
                    (zh ? '<p>我们还需要对Rational进行比较, 所以需要重写Object类的equals方法. equals方法和hashCode方法通常一起重写,以便<code>HashMap</code>之类的类使用</p>'
                        : '<p>We need to compare between Rationals, so we should override equals method of Object. Equals and hashCode usually are overridden together, so that they can be used in <code>HashMap</code></p>') +
                    '<pre>' + highlighting('test.lt', "" +
                        "equals(o):bool\n" +
                        "    if o is type Rational\n" +
                        "        return o.n==n and o.d==d\n" +
                        "    else\n" +
                        "        return false\n" +
                        "\n" +
                        "hashCode():int\n" +
                        "    return n+d"
                        , {}) +
                    '</pre>' +
                    (zh ? '<p>现在可以在 <code>Latte</code> 中使用 <code>==</code> 和 <code>!=</code> 运算符比较Rational了</p>'
                        : '<p>Now you can use <code>==</code> and <code>!=</code> operators to compare between Rational in <code>Latte</code></p>') +
                    '<br>' +
                    (zh ? '<p>接下来是运算符绑定</p>' +
                        '<p>最基本的运算有 <code>+ - * /</code>, 我们目标是使用运算符来计算Rational的值</p>'
                            : '<p>Then there\'s operator binding.</p>' +
                        '<p>The operators we want to override are <code>+ - * /</code>, the goal is to use them to calculate the Rational value.</p>'
                    ) +

                    '<pre>' + highlighting('test.lt', "" +
                        "add(o:Rational):Rational = Rational(n*o.d+o.n*d, d*o.d)\n" +
                        "subtract(o:Rational):Rational = Rational(n*o.d-o.n*d, d*o.d)\n" +
                        "multiply(o:Rational):Rational = Rational(n*o.n, d*o.d)\n" +
                        "divide(o:Rational):Rational = Rational(n*o.d, d*o.n)"
                        , {}) +
                    '</pre>' +
                    (zh ? (
                        '<p>运算符绑定非常简单, 写出符合签名的方法即可完成运算符的绑定. 现在可以使用 <code>+ - * /</code> 来进行Rational的运算了!</p>' +
                        '<p>至此我们完成了Rational类的构造, 不过还可以做一些小小的修改, 比如说除数默认为1. 需要将类构造参数修改为:</p>'
                    ) : (
                        '<p>The operator binding is very simple, just write methods with correct signatures. Now you can use <code>+ - * /</code> to calculate Rationals!</p>' +
                        '<p>So far, we finished the definition of class Rational. However we can make some modifications, e.g. the denominator is 1 as default. Constructing parameters should be changed into:</p>'
                    )) +
                    '<pre>' + highlighting('test.lt', "" +
                        "class Rational(n:int, d:int=1)"
                        , {}) +
                    '</pre>' +
                    (zh ? '<p>设置了一个参数默认值</p>' : '<p>A parameter default value is set.</p>') +
                    '<br>' +
                    (zh ? '<p>Rational类所有代码如下</p>' : '<p>The Rational source code:</p>') +
                    '<pre>' + highlighting('test.lt', "" +
                        "class Rational(n:int, d:int=1)\n" +
                        "    if d is 0\n" +
                        "        throw IllegalArgumentException(\"divider cannot be 0\")\n" +
                        "\n" +
                        "    neg= n * d < 0\n" +
                        "    n=Math.abs(n)\n" +
                        "    d=Math.abs(d)\n" +
                        "    g=gcd(n,d)\n" +
                        "    n/=g\n" +
                        "    d/=g\n" +
                        "\n" +
                        "    if neg\n" +
                        "        n=-n\n" +
                        "\n" +
                        "    toString():String = n + (\n" +
                        "        if d==1\n" +
                        "            return  \"\"\n" +
                        "        return  \"/\" + d)\n" +
                        "\n" +
                        "    equals(o):bool\n" +
                        "        if o is type Rational\n" +
                        "            return o.n==n and o.d==d\n" +
                        "        else\n" +
                        "            return false\n" +
                        "\n" +
                        "    hashCode():int\n" +
                        "        return n+d\n" +
                        "\n" +
                        "    add(o:Rational):Rational = Rational(n*o.d+o.n*d, d*o.d)\n" +
                        "    subtract(o:Rational):Rational = Rational(n*o.d-o.n*d, d*o.d)\n" +
                        "    multiply(o:Rational):Rational = Rational(n*o.n, d*o.d)\n" +
                        "    divide(o:Rational):Rational = Rational(n*o.d, d*o.n)\n" +
                        "\n" +
                        "    static\n" +
                        "        private gcd(a:int, b:int)\n" +
                        "            if b\n" +
                        "                return gcd(b, a % b)\n" +
                        "            else\n" +
                        "                return a"
                        , {}) +
                    '</pre>' +
                    (zh ? '<p>为了测试Rational类,我们可以写一个TestRational</p>'
                        : '<p>We can write a TestRational to test class Rational.</p>') +
                    '<pre>' + highlighting('test.lt', "" +
                        "class TestRational\n" +
                        "    static\n" +
                        "        testAdd(a:Rational, b:Rational)=a + b\n" +
                        "        testSubtract(a:Rational, b:Rational)=a - b\n" +
                        "        testMultiply(a:Rational, b:Rational)=a * b\n" +
                        "        testDivide(a:Rational, b:Rational)=a / b\n" +
                        "        testEquals(a:Rational, b:Rational)= a==b"
                        , {}) +
                    '</pre>' +
                    '<blockquote>' + (zh ? '关于Rational的测试用例可以在' : 'You can find the test case of Rational in') + ' <a target="_blank" href="https://github.com/wkgcass/Latte-lang/blob/master/latte-compiler/src/test/java/lt/compiler/cases/TestDemo.java">TestDemo</a> ' + (zh ? '中找到' : '') + '</blockquote>'
                )
            },
            {
                title: 'Html',
                content: $sce.trustAsHtml('' +
                    (zh ? (
                    '<p>在scala中原生支持xml字面量, 在kotlin中也可以方便的书写html. 作为一款同样灵活的语言, Latte也可以使用标准语法书写html</p>' +
                    '<p>我们的目标是:用如下代码</p>')
                        : (
                        '<p>XML literals are supported in scala, and HTML can be written easily in Kotlin. As a flexible language, Latte also can write HTML with basic syntax.</p>' +
                        '<p>Our goal is : using the following code</p>'
                    )) +
                    '<pre>' + highlighting('html.lt',
                        'html + [\n' +
                        '    head\n' +
                        '    body + [\n' +
                        '        form + [\n' +
                        '            input(typ="text" value="value")\n' +
                        '            input(typ="submit" value="OK")\n' +
                        '        ]\n' +
                        '    ]\n' +
                        '] toString', {}
                    ) +
                    '</pre>' +
                    (zh ? '<p>生成如下HTML</p>' : '<p>to generate the following HTML</p>') +
                    '<textarea style="width:100%;resize: none;border: none;" disabled>' +
                    '<html><head></head><body><form><input type="text" value="value"><input type="submit" value="OK"></form></body></html>' +
                    '</textarea><br><br>' +
                    (zh ? (
                        '<p>通过本示例您可以了解有关<code>data class</code>的用法</p>' +
                        '<p>应当使用 <code>data class</code> 来模拟html的标签, 并通过列表<code>[]</code>来模拟html的层级关系</p>' +
                        '<p>由于Latte的列表可以使用换行分割, 所以非常符合HTML"声明式"的性质</p>' +
                        '<p>所有的html都有 <code>id, name, class, style</code> 属性, 所以可以写一个抽象类来规定这些字段</p>')
                            : (
                        '<p>We will learn things about <code>data class</code> from this example.</p>' +
                        '<p>We should use <code>data class</code> to simulate html elements, and use List <code>[]</code> to simulate the hierarchical relationships.</p>' +
                        '<p>Elements in the list can be separated with new line, so the html library looks like declarative programming.</p>' +
                        '<p>All html have <code>id, name, class, style</code> attributes. So we can write an abstract class to form these attributes.</p>')
                    ) +
                    '<pre>' + highlighting('html.lt', 'abstract data class HTMLElement(id, name, cls, style)', {}) +
                    '</pre>' +
                    (zh ? (
                        '<p>只需要一句定义,所有的字段,getter和setter,toString,equals,hashCode,包括无参构造函数都已经生成好了</p>' +
                        '<p>不过,这样得到的toString方法并不是预期的 <code>&lt;标签&gt;</code> ,而是 <code>标签名(id=?, name=?, cls=?, style=?)</code></p>' +
                        '<p>考虑到还有其他可能需要提供的字段,例如 <code>input</code> 还会有 <code>type</code> 属性, 所以应当设置一个 <code>toString(attrs)</code> 方法,用于生成标签</p>'
                    ) : (
                        '<p>With only one line definition, all the fields, getters, setters, toString, equals, hashCode methods, and constructor without parameters are automatically generated.</p>' +
                        '<p>However the toString is not expected <code>&lt;标签&gt;</code> , it\'s <code>TagName(id=?, name=?, cls=?, style=?)</code> instead.</p>' +
                        '<p>And considering that some tags contain other fields, such as <code>input</code> also contains <code>type</code> attribute, so a <code>toString(attrs)</code> method is required, which helps generate the tag.</p>'
                    )) +
                    '<pre style="height: 360px;">' + highlighting('html.lt', '' +
                        'toString(attrs):String\n' +
                        '    sb = StringBuilder()\n' +
                        '    sb append "<" append this.getClass.getSimpleName\n' +
                        '    if id\n' +
                        '        sb append " id=\'" append id append "\'"\n' +
                        '    if name\n' +
                        '        sb append " name=\'" append name append "\'"\n' +
                        '    if cls\n' +
                        '        sb append " class=\'" append cls append "\'"\n' +
                        '\n' +
                        '    for entry in attrs\n' +
                        '        if entry.value\n' +
                        '        sb append " " append entry.key append "=\'" append entry.value append "\'"\n' +
                        '\n' +
                        '    sb append ">"\n' +
                        '\n' +
                        '    return sb toString', {}) +
                    '</pre>' +
                    (zh ? (
                        '<p>可以看出,若是没有赋任何参数的Input类,它将会输出 <code>&lt;input&gt;</code></p>' +
                        '<p>有些标签是自关闭的,而有些可以有innerHTML,并且需要关闭标签. 所以还需要一个抽象类,来完成innerHTML的捕捉和输出,并提供关闭标签</p>'
                    ) : (
                        '<p>We can see that the input without any parameters, the output would be <code>&lt;input&gt;</code></p>' +
                        '<p>Some tags are self-closable, but some can have innerHTML and requires closing tag. So we need a new abstract class to capture and print innerHTMLs and provide closing tags.</p>'
                    )) +
                    '<pre style="height: 360px;">' + highlighting('html.lt', '' +
                        'abstract data class HTMLElementWithClosing : HTMLElement\n' +
                        '    children : List\n' +
                        '\n' +
                        '    add(children:List)\n' +
                        '        this.children=children\n' +
                        '        return this\n' +
                        '\n' +
                        '    toString(attrs):String\n' +
                        '        sb=StringBuilder()\n' +
                        '        sb append HTMLElement.this.toString(attrs)\n' +
                        '\n' +
                        '    if children\n' +
                        '        for i in children\n' +
                        '            sb append i\n' +
                        '\n' +
                        '    sb append "</" append this.getClass.getSimpleName append ">"\n' +
                        '    return sb toString', {}) +
                    '</pre>' +
                    (zh
                        ? '<p>可以看出,Latte中访问父类方法使用的是 <code>父类.this.方法(...)</code> . 这样,所有的节点都可以很方便的表示了</p>'
                        : '<p>We can see that invoking methods from super class is written as <code>SuperClass.this.method(...)</code> . Now, all tags are easily represented.</p>') +
                    '<pre>' + highlighting('html.lt', '' +
                        'data class html : HTMLElementWithClosing\n' +
                        '    toString():String = toString({})\n' +
                        '\n' +
                        'data class head : HTMLElementWithClosing\n' +
                        '    toString():String = toString({})\n' +
                        '\n' +
                        'data class body : HTMLElementWithClosing\n' +
                        '    toString():String = toString({})\n' +
                        '\n' +
                        'data class form : HTMLElementWithClosing\n' +
                        '    toString():String = toString({})\n' +
                        '\n' +
                        'data class input(typ) : HTMLElement\n' +
                        '    toString():String = toString({"type" : typ})', {}) +
                    '</pre>' +
                    (zh ? (
                        '<p>现在就可以使用一开始提到的方式来书写HTML了!</p>' +
                        '<blockquote>完整的html支持和这个实现有些区别, 可以在 <a target="_blank" href="https://github.com/wkgcass/Latte-lang/blob/master/latte-library/src/main/latte/lt/dsl/html.lt">这里</a> 找到</blockquote>'
                    ) : (
                        '<p>Now we can use the method mentioned at the beginning to write HTML.</p>' +
                        '<blockquote>Full html support has some differences from this implementation, which can be found <a target="_blank" href="https://github.com/wkgcass/Latte-lang/blob/master/src/main/resources/lt/html.lt">here</a></blockquote>'
                    ))
                )
            },
            {
                title: zh ? '使用 Vertx.x 或 Jetty' : "Work with Vert.x or Jetty",
                content: $sce.trustAsHtml('' +
                    (zh ? (
                    '<p>Latte 可以与Java完美互通.所以Java能够使用的库, Latte都可以使用. 比如 <code>Vert.x</code> 和 <code>Jetty</code></p>' +
                    '<p>Vert.x 和 Jetty 都可以作为嵌入语言的服务端, 所以不需要额外的Servlet容器, 也无需编译, 直接在Latte脚本中编写即可构建一个服务端.</p>' +
                    '<p>这篇示例将直接给出效果和代码:</p>' +
                    '<h3>最终效果</h3>') : (
                        '<p>Latte can work with Java perfectly. So all libraries that work on Java can also work on Latte. e.g. <code>Vert.x</code> and <code>Jetty</code></p>' +
                        '<p>Vert.x and Jetty are both server embedded in program. Extra servlet containers are not required, and you can start a server with Latte scripts without compiling.</p>' +
                        '<p>This example will show you code and result directly:</p>' +
                        '<h3>The Result</h3>'
                    )) +
                    '<iframe src=\'example/vertx.html\' width=\'100%\' style=\'border:none;\'></iframe>' +
                    '<h3>Vert.x</h3>' +
                    '<pre>' + highlighting('', '' +
                        'import io::vertx::core::_\n' +
                        '\n' +
                        'import lt::dsl::html::_\n' +
                        '\n' +
                        'Vertx.vertx().createHttpServer().requestHandler(\n' +
                        '    (req)->\n' +
                        '        req.response().\n' +
                        '        end(\n' +
                        '            html + [\n' +
                        '                head + [\n' +
                        "                    link(rel='stylesheet' , href='http://cdn.bootcss.com/bootstrap/3.3.6/css/bootstrap.min.css')\n" +
                        '                ]\n' +
                        '                body + [\n' +
                        '                    div(cls=\'container\') + [\n' +
                        '                        h1 + ["Latte with Vert.x or Jetty"]\n' +
                        "                        button(cls='btn btn-lg btn-success' , onclick='window.open(\"http://latte-lang.org\")') + [\n" +
                        '                            "Visit latte-lang.org"\n' +
                        '                        ]\n' +
                        '                    ]\n' +
                        '                ]\n' +
                        '            ] toString\n' +
                        '        )\n' +
                        ').listen(3000)'
                        , {}) + '</pre>' +
                    '<h3>Jetty (Servlet)</h3>' +
                    '<pre>' + highlighting('', '' +

                        'import javax::servlet::http::_\n' +
                        'import org::eclipse::jetty::server::_\n' +
                        'import org::eclipse::jetty::server::handler::_\n' +
                        'import org::eclipse::jetty::servlet::_\n' +
                        '\n' +
                        'import lt::dsl::html::_\n' +
                        '\n' +
                        'server = Server(3000)\n' +
                        '\n' +
                        'context = ServletContextHandler()\n' +
                        'context addServlet ServletHolder(IndexServlet()), "/"\n' +
                        '\n' +
                        'server setHandler context\n' +
                        'server start\n' +
                        'server join\n' +
                        '\n' +
                        'class IndexServlet : HttpServlet\n' +
                        '    @Override\n' +
                        '    protected doGet(request : HttpServletRequest, response : HttpServletResponse):Unit\n' +
                        '        response setContentType "text/html;charset=utf-8"\n' +
                        '        response.writer.println(\n' +
                        '            html + [\n' +
                        '                head + [\n' +
                        "                    link(rel='stylesheet' , href='http://cdn.bootcss.com/bootstrap/3.3.6/css/bootstrap.min.css')\n" +
                        '                ]\n' +
                        '                body + [\n' +
                        '                    div(cls=\'container\') + [\n' +
                        '                        h1 + ["Latte with Vert.x or Jetty"]\n' +
                        "                        button(cls='btn btn-lg btn-success' , onclick='window.open(\"http://latte-lang.org\")') + [\n" +
                        '                            "Visit latte-lang.org"\n' +
                        '                        ]\n' +
                        '                    ]\n' +
                        '                ]\n' +
                        '            ]\n' +
                        '        )'
                        , {}) + '</pre>' +
                    (zh ? '<blockquote>记得把依赖项加入class-path</blockquote>' : 'Don\'t forget to add depended jars into class-path.')
                )
            }
        ];
        $scope.switchTo = function (i) {
            $scope.showExample = i;
        }
    }
    ]);
});