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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * define a method
 */
public class MethodDef implements Definition {
        public final String name;
        public final Set<Modifier> modifiers;
        public final AST.Access returnType;
        public final List<VariableDef> params;
        public final Set<AST.Anno> annos;
        public final List<Statement> body;

        private final LineCol lineCol;

        public MethodDef(String name, Set<Modifier> modifiers, AST.Access returnType, List<VariableDef> params, Set<AST.Anno> annos, List<Statement> body, LineCol lineCol) {
                this.name = name;
                this.lineCol = lineCol;
                this.modifiers = new HashSet<>(modifiers);
                this.returnType = returnType;
                this.params = params;
                this.annos = new HashSet<>(annos);
                this.body = new ArrayList<>(body);
        }

        @Override
        public String toString() {
                StringBuilder sb = new StringBuilder("MethodDef(");
                for (AST.Anno anno : annos) {
                        sb.append(anno).append("");
                }
                for (Modifier m : modifiers) {
                        sb.append(m).append(" ");
                }
                sb.append(name).append("(");
                boolean isFirst = true;
                for (VariableDef v : params) {
                        if (isFirst)
                                isFirst = false;
                        else
                                sb.append(",");
                        sb.append(v);
                }
                sb.append(")");
                if (returnType != null)
                        sb.append(":").append(returnType);

                sb.append(body);

                sb.append(")");
                return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                MethodDef methodDef = (MethodDef) o;

                if (!name.equals(methodDef.name)) return false;
                if (!modifiers.equals(methodDef.modifiers)) return false;
                if (returnType != null ? !returnType.equals(methodDef.returnType) : methodDef.returnType != null)
                        return false;
                if (!params.equals(methodDef.params)) return false;
                if (!annos.equals(methodDef.annos)) return false;
                //
                return body.equals(methodDef.body);
        }

        @Override
        public int hashCode() {
                int result = name.hashCode();
                result = 31 * result + modifiers.hashCode();
                result = 31 * result + (returnType != null ? returnType.hashCode() : 0);
                result = 31 * result + params.hashCode();
                result = 31 * result + annos.hashCode();
                result = 31 * result + body.hashCode();
                return result;
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }
}
