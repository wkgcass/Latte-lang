package lt.lang;

import lt.compiler.LtBug;

import java.lang.invoke.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * invoke dynamic
 */
public class Dynamic {
        @SuppressWarnings("unused")
        public static final int GET_FIELD = 1;
        @SuppressWarnings("unused")
        public static final int GET_STATIC = 2;
        @SuppressWarnings("unused")
        public static final int PUT_FIELD = 3;
        @SuppressWarnings("unused")
        public static final int PUT_STATIC = 4;
        @SuppressWarnings("unused")
        public static final int INVOKE_VIRTUAL = 5;
        public static final int INVOKE_STATIC = 6;
        public static final int INVOKE_SPECIAL = 7;
        @SuppressWarnings("unused")
        public static final int NEW_INVOKE_SPECIAL = 8;
        @SuppressWarnings("unused")
        public static final int INVOKE_INTERFACE = 9;

        private static final int PRIMITIVE_BOX_CAST_BASE = 233;

        private Dynamic() {
        }

        private static MethodHandle methodHandle;

        static {
                try {
                        methodHandle = MethodHandles.lookup().findStatic(Dynamic.class, "invoke", MethodType.methodType(Object.class, Object.class, Class.class, String.class, boolean[].class, Object[].class));
                } catch (NoSuchMethodException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                }
        }

        @SuppressWarnings("unused")
        public static CallSite bootstrap(MethodHandles.Lookup lookup, String methodName, MethodType methodType) throws NoSuchMethodException, IllegalAccessException {
                boolean[] primitives = new boolean[methodType.parameterCount() - 1];
                for (int i = 0; i < primitives.length; ++i) {
                        primitives[i] = methodType.parameterType(i + 1).isPrimitive();
                }
                MethodHandle mh = MethodHandles.insertArguments(methodHandle, 1, lookup.lookupClass(), methodName, primitives);
                mh = mh.asCollector(Object[].class, methodType.parameterCount() - 1).asType(methodType);

                MutableCallSite mCallSite = new MutableCallSite(mh);
                mCallSite.setTarget(mh);
                return mCallSite;
        }

        private static void fillMethodCandidates(Class<?> c, Class<?> invoker, String method, Object[] args, List<Method> methodList) {
                out:
                for (Method m : c.getDeclaredMethods()) {
                        if (!m.getName().equals(method)) continue;
                        if (m.getParameterCount() != args.length) continue;

                        if (Modifier.isPrivate(m.getModifiers())) {
                                // private
                                if (!invoker.equals(c)) continue;
                        } else if (Modifier.isProtected(m.getModifiers())) {
                                // protected
                                if (!c.getPackage().equals(invoker.getPackage()) && !c.isAssignableFrom(invoker)) continue;
                        } else if (!Modifier.isPublic(m.getModifiers())) {
                                // package access
                                if (!c.getPackage().equals(invoker.getPackage())) continue;
                        }
                        // else public
                        // do nothing

                        for (int i = 0; i < args.length; ++i) {
                                if (!m.getParameterTypes()[i].isInstance(args[i])) {
                                        // is not instance
                                        // check primitive
                                        Class<?> cls = m.getParameterTypes()[i];
                                        Object obj = args[i];
                                        if (cls.equals(int.class)) {
                                                if (!obj.getClass().equals(Integer.class)) continue out;
                                        } else if (cls.equals(short.class)) {
                                                if (!obj.getClass().equals(Short.class)) continue out;
                                        } else if (cls.equals(byte.class)) {
                                                if (!obj.getClass().equals(Byte.class)) continue out;
                                        } else if (cls.equals(boolean.class)) {
                                                if (!obj.getClass().equals(Boolean.class)) continue out;
                                        } else if (cls.equals(char.class)) {
                                                if (!obj.getClass().equals(Character.class)) continue out;
                                        } else if (cls.equals(long.class)) {
                                                if (!obj.getClass().equals(Long.class)) continue out;
                                        } else if (cls.equals(double.class)) {
                                                if (!obj.getClass().equals(Double.class)) continue out;
                                        } else if (cls.equals(float.class)) {
                                                if (!obj.getClass().equals(Float.class)) continue out;
                                        } else {
                                                continue out;
                                        }
                                }
                        }

                        methodList.add(m);
                }
        }

