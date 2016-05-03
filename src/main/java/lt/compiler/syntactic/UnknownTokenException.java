package lt.compiler.syntactic;

import lt.compiler.LineCol;
import lt.compiler.SyntaxException;

/**
 * unknown token
 */
public class UnknownTokenException extends SyntaxException {
        public UnknownTokenException(String token, LineCol lineCol) {
                super("unknown token " + token, lineCol);
        }
}
