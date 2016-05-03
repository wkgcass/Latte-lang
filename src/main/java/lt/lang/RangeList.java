package lt.lang;

import java.util.*;

/**
 * range list
 */
public class RangeList extends AbstractList {
        private static final Object lock = new Object();

        private List<Object> list;
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

        private void initList() {
                if (list == null) {
                        synchronized (lock) {
                                if (list == null) {
                                        list = new ArrayList<>();
                                        if (increment != 0) for (int i = start; end_inclusive ? i != end + increment : i != end; i += increment) list.add(i);
                                }
                        }
                }
        }

        @Override
        public void add(int index, Object element) {
                initList();
                list.add(index, element);
        }

        @Override
        public Object remove(int index) {
                initList();
                return list.remove(index);
        }

        @Override
        public Object get(int index) {
                if (list == null) {
                        if (index >= 0 && index < size()) {
                                return index * increment + start;
                        } else throw new IndexOutOfBoundsException(String.valueOf(index));
                } else {
                        return list.get(index);
                }
        }

        @Override
        public int size() {
                if (null == list) {
                        if (end - start >= 0) {
                                return end - start + (end_inclusive ? 1 : 0);
                        } else {
                                return start - end + (end_inclusive ? 1 : 0);
                        }
                } else return list.size();
        }
}
