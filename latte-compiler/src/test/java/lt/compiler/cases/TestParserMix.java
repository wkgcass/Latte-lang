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
import lt.compiler.IndentScanner;
import lt.compiler.Properties;
import lt.compiler.lexical.ElementStartNode;
import lt.compiler.syntactic.*;
import lt.compiler.syntactic.def.MethodDef;
import lt.compiler.syntactic.def.VariableDef;
import lt.compiler.syntactic.literal.BoolLiteral;
import lt.compiler.syntactic.literal.NumberLiteral;
import lt.compiler.syntactic.literal.StringLiteral;
import lt.compiler.syntactic.operation.TwoVariableOperation;
import lt.compiler.syntactic.pre.Modifier;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

import static org.junit.Assert.*;

/**
 * mix test of Syntactic processor results
 */
public class TestParserMix {
        private static List<Statement> parse(String stmt) throws IOException, SyntaxException {
                IndentScanner processor = new IndentScanner("test", new StringReader(stmt), new Properties(), new ErrorManager(true));
                ElementStartNode root = processor.scan();

                Parser syntacticProcessor = new Parser(root, new ErrorManager(true));

                return syntacticProcessor.parse();
        }

        @Test
        public void test2VarOperatorAndAccessAndPar() throws Exception {
                List<Statement> statements = parse("list * (1+2)");
                assertEquals(1, statements.size());
                Statement statement = statements.get(0);

                TwoVariableOperation tvo = new TwoVariableOperation(
                        "*",
                        new AST.Access(null, "list", LineCol.SYNTHETIC),
                        new TwoVariableOperation(
                                "+",
                                new NumberLiteral("1", LineCol.SYNTHETIC),
                                new NumberLiteral("2", LineCol.SYNTHETIC),
                                LineCol.SYNTHETIC),
                        LineCol.SYNTHETIC);
                assertEquals(tvo, statement);
        }

        @Test
        public void testAnnotationOnWrongPosition() throws Exception {
                try {
                        parse("" +
                                "@Anno()\n" +
                                "1+1");
                        fail();
                } catch (SyntaxException ignore) {
                }
        }

        @Test
        public void testModifierOnWrongPosition() throws Exception {
                try {
                        parse("pri 1");
                        fail();
                } catch (SyntaxException ignore) {
                }
        }

        @Test
        public void testAccessDotAndOperator1() throws Exception {
                List<Statement> statements = parse("1+a.b.c+2");
                assertEquals(1, statements.size());
                Statement statement = statements.get(0);

                TwoVariableOperation tvo = new TwoVariableOperation(
                        "+",
                        new TwoVariableOperation(
                                "+",
                                new NumberLiteral("1", LineCol.SYNTHETIC),
                                new AST.Access(new AST.Access(new AST.Access(null, "a", LineCol.SYNTHETIC), "b", LineCol.SYNTHETIC), "c", LineCol.SYNTHETIC),
                                LineCol.SYNTHETIC),
                        new NumberLiteral("2", LineCol.SYNTHETIC),
                        LineCol.SYNTHETIC);
                assertEquals(tvo, statement);
        }

        @Test
        public void testAccessDotAndOperator2() throws Exception {
                List<Statement> statements = parse("1+list.get(0)+2");
                assertEquals(1, statements.size());
                Statement statement = statements.get(0);

                TwoVariableOperation tvo = new TwoVariableOperation(
                        "+",
                        new TwoVariableOperation(
                                "+",
                                new NumberLiteral("1", LineCol.SYNTHETIC),
                                new AST.Invocation(new AST.Access(new AST.Access(null, "list", LineCol.SYNTHETIC), "get", LineCol.SYNTHETIC),
                                        Collections.singletonList(new NumberLiteral("0", LineCol.SYNTHETIC)), false, LineCol.SYNTHETIC),
                                LineCol.SYNTHETIC),
                        new NumberLiteral("2", LineCol.SYNTHETIC),
                        LineCol.SYNTHETIC);
                assertEquals(tvo, statement);
        }

