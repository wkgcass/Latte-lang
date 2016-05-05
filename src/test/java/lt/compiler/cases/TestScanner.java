package lt.compiler.cases;

import lt.compiler.Scanner;
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
                // # lt.test
                Scanner processor = new Scanner("test", new StringReader("# lt::test"), new Scanner.Properties());
                ElementStartNode root = processor.parse();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "#", TokenType.SYMBOL);
                root2.setLinkedNode(args.previous);
                ElementStartNode startNode = new ElementStartNode(args, 4);
                args.previous = startNode;
                args.startNodeStack.push(startNode);
                args.previous = null;
                args.previous = new Element(args, "lt", TokenType.VALID_NAME);
                args.previous = new Element(args, "::", TokenType.SYMBOL);
                args.previous = new Element(args, "test", TokenType.VALID_NAME);

                assertEquals(root2, root);
        }

        @Test
        public void testUse() throws Exception {
                // #> packageName._
                Scanner processor = new Scanner("test", new StringReader("" +
                        "#> Package::name::*\n" +
                        "    Package::name::Test"), new Scanner.Properties());
                ElementStartNode root = processor.parse();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "#>", TokenType.SYMBOL);
                root2.setLinkedNode(args.previous);
                ElementStartNode startNode = new ElementStartNode(args, 4);
                args.previous = null;
                args.previous = new Element(args, "Package", TokenType.VALID_NAME);
                startNode.setLinkedNode(args.previous);
                args.previous = new Element(args, "::", TokenType.SYMBOL);
                args.previous = new Element(args, "name", TokenType.VALID_NAME);
                args.previous = new Element(args, "::", TokenType.SYMBOL);
                args.previous = new Element(args, "*", TokenType.SYMBOL);
                args.previous = new EndingNode(args, EndingNode.WEAK);
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
                Scanner processor = new Scanner("test", new StringReader("class ClassName"), new Scanner.Properties());
                ElementStartNode root = processor.parse();

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
                Scanner processor = new Scanner("test", new StringReader("class ClassName(arg1,arg2=value2)"), new Scanner.Properties());
                ElementStartNode root = processor.parse();

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
                Scanner processor = new Scanner("test", new StringReader("val value = 1"), new Scanner.Properties());
                ElementStartNode root = processor.parse();

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
                Scanner processor = new Scanner("test", new StringReader("" +
                        "val trim(input)\n" +
                        "    <input.trim()"), new Scanner.Properties());
                ElementStartNode root = processor.parse();

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
                args.previous = new Element(args, "<", TokenType.SYMBOL);
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
                Scanner processor = new Scanner("test", new StringReader("voidMethod(input)=0"), new Scanner.Properties());
                ElementStartNode root = processor.parse();

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
                Scanner processor = new Scanner("test", new StringReader("pub val abs class X"), new Scanner.Properties());
                ElementStartNode root = processor.parse();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "pub", TokenType.MODIFIER);
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "val", TokenType.MODIFIER);
                args.previous = new Element(args, "abs", TokenType.MODIFIER);
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
                Scanner processor = new Scanner("test", new StringReader(
                        "" +
                                "if true\n" +
                                "    <\"hello world\"\n" +
                                "elseif false\n" +
                                "    <\"hello\"\n" +
                                "else\n" +
                                "    <\"world\""), new Scanner.Properties());
                ElementStartNode root = processor.parse();

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
                args.previous = new Element(args, "<", TokenType.SYMBOL);
                startNode1.setLinkedNode(args.previous);
                args.previous = new Element(args, "\"hello world\"", TokenType.STRING);

                args.previous = null;
                args.previous = new Element(args, "<", TokenType.SYMBOL);
                startNode2.setLinkedNode(args.previous);
                args.previous = new Element(args, "\"hello\"", TokenType.STRING);

                args.previous = null;
                args.previous = new Element(args, "<", TokenType.SYMBOL);
                startNode3.setLinkedNode(args.previous);
                args.previous = new Element(args, "\"world\"", TokenType.STRING);

                assertEquals(root2, root);
        }

        @Test
        public void testFor1() throws Exception {
                //for i @ iterable
                //    i
                Scanner processor = new Scanner("test", new StringReader(
                        "" +
                                "for i in iterable\n" +
                                "    i"), new Scanner.Properties());
                ElementStartNode root = processor.parse();

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
                Scanner processor = new Scanner("test", new StringReader(
                        "" +
                                "while true\n" +
                                "    i+=1"), new Scanner.Properties());
                ElementStartNode root = processor.parse();

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
                Scanner processor = new Scanner("test", new StringReader(
                        "" +
                                "do\n" +
                                "    i+=1\n" +
                                "while true"), new Scanner.Properties());
                ElementStartNode root = processor.parse();

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
                Scanner processor = new Scanner("test", new StringReader(
                        "" +
                                "try\n" +
                                "    throw e\n" +
                                "catch e\n" +
                                "    SomeException,AnotherException\n" +
                                "        throw RuntimeException(e)\n" +
                                "    OtherException\n" +
                                "finally\n" +
                                "    <ret"), new Scanner.Properties());
                ElementStartNode root = processor.parse();

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
                args.previous = new Element(args, "<", TokenType.SYMBOL);
                startNode3.setLinkedNode(args.previous);
                args.previous = new Element(args, "ret", TokenType.VALID_NAME);

                assertEquals(root2, root);
        }

        @Test
        public void testLambda() throws Exception {
                //list.stream().filter(
                //    (e)=>
                //        e>10
                //)
                Scanner processor = new Scanner("test", new StringReader("list.stream().filter(\n    (e)->e>10)"), new Scanner.Properties());
                ElementStartNode root = processor.parse();

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

        /*
        @Test
        public void testAnonymousClass() throws Exception {
                //list.stream().filter(
                //    Predicate()
                //        @Override
                //        test(x) : bool
                //            < x > 10
                //)
                Scanner processor = new Scanner(new StringReader(
                        "" +
                                "list.stream().filter(\n" +
                                "    Predicate()\n" +
                                "        test(x)\n" +
                                "            < x > 10\n" +
                                ")"), 4);
                ElementStartNode root = processor.parse();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "list");
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, ".");
                args.previous = new Element(args, "stream");
                args.previous = new Element(args, "(");
                args.previous = new Element(args, ")");
                args.previous = new Element(args, ".");
                args.previous = new Element(args, "filter");
                args.previous = new Element(args, "(");
                ElementStartNode startNode1 = new ElementStartNode(args, 4);
                args.previous = startNode1;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, ")");

                args.previous = null;
                args.previous = new Element(args, "Predicate");
                startNode1.setLinkedNode(args.previous);
                args.previous = new Element(args, "(");
                args.previous = new Element(args, ")");
                ElementStartNode startNode2 = new ElementStartNode(args, 8);

                args.previous = null;
                args.previous = new Element(args, "test");
                startNode2.setLinkedNode(args.previous);
                args.previous = new Element(args, "(");
                ElementStartNode startNode3 = new ElementStartNode(args, 12);
                args.previous = startNode3;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, ")");
                ElementStartNode startNode4 = new ElementStartNode(args, 12);

                args.previous = null;
                args.previous = new Element(args, "x");
                startNode3.setLinkedNode(args.previous);

                args.previous = null;
                args.previous = new Element(args, "<");
                startNode4.setLinkedNode(args.previous);
                args.previous = new Element(args, "x");
                args.previous = new Element(args, ">");
                args.previous = new Element(args, "10");

                assertEquals(root2, root);
        }
        */

        @Test
        public void testOperators() throws Exception {
                Scanner processor = new Scanner("test", new StringReader(
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
                                "--i\n" +
                                "x=:=y\n" +
                                "x!:=y"), new Scanner.Properties());
                ElementStartNode root = processor.parse();

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

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "x", TokenType.VALID_NAME);
                args.previous = new Element(args, "=:=", TokenType.SYMBOL);
                args.previous = new Element(args, "y", TokenType.VALID_NAME);

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "x", TokenType.VALID_NAME);
                args.previous = new Element(args, "!:=", TokenType.SYMBOL);
                args.previous = new Element(args, "y", TokenType.VALID_NAME);

                assertEquals(root2, root);
        }

        @Test
        public void testSpacesAtTheFront() throws Exception {
                Scanner processor = new Scanner("test", new StringReader(
                        "  #> package::name::*"
                ), new Scanner.Properties());
                ElementStartNode root = processor.parse();
                Node n = root.getLinkedNode();
                assertTrue(n instanceof Element);
                Element e = (Element) n;
                assertEquals(1, e.getLineCol().line);
                assertEquals(3, e.getLineCol().column);
        }

        @Test
        public void testIndent() throws Exception {
                Scanner.Properties properties = new Scanner.Properties();
                properties._INDENTATION_ = 2;
                Scanner processor = new Scanner("test", new StringReader(
                        // the statements is copied from testIf
                        // but changed indentation to 2
                        "" +
                                "if true\n" +
                                "  <\"hello world\"\n" +
                                "elseif false\n" +
                                "  <\"hello\"\n" +
                                "else\n" +
                                "  <\"world\""), properties);
                processor.parse();
        }

        @Test
        public void testDefine() throws Exception {
                Scanner processor = new Scanner("test", new StringReader(
                        "" +
                                "define 'CREATE TABLE' as 'class'\n" +
                                "CREATE TABLE User"), new Scanner.Properties());
                ElementStartNode root = processor.parse();
                Node n = root.getLinkedNode();
                assertTrue(n instanceof Element);
                assertEquals("class", ((Element) n).getContent());
                n = n.next();
                assertTrue(n instanceof Element);
                assertEquals("User", ((Element) n).getContent());
                n = n.next();
                assertNull(n);
        }

        @Test
        public void testErrWhenPreProcessing1() throws Exception {
                Scanner processor = new Scanner("test", new StringReader(
                        "" +
                                "define 'CREATE TABLE' 'class'"), // there should be an `as`
                        new Scanner.Properties());
                try {
                        processor.parse();
                        fail();
                } catch (SyntaxException e) {
                        assertEquals(22, e.lineCol.column);
                }
        }

        @Test
        public void testErrWhenPreProcessing2() throws Exception {
                Scanner processor = new Scanner("test", new StringReader(
                        "" +
                                "define 'CREATE TABLE' as 'class'\n" +
                                "undef 'A'"), // A is not defined
                        new Scanner.Properties());
                try {
                        processor.parse();
                        fail();
                } catch (SyntaxException e) {
                        assertEquals("\"A\" is not defined at test(2,1)", e.getMessage());
                        assertEquals(2, e.lineCol.line);
                        assertEquals(1, e.lineCol.column);
                }
        }
}
