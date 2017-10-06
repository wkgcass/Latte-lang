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
import lt.compiler.LtBug;

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
                if (tokenType == null) throw new LtBug("tokenType should not be null");
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

        public boolean equalsIgnoreIndent(Node o) {
                return this == o || !(o == null || getClass() != o.getClass()) && tokenType == o.tokenType && !(next != null ? !next.equalsIgnoreIndent(o.next) : o.next != null);

        }
}
