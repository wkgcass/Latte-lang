$(document).ready(function () {
    // create sidebar and attach to menu open
    $('.ui.sidebar').sidebar('attach events', '.toc.item');

    var app = angular.module('download', []);
    app.controller('controller', ['$scope', '$sce', function ($scope, $sce) {
        $scope.navs = common_navs();
        $scope.navs[1].active = true;

        $scope.git_repo = common_git_repo();

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
                    "</ul>"),
                $sce.trustAsHtml("如果手动编译请编译 <code>/src/main/java</code> 目录"),
                $sce.trustAsHtml("若需自动编译, 下载 Master 分支, 然后执行<code>mvn clean package</code>, 在生成的target目录下可以找到打包的 jar 文件, 并且在跟目录会生成批处理文件."),
                $sce.trustAsHtml("执行 <code>.&#47;lesstyping</code> 即可启动 REPL, 当然也可将jar文件引入到项目中使用")
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
                    "compiler.add(\"class-path\")\n" +
                    "    .shiftRight(\"output directory\")\n" +
                    "    .compile(lt.lang.Utils.filesInDirectory(\"source file directory\"))" +
                    "</textarea>" +
                    "<script>var editor = CodeMirror.fromTextArea(document.getElementById('compile_java'));editor.setSize('auto', '100px');</script>")
            ]
        };
        $scope.compile = {
            title: "Script 与 编译",
            contents: [
                $sce.trustAsHtml("LessTyping支持Script, 所以编译建议通过脚本完成"),
                $sce.trustAsHtml(common_script_highlighting()),
                $sce.trustAsHtml("可以使用如下方法运行脚本"),
                $sce.trustAsHtml("<code>.&#47;lesstyping script-file-path script-arguments</code>"),
                $sce.trustAsHtml("或者进入REPL后, 输入"),
                $sce.trustAsHtml("<code>:script script-path<br>" +
                    "script run<br>" +
                    "script run ['string-array-as-arguments']" +
                    "</code>")
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
                $sce.trustAsHtml("在Atom中搜索并安装 <code>Atom-LessTyping-Highlighting</code> 或者按照<a href='https://atom.io/packages/Atom-LessTyping-Highlighting'>这里</a>的步骤进行安装"),
                $sce.trustAsHtml("<img src='images/highlight.png'>")
            ]
        };
        $scope.ide = {
            title: "IDE",
            contents: [
                $sce.trustAsHtml("针对Atom开发了一个LessTyping的IDE"),
                $sce.trustAsHtml("在Atom中搜索并安装 <code>atom-lesstyping-ide</code> 并按照<a target='_blank' href='https://atom.io/packages/atom-lesstyping-ide'>这里</a>的步骤进行配置")
            ]
        }
    }
    ])
    ;
})
;