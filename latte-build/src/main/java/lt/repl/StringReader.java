package lt.repl;

/**
 * reads a line
 */
public interface StringReader {
        void setIO(IO io);

        void setCtrlCHandler(CtrlCHandler handler);

        String read() throws Exception;
}
