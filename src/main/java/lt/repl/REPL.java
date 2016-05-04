package lt.repl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Scanner;

/**
 * repl
 */
public class REPL {
        private REPL() {
        }

        public static void main(String[] args) {
                System.out.println("LessTyping REPL");
                System.out.print(">");
                Evaluator evaluator = new Evaluator();
                Scanner scanner = new Scanner(System.in);
                StringBuilder sb = new StringBuilder();
                while (true) {
                        String str = scanner.nextLine();
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
                                                        System.out.println(name + " : " + o.getClass().getName() + " = " + o);
                                                }
                                                System.out.print("\n>");
                                        } catch (Throwable t) {
                                                t.printStackTrace();
                                                try {
                                                        Thread.sleep(10);
                                                } catch (InterruptedException e) {
                                                        e.printStackTrace();
                                                }
                                                System.out.print(">");
                                        }
                                        sb.delete(0, sb.length());
                                }
                        } else {
                                sb.append(str).append("\n");
                                System.out.print(">");
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
                        System.out.println("    " + f.getName() + " : " + f.getType() + " = " + value);
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
}
