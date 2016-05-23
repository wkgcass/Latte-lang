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

import lt.compiler.*;
import lt.compiler.Scanner;
import lt.compiler.lexical.ElementStartNode;
import lt.compiler.syntactic.*;
import lt.compiler.syntactic.def.ClassDef;
import lt.compiler.syntactic.def.InterfaceDef;
import lt.compiler.syntactic.def.MethodDef;
import lt.compiler.syntactic.def.VariableDef;
import lt.compiler.syntactic.literal.BoolLiteral;
import lt.compiler.syntactic.literal.NumberLiteral;
import lt.compiler.syntactic.literal.StringLiteral;
import lt.compiler.syntactic.operation.OneVariableOperation;
import lt.compiler.syntactic.operation.TwoVariableOperation;
import lt.compiler.syntactic.operation.UnaryOneVariableOperation;
import lt.compiler.syntactic.pre.Import;
import lt.compiler.syntactic.pre.Modifier;
import lt.compiler.syntactic.pre.PackageDeclare;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

import static org.junit.Assert.*;

/**
 * test SyntacticProcessor
 */
public class TestParser {
        private static List<Statement> parse(String stmt) throws IOException, SyntaxException {
                lt.compiler.Scanner processor = new lt.compiler.Scanner("test", new StringReader(stmt), new Scanner.Properties(), new ErrorManager(true));
                ElementStartNode root = processor.scan();

                Parser syntacticProcessor = new Parser(root, new ErrorManager(true));

                return syntacticProcessor.parse();
        }

        @Test
        public void testOperatorInSamePriority() throws Exception {
                List<Statement> statements = parse("1+2-3+4");

                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                NumberLiteral one = new NumberLiteral("1", LineCol.SYNTHETIC);
                NumberLiteral two = new NumberLiteral("2", LineCol.SYNTHETIC);
                TwoVariableOperation tvo1 = new TwoVariableOperation("+", one, two, LineCol.SYNTHETIC);

                NumberLiteral three = new NumberLiteral("3", LineCol.SYNTHETIC);
                TwoVariableOperation tvo2 = new TwoVariableOperation("-", tvo1, three, LineCol.SYNTHETIC);

                NumberLiteral four = new NumberLiteral("4", LineCol.SYNTHETIC);
                TwoVariableOperation tvo3 = new TwoVariableOperation("+", tvo2, four, LineCol.SYNTHETIC);

                assertEquals(tvo3, s);
        }

        @Test
        public void testOperatorInDifferentPriorities() throws Exception {
                List<Statement> statements = parse("1+2*3+4");

                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                NumberLiteral one = new NumberLiteral("1", LineCol.SYNTHETIC);
                NumberLiteral two = new NumberLiteral("2", LineCol.SYNTHETIC);
                NumberLiteral three = new NumberLiteral("3", LineCol.SYNTHETIC);
                NumberLiteral four = new NumberLiteral("4", LineCol.SYNTHETIC);

                TwoVariableOperation tvo1 = new TwoVariableOperation("*", two, three, LineCol.SYNTHETIC);


                TwoVariableOperation tvo2 = new TwoVariableOperation("+", one, tvo1, LineCol.SYNTHETIC);


                TwoVariableOperation tvo3 = new TwoVariableOperation("+", tvo2, four, LineCol.SYNTHETIC);

                assertEquals(tvo3, s);
        }

        @Test
        public void test1Plus2() throws Exception {
                List<Statement> statements = parse("1+2");

                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                NumberLiteral one = new NumberLiteral("1", LineCol.SYNTHETIC);
                NumberLiteral two = new NumberLiteral("2", LineCol.SYNTHETIC);
                TwoVariableOperation tvo1 = new TwoVariableOperation("+", one, two, LineCol.SYNTHETIC);

                assertEquals(tvo1, s);
        }

        @Test
        public void test1Plus2Multi3() throws Exception {
                List<Statement> statements = parse("1+2*3");

                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                NumberLiteral one = new NumberLiteral("1", LineCol.SYNTHETIC);
                NumberLiteral two = new NumberLiteral("2", LineCol.SYNTHETIC);
                NumberLiteral three = new NumberLiteral("3", LineCol.SYNTHETIC);
                TwoVariableOperation tvo1 = new TwoVariableOperation("*", two, three, LineCol.SYNTHETIC);
                TwoVariableOperation tvo2 = new TwoVariableOperation("+", one, tvo1, LineCol.SYNTHETIC);

                assertEquals(tvo2, s);
        }

        @Test
        public void test1Plus2Multi3Div4() throws Exception {
                List<Statement> statements = parse("1+2*3/4");

                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                NumberLiteral one = new NumberLiteral("1", LineCol.SYNTHETIC);
                NumberLiteral two = new NumberLiteral("2", LineCol.SYNTHETIC);
                NumberLiteral three = new NumberLiteral("3", LineCol.SYNTHETIC);
                NumberLiteral four = new NumberLiteral("4", LineCol.SYNTHETIC);
                TwoVariableOperation tvo1 = new TwoVariableOperation("*", two, three, LineCol.SYNTHETIC);
                TwoVariableOperation tvo2 = new TwoVariableOperation("/", tvo1, four, LineCol.SYNTHETIC);
                TwoVariableOperation tvo3 = new TwoVariableOperation("+", one, tvo2, LineCol.SYNTHETIC);

                assertEquals(tvo3, s);
        }

        @Test
        public void test1Plus2Multi3Div4Minus5() throws Exception {
                List<Statement> statements = parse("1+2*3/4-5");

                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                NumberLiteral one = new NumberLiteral("1", LineCol.SYNTHETIC);
                NumberLiteral two = new NumberLiteral("2", LineCol.SYNTHETIC);
                NumberLiteral three = new NumberLiteral("3", LineCol.SYNTHETIC);
                NumberLiteral four = new NumberLiteral("4", LineCol.SYNTHETIC);
                NumberLiteral five = new NumberLiteral("5", LineCol.SYNTHETIC);
                TwoVariableOperation tvo1 = new TwoVariableOperation("*", two, three, LineCol.SYNTHETIC);
                TwoVariableOperation tvo2 = new TwoVariableOperation("/", tvo1, four, LineCol.SYNTHETIC);
                TwoVariableOperation tvo3 = new TwoVariableOperation("+", one, tvo2, LineCol.SYNTHETIC);
                TwoVariableOperation tvo4 = new TwoVariableOperation("-", tvo3, five, LineCol.SYNTHETIC);

                assertEquals(tvo4, s);
        }

        @Test
        public void testPar1Plus2ParMulti3() throws Exception {
                List<Statement> statements = parse("(1+2)*3");

                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                NumberLiteral one = new NumberLiteral("1", LineCol.SYNTHETIC);
                NumberLiteral two = new NumberLiteral("2", LineCol.SYNTHETIC);
                NumberLiteral three = new NumberLiteral("3", LineCol.SYNTHETIC);
                TwoVariableOperation tvo1 = new TwoVariableOperation("+", one, two, LineCol.SYNTHETIC);
                TwoVariableOperation tvo2 = new TwoVariableOperation("*", tvo1, three, LineCol.SYNTHETIC);

                assertEquals(tvo2, s);
        }

        @Test
        public void test2VarOperatorFinal() throws Exception {
                List<Statement> statements = parse("1*3/(4+5)*6-(7/8+9)-10-15");

                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                NumberLiteral one = new NumberLiteral("1", LineCol.SYNTHETIC);
                NumberLiteral three = new NumberLiteral("3", LineCol.SYNTHETIC);
                NumberLiteral four = new NumberLiteral("4", LineCol.SYNTHETIC);
                NumberLiteral five = new NumberLiteral("5", LineCol.SYNTHETIC);
                NumberLiteral six = new NumberLiteral("6", LineCol.SYNTHETIC);
                NumberLiteral seven = new NumberLiteral("7", LineCol.SYNTHETIC);
                NumberLiteral eight = new NumberLiteral("8", LineCol.SYNTHETIC);
                NumberLiteral nine = new NumberLiteral("9", LineCol.SYNTHETIC);
                NumberLiteral ten = new NumberLiteral("10", LineCol.SYNTHETIC);
                NumberLiteral fifteen = new NumberLiteral("15", LineCol.SYNTHETIC);

                TwoVariableOperation oneMULthree = new TwoVariableOperation("*", one, three, LineCol.SYNTHETIC);
                TwoVariableOperation fourPLUSfive = new TwoVariableOperation("+", four, five, LineCol.SYNTHETIC);
                TwoVariableOperation DIV1 = new TwoVariableOperation("/", oneMULthree, fourPLUSfive, LineCol.SYNTHETIC);
                TwoVariableOperation MUL1 = new TwoVariableOperation("*", DIV1, six, LineCol.SYNTHETIC);
                TwoVariableOperation sevenDIVIDEeight = new TwoVariableOperation("/", seven, eight, LineCol.SYNTHETIC);
                TwoVariableOperation DIVPLUSnine = new TwoVariableOperation("+", sevenDIVIDEeight, nine, LineCol.SYNTHETIC);
                TwoVariableOperation MINUS1 = new TwoVariableOperation("-", MUL1, DIVPLUSnine, LineCol.SYNTHETIC);
                TwoVariableOperation MINUS10 = new TwoVariableOperation("-", MINUS1, ten, LineCol.SYNTHETIC);
                TwoVariableOperation MINUS15 = new TwoVariableOperation("-", MINUS10, fifteen, LineCol.SYNTHETIC);

                assertEquals(MINUS15, s);
        }

