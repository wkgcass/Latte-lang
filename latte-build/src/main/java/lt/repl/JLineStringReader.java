package lt.repl;

import jline.console.ConsoleReader;
import jline.console.UserInterruptException;

import java.io.IOException;

/**
 * read line from jLine
 */
public class JLineStringReader implements StringReader {
        private ConsoleReader reader;
        private IO io;
        private CtrlCHandler handler = null;

        public JLineStringReader() {
        }

        @Override
        public void setIO(IO io) {
                this.io = io;
                io.out.println("using jline.console.ConsoleReader to read input");
        }

        @Override
        public void setCtrlCHandler(CtrlCHandler handler) {
                this.handler = handler;
        }

        private void initReader() throws IOException {
                if (reader != null) {
                        reader.close();
                }
                reader = new ConsoleReader(io.inStream, io.outStream);
                reader.setHandleUserInterrupt(true);
        }

        @Override
        public String read() throws Exception {
                if (reader == null) {
                        initReader();
                }

                String line;
                while (true) {
                        try {
                                line = reader.readLine();
                                break;
                        } catch (UserInterruptException ignore) {
                                if (handler != null) {
                                        handler.handle();
                                }
                        }
                }
                if (line == null) return null;
                return line + '\n';
        }
}
