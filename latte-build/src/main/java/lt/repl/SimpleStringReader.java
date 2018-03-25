package lt.repl;

import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * scanner line reader
 */
public class SimpleStringReader implements StringReader {
        private IO io;

        public SimpleStringReader() {
        }

        @Override
        public void setIO(IO io) {
                this.io = io;
                io.out.println("using java.util.Scanner to read input");
        }

        @Override
        public void setCtrlCHandler(final CtrlCHandler ctrlCSignalHandler) {
                SignalHandler handler = new SignalHandler() {
                        @Override
                        public void handle(Signal signal) {
                                if (!signal.getName().equals("INT")) {
                                        return;
                                }
                                ctrlCSignalHandler.handle();
                        }
                };
                Signal.handle(new Signal("INT"), handler);
        }

        @Override
        public String read() throws Exception {
                int b = io.in.read();
                if (b <= 0) {
                        return null;
                }
                return String.valueOf((char) b);
        }
}
