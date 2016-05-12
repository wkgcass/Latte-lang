package lt.compiler;

import lt.compiler.lexical.IllegalIndentationException;
import lt.compiler.syntactic.DuplicateVariableNameException;
import lt.compiler.syntactic.UnexpectedNewLayerException;
import lt.compiler.syntactic.UnknownTokenException;

import java.io.PrintStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * error manager<br>
 * controls compiling errors/info/warnings display and exception throwing
 */
public class ErrorManager {
        public static class Out {
                public PrintStream debug = null;
                public PrintStream info = System.out;
                public PrintStream warn = System.err;
                public PrintStream err = System.err;

                public static Out allNull() {
                        Out out = new Out();
                        out.debug = null;
                        out.info = null;
                        out.warn = null;
                        out.err = null;
                        return out;
                }
        }

        private final boolean fastFail;
        public Out out = new Out();
        public final List<CompilingError> errorList = new ArrayList<>();

        public final StringBuilder sb = new StringBuilder();

        public static class CompilingError {
                public final String msg;
                public final LineCol lineCol;
                public final int type;

                public static final int Syntax = 1;
                public static final int UnknownToken = 2;
                public static final int UnexpectedToken = 3;
                public static final int Indentation = 4;
                public static final int UnexpectedEnd = 5;
                public static final int UnexpectedNewLayer = 6;
                public static final int DuplicateVariableName = 7;

                private CompilingError(String msg, LineCol lineCol, int type) {
                        this.msg = msg;
                        this.lineCol = lineCol;
                        this.type = type;
                }
        }

        private static final DateFormat df = DateFormat.getDateTimeInstance();

        /**
         * construct an ErrorManager
         *
         * @param fastFail throw exception when meets an exception
         */
        public ErrorManager(boolean fastFail) {
                this.fastFail = fastFail;
        }

        private void print(String msg, PrintStream out) {
                if (out != null) {
                        msg = "[" + df.format(new Date()) + "]" + msg;
                        out.println(msg);
                        sb.append(msg).append("\n");
                }
        }

        /**
         * print an error to printStream
         *
         * @param msg the message to print
         */
        private void error(String msg) {
                print("[ ERROR ] " + msg, out.err);
        }

        /**
         * print a warning to printStream
         *
         * @param msg the message to print
         */
        public void warning(String msg) {
                print("[WARNING] " + msg, out.warn);
        }

        /**
         * print a info to printStream
         *
         * @param msg the message to print
         */
        public void info(String msg) {
                print("[ INFO  ] " + msg, out.info);
        }

        /**
         * print a debug info to printStream
         *
         * @param msg the message to print
         */
        public void debug(String msg) {
                print("[ DEBUG ] " + msg, out.debug);
        }

        /**
         * got a syntax exception
         *
         * @param msg     message
         * @param lineCol file,line,column info
         * @throws SyntaxException compiling error
         */
        public void SyntaxException(String msg, LineCol lineCol) throws SyntaxException {
                if (fastFail) throw new SyntaxException(msg, lineCol);
                error(msg + " at " + lineCol);
                errorList.add(new CompilingError(msg, lineCol, CompilingError.Syntax));
        }

        /**
         * got an unexpected end
         *
         * @param lineCol file,line,column info
         * @throws UnexpectedEndException compiling error
         */
        public void UnexpectedEndException(LineCol lineCol) throws UnexpectedEndException {
                if (fastFail) throw new UnexpectedEndException(lineCol);
                final String msg = "unexpected end";
                error(msg + " at " + lineCol);
                errorList.add(new CompilingError(msg, lineCol, CompilingError.UnexpectedEnd));
        }

        /**
         * occurred an unexpected token
         *
         * @param expected expected token structure
         * @param got      got token
         * @param lineCol  file,line,column info
         * @throws UnexpectedTokenException compiling error
         */
        public void UnexpectedTokenException(String expected, String got, LineCol lineCol) throws UnexpectedTokenException {
                if (fastFail) throw new UnexpectedTokenException(expected, got, lineCol);
                final String msg = "expecting " + expected + ", but got " + got;
                error(msg + " at " + lineCol);
                errorList.add(new CompilingError(msg, lineCol, CompilingError.UnexpectedToken));

        }

        /**
         * occurred an unexpected token
         *
         * @param token   unexpected token
         * @param lineCol file,line,column info
         * @throws UnexpectedTokenException compiling error
         */
        public void UnexpectedTokenException(String token, LineCol lineCol) throws UnexpectedTokenException {
                if (fastFail) throw new UnexpectedTokenException(token, lineCol);
                final String msg = "unexpected token " + token;
                error(msg + " at " + lineCol);
                errorList.add(new CompilingError(msg, lineCol, CompilingError.UnexpectedToken));
        }

        /**
         * occurred illegal indentation
         *
         * @param expectedIndent the expected indent
         * @param lineCol        file,line,column info
         * @throws IllegalIndentationException compiling error
         */
        public void IllegalIndentationException(int expectedIndent, LineCol lineCol) throws IllegalIndentationException {
                if (fastFail) throw new IllegalIndentationException(expectedIndent, lineCol);
                final String msg = "the indentation should be " + expectedIndent + " spaces";
                error(msg + " at " + lineCol);
                errorList.add(new CompilingError(msg, lineCol, CompilingError.Indentation));
        }

        /**
         * occurred an unexpected new Layer
         *
         * @param lineCol file,line,column info
         * @throws UnexpectedNewLayerException compiling error
         */
        public void UnexpectedNewLayerException(LineCol lineCol) throws UnexpectedNewLayerException {
                if (fastFail) throw new UnexpectedNewLayerException(lineCol);
                final String msg = "unexpected new layer";
                error(msg + " at " + lineCol);
                errorList.add(new CompilingError(msg, lineCol, CompilingError.UnexpectedNewLayer));
        }

        /**
         * occurred an unknown token
         *
         * @param token   the unknown token
         * @param lineCol file,line,column info
         * @throws UnknownTokenException compiling error
         */
        public void UnknownTokenException(String token, LineCol lineCol) throws UnknownTokenException {
                if (fastFail) throw new UnknownTokenException(token, lineCol);
                final String msg = "unknown token " + token;
                error(msg + " at " + lineCol);
                errorList.add(new CompilingError(msg, lineCol, CompilingError.UnknownToken));
        }

        public void DuplicateVariableNameException(String name, LineCol lineCol) throws DuplicateVariableNameException {
                if (fastFail) throw new DuplicateVariableNameException(name, lineCol);
                final String msg = "duplicate name " + name;
                error(msg + " at " + lineCol);
                errorList.add(new CompilingError(msg, lineCol, CompilingError.DuplicateVariableName));
        }
}
