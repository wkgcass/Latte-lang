package lt.lang.implicit.collection;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * rich stream
 */
public class RichStream<T> {
        private final Stream<T> stream;

        public RichStream(Stream<T> stream) {
                this.stream = stream;
        }

        public List<T> toList() {
                return stream.collect(Collectors.toList());
        }

        public Set<T> toSet() {
                return stream.collect(Collectors.toSet());
        }
}
