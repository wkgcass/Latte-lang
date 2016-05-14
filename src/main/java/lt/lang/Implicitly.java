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
