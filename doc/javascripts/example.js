$(document).ready(function () {
    // create sidebar and attach to menu open
    $('.ui.sidebar').sidebar('attach events', '.toc.item');

    var app = angular.module('example', []);
    app.controller('controller', ['$scope', '$sce', function ($scope, $sce) {
        $scope.navs = common_navs();
        $scope.navs[4].active = true;

        $scope.git_repo = common_git_repo();

        $scope.examples = [
            {
                title: '你好, LessTyping',
                content: $sce.trustAsHtml('' +
                    '<p>进入REPL交互程序,你将会看到 <code>lt&gt;&nbsp;</code></p>' +
                    '<p>在右侧输入语句或表达式, 并按两次回车, REPL将编译并执行输入的表达式</p>' +
                    '<p>输入 ' + highlighting('test.lt', "println('Hello LessTyping')", {}) + '</p><p>控制台将打印 <code>Hello LessTyping</code></p>')
            },
            {
                title: '九九乘法表',
                content: $sce.trustAsHtml('' +
                    '<p>我们想得到的结果是这样的:</p>' +
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
                    '<p>只需双重循环即可完成</p>' +
                    '<pre>' + highlighting('test.lt', "" +
                        "for i in 1..9\n" +
                        "    for j in 1..i\n" +
                        "        System.out.print(j+\"*\"+i+\"=\"+(j*i)+\"\\t\")\n" +
                        "    System.out.println()"
                        , {}) +
                    '</pre>' +
                    '<p>这条循环语句也可以在REPL中完成. 在全部输入完成后输入两次回车即可得到结果</p>' +
                    '<p>其中 <code>1..9</code> 表示构建一个从1到9(包括)的列表</p>' +
                    '<p>类似的还有 <code>1.:9</code> 表示一个从1到9(不包括)的列表</p>'
                )
            },
            {
                title: 'Rational',
                content: $sce.trustAsHtml('' +
                    '<p>Rational表示分数, 通过这个例子, 我们可以得知如何<b>重载方法</b>以及<b>绑定运算符</b></p>' +
                    '<p>首先, 要构造Rational则需要两个整数,分别表示分子和分母</p>' +
                    '<pre>' + highlighting('test.lt', "" +
                        "class Rational(n:int, d:int)"
                        , {}) +
                    '</pre>' +
                    '<p>定义了一个类, 它拥有一个"带两个整数参数"的构造函数</p>' +
                    '<p>在 <code>LessTyping</code> 中,构造函数的参数将直接视为字段(Field)</p>' +
                    '<p>同时,构造块中定义的变量同样视为字段. 在 <code>LessTyping</code> 中字段默认为私有, 所以不用担心这些值被污染</p>' +
                    '<br>' +
                    '<p>输入时需要限定分母不为0, 在分母为0时需要抛出异常</p>' +
                    '<pre>' + highlighting('test.lt', "" +
                        "if d is 0\n" +
                        "    throw IllegalArgumentException(\"divider cannot be 0\")"
                        , {}) +
                    '</pre>' +
                    '<br>' +
                    '<p>现在可以确保任何输入都是合法的了. 如何在控制台看到这些值呢? 我们需要重载 <code>toString</code> 方法</p>' +
                    '<pre>' + highlighting('test.lt', "" +
                        "toString():String = n + (\n" +
                        "    if d==1\n" +
                        "        < \"\"\n" +
                        "    < \"/\" + d)"
                        , {}) +
                    '</pre>' +
                    '<p>重载了Object类的toString()方法, 这样就可以直接在控制台打印Rational对象了, 结果类似于 <code>1/2</code></p>' +
                    '<br>' +
                    '<p>输入有可能可以约分, 我们需要增加一个gcd(int,int)函数来获取最大公约数</p>' +
                    '<pre>' + highlighting('test.lt', "" +
                        "static\n" +
                        "    pri gcd(a:int, b:int)\n" +
                        "        if b\n" +
                        "            <gcd(b, a % b)\n" +
                        "        else\n" +
                        "            <a"
                        , {}) +
                    '</pre>' +
                    '<p>定义了一个静态gcd(int,int)方法, 用辗转相除法获取最大公约数</p>' +
                    '<p>在构造块中对传入的数进行约分</p>' +
                    '<pre>' + highlighting('test.lt', "" +
                        "neg= n * d < 0\n" +
                        "n=Math.`abs`(n)\n" +
                        "d=Math.`abs`(d)\n" +
                        "g=gcd(n,d)\n" +
                        "n/=g\n" +
                        "d/=g\n" +
                        "\n" +
                        "if neg\n" +
                        "    n=-n"
                        , {}) +
                    '</pre>' +
                    '<p>Math.`abs`使用了<code>``</code>符号,因为abs是<code>LessTyping</code>的一个关键字,用于表示<code>abstract</code></p>' +
                    '<p>但abs是合法的java名称, 所以使用<code>`abs`</code>符号来使用这个名称</p>' +
                    '<p>现在, 所有Rational都是最简形式</p>' +
                    '<br>' +
                    '<p>我们还需要对Rational进行比较, 所以需要重写Object类的equals方法. equals方法和hashCode方法通常一起重写,以便<code>java::lang::HashMap</code>之类的类使用</p>' +
                    '<pre>' + highlighting('test.lt', "" +
                        "equals(o):bool\n" +
                        "    if o is type Rational\n" +
                        "        <o.n==n and o.d==d\n" +
                        "    else\n" +
                        "        <false\n" +
                        "\n" +
                        "hashCode():int\n" +
                        "    <n+d"
                        , {}) +
                    '</pre>' +
                    '<p>现在可以在 <code>LessTyping</code> 中使用 <code>==</code> 和 <code>!=</code> 运算符比较Rational了</p>' +
                    '<br>' +
                    '<p>接下来是运算符绑定</p>' +
                    '<p>最基本的运算有 <code>+ - * /</code>, 我们目标是使用运算符来计算Rational的值</p>' +
                    '<pre>' + highlighting('test.lt', "" +
                        "add(o:Rational):Rational = Rational(n*o.d+o.n*d, d*o.d)\n" +
                        "subtract(o:Rational):Rational = Rational(n*o.d-o.n*d, d*o.d)\n" +
                        "multiply(o:Rational):Rational = Rational(n*o.n, d*o.d)\n" +
                        "divide(o:Rational):Rational = Rational(n*o.d, d*o.n)"
                        , {}) +
                    '</pre>' +
                    '<p>运算符绑定非常简单, 写出符合签名的方法即可完成运算符的绑定. 现在可以使用 <code>+ - * /</code> 来进行Rational的运算了!</p>' +
                    '<p>至此我们完成了Rational类的构造, 不过还可以做一些小小的修改, 比如说除数默认为1. 需要将类构造参数修改为:</p>' +
                    '<pre>' + highlighting('test.lt', "" +
                        "class Rational(n:int, d:int=1)"
                        , {}) +
                    '</pre>' +
                    '<p>设置了一个参数默认值</p>' +
                    '<br>' +
                    '<p>Rational类所有代码如下</p>' +
                    '<pre>' + highlighting('test.lt', "" +
                        "class Rational(n:int, d:int=1)\n" +
                        "    if d is 0\n" +
                        "        throw IllegalArgumentException(\"divider cannot be 0\")\n" +
                        "\n" +
                        "    neg= n * d < 0\n" +
                        "    n=Math.`abs`(n)\n" +
                        "    d=Math.`abs`(d)\n" +
                        "    g=gcd(n,d)\n" +
                        "    n/=g\n" +
                        "    d/=g\n" +
                        "\n" +
                        "    if neg\n" +
                        "        n=-n\n" +
                        "\n" +
                        "    toString():String = n + (\n" +
                        "        if d==1\n" +
                        "            < \"\"\n" +
                        "        < \"/\" + d)\n" +
                        "\n" +
                        "    equals(o):bool\n" +
                        "        if o is type Rational\n" +
                        "            <o.n==n and o.d==d\n" +
                        "        else\n" +
                        "            <false\n" +
                        "\n" +
                        "    hashCode():int\n" +
                        "        <n+d\n" +
                        "\n" +
                        "    add(o:Rational):Rational = Rational(n*o.d+o.n*d, d*o.d)\n" +
                        "    subtract(o:Rational):Rational = Rational(n*o.d-o.n*d, d*o.d)\n" +
                        "    multiply(o:Rational):Rational = Rational(n*o.n, d*o.d)\n" +
                        "    divide(o:Rational):Rational = Rational(n*o.d, d*o.n)\n" +
                        "\n" +
                        "    static\n" +
                        "        pri gcd(a:int, b:int)\n" +
                        "            if b\n" +
                        "                <gcd(b, a % b)\n" +
                        "            else\n" +
                        "                <a"
                        , {}) +
                    '</pre>' +
                    '<p>为了测试Rational类,我们可以写一个TestRational</p>' +
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
                    '<blockquote>关于Rational的测试用例可以在 <a target="_blank" href="https://github.com/wkgcass/LessTyping/blob/master/src/test/java/lt/compiler/cases/TestDemo.java">TestDemo</a> 中找到</blockquote>'
                )
            }
        ];
    }
    ])
    ;
})
;