        /**
         * bfs search the required type
         *
         * @param current  current
         * @param required required
         * @return cast steps - 0 means no cast
         */
        private static int bfsSearch(Class<?> current, Class<?> required) {
                Set<Class<?>> visited = new HashSet<>();
                Queue<Class<?>> queue = new ArrayDeque<>();
                List<Class<?>> ready = new LinkedList<>();
                queue.add(current);
                visited.add(current);
                int count = 0;
                while (!queue.isEmpty() || !ready.isEmpty()) {
                        if (queue.isEmpty()) {
                                queue.addAll(ready);
                                ready.clear();
                                ++count;
                        }

                        Class<?> c = queue.remove();
                        if (c.equals(required)) return count;
                        // fill in super
                        if (c.getSuperclass() != null
                                &&
                                !visited.contains(c.getSuperclass())) {

                                ready.add(c.getSuperclass());
                                visited.add(c.getSuperclass());
                        }
                        for (Class<?> i : c.getInterfaces()) {
                                if (!visited.contains(i)) {
                                        ready.add(i);
                                        visited.add(i);
                                }
                        }
                }
                throw new LtBug(required + " is not assignable from " + current);
        }

        public static Object invoke(Object o, Class<?> invoker, String method, boolean[] primitives, Object[] args) throws Throwable {
                if (primitives.length != args.length) throw new LtBug("primitives.length should equal to args.length");
                List<Method> methodList = new ArrayList<>();

                Class<?> c = o.getClass();
                while (c != null) {
                        fillMethodCandidates(c, invoker, method, args, methodList);
                        c = c.getSuperclass();
                }
                c = o.getClass();
                Queue<Class<?>> interfaces = new ArrayDeque<>();
                Collections.addAll(interfaces, c.getInterfaces());
                while (!interfaces.isEmpty()) {
                        Class<?> i = interfaces.remove();
                        fillMethodCandidates(i, invoker, method, args, methodList);
                        Collections.addAll(interfaces, i.getInterfaces());
                }

                if (methodList.isEmpty()) {
                        if (args.length == 1 && isBoxType(o.getClass()) && isBoxType(args[0].getClass())) {
                                return invokePrimitive(o, method, args[0]);
                        } else if (args.length == 0 && isBoxType(o.getClass())) {
                                return invokePrimitive(o, method);
                        } else if (method.equals("add")
                                && args.length == 1
                                && (args[0] instanceof String || o instanceof String)
                                && !(o instanceof Undefined)
                                && !(args[0] instanceof Undefined)) {
                                // string add
                                return String.valueOf(o) + String.valueOf(args[0]);
                        } else if (method.equals("set")) {
                                return invoke(o, invoker, "put", primitives, args);
                        } else
                                throw new RuntimeException("cannot find method to invoke " + o + "." + method + Arrays.toString(args));
                }

                // calculate every method's cast steps
                Map<Method, int[]> steps = new HashMap<>();
                for (Method m : methodList) {
                        int[] step = new int[args.length];
                        for (int i = 0; i < args.length; ++i) {
                                Class<?> type = m.getParameterTypes()[i];
                                if (primitives[i] && type.isPrimitive()) {
                                        step[i] = 0;
                                } else if (primitives[i]) {
                                        // param is not primitive
                                        step[i] = PRIMITIVE_BOX_CAST_BASE; // first cast to box type
                                        step[i] += bfsSearch(args[i].getClass(), type);
                                } else if (type.isPrimitive()) {
                                        // arg is not primitive
                                        step[i] = PRIMITIVE_BOX_CAST_BASE; // cast to primitive
                                } else {
                                        // both not primitive
                                        step[i] = bfsSearch(args[i].getClass(), type);
                                }
                        }
                        steps.put(m, step);
                }

                // choose the best match
                Method methodToInvoke = null;
                int[] step = null;
                for (Map.Entry<Method, int[]> entry : steps.entrySet()) {
                        if (methodToInvoke == null) {
                                methodToInvoke = entry.getKey();
                                step = entry.getValue();
                        } else {
                                int[] newStep = entry.getValue();
                                boolean isBetter = false;
                                boolean isWorse = false;
                                for (int i = 0; i < step.length; ++i) {
                                        if (step[i] == newStep[i]) continue;
                                        else if (step[i] > newStep[i]) isBetter = true;
                                        else if (step[i] < newStep[i]) isWorse = true;

                                        if (isBetter && isWorse)
                                                throw new RuntimeException(
                                                        "cannot decide which method to invoke:\n"
                                                                + methodToInvoke + ":" + Arrays.toString(step) + "\n"
                                                                + entry.getKey() + ":" + Arrays.toString(newStep));
                                }

                                if (isBetter) {
                                        methodToInvoke = entry.getKey();
                                        step = entry.getValue();
                                }
                        }
                }

                assert methodToInvoke != null;
                methodToInvoke.setAccessible(true);

                try {
                        return methodToInvoke.invoke(o, args);
                } catch (InvocationTargetException e) {
                        throw e.getCause();
                }
        }