        @Test
        public void testOperators() throws Exception {
                List<Statement> statements = parse("+1++ -3^!true+2+\"abc\"");

                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                NumberLiteral one = new NumberLiteral("1", LineCol.SYNTHETIC);
                OneVariableOperation postPlusPlus = new OneVariableOperation("++", one, LineCol.SYNTHETIC);
                UnaryOneVariableOperation plusOne = new UnaryOneVariableOperation("+", postPlusPlus, LineCol.SYNTHETIC);
                NumberLiteral three = new NumberLiteral("3", LineCol.SYNTHETIC);
                TwoVariableOperation minus = new TwoVariableOperation("-", plusOne, three, LineCol.SYNTHETIC);

                BoolLiteral tr = new BoolLiteral("true", LineCol.SYNTHETIC);
                UnaryOneVariableOperation not = new UnaryOneVariableOperation("!", tr, LineCol.SYNTHETIC);

                NumberLiteral two = new NumberLiteral("2", LineCol.SYNTHETIC);
                TwoVariableOperation plusTwo = new TwoVariableOperation("+", not, two, LineCol.SYNTHETIC);
                StringLiteral abc = new StringLiteral("\"abc\"", LineCol.SYNTHETIC);
                TwoVariableOperation plusABC = new TwoVariableOperation("+", plusTwo, abc, LineCol.SYNTHETIC);

                TwoVariableOperation xor = new TwoVariableOperation("^", minus, plusABC, LineCol.SYNTHETIC);

                assertEquals(xor, s);
        }

        @Test
        public void testPost1VarWithOperatorPriority() throws Exception {
                List<Statement> statements = parse("1+1++ *1");
                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                NumberLiteral n1 = new NumberLiteral("1", LineCol.SYNTHETIC);
                NumberLiteral n2 = new NumberLiteral("1", LineCol.SYNTHETIC);
                NumberLiteral n3 = new NumberLiteral("1", LineCol.SYNTHETIC);

                OneVariableOperation ovo = new OneVariableOperation("++", n2, LineCol.SYNTHETIC);
                TwoVariableOperation tvo1 = new TwoVariableOperation("*", ovo, n3, LineCol.SYNTHETIC);
                TwoVariableOperation tvo2 = new TwoVariableOperation("+", n1, tvo1, LineCol.SYNTHETIC);

                assertEquals(tvo2, s);
        }

        @Test
        public void testPackage() throws Exception {
                List<Statement> statements = parse("java::lang::Integer");
                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                AST.PackageRef pkg = new AST.PackageRef("java::lang", LineCol.SYNTHETIC);
                AST.Access a = new AST.Access(pkg, "Integer", LineCol.SYNTHETIC);

                assertEquals(a, s);
        }

        @Test
        public void testPkgAccess() throws Exception {
                List<Statement> statements = parse("java::lang::String.cls");
                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                AST.PackageRef pkg = new AST.PackageRef("java::lang", LineCol.SYNTHETIC);
                AST.Access access = new AST.Access(pkg, "String", LineCol.SYNTHETIC);
                AST.Access access2 = new AST.Access(access, "cls", LineCol.SYNTHETIC);

                assertEquals(access2, s);
        }

        @Test
        public void testInvocation() throws Exception {
                List<Statement> statements = parse("java::lang::String.valueOf(true)");
                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                AST.PackageRef pkg = new AST.PackageRef("java::lang", LineCol.SYNTHETIC);
                AST.Access access = new AST.Access(pkg, "String", LineCol.SYNTHETIC);
                AST.Access access2 = new AST.Access(access, "valueOf", LineCol.SYNTHETIC);

                AST.Invocation invocation = new AST.Invocation(access2,
                        Collections.singletonList(
                                new BoolLiteral("true", LineCol.SYNTHETIC)),
                        false, LineCol.SYNTHETIC);

                assertEquals(invocation, s);
        }

        @Test
        public void testInvocationNoArg() throws Exception {
                List<Statement> statements = parse("method()");
                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                AST.Invocation invocation = new AST.Invocation(new AST.Access(null, "method", LineCol.SYNTHETIC), Collections.emptyList(), false, LineCol.SYNTHETIC);

                assertEquals(invocation, s);
        }

        @Test
        public void testVariableWithInitValue() throws Exception {
                List<Statement> statements = parse("i=1");
                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                VariableDef v = new VariableDef("i", new HashSet<>(), Collections.emptySet(), LineCol.SYNTHETIC);
                NumberLiteral n = new NumberLiteral("1", LineCol.SYNTHETIC);
                v.setInit(n);

                assertEquals(v, s);
        }

        @Test
        public void testVariableWithInitType_FullName() throws Exception {
                List<Statement> statements = parse("i:java::lang::Integer");
                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                VariableDef v = new VariableDef("i", new HashSet<>(), Collections.emptySet(), LineCol.SYNTHETIC);
                AST.PackageRef pkg = new AST.PackageRef("java::lang", LineCol.SYNTHETIC);
                AST.Access access = new AST.Access(pkg, "Integer", LineCol.SYNTHETIC);
                v.setType(access);

                assertEquals(v, s);
        }

        @Test
        public void testVariableWithInitType_SimpleName() throws Exception {
                List<Statement> statements = parse("i:Integer");
                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                VariableDef v = new VariableDef("i", new HashSet<>(), Collections.emptySet(), LineCol.SYNTHETIC);
                AST.Access access = new AST.Access(null, "Integer", LineCol.SYNTHETIC);
                v.setType(access);

                assertEquals(v, s);
        }

        @Test
        public void testVariableWithInitType_FullName_Inner() throws Exception {
                List<Statement> statements = parse("i:myPackage::ClassName.Inner");
                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                VariableDef v = new VariableDef("i", new HashSet<>(), Collections.emptySet(), LineCol.SYNTHETIC);
                AST.PackageRef pkg = new AST.PackageRef("myPackage", LineCol.SYNTHETIC);
                AST.Access access1 = new AST.Access(pkg, "ClassName", LineCol.SYNTHETIC);
                AST.Access access2 = new AST.Access(access1, "Inner", LineCol.SYNTHETIC);
                v.setType(access2);

                assertEquals(v, s);
        }

        @Test
        public void testVariableWithInitType_SimpleName_Inner() throws Exception {
                List<Statement> statements = parse("i:ClassName.Inner");
                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                VariableDef v = new VariableDef("i", new HashSet<>(), Collections.emptySet(), LineCol.SYNTHETIC);
                AST.Access access1 = new AST.Access(null, "ClassName", LineCol.SYNTHETIC);
                AST.Access access2 = new AST.Access(access1, "Inner", LineCol.SYNTHETIC);
                v.setType(access2);

                assertEquals(v, s);
        }

        @Test
        public void testVariableWithInitType_FullName_Init() throws Exception {
                List<Statement> statements = parse("i:java::lang::Integer=1");
                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                VariableDef v = new VariableDef("i", new HashSet<>(), Collections.emptySet(), LineCol.SYNTHETIC);
                AST.PackageRef pkg = new AST.PackageRef("java::lang", LineCol.SYNTHETIC);
                AST.Access access = new AST.Access(pkg, "Integer", LineCol.SYNTHETIC);
                v.setType(access);

                NumberLiteral n = new NumberLiteral("1", LineCol.SYNTHETIC);
                v.setInit(n);

                assertEquals(v, s);
        }

        @Test
        public void testVariableWithInitType_SimpleName_Init() throws Exception {
                List<Statement> statements = parse("i:Integer=1");
                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                VariableDef v = new VariableDef("i", new HashSet<>(), Collections.emptySet(), LineCol.SYNTHETIC);
                AST.Access access = new AST.Access(null, "Integer", LineCol.SYNTHETIC);
                v.setType(access);

                NumberLiteral n = new NumberLiteral("1", LineCol.SYNTHETIC);
                v.setInit(n);

                assertEquals(v, s);
        }

