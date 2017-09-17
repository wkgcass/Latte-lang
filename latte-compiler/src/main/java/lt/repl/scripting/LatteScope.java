package lt.repl.scripting;

import javax.script.Bindings;
import java.util.*;

/**
 * eval scope
 */
public class LatteScope implements Bindings {
        private final Map<String, Class<?>> types = new HashMap<String, Class<?>>();
        private final Map<String, Object> objects = new LinkedHashMap<String, Object>();

        public LatteScope() {
        }

        @Override
        public synchronized Object put(String name, Object value) {
                if (name == null || name.isEmpty()) throw new IllegalArgumentException("invalid name, empty");
                if (types.containsKey(name)) {
                        Class<?> type = types.get(name);
                        if (type.isPrimitive()) {
                                if (value == null) {
                                        throw new NullPointerException("assign null to primitive type variable");
                                }
                                if (type == int.class) {
                                        type = Integer.class;
                                } else if (type == long.class) {
                                        type = Long.class;
                                } else if (type == float.class) {
                                        type = Float.class;
                                } else if (type == double.class) {
                                        type = Double.class;
                                } else if (type == short.class) {
                                        type = Short.class;
                                } else if (type == byte.class) {
                                        type = Byte.class;
                                } else if (type == char.class) {
                                        type = Character.class;
                                } else if (type == boolean.class) {
                                        type = Boolean.class;
                                } else throw new IllegalArgumentException("unknown primitive type " + type);
                        }
                        if (value == null || type.isInstance(value)) {
                                return objects.put(name, value);
                        } else {
                                throw new IllegalArgumentException(name + " is not of type " + type.getName());
                        }
                } else {
                        return putNew(name, value, Object.class);
                }
        }

        public synchronized Object putNew(String name, Object value, Class<?> type) {
                types.put(name, type);
                return objects.put(name, value);
        }

        @Override
        public synchronized void putAll(Map<? extends String, ?> toMerge) {
                for (Entry<? extends String, ?> e : toMerge.entrySet()) {
                        put(e.getKey(), e.getValue());
                }
        }

        @Override
        public void clear() {
                objects.clear();
        }

        @Override
        public Set<String> keySet() {
                return objects.keySet();
        }

        @Override
        public Collection<Object> values() {
                return objects.values();
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
                return objects.entrySet();
        }

        @Override
        public int size() {
                return objects.size();
        }

        @Override
        public boolean isEmpty() {
                return objects.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
                return objects.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
                return objects.containsValue(value);
        }

        @Override
        public Object get(Object key) {
                return objects.get(key);
        }

        public Class<?> getType(String key) {
                return types.get(key);
        }

        @Override
        public Object remove(Object key) {
                return objects.remove(key);
        }

        @Override
        public String toString() {
                StringBuilder sb = new StringBuilder("{");
                boolean isFirst = true;
                for (Entry<String, Object> entry : objects.entrySet()) {
                        String name = entry.getKey();
                        Object value = entry.getValue();
                        Class<?> type = types.get(name);
                        if (isFirst) {
                                isFirst = false;
                        } else {
                                sb.append(",");
                        }
                        sb.append(name).append(":").append(type.getName()).append("=").append(value);
                }
                sb.append("}");
                return sb.toString();
        }
}
