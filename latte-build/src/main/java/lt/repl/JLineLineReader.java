package lt.repl;

import jline.ConsoleReader;

/**
 * read line from jLine
 */
public class JLineLineReader implements LineReader {
        private ConsoleReader reader;

        @Override
        public String readLine() throws Exception {
                if (reader == null) {
                        reader = new ConsoleReader();
                }
                return reader.readLine();
        }
}
