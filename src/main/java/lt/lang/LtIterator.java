package lt.lang;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * iterable
 */
public abstract class LtIterator implements Iterator {
        private Object[] array;
        private Iterator it;
        private Enumeration en;

        static class ArrayIt extends LtIterator {
                final Object[] array;
                int index = 0;

                ArrayIt(Object[] array) {
                        this.array = array;
                }

                @Override
                public boolean hasNext() {
                        return index < array.length; // index from 0 to array.length
                }

                @Override
                public Object next() {
                        return array[index++]; // index from 0 to array.length
                }
        }

        static class It extends LtIterator {
                final Iterator it;

                It(Iterator it) {
                        this.it = it;
                }

                @Override
                public boolean hasNext() {
                        return it.hasNext();
                }

                @Override
                public Object next() {
                        return it.next();
                }
        }

        static class EnIt extends LtIterator {
                final Enumeration en;

                EnIt(Enumeration en) {
                        this.en = en;
                }

                @Override
                public boolean hasNext() {
                        return en.hasMoreElements();
                }

                @Override
                public Object next() {
                        return en.nextElement();
                }
        }

        public static LtIterator getIterator(Object o) {
                if (o.getClass().isArray()) {
                        return new ArrayIt((Object[]) o);
                } else if (o instanceof Iterable) {
                        return new It(((Iterable) o).iterator());
                } else if (o instanceof Iterator) {
                        return new It((Iterator) o);
                } else if (o instanceof Enumeration) {
                        return new EnIt((Enumeration) o);
                } else throw new RuntimeException("cannot iterate on " + o);
        }

        public abstract boolean hasNext();

        public abstract Object next();
}
