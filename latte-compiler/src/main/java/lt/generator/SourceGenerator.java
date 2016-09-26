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

package lt.generator;

import lt.compiler.*;
import lt.compiler.syntactic.Expression;
import lt.compiler.syntactic.Statement;

import java.util.List;

/**
 * a generator that generates source code.
 */
public interface SourceGenerator {
        int EXPRESSION = 1;
        int SERIALIZE = 2;
        int VALUE = 3;

        /**
         * initiate the generator. This method would be invoked after the generator is constructed.
         *
         * @param ast       AST
         * @param processor semantic processor
         * @param scope     semantic scope
         * @param lineCol   lineCol
         * @param err       Error manager
         * @throws SyntaxException compiling error
         */
        void init(List<Statement> ast, SemanticProcessor processor, SemanticScope scope, LineCol lineCol, ErrorManager err) throws SyntaxException;

        /**
         * generate an object. if the result type is {@link #EXPRESSION}, then the object <b>must</b> be an {@link Expression}.<br>
         * if the result type is {@link #SERIALIZE}, then the object <b>must</b> be a {@link java.io.Serializable} object.<br>
         * if the result type is {@link #VALUE}, then the object <b>must</b> be a {@link lt.compiler.semantic.Value} object.
         *
         * @return the transformed state
         * @throws SyntaxException exception
         */
        Object generate() throws SyntaxException;

        /**
         * @return {@link #EXPRESSION} or {@link #SERIALIZE} or {@link #VALUE}
         */
        int resultType();
}
