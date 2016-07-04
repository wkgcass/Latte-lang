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

package lt.compiler.semantic;

import lt.compiler.LineCol;

/**
 * annotation field
 */
public class SAnnoField extends SMethodDef {
        private String name;
        private STypeDef type;
        private Value defaultValue;

        public SAnnoField() {
                super(LineCol.SYNTHETIC);
        }

        public void setDefaultValue(Value defaultValue) {
                this.defaultValue = defaultValue;
        }

        public void setName(String name) {
                this.name = name;
        }

        public void setType(STypeDef type) {
                this.type = type;
        }

        public String name() {
                return name;
        }

        public STypeDef type() {
                return type;
        }

        public Value defaultValue() {
                return defaultValue;
        }

        @Override
        public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append(type.fullName()).append(" ").append(name).append("()");
                if (defaultValue != null) {
                        sb.append(" default ").append(defaultValue).append(";");
                }
                return sb.toString();
        }
}
