package lt.compiler;

/**
 * wrong syntax
 */
public class SyntaxException extends CompileException {
        public final LineCol lineCol;

        public SyntaxException(String msg, LineCol lineCol) {
                super(msg +
                        (lineCol == LineCol.SYNTHETIC ?
                                "" :
                                (" at " + lineCol.fileName + "(" + lineCol.line + "," + lineCol.column + ")")));
                this.lineCol = lineCol;
        }

        public SyntaxException(LineCol lineCol) {
                super("syntax exception " +
                        (lineCol == LineCol.SYNTHETIC ?
                                "" :
                                ("at " + lineCol.fileName + "(" + lineCol.line + "," + lineCol.column + ")")));
                this.lineCol = lineCol;
        }
}
