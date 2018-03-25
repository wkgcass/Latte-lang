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
import lt.repl.scripting.Config;
import lt.repl.scripting.EvalEntry;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * repl
 */
public class REPL {
        private final String lineStarter = "lt> ";
        private final String multipleLine = "  | ";
        private StringBuilder replSourceRec = new StringBuilder();
        private final IO io;
        private final CtrlCHandler.ExitCallback exitCallback;
        private StringReader reader;

        public REPL(StringReader reader, IO io, CtrlCHandler.ExitCallback exitCallback) {
                this.reader = reader;
                this.io = io;
                this.exitCallback = exitCallback;
        }

        public void start() throws Exception {
                io.out.println("Welcome to Latte lang");
                io.out.println("Type in expressions and double Enter to have them evaluated.");
                io.out.println("Type :help for more information.");
                io.out.println("for syntax help, please visit https://github.com/wkgcass/Latte-lang/");
                io.out.println();

                ClassPathLoader classPathLoader = new ClassPathLoader(Thread.currentThread().getContextClassLoader());

                Evaluator evaluator = new Evaluator(classPathLoader);
                reader.setIO(io);
                io.out.println();
                io.out.print(lineStarter);

                StringBuilder strBuilder = new StringBuilder();
                out:
                while (true) {
                        while (true) {
                                String s = reader.read();
                                if (s == null) {
                                        // the reader reached EOF
                                        break out;
                                }
                                if (s.endsWith("\n") || s.endsWith("\r")) {
                                        if (s.endsWith("\r\n")) {
                                                strBuilder.append(s.subSequence(0, s.length() - 2));
                                        } else {
                                                strBuilder.append(s.subSequence(0, s.length() - 1));
                                        }
                                        break;
                                }
                                strBuilder.append(s);
                        }
                        String str = strBuilder.toString();
                        strBuilder.delete(0, strBuilder.length());
                        if (str.trim().startsWith(":")) {
                                String cmd = str.trim();
                                // some functions that controls the REPL
                                if (cmd.equals(":help")) {
                                        io.out.println(":q                                         exit the REPL");
                                        io.out.println(":reset                                     reset the repl to its initial state, forgetting all session entries");
                                        io.out.println(":restart                                   restart the repl environment, drop all loaded jars");
                                        io.out.println(":                                          set current input to empty string");
                                        io.out.println(":cp <class-path>                           load classes");
                                        io.out.println(":script <script-path>                      compile a script");
                                        io.out.println("----------------------------------------------------------------");
                                        io.out.println(":scanner-indent                            use IndentScanner to scan input");
                                        io.out.println(":scanner-brace                             use BraceScanner to scan input");
                                        io.out.println("----------------------------------------------------------------");
                                        io.out.println("Compiler()                                 construct a new Compiler");
                                        io.out.println("compiler >> '<directory>'                  set compiler output directory");
                                        io.out.println("compiler compile filesInDirectory('<directory>')");
                                        io.out.println("                                           start compiling and generate class files");
                                        io.out.println("----------------------------------------------------------------");
                                        io.out.println("ScriptCompiler()                           construct a new ScriptCompiler");
                                        io.out.println("scriptCompiler << File('')                 add source code file");
                                        io.out.println("scriptCompiler compile File('script')      compile the script");
                                } else if (cmd.equals(":q")) {
                                        break;
                                } else if (cmd.equals(":reset")) {
                                        replSourceRec.delete(0, replSourceRec.length());
                                        evaluator = new Evaluator(classPathLoader);
                                } else if (cmd.equals(":restart")) {
                                        replSourceRec.delete(0, replSourceRec.length());
                                        classPathLoader = new ClassPathLoader(Thread.currentThread().getContextClassLoader());
                                        evaluator = new Evaluator(classPathLoader);
                                } else if (cmd.startsWith(":cp ")) {
                                        String cp = cmd.substring(":cp ".length()).trim();
                                        try {
                                                URL url = new URL(new File(cp).toURI().toString());
                                                classPathLoader.load(url);
                                        } catch (Throwable t) {
                                                t.printStackTrace(io.err);
                                                sleep(10);
                                        }
                                } else if (cmd.startsWith(":script ")) {
                                        String run = cmd.substring(":script ".length()).trim();
                                        try {
                                                ScriptCompiler scriptCompiler = new ScriptCompiler(classPathLoader);
                                                ScriptCompiler.Script script = scriptCompiler.compile(new File(run));
                                                evaluator.put("script", script);
                                                io.out.println("script : " + script.getClass().getName() + " = " + script);
                                        } catch (Throwable t) {
                                                t.printStackTrace(io.err);
                                                sleep(10);
                                        }
                                } else if (cmd.equals(":scanner-indent")) {
                                        evaluator.setScannerType(Config.SCANNER_TYPE_INDENT);
                                } else if (cmd.equals(":scanner-brace")) {
                                        evaluator.setScannerType(Config.SCANNER_TYPE_BRACE);
                                } else if (cmd.equals(":")) {
                                        replSourceRec.delete(0, replSourceRec.length());
                                } else {
                                        io.err.println("unknown command " + cmd + ", Type :help for more more information");
                                        sleep(10);
                                }
                                io.out.print("\n" + lineStarter);
                        } else {
                                if (str.trim().isEmpty()) {
                                        if (replSourceRec.length() != 0) {
                                                // do repl
                                                String stmt = replSourceRec.toString();
                                                try {
                                                        EvalEntry entry = evaluator.eval(stmt);
                                                        String name = entry.name;
                                                        Object o = entry.result;
                                                        if (name == null) {
                                                                showObjectStructure(o);
                                                        } else {
                                                                io.out.println(name + " : " + entry.type.getName().replaceAll("\\.", "::") + " = " + o);
                                                        }
                                                        io.out.print("\n" + lineStarter);
                                                } catch (Throwable t) {
                                                        if (t instanceof InvocationTargetException) {
                                                                t.getCause().printStackTrace(io.err);
                                                        } else if (t instanceof SyntaxException) {
                                                                int line = ((SyntaxException) t).lineCol.line - 1;
                                                                int col = ((SyntaxException) t).lineCol.column - 1;

                                                                if (line < 0) {
                                                                        io.err.println(t.getMessage());
                                                                } else {

                                                                        String[] strs = stmt.split("\\n|\\r");
                                                                        String s = strs[line];
                                                                        io.err.println(s);
                                                                        for (int i = 0; i < col; ++i) {
                                                                                io.err.print(" ");
                                                                        }
                                                                        io.err.print("^ ");
                                                                        io.err.println(t.getMessage());
                                                                }
                                                        } else {
                                                                t.printStackTrace(io.err);
                                                        }
                                                        sleep(10);
                                                        io.out.print(lineStarter);
                                                }
                                                replSourceRec.delete(0, replSourceRec.length());
                                        } else {
                                                io.out.print(lineStarter);
                                        }
                                } else {
                                        replSourceRec.append(str).append("\n");
                                        io.out.print(multipleLine);
                                }
                        }

                }
                exitCallback.exit();
        }

