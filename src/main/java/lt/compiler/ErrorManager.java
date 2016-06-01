/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 KuiGang Wang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package lt.compiler;

import lt.compiler.lexical.IllegalIndentationException;
import lt.compiler.syntactic.DuplicateVariableNameException;
import lt.compiler.syntactic.UnexpectedNewLayerException;
import lt.compiler.syntactic.UnknownTokenException;

import java.io.PrintStream;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * controls compiling errors/info/warnings display and exception throwing.
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

        /**
         * records {fileName =&gt;{LineNumber=&gt;LineContent}}
         */
        public final Map<String, Map<Integer, String>> lineRecord = new ConcurrentHashMap<>();

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

        /**
         * put the line record
         *
         * @param file    file name
         * @param line    the line number
         * @param content the content of the line
         */
        public void putLineRecord(String file, int line, String content) {
                Map<Integer, String> map;
                if (lineRecord.containsKey(file)) {
                        map = lineRecord.get(file);
                } else {
                        map = new HashMap<>();
                        lineRecord.put(file, map);
                }

                map.put(line, content);
        }

        /**
         * build error info. The error info looks like :<br>
         * <pre>
         * the statement
         *     ^
         * </pre><br>
         * detailed info can directly concat to this string
         *
         * @param file the file
         * @param line line
         * @param col  column
         * @return the error info
         */
        public String buildErrInfo(String file, int line, int col) {
                if (!lineRecord.containsKey(file)) return "";
                String content = lineRecord.get(file).get(line);
                if (content == null) return "";
                StringBuilder sb = new StringBuilder("\n");
                sb.append(content).append("\n");
                for (int i = 1; i < col; ++i) {
                        sb.append(" ");
                }
                sb.append("^ ");
                return sb.toString();
        }

        /**
         * invoke {@link #buildErrInfo(String, int, int)}
         *
         * @param lineCol line col
         * @return the error info
         */
        public String buildErrInfo(LineCol lineCol) {
                if (lineCol == null || lineCol.fileName == null || lineCol.line <= 0 || lineCol.column <= 0) return "";
                return buildErrInfo(lineCol.fileName, lineCol.line, lineCol.column);
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
        public void error(String msg) {
                print("[ERROR] " + msg, out.err);
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
                print("[INFO] " + msg, out.info);
        }

        /**
         * print a debug info to printStream
         *
         * @param msg the message to print
         */
        public void debug(String msg) {
                print("[DEBUG] " + msg, out.debug);
        }

        /**
         * got a syntax exception
         *
         * @param msg     message
         * @param lineCol file,line,column info
         * @throws SyntaxException compiling error
         */
        public void SyntaxException(String msg, LineCol lineCol) throws SyntaxException {
                msg = buildErrInfo(lineCol) + msg;

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
                String msg = "expecting " + expected + ", but got " + got;
                msg = buildErrInfo(lineCol) + msg;

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
                String msg = "unexpected token " + token;
                msg = buildErrInfo(lineCol) + msg;

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
                String msg = "the indentation should be " + expectedIndent + " spaces";
                msg = buildErrInfo(lineCol) + msg;

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
                String msg = "unexpected new layer";
                msg = buildErrInfo(lineCol) + msg;

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
                String msg = "unknown token " + token;
                msg = buildErrInfo(lineCol) + msg;

                error(msg + " at " + lineCol);
                errorList.add(new CompilingError(msg, lineCol, CompilingError.UnknownToken));
        }

        public void DuplicateVariableNameException(String name, LineCol lineCol) throws DuplicateVariableNameException {
                if (fastFail) throw new DuplicateVariableNameException(name, lineCol);
                String msg = "duplicate name " + name;
                msg = buildErrInfo(lineCol) + msg;

                error(msg + " at " + lineCol);
                errorList.add(new CompilingError(msg, lineCol, CompilingError.DuplicateVariableName));
        }
}
