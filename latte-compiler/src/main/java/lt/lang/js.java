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

package lt.lang;

import lt.compiler.*;
import lt.compiler.semantic.SClassDef;
import lt.compiler.semantic.Value;
import lt.compiler.semantic.builtin.StringConstantValue;
import lt.compiler.syntactic.*;
import lt.compiler.syntactic.def.*;
import lt.compiler.syntactic.literal.BoolLiteral;
import lt.compiler.syntactic.literal.NumberLiteral;
import lt.compiler.syntactic.literal.RegexLiteral;
import lt.compiler.syntactic.literal.StringLiteral;
import lt.compiler.syntactic.operation.OneVariableOperation;
import lt.compiler.syntactic.operation.TwoVariableOperation;
import lt.compiler.syntactic.operation.UnaryOneVariableOperation;
import lt.compiler.syntactic.pre.Modifier;
import lt.generator.SourceGenerator;

import java.util.*;

/**
 * transform latte AST into javascript code
 */
public class js implements SourceGenerator {
        private static final int INDENT = 4;

        private List<Statement> ast;
        private SemanticProcessor processor;
        private ErrorManager err;

        @Override
        public void init(List<Statement> ast, SemanticProcessor processor, SemanticScope scope, LineCol lineCol, ErrorManager err) {
                this.ast = ast;
                this.processor = processor;
                this.err = err;
        }

        /**
         * start the generation process
         *
         * @return js code
         * @throws SyntaxException exception
         */
        @Override
        public Value generate() throws SyntaxException {
                StringBuilder sb = new StringBuilder();
                buildStatements(sb, ast, 0);
                StringConstantValue s = new StringConstantValue(sb.toString().trim());
                s.setType((SClassDef) processor.getTypeWithName("java.lang.String", LineCol.SYNTHETIC));
                return s;
        }

        @Override
        public int resultType() {
                return VALUE;
        }

        /**
         * build a list of statements. the `;` at the end is automatically generated.
         *
         * @param sb          string builder
         * @param statements  statements
         * @param indentation indentation
         * @throws SyntaxException exception
         */
        private void buildStatements(StringBuilder sb, List<Statement> statements, int indentation) throws SyntaxException {
                for (Statement stmt : statements) {
                        buildStatement(sb, stmt, indentation);
                        if (sb.charAt(sb.length() - 1) != '\n') {
                                sb.append(";\n");
                        }
                }
        }

        /**
         * build indentation spaces.
         *
         * @param sb          string builder
         * @param indentation spaces count
         */
        private void buildIndentation(StringBuilder sb, int indentation) {
                for (int i = 0; i < indentation; ++i) {
                        sb.append(" ");
                }
        }

        /**
         * no annotations
         *
         * @param annos annotations' container
         * @throws SyntaxException exception
         */
        private void assertNoAnno(Collection<AST.Anno> annos) throws SyntaxException {
                if (!annos.isEmpty()) {
                        err.SyntaxException("JavaScript don't have annotations", annos.iterator().next().line_col());
                }
        }

        /**
         * no modifiers
         *
         * @param modifiers modifiers' container
         * @throws SyntaxException exception
         */
        private void assertNoModifier(Collection<Modifier> modifiers) throws SyntaxException {
                if (!modifiers.isEmpty()) {
                        if (modifiers.size() != 1 || !modifiers.contains(new Modifier(Modifier.Available.DEF, LineCol.SYNTHETIC))) {
                                err.SyntaxException("JavaScript don't have modifiers", modifiers.iterator().next().line_col());
                        }
                }
        }

        /**
         * no type
         *
         * @param type the type, which asserts to be null.
         * @throws SyntaxException exception
         */
        private void assertNoType(AST.Access type) throws SyntaxException {
                if (type != null) {
                        err.SyntaxException("JavaScript don't have type", type.line_col());
                }
        }

