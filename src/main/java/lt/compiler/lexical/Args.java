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
}
