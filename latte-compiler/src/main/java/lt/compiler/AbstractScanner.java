package lt.compiler;

import lt.compiler.lexical.*;
import lt.compiler.syntactic.UnknownTokenException;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * the base scanner.
 */
public abstract class AbstractScanner implements Scanner {
        /**
         * the scanner creates a new <b>Layer</b> when meets these strings<br>
         * e.g.<br>
         * <pre>
         * lambda = ()-&gt; a+b
         * </pre>
         * would be considered as the following token tree<br>
         * <pre>
         * [lambda]-[=]-[(]-[)]-[-&gt;]-[|]-[END]
         *                            |
         *                            +-[a]-[+]-[b]
         * </pre>
         */
        public final Set<String> LAYER = new HashSet<>(Arrays.asList("->", "|-"));
        public final Set<String> LAYER_END = new HashSet<>(Collections.singletonList("-|"));
        /**
         * the input should be split when meets these tokens
         */
        public final Set<String> SPLIT_X = new HashSet<>(Arrays.asList(
                ".", // class positioning or method access
                ":", // type specification or generic extends
                "::", // package::package
                "=", // assignment
                "^^", // pow
                "!", "&&", "||", // logic
                "!=", "==", "!==", "===", // equals/reference equals
                "<", ">", "<=", ">=", // comparison or generic extends/super
                "+", "-", "*", "/", "%", // operators
                "++", "--",
                "@", // @Annotation
                "=:=", "!:=", // equal or not$equal
                "..", ".:", // list generator
                "...", // pass
                ":::", // concat
                ":=", // assign
                "#" // generator
        ));
        public final Set<String> SPLIT_TWO_VAR_OP_THAT_CAN_BE_USED_WITH_ASSIGN = new HashSet<>(Arrays.asList(
                "+", "-", "*", "/", "%",
                "<<", ">>", ">>>", // shift
                "&", "^", "|", "~" // bit logic
        ));
        /**
         * symbols that let the scanner know the following input should be scanned as a string<br>
         * a string starts with one of these symbols and ends with the same symbol.
         */
        public final Set<String> STRING = new HashSet<>(Arrays.asList("\"", "'", "`", "//"));
        /**
         * the escape character.
         */
        public static String ESCAPE = "\\";
        /**
         * these tokens split the input, but them themselves won't be recorded into the token tree. e.g.<br>
         * <pre>
         * list add 1
         * </pre>
         * the spaces split the input, but they won't be recorded.
         */
        public final Set<String> NO_RECORD = new HashSet<>(Collections.singletonList(" "));
        /**
         * the str is considered as ending text. Append an EndingNode to the token tree
         */
        public static String ENDING = ",";
        /**
         * comment, strings after the token are ignored
         */
        public static String COMMENT = ";";
        /**
         * multiple line comment start symbol
         */
        public static String MultipleLineCommentStart = "/*";
        /**
         * multiple line comment end symbol
         */
        public static String MultipleLineCommentEnd = "*/";
        /**
         * the scanner creates a new layer when meets a <tt>key</tt> and the layer finishes at corresponding <tt>value</tt><br>
         * <pre>
         * map = {'name':'cass'}
         * </pre>
         * would be considered as the following token tree<br>
         * <pre>
         * [map]-[=]-[{]-[|]-[}]-[END]
         *                |
         *                --['name']-[:]-['cass']
         * </pre>
         */
        public final Map<String, String> PAIR = new HashMap<>();
        /**
         * the input should be split when meets these tokens
         */
        public final List<String> SPLIT;

