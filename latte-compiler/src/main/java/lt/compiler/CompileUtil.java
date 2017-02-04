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
import lt.compiler.syntactic.pre.Modifier;

import java.util.*;

/**
 * compile util<br>
 * checks the element type<br>
 * and defines keywords and operator priority
 */
public class CompileUtil {
        /**
         * check the given string is a number
         *
         * @param str string
         * @return true if the string represents a number
         */
        public static boolean isNumber(String str) {
                try {
                        //noinspection ResultOfMethodCallIgnored
                        Double.parseDouble(str);
                        return true;
                } catch (NumberFormatException ignore) {
                        return false;
                }
        }

        /**
         * check the given string is a boolean literal
         *
         * @param str string
         * @return true if it's a boolean.(true,false,yes,no)
         */
        public static boolean isBoolean(String str) {
                return str.equals("true") || str.equals("false") || str.equals("yes") || str.equals("no");
        }

        /**
         * check the given string is a string literal
         *
         * @param str string
         * @return true/false
         */
        public static boolean isString(String str) {
                return (
                        (str.startsWith("\"") && str.endsWith("\"")) || (str.startsWith("'") && str.endsWith("'"))
                ) && str.length() > 1;
        }

        private static Set<String> keys = new HashSet<>(Arrays.asList(
                "is", "not", "bool", "yes", "no", "type", "as",
                "in", "elseif", "package", "import",
                "break", "continue", "return", "fun", "require",
                "new", "object", "implicit", "match", "case"
        ));

        private static Set<String> javaKeys = new HashSet<>(Arrays.asList(
                "abstract", "assert", "boolean", "break", "byte", "case",
                "catch", "char", "class", "const", "continue", "default",
                "do", "double", "else", "enum", "extends", "final", "finally",
                "float", "for", "if", "implements", "import", "instanceof",
                "int", "interface", "long", "native", "new", "null", "package",
                "private", "protected", "public", "return", "short", "static",
                "strictfp", "throw", "try", "while", "void"
        ));

        public static boolean isKey(String str) {
                return keys.contains(str);
        }

        public static String SingletonFieldName = "singletonInstance";

        /**
         * check whether the given string can be a valid java name
         *
         * @param str string
         * @return true/false
         */
        public static boolean isJavaValidName(String str) {
                if (str.isEmpty()) return false;
                if (javaKeys.contains(str)) return false;
                char first = str.charAt(0);
                if (isValidNameStartChar(first)) {
                        for (int i = 1; i < str.length(); ++i) {
                                char c = str.charAt(i);
                                if (!isValidNameChar(c)) return false;
                        }
                        return true;
                } else {
                        return false;
                }
        }

        /**
         * check whether the given string can be a name
         *
         * @param str string
         * @return true/false
         */
        public static boolean isValidName(String str) {
                if (str.startsWith("`") && str.endsWith("`")) {
                        return isJavaValidName(str.substring(1, str.length() - 1));
                }
                return isJavaValidName(str) && !keys.contains(str);
        }

        /**
         * check whether is defined as a package access
         *
         * @param element element
         * @return true/false (packageName::name)
         */
        public static boolean isPackage(Element element) {
                if (element.getTokenType() == TokenType.VALID_NAME && element.hasNext()) {
                        Node next = element.next();
                        if (next instanceof Element) {
                                String nextContent = ((Element) next).getContent();
                                if (nextContent.equals("::") && next.hasNext()) {
                                        Node nextNext = next.next();
                                        if (nextNext instanceof Element) {
                                                return nextNext.getTokenType() == TokenType.VALID_NAME;
                                        }
                                }
                        }
                }
                return false;
        }

        /**
         * check whether the given char can be one of a name
         *
         * @param c char
         * @return true/false (a-z|A-Z|$|_|0-9)
         */
        public static boolean isValidNameChar(char c) {
                return isValidNameStartChar(c) || (c >= '0' && c <= '9');
        }

        /**
         * check whether the given char can be start of a name
         *
         * @param c char
         * @return true/false (a-z|A-Z|$|_)
         */
        public static boolean isValidNameStartChar(char c) {
                return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '$' || c == '_';
        }

        private static Set<String> modifiers = new HashSet<>(Arrays.asList(
                "public", "protected", "private", "internal",
                "abstract", "val", "native", "synchronized", "transient", "volatile", "strictfp",
                "data", "var", "def", "nonnull", "nonempty", "implicit"
        ));

