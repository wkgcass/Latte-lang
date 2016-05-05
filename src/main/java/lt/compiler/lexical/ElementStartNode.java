package lt.compiler.lexical;

/**
 * starts an element
 */
public class ElementStartNode extends Node {
        private int indent;
        private Node linkedNode;

        public ElementStartNode(Args args, int indent) {
                super(args, TokenType.ElementStartNode);
                this.indent = indent;
        }

        public int getIndent() {
                return indent;
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                if (!super.equals(o)) return false;

                ElementStartNode that = (ElementStartNode) o;

                return !(linkedNode != null ? !linkedNode.equals(that.linkedNode) : that.linkedNode != null);
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
}
