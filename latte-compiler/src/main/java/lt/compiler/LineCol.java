/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 KuiGang Wang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
        public int length;
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

        @Override
        public String toString() {
                return fileName + "(" + line + ", " + column + ")";
        }
}
