package lt.lang.implicit.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * rich list
 */
@SuppressWarnings("unused")
public class RichList<E> {
        private final List<E> list;

        public RichList(List<E> list) {
                this.list = list;
        }

        /**
         * concat one element and return a new List.
         * the current list will not be modified
         *
         * @param elem the element to concat
         * @return a new List containing elements in current list and the given element
         */
        public List<E> concat(E elem) {
                if (elem instanceof Collection) {
                        return concat((Collection) elem);
                }
                List<E> newList = new LinkedList<E>(list);
                newList.add(elem);
                return newList;
        }

        /**
         * concat all elements and return a new List.
         * the current list will not be modified
         *
         * @param collection elements to concat
         * @return a new List containing elements in both current list and the given list
         */
        @SuppressWarnings("unchecked")
        public List<E> concat(Collection collection) {
                List<E> newList = new LinkedList<E>(list);
                newList.addAll(collection);
                return newList;
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
                for (E e : list) {
                        if (isFirst) isFirst = false;
                        else sb.append(separator);
                        sb.append(e);
                }
                return sb.toString();
        }

        /**
         * reverse the elements' sequence<br>
         * the method will modify the current List and won't generate a new List
         *
         * @return the original List object, also known as <code>this</code>
         */
        public List<E> reverse() {
                Collections.reverse(list);
                return list;
        }

        /**
         * remove and return the first element
         *
         * @return the removed value
         */
        public E shift() {
                return list.remove(0);
        }

        /**
         * select elements from current list
         *
         * @param fromIndex low endpoint (inclusive) of the subList (negative number means size()-fromIndex)
         * @param toIndex   high endpoint (exclusive) of the subList (negative number means size()-toIndex)
         * @return a List containing selected elements
         */
        public List<E> slice(int fromIndex, int toIndex) {
                int size = list.size();
                if (fromIndex < 0) fromIndex = size - fromIndex;
                if (toIndex < 0) toIndex = size - toIndex;
                return new LinkedList<E>(list.subList(fromIndex, toIndex));
        }

        /**
         * select elements from current list
         *
         * @param fromIndex the subList starts at this position
         * @return a List containing selected elements
         */
        public List slice(int fromIndex) {
                return slice(fromIndex, list.size());
        }

        /**
         * insert one element to the head of the List
         *
         * @param element the element to insert
         * @return the original List object, also known as <code>this</code>
         */
        public List<E> unshift(E element) {
                if (element instanceof Collection) {
                        return unshift((Collection) element);
                }
                list.add(0, element);
                return list;
        }

        /**
         * insert all elements to the head of the List
         *
         * @param elements elements to be inserted
         * @return the original List object, also known as <code>this</code>
         */
        @SuppressWarnings("unchecked")
        public List<E> unshift(Collection elements) {
                list.addAll(0, elements);
                return list;
        }

        /**
         * Returns the number of elements in this list.
         *
         * @return the number of elements in this list
         */
        public int length() {
                return list.size();
        }

        /**
         * create an immutable list with all current elements contained
         *
         * @return an immutable list
         */
        public List<E> immutable() {
                return Collections.unmodifiableList(list);
        }
}