        @Test
        public void testVariableWithInitType_FullName_Inner_Init() throws Exception {
                List<Statement> statements = parse("i:myPackage::ClassName.Inner=1");
                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                VariableDef v = new VariableDef("i", new HashSet<>(), Collections.emptySet(), LineCol.SYNTHETIC);
                AST.PackageRef pkg = new AST.PackageRef("myPackage", LineCol.SYNTHETIC);
                AST.Access access1 = new AST.Access(pkg, "ClassName", LineCol.SYNTHETIC);
                AST.Access access2 = new AST.Access(access1, "Inner", LineCol.SYNTHETIC);
                v.setType(access2);

                NumberLiteral n = new NumberLiteral("1", LineCol.SYNTHETIC);
                v.setInit(n);

                assertEquals(v, s);
        }

        @Test
        public void testVariableWithInitType_SimpleName_Inner_Init() throws Exception {
                List<Statement> statements = parse("i:ClassName.Inner=1");
                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                VariableDef v = new VariableDef("i", new HashSet<>(), Collections.emptySet(), LineCol.SYNTHETIC);
                AST.Access access1 = new AST.Access(null, "ClassName", LineCol.SYNTHETIC);
                AST.Access access2 = new AST.Access(access1, "Inner", LineCol.SYNTHETIC);
                v.setType(access2);

                NumberLiteral n = new NumberLiteral("1", LineCol.SYNTHETIC);
                v.setInit(n);

                assertEquals(v, s);
        }

        @Test
        public void testVariableWithInitType_SimpleName_Inner_Init_Operator() throws Exception {
                List<Statement> statements = parse("i:ClassName.Inner=1+2");
                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                VariableDef v = new VariableDef("i", new HashSet<>(), Collections.emptySet(), LineCol.SYNTHETIC);
                AST.Access access1 = new AST.Access(null, "ClassName", LineCol.SYNTHETIC);
                AST.Access access2 = new AST.Access(access1, "Inner", LineCol.SYNTHETIC);
                v.setType(access2);

                NumberLiteral n1 = new NumberLiteral("1", LineCol.SYNTHETIC);
                NumberLiteral n2 = new NumberLiteral("2", LineCol.SYNTHETIC);
                TwoVariableOperation o = new TwoVariableOperation("+", n1, n2, LineCol.SYNTHETIC);
                v.setInit(o);

                assertEquals(v, s);
        }

        @Test
        public void testModifier() throws Exception {
                List<Statement> statements = parse("val i:ClassName.Inner=1+2");
                assertEquals(1, statements.size());

                Statement s = statements.get(0);

                VariableDef v = new VariableDef("i", new HashSet<>(Collections.singletonList(new Modifier(Modifier.Available.VAL, LineCol.SYNTHETIC))), Collections.emptySet(), LineCol.SYNTHETIC);
                AST.Access access1 = new AST.Access(null, "ClassName", LineCol.SYNTHETIC);
                AST.Access access2 = new AST.Access(access1, "Inner", LineCol.SYNTHETIC);
                v.setType(access2);

                NumberLiteral n1 = new NumberLiteral("1", LineCol.SYNTHETIC);
                NumberLiteral n2 = new NumberLiteral("2", LineCol.SYNTHETIC);
                TwoVariableOperation o = new TwoVariableOperation("+", n1, n2, LineCol.SYNTHETIC);
                v.setInit(o);

                assertEquals(v, s);
        }

        @Test
        public void testAssign() throws Exception {
                List<Statement> statements = parse("i=1\ni=2");
                assertEquals(2, statements.size());

                Statement s = statements.get(1);

                AST.Access access = new AST.Access(null, "i", LineCol.SYNTHETIC);
                NumberLiteral n = new NumberLiteral("2", LineCol.SYNTHETIC);
                AST.Assignment ass = new AST.Assignment(access, "=", n, LineCol.SYNTHETIC);

                assertEquals(ass, s);
        }

        @Test
        public void testMethodNormal_NoParam() throws Exception {
                List<Statement> list = parse(
                        "" +
                                "method()\n" +
                                "    a=false"
                );

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                VariableDef v = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(new BoolLiteral("false", LineCol.SYNTHETIC));

                MethodDef methodDef = new MethodDef("method",
                        Collections.emptySet(),
                        null,
                        Collections.emptyList(),
                        Collections.emptySet(),
                        Collections.singletonList(v), LineCol.SYNTHETIC);

                assertEquals(methodDef, stmt);
        }

        @Test
        public void testMethodType_NoParam() throws Exception {
                List<Statement> list = parse(
                        "" +
                                "method():Integer\n" +
                                "    a=false"
                );

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                VariableDef v = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(new BoolLiteral("false", LineCol.SYNTHETIC));

                MethodDef methodDef = new MethodDef("method",
                        Collections.emptySet(),
                        new AST.Access(null, "Integer", LineCol.SYNTHETIC),
                        Collections.emptyList(),
                        Collections.emptySet(),
                        Collections.singletonList(v), LineCol.SYNTHETIC);

                assertEquals(methodDef, stmt);
        }

        @Test
        public void testMethodType_NoParam_NoStmt() throws Exception {
                List<Statement> list = parse("method():Integer");

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                MethodDef methodDef = new MethodDef("method",
                        Collections.emptySet(),
                        new AST.Access(null, "Integer", LineCol.SYNTHETIC),
                        Collections.emptyList(),
                        Collections.emptySet(),
                        Collections.emptyList(), LineCol.SYNTHETIC);

                assertEquals(methodDef, stmt);
        }

        @Test
        public void testMethodEmpty_NoParam() throws Exception {
                List<Statement> list = parse("method()=...");

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                MethodDef methodDef = new MethodDef("method",
                        Collections.emptySet(),
                        null,
                        Collections.emptyList(),
                        Collections.emptySet(),
                        Collections.emptyList(), LineCol.SYNTHETIC);

                assertEquals(methodDef, stmt);
        }

        @Test
        public void testMethodOne_Stmt_NoParam() throws Exception {
                List<Statement> list = parse("method()=123");

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                MethodDef methodDef = new MethodDef("method",
                        Collections.emptySet(),
                        null,
                        Collections.emptyList(),
                        Collections.emptySet(),
                        Collections.singletonList(
                                new AST.Return(new NumberLiteral("123", LineCol.SYNTHETIC), LineCol.SYNTHETIC)
                        ), LineCol.SYNTHETIC);

                assertEquals(methodDef, stmt);
        }

        @Test
        public void testMethodGeneral() throws Exception {
                List<Statement> list = parse(
                        "" +
                                "abstract method(a,b:Character):Integer\n" +
                                "    a=false"
                );

                assertEquals(1, list.size());
                Statement stmt = list.get(0);
                VariableDef a = new VariableDef("a", new HashSet<>(Collections.emptySet()), Collections.emptySet(), LineCol.SYNTHETIC);
                VariableDef b = new VariableDef("b", new HashSet<>(), Collections.emptySet(), LineCol.SYNTHETIC);
                b.setType(new AST.Access(null, "Character", LineCol.SYNTHETIC));
                List<VariableDef> vars = Arrays.asList(a, b);

                AST.Assignment assignment = new AST.Assignment(new AST.Access(null, "a", LineCol.SYNTHETIC), "=", new BoolLiteral("false", LineCol.SYNTHETIC), LineCol.SYNTHETIC);

                MethodDef methodDef = new MethodDef(
                        "method",
                        new HashSet<>(Collections.singletonList(new Modifier(Modifier.Available.ABSTRACT, LineCol.SYNTHETIC))),
                        new AST.Access(null, "Integer", LineCol.SYNTHETIC),
                        vars,
                        Collections.emptySet(),
                        Collections.singletonList(assignment),
                        LineCol.SYNTHETIC);

                assertEquals(methodDef, stmt);
        }

        @Test
        public void testReturn() throws Exception {
                List<Statement> list = parse("return i+1");

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                AST.Access access = new AST.Access(null, "i", LineCol.SYNTHETIC);
                NumberLiteral one = new NumberLiteral("1", LineCol.SYNTHETIC);
                TwoVariableOperation tvo = new TwoVariableOperation("+", access, one, LineCol.SYNTHETIC);
                AST.Return r = new AST.Return(tvo, LineCol.SYNTHETIC);

                assertEquals(r, stmt);
        }

        @Test
        public void testReturnVoid() throws Exception {
                List<Statement> list = parse("return ");
                assertEquals(1, list.size());

                assertNull(((AST.Return) list.get(0)).exp);
        }

        @Test
        public void testJavaName() throws Exception {
                try {
                        parse("val=1");
                        fail();
                } catch (Exception ignore) {
                }
                List<Statement> list = parse("`val`=1");

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                VariableDef v = new VariableDef("val", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));

