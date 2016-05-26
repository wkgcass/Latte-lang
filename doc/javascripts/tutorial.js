$(document).ready(function () {
    // create sidebar and attach to menu open
    $('.ui.sidebar').sidebar('attach events', '.toc.item');

    var app = angular.module('tutorial', []);
    app.controller('controller', ['$scope', '$sce', function ($scope, $sce) {
        $scope.navs = common_navs();
        $scope.navs[2].active = true;

        $scope.git_repo = common_git_repo();

        $scope.descr = "LessTyping是一种JVM语言, 它与Java有许多相似之处. 这里给出LessTyping与Java相同语义的比较";

        function getLessTypingHtml(str) {
            return $sce.trustAsHtml(highlighting("", str, {}));
        }

        $scope.tutorials = [
            {
                title: "Hello World",
                java: "System.out.println(\"hello world\");",
                lesstyping: getLessTypingHtml("println('hello world')"),
                note: $sce.trustAsHtml("LessTyping隐式导入 <code>lt.lang.Utils</code> 下的所有static方法.")
            },
            {
                title: "注释",
                java: "" +
                "// comment\n" +
                "/*\n" +
                "multiple line comment\n" +
                "*/",
                lesstyping: getLessTypingHtml("" +
                    "; comment\n" +
                    "/*\n" +
                    "multiple line comment\n" +
                    "*/")
            },
            {
                title: "值",
                java: "" +
                "int i = 1;\n" +
                "float f =  1.2\n" +
                "Object o = new Object()\n" +
                "int[] arr = {1,2,3}\n" +
                "\n" +
                "List list = new LinkedList();\n" +
                "list.add(1);  list.add(2);  list.add(3)\n" +
                "\n" +
                "Map map = new LinkedHashMap()\n" +
                "map.put(\"id\", 12);\n" +
                "map.put(\"name\", \"cass\");\n" +
                "map.put(\"age\", 22);",
                lesstyping: getLessTypingHtml("" +
                    "i : int = 1\n" +
                    "f : float = 1.2\n" +
                    "o = Object()\n" +
                    "arr : []int = [1,2,3]\n" +
                    "\n" +
                    "list = [1,2,3]\n" +
                    "\n" +
                    "map = {\n" +
                    "    'id' : 12\n" +
                    "    'name' : 'cass'\n" +
                    "    'age' : 22\n" +
                    "}"),
                note: $sce.trustAsHtml("LessTyping是强弱类型结合的. 对于没有声明类型的变量将视为 <code>java.lang.Object</code>")
            },
            {
                title: "类型定义",
                java: "" +
                "public class User {\n" +
                "    private int id\n" +
                "    private String name\n" +
                "\n" +
                "    public User(int id, String name) {\n" +
                "        this.id=id;\n" +
                "        this.name=name;\n" +
                "    }\n" +
                "    public String toString() {\n" +
                "        return \"User(id=\"+id+\", name=\"+name+\")\";\n" +
                "    }\n" +
                "}",
                lesstyping: getLessTypingHtml("" +
                    "class User(id : int, name : String)\n" +
                    "    toString():String='User(id='+id+', name='+name+')'\n" +
                    "\n" +
                    "; 若不考虑与java的交互, 可以省略类型\n" +
                    "; 更偏向弱类型的写法\n" +
                    "class User(id,name)\n" +
                    "\n" +
                    "; 可以将参数通过换行隔开\n" +
                    "class User(\n" +
                    "    id : int\n" +
                    "    name : String\n" +
                    ")"),
                note: $sce.trustAsHtml("构造块中的参数与变量都将视为类的字段")
            },
            {
                title: "Data Class",
                java: "" +
                "public class User {\n" +
                "    private int id;\n" +
                "    private String name;\n" +
                "    public User() {\n" +
                "    }\n" +
                "    public User(int id, String name) {\n" +
                "        this.id=id;\n" +
                "        this.name=name;\n" +
                "    }\n" +
                "    public boolean equals(Object o) {\n" +
                "        return o instanceof User\n" +
                "            && ((User)o).id==this.id\n" +
                "            && ((User)o).name.equals(this.name);\n" +
                "    }\n" +
                "    public int hashCode() {\n" +
                "        return id+name.hashCode();\n" +
                "    }\n" +
                "    public String toString() {\n" +
                "        return \"User(id=\"+id+\",name=\"+name+\")\";\n" +
                "    }\n" +
                "    public int getId() {\n" +
                "        return id;\n" +
                "    }\n" +
                "    public String getName() {\n" +
                "        return name;\n" +
                "    }\n" +
                "    public void setId(int id) {\n" +
                "        this.id=id;\n" +
                "    }\n" +
                "    public void setName(String name) {\n" +
                "        this.name=name;\n" +
                "    }\n" +
                "}",
                lesstyping: getLessTypingHtml("" +
                    "data class User(id:int, name:String)")
            },
            {
                title: "运算符重载",
                java: "" +
                "BigInteger a = new BigInteger(\"16\");\n" +
                "BigInteger b = new BigInteger(\"3\");\n" +
                "BigInteger addRes = a.add(b);\n" +
                "BigInteger subRes = a.subtract(b);\n" +
                "\n" +
                "List list = new LinkedList();\n" +
                "list.add(1);",
                lesstyping: getLessTypingHtml("" +
                    "a = BigInteger(\"16\")\n" +
                    "b = BigInteger(\"3\")\n" +
                    "addRes = a + b\n" +
                    "subRes = a - b\n" +
                    "\n" +
                    "list = []\n" +
                    "list + 1")
            },
            {
                title: "DSL",
                java: "" +
                "StringBuilder sb = new StringBuilder();\n" +
                "sb.append(\"Hello \").append(\"world\").toString();\n" +
                "\n" +
                "Map map = new LinkedHashMap()\n" +
                "map.put(\"cass\", 22);",
                lesstyping: getLessTypingHtml("" +
                    "sb = StringBuilder()\n" +
                    "sb append 'Hello ' append 'world' toString\n" +
                    "\n" +
                    "map = {}\n" +
                    "map put 'cass', 22")
            },
            {
                title: "条件语句",
                java: "" +
                "if(list != null) {\n" +
                "    a = list\n" +
                "} else {\n" +
                "    a = new LinkedList();\n" +
                "    map.put('key', a);\n" +
                "}",
                lesstyping: getLessTypingHtml("" +
                    "if list\n" +
                    "    a = list\n" +
                    "else\n" +
                    "    a = []\n" +
                    "    map['key']=a"),
                note: $sce.trustAsHtml("<code>null, undefined, 0</code> 都将转化为 <code>false</code>")
            },
            {
                title: "For 循环",
                java: "" +
                "for(User u : userList) {\n" +
                "    System.out.println(u);\n" +
                "}\n" +
                "\n" +
                "for(int i=0;i<arr.length;++i) {\n" +
                "    System.out.println(\"第\"+i+\"个元素值为\"+arr[i]);" +
                "}",
                lesstyping: getLessTypingHtml("" +
                    "for u in userList\n" +
                    "    println(u)\n" +
                    "\n" +
                    "for i in 0.:arr.length\n" +
                    "    println(\"第\"+i+'个元素值为'+arr[i])"),
                note: $sce.trustAsHtml("for语句可以接受 <code>Iterable, Iterator, Enumeration, 数组</code> 作为循环依据")
            },
            {
                title: "While/Do-While 循环",
                java: "" +
                "while(line != null) {\n" +
                "    if(line.equals(\"\") {\n" +
                "        // do something\n" +
                "    }\n" +
                "    line = reader.readLine()\n" +
                "}\n" +
                "\n" +
                "do {\n" +
                "    // do something\n" +
                "} while(boolValue)",
                lesstyping: getLessTypingHtml("" +
                    "while line\n" +
                    "    if line == ''\n" +
                    "        ...\n" +
                    "    line = reader readLine\n" +
                    "\n" +
                    "do\n" +
                    "    ...\n" +
                    "while boolValue"),
                note: $sce.trustAsHtml("<code>null, undefined, 0</code> 都将转化为 <code>false</code>")
            },
            {
                title: "Try-Catch-Finally",
                java: "" +
                "try {\n" +
                "    stream.read();\n" +
                "} catch(IOException e) {\n" +
                "    LOGGER.info(e.getMessage());\n" +
                "} catch(RuntimeException e) {\n" +
                "    LOGGER.debug(e.getMessage());\n" +
                "} finally {\n" +
                "    stream.close();\n" +
                "}",
                lesstyping: getLessTypingHtml("" +
                    "try\n" +
                    "    stream read\n" +
                    "catch e\n" +
                    "    if e is type IOException\n" +
                    "        LOGGER.info(e.getMessage())\n" +
                    "    elseif e is type RuntimeException\n" +
                    "        LOGGER.debug(e.getMessage())\n" +
                    "    else\n" +
                    "        throw e\n" +
                    "finally\n" +
                    "    stream close\n" +
                    "\n" +
                    "throw 'a string'"),
                note: $sce.trustAsHtml("LessTyping允许throw与catch任何类型, 例如 throw \"error message\", 所以并不直接提供java的catch Type")
            },
            {
                title: "Synchronized",
                java: "" +
                "synchronized(a) {\n" +
                "    synchronized(b) {\n" +
                "        // do something\n" +
                "    }\n" +
                "}",
                lesstyping: getLessTypingHtml("" +
                    "synchronized(a,b)\n" +
                    "    ...")
            },
            {
                title: "Lambda",
                java: "" +
                "list.stream().map(e->e.toString())\n" +
                ".collect(Collectors.toList());\n" +
                "\n" +
                "list.forEach(e->{\n" +
                "    if(e<10){\n" +
                "        System.out.println(e);\n" +
                "    }\n" +
                "});",
                lesstyping: getLessTypingHtml("" +
                    "list.stream().map(\n" +
                    "    (e)->e.toString\n" +
                    ").collect(Collectors.toList())\n" +
                    "\n" +
                    "list.forEach(\n" +
                    "    (e)->\n" +
                    "        if e < 10\n" +
                    "            println(e)\n" +
                    ")"),
                note: $sce.trustAsHtml("LessTyping的<code>lambda</code>与java的几乎一样. 区别在于 1.LessTyping不需要大括号,而是以缩进来代替. 2.对于单参数不能省略括号")
            }
        ];
        $scope.printScriptForJava = function (index, java) {
            var lineCount = java.split(/\n|\r/g).length;
            return "var editor = CodeMirror.fromTextArea(document.getElementById('java_" + index + "'));editor.setSize('auto', '" + (lineCount * 20 + 30) + "');";
        }
    }]);
});