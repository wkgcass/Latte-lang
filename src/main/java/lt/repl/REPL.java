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

import lt.compiler.SyntaxException;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Scanner;

/**
 * repl
 */
public class REPL {
        private static final String lineStarter = "lt> ";
        private static final String multipleLine = "  | ";

        private REPL() {
        }

        public static void main(String[] args) {
                if (args != null && args.length != 0) {
                        // run scripts
                        String path = args[0];
                        File f = new File(path);
                        ScriptCompiler s = new ScriptCompiler(ClassLoader.getSystemClassLoader());
                        try {
                                ScriptCompiler.Script script = s.compile(f);
                                String[] scriptArgs = new String[args.length - 1];
                                System.arraycopy(args, 1, scriptArgs, 0, args.length - 1);
                                script.run(scriptArgs);
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                } else {
                        System.out.println("Welcome to LessTyping");
                        System.out.println("Type in expressions and double Enter to have them evaluated.");
                        System.out.println("Type :help for more information.");
                        System.out.println("for syntax help, please visit https://github.com/wkgcass/LessTyping/");
                        System.out.println();
                        System.out.print(lineStarter);

                        ClassPathLoader classPathLoader = new ClassPathLoader();

                        Evaluator evaluator = new Evaluator(classPathLoader);
                        Scanner scanner = new Scanner(System.in);
                        StringBuilder sb = new StringBuilder();
                        while (true) {
                                String str = scanner.nextLine();
                                if (str.trim().startsWith(":")) {
                                        String cmd = str.trim();
                                        // some functions that controls the REPL
                                        if (cmd.equals(":help")) {
                                                System.out.println(":q                                         exit the REPL");
                                                System.out.println(":reset                                     reset the repl to its initial state, forgetting all session entries");
                                                System.out.println(":restart                                   restart the repl environment, drop all loaded jars");
                                                System.out.println(":                                          set current input to empty string");
                                                System.out.println(":cp <class-path>                           load classes");
                                                System.out.println(":script <script-path>                      compile a script");
                                                System.out.println("----------------------------------------------------------------");
                                                System.out.println("Compiler()                                 construct a new Compiler");
                                                System.out.println("compiler >> '<directory>'                  set compiler output directory");
                                                System.out.println("compiler compile filesInDirectory('<directory>')");
                                                System.out.println("                                           start compiling and generate class files");
                                                System.out.println("----------------------------------------------------------------");
                                                System.out.println("ScriptCompiler()                           construct a new ScriptCompiler");
                                                System.out.println("scriptCompiler << File('')                 add source code file");
                                                System.out.println("scriptCompiler compile File('script')      compile the script");
                                        } else if (cmd.equals(":q")) {
                                                break;
                                        } else if (cmd.equals(":reset")) {
                                                sb.delete(0, sb.length());
                                                evaluator = new Evaluator(classPathLoader);
                                        } else if (cmd.equals(":restart")) {
                                                sb.delete(0, sb.length());
                                                classPathLoader = new ClassPathLoader();
                                                evaluator = new Evaluator(classPathLoader);
                                        } else if (cmd.startsWith(":cp ")) {
                                                String cp = cmd.substring(":cp ".length()).trim();
                                                try {
                                                        URL url = new URL(new File(cp).toURI().toString());
                                                        classPathLoader.load(url);
                                                } catch (Throwable t) {
                                                        t.printStackTrace();
                                                        sleep(10);
                                                }
                                        } else if (cmd.startsWith(":script ")) {
                                                String run = cmd.substring(":script ".length()).trim();
                                                try {
                                                        ScriptCompiler scriptCompiler = new ScriptCompiler(classPathLoader);
                                                        ScriptCompiler.Script script = scriptCompiler.compile(new File(run));
                                                        evaluator.put("script", script);
                                                        System.out.println("script : " + script.getClass().getName() + " = " + script);
                                                } catch (Throwable t) {
                                                        t.printStackTrace();
                                                        sleep(10);
                                                }
                                        } else if (cmd.equals(":")) {
                                                sb.delete(0, sb.length());
                                        } else {
                                                System.err.println("unknown command " + cmd + ", Type :help for more more information");
                                                sleep(10);
                                        }
                                        System.out.print("\n" + lineStarter);
                                } else {
                                        if (str.trim().isEmpty()) {
                                                if (sb.length() != 0) {
                                                        // do repl
                                                        String stmt = sb.toString();
                                                        try {
                                                                Evaluator.Entry entry = evaluator.eval(stmt);
                                                                String name = entry.name;
                                                                Object o = entry.result;
                                                                if (name == null) {
                                                                        showObjectStructure(o);
                                                                } else {
                                                                        System.out.println(name + (o == null ? "" : " : " + o.getClass().getName()) + " = " + o);
                                                                }
                                                                System.out.print("\n" + lineStarter);
                                                        } catch (Throwable t) {
                                                                if (t instanceof InvocationTargetException) {
                                                                        t.getCause().printStackTrace();
                                                                } else if (t instanceof SyntaxException) {
                                                                        int line = ((SyntaxException) t).lineCol.line - 1;
                                                                        int col = ((SyntaxException) t).lineCol.column - 1;
                                                                        String[] strs = stmt.split("\\n|\\r");
                                                                        String s = strs[line];
                                                                        System.err.println(s);
                                                                        for (int i = 0; i < col; ++i) {
                                                                                System.err.print(" ");
                                                                        }
                                                                        System.err.print("^ ");
                                                                        System.err.println(t.getMessage());
                                                                } else {
                                                                        t.printStackTrace();
                                                                }
                                                                sleep(10);
                                                                System.out.print(lineStarter);
                                                        }
                                                        sb.delete(0, sb.length());
                                                } else {
                                                        System.out.print(lineStarter);
                                                }
                                        } else {
                                                sb.append(str).append("\n");
                                                System.out.print(multipleLine);
                                        }
                                }

                        }
                }
        }

        private static void showObjectStructure(Object o) throws IllegalAccessException {
                Class<?> cls = o.getClass();
                String className = cls.getName();
                System.out.println("class " + className);
                for (Field f : cls.getDeclaredFields()) {
                        f.setAccessible(true);
                        Object value = f.get(o);
                        System.out.println("    " + f.getName() + " : " + f.getType().getName() + " = " + value);
                }
                for (Method m : cls.getDeclaredMethods()) {
                        System.out.print("    " + m.getName() + "(");
                        boolean isFirst = true;
                        for (Class<?> paramT : m.getParameterTypes()) {
                                if (isFirst) {
                                        isFirst = false;
                                } else {
                                        System.out.print(", ");
                                }
                                System.out.print(paramT);
                        }
                        System.out.println(") : " + m.getReturnType());
                }
        }

        private static void sleep(long millis) {
                try {
                        Thread.sleep(millis);
                } catch (InterruptedException e) {
                        e.printStackTrace();
                }
        }
}