        private static Set<String> accessModifiers = new HashSet<>(Arrays.asList(
                "public", "protected", "private", "internal"
        ));

        public static boolean isModifier(String str) {
                return modifiers.contains(str);
        }

        /**
         * check whether the str represented modifier is compatible with existing modifiers
         *
         * @param str       str
         * @param modifiers modifiers
         * @return true/false
         */
        public static boolean modifierIsCompatible(String str, Set<Modifier> modifiers) {
                boolean isAccessMod = accessModifiers.contains(str);
                Modifier.Available mod = getModifierFromString(str);
                for (Modifier m : modifiers) {
                        if (m.modifier.equals(mod)
                                ||
                                (isAccessMod &&
                                        (m.modifier.equals(Modifier.Available.PUBLIC)
                                                || m.modifier.equals(Modifier.Available.PRIVATE)
                                                || m.modifier.equals(Modifier.Available.PROTECTED)
                                                || m.modifier.equals(Modifier.Available.PKG))
                                )
                                || (mod.equals(Modifier.Available.VAL) && m.modifier.equals(Modifier.Available.ABSTRACT))
                                || (mod.equals(Modifier.Available.ABSTRACT) && m.modifier.equals(Modifier.Available.VAL))
                                || (mod.equals(Modifier.Available.VAL) && m.modifier.equals(Modifier.Available.VAR))
                                || (mod.equals(Modifier.Available.VAR) && m.modifier.equals(Modifier.Available.VAL))
                                )
                                return false;
                }
                return true;
        }

        public static Modifier.Available getModifierFromString(String str) {
                switch (str) {
                        case "public":
                                return Modifier.Available.PUBLIC;
                        case "private":
                                return Modifier.Available.PRIVATE;
                        case "protected":
                                return Modifier.Available.PROTECTED;
                        case "internal":
                                return Modifier.Available.PKG;
                        case "abstract":
                                return Modifier.Available.ABSTRACT;
                        case "val":
                                return Modifier.Available.VAL;
                        case "native":
                                return Modifier.Available.NATIVE;
                        case "synchronized":
                                return Modifier.Available.SYNCHRONIZED;
                        case "transient":
                                return Modifier.Available.TRANSIENT;
                        case "volatile":
                                return Modifier.Available.VOLATILE;
                        case "strictfp":
                                return Modifier.Available.STRICTFP;
                        case "data":
                                return Modifier.Available.DATA;
                        case "var":
                                return Modifier.Available.VAR;
                        case "def":
                                return Modifier.Available.DEF;
                        case "nonnull":
                                return Modifier.Available.NONNULL;
                        case "nonempty":
                                return Modifier.Available.NONEMPTY;
                        case "implicit":
                                return Modifier.Available.IMPLICIT;
                        default:
                                throw new LtBug("invalid modifier " + str);
                }
        }

        public static final int NOT_METHOD_DEF = 0;
        public static final int METHOD_DEF_NORMAL = 1;
        public static final int METHOD_DEF_TYPE = 2;
        public static final int METHOD_DEF_EMPTY = 3;
        public static final int METHOD_DEF_ONE_STMT = 4;

        public static Node get_next_node(Node n) {
                if (n == null) return null;

                if (n.next() instanceof EndingNode) {
                        return get_next_node(n.next());
                } else {
                        return n.next();
                }
        }

        /**
         * check whether the element starts a lambda.
         * The element may be (...)-&gt; or xxx-&gt;
         *
         * @param elem elem
         * @return true / false
         * @throws UnexpectedEndException syntax error
         */
        public static boolean isLambda(Element elem) throws UnexpectedEndException {
                if (elem.getContent().equals("(")) {
                        Node n = get_next_node(elem);
                        if (n instanceof ElementStartNode) {
                                n = get_next_node(n);
                        }
                        if (n instanceof Element) {
                                if (((Element) n).getContent().equals(")")) {
                                        n = get_next_node(n);
                                        if (n instanceof Element && ((Element) n).getContent().equals("->")) {
                                                return true;
                                        }
                                }
                        }
                } else if (elem.getTokenType() == TokenType.VALID_NAME) {
                        Node n = get_next_node(elem);
                        if (n instanceof Element) {
                                if (((Element) n).getContent().equals("->")) {
                                        return true;
                                }
                        }
                }
                return false;
        }

