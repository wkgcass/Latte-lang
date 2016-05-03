package lt.compiler.syntactic.literal;

import lt.compiler.LineCol;
import lt.compiler.syntactic.Literal;

/**
 * bool literal
 */
public class BoolLiteral extends Literal {
        public BoolLiteral(String literal, LineCol lineCol) {
                super(BOOL, literal, lineCol);
        }
}
