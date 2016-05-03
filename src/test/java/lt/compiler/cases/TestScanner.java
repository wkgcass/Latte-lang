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
                Scanner processor = new Scanner("test", new StringReader("# lt::test"), 4);
                ElementStartNode root = processor.parse();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "#");
                root2.setLinkedNode(args.previous);
                ElementStartNode startNode = new ElementStartNode(args, 4);
                args.previous = startNode;
                args.startNodeStack.push(startNode);
                args.previous = null;
                args.previous = new Element(args, "lt");
                args.previous = new Element(args, "::");
                args.previous = new Element(args, "test");

                assertEquals(root2, root);
        }

        @Test
        public void testUse() throws Exception {
                // #> packageName._
                Scanner processor = new Scanner("test", new StringReader("" +
                        "#> package::name::*\n" +
                        "    package::name::Test"), 4);
                ElementStartNode root = processor.parse();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "#>");
                root2.setLinkedNode(args.previous);
                ElementStartNode startNode = new ElementStartNode(args, 4);
                args.previous = null;
                args.previous = new Element(args, "package");
                startNode.setLinkedNode(args.previous);
                args.previous = new Element(args, "::");
                args.previous = new Element(args, "name");
                args.previous = new Element(args, "::");
                args.previous = new Element(args, "*");
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, "package");
                args.previous = new Element(args, "::");
                args.previous = new Element(args, "name");
                args.previous = new Element(args, "::");
                new Element(args, "Test");

                assertEquals(root2, root);
        }

        @Test
        public void testCls1() throws Exception {
                // class ClassName
                Scanner processor = new Scanner("test", new StringReader("class ClassName"), 4);
                ElementStartNode root = processor.parse();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "class");
                root2.setLinkedNode(args.previous);
                new Element(args, "ClassName");

                assertEquals(root2, root);
        }

        @Test
        public void testCls2() throws Exception {
                // class ClassName(arg1:Type1,arg2:Type2)
                Scanner processor = new Scanner("test", new StringReader("class ClassName(arg1,arg2=value2)"), 4);
                ElementStartNode root = processor.parse();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "class");
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "ClassName");
                args.previous = new Element(args, "(");
                ElementStartNode startNode = new ElementStartNode(args, 4);
                args.previous = startNode;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                new Element(args, ")");

                args.previous = null;
                args.previous = new Element(args, "arg1");
                startNode.setLinkedNode(args.previous);

                args.previous = new EndingNode(args, EndingNode.STRONG);

                args.previous = new Element(args, "arg2");
                args.previous = new Element(args, "=");
                args.previous = new Element(args, "value2");

                assertEquals(root2, root);
        }

        @Test
        public void testVariable3() throws Exception {
                // val value:Type = 1
                Scanner processor = new Scanner("test", new StringReader("val value = 1"), 4);
                ElementStartNode root = processor.parse();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "val");
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "value");
                args.previous = new Element(args, "=");
                new Element(args, "1");

                assertEquals(root2, root);
        }

        @Test
        public void testMethod1() throws Exception {
                //val trim(input)
                //    <input.trim()
                Scanner processor = new Scanner("test", new StringReader("" +
                        "val trim(input)\n" +
                        "    <input.trim()"), 4);
                ElementStartNode root = processor.parse();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "val");
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "trim");
                args.previous = new Element(args, "(");
                ElementStartNode startNode = new ElementStartNode(args, 4);
                args.previous = startNode;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, ")");
                ElementStartNode startNode1 = new ElementStartNode(args, 4);

                args.previous = null;
                args.previous = new Element(args, "input");
                startNode.setLinkedNode(args.previous);

                args.previous = null;
                args.previous = new Element(args, "<");
                startNode1.setLinkedNode(args.previous);
                args.previous = new Element(args, "input");
                args.previous = new Element(args, ".");
                args.previous = new Element(args, "trim");
                args.previous = new Element(args, "(");
                args.previous = new Element(args, ")");

                assertEquals(root2, root);
        }

        @Test
        public void testMethod2() throws Exception {
                // voidMethod(input)=0
                Scanner processor = new Scanner("test", new StringReader("voidMethod(input)=0"), 4);
                ElementStartNode root = processor.parse();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "voidMethod");
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "(");
                ElementStartNode startNode = new ElementStartNode(args, 4);
                args.previous = startNode;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, ")");
                args.previous = new Element(args, "=");
                args.previous = new Element(args, "0");

                args.previous = null;
                args.previous = new Element(args, "input");
                startNode.setLinkedNode(args.previous);

                assertEquals(root2, root);
        }

        @Test
        public void testModifiers() throws Exception {
                //pub val abs class X
                Scanner processor = new Scanner("test", new StringReader("pub val abs class X"), 4);
                ElementStartNode root = processor.parse();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "pub");
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "val");
                args.previous = new Element(args, "abs");
                args.previous = new Element(args, "class");
                args.previous = new Element(args, "X");

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
                                "    <\"world\""), 4);
                ElementStartNode root = processor.parse();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "if");
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "true");
                ElementStartNode startNode1 = new ElementStartNode(args, 4);
                args.previous = startNode1;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, "elseif");
                args.previous = new Element(args, "false");
                ElementStartNode startNode2 = new ElementStartNode(args, 4);
                args.previous = startNode2;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, "else");
                ElementStartNode startNode3 = new ElementStartNode(args, 4);

                args.previous = null;
                args.previous = new Element(args, "<");
                startNode1.setLinkedNode(args.previous);
                args.previous = new Element(args, "\"hello world\"");

                args.previous = null;
                args.previous = new Element(args, "<");
                startNode2.setLinkedNode(args.previous);
                args.previous = new Element(args, "\"hello\"");

                args.previous = null;
                args.previous = new Element(args, "<");
                startNode3.setLinkedNode(args.previous);
                args.previous = new Element(args, "\"world\"");

                assertEquals(root2, root);
        }

        @Test
        public void testFor1() throws Exception {
                //for i @ iterable
                //    i
                Scanner processor = new Scanner("test", new StringReader(
                        "" +
                                "for i @ iterable\n" +
                                "    i"), 4);
                ElementStartNode root = processor.parse();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "for");
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "i");
                args.previous = new Element(args, "@");
                args.previous = new Element(args, "iterable");
                ElementStartNode startNode = new ElementStartNode(args, 4);

                args.previous = null;
                startNode.setLinkedNode(new Element(args, "i"));

                assertEquals(root2, root);
        }

        @Test
        public void testWhile1() throws Exception {
                //while true
                //    i+=1
                Scanner processor = new Scanner("test", new StringReader(
                        "" +
                                "while true\n" +
                                "    i+=1"), 4);
                ElementStartNode root = processor.parse();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "while");
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "true");
                ElementStartNode startNode = new ElementStartNode(args, 4);

                args.previous = null;
                args.previous = new Element(args, "i");
                startNode.setLinkedNode(args.previous);
                args.previous = new Element(args, "+=");
                args.previous = new Element(args, "1");

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
                                "while true"), 4);
                ElementStartNode root = processor.parse();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "do");
                root2.setLinkedNode(args.previous);
                ElementStartNode startNode = new ElementStartNode(args, 4);
                args.previous = startNode;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, "while");
                args.previous = new Element(args, "true");

                args.previous = null;
                args.previous = new Element(args, "i");
                startNode.setLinkedNode(args.previous);
                args.previous = new Element(args, "+=");
                args.previous = new Element(args, "1");

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
                                "    <ret"), 4);
                ElementStartNode root = processor.parse();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);
                args.previous = new Element(args, "try");
                root2.setLinkedNode(args.previous);
                ElementStartNode startNode1 = new ElementStartNode(args, 4);
                args.previous = startNode1;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, "catch");
                args.previous = new Element(args, "e");
                ElementStartNode startNode2 = new ElementStartNode(args, 4);
                args.previous = startNode2;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, "finally");
                ElementStartNode startNode3 = new ElementStartNode(args, 4);

                args.previous = null;
                args.previous = new Element(args, "throw");
                startNode1.setLinkedNode(args.previous);
                args.previous = new Element(args, "e");

                args.previous = null;
                args.previous = new Element(args, "SomeException");
                startNode2.setLinkedNode(args.previous);
                args.previous = new EndingNode(args, EndingNode.STRONG);
                args.previous = new Element(args, "AnotherException");
                ElementStartNode startNode1_1 = new ElementStartNode(args, 8);
                args.previous = startNode1_1;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, "OtherException");

                args.previous = null;
                args.previous = new Element(args, "throw");
                startNode1_1.setLinkedNode(args.previous);
                args.previous = new Element(args, "RuntimeException");
                args.previous = new Element(args, "(");
                ElementStartNode startNode1_1_1 = new ElementStartNode(args, 12);
                args.previous = startNode1_1_1;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, ")");

                args.previous = null;
                args.previous = new Element(args, "e");
                startNode1_1_1.setLinkedNode(args.previous);

                args.previous = null;
                args.previous = new Element(args, "<");
                startNode3.setLinkedNode(args.previous);
                args.previous = new Element(args, "ret");

                assertEquals(root2, root);
        }

        @Test
        public void testLambda() throws Exception {
                //list.stream().filter(
                //    (e)=>
                //        e>10
                //)
                Scanner processor = new Scanner("test", new StringReader("list.stream().filter(\n    (e)->e>10)"), 4);
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
                args.previous = new Element(args, "(");
                startNode1.setLinkedNode(args.previous);
                ElementStartNode startNode2 = new ElementStartNode(args, 8);
                args.previous = startNode2;
                args.previous = new EndingNode(args, EndingNode.WEAK);
                args.previous = new Element(args, ")");
                args.previous = new Element(args, "->");
                ElementStartNode startNode3 = new ElementStartNode(args, 8);

                args.previous = null;
                args.previous = new Element(args, "e");
                startNode2.setLinkedNode(args.previous);

                args.previous = null;
                args.previous = new Element(args, "e");
                startNode3.setLinkedNode(args.previous);
                args.previous = new Element(args, ">");
                args.previous = new Element(args, "10");

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
                                "x!:=y"), 4);
                ElementStartNode root = processor.parse();

                Args args = new Args();
                ElementStartNode root2 = new ElementStartNode(args, 0);

                args.previous = new Element(args, "1");
                root2.setLinkedNode(args.previous);
                args.previous = new Element(args, "+");
                args.previous = new Element(args, "2");

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "3");
                args.previous = new Element(args, "-");
                args.previous = new Element(args, "4");

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "5");
                args.previous = new Element(args, "*");
                args.previous = new Element(args, "6");

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "7");
                args.previous = new Element(args, "/");
                args.previous = new Element(args, "8");

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "9");
                args.previous = new Element(args, "%");
                args.previous = new Element(args, "10");

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "i");
                args.previous = new Element(args, "+=");
                args.previous = new Element(args, "11");

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "i");
                args.previous = new Element(args, "-=");
                args.previous = new Element(args, "12");

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "i");
                args.previous = new Element(args, "*=");
                args.previous = new Element(args, "13");

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "i");
                args.previous = new Element(args, "/=");
                args.previous = new Element(args, "14");

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "i");
                args.previous = new Element(args, "%=");
                args.previous = new Element(args, "15");

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "i");
                args.previous = new Element(args, "++");

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "i");
                args.previous = new Element(args, "--");

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "++");
                args.previous = new Element(args, "i");

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "--");
                args.previous = new Element(args, "i");

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "x");
                args.previous = new Element(args, "=:=");
                args.previous = new Element(args, "y");

                args.previous = new EndingNode(args, EndingNode.WEAK);

                args.previous = new Element(args, "x");
                args.previous = new Element(args, "!:=");
                args.previous = new Element(args, "y");

                assertEquals(root2, root);
        }

        @Test
        public void testSpacesAtTheFront() throws Exception {
                Scanner processor = new Scanner("test", new StringReader(
                        "  #> package::name::*"
                ), 4);
                ElementStartNode root = processor.parse();
                Node n = root.getLinkedNode();
                assertTrue(n instanceof Element);
                Element e = (Element) n;
                assertEquals(1, e.getLineCol().line);
                assertEquals(3, e.getLineCol().column);
        }

        @Test
        public void testIndent() throws Exception {
                Scanner processor = new Scanner("test", new StringReader(
                        // the statements is copied from testIf
                        // but changed indentation to 2
                        "" +
                                "if true\n" +
                                "  <\"hello world\"\n" +
                                "elseif false\n" +
                                "  <\"hello\"\n" +
                                "else\n" +
                                "  <\"world\""), 2);
                processor.parse();
        }

        @Test
        public void testDefine() throws Exception {
                Scanner processor = new Scanner("test", new StringReader(
                        "" +
                                "define 'CREATE TABLE' as 'class'\n" +
                                "CREATE TABLE User"), 4);
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
                        4);
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
                        4);
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
