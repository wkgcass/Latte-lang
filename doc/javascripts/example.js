$(document).ready(function () {
    // create sidebar and attach to menu open
    $('.ui.sidebar').sidebar('attach events', '.toc.item');

    var app = angular.module('dev', []);
    app.controller('controller', ['$scope', '$sce', function ($scope, $sce) {
        $scope.navs = common_navs();
        $scope.navs[4].active = true;

        $scope.git_repo = common_git_repo();


    }]);
});