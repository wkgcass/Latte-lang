$(document).ready(function () {
    // create sidebar and attach to menu open
    $('.ui.sidebar').sidebar('attach events', '.toc.item');

    var app = angular.module('download', []);
    app.controller('controller', ['$scope', '$sce', function ($scope, $sce) {
        $scope.navs = common_navs();
        $scope.navs[1].active = true;

        $scope.source_code = {
            title: "源代码",
            contents: [
                $sce.trustAsHtml("LessTyping源代码托管于 <a href='" + common_git_repo() + "'>Github</a>"),
                $sce.trustAsHtml("您可以下载整个 <code>master</code> 分支自行Build , 或者下载已经编译的 <code>jar</code> 文件")
            ],
            download: "下载 Master 分支",
            link: "https://github.com/wkgcass/LessTyping/archive/master.zip"
        };
        $scope.build = {
            title: "依赖项 与 Build",
            contents: [
                $sce.trustAsHtml("" +
                    "<ul class='ui list'>" +
                    "<li>依赖于 <code>JRE 8</code></li>" +
                    "<li>自动Build还需 <code>Maven 3</code></li>" +
                    "<li>如果手动编译需要引入 <code>asm 5.1</code></li>" +
                    "</ul>"),
                $sce.trustAsHtml("直接下载 Master 分支, 然后执行<code>mvn clean package</code>, 在生成的target目录下可以找到打包的 jar 文件."),
                $sce.trustAsHtml("执行 <code>java -jar *.jar</code> 即可启动 REPL, 当然也可引入到项目中使用")
            ]
        };
        $scope.compile = {
            title: "编译",
            contents: [
                $sce.trustAsHtml("可以在REPL中, 也可以编写java代码进行编译"),
                $sce.trustAsHtml("<h3>REPL中编译</h3>"),
                $sce.trustAsHtml("在REPL环境中使用Compiler()构造一个编译器对象, 然后通过如下的方法调用进行编译"),
                $sce.trustAsHtml(common_compile_highlighting()),
                $sce.trustAsHtml("<h3>编写java代码编译</h3>"),
                $sce.trustAsHtml("" +
                    "<textarea id='compile_java'>\n" +
                    "Compiler compiler=new Compiler();\n" +
                    "compiler.shiftLeft(\"source file directory\")\n" +
                    "    .shiftRight(\"output directory\")\n" +
                    "    .compile()" +
                    "</textarea>" +
                    "<script>var editor = CodeMirror.fromTextArea(document.getElementById('compile_java'));editor.setSize('auto', '100px');</script>")
            ]
        };
        $scope.highlight = {
            title: "语法高亮",
            contents: [
                $sce.trustAsHtml("LessTyping 支持 <code>HTML</code> 和 <code>Atom 编辑器</code> 的语法高亮"),
                $sce.trustAsHtml("<h3>HTML</h3>"),
                $sce.trustAsHtml("HTML语法高亮基于<code>LessTyping Scanner for JavaScript</code>. 在您的html文件中引入如下文件和代码即可获取高亮生成的内容"),
                $sce.trustAsHtml("<textarea id='highlighting_html'>\n" +
                    "<script src=\"javascripts/scanner.js\"></script>\n" +
                    "<script src=\"javascripts/code.js\"></script>\n" +
                    "<link rel='stylesheet' href=\"stylesheets/code.css\">\n\n" +
                    "<script>\n" +
                    "var code = ''\n" +
                    "var res = highlighting('', code, {});\n" +
                    "$('pre').html(res);\n" +
                    "</script>" +
                    "</textarea>\n" +
                    "<script>var editor = CodeMirror.fromTextArea(document.getElementById('highlighting_html'));editor.setSize('auto', '220px');</script>"),
                $sce.trustAsHtml("<h3>Atom</h3>"),
                $sce.trustAsHtml("将下载文档的 <code>/language-lesstyping</code> , 直接放置在<code> ~/.atom/packages/ </code>目录下即可"),
                $sce.trustAsHtml("<img src='images/highlight.png'></img>")
            ]
        }
    }
    ])
    ;
})
;