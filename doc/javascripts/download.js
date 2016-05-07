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
                $sce.trustAsHtml("")
            ]
        }
    }]);
});