        {
                PAIR.put("(", ")"); // arguments/procedures/expressions
                PAIR.put("{", "}"); // map
                PAIR.put("[", "]"); // array[index]

                SPLIT_X.addAll(NO_RECORD);
                SPLIT_X.addAll(SPLIT_TWO_VAR_OP_THAT_CAN_BE_USED_WITH_ASSIGN);
                SPLIT_X.addAll(SPLIT_TWO_VAR_OP_THAT_CAN_BE_USED_WITH_ASSIGN.stream().map(s -> s + "=").collect(Collectors.toList()));

                Set<String> set = new HashSet<>();
                set.addAll(LAYER);
                set.addAll(LAYER_END);
                set.addAll(SPLIT_X);
                set.add(ENDING);
                set.add(COMMENT);
                set.add(MultipleLineCommentStart);
                set.add(MultipleLineCommentEnd);
                set.addAll(PAIR.keySet());
                set.addAll(PAIR.values());

                // the longest string is considered first
                SPLIT = set.stream().sorted((a, b) -> b.length() - a.length()).collect(Collectors.toList());
                SPLIT.addAll(0, STRING);
        }

        protected static class LineAndString {
                String str;
                String line;
                LineCol lineCol;
        }

        protected final String fileName;
        protected final PushLineBackReader reader;
        protected final Properties properties;
        protected final ErrorManager err;

        /**
         * initiate the processor with a reader
         *
         * @param fileName   the input text file name
         * @param reader     text reader
         * @param properties properties for the Scanner
         * @param err        error manager
         */
        public AbstractScanner(String fileName, Reader reader, Properties properties, ErrorManager err) {
                this.fileName = fileName;
                this.properties = properties;
                this.err = err;
                if (reader instanceof PushLineBackReader) {
                        this.reader = (PushLineBackReader) reader;
                } else {
                        this.reader = new PushLineBackReader(reader);
                }
        }

        /**
         * scan the text and generate a token tree.
         *
         * @return start node
         * @throws IOException     exception when reading the text
         * @throws SyntaxException exception when meets a syntax error
         */
        @Override
        public ElementStartNode scan() throws IOException, SyntaxException {
                Args args = new Args();
                args.fileName = fileName;
                ElementStartNode elementStartNode = new ElementStartNode(args, 0);
                args.startNodeStack.push(elementStartNode);
                args.currentLine = properties._LINE_BASE_;
                scan(args);
                finalCheck(elementStartNode);
                return elementStartNode;
        }

        protected abstract void scan(Args args) throws IOException, SyntaxException;

        /**
         * remove useless EndingNode and useless StartNode <br>
         * join double literal<br>
         * remove `` from the valid names
         *
         * @param root root node
         * @throws UnknownTokenException the token is unknown
         */
        protected void finalCheck(ElementStartNode root) throws UnknownTokenException {
                if (root.hasLinkedNode()) {
                        Node n = root.getLinkedNode();
                        while (n != null) {
                                if (n instanceof ElementStartNode) {
                                        finalCheck((ElementStartNode) n);
                                }
                                if (n instanceof EndingNode && (!n.hasNext() || !(n.next() instanceof Element))) {
                                        if (n.hasPrevious()) {
                                                n.previous().setNext(n.next());
                                        }
                                        if (n.hasNext()) {
                                                n.next().setPrevious(n.previous());
                                        }
                                } else if (n instanceof Element) {
                                        ((Element) n).checkWhetherIsValidName();
                                        if (((Element) n).getContent().equals(".")
                                                && n.hasPrevious()
                                                && n.hasNext()
                                                && n.previous() instanceof Element
                                                && n.next() instanceof Element
                                                && CompileUtil.isNumber(((Element) n.previous()).getContent())
                                                && CompileUtil.isNumber(((Element) n.next()).getContent())
                                                && !((Element) n.previous()).getContent().contains(".")
                                                && !((Element) n.next()).getContent().contains(".")) {
                                                Element pre = (Element) n.previous();
                                                Element ne = (Element) n.next();
                                                String s = pre.getContent() + "." + ne.getContent();
                                                Element element = new Element(new Args(), s, getTokenType(s, pre.getLineCol()));
                                                element.setLineCol(pre.getLineCol());

                                                element.setPrevious(pre.previous());
                                                element.setNext(ne.next());

                                                element.getLineCol().length = s.length();

                                                if (element.hasPrevious()) {
                                                        element.previous().setNext(element);
                                                } else {
                                                        root.setLinkedNode(element);
                                                }
                                                if (element.hasNext()) {
                                                        element.next().setPrevious(element);
                                                }
                                        }
                                }

                                n = n.next();
                        }
                        n = root.getLinkedNode();
                        while (n != null) {
                                if (n instanceof ElementStartNode && n.hasNext() && !(n.next() instanceof EndingNode)) {
                                        Node next = n.next();

                                        Args args = new Args();
                                        args.previous = n;
                                        args.currentLine = n.getLineCol().line;
                                        args.currentCol = n.getLineCol().column;
                                        EndingNode endingNode = new EndingNode(args, EndingNode.SYNTHETIC);

                                        endingNode.setNext(next);
                                        next.setPrevious(endingNode);
                                }
                                n = n.next();
                        }
                } else {
                        if (root.hasPrevious()) {
                                root.previous().setNext(root.next());
                        }
                        if (root.hasNext()) {
                                root.next().setPrevious(root.previous());
                        }
                }
        }

