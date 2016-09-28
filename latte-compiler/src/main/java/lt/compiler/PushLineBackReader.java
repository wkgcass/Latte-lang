package lt.compiler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;

/**
 * a line reader that can push lines back from the outside.
 */
public class PushLineBackReader extends Reader {
        private final BufferedReader reader;
        private final LinkedList<String> lines = new LinkedList<>();

        public PushLineBackReader(Reader reader) {
                if (reader instanceof BufferedReader) {
                        this.reader = (BufferedReader) reader;
                } else {
                        this.reader = new BufferedReader(reader);
                }
        }

        public String readLine() throws IOException {
                if (lines.isEmpty()) {
                        return reader.readLine();
                } else {
                        return lines.pop();
                }
        }

        public void push(String line) {
                lines.push(line);
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
                return reader.read(cbuf, off, len);
        }

        @Override
        public void close() throws IOException {
                reader.close();
        }
}
