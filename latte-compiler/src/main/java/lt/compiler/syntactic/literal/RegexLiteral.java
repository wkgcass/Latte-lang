package lt.compiler.syntactic.literal;

import lt.compiler.LineCol;
import lt.compiler.syntactic.Literal;

/**
 * regex literal
 */
public class RegexLiteral extends Literal {
        public RegexLiteral(String literal, LineCol lineCol) {
                super(REGEX, literal, lineCol);
        }
}
