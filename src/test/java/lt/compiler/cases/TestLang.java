package lt.compiler.cases;

import lt.lang.RangeList;
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
}
