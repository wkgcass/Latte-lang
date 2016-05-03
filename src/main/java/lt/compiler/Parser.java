package lt.compiler;

import lt.compiler.lexical.ElementStartNode;
import lt.compiler.lexical.EndingNode;
import lt.compiler.lexical.Node;
import lt.compiler.syntactic.def.ClassDef;
import lt.compiler.syntactic.def.InterfaceDef;
import lt.compiler.syntactic.def.MethodDef;
import lt.compiler.syntactic.def.VariableDef;
import lt.compiler.syntactic.literal.BoolLiteral;
import lt.compiler.syntactic.literal.NumberLiteral;
import lt.compiler.syntactic.literal.StringLiteral;
import lt.compiler.syntactic.operation.TwoVariableOperation;
import lt.compiler.syntactic.pre.Import;
import lt.compiler.syntactic.pre.Modifier;
import lt.compiler.syntactic.pre.PackageDeclare;
import lt.compiler.lexical.Element;
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
         * <b>state</b> <br>
         * when parsing an expression and reaching a start node, if this variable is true, return normally. else throw exception
         */
        private boolean expectingStartNode = false;
        /**
         * <b>state</b>  <br>
         * is parsing try <br>
         * the strategies of parsing <tt>try-catch-finally</tt> is different from parsing normal code blocks
         */
        private boolean isParsingTry = false;
        /**
         * <b>state</b>  <br>
         * is parsing map literal
         */
        private boolean isParsingMap = false;
        /**
         * <b>state</b> <br>
         * is parsing operator like invocation
         */
        private boolean isParsingOperatorLikeInvocation = false;

        /**
         * init the syntactic processor with element start node
         *
         * @param root root node
         */
        public Parser(ElementStartNode root) {
                this.current = root.getLinkedNode();
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

                                parse_expression(); // get key (one before ':')
                                Expression key = parsedExps.pop(); // the key is later pushed into the stack

                                nextNode(false);

                                parse_expression(); // get value (one after ':')
                                Expression value = parsedExps.pop();

                                // add k,v into list, and return.
                                // parse_map would take the list[0,2,4,6,8,...] as key, and list[1,3,5,7,9,...] as value
                                list.add(key);
                                list.add(value);

                                nextNode(true);

                                assert last1VarUnaryOps.empty();
                                last2VarOps.clear();
                        } else {
                                // common handling process
                                Statement stmt = parse_statement(); // invoke parse_statement to get one statement
                                if (current == null && stmt == null)
                                        break; // break when reaching the end of nodes
                                if (!parsedExps.empty()) {
                                        // check parsedExps stack
                                        // the stack should be empty because the parse_statement() method would
                                        // take the existing expression from the stack
                                        // and the stack should finally be filled with only one expression after
                                        // invoking parse_expression()

                                        // generate exception cause message
                                        StringBuilder sb = new StringBuilder();
                                        for (Expression e : parsedExps) {
                                                sb.append(e.toString()).append(" at ").append(e.line_col().fileName).append("(").append(e.line_col().line).append(",").append(e.line_col().column).append(")\n");
                                        }
                                        // it should be a bug
                                        // this err only appears only because parse_x methods don't correctly stop the parsing process when meet an invalid input
                                        throw new LtBug("parsed expression stack should be empty, but got\n" + sb + "and got statement " + stmt);
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
                if (current == null) return;
                Node next = current.next();
                if (next == null || (next instanceof EndingNode && ((EndingNode) next).getType() == EndingNode.STRONG)) {
                        if (!canBeEnd) {
                                throw new UnexpectedEndException(current.getLineCol());
                        }
                }
                current = next;
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
                if (isParsingTry) {
                        annosIsEmpty();
                        modifiersIsEmpty();
                        // specially handled : try
                        // syntax of 'catch' is presented as:
                        // catch <exception variable>
                        //     <Exception Type>[,...]
                        //         handling process

                        // as a result, 'catch' is differed from any other processes
                        // this branch of code would be invoked when current node is Element(<Exception Type>) node
                        LineCol lineCol = current.getLineCol();

                        List<AST.Access> exceptionTypes = new ArrayList<>();
                        while (current instanceof Element) {
                                Expression exp = get_exp(true); // get expression
                                if (exp instanceof AST.Access) {
                                        // the expression has to be Access
                                        // e.g. java::lang::Exception or Exception are all parsed into Access
                                        exceptionTypes.add((AST.Access) exp);
                                } else {
                                        throw new UnexpectedTokenException("exception type", exp.toString(), exp.line_col());
                                }

                                // current node might be ending node
                                // if it's STRONG, means there are other Exception Types to go
                                // or it's WEAK or null, means there's no Exception Type to parse and no exception handling process
                                // else, it should be ElementStartNode
                                if (current instanceof EndingNode && ((EndingNode) current).getType() == EndingNode.STRONG) {
                                        nextNode(true);
                                }
                        }

                        // no exception handling process
                        if (current == null || current instanceof EndingNode) {
                                // the catch is not necessary, it might not exist
                                // so if reaching an ending node and no exception type found, it should return null
                                if (exceptionTypes.isEmpty()) {
                                        return null;
                                } else {
                                        // only catch but no handling process
                                        return new AST.Try.Catch(exceptionTypes, Collections.emptyList(), lineCol);
                                }
                        }
                        // parse element start node (the exception handling process)
                        if (current instanceof ElementStartNode) {
                                return new AST.Try.Catch(
                                        exceptionTypes,
                                        parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false, false),
                                        lineCol);
                        } else {
                                throw new UnexpectedTokenException(current.toString(), current.getLineCol());
                        }
                } else {
                        // common process
                        if (current instanceof Element) {
                                String content = ((Element) current).getContent(); // get content

                                if (isSync((Element) current)) {
                                        annosIsEmpty();
                                        modifiersIsEmpty();

                                        return parse_synchronized();
                                } else if (isModifier(content) && !((Element) current).isValidName) {

                                        parse_modifier();
                                        return null;

                                } else if (content.equals("if")) {
                                        annosIsEmpty();
                                        modifiersIsEmpty();

                                        return parse_if();
                                } else if (content.equals("for")) {
                                        annosIsEmpty();
                                        modifiersIsEmpty();

                                        return parse_for();
                                } else if (content.equals("do")) {
                                        annosIsEmpty();
                                        modifiersIsEmpty();

                                        return parse_do_while();
                                } else if (content.equals("while")) {
                                        annosIsEmpty();
                                        modifiersIsEmpty();

                                        return parse_while();
                                } else if (content.equals("static")) {
                                        annosIsEmpty();
                                        modifiersIsEmpty();

                                        LineCol lineCol = current.getLineCol();
                                        if (current.next() instanceof ElementStartNode) {
                                                // static
                                                //     ...
                                                nextNode(false);
                                                return new AST.StaticScope(
                                                        parseElemStart((ElementStartNode) current, false, Collections.emptySet(), false, false),
                                                        lineCol);

                                        } else if (current.next() instanceof Element) {
                                                // static ...
                                                nextNode(false);
                                                Element curr = (Element) current;
                                                Statement stmt = parse_statement();
                                                if (stmt == null) throw new UnexpectedTokenException("a valid statement", curr.toString(), curr.getLineCol());
                                                return new AST.StaticScope(Collections.singletonList(stmt), lineCol);

                                        } else {
                                                // static
                                                // and no other statements/expressions
                                                return null;
                                        }
                                } else if (content.equals("class")) {
                                        return parse_class();
                                } else if (content.equals("interface")) {
                                        return parse_interface();
                                } else if (content.equals("...")) {
                                        return new AST.Pass(current.getLineCol());
                                } else if (content.equals("try")) {
                                        annosIsEmpty();
                                        modifiersIsEmpty();

                                        return parse_try();
                                } else if (content.equals("throw")) {
                                        annosIsEmpty();
                                        modifiersIsEmpty();

                                        return parse_throw();
                                } else if (content.equals("@")) {
                                        modifiersIsEmpty();

                                        parse_anno();
                                        return null;
                                } else if (content.equals("<")) {
                                        annosIsEmpty();
                                        modifiersIsEmpty();

                                        // return
                                        LineCol lineCol = current.getLineCol();

                                        if (!(current.next() instanceof Element)) {
                                                return new AST.Return(null, lineCol);
                                        } else {
                                                Expression e = next_exp(false);
                                                return new AST.Return(e, lineCol);
                                        }

                                } else if (content.equals("#")) {
                                        modifiersIsEmpty();

                                        // package declare
                                        return parse_pkg_declare();
                                } else if (content.equals("#>")) {
                                        annosIsEmpty();
                                        modifiersIsEmpty();

                                        return parse_pkg_import();
                                } else {
                                        // check whether is method def
                                        int def_method_type = checkMethodDef((Element) current);
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
                                        } else {
                                                // parse expression until the EndingNode or null
                                                while (true) {
                                                        parse_expression();
                                                        if (current == null || !(current instanceof Element)) {
                                                                if (parsedExps.empty()) return null;
                                                                return parsedExps.pop();
                                                        }
                                                }
                                        }
                                }
                        } else {
                                // not element, go on and try to parse again
                                nextNode(true);
                                return parse_statement();
                        }
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
                expecting("(", current.previous(), current);

                List<AST.Access> expressions = new ArrayList<>();
                if (current.next() instanceof ElementStartNode) {
                        nextNode(false);
                        List<Statement> statements = parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false, false);

                        for (Statement s : statements) {
                                if (s instanceof AST.Access) {
                                        expressions.add((AST.Access) s);
                                } else {
                                        throw new UnexpectedTokenException("object reference", s.toString(), s.line_col());
                                }
                        }
                }

                nextNode(false);
                expecting(")", current.previous(), current);
                nextNode(true);
                List<Statement> statements = null;
                if (current instanceof ElementStartNode) {
                        statements = parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false, false);
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
         * @param parseTry     set {@link #isParsingTry} to given arg
         * @return parsed result (list of statements)
         * @throws SyntaxException compiling error
         */
        private List<Statement> parseElemStart(
                ElementStartNode startNode,
                boolean addUsedNames,
                Set<String> names,
                boolean parseMap,
                boolean parseTry)
                throws SyntaxException {
                Parser parser = new Parser(startNode);
                if (addUsedNames) {
                        parser.addUsedVarNames(usedVarNames);
                        parser.addUsedVarNames(names);
                }
                parser.isParsingMap = parseMap;
                parser.isParsingTry = parseTry;
                return parser.parse();
        }

        /**
         * parse while<br>
         * <code>
         * while boolean<br>
         * ...
         * </code>
         *
         * @return While
         * @throws SyntaxException compiling error
         */
        private AST.While parse_while() throws SyntaxException {
                LineCol lineCol = current.getLineCol();

                Expression condition = next_exp(true); // the boolean expression

                if (current instanceof ElementStartNode) {
                        // parse while body
                        return new AST.While(condition, parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false, false), false, lineCol);
                } else {
                        throw new UnexpectedTokenException("while body", current.toString(), current.getLineCol());
                }
        }

        /**
         * parse do_while<br>
         * <code>
         * do<br>
         * ...<br>
         * while boolean
         * </code>
         *
         * @return While
         * @throws SyntaxException compiling error
         */
        private AST.While parse_do_while() throws SyntaxException {
                LineCol lineCol = current.getLineCol();

                nextNode(true); // current node should be ElementStartNode

                if (current instanceof ElementStartNode) {
                        List<Statement> statements = parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false, false);
                        nextNode(false);
                        expecting("while", current.previous(), current);

                        Expression condition = next_exp(true); // the boolean expression

                        return new AST.While(condition, statements, true, lineCol);
                } else {
                        throw new UnexpectedTokenException("while body", current.toString(), current.getLineCol());
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
                List<Import.ImportDetail> importDetails = new ArrayList<>();
                nextNode(false);

                if (current instanceof ElementStartNode) {
                        Parser processor = new Parser((ElementStartNode) current);
                        List<Statement> statements = processor.parse();

                        for (Statement stmt : statements) {
                                if (stmt instanceof AST.Access) {
                                        // import should firstly be parsed into Access
                                        AST.Access a = (AST.Access) stmt;
                                        Import.ImportDetail detail;
                                        if (a.name.equals("_")) { // ends with '_'
                                                if (a.exp instanceof AST.PackageRef) {
                                                        // import all from a package
                                                        detail = new Import.ImportDetail((AST.PackageRef) a.exp, null, true);
                                                } else {
                                                        // import static
                                                        detail = new Import.ImportDetail(null, (AST.Access) a.exp, true);
                                                }
                                        } else {
                                                // import class or inner class
                                                detail = new Import.ImportDetail(null, a, false);
                                        }

                                        importDetails.add(detail);
                                } else {
                                        throw new UnexpectedTokenException("import statement", stmt.toString(), stmt.line_col());
                                }
                        }
                } else {
                        throw new UnexpectedTokenException("import statements", current.toString(), current.getLineCol());
                }

                return new Import(importDetails, lineCol);
        }

        /**
         * declare a package<br>
         * <code>
         * # java::util
         * </code>
         *
         * @return PackageDeclare
         * @throws SyntaxException
         */
        private PackageDeclare parse_pkg_declare() throws SyntaxException {
                LineCol lineCol = current.getLineCol();

                nextNode(false);
                if (current instanceof ElementStartNode) {
                        Node pkgNode = ((ElementStartNode) current).getLinkedNode();
                        if (pkgNode instanceof Element) {
                                StringBuilder sb = new StringBuilder();
                                boolean isName = true;

                                while (pkgNode != null && (pkgNode instanceof Element) && (((Element) pkgNode).getContent().equals("::")
                                        ||
                                        ((Element) pkgNode).isValidName)) {
                                        Element elem = (Element) pkgNode;
                                        String s = elem.getContent();
                                        if (!isName && !s.equals("::")) {
                                                throw new UnexpectedTokenException("::", s, elem.getLineCol());
                                        }
                                        isName = !isName;
                                        sb.append(s);
                                        pkgNode = pkgNode.next();
                                }

                                AST.PackageRef pkg = new AST.PackageRef(sb.toString(), lineCol);
                                return new PackageDeclare(pkg, lineCol);
                        } else {
                                throw new UnexpectedTokenException("package", pkgNode.toString(), pkgNode.getLineCol());
                        }
                } else {
                        throw new UnexpectedTokenException(current.toString(), current.getLineCol());
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
                Expression e = next_exp(false); // annotation
                // might be Invocation
                // might be Access

                AST.Anno anno;
                if (e instanceof AST.Invocation) {
                        AST.Invocation inv = (AST.Invocation) e;
                        List<AST.Assignment> assignments = new ArrayList<>();
                        for (Expression exp : inv.args) {
                                // convert into assignments
                                if (exp instanceof VariableDef) {
                                        VariableDef v = (VariableDef) exp;

                                        // only takes literal or Array
                                        if (v.getInit() instanceof Literal || v.getInit() instanceof AST.ArrayExp) {
                                                AST.Assignment a = new AST.Assignment(
                                                        new AST.Access(null, v.getName(), v.line_col()),
                                                        "=",
                                                        v.getInit(), v.line_col());
                                                assignments.add(a);
                                        } else {
                                                throw new UnexpectedTokenException("literal or array", v.toString(), v.line_col());
                                        }
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
                        anno = new AST.Anno(inv.access, assignments, lineCol);
                } else if (e instanceof AST.Access) {
                        anno = new AST.Anno((AST.Access) e, Collections.emptyList(), e.line_col());
                } else {
                        throw new UnexpectedTokenException("annotation definition", e.toString(), e.line_col());
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

                if (current.next() instanceof EndingNode) {
                        throw new SyntaxException("invalid try statement", current.next().getLineCol());
                }

                nextNode(true);

                List<Statement> statements = null;
                if (current instanceof ElementStartNode) {
                        statements = parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false, false);

                        nextNode(true);
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
                List<AST.Try.Catch> catchList = new ArrayList<>();
                if (current instanceof Element) {
                        String cat = ((Element) current).getContent();
                        // catch
                        if (cat.equals("catch")) {
                                nextNode(false);

                                if (current instanceof Element) {
                                        eName = ((Element) current).getContent(); // catch e
                                        if (((Element) current).isValidName) {
                                                if (usedVarNames.contains(eName)) {
                                                        throw new DuplicateVariableNameException(eName, current.getLineCol());
                                                } else {
                                                        // catch e
                                                        nextNode(true);
                                                        if (!(current instanceof EndingNode)) {
                                                                if (current instanceof ElementStartNode) {
                                                                        List<Statement> catches = parseElemStart(
                                                                                (ElementStartNode) current,
                                                                                true,
                                                                                new HashSet<>(Collections.singletonList(eName)), // the exception holder name
                                                                                false, true);

                                                                        for (Statement stmt : catches) {
                                                                                if (stmt instanceof AST.Try.Catch) {
                                                                                        catchList.add((AST.Try.Catch) stmt);
                                                                                } else {
                                                                                        throw new UnexpectedTokenException("catch statements", stmt.toString(), stmt.line_col());
                                                                                }
                                                                        }

                                                                        nextNode(true);
                                                                        // if it's finally then go next
                                                                        // else just return, let invoker (parse_statement()) to invoke nextNode()
                                                                        if (current instanceof EndingNode
                                                                                && current.next() instanceof Element
                                                                                && ((Element) current.next()).getContent().equals("finally")) {
                                                                                nextNode(false);
                                                                        }
                                                                } else {
                                                                        // element
                                                                        throw new UnexpectedTokenException(current.toString(), current.getLineCol());
                                                                }
                                                        }
                                                }
                                        } else {
                                                throw new UnexpectedTokenException("valid variable name", eName, current.getLineCol());
                                        }
                                } else {
                                        throw new UnexpectedTokenException("exception variable", current.toString(), current.getLineCol());
                                }
                        }
                }

                List<Statement> fin = new ArrayList<>();
                if (current instanceof Element) {
                        String f = ((Element) current).getContent();
                        // finally
                        if (f.equals("finally")) {
                                nextNode(true);
                                if (current instanceof ElementStartNode) {
                                        fin = parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false, false);
                                }
                        }
                }

                return new AST.Try(
                        statements == null ? Collections.emptyList() : statements,
                        eName,
                        catchList,
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
         * @throws SyntaxException
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
                        if (((Element) current).isValidName) {
                                nextNode(true); // can be : or ending or startNode
                                // interface name :
                                List<AST.Access> accesses = new ArrayList<>();

                                if (current instanceof Element) {
                                        expecting(":", current.previous(), current);

                                        nextNode(false);
                                        while (true) {
                                                if (current instanceof Element && ((Element) current).isValidName) {
                                                        Expression e = get_exp(true);

                                                        if (e instanceof AST.Access) {
                                                                accesses.add((AST.Access) e);
                                                        } else {
                                                                throw new UnexpectedTokenException("super interface", e.toString(), e.line_col());
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

                                List<Statement> statements = null;
                                if (current instanceof ElementStartNode) {
                                        // interface name
                                        //     ...
                                        statements = parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false, false);
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
                                throw new UnexpectedTokenException("valid interface name", name, current.getLineCol());
                        }
                } else {
                        throw new UnexpectedTokenException("interface name", current.toString(), current.getLineCol());
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
                        if (((Element) current).isValidName) {
                                List<VariableDef> params = null;

                                Set<String> newParamNames = new HashSet<>();
                                nextNode(true); // can be ( or : or ending or ending or startNode
                                if (current instanceof Element) {
                                        String p = ((Element) current).getContent();
                                        switch (p) {
                                                case "(":
                                                        nextNode(false);
                                                        if (current instanceof ElementStartNode) {
                                                                // class ClassName(口
                                                                Parser processor = new Parser((ElementStartNode) current);
                                                                List<Statement> list = processor.parse();

                                                                params = new ArrayList<>();
                                                                boolean MustHaveInit = false;
                                                                for (Statement stmt : list) {
                                                                        if (stmt instanceof AST.Access) {
                                                                                if (MustHaveInit) {
                                                                                        throw new SyntaxException("parameter with init", stmt.line_col());
                                                                                }
                                                                                AST.Access access = (AST.Access) stmt;
                                                                                if (access.exp != null) {
                                                                                        throw new UnexpectedTokenException("param def", access.toString(), access.line_col());
                                                                                } else {
                                                                                        VariableDef v = new VariableDef(access.name, Collections.emptySet(), annos, current.getLineCol());
                                                                                        annos.clear();
                                                                                        params.add(v);

                                                                                        newParamNames.add(v.getName());
                                                                                }
                                                                        } else if (stmt instanceof VariableDef) {
                                                                                if (((VariableDef) stmt).getInit() == null) {
                                                                                        if (MustHaveInit) {
                                                                                                throw new SyntaxException("parameter with init", stmt.line_col());
                                                                                        }
                                                                                } else {
                                                                                        MustHaveInit = true;
                                                                                }

                                                                                params.add((VariableDef) stmt);

                                                                                newParamNames.add(((VariableDef) stmt).getName());
                                                                        } else {
                                                                                throw new UnexpectedTokenException("param def", stmt.toString(), stmt.line_col());
                                                                        }
                                                                }

                                                                nextNode(false); // )
                                                                if (current instanceof Element) {
                                                                        String rightP = ((Element) current).getContent();
                                                                        if (rightP.equals(")")) {
                                                                                nextNode(true);
                                                                        } else {
                                                                                throw new UnexpectedTokenException(")", rightP, current.getLineCol());
                                                                        }
                                                                } else {
                                                                        throw new UnexpectedTokenException(")", current.toString(), current.getLineCol());
                                                                }
                                                        } else if (current instanceof Element) {
                                                                expecting(")", current.previous(), current);
                                                                // class ClassName()

                                                                params = Collections.emptyList();
                                                                nextNode(true);
                                                        } else {
                                                                throw new UnexpectedTokenException(current.toString(), current.getLineCol());
                                                        }
                                                        break;
                                                case ":":
                                                        // do nothing
                                                        break;
                                                default:
                                                        throw new UnexpectedTokenException("( or :", p, current.getLineCol());
                                        }
                                }

                                AST.Invocation invocation = null;
                                List<AST.Access> accesses = new ArrayList<>();

                                if (current instanceof Element) {
                                        // :
                                        expecting(":", current.previous(), current);
                                        nextNode(false);
                                        while (true) {
                                                if (current instanceof Element && ((Element) current).isValidName) {
                                                        Expression e = get_exp(true);

                                                        if (e instanceof AST.Access) {
                                                                accesses.add((AST.Access) e);
                                                        } else if (e instanceof AST.Invocation) {
                                                                if (invocation == null) {
                                                                        invocation = (AST.Invocation) e;
                                                                } else {
                                                                        throw new SyntaxException("Multiple Inheritance is not allowed", e.line_col());
                                                                }
                                                        } else {
                                                                throw new UnexpectedTokenException("super class or super interfaces", e.toString(), e.line_col());
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
                                        stmts = parseElemStart((ElementStartNode) current, true, newParamNames, false, false);
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
                                throw new UnexpectedTokenException("valid class name", name, current.getLineCol());
                        }
                } else {
                        throw new UnexpectedTokenException("class name", current.toString(), current.getLineCol());
                }
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
                Element varElem = (Element) current;
                String varName = varElem.getContent();
                if (!varElem.isValidName) {
                        throw new UnexpectedTokenException("valid variable name", varName, current.getLineCol());
                }
                if (usedVarNames.contains(varName)) {
                        throw new DuplicateVariableNameException(varName, current.getLineCol());
                }

                nextNode(false); // in
                expecting("in", current.previous(), current);

                Expression exp = next_exp(true); // expression

                List<Statement> statements = null;
                if (current instanceof ElementStartNode) {
                        Parser processor = new Parser((ElementStartNode) current);

                        Set<String> set = new HashSet<>(usedVarNames);
                        set.add(varName);
                        processor.addUsedVarNames(set);

                        statements = processor.parse();
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
                                nextNode(true);
                        } else {
                                nextNode(false);
                        }
                        if (content.equals("if") || content.equals("elseif")) {

                                if (isLast) {
                                        throw new SyntaxException("if-else had already reached else but got " + content + " instead", current.getLineCol());
                                }

                                condition = get_exp(true);
                        }

                        List<Statement> list = null;
                        if (current instanceof ElementStartNode) {
                                list = parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false, false);
                        }

                        if (condition == null)
                                isLast = true;

                        AST.If.IfPair pair = new AST.If.IfPair(condition, list == null ? Collections.emptyList() : list, ifPairLineCol);
                        pairs.add(pair);

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
                expecting("(", current.previous(), current);
                nextNode(false);
                if (current instanceof ElementStartNode) {
                        // method(口
                        Parser processor = new Parser((ElementStartNode) current);
                        List<Statement> statements = processor.parse();
                        boolean MustHaveInit = false;
                        for (Statement s : statements) {
                                if (s instanceof AST.Access && ((AST.Access) s).exp == null) {
                                        if (MustHaveInit) {
                                                throw new SyntaxException("parameter with init value", s.line_col());
                                        }

                                        AST.Access access = (AST.Access) s;
                                        VariableDef d = new VariableDef(access.name, Collections.emptySet(), annos, access.line_col());
                                        annos.clear();
                                        variableList.add(d);
                                        names.add(access.name);
                                } else if (s instanceof VariableDef) {
                                        if (((VariableDef) s).getInit() == null) {
                                                if (MustHaveInit)
                                                        throw new SyntaxException("parameter with init value", s.line_col());
                                        } else {
                                                MustHaveInit = true;
                                        }
                                        variableList.add((VariableDef) s);
                                        names.add(((VariableDef) s).getName());
                                } else throw new UnexpectedTokenException("parameter", s.toString(), s.line_col());
                        }
                        nextNode(false);
                }
                expecting(")", current.previous(), current);
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

                Parser processor = new Parser((ElementStartNode) current);
                names.addAll(usedVarNames);
                processor.addUsedVarNames(names);
                List<Statement> stmts = processor.parse();
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

                List<VariableDef> variableList = new ArrayList<>();
                Set<String> names = new HashSet<>();
                parse_method_def_variables(variableList, names);
                // method(..)
                nextNode(false); // method(..)=
                nextNode(false); // method(..)=pass
                nextNode(true);

                MethodDef def = new MethodDef(methodName, modifiers, null, variableList, annos,
                        Collections.emptyList(),
                        lineCol);
                annos.clear();
                modifiers.clear();
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

                List<VariableDef> variableList = new ArrayList<>();
                Set<String> names = new HashSet<>();
                parse_method_def_variables(variableList, names);
                // method(..)
                nextNode(false); // method(..)=
                nextNode(false); // method(..)=exp

                parse_expression();

                Expression exp = parsedExps.pop();
                MethodDef def = new MethodDef(methodName, modifiers, null, variableList, annos, Collections.singletonList(
                        new AST.Return(exp, exp.line_col())
                ), lineCol);
                annos.clear();
                modifiers.clear();
                return def;
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
                        nextNode(false); // value
                        parse_expression();
                        Expression exp = parsedExps.pop();
                        if (!(current instanceof EndingNode || current == null)) {
                                throw new UnexpectedTokenException("EndingNode", current.toString(), current.getLineCol());
                        }
                        MethodDef def = new MethodDef(methodName, modifiers, returnType, variableList, annos,
                                Collections.singletonList(
                                        new AST.Return(exp, exp.line_col())
                                ),
                                lineCol);
                        annos.clear();
                        modifiers.clear();
                        return def;
                } else {
                        if (current instanceof ElementStartNode) {
                                // initialize processor
                                Parser processor = new Parser((ElementStartNode) current);
                                names.addAll(usedVarNames);
                                processor.addUsedVarNames(names);
                                // parse
                                List<Statement> list = processor.parse();
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

                                        if (isNumber(content)) {
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                NumberLiteral numberLiteral = new NumberLiteral(content, current.getLineCol());

                                                parsedExps.push(numberLiteral);
                                                nextNode(true);
                                                parse_expression();

                                        } else if (isBoolean(content)) {
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                BoolLiteral boolLiteral = new BoolLiteral(content, current.getLineCol());

                                                parsedExps.push(boolLiteral);
                                                nextNode(true);
                                                parse_expression();

                                        } else if (isString(content)) {
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                StringLiteral stringLiteral = new StringLiteral(content, current.getLineCol());

                                                parsedExps.push(stringLiteral);
                                                nextNode(true);
                                                parse_expression();

                                        } else if (content.equals("type")) {
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                LineCol lineCol = current.getLineCol();
                                                nextNode(false);
                                                AST.Access access = parse_cls_for_type_spec();
                                                parsedExps.push(new AST.TypeOf(access, lineCol));
                                                // TODO nextNode(true);
                                                parse_expression();

                                        } else if (content.equals("null")) {

                                                annosIsEmpty();
                                                modifiersIsEmpty();
                                                parsedExps.push(new AST.Null(current.getLineCol()));
                                                nextNode(true);
                                                parse_expression();

                                        } else if (content.equals(".")) {
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

                                        } else if (isTwoVariableOperator(content)) {
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                parse_twoVarOperation();

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

                                                if (parsedExps.empty() || (isParsingMap && parsedExps.size() <= 1)) {
                                                        parse_array_exp();
                                                } else {
                                                        parse_index_access();
                                                }
                                        } else if (content.equals("{")) {
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                parse_map();

                                        } else if (content.equals("(")) {
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                if (isLambda((Element) current)) {
                                                        parse_lambda();
                                                } else {

                                                        nextNode(false);

                                                        if (current instanceof Element) {
                                                                // element should be ')'
                                                                expecting(")", current.previous(), current);
                                                                if (!parsedExps.empty() && (parsedExps.peek() instanceof AST.Access)) {
                                                                        // method() invocation
                                                                        AST.Access access = (AST.Access) parsedExps.pop();
                                                                        AST.Invocation invocation = new AST.Invocation(access, new Expression[0], access.line_col());
                                                                        parsedExps.push(invocation);
                                                                } else {
                                                                        throw new UnexpectedTokenException(")", current.getLineCol());
                                                                }

                                                                nextNode(true);
                                                                parse_expression();
                                                        } else if (current instanceof ElementStartNode) {
                                                                // element start node : ...(口)...
                                                                ElementStartNode startNode = (ElementStartNode) current;
                                                                List<Statement> statements = parseElemStart(startNode, false, Collections.emptySet(), false, false);

                                                                if (!statements.isEmpty()) {
                                                                        if (!parsedExps.empty() && (parsedExps.peek() instanceof AST.Access)) {
                                                                                // method(...)
                                                                                AST.Access access = (AST.Access) parsedExps.pop();
                                                                                Expression[] args = new Expression[statements.size()];
                                                                                for (int i = 0; i < statements.size(); ++i) {
                                                                                        Statement stmt = statements.get(i);
                                                                                        if (!(stmt instanceof Expression)) {
                                                                                                throw new UnexpectedTokenException("expression", stmt.toString(), stmt.line_col());
                                                                                        }
                                                                                        args[i] = (Expression) stmt;
                                                                                }

                                                                                AST.Invocation invocation = new AST.Invocation(access, args, current.getLineCol());
                                                                                parsedExps.push(invocation);
                                                                        } else {
                                                                                // something like 3*(1+2)
                                                                                if (statements.size() == 1) {
                                                                                        Statement stmt = statements.get(0);
                                                                                        if (stmt instanceof Expression) {
                                                                                                parsedExps.push((Expression) stmt);
                                                                                        } else if (stmt instanceof AST.Return) {
                                                                                                AST.Procedure procedure = new AST.Procedure(statements, startNode.getLineCol());
                                                                                                parsedExps.push(procedure);
                                                                                        } else {
                                                                                                throw new UnexpectedTokenException("return statement in closure", stmt.toString(), stmt.line_col());
                                                                                        }
                                                                                } else if (statements.size() != 0) {
                                                                                        AST.Procedure procedure = new AST.Procedure(statements, startNode.getLineCol());
                                                                                        parsedExps.push(procedure);
                                                                                } else {
                                                                                        throw new UnexpectedTokenException("closure", startNode.getLineCol());
                                                                                }
                                                                        }
                                                                } else {
                                                                        throw new UnexpectedTokenException("arguments", startNode.toString(), startNode.getLineCol());
                                                                }

                                                                nextNode(false); // should be ')'
                                                                expecting(")", startNode, current);
                                                                nextNode(true);
                                                                parse_expression();
                                                        }
                                                }
                                        } else if (isPackage((Element) current)) {
                                                annosIsEmpty();
                                                modifiersIsEmpty();

                                                parse_package(true);

                                        } else if (((Element) current).isValidName) {
                                                // could be a var if it's the first expression in the exp stack
                                                // or it could be method invocation

                                                if (parsedExps.empty()) {
                                                        parse_var();
                                                } else {
                                                        parse_operator_like_invocation();
                                                }

                                        } else if (content.equals("as")) {

                                                annosIsEmpty();
                                                if (parsedExps.isEmpty()) {
                                                        throw new UnexpectedTokenException("expression", "as", current.getLineCol());
                                                } else {
                                                        LineCol lineCol = current.getLineCol();
                                                        Expression exp = parsedExps.pop();
                                                        nextNode(false);
                                                        AST.Access type = parse_cls_for_type_spec();
                                                        AST.AsType asType = new AST.AsType(exp, type, lineCol);
                                                        parsedExps.push(asType);
                                                }
                                        } else if (content.equals("undefined")) {

                                                annosIsEmpty();
                                                parsedExps.push(new AST.UndefinedExp(current.getLineCol()));
                                                nextNode(true);
                                                parse_expression();

                                        } else {
                                                throw new UnknownTokenException(content, current.getLineCol());
                                        }

                                        break;
                                }
                        }

                } else if (current instanceof ElementStartNode) {
                        if (!expectingStartNode) {
                                throw new UnexpectedNewLayerException(current.getLineCol());
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
                                opArgs.toArray(new Expression[opArgs.size()]),
                                opLineCol);
                        parsedExps.push(invocation);
                } else {
                        // a.op()
                        if (!last2VarOps.empty()) {
                                last2VarOps.pop();
                                parsedExps.push(a);
                                return;
                        }
                        nextNode(true);
                        AST.Invocation invocation = new AST.Invocation(new AST.Access(a, op, opLineCol), new Expression[0], opLineCol);
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
                        expecting("}", current.previous(), current);
                        parsedExps.push(new AST.MapExp(new LinkedHashMap<>(), lineCol));

                        nextNode(true);
                } else {
                        // current instance of ElementStartNode
                        expecting("}", current, current.next() == null ? null : current.next().next());

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
                List<Statement> stmts = parseElemStart(startNode, true, Collections.emptySet(), true, false);
                if (stmts.size() % 2 != 0) {
                        throw new SyntaxException("invalid map contents", startNode.getLineCol());
                }

                boolean isKey = true;
                LinkedHashMap<Expression, Expression> map = new LinkedHashMap<>();
                Expression exp = null;
                for (Statement s : stmts) {
                        if (s instanceof Expression) {
                                if (isKey) {
                                        exp = (Expression) s;
                                } else {
                                        map.put(exp, (Expression) s);
                                }
                                isKey = !isKey;
                        } else {
                                throw new UnexpectedTokenException("expression", s.toString(), s.line_col());
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
                        expecting("]", current.previous(), current);
                        parsedExps.push(new AST.Index(e, Collections.emptyList(), e.line_col()));

                        nextNode(true);
                } else {
                        // current instance of ElementStartNode
                        expecting("]", current, current.next() == null ? null : current.next().next());
                        List<Statement> stmts = parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false, false);
                        List<Expression> exps = new ArrayList<>();

                        for (Statement stmt : stmts) {
                                if (stmt instanceof Expression) {
                                        exps.add((Expression) stmt);
                                } else {
                                        throw new UnexpectedTokenException("index access expression", stmt.toString(), stmt.line_col());
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
                        expecting("]", current.previous(), current);
                        parsedExps.push(new AST.ArrayExp(Collections.emptyList(), lineCol));

                        nextNode(true);
                } else {
                        // current instanceof ElementStartNode
                        // [...]
                        expecting("]", current, current.next() == null ? null : current.next().next());
                        List<Statement> stmts = parseElemStart((ElementStartNode) current, true, Collections.emptySet(), false, false);
                        List<Expression> exps = new ArrayList<>();

                        for (Statement stmt : stmts) {
                                if (stmt instanceof Expression) {
                                        exps.add((Expression) stmt);
                                } else {
                                        throw new UnexpectedTokenException("array contents", stmt.toString(), stmt.line_col());
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

                nextNode(false);
                List<VariableDef> variableDefList = new ArrayList<>();
                Set<String> set = new HashSet<>();
                if (current instanceof ElementStartNode) {
                        List<Statement> list = parseElemStart((ElementStartNode) current, false, Collections.emptySet(), false, false);
                        for (Statement statement : list) {
                                if (statement instanceof AST.Access) {
                                        AST.Access access = (AST.Access) statement;
                                        if (access.exp == null) {
                                                VariableDef v = new VariableDef(access.name, Collections.emptySet(), annos, LineCol.SYNTHETIC);
                                                annos.clear();
                                                variableDefList.add(v);

                                                set.add(access.name);
                                        } else {
                                                throw new UnexpectedTokenException("variable", access.exp.toString(), access.exp.line_col());
                                        }
                                } else if (statement instanceof VariableDef) {
                                        VariableDef v = (VariableDef) statement;
                                        variableDefList.add(v);

                                        set.add(v.getName());
                                } else {
                                        throw new UnexpectedTokenException("variable", statement.toString(), statement.line_col());
                                }
                        }

                        nextNode(false);
                }

                nextNode(false); // =>
                nextNode(false);
                // (...)=>口

                Parser pr = new Parser((ElementStartNode) current);
                set.addAll(usedVarNames);
                pr.addUsedVarNames(set);
                List<Statement> stmts = pr.parse();
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

        private void parsedExpsNotEmpty(Node tokenNode) throws UnexpectedTokenException {
                if (parsedExps.empty()) {
                        throw new UnexpectedTokenException(tokenNode.toString(), tokenNode.getLineCol());
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
         * parse a package, which will may fill the stack with {@link AST.PackageRef} or {@link AST.Access} containing one
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
                        ((Element) current).isValidName)) {
                        Element elem = (Element) current;
                        String s = elem.getContent();
                        if (!isName && !s.equals("::")) {
                                throw new UnexpectedTokenException("::", s, elem.getLineCol());
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
                        if (!((Element) current).isValidName) {
                                throw new UnexpectedTokenException("valid name", name, current.getLineCol());
                        }

                        AST.Access access = new AST.Access(exp, name, lineCol);
                        parsedExps.push(access);

                        nextNode(true);
                        if (parse_exp) {
                                parse_expression();
                        }
                } else {
                        throw new UnexpectedTokenException("valid name", current.toString(), current.getLineCol());
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
                        throw new SyntaxException("annotations are not presented at correct position", lineCol);
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
                        throw new SyntaxException("modifiers are not in the right position", lineCol);
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
                        if (access.exp == null && !usedVarNames.contains(access.name)) {
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
                        throw new UnexpectedTokenException("variable", exp.toString(), current.getLineCol());
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
                        modifiers.add(new Modifier(modifier, current.getLineCol()));
                } else {
                        throw new UnexpectedTokenException("valid modifier", modifier, elem.getLineCol());
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
                                throw new DuplicateVariableNameException(content, current.getLineCol());
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
                int arrayDepth = 0;
                while (((Element) current).getContent().equals("[")) {
                        nextNode(false);
                        expecting("]", current.previous(), current);
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

                } else if (((Element) current).isValidName
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
                        throw new UnexpectedTokenException("type", ((Element) current).getContent(), current.getLineCol());
                }

                for (int i = 0; i < arrayDepth; ++i) {
                        a = new AST.Access(a, "[]", a.line_col());
                }

                return a;
        }

        /**
         * parse type specification (:)
         *
         * @throws SyntaxException compiling error
         */
        private void parse_type_spec() throws SyntaxException {
                LineCol lineCol = current.getLineCol();

                parsedExpsNotEmpty(current);
                Expression v = parsedExps.pop();

                if (v instanceof AST.Access) {
                        if (((AST.Access) v).exp != null) {
                                throw new UnexpectedTokenException("variable definition", v.toString(), v.line_col());
                        }

                        String name = ((AST.Access) v).name;
                        if (usedVarNames.contains(name)) {
                                throw new DuplicateVariableNameException(name, v.line_col());
                        }
                        v = new VariableDef(name, modifiers, annos, v.line_col());
                        annos.clear();
                        usedVarNames.add(name);
                        modifiers.clear();
                }

                if (!(v instanceof VariableDef)) {
                        throw new UnexpectedTokenException("variable", v.toString(), v.line_col());
                }

                nextNode(false);
                if (current instanceof Element) {
                        AST.Access a = parse_cls_for_type_spec();
                        ((VariableDef) v).setType(a);
                } else {
                        throw new UnexpectedTokenException("type", current.toString(), current == null ? lineCol : current.getLineCol());
                }

                parsedExps.push(v);
                parse_expression();
        }
}
