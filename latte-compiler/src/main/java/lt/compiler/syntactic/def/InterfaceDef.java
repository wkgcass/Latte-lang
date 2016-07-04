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

package lt.compiler.syntactic.def;

import lt.compiler.LineCol;
import lt.compiler.syntactic.*;
import lt.compiler.syntactic.pre.Modifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * interface definition
 */
public class InterfaceDef implements Definition {
        public final String name;
        public final Set<Modifier> modifiers;
        public final List<AST.Access> superInterfaces;
        public final List<Statement> statements;
        public final Set<AST.Anno> annos;

        private final LineCol lineCol;

        public InterfaceDef(String name, Set<Modifier> modifiers, List<AST.Access> superInterfaces, Set<AST.Anno> annos, List<Statement> statements, LineCol lineCol) {
                this.name = name;
                this.lineCol = lineCol;
                this.modifiers = new HashSet<>(modifiers);
                this.superInterfaces = superInterfaces;
                this.statements = statements;
                this.annos = new HashSet<>(annos);
        }

        @Override
        public String toString() {
                StringBuilder sb = new StringBuilder("(");
                for (AST.Anno anno : annos) {
                        sb.append(anno).append(" ");
                }
                for (Modifier m : modifiers) {
                        sb.append(m).append(" ");
                }
                sb.append("interface ").append(name);
                if (!superInterfaces.isEmpty()) {
                        sb.append(" : ");
                        boolean isFirst = true;
                        for (AST.Access access : superInterfaces) {
                                if (isFirst) {
                                        isFirst = false;
                                } else {
                                        sb.append(",");
                                }
                                sb.append(access);
                        }
                }
                sb.append(" ").append(statements).append(")");
                return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                InterfaceDef that = (InterfaceDef) o;

                if (!name.equals(that.name)) return false;
                if (!modifiers.equals(that.modifiers)) return false;
                if (!superInterfaces.equals(that.superInterfaces)) return false;
                if (!statements.equals(that.statements)) return false;
                return annos.equals(that.annos);
        }

        @Override
        public int hashCode() {
                int result = name.hashCode();
                result = 31 * result + modifiers.hashCode();
                result = 31 * result + superInterfaces.hashCode();
                result = 31 * result + statements.hashCode();
                result = 31 * result + annos.hashCode();
                return result;
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }
}