        @Test
        public void testClosureAndOperator() throws Exception {
                List<Statement> statements = parse("" +
                        "1+(\n" +
                        "    if i==1\n" +
                        "        return 5\n" +
                        "    return 2\n" +
                        ")+2");
                assertEquals(1, statements.size());
                Statement statement = statements.get(0);

                TwoVariableOperation tvo = new TwoVariableOperation(
                        "+",
                        new TwoVariableOperation(
                                "+",
                                new NumberLiteral("1", LineCol.SYNTHETIC),
                                new AST.Procedure(Arrays.asList(
                                        new AST.If(Collections.singletonList(new AST.If.IfPair(new TwoVariableOperation(
                                                "==",
                                                new AST.Access(null, "i", LineCol.SYNTHETIC),
                                                new NumberLiteral("1", LineCol.SYNTHETIC)
                                                , LineCol.SYNTHETIC),
                                                Collections.singletonList(
                                                        new AST.Return(new NumberLiteral("5", LineCol.SYNTHETIC), LineCol.SYNTHETIC)
                                                ), LineCol.SYNTHETIC))
                                                , LineCol.SYNTHETIC),
                                        new AST.Return(new NumberLiteral("2", LineCol.SYNTHETIC), LineCol.SYNTHETIC)
                                ), LineCol.SYNTHETIC),
                                LineCol.SYNTHETIC),
                        new NumberLiteral("2", LineCol.SYNTHETIC),
                        LineCol.SYNTHETIC);

                assertEquals(tvo, statement);
        }

        @Test
        public void testMethodMultipleAnnotation() throws Exception {
                List<Statement> statements = parse("" +
                        "@Anno\n" +
                        "public method(\n" +
                        "    @Anno1\n" +
                        "    arg0\n" +
                        "    @Anno2\n" +
                        "    arg1" +
                        "):Unit");
                assertEquals(1, statements.size());
                Statement statement = statements.get(0);

                MethodDef m = new MethodDef(
                        "method",
                        new HashSet<>(Collections.singletonList(
                                new Modifier(Modifier.Available.PUBLIC, LineCol.SYNTHETIC)
                        )),
                        new AST.Access(null, "Unit", LineCol.SYNTHETIC),
                        Arrays.asList(
                                new VariableDef("arg0", Collections.emptySet(), new HashSet<>(Collections.singletonList(
                                        new AST.Anno(new AST.Access(null, "Anno1", LineCol.SYNTHETIC), Collections.emptyList(), LineCol.SYNTHETIC)
                                )), LineCol.SYNTHETIC),
                                new VariableDef("arg1", Collections.emptySet(), new HashSet<>(Collections.singletonList(
                                        new AST.Anno(new AST.Access(null, "Anno2", LineCol.SYNTHETIC), Collections.emptyList(), LineCol.SYNTHETIC)
                                )), LineCol.SYNTHETIC)
                        ),
                        new HashSet<>(Collections.singletonList(
                                new AST.Anno(new AST.Access(null, "Anno", LineCol.SYNTHETIC), Collections.emptyList(), LineCol.SYNTHETIC)
                        )),
                        Collections.emptyList(),
                        LineCol.SYNTHETIC);

                assertEquals(m, statement);
        }

        @Test
        public void testMethodDefInitVal() throws Exception {
                try {
                        parse("method(arg0,arg1=1,arg2)=0");
                        fail();
                } catch (SyntaxException ignore) {
                }
        }

        @Test
        public void testMethodDefInitValPass() throws Exception {
                try {
                        parse("method(arg0,arg1=1,arg2=2)=0");
                } catch (SyntaxException ignore) {
                        fail();
                }
        }

        @Test
        public void testClassDefInitVal() throws Exception {
                try {
                        parse("class C(arg0,arg1=1,arg2)");
                        fail();
                } catch (SyntaxException ignore) {
                }
        }

        @Test
        public void testClassDefInitValPass() throws Exception {
                try {
                        parse("class C(arg0,arg1=1,arg2=2)");
                } catch (SyntaxException ignore) {
                        fail();
                }
        }

