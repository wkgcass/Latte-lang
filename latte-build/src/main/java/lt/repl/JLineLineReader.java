package lt.repl;

import java.lang.reflect.Method;

/**
 * read line from jLine
 */
public class JLineLineReader implements LineReader {
        private final Class<?> ConsoleReader;
        private final Method readLine;
        private Object reader;

        public JLineLineReader() {
                try {
                        ConsoleReader = Class.forName("jline.console.ConsoleReader");
                        readLine = ConsoleReader.getMethod("readLine");
                } catch (Throwable e) {
                        throw new Error(e);
                }
                System.out.println("using jline.console.ConsoleReader to read input");
        }

        @Override
        public String readLine() throws Exception {
                if (reader == null) {
                        reader = ConsoleReader.newInstance();
                }

                return (String) readLine.invoke(reader);
        }
}
