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

package lt.lang;

import java.util.*;

/**
 * range list. The list is used to support <code>..</code> and <code>.:</code> operators,
 * and the list is immutable
 */
public class RangeList extends AbstractList {
        private static final Object lock = new Object();
        private final int start;
        private final int end;
        // [start,end]
        private final boolean end_inclusive;

        private final int increment;

        public RangeList(int start, int end, boolean end_inclusive) {
                this.end = end;
                this.start = start;
                this.end_inclusive = end_inclusive;

                if (end == start) increment = 0;
                else increment = end - start > 0 ? 1 : -1;
        }

        @Override
        public Object get(int index) {
                if (index >= 0 && index < size()) {
                        return index * increment + start;
                } else throw new IndexOutOfBoundsException(String.valueOf(index));
        }

        @Override
        public int size() {
                if (end - start >= 0) {
                        return end - start + (end_inclusive ? 1 : 0);
                } else {
                        return start - end + (end_inclusive ? 1 : 0);
                }
        }
}
