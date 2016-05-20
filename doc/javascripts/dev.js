$(document).ready(function () {
    // create sidebar and attach to menu open
    $('.ui.sidebar').sidebar('attach events', '.toc.item');

    var app = angular.module('dev', []);
    app.controller('controller', ['$scope', '$sce', function ($scope, $sce) {
        $scope.navs = common_navs();
        $scope.navs[5].active = true;

        $scope.git_repo = common_git_repo();

        $scope.compiler = {
            title: "编译器",
            content: $sce.trustAsHtml("<p>LessTyping的编译器分为" +
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
        };

        $scope.src_code = {
            title: "源代码结构",
            descr: "LessTyping采用标准的Maven目录结构"
        };
    }]);
});