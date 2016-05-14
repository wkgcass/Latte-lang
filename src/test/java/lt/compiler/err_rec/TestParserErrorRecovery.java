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

package lt.compiler.err_rec;

import lt.compiler.*;
import lt.compiler.syntactic.AST;
import lt.compiler.syntactic.Statement;
import lt.compiler.syntactic.literal.NumberLiteral;
import lt.compiler.syntactic.operation.TwoVariableOperation;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * parser error recovery
 */
public class TestParserErrorRecovery {
        private List<Statement> parse(String code, ErrorManager err) throws IOException, SyntaxException {
                Scanner scanner = new Scanner("test.lt", new StringReader(code), new Scanner.Properties(), err);
                Parser parser = new Parser(scanner.scan(), err);
                return parser.parse();
        }

        @Test
        public void testNextNodeNotNull() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                        "1+,2"
                /*         ^Unexpected End */
                        , err);
                assertEquals(1, err.errorList.size());
                assertEquals(1, err.errorList.get(0).lineCol.line);
                assertEquals(3, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedEnd, err.errorList.get(0).type);

                assertEquals(1, statements.size());
                assertEquals(
                        new TwoVariableOperation(
                                "+",
                                new NumberLiteral("1", LineCol.SYNTHETIC),
                                new NumberLiteral("2", LineCol.SYNTHETIC), LineCol.SYNTHETIC),
                        statements.get(0)
                );
        }
}
