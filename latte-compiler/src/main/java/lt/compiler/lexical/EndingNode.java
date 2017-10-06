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
 * ends a expression or statement
 */
public class EndingNode extends Node {
        public static final int STRONG = 0;
        public static final int WEAK = 1;
        public static final int SYNTHETIC = 2;

        private int type;

        public EndingNode(Args args, int type) {
                super(args, type == STRONG ? TokenType.EndingNodeStrong : TokenType.EndingNode);
                if (type != STRONG && type != WEAK && type != SYNTHETIC) {
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
        public boolean equalsIgnoreIndent(Node node) {
                return super.equalsIgnoreIndent(node);
        }

        @Override
        public String toString() {
                return (type == STRONG ? "," : "NewLine");
        }

        @Override
        public void remove() {
                throw new UnsupportedOperationException();
        }
}
