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

package lt.compiler.syntactic.operation;

import lt.compiler.CompileUtil;
import lt.compiler.LineCol;
import lt.compiler.syntactic.Expression;
import lt.compiler.syntactic.Operation;
import lt.compiler.syntactic.Statement;
import lt.lang.function.Function1;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class UnaryOneVariableOperation implements Operation {
        public UnaryOneVariableOperation(String operator, Expression e1, LineCol lineCol) {
                this.operator = operator;
                this.lineCol = lineCol;
                this.expressions = Collections.singletonList(e1);
        }

        private String operator;
        private List<Expression> expressions;
        private final LineCol lineCol;

        @Override
        public String operator() {
                return operator;
        }

        @Override
        public List<Expression> expressions() {
                return expressions;
        }

        @Override
        public int invokeOn() {
                return 0;
        }

        @Override
        public boolean isUnary() {
                return true;
        }

        @Override
        public String toString() {
                return "(" + operator() + " " + expressions().get(0) + ")";
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                UnaryOneVariableOperation that = (UnaryOneVariableOperation) o;

                return !(operator != null ? !operator.equals(that.operator) : that.operator != null) && !(expressions != null ? !expressions.equals(that.expressions) : that.expressions != null);
        }

        @Override
        public int hashCode() {
                int result = operator != null ? operator.hashCode() : 0;
                result = 31 * result + (expressions != null ? expressions.hashCode() : 0);
                return result;
        }

        @Override
        public LineCol line_col() {
                return lineCol;
        }

        @Override
        public void foreachInnerStatements(Function1<Boolean, ? super Statement> f) throws Exception {
                CompileUtil.visitStmt(expressions, f);
        }
}
