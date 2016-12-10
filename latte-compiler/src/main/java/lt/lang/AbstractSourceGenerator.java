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

package lt.lang;

import lt.compiler.*;
import lt.compiler.syntactic.Statement;
import lt.generator.SourceGenerator;

import java.util.List;

/**
 * the generator that save all init params to fields.
 */
public abstract class AbstractSourceGenerator implements SourceGenerator {
        protected List<Statement> ast;
        protected SemanticProcessor processor;
        protected SemanticScope scope;
        protected LineCol lineCol;
        protected ErrorManager err;

        @Override
        public void init(List<Statement> ast, SemanticProcessor processor, SemanticScope scope, LineCol lineCol, ErrorManager err) throws SyntaxException {
                this.ast = ast;
                this.processor = processor;
                this.scope = scope;
                this.lineCol = lineCol;
                this.err = err;
        }
}
