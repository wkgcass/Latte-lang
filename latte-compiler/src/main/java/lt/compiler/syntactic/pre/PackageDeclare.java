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

package lt.compiler.syntactic.pre;

import lt.compiler.LineCol;
import lt.compiler.syntactic.AST;
import lt.compiler.syntactic.Pre;
import lt.compiler.syntactic.Statement;
import lt.lang.function.Function2;

/**
 * declare current package
 */
public class PackageDeclare implements Pre {
        public final AST.PackageRef pkg;
        private final LineCol lineCol;

        public PackageDeclare(AST.PackageRef pkg, LineCol lineCol) {
                this.pkg = pkg;
                this.lineCol = lineCol;
        }

        @Override
        public String toString() {
                return "(package " + pkg + ")";
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                PackageDeclare that = (PackageDeclare) o;

                return pkg.equals(that.pkg);
        }

        @Override
        public int hashCode() {
                return pkg.hashCode();
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }

        @Override
        public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                // nothing to visit
        }
}
