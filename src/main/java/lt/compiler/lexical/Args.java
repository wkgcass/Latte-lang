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

        public LineCol generateLineCol() {
                return new LineCol(fileName, currentLine, currentCol);
        }

        public Map<String, String> defined = new LinkedHashMap<>();
}