        private static Object invokePrimitive(Object o, String op) {
                switch (op) {
                        case "not":
                                o = prepareNumber(o);
                                if (o == null) break;
                                if (o instanceof Integer) return ~((Number) o).intValue();
                                if (o instanceof Long) return ~((Number) o).longValue();
                                break;
                        case "negate":
                                o = prepareNumber(o);
                                if (o == null) break;
                                if (o instanceof Integer) return -((Number) o).intValue();
                                if (o instanceof Float) return -((Number) o).floatValue();
                                if (o instanceof Long) return -((Number) o).longValue();
                                if (o instanceof Double) return -((Number) o).doubleValue();
                                break;
                        default:
                                throw new RuntimeException("unknown one variable operation method " + op);
                }
                throw new RuntimeException("cannot invoke " + o + "." + op);
        }

        /**
         * cast integer/short/byte into Integer<br>
         * let Long/Float/Double stay their types<br>
         * cast other Number into Double
         *
         * @param n any type to be prepared
         * @return Integer/Long/Float/Double or null if it's not number
         */
        private static Object prepareNumber(Object n) {
                if (n instanceof Integer
                        || n instanceof Short
                        || n instanceof Byte) return ((Number) n).intValue();
                else if (n instanceof Character) return (int) (Character) n;
                else if (n instanceof Long || n instanceof Float || n instanceof Double) return n;
                else if (n instanceof Number) return ((Number) n).doubleValue();
                else return null;
        }

        /**
         * get top type<br>
         * Integer and Integer ==> Integer<br>
         * Integer and Float ==> Float<br>
         * Integer and Long ==> Long<br>
         * <br>
         * Float and Float ==> Float<br>
         * Float and Long ==> Double<br>
         * <br>
         * Long and Long ==> Long<br>
         * <br>
         * any and Double ==> Double<br>
         *
         * @param a Integer/Long/Float/Double
         * @param b Integer/Long/Float/Double
         * @return 1 for Integer, 2 for Float, 3 for Long, 4 for Double
         */
        private static int topType(Object a, Object b) {
                if (a instanceof Double || b instanceof Double) return 4;
                if (a instanceof Long || b instanceof Long) {
                        if (a instanceof Float || b instanceof Float) return 4;
                        return 3;
                }
                if (a instanceof Float || b instanceof Float) return 2;
                return 1;
        }

