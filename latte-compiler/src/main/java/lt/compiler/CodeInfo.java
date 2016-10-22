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

package lt.compiler;

import lt.compiler.semantic.Instruction;
import lt.dependencies.asm.Label;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * stack depth and local variable count
 */
public class CodeInfo {
        public enum Size {
                _1, _2
        }

        private int currentStackDepth;
        private Stack<Size> currentStack = new Stack<>();
        private int maxStack;
        private int maxLocal;

        public static class Container {
                public final Label label;
                public boolean isVisited;

                public Container(Label label) {
                        this.label = label;
                }
        }

        public final Map<Instruction, Container> insToLabel = new HashMap<>();

        public CodeInfo(int localInit) {
                this.maxLocal = localInit;
        }

        public void push(Size size) {
                currentStack.push(size);
                if (Size._1 == size) currentStackDepth += 1;
                else currentStackDepth += 2;
                if (currentStackDepth > maxStack) {
                        maxStack = currentStackDepth;
                }
        }

        public void pop(int count) {
                for (int i = 0; i < count; ++i) {
                        Size size = currentStack.pop();
                        if (size == Size._1) currentStackDepth -= 1;
                        else currentStackDepth -= 2;
                }
        }

        public int getMaxLocal() {
                return maxLocal;
        }

        public void registerLocal(int theLocal) {
                if (theLocal + 1 > this.maxLocal) {
                        this.maxLocal = theLocal + 1;
                }
        }

        public int getMaxStack() {
                return maxStack;
        }

        public int getCurrentStackDepth() {
                return currentStackDepth;
        }

        public Size peekSize() {
                return currentStack.peek();
        }

        @Override
        public String toString() {
                return "CodeInfo{" +
                        "maxStack=" + maxStack +
                        ", maxLocal=" + maxLocal +
                        '}';
        }
}
