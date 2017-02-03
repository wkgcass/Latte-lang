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
import lt.compiler.lexical.*;
import org.junit.Test;

import java.io.StringReader;

import static org.junit.Assert.assertEquals;

/**
 * test
 */
public class TestScannerLayerControl {
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
        public void testMethod1() throws Exception {
                //val trim(input)
                //    <input.trim()
                IndentScanner processor = new IndentScanner("test", new StringReader("" +
                        "val trim(input) {return input.trim()}"), new Properties(), new ErrorManager(true));
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
        public void testIf() throws Exception {
                //if true
                //    <"hello world"
                //elseif false
                //    <"hello"
                //else
                //    <"world"
                IndentScanner processor = new IndentScanner("test", new StringReader(
                        "if true {return \"hello world\"} elseif false {return \"hello\"} else {return \"world\"}"),
                        new Properties(), new ErrorManager(true));
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
                        "for i in iterable {i}"), new Properties(), new ErrorManager(true));
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
                        "while true {i+=1}"), new Properties(), new ErrorManager(true));
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
                        "do {i+=1} while true"), new Properties(), new ErrorManager(true));
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
                // the syntax is not used
                // but the test remains
                //try
                //    throw e
                //catch e
                //    SomeException,AnotherException
                //        throw RuntimeException(e)
                //    OtherException
                //finally
                //    return ret
                IndentScanner processor = new IndentScanner("test", new StringReader(
                        "" +
                                "try " +
                                /**/"{throw e} " +
                                "catch e " +
                                /**/"{SomeException,AnotherException " +
                                /* ** */"{throw RuntimeException(e)} " +
                                /**/"OtherException } " +
                                "finally " +
                                /**/"{return ret}"), new Properties(), new ErrorManager(true));
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
}
