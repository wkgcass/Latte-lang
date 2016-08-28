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

package lt.compiler;

import lt.compiler.lexical.*;
import lt.compiler.syntactic.def.*;
import lt.compiler.syntactic.literal.BoolLiteral;
import lt.compiler.syntactic.literal.NumberLiteral;
import lt.compiler.syntactic.literal.RegexLiteral;
import lt.compiler.syntactic.literal.StringLiteral;
import lt.compiler.syntactic.operation.TwoVariableOperation;
import lt.compiler.syntactic.pre.Import;
import lt.compiler.syntactic.pre.Modifier;
import lt.compiler.syntactic.pre.PackageDeclare;
import lt.compiler.syntactic.*;
import lt.compiler.syntactic.operation.OneVariableOperation;
import lt.compiler.syntactic.operation.UnaryOneVariableOperation;

import java.util.*;

import static lt.compiler.CompileUtil.*;

/**
 * syntactic processor
 */
public class Parser {
        /**
         * parse fail is used to skip current statement.
         */
        private class ParseFail extends RuntimeException {
        }

        /**
         * current node
         */
        private Node current;
        /**
         * parsed expression stack
         */
        private Stack<Expression> parsedExps = new Stack<>();
        /**
         * 2 variable operators
         */
        private Stack<String> last2VarOps = new Stack<>();
        /**
         * 1 variable operators (unary)
         */
        private Stack<String> last1VarUnaryOps = new Stack<>();
        /**
         * the names that's already used
         */
        private Set<String> usedVarNames = new HashSet<>();
        /**
         * currently parsed modifiers
         */
        private Set<Modifier> modifiers = new HashSet<>();
        /**
         * currently parsed annotations
         */
        private Set<AST.Anno> annos = new HashSet<>();

        /**
         * <b>state</b>.<br>
         * when parsing an expression and reaching a start node, if this variable is true, return normally. else throw exception
         */
        private boolean expectingStartNode = false;
        /**
         * <b>state</b>.<br>
         * is parsing map literal
         */
        private boolean isParsingMap = false;
        /**
         * <b>state</b>.<br>
         * is parsing operator like invocation
         */
        private boolean isParsingOperatorLikeInvocation = false;
        /**
         * <b>state</b>.<br>
         * consider annotations as values
         */
        private boolean annotationAsExpression = false;
        /**
         * error manager
         */
        private final ErrorManager err;

        /**
         * init the syntactic processor with element start node
         *
         * @param root root node
         * @param err  error manager
         */
        public Parser(ElementStartNode root, ErrorManager err) {
                this.current = root.getLinkedNode();
                this.err = err;
        }

        /**
         * add variable names that's already used(must be invoked by parent parser)
         *
         * @param names names
         */
        private void addUsedVarNames(Set<String> names) {
                this.usedVarNames.addAll(names);
        }

        /**
         * jump to the end of the statement
         *
         * @throws SyntaxException compiling error
         */
        private void jumpToTheNearestEndingNode() throws SyntaxException {
                while (current != null && (!(current instanceof EndingNode))) {
                        nextNode(true);
                }
        }

        /**
         * parse the nodes into a list of statements
         *
         * @return a list of statements
         * @throws SyntaxException compiling errors
         */
        public List<Statement> parse() throws SyntaxException {
                List<Statement> list = new ArrayList<>(); // the result list
                while (true) {
                        if (isParsingMap) {
                                annosIsEmpty();
                                modifiersIsEmpty();

                                // specially handled : map literal
                                if (current == null) break;
                                LineCol lineCol = current.getLineCol();

                                parse_expression(); // get key (one before ':')

                                boolean continueParsing = true;

                                if (parsedExps.empty()) {
                                        err.SyntaxException("key is not set", current == null ? lineCol : current.getLineCol());
                                        jumpToTheNearestEndingNode();
                                        nextNode(true);
                                        continueParsing = false;
                                } else {
                                        Expression key = parsedExps.pop(); // the key is later pushed into the stack
                                        list.add(key);
                                }

                                if (!continueParsing) continue;


                                if (null == current || current.next() instanceof EndingNode || (current.next() instanceof Element && ((Element) current.next()).getContent().equals(","))) {
                                        err.SyntaxException("value is not set", current == null ? lineCol : current.getLineCol());
                                        list.remove(list.size() - 1);
                                        nextNode(true);
                                        if (current instanceof EndingNode) nextNode(true);
                                        continue;
                                }
                                expecting(":", current.previous(), current, err);
                                nextNode(false);

                                parse_expression(); // get value (one after ':')
                                if (parsedExps.empty()) {
                                        err.SyntaxException("value is not set", current == null ? lineCol : current.getLineCol());
                                        jumpToTheNearestEndingNode();
                                        list.remove(list.size() - 1);
                                } else {
                                        Expression value = parsedExps.pop();
                                        list.add(value);
                                }

                                // add k,v into list, and return.
                                // parse_map would take the list[0,2,4,6,8,...] as key, and list[1,3,5,7,9,...] as value

                                nextNode(true);

                                assert last1VarUnaryOps.empty();
                                last2VarOps.clear();
                        } else {
                                // common handling process
                                Statement stmt;
                                try {
                                        stmt = parse_statement(); // invoke parse_statement to get one statement
                                } catch (ParseFail ignore) {
                                        jumpToTheNearestEndingNode();
                                        continue;
                                } catch (SyntaxException e) {
                                        err.SyntaxException(e.msg, e.lineCol);
                                        jumpToTheNearestEndingNode();
                                        continue;
                                }
                                if (current == null && stmt == null)
                                        break; // break when reaching the end of nodes
                                if (!parsedExps.empty()) {
                                        // check parsedExps stack
                                        // the stack should be empty because the parse_statement() method would
                                        // take the existing expression from the stack
                                        // and the stack should finally be filled with only one expression after
                                        // invoking parse_expression()

                                        // generate exception cause message
                                        LineCol theFirstLineCol = null;
                                        StringBuilder sb = new StringBuilder();
                                        for (Expression e : parsedExps) {
                                                sb.append(e.toString()).append(" at ").append(e.line_col().fileName).append("(").append(e.line_col().line).append(",").append(e.line_col().column).append(")\n");
                                                if (theFirstLineCol == null)
                                                        theFirstLineCol = e.line_col();
                                        }
                                        // it should be a bug
                                        // this err only appears only because parse_x methods don't correctly stop the parsing process when meet an invalid input
                                        err.SyntaxException("got tokens which are no where to place\n" + sb + "the parsed statement is \n" + stmt + "\n", theFirstLineCol);
                                        // ignore these tokens
                                        parsedExps.clear();
                                }

                                // these operators must be empty
                                assert last1VarUnaryOps.empty();
                                // this stack may not be empty.
                                // the peek of stack is used to determine whether to return or proceed when meeting another 2 var operator
                                last2VarOps.clear();

                                if (stmt != null)
                                        list.add(stmt);
                                nextNode(true);
                        }
                }
                return list;
        }

        /**
         * go to next node.<br>
         * {@link #current} would be set to the next node<br>
         * if the canBeEnd is set to <i>false</i>, an EndingNode.WEAK would be ignored or an EndingNode.STRONG would throw exception
         *
         * @param canBeEnd whether the next node can be an end
         * @throws SyntaxException compiling error thrown if meets an end but cannot be end
         */
        private void nextNode(boolean canBeEnd) throws SyntaxException {
                if (current == null) {
                        if (canBeEnd) {
                                return;
                        } else {
                                throw new LtBug("if canBeEnd is false, then current shouldn't be null");
                        }
                }
                Node next = current.next();
                if (next == null || (next instanceof EndingNode && ((EndingNode) next).getType() == EndingNode.STRONG)) {
                        if (!canBeEnd) {
                                LineCol lineCol = new LineCol(
                                        current.getLineCol().fileName,
                                        current.getLineCol().line,
                                        current.getLineCol().column + current.getLineCol().length);
                                err.UnexpectedEndException(lineCol);
                                // if it's not the last node, jump this token
                                // if it's the last node, ignore the whole statement
                                if (next == null || next.next() == null) {
                                        err.debug("the next node is null, ignore the whole statement");
                                        throw new ParseFail();
                                } else {
                                        err.debug("the next node is not null, skip current and go to the next node");
                                        current = next.next();
                                }
                        } else {
                                current = next;
                        }
                } else {
                        current = next;
                }
                if (next instanceof EndingNode && ((EndingNode) next).getType() == EndingNode.WEAK) {
                        if (!canBeEnd) {
                                nextNode(false);
                        }
                }
        }

