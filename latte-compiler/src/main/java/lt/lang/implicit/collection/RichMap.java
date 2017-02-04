package lt.lang.implicit.collection;

import java.util.Collections;
import java.util.Map;

/**
 * rich map
 */
public class RichMap<K, V> {
        private final Map<K, V> map;

        public RichMap(Map<K, V> map) {
                this.map = map;
        }

        /**
         * create an immutable map with all current entries contained
         *
         * @return an immutable map
         */
        public Map<K, V> immutable() {
                return Collections.unmodifiableMap(map);
        }
}