        /**
         * build statements.
         *
         * @param sb          string builder
         * @param stmt        statements
         * @param indentation indentation
         * @throws SyntaxException exception
         */
        private void buildStatement(StringBuilder sb, Statement stmt, int indentation) throws SyntaxException {
                if (stmt instanceof ClassDef) {
                        buildClass(sb, (ClassDef) stmt, indentation);
                } else if (stmt instanceof InterfaceDef) {
                        err.SyntaxException("JavaScript don't have interfaces", stmt.line_col());
                } else if (stmt instanceof FunDef) {
                        buildFun(sb, (FunDef) stmt, indentation);
                } else if (stmt instanceof MethodDef) {
                        buildMethod(sb, (MethodDef) stmt, indentation);
                } else if (stmt instanceof Pre) {
                        err.SyntaxException("JavaScript don't support " + stmt, stmt.line_col());
                } else if (stmt instanceof Expression) {
                        buildIndentation(sb, indentation);
                        buildExpression(sb, (Expression) stmt, indentation);
                } else if (stmt instanceof AST.Anno) {
                        err.SyntaxException("JavaScript don't support annotations", stmt.line_col());
                } else if (stmt instanceof AST.For) {
                        buildFor(sb, (AST.For) stmt, indentation);
                } else if (stmt instanceof AST.If) {
                        buildIf(sb, (AST.If) stmt, indentation);
                } else if (stmt instanceof AST.Pass) {
                        // do nothing
                        buildIndentation(sb, indentation);
                        sb.append("/* pass */");
                } else if (stmt instanceof AST.Return) {
                        buildIndentation(sb, indentation);
                        sb.append("return ");

                        buildExpression(sb, ((AST.Return) stmt).exp, indentation);
                } else if (stmt instanceof AST.StaticScope) {
                        err.SyntaxException("JavaScript don't support static", stmt.line_col());

                        buildIndentation(sb, indentation);
                        sb.append("function static() {\n");

                        buildStatements(sb, ((AST.StaticScope) stmt).statements, indentation + INDENT);

                        buildIndentation(sb, indentation);
                        sb.append("}\n");
                } else if (stmt instanceof AST.Synchronized) {
                        err.SyntaxException("JavaScript don't support synchronized", stmt.line_col());

                        buildIndentation(sb, indentation);
                        sb.append("function synchronized() {\n");

                        buildStatements(sb, ((AST.Synchronized) stmt).statements, indentation + INDENT);

                        buildIndentation(sb, indentation);
                        sb.append("}\n");
                } else if (stmt instanceof AST.Throw) {
                        buildIndentation(sb, indentation);
                        sb.append("throw ");

                        buildExpression(sb, ((AST.Throw) stmt).exp, indentation);
                } else if (stmt instanceof AST.Try) {

                        buildIndentation(sb, indentation);
                        sb.append("try {\n");

                        buildStatements(sb, ((AST.Try) stmt).statements, indentation + INDENT);

                        buildIndentation(sb, indentation);
                        sb.append("} catch (").append(((AST.Try) stmt).varName).append(") {\n");

                        buildStatements(sb, ((AST.Try) stmt).catchStatements, indentation + INDENT);

                        buildIndentation(sb, indentation);
                        sb.append("} finally {\n");

                        buildStatements(sb, ((AST.Try) stmt).fin, indentation + INDENT);

                        buildIndentation(sb, indentation);
                        sb.append("}\n");
                } else if (stmt instanceof AST.While) {
                        if (((AST.While) stmt).doWhile) {
                                buildIndentation(sb, indentation);
                                sb.append("do {\n");

                                buildStatements(sb, ((AST.While) stmt).statements, indentation + INDENT);

                                buildIndentation(sb, indentation);
                                sb.append("} while (");

                                buildExpression(sb, ((AST.While) stmt).condition, indentation);

                                sb.append(");\n");
                        } else {
                                buildIndentation(sb, indentation);
                                sb.append("while (");

                                buildExpression(sb, ((AST.While) stmt).condition, indentation);
                                sb.append(") {\n");
                                buildStatements(sb, ((AST.While) stmt).statements, indentation + INDENT);

                                buildIndentation(sb, indentation);
                                sb.append("}\n");
                        }
                } else if (stmt instanceof AST.Continue) {
                        buildIndentation(sb, indentation);
                        sb.append("continue");
                } else if (stmt instanceof AST.Break) {
                        buildIndentation(sb, indentation);
                        sb.append("break");
                } else throw new LtBug("unknown token " + stmt);
        }

