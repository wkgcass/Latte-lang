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

import java.util.ArrayList;
import java.util.List;

/**
 * parameter definition
 */
public class SParameter implements LeftValue, SAnnotationPresentable {
        private String name;
        private final List<SAnno> annos = new ArrayList<SAnno>();
        private STypeDef type;
        private SInvokable target;
        private boolean canChange = true;
        private boolean notNull = false;
        private boolean notEmpty = false;
        private boolean used = false;
        private boolean isCapture = false;

        public void setTarget(SInvokable target) {
                this.target = target;
        }

        public void setName(String name) {
                this.name = name;
        }

        public void setType(STypeDef type) {
                this.type = type;
        }

        public void setCanChange(boolean canChange) {
                this.canChange = canChange;
        }

        public boolean isNotNull() {
                return notNull;
        }

        public void setNotNull(boolean notNull) {
                this.notNull = notNull;
        }

        public boolean isNotEmpty() {
                return notEmpty;
        }

        public void setNotEmpty(boolean notEmpty) {
                this.notEmpty = notEmpty;
        }

        public String name() {
                return name;
        }

        @Override
        public boolean canChange() {
                return canChange;
        }

        @Override
        public boolean alreadyAssigned() {
                return true;
        }

        @Override
        public void assign() {
                // empty implementation
                // not required
        }

        @Override
        public List<SAnno> annos() {
                return annos;
        }

        @Override
        public STypeDef type() {
                return type;
        }

        public SInvokable target() {
                return target;
        }

        @Override
        public String toString() {
                String str = "";
                if (!canChange()) str += "final ";
                str += type().fullName() + " " + name();
                return str;
        }

        @Override
        public void setUsed(boolean used) {
                this.used = used;
        }

        @Override
        public boolean isUsed() {
                return used;
        }

        /**
         * check whether the parameter is a captured param
         *
         * @return true/false
         */
        public boolean isCapture() {
                return isCapture;
        }

        /**
         * mark the parameter whether it's a captured param
         *
         * @param capture true/false
         */
        public void setCapture(boolean capture) {
                isCapture = capture;
        }
}
