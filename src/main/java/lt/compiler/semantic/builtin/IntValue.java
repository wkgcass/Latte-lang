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

package lt.compiler.semantic.builtin;

import lt.compiler.semantic.ConstantValue;
import lt.compiler.semantic.PrimitiveValue;
import lt.compiler.semantic.STypeDef;

/**
 * int value
 */
public class IntValue implements PrimitiveValue, ConstantValue {
        private final int value;

        public IntValue(int value) {
                this.value = value;
        }

        @Override
        public STypeDef type() {
                return IntTypeDef.get();
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                IntValue intValue = (IntValue) o;

                return value == intValue.value;
        }

        @Override
        public int hashCode() {
                return value;
        }

        @Override
        public byte[] getByte() {
                byte[] bytes = new byte[4];
                bytes[0] = (byte) (value & 0xff);
                bytes[1] = (byte) ((value & 0xff00) >> 8);
                bytes[2] = (byte) ((value & 0xff0000) >> 16);
                bytes[3] = (byte) ((value & 0xff000000) >> 24);
                return bytes;
        }

        @Override
        public String toString() {
                return Integer.toString(value);
        }

        public int getValue() {
                return value;
        }
}
