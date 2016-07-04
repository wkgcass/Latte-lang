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

package lt.compiler.syntactic;

import lt.compiler.LineCol;

/**
 * literal "string", 'string', 1, 2.3, true/false/yes/no
 */
public abstract class Literal implements Expression {
        public static final int NUMBER = 0;
        public static final int STRING = 1;
        public static final int BOOL = 2;
        public static final int REGEX = 3;

        private int type;
        private String literal;

        private final LineCol lineCol;

        public Literal(int type, String literal, LineCol lineCol) {
                this.type = type;
                this.literal = literal;
                this.lineCol = lineCol;
        }

        public int type() {
                return type;
        }

        public String literal() {
                return literal;
        }

        @Override
        public String toString() {
                return literal;
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Literal literal1 = (Literal) o;

                return type == literal1.type && !(literal != null ? !literal.equals(literal1.literal) : literal1.literal != null);
        }

        @Override
        public int hashCode() {
                int result = type;
                result = 31 * result + (literal != null ? literal.hashCode() : 0);
                return result;
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }
}
