package lt.lang.implicit.collection;

import lt.lang.function.Function1;

import java.util.LinkedList;
import java.util.List;

/**
 * rich iterator
 */
public class RichIterable<T> {
        private final Iterable<T> iterable;

        public RichIterable(Iterable<T> iterable) {
                this.iterable = iterable;
        }

        public void forEach(Function1<Void, ? super T> f) throws Exception {
                for (T t : iterable) {
                        f.apply(t);
                }
        }

        public List<T> filter(Function1<Boolean, ? super T> f) throws Exception {
                List<T> list = new LinkedList<T>();
                for (T t : iterable) {
                        if (f.apply(t)) {
                                list.add(t);
                        }
                }
                return list;
        }

        public <U> List<U> map(Function1<? extends U, ? super T> f) throws Exception {
                List<U> list = new LinkedList<U>();
                for (T t : iterable) {
                        list.add(f.apply(t));
                }
                return list;
        }
}
