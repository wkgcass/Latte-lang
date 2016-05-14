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
 * long value
 */
public class LongValue implements PrimitiveValue, ConstantValue {
        private final long value;

        public LongValue(long value) {
                this.value = value;
        }

        @Override
        public STypeDef type() {
                return LongTypeDef.get();
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                LongValue longValue = (LongValue) o;

                return value == longValue.value;
        }

        @Override
        public int hashCode() {
                return (int) (value ^ (value >>> 32));
        }

        @Override
        public byte[] getByte() {
                byte[] bytes = new byte[8];
                bytes[0] = (byte) (value & 0xff);
                bytes[1] = (byte) ((value >> 8) & 0xff);
                bytes[2] = (byte) ((value >> 16) & 0xff);
                bytes[3] = (byte) ((value >> 24) & 0xff);
                bytes[4] = (byte) ((value >> 32) & 0xff);
                bytes[5] = (byte) ((value >> 40) & 0xff);
                bytes[6] = (byte) ((value >> 48) & 0xff);
                bytes[7] = (byte) ((value >> 56) & 0xff);
                return bytes;
        }

        @Override
        public String toString() {
                return Long.toString(value);
        }

        public long getValue() {
                return value;
        }
}