        @Test
        public void testMultipleLines() throws Exception {
                List<Statement> statements = parse("" +
                        "3+\n" +
                        "2");
                assertEquals(1, statements.size());
                Statement statement = statements.get(0);

                TwoVariableOperation tvo = new TwoVariableOperation("+", new NumberLiteral("3", LineCol.SYNTHETIC), new NumberLiteral("2", LineCol.SYNTHETIC), LineCol.SYNTHETIC);
                assertEquals(tvo, statement);
        }

        @Test
        public void testPrimitive() throws Exception {
                List<Statement> statements = parse("i:int");
                assertEquals(1, statements.size());
                Statement statement = statements.get(0);

                VariableDef v = new VariableDef("i", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setType(new AST.Access(null, "int", LineCol.SYNTHETIC));
                assertEquals(v, statement);
        }

        @Test
        public void testPrimitiveAssign() throws Exception {
                List<Statement> statements = parse("i:bool=yes");
                assertEquals(1, statements.size());
                Statement statement = statements.get(0);

                VariableDef v = new VariableDef("i", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setType(new AST.Access(null, "bool", LineCol.SYNTHETIC));
                v.setInit(new BoolLiteral("yes", LineCol.SYNTHETIC));
                assertEquals(v, statement);
        }

        @Test
        public void testMethodPrimitive() throws Exception {
                List<Statement> statements = parse("method():int");
                assertEquals(1, statements.size());
                Statement statement = statements.get(0);

                MethodDef m = new MethodDef("method", Collections.emptySet(), new AST.Access(null, "int", LineCol.SYNTHETIC), Collections.emptyList(), Collections.emptySet(), Collections.emptyList(), LineCol.SYNTHETIC);
                assertEquals(m, statement);
        }

        @Test
        public void testMapList() throws Exception {
                List<Statement> statements = parse("{a:[1,2]}");
                assertEquals(1, statements.size());
                Statement statement = statements.get(0);

                LinkedHashMap<Expression, Expression> map = new LinkedHashMap<>();
                map.put(new AST.Access(null, "a", LineCol.SYNTHETIC),
                        new AST.ArrayExp(Arrays.asList(
                                new NumberLiteral("1", LineCol.SYNTHETIC),
                                new NumberLiteral("2", LineCol.SYNTHETIC)
                        ), LineCol.SYNTHETIC));
                AST.MapExp mapExp = new AST.MapExp(map, LineCol.SYNTHETIC);
                assertEquals(mapExp, statement);
        }

        @Test
        public void testListMap() throws Exception {
                List<Statement> statements = parse("[{'a':b},{'c':d}]");
                assertEquals(1, statements.size());
                Statement statement = statements.get(0);

                LinkedHashMap<Expression, Expression> map1 = new LinkedHashMap<>();
                map1.put(new StringLiteral("'a'", LineCol.SYNTHETIC), new AST.Access(null, "b", LineCol.SYNTHETIC));
                LinkedHashMap<Expression, Expression> map2 = new LinkedHashMap<>();
                map2.put(new StringLiteral("'c'", LineCol.SYNTHETIC), new AST.Access(null, "d", LineCol.SYNTHETIC));
                AST.ArrayExp arrayExp = new AST.ArrayExp(Arrays.asList(
                        new AST.MapExp(map1, LineCol.SYNTHETIC),
                        new AST.MapExp(map2, LineCol.SYNTHETIC)
                ), LineCol.SYNTHETIC);
                assertEquals(arrayExp, statement);
        }

        @Test
        public void testMapWithListAndOther() throws Exception {
                List<Statement> statements = parse("{a:[1,2],b:c}");
                assertEquals(1, statements.size());
                Statement statement = statements.get(0);

                LinkedHashMap<Expression, Expression> map = new LinkedHashMap<>();
                map.put(new AST.Access(null, "a", LineCol.SYNTHETIC),
                        new AST.ArrayExp(Arrays.asList(
                                new NumberLiteral("1", LineCol.SYNTHETIC),
                                new NumberLiteral("2", LineCol.SYNTHETIC)
                        ), LineCol.SYNTHETIC));
                map.put(new AST.Access(null, "b", LineCol.SYNTHETIC),
                        new AST.Access(null, "c", LineCol.SYNTHETIC));
                AST.MapExp mapExp = new AST.MapExp(map, LineCol.SYNTHETIC);
                assertEquals(mapExp, statement);
        }

        @Test
        public void testListWithMapAndOther() throws Exception {
                List<Statement> statements = parse("[a,{'c':d}]");
                assertEquals(1, statements.size());
                Statement statement = statements.get(0);

                LinkedHashMap<Expression, Expression> map2 = new LinkedHashMap<>();
                map2.put(new StringLiteral("'c'", LineCol.SYNTHETIC), new AST.Access(null, "d", LineCol.SYNTHETIC));
                AST.ArrayExp arrayExp = new AST.ArrayExp(Arrays.asList(
                        new AST.Access(null, "a", LineCol.SYNTHETIC),
                        new AST.MapExp(map2, LineCol.SYNTHETIC)
                ), LineCol.SYNTHETIC);
                assertEquals(arrayExp, statement);
        }

        @Test
        public void testArrayPrimitive() throws Exception {
                List<Statement> statements = parse("i:[]int");
                assertEquals(1, statements.size());
                Statement statement = statements.get(0);

                VariableDef v = new VariableDef("i", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setType(new AST.Access(new AST.Access(null, "int", LineCol.SYNTHETIC), "[]", LineCol.SYNTHETIC));
                assertEquals(v, statement);
        }

        @Test
        public void testArrayPkgClass() throws Exception {
                List<Statement> statements = parse("i:[]java::util::List");
                assertEquals(1, statements.size());
                Statement statement = statements.get(0);

                VariableDef v = new VariableDef("i", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setType(new AST.Access(new AST.Access(new AST.PackageRef("java::util", LineCol.SYNTHETIC), "List", LineCol.SYNTHETIC), "[]", LineCol.SYNTHETIC));
                assertEquals(v, statement);
        }

        @Test
        public void testTypeOfArray() throws Exception {
                List<Statement> statements = parse("type []int");
                assertEquals(1, statements.size());
                Statement statement = statements.get(0);

                AST.TypeOf typeOf = new AST.TypeOf(new AST.Access(new AST.Access(null, "int", LineCol.SYNTHETIC), "[]", LineCol.SYNTHETIC), LineCol.SYNTHETIC);
                assertEquals(typeOf, statement);
        }

        @Test
        public void testIndexAssign() throws Exception {
                List<Statement> list = parse("a[1]=2");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);

                assertEquals(
                        new AST.Assignment(
                                new AST.Access(new AST.Index(new AST.Access(null, "a", LineCol.SYNTHETIC), Collections.singletonList(new NumberLiteral("1", LineCol.SYNTHETIC)), LineCol.SYNTHETIC), null, LineCol.SYNTHETIC),
                                "=",
                                new NumberLiteral("2", LineCol.SYNTHETIC)
                                , LineCol.SYNTHETIC)
                        ,
                        stmt
                );
        }

        @Test
        public void testUndefinedAssign() throws Exception {
                List<Statement> list = parse("a=undefined");

                assertEquals(1, list.size());

                Statement stmt = list.get(0);
                VariableDef v = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(new AST.UndefinedExp(LineCol.SYNTHETIC));
                assertEquals(v, stmt);
        }

        @Test
        public void testStaticStatement() throws Exception {
                List<Statement> list = parse("" +
                        "static method():Unit\n" +
                        "static field=1\n" +
                        "static System.out.println('hello world')");

                assertEquals(3, list.size());

                Statement stmt0 = list.get(0);
                Statement stmt1 = list.get(1);
                Statement stmt2 = list.get(2);

                assertTrue(stmt0 instanceof AST.StaticScope);
                assertTrue(stmt1 instanceof AST.StaticScope);
                assertTrue(stmt2 instanceof AST.StaticScope);

                stmt0 = ((AST.StaticScope) stmt0).statements.get(0);
                stmt1 = ((AST.StaticScope) stmt1).statements.get(0);
                stmt2 = ((AST.StaticScope) stmt2).statements.get(0);

                assertTrue(stmt0 instanceof MethodDef);
                assertTrue(stmt1 instanceof VariableDef);
                assertTrue(stmt2 instanceof AST.Invocation);
        }

        @Test
        public void testMap_OperatorLikeInvocation() throws Exception {
                List<Statement> list = parse("" +
                        "{\n" +
                        "    'a':a op b\n" +
                        "    a op b:'b'\n" +
                        "    'a':a op\n" +
                        "    a op:'b'\n" +
                        "}");

                assertEquals(1, list.size());
                AST.MapExp mapExp = new AST.MapExp(new LinkedHashMap<Expression, Expression>() {
                        {
                                put(new StringLiteral("'a'", null), new AST.Invocation(
                                        new AST.Access(new AST.Access(null, "a", null), "op", null),
                                        Collections.singletonList(new AST.Access(null, "b", null)),
                                        false, null
                                ));
                                put(new AST.Invocation(
                                        new AST.Access(new AST.Access(null, "a", null), "op", null),
                                        Collections.singletonList(new AST.Access(null, "b", null)),
                                        false, null
                                ), new StringLiteral("'b'", null));
                                put(new StringLiteral("'a'", null), new AST.Invocation(
                                        new AST.Access(new AST.Access(null, "a", null), "op", null),
                                        Collections.emptyList(),
                                        false, null
                                ));
                                put(new AST.Invocation(
                                        new AST.Access(new AST.Access(null, "a", null), "op", null),
                                        Collections.emptyList(),
                                        false, null
                                ), new StringLiteral("'b'", null));
                        }
                }, null);
                assertEquals(mapExp, list.get(0));
        }

        @Test
        public void testOperatorLikeInvocationWithTwoVarOp() throws Exception {
                List<Statement> list = parse("a op b + 1 op 2 op");
                assertEquals(1, list.size());

                // ((a op (b+1)) op 2) op
                AST.Invocation invocation = new AST.Invocation(
                        new AST.Access(
                                new AST.Invocation(
                                        new AST.Access(
                                                new AST.Invocation(
                                                        new AST.Access(
                                                                new AST.Access(null, "a", null),
                                                                "op",
                                                                null
                                                        ),
                                                        Collections.singletonList(
                                                                new TwoVariableOperation(
                                                                        "+",
                                                                        new AST.Access(
                                                                                null,
                                                                                "b",
                                                                                null
                                                                        ),
                                                                        new NumberLiteral("1", null),
                                                                        null
                                                                )
                                                        ),
                                                        false, null
                                                ),
                                                "op",
                                                null
                                        ),
                                        Collections.singletonList(new NumberLiteral("2", null)),
                                        false, null
                                ),
                                "op",
                                null
                        ),
                        Collections.emptyList(),
                        false, null
                );

                assertEquals(invocation, list.get(0));
        }

        @Test
        public void testMapIndexAccess() throws Exception {
                List<Statement> list = parse(
                        "" +
                                "{\n" +
                                "    'USER' : session['USER']\n" +
                                "}"
                );
                assertEquals(
                        Collections.singletonList(
                                new AST.MapExp(new LinkedHashMap<Expression, Expression>() {{
                                        put(new StringLiteral("'USER'", LineCol.SYNTHETIC),
                                                new AST.Index(
                                                        new AST.Access(null, "session", LineCol.SYNTHETIC),
                                                        Collections.singletonList(
                                                                new StringLiteral("'USER'", LineCol.SYNTHETIC)
                                                        ), LineCol.SYNTHETIC
                                                ));
                                }}, LineCol.SYNTHETIC)
                        ),
                        list
                );
        }
}
