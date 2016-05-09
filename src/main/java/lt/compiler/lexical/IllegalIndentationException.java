package lt.compiler.lexical;

import lt.compiler.LineCol;
import lt.compiler.SyntaxException;

/**
 * illegal indent. the indent should always be {@link lt.compiler.Scanner.Properties#_INDENTATION_} spaces
 */
public class IllegalIndentationException extends SyntaxException {
        public IllegalIndentationException(int _INDENT, LineCol lineCol) {
                super("the indentation should be " + _INDENT + " spaces", lineCol);
        }
}