        /**
         * build expressions.
         *
         * @param sb          string builder
         * @param exp         expression
         * @param indentation indentation
         * @throws SyntaxException exception
         */
        private void buildExpression(StringBuilder sb, Expression exp, int indentation) throws SyntaxException {
                if (exp instanceof VariableDef) {
                        assertNoAnno(((VariableDef) exp).getAnnos());
                        assertNoModifier(((VariableDef) exp).getModifiers());
                        assertNoType(((VariableDef) exp).getType());

                        // var xxx
                        sb.append("var ").append(((VariableDef) exp).getName());
                        if (((VariableDef) exp).getInit() != null) {
                                // =
                                sb.append(" = ");
                                // expression
                                buildExpression(sb, ((VariableDef) exp).getInit(), indentation);
                        }
                } else if (exp instanceof Literal) {
                        String literal = ((Literal) exp).literal();
                        if (exp instanceof BoolLiteral) {
                                if (literal.equals("true") || literal.equals("yes")) {
                                        sb.append("true");
                                } else {
                                        sb.append("false");
                                }
                        } else if (exp instanceof NumberLiteral) {
                                sb.append(literal);
                        } else if (exp instanceof RegexLiteral) {
                                String regex = CompileUtil.getRegexStr(literal);
                                sb.append("/").append(regex.replace("/", "\\/")).append("/");
                        } else if (exp instanceof StringLiteral) {
                                sb.append(literal);
                        } else throw new LtBug("unknown literal type " + exp.getClass());
                } else if (exp instanceof Operation) {
                        if (exp instanceof UnaryOneVariableOperation) {
                                sb.append(((UnaryOneVariableOperation) exp).operator());
                                buildExpression(sb, ((UnaryOneVariableOperation) exp).expressions().get(0), indentation);
                        } else if (exp instanceof OneVariableOperation) {
                                buildExpression(sb, ((OneVariableOperation) exp).expressions().get(0), indentation);
                                sb.append(((OneVariableOperation) exp).operator());
                        } else if (exp instanceof TwoVariableOperation) {
                                buildExpression(sb, ((TwoVariableOperation) exp).expressions().get(0), indentation);
                                sb.append(" ").append(((TwoVariableOperation) exp).operator()).append(" ");
                                buildExpression(sb, ((TwoVariableOperation) exp).expressions().get(1), indentation);
                        } else throw new LtBug("unknown operation type " + exp.getClass());
                } else if (exp instanceof AST.Access) {
                        if (((AST.Access) exp).exp != null) {
                                buildExpression(sb, ((AST.Access) exp).exp, indentation);
                                sb.append(".");
                        }
                        sb.append(((AST.Access) exp).name);
                } else if (exp instanceof AST.AnnoExpression) {
                        err.SyntaxException("JavaScript don't support annotations", exp.line_col());
                } else if (exp instanceof AST.ArrayExp) {
                        sb.append("[");
                        buildArguments(sb, ((AST.ArrayExp) exp).list, indentation);
                        sb.append("]");
                } else if (exp instanceof AST.Assignment) {
                        buildExpression(sb, ((AST.Assignment) exp).assignTo, indentation);
                        sb.append(" = ");
                        buildExpression(sb, ((AST.Assignment) exp).assignFrom, indentation);
                } else if (exp instanceof AST.GeneratorSpec) {
                        err.SyntaxException("JavaScript don't support generator specifying", exp.line_col());
                } else if (exp instanceof AST.AsType) {
                        err.SyntaxException("JavaScript don't support type", exp.line_col());
                } else if (exp instanceof AST.Procedure) {
                        sb.append("(function(){\n");
                        buildStatements(sb, ((AST.Procedure) exp).statements, indentation + INDENT);
                        sb.append("})()");
                } else if (exp instanceof AST.Index) {
                        buildExpression(sb, ((AST.Index) exp).exp, indentation);
                        for (Expression e : ((AST.Index) exp).args) {
                                sb.append("[");
                                buildExpression(sb, e, indentation);
                                sb.append("]");
                        }
                } else if (exp instanceof AST.Invocation) {
                        if (((AST.Invocation) exp).invokeWithNames) {
                                err.SyntaxException("JavaScript don't support invoke with name", exp.line_col());
                        }
                        buildExpression(sb, ((AST.Invocation) exp).exp, indentation);
                        sb.append("(");
                        buildArguments(sb, ((AST.Invocation) exp).args, indentation);
                        sb.append(")");
                } else if (exp instanceof AST.Lambda) {
                        // check last statement
                        AST.Lambda l = (AST.Lambda) exp;
                        if (!l.statements.isEmpty()) {
                                Statement stmt = l.statements.get(l.statements.size() - 1);
                                if (stmt instanceof Expression) {
                                        l.statements.set(l.statements.size() - 1, new AST.Return((Expression) stmt, stmt.line_col()));
                                }
                        }

                        sb.append("function(");
                        buildParameters(sb, ((AST.Lambda) exp).params);
                        sb.append(") {\n");
                        buildStatements(sb, ((AST.Lambda) exp).statements, indentation + INDENT);
                        sb.append("}");
                } else if (exp instanceof AST.MapExp) {
                        sb.append("{\n");
                        boolean isFirst = true;
                        for (Map.Entry<Expression, Expression> entry : ((AST.MapExp) exp).map.entrySet()) {
                                if (isFirst) {
                                        isFirst = false;
                                } else {
                                        sb.append(",\n");
                                }
                                buildIndentation(sb, indentation + INDENT);
                                buildExpression(sb, entry.getKey(), indentation + INDENT);
                                sb.append(" : ");
                                buildExpression(sb, entry.getValue(), indentation + INDENT);
                        }
                        sb.append("\n");
                        buildIndentation(sb, indentation);
                        sb.append("}");
                } else if (exp instanceof AST.New) {
                        sb.append("new ");
                        buildExpression(sb, ((AST.New) exp).invocation, indentation);
                } else if (exp instanceof AST.Null) {
                        sb.append("null");
                } else if (exp instanceof AST.PackageRef) {
                        err.SyntaxException("JavaScript don't support packages", exp.line_col());
                        sb.append("'(compile error)'");
                } else if (exp instanceof AST.Require) {
                        sb.append("require(");
                        buildExpression(sb, ((AST.Require) exp).required, indentation);
                        sb.append(")");
                } else if (exp instanceof AST.TypeOf) {
                        err.SyntaxException("JavaScript don't have type", exp.line_col());
                        sb.append("'compile error'");
                } else throw new LtBug("unknown token " + exp);
        }

