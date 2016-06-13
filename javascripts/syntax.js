$(document).ready(function () {
    // create sidebar and attach to menu open
    $('.ui.sidebar').sidebar('attach events', '.toc.item');

    var app = angular.module('syntax', []);
    app.controller('controller', ['$scope', '$sce', function ($scope, $sce) {
        $scope.navs = common_navs();
        $scope.navs[3].active = true;

        $scope.git_repo = common_git_repo();

        files = {};
        function getLtFile(name) {
            $.ajax({
                url: 'https://raw.githubusercontent.com/wkgcass/Latte-lang/master/src/test/resources/lang-demo/' + name,
                async: false,
                dataType: 'text',
                success: function (res) {
                    files[name] = res;
                }
            });
        }

        getLtFile('advanced.lt');
        getLtFile('literals.lts');
        getLtFile('ltFileStructure.lt');
        getLtFile('operator.lt');
        getLtFile('statements.lts');
        getLtFile('typeDef.lt');

        $scope.descr = $sce.trustAsHtml("" +
            "如下语法规则示例代码直接通过ajax获取自" +
            "<a href='https://github.com/wkgcass/Latte-lang/tree/master/src/test/resources/lang-demo/'>github</a>, " +
            "并且它们也被视为<a href='https://github.com/wkgcass/Latte-lang/tree/master/src/test/java/lt/compiler/cases/TestDemo.java'>测试用例</a>");

        $scope.files = files;

        $scope.highlightLt = function (file, code) {
            return $sce.trustAsHtml(highlighting(file, code, {}));
        }
    }]);
});