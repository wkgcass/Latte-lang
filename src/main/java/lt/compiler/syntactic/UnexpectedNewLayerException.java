package lt.compiler.syntactic;

import lt.compiler.LineCol;
import lt.compiler.SyntaxException;

/**
 * unexpected new layer. which means there's an unexpected ElementStartNode
 */
public class UnexpectedNewLayerException extends SyntaxException {
        public UnexpectedNewLayerException(LineCol lineCol) {
                super(lineCol);
        }
}
