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

package lt.compiler.semantic;

import lt.compiler.LineCol;

import java.util.ArrayList;
import java.util.List;

/**
 * invokable
 */
abstract public class SInvokable extends SMember {
        private final List<SParameter> parameters = new ArrayList<SParameter>();
        private STypeDef returnType;
        private final List<Instruction> statements = new ArrayList<Instruction>();
        private final List<ExceptionTable> exceptionTables = new ArrayList<ExceptionTable>();
        private final InvokableMeta meta = new InvokableMeta();

        public SInvokable(LineCol lineCol) {
                super(lineCol);
        }

        public void setReturnType(STypeDef returnType) {
                this.returnType = returnType;
        }

        public List<SParameter> getParameters() {
                return parameters;
        }

        public STypeDef getReturnType() {
                return returnType;
        }

        public List<Instruction> statements() {
                return statements;
        }

        public List<ExceptionTable> exceptionTables() {
                return exceptionTables;
        }

        public InvokableMeta meta() {
                return meta;
        }
}