                assertEquals(v, stmt);
        }

        @Test
        public void testIf() throws Exception {
                List<Statement> list = parse("if true");

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                AST.If iff = new AST.If(Collections.singletonList(new AST.If.IfPair(new BoolLiteral("true", LineCol.SYNTHETIC), Collections.emptyList(), LineCol.SYNTHETIC)), LineCol.SYNTHETIC);
                assertEquals(iff, stmt);
        }

        @Test
        public void testIf_Body() throws Exception {
                List<Statement> list = parse("" +
                        "if true\n" +
                        "    a=1");

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                VariableDef v = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));
                AST.If iff = new AST.If(Collections.singletonList(new AST.If.IfPair(new BoolLiteral("true", LineCol.SYNTHETIC), Collections.singletonList(v), LineCol.SYNTHETIC)), LineCol.SYNTHETIC);
                assertEquals(iff, stmt);
        }

        @Test
        public void testIf_Body_Elseif() throws Exception {
                List<Statement> list = parse("" +
                        "if true\n" +
                        "    a=1\n" +
                        "elseif boolval");

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                VariableDef v = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));
                AST.If iff = new AST.If(Arrays.asList(
                        new AST.If.IfPair(new BoolLiteral("true", LineCol.SYNTHETIC), Collections.singletonList(v), LineCol.SYNTHETIC),
                        new AST.If.IfPair(new AST.Access(null, "boolval", LineCol.SYNTHETIC), Collections.emptyList(), LineCol.SYNTHETIC)
                ), LineCol.SYNTHETIC);
                assertEquals(iff, stmt);
        }

        @Test
        public void testIf_Body_Elseif_Body() throws Exception {
                List<Statement> list = parse("" +
                        "if true\n" +
                        "    a=1\n" +
                        "elseif boolval\n" +
                        "    a=2");

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                VariableDef v1 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v1.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));

                VariableDef v2 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v2.setInit(new NumberLiteral("2", LineCol.SYNTHETIC));
                AST.If iff = new AST.If(Arrays.asList(
                        new AST.If.IfPair(new BoolLiteral("true", LineCol.SYNTHETIC), Collections.singletonList(v1), LineCol.SYNTHETIC),
                        new AST.If.IfPair(new AST.Access(null, "boolval", LineCol.SYNTHETIC), Collections.singletonList(v2), LineCol.SYNTHETIC)
                ), LineCol.SYNTHETIC);
                assertEquals(iff, stmt);
        }

        @Test
        public void testIf_Body_Elseif_Body_Else() throws Exception {
                List<Statement> list = parse("" +
                        "if true\n" +
                        "    a=1\n" +
                        "elseif boolval\n" +
                        "    a=2\n" +
                        "else\n");

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                VariableDef v1 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v1.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));

                VariableDef v2 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v2.setInit(new NumberLiteral("2", LineCol.SYNTHETIC));
                AST.If iff = new AST.If(Arrays.asList(
                        new AST.If.IfPair(new BoolLiteral("true", LineCol.SYNTHETIC), Collections.singletonList(v1), LineCol.SYNTHETIC),
                        new AST.If.IfPair(new AST.Access(null, "boolval", LineCol.SYNTHETIC), Collections.singletonList(v2), LineCol.SYNTHETIC),
                        new AST.If.IfPair(null, Collections.emptyList(), LineCol.SYNTHETIC)
                ), LineCol.SYNTHETIC);
                assertEquals(iff, stmt);
        }

        @Test
        public void testIf_Body_Elseif_Body_Else_Body() throws Exception {
                List<Statement> list = parse("" +
                        "if true\n" +
                        "    a=1\n" +
                        "elseif boolval\n" +
                        "    a=2\n" +
                        "else\n" +
                        "    a=3");

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                VariableDef v1 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v1.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));

                VariableDef v2 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v2.setInit(new NumberLiteral("2", LineCol.SYNTHETIC));

                VariableDef v3 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v3.setInit(new NumberLiteral("3", LineCol.SYNTHETIC));
                AST.If iff = new AST.If(Arrays.asList(
                        new AST.If.IfPair(new BoolLiteral("true", LineCol.SYNTHETIC), Collections.singletonList(v1), LineCol.SYNTHETIC),
                        new AST.If.IfPair(new AST.Access(null, "boolval", LineCol.SYNTHETIC), Collections.singletonList(v2), LineCol.SYNTHETIC),
                        new AST.If.IfPair(null, Collections.singletonList(v3), LineCol.SYNTHETIC)
                ), LineCol.SYNTHETIC);
                assertEquals(iff, stmt);
        }

        @Test
        public void testIf_Body_2_Elseif_Body_Else_Body__1() throws Exception {
                List<Statement> list = parse("" +
                        "if true\n" +
                        "    a=1\n" +
                        "elseif bool1\n" +
                        "    a=2\n" +
                        "elseif bool2\n" +
                        "else\n" +
                        "    a=3");

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                VariableDef v1 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v1.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));

                VariableDef v2 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v2.setInit(new NumberLiteral("2", LineCol.SYNTHETIC));

                VariableDef v4 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v4.setInit(new NumberLiteral("3", LineCol.SYNTHETIC));
                AST.If iff = new AST.If(Arrays.asList(
                        new AST.If.IfPair(new BoolLiteral("true", LineCol.SYNTHETIC), Collections.singletonList(v1), LineCol.SYNTHETIC),
                        new AST.If.IfPair(new AST.Access(null, "bool1", LineCol.SYNTHETIC), Collections.singletonList(v2), LineCol.SYNTHETIC),
                        new AST.If.IfPair(new AST.Access(null, "bool2", LineCol.SYNTHETIC), Collections.emptyList(), LineCol.SYNTHETIC),
                        new AST.If.IfPair(null, Collections.singletonList(v4), LineCol.SYNTHETIC)
                ), LineCol.SYNTHETIC);
                assertEquals(iff, stmt);
        }

        @Test
        public void testIf_Body_2_Elseif_Else_Body() throws Exception {
                List<Statement> list = parse("" +
                        "if true\n" +
                        "    a=1\n" +
                        "elseif bool1\n" +
                        "elseif bool2\n" +
                        "else\n" +
                        "    a=3");

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                VariableDef v1 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v1.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));

                VariableDef v4 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v4.setInit(new NumberLiteral("3", LineCol.SYNTHETIC));
                AST.If iff = new AST.If(Arrays.asList(
                        new AST.If.IfPair(new BoolLiteral("true", LineCol.SYNTHETIC), Collections.singletonList(v1), LineCol.SYNTHETIC),
                        new AST.If.IfPair(new AST.Access(null, "bool1", LineCol.SYNTHETIC), Collections.emptyList(), LineCol.SYNTHETIC),
                        new AST.If.IfPair(new AST.Access(null, "bool2", LineCol.SYNTHETIC), Collections.emptyList(), LineCol.SYNTHETIC),
                        new AST.If.IfPair(null, Collections.singletonList(v4), LineCol.SYNTHETIC)
                ), LineCol.SYNTHETIC);
                assertEquals(iff, stmt);
        }

        @Test
        public void testIf_Body_Elseif_Else_Body() throws Exception {
                List<Statement> list = parse("" +
                        "if true\n" +
                        "    a=1\n" +
                        "elseif bool2\n" +
                        "else\n" +
                        "    a=3");

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                VariableDef v1 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v1.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));

                VariableDef v4 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v4.setInit(new NumberLiteral("3", LineCol.SYNTHETIC));
                AST.If iff = new AST.If(Arrays.asList(
                        new AST.If.IfPair(new BoolLiteral("true", LineCol.SYNTHETIC), Collections.singletonList(v1), LineCol.SYNTHETIC),
                        new AST.If.IfPair(new AST.Access(null, "bool2", LineCol.SYNTHETIC), Collections.emptyList(), LineCol.SYNTHETIC),
                        new AST.If.IfPair(null, Collections.singletonList(v4), LineCol.SYNTHETIC)
                ), LineCol.SYNTHETIC);
                assertEquals(iff, stmt);
        }

        @Test
        public void testFor1() throws Exception {
                List<Statement> list = parse("for i in ite");

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                AST.For f = new AST.For("i", new AST.Access(null, "ite", LineCol.SYNTHETIC), Collections.emptyList(), LineCol.SYNTHETIC);
                assertEquals(f, stmt);
        }

        @Test
        public void testFor2() throws Exception {
                List<Statement> list = parse("for i in ite\n    a=1");

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                VariableDef v = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));
                AST.For f = new AST.For("i", new AST.Access(null, "ite", LineCol.SYNTHETIC), Collections.singletonList(v), LineCol.SYNTHETIC);
                assertEquals(f, stmt);
        }

        @Test
        public void testLambda_NO_PARAM() throws Exception {
                List<Statement> list = parse("()->1+1");

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                AST.Lambda l = new AST.Lambda(
                        Collections.emptyList(),
                        Collections.singletonList(
                                new AST.Return(
                                        new TwoVariableOperation("+",
                                                new NumberLiteral("1", LineCol.SYNTHETIC),
                                                new NumberLiteral("1", LineCol.SYNTHETIC), LineCol.SYNTHETIC), LineCol.SYNTHETIC)),
                        LineCol.SYNTHETIC);

                assertEquals(l, stmt);
        }

        @Test
        public void testLambda_2_PARAM() throws Exception {
                List<Statement> list = parse("(a,b)->a+b");

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                AST.Lambda l = new AST.Lambda(
                        Arrays.asList(
                                new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC),
                                new VariableDef("b", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC)
                        ),
                        Collections.singletonList(
                                new AST.Return(
                                        new TwoVariableOperation("+",
                                                new AST.Access(null, "a", LineCol.SYNTHETIC),
                                                new AST.Access(null, "b", LineCol.SYNTHETIC), LineCol.SYNTHETIC), LineCol.SYNTHETIC)),
                        LineCol.SYNTHETIC);

                assertEquals(l, stmt);
        }

        @Test
        public void testLambda_in_method_invocation() throws Exception {
                List<Statement> list = parse(
                        "" +
                                "method(\n" +
                                "    (a)->a+1\n" +
                                "    1" +
                                ")"
                );

                assertEquals(1, list.size());
                Statement stmt = list.get(0);

                AST.Invocation invocation = new AST.Invocation(new AST.Access(null, "method", LineCol.SYNTHETIC), Arrays.asList(
                        new AST.Lambda(Collections.singletonList(new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC)), Collections.singletonList(
                                new AST.Return(
                                        new TwoVariableOperation(
                                                "+",
                                                new AST.Access(null, "a", LineCol.SYNTHETIC),
                                                new NumberLiteral("1", LineCol.SYNTHETIC),
                                                LineCol.SYNTHETIC), LineCol.SYNTHETIC)), LineCol.SYNTHETIC),
                        new NumberLiteral("1", LineCol.SYNTHETIC)
                ), false, LineCol.SYNTHETIC);

                assertEquals(invocation, stmt);
        }

        @Test
        public void testStatic1() throws Exception {
                List<Statement> list = parse("static a=1");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                VariableDef v = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));
                AST.StaticScope st = new AST.StaticScope(Collections.singletonList(v), LineCol.SYNTHETIC);

                assertEquals(st, stmt);
        }

        @Test
        public void testStatic2() throws Exception {
                List<Statement> list = parse("" +
                        "static\n" +
                        "    a=1\n" +
                        "    b=2");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                VariableDef v1 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v1.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));
                VariableDef v2 = new VariableDef("b", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v2.setInit(new NumberLiteral("2", LineCol.SYNTHETIC));

                AST.StaticScope st = new AST.StaticScope(Arrays.asList(v1, v2), LineCol.SYNTHETIC);

                assertEquals(st, stmt);
        }

        @Test
        public void testClass_NonArg() throws Exception {
                List<Statement> list = parse("class C:Type(),Type2\n    a=2");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                VariableDef v = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(new NumberLiteral("2", LineCol.SYNTHETIC));
                ClassDef def = new ClassDef("C", Collections.emptySet(), Collections.emptyList(), new AST.Invocation(new AST.Access(null, "Type", LineCol.SYNTHETIC), Collections.emptyList(), false, LineCol.SYNTHETIC), Collections.singletonList(new AST.Access(null, "Type2", LineCol.SYNTHETIC)), Collections.emptySet(), Collections.singletonList(v), LineCol.SYNTHETIC);

                assertEquals(def, stmt);
        }

        @Test
        public void testClass_Arg() throws Exception {
                List<Statement> list = parse("class C(arg):Type(),Type2\n    a=2");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                VariableDef v = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(new NumberLiteral("2", LineCol.SYNTHETIC));
                ClassDef def = new ClassDef("C", Collections.emptySet(), Collections.singletonList(
                        new VariableDef("arg", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC)
                ), new AST.Invocation(new AST.Access(null, "Type", LineCol.SYNTHETIC), Collections.emptyList(), false, LineCol.SYNTHETIC), Collections.singletonList(new AST.Access(null, "Type2", LineCol.SYNTHETIC)), Collections.emptySet(), Collections.singletonList(v), LineCol.SYNTHETIC);

                assertEquals(def, stmt);
        }

        @Test
        public void testClass_Arg_SuperInvocation() throws Exception {
                List<Statement> list = parse("class C(arg):Type(arg),Type2\n    a=2");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                VariableDef v = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(new NumberLiteral("2", LineCol.SYNTHETIC));
                ClassDef def = new ClassDef("C", Collections.emptySet(), Collections.singletonList(
                        new VariableDef("arg", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC)
                ), new AST.Invocation(new AST.Access(null, "Type", LineCol.SYNTHETIC), Collections.singletonList(new AST.Access(null, "arg", LineCol.SYNTHETIC)), false, LineCol.SYNTHETIC), Collections.singletonList(new AST.Access(null, "Type2", LineCol.SYNTHETIC)), Collections.emptySet(), Collections.singletonList(v), LineCol.SYNTHETIC);

                assertEquals(def, stmt);
        }

        @Test
        public void testClass_Arg_SuperInvocation_Modifiers() throws Exception {
                List<Statement> list = parse("abstract class C(arg):Type(arg),Type2\n    a=2");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                VariableDef v = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(new NumberLiteral("2", LineCol.SYNTHETIC));
                ClassDef def = new ClassDef("C", new HashSet<>(Collections.singletonList(
                        new Modifier(Modifier.Available.ABSTRACT, LineCol.SYNTHETIC)
                )), Collections.singletonList(
                        new VariableDef("arg", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC)
                ), new AST.Invocation(new AST.Access(null, "Type", LineCol.SYNTHETIC), Collections.singletonList(new AST.Access(null, "arg", LineCol.SYNTHETIC)), false, LineCol.SYNTHETIC), Collections.singletonList(new AST.Access(null, "Type2", LineCol.SYNTHETIC)), Collections.emptySet(), Collections.singletonList(v), LineCol.SYNTHETIC);

                assertEquals(def, stmt);
        }

        @Test
        public void testInterface_simple() throws Exception {
                List<Statement> list = parse("interface A");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                InterfaceDef def = new InterfaceDef("A", Collections.emptySet(), Collections.emptyList(), Collections.emptySet(), Collections.emptyList(), LineCol.SYNTHETIC);
                assertEquals(def, stmt);
        }

        @Test
        public void testInterface_super_interfaces() throws Exception {
                List<Statement> list = parse("interface A:B,C");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                InterfaceDef def = new InterfaceDef("A", Collections.emptySet(), Arrays.asList(
                        new AST.Access(null, "B", LineCol.SYNTHETIC),
                        new AST.Access(null, "C", LineCol.SYNTHETIC)
                ), Collections.emptySet(), Collections.emptyList(), LineCol.SYNTHETIC);
                assertEquals(def, stmt);
        }

        @Test
        public void testInterface_super_interfaces_stmt() throws Exception {
                List<Statement> list = parse("interface A:B,C\n    method()=...");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                InterfaceDef def = new InterfaceDef("A", Collections.emptySet(), Arrays.asList(
                        new AST.Access(null, "B", LineCol.SYNTHETIC),
                        new AST.Access(null, "C", LineCol.SYNTHETIC)
                ), Collections.emptySet(), Collections.singletonList(
                        new MethodDef("method", Collections.emptySet(), null, Collections.emptyList(), Collections.emptySet(),
                                Collections.emptyList(), LineCol.SYNTHETIC)
                ), LineCol.SYNTHETIC);
                assertEquals(def, stmt);
        }

        @Test
        public void testInterface_super_interfaces_stmt_modifiers() throws Exception {
                List<Statement> list = parse("protected interface A:B,C\n    method()=...");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                InterfaceDef def = new InterfaceDef("A", new HashSet<>(Collections.singletonList(
                        new Modifier(Modifier.Available.PROTECTED, LineCol.SYNTHETIC)
                )), Arrays.asList(
                        new AST.Access(null, "B", LineCol.SYNTHETIC),
                        new AST.Access(null, "C", LineCol.SYNTHETIC)
                ), Collections.emptySet(), Collections.singletonList(
                        new MethodDef("method", Collections.emptySet(), null, Collections.emptyList(), Collections.emptySet(),
                                Collections.emptyList(), LineCol.SYNTHETIC)
                ), LineCol.SYNTHETIC);
                assertEquals(def, stmt);
        }

        @Test
        public void testTryAll() throws Exception {
                List<Statement> list = parse(
                        "" +
                                "try\n" +
                                "    a=1\n" +
                                "catch e\n" +
                                "    if e is type Exception or e is type Throwable\n" +
                                "        a=2\n" +
                                "    elseif e is type RuntimeException\n" +
                                "        a=3\n" +
                                "finally\n" +
                                "    a=4");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                VariableDef v1 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v1.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));

                VariableDef v2 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v2.setInit(new NumberLiteral("2", LineCol.SYNTHETIC));

                VariableDef v3 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v3.setInit(new NumberLiteral("3", LineCol.SYNTHETIC));

                VariableDef v4 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v4.setInit(new NumberLiteral("4", LineCol.SYNTHETIC));

                AST.Try t = new AST.Try(Collections.singletonList(v1), "e", Collections.singletonList(
                        new AST.If(
                                Arrays.asList(
                                        new AST.If.IfPair(
                                                new TwoVariableOperation("or",
                                                        new TwoVariableOperation("is",
                                                                new AST.Access(
                                                                        null, "e", LineCol.SYNTHETIC),
                                                                new AST.TypeOf(
                                                                        new AST.Access(null, "Exception", LineCol.SYNTHETIC),
                                                                        LineCol.SYNTHETIC
                                                                ),
                                                                LineCol.SYNTHETIC),
                                                        new TwoVariableOperation("is",
                                                                new AST.Access(
                                                                        null, "e", LineCol.SYNTHETIC),
                                                                new AST.TypeOf(
                                                                        new AST.Access(null, "Throwable", LineCol.SYNTHETIC),
                                                                        LineCol.SYNTHETIC
                                                                ),
                                                                LineCol.SYNTHETIC),
                                                        LineCol.SYNTHETIC
                                                ),
                                                Collections.singletonList(v2),
                                                LineCol.SYNTHETIC
                                        ),
                                        new AST.If.IfPair(
                                                new TwoVariableOperation("is",
                                                        new AST.Access(
                                                                null, "e", LineCol.SYNTHETIC),
                                                        new AST.TypeOf(
                                                                new AST.Access(null, "RuntimeException", LineCol.SYNTHETIC),
                                                                LineCol.SYNTHETIC
                                                        ),
                                                        LineCol.SYNTHETIC),
                                                Collections.singletonList(v3),
                                                LineCol.SYNTHETIC
                                        )
                                ),
                                LineCol.SYNTHETIC
                        )
                ), Collections.singletonList(v4), LineCol.SYNTHETIC);

                assertEquals(t, stmt);
        }

        @Test
        public void testTryOneCatchNoProcess() throws Exception {
                List<Statement> list = parse(
                        "" +
                                "try\n" +
                                "    a=1\n" +
                                "catch e");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                VariableDef v1 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v1.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));

                AST.Try t = new AST.Try(Collections.singletonList(v1), "e", Collections.emptyList(), Collections.emptyList(), LineCol.SYNTHETIC);

                assertEquals(t, stmt);
        }

        @Test
        public void testTryFinally() throws Exception {
                List<Statement> list = parse(
                        "" +
                                "try\n" +
                                "    a=1\n" +
                                "finally\n" +
                                "    a=2");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                VariableDef v1 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v1.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));

                VariableDef v2 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v2.setInit(new NumberLiteral("2", LineCol.SYNTHETIC));

                AST.Try t = new AST.Try(Collections.singletonList(v1), null, Collections.emptyList(),
                        Collections.singletonList(v2),
                        LineCol.SYNTHETIC);
                assertEquals(t, stmt);
        }

        @Test
        public void testThrow() throws Exception {
                List<Statement> list = parse("throw e");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                AST.Throw t = new AST.Throw(new AST.Access(null, "e", LineCol.SYNTHETIC), LineCol.SYNTHETIC);
                assertEquals(t, stmt);
        }

        @Test
        public void testAnnoVariable() throws Exception {
                List<Statement> list = parse("@Anno(abc=1)\ni=2");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                VariableDef v = new VariableDef(
                        "i",
                        Collections.emptySet(),
                        new HashSet<>(Collections.singletonList(
                                new AST.Anno(new AST.Access(null, "Anno", LineCol.SYNTHETIC), Collections.singletonList(
                                        new AST.Assignment(new AST.Access(null, "abc", LineCol.SYNTHETIC), "=", new NumberLiteral("1", LineCol.SYNTHETIC), LineCol.SYNTHETIC)
                                ), LineCol.SYNTHETIC)
                        )),
                        LineCol.SYNTHETIC);
                v.setInit(new NumberLiteral("2", LineCol.SYNTHETIC));
                assertEquals(v, stmt);
        }

        @Test
        public void testAnnoInterface() throws Exception {
                List<Statement> list = parse("@Anno\ninterface I");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                InterfaceDef i = new InterfaceDef("I", Collections.emptySet(), Collections.emptyList(),
                        new HashSet<>(Collections.singletonList(
                                new AST.Anno(new AST.Access(null, "Anno", LineCol.SYNTHETIC), Collections.emptyList(), LineCol.SYNTHETIC)
                        )),
                        Collections.emptyList(), LineCol.SYNTHETIC);
                assertEquals(i, stmt);
        }

        @Test
        public void testAnnoClass() throws Exception {
                List<Statement> list = parse("@Anno\nclass C");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                ClassDef c = new ClassDef(
                        "C",
                        Collections.emptySet(),
                        Collections.emptyList(),
                        null,
                        Collections.emptyList(),
                        new HashSet<>(Collections.singletonList(
                                new AST.Anno(new AST.Access(null, "Anno", LineCol.SYNTHETIC), Collections.emptyList(), LineCol.SYNTHETIC)
                        )),
                        Collections.emptyList(),
                        LineCol.SYNTHETIC);
                assertEquals(c, stmt);
        }

        @Test
        public void testAnnoMethod() throws Exception {
                List<Statement> list = parse("@Anno\nmethod()=...");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                MethodDef m = new MethodDef(
                        "method",
                        Collections.emptySet(),
                        null,
                        Collections.emptyList(),
                        new HashSet<>(Collections.singletonList(
                                new AST.Anno(new AST.Access(null, "Anno", LineCol.SYNTHETIC), Collections.emptyList(), LineCol.SYNTHETIC)
                        )),
                        Collections.emptyList(),
                        LineCol.SYNTHETIC);
                assertEquals(m, stmt);
        }

        @Test
        public void testArrayExp() throws Exception {
                List<Statement> list = parse("[1,2]");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                AST.ArrayExp arr = new AST.ArrayExp(Arrays.asList(
                        new NumberLiteral("1", LineCol.SYNTHETIC),
                        new NumberLiteral("2", LineCol.SYNTHETIC)
                ), LineCol.SYNTHETIC);
                assertEquals(arr, stmt);
        }

        @Test
        public void testArrayExp0() throws Exception {
                List<Statement> list = parse("[]");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                AST.ArrayExp arr = new AST.ArrayExp(Collections.emptyList(), LineCol.SYNTHETIC);
                assertEquals(arr, stmt);
        }

        @Test
        public void testIndexAccess1() throws Exception {
                List<Statement> list = parse("array[1]");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                AST.Index index = new AST.Index(new AST.Access(null, "array", LineCol.SYNTHETIC), Collections.singletonList(
                        new NumberLiteral("1", LineCol.SYNTHETIC)
                ), LineCol.SYNTHETIC);
                assertEquals(index, stmt);
        }

        @Test
        public void testIndexAccess2() throws Exception {
                List<Statement> list = parse("array[1,2]");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                AST.Index index = new AST.Index(new AST.Access(null, "array", LineCol.SYNTHETIC), Arrays.asList(
                        new NumberLiteral("1", LineCol.SYNTHETIC),
                        new NumberLiteral("2", LineCol.SYNTHETIC)
                ), LineCol.SYNTHETIC);
                assertEquals(index, stmt);
        }

        @Test
        public void testIndexAccess0() throws Exception {
                List<Statement> list = parse("array[]");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                AST.Index index = new AST.Index(new AST.Access(null, "array", LineCol.SYNTHETIC), Collections.emptyList(), LineCol.SYNTHETIC);
                assertEquals(index, stmt);
        }

        @Test
        public void testMap() throws Exception {
                List<Statement> list = parse("{a:b}");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                LinkedHashMap<Expression, Expression> map = new LinkedHashMap<>();
                map.put(new AST.Access(null, "a", LineCol.SYNTHETIC), new AST.Access(null, "b", LineCol.SYNTHETIC));
                AST.MapExp mapExp = new AST.MapExp(map, LineCol.SYNTHETIC);
                assertEquals(mapExp, stmt);
        }

        @Test
        public void testMap_two_key() throws Exception {
                List<Statement> list = parse("{a:b,c:d}");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                LinkedHashMap<Expression, Expression> map = new LinkedHashMap<>();
                map.put(new AST.Access(null, "a", LineCol.SYNTHETIC), new AST.Access(null, "b", LineCol.SYNTHETIC));
                map.put(new AST.Access(null, "c", LineCol.SYNTHETIC), new AST.Access(null, "d", LineCol.SYNTHETIC));
                AST.MapExp mapExp = new AST.MapExp(map, LineCol.SYNTHETIC);
                assertEquals(mapExp, stmt);
        }

        @Test
        public void testMap_in_map() throws Exception {
                List<Statement> list = parse(
                        "{a:{b:c}}"
                );

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                LinkedHashMap<Expression, Expression> map = new LinkedHashMap<>();

                LinkedHashMap<Expression, Expression> inside_map = new LinkedHashMap<>();
                inside_map.put(new AST.Access(null, "b", LineCol.SYNTHETIC), new AST.Access(null, "c", LineCol.SYNTHETIC));
                AST.MapExp inside = new AST.MapExp(inside_map, LineCol.SYNTHETIC);

                map.put(new AST.Access(null, "a", LineCol.SYNTHETIC), inside);
                AST.MapExp mapExp = new AST.MapExp(map, LineCol.SYNTHETIC);
                assertEquals(mapExp, stmt);
        }

        @Test
        public void testMap_assign() throws Exception {
                List<Statement> list = parse("map={a:b}");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                LinkedHashMap<Expression, Expression> map = new LinkedHashMap<>();
                map.put(new AST.Access(null, "a", LineCol.SYNTHETIC), new AST.Access(null, "b", LineCol.SYNTHETIC));
                AST.MapExp mapExp = new AST.MapExp(map, LineCol.SYNTHETIC);

                VariableDef v = new VariableDef("map", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(mapExp);
                assertEquals(v, stmt);
        }

        @Test
        public void testMap_in_map_assign() throws Exception {
                List<Statement> list = parse(
                        "map={a:{b:c}}"
                );

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                LinkedHashMap<Expression, Expression> map = new LinkedHashMap<>();

                LinkedHashMap<Expression, Expression> inside_map = new LinkedHashMap<>();
                inside_map.put(new AST.Access(null, "b", LineCol.SYNTHETIC), new AST.Access(null, "c", LineCol.SYNTHETIC));
                AST.MapExp inside = new AST.MapExp(inside_map, LineCol.SYNTHETIC);

                map.put(new AST.Access(null, "a", LineCol.SYNTHETIC), inside);
                AST.MapExp mapExp = new AST.MapExp(map, LineCol.SYNTHETIC);

                VariableDef v = new VariableDef("map", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(mapExp);
                assertEquals(v, stmt);
        }

        @Test
        public void testMap_in_map_pretty() throws Exception {
                List<Statement> list = parse(
                        "" +
                                "{\n" +
                                "    a:{\n" +
                                "        b:c\n" +
                                "    }\n" +
                                "}"
                );

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                LinkedHashMap<Expression, Expression> map = new LinkedHashMap<>();

                LinkedHashMap<Expression, Expression> inside_map = new LinkedHashMap<>();
                inside_map.put(new AST.Access(null, "b", LineCol.SYNTHETIC), new AST.Access(null, "c", LineCol.SYNTHETIC));
                AST.MapExp inside = new AST.MapExp(inside_map, LineCol.SYNTHETIC);

                map.put(new AST.Access(null, "a", LineCol.SYNTHETIC), inside);
                AST.MapExp mapExp = new AST.MapExp(map, LineCol.SYNTHETIC);
                assertEquals(mapExp, stmt);
        }

        @Test
        public void testPkgDeclare1() throws Exception {
                List<Statement> list = parse("package lt");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                PackageDeclare p = new PackageDeclare(new AST.PackageRef("lt", LineCol.SYNTHETIC), LineCol.SYNTHETIC);
                assertEquals(p, stmt);
        }

        @Test
        public void testPkgDeclare2() throws Exception {
                List<Statement> list = parse("package lt::lang::util");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                PackageDeclare p = new PackageDeclare(new AST.PackageRef("lt::lang::util", LineCol.SYNTHETIC), LineCol.SYNTHETIC);
                assertEquals(p, stmt);
        }

        @Test
        public void testImportPackageAll() throws Exception {
                List<Statement> list = parse("import lt::lang::_");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                Import i = new Import(new AST.PackageRef("lt::lang", LineCol.SYNTHETIC), null, true, LineCol.SYNTHETIC);
                assertEquals(i, stmt);
        }

        @Test
        public void testImportClass() throws Exception {
                List<Statement> list = parse("import lt::lang::Cls");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                Import i = new Import(null, new AST.Access(new AST.PackageRef("lt::lang", LineCol.SYNTHETIC), "Cls", LineCol.SYNTHETIC), false
                        , LineCol.SYNTHETIC);
                assertEquals(i, stmt);
        }

        @Test
        public void testImportClassAll() throws Exception {
                List<Statement> list = parse("import lt::lang::Cls._");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                Import i = new Import(null, new AST.Access(new AST.PackageRef("lt::lang", LineCol.SYNTHETIC), "Cls", LineCol.SYNTHETIC), true
                        , LineCol.SYNTHETIC);
                assertEquals(i, stmt);
        }

        @Test
        public void testImportInnerClass() throws Exception {
                List<Statement> list = parse("import lt::lang::Cls.Inner");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                Import i = new Import(null, new AST.Access(new AST.Access(new AST.PackageRef("lt::lang", LineCol.SYNTHETIC), "Cls", LineCol.SYNTHETIC), "Inner", LineCol.SYNTHETIC), false
                        , LineCol.SYNTHETIC);
                assertEquals(i, stmt);
        }

        @Test
        public void testImportInnerClassAll() throws Exception {
                List<Statement> list = parse("import lt::lang::Cls.Inner._");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                Import i = new Import(null, new AST.Access(new AST.Access(new AST.PackageRef("lt::lang", LineCol.SYNTHETIC), "Cls", LineCol.SYNTHETIC), "Inner", LineCol.SYNTHETIC), true
                        , LineCol.SYNTHETIC);
                assertEquals(i, stmt);
        }

        @Test
        public void testImportClassAllNoPKG() throws Exception {
                List<Statement> list = parse("import Cls._");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                Import i = new Import(null, new AST.Access(null, "Cls", LineCol.SYNTHETIC), true
                        , LineCol.SYNTHETIC);
                assertEquals(i, stmt);
        }

        @Test
        public void testImportInnerClassAllNoPKG() throws Exception {
                List<Statement> list = parse("import Cls.Inner._");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                Import i = new Import(null, new AST.Access(new AST.Access(null, "Cls", LineCol.SYNTHETIC), "Inner", LineCol.SYNTHETIC), true
                        , LineCol.SYNTHETIC);
                assertEquals(i, stmt);
        }

        @Test
        public void testImportInnerClassNoPKG() throws Exception {
                List<Statement> list = parse("import Cls.Inner");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                Import i = new Import(null, new AST.Access(new AST.Access(null, "Cls", LineCol.SYNTHETIC), "Inner", LineCol.SYNTHETIC), false
                        , LineCol.SYNTHETIC);
                assertEquals(i, stmt);
        }

        @Test
        public void testWhile() throws Exception {
                List<Statement> list = parse("" +
                        "while true\n" +
                        "    1");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                AST.While w = new AST.While(new BoolLiteral("true", LineCol.SYNTHETIC), Collections.singletonList(
                        new NumberLiteral("1", LineCol.SYNTHETIC)
                ), false, LineCol.SYNTHETIC);
                assertEquals(w, stmt);
        }

        @Test
        public void testDoWhile() throws Exception {
                List<Statement> list = parse("" +
                        "do\n" +
                        "    1\n" +
                        "while true");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                AST.While w = new AST.While(new BoolLiteral("true", LineCol.SYNTHETIC), Collections.singletonList(
                        new NumberLiteral("1", LineCol.SYNTHETIC)
                ), true, LineCol.SYNTHETIC);
                assertEquals(w, stmt);
        }

        @Test
        public void testAnnoArray() throws Exception {
                List<Statement> list = parse("" +
                        "@Anno(a=[1,2])\n" +
                        "i=2");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                VariableDef v = new VariableDef("i", Collections.emptySet(), new HashSet<>(Collections.singletonList(
                        new AST.Anno(new AST.Access(null, "Anno", LineCol.SYNTHETIC), Collections.singletonList(
                                new AST.Assignment(new AST.Access(null, "a", LineCol.SYNTHETIC), "=", new AST.ArrayExp(Arrays.asList(
                                        new NumberLiteral("1", LineCol.SYNTHETIC),
                                        new NumberLiteral("2", LineCol.SYNTHETIC)
                                ), LineCol.SYNTHETIC), LineCol.SYNTHETIC)
                        ), LineCol.SYNTHETIC)
                )), LineCol.SYNTHETIC);
                v.setInit(new NumberLiteral("2", LineCol.SYNTHETIC));
                assertEquals(v, stmt);
        }

        @Test
        public void testAnnoNoAssign() throws Exception {
                List<Statement> list = parse("" +
                        "@Anno([1,2])\n" +
                        "i=2");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                VariableDef v = new VariableDef("i", Collections.emptySet(), new HashSet<>(Collections.singletonList(
                        new AST.Anno(new AST.Access(null, "Anno", LineCol.SYNTHETIC), Collections.singletonList(
                                new AST.Assignment(new AST.Access(null, "value", LineCol.SYNTHETIC), "=", new AST.ArrayExp(Arrays.asList(
                                        new NumberLiteral("1", LineCol.SYNTHETIC),
                                        new NumberLiteral("2", LineCol.SYNTHETIC)
                                ), LineCol.SYNTHETIC), LineCol.SYNTHETIC)
                        ), LineCol.SYNTHETIC)
                )), LineCol.SYNTHETIC);
                v.setInit(new NumberLiteral("2", LineCol.SYNTHETIC));
                assertEquals(v, stmt);
        }

        @Test
        public void testClosure() throws Exception {
                List<Statement> list = parse("(return 1)");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                AST.Procedure procedure = new AST.Procedure(Collections.singletonList(
                        new AST.Return(new NumberLiteral("1", LineCol.SYNTHETIC), LineCol.SYNTHETIC)
                ), LineCol.SYNTHETIC);
                assertEquals(procedure, stmt);
        }

        @Test
        public void testClosureMultipleLine() throws Exception {
                List<Statement> list = parse("" +
                        "(\n" +
                        "    i=1\n" +
                        "    return i\n" +
                        ")");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                VariableDef v = new VariableDef("i", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));
                AST.Procedure procedure = new AST.Procedure(Arrays.asList(
                        v,
                        new AST.Return(new AST.Access(null, "i", LineCol.SYNTHETIC), LineCol.SYNTHETIC)
                ), LineCol.SYNTHETIC);
                assertEquals(procedure, stmt);
        }

        @Test
        public void testSync() throws Exception {
                List<Statement> list = parse("synchronized(lock)\n    i=1");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                VariableDef v = new VariableDef("i", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));

                AST.Synchronized s = new AST.Synchronized(Collections.singletonList(
                        new AST.Access(null, "lock", LineCol.SYNTHETIC)
                ), Collections.singletonList(
                        v
                ), LineCol.SYNTHETIC);
                assertEquals(s, stmt);
        }

        @Test
        public void testArrayType() throws Exception {
                List<Statement> list = parse("i:[]Type");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                VariableDef v = new VariableDef("i", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setType(new AST.Access(new AST.Access(null, "Type", LineCol.SYNTHETIC), "[]", LineCol.SYNTHETIC));
                assertEquals(v, stmt);
        }

        @Test
        public void test2ArrayType() throws Exception {
                List<Statement> list = parse("i:[][]Type");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                VariableDef v = new VariableDef("i", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setType(new AST.Access(new AST.Access(new AST.Access(null, "Type", LineCol.SYNTHETIC), "[]", LineCol.SYNTHETIC), "[]", LineCol.SYNTHETIC));
                assertEquals(v, stmt);
        }

        @Test
        public void testTypeOf() throws Exception {
                List<Statement> list = parse("type int");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                AST.TypeOf typeOf = new AST.TypeOf(new AST.Access(null, "int", LineCol.SYNTHETIC), LineCol.SYNTHETIC);
                assertEquals(typeOf, stmt);
        }

        @Test
        public void testIn() throws Exception {
                List<Statement> list = parse("1 in [1,2]");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                TwoVariableOperation tvo = new TwoVariableOperation("in",
                        new NumberLiteral("1", LineCol.SYNTHETIC),
                        new AST.ArrayExp(Arrays.asList(
                                new NumberLiteral("1", LineCol.SYNTHETIC),
                                new NumberLiteral("2", LineCol.SYNTHETIC)
                        ), LineCol.SYNTHETIC)
                        , LineCol.SYNTHETIC);
                assertEquals(tvo, stmt);
        }

        @Test
        public void testAbstractClass() throws Exception {
                List<Statement> list = parse("abstract class AbsCls(id,name)");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                ClassDef c = new ClassDef(
                        "AbsCls",
                        new HashSet<>(Collections.singletonList(new Modifier(Modifier.Available.ABSTRACT, LineCol.SYNTHETIC))),
                        Arrays.asList(
                                new VariableDef("id", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC),
                                new VariableDef("name", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC)
                        ),
                        null,
                        Collections.emptyList(),
                        Collections.emptySet(),
                        Collections.emptyList(), LineCol.SYNTHETIC);
                assertEquals(c, stmt);
        }

        @Test
        public void testPass() throws Exception {
                List<Statement> list = parse("method()\n    ...");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                MethodDef m = new MethodDef("method", Collections.emptySet(), null, Collections.emptyList(), Collections.emptySet(), Collections.singletonList(
                        new AST.Pass(LineCol.SYNTHETIC)
                ), LineCol.SYNTHETIC);
                assertEquals(m, stmt);
        }

        @Test
        public void testNull() throws Exception {
                List<Statement> list = parse("null");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                AST.Null n = new AST.Null(LineCol.SYNTHETIC);
                assertEquals(n, stmt);
        }

        @Test
        public void testAsType() throws Exception {
                List<Statement> list = parse("1 as java::lang::List");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                AST.AsType asType = new AST.AsType(new NumberLiteral("1", LineCol.SYNTHETIC), new AST.Access(new AST.PackageRef("java::lang", LineCol.SYNTHETIC), "List", LineCol.SYNTHETIC), LineCol.SYNTHETIC);
                assertEquals(asType, stmt);
        }

        @Test
        public void testUndefined() throws Exception {
                List<Statement> list = parse("undefined");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                assertEquals(new AST.UndefinedExp(LineCol.SYNTHETIC), stmt);
        }

        @Test
        public void testUnaryInc() throws Exception {
                List<Statement> list = parse("++i");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                UnaryOneVariableOperation ovo = new UnaryOneVariableOperation("++", new AST.Access(null, "i", LineCol.SYNTHETIC), LineCol.SYNTHETIC);
                assertEquals(ovo, stmt);
        }

        @Test
        public void testOperatorLikeInvocation() throws Exception {
                List<Statement> list = parse("a op b\na op");

                assertEquals(2, list.size());

                Statement stmt = list.get(0);
                AST.Invocation invocation = new AST.Invocation(new AST.Access(new AST.Access(null, "a", LineCol.SYNTHETIC), "op", LineCol.SYNTHETIC), Collections.singletonList(new AST.Access(null, "b", LineCol.SYNTHETIC)), false, LineCol.SYNTHETIC);
                assertEquals(invocation, stmt);

                stmt = list.get(1);
                invocation = new AST.Invocation(new AST.Access(new AST.Access(null, "a", LineCol.SYNTHETIC), "op", LineCol.SYNTHETIC), Collections.emptyList(), false, LineCol.SYNTHETIC);
                assertEquals(invocation, stmt);
        }

        @Test
        public void testOperatorLikeInvocation2() throws Exception {
                List<Statement> list = parse("db select a, b, c from user");
                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                AST.Invocation invocation = new AST.Invocation(
                        new AST.Access(
                                new AST.Invocation(
                                        new AST.Access(
                                                new AST.Access(null, "db", null),
                                                "select",
                                                null),
                                        Arrays.asList(
                                                new AST.Access(null, "a", null),
                                                new AST.Access(null, "b", null),
                                                new AST.Access(null, "c", null)
                                        ),
                                        false, null
                                ),
                                "from",
                                null
                        ),
                        Collections.singletonList(new AST.Access(null, "user", null)),
                        false, null
                );
                assertEquals(invocation, stmt);
        }

        @Test
        public void testInvokeWithName() throws Exception {
                List<Statement> list = parse("test(a=1, b=2)");
                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                VariableDef v1 = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v1.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));
                VariableDef v2 = new VariableDef("b", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v2.setInit(new NumberLiteral("2", LineCol.SYNTHETIC));
                AST.Invocation invocation = new AST.Invocation(
                        new AST.Access(
                                null, "test", null
                        ),
                        Arrays.asList(
                                v1, v2
                        ),
                        true, null
                );
                assertEquals(invocation, stmt);
        }

        @Test
        public void testDataClass() throws Exception {
                List<Statement> list = parse("data class User");
                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                assertEquals(
                        new ClassDef("User",
                                Collections.singleton(new Modifier(Modifier.Available.DATA, LineCol.SYNTHETIC)),
                                Collections.emptyList(),
                                null,
                                Collections.emptyList(),
                                Collections.emptySet(),
                                Collections.emptyList(), LineCol.SYNTHETIC),
                        stmt
                );
        }
}