        /**
         * parse a statement<br>
         * <code>
         * S->Statement<br>
         * <br>
         * Statement->Expression<br>
         * Statement->Pre<br>
         * Statement->Def<br>
         * <br>
         * Pre->Import<br>
         * Pre->modifier<br>
         * Pre->PackageDeclare<br>
         * <br>
         * Def->ClassDef<br>
         * Def->InterfaceDef<br>
         * Def->MethodDef<br>
         * Def->VariableDef
         * </code>
         *
         * @return a statement object or null if there's no statements in this shift
         * @throws SyntaxException compiling error
         */
        private Statement parse_statement() throws SyntaxException {
                if (current == null) return null; // there's no node
                // common process
                if (current instanceof Element) {
                        String content = ((Element) current).getContent(); // get content

                        if (isSync((Element) current)) {
                                // sync is both key and modifier
                                // it's parsed independently
                                annosIsEmpty();
                                modifiersIsEmpty();

                                return parse_synchronized();
                        } else if (current.getTokenType() == TokenType.MODIFIER) {

                                parse_modifier();
                                return null;

                        } else if (current.getTokenType() == TokenType.KEY) {
                                switch (content) {
                                        case "if":
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                return parse_if();
                                        case "for":
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                return parse_for();
                                        case "do":
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                return parse_do_while();
                                        case "while":
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                return parse_while();
                                        case "static":
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                LineCol lineCol = current.getLineCol();
                                                if (current.next() instanceof ElementStartNode) {
                                                        // static
                                                        //     ...
                                                        nextNode(false);
                                                        return new AST.StaticScope(
                                                                parseElemStart((ElementStartNode) current, false, Collections.emptySet(), false),
                                                                lineCol);

                                                } else if (current.next() instanceof Element) {
                                                        // static ...
                                                        nextNode(false);
                                                        Element curr = (Element) current;
                                                        Statement stmt = parse_statement();
                                                        if (stmt == null) {
                                                                err.UnexpectedTokenException("a valid statement", curr.toString(), curr.getLineCol());
                                                                err.debug("skip the static statements");
                                                                // ignore the static
                                                                // make it (static pass)
                                                                throw new ParseFail();
                                                        }
                                                        return new AST.StaticScope(Collections.singletonList(stmt), lineCol);

                                                } else {
                                                        // static
                                                        // and no other statements/expressions
                                                        return null;
                                                }
                                        case "class":
                                                return parse_class();
                                        case "interface":
                                                return parse_interface();
                                        case "fun":
                                                return parse_fun();

                                        case "try":
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                return parse_try();
                                        case "throw":
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                return parse_throw();
                                        case "package":
                                                modifiersIsEmpty();

                                                // package declare
                                                return parse_pkg_declare();
                                        case "import":
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                return parse_pkg_import();
                                        case "continue":
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                return new AST.Continue(current.getLineCol());
                                        case "break":
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                return new AST.Break(current.getLineCol());
                                        case "return":
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                // return
                                                lineCol = current.getLineCol();

                                                if (!(current.next() instanceof Element)) {
                                                        return new AST.Return(null, lineCol);
                                                } else {
                                                        Expression e = next_exp(false);
                                                        return new AST.Return(e, lineCol);
                                                }
                                }
                        } else if (current.getTokenType() == TokenType.SYMBOL) {
                                switch (content) {
                                        case "...":
                                                return new AST.Pass(current.getLineCol());
                                        case "@":
                                                modifiersIsEmpty();

                                                boolean tmp = annotationAsExpression;
                                                annotationAsExpression = true;
                                                parse_anno();
                                                annotationAsExpression = tmp;

                                                if (annotationAsExpression)
                                                        if (!annos.isEmpty()) {
                                                                AST.Anno anno = annos.iterator().next();
                                                                annos.clear();
                                                                return new AST.AnnoExpression(anno);
                                                        }

                                                return null;
                                }
                        }

                        // other tokens

                        if (current.getTokenType() == TokenType.VALID_NAME) {
                                // check whether is method def
                                int def_method_type = checkMethodDef((Element) current,
                                        !annos.isEmpty() || !modifiers.isEmpty());
                                if (def_method_type == METHOD_DEF_TYPE) {
                                        // method():Type
                                        return parse_method_def_type();
                                } else if (def_method_type == METHOD_DEF_EMPTY) {
                                        // method()=pass
                                        return parse_method_def_empty();
                                } else if (def_method_type == METHOD_DEF_NORMAL) {
                                        // method()
                                        //     ...
                                        return parse_method_def_normal();
                                } else if (def_method_type == METHOD_DEF_ONE_STMT) {
                                        // method()=....
                                        return parse_method_def_one_stmt();
                                }
                        }

                        // parse expression until EndingNode or null
                        while (true) {
                                parse_expression();
                                if (current == null || !(current instanceof Element)) {
                                        if (parsedExps.empty()) return null;
                                        return parsedExps.pop();
                                }
                        }
                } else {
                        // not element, go on and try to parse again
                        nextNode(true);
                        return parse_statement();
                }
        }

        /**
         * parse synchornized<br>
         * <code>
         * sync(expression,...)<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;...
         * </code>
         *
         * @return Synchronized
         * @throws SyntaxException compiling error
         */
        private AST.Synchronized parse_synchronized() throws SyntaxException {
                LineCol lineCol = current.getLineCol();
                nextNode(false);
                expecting("(", current.previous(), current, err);

                List<Expression> expressions = new ArrayList<>();
                if (current.next() instanceof ElementStartNode) {
                        nextNode(false);
                        List<Statement> statements = parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false);

                        for (Statement s : statements) {
                                if (s instanceof Expression) {
                                        expressions.add((AST.Access) s);
                                } else {
                                        err.UnexpectedTokenException("expression", s.toString(), s.line_col());
                                        // ignore this value and continue on
                                        err.debug("ignore this value");
                                }
                        }
                }