        private void showObjectStructure(Object o) throws IllegalAccessException {
                Class<?> cls = o.getClass();
                String className = cls.getName();
                io.out.println("class " + className);
                for (Field f : cls.getDeclaredFields()) {
                        f.setAccessible(true);
                        Object value = f.get(o);
                        io.out.println("    " + f.getName() + " : " + f.getType().getName().replaceAll("\\.", "::") + " = " + value);
                }
                for (Method m : cls.getDeclaredMethods()) {
                        io.out.print("    " + m.getName() + "(");
                        boolean isFirst = true;
                        for (Class<?> paramT : m.getParameterTypes()) {
                                if (isFirst) {
                                        isFirst = false;
                                } else {
                                        io.out.print(", ");
                                }
                                io.out.print(paramT);
                        }
                        io.out.println(") : " + m.getReturnType().getName().replaceAll("\\.", "::"));
                }
        }

        private void sleep(long millis) {
                try {
                        Thread.sleep(millis);
                } catch (InterruptedException e) {
                        e.printStackTrace(io.err);
                }
        }

        public void handleCtrlC() {
                // listen ctrl-c
                CtrlCHandler ctrlCHandler = new CtrlCHandlerImpl(io);
                ctrlCHandler.setExitCallback(exitCallback);
                ctrlCHandler.setAlert(new Runnable() {
                        @Override
                        public void run() {
                                io.out.print(lineStarter);
                                replSourceRec.delete(0, replSourceRec.length());
                        }
                });
                reader.setCtrlCHandler(ctrlCHandler);
        }
}
