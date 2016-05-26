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

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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
                 * @throws InvocationTargetException exception
                 * @throws IllegalAccessException    exception
                 */
                public Script run() throws InvocationTargetException, IllegalAccessException {
                        return run(new String[0]);
                }

                /**
                 * invoke {@link #scriptMethod} with given arguments
                 *
                 * @param args arguments
                 * @return the Result object itself (invoke {@link #getResult()}) to get the return value
                 * @throws InvocationTargetException exception
                 * @throws IllegalAccessException    exception
                 */
                public Script run(String[] args) throws InvocationTargetException, IllegalAccessException {
                        result = scriptMethod.invoke(null, new Object[]{args});
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
         * compile the script
         *
         * @param script script
         * @return compiling result
         * @throws Exception exception
         */
        public Script compile(String script) throws Exception {
                return compile(new StringReader(script));
        }

        /**
         * compile the script
         *
         * @param scriptFile script file
         * @return compiling result
         * @throws Exception exception
         */
        public Script compile(File scriptFile) throws Exception {
                return compile(new FileReader(scriptFile));
        }

        /**
         * compile the script
         *
         * @param scriptReader script reader
         * @return compiling result
         * @throws Exception exception
         */
        public Script compile(Reader scriptReader) throws Exception {
                String nameForTheScript = "Script$LessTyping$";
                int i = 0;
                while (sources.containsKey(nameForTheScript + i)) ++i;

                nameForTheScript += i;

                StringBuilder sb = new StringBuilder();
                // class N
                //     static
                //         method(args:String[])
                //             ...
                //
                sb.append("class ").append(nameForTheScript)
                        .append("\n    static")
                        .append("\n        method(args:[]String)\n");
                String s;
                BufferedReader br = new BufferedReader(scriptReader);
                while ((s = br.readLine()) != null) {
                        for (int x = 0; x < 12; ++x) {
                                sb.append(" ");
                        }
                        sb.append(s).append("\n");
                }

                sources.put(nameForTheScript, sb.toString());

                ClassLoader loader = compiler.compile(sources);
                Class<?> scriptCls = loader.loadClass(nameForTheScript);
                Method scriptMethod = scriptCls.getMethod("method", String[].class);

                return new Script(loader, scriptCls, scriptMethod);
        }
}
