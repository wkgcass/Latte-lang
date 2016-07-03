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

package lt.compiler.syntactic;

import lt.compiler.LineCol;
import lt.compiler.syntactic.def.VariableDef;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * a file containing all node definitions of the ast tree<br>
 */
public class AST {
        private AST() {
        }

        /**
         * access
         */
        public static class Access implements Expression {
                public final Expression exp;
                public final String name;
                private final LineCol lineCol;

                public Access(Expression exp, String name, LineCol lineCol) {
                        this.exp = exp;
                        this.name = name;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        return "(" + (exp == null ? "" : exp) + ((exp != null && name != null) ? "." : "") + (name == null ? "" : name) + ")";
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        Access access = (Access) o;

                        return !(exp != null ? !exp.equals(access.exp) : access.exp != null) && !(name != null ? !name.equals(access.name) : access.name != null);
                }

                @Override
                public int hashCode() {
                        int result = exp != null ? exp.hashCode() : 0;
                        result = 31 * result + (name != null ? name.hashCode() : 0);
                        return result;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * annotation instance
         */
        public static class Anno implements Statement {
                public final Access anno;
                public final List<Assignment> args;
                private final LineCol lineCol;

                public Anno(Access anno, List<Assignment> args, LineCol lineCol) {
                        this.anno = anno;
                        this.args = args;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        StringBuilder sb = new StringBuilder("(@");
                        sb.append(anno);
                        if (!args.isEmpty()) {
                                sb.append("(");
                                boolean isFirst = true;
                                for (Assignment a : args) {
                                        if (isFirst) {
                                                isFirst = false;
                                        } else {
                                                sb.append(",");
                                        }
                                        sb.append(a);
                                }
                                sb.append(")");
                        }
                        sb.append(")");
                        return sb.toString();
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        Anno anno1 = (Anno) o;

                        if (!anno.equals(anno1.anno)) return false;
                        //
                        return args.equals(anno1.args);
                }

                @Override
                public int hashCode() {
                        int result = anno.hashCode();
                        result = 31 * result + args.hashCode();
                        return result;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * annotation as expression
         */
        public static class AnnoExpression implements Expression {
                public final Anno anno;

                public AnnoExpression(Anno anno) {
                        this.anno = anno;
                }

                @Override
                public LineCol line_col() {
                        return anno.line_col();
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        AnnoExpression that = (AnnoExpression) o;

                        return anno != null ? anno.equals(that.anno) : that.anno == null;
                }

                @Override
                public int hashCode() {
                        return anno != null ? anno.hashCode() : 0;
                }

                @Override
                public String toString() {
                        return anno.toString();
                }
        }

        /**
         * array expression
         */
        public static class ArrayExp implements Expression {
                public final List<Expression> list;
                private final LineCol lineCol;

                public ArrayExp(List<Expression> list, LineCol lineCol) {
                        this.list = list;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        StringBuilder sb = new StringBuilder("[");
                        boolean isFirst = true;
                        for (Expression e : list) {
                                if (isFirst) {
                                        isFirst = false;
                                } else {
                                        sb.append(",");
                                }
                                sb.append(e);
                        }
                        sb.append("]");
                        return sb.toString();
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        ArrayExp arrayExp = (ArrayExp) o;

                        return list.equals(arrayExp.list);
                }

                @Override
                public int hashCode() {
                        return list.hashCode();
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * assignment assignTo = assignFrom
         */
        public static class Assignment implements Expression {
                public final Access assignTo;
                public final String op;
                public final Expression assignFrom;
                private final LineCol lineCol;

                public Assignment(Access assignTo, String op, Expression assignFrom, LineCol lineCol) {
                        this.assignTo = assignTo;
                        this.op = op;
                        this.assignFrom = assignFrom;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        return "Assignment(" + assignTo + " " + op + " " + assignFrom + ")";
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        Assignment that = (Assignment) o;

                        if (!assignTo.equals(that.assignTo)) return false;
                        if (!op.equals(that.op)) return false;
                        //
                        return assignFrom.equals(that.assignFrom);
                }

                @Override
                public int hashCode() {
                        int result = assignTo.hashCode();
                        result = 31 * result + op.hashCode();
                        result = 31 * result + assignFrom.hashCode();
                        return result;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * as type<br>
         * exp as Type
         */
        public static class AsType implements Expression {
                public final Expression exp;
                public final Access type;
                private final LineCol lineCol;

                public AsType(Expression exp, Access type, LineCol lineCol) {
                        this.exp = exp;
                        this.type = type;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        return "(" + exp + " as " + type + ")";
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        AsType asType = (AsType) o;

                        if (!exp.equals(asType.exp)) return false;
                        //
                        return type.equals(asType.type);
                }

                @Override
                public int hashCode() {
                        int result = exp.hashCode();
                        result = 31 * result + type.hashCode();
                        return result;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * procedure
         */
        public static class Procedure implements Expression {
                public final List<Statement> statements;
                private final LineCol lineCol;

                public Procedure(List<Statement> statements, LineCol lineCol) {
                        this.statements = statements;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        return "(" + statements + ")";
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        Procedure procedure = (Procedure) o;

                        return statements.equals(procedure.statements);
                }

                @Override
                public int hashCode() {
                        return statements.hashCode();
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * for expression
         */
        public static class For implements Statement {
                public final String name;
                public final Expression exp;
                public final List<Statement> body;
                private final LineCol lineCol;

                public For(String name, Expression exp, List<Statement> body, LineCol lineCol) {
                        this.name = name;
                        this.exp = exp;
                        this.body = body;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        return "(" + "for " + name + " @ " + exp + " " + body + ")";
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        For aFor = (For) o;

                        return !(name != null ? !name.equals(aFor.name) : aFor.name != null) && !(exp != null ? !exp.equals(aFor.exp) : aFor.exp != null) && !(body != null ? !body.equals(aFor.body) : aFor.body != null);
                }

                @Override
                public int hashCode() {
                        int result = name != null ? name.hashCode() : 0;
                        result = 31 * result + (exp != null ? exp.hashCode() : 0);
                        result = 31 * result + (body != null ? body.hashCode() : 0);
                        return result;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * if exp
         */
        public static class If implements Statement {
                public static class IfPair {
                        public final Expression condition;
                        public final List<Statement> body;
                        public final LineCol lineCol;

                        public IfPair(Expression condition, List<Statement> body, LineCol lineCol) {
                                this.body = body;
                                this.condition = condition;
                                this.lineCol = lineCol;
                        }

                        @Override
                        public boolean equals(Object o) {
                                if (this == o) return true;
                                if (o == null || getClass() != o.getClass()) return false;

                                IfPair pair = (IfPair) o;

                                return !(condition != null ? !condition.equals(pair.condition) : pair.condition != null) && !(body != null ? !body.equals(pair.body) : pair.body != null);
                        }

                        @Override
                        public int hashCode() {
                                int result = condition != null ? condition.hashCode() : 0;
                                result = 31 * result + (body != null ? body.hashCode() : 0);
                                return result;
                        }
                }

                public List<IfPair> ifs;
                private final LineCol lineCol;

                public If(List<IfPair> ifs, LineCol lineCol) {
                        this.ifs = ifs;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        StringBuilder sb = new StringBuilder("(");

                        boolean isFirst = true;
                        for (IfPair pair : ifs) {
                                if (isFirst) {
                                        isFirst = false;
                                        sb.append("(if (").append(pair.condition).append(")").append(pair.body).append(")");
                                } else {
                                        if (pair.condition == null) {
                                                sb.append("(else ").append(pair.body).append(")");
                                        } else {
                                                sb.append("(elseif (").append(pair.condition).append(")").append(pair.body).append(")");
                                        }
                                }
                        }

                        sb.append(")");
                        return sb.toString();
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        If anIf = (If) o;

                        return !(ifs != null ? !ifs.equals(anIf.ifs) : anIf.ifs != null);

                }

                @Override
                public int hashCode() {
                        return ifs != null ? ifs.hashCode() : 0;
                }
        }

        /**
         * index access
         */
        public static class Index implements Expression {
                public final Expression exp;
                public final List<Expression> args;
                private final LineCol lineCol;

                public Index(Expression exp, List<Expression> args, LineCol lineCol) {
                        this.exp = exp;
                        this.args = args;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        StringBuilder sb = new StringBuilder("(").append(exp).append("[");
                        boolean isFirst = true;
                        for (Expression e : args) {
                                if (isFirst) {
                                        isFirst = false;
                                } else {
                                        sb.append(",");
                                }
                                sb.append(e);
                        }
                        sb.append("]").append(")");
                        return sb.toString();
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        Index index = (Index) o;

                        if (!exp.equals(index.exp)) return false;
                        //
                        return args.equals(index.args);
                }

                @Override
                public int hashCode() {
                        int result = exp.hashCode();
                        result = 31 * result + args.hashCode();
                        return result;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * invocation of methods/constructors
         */
        public static class Invocation implements Expression {
                public final Access access;
                public final List<Expression> args;
                public final boolean invokeWithNames;
                private final LineCol lineCol;

                public Invocation(Access access, List<Expression> args, boolean invokeWithNames, LineCol lineCol) {
                        this.access = access;
                        this.invokeWithNames = invokeWithNames;
                        this.lineCol = lineCol;
                        this.args = args;
                }

                @Override
                public String toString() {
                        StringBuilder sb = new StringBuilder("Invocation(");
                        sb.append(access);
                        sb.append("(");
                        boolean isFirst = true;
                        for (Expression e : args) {
                                if (isFirst) {
                                        isFirst = false;
                                } else {
                                        sb.append(",");
                                }
                                sb.append(e);
                        }
                        sb.append(")");
                        sb.append(")");
                        return sb.toString();
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        Invocation that = (Invocation) o;

                        if (invokeWithNames != that.invokeWithNames) return false;
                        if (access != null ? !access.equals(that.access) : that.access != null) return false;
                        //
                        return !(args != null ? !args.equals(that.args) : that.args != null);
                }

                @Override
                public int hashCode() {
                        int result = access != null ? access.hashCode() : 0;
                        result = 31 * result + (args != null ? args.hashCode() : 0);
                        result = 31 * result + (invokeWithNames ? 1 : 0);
                        return result;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * lambda expression
         */
        public static class Lambda implements Expression {
                public final List<VariableDef> params;
                public final List<Statement> statements;
                private final LineCol lineCol;

                public Lambda(List<VariableDef> params, List<Statement> statements, LineCol lineCol) {
                        this.params = params;
                        this.statements = statements;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        StringBuilder sb = new StringBuilder("Lambda((");
                        boolean isFirst = true;
                        for (VariableDef v : params) {
                                if (isFirst) {
                                        isFirst = false;
                                } else {
                                        sb.append(",");
                                }
                                sb.append(v);
                        }
                        sb.append(")=>").append(statements).append(")");
                        return sb.toString();
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        Lambda lambda = (Lambda) o;

                        return !(params != null ? !params.equals(lambda.params) : lambda.params != null) && !(statements != null ? !statements.equals(lambda.statements) : lambda.statements != null);
                }

                @Override
                public int hashCode() {
                        int result = params != null ? params.hashCode() : 0;
                        result = 31 * result + (statements != null ? statements.hashCode() : 0);
                        return result;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * map expression
         */
        public static class MapExp implements Expression {
                public final LinkedHashMap<Expression, Expression> map;
                private final LineCol lineCol;

                public MapExp(LinkedHashMap<Expression, Expression> map, LineCol lineCol) {
                        this.map = map;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        StringBuilder sb = new StringBuilder("{");
                        boolean isFirst = true;
                        for (Expression e : map.keySet()) {
                                if (isFirst) {
                                        isFirst = false;
                                } else {
                                        sb.append(",");
                                }
                                sb.append(e).append(":").append(map.get(e));
                        }
                        sb.append("}");
                        return sb.toString();
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        MapExp mapExp = (MapExp) o;

                        return map.equals(mapExp.map);

                }

                @Override
                public int hashCode() {
                        return map.hashCode();
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * new
         */
        public static class New implements Expression {
                public final Invocation invocation;
                private final LineCol lineCol;

                public New(Invocation invocation, LineCol lineCol) {
                        this.invocation = invocation;
                        this.lineCol = lineCol;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        New aNew = (New) o;

                        return invocation.equals(aNew.invocation);
                }

                @Override
                public int hashCode() {
                        return invocation.hashCode();
                }
        }

        /**
         * null value
         */
        public static class Null implements Expression {
                private final LineCol lineCol;

                public Null(LineCol lineCol) {
                        this.lineCol = lineCol;
                }

                @Override
                public boolean equals(Object obj) {
                        return obj instanceof Null;
                }

                @Override
                public int hashCode() {
                        return 0;
                }

                @Override
                public String toString() {
                        return "(null)";
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * package
         */
        public static class PackageRef implements Expression {
                public final String pkg;
                private final LineCol lineCol;

                public PackageRef(String pkg, LineCol lineCol) {
                        this.pkg = pkg;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        return "(" + pkg + ")";
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        PackageRef that = (PackageRef) o;

                        return !(pkg != null ? !pkg.equals(that.pkg) : that.pkg != null);
                }

                @Override
                public int hashCode() {
                        return pkg != null ? pkg.hashCode() : 0;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * pass
         */
        public static class Pass implements Statement {
                private final LineCol lineCol;

                public Pass(LineCol lineCol) {
                        this.lineCol = lineCol;
                }

                @Override
                public boolean equals(Object obj) {
                        return obj instanceof Pass;
                }

                @Override
                public int hashCode() {
                        return 0;
                }

                @Override
                public String toString() {
                        return "(...)";
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * require a lt file
         */
        public static class Require implements Expression {
                public final Expression required;
                private final LineCol lineCol;

                public Require(Expression required, LineCol lineCol) {
                        this.required = required;
                        this.lineCol = lineCol;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * return statement
         */
        public static class Return implements Statement {
                public final Expression exp;
                private final LineCol lineCol;

                public Return(Expression exp, LineCol lineCol) {
                        this.exp = exp;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        return "Return(" + exp + ")";
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        Return aReturn = (Return) o;

                        return !(exp != null ? !exp.equals(aReturn.exp) : aReturn.exp != null);
                }

                @Override
                public int hashCode() {
                        return exp != null ? exp.hashCode() : 0;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * static scope
         */
        public static class StaticScope implements Statement {
                public final List<Statement> statements;
                private final LineCol lineCol;

                public StaticScope(List<Statement> statements, LineCol lineCol) {
                        this.statements = statements;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        return "Static(" + statements + ")";
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        StaticScope that = (StaticScope) o;

                        return !(statements != null ? !statements.equals(that.statements) : that.statements != null);

                }

                @Override
                public int hashCode() {
                        return statements != null ? statements.hashCode() : 0;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * synchronized
         */
        public static class Synchronized implements Statement {
                public final List<Expression> toSync;
                public final List<Statement> statements;
                private final LineCol lineCol;

                public Synchronized(List<Expression> toSync, List<Statement> statements, LineCol lineCol) {
                        this.toSync = toSync;
                        this.statements = statements;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        StringBuilder sb = new StringBuilder("(sync (");
                        boolean isFirst = true;
                        for (Expression e : toSync) {
                                if (isFirst) {
                                        isFirst = false;
                                } else {
                                        sb.append(",");
                                }
                                sb.append(e);
                        }
                        sb.append(") ").append(statements).append(")");
                        return sb.toString();
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        Synchronized that = (Synchronized) o;

                        if (!toSync.equals(that.toSync)) return false;
                        //
                        return statements.equals(that.statements);
                }

                @Override
                public int hashCode() {
                        int result = toSync.hashCode();
                        result = 31 * result + statements.hashCode();
                        return result;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * throw
         */
        public static class Throw implements Statement {
                public final Expression exp;
                private final LineCol lineCol;

                public Throw(Expression exp, LineCol lineCol) {
                        this.exp = exp;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        return "(throw " + exp + ")";
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        Throw aThrow = (Throw) o;

                        return exp.equals(aThrow.exp);
                }

                @Override
                public int hashCode() {
                        return exp.hashCode();
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * try
         */
        public static class Try implements Statement {
                public final List<Statement> statements;
                public final List<Statement> catchStatements;
                public final String varName;
                public final List<Statement> fin;
                private final LineCol lineCol;

                public Try(List<Statement> statements, String varName, List<Statement> catchStatements, List<Statement> fin, LineCol lineCol) {
                        this.statements = statements;
                        this.catchStatements = catchStatements;
                        this.varName = varName;
                        this.fin = fin;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        return "(try " + statements + " catch " + varName + " " + catchStatements + " finally " + fin + ")";
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        Try aTry = (Try) o;

                        if (statements != null ? !statements.equals(aTry.statements) : aTry.statements != null)
                                return false;
                        if (catchStatements != null ? !catchStatements.equals(aTry.catchStatements) : aTry.catchStatements != null)
                                return false;
                        if (varName != null ? !varName.equals(aTry.varName) : aTry.varName != null) return false;
                        //
                        return !(fin != null ? !fin.equals(aTry.fin) : aTry.fin != null);
                }

                @Override
                public int hashCode() {
                        int result = statements != null ? statements.hashCode() : 0;
                        result = 31 * result + (catchStatements != null ? catchStatements.hashCode() : 0);
                        result = 31 * result + (varName != null ? varName.hashCode() : 0);
                        result = 31 * result + (fin != null ? fin.hashCode() : 0);
                        return result;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * type
         */
        public static class TypeOf implements Expression {
                public final Access type;
                private final LineCol lineCol;

                public TypeOf(Access type, LineCol lineCol) {
                        this.type = type;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        return "(type " + type + ")";
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        TypeOf typeOf = (TypeOf) o;

                        return type.equals(typeOf.type);
                }

                @Override
                public int hashCode() {
                        return type.hashCode();
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * undefined expression
         */
        public static class UndefinedExp implements Expression {
                private final LineCol lineCol;

                public UndefinedExp(LineCol lineCol) {
                        this.lineCol = lineCol;
                }

                @Override
                public boolean equals(Object obj) {
                        return this == obj || obj instanceof UndefinedExp;
                }

                @Override
                public int hashCode() {
                        return 0;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * while
         */
        public static class While implements Statement {
                public final Expression condition;
                public final List<Statement> statements;
                public final boolean doWhile;
                private final LineCol lineCol;

                public While(Expression condition, List<Statement> statements, boolean doWhile, LineCol lineCol) {
                        this.condition = condition;
                        this.statements = statements;
                        this.doWhile = doWhile;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        StringBuilder sb = new StringBuilder("(");
                        if (doWhile) {
                                sb.append("do ").append(statements).append(" while ").append(condition);
                        } else {
                                sb.append("while ").append(condition).append(" ").append(statements);
                        }
                        sb.append(")");
                        return sb.toString();
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        While aWhile = (While) o;

                        if (doWhile != aWhile.doWhile) return false;
                        if (!condition.equals(aWhile.condition)) return false;
                        //
                        return statements.equals(aWhile.statements);
                }

                @Override
                public int hashCode() {
                        int result = condition.hashCode();
                        result = 31 * result + statements.hashCode();
                        result = 31 * result + (doWhile ? 1 : 0);
                        return result;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * continue statement
         */
        public static class Continue implements Statement {
                private final LineCol lineCol;

                public Continue(LineCol lineCol) {
                        this.lineCol = lineCol;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }

        /**
         * break statement
         */
        public static class Break implements Statement {
                private final LineCol lineCol;

                public Break(LineCol lineCol) {
                        this.lineCol = lineCol;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }
        }
}
