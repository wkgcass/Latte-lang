#LessTyping

`LessTyping` is based on java 8. it is a language with both strong and week typing. it is compiled to JVM byte code, and can collaborate with any Java library.

check `src/test/resources/lang-demo` for syntax tutorials.

Generally the syntax looks like `Python`,  4 spaces indentation is forced , and it doesn't require `;` at the end of a statement/expression

comments are started by `;` , so it's ok if you like writing a `;` at the end of a line

`LessTyping` supports operator override  
besides built in operator behavior, there are several operators to be overridden, which will be converted to method invocation

`LessTyping` supports dsl syntax  
e.g.

    db select id, name from User where id > 10 and name != 'cass' orderBy id.desc
    
>check `/lang-demo/statements.lts` for more info
    
which would be parsed into method invocation

    db.select(id,name).form(User).where(id>10).and(name!='cass').orderBy(id.desc)
    
`LessTyping` supports pre processing `define`  
e.g.

    define 'CREATE TABLE' as 'class'
    define 'VARCHAR' as ':String'
    define 'NUMBER' as ':int'
    
    CREATE TABLE User(
        id NUMBER
        name VARCHAR
    )
   
>check `/lang-demo/ltFileStructure.lt` for more info

`LessTyping` supports scripts. the suffix is usually `lt`, but for scripts, it should be `lts`

the scripts would be compiled to classes as well,   
the class name is the same as `lts` file name, and a `main` method would be added into the class  
the scripts would be filled into `class constructing block`