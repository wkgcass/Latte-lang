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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * list for <tt>LessTyping</tt><br>
 * The List supports functions of <tt>Array</tt> provided in <tt>JavaScript</tt><br>
 * <ul>
 * <li>concat(...)</li>
 * <li>join(separator)</li>
 * <li>reverse()</li>
 * <li>shift()</li>
 * <li>slice(...)</li>
 * <li>unshift(...)</li>
 * </ul>
 * construct a list using <code>[elems...]</code>
 */
public class List extends LinkedList<Object> {
        /**
         * construct a new List
         */
        public List() {
        }

        /**
         * construct a new List with given initial elements
         *
         * @param list list containing initial elements
         */
        public List(Collection<?> list) {
                super(list);
        }

        /**
         * concat one element and return a new List.
         * the current list will not be modified
         *
         * @param elem the element to concat
         * @return a new List containing elements in current list and the given element
         */
        public List concat(Object elem) {
                List newList = new List();
                newList.addAll(this);
                newList.add(elem);
                return newList;
        }

        /**
         * concat all elements and return a new List.
         * the current list will not be modified
         *
         * @param elements elements to concat
         * @return a new List containing elements in both current list and the given list
         */
        public List concat(Collection<?> elements) {
                List newList = new List();
                newList.addAll(this);
                newList.addAll(elements);
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
                for (Object e : this) {
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
        public List reverse() {
                Collections.reverse(this);
                return this;
        }

        /**
         * remove and return the first element
         *
         * @return the removed value
         */
        public Object shift() {
                return remove(0);
        }

        /**
         * select elements from current list
         *
         * @param fromIndex low endpoint (inclusive) of the subList (negative number means size()-fromIndex)
         * @param toIndex   high endpoint (exclusive) of the subList (negative number means size()-toIndex)
         * @return a List containing selected elements
         */
        public List slice(int fromIndex, int toIndex) {
                int size = size();
                if (fromIndex < 0) fromIndex = size - fromIndex;
                if (toIndex < 0) toIndex = size - toIndex;
                return new List(subList(fromIndex, toIndex));
        }

        /**
         * select elements from current list
         *
         * @param fromIndex the subList starts at this position
         * @return a List containing selected elements
         */
        public List slice(int fromIndex) {
                return slice(fromIndex, size());
        }

        /**
         * insert one element to the head of the List
         *
         * @param element the element to insert
         * @return the original List object, also known as <code>this</code>
         */
        public List unshift(Object element) {
                add(0, element);
                return this;
        }

        /**
         * insert all elements to the head of the List
         *
         * @param elements elements to be inserted
         * @return the original List object, also known as <code>this</code>
         */
        public List unshift(Collection<?> elements) {
                addAll(0, elements);
                return this;
        }

        /**
         * Returns the number of elements in this list.
         *
         * @return the number of elements in this list
         */
        public int length() {
                return size();
        }
}
