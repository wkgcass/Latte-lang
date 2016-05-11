package lt.lang;

import lt.lang.function.Function1;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * an object for implicit type conversion
 */
public class Implicitly {
        private static final Implicitly implicitly = new Implicitly();

        /**
         * it's a map of (t1 => (t2 => ImplicitSession))
         */
        private Map<Class<?>, Map<Class<?>, ImplicitSession>> implicitSessionMap = new ConcurrentHashMap<>();

        private Implicitly() {
        }

        public static Implicitly get() {
                return implicitly;
        }

        public class ImplicitSession {
                private final Class<?> t1;
                private Class<?> t2;
                private int distance = 1;
                private Function1 func;

                public ImplicitSession(Class<?> t1) {
                        this.t1 = t1;
                }

                public ImplicitSession to(Class<?> t2) {
                        Map<Class<?>, ImplicitSession> map;
                        if (implicitSessionMap.containsKey(t1)) {
                                map = implicitSessionMap.get(t1);
                        } else {
                                map = new ConcurrentHashMap<>();
                                implicitSessionMap.put(t1, map);
                        }

                        if (map.containsKey(t2)) throw new RuntimeException("already exists : " + map.get(t2));
                        map.put(t2, this);

                        this.t2 = t2;
                        return this;
                }

                @Override
                public String toString() {
                        return "implicitly convert type " + t1.getName().replace(".", "::") + " to type " + t2.getName().replace(".", "::") + " distance " + distance + " via " + func;
                }

                public ImplicitSession distance(int dist) {
                        this.distance = dist;
                        return this;
                }

                public ImplicitSession via(Function1 func) {
                        this.func = func;
                        return this;
                }
        }

        public ImplicitSession convert(Class<?> t1) {
                return new ImplicitSession(t1);
        }
}
