package lt.compiler.lexical;

/**
 * ends a expression or statement
 */
public class EndingNode extends Node {
        public static final int STRONG = 0;
        public static final int WEAK = 1;

        private int type;

        public EndingNode(Args args, int type) {
                super(args, type == STRONG ? TokenType.EndingNodeStrong : TokenType.EndingNode);
                if (type != STRONG && type != WEAK) {
                        throw new IllegalArgumentException("unexpected type number " + type + ", use EndingNode.STRONG or EndingNode.WEAK");
                }
                this.type = type;
        }

        public int getType() {
                return type;
        }

        @Override
        public String toString(int indent) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < indent; ++i) {
                        sb.append(" ");
                }
                sb.append("(").append(type == STRONG ? "STRONG" : "WEAK").append(")\n");
                if (hasNext()) {
                        sb.append(next().toString(indent));
                }
                return sb.toString();
        }

        @Override
        public String toString() {
                return (type == STRONG ? "," : "NewLine");
        }
}