        public static int checkMethodDef(Element elem, boolean annosOrModifiersNotEmpty) throws UnexpectedEndException {
                if (elem.getTokenType() == TokenType.VALID_NAME) {
                        Node nodeAfterRightPar = null;
                        Node rightPar = null;

                        // method
                        Node n1 = get_next_node(elem);
                        if (n1 instanceof Element) {
                                String p = ((Element) n1).getContent();
                                if (p.equals("(")) {
                                        Node n2 = get_next_node(n1);
                                        if (n2 instanceof ElementStartNode) {
                                                // method(口
                                                Node n3 = get_next_node(n2);
                                                if (n3 instanceof Element) {
                                                        // method(口)
                                                        if (((Element) n3).getContent().equals(")")) {
                                                                rightPar = n3;
                                                                nodeAfterRightPar = get_next_node(n3);
                                                        }
                                                }
                                        } else if (n2 instanceof Element) {
                                                // method()
                                                if (((Element) n2).getContent().equals(")")) {
                                                        rightPar = n2;
                                                        nodeAfterRightPar = get_next_node(n2);
                                                }
                                        }
                                }
                        }

                        if (nodeAfterRightPar == null) {
                                if (rightPar != null && annosOrModifiersNotEmpty) {
                                        return METHOD_DEF_EMPTY;
                                }
                        } else {
                                if (nodeAfterRightPar instanceof ElementStartNode) {
                                        if (annosOrModifiersNotEmpty) {
                                                return METHOD_DEF_NORMAL;
                                        }
                                } else if (nodeAfterRightPar instanceof Element) {
                                        String s = ((Element) nodeAfterRightPar).getContent();
                                        if (s.equals(":")) {
                                                return METHOD_DEF_TYPE;
                                        } else if (s.equals("=")) {
                                                Node nn = get_next_node(nodeAfterRightPar);
                                                if (nn instanceof Element) {
                                                        if (((Element) nn).getContent().equals("..."))
                                                                return METHOD_DEF_EMPTY;
                                                        else
                                                                return METHOD_DEF_ONE_STMT;
                                                }
                                        }
                                } else if (nodeAfterRightPar instanceof EndingNode && annosOrModifiersNotEmpty) {
                                        return METHOD_DEF_EMPTY;
                                }
                        }
                }
                return NOT_METHOD_DEF;
        }

        private static Set<String> twoVarOperators;

        public static boolean isTwoVariableOperator(String str) {
                return twoVarOperators.contains(str);
        }

        private static Set<String> oneVarOperatorsPost = new HashSet<>(Arrays.asList(
                "++", "--"
        ));

        public static boolean isOneVariableOperatorPost(String str) {
                return oneVarOperatorsPost.contains(str);
        }

        private static Set<String> oneVarOperatorsPreWithoutCheckingExps = new HashSet<>(Arrays.asList(
                "!", "~"
        ));

        private static Set<String> oneVarOperatorsPreMustCheckExps = new HashSet<>(Arrays.asList(
                "++", "--", "!", "~", "+", "-"
        ));

        public static boolean isOneVariableOperatorPreWithoutCheckingExps(String str) {
                return oneVarOperatorsPreWithoutCheckingExps.contains(str);
        }

        public static boolean isOneVariableOperatorPreMustCheckExps(String str) {
                return oneVarOperatorsPreMustCheckExps.contains(str);
        }

        private static String[][] twoVar_priority = {
                // invokes concat(?)
                {":::"},
                {"^^"}, // pow
                {"*", "/", "%"},
                {"+", "-"},
                {"<<", ">>", ">>>"},
                {">", "<", ">=", "<="},
                {"==", "!=", "===", "!==", "=:=", "!:=", "is", "not", "in"},
                {"&"},
                {"^"},
                {"|"},
                {"&&", "and"},
                {"||", "or"},
                {":="}
        };

        /**
         * a higher or equal to b
         *
         * @param a a
         * @param b b
         * @return true if a higher or equal to b
         */
        public static boolean twoVar_higherOrEqual(String a, String b) {
                int indexA = find_twoVar_priority(a);
                if (indexA == -1) {
                        indexA = twoVar_priority.length;
                }
                int indexB = find_twoVar_priority(b);
                if (indexB == -1) {
                        indexB = twoVar_priority.length;
                }
                return indexA <= indexB;
        }

