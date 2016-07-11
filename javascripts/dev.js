$(document).ready(function () {
    // create sidebar and attach to menu open
    $('.ui.sidebar').sidebar('attach events', '.toc.item');

    var app = angular.module('dev', []);
    app.controller('controller', ['$scope', '$sce', function ($scope, $sce) {
        var zh=useZh();

        $scope.navs = common_navs();
        $scope.navs[5].active = true;

        $scope.git_repo = common_git_repo();

        $scope.issue={
            title: zh?"提交Issue":"Submit Issues",
            content: zh
                ?$sce.trustAsHtml("若发现任何BUG, 有更好的建议或是希望拥有的特性, 都可以在<a href=\"https://github.com/wkgcass/Latte-lang/issues\">issue</a>中提出")
                :$sce.trustAsHtml("If you find any bug, or have advises or request for new features, submit them via <a href=\"https://github.com/wkgcass/Latte-lang/issues\">issue</a>.")
        }

        $scope.compiler = {
            title: zh?"编译器":"Compiler",
            content: zh?$sce.trustAsHtml("<p>Latte的编译器分为" +
                "词法分析(<code>lt.compiler.Scanner</code>)," +
                "语法分析(<code>lt.compiler.Parser</code>)," +
                "语义分析(<code>lt.compiler.SemanticProcessor</code>)," +
                "字节码生成(<code>lt.compiler.CodeGenerator</code>)</p>" +
                "<ol>" +
                "<li>Scanner将输入转换为Token并标注类别</li>" +
                "<li>Parser将Token转换为AST</li>" +
                "<li>SemanticProcessor将AST转换为比较接近字节码的表示形式 (语义分析不作为Parser子程序)</li>" +
                "<li>CodeGenerator生成字节码 (使用ASM5.1作为生成器. 已在工程内对其repackage以防止依赖冲突)</li>" +
                "</ol>")
                :$sce.trustAsHtml("<p>Latte compiler is divided into:<br>" +
                "Lexical (<code>lt.compiler.Scanner</code>),<br>" +
                "Syntactic (<code>lt.compiler.Parser</code>),<br>" +
                "Semantic (<code>lt.compiler.SemanticProcessor</code>),<br>" +
                "Code generator (<code>lt.compiler.CodeGenerator</code>)</p>" +
                "<ol>" +
                "<li>Scanner transforms input text into Tokens and mark these tokens with categories</li>" +
                "<li>Parser parses Tokens into AST</li>" +
                "<li>SemanticProcessor transforms AST to the form that is close to byte code. (The semanticProcessor is NOT a sub-procedure of Parser)</li>" +
                "<li>CodeGenerator generates byte code. (ASM5.1 is used as the generator. It's repackaged in the project to avoid dependency conflict.)</li>" +
                "</ol>")
        };

        $scope.src_code = {
            title: zh?"源代码结构":"Structure of the Source",
            descr: zh?"Latte使用Maven工程的模块化结构":"Latte uses Maven modules."
        };
    }]);
});