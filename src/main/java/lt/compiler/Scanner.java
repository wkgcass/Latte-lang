package lt.compiler;

import lt.compiler.lexical.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * transform plain <tt>LessTyping</tt> text into Token Tree<br>
 * <tt>LessTyping</tt> forces {@link #_INDENT} indents to differ program blocks<br>
 * so, instead of a stream of Tokens, the Scanner builds a Tree of Tokens to record different layers of the code<br>
 * the parse method returns the root token of the Token Tree<br>
 * the Tree starts with an ElementStartNode. Element represents useful info about the program.<br>
 * EndingNode means the previous node and the next node might be separated
 *
 * @see Node
 * @see Element
 * @see ElementStartNode
 * @see EndingNode
 */
public class Scanner {
        /**
         * create a new layer if it's the first element of a line<br>
         * and only when indent becomes smaller than when entering the layer, the layer would finish
         */
        public final static Set<String> LAYER = new HashSet<>(Arrays.asList("#>", "#", "->"));
        /**
         * split into 2 tokens (the chars already read when meeting these words would be one token) when meet these words
         */
        public final static Set<String> SPLIT_X = new HashSet<>(Arrays.asList(
                ".", // class positioning or method access
                ":", // type specification or generic extends
                "::", // package::package
                "=", "+=", "-=", "*=", "/=", "%=", // assignment
                "<<", ">>", ">>>", // shift
                "&", "^", "|", "~", // bit logic
                "^^", // pow
                "!", "&&", "||", // logic
                "!=", "==", "!==", "===", // equals/reference equals
                "<", ">", "<=", ">=", // comparison or generic extends/super
                "+", "-", "*", "/", "%", // operators
                "++", "--",
                "@", // @Annotation
                "=:=", "!:=", // equal or not$equal
                "..", ".:", // list generator
                "..." // pass
        ));
        /**
         * string start/end
         */
        public final static Set<String> STRING = new HashSet<>(Arrays.asList("\"", "'", "`"));
        public static final String ESCAPE = "\\";
        /**
         * don't record these, just consider as a simple word to split words
         */
        public final static Set<String> NO_RECORD = new HashSet<>(Collections.singletonList(" "));
        /**
         * the str is considered as ending text, append an EndingNode
         */
        public static final String ENDING = ",";
        /**
         * comment, strings behind these are ignored
         */
        public static final String COMMENT = ";";
        /**
         * create a new layer at 'key' and the layer finishes at 'value'<br>
         * the pair key and value should be only one key
         */
        public final static Map<String, String> PAIR = new HashMap<>();
        /**
         * split when meeting these<br>
         * strings are checked before anything else
         */
        public final static List<String> SPLIT;

        static {
                PAIR.put("(", ")"); // arguments
                PAIR.put("{", "}"); // map
                PAIR.put("[", "]"); // array[index]

                SPLIT_X.addAll(NO_RECORD);

                Set<String> set = new HashSet<>();
                set.addAll(LAYER);
                set.addAll(SPLIT_X);
                set.add(ENDING);
                set.add(COMMENT);
                set.addAll(PAIR.keySet());
                set.addAll(PAIR.values());

                SPLIT = set.stream().sorted((a, b) -> b.length() - a.length()).collect(Collectors.toList());
                SPLIT.addAll(0, STRING);
        }

        private final BufferedReader reader;
        private final int _INDENT;
        private final String fileName;

        /**
         * initiate the processor with a reader
         *
         * @param reader text reader
         */
        public Scanner(String fileName, Reader reader, int _INDENT) {
                this.fileName = fileName;
                this._INDENT = _INDENT;
                if (reader instanceof BufferedReader) {
                        this.reader = (BufferedReader) reader;
                } else {
                        this.reader = new BufferedReader(reader);
                }
        }

        /**
         * parse the strings from the reader<br>
         * the method will create an Args object as context of the parsing process.<br>
         * then create a new ElementStartNode<br>
         * then invoke {@link Scanner#parse(Args)} to parse text to node of the tree<br>
         * after the parsing process finishes, a check for useless EndingNodes and number literals will be invoked<br>
         * finally the root node {@link ElementStartNode} would be returned
         *
         * @return parsed result
         * @throws IOException     exceptions during reading chars from reader
         * @throws SyntaxException syntax exceptions, including {@link SyntaxException}, {@link UnexpectedTokenException}, {@link IllegalIndentationException}
         * @see Args
         * @see #parse(Args)
         */
        public ElementStartNode parse() throws IOException, SyntaxException {
                Args args = new Args();
                args.fileName = fileName;
                ElementStartNode elementStartNode = new ElementStartNode(args, 0);
                args.startNodeStack.push(elementStartNode);
                parse(args);
                finalCheck(elementStartNode);
                return elementStartNode;
        }

        private class LineAndString {
                String str;
                String line;
                LineCol lineCol;
        }

        /**
         * get string for pre processing
         *
         * @param line           the string start with blank and {@link #STRING} character<br>
         *                       (start with 0, 1 or more spaces and {@link #STRING} char)
         * @param args           args
         * @param originalString the original line
         * @param command        command (define|undef)
         * @return (retrievedStr, lineWithOutTheRetrievedStr)
         * @throws SyntaxException compiling error
         */
        private LineAndString getStringForPreProcessing(String line, Args args, String originalString, String command) throws SyntaxException {
                String str = line.trim();
                if (str.isEmpty()) throw new SyntaxException("illegal " + command + " command " + originalString,
                        args.generateLineCol());

                String token = String.valueOf(str.charAt(0));
                if (!STRING.contains(token))
                        throw new SyntaxException("illegal " + command + " command " + originalString, args.generateLineCol());
                args.currentCol += line.indexOf(token); // set current column

                line = str;

                int lastIndex = 0;
                LineCol lineCol = args.generateLineCol();
                while (true) {
                        int index = line.indexOf(token, lastIndex + 1);
                        if (line.length() <= 1 || index == -1)
                                throw new SyntaxException("end of string not found", lineCol);
                        String c = String.valueOf(line.charAt(index - 1));
                        if (!ESCAPE.equals(c)) {
                                // the string starts at minIndex and ends at index
                                String s = line.substring(0, index + 1);

                                args.currentCol += (index + token.length());
                                line = line.substring(index + 1);

                                token = s;

                                break;
                        }

                        lastIndex = index;
                }
                LineAndString las = new LineAndString();
                las.line = line;
                las.str = token;
                las.lineCol = lineCol;
                return las;
        }

        /**
         * <ol>
         * <li>read a line from the {@link #reader}</li>
         * <li>check indent</li>
         * <li>check layer:indent = lastLayerIndent + {@link #_INDENT} means start a new startNode. indent < lastLayerIndent means it should resume to upper layers</li>
         * <li>invoke {@link #parse(String, Args)}</li>
         * <li>append {@link EndingNode}</li>
         * </ol>
         *
         * @param args args context
         * @throws IOException     exceptions during reading chars from reader
         * @throws SyntaxException syntax exceptions, including {@link SyntaxException}, {@link UnexpectedTokenException}, {@link IllegalIndentationException}
         * @see #parse(String, Args)
         */
        private void parse(Args args) throws IOException, SyntaxException {
                String line = reader.readLine();
                int rootIndent = -1;
                while (line != null) {
                        ++args.currentLine;

                        // pre processing
                        if (line.startsWith("define")) {
                                if (!line.equals("define") &&
                                        SPLIT.contains(line.substring("define".length(), "define".length() + 1))) {
                                        ++args.currentCol;

                                        String originalString = line;

                                        line = line.substring("define".length());
                                        args.currentCol += "define".length();
                                        LineAndString las1 = getStringForPreProcessing(line, args, originalString, "define");
                                        line = las1.line;
                                        String target = las1.str.substring(1, las1.str.length() - 1);
                                        if (target.length() == 0) throw new SyntaxException("define <target> length cannot be 0", las1.lineCol);
                                        if (target.contains(ESCAPE)) throw new SyntaxException("define <target> cannot contain escape char", las1.lineCol);

                                        if (!line.trim().startsWith("as")) {
                                                throw new SyntaxException("illegal define command " +
                                                        "(there should be an `as` between <target> and <replacement>)",
                                                        args.generateLineCol());
                                        }

                                        int asPos = line.indexOf("as");
                                        line = line.substring(asPos + 2);

                                        if (line.isEmpty()) throw new SyntaxException("illegal define command " + originalString,
                                                args.generateLineCol());
                                        if (!SPLIT.contains(String.valueOf(line.charAt(0))))
                                                throw new SyntaxException("illegal define command " +
                                                        "(there should be an `as` between <target> and <replacement>)",
                                                        args.generateLineCol());

                                        args.currentCol += (asPos + 2);

                                        LineAndString las2 = getStringForPreProcessing(line, args, originalString, "define");
                                        line = las2.line;
                                        String replacement = las2.str.substring(1, las2.str.length() - 1); // defined replacement
                                        if (replacement.contains(ESCAPE))
                                                throw new SyntaxException("define <replacement> cannot contain escape char", las2.lineCol);
                                        if (line.trim().length() != 0) throw new SyntaxException("illegal define command " +
                                                "(there should not be characters after <replacement>)", args.generateLineCol());

                                        args.defined.put(target, replacement);

                                        line = reader.readLine();
                                        args.currentCol = 0;
                                        continue;
                                }
                        } else if (line.startsWith("undef")) {
                                if (!line.equals("undef") &&
                                        SPLIT.contains(line.substring("undef".length(), "undef".length() + 1))) {
                                        ++args.currentCol;

                                        LineCol lineStart = args.generateLineCol();
                                        String originalString = line;

                                        line = line.substring("undef".length());
                                        args.currentCol += "undef".length();
                                        LineAndString las1 = getStringForPreProcessing(line, args, originalString, "undef");
                                        line = las1.line;
                                        String target = las1.str.substring(1, las1.str.length() - 1);
                                        if (target.length() == 0) throw new SyntaxException("undef <target> length cannot be 0", las1.lineCol);
                                        if (target.contains(ESCAPE)) throw new SyntaxException("undef <target> cannot contain escape char", las1.lineCol);

                                        if (line.trim().length() != 0) throw new SyntaxException("illegal undef command " +
                                                "(there should not be characters after <target>)", args.generateLineCol());

                                        if (!args.defined.containsKey(target)) throw new SyntaxException("\"" + target + "\" is not defined", lineStart);

                                        args.defined.remove(target);

                                        line = reader.readLine();
                                        args.currentCol = 0;
                                        continue;
                                }
                        }

                        // the line is nothing but comment
                        if (line.trim().startsWith(COMMENT)) {
                                line = reader.readLine();
                                continue;
                        }

                        int COMMENT_index = line.indexOf(COMMENT);
                        if (COMMENT_index != -1) {
                                String pre = line.substring(0, COMMENT_index);
                                String post = line.substring(COMMENT_index);
                                for (Map.Entry<String, String> definedEntry : args.defined.entrySet()) {
                                        pre = pre.replace(definedEntry.getKey(), definedEntry.getValue());
                                }
                                line = pre + post;
                        } else {
                                for (Map.Entry<String, String> definedEntry : args.defined.entrySet()) {
                                        line = line.replace(definedEntry.getKey(), definedEntry.getValue());
                                }
                        }

                        // get front spaces
                        int spaces = 0;
                        for (int i = 0; i < line.length(); ++i) {
                                if (line.charAt(i) != ' ') {
                                        spaces = i;
                                        break;
                                }
                        }

                        // set root indent
                        if (rootIndent == -1) {
                                rootIndent = spaces;
                                spaces = 0;
                        } else {
                                spaces -= rootIndent;
                        }

                        // check space indent
                        if (spaces % _INDENT != 0) throw new IllegalIndentationException(_INDENT, args.generateLineCol());

                        // remove spaces
                        line = line.trim();

                        args.currentCol = spaces + 1 + rootIndent;

                        // check it's an empty line
                        if (line.isEmpty()) {
                                line = reader.readLine();
                                continue;
                        }

                        // check start node
                        if (args.startNodeStack.lastElement().getIndent() != spaces) {
                                if (args.startNodeStack.lastElement().getIndent() > spaces) {
                                        // smaller indent
                                        redirectToStartNodeByIndent(args, spaces + _INDENT);
                                } else if (args.startNodeStack.lastElement().getIndent() == spaces - _INDENT) {
                                        // greater indent
                                        createStartNode(args);
                                } else {
                                        throw new IllegalIndentationException(_INDENT, args.generateLineCol());
                                }
                        }

                        // start parsing
                        parse(line, args);

                        if (args.previous instanceof Element) {
                                args.previous = new EndingNode(args, EndingNode.WEAK);
                        }

                        line = reader.readLine();
                }
        }

        /**
         * when the given line is not empty, do parsing.<br>
         * <ol>
         * <li>check whether the line contains tokens in {@link #SPLIT}<br>
         * get the most front token, if several tokens are at the same position, then choose the string literal, then choose the longest one</li>
         * <li>if the token not found, consider the whole line as one element and append to previous node</li>
         * <li>else</li>
         * <li>record text before the token as an element and do appending</li>
         * <li>check which category the token is in<br>
         * <ul>
         * <li>{@link #LAYER} means starts after recording the token, a new ElementStartNode should be started, invoke {@link #createStartNode(Args)}</li>
         * <li>{@link #SPLIT_X}: the previous element and next element should be in the same layer. if it's also among {@link #NO_RECORD}, the token won't be recorded</li>
         * <li>{@link #STRING}: the next element is a string or a character. these characters should be considered as one element</li>
         * <li>{@link #ENDING}: append a new {@link EndingNode} to prevent generated nodes being ambiguous. NOTE that it only means the end of an expression or a statement. not the parsing process</li>
         * <li>{@link #COMMENT}: it's the start of a comment. the following chars would be ignored</li>
         * <li>{@link #PAIR}: for keys, it will start a new layer. for values, it will end the layer created by the key</li>
         * </ul>
         * </li>
         * </ol>
         *
         * @param line line to parse
         * @param args args context
         * @throws SyntaxException syntax exceptions, including {@link SyntaxException}, {@link UnexpectedTokenException}
         */
        private void parse(String line, Args args) throws SyntaxException {
                if (line.isEmpty()) return;
                // check SPLIT
                // find the pattern at minimum location index and with longest words
                int minIndex = line.length();
                String token = null; // recorded token
                for (String s : SPLIT) {
                        if (line.contains(s)) {
                                int index = line.indexOf(s);
                                if (index != -1 && index < minIndex) {
                                        minIndex = index;
                                        token = s;
                                }
                        }
                }

                if (token == null) {
                        // not found, append to previous
                        args.previous = new Element(args, line);
                } else {
                        String copyOfLine = line;
                        String str = line.substring(0, minIndex);
                        if (!str.isEmpty()) {
                                // record text before the token
                                args.previous = new Element(args, str);
                                args.currentCol += str.length();
                        }

                        if (LAYER.contains(token)) {
                                // start new layer
                                args.previous = new Element(args, token);
                                createStartNode(args);
                        } else if (SPLIT_X.contains(token)) {
                                // do split check
                                if (!NO_RECORD.contains(token)) {
                                        // record this token
                                        args.previous = new Element(args, token);
                                }
                        } else if (STRING.contains(token)) {
                                // string literal
                                int lastIndex = minIndex;
                                while (true) {
                                        int index = line.indexOf(token, lastIndex + 1);
                                        if (line.length() <= 1 || index == -1)
                                                throw new SyntaxException("end of string not found", args.generateLineCol());
                                        String c = String.valueOf(line.charAt(index - 1));
                                        if (!ESCAPE.equals(c)) {
                                                // the string starts at minIndex and ends at index
                                                String s = line.substring(minIndex, index + 1);

                                                args.previous = new Element(args, s);
                                                args.currentCol += (index - minIndex + 1 - token.length());
                                                line = line.substring(index + 1);

                                                token = s;
                                                break;
                                        }

                                        lastIndex = index;
                                }
                        } else if (ENDING.equals(token)) {
                                // ending
                                if (args.previous instanceof Element) {
                                        args.previous = new EndingNode(args, EndingNode.STRONG);
                                }
                        } else if (COMMENT.equals(token)) {
                                // comment
                                line = ""; // ignore all
                        } else if (PAIR.containsKey(token)) {
                                // pair start
                                args.previous = new Element(args, token);
                                createStartNode(args);
                                args.pairEntryStack.push(new PairEntry(token, args.startNodeStack.lastElement()));
                        } else if (PAIR.containsValue(token)) {
                                // pair end
                                PairEntry entry = args.pairEntryStack.pop();
                                String start = entry.key;
                                if (!token.equals(PAIR.get(start))) {
                                        throw new UnexpectedTokenException(PAIR.get(start), token, args.generateLineCol());
                                }

                                ElementStartNode startNode = entry.startNode;
                                if (startNode.hasNext()) {
                                        throw new UnexpectedTokenException("null", startNode.next().toString(), args.generateLineCol());
                                }

                                if (args.startNodeStack.lastElement().getIndent() >= startNode.getIndent()) {
                                        redirectToStartNodeByIndent(args, startNode.getIndent());
                                } else if (args.startNodeStack.lastElement().getIndent() == startNode.getIndent() - _INDENT) {
                                        args.previous = startNode;
                                } else {
                                        throw new SyntaxException("indentation of " + token + " should >= " + start + "'s indent or equal to " + start + "'s indent - " + _INDENT, args.generateLineCol());
                                }
                                args.previous = new Element(args, token);
                        } else {
                                throw new UnexpectedTokenException(token, args.generateLineCol());
                        }

                        // column
                        args.currentCol += token.length();
                        if (copyOfLine.equals(line)) {
                                // line hasn't changed, do default modification
                                line = line.substring(minIndex + token.length());
                        }
                        // recursively parse
                        parse(line, args);
                }
        }

        private void redirectToStartNodeByIndent(Args args, int indent) throws UnexpectedTokenException {
                ElementStartNode startNode = args.startNodeStack.pop();

                if (startNode == null) throw new NoSuchElementException();
                if (startNode.getIndent() == indent) {
                        if (startNode.hasNext()) {
                                throw new UnexpectedTokenException("null", startNode.next().toString(), args.generateLineCol());
                        }
                        // do redirect
                        args.previous = startNode;
                } else {
                        if (startNode.getIndent() < indent || args.startNodeStack.empty()) {
                                throw new NoSuchElementException("position=" + args.currentLine + ":" + args.currentCol + ",indent=" + indent);
                        }
                        redirectToStartNodeByIndent(args, indent);
                }
        }

        private void createStartNode(Args args) {
                ElementStartNode elementStartNode = new ElementStartNode(args, args.startNodeStack.lastElement().getIndent() + _INDENT);
                args.previous = null;
                args.startNodeStack.push(elementStartNode);
        }

        /**
         * remove useless EndingNode and useless StartNode <br>
         * and join double literal<br>
         * and mark those valid name and remove `` from the names
         *
         * @param root root node
         */
        private void finalCheck(ElementStartNode root) {
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
                                                Element element = new Element(new Args(), s);
                                                element.setLineCol(pre.getLineCol());

                                                element.setPrevious(pre.previous());
                                                element.setNext(ne.next());

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
                                if (n instanceof ElementStartNode && n.hasNext()) {
                                        Node next = n.next();

                                        Args args = new Args();
                                        args.previous = n;
                                        args.currentLine = n.getLineCol().line;
                                        args.currentCol = n.getLineCol().column;
                                        EndingNode endingNode = new EndingNode(args, EndingNode.WEAK);

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
}
