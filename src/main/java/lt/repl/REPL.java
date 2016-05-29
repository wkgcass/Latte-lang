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
import lt.lang.Utils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
                        runCommands(args);
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
                                                System.out.println(":import <import-stmt>                      the import statement");
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
                                        } else if (cmd.startsWith(":import ")) {
                                                String im = cmd.substring(":import ".length()).trim();
                                                evaluator.addImport(im);
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

                                                                        if (line < 0) {
                                                                                System.err.println(t.getMessage());
                                                                        } else {

                                                                                String[] strs = stmt.split("\\n|\\r");
                                                                                String s = strs[line];
                                                                                System.err.println(s);
                                                                                for (int i = 0; i < col; ++i) {
                                                                                        System.err.print(" ");
                                                                                }
                                                                                System.err.print("^ ");
                                                                                System.err.println(t.getMessage());
                                                                        }
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

        /**
         * run the program with arguments
         *
         * @param args arguments
         */
        private static void runCommands(String[] args) {
                String command = args[0];
                switch (command) {
                        case "help":
                        case "-help":
                        case "--help":
                        case "-h":
                        case "/h":
                        case "/help":
                                // help
                                System.out.println("" +
                                        "usage: -s <script-location> [arguments [,...]]\n" +
                                        "       -c <source-directory> [-r] [-o <output-directory>] [-cp <classpath[:...]>]\n" +
                                        "       ClassName [-cp <classpath[:...]>]\n" +
                                        "       -repl" +
                                        "\n" +
                                        "-s       specify the script location and run the script\n" +
                                        "-c       specify the source file directory and compile *.lt files\n" +
                                        "-r       [option] add sub directory files to compiling list.\n" +
                                        "-o       [option] specify the output directory. (the source-directory/target/classes/ as default)\n" +
                                        "-cp      [option] the classpath. use ':' to separate the class-paths\n" +
                                        "-repl    start the repl (or run the program with 0 arguments)");
                                break;
                        case "-s":
                                // run scripts
                                // -s ?
                                if (args.length < 2) {
                                        System.err.println("invalid command -s. the script file location should be specified\n" +
                                                "see --help");
                                        return;
                                }
                                String path = args[1];
                                File f = new File(path);
                                ScriptCompiler s = new ScriptCompiler(ClassLoader.getSystemClassLoader());
                                try {
                                        ScriptCompiler.Script script = s.compile(f);
                                        String[] scriptArgs = new String[args.length - 2];
                                        System.arraycopy(args, 2, scriptArgs, 0, args.length - 2);
                                        script.run(scriptArgs);
                                } catch (Exception e) {
                                        e.printStackTrace();
                                }
                                break;
                        case "-c":
                                // compile
                                // -c ?
                                if (args.length < 2) {
                                        System.err.println("invalid command -c. the source directory should be specified\n" +
                                                "see --help");
                                        return;
                                }
                                String sourceDir = args[1];
                                boolean recursive = false;
                                String outputDir = sourceDir + File.separator + "target" + File.separator + "classes";
                                List<URL> classPaths = new ArrayList<>();

                                for (int i = 2; i < args.length; ++i) {
                                        String cmd = args[i];
                                        switch (cmd) {
                                                case "-r":
                                                        recursive = true;
                                                        break;
                                                case "-o":
                                                        if (args.length - 1 == i) {
                                                                System.err.println("invalid option -o. the output directory should be specified");
                                                                System.err.println("see --help");
                                                                return;
                                                        }
                                                        outputDir = args[++i];
                                                        break;
                                                case "-cp":
                                                        if (args.length - 1 == i) {
                                                                System.err.println("invalid option -cp. the class-path should be specified");
                                                                System.err.println("see --help");
                                                                return;
                                                        }
                                                        String[] class_paths = args[++i].split(":");
                                                        for (String class_path : class_paths) {
                                                                try {
                                                                        classPaths.add(new URL(new File(class_path).toURI().toString()));
                                                                } catch (MalformedURLException e) {
                                                                        System.err.println("[ERROR] " + e.getMessage());
                                                                        return;
                                                                }
                                                        }

                                                        break;
                                                default:
                                                        System.err.println("unknown option " + cmd);
                                                        System.err.println("see --help");
                                                        return;
                                        }
                                }

                                Compiler compiler = new Compiler();
                                File outputDirFile = new File(outputDir);
                                if (!outputDirFile.exists()) outputDirFile.mkdirs();
                                compiler.config.result.outputDir = outputDirFile;
                                compiler.config.classpath = classPaths;

                                try {
                                        compiler.compile(Utils.filesInDirectory(sourceDir, recursive));
                                } catch (Exception e) {
                                        System.err.println("[ERROR] " + e.getMessage());
                                        return;
                                }
                                break;
                        case "-repl":
                                // repl
                                main(new String[0]);
                                break;
                        default:
                                // run
                                List<URL> urls = new ArrayList<>();
                                try {
                                        String url = new File("").toURI().toString();
                                        urls.add(new URL(url));
                                } catch (MalformedURLException e) {
                                        System.err.println("[ERROR] " + e.getMessage());
                                        return;
                                }
                                String[] runArgs = new String[0];
                                loop:
                                for (int i = 1; i < args.length; ++i) {
                                        String cmd = args[i];
                                        switch (cmd) {
                                                case "-cp":
                                                        if (i == args.length - 1) {
                                                                System.err.println("invalid option -cp. the class-path should be specified");
                                                                System.err.println("see --help");
                                                                return;
                                                        }
                                                        String cps = args[++i];
                                                        for (String cp : cps.split(":")) {
                                                                try {
                                                                        urls.add(new URL(new File(cp).toURI().toString()));
                                                                } catch (MalformedURLException e) {
                                                                        System.err.println("[ERROR] " + e.getMessage());
                                                                        return;
                                                                }
                                                        }
                                                        break;
                                                case "-args":
                                                        runArgs = new String[args.length - 1 - i];
                                                        System.arraycopy(args, i + 1, runArgs, 0, runArgs.length);
                                                        break loop;
                                                default:
                                                        System.err.println("unknown option " + cmd);
                                                        System.err.println("see --help");
                                        }
                                }
                                URLClassLoader urlClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
                                try {
                                        Class<?> cls = urlClassLoader.loadClass(command.replace("::", "."));
                                        Method method = cls.getDeclaredMethod("main", String[].class);
                                        method.setAccessible(true);
                                        method.invoke(null, new Object[]{runArgs});
                                } catch (Exception e) {
                                        System.err.println("[ERROR] " + e.getMessage());
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
