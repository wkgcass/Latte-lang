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
import java.util.Iterator;
import java.util.Set;

/**
 * define a variable
 */
public class VariableDef implements Definition, Expression {
        private String name;
        private AST.Access type;
        private Expression init;
        private Set<Modifier> modifiers;
        private Set<AST.Anno> annos;

        private final LineCol lineCol;

        public VariableDef(String name, Set<Modifier> modifiers, Set<AST.Anno> annos, LineCol lineCol) {
                // remove var from modifiers
                Iterator<Modifier> it = modifiers.iterator();
                while (it.hasNext()) {
                        Modifier m = it.next();
                        if (m.modifier.equals(Modifier.Available.VAR)) {
                                it.remove();
                                break;
                        }
                }

                this.name = name;
                this.lineCol = lineCol;
                this.modifiers = new HashSet<Modifier>(modifiers);
                this.annos = new HashSet<AST.Anno>(annos);
        }

        public String getName() {
                return name;
        }

        public AST.Access getType() {
                return type;
        }

        public void setType(AST.Access type) {
                this.type = type;
        }

        public Expression getInit() {
                return init;
        }

        public void setInit(Expression init) {
                this.init = init;
        }

        public Set<Modifier> getModifiers() {
                return modifiers;
        }

        public Set<AST.Anno> getAnnos() {
                return annos;
        }

        @Override
        public String toString() {
                StringBuilder sb = new StringBuilder("VariableDef(");
                for (AST.Anno anno : annos) {
                        sb.append(anno).append(" ");
                }
                for (Modifier m : modifiers) {
                        sb.append(m).append(" ");
                }
                sb.append("(").append(name).append(")");
                if (type != null) {
                        sb.append(" : ").append(type);
                }
                if (init != null) {
                        sb.append(" = ").append(init);
                }
                sb.append(")");
                return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                VariableDef that = (VariableDef) o;

                if (!name.equals(that.name)) return false;
                if (type != null ? !type.equals(that.type) : that.type != null) return false;
                if (init != null ? !init.equals(that.init) : that.init != null) return false;
                if (!modifiers.equals(that.modifiers)) return false;
                //
                return annos.equals(that.annos);
        }

        @Override
        public int hashCode() {
                int result = name.hashCode();
                result = 31 * result + (type != null ? type.hashCode() : 0);
                result = 31 * result + (init != null ? init.hashCode() : 0);
                result = 31 * result + modifiers.hashCode();
                result = 31 * result + annos.hashCode();
                return result;
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }
}
