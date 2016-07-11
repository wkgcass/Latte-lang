$(document).ready(function () {
    // create sidebar and attach to menu open
    $('.ui.sidebar').sidebar('attach events', '.toc.item');

    var app = angular.module('syntax', []);
    app.controller('controller', ['$scope', '$sce', function ($scope, $sce) {
        $scope.navs = common_navs();
        $scope.navs[3].active = true;

        $scope.git_repo = common_git_repo();

        files = [];
        function getLtFile(name) {
            $.ajax({
                url: 'https://raw.githubusercontent.com/wkgcass/Latte-lang/master/latte-compiler/src/test/resources/lang-demo/' + name,
                async: false,
                dataType: 'text',
                success: function (res) {
                    files.push({
                        file: name,
                        code: res
                    });
                }
            });
        }

        getLtFile('ltFileStructure.lt');
        getLtFile('literals.lts');
        getLtFile('statements.lts');
        getLtFile('typeDef.lt');
        getLtFile('operator.lt');
        getLtFile('list-map.lts');
        getLtFile('advanced.lt');

        var zh=useZh();

        $scope.descr = $sce.trustAsHtml("" +
            (zh?"如下语法规则示例代码直接通过ajax获取自":"The following syntax examples are directly retrieved via ajax from ") +
            "<a href='https://github.com/wkgcass/Latte-lang/tree/master/src/test/resources/lang-demo/'>github</a>, " +
            (zh?"并且它们也被视为":"and they are considered as ") +
                "<a href='https://github.com/wkgcass/Latte-lang/tree/master/src/test/java/lt/compiler/cases/TestDemo.java'>" +
                    (zh?"测试用例":"test cases") +
                "</a>");

        $scope.files = files;

        $scope.highlightLt = function (file, code) {
            return $sce.trustAsHtml(highlighting(file, code, {}));
        }
    }]);
});