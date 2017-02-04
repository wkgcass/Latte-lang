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

package lt.compiler.cases;

import lt.lang.LtRuntime;
import lt.util.RangeList;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * lang
 */
public class TestLang {
        @Test
        public void testRangeListSize() throws Exception {
                RangeList list = new RangeList(1, 4, true);
                assertEquals(4, list.size());
        }

        @Test
        public void testRangeListGet() throws Exception {
                RangeList list = new RangeList(1, 4, true);
                assertEquals(2, list.get(1));
        }

        @Test
        public void testRangeListEquals() throws Exception {
                RangeList list = new RangeList(1, 4, true);
                assertEquals(Arrays.asList(1, 2, 3, 4), list);
        }

        @Test
        public void testRangeList1_1() throws Exception {
                RangeList list = new RangeList(1, 1, true);
                assertEquals(Collections.singletonList(1), list);

                RangeList list2 = new RangeList(1, 1, false);
                assertEquals(Collections.emptyList(), list2);
        }

        @Test
        public void testRequire() throws Throwable {
                Object o1 = LtRuntime.require(this.getClass(), "cp:test_require2.lts");
                Object o2 = LtRuntime.require(this.getClass(), "cp:test_require2.lts");
                assertTrue(o1 == o2);
        }
}
