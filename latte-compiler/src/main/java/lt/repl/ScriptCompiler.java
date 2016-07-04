/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 KuiGang Wang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package lt.repl;

import lt.compiler.*;
import lt.compiler.Scanner;
import lt.compiler.syntactic.AST;
import lt.compiler.syntactic.Statement;
import lt.compiler.syntactic.def.*;
import lt.compiler.syntactic.pre.Import;
import lt.compiler.syntactic.pre.PackageDeclare;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * script compiler. The script compiler first transform the script into<br>
 * <pre>
 * class Script$generatedName
 *     static
 *         method(args:[]String)
 *             ... ; the script
 * </pre>
 */
public class ScriptCompiler {
        /**
         * holder of the compiling results
         */
        public static class Script {
                /**
                 * class loader
                 */
                public final ClassLoader classLoader;
                /**
                 * the script class
                 */
                public final Class<?> scriptClass;
                /**
                 * the script method (<code>method([]String)</code>)
                 */
                public final Method scriptMethod;

                /**
                 * the return value of {@link #scriptMethod}
                 */
                private Object result;

                /**
                 * constructing the result
                 *
                 * @param classLoader  class loader
                 * @param scriptClass  the script class
                 * @param scriptMethod the script method
                 */
                Script(ClassLoader classLoader, Class<?> scriptClass, Method scriptMethod) {
                        this.classLoader = classLoader;
                        this.scriptClass = scriptClass;
                        this.scriptMethod = scriptMethod;
                }

                /**
                 * invoke {@link #scriptMethod} with a zero-length String array
                 *
                 * @return the Result object itself (invoke {@link #getResult()}) to get the return value
                 * @throws Throwable throwable
                 */
                public Script run() throws Throwable {
                        return run(new String[0]);
                }

                /**
                 * invoke {@link #scriptMethod} with given arguments
                 *
                 * @param args arguments
                 * @return the Result object itself (invoke {@link #getResult()}) to get the return value
                 * @throws Throwable throwable
                 */
                public Script run(String[] args) throws Throwable {
                        try {
                                result = scriptMethod.invoke(scriptClass.newInstance(), new Object[]{args});
                        } catch (InvocationTargetException e) {
                                throw e.getTargetException();
                        }
                        return this;
                }

                /**
                 * get the return value of {@link #scriptMethod}
                 *
                 * @return the return value
                 */
                public Object getResult() {
                        return result;
                }
        }

        private final Compiler compiler;

        private Map<String, Object> sources = new HashMap<>();

        /**
         * construct a script compiler
         *
         * @param parentLoader parent loader
         */
        public ScriptCompiler(ClassLoader parentLoader) {
                this.compiler = new Compiler(parentLoader);
        }

        /**
         * add class path
         *
         * @param classPath class path
         * @return the ScriptCompiler itself
         * @throws IOException exception
         */
        public ScriptCompiler add(String classPath) throws IOException {
                compiler.add(classPath);
                return this;
        }

        /**
         * add class path
         *
         * @param classPath class path
         * @return the ScriptCompiler itself
         * @throws IOException exception
         */
        public ScriptCompiler add(File classPath) throws IOException {
                compiler.add(classPath);
                return this;
        }

        /**
         * add class path
         *
         * @param classPath class path
         * @return the ScriptCompiler itself
         */
        public ScriptCompiler add(URL classPath) {
                compiler.add(classPath);
                return this;
        }

        /**
         * add source code
         *
         * @param sourceName source code name
         * @param source     source
         * @return the ScriptCompiler itself
         */
        public ScriptCompiler shiftLeft(String sourceName, String source) {
                sources.put(sourceName, source);
                return this;
        }

        /**
         * add source code
         *
         * @param source source
         * @return the ScriptCompiler itself
         */
        public ScriptCompiler shiftLeft(File source) {
                if (source.isFile()) {
                        sources.put(source.getName(), source);
                } else throw new IllegalArgumentException("not a file");
                return this;
        }

        /**
         * add source code
         *
         * @param sourceName source code name
         * @param source     source
         * @return the ScriptCompiler itself
         */
        public ScriptCompiler shiftLeft(String sourceName, Reader source) {
                sources.put(sourceName, source);
                return this;
        }

        /**
         * add source codes
         *
         * @param sources sources
         * @return the ScriptCompiler itself
         */
        public ScriptCompiler shiftLeft(Map<String, ?> sources) {
                this.sources.putAll(sources);
                return this;
        }

