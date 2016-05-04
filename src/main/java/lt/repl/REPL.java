package lt.repl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Scanner;
import java.util.jar.JarFile;

/**
 * repl
 */
public class REPL {
        private static final String lineStarter = "lt> ";
        private static final String multipleLine = "  | ";

        private REPL() {
        }

        public static void main(String[] args) {
                System.out.println("Welcome to LessTyping");
                System.out.println("Type in expressions and double Enter to have them evaluated.");
                System.out.println("Type :help for more information.");
                System.out.println("for syntax help, please visit https://github.com/wkgcass/LessTyping/");
                System.out.println();
                System.out.print(lineStarter);

                JarLoader jarLoader = new JarLoader();

                Evaluator evaluator = new Evaluator(jarLoader);
                Scanner scanner = new Scanner(System.in);
                StringBuilder sb = new StringBuilder();
                while (true) {
                        String str = scanner.nextLine();
                        if (str.trim().startsWith(":")) {
                                String cmd = str.trim();
                                // some functions that controls the REPL
                                if (cmd.equals(":help")) {
                                        System.out.println(":q                          exit the REPL");
                                        System.out.println(":reset                      reset the repl to its initial state, forgetting all session entries");
                                        System.out.println(":restart                    restart the repl environment, drop all loaded jars");
                                        System.out.println(":                           set current input to empty string");
                                        System.out.println(":jar <jar-file-name>        load a jar file");
                                        System.out.println("-------------------------------------------");
                                        System.out.println("Compiler()                  instantiate a new Compiler");
                                        System.out.println("compiler << '<directory>'   add the <directory> to compiling list");
                                        System.out.println("compiler >> '<directory>'   set compiler output directory");
                                        System.out.println("compiler compile            start compiling and generate class files");
                                        System.out.println("compiler configure {}       configure the compiler with given configuration");
                                } else if (cmd.equals(":q")) {
                                        break;
                                } else if (cmd.equals(":reset")) {
                                        sb.delete(0, sb.length());
                                        evaluator = new Evaluator(jarLoader);
                                } else if (cmd.equals(":restart")) {
                                        sb.delete(0, sb.length());
                                        jarLoader = new JarLoader();
                                        evaluator = new Evaluator(jarLoader);
                                } else if (cmd.startsWith(":jar ")) {
                                        String jarFileName = cmd.substring(":jar ".length()).trim();
                                        try {
                                                JarFile jarFile = new JarFile(jarFileName);
                                                jarLoader.loadAll(jarFile);
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
                                                try {
                                                        Evaluator.Entry entry = evaluator.eval(sb.toString());
                                                        String name = entry.name;
                                                        Object o = entry.result;
                                                        if (name == null) {
                                                                showObjectStructure(o);
                                                        } else {
                                                                System.out.println(name + (o == null ? "" : " : " + o.getClass().getName()) + " = " + o);
                                                        }
                                                        System.out.print("\n" + lineStarter);
                                                } catch (Throwable t) {
                                                        t.printStackTrace();
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
