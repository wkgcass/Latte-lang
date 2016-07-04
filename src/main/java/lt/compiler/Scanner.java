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
import lt.compiler.syntactic.UnknownTokenException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * transform plain <tt>Latte</tt> text into Tokens<br>
 * <tt>Latte</tt> forces {@link lt.compiler.Scanner.Properties#_INDENTATION_} indentation to differ program blocks<br>
 * Instead of using <tt>INDENT</tt> and <tt>DEDENT</tt>, the Scanner uses a <b>Tree of Tokens</b> to record the indentation info.
 * Consistent statements with the same indentation are in the same <b>Layer</b><br>
 * <pre>
 * class User
 *     id : int
 *     name : String
 * </pre>
 * <code>(id : int)</code> and <code>(name : String)</code> are in the same layer<br>
 * the text would be considered as the following token tree<br>
 * <pre>
 * [class]-[User]-[|]-[END]
 *                 |
 *                 --[id]-[:]-[int]-[END]-[name]-[:]-[String]
 * </pre>
 *
 * @see Node
 * @see Element
 * @see ElementStartNode
 * @see EndingNode
 */
public class Scanner {
        /**
         * properties for the scanner
         */
        public static class Properties {
                /**
                 * indentation
                 */
                public int _INDENTATION_ = 4;
                /**
                 * base of line numbers. all lines += _LINE_BASE_
                 */
                public int _LINE_BASE_ = 0;
                /**
                 * base of column numbers. all columns += _COLUMN_BASE_
                 */
                public int _COLUMN_BASE_ = 0;
        }

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
         *                            --[a]-[+]-[b]
         * </pre>
         */
        public final static Set<String> LAYER = new HashSet<>(Collections.singletonList("->"));
        /**
         * the input should be split when meets these tokens
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
                "...", // pass
                ":::", // concat
                ":=", // assign
                "#" // generator
        ));
        /**
         * symbols that let the scanner know the following input should be scanned as a string<br>
         * a string starts with one of these symbols and ends with the same symbol.
         */
        public final static Set<String> STRING = new HashSet<>(Arrays.asList("\"", "'", "`", "//"));
        /**
         * the escape character.
         */
        public static final String ESCAPE = "\\";
        /**
         * these tokens split the input, but them themselves won't be recorded into the token tree. e.g.<br>
         * <pre>
         * list add 1
         * </pre>
         * the spaces split the input, but they won't be recorded.
         */
        public final static Set<String> NO_RECORD = new HashSet<>(Collections.singletonList(" "));
        /**
         * the str is considered as ending text. Append an EndingNode to the token tree
         */
        public static final String ENDING = ",";
        /**
         * comment, strings after the token are ignored
         */
        public static final String COMMENT = ";";
        /**
         * multiple line comment start symbol
         */
        public static final String MultipleLineCommentStart = "/*";
        /**
         * multiple line comment end symbol
         */
        public static final String MultipleLineCommentEnd = "*/";
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
        public final static Map<String, String> PAIR = new HashMap<>();
        /**
         * the input should be split when meets these tokens
         */
        public final static List<String> SPLIT;

        static {
                PAIR.put("(", ")"); // arguments/procedures/expressions
                PAIR.put("{", "}"); // map
                PAIR.put("[", "]"); // array[index]

                SPLIT_X.addAll(NO_RECORD);

                Set<String> set = new HashSet<>();
                set.addAll(LAYER);
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

        private final BufferedReader reader;
        private final Properties properties;
        private final ErrorManager err;
        private final String fileName;

        /**
         * initiate the processor with a reader
         *
         * @param fileName   the input text file name
         * @param reader     text reader
         * @param properties properties for the Scanner
         * @param err        error manager
         */
        public Scanner(String fileName, Reader reader, Properties properties, ErrorManager err) {
                this.fileName = fileName;
                this.properties = properties;
                this.err = err;
                if (reader instanceof BufferedReader) {
                        this.reader = (BufferedReader) reader;
                } else {
                        this.reader = new BufferedReader(reader);
                }
        }

        /**
         * scan the inputs from the {@link #reader} and build a <b>token tree</b><br>
         * <ol>
         * <li>create an Args object as context of the parsing process.</li>
         * <li>create a new {@link ElementStartNode} as the root node</li>
         * <li>then invoke {@link Scanner#scan(Args)} to scan the inputs</li>
         * <li>after the parsing process finishes, a check for useless {@link EndingNode} and number literals will be invoked ({@link #finalCheck(ElementStartNode)})</li>
         * <li>finally the root node would be returned</li>
         * </ol>
         *
         * @return the root node
         * @throws IOException     exceptions during reading lines from reader
         * @throws SyntaxException syntax exceptions, including {@link SyntaxException}, {@link UnexpectedTokenException}, {@link IllegalIndentationException}
         * @see Args
         * @see #scan(Args)
         */
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

        private class LineAndString {
                String str;
                String line;
                LineCol lineCol;
        }

        /**
         * get string for pre processing
         *
         * @param line           the string starts with 0, 1 or more spaces and followed by a {@link #STRING} character
         * @param args           args
         * @param originalString the original line
         * @param command        command (define|undef)
         * @return (retrievedStr, lineWithOutTheRetrievedStr) or null if the process failed
         * @throws SyntaxException compiling error
         */
        private LineAndString getStringForPreProcessing(String line, Args args, String originalString, String command) throws SyntaxException {
                String str = line.trim();
                if (str.isEmpty()) {
                        err.SyntaxException("illegal " + command + " command " + originalString,
                                args.generateLineCol());
                        return null;
                }

                String token = String.valueOf(str.charAt(0));
                if (!STRING.contains(token)) {
                        err.SyntaxException("illegal " + command + " command " + originalString, args.generateLineCol());
                        return null;
                }
                args.currentCol += line.indexOf(token); // set current column

                line = str;

                int lastIndex = 0;
                LineCol lineCol = args.generateLineCol();
                while (true) {
                        int index = line.indexOf(token, lastIndex + 1);
                        if (line.length() <= 1 || index == -1) {
                                err.SyntaxException("end of string not found", lineCol);
                                // assume that the string ends at line end
                                err.debug("assume that the " + token + " end is line end");
                                token = line + token;
                                break;
                        } else {
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
                }
                LineAndString las = new LineAndString();
                las.line = line;
                las.str = token; // the token is used to store the whole string
                las.lineCol = lineCol;
                return las;
        }

        /**
         * <ol>
         * <li>read a line from the {@link #reader}</li>
         * <li>check indentation</li>
         * <li>check layer => if indent = lastLayerIndent + {@link lt.compiler.Scanner.Properties#_INDENTATION_}, then start a new layer.
         * elseif indent < lastLayerIndent then it should go back to upper layers<br>
         * </li>
         * <li>invoke {@link #scan(String, Args)} to scan the current line</li>
         * <li>append an {@link EndingNode}</li>
         * </ol>
         *
         * @param args args context
         * @throws IOException     exceptions during reading chars from reader
         * @throws SyntaxException syntax exceptions, including {@link SyntaxException}, {@link UnexpectedTokenException}, {@link IllegalIndentationException}
         * @see #scan(String, Args)
         */
        private void scan(Args args) throws IOException, SyntaxException {
                String line = reader.readLine();
                int rootIndent = -1;
                while (line != null) {
                        ++args.currentLine;

                        err.putLineRecord(args.fileName, args.currentLine, line);

                        args.currentCol = properties._COLUMN_BASE_;
                        args.useDefine.clear();

                        if (args.multipleLineComment) {
                                if (!line.contains(MultipleLineCommentEnd)) {
                                        line = reader.readLine();
                                        continue;
                                } else {
                                        int subCol = line.indexOf(MultipleLineCommentEnd) + MultipleLineCommentEnd.length();
                                        line = line.substring(subCol);
                                        args.currentCol += (subCol + 1);
                                        args.multipleLineComment = false;
                                }
                        } else {
                                // pre processing
                                if (line.startsWith("define")) {
                                        if (!line.equals("define") &&
                                                SPLIT.contains(line.substring("define".length(), "define".length() + 1))) {
                                                ++args.currentCol;

                                                String originalString = line;

                                                line = line.substring("define".length());
                                                args.currentCol += "define".length();
                                                LineAndString las1 = getStringForPreProcessing(line, args, originalString, "define");

                                                if (las1 == null) {
                                                        line = reader.readLine();
                                                        continue;
                                                }

                                                line = las1.line;
                                                String target = las1.str.substring(1, las1.str.length() - 1);
                                                if (target.length() == 0) {
                                                        err.SyntaxException("define <target> length cannot be 0", las1.lineCol);
                                                        line = reader.readLine();
                                                        continue;
                                                }
                                                if (target.contains(ESCAPE)) {
                                                        err.SyntaxException("define <target> cannot contain escape char", las1.lineCol);
                                                        line = reader.readLine();
                                                        continue;
                                                }

                                                if (!line.trim().startsWith("as")) {
                                                        err.SyntaxException("illegal define command " +
                                                                        "(there should be an `as` between <target> and <replacement>)",
                                                                args.generateLineCol());
                                                        line = reader.readLine();
                                                        continue;
                                                }

                                                int asPos = line.indexOf("as");
                                                line = line.substring(asPos + 2);

                                                if (line.isEmpty()) {
                                                        err.SyntaxException("illegal define command " + originalString,
                                                                args.generateLineCol());
                                                        line = reader.readLine();
                                                        continue;
                                                }
                                                if (!SPLIT.contains(String.valueOf(line.charAt(0)))) {
                                                        err.SyntaxException("illegal define command " +
                                                                        "(there should be an `as` between <target> and <replacement>)",
                                                                args.generateLineCol());
                                                        line = reader.readLine();
                                                        continue;
                                                }

                                                args.currentCol += (asPos + 2);

                                                LineAndString las2 = getStringForPreProcessing(line, args, originalString, "define");

                                                if (las2 == null) {
                                                        line = reader.readLine();
                                                        continue;
                                                }

                                                line = las2.line;
                                                String replacement = las2.str.substring(1, las2.str.length() - 1); // defined replacement
                                                if (replacement.contains(ESCAPE)) {
                                                        err.SyntaxException("define <replacement> cannot contain escape char", las2.lineCol);
                                                        line = reader.readLine();
                                                        continue;
                                                }
                                                if (line.trim().length() != 0) {
                                                        err.SyntaxException("illegal define command " +
                                                                "(there should not be characters after <replacement>)", args.generateLineCol());
                                                        line = reader.readLine();
                                                        continue;
                                                }

                                                args.defined.put(target, replacement);

                                                line = reader.readLine();
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

                                                if (las1 == null) {
                                                        line = reader.readLine();
                                                        continue;
                                                }

                                                line = las1.line;
                                                String target = las1.str.substring(1, las1.str.length() - 1);
                                                if (target.length() == 0) {
                                                        err.SyntaxException("undef <target> length cannot be 0", las1.lineCol);
                                                        line = reader.readLine();
                                                        continue;
                                                }
                                                if (target.contains(ESCAPE)) {
                                                        err.SyntaxException("undef <target> cannot contain escape char", las1.lineCol);
                                                        line = reader.readLine();
                                                        continue;
                                                }

                                                if (line.trim().length() != 0) {
                                                        err.SyntaxException("illegal undef command " +
                                                                "(there should not be characters after <target>)", args.generateLineCol());
                                                        line = reader.readLine();
                                                        continue;
                                                }

                                                if (!args.defined.containsKey(target)) {
                                                        err.SyntaxException("\"" + target + "\" is not defined", lineStart);
                                                        line = reader.readLine();
                                                        continue;
                                                }

                                                args.defined.remove(target);

                                                line = reader.readLine();
                                                continue;
                                        }
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
                                        String tmp = pre;
                                        pre = pre.replace(definedEntry.getKey(), definedEntry.getValue());
                                        if (!tmp.equals(pre)) {
                                                args.useDefine.put(definedEntry.getKey(), definedEntry.getValue());
                                        }
                                }
                                line = pre + post;
                        } else {
                                for (Map.Entry<String, String> definedEntry : args.defined.entrySet()) {
                                        String tmp = line;
                                        line = line.replace(definedEntry.getKey(), definedEntry.getValue());
                                        if (!tmp.equals(line)) {
                                                args.useDefine.put(definedEntry.getKey(), definedEntry.getValue());
                                        }
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

                        if (args.currentCol == properties._COLUMN_BASE_) {
                                args.currentCol += spaces + 1 + rootIndent;
                        }

                        // check space indent
                        int indentation;
                        if (spaces % properties._INDENTATION_ != 0) {
                                err.IllegalIndentationException(
                                        properties._INDENTATION_, args.generateLineCol());
                                // assume that the indentation is spaces + (spaces%INDENTATION)
                                int calculatedIndent = spaces + properties._INDENTATION_ - (spaces % properties._INDENTATION_);
                                err.debug("assume that the indentation is spaces + _INDENTATION_ - (spaces % _INDENTATION_) : " + calculatedIndent);

                                indentation = calculatedIndent;
                        } else indentation = spaces;

                        // remove spaces
                        line = line.trim();

                        // check it's an empty line
                        if (line.isEmpty()) {
                                line = reader.readLine();
                                continue;
                        }

                        // check start node
                        if (args.startNodeStack.lastElement().getIndent() != indentation) {
                                if (args.startNodeStack.lastElement().getIndent() > indentation) {
                                        // smaller indent
                                        redirectToStartNodeByIndent(args, indentation + properties._INDENTATION_);
                                } else if (args.startNodeStack.lastElement().getIndent() == indentation - properties._INDENTATION_) {
                                        // greater indent
                                        createStartNode(args);
                                } else {
                                        err.IllegalIndentationException(properties._INDENTATION_, args.generateLineCol());
                                        // greater than parent indentation too much
                                        // assume it's parent + _INDENTATION_
                                        err.debug("assume it's parent.indentation + _INDENTATION_");
                                        createStartNode(args);

                                }
                        }

                        // start parsing
                        scan(line, args);

                        if (!args.multipleLineComment) {

                                if (args.previous instanceof Element) {
                                        args.previous = new EndingNode(args, EndingNode.WEAK);
                                }
                        }

                        line = reader.readLine();
                }
        }

        /**
         * when the given line is not empty, do scanning.<br>
         * <ol>
         * <li>check whether the line contains tokens in {@link #SPLIT}<br>
         * get the most front token, if several tokens are at the same position, then choose the longest one</li>
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
         * </ol><br>
         * here's an example of how the method works<br>
         * given the following input:<br>
         * <pre>
         * val map={'name':'cass'}
         * </pre>
         * <ol>
         * <li>the most front and longest token is ' ', and ' ' is in {@link #NO_RECORD} ::: <code>val/map={'name':'cass'}</code></li>
         * <li>the most front and longest token is '=' ::: <code>val/map/=/{'name':'cass'}</code></li>
         * <li>the most front and longest token is '{', and '{' is a key of {@link #PAIR} ::: <code>val/map/=/{/(LAYER-START/'name':'cass'})</code></li>
         * <li>the most front and longest token is "'", and "'" is in {@link #STRING} ::: <code>val/map/=/{/(LAYER-START/'name'/:'cass'})</code></li>
         * <li>the most front and longest token is ':' ::: <code>val/map/=/{/(LAYER-START/'name'/:/'cass'})</code></li>
         * <li>the most front and longest token is "'", and "'" is in {@link #STRING} ::: <code>val/map/=/{/(LAYER-START/'name'/:/'cass'/})</code></li>
         * <li>the most front and longest token is "}", and '}' is a value of {@link #PAIR} ::: <code>val/map/=/{/(LAYER-START/'name'/:/'cass')}</code></li>
         * </ol><br>
         * the result is <code>val/map/=/{/(LAYER-START/'name'/:/'cass')}</code><br>
         * set a breakpoint in the method and focus on variable <tt>line</tt>, you will get exactly the same intermediate results
         *
         * @param line line to parse
         * @param args args context
         * @throws SyntaxException syntax exceptions, including {@link SyntaxException}, {@link UnexpectedTokenException}
         */
        private void scan(String line, Args args) throws SyntaxException {
                if (line.isEmpty()) return;

                // check multiple line comment
                if (args.multipleLineComment) {
                        if (line.contains(MultipleLineCommentEnd)) {
                                int subCol = line.indexOf(MultipleLineCommentEnd) + MultipleLineCommentEnd.length();
                                args.currentCol += subCol;
                                line = line.substring(subCol);
                                args.multipleLineComment = false;
                        } else {
                                return;
                        }
                }

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
                        if (!line.isEmpty()) {
                                // not found, simply append whole input to previous
                                TokenType type = getTokenType(line, args.generateLineCol());
                                if (type != null) {
                                        // unknown token, ignore this token
                                        args.previous = new Element(args, line, type);
                                        args.currentCol += line.length();
                                }
                        }
                } else {
                        String copyOfLine = line;
                        String str = line.substring(0, minIndex);
                        if (!str.isEmpty()) {
                                // record text before the token
                                TokenType type = getTokenType(str, args.generateLineCol());
                                if (type != null) {
                                        args.previous = new Element(args, str, type);
                                }
                                args.currentCol += str.length();
                        }

                        if (LAYER.contains(token)) {
                                // start new layer
                                args.previous = new Element(args, token, getTokenType(token, args.generateLineCol()));
                                createStartNode(args);
                        } else if (SPLIT_X.contains(token)) {
                                // do split check
                                if (!NO_RECORD.contains(token)) {
                                        // record this token
                                        args.previous = new Element(args, token, getTokenType(token, args.generateLineCol()));
                                }
                        } else if (STRING.contains(token)) {
                                // string literal
                                int lastIndex = minIndex;
                                while (true) {
                                        int index = line.indexOf(token, lastIndex + token.length());
                                        if (token.equals("//")) {
                                                while (line.length() > index + 2) {
                                                        if (line.charAt(index + 2) == '/') {
                                                                ++index;
                                                        } else {
                                                                break;
                                                        }
                                                }
                                        }
                                        if (line.length() <= 1 || index == -1) {
                                                err.SyntaxException("end of string not found", args.generateLineCol());
                                                // assume that the end is line end
                                                err.debug("assume that the " + token + " end is line end");

                                                String generated = line.substring(minIndex) + token;

                                                args.previous = new Element(args, generated, getTokenType(generated, args.generateLineCol()));
                                                args.currentCol += (index - minIndex) - token.length(); // the length would be added in later steps
                                                line = line.substring(index + 1);

                                                break;
                                        } else {
                                                String c = String.valueOf(line.charAt(index - 1));
                                                // check
                                                boolean isStringEnd = !ESCAPE.equals(c) || checkStringEnd(line, index - 1);

                                                if (isStringEnd) {
                                                        // the string starts at minIndex and ends at index
                                                        String s = line.substring(minIndex, index + token.length());

                                                        args.previous = new Element(args, s, getTokenType(s, args.generateLineCol()));
                                                        args.currentCol += (index - minIndex);
                                                        line = line.substring(index + token.length());
                                                        break;
                                                }

                                                lastIndex = index;
                                        }
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
                                args.previous = new Element(args, token, getTokenType(token, args.generateLineCol()));
                                createStartNode(args);
                                args.pairEntryStack.push(new PairEntry(token, args.startNodeStack.lastElement()));
                        } else if (PAIR.containsValue(token)) {
                                // pair end
                                PairEntry entry = args.pairEntryStack.pop();
                                String start = entry.key;
                                if (!token.equals(PAIR.get(start))) {
                                        err.UnexpectedTokenException(PAIR.get(start), token, args.generateLineCol());
                                        // assume that the pair ends
                                        err.debug("assume that the pair ends");
                                }

                                ElementStartNode startNode = entry.startNode;
                                if (startNode.hasNext()) {
                                        err.SyntaxException(
                                                "indentation of " + startNode.next() + " should be " + startNode.getIndent(),
                                                startNode.next().getLineCol());
                                        // fill the LinkedNode with all nodes after the startNode
                                        Node n = startNode.next();
                                        n.setPrevious(null);
                                        startNode.setNext(null);
                                        startNode.setLinkedNode(n);
                                }

                                if (args.startNodeStack.lastElement().getIndent() >= startNode.getIndent()) {
                                        redirectToStartNodeByIndent(args, startNode.getIndent());
                                } else if (args.startNodeStack.lastElement().getIndent() == startNode.getIndent() - properties._INDENTATION_) {
                                        args.previous = startNode;
                                } else {
                                        err.SyntaxException(
                                                "indentation of " + token + " (" + args.startNodeStack.lastElement().getIndent() + ") should >= " + start + "'s indentation - 4 (" + (startNode.getIndent() - 4) + ")",
                                                args.generateLineCol());
                                        // assume they are the same
                                        err.debug("assume that " + token + "'s indentation is the same with " + start + "'s indentation");
                                        args.previous = startNode;
                                }
                                args.previous = new Element(args, PAIR.get(start), getTokenType(token, args.generateLineCol()));
                        } else if (token.equals(MultipleLineCommentStart)) {
                                if (!args.multipleLineComment) {
                                        args.multipleLineComment = true;
                                }
                        } else {
                                err.UnknownTokenException(token, args.generateLineCol());
                                // unknown token
                                // simply ignore the token
                        }

                        // column
                        args.currentCol += token.length();
                        if (copyOfLine.equals(line)) {
                                // line hasn't changed, do default modification
                                line = line.substring(minIndex + token.length());
                        }
                        // recursively parse
                        scan(line, args);
                }
        }

        /**
         * check whether it's the string end. count the `\`, check whether it%2==0
         *
         * @param line  the line
         * @param index index of `\`
         * @return true/false
         */
        private boolean checkStringEnd(String line, int index) {
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
         * @param args   args context
         * @param indent required indentation
         * @throws UnexpectedTokenException compiling error
         */
        private void redirectToStartNodeByIndent(Args args, int indent) throws UnexpectedTokenException {
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
                } else {
                        if (startNode.getIndent() < indent || args.startNodeStack.empty()) {
                                throw new LtBug("position=" + args.currentLine + ":" + args.currentCol + ",indent=" + indent);
                        }
                        redirectToStartNodeByIndent(args, indent);
                }
        }

        /**
         * create an {@link ElementStartNode}
         *
         * @param args args context
         */
        private void createStartNode(Args args) {
                ElementStartNode elementStartNode = new ElementStartNode(args, args.startNodeStack.lastElement().getIndent() + properties._INDENTATION_);
                args.previous = null;
                args.startNodeStack.push(elementStartNode);
        }

        /**
         * @param str     the token to check type
         * @param lineCol line column file
         * @return TokenType or null if it's an unknown token
         * @throws UnknownTokenException exception
         */
        private TokenType getTokenType(String str, LineCol lineCol) throws UnknownTokenException {
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
                err.UnknownTokenException(str, lineCol);
                // ignore the token, and return null
                return null;
        }

        /**
         * remove useless EndingNode and useless StartNode <br>
         * join double literal<br>
         * remove `` from the valid names
         *
         * @param root root node
         */
        private void finalCheck(ElementStartNode root) throws UnknownTokenException {
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