        /**
         * compile the script
         *
         * @param name   the script name
         * @param script script
         * @return compiling result
         * @throws Exception exception
         */
        public Script compile(String name, String script) throws Exception {
                return compile(name, new StringReader(script));
        }

        /**
         * compile the script
         *
         * @param scriptFile script file
         * @return compiling result
         * @throws Exception exception
         */
        public Script compile(File scriptFile) throws Exception {
                return compile(scriptFile.getName(), new FileReader(scriptFile));
        }

        private boolean scriptNameAlreadyUsed(ClassLoader loader, String name) {
                try {
                        Class.forName(name);
                        return true;
                } catch (ClassNotFoundException e) {
                        try {
                                loader.loadClass(name);
                                return true;
                        } catch (ClassNotFoundException e2) {
                                return false;
                        }
                }
        }

        /**
         * compile the script
         *
         * @param name         the script name
         * @param scriptReader script reader
         * @return compiling result
         * @throws Exception exception
         */
        public Script compile(String name, Reader scriptReader) throws Exception {
                ClassLoader theCompiledClasses = compiler.compile(sources);

                String nameForTheScript = "Script$Latte$";
                int i = 0;
                while (scriptNameAlreadyUsed(theCompiledClasses, nameForTheScript + i)) ++i;

                nameForTheScript += i;

                // class N
                //     static
                //         method(args:String[])
                //             ...
                //
                ErrorManager err = new ErrorManager(true);
                Scanner scanner = new Scanner(name, scriptReader, new Scanner.Properties(), err);
                Parser parser = new Parser(scanner.scan(), err);
                List<Statement> statements = parser.parse();

                List<Statement> innerStatements = new ArrayList<>();
                List<Statement> defsAndImports = new ArrayList<>();

                for (Statement stmt : statements) {
                        if (stmt instanceof ClassDef || stmt instanceof InterfaceDef || stmt instanceof Import || stmt instanceof FunDef) {
                                defsAndImports.add(stmt);
                        } else if (stmt instanceof PackageDeclare) {
                                err.SyntaxException("scripts cannot have package declaration", stmt.line_col());
                        } else {
                                innerStatements.add(stmt);
                        }
                }

                VariableDef v = new VariableDef("args", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setType(new AST.Access(new AST.Access(null, "String", LineCol.SYNTHETIC), "[]", LineCol.SYNTHETIC));
                ClassDef classDef = new ClassDef(
                        nameForTheScript,
                        Collections.emptySet(),
                        Collections.emptyList(),
                        null,
                        Collections.emptyList(),
                        Collections.emptySet(),
                        Collections.singletonList(
                                new MethodDef(
                                        "method",
                                        Collections.emptySet(),
                                        null,
                                        Collections.singletonList(v),
                                        Collections.emptySet(),
                                        innerStatements,
                                        LineCol.SYNTHETIC
                                )
                        ),
                        new LineCol(name, 0, 0)
                );

                defsAndImports.add(classDef);
                defsAndImports.add(new Import(new AST.PackageRef("lt::util", LineCol.SYNTHETIC), null, true, LineCol.SYNTHETIC));
                defsAndImports.add(new Import(new AST.PackageRef("java::util", LineCol.SYNTHETIC), null, true, LineCol.SYNTHETIC));
                defsAndImports.add(new Import(new AST.PackageRef("java::math", LineCol.SYNTHETIC), null, true, LineCol.SYNTHETIC));
                defsAndImports.add(new Import(new AST.PackageRef("java::io", LineCol.SYNTHETIC), null, true, LineCol.SYNTHETIC));
                defsAndImports.add(new Import(new AST.PackageRef("lt::repl", LineCol.SYNTHETIC), null, true, LineCol.SYNTHETIC));

                SemanticProcessor sp = new SemanticProcessor(new HashMap<String, List<Statement>>() {{
                        put(name, defsAndImports);
                }}, theCompiledClasses, err);
                CodeGenerator cg = new CodeGenerator(sp.parse());
                Map<String, byte[]> map = cg.generate();
                ClassLoader loader = new ClassLoader(theCompiledClasses) {
                        @Override
                        protected Class<?> findClass(String name) throws ClassNotFoundException {
                                if (map.containsKey(name)) {
                                        byte[] bs = map.get(name);
                                        return defineClass(name, bs, 0, bs.length);
                                } else throw new ClassNotFoundException(name);
                        }
                };

                Class<?> scriptCls = loader.loadClass(nameForTheScript);
                Method scriptMethod = scriptCls.getMethod("method", String[].class);

                return new Script(loader, scriptCls, scriptMethod);
        }
}
