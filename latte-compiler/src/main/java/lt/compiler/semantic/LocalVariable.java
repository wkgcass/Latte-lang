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

/**
 * local variable
 */
public class LocalVariable implements LeftValue {
        private final boolean canChange;
        private STypeDef type;
        private boolean used;

        private boolean alreadyAssigned = false;

        private SParameter wrappingParam;

        public LocalVariable(STypeDef type, boolean canChange) {
                this.type = type;
                this.canChange = canChange;
        }

        public void setType(STypeDef type) {
                this.type = type;
        }

        @Override
        public boolean canChange() {
                return canChange;
        }

        @Override
        public boolean alreadyAssigned() {
                return alreadyAssigned;
        }

        @Override
        public void assign() {
                alreadyAssigned = true;
                setUsed(true);
        }

        @Override
        public boolean isUsed() {
                return used;
        }

        @Override
        public void setUsed(boolean used) {
                this.used = used;
        }

        @Override
        public STypeDef type() {
                return type;
        }

        @Override
        public String toString() {
                return "LocalVariable{" +
                        "alreadyAssigned=" + alreadyAssigned +
                        ", canChange=" + canChange +
                        ", type=" + type +
                        '}';
        }

        public SParameter getWrappingParam() {
                return wrappingParam;
        }

        public void setWrappingParam(SParameter wrappingParam) {
                this.wrappingParam = wrappingParam;
        }
}