        private static int find_twoVar_priority(String s) {
                for (int i = 0; i < twoVar_priority.length; ++i) {
                        String[] arr = twoVar_priority[i];
                        for (String anArr : arr) {
                                if (anArr.equals(s)) {
                                        return i;
                                }
                        }
                }
                return -1;
        }

        public static void expecting(String token, Node previous, Node got, ErrorManager err) throws UnexpectedTokenException, UnexpectedEndException {
                if (got == null) {
                        err.UnexpectedEndException(previous.getLineCol());
                } else if (!(got instanceof Element)) {
                        throw new UnexpectedTokenException("", token, got.getClass().getSimpleName(), got.getLineCol());
                } else if (!((Element) got).getContent().endsWith(token)) {
                        throw new UnexpectedTokenException("", token, ((Element) got).getContent(), got.getLineCol());
                }
        }

        public static boolean isAssign(String s) {
                if (s.equals("=")) return true;
                if (s.endsWith("=")) {
                        String before = s.substring(0, s.length() - 1);
                        if (isTwoVariableOperator(before)) return true;
                }
                return false;
        }

        public static boolean isDestructing(String str) {
                return str.equals("<-");
        }

        public static boolean isDestructing(Element e) {
                if (e.getTokenType() != TokenType.VALID_NAME) return false;
                while (true) {
                        if (!(e.next() instanceof Element)) return false;
                        e = (Element) e.next();
                        if (e.getContent().equals("(")) break;
                        if (e.getTokenType() != TokenType.VALID_NAME
                                && !e.getContent().equals("::")
                                && !e.getContent().equals(".")) {
                                return false;
                        }
                }

                Node rightPar;
                // (
                if (e.next() instanceof ElementStartNode) {
                        if (e.next().next() instanceof EndingNode
                                && e.next().next().next() instanceof Element
                                && ((Element) e.next().next().next()).getContent().equals(")")) {
                                rightPar = e.next().next().next();
                        } else {
                                return false;
                        }
                } else if (e.next() instanceof Element) {
                        if (((Element) e.next()).getContent().equals(")")) {
                                // )
                                rightPar = e.next();
                        } else {
                                return false;
                        }
                } else return false;

                return rightPar.next() instanceof Element && ((Element) rightPar.next()).getContent().equals("<-");
        }

        public static boolean isDestructingWithoutType(Element e) {
                // previous node is not TypeName
                if (e.previous() != null && e.previous().getTokenType() == TokenType.VALID_NAME) return false;
                // (......)
                if (!e.getContent().equals("(")) return false;
                if (e.next() instanceof Element) {
                        e = (Element) e.next();
                } else if (e.next() instanceof ElementStartNode) {
                        Node n = e.next();
                        if (n.next() instanceof EndingNode) n = n.next();
                        if (!(n.next() instanceof Element)) return false;
                        e = (Element) n.next();
                } else return false;
                if (!e.getContent().equals(")")) return false;

                // <- | =
                if (!(e.next() instanceof Element)) return false;
                if (!((Element) e.next()).getContent().equals("<-")
                        &&
                        !((Element) e.next()).getContent().equals("=")) return false;
                //
                return true;
        }

        public static boolean isSync(Element elem) {
                String content = elem.getContent();
                if (content.equals("synchronized")) {
                        Node n = get_next_node(elem);
                        if (n instanceof Element) {
                                String s = ((Element) n).getContent();
                                if (s.equals("(")) {
                                        return true;
                                }
                        }
                }
                return false;
        }

        private static Set<String> primitives = new HashSet<>(Arrays.asList(
                "int", "double", "float", "short", "long", "byte", "char", "bool"
        ));

        public static boolean isPrimitive(String s) {
                return primitives.contains(s);
        }

        static {
                // 2 var op
                twoVarOperators = new HashSet<>();
                for (String[] sArr : twoVar_priority) {
                        Collections.addAll(twoVarOperators, sArr);
                }

                keys.addAll(modifiers);
                keys.addAll(javaKeys);
        }

        public static String validateValidName(String validName) {
                if (validName.startsWith("`")) return validName.substring(1, validName.length() - 1);
                return validName;
        }

        public static boolean isSymbol(String str) {
                return isTwoVariableOperator(str)
                        || isOneVariableOperatorPost(str)
                        || isOneVariableOperatorPreMustCheckExps(str)
                        || isOneVariableOperatorPreWithoutCheckingExps(str)
                        || isAssign(str)
                        || isDestructing(str);
        }
}
