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
import lt.compiler.syntactic.Expression;
import lt.compiler.syntactic.Statement;
import lt.compiler.syntactic.def.ClassDef;
import lt.compiler.syntactic.def.InterfaceDef;
import lt.compiler.syntactic.def.MethodDef;
import lt.compiler.syntactic.def.VariableDef;
import lt.compiler.syntactic.literal.BoolLiteral;
import lt.compiler.syntactic.literal.NumberLiteral;
import lt.compiler.syntactic.literal.StringLiteral;
import lt.compiler.syntactic.operation.TwoVariableOperation;
import lt.compiler.syntactic.pre.PackageDeclare;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.*;

/**
 * parser error recovery
 */
public class TestParserErrorRecovery {
        private List<Statement> parse(String code, ErrorManager err) throws IOException, SyntaxException {
                IndentScanner scanner = new IndentScanner("test.lt", new StringReader(code), new Properties(), err);
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

        @Test
        public void testNextNodeNull() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "1+"
                /*         ^Unexpected End */
                        , err);
                assertEquals(1, err.errorList.size());
                assertEquals(1, err.errorList.get(0).lineCol.line);
                assertEquals(3, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedEnd, err.errorList.get(0).type);

                assertEquals(0, statements.size());
        }

        @Test
        public void testTokenNoWhereToPlace() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "if a type Object\n" +
                /*          ^no where to place */
                                "    ..."
                        , err);

                assertEquals(1, err.errorList.size());
                assertEquals(1, err.errorList.get(0).lineCol.line);
                assertEquals(4, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.Syntax, err.errorList.get(0).type);

                assertEquals(1, statements.size());
                assertEquals(
                        new AST.If(Collections.singletonList(
                                new AST.If.IfPair(
                                        new AST.TypeOf(new AST.Access(null, "Object", LineCol.SYNTHETIC), LineCol.SYNTHETIC),
                                        Collections.singletonList(new AST.Pass(LineCol.SYNTHETIC)),
                                        LineCol.SYNTHETIC)
                        ), LineCol.SYNTHETIC),
                        statements.get(0)
                );
        }

        @Test
        public void testStaticNoStmt() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "static private"
                /*              ^not a statement */
                        , err);

                assertEquals(1, err.errorList.size());
                assertEquals(1, err.errorList.get(0).lineCol.line);
                assertEquals(8, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(0).type);

                assertEquals(0, statements.size());
        }

        @Test
        public void testSyncNotExp() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "synchronized(a, method():Unit)\n" +
                /*                       ^not an expression */
                                "    ..."
                        , err);

                assertEquals(1, err.errorList.size());
                assertEquals(1, err.errorList.get(0).lineCol.line);
                assertEquals(17, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(0).type);

                assertEquals(1, statements.size());
                assertEquals(
                        new AST.Synchronized(
                                Collections.singletonList(new AST.Access(null, "a", LineCol.SYNTHETIC)),
                                Collections.singletonList(new AST.Pass(LineCol.SYNTHETIC)),
                                LineCol.SYNTHETIC
                        ),
                        statements.get(0)
                );
        }

        @Test
        public void testWhileEmptyBody() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "while true"
                /*       ^not elem start */
                        , err);

                assertEquals(1, err.errorList.size());
                assertEquals(1, err.errorList.get(0).lineCol.line);
                assertEquals(1, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(0).type);

                assertEquals(1, statements.size());
                assertEquals(
                        new AST.While(
                                new BoolLiteral("true", LineCol.SYNTHETIC),
                                Collections.emptyList(),
                                false,
                                LineCol.SYNTHETIC
                        ),
                        statements.get(0)
                );
        }

        @Test
        public void testDoWhileEmptyBody() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "do a"
                /*          ^unexpected
                 *       ^unexpected end
                 */
                        , err);

                assertEquals(2, err.errorList.size());
                assertEquals(1, err.errorList.get(0).lineCol.line);
                assertEquals(4, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(0).type);

                assertEquals(1, err.errorList.get(1).lineCol.line);
                assertEquals(1, err.errorList.get(1).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedEnd, err.errorList.get(1).type);

                assertEquals(0, statements.size());
        }

        @Test
        public void testPkgImportNotAccess() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "import method()\n" +
                /*              ^not access */
                                "import method()._"
                /*              ^not import static */
                        , err);

                assertEquals(2, err.errorList.size());
                assertEquals(1, err.errorList.get(0).lineCol.line);
                assertEquals(8, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(0).type);
                assertEquals(2, err.errorList.get(1).lineCol.line);
                assertEquals(8, err.errorList.get(1).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(0).type);

                assertEquals(0, statements.size());
        }

        @Test
        public void testPkgDec() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "package pack.clsx\n" +
                /*                   ^not :: not name */
                                "package pack cls\n" +
                /*                    ^shouldn't be name */
                                "package\n" +
                /*              ^shouldn't be name */
                                "    ..."
                        , err);

                assertEquals(4, err.errorList.size());
                assertEquals(1, err.errorList.get(0).lineCol.line);
                assertEquals(13, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(0).type);

                assertEquals(4, err.errorList.size());
                assertEquals(2, err.errorList.get(1).lineCol.line);
                assertEquals(14, err.errorList.get(1).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(1).type);

                assertEquals(2, err.errorList.get(2).lineCol.line);
                assertEquals(1, err.errorList.get(2).lineCol.column);
                assertEquals(ErrorManager.CompilingError.Syntax, err.errorList.get(2).type);

                assertEquals(4, err.errorList.get(3).lineCol.line);
                assertEquals(5, err.errorList.get(3).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(3).type);

                assertEquals(
                        Arrays.asList(
                                new PackageDeclare(new AST.PackageRef("pack::clsx", LineCol.SYNTHETIC), LineCol.SYNTHETIC),
                                new PackageDeclare(new AST.PackageRef("pack", LineCol.SYNTHETIC), LineCol.SYNTHETIC),
                                new PackageDeclare(new AST.PackageRef("", LineCol.SYNTHETIC), LineCol.SYNTHETIC)
                        ),
                        statements
                );
        }

        @Test
        public void testAnno() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "@[]\n" +
                /*        ^expecting annotation instance */
                                "a"
                        , err);

                assertEquals(1, err.errorList.size());

                assertEquals(1, err.errorList.get(0).lineCol.line);
                assertEquals(2, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(0).type);

                assertEquals(1, statements.size());
                assertEquals(
                        new AST.Access(null, "a", LineCol.SYNTHETIC),
                        statements.get(0)
                );
        }

        @Test
        public void testTry() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "try\n" +
                /*       ^invalid try */
                                "try\n" +
                                "    ...\n" +
                                "catch e fail1\n" +
                /*                    ^unexpected */
                                "try\n" +
                                "    ...\n" +
                                "finally fail2"
                /*               ^unexpected token */
                        , err);

                assertEquals(3, err.errorList.size());

                assertEquals(1, err.errorList.get(0).lineCol.line);
                assertEquals(1, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.Syntax, err.errorList.get(0).type);

                assertEquals(4, err.errorList.get(1).lineCol.line);
                assertEquals(9, err.errorList.get(1).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(1).type);

                assertEquals(7, err.errorList.get(2).lineCol.line);
                assertEquals(9, err.errorList.get(2).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(2).type);

                assertEquals(
                        Arrays.asList(
                                new AST.Try(
                                        Collections.singletonList(new AST.Pass(LineCol.SYNTHETIC)),
                                        "e",
                                        Collections.emptyList(),
                                        Collections.emptyList(),
                                        LineCol.SYNTHETIC
                                ),
                                new AST.Try(
                                        Collections.singletonList(new AST.Pass(LineCol.SYNTHETIC)),
                                        null,
                                        Collections.emptyList(),
                                        Collections.emptyList(),
                                        LineCol.SYNTHETIC
                                )
                        ),
                        statements
                );
        }

        @Test
        public void testInterface() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "interface I:invoke()\n" +
                /*                   ^invalid try */
                                "interface :\n" +
                /*                 ^invalid try */
                                "interface\n" +
                                "    ...."
                /*           ^invalid try */
                        , err);

                assertEquals(3, err.errorList.size());

                assertEquals(1, err.errorList.get(0).lineCol.line);
                assertEquals(13, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(0).type);

                assertEquals(2, err.errorList.get(1).lineCol.line);
                assertEquals(11, err.errorList.get(1).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(1).type);

                assertEquals(4, err.errorList.get(2).lineCol.line);
                assertEquals(5, err.errorList.get(2).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(2).type);

                assertEquals(
                        Collections.singletonList(
                                new InterfaceDef(
                                        "I", Collections.emptySet(), Collections.emptyList(),
                                        Collections.emptySet(), Collections.emptyList(), LineCol.SYNTHETIC)
                        ),
                        statements
                );
        }

        @Test
        public void testClass() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "class C(a=1,b)\n" +
                /*                   ^should have init value */
                                "class C(a, method())\n" +
                /*                  ^not variableDef or Access(null,'') */
                                "class C*\n" +
                /*              ^unexpected */
                                "class C:P1(),P2()\n" +
                /*                    ^multiple Inheritance */
                                "class C:a*2\n" +
                /*                ^not invocation nor access */
                                "class\n" +
                                "    ..."
                /*           ^unexpected */
                        , err);

                assertEquals(6, err.errorList.size());

                assertEquals(1, err.errorList.get(0).lineCol.line);
                assertEquals(13, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.Syntax, err.errorList.get(0).type);

                assertEquals(2, err.errorList.get(1).lineCol.line);
                assertEquals(12, err.errorList.get(1).lineCol.column);
                assertEquals(ErrorManager.CompilingError.Syntax, err.errorList.get(1).type);

                assertEquals(3, err.errorList.get(2).lineCol.line);
                assertEquals(8, err.errorList.get(2).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(2).type);

                assertEquals(4, err.errorList.get(3).lineCol.line);
                assertEquals(14, err.errorList.get(3).lineCol.column);
                assertEquals(ErrorManager.CompilingError.Syntax, err.errorList.get(3).type);

                assertEquals(5, err.errorList.get(4).lineCol.line);
                assertEquals(10, err.errorList.get(4).lineCol.column);
                assertEquals(ErrorManager.CompilingError.Syntax, err.errorList.get(4).type);

                assertEquals(7, err.errorList.get(5).lineCol.line);
                assertEquals(5, err.errorList.get(5).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(5).type);

                VariableDef var_1_a = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                var_1_a.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));
                assertEquals(Arrays.asList(
                        new ClassDef(
                                "C", Collections.emptySet(),
                                Arrays.asList(
                                        var_1_a,
                                        new VariableDef("b", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC)
                                ),
                                null,
                                Collections.emptyList(),
                                Collections.emptySet(),
                                Collections.emptyList(),
                                LineCol.SYNTHETIC
                        ),
                        new ClassDef(
                                "C", Collections.emptySet(),
                                Collections.singletonList(
                                        new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC)
                                ),
                                null,
                                Collections.emptyList(),
                                Collections.emptySet(),
                                Collections.emptyList(),
                                LineCol.SYNTHETIC
                        ),
                        new ClassDef(
                                "C", Collections.emptySet(),
                                Collections.emptyList(),
                                null,
                                Collections.emptyList(),
                                Collections.emptySet(),
                                Collections.emptyList(),
                                LineCol.SYNTHETIC
                        ),
                        new ClassDef(
                                "C", Collections.emptySet(),
                                Collections.emptyList(),
                                new AST.Invocation(
                                        new AST.Access(null, "P1", LineCol.SYNTHETIC),
                                        Collections.emptyList(),
                                        false, LineCol.SYNTHETIC
                                ),
                                Collections.singletonList(
                                        new AST.Access(null, "P2", LineCol.SYNTHETIC)
                                ),
                                Collections.emptySet(),
                                Collections.emptyList(),
                                LineCol.SYNTHETIC
                        ),
                        new ClassDef(
                                "C", Collections.emptySet(),
                                Collections.emptyList(),
                                null,
                                Collections.emptyList(),
                                Collections.emptySet(),
                                Collections.emptyList(),
                                LineCol.SYNTHETIC
                        )
                ), statements);
        }

        @Test
        public void testFor() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "for\n" +
                                "    ..."
                /*           ^unexpected */
                        , err);

                assertEquals(1, err.errorList.size());

                assertEquals(2, err.errorList.get(0).lineCol.line);
                assertEquals(5, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.Syntax, err.errorList.get(0).type);

                assertEquals(0, statements.size());
        }

        @Test
        public void testIf() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "if true\n" +
                                "else\n" +
                                "elseif true"
                /*              ^syntax */
                        , err);

                assertEquals(1, err.errorList.size());

                assertEquals(3, err.errorList.get(0).lineCol.line);
                assertEquals(8, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.Syntax, err.errorList.get(0).type);

                assertEquals(1, statements.size());
                assertEquals(
                        new AST.If(Arrays.asList(
                                new AST.If.IfPair(new BoolLiteral("true", LineCol.SYNTHETIC), Collections.emptyList(), LineCol.SYNTHETIC),
                                new AST.If.IfPair(null, Collections.emptyList(), LineCol.SYNTHETIC)
                        ), LineCol.SYNTHETIC),
                        statements.get(0)
                );
        }

        @Test
        public void testMethod() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "method(a=1,b):Unit\n" +
                /*                  ^no init value */
                                "method(a=1,method()):Unit\n"
                /*                  ^syntax */
                        , err);

                assertEquals(2, err.errorList.size());

                assertEquals(1, err.errorList.get(0).lineCol.line);
                assertEquals(12, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.Syntax, err.errorList.get(0).type);

                assertEquals(2, err.errorList.get(1).lineCol.line);
                assertEquals(12, err.errorList.get(1).lineCol.column);
                assertEquals(ErrorManager.CompilingError.Syntax, err.errorList.get(1).type);

                VariableDef v = new VariableDef("a", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC);
                v.setInit(new NumberLiteral("1", LineCol.SYNTHETIC));

                assertEquals(
                        Arrays.asList(
                                new MethodDef(
                                        "method", Collections.emptySet(),
                                        new AST.Access(null, "Unit", LineCol.SYNTHETIC),
                                        Arrays.asList(
                                                v,
                                                new VariableDef("b", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC)
                                        ),
                                        Collections.emptySet(),
                                        Collections.emptyList(),
                                        LineCol.SYNTHETIC
                                ),
                                new MethodDef(
                                        "method", Collections.emptySet(),
                                        new AST.Access(null, "Unit", LineCol.SYNTHETIC),
                                        Collections.singletonList(v),
                                        Collections.emptySet(),
                                        Collections.emptyList(),
                                        LineCol.SYNTHETIC
                                )
                        ),
                        statements
                );
        }

        @Test
        public void testAs() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "as 1"
                /*                  ^syntax */
                        , err);

                assertEquals(1, err.errorList.size());
                assertEquals(1, err.errorList.get(0).lineCol.line);
                assertEquals(1, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(0).type);

                assertEquals(0, statements.size());
        }

        @Test
        public void testInvocation() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "method()()\n" +
                /*       ^!!!! it's NO longer a syntax error */
                                "method(())\n" +
                /*              ^syntax */
                                "method(class C, a)"
                /*              ^syntax */
                        , err);

                assertEquals(2, err.errorList.size());

                assertEquals(2, err.errorList.get(0).lineCol.line);
                assertEquals(9, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.Syntax, err.errorList.get(0).type);

                assertEquals(3, err.errorList.get(1).lineCol.line);
                assertEquals(8, err.errorList.get(1).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(1).type);

                assertEquals(
                        Arrays.asList(
                                new AST.Invocation(
                                        new AST.Invocation(
                                                new AST.Access(null, "method", LineCol.SYNTHETIC),
                                                Collections.emptyList(),
                                                false, LineCol.SYNTHETIC
                                        ),
                                        Collections.emptyList(),
                                        false, LineCol.SYNTHETIC
                                ),
                                new AST.Invocation(
                                        new AST.Access(null, "method", LineCol.SYNTHETIC),
                                        Collections.emptyList(),
                                        false, LineCol.SYNTHETIC
                                ),
                                new AST.Invocation(
                                        new AST.Access(null, "method", LineCol.SYNTHETIC),
                                        Collections.singletonList(new AST.Access(null, "a", LineCol.SYNTHETIC)),
                                        false, LineCol.SYNTHETIC
                                )
                        ),
                        statements
                );
        }

        @Test
        public void testMap() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "{\n" +
                                "    'a':,\n" +
                /*              ^syntax */
                                "    'b':2\n" +
                                "}\n" +
                                "{\n" +
                                "    'a':1\n" +
                                "    'b':class C\n" +
                /*               ^syntax */
                                "    'c':3\n" +
                                "}\n" +
                                "{\n" +
                                "    'a'\n" +
                /*           ^syntax */
                                "}"
                        , err);

                assertEquals(4, err.errorList.size());
                assertEquals(2, err.errorList.get(0).lineCol.line);
                assertEquals(8, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.Syntax, err.errorList.get(0).type);

                assertEquals(7, err.errorList.get(1).lineCol.line);
                assertEquals(9, err.errorList.get(1).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(1).type);

                assertEquals(7, err.errorList.get(2).lineCol.line);
                assertEquals(15, err.errorList.get(2).lineCol.column);
                assertEquals(ErrorManager.CompilingError.Syntax, err.errorList.get(2).type);

                assertEquals(11, err.errorList.get(3).lineCol.line);
                assertEquals(5, err.errorList.get(3).lineCol.column);
                assertEquals(ErrorManager.CompilingError.Syntax, err.errorList.get(3).type);

                assertEquals(
                        Arrays.asList(
                                new AST.MapExp(
                                        new LinkedHashMap<Expression, Expression>() {{
                                                put(new StringLiteral("'b'", LineCol.SYNTHETIC), new NumberLiteral("2", LineCol.SYNTHETIC));
                                        }},
                                        LineCol.SYNTHETIC
                                ),
                                new AST.MapExp(
                                        new LinkedHashMap<Expression, Expression>() {{
                                                put(new StringLiteral("'a'", LineCol.SYNTHETIC), new NumberLiteral("1", LineCol.SYNTHETIC));
                                                put(new StringLiteral("'c'", LineCol.SYNTHETIC), new NumberLiteral("3", LineCol.SYNTHETIC));
                                        }},
                                        LineCol.SYNTHETIC
                                ),
                                new AST.MapExp(
                                        new LinkedHashMap<>(),
                                        LineCol.SYNTHETIC
                                )
                        ),
                        statements
                );
        }

        @Test
        public void testIndexAndArray() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "arr[1,class C,3]\n" +
                /*              ^unexpected */
                                "[1,class C,3]"
                /*          ^unexpected */
                        , err);

                assertEquals(2, err.errorList.size());
                assertEquals(1, err.errorList.get(0).lineCol.line);
                assertEquals(7, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(0).type);

                assertEquals(2, err.errorList.get(1).lineCol.line);
                assertEquals(4, err.errorList.get(1).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(1).type);

                assertEquals(
                        Arrays.asList(
                                new AST.Index(
                                        new AST.Access(null, "arr", LineCol.SYNTHETIC),
                                        Arrays.asList(
                                                new NumberLiteral("1", LineCol.SYNTHETIC),
                                                new NumberLiteral("3", LineCol.SYNTHETIC)
                                        ),
                                        LineCol.SYNTHETIC
                                ),
                                new AST.ArrayExp(
                                        Arrays.asList(
                                                new NumberLiteral("1", LineCol.SYNTHETIC),
                                                new NumberLiteral("3", LineCol.SYNTHETIC)
                                        ),
                                        LineCol.SYNTHETIC
                                )
                        ),
                        statements
                );
        }

        @Test
        public void testLambda() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "(a.b, method(), c)->1"
                /*              ^unexpected */
                        , err);

                assertEquals(2, err.errorList.size());
                assertEquals(1, err.errorList.get(0).lineCol.line);
                assertEquals(2, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(0).type);

                assertEquals(1, err.errorList.get(1).lineCol.line);
                assertEquals(7, err.errorList.get(1).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(1).type);

                assertEquals(1, statements.size());
                assertEquals(
                        new AST.Lambda(
                                Collections.singletonList(new VariableDef("c", Collections.emptySet(), Collections.emptySet(), LineCol.SYNTHETIC)),
                                Collections.singletonList(new AST.Return(new NumberLiteral("1", LineCol.SYNTHETIC), LineCol.SYNTHETIC)),
                                LineCol.SYNTHETIC
                        ),
                        statements.get(0)
                );
        }

        @Test
        public void testAccessAssign() throws Exception {
                ErrorManager err = new ErrorManager(false);
                err.out = ErrorManager.Out.allNull();

                List<Statement> statements = parse("" +
                                "a.\n" +
                                "    ...\n"
                /*           ^unexpected */
                        , err);

                assertEquals(1, err.errorList.size());
                assertEquals(2, err.errorList.get(0).lineCol.line);
                assertEquals(5, err.errorList.get(0).lineCol.column);
                assertEquals(ErrorManager.CompilingError.UnexpectedToken, err.errorList.get(0).type);

                assertEquals(0, statements.size());
        }
}
