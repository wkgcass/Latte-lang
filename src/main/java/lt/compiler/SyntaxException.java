package lt.compiler;

/**
 * wrong syntax
 */
public class SyntaxException extends CompileException {
        public final LineCol lineCol;

        public SyntaxException(String msg, LineCol lineCol) {
                super(msg +
                        (lineCol == LineCol.SYNTHETIC
                                ? ""
                                : (" at " + lineCol.fileName + "(" + lineCol.line + "," + lineCol.column + ")")) +
                        (lineCol.useDefine.isEmpty()
                                ? ""
                                : "\nThis line uses defined replacement (" + lineCol.useDefine + "), the column might not be precise"));
                this.lineCol = lineCol;
        }

        public SyntaxException(LineCol lineCol) {
                this("syntax exception", lineCol);
        }
}
