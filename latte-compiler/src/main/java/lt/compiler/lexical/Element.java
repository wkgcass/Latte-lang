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

import lt.compiler.CompileUtil;

/**
 * element
 */
public class Element extends Node {
        private String content;

        public Element(Args args, String content, TokenType tokenType) {
                super(args, tokenType);
                this.content = content;
                getLineCol().length = this.content.length();
        }

        public String getContent() {
                return content;
        }

        public void setContent(String content) {
                this.content = content;
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
                sb.append(content).append("    ").append(getTokenType()).append("\n");
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
