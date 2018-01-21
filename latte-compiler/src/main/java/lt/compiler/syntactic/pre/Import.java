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

import lt.compiler.CompileUtil;
import lt.compiler.LineCol;
import lt.compiler.syntactic.AST;
import lt.compiler.syntactic.Pre;
import lt.compiler.syntactic.Statement;
import lt.lang.function.Function1;

/**
 * import
 */
public class Import implements Pre {
        public final AST.PackageRef pkg;
        public final AST.Access access;
        public final boolean importAll;
        public final boolean implicit;
        private final LineCol lineCol;

        public Import(AST.PackageRef pkg, AST.Access access, boolean importAll, boolean implicit, LineCol lineCol) {
                this.pkg = pkg;
                this.access = access;
                this.importAll = importAll;
                this.implicit = implicit;
                this.lineCol = lineCol;
        }

        @Override
        public String toString() {
                StringBuilder sb = new StringBuilder("(import ");
                if (implicit) {
                        sb.append("implicit ");
                }
                if (pkg == null) {
                        if (importAll) {
                                sb.append(access.toString()).append("._");
                        } else {
                                sb.append(access.toString());
                        }
                } else {
                        if (importAll) {
                                sb.append(pkg.toString()).append("._");
                        } else {
                                sb.append("(invalid import)");
                        }
                }
                sb.append(")");
                return sb.toString();
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }

        @Override
        public void foreachInnerStatements(Function1<Boolean, ? super Statement> f) throws Exception {
                CompileUtil.visitStmt(pkg, f);
                CompileUtil.visitStmt(access, f);
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Import anImport = (Import) o;

                if (importAll != anImport.importAll) return false;
                if (implicit != anImport.implicit) return false;
                if (pkg != null ? !pkg.equals(anImport.pkg) : anImport.pkg != null) return false;
                //
                return access != null ? access.equals(anImport.access) : anImport.access == null;

        }

        @Override
        public int hashCode() {
                int result = pkg != null ? pkg.hashCode() : 0;
                result = 31 * result + (access != null ? access.hashCode() : 0);
                result = 31 * result + (importAll ? 1 : 0);
                result = 31 * result + (implicit ? 1 : 0);
                return result;
        }
}
