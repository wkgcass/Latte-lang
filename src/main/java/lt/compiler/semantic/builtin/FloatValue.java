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
 * float value
 */
public class FloatValue implements PrimitiveValue, ConstantValue {
        private final float value;

        public FloatValue(float value) {
                this.value = value;
        }

        @Override
        public STypeDef type() {
                return FloatTypeDef.get();
        }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                FloatValue that = (FloatValue) o;

                return Float.compare(that.value, value) == 0;
        }

        @Override
        public int hashCode() {
                return (value != +0.0f ? Float.floatToIntBits(value) : 0);
        }

        @Override
        public byte[] getByte() {
                int data = Float.floatToIntBits(value);
                byte[] bytes = new byte[4];
                bytes[0] = (byte) (data & 0xff);
                bytes[1] = (byte) ((data & 0xff00) >> 8);
                bytes[2] = (byte) ((data & 0xff0000) >> 16);
                bytes[3] = (byte) ((data & 0xff000000) >> 24);
                return bytes;
        }

        @Override
        public String toString() {
                return Float.toString(value);
        }

        public float getValue() {
                return value;
        }
}