        private static Object invokePrimitive(Object a, String op, Object b) {
                switch (op) {
                        case Lang.add:
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                int topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() + ((Number) b).intValue();
                                if (topType == 2) return ((Number) a).floatValue() + ((Number) b).floatValue();
                                if (topType == 3) return ((Number) a).longValue() + ((Number) b).longValue();
                                return ((Number) a).doubleValue() + ((Number) b).doubleValue();
                        case Lang.and:
                                if (a instanceof Boolean && b instanceof Boolean)
                                        return (Boolean) a & (Boolean) b;
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() & ((Number) b).intValue();
                                if (topType == 2) break;
                                if (topType == 3) return ((Number) a).longValue() & ((Number) b).longValue();
                                break;
                        case Lang.or:
                                if (a instanceof Boolean && b instanceof Boolean)
                                        return (Boolean) a | (Boolean) b;
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() | ((Number) b).intValue();
                                if (topType == 2) break;
                                if (topType == 3) return ((Number) a).longValue() | ((Number) b).longValue();
                                break;
                        case Lang.divide:
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() / ((Number) b).intValue();
                                if (topType == 2) return ((Number) a).floatValue() / ((Number) b).floatValue();
                                if (topType == 3) return ((Number) a).longValue() / ((Number) b).longValue();
                                return ((Number) a).doubleValue() / ((Number) b).doubleValue();
                        case Lang.ge:
                                double x;
                                double y;
                                if (a instanceof Number) x = ((Number) a).doubleValue();
                                else if (a instanceof Character) x = (Character) a;
                                else break;

                                if (b instanceof Number) y = ((Number) b).doubleValue();
                                else if (b instanceof Character) y = ((Character) b);
                                else break;

                                return x >= y;
                        case Lang.gt:
                                if (a instanceof Number) x = ((Number) a).doubleValue();
                                else if (a instanceof Character) x = (Character) a;
                                else break;

                                if (b instanceof Number) y = ((Number) b).doubleValue();
                                else if (b instanceof Character) y = ((Character) b);
                                else break;

                                return x > y;
                        case Lang.le:
                                if (a instanceof Number) x = ((Number) a).doubleValue();
                                else if (a instanceof Character) x = (Character) a;
                                else break;

                                if (b instanceof Number) y = ((Number) b).doubleValue();
                                else if (b instanceof Character) y = ((Character) b);
                                else break;

                                return x <= y;
                        case Lang.lt:
                                if (a instanceof Number) x = ((Number) a).doubleValue();
                                else if (a instanceof Character) x = (Character) a;
                                else break;

                                if (b instanceof Number) y = ((Number) b).doubleValue();
                                else if (b instanceof Character) y = ((Character) b);
                                else break;

                                return x < y;
                        case Lang.multiply:
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() * ((Number) b).intValue();
                                if (topType == 2) return ((Number) a).floatValue() * ((Number) b).floatValue();
                                if (topType == 3) return ((Number) a).longValue() * ((Number) b).longValue();
                                return ((Number) a).doubleValue() * ((Number) b).doubleValue();
                        case Lang.remainder:
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() % ((Number) b).intValue();
                                if (topType == 2) break;
                                if (topType == 3) return ((Number) a).longValue() % ((Number) b).longValue();
                                break;
                        case Lang.shiftLeft:
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() << ((Number) b).intValue();
                                if (topType == 2) break;
                                if (topType == 3) return ((Number) a).longValue() << ((Number) b).longValue();
                                break;
                        case Lang.shiftRight:
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() >> ((Number) b).intValue();
                                if (topType == 2) break;
                                if (topType == 3) return ((Number) a).longValue() >> ((Number) b).longValue();
                                break;
                        case Lang.subtract:
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() - ((Number) b).intValue();
                                if (topType == 2) return ((Number) a).floatValue() - ((Number) b).floatValue();
                                if (topType == 3) return ((Number) a).longValue() - ((Number) b).longValue();
                                return ((Number) a).doubleValue() - ((Number) b).doubleValue();
                        case Lang.unsignedShiftRight:
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() >>> ((Number) b).intValue();
                                if (topType == 2) break;
                                if (topType == 3) return ((Number) a).longValue() >>> ((Number) b).longValue();
                                break;
                        case Lang.xor:
                                a = prepareNumber(a);
                                if (a == null) break;
                                b = prepareNumber(b);
                                if (b == null) break;
                                topType = topType(a, b);
                                if (topType == 1) return ((Number) a).intValue() ^ ((Number) b).intValue();
                                if (topType == 2) break;
                                if (topType == 3) return ((Number) a).longValue() ^ ((Number) b).longValue();
                                break;
                        default:
                                throw new RuntimeException("unknown two-variable operation method " + op);
                }
                throw new RuntimeException("cannot invoke " + a + " " + op + " " + b);
        }

        private static boolean isBoxType(Class<?> cls) {
                return cls.equals(Integer.class)
                        || cls.equals(Short.class)
                        || cls.equals(Byte.class)
                        || cls.equals(Boolean.class)
                        || cls.equals(Long.class)
                        || cls.equals(Character.class)
                        || cls.equals(Float.class)
                        || cls.equals(Double.class);
        }

}