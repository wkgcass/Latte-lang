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

package lt.compiler.lexical;

import lt.compiler.LineCol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

public class Args {
        public String fileName;
        /**
         * previous node
         */
        public Node previous;
        /**
         * current line to parse
         */
        public int currentLine;
        /**
         * current line to parse
         */
        public int currentCol;
        /**
         * element start nodes
         */
        public Stack<ElementStartNode> startNodeStack = new Stack<>();
        /**
         * pair stack
         */
        public Stack<PairEntry> pairEntryStack = new Stack<>();

        /**
         * @return a new LineCol object containing current file, line, column and whether it uses define command
         */
        public LineCol generateLineCol() {
                LineCol res = new LineCol(fileName, currentLine, currentCol);
                res.useDefine.putAll(useDefine);
                return res;
        }

        /**
         * the defined strings
         */
        public Map<String, String> defined = new LinkedHashMap<>();
        /**
         * whether this line uses define
         */
        public Map<String, String> useDefine = new LinkedHashMap<>();
        /**
         * is parsing multiple line comment
         */
        public boolean multipleLineComment = false;
}