                nextNode(false);
                expecting(")", current.previous(), current, err);
                nextNode(true);
                List<Statement> statements = null;
                if (current instanceof ElementStartNode) {
                        statements = parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false);
                }
                return new AST.Synchronized(
                        expressions,
                        statements == null ? Collections.emptyList() : statements,
                        lineCol);
        }

        /**
         * create a new syntactic processor and parse the given node
         *
         * @param startNode    ElementStartNode (root node)
         * @param addUsedNames if true, invoke {@link #addUsedVarNames(Set)}
         * @param parseMap     set {@link #isParsingMap} to given arg
         * @return parsed result (list of statements)
         * @throws SyntaxException compiling error
         */
        private List<Statement> parseElemStart(
                ElementStartNode startNode,
                boolean addUsedNames,
                Set<String> names,
                boolean parseMap)
                throws SyntaxException {
                Parser parser = new Parser(startNode, err);
                if (addUsedNames) {
                        parser.addUsedVarNames(usedVarNames);
                        parser.addUsedVarNames(names);
                }
                parser.isParsingMap = parseMap;
                parser.annotationAsExpression = this.annotationAsExpression;
                return parser.parse();
        }

        /**
         * parse while<br>
         * <code>
         * while boolean<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;...
         * </code>
         *
         * @return While
         * @throws SyntaxException compiling error
         */
        private AST.While parse_while() throws SyntaxException {
                LineCol lineCol = current.getLineCol();

                Expression condition = next_exp(true); // the boolean expression

                List<Statement> body;
                if (current instanceof ElementStartNode) {
                        // parse while body
                        body = parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false);
                } else {
                        err.UnexpectedTokenException("while body", current == null ? "LineEnd" : current.toString(), current == null ? lineCol : current.getLineCol());
                        err.debug("assume that the body is empty");
                        // assume that the body is empty
                        body = Collections.emptyList();
                        jumpToTheNearestEndingNode();
                }
                return new AST.While(condition, body, false, lineCol);
        }

        /**
         * parse do_while<br>
         * <code>
         * do<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;...<br>
         * while boolean
         * </code>
         *
         * @return While
         * @throws SyntaxException compiling error
         */
        private AST.While parse_do_while() throws SyntaxException {
                LineCol lineCol = current.getLineCol();

                nextNode(false); // then current node should be ElementStartNode

                List<Statement> statements;

                if (current instanceof ElementStartNode) {
                        statements = parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false);
                } else {
                        err.UnexpectedTokenException("while body", current == null ? "LineEnd" : current.toString(), current == null ? lineCol : current.getLineCol());
                        err.debug("assume that the body is empty");
                        // assume that the body is empty
                        statements = Collections.emptyList();
                        jumpToTheNearestEndingNode();
                }

                if (current == null) {
                        err.UnexpectedEndException(lineCol);
                        return null;
                } else {
                        nextNode(false);
                        expecting("while", current.previous(), current, err);

                        Expression condition = next_exp(true); // the boolean expression

                        return new AST.While(condition, statements, true, lineCol);
                }
        }

        /**
         * parse import<br>
         * <code>
         * #&gt;<br>
         * &nbsp;&nbsp;&nbsp; java::util::List         ;import one class<br>
         * &nbsp;&nbsp;&nbsp; java::util::_            ;import all from a package<br>
         * &nbsp;&nbsp;&nbsp; java::util::Arrays._     ;import static<br>
         * &nbsp;&nbsp;&nbsp; java::util::Map.Entry    ;import inner class<br>
         * &nbsp;&nbsp;&nbsp; java::util::Map.Entry._  ;import static from inner class<br>
         * &nbsp;&nbsp;&nbsp; Cls._                    ;import static from class in root package
         * </code>
         *
         * @return Import
         * @throws SyntaxException compiling error
         */
        private Import parse_pkg_import() throws SyntaxException {
                LineCol lineCol = current.getLineCol();

                Expression stmt = next_exp(false);

                if (stmt instanceof AST.Access) {
                        // import should firstly be parsed into Access
                        AST.Access a = (AST.Access) stmt;
                        AST.PackageRef pkg = null;
                        AST.Access access = null;
                        boolean importAll;
                        if (a.name.equals("_")) { // ends with '_'
                                if (a.exp instanceof AST.PackageRef) {
                                        // import all from a package
                                        pkg = (AST.PackageRef) a.exp;
                                        importAll = true;
                                } else {
                                        // import static
                                        if (!(a.exp instanceof AST.Access)) {
                                                err.UnexpectedTokenException("package::class", a.exp.toString(), a.exp.line_col());
                                                err.debug("ignore this import");
                                                return null;
                                        }

                                        access = (AST.Access) a.exp;
                                        importAll = true;
                                }
                        } else {
                                // import class or inner class
                                access = a;
                                importAll = false;
                        }

                        return new Import(pkg, access, importAll, lineCol);
                } else {
                        err.UnexpectedTokenException("import statement", stmt.toString(), stmt.line_col());
                        // ignore the statement
                        err.debug("ignore this import");
                        return null;
                }
        }

        /**
         * declare a package<br>
         * <code>
         * # java::util
         * </code>
         *
         * @return PackageDeclare
         * @throws SyntaxException compiling error
         */
        private PackageDeclare parse_pkg_declare() throws SyntaxException {
                LineCol lineCol = current.getLineCol();

                nextNode(false);
                if (current instanceof Element) {
                        StringBuilder sb = new StringBuilder();
                        boolean isName = true;

                        while (current != null && (current instanceof Element)) {
                                Element elem = (Element) current;
                                String s = elem.getContent();
                                if (!isName && !s.equals("::")) {
                                        err.UnexpectedTokenException("::", s, elem.getLineCol());
                                        err.debug("make it '::'");
                                        s = "::";
                                }
                                isName = !isName;
                                sb.append(s);
                                nextNode(true);
                        }

                        // isName should be false
                        if (isName) {
                                sb.delete(sb.length() - 2, sb.length());

                                err.SyntaxException("package name should end with a valid name", lineCol);
                                return new PackageDeclare(new AST.PackageRef(sb.toString(), LineCol.SYNTHETIC), lineCol);
                        }

                        AST.PackageRef pkg = new AST.PackageRef(sb.toString(), lineCol);
                        return new PackageDeclare(pkg, lineCol);
                } else {
                        err.UnexpectedTokenException("package declare", current.toString(), current.getLineCol());
                        err.debug("let it be (default package)");
                        return new PackageDeclare(new AST.PackageRef("", LineCol.SYNTHETIC), lineCol);
                }
        }

        /**
         * annotation<br>
         * <code>
         * <p/>
         * &nbsp;@Anno<br>
         * &nbsp;@Anno()<br>
         * &nbsp;@Anno(exp)<br>
         * &nbsp;@Anno(a=exp)<br>
         * &nbsp;@Anno(a=exp1,b=exp2,exp)<br>
         * </code>
         *
         * @throws SyntaxException compiling error
         */
        private void parse_anno() throws SyntaxException {
                LineCol lineCol = current.getLineCol();

                Set<AST.Anno> storeCurrentAnnos = new HashSet<>(annos);
                annos.clear();

                Expression e = next_exp(false); // annotation
                // might be Invocation
                // might be Access

                // restore the stored annos
                annos.addAll(storeCurrentAnnos);

                AST.Anno anno;
                if (e instanceof AST.Invocation && ((AST.Invocation) e).exp instanceof AST.Access) {
                        // @Anno(...)

                        AST.Invocation inv = (AST.Invocation) e;
                        List<AST.Assignment> assignments = new ArrayList<>();
                        for (Expression exp : inv.args) {
                                // convert into assignments
                                if (exp instanceof VariableDef) {
                                        VariableDef v = (VariableDef) exp;

                                        AST.Assignment a = new AST.Assignment(
                                                new AST.Access(null, v.getName(), v.line_col()),
                                                "=",
                                                v.getInit(), v.line_col());
                                        assignments.add(a);
                                } else if (exp instanceof AST.Assignment) {
                                        assignments.add((AST.Assignment) exp);
                                } else {
                                        AST.Assignment a = new AST.Assignment(
                                                new AST.Access(null, "value", exp.line_col()),
                                                "=",
                                                exp,
                                                LineCol.SYNTHETIC
                                        );
                                        assignments.add(a);
                                }
                        }
                        anno = new AST.Anno((AST.Access) inv.exp, assignments, lineCol);
                } else if (e instanceof AST.Access) {
                        // @Anno

                        anno = new AST.Anno((AST.Access) e, Collections.emptyList(), e.line_col());
                } else {
                        err.UnexpectedTokenException("annotation instance", e.toString(), e.line_col());
                        err.debug("ignore this annotation");
                        return;
                }

                annos.add(anno);
        }

        /**
         * parse throw<br>
         * <code>
         * throw exp
         * </code>
         *
         * @return Throw
         * @throws SyntaxException compiling error
         */
        private AST.Throw parse_throw() throws SyntaxException {
                LineCol lineCol = current.getLineCol();

                Expression exp = next_exp(false);

                return new AST.Throw(exp, lineCol);
        }

        /**
         * parse try<br>
         * <code>
         * try<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;...<br>
         * [catch exVar]<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;Exceptions,...<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;...<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;Other Exceptions,...<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;...<br>
         * [finally]<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;...
         * </code>
         *
         * @return Try
         * @throws SyntaxException compiling error
         */
        private AST.Try parse_try() throws SyntaxException {
                LineCol lineCol = current.getLineCol();

                if (!(current.next() instanceof ElementStartNode)) {
                        err.SyntaxException("invalid try statement without statements", lineCol);
                        err.debug("ignore the try statement");
                        return null;
                }

                nextNode(false); // element start node
                // try[|] catch <var> [|] / finally [|]

                List<Statement> statements = parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false);

                nextNode(true); // catch <var> [|] finally [|]

                if (current == null) {
                        err.SyntaxException("invalid try statement without catch or finally", lineCol);
                        err.debug("assume no catch and finally");
                        return new AST.Try(statements, null, Collections.emptyList(), Collections.emptyList(), lineCol);
                }

                if (current instanceof EndingNode
                        && current.next() instanceof Element
                        &&
                        (
                                ((Element) current.next()).getContent().equals("catch")
                                        || ((Element) current.next()).getContent().equals("finally")
                        )) {
                        nextNode(false);
                }

                String eName = null;
                List<Statement> catchStatements = new ArrayList<>();
                if (current instanceof Element) {
                        String cat = ((Element) current).getContent();
                        // catch
                        if (cat.equals("catch")) {
                                nextNode(false); // <var> [|] finally [|]

                                if ((current instanceof Element)) {
                                        eName = ((Element) current).getContent(); // catch e
                                } else {
                                        err.UnexpectedTokenException("exception variable", current.toString(), current.getLineCol());
                                        err.debug("let the exception variable be 'e'");
                                        eName = "e";
                                }
                                if (current.getTokenType() != TokenType.VALID_NAME) {
                                        err.UnexpectedTokenException("valid variable name", eName, current.getLineCol());
                                        err.debug("assume that it's a valid name");
                                }
                                if (usedVarNames.contains(eName)) {
                                        err.DuplicateVariableNameException(eName, current.getLineCol());
                                        err.debug("assume that it's an unused name");
                                }
                                // catch e
                                nextNode(true); // [|] finally [|]
                                if (null != current && !(current instanceof EndingNode)) {
                                        if (current instanceof ElementStartNode) {
                                                catchStatements.addAll(
                                                        parseElemStart(
                                                                (ElementStartNode) current,
                                                                true,
                                                                Collections.singleton(eName), // the exception holder name
                                                                false));

                                                nextNode(true); // finally [|]
                                                // if it's finally then go next
                                                if (current instanceof EndingNode
                                                        && current.next() instanceof Element
                                                        && ((Element) current.next()).getContent().equals("finally")) {
                                                        nextNode(false);
                                                }
                                        } else {
                                                // element
                                                err.UnexpectedTokenException(current.toString(), current.getLineCol());
                                                err.debug("ignore this token");
                                                jumpToTheNearestEndingNode();
                                        }
                                }
                        }
                }

                List<Statement> fin = new ArrayList<>();
                if (current instanceof Element) {
                        String f = ((Element) current).getContent();
                        // finally
                        if (f.equals("finally")) {
                                LineCol finallyLineCol = current.getLineCol();
                                nextNode(true);
                                if (current instanceof ElementStartNode) {
                                        fin = parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false);
                                } else if (current != null && !(current instanceof EndingNode)) {
                                        err.UnexpectedTokenException(
                                                current == null ? "NewLine" : current.toString(),
                                                current == null ? finallyLineCol : current.getLineCol());
                                        err.debug("ignore this token");
                                        jumpToTheNearestEndingNode();
                                }
                        }
                }

                return new AST.Try(
                        statements == null ? Collections.emptyList() : statements,
                        eName,
                        catchStatements,
                        fin,
                        lineCol);
        }

        /**
         * parse interface<br>
         * <code>
         * interface Name [ : SuperInterface,...]<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;...
         * </code>
         *
         * @return InterfaceDef
         * @throws SyntaxException compiling error
         */
        private InterfaceDef parse_interface() throws SyntaxException {
                LineCol lineCol = current.getLineCol();

                // record modifiers and annotation
                Set<Modifier> set = new HashSet<>(modifiers);
                modifiers.clear();
                Set<AST.Anno> annos = new HashSet<>(this.annos);
                this.annos.clear();

                // interface
                nextNode(false); // interface name

                if (current instanceof Element) {
                        String name = ((Element) current).getContent();

                        List<AST.Access> accesses;

                        if (current.getTokenType() == TokenType.VALID_NAME) {
                                nextNode(true); // can be : or ending or startNode
                                // interface name :
                                accesses = new ArrayList<>();

                                if (current instanceof Element) {
                                        expecting(":", current.previous(), current, err);

                                        nextNode(false);
                                        while (true) {
                                                if (current.getTokenType() == TokenType.VALID_NAME) {
                                                        Expression e = get_exp(true);

                                                        if (e instanceof AST.Access) {
                                                                accesses.add((AST.Access) e);
                                                        } else {
                                                                err.UnexpectedTokenException("super interface", e.toString(), e.line_col());
                                                                err.debug("ignore this super interface");
                                                        }
                                                        if (current instanceof EndingNode && ((EndingNode) current).getType() == EndingNode.STRONG) {
                                                                nextNode(true);
                                                        } else {
                                                                break;
                                                        }
                                                } else {
                                                        break;
                                                }
                                        }
                                }
                        } else {
                                err.UnexpectedTokenException("valid interface name", name, current.getLineCol());
                                err.debug("ignore the interface");

                                throw new ParseFail();
                        }

                        List<Statement> statements = null;
                        if (current instanceof ElementStartNode) {
                                // interface name
                                //     ...
                                statements = parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false);
                                nextNode(true);
                        }

                        InterfaceDef interfaceDef = new InterfaceDef(name, set,
                                accesses,
                                annos,
                                statements == null ? Collections.emptyList() : statements,
                                lineCol);
                        annos.clear();
                        return interfaceDef;
                } else {
                        err.UnexpectedTokenException("interface name", current.toString(), current.getLineCol());
                        err.debug("ignore this interface declaration");
                        return null;
                }
        }

        /**
         * parse class<br>
         * <code>
         * class ClassName [(params,...)] [: [SuperClass[(arg,...)],SuperInterfaces,...]]<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;...
         * </code>
         *
         * @return ClassDef
         * @throws SyntaxException compiling error
         */
        private ClassDef parse_class() throws SyntaxException {
                LineCol lineCol = current.getLineCol();

                // record modifiers and annotations
                Set<Modifier> set = new HashSet<>(modifiers);
                modifiers.clear();
                Set<AST.Anno> annos = new HashSet<>(this.annos);
                this.annos.clear();
                // class
                nextNode(false); // class name

                if (current instanceof Element) {
                        String name = ((Element) current).getContent();
                        if (current.getTokenType() != TokenType.VALID_NAME) {
                                err.UnexpectedTokenException("valid class name", name, current.getLineCol());
                                err.debug("assume the token is a valid name");
                        }
                        List<VariableDef> params = null;

                        Set<String> newParamNames = new HashSet<>();
                        nextNode(true); // can be ( or : or ending or ending or startNode
                        if (current instanceof Element) {
                                String p = ((Element) current).getContent();
                                switch (p) {
                                        case "(":
                                                // class Cls(...)

                                                nextNode(false);
                                                if (current instanceof ElementStartNode) {
                                                        // class ClassName(口
                                                        List<Statement> list = parseElemStart(
                                                                (ElementStartNode) current, false, Collections.emptySet(), false);

                                                        params = new ArrayList<>();
                                                        boolean MustHaveInit = false;
                                                        for (Statement stmt : list) {
                                                                if (stmt instanceof AST.Access) {
                                                                        if (MustHaveInit) {
                                                                                err.SyntaxException("expecting parameter with init value", stmt.line_col());
                                                                                err.debug("assume it has init value");
                                                                        }
                                                                        AST.Access access = (AST.Access) stmt;
                                                                        if (access.exp != null) {
                                                                                err.SyntaxException("parameter cannot be " + access.toString(), access.line_col());
                                                                                err.debug("ignore access.exp");
                                                                        }
                                                                        VariableDef v = new VariableDef(access.name, Collections.emptySet(), Collections.emptySet(), current.getLineCol());
                                                                        params.add(v);

                                                                        newParamNames.add(v.getName());
                                                                } else if (stmt instanceof VariableDef) {
                                                                        if (((VariableDef) stmt).getInit() == null) {
                                                                                if (MustHaveInit) {
                                                                                        err.SyntaxException("expecting parameter with init value", stmt.line_col());
                                                                                        err.debug("assume it has init value");
                                                                                }
                                                                        } else {
                                                                                MustHaveInit = true;
                                                                        }

                                                                        params.add((VariableDef) stmt);

                                                                        newParamNames.add(((VariableDef) stmt).getName());
                                                                } else {
                                                                        err.SyntaxException("parameter cannot be " + stmt.toString(), stmt.line_col());
                                                                        err.debug("ignore this parameter def");
                                                                }
                                                        }


                                                        nextNode(false); // )
                                                        expecting(")", current.previous(), current, err);
                                                        nextNode(true);
                                                } else if (current instanceof Element) {
                                                        expecting(")", current.previous(), current, err);
                                                        // class ClassName()

                                                        params = Collections.emptyList();
                                                        nextNode(true);
                                                } else {
                                                        err.UnexpectedTokenException(current.toString(), current.getLineCol());
                                                        err.debug("ignore the parameters");
                                                }
                                                break;
                                        case ":":
                                                // class Cls:...
                                                // do nothing
                                                break;
                                        default:
                                                err.UnexpectedTokenException("( or :", p, current.getLineCol());
                                                err.debug("ignore the token");
                                                nextNode(true);
                                }
                        }

                        AST.Invocation invocation = null; // super class with constructor arguments
                        List<AST.Access> accesses = new ArrayList<>(); // inherit without invocation (class or interface)

                        if (current instanceof Element) {
                                // :
                                expecting(":", current.previous(), current, err);
                                nextNode(false);
                                while (true) {
                                        if (current.getTokenType() == TokenType.VALID_NAME) {
                                                Expression e = get_exp(true);

                                                if (e instanceof AST.Access) {
                                                        accesses.add((AST.Access) e);
                                                } else if (e instanceof AST.Invocation && ((AST.Invocation) e).exp instanceof AST.Access) {
                                                        if (invocation == null) {
                                                                invocation = (AST.Invocation) e;
                                                        } else {
                                                                err.SyntaxException("Multiple Inheritance is not allowed", e.line_col());
                                                                err.debug("ignore the arguments and only record the name");
                                                                accesses.add((AST.Access) ((AST.Invocation) e).exp);
                                                        }
                                                } else {
                                                        err.SyntaxException("super class or super interfaces cannot be " + e.toString(), e.line_col());
                                                        err.debug("ignore this inheritance");
                                                }
                                                if (current instanceof EndingNode && ((EndingNode) current).getType() == EndingNode.STRONG) {
                                                        nextNode(true);
                                                } else {
                                                        break;
                                                }
                                        } else {
                                                break;
                                        }
                                }
                        }

                        // statements
                        List<Statement> stmts = null;
                        if (current instanceof ElementStartNode) {
                                stmts = parseElemStart((ElementStartNode) current, true, newParamNames, false);
                        }

                        return new ClassDef(
                                name,
                                set,
                                params == null ? Collections.emptyList() : params,
                                invocation,
                                accesses,
                                annos, stmts == null ? Collections.emptyList() : stmts,
                                lineCol);
                } else {
                        err.UnexpectedTokenException("class name", current.toString(), current.getLineCol());
                        err.debug("ignore this class definition");
                        throw new ParseFail();
                }
        }

        /**
         * parse function.<br>
         * <code>
         * fun FunctionName [(params,...)] : SuperType<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;...
         * </code>
         *
         * @return FunDef
         * @throws SyntaxException compiling error
         */
        private FunDef parse_fun() throws SyntaxException {
                ClassDef classDef = parse_class();
                if (classDef.superWithoutInvocation.isEmpty()) {
                        classDef.superWithoutInvocation.add(
                                new AST.Access(
                                        new AST.PackageRef("lt::lang::function", LineCol.SYNTHETIC),
                                        "Function" + classDef.params.size(),
                                        LineCol.SYNTHETIC
                                )
                        );
                }
                if ((classDef.superWithInvocation != null) || classDef.superWithoutInvocation.size() != 1) {
                        err.SyntaxException("function definitions should have one super type, which should be functional interface or functional abstract class",
                                classDef.line_col());
                        // if no super type defined, then assert it is lt::lang::FunctionX (X is parameter size)
                        if (classDef.superWithoutInvocation.isEmpty()) {
                                classDef.superWithoutInvocation.add(
                                        new AST.Access(
                                                new AST.PackageRef(
                                                        "lt::lang::function", LineCol.SYNTHETIC
                                                ),
                                                "Function" + classDef.params.size(),
                                                LineCol.SYNTHETIC
                                        )
                                );
                        }
                }
                if (!classDef.modifiers.isEmpty()) {
                        err.SyntaxException("function definitions do not have modifiers", classDef.line_col());
                }

                // transform into fun

                return new FunDef(
                        classDef.name,
                        classDef.params,
                        classDef.superWithoutInvocation.get(0),
                        classDef.annos,
                        classDef.statements,
                        classDef.line_col()
                );
        }

        /**
         * for<br>
         * <code>
         * for i @ exp<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;...
         * </code>
         *
         * @return For
         * @throws SyntaxException compiling error
         */
        private AST.For parse_for() throws SyntaxException {
                LineCol lineCol = current.getLineCol();
                nextNode(false); // variable
                if (!(current instanceof Element)) {
                        err.SyntaxException("invalid for statement", current == null ? lineCol : current.getLineCol());
                        err.debug("ignore the statement");
                        throw new ParseFail();
                }
                Element varElem = (Element) current;
                String varName = varElem.getContent();
                if (varElem.getTokenType() != TokenType.VALID_NAME) {
                        err.UnexpectedTokenException("valid variable name", varName, current.getLineCol());
                        err.debug("assume that the name is 'i'");
                        varName = "i";
                }
                if (usedVarNames.contains(varName)) {
                        err.DuplicateVariableNameException(varName, current.getLineCol());
                        err.debug("assume that it's an unused name");
                }

                nextNode(false); // in
                expecting("in", current.previous(), current, err);

                Expression exp = next_exp(true); // expression

                List<Statement> statements = null;
                if (current instanceof ElementStartNode) {
                        statements = parseElemStart((ElementStartNode) current, true, Collections.singleton(varName), false);
                }

                return new AST.For(
                        varName,
                        exp,
                        statements == null ? Collections.emptyList() : statements,
                        lineCol);
        }

        /**
         * if<br>
         * <code>
         * if expression<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;...<br>
         * elseif expression<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;...<br>
         * else<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;...
         * </code>
         *
         * @return If
         * @throws SyntaxException compiling error
         */
        private AST.If parse_if() throws SyntaxException {
                LineCol lineCol = current.getLineCol();
                List<AST.If.IfPair> pairs = new ArrayList<>();

                boolean isLast = false;

                while (current instanceof Element || current instanceof EndingNode) {
                        LineCol ifPairLineCol = current.getLineCol();

                        // parse expression next to if/elseif/else
                        if (current instanceof EndingNode
                                && current.next() instanceof Element) {
                                String content = ((Element) current.next()).getContent();
                                if (content.equals("elseif") || content.equals("else")) {
                                        nextNode(false);
                                } else {
                                        break;
                                }
                        }
                        Expression condition = null;
                        String content = ((Element) current).getContent();

                        // out of if scope
                        if (!content.equals("if") && !content.equals("elseif") && !content.equals("else")) {
                                break;
                        }

                        // nodes next to else might be Ending or ElemStart
                        if (((Element) current).getContent().equals("else")) {
                                if (isLast) {
                                        err.SyntaxException("if-else statement had already reached 'else' but got " + content + " instead", current.getLineCol());
                                        err.debug("ignore this if branch");
                                } else {
                                        nextNode(true);
                                }
                        } else {
                                nextNode(false);
                        }
                        if (content.equals("if") || content.equals("elseif")) {

                                if (isLast) {
                                        err.SyntaxException("if-else statement had already reached 'else' but got " + content + " instead", current.getLineCol());
                                        err.debug("ignore this if branch");
                                } else {
                                        condition = get_exp(true);
                                }
                        }

                        List<Statement> list = null;
                        if (current instanceof ElementStartNode) {
                                list = parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false);
                        }

                        if (condition == null) {
                                if (!isLast) {
                                        isLast = true;

                                        AST.If.IfPair pair = new AST.If.IfPair(null, list == null ? Collections.emptyList() : list, ifPairLineCol);
                                        pairs.add(pair);
                                }
                        } else {
                                AST.If.IfPair pair = new AST.If.IfPair(condition, list == null ? Collections.emptyList() : list, ifPairLineCol);
                                pairs.add(pair);
                        }

                        nextNode(true);
                        last2VarOps.clear();
                }

                if (current != null) {
                        // the last loop invoked nextNode(true)
                        // but parse_statement() also invokes nextNode(true)
                        current = current.previous();
                }

                return new AST.If(pairs, lineCol);
        }

        /**
         * parse method param contents<br>
         * method<b>(params,...)</b><br>
         * the current node should be `method name`<br>
         * after the process, current node would be the ')'
         *
         * @param variableList variable list to fill
         * @param names        a set of defined names to fill
         * @throws SyntaxException compiling error
         */
        private void parse_method_def_variables(List<VariableDef> variableList, Set<String> names) throws SyntaxException {
                nextNode(false); // method(
                expecting("(", current.previous(), current, err);
                nextNode(false);
                if (current instanceof ElementStartNode) {
                        // method(口
                        List<Statement> statements = parseElemStart((ElementStartNode) current, false, Collections.emptySet(), false);
                        boolean MustHaveInit = false;
                        for (Statement s : statements) {
                                if (s instanceof AST.Access && ((AST.Access) s).exp == null) {
                                        if (MustHaveInit) {
                                                err.SyntaxException("expecting parameter with init value", s.line_col());
                                                err.debug("assume it has init value");
                                        }

                                        AST.Access access = (AST.Access) s;
                                        VariableDef d = new VariableDef(access.name, Collections.emptySet(), Collections.emptySet(), access.line_col());
                                        variableList.add(d);
                                        names.add(access.name);
                                } else if (s instanceof VariableDef) {
                                        if (((VariableDef) s).getInit() == null) {
                                                if (MustHaveInit) {
                                                        err.SyntaxException("expecting parameter with init value", s.line_col());
                                                        err.debug("assume it has init value");
                                                }
                                        } else {
                                                MustHaveInit = true;
                                        }
                                        variableList.add((VariableDef) s);
                                        names.add(((VariableDef) s).getName());
                                } else {
                                        err.SyntaxException("parameter cannot be " + s.toString(), s.line_col());
                                        err.debug("ignore this parameter");
                                }
                        }
                        nextNode(false);
                }
                expecting(")", current.previous(), current, err);
        }

        /**
         * parse method definition<br>
         * <code>
         * method(params,...)<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;...
         * </code>
         *
         * @return MethodDef
         * @throws SyntaxException compiling error
         */
        private MethodDef parse_method_def_normal() throws SyntaxException {
                LineCol lineCol = current.getLineCol();
                String methodName = ((Element) current).getContent();
                Set<AST.Anno> annos = new HashSet<>(this.annos);
                this.annos.clear();

                List<VariableDef> variableList = new ArrayList<>();
                Set<String> names = new HashSet<>();
                parse_method_def_variables(variableList, names);
                // method(..)
                nextNode(false); // 口

                List<Statement> stmts = parseElemStart((ElementStartNode) current, true, names, false);
                MethodDef def = new MethodDef(methodName, modifiers, null, variableList, annos, stmts, lineCol);
                annos.clear();
                modifiers.clear();
                return def;
        }

        /**
         * parse method definition<br>
         * <code>
         * method(params,...)=pass
         * </code>
         *
         * @return MethodDef
         * @throws SyntaxException compiling error
         */
        private MethodDef parse_method_def_empty() throws SyntaxException {
                LineCol lineCol = current.getLineCol();
                String methodName = ((Element) current).getContent();
                Set<AST.Anno> annos = new HashSet<>(this.annos);
                this.annos.clear();
                Set<Modifier> modifiers = new HashSet<>(this.modifiers);
                this.modifiers.clear();

                List<VariableDef> variableList = new ArrayList<>();
                Set<String> names = new HashSet<>();
                parse_method_def_variables(variableList, names);
                // method(..)

                nextNode(true); // method(..)= or method(..)
                if (current != null && !(current instanceof EndingNode)) {
                        nextNode(false); // method(..)=pass
                        nextNode(true);
                }

                MethodDef def = new MethodDef(methodName, modifiers, null, variableList, annos,
                        Collections.emptyList(),
                        lineCol);
                annos.clear();
                return def;
        }

        /**
         * parse method definition<br>
         * <code>
         * method(params,...)=exp
         * </code>
         *
         * @return MethodDef
         * @throws SyntaxException compiling error
         */
        private MethodDef parse_method_def_one_stmt() throws SyntaxException {
                LineCol lineCol = current.getLineCol();
                String methodName = ((Element) current).getContent();
                Set<AST.Anno> annos = new HashSet<>(this.annos);
                this.annos.clear();
                Set<Modifier> modifiers = new HashSet<>(this.modifiers);
                this.modifiers.clear();

                List<VariableDef> variableList = new ArrayList<>();
                Set<String> names = new HashSet<>();
                parse_method_def_variables(variableList, names);
                // method(..)
                nextNode(false); // method(..)=
                nextNode(false); // method(..)=exp

                parse_expression();

                Expression exp = parsedExps.pop();
                return new MethodDef(methodName, modifiers, null, variableList, annos, Collections.singletonList(
                        new AST.Return(exp, exp.line_col())
                ), lineCol);
        }

        /**
         * parse method definition<br>
         * <code>
         * method(params,...):Type<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;...
         * </code>
         *
         * @return MethodDef
         * @throws SyntaxException compiling error
         */
        private MethodDef parse_method_def_type() throws SyntaxException {
                LineCol lineCol = current.getLineCol();
                String methodName = ((Element) current).getContent();
                Set<AST.Anno> annos = new HashSet<>(this.annos);
                this.annos.clear();

                List<VariableDef> variableList = new ArrayList<>();
                Set<String> names = new HashSet<>();
                parse_method_def_variables(variableList, names);
                // method(..)
                nextNode(false); // method(..):
                nextNode(false); // method(..):..
                AST.Access returnType = parse_cls_for_type_spec();

                if (current instanceof Element && ((Element) current).getContent().equals("=")) {
                        // check "="
                        if (current.next() instanceof Element && ((Element) current.next()).getContent().equals("...")) {
                                return new MethodDef(methodName, modifiers, returnType, variableList, annos,
                                        Collections.emptyList(),
                                        lineCol);
                        } else {
                                Expression exp = next_exp(false);

                                return new MethodDef(methodName, modifiers, returnType, variableList, annos,
                                        Collections.singletonList(
                                                new AST.Return(exp, exp.line_col())
                                        ),
                                        lineCol);
                        }
                } else {
                        if (current instanceof ElementStartNode) {
                                // parse
                                List<Statement> list = parseElemStart((ElementStartNode) current, true, names, false);
                                MethodDef def = new MethodDef(methodName, modifiers, returnType, variableList, annos, list, lineCol);
                                annos.clear();
                                modifiers.clear();
                                return def;
                        } else {
                                // ending or null
                                MethodDef def = new MethodDef(methodName, modifiers, returnType, variableList, annos, Collections.emptyList(), lineCol);
                                annos.clear();
                                modifiers.clear();
                                return def;
                        }
                }
        }

        /**
         * parse an expression
         *
         * @throws SyntaxException compiling errors
         */
        private void parse_expression() throws SyntaxException {
                if (current == null) {
                        return;
                }
                if (current instanceof Element) {
                        String content = ((Element) current).getContent();

                        boolean doCheckParsedExps = true;
                        // first go to `doCheckParsedExps` branch, if not handled, then goto normal branch
                        while (true) {
                                if (doCheckParsedExps) {
                                        if (parsedExps.empty()) {
                                                // check unary operator that require no parsed expressions
                                                if (isOneVariableOperatorPreMustCheckExps(content)) {
                                                        annosIsEmpty();
                                                        modifiersIsEmpty();

                                                        parse_oneVarPreOperation();

                                                        return;
                                                }
                                        }

                                        doCheckParsedExps = false;
                                } else {

                                        if (current.getTokenType() == TokenType.NUMBER) {
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                NumberLiteral numberLiteral = new NumberLiteral(content, current.getLineCol());

                                                parsedExps.push(numberLiteral);
                                                nextNode(true);
                                                parse_expression();

                                        } else if (current.getTokenType() == TokenType.BOOL) {
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                BoolLiteral boolLiteral = new BoolLiteral(content, current.getLineCol());

                                                parsedExps.push(boolLiteral);
                                                nextNode(true);
                                                parse_expression();

                                        } else if (current.getTokenType() == TokenType.STRING) {
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                StringLiteral stringLiteral = new StringLiteral(content, current.getLineCol());

                                                parsedExps.push(stringLiteral);
                                                nextNode(true);
                                                parse_expression();

                                        } else if (current.getTokenType() == TokenType.REGEX) {
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                RegexLiteral regexLiteral = new RegexLiteral(content, current.getLineCol());

                                                parsedExps.push(regexLiteral);
                                                nextNode(true);
                                                parse_expression();

                                        } else if (isTwoVariableOperator(content) &&
                                                (
                                                        current.getTokenType() == TokenType.KEY || (current.getTokenType() == TokenType.SYMBOL)
                                                )) {
                                                // is/not/in are both two var op and keys
                                                // it's handled independently
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                parse_twoVarOperation();

                                        } else if (current.getTokenType() == TokenType.KEY) {
                                                switch (content) {
                                                        case "type":
                                                                annosIsEmpty();
                                                                modifiersIsEmpty();

                                                                LineCol lineCol = current.getLineCol();
                                                                nextNode(false);
                                                                AST.Access access = parse_cls_for_type_spec();
                                                                parsedExps.push(new AST.TypeOf(access, lineCol));

                                                                parse_expression();

                                                                break;
                                                        case "null":

                                                                annosIsEmpty();
                                                                modifiersIsEmpty();
                                                                parsedExps.push(new AST.Null(current.getLineCol()));
                                                                nextNode(true);
                                                                parse_expression();
                                                                break;
                                                        case "as":

                                                                annosIsEmpty();
                                                                modifiersIsEmpty();

                                                                if (parsedExps.isEmpty()) {
                                                                        err.UnexpectedTokenException("expression", "as", current.getLineCol());
                                                                        err.debug("ignore the statement");
                                                                        throw new ParseFail();
                                                                } else {
                                                                        lineCol = current.getLineCol();
                                                                        Expression exp = parsedExps.pop();
                                                                        nextNode(false);
                                                                        AST.Access type = parse_cls_for_type_spec();
                                                                        AST.AsType asType = new AST.AsType(exp, type, lineCol);
                                                                        parsedExps.push(asType);
                                                                }
                                                                break;
                                                        case "undefined":

                                                                annosIsEmpty();
                                                                modifiersIsEmpty();

                                                                parsedExps.push(new AST.UndefinedExp(current.getLineCol()));
                                                                nextNode(true);
                                                                parse_expression();

                                                                break;
                                                        case "require":

                                                                annosIsEmpty();
                                                                modifiersIsEmpty();
                                                                nextNode(false);
                                                                lineCol = current.getLineCol();
                                                                Expression exp = get_exp(false);
                                                                parsedExps.push(new AST.Require(exp, lineCol));
                                                                parse_expression();
                                                                break;
                                                        case "new":
                                                                annosIsEmpty();
                                                                modifiersIsEmpty();

                                                                // new
                                                                lineCol = current.getLineCol();

                                                                Expression next = next_exp(false);
                                                                AST.New aNew;
                                                                if (next instanceof AST.Invocation) {
                                                                        if (((AST.Invocation) next).invokeWithNames) {
                                                                                err.SyntaxException("constructing an object does not support invokeWithNames", next.line_col());
                                                                                // assume it's not invokeWithNames
                                                                        }
                                                                        aNew = new AST.New((AST.Invocation) next, lineCol);
                                                                } else if (next instanceof AST.Access) {
                                                                        aNew = new AST.New(
                                                                                new AST.Invocation(
                                                                                        next,
                                                                                        Collections.emptyList(),
                                                                                        false,
                                                                                        next.line_col()
                                                                                ),
                                                                                lineCol
                                                                        );
                                                                } else {
                                                                        err.UnexpectedTokenException(
                                                                                "invoking a constructor",
                                                                                next.toString(), next.line_col());
                                                                        // ignore the exp
                                                                        throw new ParseFail();
                                                                }
                                                                parsedExps.push(aNew);
                                                                parse_expression();
                                                                break;

                                                        default:
                                                                err.UnexpectedTokenException(content, current.getLineCol());
                                                                // ignore
                                                                err.debug("ignore the token");
                                                                nextNode(true);
                                                }

                                        } else if (current.getTokenType() == TokenType.SYMBOL) {

                                                if (content.equals(".")) {
                                                        annosIsEmpty();
                                                        modifiersIsEmpty();

                                                        parse_access(true);

                                                } else if (isOneVariableOperatorPreWithoutCheckingExps(content)) {
                                                        annosIsEmpty();
                                                        modifiersIsEmpty();

                                                        parse_oneVarPreOperation();

                                                } else if (isOneVariableOperatorPost(content)) {
                                                        annosIsEmpty();
                                                        modifiersIsEmpty();

                                                        parse_oneVarPostOperation();

                                                } else if (isAssign(content)) {
                                                        annosIsEmpty();
                                                        modifiersIsEmpty();

                                                        parse_assign();

                                                } else if (content.equals(":")) {
                                                        annosIsEmpty();
                                                        modifiersIsEmpty();

                                                        if (isParsingMap) {
                                                                // directly return
                                                                // the key would be retrieved from the parsedExp stack
                                                                return;
                                                        } else {
                                                                parse_type_spec();
                                                        }

                                                } else if (content.equals("[")) {
                                                        annosIsEmpty();
                                                        modifiersIsEmpty();

                                                        if (parsedExps.empty()) {
                                                                parse_array_exp();
                                                        } else {
                                                                parse_index_access();
                                                        }
                                                } else if (content.equals("{")) {
                                                        annosIsEmpty();
                                                        modifiersIsEmpty();

                                                        parse_map();

                                                } else if (content.equals("@")) {
                                                        annosIsEmpty();
                                                        modifiersIsEmpty();

                                                        boolean tmp = annotationAsExpression;
                                                        annotationAsExpression = true;
                                                        parse_anno();
                                                        annotationAsExpression = tmp;
                                                        if (!annos.isEmpty()) {
                                                                AST.Anno anno = annos.iterator().next();
                                                                parsedExps.push(new AST.AnnoExpression(anno));
                                                                annos.clear();
                                                        }

                                                } else if (content.equals("#")) {
                                                        annosIsEmpty();
                                                        modifiersIsEmpty();

                                                        LineCol lineCol = current.getLineCol();
                                                        Expression theType = next_exp(true);
                                                        if (theType instanceof AST.Access) {
                                                                List<Statement> ast;
                                                                if (current instanceof ElementStartNode) {
                                                                        ast = parseElemStart((ElementStartNode) current, false, Collections.emptySet(), false);
                                                                        nextNode(true);
                                                                } else {
                                                                        ast = Collections.emptyList();
                                                                }
                                                                parsedExps.push(new AST.GeneratorSpec((AST.Access) theType, ast, lineCol));
                                                        } else {
                                                                err.UnexpectedTokenException("a type", theType.toString(), theType.line_col());
                                                        }

                                                } else if (content.equals("(")) {
                                                        annosIsEmpty();
                                                        modifiersIsEmpty();

                                                        if (isLambda((Element) current)) {
                                                                parse_lambda();
                                                        } else {

                                                                nextNode(false);

                                                                if (current instanceof Element) {
                                                                        // element should be ')'
                                                                        expecting(")", current.previous(), current, err);
                                                                        if (!parsedExps.empty()) {
                                                                                // method() invocation
                                                                                Expression invocationExp = parsedExps.pop();
                                                                                AST.Invocation invocation = new AST.Invocation(invocationExp, Collections.emptyList(),
                                                                                        false, invocationExp.line_col());
                                                                                parsedExps.push(invocation);
                                                                        } else {
                                                                                err.SyntaxException("it should be the method to invoke", parsedExps.empty() ? current.getLineCol() : parsedExps.peek().line_col());
                                                                        }

                                                                        nextNode(true);
                                                                        parse_expression();
                                                                } else if (current instanceof ElementStartNode) {
                                                                        // element start node : ...(口)...
                                                                        ElementStartNode startNode = (ElementStartNode) current;
                                                                        List<Statement> statements = parseElemStart(startNode, true, Collections.emptySet(), false);

                                                                        if (!parsedExps.empty()) {
                                                                                // method(...) or xx[i]() or xx()()()...()
                                                                                Expression invocationExp = parsedExps.pop();
                                                                                List<Expression> args = new ArrayList<>();

                                                                                boolean allVarDef = !statements.isEmpty();
                                                                                for (Statement stmt : statements) {
                                                                                        if ((stmt instanceof Expression)) {
                                                                                                args.add((Expression) stmt);

                                                                                                if (stmt instanceof VariableDef) {
                                                                                                        if (((VariableDef) stmt).getInit() == null
                                                                                                                ||
                                                                                                                !((VariableDef) stmt).getAnnos().isEmpty()
                                                                                                                ||
                                                                                                                !((VariableDef) stmt).getModifiers().isEmpty()) {
                                                                                                                allVarDef = false;
                                                                                                        }
                                                                                                } else {
                                                                                                        allVarDef = false;
                                                                                                }
                                                                                        } else {
                                                                                                err.UnexpectedTokenException("expression", stmt.toString(), stmt.line_col());
                                                                                                err.debug("ignore the argument");

                                                                                                allVarDef = false;
                                                                                        }
                                                                                }

                                                                                AST.Invocation invocation = new AST.Invocation(invocationExp,
                                                                                        args, allVarDef, current.getLineCol());
                                                                                parsedExps.push(invocation);
                                                                        } else {
                                                                                if (statements.size() == 1) {
                                                                                        Statement stmt = statements.get(0);
                                                                                        if (stmt instanceof Expression) {
                                                                                                // something like 3*(1+2)
                                                                                                parsedExps.push((Expression) stmt);
                                                                                        } else {
                                                                                                // statement
                                                                                                AST.Procedure procedure = new AST.Procedure(statements, startNode.getLineCol());
                                                                                                parsedExps.push(procedure);
                                                                                        }
                                                                                } else {
                                                                                        AST.Procedure procedure = new AST.Procedure(statements, startNode.getLineCol());
                                                                                        parsedExps.push(procedure);
                                                                                }
                                                                        }

                                                                        nextNode(false); // should be ')'
                                                                        expecting(")", startNode, current, err);
                                                                        nextNode(true);
                                                                        parse_expression();
                                                                }
                                                        }
                                                } else {
                                                        err.UnexpectedTokenException(content, current.getLineCol());
                                                        err.debug("ignore the token");
                                                        nextNode(true);
                                                }
                                        } else if (current.getTokenType() == TokenType.VALID_NAME) {
                                                if (isLambda((Element) current)) {
                                                        parse_lambda();
                                                } else if (isPackage((Element) current)) {
                                                        annosIsEmpty();
                                                        modifiersIsEmpty();

                                                        parse_package(true);

                                                } else {
                                                        // could be a var if it's the first expression in the exp stack
                                                        // or it could be method invocation

                                                        if (parsedExps.empty()) {
                                                                parse_var();
                                                        } else {
                                                                parse_operator_like_invocation();
                                                        }

                                                }
                                        } else {
                                                err.UnexpectedTokenException(content, current.getLineCol());
                                                err.debug("ignore the token");
                                                nextNode(true);
                                        }

                                        break;
                                }
                        }

                } else if (current instanceof ElementStartNode) {
                        if (!expectingStartNode) {
                                err.UnexpectedNewLayerException(current.getLineCol());
                                err.debug("ignore the statement");
                                throw new ParseFail();
                        }
                }
                // else
        }

        /**
         * parse expression like these<br>
         * <code>a op b</code><br>
         * which would be parsed into <code>a.op(b)</code><br>
         * <code>a op</code><br>
         * which would be parsed into <code>a.op()</code>
         * </code>
         */
        private void parse_operator_like_invocation() throws SyntaxException {
                // is parsing operator like invocation then disable this feature
                if (isParsingOperatorLikeInvocation) return;

                parsedExpsNotEmpty(current);
                Expression a = parsedExps.pop();
                String op = ((Element) current).getContent();
                LineCol opLineCol = current.getLineCol();

                if (current.next() instanceof Element && (!isParsingMap || !((Element) current.next()).getContent().equals(":"))) {
                        // is two var op
                        if (!last2VarOps.empty()) {
                                String lastOp = last2VarOps.pop();
                                if (twoVar_higherOrEqual(lastOp, op)) {
                                        parsedExps.push(a);
                                        return;
                                }
                                last2VarOps.push(lastOp);
                        }
                        nextNode(true);
                        // another expression exists
                        // e.g. a op b
                        last2VarOps.push(op);
                        List<Expression> opArgs = new ArrayList<>();
                        opArgs.add(get_exp(false));

                        while (current instanceof EndingNode && ((EndingNode) current).getType() == EndingNode.STRONG) {
                                // take out all var op from stack
                                Stack<String> tmp = new Stack<>();
                                while (!last2VarOps.empty()) tmp.push(last2VarOps.pop());
                                nextNode(false);

                                isParsingOperatorLikeInvocation = true;

                                opArgs.add(get_exp(false));

                                isParsingOperatorLikeInvocation = false;

                                while (!tmp.empty()) last2VarOps.push(tmp.pop());
                        }
                        AST.Invocation invocation = new AST.Invocation(
                                new AST.Access(a, op, opLineCol),
                                opArgs,
                                false, opLineCol);
                        parsedExps.push(invocation);
                } else {
                        // a.op()
                        if (!last2VarOps.empty()) {
                                last2VarOps.pop();
                                parsedExps.push(a);
                                return;
                        }
                        nextNode(true);
                        AST.Invocation invocation = new AST.Invocation(new AST.Access(a, op, opLineCol), Collections.emptyList(), false, opLineCol);
                        parsedExps.push(invocation);
                }

                parse_expression();
        }

        /**
         * parse map<br>
         * <code>
         * {<br>
         * &nbsp;&nbsp;&nbsp;&nbsp;a:b<br>
         * }
         * </code>
         *
         * @throws SyntaxException compiling error
         */
        private void parse_map() throws SyntaxException {
                LineCol lineCol = current.getLineCol();
                nextNode(false);

                if (current instanceof Element) {
                        // {}
                        expecting("}", current.previous(), current, err);
                        parsedExps.push(new AST.MapExp(new LinkedHashMap<>(), lineCol));

                        nextNode(true);
                } else {
                        // current instance of ElementStartNode
                        expecting("}", current, current.next() == null ? null : current.next().next(), err);

                        parsedExps.push(parseExpMap((ElementStartNode) current));

                        nextNode(false); // }
                        nextNode(true);
                }

                parse_expression();
        }

        /**
         * parse map contents
         *
         * @param startNode root
         * @return MapExp
         * @throws SyntaxException compiling error
         */
        private AST.MapExp parseExpMap(ElementStartNode startNode) throws SyntaxException {
                List<Statement> stmts = parseElemStart(startNode, true, Collections.emptySet(), true);
                if (stmts.size() % 2 != 0) {
                        throw new LtBug("the list should contain key-value entries");
                }

                boolean isKey = true;
                LinkedHashMap<Expression, Expression> map = new LinkedHashMap<>();
                Expression exp = null;

                boolean jumpValue = false;

                for (Statement s : stmts) {
                        if (jumpValue) {
                                jumpValue = false;
                                continue;
                        }
                        if (s instanceof Expression) {
                                if (isKey) {
                                        exp = (Expression) s;
                                } else {
                                        map.put(exp, (Expression) s);
                                }
                                isKey = !isKey;
                        } else {
                                err.UnexpectedTokenException("expression", s.toString(), s.line_col());
                                err.debug("ignore this entry");
                                if (isKey) {
                                        jumpValue = true;
                                } else {
                                        isKey = true;
                                }
                        }
                }
                return new AST.MapExp(map, startNode.getLineCol());
        }

        /**
         * parse index<br>
         * <code>
         * v[i]<br>
         * v[]<br>
         * v[i,j]
         * </code>
         *
         * @throws SyntaxException compiling error
         */
        private void parse_index_access() throws SyntaxException {
                Expression e = parsedExps.pop();
                nextNode(false);
                if (current instanceof Element) {
                        // e[]
                        expecting("]", current.previous(), current, err);
                        parsedExps.push(new AST.Index(e, Collections.emptyList(), e.line_col()));

                        nextNode(true);
                } else {
                        // current instance of ElementStartNode
                        expecting("]", current, current.next() == null ? null : current.next().next(), err);
                        List<Statement> stmts = parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false);
                        List<Expression> exps = new ArrayList<>();

                        for (Statement stmt : stmts) {
                                if (stmt instanceof Expression) {
                                        exps.add((Expression) stmt);
                                } else {
                                        err.UnexpectedTokenException("index access expression", stmt.toString(), stmt.line_col());
                                        err.debug("ignore the statement");
                                }
                        }
                        parsedExps.push(new AST.Index(e, exps, e.line_col()));

                        nextNode(false); // [...]
                        nextNode(true); // [...] ..
                }

                parse_expression();
        }

        /**
         * parse array expression<br>
         * <code>
         * [expression,...]
         * </code>
         *
         * @throws SyntaxException compiling error
         */
        private void parse_array_exp() throws SyntaxException {
                LineCol lineCol = current.getLineCol();
                nextNode(false);
                if (current instanceof Element) {
                        // [] empty array
                        expecting("]", current.previous(), current, err);
                        parsedExps.push(new AST.ArrayExp(Collections.emptyList(), lineCol));

                        nextNode(true);
                } else {
                        // current instanceof ElementStartNode
                        // [...]
                        expecting("]", current, current.next() == null ? null : current.next().next(), err);
                        List<Statement> stmts = parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false);
                        List<Expression> exps = new ArrayList<>();

                        for (Statement stmt : stmts) {
                                if (stmt instanceof Expression) {
                                        exps.add((Expression) stmt);
                                } else {
                                        err.UnexpectedTokenException("array contents", stmt.toString(), stmt.line_col());
                                        err.debug("ignore the statement");
                                }
                        }
                        parsedExps.push(new AST.ArrayExp(exps, lineCol));

                        nextNode(false); // [...]
                        nextNode(true); // [...] ..
                }

                parse_expression();
        }

        /**
         * parse lambda<br>
         * <code>
         * ()=><br>
         * (args,...)=>
         * </code>
         *
         * @throws SyntaxException compiling errors
         */
        private void parse_lambda() throws SyntaxException {
                LineCol lineCol = current.getLineCol();

                List<VariableDef> variableDefList = new ArrayList<>();
                Set<String> set = new HashSet<>();
                if (((Element) current).getContent().equals("(")) {
                        nextNode(false);
                        if (current instanceof ElementStartNode) {
                                List<Statement> list = parseElemStart((ElementStartNode) current, false, Collections.emptySet(), false);
                                for (Statement statement : list) {
                                        if (statement instanceof AST.Access) {
                                                AST.Access access = (AST.Access) statement;
                                                if (access.exp == null) {
                                                        VariableDef v = new VariableDef(access.name, Collections.emptySet(), annos, access.line_col());
                                                        annos.clear();
                                                        variableDefList.add(v);

                                                        set.add(access.name);
                                                } else {
                                                        err.UnexpectedTokenException("variable", access.exp.toString(), access.exp.line_col());
                                                        err.debug("ignore the variable");
                                                }
                                        } else if (statement instanceof VariableDef) {
                                                VariableDef v = (VariableDef) statement;
                                                variableDefList.add(v);

                                                set.add(v.getName());
                                        } else {
                                                err.UnexpectedTokenException("variable", statement.toString(), statement.line_col());
                                                err.debug("ignore the variable");
                                        }
                                }

                                nextNode(false);
                        }
                } else {
                        // it's a valid name
                        assert isValidName(((Element) current).getContent());
                        String name = ((Element) current).getContent();
                        set.add(name);
                        variableDefList.add(new VariableDef(name, Collections.emptySet(), Collections.emptySet(), current.getLineCol()));
                }

                nextNode(false); // ->
                nextNode(false);
                // (...)->口

                if (!(current instanceof ElementStartNode)) {
                        err.UnexpectedTokenException("new layer", current.toString(), current.getLineCol());
                }

                List<Statement> stmts = parseElemStart((ElementStartNode) current, true, set, false);
                if (stmts.size() == 1 && stmts.get(0) instanceof Expression) {
                        AST.Return ret = new AST.Return((Expression) stmts.get(0), stmts.get(0).line_col());
                        stmts.clear();
                        stmts.add(ret);
                }

                AST.Lambda l = new AST.Lambda(variableDefList, stmts, lineCol);
                nextNode(true);

                parsedExps.push(l);

                parse_expression();
        }

        /**
         * check whether the parsedExps stack is empty
         *
         * @param tokenNode current token
         * @throws UnexpectedTokenException compiling error
         */
        private void parsedExpsNotEmpty(Node tokenNode) throws UnexpectedTokenException {
                if (parsedExps.empty()) {
                        err.UnexpectedTokenException(tokenNode.toString(), tokenNode.getLineCol());
                        throw new ParseFail();
                }
        }

        /**
         * parse 2 variable operations<br>
         * <code>
         * + - * / % = == != &gt; &lt; and other operations
         * </code>
         *
         * @throws SyntaxException compiling error
         */
        private void parse_twoVarOperation() throws SyntaxException {
                Element opNode = (Element) current;
                String op = opNode.getContent();

                LineCol lineCol = current.getLineCol();

                parsedExpsNotEmpty(opNode);
                Expression e1 = parsedExps.pop();

                // check operator priority
                if (!last1VarUnaryOps.empty()) {
                        parsedExps.push(e1);
                        return;
                }
                if (!last2VarOps.empty() && twoVar_higherOrEqual(last2VarOps.peek(), op)) {
                        // e.g. 1+2*3-4
                        // stack:(empty)
                        // 1+ stack:
                        // 2* stack:+ ( + < * )
                        // 3- stack: + * ( * > - )
                        // then return 3 directly
                        // then return 2*3
                        // then return 1+(2*3)
                        // then
                        // 1+(2*3)- stack:
                        // 4 return 4
                        // (1+(2*3))-4
                        parsedExps.push(e1);
                        last2VarOps.pop();
                        return;
                }
                last2VarOps.push(op);
                Expression e2 = next_exp(false);

                TwoVariableOperation tvo = new TwoVariableOperation(op, e1, e2, lineCol);
                parsedExps.push(tvo);

                parse_expression();
        }

        /**
         * parse one variable post operations
         *
         * @throws SyntaxException compiling error
         */
        private void parse_oneVarPostOperation() throws SyntaxException {
                Element opNode = (Element) current;
                String op = opNode.getContent();

                // get exp
                parsedExpsNotEmpty(opNode);
                Expression e = parsedExps.pop();

                OneVariableOperation ovo = new OneVariableOperation(op, e, opNode.getLineCol());
                parsedExps.push(ovo);

                // continue
                nextNode(true);

                parse_expression();
        }

        /**
         * parse one variable pre operations
         *
         * @throws SyntaxException compiling error
         */
        private void parse_oneVarPreOperation() throws SyntaxException {
                Element opNode = (Element) current;
                String op = opNode.getContent();
                last1VarUnaryOps.push(op);

                // get exp
                Expression e = next_exp(false);

                UnaryOneVariableOperation uovo = new UnaryOneVariableOperation(op, e, opNode.getLineCol());
                parsedExps.push(uovo);

                last1VarUnaryOps.pop();

                parse_expression();
        }

        /**
         * parse a package, which will may fill the stack with {@link AST.PackageRef} or {@link AST.Access}
         *
         * @param parse_exp call {@link #parse_expression()} after finished
         * @throws SyntaxException compiling error
         */
        private void parse_package(boolean parse_exp) throws SyntaxException {
                StringBuilder sb = new StringBuilder();
                boolean isName = true;

                LineCol lineCol = current.getLineCol();

                while (current != null && (current instanceof Element) && (((Element) current).getContent().equals("::")
                        ||
                        current.getTokenType() == TokenType.VALID_NAME
                )) {
                        Element elem = (Element) current;
                        String s = elem.getContent();
                        if (!isName && !s.equals("::")) {
                                err.UnexpectedTokenException("::", s, elem.getLineCol());
                                err.debug("let the string be '::'");
                                s = "::";
                        }
                        isName = !isName;
                        sb.append(s);
                        nextNode(true);
                }

                String str = sb.toString();
                AST.PackageRef pkg = new AST.PackageRef(str.substring(0, str.lastIndexOf("::")), lineCol);
                String cls = str.substring(str.lastIndexOf("::") + 2);
                AST.Access access = new AST.Access(pkg, cls, lineCol);
                parsedExps.push(access);

                if (parse_exp) {
                        parse_expression();
                }
        }

        /**
         * parse XXX.xx
         *
         * @param parse_exp invoke {@link #parse_expression()} after finished
         * @throws SyntaxException compiling error
         */
        private void parse_access(boolean parse_exp) throws SyntaxException {
                LineCol lineCol = current.getLineCol();

                parsedExpsNotEmpty(current);
                Expression exp = parsedExps.pop();
                nextNode(false);
                if (current instanceof Element) {
                        String name = ((Element) current).getContent();
                        if (current.getTokenType() != TokenType.VALID_NAME) {
                                err.UnexpectedTokenException("valid name", name, current.getLineCol());
                                err.debug("assume that the token is a valid name");
                        }

                        AST.Access access = new AST.Access(exp, name, lineCol);
                        parsedExps.push(access);

                        nextNode(true);
                        if (parse_exp) {
                                parse_expression();
                        }
                } else {
                        err.UnexpectedTokenException("valid name", current.toString(), current.getLineCol());
                        err.debug("ignore the statement");
                        throw new ParseFail();
                }
        }

        /**
         * parse next expression<br>
         * nextNode(false) would be invoked
         *
         * @param expectingStartNode true if expecting start node
         * @return expression
         * @throws SyntaxException compiling error
         */
        private Expression next_exp(boolean expectingStartNode) throws SyntaxException {
                nextNode(false);
                return get_exp(expectingStartNode);
        }

        /**
         * parse current expression
         *
         * @param expectingStartNode true if expecting start node
         * @return expression
         * @throws SyntaxException compiling error
         */
        private Expression get_exp(boolean expectingStartNode) throws SyntaxException {
                // set expecting start node
                if (expectingStartNode) {
                        this.expectingStartNode = true;
                }
                parse_expression();
                // reset expecting start node
                if (expectingStartNode) {
                        this.expectingStartNode = false;
                }
                return parsedExps.pop();
        }

        /**
         * annotation set {@link #annos} should be empty
         */
        private void annosIsEmpty() throws SyntaxException {
                if (!annos.isEmpty()) {
                        LineCol lineCol = null;
                        for (AST.Anno a : annos) {
                                if (lineCol == null || a.line_col().line < lineCol.line) {
                                        lineCol = a.line_col();
                                } else if (a.line_col().line == lineCol.line) {
                                        if (a.line_col().column < lineCol.column) {
                                                lineCol = a.line_col();
                                        }
                                }
                        }
                        err.SyntaxException("annotations are not presented at correct position", lineCol);
                        err.debug("clear the annotation set");
                        annos.clear();
                }
        }

        /**
         * modifier set {@link #modifiers} shoud be empty
         *
         * @throws SyntaxException compiling error
         */
        private void modifiersIsEmpty() throws SyntaxException {
                if (!modifiers.isEmpty()) {
                        LineCol lineCol = null;
                        for (Modifier m : modifiers) {
                                if (lineCol == null || m.line_col().line < lineCol.line) {
                                        lineCol = m.line_col();
                                } else if (m.line_col().line == lineCol.line) {
                                        if (m.line_col().column < lineCol.column) {
                                                lineCol = m.line_col();
                                        }
                                }
                        }
                        err.SyntaxException("modifiers are not in the right position", lineCol);
                        err.debug("clear the modifier set");
                        modifiers.clear();
                }
        }

        /**
         * parse assign
         *
         * @throws SyntaxException compiling error
         */
        private void parse_assign() throws SyntaxException {
                String op = ((Element) current).getContent();

                parsedExpsNotEmpty(current);
                Expression exp = parsedExps.pop();

                LineCol lineCol = current.getLineCol();
                if (exp instanceof AST.Access) {
                        AST.Access access = (AST.Access) exp;
                        if (access.exp == null && !usedVarNames.contains(access.name) && op.equals("=")) {
                                // define a new variable
                                VariableDef def = new VariableDef(access.name, modifiers, annos, access.line_col());
                                annos.clear();
                                modifiers.clear();
                                usedVarNames.add(access.name);

                                Expression e = next_exp(false);

                                def.setInit(e);
                                parsedExps.push(def);

                        } else {
                                Expression e = next_exp(false);
                                AST.Assignment a = new AST.Assignment((AST.Access) exp, op, e, lineCol);

                                parsedExps.push(a);
                        }
                } else if (exp instanceof AST.Index) {
                        AST.Index index = (AST.Index) exp;
                        Expression e = next_exp(false);
                        AST.Assignment a = new AST.Assignment(new AST.Access(index, null, index.line_col()), op, e, lineCol);

                        parsedExps.push(a);
                } else if (exp instanceof VariableDef) {
                        VariableDef def = (VariableDef) exp;

                        Expression e = next_exp(false);
                        def.setInit(e);

                        parsedExps.push(def);
                } else {
                        err.UnexpectedTokenException("assignable variable", exp.toString(), exp.line_col());
                        err.debug("ignore the assign statement");
                        throw new ParseFail();
                }

                parse_expression();
        }

        /**
         * parse modifiers
         *
         * @throws SyntaxException compiling error
         */
        private void parse_modifier() throws SyntaxException {
                Element elem = (Element) current;
                String modifier = elem.getContent();
                if (modifierIsCompatible(modifier, modifiers)) {
                        modifiers.add(new Modifier(getModifierFromString(modifier), current.getLineCol()));
                } else {
                        err.UnexpectedTokenException("valid modifier", modifier, elem.getLineCol());
                        err.debug("ignore this modifier");
                }
        }

        /**
         * parse variable
         *
         * @throws SyntaxException compiling error
         */
        private void parse_var() throws SyntaxException {
                String content = ((Element) current).getContent();
                if (!modifiers.isEmpty() || !annos.isEmpty()) {
                        // define
                        if (usedVarNames.contains(content)) {
                                err.DuplicateVariableNameException(content, current.getLineCol());
                                err.debug("assume that it's an unused name");
                        }

                        VariableDef def = new VariableDef(content, modifiers, annos, current.getLineCol());
                        annos.clear();
                        modifiers.clear();
                        usedVarNames.add(content);
                        parsedExps.push(def);

                } else {
                        AST.Access access = new AST.Access(null, content, current.getLineCol());
                        parsedExps.push(access);
                }

                nextNode(true);
                parse_expression();
        }

        /**
         * parse class for type specification
         *
         * @return Access
         * @throws SyntaxException compiling error
         */
        private AST.Access parse_cls_for_type_spec() throws SyntaxException {
                AST.Access a;

                // parse array
                // []Type or [][]Type ...
                int arrayDepth = 0;
                while (((Element) current).getContent().equals("[")) {
                        nextNode(false);
                        expecting("]", current.previous(), current, err);
                        nextNode(false);
                        ++arrayDepth;
                }
                if (isPackage((Element) current)) {
                        parse_package(false);

                        while (current instanceof Element && ((Element) current).getContent().equals(".")) {
                                // package::name::Class.Inner
                                parse_access(false);
                        }
                        // i:pkg::Cls.[Inner]
                        a = (AST.Access) parsedExps.pop();

                } else if (current.getTokenType() == TokenType.VALID_NAME
                        ||
                        isPrimitive(((Element) current).getContent())) {
                        // Cls.Inner
                        AST.Access access = new AST.Access(null, ((Element) current).getContent(), current.getLineCol());
                        parsedExps.push(access);
                        nextNode(true);

                        while (current instanceof Element && ((Element) current).getContent().equals(".")) {
                                // package::name::Class.[Inner]
                                parse_access(false);
                        }

                        a = (AST.Access) parsedExps.pop();
                } else {
                        err.UnexpectedTokenException("type", ((Element) current).getContent(), current.getLineCol());
                        err.debug("assume that the token is Object");
                        a = new AST.Access(null, "Object", LineCol.SYNTHETIC);
                }

                for (int i = 0; i < arrayDepth; ++i) {
                        a = new AST.Access(a, "[]", a.line_col());
                }

                return a;
        }

        /**
         * parse type specification (:)<br>
         * the result would be VariableDef
         *
         * @throws SyntaxException compiling error
         */
        private void parse_type_spec() throws SyntaxException {
                LineCol lineCol = current.getLineCol();

                parsedExpsNotEmpty(current);
                Expression v = parsedExps.pop();

                if (v instanceof AST.Access) {
                        if (((AST.Access) v).exp != null) {
                                err.UnexpectedTokenException("variable definition", v.toString(), v.line_col());
                                err.debug("ignore current statement");
                                throw new ParseFail();
                        }

                        String name = ((AST.Access) v).name;
                        if (usedVarNames.contains(name)) {
                                err.DuplicateVariableNameException(name, v.line_col());
                                err.debug("assume that it's an unused name");
                        }
                        v = new VariableDef(name, modifiers, annos, v.line_col());
                        annos.clear();
                        usedVarNames.add(name);
                        modifiers.clear();
                }

                if (!(v instanceof VariableDef)) {
                        err.UnexpectedTokenException("variable", v.toString(), v.line_col());
                        err.debug("ignore current statement");
                        throw new ParseFail();
                }

                nextNode(false);
                if (current instanceof Element) {
                        AST.Access a = parse_cls_for_type_spec();
                        ((VariableDef) v).setType(a);
                } else {
                        err.UnexpectedTokenException("type", current.toString(), current == null ? lineCol : current.getLineCol());
                        err.debug("ignore current statement");
                        throw new ParseFail();
                }

                parsedExps.push(v);
                parse_expression();
        }
}