        /**
         * @param str     the token to check type
         * @param lineCol line column file
         * @return TokenType or null if it's an unknown token
         * @throws UnknownTokenException exception
         */
        protected final TokenType getTokenType(String str, LineCol lineCol) throws UnknownTokenException {
                if (CompileUtil.isBoolean(str)) return TokenType.BOOL;
                if (CompileUtil.isModifier(str)) return TokenType.MODIFIER;
                if (CompileUtil.isNumber(str)) return TokenType.NUMBER;
                if (CompileUtil.isRegex(str)) return TokenType.REGEX;
                if (CompileUtil.isString(str)) return TokenType.STRING;
                if (CompileUtil.isKey(str))
                        return TokenType.KEY; // however in/is/not are two variable operators, they are marked as keys
                if (CompileUtil.isSymbol(str)) return TokenType.SYMBOL;
                if (SPLIT.contains(str)) return TokenType.SYMBOL;
                if (CompileUtil.isValidName(str)) return TokenType.VALID_NAME;
                if (LAYER_END.contains(str)) return TokenType.LAYER_END;
                err.UnknownTokenException(str, lineCol);
                // ignore the token, and return null
                return null;
        }

        /**
         * create an {@link ElementStartNode}
         *
         * @param args args context
         */
        protected final void createStartNode(Args args) {
                ElementStartNode elementStartNode = new ElementStartNode(args, args.startNodeStack.lastElement().getIndent() + properties._INDENTATION_);
                args.previous = null;
                args.startNodeStack.push(elementStartNode);
        }

        /**
         * check whether it's the string end. count the `\`, check whether it%2==0
         *
         * @param line  the line
         * @param index index of `\`
         * @return true/false
         */
        protected final boolean checkStringEnd(String line, int index) {
                int count = 0;
                char[] arr = line.toCharArray();
                for (int i = index; i > 0; --i) {
                        char c = arr[i];
                        if (c == '\\') ++count;
                        else break;
                }
                return count % 2 == 0;
        }

        /**
         * pop one or more nodes from {@link Args#startNodeStack}, the last popped node's indentation should be the same as required indent
         *
         * @param args    args context
         * @param indent  required indentation
         * @param newLine reaching a "new line"
         * @throws UnexpectedTokenException compiling error
         */
        protected final void redirectToStartNodeByIndent(Args args, int indent, boolean newLine) throws UnexpectedTokenException {
                if (args.startNodeStack.empty()) {
                        throw new LtBug("this should never happen");
                }

                ElementStartNode startNode = args.startNodeStack.pop();

                if (startNode.getIndent() == indent) {
                        if (startNode.hasNext()) {
                                throw new LtBug("startNode in this step should never have nodes appended");
                        }
                        // do redirect
                        args.previous = startNode;
                        if (startNode.hasPrevious() && startNode.previous() instanceof Element
                                && ((Element) startNode.previous()).getContent().equals("|-")
                                && newLine) {
                                args.previous = new EndingNode(args, EndingNode.WEAK);
                        }
                } else {
                        if (startNode.getIndent() < indent || args.startNodeStack.empty()) {
                                throw new LtBug("position=" + args.currentLine + ":" + args.currentCol + ",indent=" + indent);
                        }
                        redirectToStartNodeByIndent(args, indent, newLine);
                }
        }
}
