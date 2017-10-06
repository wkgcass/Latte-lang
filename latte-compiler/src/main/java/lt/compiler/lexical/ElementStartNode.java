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

/**
 * starts an element
 */
public class ElementStartNode extends Node {
        private final Indent indent;
        private Node linkedNode;

        public ElementStartNode(Args args, Indent indent) {
                super(args, TokenType.ElementStartNode);
                this.indent = indent;
        }

        public Indent getIndent() {
                return indent;
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                if (!super.equals(o)) return false;

                ElementStartNode that = (ElementStartNode) o;

                return !(linkedNode != null ? !linkedNode.equals(that.linkedNode) : that.linkedNode != null) && this.getIndent().equals(that.getIndent());
        }

        @Override
        public String toString(int indent) {
                StringBuilder sb = new StringBuilder();
                if (hasLinkedNode()) {
                        sb.append(getLinkedNode().toString(indent + 4));
                }
                if (hasNext()) {
                        sb.append(next().toString(indent));
                }
                return sb.toString();
        }

        public boolean hasLinkedNode() {
                return linkedNode != null;
        }

        public void setLinkedNode(Node linkedNode) {
                this.linkedNode = linkedNode;
        }

        public Node getLinkedNode() {
                return linkedNode;
        }

        @Override
        public String toString() {
                return "NewLayer";
        }

        @Override
        public void remove() {
                throw new UnsupportedOperationException();
        }

        @Override
        public boolean equalsIgnoreIndent(Node o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                if (!super.equalsIgnoreIndent(o)) return false;

                ElementStartNode that = (ElementStartNode) o;

                return !(linkedNode != null ? !linkedNode.equalsIgnoreIndent(that.linkedNode) : that.linkedNode != null);
        }
}
