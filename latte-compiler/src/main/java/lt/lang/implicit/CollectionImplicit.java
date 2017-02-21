package lt.lang.implicit;

import lt.runtime.Implicit;
import lt.runtime.LatteObject;
import lt.lang.implicit.collection.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * implicit casts on collections
 */
@LatteObject
@Implicit
public class CollectionImplicit {
        public static final CollectionImplicit singletonInstance = new CollectionImplicit();

        private CollectionImplicit() {
        }

        @Implicit
        public <E> RichList<E> cast(List<E> list) {
                return new RichList<E>(list);
        }

        @Implicit
        public <E> RichSet<E> cast(Set<E> set) {
                return new RichSet<E>(set);
        }

        @Implicit
        public <K, V> RichMap cast(Map<K, V> map) {
                return new RichMap<K, V>(map);
        }

        @Implicit
        public <T> RichIterable<T> cast(Iterable<T> i) {
                return new RichIterable<T>(i);
        }
}
