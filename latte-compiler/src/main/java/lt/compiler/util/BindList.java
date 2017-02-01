package lt.compiler.util;

import java.util.AbstractList;
import java.util.List;

/**
 * bind 2 lists into one
 */
public class BindList<E> extends AbstractList<E> {
        private final List<E> list1;
        private final List<E> list2;

        public BindList(List<E> list1, List<E> list2) {
                this.list1 = list1;
                this.list2 = list2;
        }

        @Override
        public E get(int index) {
                if (index < 0 || index >= list1.size() + list2.size())
                        throw new IndexOutOfBoundsException(String.valueOf(index));
                if (index < list1.size()) return list1.get(index);
                index -= list1.size();
                return list2.get(index);
        }

        @Override
        public E set(int index, E element) {
                if (index < 0 || index >= list1.size() + list2.size())
                        throw new IndexOutOfBoundsException(String.valueOf(index));
                if (index < list1.size()) return list1.set(index, element);
                index -= list1.size();
                return list2.set(index, element);
        }

        @Override
        public int size() {
                return list1.size() + list2.size();
        }
}
