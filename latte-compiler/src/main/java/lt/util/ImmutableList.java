package lt.util;

import java.util.*;
import java.util.List;

/**
 * the immutable list. supports immutable operations.
 */
public class ImmutableList extends AbstractList<Object> {
        private final java.util.List<Object> list;

        public ImmutableList(java.util.List<?> list) {
                this.list = new ArrayList<>(list);
        }

        @Override
        public Object get(int index) {
                return list.get(index);
        }

        @Override
        public int size() {
                return list.size();
        }

        public int length() {
                return size();
        }

        /**
         * concat one element and return a new List.
         * the current list will not be modified
         *
         * @param elem the element to concat
         * @return a new List containing elements in current list and the given element
         */
        public ImmutableList concat(Object elem) {
                if (elem instanceof java.util.List) return concat((List<?>) elem);
                ImmutableList immList = new ImmutableList(list);
                immList.list.add(elem);
                return immList;
        }

        /**
         * concat all elements and return a new List.
         * the current list will not be modified
         *
         * @param elements elements to concat
         * @return a new List containing elements in both current list and the given list
         */
        public ImmutableList concat(java.util.List<?> elements) {
                ImmutableList immList = new ImmutableList(list);
                immList.list.addAll(elements);
                return immList;
        }

        /**
         * put all elements into a string, the string is separated by the given separator
         *
         * @param separator separator
         * @return a string containing all elements and separated by the separator
         */
        public String join(String separator) {
                StringBuilder sb = new StringBuilder();
                boolean isFirst = true;
                for (Object e : this) {
                        if (isFirst) isFirst = false;
                        else sb.append(separator);
                        sb.append(e);
                }
                return sb.toString();
        }

        /**
         * select elements from current list
         *
         * @param fromIndex low endpoint (inclusive) of the subList (negative number means size()-fromIndex)
         * @param toIndex   high endpoint (exclusive) of the subList (negative number means size()-toIndex)
         * @return a List containing selected elements
         */
        public ImmutableList slice(int fromIndex, int toIndex) {
                int size = size();
                if (fromIndex < 0) fromIndex = size - fromIndex;
                if (toIndex < 0) toIndex = size - toIndex;
                return new ImmutableList(subList(fromIndex, toIndex));
        }

        /**
         * select elements from current list
         *
         * @param fromIndex the subList starts at this position
         * @return a List containing selected elements
         */
        public ImmutableList slice(int fromIndex) {
                return new ImmutableList(subList(fromIndex, size()));
        }
}