        /**
         * build method/function parameters
         *
         * @param sb     string builder
         * @param params parameters
         * @throws SyntaxException exception
         */
        private void buildParameters(StringBuilder sb, List<VariableDef> params) throws SyntaxException {
                boolean isFirst = true;
                for (VariableDef v : params) {
                        assertNoAnno(v.getAnnos());
                        assertNoModifier(v.getModifiers());
                        assertNoType(v.getType());

                        if (isFirst) {
                                isFirst = false;
                        } else {
                                sb.append(", ");
                        }
                        sb.append(v.getName());
                }
        }

        /**
         * build arguments
         *
         * @param sb          string builder
         * @param args        arguments
         * @param indentation indentation
         * @throws SyntaxException exception
         */
        private void buildArguments(StringBuilder sb, List<Expression> args, int indentation) throws SyntaxException {
                boolean isFirst = true;
                for (Expression e : args) {
                        if (isFirst) {
                                isFirst = false;
                        } else {
                                sb.append(", ");
                        }
                        buildExpression(sb, e, indentation);
                }
        }

        /**
         * build statements for default values
         *
         * @param sb          string builder
         * @param params      parameters
         * @param indentation indentation
         * @throws SyntaxException exception
         */
        private void buildDefaultValues(StringBuilder sb, List<VariableDef> params, int indentation) throws SyntaxException {
                for (VariableDef v : params) {
                        if (v.getInit() != null) {
                                buildIndentation(sb, indentation);
                                sb.append(v.getName()).append(" = ").append(v.getName()).append(" ? ").append(v.getName()).append(" : ");
                                buildExpression(sb, v.getInit(), indentation);
                                sb.append(";\n");
                        }
                }
        }

