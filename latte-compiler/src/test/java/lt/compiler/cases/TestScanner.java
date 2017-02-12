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

package lt.compiler.cases;

import lt.compiler.ErrorManager;
import lt.compiler.IndentScanner;
import lt.compiler.Properties;
import lt.compiler.SyntaxException;
import lt.compiler.lexical.*;
import org.junit.Test;

import java.io.StringReader;

import static org.junit.Assert.*;

/**
 * test
 */
public class TestScanner {
        @Test
        public void testPkg() throws Exception {
                // package lt.test
                IndentScanner processor = new IndentScanner("test", new StringReader("package lt::test"), new Properties(), new ErrorManager(true));
                ElementStartNode root = processor.scan();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "package", TokenType.KEY);
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "lt", TokenType.VALID_NAME);
                args.previous = new Element(args, "::", TokenType.SYMBOL);
                args.previous = new Element(args, "test", TokenType.VALID_NAME);

                assertEquals(root2, root);
        }

        @Test
        public void testImport() throws Exception {
                // import packageName._
                IndentScanner processor = new IndentScanner("test", new StringReader("" +
                        "import Package::name::_\n" +
                        "import Package::name::Test"), new Properties(), new ErrorManager(true));
                ElementStartNode root = processor.scan();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "import", TokenType.KEY);
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "Package", TokenType.VALID_NAME);
                args.previous = new Element(args, "::", TokenType.SYMBOL);
                args.previous = new Element(args, "name", TokenType.VALID_NAME);
                args.previous = new Element(args, "::", TokenType.SYMBOL);
                args.previous = new Element(args, "_", TokenType.VALID_NAME);
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, "import", TokenType.KEY);
                args.previous = new Element(args, "Package", TokenType.VALID_NAME);
                args.previous = new Element(args, "::", TokenType.SYMBOL);
                args.previous = new Element(args, "name", TokenType.VALID_NAME);
                args.previous = new Element(args, "::", TokenType.SYMBOL);
                new Element(args, "Test", TokenType.VALID_NAME);

                assertEquals(root2, root);
        }

        @Test
        public void testCls1() throws Exception {
                // class ClassName
                IndentScanner processor = new IndentScanner("test", new StringReader("class ClassName"), new Properties(), new ErrorManager(true));
                ElementStartNode root = processor.scan();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "class", TokenType.KEY);
                root2.setLinkedNode(args.previous);
                new Element(args, "ClassName", TokenType.VALID_NAME);

                assertEquals(root2, root);
        }

        @Test
        public void testCls2() throws Exception {
                // class ClassName(arg1:Type1,arg2:Type2)
                IndentScanner processor = new IndentScanner("test", new StringReader("class ClassName(arg1,arg2=value2)"), new Properties(), new ErrorManager(true));
                ElementStartNode root = processor.scan();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "class", TokenType.KEY);
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "ClassName", TokenType.VALID_NAME);
                args.previous = new Element(args, "(", TokenType.SYMBOL);
                ElementStartNode startNode = new ElementStartNode(args, 4);
                args.previous = startNode;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                new Element(args, ")", TokenType.SYMBOL);

                args.previous = null;
                args.previous = new Element(args, "arg1", TokenType.VALID_NAME);
                startNode.setLinkedNode(args.previous);

                args.previous = new EndingNode(args, EndingNode.STRONG);

                args.previous = new Element(args, "arg2", TokenType.VALID_NAME);
                args.previous = new Element(args, "=", TokenType.SYMBOL);
                args.previous = new Element(args, "value2", TokenType.VALID_NAME);

                assertEquals(root2, root);
        }

        @Test
        public void testVariable3() throws Exception {
                // val value:Type = 1
                IndentScanner processor = new IndentScanner("test", new StringReader("val value = 1"), new Properties(), new ErrorManager(true));
                ElementStartNode root = processor.scan();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "val", TokenType.MODIFIER);
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "value", TokenType.VALID_NAME);
                args.previous = new Element(args, "=", TokenType.SYMBOL);
                new Element(args, "1", TokenType.NUMBER);

                assertEquals(root2, root);
        }

        @Test
        public void testMethod1() throws Exception {
                //val trim(input)
                //    <input.trim()
                IndentScanner processor = new IndentScanner("test", new StringReader("" +
                        "val trim(input)\n" +
                        "    return input.trim()"), new Properties(), new ErrorManager(true));
                ElementStartNode root = processor.scan();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "val", TokenType.MODIFIER);
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "trim", TokenType.VALID_NAME);
                args.previous = new Element(args, "(", TokenType.SYMBOL);
                ElementStartNode startNode = new ElementStartNode(args, 4);
                args.previous = startNode;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, ")", TokenType.SYMBOL);
                ElementStartNode startNode1 = new ElementStartNode(args, 4);

                args.previous = null;
                args.previous = new Element(args, "input", TokenType.VALID_NAME);
                startNode.setLinkedNode(args.previous);

                args.previous = null;
                args.previous = new Element(args, "return", TokenType.KEY);
                startNode1.setLinkedNode(args.previous);
                args.previous = new Element(args, "input", TokenType.VALID_NAME);
                args.previous = new Element(args, ".", TokenType.SYMBOL);
                args.previous = new Element(args, "trim", TokenType.VALID_NAME);
                args.previous = new Element(args, "(", TokenType.SYMBOL);
                args.previous = new Element(args, ")", TokenType.SYMBOL);

                assertEquals(root2, root);
        }

        @Test
        public void testMethod2() throws Exception {
                // voidMethod(input)=0
                IndentScanner processor = new IndentScanner("test", new StringReader("voidMethod(input)=0"), new Properties(), new ErrorManager(true));
                ElementStartNode root = processor.scan();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "voidMethod", TokenType.VALID_NAME);
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "(", TokenType.SYMBOL);
                ElementStartNode startNode = new ElementStartNode(args, 4);
                args.previous = startNode;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, ")", TokenType.SYMBOL);
                args.previous = new Element(args, "=", TokenType.SYMBOL);
                args.previous = new Element(args, "0", TokenType.NUMBER);

                args.previous = null;
                args.previous = new Element(args, "input", TokenType.VALID_NAME);
                startNode.setLinkedNode(args.previous);

                assertEquals(root2, root);
        }

        @Test
        public void testModifiers() throws Exception {
                //pub val abs class X
                IndentScanner processor = new IndentScanner("test", new StringReader("public val abstract class X"), new Properties(), new ErrorManager(true));
                ElementStartNode root = processor.scan();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "public", TokenType.MODIFIER);
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "val", TokenType.MODIFIER);
                args.previous = new Element(args, "abstract", TokenType.MODIFIER);
                args.previous = new Element(args, "class", TokenType.KEY);
                args.previous = new Element(args, "X", TokenType.VALID_NAME);

                assertEquals(root2, root);
        }

        @Test
        public void testIf() throws Exception {
                //if true
                //    <"hello world"
                //elseif false
                //    <"hello"
                //else
                //    <"world"
                IndentScanner processor = new IndentScanner("test", new StringReader(
                        "" +
                                "if true\n" +
                                "    return \"hello world\"\n" +
                                "elseif false\n" +
                                "    return \"hello\"\n" +
                                "else\n" +
                                "    return \"world\""), new Properties(), new ErrorManager(true));
                ElementStartNode root = processor.scan();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "if", TokenType.KEY);
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "true", TokenType.BOOL);
                ElementStartNode startNode1 = new ElementStartNode(args, 4);
                args.previous = startNode1;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, "elseif", TokenType.KEY);
                args.previous = new Element(args, "false", TokenType.BOOL);
                ElementStartNode startNode2 = new ElementStartNode(args, 4);
                args.previous = startNode2;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, "else", TokenType.KEY);
                ElementStartNode startNode3 = new ElementStartNode(args, 4);

                args.previous = null;
                args.previous = new Element(args, "return", TokenType.KEY);
                startNode1.setLinkedNode(args.previous);
                args.previous = new Element(args, "\"hello world\"", TokenType.STRING);

                args.previous = null;
                args.previous = new Element(args, "return", TokenType.KEY);
                startNode2.setLinkedNode(args.previous);
                args.previous = new Element(args, "\"hello\"", TokenType.STRING);

                args.previous = null;
                args.previous = new Element(args, "return", TokenType.KEY);
                startNode3.setLinkedNode(args.previous);
                args.previous = new Element(args, "\"world\"", TokenType.STRING);

                assertEquals(root2, root);
        }

        @Test
        public void testFor1() throws Exception {
                //for i @ iterable
                //    i
                IndentScanner processor = new IndentScanner("test", new StringReader(
                        "" +
                                "for i in iterable\n" +
                                "    i"), new Properties(), new ErrorManager(true));
                ElementStartNode root = processor.scan();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "for", TokenType.KEY);
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "i", TokenType.VALID_NAME);
                args.previous = new Element(args, "in", TokenType.KEY);
                args.previous = new Element(args, "iterable", TokenType.VALID_NAME);
                ElementStartNode startNode = new ElementStartNode(args, 4);

                args.previous = null;
                startNode.setLinkedNode(new Element(args, "i", TokenType.VALID_NAME));

                assertEquals(root2, root);
        }

        @Test
        public void testWhile1() throws Exception {
                //while true
                //    i+=1
                IndentScanner processor = new IndentScanner("test", new StringReader(
                        "" +
                                "while true\n" +
                                "    i+=1"), new Properties(), new ErrorManager(true));
                ElementStartNode root = processor.scan();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "while", TokenType.KEY);
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "true", TokenType.BOOL);
                ElementStartNode startNode = new ElementStartNode(args, 4);

                args.previous = null;
                args.previous = new Element(args, "i", TokenType.VALID_NAME);
                startNode.setLinkedNode(args.previous);
                args.previous = new Element(args, "+=", TokenType.SYMBOL);
                args.previous = new Element(args, "1", TokenType.NUMBER);

                assertEquals(root2, root);
        }

        @Test
        public void testWhile2() throws Exception {
                // do
                //     i+=1
                // while true
                IndentScanner processor = new IndentScanner("test", new StringReader(
                        "" +
                                "do\n" +
                                "    i+=1\n" +
                                "while true"), new Properties(), new ErrorManager(true));
                ElementStartNode root = processor.scan();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "do", TokenType.KEY);
                root2.setLinkedNode(args.previous);
                ElementStartNode startNode = new ElementStartNode(args, 4);
                args.previous = startNode;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, "while", TokenType.KEY);
                args.previous = new Element(args, "true", TokenType.BOOL);

                args.previous = null;
                args.previous = new Element(args, "i", TokenType.VALID_NAME);
                startNode.setLinkedNode(args.previous);
                args.previous = new Element(args, "+=", TokenType.SYMBOL);
                args.previous = new Element(args, "1", TokenType.NUMBER);

                assertEquals(root2, root);
        }

        @Test
        public void testTry() throws Exception {
                //try
                //    throw e
                //catch e
                //    SomeException,AnotherException
                //        throw RuntimeException(e)
                //    OtherException
                //finally
                //    <ret
                IndentScanner processor = new IndentScanner("test", new StringReader(
                        "" +
                                "try\n" +
                                "    throw e\n" +
                                "catch e\n" +
                                "    SomeException,AnotherException\n" +
                                "        throw RuntimeException(e)\n" +
                                "    OtherException\n" +
                                "finally\n" +
                                "    return ret"), new Properties(), new ErrorManager(true));
                ElementStartNode root = processor.scan();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "try", TokenType.KEY);
                root2.setLinkedNode(args.previous);
                ElementStartNode startNode1 = new ElementStartNode(args, 4);
                args.previous = startNode1;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, "catch", TokenType.KEY);
                args.previous = new Element(args, "e", TokenType.VALID_NAME);
                ElementStartNode startNode2 = new ElementStartNode(args, 4);
                args.previous = startNode2;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, "finally", TokenType.KEY);
                ElementStartNode startNode3 = new ElementStartNode(args, 4);

                args.previous = null;
                args.previous = new Element(args, "throw", TokenType.KEY);
                startNode1.setLinkedNode(args.previous);
                args.previous = new Element(args, "e", TokenType.VALID_NAME);

                args.previous = null;
                args.previous = new Element(args, "SomeException", TokenType.VALID_NAME);
                startNode2.setLinkedNode(args.previous);
                args.previous = new EndingNode(args, EndingNode.STRONG);
                args.previous = new Element(args, "AnotherException", TokenType.VALID_NAME);
                ElementStartNode startNode1_1 = new ElementStartNode(args, 8);
                args.previous = startNode1_1;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, "OtherException", TokenType.VALID_NAME);

                args.previous = null;
                args.previous = new Element(args, "throw", TokenType.KEY);
                startNode1_1.setLinkedNode(args.previous);
                args.previous = new Element(args, "RuntimeException", TokenType.VALID_NAME);
                args.previous = new Element(args, "(", TokenType.SYMBOL);
                ElementStartNode startNode1_1_1 = new ElementStartNode(args, 12);
                args.previous = startNode1_1_1;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, ")", TokenType.SYMBOL);

                args.previous = null;
                args.previous = new Element(args, "e", TokenType.VALID_NAME);
                startNode1_1_1.setLinkedNode(args.previous);

                args.previous = null;
                args.previous = new Element(args, "return", TokenType.KEY);
                startNode3.setLinkedNode(args.previous);
                args.previous = new Element(args, "ret", TokenType.VALID_NAME);

                assertEquals(root2, root);
        }

        @Test
        public void testLambda() throws Exception {
                //list.stream().filter(
                //    (e)->
                //        e>10
                //)
                IndentScanner processor = new IndentScanner("test", new StringReader("list.stream().filter(\n    (e)->e>10)"), new Properties(), new ErrorManager(true));
                ElementStartNode root = processor.scan();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "list", TokenType.VALID_NAME);
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, ".", TokenType.SYMBOL);
                args.previous = new Element(args, "stream", TokenType.VALID_NAME);
                args.previous = new Element(args, "(", TokenType.SYMBOL);
                args.previous = new Element(args, ")", TokenType.SYMBOL);
                args.previous = new Element(args, ".", TokenType.SYMBOL);
                args.previous = new Element(args, "filter", TokenType.VALID_NAME);
                args.previous = new Element(args, "(", TokenType.SYMBOL);
                ElementStartNode startNode1 = new ElementStartNode(args, 4);
                args.previous = startNode1;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, ")", TokenType.SYMBOL);

                args.previous = null;
                args.previous = new Element(args, "(", TokenType.SYMBOL);
                startNode1.setLinkedNode(args.previous);
                ElementStartNode startNode2 = new ElementStartNode(args, 8);
                args.previous = startNode2;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, ")", TokenType.SYMBOL);
                args.previous = new Element(args, "->", TokenType.SYMBOL);
                ElementStartNode startNode3 = new ElementStartNode(args, 8);

                args.previous = null;
                args.previous = new Element(args, "e", TokenType.VALID_NAME);
                startNode2.setLinkedNode(args.previous);

                args.previous = null;
                args.previous = new Element(args, "e", TokenType.VALID_NAME);
                startNode3.setLinkedNode(args.previous);
                args.previous = new Element(args, ">", TokenType.SYMBOL);
                args.previous = new Element(args, "10", TokenType.NUMBER);

                assertEquals(root2, root);
        }

        @Test
        public void testOperators() throws Exception {
                IndentScanner processor = new IndentScanner("test", new StringReader(
                        "" +
                                "1+2\n" +
                                "3-4\n" +
                                "5*6\n" +
                                "7/8\n" +
                                "9%10\n" +
                                "i+=11\n" +
                                "i-=12\n" +
                                "i*=13\n" +
                                "i/=14\n" +
                                "i%=15\n" +
                                "i++\n" +
                                "i--\n" +
                                "++i\n" +
                                "--i"), new Properties(), new ErrorManager(true));
                ElementStartNode root = processor.scan();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);

                args.previous = new Element(args, "1", TokenType.NUMBER);
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "+", TokenType.SYMBOL);
                args.previous = new Element(args, "2", TokenType.NUMBER);

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "3", TokenType.NUMBER);
                args.previous = new Element(args, "-", TokenType.SYMBOL);
                args.previous = new Element(args, "4", TokenType.NUMBER);

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "5", TokenType.NUMBER);
                args.previous = new Element(args, "*", TokenType.SYMBOL);
                args.previous = new Element(args, "6", TokenType.NUMBER);

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "7", TokenType.NUMBER);
                args.previous = new Element(args, "/", TokenType.SYMBOL);
                args.previous = new Element(args, "8", TokenType.NUMBER);

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "9", TokenType.NUMBER);
                args.previous = new Element(args, "%", TokenType.SYMBOL);
                args.previous = new Element(args, "10", TokenType.NUMBER);

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "i", TokenType.VALID_NAME);
                args.previous = new Element(args, "+=", TokenType.SYMBOL);
                args.previous = new Element(args, "11", TokenType.NUMBER);

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "i", TokenType.VALID_NAME);
                args.previous = new Element(args, "-=", TokenType.SYMBOL);
                args.previous = new Element(args, "12", TokenType.NUMBER);

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "i", TokenType.VALID_NAME);
                args.previous = new Element(args, "*=", TokenType.SYMBOL);
                args.previous = new Element(args, "13", TokenType.NUMBER);

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "i", TokenType.VALID_NAME);
                args.previous = new Element(args, "/=", TokenType.SYMBOL);
                args.previous = new Element(args, "14", TokenType.NUMBER);

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "i", TokenType.VALID_NAME);
                args.previous = new Element(args, "%=", TokenType.SYMBOL);
                args.previous = new Element(args, "15", TokenType.NUMBER);

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "i", TokenType.VALID_NAME);
                args.previous = new Element(args, "++", TokenType.SYMBOL);

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "i", TokenType.VALID_NAME);
                args.previous = new Element(args, "--", TokenType.SYMBOL);

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "++", TokenType.SYMBOL);
                args.previous = new Element(args, "i", TokenType.VALID_NAME);

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "--", TokenType.SYMBOL);
                args.previous = new Element(args, "i", TokenType.VALID_NAME);

                assertEquals(root2, root);
        }

        @Test
        public void testSpacesAtTheFront() throws Exception {
                IndentScanner processor = new IndentScanner("test", new StringReader(
                        "  import package::name::_"
                ), new Properties(), new ErrorManager(true));
                ElementStartNode root = processor.scan();
                Node n = root.getLinkedNode();
                assertTrue(n instanceof Element);
                Element e = (Element) n;
                assertEquals(1, e.getLineCol().line);
                assertEquals(3, e.getLineCol().column);
        }

        @Test
        public void testIndent() throws Exception {
                Properties properties = new Properties();
                properties._INDENTATION_ = 2;
                IndentScanner processor = new IndentScanner("test", new StringReader(
                        // the statements is copied from testIf
                        // but changed indentation to 2
                        "" +
                                "if true\n" +
                                "  return \"hello world\"\n" +
                                "elseif false\n" +
                                "  return \"hello\"\n" +
                                "else\n" +
                                "  return \"world\""), properties, new ErrorManager(true));
                processor.scan();
        }

        @Test
        public void testMultipleLineComment() throws Exception {
                IndentScanner processor = new IndentScanner("test", new StringReader(
                        "" +
                                "a=1/**/\n" + // inline comment 1
                                "a/**/=2\n" + // inline comment 2
                                "a/* a */=3\n" + // inline comment 3
                                "a=4/* a */\n" + // inline comment 4
                                "a/*\n" +
                                "*/=5\n" +// multiple line 1
                                "/*\n" +
                                "*/a=6\n" + // multiple line 2
                                "/*\n" +
                                "a\n" +
                                "*/a=7" + // multiple line 3
                                ""),
                        new Properties(), new ErrorManager(true));
                ElementStartNode root = processor.scan();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                Element e = new Element(args, "a", TokenType.VALID_NAME);
                root2.setLinkedNode(e);
                args.previous = e;

                args.previous = new Element(args, "=", TokenType.SYMBOL);
                args.previous = new Element(args, "1", TokenType.NUMBER);
                for (int i = 2; i <= 7; ++i) {
                        args.previous = new EndingNode(args, EndingNode.WEAK);
                        args.previous = new Element(args, "a", TokenType.VALID_NAME);
                        args.previous = new Element(args, "=", TokenType.SYMBOL);
                        args.previous = new Element(args, "" + i, TokenType.NUMBER);
                }

                assertEquals(root2, root);
        }
}
