package lt.lang.implicit;

import lt.lang.Implicit;
import lt.lang.LatteObject;
import lt.lang.implicit.collection.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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
                return new RichList<>(list);
        }

        @Implicit
        public <E> RichSet<E> cast(Set<E> set) {
                return new RichSet<>(set);
        }

        @Implicit
        public <E> RichCollection<E> cast(Collection<E> collection) {
                return new RichCollection<>(collection);
        }

        @Implicit
        public <K, V> RichMap cast(Map<K, V> map) {
                return new RichMap<>(map);
        }

        @Implicit
        public <T> RichStream<T> cast(Stream<T> stream) {
                return new RichStream<>(stream);
        }

        @Implicit
        public <E> List<E> castStreamToList(Stream<E> stream) {
                return cast(stream).toList();
        }

        @Implicit
        public <E> Set<E> castStreamToSet(Stream<E> stream) {
                return cast(stream).toSet();
        }
}
