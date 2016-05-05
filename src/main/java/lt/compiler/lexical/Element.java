package lt.compiler.lexical;

import lt.compiler.CompileUtil;

/**
 * element
 */
public class Element extends Node {
        private String content;

        public Element(Args args, String content, TokenType tokenType) {
                super(args, tokenType);
                this.content = content;
        }

        public String getContent() {
                return content;
        }

        public void checkWhetherIsValidName() {
                if (getTokenType() == TokenType.VALID_NAME) {
                        content = CompileUtil.validateValidName(content);
                }
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                if (!super.equals(o)) return false;

                Element element = (Element) o;

                return !(content != null ? !content.equals(element.content) : element.content != null);
        }

        @Override
        public String toString(int indent) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < indent; ++i) {
                        sb.append(" ");
                }
                sb.append(content).append("\n");
                if (hasNext()) {
                        sb.append(next().toString(indent));
                }
                return sb.toString();
        }

        @Override
        public String toString() {
                return content;
        }
}
