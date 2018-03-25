package lt.repl;

import java.io.*;

public class IO {
        public final InputStream inStream;
        public final OutputStream outStream;
        public final OutputStream errStream;
        public final Reader in;
        public final PrintStream out;
        public final PrintStream err;

        public IO(InputStream in, OutputStream out, OutputStream err) {
                this.inStream = in;
                this.outStream = out;
                this.errStream = err;

                this.in = new InputStreamReader(in);
                this.out = new PrintStream(out, true);
                this.err = new PrintStream(err, true);
        }
}
