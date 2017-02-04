package lt.lang.implicit.collection;

import java.util.Collections;
import java.util.Set;

/**
 * rich set
 */
public class RichSet<E> extends RichCollection<E> {
        private final Set<E> set;

        public RichSet(Set<E> set) {
                super(set);
                this.set = set;
        }

        /**
         * get an immutable set with all current elements contained
         *
         * @return an immutable set
         */
        public Set<E> immutable() {
                return Collections.unmodifiableSet(set);
        }
}
