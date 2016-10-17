package lt.repl;

import jline.console.ConsoleReader;

/**
 * read line from jLine
 */
public class JLineLineReader implements LineReader {
        private ConsoleReader reader;

        public JLineLineReader() {
                System.out.println("using jline.console.ConsoleReader to read input");
        }

        @Override
        public String readLine() throws Exception {
                if (reader == null) {
                        reader = new ConsoleReader();
                }
                return reader.readLine();
        }
}
