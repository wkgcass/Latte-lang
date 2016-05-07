package lt.compiler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * line, column and filename info
 */
public class LineCol {
        public final String fileName;
        public final int line;
        public final int column;
        public final Map<String, String> useDefine = new LinkedHashMap<>();

        /**
         * construct an LineCol that represents (filename, line, column and whether uses define replacement) of a Token
         *
         * @param fileName file name
         * @param line     line number starts from 1
         * @param column   column starts from 1
         */
        public LineCol(String fileName, int line, int column) {
                this.fileName = fileName;
                this.line = line;
                this.column = column;
        }

        /**
         * a synthetic line col object
         */
        public static final LineCol SYNTHETIC = new LineCol(null, 0, 0);
}
