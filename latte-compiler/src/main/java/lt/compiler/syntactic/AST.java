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
import lt.compiler.syntactic.pre.Modifier;
import lt.lang.function.Function2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static lt.compiler.CompileUtil.visitStmt;

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
                public final List<Access> generics = new ArrayList<Access>(); // len = 0 means no generic
                private final LineCol lineCol;

                public Access(Expression exp, String name, LineCol lineCol) {
                        this.exp = exp;
                        this.name = name;
                        this.lineCol = lineCol;
                }

                @Override
                public String toString() {
                        return "(" + (exp == null ? "" : exp) + ((exp != null && name != null) ? "." : "") + (name == null ? "" : name) + (generics.size() == 0 ? "" : "<:" + generics.toString() + ":>") + ")";
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        Access access = (Access) o;

                        return (exp != null ? exp.equals(access.exp) : access.exp == null) && (name != null ? name.equals(access.name) : access.name == null) && generics.equals(access.generics);
                }

                @Override
                public int hashCode() {
                        int result = exp != null ? exp.hashCode() : 0;
                        result = 31 * result + (name != null ? name.hashCode() : 0);
                        result = 31 * result + generics.hashCode();
                        return result;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(exp, f, t);
                        visitStmt(generics, f, t);
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(anno, f, t);
                        visitStmt(args, f, t);
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
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(anno, f, t);
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(list, f, t);
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(assignTo, f, t);
                        visitStmt(assignFrom, f, t);
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(exp, f, t);
                        visitStmt(type, f, t);
                }
        }

        public static class Destruct implements Expression {
                public final Set<Modifier> modifiers;
                public final Set<Anno> annos;
                public final Pattern_Destruct pattern;
                public final Expression exp;
                private final LineCol lineCol;

                public Destruct(Set<Modifier> modifiers, Set<Anno> annos, Pattern_Destruct pattern, Expression exp, LineCol lineCol) {
                        this.modifiers = modifiers;
                        this.annos = annos;
                        this.pattern = pattern;
                        this.exp = exp;
                        this.lineCol = lineCol;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(modifiers, f, t);
                        visitStmt(annos, f, t);
                        visitStmt(pattern, f, t);
                        visitStmt(exp, f, t);
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        Destruct destruct = (Destruct) o;

                        if (!modifiers.equals(destruct.modifiers)) return false;
                        if (!annos.equals(destruct.annos)) return false;
                        if (!pattern.equals(destruct.pattern)) return false;
                        //
                        return exp.equals(destruct.exp);

                }

                @Override
                public int hashCode() {
                        int result = modifiers.hashCode();
                        result = 31 * result + annos.hashCode();
                        result = 31 * result + pattern.hashCode();
                        result = 31 * result + exp.hashCode();
                        return result;
                }

                @Override
                public String toString() {
                        return "(" + annos + " " + modifiers + " " + pattern + " <- " + exp + ')';
                }
        }

        /**
         * pattern
         */
        public abstract static class Pattern implements Statement, Serializable {
                public final PatternType patternType;

                public Pattern(PatternType patternType) {
                        this.patternType = patternType;
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        Pattern pattern = (Pattern) o;

                        return patternType == pattern.patternType;

                }

                @Override
                public int hashCode() {
                        return patternType.hashCode();
                }

                @Override
                public String toString() {
                        return "Pattern(" + patternType + ")";
                }

                @Override
                public LineCol line_col() {
                        throw new UnsupportedOperationException();
                }
        }

        public static class Pattern_Default extends Pattern {
                private static final Pattern_Default def = new Pattern_Default();

                public static Pattern_Default get() {
                        return def;
                }

                private Pattern_Default() {
                        super(PatternType.DEFAULT);
                }

                @Override
                public String toString() {
                        return "(_)";
                }

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        // nothing to visit
                }
        }

        /**
         * something like
         * case _:XXX
         */
        public static class Pattern_Type extends Pattern {
                public final Access type;

                public Pattern_Type(Access type) {
                        super(PatternType.TYPE);
                        this.type = type;
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;
                        if (!super.equals(o)) return false;

                        Pattern_Type that = (Pattern_Type) o;

                        return type.equals(that.type);

                }

                @Override
                public int hashCode() {
                        int result = super.hashCode();
                        result = 31 * result + type.hashCode();
                        return result;
                }

                @Override
                public String toString() {
                        return "(_:" + type + ")";
                }

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(type, f, t);
                }
        }

        /**
         * explicit value
         */
        public static class Pattern_Value extends Pattern {
                public final Expression exp;

                public Pattern_Value(Expression exp) {
                        super(PatternType.VALUE);
                        this.exp = exp;
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;
                        if (!super.equals(o)) return false;

                        Pattern_Value that = (Pattern_Value) o;

                        return exp.equals(that.exp);

                }

                @Override
                public int hashCode() {
                        int result = super.hashCode();
                        result = 31 * result + exp.hashCode();
                        return result;
                }

                @Override
                public String toString() {
                        return "(" + exp + ")";
                }

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(exp, f, t);
                }
        }

        /**
         * destruct:
         * case Bean(_, a, 1)
         */
        public static class Pattern_Destruct extends Pattern {
                public final Access type;
                public final List<Pattern> subPatterns;

                public Pattern_Destruct(Access type, List<Pattern> subPatterns) {
                        super(PatternType.DESTRUCT);
                        this.type = type;
                        this.subPatterns = subPatterns;
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;
                        if (!super.equals(o)) return false;

                        Pattern_Destruct that = (Pattern_Destruct) o;

                        if (type != null ? !type.equals(that.type) : that.type != null) return false;
                        //
                        return subPatterns.equals(that.subPatterns);

                }

                @Override
                public int hashCode() {
                        int result = super.hashCode();
                        result = 31 * result + (type != null ? type.hashCode() : 0);
                        result = 31 * result + subPatterns.hashCode();
                        return result;
                }

                @Override
                public String toString() {
                        return "(" + type + subPatterns + ")";
                }

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(type, f, t);
                        visitStmt(subPatterns, f, t);
                }
        }

        public static class Pattern_Define extends Pattern {
                public final String name;
                public final Access type;

                public Pattern_Define(String name, Access type) {
                        super(PatternType.DEFINE);
                        this.name = name;
                        this.type = type;
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;
                        if (!super.equals(o)) return false;

                        Pattern_Define that = (Pattern_Define) o;

                        if (!name.equals(that.name)) return false;
                        //
                        return type != null ? type.equals(that.type) : that.type == null;

                }

                @Override
                public int hashCode() {
                        int result = super.hashCode();
                        result = 31 * result + name.hashCode();
                        result = 31 * result + (type != null ? type.hashCode() : 0);
                        return result;
                }

                @Override
                public String toString() {
                        return "(" + name + ":" + type + ")";
                }

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(type, f, t);
                }
        }

        public enum PatternType {
                TYPE, VALUE, DESTRUCT, DEFAULT, DEFINE
        }

        public static class PatternCondition implements Statement {
                public final Pattern pattern;
                public final Expression condition;

                public PatternCondition(Pattern pattern, Expression condition) {
                        this.pattern = pattern;
                        this.condition = condition;
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        PatternCondition that = (PatternCondition) o;

                        if (!pattern.equals(that.pattern)) return false;
                        //
                        return condition != null ? condition.equals(that.condition) : that.condition == null;

                }

                @Override
                public int hashCode() {
                        int result = pattern.hashCode();
                        result = 31 * result + (condition != null ? condition.hashCode() : 0);
                        return result;
                }

                @Override
                public String toString() {
                        return "(" + pattern + " if " + condition + ")";
                }

                @Override
                public LineCol line_col() {
                        throw new UnsupportedOperationException();
                }

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(pattern, f, t);
                        visitStmt(condition, f, t);
                }
        }

        /**
         * pattern matching
         */
        public static class PatternMatching implements Expression {
                public final Expression expToMatch;
                public final LinkedHashMap<PatternCondition, List<Statement>> patternsToStatements;
                private final LineCol lineCol;

                public PatternMatching(Expression expToMatch, LinkedHashMap<PatternCondition, List<Statement>> patternsToStatements, LineCol lineCol) {
                        this.expToMatch = expToMatch;
                        this.patternsToStatements = patternsToStatements;
                        this.lineCol = lineCol;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(expToMatch, f, t);
                        visitStmt(patternsToStatements.keySet(), f, t);
                        for (List<Statement> l : patternsToStatements.values()) {
                                visitStmt(l, f, t);
                        }
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        PatternMatching that = (PatternMatching) o;

                        if (!expToMatch.equals(that.expToMatch)) return false;
                        //
                        return patternsToStatements.equals(that.patternsToStatements);

                }

                @Override
                public int hashCode() {
                        int result = expToMatch.hashCode();
                        result = 31 * result + patternsToStatements.hashCode();
                        return result;
                }

                @Override
                public String toString() {
                        return "PatternMatching(" + expToMatch +
                                ", " + patternsToStatements + ')';
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(statements, f, t);
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(exp, f, t);
                        visitStmt(body, f, t);
                }
        }

        /**
         * specifying a generator
         */
        public static class GeneratorSpec implements Expression {
                public final Access type;
                public final List<Statement> ast;
                private final LineCol lineCol;

                public GeneratorSpec(Access type, List<Statement> ast, LineCol lineCol) {
                        this.type = type;
                        this.ast = ast;
                        this.lineCol = lineCol;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(type, f, t);
                        visitStmt(ast, f, t);
                }

                @Override
                public boolean equals(Object o) {
                        if (this == o) return true;
                        if (o == null || getClass() != o.getClass()) return false;

                        GeneratorSpec that = (GeneratorSpec) o;

                        if (!type.equals(that.type)) return false;
                        //
                        return ast.equals(that.ast);
                }

                @Override
                public int hashCode() {
                        int result = type.hashCode();
                        result = 31 * result + ast.hashCode();
                        return result;
                }

                @Override
                public String toString() {
                        return "GenSpec#" + type + "(" + ast + ")";
                }
        }

        /**
         * if exp
         */
        public static class If implements Statement {
                public static class IfPair implements Statement {
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

                        @Override
                        public LineCol line_col() {
                                throw new UnsupportedOperationException();
                        }

                        @Override
                        public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                                visitStmt(condition, f, t);
                                visitStmt(body, f, t);
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
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(ifs, f, t);
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(exp, f, t);
                        visitStmt(args, f, t);
                }
        }

        /**
         * invocation of methods/constructors
         */
        public static class Invocation implements Expression {
                public final Expression exp;
                public final List<Expression> args;
                public final boolean invokeWithNames;
                private final LineCol lineCol;

                public Invocation(Expression exp, List<Expression> args, boolean invokeWithNames, LineCol lineCol) {
                        this.exp = exp;
                        this.invokeWithNames = invokeWithNames;
                        this.lineCol = lineCol;
                        this.args = args;
                }

                @Override
                public String toString() {
                        StringBuilder sb = new StringBuilder("Invocation(");
                        sb.append(exp);
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
                        if (exp != null ? !exp.equals(that.exp) : that.exp != null) return false;
                        //
                        return !(args != null ? !args.equals(that.args) : that.args != null);
                }

                @Override
                public int hashCode() {
                        int result = exp != null ? exp.hashCode() : 0;
                        result = 31 * result + (args != null ? args.hashCode() : 0);
                        result = 31 * result + (invokeWithNames ? 1 : 0);
                        return result;
                }

                @Override
                public LineCol line_col() {
                        return lineCol;
                }

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(exp, f, t);
                        visitStmt(args, f, t);
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
                        sb.append(")->").append(statements).append(")");
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(params, f, t);
                        visitStmt(statements, f, t);
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(map.keySet(), f, t);
                        visitStmt(map.values(), f, t);
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
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(invocation, f, t);
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        // nothing to visit
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        // nothing to visit
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        // nothing to visit
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(required, f, t);
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(exp, f, t);
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(statements, f, t);
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(toSync, f, t);
                        visitStmt(statements, f, t);
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(exp, f, t);
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(statements, f, t);
                        visitStmt(catchStatements, f, t);
                        visitStmt(fin, f, t);
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(type, f, t);
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        visitStmt(condition, f, t);
                        visitStmt(statements, f, t);
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        // do nothing
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

                @Override
                public <T> void foreachInnerStatements(Function2<Boolean, ? super Statement, T> f, T t) throws Exception {
                        // do nothing
                }
        }
}
