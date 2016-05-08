$(document).ready(function () {
    // create sidebar and attach to menu open
    $('.ui.sidebar').sidebar('attach events', '.toc.item');

    var app = angular.module('dev', []);
    app.controller('controller', ['$scope', '$sce', function ($scope, $sce) {
        $scope.navs = common_navs();
        $scope.navs[5].active = true;

        $scope.src_code = {
            title: "源代码结构",
            descr: "LessTyping采用标准的Maven目录结构"
        };
    }]);
});