        /**
         * build a class definition.
         *
         * @param sb          string builder
         * @param classDef    class definition
         * @param indentation indentation
         * @throws SyntaxException exception
         */
        private void buildClass(StringBuilder sb, ClassDef classDef, int indentation) throws SyntaxException {
                assertNoAnno(classDef.annos);
                assertNoModifier(classDef.modifiers);
                if (classDef.superWithInvocation != null)
                        err.SyntaxException("JavaScript don't have parent classes", classDef.superWithInvocation.line_col());
                if (!classDef.superWithoutInvocation.isEmpty())
                        err.SyntaxException("JavaScript don't have parent classes or interfaces", classDef.superWithoutInvocation.get(0).line_col());

                // function xxx(
                sb.append("function ").append(classDef.name).append("(");
                // parameters
                buildParameters(sb, classDef.params);
                // ) {
                sb.append(") {\n");
                // default values
                buildDefaultValues(sb, classDef.params, indentation + INDENT);
                // statements
                buildStatements(sb, classDef.statements, indentation + INDENT);
                // set fields
                for (VariableDef v : classDef.params) {
                        buildIndentation(sb, indentation + INDENT);
                        sb.append("this.").append(v.getName()).append(" = ").append(v.getName()).append(";\n");
                }
                sb.append("}\n");
        }

        /**
         * build a function
         *
         * @param sb          sb
         * @param funDef      function definition
         * @param indentation indentation
         * @throws SyntaxException exception
         */
        private void buildFun(StringBuilder sb, FunDef funDef, int indentation) throws SyntaxException {
                assertNoAnno(funDef.annos);

                // function xxx(
                buildIndentation(sb, indentation);
                sb.append("function ").append(funDef.name).append("(");
                // parameters
                buildParameters(sb, funDef.params);
                // ) {
                buildIndentation(sb, indentation);
                sb.append(") {\n");
                // statements
                buildStatements(sb, funDef.statements, indentation + INDENT);
                // }
                buildIndentation(sb, indentation);
                sb.append("}\n");
        }

        /**
         * build a method
         *
         * @param sb          sb
         * @param methodDef   method definition
         * @param indentation indentation
         * @throws SyntaxException exception
         */
        private void buildMethod(StringBuilder sb, MethodDef methodDef, int indentation) throws SyntaxException {
                assertNoAnno(methodDef.annos);
                assertNoType(methodDef.returnType);
                assertNoModifier(methodDef.modifiers);

                // this.xxx = function(
                buildIndentation(sb, indentation);
                if (indentation == 0) {
                        sb.append("function ").append(methodDef.name).append("(");
                } else {
                        sb.append("this.").append(methodDef.name).append(" = function(");
                }
                // parameters
                buildParameters(sb, methodDef.params);
                // ) {
                sb.append(") {\n");
                // default values
                buildDefaultValues(sb, methodDef.params, indentation + INDENT);
                // statements
                buildStatements(sb, methodDef.body, indentation + INDENT);
                // }
                buildIndentation(sb, indentation);
                sb.append("}\n");
        }

        /**
         * build for statements
         *
         * @param sb          sb
         * @param aFor        for
         * @param indentation indentation
         * @throws SyntaxException exception
         */
        private void buildFor(StringBuilder sb, AST.For aFor, int indentation) throws SyntaxException {
                // for (var xx in
                buildIndentation(sb, indentation);
                sb.append("for (var ").append(aFor.name).append(" in ");
                // expression
                buildExpression(sb, aFor.exp, indentation);
                // ) {
                sb.append(") {\n");
                // statements
                buildStatements(sb, aFor.body, indentation + INDENT);
                // }
                buildIndentation(sb, indentation);
                sb.append("}\n");
        }

        /**
         * build if statement
         *
         * @param sb          sb
         * @param anIf        if
         * @param indentation indentation
         * @throws SyntaxException exception
         */
        private void buildIf(StringBuilder sb, AST.If anIf, int indentation) throws SyntaxException {
                boolean isFirst = true;
                for (AST.If.IfPair pair : anIf.ifs) {
                        if (isFirst) {
                                isFirst = false;
                                // if (
                                buildIndentation(sb, indentation);
                                sb.append("if (");
                        } else {
                                if (pair.condition == null) {
                                        // else
                                        sb.append("else");
                                } else {
                                        // else if (
                                        sb.append("else if (");
                                }
                        }
                        if (pair.condition != null) {
                                // expressions)
                                buildExpression(sb, pair.condition, indentation);
                                sb.append(")");
                        }
                        // {
                        sb.append(" {\n");
                        // statements
                        buildStatements(sb, pair.body, indentation + INDENT);
                        // }
                        buildIndentation(sb, indentation);
                        sb.append("} ");
                }
                sb.append("\n");
        }
}
