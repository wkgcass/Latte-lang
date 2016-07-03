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

package lt.js;

import lt.compiler.ErrorManager;
import lt.compiler.Parser;
import lt.compiler.Scanner;
import lt.compiler.SyntaxException;
import lt.compiler.syntactic.Statement;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

/**
 * test js
 */
public class TestJsSupport {
        private String compile(String source) throws IOException, SyntaxException {
                ErrorManager err = new ErrorManager(true);
                Scanner scanner = new Scanner("test-js.ltjs", new StringReader(source), new Scanner.Properties(), err);
                Parser parser = new Parser(scanner.scan(), err);
                JSGenerator js = new JSGenerator(new HashMap<String, List<Statement>>() {{
                        put("test-js.ltjs", parser.parse());
                }}, err);
                return js.generate().get("test-js.ltjs").trim();
        }

        @Test
        public void testName() throws Exception {
                assertEquals("" +
                                "var x;\n" +
                                "var y = 1;"
                        ,
                        compile("" +
                                "var x\n" +
                                "y=1"));
        }

        @Test
        public void testLiteral() throws Exception {
                assertEquals("" +
                                "1;\n" +
                                "'abc';\n" +
                                "true;\n" +
                                "true;\n" +
                                "false;\n" +
                                "false;\n" +
                                "/\\b+/;\n" +
                                "/\\//;"
                        ,
                        compile("" +
                                "1\n" +
                                "'abc'\n" +
                                "true\n" +
                                "yes\n" +
                                "false\n" +
                                "no\n" +
                                "//\\b+//\n" +
                                "/////"));
        }

        @Test
        public void testOperator() throws Exception {
                assertEquals("" +
                                "++a;\n" +
                                "a++;\n" +
                                "a + b;"
                        , compile("" +
                                "++a\n" +
                                "a++\n" +
                                "a+b"));
        }

        @Test
        public void testAccess() throws Exception {
                assertEquals("" +
                                "a.b;\n" +
                                "a;"
                        ,
                        compile("" +
                                "a.b\n" +
                                "a"));
        }

        @Test
        public void testArray() throws Exception {
                assertEquals("" +
                                "[1, 2, 3];"
                        ,
                        compile("" +
                                "[1,2,3]"));
        }

        @Test
        public void testAssign() throws Exception {
                assertEquals("" +
                                "var a;\n" +
                                "a = 1;"
                        ,
                        compile("" +
                                "var a\n" +
                                "a=1"));
        }

        @Test
        public void testProcedure() throws Exception {
                assertEquals("" +
                                "(function(){\n" +
                                "    return 1 + 1;\n" +
                                "})();",
                        compile("" +
                                "(return 1+1)"));
        }

        @Test
        public void testIndex() throws Exception {
                assertEquals("" +
                                "a[1];\n" +
                                "a[1][2];",
                        compile("" +
                                "a[1]\n" +
                                "a[1,2]"));
        }

        @Test
        public void testLambda() throws Exception {
                assertEquals("" +
                        "function(x) {\n" +
                        "    return x + 1;\n" +
                        "};", compile("x->x+1"));
        }

        @Test
        public void testMap() throws Exception {
                assertEquals("" +
                        "{\n" +
                        "    'a' : 1,\n" +
                        "    'b' : 2\n" +
                        "};", compile("{'a':1,'b':2}"));
        }

        @Test
        public void testNew() throws Exception {
                assertEquals("new A();\nnew A(1);", compile("new A\nnew A(1)"));
        }

        @Test
        public void testNull() throws Exception {
                assertEquals("null;", compile("null"));
        }

        @Test
        public void testRequire() throws Exception {
                assertEquals("require('xx');", compile("require 'xx'"));
        }

        @Test
        public void testUndefinedExp() throws Exception {
                assertEquals("undefined;", compile("undefined"));
        }

        @Test
        public void testClassDef() throws Exception {
                assertEquals("" +
                                "function A(a, b, c) {\n" +
                                "    b = b ? b : 1;\n" +
                                "    c = c ? c : 2;\n" +
                                "    a + b + c;\n" +
                                "    this.a = a;\n" +
                                "    this.b = b;\n" +
                                "    this.c = c;\n" +
                                "}",
                        compile("" +
                                "class A(a,b=1,c=2)\n" +
                                "    a+b+c"));
        }

        @Test
        public void testFun() throws Exception {
                assertEquals("" +
                        "function F(a) {\n" +
                        "    return a + 1;\n" +
                        "}", compile("" +
                        "fun F(a)\n" +
                        "    return a + 1"));
        }

        @Test
        public void testFor() throws Exception {
                assertEquals("" +
                                "for (var i in x) {\n" +
                                "    i + 1;\n" +
                                "}",
                        compile("for i in x\n" +
                                "    i + 1"));
        }

        @Test
        public void testIf() throws Exception {
                assertEquals("" +
                                "if (a) {\n" +
                                "    return 1;\n" +
                                "} else if (b) {\n" +
                                "    return 2;\n" +
                                "} else if (c) {\n" +
                                "    return 3;\n" +
                                "} else {\n" +
                                "    return 4;\n" +
                                "}",
                        compile("if a\n" +
                                "    return 1\n" +
                                "elseif b\n" +
                                "    return 2\n" +
                                "elseif c\n" +
                                "    return 3\n" +
                                "else\n" +
                                "    return 4"));
        }

        @Test
        public void testPass() throws Exception {
                assertEquals("/* pass */;", compile("..."));
        }

        @Test
        public void testThrow() throws Exception {
                assertEquals("throw e;", compile("throw e"));
        }

        @Test
        public void testTry() throws Exception {
                assertEquals("" +
                                "try {\n" +
                                "    1 + 1;\n" +
                                "} catch (e) {\n" +
                                "    2 + 2;\n" +
                                "} finally {\n" +
                                "    3 + 3;\n" +
                                "}",
                        compile("try\n" +
                                "    1+1\n" +
                                "catch e\n" +
                                "    2+2\n" +
                                "finally\n" +
                                "    3+3"));
        }

        @Test
        public void testWhile() throws Exception {
                assertEquals("" +
                                "do {\n" +
                                "    1 + 1;\n" +
                                "} while (b);\n" +
                                "while (b) {\n" +
                                "    2 + 2;\n" +
                                "}",
                        compile("do\n" +
                                "    1+1\n" +
                                "while b\n" +
                                "while b\n" +
                                "    2 + 2"));
        }

        @Test
        public void testContinue() throws Exception {
                assertEquals("continue;", compile("continue"));
        }

        @Test
        public void testBreak() throws Exception {
                assertEquals("break;", compile("break"));
        }
}
