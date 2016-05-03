package lt.compiler;

/**
 * line, column and filename info
 */
public class LineCol {
        public final String fileName;
        public final int line;
        public final int column;

        public LineCol(String fileName, int line, int column) {
                this.fileName = fileName;
                this.line = line;
                this.column = column;
        }

        public static final LineCol SYNTHETIC = new LineCol(null, 0, 0);
}
