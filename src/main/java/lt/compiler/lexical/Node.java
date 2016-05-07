package lt.compiler.lexical;

import lt.compiler.LineCol;

import java.util.Iterator;

/**
 * parsed node from plain jes text. the nodes are chained up
 */
public abstract class Node implements Iterator<Node>, Iterable {
        private LineCol lineCol;
        private Node next;
        private Node previous;

        private final TokenType tokenType;

        public Node(Args args, TokenType tokenType) {
                this.tokenType = tokenType;
                lineCol = args.generateLineCol();
                this.previous = args.previous;

                if (hasPrevious()) {
                        assert previous != null;
                        previous.next = this;
                } else if (!args.startNodeStack.empty()) {
                        args.startNodeStack.lastElement().setLinkedNode(this);
                }
        }

        public TokenType getTokenType() {
                return tokenType;
        }

        public boolean hasNext() {
                return next != null;
        }

        public boolean hasPrevious() {
                return previous != null;
        }

        public LineCol getLineCol() {
                return lineCol;
        }

        public void setLineCol(LineCol lineCol) {
                this.lineCol = lineCol;
        }

        public Node next() {
                return next;
        }

        public Node previous() {
                return previous;
        }

        public void setNext(Node next) {
                this.next = next;
        }

        public void setPrevious(Node previous) {
                this.previous = previous;
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                Node node = (Node) o;
                return tokenType == node.tokenType && !(next != null ? !next.equals(node.next) : node.next != null);

        }

        @Override
        public Iterator iterator() {
                return this;
        }

        public abstract String toString(int indent);
}
