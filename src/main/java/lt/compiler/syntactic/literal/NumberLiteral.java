package lt.compiler.syntactic.literal;

import lt.compiler.LineCol;
import lt.compiler.syntactic.Literal;

/**
 * number literal
 */
public class NumberLiteral extends Literal {
        public NumberLiteral(String literal, LineCol lineCol) {
                super(NUMBER, literal, lineCol);
        }
}
